package com.example.ui.screens.status.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.AttributeBreakdown
import com.example.ui.components.GlowingProgressBar
import com.example.ui.theme.SystemColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttributeDetailBottomSheet(
    attribute: AttributeBreakdown?,
    onDismiss: () -> Unit
) {
    if (attribute == null) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val accent = SystemColors.attributeAccent(attribute.type)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SystemColors.DeepNavy,
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 36.dp, height = 4.dp),
                color = SystemColors.MutedSilver.copy(alpha = 0.4f),
                shape = CircleShape
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .testTag("attribute_detail_sheet"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with code, display name, and dismiss button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = accent.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = attribute.type.code,
                            style = MaterialTheme.typography.titleMedium,
                            color = accent,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = attribute.type.displayName.uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            color = SystemColors.PureIce
                        )
                        Text(
                            text = "TOTAL RATING: ${attribute.totalValue} PTS",
                            style = MaterialTheme.typography.labelSmall,
                            color = accent
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = SystemColors.MutedSilver
                    )
                }
            }

            // Description card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SystemColors.PanelSurface,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(0.75.dp, SystemColors.SubtleBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "ATTRIBUTE SPECIFICATION",
                        style = MaterialTheme.typography.labelSmall,
                        color = SystemColors.MutedSilver
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = attribute.type.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SystemColors.PureIce
                    )
                }
            }

            // Training Focus and Contributing Domain Activities
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SystemColors.PanelSurface,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(0.75.dp, accent.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Insights,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "PRIMARY TRAINING FOCUS",
                            style = MaterialTheme.typography.labelSmall,
                            color = accent
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = attribute.type.trainingFocus,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SystemColors.PureIce
                    )
                }
            }

            // Point Provenance Breakdown
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SystemColors.ObsidianBlack.copy(alpha = 0.7f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(0.75.dp, SystemColors.SubtleBorder)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "POINT PROVENANCE & BREAKDOWN",
                        style = MaterialTheme.typography.labelSmall,
                        color = SystemColors.MutedSilver
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Base Score", style = MaterialTheme.typography.bodyMedium, color = SystemColors.MutedSilver)
                        Text(text = "${attribute.baseValue} PTS", style = MaterialTheme.typography.labelMedium, color = SystemColors.PureIce)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Verified Bonus (Health Connect / Assessment)", style = MaterialTheme.typography.bodyMedium, color = SystemColors.VerifiedEmerald)
                        Text(text = "+${attribute.verifiedPoints} PTS", style = MaterialTheme.typography.labelMedium, color = SystemColors.VerifiedEmerald)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Self-Reported Bonus", style = MaterialTheme.typography.bodyMedium, color = SystemColors.WarningAmber)
                        Text(text = "+${attribute.selfReportedPoints} PTS", style = MaterialTheme.typography.labelMedium, color = SystemColors.WarningAmber)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Equipment & Artifact Bonus", style = MaterialTheme.typography.bodyMedium, color = SystemColors.MutedSilver)
                        Text(text = "+0 PTS", style = MaterialTheme.typography.labelMedium, color = SystemColors.MutedSilver)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Point Allocation Mode", style = MaterialTheme.typography.bodyMedium, color = SystemColors.MutedSilver)
                        Text(text = "Deterministic Activity Earned", style = MaterialTheme.typography.labelMedium, color = accent)
                    }
                }
            }

            // XP Progression to Next Attribute Point
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "NEXT POINT THRESHOLD",
                        style = MaterialTheme.typography.labelSmall,
                        color = SystemColors.MutedSilver
                    )
                    Text(
                        text = "${attribute.progressXp} / ${attribute.progressXpRequired} XP",
                        style = MaterialTheme.typography.labelMedium,
                        color = accent
                    )
                }

                GlowingProgressBar(
                    progress = attribute.progressFraction,
                    height = 8.dp,
                    startColor = accent,
                    endColor = SystemColors.ElectricCyan
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Dismiss Button
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SystemColors.SecondarySurface,
                    contentColor = SystemColors.PureIce
                ),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, SystemColors.SubtleBorder)
            ) {
                Text(text = "CLOSE DETAILS", style = MaterialTheme.typography.labelMedium)
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
