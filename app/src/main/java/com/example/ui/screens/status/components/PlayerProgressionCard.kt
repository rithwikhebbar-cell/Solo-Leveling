package com.example.ui.screens.status.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.engine.NextRankProgress
import com.example.domain.model.PlayerProfile
import com.example.ui.components.GlowingProgressBar
import com.example.ui.components.HolographicPanel
import com.example.ui.components.SectionHeader
import com.example.ui.theme.SystemColors

@Composable
fun PlayerProgressionCard(
    player: PlayerProfile,
    nextRankProgress: NextRankProgress,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SectionHeader(
            title = "Player Progression",
            subtitle = "XP Accumulation & Level Threshold"
        )

        HolographicPanel(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("status_progression_card"),
            accentColor = SystemColors.ElectricCyan,
            secondaryColor = SystemColors.ArcaneViolet,
            contentPadding = 16.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Top Level numbers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "LEVEL ${player.level} -> ${player.level + 1}",
                            style = MaterialTheme.typography.titleMedium,
                            color = SystemColors.PureIce
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "CURRENT THRESHOLD PROGRESS",
                            style = MaterialTheme.typography.labelSmall,
                            color = SystemColors.MutedSilver
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${player.currentLevelXp} / ${player.xpToNextLevel} XP",
                            style = MaterialTheme.typography.titleMedium,
                            color = SystemColors.ElectricCyan
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val pct = (player.levelProgressFraction * 100).toInt()
                        Text(
                            text = "$pct% COMPLETED",
                            style = MaterialTheme.typography.labelSmall,
                            color = SystemColors.MetallicSilver
                        )
                    }
                }

                // Glowing animated progress bar
                GlowingProgressBar(
                    progress = player.levelProgressFraction,
                    height = 9.dp,
                    startColor = SystemColors.ElectricCyan,
                    endColor = SystemColors.ArcaneViolet
                )

                // Bottom: Lifetime Total XP & Next Rank Target
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TOTAL LIFETIME XP: ${player.totalXp}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SystemColors.MutedSilver
                    )

                    val targetRank = nextRankProgress.nextRank
                    if (targetRank != null) {
                        val rankPct = (nextRankProgress.overallProgress * 100).toInt()
                        Text(
                            text = "NEXT RANK: ${targetRank.displayName.uppercase()} ($rankPct%)",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (nextRankProgress.isEligibleForPromotion) SystemColors.MonarchGold else SystemColors.ElectricCyan
                        )
                    } else {
                        Text(
                            text = "MAX RANK ACHIEVED",
                            style = MaterialTheme.typography.labelSmall,
                            color = SystemColors.MonarchGold
                        )
                    }
                }

                // Ascension Eligibility Alert Banner
                if (nextRankProgress.isEligibleForPromotion) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = SystemColors.MonarchGold.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(0.75.dp, SystemColors.MonarchGold.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ASCENSION PROTOCOL UNLOCKED",
                                style = MaterialTheme.typography.labelSmall,
                                color = SystemColors.MonarchGold
                            )
                            Text(
                                text = "READY FOR PROMOTION",
                                style = MaterialTheme.typography.labelSmall,
                                color = SystemColors.PureIce
                            )
                        }
                    }
                }
            }
        }
    }
}
