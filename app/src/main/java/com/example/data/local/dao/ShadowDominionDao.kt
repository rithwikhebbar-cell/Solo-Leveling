package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ShadowDominionActivityEntity
import com.example.data.local.entity.ShadowDominionEntity
import com.example.data.local.entity.ShadowDominionResearchEntity
import com.example.data.local.entity.ShadowGarrisonEntity
import com.example.data.local.entity.ShadowInfrastructureEntity
import com.example.data.local.entity.ShadowTerritoryEntity
import com.example.domain.model.DEFAULT_SHADOW_ACCOUNT_OWNER_ID
import kotlinx.coroutines.flow.Flow

// ============================================================================
// PHASE 6.6 — ROOM DAO FOR SHADOW DOMINION & REALM EXPANSION
// ============================================================================

@Dao
interface ShadowDominionDao {

    // -------------------------------------------------------------------------
    // 1. SHADOW DOMINION PROFILE
    // -------------------------------------------------------------------------

    @Query("SELECT * FROM shadow_dominion WHERE accountOwnerId = :accountOwnerId LIMIT 1")
    fun observeDominionForOwner(accountOwnerId: String): Flow<ShadowDominionEntity?>

    fun observeDominion(): Flow<ShadowDominionEntity?> =
        observeDominionForOwner(DEFAULT_SHADOW_ACCOUNT_OWNER_ID)

    @Query("SELECT * FROM shadow_dominion WHERE accountOwnerId = :accountOwnerId LIMIT 1")
    suspend fun getDominionForOwnerOnce(accountOwnerId: String): ShadowDominionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDominion(entity: ShadowDominionEntity): Long

    @Query("UPDATE shadow_dominion SET dominionLevel = :level, developmentStageName = :stageName, citadelLevel = :citadelLevel, updatedAtEpochMs = :updatedAtMs WHERE accountOwnerId = :accountOwnerId")
    suspend fun updateDominionLevelAndStage(
        accountOwnerId: String,
        level: Int,
        stageName: String,
        citadelLevel: Int,
        updatedAtMs: Long
    ): Int

    @Query("UPDATE shadow_dominion SET controlledTerritoriesCount = :count, updatedAtEpochMs = :updatedAtMs WHERE accountOwnerId = :accountOwnerId")
    suspend fun updateTerritoryCount(accountOwnerId: String, count: Int, updatedAtMs: Long): Int

    @Query("UPDATE shadow_dominion SET infrastructureCount = :count, updatedAtEpochMs = :updatedAtMs WHERE accountOwnerId = :accountOwnerId")
    suspend fun updateInfrastructureCount(accountOwnerId: String, count: Int, updatedAtMs: Long): Int

    // -------------------------------------------------------------------------
    // 2. TERRITORIES
    // -------------------------------------------------------------------------

    @Query("SELECT * FROM shadow_territories WHERE accountOwnerId = :accountOwnerId ORDER BY territoryId ASC")
    fun observeTerritoriesForOwner(accountOwnerId: String): Flow<List<ShadowTerritoryEntity>>

    @Query("SELECT * FROM shadow_territories WHERE accountOwnerId = :accountOwnerId ORDER BY territoryId ASC")
    suspend fun getAllTerritoriesForOwnerOnce(accountOwnerId: String): List<ShadowTerritoryEntity>

    @Query("SELECT * FROM shadow_territories WHERE territoryId = :territoryId AND accountOwnerId = :accountOwnerId LIMIT 1")
    suspend fun getTerritoryById(territoryId: String, accountOwnerId: String): ShadowTerritoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTerritory(entity: ShadowTerritoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTerritories(entities: List<ShadowTerritoryEntity>)

    @Update
    suspend fun updateTerritory(entity: ShadowTerritoryEntity): Int

    @Query("UPDATE shadow_territories SET stateName = :stateName, explorationProgressPercent = :progressPercent, explorationEndsAtEpochMs = :endsAtMs WHERE territoryId = :territoryId AND accountOwnerId = :accountOwnerId")
    suspend fun updateTerritoryExplorationState(
        territoryId: String,
        accountOwnerId: String,
        stateName: String,
        progressPercent: Int,
        endsAtMs: Long?
    ): Int

    @Query("UPDATE shadow_territories SET stateName = 'CONTROLLED', claimedAtEpochMs = :claimedAtMs, explorationProgressPercent = 100, explorationEndsAtEpochMs = NULL WHERE territoryId = :territoryId AND accountOwnerId = :accountOwnerId")
    suspend fun claimTerritory(territoryId: String, accountOwnerId: String, claimedAtMs: Long): Int

    @Query("UPDATE shadow_territories SET assignedSquadId = :squadId WHERE territoryId = :territoryId AND accountOwnerId = :accountOwnerId")
    suspend fun assignSquadToTerritory(territoryId: String, accountOwnerId: String, squadId: String?): Int

    // -------------------------------------------------------------------------
    // 3. INFRASTRUCTURES
    // -------------------------------------------------------------------------

    @Query("SELECT * FROM shadow_infrastructures WHERE accountOwnerId = :accountOwnerId ORDER BY updatedAtEpochMs DESC")
    fun observeInfrastructuresForOwner(accountOwnerId: String): Flow<List<ShadowInfrastructureEntity>>

    @Query("SELECT * FROM shadow_infrastructures WHERE territoryId = :territoryId AND accountOwnerId = :accountOwnerId ORDER BY structureId ASC")
    fun observeInfrastructuresForTerritory(territoryId: String, accountOwnerId: String): Flow<List<ShadowInfrastructureEntity>>

    @Query("SELECT * FROM shadow_infrastructures WHERE accountOwnerId = :accountOwnerId ORDER BY updatedAtEpochMs DESC")
    suspend fun getAllInfrastructuresForOwnerOnce(accountOwnerId: String): List<ShadowInfrastructureEntity>

    @Query("SELECT * FROM shadow_infrastructures WHERE territoryId = :territoryId AND accountOwnerId = :accountOwnerId")
    suspend fun getInfrastructuresForTerritoryOnce(territoryId: String, accountOwnerId: String): List<ShadowInfrastructureEntity>

    @Query("SELECT * FROM shadow_infrastructures WHERE structureId = :structureId AND accountOwnerId = :accountOwnerId LIMIT 1")
    suspend fun getInfrastructureById(structureId: String, accountOwnerId: String): ShadowInfrastructureEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInfrastructure(entity: ShadowInfrastructureEntity): Long

    @Update
    suspend fun updateInfrastructure(entity: ShadowInfrastructureEntity): Int

    @Query("DELETE FROM shadow_infrastructures WHERE structureId = :structureId AND accountOwnerId = :accountOwnerId")
    suspend fun deleteInfrastructure(structureId: String, accountOwnerId: String): Int

    // -------------------------------------------------------------------------
    // 4. GARRISONS
    // -------------------------------------------------------------------------

    @Query("SELECT * FROM shadow_garrisons WHERE accountOwnerId = :accountOwnerId")
    fun observeGarrisonsForOwner(accountOwnerId: String): Flow<List<ShadowGarrisonEntity>>

    @Query("SELECT * FROM shadow_garrisons WHERE territoryId = :territoryId AND accountOwnerId = :accountOwnerId LIMIT 1")
    fun observeGarrisonForTerritory(territoryId: String, accountOwnerId: String): Flow<ShadowGarrisonEntity?>

    @Query("SELECT * FROM shadow_garrisons WHERE accountOwnerId = :accountOwnerId")
    suspend fun getAllGarrisonsForOwnerOnce(accountOwnerId: String): List<ShadowGarrisonEntity>

    @Query("SELECT * FROM shadow_garrisons WHERE territoryId = :territoryId AND accountOwnerId = :accountOwnerId LIMIT 1")
    suspend fun getGarrisonForTerritoryOnce(territoryId: String, accountOwnerId: String): ShadowGarrisonEntity?

    @Query("SELECT * FROM shadow_garrisons WHERE garrisonId = :garrisonId AND accountOwnerId = :accountOwnerId LIMIT 1")
    suspend fun getGarrisonById(garrisonId: String, accountOwnerId: String): ShadowGarrisonEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGarrison(entity: ShadowGarrisonEntity): Long

    @Query("DELETE FROM shadow_garrisons WHERE garrisonId = :garrisonId AND accountOwnerId = :accountOwnerId")
    suspend fun deleteGarrison(garrisonId: String, accountOwnerId: String): Int

    // -------------------------------------------------------------------------
    // 5. RESEARCH
    // -------------------------------------------------------------------------

    @Query("SELECT * FROM shadow_dominion_research WHERE accountOwnerId = :accountOwnerId ORDER BY branchName ASC, level ASC")
    fun observeResearchForOwner(accountOwnerId: String): Flow<List<ShadowDominionResearchEntity>>

    @Query("SELECT * FROM shadow_dominion_research WHERE accountOwnerId = :accountOwnerId ORDER BY branchName ASC, level ASC")
    suspend fun getAllResearchForOwnerOnce(accountOwnerId: String): List<ShadowDominionResearchEntity>

    @Query("SELECT * FROM shadow_dominion_research WHERE projectKey = :projectKey AND accountOwnerId = :accountOwnerId LIMIT 1")
    suspend fun getResearchByProjectKey(accountOwnerId: String, projectKey: String): ShadowDominionResearchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertResearch(entity: ShadowDominionResearchEntity): Long

    // -------------------------------------------------------------------------
    // 6. ACTIVITIES
    // -------------------------------------------------------------------------

    @Query("SELECT * FROM shadow_dominion_activities WHERE accountOwnerId = :accountOwnerId ORDER BY timestampEpochMs DESC LIMIT :limit")
    fun observeRecentActivitiesForOwner(accountOwnerId: String, limit: Int = 50): Flow<List<ShadowDominionActivityEntity>>

    @Query("SELECT * FROM shadow_dominion_activities WHERE accountOwnerId = :accountOwnerId ORDER BY timestampEpochMs DESC")
    suspend fun getAllActivitiesForOwnerOnce(accountOwnerId: String): List<ShadowDominionActivityEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(entity: ShadowDominionActivityEntity): Long
}
