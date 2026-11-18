package com.example.ui.screens.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.PlayerProfile
import com.example.ui.components.HolographicPanel
import com.example.ui.components.RankBadge
import com.example.ui.theme.SystemColors

@Composable
fun HunterHeroOverviewCard(
    player: PlayerProfile,
    onInspectStatus: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rankAccent = SystemColors.rankAccent(player.rank)

    HolographicPanel(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onInspectStatus)
            .testTag("hunter_hero_overview_card"),
        accentColor = rankAccent,
        secondaryColor = SystemColors.ElectricCyan,
        contentPadding = 0.dp,
        isHero = true
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Atmospheric background art layer
            Image(
                painter = painterResource(id = R.drawable.img_system_hud_banner),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(0.20f)
            )

            // Cybernetic HUD radial illumination overlay
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                rankAccent.copy(alpha = 0.14f),
                                SystemColors.DeepNavy.copy(alpha = 0.88f),
                                SystemColors.ObsidianBlack.copy(alpha = 0.96f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Top row: Player identity & Hunter Rank Sigil
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
                                text = "AWAKENED HUNTER",
                                style = MaterialTheme.typography.labelSmall,
                                color = SystemColors.ElectricCyan
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = player.codename,
                            style = MaterialTheme.typography.headlineLarge,
                            color = SystemColors.PureIce,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "${player.rank.displayName} • ${player.title}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = rankAccent
                        )
                    }

                    // Rank Crest
                    RankBadge(rank = player.rank)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom telemetry badges (Streak & Verified Ratio)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left badges: Streak & Verified stats
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Streak badge
                        Surface(
                            color = SystemColors.ObsidianBlack.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(0.75.dp, SystemColors.MonarchGold.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Bolt,
                                    contentDescription = "Streak",
                                    tint = SystemColors.MonarchGold,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${player.streakDays}D STREAK",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SystemColors.MonarchGold
                                )
                            }
                        }

                        // Verified badge
                        Surface(
                            color = SystemColors.ObsidianBlack.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(0.75.dp, SystemColors.VerifiedEmerald.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
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

                    // Right: Inspect Status Action
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text(
                            text = "STATUS",
                            style = MaterialTheme.typography.labelMedium,
                            color = SystemColors.ElectricCyan
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = "Inspect Status",
                            tint = SystemColors.ElectricCyan,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}
