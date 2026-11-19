package com.example.ui.screens.physical.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.RecoveryState
import com.example.ui.theme.SystemColors
import com.example.ui.theme.SystemSpacing

enum class PhysicalSubTab(val label: String) {
    OVERVIEW("Overview"),
    JOGGING("Jogging"),
    WORKOUTS("Workouts"),
    RECOVERY("Recovery"),
    HEALTH_CONNECT("Health Connect")
}

@Composable
fun PhysicalCommandHeader(
    selectedTab: PhysicalSubTab,
    onTabSelected: (PhysicalSubTab) -> Unit,
    recoveryState: RecoveryState,
    physicalPowerScore: Int,
    activeDays7d: Int,
    modifier: Modifier = Modifier
) {
    val tierLabel = when {
        physicalPowerScore >= 200 -> "MONARCH PHYSIQUE"
        physicalPowerScore >= 120 -> "ELITE VIGOR"
        physicalPowerScore >= 60 -> "CONDITIONED HUNTER"
        else -> "INITIATE CONDITIONING"
    }

    val recoveryColor = when (recoveryState) {
        RecoveryState.READY -> SystemColors.VerifiedEmerald
        RecoveryState.MODERATE -> SystemColors.ElectricCyan
        RecoveryState.RECOVERY_RECOMMENDED -> SystemColors.WarningAmber
        RecoveryState.REST_RECOMMENDED -> SystemColors.DangerCrimson
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SystemSpacing.itemSpacing)
    ) {
        // System telemetry badge & Title
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(SystemColors.ElectricCyan)
                )
                Text(
                    text = "SYSTEM // PHYSICAL DEVELOPMENT COMMAND CENTRE",
                    style = MaterialTheme.typography.labelSmall,
                    color = SystemColors.ElectricCyan
                )
            }

            Text(
                text = "PHYSICAL",
                style = MaterialTheme.typography.displayMedium,
                color = SystemColors.PureIce
            )

            Text(
                text = "Hunter conditioning, aerobic telemetry & recovery engine",
                style = MaterialTheme.typography.bodyMedium,
                color = SystemColors.MutedSilver
            )
        }

        // Quick status pills row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Conditioning Tier Pill
            Surface(
                color = SystemColors.SecondarySurface,
                shape = RoundedCornerShape(SystemSpacing.cornerSmall),
                border = BorderStroke(SystemSpacing.borderSubtle, SystemColors.SubtleBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Speed,
                        contentDescription = null,
                        tint = SystemColors.ElectricCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = tierLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = SystemColors.PureIce
                    )
                }
            }

            // Recovery Readiness Pill
            Surface(
                color = recoveryColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(SystemSpacing.cornerSmall),
                border = BorderStroke(SystemSpacing.borderSubtle, recoveryColor.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(recoveryColor)
                    )
                    Text(
                        text = "RECOVERY: ${recoveryState.label.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = recoveryColor
                    )
                }
            }

            // Consistency Pill
            Surface(
                color = SystemColors.SecondarySurface,
                shape = RoundedCornerShape(SystemSpacing.cornerSmall),
                border = BorderStroke(SystemSpacing.borderSubtle, SystemColors.SubtleBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FitnessCenter,
                        contentDescription = null,
                        tint = SystemColors.ArcaneViolet,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "$activeDays7d / 7D ACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = SystemColors.PureIce
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Tactical Sub-Tab Selector
        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PhysicalSubTab.entries.forEach { tab ->
                val isSelected = tab == selectedTab
                val activeColor = SystemColors.ElectricCyan

                val icon = when (tab) {
                    PhysicalSubTab.OVERVIEW -> Icons.Rounded.Speed
                    PhysicalSubTab.JOGGING -> Icons.Rounded.DirectionsRun
                    PhysicalSubTab.WORKOUTS -> Icons.Rounded.FitnessCenter
                    PhysicalSubTab.RECOVERY -> Icons.Rounded.MonitorHeart
                    PhysicalSubTab.HEALTH_CONNECT -> Icons.Rounded.HealthAndSafety
                }

                Surface(
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onTabSelected(tab) }
                        .testTag("physical_tab_${tab.name.lowercase()}"),
                    color = if (isSelected) activeColor.copy(alpha = 0.16f) else SystemColors.SecondarySurface,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(
                        width = if (isSelected) 1.dp else 0.75.dp,
                        color = if (isSelected) activeColor.copy(alpha = 0.8f) else SystemColors.SubtleBorder
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(activeColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) activeColor else SystemColors.MutedSilver,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) activeColor else SystemColors.MutedSilver
                        )
                    }
                }
            }
        }
    }
}
