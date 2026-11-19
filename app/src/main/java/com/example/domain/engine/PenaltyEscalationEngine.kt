package com.example.domain.engine

import com.example.domain.model.ConfirmedFailureRecord
import com.example.domain.model.DefaultPenaltyPolicy
import com.example.domain.model.PenaltyEscalationRecord
import com.example.domain.model.PenaltyPolicy
import com.example.domain.model.PenaltySeverity
import kotlin.math.min

/**
 * Pure Kotlin, deterministic escalation engine.
 * Calculates severity adjustments and bonus points based on distinct confirmed failures
 * within a rolling history window.
 *
 * Guarantees:
 * - Deterministic outcome.
 * - Exemptions and duplicate records are never counted.
 * - Failures outside the rolling window are excluded.
 * - Severity is strictly capped at [PenaltyPolicy.maxSeverity] (P4).
 * - Bonus points are strictly capped at [PenaltyPolicy.maxEscalationBonusPoints].
 * - Full audit trail with transparent explanation.
 */
object PenaltyEscalationEngine {

    fun evaluate(
        baseSeverity: PenaltySeverity,
        failureHistory: List<ConfirmedFailureRecord>,
        currentFailure: ConfirmedFailureRecord,
        policy: PenaltyPolicy = DefaultPenaltyPolicy
    ): PenaltyEscalationRecord {
        val windowStartEpochMs = currentFailure.confirmedAtEpochMs - policy.rollingHistoryWindowMs

        // Filter and deduplicate distinct eligible failures strictly within the rolling window
        val distinctEligibleInWindow = failureHistory
            .asSequence()
            // Exclude current failure itself
            .filter { it.failureId != currentFailure.failureId && it.commitmentId != currentFailure.commitmentId }
            // Must be within rolling window
            .filter { it.confirmedAtEpochMs in windowStartEpochMs..currentFailure.confirmedAtEpochMs }
            // Deduplicate by commitmentId to prevent duplicate counts for same cycle
            .distinctBy { it.commitmentId }
            .toList()

        val count = distinctEligibleInWindow.size

        return when {
            count < policy.escalationThresholdFailures -> {
                PenaltyEscalationRecord(
                    evaluationTimestampEpochMs = currentFailure.confirmedAtEpochMs,
                    rollingWindowStartEpochMs = windowStartEpochMs,
                    eligibleFailuresInWindow = count,
                    originalSeverity = baseSeverity,
                    escalatedSeverity = baseSeverity,
                    escalationBonusPoints = 0,
                    reason = if (count == 0) {
                        "Initial failure in ${policy.rollingHistoryWindowDays}-day window. Standard baseline severity applied."
                    } else {
                        "1 eligible failure in ${policy.rollingHistoryWindowDays}-day window. Threshold for escalation not reached."
                    }
                )
            }
            count == policy.escalationThresholdFailures -> {
                val escalated = promoteSeverity(baseSeverity, steps = 1, maxSeverity = policy.maxSeverity)
                val bonus = min(policy.bonusPerEscalationStep, policy.maxEscalationBonusPoints)
                PenaltyEscalationRecord(
                    evaluationTimestampEpochMs = currentFailure.confirmedAtEpochMs,
                    rollingWindowStartEpochMs = windowStartEpochMs,
                    eligibleFailuresInWindow = count,
                    originalSeverity = baseSeverity,
                    escalatedSeverity = escalated,
                    escalationBonusPoints = bonus,
                    reason = "Escalation triggered: $count eligible failures recorded within ${policy.rollingHistoryWindowDays} days. Severity increased by 1 tier with +${bonus} PP bonus."
                )
            }
            else -> {
                // count > escalationThresholdFailures
                val steps = min(count - 1, 2)
                val escalated = promoteSeverity(baseSeverity, steps = steps, maxSeverity = policy.maxSeverity)
                val rawBonus = count * policy.bonusPerEscalationStep
                val bonus = min(rawBonus, policy.maxEscalationBonusPoints)
                PenaltyEscalationRecord(
                    evaluationTimestampEpochMs = currentFailure.confirmedAtEpochMs,
                    rollingWindowStartEpochMs = windowStartEpochMs,
                    eligibleFailuresInWindow = count,
                    originalSeverity = baseSeverity,
                    escalatedSeverity = escalated,
                    escalationBonusPoints = bonus,
                    reason = "Repeated failure escalation: $count eligible failures in ${policy.rollingHistoryWindowDays}-day window. Severity escalated to ${escalated.displayName} (+${bonus} PP bonus, maximum ceiling enforced)."
                )
            }
        }
    }

    private fun promoteSeverity(
        initial: PenaltySeverity,
        steps: Int,
        maxSeverity: PenaltySeverity
    ): PenaltySeverity {
        var current = initial
        repeat(steps) {
            if (current.level < maxSeverity.level) {
                current = current.nextLevel()
            }
        }
        return if (current.level > maxSeverity.level) maxSeverity else current
    }
}
