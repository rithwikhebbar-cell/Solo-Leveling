package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.MonarchSkillUnlockEntity
import com.example.data.local.entity.ShadowDeploymentEntity
import com.example.data.local.entity.ShadowEntity
import com.example.data.local.entity.ShadowExtractionEntity
import com.example.data.local.entity.ShadowFormationEntity
import com.example.data.local.entity.ShadowMonarchProfileEntity
import com.example.data.local.entity.ShadowQuestProgressEntity
import com.example.data.local.entity.ShadowResourceTransactionEntity
import com.example.data.local.entity.ShadowSkillUnlockEntity
import kotlinx.coroutines.flow.Flow

// ============================================================================
// MILESTONE 11 — SHADOW MONARCH DAOs
// ============================================================================

@Dao
interface ShadowMonarchProfileDao {
    @Query("SELECT * FROM shadow_monarch_profile WHERE id = 1 LIMIT 1")
    fun observeProfile(): Flow<ShadowMonarchProfileEntity?>

    @Query("SELECT * FROM shadow_monarch_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfileOnce(): ShadowMonarchProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(entity: ShadowMonarchProfileEntity)
}

@Dao
interface ShadowDao {
    @Query("SELECT * FROM shadows WHERE isRetired = 0 ORDER BY extractedAtEpochMs DESC")
    fun observeActiveShadows(): Flow<List<ShadowEntity>>

    @Query("SELECT * FROM shadows ORDER BY extractedAtEpochMs DESC")
    fun observeAllShadows(): Flow<List<ShadowEntity>>

    @Query("SELECT * FROM shadows WHERE shadowId = :shadowId LIMIT 1")
    suspend fun getShadowById(shadowId: String): ShadowEntity?

    @Query("SELECT * FROM shadows WHERE isRetired = 0 ORDER BY extractedAtEpochMs DESC")
    suspend fun getActiveShadowsOnce(): List<ShadowEntity>

    @Query("SELECT * FROM shadows ORDER BY extractedAtEpochMs DESC")
    suspend fun getAllShadowsOnce(): List<ShadowEntity>

    @Query("SELECT COUNT(*) FROM shadows WHERE isRetired = 0")
    suspend fun getActiveShadowCount(): Int

    @Query("SELECT COUNT(*) FROM shadows")
    suspend fun getTotalShadowCount(): Int

    @Query("SELECT * FROM shadows WHERE shadowClassName = :className AND isRetired = 0 ORDER BY level DESC")
    suspend fun getShadowsByClass(className: String): List<ShadowEntity>

    @Query("SELECT * FROM shadows WHERE currentRankName = :rankName AND isRetired = 0 ORDER BY level DESC")
    suspend fun getShadowsByRank(rankName: String): List<ShadowEntity>

    @Query("SELECT * FROM shadows WHERE isRetired = 0 ORDER BY level DESC LIMIT 1")
    suspend fun getHighestLevelShadow(): ShadowEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertShadow(entity: ShadowEntity): Long

    @Update
    suspend fun updateShadow(entity: ShadowEntity)

    @Query("UPDATE shadows SET isRetired = 1 WHERE shadowId = :shadowId")
    suspend fun retireShadow(shadowId: String)

    @Query("UPDATE shadows SET deploymentStatusName = :statusName WHERE shadowId = :shadowId")
    suspend fun updateDeploymentStatus(shadowId: String, statusName: String)

    @Query("UPDATE shadows SET equippedFormationId = :formationId WHERE shadowId = :shadowId")
    suspend fun setFormation(shadowId: String, formationId: String?)

    @Query("DELETE FROM shadows WHERE shadowId = :shadowId")
    suspend fun deleteShadow(shadowId: String)
}

@Dao
interface ShadowExtractionDao {
    @Query("SELECT * FROM shadow_extractions ORDER BY timestampEpochMs DESC")
    fun observeExtractions(): Flow<List<ShadowExtractionEntity>>

    @Query("SELECT * FROM shadow_extractions ORDER BY timestampEpochMs DESC")
    suspend fun getAllExtractionsOnce(): List<ShadowExtractionEntity>

    @Query("SELECT * FROM shadow_extractions WHERE extractionId = :extractionId LIMIT 1")
    suspend fun getExtractionById(extractionId: String): ShadowExtractionEntity?

    @Query("SELECT * FROM shadow_extractions WHERE sourceEventId = :sourceEventId LIMIT 1")
    suspend fun getExtractionBySourceEventId(sourceEventId: String): ShadowExtractionEntity?

    @Query("SELECT * FROM shadow_extractions WHERE sourceTypeName = :sourceTypeName AND sourceEventId = :sourceEventId LIMIT 1")
    suspend fun getExtractionBySource(sourceTypeName: String, sourceEventId: String): ShadowExtractionEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExtraction(entity: ShadowExtractionEntity): Long

    @Update
    suspend fun updateExtraction(entity: ShadowExtractionEntity)

    @Query("SELECT COUNT(*) FROM shadow_extractions WHERE outcomeName = 'SUCCESS' AND isConfirmed = 1")
    suspend fun getConfirmedExtractionCount(): Int
}

@Dao
interface ShadowSkillUnlockDao {
    @Query("SELECT * FROM shadow_skill_unlocks WHERE shadowId = :shadowId")
    fun observeUnlocksForShadow(shadowId: String): Flow<List<ShadowSkillUnlockEntity>>

    @Query("SELECT * FROM shadow_skill_unlocks WHERE shadowId = :shadowId")
    suspend fun getUnlocksForShadowOnce(shadowId: String): List<ShadowSkillUnlockEntity>

    @Query("SELECT * FROM shadow_skill_unlocks WHERE shadowId = :shadowId AND skillId = :skillId LIMIT 1")
    suspend fun getUnlock(shadowId: String, skillId: String): ShadowSkillUnlockEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUnlock(entity: ShadowSkillUnlockEntity): Long
}

@Dao
interface ShadowDeploymentDao {
    @Query("SELECT * FROM shadow_deployments WHERE statusName = 'DEPLOYED' ORDER BY assignedAtEpochMs DESC")
    fun observeActiveDeployments(): Flow<List<ShadowDeploymentEntity>>

    @Query("SELECT * FROM shadow_deployments ORDER BY assignedAtEpochMs DESC")
    fun observeAllDeployments(): Flow<List<ShadowDeploymentEntity>>

    @Query("SELECT * FROM shadow_deployments WHERE shadowId = :shadowId AND statusName = 'DEPLOYED' LIMIT 1")
    suspend fun getActiveDeploymentForShadow(shadowId: String): ShadowDeploymentEntity?

    @Query("SELECT * FROM shadow_deployments WHERE deploymentId = :deploymentId LIMIT 1")
    suspend fun getDeploymentById(deploymentId: String): ShadowDeploymentEntity?

    @Query("SELECT * FROM shadow_deployments WHERE statusName = 'DEPLOYED'")
    suspend fun getActiveDeploymentsOnce(): List<ShadowDeploymentEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDeployment(entity: ShadowDeploymentEntity): Long

    @Update
    suspend fun updateDeployment(entity: ShadowDeploymentEntity)

    @Query("UPDATE shadow_deployments SET statusName = 'RECALLED', recalledAtEpochMs = :recalledAtMs WHERE deploymentId = :deploymentId")
    suspend fun recallDeployment(deploymentId: String, recalledAtMs: Long)
}

@Dao
interface ShadowFormationDao {
    @Query("SELECT * FROM shadow_formations ORDER BY updatedAtEpochMs DESC")
    fun observeFormations(): Flow<List<ShadowFormationEntity>>

    @Query("SELECT * FROM shadow_formations ORDER BY updatedAtEpochMs DESC")
    suspend fun getAllFormationsOnce(): List<ShadowFormationEntity>

    @Query("SELECT * FROM shadow_formations WHERE formationId = :formationId LIMIT 1")
    suspend fun getFormationById(formationId: String): ShadowFormationEntity?

    @Query("SELECT * FROM shadow_formations WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveFormationOnce(): ShadowFormationEntity?

    @Query("SELECT * FROM shadow_formations WHERE isActive = 1 LIMIT 1")
    fun observeActiveFormation(): Flow<ShadowFormationEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFormation(entity: ShadowFormationEntity)

    @Query("UPDATE shadow_formations SET isActive = 0")
    suspend fun clearAllActiveFormations()

    @Query("UPDATE shadow_formations SET isActive = 1 WHERE formationId = :formationId")
    suspend fun setActiveFormation(formationId: String)

    @Query("DELETE FROM shadow_formations WHERE formationId = :formationId")
    suspend fun deleteFormation(formationId: String)
}

@Dao
interface MonarchSkillUnlockDao {
    @Query("SELECT * FROM monarch_skill_unlocks ORDER BY unlockedAtEpochMs ASC")
    fun observeMonarchSkillUnlocks(): Flow<List<MonarchSkillUnlockEntity>>

    @Query("SELECT * FROM monarch_skill_unlocks")
    suspend fun getAllMonarchSkillUnlocksOnce(): List<MonarchSkillUnlockEntity>

    @Query("SELECT * FROM monarch_skill_unlocks WHERE skillId = :skillId LIMIT 1")
    suspend fun getMonarchSkillUnlock(skillId: String): MonarchSkillUnlockEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMonarchSkillUnlock(entity: MonarchSkillUnlockEntity): Long
}

@Dao
interface ShadowQuestProgressDao {
    @Query("SELECT * FROM shadow_quest_progress ORDER BY questId ASC")
    fun observeQuestProgress(): Flow<List<ShadowQuestProgressEntity>>

    @Query("SELECT * FROM shadow_quest_progress")
    suspend fun getAllQuestProgressOnce(): List<ShadowQuestProgressEntity>

    @Query("SELECT * FROM shadow_quest_progress WHERE questId = :questId LIMIT 1")
    suspend fun getQuestProgress(questId: String): ShadowQuestProgressEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertQuestProgress(entity: ShadowQuestProgressEntity): Long

    @Update
    suspend fun updateQuestProgress(entity: ShadowQuestProgressEntity)

    @Query("SELECT COUNT(*) FROM shadow_quest_progress WHERE isCompleted = 1 AND rewardClaimedAtEpochMs IS NOT NULL")
    suspend fun getCompletedAndClaimedCount(): Int
}

@Dao
interface ShadowResourceDao {
    @Query("SELECT * FROM shadow_resource_transactions ORDER BY timestampEpochMs DESC")
    fun observeTransactions(): Flow<List<ShadowResourceTransactionEntity>>

    @Query("SELECT * FROM shadow_resource_transactions WHERE resourceTypeName = :typeName ORDER BY timestampEpochMs DESC")
    suspend fun getTransactionsByType(typeName: String): List<ShadowResourceTransactionEntity>

    @Query("SELECT transactionId FROM shadow_resource_transactions WHERE sourceEventId = :sourceEventId AND resourceTypeName = :typeName LIMIT 1")
    suspend fun getTransactionBySourceEvent(sourceEventId: String, typeName: String): String?

    @Query("SELECT COALESCE(balanceAfter, 0) FROM shadow_resource_transactions WHERE resourceTypeName = :typeName ORDER BY timestampEpochMs DESC LIMIT 1")
    suspend fun getCurrentBalance(typeName: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(entity: ShadowResourceTransactionEntity): Long
}

