package com.example.domain.model

// ============================================================================
// MILESTONE 12 — PHASE 6.4: SHADOW BOND & LOYALTY CONFIGURATION
// Centralized, deterministic thresholds and models for Shadow relationships.
// Progression is completely isolated from Player XP, Momentum XP, and Shadow XP.
// ============================================================================

enum class ShadowRelationshipStage(
    val stageName: String,
    val order: Int,
    val minBondLevel: Int,
    val minBondPoints: Long,
    val minLoyalty: Int,
    val minTrust: Int,
    val title: String,
    val description: String
) {
    UNFAMILIAR(
        stageName = "Unfamiliar",
        order = 1,
        minBondLevel = 1,
        minBondPoints = 0L,
        minLoyalty = 0,
        minTrust = 0,
        title = "Shadow Neophyte",
        description = "A newly bound shadow standing cautiously under the Monarch's presence."
    ),
    RECOGNIZED(
        stageName = "Recognized",
        order = 2,
        minBondLevel = 2,
        minBondPoints = 100L,
        minLoyalty = 25,
        minTrust = 20,
        title = "Sworn Legionnaire",
        description = "The shadow acknowledges the Monarch's authority and responds with disciplined readiness."
    ),
    TRUSTING(
        stageName = "Trusting",
        order = 3,
        minBondLevel = 4,
        minBondPoints = 350L,
        minLoyalty = 50,
        minTrust = 45,
        title = "Trusted Vanguard",
        description = "Shared trials have built mutual reliability. The shadow anticipates commands seamlessly."
    ),
    LOYAL(
        stageName = "Loyal",
        order = 4,
        minBondLevel = 6,
        minBondPoints = 800L,
        minLoyalty = 70,
        minTrust = 65,
        title = "Loyal Retainer",
        description = "Unquestioning allegiance forged through adversity. The shadow guards the Monarch without hesitation."
    ),
    DEVOTED(
        stageName = "Devoted",
        order = 5,
        minBondLevel = 8,
        minBondPoints = 1600L,
        minLoyalty = 85,
        minTrust = 80,
        title = "Devoted Knight",
        description = "Bound by profound spiritual allegiance. The shadow draws resolute pride from the Monarch's will."
    ),
    UNBREAKABLE(
        stageName = "Unbreakable",
        order = 6,
        minBondLevel = 10,
        minBondPoints = 3000L,
        minLoyalty = 95,
        minTrust = 95,
        title = "Monarch's Right Hand",
        description = "An eternal, absolute bond that transcends mortality. Inextricably woven with the Shadow Sovereign."
    );

    fun nextStage(): ShadowRelationshipStage? {
        val all = entries
        val idx = all.indexOf(this)
        return if (idx in 0 until all.lastIndex) all[idx + 1] else null
    }

    companion object {
        fun fromOrder(order: Int): ShadowRelationshipStage {
            return entries.find { it.order == order } ?: UNFAMILIAR
        }
    }
}

enum class ShadowPersonality(
    val traitName: String,
    val summary: String,
    val battleDemeanor: String,
    val dialogueSnippet: String
) {
    DISCIPLINED(
        traitName = "Disciplined",
        summary = "Methodical, obedient, and precise. Values orderly execution and unyielding routine.",
        battleDemeanor = "Maintains rigid defensive formations and moves with measured precision.",
        dialogueSnippet = "Orders received. My blade moves only at the Sovereign's command."
    ),
    PROTECTIVE(
        traitName = "Protective",
        summary = "Vigilant, unyielding shield of the Monarch. Prioritizes the Sovereign's safety above all else.",
        battleDemeanor = "Places itself directly between danger and the Monarch with unyielding resolve.",
        dialogueSnippet = "No harm shall approach my Lord while shadow still clings to this armor."
    ),
    AGGRESSIVE(
        traitName = "Aggressive",
        summary = "Fierce, relentless vanguard. Eager to break enemy lines and press the offensive.",
        battleDemeanor = "Surges forward with devastating fury, crushing opposing forces without pause.",
        dialogueSnippet = "Point me toward the enemy, Sovereign. Their annihilation is assured."
    ),
    STRATEGIC(
        traitName = "Strategic",
        summary = "Observant, calculating, and analytical. Anticipates tactical shifts before they manifest.",
        battleDemeanor = "Surveys the field, neutralizing high-value threats and coordinating flanks.",
        dialogueSnippet = "The battlefield unfolds as anticipated. The path to victory is clear."
    ),
    CALM(
        traitName = "Calm",
        summary = "Tranquil, focused, and steady under pressure. Maintains unwavering composure.",
        battleDemeanor = "Strikes with icy serenity, unbothered by chaos or numerical disadvantage.",
        dialogueSnippet = "Still waters run deep. My aim remains true in the midst of the storm."
    ),
    DEVOTED(
        traitName = "Devoted",
        summary = "Reverent, zealous, and wholly consecrated to the Monarch's eternal rule.",
        battleDemeanor = "Fights with spiritual fervency, drawing limitless stamina from the Monarch's presence.",
        dialogueSnippet = "My life and soul belong to the Monarch. Command, and I shall answer."
    );

    companion object {
        fun forShadow(shadowClass: ShadowClass, archetypeId: String? = null): ShadowPersonality {
            return when (archetypeId) {
                "arch_igris_blade" -> DEVOTED
                "arch_iron_sentinel" -> PROTECTIVE
                "arch_bellion_grand" -> STRATEGIC
                "arch_tank_ursine" -> AGGRESSIVE
                "arch_tusk_shaman" -> CALM
                "arch_beru_predator" -> DEVOTED
                else -> when (shadowClass) {
                    ShadowClass.VANGUARD -> AGGRESSIVE
                    ShadowClass.GUARDIAN -> PROTECTIVE
                    ShadowClass.RANGER -> CALM
                    ShadowClass.ARCANIST -> STRATEGIC
                    ShadowClass.TACTICIAN -> DISCIPLINED
                    ShadowClass.SUPPORT -> DEVOTED
                }
            }
        }
    }
}

enum class ShadowBondActivityType(
    val code: String,
    val label: String,
    val baseBondPoints: Int,
    val loyaltyGain: Int,
    val trustGain: Int,
    val descriptionTemplate: String
) {
    DUNGEON_COMPLETION(
        code = "DUNGEON_COMPLETION",
        label = "Dungeon Clearance",
        baseBondPoints = 50,
        loyaltyGain = 5,
        trustGain = 4,
        descriptionTemplate = "Fought side-by-side during dungeon clearance"
    ),
    BOSS_BATTLE(
        code = "BOSS_BATTLE",
        label = "Boss Victory",
        baseBondPoints = 100,
        loyaltyGain = 10,
        trustGain = 8,
        descriptionTemplate = "Overcame a formidable boss encounter together"
    ),
    DEPLOYMENT_SUCCESS(
        code = "DEPLOYMENT_SUCCESS",
        label = "Tactical Deployment",
        baseBondPoints = 30,
        loyaltyGain = 3,
        trustGain = 4,
        descriptionTemplate = "Deployed to real-world operational support assignment"
    ),
    DEPLOYMENT_RECALL(
        code = "DEPLOYMENT_RECALL",
        label = "Mission Return",
        baseBondPoints = 20,
        loyaltyGain = 2,
        trustGain = 3,
        descriptionTemplate = "Returned successfully from operational assignment"
    ),
    QUEST_COMPLETION(
        code = "QUEST_COMPLETION",
        label = "Shadow Quest",
        baseBondPoints = 40,
        loyaltyGain = 4,
        trustGain = 4,
        descriptionTemplate = "Assisted in completing an authoritative Monarch quest"
    ),
    SHADOW_EVOLUTION(
        code = "SHADOW_EVOLUTION",
        label = "Shadow Ascension",
        baseBondPoints = 80,
        loyaltyGain = 8,
        trustGain = 6,
        descriptionTemplate = "Ascended to a higher evolution stage under the Monarch's guidance"
    )
}

enum class ShadowBondMilestoneType(
    val code: String,
    val title: String,
    val description: String
) {
    FIRST_DEPLOYMENT(
        code = "FIRST_DEPLOYMENT",
        title = "First Vanguard Command",
        description = "First successful tactical deployment under the Monarch's directive."
    ),
    FIRST_MISSION_COMPLETE(
        code = "FIRST_MISSION_COMPLETE",
        title = "Mission Accomplished",
        description = "First operational mission successfully completed alongside the Monarch."
    ),
    FIRST_BOSS_VICTORY(
        code = "FIRST_BOSS_VICTORY",
        title = "Titan's Fall",
        description = "Stood victorious together in the face of a terrifying Boss."
    ),
    FIRST_EVOLUTION(
        code = "FIRST_EVOLUTION",
        title = "Ascension of the Void",
        description = "Evolved to a higher stage under the Monarch's sovereign authority."
    ),
    LOYALTY_DEVOTION(
        code = "LOYALTY_DEVOTION",
        title = "Unwavering Allegiance",
        description = "Attained a loyalty rating of 80 or higher."
    ),
    TRUST_ABSOLUTE(
        code = "TRUST_ABSOLUTE",
        title = "Absolute Trust",
        description = "Attained a trust rating of 80 or higher."
    ),
    UNBREAKABLE_BOND(
        code = "UNBREAKABLE_BOND",
        title = "Soul Inextricable",
        description = "Attained the pinnacle relationship stage: Unbreakable."
    )
}

// ----------------------------------------------------------------------------
// DOMAIN MODELS
// ----------------------------------------------------------------------------

data class ShadowBondProfile(
    val shadowId: String,
    val accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
    val bondLevel: Int = 1,
    val bondPoints: Long = 0L,
    val loyaltyRating: Int = 15,
    val trustRating: Int = 10,
    val relationshipStage: ShadowRelationshipStage = ShadowRelationshipStage.UNFAMILIAR,
    val personality: ShadowPersonality = ShadowPersonality.DISCIPLINED,
    val totalActivitiesCount: Int = 0,
    val lastBondActivityEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis()
) {
    val xpForCurrentLevel: Long get() = ShadowBondConfig.getXpRequiredForLevel(bondLevel)
    val xpForNextLevel: Long get() = ShadowBondConfig.getXpRequiredForLevel((bondLevel + 1).coerceAtMost(ShadowBondConfig.MAX_BOND_LEVEL))
    val isMaxLevel: Boolean get() = bondLevel >= ShadowBondConfig.MAX_BOND_LEVEL
    val levelProgressPercent: Float
        get() {
            if (isMaxLevel) return 1.0f
            val currentBase = xpForCurrentLevel
            val nextBase = xpForNextLevel
            val range = nextBase - currentBase
            return if (range > 0) ((bondPoints - currentBase).toFloat() / range).coerceIn(0.0f, 1.0f) else 1.0f
        }
}

// Convenience extension aliases for UI compatibility
val ShadowPersonality.label: String get() = traitName
val ShadowPersonality.description: String get() = summary
val ShadowBondMilestone.unlockedAtEpochMs: Long get() = timestampEpochMs
val ShadowBondEvent.occurredAtEpochMs: Long get() = timestampEpochMs
val ShadowBondEvent.bondPointsEarned: Int get() = bondPointsAwarded


data class ShadowBondEvent(
    val eventId: String,
    val accountOwnerId: String,
    val shadowId: String,
    val activityType: ShadowBondActivityType,
    val sourceEventId: String,
    val bondPointsAwarded: Int,
    val loyaltyDelta: Int,
    val trustDelta: Int,
    val description: String,
    val timestampEpochMs: Long,
    val idempotencyKey: String
)

data class ShadowBondMilestone(
    val milestoneId: String,
    val accountOwnerId: String,
    val shadowId: String,
    val milestoneType: ShadowBondMilestoneType,
    val title: String,
    val description: String,
    val sourceEventId: String,
    val timestampEpochMs: Long,
    val verificationStatus: String = "VERIFIED"
)

data class ShadowBondSummary(
    val shadowId: String,
    val shadowName: String,
    val shadowClass: ShadowClass,
    val bondLevel: Int,
    val relationshipStage: ShadowRelationshipStage,
    val loyaltyRating: Int,
    val trustRating: Int,
    val personality: ShadowPersonality
)

data class ShadowArmyBondOverview(
    val totalShadows: Int,
    val bondedShadowsCount: Int,
    val averageBondLevel: Float,
    val highestRelationshipStage: ShadowRelationshipStage,
    val stageDistribution: Map<ShadowRelationshipStage, Int>,
    val mostLoyalShadows: List<ShadowBondSummary>,
    val recentMilestones: List<ShadowBondMilestone>
)

// ----------------------------------------------------------------------------
// CENTRALIZED CONFIGURATION MATH
// ----------------------------------------------------------------------------

object ShadowBondConfig {
    const val MAX_BOND_LEVEL = 10
    const val MAX_STAT_RATING = 100
    const val MIN_STAT_RATING = 0

    private val LEVEL_THRESHOLDS = longArrayOf(
        0L,    // Level 1
        100L,  // Level 2
        200L,  // Level 3
        350L,  // Level 4
        550L,  // Level 5
        800L,  // Level 6
        1150L, // Level 7
        1600L, // Level 8
        2200L, // Level 9
        3000L  // Level 10
    )

    fun getXpRequiredForLevel(level: Int): Long {
        val clamped = level.coerceIn(1, MAX_BOND_LEVEL)
        return LEVEL_THRESHOLDS[clamped - 1]
    }

    fun calculateBondLevel(totalPoints: Long): Int {
        var level = 1
        for (i in 1 until LEVEL_THRESHOLDS.size) {
            if (totalPoints >= LEVEL_THRESHOLDS[i]) {
                level = i + 1
            } else {
                break
            }
        }
        return level.coerceIn(1, MAX_BOND_LEVEL)
    }

    fun getLoyaltyDescription(loyalty: Int): String {
        return when (loyalty.coerceIn(0, 100)) {
            in 0..25 -> "Bound by extraction protocol; distant allegiance."
            in 26..50 -> "Respectful obedience to command directives."
            in 51..75 -> "Fierce devotion forged in shared trials."
            else -> "Eternal, unwavering loyalty unto the void."
        }
    }

    fun getTrustDescription(trust: Int): String {
        return when (trust.coerceIn(0, 100)) {
            in 0..25 -> "Tentative acknowledgment of Monarch's authority."
            in 26..50 -> "Steady operational reliance and field reliability."
            in 51..75 -> "Deep tactical faith in the Monarch's commands."
            else -> "Absolute unquestioned trust in the Monarch's will."
        }
    }

    fun getEvolvingDialogue(
        personality: ShadowPersonality,
        stage: ShadowRelationshipStage
    ): String {
        return when (stage) {
            ShadowRelationshipStage.UNFAMILIAR -> "Awaiting instructions, Sovereign."
            ShadowRelationshipStage.RECOGNIZED -> personality.dialogueSnippet
            ShadowRelationshipStage.TRUSTING -> "My blade is steady. I anticipate your command, Monarch."
            ShadowRelationshipStage.LOYAL -> "Through fire or shadow, I stand beside you."
            ShadowRelationshipStage.DEVOTED -> "Your glory is my purpose. The shadow legion marches as one."
            ShadowRelationshipStage.UNBREAKABLE -> "Even if the stars burn to ash, my allegiance to the Shadow Sovereign shall never waver."
        }
    }
}
