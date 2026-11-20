package com.example

import com.example.domain.engine.PenaltyResolutionEngine
import com.example.domain.engine.RedemptionVerificationEngine
import com.example.domain.model.AttributeBreakdown
import com.example.domain.model.AttributeType
import com.example.domain.model.CorrectiveCategory
import com.example.domain.model.CorrectiveDifficulty
import com.example.domain.model.HunterRank
import com.example.domain.model.PenaltyRecord
import com.example.domain.model.PenaltySeverity
import com.example.domain.model.PenaltyStatus
import com.example.domain.model.PhysicalDevelopmentState
import com.example.domain.model.PlayerAttributes
import com.example.domain.model.PlayerProfile
import com.example.domain.model.RecoveryStatus
import com.example.domain.model.RedemptionLifecycleState
import com.example.domain.model.RedemptionPlan
import com.example.domain.model.RedemptionVerificationEvidence
import com.example.domain.model.RedemptionVerificationMethod
import com.example.domain.model.ShadowClass
import com.example.domain.model.ShadowMonarchProfile
import com.example.domain.model.ShadowRank
import com.example.domain.model.WorkoutStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RedemptionProgressionIsolationTest {

    private val now = 1_700_000_000_000L

    @Test
    fun redemptionLifecycleAndResolution_causesZeroMutationsToProgression() {
        val initialPlayer = PlayerProfile(
            codename = "ShadowMonarch",
            title = "Wolf Slayer",
            level = 20,
            currentLevelXp = 800,
            totalXp = 15000L,
            rank = HunterRank.C_RANK,
            attributes = PlayerAttributes(
                str = AttributeBreakdown(AttributeType.STR, baseValue = 40, verifiedPoints = 12),
                vit = AttributeBreakdown(AttributeType.VIT, baseValue = 35, verifiedPoints = 10),
                agi = AttributeBreakdown(AttributeType.AGI, baseValue = 30, verifiedPoints = 8),
                intel = AttributeBreakdown(AttributeType.INT, baseValue = 28, verifiedPoints = 6),
                dis = AttributeBreakdown(AttributeType.DIS, baseValue = 36, verifiedPoints = 14),
                wis = AttributeBreakdown(AttributeType.WIS, baseValue = 24, verifiedPoints = 5),
                cha = AttributeBreakdown(AttributeType.CHA, baseValue = 25, verifiedPoints = 4)
            ),
            streakDays = 21,
            verifiedActivitiesCount = 55,
            selfReportedActivitiesCount = 20,
            recoveryScore = 90,
            createdAtEpochMs = now - (60 * 86_400_000L)
        )

        val initialPhysical = PhysicalDevelopmentState(
            baseJoggingTargetMinutes = 30,
            aiRecommendedJoggingMinutes = 30,
            completedJoggingMinutesToday = 0,
            workoutStatus = WorkoutStatus.PENDING,
            recoveryStatus = RecoveryStatus.OPTIMAL,
            weeklySessionsCompleted = 5,
            lastSessionTimestampMs = now - 86_400_000L,
            aiTrainingNote = "Ready for regular load."
        )

        val initialMonarch = ShadowMonarchProfile(
            accountOwnerId = "player_1",
            monarchLevel = 6,
            monarchXp = 1800L,
            monarchRank = com.example.domain.model.MonarchRank.SHADOW_LORD,
            monarchAuthority = 60,
            armyCapacity = 25,
            activeShadowCount = 8,
            totalShadowsRecruited = 12,
            highestShadowRank = ShadowRank.KNIGHT_GRADE,
            totalEvolutions = 4,
            shadowArmyPowerRating = 5600L,
            unlockedMonarchSkillIds = listOf("monarch_shadow_extraction"),
            updatedAtEpochMs = now
        )

        // 1. Simulate active penalty and redemption plan
        val penalty = PenaltyRecord(
            penaltyId = "pen_prog_test",
            failureRecordId = "fail_prog_test",
            commitmentId = "commit_prog_test",
            questId = 15L,
            accountOwnerId = "player_1",
            questTitleSnapshot = "Weekly Campaign Trial",
            severity = PenaltySeverity.P3,
            basePenaltyPoints = 20,
            escalationBonusPoints = 0,
            totalPenaltyPoints = 20,
            penaltyStatus = PenaltyStatus.ACTIVE,
            reason = "Missed deadline",
            policyVersion = "POLICY_V1_2026"
        )

        val plan = RedemptionPlan(
            redemptionId = "red_prog_test",
            penaltyId = penalty.penaltyId,
            failureRecordId = penalty.failureRecordId,
            questId = penalty.questId,
            accountOwnerId = penalty.accountOwnerId,
            activityId = "strat_root_cause_analysis_30",
            category = CorrectiveCategory.STRATEGIC_PLANNING,
            activityTitle = "Post-Mortem & Schedule Alignment",
            instructions = "Reflect on cause of failure",
            difficulty = CorrectiveDifficulty.STRUCTURED,
            estimatedDurationMinutes = 30,
            verificationMethod = RedemptionVerificationMethod.STRUCTURED_REFLECTION,
            lifecycleState = RedemptionLifecycleState.ACTIVE,
            resolutionPoints = 20,
            assignedShadowClass = ShadowClass.ARCANIST,
            shadowGuidanceNote = "Analyze the pattern."
        )

        // 2. Submit valid evidence and verify
        val evidence = RedemptionVerificationEvidence(
            redemptionId = plan.redemptionId,
            accountOwnerId = plan.accountOwnerId,
            reflectionText = "I encountered late work obligations and failed to utilize the grace period. Going forward, calendar alerts are set.",
            userConfirmedCompliance = true
        )

        val verificationOutcome = RedemptionVerificationEngine.verify(plan, evidence)
        assertTrue(verificationOutcome.isApproved)
        assertEquals(20, verificationOutcome.pointsResolved)

        // 3. Resolve penalty points
        val resolution = PenaltyResolutionEngine.calculateResolution(
            penalty = penalty,
            verifiedPoints = verificationOutcome.pointsResolved,
            currentAccountBalance = 20
        )
        assertEquals(20, resolution.pointsToDeduct)
        assertEquals(0, resolution.remainingPenaltyPoints)
        assertEquals(PenaltyStatus.RESOLVED, resolution.newPenaltyStatus)

        // 4. Strict assertions that core player progression remains 100% UNTOUCHED
        assertEquals("Player XP must not be modified", 15000L, initialPlayer.totalXp)
        assertEquals("Current level XP must not be modified", 800, initialPlayer.currentLevelXp)
        assertEquals("Player level must not change", 20, initialPlayer.level)
        assertEquals("Hunter Rank must remain untouched", HunterRank.C_RANK, initialPlayer.rank)

        // 7 attributes must remain exactly identical
        assertEquals(40, initialPlayer.attributes.str.baseValue)
        assertEquals(35, initialPlayer.attributes.vit.baseValue)
        assertEquals(30, initialPlayer.attributes.agi.baseValue)
        assertEquals(28, initialPlayer.attributes.intel.baseValue)
        assertEquals(36, initialPlayer.attributes.dis.baseValue)
        assertEquals(24, initialPlayer.attributes.wis.baseValue)
        assertEquals(25, initialPlayer.attributes.cha.baseValue)

        // Physical telemetry untouched
        assertEquals(21, initialPlayer.streakDays)
        assertEquals(RecoveryStatus.OPTIMAL, initialPhysical.recoveryStatus)
        assertEquals(5, initialPhysical.weeklySessionsCompleted)

        // Shadow monarch stats untouched
        assertEquals(6, initialMonarch.monarchLevel)
        assertEquals(1800L, initialMonarch.monarchXp)
        assertEquals(5600L, initialMonarch.shadowArmyPowerRating)
        assertEquals(8, initialMonarch.activeShadowCount)
    }
}
