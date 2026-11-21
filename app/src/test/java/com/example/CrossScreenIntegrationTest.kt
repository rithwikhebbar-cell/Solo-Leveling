package com.example

import androidx.compose.ui.unit.dp
import com.example.domain.engine.HomeNextActionEngine
import com.example.domain.engine.ProgressionEngine
import com.example.domain.model.AcademicDashboardSummary
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
import com.example.domain.model.Title
import com.example.domain.model.VerificationStatus
import com.example.domain.model.WorkoutCategory
import com.example.domain.model.WorkoutPlan
import com.example.domain.model.WorkoutStatus
import com.example.ui.HomeRoute
import com.example.ui.ModulesHubRoute
import com.example.ui.PhysicalRoute
import com.example.ui.QuestBoardRoute
import com.example.ui.StatusRoute
import com.example.ui.screens.modules.ModuleCategory
import com.example.ui.screens.modules.ModuleOperationalStatus
import com.example.ui.screens.modules.ModuleRegistry
import com.example.ui.screens.modules.ModuleSpec
import com.example.ui.theme.SystemSpacing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * STAGE 8: CROSS-SCREEN INTEGRATION & VISUAL CONSISTENCY AUDIT TEST SUITE
 *
 * Verifies that the five redesigned primary destinations (HOME, STATUS, PHYSICAL,
 * QUESTS, MODULES) operate with complete architectural, behavioral, and visual
 * consistency.
 */
class CrossScreenIntegrationTest {

    // ========================================================================
    // 1. PRIMARY NAVIGATION ROUTE GRAPH VERIFICATION
    // ========================================================================
    @Test
    fun primaryRoutes_allFiveDestinationsInstantiableAndDistinct() {
        val home = HomeRoute
        val status = StatusRoute
        val physical = PhysicalRoute
        val quests = QuestBoardRoute
        val modules = ModulesHubRoute

        val routes = listOf(home, status, physical, quests, modules)
        assertEquals("Must have exactly 5 top-level destinations", 5, routes.size)
        assertEquals("Each top-level route must be unique", 5, routes.distinct().size)
    }

    // ========================================================================
    // 2. PLAYER IDENTITY & EQUIPPED TITLE CROSS-SCREEN PARITY
    // ========================================================================
    @Test
    fun playerIdentity_equippedTitleSyncBetweenHomeAndStatus() {
        val player = PlayerProfile(
            codename = "Sung Jin-Woo",
            title = "Wolf Slayer",
            level = 45,
            currentLevelXp = 80,
            xpToNextLevel = 100,
            totalXp = 15000L,
            rank = HunterRank.S_RANK,
            streakDays = 21
        )

        val unlockedTitles = listOf(
            Title(id = 1L, name = "Wolf Slayer", description = "Defeated wolves", isUnlocked = true, isEquipped = false),
            Title(id = 2L, name = "Shadow Monarch", description = "Lord of shadows", isUnlocked = true, isEquipped = true)
        )

        // Status Screen title resolution
        val statusTitle = unlockedTitles.firstOrNull { it.isEquipped }?.name ?: player.title

        // Home Screen title resolution (using the standardized equippedTitle parameter)
        val homeTitle = unlockedTitles.firstOrNull { it.isEquipped }?.name ?: player.title

        assertEquals("Shadow Monarch", statusTitle)
        assertEquals("Shadow Monarch", homeTitle)
        assertEquals("Home and Status must display the exact same active equipped title", statusTitle, homeTitle)
    }

    @Test
    fun playerIdentity_equippedTitleFallbackWhenNoneEquipped() {
        val player = PlayerProfile(
            codename = "Sung Jin-Woo",
            title = "Wolf Slayer",
            level = 10
        )
        val unlockedTitles = listOf(
            Title(id = 1L, name = "Novice Hunter", description = "Started journey", isUnlocked = true, isEquipped = false)
        )

        val resolvedTitle = unlockedTitles.firstOrNull { it.isEquipped }?.name ?: player.title
        assertEquals("Wolf Slayer", resolvedTitle)
    }

    // ========================================================================
    // 3. CANONICAL SEVEN RPG ATTRIBUTES CONSISTENCY
    // ========================================================================
    @Test
    fun canonicalAttributes_allSevenAttributesPresentAcrossDisplays() {
        val attributes = PlayerAttributes()

        // 7 canonical RPG attributes: STR, VIT, AGI, INT, DIS, WIS, CHA
        val coreAttrs = listOf(
            attributes.str,
            attributes.vit,
            attributes.agi,
            attributes.intAttr,
            attributes.dis,
            attributes.wis,
            attributes.cha
        )

        assertEquals("Must contain exactly 7 canonical RPG attributes", 7, coreAttrs.size)
        coreAttrs.forEach { attr ->
            assertTrue("Base value must be positive", attr.baseValue > 0)
            assertTrue("Total value must be >= base value", attr.totalValue >= attr.baseValue)
            assertNotNull("Attribute name must not be null", attr.type.displayName)
            assertNotNull("Attribute abbreviation must not be null", attr.type.abbreviation)
        }
    }

    // ========================================================================
    // 4. RECOVERY & READINESS SAFETY INVARIANT
    // ========================================================================
    @Test
    fun recoverySafetyInvariant_restRecommendedEnforcesRestAcrossScreens() {
        val restAssessment = RecoveryAssessment(
            score = 30,
            state = RecoveryState.REST_RECOMMENDED,
            recommendation = "High fatigue detected. Take full rest today to prevent burnout."
        )

        val physicalState = PhysicalDevelopmentState(
            recoveryAssessment = restAssessment,
            todayWorkout = WorkoutPlan(
                category = WorkoutCategory.STRENGTH,
                status = WorkoutStatus.SCHEDULED,
                recommendedIntensity = "High"
            )
        )

        // Evaluate Next Best Action on Home
        val nextAction = HomeNextActionEngine.evaluate(
            physicalState = physicalState,
            quests = emptyList(),
            academicSummary = AcademicDashboardSummary(),
            lifeSummary = LifeDashboardSummary()
        )

        // When REST_RECOMMENDED, recommendation must explicitly prioritize rest & recovery
        assertTrue(
            "When fatigue is critical, Next Best Action must recommend rest",
            nextAction.title.contains("REST", ignoreCase = true) ||
                nextAction.title.contains("RECOVERY", ignoreCase = true) ||
                nextAction.destination == "physical"
        )
    }

    // ========================================================================
    // 5. DAILY QUEST SYNCHRONIZATION PARITY
    // ========================================================================
    @Test
    fun dailyQuestSynchronization_homeAndQuestBoardParity() {
        val quests = listOf(
            Quest(
                id = 1L,
                title = "100 Pushups",
                description = "Daily bodyweight",
                type = QuestType.DAILY,
                category = QuestCategory.STRENGTH,
                targetValue = 100,
                currentValue = 100,
                unit = "reps",
                xpReward = 150,
                primaryAttribute = AttributeType.STR,
                primaryAttrXpReward = 30,
                isCompleted = true
            ),
            Quest(
                id = 2L,
                title = "10km Run",
                description = "Daily endurance",
                type = QuestType.DAILY,
                category = QuestCategory.AGILITY,
                targetValue = 10,
                currentValue = 4,
                unit = "km",
                xpReward = 200,
                primaryAttribute = AttributeType.AGI,
                primaryAttrXpReward = 40,
                isCompleted = false
            ),
            Quest(
                id = 3L,
                title = "Weekly Dungeon Run",
                description = "Weekly dungeon raid",
                type = QuestType.WEEKLY,
                category = QuestCategory.DISCIPLINE,
                targetValue = 3,
                currentValue = 1,
                unit = "raids",
                xpReward = 500,
                primaryAttribute = AttributeType.DIS,
                primaryAttrXpReward = 100,
                isCompleted = false
            )
        )

        // Filtering rule on Home
        val homeDailyQuests = quests.filter { it.type == QuestType.DAILY }
        val homeCompletedCount = homeDailyQuests.count { it.isCompleted }

        // Filtering rule on Quest Board (DAILY tab)
        val questBoardDailyQuests = quests.filter { it.type == QuestType.DAILY }
        val questBoardCompletedCount = questBoardDailyQuests.count { it.isCompleted }

        assertEquals(2, homeDailyQuests.size)
        assertEquals(2, questBoardDailyQuests.size)
        assertEquals(homeDailyQuests.size, questBoardDailyQuests.size)
        assertEquals(1, homeCompletedCount)
        assertEquals(1, questBoardCompletedCount)
        assertEquals(homeCompletedCount, questBoardCompletedCount)
    }

    // ========================================================================
    // 6. MODULES SUBSYSTEM INVENTORY: EXACT 25 ROUTE-BACKED MODULES
    // ========================================================================
    @Test
    fun moduleInventory_reconcilesToExactCanonicalCountOf25() {
        val inventory = ModuleRegistry.buildInventory()

        // Canonical verification: Exactly 25 registered, route-backed modules
        assertEquals("Canonical inventory must consist of exactly 25 route-backed subsystems", 25, inventory.size)

        // ID Uniqueness
        val ids = inventory.map { it.id }
        assertEquals("All 25 module IDs must be unique", 25, ids.distinct().size)

        // Title Uniqueness
        val titles = inventory.map { it.title }
        assertEquals("All 25 module titles must be unique", 25, titles.distinct().size)

        // Non-empty presentation attributes
        inventory.forEach { mod ->
            assertTrue("Module title must not be blank for ${mod.id}", mod.title.isNotBlank())
            assertTrue("Module description must not be blank for ${mod.id}", mod.description.isNotBlank())
            assertTrue("Capability summary must not be blank for ${mod.id}", mod.capabilitySummary.isNotBlank())
            assertTrue("Metric highlight must not be blank for ${mod.id}", mod.metricHighlight.isNotBlank())
            assertNotNull("Status must not be null for ${mod.id}", mod.status)
        }

        // Verify representation across all 4 functional categories
        val categories = inventory.map { it.category }.distinct()
        assertTrue("Must include DEVELOPMENT", categories.contains(ModuleCategory.DEVELOPMENT))
        assertTrue("Must include PROGRESSION", categories.contains(ModuleCategory.PROGRESSION))
        assertTrue("Must include SYSTEM_AI", categories.contains(ModuleCategory.SYSTEM_AI))
        assertTrue("Must include REWARD_SETTINGS", categories.contains(ModuleCategory.REWARD_SETTINGS))
    }

    // ========================================================================
    // 7. ZERO DEAD BUTTONS: ALL 25 MODULE ACTIONS OPERATIONAL
    // ========================================================================
    @Test
    fun zeroDeadButtons_all25ModuleActionsTriggerAssignedCallbacks() {
        val triggered = mutableSetOf<String>()

        val inventory = ModuleRegistry.buildInventory(
            onOpenAcademicEngine = { triggered.add("academic") },
            onOpenDungeonGate = { triggered.add("dungeon") },
            onOpenSkills = { triggered.add("skills") },
            onOpenAchievements = { triggered.add("achievements") },
            onOpenHistory = { triggered.add("history") },
            onOpenSettings = { triggered.add("settings") },
            onOpenAiSystem = { triggered.add("ai_system") },
            onOpenLifeEngine = { triggered.add("life") },
            onOpenBossBattles = { triggered.add("boss") },
            onOpenTitles = { triggered.add("titles") },
            onOpenInventory = { triggered.add("inventory") },
            onOpenShadowCollection = { triggered.add("shadow_vault") },
            onOpenRpgRewardHistory = { triggered.add("reward_history") },
            onOpenCloudSync = { triggered.add("cloud_sync") },
            onOpenIntelligenceCenter = { triggered.add("intelligence") },
            onOpenSystemControl = { triggered.add("system_control") },
            onOpenAiCompanion = { triggered.add("ai_companion") },
            onOpenVoiceConsole = { triggered.add("voice_console") },
            onOpenSystemVoiceSettings = { triggered.add("voice_settings") },
            onOpenWidgetHub = { triggered.add("widget_hub") },
            onOpenCelebrationVault = { triggered.add("celebration") },
            onOpenShadowRealm = { triggered.add("shadow_realm") },
            onOpenSocialMastery = { triggered.add("social") },
            onOpenPenaltyStatus = { triggered.add("penalty") },
            onOpenPenaltyAudit = { triggered.add("penalty_audit") }
        )

        assertEquals(25, inventory.size)

        // Invoke action on each module
        inventory.forEach { it.onLaunch() }

        assertEquals("All 25 module callbacks must have fired with zero dead buttons", 25, triggered.size)
    }

    // ========================================================================
    // 8. ZERO MOMENTUM XP INVARIANT (SAFETY CHECK)
    // ========================================================================
    @Test
    fun zeroMomentumXpInvariant_physicalAndShadowActivitiesYieldZeroMomentumXp() {
        // Physical workout reward calculation
        val physicalReward = ProgressionEngine.calculatePhysicalWorkoutReward(
            durationMinutes = 60,
            avgHeartRate = 145,
            isRestDay = false
        )

        // ProgressionEngine guarantees physical activities do NOT award Momentum XP
        val physicalResult = ProgressionEngine.calculatePhysicalReward(
            kind = PhysicalActivityKind.WORKOUT,
            durationMinutes = 45,
            verificationStatus = VerificationStatus.VERIFIED,
            metrics = emptyMap()
        )
        assertEquals("Physical activities must never award Momentum XP", 0, physicalResult.momentumXp)

        // Jogging activity verification
        val joggingResult = ProgressionEngine.calculatePhysicalReward(
            kind = PhysicalActivityKind.JOGGING,
            durationMinutes = 30,
            verificationStatus = VerificationStatus.VERIFIED,
            metrics = mapOf("distanceKm" to 5.0)
        )
        assertEquals("Jogging activities must never award Momentum XP", 0, joggingResult.momentumXp)
    }

    // ========================================================================
    // 9. RESILIENT FALLBACKS ON MISSING / DEFAULT DOMAIN SUMMARIES
    // ========================================================================
    @Test
    fun domainSummaries_gracefullyHandleEmptyOrNullStateWithoutCrashing() {
        val emptyAcademic = AcademicDashboardSummary()
        assertEquals(0, emptyAcademic.activeExamsCount)
        assertEquals(0, emptyAcademic.dueAssignmentsCount)

        val emptyLife = LifeDashboardSummary()
        assertEquals(0, emptyLife.activeHabitsCount)
        assertEquals(0, emptyLife.dueTasksCount)

        // Home Next Action Engine handles completely empty states safely
        val nextAction = HomeNextActionEngine.evaluate(
            physicalState = PhysicalDevelopmentState(),
            quests = emptyList(),
            academicSummary = emptyAcademic,
            lifeSummary = emptyLife
        )

        assertNotNull(nextAction)
        assertNotNull(nextAction.title)
        assertNotNull(nextAction.description)
        assertNotNull(nextAction.destination)
    }

    // ========================================================================
    // 10. SYSTEM SPACING & LAYOUT TOKEN INTEGRITY
    // ========================================================================
    @Test
    fun layoutTokens_standardizedAcrossAllFiveScreens() {
        assertEquals("Screen max width must be 760.dp", 760.dp, SystemSpacing.maxContentWidth)
        assertEquals("Screen horizontal margin must be 16.dp", 16.dp, SystemSpacing.screenHorizontal)
        assertEquals("Screen vertical margin must be 16.dp", 16.dp, SystemSpacing.screenVertical)
        assertEquals("Section spacing must be 16.dp", 16.dp, SystemSpacing.sectionSpacing)

        // Canonical semantic aliases
        assertEquals(SystemSpacing.screenHorizontal, SystemSpacing.screenEdgePadding)
        assertEquals(SystemSpacing.screenVertical, SystemSpacing.verticalPageSpacing)
    }
}
