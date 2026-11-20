package com.example.domain.model

import java.util.UUID

/**
 * The explicit lifecycle states of a player commitment.
 * Non-accepted quests never enter failure evaluation.
 */
enum class CommitmentState(val label: String, val isTerminal: Boolean) {
    NOT_ACCEPTED("Not Accepted", false),
    ACCEPTED("Accepted", false),
    ACTIVE("Active", false),
    COMPLETED("Completed", true),
    OVERDUE("Overdue", false),
    PENDING_REVIEW("Pending Review", false),
    FAILED("Failed", true),
    RESCHEDULED("Rescheduled", false),
    EXEMPTED("Exempted", true),
    CANCELLED("Cancelled", true)
}

/**
 * Deterministic status for grace-period evaluation.
 */
enum class GracePeriodPhase(val label: String) {
    BEFORE_DEADLINE("Before Deadline"),
    GRACE_PERIOD_ACTIVE("Grace Period Active"),
    GRACE_PERIOD_EXPIRED("Grace Period Expired")
}

/**
 * Strongly typed failure decision outcome.
 */
enum class FailureDecisionOutcome(val label: String, val isFailure: Boolean) {
    NOT_ELIGIBLE("Not Eligible", false),
    NOT_YET_OVERDUE("Not Yet Overdue", false),
    GRACE_PERIOD_ACTIVE("Grace Period Active", false),
    COMPLETED("Completed", false),
    PENDING_EXEMPTION_REVIEW("Pending Exemption Review", false),
    EXEMPTED("Exempted", false),
    RESCHEDULED("Rescheduled", false),
    CANCELLED("Cancelled", false),
    FAILURE_CONFIRMED("Failure Confirmed", true)
}

/**
 * Valid exemption reason categories.
 */
enum class ExemptionReasonCategory(val label: String, val requiresDocumentation: Boolean) {
    ILLNESS("Illness", false),
    INJURY("Injury", false),
    EMERGENCY("Personal Emergency", false),
    SYSTEM_OUTAGE("System Outage", true),
    FORCE_MAJEURE("Force Majeure", false),
    RECOVERY_REQUIREMENT("Recovery Requirement", false),
    UNEXPECTED_RESPONSIBILITY("Unexpected Responsibility", false),
    OTHER_EXCEPTIONAL("Other Exceptional", false)
}

/**
 * Lifecycle state of an exemption request.
 */
enum class ExemptionLifecycleState(val label: String) {
    REQUESTED("Requested"),
    PENDING_REVIEW("Pending Review"),
    APPROVED("Approved"),
    DENIED("Denied"),
    WITHDRAWN("Withdrawn"),
    CLOSED("Closed")
}

/**
 * Abstraction for deterministic time operations during testing.
 */
fun interface Clock {
    fun nowEpochMs(): Long
}

object SystemClock : Clock {
    override fun nowEpochMs(): Long = System.currentTimeMillis()
}

/**
 * Domain model representing an explicit quest commitment by a player.
 */
data class QuestCommitment(
    val commitmentId: String = UUID.randomUUID().toString(),
    val questId: Long,
    val accountOwnerId: String,
    val logicalCycleId: String,
    val state: CommitmentState = CommitmentState.NOT_ACCEPTED,
    val acceptedAtEpochMs: Long? = null,
    val deadlineEpochMs: Long,
    val gracePeriodMinutes: Int = DEFAULT_GRACE_PERIOD_MINUTES,
    val gracePeriodExpiresAtEpochMs: Long = deadlineEpochMs + (gracePeriodMinutes * 60_000L),
    val rescheduleCount: Int = 0,
    val originalDeadlineEpochMs: Long = deadlineEpochMs,
    val completedAtEpochMs: Long? = null,
    val verificationStatus: VerificationStatus = VerificationStatus.SELF_REPORTED
) {
    companion object {
        const val DEFAULT_GRACE_PERIOD_MINUTES = 30
        const val MIN_GRACE_PERIOD_MINUTES = 15
        const val MAX_GRACE_PERIOD_MINUTES = 60
        const val MAX_ALLOWED_RESCHEDULES = 2

        fun buildCommitmentId(questId: Long, logicalCycleId: String): String =
            "commit_${questId}_${logicalCycleId}"
    }

    val isAccepted: Boolean
        get() = state != CommitmentState.NOT_ACCEPTED

    val isFailureEligible: Boolean
        get() = state in setOf(CommitmentState.ACCEPTED, CommitmentState.ACTIVE, CommitmentState.OVERDUE)
}

/**
 * Detailed failure decision outcome with reason code and audit telemetry.
 */
data class QuestFailureEvaluation(
    val commitmentId: String,
    val questId: Long,
    val logicalCycleId: String,
    val decision: FailureDecisionOutcome,
    val reasonCode: String,
    val evaluatedAtEpochMs: Long,
    val deadlineEpochMs: Long,
    val gracePeriodExpiresAtEpochMs: Long,
    val exemptionId: String? = null,
    val recoveryProtected: Boolean = false
)

/**
 * Domain model for an exemption request on a commitment.
 */
data class QuestExemption(
    val exemptionId: String = UUID.randomUUID().toString(),
    val commitmentId: String,
    val questId: Long,
    val accountOwnerId: String,
    val category: ExemptionReasonCategory,
    val lifecycleState: ExemptionLifecycleState = ExemptionLifecycleState.REQUESTED,
    val explanation: String,
    val requestedAtEpochMs: Long = System.currentTimeMillis(),
    val reviewedAtEpochMs: Long? = null,
    val reviewerNotes: String? = null
)

/**
 * Domain model for tracking an auditable deadline reschedule.
 */
data class QuestRescheduleRecord(
    val rescheduleId: String = UUID.randomUUID().toString(),
    val commitmentId: String,
    val questId: Long,
    val previousDeadlineEpochMs: Long,
    val revisedDeadlineEpochMs: Long,
    val reasonCategory: String,
    val rescheduledAtEpochMs: Long = System.currentTimeMillis()
)

/**
 * Persistent failure record surviving resets and app restarts.
 */
data class ConfirmedFailureRecord(
    val failureId: String = UUID.randomUUID().toString(),
    val commitmentId: String,
    val questId: Long,
    val logicalCycleId: String,
    val accountOwnerId: String,
    val deadlineEpochMs: Long,
    val gracePeriodExpiresAtEpochMs: Long,
    val confirmedAtEpochMs: Long,
    val failureReasonCode: String,
    val policyVersion: Int = 1,
    val exemptionEvaluated: Boolean = false
)
