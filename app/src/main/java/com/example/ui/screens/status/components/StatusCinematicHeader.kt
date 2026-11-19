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
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.HunterRank
import com.example.ui.components.RankBadge
import com.example.ui.theme.SystemColors

@Composable
fun StatusCinematicHeader(
    level: Int,
    rank: HunterRank,
    modifier: Modifier = Modifier,
    trailingAction: (@Composable () -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("status_screen_header"),
        color = SystemColors.DeepNavy.copy(alpha = 0.88f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    SystemColors.PanelBorderCyan,
                    SystemColors.PanelBorderViolet,
                    SystemColors.PanelBorderCyan.copy(alpha = 0.25f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: System branding & Title
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(SystemColors.ElectricCyan)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SYSTEM // PLAYER PROFILE",
                            style = MaterialTheme.typography.labelSmall,
                            color = SystemColors.ElectricCyan
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "STATUS",
                        style = MaterialTheme.typography.displayLarge,
                        color = SystemColors.PureIce
                    )
                    Text(
                        text = "Hunter Profile & Character Development",
                        style = MaterialTheme.typography.bodySmall,
                        color = SystemColors.MutedSilver
                    )
                }

                // Right: Level pill & Rank Badge & optional trailing
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = SystemColors.ObsidianBlack.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, SystemColors.PanelBorderCyan)
                    ) {
                        Text(
                            text = "LV. ${level.toString().padStart(2, '0')}",
                            style = MaterialTheme.typography.labelMedium,
                            color = SystemColors.PureIce,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    RankBadge(rank = rank, compact = true)
                    trailingAction?.invoke()
                }
            }
        }
    }
}
