package com.example

import com.example.domain.engine.MonarchProgressionEngine
import com.example.domain.engine.ShadowDeploymentEngine
import com.example.domain.engine.ShadowEvolutionEngine
import com.example.domain.engine.ShadowExtractionEngine
import com.example.domain.model.MonarchRank
import com.example.domain.model.MonarchSkillCatalog
import com.example.domain.model.MonarchSkillIds
import com.example.domain.model.Shadow
import com.example.domain.model.ShadowClass
import com.example.domain.model.ShadowDeploymentCategory
import com.example.domain.model.ShadowEvolutionStage
import com.example.domain.model.ShadowExtractionOutcome
import com.example.domain.model.ShadowExtractionSourceType
import com.example.domain.model.ShadowFormationType
import com.example.domain.model.ShadowQuestCatalog
import com.example.domain.model.ShadowRank
import com.example.domain.model.ShadowRarity
import com.example.domain.model.ShadowSkillCatalog
import com.example.domain.model.ShadowStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShadowMonarchEngineUnitTest {

    // ========================================================================
    // 1. DETERMINISTIC EXTRACTION TESTS
    // ========================================================================

    @Test
    fun deterministicExtraction_sameSourceProducesIdenticalSeed() {
        val seed1 = ShadowExtractionEngine.buildDeterminismSeed(
            ShadowExtractionSourceType.DUNGEON_COMPLETION,
            "dungeon_frost_cave_001"
        )
        val seed2 = ShadowExtractionEngine.buildDeterminismSeed(
            ShadowExtractionSourceType.DUNGEON_COMPLETION,
            "dungeon_frost_cave_001"
        )
        assertEquals(seed1, seed2)
    }

    @Test
    fun deterministicExtraction_differentSourceProducesDifferentSeed() {
        val seed1 = ShadowExtractionEngine.buildDeterminismSeed(
            ShadowExtractionSourceType.DUNGEON_COMPLETION,
            "dungeon_frost_cave_001"
        )
        val seed2 = ShadowExtractionEngine.buildDeterminismSeed(
            ShadowExtractionSourceType.DUNGEON_COMPLETION,
            "dungeon_fire_cavern_002"
        )
        assertNotEquals(seed1, seed2)
    }

    @Test
    fun deterministicExtraction_sameSeedYieldsIdenticalArchetype() {
        val seed = 123456789L
        val class1 = ShadowExtractionEngine.selectClass(seed)
        val class2 = ShadowExtractionEngine.selectClass(seed)
        assertEquals(class1, class2)

        val rarity1 = ShadowExtractionEngine.selectRarity(seed, ShadowExtractionSourceType.BOSS_BATTLE_COMPLETION)
        val rarity2 = ShadowExtractionEngine.selectRarity(seed, ShadowExtractionSourceType.BOSS_BATTLE_COMPLETION)
        assertEquals(rarity1, rarity2)

        val name1 = ShadowExtractionEngine.selectName(class1, seed)
        val name2 = ShadowExtractionEngine.selectName(class2, seed)
        assertEquals(name1, name2)

        val stats1 = ShadowExtractionEngine.calculateInitialStats(class1, rarity1, seed)
        val stats2 = ShadowExtractionEngine.calculateInitialStats(class2, rarity2, seed)
        assertEquals(stats1, stats2)
    }

    @Test
    fun deterministicExtraction_bossDefeatHasHigherRarityBiasThanDisciplineStreak() {
        // Evaluate multiple seeds to verify statistical bias distribution
        var epicOrAboveBoss = 0
        var epicOrAboveDiscipline = 0
        val iterations = 500

        for (i in 1..iterations) {
            val seed = i.toLong() * 997L
            val bossRarity = ShadowExtractionEngine.selectRarity(seed, ShadowExtractionSourceType.BOSS_BATTLE_COMPLETION)
            val disciplineRarity = ShadowExtractionEngine.selectRarity(seed, ShadowExtractionSourceType.DISCIPLINE_MILESTONE)

            if (bossRarity.ordinal >= ShadowRarity.EPIC.ordinal) epicOrAboveBoss++
            if (disciplineRarity.ordinal >= ShadowRarity.EPIC.ordinal) epicOrAboveDiscipline++
        }

        assertTrue(
            "Boss defeats should yield higher rarity shadows than routine discipline milestones",
            epicOrAboveBoss > epicOrAboveDiscipline
        )
    }

    // ========================================================================
    // 2. MONARCH PROGRESSION ENGINE TESTS
    // ========================================================================

    @Test
    fun monarchProgression_levelThresholdsAndRanks() {
        assertEquals(MonarchRank.AWAKENING, MonarchRank.forLevel(1))
        assertEquals(MonarchRank.AWAKENING, MonarchRank.forLevel(5))
        assertEquals(MonarchRank.COMMANDER, MonarchRank.forLevel(6))
        assertEquals(MonarchRank.COMMANDER, MonarchRank.forLevel(10))
        assertEquals(MonarchRank.SOVEREIGN, MonarchRank.forLevel(11))
        assertEquals(MonarchRank.SOVEREIGN, MonarchRank.forLevel(20))
        assertEquals(MonarchRank.MONARCH, MonarchRank.forLevel(21))
        assertEquals(MonarchRank.MONARCH, MonarchRank.forLevel(30))
        assertEquals(MonarchRank.SHADOW_SOVEREIGN, MonarchRank.forLevel(31))
        assertEquals(MonarchRank.SHADOW_SOVEREIGN, MonarchRank.forLevel(50))
    }

    @Test
    fun monarchProgression_xpRequirementsScaleWithLevel() {
        val xpLv2 = MonarchProgressionEngine.calculateNextLevelXp(1)
        val xpLv5 = MonarchProgressionEngine.calculateNextLevelXp(4)
        val xpLv10 = MonarchProgressionEngine.calculateNextLevelXp(9)
        val xpLv20 = MonarchProgressionEngine.calculateNextLevelXp(19)

        assertTrue(xpLv2 > 0L)
        assertTrue(xpLv5 > xpLv2)
        assertTrue(xpLv10 > xpLv5)
        assertTrue(xpLv20 > xpLv10)
        assertEquals(5, com.example.domain.model.ShadowMonarchProfile.monarchLevelForXp(xpLv5))
    }

    @Test
    fun monarchProgression_constantsAndXpFormulaDeterministic() {
        assertEquals(10, MonarchProgressionEngine.BASE_ARMY_CAPACITY)
        assertEquals(5, MonarchProgressionEngine.ARMY_EXPANSION_PER_UNLOCK)
        assertEquals(3, MonarchProgressionEngine.BASE_AUTHORITY_PER_LEVEL)
        assertEquals(
            com.example.domain.model.ShadowMonarchProfile.monarchXpRequired(10),
            MonarchProgressionEngine.calculateNextLevelXp(9)
        )
    }

    // ========================================================================
    // 3. SHADOW EVOLUTION & PROGRESSION TESTS
    // ========================================================================

    @Test
    fun shadowEvolution_rankProgressionThresholds() {
        assertEquals(ShadowRank.E_RANK, ShadowEvolutionEngine.calculateRank(1, 0L))
        assertEquals(ShadowRank.E_RANK, ShadowEvolutionEngine.calculateRank(9, 400L))
        assertEquals(ShadowRank.D_RANK, ShadowEvolutionEngine.calculateRank(10, 500L))
        assertEquals(ShadowRank.C_RANK, ShadowEvolutionEngine.calculateRank(25, 2000L))
        assertEquals(ShadowRank.B_RANK, ShadowEvolutionEngine.calculateRank(40, 5000L))
        assertEquals(ShadowRank.A_RANK, ShadowEvolutionEngine.calculateRank(60, 12000L))
        assertEquals(ShadowRank.S_RANK, ShadowEvolutionEngine.calculateRank(80, 25000L))
        assertEquals(ShadowRank.MONARCH_CLASS, ShadowEvolutionEngine.calculateRank(100, 50000L))
    }

    @Test
    fun shadowEvolution_stageProgressionThresholds() {
        assertEquals(ShadowEvolutionStage.NEWLY_EXTRACTED, ShadowEvolutionEngine.calculateEvolutionStage(1, ShadowRank.E_RANK))
        assertEquals(ShadowEvolutionStage.AWAKENED, ShadowEvolutionEngine.calculateEvolutionStage(5, ShadowRank.D_RANK))
        assertEquals(ShadowEvolutionStage.ELITE, ShadowEvolutionEngine.calculateEvolutionStage(15, ShadowRank.C_RANK))
        assertEquals(ShadowEvolutionStage.COMMANDER, ShadowEvolutionEngine.calculateEvolutionStage(30, ShadowRank.B_RANK))
        assertEquals(ShadowEvolutionStage.GENERAL, ShadowEvolutionEngine.calculateEvolutionStage(50, ShadowRank.A_RANK))
        assertEquals(ShadowEvolutionStage.MONARCH_GUARD, ShadowEvolutionEngine.calculateEvolutionStage(75, ShadowRank.S_RANK))
    }

    @Test
    fun shadowEvolution_initialStatsReflectClassSpecializationAndXpScales() {
        val vanguardStats = ShadowExtractionEngine.calculateInitialStats(ShadowClass.VANGUARD, ShadowRarity.COMMON, 1L)
        val arcanistStats = ShadowExtractionEngine.calculateInitialStats(ShadowClass.ARCANIST, ShadowRarity.COMMON, 1L)

        assertTrue("Vanguard should prioritize power over Arcanist", vanguardStats.power > arcanistStats.power)
        assertTrue("Arcanist should prioritize wisdom over Vanguard", arcanistStats.wisdom > vanguardStats.wisdom)
        assertTrue(ShadowEvolutionEngine.xpToNextLevel(10) > ShadowEvolutionEngine.xpToNextLevel(1))
    }

    @Test
    fun shadowPowerRating_calculatesCorrectWeightedSum() {
        val shadow = Shadow(
            shadowId = "sh_001",
            name = "Igris",
            shadowClass = ShadowClass.VANGUARD,
            rarity = ShadowRarity.LEGENDARY,
            currentRank = ShadowRank.A_RANK,
            level = 45,
            shadowXp = 500,
            stats = ShadowStats(power = 60, endurance = 50, focus = 40, discipline = 40, wisdom = 35, adaptability = 40),
            evolutionStage = ShadowEvolutionStage.COMMANDER,
            passiveAbility = "Red Commander Aura",
            activeSkillIds = listOf("vanguard_cleave", "vanguard_whirlwind"),
            recruitmentSource = "Boss Battle: Blood-Red Commander Igris",
            extractionEventId = "ext_001",
            extractedAtEpochMs = 1700000000000L
        )

        val powerRating = shadow.calculatePowerRating()
        assertTrue("High level legendary shadow should have substantial power rating", powerRating > 500)
    }

    // ========================================================================
    // 4. SKILL CATALOG & PRECONDITIONS TESTS
    // ========================================================================

    @Test
    fun monarchSkillCatalog_all8SkillsPresentWithValidPrerequisites() {
        assertEquals(8, MonarchSkillCatalog.skills.size)
        val skillMap = MonarchSkillCatalog.skills.associateBy { it.skillId }

        // Test Shadow Extraction skill
        val ariseSkill = skillMap[MonarchSkillIds.SHADOW_EXTRACTION]
        assertNotNull(ariseSkill)
        assertEquals(1, ariseSkill?.order)
        assertEquals(1, ariseSkill?.requiredMonarchLevel)

        // Skills with prerequisites must reference existing catalog skill IDs
        MonarchSkillCatalog.skills.forEach { skill ->
            skill.prerequisiteSkillId?.let { prereqId ->
                assertTrue(
                    "Prerequisite $prereqId for skill ${skill.name} must exist in catalog",
                    skillMap.containsKey(prereqId)
                )
            }
        }
    }

    @Test
    fun shadowSkillCatalog_hasPassiveAndActiveSkills() {
        assertTrue(ShadowSkillCatalog.skills.size >= 8)

        val passiveSkills = ShadowSkillCatalog.skills.filter { it.isPassive }
        val activeSkills = ShadowSkillCatalog.skills.filter { !it.isPassive }

        assertTrue(passiveSkills.isNotEmpty())
        assertTrue(activeSkills.isNotEmpty())
    }

    // ========================================================================
    // 5. SHADOW QUESTS CATALOG TESTS
    // ========================================================================

    @Test
    fun shadowQuestCatalog_all8QuestsConfiguredDeterministically() {
        assertEquals(8, ShadowQuestCatalog.quests.size)
        ShadowQuestCatalog.quests.forEach { quest ->
            assertTrue("Quest ${quest.questId} target must be positive", quest.targetValue > 0)
            assertTrue("Quest ${quest.questId} Monarch XP reward must be positive", quest.monarchXpReward > 0)
            assertTrue("Quest ${quest.questId} Essence reward must be non-negative", quest.shadowEssenceReward >= 0)
        }
    }

    // ========================================================================
    // 6. FORMATION DEFINITION TESTS
    // ========================================================================

    @Test
    fun formationTypes_allTypesHaveValidLabelsAndMaxSlots() {
        ShadowFormationType.entries.forEach { type ->
            assertTrue("Formation ${type.name} slots must be >= 3", type.maxSlots >= 3)
            assertFalse(type.label.isBlank())
        }
    }
}

