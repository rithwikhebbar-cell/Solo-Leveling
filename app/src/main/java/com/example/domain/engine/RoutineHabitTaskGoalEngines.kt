package com.example.domain.engine

import com.example.domain.model.GoalMilestone
import com.example.domain.model.GoalStatus
import com.example.domain.model.GoalWithDetails
import com.example.domain.model.Habit
import com.example.domain.model.HabitAnalyticsSummary
import com.example.domain.model.HabitCalendarCell
import com.example.domain.model.HabitCompletionRecord
import com.example.domain.model.HabitDayStatus
import com.example.domain.model.LifeGoal
import com.example.domain.model.LifeRoutine
import com.example.domain.model.PersonalTask
import com.example.domain.model.PersonalTaskStatus
import com.example.domain.model.RoutineActivity
import com.example.domain.model.RoutineActivityStatus
import com.example.domain.model.RoutineCompletionRecord
import com.example.domain.model.RoutineDaySummary
import com.example.domain.model.TaskCategory
import com.example.domain.model.TaskRecurrenceType
import java.time.DayOfWeek
import java.time.LocalDate

enum class TaskSortOption(val label: String) {
    DUE_DATE("Due Date"),
    PRIORITY("Priority"),
    CATEGORY("Category"),
    STATUS("Status")
}

object RoutineEngine {

    fun isScheduledOn(routine: LifeRoutine, epochDay: Long): Boolean {
        if (!routine.isEnabled) return false
        val day = LocalDate.ofEpochDay(epochDay).dayOfWeek
        return day in routine.scheduledDays
    }

    fun summarizeRoutineForDay(
        routine: LifeRoutine,
        activities: List<RoutineActivity>,
        completions: List<RoutineCompletionRecord>,
        targetEpochDay: Long,
        exemptRestDays: Set<DayOfWeek> = emptySet()
    ): RoutineDaySummary {
        val sortedActivities = activities.filter { it.routineId == routine.id }.sortedBy { it.orderIndex }
        val todayRecords = completions
            .filter { it.routineId == routine.id && it.dateEpochDay == targetEpochDay }
            .associateBy { it.activityId }

        val scheduledToday = isScheduledOn(routine, targetEpochDay)
        val requiredActivities = sortedActivities.filter { !it.isOptional }
        val completedCount = requiredActivities.count { act ->
            val st = todayRecords[act.id]?.status
            st == RoutineActivityStatus.COMPLETED || st == RoutineActivityStatus.SKIPPED
        }
        val missedCount = requiredActivities.count { act ->
            todayRecords[act.id]?.status == RoutineActivityStatus.MISSED
        }
        val reqCount = requiredActivities.size
        val fraction = if (reqCount == 0) {
            if (sortedActivities.isNotEmpty() && sortedActivities.all { todayRecords[it.id]?.status == RoutineActivityStatus.COMPLETED }) 1f else 0f
        } else {
            (completedCount.toFloat() / reqCount).coerceIn(0f, 1f)
        }

        // 30-day consistency computed only over scheduled, non-rest-exempt days
        var evaluatedDays = 0
        var successfulDays = 0
        var currentStreak = 0
        var streakOpen = true

        for (offset in 0..29) {
            val day = targetEpochDay - offset
            val dow = LocalDate.ofEpochDay(day).dayOfWeek
            if (!isScheduledOn(routine, day)) continue

            val dayRecords = completions.filter { it.routineId == routine.id && it.dateEpochDay == day }
            val isRestExemptDay = routine.isRestExemptAllowed &&
                (dow in exemptRestDays || (dayRecords.isNotEmpty() && dayRecords.all { it.status == RoutineActivityStatus.SKIPPED }))
            if (isRestExemptDay) {
                continue
            }

            val dayReqCompleted = if (reqCount == 0) {
                dayRecords.any { it.status == RoutineActivityStatus.COMPLETED }
            } else {
                val doneIds = dayRecords
                    .filter { it.status == RoutineActivityStatus.COMPLETED || it.status == RoutineActivityStatus.SKIPPED }
                    .map { it.activityId }
                    .toSet()
                requiredActivities.all { it.id in doneIds }
            }

            if (offset == 0 && !dayReqCompleted && dayRecords.none { it.status == RoutineActivityStatus.MISSED }) {
                // Today not completed yet; do not prematurely break yesterday's active streak or count today as failure
                continue
            }

            evaluatedDays++
            if (dayReqCompleted) {
                successfulDays++
                if (streakOpen) currentStreak++
            } else {
                streakOpen = false
            }
        }

        val consistencyPct = if (evaluatedDays == 0) 0 else ((successfulDays * 100) / evaluatedDays).coerceIn(0, 100)

        return RoutineDaySummary(
            routine = routine,
            activities = sortedActivities,
            completionsToday = todayRecords,
            isScheduledToday = scheduledToday,
            completedCount = completedCount,
            requiredCount = reqCount,
            completionFraction = fraction,
            currentStreakDays = currentStreak,
            consistencyRate30dPercent = consistencyPct,
            missedRequiredCount = missedCount
        )
    }
}

object HabitEngine {

    fun resolveStatusForDay(
        habit: Habit,
        epochDay: Long,
        todayEpochDay: Long,
        record: HabitCompletionRecord?,
        exemptRestDays: Set<DayOfWeek> = emptySet()
    ): HabitDayStatus {
        val dow = LocalDate.ofEpochDay(epochDay).dayOfWeek
        if (!habit.isScheduledOn(dow)) {
            return HabitDayStatus.NOT_SCHEDULED
        }
        if (record != null) {
            return record.status
        }
        if (habit.isRestDayProtected && dow in exemptRestDays) {
            return HabitDayStatus.SKIPPED
        }
        return if (epochDay < todayEpochDay) {
            HabitDayStatus.MISSED
        } else {
            // Today or future without an explicit log yet -> treat safely in UI via NOT_SCHEDULED vs PENDING check,
            // returning PARTIAL/NOT_SCHEDULED is avoided; return SKIPPED? Wait: return MISSED only strictly for past days.
            HabitDayStatus.NOT_SCHEDULED
        }
    }

    fun evaluateLoggedStatus(habit: Habit, achievedValue: Double): HabitDayStatus {
        val safeValue = achievedValue.coerceAtLeast(0.0)
        return when {
            safeValue >= habit.targetValue.coerceAtLeast(1.0) -> HabitDayStatus.COMPLETED
            safeValue > 0.0 -> HabitDayStatus.PARTIAL
            else -> HabitDayStatus.MISSED
        }
    }

    fun buildAnalyticsSummary(
        habit: Habit,
        allRecords: List<HabitCompletionRecord>,
        todayEpochDay: Long,
        exemptRestDays: Set<DayOfWeek> = emptySet()
    ): HabitAnalyticsSummary {
        val recordsByDay = allRecords
            .filter { it.habitId == habit.id }
            .associateBy { it.dateEpochDay }

        val todayRecord = recordsByDay[todayEpochDay]
        val todayDow = LocalDate.ofEpochDay(todayEpochDay).dayOfWeek
        val todayStatus = when {
            !habit.isScheduledOn(todayDow) -> HabitDayStatus.NOT_SCHEDULED
            todayRecord != null -> todayRecord.status
            habit.isRestDayProtected && todayDow in exemptRestDays -> HabitDayStatus.SKIPPED
            else -> HabitDayStatus.NOT_SCHEDULED // pending for today until logged
        }

        val cells = (29 downTo 0).map { offset ->
            val day = todayEpochDay - offset
            val rec = recordsByDay[day]
            val dow = LocalDate.ofEpochDay(day).dayOfWeek
            val st = when {
                !habit.isScheduledOn(dow) -> HabitDayStatus.NOT_SCHEDULED
                rec != null -> rec.status
                habit.isRestDayProtected && dow in exemptRestDays -> HabitDayStatus.SKIPPED
                day < todayEpochDay -> HabitDayStatus.MISSED
                else -> HabitDayStatus.NOT_SCHEDULED
            }
            HabitCalendarCell(
                dateEpochDay = day,
                status = st,
                achievedValue = rec?.achievedValue ?: 0.0
            )
        }

        fun completionRateForWindow(daysWindow: Int): Int {
            var scheduledCount = 0
            var score = 0.0
            for (offset in 0 until daysWindow) {
                val day = todayEpochDay - offset
                val rec = recordsByDay[day]
                val dow = LocalDate.ofEpochDay(day).dayOfWeek
                if (!habit.isScheduledOn(dow)) continue
                if (habit.isRestDayProtected && dow in exemptRestDays) continue
                if (rec?.status == HabitDayStatus.SKIPPED) continue
                if (offset == 0 && rec == null) continue // today hasn't ended yet

                scheduledCount++
                score += when (rec?.status) {
                    HabitDayStatus.COMPLETED -> 1.0
                    HabitDayStatus.PARTIAL -> 0.5
                    else -> 0.0
                }
            }
            return if (scheduledCount == 0) 0 else ((score * 100.0) / scheduledCount).toInt().coerceIn(0, 100)
        }

        val streakSummary = StreakEngine.evaluateHabitStreak(
            habit = habit,
            recordsByDay = recordsByDay,
            todayEpochDay = todayEpochDay,
            exemptRestDays = exemptRestDays
        )

        return HabitAnalyticsSummary(
            habit = habit,
            todayStatus = todayStatus,
            todayAchievedValue = todayRecord?.achievedValue ?: 0.0,
            currentStreakDays = streakSummary.currentStreakDays,
            longestStreakDays = streakSummary.longestStreakDays,
            weeklyCompletionRatePercent = completionRateForWindow(7),
            monthlyCompletionRatePercent = completionRateForWindow(30),
            completedDays30d = cells.count { it.status == HabitDayStatus.COMPLETED },
            partialDays30d = cells.count { it.status == HabitDayStatus.PARTIAL },
            skippedDays30d = cells.count { it.status == HabitDayStatus.SKIPPED },
            missedDays30d = cells.count { it.status == HabitDayStatus.MISSED },
            recentCalendar = cells
        )
    }
}

object PersonalTaskEngine {

    fun computeNextDueDate(task: PersonalTask, referenceEpochDay: Long): Long? {
        val recurrence = task.recurrence ?: return null
        if (recurrence.recurrenceType == TaskRecurrenceType.NONE) return null

        val baseDay = maxOf(task.dueDateEpochDay ?: referenceEpochDay, referenceEpochDay)
        val candidateDay = when (recurrence.recurrenceType) {
            TaskRecurrenceType.NONE -> return null
            TaskRecurrenceType.DAILY -> baseDay + 1L
            TaskRecurrenceType.WEEKDAYS -> {
                var date = LocalDate.ofEpochDay(baseDay).plusDays(1)
                while (date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY) {
                    date = date.plusDays(1)
                }
                date.toEpochDay()
            }
            TaskRecurrenceType.WEEKLY -> {
                if (recurrence.selectedDays.isEmpty()) {
                    baseDay + 7L
                } else {
                    var date = LocalDate.ofEpochDay(baseDay).plusDays(1)
                    var guard = 0
                    while (date.dayOfWeek !in recurrence.selectedDays && guard < 14) {
                        date = date.plusDays(1)
                        guard++
                    }
                    date.toEpochDay()
                }
            }
            TaskRecurrenceType.MONTHLY -> LocalDate.ofEpochDay(baseDay).plusMonths(1).toEpochDay()
            TaskRecurrenceType.CUSTOM_INTERVAL -> baseDay + recurrence.intervalDays.coerceAtLeast(1).toLong()
        }

        val endLimit = recurrence.endDateEpochDay
        return if (endLimit != null && candidateDay > endLimit) null else candidateDay
    }

    fun filterAndSort(
        tasks: List<PersonalTask>,
        categoryFilter: TaskCategory? = null,
        statusFilter: PersonalTaskStatus? = null,
        searchQuery: String = "",
        sortOption: TaskSortOption = TaskSortOption.DUE_DATE,
        includeArchived: Boolean = false
    ): List<PersonalTask> {
        val normalizedQuery = searchQuery.trim().lowercase()
        val filtered = tasks.filter { task ->
            val archiveMatch = includeArchived || task.status != PersonalTaskStatus.ARCHIVED
            val catMatch = categoryFilter == null || task.category == categoryFilter
            val statusMatch = statusFilter == null || task.status == statusFilter
            val queryMatch = normalizedQuery.isBlank() ||
                task.title.lowercase().contains(normalizedQuery) ||
                task.description.lowercase().contains(normalizedQuery)
            archiveMatch && catMatch && statusMatch && queryMatch
        }

        return when (sortOption) {
            TaskSortOption.DUE_DATE -> filtered.sortedWith(
                compareBy<PersonalTask> { it.status == PersonalTaskStatus.COMPLETED || it.status == PersonalTaskStatus.ARCHIVED }
                    .thenBy { it.dueDateEpochDay ?: Long.MAX_VALUE }
                    .thenByDescending { it.priority.weight }
            )
            TaskSortOption.PRIORITY -> filtered.sortedWith(
                compareBy<PersonalTask> { it.status == PersonalTaskStatus.COMPLETED || it.status == PersonalTaskStatus.ARCHIVED }
                    .thenByDescending { it.priority.weight }
                    .thenBy { it.dueDateEpochDay ?: Long.MAX_VALUE }
            )
            TaskSortOption.CATEGORY -> filtered.sortedWith(
                compareBy<PersonalTask> { it.category.name }
                    .thenByDescending { it.priority.weight }
            )
            TaskSortOption.STATUS -> filtered.sortedWith(
                compareBy<PersonalTask> { it.status.ordinal }
                    .thenByDescending { it.priority.weight }
            )
        }
    }
}

object GoalEngine {

    /**
     * Computes goal completion strictly from completed weighted milestones or completed linked tasks.
     * Never inflates progress arbitrarily without recorded milestone or task completions.
     */
    fun computeProgressPercent(
        goal: LifeGoal,
        milestones: List<GoalMilestone>,
        linkedTasks: List<PersonalTask>
    ): Int {
        if (goal.status == GoalStatus.COMPLETED) return 100
        val goalMilestones = milestones.filter { it.goalId == goal.id }
        if (goalMilestones.isNotEmpty()) {
            val totalWeight = goalMilestones.sumOf { it.weight.coerceAtLeast(1) }
            val doneWeight = goalMilestones.filter { it.isCompleted }.sumOf { it.weight.coerceAtLeast(1) }
            return if (totalWeight == 0) 0 else ((doneWeight * 100) / totalWeight).coerceIn(0, 100)
        }

        val goalTasks = linkedTasks.filter { it.linkedGoalId == goal.id && it.status != PersonalTaskStatus.ARCHIVED }
        if (goalTasks.isNotEmpty()) {
            val done = goalTasks.count { it.status == PersonalTaskStatus.COMPLETED }
            return ((done * 100) / goalTasks.size).coerceIn(0, 100)
        }
        return 0
    }

    fun isBehindSchedule(
        goal: LifeGoal,
        computedProgressPercent: Int,
        todayEpochDay: Long
    ): Boolean {
        if (goal.status != GoalStatus.ACTIVE) return false
        if (todayEpochDay > goal.targetDateEpochDay && computedProgressPercent < 100) return true
        val totalSpan = (goal.targetDateEpochDay - goal.startDateEpochDay).coerceAtLeast(1L)
        val elapsed = (todayEpochDay - goal.startDateEpochDay).coerceIn(0L, totalSpan)
        val expectedPercent = ((elapsed * 100L) / totalSpan).toInt()
        return expectedPercent >= 25 && computedProgressPercent + 20 < expectedPercent
    }

    fun buildGoalWithDetails(
        goal: LifeGoal,
        milestones: List<GoalMilestone>,
        progressRecords: List<com.example.domain.model.GoalProgressRecord>,
        allTasks: List<PersonalTask>,
        todayEpochDay: Long
    ): GoalWithDetails {
        val goalMilestones = milestones.filter { it.goalId == goal.id }.sortedBy { it.orderIndex }
        val goalTasks = allTasks.filter { it.linkedGoalId == goal.id }
        val progress = computeProgressPercent(goal, goalMilestones, goalTasks)
        val records = progressRecords.filter { it.goalId == goal.id }.sortedByDescending { it.createdAtEpochMs }
        return GoalWithDetails(
            goal = goal,
            milestones = goalMilestones,
            progressRecords = records,
            computedProgressPercent = progress,
            completedMilestonesCount = goalMilestones.count { it.isCompleted },
            totalMilestonesCount = goalMilestones.size,
            linkedCompletedTasksCount = goalTasks.count { it.status == PersonalTaskStatus.COMPLETED },
            isBehindSchedule = isBehindSchedule(goal, progress, todayEpochDay)
        )
    }
}
