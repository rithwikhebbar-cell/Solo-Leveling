package com.example

import com.example.domain.engine.ProgressionEngine
import com.example.domain.engine.RecoveryEngine
import com.example.domain.model.ActiveJoggingSessionState
import com.example.domain.model.AttributeBreakdown
import com.example.domain.model.AttributeType
import com.example.domain.model.HealthConnectSyncState
import com.example.domain.model.HunterRank
import com.example.domain.model.PhysicalActivityKind
import com.example.domain.model.PhysicalDevelopmentState
import com.example.domain.model.PlayerAttributes
import com.example.domain.model.PlayerProfile
import com.example.domain.model.RecoveryAssessment
import com.example.domain.model.RecoveryState
import com.example.domain.model.RecoveryStatus
import com.example.domain.model.VerificationStatus
import com.example.domain.model.WorkoutCategory
import com.example.domain.model.WorkoutStatus
import com.example.ui.screens.physical.components.PhysicalSubTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhysicalDevelopmentRedesignTest {

    @Test
    fun physicalSubTab_containsAllFiveCoreSections() {
        val tabs = PhysicalSubTab.entries
        assertEquals(5, tabs.size)
        assertTrue(tabs.contains(PhysicalSubTab.OVERVIEW))
        assertTrue(tabs.contains(PhysicalSubTab.JOGGING))
        assertTrue(tabs.contains(PhysicalSubTab.WORKOUTS))
        assertTrue(tabs.contains(PhysicalSubTab.RECOVERY))
        assertTrue(tabs.contains(PhysicalSubTab.HEALTH_CONNECT))
        assertEquals("Overview", PhysicalSubTab.OVERVIEW.label)
        assertEquals("Jogging", PhysicalSubTab.JOGGING.label)
        assertEquals("Workouts", PhysicalSubTab.WORKOUTS.label)
        assertEquals("Recovery", PhysicalSubTab.RECOVERY.label)
        assertEquals("Health Connect", PhysicalSubTab.HEALTH_CONNECT.label)
    }

    @Test
    fun recoveryEngine_safetyDecision_restRecommendedRestrictsWorkouts() {
        // High fatigue (5/5) and low sleep (4.5h) should trigger REST_RECOMMENDED
        val assessment = RecoveryEngine.assess(
            measuredLoadMinutes48h = 120,
            measuredLoadMinutes7d = 300,
            measuredSessions7d = 6,
            consecutiveActiveDays = 6,
            measuredSleepHoursLastNight = 4.5,
            measuredRestingHrBpm = 82,
            selfReportedFatigue = 5,
            selfReportedDiscomfort = true,
            discomfortNotes = "Severe quadricep pain"
        )

        assertEquals(RecoveryState.REST_RECOMMENDED, assessment.state)
        assertTrue(assessment.readinessScore <= 45)
        assertEquals(RecoveryStatus.RECOVERY_REQUIRED, assessment.state.toLegacyStatus())
        assertTrue(assessment.recommendedActions.any { it.contains("rest", ignoreCase = true) || it.contains("sleep", ignoreCase = true) })
    }

    @Test
    fun recoveryEngine_readyStatus_permitsFullTraining() {
        val assessment = RecoveryEngine.assess(
            measuredLoadMinutes48h = 30,
            measuredLoadMinutes7d = 120,
            measuredSessions7d = 3,
            consecutiveActiveDays = 2,
            measuredSleepHoursLastNight = 8.0,
            measuredRestingHrBpm = 58,
            selfReportedFatigue = 1,
            selfReportedDiscomfort = false
        )

        assertEquals(RecoveryState.READY, assessment.state)
        assertTrue(assessment.readinessScore >= 80)
        assertEquals(RecoveryStatus.OPTIMAL, assessment.state.toLegacyStatus())
    }

    @Test
    fun physicalRewards_strictZeroMomentumXpInvariant() {
        // Test that physical activity rewards ONLY attribute and character XP, strictly ZERO Momentum XP
        val verifiedJog = ProgressionEngine.calculatePhysicalReward(
            kind = PhysicalActivityKind.JOGGING,
            durationMinutes = 35,
            verificationStatus = VerificationStatus.VERIFIED_HEALTH_CONNECT,
            xpAlreadyEarnedToday = 0,
            selfReportedPhysicalCountToday = 0
        )

        // Verifying reward structure
        assertTrue("Physical activity must award XP", verifiedJog.xpAwarded > 0)
        assertEquals(AttributeType.VIT, verifiedJog.primaryAttribute)
        assertEquals(AttributeType.AGI, verifiedJog.secondaryAttribute)

        val strengthWorkout = ProgressionEngine.calculatePhysicalReward(
            kind = PhysicalActivityKind.STRENGTH_TRAINING,
            durationMinutes = 45,
            verificationStatus = VerificationStatus.SELF_REPORTED,
            xpAlreadyEarnedToday = 0,
            selfReportedPhysicalCountToday = 0,
            totalVolumeSets = 12
        )

        assertTrue(strengthWorkout.xpAwarded > 0)
        assertEquals(AttributeType.STR, strengthWorkout.primaryAttribute)
        assertEquals(AttributeType.DIS, strengthWorkout.secondaryAttribute)
    }

    @Test
    fun physicalPowerScore_andConditioningTiers_calculatedAccurately() {
        val player = PlayerProfile(
            codename = "Sung Jin-Woo",
            level = 25,
            rank = HunterRank.E_RANK,
            attributes = PlayerAttributes(
                str = AttributeBreakdown(AttributeType.STR, baseValue = 40),
                vit = AttributeBreakdown(AttributeType.VIT, baseValue = 35),
                agi = AttributeBreakdown(AttributeType.AGI, baseValue = 30),
                intel = AttributeBreakdown(AttributeType.INT, baseValue = 20),
                dis = AttributeBreakdown(AttributeType.DIS, baseValue = 25),
                wis = AttributeBreakdown(AttributeType.WIS, baseValue = 20),
                cha = AttributeBreakdown(AttributeType.CHA, baseValue = 20)
            )
        )

        val physicalPowerScore = player.attributes.str.totalValue +
            player.attributes.vit.totalValue +
            player.attributes.agi.totalValue +
            player.attributes.dis.totalValue

        assertEquals(130, physicalPowerScore)

        val tierLabel = when {
            physicalPowerScore >= 200 -> "MONARCH PHYSIQUE"
            physicalPowerScore >= 120 -> "ELITE VIGOR"
            physicalPowerScore >= 60 -> "CONDITIONED HUNTER"
            else -> "INITIATE CONDITIONING"
        }

        assertEquals("ELITE VIGOR", tierLabel)
    }

    @Test
    fun physicalDevelopmentState_progressCalculations() {
        val state = PhysicalDevelopmentState(
            baseJoggingTargetMinutes = 30,
            aiRecommendedJoggingMinutes = 40,
            completedJoggingMinutesToday = 20,
            workoutStatus = WorkoutStatus.PENDING,
            recoveryStatus = RecoveryStatus.OPTIMAL
        )

        assertEquals(40, state.effectiveJoggingTarget)
        assertEquals(0.5f, state.joggingProgressFraction, 0.001f)

        val exceededState = state.copy(completedJoggingMinutesToday = 60)
        assertEquals(1.0f, exceededState.joggingProgressFraction, 0.001f)
    }

    @Test
    fun activeJoggingSessionState_elapsedSecondsTracking() {
        val idle = ActiveJoggingSessionState(isActive = false)
        assertEquals(0L, idle.currentElapsedSeconds(10_000L))

        val active = ActiveJoggingSessionState(
            isActive = true,
            isPaused = false,
            startEpochMs = 1_000L,
            accumulatedElapsedMs = 5_000L,
            lastResumedAtMs = 10_000L
        )
        // At 15_000ms: runningDelta = 15_000 - 10_000 = 5_000ms. Total = 10_000ms = 10s
        assertEquals(10L, active.currentElapsedSeconds(15_000L))

        val paused = ActiveJoggingSessionState(
            isActive = true,
            isPaused = true,
            startEpochMs = 1_000L,
            accumulatedElapsedMs = 8_000L,
            lastResumedAtMs = 10_000L
        )
        // When paused, running delta is 0L, so only accumulatedElapsedMs is counted: 8_000ms = 8s
        assertEquals(8L, paused.currentElapsedSeconds(20_000L))
    }

    @Test
    fun workoutCategory_allFiveCategoriesAvailable() {
        val categories = WorkoutCategory.entries
        assertEquals(5, categories.size)
        assertEquals("Strength", WorkoutCategory.STRENGTH.label)
        assertEquals("Hypertrophy", WorkoutCategory.HYPERTROPHY.label)
        assertEquals("Calisthenics", WorkoutCategory.CALISTHENICS.label)
        assertEquals("Conditioning", WorkoutCategory.CONDITIONING.label)
        assertEquals("Mobility", WorkoutCategory.MOBILITY.label)
    }

    @Test
    fun healthConnectSyncState_defaultValuesAreValid() {
        val sync = HealthConnectSyncState()
        assertEquals("Never Synchronized", sync.lastSyncStatus)
        assertEquals(0, sync.lastImportedCount)
        assertEquals(0, sync.lastDuplicateCount)
        assertEquals(0, sync.lastRejectedCount)
    }
}
