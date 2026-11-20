package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.integration.BackendAiGateway
import com.example.data.integration.BackendAiProxyRequest
import com.example.data.local.SoloLevelingDatabase
import com.example.data.repository.PenaltyRepository
import com.example.data.repository.QuestCommitmentRepository
import com.example.data.repository.RedemptionRepository
import com.example.domain.engine.FailurePatternAnalysisEngine
import com.example.domain.engine.PenaltyAnalyticsEngine
import com.example.domain.model.CommitmentState
import com.example.domain.model.ConsistencyMetrics
import com.example.domain.model.DetectedFailurePattern
import com.example.domain.model.ExemptionLifecycleState
import com.example.domain.model.ExemptionReasonCategory
import com.example.domain.model.FailureTrendAnalysis
import com.example.domain.model.PenaltyAuditTimelineItem
import com.example.domain.model.PenaltyOverviewSummary
import com.example.domain.model.PenaltyRecord
import com.example.domain.model.PenaltyTransaction
import com.example.domain.model.QuestCommitment
import com.example.domain.model.QuestExemption
import com.example.domain.model.RecoveryState
import com.example.domain.model.RedemptionPerformanceMetrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZoneId

data class PenaltyAuditUiState(
    val isLoading: Boolean = true,
    val accountOwnerId: String = "local_vault_player_1",
    val overview: PenaltyOverviewSummary = PenaltyOverviewSummary(accountOwnerId = "local_vault_player_1"),
    val failureTrends: FailureTrendAnalysis = FailureTrendAnalysis(),
    val redemptionMetrics: RedemptionPerformanceMetrics = RedemptionPerformanceMetrics(),
    val consistencyMetrics: ConsistencyMetrics = ConsistencyMetrics(),
    val detectedPatterns: List<DetectedFailurePattern> = emptyList(),
    val timelineItems: List<PenaltyAuditTimelineItem> = emptyList(),
    val transactions: List<PenaltyTransaction> = emptyList(),
    val exemptions: List<QuestExemption> = emptyList(),
    val eligibleCommitmentsForExemption: List<QuestCommitment> = emptyList(),
    val questTitleMap: Map<Long, String> = emptyMap(),
    val selectedTab: Int = 0,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val aiAdvisoryExplanation: String? = null,
    val isGeneratingAdvisory: Boolean = false,
    val isReconciling: Boolean = false
)

class PenaltyAuditViewModel(
    private val penaltyRepository: PenaltyRepository,
    private val questCommitmentRepository: QuestCommitmentRepository,
    private val redemptionRepository: RedemptionRepository,
    private val database: SoloLevelingDatabase,
    private val backendAiGateway: BackendAiGateway? = null,
    private val accountOwnerId: String = "local_vault_player_1"
) : ViewModel() {

    private val _uiState = MutableStateFlow(PenaltyAuditUiState(accountOwnerId = accountOwnerId))
    val uiState: StateFlow<PenaltyAuditUiState> = _uiState.asStateFlow()

    private var questCategoryCache: Map<Long, String> = emptyMap()
    private var questTitleCache: Map<Long, String> = emptyMap()

    init {
        loadQuestMetadata()
        observeData()
    }

    private fun loadQuestMetadata() {
        viewModelScope.launch(Dispatchers.IO) {
            val quests = database.questDao().getAllQuests()
            questCategoryCache = quests.associate { it.id to it.categoryName }
            questTitleCache = quests.associate { it.id to it.title }
        }
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                penaltyRepository.observeAccountBalance(accountOwnerId),
                penaltyRepository.observeAllPenalties(accountOwnerId),
                penaltyRepository.observeRecentTransactions(accountOwnerId, 200),
                questCommitmentRepository.observeAllCommitments(accountOwnerId),
                questCommitmentRepository.observeAllFailures(),
                questCommitmentRepository.observeAllExemptions(accountOwnerId),
                redemptionRepository.observeAllRedemptions(accountOwnerId)
            ) { balance, penalties, transactions, commitments, failures, exemptions, redemptions ->
                withContext(Dispatchers.Default) {
                    val recoveryAssessment = database.recoveryAssessmentDao().getLatestOnce()
                    val recoveryState = recoveryAssessment?.let {
                        runCatching { RecoveryState.valueOf(it.recoveryStateName) }.getOrNull()
                    }

                    val overview = PenaltyAnalyticsEngine.calculateOverviewSummary(
                        accountOwnerId = accountOwnerId,
                        currentBalance = balance.currentBalance,
                        lifetimeAccrued = balance.lifetimePointsAccrued,
                        transactions = transactions,
                        penalties = penalties
                    )

                    val trends = PenaltyAnalyticsEngine.calculateFailureTrends(
                        commitments = commitments,
                        failures = failures,
                        questCategoryMap = questCategoryCache,
                        zoneId = ZoneId.systemDefault()
                    )

                    val redemptionMetrics = PenaltyAnalyticsEngine.calculateRedemptionMetrics(
                        activities = redemptions
                    )

                    val consistency = PenaltyAnalyticsEngine.calculateConsistencyMetrics(
                        commitments = commitments,
                        failures = failures,
                        nowEpochMs = System.currentTimeMillis(),
                        zoneId = ZoneId.systemDefault()
                    )

                    val patterns = FailurePatternAnalysisEngine.analyzePatterns(
                        commitments = commitments,
                        failures = failures,
                        questCategoryMap = questCategoryCache,
                        recentRecoveryState = recoveryState,
                        zoneId = ZoneId.systemDefault(),
                        nowEpochMs = System.currentTimeMillis()
                    )

                    val timeline = PenaltyAnalyticsEngine.buildAuditTimeline(
                        commitments = commitments,
                        failures = failures,
                        penalties = penalties,
                        transactions = transactions,
                        redemptions = redemptions,
                        exemptions = exemptions
                    )

                    val eligibleCommitments = commitments.filter {
                        it.state in setOf(
                            CommitmentState.ACCEPTED,
                            CommitmentState.ACTIVE,
                            CommitmentState.OVERDUE,
                            CommitmentState.FAILED
                        )
                    }

                    _uiState.value.copy(
                        isLoading = false,
                        overview = overview,
                        failureTrends = trends,
                        redemptionMetrics = redemptionMetrics,
                        consistencyMetrics = consistency,
                        detectedPatterns = patterns,
                        timelineItems = timeline,
                        transactions = transactions,
                        exemptions = exemptions,
                        eligibleCommitmentsForExemption = eligibleCommitments,
                        questTitleMap = questTitleCache
                    )
                }
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun submitExemption(
        commitmentId: String,
        category: ExemptionReasonCategory,
        explanation: String
    ) {
        if (explanation.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Exemption explanation cannot be empty."
            )
            return
        }

        viewModelScope.launch {
            val result = questCommitmentRepository.requestExemption(
                commitmentId = commitmentId,
                category = category,
                explanation = explanation,
                accountOwnerId = accountOwnerId
            )
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    statusMessage = "Exemption request submitted for review under ${category.label}.",
                    errorMessage = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to submit exemption request."
                )
            }
        }
    }

    fun withdrawExemption(exemptionId: String) {
        viewModelScope.launch {
            val result = questCommitmentRepository.withdrawExemption(exemptionId)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    statusMessage = "Exemption request withdrawn.",
                    errorMessage = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to withdraw exemption."
                )
            }
        }
    }

    fun reviewExemption(
        exemptionId: String,
        isApproved: Boolean,
        reviewerNotes: String? = null
    ) {
        viewModelScope.launch {
            val result = questCommitmentRepository.reviewExemption(
                exemptionId = exemptionId,
                isApproved = isApproved,
                reviewerNotes = reviewerNotes
            )
            if (result.isSuccess) {
                val action = if (isApproved) "approved with any necessary ledger reversals applied" else "denied"
                _uiState.value = _uiState.value.copy(
                    statusMessage = "Exemption request $action.",
                    errorMessage = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to review exemption."
                )
            }
        }
    }

    fun reconcileLedger() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isReconciling = true)
            try {
                val reconciledBalance = penaltyRepository.reconcileAccountBalance(accountOwnerId)
                _uiState.value = _uiState.value.copy(
                    isReconciling = false,
                    statusMessage = "Ledger mathematically reconciled. Authoritative balance: $reconciledBalance PP",
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isReconciling = false,
                    errorMessage = "Ledger reconciliation failed: ${e.message}"
                )
            }
        }
    }

    fun requestAiAdvisoryExplanation() {
        if (backendAiGateway == null) {
            _uiState.value = _uiState.value.copy(
                aiAdvisoryExplanation = "AI Strategic Gateway is offline. Deterministic pattern insights and recommendations remain fully functional."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGeneratingAdvisory = true)
            try {
                val state = _uiState.value
                val contextSummary = buildString {
                    appendLine("Active Balance: ${state.overview.currentBalance} PP")
                    appendLine("Total Commitments: ${state.failureTrends.totalCommitments}")
                    appendLine("Failures: ${state.failureTrends.failedCommitments}")
                    appendLine("Patterns: ${state.detectedPatterns.joinToString { it.title }}")
                }

                val request = BackendAiProxyRequest(
                    model = "default",
                    requestType = "ADVISORY",
                    systemPrompt = "You are the System Administrator for Solo Leveling. Explain the player's failure friction patterns constructively. You have 0 authority over points, decisions, or progression. Offer non-punitive strategic advice.",
                    contextSummary = contextSummary,
                    userPrompt = "Provide a high-level strategic overview of my commitment habits and recovery advice.",
                    stream = false
                )

                val response = backendAiGateway.requestStructured("http://localhost", request)
                _uiState.value = _uiState.value.copy(
                    isGeneratingAdvisory = false,
                    aiAdvisoryExplanation = response.fullText.ifBlank { "Advisory explanation generated successfully." }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isGeneratingAdvisory = false,
                    aiAdvisoryExplanation = "Deterministic recommendation: Focus on foundational consistency, cap daily commitments to 3 core quests, and prioritize physiological rest."
                )
            }
        }
    }

    fun setSelectedTab(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = index)
    }

    fun clearStatusMessage() {
        _uiState.value = _uiState.value.copy(statusMessage = null, errorMessage = null)
    }

    companion object {
        fun provideFactory(
            penaltyRepository: PenaltyRepository,
            questCommitmentRepository: QuestCommitmentRepository,
            redemptionRepository: RedemptionRepository,
            database: SoloLevelingDatabase,
            backendAiGateway: BackendAiGateway? = null,
            accountOwnerId: String = "local_vault_player_1"
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PenaltyAuditViewModel(
                    penaltyRepository = penaltyRepository,
                    questCommitmentRepository = questCommitmentRepository,
                    redemptionRepository = redemptionRepository,
                    database = database,
                    backendAiGateway = backendAiGateway,
                    accountOwnerId = accountOwnerId
                ) as T
            }
        }
    }
}
