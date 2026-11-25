package com.example.domain.model

data class ActivityLog(
    val id: Long = 0,
    val title: String,
    val category: QuestCategory,
    val durationMinutes: Int,
    val verificationStatus: VerificationStatus,
    val xpAwarded: Int,
    val primaryAttribute: AttributeType,
    val primaryAttrXpAwarded: Int,
    val notes: String = "",
    val timestampEpochMs: Long = System.currentTimeMillis()
)

enum class WorkoutStatus(val label: String) {
    PENDING("Ready for Training"),
    IN_PROGRESS("Session Active"),
    COMPLETED("Session Complete"),
    REST_DAY("Scheduled Recovery")
}

enum class RecoveryStatus(val label: String, val guidance: String) {
    OPTIMAL(
        label = "Optimal Readiness",
        guidance = "Full training load cleared. Maintain 30-min aerobic baseline."
    ),
    MODERATE_LOAD(
        label = "Moderate Fatigue",
        guidance = "Keep pace steady; prioritize hydration and mobility cooldown."
    ),
    RECOVERY_REQUIRED(
        label = "Recovery Priority",
        guidance = "High cumulative load detected. Shift focus to light walking and sleep."
    )
}

data class PhysicalDevelopmentState(
    val baseJoggingTargetMinutes: Int = 30,
    val aiRecommendedJoggingMinutes: Int = 30,
    val completedJoggingMinutesToday: Int = 0,
    val workoutStatus: WorkoutStatus = WorkoutStatus.PENDING,
    val recoveryStatus: RecoveryStatus = RecoveryStatus.OPTIMAL,
    val weeklySessionsCompleted: Int = 0,
    val lastSessionTimestampMs: Long? = null,
    val aiTrainingNote: String = "30-minute aerobic base protocol active. Verified sessions via Health Connect carry full progression weight.",
    val activeJoggingSession: ActiveJoggingSessionState? = null,
    val todayWorkout: WorkoutSession? = null,
    val recoveryAssessment: RecoveryAssessment = RecoveryAssessment(
        state = RecoveryState.READY,
        readinessScore = 85,
        measuredLoadMinutes48h = 60,
        measuredLoadMinutes7d = 200,
        measuredSessions7d = 4,
        consecutiveActiveDays = 3,
        recommendedActions = emptyList(),
        summaryMessage = "Readiness optimal."
    ),
    val weeklyActivity: List<WorkoutSession> = emptyList(),
    val conditioningRank: String = "E"
) {
    val effectiveJoggingTarget: Int
        get() = aiRecommendedJoggingMinutes.coerceAtLeast(10)

    val joggingProgressFraction: Float
        get() = (completedJoggingMinutesToday.toFloat() / effectiveJoggingTarget).coerceIn(0f, 1f)
}

enum class NotificationCategory(val label: String) {
    QUEST_UPDATE("QUEST"),
    DAILY_QUEST_REMINDER("DAILY QUEST"),
    OVERDUE_REVISION("REVISION DUE"),
    UPCOMING_EXAM("EXAM ALERT"),
    WORKOUT_REMINDER("WORKOUT"),
    RECOVERY_WARNING("RECOVERY ALERT"),
    HABIT_REMINDER("HABIT"),
    ROUTINE_REMINDER("ROUTINE"),
    TASK_DEADLINE("TASK DUE"),
    OVERDUE_TASK("OVERDUE TASK"),
    GOAL_MILESTONE("GOAL MILESTONE"),
    DAILY_PLANNING_REMINDER("DAILY PLAN"),
    WEEKLY_REVIEW_REMINDER("WEEKLY REVIEW"),
    LEVEL_UP("LEVEL UP"),
    RANK_PROMOTION("RANK"),
    ACHIEVEMENT("MILESTONE"),
    AI_RECOMMENDATION("AI MASTER"),
    DUNGEON_UPDATE("DUNGEON GATE"),
    BOSS_PHASE("BOSS RAID"),
    SKILL_UNLOCKED("SKILL"),
    DAILY_SUMMARY("DAILY SUMMARY"),
    SYSTEM_ALERT("SYSTEM")
}

data class SystemNotification(
    val id: Long = 0,
    val title: String,
    val message: String,
    val category: NotificationCategory,
    val isRead: Boolean = false,
    val timestampEpochMs: Long = System.currentTimeMillis(),
    val deepLinkRoute: String? = null
)
