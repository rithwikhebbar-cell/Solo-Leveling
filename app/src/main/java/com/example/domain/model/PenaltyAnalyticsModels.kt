package com.example.domain.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

/**
 * Top-level KPI overview of the player's Penalty Point balance,
 * accrued/deducted ledger totals, active/resolved counts, and recovery rate.
 */
data class PenaltyOverviewSummary(
    val accountOwnerId: String,
    val currentBalance: Int = 0,
    val lifetimePointsAccrued: Int = 0,
    val lifetimePointsDeducted: Int = 0,
    val activePenaltiesCount: Int = 0,
    val resolvedPenaltiesCount: Int = 0,
    val exemptedPenaltiesCount: Int = 0,
    val recoveryRatePercent: Double = 0.0
)

/**
 * Deterministic analysis of failure trends, category distributions,
 * temporal clusters (time of day, day of week), and commitment rates.
 */
data class FailureTrendAnalysis(
    val totalCommitments: Int = 0,
    val successfulCommitments: Int = 0,
    val failedCommitments: Int = 0,
    val exemptedCommitments: Int = 0,
    val completionRatePercent: Double = 0.0,
    val failureRatePercent: Double = 0.0,
    val failuresByCategory: Map<String, Int> = emptyMap(),
    val failuresByTimeOfDay: Map<String, Int> = emptyMap(),
    val failuresByDayOfWeek: Map<String, Int> = emptyMap()
)

/**
 * Quantitative metrics measuring player engagement and success with
 * constructive corrective redemption activities.
 */
data class RedemptionPerformanceMetrics(
    val activitiesAssigned: Int = 0,
    val activitiesCompleted: Int = 0,
    val activitiesRescheduled: Int = 0,
    val activitiesExpired: Int = 0,
    val redemptionSuccessRatePercent: Double = 0.0,
    val averageResolutionHours: Double = 0.0
)

/**
 * Consistency indicators tracking clean streaks without quest failure.
 */
data class ConsistencyMetrics(
    val currentCleanStreakDays: Int = 0,
    val longestCleanStreakDays: Int = 0,
    val lastFailureEpochMs: Long? = null,
    val streakTrend: ConsistencyTrend = ConsistencyTrend.STABLE
)

enum class ConsistencyTrend(val label: String) {
    IMPROVING("Improving"),
    STABLE("Stable"),
    AT_RISK("At Risk"),
    RECOVERING("Recovering")
}

/**
 * Categorization of structural friction patterns detected in commitment data.
 */
enum class FailurePatternType(val label: String) {
    EXCESSIVE_DAILY_COMMITMENTS("Excessive Daily Commitments"),
    REPEATED_MISSED_DEADLINES("Repeated Missed Deadlines"),
    REPEATED_RESCHEDULING("Repeated Rescheduling"),
    ACADEMIC_RECOVERY_CONFLICT("Academic/Recovery Schedule Conflict"),
    HIGH_FATIGUE_OVEREXERTION("High Fatigue Overexertion")
}

enum class PatternSeverity(val label: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High")
}

/**
 * Deterministic detection result identifying actionable systemic friction
 * with constructive, non-punitive recommendations.
 */
data class DetectedFailurePattern(
    val patternId: String = UUID.randomUUID().toString(),
    val patternType: FailurePatternType,
    val severity: PatternSeverity,
    val evidenceCount: Int,
    val title: String,
    val description: String,
    val actionableRecommendation: String,
    val detectedAtEpochMs: Long = System.currentTimeMillis()
)

enum class PenaltyTimelineType(val label: String) {
    COMMITMENT("Commitment"),
    FAILURE("Failure Confirmed"),
    ALLOCATION("Penalty Allocated"),
    ESCALATION("Escalation Bonus"),
    REDEMPTION_START("Redemption Started"),
    REDEMPTION_VERIFICATION("Verification Recorded"),
    PENALTY_RESOLUTION("Penalty Resolved"),
    EXEMPTION_REQUEST("Exemption Requested"),
    EXEMPTION_APPROVAL("Exemption Approved"),
    EXEMPTION_DENIAL("Exemption Denied"),
    REVERSAL("Penalty Reversal")
}

/**
 * Chronological audit timeline item tracing complete quest commitment to penalty resolution.
 */
data class PenaltyAuditTimelineItem(
    val id: String,
    val timestampEpochMs: Long,
    val type: PenaltyTimelineType,
    val title: String,
    val detail: String,
    val statusTag: String
)

enum class CorrectiveActivityLifecycleState {
    PENDING,
    ACTIVE,
    COMPLETED,
    VERIFIED,
    EXPIRED,
    CANCELLED
}

enum class VerificationMethod {
    STRUCTURED_REFLECTION,
    CHECKLIST_CONFIRMATION,
    PHYSICAL_SESSION_VERIFIED,
    ACADEMIC_STUDY_SESSION,
    USER_ATTESTATION
}

data class CorrectiveActivityRecord(
    val activityId: String = "",
    val penaltyId: String = "",
    val accountOwnerId: String = "",
    val title: String = "",
    val description: String = "",
    val category: QuestCategory = QuestCategory.DISCIPLINE,
    val pointValue: Int = 0,
    val assignedShadowClass: ShadowClass = ShadowClass.VANGUARD,
    val verificationMethod: VerificationMethod = VerificationMethod.STRUCTURED_REFLECTION,
    val lifecycleState: CorrectiveActivityLifecycleState = CorrectiveActivityLifecycleState.PENDING,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val completedAtEpochMs: Long? = null,
    val verifiedAtEpochMs: Long? = null,
    val rescheduleCount: Int = 0
)

fun RedemptionPlan.toCorrectiveActivityRecord(): CorrectiveActivityRecord {
    return CorrectiveActivityRecord(
        activityId = activityId,
        penaltyId = penaltyId,
        accountOwnerId = accountOwnerId,
        title = activityTitle,
        description = instructions,
        category = when (category) {
            CorrectiveCategory.ACADEMIC -> QuestCategory.INTELLECT
            CorrectiveCategory.PHYSICAL -> QuestCategory.PHYSICAL
            CorrectiveCategory.DISCIPLINE -> QuestCategory.DISCIPLINE
            CorrectiveCategory.LIFE_MANAGEMENT -> QuestCategory.SOCIAL
            CorrectiveCategory.STRATEGIC_PLANNING -> QuestCategory.INTELLECT
        },
        pointValue = resolutionPoints,
        assignedShadowClass = assignedShadowClass,
        verificationMethod = when (verificationMethod) {
            RedemptionVerificationMethod.STRUCTURED_REFLECTION -> VerificationMethod.STRUCTURED_REFLECTION
            RedemptionVerificationMethod.CHECKLIST_CONFIRMATION -> VerificationMethod.CHECKLIST_CONFIRMATION
            RedemptionVerificationMethod.PHYSICAL_SESSION_VERIFIED -> VerificationMethod.PHYSICAL_SESSION_VERIFIED
            RedemptionVerificationMethod.ACADEMIC_STUDY_VERIFIED -> VerificationMethod.ACADEMIC_STUDY_SESSION
            else -> VerificationMethod.USER_ATTESTATION
        },
        lifecycleState = when (lifecycleState) {
            RedemptionLifecycleState.VERIFIED_COMPLETED -> CorrectiveActivityLifecycleState.VERIFIED
            RedemptionLifecycleState.CANCELLED, RedemptionLifecycleState.REJECTED -> CorrectiveActivityLifecycleState.CANCELLED
            RedemptionLifecycleState.ACTIVE -> CorrectiveActivityLifecycleState.ACTIVE
            else -> CorrectiveActivityLifecycleState.PENDING
        },
        createdAtEpochMs = createdAtEpochMs,
        completedAtEpochMs = completedAtEpochMs,
        verifiedAtEpochMs = verifiedAtEpochMs
    )
}

