package com.example.data.repository

import android.os.Build
import androidx.room.withTransaction
import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.entity.CloudAuthSessionEntity
import com.example.data.local.entity.CloudBackupEntity
import com.example.data.local.entity.RegisteredDeviceEntity
import com.example.data.local.entity.toDomain
import com.example.data.local.entity.toEntity
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.remote.CloudBackupBundle
import com.example.data.remote.CloudCallResult
import com.example.data.remote.CloudDataMapper
import com.example.data.remote.RemoteBackupDto
import com.example.data.remote.RemoteDeviceDto
import com.example.data.remote.SupabaseApiPort
import com.example.data.remote.TokenStorage
import com.example.data.sync.CloudSyncEngine
import com.example.data.sync.SyncQueueManager
import com.example.data.sync.SyncStatusTracker
import com.example.domain.engine.ProgressionEngine
import com.example.domain.engine.SyncConflictResolver
import com.example.domain.model.AccountStatus
import com.example.domain.model.AuthStatus
import com.example.domain.model.AuthValidator
import com.example.domain.model.BackupStatus
import com.example.domain.model.CloudBackupRecord
import com.example.domain.model.CloudProgressionEventPayload
import com.example.domain.model.CloudSettingsPayload
import com.example.domain.model.CloudTaskPayload
import com.example.domain.model.ConflictResolutionChoice
import com.example.domain.model.ConflictStatus
import com.example.domain.model.DeviceRegistration
import com.example.domain.model.HunterRank
import com.example.domain.model.PlayerIdentity
import com.example.domain.model.RestorePreview
import com.example.domain.model.SyncConflict
import com.example.domain.model.SyncDiagnosticsLog
import com.example.domain.model.SyncEntityType
import com.example.domain.model.SyncOperationType
import com.example.domain.model.SyncQueueItem
import com.example.domain.model.SyncState
import com.example.domain.model.SyncSummaryState
import com.example.domain.model.SystemEventType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonElement
import java.time.Instant
import java.util.UUID

class SyncRepository(
    private val database: SoloLevelingDatabase,
    private val preferencesRepository: UserPreferencesRepository,
    private val supabaseApi: SupabaseApiPort,
    private val tokenStorage: TokenStorage,
    val queueManager: SyncQueueManager,
    val statusTracker: SyncStatusTracker,
    private val syncEngine: CloudSyncEngine,
    private val systemEventRepository: SystemEventRepository? = null
) {

    fun observePlayerIdentity(): Flow<PlayerIdentity?> =
        database.cloudAuthDao().observeSession().map { it?.toDomain() }

    fun observeDevices(): Flow<List<DeviceRegistration>> =
        database.registeredDeviceDao().observeDevices().map { list -> list.map { it.toDomain() } }

    fun observePendingQueue(): Flow<List<SyncQueueItem>> =
        database.syncQueueDao().observeQueue().map { list -> list.map { it.toDomain() } }

    fun observeConflicts(): Flow<List<SyncConflict>> =
        database.syncConflictDao().observeConflicts().map { list -> list.map { it.toDomain() } }

    fun observeBackups(): Flow<List<CloudBackupRecord>> =
        database.cloudBackupDao().observeBackups().map { list -> list.map { it.toDomain() } }

    fun observeDiagnostics(): Flow<List<SyncDiagnosticsLog>> = statusTracker.observeDiagnostics()

    val syncSummaryState: StateFlow<SyncSummaryState> = statusTracker.summaryState

    fun isCloudConfigured(): Boolean = supabaseApi.isConfigured()

    fun buildOAuthUrl(provider: String, redirectUri: String): String? =
        supabaseApi.buildOAuthAuthorizeUrl(provider, redirectUri)

    suspend fun signUp(email: String, password: String, displayName: String): Result<PlayerIdentity> {
        val emailCheck = AuthValidator.validateEmail(email)
        if (!emailCheck.isValid) return Result.failure(IllegalArgumentException(emailCheck.errorMessage))
        val pwCheck = AuthValidator.validatePassword(password)
        if (!pwCheck.isValid) return Result.failure(IllegalArgumentException(pwCheck.errorMessage))
        val nameCheck = AuthValidator.validateDisplayName(displayName)
        if (!nameCheck.isValid) return Result.failure(IllegalArgumentException(nameCheck.errorMessage))

        val now = System.currentTimeMillis()
        return when (val res = supabaseApi.signUp(email.trim(), password, displayName.trim())) {
            is CloudCallResult.Success -> {
                val auth = res.data
                val userId = auth.user?.id?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
                val emailVerified = !auth.user?.emailConfirmedAt.isNullOrBlank()
                if (auth.accessToken.isNotBlank()) {
                    tokenStorage.saveTokens(auth.accessToken, auth.refreshToken)
                }
                val previousUserId = database.cloudAuthDao().getSessionOnce()?.supabaseUserId
                if (previousUserId != null && previousUserId != userId) {
                    clearAccountSyncState()
                }
                val profile = database.playerDao().getPlayerOnce()?.toDomain()
                val identity = persistSessionFromAuth(
                    userId = userId,
                    email = email.trim(),
                    displayName = displayName.trim(),
                    emailVerified = emailVerified,
                    expiresAtEpochMs = if (auth.accessToken.isNotBlank()) now + auth.expiresInSeconds * 1000L else null,
                    level = profile?.level ?: 1,
                    totalXp = profile?.totalXp ?: 0L,
                    rank = profile?.rank ?: HunterRank.E_RANK,
                    activeTitle = profile?.title ?: "Awakened E-Rank",
                    nowEpochMs = now
                )
                ensureCurrentDeviceRegistered(userId, auth.accessToken, now)
                Result.success(identity)
            }
            is CloudCallResult.Failure -> Result.failure(IllegalStateException(res.message))
        }
    }

    suspend fun signIn(email: String, password: String): Result<PlayerIdentity> {
        val emailCheck = AuthValidator.validateEmail(email)
        if (!emailCheck.isValid) return Result.failure(IllegalArgumentException(emailCheck.errorMessage))
        if (password.isBlank()) return Result.failure(IllegalArgumentException("Password is required."))

        val now = System.currentTimeMillis()
        return when (val res = supabaseApi.signIn(email.trim(), password)) {
            is CloudCallResult.Success -> {
                val auth = res.data
                val userId = auth.user?.id?.takeIf { it.isNotBlank() }
                    ?: return Result.failure(IllegalStateException("Missing user identity from authentication server."))
                tokenStorage.saveTokens(auth.accessToken, auth.refreshToken)

                val previousUserId = database.cloudAuthDao().getSessionOnce()?.supabaseUserId
                if (previousUserId != null && previousUserId != userId) {
                    clearAccountSyncState()
                }
                val localProfile = database.playerDao().getPlayerOnce()?.toDomain()
                val remoteIdentity = when (
                    val idRes = supabaseApi.fetchOrInitializePlayerIdentity(
                        accessToken = auth.accessToken,
                        userId = userId,
                        displayName = localProfile?.codename ?: "Hunter"
                    )
                ) {
                    is CloudCallResult.Success -> idRes.data
                    is CloudCallResult.Failure -> null
                }

                val emailVerified = !auth.user.emailConfirmedAt.isNullOrBlank()
                val identity = persistSessionFromAuth(
                    userId = userId,
                    email = email.trim(),
                    displayName = remoteIdentity?.displayName ?: localProfile?.codename ?: "Hunter",
                    emailVerified = emailVerified,
                    expiresAtEpochMs = now + auth.expiresInSeconds * 1000L,
                    level = maxOf(remoteIdentity?.level ?: 1, localProfile?.level ?: 1),
                    totalXp = maxOf(remoteIdentity?.totalXp ?: 0L, localProfile?.totalXp ?: 0L),
                    rank = localProfile?.rank ?: HunterRank.E_RANK,
                    activeTitle = remoteIdentity?.activeTitle ?: localProfile?.title ?: "Awakened E-Rank",
                    playerIdentityUuid = remoteIdentity?.id?.takeIf { it.isNotBlank() },
                    nowEpochMs = now
                )
                ensureCurrentDeviceRegistered(userId, auth.accessToken, now)
                preferencesRepository.setSupabaseCloudSyncEnabled(true)
                Result.success(identity)
            }
            is CloudCallResult.Failure -> Result.failure(IllegalStateException(res.message))
        }
    }

    suspend fun refreshSessionAndVerification(): Result<PlayerIdentity> {
        val sess = database.cloudAuthDao().getSessionOnce()
            ?: return Result.failure(IllegalStateException("No active account session."))
        val refreshToken = tokenStorage.getRefreshToken()
        val now = System.currentTimeMillis()

        if (!refreshToken.isNullOrBlank()) {
            when (val r = supabaseApi.refreshSession(refreshToken)) {
                is CloudCallResult.Success -> {
                    tokenStorage.saveTokens(r.data.accessToken, r.data.refreshToken)
                    val verified = !r.data.user?.emailConfirmedAt.isNullOrBlank() || sess.emailVerified
                    val updated = sess.copy(
                        emailVerified = verified,
                        accountStatusName = if (verified) AccountStatus.ACTIVE.name else AccountStatus.PENDING_VERIFICATION.name,
                        authStatusName = if (verified) AuthStatus.SIGNED_IN.name else AuthStatus.EMAIL_UNVERIFIED.name,
                        expiresAtEpochMs = now + r.data.expiresInSeconds * 1000L,
                        updatedAtEpochMs = now
                    )
                    database.cloudAuthDao().upsertSession(updated)
                    return Result.success(updated.toDomain())
                }
                is CloudCallResult.Failure -> {
                    if (r.isSessionExpired || r.code == 400 || r.code == 401) {
                        val expired = sess.copy(
                            authStatusName = AuthStatus.SESSION_EXPIRED.name,
                            updatedAtEpochMs = now
                        )
                        database.cloudAuthDao().upsertSession(expired)
                        return Result.failure(IllegalStateException("Session expired. Sign in again to refresh credentials."))
                    }
                    return Result.failure(IllegalStateException(r.message))
                }
            }
        }
        val access = tokenStorage.getAccessToken()
            ?: return Result.failure(IllegalStateException("Session expired. Please sign in again."))
        return when (val u = supabaseApi.fetchUser(access)) {
            is CloudCallResult.Success -> {
                val verified = !u.data.emailConfirmedAt.isNullOrBlank()
                val updated = sess.copy(
                    emailVerified = verified,
                    accountStatusName = if (verified) AccountStatus.ACTIVE.name else AccountStatus.PENDING_VERIFICATION.name,
                    authStatusName = if (verified) AuthStatus.SIGNED_IN.name else AuthStatus.EMAIL_UNVERIFIED.name,
                    updatedAtEpochMs = now
                )
                database.cloudAuthDao().upsertSession(updated)
                Result.success(updated.toDomain())
            }
            is CloudCallResult.Failure -> Result.failure(IllegalStateException(u.message))
        }
    }

    suspend fun resendVerificationEmail(): Result<String> {
        val sess = database.cloudAuthDao().getSessionOnce()
            ?: return Result.failure(IllegalStateException("No pending account found."))
        return when (val res = supabaseApi.resendVerificationEmail(sess.email)) {
            is CloudCallResult.Success -> Result.success("Verification email resent to ${sess.email}.")
            is CloudCallResult.Failure -> Result.failure(IllegalStateException(res.message))
        }
    }

    suspend fun sendPasswordRecovery(email: String): Result<String> {
        val check = AuthValidator.validateEmail(email)
        if (!check.isValid) return Result.failure(IllegalArgumentException(check.errorMessage))
        return when (val res = supabaseApi.sendPasswordRecovery(email.trim())) {
            is CloudCallResult.Success -> Result.success("Password recovery instructions sent to ${email.trim()}.")
            is CloudCallResult.Failure -> Result.failure(IllegalStateException(res.message))
        }
    }

    suspend fun resetPassword(newPassword: String): Result<String> {
        val check = AuthValidator.validatePassword(newPassword)
        if (!check.isValid) return Result.failure(IllegalArgumentException(check.errorMessage))
        val accessToken = tokenStorage.getAccessToken()
            ?: return Result.failure(IllegalStateException("Active session or recovery token required before resetting password."))
        return when (val res = supabaseApi.updatePassword(accessToken, newPassword)) {
            is CloudCallResult.Success -> Result.success("Password updated successfully.")
            is CloudCallResult.Failure -> Result.failure(IllegalStateException(res.message))
        }
    }

    suspend fun signOut(): Result<Unit> {
        val token = tokenStorage.getAccessToken()
        if (!token.isNullOrBlank() && supabaseApi.isConfigured()) {
            supabaseApi.signOut(token)
        }
        tokenStorage.clearTokens()
        database.withTransaction {
            database.cloudAuthDao().clearSession()
            clearAccountSyncState()
        }
        preferencesRepository.setSupabaseCloudSyncEnabled(false)
        return Result.success(Unit)
    }

    private suspend fun clearAccountSyncState() {
        database.registeredDeviceDao().clearAll()
        database.syncQueueDao().clearAll()
        database.syncConflictDao().clearAll()
        database.syncMetaDao().clearCursors()
        database.syncMetaDao().clearAllMeta()
    }

    suspend fun updateDisplayName(newName: String): Result<String> {
        val check = AuthValidator.validateDisplayName(newName)
        if (!check.isValid) return Result.failure(IllegalArgumentException(check.errorMessage))
        val sess = database.cloudAuthDao().getSessionOnce()
            ?: return Result.failure(IllegalStateException("Sign in first to update cloud display name."))

        val clean = newName.trim()
        val now = System.currentTimeMillis()
        database.playerDao().getPlayerOnce()?.let { player ->
            database.playerDao().upsertPlayer(player.copy(codename = clean))
        }
        database.cloudAuthDao().upsertSession(sess.copy(displayName = clean, updatedAtEpochMs = now))

        val accessToken = tokenStorage.getAccessToken()
        if (!accessToken.isNullOrBlank() && supabaseApi.isConfigured()) {
            when (val res = supabaseApi.updateDisplayName(accessToken, sess.supabaseUserId, clean)) {
                is CloudCallResult.Failure -> return Result.success("Saved locally (will sync when online: ${res.message}).")
                is CloudCallResult.Success -> Unit
            }
        }
        return Result.success("Player display name updated to '$clean'.")
    }

    suspend fun setCloudSyncConsent(granted: Boolean) {
        val sess = database.cloudAuthDao().getSessionOnce() ?: return
        database.cloudAuthDao().upsertSession(
            sess.copy(cloudSyncConsentGranted = granted, updatedAtEpochMs = System.currentTimeMillis())
        )
        preferencesRepository.setSupabaseCloudSyncEnabled(granted && sess.autoSyncEnabled)
    }

    suspend fun setAutoSyncEnabled(enabled: Boolean) {
        val sess = database.cloudAuthDao().getSessionOnce() ?: return
        database.cloudAuthDao().upsertSession(
            sess.copy(autoSyncEnabled = enabled, updatedAtEpochMs = System.currentTimeMillis())
        )
        preferencesRepository.setSupabaseCloudSyncEnabled(enabled && sess.cloudSyncConsentGranted)
    }

    suspend fun ensureCurrentDeviceRegistered(
        userId: String,
        accessToken: String?,
        nowEpochMs: Long = System.currentTimeMillis()
    ): DeviceRegistration {
        val deviceUuid = tokenStorage.getOrCreateDeviceUuid()
        val entity = RegisteredDeviceEntity(
            deviceUuid = deviceUuid,
            userId = userId,
            deviceName = "${Build.MANUFACTURER ?: "Android"} ${Build.MODEL ?: "Device"}".trim(),
            manufacturerModel = Build.MODEL ?: "Android",
            osVersion = "Android ${Build.VERSION.RELEASE ?: "14"}",
            appVersion = "1.0",
            lastActiveEpochMs = nowEpochMs,
            lastSyncEpochMs = null,
            syncStatusName = SyncState.SYNCED.name,
            isCurrentDevice = true,
            isRevoked = false,
            createdAtEpochMs = nowEpochMs
        )
        database.registeredDeviceDao().upsertDevice(entity)
        if (!accessToken.isNullOrBlank() && supabaseApi.isConfigured()) {
            supabaseApi.upsertDevice(
                accessToken,
                RemoteDeviceDto(
                    id = entity.deviceUuid,
                    userId = userId,
                    deviceName = entity.deviceName,
                    manufacturerModel = entity.manufacturerModel,
                    osVersion = entity.osVersion,
                    appVersion = entity.appVersion,
                    syncStatus = "SYNCED",
                    isRevoked = false,
                    lastActiveAt = Instant.ofEpochMilli(nowEpochMs).toString()
                )
            )
        }
        return entity.toDomain()
    }

    suspend fun refreshDevices(): Result<List<DeviceRegistration>> {
        val sess = database.cloudAuthDao().getSessionOnce()
            ?: return Result.failure(IllegalStateException("Sign in to inspect linked devices."))
        val token = tokenStorage.getAccessToken()
            ?: return Result.success(database.registeredDeviceDao().getDevicesOnce().map { it.toDomain() })
        val currentUuid = tokenStorage.getOrCreateDeviceUuid()
        return when (val res = supabaseApi.listDevices(token, sess.supabaseUserId)) {
            is CloudCallResult.Success -> {
                val mapped = res.data.map { dto ->
                    RegisteredDeviceEntity(
                        deviceUuid = dto.id,
                        userId = dto.userId,
                        deviceName = dto.deviceName,
                        manufacturerModel = dto.manufacturerModel,
                        osVersion = dto.osVersion,
                        appVersion = dto.appVersion,
                        lastActiveEpochMs = System.currentTimeMillis(),
                        lastSyncEpochMs = null,
                        syncStatusName = runCatching { SyncState.valueOf(dto.syncStatus) }.getOrDefault(SyncState.SYNCED).name,
                        isCurrentDevice = dto.id == currentUuid,
                        isRevoked = dto.isRevoked,
                        createdAtEpochMs = System.currentTimeMillis()
                    )
                }
                database.registeredDeviceDao().upsertDevices(mapped)
                Result.success(mapped.map { it.toDomain() })
            }
            is CloudCallResult.Failure -> Result.failure(IllegalStateException(res.message))
        }
    }

    suspend fun revokeDevice(deviceUuid: String): Result<String> {
        val sess = database.cloudAuthDao().getSessionOnce()
            ?: return Result.failure(IllegalStateException("Not authenticated."))
        database.registeredDeviceDao().markDeviceRevoked(deviceUuid)
        val token = tokenStorage.getAccessToken()
        if (!token.isNullOrBlank() && supabaseApi.isConfigured()) {
            val res = supabaseApi.revokeDevice(token, sess.supabaseUserId, deviceUuid)
            if (res is CloudCallResult.Failure) {
                return Result.failure(IllegalStateException(res.message))
            }
        }
        if (deviceUuid == tokenStorage.getOrCreateDeviceUuid()) {
            signOut()
            return Result.success("Current device revoked and signed out.")
        }
        return Result.success("Device access revoked.")
    }

    suspend fun syncNow(triggerSource: String = "MANUAL"): Result<SyncDiagnosticsLog> {
        val res = syncEngine.runIncrementalSync(triggerSource)
        res.onSuccess { log ->
            systemEventRepository?.publishEvent(
                eventType = SystemEventType.SYNC_COMPLETED,
                relatedEntityId = log.operationUuid,
                sourceModule = "CLOUD",
                metadata = mapOf(
                    "uploaded" to log.recordsUploaded.toString(),
                    "downloaded" to log.recordsDownloaded.toString()
                ),
                processImmediately = false
            )
        }.onFailure { err ->
            systemEventRepository?.publishEvent(
                eventType = SystemEventType.SYNC_FAILED,
                sourceModule = "CLOUD",
                metadata = mapOf("error" to (err.message ?: "Sync failed").take(120)),
                processImmediately = false
            )
        }
        return res
    }

    suspend fun retryQueueItem(queueId: Long) {
        queueManager.retryItemNow(queueId)
    }

    suspend fun resolveConflict(
        conflictId: Long,
        choice: ConflictResolutionChoice,
        nowEpochMs: Long = System.currentTimeMillis()
    ): Result<String> {
        val conflict = database.syncConflictDao().getById(conflictId)
            ?: return Result.failure(IllegalArgumentException("Conflict not found."))
        val sess = database.cloudAuthDao().getSessionOnce()
        val userId = sess?.supabaseUserId ?: "local"

        if (conflict.entityTypeName == SyncEntityType.PERSONAL_TASK.name) {
            val localDto = runCatching { CloudDataMapper.json.decodeFromString<CloudTaskPayload>(conflict.localPayloadJson) }.getOrNull()
            val cloudDto = runCatching { CloudDataMapper.json.decodeFromString<CloudTaskPayload>(conflict.cloudPayloadJson) }.getOrNull()
            val chosen = when (choice) {
                ConflictResolutionChoice.KEEP_LOCAL -> localDto
                ConflictResolutionChoice.KEEP_CLOUD -> cloudDto
                ConflictResolutionChoice.MERGE_SAFE -> {
                    if (localDto != null && cloudDto != null) {
                        localDto.copy(
                            description = cloudDto.description.ifBlank { localDto.description },
                            postponeCount = maxOf(localDto.postponeCount, cloudDto.postponeCount),
                            deletedAtEpochMs = null,
                            updatedAtEpochMs = nowEpochMs
                        )
                    } else localDto ?: cloudDto
                }
            }
            if (chosen != null) {
                val existing = database.personalTaskDao().getAllTasksOnce().find { it.id.toString() == conflict.localKey }
                if (existing != null) {
                    if (chosen.deletedAtEpochMs != null) {
                        database.personalTaskDao().deleteTask(existing.id)
                    } else {
                        database.personalTaskDao().updateTask(
                            existing.copy(
                                title = chosen.title,
                                description = chosen.description,
                                statusName = chosen.statusName,
                                priorityName = chosen.priorityName,
                                postponeCount = chosen.postponeCount,
                                completedAtEpochMs = chosen.completedAtEpochMs,
                                updatedAtEpochMs = nowEpochMs
                            )
                        )
                    }
                }
                queueManager.enqueueChange(
                    userId = userId,
                    entityType = SyncEntityType.PERSONAL_TASK,
                    localKey = conflict.localKey,
                    operation = if (chosen.deletedAtEpochMs != null) SyncOperationType.DELETE else SyncOperationType.UPSERT,
                    payloadJson = CloudDataMapper.json.encodeToString(chosen),
                    idempotencyKey = "RESOLVE:${conflict.conflictId}:$nowEpochMs",
                    nowEpochMs = nowEpochMs
                )
            }
        }

        val resolvedStatus = when (choice) {
            ConflictResolutionChoice.KEEP_LOCAL -> ConflictStatus.RESOLVED_LOCAL
            ConflictResolutionChoice.KEEP_CLOUD -> ConflictStatus.RESOLVED_CLOUD
            ConflictResolutionChoice.MERGE_SAFE -> ConflictStatus.RESOLVED_MERGED
        }
        database.syncConflictDao().update(
            conflict.copy(
                statusName = resolvedStatus.name,
                resolvedAtEpochMs = nowEpochMs
            )
        )
        return Result.success("Conflict resolved using ${choice.name.replace('_', ' ')}.")
    }

    suspend fun createCloudBackupSnapshot(nowEpochMs: Long = System.currentTimeMillis()): Result<CloudBackupRecord> {
        val sess = database.cloudAuthDao().getSessionOnce()
        val userId = sess?.supabaseUserId ?: "offline-local"
        val player = database.playerDao().getPlayerOnce()?.toDomain()
            ?: return Result.failure(IllegalStateException("Player profile unavailable."))
        val prefs = preferencesRepository.preferencesFlow.first()

        val habits = database.habitDao().getAllHabitsOnce().map {
            CloudDataMapper.habitToPayload(it, CloudDataMapper.stableUuid(userId, SyncEntityType.HABIT, it.id.toString()))
        }
        val completions = database.habitDao().getCompletionsSinceOnce(0L).map {
            val hUuid = CloudDataMapper.stableUuid(userId, SyncEntityType.HABIT, it.habitId.toString())
            val cUuid = CloudDataMapper.stableUuid(userId, SyncEntityType.HABIT_COMPLETION, "${it.habitId}_${it.dateEpochDay}")
            CloudDataMapper.habitCompletionToPayload(it, cUuid, hUuid)
        }
        val tasks = database.personalTaskDao().getAllTasksOnce().map {
            CloudDataMapper.taskToPayload(it, CloudDataMapper.stableUuid(userId, SyncEntityType.PERSONAL_TASK, it.id.toString()))
        }
        val events = database.rpgAuditDao().getAllOnce().map { audit ->
            CloudProgressionEventPayload(
                idempotencyKey = audit.rewardKey,
                sourceModule = "RPG",
                sourceRecordId = audit.sourceId,
                title = audit.title,
                verificationStatus = audit.verificationProvenance,
                xpAwarded = audit.xpAwarded,
                primaryAttribute = audit.primaryAttrName,
                primaryAttrXp = audit.primaryAttrXp,
                secondaryAttribute = audit.secondaryAttrName,
                secondaryAttrXp = audit.secondaryAttrXp,
                wasCapped = audit.wasCapped,
                occurredAtEpochMs = audit.timestampEpochMs
            )
        }

        val bundle = CloudBackupBundle(
            schemaVersion = 7,
            exportedAtEpochMs = nowEpochMs,
            playerCodename = player.codename,
            playerTitle = player.title,
            playerLevel = player.level,
            playerCurrentXp = player.currentLevelXp,
            playerTotalXp = player.totalXp,
            playerRankName = player.rank.name,
            streakDays = player.streakDays,
            verifiedActivitiesCount = player.verifiedActivitiesCount,
            selfReportedActivitiesCount = player.selfReportedActivitiesCount,
            habits = habits,
            habitCompletions = completions,
            tasks = tasks,
            progressionEvents = events,
            settings = CloudSettingsPayload(
                hudGlowEnabled = prefs.hudGlowEnabled,
                dailyResetHour = prefs.dailyResetHour,
                healthConnectSyncEnabled = prefs.healthConnectSyncEnabled,
                supabaseCloudSyncEnabled = prefs.supabaseCloudSyncEnabled,
                academicDailyGoalMinutes = prefs.academicDailyGoalMinutes,
                academicWeeklyGoalMinutes = prefs.academicWeeklyGoalMinutes,
                openRouterModelId = prefs.openRouterModelId,
                updatedAtEpochMs = nowEpochMs
            )
        )

        val payloadJson = CloudDataMapper.json.encodeToString(bundle)
        val checksum = SyncConflictResolver.computeSha256(payloadJson)
        val recordCount = habits.size + completions.size + tasks.size + events.size
        val backupUuid = UUID.randomUUID().toString()
        var status = BackupStatus.CREATED
        var uploadedAt: Long? = null

        val token = tokenStorage.getAccessToken()
        if (sess != null && !token.isNullOrBlank() && supabaseApi.isConfigured()) {
            val remoteDto = RemoteBackupDto(
                id = backupUuid,
                userId = userId,
                deviceId = tokenStorage.getOrCreateDeviceUuid(),
                schemaVersion = 7,
                progressionChecksum = checksum,
                totalXpSnapshot = player.totalXp,
                playerLevelSnapshot = player.level,
                recordCount = recordCount,
                summaryText = "Lv.${player.level} ${player.rank.displayName} • $recordCount records",
                snapshotJson = CloudDataMapper.json.decodeFromString<JsonElement>(payloadJson),
                status = "UPLOADED"
            )
            if (supabaseApi.uploadCloudBackup(token, remoteDto) is CloudCallResult.Success) {
                status = BackupStatus.UPLOADED
                uploadedAt = nowEpochMs
            }
        }

        val entity = CloudBackupEntity(
            backupUuid = backupUuid,
            userId = userId,
            schemaVersion = 7,
            progressionChecksum = checksum,
            summaryText = "Lv.${player.level} ${player.rank.displayName} • $recordCount records",
            payloadJson = payloadJson,
            totalXpSnapshot = player.totalXp,
            playerLevelSnapshot = player.level,
            recordCount = recordCount,
            statusName = status.name,
            createdAtEpochMs = nowEpochMs,
            uploadedAtEpochMs = uploadedAt
        )
        database.cloudBackupDao().upsert(entity)
        return Result.success(entity.toDomain())
    }

    suspend fun buildRestorePreview(backupUuid: String): Result<RestorePreview> {
        val entity = database.cloudBackupDao().getByUuid(backupUuid)
            ?: return Result.failure(IllegalArgumentException("Backup snapshot not found."))
        val bundle = runCatching {
            CloudDataMapper.json.decodeFromString<CloudBackupBundle>(entity.payloadJson)
        }.getOrNull() ?: return Result.failure(IllegalStateException("Backup payload is corrupted or unreadable."))

        val computedChecksum = SyncConflictResolver.computeSha256(entity.payloadJson)
        val isChecksumValid = computedChecksum == entity.progressionChecksum
        val isSchemaCompatible = bundle.schemaVersion in 6..7
        val currentPlayer = database.playerDao().getPlayerOnce()?.toDomain()
        val currentXp = currentPlayer?.totalXp ?: 0L
        val currentLevel = currentPlayer?.level ?: 1
        val localNewer = currentXp > bundle.playerTotalXp

        val warnings = buildList {
            if (!isChecksumValid) add("Checksum mismatch detected — do NOT restore corrupted snapshot.")
            if (!isSchemaCompatible) add("Incompatible schema version v${bundle.schemaVersion}.")
            if (localNewer) add("Local progression (${currentXp} XP, Lv.$currentLevel) is higher than backup (${bundle.playerTotalXp} XP). Local XP will not be overwritten downward unless empty.")
        }

        return Result.success(
            RestorePreview(
                backup = entity.toDomain(),
                currentLevel = currentLevel,
                currentTotalXp = currentXp,
                isChecksumValid = isChecksumValid,
                isSchemaCompatible = isSchemaCompatible,
                isLocalNewerThanBackup = localNewer,
                warnings = warnings,
                recordBreakdown = mapOf(
                    "Habits" to bundle.habits.size,
                    "Habit Logs" to bundle.habitCompletions.size,
                    "Personal Tasks" to bundle.tasks.size,
                    "Audit Events" to bundle.progressionEvents.size
                )
            )
        )
    }

    suspend fun executeRestore(backupUuid: String): Result<String> {
        val preview = buildRestorePreview(backupUuid).getOrElse { return Result.failure(it) }
        if (!preview.isChecksumValid) {
            return Result.failure(IllegalStateException("Backup integrity check failed. Restore aborted safely."))
        }
        if (!preview.isSchemaCompatible) {
            return Result.failure(IllegalStateException("Backup schema version is incompatible."))
        }

        val entity = database.cloudBackupDao().getByUuid(backupUuid)!!
        val bundle = CloudDataMapper.json.decodeFromString<CloudBackupBundle>(entity.payloadJson)

        val token = tokenStorage.getAccessToken()
        if (!token.isNullOrBlank() && supabaseApi.isConfigured()) {
            supabaseApi.validateCloudRestore(token, backupUuid)
        }

        database.withTransaction {
            val current = database.playerDao().getPlayerOnce()?.toDomain()
            val appliedKeys = database.rpgAuditDao().getAllOnce().map { it.rewardKey }.toSet()
            if (current != null) {
                val (reconciled, _) = SyncConflictResolver.reconcileProgressionFromEvents(
                    baselineProfile = current,
                    allEvents = bundle.progressionEvents,
                    alreadyAppliedKeys = appliedKeys
                )
                database.playerDao().upsertPlayer(
                    reconciled.copy(
                        codename = bundle.playerCodename,
                        title = bundle.playerTitle
                    ).toEntity()
                )
            }
            database.cloudBackupDao().upsert(entity.copy(statusName = BackupStatus.RESTORED.name))
        }
        return Result.success("Restored snapshot safely with ProgressionEngine validation.")
    }

    suspend fun deleteAccount(
        confirmationPhrase: String,
        password: String
    ): Result<String> {
        if (confirmationPhrase.trim() != "DELETE MY ACCOUNT") {
            return Result.failure(IllegalArgumentException("Type DELETE MY ACCOUNT exactly to confirm."))
        }
        val sess = database.cloudAuthDao().getSessionOnce()
            ?: return Result.failure(IllegalStateException("No authenticated cloud account active."))

        if (supabaseApi.isConfigured()) {
            // Reauthenticate before sensitive deletion when password provided
            if (password.isNotBlank()) {
                when (val reauth = supabaseApi.signIn(sess.email, password)) {
                    is CloudCallResult.Success -> tokenStorage.saveTokens(reauth.data.accessToken, reauth.data.refreshToken)
                    is CloudCallResult.Failure -> return Result.failure(IllegalStateException("Re-authentication failed: ${reauth.message}"))
                }
            }
            val token = tokenStorage.getAccessToken()
                ?: return Result.failure(IllegalStateException("Valid authenticated session required to delete cloud account."))

            when (val res = supabaseApi.requestAccountDeletion(token, confirmationPhrase.trim())) {
                is CloudCallResult.Failure -> return Result.failure(IllegalStateException(res.message))
                is CloudCallResult.Success -> Unit
            }
        }

        // Clean local cloud credentials, sync outbox, devices & backups safely
        tokenStorage.clearTokens()
        database.withTransaction {
            database.cloudAuthDao().clearSession()
            database.registeredDeviceDao().clearAll()
            database.syncQueueDao().clearAll()
            database.syncConflictDao().clearAll()
            database.syncMetaDao().clearCursors()
            database.syncMetaDao().clearAllMeta()
            database.cloudBackupDao().clearAll()
        }
        preferencesRepository.setSupabaseCloudSyncEnabled(false)
        return Result.success("Account deleted cleanly. Device switched back to Offline Vault mode.")
    }

    private suspend fun persistSessionFromAuth(
        userId: String,
        email: String,
        displayName: String,
        emailVerified: Boolean,
        expiresAtEpochMs: Long?,
        level: Int,
        totalXp: Long,
        rank: HunterRank,
        activeTitle: String,
        playerIdentityUuid: String? = null,
        nowEpochMs: Long
    ): PlayerIdentity {
        val identityUuid = playerIdentityUuid
            ?: CloudDataMapper.stableUuid(userId, SyncEntityType.PLAYER_SETTINGS, "identity")
        val entity = CloudAuthSessionEntity(
            id = 1,
            supabaseUserId = userId,
            playerIdentityUuid = identityUuid,
            email = email,
            displayName = displayName,
            activeTitle = activeTitle,
            hunterRankName = rank.name,
            level = level,
            totalXp = totalXp,
            accountStatusName = if (emailVerified) AccountStatus.ACTIVE.name else AccountStatus.PENDING_VERIFICATION.name,
            authStatusName = if (emailVerified) AuthStatus.SIGNED_IN.name else AuthStatus.EMAIL_UNVERIFIED.name,
            emailVerified = emailVerified,
            autoSyncEnabled = true,
            cloudSyncConsentGranted = true,
            expiresAtEpochMs = expiresAtEpochMs,
            lastSyncAtEpochMs = null,
            createdAtEpochMs = nowEpochMs,
            updatedAtEpochMs = nowEpochMs
        )
        database.cloudAuthDao().upsertSession(entity)
        return entity.toDomain()
    }
}
