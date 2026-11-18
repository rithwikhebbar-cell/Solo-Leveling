package com.example.ui.screens.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.Quest
import com.example.ui.components.GlowingProgressBar
import com.example.ui.components.HolographicPanel
import com.example.ui.components.ProvenanceChip
import com.example.ui.components.SystemEmptyState
import com.example.ui.components.SystemSectionTitle
import com.example.ui.theme.SystemColors

@Composable
fun DailyQuestTacticalSection(
    dailyQuests: List<Quest>,
    completedCount: Int,
    pendingCount: Int,
    progressFraction: Float,
    onCompleteQuest: (Long) -> Unit,
    onOpenQuestBoard: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingQuestIds by remember { mutableStateOf(setOf<Long>()) }

    // Clean up completed quests from pending set
    val activePendingIds = remember(dailyQuests, pendingQuestIds) {
        val activeIds = dailyQuests.filter { !it.isCompleted }.map { it.id }.toSet()
        pendingQuestIds.intersect(activeIds)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SystemSectionTitle(
            title = "Daily Quests",
            subtitle = "$completedCount completed • $pendingCount pending",
            trailing = {
                TextButton(
                    onClick = onOpenQuestBoard,
                    modifier = Modifier.testTag("open_quest_board_button")
                ) {
                    Text(
                        text = "QUEST BOARD",
                        style = MaterialTheme.typography.labelSmall,
                        color = SystemColors.ElectricCyan
                    )
                }
            }
        )

        HolographicPanel(
            modifier = Modifier.fillMaxWidth(),
            accentColor = SystemColors.ElectricCyan,
            secondaryColor = SystemColors.ArcaneViolet,
            contentPadding = 14.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Header progress summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DAILY COMPLETION PROTOCOL",
                        style = MaterialTheme.typography.labelMedium,
                        color = SystemColors.MetallicSilver
                    )
                    Text(
                        text = "${(progressFraction * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = SystemColors.ElectricCyan
                    )
                }

                GlowingProgressBar(
                    progress = progressFraction,
                    height = 7.dp,
                    startColor = SystemColors.ElectricCyan,
                    endColor = SystemColors.VerifiedEmerald
                )

                if (dailyQuests.isEmpty()) {
                    SystemEmptyState(
                        title = "No Daily Objectives",
                        description = "All daily quests cleared or none currently assigned. Check Quest Board for active campaigns."
                    )
                } else {
                    dailyQuests.forEach { quest ->
                        TacticalQuestCard(
                            quest = quest,
                            isProcessing = quest.id in activePendingIds,
                            onComplete = {
                                if (quest.id !in activePendingIds && !quest.isCompleted) {
                                    pendingQuestIds = pendingQuestIds + quest.id
                                    onCompleteQuest(quest.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TacticalQuestCard(
    quest: Quest,
    isProcessing: Boolean,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = SystemColors.ObsidianBlack.copy(alpha = 0.60f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            width = 0.75.dp,
            color = if (quest.isCompleted) {
                SystemColors.VerifiedEmerald.copy(alpha = 0.45f)
            } else {
                SystemColors.SubtleBorder
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Icon
            Icon(
                imageVector = if (quest.isCompleted) {
                    Icons.Rounded.CheckCircle
                } else {
                    Icons.Rounded.RadioButtonUnchecked
                },
                contentDescription = if (quest.isCompleted) "Completed" else "Pending",
                tint = if (quest.isCompleted) {
                    SystemColors.VerifiedEmerald
                } else {
                    SystemColors.ElectricCyan
                },
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Quest details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = quest.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (quest.isCompleted) SystemColors.MutedSilver else SystemColors.PureIce,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${quest.targetValue} ${quest.unit} • +${quest.xpReward} XP • ${quest.primaryAttribute.code}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SystemColors.MutedSilver
                    )
                    ProvenanceChip(status = quest.verificationStatus)
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Completion Trigger Button
            if (!quest.isCompleted) {
                OutlinedButton(
                    onClick = onComplete,
                    enabled = !isProcessing,
                    modifier = Modifier.testTag("complete_quest_${quest.id}"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isProcessing) SystemColors.SubtleBorder else SystemColors.ElectricCyan
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = SystemColors.ElectricCyan,
                        disabledContentColor = SystemColors.MutedSilver
                    )
                ) {
                    Text(
                        text = if (isProcessing) "CLEARING..." else "COMPLETE",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            } else {
                Text(
                    text = "CLEARED",
                    style = MaterialTheme.typography.labelSmall,
                    color = SystemColors.VerifiedEmerald
                )
            }
        }
    }
}
