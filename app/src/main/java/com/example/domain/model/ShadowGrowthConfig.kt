package com.example.domain.model

// ============================================================================
// PHASE 6.3 — SHADOW GROWTH & EVOLUTION CONFIGURATION
// Centralized, deterministic progression rules, stage level caps, resource costs,
// attribute growth formulas, and evolution state evaluations.
// ============================================================================

object ShadowGrowthConfig {

    // ------------------------------------------------------------------------
    // STAGE LEVEL CAPS
    // A shadow cannot level up beyond the cap of its current evolution stage
    // until the player explicitly evolves it in the Evolution Chamber.
    // ------------------------------------------------------------------------
    val STAGE_LEVEL_CAPS: Map<ShadowEvolutionStage, Int> = mapOf(
        ShadowEvolutionStage.NEWLY_EXTRACTED to 10,
        ShadowEvolutionStage.AWAKENED to 25,
        ShadowEvolutionStage.ELITE to 45,
        ShadowEvolutionStage.COMMANDER to 70,
        ShadowEvolutionStage.GENERAL to 90,
        ShadowEvolutionStage.MONARCH_GUARD to 100
    )

    fun getStageLevelCap(stage: ShadowEvolutionStage): Int =
        STAGE_LEVEL_CAPS[stage] ?: 10

    // ------------------------------------------------------------------------
    // AUTHORITATIVE XP SOURCE VALUES
    // XP must originate solely from verified gameplay events. Direct UI manipulation
    // and manual training buttons are strictly prohibited.
    // ------------------------------------------------------------------------
    const val XP_DUNGEON_COMPLETION: Int = 120
    const val XP_BOSS_DEFEAT: Int = 250
    const val XP_QUEST_COMPLETION: Int = 150
    const val XP_MILESTONE: Int = 100

    // ------------------------------------------------------------------------
    // EVOLUTION STAGE RESOURCE COSTS
    // Strict transactional costs consumed during explicit player evolution.
    // ------------------------------------------------------------------------
    val EVOLUTION_STAGE_COSTS: Map<ShadowEvolutionStage, Map<ShadowResourceType, Int>> = mapOf(
        ShadowEvolutionStage.AWAKENED to mapOf(
            ShadowResourceType.SHADOW_ESSENCE to 50,
            ShadowResourceType.EVOLUTION_CORES to 1
        ),
        ShadowEvolutionStage.ELITE to mapOf(
            ShadowResourceType.SHADOW_ESSENCE to 150,
            ShadowResourceType.EVOLUTION_CORES to 3,
            ShadowResourceType.MONARCH_FRAGMENTS to 20
        ),
        ShadowEvolutionStage.COMMANDER to mapOf(
            ShadowResourceType.SHADOW_ESSENCE to 300,
            ShadowResourceType.EVOLUTION_CORES to 5,
            ShadowResourceType.MONARCH_FRAGMENTS to 50,
            ShadowResourceType.SHADOW_CRYSTALS to 10
        ),
        ShadowEvolutionStage.GENERAL to mapOf(
            ShadowResourceType.SHADOW_ESSENCE to 600,
            ShadowResourceType.EVOLUTION_CORES to 10,
            ShadowResourceType.MONARCH_FRAGMENTS to 100,
            ShadowResourceType.SHADOW_CRYSTALS to 25
        ),
        ShadowEvolutionStage.MONARCH_GUARD to mapOf(
            ShadowResourceType.SHADOW_ESSENCE to 1200,
            ShadowResourceType.EVOLUTION_CORES to 20,
            ShadowResourceType.MONARCH_FRAGMENTS to 250,
            ShadowResourceType.SHADOW_CRYSTALS to 50
        )
    )

    fun getEvolutionCosts(targetStage: ShadowEvolutionStage): Map<ShadowResourceType, Int> =
        EVOLUTION_STAGE_COSTS[targetStage] ?: emptyMap()

    // ------------------------------------------------------------------------
    // EVOLUTION STAGE STAT BONUS INCREMENTS
    // Incremental attributes granted upon achieving each evolution stage.
    // ------------------------------------------------------------------------
    val STAGE_STAT_INCREMENTS: Map<ShadowEvolutionStage, ShadowStats> = mapOf(
        ShadowEvolutionStage.NEWLY_EXTRACTED to ShadowStats(0, 0, 0, 0, 0, 0),
        ShadowEvolutionStage.AWAKENED to ShadowStats(5, 5, 5, 5, 5, 5),
        ShadowEvolutionStage.ELITE to ShadowStats(7, 7, 7, 7, 7, 7),
        ShadowEvolutionStage.COMMANDER to ShadowStats(10, 10, 10, 10, 10, 10),
        ShadowEvolutionStage.GENERAL to ShadowStats(13, 13, 13, 13, 13, 13),
        ShadowEvolutionStage.MONARCH_GUARD to ShadowStats(15, 15, 15, 15, 15, 15)
    )

    fun getStageStatBonusIncrement(stage: ShadowEvolutionStage): ShadowStats =
        STAGE_STAT_INCREMENTS[stage] ?: ShadowStats(0, 0, 0, 0, 0, 0)

    // ------------------------------------------------------------------------
    // DETERMINISTIC CLASS-BASED STAT GROWTH PER LEVEL
    // Reflects each tactical class's battlefield role and specialization.
    // ------------------------------------------------------------------------
    fun getStatGrowthPerLevel(shadowClass: ShadowClass): ShadowStats {
        return when (shadowClass) {
            ShadowClass.VANGUARD -> ShadowStats(
                power = 2,
                endurance = 2,
                focus = 1,
                discipline = 1,
                wisdom = 0,
                adaptability = 1
            )
            ShadowClass.GUARDIAN -> ShadowStats(
                power = 1,
                endurance = 2,
                focus = 1,
                discipline = 2,
                wisdom = 0,
                adaptability = 1
            )
            ShadowClass.RANGER -> ShadowStats(
                power = 1,
                endurance = 1,
                focus = 2,
                discipline = 1,
                wisdom = 0,
                adaptability = 2
            )
            ShadowClass.ARCANIST -> ShadowStats(
                power = 0,
                endurance = 1,
                focus = 2,
                discipline = 1,
                wisdom = 2,
                adaptability = 1
            )
            ShadowClass.TACTICIAN -> ShadowStats(
                power = 1,
                endurance = 1,
                focus = 2,
                discipline = 1,
                wisdom = 2,
                adaptability = 0
            )
            ShadowClass.SUPPORT -> ShadowStats(
                power = 0,
                endurance = 1,
                focus = 1,
                discipline = 2,
                wisdom = 2,
                adaptability = 1
            )
        }
    }
}

// ----------------------------------------------------------------------------
// EVOLUTION EVALUATION DOMAIN MODELS
// ----------------------------------------------------------------------------

enum class ShadowEvolutionState(val label: String) {
    READY_TO_EVOLVE("Ready to Evolve"),
    INSUFFICIENT_LEVEL("Level Requirement Not Met"),
    INSUFFICIENT_RANK("Rank Requirement Not Met"),
    INSUFFICIENT_RESOURCES("Insufficient Resources"),
    REQUIREMENTS_NOT_MET("Requirements Not Met"),
    MAX_STAGE_REACHED("Maximum Evolution Stage Reached"),
    LOCKED("Evolution Locked")
}

data class ShadowEvolutionEvaluation(
    val shadowId: String,
    val shadowName: String,
    val currentStage: ShadowEvolutionStage,
    val targetStage: ShadowEvolutionStage?,
    val state: ShadowEvolutionState,
    val isEligible: Boolean,
    val currentLevel: Int,
    val requiredLevel: Int,
    val currentRank: ShadowRank,
    val requiredRank: ShadowRank,
    val resourceCosts: Map<ShadowResourceType, Int>,
    val currentBalances: Map<ShadowResourceType, Int>,
    val blockers: List<String>,
    val statPreview: ShadowStats?
)

data class ShadowGrowthOverview(
    val totalShadows: Int = 0,
    val readyToEvolveCount: Int = 0,
    val eligibleToLevelUpCount: Int = 0,
    val averageLevel: Float = 0f,
    val highestLevelShadow: Shadow? = null,
    val stageDistribution: Map<ShadowEvolutionStage, Int> = emptyMap()
)
