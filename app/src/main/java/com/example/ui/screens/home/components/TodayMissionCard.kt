package com.example.ui.screens.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material.icons.rounded.Shield
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.Quest
import com.example.domain.model.QuestCategory
import com.example.ui.components.GlowingProgressBar
import com.example.ui.components.HolographicPanel
import com.example.ui.components.ProvenanceChip
import com.example.ui.theme.SystemColors
import com.example.ui.theme.SystemSpacing

@Composable
fun TodayMissionCard(
    dailyQuests: List<Quest>,
    onCompleteQuest: (Long) -> Unit,
    onOpenQuestBoard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalDaily = dailyQuests.size
    val completedDaily = dailyQuests.count { it.isCompleted }
    val isAllCleared = totalDaily > 0 && completedDaily == totalDaily

    // Select primary active quest (first uncompleted)
    val primaryQuest = dailyQuests.firstOrNull { !it.isCompleted } ?: dailyQuests.firstOrNull()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Section Header
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
                    imageVector = Icons.Rounded.ElectricBolt,
                    contentDescription = null,
                    tint = SystemColors.ElectricCyan,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "TODAY'S MISSION",
                    style = MaterialTheme.typography.titleMedium,
                    color = SystemColors.PureIce
                )
            }

            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(SystemSpacing.cornerSmall))
                    .clickable(onClick = onOpenQuestBoard)
                    .testTag("home_view_all_quests_button"),
                color = SystemColors.SecondarySurface,
                shape = RoundedCornerShape(SystemSpacing.cornerSmall),
                border = BorderStroke(1.dp, SystemColors.SubtleBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (totalDaily > 0) "$completedDaily/$totalDaily CLEARED" else "QUEST BOARD",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isAllCleared) SystemColors.VerifiedEmerald else SystemColors.ElectricCyan
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = if (isAllCleared) SystemColors.VerifiedEmerald else SystemColors.ElectricCyan,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        if (primaryQuest == null) {
            // Empty state
            HolographicPanel(
                modifier = Modifier.fillMaxWidth(),
                accentColor = SystemColors.ElectricCyan,
                contentPadding = SystemSpacing.cardPadding
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                ) {
                    Text(
                        text = "NO ACTIVE DAILY MISSIONS",
                        style = MaterialTheme.typography.titleSmall,
                        color = SystemColors.PureIce
                    )
                    Text(
                        text = "Synchronize system directives or create a custom quest on the Quest Board.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SystemColors.MutedSilver
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = onOpenQuestBoard,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SystemColors.ElectricCyan,
                            contentColor = SystemColors.ObsidianBlack
                        ),
                        shape = RoundedCornerShape(SystemSpacing.cornerSmall)
                    ) {
                        Text("OPEN QUEST BOARD", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        } else if (isAllCleared) {
            // All cleared state
            HolographicPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("today_mission_cleared_card"),
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = SystemColors.VerifiedEmerald,
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text(
                                    text = "DAILY PROTOCOL COMPLETE",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = SystemColors.PureIce
                                )
                                Text(
                                    text = "All $totalDaily daily objectives verified. Penalty Quest averted.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SystemColors.VerifiedEmerald
                                )
                            }
                        }

                        Surface(
                            color = SystemColors.VerifiedEmerald.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(SystemSpacing.cornerSmall),
                            border = BorderStroke(1.dp, SystemColors.VerifiedEmerald.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "100%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SystemColors.VerifiedEmerald,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    GlowingProgressBar(
                        progress = 1.0f,
                        height = 6.dp,
                        startColor = SystemColors.VerifiedEmerald,
                        endColor = SystemColors.ElectricCyan
                    )
                }
            }
        } else {
            // Active Primary Daily Quest Card
            val categoryColor = when (primaryQuest.category) {
                QuestCategory.PHYSICAL -> SystemColors.VerifiedEmerald
                QuestCategory.INTELLECT -> SystemColors.ArcaneViolet
                QuestCategory.DISCIPLINE -> SystemColors.SystemBlue
                QuestCategory.SOCIAL -> SystemColors.ElectricCyan
                QuestCategory.RECOVERY -> SystemColors.WarningAmber
            }

            HolographicPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("today_mission_primary_card"),
                accentColor = categoryColor,
                contentPadding = SystemSpacing.cardPadding
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Category & Provenance Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                color = categoryColor.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(SystemSpacing.cornerSmall),
                                border = BorderStroke(1.dp, categoryColor.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = primaryQuest.category.label.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = categoryColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }

                            Text(
                                text = "• PRIMARY DIRECTIVE",
                                style = MaterialTheme.typography.labelSmall,
                                color = SystemColors.ElectricCyan
                            )
                        }

                        ProvenanceChip(status = primaryQuest.verificationStatus)
                    }

                    // Title & Description
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = primaryQuest.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = SystemColors.PureIce
                        )

                        if (primaryQuest.description.isNotBlank()) {
                            Text(
                                text = primaryQuest.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = SystemColors.MutedSilver
                            )
                        }
                    }

                    // Progress bar & Telemetry
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CURRENT PROGRESS",
                                style = MaterialTheme.typography.labelSmall,
                                color = SystemColors.MutedSilver
                            )
                            Text(
                                text = "${primaryQuest.currentValue} / ${primaryQuest.targetValue} ${primaryQuest.unit} (${(primaryQuest.progressFraction * 100).toInt()}%)",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                color = SystemColors.ElectricCyan
                            )
                        }

                        GlowingProgressBar(
                            progress = primaryQuest.progressFraction,
                            height = 6.dp,
                            startColor = categoryColor,
                            endColor = SystemColors.ElectricCyan
                        )
                    }

                    // Rewards & Action Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Reward tags
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                color = SystemColors.MonarchGold.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(SystemSpacing.cornerSmall),
                                border = BorderStroke(1.dp, SystemColors.MonarchGold.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "+${primaryQuest.xpReward} XP",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SystemColors.MonarchGold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            Surface(
                                color = SystemColors.SecondarySurface,
                                shape = RoundedCornerShape(SystemSpacing.cornerSmall),
                                border = BorderStroke(1.dp, SystemColors.SubtleBorder)
                            ) {
                                Text(
                                    text = "${primaryQuest.primaryAttribute.code} +${primaryQuest.primaryAttrXpReward}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SystemColors.attributeAccent(primaryQuest.primaryAttribute),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Actions
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onOpenQuestBoard,
                                modifier = Modifier.heightIn(min = 44.dp),
                                shape = RoundedCornerShape(SystemSpacing.cornerSmall),
                                border = BorderStroke(1.dp, SystemColors.PanelBorderCyan),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = SystemColors.ElectricCyan
                                )
                            ) {
                                Text("BOARD", style = MaterialTheme.typography.labelMedium)
                            }

                            Button(
                                onClick = { onCompleteQuest(primaryQuest.id) },
                                modifier = Modifier
                                    .heightIn(min = 44.dp)
                                    .testTag("home_complete_primary_quest_${primaryQuest.id}"),
                                shape = RoundedCornerShape(SystemSpacing.cornerSmall),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SystemColors.ElectricCyan,
                                    contentColor = SystemColors.ObsidianBlack
                                )
                            ) {
                                Text("COMPLETE", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
