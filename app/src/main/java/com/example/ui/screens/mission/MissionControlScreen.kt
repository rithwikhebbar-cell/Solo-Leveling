package com.example.ui.screens.mission

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.MissionCategory
import com.example.domain.model.MissionPriority
import com.example.domain.model.MissionSourceModule
import com.example.domain.model.MissionStatus
import com.example.domain.model.RecoveryState
import com.example.domain.model.UnifiedMissionItem
import com.example.ui.components.GlowingProgressBar
import com.example.ui.components.HolographicPanel
import com.example.ui.components.SystemEmptyState
import com.example.ui.components.SystemErrorState
import com.example.ui.components.SystemLoadingView
import com.example.ui.components.SystemSectionTitle
import com.example.ui.theme.SystemColors
import com.example.ui.viewmodel.MissionControlUiState
import java.time.LocalDate

enum class MissionFilterTab(val label: String) {
    ALL("All"),
    PRIORITY("Priority Queue"),
    DAILY("Daily"),
    WEEKLY("Weekly"),
    LONG_TERM("Long-Term"),
    HISTORY("History")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MissionControlScreen(
    uiState: MissionControlUiState,
    onClearFeedback: () -> Unit,
    onCreateMission: (
        title: String,
        description: String,
        category: MissionCategory,
        priority: MissionPriority,
        module: MissionSourceModule,
        targetValue: Int,
        unit: String,
        startDateEpochDay: Long?,
        deadlineEpochDay: Long?,
        prerequisiteMissionIds: List<String>
    ) -> Unit,
    onUpdateMission: (
        missionId: String,
        title: String,
        description: String,
        priority: MissionPriority,
        module: MissionSourceModule,
        targetValue: Int,
        unit: String,
        startDateEpochDay: Long?,
        deadlineEpochDay: Long?,
        prerequisiteMissionIds: List<String>
    ) -> Unit,
    onTogglePause: (String) -> Unit,
    onArchiveMission: (String) -> Unit,
    onCompleteMission: (String) -> Unit,
    onExecuteRoute: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableStateOf(MissionFilterTab.ALL) }
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var editingMission by remember { mutableStateOf<UnifiedMissionItem?>(null) }
    var archivingMission by remember { mutableStateOf<UnifiedMissionItem?>(null) }

    if (selectedTab != MissionFilterTab.ALL) {
        BackHandler {
            selectedTab = MissionFilterTab.ALL
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = SystemColors.ObsidianBlack,
        floatingActionButton = {
            if (uiState is MissionControlUiState.Ready) {
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = SystemColors.ElectricCyan,
                    contentColor = SystemColors.ObsidianBlack,
                    modifier = Modifier.testTag("mission_create_fab")
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Create New Mission")
                }
            }
        }
    ) { innerPadding ->
        when (uiState) {
            is MissionControlUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding)
            ) { SystemLoadingView() }

            is MissionControlUiState.Error -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(20.dp)
            ) {
                SystemErrorState(message = uiState.message, onRetry = {})
            }

            is MissionControlUiState.Ready -> {
                val snapshot = uiState.snapshot
                val overview = snapshot.overview
                val isRecoveryRestricted = snapshot.recoveryState == RecoveryState.REST_RECOMMENDED ||
                    snapshot.recoveryState == RecoveryState.RECOVERY_RECOMMENDED

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(SystemColors.ObsidianBlack),
                    contentAlignment = Alignment.TopCenter
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 760.dp),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (uiState.isBusy) {
                            item {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = SystemColors.ElectricCyan,
                                    trackColor = SystemColors.PanelSurface
                                )
                            }
                        }

                        if (!uiState.feedbackMessage.isNullOrBlank()) {
                            item {
                                HolographicPanel(accentColor = SystemColors.ElectricCyan, contentPadding = 12.dp) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = uiState.feedbackMessage,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SystemColors.PureIce,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = onClearFeedback,
                                            modifier = Modifier.testTag("dismiss_mission_feedback")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = "Dismiss message",
                                                tint = SystemColors.ElectricCyan
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (isRecoveryRestricted) {
                            item {
                                HolographicPanel(accentColor = SystemColors.DangerCrimson, contentPadding = 12.dp) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Warning,
                                            contentDescription = "Recovery Alert",
                                            tint = SystemColors.DangerCrimson
                                        )
                                        Column {
                                            Text(
                                                text = "RECOVERY GUARDRAIL ACTIVE (${snapshot.recoveryState.code})",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = SystemColors.DangerCrimson
                                            )
                                            Text(
                                                text = "Demanding physical missions are deprioritized in your Priority Queue until readiness recovers.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = SystemColors.PureIce
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // SECTION 1: MISSION OVERVIEW
                        item {
                            HolographicPanel(
                                accentColor = SystemColors.ElectricCyan,
                                secondaryColor = SystemColors.ArcaneViolet
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        SystemSectionTitle(
                                            title = "1. Mission Overview",
                                            subtitle = "Unified Daily, Weekly & Long-Term Telemetry"
                                        )
                                        OutlinedButton(
                                            onClick = { showCreateDialog = true },
                                            modifier = Modifier.testTag("mission_overview_create_btn"),
                                            border = BorderStroke(1.dp, SystemColors.ElectricCyan)
                                        ) {
                                            Text("NEW", color = SystemColors.ElectricCyan)
                                        }
                                    }

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        MissionStatBadge("Active", overview.activeCount.toString(), SystemColors.ElectricCyan)
                                        MissionStatBadge("Completed", overview.completedCount.toString(), SystemColors.VerifiedEmerald)
                                        MissionStatBadge(
                                            "Overdue",
                                            overview.overdueCount.toString(),
                                            if (overview.overdueCount > 0) SystemColors.DangerCrimson else SystemColors.MutedSilver
                                        )
                                        MissionStatBadge("Blocked", overview.blockedCount.toString(), SystemColors.WarningAmber)
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(
                                                "Daily Objectives (${overview.dailyCompletedCount}/${overview.dailyTotalCount})",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = SystemColors.PureIce
                                            )
                                            Text(
                                                "${overview.dailyCompletionRatePercent}%",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = SystemColors.ElectricCyan
                                            )
                                        }
                                        GlowingProgressBar(
                                            progress = overview.dailyCompletionRatePercent / 100f,
                                            height = 7.dp,
                                            startColor = SystemColors.ElectricCyan,
                                            endColor = SystemColors.VerifiedEmerald
                                        )
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(
                                                "Weekly Mission Progress (${overview.weeklyCompletedCount}/${overview.weeklyTotalCount})",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = SystemColors.PureIce
                                            )
                                            Text(
                                                "${overview.weeklyProgressPercent}%",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = SystemColors.ArcaneViolet
                                            )
                                        }
                                        GlowingProgressBar(
                                            progress = overview.weeklyProgressPercent / 100f,
                                            height = 7.dp,
                                            startColor = SystemColors.ArcaneViolet,
                                            endColor = SystemColors.ElectricCyan
                                        )
                                    }

                                    if (overview.upcomingDeadlines.isNotEmpty()) {
                                        Text(
                                            text = "UPCOMING DEADLINES",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SystemColors.WarningAmber
                                        )
                                        overview.upcomingDeadlines.take(3).forEach { dl ->
                                            val dlText = dl.deadlineEpochDay?.let { LocalDate.ofEpochDay(it).toString() } ?: "Scheduled"
                                            Text(
                                                text = "• ${dl.title} ($dlText)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (dl.isOverdue) SystemColors.DangerCrimson else SystemColors.MetallicSilver
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Filter Chips
                        item {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                MissionFilterTab.entries.forEach { tab ->
                                    FilterChip(
                                        selected = selectedTab == tab,
                                        onClick = { selectedTab = tab },
                                        label = { Text(tab.label) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = SystemColors.ElectricCyan.copy(alpha = 0.22f)
                                        ),
                                        modifier = Modifier.testTag("mission_tab_${tab.name.lowercase()}")
                                    )
                                }
                            }
                        }

                        // SECTION 2: PRIORITY QUEUE
                        if (selectedTab == MissionFilterTab.ALL || selectedTab == MissionFilterTab.PRIORITY) {
                            item {
                                SystemSectionTitle(
                                    title = "2. Priority Queue",
                                    subtitle = "Ranked by deadline urgency, priority, dependencies & recovery"
                                )
                            }
                            if (snapshot.priorityQueue.isEmpty()) {
                                item {
                                    SystemEmptyState(
                                        title = "Priority Queue Clear",
                                        description = "No pending active missions queued. Create an objective or open a domain hub."
                                    )
                                }
                            } else {
                                items(snapshot.priorityQueue, key = { "pq_${it.missionId}" }) { mission ->
                                    MissionItemCard(
                                        mission = mission,
                                        onComplete = { onCompleteMission(mission.missionId) },
                                        onTogglePause = { onTogglePause(mission.missionId) },
                                        onEdit = { editingMission = mission },
                                        onArchive = { archivingMission = mission },
                                        onExecute = { onExecuteRoute(mission.executeRoute) }
                                    )
                                }
                            }
                        }

                        // SECTION 3: ACTIVE MISSIONS
                        if (selectedTab == MissionFilterTab.ALL) {
                            item {
                                SystemSectionTitle(
                                    title = "3. Active Missions",
                                    subtitle = "${snapshot.activeMissions.size} in progress or ready to execute"
                                )
                            }
                            if (snapshot.activeMissions.isEmpty()) {
                                item {
                                    SystemEmptyState(
                                        title = "No Active Missions",
                                        description = "Tap + to schedule a Daily, Weekly, or Long-Term mission."
                                    )
                                }
                            } else {
                                items(snapshot.activeMissions, key = { "active_${it.missionId}" }) { mission ->
                                    MissionItemCard(
                                        mission = mission,
                                        onComplete = { onCompleteMission(mission.missionId) },
                                        onTogglePause = { onTogglePause(mission.missionId) },
                                        onEdit = { editingMission = mission },
                                        onArchive = { archivingMission = mission },
                                        onExecute = { onExecuteRoute(mission.executeRoute) }
                                    )
                                }
                            }
                        }

                        // SECTION 4: DAILY OBJECTIVES
                        if (selectedTab == MissionFilterTab.ALL || selectedTab == MissionFilterTab.DAILY) {
                            item {
                                SystemSectionTitle(
                                    title = "4. Daily Objectives",
                                    subtitle = "${overview.dailyCompletedCount}/${overview.dailyTotalCount} fulfilled today"
                                )
                            }
                            if (snapshot.dailyMissions.isEmpty()) {
                                item {
                                    SystemEmptyState(
                                        title = "No Daily Objectives",
                                        description = "Add daily missions or review Quest Board directives."
                                    )
                                }
                            } else {
                                items(snapshot.dailyMissions, key = { "daily_${it.missionId}" }) { mission ->
                                    MissionItemCard(
                                        mission = mission,
                                        onComplete = { onCompleteMission(mission.missionId) },
                                        onTogglePause = { onTogglePause(mission.missionId) },
                                        onEdit = { editingMission = mission },
                                        onArchive = { archivingMission = mission },
                                        onExecute = { onExecuteRoute(mission.executeRoute) }
                                    )
                                }
                            }
                        }

                        // SECTION 5: WEEKLY OBJECTIVES
                        if (selectedTab == MissionFilterTab.ALL || selectedTab == MissionFilterTab.WEEKLY) {
                            item {
                                SystemSectionTitle(
                                    title = "5. Weekly Objectives",
                                    subtitle = "${overview.weeklyCompletedCount}/${overview.weeklyTotalCount} weekly directives"
                                )
                            }
                            if (snapshot.weeklyMissions.isEmpty()) {
                                item {
                                    SystemEmptyState(
                                        title = "No Weekly Objectives",
                                        description = "Schedule weekly sprints or recurring weekly objectives."
                                    )
                                }
                            } else {
                                items(snapshot.weeklyMissions, key = { "weekly_${it.missionId}" }) { mission ->
                                    MissionItemCard(
                                        mission = mission,
                                        onComplete = { onCompleteMission(mission.missionId) },
                                        onTogglePause = { onTogglePause(mission.missionId) },
                                        onEdit = { editingMission = mission },
                                        onArchive = { archivingMission = mission },
                                        onExecute = { onExecuteRoute(mission.executeRoute) }
                                    )
                                }
                            }
                        }

                        // SECTION 6: LONG-TERM GOALS
                        if (selectedTab == MissionFilterTab.ALL || selectedTab == MissionFilterTab.LONG_TERM) {
                            item {
                                SystemSectionTitle(
                                    title = "6. Long-Term Goals",
                                    subtitle = "${snapshot.longTermMissions.size} strategic campaign objectives"
                                )
                            }
                            if (snapshot.longTermMissions.isEmpty()) {
                                item {
                                    SystemEmptyState(
                                        title = "No Long-Term Goals",
                                        description = "Create a long-term goal to track milestones and timeline trajectory."
                                    )
                                }
                            } else {
                                items(snapshot.longTermMissions, key = { "lt_${it.missionId}" }) { mission ->
                                    MissionItemCard(
                                        mission = mission,
                                        onComplete = { onCompleteMission(mission.missionId) },
                                        onTogglePause = { onTogglePause(mission.missionId) },
                                        onEdit = { editingMission = mission },
                                        onArchive = { archivingMission = mission },
                                        onExecute = { onExecuteRoute(mission.executeRoute) }
                                    )
                                }
                            }
                        }

                        // SECTION 7: MISSION HISTORY
                        if (selectedTab == MissionFilterTab.ALL || selectedTab == MissionFilterTab.HISTORY) {
                            item {
                                SystemSectionTitle(
                                    title = "7. Mission History",
                                    subtitle = "${snapshot.historyMissions.size} completed, archived or expired missions"
                                )
                            }
                            if (snapshot.historyMissions.isEmpty()) {
                                item {
                                    SystemEmptyState(
                                        title = "No Mission History Yet",
                                        description = "Completed, archived, or failed missions are archived here."
                                    )
                                }
                            } else {
                                items(snapshot.historyMissions, key = { "hist_${it.missionId}" }) { mission ->
                                    MissionItemCard(
                                        mission = mission,
                                        onComplete = {},
                                        onTogglePause = {},
                                        onEdit = {},
                                        onArchive = {},
                                        onExecute = { onExecuteRoute(mission.executeRoute) }
                                    )
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(72.dp)) }
                    }
                }

                if (showCreateDialog) {
                    MissionEditorDialog(
                        existing = null,
                        availablePrerequisites = snapshot.allNonArchived.filter { !it.status.isTerminal },
                        onDismiss = { showCreateDialog = false },
                        onSave = { title, desc, cat, prio, mod, target, unit, startDay, deadlineDay, prereqs ->
                            onCreateMission(title, desc, cat, prio, mod, target, unit, startDay, deadlineDay, prereqs)
                            showCreateDialog = false
                        }
                    )
                }

                val editTarget = editingMission
                if (editTarget != null) {
                    MissionEditorDialog(
                        existing = editTarget,
                        availablePrerequisites = snapshot.allNonArchived.filter {
                            !it.status.isTerminal && it.missionId != editTarget.missionId
                        },
                        onDismiss = { editingMission = null },
                        onSave = { title, desc, _, prio, mod, target, unit, startDay, deadlineDay, prereqs ->
                            onUpdateMission(
                                editTarget.missionId,
                                title,
                                desc,
                                prio,
                                mod,
                                target,
                                unit,
                                startDay,
                                deadlineDay,
                                prereqs
                            )
                            editingMission = null
                        }
                    )
                }

                val archiveTarget = archivingMission
                if (archiveTarget != null) {
                    AlertDialog(
                        onDismissRequest = { archivingMission = null },
                        containerColor = SystemColors.DeepNavy,
                        title = { Text("Archive Mission?", color = SystemColors.PureIce) },
                        text = {
                            Text(
                                text = "Archive '${archiveTarget.title}' and move it to Mission History? Existing XP and progression logs remain untouched.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SystemColors.MetallicSilver
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    onArchiveMission(archiveTarget.missionId)
                                    archivingMission = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SystemColors.WarningAmber, contentColor = SystemColors.ObsidianBlack)
                            ) {
                                Text("ARCHIVE")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { archivingMission = null }) {
                                Text("CANCEL")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MissionStatBadge(label: String, value: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(0.75.dp, color.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            Text(value, style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = SystemColors.MutedSilver)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MissionItemCard(
    mission: UnifiedMissionItem,
    onComplete: () -> Unit,
    onTogglePause: () -> Unit,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onExecute: () -> Unit
) {
    val accent = when {
        mission.status == MissionStatus.COMPLETED -> SystemColors.VerifiedEmerald
        mission.isOverdue || mission.status == MissionStatus.FAILED -> SystemColors.DangerCrimson
        mission.isBlocked || mission.isRecoveryRestricted -> SystemColors.WarningAmber
        mission.status == MissionStatus.PAUSED -> SystemColors.MutedSilver
        else -> SystemColors.ElectricCyan
    }

    HolographicPanel(
        accentColor = accent,
        modifier = Modifier.testTag("mission_card_${mission.missionId}")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MissionPill(mission.category.label.uppercase(), SystemColors.ElectricCyan)
                MissionPill(mission.module.label.uppercase(), SystemColors.ArcaneViolet)
                MissionPill(mission.status.label.uppercase(), accent)
                MissionPill("P:${mission.priority.label.uppercase()}", SystemColors.MonarchGold)
                if (mission.isOverdue) {
                    MissionPill("OVERDUE", SystemColors.DangerCrimson)
                }
                if (mission.isBlocked) {
                    MissionPill("BLOCKED", SystemColors.WarningAmber)
                }
                if (mission.isRecoveryRestricted) {
                    MissionPill("RECOVERY GUARD", SystemColors.DangerCrimson)
                }
            }

            Text(
                text = mission.title,
                style = MaterialTheme.typography.titleMedium,
                color = SystemColors.PureIce
            )
            if (mission.description.isNotBlank()) {
                Text(
                    text = mission.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = SystemColors.MetallicSilver
                )
            }

            if (mission.priorityReason.isNotBlank() && !mission.status.isTerminal) {
                Text(
                    text = "Priority (${mission.priorityScore}/100): ${mission.priorityReason}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (mission.isBlocked || mission.isRecoveryRestricted) SystemColors.WarningAmber else SystemColors.ElectricCyan
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = "${mission.currentValue}/${mission.targetValue} ${mission.unit}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SystemColors.PureIce
                    )
                    val dl = mission.deadlineEpochDay?.let { "Due: ${LocalDate.ofEpochDay(it)}" } ?: "No deadline"
                    Text(
                        text = dl,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (mission.isOverdue) SystemColors.DangerCrimson else SystemColors.MutedSilver
                    )
                }
                GlowingProgressBar(
                    progress = mission.progressFraction,
                    height = 6.dp,
                    startColor = accent,
                    endColor = SystemColors.ElectricCyan
                )
            }

            if (!mission.status.isTerminal) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = onExecute,
                        modifier = Modifier.testTag("mission_execute_${mission.missionId}"),
                        border = BorderStroke(1.dp, SystemColors.ElectricCyan)
                    ) {
                        Icon(Icons.Rounded.RocketLaunch, contentDescription = "Execute in module", modifier = Modifier.size(16.dp), tint = SystemColors.ElectricCyan)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("EXECUTE", color = SystemColors.ElectricCyan)
                    }

                    if (mission.canCompleteDirectly) {
                        Button(
                            onClick = onComplete,
                            enabled = !mission.isBlocked && mission.status != MissionStatus.PAUSED,
                            modifier = Modifier.testTag("mission_complete_${mission.missionId}"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SystemColors.VerifiedEmerald,
                                contentColor = SystemColors.ObsidianBlack
                            )
                        ) {
                            Icon(
                                imageVector = if (mission.isBlocked) Icons.Rounded.Lock else Icons.Rounded.CheckCircle,
                                contentDescription = "Complete mission",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (mission.isBlocked) "LOCKED" else "COMPLETE")
                        }
                    }

                    if (mission.canPause) {
                        IconButton(
                            onClick = onTogglePause,
                            modifier = Modifier.testTag("mission_pause_${mission.missionId}")
                        ) {
                            Icon(
                                imageVector = if (mission.status == MissionStatus.PAUSED) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                                contentDescription = if (mission.status == MissionStatus.PAUSED) "Resume mission" else "Pause mission",
                                tint = SystemColors.ElectricCyan
                            )
                        }
                    }

                    if (mission.canEdit) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.testTag("mission_edit_${mission.missionId}")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = "Edit mission",
                                tint = SystemColors.MetallicSilver
                            )
                        }
                    }

                    if (mission.canArchive) {
                        IconButton(
                            onClick = onArchive,
                            modifier = Modifier.testTag("mission_archive_${mission.missionId}")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Archive,
                                contentDescription = "Archive mission",
                                tint = SystemColors.WarningAmber
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MissionPill(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(0.75.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MissionEditorDialog(
    existing: UnifiedMissionItem?,
    availablePrerequisites: List<UnifiedMissionItem>,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        description: String,
        category: MissionCategory,
        priority: MissionPriority,
        module: MissionSourceModule,
        targetValue: Int,
        unit: String,
        startDateEpochDay: Long?,
        deadlineEpochDay: Long?,
        prerequisiteMissionIds: List<String>
    ) -> Unit
) {
    val today = remember { LocalDate.now().toEpochDay() }
    var title by rememberSaveable { mutableStateOf(existing?.title ?: "") }
    var description by rememberSaveable { mutableStateOf(existing?.description ?: "") }
    var category by rememberSaveable { mutableStateOf(existing?.category ?: MissionCategory.DAILY) }
    var priority by rememberSaveable { mutableStateOf(existing?.priority ?: MissionPriority.MEDIUM) }
    var module by rememberSaveable { mutableStateOf(existing?.module ?: MissionSourceModule.GENERAL) }
    var targetText by rememberSaveable { mutableStateOf((existing?.targetValue ?: 1).toString()) }
    var unit by rememberSaveable { mutableStateOf(existing?.unit ?: "units") }
    var deadlineDaysText by rememberSaveable {
        val defaultOffset = existing?.deadlineEpochDay?.let { (it - today).coerceAtLeast(0L) } ?: when (category) {
            MissionCategory.DAILY -> 0L
            MissionCategory.WEEKLY -> 6L
            MissionCategory.LONG_TERM -> 30L
        }
        mutableStateOf(defaultOffset.toString())
    }
    var selectedPrereqs by remember { mutableStateOf(existing?.prerequisiteMissionIds?.toSet() ?: emptySet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SystemColors.DeepNavy,
        title = {
            Text(
                text = if (existing == null) "Create Mission Objective" else "Edit Mission Objective",
                color = SystemColors.PureIce
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Mission Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("mission_title_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Notes") },
                    modifier = Modifier.fillMaxWidth().testTag("mission_desc_input")
                )

                if (existing == null) {
                    Text("Category", style = MaterialTheme.typography.labelMedium, color = SystemColors.PureIce)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MissionCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = {
                                    category = cat
                                    deadlineDaysText = when (cat) {
                                        MissionCategory.DAILY -> "0"
                                        MissionCategory.WEEKLY -> "6"
                                        MissionCategory.LONG_TERM -> "30"
                                    }
                                },
                                label = { Text(cat.label) }
                            )
                        }
                    }
                }

                Text("Priority", style = MaterialTheme.typography.labelMedium, color = SystemColors.PureIce)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MissionPriority.entries.forEach { prio ->
                        FilterChip(
                            selected = priority == prio,
                            onClick = { priority = prio },
                            label = { Text(prio.label) }
                        )
                    }
                }

                Text("Module Association", style = MaterialTheme.typography.labelMedium, color = SystemColors.PureIce)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    MissionSourceModule.entries.forEach { mod ->
                        FilterChip(
                            selected = module == mod,
                            onClick = { module = mod },
                            label = { Text(mod.label) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = targetText,
                        onValueChange = { targetText = it.filter { ch -> ch.isDigit() }.take(5) },
                        label = { Text("Target") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it.take(16) },
                        label = { Text("Unit") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = deadlineDaysText,
                    onValueChange = { deadlineDaysText = it.filter { ch -> ch.isDigit() }.take(3) },
                    label = { Text("Deadline (Days from today)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (availablePrerequisites.isNotEmpty()) {
                    Text(
                        "Prerequisites (Optional Dependencies)",
                        style = MaterialTheme.typography.labelMedium,
                        color = SystemColors.ElectricCyan
                    )
                    availablePrerequisites
                        .sortedByDescending { it.missionId in selectedPrereqs }
                        .take(12)
                        .forEach { prereq ->
                        val checked = prereq.missionId in selectedPrereqs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = {
                                    selectedPrereqs = if (checked) {
                                        selectedPrereqs - prereq.missionId
                                    } else {
                                        selectedPrereqs + prereq.missionId
                                    }
                                }
                            )
                            Text(
                                text = "${prereq.title} (${prereq.category.label})",
                                style = MaterialTheme.typography.bodySmall,
                                color = SystemColors.PureIce
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = (targetText.toIntOrNull() ?: 1).coerceAtLeast(1)
                    val offset = deadlineDaysText.toLongOrNull() ?: 0L
                    onSave(
                        title,
                        description,
                        category,
                        priority,
                        module,
                        target,
                        unit,
                        existing?.startDateEpochDay ?: today,
                        today + offset,
                        selectedPrereqs.toList()
                    )
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("save_mission_dialog_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SystemColors.ElectricCyan,
                    contentColor = SystemColors.ObsidianBlack
                )
            ) {
                Text("SAVE")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}
