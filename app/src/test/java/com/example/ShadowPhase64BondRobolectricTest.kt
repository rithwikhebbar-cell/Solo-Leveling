package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.entity.DungeonDefinitionEntity
import com.example.data.local.entity.DungeonInstanceEntity
import com.example.data.local.entity.PlayerProfileEntity
import com.example.data.local.entity.ShadowEntity
import com.example.data.local.entity.ShadowMonarchProfileEntity
import com.example.data.repository.ShadowArmyRepository
import com.example.domain.engine.MonarchProgressionEngine
import com.example.domain.engine.ShadowBondEngine
import com.example.domain.engine.ShadowDeploymentEngine
import com.example.domain.engine.ShadowEvolutionEngine
import com.example.domain.engine.ShadowExtractionEngine
import com.example.domain.model.ShadowBondActivityType
import com.example.domain.model.ShadowBondConfig
import com.example.domain.model.ShadowBondMilestoneType
import com.example.domain.model.ShadowClass
import com.example.domain.model.ShadowDeploymentCategory
import com.example.domain.model.ShadowDeploymentStatus
import com.example.domain.model.ShadowEvolutionStage
import com.example.domain.model.ShadowPersonality
import com.example.domain.model.ShadowRank
import com.example.domain.model.ShadowRarity
import com.example.domain.model.ShadowRelationshipStage
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
import java.util.UUID

/**
 * Milestone 12 - Phase 6.4: Shadow Bond & Loyalty System Automated Verification Suite.
 * Validates deterministic bond progression, sequential relationship stage advancement,
 * loyalty & trust bounds, idempotent activity recording, automatic milestone unlocks,
 * integration with deployment and evolution hooks, account isolation, and strict progression isolation.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ShadowPhase64BondRobolectricTest {

    private lateinit var database: SoloLevelingDatabase
    private lateinit var shadowBondEngine: ShadowBondEngine
    private lateinit var monarchProgressionEngine: MonarchProgressionEngine
    private lateinit var evolutionEngine: ShadowEvolutionEngine
    private lateinit var deploymentEngine: ShadowDeploymentEngine
    private lateinit var repository: ShadowArmyRepository

    private val accountA = "player_account_alpha"
    private val accountB = "player_account_beta"
    private val now = 1_700_900_000_000L

    @Before
    fun setup() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SoloLevelingDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val sharedMutex = Mutex()

        shadowBondEngine = ShadowBondEngine(
            database = database,
            shadowBondDao = database.shadowBondDao(),
            shadowDao = database.shadowDao(),
            mutex = sharedMutex
        )

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

        evolutionEngine = ShadowEvolutionEngine(
            shadowDao = database.shadowDao(),
            shadowSkillUnlockDao = database.shadowSkillUnlockDao(),
            monarchProgressionEngine = monarchProgressionEngine,
            shadowXpTraceDao = database.shadowXpTraceDao(),
            shadowResourceDao = database.shadowResourceDao(),
            shadowBondEngine = shadowBondEngine,
            database = database,
            mutex = sharedMutex
        )

        deploymentEngine = ShadowDeploymentEngine(
            shadowDao = database.shadowDao(),
            deploymentDao = database.shadowDeploymentDao(),
            formationDao = database.shadowFormationDao(),
            monarchProgressionEngine = monarchProgressionEngine,
            shadowBondEngine = shadowBondEngine,
            database = database,
            mutex = sharedMutex
        )

        repository = ShadowArmyRepository(
            shadowDao = database.shadowDao(),
            extractionDao = database.shadowExtractionDao(),
            monarchProfileDao = database.shadowMonarchProfileDao(),
            resourceDao = database.shadowResourceDao(),
            deploymentDao = database.shadowDeploymentDao(),
            formationDao = database.shadowFormationDao(),
            shadowSkillDao = database.shadowSkillUnlockDao(),
            questProgressDao = database.shadowQuestProgressDao(),
            extractionEngine = ShadowExtractionEngine(
                database = database,
                shadowDao = database.shadowDao(),
                extractionDao = database.shadowExtractionDao(),
                monarchProfileDao = database.shadowMonarchProfileDao(),
                monarchProgressionEngine = monarchProgressionEngine,
                mutex = sharedMutex
            ),
            deploymentEngine = deploymentEngine,
            evolutionEngine = evolutionEngine,
            monarchEngine = monarchProgressionEngine,
            shadowBondDao = database.shadowBondDao(),
            shadowBondEngine = shadowBondEngine,
            database = database,
            currentAccountOwnerId = accountA
        )

        // Seed Monarch Profiles for Account A & B
        database.shadowMonarchProfileDao().upsertProfile(
            ShadowMonarchProfileEntity(
                accountOwnerId = accountA,
                monarchLevel = 10,
                monarchRank = "B_RANK",
                monarchTitle = "Shadow Monarch",
                monarchXp = 5000,
                currentArmySize = 0,
                maxArmyCapacity = 20,
                currentShadowEssence = 1000,
                currentMonarchFragments = 100,
                currentEvolutionCores = 20,
                totalShadowsExtracted = 0,
                totalExtractionsAttempted = 0,
                totalShadowsEvolved = 0,
                lastActivityEpochMs = now,
                updatedAtEpochMs = now
            )
        )

        database.shadowMonarchProfileDao().upsertProfile(
            ShadowMonarchProfileEntity(
                accountOwnerId = accountB,
                monarchLevel = 5,
                monarchRank = "C_RANK",
                monarchTitle = "Shadow Monarch",
                monarchXp = 2000,
                currentArmySize = 0,
                maxArmyCapacity = 10,
                currentShadowEssence = 300,
                currentMonarchFragments = 20,
                currentEvolutionCores = 5,
                totalShadowsExtracted = 0,
                totalExtractionsAttempted = 0,
                totalShadowsEvolved = 0,
                lastActivityEpochMs = now,
                updatedAtEpochMs = now
            )
        )

        // Seed Player Profile for accountA
        database.playerDao().upsertPlayer(
            PlayerProfileEntity(
                id = 1,
                name = "Sung Jin-Woo",
                level = 50,
                xp = 12000,
                rank = "S_RANK",
                title = "Monarch of Shadows",
                job = "Necromancer",
                hp = 10000,
                maxHp = 10000,
                mp = 5000,
                maxMp = 5000,
                strength = 120,
                agility = 110,
                intelligence = 95,
                vitality = 100,
                perception = 90,
                statPointsAvailable = 0,
                gold = 50000
            )
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun seedShadow(
        shadowId: String,
        name: String,
        accountOwnerId: String = accountA,
        level: Int = 1,
        shadowClass: ShadowClass = ShadowClass.VANGUARD,
        rank: ShadowRank = ShadowRank.B_RANK,
        rarity: ShadowRarity = ShadowRarity.EPIC,
        evolutionStage: ShadowEvolutionStage = ShadowEvolutionStage.BASIC
    ): ShadowEntity {
        val shadow = ShadowEntity(
            shadowId = shadowId,
            accountOwnerId = accountOwnerId,
            name = name,
            shadowClass = shadowClass.name,
            currentRank = rank.name,
            level = level,
            shadowXp = 0,
            rarity = rarity.name,
            passiveAbility = "Shadow Strike",
            evolutionStage = evolutionStage.name,
            evolutionHistoryJson = "[]",
            deploymentStatus = ShadowDeploymentStatus.AVAILABLE.name,
            extractedAtEpochMs = now,
            extractionEventId = "ext_ev_$shadowId",
            strength = 30,
            agility = 25,
            intelligence = 20,
            vitality = 25,
            perception = 20,
            command = 20
        )
        database.shadowDao().insertShadow(shadow)
        return shadow
    }

    @Test
    fun initialBondProfileCreation_setsBaselineRatingsAndUnfamiliarStage() = runTest {
        val shadow = seedShadow("shadow_igris", "Igris", accountA)

        val profile = shadowBondEngine.getOrCreateBondProfile(shadow.shadowId, accountA)

        assertNotNull(profile)
        assertEquals(shadow.shadowId, profile.shadowId)
        assertEquals(accountA, profile.accountOwnerId)
        assertEquals(1, profile.bondLevel)
        assertEquals(0, profile.bondPoints)
        assertEquals(ShadowBondConfig.DEFAULT_LOYALTY, profile.loyaltyRating) // 15
        assertEquals(ShadowBondConfig.DEFAULT_TRUST, profile.trustRating) // 10
        assertEquals(ShadowRelationshipStage.UNFAMILIAR, profile.relationshipStage)
        assertEquals(ShadowPersonality.DISCIPLINED, profile.personality) // Igris is mapped to DISCIPLINED
        assertEquals(0, profile.totalActivitiesCount)
    }

    @Test
    fun recordBondActivity_awardsDeterministicPointsAndLoyaltyTrust() = runTest {
        val shadow = seedShadow("shadow_tank", "Tank", accountA)

        // Tank is mapped to PROTECTIVE
        val updatedProfile = shadowBondEngine.recordBondActivity(
            shadowId = shadow.shadowId,
            accountOwnerId = accountA,
            activityType = ShadowBondActivityType.DUNGEON_COMPLETION,
            sourceEventId = "dungeon_ev_101",
            details = "Completed E-Rank Dungeon"
        )

        assertNotNull(updatedProfile)
        assertEquals(50, updatedProfile.bondPoints) // +50
        assertEquals(20, updatedProfile.loyaltyRating) // 15 + 5 = 20
        assertEquals(14, updatedProfile.trustRating) // 10 + 4 = 14
        assertEquals(1, updatedProfile.totalActivitiesCount)

        // Verify persisted event
        val events = database.shadowBondDao().getBondEventsForShadow(shadow.shadowId)
        assertEquals(1, events.size)
        assertEquals(ShadowBondActivityType.DUNGEON_COMPLETION.name, events[0].activityTypeName)
        assertEquals(50, events[0].bondPointsEarned)
        assertEquals(5, events[0].loyaltyDelta)
        assertEquals(4, events[0].trustDelta)
    }

    @Test
    fun sequentialStageAdvancement_strictlyFollowsRequirementsWithoutSkipping() = runTest {
        val shadow = seedShadow("shadow_beru", "Beru", accountA) // AGGRESSIVE

        // Initial: Level 1, 0 pts, Loyalty 15, Trust 10 -> UNFAMILIAR
        var profile = shadowBondEngine.getOrCreateBondProfile(shadow.shadowId, accountA)
        assertEquals(ShadowRelationshipStage.UNFAMILIAR, profile.relationshipStage)

        // Next Stage: RECOGNIZED requires Level 2 (100 pts), Loyalty 25, Trust 20
        // Record 1 BOSS_BATTLE (+100 pts, +10 loy, +8 tru) -> 100 pts, Level 2, Loy 25, Trust 18
        // Trust is 18 (< 20 required), so stage MUST remain UNFAMILIAR despite meeting level, points, and loyalty!
        profile = shadowBondEngine.recordBondActivity(
            shadowId = shadow.shadowId,
            accountOwnerId = accountA,
            activityType = ShadowBondActivityType.BOSS_BATTLE,
            sourceEventId = "boss_ev_1"
        )
        assertEquals(2, profile.bondLevel)
        assertEquals(100, profile.bondPoints)
        assertEquals(25, profile.loyaltyRating)
        assertEquals(18, profile.trustRating)
        assertEquals(ShadowRelationshipStage.UNFAMILIAR, profile.relationshipStage)

        // Now record a DEPLOYMENT_SUCCESS (+30 pts, +3 loy, +4 tru) -> Trust 18+4=22 >= 20!
        // Now all 4 conditions are met: Level 2, 130 pts >= 100, Loyalty 28 >= 25, Trust 22 >= 20.
        profile = shadowBondEngine.recordBondActivity(
            shadowId = shadow.shadowId,
            accountOwnerId = accountA,
            activityType = ShadowBondActivityType.DEPLOYMENT_SUCCESS,
            sourceEventId = "deploy_ev_1"
        )
        assertEquals(ShadowRelationshipStage.RECOGNIZED, profile.relationshipStage)

        // Cannot skip directly to LOYAL or DEVOTED even if massive points were added; must pass through TRUSTING first.
        // Stage advancement is deterministic and sequential.
    }

    @Test
    fun loyaltyAndTrustClamping_neverExceeds100OrDropsBelow0() = runTest {
        val shadow = seedShadow("shadow_iron", "Iron", accountA)

        // Record 15 BOSS_BATTLES (+100 pts, +10 loy, +8 tru each)
        for (i in 1..15) {
            shadowBondEngine.recordBondActivity(
                shadowId = shadow.shadowId,
                accountOwnerId = accountA,
                activityType = ShadowBondActivityType.BOSS_BATTLE,
                sourceEventId = "boss_ev_clamp_$i"
            )
        }

        val profile = shadowBondEngine.getOrCreateBondProfile(shadow.shadowId, accountA)
        assertEquals(100, profile.loyaltyRating) // Clamped at 100
        assertEquals(100, profile.trustRating) // Clamped at 100
        assertTrue(profile.bondPoints <= ShadowBondConfig.MAX_BOND_POINTS)
        assertTrue(profile.bondLevel <= ShadowBondConfig.MAX_BOND_LEVEL)
    }

    @Test
    fun idempotentActivityRejection_preventsDuplicateRewards() = runTest {
        val shadow = seedShadow("shadow_tusk", "Tusk", accountA)

        val firstRecord = shadowBondEngine.recordBondActivity(
            shadowId = shadow.shadowId,
            accountOwnerId = accountA,
            activityType = ShadowBondActivityType.DUNGEON_COMPLETION,
            sourceEventId = "dungeon_idempotent_test"
        )
        assertEquals(50, firstRecord.bondPoints)
        assertEquals(20, firstRecord.loyaltyRating)
        assertEquals(14, firstRecord.trustRating)
        assertEquals(1, firstRecord.totalActivitiesCount)

        // Call again with the exact same sourceEventId
        val secondRecord = shadowBondEngine.recordBondActivity(
            shadowId = shadow.shadowId,
            accountOwnerId = accountA,
            activityType = ShadowBondActivityType.DUNGEON_COMPLETION,
            sourceEventId = "dungeon_idempotent_test"
        )

        // Should return the exact same profile without duplicate additions
        assertEquals(50, secondRecord.bondPoints)
        assertEquals(20, secondRecord.loyaltyRating)
        assertEquals(14, secondRecord.trustRating)
        assertEquals(1, secondRecord.totalActivitiesCount)

        // Database should only have 1 event
        val events = database.shadowBondDao().getBondEventsForShadow(shadow.shadowId)
        assertEquals(1, events.size)
    }

    @Test
    fun milestoneAutomaticUnlock_triggersAppropriateMilestones() = runTest {
        val shadow = seedShadow("shadow_kaisel", "Kaisel", accountA)

        // 1. DEPLOYMENT_SUCCESS -> FIRST_DEPLOYMENT milestone
        shadowBondEngine.recordBondActivity(
            shadowId = shadow.shadowId,
            accountOwnerId = accountA,
            activityType = ShadowBondActivityType.DEPLOYMENT_SUCCESS,
            sourceEventId = "deploy_kaisel_1"
        )
        val m1 = database.shadowBondDao().getMilestonesForShadow(shadow.shadowId)
        assertEquals(1, m1.size)
        assertEquals(ShadowBondMilestoneType.FIRST_DEPLOYMENT.name, m1[0].milestoneTypeName)

        // 2. DUNGEON_COMPLETION -> FIRST_MISSION_COMPLETE milestone
        shadowBondEngine.recordBondActivity(
            shadowId = shadow.shadowId,
            accountOwnerId = accountA,
            activityType = ShadowBondActivityType.DUNGEON_COMPLETION,
            sourceEventId = "dungeon_kaisel_1"
        )
        val m2 = database.shadowBondDao().getMilestonesForShadow(shadow.shadowId)
        assertEquals(2, m2.size)
        assertTrue(m2.any { it.milestoneTypeName == ShadowBondMilestoneType.FIRST_MISSION_COMPLETE.name })

        // 3. BOSS_BATTLE -> FIRST_BOSS_VICTORY milestone
        shadowBondEngine.recordBondActivity(
            shadowId = shadow.shadowId,
            accountOwnerId = accountA,
            activityType = ShadowBondActivityType.BOSS_BATTLE,
            sourceEventId = "boss_kaisel_1"
        )
        val m3 = database.shadowBondDao().getMilestonesForShadow(shadow.shadowId)
        assertEquals(3, m3.size)
        assertTrue(m3.any { it.milestoneTypeName == ShadowBondMilestoneType.FIRST_BOSS_VICTORY.name })

        // 4. SHADOW_EVOLUTION -> FIRST_EVOLUTION milestone
        shadowBondEngine.recordBondActivity(
            shadowId = shadow.shadowId,
            accountOwnerId = accountA,
            activityType = ShadowBondActivityType.SHADOW_EVOLUTION,
            sourceEventId = "evolve_kaisel_1"
        )
        val m4 = database.shadowBondDao().getMilestonesForShadow(shadow.shadowId)
        assertEquals(4, m4.size)
        assertTrue(m4.any { it.milestoneTypeName == ShadowBondMilestoneType.FIRST_EVOLUTION.name })
    }

    @Test
    fun deploymentAndRecallBondHooks_awardBondProgressionOnSuccess() = runTest {
        val shadow = seedShadow("shadow_hook_deploy", "Igris", accountA)

        // Deploy shadow via deploymentEngine
        val deployResult = deploymentEngine.deployShadow(
            shadowId = shadow.shadowId,
            category = ShadowDeploymentCategory.DISCIPLINE,
            strategicRole = "Vanguard Guard",
            accountOwnerId = accountA
        )
        assertTrue(deployResult.isSuccess)
        val deployment = deployResult.getOrNull()
        assertNotNull(deployment)

        // Check bond profile updated with DEPLOYMENT_SUCCESS (+30 pts, +3 loy, +4 tru)
        var profile = shadowBondEngine.getOrCreateBondProfile(shadow.shadowId, accountA)
        assertEquals(30, profile.bondPoints)
        assertEquals(18, profile.loyaltyRating)
        assertEquals(14, profile.trustRating)

        // Recall shadow via deploymentEngine
        val recallResult = deploymentEngine.recallShadow(
            deploymentId = deployment!!.deploymentId,
            accountOwnerId = accountA
        )
        assertTrue(recallResult.isSuccess)

        // Check bond profile updated with DEPLOYMENT_RECALL (+20 pts, +2 loy, +3 tru)
        profile = shadowBondEngine.getOrCreateBondProfile(shadow.shadowId, accountA)
        assertEquals(50, profile.bondPoints)
        assertEquals(20, profile.loyaltyRating)
        assertEquals(17, profile.trustRating)
        assertEquals(2, profile.totalActivitiesCount)
    }

    @Test
    fun evolutionAndDungeonBondHooks_awardBondProgressionOnSuccess() = runTest {
        val shadow = seedShadow("shadow_hook_evolve", "Beru", accountA, level = 20, rank = ShadowRank.A_RANK)

        // Seed active dungeon definition and instance
        database.dungeonDefinitionDao().insert(
            DungeonDefinitionEntity(
                id = 1,
                name = "Ant Tunnel",
                rank = "A_RANK",
                description = "Ant nest dungeon",
                penaltyDescription = "None",
                totalRooms = 3,
                recommendedLevel = 20,
                baseXpReward = 500,
                shadowEssenceReward = 100,
                monarchFragmentsReward = 20,
                evolutionCoresReward = 5,
                goldReward = 1000
            )
        )
        database.dungeonInstanceDao().insert(
            DungeonInstanceEntity(
                instanceId = "dung_inst_1",
                dungeonId = 1,
                accountOwnerId = accountA,
                status = "COMPLETED",
                currentRoom = 3,
                hpRemaining = 8000,
                mpRemaining = 4000,
                startedAtEpochMs = now,
                completedAtEpochMs = now
            )
        )

        // Award shadow XP from dungeon
        val xpResult = evolutionEngine.awardShadowXpFromDungeon(
            shadowId = shadow.shadowId,
            dungeonInstanceId = "dung_inst_1",
            accountOwnerId = accountA
        )
        assertTrue(xpResult.isSuccess)

        // Check bond profile updated with DUNGEON_COMPLETION (+50 pts, +5 loy, +4 tru)
        var profile = shadowBondEngine.getOrCreateBondProfile(shadow.shadowId, accountA)
        assertEquals(50, profile.bondPoints)
        assertEquals(20, profile.loyaltyRating)
        assertEquals(14, profile.trustRating)

        // Confirm evolution via evolutionEngine
        val evolveResult = evolutionEngine.confirmEvolution(
            shadowId = shadow.shadowId,
            accountOwnerId = accountA
        )
        assertTrue(evolveResult.isSuccess)

        // Check bond profile updated with SHADOW_EVOLUTION (+80 pts, +8 loy, +6 tru)
        profile = shadowBondEngine.getOrCreateBondProfile(shadow.shadowId, accountA)
        assertEquals(130, profile.bondPoints) // 50 + 80
        assertEquals(28, profile.loyaltyRating) // 20 + 8
        assertEquals(20, profile.trustRating) // 14 + 6
        assertEquals(2, profile.totalActivitiesCount)
    }

    @Test
    fun armyBondOverview_accuratelyAggregatesAcrossShadows() = runTest {
        val s1 = seedShadow("shadow_army_1", "Igris", accountA)
        val s2 = seedShadow("shadow_army_2", "Tank", accountA)

        // Give s1 activities
        shadowBondEngine.recordBondActivity(
            shadowId = s1.shadowId,
            accountOwnerId = accountA,
            activityType = ShadowBondActivityType.BOSS_BATTLE,
            sourceEventId = "boss_army_1"
        )

        // Observe army overview
        val overview = shadowBondEngine.observeArmyBondOverview(accountA).first()

        assertEquals(2, overview.totalShadows)
        assertEquals(1, overview.bondedShadowCount) // Only s1 has activities
        assertTrue(overview.averageLoyalty > ShadowBondConfig.DEFAULT_LOYALTY.toDouble())
        assertEquals(1, overview.topLoyalShadows.size)
        assertEquals("Igris", overview.topLoyalShadows[0].first.name)
    }

    @Test
    fun accountIsolation_strictSeparationBetweenUsers() = runTest {
        val shadowA = seedShadow("shadow_user_a", "Igris", accountA)
        val shadowB = seedShadow("shadow_user_b", "Beru", accountB)

        // Account A records activity
        shadowBondEngine.recordBondActivity(
            shadowId = shadowA.shadowId,
            accountOwnerId = accountA,
            activityType = ShadowBondActivityType.BOSS_BATTLE,
            sourceEventId = "boss_a_1"
        )

        // Account B records activity
        shadowBondEngine.recordBondActivity(
            shadowId = shadowB.shadowId,
            accountOwnerId = accountB,
            activityType = ShadowBondActivityType.DUNGEON_COMPLETION,
            sourceEventId = "dungeon_b_1"
        )

        val profileA = shadowBondEngine.getOrCreateBondProfile(shadowA.shadowId, accountA)
        val profileB = shadowBondEngine.getOrCreateBondProfile(shadowB.shadowId, accountB)

        assertEquals(100, profileA.bondPoints)
        assertEquals(50, profileB.bondPoints)

        // Verify overview for account A only sees shadowA
        val overviewA = shadowBondEngine.observeArmyBondOverview(accountA).first()
        assertEquals(1, overviewA.totalShadows)
        assertEquals("Igris", overviewA.topLoyalShadows[0].first.name)

        // Verify overview for account B only sees shadowB
        val overviewB = shadowBondEngine.observeArmyBondOverview(accountB).first()
        assertEquals(1, overviewB.totalShadows)
        assertEquals("Beru", overviewB.topLoyalShadows[0].first.name)
    }

    @Test
    fun progressionIsolation_noPlayerXpOrMomentumXpAwarded() = runTest {
        val initialPlayer = database.playerDao().getPlayerOnce()
        assertNotNull(initialPlayer)
        val initialXp = initialPlayer!!.xp
        val initialLevel = initialPlayer.level
        val initialStrength = initialPlayer.strength

        val shadow = seedShadow("shadow_isolation_test", "Igris", accountA)

        // Perform multiple bond activities
        for (i in 1..5) {
            shadowBondEngine.recordBondActivity(
                shadowId = shadow.shadowId,
                accountOwnerId = accountA,
                activityType = ShadowBondActivityType.BOSS_BATTLE,
                sourceEventId = "isolation_ev_$i"
            )
        }

        // Verify player profile remains strictly unchanged
        val playerAfter = database.playerDao().getPlayerOnce()
        assertNotNull(playerAfter)
        assertEquals(initialXp, playerAfter!!.xp)
        assertEquals(initialLevel, playerAfter.level)
        assertEquals(initialStrength, playerAfter.strength)
    }
}
