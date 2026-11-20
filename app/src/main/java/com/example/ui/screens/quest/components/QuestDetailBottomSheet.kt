package com.example.ui.screens.quest.components

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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.Quest
import com.example.domain.model.QuestCategory
import com.example.ui.components.GlowingProgressBar
import com.example.ui.components.ProvenanceChip
import com.example.ui.theme.SystemColors
import com.example.ui.theme.SystemSpacing
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestDetailBottomSheet(
    quest: Quest?,
    isCompleting: Boolean,
    onComplete: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    if (quest == null) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val categoryColor = when (quest.category) {
        QuestCategory.PHYSICAL -> SystemColors.VerifiedEmerald
        QuestCategory.INTELLECT -> SystemColors.ArcaneViolet
        QuestCategory.DISCIPLINE -> SystemColors.SystemBlue
        QuestCategory.SOCIAL -> SystemColors.ElectricCyan
        QuestCategory.RECOVERY -> SystemColors.WarningAmber
    }

    val accent = if (quest.isCompleted) SystemColors.VerifiedEmerald else SystemColors.ElectricCyan

    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        .withZone(ZoneId.systemDefault())

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
                .testTag("quest_detail_bottom_sheet"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Header with Badges and Close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = categoryColor.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(SystemSpacing.cornerSmall),
                        border = BorderStroke(1.dp, categoryColor.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = quest.category.label.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = categoryColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = "• ${quest.type.label.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SystemColors.MutedSilver
                    )

                    ProvenanceChip(status = quest.verificationStatus)
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

            // 2. Title & Mission Briefing
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = quest.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = SystemColors.PureIce
                )

                if (quest.description.isNotBlank()) {
                    Text(
                        text = quest.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SystemColors.MutedSilver
                    )
                }
            }

            // 3. Objective Progress Telemetry
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SystemColors.PanelSurface,
                shape = RoundedCornerShape(SystemSpacing.cornerMedium),
                border = BorderStroke(SystemSpacing.borderSubtle, SystemColors.SubtleBorder)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "OBJECTIVE TELEMETRY",
                            style = MaterialTheme.typography.labelSmall,
                            color = SystemColors.MutedSilver
                        )
                        Text(
                            text = if (quest.isCompleted) {
                                "COMPLETED (${quest.targetValue}/${quest.targetValue} ${quest.unit})"
                            } else {
                                "${quest.currentValue} / ${quest.targetValue} ${quest.unit} (${(quest.progressFraction * 100).toInt()}%)"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (quest.isCompleted) SystemColors.VerifiedEmerald else SystemColors.ElectricCyan
                        )
                    }

                    GlowingProgressBar(
                        progress = if (quest.isCompleted) 1f else quest.progressFraction,
                        height = 8.dp,
                        startColor = accent,
                        endColor = SystemColors.ElectricCyan
                    )
                }
            }

            // 4. Rewards Breakdown
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "REWARD PROTOCOL",
                    style = MaterialTheme.typography.labelSmall,
                    color = SystemColors.MutedSilver
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // XP Reward Pill
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = SystemColors.MonarchGold.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(SystemSpacing.cornerMedium),
                        border = BorderStroke(SystemSpacing.borderSubtle, SystemColors.MonarchGold.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "PLAYER XP",
                                style = MaterialTheme.typography.labelSmall,
                                color = SystemColors.MonarchGold
                            )
                            Text(
                                text = "+${quest.xpReward} XP",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SystemColors.MonarchGold
                            )
                        }
                    }

                    // Primary Attribute Reward Pill
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = SystemColors.attributeAccent(quest.primaryAttribute).copy(alpha = 0.12f),
                        shape = RoundedCornerShape(SystemSpacing.cornerMedium),
                        border = BorderStroke(
                            SystemSpacing.borderSubtle,
                            SystemColors.attributeAccent(quest.primaryAttribute).copy(alpha = 0.4f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = quest.primaryAttribute.displayName.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = SystemColors.attributeAccent(quest.primaryAttribute)
                            )
                            Text(
                                text = "+${quest.primaryAttrXpReward} ATTR XP",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SystemColors.attributeAccent(quest.primaryAttribute)
                            )
                        }
                    }
                }
            }

            // 5. System Timestamps Metadata
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SystemColors.SecondarySurface,
                shape = RoundedCornerShape(SystemSpacing.cornerSmall),
                border = BorderStroke(SystemSpacing.borderSubtle, SystemColors.SubtleBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = SystemColors.MutedSilver,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "CREATED: ${dateFormatter.format(Instant.ofEpochMilli(quest.createdAtEpochMs))}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = SystemColors.MutedSilver
                        )
                    }

                    if (quest.isCompleted && quest.completedAtEpochMs != null) {
                        Text(
                            text = "CLEARED",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SystemColors.VerifiedEmerald
                        )
                    }
                }
            }

            // 6. Action Button / Status
            if (quest.isCompleted) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(SystemSpacing.buttonHeight),
                    color = SystemColors.VerifiedEmerald.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(SystemSpacing.cornerMedium),
                    border = BorderStroke(1.dp, SystemColors.VerifiedEmerald.copy(alpha = 0.4f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = SystemColors.VerifiedEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "MISSION ACCOMPLISHED & ARCHIVED",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = SystemColors.VerifiedEmerald
                            )
                        }
                    }
                }
            } else {
                Button(
                    onClick = { onComplete(quest.id) },
                    enabled = !isCompleting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(SystemSpacing.buttonHeight)
                        .testTag("quest_detail_complete_button"),
                    shape = RoundedCornerShape(SystemSpacing.cornerMedium),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SystemColors.ElectricCyan,
                        contentColor = SystemColors.ObsidianBlack,
                        disabledContainerColor = SystemColors.ElectricCyan.copy(alpha = 0.4f),
                        disabledContentColor = SystemColors.ObsidianBlack.copy(alpha = 0.6f)
                    )
                ) {
                    if (isCompleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = SystemColors.ObsidianBlack,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "VERIFYING...",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "COMPLETE MISSION",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
