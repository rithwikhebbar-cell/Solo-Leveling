package com.example.ui.screens.modules

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.components.HolographicPanel
import com.example.ui.theme.SystemColors
import com.example.ui.theme.SystemSpacing

@Composable
fun ModuleCard(
    mod: ModuleSpec,
    onCardClick: () -> Unit,
    onDirectLaunch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLocked = mod.status == ModuleOperationalStatus.LOCKED
    val isOperational = mod.status.isOperational

    val statusColor = when (mod.status) {
        ModuleOperationalStatus.ACTIVE -> SystemColors.VerifiedEmerald
        ModuleOperationalStatus.AVAILABLE -> SystemColors.ElectricCyan
        ModuleOperationalStatus.LOCKED -> SystemColors.DangerCrimson
        ModuleOperationalStatus.COMING_SOON -> SystemColors.MonarchGold
        ModuleOperationalStatus.UNAVAILABLE -> SystemColors.MutedSilver
    }

    HolographicPanel(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SystemSpacing.cornerMedium))
            .clickable(onClick = onCardClick)
            .alpha(if (isLocked) 0.65f else 1.0f)
            .testTag("module_card_${mod.id}"),
        accentColor = if (isLocked) SystemColors.MutedSilver else mod.accentColor,
        contentPadding = SystemSpacing.cardPadding
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Top Row: Icon, Title & Category, Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Icon Badge
                    Surface(
                        color = mod.accentColor.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(SystemSpacing.cornerSmall),
                        border = BorderStroke(1.dp, mod.accentColor.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = if (isLocked) Icons.Rounded.Lock else mod.icon,
                            contentDescription = mod.title,
                            tint = if (isLocked) SystemColors.MutedSilver else mod.accentColor,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = mod.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SystemColors.PureIce,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = mod.category.label.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = mod.accentColor
                        )
                    }
                }

                // Operational Status Badge
                Surface(
                    color = statusColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(SystemSpacing.cornerSmall),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = mod.status.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Description
            Text(
                text = mod.description,
                style = MaterialTheme.typography.bodySmall,
                color = SystemColors.MutedSilver,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Telemetry / Highlight Capsule
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SystemColors.ObsidianBlack.copy(alpha = 0.55f),
                shape = RoundedCornerShape(SystemSpacing.cornerSmall),
                border = BorderStroke(1.dp, SystemColors.SubtleBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "CAPABILITY FOCUS",
                        style = MaterialTheme.typography.labelSmall,
                        color = SystemColors.MutedSilver
                    )
                    Text(
                        text = mod.metricHighlight,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = mod.accentColor
                    )
                }
            }

            // Bottom Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = mod.statusRationale,
                    style = MaterialTheme.typography.labelSmall,
                    color = SystemColors.MutedSilver,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )

                // Launch Action
                if (isOperational) {
                    Button(
                        onClick = onDirectLaunch,
                        modifier = Modifier
                            .heightIn(min = 44.dp)
                            .testTag("module_launch_${mod.id}"),
                        shape = RoundedCornerShape(SystemSpacing.cornerSmall),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = mod.accentColor,
                            contentColor = SystemColors.ObsidianBlack
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "LAUNCH",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                } else {
                    Surface(
                        modifier = Modifier.heightIn(min = 44.dp),
                        color = SystemColors.SecondarySurface,
                        shape = RoundedCornerShape(SystemSpacing.cornerSmall),
                        border = BorderStroke(1.dp, SystemColors.SubtleBorder)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "RESTRICTED",
                                style = MaterialTheme.typography.labelSmall,
                                color = SystemColors.MutedSilver
                            )
                        }
                    }
                }
            }
        }
    }
}
