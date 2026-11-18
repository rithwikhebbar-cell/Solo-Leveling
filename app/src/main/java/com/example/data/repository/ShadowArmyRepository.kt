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
import com.example.domain.model.ShadowArmyAnalytics
import com.example.domain.model.ShadowClass
import com.example.domain.model.ShadowDeploymentRecord
import com.example.domain.model.ShadowEvolutionStage
import com.example.domain.model.ShadowExtractionEvent
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
    // Engines
    val monarchProgressionEngine: MonarchProgressionEngine,
    val extractionEngine: ShadowExtractionEngine,
    val evolutionEngine: ShadowEvolutionEngine,
    val deploymentEngine: ShadowDeploymentEngine,
    val database: SoloLevelingDatabase? = null,
    private val mutex: Mutex = Mutex()
) {

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
}
