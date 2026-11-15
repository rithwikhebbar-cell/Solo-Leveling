package com.example.domain.engine

import com.example.domain.model.AcademicExam
import com.example.domain.model.AiDailyPlanItem
import com.example.domain.model.AvailabilityWindow
import com.example.domain.model.DisciplineAssessment
import com.example.domain.model.GoalMilestone
import com.example.domain.model.GoalStatus
import com.example.domain.model.GoalWithDetails
import com.example.domain.model.Habit
import com.example.domain.model.HabitAnalyticsSummary
import com.example.domain.model.HabitCompletionRecord
import com.example.domain.model.HabitDayStatus
import com.example.domain.model.JoggingSession
import com.example.domain.model.LifeGoal
import com.example.domain.model.LifeRoutine
import com.example.domain.model.PersonalTask
import com.example.domain.model.PersonalTaskStatus
import com.example.domain.model.ProposalStatus
import com.example.domain.model.RoutineActivity
import com.example.domain.model.RoutineActivityStatus
import com.example.domain.model.RoutineCompletionRecord
import com.example.domain.model.RoutineDaySummary
import com.example.domain.model.ScheduleConflict
import com.example.domain.model.ScheduledActivityItem
import com.example.domain.model.ScheduledActivityReference
import com.example.domain.model.ScheduledSourceType
import com.example.domain.model.ScheduledRevisionItem
import com.example.domain.model.StreakSummary
import com.example.domain.model.StudySession
import com.example.domain.model.TimeSlotSuggestion
import com.example.domain.model.WeeklyReviewSummary
import com.example.domain.model.WorkoutSession
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object StreakEngine {

    /**
     * Evaluates deterministic streaks honoring scheduled days only, skipping configured rest days
     * and explicit rest-exempt skips without punishment, and exposing a bounded bonus cap (max 20%).
     */
    fun evaluateHabitStreak(
        habit: Habit,
        recordsByDay: Map<Long, HabitCompletionRecord>,
        todayEpochDay: Long,
        exemptRestDays: Set<DayOfWeek> = emptySet(),
        lookbackDays: Int = 180
    ): StreakSummary {
        var currentStreak = 0
        var longestStreak = 0
        var runningStreak = 0
        var currentStreakClosed = false
        var exemptCount = 0
        var evaluatedScheduledDays = 0
        var missedCount = 0

        // Walk backward from today for current streak + stats
        for (offset in 0 until lookbackDays) {
            val day = todayEpochDay - offset
            val dow = LocalDate.ofEpochDay(day).dayOfWeek
            if (!habit.isScheduledOn(dow)) continue

            val record = recordsByDay[day]
            val isProtectedRest = habit.isRestDayProtected && dow in exemptRestDays

            if (isProtectedRest) {
                exemptCount++
                continue
            }

            if (record?.status == HabitDayStatus.SKIPPED) {
                exemptCount++
                continue
            }

            if (offset == 0 && record == null) {
                // Day still in progress; do not mark today missed before end of day
                continue
            }

            evaluatedScheduledDays++
            val succeeded = record?.status == HabitDayStatus.COMPLETED
            if (succeeded) {
                if (!currentStreakClosed) {
                    currentStreak++
                }
            } else {
                missedCount++
                currentStreakClosed = true
            }
        }

        // Compute longest streak chronologically across lookback window
        for (offset in (lookbackDays - 1) downTo 0) {
            val day = todayEpochDay - offset
            val dow = LocalDate.ofEpochDay(day).dayOfWeek
            if (!habit.isScheduledOn(dow)) continue
            val record = recordsByDay[day]
            val isProtectedRest = habit.isRestDayProtected && dow in exemptRestDays
            if (isProtectedRest || record?.status == HabitDayStatus.SKIPPED) continue
            if (offset == 0 && record == null) continue

            if (record?.status == HabitDayStatus.COMPLETED) {
                runningStreak++
                if (runningStreak > longestStreak) longestStreak = runningStreak
            } else {
                runningStreak = 0
            }
        }

        val capBonus = (currentStreak * 2).coerceAtMost(20)
        return StreakSummary(
            currentStreakDays = currentStreak,
            longestStreakDays = longestStreak.coerceAtLeast(currentStreak),
            exemptRestDaysCount = exemptCount,
            scheduledDaysEvaluated = evaluatedScheduledDays,
            missedDaysCount = missedCount,
            streakCapBonusPercent = capBonus
        )
    }
}

object ScheduleCalendarEngine {

    /**
     * Merges stored Schedule references with live Academic, Physical, Routine, Habit, Task, and Goal records
     * by reference (zero duplication of original source entities).
     */
    fun buildUnifiedItemsForDay(
        targetEpochDay: Long,
        routines: List<LifeRoutine>,
        routineActivities: List<RoutineActivity>,
        routineCompletions: List<RoutineCompletionRecord>,
        habits: List<Habit>,
        habitCompletions: List<HabitCompletionRecord>,
        tasks: List<PersonalTask>,
        goals: List<LifeGoal>,
        goalMilestones: List<GoalMilestone>,
        storedReferences: List<ScheduledActivityReference>,
        studySessions: List<StudySession> = emptyList(),
        exams: List<AcademicExam> = emptyList(),
        revisionQueue: List<ScheduledRevisionItem> = emptyList(),
        jogSessions: List<JoggingSession> = emptyList(),
        workoutSessions: List<WorkoutSession> = emptyList(),
        aiPlanItems: List<AiDailyPlanItem> = emptyList(),
        todayEpochDay: Long = LocalDate.now().toEpochDay(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<ScheduledActivityItem> {
        val raw = mutableListOf<ScheduledActivityItem>()
        val explicitKeys = mutableSetOf<Pair<ScheduledSourceType, Long>>()

        // 1. Stored references for the target day (custom events & explicitly placed slots)
        storedReferences
            .filter { it.dateEpochDay == targetEpochDay }
            .forEach { ref ->
                explicitKeys.add(ref.sourceType to ref.sourceRecordId)
                raw.add(
                    ScheduledActivityItem(
                        referenceId = ref.id,
                        sourceType = ref.sourceType,
                        sourceRecordId = ref.sourceRecordId,
                        title = ref.title,
                        subtitle = ref.subtitle,
                        dateEpochDay = ref.dateEpochDay,
                        startMinuteOfDay = ref.startMinuteOfDay,
                        durationMinutes = ref.durationMinutes,
                        isCompleted = ref.isCompletedSnapshot,
                        isAllDay = ref.isAllDay,
                        isReadOnlyExternal = ref.sourceType in setOf(
                            ScheduledSourceType.ACADEMIC_STUDY,
                            ScheduledSourceType.ACADEMIC_EXAM,
                            ScheduledSourceType.PHYSICAL_TRAINING
                        ),
                        timezoneId = ref.timezoneId
                    )
                )
            }

        // 2. Routines enabled & scheduled for target day's DayOfWeek
        routines.filter { RoutineEngine.isScheduledOn(it, targetEpochDay) }.forEach { routine ->
            if ((ScheduledSourceType.ROUTINE to routine.id) !in explicitKeys) {
                val acts = routineActivities.filter { it.routineId == routine.id && !it.isOptional }
                val doneIds = routineCompletions
                    .filter { it.routineId == routine.id && it.dateEpochDay == targetEpochDay && it.status == RoutineActivityStatus.COMPLETED }
                    .map { it.activityId }
                    .toSet()
                val completed = acts.isNotEmpty() && acts.all { it.id in doneIds }
                raw.add(
                    ScheduledActivityItem(
                        sourceType = ScheduledSourceType.ROUTINE,
                        sourceRecordId = routine.id,
                        title = routine.title,
                        subtitle = "${routine.timeSlot.label} • ${acts.size} core steps",
                        dateEpochDay = targetEpochDay,
                        startMinuteOfDay = routine.startMinuteOfDay,
                        durationMinutes = routine.targetDurationMinutes.coerceAtLeast(10),
                        isCompleted = completed,
                        priorityWeight = 3
                    )
                )
            }
        }

        // 3. Habits scheduled with preferred time on target day
        val targetDow = LocalDate.ofEpochDay(targetEpochDay).dayOfWeek
        habits.filter { !it.isArchived && it.isScheduledOn(targetDow) && it.preferredStartMinute != null }.forEach { habit ->
            if ((ScheduledSourceType.HABIT to habit.id) !in explicitKeys) {
                val rec = habitCompletions.firstOrNull { it.habitId == habit.id && it.dateEpochDay == targetEpochDay }
                raw.add(
                    ScheduledActivityItem(
                        sourceType = ScheduledSourceType.HABIT,
                        sourceRecordId = habit.id,
                        title = habit.title,
                        subtitle = "Habit • ${habit.category.label}",
                        dateEpochDay = targetEpochDay,
                        startMinuteOfDay = habit.preferredStartMinute ?: 9 * 60,
                        durationMinutes = if (habit.habitType.name == "DURATION") habit.targetValue.toInt().coerceIn(10, 120) else 20,
                        isCompleted = rec?.status == HabitDayStatus.COMPLETED,
                        priorityWeight = 2
                    )
                )
            }
        }

        // 4. Personal tasks due on target day
        tasks.filter { it.status != PersonalTaskStatus.ARCHIVED && it.dueDateEpochDay == targetEpochDay }.forEach { task ->
            if ((ScheduledSourceType.PERSONAL_TASK to task.id) !in explicitKeys) {
                raw.add(
                    ScheduledActivityItem(
                        sourceType = ScheduledSourceType.PERSONAL_TASK,
                        sourceRecordId = task.id,
                        title = task.title,
                        subtitle = "${task.category.label} • ${task.priority.label}",
                        dateEpochDay = targetEpochDay,
                        startMinuteOfDay = task.scheduledStartMinute ?: 11 * 60,
                        durationMinutes = task.estimatedMinutes.coerceAtLeast(15),
                        isCompleted = task.status == PersonalTaskStatus.COMPLETED,
                        isAllDay = task.scheduledStartMinute == null,
                        priorityWeight = task.priority.weight,
                        timezoneId = task.timezoneId
                    )
                )
            }
        }

        // 5. Goal milestones due on target day
        val goalById = goals.associateBy { it.id }
        goalMilestones.filter { it.dueDateEpochDay == targetEpochDay }.forEach { milestone ->
            if ((ScheduledSourceType.GOAL_MILESTONE to milestone.id) !in explicitKeys) {
                val goalTitle = goalById[milestone.goalId]?.title ?: "Life Goal"
                raw.add(
                    ScheduledActivityItem(
                        sourceType = ScheduledSourceType.GOAL_MILESTONE,
                        sourceRecordId = milestone.id,
                        title = milestone.title,
                        subtitle = "Milestone • $goalTitle",
                        dateEpochDay = targetEpochDay,
                        startMinuteOfDay = 17 * 60,
                        durationMinutes = 30,
                        isCompleted = milestone.isCompleted,
                        isAllDay = true,
                        priorityWeight = 3
                    )
                )
            }
        }

        // 6. Academic study sessions recorded on target day (by reference)
        studySessions.forEach { session ->
            val zdt = Instant.ofEpochMilli(session.startEpochMs).atZone(zoneId)
            if (zdt.toLocalDate().toEpochDay() == targetEpochDay &&
                (ScheduledSourceType.ACADEMIC_STUDY to session.id) !in explicitKeys
            ) {
                raw.add(
                    ScheduledActivityItem(
                        sourceType = ScheduledSourceType.ACADEMIC_STUDY,
                        sourceRecordId = session.id,
                        title = session.title,
                        subtitle = "Academic (${session.subjectName}) • ${session.durationMinutes}m",
                        dateEpochDay = targetEpochDay,
                        startMinuteOfDay = zdt.hour * 60 + zdt.minute,
                        durationMinutes = session.durationMinutes.coerceAtLeast(10),
                        isCompleted = true,
                        isReadOnlyExternal = true,
                        priorityWeight = 3
                    )
                )
            }
        }

        // 7. Academic exams on target day (by reference)
        exams.filter { !it.isCompleted }.forEach { exam ->
            val zdt = Instant.ofEpochMilli(exam.examDateEpochMs).atZone(zoneId)
            if (zdt.toLocalDate().toEpochDay() == targetEpochDay &&
                (ScheduledSourceType.ACADEMIC_EXAM to exam.id) !in explicitKeys
            ) {
                raw.add(
                    ScheduledActivityItem(
                        sourceType = ScheduledSourceType.ACADEMIC_EXAM,
                        sourceRecordId = exam.id,
                        title = "EXAM: ${exam.title}",
                        subtitle = "${exam.subjectName} • Target ${exam.targetScoreDisplay}",
                        dateEpochDay = targetEpochDay,
                        startMinuteOfDay = (zdt.hour * 60 + zdt.minute).takeIf { it > 0 } ?: 9 * 60,
                        durationMinutes = 90,
                        isCompleted = exam.isCompleted,
                        isReadOnlyExternal = true,
                        priorityWeight = 4
                    )
                )
            }
        }

        // 8. Due / Overdue revisions projected onto today (by reference)
        if (targetEpochDay == todayEpochDay) {
            revisionQueue.take(3).forEachIndexed { idx, rev ->
                if ((ScheduledSourceType.ACADEMIC_REVISION to rev.topic.id) !in explicitKeys) {
                    raw.add(
                        ScheduledActivityItem(
                            sourceType = ScheduledSourceType.ACADEMIC_REVISION,
                            sourceRecordId = rev.topic.id,
                            title = "Revise: ${rev.topic.title}",
                            subtitle = "${rev.subjectName} • ${rev.urgency.label}",
                            dateEpochDay = targetEpochDay,
                            startMinuteOfDay = 16 * 60 + (idx * 25),
                            durationMinutes = 20,
                            isCompleted = false,
                            priorityWeight = 3
                        )
                    )
                }
            }
        }

        // 9. Physical jogging & workout sessions on target day (by reference)
        jogSessions.forEach { jog ->
            val zdt = Instant.ofEpochMilli(jog.startEpochMs).atZone(zoneId)
            if (zdt.toLocalDate().toEpochDay() == targetEpochDay &&
                (ScheduledSourceType.PHYSICAL_TRAINING to jog.id) !in explicitKeys
            ) {
                raw.add(
                    ScheduledActivityItem(
                        sourceType = ScheduledSourceType.PHYSICAL_TRAINING,
                        sourceRecordId = jog.id,
                        title = jog.title,
                        subtitle = "Physical (${jog.activityKind.label}) • ${jog.durationMinutes}m",
                        dateEpochDay = targetEpochDay,
                        startMinuteOfDay = zdt.hour * 60 + zdt.minute,
                        durationMinutes = jog.durationMinutes.coerceAtLeast(10),
                        isCompleted = true,
                        isReadOnlyExternal = true,
                        priorityWeight = 3
                    )
                )
            }
        }
        workoutSessions.forEach { workout ->
            val zdt = Instant.ofEpochMilli(workout.startEpochMs).atZone(zoneId)
            if (zdt.toLocalDate().toEpochDay() == targetEpochDay &&
                (ScheduledSourceType.PHYSICAL_TRAINING to (100_000L + workout.id)) !in explicitKeys
            ) {
                raw.add(
                    ScheduledActivityItem(
                        sourceType = ScheduledSourceType.PHYSICAL_TRAINING,
                        sourceRecordId = 100_000L + workout.id,
                        title = workout.title,
                        subtitle = "Strength (${workout.totalSets} sets) • ${workout.durationMinutes}m",
                        dateEpochDay = targetEpochDay,
                        startMinuteOfDay = zdt.hour * 60 + zdt.minute,
                        durationMinutes = workout.durationMinutes.coerceAtLeast(10),
                        isCompleted = true,
                        isReadOnlyExternal = true,
                        priorityWeight = 3
                    )
                )
            }
        }

        // 10. Approved AI plan proposals shown on today
        if (targetEpochDay == todayEpochDay) {
            aiPlanItems.filter { it.status == ProposalStatus.APPROVED }.forEach { plan ->
                raw.add(
                    ScheduledActivityItem(
                        sourceType = ScheduledSourceType.AI_PLAN_PROPOSAL,
                        sourceRecordId = plan.id,
                        title = plan.taskTitle,
                        subtitle = "AI Approved Plan • ${plan.module}",
                        dateEpochDay = targetEpochDay,
                        startMinuteOfDay = plan.startMinuteOfDay,
                        durationMinutes = plan.durationMinutes,
                        isCompleted = false,
                        priorityWeight = plan.priority.weight
                    )
                )
            }
        }

        val sorted = raw.sortedWith(
            compareBy<ScheduledActivityItem> { !it.isAllDay }
                .thenBy { it.startMinuteOfDay }
                .thenByDescending { it.priorityWeight }
        )
        val conflicts = detectConflicts(sorted)
        val conflictingKeys = conflicts.flatMap { listOf(it.first.sourceType to it.first.sourceRecordId, it.second.sourceType to it.second.sourceRecordId) }.toSet()

        return sorted.map { item ->
            if ((item.sourceType to item.sourceRecordId) in conflictingKeys && !item.isAllDay) {
                item.copy(hasConflict = true)
            } else {
                item
            }
        }
    }

    fun detectConflicts(items: List<ScheduledActivityItem>): List<ScheduleConflict> {
        val timed = items.filter { !it.isAllDay }.sortedBy { it.startMinuteOfDay }
        val conflicts = mutableListOf<ScheduleConflict>()
        for (i in timed.indices) {
            val a = timed[i]
            for (j in i + 1 until timed.size) {
                val b = timed[j]
                if (a.dateEpochDay != b.dateEpochDay) continue
                if (b.startMinuteOfDay >= a.endMinuteOfDay) break
                val overlap = minOf(a.endMinuteOfDay, b.endMinuteOfDay) - maxOf(a.startMinuteOfDay, b.startMinuteOfDay)
                if (overlap > 0) {
                    conflicts.add(
                        ScheduleConflict(
                            first = a,
                            second = b,
                            overlapMinutes = overlap,
                            description = "'${a.title}' overlaps '${b.title}' by ${overlap}m (${a.formattedWindow()} vs ${b.formattedWindow()})"
                        )
                    )
                }
            }
        }
        return conflicts
    }

    fun suggestAlternativeSlots(
        durationMinutes: Int,
        targetEpochDay: Long,
        existingItems: List<ScheduledActivityItem>,
        availability: AvailabilityWindow = AvailabilityWindow(),
        maxSuggestions: Int = 5
    ): List<TimeSlotSuggestion> {
        val needed = durationMinutes.coerceIn(10, 240)
        val occupied = existingItems
            .filter { !it.isAllDay && it.dateEpochDay == targetEpochDay }
            .map { it.startMinuteOfDay to it.endMinuteOfDay }
            .sortedBy { it.first }

        val suggestions = mutableListOf<TimeSlotSuggestion>()
        var cursor = availability.startMinuteOfDay.coerceIn(0, 23 * 60)
        val limitEnd = availability.endMinuteOfDay.coerceIn(cursor + needed, 24 * 60)

        while (cursor + needed <= limitEnd && suggestions.size < maxSuggestions) {
            val end = cursor + needed
            val clash = occupied.firstOrNull { (occStart, occEnd) ->
                minOf(end, occEnd) > maxOf(cursor, occStart)
            }
            if (clash == null) {
                suggestions.add(
                    TimeSlotSuggestion(
                        dateEpochDay = targetEpochDay,
                        startMinuteOfDay = cursor,
                        durationMinutes = needed
                    )
                )
                cursor += maxOf(needed + 15, 30)
            } else {
                cursor = (clash.second + 10).coerceAtLeast(cursor + 15)
            }
        }
        return suggestions
    }
}

object DisciplineEngine {

    fun assessDay(
        targetEpochDay: Long,
        routineSummaries: List<RoutineDaySummary>,
        habitSummaries: List<HabitAnalyticsSummary>,
        tasks: List<PersonalTask>,
        goalsWithDetails: List<GoalWithDetails>,
        scheduledItemsToday: List<ScheduledActivityItem>,
        conflictsToday: List<ScheduleConflict>,
        exemptRestDays: Set<DayOfWeek> = emptySet()
    ): DisciplineAssessment {
        val dow = LocalDate.ofEpochDay(targetEpochDay).dayOfWeek
        val isRestDay = dow in exemptRestDays

        val scheduledRoutines = routineSummaries.filter { it.isScheduledToday }
        val routinePct = if (scheduledRoutines.isEmpty()) null else {
            val avg = scheduledRoutines.map { (it.completionFraction * 100f).toInt() }.average()
            avg.toInt().coerceIn(0, 100)
        }

        val scheduledHabits = habitSummaries.filter {
            it.habit.isScheduledOn(dow) && !(it.habit.isRestDayProtected && isRestDay)
        }
        val habitPct = if (scheduledHabits.isEmpty()) null else {
            val pts = scheduledHabits.sumOf {
                when (it.todayStatus) {
                    HabitDayStatus.COMPLETED -> 100
                    HabitDayStatus.PARTIAL -> 50
                    HabitDayStatus.SKIPPED -> 100
                    else -> 0
                }
            }
            (pts / scheduledHabits.size).coerceIn(0, 100)
        }

        val dueOrActiveTasks = tasks.filter {
            it.status != PersonalTaskStatus.ARCHIVED &&
                (it.dueDateEpochDay == targetEpochDay || (it.dueDateEpochDay != null && it.dueDateEpochDay < targetEpochDay && it.status != PersonalTaskStatus.COMPLETED))
        }
        val taskPct = if (dueOrActiveTasks.isEmpty()) null else {
            val done = dueOrActiveTasks.count { it.status == PersonalTaskStatus.COMPLETED }
            ((done * 100) / dueOrActiveTasks.size).coerceIn(0, 100)
        }

        val schedulePct = if (scheduledItemsToday.isEmpty()) null else {
            val completedBlocks = scheduledItemsToday.count { it.isCompleted }
            val rawAdherence = ((completedBlocks * 100) / scheduledItemsToday.size)
            val conflictPenalty = (conflictsToday.size * 8).coerceAtMost(24)
            (rawAdherence - conflictPenalty).coerceIn(0, 100)
        }

        val activeGoals = goalsWithDetails.filter { it.goal.status == GoalStatus.ACTIVE || it.goal.status == GoalStatus.COMPLETED }
        val goalPct = if (activeGoals.isEmpty()) null else {
            activeGoals.map { it.computedProgressPercent }.average().toInt().coerceIn(0, 100)
        }

        val weightedComponents = listOfNotNull(
            routinePct?.let { Triple("Routines", it, 0.25) },
            habitPct?.let { Triple("Habits", it, 0.25) },
            taskPct?.let { Triple("Tasks", it, 0.20) },
            schedulePct?.let { Triple("Schedule", it, 0.15) },
            goalPct?.let { Triple("Goals", it, 0.15) }
        )

        val totalScore = if (weightedComponents.isEmpty()) {
            0
        } else {
            val weightSum = weightedComponents.sumOf { it.third }
            val rawScore = weightedComponents.sumOf { it.second * it.third } / weightSum
            rawScore.toInt().coerceIn(0, 100)
        }

        val strongest = weightedComponents.maxByOrNull { it.second }?.first ?: "Getting Started"
        val weakest = weightedComponents.minByOrNull { it.second }?.first ?: "None Flagged"

        val missedCommitments = tasks.count { it.isOverdue(targetEpochDay) } +
            routineSummaries.sumOf { it.missedRequiredCount } +
            habitSummaries.count { it.todayStatus == HabitDayStatus.MISSED }

        val exemptDaysCount = (if (isRestDay) 1 else 0) +
            habitSummaries.count { it.todayStatus == HabitDayStatus.SKIPPED }

        val reflection = buildString {
            if (weightedComponents.isEmpty()) {
                append("No life routines, habits, or tasks scheduled yet. Add a structured morning protocol or keystone habit to begin building DIS.")
            } else {
                append("Discipline Index $totalScore/100. Strongest consistency in $strongest.")
                if (missedCommitments > 0) {
                    append(" $missedCommitments open commitment(s) can be rescheduled without penalty.")
                } else {
                    append(" Zero overdue commitments detected.")
                }
                if (exemptDaysCount > 0) {
                    append(" Protected rest/skipped commitments are excluded from failure penalties.")
                }
            }
        }

        return DisciplineAssessment(
            dateEpochDay = targetEpochDay,
            disciplineScore = totalScore,
            routineConsistencyPercent = routinePct ?: 0,
            habitConsistencyPercent = habitPct ?: 0,
            taskCompletionPercent = taskPct ?: 0,
            scheduleAdherencePercent = schedulePct ?: 0,
            goalProgressPercent = goalPct ?: 0,
            strongestArea = strongest,
            weakestArea = weakest,
            missedCommitmentsCount = missedCommitments,
            exemptRestDaysCount = exemptDaysCount,
            reflectionSummary = reflection
        )
    }

    fun buildWeeklyReview(
        weekEndEpochDay: Long,
        assessments: List<DisciplineAssessment>,
        routineSummaries: List<RoutineDaySummary>,
        habitSummaries: List<HabitAnalyticsSummary>,
        tasks: List<PersonalTask>,
        goalsWithDetails: List<GoalWithDetails>,
        conflictsCount: Int,
        studyMinutesTotal: Int,
        physicalSessionsTotal: Int
    ): WeeklyReviewSummary {
        val weekStartEpochDay = weekEndEpochDay - 6L
        val weekAssessments = assessments.filter { it.dateEpochDay in weekStartEpochDay..weekEndEpochDay }
        val avgDiscipline = if (weekAssessments.isEmpty()) {
            assessments.firstOrNull()?.disciplineScore ?: 0
        } else {
            weekAssessments.map { it.disciplineScore }.average().toInt().coerceIn(0, 100)
        }

        val routineCompleted = routineSummaries.sumOf { it.completedCount }
        val routineScheduled = routineSummaries.sumOf { it.requiredCount }
        val habitsCompleted = habitSummaries.sumOf { it.completedDays30d }
        val habitsScheduled = habitSummaries.size * 7
        val tasksCompleted = tasks.count {
            it.status == PersonalTaskStatus.COMPLETED &&
                (it.completedAtEpochMs != null &&
                    Instant.ofEpochMilli(it.completedAtEpochMs).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay() in weekStartEpochDay..weekEndEpochDay)
        }
        val overdueTasks = tasks.count { it.isOverdue(weekEndEpochDay) }
        val milestonesDone = goalsWithDetails.sumOf { it.completedMilestonesCount }

        val areaScores = mapOf(
            "Routines" to (weekAssessments.map { it.routineConsistencyPercent }.average().takeIf { !it.isNaN() } ?: 0.0),
            "Habits" to (weekAssessments.map { it.habitConsistencyPercent }.average().takeIf { !it.isNaN() } ?: 0.0),
            "Tasks" to (weekAssessments.map { it.taskCompletionPercent }.average().takeIf { !it.isNaN() } ?: 0.0),
            "Goals" to (weekAssessments.map { it.goalProgressPercent }.average().takeIf { !it.isNaN() } ?: 0.0)
        )
        val strongestDomain = areaScores.maxByOrNull { it.value }?.key ?: "Discipline Foundation"

        val advice = when {
            conflictsCount > 0 -> "Resolve $conflictsCount overlapping schedule block(s) in Daily Schedule to protect focus blocks."
            overdueTasks > 0 -> "Triage $overdueTasks overdue task(s): complete the smallest high-priority action or reschedule realistically."
            avgDiscipline >= 75 -> "Strong weekly adherence ($avgDiscipline/100). Maintain current cadence and avoid over-scheduling rest days."
            else -> "Simplify active commitments to 1 keystone morning routine and 2 daily non-negotiable habits for sustainable momentum."
        }

        return WeeklyReviewSummary(
            weekStartEpochDay = weekStartEpochDay,
            weekEndEpochDay = weekEndEpochDay,
            averageDisciplineScore = avgDiscipline,
            routineActivitiesCompleted = routineCompleted,
            routineActivitiesScheduled = routineScheduled,
            habitsCompletedCount = habitsCompleted,
            habitsScheduledCount = habitsScheduled,
            tasksCompletedCount = tasksCompleted,
            tasksOverdueCount = overdueTasks,
            goalMilestonesCompletedCount = milestonesDone,
            scheduleConflictsCount = conflictsCount,
            studyMinutesTotal = studyMinutesTotal,
            physicalSessionsTotal = physicalSessionsTotal,
            strongestDomain = strongestDomain,
            priorityAdjustmentAdvice = advice
        )
    }
}
