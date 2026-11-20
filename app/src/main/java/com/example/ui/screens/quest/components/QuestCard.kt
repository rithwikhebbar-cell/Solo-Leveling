package com.example.ui.screens.quest.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.Quest
import com.example.domain.model.QuestCategory
import com.example.ui.components.GlowingProgressBar
import com.example.ui.components.HolographicPanel
import com.example.ui.components.ProvenanceChip
import com.example.ui.theme.SystemColors
import com.example.ui.theme.SystemSpacing

@Composable
fun QuestCard(
    quest: Quest,
    isCompleting: Boolean,
    onComplete: () -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryColor = when (quest.category) {
        QuestCategory.PHYSICAL -> SystemColors.VerifiedEmerald
        QuestCategory.INTELLECT -> SystemColors.ArcaneViolet
        QuestCategory.DISCIPLINE -> SystemColors.SystemBlue
        QuestCategory.SOCIAL -> SystemColors.ElectricCyan
        QuestCategory.RECOVERY -> SystemColors.WarningAmber
    }

    val accent = if (quest.isCompleted) SystemColors.VerifiedEmerald else SystemColors.ElectricCyan

    HolographicPanel(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SystemSpacing.cornerLarge))
            .clickable(onClick = onCardClick)
            .alpha(if (quest.isCompleted) 0.72f else 1f)
            .testTag("quest_card_${quest.id}"),
        accentColor = if (quest.isCompleted) SystemColors.VerifiedEmerald else categoryColor,
        contentPadding = SystemSpacing.cardPadding
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // 1. Top Badges Row: Category, Type, and Provenance
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
                            text = quest.category.label.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = categoryColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Text(
                        text = "• ${quest.type.label.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SystemColors.MutedSilver
                    )
                }

                ProvenanceChip(status = quest.verificationStatus)
            }

            // 2. Mission Title & Description
            Text(
                text = quest.title,
                style = MaterialTheme.typography.titleMedium,
                color = SystemColors.PureIce
            )

            if (quest.description.isNotBlank()) {
                Text(
                    text = quest.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = SystemColors.MutedSilver,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 3. Progress Telemetry & Bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "OBJECTIVE PROGRESS",
                        style = MaterialTheme.typography.labelSmall,
                        color = SystemColors.MutedSilver
                    )
                    Text(
                        text = if (quest.isCompleted) {
                            "COMPLETE (${quest.targetValue}/${quest.targetValue} ${quest.unit})"
                        } else {
                            "${quest.currentValue} / ${quest.targetValue} ${quest.unit} (${(quest.progressFraction * 100).toInt()}%)"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (quest.isCompleted) SystemColors.VerifiedEmerald else SystemColors.ElectricCyan
                    )
                }

                GlowingProgressBar(
                    progress = if (quest.isCompleted) 1f else quest.progressFraction,
                    height = 6.dp,
                    startColor = accent,
                    endColor = SystemColors.ElectricCyan
                )
            }

            // 4. Rewards Breakdown and Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reward badges
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = SystemColors.MonarchGold.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(SystemSpacing.cornerSmall),
                        border = BorderStroke(SystemSpacing.borderSubtle, SystemColors.MonarchGold.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "+${quest.xpReward} XP",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = SystemColors.MonarchGold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        color = SystemColors.SecondarySurface,
                        shape = RoundedCornerShape(SystemSpacing.cornerSmall),
                        border = BorderStroke(SystemSpacing.borderSubtle, SystemColors.SubtleBorder)
                    ) {
                        Text(
                            text = "${quest.primaryAttribute.code} +${quest.primaryAttrXpReward}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SystemColors.attributeAccent(quest.primaryAttribute),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Action or Cleared indicator
                if (quest.isCompleted) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Cleared",
                            tint = SystemColors.VerifiedEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "CLEARED",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = SystemColors.VerifiedEmerald
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = onComplete,
                        enabled = !isCompleting,
                        modifier = Modifier
                            .heightIn(min = SystemSpacing.minTouchTarget)
                            .testTag("quest_board_complete_${quest.id}"),
                        shape = RoundedCornerShape(SystemSpacing.cornerMedium),
                        border = BorderStroke(1.dp, SystemColors.ElectricCyan),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = SystemColors.ElectricCyan
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        if (isCompleting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = SystemColors.ElectricCyan
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SAVING...", style = MaterialTheme.typography.labelMedium)
                        } else {
                            Text("COMPLETE", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}
