package com.example.ui.screens.system

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoMode
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CalendarViewDay
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ReportProblem
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.AutomationExecutionStatus
import com.example.domain.model.SystemHealthLevel
import com.example.ui.components.HolographicPanel
import com.example.ui.components.SystemEmptyState
import com.example.ui.components.SystemErrorState
import com.example.ui.components.SystemLoadingView
import com.example.ui.components.SystemSectionTitle
import com.example.ui.theme.SystemColors
import com.example.ui.viewmodel.SystemControlUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun healthAccentColor(level: SystemHealthLevel): Color = when (level) {
    SystemHealthLevel.HEALTHY -> SystemColors.VerifiedEmerald
    SystemHealthLevel.WARNING -> SystemColors.WarningAmber
    SystemHealthLevel.DEGRADED -> SystemColors.ArcaneViolet
    SystemHealthLevel.CRITICAL -> SystemColors.DangerCrimson
}

internal fun formatEpochTimestamp(epochMs: Long?): String {
    if (epochMs == null || epochMs <= 0L) return "Never"
    return SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(epochMs))
}

@Composable
internal fun SystemStateScaffold(
    uiState: SystemControlUiState,
    onRetry: () -> Unit,
    feedbackMessage: String?,
    onClearFeedback: () -> Unit,
    content: @Composable (SystemControlUiState.Ready) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SystemColors.ObsidianBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        when (uiState) {
            is SystemControlUiState.Loading -> SystemLoadingView("INITIALIZING SYSTEM CONTROL CENTER...")
            is SystemControlUiState.Error -> Box(Modifier.padding(20.dp)) {
                SystemErrorState(message = uiState.message, onRetry = onRetry)
            }
            is SystemControlUiState.Ready -> Column(modifier = Modifier.fillMaxSize()) {
                if (!feedbackMessage.isNullOrBlank()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        color = SystemColors.PanelSurfaceElevated,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, SystemColors.ElectricCyan.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = feedbackMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = SystemColors.PureIce,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(
                                onClick = onClearFeedback,
                                modifier = Modifier.testTag("system_feedback_dismiss")
                            ) {
                                Text("OK", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    content(uiState)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SystemOverviewScreen(
    uiState: SystemControlUiState,
    onRetry: () -> Unit,
    onClearFeedback: () -> Unit,
    onOpenModulesStatus: () -> Unit,
    onOpenAutomations: () -> Unit,
    onOpenAutomationHistory: () -> Unit,
    onOpenExecutionTimeline: () -> Unit,
    onOpenEventHistory: () -> Unit,
    onOpenDataIntegrity: () -> Unit,
    onOpenHealthMonitor: () -> Unit,
    onOpenPerformance: () -> Unit,
    onOpenRecoveryCenter: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAiCompanion: () -> Unit = {},
    onOpenVoiceConsole: () -> Unit = {},
    onOpenSystemVoiceSettings: () -> Unit = {},
    onOpenWidgetHub: () -> Unit = {},
    onOpenCelebrationVault: () -> Unit = {}
) {
    SystemStateScaffold(
        uiState = uiState,
        onRetry = onRetry,
        feedbackMessage = (uiState as? SystemControlUiState.Ready)?.feedbackMessage,
        onClearFeedback = onClearFeedback
    ) { state ->
        val accent = healthAccentColor(state.healthReport.overallStatus)
        val links = listOf(
            Triple("AI System Companion", "Holographic advisory Q&A, consent guard & speech", Icons.Rounded.AutoAwesome to onOpenAiCompanion),
            Triple("Voice Command Console", "Tap-to-speak allowlisted navigation & fallback", Icons.Rounded.Mic to onOpenVoiceConsole),
            Triple("System Voice & Alerts", "TTS voice controls, announcement categories & feed", Icons.Rounded.Psychology to onOpenSystemVoiceSettings),
            Triple("Home-Screen Widgets", "4 Jetpack Glance widgets & live telemetry snapshots", Icons.Rounded.Widgets to onOpenWidgetHub),
            Triple("Celebration Vault", "Replay verified Level-Up & Rank Promotion badges", Icons.Rounded.EmojiEvents to onOpenCelebrationVault),
            Triple("Module Status", "${state.moduleStatuses.size} subsystems wired", Icons.Rounded.DeveloperBoard to onOpenModulesStatus),
            Triple("Daily Execution", "${state.timeline.completedCount}/${state.timeline.items.size} items today", Icons.Rounded.CalendarViewDay to onOpenExecutionTimeline),
            Triple("Automations", "${state.jobs.count { it.isEnabled }}/${state.jobs.size} workers active", Icons.Rounded.AutoMode to onOpenAutomations),
            Triple("Automation History", "${state.executionLogs.size} logs • ${state.dailyCycles.size} cycles", Icons.Rounded.History to onOpenAutomationHistory),
            Triple("Event History", "${state.eventDiagnostics.processedCount} processed • ${state.eventDiagnostics.failedCount + state.eventDiagnostics.deadLetterCount} errs", Icons.Rounded.Bolt to onOpenEventHistory),
            Triple("Data Integrity", "${state.findings.count { it.status.name == "OPEN" }} open findings", Icons.Rounded.Shield to onOpenDataIntegrity),
            Triple("Health Monitor", "${state.healthReport.overallStatus.label} (${state.healthReport.components.size} checks)", Icons.Rounded.HealthAndSafety to onOpenHealthMonitor),
            Triple("Performance", "DB v${state.performanceMetrics.databaseVersion} • ${state.performanceMetrics.lastSweepDurationMs}ms sweep", Icons.Rounded.Speed to onOpenPerformance),
            Triple("Recovery Center", "${state.recoveryOperations.size} recovery records", Icons.Rounded.Build to onOpenRecoveryCenter),
            Triple("Notifications", "${state.notifications.count { !it.isRead }} unread alerts", Icons.Rounded.Notifications to onOpenNotifications),
            Triple("System Settings", "Timezone ${state.systemPrefs.preferredTimezoneId} & experience prefs", Icons.Rounded.Settings to onOpenSettings)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SystemSectionTitle(
                    title = "System Control Center",
                    subtitle = "Architecture coordination, automation, integrity & health",
                    trailing = {
                        IconButtonRefresh(onClick = onRetry, testTag = "system_refresh_button")
                    }
                )
            }
            item {
                HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = accent) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("OVERALL SYSTEM STATUS", style = MaterialTheme.typography.labelMedium, color = SystemColors.MetallicSilver)
                            Surface(
                                color = accent.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, accent.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = state.healthReport.overallStatus.label.uppercase(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = accent,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Text(
                            text = "${state.timeline.activeOrReadyCount} scheduled blocks pending • ${state.eventDiagnostics.pendingCount} queued events • Room v${state.performanceMetrics.databaseVersion}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SystemColors.PureIce
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onOpenExecutionTimeline,
                                modifier = Modifier.testTag("overview_open_timeline_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = SystemColors.ElectricCyan, contentColor = SystemColors.ObsidianBlack)
                            ) {
                                Text("TODAY'S TIMELINE", style = MaterialTheme.typography.labelMedium)
                            }
                            OutlinedButton(
                                onClick = onOpenHealthMonitor,
                                modifier = Modifier.testTag("overview_open_health_btn"),
                                border = BorderStroke(1.dp, accent)
                            ) {
                                Text("INSPECT HEALTH", style = MaterialTheme.typography.labelMedium, color = accent)
                            }
                        }
                    }
                }
            }
            items(links) { (title, subtitle, pair) ->
                val (icon, onClick) = pair
                ControlLinkRow(title = title, subtitle = subtitle, icon = icon, accent = SystemColors.ElectricCyan, onClick = onClick)
            }
        }
    }
}

@Composable
private fun IconButtonRefresh(onClick: () -> Unit, testTag: String) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.testTag(testTag),
        border = BorderStroke(1.dp, SystemColors.ElectricCyan.copy(alpha = 0.6f)),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(Icons.Rounded.Refresh, contentDescription = "Refresh telemetry", tint = SystemColors.ElectricCyan, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text("SCAN", style = MaterialTheme.typography.labelMedium, color = SystemColors.ElectricCyan)
    }
}

@Composable
private fun ControlLinkRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit
) {
    HolographicPanel(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("control_link_${title.lowercase().replace(" ", "_")}"),
        accentColor = accent,
        contentPadding = 14.dp
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = title, tint = accent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title.uppercase(), style = MaterialTheme.typography.titleSmall, color = SystemColors.PureIce)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = SystemColors.MutedSilver)
            }
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Open $title", tint = SystemColors.MetallicSilver, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun ModuleIntegrationStatusScreen(
    uiState: SystemControlUiState,
    onRetry: () -> Unit,
    onClearFeedback: () -> Unit,
    onNavigateToModule: (String) -> Unit
) {
    SystemStateScaffold(
        uiState = uiState,
        onRetry = onRetry,
        feedbackMessage = (uiState as? SystemControlUiState.Ready)?.feedbackMessage,
        onClearFeedback = onClearFeedback
    ) { state ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 760.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SystemSectionTitle(
                    title = "Module Integration Status",
                    subtitle = "Live coordination across Player, Physical, Academic, Life, RPG, Cloud & Intelligence"
                )
            }
            items(state.moduleStatuses, key = { it.moduleId }) { mod ->
                val accent = healthAccentColor(mod.healthLevel)
                HolographicPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToModule(mod.routeKey) }
                        .testTag("module_status_${mod.moduleId.lowercase()}"),
                    accentColor = accent
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(mod.title.uppercase(), style = MaterialTheme.typography.titleSmall, color = SystemColors.PureIce)
                            Text(mod.healthLevel.label.uppercase(), style = MaterialTheme.typography.labelMedium, color = accent)
                        }
                        Text(mod.statusLabel, style = MaterialTheme.typography.labelSmall, color = SystemColors.ElectricCyan)
                        Text(mod.summary, style = MaterialTheme.typography.bodySmall, color = SystemColors.MetallicSilver)
                    }
                }
            }
        }
    }
}

@Composable
fun AutomationDashboardScreen(
    uiState: SystemControlUiState,
    onRetry: () -> Unit,
    onClearFeedback: () -> Unit,
    onToggleJob: (String, Boolean) -> Unit,
    onRunJobNow: (String) -> Unit,
    onRunDailyCycleNow: () -> Unit,
    onOpenHistory: () -> Unit
) {
    SystemStateScaffold(
        uiState = uiState,
        onRetry = onRetry,
        feedbackMessage = (uiState as? SystemControlUiState.Ready)?.feedbackMessage,
        onClearFeedback = onClearFeedback
    ) { state ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 760.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SystemSectionTitle(
                    title = "Automation Dashboard",
                    subtitle = "Idempotent WorkManager registry with user toggles & overlap locking",
                    trailing = {
                        TextButton(onClick = onOpenHistory, modifier = Modifier.testTag("open_automation_history_btn")) {
                            Text("HISTORY", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                )
            }
            item {
                Button(
                    onClick = onRunDailyCycleNow,
                    enabled = !state.isBusy,
                    modifier = Modifier.fillMaxWidth().testTag("run_daily_cycle_now_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = SystemColors.ArcaneViolet, contentColor = SystemColors.PureIce)
                ) {
                    Text("VERIFY & CATCH UP DAILY CYCLE", style = MaterialTheme.typography.labelLarge)
                }
            }
            items(state.jobs, key = { it.automationId }) { job ->
                val accent = when (job.currentStatus) {
                    AutomationExecutionStatus.FAILED -> SystemColors.DangerCrimson
                    AutomationExecutionStatus.RETRY_SCHEDULED -> SystemColors.WarningAmber
                    AutomationExecutionStatus.DISABLED -> SystemColors.MutedSilver
                    AutomationExecutionStatus.RUNNING -> SystemColors.ArcaneViolet
                    else -> SystemColors.ElectricCyan
                }
                HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = accent) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(job.name.uppercase(), style = MaterialTheme.typography.titleSmall, color = SystemColors.PureIce)
                                Text("${job.frequencyLabel} • ${job.triggerType.label}", style = MaterialTheme.typography.labelSmall, color = accent)
                            }
                            Switch(
                                checked = job.isEnabled,
                                onCheckedChange = { onToggleJob(job.automationId, it) },
                                modifier = Modifier.testTag("toggle_job_${job.automationId}"),
                                colors = SwitchDefaults.colors(checkedThumbColor = SystemColors.ObsidianBlack, checkedTrackColor = SystemColors.ElectricCyan)
                            )
                        }
                        Text(job.description, style = MaterialTheme.typography.bodySmall, color = SystemColors.MetallicSilver)
                        Text(
                            text = "Status: ${job.currentStatus.label} • Runs: ${job.executionCount} • Last: ${formatEpochTimestamp(job.lastExecutionEpochMs)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SystemColors.MutedSilver
                        )
                        if (!job.lastFailureReason.isNullOrBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.ReportProblem, contentDescription = "Failure", tint = SystemColors.DangerCrimson, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(job.lastFailureReason, style = MaterialTheme.typography.bodySmall, color = SystemColors.DangerCrimson)
                            }
                        }
                        OutlinedButton(
                            onClick = { onRunJobNow(job.automationId) },
                            enabled = !state.isBusy,
                            modifier = Modifier.testTag("run_job_${job.automationId}"),
                            border = BorderStroke(1.dp, accent)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("RUN NOW", style = MaterialTheme.typography.labelMedium, color = accent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AutomationHistoryScreen(
    uiState: SystemControlUiState,
    onRetry: () -> Unit,
    onClearFeedback: () -> Unit
) {
    SystemStateScaffold(
        uiState = uiState,
        onRetry = onRetry,
        feedbackMessage = (uiState as? SystemControlUiState.Ready)?.feedbackMessage,
        onClearFeedback = onClearFeedback
    ) { state ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 760.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SystemSectionTitle(title = "Daily Cycle History", subtitle = "${state.dailyCycles.size} idempotent daily reset records")
            }
            if (state.dailyCycles.isEmpty()) {
                item { SystemEmptyState("No Daily Cycles Recorded", "Cycle records appear cleanly on first daily boundary run.") }
            } else {
                items(state.dailyCycles.take(10), key = { it.epochDay }) { cycle ->
                    HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.VerifiedEmerald) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("EPOCH DAY ${cycle.epochDay} (${cycle.timezoneId})", style = MaterialTheme.typography.titleSmall, color = SystemColors.PureIce)
                            Text(
                                text = "Trigger: ${cycle.triggerSource} • Quests reset: ${cycle.dailyQuestsResetCount} • Catch-up gap: ${cycle.missedDaysCaughtUp}d",
                                style = MaterialTheme.typography.bodySmall,
                                color = SystemColors.MetallicSilver
                            )
                            Text(cycle.notes, style = MaterialTheme.typography.labelSmall, color = SystemColors.MutedSilver)
                        }
                    }
                }
            }
            item {
                Spacer(Modifier.height(6.dp))
                SystemSectionTitle(title = "Worker Execution Logs", subtitle = "${state.executionLogs.size} chronological runs")
            }
            if (state.executionLogs.isEmpty()) {
                item { SystemEmptyState("No Worker Runs Logged", "Trigger an automation job or wait for scheduled WorkManager runs.") }
            } else {
                items(state.executionLogs, key = { it.executionUuid }) { log ->
                    val accent = if (log.status == AutomationExecutionStatus.SUCCEEDED) SystemColors.ElectricCyan else SystemColors.DangerCrimson
                    HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = accent) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(log.automationId.uppercase(), style = MaterialTheme.typography.titleSmall, color = SystemColors.PureIce)
                                Text(log.status.label.uppercase(), style = MaterialTheme.typography.labelSmall, color = accent)
                            }
                            Text(log.summaryMessage, style = MaterialTheme.typography.bodySmall, color = SystemColors.MetallicSilver)
                            Text("${log.triggerSource} • ${formatEpochTimestamp(log.completedAtEpochMs)}", style = MaterialTheme.typography.labelSmall, color = SystemColors.MutedSilver)
                        }
                    }
                }
            }
        }
    }
}
