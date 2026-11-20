package com.example.ui.screens.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.components.HolographicPanel
import com.example.ui.components.StatusStatPill
import com.example.ui.theme.SystemColors
import com.example.ui.theme.SystemSpacing

@Composable
fun DailyProgressSnapshotSection(
    dailyQuestsCompleted: Int,
    dailyQuestsTotal: Int,
    physicalWorkoutCompleted: Boolean,
    physicalJogMinutes: Int,
    studyMinutesToday: Int,
    studyMinutesGoal: Int,
    habitsCompletedToday: Int,
    habitsTotalToday: Int,
    streakDays: Int,
    modifier: Modifier = Modifier
) {
    HolographicPanel(
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_progress_snapshot_section"),
        accentColor = SystemColors.ElectricCyan,
        contentPadding = SystemSpacing.cardPadding
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header
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
                        imageVector = Icons.Rounded.Timeline,
                        contentDescription = null,
                        tint = SystemColors.ElectricCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "DAILY EXECUTION SNAPSHOT",
                        style = MaterialTheme.typography.labelSmall,
                        color = SystemColors.ElectricCyan
                    )
                }

                Text(
                    text = "REAL-TIME TELEMETRY",
                    style = MaterialTheme.typography.labelSmall,
                    color = SystemColors.MutedSilver
                )
            }

            // Metric pills row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatusStatPill(
                    label = "QUESTS",
                    value = "$dailyQuestsCompleted/$dailyQuestsTotal",
                    modifier = Modifier.weight(1f),
                    accentColor = SystemColors.ElectricCyan
                )

                StatusStatPill(
                    label = "TRAIN",
                    value = if (physicalWorkoutCompleted) "CLEARED" else "${physicalJogMinutes}M",
                    modifier = Modifier.weight(1f),
                    accentColor = SystemColors.VerifiedEmerald
                )

                StatusStatPill(
                    label = "STUDY",
                    value = "${studyMinutesToday}M",
                    modifier = Modifier.weight(1f),
                    accentColor = SystemColors.ArcaneViolet
                )

                StatusStatPill(
                    label = "HABITS",
                    value = "$habitsCompletedToday/$habitsTotalToday",
                    modifier = Modifier.weight(1f),
                    accentColor = SystemColors.SystemBlue
                )

                StatusStatPill(
                    label = "STREAK",
                    value = "${streakDays}D",
                    modifier = Modifier.weight(1f),
                    accentColor = SystemColors.MonarchGold
                )
            }
        }
    }
}
