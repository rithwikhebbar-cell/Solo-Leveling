package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.ShadowBondEventEntity
import com.example.data.local.entity.ShadowBondMilestoneEntity
import com.example.data.local.entity.ShadowBondProfileEntity
import kotlinx.coroutines.flow.Flow

// ============================================================================
// MILESTONE 12 — PHASE 6.4: SHADOW BOND DAO
// ============================================================================

@Dao
interface ShadowBondDao {

    // -------------------------------------------------------------------------
    // BOND PROFILES
    // -------------------------------------------------------------------------

    @Query("SELECT * FROM shadow_bond_profiles WHERE shadowId = :shadowId AND accountOwnerId = :accountOwnerId LIMIT 1")
    suspend fun getBondProfile(shadowId: String, accountOwnerId: String): ShadowBondProfileEntity?

    @Query("SELECT * FROM shadow_bond_profiles WHERE shadowId = :shadowId AND accountOwnerId = :accountOwnerId LIMIT 1")
    fun observeBondProfile(shadowId: String, accountOwnerId: String): Flow<ShadowBondProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBondProfile(entity: ShadowBondProfileEntity): Long

    @Query("SELECT * FROM shadow_bond_profiles WHERE accountOwnerId = :accountOwnerId")
    fun observeAllBondProfilesForOwner(accountOwnerId: String): Flow<List<ShadowBondProfileEntity>>

    @Query("SELECT * FROM shadow_bond_profiles WHERE accountOwnerId = :accountOwnerId")
    suspend fun getAllBondProfilesForOwner(accountOwnerId: String): List<ShadowBondProfileEntity>

    // -------------------------------------------------------------------------
    // BOND EVENTS
    // -------------------------------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBondEvent(entity: ShadowBondEventEntity): Long

    @Query("SELECT * FROM shadow_bond_events WHERE idempotencyKey = :idempotencyKey LIMIT 1")
    suspend fun getBondEventByIdempotencyKey(idempotencyKey: String): ShadowBondEventEntity?

    @Query("SELECT * FROM shadow_bond_events WHERE shadowId = :shadowId AND accountOwnerId = :accountOwnerId ORDER BY timestampEpochMs DESC")
    suspend fun getBondEventsForShadow(shadowId: String, accountOwnerId: String): List<ShadowBondEventEntity>

    @Query("SELECT * FROM shadow_bond_events WHERE shadowId = :shadowId AND accountOwnerId = :accountOwnerId ORDER BY timestampEpochMs DESC")
    fun observeBondEventsForShadow(shadowId: String, accountOwnerId: String): Flow<List<ShadowBondEventEntity>>

    // -------------------------------------------------------------------------
    // BOND MILESTONES
    // -------------------------------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBondMilestone(entity: ShadowBondMilestoneEntity): Long

    @Query("SELECT * FROM shadow_bond_milestones WHERE shadowId = :shadowId AND accountOwnerId = :accountOwnerId ORDER BY timestampEpochMs DESC")
    suspend fun getBondMilestonesForShadow(shadowId: String, accountOwnerId: String): List<ShadowBondMilestoneEntity>

    @Query("SELECT * FROM shadow_bond_milestones WHERE shadowId = :shadowId AND accountOwnerId = :accountOwnerId ORDER BY timestampEpochMs DESC")
    fun observeBondMilestonesForShadow(shadowId: String, accountOwnerId: String): Flow<List<ShadowBondMilestoneEntity>>

    @Query("SELECT COUNT(*) > 0 FROM shadow_bond_milestones WHERE shadowId = :shadowId AND accountOwnerId = :accountOwnerId AND milestoneTypeName = :milestoneTypeName")
    suspend fun hasMilestone(shadowId: String, accountOwnerId: String, milestoneTypeName: String): Boolean

    @Query("SELECT * FROM shadow_bond_milestones WHERE accountOwnerId = :accountOwnerId ORDER BY timestampEpochMs DESC")
    fun observeAllMilestonesForOwner(accountOwnerId: String): Flow<List<ShadowBondMilestoneEntity>>

    @Query("SELECT * FROM shadow_bond_milestones WHERE accountOwnerId = :accountOwnerId ORDER BY timestampEpochMs DESC LIMIT :limit")
    suspend fun getRecentMilestonesForOwner(accountOwnerId: String, limit: Int = 10): List<ShadowBondMilestoneEntity>

    @Query("SELECT * FROM shadow_bond_milestones WHERE accountOwnerId = :accountOwnerId ORDER BY timestampEpochMs DESC LIMIT :limit")
    fun observeRecentMilestonesForOwner(accountOwnerId: String, limit: Int = 10): Flow<List<ShadowBondMilestoneEntity>>
}
