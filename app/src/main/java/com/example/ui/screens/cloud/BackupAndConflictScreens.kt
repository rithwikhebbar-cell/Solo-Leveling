package com.example.ui.screens.cloud

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.ConflictResolutionChoice
import com.example.domain.model.ConflictStatus
import com.example.ui.components.HolographicPanel
import com.example.ui.components.SystemEmptyState
import com.example.ui.components.SystemSectionTitle
import com.example.ui.theme.SystemColors
import com.example.ui.viewmodel.CloudSyncUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Sync UX #5: Conflict Resolution Screen
@Composable
fun ConflictResolutionScreen(
    uiState: CloudSyncUiState,
    onResolveConflict: (conflictId: Long, choice: ConflictResolutionChoice) -> Unit
) {
    val openConflicts = uiState.conflicts.filter { it.status == ConflictStatus.OPEN }
    val resolvedConflicts = uiState.conflicts.filter { it.status != ConflictStatus.OPEN }

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
                    title = "Conflict Resolution",
                    subtitle = "${openConflicts.size} conflicts requiring safe user confirmation"
                )
            }
            item {
                CloudFeedbackBanner(uiState.feedbackMessage, uiState.errorMessage, uiState.isBusy)
            }
            if (openConflicts.isEmpty()) {
                item {
                    SystemEmptyState(
                        title = "No Active Conflicts",
                        description = "All multi-device records, completions, and progression events are converged cleanly."
                    )
                }
            } else {
                items(openConflicts, key = { it.conflictId }) { conflict ->
                    HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.WarningAmber) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Rounded.WarningAmber, contentDescription = "Conflict", tint = SystemColors.WarningAmber)
                                Text("${conflict.entityType.label}: ${conflict.title}", style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
                            }
                            Text(conflict.conflictReason, style = MaterialTheme.typography.bodySmall, color = SystemColors.WarningAmber)
                            Text("THIS DEVICE: ${conflict.localSummary}", style = MaterialTheme.typography.bodySmall, color = SystemColors.ElectricCyan)
                            Text("CLOUD VERSION: ${conflict.cloudSummary}", style = MaterialTheme.typography.bodySmall, color = SystemColors.ArcaneViolet)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { onResolveConflict(conflict.conflictId, ConflictResolutionChoice.KEEP_LOCAL) },
                                    modifier = Modifier.weight(1f).testTag("conflict_keep_local_${conflict.conflictId}"),
                                    border = BorderStroke(1.dp, SystemColors.ElectricCyan)
                                ) {
                                    Text("KEEP LOCAL", color = SystemColors.ElectricCyan)
                                }
                                OutlinedButton(
                                    onClick = { onResolveConflict(conflict.conflictId, ConflictResolutionChoice.KEEP_CLOUD) },
                                    modifier = Modifier.weight(1f).testTag("conflict_keep_cloud_${conflict.conflictId}"),
                                    border = BorderStroke(1.dp, SystemColors.ArcaneViolet)
                                ) {
                                    Text("KEEP CLOUD", color = SystemColors.ArcaneViolet)
                                }
                            }
                            Button(
                                onClick = { onResolveConflict(conflict.conflictId, ConflictResolutionChoice.MERGE_SAFE) },
                                modifier = Modifier.fillMaxWidth().testTag("conflict_merge_${conflict.conflictId}"),
                                colors = ButtonDefaults.buttonColors(containerColor = SystemColors.VerifiedEmerald, contentColor = SystemColors.ObsidianBlack)
                            ) {
                                Text("MERGE SAFELY (PRESERVE DATA)")
                            }
                        }
                    }
                }
            }

            if (resolvedConflicts.isNotEmpty()) {
                item {
                    Text("RECENTLY RESOLVED (${resolvedConflicts.size})", style = MaterialTheme.typography.labelMedium, color = SystemColors.MutedSilver)
                }
                items(resolvedConflicts.take(10), key = { it.conflictId }) { item ->
                    HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.VerifiedEmerald) {
                        Text("${item.title} — ${item.status.name}", style = MaterialTheme.typography.bodySmall, color = SystemColors.MetallicSilver)
                    }
                }
            }
        }
    }
}

// Sync UX #7: Cloud Backup Screen
@Composable
fun CloudBackupScreen(
    uiState: CloudSyncUiState,
    onCreateBackup: () -> Unit,
    onInspectBackupForRestore: (backupUuid: String) -> Unit
) {
    val fmt = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }
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
                    title = "Cloud Backups & Snapshots",
                    subtitle = "SHA-256 integrity-checked versioned archives"
                )
            }
            item {
                CloudFeedbackBanner(uiState.feedbackMessage, uiState.errorMessage, uiState.isBusy)
            }
            item {
                HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.VerifiedEmerald) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Create a consistent snapshot of your Player Profile, Habits, Tasks, Settings, and Progression History. Restores are always previewed and validated by ProgressionEngine before execution.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SystemColors.MetallicSilver
                        )
                        Button(
                            onClick = onCreateBackup,
                            enabled = !uiState.isBusy,
                            modifier = Modifier.fillMaxWidth().testTag("create_backup_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = SystemColors.ElectricCyan, contentColor = SystemColors.ObsidianBlack)
                        ) {
                            Icon(Icons.Rounded.Backup, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("CREATE BACKUP SNAPSHOT")
                        }
                    }
                }
            }
            if (uiState.backups.isEmpty()) {
                item {
                    SystemEmptyState(
                        title = "No Snapshots Created",
                        description = "Create a manual snapshot above before major device changes."
                    )
                }
            } else {
                items(uiState.backups, key = { it.backupUuid }) { backup ->
                    HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.ElectricCyan) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(backup.summaryText, style = MaterialTheme.typography.titleSmall, color = SystemColors.PureIce)
                                Text(backup.status.label, style = MaterialTheme.typography.labelSmall, color = SystemColors.VerifiedEmerald)
                            }
                            Text(
                                "Schema v${backup.schemaVersion} • Created ${fmt.format(Date(backup.createdAtEpochMs))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = SystemColors.MetallicSilver
                            )
                            Text(
                                "SHA-256: ${backup.progressionChecksum.take(18)}...",
                                style = MaterialTheme.typography.labelSmall,
                                color = SystemColors.MutedSilver
                            )
                            OutlinedButton(
                                onClick = { onInspectBackupForRestore(backup.backupUuid) },
                                modifier = Modifier.fillMaxWidth().testTag("preview_restore_${backup.backupUuid}"),
                                border = BorderStroke(1.dp, SystemColors.ArcaneViolet)
                            ) {
                                Icon(Icons.Rounded.Restore, contentDescription = null, tint = SystemColors.ArcaneViolet)
                                Spacer(Modifier.width(8.dp))
                                Text("PREVIEW RESTORE", color = SystemColors.PureIce)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Sync UX #8: Restore Preview Screen
@Composable
fun RestorePreviewScreen(
    uiState: CloudSyncUiState,
    onExecuteRestore: (backupUuid: String) -> Unit
) {
    val preview = uiState.restorePreview
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
                    title = "Restore Preview & Verification",
                    subtitle = "Inspect checksum, schema compatibility & progression impact"
                )
            }
            item {
                CloudFeedbackBanner(uiState.feedbackMessage, uiState.errorMessage, uiState.isBusy)
            }
            if (preview == null) {
                item {
                    SystemEmptyState(
                        title = "No Backup Selected",
                        description = "Select a backup snapshot from Cloud Backups to inspect its restore preview."
                    )
                }
            } else {
                item {
                    HolographicPanel(
                        modifier = Modifier.fillMaxWidth(),
                        accentColor = if (preview.isChecksumValid && preview.isSchemaCompatible) SystemColors.VerifiedEmerald else SystemColors.DangerCrimson
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Rounded.VerifiedUser, contentDescription = "Integrity check", tint = SystemColors.ElectricCyan)
                                Text(
                                    text = if (preview.isChecksumValid) "CHECKSUM VERIFIED" else "CORRUPTED BACKUP",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = SystemColors.PureIce
                                )
                            }
                            Text(
                                "Backup Level: ${preview.backup.playerLevelSnapshot} (${preview.backup.totalXpSnapshot} XP) vs Current Local: Lv.${preview.currentLevel} (${preview.currentTotalXp} XP)",
                                style = MaterialTheme.typography.bodySmall,
                                color = SystemColors.MetallicSilver
                            )
                            preview.recordBreakdown.forEach { (k, v) ->
                                Text("• $k: $v records", style = MaterialTheme.typography.bodySmall, color = SystemColors.PureIce)
                            }
                            preview.warnings.forEach { warn ->
                                Text("⚠ $warn", style = MaterialTheme.typography.bodySmall, color = SystemColors.WarningAmber)
                            }
                            Button(
                                onClick = { onExecuteRestore(preview.backup.backupUuid) },
                                enabled = preview.isChecksumValid && preview.isSchemaCompatible && !uiState.isBusy,
                                modifier = Modifier.fillMaxWidth().testTag("execute_restore_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = SystemColors.ElectricCyan, contentColor = SystemColors.ObsidianBlack)
                            ) {
                                Text("CONFIRM TRANSACTIONAL RESTORE")
                            }
                        }
                    }
                }
            }
        }
    }
}
