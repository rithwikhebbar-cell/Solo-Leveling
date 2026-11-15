package com.example.ui.screens

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.model.ActivityLog
import com.example.domain.model.SyncSummaryState
import com.example.ui.components.HolographicPanel
import com.example.ui.components.ProvenanceChip
import com.example.ui.components.SystemEmptyState
import com.example.ui.components.SystemSectionTitle
import com.example.ui.screens.cloud.SyncStatusIndicatorCard
import com.example.ui.theme.SystemColors
import com.example.ui.viewmodel.RealityOsUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    state: RealityOsUiState.Ready,
    modifier: Modifier = Modifier
) {
    val formatter = remember {
        SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SystemColors.ObsidianBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SystemSectionTitle(
                    title = "Activity & Progression History",
                    subtitle = "${state.activityLogs.size} persisted events"
                )
            }

            if (state.activityLogs.isEmpty()) {
                item {
                    SystemEmptyState(
                        title = "No Recorded Activities Yet",
                        description = "Complete a quest or log a real-world training session from the Home Dashboard to populate your verified history."
                    )
                }
            } else {
                items(state.activityLogs, key = { it.id }) { log ->
                    ActivityHistoryCard(
                        log = log,
                        formattedTime = formatter.format(Date(log.timestampEpochMs))
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityHistoryCard(
    log: ActivityLog,
    formattedTime: String
) {
    val accent = if (log.verificationStatus.isVerified) SystemColors.VerifiedEmerald else SystemColors.ElectricCyan
    HolographicPanel(
        modifier = Modifier.fillMaxWidth(),
        accentColor = accent,
        contentPadding = 14.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${log.category.label.uppercase()} • $formattedTime",
                    style = MaterialTheme.typography.labelSmall,
                    color = SystemColors.MutedSilver
                )
                ProvenanceChip(status = log.verificationStatus)
            }

            Text(
                text = log.title,
                style = MaterialTheme.typography.titleMedium,
                color = SystemColors.PureIce
            )

            if (log.notes.isNotBlank()) {
                Text(
                    text = log.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = SystemColors.MetallicSilver
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${log.durationMinutes} min",
                    style = MaterialTheme.typography.labelMedium,
                    color = SystemColors.MetallicSilver
                )
                Text(
                    text = "+${log.xpAwarded} XP • ${log.primaryAttribute.code} +${log.primaryAttrXpAwarded}",
                    style = MaterialTheme.typography.labelMedium,
                    color = SystemColors.ElectricCyan
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(
    state: RealityOsUiState.Ready,
    onUpdateCodename: (String) -> Unit,
    onUpdateJoggingBaseline: (Int) -> Unit,
    onSetHudGlow: (Boolean) -> Unit,
    onSetHealthConnectSync: (Boolean) -> Unit,
    onSetSupabaseSync: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    syncSummary: SyncSummaryState = SyncSummaryState(),
    onOpenCloudGateway: () -> Unit = {},
    onOpenCloudSyncDashboard: () -> Unit = {}
) {
    var codenameInput by rememberSaveable(state.player.codename) {
        mutableStateOf(state.player.codename)
    }
    var joggingInput by rememberSaveable(state.physicalState.baseJoggingTargetMinutes) {
        mutableStateOf(state.physicalState.baseJoggingTargetMinutes.toString())
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SystemColors.ObsidianBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SystemSectionTitle(
                    title = "System Configuration",
                    subtitle = "Identity, training baseline & integration ports"
                )
            }

            // Identity & Baseline (never attributes or rank)
            item {
                HolographicPanel(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "PLAYER IDENTITY",
                            style = MaterialTheme.typography.titleMedium,
                            color = SystemColors.ElectricCyan
                        )
                        OutlinedTextField(
                            value = codenameInput,
                            onValueChange = { codenameInput = it.take(28) },
                            label = { Text("Player Codename") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_codename_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SystemColors.ElectricCyan,
                                unfocusedBorderColor = SystemColors.PanelBorderCyan,
                                focusedTextColor = SystemColors.PureIce,
                                unfocusedTextColor = SystemColors.PureIce
                            )
                        )
                        OutlinedTextField(
                            value = joggingInput,
                            onValueChange = { joggingInput = it.filter { ch -> ch.isDigit() }.take(3) },
                            label = { Text("Daily Jogging Baseline (minutes, 10–120)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SystemColors.ElectricCyan,
                                unfocusedBorderColor = SystemColors.PanelBorderCyan,
                                focusedTextColor = SystemColors.PureIce,
                                unfocusedTextColor = SystemColors.PureIce
                            )
                        )
                        Button(
                            onClick = {
                                onUpdateCodename(codenameInput)
                                joggingInput.toIntOrNull()?.let(onUpdateJoggingBaseline)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_settings_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SystemColors.ElectricCyan,
                                contentColor = SystemColors.ObsidianBlack
                            )
                        ) {
                            Text("SAVE CONFIGURATION", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            // Interface Preferences
            item {
                HolographicPanel(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = SystemColors.ArcaneViolet
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "INTERFACE & ARCHITECTURE PORTS",
                            style = MaterialTheme.typography.titleMedium,
                            color = SystemColors.ArcaneViolet
                        )
                        SettingToggleRow(
                            title = "Holographic HUD Corner Brackets",
                            subtitle = "Render sci-fi corner accents on panels",
                            checked = state.preferences.hudGlowEnabled,
                            onCheckedChange = onSetHudGlow
                        )
                        SettingToggleRow(
                            title = "Google Health Connect Adapter",
                            subtitle = "${state.healthConnectReadiness.label} — ${state.healthConnectReadiness.detail}",
                            checked = state.preferences.healthConnectSyncEnabled,
                            onCheckedChange = onSetHealthConnectSync
                        )
                        SettingToggleRow(
                            title = "Supabase Cloud Sync Port",
                            subtitle = "Status: ${state.cloudSyncStatus.label}",
                            checked = state.preferences.supabaseCloudSyncEnabled,
                            onCheckedChange = onSetSupabaseSync
                        )
                    }
                }
            }

            item {
                SyncStatusIndicatorCard(
                    summary = syncSummary,
                    onClick = onOpenCloudSyncDashboard
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenCloudGateway,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_open_cloud_gateway"),
                        border = BorderStroke(1.dp, SystemColors.ElectricCyan)
                    ) {
                        Text("CLOUD ACCOUNT", color = SystemColors.ElectricCyan)
                    }
                    Button(
                        onClick = onOpenCloudSyncDashboard,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("settings_open_sync_dashboard"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SystemColors.ElectricCyan,
                            contentColor = SystemColors.ObsidianBlack
                        )
                    ) {
                        Text("SYNC DASHBOARD")
                    }
                }
            }

            item {
                HolographicPanel(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = SystemColors.MutedSilver
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "INTEGRITY NOTICE",
                            style = MaterialTheme.typography.labelMedium,
                            color = SystemColors.ElectricCyan
                        )
                        Text(
                            text = "Attributes, Level, and Rank are strictly governed by the deterministic ProgressionEngine and cannot be modified manually.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SystemColors.MetallicSilver
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = SystemColors.PureIce
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = SystemColors.MutedSilver
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = SystemColors.ObsidianBlack,
                checkedTrackColor = SystemColors.ElectricCyan
            )
        )
    }
}
