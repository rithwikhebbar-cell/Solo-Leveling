package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.CloudAuthSessionEntity
import com.example.data.local.entity.CloudBackupEntity
import com.example.data.local.entity.CloudSyncCursorEntity
import com.example.data.local.entity.RegisteredDeviceEntity
import com.example.data.local.entity.SyncConflictEntity
import com.example.data.local.entity.SyncOperationLogEntity
import com.example.data.local.entity.SyncQueueEntity
import com.example.data.local.entity.SyncRecordMetaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CloudAuthDao {
    @Query("SELECT * FROM cloud_auth_sessions WHERE id = 1 LIMIT 1")
    fun observeSession(): Flow<CloudAuthSessionEntity?>

    @Query("SELECT * FROM cloud_auth_sessions WHERE id = 1 LIMIT 1")
    suspend fun getSessionOnce(): CloudAuthSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(entity: CloudAuthSessionEntity)

    @Query("DELETE FROM cloud_auth_sessions")
    suspend fun clearSession()
}

@Dao
interface RegisteredDeviceDao {
    @Query("SELECT * FROM registered_devices ORDER BY isCurrentDevice DESC, lastActiveEpochMs DESC")
    fun observeDevices(): Flow<List<RegisteredDeviceEntity>>

    @Query("SELECT * FROM registered_devices ORDER BY isCurrentDevice DESC, lastActiveEpochMs DESC")
    suspend fun getDevicesOnce(): List<RegisteredDeviceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDevice(device: RegisteredDeviceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDevices(devices: List<RegisteredDeviceEntity>)

    @Query("UPDATE registered_devices SET isRevoked = 1, syncStatusName = 'FAILED' WHERE deviceUuid = :deviceUuid")
    suspend fun markDeviceRevoked(deviceUuid: String)

    @Query("DELETE FROM registered_devices")
    suspend fun clearAll()
}

@Dao
interface SyncMetaDao {
    @Query("SELECT * FROM sync_records_meta WHERE compositeKey = :compositeKey LIMIT 1")
    suspend fun getByCompositeKey(compositeKey: String): SyncRecordMetaEntity?

    @Query("SELECT * FROM sync_records_meta WHERE cloudUuid = :cloudUuid LIMIT 1")
    suspend fun getByCloudUuid(cloudUuid: String): SyncRecordMetaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(meta: SyncRecordMetaEntity)

    @Query("SELECT * FROM cloud_sync_cursor WHERE entityTypeName = :entityType LIMIT 1")
    suspend fun getCursor(entityType: String): CloudSyncCursorEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCursor(cursor: CloudSyncCursorEntity)

    @Query("DELETE FROM cloud_sync_cursor")
    suspend fun clearCursors()

    @Query("DELETE FROM sync_records_meta")
    suspend fun clearAllMeta()
}

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue ORDER BY createdAtEpochMs ASC")
    fun observeQueue(): Flow<List<SyncQueueEntity>>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE syncStateName IN ('PENDING_UPLOAD', 'UPLOADING', 'FAILED')")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE syncStateName = 'FAILED'")
    fun observeFailedCount(): Flow<Int>

    @Query("SELECT * FROM sync_queue WHERE syncStateName IN ('PENDING_UPLOAD', 'FAILED') AND nextRetryAtEpochMs <= :nowEpochMs ORDER BY createdAtEpochMs ASC LIMIT :limit")
    suspend fun getReadyBatch(nowEpochMs: Long, limit: Int): List<SyncQueueEntity>

    @Query("SELECT * FROM sync_queue ORDER BY createdAtEpochMs ASC")
    suspend fun getAllOnce(): List<SyncQueueEntity>

    @Query("SELECT * FROM sync_queue WHERE idempotencyKey = :key LIMIT 1")
    suspend fun getByIdempotencyKey(key: String): SyncQueueEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(item: SyncQueueEntity): Long

    @Update
    suspend fun update(item: SyncQueueEntity)

    @Query("DELETE FROM sync_queue WHERE queueId = :queueId")
    suspend fun deleteById(queueId: Long)

    @Query("DELETE FROM sync_queue WHERE syncStateName IN ('UPLOADED', 'SYNCED')")
    suspend fun pruneCompleted()

    @Query("DELETE FROM sync_queue")
    suspend fun clearAll()
}

@Dao
interface SyncConflictDao {
    @Query("SELECT * FROM sync_conflicts ORDER BY createdAtEpochMs DESC")
    fun observeConflicts(): Flow<List<SyncConflictEntity>>

    @Query("SELECT COUNT(*) FROM sync_conflicts WHERE statusName = 'OPEN'")
    fun observeOpenConflictCount(): Flow<Int>

    @Query("SELECT * FROM sync_conflicts WHERE statusName = 'OPEN' ORDER BY createdAtEpochMs DESC")
    suspend fun getOpenConflictsOnce(): List<SyncConflictEntity>

    @Query("SELECT * FROM sync_conflicts WHERE conflictId = :conflictId LIMIT 1")
    suspend fun getById(conflictId: Long): SyncConflictEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(conflict: SyncConflictEntity): Long

    @Update
    suspend fun update(conflict: SyncConflictEntity)

    @Query("DELETE FROM sync_conflicts")
    suspend fun clearAll()
}

@Dao
interface SyncDiagnosticsDao {
    @Query("SELECT * FROM sync_operation_logs ORDER BY startedAtEpochMs DESC LIMIT 50")
    fun observeLogs(): Flow<List<SyncOperationLogEntity>>

    @Query("SELECT * FROM sync_operation_logs ORDER BY startedAtEpochMs DESC LIMIT 1")
    suspend fun getLatestOnce(): SyncOperationLogEntity?

    @Query("SELECT * FROM sync_operation_logs WHERE statusName = 'SYNCED' ORDER BY completedAtEpochMs DESC LIMIT 1")
    fun observeLatestSuccess(): Flow<SyncOperationLogEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: SyncOperationLogEntity)

    @Query("DELETE FROM sync_operation_logs")
    suspend fun clearAll()
}

@Dao
interface CloudBackupDao {
    @Query("SELECT * FROM cloud_backups ORDER BY createdAtEpochMs DESC")
    fun observeBackups(): Flow<List<CloudBackupEntity>>

    @Query("SELECT * FROM cloud_backups WHERE backupUuid = :backupUuid LIMIT 1")
    suspend fun getByUuid(backupUuid: String): CloudBackupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(backup: CloudBackupEntity)

    @Query("DELETE FROM cloud_backups")
    suspend fun clearAll()
}
