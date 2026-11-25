package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.MissionControlRepository
import com.example.domain.model.MissionCategory
import com.example.domain.model.MissionControlSnapshot
import com.example.domain.model.MissionPriority
import com.example.domain.model.MissionSourceModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface MissionControlUiState {
    data object Loading : MissionControlUiState
    data class Error(val message: String) : MissionControlUiState
    data class Ready(
        val snapshot: MissionControlSnapshot = MissionControlSnapshot(),
        val isBusy: Boolean = false,
        val feedbackMessage: String? = null
    ) : MissionControlUiState
}

class MissionControlViewModel(
    private val repository: MissionControlRepository
) : ViewModel() {

    private val isBusy = MutableStateFlow(false)
    private val feedbackMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<MissionControlUiState> = combine(
        repository.snapshotFlow,
        isBusy,
        feedbackMessage
    ) { snapshot, busy, msg ->
        MissionControlUiState.Ready(
            snapshot = snapshot,
            isBusy = busy,
            feedbackMessage = msg
        ) as MissionControlUiState
    }
        .catch { e -> emit(MissionControlUiState.Error(e.message ?: "Failed to load Mission Control.")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MissionControlUiState.Loading
        )

    init {
        viewModelScope.launch {
            runCatching { repository.evaluateOverdueEvents() }
        }
    }

    fun clearFeedback() {
        feedbackMessage.value = null
    }

    fun createMission(
        title: String,
        description: String,
        category: MissionCategory,
        priority: MissionPriority,
        module: MissionSourceModule,
        targetValue: Int,
        unit: String,
        startDateEpochDay: Long?,
        deadlineEpochDay: Long?,
        prerequisiteMissionIds: List<String>
    ) {
        viewModelScope.launch {
            isBusy.value = true
            repository.createMission(
                title = title,
                description = description,
                category = category,
                priority = priority,
                module = module,
                targetValue = targetValue,
                unit = unit,
                startDateEpochDay = startDateEpochDay,
                deadlineEpochDay = deadlineEpochDay,
                prerequisiteMissionIds = prerequisiteMissionIds
            ).onSuccess {
                feedbackMessage.value = "Mission created and queued in Mission Control."
            }.onFailure { e ->
                feedbackMessage.value = e.message ?: "Could not create mission."
            }
            isBusy.value = false
        }
    }

    fun updateMission(
        missionId: String,
        title: String,
        description: String,
        priority: MissionPriority,
        module: MissionSourceModule,
        targetValue: Int,
        unit: String,
        startDateEpochDay: Long?,
        deadlineEpochDay: Long?,
        prerequisiteMissionIds: List<String>
    ) {
        viewModelScope.launch {
            isBusy.value = true
            repository.updateMission(
                missionId = missionId,
                title = title,
                description = description,
                priority = priority,
                module = module,
                targetValue = targetValue,
                unit = unit,
                startDateEpochDay = startDateEpochDay,
                deadlineEpochDay = deadlineEpochDay,
                prerequisiteMissionIds = prerequisiteMissionIds
            ).onSuccess {
                feedbackMessage.value = "Mission updated."
            }.onFailure { e ->
                feedbackMessage.value = e.message ?: "Could not update mission."
            }
            isBusy.value = false
        }
    }

    fun togglePauseMission(missionId: String) {
        viewModelScope.launch {
            repository.togglePauseMission(missionId)
                .onSuccess { paused ->
                    feedbackMessage.value = if (paused) "Mission paused." else "Mission resumed."
                }
                .onFailure { e ->
                    feedbackMessage.value = e.message ?: "Unable to toggle pause."
                }
        }
    }

    fun archiveMission(missionId: String) {
        viewModelScope.launch {
            repository.archiveMission(missionId)
                .onSuccess {
                    feedbackMessage.value = "Mission moved to Mission History."
                }
                .onFailure { e ->
                    feedbackMessage.value = e.message ?: "Unable to archive mission."
                }
        }
    }

    fun completeMission(missionId: String) {
        viewModelScope.launch {
            isBusy.value = true
            repository.completeMission(missionId)
                .onSuccess { msg ->
                    feedbackMessage.value = msg
                }
                .onFailure { e ->
                    feedbackMessage.value = e.message ?: "Unable to complete mission."
                }
            isBusy.value = false
        }
    }

    companion object {
        fun factory(repository: MissionControlRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MissionControlViewModel(repository) as T
                }
            }
    }
}
