package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AiSystemRepository
import com.example.data.repository.RealityOsRepository
import com.example.domain.model.AiChatMessage
import com.example.domain.model.AiConnectionStatus
import com.example.domain.model.AiConsentState
import com.example.domain.model.AiContextScope
import com.example.domain.model.AiConversation
import com.example.domain.model.AiDailyPlanItem
import com.example.domain.model.AiModelPreferenceState
import com.example.domain.model.AiQuestProposal
import com.example.domain.model.AiRequestAuditRecord
import com.example.domain.model.DevelopmentAnalysisReport
import com.example.domain.model.PlanMode
import com.example.domain.model.PlayerContextSnapshot
import com.example.domain.model.PlayerProfile
import com.example.domain.model.SystemNotificationPreferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface AiSystemUiState {
    data object Loading : AiSystemUiState
    data class Error(val message: String) : AiSystemUiState
    data class Ready(
        val player: PlayerProfile,
        val modelPreferences: AiModelPreferenceState,
        val consentState: AiConsentState,
        val notificationPreferences: SystemNotificationPreferences,
        val conversations: List<AiConversation>,
        val activeConversationId: Long?,
        val messages: List<AiChatMessage>,
        val streamingPreview: String,
        val isSendingChat: Boolean,
        val isGeneratingProposals: Boolean,
        val isGeneratingPlan: Boolean,
        val connectionStatus: AiConnectionStatus,
        val lastErrorDetail: String?,
        val lastUserPromptForRetry: String?,
        val contextSnapshot: PlayerContextSnapshot?,
        val analysisReport: DevelopmentAnalysisReport?,
        val questProposals: List<AiQuestProposal>,
        val selectedPlanMode: PlanMode,
        val dailyPlanItems: List<AiDailyPlanItem>,
        val recentAudits: List<AiRequestAuditRecord>,
        val feedbackMessage: String?
    ) : AiSystemUiState
}

private data class AiTransientUiState(
    val activeConversationId: Long? = null,
    val streamingPreview: String = "",
    val isSendingChat: Boolean = false,
    val isGeneratingProposals: Boolean = false,
    val isGeneratingPlan: Boolean = false,
    val connectionStatus: AiConnectionStatus = AiConnectionStatus.LOCAL_FALLBACK,
    val lastErrorDetail: String? = null,
    val lastUserPromptForRetry: String? = null,
    val selectedPlanMode: PlanMode = PlanMode.DAILY,
    val contextSnapshot: PlayerContextSnapshot? = null,
    val analysisReport: DevelopmentAnalysisReport? = null,
    val feedbackMessage: String? = null
)

private data class AiConfigBundle(
    val player: PlayerProfile,
    val modelPrefs: AiModelPreferenceState,
    val consent: AiConsentState,
    val notifPrefs: SystemNotificationPreferences
)

private data class AiContentBundle(
    val conversations: List<AiConversation>,
    val messages: List<AiChatMessage>,
    val questProposals: List<AiQuestProposal>,
    val dailyPlanItems: List<AiDailyPlanItem>,
    val audits: List<AiRequestAuditRecord>
)

@OptIn(ExperimentalCoroutinesApi::class)
class AiSystemViewModel(
    private val aiSystemRepository: AiSystemRepository,
    private val realityOsRepository: RealityOsRepository
) : ViewModel() {

    private val selectedConversationIdFlow = MutableStateFlow<Long?>(null)
    private val transientState = MutableStateFlow(AiTransientUiState())

    private val activeMessagesFlow = combine(
        aiSystemRepository.conversations,
        selectedConversationIdFlow
    ) { conversations, selectedId ->
        selectedId?.takeIf { id -> conversations.any { it.id == id } } ?: conversations.firstOrNull()?.id
    }.flatMapLatest { resolvedId ->
        if (resolvedId == null) flowOf(emptyList()) else aiSystemRepository.observeMessages(resolvedId)
    }

    private val configBundleFlow = combine(
        realityOsRepository.playerProfile,
        aiSystemRepository.modelPreferences,
        aiSystemRepository.consentState,
        aiSystemRepository.notificationPreferences
    ) { player, modelPrefs, consent, notifPrefs ->
        AiConfigBundle(player, modelPrefs, consent, notifPrefs)
    }

    private val contentBundleFlow = combine(
        aiSystemRepository.conversations,
        activeMessagesFlow,
        aiSystemRepository.questProposals,
        aiSystemRepository.dailyPlanProposals,
        aiSystemRepository.recentAudits
    ) { convs, msgs, quests, plans, audits ->
        AiContentBundle(convs, msgs, quests, plans, audits)
    }

    val uiState: StateFlow<AiSystemUiState> = combine(
        configBundleFlow,
        contentBundleFlow,
        transientState,
        selectedConversationIdFlow
    ) { config, content, transient, explicitConvId ->
        val resolvedConvId = explicitConvId?.takeIf { id -> content.conversations.any { it.id == id } }
            ?: content.conversations.firstOrNull()?.id
        val effectiveStatus = when {
            transient.isSendingChat && config.modelPrefs.streamingEnabled -> AiConnectionStatus.STREAMING
            transient.isSendingChat -> AiConnectionStatus.CONNECTING
            transient.connectionStatus != AiConnectionStatus.LOCAL_FALLBACK -> transient.connectionStatus
            !config.consent.cloudAiConsentGranted || !config.modelPrefs.aiEnabled -> AiConnectionStatus.LOCAL_FALLBACK
            config.modelPrefs.backendBaseUrl.isBlank() -> AiConnectionStatus.BACKEND_UNCONFIGURED
            else -> AiConnectionStatus.CONNECTED
        }
        AiSystemUiState.Ready(
            player = config.player,
            modelPreferences = config.modelPrefs,
            consentState = config.consent,
            notificationPreferences = config.notifPrefs,
            conversations = content.conversations,
            activeConversationId = resolvedConvId,
            messages = content.messages,
            streamingPreview = transient.streamingPreview,
            isSendingChat = transient.isSendingChat,
            isGeneratingProposals = transient.isGeneratingProposals,
            isGeneratingPlan = transient.isGeneratingPlan,
            connectionStatus = effectiveStatus,
            lastErrorDetail = transient.lastErrorDetail,
            lastUserPromptForRetry = transient.lastUserPromptForRetry,
            contextSnapshot = transient.contextSnapshot,
            analysisReport = transient.analysisReport,
            questProposals = content.questProposals,
            selectedPlanMode = transient.selectedPlanMode,
            dailyPlanItems = content.dailyPlanItems.filter { it.planMode == transient.selectedPlanMode },
            recentAudits = content.audits,
            feedbackMessage = transient.feedbackMessage
        ) as AiSystemUiState
    }.catch { error ->
        emit(AiSystemUiState.Error(error.message ?: "Failed to load AI System Administrator."))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AiSystemUiState.Loading
    )

    init {
        refreshAnalysis()
    }

    fun refreshAnalysis(scope: AiContextScope = AiContextScope.FULL) {
        viewModelScope.launch {
            runCatching {
                aiSystemRepository.analyzeDevelopment(scope)
            }.onSuccess { (snapshot, report) ->
                transientState.update {
                    it.copy(contextSnapshot = snapshot, analysisReport = report)
                }
            }
        }
    }

    fun selectConversation(conversationId: Long) {
        selectedConversationIdFlow.value = conversationId
        transientState.update { it.copy(activeConversationId = conversationId) }
    }

    fun startNewConversation() {
        viewModelScope.launch {
            val id = aiSystemRepository.createConversation(title = "New Tactical Briefing")
            selectedConversationIdFlow.value = id
            transientState.update { it.copy(activeConversationId = id, streamingPreview = "", lastErrorDetail = null) }
        }
    }

    fun deleteConversation(conversationId: Long) {
        viewModelScope.launch {
            aiSystemRepository.deleteConversation(conversationId)
            if (selectedConversationIdFlow.value == conversationId) {
                selectedConversationIdFlow.value = null
            }
        }
    }

    fun clearAllConversations() {
        viewModelScope.launch {
            aiSystemRepository.clearAllConversations()
            selectedConversationIdFlow.value = null
            transientState.update { it.copy(feedbackMessage = "AI conversation history cleared.") }
        }
    }

    fun sendChatMessage(prompt: String) {
        val clean = prompt.trim()
        if (clean.isBlank()) return
        viewModelScope.launch {
            transientState.update {
                it.copy(
                    isSendingChat = true,
                    streamingPreview = "",
                    lastErrorDetail = null,
                    lastUserPromptForRetry = clean
                )
            }
            val buffer = StringBuilder()
            val convId = selectedConversationIdFlow.value
            val result = aiSystemRepository.sendChatMessage(
                conversationId = convId,
                userPrompt = clean,
                onStreamToken = { delta ->
                    buffer.append(delta)
                    transientState.update { it.copy(streamingPreview = buffer.toString()) }
                }
            )
            selectedConversationIdFlow.value = result.conversationId
            transientState.update {
                it.copy(
                    activeConversationId = result.conversationId,
                    isSendingChat = false,
                    streamingPreview = "",
                    connectionStatus = result.connectionStatus,
                    lastErrorDetail = result.errorDetail,
                    lastUserPromptForRetry = if (result.errorDetail != null) clean else null
                )
            }
            refreshAnalysis()
        }
    }

    fun retryLastPrompt() {
        val last = transientState.value.lastUserPromptForRetry ?: return
        sendChatMessage(last)
    }

    fun generateQuestProposals() {
        viewModelScope.launch {
            transientState.update { it.copy(isGeneratingProposals = true, feedbackMessage = null) }
            val result = aiSystemRepository.generateQuestProposals()
            transientState.update {
                it.copy(
                    isGeneratingProposals = false,
                    connectionStatus = result.connectionStatus,
                    feedbackMessage = result.note
                )
            }
            refreshAnalysis()
        }
    }

    fun approveQuestProposal(
        proposalId: Long,
        editedTitle: String? = null,
        editedDescription: String? = null,
        editedDurationMinutes: Int? = null
    ) {
        viewModelScope.launch {
            val res = aiSystemRepository.approveQuestProposal(
                proposalId = proposalId,
                editedTitle = editedTitle,
                editedDescription = editedDescription,
                editedDurationMinutes = editedDurationMinutes
            )
            transientState.update {
                it.copy(feedbackMessage = res.getOrElse { err -> err.message ?: "Could not approve quest." })
            }
        }
    }

    fun rejectQuestProposal(proposalId: Long) {
        viewModelScope.launch {
            aiSystemRepository.rejectQuestProposal(proposalId)
            transientState.update { it.copy(feedbackMessage = "Quest proposal dismissed.") }
        }
    }

    fun selectPlanMode(mode: PlanMode) {
        transientState.update { it.copy(selectedPlanMode = mode) }
    }

    fun generatePlanForSelectedMode() {
        val mode = transientState.value.selectedPlanMode
        viewModelScope.launch {
            transientState.update { it.copy(isGeneratingPlan = true, feedbackMessage = null) }
            val result = aiSystemRepository.generatePlanProposals(mode)
            transientState.update {
                it.copy(
                    isGeneratingPlan = false,
                    connectionStatus = result.connectionStatus,
                    feedbackMessage = result.note
                )
            }
        }
    }

    fun approvePlanItem(
        itemId: Long,
        editedTitle: String? = null,
        editedStartMinute: Int? = null,
        editedDurationMinutes: Int? = null
    ) {
        viewModelScope.launch {
            aiSystemRepository.approvePlanItem(itemId, editedTitle, editedStartMinute, editedDurationMinutes)
            transientState.update { it.copy(feedbackMessage = "Schedule block approved.") }
        }
    }

    fun rejectPlanItem(itemId: Long) {
        viewModelScope.launch {
            aiSystemRepository.rejectPlanItem(itemId)
            transientState.update { it.copy(feedbackMessage = "Schedule block rejected.") }
        }
    }

    fun selectModel(modelId: String) {
        val clean = modelId.trim()
        if (clean.isBlank()) return
        viewModelScope.launch {
            aiSystemRepository.updateModelPreference { it.copy(selectedModelId = clean) }
            transientState.update { it.copy(feedbackMessage = "Selected model: $clean") }
        }
    }

    fun updateBackendBaseUrl(url: String) {
        viewModelScope.launch {
            aiSystemRepository.updateModelPreference { it.copy(backendBaseUrl = url.trim()) }
            transientState.update { it.copy(feedbackMessage = "Backend AI Proxy URL saved.") }
        }
    }

    fun setAiEnabled(enabled: Boolean) {
        viewModelScope.launch {
            aiSystemRepository.updateModelPreference { it.copy(aiEnabled = enabled) }
        }
    }

    fun setStreamingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            aiSystemRepository.updateModelPreference { it.copy(streamingEnabled = enabled) }
        }
    }

    fun updateConsent(
        cloudConsent: Boolean? = null,
        sharePhysical: Boolean? = null,
        shareAcademic: Boolean? = null,
        shareQuests: Boolean? = null
    ) {
        viewModelScope.launch {
            aiSystemRepository.updateConsent { cur ->
                cur.copy(
                    cloudAiConsentGranted = cloudConsent ?: cur.cloudAiConsentGranted,
                    sharePhysicalWithAi = sharePhysical ?: cur.sharePhysicalWithAi,
                    shareAcademicWithAi = shareAcademic ?: cur.shareAcademicWithAi,
                    shareQuestsWithAi = shareQuests ?: cur.shareQuestsWithAi
                )
            }
            refreshAnalysis()
        }
    }

    fun updateNotificationPrefs(transform: (SystemNotificationPreferences) -> SystemNotificationPreferences) {
        viewModelScope.launch {
            aiSystemRepository.updateNotificationPreferences(transform)
        }
    }

    fun triggerNotificationEvaluation(context: Context) {
        viewModelScope.launch {
            val count = aiSystemRepository.evaluateAndTriggerNotifications(context)
            transientState.update {
                it.copy(
                    feedbackMessage = if (count > 0) {
                        "Dispatched $count system alert(s) to notification feed."
                    } else {
                        "No new notifications needed (quiet hours, quota, or already sent)."
                    }
                )
            }
        }
    }

    fun clearFeedback() {
        transientState.update { it.copy(feedbackMessage = null, lastErrorDetail = null) }
    }

    companion object {
        fun factory(
            aiSystemRepository: AiSystemRepository,
            realityOsRepository: RealityOsRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AiSystemViewModel(aiSystemRepository, realityOsRepository) as T
            }
        }
    }
}
