package com.example.domain.engine

import com.example.domain.model.AcademicExam
import com.example.domain.model.AcademicTopic
import com.example.domain.model.GoalMilestone
import com.example.domain.model.Habit
import com.example.domain.model.HabitCompletionRecord
import com.example.domain.model.HabitDayStatus
import com.example.domain.model.LifeRoutine
import com.example.domain.model.NotificationCategory
import com.example.domain.model.PersonalTask
import com.example.domain.model.PersonalTaskStatus
import com.example.domain.model.PhysicalDevelopmentState
import com.example.domain.model.Quest
import com.example.domain.model.QuestType
import com.example.domain.model.RecoveryAssessment
import com.example.domain.model.RecoveryState
import com.example.domain.model.SystemNotification
import com.example.domain.model.SystemNotificationPreferences
import com.example.domain.model.WorkoutStatus
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class PlannedSystemNotification(
    val title: String,
    val message: String,
    val category: NotificationCategory,
    val dedupKey: String,
    val deepLinkRoute: String
)

/**
 * Time-zone aware, quiet-hours respecting, spam-guarded System Notification Engine.
 * Critical recovery warnings are never overridden or suppressed by AI motivational messages.
 */
object NotificationSchedulerEngine {

    fun isInQuietHours(
        nowEpochMs: Long,
        quietStartHour: Int,
        quietEndHour: Int,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Boolean {
        val start = quietStartHour.coerceIn(0, 23)
        val end = quietEndHour.coerceIn(0, 23)
        if (start == end) return false
        val hour = Instant.ofEpochMilli(nowEpochMs).atZone(zoneId).hour
        return if (start < end) {
            hour in start until end
        } else {
            hour >= start || hour < end
        }
    }

    fun evaluatePendingNotifications(
        prefs: SystemNotificationPreferences,
        physical: PhysicalDevelopmentState,
        recovery: RecoveryAssessment,
        pendingDailyQuests: List<Quest>,
        topics: List<AcademicTopic>,
        exams: List<AcademicExam>,
        existingNotifications: List<SystemNotification>,
        nowEpochMs: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
        habits: List<Habit> = emptyList(),
        habitCompletionsToday: List<HabitCompletionRecord> = emptyList(),
        routines: List<LifeRoutine> = emptyList(),
        tasks: List<PersonalTask> = emptyList(),
        goalMilestones: List<GoalMilestone> = emptyList()
    ): List<PlannedSystemNotification> {
        if (!prefs.notificationsEnabled) return emptyList()

        val today: LocalDate = Instant.ofEpochMilli(nowEpochMs).atZone(zoneId).toLocalDate()
        val todayStartMs = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val sentToday = existingNotifications.filter { it.timestampEpochMs >= todayStartMs }
        val remainingQuota = (prefs.maxNotificationsPerDay - sentToday.size).coerceAtLeast(0)

        val inQuietHours = isInQuietHours(nowEpochMs, prefs.quietHoursStartHour, prefs.quietHoursEndHour, zoneId)
        val candidates = mutableListOf<PlannedSystemNotification>()

        // 1. Critical recovery warning takes highest precedence & cannot be suppressed by AI messages
        if (prefs.enableRecoveryWarnings &&
            (recovery.state == RecoveryState.REST_RECOMMENDED || recovery.state == RecoveryState.RECOVERY_RECOMMENDED || recovery.selfReportedDiscomfort)
        ) {
            candidates.add(
                PlannedSystemNotification(
                    title = "Recovery Warning (${recovery.state.code})",
                    message = recovery.summaryMessage,
                    category = NotificationCategory.RECOVERY_WARNING,
                    dedupKey = "recovery_${today}_${recovery.state.name}",
                    deepLinkRoute = "physical"
                )
            )
        }

        if (inQuietHours) {
            // During quiet hours only critical recovery warnings pass if quota allows (or 1 urgent safety notice)
            return deduplicateAgainstRecent(candidates, sentToday).take(1)
        }

        if (remainingQuota == 0) return emptyList()

        // 2. Overdue revisions
        if (prefs.enableOverdueRevisions) {
            val overdueCount = topics.count { topic ->
                val due = topic.nextRevisionDueEpochMs ?: nowEpochMs
                Instant.ofEpochMilli(due).atZone(zoneId).toLocalDate().isBefore(today)
            }
            if (overdueCount > 0) {
                candidates.add(
                    PlannedSystemNotification(
                        title = "Overdue Revisions ($overdueCount)",
                        message = "$overdueCount topic(s) need spaced recall review to protect estimated familiarity.",
                        category = NotificationCategory.OVERDUE_REVISION,
                        dedupKey = "overdue_rev_$today",
                        deepLinkRoute = "academic_revisions"
                    )
                )
            }
        }

        // 3. Upcoming exams within 7 days
        if (prefs.enableUpcomingExams) {
            val urgentExam = exams
                .filter { !it.isCompleted }
                .map { it to ExamCountdownCalculator.daysRemaining(it.examDateEpochMs, nowEpochMs) }
                .filter { it.second in 0L..7L }
                .minByOrNull { it.second }
            if (urgentExam != null) {
                val (exam, days) = urgentExam
                candidates.add(
                    PlannedSystemNotification(
                        title = "Upcoming Exam: ${exam.title} (${days}d)",
                        message = "${exam.subjectName} examination is in $days day(s). Check Readiness & Prep Quests.",
                        category = NotificationCategory.UPCOMING_EXAM,
                        dedupKey = "exam_${exam.id}_$today",
                        deepLinkRoute = "academic_exams"
                    )
                )
            }
        }

        // 4. Daily quest reminders
        if (prefs.enableDailyQuestReminders) {
            val pendingDaily = pendingDailyQuests.count { it.type == QuestType.DAILY && !it.isCompleted }
            if (pendingDaily > 0) {
                candidates.add(
                    PlannedSystemNotification(
                        title = "Pending Daily Quests ($pendingDaily)",
                        message = "$pendingDaily daily quest(s) remain incomplete today.",
                        category = NotificationCategory.DAILY_QUEST_REMINDER,
                        dedupKey = "daily_quest_$today",
                        deepLinkRoute = "quest_board"
                    )
                )
            }
        }

        // 5. Workout reminder (ONLY when recovery state allows training!)
        if (prefs.enableWorkoutReminders &&
            recovery.state != RecoveryState.REST_RECOMMENDED &&
            recovery.state != RecoveryState.RECOVERY_RECOMMENDED &&
            !recovery.selfReportedDiscomfort &&
            physical.workoutStatus == WorkoutStatus.PENDING &&
            physical.completedJoggingMinutesToday < physical.effectiveJoggingTarget
        ) {
            candidates.add(
                PlannedSystemNotification(
                    title = "Daily Training Window Open",
                    message = "Daily jogging progress: ${physical.completedJoggingMinutesToday}/${physical.effectiveJoggingTarget} min.",
                    category = NotificationCategory.WORKOUT_REMINDER,
                    dedupKey = "workout_$today",
                    deepLinkRoute = "physical"
                )
            )
        }

        val todayEpochDay = today.toEpochDay()
        val zdt = Instant.ofEpochMilli(nowEpochMs).atZone(zoneId)
        val hourOfDay = zdt.hour

        // 6. Overdue personal tasks
        if (prefs.enableOverdueTasks) {
            val overdueTasks = tasks.filter { it.isOverdue(todayEpochDay) }
            if (overdueTasks.isNotEmpty()) {
                candidates.add(
                    PlannedSystemNotification(
                        title = "Overdue Personal Tasks (${overdueTasks.size})",
                        message = "${overdueTasks.size} task(s) need attention or rescheduling: ${overdueTasks.first().title}.",
                        category = NotificationCategory.OVERDUE_TASK,
                        dedupKey = "overdue_tasks_$today",
                        deepLinkRoute = "tasks"
                    )
                )
            }
        }

        // 7. Personal task / mission due today or within 24h
        if (prefs.enableTaskDeadlines) {
            val dueSoon = tasks.filter {
                it.status != PersonalTaskStatus.COMPLETED &&
                    it.status != PersonalTaskStatus.ARCHIVED &&
                    it.dueDateEpochDay != null &&
                    it.dueDateEpochDay in todayEpochDay..(todayEpochDay + 1L)
            }
            if (dueSoon.isNotEmpty()) {
                val dueTodayCount = dueSoon.count { it.dueDateEpochDay == todayEpochDay }
                val header = if (dueTodayCount > 0) "Missions Due Today ($dueTodayCount)" else "Approaching Deadlines (${dueSoon.size})"
                candidates.add(
                    PlannedSystemNotification(
                        title = header,
                        message = "${dueSoon.first().title}${if (dueSoon.size > 1) " (+${dueSoon.size - 1} more)" else ""} scheduled soon.",
                        category = NotificationCategory.TASK_DEADLINE,
                        dedupKey = "tasks_due_$today",
                        deepLinkRoute = "mission_control"
                    )
                )
            }
        }

        // 8. Habit reminder
        if (prefs.enableHabitReminders) {
            val loggedHabitIds = habitCompletionsToday
                .filter { it.dateEpochDay == todayEpochDay && (it.status == HabitDayStatus.COMPLETED || it.status == HabitDayStatus.SKIPPED) }
                .map { it.habitId }
                .toSet()
            val pendingHabits = habits.filter { !it.isArchived && it.reminderEnabled && it.isScheduledOn(today.dayOfWeek) && it.id !in loggedHabitIds }
            if (pendingHabits.isNotEmpty()) {
                candidates.add(
                    PlannedSystemNotification(
                        title = "Pending Daily Habits (${pendingHabits.size})",
                        message = "Scheduled today: ${pendingHabits.take(2).joinToString(", ") { it.title }}.",
                        category = NotificationCategory.HABIT_REMINDER,
                        dedupKey = "habit_rem_$today",
                        deepLinkRoute = "habits"
                    )
                )
            }
        }

        // 9. Routine reminder
        if (prefs.enableRoutineReminders) {
            val upcomingRoutine = routines
                .filter { it.isEnabled && it.reminderEnabled && today.dayOfWeek in it.scheduledDays }
                .firstOrNull { (it.startMinuteOfDay / 60) >= hourOfDay }
            if (upcomingRoutine != null) {
                candidates.add(
                    PlannedSystemNotification(
                        title = "Routine Window: ${upcomingRoutine.title}",
                        message = "${upcomingRoutine.timeSlot.label} protocol (${upcomingRoutine.targetDurationMinutes}m).",
                        category = NotificationCategory.ROUTINE_REMINDER,
                        dedupKey = "routine_${upcomingRoutine.id}_$today",
                        deepLinkRoute = "routines"
                    )
                )
            }
        }

        // 10. Upcoming goal milestone deadline (within 3 days)
        if (prefs.enableGoalMilestones) {
            val urgentMilestone = goalMilestones
                .filter { !it.isCompleted && it.dueDateEpochDay != null && it.dueDateEpochDay in todayEpochDay..(todayEpochDay + 3L) }
                .minByOrNull { it.dueDateEpochDay ?: Long.MAX_VALUE }
            if (urgentMilestone != null) {
                candidates.add(
                    PlannedSystemNotification(
                        title = "Goal Milestone Due Soon",
                        message = "'${urgentMilestone.title}' target falls within 3 days.",
                        category = NotificationCategory.GOAL_MILESTONE,
                        dedupKey = "milestone_${urgentMilestone.id}_$today",
                        deepLinkRoute = "mission_control"
                    )
                )
            }
        }

        // 11. Daily Planning Reminder (morning window)
        if (prefs.enableDailyPlanningReminders && hourOfDay in 6..10) {
            candidates.add(
                PlannedSystemNotification(
                    title = "Daily Schedule Briefing",
                    message = "Review today's missions, resolve conflicts, and lock keystone habits.",
                    category = NotificationCategory.DAILY_PLANNING_REMINDER,
                    dedupKey = "daily_plan_$today",
                    deepLinkRoute = "mission_control"
                )
            )
        }

        // 12. Weekly Intelligence & Review Reminder (Sunday afternoon/evening)
        if (prefs.enableWeeklyReviewReminders && today.dayOfWeek == DayOfWeek.SUNDAY && hourOfDay >= 16) {
            candidates.add(
                PlannedSystemNotification(
                    title = "Weekly Intelligence Report Available",
                    message = "Inspect your 7-day performance comparison, goal pace, and recalibrate next week.",
                    category = NotificationCategory.WEEKLY_REVIEW_REMINDER,
                    dedupKey = "weekly_review_$today",
                    deepLinkRoute = "weekly_intelligence"
                )
            )
        }

        return deduplicateAgainstRecent(candidates, sentToday).take(remainingQuota)
    }

    private fun deduplicateAgainstRecent(
        candidates: List<PlannedSystemNotification>,
        sentToday: List<SystemNotification>
    ): List<PlannedSystemNotification> {
        return candidates.filter { cand ->
            sentToday.none { existing ->
                existing.category == cand.category && existing.title == cand.title
            }
        }
    }
}
