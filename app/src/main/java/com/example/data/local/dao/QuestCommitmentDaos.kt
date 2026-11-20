package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ConfirmedFailureRecordEntity
import com.example.data.local.entity.QuestCommitmentEntity
import com.example.data.local.entity.QuestExemptionEntity
import com.example.data.local.entity.QuestRescheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestCommitmentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCommitment(commitment: QuestCommitmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommitments(commitments: List<QuestCommitmentEntity>)

    @Update
    suspend fun updateCommitment(commitment: QuestCommitmentEntity)

    @Query("SELECT * FROM quest_commitments WHERE commitmentId = :commitmentId")
    suspend fun getCommitmentById(commitmentId: String): QuestCommitmentEntity?

    @Query("SELECT * FROM quest_commitments WHERE questId = :questId AND logicalCycleId = :logicalCycleId")
    suspend fun getCommitmentForQuestAndCycle(questId: Long, logicalCycleId: String): QuestCommitmentEntity?

    @Query("SELECT * FROM quest_commitments WHERE logicalCycleId = :logicalCycleId ORDER BY deadlineEpochMs ASC")
    fun observeCommitmentsForCycle(logicalCycleId: String): Flow<List<QuestCommitmentEntity>>

    @Query("SELECT * FROM quest_commitments WHERE logicalCycleId = :logicalCycleId ORDER BY deadlineEpochMs ASC")
    suspend fun getCommitmentsForCycleOnce(logicalCycleId: String): List<QuestCommitmentEntity>

    @Query("SELECT * FROM quest_commitments WHERE accountOwnerId = :accountOwnerId ORDER BY deadlineEpochMs DESC")
    fun observeAllCommitments(accountOwnerId: String): Flow<List<QuestCommitmentEntity>>

    @Query("SELECT * FROM quest_commitments WHERE accountOwnerId = :accountOwnerId ORDER BY deadlineEpochMs DESC")
    suspend fun getAllCommitmentsOnce(accountOwnerId: String): List<QuestCommitmentEntity>

    @Query("SELECT * FROM quest_commitments WHERE stateName IN ('ACCEPTED', 'ACTIVE', 'RESCHEDULED', 'OVERDUE') AND gracePeriodExpiresAtEpochMs <= :beforeOrAtEpochMs")
    suspend fun getOverdueCommitmentsEligibleForFailure(beforeOrAtEpochMs: Long): List<QuestCommitmentEntity>

    @Query("UPDATE quest_commitments SET stateName = :stateName, updatedAtEpochMs = :updatedAtEpochMs WHERE commitmentId = :commitmentId")
    suspend fun updateCommitmentState(commitmentId: String, stateName: String, updatedAtEpochMs: Long)

    @Query("""
        UPDATE quest_commitments 
        SET deadlineEpochMs = :newDeadlineEpochMs, 
            gracePeriodExpiresAtEpochMs = :newGraceExpiryEpochMs, 
            rescheduleCount = :rescheduleCount, 
            stateName = :stateName, 
            updatedAtEpochMs = :updatedAtEpochMs 
        WHERE commitmentId = :commitmentId
    """)
    suspend fun updateCommitmentDeadline(
        commitmentId: String,
        newDeadlineEpochMs: Long,
        newGraceExpiryEpochMs: Long,
        rescheduleCount: Int,
        stateName: String,
        updatedAtEpochMs: Long
    )
}

@Dao
interface ConfirmedFailureDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFailure(failure: ConfirmedFailureRecordEntity): Long

    @Query("SELECT * FROM confirmed_quest_failures WHERE commitmentId = :commitmentId")
    suspend fun getFailureByCommitment(commitmentId: String): ConfirmedFailureRecordEntity?

    @Query("SELECT * FROM confirmed_quest_failures WHERE logicalCycleId = :logicalCycleId ORDER BY confirmedAtEpochMs DESC")
    suspend fun getFailuresForCycle(logicalCycleId: String): List<ConfirmedFailureRecordEntity>

    @Query("SELECT * FROM confirmed_quest_failures ORDER BY confirmedAtEpochMs DESC")
    fun observeAllFailures(): Flow<List<ConfirmedFailureRecordEntity>>

    @Query("SELECT * FROM confirmed_quest_failures ORDER BY confirmedAtEpochMs DESC")
    suspend fun getAllFailuresOnce(): List<ConfirmedFailureRecordEntity>

    @Query("SELECT COUNT(*) FROM confirmed_quest_failures")
    suspend fun getFailureCount(): Int
}

@Dao
interface QuestExemptionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExemption(exemption: QuestExemptionEntity): Long

    @Query("SELECT * FROM quest_exemptions WHERE exemptionId = :exemptionId")
    suspend fun getExemptionById(exemptionId: String): QuestExemptionEntity?

    @Query("SELECT * FROM quest_exemptions WHERE commitmentId = :commitmentId ORDER BY requestedAtEpochMs DESC LIMIT 1")
    suspend fun getActiveExemptionForCommitment(commitmentId: String): QuestExemptionEntity?

    @Query("SELECT * FROM quest_exemptions WHERE accountOwnerId = :accountOwnerId ORDER BY requestedAtEpochMs DESC")
    fun observeAllExemptions(accountOwnerId: String): Flow<List<QuestExemptionEntity>>

    @Query("SELECT * FROM quest_exemptions WHERE accountOwnerId = :accountOwnerId ORDER BY requestedAtEpochMs DESC")
    suspend fun getAllExemptionsOnce(accountOwnerId: String): List<QuestExemptionEntity>

    @Query("SELECT * FROM quest_exemptions WHERE lifecycleStateName = :lifecycleStateName ORDER BY requestedAtEpochMs DESC")
    suspend fun getExemptionsByState(lifecycleStateName: String): List<QuestExemptionEntity>

    @Query("""
        UPDATE quest_exemptions 
        SET lifecycleStateName = :lifecycleStateName, 
            reviewedAtEpochMs = :reviewedAtEpochMs, 
            reviewerNotes = :reviewerNotes 
        WHERE exemptionId = :exemptionId
    """)
    suspend fun updateExemptionStatus(
        exemptionId: String,
        lifecycleStateName: String,
        reviewedAtEpochMs: Long,
        reviewerNotes: String?
    )
}

@Dao
interface QuestRescheduleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReschedule(reschedule: QuestRescheduleEntity): Long

    @Query("SELECT * FROM quest_reschedules WHERE commitmentId = :commitmentId ORDER BY rescheduledAtEpochMs DESC")
    suspend fun getReschedulesForCommitment(commitmentId: String): List<QuestRescheduleEntity>
}
