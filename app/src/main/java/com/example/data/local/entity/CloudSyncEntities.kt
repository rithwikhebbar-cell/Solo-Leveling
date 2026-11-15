package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.AccountStatus
import com.example.domain.model.AuthStatus
import com.example.domain.model.BackupStatus
import com.example.domain.model.CloudBackupRecord
import com.example.domain.model.ConflictStatus
import com.example.domain.model.DeviceRegistration
import com.example.domain.model.HunterRank
import com.example.domain.model.PlayerIdentity
import com.example.domain.model.SyncConflict
import com.example.domain.model.SyncDiagnosticsLog
import com.example.domain.model.SyncEntityType
import com.example.domain.model.SyncOperationType
import com.example.domain.model.SyncQueueItem
import com.example.domain.model.SyncState

// 1. Persisted account/player identity metadata (NO plaintext passwords or tokens stored here!)
@Entity(tableName = "cloud_auth_sessions")
data class CloudAuthSessionEntity(
    @PrimaryKey val id: Int = 1,
    val supabaseUserId: String,
    val playerIdentityUuid: String,
    val email: String,
    val displayName: String,
    val activeTitle: String,
    val hunterRankName: String,
    val level: Int,
    val totalXp: Long,
    val accountStatusName: String,
    val authStatusName: String,
    val emailVerified: Boolean,
    val autoSyncEnabled: Boolean,
    val cloudSyncConsentGranted: Boolean,
    val expiresAtEpochMs: Long?,
    val lastSyncAtEpochMs: Long?,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)

// 2. Multi-device registration registry
@Entity(
    tableName = "registered_devices",
    indices = [Index(value = ["userId"]), Index(value = ["lastActiveEpochMs"])]
)
data class RegisteredDeviceEntity(
    @PrimaryKey val deviceUuid: String,
    val userId: String,
    val deviceName: String,
    val manufacturerModel: String,
    val osVersion: String,
    val appVersion: String,
    val lastActiveEpochMs: Long,
    val lastSyncEpochMs: Long?,
    val syncStatusName: String,
    val isCurrentDevice: Boolean,
    val isRevoked: Boolean,
    val createdAtEpochMs: Long
)

// 3. Per-record sync mapping & tombstone version tracker
@Entity(
    tableName = "sync_records_meta",
    indices = [
        Index(value = ["entityType", "localKey"], unique = true),
        Index(value = ["cloudUuid"]),
        Index(value = ["syncStateName"])
    ]
)
data class SyncRecordMetaEntity(
    @PrimaryKey val compositeKey: String,
    val entityType: String,
    val localKey: String,
    val cloudUuid: String,
    val userId: String,
    val version: Int,
    val updatedAtEpochMs: Long,
    val deletedAtEpochMs: Long?,
    val syncStateName: String,
    val lastSyncedHash: String,
    val lastError: String?
)

// 4. Persistent offline-first Sync Outbox Queue
@Entity(
    tableName = "sync_queue",
    indices = [
        Index(value = ["idempotencyKey"], unique = true),
        Index(value = ["syncStateName"]),
        Index(value = ["nextRetryAtEpochMs"])
    ]
)
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val queueId: Long = 0,
    val entityTypeName: String,
    val localKey: String,
    val cloudUuid: String,
    val operationName: String,
    val payloadJson: String,
    val idempotencyKey: String,
    val syncStateName: String,
    val retryCount: Int,
    val nextRetryAtEpochMs: Long,
    val lastError: String?,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)

// 5. Explicit Conflict Resolution Log
@Entity(
    tableName = "sync_conflicts",
    indices = [
        Index(value = ["statusName"]),
        Index(value = ["entityTypeName", "localKey"])
    ]
)
data class SyncConflictEntity(
    @PrimaryKey(autoGenerate = true) val conflictId: Long = 0,
    val entityTypeName: String,
    val localKey: String,
    val cloudUuid: String,
    val title: String,
    val conflictReason: String,
    val localSummary: String,
    val cloudSummary: String,
    val localPayloadJson: String,
    val cloudPayloadJson: String,
    val localUpdatedAtEpochMs: Long,
    val cloudUpdatedAtEpochMs: Long,
    val resolutionStrategy: String,
    val statusName: String,
    val resolvedAtEpochMs: Long?,
    val createdAtEpochMs: Long
)

// 6. Structured Sync Diagnostics & Observability History
@Entity(
    tableName = "sync_operation_logs",
    indices = [Index(value = ["startedAtEpochMs"])]
)
data class SyncOperationLogEntity(
    @PrimaryKey val operationUuid: String,
    val triggerSource: String,
    val startedAtEpochMs: Long,
    val completedAtEpochMs: Long?,
    val statusName: String,
    val recordsUploaded: Int,
    val recordsDownloaded: Int,
    val recordsSkipped: Int,
    val conflictsDetected: Int,
    val failedOperations: Int,
    val retryCount: Int,
    val errorCategory: String?,
    val safeMessage: String
)

// 7. Cloud Backups Metadata & Local Snapshot Store
@Entity(
    tableName = "cloud_backups",
    indices = [Index(value = ["userId"]), Index(value = ["createdAtEpochMs"])]
)
data class CloudBackupEntity(
    @PrimaryKey val backupUuid: String,
    val userId: String,
    val schemaVersion: Int,
    val progressionChecksum: String,
    val summaryText: String,
    val payloadJson: String,
    val totalXpSnapshot: Long,
    val playerLevelSnapshot: Int,
    val recordCount: Int,
    val statusName: String,
    val createdAtEpochMs: Long,
    val uploadedAtEpochMs: Long?
)

// 8. Incremental Pull Cursors per entity type
@Entity(tableName = "cloud_sync_cursor")
data class CloudSyncCursorEntity(
    @PrimaryKey val entityTypeName: String,
    val lastPulledUpdatedAtIso: String,
    val lastPulledEpochMs: Long,
    val lastPageOffset: Int,
    val updatedAtEpochMs: Long
)

// Domain Mappers
fun CloudAuthSessionEntity.toDomain() = PlayerIdentity(
    playerId = playerIdentityUuid,
    supabaseUserId = supabaseUserId,
    email = email,
    displayName = displayName,
    activeTitle = activeTitle,
    hunterRank = runCatching { HunterRank.valueOf(hunterRankName) }.getOrDefault(HunterRank.E_RANK),
    level = level,
    totalXp = totalXp,
    accountStatus = runCatching { AccountStatus.valueOf(accountStatusName) }.getOrDefault(AccountStatus.ACTIVE),
    emailVerified = emailVerified,
    authStatus = runCatching { AuthStatus.valueOf(authStatusName) }.getOrDefault(AuthStatus.SIGNED_OUT),
    createdAtEpochMs = createdAtEpochMs,
    lastSyncAtEpochMs = lastSyncAtEpochMs,
    expiresAtEpochMs = expiresAtEpochMs,
    autoSyncEnabled = autoSyncEnabled,
    cloudSyncConsentGranted = cloudSyncConsentGranted
)

fun RegisteredDeviceEntity.toDomain() = DeviceRegistration(
    deviceUuid = deviceUuid,
    userId = userId,
    deviceName = deviceName,
    manufacturerModel = manufacturerModel,
    osVersion = osVersion,
    appVersion = appVersion,
    lastActiveEpochMs = lastActiveEpochMs,
    lastSyncEpochMs = lastSyncEpochMs,
    syncStatus = runCatching { SyncState.valueOf(syncStatusName) }.getOrDefault(SyncState.SYNCED),
    isCurrentDevice = isCurrentDevice,
    isRevoked = isRevoked,
    createdAtEpochMs = createdAtEpochMs
)

fun SyncQueueEntity.toDomain() = SyncQueueItem(
    queueId = queueId,
    entityType = runCatching { SyncEntityType.valueOf(entityTypeName) }.getOrDefault(SyncEntityType.PERSONAL_TASK),
    localKey = localKey,
    cloudUuid = cloudUuid,
    operation = runCatching { SyncOperationType.valueOf(operationName) }.getOrDefault(SyncOperationType.UPSERT),
    payloadJson = payloadJson,
    idempotencyKey = idempotencyKey,
    syncState = runCatching { SyncState.valueOf(syncStateName) }.getOrDefault(SyncState.PENDING_UPLOAD),
    retryCount = retryCount,
    nextRetryAtEpochMs = nextRetryAtEpochMs,
    lastError = lastError,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs
)

fun SyncConflictEntity.toDomain() = SyncConflict(
    conflictId = conflictId,
    entityType = runCatching { SyncEntityType.valueOf(entityTypeName) }.getOrDefault(SyncEntityType.PERSONAL_TASK),
    localKey = localKey,
    cloudUuid = cloudUuid,
    title = title,
    conflictReason = conflictReason,
    localSummary = localSummary,
    cloudSummary = cloudSummary,
    localPayloadJson = localPayloadJson,
    cloudPayloadJson = cloudPayloadJson,
    localUpdatedAtEpochMs = localUpdatedAtEpochMs,
    cloudUpdatedAtEpochMs = cloudUpdatedAtEpochMs,
    resolutionStrategy = resolutionStrategy,
    status = runCatching { ConflictStatus.valueOf(statusName) }.getOrDefault(ConflictStatus.OPEN),
    resolvedAtEpochMs = resolvedAtEpochMs,
    createdAtEpochMs = createdAtEpochMs
)

fun SyncOperationLogEntity.toDomain() = SyncDiagnosticsLog(
    operationUuid = operationUuid,
    triggerSource = triggerSource,
    startedAtEpochMs = startedAtEpochMs,
    completedAtEpochMs = completedAtEpochMs,
    status = runCatching { SyncState.valueOf(statusName) }.getOrDefault(SyncState.SYNCED),
    recordsUploaded = recordsUploaded,
    recordsDownloaded = recordsDownloaded,
    recordsSkipped = recordsSkipped,
    conflictsDetected = conflictsDetected,
    failedOperations = failedOperations,
    retryCount = retryCount,
    errorCategory = errorCategory,
    safeMessage = safeMessage
)

fun CloudBackupEntity.toDomain() = CloudBackupRecord(
    backupUuid = backupUuid,
    userId = userId,
    schemaVersion = schemaVersion,
    progressionChecksum = progressionChecksum,
    summaryText = summaryText,
    totalXpSnapshot = totalXpSnapshot,
    playerLevelSnapshot = playerLevelSnapshot,
    recordCount = recordCount,
    status = runCatching { BackupStatus.valueOf(statusName) }.getOrDefault(BackupStatus.CREATED),
    createdAtEpochMs = createdAtEpochMs,
    uploadedAtEpochMs = uploadedAtEpochMs
)
