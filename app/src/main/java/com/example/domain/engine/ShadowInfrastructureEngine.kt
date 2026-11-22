package com.example.domain.engine

import androidx.room.withTransaction
import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.dao.ShadowDominionDao
import com.example.data.local.dao.ShadowResourceDao
import com.example.data.local.entity.ShadowDominionActivityEntity
import com.example.data.local.entity.ShadowInfrastructureEntity
import com.example.data.local.entity.ShadowResourceTransactionEntity
import com.example.data.local.entity.toDomain
import com.example.data.local.entity.toEntity
import com.example.domain.model.DEFAULT_SHADOW_ACCOUNT_OWNER_ID
import com.example.domain.model.DominionActivityType
import com.example.domain.model.InfrastructureStatus
import com.example.domain.model.ShadowDominionConfig
import com.example.domain.model.ShadowInfrastructure
import com.example.domain.model.ShadowInfrastructureType
import com.example.domain.model.ShadowResourceTransactionType
import com.example.domain.model.ShadowResourceType
import com.example.domain.model.ShadowTerritoryState
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

// ============================================================================
// PHASE 6.6 — SHADOW INFRASTRUCTURE & RESOURCE PRODUCTION ENGINE
// ============================================================================

class ShadowInfrastructureEngine(
    private val database: SoloLevelingDatabase?,
    private val shadowDominionDao: ShadowDominionDao,
    private val shadowResourceDao: ShadowResourceDao,
    private val shadowDominionEngine: ShadowDominionEngine,
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

    /**
     * Constructs a new infrastructure facility on a controlled territory.
     * Transactionally deducts resource costs and updates realm stage.
     */
    suspend fun constructInfrastructure(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
        territoryId: String,
        structureType: ShadowInfrastructureType
    ): Result<ShadowInfrastructure> {
        val owner = accountOwnerId.trim().ifBlank { DEFAULT_SHADOW_ACCOUNT_OWNER_ID }

        return runCatching {
            withLockedTransaction {
                val territory = shadowDominionDao.getTerritoryById(territoryId, owner)?.toDomain()
                    ?: throw IllegalArgumentException("Territory not found: $territoryId")

                if (territory.state != ShadowTerritoryState.CONTROLLED &&
                    territory.state != ShadowTerritoryState.DEVELOPING &&
                    territory.state != ShadowTerritoryState.FORTIFIED
                ) {
                    throw IllegalStateException("Infrastructure can only be constructed on controlled territories (current: ${territory.state.label})")
                }

                // Check resource costs
                val costs = ShadowDominionConfig.getInfrastructureCost(structureType, targetLevel = 1)
                for ((resType, amount) in costs) {
                    val currentBal = shadowResourceDao.getCurrentBalanceForOwner(owner, resType.name)
                    if (currentBal < amount) {
                        throw IllegalStateException("Insufficient ${resType.label}. Required: $amount, Available: $currentBal")
                    }
                }

                val now = System.currentTimeMillis()
                val eventId = "infra_build_${UUID.randomUUID()}"

                // Deduct resources atomically
                for ((resType, amount) in costs) {
                    val currentBal = shadowResourceDao.getCurrentBalanceForOwner(owner, resType.name)
                    shadowResourceDao.insertTransaction(
                        ShadowResourceTransactionEntity(
                            transactionId = UUID.randomUUID().toString(),
                            accountOwnerId = owner,
                            resourceTypeName = resType.name,
                            amount = -amount,
                            transactionTypeName = ShadowResourceTransactionType.CONSUMED.name,
                            sourceEventId = eventId,
                            sourceDescription = "Constructed ${structureType.label} in ${territory.name}",
                            balanceAfter = currentBal - amount,
                            timestampEpochMs = now
                        )
                    )
                }

                val structureId = "infra_${structureType.name.lowercase()}_${UUID.randomUUID().toString().take(8)}"
                val newStructure = ShadowInfrastructure(
                    structureId = structureId,
                    accountOwnerId = owner,
                    territoryId = territoryId,
                    structureType = structureType,
                    level = 1,
                    status = InfrastructureStatus.ACTIVE,
                    lastResourceCollectionEpochMs = now,
                    createdAtEpochMs = now,
                    updatedAtEpochMs = now
                )
                shadowDominionDao.insertInfrastructure(newStructure.toEntity())

                // Advance territory state to DEVELOPING if it was purely CONTROLLED
                if (territory.state == ShadowTerritoryState.CONTROLLED) {
                    shadowDominionDao.updateTerritory(
                        territory.copy(state = ShadowTerritoryState.DEVELOPING).toEntity()
                    )
                }

                shadowDominionDao.insertActivity(
                    ShadowDominionActivityEntity(
                        activityId = UUID.randomUUID().toString(),
                        accountOwnerId = owner,
                        activityTypeName = DominionActivityType.INFRASTRUCTURE_CONSTRUCTED.name,
                        title = "Constructed: ${structureType.label}",
                        description = "Erected ${structureType.label} in ${territory.name}.",
                        timestampEpochMs = now,
                        entityReferenceId = structureId
                    )
                )

                // Re-evaluate dominion progression stage
                shadowDominionEngine.reevaluateDominionStage(owner)

                newStructure
            }
        }
    }

    /**
     * Upgrades an existing infrastructure structure to its next level tier.
     */
    suspend fun upgradeInfrastructure(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
        structureId: String
    ): Result<ShadowInfrastructure> {
        val owner = accountOwnerId.trim().ifBlank { DEFAULT_SHADOW_ACCOUNT_OWNER_ID }

        return runCatching {
            withLockedTransaction {
                val structure = shadowDominionDao.getInfrastructureById(structureId, owner)?.toDomain()
                    ?: throw IllegalArgumentException("Infrastructure not found: $structureId")

                if (structure.level >= structure.structureType.maxLevel) {
                    throw IllegalStateException("Infrastructure has reached maximum level (${structure.structureType.maxLevel})")
                }

                val nextLevel = structure.level + 1
                val costs = ShadowDominionConfig.getInfrastructureCost(structure.structureType, targetLevel = nextLevel)

                for ((resType, amount) in costs) {
                    val currentBal = shadowResourceDao.getCurrentBalanceForOwner(owner, resType.name)
                    if (currentBal < amount) {
                        throw IllegalStateException("Insufficient ${resType.label}. Required: $amount, Available: $currentBal")
                    }
                }

                val now = System.currentTimeMillis()
                val eventId = "infra_upg_${UUID.randomUUID()}"

                for ((resType, amount) in costs) {
                    val currentBal = shadowResourceDao.getCurrentBalanceForOwner(owner, resType.name)
                    shadowResourceDao.insertTransaction(
                        ShadowResourceTransactionEntity(
                            transactionId = UUID.randomUUID().toString(),
                            accountOwnerId = owner,
                            resourceTypeName = resType.name,
                            amount = -amount,
                            transactionTypeName = ShadowResourceTransactionType.CONSUMED.name,
                            sourceEventId = eventId,
                            sourceDescription = "Upgraded ${structure.structureType.label} to Level $nextLevel",
                            balanceAfter = currentBal - amount,
                            timestampEpochMs = now
                        )
                    )
                }

                val updated = structure.copy(
                    level = nextLevel,
                    status = InfrastructureStatus.ACTIVE,
                    updatedAtEpochMs = now
                )
                shadowDominionDao.updateInfrastructure(updated.toEntity())

                shadowDominionDao.insertActivity(
                    ShadowDominionActivityEntity(
                        activityId = UUID.randomUUID().toString(),
                        accountOwnerId = owner,
                        activityTypeName = DominionActivityType.INFRASTRUCTURE_UPGRADED.name,
                        title = "Upgraded: ${structure.structureType.label} (Lv $nextLevel)",
                        description = "${structure.structureType.label} enhanced to Level $nextLevel.",
                        timestampEpochMs = now,
                        entityReferenceId = structureId
                    )
                )

                shadowDominionEngine.reevaluateDominionStage(owner)

                updated
            }
        }
    }

    /**
     * Calculates the deterministic accumulated resource amount from a RESOURCE_EXTRACTOR.
     */
    fun calculateAccumulatedResources(
        structure: ShadowInfrastructure,
        nowEpochMs: Long = System.currentTimeMillis()
    ): Int {
        if (structure.structureType != ShadowInfrastructureType.RESOURCE_EXTRACTOR) return 0
        val elapsedMs = (nowEpochMs - structure.lastResourceCollectionEpochMs)
            .coerceAtLeast(0L)
            .coerceAtMost(ShadowDominionConfig.RESOURCE_COLLECTION_CAP_MS)

        val hours = elapsedMs / 3600_000.0
        val yieldPerHour = ShadowDominionConfig.getExtractorHourlyYield(structure.level)
        val yield = (hours * yieldPerHour).toInt()

        // Minimum 1 if at least 30 seconds have passed (for smooth interactive experience)
        return if (yield == 0 && elapsedMs >= 30_000L) 1 else yield
    }

    /**
     * Harvests accumulated resources from an active RESOURCE_EXTRACTOR.
     * Transactionally inserts earned resources into the ledger.
     */
    suspend fun collectResources(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
        structureId: String
    ): Result<Int> {
        val owner = accountOwnerId.trim().ifBlank { DEFAULT_SHADOW_ACCOUNT_OWNER_ID }

        return runCatching {
            withLockedTransaction {
                val structure = shadowDominionDao.getInfrastructureById(structureId, owner)?.toDomain()
                    ?: throw IllegalArgumentException("Infrastructure not found: $structureId")

                if (structure.structureType != ShadowInfrastructureType.RESOURCE_EXTRACTOR) {
                    throw IllegalStateException("Only Resource Extractors yield harvestable resources")
                }

                val now = System.currentTimeMillis()
                val accumulated = calculateAccumulatedResources(structure, now)

                if (accumulated <= 0) {
                    throw IllegalStateException("No resources available to collect at this time")
                }

                val resourceType = structure.structureType.resourceGenerated ?: ShadowResourceType.SHADOW_ESSENCE
                val curBal = shadowResourceDao.getCurrentBalanceForOwner(owner, resourceType.name)

                val eventId = "collect_${UUID.randomUUID()}"
                shadowResourceDao.insertTransaction(
                    ShadowResourceTransactionEntity(
                        transactionId = UUID.randomUUID().toString(),
                        accountOwnerId = owner,
                        resourceTypeName = resourceType.name,
                        amount = accumulated,
                        transactionTypeName = ShadowResourceTransactionType.EARNED.name,
                        sourceEventId = eventId,
                        sourceDescription = "Extracted from ${structure.structureType.label} (Lv ${structure.level})",
                        balanceAfter = curBal + accumulated,
                        timestampEpochMs = now
                    )
                )

                // Update collection timestamp
                shadowDominionDao.updateInfrastructure(
                    structure.copy(
                        lastResourceCollectionEpochMs = now,
                        updatedAtEpochMs = now
                    ).toEntity()
                )

                shadowDominionDao.insertActivity(
                    ShadowDominionActivityEntity(
                        activityId = UUID.randomUUID().toString(),
                        accountOwnerId = owner,
                        activityTypeName = DominionActivityType.RESOURCES_COLLECTED.name,
                        title = "Harvested: $accumulated ${resourceType.label}",
                        description = "Gathered $accumulated ${resourceType.label} from territorial resource extractors.",
                        timestampEpochMs = now,
                        entityReferenceId = structureId
                    )
                )

                accumulated
            }
        }
    }
}
