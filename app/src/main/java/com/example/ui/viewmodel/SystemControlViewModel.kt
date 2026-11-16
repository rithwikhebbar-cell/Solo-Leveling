package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.automation.AutomationOrchestrator
import com.example.data.repository.SystemControlRepository
import com.example.data.repository.SystemEventRepository
import com.example.domain.engine.UnifiedDailyExecutionEngine
import com.example.domain.model.AutomationExecutionRecord
import com.example.domain.model.AutomationJobSpec
import com.example.domain.model.DailyCycleExecution
import com.example.domain.model.DailyExecutionItem
import com.example.domain.model.DailyExecutionStatus
import com.example.domain.model.DailyExecutionSummary
import com.example.domain.model.EventDiagnosticsSummary
import com.example.domain.model.ModuleIntegrationInfo
import com.example.domain.model.RecoveryOperationRecord
import com.example.domain.model.SystemControlPreferences
import com.example.domain.model.SystemEvent
import com.example.domain.model.SystemHealthReport
import com.example.domain.model.SystemIntegrityFinding
import com.example.domain.model.SystemNotification
import com.example.domain.model.SystemNotificationPreferences
import com.example.domain.model.SystemPerformanceMetrics
import com.example.domain.model.TimeSlotSuggestion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface SystemControlUiState {
    data object Loading : SystemControlUiState
    data class Error(val message: String) : SystemControlUiState
    data class Ready(
        val healthReport: SystemHealthReport = SystemHealthReport(),
        val performanceMetrics: SystemPerformanceMetrics = SystemPerformanceMetrics(),
        val moduleStatuses: List<ModuleIntegrationInfo> = emptyList(),
        val jobs: List<AutomationJobSpec> = emptyList(),
        val executionLogs: List<AutomationExecutionRecord> = emptyList(),
        val dailyCycles: List<DailyCycleExecution> = emptyList(),
        val timeline: DailyExecutionSummary = DailyExecutionSummary(LocalDate.now().toEpochDay()),
        val events: List<SystemEvent> = emptyList(),
        val eventDiagnostics: EventDiagnosticsSummary = EventDiagnosticsSummary(),
        val findings: List<SystemIntegrityFinding> = emptyList(),
        val recoveryOperations: List<RecoveryOperationRecord> = emptyList(),
        val notifications: List<SystemNotification> = emptyList(),
        val notificationPrefs: SystemNotificationPreferences = SystemNotificationPreferences(),
        val systemPrefs: SystemControlPreferences = SystemControlPreferences(),
        val rescheduleSlots: List<TimeSlotSuggestion> = emptyList(),
        val rescheduleTargetKey: String? = null,
        val isBusy: Boolean = false,
        val feedbackMessage: String? = null
    ) : SystemControlUiState
}

private data class CoreSystemBundle(
    val health: SystemHealthReport,
    val perf: SystemPerformanceMetrics,
    val modules: List<ModuleIntegrationInfo>,
    val jobs: List<AutomationJobSpec>,
    val logs: List<AutomationExecutionRecord>
)

private data class ExecutionEventBundle(
    val cycles: List<DailyCycleExecution>,
    val timeline: DailyExecutionSummary,
    val events: List<SystemEvent>,
    val findings: List<SystemIntegrityFinding>,
    val recoveries: List<RecoveryOperationRecord>
)

private data class PrefsNotifBundle(
    val notifications: List<SystemNotification>,
    val notifPrefs: SystemNotificationPreferences,
    val sysPrefs: SystemControlPreferences,
    val reschedule: Pair<String?, List<TimeSlotSuggestion>>
)

class SystemControlViewModel(
    private val systemControlRepository: SystemControlRepository,
    private val systemEventRepository: SystemEventRepository,
    private val automationOrchestrator: AutomationOrchestrator,
    @Suppress("unused") private val appContext: Context
) : ViewModel() {

    private val isBusy = MutableStateFlow(false)
    private val feedbackMessage = MutableStateFlow<String?>(null)
    private val eventDiagnosticsFlow = MutableStateFlow(EventDiagnosticsSummary())
    private val rescheduleState = MutableStateFlow<Pair<String?, List<TimeSlotSuggestion>>>(null to emptyList())

    private val coreFlow = combine(
        systemControlRepository.latestHealthReport,
        systemControlRepository.performanceMetrics,
        systemControlRepository.moduleStatuses,
        automationOrchestrator.observeJobs(),
        automationOrchestrator.observeExecutionLogs(60)
    ) { health, perf, modules, jobs, logs ->
        CoreSystemBundle(health, perf, modules, jobs, logs)
    }

    private val execFlow = combine(
        systemControlRepository.observeDailyCycles(),
        systemControlRepository.observeDailyExecutionTimeline(LocalDate.now().toEpochDay()),
        systemEventRepository.observeRecentEvents(100),
        systemControlRepository.observeIntegrityFindings(),
        systemControlRepository.observeRecoveryOperations()
    ) { cycles, timeline, events, findings, recoveries ->
        ExecutionEventBundle(cycles, timeline, events, findings, recoveries)
    }

    private val prefsFlow = combine(
        systemControlRepository.observeNotifications(),
        systemControlRepository.observeNotificationPreferences(),
        systemControlRepository.observePreferences(),
        rescheduleState
    ) { notifs, nPrefs, sPrefs, resched ->
        PrefsNotifBundle(notifs, nPrefs, sPrefs, resched)
    }

    val uiState: StateFlow<SystemControlUiState> = combine(
        coreFlow,
        execFlow,
        prefsFlow,
        eventDiagnosticsFlow,
        combine(isBusy, feedbackMessage) { b, m -> b to m }
    ) { core, exec, prefs, diag, (busy, msg) ->
        SystemControlUiState.Ready(
            healthReport = core.health,
            performanceMetrics = core.perf,
            moduleStatuses = core.modules,
            jobs = core.jobs,
            executionLogs = core.logs,
            dailyCycles = exec.cycles,
            timeline = exec.timeline,
            events = exec.events,
            eventDiagnostics = diag,
            findings = exec.findings,
            recoveryOperations = exec.recoveries,
            notifications = prefs.notifications,
            notificationPrefs = prefs.notifPrefs,
            systemPrefs = prefs.sysPrefs,
            rescheduleTargetKey = prefs.reschedule.first,
            rescheduleSlots = prefs.reschedule.second,
            isBusy = busy,
            feedbackMessage = msg
        ) as SystemControlUiState
    }
        .catch { e -> emit(SystemControlUiState.Error(e.message ?: "Failed to load System Control Center.")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SystemControlUiState.Loading
        )

    init {
        refreshSystemDiagnostics(silent = true)
    }

    fun clearFeedback() {
        feedbackMessage.value = null
    }

    fun clearRescheduleSelection() {
        rescheduleState.value = null to emptyList()
    }

    fun refreshSystemDiagnostics(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) isBusy.value = true
            runCatching {
                automationOrchestrator.ensureRegistrySeeded()
                eventDiagnosticsFlow.value = systemEventRepository.getDiagnosticsSummary()
                systemControlRepository.evaluateSystemHealthAndPerformance()
                if (!silent) feedbackMessage.value = "System health & performance telemetry updated."
            }.onFailure { e ->
                if (!silent) feedbackMessage.value = "Diagnostics failed: ${e.message ?: "Unknown"}"
            }
            if (!silent) isBusy.value = false
        }
    }

    fun runAutomationNow(automationId: String) {
        viewModelScope.launch {
            isBusy.value = true
            runCatching {
                val res = automationOrchestrator.runJobNow(automationId, "MANUAL_USER_RUN")
                eventDiagnosticsFlow.value = systemEventRepository.getDiagnosticsSummary()
                systemControlRepository.evaluateSystemHealthAndPerformance()
                feedbackMessage.value = res.getOrElse { "Failed: ${it.message}" }
            }.onFailure { e ->
                feedbackMessage.value = "Automation error: ${e.message}"
            }
            isBusy.value = false
        }
    }

    fun toggleAutomationEnabled(automationId: String, enabled: Boolean) {
        viewModelScope.launch {
            runCatching {
                automationOrchestrator.setJobEnabled(automationId, enabled)
                systemControlRepository.evaluateSystemHealthAndPerformance()
                feedbackMessage.value = "${if (enabled) "Enabled" else "Disabled"} automation '$automationId'."
            }
        }
    }

    fun runDailyCycleNow() {
        viewModelScope.launch {
            isBusy.value = true
            runCatching {
                val res = automationOrchestrator.runDailyCycleIdempotent("MANUAL_CYCLE_CHECK")
                eventDiagnosticsFlow.value = systemEventRepository.getDiagnosticsSummary()
                systemControlRepository.evaluateSystemHealthAndPerformance()
                val cycle = res.getOrNull()
                feedbackMessage.value = if (cycle == null) {
                    "Daily cycle already completed for current logical day."
                } else {
                    "Daily cycle executed cleanly (caught up ${cycle.missedDaysCaughtUp} day(s))."
                }
            }.onFailure { e ->
                feedbackMessage.value = "Daily cycle error: ${e.message}"
            }
            isBusy.value = false
        }
    }

    fun updateExecutionStatus(
        item: DailyExecutionItem,
        newStatus: DailyExecutionStatus,
        evidenceReference: String = "",
        plannedStartMinuteOverride: Int? = null
    ) {
        viewModelScope.launch {
            isBusy.value = true
            val res = systemControlRepository.updateExecutionStatus(
                item = item,
                newStatus = newStatus,
                evidenceReference = evidenceReference,
                plannedStartMinuteOverride = plannedStartMinuteOverride
            )
            res.onSuccess {
                clearRescheduleSelection()
                eventDiagnosticsFlow.value = systemEventRepository.getDiagnosticsSummary()
                feedbackMessage.value = "${item.title} marked ${newStatus.label}."
            }.onFailure { e ->
                feedbackMessage.value = e.message ?: "Execution update blocked."
            }
            isBusy.value = false
        }
    }

    fun loadRescheduleSlots(item: DailyExecutionItem, allItems: List<DailyExecutionItem>) {
        val slots = UnifiedDailyExecutionEngine.findRescheduleSlots(item, allItems)
        rescheduleState.value = item.executionKey to slots
    }

    fun runIntegrityScan() {
        viewModelScope.launch {
            isBusy.value = true
            runCatching {
                val open = systemControlRepository.runIntegrityScan()
                systemControlRepository.evaluateSystemHealthAndPerformance()
                feedbackMessage.value = if (open.isEmpty()) {
                    "Cross-module integrity scan clean: 0 open anomalies."
                } else {
                    "Scan complete: ${open.size} open finding(s) require review."
                }
            }.onFailure { e ->
                feedbackMessage.value = "Integrity scan failed: ${e.message}"
            }
            isBusy.value = false
        }
    }

    fun repairIntegrityFinding(findingUuid: String, confirmedDestructive: Boolean) {
        viewModelScope.launch {
            isBusy.value = true
            systemControlRepository.repairFinding(findingUuid, confirmedDestructive)
                .onSuccess { detail ->
                    eventDiagnosticsFlow.value = systemEventRepository.getDiagnosticsSummary()
                    systemControlRepository.evaluateSystemHealthAndPerformance()
                    feedbackMessage.value = detail
                }
                .onFailure { e ->
                    feedbackMessage.value = e.message ?: "Repair failed."
                }
            isBusy.value = false
        }
    }

    fun dismissIntegrityFinding(findingUuid: String) {
        viewModelScope.launch {
            systemControlRepository.dismissFinding(findingUuid)
            systemControlRepository.evaluateSystemHealthAndPerformance()
            feedbackMessage.value = "Finding dismissed."
        }
    }

    fun retryEvent(eventId: String) {
        viewModelScope.launch {
            isBusy.value = true
            runCatching {
                systemEventRepository.retryEvent(eventId)
                eventDiagnosticsFlow.value = systemEventRepository.getDiagnosticsSummary()
                systemControlRepository.evaluateSystemHealthAndPerformance()
                feedbackMessage.value = "Re-dispatched event."
            }
            isBusy.value = false
        }
    }

    fun executeFullRecovery() {
        viewModelScope.launch {
            isBusy.value = true
            runCatching {
                val summary = systemControlRepository.executeRecoveryAll()
                eventDiagnosticsFlow.value = systemEventRepository.getDiagnosticsSummary()
                feedbackMessage.value = summary
            }.onFailure { e ->
                feedbackMessage.value = "Recovery operation failed: ${e.message}"
            }
            isBusy.value = false
        }
    }

    fun updateSystemPreferences(updated: SystemControlPreferences) {
        viewModelScope.launch {
            systemControlRepository.updatePreferences(updated)
            automationOrchestrator.scheduleAllEnabledWorkers()
            systemControlRepository.evaluateSystemHealthAndPerformance()
            feedbackMessage.value = "System integration settings saved."
        }
    }

    fun updateNotificationPreferences(updated: SystemNotificationPreferences) {
        viewModelScope.launch {
            systemControlRepository.updateNotificationPreferences(updated)
            systemControlRepository.evaluateSystemHealthAndPerformance()
            feedbackMessage.value = "Notification preferences updated."
        }
    }

    fun markNotificationRead(id: Long) {
        viewModelScope.launch {
            systemControlRepository.markNotificationRead(id)
        }
    }

    fun clearReadNotifications() {
        viewModelScope.launch {
            systemControlRepository.clearReadNotifications()
            feedbackMessage.value = "Cleared acknowledged alerts."
        }
    }

    companion object {
        fun factory(
            systemControlRepository: SystemControlRepository,
            systemEventRepository: SystemEventRepository,
            automationOrchestrator: AutomationOrchestrator,
            context: Context
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SystemControlViewModel(
                    systemControlRepository = systemControlRepository,
                    systemEventRepository = systemEventRepository,
                    automationOrchestrator = automationOrchestrator,
                    appContext = context.applicationContext
                ) as T
            }
        }
    }
}
