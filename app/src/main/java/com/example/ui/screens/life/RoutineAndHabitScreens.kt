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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.model.HabitCategory
import com.example.domain.model.HabitDayStatus
import com.example.domain.model.HabitType
import com.example.domain.model.RoutineActivityStatus
import com.example.domain.model.RoutineTimeSlot
import com.example.ui.components.GlowingProgressBar
import com.example.ui.components.HolographicPanel
import com.example.ui.components.SystemEmptyState
import com.example.ui.components.SystemErrorState
import com.example.ui.components.SystemLoadingView
import com.example.ui.components.SystemSectionTitle
import com.example.ui.theme.SystemColors
import com.example.ui.viewmodel.LifeManagementUiState
import java.time.DayOfWeek
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoutineManagerScreen(
    uiState: LifeManagementUiState,
    onRetry: () -> Unit,
    onCreateRoutine: (String, String, RoutineTimeSlot, Int, Set<DayOfWeek>, List<Pair<String, Int>>) -> Unit,
    onToggleEnabled: (Long, Boolean) -> Unit,
    onDeleteRoutine: (Long) -> Unit,
    onAddStep: (Long, String, Int) -> Unit,
    onMoveStep: (Long, Long, Boolean) -> Unit,
    onSetStepStatus: (Long, Long, RoutineActivityStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is LifeManagementUiState.Loading -> SystemLoadingView(modifier = modifier)
        is LifeManagementUiState.Error -> SystemErrorState(uiState.message, onRetry, modifier)
        is LifeManagementUiState.Ready -> {
            var showCreateForm by rememberSaveable { mutableStateOf(false) }
            var title by rememberSaveable { mutableStateOf("") }
            var selectedSlot by rememberSaveable { mutableStateOf(RoutineTimeSlot.MORNING) }
            var firstStepTitle by rememberSaveable { mutableStateOf("") }
            var selectedDays by remember { mutableStateOf(DayOfWeek.entries.toSet()) }
            var addStepInputs by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }

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
                            title = "Routine Manager",
                            subtitle = "Morning, afternoon, evening, night & custom blocks",
                            trailing = {
                                OutlinedButton(
                                    onClick = { showCreateForm = !showCreateForm },
                                    modifier = Modifier.testTag("toggle_create_routine_button")
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (showCreateForm) "Close" else "New Routine")
                                }
                            }
                        )
                    }

                    if (showCreateForm) {
                        item {
                            HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.ElectricCyan) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("CREATE ROUTINE (0 XP ON CREATION)", style = MaterialTheme.typography.titleSmall, color = SystemColors.ElectricCyan)
                                    OutlinedTextField(
                                        value = title,
                                        onValueChange = { title = it },
                                        label = { Text("Routine Title (e.g., Morning Sovereignty)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("routine_title_input")
                                    )
                                    OutlinedTextField(
                                        value = firstStepTitle,
                                        onValueChange = { firstStepTitle = it },
                                        label = { Text("First Step (e.g., Hydrate + Make Bed)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("routine_first_step_input")
                                    )
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        RoutineTimeSlot.entries.forEach { slot ->
                                            FilterChip(
                                                selected = selectedSlot == slot,
                                                onClick = { selectedSlot = slot },
                                                label = { Text(slot.label) }
                                            )
                                        }
                                    }
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        DayOfWeek.entries.forEach { dow ->
                                            val chosen = dow in selectedDays
                                            FilterChip(
                                                selected = chosen,
                                                onClick = {
                                                    selectedDays = if (chosen && selectedDays.size > 1) selectedDays - dow else selectedDays + dow
                                                },
                                                label = { Text(dow.name.take(3)) }
                                            )
                                        }
                                    }
                                    Button(
                                        onClick = {
                                            if (title.isNotBlank()) {
                                                val steps = if (firstStepTitle.isBlank()) listOf("Core Routine Check-In" to 15) else listOf(firstStepTitle.trim() to 15)
                                                onCreateRoutine(title, "", selectedSlot, selectedSlot.defaultStartMinute, selectedDays, steps)
                                                title = ""
                                                firstStepTitle = ""
                                                showCreateForm = false
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SystemColors.ElectricCyan, contentColor = SystemColors.ObsidianBlack),
                                        modifier = Modifier.fillMaxWidth().testTag("submit_create_routine_button")
                                    ) {
                                        Text("Save Routine")
                                    }
                                }
                            }
                        }
                    }

                    if (uiState.routineSummaries.isEmpty()) {
                        item {
                            SystemEmptyState("No Routines Configured", "Create a structured morning or evening protocol above.")
                        }
                    } else {
                        items(uiState.routineSummaries, key = { it.routine.id }) { summary ->
                            val r = summary.routine
                            HolographicPanel(
                                modifier = Modifier.fillMaxWidth(),
                                accentColor = if (r.isEnabled) SystemColors.ElectricCyan else SystemColors.MutedSilver
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(r.title, style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
                                            Text(
                                                text = "${r.timeSlot.label} • ${r.scheduledDaysDisplay()} • ${summary.consistencyRate30dPercent}% 30d • ${summary.currentStreakDays}d streak",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SystemColors.MutedSilver
                                            )
                                        }
                                        Switch(checked = r.isEnabled, onCheckedChange = { onToggleEnabled(r.id, it) })
                                        IconButton(onClick = { onDeleteRoutine(r.id) }) {
                                            Icon(Icons.Rounded.Delete, contentDescription = "Delete routine", tint = SystemColors.DangerCrimson)
                                        }
                                    }
                                    GlowingProgressBar(progress = summary.completionFraction, height = 6.dp)
                                    summary.activities.forEachIndexed { idx, act ->
                                        val st = summary.completionsToday[act.id]?.status ?: RoutineActivityStatus.PENDING
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("${act.title} (${act.durationMinutes}m)", style = MaterialTheme.typography.bodySmall, color = SystemColors.PureIce)
                                                Text("Status: ${st.label}", style = MaterialTheme.typography.labelSmall, color = if (st == RoutineActivityStatus.COMPLETED) SystemColors.VerifiedEmerald else SystemColors.MutedSilver)
                                            }
                                            Row {
                                                if (idx > 0) {
                                                    IconButton(onClick = { onMoveStep(r.id, act.id, true) }, modifier = Modifier.size(32.dp)) {
                                                        Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move step up", tint = SystemColors.MutedSilver, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                                if (idx < summary.activities.lastIndex) {
                                                    IconButton(onClick = { onMoveStep(r.id, act.id, false) }, modifier = Modifier.size(32.dp)) {
                                                        Icon(Icons.Rounded.ArrowDownward, contentDescription = "Move step down", tint = SystemColors.MutedSilver, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                                TextButton(onClick = {
                                                    val next = if (st == RoutineActivityStatus.COMPLETED) RoutineActivityStatus.PENDING else RoutineActivityStatus.COMPLETED
                                                    onSetStepStatus(r.id, act.id, next)
                                                }) {
                                                    Text(if (st == RoutineActivityStatus.COMPLETED) "Undo" else "Done")
                                                }
                                                TextButton(onClick = { onSetStepStatus(r.id, act.id, RoutineActivityStatus.SKIPPED) }) {
                                                    Text("Skip")
                                                }
                                            }
                                        }
                                    }

                                    val stepDraft = addStepInputs[r.id].orEmpty()
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedTextField(
                                            value = stepDraft,
                                            onValueChange = { addStepInputs = addStepInputs + (r.id to it) },
                                            label = { Text("Add Step") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedButton(onClick = {
                                            if (stepDraft.isNotBlank()) {
                                                onAddStep(r.id, stepDraft, 10)
                                                addStepInputs = addStepInputs - r.id
                                            }
                                        }) { Text("Add") }
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
fun HabitDashboardScreen(
    uiState: LifeManagementUiState,
    onRetry: () -> Unit,
    onLogHabit: (Long, Double?, HabitDayStatus?, String) -> Unit,
    onOpenHabitEditor: (Long?) -> Unit,
    onOpenHabitAnalytics: (Long?) -> Unit,
    onArchiveHabit: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is LifeManagementUiState.Loading -> SystemLoadingView(modifier = modifier)
        is LifeManagementUiState.Error -> SystemErrorState(uiState.message, onRetry, modifier)
        is LifeManagementUiState.Ready -> {
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
                            title = "Habit Engine",
                            subtitle = "Healthy streaks with Partial & Rest-Exempt support",
                            trailing = {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(onClick = { onOpenHabitAnalytics(null) }) {
                                        Icon(Icons.Rounded.BarChart, contentDescription = "Habit Analytics", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Stats")
                                    }
                                    Button(
                                        onClick = { onOpenHabitEditor(null) },
                                        colors = ButtonDefaults.buttonColors(containerColor = SystemColors.ElectricCyan, contentColor = SystemColors.ObsidianBlack),
                                        modifier = Modifier.testTag("create_habit_button")
                                    ) {
                                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text("Habit")
                                    }
                                }
                            }
                        )
                    }

                    if (uiState.habitSummaries.isEmpty()) {
                        item {
                            SystemEmptyState("No Active Habits", "Create a binary, duration, or count habit using the + Habit button.")
                        }
                    } else {
                        items(uiState.habitSummaries, key = { it.habit.id }) { summary ->
                            val h = summary.habit
                            val accent = when (summary.todayStatus) {
                                HabitDayStatus.COMPLETED -> SystemColors.VerifiedEmerald
                                HabitDayStatus.PARTIAL -> SystemColors.WarningAmber
                                HabitDayStatus.SKIPPED -> SystemColors.ElectricCyan
                                else -> SystemColors.SystemBlue
                            }
                            HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = accent) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(h.title, style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
                                            Text(
                                                text = "${h.habitType.label} • ${h.category.label} • Target: ${h.targetValue.toInt()} ${h.unit}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SystemColors.MutedSilver
                                            )
                                            Text(
                                                text = "Streak: ${summary.currentStreakDays}d (Best ${summary.longestStreakDays}d) • 7d: ${summary.weeklyCompletionRatePercent}% • 30d: ${summary.monthlyCompletionRatePercent}%",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = accent
                                            )
                                        }
                                        Row {
                                            IconButton(onClick = { onOpenHabitEditor(h.id) }) {
                                                Icon(Icons.Rounded.Edit, contentDescription = "Edit habit", tint = SystemColors.ElectricCyan)
                                            }
                                            IconButton(onClick = { onOpenHabitAnalytics(h.id) }) {
                                                Icon(Icons.Rounded.BarChart, contentDescription = "Habit calendar analytics", tint = SystemColors.ArcaneViolet)
                                            }
                                        }
                                    }
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { onLogHabit(h.id, h.targetValue, HabitDayStatus.COMPLETED, "") },
                                            colors = ButtonDefaults.buttonColors(containerColor = SystemColors.VerifiedEmerald, contentColor = SystemColors.ObsidianBlack),
                                            modifier = Modifier.testTag("complete_habit_${h.id}")
                                        ) {
                                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Complete")
                                        }
                                        OutlinedButton(onClick = { onLogHabit(h.id, (h.targetValue / 2.0).coerceAtLeast(0.5), HabitDayStatus.PARTIAL, "") }) {
                                            Text("Partial")
                                        }
                                        OutlinedButton(onClick = { onLogHabit(h.id, 0.0, HabitDayStatus.SKIPPED, "Scheduled rest/recovery") }) {
                                            Text("Skip (Exempt)")
                                        }
                                        TextButton(onClick = { onArchiveHabit(h.id) }) {
                                            Text("Archive", color = SystemColors.MutedSilver)
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
fun HabitEditorScreen(
    habitId: Long?,
    uiState: LifeManagementUiState,
    onRetry: () -> Unit,
    onSaveHabit: (Long?, String, String, HabitType, HabitCategory, Double, String, Set<DayOfWeek>, Int?, Boolean) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is LifeManagementUiState.Loading -> SystemLoadingView(modifier = modifier)
        is LifeManagementUiState.Error -> SystemErrorState(uiState.message, onRetry, modifier)
        is LifeManagementUiState.Ready -> {
            val existing = habitId?.let { id -> uiState.habitSummaries.firstOrNull { it.habit.id == id }?.habit }
            var title by rememberSaveable(habitId) { mutableStateOf(existing?.title.orEmpty()) }
            var description by rememberSaveable(habitId) { mutableStateOf(existing?.description.orEmpty()) }
            var habitType by rememberSaveable(habitId) { mutableStateOf(existing?.habitType ?: HabitType.BINARY) }
            var category by rememberSaveable(habitId) { mutableStateOf(existing?.category ?: HabitCategory.DISCIPLINE) }
            var targetText by rememberSaveable(habitId) { mutableStateOf((existing?.targetValue?.toInt() ?: 1).toString()) }
            var unit by rememberSaveable(habitId) { mutableStateOf(existing?.unit ?: "check") }
            var scheduledDays by remember(habitId) { mutableStateOf(existing?.scheduledDays ?: DayOfWeek.entries.toSet()) }
            var restProtected by rememberSaveable(habitId) { mutableStateOf(existing?.isRestDayProtected ?: true) }

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
                            title = if (existing == null) "Create Habit" else "Edit Habit",
                            subtitle = "Configure measurable type, weekdays & rest-day shield"
                        )
                    }
                    item {
                        HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.ElectricCyan) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = title,
                                    onValueChange = { title = it },
                                    label = { Text("Habit Name") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("habit_editor_title_input")
                                )
                                OutlinedTextField(
                                    value = description,
                                    onValueChange = { description = it },
                                    label = { Text("Intention / Cue") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text("Habit Type", style = MaterialTheme.typography.labelLarge, color = SystemColors.ElectricCyan)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    HabitType.entries.forEach { t ->
                                        FilterChip(selected = habitType == t, onClick = { habitType = t }, label = { Text(t.label) })
                                    }
                                }
                                Text("Target Category", style = MaterialTheme.typography.labelLarge, color = SystemColors.ElectricCyan)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    HabitCategory.entries.forEach { c ->
                                        FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c.label) })
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = targetText,
                                        onValueChange = { targetText = it },
                                        label = { Text("Target Value") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = unit,
                                        onValueChange = { unit = it },
                                        label = { Text("Unit") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Text("Scheduled Days", style = MaterialTheme.typography.labelLarge, color = SystemColors.ElectricCyan)
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    DayOfWeek.entries.forEach { dow ->
                                        val chosen = dow in scheduledDays
                                        FilterChip(
                                            selected = chosen,
                                            onClick = { scheduledDays = if (chosen && scheduledDays.size > 1) scheduledDays - dow else scheduledDays + dow },
                                            label = { Text(dow.name.take(3)) }
                                        )
                                    }
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("Protect streak on exempt rest days", style = MaterialTheme.typography.bodySmall, color = SystemColors.PureIce)
                                    Switch(checked = restProtected, onCheckedChange = { restProtected = it })
                                }
                                Button(
                                    onClick = {
                                        if (title.isNotBlank()) {
                                            val targetVal = (targetText.toDoubleOrNull() ?: 1.0).coerceAtLeast(1.0)
                                            onSaveHabit(habitId, title, description, habitType, category, targetVal, unit, scheduledDays, existing?.preferredStartMinute, restProtected)
                                            onNavigateBack()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SystemColors.ElectricCyan, contentColor = SystemColors.ObsidianBlack),
                                    modifier = Modifier.fillMaxWidth().testTag("save_habit_button")
                                ) {
                                    Text("Save Habit")
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
fun HabitAnalyticsScreen(
    habitId: Long?,
    uiState: LifeManagementUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is LifeManagementUiState.Loading -> SystemLoadingView(modifier = modifier)
        is LifeManagementUiState.Error -> SystemErrorState(uiState.message, onRetry, modifier)
        is LifeManagementUiState.Ready -> {
            var activeId by rememberSaveable(habitId) { mutableStateOf(habitId ?: uiState.habitSummaries.firstOrNull()?.habit?.id) }
            val selected = uiState.habitSummaries.firstOrNull { it.habit.id == activeId } ?: uiState.habitSummaries.firstOrNull()

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
                        SystemSectionTitle("Habit History & Analytics", "30-day matrix honoring scheduled & rest days")
                    }
                    if (selected == null) {
                        item { SystemEmptyState("No Habits Tracked Yet", "Create a habit first to inspect streak history.") }
                    } else {
                        item {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                uiState.habitSummaries.forEach { s ->
                                    FilterChip(
                                        selected = s.habit.id == selected.habit.id,
                                        onClick = { activeId = s.habit.id },
                                        label = { Text(s.habit.title) }
                                    )
                                }
                            }
                        }
                        item {
                            HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.VerifiedEmerald) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(selected.habit.title.uppercase(), style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
                                    Text("Current Streak: ${selected.currentStreakDays}d • Longest Streak: ${selected.longestStreakDays}d", style = MaterialTheme.typography.bodyMedium, color = SystemColors.MonarchGold)
                                    Text("7-Day Rate: ${selected.weeklyCompletionRatePercent}% • 30-Day Rate: ${selected.monthlyCompletionRatePercent}%", style = MaterialTheme.typography.bodySmall, color = SystemColors.ElectricCyan)
                                    Text("30d Breakdown: ${selected.completedDays30d} completed • ${selected.partialDays30d} partial • ${selected.skippedDays30d} rest/skipped • ${selected.missedDays30d} missed", style = MaterialTheme.typography.labelSmall, color = SystemColors.MetallicSilver)
                                }
                            }
                        }
                        item {
                            HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.ElectricCyan) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("LAST 30 DAYS", style = MaterialTheme.typography.labelLarge, color = SystemColors.ElectricCyan)
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        selected.recentCalendar.forEach { cell ->
                                            val color = when (cell.status) {
                                                HabitDayStatus.COMPLETED -> SystemColors.VerifiedEmerald
                                                HabitDayStatus.PARTIAL -> SystemColors.WarningAmber
                                                HabitDayStatus.SKIPPED -> SystemColors.ElectricCyan
                                                HabitDayStatus.MISSED -> SystemColors.DangerCrimson
                                                HabitDayStatus.NOT_SCHEDULED -> SystemColors.MutedSilver
                                            }
                                            Surface(
                                                color = color.copy(alpha = 0.2f),
                                                border = BorderStroke(1.dp, color.copy(alpha = 0.7f)),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = LocalDate.ofEpochDay(cell.dateEpochDay).dayOfMonth.toString(),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = color,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
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
        }
    }
}
