package com.example.data.sync

import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.entity.SyncQueueEntity
import com.example.data.local.entity.SyncRecordMetaEntity
import com.example.data.remote.CloudDataMapper
import com.example.domain.engine.SyncConflictResolver
import com.example.domain.engine.SyncRetryPolicy
import com.example.domain.model.AuthValidator
import com.example.domain.model.CloudProgressionEventPayload
import com.example.domain.model.SyncEntityType
import com.example.domain.model.SyncOperationType
import com.example.domain.model.SyncState
import kotlinx.serialization.encodeToString

class SyncQueueManager(private val database: SoloLevelingDatabase) {

    private val queueDao = database.syncQueueDao()
    private val metaDao = database.syncMetaDao()

    suspend fun enqueueChange(
        userId: String,
        entityType: SyncEntityType,
        localKey: String,
        operation: SyncOperationType,
        payloadJson: String,
        idempotencyKey: String? = null,
        nowEpochMs: Long = System.currentTimeMillis()
    ) {
        val compositeKey = "${entityType.name}:$localKey"
        val existingMeta = metaDao.getByCompositeKey(compositeKey)
        val cloudUuid = existingMeta?.cloudUuid
            ?: CloudDataMapper.stableUuid(userId.ifBlank { "local" }, entityType, localKey)
        val payloadHash = SyncConflictResolver.computeSha256("${operation.name}:$payloadJson")

        if (existingMeta != null &&
            existingMeta.lastSyncedHash == payloadHash &&
            existingMeta.syncStateName == SyncState.SYNCED.name
        ) {
            return
        }

        val idemKey = idempotencyKey ?: "${entityType.name}:$cloudUuid:${operation.name}:$payloadHash"
        val existingQueued = queueDao.getByIdempotencyKey(idemKey)
        if (existingQueued == null) {
            queueDao.insertIgnore(
                SyncQueueEntity(
                    entityTypeName = entityType.name,
                    localKey = localKey,
                    cloudUuid = cloudUuid,
                    operationName = operation.name,
                    payloadJson = payloadJson,
                    idempotencyKey = idemKey,
                    syncStateName = SyncState.PENDING_UPLOAD.name,
                    retryCount = 0,
                    nextRetryAtEpochMs = 0L,
                    lastError = null,
                    createdAtEpochMs = nowEpochMs,
                    updatedAtEpochMs = nowEpochMs
                )
            )
        }

        metaDao.upsert(
            SyncRecordMetaEntity(
                compositeKey = compositeKey,
                entityType = entityType.name,
                localKey = localKey,
                cloudUuid = cloudUuid,
                userId = userId,
                version = (existingMeta?.version ?: 0) + 1,
                updatedAtEpochMs = nowEpochMs,
                deletedAtEpochMs = if (operation == SyncOperationType.DELETE) nowEpochMs else null,
                syncStateName = SyncState.PENDING_UPLOAD.name,
                lastSyncedHash = existingMeta?.lastSyncedHash ?: "",
                lastError = null
            )
        )
    }

    /**
     * Scans local Room entities for changes created offline or since the last sync
     * using stable SHA-256 payload hashes so unchanged rows are skipped cleanly.
     */
    suspend fun scanAndEnqueueLocalChanges(
        userId: String,
        nowEpochMs: Long = System.currentTimeMillis()
    ) {
        // 1. Personal tasks
        database.personalTaskDao().getAllTasksOnce().forEach { task ->
            val key = task.id.toString()
            val cloudUuid = resolveOrAssignUuid(userId, SyncEntityType.PERSONAL_TASK, key)
            val payload = CloudDataMapper.taskToPayload(task, cloudUuid)
            enqueueChange(
                userId = userId,
                entityType = SyncEntityType.PERSONAL_TASK,
                localKey = key,
                operation = SyncOperationType.UPSERT,
                payloadJson = CloudDataMapper.json.encodeToString(payload),
                nowEpochMs = nowEpochMs
            )
        }

        // 2. Habits & Habit Completions
        val habits = database.habitDao().getAllHabitsOnce()
        habits.forEach { habit ->
            val key = habit.id.toString()
            val cloudUuid = resolveOrAssignUuid(userId, SyncEntityType.HABIT, key)
            val payload = CloudDataMapper.habitToPayload(habit, cloudUuid)
            enqueueChange(
                userId = userId,
                entityType = SyncEntityType.HABIT,
                localKey = key,
                operation = SyncOperationType.UPSERT,
                payloadJson = CloudDataMapper.json.encodeToString(payload),
                nowEpochMs = nowEpochMs
            )
        }

        database.habitDao().getCompletionsSinceOnce(0L).forEach { comp ->
            val habitUuid = resolveOrAssignUuid(userId, SyncEntityType.HABIT, comp.habitId.toString())
            val key = "${comp.habitId}_${comp.dateEpochDay}"
            val cloudUuid = resolveOrAssignUuid(userId, SyncEntityType.HABIT_COMPLETION, key)
            val payload = CloudDataMapper.habitCompletionToPayload(comp, cloudUuid, habitUuid)
            enqueueChange(
                userId = userId,
                entityType = SyncEntityType.HABIT_COMPLETION,
                localKey = key,
                operation = SyncOperationType.UPSERT,
                payloadJson = CloudDataMapper.json.encodeToString(payload),
                idempotencyKey = "HABIT_COMP:$habitUuid:${comp.dateEpochDay}:${comp.statusName}:${comp.achievedValue}",
                nowEpochMs = nowEpochMs
            )
        }

        // 3. Jogging sessions
        database.joggingSessionDao().getSessionsSince(0L).forEach { jog ->
            val key = jog.sourceId
            val cloudUuid = resolveOrAssignUuid(userId, SyncEntityType.PHYSICAL_RECORD, key)
            val rowJson = CloudDataMapper.joggingToPostgrestRow(userId, jog, cloudUuid).toString()
            enqueueChange(
                userId = userId,
                entityType = SyncEntityType.PHYSICAL_RECORD,
                localKey = key,
                operation = SyncOperationType.UPSERT,
                payloadJson = rowJson,
                idempotencyKey = "PHYS:${jog.sourceId}",
                nowEpochMs = nowEpochMs
            )
        }

        // 4. Academic subjects
        database.academicSubjectDao().getActiveSubjectsOnce().forEach { subj ->
            val key = subj.id.toString()
            val cloudUuid = resolveOrAssignUuid(userId, SyncEntityType.ACADEMIC_SUBJECT, key)
            val rowJson = CloudDataMapper.academicSubjectToPostgrestRow(userId, subj, cloudUuid).toString()
            enqueueChange(
                userId = userId,
                entityType = SyncEntityType.ACADEMIC_SUBJECT,
                localKey = key,
                operation = SyncOperationType.UPSERT,
                payloadJson = rowJson,
                nowEpochMs = nowEpochMs
            )
        }

        // 5. Immutable RPG & Life & Academic progression events (idempotency-keyed)
        database.rpgAuditDao().getAllOnce().forEach { audit ->
            val ev = CloudProgressionEventPayload(
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
            enqueueChange(
                userId = userId,
                entityType = SyncEntityType.PROGRESSION_EVENT,
                localKey = audit.rewardKey,
                operation = SyncOperationType.UPSERT,
                payloadJson = CloudDataMapper.json.encodeToString(ev),
                idempotencyKey = "PROG:${audit.rewardKey}",
                nowEpochMs = nowEpochMs
            )
        }
    }

    private suspend fun resolveOrAssignUuid(
        userId: String,
        type: SyncEntityType,
        localKey: String
    ): String {
        val existing = metaDao.getByCompositeKey("${type.name}:$localKey")
        return existing?.cloudUuid ?: CloudDataMapper.stableUuid(userId.ifBlank { "local" }, type, localKey)
    }

    suspend fun getReadyBatch(nowEpochMs: Long, limit: Int = 50): List<SyncQueueEntity> =
        queueDao.getReadyBatch(nowEpochMs, limit)

    suspend fun markUploading(item: SyncQueueEntity, nowEpochMs: Long = System.currentTimeMillis()) {
        queueDao.update(item.copy(syncStateName = SyncState.UPLOADING.name, updatedAtEpochMs = nowEpochMs))
    }

    suspend fun markSynced(item: SyncQueueEntity, userId: String, nowEpochMs: Long = System.currentTimeMillis()) {
        queueDao.deleteById(item.queueId)
        val compositeKey = "${item.entityTypeName}:${item.localKey}"
        val existing = metaDao.getByCompositeKey(compositeKey)
        val hash = SyncConflictResolver.computeSha256("${item.operationName}:${item.payloadJson}")
        metaDao.upsert(
            SyncRecordMetaEntity(
                compositeKey = compositeKey,
                entityType = item.entityTypeName,
                localKey = item.localKey,
                cloudUuid = item.cloudUuid,
                userId = userId,
                version = existing?.version ?: 1,
                updatedAtEpochMs = nowEpochMs,
                deletedAtEpochMs = if (item.operationName == SyncOperationType.DELETE.name) nowEpochMs else null,
                syncStateName = SyncState.SYNCED.name,
                lastSyncedHash = hash,
                lastError = null
            )
        )
    }

    suspend fun markFailed(
        item: SyncQueueEntity,
        errorMessage: String,
        nowEpochMs: Long = System.currentTimeMillis()
    ) {
        val nextRetryCount = item.retryCount + 1
        val delayMs = SyncRetryPolicy.nextRetryDelayMs(nextRetryCount)
        val sanitized = AuthValidator.sanitizeLogMessage(errorMessage)
        queueDao.update(
            item.copy(
                syncStateName = SyncState.FAILED.name,
                retryCount = nextRetryCount,
                nextRetryAtEpochMs = if (SyncRetryPolicy.shouldRetry(nextRetryCount)) nowEpochMs + delayMs else Long.MAX_VALUE / 2,
                lastError = sanitized,
                updatedAtEpochMs = nowEpochMs
            )
        )
    }

    suspend fun retryItemNow(queueId: Long, nowEpochMs: Long = System.currentTimeMillis()) {
        val item = queueDao.getAllOnce().find { it.queueId == queueId } ?: return
        queueDao.update(
            item.copy(
                syncStateName = SyncState.PENDING_UPLOAD.name,
                retryCount = 0,
                nextRetryAtEpochMs = 0L,
                lastError = null,
                updatedAtEpochMs = nowEpochMs
            )
        )
    }
}
