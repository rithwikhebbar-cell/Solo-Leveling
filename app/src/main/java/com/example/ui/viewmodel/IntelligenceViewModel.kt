package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.IntelligenceDomainInsights
import com.example.data.repository.IntelligenceRepository
import com.example.domain.model.AdaptiveDailyPlan
import com.example.domain.model.AdvisorConsultationResult
import com.example.domain.model.ConsistencyPrediction
import com.example.domain.model.DevelopmentRoadmap
import com.example.domain.model.IntelligenceReport
import com.example.domain.model.PersonalizedRecommendation
import com.example.domain.model.PlayerIntelligencePreferences
import com.example.domain.model.PlayerIntelligenceProfile
import com.example.domain.model.RecommendationFeedbackRecord
import com.example.domain.model.RecommendationStatus
import com.example.domain.model.ReportPeriodType
import com.example.domain.model.RoadmapHorizon
import com.example.domain.model.StrengthWeaknessFinding
import com.example.domain.model.WorkloadAssessment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface IntelligenceUiState {
    data object Loading : IntelligenceUiState
    data class Error(val message: String) : IntelligenceUiState
    data class Ready(
        val profile: PlayerIntelligenceProfile? = null,
        val findings: List<StrengthWeaknessFinding> = emptyList(),
        val latestPlan: AdaptiveDailyPlan? = null,
        val workload: WorkloadAssessment? = null,
        val predictions: List<ConsistencyPrediction> = emptyList(),
        val recommendations: List<PersonalizedRecommendation> = emptyList(),
        val feedbackHistory: List<RecommendationFeedbackRecord> = emptyList(),
        val roadmaps: List<DevelopmentRoadmap> = emptyList(),
        val reports: List<IntelligenceReport> = emptyList(),
        val preferences: PlayerIntelligencePreferences = PlayerIntelligencePreferences(),
        val consultations: List<AdvisorConsultationResult> = emptyList(),
        val domainInsights: IntelligenceDomainInsights = IntelligenceDomainInsights(),
        val isBusy: Boolean = false,
        val feedbackMessage: String? = null
    ) : IntelligenceUiState
}

private data class CoreIntelligenceBundle(
    val profile: PlayerIntelligenceProfile?,
    val findings: List<StrengthWeaknessFinding>,
    val latestPlan: AdaptiveDailyPlan?,
    val workload: WorkloadAssessment?,
    val predictions: List<ConsistencyPrediction>
)

private data class SecondaryIntelligenceBundle(
    val recommendations: List<PersonalizedRecommendation>,
    val feedbackHistory: List<RecommendationFeedbackRecord>,
    val roadmaps: List<DevelopmentRoadmap>,
    val reports: List<IntelligenceReport>,
    val preferences: PlayerIntelligencePreferences
)

class IntelligenceViewModel(
    private val repository: IntelligenceRepository
) : ViewModel() {

    private val isBusy = MutableStateFlow(false)
    private val feedbackMessage = MutableStateFlow<String?>(null)

    private val coreFlow = combine(
        repository.profile,
        repository.findings,
        repository.latestPlan,
        repository.latestWorkload,
        repository.predictions
    ) { profile, findings, plan, workload, predictions ->
        CoreIntelligenceBundle(profile, findings, plan, workload, predictions)
    }

    private val secondaryFlow = combine(
        repository.recommendations,
        repository.feedbackHistory,
        repository.roadmaps,
        repository.reports,
        repository.preferences
    ) { recs, feedback, roadmaps, reports, prefs ->
        SecondaryIntelligenceBundle(recs, feedback, roadmaps, reports, prefs)
    }

    val uiState: StateFlow<IntelligenceUiState> = combine(
        coreFlow,
        secondaryFlow,
        repository.consultations,
        repository.domainInsights,
        combine(isBusy, feedbackMessage) { busy, msg -> busy to msg }
    ) { core, sec, consultations, insights, (busy, msg) ->
        IntelligenceUiState.Ready(
            profile = core.profile,
            findings = core.findings,
            latestPlan = core.latestPlan,
            workload = core.workload,
            predictions = core.predictions,
            recommendations = sec.recommendations,
            feedbackHistory = sec.feedbackHistory,
            roadmaps = sec.roadmaps,
            reports = sec.reports,
            preferences = sec.preferences,
            consultations = consultations,
            domainInsights = insights,
            isBusy = busy,
            feedbackMessage = msg
        ) as IntelligenceUiState
    }
        .catch { e -> emit(IntelligenceUiState.Error(e.message ?: "Failed to load intelligence state.")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = IntelligenceUiState.Loading
        )

    init {
        refreshIntelligence(force = false)
    }

    fun clearFeedback() {
        feedbackMessage.value = null
    }

    fun refreshIntelligence(force: Boolean = true) {
        viewModelScope.launch {
            isBusy.value = true
            runCatching {
                val updated = repository.refreshIntelligenceIfNeeded(force = force)
                if (force) {
                    feedbackMessage.value = if (updated) {
                        "Intelligence diagnostics synchronized with latest stored telemetry."
                    } else {
                        "Telemetry unchanged; cached intelligence profile is already up to date."
                    }
                }
            }.onFailure { e ->
                feedbackMessage.value = "Intelligence refresh error: ${e.message ?: "Unknown"}"
            }
            isBusy.value = false
        }
    }

    fun generateDailyPlan(isPartialDay: Boolean = false) {
        viewModelScope.launch {
            isBusy.value = true
            runCatching {
                val plan = repository.generateDailyAdaptivePlan(
                    targetEpochDay = LocalDate.now().toEpochDay(),
                    isPartialDay = isPartialDay
                )
                feedbackMessage.value = "Generated ${if (isPartialDay) "partial-day" else "full-day"} draft plan with ${plan.blocks.size} block(s)."
            }.onFailure { e ->
                feedbackMessage.value = "Plan generation failed: ${e.message ?: "Unknown"}"
            }
            isBusy.value = false
        }
    }

    fun approveDailyPlan(planId: Long) {
        viewModelScope.launch {
            isBusy.value = true
            runCatching {
                val count = repository.approveAdaptivePlan(planId)
                feedbackMessage.value = "Approved and committed $count adaptive block(s) to unified Calendar."
            }.onFailure { e ->
                feedbackMessage.value = "Plan approval failed: ${e.message ?: "Unknown"}"
            }
            isBusy.value = false
        }
    }

    fun updateBlockProgress(blockId: Long, completedMinutes: Int) {
        viewModelScope.launch {
            runCatching {
                repository.updatePlanBlockProgress(blockId, completedMinutes)
                feedbackMessage.value = "Updated block progress ($completedMinutes min)."
            }.onFailure { e ->
                feedbackMessage.value = "Progress update failed: ${e.message ?: "Unknown"}"
            }
        }
    }

    fun rescheduleBlock(blockId: Long, newStartMinute: Int) {
        viewModelScope.launch {
            runCatching {
                val res = repository.reschedulePlanBlock(blockId, newStartMinute)
                feedbackMessage.value = res?.let { "Rescheduled block to ${it.formattedWindow()}." } ?: "Block not found."
            }.onFailure { e ->
                feedbackMessage.value = "Reschedule failed: ${e.message ?: "Unknown"}"
            }
        }
    }

    fun respondToRecommendation(recUuid: String, action: RecommendationStatus, note: String = "") {
        viewModelScope.launch {
            runCatching {
                repository.respondToRecommendation(recUuid, action, note)
                feedbackMessage.value = "Recommendation marked ${action.label}."
            }.onFailure { e ->
                feedbackMessage.value = "Action failed: ${e.message ?: "Unknown"}"
            }
        }
    }

    fun generateRoadmap(horizon: RoadmapHorizon, focusDomains: List<String>) {
        viewModelScope.launch {
            isBusy.value = true
            runCatching {
                val roadmap = repository.generateRoadmap(horizon, focusDomains)
                feedbackMessage.value = "Generated draft ${horizon.label} with ${roadmap.milestones.size} milestone(s)."
            }.onFailure { e ->
                feedbackMessage.value = "Roadmap failed: ${e.message ?: "Unknown"}"
            }
            isBusy.value = false
        }
    }

    fun approveRoadmap(roadmapUuid: String) {
        viewModelScope.launch {
            runCatching {
                repository.approveRoadmap(roadmapUuid)
                feedbackMessage.value = "Roadmap approved and locked for tracking."
            }
        }
    }

    fun toggleRoadmapMilestone(milestoneUuid: String) {
        viewModelScope.launch {
            runCatching {
                repository.toggleRoadmapMilestone(milestoneUuid)
            }
        }
    }

    fun editRoadmapMilestone(milestoneUuid: String, title: String, notes: String) {
        viewModelScope.launch {
            runCatching {
                if (repository.editRoadmapMilestone(milestoneUuid, title, notes)) {
                    feedbackMessage.value = "Milestone updated."
                }
            }
        }
    }

    fun generateReport(periodType: ReportPeriodType) {
        viewModelScope.launch {
            isBusy.value = true
            runCatching {
                val report = repository.generateReport(periodType)
                feedbackMessage.value = "Generated ${report.periodType.label} (${report.periodLabel})."
            }.onFailure { e ->
                feedbackMessage.value = "Report generation failed: ${e.message ?: "Unknown"}"
            }
            isBusy.value = false
        }
    }

    fun askAdvisor(question: String) {
        viewModelScope.launch {
            isBusy.value = true
            runCatching {
                repository.askAdvisor(question)
                feedbackMessage.value = "Advisor response logged."
            }.onFailure { e ->
                feedbackMessage.value = "Advisor query failed: ${e.message ?: "Unknown"}"
            }
            isBusy.value = false
        }
    }

    fun updatePreferences(updated: PlayerIntelligencePreferences) {
        viewModelScope.launch {
            isBusy.value = true
            runCatching {
                repository.updatePreferences(updated)
                feedbackMessage.value = "Intelligence preferences updated."
            }.onFailure { e ->
                feedbackMessage.value = "Save failed: ${e.message ?: "Unknown"}"
            }
            isBusy.value = false
        }
    }

    fun clearAllIntelligenceData() {
        viewModelScope.launch {
            isBusy.value = true
            runCatching {
                repository.clearAllIntelligenceData()
                feedbackMessage.value = "Cleared all stored Intelligence diagnostics, plans, roadmaps, and reports. Core player progression preserved."
            }.onFailure { e ->
                feedbackMessage.value = "Clear failed: ${e.message ?: "Unknown"}"
            }
            isBusy.value = false
        }
    }

    companion object {
        fun factory(repository: IntelligenceRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return IntelligenceViewModel(repository) as T
                }
            }
    }
}
