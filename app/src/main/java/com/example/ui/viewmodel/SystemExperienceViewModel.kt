package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.RealityOsRepository
import com.example.data.repository.SystemExperienceRepository
import com.example.domain.model.AwakeningStage
import com.example.domain.model.CelebrationEvent
import com.example.domain.model.PlayerProfile
import com.example.domain.model.SystemAnnouncementRecord
import com.example.domain.model.SystemExperiencePreferences
import com.example.domain.model.TtsSpeechPriority
import com.example.domain.model.VoiceCommandHistoryRecord
import com.example.domain.model.VoiceCommandResolution
import com.example.domain.model.VoiceEngineAvailability
import com.example.domain.model.WidgetSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface SystemExperienceUiState {
    data object Loading : SystemExperienceUiState
    data class Error(val message: String) : SystemExperienceUiState
    data class Ready(
        val preferences: SystemExperiencePreferences,
        val player: PlayerProfile,
        val unacknowledgedCelebrations: List<CelebrationEvent>,
        val celebrationHistory: List<CelebrationEvent>,
        val announcements: List<SystemAnnouncementRecord>,
        val voiceHistory: List<VoiceCommandHistoryRecord>,
        val voiceAvailability: VoiceEngineAvailability,
        val lastSpokenMessage: String?,
        val widgetSnapshot: WidgetSnapshot,
        val activeReplayCelebration: CelebrationEvent?,
        val pendingVoiceConfirmation: VoiceCommandResolution?,
        val lastVoiceResolution: VoiceCommandResolution?,
        val feedbackMessage: String?
    ) : SystemExperienceUiState
}

private data class ExperienceTransientState(
    val activeReplayCelebration: CelebrationEvent? = null,
    val pendingVoiceConfirmation: VoiceCommandResolution? = null,
    val lastVoiceResolution: VoiceCommandResolution? = null,
    val feedbackMessage: String? = null
)

private data class ExperienceCoreBundle(
    val prefs: SystemExperiencePreferences,
    val player: PlayerProfile,
    val unacked: List<CelebrationEvent>,
    val history: List<CelebrationEvent>,
    val announcements: List<SystemAnnouncementRecord>
)

private data class ExperienceIoBundle(
    val voiceHistory: List<VoiceCommandHistoryRecord>,
    val voiceAvailability: VoiceEngineAvailability,
    val lastSpoken: String?,
    val widgetSnapshot: WidgetSnapshot
)

class SystemExperienceViewModel(
    private val experienceRepository: SystemExperienceRepository,
    private val realityOsRepository: RealityOsRepository
) : ViewModel() {

    private val transient = MutableStateFlow(ExperienceTransientState())

    private val coreBundleFlow = combine(
        experienceRepository.observePreferences(),
        realityOsRepository.playerProfile,
        experienceRepository.observeUnacknowledgedCelebrations(),
        experienceRepository.observeCelebrations(),
        experienceRepository.observeAnnouncements()
    ) { prefs, player, unacked, history, announcements ->
        ExperienceCoreBundle(prefs, player, unacked, history, announcements)
    }

    private val ioBundleFlow = combine(
        experienceRepository.observeVoiceHistory(),
        experienceRepository.voiceAvailability,
        experienceRepository.lastSpokenMessage,
        experienceRepository.widgetSnapshot
    ) { voiceHist, voiceAvail, lastSpoken, widgetSnap ->
        ExperienceIoBundle(voiceHist, voiceAvail, lastSpoken, widgetSnap)
    }

    val uiState: StateFlow<SystemExperienceUiState> = combine(
        coreBundleFlow,
        ioBundleFlow,
        transient
    ) { core, io, local ->
        SystemExperienceUiState.Ready(
            preferences = core.prefs,
            player = core.player,
            unacknowledgedCelebrations = core.unacked,
            celebrationHistory = core.history,
            announcements = core.announcements,
            voiceHistory = io.voiceHistory,
            voiceAvailability = io.voiceAvailability,
            lastSpokenMessage = io.lastSpoken,
            widgetSnapshot = io.widgetSnapshot,
            activeReplayCelebration = local.activeReplayCelebration,
            pendingVoiceConfirmation = local.pendingVoiceConfirmation,
            lastVoiceResolution = local.lastVoiceResolution,
            feedbackMessage = local.feedbackMessage
        ) as SystemExperienceUiState
    }.catch { err ->
        emit(SystemExperienceUiState.Error(err.message ?: "Unable to load System Experience layer."))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SystemExperienceUiState.Loading
    )

    init {
        viewModelScope.launch {
            experienceRepository.ensureInitialized()
            experienceRepository.syncCelebrationsFromPlayerProfile()
        }
        viewModelScope.launch {
            realityOsRepository.playerProfile.collect {
                experienceRepository.syncCelebrationsFromPlayerProfile()
            }
        }
    }

    fun updatePreferences(transform: (SystemExperiencePreferences) -> SystemExperiencePreferences) {
        viewModelScope.launch {
            experienceRepository.updatePreferences(transform)
            transient.update { it.copy(feedbackMessage = "System Experience preferences updated.") }
        }
    }

    fun completeAwakening() {
        viewModelScope.launch {
            experienceRepository.completeAwakening()
        }
    }

    fun replayAwakening() {
        viewModelScope.launch {
            experienceRepository.triggerAwakeningReplay()
        }
    }

    fun speakAwakeningStage(stage: AwakeningStage, profile: PlayerProfile) {
        viewModelScope.launch {
            experienceRepository.speakAwakeningStage(stage, profile)
        }
    }

    fun acknowledgeCelebration(celebrationId: String) {
        viewModelScope.launch {
            experienceRepository.acknowledgeCelebration(celebrationId)
            transient.update { it.copy(activeReplayCelebration = null) }
        }
    }

    fun replayCelebration(event: CelebrationEvent) {
        transient.update { it.copy(activeReplayCelebration = event) }
    }

    fun dismissReplayCelebration() {
        transient.update { it.copy(activeReplayCelebration = null) }
    }

    fun markAnnouncementRead(id: Long) {
        viewModelScope.launch {
            experienceRepository.markAnnouncementRead(id)
        }
    }

    fun markAllAnnouncementsRead() {
        viewModelScope.launch {
            experienceRepository.markAllAnnouncementsRead()
        }
    }

    fun submitVoiceTranscript(
        transcript: String,
        onNavigateDeepLink: (String) -> Unit
    ) {
        viewModelScope.launch {
            val res = experienceRepository.executeVoiceTranscript(transcript, confirmedSensitiveAction = false)
            if (res.matched && res.requiresConfirmation) {
                transient.update {
                    it.copy(
                        pendingVoiceConfirmation = res,
                        lastVoiceResolution = res
                    )
                }
            } else {
                transient.update { it.copy(lastVoiceResolution = res, pendingVoiceConfirmation = null) }
                if (res.matched && res.deepLinkRoute != null) {
                    if (res.deepLinkRoute == "awakening") {
                        experienceRepository.triggerAwakeningReplay()
                    } else {
                        onNavigateDeepLink(res.deepLinkRoute)
                    }
                }
            }
        }
    }

    fun confirmPendingVoiceCommand(onNavigateDeepLink: (String) -> Unit) {
        val pending = transient.value.pendingVoiceConfirmation ?: return
        viewModelScope.launch {
            val res = experienceRepository.executeVoiceTranscript(pending.spokenTranscript, confirmedSensitiveAction = true)
            transient.update { it.copy(pendingVoiceConfirmation = null, lastVoiceResolution = res) }
            if (pending.deepLinkRoute == "awakening") {
                experienceRepository.triggerAwakeningReplay()
            } else if (pending.deepLinkRoute != null) {
                onNavigateDeepLink(pending.deepLinkRoute)
            }
        }
    }

    fun dismissPendingVoiceConfirmation() {
        transient.update { it.copy(pendingVoiceConfirmation = null) }
    }

    fun clearVoiceHistory() {
        viewModelScope.launch {
            experienceRepository.clearVoiceHistory()
            transient.update { it.copy(feedbackMessage = "Voice command log cleared.") }
        }
    }

    fun testSystemVoice() {
        viewModelScope.launch {
            experienceRepository.testSystemVoice()
            transient.update { it.copy(feedbackMessage = "Testing System Voice output.") }
        }
    }

    fun stopVoice() {
        experienceRepository.stopVoice()
    }

    fun speakText(text: String, priority: TtsSpeechPriority = TtsSpeechPriority.NORMAL, interrupt: Boolean = true) {
        viewModelScope.launch {
            experienceRepository.speakCustomText(text, priority, interrupt)
        }
    }

    fun refreshWidgets() {
        viewModelScope.launch {
            experienceRepository.refreshWidgets()
            transient.update { it.copy(feedbackMessage = "All 4 Home-Screen Widgets synchronized with live telemetry.") }
        }
    }

    fun clearFeedback() {
        transient.update { it.copy(feedbackMessage = null) }
    }

    companion object {
        fun factory(
            experienceRepository: SystemExperienceRepository,
            realityOsRepository: RealityOsRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SystemExperienceViewModel(experienceRepository, realityOsRepository) as T
            }
        }
    }
}
