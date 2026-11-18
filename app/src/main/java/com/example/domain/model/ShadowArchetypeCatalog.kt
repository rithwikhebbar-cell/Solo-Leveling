package com.example.domain.model

data class ShadowArchetype(
    val archetypeId: String,
    val name: String,
    val shadowClass: ShadowClass,
    val combatRole: String,
    val rarity: ShadowRarity,
    val rank: ShadowRank,
    val basePower: Int,
    val baseEndurance: Int,
    val baseFocus: Int,
    val baseDiscipline: Int,
    val baseWisdom: Int,
    val baseAdaptability: Int,
    val visualDescription: String,
    val recruitmentRequirement: String,
    val loreDescription: String,
    val passiveTrait: String
)

object ShadowArchetypeCatalog {
    val archetypes: List<ShadowArchetype> = listOf(
        // ---------------------------------------------------------------------
        // VANGUARD (Offensive & Physical Development Specialist)
        // ---------------------------------------------------------------------
        ShadowArchetype(
            archetypeId = "arch_igris_blade",
            name = "Crimson Knight Commander",
            shadowClass = ShadowClass.VANGUARD,
            combatRole = "Frontline Vanguard Commander",
            rarity = ShadowRarity.EPIC,
            rank = ShadowRank.D_RANK,
            basePower = 22,
            baseEndurance = 20,
            baseFocus = 18,
            baseDiscipline = 16,
            baseWisdom = 15,
            baseAdaptability = 16,
            visualDescription = "Clad in crimson-trimmed plate armor with a flowing plume helm and glowing visor.",
            recruitmentRequirement = "Complete Boss Battle: Crimson Vanguard Commander",
            loreDescription = "A master swordsman whose loyalty was forged in the crucible of eternal discipline.",
            passiveTrait = "Battle Fervor: Inspires rigorous preparation before physical development sessions."
        ),
        ShadowArchetype(
            archetypeId = "arch_bellion_grand",
            name = "Primordial Grand Marshal",
            shadowClass = ShadowClass.VANGUARD,
            combatRole = "Grand Army Commander",
            rarity = ShadowRarity.LEGENDARY,
            rank = ShadowRank.A_RANK,
            basePower = 32,
            baseEndurance = 30,
            baseFocus = 28,
            baseDiscipline = 30,
            baseWisdom = 28,
            baseAdaptability = 29,
            visualDescription = "The primordial shadow soldier bearing a massive twin-winged greatblade and ancient armor.",
            recruitmentRequirement = "Complete Monarch's Trial & Reach Rank S",
            loreDescription = "The ancient vanguard commander of the shadow realm, commanding absolute loyalty.",
            passiveTrait = "Primordial Authority: Elevates maximum army capacity and formation stability."
        ),
        ShadowArchetype(
            archetypeId = "arch_iron_sentinel",
            name = "Iron Sentinel",
            shadowClass = ShadowClass.VANGUARD,
            combatRole = "Heavy Shock Footman",
            rarity = ShadowRarity.COMMON,
            rank = ShadowRank.E_RANK,
            basePower = 14,
            baseEndurance = 12,
            baseFocus = 8,
            baseDiscipline = 10,
            baseWisdom = 6,
            baseAdaptability = 10,
            visualDescription = "A solid shadow foot soldier carrying a jagged black broadsword and crested iron shield.",
            recruitmentRequirement = "Clear any qualifying E-Rank Gate",
            loreDescription = "A stalwart standard footman who marches without complaint into battle.",
            passiveTrait = "Unyielding March: Provides a steady foundation for routine physical challenges."
        ),
        ShadowArchetype(
            archetypeId = "arch_storm_striker",
            name = "Storm Blade Vanguard",
            shadowClass = ShadowClass.VANGUARD,
            combatRole = "Tempest Flank Assault",
            rarity = ShadowRarity.RARE,
            rank = ShadowRank.C_RANK,
            basePower = 20,
            baseEndurance = 16,
            baseFocus = 15,
            baseDiscipline = 14,
            baseWisdom = 12,
            baseAdaptability = 18,
            visualDescription = "Wreathed in crackling blue plasma currents, wielding dual electrifying scimitars.",
            recruitmentRequirement = "Complete Dungeon: Lightning Wyrm Cavern",
            loreDescription = "Strikes with the sudden fury of a thunderclap across disrupted enemy formations.",
            passiveTrait = "Static Surge: Enhances dynamic workout pacing and high-intensity interval consistency."
        ),

        // ---------------------------------------------------------------------
        // GUARDIAN (Defensive & Consistency/Recovery Specialist)
        // ---------------------------------------------------------------------
        ShadowArchetype(
            archetypeId = "arch_tank_tank",
            name = "Ursine Iron Vanguard",
            shadowClass = ShadowClass.GUARDIAN,
            combatRole = "Unyielding Fortress",
            rarity = ShadowRarity.RARE,
            rank = ShadowRank.D_RANK,
            basePower = 18,
            baseEndurance = 25,
            baseFocus = 14,
            baseDiscipline = 20,
            baseWisdom = 12,
            baseAdaptability = 14,
            visualDescription = "A towering ursine shadow beast bearing heavy spiked armor and unbreakable resolve.",
            recruitmentRequirement = "Complete Dungeon: Iron Beast Lair",
            loreDescription = "A colossal beast vanguard that absorbs physical strain and fortifies endurance.",
            passiveTrait = "Fortitude Shield: Stabilizes discipline metrics during strenuous routines."
        ),
        ShadowArchetype(
            archetypeId = "arch_obsidian_bulwark",
            name = "Obsidian Bulwark",
            shadowClass = ShadowClass.GUARDIAN,
            combatRole = "Heavy Rampart Guardian",
            rarity = ShadowRarity.COMMON,
            rank = ShadowRank.E_RANK,
            basePower = 10,
            baseEndurance = 18,
            baseFocus = 8,
            baseDiscipline = 14,
            baseWisdom = 8,
            baseAdaptability = 10,
            visualDescription = "A monolithic shadowy figure formed from interlocking jagged obsidian slabs.",
            recruitmentRequirement = "Log 3 consecutive days of completed physical routines",
            loreDescription = "Formed from condensed void bedrock, it deflects doubt and exhaustion.",
            passiveTrait = "Rockbound Stance: Reduces discipline decay when workout intensity peaks."
        ),
        ShadowArchetype(
            archetypeId = "arch_frost_colossus",
            name = "Frost Aegis Colossus",
            shadowClass = ShadowClass.GUARDIAN,
            combatRole = "Glacial Barrier Commander",
            rarity = ShadowRarity.EPIC,
            rank = ShadowRank.C_RANK,
            basePower = 19,
            baseEndurance = 28,
            baseFocus = 16,
            baseDiscipline = 22,
            baseWisdom = 15,
            baseAdaptability = 15,
            visualDescription = "A colossal armor-clad guardian radiating sub-zero mist with an ornate crystal greatshield.",
            recruitmentRequirement = "Complete Boss Battle: Frozen Sovereign Remnant",
            loreDescription = "Encases the monarch's resolve in impenetrable permafrost, halting burnout.",
            passiveTrait = "Glacial Ward: Surfaces deep recovery alerts before systemic fatigue accumulates."
        ),
        ShadowArchetype(
            archetypeId = "arch_adamant_gargoyle",
            name = "Adamant Gargoyle",
            shadowClass = ShadowClass.GUARDIAN,
            combatRole = "Aerial Perimeter Sentinel",
            rarity = ShadowRarity.UNCOMMON,
            rank = ShadowRank.E_RANK,
            basePower = 14,
            baseEndurance = 19,
            baseFocus = 11,
            baseDiscipline = 15,
            baseWisdom = 10,
            baseAdaptability = 12,
            visualDescription = "A stone-winged predatory sentinel perching vigilantly with glowing cyan runes.",
            recruitmentRequirement = "Complete Dungeon: Sunken Cathedral",
            loreDescription = "Maintains unblinking vigilance over daily habit queues.",
            passiveTrait = "Perimeter Vigil: Guards scheduled routines against unexpected delays."
        ),

        // ---------------------------------------------------------------------
        // RANGER (Precision & Academic/Focus Specialist)
        // ---------------------------------------------------------------------
        ShadowArchetype(
            archetypeId = "arch_kaisel_ranger",
            name = "Kaisel the Sky Sovereign",
            shadowClass = ShadowClass.RANGER,
            combatRole = "Aerial Recon & Strike",
            rarity = ShadowRarity.EPIC,
            rank = ShadowRank.C_RANK,
            basePower = 21,
            baseEndurance = 18,
            baseFocus = 20,
            baseDiscipline = 19,
            baseWisdom = 18,
            baseAdaptability = 24,
            visualDescription = "A majestic skeletal wyvern wreathed in blue flame with wings of absolute darkness.",
            recruitmentRequirement = "Complete Dungeon: Demon Castle Floor 50",
            loreDescription = "Soars above administrative bottlenecks to survey the broader strategic landscape.",
            passiveTrait = "Aerial Vantage: Highlights high-priority tasks and long-term milestones."
        ),
        ShadowArchetype(
            archetypeId = "arch_echo_archer",
            name = "Echo Scout Ranger",
            shadowClass = ShadowClass.RANGER,
            combatRole = "Long-Range Marksman",
            rarity = ShadowRarity.COMMON,
            rank = ShadowRank.E_RANK,
            basePower = 12,
            baseEndurance = 10,
            baseFocus = 16,
            baseDiscipline = 12,
            baseWisdom = 10,
            baseAdaptability = 14,
            visualDescription = "A slender shadow archer drawing a recurring energy recurve bow with steady aim.",
            recruitmentRequirement = "Complete 1 Academic Focus Session",
            loreDescription = "Tracks distant goals with pinpoint focus and unswerving calm.",
            passiveTrait = "Focus Calibration: Improves academic study sprint concentration."
        ),
        ShadowArchetype(
            archetypeId = "arch_shadow_sniper",
            name = "Phantom Bow Sniper",
            shadowClass = ShadowClass.RANGER,
            combatRole = "Ultra-Long Precision Specialist",
            rarity = ShadowRarity.RARE,
            rank = ShadowRank.D_RANK,
            basePower = 18,
            baseEndurance = 14,
            baseFocus = 24,
            baseDiscipline = 16,
            baseWisdom = 14,
            baseAdaptability = 18,
            visualDescription = "A cloaked phantom operating a crystalline arbalest that hums with quiet power.",
            recruitmentRequirement = "Unlock Achievement: Deep Work Marathon",
            loreDescription = "Eliminates cognitive distractions from across the battlefield.",
            passiveTrait = "Zero Distraction: Enhances uninterrupted focus during study blocks."
        ),
        ShadowArchetype(
            archetypeId = "arch_tempest_falcon",
            name = "Tempest Falcon",
            shadowClass = ShadowClass.RANGER,
            combatRole = "Rapid Aerial Scout",
            rarity = ShadowRarity.UNCOMMON,
            rank = ShadowRank.E_RANK,
            basePower = 13,
            baseEndurance = 12,
            baseFocus = 18,
            baseDiscipline = 13,
            baseWisdom = 11,
            baseAdaptability = 19,
            visualDescription = "A sleek raptor of condensed shadows leaving vapor trails of midnight mist.",
            recruitmentRequirement = "Complete 3 rapid review sessions in a single week",
            loreDescription = "Delivers instantaneous recon on pending coursework and tasks.",
            passiveTrait = "Rapid Horizon: Highlights impending deadlines before they become emergencies."
        ),

        // ---------------------------------------------------------------------
        // ARCANIST (Intelligence & Analysis Specialist)
        // ---------------------------------------------------------------------
        ShadowArchetype(
            archetypeId = "arch_tusk_mage",
            name = "Tusk the Arcanist",
            shadowClass = ShadowClass.ARCANIST,
            combatRole = "Arcane Artillery Specialist",
            rarity = ShadowRarity.EPIC,
            rank = ShadowRank.C_RANK,
            basePower = 15,
            baseEndurance = 14,
            baseFocus = 26,
            baseDiscipline = 18,
            baseWisdom = 24,
            baseAdaptability = 20,
            visualDescription = "A robed high-orc sorcerer crackling with violet lightning and floating runic tomes.",
            recruitmentRequirement = "Complete Academic Quest: Advanced Calculus Mastery",
            loreDescription = "Master of destructive arcane formulae and intellectual focus.",
            passiveTrait = "Arcane Analysis: Provides deep analytical breakdowns of study and research output."
        ),
        ShadowArchetype(
            archetypeId = "arch_void_weaver",
            name = "Void Scripture Weaver",
            shadowClass = ShadowClass.ARCANIST,
            combatRole = "Runic Formulation Adept",
            rarity = ShadowRarity.COMMON,
            rank = ShadowRank.E_RANK,
            basePower = 10,
            baseEndurance = 10,
            baseFocus = 16,
            baseDiscipline = 12,
            baseWisdom = 16,
            baseAdaptability = 12,
            visualDescription = "A cowl-clad scribe whose fingers weave shimmering violet glyphs in mid-air.",
            recruitmentRequirement = "Complete 1 Academic Revision Cycle",
            loreDescription = "Decodes complex domain knowledge into structured memory constructs.",
            passiveTrait = "Glyph Mnemonic: Boosts revision retention and academic exam preparation."
        ),
        ShadowArchetype(
            archetypeId = "arch_astral_sage",
            name = "Astral Starcaller",
            shadowClass = ShadowClass.ARCANIST,
            combatRole = "Celestial Divination Specialist",
            rarity = ShadowRarity.RARE,
            rank = ShadowRank.D_RANK,
            basePower = 16,
            baseEndurance = 14,
            baseFocus = 22,
            baseDiscipline = 16,
            baseWisdom = 23,
            baseAdaptability = 18,
            visualDescription = "Surrounded by an orbiting orrery of starlit shadow spheres and ancient astrolabes.",
            recruitmentRequirement = "Achieve Grade A on any comprehensive examination",
            loreDescription = "Translates theoretical models into clear cognitive insights.",
            passiveTrait = "Starlight Computation: Illuminates difficult concepts during complex problem solving."
        ),
        ShadowArchetype(
            archetypeId = "arch_nether_pyromancer",
            name = "Nether Flame Sorcerer",
            shadowClass = ShadowClass.ARCANIST,
            combatRole = "Combustion Area Denial",
            rarity = ShadowRarity.UNCOMMON,
            rank = ShadowRank.E_RANK,
            basePower = 16,
            baseEndurance = 11,
            baseFocus = 18,
            baseDiscipline = 13,
            baseWisdom = 17,
            baseAdaptability = 13,
            visualDescription = "Burns with cold black-violet flames that consume intellectual inertia.",
            recruitmentRequirement = "Complete Dungeon: Magma Vault Gate",
            loreDescription = "Incinerates procrastination and clears mental lethargy.",
            passiveTrait = "Purging Flame: Accelerates initial task startup and momentum acquisition."
        ),

        // ---------------------------------------------------------------------
        // TACTICIAN (Strategic & Planning/Execution Specialist)
        // ---------------------------------------------------------------------
        ShadowArchetype(
            archetypeId = "arch_fang_assassin",
            name = "Fang the Silent Stalker",
            shadowClass = ShadowClass.TACTICIAN,
            combatRole = "Precision Eliminator",
            rarity = ShadowRarity.RARE,
            rank = ShadowRank.D_RANK,
            basePower = 24,
            baseEndurance = 12,
            baseFocus = 22,
            baseDiscipline = 15,
            baseWisdom = 14,
            baseAdaptability = 22,
            visualDescription = "A fluid shadow figure draped in obsidian cowls wielding dual-bladed daggers.",
            recruitmentRequirement = "Complete Social Mission: Active Listening & Speed Drills",
            loreDescription = "Strikes from the blind spots of hesitation with absolute precision.",
            passiveTrait = "Shadow Strike: Enhances task execution speed and rapid response tracking."
        ),
        ShadowArchetype(
            archetypeId = "arch_beru_marshal",
            name = "Beru the Ant Marshal",
            shadowClass = ShadowClass.TACTICIAN,
            combatRole = "Elite Royal Guard",
            rarity = ShadowRarity.LEGENDARY,
            rank = ShadowRank.B_RANK,
            basePower = 28,
            baseEndurance = 22,
            baseFocus = 25,
            baseDiscipline = 26,
            baseWisdom = 20,
            baseAdaptability = 27,
            visualDescription = "A humanoid chitinous ant monarch exhibiting aristocratic menace and razor-sharp claws.",
            recruitmentRequirement = "Earn Achievement: Ant King Subjugation",
            loreDescription = "Devoted entirely to the Monarch's absolute supremacy and elite efficiency.",
            passiveTrait = "Royal Command: Boosts overall system productivity and quest completion rewards."
        ),
        ShadowArchetype(
            archetypeId = "arch_chrono_stalker",
            name = "Chrono Blade Assassin",
            shadowClass = ShadowClass.TACTICIAN,
            combatRole = "Temporal Ambush Operator",
            rarity = ShadowRarity.UNCOMMON,
            rank = ShadowRank.E_RANK,
            basePower = 18,
            baseEndurance = 12,
            baseFocus = 16,
            baseDiscipline = 14,
            baseWisdom = 13,
            baseAdaptability = 19,
            visualDescription = "Blurs across multiple timeline shadows, leaving echoes of curved daggers.",
            recruitmentRequirement = "Complete 5 tasks strictly on or before their schedule deadline",
            loreDescription = "Executes schedule transitions with microsecond accuracy.",
            passiveTrait = "Temporal Slip: Mitigates time leakage between scheduled calendar blocks."
        ),
        ShadowArchetype(
            archetypeId = "arch_phantom_schemer",
            name = "Shadow Grid Architect",
            shadowClass = ShadowClass.TACTICIAN,
            combatRole = "Strategic Mastermind",
            rarity = ShadowRarity.COMMON,
            rank = ShadowRank.E_RANK,
            basePower = 11,
            baseEndurance = 11,
            baseFocus = 17,
            baseDiscipline = 15,
            baseWisdom = 16,
            baseAdaptability = 16,
            visualDescription = "A tall shadowy figure manipulating a floating blueprint matrix of mission vectors.",
            recruitmentRequirement = "Create and organize a full weekly schedule",
            loreDescription = "Translates chaotic daily demands into an orderly tactical roadmap.",
            passiveTrait = "Grid Optimization: Automatically highlights daily scheduling bottlenecks."
        ),

        // ---------------------------------------------------------------------
        // SUPPORT (Discipline, Habits, Vitality & Morale Specialist)
        // ---------------------------------------------------------------------
        ShadowArchetype(
            archetypeId = "arch_disciplined_steward",
            name = "Discipline Steward",
            shadowClass = ShadowClass.SUPPORT,
            combatRole = "Habit & Routine Overseer",
            rarity = ShadowRarity.COMMON,
            rank = ShadowRank.E_RANK,
            basePower = 9,
            baseEndurance = 15,
            baseFocus = 13,
            baseDiscipline = 18,
            baseWisdom = 14,
            baseAdaptability = 13,
            visualDescription = "A composed figure bearing an hourglass of shadow sand and a ledger of persistence.",
            recruitmentRequirement = "Complete all daily morning routines without failure",
            loreDescription = "Enforces quiet consistency through patient and unyielding devotion.",
            passiveTrait = "Discipline Anchor: Prevents routine streak loss during minor disruptions."
        ),
        ShadowArchetype(
            archetypeId = "arch_vital_herald",
            name = "Vital Echo Herald",
            shadowClass = ShadowClass.SUPPORT,
            combatRole = "Recovery & Morale Booster",
            rarity = ShadowRarity.UNCOMMON,
            rank = ShadowRank.E_RANK,
            basePower = 11,
            baseEndurance = 16,
            baseFocus = 12,
            baseDiscipline = 16,
            baseWisdom = 15,
            baseAdaptability = 15,
            visualDescription = "Carries a bronze horn that sounds inaudibly, sending waves of soothing shadow warmth.",
            recruitmentRequirement = "Log 8 hours of restorative sleep across 3 consecutive nights",
            loreDescription = "Rejuvenates tired muscles and refreshes cognitive clarity after heavy days.",
            passiveTrait = "Restoration Pulse: Elevates recovery scores following rigorous physical sessions."
        ),
        ShadowArchetype(
            archetypeId = "arch_seraphic_shadow",
            name = "Seraphic Shroud Support",
            shadowClass = ShadowClass.SUPPORT,
            combatRole = "Guardian Empath & Balancer",
            rarity = ShadowRarity.RARE,
            rank = ShadowRank.D_RANK,
            basePower = 13,
            baseEndurance = 18,
            baseFocus = 18,
            baseDiscipline = 19,
            baseWisdom = 21,
            baseAdaptability = 18,
            visualDescription = "An ethereal winged silhouette draped in flowing dark silk that gently mends fraying spirits.",
            recruitmentRequirement = "Complete Social Mission: Empathetic De-escalation & Mediation",
            loreDescription = "Maintains balance between ambition, social presence, and mental wellness.",
            passiveTrait = "Harmonic Equilibrium: Stabilizes life balance scores across all domains."
        ),
        ShadowArchetype(
            archetypeId = "arch_monarch_sovereign_core",
            name = "Monarch Sovereign Essence",
            shadowClass = ShadowClass.SUPPORT,
            combatRole = "Army Harmony Core",
            rarity = ShadowRarity.MYTHIC,
            rank = ShadowRank.S_RANK,
            basePower = 28,
            baseEndurance = 28,
            baseFocus = 30,
            baseDiscipline = 32,
            baseWisdom = 32,
            baseAdaptability = 30,
            visualDescription = "The supreme manifestation of monarchic will, crowned in midnight auroras and eternal stars.",
            recruitmentRequirement = "Reach Monarch Level 30 & Maintain 90% System Discipline for 30 Days",
            loreDescription = "The ultimate synthesis of purpose and mastery, binding all shadows into one unbroken army.",
            passiveTrait = "Sovereign Harmony: Maximizes all active deployment synergies across the entire realm."
        )
    )

    val totalCount: Int get() = archetypes.size

    fun findById(archetypeId: String): ShadowArchetype? =
        archetypes.firstOrNull { it.archetypeId == archetypeId }

    fun findByName(name: String): ShadowArchetype? =
        archetypes.firstOrNull { it.name.equals(name, ignoreCase = true) }

    fun findForClass(shadowClass: ShadowClass): List<ShadowArchetype> =
        archetypes.filter { it.shadowClass == shadowClass }

    fun findForRarity(rarity: ShadowRarity): List<ShadowArchetype> =
        archetypes.filter { it.rarity == rarity }

    fun findForRank(rank: ShadowRank): List<ShadowArchetype> =
        archetypes.filter { it.rank == rank }
}
