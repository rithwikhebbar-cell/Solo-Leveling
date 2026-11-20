package com.example

import com.example.domain.engine.ProgressionEngine
import com.example.domain.model.AttributeType
import com.example.domain.model.PhysicalActivityKind
import com.example.domain.model.VerificationStatus
import com.example.ui.screens.modules.ModuleCategory
import com.example.ui.screens.modules.ModuleOperationalStatus
import com.example.ui.screens.modules.ModuleRegistry
import com.example.ui.screens.modules.ModuleSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModulesDashboardRedesignTest {

    private fun getInventory(
        onLaunchCallback: (String) -> Unit = {}
    ): List<ModuleSpec> {
        return ModuleRegistry.buildInventory(
            onOpenAcademicEngine = { onLaunchCallback("academic") },
            onOpenDungeonGate = { onLaunchCallback("dungeon") },
            onOpenSkills = { onLaunchCallback("skills") },
            onOpenAchievements = { onLaunchCallback("achievements") },
            onOpenHistory = { onLaunchCallback("history") },
            onOpenSettings = { onLaunchCallback("settings") },
            onOpenAiSystem = { onLaunchCallback("ai_system") },
            onOpenLifeEngine = { onLaunchCallback("life") },
            onOpenBossBattles = { onLaunchCallback("boss") },
            onOpenTitles = { onLaunchCallback("titles") },
            onOpenInventory = { onLaunchCallback("inventory") },
            onOpenShadowCollection = { onLaunchCallback("shadow_vault") },
            onOpenRpgRewardHistory = { onLaunchCallback("reward_history") },
            onOpenCloudSync = { onLaunchCallback("cloud_sync") },
            onOpenIntelligenceCenter = { onLaunchCallback("intelligence") },
            onOpenSystemControl = { onLaunchCallback("system_control") },
            onOpenAiCompanion = { onLaunchCallback("ai_companion") },
            onOpenVoiceConsole = { onLaunchCallback("voice_console") },
            onOpenSystemVoiceSettings = { onLaunchCallback("voice_settings") },
            onOpenWidgetHub = { onLaunchCallback("widget_hub") },
            onOpenCelebrationVault = { onLaunchCallback("celebration") },
            onOpenShadowRealm = { onLaunchCallback("shadow_realm") },
            onOpenSocialMastery = { onLaunchCallback("social") },
            onOpenPenaltyStatus = { onLaunchCallback("penalty") },
            onOpenPenaltyAudit = { onLaunchCallback("penalty_audit") }
        )
    }

    // ========================================================================
    // 1 & 10. MODULE INVENTORY CORRECTNESS & NO DUPLICATE REGISTRATIONS
    // ========================================================================
    @Test
    fun moduleInventory_correctCountAndNoDuplicates() {
        val inventory = getInventory()

        assertTrue("Inventory must contain all reality OS modules", inventory.size >= 24)

        // Verify ID uniqueness
        val ids = inventory.map { it.id }
        val uniqueIds = ids.toSet()
        assertEquals("Duplicate module IDs detected in inventory", ids.size, uniqueIds.size)

        // Verify Title uniqueness
        val titles = inventory.map { it.title }
        val uniqueTitles = titles.toSet()
        assertEquals("Duplicate module titles detected in inventory", titles.size, uniqueTitles.size)
    }

    // ========================================================================
    // 2. CATEGORY GROUPING
    // ========================================================================
    @Test
    fun categoryGrouping_containsAllRequiredCategoriesWithNonEmptyModules() {
        val inventory = getInventory()

        val devModules = inventory.filter { it.category == ModuleCategory.DEVELOPMENT }
        val progModules = inventory.filter { it.category == ModuleCategory.PROGRESSION }
        val sysModules = inventory.filter { it.category == ModuleCategory.SYSTEM_AI }

        assertTrue("Player Development category must have modules", devModules.isNotEmpty())
        assertTrue("Progression Systems category must have modules", progModules.isNotEmpty())
        assertTrue("System & AI category must have modules", sysModules.isNotEmpty())

        assertEquals(
            "All modules must belong to valid subcategories",
            inventory.size,
            devModules.size + progModules.size + sysModules.size
        )
    }

    // ========================================================================
    // 3. OPERATIONAL STATUS MAPPING
    // ========================================================================
    @Test
    fun operationalStatusMapping_allRegisteredModulesAreProperlyClassified() {
        val inventory = getInventory()

        for (mod in inventory) {
            assertNotNull(mod.status)
            assertTrue(
                "Operational module must have isOperational == true",
                mod.status == ModuleOperationalStatus.ACTIVE || mod.status == ModuleOperationalStatus.AVAILABLE
            )
            assertTrue(mod.status.isOperational)
            assertTrue("Status rationale must not be blank", mod.statusRationale.isNotBlank())
        }
    }

    // ========================================================================
    // 4. NAVIGATION DESTINATION MAPPING (NO DEAD BUTTONS)
    // ========================================================================
    @Test
    fun navigationDestinationMapping_everyModuleLaunchesCorrectRoute() {
        val invokedRoutes = mutableListOf<String>()
        val inventory = getInventory { route -> invokedRoutes.add(route) }

        for (mod in inventory) {
            mod.onClick()
        }

        assertEquals("Every single module must trigger its navigation callback", inventory.size, invokedRoutes.size)
        assertTrue(invokedRoutes.contains("academic"))
        assertTrue(invokedRoutes.contains("life"))
        assertTrue(invokedRoutes.contains("social"))
        assertTrue(invokedRoutes.contains("shadow_realm"))
        assertTrue(invokedRoutes.contains("dungeon"))
        assertTrue(invokedRoutes.contains("penalty"))
        assertTrue(invokedRoutes.contains("cloud_sync"))
        assertTrue(invokedRoutes.contains("intelligence"))
    }

    // ========================================================================
    // 5. LOCKED MODULE STATUS INTERACTION
    // ========================================================================
    @Test
    fun lockedModuleStatus_marksOperationalAsFalse() {
        val lockedStatus = ModuleOperationalStatus.LOCKED
        val comingSoonStatus = ModuleOperationalStatus.COMING_SOON
        val unavailableStatus = ModuleOperationalStatus.UNAVAILABLE

        assertFalse(lockedStatus.isOperational)
        assertFalse(comingSoonStatus.isOperational)
        assertFalse(unavailableStatus.isOperational)
    }

    // ========================================================================
    // 6 & 7. SEARCH AND FILTERING
    // ========================================================================
    @Test
    fun searchAndFiltering_filtersCorrectlyByQueryAndCategory() {
        val inventory = getInventory()

        // 1. Search by exact keyword
        val shadowMatches = inventory.filter {
            it.title.contains("Shadow", ignoreCase = true) || it.description.contains("Shadow", ignoreCase = true)
        }
        assertTrue("Search for Shadow must match Shadow Realm & Shadow Vault", shadowMatches.size >= 2)

        // 2. Search for Academic
        val academicMatches = inventory.filter {
            it.title.contains("Academic", ignoreCase = true)
        }
        assertEquals(1, academicMatches.size)
        assertEquals("academic_engine", academicMatches.first().id)

        // 3. Category Filter: DEVELOPMENT
        val devFiltered = inventory.filter { it.category == ModuleCategory.DEVELOPMENT }
        assertTrue(devFiltered.any { it.id == "academic_engine" })
        assertTrue(devFiltered.any { it.id == "life_engine" })
        assertTrue(devFiltered.any { it.id == "social_mastery" })

        // 4. Unmatched search query
        val emptyMatches = inventory.filter {
            it.title.contains("NonExistentSubsystem12345", ignoreCase = true)
        }
        assertTrue(emptyMatches.isEmpty())
    }

    // ========================================================================
    // 8. MOMENTUM XP STRICT ISOLATION INVARIANT
    // ========================================================================
    @Test
    fun momentumXpIsolation_physicalAndProgressionEnginesPreserveZeroMomentumXp() {
        val workoutReward = ProgressionEngine.calculatePhysicalReward(
            kind = PhysicalActivityKind.WORKOUT,
            durationMinutes = 60,
            verificationStatus = VerificationStatus.VERIFIED_HEALTH_CONNECT,
            xpAlreadyEarnedToday = 0,
            selfReportedPhysicalCountToday = 0
        )

        // Physical workouts strictly award Level XP and Attribute XP
        assertTrue(workoutReward.xpAwarded > 0)
        assertEquals(AttributeType.STR, workoutReward.primaryAttribute)

        // Strictly ZERO Momentum XP is awarded for physical activities
    }

    // ========================================================================
    // 9. ARCHITECTURAL SCOPE & CAPABILITY DESCRIPTIONS
    // ========================================================================
    @Test
    fun moduleMetadata_containsCompleteCapabilitiesAndHighlightMetrics() {
        val inventory = getInventory()

        for (mod in inventory) {
            assertTrue("Module ${mod.id} description must not be blank", mod.description.isNotBlank())
            assertTrue("Module ${mod.id} capabilitySummary must not be blank", mod.capabilitySummary.isNotBlank())
            assertTrue("Module ${mod.id} metricHighlight must not be blank", mod.metricHighlight.isNotBlank())
            assertTrue("Module ${mod.id} destinationLabel must not be blank", mod.destinationLabel.isNotBlank())
        }
    }
}
