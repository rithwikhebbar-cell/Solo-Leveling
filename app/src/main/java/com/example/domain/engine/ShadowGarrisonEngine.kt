package com.example.domain.engine

import androidx.room.withTransaction
import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.dao.ShadowDominionDao
import com.example.data.local.entity.ShadowDominionActivityEntity
import com.example.data.local.entity.ShadowGarrisonEntity
import com.example.data.local.entity.toDomain
import com.example.domain.model.DEFAULT_SHADOW_ACCOUNT_OWNER_ID
import com.example.domain.model.DominionActivityType
import com.example.domain.model.ShadowDeploymentCategory
import com.example.domain.model.ShadowGarrison
import com.example.domain.model.ShadowTacticalRole
import com.example.domain.model.ShadowTerritoryState
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

// ============================================================================
// PHASE 6.6 — SHADOW GARRISON & TERRITORIAL DEFENSE ENGINE
// ============================================================================

class ShadowGarrisonEngine(
    private val database: SoloLevelingDatabase?,
    private val shadowDominionDao: ShadowDominionDao,
    private val shadowSquadEngine: ShadowSquadEngine,
    private val shadowDeploymentEngine: ShadowDeploymentEngine,
    private val tacticalIntelligenceEngine: ShadowTacticalIntelligenceEngine,
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
     * Stations an existing tactical squad to defend a controlled territory.
     * Deploys Shadows authoritatively through ShadowDeploymentEngine.
     */
    suspend fun stationGarrison(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
        territoryId: String,
        squadId: String
    ): Result<ShadowGarrison> {
        val owner = accountOwnerId.trim().ifBlank { DEFAULT_SHADOW_ACCOUNT_OWNER_ID }

        return runCatching {
            withLockedTransaction {
                val territory = shadowDominionDao.getTerritoryById(territoryId, owner)?.toDomain()
                    ?: throw IllegalArgumentException("Territory not found: $territoryId")

                if (territory.state != ShadowTerritoryState.CONTROLLED &&
                    territory.state != ShadowTerritoryState.DEVELOPING &&
                    territory.state != ShadowTerritoryState.FORTIFIED
                ) {
                    throw IllegalStateException("Garrisons can only be stationed on secured territories (current: ${territory.state.label})")
                }

                val squadDetail = shadowSquadEngine.getSquadDetail(owner, squadId)
                    ?: throw IllegalArgumentException("Squad not found: $squadId")

                if (squadDetail.members.isEmpty()) {
                    throw IllegalStateException("Cannot station empty squad as garrison")
                }

                // If territory already has a garrison, recall it first
                val existingGarrison = shadowDominionDao.getGarrisonForTerritoryOnce(territoryId, owner)
                if (existingGarrison != null) {
                    val prevSquad = shadowSquadEngine.getSquadDetail(owner, existingGarrison.squadId)
                    prevSquad?.members?.forEach { member ->
                        shadowDeploymentEngine.recallShadow(member.shadow.shadowId, owner)
                    }
                    shadowDominionDao.deleteGarrison(existingGarrison.garrisonId, owner)
                }

                // Deploy new squad members
                for (member in squadDetail.members) {
                    val deployRes = shadowDeploymentEngine.deployShadow(
                        shadowId = member.shadow.shadowId,
                        category = ShadowDeploymentCategory.TERRITORY_DEFENSE,
                        role = "Territorial Garrison (${territory.name})",
                        accountOwnerId = owner
                    )
                    if (deployRes.isFailure) {
                        throw IllegalStateException("Failed to deploy garrison member ${member.shadow.name}: ${deployRes.exceptionOrNull()?.message}")
                    }
                }

                // Calculate defensive rating from squad capabilities
                val avgDefense = squadDetail.members.map {
                    tacticalIntelligenceEngine.evaluateCapabilityVector(it.shadow).defensive
                }.average().toInt().coerceIn(10, 100)

                val garrisonId = "garrison_${territoryId}_$owner"
                val now = System.currentTimeMillis()
                val garrison = ShadowGarrison(
                    garrisonId = garrisonId,
                    accountOwnerId = owner,
                    territoryId = territoryId,
                    squadId = squadId,
                    assignedAtEpochMs = now,
                    defensiveRating = avgDefense,
                    readinessStatus = "STATIONED"
                )

                shadowDominionDao.upsertGarrison(
                    ShadowGarrisonEntity(
                        garrisonId = garrisonId,
                        accountOwnerId = owner,
                        territoryId = territoryId,
                        squadId = squadId,
                        assignedAtEpochMs = now,
                        defensiveRating = avgDefense,
                        readinessStatus = "STATIONED"
                    )
                )

                shadowDominionDao.assignSquadToTerritory(territoryId, owner, squadId)

                // If territory has defense towers and garrison, mark as FORTIFIED
                val hasTower = shadowDominionDao.getInfrastructuresForTerritoryOnce(territoryId, owner)
                    .any { it.structureTypeName == "DEFENSE_TOWER" }
                if (hasTower && territory.state != ShadowTerritoryState.FORTIFIED) {
                    shadowDominionDao.updateTerritory(
                        territory.copy(state = ShadowTerritoryState.FORTIFIED).toEntity()
                    )
                }

                shadowDominionDao.insertActivity(
                    ShadowDominionActivityEntity(
                        activityId = UUID.randomUUID().toString(),
                        accountOwnerId = owner,
                        activityTypeName = DominionActivityType.GARRISON_STATIONED.name,
                        title = "Garrison Stationed: ${squadDetail.squad.name}",
                        description = "Deployed ${squadDetail.squad.name} (${squadDetail.members.size} shadows) to protect ${territory.name}.",
                        timestampEpochMs = now,
                        entityReferenceId = garrisonId
                    )
                )

                garrison
            }
        }
    }

    /**
     * Recalls a stationed garrison, freeing Shadows for other deployments.
     */
    suspend fun recallGarrison(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
        garrisonId: String
    ): Result<Unit> {
        val owner = accountOwnerId.trim().ifBlank { DEFAULT_SHADOW_ACCOUNT_OWNER_ID }

        return runCatching {
            withLockedTransaction {
                val garrison = shadowDominionDao.getGarrisonById(garrisonId, owner)?.toDomain()
                    ?: throw IllegalArgumentException("Garrison not found: $garrisonId")

                val squadDetail = shadowSquadEngine.getSquadDetail(owner, garrison.squadId)
                squadDetail?.members?.forEach { member ->
                    shadowDeploymentEngine.recallShadow(member.shadow.shadowId, owner)
                }

                shadowDominionDao.deleteGarrison(garrisonId, owner)
                shadowDominionDao.assignSquadToTerritory(garrison.territoryId, owner, null)

                val territory = shadowDominionDao.getTerritoryById(garrison.territoryId, owner)?.toDomain()
                if (territory != null && territory.state == ShadowTerritoryState.FORTIFIED) {
                    shadowDominionDao.updateTerritory(
                        territory.copy(state = ShadowTerritoryState.DEVELOPING).toEntity()
                    )
                }

                val now = System.currentTimeMillis()
                shadowDominionDao.insertActivity(
                    ShadowDominionActivityEntity(
                        activityId = UUID.randomUUID().toString(),
                        accountOwnerId = owner,
                        activityTypeName = DominionActivityType.GARRISON_RECALLED.name,
                        title = "Garrison Recalled",
                        description = "Stationed shadows have been recalled to the Monarch's main army reserve.",
                        timestampEpochMs = now,
                        entityReferenceId = garrisonId
                    )
                )
            }
        }
    }

    /**
     * Generates defensive security alerts across all controlled territories.
     */
    suspend fun evaluateRealmSecurityAlerts(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): List<String> {
        val owner = accountOwnerId.trim().ifBlank { DEFAULT_SHADOW_ACCOUNT_OWNER_ID }
        val territories = shadowDominionDao.getAllTerritoriesForOwnerOnce(owner)
        val garrisons = shadowDominionDao.getAllGarrisonsForOwnerOnce(owner).associateBy { it.territoryId }
        val alerts = mutableListOf<String>()

        for (t in territories) {
            if (t.stateName == ShadowTerritoryState.CONTROLLED.name ||
                t.stateName == ShadowTerritoryState.DEVELOPING.name
            ) {
                val g = garrisons[t.territoryId]
                if (g == null) {
                    alerts.add("Territory [${t.name}] is completely unguarded without a stationed garrison.")
                } else {
                    val squad = shadowSquadEngine.getSquadDetail(owner, g.squadId)
                    val hasFrontline = squad?.members?.any {
                        tacticalIntelligenceEngine.buildTacticalProfile(it.shadow).primaryRole == ShadowTacticalRole.FRONTLINE
                    } == true
                    if (!hasFrontline) {
                        alerts.add("Garrison at [${t.name}] lacks a Frontline defender.")
                    }
                }
            }
        }
        return alerts
    }
}
