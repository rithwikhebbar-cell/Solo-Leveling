package com.example.ui.screens.modules

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.SystemColors
import com.example.ui.theme.SystemSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleDetailBottomSheet(
    mod: ModuleSpec?,
    onLaunch: () -> Unit,
    onDismiss: () -> Unit
) {
    if (mod == null) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isLocked = mod.status == ModuleOperationalStatus.LOCKED
    val isOperational = mod.status.isOperational

    val statusColor = when (mod.status) {
        ModuleOperationalStatus.ACTIVE -> SystemColors.VerifiedEmerald
        ModuleOperationalStatus.AVAILABLE -> SystemColors.ElectricCyan
        ModuleOperationalStatus.LOCKED -> SystemColors.DangerCrimson
        ModuleOperationalStatus.COMING_SOON -> SystemColors.MonarchGold
        ModuleOperationalStatus.UNAVAILABLE -> SystemColors.MutedSilver
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SystemColors.DeepNavy,
        shape = RoundedCornerShape(
            topStart = SystemSpacing.cornerLarge,
            topEnd = SystemSpacing.cornerLarge
        ),
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 36.dp, height = 4.dp),
                color = SystemColors.MutedSilver.copy(alpha = 0.4f),
                shape = CircleShape
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .testTag("module_detail_sheet"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
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
                                .size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = mod.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = SystemColors.PureIce
                        )
                        Text(
                            text = mod.category.label.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = mod.accentColor
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = SystemColors.MutedSilver
                    )
                }
            }

            // 2. Operational Status Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = statusColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(SystemSpacing.cornerMedium),
                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (isOperational) Icons.Rounded.CheckCircle else Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "STATUS: ${mod.status.label}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                        Text(
                            text = mod.statusRationale,
                            style = MaterialTheme.typography.bodySmall,
                            color = SystemColors.PureIce
                        )
                    }
                }
            }

            // 3. Subsystem Overview & Description
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "SUBSYSTEM BRIEFING",
                    style = MaterialTheme.typography.labelSmall,
                    color = SystemColors.MutedSilver
                )
                Text(
                    text = mod.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SystemColors.PureIce
                )
            }

            // 4. Core Capabilities & Architecture Scope
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SystemColors.PanelSurface,
                shape = RoundedCornerShape(SystemSpacing.cornerMedium),
                border = BorderStroke(1.dp, SystemColors.SubtleBorder)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "CAPABILITY MATRIX",
                        style = MaterialTheme.typography.labelSmall,
                        color = SystemColors.MutedSilver
                    )
                    Text(
                        text = mod.capabilitySummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = SystemColors.MetallicSilver
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TELEMETRY FOCUS",
                            style = MaterialTheme.typography.labelSmall,
                            color = SystemColors.MutedSilver
                        )
                        Text(
                            text = mod.metricHighlight,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = mod.accentColor
                        )
                    }
                }
            }

            // 5. Action Button
            if (isOperational) {
                Button(
                    onClick = {
                        onDismiss()
                        onLaunch()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(SystemSpacing.buttonHeight)
                        .testTag("module_detail_launch_button"),
                    shape = RoundedCornerShape(SystemSpacing.cornerMedium),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = mod.accentColor,
                        contentColor = SystemColors.ObsidianBlack
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "OPEN ${mod.title.uppercase()}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(SystemSpacing.buttonHeight),
                    color = SystemColors.SecondarySurface,
                    shape = RoundedCornerShape(SystemSpacing.cornerMedium),
                    border = BorderStroke(1.dp, SystemColors.SubtleBorder)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "SUBSYSTEM ACCESS RESTRICTED",
                            style = MaterialTheme.typography.labelMedium,
                            color = SystemColors.MutedSilver
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
