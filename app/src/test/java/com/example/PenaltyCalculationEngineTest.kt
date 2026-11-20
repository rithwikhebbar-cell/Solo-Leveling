package com.example

import com.example.domain.engine.PenaltyCalculationEngine
import com.example.domain.model.AttributeType
import com.example.domain.model.CommitmentState
import com.example.domain.model.ConfirmedFailureRecord
import com.example.domain.model.DefaultPenaltyPolicy
import com.example.domain.model.PenaltySeverity
import com.example.domain.model.Quest
import com.example.domain.model.QuestCategory
import com.example.domain.model.QuestCommitment
import com.example.domain.model.QuestType
import com.example.domain.model.VerificationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PenaltyCalculationEngineTest {

    private val now = 1_700_000_000_000L

    private fun createQuest(
        id: Long = 1L,
        title: String = "Test Quest",
        type: QuestType = QuestType.DAILY,
        category: QuestCategory = QuestCategory.PHYSICAL,
        targetValue: Int = 30,
        xpReward: Int = 45,
        isCompleted: Boolean = false
    ) = Quest(
        id = id,
        title = title,
        description = "Test Description",
        type = type,
        category = category,
        targetValue = targetValue,
        currentValue = if (isCompleted) targetValue else 0,
        unit = "min",
        xpReward = xpReward,
        primaryAttribute = AttributeType.VIT,
        primaryAttrXpReward = 40,
        isCompleted = isCompleted,
        verificationStatus = VerificationStatus.SELF_REPORTED,
        createdAtEpochMs = now - 86_400_000L
    )

    private fun createCommitment(
        questId: Long = 1L,
        state: CommitmentState = CommitmentState.ACCEPTED,
        rescheduleCount: Int = 0
    ) = QuestCommitment(
        commitmentId = "commit_${questId}_cycle_100",
        questId = questId,
        accountOwnerId = "player_1",
        logicalCycleId = "cycle_100",
        state = state,
        acceptedAtEpochMs = now - 3600_000L,
        deadlineEpochMs = now - 1800_000L,
        gracePeriodMinutes = 30,
        rescheduleCount = rescheduleCount
    )

    private fun createFailureRecord(
        questId: Long = 1L,
        commitmentId: String = "commit_${questId}_cycle_100",
        exemptionEvaluated: Boolean = false
    ) = ConfirmedFailureRecord(
        failureId = "fail_${questId}_cycle_100",
        commitmentId = commitmentId,
        questId = questId,
        logicalCycleId = "cycle_100",
        accountOwnerId = "player_1",
        deadlineEpochMs = now - 1800_000L,
        gracePeriodExpiresAtEpochMs = now,
        confirmedAtEpochMs = now,
        failureReasonCode = "GRACE_PERIOD_EXPIRED",
        policyVersion = 1,
        exemptionEvaluated = exemptionEvaluated
    )

    // ========================================================================
    // 1. SEVERITY TIERS EVALUATION
    // ========================================================================

    @Test
    fun minorCommitment_evaluatesToP1Severity() {
        val quest = createQuest(targetValue = 15, xpReward = 25, type = QuestType.DAILY)
        val commitment = createCommitment(questId = quest.id)
        val failure = createFailureRecord(questId = quest.id)

        val result = PenaltyCalculationEngine.calculate(failure, commitment, quest)
        assertEquals(PenaltySeverity.P1, result.severity)
        assertEquals(5, result.basePenaltyPoints)
        assertEquals(0, result.escalationBonusPoints)
        assertEquals(5, result.totalPenaltyPoints)
        assertEquals(DefaultPenaltyPolicy.policyVersion, result.policyVersion)
    }

    @Test
    fun standardDailyCommitment_evaluatesToP2Severity() {
        val quest = createQuest(targetValue = 30, xpReward = 45, type = QuestType.DAILY)
        val commitment = createCommitment(questId = quest.id)
        val failure = createFailureRecord(questId = quest.id)

        val result = PenaltyCalculationEngine.calculate(failure, commitment, quest)
        assertEquals(PenaltySeverity.P2, result.severity)
        assertEquals(10, result.basePenaltyPoints)
        assertEquals(0, result.escalationBonusPoints)
        assertEquals(10, result.totalPenaltyPoints)
    }

    @Test
    fun weeklyHighEffortCommitment_evaluatesToP3Severity() {
        val quest = createQuest(targetValue = 150, xpReward = 120, type = QuestType.WEEKLY)
        val commitment = createCommitment(questId = quest.id)
        val failure = createFailureRecord(questId = quest.id)

        val result = PenaltyCalculationEngine.calculate(failure, commitment, quest)
        assertEquals(PenaltySeverity.P3, result.severity)
        assertEquals(20, result.basePenaltyPoints)
        assertEquals(0, result.escalationBonusPoints)
        assertEquals(20, result.totalPenaltyPoints)
    }

    @Test
    fun longTermMilestoneCommitment_evaluatesToP4Severity() {
        val quest = createQuest(targetValue = 5, xpReward = 200, type = QuestType.LONG_TERM)
        val commitment = createCommitment(questId = quest.id)
        val failure = createFailureRecord(questId = quest.id)

        val result = PenaltyCalculationEngine.calculate(failure, commitment, quest)
        assertEquals(PenaltySeverity.P4, result.severity)
        assertEquals(30, result.basePenaltyPoints)
        assertEquals(0, result.escalationBonusPoints)
        assertEquals(30, result.totalPenaltyPoints)
    }

    @Test
    fun repeatedRescheduledMinorCommitment_escalatesBaseSeverityToP2() {
        // Minor quest normally P1, but player rescheduled twice and still failed
        val quest = createQuest(targetValue = 15, xpReward = 20, type = QuestType.DAILY)
        val commitment = createCommitment(questId = quest.id, rescheduleCount = 2)
        val failure = createFailureRecord(questId = quest.id)

        val result = PenaltyCalculationEngine.calculate(failure, commitment, quest)
        assertEquals(PenaltySeverity.P2, result.severity)
        assertEquals(10, result.basePenaltyPoints)
    }

    // ========================================================================
    // 2. EXCLUSIONS & ELIGIBILITY GUARDS
    // ========================================================================

    @Test
    fun unacceptedCommitment_isNotEligible() {
        val quest = createQuest()
        val commitment = createCommitment(state = CommitmentState.NOT_ACCEPTED)
        val failure = createFailureRecord()

        val eligibility = PenaltyCalculationEngine.checkEligibility(commitment, quest, failure)
        assertTrue(eligibility is PenaltyCalculationEngine.EligibilityResult.Ineligible)
    }

    @Test
    fun completedQuest_isNotEligible() {
        val quest = createQuest(isCompleted = true)
        val commitment = createCommitment()
        val failure = createFailureRecord()

        val eligibility = PenaltyCalculationEngine.checkEligibility(commitment, quest, failure)
        assertTrue(eligibility is PenaltyCalculationEngine.EligibilityResult.Ineligible)
    }

    @Test
    fun approvedExemption_isNotEligible() {
        val quest = createQuest()
        val commitment = createCommitment(state = CommitmentState.EXEMPTED)
        val failure = createFailureRecord(exemptionEvaluated = true)

        val eligibility = PenaltyCalculationEngine.checkEligibility(commitment, quest, failure)
        assertTrue(eligibility is PenaltyCalculationEngine.EligibilityResult.Ineligible)
    }

    @Test
    fun pendingExemptionReview_isProtectedFromPenalty() {
        val quest = createQuest()
        val commitment = createCommitment(state = CommitmentState.PENDING_REVIEW)
        val failure = createFailureRecord()

        val eligibility = PenaltyCalculationEngine.checkEligibility(commitment, quest, failure)
        assertTrue(eligibility is PenaltyCalculationEngine.EligibilityResult.Ineligible)
    }

    @Test
    fun cancelledCommitment_isNotEligible() {
        val quest = createQuest()
        val commitment = createCommitment(state = CommitmentState.CANCELLED)
        val failure = createFailureRecord()

        val eligibility = PenaltyCalculationEngine.checkEligibility(commitment, quest, failure)
        assertTrue(eligibility is PenaltyCalculationEngine.EligibilityResult.Ineligible)
    }
}
