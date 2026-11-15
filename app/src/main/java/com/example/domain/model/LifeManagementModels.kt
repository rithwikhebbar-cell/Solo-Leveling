package com.example.domain.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId

enum class RoutineTimeSlot(val label: String, val defaultStartMinute: Int) {
    MORNING("Morning Protocol", 7 * 60),
    AFTERNOON("Afternoon Block", 13 * 60),
    EVENING("Evening Protocol", 18 * 60),
    NIGHT("Night Wind-Down", 21 * 60),
    CUSTOM("Custom Window", 10 * 60)
}

enum class RoutineActivityStatus(val label: String) {
    PENDING("Pending"),
    COMPLETED("Completed"),
    SKIPPED("Skipped (Exempt)"),
    MISSED("Missed")
}

data class LifeRoutine(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val timeSlot: RoutineTimeSlot = RoutineTimeSlot.MORNING,
    val startMinuteOfDay: Int = 7 * 60,
    val scheduledDays: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    val isEnabled: Boolean = true,
    val targetDurationMinutes: Int = 30,
    val reminderEnabled: Boolean = true,
    val isRestExemptAllowed: Boolean = true,
    val createdAtEpochMs: Long = System.currentTimeMillis()
) {
    fun scheduledDaysDisplay(): String {
        if (scheduledDays.size == 7) return "Every Day"
        val weekdays = setOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY
        )
        if (scheduledDays == weekdays) return "Weekdays"
        val weekends = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
        if (scheduledDays == weekends) return "Weekends"
        return DayOfWeek.entries.filter { it in scheduledDays }.joinToString(", ") {
            it.name.take(3)
        }
    }
}

data class RoutineActivity(
    val id: Long = 0,
    val routineId: Long,
    val title: String,
    val durationMinutes: Int = 10,
    val orderIndex: Int = 0,
    val isOptional: Boolean = false
)

data class RoutineCompletionRecord(
    val id: Long = 0,
    val routineId: Long,
    val activityId: Long,
    val dateEpochDay: Long,
    val status: RoutineActivityStatus = RoutineActivityStatus.COMPLETED,
    val note: String = "",
    val completedAtEpochMs: Long = System.currentTimeMillis()
)

data class RoutineDaySummary(
    val routine: LifeRoutine,
    val activities: List<RoutineActivity>,
    val completionsToday: Map<Long, RoutineCompletionRecord>,
    val isScheduledToday: Boolean,
    val completedCount: Int,
    val requiredCount: Int,
    val completionFraction: Float,
    val currentStreakDays: Int,
    val consistencyRate30dPercent: Int,
    val missedRequiredCount: Int
)

enum class HabitType(val label: String) {
    BINARY("Yes / No"),
    DURATION("Timed (min)"),
    COUNT("Repetition Count"),
    MEASURABLE("Measurable Target"),
    CUSTOM("Custom Protocol")
}

enum class HabitCategory(val label: String, val targetAttribute: AttributeType) {
    DISCIPLINE("Discipline", AttributeType.DIS),
    HEALTH("Health & Recovery", AttributeType.VIT),
    MINDSET("Mindset & Reflection", AttributeType.WIS),
    PRODUCTIVITY("Focus & Execution", AttributeType.DIS),
    SOCIAL("Social & Communication", AttributeType.CHA),
    FINANCE("Financial Order", AttributeType.WIS),
    LEARNING("Skill & Reading", AttributeType.INT)
}

enum class HabitDayStatus(val label: String) {
    COMPLETED("Completed"),
    PARTIAL("Partial"),
    SKIPPED("Skipped / Rest Exempt"),
    MISSED("Missed"),
    NOT_SCHEDULED("Not Scheduled")
}

data class Habit(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val habitType: HabitType = HabitType.BINARY,
    val category: HabitCategory = HabitCategory.DISCIPLINE,
    val targetValue: Double = 1.0,
    val unit: String = "check",
    val scheduledDays: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    val preferredStartMinute: Int? = null,
    val reminderEnabled: Boolean = true,
    val isRestDayProtected: Boolean = true,
    val isArchived: Boolean = false,
    val createdAtEpochMs: Long = System.currentTimeMillis()
) {
    fun isScheduledOn(dayOfWeek: DayOfWeek): Boolean = dayOfWeek in scheduledDays
}

data class HabitCompletionRecord(
    val id: Long = 0,
    val habitId: Long,
    val dateEpochDay: Long,
    val status: HabitDayStatus,
    val achievedValue: Double = 0.0,
    val skipReason: String = "",
    val note: String = "",
    val recordedAtEpochMs: Long = System.currentTimeMillis()
)

data class HabitCalendarCell(
    val dateEpochDay: Long,
    val status: HabitDayStatus,
    val achievedValue: Double = 0.0
)

data class HabitAnalyticsSummary(
    val habit: Habit,
    val todayStatus: HabitDayStatus,
    val todayAchievedValue: Double,
    val currentStreakDays: Int,
    val longestStreakDays: Int,
    val weeklyCompletionRatePercent: Int,
    val monthlyCompletionRatePercent: Int,
    val completedDays30d: Int,
    val partialDays30d: Int,
    val skippedDays30d: Int,
    val missedDays30d: Int,
    val recentCalendar: List<HabitCalendarCell>
)

enum class TaskCategory(val label: String, val primaryAttribute: AttributeType) {
    PERSONAL("Personal", AttributeType.DIS),
    HOUSEHOLD("Household", AttributeType.DIS),
    ADMINISTRATIVE("Administrative", AttributeType.WIS),
    FINANCIAL("Financial", AttributeType.WIS),
    SOCIAL("Social", AttributeType.CHA),
    CAREER("Career", AttributeType.DIS),
    LEARNING("Learning", AttributeType.INT),
    MISCELLANEOUS("Miscellaneous", AttributeType.DIS)
}

enum class TaskPriority(val label: String, val weight: Int) {
    LOW("Low", 1),
    MEDIUM("Medium", 2),
    HIGH("High", 3),
    CRITICAL("Critical", 4)
}

enum class TaskRecurrenceType(val label: String) {
    NONE("One-Time"),
    DAILY("Daily"),
    WEEKDAYS("Weekdays (Mon–Fri)"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    CUSTOM_INTERVAL("Every N Days")
}

enum class PersonalTaskStatus(val label: String) {
    PENDING("Pending"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    POSTPONED("Postponed"),
    ARCHIVED("Archived")
}

data class TaskRecurrenceConfig(
    val id: Long = 0,
    val taskId: Long,
    val recurrenceType: TaskRecurrenceType = TaskRecurrenceType.NONE,
    val intervalDays: Int = 1,
    val selectedDays: Set<DayOfWeek> = emptySet(),
    val endDateEpochDay: Long? = null
)

data class PersonalTask(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val category: TaskCategory = TaskCategory.PERSONAL,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val status: PersonalTaskStatus = PersonalTaskStatus.PENDING,
    val dueDateEpochDay: Long? = null,
    val scheduledStartMinute: Int? = null,
    val estimatedMinutes: Int = 30,
    val linkedGoalId: Long? = null,
    val isTimeSensitive: Boolean = false,
    val postponeCount: Int = 0,
    val timezoneId: String = ZoneId.systemDefault().id,
    val recurrence: TaskRecurrenceConfig? = null,
    val completedAtEpochMs: Long? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis()
) {
    fun isOverdue(todayEpochDay: Long): Boolean {
        if (status == PersonalTaskStatus.COMPLETED || status == PersonalTaskStatus.ARCHIVED) return false
        return dueDateEpochDay != null && dueDateEpochDay < todayEpochDay
    }

    fun isDueToday(todayEpochDay: Long): Boolean {
        if (status == PersonalTaskStatus.COMPLETED || status == PersonalTaskStatus.ARCHIVED) return false
        return dueDateEpochDay == todayEpochDay
    }
}

enum class GoalTimeframe(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    QUARTERLY("Quarterly"),
    ANNUAL("Annual"),
    LONG_TERM("Long-Term")
}

enum class GoalStatus(val label: String) {
    ACTIVE("Active"),
    PAUSED("Paused"),
    COMPLETED("Completed"),
    ARCHIVED("Archived")
}

data class GoalMilestone(
    val id: Long = 0,
    val goalId: Long,
    val title: String,
    val weight: Int = 1,
    val dueDateEpochDay: Long? = null,
    val isCompleted: Boolean = false,
    val completedAtEpochMs: Long? = null,
    val orderIndex: Int = 0
)

data class GoalProgressRecord(
    val id: Long = 0,
    val goalId: Long,
    val note: String,
    val recordedProgressPercent: Int,
    val adjustmentSummary: String = "",
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

data class LifeGoal(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val timeframe: GoalTimeframe = GoalTimeframe.MONTHLY,
    val status: GoalStatus = GoalStatus.ACTIVE,
    val targetAttribute: AttributeType = AttributeType.DIS,
    val completionCriteria: String = "",
    val startDateEpochDay: Long,
    val targetDateEpochDay: Long,
    val linkedModule: String = "LIFE",
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

data class GoalWithDetails(
    val goal: LifeGoal,
    val milestones: List<GoalMilestone>,
    val progressRecords: List<GoalProgressRecord>,
    val computedProgressPercent: Int,
    val completedMilestonesCount: Int,
    val totalMilestonesCount: Int,
    val linkedCompletedTasksCount: Int,
    val isBehindSchedule: Boolean
)

enum class ScheduledSourceType(val label: String, val moduleTag: String) {
    ROUTINE("Routine", "LIFE"),
    HABIT("Habit", "LIFE"),
    PERSONAL_TASK("Personal Task", "LIFE"),
    GOAL_MILESTONE("Goal Milestone", "LIFE"),
    ACADEMIC_STUDY("Study Session", "ACADEMIC"),
    ACADEMIC_REVISION("Spaced Revision", "ACADEMIC"),
    ACADEMIC_EXAM("Examination", "ACADEMIC"),
    PHYSICAL_TRAINING("Physical Training", "PHYSICAL"),
    AI_PLAN_PROPOSAL("AI Plan Block", "AI"),
    CUSTOM_EVENT("Scheduled Event", "LIFE")
}

enum class CalendarViewMode(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    AGENDA("Agenda")
}

data class ScheduledActivityReference(
    val id: Long = 0,
    val sourceType: ScheduledSourceType,
    val sourceRecordId: Long,
    val title: String,
    val subtitle: String = "",
    val dateEpochDay: Long,
    val startMinuteOfDay: Int,
    val durationMinutes: Int,
    val isAllDay: Boolean = false,
    val isRecurringInstance: Boolean = false,
    val isCompletedSnapshot: Boolean = false,
    val timezoneId: String = ZoneId.systemDefault().id,
    val notes: String = "",
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

data class ScheduledActivityItem(
    val referenceId: Long = 0,
    val sourceType: ScheduledSourceType,
    val sourceRecordId: Long,
    val title: String,
    val subtitle: String,
    val dateEpochDay: Long,
    val startMinuteOfDay: Int,
    val durationMinutes: Int,
    val isCompleted: Boolean = false,
    val isAllDay: Boolean = false,
    val isReadOnlyExternal: Boolean = false,
    val priorityWeight: Int = 2,
    val timezoneId: String = ZoneId.systemDefault().id,
    val hasConflict: Boolean = false
) {
    val endMinuteOfDay: Int get() = (startMinuteOfDay + durationMinutes).coerceAtMost(24 * 60)

    fun formattedWindow(): String {
        if (isAllDay) return "All Day"
        val sh = (startMinuteOfDay / 60).coerceIn(0, 23)
        val sm = (startMinuteOfDay % 60).coerceIn(0, 59)
        val eh = (endMinuteOfDay / 60).coerceIn(0, 24)
        val em = (endMinuteOfDay % 60).coerceIn(0, 59)
        return "%02d:%02d – %02d:%02d".format(sh, sm, eh, em)
    }
}

data class ScheduleConflict(
    val first: ScheduledActivityItem,
    val second: ScheduledActivityItem,
    val overlapMinutes: Int,
    val description: String
)

data class TimeSlotSuggestion(
    val dateEpochDay: Long,
    val startMinuteOfDay: Int,
    val durationMinutes: Int
) {
    fun formattedWindow(): String {
        val sh = (startMinuteOfDay / 60).coerceIn(0, 23)
        val sm = (startMinuteOfDay % 60).coerceIn(0, 59)
        val endMin = (startMinuteOfDay + durationMinutes).coerceAtMost(24 * 60)
        val eh = (endMin / 60).coerceIn(0, 24)
        val em = (endMin % 60).coerceIn(0, 59)
        return "%02d:%02d – %02d:%02d".format(sh, sm, eh, em)
    }
}

data class AvailabilityWindow(
    val startMinuteOfDay: Int = 7 * 60,
    val endMinuteOfDay: Int = 22 * 60,
    val restDays: Set<DayOfWeek> = emptySet(),
    val zoneId: ZoneId = ZoneId.systemDefault()
)

data class StreakSummary(
    val currentStreakDays: Int,
    val longestStreakDays: Int,
    val exemptRestDaysCount: Int,
    val scheduledDaysEvaluated: Int,
    val missedDaysCount: Int,
    val streakCapBonusPercent: Int
)

data class DisciplineAssessment(
    val id: Long = 0,
    val dateEpochDay: Long,
    val disciplineScore: Int,
    val routineConsistencyPercent: Int,
    val habitConsistencyPercent: Int,
    val taskCompletionPercent: Int,
    val scheduleAdherencePercent: Int,
    val goalProgressPercent: Int,
    val strongestArea: String,
    val weakestArea: String,
    val missedCommitmentsCount: Int,
    val exemptRestDaysCount: Int,
    val reflectionSummary: String,
    val calculatedAtEpochMs: Long = System.currentTimeMillis()
)

data class WeeklyReviewSummary(
    val weekStartEpochDay: Long,
    val weekEndEpochDay: Long,
    val averageDisciplineScore: Int,
    val routineActivitiesCompleted: Int,
    val routineActivitiesScheduled: Int,
    val habitsCompletedCount: Int,
    val habitsScheduledCount: Int,
    val tasksCompletedCount: Int,
    val tasksOverdueCount: Int,
    val goalMilestonesCompletedCount: Int,
    val scheduleConflictsCount: Int,
    val studyMinutesTotal: Int,
    val physicalSessionsTotal: Int,
    val strongestDomain: String,
    val priorityAdjustmentAdvice: String
)

data class LifeDashboardSummary(
    val todayDisciplineScore: Int = 0,
    val overallConsistencyPercent: Int = 0,
    val activeDisciplineStreakDays: Int = 0,
    val activeRoutinesCount: Int = 0,
    val completedRoutinesToday: Int = 0,
    val activeHabitsCount: Int = 0,
    val completedHabitsToday: Int = 0,
    val pendingTasksCount: Int = 0,
    val dueTodayTasksCount: Int = 0,
    val overdueTasksCount: Int = 0,
    val activeGoalsCount: Int = 0,
    val todayScheduledCount: Int = 0,
    val todayConflictCount: Int = 0,
    val lifeXpToday: Int = 0,
    val dailyLifeXpCap: Int = 200
)

enum class LifeActivityKind(val label: String) {
    ROUTINE_COMPLETION("Routine Activity Completed"),
    HABIT_COMPLETION("Habit Completed"),
    PERSONAL_TASK("Personal Task Completed"),
    GOAL_MILESTONE("Goal Milestone Achieved"),
    GOAL_REVIEW("Goal Review Logged"),
    SOCIAL_COMMITMENT("Social Commitment Fulfilled"),
    REFLECTION("Structured Discipline Review")
}

fun Set<DayOfWeek>.toCsvString(): String =
    DayOfWeek.entries.filter { it in this }.joinToString(",") { it.value.toString() }

fun String.parseDayOfWeekCsv(): Set<DayOfWeek> =
    split(",")
        .mapNotNull { it.trim().toIntOrNull() }
        .mapNotNull { value -> DayOfWeek.entries.firstOrNull { it.value == value } }
        .toSet()

fun epochMsToLocalDate(epochMs: Long, zoneId: ZoneId = ZoneId.systemDefault()) =
    Instant.ofEpochMilli(epochMs).atZone(zoneId).toLocalDate()
