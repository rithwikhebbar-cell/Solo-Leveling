package com.example.domain.model

import kotlinx.serialization.Serializable

enum class MissionCategory(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    LONG_TERM("Long-Term")
}

enum class MissionStatus(val label: String, val isTerminal: Boolean) {
    NOT_STARTED("Not Started", false),
    ACTIVE("Active", false),
    PAUSED("Paused", false),
    COMPLETED("Completed", true),
    FAILED("Failed", true),
    ARCHIVED("Archived", true)
}

enum class MissionPriority(val label: String, val weight: Int) {
    LOW("Low", 1),
    MEDIUM("Medium", 2),
    HIGH("High", 3),
    CRITICAL("Critical", 4)
}

enum class MissionSourceType(val label: String) {
    QUEST("System Quest"),
    PERSONAL_TASK("Personal Objective"),
    LIFE_GOAL("Long-Term Goal"),
    ACADEMIC_QUEST("Academic Protocol"),
    PHYSICAL_PROTOCOL("Physical Protocol")
}

enum class MissionSourceModule(val label: String, val executeRoute: String) {
    PHYSICAL("Physical", "physical"),
    ACADEMIC("Academic", "academic_revisions"),
    LIFE("Life & Habits", "life_dashboard"),
    QUEST("Quest Board", "quest_board"),
    GENERAL("Mission Control", "mission_control")
}

@Serializable
data class MissionMetadataOverride(
    val missionId: String,
    val prerequisiteMissionIds: List<String> = emptyList(),
    val isPaused: Boolean = false,
    val isArchivedOverride: Boolean = false,
    val startDateEpochDay: Long? = null,
    val moduleName: String? = null,
    val targetValueOverride: Int? = null,
    val unitOverride: String? = null,
    val deadlineEpochDayOverride: Long? = null,
    val updatedAtEpochMs: Long = 0L
)

@Serializable
data class MissionMetadataStorePayload(
    val items: Map<String, MissionMetadataOverride> = emptyMap()
)

data class MissionValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)

data class UnifiedMissionItem(
    val missionId: String,
    val sourceType: MissionSourceType,
    val sourceRecordId: Long,
    val title: String,
    val description: String,
    val category: MissionCategory,
    val module: MissionSourceModule,
    val status: MissionStatus,
    val priority: MissionPriority,
    val currentValue: Int,
    val targetValue: Int,
    val unit: String,
    val startDateEpochDay: Long? = null,
    val deadlineEpochDay: Long? = null,
    val deadlineEpochMs: Long? = null,
    val completedAtEpochMs: Long? = null,
    val prerequisiteMissionIds: List<String> = emptyList(),
    val blockedByTitles: List<String> = emptyList(),
    val isBlocked: Boolean = false,
    val isOverdue: Boolean = false,
    val isRecoveryRestricted: Boolean = false,
    val priorityScore: Int = 0,
    val priorityReason: String = "",
    val executeRoute: String = module.executeRoute,
    val canEdit: Boolean = false,
    val canPause: Boolean = false,
    val canCompleteDirectly: Boolean = false,
    val canArchive: Boolean = false,
    val xpRewardHint: Int? = null,
    val isDemandingPhysical: Boolean = false
) {
    val progressFraction: Float
        get() = if (targetValue <= 0) 0f else (currentValue.toFloat() / targetValue.toFloat()).coerceIn(0f, 1f)

    val progressPercent: Int
        get() = (progressFraction * 100f).toInt().coerceIn(0, 100)
}

data class MissionOverviewMetrics(
    val activeCount: Int = 0,
    val completedCount: Int = 0,
    val overdueCount: Int = 0,
    val pausedCount: Int = 0,
    val blockedCount: Int = 0,
    val dailyCompletedCount: Int = 0,
    val dailyTotalCount: Int = 0,
    val dailyCompletionRatePercent: Int = 0,
    val weeklyCompletedCount: Int = 0,
    val weeklyTotalCount: Int = 0,
    val weeklyProgressPercent: Int = 0,
    val upcomingDeadlines: List<UnifiedMissionItem> = emptyList()
)

data class MissionControlSnapshot(
    val overview: MissionOverviewMetrics = MissionOverviewMetrics(),
    val priorityQueue: List<UnifiedMissionItem> = emptyList(),
    val activeMissions: List<UnifiedMissionItem> = emptyList(),
    val dailyMissions: List<UnifiedMissionItem> = emptyList(),
    val weeklyMissions: List<UnifiedMissionItem> = emptyList(),
    val longTermMissions: List<UnifiedMissionItem> = emptyList(),
    val historyMissions: List<UnifiedMissionItem> = emptyList(),
    val allNonArchived: List<UnifiedMissionItem> = emptyList(),
    val recoveryState: RecoveryState = RecoveryState.READY,
    val evaluationEpochMs: Long = 0L
)
