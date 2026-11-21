package com.example.data.sync

import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.entity.SyncQueueEntity
import com.example.data.local.entity.SyncRecordMetaEntity
import com.example.data.remote.CloudDataMapper
import com.example.domain.engine.SyncConflictResolver
import com.example.domain.engine.SyncRetryPolicy
import com.example.domain.model.AuthValidator
import com.example.domain.model.CloudProgressionEventPayload
import com.example.domain.model.DEFAULT_SHADOW_ACCOUNT_OWNER_ID
import com.example.domain.model.SyncEntityType
import com.example.domain.model.SyncOperationType
import com.example.domain.model.SyncState
import kotlinx.serialization.encodeToString

class SyncQueueManager(private val database: SoloLevelingDatabase) {

    private val queueDao = database.syncQueueDao()
    private val metaDao = database.syncMetaDao()

    companion object {
        private val SHADOW_SYNC_TYPES = setOf(
            SyncEntityType.SHADOW_MONARCH_PROFILE,
            SyncEntityType.SHADOW,
            SyncEntityType.SHADOW_EXTRACTION,
            SyncEntityType.SHADOW_SKILL_UNLOCK,
            SyncEntityType.SHADOW_DEPLOYMENT,
            SyncEntityType.SHADOW_FORMATION,
            SyncEntityType.MONARCH_SKILL_UNLOCK,
            SyncEntityType.SHADOW_QUEST_PROGRESS,
            SyncEntityType.SHADOW_RESOURCE_TRANSACTION,
            SyncEntityType.SHADOW_EVIDENCE_CLAIM,
            SyncEntityType.SHADOW_RESONANCE_RECORD,
            SyncEntityType.SHADOW_BOND_PROFILE,
            SyncEntityType.SHADOW_BOND_EVENT,
            SyncEntityType.SHADOW_BOND_MILESTONE,
            SyncEntityType.SHADOW_SQUAD,
            SyncEntityType.SHADOW_SQUAD_MEMBER,
            SyncEntityType.SHADOW_MISSION_ASSIGNMENT
        )

        fun isShadowEntityType(entityType: SyncEntityType): Boolean = entityType in SHADOW_SYNC_TYPES

        fun scopedShadowLocalKey(userId: String, rawKey: String): String = "${userId}::$rawKey"
    }

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

        // 5. Immutable RPG, Life, Academic & Social progression events (account-scoped, idempotency-keyed)
        database.rpgAuditDao().getAllOnce()
            .filter { it.accountOwnerId == userId || it.accountOwnerId == com.example.AppContainer.DEFAULT_LOCAL_ACCOUNT_OWNER_ID }
            .forEach { audit ->
                val resolvedModule = if (audit.sourceType == "SOCIAL_MASTERY") "SOCIAL_MASTERY" else "RPG"
                val ev = CloudProgressionEventPayload(
                    idempotencyKey = audit.rewardKey,
                    sourceModule = resolvedModule,
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

        // 6. Shadow Monarch System (Strictly account-isolated: NEVER scan or upload local_vault_player_1 rows)
        val cleanUserId = userId.trim()
        if (cleanUserId.isNotBlank() && cleanUserId != DEFAULT_SHADOW_ACCOUNT_OWNER_ID) {
            scanShadowEntitiesForAccount(cleanUserId, nowEpochMs)
        }
    }

    private suspend fun scanShadowEntitiesForAccount(userId: String, nowEpochMs: Long) {
        database.shadowMonarchProfileDao().getProfileForOwnerOnce(userId)?.let { profile ->
            val key = scopedShadowLocalKey(userId, profile.accountOwnerId)
            val cloudUuid = resolveOrAssignUuid(userId, SyncEntityType.SHADOW_MONARCH_PROFILE, key)
            val payload = CloudDataMapper.shadowMonarchProfileToPayload(profile, cloudUuid)
            enqueueChange(
                userId = userId,
                entityType = SyncEntityType.SHADOW_MONARCH_PROFILE,
                localKey = key,
                operation = SyncOperationType.UPSERT,
                payloadJson = CloudDataMapper.json.encodeToString(payload),
                nowEpochMs = nowEpochMs
            )
        }

        database.shadowDao().getAllShadowsForOwnerOnce(userId).forEach { shadow ->
            val key = scopedShadowLocalKey(userId, shadow.shadowId)
            val cloudUuid = resolveOrAssignUuid(userId, SyncEntityType.SHADOW, key)
            val payload = CloudDataMapper.shadowToPayload(shadow, cloudUuid)
            enqueueChange(
                userId = userId,
                entityType = SyncEntityType.SHADOW,
                localKey = key,
                operation = SyncOperationType.UPSERT,
                payloadJson = CloudDataMapper.json.encodeToString(payload),
                nowEpochMs = nowEpochMs
            )
        }

        database.shadowExtractionDao().getAllExtractionsForOwnerOnce(userId).forEach { extraction ->
            val key = scopedShadowLocalKey(userId, extraction.extractionId)
            val cloudUuid = resolveOrAssignUuid(userId, SyncEntityType.SHADOW_EXTRACTION, key)
            val payload = CloudDataMapper.shadowExtractionToPayload(extraction, cloudUuid)
            enqueueChange(
                userId = userId,
                entityType = SyncEntityType.SHADOW_EXTRACTION,
                localKey = key,
                operation = SyncOperationType.UPSERT,
                payloadJson = CloudDataMapper.json.encodeToString(payload),
                nowEpochMs = nowEpochMs
            )
        }

        database.shadowSkillUnlockDao().getAllUnlocksForOwnerOnce(userId).forEach { unlock ->
            val key = scopedShadowLocalKey(userId, "${unlock.shadowId}::${unlock.skillId}")
            val cloudUuid = resolveOrAssignUuid(userId, SyncEntityType.SHADOW_SKILL_UNLOCK, key)
            val payload = CloudDataMapper.shadowSkillUnlockToPayload(unlock, cloudUuid)
            enqueueChange(
                userId = userId,
                entityType = SyncEntityType.SHADOW_SKILL_UNLOCK,
                localKey = key,
                operation = SyncOperationType.UPSERT,
                payloadJson = CloudDataMapper.json.encodeToString(payload),
                nowEpochMs = nowEpochMs
            )
        }

        database.shadowDeploymentDao().getAllDeploymentsForOwnerOnce(userId).forEach { deployment ->
            val key = scopedShadowLocalKey(userId, deployment.deploymentId)
            val cloudUuid = resolveOrAssignUuid(userId, SyncEntityType.SHADOW_DEPLOYMENT, key)
            val payload = CloudDataMapper.shadowDeploymentToPayload(deployment, cloudUuid)
            enqueueChange(
                userId = userId,
                entityType = SyncEntityType.SHADOW_DEPLOYMENT,
                localKey = key,
                operation = SyncOperationType.UPSERT,
                payloadJson = CloudDataMapper.json.encodeToString(payload),
                nowEpochMs = nowEpochMs
            )
        }

        database.shadowFormationDao().getAllFormationsForOwnerOnce(userId).forEach { formation ->
            val key = scopedShadowLocalKey(userId, formation.formationId)
            val cloudUuid = resolveOrAssignUuid(userId, SyncEntityType.SHADOW_FORMATION, key)
            val payload = CloudDataMapper.shadowFormationToPayload(formation, cloudUuid)
            enqueueChange(
                userId = userId,
                entityType = SyncEntityType.SHADOW_FORMATION,
                localKey = key,
                operation = SyncOperationType.UPSERT,
                payloadJson = CloudDataMapper.json.encodeToString(payload),
                nowEpochMs = nowEpochMs
            )
        }

        database.monarchSkillUnlockDao().getAllMonarchSkillUnlocksForOwnerOnce(userId).forEach { unlock ->
            val key = scopedShadowLocalKey(userId, unlock.skillId)
            val cloudUuid = resolveOrAssignUuid(userId, SyncEntityType.MONARCH_SKILL_UNLOCK, key)
            val payload = CloudDataMapper.monarchSkillUnlockToPayload(unlock, cloudUuid)
            enqueueChange(
                userId = userId,
                entityType = SyncEntityType.MONARCH_SKILL_UNLOCK,
                localKey = key,
                operation = SyncOperationType.UPSERT,
                payloadJson = CloudDataMapper.json.encodeToString(payload),
                nowEpochMs = nowEpochMs
            )
        }

        database.shadowQuestProgressDao().getAllQuestProgressForOwnerOnce(userId).forEach { quest ->
            val key = scopedShadowLocalKey(userId, quest.questId)
            val cloudUuid = resolveOrAssignUuid(userId, SyncEntityType.SHADOW_QUEST_PROGRESS, key)
            val payload = CloudDataMapper.shadowQuestProgressToPayload(quest, cloudUuid)
            enqueueChange(
                userId = userId,
                entityType = SyncEntityType.SHADOW_QUEST_PROGRESS,
                localKey = key,
                operation = SyncOperationType.UPSERT,
                payloadJson = CloudDataMapper.json.encodeToString(payload),
                nowEpochMs = nowEpochMs
            )
        }

        database.shadowResourceDao().getAllTransactionsForOwnerOnce(userId).forEach { tx ->
            val key = scopedShadowLocalKey(userId, tx.transactionId)
            val cloudUuid = resolveOrAssignUuid(userId, SyncEntityType.SHADOW_RESOURCE_TRANSACTION, key)
            val payload = CloudDataMapper.shadowResourceTransactionToPayload(tx, cloudUuid)
            enqueueChange(
                userId = userId,
                entityType = SyncEntityType.SHADOW_RESOURCE_TRANSACTION,
                localKey = key,
                operation = SyncOperationType.UPSERT,
                payloadJson = CloudDataMapper.json.encodeToString(payload),
                nowEpochMs = nowEpochMs
            )
        }

        database.shadowEvidenceClaimDao().getClaimsForOwnerOnce(userId).forEach { claim ->
            val key = scopedShadowLocalKey(userId, claim.claimId)
            val cloudUuid = resolveOrAssignUuid(userId, SyncEntityType.SHADOW_EVIDENCE_CLAIM, key)
            val payload = CloudDataMapper.shadowEvidenceClaimToPayload(claim, cloudUuid)
            enqueueChange(
                userId = userId,
                entityType = SyncEntityType.SHADOW_EVIDENCE_CLAIM,
                localKey = key,
                operation = SyncOperationType.UPSERT,
                payloadJson = CloudDataMapper.json.encodeToString(payload),
                nowEpochMs = nowEpochMs
            )
        }

        for (resonance in database.shadowResonanceDao().getResonanceRecordsForOwnerOnce(userId)) {
            val key = scopedShadowLocalKey(userId, resonance.resonanceId)
            val cloudUuid = resolveOrAssignUuid(userId, SyncEntityType.SHADOW_RESONANCE_RECORD, key)
            val payload = CloudDataMapper.shadowResonanceToPayload(resonance, cloudUuid)
            enqueueChange(
                userId = userId,
                entityType = SyncEntityType.SHADOW_RESONANCE_RECORD,
                localKey = key,
                operation = SyncOperationType.UPSERT,
                payloadJson = CloudDataMapper.json.encodeToString(payload),
                nowEpochMs = nowEpochMs
            )
        }

        for (bond in database.shadowBondDao().getAllBondProfilesForOwner(userId)) {
            val key = scopedShadowLocalKey(userId, bond.shadowId)
            val cloudUuid = resolveOrAssignUuid(userId, SyncEntityType.SHADOW_BOND_PROFILE, key)
            val payload = CloudDataMapper.shadowBondProfileToPayload(bond, cloudUuid)
            enqueueChange(
                userId = userId,
                entityType = SyncEntityType.SHADOW_BOND_PROFILE,
                localKey = key,
                operation = SyncOperationType.UPSERT,
                payloadJson = CloudDataMapper.json.encodeToString(payload),
                nowEpochMs = nowEpochMs
            )
        }

        for (squad in database.shadowTacticalDao().getAllSquadsForOwnerOnce(userId)) {
            val key = scopedShadowLocalKey(userId, squad.squadId)
            val cloudUuid = resolveOrAssignUuid(userId, SyncEntityType.SHADOW_SQUAD, key)
            val payload = CloudDataMapper.shadowSquadToPayload(squad, cloudUuid)
            enqueueChange(
                userId = userId,
                entityType = SyncEntityType.SHADOW_SQUAD,
                localKey = key,
                operation = SyncOperationType.UPSERT,
                payloadJson = CloudDataMapper.json.encodeToString(payload),
                nowEpochMs = nowEpochMs
            )
        }

        for (member in database.shadowTacticalDao().getAllSquadMembersForOwnerOnce(userId)) {
            val key = scopedShadowLocalKey(userId, "${member.squadId}_${member.shadowId}")
            val cloudUuid = resolveOrAssignUuid(userId, SyncEntityType.SHADOW_SQUAD_MEMBER, key)
            val payload = CloudDataMapper.shadowSquadMemberToPayload(member, cloudUuid)
            enqueueChange(
                userId = userId,
                entityType = SyncEntityType.SHADOW_SQUAD_MEMBER,
                localKey = key,
                operation = SyncOperationType.UPSERT,
                payloadJson = CloudDataMapper.json.encodeToString(payload),
                nowEpochMs = nowEpochMs
            )
        }

        for (assignment in database.shadowTacticalDao().getAllAssignmentsForOwnerOnce(userId)) {
            val key = scopedShadowLocalKey(userId, assignment.assignmentId)
            val cloudUuid = resolveOrAssignUuid(userId, SyncEntityType.SHADOW_MISSION_ASSIGNMENT, key)
            val payload = CloudDataMapper.shadowMissionAssignmentToPayload(assignment, cloudUuid)
            enqueueChange(
                userId = userId,
                entityType = SyncEntityType.SHADOW_MISSION_ASSIGNMENT,
                localKey = key,
                operation = SyncOperationType.UPSERT,
                payloadJson = CloudDataMapper.json.encodeToString(payload),
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

    /**
     * Returns ready queue items scoped to userId so queued Shadow records from another account
     * or local vault can never be uploaded under the wrong session.
     */
    suspend fun getReadyBatchForUser(
        userId: String,
        nowEpochMs: Long,
        limit: Int = 50
    ): List<SyncQueueEntity> {
        val candidates = queueDao.getReadyBatch(nowEpochMs, (limit * 3).coerceAtLeast(limit))
        val filtered = mutableListOf<SyncQueueEntity>()
        for (item in candidates) {
            val type = runCatching { SyncEntityType.valueOf(item.entityTypeName) }.getOrNull()
            val meta = metaDao.getByCompositeKey("${item.entityTypeName}:${item.localKey}")
            val owned = if (type != null && isShadowEntityType(type)) {
                userId.isNotBlank() &&
                    userId != DEFAULT_SHADOW_ACCOUNT_OWNER_ID &&
                    item.localKey.startsWith("${userId}::") &&
                    meta?.userId == userId
            } else {
                (meta?.userId ?: userId) == userId
            }
            if (owned) {
                filtered.add(item)
                if (filtered.size >= limit) break
            }
        }
        return filtered
    }

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
