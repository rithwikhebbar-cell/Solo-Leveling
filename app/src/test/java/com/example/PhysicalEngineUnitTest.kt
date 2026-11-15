package com.example

import com.example.domain.engine.ProgressionEngine
import com.example.domain.engine.RecoveryEngine
import com.example.domain.model.AttributeType
import com.example.domain.model.PhysicalActivityKind
import com.example.domain.model.RecoveryState
import com.example.domain.model.VerificationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhysicalEngineUnitTest {

    @Test
    fun validateSession_rejectsInvalidInputs() {
        val now = 1_700_000_000_000L

        assertFalse(ProgressionEngine.validateSession("", now - 1800_000L, now, 30, null, now).isValid)
        assertFalse(ProgressionEngine.validateSession("Run", now, now, 0, null, now).isValid)
        assertFalse(ProgressionEngine.validateSession("Run", now, now - 1000L, 10, null, now).isValid)
        assertFalse(ProgressionEngine.validateSession("Run", now - 400 * 60_000L, now, 400, null, now).isValid)
        assertFalse(ProgressionEngine.validateSession("Run", now, now + 30 * 60_000L, 30, null, now).isValid)
        assertFalse(ProgressionEngine.validateSession("Run", now - 1800_000L, now, 30, -50.0, now).isValid)

        assertTrue(ProgressionEngine.validateSession("Morning Jog", now - 1800_000L, now, 30, 4200.0, now).isValid)
    }

    @Test
    fun calculatePhysicalReward_mapsAttributesAndEnforcesDailyCaps() {
        val jogVerified = ProgressionEngine.calculatePhysicalReward(
            kind = PhysicalActivityKind.JOGGING,
            durationMinutes = 30,
            verificationStatus = VerificationStatus.VERIFIED_HEALTH_CONNECT,
            xpAlreadyEarnedToday = 0,
            selfReportedPhysicalCountToday = 0
        )
        assertEquals(AttributeType.VIT, jogVerified.primaryAttribute)
        assertEquals(AttributeType.AGI, jogVerified.secondaryAttribute)
        assertEquals(72, jogVerified.xpAwarded)
        assertFalse(jogVerified.wasCapped)

        val strengthSelf = ProgressionEngine.calculatePhysicalReward(
            kind = PhysicalActivityKind.STRENGTH_TRAINING,
            durationMinutes = 40,
            verificationStatus = VerificationStatus.SELF_REPORTED,
            xpAlreadyEarnedToday = 0,
            selfReportedPhysicalCountToday = 0,
            totalVolumeSets = 10
        )
        assertEquals(AttributeType.STR, strengthSelf.primaryAttribute)
        assertEquals(AttributeType.DIS, strengthSelf.secondaryAttribute)
        assertTrue(strengthSelf.xpAwarded > 0)

        // Exceeding daily self-reported limit yields 0 XP
        val cappedSelf = ProgressionEngine.calculatePhysicalReward(
            kind = PhysicalActivityKind.JOGGING,
            durationMinutes = 30,
            verificationStatus = VerificationStatus.SELF_REPORTED,
            xpAlreadyEarnedToday = 80,
            selfReportedPhysicalCountToday = ProgressionEngine.DAILY_SELF_REPORTED_PHYSICAL_LIMIT
        )
        assertEquals(0, cappedSelf.xpAwarded)
        assertTrue(cappedSelf.wasCapped)

        // Approaching daily total XP cap clamps rewarded XP
        val clampedTotal = ProgressionEngine.calculatePhysicalReward(
            kind = PhysicalActivityKind.RUNNING,
            durationMinutes = 30,
            verificationStatus = VerificationStatus.VERIFIED_HEALTH_CONNECT,
            xpAlreadyEarnedToday = ProgressionEngine.DAILY_PHYSICAL_XP_CAP - 15,
            selfReportedPhysicalCountToday = 0
        )
        assertEquals(15, clampedTotal.xpAwarded)
        assertTrue(clampedTotal.wasCapped)
    }

    @Test
    fun recoveryEngine_evaluatesAllFourStatesAndRespectsDiscomfort() {
        val ready = RecoveryEngine.assess(
            measuredLoadMinutes48h = 30,
            measuredLoadMinutes7d = 90,
            measuredSessions7d = 3,
            consecutiveActiveDays = 1,
            measuredSleepHoursLastNight = 7.8,
            selfReportedFatigue = 1,
            selfReportedDiscomfort = false
        )
        assertEquals(RecoveryState.READY, ready.state)

        val moderate = RecoveryEngine.assess(
            measuredLoadMinutes48h = 95,
            measuredLoadMinutes7d = 210,
            measuredSessions7d = 5,
            consecutiveActiveDays = 4,
            measuredSleepHoursLastNight = 7.0
        )
        assertEquals(RecoveryState.MODERATE, moderate.state)

        val recoveryRecommended = RecoveryEngine.assess(
            measuredLoadMinutes48h = 120,
            measuredLoadMinutes7d = 370,
            measuredSessions7d = 6,
            consecutiveActiveDays = 4,
            measuredSleepHoursLastNight = 7.2
        )
        assertEquals(RecoveryState.RECOVERY_RECOMMENDED, recoveryRecommended.state)

        val restDueToDiscomfort = RecoveryEngine.assess(
            measuredLoadMinutes48h = 20,
            measuredLoadMinutes7d = 40,
            measuredSessions7d = 2,
            consecutiveActiveDays = 1,
            selfReportedFatigue = 2,
            selfReportedDiscomfort = true,
            discomfortNotes = "Right knee soreness"
        )
        assertEquals(RecoveryState.REST_RECOMMENDED, restDueToDiscomfort.state)
        assertTrue(restDueToDiscomfort.readinessScore <= 35)
    }

    @Test
    fun joggingTargetProposal_adjustsSafelyForRecoveryAndConsistency() {
        val restProposal = RecoveryEngine.proposeJoggingTargetAdjustment(
            currentTargetMinutes = 30,
            baseTargetMinutes = 30,
            recoveryState = RecoveryState.REST_RECOMMENDED,
            weeklyJoggingDays = 3
        )
        assertNotNull(restProposal)
        assertEquals(10, restProposal!!.proposedMinutes)

        val progressionProposal = RecoveryEngine.proposeJoggingTargetAdjustment(
            currentTargetMinutes = 30,
            baseTargetMinutes = 30,
            recoveryState = RecoveryState.READY,
            weeklyJoggingDays = 5
        )
        assertNotNull(progressionProposal)
        assertEquals(35, progressionProposal!!.proposedMinutes)

        val noChange = RecoveryEngine.proposeJoggingTargetAdjustment(
            currentTargetMinutes = 30,
            baseTargetMinutes = 30,
            recoveryState = RecoveryState.READY,
            weeklyJoggingDays = 2
        )
        assertNull(noChange)
    }
}
