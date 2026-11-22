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
import com.example.domain.model.CitadelUpgradeType
import com.example.domain.model.DEFAULT_SHADOW_ACCOUNT_OWNER_ID
import com.example.domain.model.DominionActivityType
import com.example.domain.model.InfrastructureStatus
import com.example.domain.model.ShadowDominion
import com.example.domain.model.ShadowDominionConfig
import com.example.domain.model.ShadowDominionStage
import com.example.domain.model.ShadowInfrastructureType
import com.example.domain.model.ShadowResourceTransactionType
import com.example.domain.model.ShadowTerritoryState
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

// ============================================================================
// PHASE 6.6 — SHADOW DOMINION CORE & CITADEL ENGINE
// ============================================================================

class ShadowDominionEngine(
    private val database: SoloLevelingDatabase?,
    private val shadowDominionDao: ShadowDominionDao,
    private val shadowResourceDao: ShadowResourceDao,
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
     * Initializes the player's persistent Shadow Dominion if not yet created.
     * Seeds initial realm territories and base structures.
     */
    suspend fun ensureDominionInitialized(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): ShadowDominion {
        val owner = accountOwnerId.trim().ifBlank { DEFAULT_SHADOW_ACCOUNT_OWNER_ID }

        return withLockedTransaction {
            val existing = shadowDominionDao.getDominionForOwnerOnce(owner)?.toDomain()
            if (existing != null) {
                return@withLockedTransaction existing
            }

            val now = System.currentTimeMillis()
            val dominionId = "dominion_$owner"
            val initialDominion = ShadowDominion(
                dominionId = dominionId,
                accountOwnerId = owner,
                dominionName = "Domain of the Shadow Monarch",
                dominionLevel = 1,
                developmentStage = ShadowDominionStage.DORMANT_REALM,
                citadelLevel = 1,
                controlledTerritoriesCount = 1,
                infrastructureCount = 1,
                totalDevelopmentEvents = 1,
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            )
            shadowDominionDao.upsertDominion(initialDominion.toEntity())

            // Seed standard territories for this account
            val territoryEntities = ShadowDominionConfig.INITIAL_TERRITORIES.map { t ->
                t.copy(
                    accountOwnerId = owner,
                    discoveredAtEpochMs = if (t.state != ShadowTerritoryState.UNKNOWN) now else null,
                    claimedAtEpochMs = if (t.state == ShadowTerritoryState.CONTROLLED) now else null
                ).toEntity()
            }
            shadowDominionDao.insertTerritories(territoryEntities)

            // Seed initial Shadow Barracks on the Heartland
            val initialBarracks = ShadowInfrastructureEntity(
                structureId = "infra_barracks_heartland_$owner",
                accountOwnerId = owner,
                territoryId = "territory_heartland",
                structureTypeName = ShadowInfrastructureType.SHADOW_BARRACKS.name,
                level = 1,
                statusName = InfrastructureStatus.ACTIVE.name,
                lastResourceCollectionEpochMs = now,
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            )
            shadowDominionDao.insertInfrastructure(initialBarracks)

            // Record genesis activity
            shadowDominionDao.insertActivity(
                ShadowDominionActivityEntity(
                    activityId = UUID.randomUUID().toString(),
                    accountOwnerId = owner,
                    activityTypeName = DominionActivityType.DOMINION_INITIALIZED.name,
                    title = "Shadow Dominion Awakened",
                    description = "The Shadow Monarch has laid claim to the ancestral Shadow Heartland.",
                    timestampEpochMs = now,
                    entityReferenceId = dominionId
                )
            )

            initialDominion
        }
    }

    /**
     * Retrieves the current Dominion profile.
     */
    suspend fun getDominion(accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID): ShadowDominion? {
        val owner = accountOwnerId.trim().ifBlank { DEFAULT_SHADOW_ACCOUNT_OWNER_ID }
        return shadowDominionDao.getDominionForOwnerOnce(owner)?.toDomain()
    }

    /**
     * Upgrades a specific wing of the Shadow Monarch Citadel.
     * Transactionally validates resource balances, deducts costs, and logs activity.
     */
    suspend fun upgradeCitadel(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
        upgradeType: CitadelUpgradeType
    ): Result<ShadowDominion> {
        val owner = accountOwnerId.trim().ifBlank { DEFAULT_SHADOW_ACCOUNT_OWNER_ID }

        return runCatching {
            withLockedTransaction {
                val dominion = shadowDominionDao.getDominionForOwnerOnce(owner)?.toDomain()
                    ?: throw IllegalStateException("Dominion not initialized for $owner")

                if (dominion.citadelLevel >= ShadowDominionConfig.MAX_CITADEL_LEVEL) {
                    throw IllegalStateException("Citadel has already achieved maximum level (${ShadowDominionConfig.MAX_CITADEL_LEVEL})")
                }

                // Check resource costs
                val costs = ShadowDominionConfig.getCitadelUpgradeCost(upgradeType, dominion.citadelLevel)
                for ((resType, amount) in costs) {
                    val currentBal = shadowResourceDao.getCurrentBalanceForOwner(owner, resType.name)
                    if (currentBal < amount) {
                        throw IllegalStateException("Insufficient ${resType.label}. Required: $amount, Available: $currentBal")
                    }
                }

                val now = System.currentTimeMillis()
                val eventId = "citadel_upg_${upgradeType.name}_${UUID.randomUUID()}"

                // Deduct resources transactionally
                for ((resType, amount) in costs) {
                    val cur = shadowResourceDao.getCurrentBalanceForOwner(owner, resType.name)
                    shadowResourceDao.insertTransaction(
                        ShadowResourceTransactionEntity(
                            transactionId = UUID.randomUUID().toString(),
                            accountOwnerId = owner,
                            resourceTypeName = resType.name,
                            amount = -amount,
                            transactionTypeName = ShadowResourceTransactionType.CONSUMED.name,
                            sourceEventId = eventId,
                            sourceDescription = "Citadel Upgrade: ${upgradeType.label} (Tier ${dominion.citadelLevel + 1})",
                            balanceAfter = cur - amount,
                            timestampEpochMs = now
                        )
                    )
                }

                // Advance Citadel Level & Dominion Level
                val newCitadelLevel = dominion.citadelLevel + 1
                val newDominionLevel = dominion.dominionLevel + 1

                // Evaluate stage progression
                val newStage = evaluateStage(
                    territoriesControlled = dominion.controlledTerritoriesCount,
                    citadelLevel = newCitadelLevel,
                    infrastructureCount = dominion.infrastructureCount,
                    researchCount = shadowDominionDao.getAllResearchForOwnerOnce(owner).count { it.isCompleted }
                )

                shadowDominionDao.updateDominionLevelAndStage(
                    accountOwnerId = owner,
                    level = newDominionLevel,
                    stageName = newStage.name,
                    citadelLevel = newCitadelLevel,
                    updatedAtMs = now
                )

                // Log persistent activity
                shadowDominionDao.insertActivity(
                    ShadowDominionActivityEntity(
                        activityId = UUID.randomUUID().toString(),
                        accountOwnerId = owner,
                        activityTypeName = DominionActivityType.CITADEL_UPGRADED.name,
                        title = "Citadel Upgraded: ${upgradeType.label}",
                        description = "Reinforced ${upgradeType.label} to Level $newCitadelLevel. Realm authority expanded.",
                        timestampEpochMs = now,
                        entityReferenceId = eventId
                    )
                )

                dominion.copy(
                    dominionLevel = newDominionLevel,
                    citadelLevel = newCitadelLevel,
                    developmentStage = newStage,
                    updatedAtEpochMs = now
                )
            }
        }
    }

    /**
     * Re-evaluates whether Dominion stage requirements are met and updates the stage if promoted.
     */
    suspend fun reevaluateDominionStage(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): ShadowDominionStage {
        val owner = accountOwnerId.trim().ifBlank { DEFAULT_SHADOW_ACCOUNT_OWNER_ID }

        return withLockedTransaction {
            val dominion = shadowDominionDao.getDominionForOwnerOnce(owner)?.toDomain()
                ?: return@withLockedTransaction ShadowDominionStage.DORMANT_REALM

            val controlledCount = shadowDominionDao.getAllTerritoriesForOwnerOnce(owner)
                .count { it.stateName == ShadowTerritoryState.CONTROLLED.name || it.stateName == ShadowTerritoryState.DEVELOPING.name || it.stateName == ShadowTerritoryState.FORTIFIED.name }
            val infraCount = shadowDominionDao.getAllInfrastructuresForOwnerOnce(owner).size
            val researchCount = shadowDominionDao.getAllResearchForOwnerOnce(owner).count { it.isCompleted }

            val evaluatedStage = evaluateStage(
                territoriesControlled = controlledCount,
                citadelLevel = dominion.citadelLevel,
                infrastructureCount = infraCount,
                researchCount = researchCount
            )

            val now = System.currentTimeMillis()
            shadowDominionDao.updateTerritoryCount(owner, controlledCount, now)
            shadowDominionDao.updateInfrastructureCount(owner, infraCount, now)

            if (evaluatedStage != dominion.developmentStage) {
                shadowDominionDao.updateDominionLevelAndStage(
                    accountOwnerId = owner,
                    level = dominion.dominionLevel,
                    stageName = evaluatedStage.name,
                    citadelLevel = dominion.citadelLevel,
                    updatedAtMs = now
                )
            }

            evaluatedStage
        }
    }

    private fun evaluateStage(
        territoriesControlled: Int,
        citadelLevel: Int,
        infrastructureCount: Int,
        researchCount: Int
    ): ShadowDominionStage {
        val stages = ShadowDominionStage.entries.sortedByDescending { it.ordinal }
        for (stage in stages) {
            if (territoriesControlled >= stage.requiredTerritories &&
                citadelLevel >= stage.requiredCitadelLevel &&
                infrastructureCount >= stage.requiredStructures &&
                researchCount >= stage.requiredResearchCount
            ) {
                return stage
            }
        }
        return ShadowDominionStage.DORMANT_REALM
    }
}
