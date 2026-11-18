package com.example.ui.screens.home.components

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
import androidx.compose.ui.unit.dp
import com.example.domain.engine.NextRankProgress
import com.example.domain.model.PlayerProfile
import com.example.ui.components.GlowingProgressBar
import com.example.ui.components.HolographicPanel
import com.example.ui.theme.SystemColors

@Composable
fun XpProgressSection(
    player: PlayerProfile,
    nextRankProgress: NextRankProgress,
    modifier: Modifier = Modifier
) {
    HolographicPanel(
        modifier = modifier.fillMaxWidth(),
        accentColor = SystemColors.ElectricCyan,
        secondaryColor = SystemColors.ArcaneViolet,
        contentPadding = 14.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Level & XP Numbers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LEVEL ${player.level} EXPERIENCE",
                    style = MaterialTheme.typography.labelMedium,
                    color = SystemColors.MetallicSilver
                )
                Text(
                    text = "${player.currentLevelXp} / ${player.xpToNextLevel} XP",
                    style = MaterialTheme.typography.labelLarge,
                    color = SystemColors.ElectricCyan
                )
            }

            // Animated XP Progress Bar
            GlowingProgressBar(
                progress = player.levelProgressFraction,
                height = 10.dp,
                startColor = SystemColors.ElectricCyan,
                endColor = SystemColors.ArcaneViolet
            )

            // Lifetime XP & Next Rank Progress Sub-bar
            val nextRank = nextRankProgress.nextRank
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${player.totalXp} TOTAL XP",
                    style = MaterialTheme.typography.labelSmall,
                    color = SystemColors.MutedSilver
                )

                if (nextRank != null) {
                    val pct = (nextRankProgress.overallProgress * 100).toInt()
                    Text(
                        text = "NEXT: ${nextRank.displayName.uppercase()} ($pct%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (nextRankProgress.isEligibleForPromotion) {
                            SystemColors.MonarchGold
                        } else {
                            SystemColors.MutedSilver
                        }
                    )
                } else {
                    Text(
                        text = "MAX RANK ACHIEVED",
                        style = MaterialTheme.typography.labelSmall,
                        color = SystemColors.MonarchGold
                    )
                }
            }

            // Promotion Readiness Indicator (when player qualifies for rank up)
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
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ASCENSION PROTOCOL AVAILABLE",
                            style = MaterialTheme.typography.labelSmall,
                            color = SystemColors.MonarchGold
                        )
                        Text(
                            text = "ELIGIBLE FOR PROMOTION",
                            style = MaterialTheme.typography.labelSmall,
                            color = SystemColors.PureIce
                        )
                    }
                }
            }
        }
    }
}
