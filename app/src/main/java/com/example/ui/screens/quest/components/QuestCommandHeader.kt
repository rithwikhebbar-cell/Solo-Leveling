package com.example.ui.screens.quest.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.components.StatusStatPill
import com.example.ui.theme.SystemColors
import com.example.ui.theme.SystemSpacing

enum class QuestTabFilter(val label: String) {
    ALL("All Missions"),
    DAILY("Daily Protocol"),
    WEEKLY("Weekly Directives"),
    LONG_TERM("Milestones"),
    COMPLETED("Cleared Archive")
}

@Composable
fun QuestCommandHeader(
    selectedTab: QuestTabFilter,
    onTabSelected: (QuestTabFilter) -> Unit,
    activeCount: Int,
    completedCount: Int,
    xpPoolAvailable: Int,
    streakDays: Int,
    onNewQuest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SystemSpacing.sectionSpacing)
    ) {
        // 1. System telemetry banner & Title with Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(SystemColors.ElectricCyan)
                    )
                    Text(
                        text = "SYSTEM // QUEST MANAGEMENT",
                        style = MaterialTheme.typography.labelSmall,
                        color = SystemColors.ElectricCyan
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "QUESTS",
                    style = MaterialTheme.typography.displayMedium,
                    color = SystemColors.PureIce
                )

                Text(
                    text = "Your missions. Your discipline. Your evolution.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SystemColors.MutedSilver
                )
            }

            Button(
                onClick = onNewQuest,
                modifier = Modifier
                    .heightIn(min = SystemSpacing.buttonHeight)
                    .testTag("new_quest_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SystemColors.ElectricCyan,
                    contentColor = SystemColors.ObsidianBlack
                ),
                shape = RoundedCornerShape(SystemSpacing.cornerMedium),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "NEW QUEST",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        // 2. Primary Mission Control Telemetry Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatusStatPill(
                label = "ACTIVE",
                value = "$activeCount",
                modifier = Modifier.weight(1f),
                accentColor = SystemColors.ElectricCyan
            )
            StatusStatPill(
                label = "CLEARED",
                value = "$completedCount",
                modifier = Modifier.weight(1f),
                accentColor = SystemColors.VerifiedEmerald
            )
            StatusStatPill(
                label = "XP POOL",
                value = "+$xpPoolAvailable",
                modifier = Modifier.weight(1f),
                accentColor = SystemColors.MonarchGold
            )
            StatusStatPill(
                label = "STREAK",
                value = "${streakDays}D",
                modifier = Modifier.weight(1f),
                accentColor = SystemColors.ArcaneViolet
            )
        }

        // 3. Tactical Category Selector
        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            QuestTabFilter.entries.forEach { tab ->
                val isSelected = tab == selectedTab
                val activeColor = SystemColors.ElectricCyan

                val icon = when (tab) {
                    QuestTabFilter.ALL -> Icons.Rounded.Assignment
                    QuestTabFilter.DAILY -> Icons.Rounded.Today
                    QuestTabFilter.WEEKLY -> Icons.Rounded.DateRange
                    QuestTabFilter.LONG_TERM -> Icons.Rounded.Flag
                    QuestTabFilter.COMPLETED -> Icons.Rounded.CheckCircle
                }

                Surface(
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onTabSelected(tab) }
                        .testTag("quest_tab_${tab.name.lowercase()}"),
                    color = if (isSelected) activeColor.copy(alpha = 0.16f) else SystemColors.SecondarySurface,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(
                        width = if (isSelected) 1.dp else 0.75.dp,
                        color = if (isSelected) activeColor.copy(alpha = 0.8f) else SystemColors.SubtleBorder
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(activeColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) activeColor else SystemColors.MutedSilver,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) activeColor else SystemColors.MutedSilver
                        )
                    }
                }
            }
        }
    }
}
