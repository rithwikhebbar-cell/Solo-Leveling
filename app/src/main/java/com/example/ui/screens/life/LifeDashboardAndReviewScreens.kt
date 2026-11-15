package com.example.ui.screens.life

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CalendarViewWeek
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Loop
import androidx.compose.material.icons.rounded.RateReview
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.components.GlowingProgressBar
import com.example.ui.components.HolographicPanel
import com.example.ui.components.SystemErrorState
import com.example.ui.components.SystemLoadingView
import com.example.ui.components.SystemSectionTitle
import com.example.ui.theme.SystemColors
import com.example.ui.viewmodel.LifeManagementUiState
import java.time.DayOfWeek
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LifeDashboardScreen(
    uiState: LifeManagementUiState,
    onRetry: () -> Unit,
    onClearFeedback: () -> Unit,
    onToggleRestDay: (DayOfWeek) -> Unit,
    onOpenRoutines: () -> Unit,
    onOpenHabits: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenGoals: () -> Unit,
    onOpenDailySchedule: () -> Unit,
    onOpenWeeklyCalendar: () -> Unit,
    onOpenMonthlyCalendar: () -> Unit,
    onOpenDisciplineAnalytics: () -> Unit,
    onOpenWeeklyReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is LifeManagementUiState.Loading -> SystemLoadingView(modifier = modifier)
        is LifeManagementUiState.Error -> SystemErrorState(uiState.message, onRetry, modifier)
        is LifeManagementUiState.Ready -> {
            var showWeeklyMode by rememberSaveable { mutableStateOf(false) }
            val d = uiState.dashboard
            val disc = uiState.todayDiscipline

            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(SystemColors.ObsidianBlack),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 780.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (uiState.feedbackBanner != null) {
                        item {
                            HolographicPanel(
                                modifier = Modifier.fillMaxWidth(),
                                accentColor = SystemColors.ElectricCyan,
                                contentPadding = 12.dp
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = uiState.feedbackBanner,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SystemColors.PureIce,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = onClearFeedback) {
                                        Text("DISMISS")
                                    }
                                }
                            }
                        }
                    }

                    // Overview Mode Toggle
                    item {
                        SystemSectionTitle(
                            title = "Life & Discipline OS",
                            subtitle = "Unified routines, habits, tasks, goals & calendar"
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = !showWeeklyMode,
                                onClick = { showWeeklyMode = false },
                                label = { Text("Daily View") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SystemColors.ElectricCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = SystemColors.ElectricCyan
                                ),
                                modifier = Modifier.testTag("life_daily_mode_chip")
                            )
                            FilterChip(
                                selected = showWeeklyMode,
                                onClick = { showWeeklyMode = true },
                                label = { Text("Weekly Overview") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SystemColors.ArcaneViolet.copy(alpha = 0.2f),
                                    selectedLabelColor = SystemColors.ArcaneViolet
                                ),
                                modifier = Modifier.testTag("life_weekly_mode_chip")
                            )
                        }
                    }

                    // Discipline Index Hero Card
                    item {
                        HolographicPanel(
                            modifier = Modifier.fillMaxWidth(),
                            accentColor = SystemColors.ElectricCyan,
                            secondaryColor = SystemColors.ArcaneViolet
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = if (showWeeklyMode) "WEEKLY DISCIPLINE AVERAGE" else "TODAY'S DISCIPLINE INDEX",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = SystemColors.ElectricCyan
                                        )
                                        Text(
                                            text = "${if (showWeeklyMode) (uiState.weeklyReview?.averageDisciplineScore ?: d.todayDisciplineScore) else d.todayDisciplineScore} / 100",
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = SystemColors.PureIce
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("BEST HABIT STREAK", style = MaterialTheme.typography.labelSmall, color = SystemColors.MutedSilver)
                                        Text("${d.activeDisciplineStreakDays} DAYS", style = MaterialTheme.typography.titleMedium, color = SystemColors.MonarchGold)
                                    }
                                }
                                GlowingProgressBar(
                                    progress = (d.todayDisciplineScore / 100f).coerceIn(0f, 1f),
                                    height = 9.dp,
                                    startColor = SystemColors.ElectricCyan,
                                    endColor = SystemColors.VerifiedEmerald
                                )
                                Text(
                                    text = disc.reflectionSummary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SystemColors.MetallicSilver
                                )
                                Text(
                                    text = "Life Progression Today: ${d.lifeXpToday} / ${d.dailyLifeXpCap} XP (Anti-farming daily cap)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SystemColors.VerifiedEmerald
                                )
                            }
                        }
                    }

                    // Rest Day Exemption Strip
                    item {
                        HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.VerifiedEmerald) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("EXEMPT REST DAYS (NO STREAK PENALTY)", style = MaterialTheme.typography.labelMedium, color = SystemColors.VerifiedEmerald)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    DayOfWeek.entries.forEach { dow ->
                                        val selected = dow in uiState.exemptRestDays
                                        FilterChip(
                                            selected = selected,
                                            onClick = { onToggleRestDay(dow) },
                                            label = { Text(dow.name.take(3)) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = SystemColors.VerifiedEmerald.copy(alpha = 0.22f),
                                                selectedLabelColor = SystemColors.VerifiedEmerald
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Navigation Grid to Subsystems
                    item {
                        SystemSectionTitle(
                            title = "Subsystems & Views",
                            subtitle = "Every operational life engine"
                        )
                    }

                    val tiles = listOf(
                        LifeNavTile("Routines", "${d.completedRoutinesToday}/${d.activeRoutinesCount} done today", Icons.Rounded.EventRepeat, SystemColors.ElectricCyan, onOpenRoutines, "nav_life_routines"),
                        LifeNavTile("Habits", "${d.completedHabitsToday}/${d.activeHabitsCount} active • ${d.activeDisciplineStreakDays}d streak", Icons.Rounded.Loop, SystemColors.VerifiedEmerald, onOpenHabits, "nav_life_habits"),
                        LifeNavTile("Personal Tasks", "${d.dueTodayTasksCount} due today • ${d.overdueTasksCount} overdue", Icons.Rounded.TaskAlt, SystemColors.SystemBlue, onOpenTasks, "nav_life_tasks"),
                        LifeNavTile("Goals & Milestones", "${d.activeGoalsCount} active structured goals", Icons.Rounded.Flag, SystemColors.MonarchGold, onOpenGoals, "nav_life_goals"),
                        LifeNavTile("Daily Schedule", "${d.todayScheduledCount} blocks • ${d.todayConflictCount} conflicts", Icons.Rounded.CalendarToday, SystemColors.ArcaneViolet, onOpenDailySchedule, "nav_life_daily_schedule"),
                        LifeNavTile("Weekly Calendar", "7-day workload & timeline blocks", Icons.Rounded.CalendarViewWeek, SystemColors.ElectricCyan, onOpenWeeklyCalendar, "nav_life_weekly_calendar"),
                        LifeNavTile("Monthly Calendar", "Month grid across all modules", Icons.Rounded.CalendarMonth, SystemColors.SystemBlue, onOpenMonthlyCalendar, "nav_life_monthly_calendar"),
                        LifeNavTile("Discipline Analytics", "Multi-factor breakdown & score", Icons.Rounded.Analytics, SystemColors.VerifiedEmerald, onOpenDisciplineAnalytics, "nav_life_discipline"),
                        LifeNavTile("Weekly Review", "Holistic 7-day life + study + fitness", Icons.Rounded.RateReview, SystemColors.MonarchGold, onOpenWeeklyReview, "nav_life_weekly_review")
                    )

                    items(tiles) { tile ->
                        HolographicPanel(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = tile.onClick)
                                .testTag(tile.testTag),
                            accentColor = tile.accent,
                            contentPadding = 12.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = tile.accent.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, tile.accent.copy(alpha = 0.5f))
                                ) {
                                    Icon(
                                        imageVector = tile.icon,
                                        contentDescription = tile.title,
                                        tint = tile.accent,
                                        modifier = Modifier.padding(8.dp).size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(tile.title.uppercase(), style = MaterialTheme.typography.titleSmall, color = SystemColors.PureIce)
                                    Text(tile.subtitle, style = MaterialTheme.typography.bodySmall, color = SystemColors.MutedSilver)
                                }
                            }
                        }
                    }

                    if (d.todayConflictCount > 0) {
                        item {
                            HolographicPanel(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = onOpenDailySchedule),
                                accentColor = SystemColors.WarningAmber
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Warning, contentDescription = "Conflict", tint = SystemColors.WarningAmber)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "${d.todayConflictCount} schedule overlap(s) detected today. Tap to resolve conflicts cleanly.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SystemColors.WarningAmber
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class LifeNavTile(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color,
    val onClick: () -> Unit,
    val testTag: String
)

@Composable
fun DisciplineAnalyticsScreen(
    uiState: LifeManagementUiState,
    onRetry: () -> Unit,
    onSaveReview: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is LifeManagementUiState.Loading -> SystemLoadingView(modifier = modifier)
        is LifeManagementUiState.Error -> SystemErrorState(uiState.message, onRetry, modifier)
        is LifeManagementUiState.Ready -> {
            val disc = uiState.todayDiscipline
            var reflectionInput by rememberSaveable { mutableStateOf("") }

            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(SystemColors.ObsidianBlack),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().widthIn(max = 780.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SystemSectionTitle(
                            title = "Discipline Analytics",
                            subtitle = "Transparent weighted computation (0 fake inflation)"
                        )
                    }
                    item {
                        HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.ElectricCyan) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("OVERALL DISCIPLINE SCORE: ${disc.disciplineScore}/100", style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
                                FactorBar("Routine Consistency (25%)", disc.routineConsistencyPercent, SystemColors.ElectricCyan)
                                FactorBar("Habit Adherence (25%)", disc.habitConsistencyPercent, SystemColors.VerifiedEmerald)
                                FactorBar("Task Completion (20%)", disc.taskCompletionPercent, SystemColors.SystemBlue)
                                FactorBar("Schedule Adherence (15%)", disc.scheduleAdherencePercent, SystemColors.ArcaneViolet)
                                FactorBar("Goal Momentum (15%)", disc.goalProgressPercent, SystemColors.MonarchGold)
                                Text(
                                    text = "Strongest: ${disc.strongestArea} • Focus Area: ${disc.weakestArea}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SystemColors.ElectricCyan
                                )
                                Text(
                                    text = "Open/missed commitments: ${disc.missedCommitmentsCount} • Protected rest/skip days: ${disc.exemptRestDaysCount}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SystemColors.MutedSilver
                                )
                            }
                        }
                    }

                    item {
                        HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.ArcaneViolet) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("LOG DISCIPLINE REFLECTION", style = MaterialTheme.typography.titleSmall, color = SystemColors.PureIce)
                                OutlinedTextField(
                                    value = reflectionInput,
                                    onValueChange = { reflectionInput = it },
                                    label = { Text("Honest assessment & next-day adjustment") },
                                    modifier = Modifier.fillMaxWidth().testTag("discipline_reflection_input")
                                )
                                Button(
                                    onClick = {
                                        onSaveReview(reflectionInput)
                                        reflectionInput = ""
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SystemColors.ElectricCyan, contentColor = SystemColors.ObsidianBlack),
                                    modifier = Modifier.fillMaxWidth().testTag("save_discipline_reflection_button")
                                ) {
                                    Text("Record Daily Discipline Review")
                                }
                            }
                        }
                    }

                    if (uiState.disciplineHistory.isNotEmpty()) {
                        item {
                            SystemSectionTitle("Assessment History", "${uiState.disciplineHistory.size} saved daily snapshot(s)")
                        }
                        items(uiState.disciplineHistory) { past ->
                            HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.MetallicSilver, contentPadding = 12.dp) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(LocalDate.ofEpochDay(past.dateEpochDay).toString(), style = MaterialTheme.typography.titleSmall, color = SystemColors.PureIce)
                                        Text("${past.disciplineScore}/100", style = MaterialTheme.typography.labelLarge, color = SystemColors.ElectricCyan)
                                    }
                                    Text(past.reflectionSummary, style = MaterialTheme.typography.bodySmall, color = SystemColors.MetallicSilver)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FactorBar(label: String, percent: Int, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = SystemColors.PureIce)
            Text("$percent%", style = MaterialTheme.typography.labelMedium, color = color)
        }
        GlowingProgressBar(progress = (percent / 100f).coerceIn(0f, 1f), height = 6.dp, startColor = color, endColor = color)
    }
}

@Composable
fun WeeklyReviewScreen(
    uiState: LifeManagementUiState,
    onRetry: () -> Unit,
    onRefreshReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is LifeManagementUiState.Loading -> SystemLoadingView(modifier = modifier)
        is LifeManagementUiState.Error -> SystemErrorState(uiState.message, onRetry, modifier)
        is LifeManagementUiState.Ready -> {
            val review = uiState.weeklyReview
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(SystemColors.ObsidianBlack),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().widthIn(max = 780.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SystemSectionTitle(
                            title = "Weekly Life Review",
                            subtitle = "7-day audit across Life, Academic & Physical",
                            trailing = {
                                TextButton(onClick = onRefreshReview) { Text("REFRESH") }
                            }
                        )
                    }
                    if (review != null) {
                        item {
                            HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.MonarchGold) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "${LocalDate.ofEpochDay(review.weekStartEpochDay)} → ${LocalDate.ofEpochDay(review.weekEndEpochDay)}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = SystemColors.MonarchGold
                                    )
                                    Text(
                                        text = "Average Discipline: ${review.averageDisciplineScore}/100",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = SystemColors.PureIce
                                    )
                                    Text("Strongest Domain: ${review.strongestDomain}", style = MaterialTheme.typography.bodyMedium, color = SystemColors.VerifiedEmerald)
                                    Text(
                                        text = "Routines: ${review.routineActivitiesCompleted}/${review.routineActivitiesScheduled} steps • Tasks Done: ${review.tasksCompletedCount} (${review.tasksOverdueCount} overdue)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SystemColors.MetallicSilver
                                    )
                                    Text(
                                        text = "Goal Milestones Cleared: ${review.goalMilestonesCompletedCount} • Study Time: ${review.studyMinutesTotal}m • Physical Workouts: ${review.physicalSessionsTotal}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SystemColors.MetallicSilver
                                    )
                                }
                            }
                        }
                        item {
                            HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.ElectricCyan) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = SystemColors.ElectricCyan)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("NEXT-WEEK CALIBRATION", style = MaterialTheme.typography.labelLarge, color = SystemColors.ElectricCyan)
                                        Text(review.priorityAdjustmentAdvice, style = MaterialTheme.typography.bodySmall, color = SystemColors.PureIce)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
