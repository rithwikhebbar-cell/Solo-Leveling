package com.example.ui.screens.status.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.PlayerProfile
import com.example.ui.components.HolographicPanel
import com.example.ui.components.RankBadge
import com.example.ui.components.StatusStatPill
import com.example.ui.theme.SystemColors

@Composable
fun HunterIdentityHeroCard(
    player: PlayerProfile,
    equippedTitle: String,
    modifier: Modifier = Modifier
) {
    val rankAccent = SystemColors.rankAccent(player.rank)

    HolographicPanel(
        modifier = modifier
            .fillMaxWidth()
            .testTag("status_identity_hero_card"),
        accentColor = rankAccent,
        secondaryColor = SystemColors.ElectricCyan,
        isHero = true,
        contentPadding = 18.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Identity Header: Codename, Honorific Title, Rank Crest
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(SystemColors.ElectricCyan)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "STATUS WINDOW // SOVEREIGN PROFILE",
                            style = MaterialTheme.typography.labelSmall,
                            color = SystemColors.ElectricCyan
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = player.codename,
                        style = MaterialTheme.typography.displayLarge,
                        color = SystemColors.PureIce,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${player.rank.displayName} • $equippedTitle",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = rankAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                RankBadge(rank = player.rank)
            }

            // Stat Telemetry Pills: LEVEL, TOTAL XP, TOTAL ATTR, RECOVERY
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusStatPill(
                    label = "LEVEL",
                    value = "${player.level}",
                    modifier = Modifier.weight(1f),
                    accentColor = SystemColors.ElectricCyan
                )
                StatusStatPill(
                    label = "TOTAL XP",
                    value = "${player.totalXp}",
                    modifier = Modifier.weight(1f),
                    accentColor = SystemColors.MonarchGold
                )
                StatusStatPill(
                    label = "TOTAL ATTR",
                    value = "${player.attributes.totalScore}",
                    modifier = Modifier.weight(1f),
                    accentColor = SystemColors.ArcaneViolet
                )
                StatusStatPill(
                    label = "RECOVERY",
                    value = "${player.recoveryScore}%",
                    modifier = Modifier.weight(1f),
                    accentColor = SystemColors.VerifiedEmerald
                )
            }

            // Sub-telemetry: Streak & Verified Ratio Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = SystemColors.ObsidianBlack.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(0.75.dp, SystemColors.MonarchGold.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = "Streak",
                            tint = SystemColors.MonarchGold,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${player.streakDays}D STREAK",
                            style = MaterialTheme.typography.labelSmall,
                            color = SystemColors.MonarchGold
                        )
                    }
                }

                Surface(
                    color = SystemColors.ObsidianBlack.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(0.75.dp, SystemColors.VerifiedEmerald.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.VerifiedUser,
                            contentDescription = "Verified ratio",
                            tint = SystemColors.VerifiedEmerald,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${player.verifiedActivitiesCount}/${player.totalActivitiesCount} VERIFIED",
                            style = MaterialTheme.typography.labelSmall,
                            color = SystemColors.MetallicSilver
                        )
                    }
                }
            }
        }
    }
}
