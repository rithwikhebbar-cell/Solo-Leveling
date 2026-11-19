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
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Sync
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
import androidx.compose.ui.unit.dp
import com.example.domain.model.RecoveryState
import com.example.domain.model.WorkoutCategory
import com.example.ui.components.GlowingProgressBar
import com.example.ui.components.HolographicPanel
import com.example.ui.components.PrimaryActionButton
import com.example.ui.components.SecondaryActionButton
import com.example.ui.theme.SystemColors
import com.example.ui.theme.SystemSpacing
import com.example.ui.viewmodel.RealityOsUiState

@Composable
fun PhysicalOverviewDashboard(
    state: RealityOsUiState.Ready,
    onOpenJogging: () -> Unit,
    onOpenWorkouts: () -> Unit,
    onOpenRecovery: () -> Unit,
    onOpenHealthConnect: () -> Unit,
    onStartJoggingLive: () -> Unit,
    onOpenLiveJogSheet: () -> Unit,
    onStartWorkoutCategory: (WorkoutCategory) -> Unit,
    physicalPowerScore: Int,
    activeDays7d: Int,
    modifier: Modifier = Modifier
) {
    val bundle = state.physicalBundle
    val recovery = bundle.recoveryAssessment
    val player = state.player
    val isRestRecommended = recovery.state == RecoveryState.REST_RECOMMENDED

    val recoveryColor = when (recovery.state) {
        RecoveryState.READY -> SystemColors.VerifiedEmerald
        RecoveryState.MODERATE -> SystemColors.ElectricCyan
        RecoveryState.RECOVERY_RECOMMENDED -> SystemColors.WarningAmber
        RecoveryState.REST_RECOMMENDED -> SystemColors.DangerCrimson
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SystemSpacing.sectionSpacing)
    ) {
        // 1. LIVE JOGGING SESSION BANNER (if active)
        if (bundle.activeJoggingSession.isActive) {
            HolographicPanel(
                modifier = Modifier.fillMaxWidth(),
                accentColor = SystemColors.VerifiedEmerald,
                isHero = true,
                contentPadding = SystemSpacing.cardPadding
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(SystemColors.VerifiedEmerald)
                            )
                            Text(
                                text = "LIVE JOGGING SESSION IN PROGRESS",
                                style = MaterialTheme.typography.labelMedium,
                                color = SystemColors.VerifiedEmerald
                            )
                        }
                        Text(
                            text = if (bundle.activeJoggingSession.isPaused) "Session paused — timer on hold" else "Recording active aerobic telemetry",
                            style = MaterialTheme.typography.bodySmall,
                            color = SystemColors.MutedSilver
                        )
                    }
                    Button(
                        onClick = onOpenLiveJogSheet,
                        modifier = Modifier.heightIn(min = SystemSpacing.minTouchTarget),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SystemColors.VerifiedEmerald,
                            contentColor = SystemColors.ObsidianBlack
                        ),
                        shape = RoundedCornerShape(SystemSpacing.cornerMedium)
                    ) {
                        Text("OPEN TIMER", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        // 2. PHYSICAL STATUS HERO CARD
        HolographicPanel(
            modifier = Modifier.fillMaxWidth(),
            accentColor = SystemColors.ElectricCyan,
            secondaryColor = SystemColors.ArcaneViolet,
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
                        Text(
                            text = "HUNTER PHYSICAL STATUS",
                            style = MaterialTheme.typography.labelSmall,
                            color = SystemColors.ElectricCyan
                        )
                        Text(
                            text = "POWER SCORE: $physicalPowerScore",
                            style = MaterialTheme.typography.headlineMedium,
                            color = SystemColors.PureIce
                        )
                    }
                    Surface(
                        color = SystemColors.SecondarySurface,
                        shape = RoundedCornerShape(SystemSpacing.cornerSmall),
                        border = BorderStroke(SystemSpacing.borderSubtle, SystemColors.SubtleBorder)
                    ) {
                        Text(
                            text = "LVL ${player.level}",
                            style = MaterialTheme.typography.labelMedium,
                            color = SystemColors.ElectricCyan,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Attribute breakdown row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PhysicalAttributePill("STR", player.attributes.str.totalValue, Modifier.weight(1f))
                    PhysicalAttributePill("VIT", player.attributes.vit.totalValue, Modifier.weight(1f))
                    PhysicalAttributePill("AGI", player.attributes.agi.totalValue, Modifier.weight(1f))
                    PhysicalAttributePill("DIS", player.attributes.dis.totalValue, Modifier.weight(1f))
                }

                // Summary load row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OverviewMetricTile("CONSISTENCY", "$activeDays7d / 7d", Modifier.weight(1f))
                    OverviewMetricTile("7D LOAD", "${recovery.measuredLoadMinutes7d}m", Modifier.weight(1f))
                    OverviewMetricTile("RECOVERY", "${recovery.readinessScore}/100", Modifier.weight(1f))
                }
            }
        }

        // 3. TODAY'S WORKOUT PROTOCOL (WITH STRICT RECOVERY SAFETY DECISION)
        if (isRestRecommended) {
            // STRICT RECOVERY SAFETY OVERRIDE: NO WORKOUT START BUTTON PERMITTED
            HolographicPanel(
                modifier = Modifier.fillMaxWidth(),
                accentColor = SystemColors.DangerCrimson,
                secondaryColor = SystemColors.WarningAmber,
                isHero = true,
                contentPadding = SystemSpacing.heroCardPadding
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Security,
                            contentDescription = null,
                            tint = SystemColors.DangerCrimson,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "SAFETY OVERRIDE // REST RECOMMENDED",
                            style = MaterialTheme.typography.titleMedium,
                            color = SystemColors.DangerCrimson
                        )
                    }

                    Text(
                        text = "Recovery readiness score is critical (${recovery.readinessScore}/100) or physical discomfort was reported. All high-intensity workout proposals are locked today to prevent overtraining and systemic regression.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SystemColors.PureIce
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PrimaryActionButton(
                            text = "LOG REST RECOVERY",
                            onClick = onOpenRecovery,
                            icon = Icons.Rounded.Bedtime,
                            containerColor = SystemColors.WarningAmber,
                            contentColor = SystemColors.ObsidianBlack,
                            modifier = Modifier.weight(1f)
                        )
                        SecondaryActionButton(
                            text = "CHECK RECOVERY",
                            onClick = onOpenRecovery,
                            borderColor = SystemColors.DangerCrimson,
                            contentColor = SystemColors.DangerCrimson,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        } else {
            // CLEARED FOR TRAINING
            HolographicPanel(
                modifier = Modifier.fillMaxWidth(),
                accentColor = SystemColors.ElectricCyan,
                secondaryColor = SystemColors.VerifiedEmerald,
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
                            Text(
                                text = "TODAY'S WORKOUT PROTOCOL",
                                style = MaterialTheme.typography.labelSmall,
                                color = SystemColors.ElectricCyan
                            )
                            Text(
                                text = "SUGGESTION: ${bundle.advisoryWorkoutCategory.first.label.uppercase()}",
                                style = MaterialTheme.typography.titleLarge,
                                color = SystemColors.PureIce
                            )
                        }
                        Surface(
                            color = SystemColors.VerifiedEmerald.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(SystemSpacing.cornerSmall),
                            border = BorderStroke(SystemSpacing.borderSubtle, SystemColors.VerifiedEmerald.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = "READY",
                                style = MaterialTheme.typography.labelSmall,
                                color = SystemColors.VerifiedEmerald,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Text(
                        text = bundle.advisoryWorkoutCategory.second,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SystemColors.MutedSilver
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PrimaryActionButton(
                            text = "START ${bundle.advisoryWorkoutCategory.first.label.uppercase()}",
                            onClick = { onStartWorkoutCategory(bundle.advisoryWorkoutCategory.first) },
                            icon = Icons.Rounded.PlayArrow,
                            modifier = Modifier.weight(1.2f)
                        )
                        SecondaryActionButton(
                            text = "LIVE JOG",
                            onClick = onStartJoggingLive,
                            icon = Icons.Rounded.DirectionsRun,
                            modifier = Modifier.weight(0.8f)
                        )
                    }
                }
            }
        }

        // 4. DAILY JOGGING TARGET CARD
        HolographicPanel(
            modifier = Modifier.fillMaxWidth(),
            accentColor = SystemColors.VerifiedEmerald,
            contentPadding = SystemSpacing.cardPadding
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DirectionsRun,
                            contentDescription = null,
                            tint = SystemColors.VerifiedEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "DAILY JOGGING PROGRESS",
                            style = MaterialTheme.typography.titleSmall,
                            color = SystemColors.PureIce
                        )
                    }
                    Text(
                        text = "${state.physicalState.completedJoggingMinutesToday} / ${state.physicalState.effectiveJoggingTarget} min",
                        style = MaterialTheme.typography.labelMedium,
                        color = SystemColors.ElectricCyan
                    )
                }

                GlowingProgressBar(
                    progress = state.physicalState.joggingProgressFraction,
                    height = 8.dp,
                    fillColor = SystemColors.VerifiedEmerald
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onOpenJogging,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = SystemSpacing.minTouchTarget),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SystemColors.ElectricCyan,
                            contentColor = SystemColors.ObsidianBlack
                        ),
                        shape = RoundedCornerShape(SystemSpacing.cornerMedium)
                    ) {
                        Text("JOGGING HUB", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = onOpenWorkouts,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = SystemSpacing.minTouchTarget),
                        shape = RoundedCornerShape(SystemSpacing.cornerMedium),
                        border = BorderStroke(SystemSpacing.borderStandard, SystemColors.SubtleBorder)
                    ) {
                        Text("ALL WORKOUTS", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // 5. RECOVERY READINESS & TELEMETRY
        HolographicPanel(
            modifier = Modifier.fillMaxWidth(),
            accentColor = recoveryColor,
            contentPadding = SystemSpacing.cardPadding
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "RECOVERY READINESS: ${recovery.state.label.uppercase()}",
                            style = MaterialTheme.typography.titleSmall,
                            color = recoveryColor
                        )
                        Text(
                            text = recovery.summaryMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = SystemColors.MutedSilver
                        )
                    }
                    Text(
                        text = "${recovery.readinessScore}/100",
                        style = MaterialTheme.typography.headlineMedium,
                        color = SystemColors.PureIce
                    )
                }

                // Measured Health Connect indicators
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
                            text = "TELEMETRY METRICS",
                            style = MaterialTheme.typography.labelSmall,
                            color = SystemColors.MutedSilver
                        )
                        Text(
                            text = "Sleep: ${recovery.measuredSleepHoursLastNight?.let { "${"%.1f".format(it)}h" } ?: "Not available"} • " +
                                "Resting HR: ${recovery.measuredRestingHrBpm?.let { "$it bpm" } ?: "Not available"} • " +
                                "Fatigue: ${recovery.selfReportedFatigue?.let { "$it/5" } ?: "Not logged"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SystemColors.PureIce
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SecondaryActionButton(
                        text = "REPORT FATIGUE",
                        onClick = onOpenRecovery,
                        icon = Icons.Rounded.EditNote,
                        modifier = Modifier.weight(1f)
                    )
                    SecondaryActionButton(
                        text = "SYNC HEALTH CONNECT",
                        onClick = onOpenHealthConnect,
                        icon = Icons.Rounded.Sync,
                        borderColor = SystemColors.ArcaneViolet,
                        contentColor = SystemColors.ArcaneViolet,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PhysicalAttributePill(
    label: String,
    value: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = SystemColors.SecondarySurface,
        shape = RoundedCornerShape(SystemSpacing.cornerMedium),
        border = BorderStroke(SystemSpacing.borderSubtle, SystemColors.SubtleBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = SystemColors.MutedSilver
            )
            Text(
                text = "$value",
                style = MaterialTheme.typography.titleMedium,
                color = SystemColors.PureIce
            )
        }
    }
}

@Composable
private fun OverviewMetricTile(
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
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = SystemColors.MutedSilver
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = SystemColors.PureIce
            )
        }
    }
}
