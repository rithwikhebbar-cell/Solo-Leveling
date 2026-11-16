package com.example.domain.model

import java.time.ZoneId
import java.util.UUID

enum class SystemEventType(val label: String, val defaultModule: String) {
    PLAYER_PROFILE_UPDATED("Player Profile Updated", "PLAYER"),
    PHYSICAL_ACTIVITY_RECORDED("Physical Activity Recorded", "PHYSICAL"),
    WORKOUT_COMPLETED("Workout Completed", "PHYSICAL"),
    JOGGING_COMPLETED("Jogging Completed", "PHYSICAL"),
    ACADEMIC_SESSION_COMPLETED("Academic Session Completed", "ACADEMIC"),
    REVISION_COMPLETED("Revision Completed", "ACADEMIC"),
    ASSESSMENT_RECORDED("Assessment Recorded", "ACADEMIC"),
    HABIT_COMPLETED("Habit Completed", "LIFE"),
    ROUTINE_COMPLETED("Routine Completed", "LIFE"),
    TASK_COMPLETED("Task Completed", "LIFE"),
    GOAL_MILESTONE_COMPLETED("Goal Milestone Completed", "LIFE"),
    DUNGEON_COMPLETED("Dungeon Completed", "RPG"),
    BOSS_BATTLE_COMPLETED("Boss Battle Completed", "RPG"),
    SKILL_UNLOCKED("Skill Unlocked", "RPG"),
    ACHIEVEMENT_UNLOCKED("Achievement Unlocked", "RPG"),
    TITLE_EARNED("Title Earned", "RPG"),
    RECOVERY_STATE_CHANGED("Recovery State Changed", "PHYSICAL"),
    DAILY_RESET_COMPLETED("Daily Reset Completed", "SYSTEM"),
    SYNC_COMPLETED("Cloud Sync Completed", "CLOUD"),
    SYNC_FAILED("Cloud Sync Failed", "CLOUD"),
    INTELLIGENCE_REFRESHED("Intelligence Refreshed", "INTELLIGENCE"),
    DAILY_EXECUTION_UPDATED("Daily Execution Updated", "EXECUTION"),
    AUTOMATION_FAILED("Automation Failed", "AUTOMATION"),
    SYSTEM_RECOVERY_EXECUTED("System Recovery Executed", "SYSTEM"),
    // Milestone 11 — Shadow Monarch Events
    SHADOW_EXTRACTION_AVAILABLE("Shadow Extraction Available", "SHADOW"),
    SHADOW_RECRUITED("New Shadow Recruited", "SHADOW"),
    SHADOW_LEVEL_UP("Shadow Level Up", "SHADOW"),
    SHADOW_EVOLUTION_AVAILABLE("Shadow Evolution Available", "SHADOW"),
    SHADOW_RANK_INCREASED("Shadow Rank Increased", "SHADOW"),
    MONARCH_LEVEL_UP("Monarch Level Up", "SHADOW"),
    MONARCH_SKILL_UNLOCKED("New Monarch Skill Unlocked", "SHADOW"),
    SHADOW_ARMY_CAPACITY_REACHED("Army Capacity Reached", "SHADOW"),
    SHADOW_FORMATION_UPDATED("Formation Updated", "SHADOW"),
    SHADOW_QUEST_COMPLETED("Shadow Quest Completed", "SHADOW")
}

enum class EventProcessingStatus(val label: String) {
    PENDING("Pending"),
    PROCESSING("Processing"),
    PROCESSED("Processed"),
    FAILED("Failed"),
    DEAD_LETTER("Requires Recovery"),
    DEDUPLICATED("Deduplicated")
}

data class SystemEvent(
    val eventId: String = UUID.randomUUID().toString(),
    val eventType: SystemEventType,
    val timestampEpochMs: Long = System.currentTimeMillis(),
    val sourceModule: String = eventType.defaultModule,
    val relatedEntityId: String = "",
    val metadata: Map<String, String> = emptyMap(),
    val schemaVersion: Int = 1,
    val processingStatus: EventProcessingStatus = EventProcessingStatus.PENDING,
    val retryCount: Int = 0,
    val errorMessage: String? = null,
    val processedAtEpochMs: Long? = null,
    val correlationId: String = eventId
)

data class EventDiagnosticsSummary(
    val totalCount: Int = 0,
    val pendingCount: Int = 0,
    val processedCount: Int = 0,
    val failedCount: Int = 0,
    val deadLetterCount: Int = 0,
    val deduplicatedCount: Int = 0
)

enum class DailyExecutionStatus(val label: String, val isTerminal: Boolean) {
    SCHEDULED("Scheduled", false),
    READY("Ready", false),
    IN_PROGRESS("In Progress", false),
    PAUSED("Paused", false),
    COMPLETED("Completed", true),
    PARTIALLY_COMPLETED("Partial", false),
    MISSED("Missed", false),
    CANCELLED("Cancelled", true),
    RESCHEDULED("Rescheduled", true)
}

data class DailyExecutionItem(
    val executionKey: String,
    val sourceModule: String,
    val activityType: ScheduledSourceType,
    val sourceRecordId: Long,
    val title: String,
    val subtitle: String,
    val dateEpochDay: Long,
    val plannedStartMinute: Int,
    val plannedEndMinute: Int,
    val durationMinutes: Int,
    val actualStartEpochMs: Long? = null,
    val actualCompletionEpochMs: Long? = null,
    val completedMinutes: Int = 0,
    val status: DailyExecutionStatus = DailyExecutionStatus.SCHEDULED,
    val priorityWeight: Int = 2,
    val isAllDay: Boolean = false,
    val hasConflict: Boolean = false,
    val recoveryRestricted: Boolean = false,
    val evidenceReference: String = "",
    val notes: String = ""
) {
    fun formattedPlannedWindow(): String {
        if (isAllDay) return "All Day"
        val sh = (plannedStartMinute / 60).coerceIn(0, 23)
        val sm = (plannedStartMinute % 60).coerceIn(0, 59)
        val eh = (plannedEndMinute / 60).coerceIn(0, 23)
        val em = (plannedEndMinute % 60).coerceIn(0, 59)
        return "%02d:%02d–%02d:%02d".format(sh, sm, eh, em)
    }
}

data class DailyExecutionSummary(
    val dateEpochDay: Long,
    val items: List<DailyExecutionItem> = emptyList(),
    val completedCount: Int = 0,
    val partialCount: Int = 0,
    val activeOrReadyCount: Int = 0,
    val missedCount: Int = 0,
    val conflictCount: Int = 0,
    val recoveryRestrictedCount: Int = 0
)

enum class AutomationTriggerType(val label: String) {
    PERIODIC_WORKMANAGER("WorkManager Periodic"),
    ONE_TIME_RECOVERY("WorkManager One-Time"),
    STARTUP_CATCHUP("App Startup Check"),
    CONNECTIVITY_RESTORED("Network Restored"),
    MANUAL("Manual Run")
}

enum class AutomationExecutionStatus(val label: String) {
    IDLE("Scheduled / Idle"),
    RUNNING("Running"),
    SUCCEEDED("Succeeded"),
    RETRY_SCHEDULED("Retry Scheduled"),
    FAILED("Failed"),
    DISABLED("Disabled by User")
}

data class AutomationJobSpec(
    val automationId: String,
    val name: String,
    val description: String,
    val triggerType: AutomationTriggerType,
    val frequencyLabel: String,
    val constraintsSummary: String,
    val isEnabled: Boolean = true,
    val lastExecutionEpochMs: Long? = null,
    val nextEligibleEpochMs: Long? = null,
    val currentStatus: AutomationExecutionStatus = AutomationExecutionStatus.IDLE,
    val retryCount: Int = 0,
    val lastFailureReason: String? = null,
    val executionCount: Int = 0
)

data class AutomationExecutionRecord(
    val id: Long = 0,
    val executionUuid: String = UUID.randomUUID().toString(),
    val automationId: String,
    val triggerSource: String,
    val startedAtEpochMs: Long,
    val completedAtEpochMs: Long,
    val status: AutomationExecutionStatus,
    val summaryMessage: String,
    val retryCount: Int = 0,
    val failureReason: String? = null
)

data class DailyCycleExecution(
    val epochDay: Long,
    val timezoneId: String,
    val executedAtEpochMs: Long,
    val triggerSource: String,
    val dailyQuestsResetCount: Int,
    val missedDaysCaughtUp: Int,
    val status: String,
    val notes: String
)

enum class IntegrityFindingCategory(val label: String) {
    PLAYER_PROGRESSION("Player & Progression"),
    PHYSICAL("Physical & Recovery"),
    ACADEMIC("Academic"),
    LIFE_MANAGEMENT("Life & Discipline"),
    RPG_AUDIT("RPG & Rewards"),
    CLOUD_SYNC("Cloud Synchronization"),
    SYSTEM_EVENTS("System Event Log")
}

enum class IntegritySeverity(val label: String) {
    INFO("Info"),
    WARNING("Warning"),
    CRITICAL("Critical")
}

enum class IntegrityFindingStatus(val label: String) {
    OPEN("Open"),
    REPAIRED("Repaired"),
    DISMISSED("Dismissed")
}

data class SystemIntegrityFinding(
    val findingUuid: String,
    val category: IntegrityFindingCategory,
    val severity: IntegritySeverity,
    val title: String,
    val description: String,
    val affectedTable: String,
    val affectedRecordKey: String,
    val repairProposal: String,
    val isDestructiveRepair: Boolean = false,
    val status: IntegrityFindingStatus = IntegrityFindingStatus.OPEN,
    val detectedAtEpochMs: Long = System.currentTimeMillis(),
    val resolvedAtEpochMs: Long? = null
)

enum class SystemHealthLevel(val label: String) {
    HEALTHY("Healthy"),
    WARNING("Warning"),
    DEGRADED("Degraded"),
    CRITICAL("Critical")
}

data class SystemHealthComponentStatus(
    val component: String,
    val status: SystemHealthLevel,
    val evidence: String,
    val timestampEpochMs: Long,
    val severity: IntegritySeverity,
    val recommendedAction: String
)

data class SystemHealthReport(
    val overallStatus: SystemHealthLevel = SystemHealthLevel.HEALTHY,
    val components: List<SystemHealthComponentStatus> = emptyList(),
    val evaluatedAtEpochMs: Long = System.currentTimeMillis()
)

data class ModuleIntegrationInfo(
    val moduleId: String,
    val title: String,
    val statusLabel: String,
    val healthLevel: SystemHealthLevel,
    val summary: String,
    val routeKey: String
)

data class SystemPerformanceMetrics(
    val databaseVersion: Int = 10,
    val indexedActivityLogsReady: Boolean = true,
    val cachedProfileFingerprint: String = "",
    val totalStoredEvents: Int = 0,
    val pendingSyncQueueSize: Int = 0,
    val activeAutomationsCount: Int = 0,
    val totalDailyExecutionItems: Int = 0,
    val lastSweepDurationMs: Long = 0L,
    val optimizationNotes: List<String> = emptyList()
)

data class RecoveryOperationRecord(
    val operationUuid: String = UUID.randomUUID().toString(),
    val operationType: String,
    val reason: String,
    val status: String,
    val details: String,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val completedAtEpochMs: Long? = null
)

data class SystemControlPreferences(
    val enableAutoProgressionEvents: Boolean = true,
    val enableAutoIntelligenceRefresh: Boolean = true,
    val enableDailyCycleAutomation: Boolean = true,
    val enableCloudAutoSync: Boolean = true,
    val enablePerformanceCaching: Boolean = true,
    val enableBackgroundNotifications: Boolean = true,
    val preferredTimezoneId: String = ZoneId.systemDefault().id,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

data class RealDeviceVerificationItem(
    val id: Int,
    val title: String,
    val environmentRequired: String,
    val verificationSteps: String
)
