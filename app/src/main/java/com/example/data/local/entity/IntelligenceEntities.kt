package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.AdaptiveDailyPlan
import com.example.domain.model.AdaptivePlanBlock
import com.example.domain.model.AdaptivePlanBlockStatus
import com.example.domain.model.AdvisorConsultationResult
import com.example.domain.model.AiConnectionStatus
import com.example.domain.model.ConfidenceLevel
import com.example.domain.model.ConsistencyPrediction
import com.example.domain.model.DataProvenanceTag
import com.example.domain.model.DevelopmentRoadmap
import com.example.domain.model.FindingCategory
import com.example.domain.model.FindingKind
import com.example.domain.model.HunterRank
import com.example.domain.model.IntelligenceReport
import com.example.domain.model.NotificationIntensity
import com.example.domain.model.PersonalizedRecommendation
import com.example.domain.model.PlanningStyle
import com.example.domain.model.PlayerIntelligencePreferences
import com.example.domain.model.PlayerIntelligenceProfile
import com.example.domain.model.RecommendationCategory
import com.example.domain.model.RecommendationFeedbackRecord
import com.example.domain.model.RecommendationStatus
import com.example.domain.model.RecoveryState
import com.example.domain.model.ReportPeriodType
import com.example.domain.model.RoadmapHorizon
import com.example.domain.model.RoadmapMilestone
import com.example.domain.model.ScheduledSourceType
import com.example.domain.model.StrengthWeaknessFinding
import com.example.domain.model.TrendDataPoint
import com.example.domain.model.WorkloadAssessment
import com.example.domain.model.WorkloadLevel
import com.example.domain.model.parseDayOfWeekCsv
import com.example.domain.model.toCsvString

// 1. IntelligenceProfileEntity (singleton snapshot id = 1)
@Entity(tableName = "intelligence_profiles")
data class IntelligenceProfileEntity(
    @PrimaryKey val id: Int = 1,
    val sourceFingerprint: String,
    val evaluatedAtEpochMs: Long,
    val playerLevel: Int,
    val hunterRankName: String,
    val physicalActivityScore: Int,
    val academicActivityScore: Int,
    val disciplineConsistencyScore: Int,
    val rpgEngagementScore: Int,
    val behavioralPeakWindow: String,
    val averageSessionMinutes: Int,
    val measuredDataRatioPercent: Int,
    val activeDaysLast30: Int,
    val hasMinimumBaseline: Boolean,
    val dataCoverageNotesJoined: String,
    val dataCompletenessPercent: Int,
    val provenanceSummary: String
)

// 2. StrengthWeaknessFindingEntity
@Entity(
    tableName = "strength_weakness_findings",
    indices = [Index(value = ["findingUuid"], unique = true), Index(value = ["priorityRank"])]
)
data class StrengthWeaknessFindingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val findingUuid: String,
    val kindName: String,
    val categoryName: String,
    val title: String,
    val description: String,
    val evidenceSummary: String,
    val dateRangeLabel: String,
    val provenanceName: String,
    val confidenceName: String,
    val priorityRank: Int,
    val insufficientData: Boolean,
    val createdAtEpochMs: Long
)

// 3. AdaptiveDailyPlanEntity
@Entity(
    tableName = "adaptive_daily_plans",
    indices = [
        Index(value = ["planUuid"], unique = true),
        Index(value = ["targetDateEpochDay"])
    ]
)
data class AdaptiveDailyPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planUuid: String,
    val targetDateEpochDay: Long,
    val generatedAtEpochMs: Long,
    val workloadLevelName: String,
    val totalPlannedMinutes: Int,
    val availableMinutes: Int,
    val isApproved: Boolean,
    val isPartialDay: Boolean,
    val notes: String
)

// 4. AdaptivePlanBlockEntity
@Entity(
    tableName = "adaptive_plan_blocks",
    foreignKeys = [
        ForeignKey(
            entity = AdaptiveDailyPlanEntity::class,
            parentColumns = ["planUuid"],
            childColumns = ["planUuid"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["blockUuid"], unique = true),
        Index(value = ["planUuid"]),
        Index(value = ["scheduledDateEpochDay", "startMinuteOfDay"])
    ]
)
data class AdaptivePlanBlockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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
    val dependencies: String,
    val statusName: String,
    val completedMinutes: Int,
    val isLocked: Boolean,
    val linkedSourceTypeName: String,
    val linkedSourceId: Long,
    val isRecoveryProtected: Boolean
)

// 5. WorkloadAssessmentEntity
@Entity(
    tableName = "workload_assessments",
    indices = [
        Index(value = ["assessmentUuid"], unique = true),
        Index(value = ["dateEpochDay"])
    ]
)
data class WorkloadAssessmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val assessmentUuid: String,
    val dateEpochDay: Long,
    val workloadLevelName: String,
    val loadScore: Int,
    val physicalLoadMinutes48h: Int,
    val academicLoadMinutesToday: Int,
    val activeCommitmentsCount: Int,
    val overdueItemsCount: Int,
    val scheduleConflictCount: Int,
    val recoveryStateName: String,
    val adjustmentDirective: String,
    val preservesRestWithoutPunishment: Boolean,
    val provenanceName: String,
    val evaluatedAtEpochMs: Long
)

// 6. ConsistencyPredictionEntity
@Entity(
    tableName = "consistency_predictions",
    indices = [Index(value = ["predictionUuid"], unique = true)]
)
data class ConsistencyPredictionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val predictionUuid: String,
    val domainLabel: String,
    val horizonDays: Int,
    val currentConsistencyPercent: Int,
    val predictedConsistencyPercent: Int,
    val slopePerWeek: Float,
    val confidenceName: String,
    val insufficientData: Boolean,
    val limitationNote: String,
    val atRiskGoalsJoined: String,
    val riskLevelLabel: String,
    val trendSeriesEncoded: String,
    val provenanceName: String,
    val evaluatedAtEpochMs: Long
)

// 7. PersonalizedRecommendationEntity
@Entity(
    tableName = "personalized_recommendations",
    indices = [
        Index(value = ["recUuid"], unique = true),
        Index(value = ["statusName"]),
        Index(value = ["priorityScore"])
    ]
)
data class PersonalizedRecommendationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recUuid: String,
    val categoryName: String,
    val title: String,
    val description: String,
    val rationale: String,
    val expectedImpact: String,
    val actionLabel: String,
    val deepLinkRoute: String,
    val priorityScore: Int,
    val statusName: String,
    val provenanceName: String,
    val confidenceName: String,
    val expiresAtEpochMs: Long,
    val cooldownUntilEpochMs: Long,
    val sourceFingerprint: String,
    val createdAtEpochMs: Long,
    val respondedAtEpochMs: Long?
)

// 8. RecommendationFeedbackEntity
@Entity(
    tableName = "recommendation_feedback",
    indices = [Index(value = ["recUuid"]), Index(value = ["timestampEpochMs"])]
)
data class RecommendationFeedbackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recUuid: String,
    val categoryName: String,
    val actionName: String,
    val feedbackNote: String,
    val timestampEpochMs: Long
)

// 9. DevelopmentRoadmapEntity
@Entity(
    tableName = "development_roadmaps",
    indices = [Index(value = ["roadmapUuid"], unique = true), Index(value = ["updatedAtEpochMs"])]
)
data class DevelopmentRoadmapEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val roadmapUuid: String,
    val title: String,
    val horizonName: String,
    val visionSummary: String,
    val focusDomainsJoined: String,
    val dependenciesSummary: String,
    val riskNotes: String,
    val nextReviewDateEpochDay: Long,
    val isApproved: Boolean,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)

// 10. RoadmapMilestoneEntity
@Entity(
    tableName = "roadmap_milestones",
    foreignKeys = [
        ForeignKey(
            entity = DevelopmentRoadmapEntity::class,
            parentColumns = ["roadmapUuid"],
            childColumns = ["roadmapUuid"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["milestoneUuid"], unique = true),
        Index(value = ["roadmapUuid"])
    ]
)
data class RoadmapMilestoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val milestoneUuid: String,
    val roadmapUuid: String,
    val title: String,
    val domain: String,
    val targetDayOffset: Int,
    val targetDateEpochDay: Long,
    val isCompleted: Boolean,
    val isUserEdited: Boolean,
    val orderIndex: Int,
    val notes: String
)

// 11. IntelligenceReportEntity
@Entity(
    tableName = "intelligence_reports",
    indices = [
        Index(value = ["reportUuid"], unique = true),
        Index(value = ["periodTypeName", "generatedAtEpochMs"])
    ]
)
data class IntelligenceReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reportUuid: String,
    val periodTypeName: String,
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
    val highlightsJoined: String,
    val risksJoined: String,
    val aiNarrativeSummary: String?,
    val aiUsedFallback: Boolean,
    val generatedAtEpochMs: Long
)

// 12. IntelligencePreferencesEntity (singleton id = 1)
@Entity(tableName = "intelligence_preferences")
data class IntelligencePreferencesEntity(
    @PrimaryKey val id: Int = 1,
    val planningStyleName: String = PlanningStyle.BALANCED.name,
    val preferredStudyHour: Int = 9,
    val preferredWorkoutHour: Int = 7,
    val maxDailyFocusMinutes: Int = 180,
    val maxDailyWorkoutMinutes: Int = 75,
    val maxRecommendationsPerDay: Int = 5,
    val notificationIntensityName: String = NotificationIntensity.MODERATE.name,
    val enableProactivePlanning: Boolean = true,
    val enableWeeklyReports: Boolean = true,
    val enableMonthlyReports: Boolean = true,
    val enableAdaptiveDungeons: Boolean = true,
    val shareIntelligenceWithCloudAi: Boolean = false,
    val sharePhysicalWithIntelligence: Boolean = true,
    val shareAcademicWithIntelligence: Boolean = true,
    val shareLifeWithIntelligence: Boolean = true,
    val shareRpgWithIntelligence: Boolean = true,
    val restDaysCsv: String = "7",
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

// 13. AdvisorConsultationEntity
@Entity(
    tableName = "advisor_consultations",
    indices = [
        Index(value = ["consultationUuid"], unique = true),
        Index(value = ["timestampEpochMs"])
    ]
)
data class AdvisorConsultationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val consultationUuid: String,
    val question: String,
    val deterministicSummary: String,
    val aiResponse: String,
    val provenanceTagsJoined: String,
    val confidenceName: String,
    val connectionStatusName: String,
    val usedFallback: Boolean,
    val limitationNotesJoined: String,
    val timestampEpochMs: Long
)

// ============================================================================
// DOMAIN MAPPERS
// ============================================================================

fun IntelligenceProfileEntity.toDomain() = PlayerIntelligenceProfile(
    sourceFingerprint = sourceFingerprint,
    evaluatedAtEpochMs = evaluatedAtEpochMs,
    playerLevel = playerLevel,
    hunterRank = runCatching { HunterRank.valueOf(hunterRankName) }.getOrDefault(HunterRank.E_RANK),
    physicalActivityScore = physicalActivityScore,
    academicActivityScore = academicActivityScore,
    disciplineConsistencyScore = disciplineConsistencyScore,
    rpgEngagementScore = rpgEngagementScore,
    behavioralPeakWindow = behavioralPeakWindow,
    averageSessionMinutes = averageSessionMinutes,
    measuredDataRatioPercent = measuredDataRatioPercent,
    activeDaysLast30 = activeDaysLast30,
    hasMinimumBaseline = hasMinimumBaseline,
    dataCoverageNotes = dataCoverageNotesJoined.split("||").filter { it.isNotBlank() },
    dataCompletenessPercent = dataCompletenessPercent,
    provenanceSummary = provenanceSummary
)

fun PlayerIntelligenceProfile.toEntity() = IntelligenceProfileEntity(
    id = 1,
    sourceFingerprint = sourceFingerprint,
    evaluatedAtEpochMs = evaluatedAtEpochMs,
    playerLevel = playerLevel,
    hunterRankName = hunterRank.name,
    physicalActivityScore = physicalActivityScore,
    academicActivityScore = academicActivityScore,
    disciplineConsistencyScore = disciplineConsistencyScore,
    rpgEngagementScore = rpgEngagementScore,
    behavioralPeakWindow = behavioralPeakWindow,
    averageSessionMinutes = averageSessionMinutes,
    measuredDataRatioPercent = measuredDataRatioPercent,
    activeDaysLast30 = activeDaysLast30,
    hasMinimumBaseline = hasMinimumBaseline,
    dataCoverageNotesJoined = dataCoverageNotes.joinToString("||"),
    dataCompletenessPercent = dataCompletenessPercent,
    provenanceSummary = provenanceSummary
)

fun StrengthWeaknessFindingEntity.toDomain() = StrengthWeaknessFinding(
    id = id,
    findingUuid = findingUuid,
    kind = runCatching { FindingKind.valueOf(kindName) }.getOrDefault(FindingKind.RISK),
    category = runCatching { FindingCategory.valueOf(categoryName) }.getOrDefault(FindingCategory.DATA_COMPLETENESS),
    title = title,
    description = description,
    evidenceSummary = evidenceSummary,
    dateRangeLabel = dateRangeLabel,
    provenance = runCatching { DataProvenanceTag.valueOf(provenanceName) }.getOrDefault(DataProvenanceTag.ESTIMATE),
    confidence = runCatching { ConfidenceLevel.valueOf(confidenceName) }.getOrDefault(ConfidenceLevel.LOW),
    priorityRank = priorityRank,
    insufficientData = insufficientData,
    createdAtEpochMs = createdAtEpochMs
)

fun StrengthWeaknessFinding.toEntity() = StrengthWeaknessFindingEntity(
    id = id,
    findingUuid = findingUuid,
    kindName = kind.name,
    categoryName = category.name,
    title = title,
    description = description,
    evidenceSummary = evidenceSummary,
    dateRangeLabel = dateRangeLabel,
    provenanceName = provenance.name,
    confidenceName = confidence.name,
    priorityRank = priorityRank,
    insufficientData = insufficientData,
    createdAtEpochMs = createdAtEpochMs
)

fun AdaptivePlanBlockEntity.toDomain() = AdaptivePlanBlock(
    id = id,
    blockUuid = blockUuid,
    planUuid = planUuid,
    title = title,
    module = module,
    scheduledDateEpochDay = scheduledDateEpochDay,
    startMinuteOfDay = startMinuteOfDay,
    durationMinutes = durationMinutes,
    priorityWeight = priorityWeight,
    priorityScore = priorityScore,
    priorityReason = priorityReason,
    dependencies = dependencies,
    status = runCatching { AdaptivePlanBlockStatus.valueOf(statusName) }.getOrDefault(AdaptivePlanBlockStatus.PENDING),
    completedMinutes = completedMinutes,
    isLocked = isLocked,
    linkedSourceType = runCatching { ScheduledSourceType.valueOf(linkedSourceTypeName) }.getOrDefault(ScheduledSourceType.AI_PLAN_PROPOSAL),
    linkedSourceId = linkedSourceId,
    isRecoveryProtected = isRecoveryProtected
)

fun AdaptivePlanBlock.toEntity() = AdaptivePlanBlockEntity(
    id = id,
    blockUuid = blockUuid,
    planUuid = planUuid,
    title = title,
    module = module,
    scheduledDateEpochDay = scheduledDateEpochDay,
    startMinuteOfDay = startMinuteOfDay,
    durationMinutes = durationMinutes,
    priorityWeight = priorityWeight,
    priorityScore = priorityScore,
    priorityReason = priorityReason,
    dependencies = dependencies,
    statusName = status.name,
    completedMinutes = completedMinutes,
    isLocked = isLocked,
    linkedSourceTypeName = linkedSourceType.name,
    linkedSourceId = linkedSourceId,
    isRecoveryProtected = isRecoveryProtected
)

fun AdaptiveDailyPlanEntity.toDomain(blocks: List<AdaptivePlanBlock>) = AdaptiveDailyPlan(
    id = id,
    planUuid = planUuid,
    targetDateEpochDay = targetDateEpochDay,
    generatedAtEpochMs = generatedAtEpochMs,
    workloadLevel = runCatching { WorkloadLevel.valueOf(workloadLevelName) }.getOrDefault(WorkloadLevel.BALANCED),
    totalPlannedMinutes = totalPlannedMinutes,
    availableMinutes = availableMinutes,
    isApproved = isApproved,
    isPartialDay = isPartialDay,
    notes = notes,
    blocks = blocks
)

fun AdaptiveDailyPlan.toEntity() = AdaptiveDailyPlanEntity(
    id = id,
    planUuid = planUuid,
    targetDateEpochDay = targetDateEpochDay,
    generatedAtEpochMs = generatedAtEpochMs,
    workloadLevelName = workloadLevel.name,
    totalPlannedMinutes = totalPlannedMinutes,
    availableMinutes = availableMinutes,
    isApproved = isApproved,
    isPartialDay = isPartialDay,
    notes = notes
)

fun WorkloadAssessmentEntity.toDomain() = WorkloadAssessment(
    id = id,
    assessmentUuid = assessmentUuid,
    dateEpochDay = dateEpochDay,
    workloadLevel = runCatching { WorkloadLevel.valueOf(workloadLevelName) }.getOrDefault(WorkloadLevel.BALANCED),
    loadScore = loadScore,
    physicalLoadMinutes48h = physicalLoadMinutes48h,
    academicLoadMinutesToday = academicLoadMinutesToday,
    activeCommitmentsCount = activeCommitmentsCount,
    overdueItemsCount = overdueItemsCount,
    scheduleConflictCount = scheduleConflictCount,
    recoveryState = runCatching { RecoveryState.valueOf(recoveryStateName) }.getOrDefault(RecoveryState.READY),
    adjustmentDirective = adjustmentDirective,
    preservesRestWithoutPunishment = preservesRestWithoutPunishment,
    provenance = runCatching { DataProvenanceTag.valueOf(provenanceName) }.getOrDefault(DataProvenanceTag.ESTIMATE),
    evaluatedAtEpochMs = evaluatedAtEpochMs
)

fun WorkloadAssessment.toEntity() = WorkloadAssessmentEntity(
    id = id,
    assessmentUuid = assessmentUuid,
    dateEpochDay = dateEpochDay,
    workloadLevelName = workloadLevel.name,
    loadScore = loadScore,
    physicalLoadMinutes48h = physicalLoadMinutes48h,
    academicLoadMinutesToday = academicLoadMinutesToday,
    activeCommitmentsCount = activeCommitmentsCount,
    overdueItemsCount = overdueItemsCount,
    scheduleConflictCount = scheduleConflictCount,
    recoveryStateName = recoveryState.name,
    adjustmentDirective = adjustmentDirective,
    preservesRestWithoutPunishment = preservesRestWithoutPunishment,
    provenanceName = provenance.name,
    evaluatedAtEpochMs = evaluatedAtEpochMs
)

fun ConsistencyPredictionEntity.toDomain() = ConsistencyPrediction(
    id = id,
    predictionUuid = predictionUuid,
    domainLabel = domainLabel,
    horizonDays = horizonDays,
    currentConsistencyPercent = currentConsistencyPercent,
    predictedConsistencyPercent = predictedConsistencyPercent,
    slopePerWeek = slopePerWeek,
    confidence = runCatching { ConfidenceLevel.valueOf(confidenceName) }.getOrDefault(ConfidenceLevel.LOW),
    insufficientData = insufficientData,
    limitationNote = limitationNote,
    atRiskGoals = atRiskGoalsJoined.split("||").filter { it.isNotBlank() },
    riskLevelLabel = riskLevelLabel,
    trendSeries = trendSeriesEncoded.split("||").mapNotNull { token ->
        val parts = token.split("::")
        if (parts.size >= 3) {
            TrendDataPoint(
                label = parts[0],
                valuePercent = parts[1].toIntOrNull() ?: 0,
                isEstimate = parts[2].toBooleanStrictOrNull() ?: false
            )
        } else null
    },
    provenance = runCatching { DataProvenanceTag.valueOf(provenanceName) }.getOrDefault(DataProvenanceTag.PREDICTION),
    evaluatedAtEpochMs = evaluatedAtEpochMs
)

fun ConsistencyPrediction.toEntity() = ConsistencyPredictionEntity(
    id = id,
    predictionUuid = predictionUuid,
    domainLabel = domainLabel,
    horizonDays = horizonDays,
    currentConsistencyPercent = currentConsistencyPercent,
    predictedConsistencyPercent = predictedConsistencyPercent,
    slopePerWeek = slopePerWeek,
    confidenceName = confidence.name,
    insufficientData = insufficientData,
    limitationNote = limitationNote,
    atRiskGoalsJoined = atRiskGoals.joinToString("||"),
    riskLevelLabel = riskLevelLabel,
    trendSeriesEncoded = trendSeries.joinToString("||") { "${it.label}::${it.valuePercent}::${it.isEstimate}" },
    provenanceName = provenance.name,
    evaluatedAtEpochMs = evaluatedAtEpochMs
)

fun PersonalizedRecommendationEntity.toDomain() = PersonalizedRecommendation(
    id = id,
    recUuid = recUuid,
    category = runCatching { RecommendationCategory.valueOf(categoryName) }.getOrDefault(RecommendationCategory.PERSONAL_DEVELOPMENT),
    title = title,
    description = description,
    rationale = rationale,
    expectedImpact = expectedImpact,
    actionLabel = actionLabel,
    deepLinkRoute = deepLinkRoute,
    priorityScore = priorityScore,
    status = runCatching { RecommendationStatus.valueOf(statusName) }.getOrDefault(RecommendationStatus.NEW),
    provenance = runCatching { DataProvenanceTag.valueOf(provenanceName) }.getOrDefault(DataProvenanceTag.ESTIMATE),
    confidence = runCatching { ConfidenceLevel.valueOf(confidenceName) }.getOrDefault(ConfidenceLevel.MODERATE),
    expiresAtEpochMs = expiresAtEpochMs,
    cooldownUntilEpochMs = cooldownUntilEpochMs,
    sourceFingerprint = sourceFingerprint,
    createdAtEpochMs = createdAtEpochMs,
    respondedAtEpochMs = respondedAtEpochMs
)

fun PersonalizedRecommendation.toEntity() = PersonalizedRecommendationEntity(
    id = id,
    recUuid = recUuid,
    categoryName = category.name,
    title = title,
    description = description,
    rationale = rationale,
    expectedImpact = expectedImpact,
    actionLabel = actionLabel,
    deepLinkRoute = deepLinkRoute,
    priorityScore = priorityScore,
    statusName = status.name,
    provenanceName = provenance.name,
    confidenceName = confidence.name,
    expiresAtEpochMs = expiresAtEpochMs,
    cooldownUntilEpochMs = cooldownUntilEpochMs,
    sourceFingerprint = sourceFingerprint,
    createdAtEpochMs = createdAtEpochMs,
    respondedAtEpochMs = respondedAtEpochMs
)

fun RecommendationFeedbackEntity.toDomain() = RecommendationFeedbackRecord(
    id = id,
    recUuid = recUuid,
    category = runCatching { RecommendationCategory.valueOf(categoryName) }.getOrDefault(RecommendationCategory.PERSONAL_DEVELOPMENT),
    action = runCatching { RecommendationStatus.valueOf(actionName) }.getOrDefault(RecommendationStatus.ACCEPTED),
    feedbackNote = feedbackNote,
    timestampEpochMs = timestampEpochMs
)

fun RecommendationFeedbackRecord.toEntity() = RecommendationFeedbackEntity(
    id = id,
    recUuid = recUuid,
    categoryName = category.name,
    actionName = action.name,
    feedbackNote = feedbackNote,
    timestampEpochMs = timestampEpochMs
)

fun RoadmapMilestoneEntity.toDomain() = RoadmapMilestone(
    id = id,
    milestoneUuid = milestoneUuid,
    roadmapUuid = roadmapUuid,
    title = title,
    domain = domain,
    targetDayOffset = targetDayOffset,
    targetDateEpochDay = targetDateEpochDay,
    isCompleted = isCompleted,
    isUserEdited = isUserEdited,
    orderIndex = orderIndex,
    notes = notes
)

fun RoadmapMilestone.toEntity() = RoadmapMilestoneEntity(
    id = id,
    milestoneUuid = milestoneUuid,
    roadmapUuid = roadmapUuid,
    title = title,
    domain = domain,
    targetDayOffset = targetDayOffset,
    targetDateEpochDay = targetDateEpochDay,
    isCompleted = isCompleted,
    isUserEdited = isUserEdited,
    orderIndex = orderIndex,
    notes = notes
)

fun DevelopmentRoadmapEntity.toDomain(milestones: List<RoadmapMilestone>) = DevelopmentRoadmap(
    id = id,
    roadmapUuid = roadmapUuid,
    title = title,
    horizon = runCatching { RoadmapHorizon.valueOf(horizonName) }.getOrDefault(RoadmapHorizon.THIRTY_DAYS),
    visionSummary = visionSummary,
    focusDomains = focusDomainsJoined.split("||").filter { it.isNotBlank() },
    dependenciesSummary = dependenciesSummary,
    riskNotes = riskNotes,
    nextReviewDateEpochDay = nextReviewDateEpochDay,
    isApproved = isApproved,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs,
    milestones = milestones
)

fun DevelopmentRoadmap.toEntity() = DevelopmentRoadmapEntity(
    id = id,
    roadmapUuid = roadmapUuid,
    title = title,
    horizonName = horizon.name,
    visionSummary = visionSummary,
    focusDomainsJoined = focusDomains.joinToString("||"),
    dependenciesSummary = dependenciesSummary,
    riskNotes = riskNotes,
    nextReviewDateEpochDay = nextReviewDateEpochDay,
    isApproved = isApproved,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs
)

fun IntelligenceReportEntity.toDomain() = IntelligenceReport(
    id = id,
    reportUuid = reportUuid,
    periodType = runCatching { ReportPeriodType.valueOf(periodTypeName) }.getOrDefault(ReportPeriodType.WEEKLY),
    periodLabel = periodLabel,
    periodStartEpochDay = periodStartEpochDay,
    periodEndEpochDay = periodEndEpochDay,
    activeDaysObserved = activeDaysObserved,
    isPartialDataWindow = isPartialDataWindow,
    measuredStatsSummary = measuredStatsSummary,
    selfReportedStatsSummary = selfReportedStatsSummary,
    workoutsCount = workoutsCount,
    joggingMinutes = joggingMinutes,
    studyMinutes = studyMinutes,
    revisionsCount = revisionsCount,
    habitsCompletedCount = habitsCompletedCount,
    tasksCompletedCount = tasksCompletedCount,
    dungeonsClearedCount = dungeonsClearedCount,
    xpGainedWindow = xpGainedWindow,
    deterministicHighlights = highlightsJoined.split("||").filter { it.isNotBlank() },
    deterministicRisks = risksJoined.split("||").filter { it.isNotBlank() },
    aiNarrativeSummary = aiNarrativeSummary,
    aiUsedFallback = aiUsedFallback,
    generatedAtEpochMs = generatedAtEpochMs
)

fun IntelligenceReport.toEntity() = IntelligenceReportEntity(
    id = id,
    reportUuid = reportUuid,
    periodTypeName = periodType.name,
    periodLabel = periodLabel,
    periodStartEpochDay = periodStartEpochDay,
    periodEndEpochDay = periodEndEpochDay,
    activeDaysObserved = activeDaysObserved,
    isPartialDataWindow = isPartialDataWindow,
    measuredStatsSummary = measuredStatsSummary,
    selfReportedStatsSummary = selfReportedStatsSummary,
    workoutsCount = workoutsCount,
    joggingMinutes = joggingMinutes,
    studyMinutes = studyMinutes,
    revisionsCount = revisionsCount,
    habitsCompletedCount = habitsCompletedCount,
    tasksCompletedCount = tasksCompletedCount,
    dungeonsClearedCount = dungeonsClearedCount,
    xpGainedWindow = xpGainedWindow,
    highlightsJoined = deterministicHighlights.joinToString("||"),
    risksJoined = deterministicRisks.joinToString("||"),
    aiNarrativeSummary = aiNarrativeSummary,
    aiUsedFallback = aiUsedFallback,
    generatedAtEpochMs = generatedAtEpochMs
)

fun IntelligencePreferencesEntity.toDomain() = PlayerIntelligencePreferences(
    planningStyle = runCatching { PlanningStyle.valueOf(planningStyleName) }.getOrDefault(PlanningStyle.BALANCED),
    preferredStudyHour = preferredStudyHour,
    preferredWorkoutHour = preferredWorkoutHour,
    maxDailyFocusMinutes = maxDailyFocusMinutes,
    maxDailyWorkoutMinutes = maxDailyWorkoutMinutes,
    maxRecommendationsPerDay = maxRecommendationsPerDay,
    notificationIntensity = runCatching { NotificationIntensity.valueOf(notificationIntensityName) }.getOrDefault(NotificationIntensity.MODERATE),
    enableProactivePlanning = enableProactivePlanning,
    enableWeeklyReports = enableWeeklyReports,
    enableMonthlyReports = enableMonthlyReports,
    enableAdaptiveDungeons = enableAdaptiveDungeons,
    shareIntelligenceWithCloudAi = shareIntelligenceWithCloudAi,
    sharePhysicalWithIntelligence = sharePhysicalWithIntelligence,
    shareAcademicWithIntelligence = shareAcademicWithIntelligence,
    shareLifeWithIntelligence = shareLifeWithIntelligence,
    shareRpgWithIntelligence = shareRpgWithIntelligence,
    restDays = restDaysCsv.parseDayOfWeekCsv(),
    updatedAtEpochMs = updatedAtEpochMs
)

fun PlayerIntelligencePreferences.toEntity() = IntelligencePreferencesEntity(
    id = 1,
    planningStyleName = planningStyle.name,
    preferredStudyHour = preferredStudyHour,
    preferredWorkoutHour = preferredWorkoutHour,
    maxDailyFocusMinutes = maxDailyFocusMinutes,
    maxDailyWorkoutMinutes = maxDailyWorkoutMinutes,
    maxRecommendationsPerDay = maxRecommendationsPerDay,
    notificationIntensityName = notificationIntensity.name,
    enableProactivePlanning = enableProactivePlanning,
    enableWeeklyReports = enableWeeklyReports,
    enableMonthlyReports = enableMonthlyReports,
    enableAdaptiveDungeons = enableAdaptiveDungeons,
    shareIntelligenceWithCloudAi = shareIntelligenceWithCloudAi,
    sharePhysicalWithIntelligence = sharePhysicalWithIntelligence,
    shareAcademicWithIntelligence = shareAcademicWithIntelligence,
    shareLifeWithIntelligence = shareLifeWithIntelligence,
    shareRpgWithIntelligence = shareRpgWithIntelligence,
    restDaysCsv = restDays.toCsvString(),
    updatedAtEpochMs = updatedAtEpochMs
)

fun AdvisorConsultationEntity.toDomain() = AdvisorConsultationResult(
    id = id,
    consultationUuid = consultationUuid,
    question = question,
    deterministicSummary = deterministicSummary,
    aiResponse = aiResponse,
    provenanceTags = provenanceTagsJoined.split(",").mapNotNull { runCatching { DataProvenanceTag.valueOf(it.trim()) }.getOrNull() },
    confidence = runCatching { ConfidenceLevel.valueOf(confidenceName) }.getOrDefault(ConfidenceLevel.LOW),
    connectionStatus = runCatching { AiConnectionStatus.valueOf(connectionStatusName) }.getOrDefault(AiConnectionStatus.LOCAL_FALLBACK),
    usedFallback = usedFallback,
    limitationNotes = limitationNotesJoined.split("||").filter { it.isNotBlank() },
    timestampEpochMs = timestampEpochMs
)

fun AdvisorConsultationResult.toEntity() = AdvisorConsultationEntity(
    id = id,
    consultationUuid = consultationUuid,
    question = question,
    deterministicSummary = deterministicSummary,
    aiResponse = aiResponse,
    provenanceTagsJoined = provenanceTags.joinToString(",") { it.name },
    confidenceName = confidence.name,
    connectionStatusName = connectionStatus.name,
    usedFallback = usedFallback,
    limitationNotesJoined = limitationNotes.joinToString("||"),
    timestampEpochMs = timestampEpochMs
)
