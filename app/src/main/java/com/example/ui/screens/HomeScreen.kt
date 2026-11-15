package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.AcademicDashboardSummary
import com.example.domain.model.AttributeBreakdown
import com.example.domain.model.LifeDashboardSummary
import com.example.domain.model.NotificationCategory
import com.example.domain.model.Quest
import com.example.domain.model.QuestCategory
import com.example.domain.model.QuestType
import com.example.domain.model.RecoveryStatus
import com.example.domain.model.SystemNotification
import com.example.ui.components.GlowingProgressBar
import com.example.ui.components.HolographicPanel
import com.example.ui.components.ProvenanceChip
import com.example.ui.components.RankBadge
import com.example.ui.components.SystemEmptyState
import com.example.ui.components.SystemSectionTitle
import com.example.ui.theme.SystemColors
import com.example.ui.viewmodel.RealityOsUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    state: RealityOsUiState.Ready,
    onNavigateToStatus: () -> Unit,
    onNavigateToPhysical: () -> Unit,
    onNavigateToQuestBoard: () -> Unit,
    onNavigateToAiSystem: () -> Unit,
    onCompleteQuest: (Long) -> Unit,
    onLogManualActivity: (String, QuestCategory, Int, Boolean, String) -> Unit,
    onAcknowledgeNotification: (Long) -> Unit,
    onClearReadNotifications: () -> Unit,
    modifier: Modifier = Modifier,
    academicSummary: AcademicDashboardSummary = AcademicDashboardSummary(),
    onNavigateToAcademic: () -> Unit = {},
    lifeSummary: LifeDashboardSummary = LifeDashboardSummary(),
    onNavigateToLife: () -> Unit = {}
) {
    var showLogSheet by rememberSaveable { mutableStateOf(false) }

    val dateString = remember {
        SimpleDateFormat("EEE, MMM dd yyyy", Locale.getDefault()).format(Date()).uppercase()
    }

    val dailyQuests = remember(state.quests) {
        state.quests.filter { it.type == QuestType.DAILY }
    }
    val completedDailyCount = dailyQuests.count { it.isCompleted }
    val pendingDailyCount = dailyQuests.count { !it.isCompleted }
    val dailyCompletionFraction = if (dailyQuests.isEmpty()) 0f else {
        completedDailyCount.toFloat() / dailyQuests.size
    }

    val activeNotifications = remember(state.notifications) {
        state.notifications.take(5)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SystemColors.ObsidianBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. SYSTEM HEADER
            item {
                SystemHeaderBar(
                    dateLabel = dateString,
                    level = state.player.level,
                    rank = state.player.rank
                )
            }

            // 2. PLAYER CARD
            item {
                PlayerOverviewCard(
                    state = state,
                    onInspectStatus = onNavigateToStatus
                )
            }

            // 3. ATTRIBUTE OVERVIEW
            item {
                AttributeOverviewSection(
                    attributes = state.player.attributes.asList(),
                    onInspectStatus = onNavigateToStatus
                )
            }

            // 4. DAILY QUESTS
            item {
                DailyQuestsSection(
                    dailyQuests = dailyQuests,
                    completedCount = completedDailyCount,
                    pendingCount = pendingDailyCount,
                    progressFraction = dailyCompletionFraction,
                    onCompleteQuest = onCompleteQuest,
                    onOpenQuestBoard = onNavigateToQuestBoard
                )
            }

            // 5. PHYSICAL DEVELOPMENT
            item {
                PhysicalDevelopmentSection(
                    state = state,
                    onOpenPhysicalHub = onNavigateToPhysical,
                    onOpenLogSheet = { showLogSheet = true },
                    onOpenAiAdvisory = onNavigateToAiSystem
                )
            }

            // 5B. ACADEMIC DEVELOPMENT
            item {
                AcademicDevelopmentOverviewSection(
                    summary = academicSummary,
                    onOpenAcademicEngine = onNavigateToAcademic
                )
            }

            // 5C. LIFE & DISCIPLINE OS
            item {
                LifeDisciplineOverviewSection(
                    summary = lifeSummary,
                    onOpenLifeEngine = onNavigateToLife
                )
            }

            // 6. SYSTEM NOTIFICATIONS
            item {
                SystemSectionTitle(
                    title = "System Notifications",
                    subtitle = "${state.notifications.count { !it.isRead }} unread alerts",
                    trailing = {
                        if (state.notifications.any { it.isRead }) {
                            TextButton(
                                onClick = onClearReadNotifications,
                                modifier = Modifier.testTag("clear_read_notifications")
                            ) {
                                Text("CLEAR READ", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                )
            }

            if (activeNotifications.isEmpty()) {
                item {
                    SystemEmptyState(
                        title = "No Active System Alerts",
                        description = "Quest completions, level-ups, and AI advisories will appear here."
                    )
                }
            } else {
                items(activeNotifications, key = { it.id }) { notification ->
                    SystemNotificationCard(
                        notification = notification,
                        onAcknowledge = { onAcknowledgeNotification(notification.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    if (showLogSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showLogSheet = false },
            sheetState = sheetState,
            containerColor = SystemColors.DeepNavy,
            contentColor = SystemColors.PureIce
        ) {
            LogActivitySheetContent(
                onDismiss = { showLogSheet = false },
                onSubmit = { title, category, minutes, isJogging, notes ->
                    onLogManualActivity(title, category, minutes, isJogging, notes)
                    showLogSheet = false
                }
            )
        }
    }
}

@Composable
private fun SystemHeaderBar(
    dateLabel: String,
    level: Int,
    rank: com.example.domain.model.HunterRank
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Shield,
                    contentDescription = "Reality OS System",
                    tint = SystemColors.ElectricCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SYSTEM // REALITY OS",
                    style = MaterialTheme.typography.labelLarge,
                    color = SystemColors.ElectricCyan
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.labelSmall,
                color = SystemColors.MutedSilver
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = SystemColors.PanelSurface,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, SystemColors.PanelBorderCyan)
            ) {
                Text(
                    text = "LV. ${level.toString().padStart(2, '0')}",
                    style = MaterialTheme.typography.labelLarge,
                    color = SystemColors.PureIce,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
            RankBadge(rank = rank, compact = true)
        }
    }
}

@Composable
private fun PlayerOverviewCard(
    state: RealityOsUiState.Ready,
    onInspectStatus: () -> Unit
) {
    val player = state.player
    val rankAccent = SystemColors.rankAccent(player.rank)

    HolographicPanel(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onInspectStatus)
            .testTag("player_overview_card"),
        accentColor = SystemColors.ElectricCyan,
        secondaryColor = SystemColors.ArcaneViolet,
        contentPadding = 0.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_system_hud_banner),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .alpha(0.24f)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AWAKENED PLAYER",
                            style = MaterialTheme.typography.labelSmall,
                            color = SystemColors.ElectricCyan
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = player.codename,
                            style = MaterialTheme.typography.headlineLarge,
                            color = SystemColors.PureIce,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${player.rank.displayName} • ${player.title}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = rankAccent
                        )
                    }
                    RankBadge(rank = player.rank)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "LEVEL ${player.level} EXPERIENCE",
                        style = MaterialTheme.typography.labelMedium,
                        color = SystemColors.MetallicSilver
                    )
                    Text(
                        text = "${player.currentLevelXp} / ${player.xpToNextLevel} XP",
                        style = MaterialTheme.typography.labelMedium,
                        color = SystemColors.ElectricCyan
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                GlowingProgressBar(
                    progress = player.levelProgressFraction,
                    height = 10.dp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val nextRank = state.nextRankProgress.nextRank
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (nextRank != null) {
                                "NEXT RANK: ${nextRank.displayName.uppercase()} (${(state.nextRankProgress.overallProgress * 100).toInt()}%)"
                            } else {
                                "MAXIMUM RANK ACHIEVED"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = SystemColors.MutedSilver
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Total Attr: ${player.attributes.totalScore} • Verified: ${player.verifiedActivitiesCount} / ${player.totalActivitiesCount}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SystemColors.MetallicSilver
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "STATUS",
                            style = MaterialTheme.typography.labelMedium,
                            color = SystemColors.ElectricCyan
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = "Inspect player status",
                            tint = SystemColors.ElectricCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AttributeOverviewSection(
    attributes: List<AttributeBreakdown>,
    onInspectStatus: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SystemSectionTitle(
            title = "Attribute Matrix",
            subtitle = "Auto-calculated • Read-only integrity",
            trailing = {
                TextButton(
                    onClick = onInspectStatus,
                    modifier = Modifier.testTag("inspect_attributes_button")
                ) {
                    Text("DETAILS", style = MaterialTheme.typography.labelMedium)
                }
            }
        )

        HolographicPanel(
            modifier = Modifier.fillMaxWidth(),
            accentColor = SystemColors.SystemBlue,
            contentPadding = 12.dp
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 4
            ) {
                attributes.forEach { attr ->
                    val accent = SystemColors.attributeAccent(attr.type)
                    Surface(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .widthIn(min = 72.dp)
                            .clickable(onClick = onInspectStatus),
                        color = SystemColors.ObsidianBlack.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(0.75.dp, accent.copy(alpha = 0.45f))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = attr.type.code,
                                style = MaterialTheme.typography.labelSmall,
                                color = accent
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${attr.totalValue}",
                                style = MaterialTheme.typography.headlineMedium,
                                color = SystemColors.PureIce
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            GlowingProgressBar(
                                progress = attr.progressFraction,
                                height = 3.dp,
                                startColor = accent,
                                endColor = SystemColors.ElectricCyan
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyQuestsSection(
    dailyQuests: List<Quest>,
    completedCount: Int,
    pendingCount: Int,
    progressFraction: Float,
    onCompleteQuest: (Long) -> Unit,
    onOpenQuestBoard: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SystemSectionTitle(
            title = "Daily Quests",
            subtitle = "$completedCount completed • $pendingCount pending",
            trailing = {
                TextButton(
                    onClick = onOpenQuestBoard,
                    modifier = Modifier.testTag("open_quest_board_button")
                ) {
                    Text("ALL QUESTS", style = MaterialTheme.typography.labelMedium)
                }
            }
        )

        HolographicPanel(
            modifier = Modifier.fillMaxWidth(),
            accentColor = SystemColors.ElectricCyan
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DAILY COMPLETION PROTOCOL",
                        style = MaterialTheme.typography.labelMedium,
                        color = SystemColors.MetallicSilver
                    )
                    Text(
                        text = "${(progressFraction * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = SystemColors.ElectricCyan
                    )
                }
                GlowingProgressBar(progress = progressFraction, height = 7.dp)

                if (dailyQuests.isEmpty()) {
                    Text(
                        text = "No daily quests active. Open Quest Board to add objectives.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SystemColors.MutedSilver
                    )
                } else {
                    dailyQuests.forEach { quest ->
                        DailyQuestRow(
                            quest = quest,
                            onComplete = { onCompleteQuest(quest.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyQuestRow(
    quest: Quest,
    onComplete: () -> Unit
) {
    Surface(
        color = SystemColors.ObsidianBlack.copy(alpha = 0.55f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            width = 0.75.dp,
            color = if (quest.isCompleted) SystemColors.VerifiedEmerald.copy(alpha = 0.45f)
            else SystemColors.SubtleGridLine
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (quest.isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = if (quest.isCompleted) "Completed" else "Pending",
                tint = if (quest.isCompleted) SystemColors.VerifiedEmerald else SystemColors.ElectricCyan,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = quest.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (quest.isCompleted) SystemColors.MutedSilver else SystemColors.PureIce,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${quest.targetValue} ${quest.unit} • +${quest.xpReward} XP • ${quest.primaryAttribute.code}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SystemColors.MutedSilver
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (!quest.isCompleted) {
                OutlinedButton(
                    onClick = onComplete,
                    modifier = Modifier.testTag("complete_quest_${quest.id}"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    border = BorderStroke(1.dp, SystemColors.ElectricCyan)
                ) {
                    Text(
                        text = "COMPLETE",
                        style = MaterialTheme.typography.labelSmall,
                        color = SystemColors.ElectricCyan
                    )
                }
            } else {
                Text(
                    text = "CLEARED",
                    style = MaterialTheme.typography.labelSmall,
                    color = SystemColors.VerifiedEmerald
                )
            }
        }
    }
}

@Composable
private fun PhysicalDevelopmentSection(
    state: RealityOsUiState.Ready,
    onOpenPhysicalHub: () -> Unit,
    onOpenLogSheet: () -> Unit,
    onOpenAiAdvisory: () -> Unit
) {
    val physical = state.physicalState
    val recoveryColor = when (physical.recoveryStatus) {
        RecoveryStatus.OPTIMAL -> SystemColors.VerifiedEmerald
        RecoveryStatus.MODERATE_LOAD -> SystemColors.WarningAmber
        RecoveryStatus.RECOVERY_REQUIRED -> SystemColors.DangerCrimson
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SystemSectionTitle(
            title = "Physical Development",
            subtitle = "Training load & recovery telemetry",
            trailing = {
                TextButton(
                    onClick = onOpenPhysicalHub,
                    modifier = Modifier.testTag("open_physical_hub_button")
                ) {
                    Text("OPEN ENGINE", style = MaterialTheme.typography.labelMedium)
                }
            }
        )

        HolographicPanel(
            modifier = Modifier.fillMaxWidth(),
            accentColor = SystemColors.VerifiedEmerald,
            secondaryColor = SystemColors.ElectricCyan
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.DirectionsRun,
                            contentDescription = "Jogging target",
                            tint = SystemColors.ElectricCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DAILY JOGGING TARGET",
                            style = MaterialTheme.typography.titleMedium,
                            color = SystemColors.PureIce
                        )
                    }
                    Text(
                        text = "${physical.completedJoggingMinutesToday} / ${physical.effectiveJoggingTarget} MIN",
                        style = MaterialTheme.typography.labelLarge,
                        color = SystemColors.ElectricCyan
                    )
                }

                GlowingProgressBar(
                    progress = physical.joggingProgressFraction,
                    height = 8.dp,
                    startColor = SystemColors.VerifiedEmerald,
                    endColor = SystemColors.ElectricCyan
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = SystemColors.ObsidianBlack.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(0.75.dp, SystemColors.SubtleGridLine)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "WORKOUT STATUS",
                                style = MaterialTheme.typography.labelSmall,
                                color = SystemColors.MutedSilver
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = physical.workoutStatus.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = SystemColors.PureIce
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        color = recoveryColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(0.75.dp, recoveryColor.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "RECOVERY STATUS",
                                style = MaterialTheme.typography.labelSmall,
                                color = recoveryColor
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = physical.recoveryStatus.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = SystemColors.PureIce
                            )
                        }
                    }
                }

                Text(
                    text = physical.aiTrainingNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = SystemColors.MetallicSilver
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onOpenLogSheet,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("open_log_activity_sheet"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SystemColors.ElectricCyan,
                            contentColor = SystemColors.ObsidianBlack
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("LOG SESSION", style = MaterialTheme.typography.labelLarge)
                    }

                    OutlinedButton(
                        onClick = onOpenAiAdvisory,
                        modifier = Modifier.testTag("open_ai_advisory_button"),
                        border = BorderStroke(1.dp, SystemColors.ArcaneViolet)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = "AI Game Master",
                            tint = SystemColors.ArcaneViolet,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AI COACH",
                            style = MaterialTheme.typography.labelMedium,
                            color = SystemColors.ArcaneViolet
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AcademicDevelopmentOverviewSection(
    summary: AcademicDashboardSummary,
    onOpenAcademicEngine: () -> Unit
) {
    val goalFraction = (summary.todayStudyMinutes.toFloat() / summary.dailyStudyGoalMinutes.coerceAtLeast(1)).coerceIn(0f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SystemSectionTitle(
            title = "Academic Development",
            subtitle = "INT • WIS • DIS progression & revision intelligence",
            trailing = {
                TextButton(
                    onClick = onOpenAcademicEngine,
                    modifier = Modifier.testTag("open_academic_engine_button")
                ) {
                    Text("OPEN ENGINE", style = MaterialTheme.typography.labelMedium)
                }
            }
        )
        HolographicPanel(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenAcademicEngine),
            accentColor = SystemColors.ArcaneViolet,
            secondaryColor = SystemColors.ElectricCyan
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("TODAY'S STUDY TARGET", style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
                    Text(
                        text = "${summary.todayStudyMinutes} / ${summary.dailyStudyGoalMinutes} MIN",
                        style = MaterialTheme.typography.labelLarge,
                        color = SystemColors.ElectricCyan
                    )
                }
                GlowingProgressBar(
                    progress = goalFraction,
                    height = 8.dp,
                    startColor = SystemColors.ArcaneViolet,
                    endColor = SystemColors.ElectricCyan
                )
                Text(
                    text = "${summary.activeSubjectsCount} active subjects • ${summary.pendingRevisionsCount} revisions due • ${summary.upcomingExamsCount} upcoming exams",
                    style = MaterialTheme.typography.bodySmall,
                    color = SystemColors.MetallicSilver
                )
            }
        }
    }
}

@Composable
private fun LifeDisciplineOverviewSection(
    summary: LifeDashboardSummary,
    onOpenLifeEngine: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SystemSectionTitle(
            title = "Life & Discipline OS",
            subtitle = "Routines • Habits • Personal Tasks • Goals • Calendar",
            trailing = {
                TextButton(
                    onClick = onOpenLifeEngine,
                    modifier = Modifier.testTag("open_life_engine_button")
                ) {
                    Text("OPEN ENGINE", style = MaterialTheme.typography.labelMedium)
                }
            }
        )
        HolographicPanel(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenLifeEngine),
            accentColor = SystemColors.VerifiedEmerald,
            secondaryColor = SystemColors.ElectricCyan
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("DISCIPLINE INDEX", style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
                    Text(
                        text = "${summary.todayDisciplineScore} / 100",
                        style = MaterialTheme.typography.labelLarge,
                        color = SystemColors.VerifiedEmerald
                    )
                }
                GlowingProgressBar(
                    progress = (summary.todayDisciplineScore / 100f).coerceIn(0f, 1f),
                    height = 8.dp,
                    startColor = SystemColors.VerifiedEmerald,
                    endColor = SystemColors.ElectricCyan
                )
                Text(
                    text = "${summary.completedHabitsToday}/${summary.activeHabitsCount} habits • ${summary.pendingTasksCount} pending tasks • ${summary.activeGoalsCount} goals • ${summary.todayScheduledCount} scheduled today",
                    style = MaterialTheme.typography.bodySmall,
                    color = SystemColors.MetallicSilver
                )
            }
        }
    }
}

@Composable
private fun SystemNotificationCard(
    notification: SystemNotification,
    onAcknowledge: () -> Unit
) {
    val accent = when (notification.category) {
        NotificationCategory.LEVEL_UP -> SystemColors.MonarchGold
        NotificationCategory.RANK_PROMOTION -> SystemColors.ArcaneViolet
        NotificationCategory.AI_RECOMMENDATION -> SystemColors.ElectricCyan
        NotificationCategory.DUNGEON_UPDATE -> SystemColors.ElectricCyan
        NotificationCategory.BOSS_PHASE -> SystemColors.DangerCrimson
        NotificationCategory.SKILL_UNLOCKED -> SystemColors.ArcaneViolet
        NotificationCategory.ACHIEVEMENT -> SystemColors.VerifiedEmerald
        NotificationCategory.QUEST_UPDATE,
        NotificationCategory.DAILY_QUEST_REMINDER -> SystemColors.SystemBlue
        NotificationCategory.OVERDUE_REVISION,
        NotificationCategory.UPCOMING_EXAM,
        NotificationCategory.TASK_DEADLINE -> SystemColors.WarningAmber
        NotificationCategory.OVERDUE_TASK,
        NotificationCategory.RECOVERY_WARNING -> SystemColors.DangerCrimson
        NotificationCategory.HABIT_REMINDER,
        NotificationCategory.ROUTINE_REMINDER,
        NotificationCategory.WORKOUT_REMINDER -> SystemColors.VerifiedEmerald
        NotificationCategory.GOAL_MILESTONE -> SystemColors.MonarchGold
        NotificationCategory.DAILY_PLANNING_REMINDER,
        NotificationCategory.WEEKLY_REVIEW_REMINDER -> SystemColors.ElectricCyan
        NotificationCategory.DAILY_SUMMARY,
        NotificationCategory.SYSTEM_ALERT -> SystemColors.MetallicSilver
    }

    HolographicPanel(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (notification.isRead) 0.68f else 1f),
        accentColor = accent,
        contentPadding = 12.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Rounded.NotificationsActive,
                contentDescription = notification.category.label,
                tint = accent,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = notification.category.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = accent
                    )
                    if (!notification.isRead) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(50))
                                .background(accent)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = SystemColors.PureIce
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = SystemColors.MetallicSilver
                )
            }
            if (!notification.isRead) {
                IconButton(
                    onClick = onAcknowledge,
                    modifier = Modifier.testTag("ack_notification_${notification.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DoneAll,
                        contentDescription = "Acknowledge notification",
                        tint = SystemColors.ElectricCyan
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LogActivitySheetContent(
    onDismiss: () -> Unit,
    onSubmit: (String, QuestCategory, Int, Boolean, String) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var durationText by rememberSaveable { mutableStateOf("30") }
    var selectedCategory by rememberSaveable { mutableStateOf(QuestCategory.PHYSICAL) }
    var isJogging by rememberSaveable { mutableStateOf(true) }
    var notes by rememberSaveable { mutableStateOf("") }

    val durationMinutes = durationText.toIntOrNull() ?: 0
    val canSubmit = title.isNotBlank() && durationMinutes in 1..240

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "LOG REAL-WORLD ACTIVITY",
                style = MaterialTheme.typography.titleLarge,
                color = SystemColors.ElectricCyan
            )
            ProvenanceChip(status = com.example.domain.model.VerificationStatus.SELF_REPORTED)
        }

        Text(
            text = "Manual entries are recorded as Self-Reported (0.65x progression weight). Connect Health Connect in Settings for 1.0x verified training.",
            style = MaterialTheme.typography.bodySmall,
            color = SystemColors.MutedSilver
        )

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Activity Name (e.g., Morning Run, Linear Algebra)") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("log_activity_title_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SystemColors.ElectricCyan,
                unfocusedBorderColor = SystemColors.PanelBorderCyan,
                focusedTextColor = SystemColors.PureIce,
                unfocusedTextColor = SystemColors.PureIce
            )
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            QuestCategory.entries.forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = {
                        selectedCategory = category
                        if (category != QuestCategory.PHYSICAL) isJogging = false
                    },
                    label = { Text(category.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SystemColors.ElectricCyan.copy(alpha = 0.2f),
                        selectedLabelColor = SystemColors.ElectricCyan
                    )
                )
            }
        }

        OutlinedTextField(
            value = durationText,
            onValueChange = { input -> durationText = input.filter { it.isDigit() }.take(3) },
            label = { Text("Duration (minutes, 1–240)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("log_activity_duration_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SystemColors.ElectricCyan,
                unfocusedBorderColor = SystemColors.PanelBorderCyan,
                focusedTextColor = SystemColors.PureIce,
                unfocusedTextColor = SystemColors.PureIce
            )
        )

        AnimatedVisibility(visible = selectedCategory == QuestCategory.PHYSICAL) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Count toward Daily Jogging Target",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SystemColors.PureIce
                )
                Switch(
                    checked = isJogging,
                    onCheckedChange = { isJogging = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SystemColors.ObsidianBlack,
                        checkedTrackColor = SystemColors.ElectricCyan
                    )
                )
            }
        }

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Session Notes (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SystemColors.ElectricCyan,
                unfocusedBorderColor = SystemColors.PanelBorderCyan,
                focusedTextColor = SystemColors.PureIce,
                unfocusedTextColor = SystemColors.PureIce
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            ) {
                Text("CANCEL")
            }
            Button(
                onClick = {
                    onSubmit(title, selectedCategory, durationMinutes, isJogging, notes)
                },
                enabled = canSubmit,
                modifier = Modifier
                    .weight(1f)
                    .testTag("submit_log_activity_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SystemColors.ElectricCyan,
                    contentColor = SystemColors.ObsidianBlack
                )
            ) {
                Text("RECORD XP", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
