package com.example.ui.screens.cloud

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PendingActions
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.SyncState
import com.example.domain.model.SyncSummaryState
import com.example.ui.components.HolographicPanel
import com.example.ui.components.SystemEmptyState
import com.example.ui.components.SystemSectionTitle
import com.example.ui.theme.SystemColors
import com.example.ui.viewmodel.CloudSyncUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Reusable Sync Status Indicator Component (Sync UX #2)
@Composable
fun SyncStatusIndicatorCard(
    summary: SyncSummaryState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = when (summary.overallState) {
        SyncState.SYNCED -> SystemColors.VerifiedEmerald
        SyncState.UPLOADING, SyncState.DOWNLOADING -> SystemColors.ElectricCyan
        SyncState.PENDING_UPLOAD, SyncState.PENDING_DOWNLOAD -> SystemColors.SystemBlue
        SyncState.CONFLICT -> SystemColors.WarningAmber
        SyncState.FAILED -> SystemColors.DangerCrimson
        else -> SystemColors.MutedSilver
    }
    val icon = when (summary.overallState) {
        SyncState.SYNCED -> Icons.Rounded.CloudDone
        SyncState.LOCAL_ONLY -> Icons.Rounded.CloudOff
        SyncState.CONFLICT -> Icons.Rounded.WarningAmber
        SyncState.FAILED -> Icons.Rounded.ErrorOutline
        else -> Icons.Rounded.CloudSync
    }
    val fmt = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    HolographicPanel(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("sync_status_indicator_card"),
        accentColor = accent
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(imageVector = icon, contentDescription = summary.overallState.label, tint = accent, modifier = Modifier.size(28.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "CLOUD SYNC // ${summary.overallState.label.uppercase()}",
                    style = MaterialTheme.typography.titleSmall,
                    color = SystemColors.PureIce
                )
                Text(
                    text = "Pending ↑${summary.pendingUploadsCount} • Conflicts: ${summary.openConflictsCount} • ${
                        summary.lastSuccessfulSyncEpochMs?.let { "Last: ${fmt.format(Date(it))}" } ?: "Offline-first active"
                    }",
                    style = MaterialTheme.typography.bodySmall,
                    color = SystemColors.MetallicSilver
                )
            }
        }
    }
}

// Sync UX #1: Cloud Sync Dashboard
@Composable
fun CloudSyncDashboardScreen(
    uiState: CloudSyncUiState,
    onSyncNow: () -> Unit,
    onOpenPendingChanges: () -> Unit,
    onOpenSyncHistory: () -> Unit,
    onOpenConflicts: () -> Unit,
    onOpenDevices: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenAccountSettings: () -> Unit,
    onOpenAuthGateway: () -> Unit
) {
    val summary = uiState.summary
    val fmt = remember { SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault()) }

    Box(
        modifier = Modifier.fillMaxSize().background(SystemColors.ObsidianBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 760.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SystemSectionTitle(
                    title = "Cloud Sync Command",
                    subtitle = "Incremental offline-first synchronization & multi-device convergence"
                )
            }
            item {
                CloudFeedbackBanner(uiState.feedbackMessage, uiState.errorMessage ?: summary.lastErrorSummary, uiState.isBusy || summary.isSyncing)
            }
            item {
                SyncStatusIndicatorCard(summary = summary, onClick = onSyncNow)
            }
            item {
                HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.ElectricCyan) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("TELEMETRY COUNTERS", style = MaterialTheme.typography.titleSmall, color = SystemColors.ElectricCyan)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            SyncStatTile("Pending Uploads", "${summary.pendingUploadsCount}", SystemColors.SystemBlue, Modifier.weight(1f))
                            Spacer(Modifier.width(8.dp))
                            SyncStatTile("Pending Pulls", "${summary.pendingDownloadsCount}", SystemColors.ArcaneViolet, Modifier.weight(1f))
                            Spacer(Modifier.width(8.dp))
                            SyncStatTile("Conflicts", "${summary.openConflictsCount}", SystemColors.WarningAmber, Modifier.weight(1f))
                        }
                        Text(
                            text = "Network: ${if (summary.isOnline) "Connected" else "Offline"} • Backend: ${if (summary.isCloudConfigured) "Supabase Ready" else "Local Vault"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SystemColors.MetallicSilver
                        )
                        Text(
                            text = "Last Successful Sync: ${summary.lastSuccessfulSyncEpochMs?.let { fmt.format(Date(it)) } ?: "Never"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SystemColors.MutedSilver
                        )
                        Button(
                            onClick = if (uiState.identity != null) onSyncNow else onOpenAuthGateway,
                            enabled = !uiState.isBusy && !summary.isSyncing,
                            modifier = Modifier.fillMaxWidth().testTag("sync_now_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = SystemColors.ElectricCyan, contentColor = SystemColors.ObsidianBlack)
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (uiState.identity != null) "SYNCHRONIZE NOW" else "SIGN IN TO SYNCHRONIZE")
                        }
                    }
                }
            }
            item {
                HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.ArcaneViolet) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("SYNCHRONIZATION SUBSYSTEMS", style = MaterialTheme.typography.titleSmall, color = SystemColors.ArcaneViolet)
                        OutlinedButton(onClick = onOpenPendingChanges, modifier = Modifier.fillMaxWidth().testTag("sync_nav_pending")) {
                            Icon(Icons.Rounded.PendingActions, contentDescription = null, tint = SystemColors.ElectricCyan)
                            Spacer(Modifier.width(8.dp))
                            Text("PENDING CHANGES (${uiState.queue.size})", color = SystemColors.PureIce)
                        }
                        OutlinedButton(onClick = onOpenConflicts, modifier = Modifier.fillMaxWidth().testTag("sync_nav_conflicts")) {
                            Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = SystemColors.WarningAmber)
                            Spacer(Modifier.width(8.dp))
                            Text("CONFLICT RESOLUTION (${summary.openConflictsCount} OPEN)", color = SystemColors.PureIce)
                        }
                        OutlinedButton(onClick = onOpenSyncHistory, modifier = Modifier.fillMaxWidth().testTag("sync_nav_history")) {
                            Icon(Icons.Rounded.History, contentDescription = null, tint = SystemColors.SystemBlue)
                            Spacer(Modifier.width(8.dp))
                            Text("SYNC HISTORY & DIAGNOSTICS", color = SystemColors.PureIce)
                        }
                        OutlinedButton(onClick = onOpenDevices, modifier = Modifier.fillMaxWidth().testTag("sync_nav_devices")) {
                            Icon(Icons.Rounded.Devices, contentDescription = null, tint = SystemColors.ArcaneViolet)
                            Spacer(Modifier.width(8.dp))
                            Text("REGISTERED DEVICES (${uiState.devices.size})", color = SystemColors.PureIce)
                        }
                        OutlinedButton(onClick = onOpenBackup, modifier = Modifier.fillMaxWidth().testTag("sync_nav_backup")) {
                            Icon(Icons.Rounded.Backup, contentDescription = null, tint = SystemColors.VerifiedEmerald)
                            Spacer(Modifier.width(8.dp))
                            Text("CLOUD BACKUPS & RESTORE", color = SystemColors.PureIce)
                        }
                        OutlinedButton(onClick = onOpenAccountSettings, modifier = Modifier.fillMaxWidth().testTag("sync_nav_account")) {
                            Icon(Icons.Rounded.AccountCircle, contentDescription = null, tint = SystemColors.MetallicSilver)
                            Spacer(Modifier.width(8.dp))
                            Text("ACCOUNT MANAGEMENT", color = SystemColors.PureIce)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncStatTile(
    label: String,
    value: String,
    accent: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SystemColors.PanelSurfaceElevated,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = accent)
            Text(label, style = MaterialTheme.typography.labelSmall, color = SystemColors.MutedSilver)
        }
    }
}

// Sync UX #3: Pending Changes Screen
@Composable
fun PendingChangesScreen(
    uiState: CloudSyncUiState,
    onRetryItem: (Long) -> Unit,
    onSyncNow: () -> Unit
) {
    val fmt = remember { SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault()) }
    Box(
        modifier = Modifier.fillMaxSize().background(SystemColors.ObsidianBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 760.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SystemSectionTitle(
                    title = "Pending Outbox Queue",
                    subtitle = "${uiState.queue.size} local mutations awaiting or retrying cloud sync",
                    trailing = {
                        Button(onClick = onSyncNow, enabled = !uiState.isBusy) {
                            Text("FLUSH")
                        }
                    }
                )
            }
            item {
                CloudFeedbackBanner(uiState.feedbackMessage, uiState.errorMessage, uiState.isBusy)
            }
            if (uiState.queue.isEmpty()) {
                item {
                    SystemEmptyState(
                        title = "Outbox Clear",
                        description = "All local records are caught up or operating cleanly in offline vault mode."
                    )
                }
            } else {
                items(uiState.queue, key = { it.queueId }) { item ->
                    val accent = when (item.syncState) {
                        SyncState.FAILED -> SystemColors.DangerCrimson
                        SyncState.UPLOADING -> SystemColors.ElectricCyan
                        else -> SystemColors.SystemBlue
                    }
                    HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = accent) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${item.entityType.label} (${item.operation.name})", style = MaterialTheme.typography.titleSmall, color = SystemColors.PureIce)
                                Text(item.syncState.label, style = MaterialTheme.typography.labelSmall, color = accent)
                            }
                            Text("Key: ${item.localKey} • Retries: ${item.retryCount}", style = MaterialTheme.typography.bodySmall, color = SystemColors.MetallicSilver)
                            Text("Updated: ${fmt.format(Date(item.updatedAtEpochMs))}", style = MaterialTheme.typography.labelSmall, color = SystemColors.MutedSilver)
                            if (!item.lastError.isNullOrBlank()) {
                                Text("Error: ${item.lastError}", style = MaterialTheme.typography.bodySmall, color = SystemColors.DangerCrimson)
                            }
                            if (item.syncState == SyncState.FAILED) {
                                OutlinedButton(
                                    onClick = { onRetryItem(item.queueId) },
                                    modifier = Modifier.fillMaxWidth().testTag("retry_queue_${item.queueId}")
                                ) {
                                    Text("RE-QUEUE NOW", color = SystemColors.ElectricCyan)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Sync UX #4: Sync History & Diagnostics
@Composable
fun SyncHistoryScreen(
    uiState: CloudSyncUiState
) {
    val fmt = remember { SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault()) }
    Box(
        modifier = Modifier.fillMaxSize().background(SystemColors.ObsidianBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 760.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SystemSectionTitle(
                    title = "Synchronization Diagnostics",
                    subtitle = "Sanitized operation telemetry (tokens & secrets never logged)"
                )
            }
            if (uiState.diagnostics.isEmpty()) {
                item {
                    SystemEmptyState(
                        title = "No Sync Runs Recorded",
                        description = "Trigger manual sync or connect an account to view incremental synchronization history."
                    )
                }
            } else {
                items(uiState.diagnostics, key = { it.operationUuid }) { log ->
                    val accent = when (log.status) {
                        SyncState.SYNCED -> SystemColors.VerifiedEmerald
                        SyncState.CONFLICT -> SystemColors.WarningAmber
                        SyncState.FAILED -> SystemColors.DangerCrimson
                        else -> SystemColors.ElectricCyan
                    }
                    HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = accent) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${log.triggerSource} • ${log.status.label}", style = MaterialTheme.typography.titleSmall, color = accent)
                                Text(fmt.format(Date(log.startedAtEpochMs)), style = MaterialTheme.typography.labelSmall, color = SystemColors.MutedSilver)
                            }
                            Text(
                                "Uploaded: ${log.recordsUploaded} • Downloaded: ${log.recordsDownloaded} • Skipped: ${log.recordsSkipped} • Conflicts: ${log.conflictsDetected}",
                                style = MaterialTheme.typography.bodySmall,
                                color = SystemColors.PureIce
                            )
                            Text("Op ID: ${log.operationUuid.take(12)} • Retries: ${log.retryCount}", style = MaterialTheme.typography.labelSmall, color = SystemColors.MutedSilver)
                            if (log.safeMessage.isNotBlank()) {
                                Text(log.safeMessage, style = MaterialTheme.typography.bodySmall, color = SystemColors.MetallicSilver)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Auth #8 & Sync UX #6: Registered Devices & Session Management
@Composable
fun RegisteredDevicesScreen(
    uiState: CloudSyncUiState,
    onRefreshDevices: () -> Unit,
    onRevokeDevice: (String) -> Unit
) {
    val fmt = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    Box(
        modifier = Modifier.fillMaxSize().background(SystemColors.ObsidianBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 760.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SystemSectionTitle(
                    title = "Registered Devices",
                    subtitle = "Multi-device Hunter sessions & remote access revocation",
                    trailing = {
                        OutlinedButton(onClick = onRefreshDevices, enabled = !uiState.isBusy) {
                            Text("REFRESH", color = SystemColors.ElectricCyan)
                        }
                    }
                )
            }
            item {
                CloudFeedbackBanner(uiState.feedbackMessage, uiState.errorMessage, uiState.isBusy)
            }
            if (uiState.devices.isEmpty()) {
                item {
                    SystemEmptyState(
                        title = "No Linked Devices",
                        description = "Sign in to register this Android device and link other phones or tablets."
                    )
                }
            } else {
                items(uiState.devices, key = { it.deviceUuid }) { dev ->
                    val accent = when {
                        dev.isRevoked -> SystemColors.DangerCrimson
                        dev.isCurrentDevice -> SystemColors.ElectricCyan
                        else -> SystemColors.ArcaneViolet
                    }
                    HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = accent) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    text = dev.deviceName + if (dev.isCurrentDevice) " (THIS DEVICE)" else "",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = SystemColors.PureIce
                                )
                                Text(
                                    text = if (dev.isRevoked) "REVOKED" else dev.syncStatus.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accent
                                )
                            }
                            Text("${dev.osVersion} • App v${dev.appVersion}", style = MaterialTheme.typography.bodySmall, color = SystemColors.MetallicSilver)
                            Text(
                                "Last Active: ${fmt.format(Date(dev.lastActiveEpochMs))} • Last Sync: ${dev.lastSyncEpochMs?.let { fmt.format(Date(it)) } ?: "Pending"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = SystemColors.MutedSilver
                            )
                            if (!dev.isRevoked) {
                                OutlinedButton(
                                    onClick = { onRevokeDevice(dev.deviceUuid) },
                                    modifier = Modifier.fillMaxWidth().testTag("revoke_device_${dev.deviceUuid}"),
                                    border = BorderStroke(1.dp, SystemColors.DangerCrimson)
                                ) {
                                    Text(
                                        if (dev.isCurrentDevice) "LOG OUT THIS DEVICE" else "REVOKE DEVICE ACCESS",
                                        color = SystemColors.DangerCrimson
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
