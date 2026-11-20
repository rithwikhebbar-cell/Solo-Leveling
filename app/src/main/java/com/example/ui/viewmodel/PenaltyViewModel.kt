package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.PenaltyRepository
import com.example.domain.model.PenaltyPointBalance
import com.example.domain.model.PenaltyRecord
import com.example.domain.model.PenaltySeverity
import com.example.domain.model.PenaltyTransaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class PenaltyUiState(
    val isLoading: Boolean = true,
    val accountOwnerId: String = "local_vault_player_1",
    val accountBalance: PenaltyPointBalance = PenaltyPointBalance("local_vault_player_1"),
    val activePenalties: List<PenaltyRecord> = emptyList(),
    val allPenalties: List<PenaltyRecord> = emptyList(),
    val recentTransactions: List<PenaltyTransaction> = emptyList(),
    val severityDistribution: Map<PenaltySeverity, Int> = emptyMap(),
    val statusMessage: String? = null
)

class PenaltyViewModel(
    private val repository: PenaltyRepository,
    private val accountOwnerId: String = "local_vault_player_1"
) : ViewModel() {

    private val _uiState = MutableStateFlow(PenaltyUiState(accountOwnerId = accountOwnerId))
    val uiState: StateFlow<PenaltyUiState> = _uiState.asStateFlow()

    init {
        observeData()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                repository.observeAccountBalance(accountOwnerId),
                repository.observeActivePenalties(accountOwnerId),
                repository.observeAllPenalties(accountOwnerId),
                repository.observeRecentTransactions(accountOwnerId)
            ) { balance, active, all, transactions ->
                val distribution = mutableMapOf<PenaltySeverity, Int>()
                PenaltySeverity.entries.forEach { distribution[it] = 0 }
                active.forEach { p ->
                    distribution[p.severity] = (distribution[p.severity] ?: 0) + 1
                }

                PenaltyUiState(
                    isLoading = false,
                    accountOwnerId = accountOwnerId,
                    accountBalance = balance,
                    activePenalties = active,
                    allPenalties = all,
                    recentTransactions = transactions,
                    severityDistribution = distribution
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun reconcile() {
        viewModelScope.launch {
            val reconciled = repository.reconcileAccountBalance(accountOwnerId)
            _uiState.value = _uiState.value.copy(
                statusMessage = "Ledger reconciled. Current balance: $reconciled PP"
            )
        }
    }

    fun clearStatusMessage() {
        _uiState.value = _uiState.value.copy(statusMessage = null)
    }

    companion object {
        fun provideFactory(
            repository: PenaltyRepository,
            accountOwnerId: String = "local_vault_player_1"
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PenaltyViewModel(repository, accountOwnerId) as T
            }
        }
    }
}
