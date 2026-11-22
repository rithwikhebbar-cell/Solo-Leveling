package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.dao.MonarchSkillUnlockDao
import com.example.data.local.dao.ShadowDao
import com.example.data.local.dao.ShadowDeploymentDao
import com.example.data.local.dao.ShadowExtractionDao
import com.example.data.local.dao.ShadowFormationDao
import com.example.data.local.dao.ShadowMonarchProfileDao
import com.example.data.local.dao.ShadowQuestProgressDao
import com.example.data.local.dao.ShadowResourceDao
import com.example.data.local.dao.ShadowSkillUnlockDao
import com.example.data.local.entity.ShadowResourceTransactionEntity
import com.example.data.local.entity.toDomain
import com.example.domain.engine.MonarchProgressionEngine
import com.example.domain.engine.ShadowDeploymentEngine
import com.example.domain.engine.ShadowEvolutionEngine
import com.example.domain.engine.ShadowExtractionEngine
import com.example.domain.model.DEFAULT_SHADOW_ACCOUNT_OWNER_ID
import com.example.domain.model.MonarchSkillCatalog
import com.example.domain.model.MonarchSkillDefinition
import com.example.domain.model.Shadow
import com.example.data.local.dao.ShadowBondDao
import com.example.data.local.dao.ShadowTacticalDao
import com.example.data.local.dao.ShadowDominionDao
import com.example.domain.engine.ShadowDominionEngine
import com.example.domain.engine.ShadowTerritoryEngine
import com.example.domain.engine.ShadowInfrastructureEngine
import com.example.domain.engine.ShadowGarrisonEngine
import com.example.domain.engine.ShadowDominionResearchEngine
import com.example.domain.engine.ShadowDominionAiAdvisor
import com.example.domain.model.ShadowDominion
import com.example.domain.model.ShadowTerritory
import com.example.domain.model.ShadowInfrastructure
import com.example.domain.model.ShadowGarrison
import com.example.domain.model.ShadowDominionResearch
import com.example.domain.model.ShadowDominionActivity
import com.example.domain.model.DominionTerritoryDetail
import com.example.domain.model.DominionOverview
import com.example.domain.model.DominionAiBriefing
import com.example.domain.model.CitadelUpgradeType
import com.example.domain.model.ShadowInfrastructureType
import com.example.domain.model.ShadowTerritoryState
import com.example.domain.model.ShadowDominionConfig
import com.example.domain.engine.ShadowTacticalIntelligenceEngine
import com.example.domain.engine.ShadowSquadEngine
import com.example.domain.engine.ShadowMissionAssignmentEngine
import com.example.domain.engine.ShadowTacticalAnalyticsEngine
import com.example.domain.model.ShadowTacticalProfile
import com.example.domain.model.ShadowTacticalRole
import com.example.domain.model.ShadowSquad
import com.example.domain.model.ShadowSquadCategory
import com.example.domain.model.SquadDetail
import com.example.domain.model.SquadMemberDetail
import com.example.domain.model.SquadSynergyReport
import com.example.domain.model.FormationTacticalAssessment
import com.example.domain.model.MissionTacticalRequirement
import com.example.domain.model.MissionSuitabilityEvaluation
import com.example.domain.model.MissionType
import com.example.domain.model.ShadowTacticalAnalytics
import com.example.domain.model.TacticalAlert
import com.example.domain.model.TacticalCapabilityVector
import com.example.domain.engine.ShadowBondEngine
import com.example.domain.model.ShadowArmyBondOverview
import com.example.domain.model.ShadowBondActivityType
import com.example.domain.model.ShadowBondEvent
import com.example.domain.model.ShadowBondMilestone
import com.example.domain.model.ShadowBondProfile
import com.example.domain.model.ShadowClass
import com.example.domain.model.ShadowDeploymentRecord
import com.example.domain.model.ShadowEvolutionStage
import com.example.domain.model.ShadowExtractionEvent
import com.example.domain.model.ShadowExtractionOutcome
import com.example.domain.model.ShadowFormation
import com.example.domain.model.ShadowMonarchProfile
import com.example.domain.model.ShadowQuestCatalog
import com.example.domain.model.ShadowQuestDefinition
import com.example.domain.model.ShadowQuestProgress
import com.example.domain.model.ShadowRank
import com.example.domain.model.ShadowRarity
import com.example.domain.model.ShadowResourceTransaction
import com.example.domain.model.ShadowResourceTransactionType
import com.example.domain.model.ShadowResourceType
import com.example.domain.model.ShadowSkillCatalog
import com.example.domain.model.ShadowSkillDefinition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

class ShadowArmyRepository(
    private val monarchProfileDao: ShadowMonarchProfileDao,
    private val shadowDao: ShadowDao,
    private val extractionDao: ShadowExtractionDao,
    private val skillUnlockDao: ShadowSkillUnlockDao,
    private val deploymentDao: ShadowDeploymentDao,
    private val formationDao: ShadowFormationDao,
    private val monarchSkillUnlockDao: MonarchSkillUnlockDao,
    private val questProgressDao: ShadowQuestProgressDao,
    private val resourceDao: ShadowResourceDao,
    private val shadowBondDao: ShadowBondDao? = null,
    private val shadowTacticalDao: ShadowTacticalDao? = null,
    private val shadowDominionDao: ShadowDominionDao? = null,
    // Engines
    val monarchProgressionEngine: MonarchProgressionEngine,
    val extractionEngine: ShadowExtractionEngine,
    val evolutionEngine: ShadowEvolutionEngine,
    val deploymentEngine: ShadowDeploymentEngine,
    shadowBondEngine: ShadowBondEngine? = null,
    tacticalIntelligenceEngine: ShadowTacticalIntelligenceEngine? = null,
    squadEngine: ShadowSquadEngine? = null,
    missionAssignmentEngine: ShadowMissionAssignmentEngine? = null,
    tacticalAnalyticsEngine: ShadowTacticalAnalyticsEngine? = null,
    dominionEngine: ShadowDominionEngine? = null,
    territoryEngine: ShadowTerritoryEngine? = null,
    infrastructureEngine: ShadowInfrastructureEngine? = null,
    garrisonEngine: ShadowGarrisonEngine? = null,
    researchEngine: ShadowDominionResearchEngine? = null,
    dominionAiAdvisor: ShadowDominionAiAdvisor? = null,
    val database: SoloLevelingDatabase? = null,
    private val mutex: Mutex = Mutex()
) {

    val tacticalIntelligence: ShadowTacticalIntelligenceEngine =
        tacticalIntelligenceEngine ?: ShadowTacticalIntelligenceEngine()

    val squadManager: ShadowSquadEngine =
        squadEngine ?: run {
            val dao = shadowTacticalDao ?: database?.shadowTacticalDao()
            val db = database ?: run {
                // In-memory fallback if database reference not passed
                null
            }
            if (dao != null && db != null) {
                ShadowSquadEngine(db, dao, shadowDao, tacticalIntelligence, mutex)
            } else {
                ShadowSquadEngine(
                    database ?: db!!,
                    dao ?: database!!.shadowTacticalDao(),
                    shadowDao,
                    tacticalIntelligence,
                    mutex
                )
            }
        }

    val missionManager: ShadowMissionAssignmentEngine =
        missionAssignmentEngine ?: run {
            val dao = shadowTacticalDao ?: database?.shadowTacticalDao()
            ShadowMissionAssignmentEngine(
                database = database!!,
                shadowTacticalDao = dao ?: database.shadowTacticalDao(),
                shadowDao = shadowDao,
                dungeonDefinitionDao = database.dungeonDefinitionDao(),
                dungeonInstanceDao = database.dungeonInstanceDao(),
                tacticalIntelligenceEngine = tacticalIntelligence,
                deploymentEngine = deploymentEngine,
                mutex = mutex
            )
        }

    val analyticsEngine: ShadowTacticalAnalyticsEngine =
        tacticalAnalyticsEngine ?: ShadowTacticalAnalyticsEngine(
            shadowDao = shadowDao,
            deploymentDao = deploymentDao,
            formationDao = formationDao,
            shadowTacticalDao = shadowTacticalDao ?: database!!.shadowTacticalDao(),
            tacticalIntelligenceEngine = tacticalIntelligence
        )

    val bondEngine: ShadowBondEngine = shadowBondEngine ?: run {
        val dao = shadowBondDao ?: database?.shadowBondDao()
        if (dao != null) {
            ShadowBondEngine(dao, shadowDao, database, mutex)
        } else {
            // In unit test stubs where database is mocked without bond dao
            ShadowBondEngine(object : ShadowBondDao {
                override suspend fun getBondProfile(shadowId: String, accountOwnerId: String) = null
                override fun observeBondProfile(shadowId: String, accountOwnerId: String) = kotlinx.coroutines.flow.flowOf(null)
                override suspend fun upsertBondProfile(entity: com.example.data.local.entity.ShadowBondProfileEntity) = 1L
                override fun observeAllBondProfilesForOwner(accountOwnerId: String): kotlinx.coroutines.flow.Flow<List<com.example.data.local.entity.ShadowBondProfileEntity>> = kotlinx.coroutines.flow.flowOf(emptyList())
                override suspend fun getAllBondProfilesForOwner(accountOwnerId: String) = emptyList<com.example.data.local.entity.ShadowBondProfileEntity>()
                override suspend fun insertBondEvent(entity: com.example.data.local.entity.ShadowBondEventEntity) = 1L
                override suspend fun getBondEventByIdempotencyKey(idempotencyKey: String) = null
                override suspend fun getBondEventsForShadow(shadowId: String, accountOwnerId: String) = emptyList<com.example.data.local.entity.ShadowBondEventEntity>()
                override fun observeBondEventsForShadow(shadowId: String, accountOwnerId: String): kotlinx.coroutines.flow.Flow<List<com.example.data.local.entity.ShadowBondEventEntity>> = kotlinx.coroutines.flow.flowOf(emptyList())
                override suspend fun insertBondMilestone(entity: com.example.data.local.entity.ShadowBondMilestoneEntity) = 1L
                override suspend fun getBondMilestonesForShadow(shadowId: String, accountOwnerId: String) = emptyList<com.example.data.local.entity.ShadowBondMilestoneEntity>()
                override fun observeBondMilestonesForShadow(shadowId: String, accountOwnerId: String): kotlinx.coroutines.flow.Flow<List<com.example.data.local.entity.ShadowBondMilestoneEntity>> = kotlinx.coroutines.flow.flowOf(emptyList())
                override suspend fun hasMilestone(shadowId: String, accountOwnerId: String, milestoneTypeName: String) = false
                override fun observeAllMilestonesForOwner(accountOwnerId: String): kotlinx.coroutines.flow.Flow<List<com.example.data.local.entity.ShadowBondMilestoneEntity>> = kotlinx.coroutines.flow.flowOf(emptyList())
                override suspend fun getRecentMilestonesForOwner(accountOwnerId: String, limit: Int) = emptyList<com.example.data.local.entity.ShadowBondMilestoneEntity>()
                override fun observeRecentMilestonesForOwner(accountOwnerId: String, limit: Int): kotlinx.coroutines.flow.Flow<List<com.example.data.local.entity.ShadowBondMilestoneEntity>> = kotlinx.coroutines.flow.flowOf(emptyList())
            }, shadowDao, database, mutex)
        }
    }

    val dominionDaoInstance: ShadowDominionDao = shadowDominionDao ?: database?.shadowDominionDao()
        ?: run {
            object : ShadowDominionDao {
                override fun observeDominionForOwner(accountOwnerId: String) = kotlinx.coroutines.flow.flowOf(null)
                override suspend fun getDominionForOwnerOnce(accountOwnerId: String) = null
                override suspend fun upsertDominion(entity: com.example.data.local.entity.ShadowDominionEntity) = 1L
                override suspend fun updateDominionLevelAndStage(accountOwnerId: String, level: Int, stageName: String, citadelLevel: Int, updatedAtMs: Long) = 1
                override suspend fun updateTerritoryCount(accountOwnerId: String, count: Int, updatedAtMs: Long) = 1
                override suspend fun updateInfrastructureCount(accountOwnerId: String, count: Int, updatedAtMs: Long) = 1
                override fun observeTerritoriesForOwner(accountOwnerId: String) = kotlinx.coroutines.flow.flowOf(emptyList<com.example.data.local.entity.ShadowTerritoryEntity>())
                override suspend fun getAllTerritoriesForOwnerOnce(accountOwnerId: String) = emptyList<com.example.data.local.entity.ShadowTerritoryEntity>()
                override suspend fun getTerritoryById(territoryId: String, accountOwnerId: String) = null
                override suspend fun insertTerritory(entity: com.example.data.local.entity.ShadowTerritoryEntity) = 1L
                override suspend fun insertTerritories(entities: List<com.example.data.local.entity.ShadowTerritoryEntity>) = Unit
                override suspend fun updateTerritory(entity: com.example.data.local.entity.ShadowTerritoryEntity) = 1
                override suspend fun updateTerritoryExplorationState(territoryId: String, accountOwnerId: String, stateName: String, progressPercent: Int, endsAtMs: Long?) = 1
                override suspend fun claimTerritory(territoryId: String, accountOwnerId: String, claimedAtMs: Long) = 1
                override suspend fun assignSquadToTerritory(territoryId: String, accountOwnerId: String, squadId: String?) = 1
                override fun observeInfrastructuresForOwner(accountOwnerId: String) = kotlinx.coroutines.flow.flowOf(emptyList<com.example.data.local.entity.ShadowInfrastructureEntity>())
                override fun observeInfrastructuresForTerritory(territoryId: String, accountOwnerId: String) = kotlinx.coroutines.flow.flowOf(emptyList<com.example.data.local.entity.ShadowInfrastructureEntity>())
                override suspend fun getAllInfrastructuresForOwnerOnce(accountOwnerId: String) = emptyList<com.example.data.local.entity.ShadowInfrastructureEntity>()
                override suspend fun getInfrastructuresForTerritoryOnce(territoryId: String, accountOwnerId: String) = emptyList<com.example.data.local.entity.ShadowInfrastructureEntity>()
                override suspend fun getInfrastructureById(structureId: String, accountOwnerId: String) = null
                override suspend fun insertInfrastructure(entity: com.example.data.local.entity.ShadowInfrastructureEntity) = 1L
                override suspend fun updateInfrastructure(entity: com.example.data.local.entity.ShadowInfrastructureEntity) = 1
                override suspend fun deleteInfrastructure(structureId: String, accountOwnerId: String) = 1
                override fun observeGarrisonsForOwner(accountOwnerId: String) = kotlinx.coroutines.flow.flowOf(emptyList<com.example.data.local.entity.ShadowGarrisonEntity>())
                override fun observeGarrisonForTerritory(territoryId: String, accountOwnerId: String) = kotlinx.coroutines.flow.flowOf(null)
                override suspend fun getAllGarrisonsForOwnerOnce(accountOwnerId: String) = emptyList<com.example.data.local.entity.ShadowGarrisonEntity>()
                override suspend fun getGarrisonForTerritoryOnce(territoryId: String, accountOwnerId: String) = null
                override suspend fun getGarrisonById(garrisonId: String, accountOwnerId: String) = null
                override suspend fun upsertGarrison(entity: com.example.data.local.entity.ShadowGarrisonEntity) = 1L
                override suspend fun deleteGarrison(garrisonId: String, accountOwnerId: String) = 1
                override fun observeResearchForOwner(accountOwnerId: String) = kotlinx.coroutines.flow.flowOf(emptyList<com.example.data.local.entity.ShadowDominionResearchEntity>())
                override suspend fun getAllResearchForOwnerOnce(accountOwnerId: String) = emptyList<com.example.data.local.entity.ShadowDominionResearchEntity>()
                override suspend fun getResearchByProjectKey(accountOwnerId: String, projectKey: String) = null
                override suspend fun upsertResearch(entity: com.example.data.local.entity.ShadowDominionResearchEntity) = 1L
                override fun observeRecentActivitiesForOwner(accountOwnerId: String, limit: Int) = kotlinx.coroutines.flow.flowOf(emptyList<com.example.data.local.entity.ShadowDominionActivityEntity>())
                override suspend fun getAllActivitiesForOwnerOnce(accountOwnerId: String) = emptyList<com.example.data.local.entity.ShadowDominionActivityEntity>()
                override suspend fun insertActivity(entity: com.example.data.local.entity.ShadowDominionActivityEntity) = 1L
            }
        }

    val dominionManager: ShadowDominionEngine = dominionEngine ?: ShadowDominionEngine(
        database = database,
        shadowDominionDao = dominionDaoInstance,
        shadowResourceDao = resourceDao,
        mutex = mutex
    )

    val territoryManager: ShadowTerritoryEngine = territoryEngine ?: ShadowTerritoryEngine(
        database = database,
        shadowDominionDao = dominionDaoInstance,
        shadowSquadEngine = squadManager,
        shadowDeploymentEngine = deploymentEngine,
        shadowDominionEngine = dominionManager,
        mutex = mutex
    )

    val infrastructureManager: ShadowInfrastructureEngine = infrastructureEngine ?: ShadowInfrastructureEngine(
        database = database,
        shadowDominionDao = dominionDaoInstance,
        shadowResourceDao = resourceDao,
        shadowDominionEngine = dominionManager,
        mutex = mutex
    )

    val garrisonManager: ShadowGarrisonEngine = garrisonEngine ?: ShadowGarrisonEngine(
        database = database,
        shadowDominionDao = dominionDaoInstance,
        shadowSquadEngine = squadManager,
        shadowDeploymentEngine = deploymentEngine,
        tacticalIntelligenceEngine = tacticalIntelligence,
        mutex = mutex
    )

    val researchManager: ShadowDominionResearchEngine = researchEngine ?: ShadowDominionResearchEngine(
        database = database,
        shadowDominionDao = dominionDaoInstance,
        shadowResourceDao = resourceDao,
        shadowDominionEngine = dominionManager,
        mutex = mutex
    )

    val dominionAdvisor: ShadowDominionAiAdvisor = dominionAiAdvisor ?: ShadowDominionAiAdvisor()

    private suspend fun <T> withLockedTransaction(block: suspend () -> T): T {
        return mutex.withLock {
            val db = database
            if (db != null) {
                db.withTransaction { block() }
            } else {
                block()
            }
        }
    }

    // -------------------------------------------------------------------------
    // ACCOUNT-SCOPED REACTIVE STREAMS
    // -------------------------------------------------------------------------

    fun observeMonarchProfile(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Flow<ShadowMonarchProfile?> {
        return monarchProfileDao.observeProfileForOwner(accountOwnerId).map { it?.toDomain() }
    }

    fun observeActiveShadows(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Flow<List<Shadow>> {
        return shadowDao.observeActiveShadowsForOwner(accountOwnerId).map { list -> list.map { it.toDomain() } }
    }

    fun observeAllShadows(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Flow<List<Shadow>> {
        return shadowDao.observeAllShadowsForOwner(accountOwnerId).map { list -> list.map { it.toDomain() } }
    }

    fun observeExtractionHistory(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Flow<List<ShadowExtractionEvent>> {
        return extractionDao.observeExtractionsForOwner(accountOwnerId).map { list -> list.map { it.toDomain() } }
    }

    fun observeActiveDeployments(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Flow<List<ShadowDeploymentRecord>> {
        return deploymentDao.observeActiveDeploymentsForOwner(accountOwnerId).map { list -> list.map { it.toDomain() } }
    }

    fun observeFormations(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Flow<List<ShadowFormation>> {
        return formationDao.observeFormationsForOwner(accountOwnerId).map { list -> list.map { it.toDomain() } }
    }

    fun observeActiveFormation(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Flow<ShadowFormation?> {
        return formationDao.observeActiveFormationForOwner(accountOwnerId).map { it?.toDomain() }
    }

    fun observeQuestProgress(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Flow<List<Pair<ShadowQuestDefinition, ShadowQuestProgress?>>> {
        return questProgressDao.observeQuestProgressForOwner(accountOwnerId).map { progressList ->
            val progressMap = progressList.associate { it.questId to it.toDomain() }
            ShadowQuestCatalog.quests.map { questDef ->
                Pair(questDef, progressMap[questDef.questId])
            }
        }
    }

    fun observeResourceTransactions(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Flow<List<ShadowResourceTransaction>> {
        return resourceDao.observeTransactionsForOwner(accountOwnerId).map { list -> list.map { it.toDomain() } }
    }

    fun observeMonarchSkillUnlocks(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Flow<List<MonarchSkillDefinition>> {
        return monarchSkillUnlockDao.observeMonarchSkillUnlocksForOwner(accountOwnerId).map { unlocks ->
            val unlockedIds = unlocks.map { it.skillId }.toSet()
            MonarchSkillCatalog.skills.filter { it.skillId in unlockedIds }
        }
    }

    // -------------------------------------------------------------------------
    // SHADOW ARMY ANALYTICS (Account-Isolated)
    // -------------------------------------------------------------------------

    suspend fun buildShadowArmyAnalytics(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): ShadowArmyAnalytics {
        val owner = accountOwnerId.trim().also {
            require(it.isNotBlank()) { "accountOwnerId must not be blank" }
        }
        val allShadows = shadowDao.getAllShadowsForOwnerOnce(owner).map { it.toDomain() }
        val activeShadows = allShadows.filter { !it.isRetired }
        val retiredShadows = allShadows.filter { it.isRetired }

        val distributionByClass = ShadowClass.entries.associateWith { cls ->
            activeShadows.count { it.shadowClass == cls }
        }
        val distributionByRarity = ShadowRarity.entries.associateWith { rarity ->
            activeShadows.count { it.rarity == rarity }
        }
        val distributionByRank = ShadowRank.entries.associateWith { rank ->
            activeShadows.count { it.currentRank == rank }
        }

        val highestLevel = activeShadows.maxByOrNull { it.level }
        val highestRank = activeShadows.maxByOrNull { it.currentRank.order }

        val totalEvolutions = allShadows.sumOf { shadow ->
            try {
                kotlinx.serialization.json.Json.decodeFromString<List<String>>(shadow.evolutionHistoryJson).size
            } catch (_: Exception) { 0 }
        }

        val deployedShadows = deploymentDao.getActiveDeploymentsForOwnerOnce(owner).map { it.toDomain() }
        val deployedShadowClasses = deployedShadows.mapNotNull { deployment ->
            activeShadows.find { it.shadowId == deployment.shadowId }?.shadowClass
        }
        val mostDeployedClass = deployedShadowClasses.groupBy { it }.maxByOrNull { it.value.size }?.key

        val resourceBalances = ShadowResourceType.entries.associateWith { type ->
            resourceDao.getCurrentBalanceForOwner(owner, type.name)
        }

        val profile = monarchProgressionEngine.getOrCreateProfile(owner)

        return ShadowArmyAnalytics(
            totalRecruited = allShadows.size,
            activeShadows = activeShadows.size,
            retiredShadows = retiredShadows.size,
            distributionByClass = distributionByClass,
            distributionByRarity = distributionByRarity,
            distributionByRank = distributionByRank,
            highestLevelShadow = highestLevel,
            highestRankShadow = highestRank,
            totalEvolutions = totalEvolutions,
            mostDeployedClass = mostDeployedClass,
            monarchProfile = profile,
            resourceBalances = resourceBalances
        )
    }

    // -------------------------------------------------------------------------
    // RESOURCE MANAGEMENT (Account-Isolated, Atomic, Non-Negative & Idempotent)
    // -------------------------------------------------------------------------

    suspend fun getCurrentBalance(
        resourceType: ShadowResourceType,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Int {
        return resourceDao.getCurrentBalanceForOwner(accountOwnerId, resourceType.name)
    }

    /**
     * Awards a shadow resource atomically and idempotently under accountOwnerId.
     * Duplicate sourceEventId replays return the current balance without double-crediting.
     */
    suspend fun awardResource(
        resourceType: ShadowResourceType,
        amount: Int,
        sourceEventId: String,
        sourceDescription: String,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Result<Int> {
        return runCatching {
            val owner = accountOwnerId.trim()
            val eventKey = sourceEventId.trim()
            require(owner.isNotBlank()) { "accountOwnerId must not be blank" }
            require(eventKey.isNotBlank()) { "sourceEventId must not be blank" }
            require(amount > 0) { "Award amount must be positive" }

            withLockedTransaction {
                monarchProgressionEngine.getOrCreateProfileLocked(owner)

                val existingTx = resourceDao.getTransactionBySourceEventAndTypeForOwner(
                    accountOwnerId = owner,
                    sourceEventId = eventKey,
                    typeName = resourceType.name,
                    transactionTypeName = ShadowResourceTransactionType.EARNED.name
                )
                if (existingTx != null) {
                    return@withLockedTransaction resourceDao.getCurrentBalanceForOwner(owner, resourceType.name)
                }

                val currentBalance = resourceDao.getCurrentBalanceForOwner(owner, resourceType.name)
                val newBalance = currentBalance + amount

                val inserted = resourceDao.insertTransaction(
                    ShadowResourceTransactionEntity(
                        transactionId = UUID.randomUUID().toString(),
                        accountOwnerId = owner,
                        resourceTypeName = resourceType.name,
                        amount = amount,
                        transactionTypeName = ShadowResourceTransactionType.EARNED.name,
                        sourceEventId = eventKey,
                        sourceDescription = sourceDescription,
                        balanceAfter = newBalance,
                        timestampEpochMs = System.currentTimeMillis()
                    )
                )
                if (inserted == -1L) {
                    return@withLockedTransaction resourceDao.getCurrentBalanceForOwner(owner, resourceType.name)
                }
                newBalance
            }
        }
    }

    /**
     * Consumes a shadow resource atomically with strict non-negative balance invariant
     * and per-account idempotency on transactionEventId.
     */
    suspend fun consumeResource(
        resourceType: ShadowResourceType,
        amount: Int,
        purpose: String,
        transactionEventId: String = "consume_${UUID.randomUUID()}",
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Result<Int> {
        return runCatching {
            val owner = accountOwnerId.trim()
            val txKey = transactionEventId.trim()
            require(owner.isNotBlank()) { "accountOwnerId must not be blank" }
            require(txKey.isNotBlank()) { "transactionEventId must not be blank" }
            require(purpose.isNotBlank()) { "purpose must not be blank" }
            require(amount > 0) { "Consume amount must be positive" }

            withLockedTransaction {
                monarchProgressionEngine.getOrCreateProfileLocked(owner)

                val existingTx = resourceDao.getTransactionBySourceEventAndTypeForOwner(
                    accountOwnerId = owner,
                    sourceEventId = txKey,
                    typeName = resourceType.name,
                    transactionTypeName = ShadowResourceTransactionType.CONSUMED.name
                )
                if (existingTx != null) {
                    throw IllegalStateException("Transaction already processed: $txKey")
                }

                val currentBalance = resourceDao.getCurrentBalanceForOwner(owner, resourceType.name)
                if (currentBalance < amount) {
                    throw IllegalStateException("Insufficient ${resourceType.label}: $currentBalance < $amount")
                }

                val newBalance = currentBalance - amount
                val inserted = resourceDao.insertTransaction(
                    ShadowResourceTransactionEntity(
                        transactionId = UUID.randomUUID().toString(),
                        accountOwnerId = owner,
                        resourceTypeName = resourceType.name,
                        amount = -amount,
                        transactionTypeName = ShadowResourceTransactionType.CONSUMED.name,
                        sourceEventId = txKey,
                        sourceDescription = purpose,
                        balanceAfter = newBalance,
                        timestampEpochMs = System.currentTimeMillis()
                    )
                )
                if (inserted == -1L) {
                    throw IllegalStateException("Duplicate resource transaction rejected: $txKey")
                }
                newBalance
            }
        }
    }

    // -------------------------------------------------------------------------
    // CONVENIENCE QUERIES (Account-Isolated)
    // -------------------------------------------------------------------------

    suspend fun getShadowById(
        shadowId: String,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Shadow? {
        return shadowDao.getShadowByIdForOwner(shadowId, accountOwnerId)?.toDomain()
    }

    fun observeShadowById(
        shadowId: String,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Flow<Shadow?> {
        return shadowDao.observeAllShadowsForOwner(accountOwnerId).map { list ->
            list.find { it.shadowId == shadowId }?.toDomain()
        }
    }

    suspend fun getMonarchProfile(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): ShadowMonarchProfile {
        return monarchProgressionEngine.getOrCreateProfile(accountOwnerId)
    }

    fun getShadowSkillCatalog(): List<ShadowSkillDefinition> = ShadowSkillCatalog.skills

    fun getMonarchSkillCatalog(): List<MonarchSkillDefinition> = MonarchSkillCatalog.skills

    suspend fun getUnlockedMonarchSkillIds(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): List<String> {
        return monarchSkillUnlockDao.getAllMonarchSkillUnlocksForOwnerOnce(accountOwnerId).map { it.skillId }
    }

    suspend fun syncShadowQuestProgress(
        dungeonCompletions: Int = 0,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ) {
        val owner = accountOwnerId.trim()
        if (owner.isBlank()) return
        withLockedTransaction {
            val allShadows = shadowDao.getAllShadowsForOwnerOnce(owner).map { it.toDomain() }
            val activeShadows = allShadows.filter { !it.isRetired }
            val deployedCount = deploymentDao.getActiveDeploymentsForOwnerOnce(owner).size
            val evolutionCount = allShadows.sumOf { shadow ->
                try {
                    kotlinx.serialization.json.Json.decodeFromString<List<String>>(shadow.evolutionHistoryJson).size
                } catch (_: Exception) { 0 }
            }
            val aRankShadows = activeShadows.count { it.currentRank.order >= ShadowRank.A_RANK.order }
            val commanderShadows = activeShadows.count {
                it.evolutionStage.order >= ShadowEvolutionStage.COMMANDER.order
            }
            monarchProgressionEngine.checkAndUpdateQuestProgressLocked(
                accountOwnerId = owner,
                shadowCount = activeShadows.size,
                deployedCount = deployedCount,
                evolutionCount = evolutionCount,
                aRankShadows = aRankShadows,
                commanderShadows = commanderShadows,
                dungeonCompletions = dungeonCompletions
            )
        }
    }

    // -------------------------------------------------------------------------
    // PHASE 6.2 — SHADOW RECRUITMENT & COLLECTION MANAGEMENT
    // -------------------------------------------------------------------------

    suspend fun getResonanceRecordsOnce(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): List<com.example.domain.model.ShadowResonanceRecord> {
        val db = database ?: return emptyList()
        return db.shadowResonanceDao().getResonanceRecordsForOwnerOnce(accountOwnerId).map { it.toDomain() }
    }

    suspend fun getEvidenceClaimsOnce(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): List<com.example.domain.model.ShadowEvidenceClaim> {
        val db = database ?: return emptyList()
        return db.shadowEvidenceClaimDao().getClaimsForOwnerOnce(accountOwnerId).map { it.toDomain() }
    }

    suspend fun getCollectionOverview(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): com.example.domain.model.ShadowCollectionOverview {
        val owner = accountOwnerId.trim()
        val allShadows = shadowDao.getAllShadowsForOwnerOnce(owner).map { it.toDomain() }
        val activeShadows = allShadows.filter { !it.isRetired }

        val catalog = com.example.domain.model.ShadowArchetypeCatalog.archetypes
        val totalCatalogCount = catalog.size

        // Match owned shadows to archetypes in catalog by name
        val collectedArchetypeIds = activeShadows.mapNotNull { shadow ->
            com.example.domain.model.ShadowArchetypeCatalog.findByName(shadow.name)?.archetypeId
        }.toSet()

        val completionPct = if (totalCatalogCount > 0) {
            (collectedArchetypeIds.size.toFloat() / totalCatalogCount.toFloat()) * 100f
        } else 0f

        val classDist = ShadowClass.entries.associateWith { cls ->
            activeShadows.count { it.shadowClass == cls }
        }
        val rarityDist = ShadowRarity.entries.associateWith { r ->
            activeShadows.count { it.rarity == r }
        }
        val rankDist = ShadowRank.entries.associateWith { rk ->
            activeShadows.count { it.currentRank == rk }
        }

        val resonance = getResonanceRecordsOnce(owner)
        val duplicateCount = extractionDao.getAllExtractionsForOwnerOnce(owner)
            .count { it.outcomeName == ShadowExtractionOutcome.DUPLICATE_RESOLVED.name }

        return com.example.domain.model.ShadowCollectionOverview(
            totalOwned = activeShadows.size,
            uniqueArchetypesCollected = collectedArchetypeIds.size,
            totalArchetypesInCatalog = totalCatalogCount,
            completionPercentage = completionPct,
            classDistribution = classDist,
            rarityDistribution = rarityDist,
            rankDistribution = rankDist,
            recentRecruits = activeShadows.sortedByDescending { it.extractedAtEpochMs }.take(5),
            duplicateResonanceCount = duplicateCount,
            resonanceRecords = resonance
        )
    }

    fun filterAndSortShadows(
        shadows: List<Shadow>,
        classFilter: ShadowClass? = null,
        rarityFilter: ShadowRarity? = null,
        rankFilter: ShadowRank? = null,
        stageFilter: ShadowEvolutionStage? = null,
        deploymentFilter: com.example.domain.model.ShadowDeploymentStatus? = null,
        sortOption: com.example.domain.model.ShadowSortOption = com.example.domain.model.ShadowSortOption.RECENTLY_RECRUITED
    ): List<Shadow> {
        var result = shadows
        if (classFilter != null) result = result.filter { it.shadowClass == classFilter }
        if (rarityFilter != null) result = result.filter { it.rarity == rarityFilter }
        if (rankFilter != null) result = result.filter { it.currentRank == rankFilter }
        if (stageFilter != null) result = result.filter { it.evolutionStage == stageFilter }
        if (deploymentFilter != null) result = result.filter { it.deploymentStatus == deploymentFilter }

        return when (sortOption) {
            com.example.domain.model.ShadowSortOption.RECENTLY_RECRUITED -> result.sortedByDescending { it.extractedAtEpochMs }
            com.example.domain.model.ShadowSortOption.RARITY -> result.sortedByDescending { it.rarity.order }
            com.example.domain.model.ShadowSortOption.LEVEL -> result.sortedByDescending { it.level }
            com.example.domain.model.ShadowSortOption.RANK -> result.sortedByDescending { it.currentRank.order }
            com.example.domain.model.ShadowSortOption.NAME -> result.sortedBy { it.name }
        }
    }

    // -------------------------------------------------------------------------
    // PHASE 6.3 — SHADOW GROWTH & EVOLUTION MANAGEMENT
    // -------------------------------------------------------------------------

    suspend fun evaluateEvolution(
        shadowId: String,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): com.example.domain.model.ShadowEvolutionEvaluation {
        return evolutionEngine.evaluateEvolution(shadowId, accountOwnerId)
    }

    suspend fun confirmEvolution(
        shadowId: String,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Result<Shadow> {
        return evolutionEngine.confirmEvolution(shadowId, accountOwnerId)
    }

    suspend fun awardShadowXpFromEvent(
        shadowId: String,
        sourceType: com.example.domain.model.ShadowExtractionSourceType,
        eventId: String,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Boolean {
        return evolutionEngine.awardShadowXpFromEvent(shadowId, sourceType, eventId, accountOwnerId)
    }

    suspend fun awardShadowXpFromDungeon(
        shadowId: String,
        dungeonId: String,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Boolean {
        return evolutionEngine.awardShadowXpFromDungeon(shadowId, dungeonId, accountOwnerId)
    }

    suspend fun awardShadowXpFromBoss(
        shadowId: String,
        bossId: String,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Boolean {
        return evolutionEngine.awardShadowXpFromBoss(shadowId, bossId, accountOwnerId)
    }

    suspend fun awardShadowXpFromQuest(
        shadowId: String,
        questId: String,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Boolean {
        return evolutionEngine.awardShadowXpFromQuest(shadowId, questId, accountOwnerId)
    }

    suspend fun getShadowGrowthOverview(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): com.example.domain.model.ShadowGrowthOverview {
        val owner = accountOwnerId.trim()
        val allShadows = shadowDao.getAllShadowsForOwnerOnce(owner).map { it.toDomain() }
        val activeShadows = allShadows.filter { !it.isRetired }

        val stageDist = ShadowEvolutionStage.entries.associateWith { stage ->
            activeShadows.count { it.evolutionStage == stage }
        }

        val avgLevel = if (activeShadows.isNotEmpty()) {
            activeShadows.map { it.level }.average().toFloat()
        } else 0f

        val highestLevel = activeShadows.maxByOrNull { it.level }

        var readyCount = 0
        var eligibleLevelCount = 0
        for (shadow in activeShadows) {
            val eval = evolutionEngine.evaluateEvolution(shadow.shadowId, owner)
            if (eval.isEligible) readyCount++
            val stageCap = com.example.domain.model.ShadowGrowthConfig.getStageLevelCap(shadow.evolutionStage)
            if (shadow.level < stageCap) eligibleLevelCount++
        }

        return com.example.domain.model.ShadowGrowthOverview(
            totalShadows = activeShadows.size,
            readyToEvolveCount = readyCount,
            eligibleToLevelUpCount = eligibleLevelCount,
            averageLevel = avgLevel,
            highestLevelShadow = highestLevel,
            stageDistribution = stageDist
        )
    }

    // -------------------------------------------------------------------------
    // SHADOW BOND & LOYALTY (Milestone 12 - Phase 6.4)
    // -------------------------------------------------------------------------

    fun observeShadowBondProfile(
        shadowId: String,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Flow<ShadowBondProfile?> {
        return bondEngine.observeBondProfile(shadowId, accountOwnerId)
    }

    suspend fun getOrCreateShadowBondProfile(
        shadowId: String,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): ShadowBondProfile {
        return bondEngine.getOrCreateBondProfile(shadowId, accountOwnerId)
    }

    fun observeShadowBondEvents(
        shadowId: String,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Flow<List<ShadowBondEvent>> {
        return bondEngine.observeBondEventsForShadow(shadowId, accountOwnerId)
    }

    fun observeShadowBondMilestones(
        shadowId: String,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Flow<List<ShadowBondMilestone>> {
        return bondEngine.observeBondMilestonesForShadow(shadowId, accountOwnerId)
    }

    suspend fun recordShadowBondActivity(
        shadowId: String,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
        activityType: ShadowBondActivityType,
        sourceEventId: String,
        description: String? = null
    ): Result<ShadowBondProfile> {
        return bondEngine.recordBondActivity(shadowId, accountOwnerId, activityType, sourceEventId, description)
    }

    fun observeArmyBondOverview(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Flow<ShadowArmyBondOverview> {
        return bondEngine.observeArmyBondOverview(accountOwnerId)
    }

    // -------------------------------------------------------------------------
    // SHADOW ARMY INTELLIGENCE & TACTICAL COMMAND (Milestone 12 - Phase 6.5)
    // -------------------------------------------------------------------------

    fun observeSquadDetailsForOwner(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Flow<List<SquadDetail>> {
        val dao = shadowTacticalDao ?: database?.shadowTacticalDao()
        if (dao == null) return kotlinx.coroutines.flow.flowOf(emptyList<SquadDetail>())

        return dao.observeSquadsForOwner(accountOwnerId).map { squadEntities ->
            val allShadows = shadowDao.getAllShadowsForOwnerOnce(accountOwnerId).map { it.toDomain() }
            val shadowMap = allShadows.associateBy { it.shadowId }

            squadEntities.map { entity ->
                val memberEntities = dao.getMembersForSquadOnce(entity.squadId)
                val memberIds = memberEntities.map { it.shadowId }
                val squad = entity.toDomain(memberIds)
                val leader = squad.leaderShadowId?.let { shadowMap[it] }

                val memberDetails = memberEntities.mapNotNull { m ->
                    val shadow = shadowMap[m.shadowId] ?: return@mapNotNull null
                    val profile = tacticalIntelligence.evaluateTacticalProfile(shadow)
                    val assignedRole = runCatching { ShadowTacticalRole.valueOf(m.assignedTacticalRoleName) }
                        .getOrDefault(profile.primaryRole)

                    SquadMemberDetail(
                        shadow = shadow,
                        tacticalProfile = profile,
                        assignedRole = assignedRole,
                        positionIndex = m.positionIndex,
                        isLeader = shadow.shadowId == squad.leaderShadowId
                    )
                }

                val squadShadows = memberDetails.map { it.shadow }
                val synergy = tacticalIntelligence.evaluateSquadSynergy(squadShadows, squad.category)

                // Squad average capability vector
                val avgCaps = if (memberDetails.isEmpty()) TacticalCapabilityVector.ZERO else {
                    val count = memberDetails.size
                    TacticalCapabilityVector(
                        offensiveCapability = memberDetails.sumOf { it.tacticalProfile.capabilities.offensiveCapability } / count,
                        defensiveCapability = memberDetails.sumOf { it.tacticalProfile.capabilities.defensiveCapability } / count,
                        mobility = memberDetails.sumOf { it.tacticalProfile.capabilities.mobility } / count,
                        rangedCapability = memberDetails.sumOf { it.tacticalProfile.capabilities.rangedCapability } / count,
                        supportCapability = memberDetails.sumOf { it.tacticalProfile.capabilities.supportCapability } / count,
                        strategicUtility = memberDetails.sumOf { it.tacticalProfile.capabilities.strategicUtility } / count,
                        survivability = memberDetails.sumOf { it.tacticalProfile.capabilities.survivability } / count
                    )
                }

                val readiness = if (memberDetails.isEmpty()) 0 else {
                    val availableCount = memberDetails.count { it.tacticalProfile.isAvailable }
                    ((availableCount.toFloat() / memberDetails.size) * synergy.synergyScore).toInt().coerceIn(0, 100)
                }

                SquadDetail(
                    squad = squad,
                    leader = leader,
                    members = memberDetails,
                    capabilityVector = avgCaps,
                    synergyReport = synergy,
                    readinessScore = readiness
                )
            }
        }
    }

    fun observeActiveSquadDetailForOwner(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Flow<SquadDetail?> {
        return observeSquadDetailsForOwner(accountOwnerId).map { squads ->
            squads.find { it.squad.isActive }
        }
    }

    suspend fun createSquad(
        name: String,
        category: ShadowSquadCategory,
        memberShadowIds: List<String> = emptyList(),
        leaderShadowId: String? = null,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Result<ShadowSquad> {
        return squadManager.createSquad(name, category, memberShadowIds, leaderShadowId, accountOwnerId)
    }

    suspend fun updateSquad(
        squadId: String,
        name: String,
        category: ShadowSquadCategory,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Result<ShadowSquad> {
        return squadManager.updateSquad(squadId, name, category, accountOwnerId)
    }

    suspend fun addMemberToSquad(
        squadId: String,
        shadowId: String,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Result<ShadowSquad> {
        return squadManager.addMemberToSquad(squadId, shadowId, accountOwnerId)
    }

    suspend fun removeMemberFromSquad(
        squadId: String,
        shadowId: String,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Result<ShadowSquad> {
        return squadManager.removeMemberFromSquad(squadId, shadowId, accountOwnerId)
    }

    suspend fun setSquadLeader(
        squadId: String,
        leaderShadowId: String,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Result<ShadowSquad> {
        return squadManager.setSquadLeader(squadId, leaderShadowId, accountOwnerId)
    }

    suspend fun setActiveSquad(
        squadId: String,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Result<Unit> {
        return squadManager.setActiveSquad(squadId, accountOwnerId)
    }

    suspend fun duplicateSquad(
        squadId: String,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Result<ShadowSquad> {
        return squadManager.duplicateSquad(squadId, accountOwnerId)
    }

    suspend fun deleteSquad(
        squadId: String,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Result<Unit> {
        return squadManager.deleteSquad(squadId, accountOwnerId)
    }

    fun getTacticalProfile(shadow: Shadow): ShadowTacticalProfile {
        return tacticalIntelligence.evaluateTacticalProfile(shadow)
    }

    fun observeActiveFormationAssessment(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Flow<FormationTacticalAssessment?> {
        return formationDao.observeActiveFormationForOwner(accountOwnerId).map { entity ->
            val formation = entity?.toDomain() ?: return@map null
            val shadows = shadowDao.getAllShadowsForOwnerOnce(accountOwnerId).map { it.toDomain() }
            tacticalIntelligence.evaluateFormation(formation, shadows)
        }
    }

    suspend fun getAvailableMissions(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): List<MissionTacticalRequirement> {
        return missionManager.getAvailableMissions(accountOwnerId)
    }

    suspend fun evaluateMissionSuitability(
        mission: MissionTacticalRequirement,
        squad: ShadowSquad?,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): MissionSuitabilityEvaluation {
        val roster = shadowDao.getAllShadowsForOwnerOnce(accountOwnerId).map { it.toDomain() }
        return missionManager.evaluateMissionSuitability(mission, squad, roster)
    }

    suspend fun confirmAndDeployMissionAssignment(
        missionId: String,
        missionType: MissionType,
        squadId: String?,
        shadowIdsToDeploy: List<String>,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Result<com.example.data.local.entity.ShadowMissionAssignmentEntity> {
        return missionManager.confirmAndDeployMissionAssignment(missionId, missionType, squadId, shadowIdsToDeploy, accountOwnerId)
    }

    suspend fun getTacticalAnalytics(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): ShadowTacticalAnalytics {
        return analyticsEngine.generateAnalytics(accountOwnerId)
    }

    suspend fun getTacticalAlerts(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): List<TacticalAlert> {
        return analyticsEngine.generateTacticalAlerts(accountOwnerId)
    }

    // -------------------------------------------------------------------------
    // PHASE 6.6 — DOMINION REPOSITORY APIS
    // -------------------------------------------------------------------------

    fun observeDominionForOwner(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Flow<ShadowDominion?> {
        return dominionDaoInstance.observeDominionForOwner(accountOwnerId).map { it?.toDomain() }
    }

    fun observeTerritoriesForOwner(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Flow<List<ShadowTerritory>> {
        return dominionDaoInstance.observeTerritoriesForOwner(accountOwnerId).map { list -> list.map { it.toDomain() } }
    }

    fun observeInfrastructuresForOwner(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Flow<List<ShadowInfrastructure>> {
        return dominionDaoInstance.observeInfrastructuresForOwner(accountOwnerId).map { list -> list.map { it.toDomain() } }
    }

    fun observeGarrisonsForOwner(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Flow<List<ShadowGarrison>> {
        return dominionDaoInstance.observeGarrisonsForOwner(accountOwnerId).map { list -> list.map { it.toDomain() } }
    }

    fun observeResearchForOwner(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Flow<List<ShadowDominionResearch>> {
        return dominionDaoInstance.observeResearchForOwner(accountOwnerId).map { list -> list.map { it.toDomain() } }
    }

    fun observeDominionActivities(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
        limit: Int = 50
    ): Flow<List<ShadowDominionActivity>> {
        return dominionDaoInstance.observeRecentActivitiesForOwner(accountOwnerId, limit).map { list -> list.map { it.toDomain() } }
    }

    suspend fun getDominionOverview(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): DominionOverview? {
        val dominion = dominionManager.getDominion(accountOwnerId) ?: return null
        val territories = dominionDaoInstance.getAllTerritoriesForOwnerOnce(accountOwnerId).map { it.toDomain() }
        val structures = dominionDaoInstance.getAllInfrastructuresForOwnerOnce(accountOwnerId).map { it.toDomain() }
        val garrisons = dominionDaoInstance.getAllGarrisonsForOwnerOnce(accountOwnerId)
        val alerts = garrisonManager.evaluateRealmSecurityAlerts(accountOwnerId)

        val resourceBalances = ShadowResourceType.entries.associateWith {
            resourceDao.getCurrentBalanceForOwner(accountOwnerId, it.name)
        }

        val territorySummary = territories.groupBy { it.state }.mapValues { it.value.size }

        val extractors = structures.filter { it.structureType == ShadowInfrastructureType.RESOURCE_EXTRACTOR }
        val yieldPerHour = extractors.sumOf { ShadowDominionConfig.getExtractorHourlyYield(it.level) }

        val citadelUpgrades = CitadelUpgradeType.entries.associateWith { dominion.citadelLevel }

        return DominionOverview(
            dominion = dominion,
            citadelUpgrades = citadelUpgrades,
            resourceBalances = resourceBalances,
            territorySummary = territorySummary,
            totalResourceYieldPerHour = yieldPerHour,
            activeGarrisonsCount = garrisons.size,
            alerts = alerts
        )
    }

    suspend fun getTerritoryDetail(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
        territoryId: String
    ): DominionTerritoryDetail? {
        return territoryManager.getTerritoryDetail(accountOwnerId, territoryId)
    }

    suspend fun ensureDominionInitialized(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): ShadowDominion {
        return dominionManager.ensureDominionInitialized(accountOwnerId)
    }

    suspend fun upgradeCitadel(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
        upgradeType: CitadelUpgradeType
    ): Result<ShadowDominion> {
        return dominionManager.upgradeCitadel(accountOwnerId, upgradeType)
    }

    suspend fun discoverTerritory(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
        territoryId: String
    ): Result<ShadowTerritory> {
        return territoryManager.discoverTerritory(accountOwnerId, territoryId)
    }

    suspend fun startTerritoryExploration(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
        territoryId: String,
        squadId: String
    ): Result<ShadowTerritory> {
        return territoryManager.startTerritoryExploration(accountOwnerId, territoryId, squadId)
    }

    suspend fun claimTerritory(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
        territoryId: String
    ): Result<ShadowTerritory> {
        return territoryManager.claimTerritory(accountOwnerId, territoryId)
    }

    suspend fun constructInfrastructure(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
        territoryId: String,
        structureType: ShadowInfrastructureType
    ): Result<ShadowInfrastructure> {
        return infrastructureManager.constructInfrastructure(accountOwnerId, territoryId, structureType)
    }

    suspend fun upgradeInfrastructure(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
        structureId: String
    ): Result<ShadowInfrastructure> {
        return infrastructureManager.upgradeInfrastructure(accountOwnerId, structureId)
    }

    suspend fun collectResources(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
        structureId: String
    ): Result<Int> {
        return infrastructureManager.collectResources(accountOwnerId, structureId)
    }

    suspend fun stationGarrison(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
        territoryId: String,
        squadId: String
    ): Result<ShadowGarrison> {
        return garrisonManager.stationGarrison(accountOwnerId, territoryId, squadId)
    }

    suspend fun recallGarrison(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
        garrisonId: String
    ): Result<Unit> {
        return garrisonManager.recallGarrison(accountOwnerId, garrisonId)
    }

    suspend fun conductResearch(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
        projectKey: String
    ): Result<ShadowDominionResearch> {
        return researchManager.conductResearch(accountOwnerId, projectKey)
    }

    suspend fun getDominionAiBriefing(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): DominionAiBriefing {
        val overview = getDominionOverview(accountOwnerId)
        val territories = dominionDaoInstance.getAllTerritoriesForOwnerOnce(accountOwnerId).map { it.toDomain() }
        return dominionAdvisor.buildDominionBriefing(overview, territories)
    }
}
