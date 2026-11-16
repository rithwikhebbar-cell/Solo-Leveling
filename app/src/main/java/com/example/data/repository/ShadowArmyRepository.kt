package com.example.data.repository

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
import com.example.domain.model.MonarchSkillCatalog
import com.example.domain.model.MonarchSkillDefinition
import com.example.domain.model.MonarchSkillIds
import com.example.domain.model.Shadow
import com.example.domain.model.ShadowArmyAnalytics
import com.example.domain.model.ShadowClass
import com.example.domain.model.ShadowDeploymentCategory
import com.example.domain.model.ShadowDeploymentRecord
import com.example.domain.model.ShadowEvolutionStage
import com.example.domain.model.ShadowExtractionEvent
import com.example.domain.model.ShadowExtractionSourceType
import com.example.domain.model.ShadowFormation
import com.example.domain.model.ShadowFormationType
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
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
    val deploymentEngine: ShadowDeploymentEngine
) {

    // -------------------------------------------------------------------------
    // REACTIVE STREAMS
    // -------------------------------------------------------------------------

    fun observeMonarchProfile(): Flow<ShadowMonarchProfile?> {
        return monarchProfileDao.observeProfile().map { it?.toDomain() }
    }

    fun observeActiveShadows(): Flow<List<Shadow>> {
        return shadowDao.observeActiveShadows().map { list -> list.map { it.toDomain() } }
    }

    fun observeAllShadows(): Flow<List<Shadow>> {
        return shadowDao.observeAllShadows().map { list -> list.map { it.toDomain() } }
    }

    fun observeExtractionHistory(): Flow<List<ShadowExtractionEvent>> {
        return extractionDao.observeExtractions().map { list -> list.map { it.toDomain() } }
    }

    fun observeActiveDeployments(): Flow<List<ShadowDeploymentRecord>> {
        return deploymentDao.observeActiveDeployments().map { list -> list.map { it.toDomain() } }
    }

    fun observeFormations(): Flow<List<ShadowFormation>> {
        return formationDao.observeFormations().map { list -> list.map { it.toDomain() } }
    }

    fun observeActiveFormation(): Flow<ShadowFormation?> {
        return formationDao.observeActiveFormation().map { it?.toDomain() }
    }

    fun observeQuestProgress(): Flow<List<Pair<ShadowQuestDefinition, ShadowQuestProgress?>>> {
        return questProgressDao.observeQuestProgress().map { progressList ->
            val progressMap = progressList.associate { it.questId to it.toDomain() }
            ShadowQuestCatalog.quests.map { questDef ->
                Pair(questDef, progressMap[questDef.questId])
            }
        }
    }

    fun observeResourceTransactions(): Flow<List<ShadowResourceTransaction>> {
        return resourceDao.observeTransactions().map { list -> list.map { it.toDomain() } }
    }

    fun observeMonarchSkillUnlocks(): Flow<List<MonarchSkillDefinition>> {
        return monarchSkillUnlockDao.observeMonarchSkillUnlocks().map { unlocks ->
            val unlockedIds = unlocks.map { it.skillId }.toSet()
            MonarchSkillCatalog.skills.filter { it.skillId in unlockedIds }
        }
    }

    // -------------------------------------------------------------------------
    // SHADOW ARMY ANALYTICS
    // -------------------------------------------------------------------------

    suspend fun buildShadowArmyAnalytics(): ShadowArmyAnalytics {
        val allShadows = shadowDao.getAllShadowsOnce().map { it.toDomain() }
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
            } catch (e: Exception) { 0 }
        }

        val deployedShadows = deploymentDao.getActiveDeploymentsOnce().map { it.toDomain() }
        val deployedShadowClasses = deployedShadows.mapNotNull { deployment ->
            activeShadows.find { it.shadowId == deployment.shadowId }?.shadowClass
        }
        val mostDeployedClass = deployedShadowClasses.groupBy { it }.maxByOrNull { it.value.size }?.key

        val resourceBalances = ShadowResourceType.entries.associateWith { type ->
            resourceDao.getCurrentBalance(type.name)
        }

        val profile = monarchProgressionEngine.getOrCreateProfile()

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
    // RESOURCE MANAGEMENT (Transactional)
    // -------------------------------------------------------------------------

    suspend fun getCurrentBalance(resourceType: ShadowResourceType): Int {
        return resourceDao.getCurrentBalance(resourceType.name)
    }

    /**
     * Awards a shadow resource in a transactional, idempotent manner.
     * Returns Result with the new balance. If duplicate, returns current balance.
     */
    suspend fun awardResource(
        resourceType: ShadowResourceType,
        amount: Int,
        sourceEventId: String,
        sourceDescription: String
    ): Result<Int> {
        return runCatching {
            // Idempotency check: already awarded for this source event
            val existingTx = resourceDao.getTransactionBySourceEvent(sourceEventId, resourceType.name)
            if (existingTx != null) {
                return@runCatching existingTx.balanceAfter
            }

            val currentBalance = getCurrentBalance(resourceType)
            val newBalance = currentBalance + amount.coerceAtLeast(0)

            resourceDao.insertTransaction(
                ShadowResourceTransactionEntity(
                    transactionId = UUID.randomUUID().toString(),
                    resourceTypeName = resourceType.name,
                    amount = amount,
                    transactionTypeName = ShadowResourceTransactionType.EARNED.name,
                    sourceEventId = sourceEventId,
                    sourceDescription = sourceDescription,
                    balanceAfter = newBalance,
                    timestampEpochMs = System.currentTimeMillis()
                )
            )
            newBalance
        }
    }

    /**
     * Consumes a shadow resource. Requires sufficient balance.
     * Enforces non-negative balance invariant.
     */
    suspend fun consumeResource(
        resourceType: ShadowResourceType,
        amount: Int,
        purpose: String,
        transactionEventId: String = "consume_${UUID.randomUUID()}"
    ): Result<Int> {
        return runCatching {
            // Idempotency check
            val existingTx = resourceDao.getTransactionBySourceEvent(transactionEventId, resourceType.name)
            if (existingTx != null) throw IllegalStateException("Transaction already processed: $transactionEventId")

            val currentBalance = getCurrentBalance(resourceType)
            if (currentBalance < amount) throw IllegalStateException("Insufficient ${resourceType.label}: $currentBalance < $amount")

            val newBalance = currentBalance - amount
            resourceDao.insertTransaction(
                ShadowResourceTransactionEntity(
                    transactionId = UUID.randomUUID().toString(),
                    resourceTypeName = resourceType.name,
                    amount = -amount,
                    transactionTypeName = ShadowResourceTransactionType.CONSUMED.name,
                    sourceEventId = transactionEventId,
                    sourceDescription = purpose,
                    balanceAfter = newBalance,
                    timestampEpochMs = System.currentTimeMillis()
                )
            )
            newBalance
        }
    }

    // -------------------------------------------------------------------------
    // CONVENIENCE QUERIES
    // -------------------------------------------------------------------------

    suspend fun getShadowById(shadowId: String): Shadow? {
        return shadowDao.getShadowById(shadowId)?.toDomain()
    }

    fun observeShadowById(shadowId: String): Flow<Shadow?> {
        return shadowDao.observeAllShadows().map { list ->
            list.find { it.shadowId == shadowId }?.toDomain()
        }
    }

    suspend fun getMonarchProfile(): ShadowMonarchProfile {
        return monarchProgressionEngine.getOrCreateProfile()
    }

    fun getShadowSkillCatalog(): List<ShadowSkillDefinition> = ShadowSkillCatalog.skills

    fun getMonarchSkillCatalog(): List<MonarchSkillDefinition> = MonarchSkillCatalog.skills

    suspend fun getUnlockedMonarchSkillIds(): List<String> {
        return monarchSkillUnlockDao.getAllMonarchSkillUnlocksOnce().map { it.skillId }
    }
}

