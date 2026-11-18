package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.ShadowRealmSettings
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.ShadowArmyRepository
import com.example.domain.model.DEFAULT_SHADOW_ACCOUNT_OWNER_ID
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
import com.example.domain.model.ShadowExtractionEvidenceItem
import com.example.domain.engine.ShadowExtractionEvidenceProvider
import com.example.domain.model.ShadowSkillCatalog
import com.example.domain.model.ShadowSkillDefinition
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

// ============================================================================
// SHADOW REALM VIEW MODEL (Account-Isolated & Persistent Settings)
// ============================================================================

data class ShadowRealmUiState(
    val isLoading: Boolean = true,
    val activeAccountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
    val monarchProfile: ShadowMonarchProfile? = null,
    val activeShadows: List<Shadow> = emptyList(),
    val allShadows: List<Shadow> = emptyList(),
    val activeDeployments: List<ShadowDeploymentRecord> = emptyList(),
    val formations: List<ShadowFormation> = emptyList(),
    val activeFormation: ShadowFormation? = null,
    val questProgress: List<Pair<ShadowQuestDefinition, ShadowQuestProgress?>> = emptyList(),
    val unlockedMonarchSkills: List<MonarchSkillDefinition> = emptyList(),
    val extractionHistory: List<ShadowExtractionEvent> = emptyList(),
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
    val repository: ShadowArmyRepository,
    private val accountOwnerIdFlow: Flow<String> = flowOf(DEFAULT_SHADOW_ACCOUNT_OWNER_ID),
    private val preferencesRepository: UserPreferencesRepository? = null
) : ViewModel() {

    private val monarchProgressionEngine = repository.monarchProgressionEngine
    private val extractionEngine = repository.extractionEngine
    private val evolutionEngine = repository.evolutionEngine
    private val deploymentEngine = repository.deploymentEngine

    private val _activeAccountOwnerId = MutableStateFlow(DEFAULT_SHADOW_ACCOUNT_OWNER_ID)
    val activeAccountOwnerId: StateFlow<String> = _activeAccountOwnerId.asStateFlow()

    private val _uiState = MutableStateFlow(ShadowRealmUiState())
    val uiState: StateFlow<ShadowRealmUiState> = _uiState.asStateFlow()

    private val _extractionUiState = MutableStateFlow(ShadowExtractionUiState())
    val extractionUiState: StateFlow<ShadowExtractionUiState> = _extractionUiState.asStateFlow()

    private val _selectedShadow = MutableStateFlow<Shadow?>(null)
    val selectedShadow: StateFlow<Shadow?> = _selectedShadow.asStateFlow()

    private val _analytics = MutableStateFlow<ShadowArmyAnalytics?>(null)
    val analytics: StateFlow<ShadowArmyAnalytics?> = _analytics.asStateFlow()

    private val _settingsState = MutableStateFlow(ShadowRealmSettings(DEFAULT_SHADOW_ACCOUNT_OWNER_ID))
    val settingsState: StateFlow<ShadowRealmSettings> = _settingsState.asStateFlow()

    private var ownerScopeJob: Job? = null

    init {
        viewModelScope.launch {
            accountOwnerIdFlow
                .map { it.trim().ifBlank { DEFAULT_SHADOW_ACCOUNT_OWNER_ID } }
                .distinctUntilChanged()
                .collect { owner ->
                    rebindToAccountOwner(owner)
                }
        }
    }

    private fun rebindToAccountOwner(owner: String) {
        ownerScopeJob?.cancel()
        _activeAccountOwnerId.value = owner
        _selectedShadow.value = null
        _extractionUiState.value = ShadowExtractionUiState()
        _analytics.value = null
        _settingsState.value = ShadowRealmSettings(accountOwnerId = owner)
        _uiState.value = ShadowRealmUiState(
            isLoading = true,
            activeAccountOwnerId = owner
        )

        ownerScopeJob = viewModelScope.launch {
            launch {
                runCatching {
                    monarchProgressionEngine.getOrCreateProfile(owner)
                    refreshResourceBalances(owner)
                }.onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
            }

            launch {
                combine(
                    repository.observeMonarchProfile(owner),
                    repository.observeActiveShadows(owner),
                    repository.observeAllShadows(owner),
                    repository.observeActiveDeployments(owner),
                    repository.observeFormations(owner)
                ) { profile, active, all, deployments, formations ->
                    val essence = repository.getCurrentBalance(ShadowResourceType.SHADOW_ESSENCE, owner)
                    val fragments = repository.getCurrentBalance(ShadowResourceType.MONARCH_FRAGMENTS, owner)
                    val cores = repository.getCurrentBalance(ShadowResourceType.EVOLUTION_CORES, owner)
                    val crystals = repository.getCurrentBalance(ShadowResourceType.SHADOW_CRYSTALS, owner)
                    _uiState.value = _uiState.value.copy(
                        isLoading = profile == null,
                        activeAccountOwnerId = owner,
                        monarchProfile = profile,
                        activeShadows = active,
                        allShadows = all,
                        activeDeployments = deployments,
                        formations = formations,
                        shadowEssenceBalance = essence,
                        monarchFragmentsBalance = fragments,
                        evolutionCoresBalance = cores,
                        shadowCrystalsBalance = crystals
                    )
                }.collect {}
            }

            launch {
                repository.observeActiveFormation(owner).collect { activeFormation ->
                    _uiState.value = _uiState.value.copy(activeFormation = activeFormation)
                }
            }

            launch {
                repository.observeQuestProgress(owner).collect { quests ->
                    _uiState.value = _uiState.value.copy(questProgress = quests)
                }
            }

            launch {
                repository.observeMonarchSkillUnlocks(owner).collect { skills ->
                    _uiState.value = _uiState.value.copy(unlockedMonarchSkills = skills)
                }
            }

            launch {
                repository.observeExtractionHistory(owner).collect { history ->
                    _uiState.value = _uiState.value.copy(extractionHistory = history)
                }
            }

            launch {
                repository.observeResourceTransactions(owner).collect {
                    refreshResourceBalances(owner)
                }
            }

            if (preferencesRepository != null) {
                launch {
                    preferencesRepository.observeShadowSettings(owner).collect { settings ->
                        _settingsState.value = settings
                    }
                }
            }
        }
    }

    suspend fun refreshResourceBalances(accountOwnerId: String = _activeAccountOwnerId.value) {
        val essence = repository.getCurrentBalance(ShadowResourceType.SHADOW_ESSENCE, accountOwnerId)
        val fragments = repository.getCurrentBalance(ShadowResourceType.MONARCH_FRAGMENTS, accountOwnerId)
        val cores = repository.getCurrentBalance(ShadowResourceType.EVOLUTION_CORES, accountOwnerId)
        val crystals = repository.getCurrentBalance(ShadowResourceType.SHADOW_CRYSTALS, accountOwnerId)
        if (_activeAccountOwnerId.value == accountOwnerId) {
            _uiState.value = _uiState.value.copy(
                shadowEssenceBalance = essence,
                monarchFragmentsBalance = fragments,
                evolutionCoresBalance = cores,
                shadowCrystalsBalance = crystals
            )
        }
    }

    // -------------------------------------------------------------------------
    // SHADOW REALM PERSISTENT SETTINGS
    // -------------------------------------------------------------------------

    fun updateExtractionAlerts(enabled: Boolean) {
        val owner = _activeAccountOwnerId.value
        val repo = preferencesRepository ?: return
        viewModelScope.launch {
            repo.setShadowExtractionAlerts(owner, enabled)
        }
    }

    fun updateEvolutionAlerts(enabled: Boolean) {
        val owner = _activeAccountOwnerId.value
        val repo = preferencesRepository ?: return
        viewModelScope.launch {
            repo.setShadowEvolutionAlerts(owner, enabled)
        }
    }

    fun updateAutoRecallOnRest(enabled: Boolean) {
        val owner = _activeAccountOwnerId.value
        val repo = preferencesRepository ?: return
        viewModelScope.launch {
            repo.setShadowAutoRecallOnRest(owner, enabled)
        }
    }

    // -------------------------------------------------------------------------
    // SHADOW EXTRACTION
    // -------------------------------------------------------------------------

    fun evaluateExtraction(
        sourceType: ShadowExtractionSourceType,
        sourceEventId: String,
        evidenceSummary: String
    ) {
        val owner = _activeAccountOwnerId.value
        viewModelScope.launch {
            _extractionUiState.value = ShadowExtractionUiState(isEvaluating = true)
            val result = extractionEngine.evaluateExtractionEligibility(
                sourceType = sourceType,
                sourceEventId = sourceEventId,
                evidenceSummary = evidenceSummary,
                accountOwnerId = owner
            )
            _extractionUiState.value = ShadowExtractionUiState(
                isEvaluating = false,
                pendingExtraction = result,
                lastResult = if (result.outcome != ShadowExtractionOutcome.PENDING_CONFIRMATION) result else null
            )
        }
    }

    fun confirmExtraction() {
        val pending = _extractionUiState.value.pendingExtraction ?: return
        val owner = _activeAccountOwnerId.value
        if (pending.accountOwnerId != owner) {
            _extractionUiState.value = _extractionUiState.value.copy(
                isConfirming = false,
                error = "Pending extraction belongs to another account."
            )
            return
        }
        viewModelScope.launch {
            _extractionUiState.value = _extractionUiState.value.copy(isConfirming = true)
            val result = extractionEngine.confirmExtraction(pending, accountOwnerId = owner)
            result.onSuccess { event ->
                _extractionUiState.value = ShadowExtractionUiState(
                    lastResult = event
                )
                refreshResourceBalances(owner)
                if (event.outcome == ShadowExtractionOutcome.SUCCESS) {
                    repository.awardResource(
                        resourceType = ShadowResourceType.SHADOW_ESSENCE,
                        amount = 25,
                        sourceEventId = "essence_extraction_${event.extractionId}",
                        sourceDescription = "Shadow Extracted: ${event.shadowName}",
                        accountOwnerId = owner
                    )
                    refreshResourceBalances(owner)
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
        val owner = _activeAccountOwnerId.value
        viewModelScope.launch {
            _selectedShadow.value = repository.getShadowById(shadowId, accountOwnerId = owner)
        }
    }

    fun clearSelectedShadow() {
        _selectedShadow.value = null
    }

    // -------------------------------------------------------------------------
    // SHADOW EVOLUTION & PROGRESSION
    // -------------------------------------------------------------------------

    fun awardShadowXp(shadowId: String, xpAmount: Int, sourceTag: String) {
        val owner = _activeAccountOwnerId.value
        viewModelScope.launch {
            evolutionEngine.awardShadowXp(
                shadowId = shadowId,
                xpAmount = xpAmount,
                sourceTag = sourceTag,
                accountOwnerId = owner
            )
        }
    }

    // -------------------------------------------------------------------------
    // DEPLOYMENT
    // -------------------------------------------------------------------------

    fun deployShadow(shadowId: String, category: ShadowDeploymentCategory, role: String) {
        val owner = _activeAccountOwnerId.value
        viewModelScope.launch {
            deploymentEngine.deployShadow(
                shadowId = shadowId,
                category = category,
                role = role,
                accountOwnerId = owner
            ).onSuccess {
                // Success — flows update automatically
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun recallShadow(deploymentId: String) {
        val owner = _activeAccountOwnerId.value
        viewModelScope.launch {
            deploymentEngine.recallShadow(deploymentId, accountOwnerId = owner).onFailure { e ->
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    // -------------------------------------------------------------------------
    // FORMATIONS
    // -------------------------------------------------------------------------

    fun createFormation(name: String, type: ShadowFormationType, description: String, memberIds: List<String>) {
        val owner = _activeAccountOwnerId.value
        viewModelScope.launch {
            deploymentEngine.createFormation(
                name = name,
                type = type,
                description = description,
                memberShadowIds = memberIds,
                accountOwnerId = owner
            ).onFailure { e ->
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun setActiveFormation(formationId: String) {
        val owner = _activeAccountOwnerId.value
        viewModelScope.launch {
            deploymentEngine.setActiveFormation(formationId, accountOwnerId = owner).onFailure { e ->
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun deleteFormation(formationId: String) {
        val owner = _activeAccountOwnerId.value
        viewModelScope.launch {
            deploymentEngine.deleteFormation(formationId, accountOwnerId = owner).onFailure { e ->
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    // -------------------------------------------------------------------------
    // ANALYTICS
    // -------------------------------------------------------------------------

    fun loadAnalytics() {
        val owner = _activeAccountOwnerId.value
        viewModelScope.launch {
            _analytics.value = repository.buildShadowArmyAnalytics(accountOwnerId = owner)
        }
    }

    // -------------------------------------------------------------------------
    // SKILL CATALOG ACCESS
    // -------------------------------------------------------------------------

    fun getShadowSkillCatalog(): List<ShadowSkillDefinition> = ShadowSkillCatalog.skills
    fun getMonarchSkillCatalog(): List<MonarchSkillDefinition> = MonarchSkillCatalog.skills

    fun unlockShadowSkill(shadowId: String, skillId: String) {
        val owner = _activeAccountOwnerId.value
        viewModelScope.launch {
            evolutionEngine.unlockShadowSkill(
                shadowId = shadowId,
                skillId = skillId,
                evidenceNote = "Player-initiated skill unlock",
                accountOwnerId = owner
            )
        }
    }

    fun unlockMonarchSkill(skillId: String) {
        val owner = _activeAccountOwnerId.value
        viewModelScope.launch {
            val profile = monarchProgressionEngine.getOrCreateProfile(owner)
            monarchProgressionEngine.tryUnlockMonarchSkill(
                skillId = skillId,
                profile = profile,
                evidenceNote = "Player-initiated unlock in Monarch Skill Tree",
                accountOwnerId = owner
            )
        }
    }

    val eligibleEvidence = kotlinx.coroutines.flow.MutableStateFlow<List<com.example.domain.model.ShadowExtractionEvidenceItem>>(emptyList())

    fun loadEligibleEvidence() {
        val owner = _activeAccountOwnerId.value
        val db = repository.database ?: return
        viewModelScope.launch {
            val provider = com.example.domain.engine.ShadowExtractionEvidenceProvider(db)
            val items = provider.getEligibleEvidenceItems(owner)
            eligibleEvidence.value = items
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    companion object {
        fun factory(
            repository: ShadowArmyRepository,
            accountOwnerIdFlow: Flow<String> = flowOf(DEFAULT_SHADOW_ACCOUNT_OWNER_ID),
            preferencesRepository: UserPreferencesRepository? = null
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ShadowRealmViewModel(
                        repository = repository,
                        accountOwnerIdFlow = accountOwnerIdFlow,
                        preferencesRepository = preferencesRepository
                    ) as T
                }
            }
    }
}
