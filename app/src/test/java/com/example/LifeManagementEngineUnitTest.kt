package com.example

import com.example.domain.engine.DisciplineEngine
import com.example.domain.engine.GoalEngine
import com.example.domain.engine.HabitEngine
import com.example.domain.engine.NotificationSchedulerEngine
import com.example.domain.engine.PersonalTaskEngine
import com.example.domain.engine.ProgressionEngine
import com.example.domain.engine.RoutineEngine
import com.example.domain.engine.ScheduleCalendarEngine
import com.example.domain.engine.StreakEngine
import com.example.domain.engine.TaskSortOption
import com.example.domain.model.AcademicExam
import com.example.domain.model.AttributeType
import com.example.domain.model.AvailabilityWindow
import com.example.domain.model.GoalMilestone
import com.example.domain.model.GoalStatus
import com.example.domain.model.Habit
import com.example.domain.model.HabitCategory
import com.example.domain.model.HabitCompletionRecord
import com.example.domain.model.HabitDayStatus
import com.example.domain.model.HabitType
import com.example.domain.model.JoggingSession
import com.example.domain.model.LifeActivityKind
import com.example.domain.model.LifeGoal
import com.example.domain.model.LifeRoutine
import com.example.domain.model.NotificationCategory
import com.example.domain.model.PersonalTask
import com.example.domain.model.PersonalTaskStatus
import com.example.domain.model.PhysicalActivityKind
import com.example.domain.model.PhysicalDevelopmentState
import com.example.domain.model.RecoveryAssessment
import com.example.domain.model.RecoveryState
import com.example.domain.model.RoutineActivity
import com.example.domain.model.RoutineActivityStatus
import com.example.domain.model.RoutineCompletionRecord
import com.example.domain.model.RoutineTimeSlot
import com.example.domain.model.ScheduledSourceType
import com.example.domain.model.StudySession
import com.example.domain.model.SystemNotificationPreferences
import com.example.domain.model.TaskCategory
import com.example.domain.model.TaskPriority
import com.example.domain.model.TaskRecurrenceConfig
import com.example.domain.model.TaskRecurrenceType
import com.example.domain.model.VerificationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class LifeManagementEngineUnitTest {

    @Test
    fun `routine engine checks weekday scheduling and computes 30d consistency excluding rest skips`() {
        val monday = LocalDate.of(2026, 6, 15) // Monday
        val mondayEpoch = monday.toEpochDay()
        val sundayEpoch = mondayEpoch - 1L
        val saturdayEpoch = mondayEpoch - 2L

        val routine = LifeRoutine(
            id = 1L,
            title = "Morning Protocol",
            timeSlot = RoutineTimeSlot.MORNING,
            startMinuteOfDay = 7 * 60,
            scheduledDays = setOf(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SUNDAY
            ),
            isRestExemptAllowed = true
        )
        assertTrue(RoutineEngine.isScheduledOn(routine, mondayEpoch))
        assertFalse(RoutineEngine.isScheduledOn(routine, saturdayEpoch))

        val activities = listOf(
            RoutineActivity(id = 10L, routineId = 1L, title = "Hydrate", durationMinutes = 5, orderIndex = 0),
            RoutineActivity(id = 11L, routineId = 1L, title = "Journal", durationMinutes = 10, orderIndex = 1)
        )
        val completions = listOf(
            // Sunday skipped on rest-exempt routine -> excluded from penalty
            RoutineCompletionRecord(routineId = 1L, activityId = 10L, dateEpochDay = sundayEpoch, status = RoutineActivityStatus.SKIPPED),
            // Today (Monday) completed
            RoutineCompletionRecord(routineId = 1L, activityId = 10L, dateEpochDay = mondayEpoch, status = RoutineActivityStatus.COMPLETED),
            RoutineCompletionRecord(routineId = 1L, activityId = 11L, dateEpochDay = mondayEpoch, status = RoutineActivityStatus.COMPLETED)
        )

        val summary = RoutineEngine.summarizeRoutineForDay(
            routine = routine,
            activities = activities,
            completions = completions,
            targetEpochDay = mondayEpoch,
            exemptRestDays = setOf(DayOfWeek.SUNDAY)
        )
        assertEquals(2, summary.completedCount)
        assertEquals(1f, summary.completionFraction, 0.001f)
        assertEquals(1, summary.currentStreakDays)
        assertEquals(0, summary.missedRequiredCount)
    }

    @Test
    fun `habit and streak engines support partial skip and rest-day exemption with capped bonus`() {
        val wednesday = LocalDate.of(2026, 6, 17) // Wed
        val wedEpoch = wednesday.toEpochDay()
        val tueEpoch = wedEpoch - 1L
        val monEpoch = wedEpoch - 2L
        val sunEpoch = wedEpoch - 3L

        val habit = Habit(
            id = 5L,
            title = "Deep Reading",
            habitType = HabitType.DURATION,
            category = HabitCategory.LEARNING,
            targetValue = 30.0,
            unit = "min",
            scheduledDays = DayOfWeek.entries.toSet(),
            isRestDayProtected = true
        )
        assertEquals(HabitDayStatus.COMPLETED, HabitEngine.evaluateLoggedStatus(habit, 30.0))
        assertEquals(HabitDayStatus.PARTIAL, HabitEngine.evaluateLoggedStatus(habit, 15.0))
        assertEquals(HabitDayStatus.MISSED, HabitEngine.evaluateLoggedStatus(habit, 0.0))

        val records = mapOf(
            // Sunday has no completion, but Sunday is an exempt rest day -> streak preserved
            monEpoch to HabitCompletionRecord(habitId = 5L, dateEpochDay = monEpoch, status = HabitDayStatus.COMPLETED, achievedValue = 30.0),
            tueEpoch to HabitCompletionRecord(habitId = 5L, dateEpochDay = tueEpoch, status = HabitDayStatus.SKIPPED, skipReason = "Recovery"),
            wedEpoch to HabitCompletionRecord(habitId = 5L, dateEpochDay = wedEpoch, status = HabitDayStatus.COMPLETED, achievedValue = 35.0)
        )

        val streak = StreakEngine.evaluateHabitStreak(
            habit = habit,
            recordsByDay = records,
            todayEpochDay = wedEpoch,
            exemptRestDays = setOf(DayOfWeek.SUNDAY)
        )
        assertEquals(2, streak.currentStreakDays)
        assertTrue(streak.exemptRestDaysCount >= 2) // skipped + rest-day Sunday
        assertTrue(streak.streakCapBonusPercent <= 20)

        // Verify streak cap at 20% even for long streak
        val longMap = (0L..25L).associate { offset ->
            val d = wedEpoch - offset
            d to HabitCompletionRecord(habitId = 5L, dateEpochDay = d, status = HabitDayStatus.COMPLETED, achievedValue = 30.0)
        }
        val cappedStreak = StreakEngine.evaluateHabitStreak(habit, longMap, wedEpoch)
        assertEquals(20, cappedStreak.streakCapBonusPercent)
    }

    @Test
    fun `personal task engine computes recurrence dates and filters sorts accurately`() {
        val friday = LocalDate.of(2026, 6, 19) // Friday
        val friEpoch = friday.toEpochDay()

        val weekdayTask = PersonalTask(
            id = 1L,
            title = "Submit Invoices",
            category = TaskCategory.FINANCIAL,
            priority = TaskPriority.HIGH,
            dueDateEpochDay = friEpoch,
            recurrence = TaskRecurrenceConfig(taskId = 1L, recurrenceType = TaskRecurrenceType.WEEKDAYS)
        )
        val nextDue = PersonalTaskEngine.computeNextDueDate(weekdayTask, friEpoch)
        assertNotNull(nextDue)
        assertEquals(DayOfWeek.MONDAY, LocalDate.ofEpochDay(nextDue!!).dayOfWeek)

        val endCappedTask = weekdayTask.copy(
            recurrence = TaskRecurrenceConfig(
                taskId = 1L,
                recurrenceType = TaskRecurrenceType.DAILY,
                endDateEpochDay = friEpoch
            )
        )
        assertNull(PersonalTaskEngine.computeNextDueDate(endCappedTask, friEpoch))

        val tasks = listOf(
            weekdayTask,
            PersonalTask(id = 2L, title = "Low Errand", category = TaskCategory.HOUSEHOLD, priority = TaskPriority.LOW, dueDateEpochDay = friEpoch + 3),
            PersonalTask(id = 3L, title = "Critical Tax Filing", category = TaskCategory.ADMINISTRATIVE, priority = TaskPriority.CRITICAL, dueDateEpochDay = friEpoch + 1)
        )
        val sortedByPriority = PersonalTaskEngine.filterAndSort(tasks, sortOption = TaskSortOption.PRIORITY)
        assertEquals(3L, sortedByPriority.first().id)

        val searched = PersonalTaskEngine.filterAndSort(tasks, searchQuery = "tax")
        assertEquals(listOf(3L), searched.map { it.id })
    }

    @Test
    fun `goal engine computes weighted milestone progress and behind schedule status cleanly`() {
        val start = 20_000L
        val target = 20_040L
        val goal = LifeGoal(
            id = 1L,
            title = "Build Emergency Reserve",
            startDateEpochDay = start,
            targetDateEpochDay = target,
            status = GoalStatus.ACTIVE
        )
        val milestones = listOf(
            GoalMilestone(id = 1L, goalId = 1L, title = "Audit Expenses", weight = 1, isCompleted = true),
            GoalMilestone(id = 2L, goalId = 1L, title = "Save Month 1–3", weight = 3, isCompleted = false)
        )
        val progress = GoalEngine.computeProgressPercent(goal, milestones, emptyList())
        assertEquals(25, progress) // 1 / 4 weighted = 25%

        // At midpoint (day 20020 => 50% elapsed), 25% vs 50% -> delta 25 > 20 -> behind schedule
        assertTrue(GoalEngine.isBehindSchedule(goal, progress, start + 20L))
        // Early in timeline (day 20004 => 10% elapsed) -> on track
        assertFalse(GoalEngine.isBehindSchedule(goal, progress, start + 4L))
    }

    @Test
    fun `schedule calendar engine merges academic and physical by reference detects overlaps and suggests open slots`() {
        val zone = ZoneId.of("UTC")
        val targetDate = LocalDate.of(2026, 6, 18)
        val targetDay = targetDate.toEpochDay()
        val nineAmMs = ZonedDateTime.of(2026, 6, 18, 9, 0, 0, 0, zone).toInstant().toEpochMilli()
        val nineFifteenMs = ZonedDateTime.of(2026, 6, 18, 9, 15, 0, 0, zone).toInstant().toEpochMilli()

        val task = PersonalTask(
            id = 42L,
            title = "Pay Tuition",
            category = TaskCategory.ADMINISTRATIVE,
            priority = TaskPriority.HIGH,
            dueDateEpochDay = targetDay,
            scheduledStartMinute = 9 * 60,
            estimatedMinutes = 45
        )
        val study = StudySession(
            id = 7L,
            sourceId = "study_7",
            subjectId = 1L,
            subjectName = "Algorithms",
            title = "DP Practice",
            startEpochMs = nineFifteenMs,
            endEpochMs = nineFifteenMs + 60 * 60_000L,
            durationMinutes = 60,
            verificationStatus = VerificationStatus.VERIFIED_SYSTEM_TIMER,
            xpAwarded = 45
        )
        val jog = JoggingSession(
            id = 3L,
            sourceId = "jog_3",
            originApp = "Test",
            title = "Morning Run",
            activityKind = PhysicalActivityKind.JOGGING,
            startEpochMs = nineAmMs - 2 * 60 * 60_000L, // 07:00
            endEpochMs = nineAmMs - 90 * 60_000L,
            durationMinutes = 30,
            verificationStatus = VerificationStatus.SELF_REPORTED,
            xpAwarded = 40
        )
        val exam = AcademicExam(
            id = 9L,
            subjectId = 1L,
            subjectName = "Algorithms",
            title = "Midterm",
            examDateEpochMs = ZonedDateTime.of(2026, 6, 18, 14, 0, 0, 0, zone).toInstant().toEpochMilli()
        )

        val unified = ScheduleCalendarEngine.buildUnifiedItemsForDay(
            targetEpochDay = targetDay,
            routines = emptyList(),
            routineActivities = emptyList(),
            routineCompletions = emptyList(),
            habits = emptyList(),
            habitCompletions = emptyList(),
            tasks = listOf(task),
            goals = emptyList(),
            goalMilestones = emptyList(),
            storedReferences = emptyList(),
            studySessions = listOf(study),
            exams = listOf(exam),
            jogSessions = listOf(jog),
            todayEpochDay = targetDay,
            zoneId = zone
        )

        assertEquals(4, unified.size)
        val externalStudy = unified.first { it.sourceType == ScheduledSourceType.ACADEMIC_STUDY }
        assertTrue(externalStudy.isReadOnlyExternal)

        val conflicts = ScheduleCalendarEngine.detectConflicts(unified)
        assertEquals(1, conflicts.size) // 09:00-09:45 overlaps 09:15-10:15 by 30m
        assertEquals(30, conflicts.first().overlapMinutes)

        val suggestions = ScheduleCalendarEngine.suggestAlternativeSlots(
            durationMinutes = 45,
            targetEpochDay = targetDay,
            existingItems = unified,
            availability = AvailabilityWindow(startMinuteOfDay = 8 * 60, endMinuteOfDay = 18 * 60, zoneId = zone)
        )
        assertTrue(suggestions.isNotEmpty())
        assertTrue(suggestions.none { it.startMinuteOfDay in (9 * 60) until (10 * 60 + 15) })
    }

    @Test
    fun `progression engine enforces life xp cap self-reported limit and attribute mapping`() {
        val taskReward = ProgressionEngine.calculateLifeDisciplineReward(
            kind = LifeActivityKind.PERSONAL_TASK,
            verificationStatus = VerificationStatus.SELF_REPORTED,
            lifeXpAlreadyEarnedToday = 0,
            selfReportedLifeCountToday = 0,
            taskCategory = TaskCategory.SOCIAL
        )
        assertEquals(AttributeType.CHA, taskReward.primaryAttribute)
        assertEquals(AttributeType.DIS, taskReward.secondaryAttribute)
        assertTrue(taskReward.xpAwarded > 0)

        val cappedByDailyXp = ProgressionEngine.calculateLifeDisciplineReward(
            kind = LifeActivityKind.GOAL_MILESTONE,
            verificationStatus = VerificationStatus.SELF_REPORTED,
            lifeXpAlreadyEarnedToday = ProgressionEngine.DAILY_LIFE_XP_CAP,
            selfReportedLifeCountToday = 1
        )
        assertEquals(0, cappedByDailyXp.xpAwarded)
        assertTrue(cappedByDailyXp.wasCapped)

        val cappedBySelfReportCount = ProgressionEngine.calculateLifeDisciplineReward(
            kind = LifeActivityKind.HABIT_COMPLETION,
            verificationStatus = VerificationStatus.SELF_REPORTED,
            lifeXpAlreadyEarnedToday = 20,
            selfReportedLifeCountToday = ProgressionEngine.DAILY_SELF_REPORTED_LIFE_LIMIT
        )
        assertEquals(0, cappedBySelfReportCount.xpAwarded)
        assertTrue(cappedBySelfReportCount.wasCapped)
    }

    @Test
    fun `discipline engine and notification scheduler evaluate scores and respect quiet hours`() {
        val assessment = DisciplineEngine.assessDay(
            targetEpochDay = 20_600L,
            routineSummaries = emptyList(),
            habitSummaries = emptyList(),
            tasks = listOf(
                PersonalTask(id = 1L, title = "Cleared", status = PersonalTaskStatus.COMPLETED, dueDateEpochDay = 20_600L)
            ),
            goalsWithDetails = emptyList(),
            scheduledItemsToday = emptyList(),
            conflictsToday = emptyList()
        )
        assertEquals(100, assessment.taskCompletionPercent)
        assertEquals(100, assessment.disciplineScore)

        val zone = ZoneId.of("UTC")
        val sundayAfternoonMs = ZonedDateTime.of(2026, 6, 21, 17, 0, 0, 0, zone).toInstant().toEpochMilli() // Sunday 17:00
        val sundayEpoch = LocalDate.of(2026, 6, 21).toEpochDay()

        val prefs = SystemNotificationPreferences(
            notificationsEnabled = true,
            quietHoursStartHour = 22,
            quietHoursEndHour = 7,
            maxNotificationsPerDay = 10
        )
        val recoveryReady = RecoveryAssessment(
            state = RecoveryState.READY,
            readinessScore = 90,
            measuredLoadMinutes48h = 30,
            measuredLoadMinutes7d = 90,
            measuredSessions7d = 3,
            consecutiveActiveDays = 2,
            recommendedActions = emptyList(),
            summaryMessage = "Ready"
        )
        val pending = NotificationSchedulerEngine.evaluatePendingNotifications(
            prefs = prefs,
            physical = PhysicalDevelopmentState(),
            recovery = recoveryReady,
            pendingDailyQuests = emptyList(),
            topics = emptyList(),
            exams = emptyList(),
            existingNotifications = emptyList(),
            nowEpochMs = sundayAfternoonMs,
            zoneId = zone,
            habits = listOf(Habit(id = 1L, title = "Hydrate")),
            routines = listOf(LifeRoutine(id = 1L, title = "Evening Reset", startMinuteOfDay = 19 * 60)),
            tasks = listOf(PersonalTask(id = 1L, title = "Overdue Bill", dueDateEpochDay = sundayEpoch - 1L)),
            goalMilestones = listOf(GoalMilestone(id = 1L, goalId = 1L, title = "Q2 Check", dueDateEpochDay = sundayEpoch + 2L))
        )

        val categories = pending.map { it.category }.toSet()
        assertTrue(NotificationCategory.OVERDUE_TASK in categories)
        assertTrue(NotificationCategory.HABIT_REMINDER in categories)
        assertTrue(NotificationCategory.ROUTINE_REMINDER in categories)
        assertTrue(NotificationCategory.GOAL_MILESTONE in categories)
        assertTrue(NotificationCategory.WEEKLY_REVIEW_REMINDER in categories)

        // Quiet hours at 23:00 should suppress all standard Life alerts
        val quietNightMs = ZonedDateTime.of(2026, 6, 21, 23, 0, 0, 0, zone).toInstant().toEpochMilli()
        val duringQuiet = NotificationSchedulerEngine.evaluatePendingNotifications(
            prefs = prefs,
            physical = PhysicalDevelopmentState(),
            recovery = recoveryReady,
            pendingDailyQuests = emptyList(),
            topics = emptyList(),
            exams = emptyList(),
            existingNotifications = emptyList(),
            nowEpochMs = quietNightMs,
            zoneId = zone,
            habits = listOf(Habit(id = 1L, title = "Hydrate")),
            tasks = listOf(PersonalTask(id = 1L, title = "Overdue Bill", dueDateEpochDay = sundayEpoch - 1L))
        )
        assertTrue(duringQuiet.isEmpty())
    }
}
