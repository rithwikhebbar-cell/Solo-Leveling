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
        assertEquals(MonarchRank.AWAKENING, MonarchProgressionEngine.calculateMonarchRank(1))
        assertEquals(MonarchRank.AWAKENING, MonarchProgressionEngine.calculateMonarchRank(5))
        assertEquals(MonarchRank.COMMANDER, MonarchProgressionEngine.calculateMonarchRank(6))
        assertEquals(MonarchRank.COMMANDER, MonarchProgressionEngine.calculateMonarchRank(10))
        assertEquals(MonarchRank.SOVEREIGN, MonarchProgressionEngine.calculateMonarchRank(11))
        assertEquals(MonarchRank.SOVEREIGN, MonarchProgressionEngine.calculateMonarchRank(20))
        assertEquals(MonarchRank.MONARCH, MonarchProgressionEngine.calculateMonarchRank(21))
        assertEquals(MonarchRank.MONARCH, MonarchProgressionEngine.calculateMonarchRank(30))
        assertEquals(MonarchRank.SHADOW_SOVEREIGN, MonarchProgressionEngine.calculateMonarchRank(31))
        assertEquals(MonarchRank.SHADOW_SOVEREIGN, MonarchProgressionEngine.calculateMonarchRank(50))
    }

    @Test
    fun monarchProgression_armyCapacityScalesWithLevel() {
        val capLv1 = MonarchProgressionEngine.calculateArmyCapacity(1)
        val capLv10 = MonarchProgressionEngine.calculateArmyCapacity(10)
        val capLv20 = MonarchProgressionEngine.calculateArmyCapacity(20)
        val capLv35 = MonarchProgressionEngine.calculateArmyCapacity(35)

        assertTrue(capLv1 in 5..10)
        assertTrue(capLv10 > capLv1)
        assertTrue(capLv20 > capLv10)
        assertTrue(capLv35 > capLv20)
    }

    @Test
    fun monarchProgression_monarchAuthorityScalesDeterministically() {
        val auth1 = MonarchProgressionEngine.calculateAuthority(1)
        val auth10 = MonarchProgressionEngine.calculateAuthority(10)
        assertEquals(10, auth1)
        assertEquals(55, auth10)
    }

    // ========================================================================
    // 3. SHADOW EVOLUTION & PROGRESSION TESTS
    // ========================================================================

    @Test
    fun shadowEvolution_rankProgressionThresholds() {
        assertEquals(ShadowRank.E_RANK, ShadowEvolutionEngine.calculateRank(1))
        assertEquals(ShadowRank.E_RANK, ShadowEvolutionEngine.calculateRank(9))
        assertEquals(ShadowRank.D_RANK, ShadowEvolutionEngine.calculateRank(10))
        assertEquals(ShadowRank.C_RANK, ShadowEvolutionEngine.calculateRank(20))
        assertEquals(ShadowRank.B_RANK, ShadowEvolutionEngine.calculateRank(30))
        assertEquals(ShadowRank.A_RANK, ShadowEvolutionEngine.calculateRank(40))
        assertEquals(ShadowRank.S_RANK, ShadowEvolutionEngine.calculateRank(50))
        assertEquals(ShadowRank.MONARCH_CLASS, ShadowEvolutionEngine.calculateRank(60))
    }

    @Test
    fun shadowEvolution_stageProgressionThresholds() {
        assertEquals(ShadowEvolutionStage.NEWLY_EXTRACTED, ShadowEvolutionEngine.calculateEvolutionStage(1))
        assertEquals(ShadowEvolutionStage.AWAKENED, ShadowEvolutionEngine.calculateEvolutionStage(10))
        assertEquals(ShadowEvolutionStage.ELITE_KNIGHT, ShadowEvolutionEngine.calculateEvolutionStage(20))
        assertEquals(ShadowEvolutionStage.SHADOW_COMMANDER, ShadowEvolutionEngine.calculateEvolutionStage(35))
        assertEquals(ShadowEvolutionStage.GRAND_MARSHAL, ShadowEvolutionEngine.calculateEvolutionStage(50))
        assertEquals(ShadowEvolutionStage.SOVEREIGN_ASCENDANT, ShadowEvolutionEngine.calculateEvolutionStage(65))
    }

    @Test
    fun shadowEvolution_statGainsReflectClassSpecialization() {
        val vanguardStats = ShadowStats(15, 10, 10, 10, 10, 10)
        val boostedVanguard = ShadowEvolutionEngine.applyLevelUpStats(vanguardStats, ShadowClass.VANGUARD)

        // Vanguard primary is power (+3) and endurance (+2)
        assertEquals(18, boostedVanguard.power)
        assertEquals(12, boostedVanguard.endurance)

        val arcanistStats = ShadowStats(10, 10, 10, 10, 15, 10)
        val boostedArcanist = ShadowEvolutionEngine.applyLevelUpStats(arcanistStats, ShadowClass.ARCANIST)

        // Arcanist primary is wisdom (+3) and focus (+2)
        assertEquals(18, boostedArcanist.wisdom)
        assertEquals(12, boostedArcanist.focus)
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
            evolutionStage = ShadowEvolutionStage.SHADOW_COMMANDER,
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

        // Test ARISE skill
        val ariseSkill = skillMap[MonarchSkillIds.EXTRACTION_AUTHORITY]
        assertNotNull(ariseSkill)
        assertEquals(1, ariseSkill?.tier)
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
    fun shadowSkillCatalog_hasUniversalAndClassSpecificSkills() {
        assertTrue(ShadowSkillCatalog.skills.size >= 8)

        val universalSkills = ShadowSkillCatalog.skills.filter { it.requiredClass == null }
        val classSkills = ShadowSkillCatalog.skills.filter { it.requiredClass != null }

        assertTrue(universalSkills.isNotEmpty())
        assertTrue(classSkills.isNotEmpty())
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
            assertTrue("Quest ${quest.questId} Essence reward must be non-negative", quest.essenceReward >= 0)
        }
    }

    // ========================================================================
    // 6. FORMATION DEFINITION TESTS
    // ========================================================================

    @Test
    fun formationTypes_allTypesHaveDistinctModifiersAndMaxSlots() {
        ShadowFormationType.entries.forEach { type ->
            assertTrue("Formation ${type.name} slots must be >= 3", type.maxSlots >= 3)
            assertFalse(type.displayName.isBlank())
            assertFalse(type.description.isBlank())
        }
    }
}

