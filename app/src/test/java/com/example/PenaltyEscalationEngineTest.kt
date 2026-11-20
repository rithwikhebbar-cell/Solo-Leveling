package com.example

import com.example.domain.engine.PenaltyEscalationEngine
import com.example.domain.model.ConfirmedFailureRecord
import com.example.domain.model.DefaultPenaltyPolicy
import com.example.domain.model.PenaltySeverity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PenaltyEscalationEngineTest {

    private val now = 1_700_000_000_000L
    private val oneDayMs = 86_400_000L

    private fun createFailure(
        id: String,
        commitmentId: String,
        timestampEpochMs: Long,
        exemptionEvaluated: Boolean = false
    ) = ConfirmedFailureRecord(
        failureId = id,
        commitmentId = commitmentId,
        questId = 1L,
        logicalCycleId = "cycle_$id",
        accountOwnerId = "player_1",
        deadlineEpochMs = timestampEpochMs - 1800_000L,
        gracePeriodExpiresAtEpochMs = timestampEpochMs,
        confirmedAtEpochMs = timestampEpochMs,
        failureReasonCode = "GRACE_PERIOD_EXPIRED",
        policyVersion = 1,
        exemptionEvaluated = exemptionEvaluated
    )

    @Test
    fun initialFailure_inRollingWindow_appliesBaselineWithoutEscalation() {
        val current = createFailure("fail_current", "commit_current", now)
        val history = emptyList<ConfirmedFailureRecord>()

        val escalation = PenaltyEscalationEngine.evaluate(
            baseSeverity = PenaltySeverity.P2,
            failureHistory = history,
            currentFailure = current,
            policy = DefaultPenaltyPolicy
        )

        assertEquals(0, escalation.eligibleFailuresInWindow)
        assertEquals(PenaltySeverity.P2, escalation.escalatedSeverity)
        assertEquals(0, escalation.escalationBonusPoints)
        assertTrue(escalation.reason.contains("Standard baseline severity"))
    }

    @Test
    fun singlePriorFailure_inRollingWindow_doesNotTriggerEscalation() {
        val current = createFailure("fail_current", "commit_current", now)
        val history = listOf(
            createFailure("fail_1", "commit_1", now - (2 * oneDayMs))
        )

        val escalation = PenaltyEscalationEngine.evaluate(
            baseSeverity = PenaltySeverity.P1,
            failureHistory = history,
            currentFailure = current,
            policy = DefaultPenaltyPolicy
        )

        assertEquals(1, escalation.eligibleFailuresInWindow)
        assertEquals(PenaltySeverity.P1, escalation.escalatedSeverity)
        assertEquals(0, escalation.escalationBonusPoints)
    }

    @Test
    fun twoPriorFailures_inRollingWindow_triggersSingleTierEscalationAndBonus() {
        val current = createFailure("fail_current", "commit_current", now)
        val history = listOf(
            createFailure("fail_1", "commit_1", now - (3 * oneDayMs)),
            createFailure("fail_2", "commit_2", now - (1 * oneDayMs))
        )

        val escalation = PenaltyEscalationEngine.evaluate(
            baseSeverity = PenaltySeverity.P2,
            failureHistory = history,
            currentFailure = current,
            policy = DefaultPenaltyPolicy
        )

        assertEquals(2, escalation.eligibleFailuresInWindow)
        assertEquals(PenaltySeverity.P3, escalation.escalatedSeverity) // P2 -> P3
        assertEquals(5, escalation.escalationBonusPoints)
        assertTrue(escalation.reason.contains("Escalation triggered"))
    }

    @Test
    fun threePriorFailures_inRollingWindow_triggersMultiTierEscalationCappedAtP4() {
        val current = createFailure("fail_current", "commit_current", now)
        val history = listOf(
            createFailure("fail_1", "commit_1", now - (5 * oneDayMs)),
            createFailure("fail_2", "commit_2", now - (3 * oneDayMs)),
            createFailure("fail_3", "commit_3", now - (1 * oneDayMs))
        )

        val escalation = PenaltyEscalationEngine.evaluate(
            baseSeverity = PenaltySeverity.P3,
            failureHistory = history,
            currentFailure = current,
            policy = DefaultPenaltyPolicy
        )

        assertEquals(3, escalation.eligibleFailuresInWindow)
        assertEquals(PenaltySeverity.P4, escalation.escalatedSeverity) // P3 -> P4
        assertEquals(15, escalation.escalationBonusPoints)
        assertTrue(escalation.reason.contains("Repeated failure escalation"))
    }

    @Test
    fun tenPriorFailures_strictlyCapsAtP4AndMaxBonus() {
        val current = createFailure("fail_current", "commit_current", now)
        val history = (1..10).map { i ->
            createFailure("fail_$i", "commit_$i", now - (i * oneDayMs))
        }

        val escalation = PenaltyEscalationEngine.evaluate(
            baseSeverity = PenaltySeverity.P1,
            failureHistory = history,
            currentFailure = current,
            policy = DefaultPenaltyPolicy
        )

        assertEquals(10, escalation.eligibleFailuresInWindow)
        assertEquals(PenaltySeverity.P4, escalation.escalatedSeverity) // P1 -> capped at P4
        assertEquals(15, escalation.escalationBonusPoints) // strictly capped at maxEscalationBonusPoints (15)
        assertTrue(escalation.reason.contains("maximum ceiling enforced"))
    }

    @Test
    fun failuresOutsideRollingWindow_areExcluded() {
        val current = createFailure("fail_current", "commit_current", now)
        val history = listOf(
            // 20 days ago (outside 14-day window)
            createFailure("fail_old_1", "commit_old_1", now - (20 * oneDayMs)),
            // 15 days ago (outside 14-day window)
            createFailure("fail_old_2", "commit_old_2", now - (15 * oneDayMs)),
            // 5 days ago (inside window)
            createFailure("fail_recent", "commit_recent", now - (5 * oneDayMs))
        )

        val escalation = PenaltyEscalationEngine.evaluate(
            baseSeverity = PenaltySeverity.P2,
            failureHistory = history,
            currentFailure = current,
            policy = DefaultPenaltyPolicy
        )

        assertEquals(1, escalation.eligibleFailuresInWindow)
        assertEquals(PenaltySeverity.P2, escalation.escalatedSeverity)
        assertEquals(0, escalation.escalationBonusPoints)
    }

    @Test
    fun duplicateFailureRecordsForSameCommitment_areDeduplicated() {
        val current = createFailure("fail_current", "commit_current", now)
        val history = listOf(
            // Duplicate evaluation records for same commitment
            createFailure("fail_1a", "commit_1", now - (2 * oneDayMs)),
            createFailure("fail_1b", "commit_1", now - (2 * oneDayMs))
        )

        val escalation = PenaltyEscalationEngine.evaluate(
            baseSeverity = PenaltySeverity.P2,
            failureHistory = history,
            currentFailure = current,
            policy = DefaultPenaltyPolicy
        )

        // Deduplication yields 1 distinct failure
        assertEquals(1, escalation.eligibleFailuresInWindow)
        assertEquals(PenaltySeverity.P2, escalation.escalatedSeverity)
    }
}
