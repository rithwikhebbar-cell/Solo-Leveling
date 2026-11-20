package com.example.domain.model

enum class AiContextScope(val label: String) {
    FULL("Full Reality OS Context"),
    PHYSICAL("Physical & Recovery"),
    ACADEMIC("Academic & Exams"),
    QUESTS("Quests & Consistency"),
    PLANNING("Daily / Weekly Planning")
}

enum class AiConnectionStatus(val label: String) {
    LOCAL_FALLBACK("Offline Deterministic Fallback"),
    BACKEND_UNCONFIGURED("Backend URL Not Set (Fallback Active)"),
    CONNECTING("Contacting AI Backend..."),
    STREAMING("Streaming AI Response"),
    CONNECTED("Backend Connected"),
    AUTH_ERROR("Backend Auth Rejected (Fallback Used)"),
    RATE_LIMITED("Rate Limited (Fallback Used)"),
    PROVIDER_ERROR("Provider Error / Timeout (Fallback Used)")
}

enum class AiChatRole(val wireValue: String) {
    USER("user"),
    ASSISTANT("assistant"),
    SYSTEM("system")
}

enum class ProposalStatus(val label: String) {
    PENDING("Pending Approval"),
    APPROVED("Approved"),
    REJECTED("Rejected")
}

enum class AiQuestProposalCategory(val label: String) {
    PHYSICAL("Physical Development"),
    ACADEMIC("Academic Development"),
    DISCIPLINE("Discipline"),
    PERSONAL("Personal Development"),
    RECOVERY("Recovery"),
    LONG_TERM("Long-Term Challenge")
}

enum class PlanMode(val label: String) {
    DAILY("Daily Plan"),
    WEEKLY("Weekly Plan"),
    EXAM_PREP("Exam Prep Plan"),
    WORKOUT_SCHEDULE("Workout Schedule"),
    RECOVERY_PLAN("Recovery Plan"),
    REVISION_SCHEDULE("Revision Schedule")
}

enum class SchedulePriority(val label: String, val weight: Int) {
    CRITICAL("Critical", 4),
    HIGH("High", 3),
    MEDIUM("Medium", 2),
    LOW("Low", 1)
}

data class AiModelOption(
    val id: String,
    val displayName: String,
    val provider: String,
    val supportsStreaming: Boolean,
    val supportsStructuredJson: Boolean,
    val contextWindowTokens: Int,
    val description: String
)

object SupportedAiModels {
    const val DEFAULT_MODEL_ID = "gemini-3.8-flash"
    const val FAST_INTENT_MODEL_ID = "gemini-3.5-flash-lite"
    const val DEEP_REASONING_MODEL_ID = "gemini-4-argon"

    val catalog: List<AiModelOption> = listOf(
        AiModelOption(
            id = DEFAULT_MODEL_ID,
            displayName = "Gemini 3.8 Flash (Recommended)",
            provider = "Google Gemini • Firebase AI Logic",
            supportsStreaming = true,
            supportsStructuredJson = true,
            contextWindowTokens = 1_000_000,
            description = "Fast, intelligent conversational reasoning with streaming & structured JSON schema."
        ),
        AiModelOption(
            id = FAST_INTENT_MODEL_ID,
            displayName = "Gemini 3.5 Flash-Lite",
            provider = "Google Gemini • Firebase AI Logic",
            supportsStreaming = true,
            supportsStructuredJson = true,
            contextWindowTokens = 1_000_000,
            description = "Ultra-low latency model optimized for rapid voice command interpretation and quick briefs."
        ),
        AiModelOption(
            id = DEEP_REASONING_MODEL_ID,
            displayName = "Gemini 4 Argon",
            provider = "Google Gemini • Firebase AI Logic",
            supportsStreaming = true,
            supportsStructuredJson = true,
            contextWindowTokens = 1_000_000,
            description = "Deep multi-step analytical reasoning for comprehensive progression audits and strategic planning."
        )
    )

    fun resolveGeminiModelId(modelId: String): String {
        val clean = modelId.trim()
        return when {
            clean == DEFAULT_MODEL_ID || clean == FAST_INTENT_MODEL_ID || clean == DEEP_REASONING_MODEL_ID -> clean
            clean.contains("argon", ignoreCase = true) -> DEEP_REASONING_MODEL_ID
            clean.contains("lite", ignoreCase = true) -> FAST_INTENT_MODEL_ID
            else -> DEFAULT_MODEL_ID
        }
    }

    fun findById(modelId: String): AiModelOption =
        catalog.find { it.id == modelId }
            ?: catalog.find { it.id == resolveGeminiModelId(modelId) }
            ?: AiModelOption(
                id = modelId,
                displayName = modelId.substringAfterLast('/'),
                provider = "Custom Proxy Model",
                supportsStreaming = true,
                supportsStructuredJson = true,
                contextWindowTokens = 128_000,
                description = "Custom backend proxy model ID."
            )
}

data class ShadowArmyContextSummary(
    val monarchLevel: Int = 1,
    val monarchTitle: String = "Shadow Initiate",
    val activeShadowsCount: Int = 0,
    val armyCapacity: Int = 3,
    val activeDeploymentsCount: Int = 0,
    val activeFormationName: String? = null,
    val totalArmyPower: Int = 0,
    val shadowEssenceBalance: Int = 0,
    val notableShadowNames: List<String> = emptyList()
)

data class PlayerContextSnapshot(
    val generatedAtEpochMs: Long = System.currentTimeMillis(),
    val scope: AiContextScope = AiContextScope.FULL,
    val profileSummary: ProfileContextSummary,
    val physicalSummary: PhysicalContextSummary? = null,
    val academicSummary: AcademicContextSummary? = null,
    val questSummary: QuestContextSummary? = null,
    val lifeSummary: LifeContextSummary? = null,
    val rpgSummary: RpgContextSummary? = null,
    val shadowArmySummary: ShadowArmyContextSummary? = null,
    val preferencesSummary: PreferencesContextSummary? = null,
    val dataCompletenessNotes: List<String> = emptyList()
)

data class LifeContextSummary(
    val disciplineScore: Int = 0,
    val activeRoutinesCount: Int = 0,
    val completedRoutinesToday: Int = 0,
    val activeHabitsCount: Int = 0,
    val completedHabitsToday: Int = 0,
    val bestHabitStreakDays: Int = 0,
    val pendingTasksCount: Int = 0,
    val overdueTasksCount: Int = 0,
    val activeGoalsCount: Int = 0,
    val behindScheduleGoalsCount: Int = 0,
    val scheduledBlocksToday: Int = 0,
    val scheduleConflictsToday: Int = 0,
    val strongestArea: String = "",
    val weakestArea: String = ""
)

data class ProfileContextSummary(
    val codename: String,
    val level: Int,
    val rankCode: String,
    val rankName: String,
    val currentLevelXp: Int,
    val xpToNextLevel: Int,
    val totalXp: Long,
    val verifiedActivitiesCount: Int,
    val selfReportedActivitiesCount: Int,
    val streakDays: Int,
    val attributeValues: Map<String, Int>,
    val attributeProvenanceNote: String,
    val nextRankName: String?,
    val nextRankMissingLevel: Int,
    val nextRankMissingAttributes: Int,
    val nextRankMissingVerified: Int
)

data class PhysicalContextSummary(
    val joggingTargetMinutes: Int,
    val completedJoggingTodayMinutes: Int,
    val weeklyJoggingMinutes: Int,
    val weeklyWorkoutCount: Int,
    val workoutStatus: String,
    val recoveryState: RecoveryState,
    val readinessScore: Int,
    val measuredLoad48hMinutes: Int,
    val measuredLoad7dMinutes: Int,
    val sleepHoursMeasured: Double?,
    val restingHrMeasuredBpm: Int?,
    val selfReportedDiscomfort: Boolean,
    val hasHealthConnectData: Boolean
)

data class AcademicContextSummary(
    val activeSubjectsCount: Int,
    val subjectNamesAndCompletion: List<String>,
    val todayStudyMinutes: Int,
    val dailyGoalMinutes: Int,
    val weeklyStudyMinutes: Int,
    val overdueRevisionsCount: Int,
    val dueTodayRevisionsCount: Int,
    val weakestEstimatedTopics: List<String>,
    val upcomingExams: List<String>,
    val recentAssessmentSummaries: List<String>
)

data class QuestContextSummary(
    val activeGeneralQuestsCount: Int,
    val completedGeneralQuestsCount: Int,
    val activeAcademicQuestsCount: Int,
    val completedAcademicQuestsCount: Int,
    val activeQuestTitles: List<String>,
    val overdueOrNearDeadlineTitles: List<String>
)

data class PreferencesContextSummary(
    val dailyResetHour: Int,
    val dailyAcademicGoalMinutes: Int,
    val weeklyAcademicGoalMinutes: Int,
    val selectedAiModelId: String,
    val cloudAiConsentGranted: Boolean
)

data class DevelopmentAnalysisReport(
    val headline: String,
    val overallSummary: String,
    val strengths: List<String>,
    val weaknesses: List<String>,
    val bottlenecks: List<String>,
    val improvementOpportunities: List<String>,
    val recommendedNextActions: List<String>,
    val provenanceDistinctionNote: String,
    val generatedAtEpochMs: Long = System.currentTimeMillis()
)

data class AiConversation(
    val id: Long = 0,
    val title: String,
    val associatedModule: String = "GENERAL",
    val modelUsed: String = SupportedAiModels.DEFAULT_MODEL_ID,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

data class AiChatMessage(
    val id: Long = 0,
    val conversationId: Long,
    val role: AiChatRole,
    val content: String,
    val associatedModule: String = "GENERAL",
    val modelUsed: String = SupportedAiModels.DEFAULT_MODEL_ID,
    val isFallback: Boolean = false,
    val timestampEpochMs: Long = System.currentTimeMillis()
)

data class AiRecommendation(
    val id: Long = 0,
    val module: String,
    val headline: String,
    val detail: String,
    val rationale: String,
    val confidenceNote: String,
    val isDismissed: Boolean = false,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

data class AiQuestProposal(
    val id: Long = 0,
    val proposalUuid: String,
    val title: String,
    val description: String,
    val category: AiQuestProposalCategory,
    val difficultyLabel: String,
    val objective: String,
    val completionCriteria: String,
    val suggestedDurationMinutes: Int,
    val deadlineEpochMs: Long? = null,
    val priority: SchedulePriority = SchedulePriority.MEDIUM,
    val reason: String,
    val targetSubjectId: Long? = null,
    val status: ProposalStatus = ProposalStatus.PENDING,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

data class AiDailyPlanItem(
    val id: Long = 0,
    val batchId: String,
    val planMode: PlanMode = PlanMode.DAILY,
    val taskTitle: String,
    val module: String,
    val startMinuteOfDay: Int,
    val durationMinutes: Int,
    val priority: SchedulePriority = SchedulePriority.MEDIUM,
    val reason: String,
    val dependencies: String = "",
    val optionalDeadlineEpochMs: Long? = null,
    val status: ProposalStatus = ProposalStatus.PENDING,
    val createdAtEpochMs: Long = System.currentTimeMillis()
) {
    val endMinuteOfDay: Int get() = startMinuteOfDay + durationMinutes
    fun formattedWindow(): String {
        val sh = (startMinuteOfDay / 60).coerceIn(0, 23)
        val sm = (startMinuteOfDay % 60).coerceIn(0, 59)
        val eh = ((startMinuteOfDay + durationMinutes) / 60).coerceIn(0, 23)
        val em = ((startMinuteOfDay + durationMinutes) % 60).coerceIn(0, 59)
        return "%02d:%02d – %02d:%02d".format(sh, sm, eh, em)
    }
}

data class AiModelPreferenceState(
    val selectedModelId: String = SupportedAiModels.DEFAULT_MODEL_ID,
    val backendBaseUrl: String = "",
    val aiEnabled: Boolean = true,
    val streamingEnabled: Boolean = true,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

data class AiConsentState(
    val cloudAiConsentGranted: Boolean = false,
    val sharePhysicalWithAi: Boolean = true,
    val shareAcademicWithAi: Boolean = true,
    val shareQuestsWithAi: Boolean = true,
    val shareLifeWithAi: Boolean = true,
    val shareRpgWithAi: Boolean = true,
    val consentVersion: Int = 1,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

data class SystemNotificationPreferences(
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

data class AiRequestAuditRecord(
    val id: Long = 0,
    val requestType: String,
    val modelId: String,
    val usedCloudBackend: Boolean,
    val success: Boolean,
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val latencyMs: Long = 0,
    val errorCode: String? = null,
    val timestampEpochMs: Long = System.currentTimeMillis()
)
