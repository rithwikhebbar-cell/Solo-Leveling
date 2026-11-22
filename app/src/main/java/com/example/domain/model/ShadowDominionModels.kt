package com.example.domain.model

import com.example.domain.model.DEFAULT_SHADOW_ACCOUNT_OWNER_ID

// ============================================================================
// PHASE 6.6 — SHADOW ARMY DOMINION & REALM EXPANSION DOMAIN MODELS
// ============================================================================

/**
 * Progression stages of the Shadow Monarch's Dominion.
 * Stages advance deterministically through territory control, citadel advancement,
 * infrastructure building, and completed research projects.
 */
enum class ShadowDominionStage(
    val stageName: String,
    val description: String,
    val requiredTerritories: Int,
    val requiredCitadelLevel: Int,
    val requiredStructures: Int,
    val requiredResearchCount: Int
) {
    DORMANT_REALM(
        stageName = "Dormant Realm",
        description = "The dormant remnants of the Monarch's ancestral shadow domain.",
        requiredTerritories = 0,
        requiredCitadelLevel = 1,
        requiredStructures = 0,
        requiredResearchCount = 0
    ),
    AWAKENED_DOMINION(
        stageName = "Awakened Dominion",
        description = "Shadow energy surges as the first territory is claimed and the Citadel awakens.",
        requiredTerritories = 1,
        requiredCitadelLevel = 1,
        requiredStructures = 1,
        requiredResearchCount = 0
    ),
    EXPANDING_DOMINION(
        stageName = "Expanding Dominion",
        description = "Multiple territories secured with active resource extraction and infrastructure.",
        requiredTerritories = 2,
        requiredCitadelLevel = 2,
        requiredStructures = 3,
        requiredResearchCount = 1
    ),
    ESTABLISHED_DOMINION(
        stageName = "Established Dominion",
        description = "A cohesive network of fortified territories backed by stationed Shadow garrisons.",
        requiredTerritories = 4,
        requiredCitadelLevel = 3,
        requiredStructures = 6,
        requiredResearchCount = 3
    ),
    SOVEREIGN_DOMINION(
        stageName = "Sovereign Dominion",
        description = "An impenetrable realm anchored by advanced research, deep wards, and elite armies.",
        requiredTerritories = 6,
        requiredCitadelLevel = 4,
        requiredStructures = 10,
        requiredResearchCount = 6
    ),
    SHADOW_MONARCHS_EMPIRE(
        stageName = "Shadow Monarch's Empire",
        description = "The absolute apex of the Shadow Realm — an eternal empire spanning all domains.",
        requiredTerritories = 7,
        requiredCitadelLevel = 5,
        requiredStructures = 15,
        requiredResearchCount = 10
    );

    fun next(): ShadowDominionStage? {
        val all = entries
        val idx = all.indexOf(this)
        return if (idx in 0 until all.lastIndex) all[idx + 1] else null
    }
}

/**
 * Citadel upgrade wings managing strategic administrative systems.
 */
enum class CitadelUpgradeType(val label: String, val description: String) {
    COMMAND_HALL(
        label = "Command Hall",
        description = "Central administrative wing enhancing Dominion authority and garrison efficiency."
    ),
    SHADOW_NEXUS(
        label = "Shadow Nexus",
        description = "Resonance nexus deepens army coordination and boosts stationed readiness."
    ),
    RESOURCE_VAULT(
        label = "Resource Vault",
        description = "Fortified vault increasing storage resilience and material processing yield."
    ),
    DEFENSIVE_CORE(
        label = "Defensive Core",
        description = "Arcane barrier core projecting ward strength across all controlled territories."
    ),
    EXPLORATION_GATE(
        label = "Exploration Gate",
        description = "Dimensional portal accelerating scouting expeditions and fog-of-war clearance."
    )
}

/**
 * Original territory categories within the Shadow Realm.
 */
enum class ShadowTerritoryCategory(
    val label: String,
    val description: String,
    val baseDefenseRating: Int,
    val primaryResourceAffinity: ShadowResourceType
) {
    SHADOW_HEARTLAND(
        label = "Shadow Heartland",
        description = "The central sanctuary immediately surrounding the Monarch's Citadel.",
        baseDefenseRating = 50,
        primaryResourceAffinity = ShadowResourceType.SHADOW_ESSENCE
    ),
    ABYSSAL_FOREST(
        label = "Abyssal Forest",
        description = "Ancient spectral timberland dense with shadowy flora and primordial materials.",
        baseDefenseRating = 30,
        primaryResourceAffinity = ShadowResourceType.ABYSSAL_MATERIALS
    ),
    OBSIDIAN_MOUNTAINS(
        label = "Obsidian Mountains",
        description = "Jagged volcanic peaks rich in dense shadow minerals and crystalline deposits.",
        baseDefenseRating = 45,
        primaryResourceAffinity = ShadowResourceType.SHADOW_CRYSTALS
    ),
    FORGOTTEN_RUINS(
        label = "Forgotten Ruins",
        description = "Desolate monolithic ruins preserving ancient Monarch relics and forgotten glyphs.",
        baseDefenseRating = 35,
        primaryResourceAffinity = ShadowResourceType.MONARCH_FRAGMENTS
    ),
    CRYSTAL_WASTELANDS(
        label = "Crystal Wastelands",
        description = "Expanses of resonating crystals that amplify shadow arcane power.",
        baseDefenseRating = 40,
        primaryResourceAffinity = ShadowResourceType.EVOLUTION_CORES
    ),
    ETERNAL_NIGHT_PLAINS(
        label = "Eternal Night Plains",
        description = "Vast darkened steppes where shadows roam freely under a moonless sky.",
        baseDefenseRating = 25,
        primaryResourceAffinity = ShadowResourceType.SHADOW_ESSENCE
    ),
    MONARCHS_FRONTIER(
        label = "Monarch's Frontier",
        description = "The outermost boundary of the Dominion buffering against the dimensional void.",
        baseDefenseRating = 60,
        primaryResourceAffinity = ShadowResourceType.SHADOW_CRYSTALS
    )
}

/**
 * Discrete state machine for territory discovery and ownership.
 */
enum class ShadowTerritoryState(val label: String) {
    UNKNOWN("Unknown"),
    DISCOVERED("Discovered"),
    EXPLORING("Exploring"),
    CONTESTED("Contested"),
    CONTROLLED("Controlled"),
    DEVELOPING("Developing"),
    FORTIFIED("Fortified");

    companion object {
        fun isValidTransition(from: ShadowTerritoryState, to: ShadowTerritoryState): Boolean {
            if (from == to) return true
            return when (from) {
                UNKNOWN -> to == DISCOVERED
                DISCOVERED -> to == EXPLORING
                EXPLORING -> to == CONTESTED || to == CONTROLLED
                CONTESTED -> to == CONTROLLED
                CONTROLLED -> to == DEVELOPING || to == FORTIFIED
                DEVELOPING -> to == FORTIFIED
                FORTIFIED -> false
            }
        }
    }
}

/**
 * Functional structures built upon controlled territories.
 */
enum class ShadowInfrastructureType(
    val label: String,
    val description: String,
    val maxLevel: Int,
    val resourceGenerated: ShadowResourceType?
) {
    SHADOW_BARRACKS(
        label = "Shadow Barracks",
        description = "Stationing facility providing garrison quarters and operational readiness.",
        maxLevel = 5,
        resourceGenerated = null
    ),
    RESOURCE_EXTRACTOR(
        label = "Resource Extractor",
        description = "Siphons ambient domain energy to produce Shadow Essence or Crystals over time.",
        maxLevel = 5,
        resourceGenerated = ShadowResourceType.SHADOW_ESSENCE
    ),
    DEFENSE_TOWER(
        label = "Defense Tower",
        description = "Projects protective shadow barriers, bolstering territory defensive rating.",
        maxLevel = 5,
        resourceGenerated = null
    ),
    RESEARCH_SANCTUM(
        label = "Research Sanctum",
        description = "Arcane laboratory conducting architectural and logistics research.",
        maxLevel = 5,
        resourceGenerated = null
    ),
    SHADOW_GATE(
        label = "Shadow Gate",
        description = "Dimensional rift conduit providing instant troop mobilization.",
        maxLevel = 3,
        resourceGenerated = null
    ),
    MONARCHS_SHRINE(
        label = "Monarch's Shrine",
        description = "Monument radiating the Monarch's regal dominion across the territory.",
        maxLevel = 3,
        resourceGenerated = null
    )
}

/**
 * Construction or upgrade state of an infrastructure item.
 */
enum class InfrastructureStatus(val label: String) {
    CONSTRUCTING("Constructing"),
    ACTIVE("Active"),
    UPGRADING("Upgrading")
}

/**
 * Research and technology trees for the Shadow Dominion.
 */
enum class ShadowResearchBranch(val label: String, val description: String) {
    DOMINION_ARCHITECTURE(
        label = "Dominion Architecture",
        description = "Advanced engineering unlocking upgraded building tiers and structural durability."
    ),
    SHADOW_LOGISTICS(
        label = "Shadow Logistics",
        description = "Supply lines and conduit networks boosting resource extraction throughput."
    ),
    TERRITORIAL_DEFENSE(
        label = "Territorial Defense",
        description = "Warding doctrines, barrier reinforcement, and defensive garrison coordination."
    ),
    RESOURCE_MANAGEMENT(
        label = "Resource Management",
        description = "Refinement techniques minimizing upgrade costs and unlocking exotic abyssal harvesting."
    ),
    EXPLORATION_TECHNOLOGY(
        label = "Exploration Technology",
        description = "Deep-fog navigation and scouting speed across uncharted frontier zones."
    ),
    ARMY_COORDINATION(
        label = "Army Coordination",
        description = "Command link doctrines enhancing multi-squad synchronization and garrison readiness."
    )
}

/**
 * Categorization for persisted Dominion activity records.
 */
enum class DominionActivityType(val label: String) {
    DOMINION_INITIALIZED("Dominion Initialized"),
    TERRITORY_DISCOVERED("Territory Discovered"),
    TERRITORY_CLAIMED("Territory Claimed"),
    INFRASTRUCTURE_CONSTRUCTED("Infrastructure Constructed"),
    INFRASTRUCTURE_UPGRADED("Infrastructure Upgraded"),
    RESOURCES_COLLECTED("Resources Collected"),
    GARRISON_STATIONED("Garrison Stationed"),
    GARRISON_RECALLED("Garrison Recalled"),
    RESEARCH_STARTED("Research Started"),
    RESEARCH_COMPLETED("Research Completed"),
    CITADEL_UPGRADED("Citadel Upgraded")
}

// ============================================================================
// DOMAIN DATA CLASSES
// ============================================================================

/**
 * Persistent central entity for a player's Shadow Dominion.
 */
data class ShadowDominion(
    val dominionId: String,
    val accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
    val dominionName: String = "Domain of the Shadow Monarch",
    val dominionLevel: Int = 1,
    val developmentStage: ShadowDominionStage = ShadowDominionStage.DORMANT_REALM,
    val citadelLevel: Int = 1,
    val controlledTerritoriesCount: Int = 1,
    val infrastructureCount: Int = 0,
    val totalDevelopmentEvents: Int = 0,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

/**
 * Represents a geographical domain territory.
 */
data class ShadowTerritory(
    val territoryId: String,
    val accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
    val name: String,
    val description: String,
    val category: ShadowTerritoryCategory,
    val state: ShadowTerritoryState,
    val developmentLevel: Int = 1,
    val defenseRating: Int = 50,
    val assignedSquadId: String? = null,
    val discoveredAtEpochMs: Long? = null,
    val claimedAtEpochMs: Long? = null,
    val explorationProgressPercent: Int = 0,
    val explorationEndsAtEpochMs: Long? = null
)

/**
 * Specific infrastructure built on a territory.
 */
data class ShadowInfrastructure(
    val structureId: String,
    val accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
    val territoryId: String,
    val structureType: ShadowInfrastructureType,
    val level: Int = 1,
    val status: InfrastructureStatus = InfrastructureStatus.ACTIVE,
    val lastResourceCollectionEpochMs: Long = System.currentTimeMillis(),
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

/**
 * Garrison stationing a squad to defend a territory.
 */
data class ShadowGarrison(
    val garrisonId: String,
    val accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
    val territoryId: String,
    val squadId: String,
    val assignedAtEpochMs: Long = System.currentTimeMillis(),
    val defensiveRating: Int = 50,
    val readinessStatus: String = "STATIONED"
)

/**
 * Completed or active research project.
 */
data class ShadowDominionResearch(
    val researchId: String,
    val accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
    val branch: ShadowResearchBranch,
    val projectKey: String,
    val title: String,
    val description: String,
    val level: Int = 1,
    val isCompleted: Boolean = false,
    val startedAtEpochMs: Long? = null,
    val completedAtEpochMs: Long? = null,
    val unlockedEffectsSummary: String = ""
)

/**
 * Persistent immutable record of an activity in the Dominion.
 */
data class ShadowDominionActivity(
    val activityId: String,
    val accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
    val activityType: DominionActivityType,
    val title: String,
    val description: String,
    val timestampEpochMs: Long = System.currentTimeMillis(),
    val entityReferenceId: String = ""
)

/**
 * Complete inspection detail for a single territory.
 */
data class DominionTerritoryDetail(
    val territory: ShadowTerritory,
    val structures: List<ShadowInfrastructure>,
    val garrison: ShadowGarrison?,
    val garrisonSquad: ShadowSquad?,
    val defenseAssessment: DominionDefensiveAssessment
)

/**
 * Defensive readiness assessment for a territory.
 */
data class DominionDefensiveAssessment(
    val territoryId: String,
    val baseDefenseRating: Int,
    val infrastructureBonus: Int,
    val garrisonRating: Int,
    val totalDefenseRating: Int,
    val securityLevel: String, // "SECURE", "DEFENDED", "VULNERABLE", "EXPOSED"
    val alerts: List<String>,
    val recommendations: List<String>
)

/**
 * High-level overview of the player's Shadow Dominion.
 */
data class DominionOverview(
    val dominion: ShadowDominion,
    val citadelUpgrades: Map<CitadelUpgradeType, Int>,
    val resourceBalances: Map<ShadowResourceType, Int>,
    val territorySummary: Map<ShadowTerritoryState, Int>,
    val totalResourceYieldPerHour: Int,
    val activeGarrisonsCount: Int,
    val alerts: List<String>
)

/**
 * Read-only tactical briefing produced by the Dominion AI Advisor.
 */
data class DominionAiBriefing(
    val briefingId: String,
    val dominionStage: ShadowDominionStage,
    val summary: String,
    val strategicPriority: String,
    val recommendedActions: List<String>,
    val generatedAtEpochMs: Long = System.currentTimeMillis()
)

// ============================================================================
// DOMINION CONFIGURATION & COSTS
// ============================================================================

object ShadowDominionConfig {
    const val MAX_CITADEL_LEVEL = 5
    const val RESOURCE_COLLECTION_CAP_MS = 24 * 3600_000L // 24 hours max accumulation

    // Initial territories to seed upon awakening
    val INITIAL_TERRITORIES = listOf(
        ShadowTerritory(
            territoryId = "territory_heartland",
            name = "Shadow Heartland",
            description = "The central stronghold surrounding the Citadel of Shadows.",
            category = ShadowTerritoryCategory.SHADOW_HEARTLAND,
            state = ShadowTerritoryState.CONTROLLED,
            defenseRating = 60
        ),
        ShadowTerritory(
            territoryId = "territory_abyssal_forest",
            name = "Abyssal Forest",
            description = "Ancient gloom shrouded in mist; rich in dark fibers.",
            category = ShadowTerritoryCategory.ABYSSAL_FOREST,
            state = ShadowTerritoryState.DISCOVERED,
            defenseRating = 30
        ),
        ShadowTerritory(
            territoryId = "territory_obsidian_mountains",
            name = "Obsidian Mountains",
            description = "Volcanic peaks with crystal veins.",
            category = ShadowTerritoryCategory.OBSIDIAN_MOUNTAINS,
            state = ShadowTerritoryState.DISCOVERED,
            defenseRating = 45
        ),
        ShadowTerritory(
            territoryId = "territory_forgotten_ruins",
            name = "Forgotten Ruins",
            description = "Monolithic remnants of an ancient throne.",
            category = ShadowTerritoryCategory.FORGOTTEN_RUINS,
            state = ShadowTerritoryState.UNKNOWN,
            defenseRating = 35
        ),
        ShadowTerritory(
            territoryId = "territory_crystal_wastelands",
            name = "Crystal Wastelands",
            description = "Resonating fields of raw crystal magic.",
            category = ShadowTerritoryCategory.CRYSTAL_WASTELANDS,
            state = ShadowTerritoryState.UNKNOWN,
            defenseRating = 40
        ),
        ShadowTerritory(
            territoryId = "territory_eternal_night",
            name = "Eternal Night Plains",
            description = "Dark endless steppes with roaming shadow beasts.",
            category = ShadowTerritoryCategory.ETERNAL_NIGHT_PLAINS,
            state = ShadowTerritoryState.UNKNOWN,
            defenseRating = 25
        ),
        ShadowTerritory(
            territoryId = "territory_monarchs_frontier",
            name = "Monarch's Frontier",
            description = "The far threshold bordering the dimensional boundary.",
            category = ShadowTerritoryCategory.MONARCHS_FRONTIER,
            state = ShadowTerritoryState.UNKNOWN,
            defenseRating = 60
        )
    )

    // Base costs for Citadel upgrades per level
    fun getCitadelUpgradeCost(type: CitadelUpgradeType, currentLevel: Int): Map<ShadowResourceType, Int> {
        val multiplier = currentLevel + 1
        return when (type) {
            CitadelUpgradeType.COMMAND_HALL -> mapOf(
                ShadowResourceType.SHADOW_ESSENCE to 200 * multiplier,
                ShadowResourceType.MONARCH_FRAGMENTS to 20 * multiplier
            )
            CitadelUpgradeType.SHADOW_NEXUS -> mapOf(
                ShadowResourceType.SHADOW_ESSENCE to 150 * multiplier,
                ShadowResourceType.EVOLUTION_CORES to 2 * multiplier
            )
            CitadelUpgradeType.RESOURCE_VAULT -> mapOf(
                ShadowResourceType.SHADOW_ESSENCE to 250 * multiplier,
                ShadowResourceType.SHADOW_CRYSTALS to 15 * multiplier
            )
            CitadelUpgradeType.DEFENSIVE_CORE -> mapOf(
                ShadowResourceType.SHADOW_ESSENCE to 180 * multiplier,
                ShadowResourceType.ABYSSAL_MATERIALS to 10 * multiplier
            )
            CitadelUpgradeType.EXPLORATION_GATE -> mapOf(
                ShadowResourceType.SHADOW_ESSENCE to 220 * multiplier,
                ShadowResourceType.SHADOW_CRYSTALS to 10 * multiplier,
                ShadowResourceType.MONARCH_FRAGMENTS to 10 * multiplier
            )
        }
    }

    // Base costs for Infrastructure construction and upgrades
    fun getInfrastructureCost(type: ShadowInfrastructureType, targetLevel: Int): Map<ShadowResourceType, Int> {
        val multiplier = targetLevel.coerceAtLeast(1)
        return when (type) {
            ShadowInfrastructureType.SHADOW_BARRACKS -> mapOf(
                ShadowResourceType.SHADOW_ESSENCE to 100 * multiplier,
                ShadowResourceType.MONARCH_FRAGMENTS to 5 * multiplier
            )
            ShadowInfrastructureType.RESOURCE_EXTRACTOR -> mapOf(
                ShadowResourceType.SHADOW_ESSENCE to 150 * multiplier,
                ShadowResourceType.SHADOW_CRYSTALS to 5 * multiplier
            )
            ShadowInfrastructureType.DEFENSE_TOWER -> mapOf(
                ShadowResourceType.SHADOW_ESSENCE to 120 * multiplier,
                ShadowResourceType.ABYSSAL_MATERIALS to 5 * multiplier
            )
            ShadowInfrastructureType.RESEARCH_SANCTUM -> mapOf(
                ShadowResourceType.SHADOW_ESSENCE to 200 * multiplier,
                ShadowResourceType.EVOLUTION_CORES to 2 * multiplier
            )
            ShadowInfrastructureType.SHADOW_GATE -> mapOf(
                ShadowResourceType.SHADOW_ESSENCE to 300 * multiplier,
                ShadowResourceType.SHADOW_CRYSTALS to 15 * multiplier
            )
            ShadowInfrastructureType.MONARCHS_SHRINE -> mapOf(
                ShadowResourceType.SHADOW_ESSENCE to 250 * multiplier,
                ShadowResourceType.MONARCH_FRAGMENTS to 25 * multiplier
            )
        }
    }

    // Resource production rate per hour for Resource Extractors
    fun getExtractorHourlyYield(level: Int): Int = 15 + (level * 10) // 25 at lvl 1, 35 at lvl 2, etc.

    // Exploration base duration in ms
    const val EXPLORATION_DURATION_MS = 60_000L // 1 minute for fast testing, persistent
}
