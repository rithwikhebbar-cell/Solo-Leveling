package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.ShadowArmyRepository
import com.example.domain.model.MonarchSkillCatalog
import com.example.domain.model.MonarchSkillDefinition
import com.example.domain.model.Shadow
import com.example.domain.model.ShadowArmyAnalytics
import com.example.domain.model.ShadowDeploymentCategory
import com.example.domain.model.ShadowDeploymentRecord
import com.example.domain.model.ShadowExtractionEvent
import com.example.domain.model.ShadowExtractionOutcome
import com.example.domain.model.ShadowExtractionSourceType
import com.example.domain.model.ShadowFormation
import com.example.domain.model.ShadowFormationType
import com.example.domain.model.ShadowMonarchProfile
import com.example.domain.model.ShadowQuestDefinition
import com.example.domain.model.ShadowQuestProgress
import com.example.domain.model.ShadowResourceType
import com.example.domain.model.ShadowSkillCatalog
import com.example.domain.model.ShadowSkillDefinition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

// ============================================================================
// SHADOW REALM VIEW MODEL
// ============================================================================

data class ShadowRealmUiState(
    val isLoading: Boolean = true,
    val monarchProfile: ShadowMonarchProfile? = null,
    val activeShadows: List<Shadow> = emptyList(),
    val allShadows: List<Shadow> = emptyList(),
    val activeDeployments: List<ShadowDeploymentRecord> = emptyList(),
    val formations: List<ShadowFormation> = emptyList(),
    val activeFormation: ShadowFormation? = null,
    val questProgress: List<Pair<ShadowQuestDefinition, ShadowQuestProgress?>> = emptyList(),
    val unlockedMonarchSkills: List<MonarchSkillDefinition> = emptyList(),
    val shadowEssenceBalance: Int = 0,
    val monarchFragmentsBalance: Int = 0,
    val evolutionCoresBalance: Int = 0,
    val shadowCrystalsBalance: Int = 0,
    val error: String? = null
)

data class ShadowExtractionUiState(
    val isEvaluating: Boolean = false,
    val pendingExtraction: ShadowExtractionEvent? = null,
    val isConfirming: Boolean = false,
    val lastResult: ShadowExtractionEvent? = null,
    val error: String? = null
)

class ShadowRealmViewModel(
    val repository: ShadowArmyRepository
) : ViewModel() {

    private val monarchProgressionEngine = repository.monarchProgressionEngine
    private val extractionEngine = repository.extractionEngine
    private val evolutionEngine = repository.evolutionEngine
    private val deploymentEngine = repository.deploymentEngine

    private val _uiState = MutableStateFlow(ShadowRealmUiState())
    val uiState: StateFlow<ShadowRealmUiState> = _uiState.asStateFlow()

    private val _extractionUiState = MutableStateFlow(ShadowExtractionUiState())
    val extractionUiState: StateFlow<ShadowExtractionUiState> = _extractionUiState.asStateFlow()

    private val _selectedShadow = MutableStateFlow<Shadow?>(null)
    val selectedShadow: StateFlow<Shadow?> = _selectedShadow.asStateFlow()

    private val _analytics = MutableStateFlow<ShadowArmyAnalytics?>(null)
    val analytics: StateFlow<ShadowArmyAnalytics?> = _analytics.asStateFlow()

    init {
        observeData()
        ensureMonarchProfile()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                repository.observeMonarchProfile(),
                repository.observeActiveShadows(),
                repository.observeAllShadows(),
                repository.observeActiveDeployments(),
                repository.observeFormations()
            ) { profile, active, all, deployments, formations ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    monarchProfile = profile,
                    activeShadows = active,
                    allShadows = all,
                    activeDeployments = deployments,
                    formations = formations
                )
            }.collect {}
        }
        viewModelScope.launch {
            repository.observeActiveFormation().collect { activeFormation ->
                _uiState.value = _uiState.value.copy(activeFormation = activeFormation)
            }
        }
        viewModelScope.launch {
            repository.observeQuestProgress().collect { quests ->
                _uiState.value = _uiState.value.copy(questProgress = quests)
            }
        }
        viewModelScope.launch {
            repository.observeMonarchSkillUnlocks().collect { skills ->
                _uiState.value = _uiState.value.copy(unlockedMonarchSkills = skills)
            }
        }
    }

    private fun ensureMonarchProfile() {
        viewModelScope.launch {
            monarchProgressionEngine.getOrCreateProfile()
            refreshResourceBalances()
        }
    }

    suspend fun refreshResourceBalances() {
        val essence = repository.getCurrentBalance(ShadowResourceType.SHADOW_ESSENCE)
        val fragments = repository.getCurrentBalance(ShadowResourceType.MONARCH_FRAGMENTS)
        val cores = repository.getCurrentBalance(ShadowResourceType.EVOLUTION_CORES)
        val crystals = repository.getCurrentBalance(ShadowResourceType.SHADOW_CRYSTALS)
        _uiState.value = _uiState.value.copy(
            shadowEssenceBalance = essence,
            monarchFragmentsBalance = fragments,
            evolutionCoresBalance = cores,
            shadowCrystalsBalance = crystals
        )
    }

    // -------------------------------------------------------------------------
    // SHADOW EXTRACTION
    // -------------------------------------------------------------------------

    fun evaluateExtraction(
        sourceType: ShadowExtractionSourceType,
        sourceEventId: String,
        evidenceSummary: String
    ) {
        viewModelScope.launch {
            _extractionUiState.value = ShadowExtractionUiState(isEvaluating = true)
            val result = extractionEngine.evaluateExtractionEligibility(sourceType, sourceEventId, evidenceSummary)
            _extractionUiState.value = ShadowExtractionUiState(
                isEvaluating = false,
                pendingExtraction = result,
                lastResult = if (result.outcome != ShadowExtractionOutcome.PENDING_CONFIRMATION) result else null
            )
        }
    }

    fun confirmExtraction() {
        val pending = _extractionUiState.value.pendingExtraction ?: return
        viewModelScope.launch {
            _extractionUiState.value = _extractionUiState.value.copy(isConfirming = true)
            val result = extractionEngine.confirmExtraction(pending)
            result.onSuccess { event ->
                _extractionUiState.value = ShadowExtractionUiState(
                    lastResult = event
                )
                refreshResourceBalances()
                if (event.outcome == ShadowExtractionOutcome.SUCCESS) {
                    repository.awardResource(
                        resourceType = ShadowResourceType.SHADOW_ESSENCE,
                        amount = 25,
                        sourceEventId = "essence_extraction_${event.extractionId}",
                        sourceDescription = "Shadow Extracted: ${event.shadowName}"
                    )
                    refreshResourceBalances()
                }
            }
            result.onFailure { e ->
                _extractionUiState.value = _extractionUiState.value.copy(
                    isConfirming = false,
                    error = e.message
                )
            }
        }
    }

    fun cancelExtraction() {
        _extractionUiState.value = ShadowExtractionUiState()
    }

    // -------------------------------------------------------------------------
    // SHADOW SELECTION
    // -------------------------------------------------------------------------

    fun selectShadow(shadowId: String) {
        viewModelScope.launch {
            _selectedShadow.value = repository.getShadowById(shadowId)
        }
    }

    fun clearSelectedShadow() {
        _selectedShadow.value = null
    }

    // -------------------------------------------------------------------------
    // SHADOW EVOLUTION & PROGRESSION
    // -------------------------------------------------------------------------

    fun awardShadowXp(shadowId: String, xpAmount: Int, sourceTag: String) {
        viewModelScope.launch {
            evolutionEngine.awardShadowXp(shadowId, xpAmount, sourceTag)
        }
    }

    // -------------------------------------------------------------------------
    // DEPLOYMENT
    // -------------------------------------------------------------------------

    fun deployShadow(shadowId: String, category: ShadowDeploymentCategory, role: String) {
        viewModelScope.launch {
            deploymentEngine.deployShadow(shadowId, category, role).onSuccess {
                // Success — flows update automatically
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun recallShadow(deploymentId: String) {
        viewModelScope.launch {
            deploymentEngine.recallShadow(deploymentId)
        }
    }

    // -------------------------------------------------------------------------
    // FORMATIONS
    // -------------------------------------------------------------------------

    fun createFormation(name: String, type: ShadowFormationType, description: String, memberIds: List<String>) {
        viewModelScope.launch {
            deploymentEngine.createFormation(name, type, description, memberIds)
        }
    }

    fun setActiveFormation(formationId: String) {
        viewModelScope.launch {
            deploymentEngine.setActiveFormation(formationId)
        }
    }

    fun deleteFormation(formationId: String) {
        viewModelScope.launch {
            deploymentEngine.deleteFormation(formationId)
        }
    }

    // -------------------------------------------------------------------------
    // ANALYTICS
    // -------------------------------------------------------------------------

    fun loadAnalytics() {
        viewModelScope.launch {
            _analytics.value = repository.buildShadowArmyAnalytics()
        }
    }

    // -------------------------------------------------------------------------
    // SKILL CATALOG ACCESS
    // -------------------------------------------------------------------------

    fun getShadowSkillCatalog(): List<ShadowSkillDefinition> = ShadowSkillCatalog.skills
    fun getMonarchSkillCatalog(): List<MonarchSkillDefinition> = MonarchSkillCatalog.skills

    fun unlockShadowSkill(shadowId: String, skillId: String) {
        viewModelScope.launch {
            evolutionEngine.unlockShadowSkill(shadowId, skillId, "Player-initiated skill unlock")
        }
    }

    fun unlockMonarchSkill(skillId: String) {
        viewModelScope.launch {
            monarchProgressionEngine.tryUnlockMonarchSkill(skillId)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    companion object {
        fun factory(repository: ShadowArmyRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ShadowRealmViewModel(repository) as T
                }
            }
    }
}

