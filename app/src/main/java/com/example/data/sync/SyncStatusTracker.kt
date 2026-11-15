package com.example.data.sync

import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.entity.toDomain
import com.example.domain.model.SyncDiagnosticsLog
import com.example.domain.model.SyncState
import com.example.domain.model.SyncSummaryState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class SyncStatusTracker(
    private val database: SoloLevelingDatabase,
    private val isCloudConfiguredProvider: () -> Boolean
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val isOnlineFlow = MutableStateFlow(true)
    private val isSyncingFlow = MutableStateFlow(false)
    private val lastErrorFlow = MutableStateFlow<String?>(null)

    val summaryState: StateFlow<SyncSummaryState> = combine(
        database.cloudAuthDao().observeSession(),
        database.syncQueueDao().observePendingCount(),
        database.syncQueueDao().observeFailedCount(),
        database.syncConflictDao().observeOpenConflictCount(),
        database.syncDiagnosticsDao().observeLatestSuccess(),
        isOnlineFlow,
        isSyncingFlow,
        lastErrorFlow
    ) { values ->
        val session = values[0] as? com.example.data.local.entity.CloudAuthSessionEntity
        val pending = values[1] as Int
        val failed = values[2] as Int
        val conflicts = values[3] as Int
        val latestSuccess = values[4] as? com.example.data.local.entity.SyncOperationLogEntity
        val isOnline = values[5] as Boolean
        val isSyncing = values[6] as Boolean
        val lastError = values[7] as String?

        val configured = isCloudConfiguredProvider()
        val overall = when {
            isSyncing -> SyncState.UPLOADING
            session == null || !configured -> SyncState.LOCAL_ONLY
            conflicts > 0 -> SyncState.CONFLICT
            failed > 0 -> SyncState.FAILED
            pending > 0 -> SyncState.PENDING_UPLOAD
            else -> SyncState.SYNCED
        }

        SyncSummaryState(
            isCloudConfigured = configured,
            isOnline = isOnline,
            isSyncing = isSyncing,
            overallState = overall,
            pendingUploadsCount = pending,
            pendingDownloadsCount = 0,
            openConflictsCount = conflicts,
            failedItemsCount = failed,
            lastSuccessfulSyncEpochMs = latestSuccess?.completedAtEpochMs ?: session?.lastSyncAtEpochMs,
            lastErrorSummary = lastError
        )
    }.stateIn(scope, SharingStarted.Eagerly, SyncSummaryState(isCloudConfigured = isCloudConfiguredProvider()))

    fun observeDiagnostics(): Flow<List<SyncDiagnosticsLog>> =
        database.syncDiagnosticsDao().observeLogs().map { list -> list.map { it.toDomain() } }

    fun setOnline(online: Boolean) {
        isOnlineFlow.value = online
    }

    fun setSyncing(syncing: Boolean) {
        isSyncingFlow.value = syncing
    }

    fun setLastError(error: String?) {
        lastErrorFlow.value = error
    }
}
