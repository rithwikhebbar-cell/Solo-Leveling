package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SoloLevelingDatabase
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

        monarchProgressionEngine = MonarchProgressionEngine(
            monarchProfileDao = database.shadowMonarchProfileDao(),
            monarchSkillUnlockDao = database.monarchSkillUnlockDao(),
            shadowDao = database.shadowDao(),
            shadowResourceDao = database.shadowResourceDao(),
            shadowQuestProgressDao = database.shadowQuestProgressDao()
        )
        extractionEngine = ShadowExtractionEngine(
            database = database,
            shadowDao = database.shadowDao(),
            extractionDao = database.shadowExtractionDao(),
            monarchProfileDao = database.shadowMonarchProfileDao(),
            monarchProgressionEngine = monarchProgressionEngine
        )
        evolutionEngine = ShadowEvolutionEngine(
            shadowDao = database.shadowDao(),
            shadowSkillUnlockDao = database.shadowSkillUnlockDao(),
            monarchProgressionEngine = monarchProgressionEngine
        )
        deploymentEngine = ShadowDeploymentEngine(
            shadowDao = database.shadowDao(),
            deploymentDao = database.shadowDeploymentDao(),
            formationDao = database.shadowFormationDao()
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
            deploymentEngine = deploymentEngine
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
        assertEquals(10, profile.monarchAuthority)
        assertEquals(5, profile.armyCapacity)
        assertEquals(0, profile.activeShadowCount)
        assertEquals(0, profile.totalShadowsRecruited)
    }

    @Test
    fun monarchProgression_awardsMonarchXpIdempotently() = runTest {
        monarchProgressionEngine.getOrCreateProfile()

        val firstAward = monarchProgressionEngine.awardMonarchXp(
            xpAmount = 150,
            sourceTag = "quest_first_blood_01",
            sourceDescription = "First extraction quest"
        )
        assertTrue(firstAward)

        // Awarding again with the exact same sourceTag should be ignored by idempotency guard
        val secondAward = monarchProgressionEngine.awardMonarchXp(
            xpAmount = 150,
            sourceTag = "quest_first_blood_01",
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

        // Recruit shadows up to default capacity of 5
        for (i in 1..5) {
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
        assertEquals(5, profile.activeShadowCount)
        assertEquals(5, profile.armyCapacity)

        // Attempt 6th extraction when army is full
        seedClearedDungeon(506L)
        val blockedAttempt = extractionEngine.evaluateExtractionEligibility(
            sourceType = ShadowExtractionSourceType.DUNGEON_COMPLETION,
            sourceEventId = "506",
            evidenceSummary = "Dungeon Clear #6"
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

        val pending = extractionEngine.evaluateExtractionEligibility(
            sourceType = ShadowExtractionSourceType.DUNGEON_COMPLETION,
            sourceEventId = "dungeon_skill_test",
            evidenceSummary = "Clear"
        )
        val event = extractionEngine.confirmExtraction(pending).getOrThrow()
        val shadowId = event.extractedShadowId ?: return@runTest

        val unlockResult = evolutionEngine.unlockShadowSkill(
            shadowId = shadowId,
            skillId = "shadow_stealth",
            evidenceNote = "Unlocked shadow stealth"
        )

        assertTrue(unlockResult.isSuccess)
        val unlocksInDb = database.shadowSkillUnlockDao().getSkillUnlocksForShadowOnce(shadowId)
        assertEquals(1, unlocksInDb.size)
        assertEquals("shadow_stealth", unlocksInDb[0].skillId)
    }

    @Test
    fun monarchSkillUnlock_requiresAuthorityAndPersists() = runTest {
        monarchProgressionEngine.getOrCreateProfile()

        // Unlocking ARISE (MonarchSkillIds.EXTRACTION_AUTHORITY) requires Lv 1 and Authority 10
        val result = monarchProgressionEngine.tryUnlockMonarchSkill(MonarchSkillIds.EXTRACTION_AUTHORITY)
        assertTrue(result.isSuccess)

        val profile = monarchProgressionEngine.getOrCreateProfile()
        assertTrue(profile.unlockedMonarchSkillIds.contains(MonarchSkillIds.EXTRACTION_AUTHORITY))

        // Attempting to unlock higher tier skill without authority fails
        val highTierResult = monarchProgressionEngine.tryUnlockMonarchSkill(MonarchSkillIds.MONARCHS_TERRITORY)
        assertFalse(highTierResult.isSuccess)
    }

    @Test
    fun shadowDeployment_lifecycleIdleToDeployedToIdle() = runTest {
        monarchProgressionEngine.getOrCreateProfile()

        val pending = extractionEngine.evaluateExtractionEligibility(
            sourceType = ShadowExtractionSourceType.DUNGEON_COMPLETION,
            sourceEventId = "dungeon_deploy_test",
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
            formationType = ShadowFormationType.DEFENSIVE_RECOVERY,
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
            val pending = extractionEngine.evaluateExtractionEligibility(
                sourceType = ShadowExtractionSourceType.DUNGEON_COMPLETION,
                sourceEventId = "analytics_dungeon_$i",
                evidenceSummary = "Clear #$i"
            )
            extractionEngine.confirmExtraction(pending)
        }

        val analytics = repository.buildShadowArmyAnalytics()
        assertEquals(3, analytics.totalShadows)
        assertEquals(0, analytics.deployedCount)
        assertTrue(analytics.totalArmyPowerRating > 0)
        assertEquals(3, analytics.distributionByClass.values.sum())
        assertEquals(3, analytics.distributionByRank.values.sum())
    }

    @Test
    fun playerBoundary_monarchProgressionNeverMutatesPlayerEntity() = runTest {
        // Initialize player in PlayerDao
        val playerDao = database.playerDao()
        val player = com.example.data.local.entity.PlayerEntity(
            codename = "SungJinWoo",
            level = 1,
            rankName = "E_RANK",
            currentLevelXp = 50,
            xpToNextLevel = 100,
            totalXp = 50,
            attributePointsAvailable = 0,
            verifiedActivitiesCount = 1,
            selfReportedActivitiesCount = 0,
            streakDays = 1,
            statStr = 10,
            statVit = 10,
            statAgi = 10,
            statInt = 10,
            statDis = 10,
            statWis = 10,
            statCha = 10
        )
        playerDao.insertPlayer(player)

        // Perform aggressive Monarch & Shadow operations
        monarchProgressionEngine.getOrCreateProfile()
        monarchProgressionEngine.awardMonarchXp(1000, "test_boundary", "Testing boundary")
        seedDefeatedBoss(701L)
        val pending = extractionEngine.evaluateExtractionEligibility(
            sourceType = ShadowExtractionSourceType.BOSS_BATTLE_COMPLETION,
            sourceEventId = "701",
            evidenceSummary = "Boss Defeat"
        )
        extractionEngine.confirmExtraction(pending)

        // Verify PlayerEntity remains completely untouched
        val playerAfter = playerDao.getPlayerProfileOnce()
        assertNotNull(playerAfter)
        assertEquals(1, playerAfter?.level)
        assertEquals(50, playerAfter?.currentLevelXp)
        assertEquals(50, playerAfter?.totalXp)
        assertEquals("E_RANK", playerAfter?.rankName)
        assertEquals(10, playerAfter?.statStr)
        assertEquals(10, playerAfter?.statVit)
        assertEquals(10, playerAfter?.statInt)
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
                extractedAtEpochMs = System.currentTimeMillis()
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
            transactionType = ShadowResourceTransactionType.EARNED,
            sourceEventId = eventId,
            description = "Earned from milestone"
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

