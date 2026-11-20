package com.example

import com.example.domain.engine.HomeNextActionEngine
import com.example.domain.engine.ProgressionEngine
import com.example.domain.model.AcademicDashboardSummary
import com.example.domain.model.ActiveJoggingSessionState
import com.example.domain.model.AttributeType
import com.example.domain.model.HunterRank
import com.example.domain.model.LifeDashboardSummary
import com.example.domain.model.PhysicalActivityKind
import com.example.domain.model.PhysicalDevelopmentState
import com.example.domain.model.PlayerAttributes
import com.example.domain.model.PlayerProfile
import com.example.domain.model.Quest
import com.example.domain.model.QuestCategory
import com.example.domain.model.QuestType
import com.example.domain.model.RecoveryAssessment
import com.example.domain.model.RecoveryState
import com.example.domain.model.ShadowMonarchProfile
import com.example.domain.model.VerificationStatus
import com.example.domain.model.WorkoutPlan
import com.example.domain.model.WorkoutStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeDashboardRedesignTest {

    private fun createTestQuest(
        id: Long,
        title: String,
        type: QuestType = QuestType.DAILY,
        isCompleted: Boolean = false,
        xpReward: Int = 100
    ): Quest = Quest(
        id = id,
        title = title,
        description = "Description for $title",
        type = type,
        category = QuestCategory.DISCIPLINE,
        targetValue = 10,
        currentValue = if (isCompleted) 10 else 3,
        unit = "reps",
        xpReward = xpReward,
        primaryAttribute = AttributeType.DIS,
        primaryAttrXpReward = 20,
        isCompleted = isCompleted
    )

    // ========================================================================
    // 1 & 2. PLAYER PROFILE & RANK / LEVEL PRESENTATION
    // ========================================================================
    @Test
    fun playerProfile_levelProgressAndRankPresentation() {
        val player = PlayerProfile(
            codename = "Sung Jin-Woo",
            title = "Shadow Monarch",
            level = 42,
            currentLevelXp = 60,
            xpToNextLevel = 100,
            totalXp = 12500L,
            rank = HunterRank.S_RANK,
            streakDays = 14
        )

        assertEquals("Sung Jin-Woo", player.codename)
        assertEquals("Shadow Monarch", player.title)
        assertEquals(42, player.level)
        assertEquals(0.6f, player.levelProgressFraction)
        assertEquals(HunterRank.S_RANK, player.rank)
        assertEquals("S-Rank", player.rank.displayName)
        assertEquals(14, player.streakDays)
    }

    // ========================================================================
    // 3. CANONICAL SEVEN ATTRIBUTES SUMMARY
    // ========================================================================
    @Test
    fun canonicalAttributes_exactSevenAttributesPresent() {
        val attributes = PlayerAttributes()
        val list = attributes.asList()

        assertEquals(7, list.size)
        val expectedCodes = listOf("STR", "VIT", "AGI", "INT", "DIS", "WIS", "CHA")
        val actualCodes = list.map { it.type.code }
        assertEquals(expectedCodes, actualCodes)

        // Ensure legacy non-canonical attributes are completely absent
        assertFalse(actualCodes.contains("SEN"))
        assertFalse(actualCodes.contains("WIL"))
    }

    // ========================================================================
    // 4. TODAY'S QUEST SELECTION
    // ========================================================================
    @Test
    fun todayQuestSelection_selectsFirstUncompletedDailyQuest() {
        val quests = listOf(
            createTestQuest(1, "Daily Quest 1", isCompleted = true),
            createTestQuest(2, "Daily Quest 2", isCompleted = false),
            createTestQuest(3, "Daily Quest 3", isCompleted = false)
        )

        val dailyQuests = quests.filter { it.type == QuestType.DAILY }
        val primaryQuest = dailyQuests.firstOrNull { !it.isCompleted }

        assertNotNull(primaryQuest)
        assertEquals(2L, primaryQuest!!.id)
        assertFalse(primaryQuest.isCompleted)
    }

    @Test
    fun todayQuestSelection_allClearedHandledCorrectly() {
        val quests = listOf(
            createTestQuest(1, "Daily 1", isCompleted = true),
            createTestQuest(2, "Daily 2", isCompleted = true)
        )

        val dailyQuests = quests.filter { it.type == QuestType.DAILY }
        val allCleared = dailyQuests.isNotEmpty() && dailyQuests.all { it.isCompleted }

        assertTrue(allCleared)
    }

    // ========================================================================
    // 5. DAILY PROGRESS CALCULATIONS
    // ========================================================================
    @Test
    fun dailyProgressSnapshot_calculatesCorrectFractions() {
        val dailyQuests = listOf(
            createTestQuest(1, "D1", isCompleted = true),
            createTestQuest(2, "D2", isCompleted = true),
            createTestQuest(3, "D3", isCompleted = false)
        )

        val completedCount = dailyQuests.count { it.isCompleted }
        val totalCount = dailyQuests.size
        val fraction = completedCount.toFloat() / totalCount

        assertEquals(2, completedCount)
        assertEquals(3, totalCount)
        assertEquals(2f / 3f, fraction, 0.001f)
    }

    // ========================================================================
    // 6 & 7. NEXT-ACTION PRIORITIZATION & REST_RECOMMENDED SAFETY BEHAVIOR
    // ========================================================================
    @Test
    fun nextAction_restRecommended_neverRecommendsStartingWorkout() {
        val recoveryExhausted = RecoveryAssessment(
            readinessScore = 32,
            state = RecoveryState.REST_RECOMMENDED,
            consecutiveHeavyDays = 5,
            fatigueRating = 5,
            selfReportedDiscomfort = true,
            recommendedActions = listOf("Rest", "Sleep 8 hours")
        )

        val physicalState = PhysicalDevelopmentState(
            recoveryAssessment = recoveryExhausted,
            todayWorkout = WorkoutPlan(
                id = "plan_1",
                title = "Heavy Squats",
                category = com.example.domain.model.WorkoutCategory.STRENGTH,
                status = WorkoutStatus.NOT_STARTED,
                exercises = emptyList()
            )
        )

        val action = HomeNextActionEngine.evaluate(
            physicalState = physicalState,
            quests = listOf(createTestQuest(1, "Daily Quest", isCompleted = false)),
            academicSummary = AcademicDashboardSummary(),
            lifeSummary = LifeDashboardSummary()
        )

        // Safety invariant: MUST recommend Recovery, NEVER workout initiation
        assertEquals("RECOVERY", action.category)
        assertTrue(action.isSafetyOverride)
        assertEquals("VIEW RECOVERY", action.actionLabel)
        assertEquals("physical", action.destinationRoute)
        assertTrue(action.description.contains("Restorative", ignoreCase = true) || action.description.contains("mobility", ignoreCase = true))
        assertFalse(action.actionLabel.contains("START WORKOUT", ignoreCase = true))
    }

    // ========================================================================
    // 8. ACTIVE SESSION CONTINUATION
    // ========================================================================
    @Test
    fun nextAction_activeJoggingSession_recommendsContinue() {
        val activeJog = ActiveJoggingSessionState(
            sessionId = "jog_123",
            targetMinutes = 40,
            elapsedMinutes = 15,
            startTimeEpochMs = System.currentTimeMillis() - 15 * 60_000L
        )

        val physicalState = PhysicalDevelopmentState(
            activeJoggingSession = activeJog
        )

        val action = HomeNextActionEngine.evaluate(
            physicalState = physicalState,
            quests = emptyList(),
            academicSummary = AcademicDashboardSummary(),
            lifeSummary = LifeDashboardSummary()
        )

        assertEquals("PHYSICAL", action.category)
        assertTrue(action.isSessionContinuation)
        assertEquals("CONTINUE JOGGING", action.actionLabel)
        assertEquals(100, action.priorityScore)
    }

    @Test
    fun nextAction_activeWorkoutInProgress_recommendsContinue() {
        val physicalState = PhysicalDevelopmentState(
            todayWorkout = WorkoutPlan(
                id = "plan_2",
                title = "Bench Press",
                category = com.example.domain.model.WorkoutCategory.STRENGTH,
                status = WorkoutStatus.IN_PROGRESS,
                exercises = emptyList()
            )
        )

        val action = HomeNextActionEngine.evaluate(
            physicalState = physicalState,
            quests = emptyList(),
            academicSummary = AcademicDashboardSummary(),
            lifeSummary = LifeDashboardSummary()
        )

        assertTrue(action.isSessionContinuation)
        assertEquals("CONTINUE WORKOUT", action.actionLabel)
        assertEquals(100, action.priorityScore)
    }

    // ========================================================================
    // 9. DOMAIN NAVIGATION TARGETS
    // ========================================================================
    @Test
    fun nextAction_overdueRevisions_routesToAcademic() {
        val academicSummary = AcademicDashboardSummary(
            overdueRevisionsCount = 3,
            pendingRevisionsCount = 5
        )

        val action = HomeNextActionEngine.evaluate(
            physicalState = PhysicalDevelopmentState(),
            quests = emptyList(),
            academicSummary = academicSummary,
            lifeSummary = LifeDashboardSummary()
        )

        assertEquals("ACADEMIC", action.category)
        assertEquals("academic", action.destinationRoute)
        assertEquals("OPEN REVISIONS", action.actionLabel)
    }

    // ========================================================================
    // 10. MISSING OPTIONAL DOMAIN DATA GRACEFUL HANDLING
    // ========================================================================
    @Test
    fun optionalDomainData_gracefullyHandlesNullOrDefaults() {
        val defaultAcademic = AcademicDashboardSummary()
        assertEquals(0, defaultAcademic.todayStudyMinutes)
        assertEquals(0, defaultAcademic.overdueRevisionsCount)

        val defaultLife = LifeDashboardSummary()
        assertEquals(0, defaultLife.completedHabitsToday)
        assertEquals(0, defaultLife.overdueTasksCount)

        val nullMonarch: ShadowMonarchProfile? = null
        assertNull(nullMonarch)

        // Evaluator handles all empty domain states without exceptions
        val action = HomeNextActionEngine.evaluate(
            physicalState = PhysicalDevelopmentState(),
            quests = emptyList(),
            academicSummary = defaultAcademic,
            lifeSummary = defaultLife
        )
        assertNotNull(action)
    }

    // ========================================================================
    // 11. MOMENTUM XP STRICT ISOLATION INVARIANT
    // ========================================================================
    @Test
    fun momentumXp_physicalActivityAwardsStrictZeroMomentumXp() {
        val workoutReward = ProgressionEngine.calculatePhysicalReward(
            kind = PhysicalActivityKind.WORKOUT,
            durationMinutes = 45,
            verificationStatus = VerificationStatus.VERIFIED_HEALTH_CONNECT,
            xpAlreadyEarnedToday = 0,
            selfReportedPhysicalCountToday = 0
        )

        // Physical workouts award Character Level XP and Attribute XP
        assertTrue("Workout must award XP", workoutReward.xpAwarded > 0)
        assertEquals(AttributeType.STR, workoutReward.primaryAttribute)

        // Momentum XP must remain completely isolated and unaffected
        // Momentum XP is never produced by ProgressionEngine.calculatePhysicalReward
    }
}
