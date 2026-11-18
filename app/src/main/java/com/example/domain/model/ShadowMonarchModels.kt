package com.example.domain.model

// ============================================================================
// MILESTONE 11 + 12 PHASE 6.1 — SHADOW ARMY DOMAIN MODELS
// ============================================================================

const val DEFAULT_SHADOW_ACCOUNT_OWNER_ID = "local_vault_player_1"

// ----------------------------------------------------------------------------
// SHADOW CLASS
// ----------------------------------------------------------------------------

enum class ShadowClass(
    val label: String,
    val description: String,
    val primarySpecialization: String,
    val secondarySpecialization: String,
    val passiveAbilityName: String,
    val colorToken: String
) {
    VANGUARD(
        label = "Vanguard",
        description = "Offensive specialist representing courage and execution.",
        primarySpecialization = "Physical Development",
        secondarySpecialization = "Challenge Preparation",
        passiveAbilityName = "Battle Fervor",
        colorToken = "shadow_vanguard"
    ),
    GUARDIAN(
        label = "Guardian",
        description = "Defensive specialist representing consistency and protection.",
        primarySpecialization = "Recovery-Aware Planning",
        secondarySpecialization = "Discipline Support",
        passiveAbilityName = "Steadfast Ward",
        colorToken = "shadow_guardian"
    ),
    RANGER(
        label = "Ranger",
        description = "Precision specialist representing concentration.",
        primarySpecialization = "Academic Focus",
        secondarySpecialization = "Structured Tasks",
        passiveAbilityName = "Precision Mark",
        colorToken = "shadow_ranger"
    ),
    ARCANIST(
        label = "Arcanist",
        description = "Intelligence specialist representing knowledge.",
        primarySpecialization = "Analysis & Learning",
        secondarySpecialization = "Strategic Planning",
        passiveAbilityName = "Arcane Insight",
        colorToken = "shadow_arcanist"
    ),
    TACTICIAN(
        label = "Tactician",
        description = "Strategic specialist representing decision-making.",
        primarySpecialization = "Scheduling & Prioritization",
        secondarySpecialization = "Planning",
        passiveAbilityName = "Strategic Foresight",
        colorToken = "shadow_tactician"
    ),
    SUPPORT(
        label = "Support",
        description = "General support specialist representing discipline.",
        primarySpecialization = "Habit Tracking",
        secondarySpecialization = "Consistency Reports",
        passiveAbilityName = "Discipline Aura",
        colorToken = "shadow_support"
    )
}

// ----------------------------------------------------------------------------
// SHADOW RANK (progression tier)
// ----------------------------------------------------------------------------

enum class ShadowRank(val label: String, val order: Int, val xpToNext: Int) {
    E_RANK("E-Rank", 1, 500),
    D_RANK("D-Rank", 2, 1200),
    C_RANK("C-Rank", 3, 2500),
    B_RANK("B-Rank", 4, 4500),
    A_RANK("A-Rank", 5, 8000),
    S_RANK("S-Rank", 6, 15000),
    MONARCH_CLASS("Monarch-Class", 7, 0);

    fun nextRank(): ShadowRank? {
        val all = entries
        val idx = all.indexOf(this)
        return if (idx in 0 until all.lastIndex) all[idx + 1] else null
    }
}

// ----------------------------------------------------------------------------
// SHADOW RARITY (recruitment rarity — separate from rank)
// ----------------------------------------------------------------------------

enum class ShadowRarity(val label: String, val order: Int, val baseStatMultiplier: Float) {
    COMMON("Common", 1, 1.0f),
    UNCOMMON("Uncommon", 2, 1.15f),
    RARE("Rare", 3, 1.3f),
    EPIC("Epic", 4, 1.5f),
    LEGENDARY("Legendary", 5, 1.8f),
    MYTHIC("Mythic", 6, 2.2f)
}

// ----------------------------------------------------------------------------
// SHADOW EVOLUTION STAGE
// ----------------------------------------------------------------------------

enum class ShadowEvolutionStage(
    val label: String,
    val order: Int,
    val minLevel: Int,
    val minRank: ShadowRank,
    val description: String
) {
    NEWLY_EXTRACTED("Newly Extracted", 1, 1, ShadowRank.E_RANK, "A shadow freshly claimed from the void."),
    AWAKENED("Awakened", 2, 5, ShadowRank.D_RANK, "The shadow has begun to manifest its true form."),
    ELITE("Elite", 3, 15, ShadowRank.C_RANK, "A powerful shadow demonstrating remarkable capability."),
    COMMANDER("Commander", 4, 30, ShadowRank.B_RANK, "A shadow that commands respect and leads with authority."),
    GENERAL("General", 5, 50, ShadowRank.A_RANK, "One of the Monarch's most formidable generals."),
    MONARCH_GUARD("Monarch Guard", 6, 75, ShadowRank.S_RANK, "An elite guardian who serves only the Shadow Monarch.")
}

// ----------------------------------------------------------------------------
// SHADOW DEPLOYMENT CATEGORY
// ----------------------------------------------------------------------------

enum class ShadowDeploymentCategory(val label: String, val description: String) {
    PHYSICAL_DEVELOPMENT("Physical Development", "Workout preparation, progress reflection, recovery reminders."),
    ACADEMIC_DEVELOPMENT("Academic Development", "Study planning, focus preparation, revision organization."),
    DISCIPLINE("Discipline", "Habit reminders, routine monitoring, consistency reports."),
    STRATEGIC_PLANNING("Strategic Planning", "Task prioritization, weekly planning, goal review."),
    DUNGEON_SUPPORT("Dungeon Support", "Challenge preparation, objective summaries, progress analysis."),
    RECOVERY_SUPPORT("Recovery Support", "Recovery dashboard awareness, rest reminders, workload review.")
}

// ----------------------------------------------------------------------------
// SHADOW EXTRACTION SOURCE TYPE
// ----------------------------------------------------------------------------

enum class ShadowExtractionSourceType(val label: String) {
    DUNGEON_COMPLETION("Dungeon Completion"),
    BOSS_BATTLE_COMPLETION("Boss Battle Completion"),
    PHYSICAL_MILESTONE("Physical Development Milestone"),
    ACADEMIC_MILESTONE("Academic Milestone"),
    DISCIPLINE_MILESTONE("Discipline Milestone"),
    ACHIEVEMENT_UNLOCK("Achievement Unlock"),
    MONARCH_QUEST("Monarch Quest"),
    CONSISTENCY_MILESTONE("Consistency Milestone")
}

// ----------------------------------------------------------------------------
// SHADOW EXTRACTION OUTCOME
// ----------------------------------------------------------------------------

enum class ShadowExtractionOutcome(val label: String) {
    SUCCESS("Shadow Extracted"),
    UNAVAILABLE("Extraction Unavailable"),
    ALREADY_CLAIMED("Already Claimed"),
    ARMY_CAPACITY_REACHED("Army Capacity Reached"),
    SOURCE_INVALID("Source Evidence Invalid"),
    PENDING_CONFIRMATION("Pending Confirmation"),
    DUPLICATE_RESOLVED("Duplicate Resonance")
}

// ----------------------------------------------------------------------------
// SHADOW DEPLOYMENT STATUS
// ----------------------------------------------------------------------------

enum class ShadowDeploymentStatus(val label: String) {
    IDLE("Idle"),
    DEPLOYED("Deployed"),
    RECALLED("Recalled")
}

// ----------------------------------------------------------------------------
// SHADOW FORMATION TYPE
// ----------------------------------------------------------------------------

enum class ShadowFormationType(val label: String, val maxSlots: Int) {
    VANGUARD_FORMATION("Vanguard Formation", 5),
    DEFENSIVE_FORMATION("Defensive Formation", 5),
    ACADEMIC_FORMATION("Academic Formation", 4),
    STRATEGIC_FORMATION("Strategic Formation", 4),
    BALANCED_FORMATION("Balanced Formation", 6),
    MONARCH_FORMATION("Monarch Formation", 8)
}

// ----------------------------------------------------------------------------
// MONARCH RANK
// ----------------------------------------------------------------------------

enum class MonarchRank(val label: String, val minLevel: Int, val description: String) {
    AWAKENING("Awakening", 1, "The Shadow Monarch stirs, power slowly coalescing."),
    COMMANDER("Commander", 6, "A commander who has proven their authority."),
    SOVEREIGN("Sovereign", 11, "A sovereign whose will shapes the Shadow Army."),
    MONARCH("Monarch", 21, "A true Monarch whose army grows without limit."),
    SHADOW_SOVEREIGN("Shadow Sovereign", 31, "The ultimate Shadow Sovereign — ARISE.");

    companion object {
        fun forLevel(level: Int): MonarchRank {
            return entries.sortedByDescending { it.minLevel }.firstOrNull { level >= it.minLevel }
                ?: AWAKENING
        }
    }
}

// ----------------------------------------------------------------------------
// MONARCH SKILL ID CONSTANTS
// ----------------------------------------------------------------------------

object MonarchSkillIds {
    const val SHADOW_EXTRACTION = "monarch_shadow_extraction"
    const val ARMY_EXPANSION = "monarch_army_expansion"
    const val SHADOW_COMMAND = "monarch_shadow_command"
    const val EVOLUTION_AUTHORITY = "monarch_evolution_authority"
    const val MONARCHS_INSIGHT = "monarch_monarchs_insight"
    const val SHADOW_SYNCHRONIZATION = "monarch_shadow_sync"
    const val SOVEREIGN_COMMAND = "monarch_sovereign_command"
    const val ABSOLUTE_AUTHORITY = "monarch_absolute_authority"
}

// ----------------------------------------------------------------------------
// SHADOW SKILL CATEGORY
// ----------------------------------------------------------------------------

enum class ShadowSkillCategory(val label: String) {
    COMBAT_INSPIRATION("Combat Inspiration"),
    STRATEGIC_PLANNING("Strategic Planning"),
    ACADEMIC_FOCUS("Academic Focus"),
    DISCIPLINE_SUPPORT("Discipline Support"),
    RECOVERY_AWARENESS("Recovery Awareness"),
    CONSISTENCY_SUPPORT("Consistency Support")
}

// ============================================================================
// DOMAIN DATA CLASSES
// ============================================================================

data class ShadowStats(
    val power: Int = 10,
    val endurance: Int = 10,
    val focus: Int = 10,
    val discipline: Int = 10,
    val wisdom: Int = 10,
    val adaptability: Int = 10
) {
    val total: Int get() = power + endurance + focus + discipline + wisdom + adaptability
}

data class ShadowMonarchProfile(
    val id: Int = 1,
    val accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
    val monarchLevel: Int = 1,
    val monarchXp: Long = 0L,
    val monarchRank: MonarchRank = MonarchRank.AWAKENING,
    val monarchAuthority: Int = 0,
    val armyCapacity: Int = 10,
    val activeShadowCount: Int = 0,
    val totalShadowsRecruited: Int = 0,
    val highestShadowRank: ShadowRank = ShadowRank.E_RANK,
    val totalEvolutions: Int = 0,
    val shadowArmyPowerRating: Long = 0L,
    val unlockedMonarchSkillIds: List<String> = emptyList(),
    val updatedAtEpochMs: Long = System.currentTimeMillis()
) {
    val xpForCurrentLevel: Long get() = monarchXpRequired(monarchLevel)
    val xpForNextLevel: Long get() = monarchXpRequired(monarchLevel + 1)
    val xpIntoCurrentLevel: Long get() = monarchXp - xpForCurrentLevel
    val xpNeededForNextLevel: Long get() = xpForNextLevel - xpForCurrentLevel

    companion object {
        fun monarchXpRequired(level: Int): Long {
            if (level <= 1) return 0L
            // Formula: cumulative XP = 100 * level^2 + 50 * level - 150
            return (100L * level * level + 50L * level - 150L).coerceAtLeast(0L)
        }
        fun monarchLevelForXp(totalXp: Long): Int {
            var level = 1
            while (monarchXpRequired(level + 1) <= totalXp) level++
            return level.coerceAtMost(999)
        }
    }
}

data class Shadow(
    val shadowId: String,
    val accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
    val name: String,
    val shadowClass: ShadowClass,
    val rarity: ShadowRarity,
    val currentRank: ShadowRank = ShadowRank.E_RANK,
    val level: Int = 1,
    val shadowXp: Long = 0L,
    val stats: ShadowStats = ShadowStats(),
    val evolutionStage: ShadowEvolutionStage = ShadowEvolutionStage.NEWLY_EXTRACTED,
    val passiveAbility: String = shadowClass.passiveAbilityName,
    val activeSkillIds: List<String> = emptyList(),
    val recruitmentSource: String = "",
    val extractionEventId: String = "",
    val extractedAtEpochMs: Long = System.currentTimeMillis(),
    val evolutionHistoryJson: String = "[]",
    val deploymentStatus: ShadowDeploymentStatus = ShadowDeploymentStatus.IDLE,
    val equippedFormationId: String? = null,
    val isRetired: Boolean = false,
    val notes: String = ""
) {
    val xpForCurrentLevel: Long get() = shadowXpRequired(level)
    val xpForNextLevel: Long get() = shadowXpRequired(level + 1)
    val xpIntoLevel: Long get() = shadowXp - xpForCurrentLevel

    fun calculatePowerRating(): Long = stats.total.toLong() * level

    companion object {
        fun shadowXpRequired(level: Int): Long {
            if (level <= 1) return 0L
            return (50L * level * level + 25L * level - 75L).coerceAtLeast(0L)
        }
        fun shadowLevelForXp(totalXp: Long): Int {
            var level = 1
            while (shadowXpRequired(level + 1) <= totalXp) level++
            return level.coerceAtMost(999)
        }
    }
}

data class ShadowExtractionEvent(
    val extractionId: String,
    val accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
    val sourceType: ShadowExtractionSourceType,
    val sourceEventId: String,
    val outcome: ShadowExtractionOutcome,
    val extractedShadowId: String?,
    val shadowName: String?,
    val shadowClass: ShadowClass?,
    val shadowRarity: ShadowRarity?,
    val determinismSeed: Long,
    val evidenceSummary: String,
    val timestampEpochMs: Long = System.currentTimeMillis(),
    val isConfirmed: Boolean = false,
    val monarchXpAwarded: Int = 0
)

data class ShadowSkillDefinition(
    val skillId: String,
    val name: String,
    val description: String,
    val category: ShadowSkillCategory,
    val requiredShadowRank: ShadowRank,
    val requiredShadowLevel: Int,
    val prerequisiteSkillIds: List<String> = emptyList(),
    val isPassive: Boolean = true,
    val cooldownHours: Int = 0,
    val effectTag: String = "",
    val unlockRequirementNote: String = ""
)

data class ShadowSkillUnlock(
    val id: Long = 0,
    val accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
    val shadowId: String,
    val skillId: String,
    val unlockedAtEpochMs: Long,
    val evidenceNote: String = ""
)

data class ShadowDeploymentRecord(
    val deploymentId: String,
    val accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
    val shadowId: String,
    val shadowName: String,
    val category: ShadowDeploymentCategory,
    val role: String,
    val assignedAtEpochMs: Long,
    val recalledAtEpochMs: Long? = null,
    val status: ShadowDeploymentStatus = ShadowDeploymentStatus.DEPLOYED,
    val notes: String = ""
)

data class ShadowFormation(
    val formationId: String,
    val accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
    val name: String,
    val formationType: ShadowFormationType,
    val description: String,
    val maxSlots: Int = formationType.maxSlots,
    val memberShadowIds: List<String> = emptyList(),
    val isActive: Boolean = false,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis()
) {
    val memberCount: Int get() = memberShadowIds.size
    val availableSlots: Int get() = (maxSlots - memberCount).coerceAtLeast(0)
}

data class MonarchSkillDefinition(
    val skillId: String,
    val name: String,
    val description: String,
    val requiredMonarchLevel: Int,
    val requiredAuthority: Int,
    val prerequisiteSkillId: String? = null,
    val order: Int = 0
)

data class MonarchSkillUnlock(
    val id: Long = 0,
    val accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
    val skillId: String,
    val unlockedAtEpochMs: Long,
    val evidenceNote: String = ""
)

data class ShadowQuestDefinition(
    val questId: String,
    val title: String,
    val description: String,
    val category: ShadowQuestCategory,
    val requirement: String,
    val targetValue: Int,
    val monarchXpReward: Int,
    val shadowXpReward: Int = 0,
    val shadowEssenceReward: Int = 0
)

enum class ShadowQuestCategory(val label: String) {
    EXTRACTION("Extraction Quests"),
    EVOLUTION("Evolution Quests"),
    ARMY_DEVELOPMENT("Army Development Quests"),
    MONARCH_CHALLENGE("Monarch Challenges"),
    SHADOW_MASTERY("Shadow Mastery Quests")
}

data class ShadowQuestProgress(
    val id: Long = 0,
    val accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
    val questId: String,
    val currentValue: Int = 0,
    val isCompleted: Boolean = false,
    val rewardClaimedAtEpochMs: Long? = null,
    val completedAtEpochMs: Long? = null
)

data class ShadowResourceTransaction(
    val transactionId: String,
    val accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
    val resourceType: ShadowResourceType,
    val amount: Int,
    val transactionType: ShadowResourceTransactionType,
    val sourceEventId: String,
    val sourceDescription: String,
    val balanceAfter: Int,
    val timestampEpochMs: Long = System.currentTimeMillis()
)

enum class ShadowResourceType(val label: String) {
    SHADOW_ESSENCE("Shadow Essence"),
    MONARCH_FRAGMENTS("Monarch Fragments"),
    EVOLUTION_CORES("Evolution Cores"),
    SHADOW_CRYSTALS("Shadow Crystals")
}

enum class ShadowResourceTransactionType(val label: String) {
    EARNED("Earned"),
    CONSUMED("Consumed")
}

// ----------------------------------------------------------------------------
// SHADOW ARMY ANALYTICS
// ----------------------------------------------------------------------------

data class ShadowArmyAnalytics(
    val totalRecruited: Int = 0,
    val activeShadows: Int = 0,
    val retiredShadows: Int = 0,
    val distributionByClass: Map<ShadowClass, Int> = emptyMap(),
    val distributionByRarity: Map<ShadowRarity, Int> = emptyMap(),
    val distributionByRank: Map<ShadowRank, Int> = emptyMap(),
    val highestLevelShadow: Shadow? = null,
    val highestRankShadow: Shadow? = null,
    val totalEvolutions: Int = 0,
    val mostDeployedClass: ShadowClass? = null,
    val monarchProfile: ShadowMonarchProfile = ShadowMonarchProfile(),
    val resourceBalances: Map<ShadowResourceType, Int> = emptyMap()
)

// ----------------------------------------------------------------------------
// MONARCH SKILL CATALOG (static definitions, deterministic)
// ----------------------------------------------------------------------------

object MonarchSkillCatalog {
    val skills: List<MonarchSkillDefinition> = listOf(
        MonarchSkillDefinition(
            skillId = MonarchSkillIds.SHADOW_EXTRACTION,
            name = "Shadow Extraction",
            description = "Enables the Shadow Monarch to extract shadows from eligible achievements and completed challenges.",
            requiredMonarchLevel = 1,
            requiredAuthority = 0,
            order = 1
        ),
        MonarchSkillDefinition(
            skillId = MonarchSkillIds.ARMY_EXPANSION,
            name = "Army Expansion",
            description = "Increases available army capacity, allowing more shadows to be recruited and deployed.",
            requiredMonarchLevel = 3,
            requiredAuthority = 10,
            prerequisiteSkillId = MonarchSkillIds.SHADOW_EXTRACTION,
            order = 2
        ),
        MonarchSkillDefinition(
            skillId = MonarchSkillIds.SHADOW_COMMAND,
            name = "Shadow Command",
            description = "Unlocks additional deployment controls and advanced assignment options.",
            requiredMonarchLevel = 6,
            requiredAuthority = 25,
            prerequisiteSkillId = MonarchSkillIds.ARMY_EXPANSION,
            order = 3
        ),
        MonarchSkillDefinition(
            skillId = MonarchSkillIds.EVOLUTION_AUTHORITY,
            name = "Evolution Authority",
            description = "Unlocks higher evolution tiers, allowing shadows to transcend into Elite and beyond.",
            requiredMonarchLevel = 11,
            requiredAuthority = 50,
            prerequisiteSkillId = MonarchSkillIds.SHADOW_COMMAND,
            order = 4
        ),
        MonarchSkillDefinition(
            skillId = MonarchSkillIds.MONARCHS_INSIGHT,
            name = "Monarch's Insight",
            description = "Provides advanced army analytics and deep strategic intelligence about shadow capabilities.",
            requiredMonarchLevel = 15,
            requiredAuthority = 75,
            prerequisiteSkillId = MonarchSkillIds.EVOLUTION_AUTHORITY,
            order = 5
        ),
        MonarchSkillDefinition(
            skillId = MonarchSkillIds.SHADOW_SYNCHRONIZATION,
            name = "Shadow Synchronization",
            description = "Enables advanced formation management and synchronized shadow deployment.",
            requiredMonarchLevel = 21,
            requiredAuthority = 100,
            prerequisiteSkillId = MonarchSkillIds.MONARCHS_INSIGHT,
            order = 6
        ),
        MonarchSkillDefinition(
            skillId = MonarchSkillIds.SOVEREIGN_COMMAND,
            name = "Sovereign Command",
            description = "Unlocks advanced Monarch interface capabilities and sovereign-tier controls.",
            requiredMonarchLevel = 28,
            requiredAuthority = 150,
            prerequisiteSkillId = MonarchSkillIds.SHADOW_SYNCHRONIZATION,
            order = 7
        ),
        MonarchSkillDefinition(
            skillId = MonarchSkillIds.ABSOLUTE_AUTHORITY,
            name = "Absolute Authority",
            description = "The final achievement of the Shadow Monarch. All shadows bow before Absolute Authority.",
            requiredMonarchLevel = 35,
            requiredAuthority = 200,
            prerequisiteSkillId = MonarchSkillIds.SOVEREIGN_COMMAND,
            order = 8
        )
    )

    fun findById(skillId: String): MonarchSkillDefinition? = skills.firstOrNull { it.skillId == skillId }
}

// ----------------------------------------------------------------------------
// SHADOW SKILL CATALOG
// ----------------------------------------------------------------------------

object ShadowSkillCatalog {
    val skills: List<ShadowSkillDefinition> = listOf(
        ShadowSkillDefinition(
            skillId = "shadow_tactical_insight",
            name = "Tactical Insight",
            description = "Provides a structured summary of an upcoming challenge, highlighting key objectives.",
            category = ShadowSkillCategory.STRATEGIC_PLANNING,
            requiredShadowRank = ShadowRank.E_RANK,
            requiredShadowLevel = 1,
            isPassive = false,
            cooldownHours = 4,
            effectTag = "TACTICAL_SUMMARY"
        ),
        ShadowSkillDefinition(
            skillId = "shadow_focus_guard",
            name = "Focus Guard",
            description = "Suggests an appropriate focus-session configuration based on your current workload.",
            category = ShadowSkillCategory.ACADEMIC_FOCUS,
            requiredShadowRank = ShadowRank.D_RANK,
            requiredShadowLevel = 5,
            prerequisiteSkillIds = listOf("shadow_tactical_insight"),
            isPassive = false,
            cooldownHours = 2,
            effectTag = "FOCUS_SESSION_SUGGESTION"
        ),
        ShadowSkillDefinition(
            skillId = "shadow_discipline_aura",
            name = "Discipline Aura",
            description = "Highlights overdue routines and neglected habits, surfacing consistency gaps.",
            category = ShadowSkillCategory.DISCIPLINE_SUPPORT,
            requiredShadowRank = ShadowRank.D_RANK,
            requiredShadowLevel = 8,
            isPassive = true,
            effectTag = "DISCIPLINE_HIGHLIGHT"
        ),
        ShadowSkillDefinition(
            skillId = "shadow_recovery_sentinel",
            name = "Recovery Sentinel",
            description = "Highlights existing recovery warnings and suggests reviewing the recovery dashboard.",
            category = ShadowSkillCategory.RECOVERY_AWARENESS,
            requiredShadowRank = ShadowRank.C_RANK,
            requiredShadowLevel = 15,
            prerequisiteSkillIds = listOf("shadow_discipline_aura"),
            isPassive = true,
            effectTag = "RECOVERY_AWARENESS"
        ),
        ShadowSkillDefinition(
            skillId = "shadow_strategic_vision",
            name = "Strategic Vision",
            description = "Helps prioritize pending tasks using existing scheduling intelligence.",
            category = ShadowSkillCategory.STRATEGIC_PLANNING,
            requiredShadowRank = ShadowRank.C_RANK,
            requiredShadowLevel = 20,
            prerequisiteSkillIds = listOf("shadow_tactical_insight"),
            isPassive = false,
            cooldownHours = 6,
            effectTag = "TASK_PRIORITY_ASSIST"
        ),
        ShadowSkillDefinition(
            skillId = "shadow_battle_fervor",
            name = "Battle Fervor",
            description = "Vanguard class passive. Surfaces workout preparation suggestions before challenging sessions.",
            category = ShadowSkillCategory.COMBAT_INSPIRATION,
            requiredShadowRank = ShadowRank.B_RANK,
            requiredShadowLevel = 30,
            isPassive = true,
            effectTag = "WORKOUT_PREP_INSPIRE"
        ),
        ShadowSkillDefinition(
            skillId = "shadow_consistency_pulse",
            name = "Consistency Pulse",
            description = "Tracks and reports consistency streaks, flagging when continuity is at risk.",
            category = ShadowSkillCategory.CONSISTENCY_SUPPORT,
            requiredShadowRank = ShadowRank.B_RANK,
            requiredShadowLevel = 35,
            isPassive = true,
            effectTag = "CONSISTENCY_TRACK"
        ),
        ShadowSkillDefinition(
            skillId = "shadow_arcane_analysis",
            name = "Arcane Analysis",
            description = "Arcanist class passive. Provides deep analytical summaries of academic progress.",
            category = ShadowSkillCategory.ACADEMIC_FOCUS,
            requiredShadowRank = ShadowRank.A_RANK,
            requiredShadowLevel = 50,
            prerequisiteSkillIds = listOf("shadow_focus_guard"),
            isPassive = true,
            effectTag = "ACADEMIC_DEEP_ANALYSIS"
        )
    )

    fun findById(skillId: String): ShadowSkillDefinition? = skills.firstOrNull { it.skillId == skillId }
}

// ----------------------------------------------------------------------------
// SHADOW QUEST CATALOG
// ----------------------------------------------------------------------------

object ShadowQuestCatalog {
    val quests: List<ShadowQuestDefinition> = listOf(
        ShadowQuestDefinition(
            questId = "sq_first_extraction",
            title = "First Extraction",
            description = "Successfully recruit your first shadow from an eligible source.",
            category = ShadowQuestCategory.EXTRACTION,
            requirement = "shadow_count >= 1",
            targetValue = 1,
            monarchXpReward = 150,
            shadowEssenceReward = 50
        ),
        ShadowQuestDefinition(
            questId = "sq_growing_army",
            title = "Growing Army",
            description = "Recruit five eligible shadows into your Shadow Army.",
            category = ShadowQuestCategory.ARMY_DEVELOPMENT,
            requirement = "shadow_count >= 5",
            targetValue = 5,
            monarchXpReward = 300,
            shadowEssenceReward = 100
        ),
        ShadowQuestDefinition(
            questId = "sq_elite_command",
            title = "Elite Command",
            description = "Develop a shadow to A-Rank through consistent progression.",
            category = ShadowQuestCategory.SHADOW_MASTERY,
            requirement = "shadow_at_a_rank >= 1",
            targetValue = 1,
            monarchXpReward = 500,
            shadowEssenceReward = 200
        ),
        ShadowQuestDefinition(
            questId = "sq_first_evolution",
            title = "First Evolution",
            description = "Evolve a shadow to the Awakened stage.",
            category = ShadowQuestCategory.EVOLUTION,
            requirement = "total_evolutions >= 1",
            targetValue = 1,
            monarchXpReward = 200,
            shadowEssenceReward = 75
        ),
        ShadowQuestDefinition(
            questId = "sq_loyal_army",
            title = "Loyal Army",
            description = "Maintain an active formation with at least 3 shadows for 7 consecutive days.",
            category = ShadowQuestCategory.MONARCH_CHALLENGE,
            requirement = "formation_active_days >= 7",
            targetValue = 7,
            monarchXpReward = 400,
            shadowEssenceReward = 150
        ),
        ShadowQuestDefinition(
            questId = "sq_ten_shadows",
            title = "Legion of Shadows",
            description = "Recruit ten shadows across all classes.",
            category = ShadowQuestCategory.ARMY_DEVELOPMENT,
            requirement = "shadow_count >= 10",
            targetValue = 10,
            monarchXpReward = 600,
            shadowEssenceReward = 250
        ),
        ShadowQuestDefinition(
            questId = "sq_first_deployment",
            title = "First Deployment",
            description = "Deploy a shadow to a strategic support role.",
            category = ShadowQuestCategory.ARMY_DEVELOPMENT,
            requirement = "deployed_count >= 1",
            targetValue = 1,
            monarchXpReward = 100,
            shadowEssenceReward = 30
        ),
        ShadowQuestDefinition(
            questId = "sq_monarch_trial",
            title = "Monarch's Trial",
            description = "Complete a qualifying dungeon and develop one shadow to Commander stage.",
            category = ShadowQuestCategory.MONARCH_CHALLENGE,
            requirement = "dungeon_completions >= 1 AND commander_stage_shadows >= 1",
            targetValue = 1,
            monarchXpReward = 800,
            shadowEssenceReward = 300
        )
    )

    fun findById(questId: String): ShadowQuestDefinition? = quests.firstOrNull { it.questId == questId }
}

// ----------------------------------------------------------------------------
// PHASE 6.2 — SHADOW COLLECTION & RESONANCE MODELS
// ----------------------------------------------------------------------------

enum class ShadowSortOption(val label: String) {
    RECENTLY_RECRUITED("Recently Recruited"),
    RARITY("Rarity"),
    LEVEL("Level"),
    RANK("Rank"),
    NAME("Name")
}

data class ShadowResonanceRecord(
    val resonanceId: String,
    val accountOwnerId: String,
    val archetypeId: String,
    val resonanceLevel: Int,
    val resonanceShards: Int,
    val lastResonanceEpochMs: Long
)

data class ShadowEvidenceClaim(
    val claimId: String,
    val accountOwnerId: String,
    val sourceType: String,
    val sourceRecordId: String,
    val claimedAtEpochMs: Long
)

data class ShadowCollectionOverview(
    val totalOwned: Int = 0,
    val uniqueArchetypesCollected: Int = 0,
    val totalArchetypesInCatalog: Int = 24,
    val completionPercentage: Float = 0f,
    val classDistribution: Map<ShadowClass, Int> = emptyMap(),
    val rarityDistribution: Map<ShadowRarity, Int> = emptyMap(),
    val rankDistribution: Map<ShadowRank, Int> = emptyMap(),
    val recentRecruits: List<Shadow> = emptyList(),
    val duplicateResonanceCount: Int = 0,
    val resonanceRecords: List<ShadowResonanceRecord> = emptyList()
)


