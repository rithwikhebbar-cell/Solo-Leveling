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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.model.ScheduledActivityItem
import com.example.domain.model.ScheduledSourceType
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
fun DailyScheduleScreen(
    uiState: LifeManagementUiState,
    onRetry: () -> Unit,
    onSelectDate: (Long) -> Unit,
    onAddScheduledEvent: (String, String, Int, Int) -> Unit,
    onRescheduleItem: (ScheduledActivityItem, Long, Int) -> Unit,
    onLoadSuggestions: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is LifeManagementUiState.Loading -> SystemLoadingView(modifier = modifier)
        is LifeManagementUiState.Error -> SystemErrorState(uiState.message, onRetry, modifier)
        is LifeManagementUiState.Ready -> {
            val selectedDate = LocalDate.ofEpochDay(uiState.selectedDateEpochDay)
            var showAgendaMode by rememberSaveable { mutableStateOf(false) }
            var showAddEvent by rememberSaveable { mutableStateOf(false) }
            var eventTitle by rememberSaveable { mutableStateOf("") }
            var eventStartHourText by rememberSaveable { mutableStateOf("14") }
            var eventDurationText by rememberSaveable { mutableStateOf("45") }

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
                            title = if (showAgendaMode) "Agenda View" else "Daily Schedule",
                            subtitle = "Unified reference timeline across Life, Academic & Physical",
                            trailing = {
                                OutlinedButton(onClick = { showAddEvent = !showAddEvent }, modifier = Modifier.testTag("toggle_add_event_button")) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text(if (showAddEvent) "Close" else "Block")
                                }
                            }
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onSelectDate(uiState.selectedDateEpochDay - 1L) }) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Previous day", tint = SystemColors.ElectricCyan)
                            }
                            Text(selectedDate.toString(), style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
                            IconButton(onClick = { onSelectDate(uiState.selectedDateEpochDay + 1L) }) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Next day", tint = SystemColors.ElectricCyan)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = !showAgendaMode, onClick = { showAgendaMode = false }, label = { Text("Timeline") })
                            FilterChip(selected = showAgendaMode, onClick = { showAgendaMode = true }, label = { Text("Agenda List") })
                        }
                    }

                    if (showAddEvent) {
                        item {
                            HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.ArcaneViolet) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = eventTitle,
                                        onValueChange = { eventTitle = it },
                                        label = { Text("Block Title") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("schedule_event_title_input")
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = eventStartHourText,
                                            onValueChange = { eventStartHourText = it },
                                            label = { Text("Start Hour (0-23)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = eventDurationText,
                                            onValueChange = { eventDurationText = it },
                                            label = { Text("Duration (min)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            if (eventTitle.isNotBlank()) {
                                                val hour = (eventStartHourText.toIntOrNull() ?: 14).coerceIn(0, 23)
                                                val dur = (eventDurationText.toIntOrNull() ?: 45).coerceIn(10, 360)
                                                onAddScheduledEvent(eventTitle, "Custom Life Block", hour * 60, dur)
                                                eventTitle = ""
                                                showAddEvent = false
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().testTag("submit_schedule_event_button")
                                    ) { Text("Add to Timeline") }
                                }
                            }
                        }
                    }

                    // Conflict Warning & Slot Resolution
                    if (uiState.selectedDayConflicts.isNotEmpty()) {
                        item {
                            HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.WarningAmber) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Warning, contentDescription = "Schedule Conflict", tint = SystemColors.WarningAmber)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("${uiState.selectedDayConflicts.size} OVERLAP CONFLICT(S)", style = MaterialTheme.typography.titleSmall, color = SystemColors.WarningAmber)
                                    }
                                    uiState.selectedDayConflicts.forEach { conflict ->
                                        Text(conflict.description, style = MaterialTheme.typography.bodySmall, color = SystemColors.PureIce)
                                        val movable = if (!conflict.second.isReadOnlyExternal) conflict.second else if (!conflict.first.isReadOnlyExternal) conflict.first else null
                                        val freeSlot = uiState.slotSuggestions.firstOrNull()
                                        if (movable != null && freeSlot != null) {
                                            OutlinedButton(onClick = {
                                                onRescheduleItem(movable, freeSlot.dateEpochDay, freeSlot.startMinuteOfDay)
                                            }) {
                                                Text("Move '${movable.title}' → ${freeSlot.formattedWindow()}")
                                            }
                                        }
                                    }
                                    TextButton(onClick = { onLoadSuggestions(30) }) {
                                        Text("Refresh Free Slot Suggestions")
                                    }
                                }
                            }
                        }
                    }

                    if (uiState.selectedDaySchedule.isEmpty()) {
                        item {
                            SystemEmptyState("Open Day", "No scheduled routines, tasks, study blocks, or workouts on this date.")
                        }
                    } else {
                        items(uiState.selectedDaySchedule) { item ->
                            val accent = when {
                                item.hasConflict -> SystemColors.WarningAmber
                                item.isCompleted -> SystemColors.VerifiedEmerald
                                item.sourceType.moduleTag == "ACADEMIC" -> SystemColors.ArcaneViolet
                                item.sourceType.moduleTag == "PHYSICAL" -> SystemColors.VerifiedEmerald
                                else -> SystemColors.ElectricCyan
                            }
                            HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = accent, contentPadding = 12.dp) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(item.formattedWindow(), style = MaterialTheme.typography.labelLarge, color = accent)
                                        Surface(
                                            color = accent.copy(alpha = 0.14f),
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, accent.copy(alpha = 0.4f))
                                        ) {
                                            Text(item.sourceType.label.uppercase(), style = MaterialTheme.typography.labelSmall, color = accent, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (item.isCompleted) {
                                            Icon(Icons.Rounded.CheckCircle, contentDescription = "Completed", tint = SystemColors.VerifiedEmerald, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        Text(item.title, style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
                                    }
                                    Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = SystemColors.MutedSilver)
                                    if (!item.isReadOnlyExternal && !showAgendaMode && uiState.slotSuggestions.isNotEmpty()) {
                                        val nextSlot = uiState.slotSuggestions.first()
                                        TextButton(onClick = { onRescheduleItem(item, nextSlot.dateEpochDay, nextSlot.startMinuteOfDay) }) {
                                            Text("Move to open slot ${nextSlot.formattedWindow()}")
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

@Composable
fun WeeklyCalendarScreen(
    uiState: LifeManagementUiState,
    onRetry: () -> Unit,
    onSelectDateAndOpenDaily: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is LifeManagementUiState.Loading -> SystemLoadingView(modifier = modifier)
        is LifeManagementUiState.Error -> SystemErrorState(uiState.message, onRetry, modifier)
        is LifeManagementUiState.Ready -> {
            val anchor = LocalDate.ofEpochDay(uiState.selectedDateEpochDay)
            val monday = anchor.with(DayOfWeek.MONDAY).toEpochDay()
            val days = (0L..6L).map { monday + it }

            Box(
                modifier = modifier.fillMaxSize().background(SystemColors.ObsidianBlack),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().widthIn(max = 780.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        SystemSectionTitle("Weekly Calendar", "Tap any day to inspect timeline & resolve overlaps")
                    }
                    items(days) { dayEpoch ->
                        val date = LocalDate.ofEpochDay(dayEpoch)
                        val items = uiState.weekScheduleByDay[dayEpoch].orEmpty()
                        val hasConflict = items.any { it.hasConflict }
                        val accent = when {
                            hasConflict -> SystemColors.WarningAmber
                            dayEpoch == uiState.selectedDateEpochDay -> SystemColors.ElectricCyan
                            else -> SystemColors.SystemBlue
                        }
                        HolographicPanel(
                            modifier = Modifier.fillMaxWidth().clickable { onSelectDateAndOpenDaily(dayEpoch) },
                            accentColor = accent,
                            contentPadding = 12.dp
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${date.dayOfWeek.name.take(3)} • $date", style = MaterialTheme.typography.titleSmall, color = SystemColors.PureIce)
                                    Text("${items.size} item(s)${if (hasConflict) " • CONFLICT" else ""}", style = MaterialTheme.typography.labelMedium, color = accent)
                                }
                                if (items.isNotEmpty()) {
                                    Text(
                                        text = items.take(3).joinToString(" | ") { "${it.formattedWindow()} ${it.title}" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SystemColors.MutedSilver
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MonthlyCalendarScreen(
    uiState: LifeManagementUiState,
    onRetry: () -> Unit,
    onSelectDateAndOpenDaily: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is LifeManagementUiState.Loading -> SystemLoadingView(modifier = modifier)
        is LifeManagementUiState.Error -> SystemErrorState(uiState.message, onRetry, modifier)
        is LifeManagementUiState.Ready -> {
            val anchor = LocalDate.ofEpochDay(uiState.selectedDateEpochDay)
            val firstDay = anchor.withDayOfMonth(1)
            val daysInMonth = anchor.lengthOfMonth()
            val today = LocalDate.now().toEpochDay()

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
                            title = "${anchor.month.name} ${anchor.year}",
                            subtitle = "Monthly density across Routines, Habits, Tasks, Exams & Training"
                        )
                    }
                    item {
                        HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.ElectricCyan) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                (1..daysInMonth).forEach { dom ->
                                    val dayEpoch = firstDay.withDayOfMonth(dom).toEpochDay()
                                    val count = uiState.monthScheduleCounts[dayEpoch] ?: 0
                                    val isToday = dayEpoch == today
                                    val accent = when {
                                        isToday -> SystemColors.ElectricCyan
                                        count > 0 -> SystemColors.VerifiedEmerald
                                        else -> SystemColors.MutedSilver
                                    }
                                    Surface(
                                        color = accent.copy(alpha = if (count > 0 || isToday) 0.18f else 0.08f),
                                        border = BorderStroke(1.dp, accent.copy(alpha = 0.6f)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.clickable { onSelectDateAndOpenDaily(dayEpoch) }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(dom.toString(), style = MaterialTheme.typography.titleSmall, color = SystemColors.PureIce)
                                            Text("${count}b", style = MaterialTheme.typography.labelSmall, color = accent)
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
