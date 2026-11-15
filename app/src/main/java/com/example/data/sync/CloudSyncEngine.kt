package com.example.data.sync

import android.os.Build
import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.entity.CloudSyncCursorEntity
import com.example.data.local.entity.PersonalTaskEntity
import com.example.data.local.entity.RegisteredDeviceEntity
import com.example.data.local.entity.SyncConflictEntity
import com.example.data.local.entity.SyncOperationLogEntity
import com.example.data.local.entity.toDomain
import com.example.data.local.entity.toEntity
import com.example.data.remote.CloudCallResult
import com.example.data.remote.CloudDataMapper
import com.example.data.remote.CloudHabitPayload
import com.example.data.remote.RemoteDeviceDto
import com.example.data.remote.SupabaseApiPort
import com.example.data.remote.TokenStorage
import com.example.domain.engine.SyncConflictResolver
import com.example.domain.engine.TaskConflictOutcome
import com.example.domain.model.AuthStatus
import com.example.domain.model.AuthValidator
import com.example.domain.model.CloudProgressionEventPayload
import com.example.domain.model.CloudTaskPayload
import com.example.domain.model.ConflictStatus
import com.example.domain.model.SyncDiagnosticsLog
import com.example.domain.model.SyncEntityType
import com.example.domain.model.SyncState
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant
import java.util.UUID

class CloudSyncEngine(
    private val database: SoloLevelingDatabase,
    private val supabaseApi: SupabaseApiPort,
    private val tokenStorage: TokenStorage,
    private val queueManager: SyncQueueManager,
    private val statusTracker: SyncStatusTracker
) {
    private val pageSize = 50

    suspend fun runIncrementalSync(
        triggerSource: String,
        nowEpochMs: Long = System.currentTimeMillis()
    ): Result<SyncDiagnosticsLog> {
        val opUuid = UUID.randomUUID().toString()
        val session = database.cloudAuthDao().getSessionOnce()
            ?: return Result.failure(IllegalStateException("Sign in to synchronize with Supabase."))

        if (!session.cloudSyncConsentGranted) {
            return Result.failure(IllegalStateException("Cloud Sync privacy consent is not enabled."))
        }
        if (!supabaseApi.isConfigured()) {
            return Result.failure(IllegalStateException("Supabase project credentials are not configured; staying in Offline Vault mode."))
        }

        statusTracker.setSyncing(true)
        var uploaded = 0
        var downloaded = 0
        var skipped = 0
        var conflicts = 0
        var failed = 0
        var totalRetries = 0
        var errorCategory: String? = null
        var safeMessage = "Synchronized cleanly."

        try {
            val accessToken = ensureValidAccessToken(session.supabaseUserId, session.expiresAtEpochMs, nowEpochMs)
                ?: return failOperation(opUuid, triggerSource, nowEpochMs, "AUTH_EXPIRED", "Session expired. Please sign in again.")

            val deviceUuid = tokenStorage.getOrCreateDeviceUuid()
            // Check remote device revocation before mutating anything
            when (val devicesResp = supabaseApi.listDevices(accessToken, session.supabaseUserId)) {
                is CloudCallResult.Success -> {
                    val remoteCurrent = devicesResp.data.find { it.id == deviceUuid }
                    if (remoteCurrent?.isRevoked == true) {
                        database.registeredDeviceDao().markDeviceRevoked(deviceUuid)
                        tokenStorage.clearTokens()
                        database.cloudAuthDao().upsertSession(
                            session.copy(authStatusName = AuthStatus.SESSION_EXPIRED.name, updatedAtEpochMs = nowEpochMs)
                        )
                        return failOperation(opUuid, triggerSource, nowEpochMs, "DEVICE_REVOKED", "This device session was revoked from another device.")
                    }
                }
                is CloudCallResult.Failure -> Unit
            }

            // Upsert current device registration heartbeat
            val currentDevice = RegisteredDeviceEntity(
                deviceUuid = deviceUuid,
                userId = session.supabaseUserId,
                deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
                manufacturerModel = Build.MODEL ?: "Android",
                osVersion = "Android ${Build.VERSION.RELEASE ?: "14"}",
                appVersion = "1.0",
                lastActiveEpochMs = nowEpochMs,
                lastSyncEpochMs = session.lastSyncAtEpochMs,
                syncStatusName = SyncState.UPLOADING.name,
                isCurrentDevice = true,
                isRevoked = false,
                createdAtEpochMs = nowEpochMs
            )
            database.registeredDeviceDao().upsertDevice(currentDevice)
            supabaseApi.upsertDevice(
                accessToken,
                RemoteDeviceDto(
                    id = currentDevice.deviceUuid,
                    userId = session.supabaseUserId,
                    deviceName = currentDevice.deviceName,
                    manufacturerModel = currentDevice.manufacturerModel,
                    osVersion = currentDevice.osVersion,
                    appVersion = currentDevice.appVersion,
                    syncStatus = "UPLOADING",
                    isRevoked = false,
                    lastActiveAt = Instant.ofEpochMilli(nowEpochMs).toString(),
                    lastSyncAt = session.lastSyncAtEpochMs?.let { Instant.ofEpochMilli(it).toString() }
                )
            )

            // Phase A: Scan & push queued local mutations in batches
            queueManager.scanAndEnqueueLocalChanges(session.supabaseUserId, nowEpochMs)

            var hasMoreQueue = true
            while (hasMoreQueue) {
                val batch = queueManager.getReadyBatch(nowEpochMs, pageSize)
                if (batch.isEmpty()) {
                    hasMoreQueue = false
                    break
                }

                // 1. Validate progression events through trusted Edge Function (never direct table writes)
                val progressionItems = batch.filter { it.entityTypeName == SyncEntityType.PROGRESSION_EVENT.name }
                if (progressionItems.isNotEmpty()) {
                    val events = progressionItems.mapNotNull {
                        runCatching {
                            CloudDataMapper.json.decodeFromString<CloudProgressionEventPayload>(it.payloadJson)
                        }.getOrNull()
                    }
                    when (val res = supabaseApi.validateProgressionEvents(accessToken, deviceUuid, events)) {
                        is CloudCallResult.Success -> {
                            progressionItems.forEach {
                                queueManager.markSynced(it, session.supabaseUserId, nowEpochMs)
                            }
                            uploaded += res.data.accepted.size
                            skipped += res.data.duplicates.size
                        }
                        is CloudCallResult.Failure -> {
                            progressionItems.forEach {
                                totalRetries += 1
                                failed += 1
                                queueManager.markFailed(it, res.message, nowEpochMs)
                            }
                            errorCategory = res.category
                            safeMessage = res.message
                        }
                    }
                }

                // 2. Push mutable entity groups via PostgREST upsert (protected by auth.uid() RLS)
                val nonProgression = batch.filter { it.entityTypeName != SyncEntityType.PROGRESSION_EVENT.name }
                    .groupBy { it.entityTypeName }

                for ((entityTypeName, items) in nonProgression) {
                    val type = runCatching { SyncEntityType.valueOf(entityTypeName) }.getOrNull() ?: continue
                    items.forEach { queueManager.markUploading(it, nowEpochMs) }

                    val rows = buildJsonArray {
                        items.forEach { item ->
                            val rowObj = convertQueuePayloadToRow(session.supabaseUserId, type, item.payloadJson)
                            if (rowObj != null) add(rowObj)
                        }
                    }
                    if (rows.isEmpty()) {
                        items.forEach {
                            queueManager.markSynced(it, session.supabaseUserId, nowEpochMs)
                            skipped += 1
                        }
                        continue
                    }

                    when (val res = supabaseApi.upsertTableRecords(accessToken, type.wireTable, rows.toString())) {
                        is CloudCallResult.Success -> {
                            items.forEach { queueManager.markSynced(it, session.supabaseUserId, nowEpochMs) }
                            uploaded += items.size
                        }
                        is CloudCallResult.Failure -> {
                            items.forEach {
                                totalRetries += 1
                                failed += 1
                                queueManager.markFailed(it, res.message, nowEpochMs)
                            }
                            errorCategory = res.category
                            safeMessage = res.message
                        }
                    }
                }
            }

            // Phase B: Paginated incremental pull & type-aware conflict resolution
            val pulledTasks = pullPaginated(accessToken, session.supabaseUserId, SyncEntityType.PERSONAL_TASK, nowEpochMs)
            downloaded += pulledTasks.size
            conflicts += applyPulledTasks(pulledTasks, session.supabaseUserId, nowEpochMs)

            val pulledEvents = pullPaginated(accessToken, session.supabaseUserId, SyncEntityType.PROGRESSION_EVENT, nowEpochMs)
            downloaded += pulledEvents.size
            reconcilePulledProgression(pulledEvents)

            // Refresh device & session sync timestamp
            database.registeredDeviceDao().upsertDevice(
                currentDevice.copy(
                    syncStatusName = if (failed > 0) SyncState.FAILED.name else SyncState.SYNCED.name,
                    lastSyncEpochMs = nowEpochMs
                )
            )
            val playerNow = database.playerDao().getPlayerOnce()?.toDomain()
            database.cloudAuthDao().upsertSession(
                session.copy(
                    level = playerNow?.level ?: session.level,
                    totalXp = playerNow?.totalXp ?: session.totalXp,
                    hunterRankName = playerNow?.rank?.name ?: session.hunterRankName,
                    activeTitle = playerNow?.title ?: session.activeTitle,
                    lastSyncAtEpochMs = nowEpochMs,
                    updatedAtEpochMs = nowEpochMs
                )
            )

            val finalStatus = when {
                conflicts > 0 -> SyncState.CONFLICT
                failed > 0 -> SyncState.FAILED
                else -> SyncState.SYNCED
            }
            val log = SyncOperationLogEntity(
                operationUuid = opUuid,
                triggerSource = triggerSource,
                startedAtEpochMs = nowEpochMs,
                completedAtEpochMs = System.currentTimeMillis(),
                statusName = finalStatus.name,
                recordsUploaded = uploaded,
                recordsDownloaded = downloaded,
                recordsSkipped = skipped,
                conflictsDetected = conflicts,
                failedOperations = failed,
                retryCount = totalRetries,
                errorCategory = errorCategory,
                safeMessage = AuthValidator.sanitizeLogMessage(safeMessage)
            )
            database.syncDiagnosticsDao().upsert(log)
            statusTracker.setLastError(if (failed > 0) safeMessage else null)
            return Result.success(log.toDomain())
        } finally {
            statusTracker.setSyncing(false)
        }
    }

    private suspend fun ensureValidAccessToken(
        userId: String,
        expiresAtEpochMs: Long?,
        nowEpochMs: Long
    ): String? {
        val currentToken = tokenStorage.getAccessToken()
        val nearExpiry = expiresAtEpochMs != null && nowEpochMs >= expiresAtEpochMs - 60_000L
        if (!currentToken.isNullOrBlank() && !nearExpiry) return currentToken

        val refreshToken = tokenStorage.getRefreshToken() ?: return currentToken
        return when (val refreshed = supabaseApi.refreshSession(refreshToken)) {
            is CloudCallResult.Success -> {
                tokenStorage.saveTokens(refreshed.data.accessToken, refreshed.data.refreshToken)
                database.cloudAuthDao().getSessionOnce()?.let { sess ->
                    database.cloudAuthDao().upsertSession(
                        sess.copy(
                            expiresAtEpochMs = nowEpochMs + refreshed.data.expiresInSeconds * 1000L,
                            authStatusName = AuthStatus.SIGNED_IN.name,
                            updatedAtEpochMs = nowEpochMs
                        )
                    )
                }
                refreshed.data.accessToken
            }
            is CloudCallResult.Failure -> {
                if (refreshed.isSessionExpired || refreshed.code == 400 || refreshed.code == 401) {
                    tokenStorage.clearTokens()
                    database.cloudAuthDao().getSessionOnce()?.let { sess ->
                        database.cloudAuthDao().upsertSession(
                            sess.copy(authStatusName = AuthStatus.SESSION_EXPIRED.name, updatedAtEpochMs = nowEpochMs)
                        )
                    }
                    null
                } else {
                    currentToken
                }
            }
        }
    }

    private suspend fun pullPaginated(
        accessToken: String,
        userId: String,
        type: SyncEntityType,
        nowEpochMs: Long
    ): List<JsonObject> {
        val cursor = database.syncMetaDao().getCursor(type.name)
        val sinceIso = cursor?.lastPulledUpdatedAtIso ?: "1970-01-01T00:00:00Z"
        val collected = mutableListOf<JsonObject>()
        var offset = 0
        while (true) {
            when (val res = supabaseApi.fetchTableChanges(accessToken, type.wireTable, userId, sinceIso, pageSize, offset)) {
                is CloudCallResult.Success -> {
                    if (res.data.isEmpty()) break
                    collected.addAll(res.data)
                    if (res.data.size < pageSize) break
                    offset += pageSize
                }
                is CloudCallResult.Failure -> break
            }
        }
        if (collected.isNotEmpty()) {
            val latestIso = collected.lastOrNull()?.get("updated_at")?.jsonPrimitive?.contentOrNull
                ?: Instant.ofEpochMilli(nowEpochMs).toString()
            database.syncMetaDao().upsertCursor(
                CloudSyncCursorEntity(
                    entityTypeName = type.name,
                    lastPulledUpdatedAtIso = latestIso,
                    lastPulledEpochMs = nowEpochMs,
                    lastPageOffset = offset,
                    updatedAtEpochMs = nowEpochMs
                )
            )
        }
        return collected
    }

    private suspend fun applyPulledTasks(
        rows: List<JsonObject>,
        userId: String,
        nowEpochMs: Long
    ): Int {
        var conflictCount = 0
        val existingTasks = database.personalTaskDao().getAllTasksOnce()
        for (row in rows) {
            val cloudUuid = row["id"]?.jsonPrimitive?.contentOrNull ?: continue
            val payloadElement = row["payload_json"]?.jsonObject ?: continue
            val cloudPayload = runCatching {
                CloudDataMapper.json.decodeFromString<CloudTaskPayload>(payloadElement.toString())
            }.getOrNull() ?: continue

            val meta = database.syncMetaDao().getByCloudUuid(cloudUuid)
            val localTask = meta?.localKey?.toLongOrNull()?.let { id -> existingTasks.find { it.id == id } }
            if (localTask == null) {
                if (cloudPayload.deletedAtEpochMs == null) {
                    database.personalTaskDao().insertTask(
                        PersonalTaskEntity(
                            title = cloudPayload.title,
                            description = cloudPayload.description,
                            categoryName = cloudPayload.categoryName,
                            priorityName = cloudPayload.priorityName,
                            statusName = cloudPayload.statusName,
                            dueDateEpochDay = cloudPayload.dueDateEpochDay,
                            scheduledStartMinute = null,
                            estimatedMinutes = cloudPayload.estimatedMinutes,
                            linkedGoalId = null,
                            isTimeSensitive = false,
                            postponeCount = cloudPayload.postponeCount,
                            timezoneId = java.time.ZoneId.systemDefault().id,
                            completedAtEpochMs = cloudPayload.completedAtEpochMs,
                            createdAtEpochMs = cloudPayload.updatedAtEpochMs.takeIf { it > 0 } ?: nowEpochMs,
                            updatedAtEpochMs = cloudPayload.updatedAtEpochMs.takeIf { it > 0 } ?: nowEpochMs
                        )
                    )
                }
            } else {
                val localPayload = CloudDataMapper.taskToPayload(localTask, cloudUuid)
                when (val resolved = SyncConflictResolver.resolvePersonalTask(localPayload, cloudPayload)) {
                    is TaskConflictOutcome.AutoResolved -> {
                        val m = resolved.merged
                        database.personalTaskDao().updateTask(
                            localTask.copy(
                                title = m.title,
                                description = m.description,
                                priorityName = m.priorityName,
                                statusName = m.statusName,
                                postponeCount = m.postponeCount,
                                completedAtEpochMs = m.completedAtEpochMs,
                                updatedAtEpochMs = m.updatedAtEpochMs
                            )
                        )
                    }
                    is TaskConflictOutcome.ManualRequired -> {
                        conflictCount += 1
                        database.syncConflictDao().insert(
                            SyncConflictEntity(
                                entityTypeName = SyncEntityType.PERSONAL_TASK.name,
                                localKey = localTask.id.toString(),
                                cloudUuid = cloudUuid,
                                title = localTask.title,
                                conflictReason = resolved.reason,
                                localSummary = "Status: ${localTask.statusName}, Updated: ${localTask.updatedAtEpochMs}",
                                cloudSummary = "Status: ${cloudPayload.statusName}, Deleted: ${cloudPayload.deletedAtEpochMs != null}",
                                localPayloadJson = CloudDataMapper.json.encodeToString(localPayload),
                                cloudPayloadJson = CloudDataMapper.json.encodeToString(cloudPayload),
                                localUpdatedAtEpochMs = localTask.updatedAtEpochMs,
                                cloudUpdatedAtEpochMs = cloudPayload.updatedAtEpochMs,
                                resolutionStrategy = "USER_DECISION_REQUIRED",
                                statusName = ConflictStatus.OPEN.name,
                                resolvedAtEpochMs = null,
                                createdAtEpochMs = nowEpochMs
                            )
                        )
                    }
                }
            }
        }
        return conflictCount
    }

    private suspend fun reconcilePulledProgression(rows: List<JsonObject>) {
        if (rows.isEmpty()) return
        val existingProfile = database.playerDao().getPlayerOnce()?.toDomain() ?: return
        val appliedKeys = database.rpgAuditDao().getAllOnce().map { it.rewardKey }.toSet()
        val remoteEvents = rows.mapNotNull { row ->
            val key = row["idempotency_key"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            CloudProgressionEventPayload(
                idempotencyKey = key,
                sourceModule = row["source_module"]?.jsonPrimitive?.contentOrNull ?: "RPG",
                sourceRecordId = row["source_record_id"]?.jsonPrimitive?.contentOrNull ?: key,
                title = row["source_module"]?.jsonPrimitive?.contentOrNull ?: "Synced Activity",
                verificationStatus = row["verification_status"]?.jsonPrimitive?.contentOrNull ?: "SELF_REPORTED",
                xpAwarded = row["xp_awarded"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0,
                primaryAttribute = row["primary_attribute"]?.jsonPrimitive?.contentOrNull ?: "DIS",
                primaryAttrXp = row["primary_attr_xp"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0,
                secondaryAttribute = row["secondary_attribute"]?.jsonPrimitive?.contentOrNull,
                secondaryAttrXp = row["secondary_attr_xp"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0,
                wasCapped = row["was_capped"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false,
                occurredAtEpochMs = System.currentTimeMillis()
            )
        }
        val (reconciled, _) = SyncConflictResolver.reconcileProgressionFromEvents(
            baselineProfile = existingProfile,
            allEvents = remoteEvents,
            alreadyAppliedKeys = appliedKeys
        )
        if (reconciled.totalXp > existingProfile.totalXp) {
            database.playerDao().upsertPlayer(reconciled.toEntity())
        }
    }

    private fun convertQueuePayloadToRow(
        userId: String,
        type: SyncEntityType,
        payloadJson: String
    ): JsonObject? {
        return when (type) {
            SyncEntityType.PERSONAL_TASK -> {
                val dto = runCatching { CloudDataMapper.json.decodeFromString<CloudTaskPayload>(payloadJson) }.getOrNull()
                    ?: return null
                CloudDataMapper.taskPayloadToPostgrestRow(userId, dto)
            }
            SyncEntityType.HABIT -> {
                val dto = runCatching { CloudDataMapper.json.decodeFromString<CloudHabitPayload>(payloadJson) }.getOrNull()
                    ?: return null
                CloudDataMapper.habitPayloadToPostgrestRow(userId, dto)
            }
            SyncEntityType.HABIT_COMPLETION -> {
                val dto = runCatching { CloudDataMapper.json.decodeFromString<com.example.data.remote.CloudHabitCompletionPayload>(payloadJson) }.getOrNull()
                    ?: return null
                CloudDataMapper.habitCompletionPayloadToPostgrestRow(userId, dto)
            }
            else -> runCatching { CloudDataMapper.json.decodeFromString<JsonObject>(payloadJson) }.getOrNull()
        }
    }

    private suspend fun failOperation(
        opUuid: String,
        triggerSource: String,
        startedAtEpochMs: Long,
        category: String,
        message: String
    ): Result<SyncDiagnosticsLog> {
        val log = SyncOperationLogEntity(
            operationUuid = opUuid,
            triggerSource = triggerSource,
            startedAtEpochMs = startedAtEpochMs,
            completedAtEpochMs = System.currentTimeMillis(),
            statusName = SyncState.FAILED.name,
            recordsUploaded = 0,
            recordsDownloaded = 0,
            recordsSkipped = 0,
            conflictsDetected = 0,
            failedOperations = 1,
            retryCount = 0,
            errorCategory = category,
            safeMessage = AuthValidator.sanitizeLogMessage(message)
        )
        database.syncDiagnosticsDao().upsert(log)
        statusTracker.setLastError(message)
        return Result.failure(IllegalStateException(message))
    }
}
