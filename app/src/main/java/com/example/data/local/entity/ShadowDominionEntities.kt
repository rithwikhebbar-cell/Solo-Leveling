package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.DominionActivityType
import com.example.domain.model.InfrastructureStatus
import com.example.domain.model.ShadowDominion
import com.example.domain.model.ShadowDominionActivity
import com.example.domain.model.ShadowDominionResearch
import com.example.domain.model.ShadowDominionStage
import com.example.domain.model.ShadowGarrison
import com.example.domain.model.ShadowInfrastructure
import com.example.domain.model.ShadowInfrastructureType
import com.example.domain.model.ShadowResearchBranch
import com.example.domain.model.ShadowTerritory
import com.example.domain.model.ShadowTerritoryCategory
import com.example.domain.model.ShadowTerritoryState

// ============================================================================
// PHASE 6.6 — ROOM ENTITIES FOR SHADOW DOMINION & REALM EXPANSION
// ============================================================================

/**
 * 1. Shadow Dominion Entity — Mirrors persistent realm identity.
 */
@Entity(
    tableName = "shadow_dominion",
    foreignKeys = [
        ForeignKey(
            entity = ShadowMonarchProfileEntity::class,
            parentColumns = ["accountOwnerId"],
            childColumns = ["accountOwnerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["accountOwnerId"], unique = true)
    ]
)
data class ShadowDominionEntity(
    @PrimaryKey
    val dominionId: String,
    val accountOwnerId: String,
    val dominionName: String,
    val dominionLevel: Int,
    val developmentStageName: String,
    val citadelLevel: Int,
    val controlledTerritoriesCount: Int,
    val infrastructureCount: Int,
    val totalDevelopmentEvents: Int,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)

fun ShadowDominionEntity.toDomain(): ShadowDominion = ShadowDominion(
    dominionId = dominionId,
    accountOwnerId = accountOwnerId,
    dominionName = dominionName,
    dominionLevel = dominionLevel,
    developmentStage = runCatching { ShadowDominionStage.valueOf(developmentStageName) }.getOrDefault(ShadowDominionStage.DORMANT_REALM),
    citadelLevel = citadelLevel,
    controlledTerritoriesCount = controlledTerritoriesCount,
    infrastructureCount = infrastructureCount,
    totalDevelopmentEvents = totalDevelopmentEvents,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs
)

fun ShadowDominion.toEntity(): ShadowDominionEntity = ShadowDominionEntity(
    dominionId = dominionId,
    accountOwnerId = accountOwnerId,
    dominionName = dominionName,
    dominionLevel = dominionLevel,
    developmentStageName = developmentStage.name,
    citadelLevel = citadelLevel,
    controlledTerritoriesCount = controlledTerritoriesCount,
    infrastructureCount = infrastructureCount,
    totalDevelopmentEvents = totalDevelopmentEvents,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs
)

/**
 * 2. Shadow Territory Entity — Geographical territories in the Realm.
 */
@Entity(
    tableName = "shadow_territories",
    foreignKeys = [
        ForeignKey(
            entity = ShadowMonarchProfileEntity::class,
            parentColumns = ["accountOwnerId"],
            childColumns = ["accountOwnerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["accountOwnerId", "territoryId"]),
        Index(value = ["accountOwnerId", "stateName"])
    ]
)
data class ShadowTerritoryEntity(
    @PrimaryKey
    val territoryId: String,
    val accountOwnerId: String,
    val name: String,
    val description: String,
    val categoryName: String,
    val stateName: String,
    val developmentLevel: Int,
    val defenseRating: Int,
    val assignedSquadId: String?,
    val discoveredAtEpochMs: Long?,
    val claimedAtEpochMs: Long?,
    val explorationProgressPercent: Int,
    val explorationEndsAtEpochMs: Long?
)

fun ShadowTerritoryEntity.toDomain(): ShadowTerritory = ShadowTerritory(
    territoryId = territoryId,
    accountOwnerId = accountOwnerId,
    name = name,
    description = description,
    category = runCatching { ShadowTerritoryCategory.valueOf(categoryName) }.getOrDefault(ShadowTerritoryCategory.SHADOW_HEARTLAND),
    state = runCatching { ShadowTerritoryState.valueOf(stateName) }.getOrDefault(ShadowTerritoryState.UNKNOWN),
    developmentLevel = developmentLevel,
    defenseRating = defenseRating,
    assignedSquadId = assignedSquadId,
    discoveredAtEpochMs = discoveredAtEpochMs,
    claimedAtEpochMs = claimedAtEpochMs,
    explorationProgressPercent = explorationProgressPercent,
    explorationEndsAtEpochMs = explorationEndsAtEpochMs
)

fun ShadowTerritory.toEntity(): ShadowTerritoryEntity = ShadowTerritoryEntity(
    territoryId = territoryId,
    accountOwnerId = accountOwnerId,
    name = name,
    description = description,
    categoryName = category.name,
    stateName = state.name,
    developmentLevel = developmentLevel,
    defenseRating = defenseRating,
    assignedSquadId = assignedSquadId,
    discoveredAtEpochMs = discoveredAtEpochMs,
    claimedAtEpochMs = claimedAtEpochMs,
    explorationProgressPercent = explorationProgressPercent,
    explorationEndsAtEpochMs = explorationEndsAtEpochMs
)

/**
 * 3. Shadow Infrastructure Entity — Structures built upon territories.
 */
@Entity(
    tableName = "shadow_infrastructures",
    foreignKeys = [
        ForeignKey(
            entity = ShadowMonarchProfileEntity::class,
            parentColumns = ["accountOwnerId"],
            childColumns = ["accountOwnerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["accountOwnerId", "territoryId"]),
        Index(value = ["accountOwnerId", "structureTypeName"])
    ]
)
data class ShadowInfrastructureEntity(
    @PrimaryKey
    val structureId: String,
    val accountOwnerId: String,
    val territoryId: String,
    val structureTypeName: String,
    val level: Int,
    val statusName: String,
    val lastResourceCollectionEpochMs: Long,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)

fun ShadowInfrastructureEntity.toDomain(): ShadowInfrastructure = ShadowInfrastructure(
    structureId = structureId,
    accountOwnerId = accountOwnerId,
    territoryId = territoryId,
    structureType = runCatching { ShadowInfrastructureType.valueOf(structureTypeName) }.getOrDefault(ShadowInfrastructureType.SHADOW_BARRACKS),
    level = level,
    status = runCatching { InfrastructureStatus.valueOf(statusName) }.getOrDefault(InfrastructureStatus.ACTIVE),
    lastResourceCollectionEpochMs = lastResourceCollectionEpochMs,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs
)

fun ShadowInfrastructure.toEntity(): ShadowInfrastructureEntity = ShadowInfrastructureEntity(
    structureId = structureId,
    accountOwnerId = accountOwnerId,
    territoryId = territoryId,
    structureTypeName = structureType.name,
    level = level,
    statusName = status.name,
    lastResourceCollectionEpochMs = lastResourceCollectionEpochMs,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs
)

/**
 * 4. Shadow Garrison Entity — Stationed squads for territorial defense.
 */
@Entity(
    tableName = "shadow_garrisons",
    foreignKeys = [
        ForeignKey(
            entity = ShadowMonarchProfileEntity::class,
            parentColumns = ["accountOwnerId"],
            childColumns = ["accountOwnerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["accountOwnerId", "territoryId"], unique = true),
        Index(value = ["accountOwnerId", "squadId"])
    ]
)
data class ShadowGarrisonEntity(
    @PrimaryKey
    val garrisonId: String,
    val accountOwnerId: String,
    val territoryId: String,
    val squadId: String,
    val assignedAtEpochMs: Long,
    val defensiveRating: Int,
    val readinessStatus: String
)

fun ShadowGarrisonEntity.toDomain(): ShadowGarrison = ShadowGarrison(
    garrisonId = garrisonId,
    accountOwnerId = accountOwnerId,
    territoryId = territoryId,
    squadId = squadId,
    assignedAtEpochMs = assignedAtEpochMs,
    defensiveRating = defensiveRating,
    readinessStatus = readinessStatus
)

fun ShadowGarrison.toEntity(): ShadowGarrisonEntity = ShadowGarrisonEntity(
    garrisonId = garrisonId,
    accountOwnerId = accountOwnerId,
    territoryId = territoryId,
    squadId = squadId,
    assignedAtEpochMs = assignedAtEpochMs,
    defensiveRating = defensiveRating,
    readinessStatus = readinessStatus
)

/**
 * 5. Shadow Dominion Research Entity — Persistent unlocked projects.
 */
@Entity(
    tableName = "shadow_dominion_research",
    foreignKeys = [
        ForeignKey(
            entity = ShadowMonarchProfileEntity::class,
            parentColumns = ["accountOwnerId"],
            childColumns = ["accountOwnerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["accountOwnerId", "projectKey"], unique = true),
        Index(value = ["accountOwnerId", "branchName"])
    ]
)
data class ShadowDominionResearchEntity(
    @PrimaryKey
    val researchId: String,
    val accountOwnerId: String,
    val branchName: String,
    val projectKey: String,
    val title: String,
    val description: String,
    val level: Int,
    val isCompleted: Boolean,
    val startedAtEpochMs: Long?,
    val completedAtEpochMs: Long?,
    val unlockedEffectsSummary: String
)

fun ShadowDominionResearchEntity.toDomain(): ShadowDominionResearch = ShadowDominionResearch(
    researchId = researchId,
    accountOwnerId = accountOwnerId,
    branch = runCatching { ShadowResearchBranch.valueOf(branchName) }.getOrDefault(ShadowResearchBranch.DOMINION_ARCHITECTURE),
    projectKey = projectKey,
    title = title,
    description = description,
    level = level,
    isCompleted = isCompleted,
    startedAtEpochMs = startedAtEpochMs,
    completedAtEpochMs = completedAtEpochMs,
    unlockedEffectsSummary = unlockedEffectsSummary
)

fun ShadowDominionResearch.toEntity(): ShadowDominionResearchEntity = ShadowDominionResearchEntity(
    researchId = researchId,
    accountOwnerId = accountOwnerId,
    branchName = branch.name,
    projectKey = projectKey,
    title = title,
    description = description,
    level = level,
    isCompleted = isCompleted,
    startedAtEpochMs = startedAtEpochMs,
    completedAtEpochMs = completedAtEpochMs,
    unlockedEffectsSummary = unlockedEffectsSummary
)

/**
 * 6. Shadow Dominion Activity Entity — Real-time event log.
 */
@Entity(
    tableName = "shadow_dominion_activities",
    foreignKeys = [
        ForeignKey(
            entity = ShadowMonarchProfileEntity::class,
            parentColumns = ["accountOwnerId"],
            childColumns = ["accountOwnerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["accountOwnerId", "timestampEpochMs"])
    ]
)
data class ShadowDominionActivityEntity(
    @PrimaryKey
    val activityId: String,
    val accountOwnerId: String,
    val activityTypeName: String,
    val title: String,
    val description: String,
    val timestampEpochMs: Long,
    val entityReferenceId: String
)

fun ShadowDominionActivityEntity.toDomain(): ShadowDominionActivity = ShadowDominionActivity(
    activityId = activityId,
    accountOwnerId = accountOwnerId,
    activityType = runCatching { DominionActivityType.valueOf(activityTypeName) }.getOrDefault(DominionActivityType.DOMINION_INITIALIZED),
    title = title,
    description = description,
    timestampEpochMs = timestampEpochMs,
    entityReferenceId = entityReferenceId
)

fun ShadowDominionActivity.toEntity(): ShadowDominionActivityEntity = ShadowDominionActivityEntity(
    activityId = activityId,
    accountOwnerId = accountOwnerId,
    activityTypeName = activityType.name,
    title = title,
    description = description,
    timestampEpochMs = timestampEpochMs,
    entityReferenceId = entityReferenceId
)
