package com.example.domain.model

import java.util.UUID

/**
 * High-level categories for corrective recovery activities.
 */
enum class CorrectiveCategory(val displayName: String) {
    ACADEMIC("Academic Recovery"),
    PHYSICAL("Physical & Movement"),
    DISCIPLINE("Discipline & Focus"),
    LIFE_MANAGEMENT("Life Management & Organization"),
    STRATEGIC_PLANNING("Strategic Planning & Analysis");

    companion object {
        fun fromName(name: String): CorrectiveCategory =
            entries.find { it.name.equals(name, ignoreCase = true) } ?: STRATEGIC_PLANNING
    }
}

/**
 * Bounded difficulty tiers for corrective activities.
 * Enforces maximum daily time limits to prevent exhaustion or overtraining.
 */
enum class CorrectiveDifficulty(val displayName: String, val estimatedMinutes: Int) {
    LIGHT("Light", 10),
    MANAGEABLE("Manageable", 20),
    STRUCTURED("Structured", 30),
    COMPREHENSIVE("Comprehensive", 45);

    companion object {
        fun fromName(name: String): CorrectiveDifficulty =
            entries.find { it.name.equals(name, ignoreCase = true) } ?: LIGHT
    }
}

/**
 * Explicit lifecycle states of a player redemption commitment.
 */
enum class RedemptionLifecycleState(val label: String, val isTerminal: Boolean) {
    GENERATED("Generated", false),
    PENDING_ACCEPTANCE("Pending Acceptance", false),
    ACCEPTED("Accepted", false),
    ACTIVE("Active", false),
    COMPLETION_PENDING_VERIFICATION("Pending Verification", false),
    VERIFIED_COMPLETED("Verified Completed", true),
    RESCHEDULED("Rescheduled", false),
    PAUSED("Paused", false),
    CANCELLED("Cancelled", true),
    REJECTED("Rejected", true);

    companion object {
        fun fromName(name: String): RedemptionLifecycleState =
            entries.find { it.name.equals(name, ignoreCase = true) } ?: GENERATED
    }
}

/**
 * Objective and auditable verification methods for redemption completion.
 */
enum class RedemptionVerificationMethod(val label: String, val requiresEvidence: Boolean) {
    STRUCTURED_REFLECTION("Structured Reflection", true),
    CHECKLIST_CONFIRMATION("Checklist Confirmation", true),
    SESSION_LOG_VERIFIED("Session Log Verified", true),
    PHYSICAL_SESSION_VERIFIED("Physical Session Verified", true),
    ACADEMIC_STUDY_VERIFIED("Academic Study Verified", true),
    USER_ATTESTATION("Auditable User Attestation", false);

    companion object {
        fun fromName(name: String): RedemptionVerificationMethod =
            entries.find { it.name.equals(name, ignoreCase = true) } ?: STRUCTURED_REFLECTION
    }
}

/**
 * Blueprint definition of an available corrective activity in the system catalog.
 */
data class CorrectiveActivityDefinition(
    val activityId: String,
    val category: CorrectiveCategory,
    val title: String,
    val instructions: String,
    val difficulty: CorrectiveDifficulty,
    val estimatedDurationMinutes: Int,
    val verificationMethod: RedemptionVerificationMethod,
    val allowedSeverityTiers: Set<PenaltySeverity>,
    val defaultShadowClass: ShadowClass,
    val checklistItems: List<String> = emptyList()
)

/**
 * Domain representation of an explicit redemption plan issued for an outstanding penalty.
 */
data class RedemptionPlan(
    val redemptionId: String = UUID.randomUUID().toString(),
    val penaltyId: String,
    val failureRecordId: String,
    val questId: Long,
    val accountOwnerId: String,
    val activityId: String,
    val category: CorrectiveCategory,
    val activityTitle: String,
    val instructions: String,
    val difficulty: CorrectiveDifficulty,
    val estimatedDurationMinutes: Int,
    val verificationMethod: RedemptionVerificationMethod,
    val lifecycleState: RedemptionLifecycleState = RedemptionLifecycleState.PENDING_ACCEPTANCE,
    val resolutionPoints: Int,
    val assignedShadowClass: ShadowClass,
    val shadowGuidanceNote: String,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val acceptedAtEpochMs: Long? = null,
    val completedAtEpochMs: Long? = null,
    val verifiedAtEpochMs: Long? = null,
    val verificationNotes: String? = null
) {
    val isPendingAcceptance: Boolean
        get() = lifecycleState == RedemptionLifecycleState.PENDING_ACCEPTANCE || lifecycleState == RedemptionLifecycleState.GENERATED

    val isActive: Boolean
        get() = lifecycleState == RedemptionLifecycleState.ACTIVE || lifecycleState == RedemptionLifecycleState.ACCEPTED
}

/**
 * Evidence payload submitted by the player to verify redemption completion.
 */
data class RedemptionVerificationEvidence(
    val evidenceId: String = UUID.randomUUID().toString(),
    val redemptionId: String,
    val accountOwnerId: String,
    val submittedAtEpochMs: Long = System.currentTimeMillis(),
    val reflectionText: String = "",
    val checklistItemsConfirmed: List<String> = emptyList(),
    val externalSessionRefId: String? = null,
    val userConfirmedCompliance: Boolean = true
)

/**
 * Verification outcome evaluated by the RedemptionVerificationEngine.
 */
data class VerificationOutcome(
    val isApproved: Boolean,
    val pointsResolved: Int,
    val rejectionReason: String? = null,
    val auditNotes: String = ""
)

/**
 * Result of resolving a penalty via verified redemption.
 */
data class PenaltyResolutionResult(
    val success: Boolean,
    val penaltyId: String,
    val redemptionId: String,
    val pointsResolved: Int,
    val remainingPenaltyBalance: Int,
    val newAccountBalance: Int,
    val penaltyFullyResolved: Boolean,
    val message: String
)
