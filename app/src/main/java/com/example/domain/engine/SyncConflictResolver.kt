package com.example.domain.engine

import com.example.domain.model.AttributeBreakdown
import com.example.domain.model.AttributeType
import com.example.domain.model.CloudProgressionEventPayload
import com.example.domain.model.CloudSettingsPayload
import com.example.domain.model.CloudTaskPayload
import com.example.domain.model.HabitCompletionRecord
import com.example.domain.model.HabitDayStatus
import com.example.domain.model.HunterRank
import com.example.domain.model.JoggingSession
import com.example.domain.model.PersonalTaskStatus
import com.example.domain.model.PlayerAttributes
import com.example.domain.model.PlayerProfile
import com.example.domain.model.RoutineCompletionRecord
import com.example.domain.model.VerificationStatus
import java.security.MessageDigest
import kotlin.math.min

/**
 * Exponential backoff policy for SyncQueueManager & WorkManager retries.
 */
object SyncRetryPolicy {
    const val MAX_RETRIES = 5
    private const val BASE_BACKOFF_MS = 15_000L
    private const val MAX_BACKOFF_MS = 3_600_000L // 1 hour

    fun nextRetryDelayMs(retryCount: Int): Long {
        val exponent = retryCount.coerceIn(0, 10)
        val delay = BASE_BACKOFF_MS shl exponent
        return min(delay, MAX_BACKOFF_MS)
    }

    fun shouldRetry(retryCount: Int): Boolean = retryCount < MAX_RETRIES
}

sealed class TaskConflictOutcome {
    data class AutoResolved(val merged: CloudTaskPayload, val note: String) : TaskConflictOutcome()
    data class ManualRequired(val reason: String) : TaskConflictOutcome()
}

/**
 * Explicit data-type-aware Conflict Resolution & Progression Integrity Engine.
 * Enforces Section 8 & Section 10 guarantees:
 * - Never uses raw LWW to overwrite XP, attributes, rank, or historical events.
 * - Preserves provenance (VERIFIED_HEALTH_CONNECT never downgraded).
 * - Deduplicates progression events strictly by idempotencyKey.
 */
object SyncConflictResolver {

    /** Rule A: Simple preferences use deterministic latest-valid-update policy. */
    fun resolveSettings(
        local: CloudSettingsPayload,
        cloud: CloudSettingsPayload
    ): CloudSettingsPayload = if (cloud.updatedAtEpochMs > local.updatedAtEpochMs) cloud else local

    /** Rule B: Personal tasks perform safe field-level merge or surface conflict when unsafe. */
    fun resolvePersonalTask(
        local: CloudTaskPayload,
        cloud: CloudTaskPayload
    ): TaskConflictOutcome {
        val localDeleted = local.deletedAtEpochMs != null
        val cloudDeleted = cloud.deletedAtEpochMs != null

        // Delete vs edit conflict -> do not silently discard edited work!
        if (localDeleted != cloudDeleted) {
            val activeSide = if (localDeleted) cloud else local
            val deleteTs = (if (localDeleted) local.deletedAtEpochMs else cloud.deletedAtEpochMs) ?: 0L
            if (activeSide.updatedAtEpochMs > deleteTs) {
                return TaskConflictOutcome.ManualRequired(
                    "Task '${activeSide.title}' was deleted on one device while edited more recently on another."
                )
            }
            val tombstoned = if (localDeleted) local else cloud
            return TaskConflictOutcome.AutoResolved(tombstoned, "Preserved newer tombstone deletion.")
        }

        val localDone = local.statusName == PersonalTaskStatus.COMPLETED.name
        val cloudDone = cloud.statusName == PersonalTaskStatus.COMPLETED.name

        // If both edited different titles/priorities cleanly apart or one completed the task while other edited description
        val mergedDescription = when {
            local.description.isBlank() -> cloud.description
            cloud.description.isBlank() -> local.description
            local.description == cloud.description -> local.description
            cloud.updatedAtEpochMs >= local.updatedAtEpochMs -> cloud.description
            else -> local.description
        }

        if (local.title != cloud.title && !localDone && !cloudDone &&
            kotlin.math.abs(local.updatedAtEpochMs - cloud.updatedAtEpochMs) < 600_000L
        ) {
            return TaskConflictOutcome.ManualRequired(
                "Task renamed concurrently to '${local.title}' vs '${cloud.title}'."
            )
        }

        val newer = if (cloud.updatedAtEpochMs > local.updatedAtEpochMs) cloud else local
        val finalStatus = if (localDone || cloudDone) PersonalTaskStatus.COMPLETED.name else newer.statusName
        val completedAt = listOfNotNull(local.completedAtEpochMs, cloud.completedAtEpochMs).minOrNull()

        return TaskConflictOutcome.AutoResolved(
            merged = newer.copy(
                description = mergedDescription,
                statusName = finalStatus,
                postponeCount = maxOf(local.postponeCount, cloud.postponeCount),
                completedAtEpochMs = if (finalStatus == PersonalTaskStatus.COMPLETED.name) completedAt else null,
                updatedAtEpochMs = maxOf(local.updatedAtEpochMs, cloud.updatedAtEpochMs)
            ),
            note = "Merged task fields preserving completion and description."
        )
    }

    /** Rule C: Habit completions never overwrite historical completion events; union by (habitId, dateEpochDay). */
    fun mergeHabitCompletions(
        local: List<HabitCompletionRecord>,
        cloud: List<HabitCompletionRecord>
    ): List<HabitCompletionRecord> {
        val byDayKey = linkedMapOf<Pair<Long, Long>, HabitCompletionRecord>()
        (local + cloud).forEach { item ->
            val key = item.habitId to item.dateEpochDay
            val existing = byDayKey[key]
            if (existing == null) {
                byDayKey[key] = item
            } else {
                val winner = when {
                    item.status == HabitDayStatus.COMPLETED && existing.status != HabitDayStatus.COMPLETED -> item
                    existing.status == HabitDayStatus.COMPLETED && item.status != HabitDayStatus.COMPLETED -> existing
                    item.achievedValue > existing.achievedValue -> item
                    item.recordedAtEpochMs > existing.recordedAtEpochMs -> item
                    else -> existing
                }
                byDayKey[key] = winner
            }
        }
        return byDayKey.values.sortedByDescending { it.dateEpochDay }
    }

    /** Rule C (Routines): Preserve routine completion history across devices. */
    fun mergeRoutineCompletions(
        local: List<RoutineCompletionRecord>,
        cloud: List<RoutineCompletionRecord>
    ): List<RoutineCompletionRecord> {
        val map = linkedMapOf<Triple<Long, Long, Long>, RoutineCompletionRecord>()
        (local + cloud).forEach { rec ->
            val key = Triple(rec.routineId, rec.activityId, rec.dateEpochDay)
            val existing = map[key]
            if (existing == null || rec.completedAtEpochMs > existing.completedAtEpochMs) {
                map[key] = rec
            }
        }
        return map.values.sortedByDescending { it.dateEpochDay }
    }

    /** Rule E: Physical records preserve original source, timestamps, and strongest provenance. */
    fun resolvePhysicalSession(
        local: JoggingSession,
        cloud: JoggingSession
    ): JoggingSession {
        val winnerProvenance = if (local.verificationStatus.isVerified && !cloud.verificationStatus.isVerified) {
            local.verificationStatus
        } else if (cloud.verificationStatus.isVerified && !local.verificationStatus.isVerified) {
            cloud.verificationStatus
        } else {
            local.verificationStatus
        }
        return if (local.verificationStatus.isVerified || cloud.createdAtEpochMs <= local.createdAtEpochMs) {
            local.copy(
                verificationStatus = winnerProvenance,
                avgHeartRateBpm = local.avgHeartRateBpm ?: cloud.avgHeartRateBpm,
                distanceMeters = local.distanceMeters ?: cloud.distanceMeters,
                xpAwarded = maxOf(local.xpAwarded, cloud.xpAwarded)
            )
        } else {
            cloud.copy(
                verificationStatus = winnerProvenance,
                avgHeartRateBpm = cloud.avgHeartRateBpm ?: local.avgHeartRateBpm,
                distanceMeters = cloud.distanceMeters ?: local.distanceMeters,
                xpAwarded = maxOf(local.xpAwarded, cloud.xpAwarded)
            )
        }
    }

    /**
     * Rules F & H & Section 10: Reconcile player XP, attributes, and Hunter Rank strictly from
     * deduplicated progression events + ProgressionEngine. Never allows device overwrite or duplicate XP.
     */
    fun reconcileProgressionFromEvents(
        baselineProfile: PlayerProfile,
        allEvents: List<CloudProgressionEventPayload>,
        alreadyAppliedKeys: Set<String>
    ): Pair<PlayerProfile, Set<String>> {
        val uniqueNewEvents = allEvents
            .distinctBy { it.idempotencyKey }
            .filter { it.idempotencyKey !in alreadyAppliedKeys }
            .sortedBy { it.occurredAtEpochMs }

        var level = baselineProfile.level.coerceAtLeast(1)
        var currentXp = baselineProfile.currentLevelXp.coerceAtLeast(0)
        var totalXp = baselineProfile.totalXp.coerceAtLeast(0L)
        var attrs = baselineProfile.attributes
        var verifiedCount = baselineProfile.verifiedActivitiesCount
        var selfReportedCount = baselineProfile.selfReportedActivitiesCount
        val appliedKeys = alreadyAppliedKeys.toMutableSet()

        uniqueNewEvents.forEach { ev ->
            val sanitizedXp = ev.xpAwarded.coerceAtLeast(0)
            val levelRes = ProgressionEngine.applyXp(level, currentXp, totalXp, sanitizedXp)
            level = levelRes.newLevel
            currentXp = levelRes.newCurrentLevelXp
            totalXp = levelRes.newTotalXp

            val isVerified = ev.verificationStatus != VerificationStatus.SELF_REPORTED.name
            if (isVerified) verifiedCount += 1 else selfReportedCount += 1

            attrs = applyAttrXp(attrs, ev.primaryAttribute, ev.primaryAttrXp.coerceAtLeast(0), isVerified)
            ev.secondaryAttribute?.let { sec ->
                attrs = applyAttrXp(attrs, sec, ev.secondaryAttrXp.coerceAtLeast(0), isVerified)
            }
            appliedKeys.add(ev.idempotencyKey)
        }

        val recomputedRank = evaluateMonotonicRank(baselineProfile.rank, level, attrs.totalScore, verifiedCount)
        val reconciled = baselineProfile.copy(
            level = level,
            currentLevelXp = currentXp,
            xpToNextLevel = ProgressionEngine.xpRequiredForLevel(level),
            totalXp = totalXp,
            rank = recomputedRank,
            attributes = attrs,
            verifiedActivitiesCount = verifiedCount,
            selfReportedActivitiesCount = selfReportedCount
        )
        return reconciled to appliedKeys
    }

    private fun applyAttrXp(
        attrs: PlayerAttributes,
        attrCode: String,
        xp: Int,
        isVerified: Boolean
    ): PlayerAttributes {
        if (xp <= 0) return attrs
        val type = AttributeType.entries.find { it.code == attrCode || it.name == attrCode } ?: return attrs
        fun add(b: AttributeBreakdown): AttributeBreakdown {
            val totalPool = b.progressXp + xp
            val points = totalPool / b.progressXpRequired.coerceAtLeast(1)
            val rem = totalPool % b.progressXpRequired.coerceAtLeast(1)
            return b.copy(
                verifiedPoints = b.verifiedPoints + if (isVerified) points else 0,
                selfReportedPoints = b.selfReportedPoints + if (!isVerified) points else 0,
                progressXp = rem
            )
        }
        return when (type) {
            AttributeType.STR -> attrs.copy(str = add(attrs.str))
            AttributeType.VIT -> attrs.copy(vit = add(attrs.vit))
            AttributeType.AGI -> attrs.copy(agi = add(attrs.agi))
            AttributeType.INT -> attrs.copy(intel = add(attrs.intel))
            AttributeType.DIS -> attrs.copy(dis = add(attrs.dis))
            AttributeType.WIS -> attrs.copy(wis = add(attrs.wis))
            AttributeType.CHA -> attrs.copy(cha = add(attrs.cha))
        }
    }

    private fun evaluateMonotonicRank(
        currentRank: HunterRank,
        level: Int,
        totalAttrScore: Int,
        verifiedCount: Int
    ): HunterRank {
        val eligible = ProgressionEngine.evaluateRank(
            level = level,
            totalAttributeScore = totalAttrScore,
            verifiedActivitiesCount = verifiedCount
        )
        return if (eligible.ordinal > currentRank.ordinal) eligible else currentRank
    }

    fun computeSha256(content: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(content.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
