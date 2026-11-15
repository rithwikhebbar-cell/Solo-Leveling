package com.example.domain.engine

import com.example.domain.model.AchievementCategory
import com.example.domain.model.AchievementDefinition
import com.example.domain.model.AchievementUnlockRecord
import com.example.domain.model.AchievementWithStatus
import com.example.domain.model.ActiveSkillEffects
import com.example.domain.model.AttributeType
import com.example.domain.model.HunterRank
import com.example.domain.model.InventoryCategory
import com.example.domain.model.InventoryItemDefinition
import com.example.domain.model.InventoryItemWithOwnership
import com.example.domain.model.PlayerInventoryRecord
import com.example.domain.model.PlayerProfile
import com.example.domain.model.PlayerTitleDefinition
import com.example.domain.model.RpgRarity
import com.example.domain.model.ShadowCollectibleDefinition
import com.example.domain.model.ShadowCollectibleWithStatus
import com.example.domain.model.ShadowCollectionCategory
import com.example.domain.model.ShadowCollectionRecord
import com.example.domain.model.SkillDefinition
import com.example.domain.model.SkillEffectSpec
import com.example.domain.model.SkillSpecialization
import com.example.domain.model.SkillTier
import com.example.domain.model.SkillTreeDependency
import com.example.domain.model.SkillUnlockRecord
import com.example.domain.model.SkillWithStatus

// ============================================================================
// SKILL SYSTEM, 5 SKILL TREES & SKILL EFFECT ENGINE
// ============================================================================

object SkillCatalog {

    val skills: List<SkillDefinition> = listOf(
        // A. Physical Development (4)
        SkillDefinition(
            id = "phys_endurance",
            name = "Endurance",
            specialization = SkillSpecialization.PHYSICAL_DEVELOPMENT,
            tier = SkillTier.COMMON,
            description = "Builds sustained aerobic pacing and unlocks extended endurance training blocks.",
            requiredRank = HunterRank.E_RANK,
            requiredLevel = 1,
            requiredAttribute = AttributeType.VIT,
            requiredAttributeValue = 11,
            treeColumn = 0,
            treeRow = 0
        ),
        SkillDefinition(
            id = "phys_strength",
            name = "Strength Development",
            specialization = SkillSpecialization.PHYSICAL_DEVELOPMENT,
            tier = SkillTier.UNCOMMON,
            description = "Unlocks progressive overload strength dungeon templates and calisthenics tiers.",
            requiredRank = HunterRank.E_RANK,
            requiredLevel = 3,
            requiredAttribute = AttributeType.STR,
            requiredAttributeValue = 12,
            requiredVerifiedCount = 1,
            treeColumn = 1,
            treeRow = 0
        ),
        SkillDefinition(
            id = "phys_agility",
            name = "Agility",
            specialization = SkillSpecialization.PHYSICAL_DEVELOPMENT,
            tier = SkillTier.RARE,
            description = "Unlocks tempo intervals and multi-discipline speed conditioning options.",
            requiredRank = HunterRank.D_RANK,
            requiredLevel = 6,
            requiredAttribute = AttributeType.AGI,
            requiredAttributeValue = 14,
            requiredVerifiedCount = 3,
            treeColumn = 0,
            treeRow = 1
        ),
        SkillDefinition(
            id = "phys_recovery",
            name = "Recovery Awareness",
            specialization = SkillSpecialization.PHYSICAL_DEVELOPMENT,
            tier = SkillTier.EPIC,
            description = "Surfaces smart deload cues and protects weekly streak metrics during rest protocols.",
            requiredRank = HunterRank.C_RANK,
            requiredLevel = 12,
            requiredAttribute = AttributeType.VIT,
            requiredAttributeValue = 16,
            requiredVerifiedCount = 6,
            treeColumn = 1,
            treeRow = 1
        ),

        // B. Academic Excellence (4)
        SkillDefinition(
            id = "acad_deep_focus",
            name = "Deep Focus",
            specialization = SkillSpecialization.ACADEMIC_EXCELLENCE,
            tier = SkillTier.COMMON,
            description = "Unlocks extended 60-minute and 90-minute Focus Timer presets.",
            requiredRank = HunterRank.E_RANK,
            requiredLevel = 1,
            requiredAttribute = AttributeType.INT,
            requiredAttributeValue = 11,
            treeColumn = 0,
            treeRow = 0
        ),
        SkillDefinition(
            id = "acad_rapid_learning",
            name = "Rapid Learning",
            specialization = SkillSpecialization.ACADEMIC_EXCELLENCE,
            tier = SkillTier.UNCOMMON,
            description = "Unlocks accelerated chapter synthesis quests and structured concept breakdowns.",
            requiredRank = HunterRank.E_RANK,
            requiredLevel = 3,
            requiredAttribute = AttributeType.INT,
            requiredAttributeValue = 13,
            treeColumn = 1,
            treeRow = 0
        ),
        SkillDefinition(
            id = "acad_retention",
            name = "Knowledge Retention",
            specialization = SkillSpecialization.ACADEMIC_EXCELLENCE,
            tier = SkillTier.RARE,
            description = "Unlocks spaced-repetition priority sorting and long-horizon retention windows.",
            requiredRank = HunterRank.D_RANK,
            requiredLevel = 6,
            requiredAttribute = AttributeType.WIS,
            requiredAttributeValue = 13,
            requiredVerifiedCount = 2,
            treeColumn = 0,
            treeRow = 1
        ),
        SkillDefinition(
            id = "acad_exam_prep",
            name = "Examination Preparation",
            specialization = SkillSpecialization.ACADEMIC_EXCELLENCE,
            tier = SkillTier.EPIC,
            description = "Unlocks intensive exam-sprint dungeon configurations and mock-test readiness tracks.",
            requiredRank = HunterRank.C_RANK,
            requiredLevel = 12,
            requiredAttribute = AttributeType.INT,
            requiredAttributeValue = 16,
            requiredVerifiedCount = 5,
            treeColumn = 1,
            treeRow = 1
        ),

        // C. Absolute Discipline (4)
        SkillDefinition(
            id = "disc_consistency",
            name = "Consistency",
            specialization = SkillSpecialization.ABSOLUTE_DISCIPLINE,
            tier = SkillTier.COMMON,
            description = "Unlocks consistency streak breakdown and protected rest-day calibration.",
            requiredRank = HunterRank.E_RANK,
            requiredLevel = 1,
            requiredAttribute = AttributeType.DIS,
            requiredAttributeValue = 11,
            requiredStreakDays = 2,
            treeColumn = 0,
            treeRow = 0
        ),
        SkillDefinition(
            id = "disc_routine_mastery",
            name = "Routine Mastery",
            specialization = SkillSpecialization.ABSOLUTE_DISCIPLINE,
            tier = SkillTier.UNCOMMON,
            description = "Unlocks advanced routine adherence diagnostics and template chaining.",
            requiredRank = HunterRank.E_RANK,
            requiredLevel = 4,
            requiredAttribute = AttributeType.DIS,
            requiredAttributeValue = 13,
            treeColumn = 1,
            treeRow = 0
        ),
        SkillDefinition(
            id = "disc_time_mgmt",
            name = "Time Management",
            specialization = SkillSpecialization.ABSOLUTE_DISCIPLINE,
            tier = SkillTier.RARE,
            description = "Unlocks conflict-free deep-work slot prioritization in planning views.",
            requiredRank = HunterRank.D_RANK,
            requiredLevel = 7,
            requiredAttribute = AttributeType.DIS,
            requiredAttributeValue = 15,
            treeColumn = 0,
            treeRow = 1
        ),
        SkillDefinition(
            id = "disc_goal_completion",
            name = "Goal Completion",
            specialization = SkillSpecialization.ABSOLUTE_DISCIPLINE,
            tier = SkillTier.LEGENDARY,
            description = "Unlocks multi-milestone tactical goal planning and Sovereign campaign access.",
            requiredRank = HunterRank.B_RANK,
            requiredLevel = 18,
            requiredAttribute = AttributeType.DIS,
            requiredAttributeValue = 19,
            requiredVerifiedCount = 10,
            treeColumn = 1,
            treeRow = 1
        ),

        // D. Strategic Wisdom (3)
        SkillDefinition(
            id = "wis_reflection",
            name = "Reflection",
            specialization = SkillSpecialization.STRATEGIC_WISDOM,
            tier = SkillTier.COMMON,
            description = "Unlocks structured end-of-week retrospective insights across all life pillars.",
            requiredRank = HunterRank.E_RANK,
            requiredLevel = 2,
            requiredAttribute = AttributeType.WIS,
            requiredAttributeValue = 11,
            treeColumn = 0,
            treeRow = 0
        ),
        SkillDefinition(
            id = "wis_strategic_planning",
            name = "Strategic Planning",
            specialization = SkillSpecialization.STRATEGIC_WISDOM,
            tier = SkillTier.RARE,
            description = "Unlocks weekly workload balancing and multi-domain dungeon sequencing.",
            requiredRank = HunterRank.D_RANK,
            requiredLevel = 6,
            requiredAttribute = AttributeType.WIS,
            requiredAttributeValue = 14,
            treeColumn = 1,
            treeRow = 0
        ),
        SkillDefinition(
            id = "wis_self_awareness",
            name = "Self-Awareness",
            specialization = SkillSpecialization.STRATEGIC_WISDOM,
            tier = SkillTier.EPIC,
            description = "Surfaces fatigue vs. capacity divergence before burnout undermines progress.",
            requiredRank = HunterRank.C_RANK,
            requiredLevel = 12,
            requiredAttribute = AttributeType.WIS,
            requiredAttributeValue = 16,
            treeColumn = 0,
            treeRow = 1
        ),

        // E. Social Mastery (4)
        SkillDefinition(
            id = "soc_communication",
            name = "Communication",
            specialization = SkillSpecialization.SOCIAL_MASTERY,
            tier = SkillTier.COMMON,
            description = "Builds deliberate interpersonal clarity via social commitment objectives.",
            requiredRank = HunterRank.E_RANK,
            requiredLevel = 2,
            requiredAttribute = AttributeType.CHA,
            requiredAttributeValue = 11,
            treeColumn = 0,
            treeRow = 0
        ),
        SkillDefinition(
            id = "soc_confidence",
            name = "Confidence",
            specialization = SkillSpecialization.SOCIAL_MASTERY,
            tier = SkillTier.UNCOMMON,
            description = "Unlocks high-visibility presentation and social challenge presets.",
            requiredRank = HunterRank.E_RANK,
            requiredLevel = 4,
            requiredAttribute = AttributeType.CHA,
            requiredAttributeValue = 13,
            treeColumn = 1,
            treeRow = 0
        ),
        SkillDefinition(
            id = "soc_leadership",
            name = "Leadership",
            specialization = SkillSpecialization.SOCIAL_MASTERY,
            tier = SkillTier.RARE,
            description = "Unlocks accountability-leadership goal milestones and mentorship quests.",
            requiredRank = HunterRank.D_RANK,
            requiredLevel = 8,
            requiredAttribute = AttributeType.CHA,
            requiredAttributeValue = 15,
            treeColumn = 0,
            treeRow = 1
        ),
        SkillDefinition(
            id = "soc_collaboration",
            name = "Collaboration",
            specialization = SkillSpecialization.SOCIAL_MASTERY,
            tier = SkillTier.MYTHIC,
            description = "Unlocks premier cooperative project campaigns without competitive toxicity.",
            requiredRank = HunterRank.A_RANK,
            requiredLevel = 28,
            requiredAttribute = AttributeType.CHA,
            requiredAttributeValue = 22,
            requiredVerifiedCount = 15,
            treeColumn = 1,
            treeRow = 1
        )
    )

    val dependencies: List<SkillTreeDependency> = listOf(
        SkillTreeDependency("phys_agility", "phys_endurance", 1),
        SkillTreeDependency("phys_recovery", "phys_strength", 1),
        SkillTreeDependency("acad_retention", "acad_deep_focus", 1),
        SkillTreeDependency("acad_exam_prep", "acad_rapid_learning", 1),
        SkillTreeDependency("disc_time_mgmt", "disc_consistency", 1),
        SkillTreeDependency("disc_goal_completion", "disc_routine_mastery", 1),
        SkillTreeDependency("wis_self_awareness", "wis_strategic_planning", 1),
        SkillTreeDependency("soc_leadership", "soc_confidence", 1),
        SkillTreeDependency("soc_collaboration", "soc_leadership", 1)
    )

    val effects: List<SkillEffectSpec> = listOf(
        SkillEffectSpec("phys_endurance", "CHALLENGE_ENDURANCE", "Extended Aerobic Tiers", "Unlocks 45m & 60m endurance dungeon options.", "CHALLENGE_OPTION"),
        SkillEffectSpec("phys_strength", "CHALLENGE_STRENGTH", "Overload Dungeon Templates", "Unlocks structured calisthenics & strength dungeon challenges.", "CHALLENGE_OPTION"),
        SkillEffectSpec("phys_agility", "CHALLENGE_AGILITY", "Tempo Conditioning Modes", "Unlocks speed & interval challenge variants.", "CHALLENGE_OPTION"),
        SkillEffectSpec("phys_recovery", "CHALLENGE_RECOVERY_GUARD", "Adaptive Deload Advisory", "Highlights safe recovery pacing across physical dungeons.", "RECOVERY_INSIGHT"),
        SkillEffectSpec("acad_deep_focus", "FOCUS_PRESET_EXTENDED", "60m & 90m Deep Focus Presets", "Unlocks extended Focus Timer duration presets.", "FOCUS_PRESET"),
        SkillEffectSpec("acad_rapid_learning", "REVISION_RAPID_SYNTH", "Rapid Synthesis Planning", "Unlocks condensed chapter mastery milestones.", "REVISION_PLANNING"),
        SkillEffectSpec("acad_retention", "REVISION_SPACED_PRIORITY", "Retention Window Planner", "Unlocks advanced spaced-repetition sorting options.", "REVISION_PLANNING"),
        SkillEffectSpec("acad_exam_prep", "REVISION_EXAM_SPRINT", "Exam Prep Blueprints", "Unlocks multi-week examination readiness campaigns.", "REVISION_PLANNING"),
        SkillEffectSpec("disc_consistency", "ROUTINE_STREAK_ANALYTICS", "Streak & Rest Protection View", "Unlocks detailed consistency breakdown inside Discipline Analytics.", "ROUTINE_ANALYTICS"),
        SkillEffectSpec("disc_routine_mastery", "ROUTINE_ADHERENCE_MATRIX", "Routine Adherence Analytics", "Unlocks routine completion trend breakdowns.", "ROUTINE_ANALYTICS"),
        SkillEffectSpec("disc_time_mgmt", "GOAL_TIME_ALLOCATION", "Tactical Time Allocation", "Unlocks deep-work buffer prioritization in planning.", "GOAL_PLANNING"),
        SkillEffectSpec("disc_goal_completion", "GOAL_MILESTONE_OPTIMIZER", "Goal Milestone Optimizer", "Unlocks advanced multi-milestone goal pacing tools.", "GOAL_PLANNING"),
        SkillEffectSpec("wis_reflection", "WIS_WEEKLY_DIAGNOSTIC", "Weekly Diagnostic Lenses", "Enriches weekly review summaries with cross-domain cues.", "GOAL_PLANNING"),
        SkillEffectSpec("wis_strategic_planning", "WIS_SEQUENCING", "Strategic Gate Sequencing", "Highlights optimal dungeon-to-goal alignment.", "GOAL_PLANNING"),
        SkillEffectSpec("wis_self_awareness", "WIS_CAPACITY_CHECK", "Load-Capacity Early Warning", "Warns earlier when workload exceeds sustainable capacity.", "RECOVERY_INSIGHT"),
        SkillEffectSpec("soc_communication", "SOC_CLARITY", "Structured Communication Goals", "Unlocks social clarity challenge blueprints.", "CHALLENGE_OPTION"),
        SkillEffectSpec("soc_confidence", "SOC_VISIBILITY", "Public Presentation Trials", "Unlocks confidence & presentation quest templates.", "CHALLENGE_OPTION"),
        SkillEffectSpec("soc_leadership", "SOC_MENTORSHIP", "Leadership Campaign Templates", "Unlocks team coordination & mentorship milestones.", "GOAL_PLANNING"),
        SkillEffectSpec("soc_collaboration", "SOC_ALLIANCE", "Cooperative Initiative Templates", "Unlocks structured group project milestone sets.", "GOAL_PLANNING")
    )
}

object SkillEngine {

    fun evaluateSkillWithStatus(
        definition: SkillDefinition,
        dependencies: List<SkillTreeDependency>,
        effects: List<SkillEffectSpec>,
        unlockedMap: Map<String, SkillUnlockRecord>,
        profile: PlayerProfile
    ): SkillWithStatus {
        val currentRecord = unlockedMap[definition.id]
        val currentMastery = currentRecord?.masteryLevel ?: 0
        val isUnlocked = currentRecord != null

        val missing = mutableListOf<String>()

        if (isUnlocked && currentMastery >= definition.maxMasteryLevel) {
            missing.add("Maximum Mastery (${definition.maxMasteryLevel}/${definition.maxMasteryLevel}) reached")
        }

        val targetMasteryOffset = if (isUnlocked) currentMastery * 2 else 0
        if (profile.rank.ordinal < definition.requiredRank.ordinal) {
            missing.add("Requires ${definition.requiredRank.displayName}")
        }
        val reqLevel = definition.requiredLevel + targetMasteryOffset
        if (profile.level < reqLevel) {
            missing.add("Requires Level $reqLevel (current Lv.${profile.level})")
        }
        val reqAttrVal = definition.requiredAttributeValue + targetMasteryOffset
        val currentAttrVal = profile.attributes.asList()
            .find { it.type == definition.requiredAttribute }
            ?.totalValue ?: 0
        if (currentAttrVal < reqAttrVal) {
            missing.add("Requires ${definition.requiredAttribute.code} $reqAttrVal (current $currentAttrVal)")
        }
        if (profile.verifiedActivitiesCount < definition.requiredVerifiedCount) {
            missing.add("Requires ${definition.requiredVerifiedCount} verified activities (current ${profile.verifiedActivitiesCount})")
        }
        if (profile.streakDays < definition.requiredStreakDays) {
            missing.add("Requires ${definition.requiredStreakDays}-day streak (current ${profile.streakDays}d)")
        }

        val skillMap = SkillCatalog.skills.associateBy { it.id }
        dependencies.filter { it.skillId == definition.id }.forEach { dep ->
            val prereqRecord = unlockedMap[dep.prerequisiteSkillId]
            val prereqName = skillMap[dep.prerequisiteSkillId]?.name ?: dep.prerequisiteSkillId
            if (prereqRecord == null || prereqRecord.masteryLevel < dep.minPrerequisiteMastery) {
                missing.add("Requires prerequisite '$prereqName' Lv.${dep.minPrerequisiteMastery}")
            }
        }

        return SkillWithStatus(
            definition = definition,
            effects = effects.filter { it.skillId == definition.id },
            prerequisites = dependencies.filter { it.skillId == definition.id },
            unlockRecord = currentRecord,
            isUnlocked = isUnlocked,
            currentMastery = currentMastery,
            canUnlockOrUpgrade = missing.isEmpty(),
            missingRequirements = missing
        )
    }
}

/**
 * Deterministically evaluates gameplay & UI perks granted by unlocked skills.
 * Guarantee: NEVER alters health metrics, NEVER bypasses recovery safety guards, NEVER awards infinite XP.
 */
object SkillEffectEngine {

    fun evaluateActiveEffects(
        unlockedRecords: List<SkillUnlockRecord>,
        effectsCatalog: List<SkillEffectSpec> = SkillCatalog.effects
    ): ActiveSkillEffects {
        val unlockedIds = unlockedRecords.map { it.skillId }.toSet()
        val activeSpecs = effectsCatalog.filter { it.skillId in unlockedIds }

        val focusPresets = buildList {
            add(25)
            add(45)
            if ("acad_deep_focus" in unlockedIds) {
                add(60)
                add(90)
            }
        }

        val challengeTiers = buildList {
            add("Standard")
            if ("phys_endurance" in unlockedIds) add("Endurance")
            if ("phys_strength" in unlockedIds) add("Strength Overload")
            if ("phys_agility" in unlockedIds) add("Tempo Conditioning")
            if ("soc_communication" in unlockedIds || "soc_confidence" in unlockedIds) add("Social Challenge")
        }

        val routineAnalytics = activeSpecs.any { it.featureTag == "ROUTINE_ANALYTICS" }
        val revisionPlanning = activeSpecs.any { it.featureTag == "REVISION_PLANNING" }
        val goalPlanning = activeSpecs.any { it.featureTag == "GOAL_PLANNING" }

        val explanations = activeSpecs.map { spec ->
            val mastery = unlockedRecords.find { it.skillId == spec.skillId }?.masteryLevel ?: 1
            "${spec.title} (Lv.$mastery): ${spec.description}"
        }

        return ActiveSkillEffects(
            unlockedSkillCount = unlockedIds.size,
            unlockedFocusPresetsMinutes = focusPresets,
            unlockedChallengeTiers = challengeTiers,
            unlockedRoutineAnalytics = routineAnalytics,
            unlockedRevisionPlanningOptions = revisionPlanning,
            unlockedGoalPlanningTools = goalPlanning,
            activeEffectExplanations = explanations
        )
    }
}

// ============================================================================
// ACHIEVEMENTS, TITLES, INVENTORY & SHADOW COLLECTION CATALOGS & EVALUATOR
// ============================================================================

object RpgCatalogs {

    val titles: List<PlayerTitleDefinition> = listOf(
        PlayerTitleDefinition("title_system_initiate", "System Initiate", "Default awakening codename title.", RpgRarity.COMMON, "Granted upon System initialization"),
        PlayerTitleDefinition("title_first_blood", "First Awakening", "Completed your first real-world activity.", RpgRarity.COMMON, "Log 1 verified or manual activity"),
        PlayerTitleDefinition("title_gate_runner", "Gate Runner", "Cleared your first Dungeon Gate.", RpgRarity.UNCOMMON, "Clear Gate E-01 or 1 dungeon"),
        PlayerTitleDefinition("title_scholar", "Arcane Scholar", "Dedicated sustained hours to structured study.", RpgRarity.UNCOMMON, "Log 180+ total study minutes"),
        PlayerTitleDefinition("title_iron_will", "The Iron Will", "Conquered the Iron Consistency Warden or built a 7-day streak.", RpgRarity.RARE, "7-day streak or Warden defeat"),
        PlayerTitleDefinition("title_monolith_breaker", "Monolith Breaker", "Conquered the Monolith of Knowledge boss campaign.", RpgRarity.RARE, "Defeat Monolith of Knowledge"),
        PlayerTitleDefinition("title_iron_vanguard", "Iron Vanguard", "Completed D-Rank Verified Conditioning Trial.", RpgRarity.RARE, "Clear Gate D-01"),
        PlayerTitleDefinition("title_endurance_hunter", "Endurance Hunter", "Conquered the Colossus of Endurance.", RpgRarity.EPIC, "Defeat Colossus of Endurance"),
        PlayerTitleDefinition("title_vanguard_commander", "Vanguard Commander", "Cleared C-Rank Vanguard Crucible.", RpgRarity.EPIC, "Clear Gate C-01"),
        PlayerTitleDefinition("title_strategist", "The Strategist", "Mastered multiple specialization skills across trees.", RpgRarity.EPIC, "Unlock 5 skills across trees"),
        PlayerTitleDefinition("title_chronos_warden", "Chronos Warden", "Conquered B-Rank Sovereign Consistency Rift.", RpgRarity.LEGENDARY, "Clear Gate B-01"),
        PlayerTitleDefinition("title_crucible_monarch", "Crucible Monarch", "Conquered the Sovereign Crucible campaign.", RpgRarity.LEGENDARY, "Defeat Sovereign Crucible"),
        PlayerTitleDefinition("title_apex_challenger", "Apex Challenger", "Cleared A-Rank Apex Crimson Nexus.", RpgRarity.MYTHIC, "Clear Gate A-01"),
        PlayerTitleDefinition("title_shadow_sovereign", "The Sovereign", "Achieved S-Rank Mastery and cleared Monarch's Citadel.", RpgRarity.MYTHIC, "Achieve S-Rank or clear S-01")
    )

    val inventoryItems: List<InventoryItemDefinition> = listOf(
        InventoryItemDefinition("item_frame_obsidian_default", "Obsidian HUD Frame", InventoryCategory.PROFILE_FRAMES, RpgRarity.COMMON, "Standard midnight cyan holographic profile border.", "Default System gear"),
        InventoryItemDefinition("item_frame_obsidian_grid", "Grid Matrix Frame", InventoryCategory.PROFILE_FRAMES, RpgRarity.UNCOMMON, "Geometric tactical border unlocked via Vault of Order.", "Clear Gate E-02"),
        InventoryItemDefinition("item_frame_monarch_gold", "Sovereign Gold Frame", InventoryCategory.PROFILE_FRAMES, RpgRarity.LEGENDARY, "High-rank gold trim celebrating apex progression.", "Clear Gate A-01 or D-Rank Ascension"),
        InventoryItemDefinition("item_theme_midnight_cyan", "Midnight Cyan Theme", InventoryCategory.SYSTEM_THEMES, RpgRarity.COMMON, "Standard electric cyan HUD accent palette.", "Default System theme"),
        InventoryItemDefinition("item_theme_arcane_citadel", "Arcane Violet Theme", InventoryCategory.SYSTEM_THEMES, RpgRarity.RARE, "Deep arcane violet holographic resonance preset.", "Clear Gate D-02 or Study Milestone"),
        InventoryItemDefinition("item_badge_cyan_initiate", "Awakening Sigil Badge", InventoryCategory.PLAYER_BADGES, RpgRarity.COMMON, "Verified badge for completing Gate E-01.", "Clear Gate E-01"),
        InventoryItemDefinition("item_badge_weekly_conqueror", "Weekly Raider Badge", InventoryCategory.PLAYER_BADGES, RpgRarity.UNCOMMON, "Emblem earned for finishing a weekly dungeon gate.", "Clear a Weekly Citadel Gate"),
        InventoryItemDefinition("item_badge_warden_sigil", "Iron Warden Crest", InventoryCategory.PLAYER_BADGES, RpgRarity.RARE, "Crest symbolizing sustained habit & routine execution.", "Defeat Iron Consistency Warden"),
        InventoryItemDefinition("item_cosmetic_emerald_aura", "Sanctuary Recovery Aura", InventoryCategory.COSMETIC_ITEMS, RpgRarity.UNCOMMON, "Calm emerald HUD badge celebrating healthy recovery balance.", "Clear Sanctuary Gate"),
        InventoryItemDefinition("item_collectible_verified_seal", "Seal of Authenticity", InventoryCategory.COLLECTIBLES, RpgRarity.RARE, "Commemorative relic awarded for 5 Health Connect / Timer verified activities.", "5 verified activities", canEquip = false),
        InventoryItemDefinition("item_special_awakening_core", "Reality OS Resonator", InventoryCategory.SPECIAL_REWARDS, RpgRarity.EPIC, "Core artifact celebrating multi-domain development across Physical, Academic, and Life OS.", "Unlock 5 Achievements", canEquip = false)
    )

    val shadowCollectibles: List<ShadowCollectibleDefinition> = listOf(
        ShadowCollectibleDefinition(
            id = "shadow_obsidian_sentinel",
            codename = "Obsidian Sentinel",
            category = ShadowCollectionCategory.MILESTONE_GUARDIANS,
            rarity = RpgRarity.RARE,
            visualMotif = "Geometric Obsidian Shield-Construct with Electric Cyan Core",
            loreDescription = "An original crystalline guardian formed from sustained aerobic endurance and verified physical trials.",
            unlockSource = "Clear Gate D-01 or Defeat Colossus of Endurance"
        ),
        ShadowCollectibleDefinition(
            id = "shadow_astral_prism",
            codename = "Astral Prism",
            category = ShadowCollectionCategory.ACHIEVEMENT_COMPANIONS,
            rarity = RpgRarity.RARE,
            visualMotif = "Levitating Arcane Violet Octahedron with Telemetry Orbitals",
            loreDescription = "A geometric scholar companion crystallizing deep study focus and knowledge retention.",
            unlockSource = "Clear Gate D-02 or Defeat Monolith of Knowledge"
        ),
        ShadowCollectibleDefinition(
            id = "shadow_aegis_monolith",
            codename = "Aegis Monolith",
            category = ShadowCollectionCategory.SHADOW_COLLECTIBLES,
            rarity = RpgRarity.EPIC,
            visualMotif = "Segmented Steel-Cyan Pillars in Synchronous Orbit",
            loreDescription = "Represents unbroken multi-domain execution across fitness, study, and personal discipline.",
            unlockSource = "Clear Gate C-01"
        ),
        ShadowCollectibleDefinition(
            id = "shadow_vanguard_construct",
            codename = "Vanguard Construct",
            category = ShadowCollectionCategory.MILESTONE_GUARDIANS,
            rarity = RpgRarity.LEGENDARY,
            visualMotif = "Hexagonal Void-Frame Beacon with Gold Trim",
            loreDescription = "An abstract sovereign construct symbolizing mastery over multi-stage real-world campaigns.",
            unlockSource = "Defeat Sovereign Crucible"
        ),
        ShadowCollectibleDefinition(
            id = "shadow_chronos_spire",
            codename = "Chronos Spire",
            category = ShadowCollectionCategory.SPECIAL_EVENT_COLLECTIBLES,
            rarity = RpgRarity.LEGENDARY,
            visualMotif = "Floating Sundial Lattice in Cyan & Violet Resonance",
            loreDescription = "Artifact representing weeks of deliberate routine cadence and protected rest balance.",
            unlockSource = "Clear Gate B-01 or 14-day Streak"
        ),
        ShadowCollectibleDefinition(
            id = "shadow_apex_sovereign",
            codename = "Apex Sovereign Sigil",
            category = ShadowCollectionCategory.SHADOW_COLLECTIBLES,
            rarity = RpgRarity.MYTHIC,
            visualMotif = "Radiant Obsidian & Monarch Gold Geometric Star",
            loreDescription = "Original high-rank emblem symbolizing peak real-life RPG discipline.",
            unlockSource = "Clear Gate S-01 or Reach S-Rank"
        )
    )

    val achievements: List<AchievementDefinition> = listOf(
        AchievementDefinition("ach_first_blood", "First Step", "Log or complete your first real-world activity.", AchievementCategory.PERSONAL_MILESTONES, RpgRarity.COMMON, "1 total activity", 1, xpBonus = 25, rewardTitleId = "title_first_blood"),
        AchievementDefinition("ach_phys_verified_5", "Verified Athlete", "Complete 5 Health Connect verified workouts or runs.", AchievementCategory.PHYSICAL, RpgRarity.RARE, "5 verified physical sessions", 5, xpBonus = 50, rewardItemId = "item_collectible_verified_seal"),
        AchievementDefinition("ach_acad_scholar", "Focused Scholar", "Accumulate 180 minutes of study sessions.", AchievementCategory.ACADEMIC, RpgRarity.UNCOMMON, "180 study minutes", 180, xpBonus = 45, rewardTitleId = "title_scholar"),
        AchievementDefinition("ach_disc_tasks_5", "Executor of Order", "Complete 5 personal tasks in Life Management.", AchievementCategory.DISCIPLINE, RpgRarity.UNCOMMON, "5 completed tasks", 5, xpBonus = 35),
        AchievementDefinition("ach_consistency_7", "Unbroken Seven", "Build a 7-day consistency streak.", AchievementCategory.CONSISTENCY, RpgRarity.RARE, "7-day streak", 7, xpBonus = 60, rewardTitleId = "title_iron_will"),
        AchievementDefinition("ach_quests_5", "Quest Clearer", "Complete 5 system or academic quests.", AchievementCategory.QUESTS, RpgRarity.UNCOMMON, "5 completed quests", 5, xpBonus = 40),
        AchievementDefinition("ach_dungeon_first", "Dungeon Gate Clearer", "Clear your first Dungeon Gate with validated objectives.", AchievementCategory.DUNGEONS, RpgRarity.UNCOMMON, "1 cleared dungeon", 1, xpBonus = 45, rewardTitleId = "title_gate_runner"),
        AchievementDefinition("ach_dungeon_5", "Gate Conqueror", "Clear 5 Dungeon Gates across any difficulty.", AchievementCategory.DUNGEONS, RpgRarity.EPIC, "5 cleared dungeons", 5, xpBonus = 85),
        AchievementDefinition("ach_boss_first", "Boss Slayer", "Defeat your first multi-stage Boss Battle campaign.", AchievementCategory.BOSS_BATTLES, RpgRarity.RARE, "1 defeated boss", 1, xpBonus = 75, rewardShadowId = "shadow_astral_prism"),
        AchievementDefinition("ach_rank_d", "D-Rank Ascension", "Earn D-Rank promotion through verified progression.", AchievementCategory.LEVELS_AND_RANKS, RpgRarity.RARE, "Reach D-Rank", 1, xpBonus = 65, rewardItemId = "item_frame_monarch_gold"),
        AchievementDefinition("ach_skills_5", "Skill Architect", "Unlock 5 distinct skills across specialization trees.", AchievementCategory.SPECIAL_CHALLENGES, RpgRarity.EPIC, "5 unlocked skills", 5, xpBonus = 70, rewardTitleId = "title_strategist", rewardItemId = "item_special_awakening_core")
    )
}

object AchievementEngine {

    fun evaluateAll(
        telemetry: PlayerTelemetrySnapshot,
        existingUnlocks: Map<String, AchievementUnlockRecord>
    ): List<AchievementWithStatus> {
        val verifiedPhysical = telemetry.jogs.count { it.verificationStatus.isVerified } +
            telemetry.workouts.count { it.verificationStatus.isVerified }
        val totalStudyMins = telemetry.studySessions.sumOf { it.durationMinutes }
        val completedTasks = telemetry.completedTasks.count { it.status == com.example.domain.model.PersonalTaskStatus.COMPLETED }

        return RpgCatalogs.achievements.map { def ->
            val currentVal = when (def.id) {
                "ach_first_blood" -> telemetry.profile.totalActivitiesCount
                "ach_phys_verified_5" -> verifiedPhysical
                "ach_acad_scholar" -> totalStudyMins
                "ach_disc_tasks_5" -> completedTasks
                "ach_consistency_7" -> telemetry.profile.streakDays
                "ach_quests_5" -> telemetry.completedQuestsCount
                "ach_dungeon_first" -> telemetry.completedDungeonsCount
                "ach_dungeon_5" -> telemetry.completedDungeonsCount
                "ach_boss_first" -> telemetry.completedBossesCount
                "ach_rank_d" -> if (telemetry.profile.rank.ordinal >= HunterRank.D_RANK.ordinal) 1 else 0
                "ach_skills_5" -> telemetry.unlockedSkillsCount
                else -> 0
            }
            val unlock = existingUnlocks[def.id]
            AchievementWithStatus(
                definition = def,
                currentValue = currentVal.coerceAtLeast(0),
                isUnlocked = unlock != null,
                unlockRecord = unlock
            )
        }
    }

    fun findNewlyEligibleAchievements(
        telemetry: PlayerTelemetrySnapshot,
        existingUnlocks: Map<String, AchievementUnlockRecord>
    ): List<Pair<AchievementDefinition, String>> {
        val evaluated = evaluateAll(telemetry, existingUnlocks)
        return evaluated.filter { !it.isUnlocked && it.currentValue >= it.definition.targetValue }
            .map { status ->
                val evidence = "${status.definition.criterionDescription}: achieved ${status.currentValue}/${status.definition.targetValue}"
                status.definition to evidence
            }
    }
}
