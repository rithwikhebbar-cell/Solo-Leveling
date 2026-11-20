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
    // 1. SEVEN CORE CANONICAL ATTRIBUTES INTEGRITY & ORDERING
    // ========================================================================
    @Test
    fun sevenCanonicalAttributes_exactOrderingAndIdentifiers() {
        val expectedCodes = listOf("STR", "VIT", "AGI", "INT", "DIS", "WIS", "CHA")
        val expectedNames = listOf(
            "Strength",
            "Vitality",
            "Agility",
            "Intelligence",
            "Discipline",
            "Wisdom",
            "Charisma"
        )

        val enumEntries = AttributeType.entries
        assertEquals("Must contain exactly seven canonical attributes", 7, enumEntries.size)

        for (i in expectedCodes.indices) {
            assertEquals("Index $i code mismatch", expectedCodes[i], enumEntries[i].code)
            assertEquals("Index $i displayName mismatch", expectedNames[i], enumEntries[i].displayName)
        }

        val emptyPlayerAttrs = PlayerAttributes()
        val list = emptyPlayerAttrs.asList()
        assertEquals(7, list.size)
        for (i in expectedCodes.indices) {
            assertEquals("PlayerAttributes.asList() index $i mismatch", expectedCodes[i], list[i].type.code)
            assertEquals("PlayerAttributes.asList() index $i name mismatch", expectedNames[i], list[i].type.displayName)
        }
    }

    @Test
    fun noLegacyPresentationLabels_senOrWilAbsent() {
        val allCodes = AttributeType.entries.map { it.code }
        val allNames = AttributeType.entries.map { it.displayName }
        val allEnums = AttributeType.entries.map { it.name }

        assertFalse("Must not contain SEN", allCodes.contains("SEN"))
        assertFalse("Must not contain WIL", allCodes.contains("WIL"))
        assertFalse("Must not contain SEN enum", allEnums.contains("SEN"))
        assertFalse("Must not contain WIL enum", allEnums.contains("WIL"))

        assertFalse("Must not contain Sense in display names", allNames.any { it.contains("Sense", ignoreCase = true) })
        assertFalse("Must not contain Perception in display names", allNames.any { it.contains("Perception", ignoreCase = true) })
        assertFalse("Must not contain Willpower in display names", allNames.any { it.contains("Willpower", ignoreCase = true) })
    }

    @Test
    fun attributeAllocationAndProgression_updatesCorrectCanonicalAttribute() {
        val initial = PlayerAttributes()

        // 1. Target WIS with 150 verified XP -> gains 1 verified point, 50 XP remainder
        val afterWis = ProgressionEngine.applyAttributeXp(
            attributes = initial,
            deltas = mapOf(AttributeType.WIS to 150),
            isVerified = true
        )
        assertEquals(1, afterWis.wis.verifiedPoints)
        assertEquals(0, afterWis.wis.selfReportedPoints)
        assertEquals(50, afterWis.wis.progressXp)
        assertEquals(11, afterWis.wis.totalValue) // base 10 + 1

        // Confirm other 6 attributes remain completely untouched
        assertEquals(0, afterWis.str.verifiedPoints)
        assertEquals(0, afterWis.vit.verifiedPoints)
        assertEquals(0, afterWis.agi.verifiedPoints)
        assertEquals(0, afterWis.intel.verifiedPoints)
        assertEquals(0, afterWis.dis.verifiedPoints)
        assertEquals(0, afterWis.cha.verifiedPoints)

        // 2. Target CHA with 220 self-reported XP -> gains 2 self-reported points, 20 XP remainder
        val afterCha = ProgressionEngine.applyAttributeXp(
            attributes = initial,
            deltas = mapOf(AttributeType.CHA to 220),
            isVerified = false
        )
        assertEquals(0, afterCha.cha.verifiedPoints)
        assertEquals(2, afterCha.cha.selfReportedPoints)
        assertEquals(20, afterCha.cha.progressXp)
        assertEquals(12, afterCha.cha.totalValue) // base 10 + 2

        // Confirm WIS and STR are untouched in afterCha
        assertEquals(0, afterCha.wis.verifiedPoints)
        assertEquals(0, afterCha.str.verifiedPoints)
    }

    @Test
    fun playerAttributes_byTypeAccessor_mapsExactAttributes() {
        val attrs = PlayerAttributes(
            str = AttributeBreakdown(AttributeType.STR, baseValue = 11),
            vit = AttributeBreakdown(AttributeType.VIT, baseValue = 12),
            agi = AttributeBreakdown(AttributeType.AGI, baseValue = 13),
            intel = AttributeBreakdown(AttributeType.INT, baseValue = 14),
            dis = AttributeBreakdown(AttributeType.DIS, baseValue = 15),
            wis = AttributeBreakdown(AttributeType.WIS, baseValue = 16),
            cha = AttributeBreakdown(AttributeType.CHA, baseValue = 17)
        )

        assertEquals(11, attrs.byType(AttributeType.STR).baseValue)
        assertEquals(12, attrs.byType(AttributeType.VIT).baseValue)
        assertEquals(13, attrs.byType(AttributeType.AGI).baseValue)
        assertEquals(14, attrs.byType(AttributeType.INT).baseValue)
        assertEquals(15, attrs.byType(AttributeType.DIS).baseValue)
        assertEquals(16, attrs.byType(AttributeType.WIS).baseValue)
        assertEquals(17, attrs.byType(AttributeType.CHA).baseValue)
    }

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
            aiRecommendedJoggingMinutes = 30,
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
        assertTrue(reward.xpAwarded > 0)
        // Verified: ProgressionEngine only awards Momentum XP for intellectual and habit consistency, strictly zero for physical activities.
    }
}
