package com.example.ui.screens.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.PhysicalDevelopmentState
import com.example.domain.model.RecoveryStatus
import com.example.ui.components.GlowingProgressBar
import com.example.ui.components.HolographicPanel
import com.example.ui.components.SystemSectionTitle
import com.example.ui.theme.SystemColors

@Composable
fun PhysicalTelemetrySection(
    physical: PhysicalDevelopmentState,
    streakDays: Int,
    onOpenPhysicalHub: () -> Unit,
    onOpenLogSheet: () -> Unit,
    onOpenAiAdvisory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val recoveryColor = when (physical.recoveryStatus) {
        RecoveryStatus.OPTIMAL -> SystemColors.VerifiedEmerald
        RecoveryStatus.MODERATE_LOAD -> SystemColors.WarningAmber
        RecoveryStatus.RECOVERY_REQUIRED -> SystemColors.DangerCrimson
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SystemSectionTitle(
            title = "Physical Development",
            subtitle = "Training Load & Recovery Readiness",
            trailing = {
                TextButton(
                    onClick = onOpenPhysicalHub,
                    modifier = Modifier.testTag("open_physical_hub_button")
                ) {
                    Text(
                        text = "OPEN HUB",
                        style = MaterialTheme.typography.labelSmall,
                        color = SystemColors.ElectricCyan
                    )
                }
            }
        )

        HolographicPanel(
            modifier = Modifier.fillMaxWidth(),
            accentColor = SystemColors.VerifiedEmerald,
            secondaryColor = SystemColors.ElectricCyan,
            contentPadding = 14.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Jogging Target Readout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.DirectionsRun,
                            contentDescription = "Jogging target",
                            tint = SystemColors.ElectricCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "DAILY JOGGING TARGET",
                            style = MaterialTheme.typography.titleMedium,
                            color = SystemColors.PureIce
                        )
                    }
                    Text(
                        text = "${physical.completedJoggingMinutesToday} / ${physical.effectiveJoggingTarget} MIN",
                        style = MaterialTheme.typography.labelLarge,
                        color = SystemColors.ElectricCyan
                    )
                }

                GlowingProgressBar(
                    progress = physical.joggingProgressFraction,
                    height = 8.dp,
                    startColor = SystemColors.VerifiedEmerald,
                    endColor = SystemColors.ElectricCyan
                )

                // Workout Status & Recovery Status Tiles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Workout Status
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = SystemColors.ObsidianBlack.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(0.75.dp, SystemColors.SubtleBorder)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "WORKOUT STATUS",
                                style = MaterialTheme.typography.labelSmall,
                                color = SystemColors.MutedSilver
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = physical.workoutStatus.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = SystemColors.PureIce
                            )
                        }
                    }

                    // Recovery Status
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = recoveryColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(0.75.dp, recoveryColor.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "RECOVERY READINESS",
                                style = MaterialTheme.typography.labelSmall,
                                color = recoveryColor
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = physical.recoveryStatus.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = SystemColors.PureIce
                            )
                        }
                    }
                }

                // Recovery Safety Directive (Rest Recommended) or AI Advisory Note
                val isRestRecommended = physical.recoveryStatus == RecoveryStatus.RECOVERY_REQUIRED
                if (isRestRecommended) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = SystemColors.DangerCrimson.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, SystemColors.DangerCrimson.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = "Rest Recommended",
                                tint = SystemColors.DangerCrimson,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "SAFETY DIRECTIVE // REST RECOMMENDED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SystemColors.DangerCrimson
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "High training load active. Intense workouts deferred. Focus on hydration, mobility, and deep rest.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SystemColors.PureIce
                                )
                            }
                        }
                    }
                } else if (physical.aiTrainingNote.isNotBlank()) {
                    Text(
                        text = physical.aiTrainingNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = SystemColors.MetallicSilver
                    )
                }

                // Action Deck (Start Workout / Recovery Hub, Log Session, AI Coach)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isRestRecommended) {
                        OutlinedButton(
                            onClick = onOpenPhysicalHub,
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("start_workout_button"),
                            border = BorderStroke(1.dp, SystemColors.DangerCrimson),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = SystemColors.DangerCrimson
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shield,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "RECOVERY HUB",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    } else {
                        Button(
                            onClick = onOpenPhysicalHub,
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("start_workout_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SystemColors.ElectricCyan,
                                contentColor = SystemColors.ObsidianBlack
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FitnessCenter,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "START WORKOUT",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onOpenLogSheet,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("open_log_activity_sheet"),
                        border = BorderStroke(1.dp, SystemColors.PanelBorderCyan),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = SystemColors.PureIce
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            tint = SystemColors.ElectricCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "LOG",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    OutlinedButton(
                        onClick = onOpenAiAdvisory,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("open_ai_advisory_button"),
                        border = BorderStroke(1.dp, SystemColors.PanelBorderViolet),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = SystemColors.ArcaneViolet
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = "AI Advisory",
                            tint = SystemColors.ArcaneViolet,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "COACH",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}
