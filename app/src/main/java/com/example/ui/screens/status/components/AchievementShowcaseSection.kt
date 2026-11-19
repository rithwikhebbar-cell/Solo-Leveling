package com.example.ui.screens.status.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.VerifiedUser
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.AchievementWithStatus
import com.example.domain.model.PlayerTitleDefinition
import com.example.ui.components.HolographicPanel
import com.example.ui.components.SectionHeader
import com.example.ui.theme.SystemColors

@Composable
fun AchievementShowcaseSection(
    equippedTitle: String,
    unlockedTitles: List<PlayerTitleDefinition>,
    unlockedAchievements: List<AchievementWithStatus>,
    onOpenTitles: () -> Unit,
    onOpenAchievements: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SectionHeader(
            title = "Honorific Titles & Achievements",
            subtitle = "${unlockedTitles.size} Titles Unlocked • ${unlockedAchievements.size} Milestones Conquered"
        )

        HolographicPanel(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("status_achievements_card"),
            accentColor = SystemColors.MonarchGold,
            secondaryColor = SystemColors.ArcaneViolet,
            contentPadding = 16.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Equipped Title Banner
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SystemColors.MonarchGold.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, SystemColors.MonarchGold.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MilitaryTech,
                            contentDescription = "Equipped Honorific",
                            tint = SystemColors.MonarchGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ACTIVE HONORIFIC TITLE",
                                style = MaterialTheme.typography.labelSmall,
                                color = SystemColors.MonarchGold
                            )
                            Text(
                                text = equippedTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = SystemColors.PureIce
                            )
                        }
                    }
                }

                // Recent achievements
                if (unlockedAchievements.isNotEmpty()) {
                    Text(
                        text = "RECENT SYSTEM ACHIEVEMENTS",
                        style = MaterialTheme.typography.labelSmall,
                        color = SystemColors.MutedSilver
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        unlockedAchievements.take(3).forEach { ach ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = SystemColors.SecondarySurface,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(0.75.dp, SystemColors.SubtleBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.EmojiEvents,
                                        contentDescription = ach.definition.title,
                                        tint = SystemColors.MonarchGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = ach.definition.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = SystemColors.PureIce,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = ach.definition.criterionDescription,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SystemColors.MutedSilver,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        color = SystemColors.ArcaneViolet.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp),
                                        border = BorderStroke(0.5.dp, SystemColors.ArcaneViolet.copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = ach.definition.rarity.label.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SystemColors.ArcaneViolet,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Complete verified training and study quests to unlock honorific titles and system achievement badges.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SystemColors.MutedSilver
                    )
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenTitles,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("open_titles_button"),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, SystemColors.MonarchGold)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MilitaryTech,
                            contentDescription = null,
                            tint = SystemColors.MonarchGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "TITLE VAULT",
                            color = SystemColors.MonarchGold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    OutlinedButton(
                        onClick = onOpenAchievements,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("open_achievements_button"),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, SystemColors.ElectricCyan)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = null,
                            tint = SystemColors.ElectricCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ACHIEVEMENTS",
                            color = SystemColors.ElectricCyan,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}
