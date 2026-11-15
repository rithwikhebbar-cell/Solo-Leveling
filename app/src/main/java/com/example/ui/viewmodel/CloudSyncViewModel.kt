package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.SyncRepository
import com.example.domain.model.CloudBackupRecord
import com.example.domain.model.ConflictResolutionChoice
import com.example.domain.model.DeviceRegistration
import com.example.domain.model.PlayerIdentity
import com.example.domain.model.RestorePreview
import com.example.domain.model.SyncConflict
import com.example.domain.model.SyncDiagnosticsLog
import com.example.domain.model.SyncQueueItem
import com.example.domain.model.SyncSummaryState
import com.example.worker.CloudSyncWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CloudSyncUiState(
    val identity: PlayerIdentity? = null,
    val summary: SyncSummaryState = SyncSummaryState(),
    val devices: List<DeviceRegistration> = emptyList(),
    val queue: List<SyncQueueItem> = emptyList(),
    val conflicts: List<SyncConflict> = emptyList(),
    val backups: List<CloudBackupRecord> = emptyList(),
    val diagnostics: List<SyncDiagnosticsLog> = emptyList(),
    val restorePreview: RestorePreview? = null,
    val isBusy: Boolean = false,
    val feedbackMessage: String? = null,
    val errorMessage: String? = null
)

class CloudSyncViewModel(
    private val syncRepository: SyncRepository
) : ViewModel() {

    private val restorePreviewFlow = MutableStateFlow<RestorePreview?>(null)
    private val isBusyFlow = MutableStateFlow(false)
    private val feedbackFlow = MutableStateFlow<String?>(null)
    private val errorFlow = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CloudSyncUiState> = combine(
        syncRepository.observePlayerIdentity(),
        syncRepository.syncSummaryState,
        syncRepository.observeDevices(),
        syncRepository.observePendingQueue(),
        syncRepository.observeConflicts(),
        syncRepository.observeBackups(),
        syncRepository.observeDiagnostics(),
        restorePreviewFlow,
        isBusyFlow,
        feedbackFlow,
        errorFlow
    ) { vals ->
        @Suppress("UNCHECKED_CAST")
        CloudSyncUiState(
            identity = vals[0] as PlayerIdentity?,
            summary = vals[1] as SyncSummaryState,
            devices = vals[2] as List<DeviceRegistration>,
            queue = vals[3] as List<SyncQueueItem>,
            conflicts = vals[4] as List<SyncConflict>,
            backups = vals[5] as List<CloudBackupRecord>,
            diagnostics = vals[6] as List<SyncDiagnosticsLog>,
            restorePreview = vals[7] as RestorePreview?,
            isBusy = vals[8] as Boolean,
            feedbackMessage = vals[9] as String?,
            errorMessage = vals[10] as String?
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), CloudSyncUiState())

    fun clearMessages() {
        feedbackFlow.value = null
        errorFlow.value = null
    }

    fun signUp(email: String, password: String, displayName: String, onSuccess: (PlayerIdentity) -> Unit) {
        viewModelScope.launch {
            isBusyFlow.value = true
            clearMessages()
            syncRepository.signUp(email, password, displayName)
                .onSuccess { identity ->
                    feedbackFlow.value = if (identity.emailVerified) {
                        "Account created & linked."
                    } else {
                        "Account created. Check ${identity.email} to verify your email."
                    }
                    onSuccess(identity)
                }
                .onFailure { errorFlow.value = it.message ?: "Sign up failed." }
            isBusyFlow.value = false
        }
    }

    fun signIn(
        email: String,
        password: String,
        appContext: Context? = null,
        onSuccess: (PlayerIdentity) -> Unit
    ) {
        viewModelScope.launch {
            isBusyFlow.value = true
            clearMessages()
            syncRepository.signIn(email, password)
                .onSuccess { identity ->
                    feedbackFlow.value = "Authenticated as ${identity.displayName}."
                    if (appContext != null) {
                        CloudSyncWorker.enqueueImmediateSync(appContext, "POST_LOGIN")
                    }
                    onSuccess(identity)
                }
                .onFailure { errorFlow.value = it.message ?: "Login failed." }
            isBusyFlow.value = false
        }
    }

    fun resendVerificationEmail() {
        viewModelScope.launch {
            isBusyFlow.value = true
            clearMessages()
            syncRepository.resendVerificationEmail()
                .onSuccess { feedbackFlow.value = it }
                .onFailure { errorFlow.value = it.message ?: "Could not resend email." }
            isBusyFlow.value = false
        }
    }

    fun refreshVerificationStatus(onVerified: () -> Unit = {}) {
        viewModelScope.launch {
            isBusyFlow.value = true
            clearMessages()
            syncRepository.refreshSessionAndVerification()
                .onSuccess { id ->
                    if (id.emailVerified) {
                        feedbackFlow.value = "Email verified. Cloud Sync active."
                        onVerified()
                    } else {
                        feedbackFlow.value = "Verification still pending. Check your inbox."
                    }
                }
                .onFailure { errorFlow.value = it.message ?: "Verification refresh failed." }
            isBusyFlow.value = false
        }
    }

    fun sendPasswordRecovery(email: String, onSent: () -> Unit) {
        viewModelScope.launch {
            isBusyFlow.value = true
            clearMessages()
            syncRepository.sendPasswordRecovery(email)
                .onSuccess {
                    feedbackFlow.value = it
                    onSent()
                }
                .onFailure { errorFlow.value = it.message ?: "Recovery request failed." }
            isBusyFlow.value = false
        }
    }

    fun resetPassword(newPassword: String, onDone: () -> Unit) {
        viewModelScope.launch {
            isBusyFlow.value = true
            clearMessages()
            syncRepository.resetPassword(newPassword)
                .onSuccess {
                    feedbackFlow.value = it
                    onDone()
                }
                .onFailure { errorFlow.value = it.message ?: "Password update failed." }
            isBusyFlow.value = false
        }
    }

    fun updateDisplayName(newName: String) {
        viewModelScope.launch {
            isBusyFlow.value = true
            clearMessages()
            syncRepository.updateDisplayName(newName)
                .onSuccess { feedbackFlow.value = it }
                .onFailure { errorFlow.value = it.message ?: "Could not update display name." }
            isBusyFlow.value = false
        }
    }

    fun setCloudSyncConsent(granted: Boolean) {
        viewModelScope.launch { syncRepository.setCloudSyncConsent(granted) }
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        viewModelScope.launch { syncRepository.setAutoSyncEnabled(enabled) }
    }

    fun signOut(onSignedOut: () -> Unit = {}) {
        viewModelScope.launch {
            isBusyFlow.value = true
            clearMessages()
            syncRepository.signOut()
            feedbackFlow.value = "Signed out. Switched to Offline Vault mode."
            isBusyFlow.value = false
            onSignedOut()
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            isBusyFlow.value = true
            clearMessages()
            syncRepository.syncNow("MANUAL")
                .onSuccess { log ->
                    feedbackFlow.value = "Sync ${log.status.label}: ↑${log.recordsUploaded} ↓${log.recordsDownloaded} (${log.conflictsDetected} conflicts)"
                }
                .onFailure { errorFlow.value = it.message ?: "Synchronization failed." }
            isBusyFlow.value = false
        }
    }

    fun retryQueueItem(queueId: Long) {
        viewModelScope.launch {
            syncRepository.retryQueueItem(queueId)
            feedbackFlow.value = "Item re-queued for upload."
        }
    }

    fun resolveConflict(conflictId: Long, choice: ConflictResolutionChoice) {
        viewModelScope.launch {
            isBusyFlow.value = true
            clearMessages()
            syncRepository.resolveConflict(conflictId, choice)
                .onSuccess { feedbackFlow.value = it }
                .onFailure { errorFlow.value = it.message ?: "Conflict resolution failed." }
            isBusyFlow.value = false
        }
    }

    fun refreshDevices() {
        viewModelScope.launch {
            isBusyFlow.value = true
            syncRepository.refreshDevices()
                .onFailure { errorFlow.value = it.message ?: "Unable to refresh devices." }
            isBusyFlow.value = false
        }
    }

    fun revokeDevice(deviceUuid: String) {
        viewModelScope.launch {
            isBusyFlow.value = true
            clearMessages()
            syncRepository.revokeDevice(deviceUuid)
                .onSuccess { feedbackFlow.value = it }
                .onFailure { errorFlow.value = it.message ?: "Failed to revoke device." }
            isBusyFlow.value = false
        }
    }

    fun createBackupSnapshot() {
        viewModelScope.launch {
            isBusyFlow.value = true
            clearMessages()
            syncRepository.createCloudBackupSnapshot()
                .onSuccess { rec ->
                    feedbackFlow.value = "Backup ${rec.status.label}: ${rec.recordCount} records verified."
                }
                .onFailure { errorFlow.value = it.message ?: "Backup creation failed." }
            isBusyFlow.value = false
        }
    }

    fun loadRestorePreview(backupUuid: String, onLoaded: () -> Unit) {
        viewModelScope.launch {
            isBusyFlow.value = true
            clearMessages()
            syncRepository.buildRestorePreview(backupUuid)
                .onSuccess { preview ->
                    restorePreviewFlow.value = preview
                    onLoaded()
                }
                .onFailure { errorFlow.value = it.message ?: "Unable to inspect backup." }
            isBusyFlow.value = false
        }
    }

    fun executeRestore(backupUuid: String, onRestored: () -> Unit) {
        viewModelScope.launch {
            isBusyFlow.value = true
            clearMessages()
            syncRepository.executeRestore(backupUuid)
                .onSuccess {
                    feedbackFlow.value = it
                    restorePreviewFlow.value = null
                    onRestored()
                }
                .onFailure { errorFlow.value = it.message ?: "Restore failed." }
            isBusyFlow.value = false
        }
    }

    fun deleteAccount(confirmationPhrase: String, password: String, onDeleted: () -> Unit) {
        viewModelScope.launch {
            isBusyFlow.value = true
            clearMessages()
            syncRepository.deleteAccount(confirmationPhrase, password)
                .onSuccess {
                    feedbackFlow.value = it
                    onDeleted()
                }
                .onFailure { errorFlow.value = it.message ?: "Account deletion failed." }
            isBusyFlow.value = false
        }
    }

    companion object {
        fun factory(syncRepository: SyncRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CloudSyncViewModel(syncRepository) as T
                }
            }
    }
}
