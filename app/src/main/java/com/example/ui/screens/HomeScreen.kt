package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.NotificationsActive
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.engine.HomeNextActionEngine
import com.example.domain.model.AcademicDashboardSummary
import com.example.domain.model.LifeDashboardSummary
import com.example.domain.model.NotificationCategory
import com.example.domain.model.QuestCategory
import com.example.domain.model.QuestType
import com.example.domain.model.ShadowMonarchProfile
import com.example.domain.model.SystemAnnouncementRecord
import com.example.domain.model.SystemNotification
import com.example.domain.model.WorkoutStatus
import com.example.ui.components.HolographicPanel
import com.example.ui.components.ProvenanceChip
import com.example.ui.components.SystemEmptyState
import com.example.ui.components.SystemSectionTitle
import com.example.ui.screens.home.components.ActiveSessionAlertBanner
import com.example.ui.screens.home.components.AttributeMatrixSection
import com.example.ui.screens.home.components.CinematicSystemHeader
import com.example.ui.screens.home.components.DailyProgressSnapshotSection
import com.example.ui.screens.home.components.DomainCommandCentresSection
import com.example.ui.screens.home.components.HunterHeroOverviewCard
import com.example.ui.screens.home.components.NextActionRecommendationCard
import com.example.ui.screens.home.components.QuickActionCommandDeck
import com.example.ui.screens.home.components.SystemAnnouncementBanner
import com.example.ui.screens.home.components.TodayMissionCard
import com.example.ui.theme.SystemColors
import com.example.ui.theme.SystemSpacing
import com.example.ui.viewmodel.RealityOsUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
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
    onNavigateToLife: () -> Unit = {},
    intelligenceHeadline: String = "Adaptive planning, predictions & personalized coaching ready",
    onNavigateToIntelligence: () -> Unit = {},
    systemHealthStatusLabel: String = "Healthy",
    onNavigateToSystemControl: () -> Unit = {},
    onNavigateToAiCompanion: () -> Unit = {},
    onNavigateToVoiceConsole: () -> Unit = {},
    announcements: List<SystemAnnouncementRecord> = emptyList(),
    onNavigateToShadowRealm: () -> Unit = {},
    monarchProfile: ShadowMonarchProfile? = null,
    shadowEssenceBalance: Int = 0,
    onNavigateRoute: (String) -> Unit = {},
    reducedMotionEnabled: Boolean = false,
    equippedTitle: String? = null
) {
    var showLogSheet by rememberSaveable { mutableStateOf(false) }

    val currentTitle = equippedTitle ?: state.player.title

    val dateString = remember {
        SimpleDateFormat("EEE, MMM dd yyyy", Locale.getDefault()).format(Date()).uppercase()
    }

    val dailyQuests = remember(state.quests) {
        state.quests.filter { it.type == QuestType.DAILY }
    }
    val completedDailyCount = dailyQuests.count { it.isCompleted }

    val activeNotifications = remember(state.notifications) {
        state.notifications.take(5)
    }

    // Deterministic Next Best Action Evaluator
    val nextAction = remember(state.physicalState, state.quests, academicSummary, lifeSummary) {
        HomeNextActionEngine.evaluate(
            physicalState = state.physicalState,
            quests = state.quests,
            academicSummary = academicSummary,
            lifeSummary = lifeSummary
        )
    }

    // Router for Next Best Action
    val handleNextActionClick: (String) -> Unit = { destination ->
        when (destination) {
            "physical" -> onNavigateToPhysical()
            "quests" -> onNavigateToQuestBoard()
            "academic" -> onNavigateToAcademic()
            "life" -> onNavigateToLife()
            "status" -> onNavigateToStatus()
            "shadow" -> onNavigateToShadowRealm()
            "intelligence" -> onNavigateToIntelligence()
            else -> onNavigateRoute(destination)
        }
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
                .widthIn(max = SystemSpacing.maxContentWidth),
            contentPadding = PaddingValues(
                horizontal = SystemSpacing.screenHorizontal,
                vertical = SystemSpacing.screenVertical
            ),
            verticalArrangement = Arrangement.spacedBy(SystemSpacing.sectionSpacing)
        ) {
            // 1. ACTIVE RUNNING / WORKOUT SESSION ALERT (Contextual)
            if (state.physicalState.activeJoggingSession != null ||
                (state.physicalState.todayWorkout != null && state.physicalState.todayWorkout.status == WorkoutStatus.IN_PROGRESS)
            ) {
                item {
                    ActiveSessionAlertBanner(
                        activeJoggingSession = state.physicalState.activeJoggingSession,
                        activeWorkout = state.physicalState.todayWorkout,
                        onContinueSession = handleNextActionClick,
                        reducedMotionEnabled = reducedMotionEnabled
                    )
                }
            }

            // 2. CINEMATIC PLAYER HEADER
            item {
                CinematicSystemHeader(
                    dateLabel = dateString,
                    level = state.player.level,
                    rank = state.player.rank,
                    codename = state.player.codename,
                    title = state.player.title,
                    levelProgressFraction = state.player.levelProgressFraction,
                    reducedMotionEnabled = reducedMotionEnabled
                )
            }

            // 3. SYSTEM BROADCAST / ANNOUNCEMENTS
            if (announcements.isNotEmpty()) {
                item {
                    SystemAnnouncementBanner(
                        announcements = announcements,
                        onNavigateRoute = onNavigateRoute
                    )
                }
            }

            // 4. PLAYER OVERVIEW HERO (Rank, Level, XP to Next, 7 Canonical Attributes, Status Link)
            item {
                HunterHeroOverviewCard(
                    player = state.player,
                    onInspectStatus = onNavigateToStatus
                )
            }

            // 5. NEXT BEST ACTION — SYSTEM RECOMMENDATION
            item {
                NextActionRecommendationCard(
                    action = nextAction,
                    onActionClick = handleNextActionClick
                )
            }

            // 6. TODAY'S MISSION (Primary Active Daily Quest)
            item {
                TodayMissionCard(
                    dailyQuests = dailyQuests,
                    onCompleteQuest = onCompleteQuest,
                    onOpenQuestBoard = onNavigateToQuestBoard
                )
            }

            // 7. DAILY EXECUTION PROGRESS SNAPSHOT
            item {
                val isWorkoutDone = state.physicalState.todayWorkout?.status == WorkoutStatus.COMPLETED
                val jogMinutes = state.physicalState.weeklyActivity.todayJoggingMinutes
                DailyProgressSnapshotSection(
                    dailyQuestsCompleted = completedDailyCount,
                    dailyQuestsTotal = dailyQuests.size,
                    physicalWorkoutCompleted = isWorkoutDone,
                    physicalJogMinutes = jogMinutes,
                    studyMinutesToday = academicSummary.todayStudyMinutes,
                    studyMinutesGoal = academicSummary.dailyStudyGoalMinutes,
                    habitsCompletedToday = lifeSummary.completedHabitsToday,
                    habitsTotalToday = lifeSummary.activeHabitsCount,
                    streakDays = state.player.streakDays
                )
            }

            // 8. DOMAIN COMMAND CENTRES (Physical, Academic, Life & Habits, Shadow Army)
            item {
                DomainCommandCentresSection(
                    physicalState = state.physicalState,
                    academicSummary = academicSummary,
                    lifeSummary = lifeSummary,
                    monarchProfile = monarchProfile,
                    shadowEssenceBalance = shadowEssenceBalance,
                    onNavigateToPhysical = onNavigateToPhysical,
                    onNavigateToAcademic = onNavigateToAcademic,
                    onNavigateToLife = onNavigateToLife,
                    onNavigateToShadowRealm = onNavigateToShadowRealm
                )
            }

            // 9. ATTRIBUTE MATRIX (Radar Chart & Detailed Canonical Attributes)
            item {
                AttributeMatrixSection(
                    attributes = state.player.attributes,
                    onInspectAttributes = onNavigateToStatus
                )
            }

            // 10. QUICK ACTION COMMAND DECK (System Realm Shortcuts)
            item {
                QuickActionCommandDeck(
                    onNavigateToPhysical = onNavigateToPhysical,
                    onNavigateToQuestBoard = onNavigateToQuestBoard,
                    onNavigateToStatus = onNavigateToStatus,
                    onNavigateToShadowRealm = onNavigateToShadowRealm,
                    onNavigateToAiCompanion = onNavigateToAiCompanion,
                    onNavigateToVoiceConsole = onNavigateToVoiceConsole,
                    onNavigateToAcademic = onNavigateToAcademic,
                    onNavigateToLife = onNavigateToLife,
                    onNavigateToIntelligence = onNavigateToIntelligence,
                    onNavigateToSystemControl = onNavigateToSystemControl,
                    academicSummary = academicSummary,
                    lifeSummary = lifeSummary,
                    intelligenceHeadline = intelligenceHeadline,
                    systemHealthStatusLabel = systemHealthStatusLabel
                )
            }

            // 11. SYSTEM ALERTS & NOTIFICATIONS
            item {
                SystemSectionTitle(
                    title = "System Alerts",
                    subtitle = "${state.notifications.count { !it.isRead }} unread alerts",
                    trailing = {
                        if (state.notifications.any { it.isRead }) {
                            TextButton(
                                onClick = onClearReadNotifications,
                                modifier = Modifier.testTag("clear_read_notifications")
                            ) {
                                Text(
                                    text = "CLEAR READ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SystemColors.ElectricCyan
                                )
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
                Spacer(modifier = Modifier.height(SystemSpacing.verticalPageSpacing))
            }
        }
    }

    // Modal Bottom Sheet for logging real-world activity
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
            .alpha(if (notification.isRead) 0.65f else 1f),
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
