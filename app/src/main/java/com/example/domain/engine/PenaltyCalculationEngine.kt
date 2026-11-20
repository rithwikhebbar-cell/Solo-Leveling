package com.example.domain.engine

import com.example.domain.model.CommitmentState
import com.example.domain.model.ConfirmedFailureRecord
import com.example.domain.model.DefaultPenaltyPolicy
import com.example.domain.model.PenaltyCalculationResult
import com.example.domain.model.PenaltyPolicy
import com.example.domain.model.PenaltySeverity
import com.example.domain.model.Quest
import com.example.domain.model.QuestCategory
import com.example.domain.model.QuestCommitment
import com.example.domain.model.QuestType

/**
 * Pure Kotlin, deterministic engine for calculating penalty severity and point values.
 * Evaluates quest importance, effort, deadline violations, and historical escalation.
 *
 * Guarantees:
 * - Deterministic output for identical inputs.
 * - Complete progression isolation (no XP, rank, or attribute mutations).
 * - Excludes unaccepted, completed, exempted, or protected commitments.
 * - Transparent, non-punitive system explanations.
 */
object PenaltyCalculationEngine {

    sealed class EligibilityResult {
        data object Eligible : EligibilityResult()
        data class Ineligible(val reason: String) : EligibilityResult()
    }

    /**
     * Checks if a commitment and failure record combination is strictly eligible for penalty allocation.
     */
    fun checkEligibility(
        commitment: QuestCommitment?,
        quest: Quest?,
        failureRecord: ConfirmedFailureRecord?
    ): EligibilityResult {
        if (failureRecord == null) {
            return EligibilityResult.Ineligible("No confirmed failure record provided.")
        }
        if (commitment == null) {
            return EligibilityResult.Ineligible("Commitment record does not exist.")
        }
        if (quest == null) {
            return EligibilityResult.Ineligible("Target quest does not exist.")
        }
        if (!commitment.isAccepted || commitment.state == CommitmentState.NOT_ACCEPTED) {
            return EligibilityResult.Ineligible("Quest was not explicitly accepted by the player.")
        }
        if (commitment.state == CommitmentState.CANCELLED) {
            return EligibilityResult.Ineligible("Commitment was legitimately cancelled.")
        }
        if (commitment.state == CommitmentState.EXEMPTED || failureRecord.exemptionEvaluated) {
            return EligibilityResult.Ineligible("Commitment was granted an approved exemption.")
        }
        if (commitment.state == CommitmentState.PENDING_REVIEW) {
            return EligibilityResult.Ineligible("Commitment has an active pending exemption review.")
        }
        if (quest.isCompleted) {
            return EligibilityResult.Ineligible("Quest was completed and verified.")
        }
        return EligibilityResult.Eligible
    }

    /**
     * Deterministically calculates the base severity and total penalty points for an eligible failure.
     */
    fun calculate(
        failureRecord: ConfirmedFailureRecord,
        commitment: QuestCommitment,
        quest: Quest,
        failureHistory: List<ConfirmedFailureRecord> = emptyList(),
        policy: PenaltyPolicy = DefaultPenaltyPolicy
    ): PenaltyCalculationResult {
        val eligibility = checkEligibility(commitment, quest, failureRecord)
        if (eligibility is EligibilityResult.Ineligible) {
            throw IllegalArgumentException("Failure is not eligible for penalty: ${eligibility.reason}")
        }

        // 1. Determine baseline severity from Quest Effort & Importance
        val baseSeverity = determineBaseSeverity(quest, commitment)

        // 2. Evaluate historical failure escalation
        val escalation = PenaltyEscalationEngine.evaluate(
            baseSeverity = baseSeverity,
            failureHistory = failureHistory,
            currentFailure = failureRecord,
            policy = policy
        )

        val finalSeverity = escalation.escalatedSeverity
        val basePoints = policy.basePointsBySeverity[finalSeverity]
            ?: policy.basePointsBySeverity[baseSeverity]
            ?: finalSeverity.basePoints
        val totalPoints = basePoints + escalation.escalationBonusPoints

        val isEscalated = escalation.escalationBonusPoints > 0 || escalation.escalatedSeverity != baseSeverity

        val reasonBuilder = StringBuilder()
        reasonBuilder.append("Failure confirmed for '${quest.title}'. Base severity: ${baseSeverity.displayName} ($basePoints PP).")
        if (isEscalated) {
            reasonBuilder.append(" Escalated to ${finalSeverity.displayName}: ${escalation.reason}")
        } else {
            reasonBuilder.append(" No escalation applied.")
        }

        return PenaltyCalculationResult(
            severity = finalSeverity,
            basePenaltyPoints = basePoints,
            escalationBonusPoints = escalation.escalationBonusPoints,
            totalPenaltyPoints = totalPoints,
            reason = reasonBuilder.toString(),
            policyVersion = policy.policyVersion,
            isEscalated = isEscalated,
            escalationReason = if (isEscalated) escalation.reason else null
        )
    }

    /**
     * Calculates base severity from objective quest metadata and commitment history.
     */
    private fun determineBaseSeverity(
        quest: Quest,
        commitment: QuestCommitment
    ): PenaltySeverity {
        // High stakes / Milestone quests
        if (quest.type == QuestType.LONG_TERM || quest.xpReward > 150) {
            return PenaltySeverity.P4
        }

        // High effort or Weekly commitments
        if (quest.type == QuestType.WEEKLY || quest.xpReward > 70 || quest.targetValue > 45) {
            return PenaltySeverity.P3
        }

        // Light / Short daily commitments
        if (quest.type == QuestType.DAILY && quest.targetValue <= 20 && quest.xpReward <= 35) {
            // If repeatedly rescheduled (exhausted all 2 allowed reschedules) and still missed, bump to P2
            return if (commitment.rescheduleCount >= QuestCommitment.MAX_ALLOWED_RESCHEDULES) {
                PenaltySeverity.P2
            } else {
                PenaltySeverity.P1
            }
        }

        // Standard Daily commitments
        return PenaltySeverity.P2
    }
}
