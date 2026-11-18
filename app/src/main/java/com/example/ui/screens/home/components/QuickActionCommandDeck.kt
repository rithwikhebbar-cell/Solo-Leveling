package com.example.ui.screens.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Castle
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.AcademicDashboardSummary
import com.example.domain.model.LifeDashboardSummary
import com.example.ui.components.SystemSectionTitle
import com.example.ui.theme.SystemColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickActionCommandDeck(
    onNavigateToPhysical: () -> Unit,
    onNavigateToQuestBoard: () -> Unit,
    onNavigateToStatus: () -> Unit,
    onNavigateToShadowRealm: () -> Unit,
    onNavigateToAiCompanion: () -> Unit,
    onNavigateToVoiceConsole: () -> Unit,
    onNavigateToAcademic: () -> Unit,
    onNavigateToLife: () -> Unit,
    onNavigateToIntelligence: () -> Unit,
    onNavigateToSystemControl: () -> Unit,
    modifier: Modifier = Modifier,
    academicSummary: AcademicDashboardSummary = AcademicDashboardSummary(),
    lifeSummary: LifeDashboardSummary = LifeDashboardSummary(),
    intelligenceHeadline: String = "Adaptive planning, predictions & personalized coaching ready",
    systemHealthStatusLabel: String = "Healthy"
) {
    val academicSubtitle = if (academicSummary.pendingRevisionsCount > 0) {
        "${academicSummary.todayStudyMinutes}m today • ${academicSummary.pendingRevisionsCount} due"
    } else {
        "${academicSummary.todayStudyMinutes}m today • Focus & Revisions"
    }

    val lifeSubtitle = if (lifeSummary.activeHabitsCount > 0) {
        "${lifeSummary.completedHabitsToday}/${lifeSummary.activeHabitsCount} habits • ${lifeSummary.pendingTasksCount} tasks"
    } else {
        "Habits & Routine OS"
    }

    val systemControlSubtitle = "Status: $systemHealthStatusLabel"

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SystemSectionTitle(
            title = "Command Center",
            subtitle = "Direct System Navigation & Subsystems"
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 2
        ) {
            CommandTile(
                title = "TRAINING HUB",
                subtitle = "Workouts & Jogging",
                icon = Icons.Rounded.FitnessCenter,
                accentColor = SystemColors.ElectricCyan,
                testTag = "command_workout",
                onClick = onNavigateToPhysical,
                modifier = Modifier.weight(1f)
            )

            CommandTile(
                title = "QUEST BOARD",
                subtitle = "Daily & Campaign",
                icon = Icons.Rounded.Assignment,
                accentColor = SystemColors.SystemBlue,
                testTag = "command_quests",
                onClick = onNavigateToQuestBoard,
                modifier = Modifier.weight(1f)
            )

            CommandTile(
                title = "SHADOW REALM",
                subtitle = "Monarch & Army",
                icon = Icons.Rounded.Castle,
                accentColor = SystemColors.ArcaneViolet,
                testTag = "command_shadow_realm",
                onClick = onNavigateToShadowRealm,
                modifier = Modifier.weight(1f)
            )

            CommandTile(
                title = "HUNTER STATUS",
                subtitle = "Profile & Traits",
                icon = Icons.Rounded.Person,
                accentColor = SystemColors.MonarchGold,
                testTag = "command_status",
                onClick = onNavigateToStatus,
                modifier = Modifier.weight(1f)
            )

            CommandTile(
                title = "AI COMPANION",
                subtitle = "System Advisor",
                icon = Icons.Rounded.AutoAwesome,
                accentColor = SystemColors.ElectricCyan,
                testTag = "home_open_ai_companion",
                onClick = onNavigateToAiCompanion,
                modifier = Modifier.weight(1f)
            )

            CommandTile(
                title = "VOICE CONSOLE",
                subtitle = "Audio Directives",
                icon = Icons.Rounded.Mic,
                accentColor = SystemColors.ArcaneViolet,
                testTag = "home_open_voice_console",
                onClick = onNavigateToVoiceConsole,
                modifier = Modifier.weight(1f)
            )

            CommandTile(
                title = "ACADEMIC ENGINE",
                subtitle = academicSubtitle,
                icon = Icons.Rounded.School,
                accentColor = SystemColors.SystemBlue,
                testTag = "open_academic_engine_button",
                onClick = onNavigateToAcademic,
                modifier = Modifier.weight(1f)
            )

            CommandTile(
                title = "LIFE & DISCIPLINE",
                subtitle = lifeSubtitle,
                icon = Icons.Rounded.Today,
                accentColor = SystemColors.VerifiedEmerald,
                testTag = "open_life_engine_button",
                onClick = onNavigateToLife,
                modifier = Modifier.weight(1f)
            )

            CommandTile(
                title = "INTELLIGENCE",
                subtitle = "Adaptive Roadmap",
                icon = Icons.Rounded.Timeline,
                accentColor = SystemColors.ElectricCyan,
                testTag = "open_intelligence_center_button",
                onClick = onNavigateToIntelligence,
                modifier = Modifier.weight(1f)
            )

            CommandTile(
                title = "SYSTEM CONTROL",
                subtitle = systemControlSubtitle,
                icon = Icons.Rounded.Settings,
                accentColor = SystemColors.MetallicSilver,
                testTag = "home_system_control_card",
                onClick = onNavigateToSystemControl,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CommandTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag(testTag),
        color = SystemColors.SecondarySurface.copy(alpha = 0.90f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(0.75.dp, accentColor.copy(alpha = 0.40f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = accentColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(0.5.dp, accentColor.copy(alpha = 0.45f))
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = SystemColors.PureIce
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = SystemColors.MutedSilver
                    )
                )
            }
        }
    }
}
