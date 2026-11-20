package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.entity.DungeonDefinitionEntity
import com.example.data.local.entity.DungeonInstanceEntity
import com.example.data.local.entity.ShadowEntity
import com.example.data.local.entity.toDomain
import com.example.data.repository.ShadowArmyRepository
import com.example.domain.engine.MonarchProgressionEngine
import com.example.domain.engine.ShadowDeploymentEngine
import com.example.domain.engine.ShadowEvolutionEngine
import com.example.domain.engine.ShadowExtractionEngine
import com.example.domain.model.Shadow
import com.example.domain.model.ShadowClass
import com.example.domain.model.ShadowDeploymentStatus
import com.example.domain.model.ShadowEvolutionStage
import com.example.domain.model.ShadowEvolutionState
import com.example.domain.model.ShadowExtractionOutcome
import com.example.domain.model.ShadowExtractionSourceType
import com.example.domain.model.ShadowGrowthConfig
import com.example.domain.model.ShadowRank
import com.example.domain.model.ShadowRarity
import com.example.domain.model.ShadowResourceType
import com.example.domain.model.ShadowStats
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ShadowPhase63GrowthRobolectricTest {

    private lateinit var database: SoloLevelingDatabase
    private lateinit var repository: ShadowArmyRepository
    private lateinit var monarchProgressionEngine: MonarchProgressionEngine
    private lateinit var extractionEngine: ShadowExtractionEngine
    private lateinit var evolutionEngine: ShadowEvolutionEngine
    private lateinit var deploymentEngine: ShadowDeploymentEngine

    private val accountA = "player_account_alpha"
    private val accountB = "player_account_beta"

    @Before
    fun setup() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SoloLevelingDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val sharedMutex = Mutex()
        monarchProgressionEngine = MonarchProgressionEngine(
            monarchProfileDao = database.shadowMonarchProfileDao(),
            monarchSkillUnlockDao = database.monarchSkillUnlockDao(),
            shadowDao = database.shadowDao(),
            shadowResourceDao = database.shadowResourceDao(),
            shadowQuestProgressDao = database.shadowQuestProgressDao(),
            shadowXpTraceDao = database.shadowXpTraceDao(),
            database = database,
            mutex = sharedMutex
        )
        extractionEngine = ShadowExtractionEngine(
            database = database,
            shadowDao = database.shadowDao(),
            extractionDao = database.shadowExtractionDao(),
            monarchProfileDao = database.shadowMonarchProfileDao(),
            monarchProgressionEngine = monarchProgressionEngine,
            mutex = sharedMutex
        )
        evolutionEngine = ShadowEvolutionEngine(
            shadowDao = database.shadowDao(),
            shadowSkillUnlockDao = database.shadowSkillUnlockDao(),
            monarchProgressionEngine = monarchProgressionEngine,
            shadowXpTraceDao = database.shadowXpTraceDao(),
            database = database,
            mutex = sharedMutex
        )
        deploymentEngine = ShadowDeploymentEngine(
            shadowDao = database.shadowDao(),
            deploymentDao = database.shadowDeploymentDao(),
            formationDao = database.shadowFormationDao(),
            monarchProgressionEngine = monarchProgressionEngine,
            database = database,
            mutex = sharedMutex
        )

        repository = ShadowArmyRepository(
            monarchProfileDao = database.shadowMonarchProfileDao(),
            shadowDao = database.shadowDao(),
            extractionDao = database.shadowExtractionDao(),
            skillUnlockDao = database.shadowSkillUnlockDao(),
            deploymentDao = database.shadowDeploymentDao(),
            formationDao = database.shadowFormationDao(),
            monarchSkillUnlockDao = database.monarchSkillUnlockDao(),
            questProgressDao = database.shadowQuestProgressDao(),
            resourceDao = database.shadowResourceDao(),
            monarchProgressionEngine = monarchProgressionEngine,
            extractionEngine = extractionEngine,
            evolutionEngine = evolutionEngine,
            deploymentEngine = deploymentEngine,
            database = database,
            mutex = sharedMutex
        )

        monarchProgressionEngine.getOrCreateProfile(accountA)
        monarchProgressionEngine.getOrCreateProfile(accountB)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun insertTestShadow(
        shadowId: String = "test_shadow_${UUID.randomUUID().toString().take(6)}",
        name: String = "Shadow Sentinel",
        shadowClass: ShadowClass = ShadowClass.VANGUARD,
        rarity: ShadowRarity = ShadowRarity.EPIC,
        rank: ShadowRank = ShadowRank.E_RANK,
        level: Int = 1,
        xp: Long = 0L,
        stage: ShadowEvolutionStage = ShadowEvolutionStage.NEWLY_EXTRACTED,
        stats: ShadowStats = ShadowStats(power = 15, endurance = 12, focus = 10, discipline = 10, wisdom = 8, adaptability = 10),
        owner: String = accountA
    ): Shadow {
        val entity = ShadowEntity(
            shadowId = shadowId,
            accountOwnerId = owner,
            name = name,
            shadowClassName = shadowClass.name,
            rarityName = rarity.name,
            currentRankName = rank.name,
            level = level,
            shadowXp = xp,
            statPower = stats.power,
            statEndurance = stats.endurance,
            statFocus = stats.focus,
            statDiscipline = stats.discipline,
            statWisdom = stats.wisdom,
            statAdaptability = stats.adaptability,
            evolutionStageName = stage.name,
            passiveAbility = shadowClass.passiveAbilityName,
            activeSkillIdsJson = "[]",
            recruitmentSource = "Verified Challenge Test",
            extractionEventId = "ext_test_001",
            extractedAtEpochMs = System.currentTimeMillis(),
            evolutionHistoryJson = "[]",
            deploymentStatusName = ShadowDeploymentStatus.IDLE.name,
            equippedFormationId = null,
            isRetired = false,
            notes = "Robolectric test shadow"
        )
        database.shadowDao().insertShadow(entity)
        return entity.toDomain()
    }

    // ========================================================================
    // 1. STAGE LEVEL CAPS ENFORCEMENT
    // ========================================================================

    @Test
    fun testStageLevelCapsEnforced_levelClampedAtStageCapUntilEvolved() = runTest {
        val shadow = insertTestShadow(owner = accountA)
        val initialCap = ShadowGrowthConfig.getStageLevelCap(ShadowEvolutionStage.NEWLY_EXTRACTED)
        assertEquals(10, initialCap)

        // Award massive XP (20,000 XP) which would normally reach level ~20
        val awarded = repository.awardShadowXpFromEvent(
            shadowId = shadow.shadowId,
            sourceType = ShadowExtractionSourceType.BOSS_BATTLE_COMPLETION,
            eventId = "massive_xp_event_1",
            accountOwnerId = accountA
        )
        assertTrue(awarded)

        // Award multiple batches to push XP far beyond cap
        for (i in 2..50) {
            repository.awardShadowXpFromEvent(
                shadowId = shadow.shadowId,
                sourceType = ShadowExtractionSourceType.DUNGEON_COMPLETION,
                eventId = "dungeon_farm_$i",
                accountOwnerId = accountA
            )
        }

        val updated = repository.getShadowById(shadow.shadowId, accountA)
        assertNotNull(updated)
        // Level MUST be strictly capped at 10 for NEWLY_EXTRACTED stage
        assertEquals(10, updated!!.level)
        assertEquals(ShadowEvolutionStage.NEWLY_EXTRACTED, updated.evolutionStage)
    }

    // ========================================================================
    // 2. SHADOW XP THRESHOLD CALCULATIONS
    // ========================================================================

    @Test
    fun testShadowXpThresholdCalculations_monotonicAndDeterministic() {
        var prevXp = -1L
        for (lvl in 1..100) {
            val req = Shadow.shadowXpRequired(lvl)
            assertTrue("XP required for level $lvl must be non-negative", req >= 0L)
            assertTrue("XP requirements must strictly increase with level", req > prevXp || lvl == 1)
            prevXp = req

            val computedLvl = Shadow.shadowLevelForXp(req)
            assertEquals("Level calculation must match required XP threshold", lvl, computedLvl)
        }
    }

    // ========================================================================
    // 3. DUPLICATE XP EVENT REJECTION
    // ========================================================================

    @Test
    fun testDuplicateXpEventRejection_identicalSourceTagRejected() = runTest {
        val shadow = insertTestShadow(owner = accountA)

        // First award succeeds
        val firstAward = repository.awardShadowXpFromDungeon(shadow.shadowId, "dungeon_frost_01", accountA)
        assertTrue(firstAward)

        val shadowAfterFirst = repository.getShadowById(shadow.shadowId, accountA)
        val xpAfterFirst = shadowAfterFirst!!.shadowXp
        assertTrue(xpAfterFirst > 0L)

        // Duplicate replay with identical dungeonId MUST be rejected
        val duplicateAward = repository.awardShadowXpFromDungeon(shadow.shadowId, "dungeon_frost_01", accountA)
        assertFalse(duplicateAward)

        val shadowAfterDuplicate = repository.getShadowById(shadow.shadowId, accountA)
        assertEquals("XP must not increase on duplicate replay", xpAfterFirst, shadowAfterDuplicate!!.shadowXp)
    }

    // ========================================================================
    // 4. DETERMINISTIC ATTRIBUTE GROWTH
    // ========================================================================

    @Test
    fun testDeterministicAttributeGrowth_classTailoredGrowthAppliedOnLevelUp() = runTest {
        val initialStats = ShadowStats(power = 10, endurance = 10, focus = 10, discipline = 10, wisdom = 10, adaptability = 10)
        val shadow = insertTestShadow(
            shadowClass = ShadowClass.VANGUARD,
            stats = initialStats,
            level = 1,
            xp = 0L,
            owner = accountA
        )

        // Award 250 XP (Boss Defeat), crossing Level 2 threshold (175 XP) and reaching Level 2 (< 450 XP)
        val awarded = repository.awardShadowXpFromBoss(shadow.shadowId, "boss_level_up", accountA)
        assertTrue(awarded)

        val updated = repository.getShadowById(shadow.shadowId, accountA)
        assertNotNull(updated)
        assertEquals(2, updated!!.level)

        // Vanguard gains per level: +2 Power, +2 Endurance, +1 Focus, +1 Discipline, +0 Wisdom, +1 Adaptability
        val growth = ShadowGrowthConfig.getStatGrowthPerLevel(ShadowClass.VANGUARD)
        assertEquals(initialStats.power + growth.power, updated.stats.power)
        assertEquals(initialStats.endurance + growth.endurance, updated.stats.endurance)
        assertEquals(initialStats.focus + growth.focus, updated.stats.focus)
        assertEquals(initialStats.discipline + growth.discipline, updated.stats.discipline)
        assertEquals(initialStats.wisdom + growth.wisdom, updated.stats.wisdom)
        assertEquals(initialStats.adaptability + growth.adaptability, updated.stats.adaptability)
    }

    // ========================================================================
    // 5. EVOLUTION ELIGIBILITY EVALUATION
    // ========================================================================

    @Test
    fun testEvolutionEligibility_reportsCorrectStatesAndBlockers() = runTest {
        // Shadow at Level 1 -> INSUFFICIENT_LEVEL
        val shadow = insertTestShadow(level = 1, rank = ShadowRank.E_RANK, owner = accountA)
        val eval1 = repository.evaluateEvolution(shadow.shadowId, accountA)
        assertEquals(ShadowEvolutionState.INSUFFICIENT_LEVEL, eval1.state)
        assertFalse(eval1.isEligible)
        assertTrue(eval1.blockers.any { it.contains("Reach Level 5") })

        // Shadow at Level 10, but insufficient rank
        val shadowLvl10 = insertTestShadow(level = 10, rank = ShadowRank.E_RANK, owner = accountA)
        val eval2 = repository.evaluateEvolution(shadowLvl10.shadowId, accountA)
        assertEquals(ShadowEvolutionState.INSUFFICIENT_RANK, eval2.state)
        assertFalse(eval2.isEligible)

        // Shadow at Level 10, Rank D, but insufficient resources
        val shadowReadyReqs = insertTestShadow(level = 10, rank = ShadowRank.D_RANK, owner = accountA)
        val eval3 = repository.evaluateEvolution(shadowReadyReqs.shadowId, accountA)
        assertEquals(ShadowEvolutionState.INSUFFICIENT_RESOURCES, eval3.state)
        assertFalse(eval3.isEligible)
        assertTrue(eval3.blockers.any { it.contains("Insufficient Shadow Essence") })

        // Fund resources: 50 Essence, 1 Core
        repository.awardResource(ShadowResourceType.SHADOW_ESSENCE, 50, "award_ess_1", "Funding essence", accountA)
        repository.awardResource(ShadowResourceType.EVOLUTION_CORES, 1, "award_cor_1", "Funding cores", accountA)

        val eval4 = repository.evaluateEvolution(shadowReadyReqs.shadowId, accountA)
        assertEquals(ShadowEvolutionState.READY_TO_EVOLVE, eval4.state)
        assertTrue(eval4.isEligible)
        assertTrue(eval4.blockers.isEmpty())
        assertNotNull(eval4.statPreview)
    }

    // ========================================================================
    // 6. INSUFFICIENT RESOURCE REJECTION
    // ========================================================================

    @Test
    fun testInsufficientResourceRejection_failsWithoutModifyingShadow() = runTest {
        val shadow = insertTestShadow(level = 10, rank = ShadowRank.D_RANK, owner = accountA)

        // Attempt evolution without resources
        val result = repository.confirmEvolution(shadow.shadowId, accountA)
        assertTrue(result.isFailure)

        val unchanged = repository.getShadowById(shadow.shadowId, accountA)
        assertEquals(ShadowEvolutionStage.NEWLY_EXTRACTED, unchanged!!.evolutionStage)
    }

    // ========================================================================
    // 7. SUCCESSFUL EVOLUTION & STAGE ADVANCEMENT
    // ========================================================================

    @Test
    fun testSuccessfulEvolution_advancesStageAppliesStatBonusAndIncreasesCap() = runTest {
        val initialStats = ShadowStats(power = 20, endurance = 18, focus = 15, discipline = 14, wisdom = 10, adaptability = 12)
        val shadow = insertTestShadow(
            level = 10,
            rank = ShadowRank.D_RANK,
            stats = initialStats,
            owner = accountA
        )

        // Fund required resources for Awakened stage (50 Essence, 1 Core)
        repository.awardResource(ShadowResourceType.SHADOW_ESSENCE, 50, "ev_fund_ess", "Essence fund", accountA)
        repository.awardResource(ShadowResourceType.EVOLUTION_CORES, 1, "ev_fund_cor", "Core fund", accountA)

        val result = repository.confirmEvolution(shadow.shadowId, accountA)
        assertTrue("Evolution must succeed when all requirements are met", result.isSuccess)

        val evolved = result.getOrNull()
        assertNotNull(evolved)
        assertEquals(ShadowEvolutionStage.AWAKENED, evolved!!.evolutionStage)

        // Verify stage bonus applied (+5 to all attributes for AWAKENED)
        val stageBonus = ShadowGrowthConfig.getStageStatBonusIncrement(ShadowEvolutionStage.AWAKENED)
        assertEquals(initialStats.power + stageBonus.power, evolved.stats.power)
        assertEquals(initialStats.endurance + stageBonus.endurance, evolved.stats.endurance)

        // Verify history JSON contains entry
        assertTrue(evolved.evolutionHistoryJson.contains("AWAKENED"))

        // Verify stage cap is now 25
        val newCap = ShadowGrowthConfig.getStageLevelCap(evolved.evolutionStage)
        assertEquals(25, newCap)
    }

    // ========================================================================
    // 8. ATOMIC RESOURCE DEDUCTION
    // ========================================================================

    @Test
    fun testAtomicResourceDeduction_exactCostsDeductedViaTransactions() = runTest {
        val shadow = insertTestShadow(level = 10, rank = ShadowRank.D_RANK, owner = accountA)

        // Deposit initial balances
        repository.awardResource(ShadowResourceType.SHADOW_ESSENCE, 120, "dep_ess", "Deposit", accountA)
        repository.awardResource(ShadowResourceType.EVOLUTION_CORES, 4, "dep_cor", "Deposit", accountA)

        val initialEssence = repository.getCurrentBalance(ShadowResourceType.SHADOW_ESSENCE, accountA)
        val initialCores = repository.getCurrentBalance(ShadowResourceType.EVOLUTION_CORES, accountA)
        assertEquals(120, initialEssence)
        assertEquals(4, initialCores)

        // Execute evolution (costs 50 Essence, 1 Core)
        val result = repository.confirmEvolution(shadow.shadowId, accountA)
        assertTrue(result.isSuccess)

        val essenceAfter = repository.getCurrentBalance(ShadowResourceType.SHADOW_ESSENCE, accountA)
        val coresAfter = repository.getCurrentBalance(ShadowResourceType.EVOLUTION_CORES, accountA)
        assertEquals(70, essenceAfter)
        assertEquals(3, coresAfter)
    }

    // ========================================================================
    // 9. FAILED TRANSACTION ROLLBACK
    // ========================================================================

    @Test
    fun testFailedTransactionRollback_noPartialDeductionsOnFailure() = runTest {
        val shadow = insertTestShadow(level = 10, rank = ShadowRank.D_RANK, owner = accountA)

        // Give Essence but NO cores (partial funds)
        repository.awardResource(ShadowResourceType.SHADOW_ESSENCE, 100, "partial_ess", "Essence deposit", accountA)
        val essenceBefore = repository.getCurrentBalance(ShadowResourceType.SHADOW_ESSENCE, accountA)
        assertEquals(100, essenceBefore)

        val result = repository.confirmEvolution(shadow.shadowId, accountA)
        assertTrue("Evolution must fail when one resource is missing", result.isFailure)

        // Verify Essence was NOT partially deducted
        val essenceAfter = repository.getCurrentBalance(ShadowResourceType.SHADOW_ESSENCE, accountA)
        assertEquals(100, essenceAfter)
    }

    // ========================================================================
    // 10. MAX STAGE REACHED REJECTION
    // ========================================================================

    @Test
    fun testMaxStageReachedRejection_monarchGuardCannotEvolveFurther() = runTest {
        val maxShadow = insertTestShadow(
            level = 100,
            rank = ShadowRank.MONARCH_CLASS,
            stage = ShadowEvolutionStage.MONARCH_GUARD,
            owner = accountA
        )

        val eval = repository.evaluateEvolution(maxShadow.shadowId, accountA)
        assertEquals(ShadowEvolutionState.MAX_STAGE_REACHED, eval.state)
        assertFalse(eval.isEligible)
        assertNull(eval.targetStage)

        val result = repository.confirmEvolution(maxShadow.shadowId, accountA)
        assertTrue(result.isFailure)
    }

    // ========================================================================
    // 11. CONCURRENT EVOLUTION PROTECTION
    // ========================================================================

    @Test
    fun testConcurrentEvolutionProtection_mutexGuardsAgainstDoubleSpend() = runTest {
        val shadow = insertTestShadow(level = 10, rank = ShadowRank.D_RANK, owner = accountA)

        // Fund exactly enough for ONE evolution
        repository.awardResource(ShadowResourceType.SHADOW_ESSENCE, 50, "exact_ess", "Single evolution essence", accountA)
        repository.awardResource(ShadowResourceType.EVOLUTION_CORES, 1, "exact_cor", "Single evolution core", accountA)

        // Launch two concurrent evolution attempts
        val deferred1 = async { repository.confirmEvolution(shadow.shadowId, accountA) }
        val deferred2 = async { repository.confirmEvolution(shadow.shadowId, accountA) }

        val results = awaitAll(deferred1, deferred2)
        val successCount = results.count { it.isSuccess }
        val failureCount = results.count { it.isFailure }

        // Exactly one MUST succeed, and one MUST fail due to insufficient remaining funds
        assertEquals(1, successCount)
        assertEquals(1, failureCount)

        val remainingEssence = repository.getCurrentBalance(ShadowResourceType.SHADOW_ESSENCE, accountA)
        val remainingCores = repository.getCurrentBalance(ShadowResourceType.EVOLUTION_CORES, accountA)
        assertEquals(0, remainingEssence)
        assertEquals(0, remainingCores)
    }

    // ========================================================================
    // 12. MONARCH XP AWARDED ONCE PER EVOLUTION
    // ========================================================================

    @Test
    fun testMonarchXpIdempotency_awards200MonarchXpPerEvolutionStage() = runTest {
        val shadow = insertTestShadow(level = 10, rank = ShadowRank.D_RANK, owner = accountA)
        val profileBefore = repository.getMonarchProfile(accountA)
        val xpBefore = profileBefore.monarchXp

        repository.awardResource(ShadowResourceType.SHADOW_ESSENCE, 50, "monarch_ess", "Essence", accountA)
        repository.awardResource(ShadowResourceType.EVOLUTION_CORES, 1, "monarch_cor", "Core", accountA)

        val result = repository.confirmEvolution(shadow.shadowId, accountA)
        assertTrue(result.isSuccess)

        val profileAfter = repository.getMonarchProfile(accountA)
        assertEquals("Monarch profile must receive exactly 200 Monarch XP", xpBefore + 200, profileAfter.monarchXp)
    }

    // ========================================================================
    // 13. ACCOUNT ISOLATION
    // ========================================================================

    @Test
    fun testAccountIsolation_crossAccountEvolutionAndXpRejected() = runTest {
        val shadowA = insertTestShadow(level = 10, rank = ShadowRank.D_RANK, owner = accountA)

        // Account B attempts to award XP to Account A's shadow -> rejected
        val xpAwarded = repository.awardShadowXpFromDungeon(shadowA.shadowId, "hack_dungeon", accountB)
        assertFalse(xpAwarded)

        // Account B attempts to evolve Account A's shadow -> rejected
        val result = repository.confirmEvolution(shadowA.shadowId, accountB)
        assertTrue(result.isFailure)

        // Account B cannot see Account A's shadow in growth overview
        val overviewB = repository.getShadowGrowthOverview(accountB)
        assertEquals(0, overviewB.totalShadows)

        val overviewA = repository.getShadowGrowthOverview(accountA)
        assertEquals(1, overviewA.totalShadows)
    }

    // ========================================================================
    // 14. PHASE 6.2 RECRUITMENT & RESONANCE REGRESSION
    // ========================================================================

    @Test
    fun testPhase62RecruitmentRegression_evidenceExtractionAndResonanceUnaffected() = runTest {
        val now = System.currentTimeMillis()
        database.dungeonDao().insertDefinition(
            DungeonDefinitionEntity(
                id = 901L,
                code = "DNG_REG_901",
                title = "Frost Regression Gate",
                description = "Regression test gate",
                categoryName = "PHYSICAL",
                difficultyName = "C_RANK",
                requiredRankName = "E_RANK",
                requiredLevel = 1,
                requiredAttributeCode = null,
                requiredAttributeValue = 0,
                timeLimitHours = 24,
                baseXpReward = 100,
                primaryAttrName = "STR",
                primaryAttrXp = 10,
                secondaryAttrName = "VIT",
                secondaryAttrXp = 5,
                rewardTitleId = null,
                rewardItemId = null,
                rewardShadowId = null,
                isRepeatable = true,
                creationSourceName = "TEST",
                isActiveCatalog = true
            )
        )

        val firstInstanceId = 9001L
        val firstSeed = ShadowExtractionEngine.buildDeterminismSeed(
            ShadowExtractionSourceType.DUNGEON_COMPLETION,
            firstInstanceId.toString()
        )
        val firstName = ShadowExtractionEngine.selectName(
            ShadowExtractionEngine.selectClass(firstSeed),
            firstSeed
        )

        var duplicateInstanceId = firstInstanceId + 1L
        while (duplicateInstanceId < firstInstanceId + 500L) {
            val candidateSeed = ShadowExtractionEngine.buildDeterminismSeed(
                ShadowExtractionSourceType.DUNGEON_COMPLETION,
                duplicateInstanceId.toString()
            )
            val candidateName = ShadowExtractionEngine.selectName(
                ShadowExtractionEngine.selectClass(candidateSeed),
                candidateSeed
            )
            if (candidateName.equals(firstName, ignoreCase = true)) break
            duplicateInstanceId++
        }

        database.dungeonDao().insertInstance(
            DungeonInstanceEntity(
                id = firstInstanceId,
                dungeonId = 901L,
                statusName = "COMPLETED",
                creationSourceName = "TEST",
                cadenceKey = "DAILY",
                startedAtEpochMs = now - 3600_000,
                pausedAtEpochMs = null,
                accumulatedPausedMs = 0L,
                deadlineEpochMs = now + 3600_000,
                completedAtEpochMs = now,
                notes = "Clear Frost Dungeon #1"
            )
        )
        database.dungeonDao().insertInstance(
            DungeonInstanceEntity(
                id = duplicateInstanceId,
                dungeonId = 901L,
                statusName = "COMPLETED",
                creationSourceName = "TEST",
                cadenceKey = "DAILY",
                startedAtEpochMs = now - 3600_000,
                pausedAtEpochMs = null,
                accumulatedPausedMs = 0L,
                deadlineEpochMs = now + 3600_000,
                completedAtEpochMs = now,
                notes = "Clear Frost Dungeon #2"
            )
        )

        // Extraction evaluation
        val pending = extractionEngine.evaluateExtractionEligibility(
            sourceType = ShadowExtractionSourceType.DUNGEON_COMPLETION,
            sourceEventId = firstInstanceId.toString(),
            evidenceSummary = "Clear Frost Dungeon",
            accountOwnerId = accountA
        )
        assertNotNull(pending)
        assertEquals(ShadowExtractionOutcome.PENDING_CONFIRMATION, pending.outcome)

        // Confirm extraction -> SUCCESS
        val confirmResult = extractionEngine.confirmExtraction(pending, accountA)
        assertTrue(confirmResult.isSuccess)
        val confirmedEvent = confirmResult.getOrNull()
        assertNotNull(confirmedEvent)
        assertEquals(ShadowExtractionOutcome.SUCCESS, confirmedEvent!!.outcome)

        // Second extraction with identical archetype -> DUPLICATE_RESOLVED
        val duplicatePending = extractionEngine.evaluateExtractionEligibility(
            sourceType = ShadowExtractionSourceType.DUNGEON_COMPLETION,
            sourceEventId = duplicateInstanceId.toString(),
            evidenceSummary = "Clear Frost Dungeon",
            accountOwnerId = accountA
        )
        assertEquals(ShadowExtractionOutcome.PENDING_CONFIRMATION, duplicatePending.outcome)
        val duplicateConfirm = extractionEngine.confirmExtraction(duplicatePending, accountA)
        assertTrue(duplicateConfirm.isSuccess)
        val duplicateEvent = duplicateConfirm.getOrNull()
        assertNotNull(duplicateEvent)
        assertEquals(ShadowExtractionOutcome.DUPLICATE_RESOLVED, duplicateEvent!!.outcome)

        // Verify resonance records tracked in repository
        val resonanceRecords = repository.getResonanceRecordsOnce(accountA)
        assertTrue("Resonance record must be logged for duplicate extraction", resonanceRecords.isNotEmpty())
    }
}
