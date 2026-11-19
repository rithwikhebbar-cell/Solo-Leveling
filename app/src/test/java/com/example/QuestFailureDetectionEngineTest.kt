package com.example

import com.example.domain.engine.DeadlineCalculator
import com.example.domain.engine.GracePeriodEngine
import com.example.domain.engine.QuestFailureDetectionEngine
import com.example.domain.model.Clock
import com.example.domain.model.CommitmentState
import com.example.domain.model.ExemptionLifecycleState
import com.example.domain.model.ExemptionReasonCategory
import com.example.domain.model.FailureDecisionOutcome
import com.example.domain.model.GracePeriodPhase
import com.example.domain.model.QuestCommitment
import com.example.domain.model.QuestExemption
import com.example.domain.model.QuestType
import com.example.domain.model.RecoveryState
import com.example.domain.model.VerificationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class QuestFailureDetectionEngineTest {

    private val baseDeadline = 1_700_000_000_000L
    private val graceMinutes = 30
    private val graceExpiry = baseDeadline + (graceMinutes * 60_000L)

    private fun testCommitment(
        state: CommitmentState = CommitmentState.ACCEPTED,
        deadline: Long = baseDeadline,
        graceMins: Int = graceMinutes
    ): QuestCommitment = QuestCommitment(
        commitmentId = "commit_test_1",
        questId = 1L,
        accountOwnerId = "test_player",
        logicalCycleId = "cycle_200",
        state = state,
        acceptedAtEpochMs = baseDeadline - 3600_000L,
        deadlineEpochMs = deadline,
        gracePeriodMinutes = graceMins,
        gracePeriodExpiresAtEpochMs = deadline + (graceMins * 60_000L)
    )

    @Test
    fun unacceptedCommitment_neverFails() {
        val commitment = testCommitment(state = CommitmentState.NOT_ACCEPTED)
        val testClock = Clock { graceExpiry + 10_000L } // Way past grace period

        val result = QuestFailureDetectionEngine.evaluateCommitment(
            commitment = commitment,
            isQuestCompleted = false,
            verificationStatus = VerificationStatus.SELF_REPORTED,
            clock = testClock
        )

        assertEquals(FailureDecisionOutcome.NOT_ELIGIBLE, result.decision)
        assertEquals("UNACCEPTED_COMMITMENT", result.reasonCode)
    }

    @Test
    fun cancelledCommitment_neverFails() {
        val commitment = testCommitment(state = CommitmentState.CANCELLED)
        val testClock = Clock { graceExpiry + 10_000L }

        val result = QuestFailureDetectionEngine.evaluateCommitment(
            commitment = commitment,
            isQuestCompleted = false,
            verificationStatus = VerificationStatus.SELF_REPORTED,
            clock = testClock
        )

        assertEquals(FailureDecisionOutcome.CANCELLED, result.decision)
        assertEquals("COMMITMENT_CANCELLED", result.reasonCode)
    }

    @Test
    fun completedQuest_neverFails() {
        val commitment = testCommitment(state = CommitmentState.ACCEPTED)
        val testClock = Clock { graceExpiry + 100_000L }

        val result = QuestFailureDetectionEngine.evaluateCommitment(
            commitment = commitment,
            isQuestCompleted = true, // Quest marked completed
            verificationStatus = VerificationStatus.VERIFIED_SYSTEM_TIMER,
            clock = testClock
        )

        assertEquals(FailureDecisionOutcome.COMPLETED, result.decision)
        assertEquals("QUEST_COMPLETED_SUCCESSFULLY", result.reasonCode)
    }

    @Test
    fun beforeDeadline_isNotYetOverdue() {
        val commitment = testCommitment(state = CommitmentState.ACCEPTED)
        val testClock = Clock { baseDeadline - 1000L } // 1 second before deadline

        val result = QuestFailureDetectionEngine.evaluateCommitment(
            commitment = commitment,
            isQuestCompleted = false,
            verificationStatus = VerificationStatus.SELF_REPORTED,
            clock = testClock
        )

        assertEquals(FailureDecisionOutcome.NOT_YET_OVERDUE, result.decision)
        assertEquals("WITHIN_SCHEDULED_DEADLINE", result.reasonCode)
    }

    @Test
    fun exactDeadline_entersGracePeriodActive() {
        val commitment = testCommitment(state = CommitmentState.ACCEPTED)
        val testClock = Clock { baseDeadline } // Exactly at deadline

        val result = QuestFailureDetectionEngine.evaluateCommitment(
            commitment = commitment,
            isQuestCompleted = false,
            verificationStatus = VerificationStatus.SELF_REPORTED,
            clock = testClock
        )

        assertEquals(FailureDecisionOutcome.GRACE_PERIOD_ACTIVE, result.decision)
        assertEquals("IN_GRACE_PERIOD", result.reasonCode)
    }

    @Test
    fun insideGracePeriod_doesNotConfirmFailure() {
        val commitment = testCommitment(state = CommitmentState.ACCEPTED)
        val testClock = Clock { baseDeadline + (15 * 60_000L) } // 15 mins into 30m grace

        val result = QuestFailureDetectionEngine.evaluateCommitment(
            commitment = commitment,
            isQuestCompleted = false,
            verificationStatus = VerificationStatus.SELF_REPORTED,
            clock = testClock
        )

        assertEquals(FailureDecisionOutcome.GRACE_PERIOD_ACTIVE, result.decision)
        assertEquals("IN_GRACE_PERIOD", result.reasonCode)
    }

    @Test
    fun exactGracePeriodExpiry_confirmsFailure() {
        val commitment = testCommitment(state = CommitmentState.ACCEPTED)
        val testClock = Clock { graceExpiry } // Exactly at grace expiry

        val result = QuestFailureDetectionEngine.evaluateCommitment(
            commitment = commitment,
            isQuestCompleted = false,
            verificationStatus = VerificationStatus.SELF_REPORTED,
            clock = testClock
        )

        assertEquals(FailureDecisionOutcome.FAILURE_CONFIRMED, result.decision)
        assertEquals("DEADLINE_AND_GRACE_EXPIRED", result.reasonCode)
    }

    @Test
    fun afterGraceExpiry_confirmsFailure() {
        val commitment = testCommitment(state = CommitmentState.ACCEPTED)
        val testClock = Clock { graceExpiry + 5000L }

        val result = QuestFailureDetectionEngine.evaluateCommitment(
            commitment = commitment,
            isQuestCompleted = false,
            verificationStatus = VerificationStatus.SELF_REPORTED,
            clock = testClock
        )

        assertEquals(FailureDecisionOutcome.FAILURE_CONFIRMED, result.decision)
        assertEquals("DEADLINE_AND_GRACE_EXPIRED", result.reasonCode)
    }

    @Test
    fun approvedExemption_preventsFailureEvenAfterGraceExpiry() {
        val commitment = testCommitment(state = CommitmentState.ACCEPTED)
        val exemption = QuestExemption(
            exemptionId = "ex_101",
            commitmentId = commitment.commitmentId,
            questId = commitment.questId,
            accountOwnerId = commitment.accountOwnerId,
            category = ExemptionReasonCategory.ILLNESS,
            lifecycleState = ExemptionLifecycleState.APPROVED,
            explanation = "Sudden fever"
        )
        val testClock = Clock { graceExpiry + 10_000L }

        val result = QuestFailureDetectionEngine.evaluateCommitment(
            commitment = commitment,
            isQuestCompleted = false,
            verificationStatus = VerificationStatus.SELF_REPORTED,
            activeExemption = exemption,
            clock = testClock
        )

        assertEquals(FailureDecisionOutcome.EXEMPTED, result.decision)
        assertEquals("EXEMPTION_APPROVED_ILLNESS", result.reasonCode)
        assertEquals("ex_101", result.exemptionId)
    }

    @Test
    fun pendingExemptionReview_pausesFailureConfirmation() {
        val commitment = testCommitment(state = CommitmentState.ACCEPTED)
        val exemption = QuestExemption(
            exemptionId = "ex_102",
            commitmentId = commitment.commitmentId,
            questId = commitment.questId,
            accountOwnerId = commitment.accountOwnerId,
            category = ExemptionReasonCategory.EMERGENCY,
            lifecycleState = ExemptionLifecycleState.PENDING_REVIEW,
            explanation = "Power outage"
        )
        val testClock = Clock { graceExpiry + 20_000L }

        val result = QuestFailureDetectionEngine.evaluateCommitment(
            commitment = commitment,
            isQuestCompleted = false,
            verificationStatus = VerificationStatus.SELF_REPORTED,
            activeExemption = exemption,
            clock = testClock
        )

        assertEquals(FailureDecisionOutcome.PENDING_EXEMPTION_REVIEW, result.decision)
        assertEquals("EXEMPTION_REVIEW_PENDING", result.reasonCode)
    }

    @Test
    fun deniedExemption_resumesNormalFailureConfirmation() {
        val commitment = testCommitment(state = CommitmentState.ACCEPTED)
        val exemption = QuestExemption(
            exemptionId = "ex_103",
            commitmentId = commitment.commitmentId,
            questId = commitment.questId,
            accountOwnerId = commitment.accountOwnerId,
            category = ExemptionReasonCategory.EMERGENCY,
            lifecycleState = ExemptionLifecycleState.DENIED,
            explanation = "Unsubstantiated"
        )
        val testClock = Clock { graceExpiry + 1000L }

        val result = QuestFailureDetectionEngine.evaluateCommitment(
            commitment = commitment,
            isQuestCompleted = false,
            verificationStatus = VerificationStatus.SELF_REPORTED,
            activeExemption = exemption,
            clock = testClock
        )

        assertEquals(FailureDecisionOutcome.FAILURE_CONFIRMED, result.decision)
        assertEquals("DEADLINE_AND_GRACE_EXPIRED", result.reasonCode)
    }

    @Test
    fun recoveryRestriction_protectsPhysicalQuest() {
        val commitment = testCommitment(state = CommitmentState.ACCEPTED)
        val testClock = Clock { graceExpiry + 50_000L }

        // Rest recommended protects physical quest
        val resultPhysical = QuestFailureDetectionEngine.evaluateCommitment(
            commitment = commitment,
            isQuestCompleted = false,
            verificationStatus = VerificationStatus.SELF_REPORTED,
            recoveryState = RecoveryState.REST_RECOMMENDED,
            isPhysicalQuest = true,
            clock = testClock
        )
        assertEquals(FailureDecisionOutcome.EXEMPTED, resultPhysical.decision)
        assertTrue(resultPhysical.recoveryProtected)

        // Non-physical quest is NOT protected by physical recovery state
        val resultAcademic = QuestFailureDetectionEngine.evaluateCommitment(
            commitment = commitment,
            isQuestCompleted = false,
            verificationStatus = VerificationStatus.SELF_REPORTED,
            recoveryState = RecoveryState.REST_RECOMMENDED,
            isPhysicalQuest = false,
            clock = testClock
        )
        assertEquals(FailureDecisionOutcome.FAILURE_CONFIRMED, resultAcademic.decision)
        assertFalse(resultAcademic.recoveryProtected)
    }

    @Test
    fun gracePeriodEngine_exactBoundaryCalculations() {
        val deadline = 10000L
        val graceMinutes = 30
        val graceExpiry = 10000L + (30 * 60_000L)

        assertEquals(GracePeriodPhase.BEFORE_DEADLINE, GracePeriodEngine.evaluatePhase(9999L, deadline, graceMinutes))
        assertEquals(GracePeriodPhase.GRACE_PERIOD_ACTIVE, GracePeriodEngine.evaluatePhase(10000L, deadline, graceMinutes))
        assertEquals(GracePeriodPhase.GRACE_PERIOD_ACTIVE, GracePeriodEngine.evaluatePhase(graceExpiry - 1L, deadline, graceMinutes))
        assertEquals(GracePeriodPhase.GRACE_PERIOD_EXPIRED, GracePeriodEngine.evaluatePhase(graceExpiry, deadline, graceMinutes))
        assertEquals(GracePeriodPhase.GRACE_PERIOD_EXPIRED, GracePeriodEngine.evaluatePhase(graceExpiry + 1L, deadline, graceMinutes))
    }

    @Test
    fun deadlineCalculator_dailyResetCalculation() {
        val zone = ZoneId.of("UTC")
        val epochDay = 20000L // arbitrary epoch day
        val deadlineMs = DeadlineCalculator.calculateDefaultDailyDeadline(epochDay, dailyResetHour = 4, zoneId = zone)

        // Target closing date is epochDay + 1 at 04:00 UTC
        val expectedDate = java.time.LocalDate.ofEpochDay(epochDay + 1)
        val expectedMs = expectedDate.atTime(4, 0).atZone(zone).toInstant().toEpochMilli()
        assertEquals(expectedMs, deadlineMs)
    }
}
