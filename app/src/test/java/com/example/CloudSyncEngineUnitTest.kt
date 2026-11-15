package com.example

import com.example.data.remote.CloudDataMapper
import com.example.domain.engine.SyncConflictResolver
import com.example.domain.engine.SyncRetryPolicy
import com.example.domain.engine.TaskConflictOutcome
import com.example.domain.model.AttributeBreakdown
import com.example.domain.model.AttributeType
import com.example.domain.model.AuthValidator
import com.example.domain.model.CloudProgressionEventPayload
import com.example.domain.model.CloudSettingsPayload
import com.example.domain.model.CloudTaskPayload
import com.example.domain.model.HabitCompletionRecord
import com.example.domain.model.HabitDayStatus
import com.example.domain.model.HunterRank
import com.example.domain.model.JoggingSession
import com.example.domain.model.PersonalTaskStatus
import com.example.domain.model.PhysicalActivityKind
import com.example.domain.model.PlayerAttributes
import com.example.domain.model.PlayerProfile
import com.example.domain.model.RoutineCompletionRecord
import com.example.domain.model.SyncEntityType
import com.example.domain.model.VerificationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudSyncEngineUnitTest {

    @Test
    fun authValidatorEnforcesEmailPasswordDisplayNameAndRedactsSecrets() {
        assertFalse(AuthValidator.validateEmail("").isValid)
        assertFalse(AuthValidator.validateEmail("not-an-email").isValid)
        assertTrue(AuthValidator.validateEmail("monarch@example.com").isValid)

        assertFalse(AuthValidator.validatePassword("short1A").isValid)
        assertFalse(AuthValidator.validatePassword("alllowercase123").isValid)
        assertFalse(AuthValidator.validatePassword("ALLUPPERCASE123").isValid)
        assertFalse(AuthValidator.validatePassword("NoNumbersHere").isValid)
        assertTrue(AuthValidator.validatePassword("ShadowRank1").isValid)

        assertFalse(AuthValidator.validateDisplayName("A").isValid)
        assertTrue(AuthValidator.validateDisplayName("Sung Jinwoo").isValid)

        val raw = "Failed HTTP 401 Authorization: Bearer eyJhbGciOi.secret.token apikey=secret_key&password=MySecret123"
        val sanitized = AuthValidator.sanitizeLogMessage(raw)
        assertFalse(sanitized.contains("eyJhbGciOi"))
        assertFalse(sanitized.contains("secret_key"))
        assertFalse(sanitized.contains("MySecret123"))
        assertTrue(sanitized.contains("[REDACTED]"))
    }

    @Test
    fun syncRetryPolicyAppliesExponentialBackoffAndCapsMaxRetries() {
        assertEquals(15_000L, SyncRetryPolicy.nextRetryDelayMs(0))
        assertEquals(30_000L, SyncRetryPolicy.nextRetryDelayMs(1))
        assertEquals(60_000L, SyncRetryPolicy.nextRetryDelayMs(2))
        assertTrue(SyncRetryPolicy.nextRetryDelayMs(20) <= 3_600_000L)
        assertTrue(SyncRetryPolicy.shouldRetry(0))
        assertTrue(SyncRetryPolicy.shouldRetry(4))
        assertFalse(SyncRetryPolicy.shouldRetry(5))
    }

    @Test
    fun resolveSettingsUsesLatestValidTimestamp() {
        val older = CloudSettingsPayload(hudGlowEnabled = true, academicDailyGoalMinutes = 60, updatedAtEpochMs = 1000L)
        val newer = CloudSettingsPayload(hudGlowEnabled = false, academicDailyGoalMinutes = 90, updatedAtEpochMs = 2000L)
        assertEquals(newer, SyncConflictResolver.resolveSettings(older, newer))
        assertEquals(newer, SyncConflictResolver.resolveSettings(newer, older))
    }

    @Test
    fun resolvePersonalTaskPreservesCompletedStateAndEscalatesUnsafeConflicts() {
        val base = CloudTaskPayload(
            cloudUuid = "task-uuid-1",
            localId = 10L,
            title = "Submit Algorithm Report",
            description = "Draft",
            categoryName = "ACADEMIC",
            priorityName = "HIGH",
            statusName = PersonalTaskStatus.PENDING.name,
            updatedAtEpochMs = 100_000L
        )
        val completedLocal = base.copy(
            statusName = PersonalTaskStatus.COMPLETED.name,
            completedAtEpochMs = 110_000L,
            updatedAtEpochMs = 110_000L
        )
        val editedCloud = base.copy(
            description = "Final PDF checked",
            postponeCount = 2,
            updatedAtEpochMs = 120_000L
        )
        val mergedOutcome = SyncConflictResolver.resolvePersonalTask(completedLocal, editedCloud)
        assertTrue(mergedOutcome is TaskConflictOutcome.AutoResolved)
        val merged = (mergedOutcome as TaskConflictOutcome.AutoResolved).merged
        assertEquals(PersonalTaskStatus.COMPLETED.name, merged.statusName)
        assertEquals("Final PDF checked", merged.description)
        assertEquals(2, merged.postponeCount)

        // Concurrent rename on two devices within conflict window -> manual decision
        val renameA = base.copy(title = "Task Alpha", updatedAtEpochMs = 200_000L)
        val renameB = base.copy(title = "Task Beta", updatedAtEpochMs = 205_000L)
        assertTrue(SyncConflictResolver.resolvePersonalTask(renameA, renameB) is TaskConflictOutcome.ManualRequired)

        // Delete vs more recent edit -> manual decision (never silently discard newer user edit)
        val deletedOlder = base.copy(deletedAtEpochMs = 300_000L, updatedAtEpochMs = 300_000L)
        val editedNewer = base.copy(description = "Important details", updatedAtEpochMs = 305_000L)
        assertTrue(SyncConflictResolver.resolvePersonalTask(deletedOlder, editedNewer) is TaskConflictOutcome.ManualRequired)

        // Newer deletion after older edit -> safe auto-tombstone
        val deletedNewer = base.copy(deletedAtEpochMs = 400_000L, updatedAtEpochMs = 400_000L)
        val editedOlder = base.copy(updatedAtEpochMs = 350_000L)
        val tombstoneOutcome = SyncConflictResolver.resolvePersonalTask(deletedNewer, editedOlder)
        assertTrue(tombstoneOutcome is TaskConflictOutcome.AutoResolved)
        assertEquals(400_000L, (tombstoneOutcome as TaskConflictOutcome.AutoResolved).merged.deletedAtEpochMs)
    }

    @Test
    fun mergeHabitAndRoutineCompletionsPreserveAppendOnlyUnion() {
        val localHabits = listOf(
            HabitCompletionRecord(1L, 1L, 20001L, HabitDayStatus.COMPLETED, 30.0, "", "Done on Phone A", 1000L),
            HabitCompletionRecord(2L, 1L, 20002L, HabitDayStatus.PENDING, 0.0, "", "", 1000L)
        )
        val cloudHabits = listOf(
            HabitCompletionRecord(9L, 1L, 20002L, HabitDayStatus.COMPLETED, 30.0, "", "Done on Tablet B", 2000L),
            HabitCompletionRecord(10L, 1L, 20003L, HabitDayStatus.COMPLETED, 45.0, "", "Extra day", 3000L)
        )
        val mergedHabits = SyncConflictResolver.mergeHabitCompletions(localHabits, cloudHabits)
        assertEquals(3, mergedHabits.size)
        assertTrue(mergedHabits.all { it.status == HabitDayStatus.COMPLETED })

        val localRoutines = listOf(RoutineCompletionRecord(1L, 1L, 10L, 20001L, 1000L))
        val cloudRoutines = listOf(RoutineCompletionRecord(2L, 1L, 11L, 20001L, 2000L))
        val mergedRoutines = SyncConflictResolver.mergeRoutineCompletions(localRoutines, cloudRoutines)
        assertEquals(2, mergedRoutines.size)
    }

    @Test
    fun resolvePhysicalSessionPreservesVerifiedHealthConnectProvenance() {
        val verifiedLocal = JoggingSession(
            id = 1L,
            sourceId = "hc-123",
            originApp = "com.google.android.apps.fitness",
            title = "Morning 5K",
            activityKind = PhysicalActivityKind.RUNNING,
            startEpochMs = 1000L,
            endEpochMs = 1_801_000L,
            durationMinutes = 30,
            distanceMeters = 5000.0,
            avgPaceSecPerKm = 360,
            avgHeartRateBpm = 152,
            verificationStatus = VerificationStatus.VERIFIED_HEALTH_CONNECT,
            xpAwarded = 80,
            notes = "",
            createdAtEpochMs = 1_801_000L
        )
        val weakerCloudCopy = verifiedLocal.copy(
            verificationStatus = VerificationStatus.SELF_REPORTED,
            avgHeartRateBpm = null,
            xpAwarded = 45
        )

        val resolved = SyncConflictResolver.resolvePhysicalSession(verifiedLocal, weakerCloudCopy)
        assertEquals(VerificationStatus.VERIFIED_HEALTH_CONNECT, resolved.verificationStatus)
        assertEquals(152, resolved.avgHeartRateBpm)
        assertEquals(80, resolved.xpAwarded)

        val resolvedReverse = SyncConflictResolver.resolvePhysicalSession(weakerCloudCopy, verifiedLocal)
        assertEquals(VerificationStatus.VERIFIED_HEALTH_CONNECT, resolvedReverse.verificationStatus)
        assertEquals(80, resolvedReverse.xpAwarded)
    }

    @Test
    fun reconcileProgressionDeduplicatesEventsAndEnforcesMonotonicRank() {
        val base = PlayerProfile(
            level = 5,
            currentLevelXp = 10,
            xpToNextLevel = 260,
            totalXp = 600L,
            rank = HunterRank.D_RANK,
            attributes = PlayerAttributes(
                str = AttributeBreakdown(AttributeType.STR, baseValue = 12, verifiedPoints = 2),
                vit = AttributeBreakdown(AttributeType.VIT, baseValue = 14, verifiedPoints = 3),
                agi = AttributeBreakdown(AttributeType.AGI, baseValue = 12),
                intel = AttributeBreakdown(AttributeType.INT, baseValue = 12),
                dis = AttributeBreakdown(AttributeType.DIS, baseValue = 14, verifiedPoints = 3),
                wis = AttributeBreakdown(AttributeType.WIS, baseValue = 12),
                cha = AttributeBreakdown(AttributeType.CHA, baseValue = 12)
            ),
            verifiedActivitiesCount = 5
        )
        val ev1 = CloudProgressionEventPayload(
            idempotencyKey = "DUNGEON:DAILY:1",
            sourceModule = "RPG",
            sourceRecordId = "d-1",
            title = "Clear Gate",
            verificationStatus = VerificationStatus.VERIFIED_HEALTH_CONNECT.name,
            xpAwarded = 300,
            primaryAttribute = AttributeType.VIT.code,
            primaryAttrXp = 120,
            secondaryAttribute = AttributeType.DIS.code,
            secondaryAttrXp = 60,
            occurredAtEpochMs = 1000L
        )
        val duplicateEv1 = ev1.copy(title = "Duplicate Replay")
        val alreadyApplied = setOf("PREVIOUSLY_APPLIED")
        val alreadyAppliedEv = ev1.copy(idempotencyKey = "PREVIOUSLY_APPLIED", xpAwarded = 999)

        val (reconciled, newKeys) = SyncConflictResolver.reconcileProgressionFromEvents(
            baselineProfile = base,
            allEvents = listOf(ev1, duplicateEv1, alreadyAppliedEv),
            alreadyAppliedKeys = alreadyApplied
        )
        assertEquals(900L, reconciled.totalXp) // Only +300 applied once
        assertEquals(6, reconciled.verifiedActivitiesCount)
        assertTrue(reconciled.rank.ordinal >= HunterRank.D_RANK.ordinal)
        assertTrue("DUNGEON:DAILY:1" in newKeys)
        assertEquals(2, newKeys.size)
    }

    @Test
    fun cloudDataMapperStableUuidAndSha256AreDeterministic() {
        val u1 = CloudDataMapper.stableUuid("user-abc", SyncEntityType.PERSONAL_TASK, "42")
        val u2 = CloudDataMapper.stableUuid("user-abc", SyncEntityType.PERSONAL_TASK, "42")
        val uOther = CloudDataMapper.stableUuid("user-abc", SyncEntityType.HABIT, "42")
        assertEquals(u1, u2)
        assertNotEquals(u1, uOther)

        val hash1 = SyncConflictResolver.computeSha256("""{"xp":900}""")
        val hash2 = SyncConflictResolver.computeSha256("""{"xp":900}""")
        assertEquals(64, hash1.length)
        assertEquals(hash1, hash2)
    }
}
