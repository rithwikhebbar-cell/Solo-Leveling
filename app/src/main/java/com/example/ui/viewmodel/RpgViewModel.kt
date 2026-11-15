package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AiSystemRepository
import com.example.data.repository.RpgDashboardSnapshot
import com.example.data.repository.RpgRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface RpgUiState {
    data object Loading : RpgUiState
    data class Error(val message: String) : RpgUiState
    data class Ready(
        val snapshot: RpgDashboardSnapshot,
        val feedbackBanner: String? = null,
        val isBusy: Boolean = false
    ) : RpgUiState
}

class RpgViewModel(
    private val rpgRepository: RpgRepository,
    private val aiSystemRepository: AiSystemRepository
) : ViewModel() {

    private val feedbackState = MutableStateFlow<String?>(null)
    private val busyState = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            runCatching { rpgRepository.ensureRpgSeededAndSynced() }
        }
    }

    val uiState: StateFlow<RpgUiState> = combine(
        rpgRepository.rpgStateFlow,
        feedbackState,
        busyState
    ) { snapshot, feedback, busy ->
        RpgUiState.Ready(
            snapshot = snapshot,
            feedbackBanner = feedback,
            isBusy = busy
        ) as RpgUiState
    }.catch { err ->
        emit(RpgUiState.Error(err.message ?: "Failed to load RPG Systems."))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RpgUiState.Loading
    )

    fun refreshAndSync() {
        viewModelScope.launch {
            busyState.value = true
            runCatching { rpgRepository.ensureRpgSeededAndSynced() }
                .onFailure { feedbackState.value = it.message ?: "Sync failed." }
            busyState.value = false
        }
    }

    fun clearFeedback() {
        feedbackState.value = null
    }

    fun enterDungeon(dungeonId: Long, onEntered: (Long) -> Unit = {}) {
        viewModelScope.launch {
            busyState.value = true
            rpgRepository.enterDungeon(dungeonId)
                .onSuccess { instId ->
                    feedbackState.value = "Gate entered. Complete objectives with real records to clear."
                    onEntered(instId)
                }
                .onFailure { feedbackState.value = it.message ?: "Unable to enter gate." }
            busyState.value = false
        }
    }

    fun pauseOrResumeDungeon(instanceId: Long) {
        viewModelScope.launch {
            rpgRepository.pauseOrResumeDungeon(instanceId)
                .onSuccess { feedbackState.value = it }
                .onFailure { feedbackState.value = it.message }
        }
    }

    fun completeDungeon(instanceId: Long) {
        viewModelScope.launch {
            busyState.value = true
            rpgRepository.completeDungeon(instanceId)
                .onSuccess { feedbackState.value = it }
                .onFailure { feedbackState.value = it.message ?: "Validation failed." }
            busyState.value = false
        }
    }

    fun abandonDungeon(instanceId: Long, onAbandoned: () -> Unit = {}) {
        viewModelScope.launch {
            rpgRepository.abandonDungeon(instanceId)
                .onSuccess {
                    feedbackState.value = it
                    onAbandoned()
                }
                .onFailure { feedbackState.value = it.message }
        }
    }

    fun startBossBattle(bossId: Long, onStarted: (Long) -> Unit = {}) {
        viewModelScope.launch {
            busyState.value = true
            rpgRepository.startBossBattle(bossId)
                .onSuccess { instId ->
                    feedbackState.value = "Boss Battle engaged. Phase objectives now track real telemetry."
                    onStarted(instId)
                }
                .onFailure { feedbackState.value = it.message }
            busyState.value = false
        }
    }

    fun refreshAndClaimBossBattle(instanceId: Long) {
        viewModelScope.launch {
            busyState.value = true
            rpgRepository.refreshAndClaimBossBattle(instanceId)
                .onSuccess { feedbackState.value = it }
                .onFailure { feedbackState.value = it.message }
            busyState.value = false
        }
    }

    fun abandonBossBattle(instanceId: Long, onAbandoned: () -> Unit = {}) {
        viewModelScope.launch {
            rpgRepository.abandonBossBattle(instanceId)
                .onSuccess {
                    feedbackState.value = it
                    onAbandoned()
                }
                .onFailure { feedbackState.value = it.message }
        }
    }

    fun unlockOrUpgradeSkill(skillId: String) {
        viewModelScope.launch {
            busyState.value = true
            rpgRepository.unlockOrUpgradeSkill(skillId)
                .onSuccess { feedbackState.value = it }
                .onFailure { feedbackState.value = it.message }
            busyState.value = false
        }
    }

    fun equipTitle(titleId: String) {
        viewModelScope.launch {
            rpgRepository.equipTitle(titleId)
                .onSuccess { feedbackState.value = it }
                .onFailure { feedbackState.value = it.message }
        }
    }

    fun toggleEquipInventoryItem(itemId: String) {
        viewModelScope.launch {
            rpgRepository.toggleEquipInventoryItem(itemId)
                .onSuccess { feedbackState.value = it }
                .onFailure { feedbackState.value = it.message }
        }
    }

    fun generateAiDungeonProposals() {
        viewModelScope.launch {
            busyState.value = true
            runCatching { aiSystemRepository.generateDungeonProposals() }
                .onSuccess { feedbackState.value = it.note }
                .onFailure { feedbackState.value = it.message ?: "Unable to generate AI dungeon proposals." }
            busyState.value = false
        }
    }

    fun approveAiDungeonProposal(proposalId: Long) {
        viewModelScope.launch {
            rpgRepository.approveAiDungeonProposal(proposalId)
                .onSuccess { feedbackState.value = "AI Dungeon proposal approved and added to Gate Catalog (0 XP until cleared)." }
                .onFailure { feedbackState.value = it.message }
        }
    }

    fun rejectAiDungeonProposal(proposalId: Long) {
        viewModelScope.launch {
            rpgRepository.rejectAiDungeonProposal(proposalId)
            feedbackState.value = "Proposal dismissed."
        }
    }

    companion object {
        fun factory(
            rpgRepository: RpgRepository,
            aiSystemRepository: AiSystemRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return RpgViewModel(rpgRepository, aiSystemRepository) as T
            }
        }
    }
}
