package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.VoiceCommandPermissions
import com.example.data.repository.RealityOsRepository
import com.example.data.repository.SystemExperienceRepository
import com.example.domain.engine.CommandExecutionResult
import com.example.domain.engine.CommandExecutionStatus
import com.example.domain.engine.VoiceInteractionMode
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

data class VoiceConversationTurn(
    val id: Long = System.currentTimeMillis(),
    val userTranscript: String,
    val systemReply: String
)

sealed interface SystemExperienceUiState {
    data object Loading : SystemExperienceUiState
    data class Error(val message: String) : SystemExperienceUiState
    data class Ready(
        val preferences: SystemExperiencePreferences,
        val voiceCommandPermissions: VoiceCommandPermissions,
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
        val voiceMode: VoiceInteractionMode,
        val isProcessingVoice: Boolean,
        val voiceStreamingReply: String,
        val lastCommandExecution: CommandExecutionResult?,
        val pendingCommandConfirmation: CommandExecutionResult?,
        val conversationTurns: List<VoiceConversationTurn>,
        val feedbackMessage: String?
    ) : SystemExperienceUiState
}

private data class ExperienceTransientState(
    val activeReplayCelebration: CelebrationEvent? = null,
    val pendingVoiceConfirmation: VoiceCommandResolution? = null,
    val lastVoiceResolution: VoiceCommandResolution? = null,
    val voiceMode: VoiceInteractionMode = VoiceInteractionMode.COMMAND,
    val isProcessingVoice: Boolean = false,
    val voiceStreamingReply: String = "",
    val activeVoiceConversationId: Long? = null,
    val lastCommandExecution: CommandExecutionResult? = null,
    val pendingCommandConfirmation: CommandExecutionResult? = null,
    val conversationTurns: List<VoiceConversationTurn> = emptyList(),
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
    val widgetSnapshot: WidgetSnapshot,
    val voicePermissions: VoiceCommandPermissions
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
        experienceRepository.widgetSnapshot,
        experienceRepository.observeVoiceCommandPermissions()
    ) { voiceHist, voiceAvail, lastSpoken, widgetSnap, voicePerms ->
        ExperienceIoBundle(voiceHist, voiceAvail, lastSpoken, widgetSnap, voicePerms)
    }

    val uiState: StateFlow<SystemExperienceUiState> = combine(
        coreBundleFlow,
        ioBundleFlow,
        transient
    ) { core, io, local ->
        SystemExperienceUiState.Ready(
            preferences = core.prefs,
            voiceCommandPermissions = io.voicePermissions,
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
            voiceMode = local.voiceMode,
            isProcessingVoice = local.isProcessingVoice,
            voiceStreamingReply = local.voiceStreamingReply,
            lastCommandExecution = local.lastCommandExecution,
            pendingCommandConfirmation = local.pendingCommandConfirmation,
            conversationTurns = local.conversationTurns,
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

    fun updateVoiceCommandPermissions(transform: (VoiceCommandPermissions) -> VoiceCommandPermissions) {
        viewModelScope.launch {
            experienceRepository.updateVoiceCommandPermissions(transform)
            transient.update { it.copy(feedbackMessage = "Voice command permissions updated.") }
        }
    }

    fun setVoiceMode(mode: VoiceInteractionMode) {
        transient.update {
            it.copy(
                voiceMode = mode,
                pendingCommandConfirmation = null,
                pendingVoiceConfirmation = null
            )
        }
    }

    fun endVoiceConversationSession() {
        experienceRepository.stopVoice()
        transient.update {
            it.copy(
                activeVoiceConversationId = null,
                conversationTurns = emptyList(),
                voiceStreamingReply = "",
                isProcessingVoice = false,
                feedbackMessage = "Voice conversation session ended."
            )
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
        val clean = transcript.trim()
        if (clean.isBlank()) return

        if (transient.value.voiceMode == VoiceInteractionMode.CONVERSATION) {
            submitVoiceConversationTurn(clean)
            return
        }

        viewModelScope.launch {
            transient.update {
                it.copy(
                    isProcessingVoice = true,
                    pendingCommandConfirmation = null,
                    pendingVoiceConfirmation = null
                )
            }
            val result = experienceRepository.executeGeminiVoiceCommand(clean, confirmedIntent = null)
            val awaitingConfirm = result.status == CommandExecutionStatus.AWAITING_CONFIRMATION
            transient.update {
                it.copy(
                    isProcessingVoice = false,
                    lastCommandExecution = result,
                    pendingCommandConfirmation = if (awaitingConfirm) result else null,
                    lastVoiceResolution = VoiceCommandResolution(
                        matched = result.status == CommandExecutionStatus.EXECUTED || awaitingConfirm,
                        spokenTranscript = clean,
                        normalizedCommand = result.commandId?.wireId ?: "NONE",
                        confidenceLabel = result.source.label,
                        requiresConfirmation = awaitingConfirm,
                        deepLinkRoute = result.deepLinkRoute,
                        responseText = result.spokenResponse
                    )
                )
            }
            if (result.status == CommandExecutionStatus.EXECUTED && result.deepLinkRoute != null) {
                if (result.deepLinkRoute == "awakening") {
                    experienceRepository.triggerAwakeningReplay()
                } else {
                    onNavigateDeepLink(result.deepLinkRoute)
                }
            }
        }
    }

    private fun submitVoiceConversationTurn(transcript: String) {
        viewModelScope.launch {
            transient.update { it.copy(isProcessingVoice = true, voiceStreamingReply = "") }
            val buffer = StringBuilder()
            val (convId, reply) = experienceRepository.executeVoiceConversationTurn(
                transcript = transcript,
                conversationId = transient.value.activeVoiceConversationId,
                onStreamToken = { delta ->
                    buffer.append(delta)
                    transient.update { it.copy(voiceStreamingReply = buffer.toString()) }
                }
            )
            transient.update { cur ->
                cur.copy(
                    isProcessingVoice = false,
                    voiceStreamingReply = "",
                    activeVoiceConversationId = convId ?: cur.activeVoiceConversationId,
                    conversationTurns = (cur.conversationTurns + VoiceConversationTurn(
                        userTranscript = transcript,
                        systemReply = reply
                    )).takeLast(12)
                )
            }
        }
    }

    fun confirmPendingVoiceCommand(onNavigateDeepLink: (String) -> Unit) {
        val pendingCmd = transient.value.pendingCommandConfirmation
        if (pendingCmd != null && pendingCmd.pendingIntent != null) {
            viewModelScope.launch {
                transient.update { it.copy(isProcessingVoice = true, pendingCommandConfirmation = null) }
                val res = experienceRepository.executeGeminiVoiceCommand(
                    transcript = pendingCmd.pendingIntent.rawTranscript,
                    confirmedIntent = pendingCmd.pendingIntent
                )
                transient.update {
                    it.copy(
                        isProcessingVoice = false,
                        lastCommandExecution = res,
                        lastVoiceResolution = VoiceCommandResolution(
                            matched = res.status == CommandExecutionStatus.EXECUTED,
                            spokenTranscript = pendingCmd.pendingIntent.rawTranscript,
                            normalizedCommand = res.commandId?.wireId ?: "NONE",
                            confidenceLabel = res.source.label,
                            requiresConfirmation = false,
                            deepLinkRoute = res.deepLinkRoute,
                            responseText = res.spokenResponse
                        )
                    )
                }
                if (res.status == CommandExecutionStatus.EXECUTED && res.deepLinkRoute != null) {
                    if (res.deepLinkRoute == "awakening") {
                        experienceRepository.triggerAwakeningReplay()
                    } else {
                        onNavigateDeepLink(res.deepLinkRoute)
                    }
                }
            }
            return
        }

        val pendingLegacy = transient.value.pendingVoiceConfirmation ?: return
        viewModelScope.launch {
            val res = experienceRepository.executeVoiceTranscript(pendingLegacy.spokenTranscript, confirmedSensitiveAction = true)
            transient.update { it.copy(pendingVoiceConfirmation = null, lastVoiceResolution = res) }
            if (pendingLegacy.deepLinkRoute == "awakening") {
                experienceRepository.triggerAwakeningReplay()
            } else if (pendingLegacy.deepLinkRoute != null) {
                onNavigateDeepLink(pendingLegacy.deepLinkRoute)
            }
        }
    }

    fun dismissPendingVoiceConfirmation() {
        transient.update {
            it.copy(
                pendingVoiceConfirmation = null,
                pendingCommandConfirmation = null
            )
        }
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

