package com.example.domain.engine

import com.example.domain.model.Clock
import com.example.domain.model.CommitmentState
import com.example.domain.model.ExemptionLifecycleState
import com.example.domain.model.FailureDecisionOutcome
import com.example.domain.model.GracePeriodPhase
import com.example.domain.model.QuestCommitment
import com.example.domain.model.QuestExemption
import com.example.domain.model.QuestFailureEvaluation
import com.example.domain.model.QuestType
import com.example.domain.model.RecoveryState
import com.example.domain.model.SystemClock
import com.example.domain.model.VerificationStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Deterministic deadline calculator using application logical day rules and dailyResetHour.
 */
object DeadlineCalculator {

    /**
     * Calculates the default deadline for a daily quest belonging to a specific logical epochDay.
     * The daily quest commitment expires when the logical day closes at the next day's dailyResetHour.
     */
    fun calculateDefaultDailyDeadline(
        targetEpochDay: Long,
        dailyResetHour: Int = 4,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long {
        val safeResetHour = dailyResetHour.coerceIn(0, 23)
        val closingCalendarDate = LocalDate.ofEpochDay(targetEpochDay).plusDays(1)
        return closingCalendarDate
            .atTime(safeResetHour, 0)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
    }

    /**
     * Calculates the default deadline for a weekly quest ending at the close of Sunday's logical cycle.
     */
    fun calculateWeeklyDeadline(
        targetEpochDay: Long,
        dailyResetHour: Int = 4,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long {
        val (_, weekEndDay) = DailyCycleManager.weekBoundsFor(targetEpochDay)
        return calculateDefaultDailyDeadline(weekEndDay, dailyResetHour, zoneId)
    }

    /**
     * Calculates deadline for a quest based on its QuestType.
     */
    fun calculateDeadlineForType(
        type: QuestType,
        targetEpochDay: Long,
        dailyResetHour: Int = 4,
        zoneId: ZoneId = ZoneId.systemDefault(),
        customTargetEpochMs: Long? = null
    ): Long {
        if (customTargetEpochMs != null && customTargetEpochMs > 0L) {
            return customTargetEpochMs
        }
        return when (type) {
            QuestType.DAILY -> calculateDefaultDailyDeadline(targetEpochDay, dailyResetHour, zoneId)
            QuestType.WEEKLY -> calculateWeeklyDeadline(targetEpochDay, dailyResetHour, zoneId)
            QuestType.LONG_TERM -> {
                // Long term defaults to 30 days from cycle start
                val targetEnd = targetEpochDay + 30L
                calculateDefaultDailyDeadline(targetEnd, dailyResetHour, zoneId)
            }
        }
    }
}

/**
 * Deterministic grace period boundary evaluator.
 */
object GracePeriodEngine {

    fun calculateGraceExpiry(deadlineEpochMs: Long, gracePeriodMinutes: Int): Long {
        val safeMinutes = gracePeriodMinutes.coerceIn(
            QuestCommitment.MIN_GRACE_PERIOD_MINUTES,
            QuestCommitment.MAX_GRACE_PERIOD_MINUTES
        )
        return deadlineEpochMs + (safeMinutes * 60_000L)
    }

    fun evaluatePhase(
        nowEpochMs: Long,
        deadlineEpochMs: Long,
        gracePeriodMinutes: Int
    ): GracePeriodPhase {
        val graceExpiry = calculateGraceExpiry(deadlineEpochMs, gracePeriodMinutes)
        return when {
            nowEpochMs < deadlineEpochMs -> GracePeriodPhase.BEFORE_DEADLINE
            nowEpochMs < graceExpiry -> GracePeriodPhase.GRACE_PERIOD_ACTIVE
            else -> GracePeriodPhase.GRACE_PERIOD_EXPIRED
        }
    }
}

/**
 * Core deterministic engine for evaluating quest commitments and confirming failures.
 * INVARIANT: Non-accepted quests, cancelled quests, completed quests, and protected exemptions
 * NEVER result in FAILURE_CONFIRMED.
 */
object QuestFailureDetectionEngine {

    fun evaluateCommitment(
        commitment: QuestCommitment,
        isQuestCompleted: Boolean,
        verificationStatus: VerificationStatus,
        recoveryState: RecoveryState? = null,
        activeExemption: QuestExemption? = null,
        isPhysicalQuest: Boolean = false,
        clock: Clock = SystemClock
    ): QuestFailureEvaluation {
        val now = clock.nowEpochMs()

        // 1. Unaccepted quests are never failure-eligible
        if (!commitment.isAccepted || commitment.state == CommitmentState.NOT_ACCEPTED) {
            return QuestFailureEvaluation(
                commitmentId = commitment.commitmentId,
                questId = commitment.questId,
                logicalCycleId = commitment.logicalCycleId,
                decision = FailureDecisionOutcome.NOT_ELIGIBLE,
                reasonCode = "UNACCEPTED_COMMITMENT",
                evaluatedAtEpochMs = now,
                deadlineEpochMs = commitment.deadlineEpochMs,
                gracePeriodExpiresAtEpochMs = commitment.gracePeriodExpiresAtEpochMs
            )
        }

        // 2. Explicitly cancelled quests
        if (commitment.state == CommitmentState.CANCELLED) {
            return QuestFailureEvaluation(
                commitmentId = commitment.commitmentId,
                questId = commitment.questId,
                logicalCycleId = commitment.logicalCycleId,
                decision = FailureDecisionOutcome.CANCELLED,
                reasonCode = "COMMITMENT_CANCELLED",
                evaluatedAtEpochMs = now,
                deadlineEpochMs = commitment.deadlineEpochMs,
                gracePeriodExpiresAtEpochMs = commitment.gracePeriodExpiresAtEpochMs
            )
        }

        // 3. Completed quest verification
        if (isQuestCompleted || commitment.state == CommitmentState.COMPLETED) {
            return QuestFailureEvaluation(
                commitmentId = commitment.commitmentId,
                questId = commitment.questId,
                logicalCycleId = commitment.logicalCycleId,
                decision = FailureDecisionOutcome.COMPLETED,
                reasonCode = "QUEST_COMPLETED_SUCCESSFULLY",
                evaluatedAtEpochMs = now,
                deadlineEpochMs = commitment.deadlineEpochMs,
                gracePeriodExpiresAtEpochMs = commitment.gracePeriodExpiresAtEpochMs
            )
        }

        // 4. Approved exemption protection
        if (activeExemption != null && activeExemption.lifecycleState == ExemptionLifecycleState.APPROVED) {
            return QuestFailureEvaluation(
                commitmentId = commitment.commitmentId,
                questId = commitment.questId,
                logicalCycleId = commitment.logicalCycleId,
                decision = FailureDecisionOutcome.EXEMPTED,
                reasonCode = "EXEMPTION_APPROVED_${activeExemption.category.name}",
                evaluatedAtEpochMs = now,
                deadlineEpochMs = commitment.deadlineEpochMs,
                gracePeriodExpiresAtEpochMs = commitment.gracePeriodExpiresAtEpochMs,
                exemptionId = activeExemption.exemptionId
            )
        }

        // 5. Pending exemption request pauses failure confirmation
        if (activeExemption != null &&
            (activeExemption.lifecycleState == ExemptionLifecycleState.REQUESTED ||
             activeExemption.lifecycleState == ExemptionLifecycleState.PENDING_REVIEW)
        ) {
            return QuestFailureEvaluation(
                commitmentId = commitment.commitmentId,
                questId = commitment.questId,
                logicalCycleId = commitment.logicalCycleId,
                decision = FailureDecisionOutcome.PENDING_EXEMPTION_REVIEW,
                reasonCode = "EXEMPTION_REVIEW_PENDING",
                evaluatedAtEpochMs = now,
                deadlineEpochMs = commitment.deadlineEpochMs,
                gracePeriodExpiresAtEpochMs = commitment.gracePeriodExpiresAtEpochMs,
                exemptionId = activeExemption.exemptionId
            )
        }

        // 6. Recovery restriction guardrail (protects physical training during injury/fatigue)
        val isRecoveryProtected = isPhysicalQuest && (
            recoveryState == RecoveryState.REST_RECOMMENDED ||
            recoveryState == RecoveryState.RECOVERY_RECOMMENDED
        )
        if (isRecoveryProtected) {
            return QuestFailureEvaluation(
                commitmentId = commitment.commitmentId,
                questId = commitment.questId,
                logicalCycleId = commitment.logicalCycleId,
                decision = FailureDecisionOutcome.EXEMPTED,
                reasonCode = "RECOVERY_RESTRICTION_PROTECTION",
                evaluatedAtEpochMs = now,
                deadlineEpochMs = commitment.deadlineEpochMs,
                gracePeriodExpiresAtEpochMs = commitment.gracePeriodExpiresAtEpochMs,
                recoveryProtected = true
            )
        }

        // 7. Time boundary evaluations
        if (now < commitment.deadlineEpochMs) {
            return QuestFailureEvaluation(
                commitmentId = commitment.commitmentId,
                questId = commitment.questId,
                logicalCycleId = commitment.logicalCycleId,
                decision = FailureDecisionOutcome.NOT_YET_OVERDUE,
                reasonCode = "WITHIN_SCHEDULED_DEADLINE",
                evaluatedAtEpochMs = now,
                deadlineEpochMs = commitment.deadlineEpochMs,
                gracePeriodExpiresAtEpochMs = commitment.gracePeriodExpiresAtEpochMs
            )
        }

        if (now < commitment.gracePeriodExpiresAtEpochMs) {
            return QuestFailureEvaluation(
                commitmentId = commitment.commitmentId,
                questId = commitment.questId,
                logicalCycleId = commitment.logicalCycleId,
                decision = FailureDecisionOutcome.GRACE_PERIOD_ACTIVE,
                reasonCode = "IN_GRACE_PERIOD",
                evaluatedAtEpochMs = now,
                deadlineEpochMs = commitment.deadlineEpochMs,
                gracePeriodExpiresAtEpochMs = commitment.gracePeriodExpiresAtEpochMs
            )
        }

        // 8. Every eligibility check passed, deadline and grace period expired -> Failure Confirmed
        return QuestFailureEvaluation(
            commitmentId = commitment.commitmentId,
            questId = commitment.questId,
            logicalCycleId = commitment.logicalCycleId,
            decision = FailureDecisionOutcome.FAILURE_CONFIRMED,
            reasonCode = "DEADLINE_AND_GRACE_EXPIRED",
            evaluatedAtEpochMs = now,
            deadlineEpochMs = commitment.deadlineEpochMs,
            gracePeriodExpiresAtEpochMs = commitment.gracePeriodExpiresAtEpochMs
        )
    }
}
