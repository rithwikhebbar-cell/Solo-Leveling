package com.example.ui.screens.physical.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.RecoveryAssessment
import com.example.domain.model.RecoveryState
import com.example.ui.components.HolographicPanel
import com.example.ui.theme.SystemColors
import com.example.ui.theme.SystemSpacing

@Composable
fun RecoveryCentreSection(
    recovery: RecoveryAssessment,
    onOpenCheckIn: () -> Unit,
    onMarkRestDay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = when (recovery.state) {
        RecoveryState.READY -> SystemColors.VerifiedEmerald
        RecoveryState.MODERATE -> SystemColors.ElectricCyan
        RecoveryState.RECOVERY_RECOMMENDED -> SystemColors.WarningAmber
        RecoveryState.REST_RECOMMENDED -> SystemColors.DangerCrimson
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SystemSpacing.sectionSpacing)
    ) {
        // 1. RECOVERY READINESS HERO
        HolographicPanel(
            modifier = Modifier.fillMaxWidth(),
            accentColor = accent,
            isHero = true,
            contentPadding = SystemSpacing.heroCardPadding
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(accent)
                            )
                            Text(
                                text = "SYSTEM RECOVERY ENGINE",
                                style = MaterialTheme.typography.labelSmall,
                                color = accent
                            )
                        }
                        Text(
                            text = recovery.state.label.uppercase(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = SystemColors.PureIce
                        )
                    }
                    Text(
                        text = "${recovery.readinessScore}/100",
                        style = MaterialTheme.typography.displayMedium,
                        color = accent
                    )
                }

                Text(
                    text = recovery.summaryMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SystemColors.MetallicSilver
                )

                // High-level load metrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RecoveryMetricTile("48H LOAD", "${recovery.measuredLoadMinutes48h}m", Modifier.weight(1f))
                    RecoveryMetricTile("7D SESSIONS", "${recovery.measuredSessions7d}", Modifier.weight(1f))
                    RecoveryMetricTile("ACTIVE STREAK", "${recovery.consecutiveActiveDays}d", Modifier.weight(1f))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onOpenCheckIn,
                        modifier = Modifier
                            .weight(1.2f)
                            .heightIn(min = SystemSpacing.minTouchTarget)
                            .testTag("open_recovery_checkin_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SystemColors.ElectricCyan,
                            contentColor = SystemColors.ObsidianBlack
                        ),
                        shape = RoundedCornerShape(SystemSpacing.cornerMedium)
                    ) {
                        Icon(Icons.Rounded.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("REPORT FATIGUE", style = MaterialTheme.typography.labelLarge)
                    }
                    OutlinedButton(
                        onClick = onMarkRestDay,
                        modifier = Modifier
                            .weight(0.8f)
                            .heightIn(min = SystemSpacing.minTouchTarget)
                            .testTag("rest_day_button"),
                        shape = RoundedCornerShape(SystemSpacing.cornerMedium),
                        border = BorderStroke(SystemSpacing.borderStandard, SystemColors.SubtleBorder)
                    ) {
                        Icon(Icons.Rounded.Bedtime, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("REST DAY", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // 2. CONTRIBUTING FACTORS & TELEMETRY
        HolographicPanel(
            modifier = Modifier.fillMaxWidth(),
            accentColor = SystemColors.ElectricCyan,
            contentPadding = SystemSpacing.cardPadding
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MonitorHeart,
                        contentDescription = null,
                        tint = SystemColors.ElectricCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "CONTRIBUTING RECOVERY FACTORS",
                        style = MaterialTheme.typography.titleMedium,
                        color = SystemColors.PureIce
                    )
                }

                // Health Connect Verified vs Self-Reported Breakdown
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RecoveryFactorRow(
                        label = "Measured Sleep (Health Connect)",
                        value = recovery.measuredSleepHoursLastNight?.let { "${"%.1f".format(it)} hrs" } ?: "Not available",
                        isVerified = recovery.measuredSleepHoursLastNight != null
                    )
                    RecoveryFactorRow(
                        label = "Resting Heart Rate (Health Connect)",
                        value = recovery.measuredRestingHrBpm?.let { "$it bpm" } ?: "Not available",
                        isVerified = recovery.measuredRestingHrBpm != null
                    )
                    RecoveryFactorRow(
                        label = "Self-Reported Fatigue Level",
                        value = recovery.selfReportedFatigue?.let { "$it / 5" } ?: "Not logged",
                        isVerified = false
                    )
                    RecoveryFactorRow(
                        label = "Physical Discomfort / Pain",
                        value = if (recovery.selfReportedDiscomfort) "YES (Caution)" else "No discomfort",
                        isVerified = false,
                        isWarning = recovery.selfReportedDiscomfort
                    )
                }

                if (recovery.discomfortNotes.isNotBlank()) {
                    Surface(
                        color = SystemColors.SecondarySurface,
                        shape = RoundedCornerShape(SystemSpacing.cornerSmall),
                        border = BorderStroke(SystemSpacing.borderSubtle, SystemColors.SubtleBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Discomfort Notes: ${recovery.discomfortNotes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = SystemColors.MetallicSilver
                            )
                        }
                    }
                }
            }
        }

        // 3. ACTIONABLE RECOVERY RECOMMENDATIONS
        if (recovery.recommendedActions.isNotEmpty()) {
            HolographicPanel(
                modifier = Modifier.fillMaxWidth(),
                accentColor = SystemColors.SystemBlue,
                contentPadding = SystemSpacing.cardPadding
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shield,
                            contentDescription = null,
                            tint = SystemColors.SystemBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "ACTIONABLE RECOVERY RECOMMENDATIONS",
                            style = MaterialTheme.typography.titleMedium,
                            color = SystemColors.SystemBlue
                        )
                    }

                    recovery.recommendedActions.forEach { action ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(SystemColors.SystemBlue)
                            )
                            Text(
                                text = action,
                                style = MaterialTheme.typography.bodyMedium,
                                color = SystemColors.PureIce
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecoveryFactorRow(
    label: String,
    value: String,
    isVerified: Boolean,
    isWarning: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = SystemColors.MetallicSilver
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (isVerified) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = "Verified",
                    tint = SystemColors.VerifiedEmerald,
                    modifier = Modifier.size(14.dp)
                )
            } else if (isWarning) {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = "Warning",
                    tint = SystemColors.DangerCrimson,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                color = when {
                    isWarning -> SystemColors.DangerCrimson
                    isVerified -> SystemColors.VerifiedEmerald
                    else -> SystemColors.PureIce
                }
            )
        }
    }
}

@Composable
private fun RecoveryMetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = SystemColors.SecondarySurface,
        shape = RoundedCornerShape(SystemSpacing.cornerMedium),
        border = BorderStroke(SystemSpacing.borderSubtle, SystemColors.SubtleBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = SystemColors.MutedSilver)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.titleSmall, color = SystemColors.PureIce)
        }
    }
}
