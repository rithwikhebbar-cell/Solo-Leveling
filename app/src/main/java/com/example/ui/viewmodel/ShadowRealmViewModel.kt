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
import com.example.domain.model.ShadowSquad
import com.example.domain.model.ShadowSquadCategory
import com.example.domain.model.SquadDetail
import com.example.domain.model.FormationTacticalAssessment
import com.example.domain.model.MissionTacticalRequirement
import com.example.domain.model.MissionSuitabilityEvaluation
import com.example.domain.model.MissionType
import com.example.domain.model.ShadowTacticalAnalytics
import com.example.domain.model.TacticalAlert
import com.example.domain.model.ShadowDominion
import com.example.domain.model.ShadowTerritory
import com.example.domain.model.ShadowInfrastructure
import com.example.domain.model.ShadowGarrison
import com.example.domain.model.ShadowDominionResearch
import com.example.domain.model.ShadowDominionActivity
import com.example.domain.model.DominionTerritoryDetail
import com.example.domain.model.DominionOverview
import com.example.domain.model.DominionAiBriefing
import com.example.domain.model.CitadelUpgradeType
import com.example.domain.model.ShadowInfrastructureType
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

    private val _collectionOverview = MutableStateFlow<com.example.domain.model.ShadowCollectionOverview?>(null)
    val collectionOverview: StateFlow<com.example.domain.model.ShadowCollectionOverview?> = _collectionOverview.asStateFlow()

    private val _sortOption = MutableStateFlow(com.example.domain.model.ShadowSortOption.RECENTLY_RECRUITED)
    val sortOption: StateFlow<com.example.domain.model.ShadowSortOption> = _sortOption.asStateFlow()

    private val _selectedEvidenceItem = MutableStateFlow<com.example.domain.model.ShadowExtractionEvidenceItem?>(null)
    val selectedEvidenceItem: StateFlow<com.example.domain.model.ShadowExtractionEvidenceItem?> = _selectedEvidenceItem.asStateFlow()

    private val _evolutionEvaluation = MutableStateFlow<com.example.domain.model.ShadowEvolutionEvaluation?>(null)
    val evolutionEvaluation: StateFlow<com.example.domain.model.ShadowEvolutionEvaluation?> = _evolutionEvaluation.asStateFlow()

    private val _isEvolving = MutableStateFlow(false)
    val isEvolving: StateFlow<Boolean> = _isEvolving.asStateFlow()

    private val _growthOverview = MutableStateFlow<com.example.domain.model.ShadowGrowthOverview?>(null)
    val growthOverview: StateFlow<com.example.domain.model.ShadowGrowthOverview?> = _growthOverview.asStateFlow()

    private val _shadowBondProfile = MutableStateFlow<com.example.domain.model.ShadowBondProfile?>(null)
    val shadowBondProfile: StateFlow<com.example.domain.model.ShadowBondProfile?> = _shadowBondProfile.asStateFlow()

    private val _shadowBondEvents = MutableStateFlow<List<com.example.domain.model.ShadowBondEvent>>(emptyList())
    val shadowBondEvents: StateFlow<List<com.example.domain.model.ShadowBondEvent>> = _shadowBondEvents.asStateFlow()

    private val _shadowBondMilestones = MutableStateFlow<List<com.example.domain.model.ShadowBondMilestone>>(emptyList())
    val shadowBondMilestones: StateFlow<List<com.example.domain.model.ShadowBondMilestone>> = _shadowBondMilestones.asStateFlow()

    private val _armyBondOverview = MutableStateFlow<com.example.domain.model.ShadowArmyBondOverview?>(null)
    val armyBondOverview: StateFlow<com.example.domain.model.ShadowArmyBondOverview?> = _armyBondOverview.asStateFlow()

    // Phase 6.5 — Tactical Command & Squad StateFlows
    private val _allSquads = MutableStateFlow<List<SquadDetail>>(emptyList())
    val allSquads: StateFlow<List<SquadDetail>> = _allSquads.asStateFlow()

    private val _activeSquad = MutableStateFlow<SquadDetail?>(null)
    val activeSquad: StateFlow<SquadDetail?> = _activeSquad.asStateFlow()

    private val _formationAssessment = MutableStateFlow<FormationTacticalAssessment?>(null)
    val formationAssessment: StateFlow<FormationTacticalAssessment?> = _formationAssessment.asStateFlow()

    private val _availableMissions = MutableStateFlow<List<MissionTacticalRequirement>>(emptyList())
    val availableMissions: StateFlow<List<MissionTacticalRequirement>> = _availableMissions.asStateFlow()

    private val _selectedMissionEvaluation = MutableStateFlow<MissionSuitabilityEvaluation?>(null)
    val selectedMissionEvaluation: StateFlow<MissionSuitabilityEvaluation?> = _selectedMissionEvaluation.asStateFlow()

    private val _tacticalAnalytics = MutableStateFlow<ShadowTacticalAnalytics?>(null)
    val tacticalAnalytics: StateFlow<ShadowTacticalAnalytics?> = _tacticalAnalytics.asStateFlow()

    private val _tacticalAlerts = MutableStateFlow<List<TacticalAlert>>(emptyList())
    val tacticalAlerts: StateFlow<List<TacticalAlert>> = _tacticalAlerts.asStateFlow()

    // Phase 6.6 — Dominion StateFlows
    private val _dominionOverview = MutableStateFlow<DominionOverview?>(null)
    val dominionOverview: StateFlow<DominionOverview?> = _dominionOverview.asStateFlow()

    private val _allTerritories = MutableStateFlow<List<ShadowTerritory>>(emptyList())
    val allTerritories: StateFlow<List<ShadowTerritory>> = _allTerritories.asStateFlow()

    private val _selectedTerritoryDetail = MutableStateFlow<DominionTerritoryDetail?>(null)
    val selectedTerritoryDetail: StateFlow<DominionTerritoryDetail?> = _selectedTerritoryDetail.asStateFlow()

    private val _allInfrastructures = MutableStateFlow<List<ShadowInfrastructure>>(emptyList())
    val allInfrastructures: StateFlow<List<ShadowInfrastructure>> = _allInfrastructures.asStateFlow()

    private val _allGarrisons = MutableStateFlow<List<ShadowGarrison>>(emptyList())
    val allGarrisons: StateFlow<List<ShadowGarrison>> = _allGarrisons.asStateFlow()

    private val _allResearch = MutableStateFlow<List<ShadowDominionResearch>>(emptyList())
    val allResearch: StateFlow<List<ShadowDominionResearch>> = _allResearch.asStateFlow()

    private val _dominionActivities = MutableStateFlow<List<ShadowDominionActivity>>(emptyList())
    val dominionActivities: StateFlow<List<ShadowDominionActivity>> = _dominionActivities.asStateFlow()

    private val _dominionAiBriefing = MutableStateFlow<DominionAiBriefing?>(null)
    val dominionAiBriefing: StateFlow<DominionAiBriefing?> = _dominionAiBriefing.asStateFlow()

    private var ownerScopeJob: Job? = null
    private var shadowBondJob: Job? = null

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
        shadowBondJob?.cancel()
        _activeAccountOwnerId.value = owner
        _selectedShadow.value = null
        _extractionUiState.value = ShadowExtractionUiState()
        _analytics.value = null
        _evolutionEvaluation.value = null
        _isEvolving.value = false
        _growthOverview.value = null
        _shadowBondProfile.value = null
        _shadowBondEvents.value = emptyList()
        _shadowBondMilestones.value = emptyList()
        _armyBondOverview.value = null
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
                    loadCollectionOverview(owner)
                    loadEligibleEvidence(owner)
                    loadGrowthOverview(owner)
                }.onFailure { e ->
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
            }

            launch {
                repository.observeArmyBondOverview(owner).collect {
                    _armyBondOverview.value = it
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

            launch {
                repository.observeArmyBondOverview(owner).collect { overview ->
                    _armyBondOverview.value = overview
                }
            }

            launch {
                repository.observeSquadDetailsForOwner(owner).collect { squads ->
                    _allSquads.value = squads
                }
            }

            launch {
                repository.observeActiveSquadDetailForOwner(owner).collect { active ->
                    _activeSquad.value = active
                }
            }

            launch {
                repository.observeActiveFormationAssessment(owner).collect { assessment ->
                    _formationAssessment.value = assessment
                }
            }

            launch {
                repository.observeDominionForOwner(owner).collect {
                    loadDominionOverview()
                }
            }

            launch {
                repository.observeTerritoriesForOwner(owner).collect { territories ->
                    _allTerritories.value = territories
                }
            }

            launch {
                repository.observeInfrastructuresForOwner(owner).collect { structures ->
                    _allInfrastructures.value = structures
                }
            }

            launch {
                repository.observeGarrisonsForOwner(owner).collect { garrisons ->
                    _allGarrisons.value = garrisons
                }
            }

            launch {
                repository.observeResearchForOwner(owner).collect { research ->
                    _allResearch.value = research
                }
            }

            launch {
                repository.observeDominionActivities(owner).collect { activities ->
                    _dominionActivities.value = activities
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
                loadCollectionOverview(owner)
                loadEligibleEvidence(owner)
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
            loadShadowBond(shadowId)
        }
    }

    fun clearSelectedShadow() {
        shadowBondJob?.cancel()
        _selectedShadow.value = null
        _shadowBondProfile.value = null
        _shadowBondEvents.value = emptyList()
        _shadowBondMilestones.value = emptyList()
    }

    fun loadShadowBond(shadowId: String) {
        val owner = _activeAccountOwnerId.value
        shadowBondJob?.cancel()
        shadowBondJob = viewModelScope.launch {
            launch {
                repository.getOrCreateShadowBondProfile(shadowId, owner)
                repository.observeShadowBondProfile(shadowId, owner).collect {
                    _shadowBondProfile.value = it
                }
            }
            launch {
                repository.observeShadowBondEvents(shadowId, owner).collect {
                    _shadowBondEvents.value = it
                }
            }
            launch {
                repository.observeShadowBondMilestones(shadowId, owner).collect {
                    _shadowBondMilestones.value = it
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // SHADOW EVOLUTION & PROGRESSION (Authoritative Ingestion Only)
    // -------------------------------------------------------------------------

    fun awardAuthoritativeXp(
        shadowId: String,
        sourceType: com.example.domain.model.ShadowExtractionSourceType,
        eventId: String
    ) {
        val owner = _activeAccountOwnerId.value
        viewModelScope.launch {
            val awarded = repository.awardShadowXpFromEvent(
                shadowId = shadowId,
                sourceType = sourceType,
                eventId = eventId,
                accountOwnerId = owner
            )
            if (awarded) {
                loadGrowthOverview(owner)
                loadEvolutionEvaluation(shadowId)
                _selectedShadow.value = repository.getShadowById(shadowId, owner)
            }
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

    fun loadEligibleEvidence(accountOwnerId: String = _activeAccountOwnerId.value) {
        val db = repository.database ?: return
        viewModelScope.launch {
            val provider = com.example.domain.engine.ShadowExtractionEvidenceProvider(db)
            val items = provider.getEligibleEvidenceItems(accountOwnerId)
            eligibleEvidence.value = items
        }
    }

    fun loadCollectionOverview(accountOwnerId: String = _activeAccountOwnerId.value) {
        viewModelScope.launch {
            _collectionOverview.value = repository.getCollectionOverview(accountOwnerId)
        }
    }

    fun setSortOption(option: com.example.domain.model.ShadowSortOption) {
        _sortOption.value = option
    }

    fun selectEvidenceItem(item: com.example.domain.model.ShadowExtractionEvidenceItem) {
        _selectedEvidenceItem.value = item
        if (item.eligibility == com.example.domain.model.ShadowEvidenceEligibility.ELIGIBLE) {
            evaluateExtraction(
                sourceType = item.sourceType,
                sourceEventId = item.sourceRecordId,
                evidenceSummary = item.title
            )
        }
    }

    fun clearSelectedEvidence() {
        _selectedEvidenceItem.value = null
        cancelExtraction()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // -------------------------------------------------------------------------
    // PHASE 6.3 — SHADOW GROWTH & EVOLUTION ACTIONS
    // -------------------------------------------------------------------------

    fun loadEvolutionEvaluation(shadowId: String) {
        val owner = _activeAccountOwnerId.value
        viewModelScope.launch {
            _evolutionEvaluation.value = repository.evaluateEvolution(shadowId, owner)
        }
    }

    fun clearEvolutionEvaluation() {
        _evolutionEvaluation.value = null
    }

    fun confirmEvolution(shadowId: String, onComplete: ((Boolean, String?) -> Unit)? = null) {
        val owner = _activeAccountOwnerId.value
        viewModelScope.launch {
            _isEvolving.value = true
            val result = repository.confirmEvolution(shadowId, owner)
            _isEvolving.value = false
            result.onSuccess { evolvedShadow ->
                _selectedShadow.value = evolvedShadow
                loadEvolutionEvaluation(shadowId)
                refreshResourceBalances(owner)
                loadGrowthOverview(owner)
                loadCollectionOverview(owner)
                onComplete?.invoke(true, null)
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(error = error.message)
                onComplete?.invoke(false, error.message)
            }
        }
    }

    fun loadGrowthOverview(accountOwnerId: String = _activeAccountOwnerId.value) {
        viewModelScope.launch {
            _growthOverview.value = repository.getShadowGrowthOverview(accountOwnerId)
        }
    }

    // -------------------------------------------------------------------------
    // PHASE 6.5 — SHADOW ARMY INTELLIGENCE & TACTICAL COMMAND ACTIONS
    // -------------------------------------------------------------------------

    fun loadTacticalCommandCenter() {
        val owner = _activeAccountOwnerId.value
        viewModelScope.launch {
            _availableMissions.value = repository.getAvailableMissions(owner)
            _tacticalAnalytics.value = repository.getTacticalAnalytics(owner)
            _tacticalAlerts.value = repository.getTacticalAlerts(owner)
        }
    }

    fun createSquad(
        name: String,
        category: ShadowSquadCategory,
        memberIds: List<String> = emptyList(),
        leaderId: String? = null,
        onComplete: ((Boolean, String?) -> Unit)? = null
    ) {
        val owner = _activeAccountOwnerId.value
        viewModelScope.launch {
            val result = repository.createSquad(name, category, memberIds, leaderId, owner)
            result.onSuccess {
                loadTacticalCommandCenter()
                onComplete?.invoke(true, null)
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(error = err.message)
                onComplete?.invoke(false, err.message)
            }
        }
    }

    fun updateSquad(
        squadId: String,
        name: String,
        category: ShadowSquadCategory,
        onComplete: ((Boolean, String?) -> Unit)? = null
    ) {
        val owner = _activeAccountOwnerId.value
        viewModelScope.launch {
            val result = repository.updateSquad(squadId, name, category, owner)
            result.onSuccess {
                loadTacticalCommandCenter()
                onComplete?.invoke(true, null)
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(error = err.message)
                onComplete?.invoke(false, err.message)
            }
        }
    }

    fun addMemberToSquad(
        squadId: String,
        shadowId: String,
        onComplete: ((Boolean, String?) -> Unit)? = null
    ) {
        val owner = _activeAccountOwnerId.value
        viewModelScope.launch {
            val result = repository.addMemberToSquad(squadId, shadowId, owner)
            result.onSuccess {
                loadTacticalCommandCenter()
                onComplete?.invoke(true, null)
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(error = err.message)
                onComplete?.invoke(false, err.message)
            }
        }
    }

    fun removeMemberFromSquad(
        squadId: String,
        shadowId: String,
        onComplete: ((Boolean, String?) -> Unit)? = null
    ) {
        val owner = _activeAccountOwnerId.value
        viewModelScope.launch {
            val result = repository.removeMemberFromSquad(squadId, shadowId, owner)
            result.onSuccess {
                loadTacticalCommandCenter()
                onComplete?.invoke(true, null)
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(error = err.message)
                onComplete?.invoke(false, err.message)
            }
        }
    }

    fun setSquadLeader(
        squadId: String,
        leaderShadowId: String,
        onComplete: ((Boolean, String?) -> Unit)? = null
    ) {
        val owner = _activeAccountOwnerId.value
        viewModelScope.launch {
            val result = repository.setSquadLeader(squadId, leaderShadowId, owner)
            result.onSuccess {
                loadTacticalCommandCenter()
                onComplete?.invoke(true, null)
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(error = err.message)
                onComplete?.invoke(false, err.message)
            }
        }
    }

    fun setActiveSquad(
        squadId: String,
        onComplete: ((Boolean, String?) -> Unit)? = null
    ) {
        val owner = _activeAccountOwnerId.value
        viewModelScope.launch {
            val result = repository.setActiveSquad(squadId, owner)
            result.onSuccess {
                loadTacticalCommandCenter()
                onComplete?.invoke(true, null)
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(error = err.message)
                onComplete?.invoke(false, err.message)
            }
        }
    }

    fun duplicateSquad(
        squadId: String,
        onComplete: ((Boolean, String?) -> Unit)? = null
    ) {
        val owner = _activeAccountOwnerId.value
        viewModelScope.launch {
            val result = repository.duplicateSquad(squadId, owner)
            result.onSuccess {
                loadTacticalCommandCenter()
                onComplete?.invoke(true, null)
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(error = err.message)
                onComplete?.invoke(false, err.message)
            }
        }
    }

    fun deleteSquad(
        squadId: String,
        onComplete: ((Boolean, String?) -> Unit)? = null
    ) {
        val owner = _activeAccountOwnerId.value
        viewModelScope.launch {
            val result = repository.deleteSquad(squadId, owner)
            result.onSuccess {
                loadTacticalCommandCenter()
                onComplete?.invoke(true, null)
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(error = err.message)
                onComplete?.invoke(false, err.message)
            }
        }
    }

    fun evaluateMission(
        mission: MissionTacticalRequirement,
        squad: ShadowSquad?
    ) {
        val owner = _activeAccountOwnerId.value
        viewModelScope.launch {
            _selectedMissionEvaluation.value = repository.evaluateMissionSuitability(mission, squad, owner)
        }
    }

    fun confirmAndDeployMission(
        missionId: String,
        missionType: MissionType,
        squadId: String?,
        shadowIdsToDeploy: List<String>,
        onComplete: ((Boolean, String?) -> Unit)? = null
    ) {
        val owner = _activeAccountOwnerId.value
        viewModelScope.launch {
            val result = repository.confirmAndDeployMissionAssignment(
                missionId, missionType, squadId, shadowIdsToDeploy, owner
            )
            result.onSuccess {
                loadTacticalCommandCenter()
                onComplete?.invoke(true, null)
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(error = err.message)
                onComplete?.invoke(false, err.message)
            }
        }
    }

    // -------------------------------------------------------------------------
    // PHASE 6.6 — DOMINION ACTIONS
    // -------------------------------------------------------------------------

    fun loadDominionOverview() {
        val owner = _activeAccountOwnerId.value
        viewModelScope.launch {
            _dominionOverview.value = repository.getDominionOverview(owner)
            _dominionAiBriefing.value = repository.getDominionAiBriefing(owner)
        }
    }

    fun initializeDominion(onComplete: ((Boolean, String?) -> Unit)? = null) {
        val owner = _activeAccountOwnerId.value
        viewModelScope.launch {
            runCatching {
                repository.ensureDominionInitialized(owner)
            }.onSuccess {
                loadDominionOverview()
                onComplete?.invoke(true, null)
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(error = err.message)
                onComplete?.invoke(false, err.message)
            }
        }
    }

    fun upgradeCitadel(
        upgradeType: CitadelUpgradeType,
        onComplete: ((Boolean, String?) -> Unit)? = null
    ) {
        val owner = _activeAccountOwnerId.value
        viewModelScope.launch {
            val res = repository.upgradeCitadel(owner, upgradeType)
            res.onSuccess {
                loadDominionOverview()
                refreshResourceBalances(owner)
                onComplete?.invoke(true, null)
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(error = err.message)
                onComplete?.invoke(false, err.message)
            }
        }
    }

    fun selectTerritory(territoryId: String) {
        val owner = _activeAccountOwnerId.value
        viewModelScope.launch {
            _selectedTerritoryDetail.value = repository.getTerritoryDetail(owner, territoryId)
        }
    }

    fun discoverTerritory(
        territoryId: String,
        onComplete: ((Boolean, String?) -> Unit)? = null
    ) {
        val owner = _activeAccountOwnerId.value
        viewModelScope.launch {
            val res = repository.discoverTerritory(owner, territoryId)
            res.onSuccess {
                loadDominionOverview()
                selectTerritory(territoryId)
                onComplete?.invoke(true, null)
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(error = err.message)
                onComplete?.invoke(false, err.message)
            }
        }
    }

    fun startTerritoryExploration(
        territoryId: String,
        squadId: String,
        onComplete: ((Boolean, String?) -> Unit)? = null
    ) {
        val owner = _activeAccountOwnerId.value
        viewModelScope.launch {
            val res = repository.startTerritoryExploration(owner, territoryId, squadId)
            res.onSuccess {
                loadDominionOverview()
                selectTerritory(territoryId)
                onComplete?.invoke(true, null)
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(error = err.message)
                onComplete?.invoke(false, err.message)
            }
        }
    }

    fun claimTerritory(
        territoryId: String,
        onComplete: ((Boolean, String?) -> Unit)? = null
    ) {
        val owner = _activeAccountOwnerId.value
        viewModelScope.launch {
            val res = repository.claimTerritory(owner, territoryId)
            res.onSuccess {
                loadDominionOverview()
                selectTerritory(territoryId)
                onComplete?.invoke(true, null)
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(error = err.message)
                onComplete?.invoke(false, err.message)
            }
        }
    }

    fun constructInfrastructure(
        territoryId: String,
        structureType: ShadowInfrastructureType,
        onComplete: ((Boolean, String?) -> Unit)? = null
    ) {
        val owner = _activeAccountOwnerId.value
        viewModelScope.launch {
            val res = repository.constructInfrastructure(owner, territoryId, structureType)
            res.onSuccess {
                loadDominionOverview()
                selectTerritory(territoryId)
                refreshResourceBalances(owner)
                onComplete?.invoke(true, null)
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(error = err.message)
                onComplete?.invoke(false, err.message)
            }
        }
    }

    fun upgradeInfrastructure(
        structureId: String,
        territoryId: String,
        onComplete: ((Boolean, String?) -> Unit)? = null
    ) {
        val owner = _activeAccountOwnerId.value
        viewModelScope.launch {
            val res = repository.upgradeInfrastructure(owner, structureId)
            res.onSuccess {
                loadDominionOverview()
                selectTerritory(territoryId)
                refreshResourceBalances(owner)
                onComplete?.invoke(true, null)
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(error = err.message)
                onComplete?.invoke(false, err.message)
            }
        }
    }

    fun collectResources(
        structureId: String,
        territoryId: String,
        onComplete: ((Boolean, String?) -> Unit)? = null
    ) {
        val owner = _activeAccountOwnerId.value
        viewModelScope.launch {
            val res = repository.collectResources(owner, structureId)
            res.onSuccess { collected ->
                loadDominionOverview()
                selectTerritory(territoryId)
                refreshResourceBalances(owner)
                onComplete?.invoke(true, "Collected $collected resources")
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(error = err.message)
                onComplete?.invoke(false, err.message)
            }
        }
    }

    fun stationGarrison(
        territoryId: String,
        squadId: String,
        onComplete: ((Boolean, String?) -> Unit)? = null
    ) {
        val owner = _activeAccountOwnerId.value
        viewModelScope.launch {
            val res = repository.stationGarrison(owner, territoryId, squadId)
            res.onSuccess {
                loadDominionOverview()
                selectTerritory(territoryId)
                onComplete?.invoke(true, null)
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(error = err.message)
                onComplete?.invoke(false, err.message)
            }
        }
    }

    fun recallGarrison(
        garrisonId: String,
        territoryId: String,
        onComplete: ((Boolean, String?) -> Unit)? = null
    ) {
        val owner = _activeAccountOwnerId.value
        viewModelScope.launch {
            val res = repository.recallGarrison(owner, garrisonId)
            res.onSuccess {
                loadDominionOverview()
                selectTerritory(territoryId)
                onComplete?.invoke(true, null)
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(error = err.message)
                onComplete?.invoke(false, err.message)
            }
        }
    }

    fun conductResearch(
        projectKey: String,
        onComplete: ((Boolean, String?) -> Unit)? = null
    ) {
        val owner = _activeAccountOwnerId.value
        viewModelScope.launch {
            val res = repository.conductResearch(owner, projectKey)
            res.onSuccess {
                loadDominionOverview()
                refreshResourceBalances(owner)
                onComplete?.invoke(true, null)
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(error = err.message)
                onComplete?.invoke(false, err.message)
            }
        }
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
