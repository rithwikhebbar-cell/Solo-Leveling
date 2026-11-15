package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.AiChatMessage
import com.example.domain.model.AiChatRole
import com.example.domain.model.AiConsentState
import com.example.domain.model.AiConversation
import com.example.domain.model.AiDailyPlanItem
import com.example.domain.model.AiModelPreferenceState
import com.example.domain.model.AiQuestProposal
import com.example.domain.model.AiQuestProposalCategory
import com.example.domain.model.AiRecommendation
import com.example.domain.model.AiRequestAuditRecord
import com.example.domain.model.PlanMode
import com.example.domain.model.ProposalStatus
import com.example.domain.model.SchedulePriority
import com.example.domain.model.SupportedAiModels
import com.example.domain.model.SystemNotificationPreferences

@Entity(
    tableName = "ai_conversations",
    indices = [Index(value = ["updatedAtEpochMs"])]
)
data class AiConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val associatedModule: String,
    val modelUsed: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)

@Entity(
    tableName = "ai_messages",
    foreignKeys = [
        ForeignKey(
            entity = AiConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["conversationId"]),
        Index(value = ["timestampEpochMs"])
    ]
)
data class AiMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: Long,
    val roleName: String,
    val content: String,
    val associatedModule: String,
    val modelUsed: String,
    val isFallback: Boolean,
    val timestampEpochMs: Long
)

@Entity(
    tableName = "ai_recommendations",
    indices = [Index(value = ["createdAtEpochMs"])]
)
data class AiRecommendationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val module: String,
    val headline: String,
    val detail: String,
    val rationale: String,
    val confidenceNote: String,
    val isDismissed: Boolean,
    val createdAtEpochMs: Long
)

@Entity(
    tableName = "ai_quest_proposals",
    indices = [
        Index(value = ["proposalUuid"], unique = true),
        Index(value = ["statusName"]),
        Index(value = ["createdAtEpochMs"])
    ]
)
data class AiQuestProposalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val proposalUuid: String,
    val title: String,
    val description: String,
    val categoryName: String,
    val difficultyLabel: String,
    val objective: String,
    val completionCriteria: String,
    val suggestedDurationMinutes: Int,
    val deadlineEpochMs: Long?,
    val priorityName: String,
    val reason: String,
    val targetSubjectId: Long?,
    val statusName: String,
    val createdAtEpochMs: Long
)

@Entity(
    tableName = "ai_daily_plan_proposals",
    indices = [
        Index(value = ["batchId"]),
        Index(value = ["statusName"]),
        Index(value = ["startMinuteOfDay"])
    ]
)
data class AiDailyPlanProposalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val batchId: String,
    val planModeName: String,
    val taskTitle: String,
    val module: String,
    val startMinuteOfDay: Int,
    val durationMinutes: Int,
    val priorityName: String,
    val reason: String,
    val dependencies: String,
    val optionalDeadlineEpochMs: Long?,
    val statusName: String,
    val createdAtEpochMs: Long
)

@Entity(tableName = "ai_model_preferences")
data class AiModelPreferenceEntity(
    @PrimaryKey val id: Int = 1,
    val selectedModelId: String = SupportedAiModels.DEFAULT_MODEL_ID,
    val backendBaseUrl: String = "",
    val aiEnabled: Boolean = true,
    val streamingEnabled: Boolean = true,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "notification_preferences")
data class NotificationPreferenceEntity(
    @PrimaryKey val id: Int = 1,
    val notificationsEnabled: Boolean = true,
    val enableDailyQuestReminders: Boolean = true,
    val enableOverdueRevisions: Boolean = true,
    val enableUpcomingExams: Boolean = true,
    val enableWorkoutReminders: Boolean = true,
    val enableRecoveryWarnings: Boolean = true,
    val enableAiRecommendations: Boolean = true,
    val enableDailySummary: Boolean = true,
    val enableHabitReminders: Boolean = true,
    val enableRoutineReminders: Boolean = true,
    val enableTaskDeadlines: Boolean = true,
    val enableOverdueTasks: Boolean = true,
    val enableGoalMilestones: Boolean = true,
    val enableDailyPlanningReminders: Boolean = true,
    val enableWeeklyReviewReminders: Boolean = true,
    val enableRpgAlerts: Boolean = true,
    val quietHoursStartHour: Int = 22,
    val quietHoursEndHour: Int = 7,
    val maxNotificationsPerDay: Int = 6,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "ai_consent_records")
data class AiConsentRecordEntity(
    @PrimaryKey val id: Int = 1,
    val cloudAiConsentGranted: Boolean = false,
    val sharePhysicalWithAi: Boolean = true,
    val shareAcademicWithAi: Boolean = true,
    val shareQuestsWithAi: Boolean = true,
    val shareLifeWithAi: Boolean = true,
    val shareRpgWithAi: Boolean = true,
    val consentVersion: Int = 1,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "ai_request_audit",
    indices = [Index(value = ["timestampEpochMs"])]
)
data class AiRequestAuditEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val requestType: String,
    val modelId: String,
    val usedCloudBackend: Boolean,
    val success: Boolean,
    val promptTokens: Int,
    val completionTokens: Int,
    val latencyMs: Long,
    val errorCode: String?,
    val timestampEpochMs: Long
)

// Domain Mappers

fun AiConversationEntity.toDomain() = AiConversation(
    id = id,
    title = title,
    associatedModule = associatedModule,
    modelUsed = modelUsed,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs
)

fun AiConversation.toEntity() = AiConversationEntity(
    id = id,
    title = title,
    associatedModule = associatedModule,
    modelUsed = modelUsed,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs
)

fun AiMessageEntity.toDomain() = AiChatMessage(
    id = id,
    conversationId = conversationId,
    role = runCatching { AiChatRole.valueOf(roleName) }.getOrDefault(AiChatRole.ASSISTANT),
    content = content,
    associatedModule = associatedModule,
    modelUsed = modelUsed,
    isFallback = isFallback,
    timestampEpochMs = timestampEpochMs
)

fun AiChatMessage.toEntity() = AiMessageEntity(
    id = id,
    conversationId = conversationId,
    roleName = role.name,
    content = content,
    associatedModule = associatedModule,
    modelUsed = modelUsed,
    isFallback = isFallback,
    timestampEpochMs = timestampEpochMs
)

fun AiRecommendationEntity.toDomain() = AiRecommendation(
    id = id,
    module = module,
    headline = headline,
    detail = detail,
    rationale = rationale,
    confidenceNote = confidenceNote,
    isDismissed = isDismissed,
    createdAtEpochMs = createdAtEpochMs
)

fun AiRecommendation.toEntity() = AiRecommendationEntity(
    id = id,
    module = module,
    headline = headline,
    detail = detail,
    rationale = rationale,
    confidenceNote = confidenceNote,
    isDismissed = isDismissed,
    createdAtEpochMs = createdAtEpochMs
)

fun AiQuestProposalEntity.toDomain() = AiQuestProposal(
    id = id,
    proposalUuid = proposalUuid,
    title = title,
    description = description,
    category = runCatching { AiQuestProposalCategory.valueOf(categoryName) }
        .getOrDefault(AiQuestProposalCategory.DISCIPLINE),
    difficultyLabel = difficultyLabel,
    objective = objective,
    completionCriteria = completionCriteria,
    suggestedDurationMinutes = suggestedDurationMinutes,
    deadlineEpochMs = deadlineEpochMs,
    priority = runCatching { SchedulePriority.valueOf(priorityName) }.getOrDefault(SchedulePriority.MEDIUM),
    reason = reason,
    targetSubjectId = targetSubjectId,
    status = runCatching { ProposalStatus.valueOf(statusName) }.getOrDefault(ProposalStatus.PENDING),
    createdAtEpochMs = createdAtEpochMs
)

fun AiQuestProposal.toEntity() = AiQuestProposalEntity(
    id = id,
    proposalUuid = proposalUuid,
    title = title,
    description = description,
    categoryName = category.name,
    difficultyLabel = difficultyLabel,
    objective = objective,
    completionCriteria = completionCriteria,
    suggestedDurationMinutes = suggestedDurationMinutes,
    deadlineEpochMs = deadlineEpochMs,
    priorityName = priority.name,
    reason = reason,
    targetSubjectId = targetSubjectId,
    statusName = status.name,
    createdAtEpochMs = createdAtEpochMs
)

fun AiDailyPlanProposalEntity.toDomain() = AiDailyPlanItem(
    id = id,
    batchId = batchId,
    planMode = runCatching { PlanMode.valueOf(planModeName) }.getOrDefault(PlanMode.DAILY),
    taskTitle = taskTitle,
    module = module,
    startMinuteOfDay = startMinuteOfDay,
    durationMinutes = durationMinutes,
    priority = runCatching { SchedulePriority.valueOf(priorityName) }.getOrDefault(SchedulePriority.MEDIUM),
    reason = reason,
    dependencies = dependencies,
    optionalDeadlineEpochMs = optionalDeadlineEpochMs,
    status = runCatching { ProposalStatus.valueOf(statusName) }.getOrDefault(ProposalStatus.PENDING),
    createdAtEpochMs = createdAtEpochMs
)

fun AiDailyPlanItem.toEntity() = AiDailyPlanProposalEntity(
    id = id,
    batchId = batchId,
    planModeName = planMode.name,
    taskTitle = taskTitle,
    module = module,
    startMinuteOfDay = startMinuteOfDay,
    durationMinutes = durationMinutes,
    priorityName = priority.name,
    reason = reason,
    dependencies = dependencies,
    optionalDeadlineEpochMs = optionalDeadlineEpochMs,
    statusName = status.name,
    createdAtEpochMs = createdAtEpochMs
)

fun AiModelPreferenceEntity.toDomain() = AiModelPreferenceState(
    selectedModelId = selectedModelId,
    backendBaseUrl = backendBaseUrl,
    aiEnabled = aiEnabled,
    streamingEnabled = streamingEnabled,
    updatedAtEpochMs = updatedAtEpochMs
)

fun AiModelPreferenceState.toEntity() = AiModelPreferenceEntity(
    id = 1,
    selectedModelId = selectedModelId,
    backendBaseUrl = backendBaseUrl,
    aiEnabled = aiEnabled,
    streamingEnabled = streamingEnabled,
    updatedAtEpochMs = updatedAtEpochMs
)

fun AiConsentRecordEntity.toDomain() = AiConsentState(
    cloudAiConsentGranted = cloudAiConsentGranted,
    sharePhysicalWithAi = sharePhysicalWithAi,
    shareAcademicWithAi = shareAcademicWithAi,
    shareQuestsWithAi = shareQuestsWithAi,
    shareLifeWithAi = shareLifeWithAi,
    shareRpgWithAi = shareRpgWithAi,
    consentVersion = consentVersion,
    updatedAtEpochMs = updatedAtEpochMs
)

fun AiConsentState.toEntity() = AiConsentRecordEntity(
    id = 1,
    cloudAiConsentGranted = cloudAiConsentGranted,
    sharePhysicalWithAi = sharePhysicalWithAi,
    shareAcademicWithAi = shareAcademicWithAi,
    shareQuestsWithAi = shareQuestsWithAi,
    shareLifeWithAi = shareLifeWithAi,
    shareRpgWithAi = shareRpgWithAi,
    consentVersion = consentVersion,
    updatedAtEpochMs = updatedAtEpochMs
)

fun NotificationPreferenceEntity.toDomain() = SystemNotificationPreferences(
    notificationsEnabled = notificationsEnabled,
    enableDailyQuestReminders = enableDailyQuestReminders,
    enableOverdueRevisions = enableOverdueRevisions,
    enableUpcomingExams = enableUpcomingExams,
    enableWorkoutReminders = enableWorkoutReminders,
    enableRecoveryWarnings = enableRecoveryWarnings,
    enableAiRecommendations = enableAiRecommendations,
    enableDailySummary = enableDailySummary,
    enableHabitReminders = enableHabitReminders,
    enableRoutineReminders = enableRoutineReminders,
    enableTaskDeadlines = enableTaskDeadlines,
    enableOverdueTasks = enableOverdueTasks,
    enableGoalMilestones = enableGoalMilestones,
    enableDailyPlanningReminders = enableDailyPlanningReminders,
    enableWeeklyReviewReminders = enableWeeklyReviewReminders,
    enableRpgAlerts = enableRpgAlerts,
    quietHoursStartHour = quietHoursStartHour,
    quietHoursEndHour = quietHoursEndHour,
    maxNotificationsPerDay = maxNotificationsPerDay,
    updatedAtEpochMs = updatedAtEpochMs
)

fun SystemNotificationPreferences.toEntity() = NotificationPreferenceEntity(
    id = 1,
    notificationsEnabled = notificationsEnabled,
    enableDailyQuestReminders = enableDailyQuestReminders,
    enableOverdueRevisions = enableOverdueRevisions,
    enableUpcomingExams = enableUpcomingExams,
    enableWorkoutReminders = enableWorkoutReminders,
    enableRecoveryWarnings = enableRecoveryWarnings,
    enableAiRecommendations = enableAiRecommendations,
    enableDailySummary = enableDailySummary,
    enableHabitReminders = enableHabitReminders,
    enableRoutineReminders = enableRoutineReminders,
    enableTaskDeadlines = enableTaskDeadlines,
    enableOverdueTasks = enableOverdueTasks,
    enableGoalMilestones = enableGoalMilestones,
    enableDailyPlanningReminders = enableDailyPlanningReminders,
    enableWeeklyReviewReminders = enableWeeklyReviewReminders,
    enableRpgAlerts = enableRpgAlerts,
    quietHoursStartHour = quietHoursStartHour,
    quietHoursEndHour = quietHoursEndHour,
    maxNotificationsPerDay = maxNotificationsPerDay,
    updatedAtEpochMs = updatedAtEpochMs
)

fun AiRequestAuditEntity.toDomain() = AiRequestAuditRecord(
    id = id,
    requestType = requestType,
    modelId = modelId,
    usedCloudBackend = usedCloudBackend,
    success = success,
    promptTokens = promptTokens,
    completionTokens = completionTokens,
    latencyMs = latencyMs,
    errorCode = errorCode,
    timestampEpochMs = timestampEpochMs
)

fun AiRequestAuditRecord.toEntity() = AiRequestAuditEntity(
    id = id,
    requestType = requestType,
    modelId = modelId,
    usedCloudBackend = usedCloudBackend,
    success = success,
    promptTokens = promptTokens,
    completionTokens = completionTokens,
    latencyMs = latencyMs,
    errorCode = errorCode,
    timestampEpochMs = timestampEpochMs
)
