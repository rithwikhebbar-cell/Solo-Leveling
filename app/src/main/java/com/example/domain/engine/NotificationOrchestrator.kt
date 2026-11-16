package com.example.domain.engine

import com.example.domain.model.AcademicExam
import com.example.domain.model.AcademicTopic
import com.example.domain.model.GoalMilestone
import com.example.domain.model.Habit
import com.example.domain.model.HabitCompletionRecord
import com.example.domain.model.LifeRoutine
import com.example.domain.model.NotificationCategory
import com.example.domain.model.PersonalTask
import com.example.domain.model.PhysicalDevelopmentState
import com.example.domain.model.Quest
import com.example.domain.model.RecoveryAssessment
import com.example.domain.model.SystemNotification
import com.example.domain.model.SystemNotificationPreferences
import java.time.ZoneId

object QuietHoursManager {
    fun isQuietNow(prefs: SystemNotificationPreferences, nowEpochMs: Long, zoneId: ZoneId): Boolean =
        NotificationSchedulerEngine.isInQuietHours(
            nowEpochMs = nowEpochMs,
            quietStartHour = prefs.quietHoursStartHour,
            quietEndHour = prefs.quietHoursEndHour,
            zoneId = zoneId
        )
}

object NotificationPriorityEngine {
    fun priorityOf(category: NotificationCategory): Int = when (category) {
        NotificationCategory.RECOVERY_WARNING -> 100
        NotificationCategory.OVERDUE_TASK, NotificationCategory.UPCOMING_EXAM -> 85
        NotificationCategory.OVERDUE_REVISION, NotificationCategory.GOAL_MILESTONE -> 75
        NotificationCategory.DAILY_QUEST_REMINDER, NotificationCategory.TASK_DEADLINE -> 65
        NotificationCategory.WORKOUT_REMINDER, NotificationCategory.HABIT_REMINDER, NotificationCategory.ROUTINE_REMINDER -> 55
        NotificationCategory.SYSTEM_ALERT, NotificationCategory.BOSS_PHASE, NotificationCategory.DUNGEON_UPDATE -> 50
        NotificationCategory.DAILY_PLANNING_REMINDER, NotificationCategory.WEEKLY_REVIEW_REMINDER, NotificationCategory.DAILY_SUMMARY -> 40
        NotificationCategory.LEVEL_UP, NotificationCategory.RANK_PROMOTION, NotificationCategory.ACHIEVEMENT,
        NotificationCategory.SKILL_UNLOCKED, NotificationCategory.QUEST_UPDATE, NotificationCategory.AI_RECOMMENDATION -> 25
    }
}

object NotificationDeduplicationManager {
    fun filterDuplicates(
        candidates: List<PlannedSystemNotification>,
        existingNotifications: List<SystemNotification>,
        dedupWindowMs: Long = 12 * 3_600_000L,
        nowEpochMs: Long = System.currentTimeMillis()
    ): List<PlannedSystemNotification> {
        val recent = existingNotifications.filter { nowEpochMs - it.timestampEpochMs <= dedupWindowMs }
        val seenKeys = mutableSetOf<String>()
        return candidates.filter { cand ->
            val uniqueInBatch = seenKeys.add(cand.dedupKey)
            val matchesRecent = recent.any { it.category == cand.category && it.title == cand.title }
            uniqueInBatch && !matchesRecent
        }
    }
}

/**
 * Coordinates Priority, Quiet Hours, Deduplication, Daily Quota, and cross-module planning
 * on top of NotificationSchedulerEngine.
 */
object NotificationOrchestrator {

    fun planNotifications(
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
        val rawPlanned = NotificationSchedulerEngine.evaluatePendingNotifications(
            prefs = prefs,
            physical = physical,
            recovery = recovery,
            pendingDailyQuests = pendingDailyQuests,
            topics = topics,
            exams = exams,
            existingNotifications = existingNotifications,
            nowEpochMs = nowEpochMs,
            zoneId = zoneId,
            habits = habits,
            habitCompletionsToday = habitCompletionsToday,
            routines = routines,
            tasks = tasks,
            goalMilestones = goalMilestones
        )
        val deduplicated = NotificationDeduplicationManager.filterDuplicates(
            candidates = rawPlanned,
            existingNotifications = existingNotifications,
            nowEpochMs = nowEpochMs
        )
        return deduplicated.sortedByDescending { NotificationPriorityEngine.priorityOf(it.category) }
    }
}
