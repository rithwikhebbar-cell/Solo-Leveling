package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.ShadowMissionAssignmentEntity
import com.example.data.local.entity.ShadowSquadEntity
import com.example.data.local.entity.ShadowSquadMemberEntity
import com.example.domain.model.DEFAULT_SHADOW_ACCOUNT_OWNER_ID
import kotlinx.coroutines.flow.Flow

// ============================================================================
// PHASE 6.5 — ROOM DAO FOR SHADOW TACTICAL SQUADS & MISSIONS
// ============================================================================

@Dao
interface ShadowTacticalDao {

    // -------------------------------------------------------------------------
    // SQUADS
    // -------------------------------------------------------------------------

    @Query("SELECT * FROM shadow_squads WHERE accountOwnerId = :accountOwnerId ORDER BY updatedAtEpochMs DESC")
    fun observeSquadsForOwner(accountOwnerId: String): Flow<List<ShadowSquadEntity>>

    fun observeSquads(): Flow<List<ShadowSquadEntity>> =
        observeSquadsForOwner(DEFAULT_SHADOW_ACCOUNT_OWNER_ID)

    @Query("SELECT * FROM shadow_squads WHERE accountOwnerId = :accountOwnerId ORDER BY updatedAtEpochMs DESC")
    suspend fun getAllSquadsForOwnerOnce(accountOwnerId: String): List<ShadowSquadEntity>

    @Query("SELECT * FROM shadow_squads WHERE squadId = :squadId AND accountOwnerId = :accountOwnerId LIMIT 1")
    suspend fun getSquadById(squadId: String, accountOwnerId: String): ShadowSquadEntity?

    @Query("SELECT * FROM shadow_squads WHERE accountOwnerId = :accountOwnerId AND isActive = 1 LIMIT 1")
    suspend fun getActiveSquadForOwnerOnce(accountOwnerId: String): ShadowSquadEntity?

    @Query("SELECT * FROM shadow_squads WHERE accountOwnerId = :accountOwnerId AND isActive = 1 LIMIT 1")
    fun observeActiveSquadForOwner(accountOwnerId: String): Flow<ShadowSquadEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSquad(entity: ShadowSquadEntity): Long

    @Update
    suspend fun updateSquad(entity: ShadowSquadEntity): Int

    @Query("DELETE FROM shadow_squads WHERE squadId = :squadId AND accountOwnerId = :accountOwnerId")
    suspend fun deleteSquad(squadId: String, accountOwnerId: String): Int

    @Query("UPDATE shadow_squads SET isActive = 0 WHERE accountOwnerId = :accountOwnerId")
    suspend fun clearActiveSquads(accountOwnerId: String): Int

    @Query("UPDATE shadow_squads SET isActive = 1, updatedAtEpochMs = :updatedAtMs WHERE squadId = :squadId AND accountOwnerId = :accountOwnerId")
    suspend fun setActiveSquad(squadId: String, accountOwnerId: String, updatedAtMs: Long): Int

    // -------------------------------------------------------------------------
    // SQUAD MEMBERS
    // -------------------------------------------------------------------------

    @Query("SELECT * FROM shadow_squad_members WHERE squadId = :squadId ORDER BY positionIndex ASC")
    fun observeMembersForSquad(squadId: String): Flow<List<ShadowSquadMemberEntity>>

    @Query("SELECT * FROM shadow_squad_members WHERE squadId = :squadId ORDER BY positionIndex ASC")
    suspend fun getMembersForSquadOnce(squadId: String): List<ShadowSquadMemberEntity>

    @Query("SELECT * FROM shadow_squad_members WHERE accountOwnerId = :accountOwnerId")
    suspend fun getAllSquadMembersForOwnerOnce(accountOwnerId: String): List<ShadowSquadMemberEntity>

    @Query("SELECT * FROM shadow_squad_members WHERE accountOwnerId = :accountOwnerId")
    fun observeAllSquadMembersForOwner(accountOwnerId: String): Flow<List<ShadowSquadMemberEntity>>

    @Query("SELECT * FROM shadow_squad_members WHERE accountOwnerId = :accountOwnerId AND shadowId = :shadowId")
    suspend fun getSquadMembershipForShadow(accountOwnerId: String, shadowId: String): List<ShadowSquadMemberEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(entity: ShadowSquadMemberEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(entities: List<ShadowSquadMemberEntity>)

    @Query("DELETE FROM shadow_squad_members WHERE squadId = :squadId AND shadowId = :shadowId")
    suspend fun deleteMember(squadId: String, shadowId: String): Int

    @Query("DELETE FROM shadow_squad_members WHERE squadId = :squadId")
    suspend fun deleteAllMembersForSquad(squadId: String): Int

    // -------------------------------------------------------------------------
    // MISSION ASSIGNMENTS
    // -------------------------------------------------------------------------

    @Query("SELECT * FROM shadow_mission_assignments WHERE accountOwnerId = :accountOwnerId ORDER BY assignedAtEpochMs DESC")
    fun observeAssignmentsForOwner(accountOwnerId: String): Flow<List<ShadowMissionAssignmentEntity>>

    @Query("SELECT * FROM shadow_mission_assignments WHERE assignmentId = :assignmentId AND accountOwnerId = :accountOwnerId LIMIT 1")
    suspend fun getAssignmentById(assignmentId: String, accountOwnerId: String): ShadowMissionAssignmentEntity?

    @Query("SELECT * FROM shadow_mission_assignments WHERE accountOwnerId = :accountOwnerId ORDER BY assignedAtEpochMs DESC")
    suspend fun getAllAssignmentsForOwnerOnce(accountOwnerId: String): List<ShadowMissionAssignmentEntity>

    @Query("SELECT * FROM shadow_mission_assignments WHERE accountOwnerId = :accountOwnerId ORDER BY assignedAtEpochMs DESC LIMIT :limit")
    suspend fun getRecentAssignmentsForOwner(accountOwnerId: String, limit: Int = 20): List<ShadowMissionAssignmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssignment(entity: ShadowMissionAssignmentEntity): Long

    @Update
    suspend fun updateAssignment(entity: ShadowMissionAssignmentEntity): Int
}
