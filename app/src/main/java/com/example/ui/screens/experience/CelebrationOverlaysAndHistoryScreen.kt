package com.example.ui.screens.experience

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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.model.CelebrationEvent
import com.example.domain.model.CelebrationType
import com.example.domain.model.HunterRank
import com.example.ui.components.HolographicPanel
import com.example.ui.components.RankBadge
import com.example.ui.components.SystemEmptyState
import com.example.ui.components.SystemSectionTitle
import com.example.ui.theme.SystemColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CelebrationModalOverlay(
    event: CelebrationEvent,
    animationsEnabled: Boolean,
    reducedMotionEnabled: Boolean,
    onAcknowledge: () -> Unit,
    onOpenStatus: () -> Unit
) {
    val newRank = HunterRank.entries.firstOrNull { it.name == event.newRankName } ?: HunterRank.E_RANK
    val isRankPromo = event.type == CelebrationType.RANK_PROMOTION
    val accent = if (isRankPromo) SystemColors.rankAccent(newRank) else SystemColors.MonarchGold
    val allowMotion = animationsEnabled && !reducedMotionEnabled

    val glowAlpha = if (allowMotion) {
        val transition = rememberInfiniteTransition(label = "celebration_glow")
        val value by transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 0.85f,
            animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse),
            label = "celebration_alpha"
        )
        value
    } else {
        0.6f
    }

    Dialog(
        onDismissRequest = onAcknowledge,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accent.copy(alpha = glowAlpha * 0.25f),
                            SystemColors.ObsidianBlack.copy(alpha = 0.96f)
                        )
                    )
                )
                .padding(24.dp)
                .testTag("celebration_overlay"),
            contentAlignment = Alignment.Center
        ) {
            HolographicPanel(
                modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(),
                accentColor = accent,
                secondaryColor = SystemColors.ElectricCyan,
                contentPadding = 24.dp
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        color = accent.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, accent)
                    ) {
                        Text(
                            text = if (isRankPromo) "SYSTEM PROMOTION VERIFIED" else "LEVEL ADVANCEMENT",
                            style = MaterialTheme.typography.labelLarge,
                            color = accent,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    if (isRankPromo) {
                        RankBadge(rank = newRank)
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.EmojiEvents,
                            contentDescription = "Level Up",
                            tint = SystemColors.MonarchGold,
                            modifier = Modifier.size(52.dp)
                        )
                    }

                    Text(
                        text = event.headline,
                        style = MaterialTheme.typography.headlineMedium,
                        color = SystemColors.PureIce,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = event.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = SystemColors.MetallicSilver,
                        textAlign = TextAlign.Center
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = SystemColors.ObsidianBlack.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(0.75.dp, SystemColors.PanelBorderCyan)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "PROGRESSION SUMMARY",
                                style = MaterialTheme.typography.labelSmall,
                                color = SystemColors.ElectricCyan
                            )
                            Text(
                                text = "Level ${event.previousLevel} → Level ${event.newLevel} • ${event.previousRankName.replace('_', ' ')} → ${newRank.displayName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SystemColors.PureIce
                            )
                            Text(
                                text = event.attributeDeltaSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = SystemColors.MutedSilver
                            )
                            if (event.titleUnlocked.isNotBlank()) {
                                Text(
                                    text = "Title Unlocked: ${event.titleUnlocked}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SystemColors.MonarchGold
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onAcknowledge()
                                onOpenStatus()
                            },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, accent)
                        ) {
                            Text("INSPECT STATUS", color = accent)
                        }
                        Button(
                            onClick = onAcknowledge,
                            modifier = Modifier.weight(1f).testTag("celebration_acknowledge_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accent,
                                contentColor = SystemColors.ObsidianBlack
                            )
                        ) {
                            Text("CONTINUE", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CelebrationHistoryScreen(
    celebrations: List<CelebrationEvent>,
    onReplayCelebration: (CelebrationEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val fmt = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SystemColors.ObsidianBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().widthIn(max = 760.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SystemSectionTitle(
                    title = "Celebration Vault",
                    subtitle = "${celebrations.size} verified Level-Up & Rank Promotion milestones"
                )
            }
            if (celebrations.isEmpty()) {
                item {
                    SystemEmptyState(
                        title = "No Milestones Celebrated Yet",
                        description = "Level-Up and Rank Promotion celebrations appear automatically whenever verified progression reaches a new threshold."
                    )
                }
            } else {
                items(celebrations, key = { it.celebrationId }) { event ->
                    val newRank = HunterRank.entries.firstOrNull { it.name == event.newRankName } ?: HunterRank.E_RANK
                    val accent = if (event.type == CelebrationType.RANK_PROMOTION) {
                        SystemColors.rankAccent(newRank)
                    } else {
                        SystemColors.MonarchGold
                    }
                    HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = accent) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.MilitaryTech,
                                    contentDescription = event.headline,
                                    tint = accent,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(event.headline, style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
                                    Text(event.message, style = MaterialTheme.typography.bodySmall, color = SystemColors.MetallicSilver)
                                    Text(fmt.format(Date(event.createdAtEpochMs)), style = MaterialTheme.typography.labelSmall, color = SystemColors.MutedSilver)
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = { onReplayCelebration(event) },
                                border = BorderStroke(1.dp, accent),
                                modifier = Modifier.testTag("replay_celebration_${event.celebrationId}")
                            ) {
                                Icon(Icons.Rounded.Replay, contentDescription = "Replay celebration", tint = accent, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("REPLAY", color = accent, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}
