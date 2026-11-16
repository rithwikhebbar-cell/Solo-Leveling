package com.example.domain.model

import java.time.DayOfWeek

enum class DataProvenanceTag(val label: String) {
    MEASURED("Measured"),
    SELF_REPORTED("Self-Reported"),
    ESTIMATE("Estimate"),
    PREDICTION("Prediction")
}

enum class ConfidenceLevel(val label: String) {
    HIGH("High Confidence"),
    MODERATE("Moderate Confidence"),
    LOW("Low Confidence"),
    INSUFFICIENT_DATA("Insufficient Data")
}

enum class WorkloadLevel(val label: String) {
    UNDERLOADED("Underloaded"),
    BALANCED("Balanced"),
    HIGH_WORKLOAD("High Workload"),
    OVERLOADED("Overloaded"),
    RECOVERY_PRIORITY("Recovery Priority")
}

enum class FindingKind(val label: String) {
    STRENGTH("Strength"),
    WEAKNESS("Weakness"),
    BOTTLENECK("Bottleneck"),
    RISK("Risk")
}

enum class FindingCategory(val label: String) {
    PHYSICAL("Physical"),
    ACADEMIC("Academic"),
    DISCIPLINE("Discipline"),
    RPG("RPG Systems"),
    RECOVERY("Recovery"),
    DATA_COMPLETENESS("Data Completeness")
}

enum class RecommendationCategory(val label: String) {
    PHYSICAL("Physical"),
    ACADEMIC("Academic"),
    DISCIPLINE("Discipline"),
    PERSONAL_DEVELOPMENT("Personal Development"),
    RPG("RPG")
}

enum class RecommendationStatus(val label: String) {
    NEW("New"),
    ACCEPTED("Accepted"),
    REJECTED("Rejected"),
    COMPLETED("Completed"),
    EXPIRED("Expired")
}

enum class RoadmapHorizon(val days: Int, val label: String) {
    SEVEN_DAYS(7, "7-Day Sprint"),
    THIRTY_DAYS(30, "30-Day Cycle"),
    NINETY_DAYS(90, "90-Day Arc"),
    SIX_MONTHS(180, "6-Month Mastery"),
    ONE_YEAR(365, "1-Year Ascension")
}

enum class PlanningStyle(val label: String, val maxLoadFactor: Float) {
    CONSERVATIVE("Conservative", 0.75f),
    BALANCED("Balanced", 1.0f),
    AMBITIOUS("Ambitious", 1.2f)
}

enum class NotificationIntensity(val label: String) {
    LOW("Minimal"),
    MODERATE("Balanced"),
    HIGH("Proactive")
}

enum class AdaptivePlanBlockStatus(val label: String) {
    PENDING("Pending"),
    APPROVED("Scheduled"),
    PARTIAL("Partial"),
    COMPLETED("Completed"),
    RESCHEDULED("Rescheduled"),
    SKIPPED("Skipped")
}

enum class ReportPeriodType(val label: String) {
    WEEKLY("Weekly Report"),
    MONTHLY("Monthly Report")
}

data class PlayerIntelligenceProfile(
    val sourceFingerprint: String = "",
    val evaluatedAtEpochMs: Long = System.currentTimeMillis(),
    val playerLevel: Int = 1,
    val hunterRank: HunterRank = HunterRank.E_RANK,
    val physicalActivityScore: Int = 0,
    val academicActivityScore: Int = 0,
    val disciplineConsistencyScore: Int = 0,
    val rpgEngagementScore: Int = 0,
    val behavioralPeakWindow: String = "Insufficient timestamp history",
    val averageSessionMinutes: Int = 0,
    val measuredDataRatioPercent: Int = 0,
    val activeDaysLast30: Int = 0,
    val hasMinimumBaseline: Boolean = false,
    val dataCoverageNotes: List<String> = emptyList(),
    val dataCompletenessPercent: Int = 0,
    val provenanceSummary: String = "Calculated strictly from stored Room activity, academic, life, and RPG records."
)

data class StrengthWeaknessFinding(
    val id: Long = 0,
    val findingUuid: String,
    val kind: FindingKind,
    val category: FindingCategory,
    val title: String,
    val description: String,
    val evidenceSummary: String,
    val dateRangeLabel: String,
    val provenance: DataProvenanceTag,
    val confidence: ConfidenceLevel,
    val priorityRank: Int = 1,
    val insufficientData: Boolean = false,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

data class AdaptivePlanBlock(
    val id: Long = 0,
    val blockUuid: String,
    val planUuid: String,
    val title: String,
    val module: String,
    val scheduledDateEpochDay: Long,
    val startMinuteOfDay: Int,
    val durationMinutes: Int,
    val priorityWeight: Int,
    val priorityScore: Int,
    val priorityReason: String,
    val dependencies: String = "",
    val status: AdaptivePlanBlockStatus = AdaptivePlanBlockStatus.PENDING,
    val completedMinutes: Int = 0,
    val isLocked: Boolean = false,
    val linkedSourceType: ScheduledSourceType = ScheduledSourceType.AI_PLAN_PROPOSAL,
    val linkedSourceId: Long = 0L,
    val isRecoveryProtected: Boolean = false
) {
    val endMinuteOfDay: Int get() = (startMinuteOfDay + durationMinutes).coerceAtMost(24 * 60)

    fun formattedWindow(): String {
        val sh = (startMinuteOfDay / 60).coerceIn(0, 23)
        val sm = (startMinuteOfDay % 60).coerceIn(0, 59)
        val eh = (endMinuteOfDay / 60).coerceIn(0, 24)
        val em = (endMinuteOfDay % 60).coerceIn(0, 59)
        return "%02d:%02d – %02d:%02d".format(sh, sm, eh, em)
    }
}

data class AdaptiveDailyPlan(
    val id: Long = 0,
    val planUuid: String,
    val targetDateEpochDay: Long,
    val generatedAtEpochMs: Long,
    val workloadLevel: WorkloadLevel,
    val totalPlannedMinutes: Int,
    val availableMinutes: Int,
    val isApproved: Boolean = false,
    val isPartialDay: Boolean = false,
    val notes: String = "",
    val blocks: List<AdaptivePlanBlock> = emptyList()
)

data class WorkloadAssessment(
    val id: Long = 0,
    val assessmentUuid: String,
    val dateEpochDay: Long,
    val workloadLevel: WorkloadLevel,
    val loadScore: Int,
    val physicalLoadMinutes48h: Int,
    val academicLoadMinutesToday: Int,
    val activeCommitmentsCount: Int,
    val overdueItemsCount: Int,
    val scheduleConflictCount: Int,
    val recoveryState: RecoveryState,
    val adjustmentDirective: String,
    val preservesRestWithoutPunishment: Boolean = true,
    val provenance: DataProvenanceTag = DataProvenanceTag.ESTIMATE,
    val evaluatedAtEpochMs: Long = System.currentTimeMillis()
)

data class TrendDataPoint(
    val label: String,
    val valuePercent: Int,
    val isEstimate: Boolean = false
)

data class ConsistencyPrediction(
    val id: Long = 0,
    val predictionUuid: String,
    val domainLabel: String,
    val horizonDays: Int,
    val currentConsistencyPercent: Int,
    val predictedConsistencyPercent: Int,
    val slopePerWeek: Float,
    val confidence: ConfidenceLevel,
    val insufficientData: Boolean,
    val limitationNote: String,
    val atRiskGoals: List<String> = emptyList(),
    val riskLevelLabel: String,
    val trendSeries: List<TrendDataPoint> = emptyList(),
    val provenance: DataProvenanceTag = DataProvenanceTag.PREDICTION,
    val evaluatedAtEpochMs: Long = System.currentTimeMillis()
)

data class PersonalizedRecommendation(
    val id: Long = 0,
    val recUuid: String,
    val category: RecommendationCategory,
    val title: String,
    val description: String,
    val rationale: String,
    val expectedImpact: String,
    val actionLabel: String,
    val deepLinkRoute: String,
    val priorityScore: Int,
    val status: RecommendationStatus = RecommendationStatus.NEW,
    val provenance: DataProvenanceTag = DataProvenanceTag.ESTIMATE,
    val confidence: ConfidenceLevel = ConfidenceLevel.MODERATE,
    val expiresAtEpochMs: Long,
    val cooldownUntilEpochMs: Long,
    val sourceFingerprint: String,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val respondedAtEpochMs: Long? = null
)

data class RecommendationFeedbackRecord(
    val id: Long = 0,
    val recUuid: String,
    val category: RecommendationCategory,
    val action: RecommendationStatus,
    val feedbackNote: String = "",
    val timestampEpochMs: Long = System.currentTimeMillis()
)

data class SubjectMasteryInsight(
    val subjectId: Long,
    val subjectName: String,
    val measuredStudyMinutes: Int,
    val assessmentAveragePercent: Int?,
    val selfReportedConfidenceAvg: Float?,
    val estimatedFamiliarityPercent: Int,
    val overdueRevisionsCount: Int,
    val dueTodayRevisionsCount: Int,
    val gapSummary: String
)

data class AcademicLearningIntelligence(
    val generatedAtEpochMs: Long = System.currentTimeMillis(),
    val totalStudyMinutes7d: Int = 0,
    val totalRevisions7d: Int = 0,
    val overdueRevisionsCount: Int = 0,
    val verifiedStudyRatioPercent: Int = 0,
    val measuredAssessmentAvgPercent: Int? = null,
    val selfReportedConfidenceAvg: Float? = null,
    val estimatedRetentionAvgPercent: Int = 0,
    val examCountdowns: List<String> = emptyList(),
    val subjectInsights: List<SubjectMasteryInsight> = emptyList(),
    val recommendedRevisionPriorities: List<String> = emptyList(),
    val limitations: List<String> = emptyList()
)

data class PhysicalDevelopmentIntelligence(
    val generatedAtEpochMs: Long = System.currentTimeMillis(),
    val measuredLoadMinutes48h: Int = 0,
    val measuredLoadMinutes7d: Int = 0,
    val weeklyJoggingMinutes: Int = 0,
    val weeklyDistanceKm: Double = 0.0,
    val weeklyWorkoutCount: Int = 0,
    val avgHeartRateBpm: Int? = null,
    val measuredSleepHoursLastNight: Double? = null,
    val recoveryState: RecoveryState = RecoveryState.READY,
    val readinessScore: Int = 100,
    val trainingTrendLabel: String = "Baseline Pending",
    val intensitySuggestion: String = "Maintain comfortable aerobic baseline.",
    val restEnforcementActive: Boolean = false,
    val safetyDisclaimer: String = "Wellness & training load guidance only — not medical advice.",
    val dataLimitations: List<String> = emptyList()
)

data class AdaptiveDungeonRecommendation(
    val dungeonId: Long,
    val code: String,
    val title: String,
    val difficulty: DungeonDifficulty,
    val category: DungeonCategory,
    val isEligible: Boolean,
    val eligibilityBlockers: List<String>,
    val suitabilityScore: Int,
    val recommendationLabel: String,
    val rationale: String,
    val recoverySafe: Boolean,
    val workloadSafe: Boolean,
    val completionRateHistoryPercent: Int?
)

data class RoadmapMilestone(
    val id: Long = 0,
    val milestoneUuid: String,
    val roadmapUuid: String,
    val title: String,
    val domain: String,
    val targetDayOffset: Int,
    val targetDateEpochDay: Long,
    val isCompleted: Boolean = false,
    val isUserEdited: Boolean = false,
    val orderIndex: Int = 0,
    val notes: String = ""
)

data class DevelopmentRoadmap(
    val id: Long = 0,
    val roadmapUuid: String,
    val title: String,
    val horizon: RoadmapHorizon,
    val visionSummary: String,
    val focusDomains: List<String>,
    val dependenciesSummary: String,
    val riskNotes: String,
    val nextReviewDateEpochDay: Long,
    val isApproved: Boolean = false,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis(),
    val milestones: List<RoadmapMilestone> = emptyList()
)

data class IntelligenceReport(
    val id: Long = 0,
    val reportUuid: String,
    val periodType: ReportPeriodType,
    val periodLabel: String,
    val periodStartEpochDay: Long,
    val periodEndEpochDay: Long,
    val activeDaysObserved: Int,
    val isPartialDataWindow: Boolean,
    val measuredStatsSummary: String,
    val selfReportedStatsSummary: String,
    val workoutsCount: Int,
    val joggingMinutes: Int,
    val studyMinutes: Int,
    val revisionsCount: Int,
    val habitsCompletedCount: Int,
    val tasksCompletedCount: Int,
    val dungeonsClearedCount: Int,
    val xpGainedWindow: Int,
    val deterministicHighlights: List<String>,
    val deterministicRisks: List<String>,
    val aiNarrativeSummary: String? = null,
    val aiUsedFallback: Boolean = true,
    val generatedAtEpochMs: Long = System.currentTimeMillis()
)

data class PlayerIntelligencePreferences(
    val planningStyle: PlanningStyle = PlanningStyle.BALANCED,
    val preferredStudyHour: Int = 9,
    val preferredWorkoutHour: Int = 7,
    val maxDailyFocusMinutes: Int = 180,
    val maxDailyWorkoutMinutes: Int = 75,
    val maxRecommendationsPerDay: Int = 5,
    val notificationIntensity: NotificationIntensity = NotificationIntensity.MODERATE,
    val enableProactivePlanning: Boolean = true,
    val enableWeeklyReports: Boolean = true,
    val enableMonthlyReports: Boolean = true,
    val enableAdaptiveDungeons: Boolean = true,
    val shareIntelligenceWithCloudAi: Boolean = false,
    val sharePhysicalWithIntelligence: Boolean = true,
    val shareAcademicWithIntelligence: Boolean = true,
    val shareLifeWithIntelligence: Boolean = true,
    val shareRpgWithIntelligence: Boolean = true,
    val restDays: Set<DayOfWeek> = setOf(DayOfWeek.SUNDAY),
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

data class AdvisorConsultationResult(
    val id: Long = 0,
    val consultationUuid: String,
    val question: String,
    val deterministicSummary: String,
    val aiResponse: String,
    val provenanceTags: List<DataProvenanceTag>,
    val confidence: ConfidenceLevel,
    val connectionStatus: AiConnectionStatus,
    val usedFallback: Boolean,
    val limitationNotes: List<String>,
    val timestampEpochMs: Long = System.currentTimeMillis()
)
