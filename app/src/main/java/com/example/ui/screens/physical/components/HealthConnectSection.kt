package com.example.ui.screens.physical.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.healthconnect.HealthConnectPermissionState
import com.example.data.healthconnect.HealthConnectSdkAvailability
import com.example.domain.model.HealthConnectSyncState
import com.example.domain.model.ImportedHealthActivity
import com.example.ui.components.HolographicPanel
import com.example.ui.components.ProvenanceChip
import com.example.ui.components.SystemEmptyState
import com.example.ui.theme.SystemColors
import com.example.ui.theme.SystemSpacing
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun HealthConnectSection(
    availability: HealthConnectSdkAvailability,
    permissions: HealthConnectPermissionState,
    syncState: HealthConnectSyncState,
    isSyncing: Boolean,
    importedActivities: List<ImportedHealthActivity>,
    timeFormatter: SimpleDateFormat,
    onRequestPermissions: () -> Unit,
    onOpenSettings: () -> Unit,
    onSyncNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAvailable = availability == HealthConnectSdkAvailability.AVAILABLE

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SystemSpacing.sectionSpacing)
    ) {
        // 1. HEALTH CONNECT INTEGRATION CARD
        HolographicPanel(
            modifier = Modifier.fillMaxWidth(),
            accentColor = SystemColors.ElectricCyan,
            contentPadding = SystemSpacing.cardPadding
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.HealthAndSafety,
                        contentDescription = null,
                        tint = SystemColors.ElectricCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "GOOGLE HEALTH CONNECT",
                            style = MaterialTheme.typography.titleMedium,
                            color = SystemColors.PureIce
                        )
                        Text(
                            text = availability.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isAvailable) SystemColors.VerifiedEmerald else SystemColors.WarningAmber
                        )
                    }
                }

                Text(
                    text = availability.detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SystemColors.MetallicSilver
                )

                // Permissions matrix
                Surface(
                    color = SystemColors.SecondarySurface,
                    shape = RoundedCornerShape(SystemSpacing.cornerMedium),
                    border = BorderStroke(SystemSpacing.borderSubtle, SystemColors.SubtleBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "READ PERMISSIONS MATRIX",
                            style = MaterialTheme.typography.labelSmall,
                            color = SystemColors.MutedSilver
                        )
                        PermissionCheckRow("Exercise Sessions (Required)", permissions.exerciseReadGranted)
                        PermissionCheckRow("Distance Records", permissions.distanceReadGranted)
                        PermissionCheckRow("Heart Rate Telemetry", permissions.heartRateReadGranted)
                        PermissionCheckRow("Sleep Recovery Sessions", permissions.sleepReadGranted)
                    }
                }

                // Sync telemetry
                Surface(
                    color = SystemColors.SecondarySurface,
                    shape = RoundedCornerShape(SystemSpacing.cornerMedium),
                    border = BorderStroke(SystemSpacing.borderSubtle, SystemColors.SubtleBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "SYNCHRONIZATION TELEMETRY",
                            style = MaterialTheme.typography.labelSmall,
                            color = SystemColors.MutedSilver
                        )
                        Text(
                            text = "Last Sync: ${syncState.lastSyncEpochMs?.let { timeFormatter.format(Date(it)) } ?: "Never"} • ${syncState.lastSyncStatus}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SystemColors.PureIce
                        )
                        if (syncState.lastImportedCount > 0 || syncState.lastDuplicateCount > 0) {
                            Text(
                                text = "Imported: ${syncState.lastImportedCount} • Duplicates: ${syncState.lastDuplicateCount} • Rejected: ${syncState.lastRejectedCount}",
                                style = MaterialTheme.typography.bodySmall,
                                color = SystemColors.ElectricCyan
                            )
                        }
                        if (syncState.lastErrorMessage != null) {
                            Text(
                                text = syncState.lastErrorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = SystemColors.WarningAmber
                            )
                        }
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onRequestPermissions,
                        enabled = isAvailable,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = SystemSpacing.minTouchTarget)
                            .testTag("grant_hc_permissions_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SystemColors.ElectricCyan,
                            contentColor = SystemColors.ObsidianBlack
                        ),
                        shape = RoundedCornerShape(SystemSpacing.cornerMedium)
                    ) {
                        Icon(Icons.Rounded.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PERMISSIONS", style = MaterialTheme.typography.labelMedium)
                    }

                    Button(
                        onClick = onSyncNow,
                        enabled = isAvailable && !isSyncing,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = SystemSpacing.minTouchTarget)
                            .testTag("sync_health_connect_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SystemColors.VerifiedEmerald,
                            contentColor = SystemColors.ObsidianBlack
                        ),
                        shape = RoundedCornerShape(SystemSpacing.cornerMedium)
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = SystemColors.ObsidianBlack
                            )
                        } else {
                            Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SYNC NOW", style = MaterialTheme.typography.labelMedium)
                    }
                }

                OutlinedButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = SystemSpacing.minTouchTarget),
                    shape = RoundedCornerShape(SystemSpacing.cornerMedium),
                    border = BorderStroke(SystemSpacing.borderStandard, SystemColors.SubtleBorder)
                ) {
                    Icon(Icons.Rounded.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("MANAGE IN HEALTH CONNECT SETTINGS", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        // 2. IMPORTED HEALTH ACTIVITIES STREAM
        Text(
            text = "IMPORTED HEALTH ACTIVITIES",
            style = MaterialTheme.typography.labelSmall,
            color = SystemColors.MutedSilver
        )

        if (importedActivities.isEmpty()) {
            SystemEmptyState(
                title = "No Imported Activities",
                description = "Sync Health Connect to import recorded exercise sessions, runs, and workouts from external fitness apps."
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                importedActivities.forEach { imported ->
                    ImportedHealthActivityCard(imported = imported)
                }
            }
        }
    }
}

@Composable
fun ImportedHealthActivityCard(
    imported: ImportedHealthActivity,
    modifier: Modifier = Modifier
) {
    HolographicPanel(
        modifier = modifier.fillMaxWidth(),
        accentColor = SystemColors.VerifiedEmerald,
        contentPadding = SystemSpacing.compactCardPadding
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = imported.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = SystemColors.PureIce
                )
                ProvenanceChip(status = imported.verificationStatus)
            }

            Text(
                text = "${imported.activityType} • ${imported.durationMinutes} min • +${imported.xpAwarded} XP",
                style = MaterialTheme.typography.labelMedium,
                color = SystemColors.ElectricCyan
            )

            Text(
                text = "Source: ${imported.originPackage} • ${imported.validationOutcome.label}",
                style = MaterialTheme.typography.bodySmall,
                color = SystemColors.MutedSilver
            )
        }
    }
}

@Composable
private fun PermissionCheckRow(label: String, granted: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = SystemColors.PureIce
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (granted) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Granted",
                    tint = SystemColors.VerifiedEmerald,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = if (granted) "GRANTED" else "MISSING",
                style = MaterialTheme.typography.labelSmall,
                color = if (granted) SystemColors.VerifiedEmerald else SystemColors.WarningAmber
            )
        }
    }
}
