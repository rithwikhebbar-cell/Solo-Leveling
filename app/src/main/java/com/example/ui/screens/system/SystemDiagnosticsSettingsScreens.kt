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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.engine.RealDeviceVerificationCatalog
import com.example.domain.model.SystemControlPreferences
import com.example.domain.model.SystemExperiencePreferences
import com.example.domain.model.SystemNotificationPreferences
import com.example.ui.components.HolographicPanel
import com.example.ui.components.SystemEmptyState
import com.example.ui.components.SystemSectionTitle
import com.example.ui.theme.SystemColors
import com.example.ui.viewmodel.SystemControlUiState

@Composable
fun PerformanceDiagnosticsScreen(
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
        val perf = state.performanceMetrics
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 760.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SystemSectionTitle(
                    title = "Performance Diagnostics",
                    subtitle = "DB indices, telemetry fingerprinting & sweep latency",
                    trailing = {
                        OutlinedButton(onClick = onRetry, modifier = Modifier.testTag("perf_rebenchmark_btn"), border = BorderStroke(1.dp, SystemColors.ElectricCyan)) {
                            Text("BENCHMARK", style = MaterialTheme.typography.labelMedium, color = SystemColors.ElectricCyan)
                        }
                    }
                )
            }
            item {
                HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.ElectricCyan) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        MetricLine("Room Schema Version", "v${perf.databaseVersion}")
                        MetricLine("activity_logs.timestampEpochMs Index", if (perf.indexedActivityLogsReady) "ACTIVE" else "MISSING")
                        MetricLine("Last Health/Perf Sweep", "${perf.lastSweepDurationMs} ms")
                        MetricLine("Stored System Events", "${perf.totalStoredEvents}")
                        MetricLine("Pending Sync Queue", "${perf.pendingSyncQueueSize}")
                        MetricLine("Active Automation Workers", "${perf.activeAutomationsCount}")
                        MetricLine("Daily Execution Overrides", "${perf.totalDailyExecutionItems}")
                        Text(
                            text = "Profile Fingerprint Cache: ${perf.cachedProfileFingerprint.take(28)}${if (perf.cachedProfileFingerprint.length > 28) "…" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SystemColors.MutedSilver
                        )
                    }
                }
            }
            items(perf.optimizationNotes) { note ->
                HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.VerifiedEmerald, contentPadding = 12.dp) {
                    Text("• $note", style = MaterialTheme.typography.bodySmall, color = SystemColors.PureIce)
                }
            }
        }
    }
}

@Composable
private fun MetricLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = SystemColors.MetallicSilver)
        Text(value, style = MaterialTheme.typography.labelLarge, color = SystemColors.ElectricCyan)
    }
}

@Composable
fun ErrorRecoveryCenterScreen(
    uiState: SystemControlUiState,
    onRetry: () -> Unit,
    onClearFeedback: () -> Unit,
    onExecuteFullRecovery: () -> Unit
) {
    SystemStateScaffold(
        uiState = uiState,
        onRetry = onRetry,
        feedbackMessage = (uiState as? SystemControlUiState.Ready)?.feedbackMessage,
        onClearFeedback = onClearFeedback
    ) { state ->
        val errCount = state.eventDiagnostics.failedCount + state.eventDiagnostics.deadLetterCount
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 760.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SystemSectionTitle(
                    title = "Error & Recovery Center",
                    subtitle = "Safe crash recovery, dead-letter replay & idempotent cycle repair"
                )
            }
            item {
                HolographicPanel(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = if (errCount > 0) SystemColors.DangerCrimson else SystemColors.VerifiedEmerald
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "$errCount FAILED / DEAD-LETTER EVENTS • ${state.eventDiagnostics.pendingCount} PENDING",
                            style = MaterialTheme.typography.titleSmall,
                            color = SystemColors.PureIce
                        )
                        Text(
                            text = "Full recovery resets stuck PROCESSING events to PENDING, replays recoverable items idempotently, verifies daily cycle catch-up, and re-runs cross-module integrity scan.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SystemColors.MetallicSilver
                        )
                        Button(
                            onClick = onExecuteFullRecovery,
                            enabled = !state.isBusy,
                            modifier = Modifier.fillMaxWidth().testTag("execute_full_recovery_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = SystemColors.ElectricCyan, contentColor = SystemColors.ObsidianBlack)
                        ) {
                            Text("EXECUTE FULL SYSTEM RECOVERY", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
            item {
                SystemSectionTitle(title = "Recovery Audit Trail", subtitle = "${state.recoveryOperations.size} recorded operations")
            }
            if (state.recoveryOperations.isEmpty()) {
                item { SystemEmptyState("No Recovery Operations Needed", "Startup crash recovery & integrity repair actions appear here.") }
            } else {
                items(state.recoveryOperations, key = { it.operationUuid }) { op ->
                    HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.ArcaneViolet) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(op.operationType, style = MaterialTheme.typography.titleSmall, color = SystemColors.PureIce)
                                Text(op.status, style = MaterialTheme.typography.labelSmall, color = SystemColors.VerifiedEmerald)
                            }
                            Text(op.details, style = MaterialTheme.typography.bodySmall, color = SystemColors.MetallicSilver)
                            Text("${op.reason} • ${formatEpochTimestamp(op.createdAtEpochMs)}", style = MaterialTheme.typography.labelSmall, color = SystemColors.MutedSilver)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationManagementScreen(
    uiState: SystemControlUiState,
    onRetry: () -> Unit,
    onClearFeedback: () -> Unit,
    onUpdateNotificationPrefs: (SystemNotificationPreferences) -> Unit,
    onMarkRead: (Long) -> Unit,
    onClearRead: () -> Unit
) {
    SystemStateScaffold(
        uiState = uiState,
        onRetry = onRetry,
        feedbackMessage = (uiState as? SystemControlUiState.Ready)?.feedbackMessage,
        onClearFeedback = onClearFeedback
    ) { state ->
        val prefs = state.notificationPrefs
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 760.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SystemSectionTitle(
                    title = "Notification Management",
                    subtitle = "Quiet hours (${prefs.quietHoursStartHour}:00–${prefs.quietHoursEndHour}:00) & quota (${prefs.maxNotificationsPerDay}/day)",
                    trailing = {
                        if (state.notifications.any { it.isRead }) {
                            TextButton(onClick = onClearRead, modifier = Modifier.testTag("notif_clear_read_btn")) {
                                Text("CLEAR READ", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                )
            }
            item {
                HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.ElectricCyan) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PreferenceToggleRow("Master Notifications", prefs.notificationsEnabled, "toggle_notif_master") {
                            onUpdateNotificationPrefs(prefs.copy(notificationsEnabled = it))
                        }
                        PreferenceToggleRow("Daily Quests & Planning", prefs.enableDailyQuestReminders, "toggle_notif_daily_quests") {
                            onUpdateNotificationPrefs(prefs.copy(enableDailyQuestReminders = it, enableDailyPlanningReminders = it))
                        }
                        PreferenceToggleRow("Academic Revisions & Exams", prefs.enableOverdueRevisions, "toggle_notif_academic") {
                            onUpdateNotificationPrefs(prefs.copy(enableOverdueRevisions = it, enableUpcomingExams = it))
                        }
                        PreferenceToggleRow("Recovery Safety Alerts", prefs.enableRecoveryWarnings, "toggle_notif_recovery") {
                            onUpdateNotificationPrefs(prefs.copy(enableRecoveryWarnings = it))
                        }
                        PreferenceToggleRow("Habits, Routines & Tasks", prefs.enableHabitReminders, "toggle_notif_life") {
                            onUpdateNotificationPrefs(prefs.copy(enableHabitReminders = it, enableRoutineReminders = it, enableTaskDeadlines = it, enableOverdueTasks = it))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Quiet Start: ${prefs.quietHoursStartHour}:00", style = MaterialTheme.typography.bodySmall, color = SystemColors.PureIce)
                            Row {
                                OutlinedButton(onClick = { onUpdateNotificationPrefs(prefs.copy(quietHoursStartHour = (prefs.quietHoursStartHour + 23) % 24)) }) { Text("-1h") }
                                Spacer(Modifier.width(6.dp))
                                OutlinedButton(onClick = { onUpdateNotificationPrefs(prefs.copy(quietHoursStartHour = (prefs.quietHoursStartHour + 1) % 24)) }) { Text("+1h") }
                            }
                        }
                    }
                }
            }
            item {
                SystemSectionTitle(title = "Recent Alerts", subtitle = "${state.notifications.count { !it.isRead }} unread")
            }
            if (state.notifications.isEmpty()) {
                item { SystemEmptyState("Inbox Clear", "No stored notifications at this time.") }
            } else {
                items(state.notifications.take(20), key = { it.id }) { alert ->
                    HolographicPanel(
                        modifier = Modifier.fillMaxWidth(),
                        accentColor = if (alert.isRead) SystemColors.MutedSilver else SystemColors.ElectricCyan
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(alert.title, style = MaterialTheme.typography.titleSmall, color = SystemColors.PureIce)
                                Text(alert.message, style = MaterialTheme.typography.bodySmall, color = SystemColors.MetallicSilver)
                            }
                            if (!alert.isRead) {
                                TextButton(onClick = { onMarkRead(alert.id) }) {
                                    Text("READ", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SystemControlSettingsScreen(
    uiState: SystemControlUiState,
    onRetry: () -> Unit,
    onClearFeedback: () -> Unit,
    onUpdateSystemPreferences: (SystemControlPreferences) -> Unit,
    experiencePrefs: SystemExperiencePreferences = SystemExperiencePreferences(),
    onUpdateExperiencePrefs: ((SystemExperiencePreferences) -> SystemExperiencePreferences) -> Unit = {},
    onReplayAwakening: () -> Unit = {},
    onOpenSystemVoiceSettings: () -> Unit = {},
    onOpenVoiceConsole: () -> Unit = {},
    onOpenWidgetHub: () -> Unit = {},
    onOpenCelebrationVault: () -> Unit = {}
) {
    var showChecklist by rememberSaveable { mutableStateOf(false) }

    SystemStateScaffold(
        uiState = uiState,
        onRetry = onRetry,
        feedbackMessage = (uiState as? SystemControlUiState.Ready)?.feedbackMessage,
        onClearFeedback = onClearFeedback
    ) { state ->
        val prefs = state.systemPrefs
        val tzOptions = listOf(java.time.ZoneId.systemDefault().id, "UTC", "America/New_York", "Europe/London", "Asia/Tokyo").distinct()

        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 760.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SystemSectionTitle(
                    title = "System Control Settings",
                    subtitle = "Event bus, background workers, caching & device verification plan"
                )
            }
            item {
                HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.ElectricCyan) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PreferenceToggleRow("Auto Progression Events", prefs.enableAutoProgressionEvents, "toggle_sys_events") {
                            onUpdateSystemPreferences(prefs.copy(enableAutoProgressionEvents = it))
                        }
                        PreferenceToggleRow("Auto Intelligence Refresh", prefs.enableAutoIntelligenceRefresh, "toggle_sys_intel") {
                            onUpdateSystemPreferences(prefs.copy(enableAutoIntelligenceRefresh = it))
                        }
                        PreferenceToggleRow("Daily Cycle Automation", prefs.enableDailyCycleAutomation, "toggle_sys_cycle") {
                            onUpdateSystemPreferences(prefs.copy(enableDailyCycleAutomation = it))
                        }
                        PreferenceToggleRow("Cloud Auto-Sync", prefs.enableCloudAutoSync, "toggle_sys_sync") {
                            onUpdateSystemPreferences(prefs.copy(enableCloudAutoSync = it))
                        }
                        PreferenceToggleRow("Telemetry Fingerprint Cache", prefs.enablePerformanceCaching, "toggle_sys_cache") {
                            onUpdateSystemPreferences(prefs.copy(enablePerformanceCaching = it))
                        }
                        PreferenceToggleRow("Background Notifications", prefs.enableBackgroundNotifications, "toggle_sys_notifs") {
                            onUpdateSystemPreferences(prefs.copy(enableBackgroundNotifications = it))
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Preferred Cycle Timezone", style = MaterialTheme.typography.labelMedium, color = SystemColors.MetallicSilver)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            tzOptions.forEach { tz ->
                                FilterChip(
                                    selected = prefs.preferredTimezoneId == tz,
                                    onClick = { onUpdateSystemPreferences(prefs.copy(preferredTimezoneId = tz)) },
                                    label = { Text(tz) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SystemColors.ElectricCyan.copy(alpha = 0.2f),
                                        selectedLabelColor = SystemColors.ElectricCyan
                                    )
                                )
                            }
                        }
                    }
                }
            }
            item {
                HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.ArcaneViolet) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "EXPERIENCE, MOTION, SOUND & ACCESSIBILITY",
                            style = MaterialTheme.typography.titleSmall,
                            color = SystemColors.ArcaneViolet
                        )
                        PreferenceToggleRow("Holographic Animations", experiencePrefs.animationsEnabled, "toggle_exp_animations") { v ->
                            onUpdateExperiencePrefs { it.copy(animationsEnabled = v) }
                        }
                        PreferenceToggleRow("Reduced Motion Mode", experiencePrefs.reducedMotionEnabled, "toggle_exp_reduced_motion") { v ->
                            onUpdateExperiencePrefs { it.copy(reducedMotionEnabled = v) }
                        }
                        PreferenceToggleRow("System Chime Effects", experiencePrefs.soundEffectsEnabled, "toggle_exp_sound") { v ->
                            onUpdateExperiencePrefs { it.copy(soundEffectsEnabled = v) }
                        }
                        PreferenceToggleRow("Haptic Feedback", experiencePrefs.hapticFeedbackEnabled, "toggle_exp_haptics") { v ->
                            onUpdateExperiencePrefs { it.copy(hapticFeedbackEnabled = v) }
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onOpenSystemVoiceSettings,
                                modifier = Modifier.testTag("control_settings_voice_btn"),
                                border = BorderStroke(1.dp, SystemColors.ElectricCyan)
                            ) {
                                Text("SYSTEM VOICE", color = SystemColors.ElectricCyan, style = MaterialTheme.typography.labelMedium)
                            }
                            OutlinedButton(
                                onClick = onOpenVoiceConsole,
                                modifier = Modifier.testTag("control_settings_mic_btn"),
                                border = BorderStroke(1.dp, SystemColors.ElectricCyan)
                            ) {
                                Text("VOICE CONSOLE", color = SystemColors.ElectricCyan, style = MaterialTheme.typography.labelMedium)
                            }
                            OutlinedButton(
                                onClick = onOpenWidgetHub,
                                modifier = Modifier.testTag("control_settings_widgets_btn"),
                                border = BorderStroke(1.dp, SystemColors.VerifiedEmerald)
                            ) {
                                Text("WIDGET HUB", color = SystemColors.VerifiedEmerald, style = MaterialTheme.typography.labelMedium)
                            }
                            OutlinedButton(
                                onClick = onOpenCelebrationVault,
                                modifier = Modifier.testTag("control_settings_celebrations_btn"),
                                border = BorderStroke(1.dp, SystemColors.MonarchGold)
                            ) {
                                Text("CELEBRATIONS", color = SystemColors.MonarchGold, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        OutlinedButton(
                            onClick = onReplayAwakening,
                            modifier = Modifier.fillMaxWidth().testTag("control_settings_replay_awakening"),
                            border = BorderStroke(1.dp, SystemColors.MonarchGold)
                        ) {
                            Text("REPLAY AWAKENING SEQUENCE", color = SystemColors.MonarchGold, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = { showChecklist = !showChecklist },
                    modifier = Modifier.fillMaxWidth().testTag("toggle_device_verification_plan_btn"),
                    border = BorderStroke(1.dp, SystemColors.ArcaneViolet)
                ) {
                    Text(
                        text = if (showChecklist) "HIDE REAL DEVICE VERIFICATION PLAN (${RealDeviceVerificationCatalog.items.size})" else "VIEW REAL DEVICE VERIFICATION PLAN (${RealDeviceVerificationCatalog.items.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = SystemColors.ArcaneViolet
                    )
                }
            }
            if (showChecklist) {
                items(RealDeviceVerificationCatalog.items, key = { it.id }) { check ->
                    HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.ArcaneViolet) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${check.id}. ${check.title}", style = MaterialTheme.typography.titleSmall, color = SystemColors.PureIce)
                            Text("Environment: ${check.environmentRequired}", style = MaterialTheme.typography.labelSmall, color = SystemColors.WarningAmber)
                            Text(check.verificationSteps, style = MaterialTheme.typography.bodySmall, color = SystemColors.MetallicSilver)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreferenceToggleRow(
    label: String,
    checked: Boolean,
    tag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = SystemColors.PureIce, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag),
            colors = SwitchDefaults.colors(checkedThumbColor = SystemColors.ObsidianBlack, checkedTrackColor = SystemColors.ElectricCyan)
        )
    }
}
