package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.entity.GoalMilestoneEntity
import com.example.data.local.entity.GoalProgressRecordEntity
import com.example.data.local.entity.HabitCompletionEntity
import com.example.data.local.entity.HabitEntity
import com.example.data.local.entity.LifeGoalEntity
import com.example.data.local.entity.LifeProgressionRecordEntity
import com.example.data.local.entity.LifeRoutineEntity
import com.example.data.local.entity.PersonalTaskEntity
import com.example.data.local.entity.RoutineActivityEntity
import com.example.data.local.entity.RoutineCompletionEntity
import com.example.data.local.entity.ScheduledActivityReferenceEntity
import com.example.data.local.entity.TaskRecurrenceEntity
import com.example.data.local.entity.toDomain
import com.example.data.local.entity.toEntity
import com.example.data.preferences.UserPreferencesRepository
import com.example.domain.engine.DisciplineEngine
import com.example.domain.engine.GoalEngine
import com.example.domain.engine.HabitEngine
import com.example.domain.engine.PersonalTaskEngine
import com.example.domain.engine.ProgressionEngine
import com.example.domain.engine.RevisionEngine
import com.example.domain.engine.RoutineEngine
import com.example.domain.engine.ScheduleCalendarEngine
import com.example.domain.model.ActivityLog
import com.example.domain.model.AttributeType
import com.example.domain.model.AvailabilityWindow
import com.example.domain.model.DisciplineAssessment
import com.example.domain.model.GoalMilestone
import com.example.domain.model.GoalStatus
import com.example.domain.model.GoalTimeframe
import com.example.domain.model.GoalWithDetails
import com.example.domain.model.Habit
import com.example.domain.model.HabitAnalyticsSummary
import com.example.domain.model.HabitCategory
import com.example.domain.model.HabitCompletionRecord
import com.example.domain.model.HabitDayStatus
import com.example.domain.model.HabitType
import com.example.domain.model.LifeActivityKind
import com.example.domain.model.LifeDashboardSummary
import com.example.domain.model.LifeGoal
import com.example.domain.model.LifeRoutine
import com.example.domain.model.NotificationCategory
import com.example.domain.model.PersonalTask
import com.example.domain.model.PersonalTaskStatus
import com.example.domain.model.PlayerProfile
import com.example.domain.model.QuestCategory
import com.example.domain.model.RoutineActivity
import com.example.domain.model.RoutineActivityStatus
import com.example.domain.model.RoutineCompletionRecord
import com.example.domain.model.RoutineDaySummary
import com.example.domain.model.RoutineTimeSlot
import com.example.domain.model.ScheduleConflict
import com.example.domain.model.ScheduledActivityItem
import com.example.domain.model.ScheduledActivityReference
import com.example.domain.model.ScheduledSourceType
import com.example.domain.model.SystemNotification
import com.example.domain.model.TaskCategory
import com.example.domain.model.TaskPriority
import com.example.domain.model.TaskRecurrenceConfig
import com.example.domain.model.TaskRecurrenceType
import com.example.domain.model.TimeSlotSuggestion
import com.example.domain.model.VerificationStatus
import com.example.domain.model.WeeklyReviewSummary
import com.example.domain.model.toCsvString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class LifeMutationFeedback(
    val message: String,
    val xpAwarded: Int = 0,
    val wasCapped: Boolean = false
)

class LifeManagementRepository(
    private val database: SoloLevelingDatabase,
    private val preferencesRepository: UserPreferencesRepository
) {
    private val routineDao = database.routineDao()
    private val habitDao = database.habitDao()
    private val taskDao = database.personalTaskDao()
    private val goalDao = database.goalDao()
    private val scheduleDao = database.scheduleDao()
    private val disciplineDao = database.disciplineDao()
    private val progressionDao = database.lifeProgressionDao()
    private val playerDao = database.playerDao()
    private val activityLogDao = database.activityLogDao()
    private val notificationDao = database.systemNotificationDao()

    private fun todayStartEpochMs(zoneId: ZoneId = ZoneId.systemDefault()): Long =
        LocalDate.now(zoneId).atStartOfDay(zoneId).toInstant().toEpochMilli()

    fun observeRoutines(): Flow<List<LifeRoutine>> =
        routineDao.observeAllRoutines().map { list -> list.map { it.toDomain() } }

    fun observeRoutineActivities(): Flow<List<RoutineActivity>> =
        routineDao.observeAllActivities().map { list -> list.map { it.toDomain() } }

    fun observeRoutineCompletions30d(): Flow<List<RoutineCompletionRecord>> {
        val sinceDay = LocalDate.now().toEpochDay() - 35L
        return routineDao.observeCompletionsSince(sinceDay).map { list -> list.map { it.toDomain() } }
    }

    fun observeHabits(): Flow<List<Habit>> =
        habitDao.observeAllHabits().map { list -> list.map { it.toDomain() } }

    fun observeHabitCompletions180d(): Flow<List<HabitCompletionRecord>> {
        val sinceDay = LocalDate.now().toEpochDay() - 180L
        return habitDao.observeCompletionsSince(sinceDay).map { list -> list.map { it.toDomain() } }
    }

    fun observeTasks(): Flow<List<PersonalTask>> = combine(
        taskDao.observeAllTasks(),
        taskDao.observeAllRecurrences()
    ) { taskEntities, recurrenceEntities ->
        val recByTaskId = recurrenceEntities.associate { it.taskId to it.toDomain() }
        taskEntities.map { it.toDomain(recByTaskId[it.id]) }
    }

    fun observeGoalsWithDetails(): Flow<List<GoalWithDetails>> = combine(
        goalDao.observeAllGoals(),
        goalDao.observeAllMilestones(),
        goalDao.observeAllProgressRecords(),
        observeTasks()
    ) { goalEntities, milestoneEntities, recordEntities, tasks ->
        val today = LocalDate.now().toEpochDay()
        val milestones = milestoneEntities.map { it.toDomain() }
        val records = recordEntities.map { it.toDomain() }
        goalEntities.map { entity ->
            GoalEngine.buildGoalWithDetails(
                goal = entity.toDomain(),
                milestones = milestones,
                progressRecords = records,
                allTasks = tasks,
                todayEpochDay = today
            )
        }
    }

    fun observeScheduleReferences(): Flow<List<ScheduledActivityReference>> =
        scheduleDao.observeAllReferences().map { list -> list.map { it.toDomain() } }

    fun observeDisciplineAssessments(): Flow<List<DisciplineAssessment>> =
        disciplineDao.observeRecentAssessments(30).map { list -> list.map { it.toDomain() } }

    fun observeLifeXpToday(): Flow<Int> =
        progressionDao.observeLifeXpSince(todayStartEpochMs())

    // --- Routines ---

    suspend fun createRoutine(
        title: String,
        description: String,
        timeSlot: RoutineTimeSlot,
        startMinuteOfDay: Int,
        scheduledDays: Set<DayOfWeek>,
        initialActivities: List<Pair<String, Int>>,
        reminderEnabled: Boolean = true,
        isRestExemptAllowed: Boolean = true
    ): Long {
        val cleanTitle = title.trim().ifBlank { "Daily Routine" }
        val activities = initialActivities.filter { it.first.isNotBlank() }
        val totalMinutes = activities.sumOf { it.second.coerceAtLeast(1) }.coerceAtLeast(10)
        val routineId = routineDao.insertRoutine(
            LifeRoutine(
                title = cleanTitle,
                description = description.trim(),
                timeSlot = timeSlot,
                startMinuteOfDay = startMinuteOfDay.coerceIn(0, 23 * 60 + 50),
                scheduledDays = scheduledDays.ifEmpty { DayOfWeek.entries.toSet() },
                isEnabled = true,
                targetDurationMinutes = totalMinutes,
                reminderEnabled = reminderEnabled,
                isRestExemptAllowed = isRestExemptAllowed
            ).toEntity()
        )
        if (activities.isNotEmpty()) {
            routineDao.insertActivities(
                activities.mapIndexed { index, (actTitle, mins) ->
                    RoutineActivity(
                        routineId = routineId,
                        title = actTitle.trim(),
                        durationMinutes = mins.coerceIn(1, 180),
                        orderIndex = index
                    ).toEntity()
                }
            )
        }
        return routineId
    }

    suspend fun updateRoutine(routine: LifeRoutine) {
        routineDao.updateRoutine(routine.copy(title = routine.title.trim().ifBlank { routine.title }).toEntity())
    }

    suspend fun toggleRoutineEnabled(routineId: Long, enabled: Boolean) {
        val current = routineDao.getRoutineById(routineId)?.toDomain() ?: return
        routineDao.updateRoutine(current.copy(isEnabled = enabled).toEntity())
    }

    suspend fun deleteRoutine(routineId: Long) {
        routineDao.deleteRoutine(routineId)
        scheduleDao.deleteBySource(ScheduledSourceType.ROUTINE.name, routineId)
    }

    suspend fun addRoutineActivity(
        routineId: Long,
        title: String,
        durationMinutes: Int,
        isOptional: Boolean = false
    ) {
        if (title.isBlank()) return
        val existing = routineDao.getActivitiesForRoutine(routineId)
        val nextOrder = (existing.maxOfOrNull { it.orderIndex } ?: -1) + 1
        routineDao.insertActivity(
            RoutineActivity(
                routineId = routineId,
                title = title.trim(),
                durationMinutes = durationMinutes.coerceIn(1, 180),
                orderIndex = nextOrder,
                isOptional = isOptional
            ).toEntity()
        )
        recomputeRoutineDuration(routineId)
    }

    suspend fun deleteRoutineActivity(routineId: Long, activityId: Long) {
        routineDao.deleteActivity(activityId)
        recomputeRoutineDuration(routineId)
    }

    suspend fun moveRoutineActivity(routineId: Long, activityId: Long, moveUp: Boolean) {
        val activities = routineDao.getActivitiesForRoutine(routineId).map { it.toDomain() }.toMutableList()
        val idx = activities.indexOfFirst { it.id == activityId }
        if (idx == -1) return
        val targetIdx = if (moveUp) idx - 1 else idx + 1
        if (targetIdx !in activities.indices) return
        val item = activities.removeAt(idx)
        activities.add(targetIdx, item)
        val reindexed = activities.mapIndexed { index, act -> act.copy(orderIndex = index).toEntity() }
        routineDao.updateActivities(reindexed)
    }

    private suspend fun recomputeRoutineDuration(routineId: Long) {
        val routine = routineDao.getRoutineById(routineId)?.toDomain() ?: return
        val total = routineDao.getActivitiesForRoutine(routineId).sumOf { it.durationMinutes }.coerceAtLeast(10)
        routineDao.updateRoutine(routine.copy(targetDurationMinutes = total).toEntity())
    }

    suspend fun setRoutineActivityStatus(
        routineId: Long,
        activityId: Long,
        status: RoutineActivityStatus,
        epochDay: Long = LocalDate.now().toEpochDay(),
        note: String = ""
    ): LifeMutationFeedback {
        if (status == RoutineActivityStatus.PENDING) {
            routineDao.deleteCompletion(routineId, activityId, epochDay)
            return LifeMutationFeedback("Routine step reset to pending.")
        }
        routineDao.upsertCompletion(
            RoutineCompletionRecord(
                routineId = routineId,
                activityId = activityId,
                dateEpochDay = epochDay,
                status = status,
                note = note.trim()
            ).toEntity()
        )

        val routine = routineDao.getRoutineById(routineId)?.toDomain()
            ?: return LifeMutationFeedback("Routine updated.")
        val activities = routineDao.getActivitiesForRoutine(routineId).map { it.toDomain() }
        val required = activities.filter { !it.isOptional }
        val completionsToday = routineDao.getCompletionsSinceOnce(epochDay)
            .filter { it.routineId == routineId && it.dateEpochDay == epochDay }
        val doneSet = completionsToday
            .filter { it.statusName == RoutineActivityStatus.COMPLETED.name || it.statusName == RoutineActivityStatus.SKIPPED.name }
            .map { it.activityId }
            .toSet()

        val allRequiredDone = if (required.isEmpty()) {
            completionsToday.any { it.statusName == RoutineActivityStatus.COMPLETED.name }
        } else {
            required.all { it.id in doneSet }
        }

        if (status == RoutineActivityStatus.COMPLETED && allRequiredDone) {
            val summary = RoutineEngine.summarizeRoutineForDay(
                routine = routine,
                activities = activities,
                completions = routineDao.getCompletionsSinceOnce(epochDay - 35L).map { it.toDomain() },
                targetEpochDay = epochDay
            )
            return awardLifeProgression(
                rewardKey = "routine_${routineId}_$epochDay",
                kind = LifeActivityKind.ROUTINE_COMPLETION,
                title = "Routine Completed: ${routine.title}",
                verificationStatus = VerificationStatus.SELF_REPORTED,
                streakDays = summary.currentStreakDays,
                notes = "${required.size.coerceAtLeast(1)} step(s) executed"
            )
        }

        return LifeMutationFeedback("Routine step updated (${status.label}).")
    }

    // --- Habits ---

    suspend fun createHabit(
        title: String,
        description: String,
        habitType: HabitType,
        category: HabitCategory,
        targetValue: Double,
        unit: String,
        scheduledDays: Set<DayOfWeek>,
        preferredStartMinute: Int? = null,
        reminderEnabled: Boolean = true,
        isRestDayProtected: Boolean = true
    ): Long {
        return habitDao.insertHabit(
            Habit(
                title = title.trim().ifBlank { "Daily Habit" },
                description = description.trim(),
                habitType = habitType,
                category = category,
                targetValue = targetValue.coerceAtLeast(1.0),
                unit = unit.trim().ifBlank { if (habitType == HabitType.DURATION) "min" else "count" },
                scheduledDays = scheduledDays.ifEmpty { DayOfWeek.entries.toSet() },
                preferredStartMinute = preferredStartMinute,
                reminderEnabled = reminderEnabled,
                isRestDayProtected = isRestDayProtected
            ).toEntity()
        )
    }

    suspend fun updateHabit(habit: Habit) {
        habitDao.updateHabit(habit.copy(title = habit.title.trim().ifBlank { habit.title }).toEntity())
    }

    suspend fun archiveHabit(habitId: Long, archived: Boolean) {
        val current = habitDao.getHabitById(habitId)?.toDomain() ?: return
        habitDao.updateHabit(current.copy(isArchived = archived).toEntity())
    }

    suspend fun deleteHabit(habitId: Long) {
        habitDao.deleteHabit(habitId)
        scheduleDao.deleteBySource(ScheduledSourceType.HABIT.name, habitId)
    }

    suspend fun logHabitProgress(
        habitId: Long,
        epochDay: Long = LocalDate.now().toEpochDay(),
        achievedValue: Double? = null,
        explicitStatus: HabitDayStatus? = null,
        skipReason: String = "",
        note: String = ""
    ): LifeMutationFeedback {
        val habit = habitDao.getHabitById(habitId)?.toDomain()
            ?: return LifeMutationFeedback("Habit not found.")
        val finalValue = achievedValue ?: when (explicitStatus) {
            HabitDayStatus.COMPLETED -> habit.targetValue
            HabitDayStatus.PARTIAL -> (habit.targetValue / 2.0).coerceAtLeast(0.5)
            else -> 0.0
        }
        val finalStatus = explicitStatus ?: HabitEngine.evaluateLoggedStatus(habit, finalValue)

        habitDao.upsertCompletion(
            HabitCompletionRecord(
                habitId = habitId,
                dateEpochDay = epochDay,
                status = finalStatus,
                achievedValue = finalValue,
                skipReason = skipReason.trim(),
                note = note.trim()
            ).toEntity()
        )

        if (finalStatus == HabitDayStatus.COMPLETED) {
            val allRecords = habitDao.getCompletionsSinceOnce(epochDay - 180L).map { it.toDomain() }
            val summary = HabitEngine.buildAnalyticsSummary(habit, allRecords, epochDay)
            return awardLifeProgression(
                rewardKey = "habit_${habitId}_$epochDay",
                kind = LifeActivityKind.HABIT_COMPLETION,
                title = "Habit Executed: ${habit.title}",
                verificationStatus = VerificationStatus.SELF_REPORTED,
                habitCategory = habit.category,
                streakDays = summary.currentStreakDays,
                notes = "$finalValue ${habit.unit}"
            )
        }

        return LifeMutationFeedback("Habit logged as ${finalStatus.label} (0 XP).")
    }

    // --- Personal Tasks ---

    suspend fun createTask(
        title: String,
        description: String,
        category: TaskCategory,
        priority: TaskPriority,
        dueDateEpochDay: Long?,
        scheduledStartMinute: Int?,
        estimatedMinutes: Int,
        linkedGoalId: Long?,
        isTimeSensitive: Boolean,
        recurrenceType: TaskRecurrenceType = TaskRecurrenceType.NONE,
        intervalDays: Int = 1,
        selectedDays: Set<DayOfWeek> = emptySet(),
        endDateEpochDay: Long? = null
    ): Long {
        val now = System.currentTimeMillis()
        val taskId = taskDao.insertTask(
            PersonalTask(
                title = title.trim().ifBlank { "Personal Task" },
                description = description.trim(),
                category = category,
                priority = priority,
                status = PersonalTaskStatus.PENDING,
                dueDateEpochDay = dueDateEpochDay,
                scheduledStartMinute = scheduledStartMinute,
                estimatedMinutes = estimatedMinutes.coerceIn(5, 480),
                linkedGoalId = linkedGoalId,
                isTimeSensitive = isTimeSensitive,
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            ).toEntity()
        )
        if (recurrenceType != TaskRecurrenceType.NONE) {
            taskDao.upsertRecurrence(
                TaskRecurrenceConfig(
                    taskId = taskId,
                    recurrenceType = recurrenceType,
                    intervalDays = intervalDays.coerceAtLeast(1),
                    selectedDays = selectedDays,
                    endDateEpochDay = endDateEpochDay
                ).toEntity()
            )
        }
        return taskId
    }

    suspend fun updateTask(task: PersonalTask) {
        val now = System.currentTimeMillis()
        taskDao.updateTask(
            task.copy(
                title = task.title.trim().ifBlank { task.title },
                updatedAtEpochMs = now
            ).toEntity()
        )
        val rec = task.recurrence
        if (rec == null || rec.recurrenceType == TaskRecurrenceType.NONE) {
            taskDao.deleteRecurrenceForTask(task.id)
        } else {
            taskDao.upsertRecurrence(rec.copy(taskId = task.id).toEntity())
        }
    }

    suspend fun completeTask(taskId: Long): LifeMutationFeedback {
        val entity = taskDao.getTaskById(taskId) ?: return LifeMutationFeedback("Task not found.")
        val rec = taskDao.getRecurrenceForTask(taskId)?.toDomain()
        val task = entity.toDomain(rec)
        if (task.status == PersonalTaskStatus.COMPLETED) {
            return LifeMutationFeedback("Task already completed.")
        }

        val now = System.currentTimeMillis()
        val today = LocalDate.now().toEpochDay()
        taskDao.updateTask(
            task.copy(
                status = PersonalTaskStatus.COMPLETED,
                completedAtEpochMs = now,
                updatedAtEpochMs = now
            ).toEntity()
        )

        // Generate next recurring task occurrence if configured (creation gives 0 XP)
        val nextDue = PersonalTaskEngine.computeNextDueDate(task, today)
        if (nextDue != null && rec != null) {
            val nextId = taskDao.insertTask(
                task.copy(
                    id = 0,
                    status = PersonalTaskStatus.PENDING,
                    dueDateEpochDay = nextDue,
                    postponeCount = 0,
                    completedAtEpochMs = null,
                    createdAtEpochMs = now,
                    updatedAtEpochMs = now
                ).toEntity()
            )
            taskDao.upsertRecurrence(rec.copy(id = 0, taskId = nextId).toEntity())
        }

        val kind = if (task.category == TaskCategory.SOCIAL) LifeActivityKind.SOCIAL_COMMITMENT else LifeActivityKind.PERSONAL_TASK
        return awardLifeProgression(
            rewardKey = "task_${task.id}",
            kind = kind,
            title = "Task Completed: ${task.title}",
            verificationStatus = VerificationStatus.SELF_REPORTED,
            taskCategory = task.category,
            notes = "${task.category.label} • ${task.priority.label}"
        )
    }

    suspend fun postponeTask(taskId: Long, days: Long = 1L) {
        val entity = taskDao.getTaskById(taskId) ?: return
        val rec = taskDao.getRecurrenceForTask(taskId)?.toDomain()
        val task = entity.toDomain(rec)
        val base = maxOf(task.dueDateEpochDay ?: LocalDate.now().toEpochDay(), LocalDate.now().toEpochDay())
        taskDao.updateTask(
            task.copy(
                dueDateEpochDay = base + days.coerceAtLeast(1L),
                status = PersonalTaskStatus.POSTPONED,
                postponeCount = task.postponeCount + 1,
                updatedAtEpochMs = System.currentTimeMillis()
            ).toEntity()
        )
    }

    suspend fun rescheduleTask(taskId: Long, newDueEpochDay: Long, newStartMinute: Int?) {
        val entity = taskDao.getTaskById(taskId) ?: return
        val task = entity.toDomain()
        taskDao.updateTask(
            task.copy(
                dueDateEpochDay = newDueEpochDay,
                scheduledStartMinute = newStartMinute,
                status = if (task.status == PersonalTaskStatus.POSTPONED) PersonalTaskStatus.PENDING else task.status,
                updatedAtEpochMs = System.currentTimeMillis()
            ).toEntity()
        )
    }

    suspend fun archiveTask(taskId: Long) {
        val entity = taskDao.getTaskById(taskId) ?: return
        taskDao.updateTask(entity.copy(statusName = PersonalTaskStatus.ARCHIVED.name, updatedAtEpochMs = System.currentTimeMillis()))
    }

    suspend fun deleteTask(taskId: Long) {
        taskDao.deleteTask(taskId)
        scheduleDao.deleteBySource(ScheduledSourceType.PERSONAL_TASK.name, taskId)
    }

    // --- Goals & Milestones ---

    suspend fun createGoal(
        title: String,
        description: String,
        timeframe: GoalTimeframe,
        targetAttribute: AttributeType,
        completionCriteria: String,
        startDateEpochDay: Long,
        targetDateEpochDay: Long,
        initialMilestones: List<String> = emptyList()
    ): Long {
        val now = System.currentTimeMillis()
        val goalId = goalDao.insertGoal(
            LifeGoal(
                title = title.trim().ifBlank { "Personal Goal" },
                description = description.trim(),
                timeframe = timeframe,
                status = GoalStatus.ACTIVE,
                targetAttribute = targetAttribute,
                completionCriteria = completionCriteria.trim(),
                startDateEpochDay = startDateEpochDay,
                targetDateEpochDay = maxOf(startDateEpochDay, targetDateEpochDay),
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            ).toEntity()
        )
        initialMilestones.filter { it.isNotBlank() }.forEachIndexed { index, mTitle ->
            goalDao.insertMilestone(
                GoalMilestone(
                    goalId = goalId,
                    title = mTitle.trim(),
                    weight = 1,
                    orderIndex = index
                ).toEntity()
            )
        }
        return goalId
    }

    suspend fun addGoalMilestone(
        goalId: Long,
        title: String,
        weight: Int = 1,
        dueDateEpochDay: Long? = null
    ) {
        if (title.isBlank()) return
        val existing = goalDao.getMilestonesForGoal(goalId)
        val nextIndex = (existing.maxOfOrNull { it.orderIndex } ?: -1) + 1
        goalDao.insertMilestone(
            GoalMilestone(
                goalId = goalId,
                title = title.trim(),
                weight = weight.coerceIn(1, 10),
                dueDateEpochDay = dueDateEpochDay,
                orderIndex = nextIndex
            ).toEntity()
        )
    }

    suspend fun completeGoalMilestone(milestoneId: Long): LifeMutationFeedback {
        val entity = goalDao.getMilestoneById(milestoneId) ?: return LifeMutationFeedback("Milestone not found.")
        if (entity.isCompleted) return LifeMutationFeedback("Milestone already completed.")
        val now = System.currentTimeMillis()
        goalDao.updateMilestone(entity.copy(isCompleted = true, completedAtEpochMs = now))

        val goal = goalDao.getGoalById(entity.goalId)?.toDomain()
            ?: return LifeMutationFeedback("Milestone completed.")
        val milestones = goalDao.getMilestonesForGoal(goal.id).map { it.toDomain() }
        val linkedTasks = taskDao.getAllTasksOnce().filter { it.linkedGoalId == goal.id }.map { it.toDomain() }
        val newProgress = GoalEngine.computeProgressPercent(goal, milestones, linkedTasks)

        goalDao.insertProgressRecord(
            GoalProgressRecordEntity(
                goalId = goal.id,
                note = "Completed milestone: ${entity.title}",
                recordedProgressPercent = newProgress,
                adjustmentSummary = "",
                createdAtEpochMs = now
            )
        )
        if (milestones.isNotEmpty() && milestones.all { it.isCompleted }) {
            goalDao.updateGoal(goal.copy(status = GoalStatus.COMPLETED, updatedAtEpochMs = now).toEntity())
        }

        return awardLifeProgression(
            rewardKey = "goal_milestone_${entity.id}",
            kind = LifeActivityKind.GOAL_MILESTONE,
            title = "Goal Milestone: ${entity.title}",
            verificationStatus = VerificationStatus.SELF_REPORTED,
            goalAttribute = goal.targetAttribute,
            notes = "${goal.title} ($newProgress%)"
        )
    }

    suspend fun recordGoalReview(
        goalId: Long,
        reviewNote: String,
        adjustmentSummary: String,
        newTargetDateEpochDay: Long? = null
    ): LifeMutationFeedback {
        val goal = goalDao.getGoalById(goalId)?.toDomain() ?: return LifeMutationFeedback("Goal not found.")
        val now = System.currentTimeMillis()
        val milestones = goalDao.getMilestonesForGoal(goalId).map { it.toDomain() }
        val tasks = taskDao.getAllTasksOnce().filter { it.linkedGoalId == goalId }.map { it.toDomain() }
        val computed = GoalEngine.computeProgressPercent(goal, milestones, tasks)

        goalDao.insertProgressRecord(
            GoalProgressRecordEntity(
                goalId = goalId,
                note = reviewNote.trim().ifBlank { "Structured milestone & timeline review" },
                recordedProgressPercent = computed,
                adjustmentSummary = adjustmentSummary.trim(),
                createdAtEpochMs = now
            )
        )
        if (newTargetDateEpochDay != null && newTargetDateEpochDay >= goal.startDateEpochDay) {
            goalDao.updateGoal(goal.copy(targetDateEpochDay = newTargetDateEpochDay, updatedAtEpochMs = now).toEntity())
        }

        val today = LocalDate.now().toEpochDay()
        return awardLifeProgression(
            rewardKey = "goal_review_${goalId}_$today",
            kind = LifeActivityKind.GOAL_REVIEW,
            title = "Goal Review: ${goal.title}",
            verificationStatus = VerificationStatus.SELF_REPORTED,
            goalAttribute = goal.targetAttribute,
            notes = adjustmentSummary.ifBlank { "Reviewed progress at $computed%" }
        )
    }

    suspend fun deleteGoal(goalId: Long) {
        goalDao.deleteGoal(goalId)
    }

    // --- Unified Calendar & Conflict Resolution ---

    suspend fun buildUnifiedItemsForRange(
        startEpochDay: Long,
        endEpochDay: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Map<Long, List<ScheduledActivityItem>> {
        val routines = routineDao.getAllRoutinesOnce().map { it.toDomain() }
        val routineActs = routineDao.observeAllActivities().first().map { it.toDomain() }
        val routineComp = routineDao.getCompletionsSinceOnce(startEpochDay - 7L).map { it.toDomain() }
        val habits = habitDao.getAllHabitsOnce().map { it.toDomain() }
        val habitComp = habitDao.getCompletionsSinceOnce(startEpochDay - 7L).map { it.toDomain() }
        val tasks = taskDao.getAllTasksOnce().map { it.toDomain() }
        val goals = goalDao.getAllGoalsOnce().map { it.toDomain() }
        val milestones = goalDao.observeAllMilestones().first().map { it.toDomain() }
        val refs = scheduleDao.getReferencesInRangeOnce(startEpochDay, endEpochDay).map { it.toDomain() }
        val studySessions = database.studySessionDao().observeAllSessions().first().map { it.toDomain() }
        val exams = database.academicAssessmentDao().getAllExamsOnce().map { it.toDomain() }
        val subjects = database.academicSubjectDao().observeAllSubjects().first().map { it.toDomain() }
        val chapters = database.academicSubjectDao().observeAllChapters().first().map { it.toDomain() }
        val topics = database.academicSubjectDao().getAllTopicsOnce().map { it.toDomain() }
        val assessments = database.academicAssessmentDao().observeAllAssessments().first().map { it.toDomain() }
        val latestRatioBySubject = assessments.groupBy { it.subjectId }.mapValues { it.value.first().normalizedRatio }
        val revisionQueue = RevisionEngine.buildQueue(
            topics = topics,
            subjectNamesById = subjects.associate { it.id to it.name },
            difficultyByChapterId = chapters.associate { it.id to it.difficulty },
            assessmentRatioBySubjectId = latestRatioBySubject
        )
        val jogs = database.joggingSessionDao().getSessionsSince(0L).map { it.toDomain() }
        val workouts = database.workoutSessionDao().getWorkoutsSince(0L).map { it.toDomain() }
        val aiPlans = database.aiProposalDao().observeDailyPlanProposals().first().map { it.toDomain() }
        val today = LocalDate.now(zoneId).toEpochDay()

        return (startEpochDay..endEpochDay).associateWith { day ->
            ScheduleCalendarEngine.buildUnifiedItemsForDay(
                targetEpochDay = day,
                routines = routines,
                routineActivities = routineActs,
                routineCompletions = routineComp,
                habits = habits,
                habitCompletions = habitComp,
                tasks = tasks,
                goals = goals,
                goalMilestones = milestones,
                storedReferences = refs,
                studySessions = studySessions,
                exams = exams,
                revisionQueue = revisionQueue,
                jogSessions = jogs,
                workoutSessions = workouts,
                aiPlanItems = aiPlans,
                todayEpochDay = today,
                zoneId = zoneId
            )
        }
    }

    suspend fun createCustomScheduledEvent(
        title: String,
        subtitle: String,
        dateEpochDay: Long,
        startMinuteOfDay: Int,
        durationMinutes: Int,
        isAllDay: Boolean = false,
        timezoneId: String = ZoneId.systemDefault().id
    ): Pair<Long, List<ScheduleConflict>> {
        val cleanTitle = title.trim().ifBlank { "Scheduled Block" }
        val safeDuration = durationMinutes.coerceIn(10, 480)
        val safeStart = startMinuteOfDay.coerceIn(0, 23 * 60 + 45)
        val id = scheduleDao.upsertReference(
            ScheduledActivityReference(
                sourceType = ScheduledSourceType.CUSTOM_EVENT,
                sourceRecordId = System.currentTimeMillis(),
                title = cleanTitle,
                subtitle = subtitle.trim().ifBlank { "Life Schedule Event" },
                dateEpochDay = dateEpochDay,
                startMinuteOfDay = safeStart,
                durationMinutes = safeDuration,
                isAllDay = isAllDay,
                timezoneId = timezoneId
            ).toEntity()
        )
        val dayItems = buildUnifiedItemsForRange(dateEpochDay, dateEpochDay)[dateEpochDay].orEmpty()
        return id to ScheduleCalendarEngine.detectConflicts(dayItems)
    }

    suspend fun rescheduleItem(
        item: ScheduledActivityItem,
        newEpochDay: Long,
        newStartMinuteOfDay: Int
    ): LifeMutationFeedback {
        if (item.isReadOnlyExternal) {
            return LifeMutationFeedback("External recorded Physical/Academic sessions are preserved historically and cannot be altered.")
        }
        val safeStart = newStartMinuteOfDay.coerceIn(0, 23 * 60 + 45)
        when (item.sourceType) {
            ScheduledSourceType.PERSONAL_TASK -> {
                rescheduleTask(item.sourceRecordId, newEpochDay, safeStart)
            }
            ScheduledSourceType.CUSTOM_EVENT -> {
                val existing = scheduleDao.getReferenceById(item.referenceId)?.toDomain()
                if (existing != null) {
                    scheduleDao.upsertReference(
                        existing.copy(
                            dateEpochDay = newEpochDay,
                            startMinuteOfDay = safeStart
                        ).toEntity()
                    )
                }
            }
            else -> {
                // Save or update an explicit reference override for this source item
                val existing = scheduleDao.getReferenceBySource(item.sourceType.name, item.sourceRecordId)?.toDomain()
                scheduleDao.upsertReference(
                    ScheduledActivityReference(
                        id = existing?.id ?: 0L,
                        sourceType = item.sourceType,
                        sourceRecordId = item.sourceRecordId,
                        title = item.title,
                        subtitle = item.subtitle,
                        dateEpochDay = newEpochDay,
                        startMinuteOfDay = safeStart,
                        durationMinutes = item.durationMinutes,
                        isAllDay = false,
                        isCompletedSnapshot = item.isCompleted,
                        timezoneId = item.timezoneId
                    ).toEntity()
                )
            }
        }
        return LifeMutationFeedback("Rescheduled '${item.title}' (0 XP).")
    }

    suspend fun deleteScheduledReference(referenceId: Long) {
        if (referenceId > 0L) scheduleDao.deleteReference(referenceId)
    }

    suspend fun suggestAlternativeSlotsForDay(
        durationMinutes: Int,
        targetEpochDay: Long,
        availability: AvailabilityWindow = AvailabilityWindow()
    ): List<TimeSlotSuggestion> {
        val dayItems = buildUnifiedItemsForRange(targetEpochDay, targetEpochDay)[targetEpochDay].orEmpty()
        return ScheduleCalendarEngine.suggestAlternativeSlots(
            durationMinutes = durationMinutes,
            targetEpochDay = targetEpochDay,
            existingItems = dayItems,
            availability = availability
        )
    }

    // --- Discipline Assessment & Weekly Review ---

    suspend fun evaluateAndPersistTodayDiscipline(
        exemptRestDays: Set<DayOfWeek> = emptySet(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): DisciplineAssessment {
        val today = LocalDate.now(zoneId).toEpochDay()
        val routines = routineDao.getAllRoutinesOnce().map { it.toDomain() }
        val routineActs = routineDao.observeAllActivities().first().map { it.toDomain() }
        val routineComp = routineDao.getCompletionsSinceOnce(today - 35L).map { it.toDomain() }
        val routineSummaries = routines.map { r ->
            RoutineEngine.summarizeRoutineForDay(r, routineActs, routineComp, today, exemptRestDays)
        }

        val habits = habitDao.getAllHabitsOnce().map { it.toDomain() }.filter { !it.isArchived }
        val habitComp = habitDao.getCompletionsSinceOnce(today - 180L).map { it.toDomain() }
        val habitSummaries = habits.map { h ->
            HabitEngine.buildAnalyticsSummary(h, habitComp, today, exemptRestDays)
        }

        val tasks = observeTasks().first()
        val goalsWithDetails = observeGoalsWithDetails().first()
        val dayItems = buildUnifiedItemsForRange(today, today, zoneId)[today].orEmpty()
        val conflicts = ScheduleCalendarEngine.detectConflicts(dayItems)

        val assessment = DisciplineEngine.assessDay(
            targetEpochDay = today,
            routineSummaries = routineSummaries,
            habitSummaries = habitSummaries,
            tasks = tasks,
            goalsWithDetails = goalsWithDetails,
            scheduledItemsToday = dayItems,
            conflictsToday = conflicts,
            exemptRestDays = exemptRestDays
        )
        val existing = disciplineDao.getAssessmentForDay(today)
        disciplineDao.upsertAssessment(assessment.copy(id = existing?.id ?: 0L).toEntity())
        return assessment
    }

    suspend fun buildWeeklyReviewSummary(
        exemptRestDays: Set<DayOfWeek> = emptySet(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): WeeklyReviewSummary {
        val today = LocalDate.now(zoneId).toEpochDay()
        val assessments = disciplineDao.getRecentAssessmentsOnce(14).map { it.toDomain() }
        val routines = routineDao.getAllRoutinesOnce().map { it.toDomain() }
        val routineActs = routineDao.observeAllActivities().first().map { it.toDomain() }
        val routineComp = routineDao.getCompletionsSinceOnce(today - 35L).map { it.toDomain() }
        val routineSummaries = routines.map {
            RoutineEngine.summarizeRoutineForDay(it, routineActs, routineComp, today, exemptRestDays)
        }
        val habits = habitDao.getAllHabitsOnce().map { it.toDomain() }.filter { !it.isArchived }
        val habitComp = habitDao.getCompletionsSinceOnce(today - 180L).map { it.toDomain() }
        val habitSummaries = habits.map {
            HabitEngine.buildAnalyticsSummary(it, habitComp, today, exemptRestDays)
        }
        val tasks = observeTasks().first()
        val goalsWithDetails = observeGoalsWithDetails().first()
        val dayItems = buildUnifiedItemsForRange(today, today, zoneId)[today].orEmpty()
        val conflictsCount = ScheduleCalendarEngine.detectConflicts(dayItems).size

        val sevenDaysAgoMs = System.currentTimeMillis() - 7 * 24 * 60 * 60_000L
        val studyMins = database.studySessionDao().observeAllSessions().first()
            .filter { it.startEpochMs >= sevenDaysAgoMs }
            .sumOf { it.durationMinutes }
        val physicalCount = database.joggingSessionDao().getSessionsSince(sevenDaysAgoMs).size +
            database.workoutSessionDao().getWorkoutsSince(sevenDaysAgoMs).size

        return DisciplineEngine.buildWeeklyReview(
            weekEndEpochDay = today,
            assessments = assessments,
            routineSummaries = routineSummaries,
            habitSummaries = habitSummaries,
            tasks = tasks,
            goalsWithDetails = goalsWithDetails,
            conflictsCount = conflictsCount,
            studyMinutesTotal = studyMins,
            physicalSessionsTotal = physicalCount
        )
    }

    suspend fun saveReflectionReview(summaryNote: String): LifeMutationFeedback {
        val today = LocalDate.now().toEpochDay()
        val assessment = evaluateAndPersistTodayDiscipline()
        val clean = summaryNote.trim().ifBlank { assessment.reflectionSummary }
        disciplineDao.upsertAssessment(
            assessment.copy(
                id = disciplineDao.getAssessmentForDay(today)?.id ?: 0L,
                reflectionSummary = clean
            ).toEntity()
        )
        return awardLifeProgression(
            rewardKey = "reflection_$today",
            kind = LifeActivityKind.REFLECTION,
            title = "Discipline Review Logged",
            verificationStatus = VerificationStatus.SELF_REPORTED,
            notes = clean
        )
    }

    // --- Deterministic Progression Wiring ---

    private suspend fun awardLifeProgression(
        rewardKey: String,
        kind: LifeActivityKind,
        title: String,
        verificationStatus: VerificationStatus,
        habitCategory: HabitCategory? = null,
        taskCategory: TaskCategory? = null,
        goalAttribute: AttributeType? = null,
        streakDays: Int = 0,
        notes: String = ""
    ): LifeMutationFeedback {
        var awarded = 0
        var capped = false
        var feedbackNote = ""

        database.withTransaction {
            if (progressionDao.hasRewardKey(rewardKey)) {
                feedbackNote = "Action recorded (already rewarded for this cycle)."
                return@withTransaction
            }

            val dayStartMs = todayStartEpochMs()
            val lifeXpToday = progressionDao.getLifeXpSince(dayStartMs)
            val selfReportedCountToday = progressionDao.getSelfReportedCountSince(dayStartMs)

            val reward = ProgressionEngine.calculateLifeDisciplineReward(
                kind = kind,
                verificationStatus = verificationStatus,
                lifeXpAlreadyEarnedToday = lifeXpToday,
                selfReportedLifeCountToday = selfReportedCountToday,
                habitCategory = habitCategory,
                taskCategory = taskCategory,
                goalAttribute = goalAttribute,
                streakDays = streakDays
            )

            awarded = reward.xpAwarded
            capped = reward.wasCapped
            feedbackNote = reward.validationNote

            val now = System.currentTimeMillis()
            val deltas = mutableMapOf<AttributeType, Int>()
            deltas[reward.primaryAttribute] = (deltas[reward.primaryAttribute] ?: 0) + reward.primaryAttrXpAwarded
            deltas[reward.secondaryAttribute] = (deltas[reward.secondaryAttribute] ?: 0) + reward.secondaryAttrXpAwarded

            progressionDao.insertRecord(
                LifeProgressionRecordEntity(
                    rewardKey = rewardKey,
                    activityKindName = kind.name,
                    title = title,
                    verificationStatusName = verificationStatus.name,
                    xpAwarded = reward.xpAwarded,
                    disXpAwarded = deltas[AttributeType.DIS] ?: 0,
                    wisXpAwarded = deltas[AttributeType.WIS] ?: 0,
                    chaXpAwarded = deltas[AttributeType.CHA] ?: 0,
                    intXpAwarded = deltas[AttributeType.INT] ?: 0,
                    vitXpAwarded = deltas[AttributeType.VIT] ?: 0,
                    notes = notes,
                    timestampEpochMs = now
                )
            )

            val profile = playerDao.getPlayerOnce()?.toDomain() ?: PlayerProfile()
            val levelRes = ProgressionEngine.applyXp(
                currentLevel = profile.level,
                currentLevelXp = profile.currentLevelXp,
                totalXp = profile.totalXp,
                gainedXp = reward.xpAwarded
            )
            val updatedAttrs = ProgressionEngine.applyAttributeXp(
                attributes = profile.attributes,
                deltas = deltas,
                isVerified = verificationStatus.isVerified
            )
            val newVerified = profile.verifiedActivitiesCount + if (verificationStatus.isVerified) 1 else 0
            val newSelfReported = profile.selfReportedActivitiesCount + if (!verificationStatus.isVerified) 1 else 0
            val newRank = ProgressionEngine.evaluateRank(
                level = levelRes.newLevel,
                totalAttributeScore = updatedAttrs.totalScore,
                verifiedActivitiesCount = newVerified
            )

            val updatedProfile = profile.copy(
                level = levelRes.newLevel,
                rank = newRank,
                currentLevelXp = levelRes.newCurrentLevelXp,
                xpToNextLevel = levelRes.newXpToNextLevel,
                totalXp = levelRes.newTotalXp,
                attributes = updatedAttrs,
                verifiedActivitiesCount = newVerified,
                selfReportedActivitiesCount = newSelfReported
            )
            playerDao.upsertPlayer(updatedProfile.toEntity())

            val logCategory = when (reward.primaryAttribute) {
                AttributeType.CHA -> QuestCategory.SOCIAL
                AttributeType.INT -> QuestCategory.INTELLECT
                AttributeType.VIT -> QuestCategory.RECOVERY
                else -> QuestCategory.DISCIPLINE
            }
            activityLogDao.insertLog(
                ActivityLog(
                    title = title,
                    category = logCategory,
                    durationMinutes = 15,
                    verificationStatus = verificationStatus,
                    xpAwarded = reward.xpAwarded,
                    primaryAttribute = reward.primaryAttribute,
                    primaryAttrXpAwarded = reward.primaryAttrXpAwarded,
                    notes = notes.ifBlank { reward.validationNote },
                    timestampEpochMs = now
                ).toEntity()
            )

            if (levelRes.levelsGained > 0) {
                notificationDao.insertNotification(
                    SystemNotification(
                        title = "Level Up! Level ${levelRes.newLevel}",
                        message = "Life discipline consistency propelled you to Level ${levelRes.newLevel}.",
                        category = NotificationCategory.LEVEL_UP,
                        deepLinkRoute = "life_dashboard",
                        timestampEpochMs = now
                    ).toEntity()
                )
            }
            if (newRank != profile.rank) {
                notificationDao.insertNotification(
                    SystemNotification(
                        title = "Rank Promotion: ${newRank.displayName}",
                        message = "Promoted from ${profile.rank.displayName} to ${newRank.displayName}.",
                        category = NotificationCategory.RANK_PROMOTION,
                        deepLinkRoute = "life_dashboard",
                        timestampEpochMs = now + 1L
                    ).toEntity()
                )
            }
        }

        val summaryMsg = if (awarded > 0) "$title (+$awarded XP)" else "$title • $feedbackNote"
        return LifeMutationFeedback(
            message = summaryMsg,
            xpAwarded = awarded,
            wasCapped = capped
        )
    }

    suspend fun computeDashboardSnapshot(zoneId: ZoneId = ZoneId.systemDefault()): LifeDashboardSummary {
        val today = LocalDate.now(zoneId).toEpochDay()
        val routines = routineDao.getAllRoutinesOnce().map { it.toDomain() }
        val routineActs = routineDao.observeAllActivities().first().map { it.toDomain() }
        val routineComp = routineDao.getCompletionsSinceOnce(today - 35L).map { it.toDomain() }
        val routineSummaries = routines.map { RoutineEngine.summarizeRoutineForDay(it, routineActs, routineComp, today) }

        val habits = habitDao.getAllHabitsOnce().map { it.toDomain() }.filter { !it.isArchived }
        val habitComp = habitDao.getCompletionsSinceOnce(today - 180L).map { it.toDomain() }
        val habitSummaries = habits.map { HabitEngine.buildAnalyticsSummary(it, habitComp, today) }

        val tasks = observeTasks().first()
        val goals = observeGoalsWithDetails().first()
        val dayItems = buildUnifiedItemsForRange(today, today, zoneId)[today].orEmpty()
        val conflicts = ScheduleCalendarEngine.detectConflicts(dayItems)
        val assessment = DisciplineEngine.assessDay(
            targetEpochDay = today,
            routineSummaries = routineSummaries,
            habitSummaries = habitSummaries,
            tasks = tasks,
            goalsWithDetails = goals,
            scheduledItemsToday = dayItems,
            conflictsToday = conflicts
        )
        val lifeXpToday = progressionDao.getLifeXpSince(todayStartEpochMs(zoneId))

        return LifeDashboardSummary(
            todayDisciplineScore = assessment.disciplineScore,
            overallConsistencyPercent = ((assessment.routineConsistencyPercent + assessment.habitConsistencyPercent) / 2).coerceIn(0, 100),
            activeDisciplineStreakDays = habitSummaries.maxOfOrNull { it.currentStreakDays } ?: 0,
            activeRoutinesCount = routines.count { it.isEnabled },
            completedRoutinesToday = routineSummaries.count { it.isScheduledToday && it.completionFraction >= 1f },
            activeHabitsCount = habits.size,
            completedHabitsToday = habitSummaries.count { it.todayStatus == HabitDayStatus.COMPLETED },
            pendingTasksCount = tasks.count { it.status == PersonalTaskStatus.PENDING || it.status == PersonalTaskStatus.IN_PROGRESS || it.status == PersonalTaskStatus.POSTPONED },
            dueTodayTasksCount = tasks.count { it.isDueToday(today) },
            overdueTasksCount = tasks.count { it.isOverdue(today) },
            activeGoalsCount = goals.count { it.goal.status == GoalStatus.ACTIVE },
            todayScheduledCount = dayItems.size,
            todayConflictCount = conflicts.size,
            lifeXpToday = lifeXpToday,
            dailyLifeXpCap = ProgressionEngine.DAILY_LIFE_XP_CAP
        )
    }
}
