package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.LifeManagementRepository
import com.example.domain.engine.DisciplineEngine
import com.example.domain.engine.HabitEngine
import com.example.domain.engine.ProgressionEngine
import com.example.domain.engine.RoutineEngine
import com.example.domain.engine.ScheduleCalendarEngine
import com.example.domain.model.AttributeType
import com.example.domain.model.DisciplineAssessment
import com.example.domain.model.GoalStatus
import com.example.domain.model.GoalTimeframe
import com.example.domain.model.GoalWithDetails
import com.example.domain.model.Habit
import com.example.domain.model.HabitAnalyticsSummary
import com.example.domain.model.HabitCategory
import com.example.domain.model.HabitDayStatus
import com.example.domain.model.HabitType
import com.example.domain.model.LifeDashboardSummary
import com.example.domain.model.PersonalTask
import com.example.domain.model.PersonalTaskStatus
import com.example.domain.model.RoutineActivityStatus
import com.example.domain.model.RoutineDaySummary
import com.example.domain.model.RoutineTimeSlot
import com.example.domain.model.ScheduleConflict
import com.example.domain.model.ScheduledActivityItem
import com.example.domain.model.TaskCategory
import com.example.domain.model.TaskPriority
import com.example.domain.model.TaskRecurrenceType
import com.example.domain.model.TimeSlotSuggestion
import com.example.domain.model.WeeklyReviewSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

sealed interface LifeManagementUiState {
    data object Loading : LifeManagementUiState
    data class Error(val message: String) : LifeManagementUiState
    data class Ready(
        val selectedDateEpochDay: Long,
        val dashboard: LifeDashboardSummary,
        val routineSummaries: List<RoutineDaySummary>,
        val habitSummaries: List<HabitAnalyticsSummary>,
        val tasks: List<PersonalTask>,
        val goalsWithDetails: List<GoalWithDetails>,
        val selectedDaySchedule: List<ScheduledActivityItem>,
        val selectedDayConflicts: List<ScheduleConflict>,
        val weekScheduleByDay: Map<Long, List<ScheduledActivityItem>>,
        val monthScheduleCounts: Map<Long, Int>,
        val disciplineHistory: List<DisciplineAssessment>,
        val todayDiscipline: DisciplineAssessment,
        val weeklyReview: WeeklyReviewSummary?,
        val slotSuggestions: List<TimeSlotSuggestion>,
        val exemptRestDays: Set<DayOfWeek>,
        val feedbackBanner: String?
    ) : LifeManagementUiState
}

private data class LifeCoreStreams(
    val routineSummaries: List<RoutineDaySummary>,
    val habitSummaries: List<HabitAnalyticsSummary>,
    val tasks: List<PersonalTask>,
    val goals: List<GoalWithDetails>,
    val assessments: List<DisciplineAssessment>,
    val lifeXpToday: Int
)

private data class LifeDynamicInputs(
    val selectedDateEpochDay: Long,
    val weekScheduleByDay: Map<Long, List<ScheduledActivityItem>>,
    val monthCounts: Map<Long, Int>,
    val weeklyReview: WeeklyReviewSummary?,
    val suggestions: List<TimeSlotSuggestion>,
    val exemptRestDays: Set<DayOfWeek>,
    val feedbackBanner: String?
)

class LifeManagementViewModel(
    private val lifeRepository: LifeManagementRepository
) : ViewModel() {

    private val selectedDateFlow = MutableStateFlow(LocalDate.now().toEpochDay())
    private val weekScheduleFlow = MutableStateFlow<Map<Long, List<ScheduledActivityItem>>>(emptyMap())
    private val monthCountsFlow = MutableStateFlow<Map<Long, Int>>(emptyMap())
    private val weeklyReviewFlow = MutableStateFlow<WeeklyReviewSummary?>(null)
    private val slotSuggestionsFlow = MutableStateFlow<List<TimeSlotSuggestion>>(emptyList())
    private val exemptRestDaysFlow = MutableStateFlow<Set<DayOfWeek>>(emptySet())
    private val feedbackFlow = MutableStateFlow<String?>(null)

    init {
        refreshScheduleAndReview()
    }

    private val coreStreamsFlow = combine(
        combine(
            lifeRepository.observeRoutines(),
            lifeRepository.observeRoutineActivities(),
            lifeRepository.observeRoutineCompletions30d()
        ) { routines, activities, completions ->
            val today = LocalDate.now().toEpochDay()
            routines.map { r -> RoutineEngine.summarizeRoutineForDay(r, activities, completions, today) }
        },
        combine(
            lifeRepository.observeHabits(),
            lifeRepository.observeHabitCompletions180d()
        ) { habits, completions ->
            val today = LocalDate.now().toEpochDay()
            habits.filter { !it.isArchived }.map { h -> HabitEngine.buildAnalyticsSummary(h, completions, today) }
        },
        lifeRepository.observeTasks(),
        lifeRepository.observeGoalsWithDetails(),
        combine(
            lifeRepository.observeDisciplineAssessments(),
            lifeRepository.observeLifeXpToday()
        ) { assessments, xp -> assessments to xp }
    ) { routineSummaries, habitSummaries, tasks, goals, (assessments, xp) ->
        LifeCoreStreams(
            routineSummaries = routineSummaries,
            habitSummaries = habitSummaries,
            tasks = tasks,
            goals = goals,
            assessments = assessments,
            lifeXpToday = xp
        )
    }

    private val dynamicInputsFlow = combine(
        selectedDateFlow,
        weekScheduleFlow,
        monthCountsFlow,
        weeklyReviewFlow,
        combine(slotSuggestionsFlow, exemptRestDaysFlow, feedbackFlow) { suggs, restDays, fb ->
            Triple(suggs, restDays, fb)
        }
    ) { selectedDay, weekMap, monthMap, weeklyRev, (suggs, restDays, fb) ->
        LifeDynamicInputs(
            selectedDateEpochDay = selectedDay,
            weekScheduleByDay = weekMap,
            monthCounts = monthMap,
            weeklyReview = weeklyRev,
            suggestions = suggs,
            exemptRestDays = restDays,
            feedbackBanner = fb
        )
    }

    val uiState: StateFlow<LifeManagementUiState> = combine(
        coreStreamsFlow,
        dynamicInputsFlow
    ) { core, dyn ->
        val today = LocalDate.now().toEpochDay()
        val selectedDayItems = dyn.weekScheduleByDay[dyn.selectedDateEpochDay].orEmpty()
        val selectedDayConflicts = ScheduleCalendarEngine.detectConflicts(selectedDayItems)
        val todayItems = dyn.weekScheduleByDay[today].orEmpty()
        val todayConflicts = ScheduleCalendarEngine.detectConflicts(todayItems)

        val todayDiscipline = DisciplineEngine.assessDay(
            targetEpochDay = today,
            routineSummaries = core.routineSummaries,
            habitSummaries = core.habitSummaries,
            tasks = core.tasks,
            goalsWithDetails = core.goals,
            scheduledItemsToday = todayItems,
            conflictsToday = todayConflicts,
            exemptRestDays = dyn.exemptRestDays
        )

        val dashboard = LifeDashboardSummary(
            todayDisciplineScore = todayDiscipline.disciplineScore,
            overallConsistencyPercent = ((todayDiscipline.routineConsistencyPercent + todayDiscipline.habitConsistencyPercent) / 2).coerceIn(0, 100),
            activeDisciplineStreakDays = core.habitSummaries.maxOfOrNull { it.currentStreakDays } ?: 0,
            activeRoutinesCount = core.routineSummaries.count { it.routine.isEnabled },
            completedRoutinesToday = core.routineSummaries.count { it.isScheduledToday && it.completionFraction >= 1f },
            activeHabitsCount = core.habitSummaries.size,
            completedHabitsToday = core.habitSummaries.count { it.todayStatus == HabitDayStatus.COMPLETED },
            pendingTasksCount = core.tasks.count { it.status == PersonalTaskStatus.PENDING || it.status == PersonalTaskStatus.IN_PROGRESS || it.status == PersonalTaskStatus.POSTPONED },
            dueTodayTasksCount = core.tasks.count { it.isDueToday(today) },
            overdueTasksCount = core.tasks.count { it.isOverdue(today) },
            activeGoalsCount = core.goals.count { it.goal.status == GoalStatus.ACTIVE },
            todayScheduledCount = todayItems.size,
            todayConflictCount = todayConflicts.size,
            lifeXpToday = core.lifeXpToday,
            dailyLifeXpCap = ProgressionEngine.DAILY_LIFE_XP_CAP
        )

        LifeManagementUiState.Ready(
            selectedDateEpochDay = dyn.selectedDateEpochDay,
            dashboard = dashboard,
            routineSummaries = core.routineSummaries,
            habitSummaries = core.habitSummaries,
            tasks = core.tasks,
            goalsWithDetails = core.goals,
            selectedDaySchedule = selectedDayItems,
            selectedDayConflicts = selectedDayConflicts,
            weekScheduleByDay = dyn.weekScheduleByDay,
            monthScheduleCounts = dyn.monthCounts,
            disciplineHistory = core.assessments,
            todayDiscipline = todayDiscipline,
            weeklyReview = dyn.weeklyReview,
            slotSuggestions = dyn.suggestions,
            exemptRestDays = dyn.exemptRestDays,
            feedbackBanner = dyn.feedbackBanner
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = LifeManagementUiState.Loading
    )

    fun refreshScheduleAndReview() {
        viewModelScope.launch {
            val anchor = LocalDate.ofEpochDay(selectedDateFlow.value)
            val weekStart = anchor.with(DayOfWeek.MONDAY).toEpochDay()
            val weekEnd = weekStart + 6L
            val weekMap = lifeRepository.buildUnifiedItemsForRange(weekStart, weekEnd)
            weekScheduleFlow.value = weekMap

            val monthFirst = anchor.withDayOfMonth(1).toEpochDay()
            val monthLast = anchor.withDayOfMonth(anchor.lengthOfMonth()).toEpochDay()
            val monthMap = lifeRepository.buildUnifiedItemsForRange(monthFirst, monthLast)
            monthCountsFlow.value = monthMap.mapValues { it.value.size }

            weeklyReviewFlow.value = lifeRepository.buildWeeklyReviewSummary(exemptRestDaysFlow.value)
            slotSuggestionsFlow.value = lifeRepository.suggestAlternativeSlotsForDay(30, selectedDateFlow.value)
        }
    }

    fun selectDate(epochDay: Long) {
        selectedDateFlow.value = epochDay
        refreshScheduleAndReview()
    }

    fun toggleExemptRestDay(dayOfWeek: DayOfWeek) {
        val current = exemptRestDaysFlow.value.toMutableSet()
        if (dayOfWeek in current) current.remove(dayOfWeek) else current.add(dayOfWeek)
        exemptRestDaysFlow.value = current
        refreshScheduleAndReview()
    }

    fun clearFeedback() {
        feedbackFlow.value = null
    }

    // Routine Actions
    fun createRoutine(
        title: String,
        description: String,
        timeSlot: RoutineTimeSlot,
        startMinuteOfDay: Int,
        scheduledDays: Set<DayOfWeek>,
        steps: List<Pair<String, Int>>
    ) {
        viewModelScope.launch {
            lifeRepository.createRoutine(title, description, timeSlot, startMinuteOfDay, scheduledDays, steps)
            feedbackFlow.value = "Routine '$title' created (0 XP until steps are executed)."
            refreshScheduleAndReview()
        }
    }

    fun toggleRoutineEnabled(routineId: Long, enabled: Boolean) {
        viewModelScope.launch {
            lifeRepository.toggleRoutineEnabled(routineId, enabled)
            refreshScheduleAndReview()
        }
    }

    fun deleteRoutine(routineId: Long) {
        viewModelScope.launch {
            lifeRepository.deleteRoutine(routineId)
            feedbackFlow.value = "Routine deleted."
            refreshScheduleAndReview()
        }
    }

    fun addRoutineStep(routineId: Long, title: String, durationMinutes: Int) {
        viewModelScope.launch {
            lifeRepository.addRoutineActivity(routineId, title, durationMinutes)
            refreshScheduleAndReview()
        }
    }

    fun moveRoutineStep(routineId: Long, activityId: Long, moveUp: Boolean) {
        viewModelScope.launch {
            lifeRepository.moveRoutineActivity(routineId, activityId, moveUp)
        }
    }

    fun setRoutineStepStatus(routineId: Long, activityId: Long, status: RoutineActivityStatus) {
        viewModelScope.launch {
            val res = lifeRepository.setRoutineActivityStatus(routineId, activityId, status)
            feedbackFlow.value = res.message
            refreshScheduleAndReview()
        }
    }

    // Habit Actions
    fun saveHabit(
        habitId: Long?,
        title: String,
        description: String,
        habitType: HabitType,
        category: HabitCategory,
        targetValue: Double,
        unit: String,
        scheduledDays: Set<DayOfWeek>,
        preferredStartMinute: Int?,
        isRestDayProtected: Boolean
    ) {
        viewModelScope.launch {
            if (habitId == null) {
                lifeRepository.createHabit(
                    title = title,
                    description = description,
                    habitType = habitType,
                    category = category,
                    targetValue = targetValue,
                    unit = unit,
                    scheduledDays = scheduledDays,
                    preferredStartMinute = preferredStartMinute,
                    isRestDayProtected = isRestDayProtected
                )
                feedbackFlow.value = "Habit '$title' created (0 XP until completed)."
            } else {
                lifeRepository.updateHabit(
                    Habit(
                        id = habitId,
                        title = title,
                        description = description,
                        habitType = habitType,
                        category = category,
                        targetValue = targetValue,
                        unit = unit,
                        scheduledDays = scheduledDays,
                        preferredStartMinute = preferredStartMinute,
                        isRestDayProtected = isRestDayProtected
                    )
                )
                feedbackFlow.value = "Habit '$title' updated."
            }
            refreshScheduleAndReview()
        }
    }

    fun logHabit(
        habitId: Long,
        achievedValue: Double? = null,
        explicitStatus: HabitDayStatus? = null,
        skipReason: String = ""
    ) {
        viewModelScope.launch {
            val res = lifeRepository.logHabitProgress(
                habitId = habitId,
                achievedValue = achievedValue,
                explicitStatus = explicitStatus,
                skipReason = skipReason
            )
            feedbackFlow.value = res.message
            refreshScheduleAndReview()
        }
    }

    fun archiveHabit(habitId: Long) {
        viewModelScope.launch {
            lifeRepository.archiveHabit(habitId, true)
            feedbackFlow.value = "Habit archived."
            refreshScheduleAndReview()
        }
    }

    // Personal Task Actions
    fun createTask(
        title: String,
        description: String,
        category: TaskCategory,
        priority: TaskPriority,
        dueDateEpochDay: Long?,
        scheduledStartMinute: Int?,
        estimatedMinutes: Int,
        linkedGoalId: Long?,
        isTimeSensitive: Boolean,
        recurrenceType: TaskRecurrenceType,
        intervalDays: Int = 1
    ) {
        viewModelScope.launch {
            lifeRepository.createTask(
                title = title,
                description = description,
                category = category,
                priority = priority,
                dueDateEpochDay = dueDateEpochDay,
                scheduledStartMinute = scheduledStartMinute,
                estimatedMinutes = estimatedMinutes,
                linkedGoalId = linkedGoalId,
                isTimeSensitive = isTimeSensitive,
                recurrenceType = recurrenceType,
                intervalDays = intervalDays
            )
            feedbackFlow.value = "Task '$title' added (0 XP until completed)."
            refreshScheduleAndReview()
        }
    }

    fun completeTask(taskId: Long) {
        viewModelScope.launch {
            val res = lifeRepository.completeTask(taskId)
            feedbackFlow.value = res.message
            refreshScheduleAndReview()
        }
    }

    fun postponeTask(taskId: Long, days: Long = 1L) {
        viewModelScope.launch {
            lifeRepository.postponeTask(taskId, days)
            feedbackFlow.value = "Task postponed by ${days}d."
            refreshScheduleAndReview()
        }
    }

    fun rescheduleTask(taskId: Long, newDueEpochDay: Long, newStartMinute: Int?) {
        viewModelScope.launch {
            lifeRepository.rescheduleTask(taskId, newDueEpochDay, newStartMinute)
            feedbackFlow.value = "Task rescheduled."
            refreshScheduleAndReview()
        }
    }

    fun archiveTask(taskId: Long) {
        viewModelScope.launch {
            lifeRepository.archiveTask(taskId)
            feedbackFlow.value = "Task archived."
            refreshScheduleAndReview()
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            lifeRepository.deleteTask(taskId)
            feedbackFlow.value = "Task deleted."
            refreshScheduleAndReview()
        }
    }

    // Goal Actions
    fun createGoal(
        title: String,
        description: String,
        timeframe: GoalTimeframe,
        targetAttribute: AttributeType,
        completionCriteria: String,
        targetDateEpochDay: Long,
        initialMilestones: List<String>
    ) {
        viewModelScope.launch {
            val today = LocalDate.now().toEpochDay()
            lifeRepository.createGoal(
                title = title,
                description = description,
                timeframe = timeframe,
                targetAttribute = targetAttribute,
                completionCriteria = completionCriteria,
                startDateEpochDay = today,
                targetDateEpochDay = targetDateEpochDay,
                initialMilestones = initialMilestones
            )
            feedbackFlow.value = "Goal '$title' created (progress starts at 0%)."
            refreshScheduleAndReview()
        }
    }

    fun addMilestone(goalId: Long, title: String, weight: Int, dueDateEpochDay: Long?) {
        viewModelScope.launch {
            lifeRepository.addGoalMilestone(goalId, title, weight, dueDateEpochDay)
            refreshScheduleAndReview()
        }
    }

    fun completeMilestone(milestoneId: Long) {
        viewModelScope.launch {
            val res = lifeRepository.completeGoalMilestone(milestoneId)
            feedbackFlow.value = res.message
            refreshScheduleAndReview()
        }
    }

    fun submitGoalReview(goalId: Long, note: String, adjustment: String) {
        viewModelScope.launch {
            val res = lifeRepository.recordGoalReview(goalId, note, adjustment)
            feedbackFlow.value = res.message
            refreshScheduleAndReview()
        }
    }

    fun deleteGoal(goalId: Long) {
        viewModelScope.launch {
            lifeRepository.deleteGoal(goalId)
            feedbackFlow.value = "Goal deleted."
            refreshScheduleAndReview()
        }
    }

    // Unified Schedule & Reflection
    fun addScheduledEvent(title: String, subtitle: String, startMinute: Int, durationMinutes: Int) {
        viewModelScope.launch {
            val (_, conflicts) = lifeRepository.createCustomScheduledEvent(
                title = title,
                subtitle = subtitle,
                dateEpochDay = selectedDateFlow.value,
                startMinuteOfDay = startMinute,
                durationMinutes = durationMinutes
            )
            feedbackFlow.value = if (conflicts.isEmpty()) {
                "Scheduled '$title' with zero conflicts."
            } else {
                "Scheduled '$title' — warning: ${conflicts.size} overlap conflict(s) detected."
            }
            refreshScheduleAndReview()
        }
    }

    fun rescheduleItem(item: ScheduledActivityItem, newEpochDay: Long, newStartMinute: Int) {
        viewModelScope.launch {
            val res = lifeRepository.rescheduleItem(item, newEpochDay, newStartMinute)
            feedbackFlow.value = res.message
            refreshScheduleAndReview()
        }
    }

    fun loadSuggestions(durationMinutes: Int) {
        viewModelScope.launch {
            slotSuggestionsFlow.value = lifeRepository.suggestAlternativeSlotsForDay(durationMinutes, selectedDateFlow.value)
        }
    }

    fun saveDisciplineReview(note: String) {
        viewModelScope.launch {
            val res = lifeRepository.saveReflectionReview(note)
            feedbackFlow.value = res.message
            refreshScheduleAndReview()
        }
    }

    companion object {
        fun factory(lifeRepository: LifeManagementRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return LifeManagementViewModel(lifeRepository) as T
                }
            }
    }
}
