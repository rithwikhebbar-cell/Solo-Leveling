package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.entity.DungeonDefinitionEntity
import com.example.data.local.entity.DungeonInstanceEntity
import com.example.data.local.entity.ShadowEntity
import com.example.data.local.entity.ShadowMonarchProfileEntity
import com.example.data.local.entity.toEntity
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.ShadowArmyRepository
import com.example.data.sync.SyncQueueManager
import com.example.domain.engine.MonarchProgressionEngine
import com.example.domain.engine.ShadowDeploymentEngine
import com.example.domain.engine.ShadowEvolutionEngine
import com.example.domain.engine.ShadowExtractionEngine
import com.example.domain.model.DEFAULT_SHADOW_ACCOUNT_OWNER_ID
import com.example.domain.model.MonarchSkillIds
import com.example.domain.model.ShadowDeploymentCategory
import com.example.domain.model.ShadowExtractionOutcome
import com.example.domain.model.ShadowExtractionSourceType
import com.example.domain.model.ShadowFormationType
import com.example.domain.model.ShadowResourceType
import com.example.domain.model.ShadowStats
import com.example.domain.model.SyncEntityType
import com.example.ui.viewmodel.ShadowRealmViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
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

/**
 * Milestone 12 - Phase 6.1: Shadow Foundation Integrity, Account Isolation, Concurrency,
 * Persistent Settings & Sync Scoping Verification Suite.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ShadowPhase61FoundationRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: SoloLevelingDatabase
    private lateinit var sharedMutex: Mutex
    private lateinit var monarchEngine: MonarchProgressionEngine
    private lateinit var extractionEngine: ShadowExtractionEngine
    private lateinit var evolutionEngine: ShadowEvolutionEngine
    private lateinit var deploymentEngine: ShadowDeploymentEngine
    private lateinit var repository: ShadowArmyRepository

    private val accountA = "acct_alpha_1111"
    private val accountB = "acct_beta_2222"

    @Before
    fun setup() = runTest {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, SoloLevelingDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        sharedMutex = Mutex()
        monarchEngine = MonarchProgressionEngine(
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
            monarchProgressionEngine = monarchEngine,
            mutex = sharedMutex
        )
        evolutionEngine = ShadowEvolutionEngine(
            shadowDao = database.shadowDao(),
            shadowSkillUnlockDao = database.shadowSkillUnlockDao(),
            monarchProgressionEngine = monarchEngine,
            shadowXpTraceDao = database.shadowXpTraceDao(),
            database = database,
            mutex = sharedMutex
        )
        deploymentEngine = ShadowDeploymentEngine(
            shadowDao = database.shadowDao(),
            deploymentDao = database.shadowDeploymentDao(),
            formationDao = database.shadowFormationDao(),
            monarchProgressionEngine = monarchEngine,
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
            monarchProgressionEngine = monarchEngine,
            extractionEngine = extractionEngine,
            evolutionEngine = evolutionEngine,
            deploymentEngine = deploymentEngine,
            database = database,
            mutex = sharedMutex
        )

        database.dungeonDao().insertDefinition(
            DungeonDefinitionEntity(
                id = 1L,
                code = "DNG_61_TEST",
                title = "Abyssal Gate",
                description = "Test dungeon",
                categoryName = "PHYSICAL",
                difficultyName = "C_RANK",
                requiredRankName = "E_RANK",
                requiredLevel = 1,
                requiredAttributeCode = null,
                requiredAttributeValue = 0,
                timeLimitHours = 24,
                baseXpReward = 150,
                primaryAttrName = "STR",
                primaryAttrXp = 15,
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
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun seedClearedDungeon(id: Long) {
        database.dungeonDao().insertInstance(
            DungeonInstanceEntity(
                id = id,
                dungeonId = 1L,
                statusName = "CLEARED",
                creationSourceName = "SYSTEM",
                cadenceKey = "DAILY",
                startedAtEpochMs = 1_700_000_000_000L,
                pausedAtEpochMs = null,
                accumulatedPausedMs = 0L,
                deadlineEpochMs = 1_700_086_400_000L,
                completedAtEpochMs = 1_700_003_600_000L,
                notes = "Cleared"
            )
        )
    }

    @Test
    fun multiAccountIsolation_enforcesCompleteSeparationAndRejectsCrossAccountMutations() = runTest {
        seedClearedDungeon(1001L)

        // Account A and Account B independently create profiles & extract from source "1001"
        val pendingA = extractionEngine.evaluateExtractionEligibility(
            ShadowExtractionSourceType.DUNGEON_COMPLETION,
            "1001",
            "Cleared Abyssal Gate",
            accountOwnerId = accountA
        )
        assertEquals(ShadowExtractionOutcome.PENDING_CONFIRMATION, pendingA.outcome)
        val extA = extractionEngine.confirmExtraction(pendingA, accountA).getOrThrow()
        assertEquals(ShadowExtractionOutcome.SUCCESS, extA.outcome)
        val shadowAId = extA.extractedShadowId!!

        val pendingB = extractionEngine.evaluateExtractionEligibility(
            ShadowExtractionSourceType.DUNGEON_COMPLETION,
            "1001",
            "Cleared Abyssal Gate",
            accountOwnerId = accountB
        )
        assertEquals(
            "Same sourceEventId must be independently claimable by a separate account",
            ShadowExtractionOutcome.PENDING_CONFIRMATION,
            pendingB.outcome
        )
        val extB = extractionEngine.confirmExtraction(pendingB, accountB).getOrThrow()
        assertEquals(ShadowExtractionOutcome.SUCCESS, extB.outcome)

        // Cross-account extraction confirmation mismatch must fail
        val crossConfirm = extractionEngine.confirmExtraction(pendingA, accountOwnerId = accountB)
        assertTrue(crossConfirm.isFailure)

        // Cross-account reads & mutations targeting Account A's shadow are blocked
        assertEquals(null, repository.getShadowById(shadowAId, accountOwnerId = accountB))
        assertFalse(evolutionEngine.awardShadowXp(shadowAId, 200, "hack_xp", accountOwnerId = accountB))
        assertFalse(evolutionEngine.unlockShadowSkill(shadowAId, "shadow_tactical_insight", "hack", accountOwnerId = accountB))
        assertTrue(deploymentEngine.deployShadow(shadowAId, ShadowDeploymentCategory.DISCIPLINE, "Hack", accountOwnerId = accountB).isFailure)
        assertTrue(deploymentEngine.createFormation("Hijack", ShadowFormationType.VANGUARD_FORMATION, "Desc", listOf(shadowAId), accountOwnerId = accountB).isFailure)

        // Deploy & create formation under Account A, ensure Account B cannot recall or activate them
        val depA = deploymentEngine.deployShadow(shadowAId, ShadowDeploymentCategory.PHYSICAL_DEVELOPMENT, "Frontline", accountOwnerId = accountA).getOrThrow()
        assertTrue(deploymentEngine.recallShadow(depA.deploymentId, accountOwnerId = accountB).isFailure)
        val formA = deploymentEngine.createFormation("Alpha Squad", ShadowFormationType.VANGUARD_FORMATION, "Assault", listOf(shadowAId), accountOwnerId = accountA).getOrThrow()
        assertTrue(deploymentEngine.setActiveFormation(formA.formationId, accountOwnerId = accountB).isFailure)
        assertTrue(deploymentEngine.deleteFormation(formA.formationId, accountOwnerId = accountB).isFailure)

        // Resource ledgers stay independent per account
        repository.awardResource(ShadowResourceType.SHADOW_ESSENCE, 200, "shared_quest_1", "Alpha reward", accountOwnerId = accountA).getOrThrow()
        repository.awardResource(ShadowResourceType.SHADOW_ESSENCE, 50, "shared_quest_1", "Beta reward", accountOwnerId = accountB).getOrThrow()
        assertEquals(200, repository.getCurrentBalance(ShadowResourceType.SHADOW_ESSENCE, accountOwnerId = accountA))
        assertEquals(50, repository.getCurrentBalance(ShadowResourceType.SHADOW_ESSENCE, accountOwnerId = accountB))
        assertTrue(repository.consumeResource(ShadowResourceType.SHADOW_ESSENCE, 100, "Too much for Beta", accountOwnerId = accountB).isFailure)

        // Analytics strictly scoped per account
        repository.syncShadowQuestProgress(dungeonCompletions = 1, accountOwnerId = accountA)
        val analyticsA = repository.buildShadowArmyAnalytics(accountA)
        val analyticsB = repository.buildShadowArmyAnalytics(accountB)
        assertEquals(1, analyticsA.activeShadows)
        assertEquals(1, analyticsB.activeShadows)
        assertEquals(200, analyticsA.resourceBalances[ShadowResourceType.SHADOW_ESSENCE])
        assertEquals(50, analyticsB.resourceBalances[ShadowResourceType.SHADOW_ESSENCE])
    }

    @Test
    fun profileUpsert_doesNotCascadeDeleteOwnedShadowTables() = runTest {
        seedClearedDungeon(2002L)
        val pending = extractionEngine.evaluateExtractionEligibility(
            ShadowExtractionSourceType.DUNGEON_COMPLETION,
            "2002",
            "Cleared Dungeon",
            accountOwnerId = accountA
        )
        val ext = extractionEngine.confirmExtraction(pending, accountA).getOrThrow()
        val shadowId = ext.extractedShadowId!!
        evolutionEngine.awardShadowXp(shadowId, 300, "xp_1", accountOwnerId = accountA)
        repository.awardResource(ShadowResourceType.SHADOW_ESSENCE, 80, "res_1", "Reward", accountOwnerId = accountA).getOrThrow()

        // Updating profile repeatedly must NOT trigger DELETE + INSERT CASCADE wipe!
        val before = monarchEngine.getOrCreateProfile(accountA)
        database.shadowMonarchProfileDao().upsertProfile(
            before.copy(monarchAuthority = 25).toEntity()
        )
        monarchEngine.refreshMonarchProfile(accountA)

        assertEquals(1, database.shadowDao().getAllShadowsForOwnerOnce(accountA).size)
        assertEquals(1, database.shadowExtractionDao().getAllExtractionsForOwnerOnce(accountA).size)
        assertEquals(1, database.shadowResourceDao().getAllTransactionsForOwnerOnce(accountA).size)
        assertTrue(database.shadowXpTraceDao().getTracesForOwnerOnce(accountA).isNotEmpty())
        assertEquals(80, repository.getCurrentBalance(ShadowResourceType.SHADOW_ESSENCE, accountA))
    }

    @Test
    fun concurrencyAndAtomicity_parallelResourceConsumeAndXpAwardsMaintainExactInvariants() = runTest {
        monarchEngine.getOrCreateProfile(accountA)
        seedClearedDungeon(3003L)
        val pending = extractionEngine.evaluateExtractionEligibility(
            ShadowExtractionSourceType.DUNGEON_COMPLETION,
            "3003",
            "Clear",
            accountOwnerId = accountA
        )
        val shadowId = extractionEngine.confirmExtraction(pending, accountA).getOrThrow().extractedShadowId!!

        // 1. Concurrent resource exhaustion: 100 balance, 10 threads each try to spend 25 -> max 4 succeed, balance == 0
        repository.awardResource(ShadowResourceType.SHADOW_ESSENCE, 100, "pool_init", "Pool", accountOwnerId = accountA).getOrThrow()
        val consumeResults = (1..10).map { i ->
            async(Dispatchers.Default) {
                repository.consumeResource(
                    resourceType = ShadowResourceType.SHADOW_ESSENCE,
                    amount = 25,
                    purpose = "Parallel spend #$i",
                    transactionEventId = "par_tx_$i",
                    accountOwnerId = accountA
                )
            }
        }.awaitAll()
        assertEquals(4, consumeResults.count { it.isSuccess })
        assertEquals(6, consumeResults.count { it.isFailure })
        assertEquals(0, repository.getCurrentBalance(ShadowResourceType.SHADOW_ESSENCE, accountA))

        // 2. Concurrent Monarch XP & Shadow XP with duplicate sourceTag -> awarded strictly once
        val baseMonarchXp = monarchEngine.getOrCreateProfile(accountA).monarchXp
        val monarchXpWinners = (1..8).map {
            async(Dispatchers.Default) {
                monarchEngine.awardMonarchXp("dup_monarch_tag", 250, "Dup test", accountOwnerId = accountA)
            }
        }.awaitAll()
        assertEquals(1, monarchXpWinners.count { it })
        assertEquals(baseMonarchXp + 250L, monarchEngine.getOrCreateProfile(accountA).monarchXp)

        val shadowXpWinners = (1..8).map {
            async(Dispatchers.Default) {
                evolutionEngine.awardShadowXp(shadowId, 180, "dup_shadow_tag", accountOwnerId = accountA)
            }
        }.awaitAll()
        assertEquals(1, shadowXpWinners.count { it })
        assertEquals(180L, repository.getShadowById(shadowId, accountA)?.shadowXp)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun viewModelAndDataStoreSettings_switchCleanlyAndPersistPerAccount() {
        Dispatchers.setMain(Dispatchers.Default)
        try {
            runBlocking {
                val prefs = UserPreferencesRepository(context)
                val ownerFlow = MutableStateFlow(DEFAULT_SHADOW_ACCOUNT_OWNER_ID)

                // Seed distinct data in Account A and Account B
                monarchEngine.getOrCreateProfile(accountA)
                monarchEngine.awardMonarchXp("vm_test_a", 400, "Alpha", accountA)
                repository.awardResource(ShadowResourceType.SHADOW_ESSENCE, 90, "vm_res_a", "Alpha Essence", accountA).getOrThrow()

                monarchEngine.getOrCreateProfile(accountB)
                repository.awardResource(ShadowResourceType.SHADOW_ESSENCE, 15, "vm_res_b", "Beta Essence", accountB).getOrThrow()

                val vm1 = ShadowRealmViewModel(repository, ownerFlow, prefs)

                // Switch to Account A and update settings
                ownerFlow.value = accountA
                withTimeout(5000) {
                    vm1.uiState.first {
                        it.activeAccountOwnerId == accountA && !it.isLoading && it.shadowEssenceBalance == 90
                    }
                }
                vm1.updateExtractionAlerts(false)
                vm1.updateAutoRecallOnRest(false)
                withTimeout(5000) {
                    vm1.settingsState.first {
                        it.accountOwnerId == accountA && !it.extractionAlertsEnabled && !it.autoRecallOnRest
                    }
                }

                // Switch to Account B -> state & settings reset cleanly to B's values
                ownerFlow.value = accountB
                withTimeout(5000) {
                    vm1.uiState.first {
                        it.activeAccountOwnerId == accountB && !it.isLoading && it.shadowEssenceBalance == 15
                    }
                }
                val bSettings = withTimeout(5000) {
                    vm1.settingsState.first { it.accountOwnerId == accountB }
                }
                assertTrue(bSettings.extractionAlertsEnabled)
                assertTrue(bSettings.autoRecallOnRest)

                // Simulate app restart with fresh ViewModel & PreferencesRepository -> Account A settings survived!
                val prefsRestart = UserPreferencesRepository(context)
                val vmRestart = ShadowRealmViewModel(repository, MutableStateFlow(accountA), prefsRestart)
                val restoredA = withTimeout(5000) {
                    vmRestart.settingsState.first { it.accountOwnerId == accountA && it.updatedAtEpochMs > 0L }
                }
                assertFalse(restoredA.extractionAlertsEnabled)
                assertTrue(restoredA.evolutionAlertsEnabled)
                assertFalse(restoredA.autoRecallOnRest)
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun syncQueueManager_strictlyIsolatesAccountShadowUploadsAndNeverUploadsLocalVault() = runTest {
        seedClearedDungeon(4004L)
        // Populate local vault shadow data + Account A + Account B
        val pendingLocal = extractionEngine.evaluateExtractionEligibility(
            ShadowExtractionSourceType.DUNGEON_COMPLETION,
            "4004",
            "Local Clear",
            accountOwnerId = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
        )
        extractionEngine.confirmExtraction(pendingLocal, DEFAULT_SHADOW_ACCOUNT_OWNER_ID).getOrThrow()

        val pendingA = extractionEngine.evaluateExtractionEligibility(
            ShadowExtractionSourceType.DUNGEON_COMPLETION,
            "4004",
            "Alpha Clear",
            accountOwnerId = accountA
        )
        extractionEngine.confirmExtraction(pendingA, accountA).getOrThrow()

        val pendingB = extractionEngine.evaluateExtractionEligibility(
            ShadowExtractionSourceType.DUNGEON_COMPLETION,
            "4004",
            "Beta Clear",
            accountOwnerId = accountB
        )
        extractionEngine.confirmExtraction(pendingB, accountB).getOrThrow()

        val queueManager = SyncQueueManager(database)
        queueManager.scanAndEnqueueLocalChanges(accountA, nowEpochMs = 1_700_900_000_000L)

        val alphaBatch = queueManager.getReadyBatchForUser(accountA, 1_700_900_000_000L, 100)
        val alphaShadowItems = alphaBatch.filter {
            SyncQueueManager.isShadowEntityType(SyncEntityType.valueOf(it.entityTypeName))
        }
        assertTrue(alphaShadowItems.isNotEmpty())
        assertTrue(
            "Every queued Shadow item must be strictly scoped to Account A",
            alphaShadowItems.all {
                it.localKey.startsWith("${accountA}::") &&
                    !it.payloadJson.contains(DEFAULT_SHADOW_ACCOUNT_OWNER_ID) &&
                    !it.payloadJson.contains(accountB)
            }
        )

        // Switching to Account B must NEVER expose Account A's queued Shadow items for upload
        val betaBatch = queueManager.getReadyBatchForUser(accountB, 1_700_900_000_000L, 100)
        assertTrue(
            "Account A queued Shadow items must never be returned when querying batch for Account B",
            betaBatch.none { it.localKey.startsWith("${accountA}::") }
        )

        // Marking Account A items synced prevents duplicate re-enqueue on unchanged scan
        alphaShadowItems.forEach { queueManager.markSynced(it, accountA, 1_700_900_001_000L) }
        queueManager.scanAndEnqueueLocalChanges(accountA, nowEpochMs = 1_700_900_002_000L)
        val afterSyncBatch = queueManager.getReadyBatchForUser(accountA, 1_700_900_002_000L, 100)
            .filter { SyncQueueManager.isShadowEntityType(SyncEntityType.valueOf(it.entityTypeName)) }
        assertTrue("Unchanged Shadow rows should be deduplicated by SHA-256 hash", afterSyncBatch.isEmpty())
    }
}
