package com.example.ui.screens.system

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.DailyExecutionItem
import com.example.domain.model.DailyExecutionStatus
import com.example.domain.model.EventProcessingStatus
import com.example.domain.model.IntegrityFindingStatus
import com.example.domain.model.IntegritySeverity
import com.example.domain.model.SystemIntegrityFinding
import com.example.ui.components.HolographicPanel
import com.example.ui.components.SystemEmptyState
import com.example.ui.components.SystemSectionTitle
import com.example.ui.theme.SystemColors
import com.example.ui.viewmodel.SystemControlUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DailyExecutionTimelineScreen(
    uiState: SystemControlUiState,
    onRetry: () -> Unit,
    onClearFeedback: () -> Unit,
    onUpdateStatus: (DailyExecutionItem, DailyExecutionStatus, String, Int?) -> Unit,
    onLoadRescheduleSlots: (DailyExecutionItem, List<DailyExecutionItem>) -> Unit,
    onClearReschedule: () -> Unit
) {
    var filterStatus by rememberSaveable { mutableStateOf<String>("ALL") }
    var evidenceTargetKey by rememberSaveable { mutableStateOf<String?>(null) }
    var evidenceInput by rememberSaveable { mutableStateOf("") }

    SystemStateScaffold(
        uiState = uiState,
        onRetry = onRetry,
        feedbackMessage = (uiState as? SystemControlUiState.Ready)?.feedbackMessage,
        onClearFeedback = onClearFeedback
    ) { state ->
        val items = state.timeline.items.filter { item ->
            when (filterStatus) {
                "ACTIVE" -> !item.status.isTerminal
                "COMPLETED" -> item.status == DailyExecutionStatus.COMPLETED
                "MISSED" -> item.status == DailyExecutionStatus.MISSED
                else -> true
            }
        }
        val evidenceTarget = state.timeline.items.firstOrNull { it.executionKey == evidenceTargetKey }

        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 760.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SystemSectionTitle(
                    title = "Daily Execution Timeline",
                    subtitle = "${state.timeline.completedCount}/${state.timeline.items.size} completed • ${state.timeline.conflictCount} conflicts • ${state.timeline.recoveryRestrictedCount} safety-gated"
                )
            }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ALL", "ACTIVE", "COMPLETED", "MISSED").forEach { tag ->
                        FilterChip(
                            selected = filterStatus == tag,
                            onClick = { filterStatus = tag },
                            label = { Text(tag) },
                            modifier = Modifier.testTag("timeline_filter_${tag.lowercase()}"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SystemColors.ElectricCyan.copy(alpha = 0.2f),
                                selectedLabelColor = SystemColors.ElectricCyan
                            )
                        )
                    }
                }
            }
            if (items.isEmpty()) {
                item {
                    SystemEmptyState(
                        title = "No Execution Items Match Filter",
                        description = "Add routines, habits, tasks, or adaptive blocks to coordinate today's execution."
                    )
                }
            } else {
                items(items, key = { it.executionKey }) { item ->
                    val accent = when {
                        item.recoveryRestricted -> SystemColors.DangerCrimson
                        item.status == DailyExecutionStatus.COMPLETED -> SystemColors.VerifiedEmerald
                        item.hasConflict || item.status == DailyExecutionStatus.MISSED -> SystemColors.WarningAmber
                        item.status == DailyExecutionStatus.IN_PROGRESS -> SystemColors.ElectricCyan
                        else -> SystemColors.SystemBlue
                    }
                    HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = accent) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(item.title, style = MaterialTheme.typography.titleSmall, color = SystemColors.PureIce, modifier = Modifier.weight(1f))
                                Text(item.status.label.uppercase(), style = MaterialTheme.typography.labelMedium, color = accent)
                            }
                            Text(
                                text = "${item.formattedPlannedWindow()} • ${item.sourceModule} • ${item.durationMinutes} min",
                                style = MaterialTheme.typography.labelSmall,
                                color = SystemColors.MetallicSilver
                            )
                            if (item.recoveryRestricted) {
                                Text(
                                    text = "RECOVERY SAFETY GUARD: Physical exertion paused during active rest warning.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SystemColors.DangerCrimson
                                )
                            } else if (item.hasConflict) {
                                Text(
                                    text = "Time conflict detected — reschedule to an open window.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SystemColors.WarningAmber
                                )
                            }
                            if (!item.status.isTerminal && !item.recoveryRestricted) {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (item.status != DailyExecutionStatus.IN_PROGRESS) {
                                        OutlinedButton(
                                            onClick = { onUpdateStatus(item, DailyExecutionStatus.IN_PROGRESS, "", null) },
                                            modifier = Modifier.testTag("exec_start_${item.executionKey}"),
                                            border = BorderStroke(1.dp, SystemColors.ElectricCyan)
                                        ) {
                                            Text("START", style = MaterialTheme.typography.labelMedium, color = SystemColors.ElectricCyan)
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = { onUpdateStatus(item, DailyExecutionStatus.PAUSED, "", null) },
                                            modifier = Modifier.testTag("exec_pause_${item.executionKey}"),
                                            border = BorderStroke(1.dp, SystemColors.WarningAmber)
                                        ) {
                                            Text("PAUSE", style = MaterialTheme.typography.labelMedium, color = SystemColors.WarningAmber)
                                        }
                                    }
                                    Button(
                                        onClick = {
                                            evidenceTargetKey = item.executionKey
                                            evidenceInput = "SESSION_CONFIRMED"
                                        },
                                        modifier = Modifier.testTag("exec_complete_${item.executionKey}"),
                                        colors = ButtonDefaults.buttonColors(containerColor = SystemColors.VerifiedEmerald, contentColor = SystemColors.ObsidianBlack)
                                    ) {
                                        Text("COMPLETE W/ PROOF", style = MaterialTheme.typography.labelMedium)
                                    }
                                    OutlinedButton(
                                        onClick = { onLoadRescheduleSlots(item, state.timeline.items) },
                                        modifier = Modifier.testTag("exec_reschedule_${item.executionKey}"),
                                        border = BorderStroke(1.dp, SystemColors.ArcaneViolet)
                                    ) {
                                        Text("RESCHEDULE", style = MaterialTheme.typography.labelMedium, color = SystemColors.ArcaneViolet)
                                    }
                                }
                            }
                            if (state.rescheduleTargetKey == item.executionKey && state.rescheduleSlots.isNotEmpty()) {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    state.rescheduleSlots.take(3).forEach { slot ->
                                        OutlinedButton(
                                            onClick = {
                                                onUpdateStatus(item, DailyExecutionStatus.SCHEDULED, "", slot.startMinuteOfDay)
                                            },
                                            border = BorderStroke(1.dp, SystemColors.ElectricCyan)
                                        ) {
                                            Text(slot.formattedWindow(), style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                    TextButton(onClick = onClearReschedule) { Text("CANCEL") }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (evidenceTarget != null) {
            AlertDialog(
                onDismissRequest = { evidenceTargetKey = null },
                containerColor = SystemColors.DeepNavy,
                title = { Text("Completion Evidence Required", color = SystemColors.PureIce) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Elapsed time alone never completes an activity. Provide a verification reference for '${evidenceTarget.title}'.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SystemColors.MetallicSilver
                        )
                        OutlinedTextField(
                            value = evidenceInput,
                            onValueChange = { evidenceInput = it },
                            label = { Text("Evidence Reference / Session Note") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("evidence_reference_input")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onUpdateStatus(evidenceTarget, DailyExecutionStatus.COMPLETED, evidenceInput, null)
                            evidenceTargetKey = null
                        },
                        enabled = evidenceInput.isNotBlank(),
                        modifier = Modifier.testTag("confirm_evidence_complete_btn")
                    ) {
                        Text("VERIFY & COMPLETE")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { evidenceTargetKey = null }) { Text("CANCEL") }
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SystemEventHistoryScreen(
    uiState: SystemControlUiState,
    onRetry: () -> Unit,
    onClearFeedback: () -> Unit,
    onRetryEvent: (String) -> Unit
) {
    var statusFilter by rememberSaveable { mutableStateOf("ALL") }

    SystemStateScaffold(
        uiState = uiState,
        onRetry = onRetry,
        feedbackMessage = (uiState as? SystemControlUiState.Ready)?.feedbackMessage,
        onClearFeedback = onClearFeedback
    ) { state ->
        val filtered = state.events.filter { ev ->
            statusFilter == "ALL" || ev.processingStatus.name == statusFilter
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 760.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SystemSectionTitle(
                    title = "System Event History",
                    subtitle = "Total ${state.eventDiagnostics.totalCount} • Processed ${state.eventDiagnostics.processedCount} • Dedup ${state.eventDiagnostics.deduplicatedCount} • Errors ${state.eventDiagnostics.failedCount + state.eventDiagnostics.deadLetterCount}"
                )
            }
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ALL", "PROCESSED", "DEDUPLICATED", "FAILED", "DEAD_LETTER").forEach { status ->
                        FilterChip(
                            selected = statusFilter == status,
                            onClick = { statusFilter = status },
                            label = { Text(status) },
                            modifier = Modifier.testTag("event_filter_${status.lowercase()}"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SystemColors.ElectricCyan.copy(alpha = 0.2f),
                                selectedLabelColor = SystemColors.ElectricCyan
                            )
                        )
                    }
                }
            }
            if (filtered.isEmpty()) {
                item { SystemEmptyState("No Matching System Events", "Events across Physical, Academic, Life, RPG & Sync appear here automatically.") }
            } else {
                items(filtered, key = { it.eventId }) { ev ->
                    val accent = when (ev.processingStatus) {
                        EventProcessingStatus.PROCESSED -> SystemColors.VerifiedEmerald
                        EventProcessingStatus.DEDUPLICATED -> SystemColors.MutedSilver
                        EventProcessingStatus.FAILED, EventProcessingStatus.DEAD_LETTER -> SystemColors.DangerCrimson
                        else -> SystemColors.ElectricCyan
                    }
                    HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = accent) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(ev.eventType.label, style = MaterialTheme.typography.titleSmall, color = SystemColors.PureIce)
                                Text(ev.processingStatus.label.uppercase(), style = MaterialTheme.typography.labelSmall, color = accent)
                            }
                            Text("${ev.sourceModule} • Entity: ${ev.relatedEntityId.ifBlank { "-" }} • ${formatEpochTimestamp(ev.timestampEpochMs)}", style = MaterialTheme.typography.labelSmall, color = SystemColors.MutedSilver)
                            if (!ev.errorMessage.isNullOrBlank()) {
                                Text(ev.errorMessage, style = MaterialTheme.typography.bodySmall, color = SystemColors.DangerCrimson)
                            }
                            if (ev.processingStatus == EventProcessingStatus.FAILED || ev.processingStatus == EventProcessingStatus.DEAD_LETTER) {
                                OutlinedButton(
                                    onClick = { onRetryEvent(ev.eventId) },
                                    modifier = Modifier.testTag("retry_event_${ev.eventId}"),
                                    border = BorderStroke(1.dp, SystemColors.DangerCrimson)
                                ) {
                                    Text("RETRY DISPATCH", style = MaterialTheme.typography.labelMedium, color = SystemColors.DangerCrimson)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DataIntegrityDashboardScreen(
    uiState: SystemControlUiState,
    onRetry: () -> Unit,
    onClearFeedback: () -> Unit,
    onRunScan: () -> Unit,
    onRepairFinding: (String, Boolean) -> Unit,
    onDismissFinding: (String) -> Unit
) {
    var destructiveConfirmTarget by rememberSaveable { mutableStateOf<String?>(null) }

    SystemStateScaffold(
        uiState = uiState,
        onRetry = onRetry,
        feedbackMessage = (uiState as? SystemControlUiState.Ready)?.feedbackMessage,
        onClearFeedback = onClearFeedback
    ) { state ->
        val confirmFinding: SystemIntegrityFinding? = state.findings.firstOrNull { it.findingUuid == destructiveConfirmTarget }

        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 760.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SystemSectionTitle(
                    title = "Data Integrity Dashboard",
                    subtitle = "${state.findings.count { it.status == IntegrityFindingStatus.OPEN }} open anomalies • Non-destructive by default",
                    trailing = {
                        Button(
                            onClick = onRunScan,
                            enabled = !state.isBusy,
                            modifier = Modifier.testTag("run_integrity_scan_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = SystemColors.ElectricCyan, contentColor = SystemColors.ObsidianBlack)
                        ) {
                            Text("SCAN NOW", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                )
            }
            if (state.findings.isEmpty()) {
                item {
                    SystemEmptyState(
                        title = "All Module Graphs Verified",
                        description = "Player progression bounds, foreign-key references, RPG audit logs, and sync queues show zero open inconsistencies."
                    )
                }
            } else {
                items(state.findings, key = { it.findingUuid }) { finding ->
                    val accent = when {
                        finding.status != IntegrityFindingStatus.OPEN -> SystemColors.MutedSilver
                        finding.severity == IntegritySeverity.CRITICAL -> SystemColors.DangerCrimson
                        finding.severity == IntegritySeverity.WARNING -> SystemColors.WarningAmber
                        else -> SystemColors.ElectricCyan
                    }
                    HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = accent) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(finding.title, style = MaterialTheme.typography.titleSmall, color = SystemColors.PureIce, modifier = Modifier.weight(1f))
                                Text("${finding.severity.label.uppercase()} • ${finding.status.label}", style = MaterialTheme.typography.labelSmall, color = accent)
                            }
                            Text(finding.description, style = MaterialTheme.typography.bodySmall, color = SystemColors.MetallicSilver)
                            Text("Proposal: ${finding.repairProposal}", style = MaterialTheme.typography.labelSmall, color = SystemColors.ElectricCyan)
                            if (finding.status == IntegrityFindingStatus.OPEN) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            if (finding.isDestructiveRepair) {
                                                destructiveConfirmTarget = finding.findingUuid
                                            } else {
                                                onRepairFinding(finding.findingUuid, false)
                                            }
                                        },
                                        modifier = Modifier.testTag("repair_finding_${finding.findingUuid}"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (finding.isDestructiveRepair) SystemColors.WarningAmber else SystemColors.VerifiedEmerald,
                                            contentColor = SystemColors.ObsidianBlack
                                        )
                                    ) {
                                        Text(if (finding.isDestructiveRepair) "CONFIRM REPAIR" else "SAFE REPAIR", style = MaterialTheme.typography.labelMedium)
                                    }
                                    OutlinedButton(onClick = { onDismissFinding(finding.findingUuid) }) {
                                        Text("DISMISS", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (confirmFinding != null) {
            AlertDialog(
                onDismissRequest = { destructiveConfirmTarget = null },
                containerColor = SystemColors.DeepNavy,
                title = { Text("Confirm Structural Cleanup", color = SystemColors.PureIce) },
                text = {
                    Text(
                        text = "${confirmFinding.title}: ${confirmFinding.repairProposal} Existing player XP, levels, and active records remain untouched.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SystemColors.MetallicSilver
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onRepairFinding(confirmFinding.findingUuid, true)
                            destructiveConfirmTarget = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SystemColors.WarningAmber, contentColor = SystemColors.ObsidianBlack)
                    ) {
                        Text("APPLY CLEANUP")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { destructiveConfirmTarget = null }) { Text("CANCEL") }
                }
            )
        }
    }
}

@Composable
fun SystemHealthMonitorScreen(
    uiState: SystemControlUiState,
    onRetry: () -> Unit,
    onClearFeedback: () -> Unit,
    onRunFullRecovery: () -> Unit
) {
    SystemStateScaffold(
        uiState = uiState,
        onRetry = onRetry,
        feedbackMessage = (uiState as? SystemControlUiState.Ready)?.feedbackMessage,
        onClearFeedback = onClearFeedback
    ) { state ->
        val overallAccent = healthAccentColor(state.healthReport.overallStatus)
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 760.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SystemSectionTitle(
                    title = "System Health Monitor",
                    subtitle = "Real operational states — no fabricated percentages"
                )
            }
            item {
                HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = overallAccent) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("AGGREGATE HEALTH: ${state.healthReport.overallStatus.label.uppercase()}", style = MaterialTheme.typography.titleMedium, color = overallAccent)
                            Text("Evaluated ${formatEpochTimestamp(state.healthReport.evaluatedAtEpochMs)}", style = MaterialTheme.typography.labelSmall, color = SystemColors.MutedSilver)
                        }
                        OutlinedButton(onClick = onRetry, modifier = Modifier.testTag("health_rescan_btn"), border = BorderStroke(1.dp, overallAccent)) {
                            Text("RE-EVALUATE", style = MaterialTheme.typography.labelMedium, color = overallAccent)
                        }
                    }
                }
            }
            items(state.healthReport.components, key = { it.component }) { comp ->
                val accent = healthAccentColor(comp.status)
                HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = accent) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(comp.component.uppercase(), style = MaterialTheme.typography.titleSmall, color = SystemColors.PureIce)
                            Text(comp.status.label.uppercase(), style = MaterialTheme.typography.labelMedium, color = accent)
                        }
                        Text(comp.evidence, style = MaterialTheme.typography.bodySmall, color = SystemColors.MetallicSilver)
                        Text("Action: ${comp.recommendedAction}", style = MaterialTheme.typography.labelSmall, color = SystemColors.ElectricCyan)
                    }
                }
            }
            item {
                Button(
                    onClick = onRunFullRecovery,
                    enabled = !state.isBusy,
                    modifier = Modifier.fillMaxWidth().testTag("health_execute_recovery_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = SystemColors.ArcaneViolet, contentColor = SystemColors.PureIce)
                ) {
                    Text("RUN SYSTEM DIAGNOSTIC RECOVERY", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
