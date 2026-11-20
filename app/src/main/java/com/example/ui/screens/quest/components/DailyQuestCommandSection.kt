package com.example.ui.screens.quest.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Warning
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.Quest
import com.example.ui.components.GlowingProgressBar
import com.example.ui.components.HolographicPanel
import com.example.ui.theme.SystemColors
import com.example.ui.theme.SystemSpacing

@Composable
fun DailyQuestCommandSection(
    dailyQuests: List<Quest>,
    pendingQuestIds: Set<Long>,
    onCompleteQuest: (Long) -> Unit,
    onQuestClick: (Quest) -> Unit,
    modifier: Modifier = Modifier
) {
    if (dailyQuests.isEmpty()) return

    val totalCount = dailyQuests.size
    val completedCount = dailyQuests.count { it.isCompleted }
    val progressFraction = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
    val isAllCleared = completedCount == totalCount && totalCount > 0

    HolographicPanel(
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_quest_command_section"),
        accentColor = if (isAllCleared) SystemColors.VerifiedEmerald else SystemColors.ElectricCyan,
        contentPadding = SystemSpacing.cardPadding
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header & Status Pill
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
                        imageVector = if (isAllCleared) Icons.Rounded.CheckCircle else Icons.Rounded.ElectricBolt,
                        contentDescription = null,
                        tint = if (isAllCleared) SystemColors.VerifiedEmerald else SystemColors.ElectricCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "DAILY PROTOCOL",
                        style = MaterialTheme.typography.titleMedium,
                        color = SystemColors.PureIce
                    )
                }

                Surface(
                    color = if (isAllCleared) {
                        SystemColors.VerifiedEmerald.copy(alpha = 0.16f)
                    } else {
                        SystemColors.ElectricCyan.copy(alpha = 0.16f)
                    },
                    shape = RoundedCornerShape(SystemSpacing.cornerSmall),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isAllCleared) {
                            SystemColors.VerifiedEmerald.copy(alpha = 0.5f)
                        } else {
                            SystemColors.ElectricCyan.copy(alpha = 0.5f)
                        }
                    )
                ) {
                    Text(
                        text = if (isAllCleared) "CLEARED" else "$completedCount / $totalCount COMPLETE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isAllCleared) SystemColors.VerifiedEmerald else SystemColors.ElectricCyan,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Progress Telemetry
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MANDATORY PROTOCOL PROGRESS",
                        style = MaterialTheme.typography.labelSmall,
                        color = SystemColors.MutedSilver
                    )
                    Text(
                        text = "${(progressFraction * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isAllCleared) SystemColors.VerifiedEmerald else SystemColors.ElectricCyan
                    )
                }

                GlowingProgressBar(
                    progress = progressFraction,
                    height = 8.dp,
                    startColor = if (isAllCleared) SystemColors.VerifiedEmerald else SystemColors.ElectricCyan,
                    endColor = if (isAllCleared) SystemColors.VerifiedEmerald else SystemColors.SystemBlue
                )
            }

            // Banner or Sub-alert
            if (isAllCleared) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SystemColors.VerifiedEmerald.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(SystemSpacing.cornerSmall),
                    border = BorderStroke(1.dp, SystemColors.VerifiedEmerald.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shield,
                            contentDescription = null,
                            tint = SystemColors.VerifiedEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Daily training protocol fulfilled. Penalty Quest averted.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SystemColors.PureIce
                        )
                    }
                }
            } else {
                Text(
                    text = "The System monitors execution. Complete all daily directives before midnight.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SystemColors.MutedSilver
                )
            }
        }
    }
}
