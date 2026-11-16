package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.AutomationExecutionLogEntity
import com.example.data.local.entity.AutomationJobEntity
import com.example.data.local.entity.DailyCycleExecutionEntity
import com.example.data.local.entity.DailyExecutionItemEntity
import com.example.data.local.entity.RecoveryOperationRecordEntity
import com.example.data.local.entity.SystemEventEntity
import com.example.data.local.entity.SystemHealthSnapshotEntity
import com.example.data.local.entity.SystemIntegrityFindingEntity
import com.example.data.local.entity.SystemPreferencesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SystemEventDao {
    @Query("SELECT * FROM system_events ORDER BY timestampEpochMs DESC LIMIT :limit")
    fun observeRecentEvents(limit: Int = 100): Flow<List<SystemEventEntity>>

    @Query("SELECT * FROM system_events ORDER BY timestampEpochMs DESC LIMIT :limit")
    suspend fun getRecentEventsOnce(limit: Int = 100): List<SystemEventEntity>

    @Query("SELECT * FROM system_events WHERE processingStatusName IN ('PENDING', 'FAILED') ORDER BY timestampEpochMs ASC LIMIT :limit")
    suspend fun getPendingOrFailedOnce(limit: Int = 50): List<SystemEventEntity>

    @Query("SELECT * FROM system_events WHERE processingStatusName = 'PROCESSING'")
    suspend fun getInterruptedProcessingOnce(): List<SystemEventEntity>

    @Query("SELECT COUNT(*) FROM system_events")
    suspend fun getTotalCount(): Int

    @Query("SELECT COUNT(*) FROM system_events WHERE processingStatusName = :statusName")
    suspend fun countByStatus(statusName: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEvent(event: SystemEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEvents(events: List<SystemEventEntity>)

    @Query("DELETE FROM system_events WHERE timestampEpochMs < :olderThanEpochMs AND processingStatusName IN ('PROCESSED', 'DEDUPLICATED')")
    suspend fun purgeOldCompleted(olderThanEpochMs: Long): Int
}

@Dao
interface AutomationDao {
    @Query("SELECT * FROM automation_jobs ORDER BY automationId ASC")
    fun observeJobs(): Flow<List<AutomationJobEntity>>

    @Query("SELECT * FROM automation_jobs ORDER BY automationId ASC")
    suspend fun getJobsOnce(): List<AutomationJobEntity>

    @Query("SELECT * FROM automation_jobs WHERE automationId = :automationId")
    suspend fun getJobById(automationId: String): AutomationJobEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertJob(job: AutomationJobEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertJobs(jobs: List<AutomationJobEntity>)

    @Query("UPDATE automation_jobs SET isEnabled = :enabled, currentStatusName = :statusName WHERE automationId = :automationId")
    suspend fun updateJobEnabled(automationId: String, enabled: Boolean, statusName: String)

    @Query("SELECT * FROM automation_execution_logs ORDER BY startedAtEpochMs DESC LIMIT :limit")
    fun observeExecutionLogs(limit: Int = 60): Flow<List<AutomationExecutionLogEntity>>

    @Query("SELECT * FROM automation_execution_logs ORDER BY startedAtEpochMs DESC LIMIT :limit")
    suspend fun getExecutionLogsOnce(limit: Int = 60): List<AutomationExecutionLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExecutionLog(log: AutomationExecutionLogEntity): Long
}

@Dao
interface DailyCycleDao {
    @Query("SELECT * FROM daily_cycle_executions ORDER BY epochDay DESC LIMIT :limit")
    fun observeCycles(limit: Int = 30): Flow<List<DailyCycleExecutionEntity>>

    @Query("SELECT * FROM daily_cycle_executions ORDER BY epochDay DESC LIMIT 1")
    suspend fun getLatestCycleOnce(): DailyCycleExecutionEntity?

    @Query("SELECT * FROM daily_cycle_executions WHERE epochDay = :epochDay")
    suspend fun getCycleForDay(epochDay: Long): DailyCycleExecutionEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCycleIfAbsent(cycle: DailyCycleExecutionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCycle(cycle: DailyCycleExecutionEntity)

    @Query("SELECT * FROM daily_execution_items WHERE dateEpochDay = :epochDay ORDER BY updatedAtEpochMs DESC")
    fun observeExecutionOverridesForDay(epochDay: Long): Flow<List<DailyExecutionItemEntity>>

    @Query("SELECT * FROM daily_execution_items WHERE dateEpochDay = :epochDay")
    suspend fun getExecutionOverridesForDayOnce(epochDay: Long): List<DailyExecutionItemEntity>

    @Query("SELECT COUNT(*) FROM daily_execution_items")
    suspend fun getTotalExecutionOverridesCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExecutionOverride(item: DailyExecutionItemEntity)
}

@Dao
interface SystemIntegrityHealthDao {
    @Query("SELECT * FROM system_integrity_findings ORDER BY detectedAtEpochMs DESC LIMIT :limit")
    fun observeFindings(limit: Int = 100): Flow<List<SystemIntegrityFindingEntity>>

    @Query("SELECT * FROM system_integrity_findings WHERE statusName = 'OPEN' ORDER BY detectedAtEpochMs DESC")
    suspend fun getOpenFindingsOnce(): List<SystemIntegrityFindingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFindings(findings: List<SystemIntegrityFindingEntity>)

    @Query("UPDATE system_integrity_findings SET statusName = :statusName, resolvedAtEpochMs = :resolvedAtEpochMs WHERE findingUuid = :findingUuid")
    suspend fun updateFindingStatus(findingUuid: String, statusName: String, resolvedAtEpochMs: Long?)

    @Query("SELECT * FROM system_health_snapshots ORDER BY evaluatedAtEpochMs DESC LIMIT :limit")
    fun observeHealthSnapshots(limit: Int = 20): Flow<List<SystemHealthSnapshotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHealthSnapshot(snapshot: SystemHealthSnapshotEntity): Long

    @Query("SELECT * FROM recovery_operation_records ORDER BY createdAtEpochMs DESC LIMIT :limit")
    fun observeRecoveryOperations(limit: Int = 40): Flow<List<RecoveryOperationRecordEntity>>

    @Query("SELECT * FROM recovery_operation_records ORDER BY createdAtEpochMs DESC LIMIT :limit")
    suspend fun getRecoveryOperationsOnce(limit: Int = 40): List<RecoveryOperationRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecoveryOperation(record: RecoveryOperationRecordEntity)

    @Query("SELECT * FROM system_preferences WHERE id = 1")
    fun observePreferences(): Flow<SystemPreferencesEntity?>

    @Query("SELECT * FROM system_preferences WHERE id = 1")
    suspend fun getPreferencesOnce(): SystemPreferencesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPreferences(prefs: SystemPreferencesEntity)
}
