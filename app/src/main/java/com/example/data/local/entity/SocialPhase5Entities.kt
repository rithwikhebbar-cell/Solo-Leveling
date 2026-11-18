package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.AdaptiveDifficulty
import com.example.domain.model.MasteryDevelopmentStage
import com.example.domain.model.MasteryEvidenceClassification
import com.example.domain.model.PersonalizedSocialRecommendation
import com.example.domain.model.PersonalizedWeeklyTrainingPlan
import com.example.domain.model.ScheduledTrainingActivity
import com.example.domain.model.SocialCompetencyProgress
import com.example.domain.model.SocialModuleIdentifier
import com.example.domain.model.SocialRecommendationCategory
import com.example.domain.model.SocialRecommendationStatus
import com.example.domain.model.TrainingActivityType

// =========================================================================
// 1. ADAPTIVE SOCIAL RECOMMENDATION ENTITY
// =========================================================================

@Entity(
    tableName = "social_adaptive_recommendations",
    foreignKeys = [
        ForeignKey(
            entity = SocialProfileEntity::class,
            parentColumns = ["accountOwnerId"],
            childColumns = ["accountOwnerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["accountOwnerId"]),
        Index(value = ["targetModule"]),
        Index(value = ["category"]),
        Index(value = ["status"]),
        Index(value = ["createdAtEpochMs"])
    ]
)
data class SocialAdaptiveRecommendationEntity(
    @PrimaryKey val recommendationId: String,
    val accountOwnerId: String,
    val title: String,
    val targetModule: String,
    val targetCompetency: String,
    val category: String,
    val explanation: String,
    val supportingEvidenceJson: String,
    val recommendedActivityId: String,
    val recommendedActivityType: String,
    val estimatedDurationMinutes: Int,
    val difficulty: String,
    val expectedLearningOutcome: String,
    val status: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)

fun SocialAdaptiveRecommendationEntity.toDomain(): PersonalizedSocialRecommendation {
    val evidenceList = runCatching {
        if (supportingEvidenceJson.isBlank() || supportingEvidenceJson == "[]") emptyList<String>()
        else supportingEvidenceJson.removeSurrounding("[", "]").split(",").map { it.trim().removeSurrounding("\"") }
    }.getOrDefault(emptyList())

    return PersonalizedSocialRecommendation(
        recommendationId = recommendationId,
        accountOwnerId = accountOwnerId,
        title = title,
        targetModule = SocialModuleIdentifier.fromCode(targetModule),
        targetCompetency = targetCompetency,
        category = SocialRecommendationCategory.fromString(category),
        explanation = explanation,
        supportingEvidenceReferences = evidenceList,
        recommendedActivityId = recommendedActivityId,
        recommendedActivityType = recommendedActivityType,
        estimatedDurationMinutes = estimatedDurationMinutes,
        difficulty = AdaptiveDifficulty.fromString(difficulty),
        expectedLearningOutcome = expectedLearningOutcome,
        status = SocialRecommendationStatus.fromString(status),
        createdAtEpochMs = createdAtEpochMs,
        updatedAtEpochMs = updatedAtEpochMs
    )
}

fun PersonalizedSocialRecommendation.toEntity(): SocialAdaptiveRecommendationEntity {
    val evidenceJson = "[" + supportingEvidenceReferences.joinToString(",") { "\"${it.replace("\"", "\\\"")}\"" } + "]"
    return SocialAdaptiveRecommendationEntity(
        recommendationId = recommendationId,
        accountOwnerId = accountOwnerId,
        title = title,
        targetModule = targetModule.code,
        targetCompetency = targetCompetency,
        category = category.name,
        explanation = explanation,
        supportingEvidenceJson = evidenceJson,
        recommendedActivityId = recommendedActivityId,
        recommendedActivityType = recommendedActivityType,
        estimatedDurationMinutes = estimatedDurationMinutes,
        difficulty = difficulty.name,
        expectedLearningOutcome = expectedLearningOutcome,
        status = status.name,
        createdAtEpochMs = createdAtEpochMs,
        updatedAtEpochMs = updatedAtEpochMs
    )
}

// =========================================================================
// 2. WEEKLY TRAINING PLAN ENTITY
// =========================================================================

@Entity(
    tableName = "social_weekly_training_plans",
    foreignKeys = [
        ForeignKey(
            entity = SocialProfileEntity::class,
            parentColumns = ["accountOwnerId"],
            childColumns = ["accountOwnerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["accountOwnerId"]),
        Index(value = ["startDateEpochMs"]),
        Index(value = ["isPaused"])
    ]
)
data class SocialWeeklyTrainingPlanEntity(
    @PrimaryKey val planId: String,
    val accountOwnerId: String,
    val weeklyDevelopmentObjective: String,
    val targetMinutesPerDay: Int,
    val targetDaysPerWeek: Int,
    val startDateEpochMs: Long,
    val endDateEpochMs: Long,
    val isPaused: Boolean,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)

// =========================================================================
// 3. SCHEDULED TRAINING ACTIVITY ENTITY
// =========================================================================

@Entity(
    tableName = "social_planned_activities",
    foreignKeys = [
        ForeignKey(
            entity = SocialWeeklyTrainingPlanEntity::class,
            parentColumns = ["planId"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["planId"]),
        Index(value = ["accountOwnerId"]),
        Index(value = ["scheduledDateEpochMs"]),
        Index(value = ["isCompleted"])
    ]
)
data class SocialPlannedActivityEntity(
    @PrimaryKey val activityId: String,
    val planId: String,
    val accountOwnerId: String,
    val dayOfWeek: Int,
    val scheduledDateEpochMs: Long,
    val moduleName: String,
    val competencyName: String,
    val activityType: String,
    val title: String,
    val referenceId: String,
    val durationMinutes: Int,
    val difficulty: String,
    val isCompleted: Boolean,
    val isSkipped: Boolean,
    val notes: String? = null,
    val completedEpochMs: Long? = null
)

fun SocialPlannedActivityEntity.toDomain() = ScheduledTrainingActivity(
    activityId = activityId,
    planId = planId,
    accountOwnerId = accountOwnerId,
    dayOfWeek = dayOfWeek,
    scheduledDateEpochMs = scheduledDateEpochMs,
    module = SocialModuleIdentifier.fromCode(moduleName),
    competencyName = competencyName,
    activityType = TrainingActivityType.fromString(activityType),
    title = title,
    referenceId = referenceId,
    durationMinutes = durationMinutes,
    difficulty = AdaptiveDifficulty.fromString(difficulty),
    isCompleted = isCompleted,
    isSkipped = isSkipped,
    notes = notes,
    completedEpochMs = completedEpochMs
)

fun ScheduledTrainingActivity.toEntity() = SocialPlannedActivityEntity(
    activityId = activityId,
    planId = planId,
    accountOwnerId = accountOwnerId,
    dayOfWeek = dayOfWeek,
    scheduledDateEpochMs = scheduledDateEpochMs,
    moduleName = module.code,
    competencyName = competencyName,
    activityType = activityType.name,
    title = title,
    referenceId = referenceId,
    durationMinutes = durationMinutes,
    difficulty = difficulty.name,
    isCompleted = isCompleted,
    isSkipped = isSkipped,
    notes = notes,
    completedEpochMs = completedEpochMs
)

fun SocialWeeklyTrainingPlanEntity.toDomain(activities: List<ScheduledTrainingActivity>) = PersonalizedWeeklyTrainingPlan(
    planId = planId,
    accountOwnerId = accountOwnerId,
    weeklyDevelopmentObjective = weeklyDevelopmentObjective,
    targetMinutesPerDay = targetMinutesPerDay,
    targetDaysPerWeek = targetDaysPerWeek,
    startDateEpochMs = startDateEpochMs,
    endDateEpochMs = endDateEpochMs,
    isPaused = isPaused,
    scheduledActivities = activities,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs
)

fun PersonalizedWeeklyTrainingPlan.toEntity() = SocialWeeklyTrainingPlanEntity(
    planId = planId,
    accountOwnerId = accountOwnerId,
    weeklyDevelopmentObjective = weeklyDevelopmentObjective,
    targetMinutesPerDay = targetMinutesPerDay,
    targetDaysPerWeek = targetDaysPerWeek,
    startDateEpochMs = startDateEpochMs,
    endDateEpochMs = endDateEpochMs,
    isPaused = isPaused,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs
)

// =========================================================================
// 4. COMPETENCY PROGRESS ENTITY
// =========================================================================

@Entity(
    tableName = "social_competency_progress",
    foreignKeys = [
        ForeignKey(
            entity = SocialProfileEntity::class,
            parentColumns = ["accountOwnerId"],
            childColumns = ["accountOwnerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["accountOwnerId"]),
        Index(value = ["moduleName"]),
        Index(value = ["competencyName"]),
        Index(value = ["masteryStage"])
    ]
)
data class SocialCompetencyProgressEntity(
    @PrimaryKey val competencyId: String,
    val accountOwnerId: String,
    val moduleName: String,
    val competencyName: String,
    val masteryStage: String,
    val recommendedDifficulty: String,
    val practiceCount: Int,
    val lastPracticedEpochMs: Long?,
    val lastEvaluatedScore: Int?,
    val evidenceClassification: String,
    val updatedAtEpochMs: Long,
    val maintenanceRequired: Boolean = false,
    val maintenanceReason: String? = null
)

fun SocialCompetencyProgressEntity.toDomain(): SocialCompetencyProgress {
    val isLegacyMaint = MasteryDevelopmentStage.isLegacyMaintenance(masteryStage)
    val safeStage = MasteryDevelopmentStage.fromString(masteryStage)
    val preservedMaintenance = maintenanceRequired || isLegacyMaint
    val preservedReason = maintenanceReason ?: if (isLegacyMaint) {
        "Legacy MAINTENANCE status safely downgraded; historical mastery will be recovered from verified activity logs."
    } else {
        null
    }
    return SocialCompetencyProgress(
        competencyId = competencyId,
        accountOwnerId = accountOwnerId,
        module = SocialModuleIdentifier.fromCode(moduleName),
        competencyName = competencyName,
        masteryStage = safeStage,
        recommendedDifficulty = AdaptiveDifficulty.fromString(recommendedDifficulty),
        practiceCount = practiceCount,
        lastPracticedEpochMs = lastPracticedEpochMs,
        lastEvaluatedScore = lastEvaluatedScore,
        evidenceClassification = MasteryEvidenceClassification.fromString(evidenceClassification),
        updatedAtEpochMs = updatedAtEpochMs,
        maintenanceRequired = preservedMaintenance,
        maintenanceReason = preservedReason
    )
}

fun SocialCompetencyProgress.toEntity() = SocialCompetencyProgressEntity(
    competencyId = competencyId,
    accountOwnerId = accountOwnerId,
    moduleName = module.code,
    competencyName = competencyName,
    masteryStage = masteryStage.name,
    recommendedDifficulty = recommendedDifficulty.name,
    practiceCount = practiceCount,
    lastPracticedEpochMs = lastPracticedEpochMs,
    lastEvaluatedScore = lastEvaluatedScore,
    evidenceClassification = evidenceClassification.name,
    updatedAtEpochMs = updatedAtEpochMs,
    maintenanceRequired = maintenanceRequired,
    maintenanceReason = maintenanceReason
)
