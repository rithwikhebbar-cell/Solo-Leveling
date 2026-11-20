package com.example.ui.screens.home.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.HunterRank
import com.example.ui.components.GlowingProgressBar
import com.example.ui.components.RankBadge
import com.example.ui.theme.LocalHudGlowEnabled
import com.example.ui.theme.SystemColors
import com.example.ui.theme.SystemSpacing

@Composable
fun CinematicSystemHeader(
    dateLabel: String,
    level: Int,
    rank: HunterRank,
    codename: String = "Player",
    title: String? = null,
    levelProgressFraction: Float = 0f,
    reducedMotionEnabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    val hudGlow = LocalHudGlowEnabled.current
    val allowMotion = !reducedMotionEnabled && hudGlow

    val pulseAlpha = if (allowMotion) {
        val transition = rememberInfiniteTransition(label = "system_online_pulse")
        val alpha by transition.animateFloat(
            initialValue = 0.45f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_alpha"
        )
        alpha
    } else {
        0.85f
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = SystemColors.DeepNavy.copy(alpha = 0.90f),
        shape = RoundedCornerShape(SystemSpacing.cornerMedium),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    SystemColors.PanelBorderCyan,
                    SystemColors.PanelBorderViolet,
                    SystemColors.PanelBorderCyan.copy(alpha = 0.3f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: System Tag & Online Pulse on Left, Level & Rank Badge on Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: System branding & status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(SystemColors.VerifiedEmerald.copy(alpha = pulseAlpha)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Shield,
                            contentDescription = "System Shield",
                            tint = SystemColors.ElectricCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "SYSTEM // REALITY OS",
                            style = MaterialTheme.typography.labelSmall,
                            color = SystemColors.ElectricCyan
                        )
                    }

                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = SystemColors.MutedSilver
                    )

                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = SystemColors.MutedSilver
                    )
                }

                // Right: Level and Rank Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = SystemColors.ObsidianBlack.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(SystemSpacing.cornerSmall),
                        border = BorderStroke(1.dp, SystemColors.PanelBorderCyan)
                    ) {
                        Text(
                            text = "LV. ${level.toString().padStart(2, '0')}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = SystemColors.PureIce,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    RankBadge(rank = rank, compact = true)
                }
            }

            // Bottom Row: Welcome Player Greeting & Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Welcome back, $codename",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = SystemColors.PureIce,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!title.isNullOrBlank()) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodySmall,
                            color = SystemColors.MonarchGold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Level Progression Bar Indicator
                if (levelProgressFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .padding(bottom = 4.dp)
                    ) {
                        GlowingProgressBar(
                            progress = levelProgressFraction,
                            height = 4.dp,
                            startColor = SystemColors.ElectricCyan,
                            endColor = SystemColors.SystemBlue
                        )
                    }
                }
            }
        }
    }
}
