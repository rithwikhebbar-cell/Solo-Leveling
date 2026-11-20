package com.example.ui.screens.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoMode
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.engine.HomeNextAction
import com.example.ui.components.HolographicPanel
import com.example.ui.theme.SystemColors
import com.example.ui.theme.SystemSpacing

@Composable
fun NextActionRecommendationCard(
    action: HomeNextAction,
    onActionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = when {
        action.isSafetyOverride -> SystemColors.WarningAmber
        action.isSessionContinuation -> SystemColors.ElectricCyan
        action.category == "RECOVERY" -> SystemColors.VerifiedEmerald
        action.category == "ACADEMIC" -> SystemColors.ArcaneViolet
        action.category == "DISCIPLINE" -> SystemColors.SystemBlue
        action.category == "PHYSICAL" -> SystemColors.VerifiedEmerald
        else -> SystemColors.ElectricCyan
    }

    HolographicPanel(
        modifier = modifier
            .fillMaxWidth()
            .testTag("next_best_action_card"),
        accentColor = accent,
        contentPadding = SystemSpacing.cardPadding
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header: Category Pill & System Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (action.isSafetyOverride) Icons.Rounded.Warning else Icons.Rounded.AutoMode,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (action.isSafetyOverride) "SAFETY PROTOCOL // SYSTEM OVERRIDE" else "NEXT BEST ACTION // SYSTEM RECOMMENDATION",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = accent
                    )
                }

                Surface(
                    color = accent.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(SystemSpacing.cornerSmall),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = action.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Title & Description
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = action.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SystemColors.PureIce
                )
                Text(
                    text = action.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SystemColors.MutedSilver
                )
            }

            // System Rationale Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SystemColors.ObsidianBlack.copy(alpha = 0.65f),
                shape = RoundedCornerShape(SystemSpacing.cornerSmall),
                border = BorderStroke(1.dp, SystemColors.SubtleBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Psychology,
                        contentDescription = null,
                        tint = SystemColors.ElectricCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "RATIONALE: ${action.rationale}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SystemColors.MetallicSilver
                    )
                }
            }

            // Action Button
            Button(
                onClick = { onActionClick(action.destinationRoute) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = SystemSpacing.buttonHeight)
                    .testTag("next_action_button"),
                shape = RoundedCornerShape(SystemSpacing.cornerSmall),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = SystemColors.ObsidianBlack
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = action.actionLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
