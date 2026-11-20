package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.PenaltyRepository
import com.example.data.repository.RedemptionRepository
import com.example.domain.model.PenaltyRecord
import com.example.domain.model.RedemptionPlan
import com.example.domain.model.RedemptionVerificationEvidence
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class RedemptionUiState(
    val isLoading: Boolean = true,
    val accountOwnerId: String = "local_vault_player_1",
    val activePenalties: List<PenaltyRecord> = emptyList(),
    val activeRedemptions: List<RedemptionPlan> = emptyList(),
    val allRedemptions: List<RedemptionPlan> = emptyList(),
    val totalPointsResolved: Int = 0,
    val outstandingPenaltyPoints: Int = 0,
    val feedbackMessage: String? = null,
    val selectedPlanForVerification: RedemptionPlan? = null
)

class RedemptionViewModel(
    private val redemptionRepository: RedemptionRepository,
    private val penaltyRepository: PenaltyRepository,
    private val accountOwnerId: String = "local_vault_player_1"
) : ViewModel() {

    private val _uiState = MutableStateFlow(RedemptionUiState(accountOwnerId = accountOwnerId))
    val uiState: StateFlow<RedemptionUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                penaltyRepository.observeActivePenalties(accountOwnerId),
                penaltyRepository.observeAccountBalance(accountOwnerId),
                redemptionRepository.observeActiveRedemptions(accountOwnerId),
                redemptionRepository.observeAllRedemptions(accountOwnerId)
            ) { activePenalties, balance, activeRedemptions, allRedemptions ->
                val resolvedSum = allRedemptions
                    .filter { it.lifecycleState == com.example.domain.model.RedemptionLifecycleState.VERIFIED_COMPLETED }
                    .sumOf { it.resolutionPoints }

                RedemptionUiState(
                    isLoading = false,
                    accountOwnerId = accountOwnerId,
                    activePenalties = activePenalties,
                    activeRedemptions = activeRedemptions,
                    allRedemptions = allRedemptions,
                    totalPointsResolved = resolvedSum,
                    outstandingPenaltyPoints = balance.currentBalance,
                    feedbackMessage = _uiState.value.feedbackMessage,
                    selectedPlanForVerification = _uiState.value.selectedPlanForVerification
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun generatePlanForPenalty(penaltyId: String) {
        viewModelScope.launch {
            val plan = redemptionRepository.generateOrGetRedemptionPlan(penaltyId, accountOwnerId)
            if (plan != null) {
                _uiState.value = _uiState.value.copy(
                    feedbackMessage = "Corrective recovery plan generated: '${plan.activityTitle}'"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    feedbackMessage = "Unable to generate plan. Please verify active penalties."
                )
            }
        }
    }

    fun acceptPlan(redemptionId: String) {
        viewModelScope.launch {
            val success = redemptionRepository.acceptRedemption(redemptionId)
            if (success) {
                _uiState.value = _uiState.value.copy(
                    feedbackMessage = "Redemption trial accepted. Execute protocol to restore compliance."
                )
            }
        }
    }

    fun requestAlternative(redemptionId: String) {
        viewModelScope.launch {
            val newPlan = redemptionRepository.requestAlternativeActivity(redemptionId)
            _uiState.value = _uiState.value.copy(
                feedbackMessage = if (newPlan != null) {
                    "Alternative activity assigned: '${newPlan.activityTitle}'"
                } else {
                    "No alternative activities available."
                }
            )
        }
    }

    fun pausePlan(redemptionId: String, reason: String = "User requested temporary recovery pause") {
        viewModelScope.launch {
            redemptionRepository.pauseRedemption(redemptionId, reason)
            _uiState.value = _uiState.value.copy(
                feedbackMessage = "Redemption paused. Take required rest before resuming."
            )
        }
    }

    fun selectPlanForVerification(plan: RedemptionPlan?) {
        _uiState.value = _uiState.value.copy(selectedPlanForVerification = plan)
    }

    fun submitVerification(redemptionId: String, reflection: String, checklists: List<String>) {
        viewModelScope.launch {
            val evidence = RedemptionVerificationEvidence(
                redemptionId = redemptionId,
                accountOwnerId = accountOwnerId,
                reflectionText = reflection,
                checklistItemsConfirmed = checklists,
                userConfirmedCompliance = true
            )
            val result = redemptionRepository.submitAndVerifyRedemption(redemptionId, evidence)
            _uiState.value = _uiState.value.copy(
                feedbackMessage = result.message,
                selectedPlanForVerification = null
            )
        }
    }

    fun clearFeedback() {
        _uiState.value = _uiState.value.copy(feedbackMessage = null)
    }

    companion object {
        fun provideFactory(
            redemptionRepository: RedemptionRepository,
            penaltyRepository: PenaltyRepository,
            accountOwnerId: String = "local_vault_player_1"
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return RedemptionViewModel(redemptionRepository, penaltyRepository, accountOwnerId) as T
            }
        }
    }
}
