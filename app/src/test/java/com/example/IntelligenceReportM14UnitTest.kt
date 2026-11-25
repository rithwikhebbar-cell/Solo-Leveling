package com.example

import com.example.domain.engine.HomeNextActionEngine
import com.example.domain.engine.IntelligenceReportEngine
import com.example.domain.engine.IntelligenceTelemetryInput
import com.example.domain.model.AcademicDashboardSummary
import com.example.domain.model.AttributeType
import com.example.domain.model.GoalStatus
import com.example.domain.model.GoalWithDetails
import com.example.domain.model.HunterRank
import com.example.domain.model.JoggingSession
import com.example.domain.model.LifeDashboardSummary
import com.example.domain.model.LifeGoal
import com.example.domain.model.MissionCategory
import com.example.domain.model.MissionPriority
import com.example.domain.model.MissionSourceModule
import com.example.domain.model.MissionSourceType
import com.example.domain.model.MissionStatus
import com.example.domain.model.PersonalTask
import com.example.domain.model.PersonalTaskStatus
import com.example.domain.model.PhysicalDevelopmentState
import com.example.domain.model.PlayerProfile
import com.example.domain.model.Quest
import com.example.domain.model.QuestCategory
import com.example.domain.model.QuestType
import com.example.domain.model.RecoveryAssessment
import com.example.domain.model.RecoveryState
import com.example.domain.model.ReportPeriodType
import com.example.domain.model.StudySession
import com.example.domain.model.UnifiedMissionItem
import com.example.domain.model.VerificationStatus
import com.example.domain.model.WorkoutStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class IntelligenceReportM14UnitTest {

    private val fixedDay: Long = 20150L
    private val dayMs = 24 * 60 * 60_000L
    private val fixedNowMs: Long = LocalDate.ofEpochDay(fixedDay)
        .atStartOfDay(ZoneOffset.UTC)
        .toInstant()
        .toEpochMilli() + 14 * 3600_000L

    private fun baseInput(
        jogs: List<JoggingSession> = emptyList(),
        studies: List<StudySession> = emptyList(),
        tasks: List<PersonalTask> = emptyList(),
        quests: List<Quest> = emptyList(),
        goals: List<GoalWithDetails> = emptyList()
    ) = IntelligenceTelemetryInput(
        profile = PlayerProfile(level = 5, rank = HunterRank.D_RANK),
        recovery = RecoveryAssessment(
            state = RecoveryState.READY,
            readinessScore = 85,
            measuredLoadMinutes48h = 30,
            measuredLoadMinutes7d = 90,
            measuredSessions7d = 2,
            consecutiveActiveDays = 1,
            selfReportedDiscomfort = false,
            recommendedActions = listOf("Maintain routine"),
            summaryMessage = "Optimal"
        ),
        joggingSessions = jogs,
        studySessions = studies,
        tasks = tasks,
        generalQuests = quests,
        goalsWithDetails = goals,
        nowEpochMs = fixedNowMs,
        todayEpochDay = fixedDay,
        zoneId = ZoneOffset.UTC
    )

    @Test
    fun `daily report captures today verified activity and mission completions accurately`() {
        val studyToday = StudySession(
            sourceId = "s-today",
            subjectId = 1L,
            subjectName = "Math",
            title = "Linear Algebra Drill",
            startEpochMs = fixedNowMs - 2 * 3600_000L,
            endEpochMs = fixedNowMs - 3600_000L,
            durationMinutes = 60,
            verificationStatus = VerificationStatus.VERIFIED_SYSTEM_TIMER,
            xpAwarded = 60
        )
        val studyYesterday = StudySession(
            sourceId = "s-yest",
            subjectId = 1L,
            subjectName = "Math",
            title = "Old Session",
            startEpochMs = fixedNowMs - dayMs,
            endEpochMs = fixedNowMs - dayMs + 30 * 60_000L,
            durationMinutes = 30,
            verificationStatus = VerificationStatus.VERIFIED_SYSTEM_TIMER,
            xpAwarded = 30
        )
        val completedQuestToday = Quest(
            id = 1L,
            title = "Daily Push-ups",
            description = "Conditioning",
            type = QuestType.DAILY,
            category = QuestCategory.PHYSICAL,
            targetValue = 20,
            currentValue = 20,
            unit = "reps",
            xpReward = 40,
            primaryAttribute = AttributeType.STR,
            primaryAttrXpReward = 15,
            isCompleted = true,
            completedAtEpochMs = fixedNowMs - 1800_000L
        )
        val input = baseInput(
            studies = listOf(studyToday, studyYesterday),
            quests = listOf(completedQuestToday)
        )

        val dailyReport = IntelligenceReportEngine.generateReport(ReportPeriodType.DAILY, input)
        assertEquals(ReportPeriodType.DAILY, dailyReport.periodType)
        assertEquals(60, dailyReport.studyMinutes)
        assertEquals(1, dailyReport.tasksCompletedCount)
        assertEquals(1, dailyReport.activeDaysObserved)
        assertFalse(dailyReport.isPartialDataWindow)
    }

    @Test
    fun `weekly comparison distinguishes absent baseline from real week-over-week deltas`() {
        val currStudy = StudySession(
            sourceId = "s-curr",
            subjectId = 1L,
            subjectName = "CS",
            title = "Algorithms",
            startEpochMs = fixedNowMs - 2 * dayMs,
            endEpochMs = fixedNowMs - 2 * dayMs + 90 * 60_000L,
            durationMinutes = 90,
            verificationStatus = VerificationStatus.VERIFIED_SYSTEM_TIMER,
            xpAwarded = 90
        )
        val noPrevInput = baseInput(studies = listOf(currStudy))
        val emptyBaseline = IntelligenceReportEngine.computeWeeklyComparison(noPrevInput)
        assertFalse(emptyBaseline.hasPreviousBaseline)
        assertNull(emptyBaseline.studyMinutesDelta)
        assertTrue(emptyBaseline.summaryNote.contains("No recorded sessions", ignoreCase = true))

        val prevStudy = StudySession(
            sourceId = "s-prev",
            subjectId = 1L,
            subjectName = "CS",
            title = "Prior Week Review",
            startEpochMs = fixedNowMs - 9 * dayMs,
            endEpochMs = fixedNowMs - 9 * dayMs + 40 * 60_000L,
            durationMinutes = 40,
            verificationStatus = VerificationStatus.VERIFIED_SYSTEM_TIMER,
            xpAwarded = 40
        )
        val withPrevInput = baseInput(studies = listOf(currStudy, prevStudy))
        val comparison = IntelligenceReportEngine.computeWeeklyComparison(withPrevInput)
        assertTrue(comparison.hasPreviousBaseline)
        assertEquals(50, comparison.studyMinutesDelta)
        assertEquals(50, comparison.xpDelta)
    }

    @Test
    fun `goal analysis evaluates pace and excludes archived goals`() {
        val activeOnTrack = GoalWithDetails(
            goal = LifeGoal(id = 1L, title = "Publish Paper", status = GoalStatus.ACTIVE, startDateEpochDay = fixedDay - 10, targetDateEpochDay = fixedDay + 14),
            milestones = emptyList(),
            progressRecords = emptyList(),
            computedProgressPercent = 60,
            completedMilestonesCount = 3,
            totalMilestonesCount = 5,
            linkedCompletedTasksCount = 0,
            isBehindSchedule = false
        )
        val behindGoal = GoalWithDetails(
            goal = LifeGoal(id = 2L, title = "Run Half Marathon", status = GoalStatus.ACTIVE, startDateEpochDay = fixedDay - 10, targetDateEpochDay = fixedDay + 5),
            milestones = emptyList(),
            progressRecords = emptyList(),
            computedProgressPercent = 15,
            completedMilestonesCount = 1,
            totalMilestonesCount = 4,
            linkedCompletedTasksCount = 0,
            isBehindSchedule = true
        )
        val archivedGoal = GoalWithDetails(
            goal = LifeGoal(id = 3L, title = "Old Archived Goal", status = GoalStatus.ARCHIVED, startDateEpochDay = fixedDay - 20, targetDateEpochDay = fixedDay + 1),
            milestones = emptyList(),
            progressRecords = emptyList(),
            computedProgressPercent = 0,
            completedMilestonesCount = 0,
            totalMilestonesCount = 1,
            linkedCompletedTasksCount = 0,
            isBehindSchedule = false
        )

        val insights = IntelligenceReportEngine.computeGoalAnalysis(
            baseInput(goals = listOf(activeOnTrack, behindGoal, archivedGoal))
        )
        assertEquals(2, insights.size)
        assertTrue(insights.first { it.goalId == 1L }.paceAssessment.contains("On track"))
        assertTrue(insights.first { it.goalId == 2L }.paceAssessment.contains("Behind schedule"))
    }

    @Test
    fun `home next action integrates unblocked mission but preserves recovery override`() {
        val topMission = UnifiedMissionItem(
            missionId = "task:42",
            sourceType = MissionSourceType.PERSONAL_TASK,
            sourceRecordId = 42L,
            title = "Complete Security Audit",
            description = "Review repository permissions",
            category = MissionCategory.DAILY,
            module = MissionSourceModule.LIFE,
            status = MissionStatus.ACTIVE,
            priority = MissionPriority.CRITICAL,
            currentValue = 0,
            targetValue = 1,
            unit = "task",
            isBlocked = false,
            isRecoveryRestricted = false,
            priorityScore = 88,
            priorityReason = "Due today"
        )

        val readyAction = HomeNextActionEngine.evaluate(
            physicalState = PhysicalDevelopmentState(
                recoveryAssessment = RecoveryAssessment(
                    state = RecoveryState.READY,
                    readinessScore = 90,
                    measuredLoadMinutes48h = 20,
                    measuredLoadMinutes7d = 60,
                    measuredSessions7d = 2,
                    consecutiveActiveDays = 1,
                    selfReportedDiscomfort = false,
                    recommendedActions = emptyList(),
                    summaryMessage = "Ready"
                ),
                workoutStatus = WorkoutStatus.PENDING
            ),
            quests = emptyList(),
            academicSummary = AcademicDashboardSummary(),
            lifeSummary = LifeDashboardSummary(),
            topUnblockedMission = topMission
        )
        assertEquals("mission_task:42", readyAction.id)
        assertEquals("mission_control", readyAction.destinationRoute)

        val recoveryAction = HomeNextActionEngine.evaluate(
            physicalState = PhysicalDevelopmentState(
                recoveryAssessment = RecoveryAssessment(
                    state = RecoveryState.REST_RECOMMENDED,
                    readinessScore = 28,
                    measuredLoadMinutes48h = 180,
                    measuredLoadMinutes7d = 240,
                    measuredSessions7d = 5,
                    consecutiveActiveDays = 5,
                    selfReportedDiscomfort = true,
                    recommendedActions = listOf("Rest"),
                    summaryMessage = "Rest recommended"
                )
            ),
            quests = emptyList(),
            academicSummary = AcademicDashboardSummary(),
            lifeSummary = LifeDashboardSummary(),
            topUnblockedMission = topMission
        )
        assertTrue(recoveryAction.isSafetyOverride)
        assertEquals("prioritize_recovery_rest", recoveryAction.id)
    }
}
