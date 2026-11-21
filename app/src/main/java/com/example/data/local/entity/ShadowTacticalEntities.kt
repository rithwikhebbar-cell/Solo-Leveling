package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.DEFAULT_SHADOW_ACCOUNT_OWNER_ID
import com.example.domain.model.ShadowSquad
import com.example.domain.model.ShadowSquadCategory
import com.example.domain.model.ShadowTacticalRole
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// ============================================================================
// PHASE 6.5 — ROOM DATABASE ENTITIES FOR TACTICAL SQUADS & ASSIGNMENTS
// ============================================================================

/**
 * 1. ShadowSquadEntity — persistent tactical squads per account
 */
@Entity(
    tableName = "shadow_squads",
    foreignKeys = [
        ForeignKey(
            entity = ShadowMonarchProfileEntity::class,
            parentColumns = ["accountOwnerId"],
            childColumns = ["accountOwnerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["squadId"], unique = true),
        Index(value = ["accountOwnerId", "isActive"]),
        Index(value = ["accountOwnerId"])
    ]
)
data class ShadowSquadEntity(
    @PrimaryKey val squadId: String,
    val accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
    val name: String,
    val categoryName: String,
    val leaderShadowId: String?,
    val isActive: Boolean,
    val maxMembers: Int,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)

/**
 * 2. ShadowSquadMemberEntity — individual shadow membership in a squad
 */
@Entity(
    tableName = "shadow_squad_members",
    primaryKeys = ["squadId", "shadowId"],
    foreignKeys = [
        ForeignKey(
            entity = ShadowSquadEntity::class,
            parentColumns = ["squadId"],
            childColumns = ["squadId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ShadowEntity::class,
            parentColumns = ["shadowId"],
            childColumns = ["shadowId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["squadId", "positionIndex"]),
        Index(value = ["accountOwnerId", "shadowId"]),
        Index(value = ["shadowId"])
    ]
)
data class ShadowSquadMemberEntity(
    val squadId: String,
    val shadowId: String,
    val accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
    val positionIndex: Int,
    val assignedTacticalRoleName: String
)

/**
 * 3. ShadowMissionAssignmentEntity — audit record of tactical mission preparations / assignments
 */
@Entity(
    tableName = "shadow_mission_assignments",
    foreignKeys = [
        ForeignKey(
            entity = ShadowMonarchProfileEntity::class,
            parentColumns = ["accountOwnerId"],
            childColumns = ["accountOwnerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["assignmentId"], unique = true),
        Index(value = ["accountOwnerId", "missionId"]),
        Index(value = ["accountOwnerId", "statusName"])
    ]
)
data class ShadowMissionAssignmentEntity(
    @PrimaryKey val assignmentId: String,
    val accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
    val missionId: String,
    val missionTypeName: String,
    val squadId: String?,
    val assignedShadowIdsJson: String, // JSON array of shadowId strings
    val statusName: String,
    val assignedAtEpochMs: Long,
    val completedAtEpochMs: Long?
)

// ============================================================================
// DOMAIN / ENTITY MAPPERS
// ============================================================================

fun ShadowSquadEntity.toDomain(memberIds: List<String>): ShadowSquad {
    val category = runCatching { ShadowSquadCategory.valueOf(categoryName) }
        .getOrDefault(ShadowSquadCategory.ASSAULT_SQUAD)
    return ShadowSquad(
        squadId = squadId,
        accountOwnerId = accountOwnerId,
        name = name,
        category = category,
        leaderShadowId = leaderShadowId,
        memberShadowIds = memberIds,
        isActive = isActive,
        maxMembers = maxMembers,
        createdAtEpochMs = createdAtEpochMs,
        updatedAtEpochMs = updatedAtEpochMs
    )
}

fun ShadowSquad.toEntity(): ShadowSquadEntity {
    return ShadowSquadEntity(
        squadId = squadId,
        accountOwnerId = accountOwnerId,
        name = name,
        categoryName = category.name,
        leaderShadowId = leaderShadowId,
        isActive = isActive,
        maxMembers = maxMembers,
        createdAtEpochMs = createdAtEpochMs,
        updatedAtEpochMs = updatedAtEpochMs
    )
}
