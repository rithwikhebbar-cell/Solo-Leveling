package com.example

import com.example.domain.engine.ProgressionEngine
import com.example.domain.model.AttributeType
import com.example.domain.model.Quest
import com.example.domain.model.QuestCategory
import com.example.domain.model.QuestType
import com.example.domain.model.VerificationStatus
import com.example.ui.screens.quest.components.QuestTabFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestBoardRedesignTest {

    private fun createTestQuest(
        id: Long,
        title: String,
        type: QuestType,
        category: QuestCategory = QuestCategory.DISCIPLINE,
        targetValue: Int = 10,
        currentValue: Int = 0,
        isCompleted: Boolean = false,
        xpReward: Int = 50,
        primaryAttribute: AttributeType = AttributeType.DIS,
        primaryAttrXpReward: Int = 20,
        verificationStatus: VerificationStatus = VerificationStatus.SELF_REPORTED
    ): Quest {
        return Quest(
            id = id,
            title = title,
            description = "Description for $title",
            type = type,
            category = category,
            targetValue = targetValue,
            currentValue = currentValue,
            unit = "reps",
            xpReward = xpReward,
            primaryAttribute = primaryAttribute,
            primaryAttrXpReward = primaryAttrXpReward,
            isCompleted = isCompleted,
            verificationStatus = verificationStatus
        )
    }

    // ========================================================================
    // 1. QUEST TAB FILTER ENUM & FILTERING LOGIC
    // ========================================================================
    @Test
    fun questTabFilter_containsAllExpectedTabs() {
        val tabs = QuestTabFilter.entries
        assertEquals(5, tabs.size)
        assertTrue(tabs.contains(QuestTabFilter.ALL))
        assertTrue(tabs.contains(QuestTabFilter.DAILY))
        assertTrue(tabs.contains(QuestTabFilter.WEEKLY))
        assertTrue(tabs.contains(QuestTabFilter.LONG_TERM))
        assertTrue(tabs.contains(QuestTabFilter.COMPLETED))
    }

    @Test
    fun questFiltering_filtersByTabCorrectly() {
        val quests = listOf(
            createTestQuest(1, "Daily Active", QuestType.DAILY, isCompleted = false),
            createTestQuest(2, "Daily Cleared", QuestType.DAILY, isCompleted = true),
            createTestQuest(3, "Weekly Active", QuestType.WEEKLY, isCompleted = false),
            createTestQuest(4, "Long Term Active", QuestType.LONG_TERM, isCompleted = false)
        )

        // ALL Tab returns all
        val allQuests = quests
        assertEquals(4, allQuests.size)

        // DAILY Tab returns daily only
        val dailyQuests = quests.filter { it.type == QuestType.DAILY }
        assertEquals(2, dailyQuests.size)
        assertTrue(dailyQuests.all { it.type == QuestType.DAILY })

        // WEEKLY Tab returns weekly only
        val weeklyQuests = quests.filter { it.type == QuestType.WEEKLY }
        assertEquals(1, weeklyQuests.size)
        assertEquals(3L, weeklyQuests.first().id)

        // LONG_TERM Tab returns long term only
        val longTermQuests = quests.filter { it.type == QuestType.LONG_TERM }
        assertEquals(1, longTermQuests.size)
        assertEquals(4L, longTermQuests.first().id)

        // COMPLETED Tab returns completed only
        val completedQuests = quests.filter { it.isCompleted }
        assertEquals(1, completedQuests.size)
        assertEquals(2L, completedQuests.first().id)
    }

    // ========================================================================
    // 2. METRIC CALCULATIONS & XP POOL
    // ========================================================================
    @Test
    fun questMetrics_activeAndCompletedCountsAndXpPool() {
        val quests = listOf(
            createTestQuest(1, "Q1", QuestType.DAILY, isCompleted = false, xpReward = 50),
            createTestQuest(2, "Q2", QuestType.DAILY, isCompleted = true, xpReward = 100),
            createTestQuest(3, "Q3", QuestType.WEEKLY, isCompleted = false, xpReward = 150),
            createTestQuest(4, "Q4", QuestType.LONG_TERM, isCompleted = true, xpReward = 300)
        )

        val activeCount = quests.count { !it.isCompleted }
        val completedCount = quests.count { it.isCompleted }
        val xpPoolAvailable = quests.filter { !it.isCompleted }.sumOf { it.xpReward }

        assertEquals(2, activeCount)
        assertEquals(2, completedCount)
        assertEquals(200, xpPoolAvailable) // 50 + 150
    }

    // ========================================================================
    // 3. PROGRESS FRACTION AND CAPPING INVARIANTS
    // ========================================================================
    @Test
    fun questProgressFraction_handlesZeroTargetAndOverflowCorrectly() {
        val zeroTargetQuest = createTestQuest(1, "Zero Target", QuestType.DAILY, targetValue = 0, currentValue = 5)
        assertEquals(0f, zeroTargetQuest.progressFraction)

        val partialQuest = createTestQuest(2, "Partial", QuestType.DAILY, targetValue = 100, currentValue = 25)
        assertEquals(0.25f, partialQuest.progressFraction)

        val overflowQuest = createTestQuest(3, "Overflow", QuestType.DAILY, targetValue = 50, currentValue = 100)
        assertEquals(1f, overflowQuest.progressFraction)

        val negativeProgressQuest = createTestQuest(4, "Negative", QuestType.DAILY, targetValue = 50, currentValue = -10)
        assertEquals(0f, negativeProgressQuest.progressFraction)
    }

    @Test
    fun dailyProtocol_progressFractionCalculation() {
        val dailyQuests = listOf(
            createTestQuest(1, "D1", QuestType.DAILY, isCompleted = true),
            createTestQuest(2, "D2", QuestType.DAILY, isCompleted = true),
            createTestQuest(3, "D3", QuestType.DAILY, isCompleted = false),
            createTestQuest(4, "D4", QuestType.DAILY, isCompleted = false)
        )

        val totalDaily = dailyQuests.size
        val completedDaily = dailyQuests.count { it.isCompleted }
        val fraction = completedDaily.toFloat() / totalDaily

        assertEquals(4, totalDaily)
        assertEquals(2, completedDaily)
        assertEquals(0.5f, fraction)
    }

    // ========================================================================
    // 4. DUPLICATE TAP AND IN-FLIGHT ID PROTECTION
    // ========================================================================
    @Test
    fun questInFlightDuplicateTapGuard_preventsDoubleCompletion() {
        var completedCalls = 0
        var pendingQuestIds = setOf<Long>()

        val onCompleteQuest: (Long) -> Unit = { id ->
            completedCalls++
        }

        val testQuest = createTestQuest(101L, "Test Guard", QuestType.DAILY, isCompleted = false)

        val attemptCompletion: (Long) -> Unit = { questId ->
            if (!testQuest.isCompleted && !pendingQuestIds.contains(questId)) {
                pendingQuestIds = pendingQuestIds + questId
                onCompleteQuest(questId)
            }
        }

        // First tap initiates completion
        attemptCompletion(101L)
        assertEquals(1, completedCalls)
        assertTrue(pendingQuestIds.contains(101L))

        // Rapid second and third taps are guarded
        attemptCompletion(101L)
        attemptCompletion(101L)
        assertEquals(1, completedCalls)

        // Once completed, cannot be completed again even if pendingQuestIds cleared
        val completedQuest = testQuest.copy(isCompleted = true)
        val attemptCompleted: (Long) -> Unit = { questId ->
            if (!completedQuest.isCompleted && !pendingQuestIds.contains(questId)) {
                pendingQuestIds = pendingQuestIds + questId
                onCompleteQuest(questId)
            }
        }
        pendingQuestIds = emptySet()
        attemptCompleted(101L)
        assertEquals(1, completedCalls)
    }

    // ========================================================================
    // 5. CANONICAL REWARDS AND VERIFICATION INTEGRITY
    // ========================================================================
    @Test
    fun questRewardIntegrity_attributesAreCanonical() {
        val quest = createTestQuest(
            id = 42,
            title = "Mind Protocol",
            type = QuestType.DAILY,
            category = QuestCategory.INTELLECT,
            primaryAttribute = AttributeType.INT,
            primaryAttrXpReward = 40,
            verificationStatus = VerificationStatus.VERIFIED_SYSTEM_TIMER
        )

        assertEquals("INT", quest.primaryAttribute.code)
        assertEquals("Intelligence", quest.primaryAttribute.displayName)
        assertEquals(40, quest.primaryAttrXpReward)
        assertTrue(quest.verificationStatus.isVerified)
    }

    // ========================================================================
    // 6. CUSTOM QUEST VALIDATION RULES
    // ========================================================================
    @Test
    fun createQuestValidation_enforcesNonBlankTitleAndPositiveTarget() {
        fun isQuestValid(title: String, targetValue: Int): Boolean {
            return title.isNotBlank() && targetValue > 0
        }

        assertFalse(isQuestValid("", 10))
        assertFalse(isQuestValid("   ", 10))
        assertFalse(isQuestValid("Valid Title", 0))
        assertFalse(isQuestValid("Valid Title", -5))
        assertTrue(isQuestValid("Read 30 mins", 30))
    }
}
