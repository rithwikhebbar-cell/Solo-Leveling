package com.example.ui.screens.shadow

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.MonarchRank
import com.example.domain.model.Shadow
import com.example.domain.model.ShadowClass
import com.example.domain.model.ShadowDeploymentStatus
import com.example.domain.model.ShadowMonarchProfile
import com.example.domain.model.ShadowRank
import com.example.domain.model.ShadowRarity
import com.example.domain.model.ShadowResourceType
import com.example.domain.model.ShadowStats
import com.example.ui.components.HolographicPanel
import com.example.ui.theme.SystemColors

// ============================================================================
// SHADOW REALM DESIGN TOKENS
// ============================================================================

object ShadowColors {
    val Obsidian = Color(0xFF070A13)
    val DeepNavy = Color(0xFF0D1424)
    val PanelSurface = Color(0xFF10192C)
    val PanelSurfaceElevated = Color(0xFF16223B)
    val Cyan = Color(0xFF00E5FF)
    val CyanDim = Color(0xFF0891B2)
    val Violet = Color(0xFF8B5CF6)
    val VioletDeep = Color(0xFF5B21B6)
    val PureIce = Color(0xFFF1F5F9)
    val Muted = Color(0xFF7D8BA3)
    val Gold = Color(0xFFFBBF24)
    val Crimson = Color(0xFFEF4444)
    val Emerald = Color(0xFF10B981)

    fun rankColor(rank: ShadowRank): Color = when (rank) {
        ShadowRank.E_RANK -> Color(0xFF94A3B8)
        ShadowRank.D_RANK -> Color(0xFF10B981)
        ShadowRank.C_RANK -> Color(0xFF38BDF8)
        ShadowRank.B_RANK -> Cyan
        ShadowRank.A_RANK -> Violet
        ShadowRank.S_RANK -> Gold
        ShadowRank.MONARCH_CLASS -> Color(0xFFF43F5E)
    }

    fun rarityColor(rarity: ShadowRarity): Color = when (rarity) {
        ShadowRarity.COMMON -> Color(0xFF94A3B8)
        ShadowRarity.UNCOMMON -> Color(0xFF22C55E)
        ShadowRarity.RARE -> Color(0xFF38BDF8)
        ShadowRarity.EPIC -> Violet
        ShadowRarity.LEGENDARY -> Gold
        ShadowRarity.MYTHIC -> Color(0xFFFF007A)
    }

    fun classColor(cls: ShadowClass): Color = when (cls) {
        ShadowClass.VANGUARD -> Crimson
        ShadowClass.GUARDIAN -> Emerald
        ShadowClass.RANGER -> Cyan
        ShadowClass.ARCANIST -> Color(0xFF818CF8)
        ShadowClass.TACTICIAN -> Violet
        ShadowClass.SUPPORT -> Gold
    }

    fun resourceColor(type: ShadowResourceType): Color = when (type) {
        ShadowResourceType.SHADOW_ESSENCE -> Cyan
        ShadowResourceType.MONARCH_FRAGMENTS -> Violet
        ShadowResourceType.EVOLUTION_CORES -> Gold
        ShadowResourceType.SHADOW_CRYSTALS -> Color(0xFFFF007A)
    }
}

// ============================================================================
// BADGES
// ============================================================================

@Composable
fun ShadowRankBadge(rank: ShadowRank, modifier: Modifier = Modifier) {
    val color = ShadowColors.rankColor(rank)
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.6f)),
        modifier = modifier
    ) {
        Text(
            text = rank.displayName,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun ShadowClassBadge(shadowClass: ShadowClass, modifier: Modifier = Modifier) {
    val color = ShadowColors.classColor(shadowClass)
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Text(
            text = shadowClass.displayName.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun ShadowRarityBadge(rarity: ShadowRarity, modifier: Modifier = Modifier) {
    val color = ShadowColors.rarityColor(rarity)
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Text(
            text = rarity.displayName,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
            color = color,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun MonarchRankBadge(rank: MonarchRank, modifier: Modifier = Modifier) {
    Surface(
        color = ShadowColors.Violet.copy(alpha = 0.2f),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, ShadowColors.Cyan.copy(alpha = 0.7f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Star,
                contentDescription = null,
                tint = ShadowColors.Gold,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = rank.title.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = ShadowColors.PureIce
            )
        }
    }
}

@Composable
fun ResourcePill(
    type: ShadowResourceType,
    amount: Int,
    modifier: Modifier = Modifier
) {
    val color = ShadowColors.resourceColor(type)
    Surface(
        color = ShadowColors.PanelSurfaceElevated,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$amount",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                color = ShadowColors.PureIce
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = type.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = ShadowColors.Muted
            )
        }
    }
}

// ============================================================================
// STATS BARS
// ============================================================================

@Composable
fun ShadowStatsDisplay(stats: ShadowStats, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        StatBarRow("POWER", stats.power, ShadowColors.Crimson)
        StatBarRow("ENDURANCE", stats.endurance, ShadowColors.Emerald)
        StatBarRow("FOCUS", stats.focus, ShadowColors.Cyan)
        StatBarRow("DISCIPLINE", stats.discipline, ShadowColors.Violet)
        StatBarRow("WISDOM", stats.wisdom, ShadowColors.Gold)
        StatBarRow("ADAPTABILITY", stats.adaptability, Color(0xFF38BDF8))
    }
}

@Composable
private fun StatBarRow(label: String, value: Int, color: Color) {
    val progress = (value / 150f).coerceIn(0f, 1f)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            color = ShadowColors.Muted,
            modifier = Modifier.width(100.dp)
        )
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = ShadowColors.PanelSurfaceElevated
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$value",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
            color = ShadowColors.PureIce,
            modifier = Modifier.width(32.dp)
        )
    }
}

// ============================================================================
// SHADOW CARD (ROSTER ITEM)
// ============================================================================

@Composable
fun ShadowCard(
    shadow: Shadow,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = ShadowColors.rankColor(shadow.currentRank).copy(alpha = 0.35f)
    Card(
        colors = CardDefaults.cardColors(containerColor = ShadowColors.PanelSurface),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank badge / Avatar placeholder
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ShadowColors.rankColor(shadow.currentRank).copy(alpha = 0.15f))
                    .border(1.dp, ShadowColors.rankColor(shadow.currentRank).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            ) {
                Text(
                    text = shadow.currentRank.displayName.take(1),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                    color = ShadowColors.rankColor(shadow.currentRank)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = shadow.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = ShadowColors.PureIce
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    if (shadow.deploymentStatus == ShadowDeploymentStatus.DEPLOYED) {
                        Surface(
                            color = ShadowColors.Emerald.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(3.dp),
                            border = BorderStroke(0.5.dp, ShadowColors.Emerald.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = "DEPLOYED",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                color = ShadowColors.Emerald,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShadowClassBadge(shadow.shadowClass)
                    ShadowRarityBadge(shadow.rarity)
                    Text(
                        text = "Lv. ${shadow.level}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = ShadowColors.Cyan
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${shadow.calculatePowerRating()} PWR",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                    color = ShadowColors.Gold
                )
                Text(
                    text = shadow.evolutionStage.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = ShadowColors.Muted
                )
            }
        }
    }
}

