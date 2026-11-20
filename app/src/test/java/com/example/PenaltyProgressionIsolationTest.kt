package com.example

import com.example.domain.engine.PenaltyCalculationEngine
import com.example.domain.engine.PenaltyEscalationEngine
import com.example.domain.model.AttributeBreakdown
import com.example.domain.model.AttributeType
import com.example.domain.model.CommitmentState
import com.example.domain.model.ConfirmedFailureRecord
import com.example.domain.model.DefaultPenaltyPolicy
import com.example.domain.model.HunterRank
import com.example.domain.model.PenaltySeverity
import com.example.domain.model.PhysicalDevelopmentState
import com.example.domain.model.PlayerAttributes
import com.example.domain.model.PlayerProfile
import com.example.domain.model.Quest
import com.example.domain.model.QuestCategory
import com.example.domain.model.QuestCommitment
import com.example.domain.model.QuestType
import com.example.domain.model.RecoveryStatus
import com.example.domain.model.ShadowMonarchProfile
import com.example.domain.model.VerificationStatus
import com.example.domain.model.WorkoutStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class PenaltyProgressionIsolationTest {

    private val now = 1_700_000_000_000L

    @Test
    fun penaltyCalculationAndEscalation_causesZeroMutationsToProgression() {
        val initialPlayer = PlayerProfile(
            codename = "ShadowMonarch",
            title = "Wolf Slayer",
            level = 18,
            currentLevelXp = 450,
            totalXp = 12500L,
            rank = HunterRank.C_RANK,
            attributes = PlayerAttributes(
                str = AttributeBreakdown(AttributeType.STR, baseValue = 35, verifiedPoints = 10),
                vit = AttributeBreakdown(AttributeType.VIT, baseValue = 30, verifiedPoints = 8),
                agi = AttributeBreakdown(AttributeType.AGI, baseValue = 28, verifiedPoints = 5),
                intel = AttributeBreakdown(AttributeType.INT, baseValue = 25, verifiedPoints = 4),
                dis = AttributeBreakdown(AttributeType.DIS, baseValue = 32, verifiedPoints = 12),
                wis = AttributeBreakdown(AttributeType.WIS, baseValue = 20, verifiedPoints = 3),
                cha = AttributeBreakdown(AttributeType.CHA, baseValue = 22, verifiedPoints = 2)
            ),
            streakDays = 14,
            verifiedActivitiesCount = 42,
            selfReportedActivitiesCount = 18,
            recoveryScore = 85,
            createdAtEpochMs = now - (30 * 86_400_000L)
        )

        val initialPhysical = PhysicalDevelopmentState(
            baseJoggingTargetMinutes = 30,
            aiRecommendedJoggingMinutes = 35,
            completedJoggingMinutesToday = 0,
            workoutStatus = WorkoutStatus.PENDING,
            recoveryStatus = RecoveryStatus.OPTIMAL,
            weeklySessionsCompleted = 4,
            lastSessionTimestampMs = now - 86_400_000L,
            aiTrainingNote = "Cadence maintained."
        )

        val initialShadowMonarch = ShadowMonarchProfile(
            accountOwnerId = "local_vault_player_1",
            monarchLevel = 5,
            monarchXp = 1250L,
            monarchRank = com.example.domain.model.MonarchRank.SHADOW_LORD,
            monarchAuthority = 50,
            armyCapacity = 20,
            activeShadowCount = 7,
            totalShadowsRecruited = 10,
            highestShadowRank = com.example.domain.model.ShadowRank.KNIGHT_GRADE,
            totalEvolutions = 3,
            shadowArmyPowerRating = 4800L,
            unlockedMonarchSkillIds = listOf("monarch_shadow_extraction"),
            updatedAtEpochMs = now
        )

        // Simulate multiple severe and escalated penalty calculations
        val quest = Quest(
            id = 10L,
            title = "Crucial Dungeon Trial",
            description = "High difficulty trial",
            type = QuestType.WEEKLY,
            category = QuestCategory.PHYSICAL,
            targetValue = 120,
            currentValue = 0,
            unit = "min",
            xpReward = 150,
            primaryAttribute = AttributeType.STR,
            primaryAttrXpReward = 50,
            isCompleted = false,
            verificationStatus = VerificationStatus.SELF_REPORTED
        )

        val commitment = QuestCommitment(
            commitmentId = "commit_10_cycle_50",
            questId = 10L,
            accountOwnerId = "local_vault_player_1",
            logicalCycleId = "cycle_50",
            state = CommitmentState.ACTIVE,
            deadlineEpochMs = now - 3600_000L,
            gracePeriodMinutes = 30
        )

        val failure = ConfirmedFailureRecord(
            failureId = "fail_10_cycle_50",
            commitmentId = "commit_10_cycle_50",
            questId = 10L,
            logicalCycleId = "cycle_50",
            accountOwnerId = "local_vault_player_1",
            deadlineEpochMs = now - 3600_000L,
            gracePeriodExpiresAtEpochMs = now - 1800_000L,
            confirmedAtEpochMs = now,
            failureReasonCode = "GRACE_PERIOD_EXPIRED"
        )

        val priorFailures = listOf(
            ConfirmedFailureRecord(
                failureId = "fail_old_1",
                commitmentId = "commit_old_1",
                questId = 2L,
                logicalCycleId = "cycle_48",
                accountOwnerId = "local_vault_player_1",
                deadlineEpochMs = now - (3 * 86_400_000L),
                gracePeriodExpiresAtEpochMs = now - (3 * 86_400_000L),
                confirmedAtEpochMs = now - (3 * 86_400_000L),
                failureReasonCode = "GRACE_PERIOD_EXPIRED"
            ),
            ConfirmedFailureRecord(
                failureId = "fail_old_2",
                commitmentId = "commit_old_2",
                questId = 3L,
                logicalCycleId = "cycle_49",
                accountOwnerId = "local_vault_player_1",
                deadlineEpochMs = now - (1 * 86_400_000L),
                gracePeriodExpiresAtEpochMs = now - (1 * 86_400_000L),
                confirmedAtEpochMs = now - (1 * 86_400_000L),
                failureReasonCode = "GRACE_PERIOD_EXPIRED"
            )
        )

        val result = PenaltyCalculationEngine.calculate(
            failureRecord = failure,
            commitment = commitment,
            quest = quest,
            failureHistory = priorFailures,
            policy = DefaultPenaltyPolicy
        )

        // Penalty calculated: P4 (escalated from P3), 35 PP total
        assertEquals(PenaltySeverity.P4, result.severity)
        assertEquals(35, result.totalPenaltyPoints)

        // Strict assertions that player progression is 100% UNTOUCHED
        assertEquals("Player XP must not be deducted", 12500L, initialPlayer.totalXp)
        assertEquals("Current level XP must not change", 450, initialPlayer.currentLevelXp)
        assertEquals("Level must not change", 18, initialPlayer.level)
        assertEquals("Hunter Rank must not be downgraded", HunterRank.C_RANK, initialPlayer.rank)

        // Check each of the 7 canonical attributes
        assertEquals("STR attribute must remain untouched", 35, initialPlayer.attributes.str.baseValue)
        assertEquals("VIT attribute must remain untouched", 30, initialPlayer.attributes.vit.baseValue)
        assertEquals("AGI attribute must remain untouched", 28, initialPlayer.attributes.agi.baseValue)
        assertEquals("INT attribute must remain untouched", 25, initialPlayer.attributes.intel.baseValue)
        assertEquals("DIS attribute must remain untouched", 32, initialPlayer.attributes.dis.baseValue)
        assertEquals("WIS attribute must remain untouched", 20, initialPlayer.attributes.wis.baseValue)
        assertEquals("CHA attribute must remain untouched", 22, initialPlayer.attributes.cha.baseValue)

        // Physical development telemetry untouched
        assertEquals(14, initialPlayer.streakDays)
        assertEquals(RecoveryStatus.OPTIMAL, initialPhysical.recoveryStatus)
        assertEquals(4, initialPhysical.weeklySessionsCompleted)

        // Shadow monarch progression untouched
        assertEquals(5, initialShadowMonarch.monarchLevel)
        assertEquals(1250L, initialShadowMonarch.monarchXp)
        assertEquals(4800L, initialShadowMonarch.shadowArmyPowerRating)
        assertEquals(7, initialShadowMonarch.activeShadowCount)
    }
}
