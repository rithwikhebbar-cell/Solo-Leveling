package com.example

import com.example.domain.engine.PenaltyAnalyticsEngine
import com.example.domain.model.CommitmentState
import com.example.domain.model.ConfirmedFailureRecord
import com.example.domain.model.ConsistencyTrend
import com.example.domain.model.CorrectiveActivityLifecycleState
import com.example.domain.model.CorrectiveActivityRecord
import com.example.domain.model.ExemptionLifecycleState
import com.example.domain.model.ExemptionReasonCategory
import com.example.domain.model.PenaltyRecord
import com.example.domain.model.PenaltySeverity
import com.example.domain.model.PenaltyStatus
import com.example.domain.model.PenaltyTimelineType
import com.example.domain.model.PenaltyTransaction
import com.example.domain.model.PenaltyTransactionType
import com.example.domain.model.QuestCategory
import com.example.domain.model.QuestCommitment
import com.example.domain.model.QuestExemption
import com.example.domain.model.ShadowClass
import com.example.domain.model.VerificationMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class PenaltyAnalyticsEngineTest {

    private val baseEpochMs = 1_700_000_000_000L // arbitrary fixed timestamp

    @Test
    fun overview_emptyDataset_returnsZeroesAndHundredPercentRecovery() {
        val overview = PenaltyAnalyticsEngine.calculateOverviewSummary(
            accountOwnerId = "test_player",
            currentBalance = 0,
            lifetimeAccrued = 0,
            transactions = emptyList(),
            penalties = emptyList()
        )

        assertEquals("test_player", overview.accountOwnerId)
        assertEquals(0, overview.currentBalance)
        assertEquals(0, overview.lifetimePointsAccrued)
        assertEquals(0, overview.lifetimePointsDeducted)
        assertEquals(0, overview.activePenaltiesCount)
        assertEquals(0, overview.resolvedPenaltiesCount)
        assertEquals(0, overview.exemptedPenaltiesCount)
        assertEquals(100.0, overview.recoveryRatePercent, 0.01)
    }

    @Test
    fun overview_withActiveAndResolvedPenalties_calculatesAccurately() {
        val penalties = listOf(
            PenaltyRecord(
                penaltyId = "p1",
                failureRecordId = "f1",
                commitmentId = "c1",
                questId = 1L,
                accountOwnerId = "test_player",
                questTitleSnapshot = "Quest 1",
                severity = PenaltySeverity.P1_MINOR,
                basePenaltyPoints = 5,
                escalationBonusPoints = 0,
                totalPenaltyPoints = 5,
                status = PenaltyStatus.ACTIVE,
                reason = "Missed",
                policyVersion = "1.0",
                createdAtEpochMs = baseEpochMs
            ),
            PenaltyRecord(
                penaltyId = "p2",
                failureRecordId = "f2",
                commitmentId = "c2",
                questId = 2L,
                accountOwnerId = "test_player",
                questTitleSnapshot = "Quest 2",
                severity = PenaltySeverity.P2_MODERATE,
                basePenaltyPoints = 10,
                escalationBonusPoints = 0,
                totalPenaltyPoints = 10,
                status = PenaltyStatus.RESOLVED,
                reason = "Missed",
                policyVersion = "1.0",
                createdAtEpochMs = baseEpochMs,
                resolvedAtEpochMs = baseEpochMs + 3600_000L
            ),
            PenaltyRecord(
                penaltyId = "p3",
                failureRecordId = "f3",
                commitmentId = "c3",
                questId = 3L,
                accountOwnerId = "test_player",
                questTitleSnapshot = "Quest 3",
                severity = PenaltySeverity.P1_MINOR,
                basePenaltyPoints = 5,
                escalationBonusPoints = 0,
                totalPenaltyPoints = 5,
                status = PenaltyStatus.EXEMPTED,
                reason = "Exempted",
                policyVersion = "1.0",
                createdAtEpochMs = baseEpochMs
            )
        )

        val transactions = listOf(
            PenaltyTransaction(
                transactionId = "t1",
                accountOwnerId = "test_player",
                penaltyId = "p1",
                transactionType = PenaltyTransactionType.ALLOCATION,
                pointsDelta = 5,
                balanceAfter = 5,
                reason = "Allocation",
                idempotencyKey = "k1",
                timestampEpochMs = baseEpochMs
            ),
            PenaltyTransaction(
                transactionId = "t2",
                accountOwnerId = "test_player",
                penaltyId = "p2",
                transactionType = PenaltyTransactionType.ALLOCATION,
                pointsDelta = 10,
                balanceAfter = 15,
                reason = "Allocation",
                idempotencyKey = "k2",
                timestampEpochMs = baseEpochMs
            ),
            PenaltyTransaction(
                transactionId = "t3",
                accountOwnerId = "test_player",
                penaltyId = "p2",
                transactionType = PenaltyTransactionType.REDEMPTION_RESOLUTION,
                pointsDelta = 10,
                balanceAfter = 5,
                reason = "Resolution",
                idempotencyKey = "k3",
                timestampEpochMs = baseEpochMs + 3600_000L
            )
        )

        val overview = PenaltyAnalyticsEngine.calculateOverviewSummary(
            accountOwnerId = "test_player",
            currentBalance = 5,
            lifetimeAccrued = 15,
            transactions = transactions,
            penalties = penalties
        )

        assertEquals(5, overview.currentBalance)
        assertEquals(15, overview.lifetimePointsAccrued)
        assertEquals(10, overview.lifetimePointsDeducted)
        assertEquals(1, overview.activePenaltiesCount)
        assertEquals(1, overview.resolvedPenaltiesCount)
        assertEquals(1, overview.exemptedPenaltiesCount)
        // 2 resolved/exempted out of 3 total = 66.7%
        assertEquals(66.7, overview.recoveryRatePercent, 0.1)
    }

    @Test
    fun failureTrends_calculatesCategoryAndTemporalClustering() {
        val commitments = listOf(
            QuestCommitment(commitmentId = "c1", questId = 1L, accountOwnerId = "p1", logicalCycleId = "cy1", state = CommitmentState.COMPLETED, deadlineEpochMs = baseEpochMs),
            QuestCommitment(commitmentId = "c2", questId = 2L, accountOwnerId = "p1", logicalCycleId = "cy1", state = CommitmentState.FAILED, deadlineEpochMs = baseEpochMs),
            QuestCommitment(commitmentId = "c3", questId = 3L, accountOwnerId = "p1", logicalCycleId = "cy1", state = CommitmentState.FAILED, deadlineEpochMs = baseEpochMs)
        )
        val failures = listOf(
            ConfirmedFailureRecord(failureId = "f1", commitmentId = "c2", questId = 2L, accountOwnerId = "p1", logicalCycleId = "cy1", failedAtEpochMs = baseEpochMs, reasonCode = "OVERDUE", triggerSource = "TEST"),
            ConfirmedFailureRecord(failureId = "f2", commitmentId = "c3", questId = 3L, accountOwnerId = "p1", logicalCycleId = "cy1", failedAtEpochMs = baseEpochMs + 7200_000L, reasonCode = "OVERDUE", triggerSource = "TEST")
        )
        val questCategoryMap = mapOf(1L to "PHYSICAL", 2L to "PHYSICAL", 3L to "ACADEMIC")

        val trends = PenaltyAnalyticsEngine.calculateFailureTrends(
            commitments = commitments,
            failures = failures,
            questCategoryMap = questCategoryMap,
            zoneId = ZoneId.of("UTC")
        )

        assertEquals(3, trends.totalCommitments)
        assertEquals(1, trends.successfulCommitments)
        assertEquals(2, trends.failedCommitments)
        assertEquals(33.3, trends.completionRatePercent, 0.1)
        assertEquals(66.7, trends.failureRatePercent, 0.1)
        assertEquals(1, trends.failuresByCategory["PHYSICAL"])
        assertEquals(1, trends.failuresByCategory["ACADEMIC"])
        assertTrue(trends.failuresByTimeOfDay.isNotEmpty())
    }

    @Test
    fun redemptionMetrics_evaluatesResolutionVelocity() {
        val activities = listOf(
            CorrectiveActivityRecord(
                activityId = "a1",
                penaltyId = "p1",
                accountOwnerId = "test_player",
                title = "Reflection",
                description = "Desc",
                category = QuestCategory.LIFE,
                pointValue = 5,
                assignedShadowClass = ShadowClass.INFANTRY,
                verificationMethod = VerificationMethod.STRUCTURED_REFLECTION,
                lifecycleState = CorrectiveActivityLifecycleState.COMPLETED,
                createdAtEpochMs = baseEpochMs,
                completedAtEpochMs = baseEpochMs + (2 * 3600_000L) // 2 hours
            ),
            CorrectiveActivityRecord(
                activityId = "a2",
                penaltyId = "p2",
                accountOwnerId = "test_player",
                title = "Study Session",
                description = "Desc",
                category = QuestCategory.ACADEMIC,
                pointValue = 10,
                assignedShadowClass = ShadowClass.MAGE,
                verificationMethod = VerificationMethod.ACADEMIC_STUDY_SESSION,
                lifecycleState = CorrectiveActivityLifecycleState.EXPIRED,
                createdAtEpochMs = baseEpochMs
            )
        )

        val metrics = PenaltyAnalyticsEngine.calculateRedemptionMetrics(activities)

        assertEquals(2, metrics.activitiesAssigned)
        assertEquals(1, metrics.activitiesCompleted)
        assertEquals(1, metrics.activitiesExpired)
        assertEquals(50.0, metrics.redemptionSuccessRatePercent, 0.1)
        assertEquals(2.0, metrics.averageResolutionHours, 0.1)
    }

    @Test
    fun consistencyMetrics_streakEvaluation() {
        val now = baseEpochMs + (5 * 86_400_000L) // 5 days after base
        val commitments = listOf(
            QuestCommitment(commitmentId = "c1", questId = 1L, accountOwnerId = "p1", logicalCycleId = "cy1", state = CommitmentState.COMPLETED, deadlineEpochMs = baseEpochMs)
        )
        val failures = listOf(
            ConfirmedFailureRecord(failureId = "f1", commitmentId = "c2", questId = 2L, accountOwnerId = "p1", logicalCycleId = "cy1", failedAtEpochMs = baseEpochMs, reasonCode = "OVERDUE", triggerSource = "TEST")
        )

        val consistency = PenaltyAnalyticsEngine.calculateConsistencyMetrics(
            commitments = commitments,
            failures = failures,
            nowEpochMs = now,
            zoneId = ZoneId.of("UTC")
        )

        assertEquals(5, consistency.currentCleanStreakDays)
        assertEquals(ConsistencyTrend.STABLE, consistency.streakTrend)
    }

    @Test
    fun auditTimeline_combinesAndOrdersEventsChronologically() {
        val commitments = listOf(
            QuestCommitment(commitmentId = "c1", questId = 1L, accountOwnerId = "p1", logicalCycleId = "cy1", state = CommitmentState.ACCEPTED, acceptedAtEpochMs = baseEpochMs, deadlineEpochMs = baseEpochMs + 1000)
        )
        val failures = listOf(
            ConfirmedFailureRecord(failureId = "f1", commitmentId = "c1", questId = 1L, accountOwnerId = "p1", logicalCycleId = "cy1", failedAtEpochMs = baseEpochMs + 2000, reasonCode = "OVERDUE", triggerSource = "TEST")
        )
        val penalties = listOf(
            PenaltyRecord(
                penaltyId = "p1", failureRecordId = "f1", commitmentId = "c1", questId = 1L, accountOwnerId = "p1",
                questTitleSnapshot = "Quest 1", severity = PenaltySeverity.P1_MINOR, basePenaltyPoints = 5, escalationBonusPoints = 0,
                totalPenaltyPoints = 5, status = PenaltyStatus.ACTIVE, reason = "Failed", policyVersion = "1.0", createdAtEpochMs = baseEpochMs + 3000
            )
        )
        val transactions = listOf(
            PenaltyTransaction(
                transactionId = "tx1", accountOwnerId = "p1", penaltyId = "p1", transactionType = PenaltyTransactionType.REVERSAL,
                pointsDelta = 5, balanceAfter = 0, reason = "Exempted", idempotencyKey = "k1", timestampEpochMs = baseEpochMs + 4000
            )
        )

        val timeline = PenaltyAnalyticsEngine.buildAuditTimeline(
            commitments = commitments,
            failures = failures,
            penalties = penalties,
            transactions = transactions,
            redemptions = emptyList(),
            exemptions = emptyList()
        )

        assertEquals(4, timeline.size)
        // Descending order: REVERSAL (4000) -> ALLOCATION (3000) -> FAILURE (2000) -> COMMITMENT (0)
        assertEquals(PenaltyTimelineType.REVERSAL, timeline[0].type)
        assertEquals(PenaltyTimelineType.ALLOCATION, timeline[1].type)
        assertEquals(PenaltyTimelineType.FAILURE, timeline[2].type)
        assertEquals(PenaltyTimelineType.COMMITMENT, timeline[3].type)
    }
}
