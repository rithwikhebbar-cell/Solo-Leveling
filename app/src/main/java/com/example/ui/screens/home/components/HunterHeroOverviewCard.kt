package com.example.ui.screens.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.PlayerProfile
import com.example.ui.components.GlowingProgressBar
import com.example.ui.components.HolographicPanel
import com.example.ui.components.RankBadge
import com.example.ui.theme.SystemColors
import com.example.ui.theme.SystemSpacing

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HunterHeroOverviewCard(
    player: PlayerProfile,
    onInspectStatus: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rankAccent = SystemColors.rankAccent(player.rank)
    val canonicalAttrs = player.attributes.asList()

    HolographicPanel(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SystemSpacing.cornerLarge))
            .clickable(onClick = onInspectStatus)
            .testTag("player_overview_card"),
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
                    .alpha(0.18f)
            )

            // Cybernetic HUD radial illumination overlay
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                rankAccent.copy(alpha = 0.15f),
                                SystemColors.DeepNavy.copy(alpha = 0.88f),
                                SystemColors.ObsidianBlack.copy(alpha = 0.96f)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(SystemSpacing.cardPadding),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Top row: Player identity & Hunter Rank Sigil
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
                                text = "AWAKENED HUNTER // STATUS ACTIVE",
                                style = MaterialTheme.typography.labelSmall,
                                color = SystemColors.ElectricCyan
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = player.codename,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = SystemColors.PureIce,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = "${player.rank.displayName} • ${player.title}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = rankAccent
                        )
                    }

                    // Rank Crest Badge
                    RankBadge(rank = player.rank)
                }

                // 2. Level & XP Telemetry Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = SystemColors.ObsidianBlack.copy(alpha = 0.60f),
                    shape = RoundedCornerShape(SystemSpacing.cornerMedium),
                    border = BorderStroke(1.dp, SystemColors.SubtleBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LEVEL ${player.level} PROGRESSION",
                                style = MaterialTheme.typography.labelSmall,
                                color = SystemColors.MutedSilver
                            )
                            Text(
                                text = "${player.currentLevelXp} / ${player.xpToNextLevel} XP (${(player.levelProgressFraction * 100).toInt()}%)",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                color = SystemColors.ElectricCyan
                            )
                        }

                        GlowingProgressBar(
                            progress = player.levelProgressFraction,
                            height = 6.dp,
                            startColor = SystemColors.ElectricCyan,
                            endColor = SystemColors.MonarchGold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TOTAL XP ACCUMULATED",
                                style = MaterialTheme.typography.labelSmall,
                                color = SystemColors.MutedSilver
                            )
                            Text(
                                text = "${player.totalXp} XP",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = SystemColors.MonarchGold
                            )
                        }
                    }
                }

                // 3. Seven Canonical RPG Attributes Summary Row
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "CORE ATTRIBUTE MATRIX",
                        style = MaterialTheme.typography.labelSmall,
                        color = SystemColors.MutedSilver
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        canonicalAttrs.forEach { attr ->
                            val attrAccent = SystemColors.attributeAccent(attr.type)
                            Surface(
                                color = attrAccent.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(SystemSpacing.cornerSmall),
                                border = BorderStroke(1.dp, attrAccent.copy(alpha = 0.35f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = attr.type.code,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = attrAccent
                                    )
                                    Text(
                                        text = "${attr.totalValue}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = SystemColors.PureIce
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Bottom row: Telemetry badges (Streak & Verified Ratio) and STATUS link
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
                            shape = RoundedCornerShape(SystemSpacing.cornerSmall),
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
                                    fontWeight = FontWeight.Bold,
                                    color = SystemColors.MonarchGold
                                )
                            }
                        }

                        // Verified badge
                        Surface(
                            color = SystemColors.ObsidianBlack.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(SystemSpacing.cornerSmall),
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

                    // Right: Inspect Status Action (Min 48dp touch target)
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(SystemSpacing.cornerSmall))
                            .clickable(onClick = onInspectStatus)
                            .testTag("inspect_attributes_button"),
                        color = SystemColors.ElectricCyan.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, SystemColors.ElectricCyan.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(SystemSpacing.cornerSmall)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "STATUS",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
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
}
