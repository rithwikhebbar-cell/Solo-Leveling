package com.example.domain.engine

import androidx.room.withTransaction
import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.dao.ShadowDominionDao
import com.example.data.local.entity.ShadowDominionActivityEntity
import com.example.data.local.entity.toDomain
import com.example.data.local.entity.toEntity
import com.example.domain.model.DEFAULT_SHADOW_ACCOUNT_OWNER_ID
import com.example.domain.model.DominionActivityType
import com.example.domain.model.DominionDefensiveAssessment
import com.example.domain.model.DominionTerritoryDetail
import com.example.domain.model.ShadowDeploymentCategory
import com.example.domain.model.ShadowDominionConfig
import com.example.domain.model.ShadowTerritory
import com.example.domain.model.ShadowTerritoryState
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

// ============================================================================
// PHASE 6.6 — SHADOW TERRITORY EXPLORATION & MANAGEMENT ENGINE
// ============================================================================

class ShadowTerritoryEngine(
    private val database: SoloLevelingDatabase?,
    private val shadowDominionDao: ShadowDominionDao,
    private val shadowSquadEngine: ShadowSquadEngine,
    private val shadowDeploymentEngine: ShadowDeploymentEngine,
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
     * Discovers an UNKNOWN territory, making it visible for exploration.
     */
    suspend fun discoverTerritory(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
        territoryId: String
    ): Result<ShadowTerritory> {
        val owner = accountOwnerId.trim().ifBlank { DEFAULT_SHADOW_ACCOUNT_OWNER_ID }

        return runCatching {
            withLockedTransaction {
                val territory = shadowDominionDao.getTerritoryById(territoryId, owner)?.toDomain()
                    ?: throw IllegalArgumentException("Territory not found: $territoryId for account $owner")

                if (territory.state != ShadowTerritoryState.UNKNOWN) {
                    return@withLockedTransaction territory
                }

                val now = System.currentTimeMillis()
                val updated = territory.copy(
                    state = ShadowTerritoryState.DISCOVERED,
                    discoveredAtEpochMs = now
                )
                shadowDominionDao.updateTerritory(updated.toEntity())

                shadowDominionDao.insertActivity(
                    ShadowDominionActivityEntity(
                        activityId = UUID.randomUUID().toString(),
                        accountOwnerId = owner,
                        activityTypeName = DominionActivityType.TERRITORY_DISCOVERED.name,
                        title = "Uncharted Land Discovered: ${territory.name}",
                        description = "Scouts have pierced the fog of war, discovering ${territory.name} (${territory.category.label}).",
                        timestampEpochMs = now,
                        entityReferenceId = territoryId
                    )
                )

                updated
            }
        }
    }

    /**
     * Assigns an eligible Shadow Squad to begin exploration of a DISCOVERED territory.
     * Deploys Shadows authoritatively through ShadowDeploymentEngine.
     */
    suspend fun startTerritoryExploration(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
        territoryId: String,
        squadId: String
    ): Result<ShadowTerritory> {
        val owner = accountOwnerId.trim().ifBlank { DEFAULT_SHADOW_ACCOUNT_OWNER_ID }

        return runCatching {
            withLockedTransaction {
                val territory = shadowDominionDao.getTerritoryById(territoryId, owner)?.toDomain()
                    ?: throw IllegalArgumentException("Territory not found: $territoryId")

                if (territory.state != ShadowTerritoryState.DISCOVERED) {
                    throw IllegalStateException("Territory must be in DISCOVERED state to explore (current: ${territory.state.label})")
                }

                val squadDetail = shadowSquadEngine.getSquadDetail(owner, squadId)
                    ?: throw IllegalArgumentException("Squad not found: $squadId")

                if (squadDetail.members.isEmpty()) {
                    throw IllegalStateException("Cannot deploy empty squad for territory exploration")
                }

                // Authoritatively deploy squad members
                for (member in squadDetail.members) {
                    val deployResult = shadowDeploymentEngine.deployShadow(
                        shadowId = member.shadow.shadowId,
                        category = ShadowDeploymentCategory.DOMINION_EXPLORATION,
                        role = "Expeditionary Scout (${territory.name})",
                        accountOwnerId = owner
                    )
                    if (deployResult.isFailure) {
                        throw IllegalStateException("Failed to deploy ${member.shadow.name}: ${deployResult.exceptionOrNull()?.message}")
                    }
                }

                val now = System.currentTimeMillis()
                val endsAt = now + ShadowDominionConfig.EXPLORATION_DURATION_MS

                shadowDominionDao.updateTerritoryExplorationState(
                    territoryId = territoryId,
                    accountOwnerId = owner,
                    stateName = ShadowTerritoryState.EXPLORING.name,
                    progressPercent = 50,
                    endsAtMs = endsAt
                )
                shadowDominionDao.assignSquadToTerritory(territoryId, owner, squadId)

                territory.copy(
                    state = ShadowTerritoryState.EXPLORING,
                    assignedSquadId = squadId,
                    explorationProgressPercent = 50,
                    explorationEndsAtEpochMs = endsAt
                )
            }
        }
    }

    /**
     * Concludes an exploration expedition and claims the territory under the Monarch's dominion.
     * Recalls deployed expedition squad members and advances realm stage.
     */
    suspend fun claimTerritory(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
        territoryId: String
    ): Result<ShadowTerritory> {
        val owner = accountOwnerId.trim().ifBlank { DEFAULT_SHADOW_ACCOUNT_OWNER_ID }

        return runCatching {
            withLockedTransaction {
                val territory = shadowDominionDao.getTerritoryById(territoryId, owner)?.toDomain()
                    ?: throw IllegalArgumentException("Territory not found: $territoryId")

                if (territory.state != ShadowTerritoryState.EXPLORING && territory.state != ShadowTerritoryState.CONTESTED) {
                    throw IllegalStateException("Territory cannot be claimed in state ${territory.state.label}")
                }

                // Recall expedition squad members
                val squadId = territory.assignedSquadId
                if (!squadId.isNullOrBlank()) {
                    val squadDetail = shadowSquadEngine.getSquadDetail(owner, squadId)
                    squadDetail?.members?.forEach { member ->
                        shadowDeploymentEngine.recallShadow(member.shadow.shadowId, owner)
                    }
                }

                val now = System.currentTimeMillis()
                shadowDominionDao.claimTerritory(territoryId, owner, now)
                shadowDominionDao.assignSquadToTerritory(territoryId, owner, null)

                shadowDominionDao.insertActivity(
                    ShadowDominionActivityEntity(
                        activityId = UUID.randomUUID().toString(),
                        accountOwnerId = owner,
                        activityTypeName = DominionActivityType.TERRITORY_CLAIMED.name,
                        title = "Territory Subjugated: ${territory.name}",
                        description = "The Shadow Monarch has claimed full control over ${territory.name}.",
                        timestampEpochMs = now,
                        entityReferenceId = territoryId
                    )
                )

                // Re-evaluate dominion progression
                shadowDominionEngine.reevaluateDominionStage(owner)

                territory.copy(
                    state = ShadowTerritoryState.CONTROLLED,
                    assignedSquadId = null,
                    claimedAtEpochMs = now,
                    explorationProgressPercent = 100,
                    explorationEndsAtEpochMs = null
                )
            }
        }
    }

    /**
     * Assembles complete territory details including structures, assigned garrison, and defensive assessment.
     */
    suspend fun getTerritoryDetail(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
        territoryId: String
    ): DominionTerritoryDetail? {
        val owner = accountOwnerId.trim().ifBlank { DEFAULT_SHADOW_ACCOUNT_OWNER_ID }

        val territory = shadowDominionDao.getTerritoryById(territoryId, owner)?.toDomain() ?: return null
        val structures = shadowDominionDao.getInfrastructuresForTerritoryOnce(territoryId, owner).map { it.toDomain() }
        val garrisonEntity = shadowDominionDao.getGarrisonForTerritoryOnce(territoryId, owner)?.toDomain()
        val garrisonSquad = garrisonEntity?.squadId?.let { shadowSquadEngine.getSquadDetail(owner, it)?.squad }

        val infraBonus = structures.filter { it.structureType.name == "DEFENSE_TOWER" }.sumOf { it.level * 20 }
        val garrisonBonus = garrisonEntity?.defensiveRating ?: 0
        val totalDefense = territory.category.baseDefenseRating + infraBonus + garrisonBonus

        val securityLevel = when {
            totalDefense >= 80 -> "FORTIFIED"
            totalDefense >= 50 -> "SECURE"
            totalDefense >= 30 -> "DEFENDED"
            else -> "EXPOSED"
        }

        val alerts = mutableListOf<String>()
        val recommendations = mutableListOf<String>()

        if (garrisonEntity == null && territory.state == ShadowTerritoryState.CONTROLLED) {
            alerts.add("Territory has no stationed garrison.")
            recommendations.add("Station an elite defense squad to safeguard the region.")
        }
        if (infraBonus == 0 && territory.state == ShadowTerritoryState.CONTROLLED) {
            recommendations.add("Construct a Defense Tower to project protective shadow barriers.")
        }

        val assessment = DominionDefensiveAssessment(
            territoryId = territoryId,
            baseDefenseRating = territory.category.baseDefenseRating,
            infrastructureBonus = infraBonus,
            garrisonRating = garrisonBonus,
            totalDefenseRating = totalDefense,
            securityLevel = securityLevel,
            alerts = alerts,
            recommendations = recommendations
        )

        return DominionTerritoryDetail(
            territory = territory,
            structures = structures,
            garrison = garrisonEntity,
            garrisonSquad = garrisonSquad,
            defenseAssessment = assessment
        )
    }
}
