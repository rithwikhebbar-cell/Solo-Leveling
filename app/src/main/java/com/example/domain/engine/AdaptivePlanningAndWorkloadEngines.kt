package com.example.domain.engine

import com.example.domain.model.AdaptiveDailyPlan
import com.example.domain.model.AdaptivePlanBlock
import com.example.domain.model.AdaptivePlanBlockStatus
import com.example.domain.model.DataProvenanceTag
import com.example.domain.model.HabitDayStatus
import com.example.domain.model.PersonalTaskStatus
import com.example.domain.model.PlayerIntelligencePreferences
import com.example.domain.model.RecoveryState
import com.example.domain.model.ScheduledActivityItem
import com.example.domain.model.ScheduledSourceType
import com.example.domain.model.WorkloadAssessment
import com.example.domain.model.WorkloadLevel
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Evaluates daily cognitive & physical workload balance without punishing rest or assuming laziness.
 * Prioritizes recovery whenever REST_RECOMMENDED, RECOVERY_RECOMMENDED, or self-reported discomfort is active.
 */
object WorkloadBalanceEngine {

    fun assess(
        input: IntelligenceTelemetryInput,
        prefs: PlayerIntelligencePreferences = PlayerIntelligencePreferences()
    ): WorkloadAssessment {
        val today = input.todayEpochDay
        val conflictsCount = ScheduleCalendarEngine.detectConflicts(input.scheduledItemsToday).size
        val overdueTasks = input.tasks.count { it.isOverdue(today) }
        val pendingTodayTasks = input.tasks.count {
            it.status != PersonalTaskStatus.COMPLETED &&
                it.status != PersonalTaskStatus.ARCHIVED &&
                (it.dueDateEpochDay == today || it.isOverdue(today))
        }
        val activeRoutinesToday = input.routines.count { RoutineEngine.isScheduledOn(it, today) }
        val todayDow = LocalDate.ofEpochDay(today).dayOfWeek
        val activeHabitsToday = input.habits.count { !it.isArchived && it.isScheduledOn(todayDow) }

        val academicTodayMins = input.studySessions.filter {
            Instant.ofEpochMilli(it.startEpochMs).atZone(input.zoneId).toLocalDate().toEpochDay() == today
        }.sumOf { it.durationMinutes }

        val scheduledMinutesToday = input.scheduledItemsToday.filter { !it.isAllDay && !it.isCompleted }.sumOf { it.durationMinutes }
        val maxDailyBudget = (prefs.maxDailyFocusMinutes + prefs.maxDailyWorkoutMinutes + 60).coerceAtLeast(150)

        val recoveryPriority = input.recovery.state == RecoveryState.REST_RECOMMENDED ||
            input.recovery.state == RecoveryState.RECOVERY_RECOMMENDED ||
            input.recovery.selfReportedDiscomfort

        val commitments = pendingTodayTasks + activeRoutinesToday + activeHabitsToday
        val rawScore = (
            (scheduledMinutesToday.toFloat() / maxDailyBudget.toFloat()) * 45f +
                (input.recovery.measuredLoadMinutes48h.toFloat() / 120f) * 25f +
                (overdueTasks * 8) +
                (conflictsCount * 10)
            ).toInt().coerceIn(5, 100)

        val level = when {
            recoveryPriority -> WorkloadLevel.RECOVERY_PRIORITY
            rawScore >= 78 || overdueTasks >= 4 || conflictsCount >= 2 -> WorkloadLevel.OVERLOADED
            rawScore >= 60 -> WorkloadLevel.HIGH_WORKLOAD
            rawScore <= 22 && commitments <= 2 -> WorkloadLevel.UNDERLOADED
            else -> WorkloadLevel.BALANCED
        }

        val directive = when (level) {
            WorkloadLevel.RECOVERY_PRIORITY ->
                "Recovery safety rule active (${input.recovery.state.code}): replace strenuous workouts with light mobility or penalty-free rest and trim non-urgent tasks."
            WorkloadLevel.OVERLOADED ->
                "Heavy load detected ($overdueTasks overdue, $conflictsCount conflict(s)). Defer low-priority blocks to tomorrow and protect essential focus + rest windows."
            WorkloadLevel.HIGH_WORKLOAD ->
                "High commitment density today. Lock top 2 priorities first and reserve recovery buffer transitions."
            WorkloadLevel.UNDERLOADED ->
                "Light schedule detected. You have room for one focused study or aerobic session if readiness is READY."
            WorkloadLevel.BALANCED ->
                "Daily commitments and physical readiness are well balanced. Execute planned blocks sequentially."
        }

        return WorkloadAssessment(
            assessmentUuid = UUID.randomUUID().toString(),
            dateEpochDay = today,
            workloadLevel = level,
            loadScore = rawScore,
            physicalLoadMinutes48h = input.recovery.measuredLoadMinutes48h,
            academicLoadMinutesToday = academicTodayMins,
            activeCommitmentsCount = commitments,
            overdueItemsCount = overdueTasks,
            scheduleConflictCount = conflictsCount,
            recoveryState = input.recovery.state,
            adjustmentDirective = directive,
            preservesRestWithoutPunishment = true,
            provenance = DataProvenanceTag.ESTIMATE,
            evaluatedAtEpochMs = input.nowEpochMs
        )
    }
}

/**
 * Generates conflict-free, recovery-safe, approval-gated Adaptive Daily Plans.
 * Respects existing scheduled events, completed tasks, partial days, and user preferences.
 */
object AdaptivePlanningEngine {

    private data class CandidateBlock(
        val title: String,
        val module: String,
        val preferredStartMinute: Int,
        val durationMinutes: Int,
        val priorityWeight: Int,
        val priorityScore: Int,
        val priorityReason: String,
        val dependencies: String = "",
        val linkedSourceType: ScheduledSourceType = ScheduledSourceType.AI_PLAN_PROPOSAL,
        val linkedSourceId: Long = 0L,
        val isRecoveryProtected: Boolean = false
    )

    fun generateDailyPlan(
        input: IntelligenceTelemetryInput,
        workload: WorkloadAssessment,
        prefs: PlayerIntelligencePreferences = PlayerIntelligencePreferences(),
        targetEpochDay: Long = input.todayEpochDay,
        existingScheduledItems: List<ScheduledActivityItem> = input.scheduledItemsToday,
        isPartialDay: Boolean = false,
        currentMinuteOfDay: Int = 8 * 60
    ): AdaptiveDailyPlan {
        val planUuid = UUID.randomUUID().toString()
        val dow = LocalDate.ofEpochDay(targetEpochDay).dayOfWeek
        val isConfiguredRestDay = dow in prefs.restDays
        val recoveryRestricted = workload.workloadLevel == WorkloadLevel.RECOVERY_PRIORITY ||
            input.recovery.state == RecoveryState.REST_RECOMMENDED ||
            input.recovery.state == RecoveryState.RECOVERY_RECOMMENDED ||
            input.recovery.selfReportedDiscomfort

        val candidates = mutableListOf<CandidateBlock>()
        val styleFactor = prefs.planningStyle.maxLoadFactor

        // 1. Physical or Recovery Block (skip if already trained today on target date)
        val alreadyTrainedToday = input.joggingSessions.any {
            Instant.ofEpochMilli(it.startEpochMs).atZone(input.zoneId).toLocalDate().toEpochDay() == targetEpochDay
        } || input.workoutSessions.any {
            Instant.ofEpochMilli(it.startEpochMs).atZone(input.zoneId).toLocalDate().toEpochDay() == targetEpochDay
        }

        if (!alreadyTrainedToday) {
            if (recoveryRestricted || isConfiguredRestDay) {
                candidates.add(
                    CandidateBlock(
                        title = if (isConfiguredRestDay) "Protected Rest & Mobility Stretch" else "Recovery Mobility & Hydration Reset",
                        module = "RECOVERY",
                        preferredStartMinute = prefs.preferredWorkoutHour * 60,
                        durationMinutes = 15,
                        priorityWeight = 4,
                        priorityScore = 95,
                        priorityReason = if (recoveryRestricted) {
                            "Recovery safety guardrail (${input.recovery.state.code}) forbids strenuous workouts."
                        } else {
                            "Configured rest day protected without penalty."
                        },
                        isRecoveryProtected = true
                    )
                )
            } else {
                val baseMins = ((input.dailyJoggingTargetMinutes * styleFactor).toInt())
                    .coerceIn(15, prefs.maxDailyWorkoutMinutes.coerceAtLeast(20))
                candidates.add(
                    CandidateBlock(
                        title = "Steady Aerobic Training (${baseMins}m)",
                        module = "PHYSICAL",
                        preferredStartMinute = prefs.preferredWorkoutHour * 60,
                        durationMinutes = baseMins,
                        priorityWeight = 3,
                        priorityScore = 82,
                        priorityReason = "Readiness is ${input.recovery.state.code} (${input.recovery.readinessScore}/100)."
                    )
                )
            }
        }

        // 2. Academic Focus & Revision Blocks (respecting overdue revisions + upcoming exams)
        val studyTodayMins = input.studySessions.filter {
            Instant.ofEpochMilli(it.startEpochMs).atZone(input.zoneId).toLocalDate().toEpochDay() == targetEpochDay
        }.sumOf { it.durationMinutes }

        val overdueTopics = input.topics.filter { (it.nextRevisionDueEpochMs ?: Long.MAX_VALUE) <= input.nowEpochMs }
        if (overdueTopics.isNotEmpty()) {
            val topTopic = overdueTopics.sortedBy { it.confidenceEstimate }.first()
            candidates.add(
                CandidateBlock(
                    title = "Spaced Recall: ${topTopic.title}",
                    module = "ACADEMIC",
                    preferredStartMinute = prefs.preferredStudyHour * 60,
                    durationMinutes = 25,
                    priorityWeight = 4,
                    priorityScore = 90,
                    priorityReason = "${overdueTopics.size} topic(s) due for spaced recall; lowest confidence prioritized.",
                    linkedSourceType = ScheduledSourceType.ACADEMIC_REVISION,
                    linkedSourceId = topTopic.id
                )
            )
        }

        val upcomingExam = input.exams.filter { !it.isCompleted && it.examDateEpochMs >= input.nowEpochMs }
            .minByOrNull { it.examDateEpochMs }
        val remainingStudyNeed = (input.dailyAcademicGoalMinutes - studyTodayMins).coerceAtLeast(0)
        if (remainingStudyNeed >= 20 || upcomingExam != null) {
            val blockDuration = ((remainingStudyNeed.coerceAtLeast(30) * styleFactor).toInt())
                .coerceIn(25, prefs.maxDailyFocusMinutes.coerceAtMost(90))
            val examNote = upcomingExam?.let {
                val days = ExamCountdownCalculator.daysRemaining(it.examDateEpochMs, input.nowEpochMs).coerceAtLeast(0)
                "Exam '${it.title}' in ${days}d"
            }
            val examUrgencyBonus = if (upcomingExam != null) 12 else 0
            candidates.add(
                CandidateBlock(
                    title = upcomingExam?.let { "Exam Focus: ${it.subjectName}" } ?: "Deep Focus Study Block",
                    module = "ACADEMIC",
                    preferredStartMinute = (prefs.preferredStudyHour * 60 + 40).coerceAtMost(20 * 60),
                    durationMinutes = blockDuration,
                    priorityWeight = if (upcomingExam != null) 4 else 3,
                    priorityScore = (76 + examUrgencyBonus).coerceAtMost(98),
                    priorityReason = examNote ?: "Advances daily ${input.dailyAcademicGoalMinutes}m academic focus goal."
                )
            )
        }

        // 3. Pending / Overdue Personal Tasks (skip completed)
        val actionableTasks = input.tasks
            .filter {
                it.status != PersonalTaskStatus.COMPLETED &&
                    it.status != PersonalTaskStatus.ARCHIVED &&
                    (it.dueDateEpochDay == null || it.dueDateEpochDay <= targetEpochDay)
            }
            .sortedWith(
                compareByDescending<com.example.domain.model.PersonalTask> { it.isOverdue(targetEpochDay) }
                    .thenByDescending { it.priority.weight }
                    .thenBy { it.dueDateEpochDay ?: Long.MAX_VALUE }
            )
            .take(if (workload.workloadLevel == WorkloadLevel.OVERLOADED || isPartialDay) 1 else 2)

        actionableTasks.forEachIndexed { idx, task ->
            val overdueBonus = if (task.isOverdue(targetEpochDay)) 14 else 0
            val goalBonus = if (task.linkedGoalId != null) 8 else 0
            val score = (55 + task.priority.weight * 7 + overdueBonus + goalBonus).coerceIn(30, 99)
            candidates.add(
                CandidateBlock(
                    title = task.title,
                    module = "LIFE",
                    preferredStartMinute = task.scheduledStartMinute ?: (14 * 60 + idx * 50),
                    durationMinutes = task.estimatedMinutes.coerceIn(15, 60),
                    priorityWeight = task.priority.weight,
                    priorityScore = score,
                    priorityReason = if (task.isOverdue(targetEpochDay)) {
                        "Overdue ${task.priority.label} task — triaged cleanly into free window."
                    } else {
                        "${task.priority.label} priority commitment${if (task.linkedGoalId != null) " linked to active goal" else ""}."
                    },
                    linkedSourceType = ScheduledSourceType.PERSONAL_TASK,
                    linkedSourceId = task.id
                )
            )
        }

        // 4. Incomplete habits scheduled today (cap to avoid overload)
        val doneHabitIdsToday = input.habitCompletions
            .filter { it.dateEpochDay == targetEpochDay && (it.status == HabitDayStatus.COMPLETED || it.status == HabitDayStatus.SKIPPED) }
            .map { it.habitId }
            .toSet()
        val pendingHabit = input.habits.firstOrNull {
            !it.isArchived && it.isScheduledOn(dow) && it.id !in doneHabitIdsToday && !(it.isRestDayProtected && isConfiguredRestDay)
        }
        if (pendingHabit != null && workload.workloadLevel != WorkloadLevel.OVERLOADED) {
            candidates.add(
                CandidateBlock(
                    title = "Habit: ${pendingHabit.title}",
                    module = "DISCIPLINE",
                    preferredStartMinute = pendingHabit.preferredStartMinute ?: (19 * 60),
                    durationMinutes = 20,
                    priorityWeight = 2,
                    priorityScore = 64,
                    priorityReason = "Maintains scheduled habit cadence for ${dow.name.lowercase().replaceFirstChar { c -> c.uppercase() }}.",
                    linkedSourceType = ScheduledSourceType.HABIT,
                    linkedSourceId = pendingHabit.id
                )
            )
        }

        // Allocate into strict non-overlapping time windows around existingScheduledItems
        val minStartWindow = if (isPartialDay) currentMinuteOfDay.coerceIn(6 * 60, 21 * 60) else 6 * 60
        val maxEndWindow = 22 * 60 + 30
        val occupied = existingScheduledItems
            .filter { !it.isAllDay && it.dateEpochDay == targetEpochDay }
            .map { it.startMinuteOfDay to it.endMinuteOfDay }
            .toMutableList()

        val sortedCandidates = candidates.sortedByDescending { it.priorityScore }
        val maxBlocks = when {
            recoveryRestricted -> 3
            isPartialDay -> 3
            workload.workloadLevel == WorkloadLevel.OVERLOADED -> 3
            else -> 5
        }

        val placedBlocks = mutableListOf<AdaptivePlanBlock>()
        val maxTotalBudget = (prefs.maxDailyFocusMinutes + prefs.maxDailyWorkoutMinutes + 90).coerceAtLeast(120)
        var accumulatedMinutes = 0

        for (cand in sortedCandidates) {
            if (placedBlocks.size >= maxBlocks) break
            if (accumulatedMinutes + cand.durationMinutes > maxTotalBudget && placedBlocks.isNotEmpty()) continue

            val slotStart = findFreeSlotStart(
                preferredStart = cand.preferredStartMinute.coerceAtLeast(minStartWindow),
                duration = cand.durationMinutes,
                occupied = occupied,
                minStart = minStartWindow,
                maxEnd = maxEndWindow
            ) ?: continue

            val slotEnd = slotStart + cand.durationMinutes
            occupied.add(slotStart to (slotEnd + 10)) // 10m transition buffer
            accumulatedMinutes += cand.durationMinutes

            placedBlocks.add(
                AdaptivePlanBlock(
                    blockUuid = UUID.randomUUID().toString(),
                    planUuid = planUuid,
                    title = cand.title,
                    module = cand.module,
                    scheduledDateEpochDay = targetEpochDay,
                    startMinuteOfDay = slotStart,
                    durationMinutes = cand.durationMinutes,
                    priorityWeight = cand.priorityWeight,
                    priorityScore = cand.priorityScore,
                    priorityReason = cand.priorityReason,
                    dependencies = cand.dependencies,
                    status = AdaptivePlanBlockStatus.PENDING,
                    linkedSourceType = cand.linkedSourceType,
                    linkedSourceId = cand.linkedSourceId,
                    isRecoveryProtected = cand.isRecoveryProtected
                )
            )
        }

        val orderedBlocks = placedBlocks.sortedBy { it.startMinuteOfDay }
        val availableMinutes = computeAvailableMinutes(occupied, minStartWindow, maxEndWindow)

        return AdaptiveDailyPlan(
            planUuid = planUuid,
            targetDateEpochDay = targetEpochDay,
            generatedAtEpochMs = input.nowEpochMs,
            workloadLevel = workload.workloadLevel,
            totalPlannedMinutes = orderedBlocks.sumOf { it.durationMinutes },
            availableMinutes = availableMinutes,
            isApproved = false,
            isPartialDay = isPartialDay,
            notes = workload.adjustmentDirective,
            blocks = orderedBlocks
        )
    }

    fun rescheduleBlock(
        block: AdaptivePlanBlock,
        newStartMinute: Int,
        otherBlocks: List<AdaptivePlanBlock>,
        existingScheduledItems: List<ScheduledActivityItem>
    ): AdaptivePlanBlock {
        val occupied = (
            existingScheduledItems
                .filter { !it.isAllDay && it.dateEpochDay == block.scheduledDateEpochDay }
                .map { it.startMinuteOfDay to it.endMinuteOfDay } +
                otherBlocks
                    .filter { it.blockUuid != block.blockUuid && it.scheduledDateEpochDay == block.scheduledDateEpochDay }
                    .map { it.startMinuteOfDay to it.endMinuteOfDay }
            )
        val safeStart = findFreeSlotStart(
            preferredStart = newStartMinute.coerceIn(6 * 60, 22 * 60),
            duration = block.durationMinutes,
            occupied = occupied,
            minStart = 6 * 60,
            maxEnd = 23 * 60
        ) ?: newStartMinute.coerceIn(6 * 60, 22 * 60)

        return block.copy(
            startMinuteOfDay = safeStart,
            status = AdaptivePlanBlockStatus.RESCHEDULED
        )
    }

    fun recordPartialCompletion(
        block: AdaptivePlanBlock,
        completedMinutes: Int
    ): AdaptivePlanBlock {
        val clamped = completedMinutes.coerceIn(0, block.durationMinutes)
        val status = when {
            clamped >= block.durationMinutes -> AdaptivePlanBlockStatus.COMPLETED
            clamped > 0 -> AdaptivePlanBlockStatus.PARTIAL
            else -> AdaptivePlanBlockStatus.SKIPPED
        }
        return block.copy(completedMinutes = clamped, status = status)
    }

    private fun findFreeSlotStart(
        preferredStart: Int,
        duration: Int,
        occupied: List<Pair<Int, Int>>,
        minStart: Int,
        maxEnd: Int
    ): Int? {
        val clampedPref = preferredStart.coerceIn(minStart, (maxEnd - duration).coerceAtLeast(minStart))
        if (!hasOverlap(clampedPref, clampedPref + duration, occupied)) {
            return clampedPref
        }
        var cursor = minStart
        while (cursor + duration <= maxEnd) {
            val clash = occupied.firstOrNull { (s, e) ->
                minOf(cursor + duration, e) > maxOf(cursor, s)
            }
            if (clash == null) return cursor
            cursor = (clash.second + 5).coerceAtLeast(cursor + 15)
        }
        return null
    }

    private fun hasOverlap(start: Int, end: Int, occupied: List<Pair<Int, Int>>): Boolean =
        occupied.any { (s, e) -> minOf(end, e) > maxOf(start, s) }

    private fun computeAvailableMinutes(occupied: List<Pair<Int, Int>>, start: Int, end: Int): Int {
        val totalSpan = (end - start).coerceAtLeast(0)
        val used = occupied.sumOf { (s, e) -> (minOf(e, end) - maxOf(s, start)).coerceAtLeast(0) }
        return (totalSpan - used).coerceAtLeast(0)
    }
}
