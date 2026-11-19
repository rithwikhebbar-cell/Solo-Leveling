package com.example.ui.screens.status.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.engine.NextRankProgress
import com.example.domain.model.HunterRank
import com.example.domain.model.PlayerProfile
import com.example.ui.components.GlowingProgressBar
import com.example.ui.components.HolographicPanel
import com.example.ui.components.SectionHeader
import com.example.ui.theme.SystemColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RankAscensionSection(
    player: PlayerProfile,
    nextRankProgress: NextRankProgress,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SectionHeader(
            title = "Rank Ascension Hierarchy",
            subtitle = "Authoritative Promotion Criteria & Evaluation"
        )

        HolographicPanel(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("status_rank_ascension_panel"),
            accentColor = SystemColors.ArcaneViolet,
            secondaryColor = SystemColors.MonarchGold,
            contentPadding = 16.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                val targetRank = nextRankProgress.nextRank
                if (targetRank != null) {
                    val rankColor = SystemColors.rankAccent(targetRank)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "NEXT RANK PROMOTION",
                                style = MaterialTheme.typography.labelSmall,
                                color = SystemColors.MutedSilver
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${targetRank.displayName.uppercase()} (${targetRank.title})",
                                style = MaterialTheme.typography.titleLarge,
                                color = rankColor
                            )
                        }

                        Surface(
                            color = if (nextRankProgress.isEligibleForPromotion) SystemColors.MonarchGold.copy(alpha = 0.16f) else SystemColors.DeepNavy,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                1.dp,
                                if (nextRankProgress.isEligibleForPromotion) SystemColors.MonarchGold else SystemColors.SubtleBorder
                            )
                        ) {
                            Text(
                                text = if (nextRankProgress.isEligibleForPromotion) "ELIGIBLE FOR PROMOTION" else "IN PROGRESS",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (nextRankProgress.isEligibleForPromotion) SystemColors.MonarchGold else SystemColors.ElectricCyan,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    // Three Promotion Threshold Progress Rows
                    AscensionThresholdRow(
                        label = "Minimum Level Requirement",
                        current = "${player.level}",
                        target = "${targetRank.threshold.minLevel}",
                        progress = nextRankProgress.levelProgress
                    )

                    AscensionThresholdRow(
                        label = "Minimum Total Attribute Score",
                        current = "${player.attributes.totalScore}",
                        target = "${targetRank.threshold.minTotalAttributes}",
                        progress = nextRankProgress.attributeProgress
                    )

                    AscensionThresholdRow(
                        label = "Verified Real-World Sessions",
                        current = "${player.verifiedActivitiesCount}",
                        target = "${targetRank.threshold.minVerifiedActivities}",
                        progress = nextRankProgress.verifiedProgress
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "NATIONAL LEVEL ACHIEVED",
                            style = MaterialTheme.typography.titleLarge,
                            color = SystemColors.MonarchGold
                        )
                        Text(
                            text = "All System Rank thresholds surpassed. The Sovereign stands at the apex of global hunters.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SystemColors.MetallicSilver
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Flow Row of all 7 ranks (unlocked vs locked)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HunterRank.entries.forEach { rank ->
                        val isUnlocked = player.rank.ordinal >= rank.ordinal
                        val isCurrent = player.rank == rank
                        val accent = SystemColors.rankAccent(rank)

                        Surface(
                            color = when {
                                isCurrent -> accent.copy(alpha = 0.22f)
                                isUnlocked -> SystemColors.PanelSurfaceElevated
                                else -> SystemColors.SecondarySurface
                            },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                width = if (isCurrent) 1.25.dp else 0.75.dp,
                                color = if (isCurrent) accent else if (isUnlocked) accent.copy(alpha = 0.4f) else SystemColors.SubtleBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isUnlocked) Icons.Rounded.MilitaryTech else Icons.Rounded.Lock,
                                    contentDescription = rank.displayName,
                                    tint = if (isUnlocked) accent else SystemColors.MutedSilver,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = rank.displayName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isUnlocked) SystemColors.PureIce else SystemColors.MutedSilver
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AscensionThresholdRow(
    label: String,
    current: String,
    target: String,
    progress: Float
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = SystemColors.MetallicSilver
            )
            Text(
                text = "$current / $target",
                style = MaterialTheme.typography.labelLarge,
                color = SystemColors.ElectricCyan
            )
        }
        GlowingProgressBar(
            progress = progress,
            height = 6.dp,
            startColor = SystemColors.ElectricCyan,
            endColor = SystemColors.VerifiedEmerald
        )
    }
}
