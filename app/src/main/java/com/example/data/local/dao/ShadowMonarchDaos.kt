package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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
import com.example.data.local.entity.ShadowXpTraceEntity
import com.example.data.local.entity.ShadowEvidenceClaimEntity
import com.example.data.local.entity.ShadowResonanceEntity
import com.example.domain.model.DEFAULT_SHADOW_ACCOUNT_OWNER_ID
import kotlinx.coroutines.flow.Flow

// ============================================================================
// MILESTONE 11 + MILESTONE 12 PHASE 6.1 — ACCOUNT-SCOPED SHADOW MONARCH DAOs
// ============================================================================
// NOTE: Never use OnConflictStrategy.REPLACE on ShadowMonarchProfileEntity or
// owned parent tables because SQLite REPLACE triggers DELETE + INSERT, firing
// ON DELETE CASCADE on child tables. Always use IGNORE + explicit UPDATE.
// ============================================================================

@Dao
interface ShadowMonarchProfileDao {
    @Query("SELECT * FROM shadow_monarch_profile WHERE accountOwnerId = :accountOwnerId LIMIT 1")
    fun observeProfileForOwner(accountOwnerId: String): Flow<ShadowMonarchProfileEntity?>

    fun observeProfile(): Flow<ShadowMonarchProfileEntity?> =
        observeProfileForOwner(DEFAULT_SHADOW_ACCOUNT_OWNER_ID)

    @Query("SELECT * FROM shadow_monarch_profile WHERE accountOwnerId = :accountOwnerId LIMIT 1")
    suspend fun getProfileForOwnerOnce(accountOwnerId: String): ShadowMonarchProfileEntity?

    suspend fun getProfileOnce(): ShadowMonarchProfileEntity? =
        getProfileForOwnerOnce(DEFAULT_SHADOW_ACCOUNT_OWNER_ID)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProfileIgnore(entity: ShadowMonarchProfileEntity): Long

    @Update
    suspend fun updateProfile(entity: ShadowMonarchProfileEntity): Int

    @Transaction
    suspend fun upsertProfile(entity: ShadowMonarchProfileEntity) {
        val inserted = insertProfileIgnore(entity)
        if (inserted == -1L) {
            updateProfile(entity)
        }
    }
}

@Dao
interface ShadowDao {
    @Query("SELECT * FROM shadows WHERE accountOwnerId = :accountOwnerId AND isRetired = 0 ORDER BY extractedAtEpochMs DESC")
    fun observeActiveShadowsForOwner(accountOwnerId: String): Flow<List<ShadowEntity>>

    fun observeActiveShadows(): Flow<List<ShadowEntity>> =
        observeActiveShadowsForOwner(DEFAULT_SHADOW_ACCOUNT_OWNER_ID)

    @Query("SELECT * FROM shadows WHERE accountOwnerId = :accountOwnerId ORDER BY extractedAtEpochMs DESC")
    fun observeAllShadowsForOwner(accountOwnerId: String): Flow<List<ShadowEntity>>

    fun observeAllShadows(): Flow<List<ShadowEntity>> =
        observeAllShadowsForOwner(DEFAULT_SHADOW_ACCOUNT_OWNER_ID)

    @Query("SELECT * FROM shadows WHERE shadowId = :shadowId AND accountOwnerId = :accountOwnerId LIMIT 1")
    suspend fun getShadowByIdForOwner(shadowId: String, accountOwnerId: String): ShadowEntity?

    @Query("SELECT * FROM shadows WHERE shadowId = :shadowId LIMIT 1")
    suspend fun getShadowById(shadowId: String): ShadowEntity?

    @Query("SELECT * FROM shadows WHERE accountOwnerId = :accountOwnerId AND isRetired = 0 ORDER BY extractedAtEpochMs DESC")
    suspend fun getActiveShadowsForOwnerOnce(accountOwnerId: String): List<ShadowEntity>

    suspend fun getActiveShadowsOnce(): List<ShadowEntity> =
        getActiveShadowsForOwnerOnce(DEFAULT_SHADOW_ACCOUNT_OWNER_ID)

    @Query("SELECT * FROM shadows WHERE accountOwnerId = :accountOwnerId ORDER BY extractedAtEpochMs DESC")
    suspend fun getAllShadowsForOwnerOnce(accountOwnerId: String): List<ShadowEntity>

    suspend fun getAllShadowsOnce(): List<ShadowEntity> =
        getAllShadowsForOwnerOnce(DEFAULT_SHADOW_ACCOUNT_OWNER_ID)

    @Query("SELECT COUNT(*) FROM shadows WHERE accountOwnerId = :accountOwnerId AND isRetired = 0")
    suspend fun getActiveShadowCountForOwner(accountOwnerId: String): Int

    suspend fun getActiveShadowCount(): Int =
        getActiveShadowCountForOwner(DEFAULT_SHADOW_ACCOUNT_OWNER_ID)

    @Query("SELECT COUNT(*) FROM shadows WHERE accountOwnerId = :accountOwnerId")
    suspend fun getTotalShadowCountForOwner(accountOwnerId: String): Int

    suspend fun getTotalShadowCount(): Int =
        getTotalShadowCountForOwner(DEFAULT_SHADOW_ACCOUNT_OWNER_ID)

    @Query("SELECT * FROM shadows WHERE accountOwnerId = :accountOwnerId AND shadowClassName = :className AND isRetired = 0 ORDER BY level DESC")
    suspend fun getShadowsByClassForOwner(accountOwnerId: String, className: String): List<ShadowEntity>

    @Query("SELECT * FROM shadows WHERE accountOwnerId = :accountOwnerId AND currentRankName = :rankName AND isRetired = 0 ORDER BY level DESC")
    suspend fun getShadowsByRankForOwner(accountOwnerId: String, rankName: String): List<ShadowEntity>

    @Query("SELECT * FROM shadows WHERE accountOwnerId = :accountOwnerId AND isRetired = 0 ORDER BY level DESC LIMIT 1")
    suspend fun getHighestLevelShadowForOwner(accountOwnerId: String): ShadowEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertShadow(entity: ShadowEntity): Long

    @Update
    suspend fun updateShadow(entity: ShadowEntity): Int

    @Query("UPDATE shadows SET isRetired = 1 WHERE shadowId = :shadowId AND accountOwnerId = :accountOwnerId")
    suspend fun retireShadowForOwner(shadowId: String, accountOwnerId: String): Int

    @Query("UPDATE shadows SET deploymentStatusName = :statusName WHERE shadowId = :shadowId AND accountOwnerId = :accountOwnerId")
    suspend fun updateDeploymentStatusForOwner(shadowId: String, accountOwnerId: String, statusName: String): Int

    @Query("UPDATE shadows SET equippedFormationId = :formationId WHERE shadowId = :shadowId AND accountOwnerId = :accountOwnerId")
    suspend fun setFormationForOwner(shadowId: String, accountOwnerId: String, formationId: String?): Int

    @Query("DELETE FROM shadows WHERE shadowId = :shadowId AND accountOwnerId = :accountOwnerId")
    suspend fun deleteShadowForOwner(shadowId: String, accountOwnerId: String): Int
}

@Dao
interface ShadowExtractionDao {
    @Query("SELECT * FROM shadow_extractions WHERE accountOwnerId = :accountOwnerId ORDER BY timestampEpochMs DESC")
    fun observeExtractionsForOwner(accountOwnerId: String): Flow<List<ShadowExtractionEntity>>

    fun observeExtractions(): Flow<List<ShadowExtractionEntity>> =
        observeExtractionsForOwner(DEFAULT_SHADOW_ACCOUNT_OWNER_ID)

    @Query("SELECT * FROM shadow_extractions WHERE accountOwnerId = :accountOwnerId ORDER BY timestampEpochMs DESC")
    suspend fun getAllExtractionsForOwnerOnce(accountOwnerId: String): List<ShadowExtractionEntity>

    suspend fun getAllExtractionsOnce(): List<ShadowExtractionEntity> =
        getAllExtractionsForOwnerOnce(DEFAULT_SHADOW_ACCOUNT_OWNER_ID)

    @Query("SELECT * FROM shadow_extractions WHERE extractionId = :extractionId AND accountOwnerId = :accountOwnerId LIMIT 1")
    suspend fun getExtractionByIdForOwner(extractionId: String, accountOwnerId: String): ShadowExtractionEntity?

    @Query("SELECT * FROM shadow_extractions WHERE extractionId = :extractionId LIMIT 1")
    suspend fun getExtractionById(extractionId: String): ShadowExtractionEntity?

    @Query("SELECT * FROM shadow_extractions WHERE accountOwnerId = :accountOwnerId AND sourceEventId = :sourceEventId LIMIT 1")
    suspend fun getExtractionBySourceEventIdForOwner(accountOwnerId: String, sourceEventId: String): ShadowExtractionEntity?

    @Query("SELECT * FROM shadow_extractions WHERE accountOwnerId = :accountOwnerId AND sourceTypeName = :sourceTypeName AND sourceEventId = :sourceEventId LIMIT 1")
    suspend fun getExtractionBySourceForOwner(
        accountOwnerId: String,
        sourceTypeName: String,
        sourceEventId: String
    ): ShadowExtractionEntity?

    suspend fun getExtractionBySource(sourceTypeName: String, sourceEventId: String): ShadowExtractionEntity? =
        getExtractionBySourceForOwner(DEFAULT_SHADOW_ACCOUNT_OWNER_ID, sourceTypeName, sourceEventId)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExtraction(entity: ShadowExtractionEntity): Long

    @Update
    suspend fun updateExtraction(entity: ShadowExtractionEntity): Int

    @Query("SELECT COUNT(*) FROM shadow_extractions WHERE accountOwnerId = :accountOwnerId AND outcomeName = 'SUCCESS' AND isConfirmed = 1")
    suspend fun getConfirmedExtractionCountForOwner(accountOwnerId: String): Int
}

@Dao
interface ShadowSkillUnlockDao {
    @Query("SELECT * FROM shadow_skill_unlocks WHERE accountOwnerId = :accountOwnerId AND shadowId = :shadowId")
    fun observeUnlocksForShadow(accountOwnerId: String, shadowId: String): Flow<List<ShadowSkillUnlockEntity>>

    @Query("SELECT * FROM shadow_skill_unlocks WHERE accountOwnerId = :accountOwnerId AND shadowId = :shadowId")
    suspend fun getUnlocksForShadowForOwnerOnce(accountOwnerId: String, shadowId: String): List<ShadowSkillUnlockEntity>

    @Query("SELECT * FROM shadow_skill_unlocks WHERE shadowId = :shadowId")
    suspend fun getUnlocksForShadowOnce(shadowId: String): List<ShadowSkillUnlockEntity>

    @Query("SELECT * FROM shadow_skill_unlocks WHERE accountOwnerId = :accountOwnerId")
    suspend fun getAllUnlocksForOwnerOnce(accountOwnerId: String): List<ShadowSkillUnlockEntity>

    @Query("SELECT * FROM shadow_skill_unlocks WHERE accountOwnerId = :accountOwnerId AND shadowId = :shadowId AND skillId = :skillId LIMIT 1")
    suspend fun getUnlockForOwner(accountOwnerId: String, shadowId: String, skillId: String): ShadowSkillUnlockEntity?

    suspend fun getUnlock(shadowId: String, skillId: String): ShadowSkillUnlockEntity? =
        getUnlockForOwner(DEFAULT_SHADOW_ACCOUNT_OWNER_ID, shadowId, skillId)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUnlock(entity: ShadowSkillUnlockEntity): Long
}

@Dao
interface ShadowDeploymentDao {
    @Query("SELECT * FROM shadow_deployments WHERE accountOwnerId = :accountOwnerId AND statusName = 'DEPLOYED' ORDER BY assignedAtEpochMs DESC")
    fun observeActiveDeploymentsForOwner(accountOwnerId: String): Flow<List<ShadowDeploymentEntity>>

    fun observeActiveDeployments(): Flow<List<ShadowDeploymentEntity>> =
        observeActiveDeploymentsForOwner(DEFAULT_SHADOW_ACCOUNT_OWNER_ID)

    @Query("SELECT * FROM shadow_deployments WHERE accountOwnerId = :accountOwnerId ORDER BY assignedAtEpochMs DESC")
    fun observeAllDeploymentsForOwner(accountOwnerId: String): Flow<List<ShadowDeploymentEntity>>

    @Query("SELECT * FROM shadow_deployments WHERE accountOwnerId = :accountOwnerId ORDER BY assignedAtEpochMs DESC")
    suspend fun getAllDeploymentsForOwnerOnce(accountOwnerId: String): List<ShadowDeploymentEntity>

    @Query("SELECT * FROM shadow_deployments WHERE accountOwnerId = :accountOwnerId AND shadowId = :shadowId AND statusName = 'DEPLOYED' LIMIT 1")
    suspend fun getActiveDeploymentForShadow(accountOwnerId: String, shadowId: String): ShadowDeploymentEntity?

    @Query("SELECT * FROM shadow_deployments WHERE deploymentId = :deploymentId AND accountOwnerId = :accountOwnerId LIMIT 1")
    suspend fun getDeploymentByIdForOwner(deploymentId: String, accountOwnerId: String): ShadowDeploymentEntity?

    @Query("SELECT * FROM shadow_deployments WHERE deploymentId = :deploymentId LIMIT 1")
    suspend fun getDeploymentById(deploymentId: String): ShadowDeploymentEntity?

    @Query("SELECT * FROM shadow_deployments WHERE accountOwnerId = :accountOwnerId AND statusName = 'DEPLOYED'")
    suspend fun getActiveDeploymentsForOwnerOnce(accountOwnerId: String): List<ShadowDeploymentEntity>

    suspend fun getActiveDeploymentsOnce(): List<ShadowDeploymentEntity> =
        getActiveDeploymentsForOwnerOnce(DEFAULT_SHADOW_ACCOUNT_OWNER_ID)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDeployment(entity: ShadowDeploymentEntity): Long

    @Update
    suspend fun updateDeployment(entity: ShadowDeploymentEntity): Int

    @Query("UPDATE shadow_deployments SET statusName = 'RECALLED', recalledAtEpochMs = :recalledAtMs WHERE deploymentId = :deploymentId AND accountOwnerId = :accountOwnerId AND statusName = 'DEPLOYED'")
    suspend fun recallDeploymentForOwner(deploymentId: String, accountOwnerId: String, recalledAtMs: Long): Int
}

@Dao
interface ShadowFormationDao {
    @Query("SELECT * FROM shadow_formations WHERE accountOwnerId = :accountOwnerId ORDER BY updatedAtEpochMs DESC")
    fun observeFormationsForOwner(accountOwnerId: String): Flow<List<ShadowFormationEntity>>

    fun observeFormations(): Flow<List<ShadowFormationEntity>> =
        observeFormationsForOwner(DEFAULT_SHADOW_ACCOUNT_OWNER_ID)

    @Query("SELECT * FROM shadow_formations WHERE accountOwnerId = :accountOwnerId ORDER BY updatedAtEpochMs DESC")
    suspend fun getAllFormationsForOwnerOnce(accountOwnerId: String): List<ShadowFormationEntity>

    suspend fun getAllFormationsOnce(): List<ShadowFormationEntity> =
        getAllFormationsForOwnerOnce(DEFAULT_SHADOW_ACCOUNT_OWNER_ID)

    @Query("SELECT * FROM shadow_formations WHERE formationId = :formationId AND accountOwnerId = :accountOwnerId LIMIT 1")
    suspend fun getFormationByIdForOwner(formationId: String, accountOwnerId: String): ShadowFormationEntity?

    @Query("SELECT * FROM shadow_formations WHERE formationId = :formationId LIMIT 1")
    suspend fun getFormationById(formationId: String): ShadowFormationEntity?

    @Query("SELECT * FROM shadow_formations WHERE accountOwnerId = :accountOwnerId AND isActive = 1 LIMIT 1")
    suspend fun getActiveFormationForOwnerOnce(accountOwnerId: String): ShadowFormationEntity?

    @Query("SELECT * FROM shadow_formations WHERE accountOwnerId = :accountOwnerId AND isActive = 1 LIMIT 1")
    fun observeActiveFormationForOwner(accountOwnerId: String): Flow<ShadowFormationEntity?>

    fun observeActiveFormation(): Flow<ShadowFormationEntity?> =
        observeActiveFormationForOwner(DEFAULT_SHADOW_ACCOUNT_OWNER_ID)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFormationIgnore(entity: ShadowFormationEntity): Long

    @Update
    suspend fun updateFormation(entity: ShadowFormationEntity): Int

    @Query("UPDATE shadow_formations SET isActive = 0 WHERE accountOwnerId = :accountOwnerId")
    suspend fun clearAllActiveFormationsForOwner(accountOwnerId: String)

    @Query("UPDATE shadow_formations SET isActive = 1, updatedAtEpochMs = :updatedAtMs WHERE formationId = :formationId AND accountOwnerId = :accountOwnerId")
    suspend fun setActiveFormationForOwner(formationId: String, accountOwnerId: String, updatedAtMs: Long): Int

    @Query("DELETE FROM shadow_formations WHERE formationId = :formationId AND accountOwnerId = :accountOwnerId")
    suspend fun deleteFormationForOwner(formationId: String, accountOwnerId: String): Int
}

@Dao
interface MonarchSkillUnlockDao {
    @Query("SELECT * FROM monarch_skill_unlocks WHERE accountOwnerId = :accountOwnerId ORDER BY unlockedAtEpochMs ASC")
    fun observeMonarchSkillUnlocksForOwner(accountOwnerId: String): Flow<List<MonarchSkillUnlockEntity>>

    fun observeMonarchSkillUnlocks(): Flow<List<MonarchSkillUnlockEntity>> =
        observeMonarchSkillUnlocksForOwner(DEFAULT_SHADOW_ACCOUNT_OWNER_ID)

    @Query("SELECT * FROM monarch_skill_unlocks WHERE accountOwnerId = :accountOwnerId")
    suspend fun getAllMonarchSkillUnlocksForOwnerOnce(accountOwnerId: String): List<MonarchSkillUnlockEntity>

    suspend fun getAllMonarchSkillUnlocksOnce(): List<MonarchSkillUnlockEntity> =
        getAllMonarchSkillUnlocksForOwnerOnce(DEFAULT_SHADOW_ACCOUNT_OWNER_ID)

    @Query("SELECT * FROM monarch_skill_unlocks WHERE accountOwnerId = :accountOwnerId AND skillId = :skillId LIMIT 1")
    suspend fun getMonarchSkillUnlockForOwner(accountOwnerId: String, skillId: String): MonarchSkillUnlockEntity?

    suspend fun getMonarchSkillUnlock(skillId: String): MonarchSkillUnlockEntity? =
        getMonarchSkillUnlockForOwner(DEFAULT_SHADOW_ACCOUNT_OWNER_ID, skillId)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMonarchSkillUnlock(entity: MonarchSkillUnlockEntity): Long
}

@Dao
interface ShadowQuestProgressDao {
    @Query("SELECT * FROM shadow_quest_progress WHERE accountOwnerId = :accountOwnerId ORDER BY questId ASC")
    fun observeQuestProgressForOwner(accountOwnerId: String): Flow<List<ShadowQuestProgressEntity>>

    fun observeQuestProgress(): Flow<List<ShadowQuestProgressEntity>> =
        observeQuestProgressForOwner(DEFAULT_SHADOW_ACCOUNT_OWNER_ID)

    @Query("SELECT * FROM shadow_quest_progress WHERE accountOwnerId = :accountOwnerId")
    suspend fun getAllQuestProgressForOwnerOnce(accountOwnerId: String): List<ShadowQuestProgressEntity>

    suspend fun getAllQuestProgressOnce(): List<ShadowQuestProgressEntity> =
        getAllQuestProgressForOwnerOnce(DEFAULT_SHADOW_ACCOUNT_OWNER_ID)

    @Query("SELECT * FROM shadow_quest_progress WHERE accountOwnerId = :accountOwnerId AND questId = :questId LIMIT 1")
    suspend fun getQuestProgressForOwner(accountOwnerId: String, questId: String): ShadowQuestProgressEntity?

    suspend fun getQuestProgress(questId: String): ShadowQuestProgressEntity? =
        getQuestProgressForOwner(DEFAULT_SHADOW_ACCOUNT_OWNER_ID, questId)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertQuestProgress(entity: ShadowQuestProgressEntity): Long

    @Update
    suspend fun updateQuestProgress(entity: ShadowQuestProgressEntity): Int

    @Query("SELECT COUNT(*) FROM shadow_quest_progress WHERE accountOwnerId = :accountOwnerId AND isCompleted = 1 AND rewardClaimedAtEpochMs IS NOT NULL")
    suspend fun getCompletedAndClaimedCountForOwner(accountOwnerId: String): Int
}

@Dao
interface ShadowResourceDao {
    @Query("SELECT * FROM shadow_resource_transactions WHERE accountOwnerId = :accountOwnerId ORDER BY timestampEpochMs DESC, rowid DESC")
    fun observeTransactionsForOwner(accountOwnerId: String): Flow<List<ShadowResourceTransactionEntity>>

    fun observeTransactions(): Flow<List<ShadowResourceTransactionEntity>> =
        observeTransactionsForOwner(DEFAULT_SHADOW_ACCOUNT_OWNER_ID)

    @Query("SELECT * FROM shadow_resource_transactions WHERE accountOwnerId = :accountOwnerId ORDER BY timestampEpochMs DESC, rowid DESC")
    suspend fun getAllTransactionsForOwnerOnce(accountOwnerId: String): List<ShadowResourceTransactionEntity>

    @Query("SELECT * FROM shadow_resource_transactions WHERE accountOwnerId = :accountOwnerId AND resourceTypeName = :typeName ORDER BY timestampEpochMs DESC, rowid DESC")
    suspend fun getTransactionsByTypeForOwner(accountOwnerId: String, typeName: String): List<ShadowResourceTransactionEntity>

    @Query("SELECT transactionId FROM shadow_resource_transactions WHERE accountOwnerId = :accountOwnerId AND sourceEventId = :sourceEventId AND resourceTypeName = :typeName LIMIT 1")
    suspend fun getTransactionBySourceEventForOwner(accountOwnerId: String, sourceEventId: String, typeName: String): String?

    @Query("SELECT transactionId FROM shadow_resource_transactions WHERE accountOwnerId = :accountOwnerId AND sourceEventId = :sourceEventId AND resourceTypeName = :typeName AND transactionTypeName = :transactionTypeName LIMIT 1")
    suspend fun getTransactionBySourceEventAndTypeForOwner(
        accountOwnerId: String,
        sourceEventId: String,
        typeName: String,
        transactionTypeName: String
    ): String?

    @Query("SELECT COALESCE(balanceAfter, 0) FROM shadow_resource_transactions WHERE accountOwnerId = :accountOwnerId AND resourceTypeName = :typeName ORDER BY timestampEpochMs DESC, rowid DESC LIMIT 1")
    suspend fun getCurrentBalanceForOwner(accountOwnerId: String, typeName: String): Int

    suspend fun getCurrentBalance(typeName: String): Int =
        getCurrentBalanceForOwner(DEFAULT_SHADOW_ACCOUNT_OWNER_ID, typeName)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransaction(entity: ShadowResourceTransactionEntity): Long
}

@Dao
interface ShadowXpTraceDao {
    @Query("SELECT * FROM shadow_xp_traces WHERE accountOwnerId = :accountOwnerId AND traceType = :traceType AND sourceTag = :sourceTag LIMIT 1")
    suspend fun findTrace(accountOwnerId: String, traceType: String, sourceTag: String): ShadowXpTraceEntity?

    @Query("SELECT * FROM shadow_xp_traces WHERE accountOwnerId = :accountOwnerId ORDER BY timestampEpochMs DESC")
    suspend fun getTracesForOwnerOnce(accountOwnerId: String): List<ShadowXpTraceEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTraceIgnore(entity: ShadowXpTraceEntity): Long
}


@Dao
interface ShadowEvidenceClaimDao {
    @Query("SELECT * FROM shadow_evidence_claims WHERE accountOwnerId = :accountOwnerId")
    suspend fun getClaimsForOwnerOnce(accountOwnerId: String): List<ShadowEvidenceClaimEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertClaim(entity: ShadowEvidenceClaimEntity): Long
}

@Dao
interface ShadowResonanceDao {
    @Query("SELECT * FROM shadow_resonance_records WHERE accountOwnerId = :accountOwnerId")
    suspend fun getResonanceRecordsForOwnerOnce(accountOwnerId: String): List<ShadowResonanceEntity>

    @Query("SELECT * FROM shadow_resonance_records WHERE accountOwnerId = :accountOwnerId AND archetypeId = :archetypeId LIMIT 1")
    suspend fun getResonanceForArchetype(accountOwnerId: String, archetypeId: String): ShadowResonanceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertResonance(entity: ShadowResonanceEntity): Long
}
