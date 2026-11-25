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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.ActiveJoggingSessionState
import com.example.domain.model.WorkoutSession
import com.example.ui.theme.LocalHudGlowEnabled
import com.example.ui.theme.SystemColors
import com.example.ui.theme.SystemSpacing

@Composable
fun ActiveSessionAlertBanner(
    activeJoggingSession: ActiveJoggingSessionState?,
    activeWorkout: WorkoutSession?,
    onContinueSession: (String) -> Unit,
    reducedMotionEnabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isJoggingActive = activeJoggingSession != null
    val isWorkoutActive = activeWorkout != null

    if (!isJoggingActive && !isWorkoutActive) return

    val hudGlow = LocalHudGlowEnabled.current
    val allowMotion = !reducedMotionEnabled && hudGlow

    val pulseAlpha = if (allowMotion) {
        val transition = rememberInfiniteTransition(label = "active_session_pulse")
        val alpha by transition.animateFloat(
            initialValue = 0.5f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_alpha"
        )
        alpha
    } else {
        0.85f
    }

    val title = if (isJoggingActive) "AEROBIC SESSION IN PROGRESS" else "TRAINING WORKOUT ACTIVE"
    val subtitle = if (isJoggingActive) {
        val mins = (activeJoggingSession?.currentElapsedSeconds() ?: 0L) / 60L
        "Jogging Session • Elapsed: $mins min"
    } else {
        "${activeWorkout?.title} • Active Exercise Protocol"
    }
    val icon = if (isJoggingActive) Icons.Rounded.DirectionsRun else Icons.Rounded.FitnessCenter

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("active_session_alert_banner"),
        color = SystemColors.DeepNavy,
        shape = RoundedCornerShape(SystemSpacing.cornerMedium),
        border = BorderStroke(1.dp, SystemColors.ElectricCyan.copy(alpha = pulseAlpha))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SystemColors.ElectricCyan.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = SystemColors.ElectricCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(SystemColors.ElectricCyan.copy(alpha = pulseAlpha))
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SystemColors.ElectricCyan
                        )
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = SystemColors.PureIce
                    )
                }
            }

            Button(
                onClick = { onContinueSession("physical") },
                shape = RoundedCornerShape(SystemSpacing.cornerSmall),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SystemColors.ElectricCyan,
                    contentColor = SystemColors.ObsidianBlack
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "RESUME",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
