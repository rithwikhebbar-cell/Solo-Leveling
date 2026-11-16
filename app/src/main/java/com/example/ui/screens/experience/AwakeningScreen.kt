package com.example.ui.screens.experience

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.domain.engine.AwakeningScriptEngine
import com.example.domain.model.AwakeningStage
import com.example.domain.model.PlayerProfile
import com.example.ui.components.HolographicPanel
import com.example.ui.components.RankBadge
import com.example.ui.theme.SystemColors

@Composable
fun AwakeningScreen(
    player: PlayerProfile,
    animationsEnabled: Boolean,
    reducedMotionEnabled: Boolean,
    onStageShown: (AwakeningStage) -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stages = AwakeningStage.entries
    var currentIndex by rememberSaveable { mutableIntStateOf(0) }
    val activeStage = stages[currentIndex.coerceIn(0, stages.lastIndex)]
    val allowMotion = animationsEnabled && !reducedMotionEnabled

    LaunchedEffect(activeStage) {
        onStageShown(activeStage)
    }

    val pulseAlpha = if (allowMotion) {
        val infiniteTransition = rememberInfiniteTransition(label = "awakening_pulse")
        val value by infiniteTransition.animateFloat(
            initialValue = 0.25f,
            targetValue = 0.85f,
            animationSpec = infiniteRepeatable(
                animation = tween(1800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "awakening_alpha"
        )
        value
    } else {
        0.55f
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SystemColors.ObsidianBlack,
                        SystemColors.DeepNavy,
                        SystemColors.ObsidianBlack
                    )
                )
            )
            .testTag("awakening_screen"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * 0.42f)
            val baseRadius = minOf(size.width, size.height) * 0.28f
            drawCircle(
                color = SystemColors.ElectricCyan.copy(alpha = pulseAlpha * 0.22f),
                radius = baseRadius * 1.45f,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = SystemColors.ArcaneViolet.copy(alpha = pulseAlpha * 0.30f),
                radius = baseRadius,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .padding(horizontal = 24.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SYSTEM // THE AWAKENING",
                    style = MaterialTheme.typography.labelLarge,
                    color = SystemColors.ElectricCyan
                )
                TextButton(
                    onClick = onComplete,
                    modifier = Modifier.testTag("awakening_skip_button")
                ) {
                    Text(
                        text = "SKIP SEQUENCE",
                        style = MaterialTheme.typography.labelMedium,
                        color = SystemColors.MutedSilver
                    )
                }
            }

            AnimatedContent(
                targetState = activeStage,
                transitionSpec = {
                    if (allowMotion) {
                        fadeIn(tween(350)) togetherWith fadeOut(tween(250))
                    } else {
                        fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                    }
                },
                label = "awakening_stage_content"
            ) { stage ->
                HolographicPanel(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = SystemColors.ElectricCyan,
                    secondaryColor = SystemColors.ArcaneViolet,
                    contentPadding = 24.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            color = SystemColors.ElectricCyan.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, SystemColors.ElectricCyan.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = stage.codeLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = SystemColors.ElectricCyan,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        if (stage == AwakeningStage.RANK_CALIBRATION) {
                            RankBadge(rank = player.rank)
                        } else {
                            Icon(
                                imageVector = if (stage == AwakeningStage.PLAYER_IDENTIFICATION) {
                                    Icons.Rounded.VerifiedUser
                                } else {
                                    Icons.Rounded.AutoAwesome
                                },
                                contentDescription = stage.headline,
                                tint = SystemColors.ElectricCyan,
                                modifier = Modifier.size(48.dp)
                            )
                        }

                        Text(
                            text = stage.headline,
                            style = MaterialTheme.typography.headlineMedium,
                            color = SystemColors.PureIce,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = AwakeningScriptEngine.buildFormattedBody(stage, player),
                            style = MaterialTheme.typography.bodyLarge,
                            color = SystemColors.MetallicSilver,
                            textAlign = TextAlign.Center
                        )

                        LinearProgressIndicator(
                            progress = { (stage.stepIndex + 1).toFloat() / stages.size },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = SystemColors.ElectricCyan,
                            trackColor = SystemColors.SubtleGridLine
                        )

                        Text(
                            text = "Step ${stage.stepIndex + 1} of ${stages.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SystemColors.MutedSilver
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentIndex > 0) {
                    OutlinedButton(
                        onClick = { currentIndex -= 1 },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, SystemColors.PanelBorderCyan)
                    ) {
                        Text("PREVIOUS", color = SystemColors.MetallicSilver)
                    }
                }
                Button(
                    onClick = {
                        if (currentIndex < stages.lastIndex) {
                            currentIndex += 1
                        } else {
                            onComplete()
                        }
                    },
                    modifier = Modifier
                        .weight(1.6f)
                        .testTag("awakening_next_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SystemColors.ElectricCyan,
                        contentColor = SystemColors.ObsidianBlack
                    )
                ) {
                    Text(
                        text = if (currentIndex < stages.lastIndex) "CONTINUE" else "ENTER REALITY OS",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
