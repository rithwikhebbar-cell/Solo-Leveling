package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.entity.PlayerProfileEntity
import com.example.data.local.entity.ShadowEntity
import com.example.data.local.entity.ShadowFormationEntity
import com.example.data.local.entity.ShadowMonarchProfileEntity
import com.example.data.repository.ShadowArmyRepository
import com.example.domain.engine.MonarchProgressionEngine
import com.example.domain.engine.ShadowBondEngine
import com.example.domain.engine.ShadowDeploymentEngine
import com.example.domain.engine.ShadowEvolutionEngine
import com.example.domain.engine.ShadowMissionAssignmentEngine
import com.example.domain.engine.ShadowSquadEngine
import com.example.domain.engine.ShadowTacticalAiAdvisor
import com.example.domain.engine.ShadowTacticalAnalyticsEngine
import com.example.domain.engine.ShadowTacticalIntelligenceEngine
import com.example.domain.model.DungeonDefinition
import com.example.domain.model.DungeonRank
import com.example.domain.model.DungeonType
import com.example.domain.model.ShadowClass
import com.example.domain.model.ShadowDeploymentCategory
import com.example.domain.model.ShadowDeploymentStatus
import com.example.domain.model.ShadowEvolutionStage
import com.example.domain.model.ShadowFormationType
import com.example.domain.model.ShadowRank
import com.example.domain.model.ShadowRarity
import com.example.domain.model.ShadowSquadCategory
import com.example.domain.model.ShadowTacticalRole
import kotlinx.coroutines.flow.first
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

/**
 * Milestone 12 - Phase 6.5: Shadow Army Intelligence & Tactical Command System Automated Verification Suite.
 * Validates deterministic capability assessment, tactical role classification, squad management (CRUD,
 * leader designation, duplicate prevention, active status switching), formation tactical balance,
 * mission suitability evaluation & confirmed deployment hook, persisted tactical analytics, read-only AI advisor,
 * account isolation, and strict progression isolation.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ShadowPhase65TacticalCommandRobolectricTest {

    private lateinit var database: SoloLevelingDatabase
    private lateinit var tacticalIntelligenceEngine: ShadowTacticalIntelligenceEngine
    private lateinit var squadEngine: ShadowSquadEngine
    private lateinit var missionAssignmentEngine: ShadowMissionAssignmentEngine
    private lateinit var analyticsEngine: ShadowTacticalAnalyticsEngine
    private lateinit var aiAdvisor: ShadowTacticalAiAdvisor
    private lateinit var deploymentEngine: ShadowDeploymentEngine
    private lateinit var bondEngine: ShadowBondEngine
    private lateinit var evolutionEngine: ShadowEvolutionEngine
    private lateinit var monarchProgressionEngine: MonarchProgressionEngine
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

        bondEngine = ShadowBondEngine(
            database = database,
            shadowBondDao = database.shadowBondDao(),
            shadowDao = database.shadowDao(),
            mutex = sharedMutex
        )

        evolutionEngine = ShadowEvolutionEngine(
            shadowDao = database.shadowDao(),
            shadowSkillUnlockDao = database.shadowSkillUnlockDao(),
            monarchProgressionEngine = monarchProgressionEngine,
            shadowXpTraceDao = database.shadowXpTraceDao(),
            shadowResourceDao = database.shadowResourceDao(),
            shadowBondEngine = bondEngine,
            database = database,
            mutex = sharedMutex
        )

        deploymentEngine = ShadowDeploymentEngine(
            shadowDao = database.shadowDao(),
            shadowDeploymentDao = database.shadowDeploymentDao(),
            monarchProgressionEngine = monarchProgressionEngine,
            shadowFormationDao = database.shadowFormationDao(),
            shadowBondEngine = bondEngine,
            database = database,
            mutex = sharedMutex
        )

        tacticalIntelligenceEngine = ShadowTacticalIntelligenceEngine(
            shadowDao = database.shadowDao(),
            shadowBondDao = database.shadowBondDao(),
            shadowFormationDao = database.shadowFormationDao()
        )

        squadEngine = ShadowSquadEngine(
            database = database,
            shadowTacticalDao = database.shadowTacticalDao(),
            shadowDao = database.shadowDao(),
            tacticalEngine = tacticalIntelligenceEngine,
            mutex = sharedMutex
        )

        missionAssignmentEngine = ShadowMissionAssignmentEngine(
            database = database,
            shadowTacticalDao = database.shadowTacticalDao(),
            shadowDao = database.shadowDao(),
            dungeonDao = database.dungeonInstanceDao(),
            shadowDeploymentEngine = deploymentEngine,
            tacticalIntelligenceEngine = tacticalIntelligenceEngine,
            squadEngine = squadEngine,
            mutex = sharedMutex
        )

        analyticsEngine = ShadowTacticalAnalyticsEngine(
            database = database,
            shadowDao = database.shadowDao(),
            shadowDeploymentDao = database.shadowDeploymentDao(),
            shadowTacticalDao = database.shadowTacticalDao(),
            tacticalEngine = tacticalIntelligenceEngine
        )

        aiAdvisor = ShadowTacticalAiAdvisor()

        repository = ShadowArmyRepository(
            shadowMonarchProfileDao = database.shadowMonarchProfileDao(),
            shadowDao = database.shadowDao(),
            shadowExtractionDao = database.shadowExtractionDao(),
            shadowFormationDao = database.shadowFormationDao(),
            shadowDeploymentDao = database.shadowDeploymentDao(),
            shadowSkillUnlockDao = database.shadowSkillUnlockDao(),
            monarchSkillUnlockDao = database.monarchSkillUnlockDao(),
            shadowQuestProgressDao = database.shadowQuestProgressDao(),
            shadowResourceDao = database.shadowResourceDao(),
            shadowEvidenceClaimDao = database.shadowEvidenceClaimDao(),
            shadowResonanceDao = database.shadowResonanceDao(),
            shadowBondDao = database.shadowBondDao(),
            shadowTacticalDao = database.shadowTacticalDao(),
            monarchProgressionEngine = monarchProgressionEngine,
            shadowExtractionEngine = null,
            shadowEvolutionEngine = evolutionEngine,
            shadowDeploymentEngine = deploymentEngine,
            shadowBondEngine = bondEngine,
            shadowTacticalIntelligenceEngine = tacticalIntelligenceEngine,
            shadowSquadEngine = squadEngine,
            shadowMissionAssignmentEngine = missionAssignmentEngine,
            shadowTacticalAnalyticsEngine = analyticsEngine,
            shadowTacticalAiAdvisor = aiAdvisor
        )

        // Seed player profile
        database.playerDao().upsertPlayer(
            PlayerProfileEntity(
                accountOwnerId = accountA,
                level = 10,
                currentXp = 500,
                totalXp = 5000,
                rank = "D",
                strength = 20,
                agility = 20,
                intelligence = 20,
                vitality = 20,
                perception = 20,
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            )
        )

        // Seed monarch profiles for Account A and B
        database.shadowMonarchProfileDao().insertProfile(
            ShadowMonarchProfileEntity(
                accountOwnerId = accountA,
                monarchLevel = 5,
                monarchRank = "B_RANK",
                monarchTitle = "Monarch of Shadows",
                monarchXp = 1000,
                currentArmySize = 3,
                maxArmyCapacity = 10,
                currentShadowEssence = 500,
                currentMonarchFragments = 50,
                currentEvolutionCores = 5,
                totalShadowsExtracted = 3,
                totalExtractionsAttempted = 4,
                totalShadowsEvolved = 1,
                lastActivityEpochMs = now,
                updatedAtEpochMs = now
            )
        )
        database.shadowMonarchProfileDao().insertProfile(
            ShadowMonarchProfileEntity(
                accountOwnerId = accountB,
                monarchLevel = 2,
                monarchRank = "E_RANK",
                monarchTitle = "Shadow Initiate",
                monarchXp = 200,
                currentArmySize = 1,
                maxArmyCapacity = 5,
                currentShadowEssence = 100,
                currentMonarchFragments = 10,
                currentEvolutionCores = 0,
                totalShadowsExtracted = 1,
                totalExtractionsAttempted = 1,
                totalShadowsEvolved = 0,
                lastActivityEpochMs = now,
                updatedAtEpochMs = now
            )
        )

        // Seed shadows for Account A:
        // 1. Igris (Vanguard / Knight) - Heavy Frontline / Assault
        database.shadowDao().insertShadow(
            ShadowEntity(
                shadowId = "shadow_igris",
                accountOwnerId = accountA,
                name = "Igris",
                shadowClass = ShadowClass.VANGUARD.name,
                currentRank = ShadowRank.A_RANK.name,
                level = 30,
                shadowXp = 500,
                rarity = ShadowRarity.EPIC.name,
                passiveAbility = "Dominion Command",
                evolutionStage = ShadowEvolutionStage.KNIGHT.name,
                evolutionHistoryJson = "[]",
                deploymentStatus = ShadowDeploymentStatus.AVAILABLE.name,
                extractedAtEpochMs = now,
                extractionEventId = "ext_1",
                strength = 50,
                agility = 40,
                intelligence = 20,
                vitality = 45,
                perception = 30,
                command = 35
            )
        )

        // 2. Tank (Tank / Elite) - Heavy Frontline Defense
        database.shadowDao().insertShadow(
            ShadowEntity(
                shadowId = "shadow_tank",
                accountOwnerId = accountA,
                name = "Tank",
                shadowClass = ShadowClass.TANK.name,
                currentRank = ShadowRank.B_RANK.name,
                level = 20,
                shadowXp = 200,
                rarity = ShadowRarity.RARE.name,
                passiveAbility = "Iron Hide",
                evolutionStage = ShadowEvolutionStage.ELITE.name,
                evolutionHistoryJson = "[]",
                deploymentStatus = ShadowDeploymentStatus.AVAILABLE.name,
                extractedAtEpochMs = now,
                extractionEventId = "ext_2",
                strength = 35,
                agility = 15,
                intelligence = 10,
                vitality = 55,
                perception = 20,
                command = 15
            )
        )

        // 3. Kaisel (Scout / Basic) - Reconnaissance & Mobility
        database.shadowDao().insertShadow(
            ShadowEntity(
                shadowId = "shadow_kaisel",
                accountOwnerId = accountA,
                name = "Kaisel",
                shadowClass = ShadowClass.SCOUT.name,
                currentRank = ShadowRank.B_RANK.name,
                level = 18,
                shadowXp = 150,
                rarity = ShadowRarity.RARE.name,
                passiveAbility = "Shadow Flight",
                evolutionStage = ShadowEvolutionStage.BASIC.name,
                evolutionHistoryJson = "[]",
                deploymentStatus = ShadowDeploymentStatus.AVAILABLE.name,
                extractedAtEpochMs = now,
                extractionEventId = "ext_3",
                strength = 20,
                agility = 50,
                intelligence = 25,
                vitality = 25,
                perception = 45,
                command = 10
            )
        )

        // Seed 1 shadow for Account B
        database.shadowDao().insertShadow(
            ShadowEntity(
                shadowId = "shadow_account_b_unit",
                accountOwnerId = accountB,
                name = "Goblin Shadow",
                shadowClass = ShadowClass.SOLDIER.name,
                currentRank = ShadowRank.E_RANK.name,
                level = 5,
                shadowXp = 50,
                rarity = ShadowRarity.COMMON.name,
                passiveAbility = "Pack Instinct",
                evolutionStage = ShadowEvolutionStage.BASIC.name,
                evolutionHistoryJson = "[]",
                deploymentStatus = ShadowDeploymentStatus.AVAILABLE.name,
                extractedAtEpochMs = now,
                extractionEventId = "ext_b_1",
                strength = 10,
                agility = 12,
                intelligence = 5,
                vitality = 10,
                perception = 10,
                command = 5
            )
        )
    }

    @After
    fun teardown() {
        database.close()
    }

    // =========================================================================
    // 1. DETERMINISTIC CAPABILITY ASSESSMENT & TACTICAL ROLE CLASSIFICATION
    // =========================================================================

    @Test
    fun tacticalCapabilityVector_andRoles_areDeterministicallyDerived() = runTest {
        val igris = database.shadowDao().getShadowById("shadow_igris", accountA)!!
        val vector = tacticalIntelligenceEngine.evaluateCapabilityVector(igris)

        // Verify values are bounded between 0 and 100
        assertTrue("Offensive capability bounded", vector.offensive in 0..100)
        assertTrue("Defensive capability bounded", vector.defensive in 0..100)
        assertTrue("Mobility capability bounded", vector.mobility in 0..100)
        assertTrue("Survivability capability bounded", vector.survivability in 0..100)

        // Igris has 50 STR, 40 AGI, 45 VIT, A-Rank, Knight stage -> High Offensive & Frontline
        assertTrue("Igris has high offensive capability", vector.offensive > 40)
        assertTrue("Igris has high survivability", vector.survivability > 40)

        val profile = tacticalIntelligenceEngine.buildTacticalProfile(igris)
        assertEquals("Igris shadow ID matches", "shadow_igris", profile.shadowId)
        // Vanguard with high STR and Command -> FRONTLINE or ASSAULT
        assertTrue(
            "Igris primary role is FRONTLINE or ASSAULT",
            profile.primaryRole == ShadowTacticalRole.FRONTLINE || profile.primaryRole == ShadowTacticalRole.ASSAULT
        )
        assertNotNull("Rationale is provided and non-empty", profile.roleRationale)
        assertTrue("Rationale describes stats and class", profile.roleRationale.isNotEmpty())

        // Tank should be classified as FRONTLINE
        val tank = database.shadowDao().getShadowById("shadow_tank", accountA)!!
        val tankProfile = tacticalIntelligenceEngine.buildTacticalProfile(tank)
        assertEquals(
            "Tank shadow class yields FRONTLINE primary role",
            ShadowTacticalRole.FRONTLINE,
            tankProfile.primaryRole
        )

        // Kaisel should be classified as RECONNAISSANCE or ASSAULT
        val kaisel = database.shadowDao().getShadowById("shadow_kaisel", accountA)!!
        val kaiselProfile = tacticalIntelligenceEngine.buildTacticalProfile(kaisel)
        assertEquals(
            "Scout class yields RECONNAISSANCE primary role",
            ShadowTacticalRole.RECONNAISSANCE,
            kaiselProfile.primaryRole
        )
    }

    // =========================================================================
    // 2. SQUAD CRUD, LEADER ASSIGNMENT, AND DUPLICATE MEMBER RESTRICTIONS
    // =========================================================================

    @Test
    fun squadManagement_createAddMembersLeaderAndActivation_worksCorrectly() = runTest {
        // 1. Create a squad
        val squadResult = squadEngine.createSquad(
            accountOwnerId = accountA,
            name = "Iron Vanguard",
            category = ShadowSquadCategory.ELITE_GUARD,
            formationType = ShadowFormationType.BALANCED_INFILTRATION,
            notes = "Primary strike team"
        )
        assertTrue("Squad created successfully", squadResult.isSuccess)
        val squad = squadResult.getOrThrow()
        assertEquals("Iron Vanguard", squad.name)
        assertEquals(ShadowSquadCategory.ELITE_GUARD, squad.category)

        // 2. Add Igris as leader
        val addIgrisResult = squadEngine.addMemberToSquad(
            accountOwnerId = accountA,
            squadId = squad.squadId,
            shadowId = "shadow_igris",
            asLeader = true
        )
        assertTrue("Igris added to squad", addIgrisResult.isSuccess)

        // 3. Add Tank as member
        val addTankResult = squadEngine.addMemberToSquad(
            accountOwnerId = accountA,
            squadId = squad.squadId,
            shadowId = "shadow_tank",
            asLeader = false
        )
        assertTrue("Tank added to squad", addTankResult.isSuccess)

        // 4. Verify duplicate shadow insertion into same squad is rejected
        val duplicateResult = squadEngine.addMemberToSquad(
            accountOwnerId = accountA,
            squadId = squad.squadId,
            shadowId = "shadow_igris",
            asLeader = false
        )
        assertTrue("Duplicate member rejected", duplicateResult.isFailure)

        // 5. Verify squad details
        val detail = squadEngine.getSquadDetail(accountA, squad.squadId)
        assertNotNull("Squad detail retrieved", detail)
        assertEquals(2, detail!!.members.size)
        assertEquals("shadow_igris", detail.squad.leaderShadowId)
        assertTrue("Leader flag is set on Igris", detail.members.first { it.shadow.shadowId == "shadow_igris" }.member.isLeader)
        assertFalse("Tank is not leader", detail.members.first { it.shadow.shadowId == "shadow_tank" }.member.isLeader)

        // 6. Test setting a new leader (Tank)
        val setLeaderResult = squadEngine.setSquadLeader(accountA, squad.squadId, "shadow_tank")
        assertTrue("Leader change succeeded", setLeaderResult.isSuccess)
        val updatedDetail = squadEngine.getSquadDetail(accountA, squad.squadId)
        assertEquals("shadow_tank", updatedDetail!!.squad.leaderShadowId)

        // 7. Test active squad switching
        val activateResult = squadEngine.setActiveSquad(accountA, squad.squadId)
        assertTrue("Active squad set", activateResult.isSuccess)
        val activeSquad = squadEngine.getActiveSquad(accountA)
        assertNotNull("Active squad exists", activeSquad)
        assertEquals(squad.squadId, activeSquad!!.squad.squadId)
    }

    @Test
    fun squadDuplication_andDeletion_worksCorrectly() = runTest {
        // Create squad and add Kaisel
        val squad = squadEngine.createSquad(
            accountOwnerId = accountA,
            name = "Recon Alpha",
            category = ShadowSquadCategory.RECON_SQUAD,
            formationType = ShadowFormationType.DEFENSIVE_PHALANX
        ).getOrThrow()

        squadEngine.addMemberToSquad(accountA, squad.squadId, "shadow_kaisel", asLeader = true)

        // Duplicate squad
        val dupResult = squadEngine.duplicateSquad(accountA, squad.squadId)
        assertTrue("Duplication succeeded", dupResult.isSuccess)
        val duplicated = dupResult.getOrThrow()
        assertEquals("Recon Alpha (Copy)", duplicated.name)

        // Verify members copied
        val dupDetail = squadEngine.getSquadDetail(accountA, duplicated.squadId)
        assertNotNull("Duplicated squad detail retrieved", dupDetail)
        assertEquals(1, dupDetail!!.members.size)
        assertEquals("shadow_kaisel", dupDetail.members[0].shadow.shadowId)

        // Delete duplicated squad
        val deleteResult = squadEngine.deleteSquad(accountA, duplicated.squadId)
        assertTrue("Squad deleted", deleteResult.isSuccess)
        val deletedDetail = squadEngine.getSquadDetail(accountA, duplicated.squadId)
        assertNull("Squad no longer exists", deletedDetail)
    }

    // =========================================================================
    // 3. FORMATION TACTICAL ASSESSMENT & WEAKNESS DETECTION
    // =========================================================================

    @Test
    fun formationAssessment_detectsStrengthsAndWeaknesses() = runTest {
        // Create an active formation with only Tank and Igris (Frontline heavy, zero ranged/support)
        database.shadowFormationDao().insertFormation(
            ShadowFormationEntity(
                formationId = "form_frontline_heavy",
                accountOwnerId = accountA,
                name = "Vanguard Wedge",
                formationTypeName = ShadowFormationType.VANGUARD_SPEARHEAD.name,
                description = "Brute force push",
                maxSlots = 5,
                memberShadowIdsJson = "[\"shadow_igris\",\"shadow_tank\"]",
                isActive = true,
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            )
        )

        val assessment = tacticalIntelligenceEngine.evaluateFormation(accountA, "form_frontline_heavy")
        assertNotNull("Assessment generated", assessment)
        assertTrue("Tactical balance score between 0 and 100", assessment!!.tacticalBalanceScore in 0..100)
        assertTrue("Role coverage contains Frontline", assessment.roleCoverage.containsKey(ShadowTacticalRole.FRONTLINE))

        // Should detect lack of ranged or support
        assertTrue(
            "Identifies missing ranged or support weaknesses",
            assessment.identifiedWeaknesses.any { it.contains("Ranged", ignoreCase = true) || it.contains("Support", ignoreCase = true) }
        )
    }

    // =========================================================================
    // 4. INTELLIGENT MISSION ASSIGNMENT & DEPLOYMENT HOOK
    // =========================================================================

    @Test
    fun missionSuitability_evaluationAndUserConfirmedDeployment_worksAuthoritatively() = runTest {
        // Create a squad for Account A
        val squad = squadEngine.createSquad(
            accountOwnerId = accountA,
            name = "Boss Assassins",
            category = ShadowSquadCategory.ASSAULT_SQUAD,
            formationType = ShadowFormationType.VANGUARD_SPEARHEAD
        ).getOrThrow()

        squadEngine.addMemberToSquad(accountA, squad.squadId, "shadow_igris", asLeader = true)

        // Evaluate suitability for an Assault / Boss mission
        val mockMission = com.example.domain.model.MissionTacticalRequirement(
            missionId = "dungeon_demon_castle",
            missionTitle = "Demon Castle Summit",
            missionType = "DUNGEON",
            recommendedRoles = listOf(ShadowTacticalRole.ASSAULT, ShadowTacticalRole.FRONTLINE),
            minimumAveragePower = 40,
            recommendedSquadSize = 2,
            threatRating = "HIGH",
            tacticalDescription = "Demon Lord encounter requiring high offensive assault power."
        )

        val evaluation = missionAssignmentEngine.evaluateMissionSuitability(
            accountOwnerId = accountA,
            mission = mockMission,
            targetSquadId = squad.squadId
        )

        assertTrue("Suitability score bounded between 0 and 100", evaluation.suitabilityScore in 0..100)
        assertTrue("Projected success rate bounded between 0 and 100", evaluation.projectedSuccessRate in 0..100)
        assertNotNull("Recommended strategy is present", evaluation.recommendedStrategy)

        // Confirm deployment via authoritative engine
        val deploymentResult = missionAssignmentEngine.confirmAndDeployMission(
            accountOwnerId = accountA,
            evaluation = evaluation,
            userConfirmed = true
        )

        assertTrue("Mission deployment confirmed and completed", deploymentResult.isSuccess)
        val assignmentRecord = deploymentResult.getOrThrow()
        assertEquals("Demon Castle Summit", assignmentRecord.missionTitle)
        assertEquals("IN_PROGRESS", assignmentRecord.status)

        // Verify Shadow status updated through authoritative ShadowDeploymentEngine
        val deployedIgris = database.shadowDao().getShadowById("shadow_igris", accountA)!!
        assertEquals(
            "Shadow deployment status reflects active mission",
            ShadowDeploymentStatus.DEPLOYED.name,
            deployedIgris.deploymentStatus
        )
    }

    // =========================================================================
    // 5. TACTICAL PERFORMANCE ANALYTICS & ALERTS
    // =========================================================================

    @Test
    fun tacticalAnalytics_derivesMetricsFromPersistedRecords() = runTest {
        val analytics = analyticsEngine.calculateAnalytics(accountA)
        assertNotNull("Analytics calculated", analytics)
        // With 3 shadows recruited:
        assertEquals("Total shadows analyzed", 3, analytics.totalShadowsEvaluated)

        // Alerts generated
        val alerts = analyticsEngine.generateTacticalAlerts(accountA)
        assertNotNull("Tactical alerts returned", alerts)
    }

    // =========================================================================
    // 6. READ-ONLY AI ADVISOR & STRICT PROGRESSION ISOLATION
    // =========================================================================

    @Test
    fun tacticalAiAdvisor_isStrictlyReadOnly_andNeverModifiesPlayerProgression() = runTest {
        val playerBefore = database.playerDao().getPlayerOnce()!!
        val monarchBefore = database.shadowMonarchProfileDao().getProfileForOwnerOnce(accountA)!!

        val briefing = aiAdvisor.buildTacticalBriefing(
            activeSquad = null,
            formationAssessment = null,
            availableMissions = emptyList(),
            analytics = analyticsEngine.calculateAnalytics(accountA),
            alerts = emptyList()
        )

        assertNotNull("AI briefing generated", briefing)
        assertTrue("Briefing contains tactical advice", briefing.adviceSummary.isNotEmpty())

        // STRICT PROGRESSION ISOLATION:
        // Ensure player XP, Level, Rank, and Monarch XP remain strictly identical
        val playerAfter = database.playerDao().getPlayerOnce()!!
        val monarchAfter = database.shadowMonarchProfileDao().getProfileForOwnerOnce(accountA)!!

        assertEquals("Player XP remains untouched", playerBefore.currentXp, playerAfter.currentXp)
        assertEquals("Player Level remains untouched", playerBefore.level, playerAfter.level)
        assertEquals("Player Rank remains untouched", playerBefore.rank, playerAfter.rank)
        assertEquals("Monarch XP remains untouched", monarchBefore.monarchXp, monarchAfter.monarchXp)
        assertEquals("Monarch Level remains untouched", monarchBefore.monarchLevel, monarchAfter.monarchLevel)
    }

    // =========================================================================
    // 7. ACCOUNT ISOLATION
    // =========================================================================

    @Test
    fun accountIsolation_preventsCrossAccountSquadAccess() = runTest {
        // Account A creates a squad
        val squadA = squadEngine.createSquad(
            accountOwnerId = accountA,
            name = "Account A Elite",
            category = ShadowSquadCategory.ELITE_GUARD,
            formationType = ShadowFormationType.BALANCED_INFILTRATION
        ).getOrThrow()

        // Account B attempts to query or mutate Account A's squad
        val detailByB = squadEngine.getSquadDetail(accountOwnerId = accountB, squadId = squadA.squadId)
        assertNull("Account B cannot read Account A squad", detailByB)

        val mutateByB = squadEngine.addMemberToSquad(
            accountOwnerId = accountB,
            squadId = squadA.squadId,
            shadowId = "shadow_account_b_unit"
        )
        assertTrue("Account B cannot add members to Account A squad", mutateByB.isFailure)
    }
}
