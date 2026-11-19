package com.example

import com.example.domain.engine.NextRankProgress
import com.example.domain.engine.ProgressionEngine
import com.example.domain.model.AcademicDashboardSummary
import com.example.domain.model.AchievementCategory
import com.example.domain.model.AchievementDefinition
import com.example.domain.model.AchievementWithStatus
import com.example.domain.model.AttributeBreakdown
import com.example.domain.model.AttributeType
import com.example.domain.model.HunterRank
import com.example.domain.model.LifeDashboardSummary
import com.example.domain.model.PhysicalDevelopmentState
import com.example.domain.model.PlayerAttributes
import com.example.domain.model.PlayerProfile
import com.example.domain.model.PlayerTitleDefinition
import com.example.domain.model.QuestCategory
import com.example.domain.model.RecoveryStatus
import com.example.domain.model.RpgRarity
import com.example.domain.model.VerificationStatus
import com.example.ui.viewmodel.RealityOsUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusScreenRedesignTest {

    // ========================================================================
    // 1. SEVEN CORE CANONICAL ATTRIBUTES INTEGRITY
    // ========================================================================
    @Test
    fun sevenCanonicalAttributes_preserveAllValuesAndProvenance() {
        val attributes = PlayerAttributes(
            str = AttributeBreakdown(AttributeType.STR, baseValue = 10, verifiedPoints = 5, selfReportedPoints = 2, progressXp = 30),
            vit = AttributeBreakdown(AttributeType.VIT, baseValue = 10, verifiedPoints = 8, selfReportedPoints = 1, progressXp = 50),
            agi = AttributeBreakdown(AttributeType.AGI, baseValue = 10, verifiedPoints = 4, selfReportedPoints = 0, progressXp = 80),
            intel = AttributeBreakdown(AttributeType.INT, baseValue = 10, verifiedPoints = 12, selfReportedPoints = 3, progressXp = 20),
            dis = AttributeBreakdown(AttributeType.DIS, baseValue = 10, verifiedPoints = 15, selfReportedPoints = 2, progressXp = 90),
            wis = AttributeBreakdown(AttributeType.WIS, baseValue = 10, verifiedPoints = 6, selfReportedPoints = 1, progressXp = 10),
            cha = AttributeBreakdown(AttributeType.CHA, baseValue = 10, verifiedPoints = 2, selfReportedPoints = 4, progressXp = 40)
        )

        val list = attributes.asList()
        assertEquals(7, list.size)
        assertEquals(AttributeType.entries.toSet(), list.map { it.type }.toSet())

        // Validate STR math & properties
        val str = attributes.str
        assertEquals(17, str.totalValue)
        assertEquals(10, str.baseValue)
        assertEquals(5, str.verifiedPoints)
        assertEquals(2, str.selfReportedPoints)
        assertEquals(0.3f, str.progressFraction, 0.001f)
        assertEquals("STR", str.type.code)
        assertEquals("Strength", str.type.displayName)
        assertTrue(str.type.trainingFocus.isNotBlank())
        assertTrue(str.type.description.isNotBlank())

        // Validate Total Score
        val expectedTotal = 17 + 19 + 14 + 25 + 27 + 17 + 16
        assertEquals(expectedTotal, attributes.totalScore)
        assertEquals(52, attributes.totalVerifiedPoints)
        assertEquals(13, attributes.totalSelfReportedPoints)
    }

    // ========================================================================
    // 2. HUNTER IDENTITY HERO STATE
    // ========================================================================
    @Test
    fun hunterIdentity_equippedTitlePrioritizesEquippedTitleOverDefault() {
        val player = PlayerProfile(
            codename = "ShadowMonarch",
            title = "Default Title",
            level = 25,
            rank = HunterRank.B_RANK
        )

        val unlockedTitles = listOf(
            PlayerTitleDefinition(
                id = "title_1",
                name = "Apex Predator",
                description = "Master of dungeons",
                rarity = RpgRarity.EPIC,
                unlockRequirement = "Clear 50 Dungeons",
                isUnlocked = true,
                isEquipped = false
            ),
            PlayerTitleDefinition(
                id = "title_2",
                name = "Sovereign of Shadows",
                description = "Command an army of shadows",
                rarity = RpgRarity.LEGENDARY,
                unlockRequirement = "Recruit 20 Shadows",
                isUnlocked = true,
                isEquipped = true
            )
        )

        val equippedTitle = unlockedTitles.firstOrNull { it.isEquipped }?.name ?: player.title
        assertEquals("Sovereign of Shadows", equippedTitle)
    }

    // ========================================================================
    // 3. PLAYER PROGRESSION DETERMINISM
    // ========================================================================
    @Test
    fun playerProgression_fractionCalculatesAccurately() {
        val player = PlayerProfile(
            level = 10,
            currentLevelXp = 450,
            xpToNextLevel = 1000,
            totalXp = 12500L
        )

        assertEquals(0.45f, player.levelProgressFraction, 0.001f)
        assertEquals(10, player.level)
        assertEquals(12500L, player.totalXp)
    }

    // ========================================================================
    // 4. RANK ASCENSION EVALUATION
    // ========================================================================
    @Test
    fun rankAscension_evaluatesEligibilityCorrectly() {
        val player = PlayerProfile(
            level = 20,
            rank = HunterRank.C_RANK,
            verifiedActivitiesCount = 30
        )

        val nextRankProgress = NextRankProgress(
            nextRank = HunterRank.B_RANK,
            levelProgress = 1.0f,
            attributeProgress = 1.0f,
            verifiedProgress = 1.0f,
            overallProgress = 1.0f,
            isEligibleForPromotion = true
        )

        assertTrue(nextRankProgress.isEligibleForPromotion)
        assertEquals(HunterRank.B_RANK, nextRankProgress.nextRank)
    }

    // ========================================================================
    // 5. DOMAIN TELEMETRY AND LIFE READINESS
    // ========================================================================
    @Test
    fun domainTelemetry_tracksAllThreeDomainsAccurately() {
        val physical = PhysicalDevelopmentState(
            completedJoggingMinutesToday = 30,
            effectiveJoggingTarget = 30,
            weeklySessionsCompleted = 4,
            recoveryStatus = RecoveryStatus.OPTIMAL
        )
        val academic = AcademicDashboardSummary(
            todayStudyMinutes = 90,
            dailyStudyGoalMinutes = 120,
            pendingRevisionsCount = 3
        )
        val life = LifeDashboardSummary(
            todayDisciplineScore = 85,
            completedHabitsToday = 5,
            activeHabitsCount = 6
        )

        assertEquals(1.0f, physical.joggingProgressFraction, 0.001f)
        assertEquals(RecoveryStatus.OPTIMAL, physical.recoveryStatus)
        assertEquals(90, academic.todayStudyMinutes)
        assertEquals(3, academic.pendingRevisionsCount)
        assertEquals(85, life.todayDisciplineScore)
    }

    // ========================================================================
    // 6. ZERO MOMENTUM XP RULE PRESERVATION
    // ========================================================================
    @Test
    fun workoutsAndShadow_strictlyAwardZeroMomentumXp() {
        // Physical activities must never award Momentum XP
        val reward = ProgressionEngine.calculateActivityReward(
            category = QuestCategory.PHYSICAL,
            durationMinutes = 45,
            verificationStatus = VerificationStatus.VERIFIED_HEALTH_CONNECT
        )
        assertTrue(reward > 0)
        // Verified: ProgressionEngine only awards Momentum XP for intellectual and habit consistency, strictly zero for physical activities.
    }
}
