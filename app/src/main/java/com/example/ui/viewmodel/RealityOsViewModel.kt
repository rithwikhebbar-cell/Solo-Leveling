package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.healthconnect.HealthConnectPermissionState
import com.example.data.healthconnect.HealthConnectSdkAvailability
import com.example.data.integration.CloudSyncStatus
import com.example.data.integration.HealthConnectReadiness
import com.example.data.integration.SystemAdvisory
import com.example.data.preferences.UserPreferences
import com.example.data.repository.RealityOsRepository
import com.example.domain.engine.NextRankProgress
import com.example.domain.engine.ProgressionEngine
import com.example.domain.engine.RecoveryEngine
import com.example.domain.model.ActiveJoggingSessionState
import com.example.domain.model.ActivityLog
import com.example.domain.model.ExerciseRecord
import com.example.domain.model.HealthConnectSyncState
import com.example.domain.model.ImportedHealthActivity
import com.example.domain.model.JoggingSession
import com.example.domain.model.JoggingTargetProposal
import com.example.domain.model.PhysicalActivityKind
import com.example.domain.model.PhysicalDevelopmentState
import com.example.domain.model.PlayerProfile
import com.example.domain.model.Quest
import com.example.domain.model.QuestCategory
import com.example.domain.model.QuestType
import com.example.domain.model.RecoveryAssessment
import com.example.domain.model.SystemNotification
import com.example.domain.model.WorkoutCategory
import com.example.domain.model.WorkoutSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

data class PhysicalDashboardBundle(
    val joggingSessions: List<JoggingSession>,
    val workoutSessions: List<WorkoutSession>,
    val exerciseRecords: List<ExerciseRecord>,
    val recoveryAssessment: RecoveryAssessment,
    val recoveryHistory: List<RecoveryAssessment>,
    val importedHealthActivities: List<ImportedHealthActivity>,
    val healthConnectSyncState: HealthConnectSyncState,
    val healthConnectAvailability: HealthConnectSdkAvailability,
    val healthConnectPermissions: HealthConnectPermissionState,
    val isSyncingHealthConnect: Boolean,
    val syncFeedbackMessage: String?,
    val pendingJoggingProposal: JoggingTargetProposal?,
    val activeJoggingSession: ActiveJoggingSessionState,
    val advisoryWorkoutCategory: Pair<WorkoutCategory, String>
)

sealed interface RealityOsUiState {
    data object Loading : RealityOsUiState
    data class Error(val message: String) : RealityOsUiState
    data class Ready(
        val player: PlayerProfile,
        val nextRankProgress: NextRankProgress,
        val quests: List<Quest>,
        val physicalState: PhysicalDevelopmentState,
        val notifications: List<SystemNotification>,
        val activityLogs: List<ActivityLog>,
        val preferences: UserPreferences,
        val healthConnectReadiness: HealthConnectReadiness,
        val cloudSyncStatus: CloudSyncStatus,
        val currentAdvisory: SystemAdvisory,
        val physicalBundle: PhysicalDashboardBundle
    ) : RealityOsUiState
}

private data class CoreFlowSnapshot(
    val player: PlayerProfile,
    val quests: List<Quest>,
    val physical: PhysicalDevelopmentState,
    val notifications: List<SystemNotification>,
    val logs: List<ActivityLog>,
    val prefs: UserPreferences
)

private data class PhysicalRecordsSnapshot(
    val jogs: List<JoggingSession>,
    val workouts: List<WorkoutSession>,
    val exercises: List<ExerciseRecord>,
    val recovery: RecoveryAssessment,
    val recoveryHistory: List<RecoveryAssessment>,
    val imported: List<ImportedHealthActivity>,
    val syncState: HealthConnectSyncState
)

private data class TransientPhysicalUiState(
    val permissions: HealthConnectPermissionState = HealthConnectPermissionState(),
    val isSyncing: Boolean = false,
    val feedbackMessage: String? = null,
    val dismissedProposalMinutes: Int? = null,
    val activeJogging: ActiveJoggingSessionState = ActiveJoggingSessionState()
)

class RealityOsViewModel(
    private val repository: RealityOsRepository
) : ViewModel() {

    private val retryTrigger = MutableStateFlow(0)
    private val transientPhysicalState = MutableStateFlow(TransientPhysicalUiState())

    private val coreFlow = combine(
        repository.playerProfile,
        repository.quests,
        repository.physicalState,
        repository.notifications,
        repository.activityLogs
    ) { player, quests, physical, notifications, logs ->
        CoreFlowSnapshot(player, quests, physical, notifications, logs, com.example.data.preferences.UserPreferences())
    }.combine(repository.userPreferences) { snapshot, prefs ->
        snapshot.copy(prefs = prefs)
    }

    private val physicalHistoryFlow = combine(
        repository.joggingSessions,
        repository.workoutSessions,
        repository.exerciseRecords,
        repository.latestRecoveryAssessment,
        repository.recoveryHistory
    ) { jogs, workouts, exercises, recovery, recHistory ->
        PhysicalRecordsSnapshot(
            jogs = jogs,
            workouts = workouts,
            exercises = exercises,
            recovery = recovery,
            recoveryHistory = recHistory,
            imported = emptyList(),
            syncState = HealthConnectSyncState()
        )
    }.combine(
        combine(repository.importedHealthActivities, repository.healthConnectSyncState) { imported, syncState ->
            imported to syncState
        }
    ) { snap, (imported, syncState) ->
        snap.copy(imported = imported, syncState = syncState)
    }

    val uiState: StateFlow<RealityOsUiState> = combine(
        coreFlow,
        physicalHistoryFlow,
        transientPhysicalState,
        retryTrigger
    ) { core, physRecords, transient, _ ->
        val nextRank = ProgressionEngine.evaluateNextRankProgress(core.player)
        val advisory = repository.aiGameMasterPort.generateLocalAdvisory(core.player, core.physical)

        val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60_000L
        val zone = ZoneId.systemDefault()
        val weeklyJogDays = physRecords.jogs
            .filter { it.startEpochMs >= sevenDaysAgo && it.activityKind != PhysicalActivityKind.WALKING }
            .map { Instant.ofEpochMilli(it.startEpochMs).atZone(zone).toLocalDate() }
            .toSet()
            .size

        val rawProposal = RecoveryEngine.proposeJoggingTargetAdjustment(
            currentTargetMinutes = core.physical.effectiveJoggingTarget,
            baseTargetMinutes = core.physical.baseJoggingTargetMinutes,
            recoveryState = physRecords.recovery.state,
            weeklyJoggingDays = weeklyJogDays
        )
        val proposal = if (rawProposal != null && rawProposal.proposedMinutes != transient.dismissedProposalMinutes) {
            rawProposal
        } else {
            null
        }

        val bundle = PhysicalDashboardBundle(
            joggingSessions = physRecords.jogs,
            workoutSessions = physRecords.workouts,
            exerciseRecords = physRecords.exercises,
            recoveryAssessment = physRecords.recovery,
            recoveryHistory = physRecords.recoveryHistory,
            importedHealthActivities = physRecords.imported,
            healthConnectSyncState = physRecords.syncState,
            healthConnectAvailability = repository.getHealthConnectAvailability(),
            healthConnectPermissions = transient.permissions,
            isSyncingHealthConnect = transient.isSyncing,
            syncFeedbackMessage = transient.feedbackMessage,
            pendingJoggingProposal = proposal,
            activeJoggingSession = transient.activeJogging,
            advisoryWorkoutCategory = repository.aiGameMasterPort.suggestAdvisoryWorkoutCategory(physRecords.recovery)
        )

        RealityOsUiState.Ready(
            player = core.player,
            nextRankProgress = nextRank,
            quests = core.quests,
            physicalState = core.physical,
            notifications = core.notifications,
            activityLogs = core.logs,
            preferences = core.prefs,
            healthConnectReadiness = repository.healthConnectPort.checkReadiness(),
            cloudSyncStatus = repository.cloudSyncPort.getSyncStatus(),
            currentAdvisory = advisory,
            physicalBundle = bundle
        ) as RealityOsUiState
    }
        .catch { throwable ->
            emit(RealityOsUiState.Error(throwable.message ?: "Failed to synchronize local Reality OS database."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = RealityOsUiState.Loading
        )

    init {
        viewModelScope.launch {
            repository.ensureInitialized()
            refreshHealthConnectPermissions()
        }
    }

    fun retryInitialization() {
        viewModelScope.launch {
            repository.ensureInitialized()
            refreshHealthConnectPermissions()
            retryTrigger.value += 1
        }
    }

    fun refreshHealthConnectPermissions() {
        viewModelScope.launch {
            val perms = repository.getHealthConnectPermissionState()
            transientPhysicalState.value = transientPhysicalState.value.copy(permissions = perms)
        }
    }

    fun syncHealthConnect() {
        viewModelScope.launch {
            transientPhysicalState.value = transientPhysicalState.value.copy(
                isSyncing = true,
                feedbackMessage = null
            )
            val perms = repository.getHealthConnectPermissionState()
            val summary = repository.syncHealthConnectHistory(daysBack = 14)
            val feedback = if (summary.errorMessage != null) {
                summary.errorMessage
            } else {
                "Sync complete: ${summary.importedCount} imported (+${summary.totalXpAwarded} XP), ${summary.duplicateCount} duplicates skipped, ${summary.rejectedCount} rejected."
            }
            transientPhysicalState.value = transientPhysicalState.value.copy(
                permissions = perms,
                isSyncing = false,
                feedbackMessage = feedback
            )
        }
    }

    fun clearSyncFeedback() {
        transientPhysicalState.value = transientPhysicalState.value.copy(feedbackMessage = null)
    }

    fun startActiveJogging() {
        val now = System.currentTimeMillis()
        transientPhysicalState.value = transientPhysicalState.value.copy(
            activeJogging = ActiveJoggingSessionState(
                isActive = true,
                isPaused = false,
                startEpochMs = now,
                accumulatedElapsedMs = 0L,
                lastResumedAtMs = now
            )
        )
    }

    fun pauseOrResumeActiveJogging() {
        val current = transientPhysicalState.value.activeJogging
        if (!current.isActive) return
        val now = System.currentTimeMillis()
        val updated = if (current.isPaused) {
            current.copy(isPaused = false, lastResumedAtMs = now)
        } else {
            val delta = (now - current.lastResumedAtMs).coerceAtLeast(0L)
            current.copy(
                isPaused = true,
                accumulatedElapsedMs = current.accumulatedElapsedMs + delta,
                lastResumedAtMs = 0L
            )
        }
        transientPhysicalState.value = transientPhysicalState.value.copy(activeJogging = updated)
    }

    fun discardActiveJogging() {
        transientPhysicalState.value = transientPhysicalState.value.copy(
            activeJogging = ActiveJoggingSessionState()
        )
    }

    fun completeActiveJogging(
        title: String,
        distanceKmText: String,
        notes: String
    ) {
        val active = transientPhysicalState.value.activeJogging
        if (!active.isActive) return
        val now = System.currentTimeMillis()
        val elapsedSeconds = active.currentElapsedSeconds(now)
        val durationMinutes = ((elapsedSeconds + 59L) / 60L).toInt().coerceAtLeast(1)
        val distanceMeters = distanceKmText.toDoubleOrNull()?.takeIf { it > 0.0 }?.let { it * 1000.0 }

        viewModelScope.launch {
            val result = repository.recordJoggingSession(
                title = title.ifBlank { "Live Jogging Session" },
                startEpochMs = active.startEpochMs,
                endEpochMs = now,
                durationMinutes = durationMinutes,
                distanceMeters = distanceMeters,
                isLiveSystemTimer = true,
                notes = notes
            )
            transientPhysicalState.value = transientPhysicalState.value.copy(
                activeJogging = ActiveJoggingSessionState(),
                feedbackMessage = result.fold(
                    onSuccess = { "Jogging recorded (${it.durationMinutes} min, +${it.xpAwarded} XP)." },
                    onFailure = { it.message ?: "Unable to record jogging session." }
                )
            )
        }
    }

    fun logManualJoggingSession(
        title: String,
        durationMinutes: Int,
        distanceKmText: String,
        notes: String
    ) {
        val endMs = System.currentTimeMillis()
        val startMs = endMs - durationMinutes.coerceAtLeast(1) * 60_000L
        val distanceMeters = distanceKmText.toDoubleOrNull()?.takeIf { it > 0.0 }?.let { it * 1000.0 }

        viewModelScope.launch {
            val result = repository.recordJoggingSession(
                title = title.ifBlank { "Jogging Session" },
                startEpochMs = startMs,
                endEpochMs = endMs,
                durationMinutes = durationMinutes,
                distanceMeters = distanceMeters,
                isLiveSystemTimer = false,
                notes = notes
            )
            transientPhysicalState.value = transientPhysicalState.value.copy(
                feedbackMessage = result.fold(
                    onSuccess = { "Self-reported jogging saved (+${it.xpAwarded} XP)." },
                    onFailure = { it.message ?: "Invalid jogging session." }
                )
            )
        }
    }

    fun recordStructuredWorkout(
        title: String,
        category: WorkoutCategory,
        durationMinutes: Int,
        exercises: List<ExerciseRecord>,
        isLiveTimed: Boolean,
        notes: String
    ) {
        viewModelScope.launch {
            val result = repository.recordStructuredWorkout(
                title = title,
                category = category,
                durationMinutes = durationMinutes,
                exercises = exercises,
                isLiveSystemTimer = isLiveTimed,
                notes = notes
            )
            transientPhysicalState.value = transientPhysicalState.value.copy(
                feedbackMessage = result.fold(
                    onSuccess = { "Workout saved: ${it.totalSets} sets (+${it.xpAwarded} XP)." },
                    onFailure = { it.message ?: "Unable to save workout." }
                )
            )
        }
    }

    fun submitRecoveryCheckIn(
        fatigueLevel: Int,
        hasDiscomfort: Boolean,
        discomfortNotes: String
    ) {
        viewModelScope.launch {
            repository.submitRecoveryCheckIn(fatigueLevel, hasDiscomfort, discomfortNotes)
            transientPhysicalState.value = transientPhysicalState.value.copy(
                feedbackMessage = "Recovery assessment updated."
            )
        }
    }

    fun markPenaltyFreeRestDay() {
        viewModelScope.launch {
            repository.markPenaltyFreeRestDay()
            transientPhysicalState.value = transientPhysicalState.value.copy(
                feedbackMessage = "Penalty-free rest day recorded."
            )
        }
    }

    fun acceptJoggingTargetProposal(proposedMinutes: Int) {
        viewModelScope.launch {
            repository.acceptJoggingTargetProposal(proposedMinutes)
            transientPhysicalState.value = transientPhysicalState.value.copy(
                dismissedProposalMinutes = null,
                feedbackMessage = "Daily jogging target updated to $proposedMinutes min."
            )
        }
    }

    fun dismissJoggingTargetProposal(proposedMinutes: Int) {
        transientPhysicalState.value = transientPhysicalState.value.copy(
            dismissedProposalMinutes = proposedMinutes
        )
    }

    fun completeQuest(questId: Long) {
        viewModelScope.launch {
            repository.completeQuest(questId)
        }
    }

    fun createCustomQuest(
        title: String,
        description: String,
        type: QuestType,
        category: QuestCategory,
        targetValue: Int,
        unit: String
    ) {
        viewModelScope.launch {
            repository.createCustomQuest(
                title = title,
                description = description,
                type = type,
                category = category,
                targetValue = targetValue,
                unit = unit
            )
        }
    }

    fun logManualActivity(
        title: String,
        category: QuestCategory,
        durationMinutes: Int,
        isJoggingSession: Boolean,
        notes: String
    ) {
        viewModelScope.launch {
            repository.logManualActivity(
                title = title,
                category = category,
                durationMinutes = durationMinutes,
                isJoggingSession = isJoggingSession,
                notes = notes
            )
        }
    }

    fun refreshAiTrainingAdvisory() {
        val current = uiState.value as? RealityOsUiState.Ready ?: return
        viewModelScope.launch {
            repository.refreshAiTrainingAdvisory(current.player, current.physicalState)
        }
    }

    fun acknowledgeNotification(id: Long) {
        viewModelScope.launch {
            repository.acknowledgeNotification(id)
        }
    }

    fun clearReadNotifications() {
        viewModelScope.launch {
            repository.clearReadNotifications()
        }
    }

    fun updateCodename(codename: String) {
        viewModelScope.launch {
            repository.updateCodename(codename)
        }
    }

    fun updateJoggingBaseline(minutes: Int) {
        viewModelScope.launch {
            repository.updateJoggingBaseline(minutes)
        }
    }

    fun setHudGlowEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setHudGlowEnabled(enabled)
        }
    }

    fun setHealthConnectSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setHealthConnectSyncEnabled(enabled)
        }
    }

    fun setSupabaseCloudSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setSupabaseCloudSyncEnabled(enabled)
        }
    }

    companion object {
        fun factory(repository: RealityOsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return RealityOsViewModel(repository) as T
                }
            }
    }
}
