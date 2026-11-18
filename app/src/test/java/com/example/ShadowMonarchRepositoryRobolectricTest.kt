package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.entity.toDomain
import com.example.data.local.entity.toEntity
import com.example.data.repository.ShadowArmyRepository
import com.example.domain.engine.MonarchProgressionEngine
import com.example.domain.engine.ShadowDeploymentEngine
import com.example.domain.engine.ShadowEvolutionEngine
import com.example.domain.engine.ShadowExtractionEngine
import com.example.domain.model.MonarchRank
import com.example.domain.model.MonarchSkillCatalog
import com.example.domain.model.MonarchSkillIds
import com.example.domain.model.ShadowClass
import com.example.domain.model.ShadowDeploymentCategory
import com.example.domain.model.ShadowDeploymentStatus
import com.example.domain.model.ShadowExtractionOutcome
import com.example.domain.model.ShadowExtractionSourceType
import com.example.domain.model.ShadowFormationType
import com.example.domain.model.ShadowRank
import com.example.domain.model.ShadowResourceType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ShadowMonarchRepositoryRobolectricTest {

    private lateinit var database: SoloLevelingDatabase
    private lateinit var repository: ShadowArmyRepository
    private lateinit var monarchProgressionEngine: MonarchProgressionEngine
    private lateinit var extractionEngine: ShadowExtractionEngine
    private lateinit var evolutionEngine: ShadowEvolutionEngine
    private lateinit var deploymentEngine: ShadowDeploymentEngine

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

        database.dungeonDao().insertDefinition(
            com.example.data.local.entity.DungeonDefinitionEntity(
                id = 1L,
                code = "TEST_DUNGEON_01",
                title = "Frost Dungeon",
                description = "Test dungeon definition",
                categoryName = "PHYSICAL",
                difficultyName = "C_RANK",
                requiredRankName = "E_RANK",
                requiredLevel = 1,
                requiredAttributeCode = null,
                requiredAttributeValue = 0,
                timeLimitHours = 24,
                baseXpReward = 200,
                primaryAttrName = "STR",
                primaryAttrXp = 20,
                secondaryAttrName = "VIT",
                secondaryAttrXp = 10,
                rewardTitleId = null,
                rewardItemId = null,
                rewardShadowId = null,
                isRepeatable = true,
                creationSourceName = "SYSTEM",
                isActiveCatalog = true
            )
        )
        database.bossBattleDao().insertBossDefinition(
            com.example.data.local.entity.BossDefinitionEntity(
                id = 1L,
                code = "TEST_BOSS_01",
                title = "Commander Igris",
                subtitle = "Blood-Red Knight",
                description = "Test boss definition",
                categoryName = "COMBAT",
                difficultyName = "B_RANK",
                requiredRankName = "E_RANK",
                requiredLevel = 1,
                xpReward = 500,
                primaryAttrName = "STR",
                primaryAttrXp = 40,
                secondaryAttrName = "AGI",
                secondaryAttrXp = 20,
                rewardTitleId = null,
                rewardItemId = null,
                rewardShadowId = null,
                totalEstimatedDays = 7
            )
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun seedClearedDungeon(id: Long = 101L): Long {
        return database.dungeonDao().insertInstance(
            com.example.data.local.entity.DungeonInstanceEntity(
                id = id,
                dungeonId = 1L,
                statusName = "CLEARED",
                creationSourceName = "SYSTEM",
                cadenceKey = "DAILY",
                startedAtEpochMs = 1700000000000L,
                pausedAtEpochMs = null,
                accumulatedPausedMs = 0L,
                deadlineEpochMs = 1700086400000L,
                completedAtEpochMs = 1700003600000L,
                notes = "Cleared dungeon"
            )
        )
    }

    private suspend fun seedDefeatedBoss(id: Long = 201L): Long {
        return database.bossBattleDao().insertBossInstance(
            com.example.data.local.entity.BossBattleInstanceEntity(
                id = id,
                bossId = 1L,
                statusName = "DEFEATED",
                startedAtEpochMs = 1700000000000L,
                completedAtEpochMs = 1700003600000L,
                abandonedAtEpochMs = null
            )
        )
    }

    @Test
    fun initialization_createsDefaultMonarchProfile() = runTest {
        val profile = monarchProgressionEngine.getOrCreateProfile()
        assertNotNull(profile)
        assertEquals(1, profile.monarchLevel)
        assertEquals(0L, profile.monarchXp)
        assertEquals(MonarchRank.AWAKENING, profile.monarchRank)
        assertEquals(0, profile.monarchAuthority)
        assertEquals(10, profile.armyCapacity)
        assertEquals(0, profile.activeShadowCount)
        assertEquals(0, profile.totalShadowsRecruited)
    }

    @Test
    fun monarchProgression_awardsMonarchXpIdempotently() = runTest {
        monarchProgressionEngine.getOrCreateProfile()

        val firstAward = monarchProgressionEngine.awardMonarchXp(
            sourceTag = "quest_first_blood_01",
            xpAmount = 150,
            sourceDescription = "First extraction quest"
        )
        assertTrue(firstAward)

        // Awarding again with the exact same sourceTag should be ignored by idempotency guard
        val secondAward = monarchProgressionEngine.awardMonarchXp(
            sourceTag = "quest_first_blood_01",
            xpAmount = 150,
            sourceDescription = "First extraction quest"
        )
        assertFalse(secondAward)

        val updatedProfile = monarchProgressionEngine.getOrCreateProfile()
        assertEquals(150L, updatedProfile.monarchXp)
    }

    @Test
    fun extractionFlow_evaluatesPendingConfirmationBeforePersisting() = runTest {
        monarchProgressionEngine.getOrCreateProfile()
        seedClearedDungeon(101L)

        val pending = extractionEngine.evaluateExtractionEligibility(
            sourceType = ShadowExtractionSourceType.DUNGEON_COMPLETION,
            sourceEventId = "101",
            evidenceSummary = "Cleared Frost Dungeon"
        )

        assertEquals(ShadowExtractionOutcome.PENDING_CONFIRMATION, pending.outcome)
        assertNotNull(pending.shadowName)
        assertNotNull(pending.shadowClass)
        assertNotNull(pending.shadowRarity)
        assertFalse(pending.isConfirmed)

        // Verify shadow has NOT been saved to DB yet
        val currentShadows = database.shadowDao().getAllShadowsOnce()
        assertTrue("Shadow should not be saved prior to player confirmation", currentShadows.isEmpty())
    }

    @Test
    fun extractionFlow_confirmPersistsShadowAndEvent() = runTest {
        monarchProgressionEngine.getOrCreateProfile()
        seedClearedDungeon(202L)

        val pending = extractionEngine.evaluateExtractionEligibility(
            sourceType = ShadowExtractionSourceType.DUNGEON_COMPLETION,
            sourceEventId = "202",
            evidenceSummary = "Cleared C-Rank Dungeon"
        )

        val result = extractionEngine.confirmExtraction(pending)
        assertTrue(result.isSuccess)
        val event = result.getOrThrow()
        assertEquals(ShadowExtractionOutcome.SUCCESS, event.outcome)
        assertTrue(event.isConfirmed)

        // Verify shadow is now in the database
        val shadowsInDb = database.shadowDao().getAllShadowsOnce()
        assertEquals(1, shadowsInDb.size)
        val shadow = shadowsInDb[0]
        assertEquals(event.extractedShadowId, shadow.shadowId)
        assertEquals(event.shadowName, shadow.name)
        assertEquals(ShadowDeploymentStatus.IDLE.name, shadow.deploymentStatusName)

        // Verify monarch profile active count updated
        val profile = monarchProgressionEngine.getOrCreateProfile()
        assertEquals(1, profile.activeShadowCount)
        assertEquals(1, profile.totalShadowsRecruited)
    }

    @Test
    fun invalidSourceEvidence_rejectedWithSourceInvalid() = runTest {
        monarchProgressionEngine.getOrCreateProfile()

        val blankSourceAttempt = extractionEngine.evaluateExtractionEligibility(
            sourceType = ShadowExtractionSourceType.DUNGEON_COMPLETION,
            sourceEventId = "",
            evidenceSummary = "Some summary"
        )
        assertEquals(ShadowExtractionOutcome.SOURCE_INVALID, blankSourceAttempt.outcome)

        val blankSummaryAttempt = extractionEngine.evaluateExtractionEligibility(
            sourceType = ShadowExtractionSourceType.DUNGEON_COMPLETION,
            sourceEventId = "101",
            evidenceSummary = ""
        )
        assertEquals(ShadowExtractionOutcome.SOURCE_INVALID, blankSummaryAttempt.outcome)
    }

    @Test
    fun compositeSourceUniqueness_allowsSameIdAcrossDifferentSourceTypes() = runTest {
        monarchProgressionEngine.getOrCreateProfile()
        seedClearedDungeon(1L)
        seedDefeatedBoss(1L)

        // Source ID "1" on Dungeon
        val dungeonPending = extractionEngine.evaluateExtractionEligibility(
            sourceType = ShadowExtractionSourceType.DUNGEON_COMPLETION,
            sourceEventId = "1",
            evidenceSummary = "Dungeon clear"
        )
        val dungeonResult = extractionEngine.confirmExtraction(dungeonPending)
        assertTrue(dungeonResult.isSuccess)

        // Source ID "1" on Boss Defeat (different source type, should be allowed)
        val bossPending = extractionEngine.evaluateExtractionEligibility(
            sourceType = ShadowExtractionSourceType.BOSS_BATTLE_COMPLETION,
            sourceEventId = "1",
            evidenceSummary = "Boss defeat"
        )
        val bossResult = extractionEngine.confirmExtraction(bossPending)
        assertTrue("Different source type with same eventId must be allowed", bossResult.isSuccess)

        val totalShadows = database.shadowDao().getAllShadowsOnce()
        assertEquals(2, totalShadows.size)
    }

    @Test
    fun concurrentExtraction_secondConfirmationRejected() = runTest {
        monarchProgressionEngine.getOrCreateProfile()
        seedClearedDungeon(301L)

        val pending1 = extractionEngine.evaluateExtractionEligibility(
            sourceType = ShadowExtractionSourceType.DUNGEON_COMPLETION,
            sourceEventId = "301",
            evidenceSummary = "Dungeon clear"
        )
        val pending2 = extractionEngine.evaluateExtractionEligibility(
            sourceType = ShadowExtractionSourceType.DUNGEON_COMPLETION,
            sourceEventId = "301",
            evidenceSummary = "Dungeon clear"
        )

        // First confirmation succeeds
        val result1 = extractionEngine.confirmExtraction(pending1)
        assertTrue(result1.isSuccess)
        assertEquals(ShadowExtractionOutcome.SUCCESS, result1.getOrThrow().outcome)

        // Second confirmation with same source event must be rejected
        val result2 = extractionEngine.confirmExtraction(pending2)
        assertTrue(result2.isSuccess)
        assertEquals(ShadowExtractionOutcome.ALREADY_CLAIMED, result2.getOrThrow().outcome)

        val shadows = database.shadowDao().getAllShadowsOnce()
        assertEquals(1, shadows.size)
    }

    @Test
    fun duplicateExtraction_rejectedWithAlreadyClaimed() = runTest {
        monarchProgressionEngine.getOrCreateProfile()
        seedDefeatedBoss(401L)

        val pending = extractionEngine.evaluateExtractionEligibility(
            sourceType = ShadowExtractionSourceType.BOSS_BATTLE_COMPLETION,
            sourceEventId = "401",
            evidenceSummary = "Defeated Commander Igris"
        )
        extractionEngine.confirmExtraction(pending)

        // Attempting to evaluate the same source event a second time
        val duplicateAttempt = extractionEngine.evaluateExtractionEligibility(
            sourceType = ShadowExtractionSourceType.BOSS_BATTLE_COMPLETION,
            sourceEventId = "401",
            evidenceSummary = "Defeated Commander Igris"
        )

        assertEquals(ShadowExtractionOutcome.ALREADY_CLAIMED, duplicateAttempt.outcome)

        // Verify only one shadow exists in database
        val shadows = database.shadowDao().getAllShadowsOnce()
        assertEquals(1, shadows.size)
    }

    @Test
    fun armyCapacityLimit_blocksExtractionWhenFull() = runTest {
        monarchProgressionEngine.getOrCreateProfile()

        // Recruit shadows up to default capacity of 10
        for (i in 1..10) {
            val eventId = (500L + i)
            seedClearedDungeon(eventId)
            val pending = extractionEngine.evaluateExtractionEligibility(
                sourceType = ShadowExtractionSourceType.DUNGEON_COMPLETION,
                sourceEventId = eventId.toString(),
                evidenceSummary = "Dungeon Clear #$i"
            )
            extractionEngine.confirmExtraction(pending)
        }

        val profile = monarchProgressionEngine.getOrCreateProfile()
        assertEquals(10, profile.activeShadowCount)
        assertEquals(10, profile.armyCapacity)

        // Attempt 11th extraction when army is full
        seedClearedDungeon(511L)
        val blockedAttempt = extractionEngine.evaluateExtractionEligibility(
            sourceType = ShadowExtractionSourceType.DUNGEON_COMPLETION,
            sourceEventId = "511",
            evidenceSummary = "Dungeon Clear #11"
        )

        assertEquals(ShadowExtractionOutcome.ARMY_CAPACITY_REACHED, blockedAttempt.outcome)
    }

    @Test
    fun shadowProgression_awardsXpAndLevelsUp() = runTest {
        monarchProgressionEngine.getOrCreateProfile()
        seedClearedDungeon(601L)

        val pending = extractionEngine.evaluateExtractionEligibility(
            sourceType = ShadowExtractionSourceType.DUNGEON_COMPLETION,
            sourceEventId = "601",
            evidenceSummary = "Clear"
        )
        val event = extractionEngine.confirmExtraction(pending).getOrThrow()
        val shadowId = event.extractedShadowId ?: return@runTest

        // Award 500 shadow XP (threshold for Lv 2 is ~150 XP)
        evolutionEngine.awardShadowXp(shadowId, 500, "daily_training_01")

        val updatedShadow = repository.getShadowById(shadowId)
        assertNotNull(updatedShadow)
        assertTrue("Shadow should have leveled up", updatedShadow!!.level > 1)
        assertEquals(500L, updatedShadow.shadowXp)
    }

    @Test
    fun shadowSkillUnlock_persistedAndVerified() = runTest {
        monarchProgressionEngine.getOrCreateProfile()
        seedClearedDungeon(702L)

        val pending = extractionEngine.evaluateExtractionEligibility(
            sourceType = ShadowExtractionSourceType.DUNGEON_COMPLETION,
            sourceEventId = "702",
            evidenceSummary = "Clear"
        )
        val event = extractionEngine.confirmExtraction(pending).getOrThrow()
        val shadowId = event.extractedShadowId ?: return@runTest

        val unlockResult = evolutionEngine.unlockShadowSkill(
            shadowId = shadowId,
            skillId = "shadow_tactical_insight",
            evidenceNote = "Unlocked tactical insight"
        )

        assertTrue(unlockResult)
        val unlocksInDb = database.shadowSkillUnlockDao().getUnlocksForShadowOnce(shadowId)
        assertEquals(1, unlocksInDb.size)
        assertEquals("shadow_tactical_insight", unlocksInDb[0].skillId)
    }

    @Test
    fun monarchSkillUnlock_requiresAuthorityAndPersists() = runTest {
        monarchProgressionEngine.getOrCreateProfile()
        monarchProgressionEngine.awardMonarchXp(
            sourceTag = "test_unlock_xp",
            xpAmount = 3000,
            sourceDescription = "Reach Monarch Lv 5 and Authority 12"
        )
        val currentProfile = monarchProgressionEngine.getOrCreateProfile()

        // Unlocking Army Expansion requires Lv 3, Authority 10, and SHADOW_EXTRACTION
        val result = monarchProgressionEngine.tryUnlockMonarchSkill(
            skillId = MonarchSkillIds.ARMY_EXPANSION,
            profile = currentProfile,
            evidenceNote = "Unlocked Army Expansion"
        )
        assertTrue(result)

        val profile = monarchProgressionEngine.getOrCreateProfile()
        assertTrue(profile.unlockedMonarchSkillIds.contains(MonarchSkillIds.ARMY_EXPANSION))

        // Attempting to unlock higher tier skill without authority fails
        val highTierResult = monarchProgressionEngine.tryUnlockMonarchSkill(
            skillId = MonarchSkillIds.ABSOLUTE_AUTHORITY,
            profile = profile,
            evidenceNote = "Too high"
        )
        assertFalse(highTierResult)
    }

    @Test
    fun shadowDeployment_lifecycleIdleToDeployedToIdle() = runTest {
        monarchProgressionEngine.getOrCreateProfile()
        seedClearedDungeon(703L)

        val pending = extractionEngine.evaluateExtractionEligibility(
            sourceType = ShadowExtractionSourceType.DUNGEON_COMPLETION,
            sourceEventId = "703",
            evidenceSummary = "Clear"
        )
        val event = extractionEngine.confirmExtraction(pending).getOrThrow()
        val shadowId = event.extractedShadowId ?: return@runTest

        // Deploy
        val deployResult = deploymentEngine.deployShadow(
            shadowId = shadowId,
            category = ShadowDeploymentCategory.DISCIPLINE,
            role = "Habit Enforcer"
        )
        assertTrue(deployResult.isSuccess)
        val deployment = deployResult.getOrThrow()

        val deployedShadow = repository.getShadowById(shadowId)
        assertEquals(ShadowDeploymentStatus.DEPLOYED, deployedShadow?.deploymentStatus)

        // Recall
        val recallResult = deploymentEngine.recallShadow(deployment.deploymentId)
        assertTrue(recallResult.isSuccess)

        val recalledShadow = repository.getShadowById(shadowId)
        assertEquals(ShadowDeploymentStatus.IDLE, recalledShadow?.deploymentStatus)
    }

    @Test
    fun formationManager_createAndActivate() = runTest {
        val createResult = deploymentEngine.createFormation(
            name = "Iron Wall Formation",
            type = ShadowFormationType.DEFENSIVE_FORMATION,
            description = "Defensive recovery focus",
            memberShadowIds = emptyList()
        )
        assertTrue(createResult.isSuccess)
        val formation = createResult.getOrThrow()

        // Set as active formation
        deploymentEngine.setActiveFormation(formation.formationId)

        val activeFormation = repository.observeActiveFormation().first()
        assertNotNull(activeFormation)
        assertEquals(formation.formationId, activeFormation?.formationId)
        assertTrue(activeFormation?.isActive == true)
    }

    @Test
    fun resourceLedger_transactionalAccounting() = runTest {
        // Award 100 Shadow Essence
        val awardResult = repository.awardResource(
            resourceType = ShadowResourceType.SHADOW_ESSENCE,
            amount = 100,
            sourceEventId = "tx_award_001",
            sourceDescription = "Dungeon rewards"
        )
        assertTrue(awardResult.isSuccess)
        assertEquals(100, repository.getCurrentBalance(ShadowResourceType.SHADOW_ESSENCE))

        // Consume 40 Shadow Essence
        val spendResult = repository.consumeResource(
            resourceType = ShadowResourceType.SHADOW_ESSENCE,
            amount = 40,
            purpose = "Skill Upgrade"
        )
        assertTrue(spendResult.isSuccess)
        assertEquals(60, repository.getCurrentBalance(ShadowResourceType.SHADOW_ESSENCE))

        // Attempting to consume more than balance fails
        val overspendResult = repository.consumeResource(
            resourceType = ShadowResourceType.SHADOW_ESSENCE,
            amount = 100,
            purpose = "Overspend"
        )
        assertFalse(overspendResult.isSuccess)
        assertEquals(60, repository.getCurrentBalance(ShadowResourceType.SHADOW_ESSENCE))
    }

    @Test
    fun shadowArmyAnalytics_computedFromRealData() = runTest {
        monarchProgressionEngine.getOrCreateProfile()

        for (i in 1..3) {
            val eventId = 1000L + i
            seedClearedDungeon(eventId)
            val pending = extractionEngine.evaluateExtractionEligibility(
                sourceType = ShadowExtractionSourceType.DUNGEON_COMPLETION,
                sourceEventId = eventId.toString(),
                evidenceSummary = "Clear #$i"
            )
            extractionEngine.confirmExtraction(pending)
        }

        val analytics = repository.buildShadowArmyAnalytics()
        assertEquals(3, analytics.totalRecruited)
        assertEquals(3, analytics.activeShadows)
        assertEquals(0, deploymentEngine.getActiveDeployments().size)
        assertTrue(analytics.monarchProfile.shadowArmyPowerRating > 0)
        assertEquals(3, analytics.distributionByClass.values.sum())
        assertEquals(3, analytics.distributionByRank.values.sum())
    }

    @Test
    fun playerBoundary_monarchProgressionNeverMutatesPlayerEntity() = runTest {
        // Initialize player in PlayerDao
        val playerDao = database.playerDao()
        val player = com.example.domain.model.PlayerProfile(
            codename = "SungJinWoo",
            level = 1,
            currentLevelXp = 50,
            xpToNextLevel = 100,
            totalXp = 50L,
            verifiedActivitiesCount = 1,
            streakDays = 1
        )
        playerDao.upsertPlayer(player.toEntity())

        // Perform aggressive Monarch & Shadow operations
        monarchProgressionEngine.getOrCreateProfile()
        monarchProgressionEngine.awardMonarchXp(
            sourceTag = "test_boundary",
            xpAmount = 1000,
            sourceDescription = "Testing boundary"
        )
        seedDefeatedBoss(701L)
        val pending = extractionEngine.evaluateExtractionEligibility(
            sourceType = ShadowExtractionSourceType.BOSS_BATTLE_COMPLETION,
            sourceEventId = "701",
            evidenceSummary = "Boss Defeat"
        )
        extractionEngine.confirmExtraction(pending)

        // Verify PlayerEntity remains completely untouched
        val playerAfter = playerDao.getPlayerOnce()?.toDomain()
        assertNotNull(playerAfter)
        assertEquals(1, playerAfter?.level)
        assertEquals(50, playerAfter?.currentLevelXp)
        assertEquals(50L, playerAfter?.totalXp)
        assertEquals(com.example.domain.model.HunterRank.E_RANK, playerAfter?.rank)
        assertEquals(10, playerAfter?.attributes?.str?.totalValue)
        assertEquals(10, playerAfter?.attributes?.vit?.totalValue)
        assertEquals(10, playerAfter?.attributes?.intel?.totalValue)
    }

    @Test
    fun sourceEvidenceValidation_rejectsUnverifiedOrNonExistentRecord() = runTest {
        monarchProgressionEngine.getOrCreateProfile()

        // Attempt extraction on non-existent dungeon ID
        val unverifiedAttempt = extractionEngine.evaluateExtractionEligibility(
            sourceType = ShadowExtractionSourceType.DUNGEON_COMPLETION,
            sourceEventId = "non_existent_dungeon_9999",
            evidenceSummary = "Fake dungeon claim"
        )
        assertEquals(ShadowExtractionOutcome.SOURCE_INVALID, unverifiedAttempt.outcome)

        // Seed genuine cleared dungeon and verify it is now accepted
        seedClearedDungeon(801L)
        val verifiedAttempt = extractionEngine.evaluateExtractionEligibility(
            sourceType = ShadowExtractionSourceType.DUNGEON_COMPLETION,
            sourceEventId = "801",
            evidenceSummary = "Genuine cleared dungeon"
        )
        assertEquals(ShadowExtractionOutcome.PENDING_CONFIRMATION, verifiedAttempt.outcome)
    }

    @Test
    fun atomicRollback_failureDuringExtractionRecordInsert_discardsStagedShadow() = runTest {
        monarchProgressionEngine.getOrCreateProfile()
        seedClearedDungeon(901L)

        val pending = extractionEngine.evaluateExtractionEligibility(
            sourceType = ShadowExtractionSourceType.DUNGEON_COMPLETION,
            sourceEventId = "901",
            evidenceSummary = "Dungeon clear"
        )

        // Pre-insert an extraction record with conflicting composite key to force SQLite unique constraint conflict
        database.shadowExtractionDao().insertExtraction(
            com.example.data.local.entity.ShadowExtractionEntity(
                extractionId = "conflicting_ext_01",
                sourceTypeName = ShadowExtractionSourceType.DUNGEON_COMPLETION.name,
                sourceEventId = "901",
                outcomeName = ShadowExtractionOutcome.SUCCESS.name,
                extractedShadowId = "sh_conflict_01",
                shadowName = "Conflict Shadow",
                shadowClassName = "VANGUARD",
                shadowRarityName = "RARE",
                determinismSeed = 999L,
                evidenceSummary = "Pre-existing conflict",
                isConfirmed = true,
                monarchXpAwarded = 100,
                timestampEpochMs = System.currentTimeMillis()
            )
        )

        // Attempting to confirm the pending extraction should fail and abort atomically
        val result = extractionEngine.confirmExtraction(pending)
        val shadows = database.shadowDao().getAllShadowsOnce()
        assertTrue(
            "Transaction rollback must ensure no staged shadow is persisted if extraction fails",
            shadows.none { it.name == pending.shadowName }
        )
    }

    @Test
    fun resourceLedger_allowsEarnedAndConsumedWithSameSourceEventId() = runTest {
        val eventId = "quest_milestone_alpha"

        // Award resources
        val awardResult = repository.awardResource(
            resourceType = ShadowResourceType.SHADOW_ESSENCE,
            amount = 100,
            sourceEventId = eventId,
            sourceDescription = "Earned from milestone"
        )
        assertTrue(awardResult.isSuccess)
        assertEquals(100, awardResult.getOrThrow())

        // Consume resources with the exact same sourceEventId (different transactionTypeName)
        val consumeResult = repository.consumeResource(
            resourceType = ShadowResourceType.SHADOW_ESSENCE,
            amount = 40,
            purpose = "Upgrade shadow skill",
            transactionEventId = eventId
        )
        assertTrue(
            "Resource ledger composite index must allow EARNED and CONSUMED with same sourceEventId",
            consumeResult.isSuccess
        )
        assertEquals(60, consumeResult.getOrThrow())
    }
}

