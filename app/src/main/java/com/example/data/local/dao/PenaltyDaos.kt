package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.PenaltyEntity
import com.example.data.local.entity.PenaltyPointAccountEntity
import com.example.data.local.entity.PenaltyTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PenaltyDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPenalty(penalty: PenaltyEntity): Long

    @Query("SELECT * FROM penalties WHERE penaltyId = :penaltyId")
    suspend fun getPenaltyById(penaltyId: String): PenaltyEntity?

    @Query("SELECT * FROM penalties WHERE failureRecordId = :failureRecordId")
    suspend fun getPenaltyByFailureRecordId(failureRecordId: String): PenaltyEntity?

    @Query("SELECT * FROM penalties WHERE commitmentId = :commitmentId LIMIT 1")
    suspend fun getPenaltyByCommitmentId(commitmentId: String): PenaltyEntity?

    @Query("SELECT * FROM penalties WHERE accountOwnerId = :accountOwnerId ORDER BY createdAtEpochMs DESC")
    fun observeAllPenalties(accountOwnerId: String): Flow<List<PenaltyEntity>>

    @Query("SELECT * FROM penalties WHERE accountOwnerId = :accountOwnerId AND penaltyStatusName IN ('ACTIVE', 'ESCALATED') ORDER BY createdAtEpochMs DESC")
    fun observeActivePenalties(accountOwnerId: String): Flow<List<PenaltyEntity>>

    @Query("SELECT * FROM penalties WHERE accountOwnerId = :accountOwnerId ORDER BY createdAtEpochMs DESC")
    suspend fun getAllPenaltiesOnce(accountOwnerId: String): List<PenaltyEntity>

    @Query("SELECT * FROM penalties WHERE accountOwnerId = :accountOwnerId AND penaltyStatusName IN ('ACTIVE', 'ESCALATED') ORDER BY createdAtEpochMs DESC")
    suspend fun getActivePenaltiesOnce(accountOwnerId: String): List<PenaltyEntity>

    @Query("SELECT COUNT(*) FROM penalties WHERE accountOwnerId = :accountOwnerId AND penaltyStatusName IN ('ACTIVE', 'ESCALATED')")
    suspend fun getActivePenaltiesCount(accountOwnerId: String): Int

    @Query("UPDATE penalties SET penaltyStatusName = :statusName, resolvedAtEpochMs = :resolvedAtEpochMs WHERE penaltyId = :penaltyId")
    suspend fun updatePenaltyStatus(penaltyId: String, statusName: String, resolvedAtEpochMs: Long?)
}

@Dao
interface PenaltyTransactionDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransaction(transaction: PenaltyTransactionEntity): Long

    @Query("SELECT * FROM penalty_transactions WHERE idempotencyKey = :idempotencyKey")
    suspend fun getTransactionByIdempotencyKey(idempotencyKey: String): PenaltyTransactionEntity?

    @Query("SELECT * FROM penalty_transactions WHERE accountOwnerId = :accountOwnerId ORDER BY timestampEpochMs DESC LIMIT :limit")
    fun observeRecentTransactions(accountOwnerId: String, limit: Int = 50): Flow<List<PenaltyTransactionEntity>>

    @Query("SELECT * FROM penalty_transactions WHERE accountOwnerId = :accountOwnerId ORDER BY timestampEpochMs ASC")
    suspend fun getAllTransactionsOnce(accountOwnerId: String): List<PenaltyTransactionEntity>

    @Query("SELECT * FROM penalty_transactions WHERE penaltyId = :penaltyId ORDER BY timestampEpochMs ASC")
    suspend fun getTransactionsForPenalty(penaltyId: String): List<PenaltyTransactionEntity>
}

@Dao
interface PenaltyAccountDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAccount(account: PenaltyPointAccountEntity)

    @Query("SELECT * FROM penalty_point_accounts WHERE accountOwnerId = :accountOwnerId")
    suspend fun getAccountOnce(accountOwnerId: String): PenaltyPointAccountEntity?

    @Query("SELECT * FROM penalty_point_accounts WHERE accountOwnerId = :accountOwnerId")
    fun observeAccount(accountOwnerId: String): Flow<PenaltyPointAccountEntity?>
}
