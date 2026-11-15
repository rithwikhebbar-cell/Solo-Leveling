package com.example.ui.screens.life

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.engine.PersonalTaskEngine
import com.example.domain.engine.TaskSortOption
import com.example.domain.model.AttributeType
import com.example.domain.model.GoalTimeframe
import com.example.domain.model.PersonalTaskStatus
import com.example.domain.model.TaskCategory
import com.example.domain.model.TaskPriority
import com.example.domain.model.TaskRecurrenceType
import com.example.ui.components.GlowingProgressBar
import com.example.ui.components.HolographicPanel
import com.example.ui.components.SystemEmptyState
import com.example.ui.components.SystemErrorState
import com.example.ui.components.SystemLoadingView
import com.example.ui.components.SystemSectionTitle
import com.example.ui.theme.SystemColors
import com.example.ui.viewmodel.LifeManagementUiState
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PersonalTaskManagerScreen(
    uiState: LifeManagementUiState,
    onRetry: () -> Unit,
    onCreateTask: (String, String, TaskCategory, TaskPriority, Long?, Int?, Int, Long?, Boolean, TaskRecurrenceType, Int) -> Unit,
    onCompleteTask: (Long) -> Unit,
    onPostponeTask: (Long, Long) -> Unit,
    onArchiveTask: (Long) -> Unit,
    onDeleteTask: (Long) -> Unit,
    onOpenTaskDetails: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is LifeManagementUiState.Loading -> SystemLoadingView(modifier = modifier)
        is LifeManagementUiState.Error -> SystemErrorState(uiState.message, onRetry, modifier)
        is LifeManagementUiState.Ready -> {
            val today = LocalDate.now().toEpochDay()
            var showCreate by rememberSaveable { mutableStateOf(false) }
            var title by rememberSaveable { mutableStateOf("") }
            var desc by rememberSaveable { mutableStateOf("") }
            var category by rememberSaveable { mutableStateOf(TaskCategory.PERSONAL) }
            var priority by rememberSaveable { mutableStateOf(TaskPriority.MEDIUM) }
            var recurrence by rememberSaveable { mutableStateOf(TaskRecurrenceType.NONE) }
            var dueOffsetDays by rememberSaveable { mutableStateOf(0L) }

            var searchQuery by rememberSaveable { mutableStateOf("") }
            var filterCategory by rememberSaveable { mutableStateOf<TaskCategory?>(null) }
            var filterStatus by rememberSaveable { mutableStateOf<PersonalTaskStatus?>(null) }
            var sortOption by rememberSaveable { mutableStateOf(TaskSortOption.DUE_DATE) }

            val visibleTasks = remember(uiState.tasks, filterCategory, filterStatus, searchQuery, sortOption) {
                PersonalTaskEngine.filterAndSort(
                    tasks = uiState.tasks,
                    categoryFilter = filterCategory,
                    statusFilter = filterStatus,
                    searchQuery = searchQuery,
                    sortOption = sortOption,
                    includeArchived = filterStatus == PersonalTaskStatus.ARCHIVED
                )
            }

            Box(
                modifier = modifier.fillMaxSize().background(SystemColors.ObsidianBlack),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().widthIn(max = 780.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SystemSectionTitle(
                            title = "Personal Tasks",
                            subtitle = "One-time, recurring & goal-linked obligations",
                            trailing = {
                                Button(
                                    onClick = { showCreate = !showCreate },
                                    colors = ButtonDefaults.buttonColors(containerColor = SystemColors.ElectricCyan, contentColor = SystemColors.ObsidianBlack),
                                    modifier = Modifier.testTag("toggle_create_task_button")
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text(if (showCreate) "Close" else "Task")
                                }
                            }
                        )
                    }

                    if (showCreate) {
                        item {
                            HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.ElectricCyan) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("NEW PERSONAL TASK (0 XP UNTIL COMPLETED)", style = MaterialTheme.typography.titleSmall, color = SystemColors.ElectricCyan)
                                    OutlinedTextField(
                                        value = title,
                                        onValueChange = { title = it },
                                        label = { Text("Task Title") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("task_title_input")
                                    )
                                    OutlinedTextField(
                                        value = desc,
                                        onValueChange = { desc = it },
                                        label = { Text("Notes / Context") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        TaskCategory.entries.forEach { cat ->
                                            FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat.label) })
                                        }
                                    }
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        TaskPriority.entries.forEach { prio ->
                                            FilterChip(selected = priority == prio, onClick = { priority = prio }, label = { Text(prio.label) })
                                        }
                                    }
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(0L to "Today", 1L to "Tomorrow", 7L to "+7 Days").forEach { (off, label) ->
                                            FilterChip(selected = dueOffsetDays == off, onClick = { dueOffsetDays = off }, label = { Text(label) })
                                        }
                                    }
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        TaskRecurrenceType.entries.forEach { rec ->
                                            FilterChip(selected = recurrence == rec, onClick = { recurrence = rec }, label = { Text(rec.label) })
                                        }
                                    }
                                    Button(
                                        onClick = {
                                            if (title.isNotBlank()) {
                                                onCreateTask(title, desc, category, priority, today + dueOffsetDays, 10 * 60, 30, null, priority == TaskPriority.CRITICAL, recurrence, 2)
                                                title = ""
                                                desc = ""
                                                showCreate = false
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SystemColors.ElectricCyan, contentColor = SystemColors.ObsidianBlack),
                                        modifier = Modifier.fillMaxWidth().testTag("submit_create_task_button")
                                    ) {
                                        Text("Save Task")
                                    }
                                }
                            }
                        }
                    }

                    // Filter & Sort Strip
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                label = { Text("Filter tasks...") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TaskSortOption.entries.forEach { s ->
                                    FilterChip(selected = sortOption == s, onClick = { sortOption = s }, label = { Text("Sort: ${s.label}") })
                                }
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(selected = filterStatus == null, onClick = { filterStatus = null }, label = { Text("All Active") })
                                FilterChip(selected = filterStatus == PersonalTaskStatus.PENDING, onClick = { filterStatus = PersonalTaskStatus.PENDING }, label = { Text("Pending") })
                                FilterChip(selected = filterStatus == PersonalTaskStatus.COMPLETED, onClick = { filterStatus = PersonalTaskStatus.COMPLETED }, label = { Text("Done") })
                                FilterChip(selected = filterStatus == PersonalTaskStatus.POSTPONED, onClick = { filterStatus = PersonalTaskStatus.POSTPONED }, label = { Text("Postponed") })
                            }
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(selected = filterCategory == null, onClick = { filterCategory = null }, label = { Text("All Categories") })
                                TaskCategory.entries.forEach { cat ->
                                    FilterChip(selected = filterCategory == cat, onClick = { filterCategory = cat }, label = { Text(cat.label) })
                                }
                            }
                        }
                    }

                    if (visibleTasks.isEmpty()) {
                        item { SystemEmptyState("No Matching Tasks", "Create a task above or relax active filters.") }
                    } else {
                        items(visibleTasks, key = { it.id }) { task ->
                            val isOverdue = task.isOverdue(today)
                            val accent = when {
                                task.status == PersonalTaskStatus.COMPLETED -> SystemColors.VerifiedEmerald
                                isOverdue -> SystemColors.DangerCrimson
                                task.priority == TaskPriority.CRITICAL -> SystemColors.WarningAmber
                                else -> SystemColors.SystemBlue
                            }
                            HolographicPanel(
                                modifier = Modifier.fillMaxWidth().clickable { onOpenTaskDetails(task.id) },
                                accentColor = accent
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(task.title, style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
                                            val dueText = task.dueDateEpochDay?.let { LocalDate.ofEpochDay(it).toString() } ?: "Unscheduled"
                                            Text(
                                                text = "${task.category.label} • ${task.priority.label} • Due $dueText${if (isOverdue) " (OVERDUE)" else ""}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = accent
                                            )
                                        }
                                        IconButton(onClick = { onDeleteTask(task.id) }) {
                                            Icon(Icons.Rounded.Delete, contentDescription = "Delete task", tint = SystemColors.DangerCrimson)
                                        }
                                    }
                                    if (task.status != PersonalTaskStatus.COMPLETED) {
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = { onCompleteTask(task.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = SystemColors.VerifiedEmerald, contentColor = SystemColors.ObsidianBlack),
                                                modifier = Modifier.testTag("complete_task_${task.id}")
                                            ) {
                                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Complete")
                                            }
                                            OutlinedButton(onClick = { onPostponeTask(task.id, 1L) }) {
                                                Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Postpone +1d")
                                            }
                                            TextButton(onClick = { onArchiveTask(task.id) }) { Text("Archive") }
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskDetailsScreen(
    taskId: Long,
    uiState: LifeManagementUiState,
    onRetry: () -> Unit,
    onCompleteTask: (Long) -> Unit,
    onPostponeTask: (Long, Long) -> Unit,
    onRescheduleTask: (Long, Long, Int?) -> Unit,
    onArchiveTask: (Long) -> Unit,
    onDeleteTask: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is LifeManagementUiState.Loading -> SystemLoadingView(modifier = modifier)
        is LifeManagementUiState.Error -> SystemErrorState(uiState.message, onRetry, modifier)
        is LifeManagementUiState.Ready -> {
            val task = uiState.tasks.firstOrNull { it.id == taskId }
            val today = LocalDate.now().toEpochDay()
            Box(
                modifier = modifier.fillMaxSize().background(SystemColors.ObsidianBlack),
                contentAlignment = Alignment.TopCenter
            ) {
                if (task == null) {
                    SystemEmptyState("Task Not Found", "This task may have been deleted.")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().widthIn(max = 780.dp),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.ElectricCyan) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(task.title.uppercase(), style = MaterialTheme.typography.titleLarge, color = SystemColors.PureIce)
                                    Text("Category: ${task.category.label} • Priority: ${task.priority.label} • Status: ${task.status.label}", style = MaterialTheme.typography.labelLarge, color = SystemColors.ElectricCyan)
                                    if (task.description.isNotBlank()) {
                                        Text(task.description, style = MaterialTheme.typography.bodyMedium, color = SystemColors.MetallicSilver)
                                    }
                                    Text(
                                        text = "Due: ${task.dueDateEpochDay?.let { LocalDate.ofEpochDay(it) } ?: "Unscheduled"} • Est: ${task.estimatedMinutes}m • Postponed: ${task.postponeCount}x",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SystemColors.MutedSilver
                                    )
                                    Text(
                                        text = "Recurrence: ${task.recurrence?.recurrenceType?.label ?: "One-Time"} • Timezone: ${task.timezoneId}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SystemColors.MutedSilver
                                    )
                                }
                            }
                        }
                        item {
                            HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.VerifiedEmerald) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("TASK ACTIONS", style = MaterialTheme.typography.titleSmall, color = SystemColors.PureIce)
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (task.status != PersonalTaskStatus.COMPLETED) {
                                            Button(onClick = { onCompleteTask(task.id) }) { Text("Complete") }
                                            OutlinedButton(onClick = { onPostponeTask(task.id, 1L) }) { Text("Postpone +1d") }
                                            OutlinedButton(onClick = { onRescheduleTask(task.id, today, 9 * 60) }) { Text("Move to Today 09:00") }
                                            OutlinedButton(onClick = { onRescheduleTask(task.id, today + 1L, 14 * 60) }) { Text("Move Tomorrow 14:00") }
                                        }
                                        OutlinedButton(onClick = { onArchiveTask(task.id); onNavigateBack() }) { Text("Archive") }
                                        OutlinedButton(onClick = { onDeleteTask(task.id); onNavigateBack() }) { Text("Delete", color = SystemColors.DangerCrimson) }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GoalManagementScreen(
    uiState: LifeManagementUiState,
    onRetry: () -> Unit,
    onCreateGoal: (String, String, GoalTimeframe, AttributeType, String, Long, List<String>) -> Unit,
    onOpenGoalDetails: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is LifeManagementUiState.Loading -> SystemLoadingView(modifier = modifier)
        is LifeManagementUiState.Error -> SystemErrorState(uiState.message, onRetry, modifier)
        is LifeManagementUiState.Ready -> {
            var showCreate by rememberSaveable { mutableStateOf(false) }
            var title by rememberSaveable { mutableStateOf("") }
            var criteria by rememberSaveable { mutableStateOf("") }
            var milestoneSeed by rememberSaveable { mutableStateOf("") }
            var timeframe by rememberSaveable { mutableStateOf(GoalTimeframe.MONTHLY) }
            var targetAttr by rememberSaveable { mutableStateOf(AttributeType.DIS) }
            var targetDaysAhead by rememberSaveable { mutableStateOf(30L) }

            Box(
                modifier = modifier.fillMaxSize().background(SystemColors.ObsidianBlack),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().widthIn(max = 780.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SystemSectionTitle(
                            title = "Goals & Milestones",
                            subtitle = "Evidence-backed progress across Daily to Long-Term horizons",
                            trailing = {
                                Button(
                                    onClick = { showCreate = !showCreate },
                                    colors = ButtonDefaults.buttonColors(containerColor = SystemColors.MonarchGold, contentColor = SystemColors.ObsidianBlack),
                                    modifier = Modifier.testTag("toggle_create_goal_button")
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text(if (showCreate) "Close" else "Goal")
                                }
                            }
                        )
                    }

                    if (showCreate) {
                        item {
                            HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.MonarchGold) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = title,
                                        onValueChange = { title = it },
                                        label = { Text("Goal Objective") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("goal_title_input")
                                    )
                                    OutlinedTextField(
                                        value = criteria,
                                        onValueChange = { criteria = it },
                                        label = { Text("Measurable Completion Criteria") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = milestoneSeed,
                                        onValueChange = { milestoneSeed = it },
                                        label = { Text("First Milestone Title") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        GoalTimeframe.entries.forEach { tf ->
                                            FilterChip(selected = timeframe == tf, onClick = {
                                                timeframe = tf
                                                targetDaysAhead = when (tf) {
                                                    GoalTimeframe.DAILY -> 1L
                                                    GoalTimeframe.WEEKLY -> 7L
                                                    GoalTimeframe.MONTHLY -> 30L
                                                    GoalTimeframe.QUARTERLY -> 90L
                                                    GoalTimeframe.ANNUAL -> 365L
                                                    GoalTimeframe.LONG_TERM -> 730L
                                                }
                                            }, label = { Text(tf.label) })
                                        }
                                    }
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(AttributeType.DIS, AttributeType.WIS, AttributeType.CHA, AttributeType.INT, AttributeType.VIT).forEach { attr ->
                                            FilterChip(selected = targetAttr == attr, onClick = { targetAttr = attr }, label = { Text(attr.code) })
                                        }
                                    }
                                    Button(
                                        onClick = {
                                            if (title.isNotBlank()) {
                                                val today = LocalDate.now().toEpochDay()
                                                val milestones = if (milestoneSeed.isBlank()) listOf("Phase 1 Foundation") else listOf(milestoneSeed)
                                                onCreateGoal(title, "", timeframe, targetAttr, criteria.ifBlank { "Complete defined milestones" }, today + targetDaysAhead, milestones)
                                                title = ""
                                                criteria = ""
                                                milestoneSeed = ""
                                                showCreate = false
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SystemColors.MonarchGold, contentColor = SystemColors.ObsidianBlack),
                                        modifier = Modifier.fillMaxWidth().testTag("submit_create_goal_button")
                                    ) { Text("Create Goal") }
                                }
                            }
                        }
                    }

                    if (uiState.goalsWithDetails.isEmpty()) {
                        item { SystemEmptyState("No Structured Goals", "Add a Weekly, Monthly, or Long-Term goal above.") }
                    } else {
                        items(uiState.goalsWithDetails, key = { it.goal.id }) { item ->
                            val g = item.goal
                            val accent = if (item.isBehindSchedule) SystemColors.WarningAmber else SystemColors.MonarchGold
                            HolographicPanel(
                                modifier = Modifier.fillMaxWidth().clickable { onOpenGoalDetails(g.id) },
                                accentColor = accent
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Icon(Icons.Rounded.Flag, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(g.title, style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
                                                Text("${g.timeframe.label} • ${g.targetAttribute.code} • Target ${LocalDate.ofEpochDay(g.targetDateEpochDay)}", style = MaterialTheme.typography.labelSmall, color = SystemColors.MutedSilver)
                                            }
                                        }
                                        Text("${item.computedProgressPercent}%", style = MaterialTheme.typography.titleMedium, color = accent)
                                    }
                                    GlowingProgressBar(progress = item.computedProgressPercent / 100f, height = 7.dp, startColor = accent, endColor = SystemColors.ElectricCyan)
                                    Text(
                                        text = "${item.completedMilestonesCount}/${item.totalMilestonesCount} milestones complete${if (item.isBehindSchedule) " • Behind target pace" else ""}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (item.isBehindSchedule) SystemColors.WarningAmber else SystemColors.MetallicSilver
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

@Composable
fun GoalDetailsScreen(
    goalId: Long,
    uiState: LifeManagementUiState,
    onRetry: () -> Unit,
    onAddMilestone: (Long, String, Int, Long?) -> Unit,
    onCompleteMilestone: (Long) -> Unit,
    onSubmitReview: (Long, String, String) -> Unit,
    onDeleteGoal: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is LifeManagementUiState.Loading -> SystemLoadingView(modifier = modifier)
        is LifeManagementUiState.Error -> SystemErrorState(uiState.message, onRetry, modifier)
        is LifeManagementUiState.Ready -> {
            val details = uiState.goalsWithDetails.firstOrNull { it.goal.id == goalId }
            var newMilestoneTitle by rememberSaveable { mutableStateOf("") }
            var reviewNote by rememberSaveable { mutableStateOf("") }
            var adjustmentNote by rememberSaveable { mutableStateOf("") }

            Box(
                modifier = modifier.fillMaxSize().background(SystemColors.ObsidianBlack),
                contentAlignment = Alignment.TopCenter
            ) {
                if (details == null) {
                    SystemEmptyState("Goal Not Found", "This goal may have been removed.")
                } else {
                    val g = details.goal
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().widthIn(max = 780.dp),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.MonarchGold) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(g.title.uppercase(), style = MaterialTheme.typography.titleLarge, color = SystemColors.PureIce, modifier = Modifier.weight(1f))
                                        IconButton(onClick = { onDeleteGoal(g.id); onNavigateBack() }) {
                                            Icon(Icons.Rounded.Delete, contentDescription = "Delete goal", tint = SystemColors.DangerCrimson)
                                        }
                                    }
                                    Text("Status: ${g.status.label} • Real Progress: ${details.computedProgressPercent}%", style = MaterialTheme.typography.labelLarge, color = SystemColors.MonarchGold)
                                    GlowingProgressBar(progress = details.computedProgressPercent / 100f, height = 8.dp)
                                    if (g.completionCriteria.isNotBlank()) {
                                        Text("Criteria: ${g.completionCriteria}", style = MaterialTheme.typography.bodySmall, color = SystemColors.MetallicSilver)
                                    }
                                }
                            }
                        }

                        item {
                            HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.ElectricCyan) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("MILESTONES (${details.completedMilestonesCount}/${details.totalMilestonesCount})", style = MaterialTheme.typography.titleSmall, color = SystemColors.ElectricCyan)
                                    details.milestones.forEach { m ->
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = m.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (m.isCompleted) SystemColors.VerifiedEmerald else SystemColors.PureIce,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (m.isCompleted) {
                                                Text("DONE", style = MaterialTheme.typography.labelMedium, color = SystemColors.VerifiedEmerald)
                                            } else {
                                                Button(
                                                    onClick = { onCompleteMilestone(m.id) },
                                                    modifier = Modifier.testTag("complete_milestone_${m.id}")
                                                ) { Text("Complete") }
                                            }
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedTextField(
                                            value = newMilestoneTitle,
                                            onValueChange = { newMilestoneTitle = it },
                                            label = { Text("Add Milestone") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedButton(onClick = {
                                            if (newMilestoneTitle.isNotBlank()) {
                                                onAddMilestone(g.id, newMilestoneTitle, 1, null)
                                                newMilestoneTitle = ""
                                            }
                                        }) { Text("Add") }
                                    }
                                }
                            }
                        }

                        item {
                            HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.ArcaneViolet) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("GOAL REVIEW & ADJUSTMENT", style = MaterialTheme.typography.titleSmall, color = SystemColors.ArcaneViolet)
                                    OutlinedTextField(value = reviewNote, onValueChange = { reviewNote = it }, label = { Text("Progress Review Note") }, modifier = Modifier.fillMaxWidth())
                                    OutlinedTextField(value = adjustmentNote, onValueChange = { adjustmentNote = it }, label = { Text("Plan / Scope Adjustment") }, modifier = Modifier.fillMaxWidth())
                                    Button(
                                        onClick = {
                                            onSubmitReview(g.id, reviewNote, adjustmentNote)
                                            reviewNote = ""
                                            adjustmentNote = ""
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("Log Goal Review") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
