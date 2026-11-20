package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.RedemptionEntity
import com.example.data.local.entity.RedemptionVerificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RedemptionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRedemption(redemption: RedemptionEntity): Long

    @Query("SELECT * FROM redemption_activities WHERE redemptionId = :redemptionId")
    suspend fun getRedemptionById(redemptionId: String): RedemptionEntity?

    @Query("SELECT * FROM redemption_activities WHERE penaltyId = :penaltyId AND lifecycleStateName NOT IN ('CANCELLED', 'REJECTED') ORDER BY createdAtEpochMs DESC LIMIT 1")
    suspend fun getActiveRedemptionForPenalty(penaltyId: String): RedemptionEntity?

    @Query("SELECT * FROM redemption_activities WHERE accountOwnerId = :accountOwnerId AND lifecycleStateName IN ('PENDING_ACCEPTANCE', 'GENERATED', 'ACCEPTED', 'ACTIVE', 'COMPLETION_PENDING_VERIFICATION') ORDER BY createdAtEpochMs DESC")
    fun observeActiveRedemptions(accountOwnerId: String): Flow<List<RedemptionEntity>>

    @Query("SELECT * FROM redemption_activities WHERE accountOwnerId = :accountOwnerId ORDER BY createdAtEpochMs DESC")
    fun observeAllRedemptions(accountOwnerId: String): Flow<List<RedemptionEntity>>

    @Query("SELECT * FROM redemption_activities WHERE accountOwnerId = :accountOwnerId AND lifecycleStateName IN ('PENDING_ACCEPTANCE', 'GENERATED', 'ACCEPTED', 'ACTIVE', 'COMPLETION_PENDING_VERIFICATION') ORDER BY createdAtEpochMs DESC")
    suspend fun getActiveRedemptionsOnce(accountOwnerId: String): List<RedemptionEntity>

    @Query("SELECT * FROM redemption_activities WHERE accountOwnerId = :accountOwnerId ORDER BY createdAtEpochMs DESC")
    suspend fun getAllRedemptionsOnce(accountOwnerId: String): List<RedemptionEntity>

    @Query("UPDATE redemption_activities SET lifecycleStateName = :stateName, acceptedAtEpochMs = :acceptedAtEpochMs WHERE redemptionId = :redemptionId")
    suspend fun updateAcceptance(redemptionId: String, stateName: String, acceptedAtEpochMs: Long)

    @Query("UPDATE redemption_activities SET lifecycleStateName = :stateName WHERE redemptionId = :redemptionId")
    suspend fun updateState(redemptionId: String, stateName: String)

    @Query("""
        UPDATE redemption_activities 
        SET lifecycleStateName = :stateName, 
            completedAtEpochMs = :completedAtEpochMs, 
            verifiedAtEpochMs = :verifiedAtEpochMs, 
            verificationNotes = :verificationNotes 
        WHERE redemptionId = :redemptionId
    """)
    suspend fun updateCompletion(
        redemptionId: String,
        stateName: String,
        completedAtEpochMs: Long,
        verifiedAtEpochMs: Long,
        verificationNotes: String?
    )
}

@Dao
interface RedemptionVerificationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVerification(verification: RedemptionVerificationEntity): Long

    @Query("SELECT * FROM redemption_verifications WHERE redemptionId = :redemptionId LIMIT 1")
    suspend fun getVerificationForRedemption(redemptionId: String): RedemptionVerificationEntity?

    @Query("SELECT * FROM redemption_verifications WHERE accountOwnerId = :accountOwnerId ORDER BY submittedAtEpochMs DESC")
    fun observeVerifications(accountOwnerId: String): Flow<List<RedemptionVerificationEntity>>
}
