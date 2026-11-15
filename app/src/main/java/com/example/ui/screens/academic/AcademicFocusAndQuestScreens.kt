package com.example.ui.screens.academic

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.engine.FocusTimerCalculator
import com.example.domain.model.AcademicQuestCategory
import com.example.domain.model.AcademicQuestDifficulty
import com.example.domain.model.AcademicQuestStatus
import com.example.domain.model.AcademicSubject
import com.example.domain.model.AcademicTopic
import com.example.domain.model.FocusPreset
import com.example.ui.components.GlowingProgressBar
import com.example.ui.components.HolographicPanel
import com.example.ui.components.ProvenanceChip
import com.example.ui.components.SystemEmptyState
import com.example.ui.components.SystemErrorState
import com.example.ui.components.SystemLoadingView
import com.example.ui.components.SystemSectionTitle
import com.example.ui.theme.SystemColors
import com.example.ui.viewmodel.AcademicUiState
import kotlinx.coroutines.delay

/**
 * Screen 5: Focus Timer & Study Session Engine.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AcademicFocusTimerScreen(
    preselectedSubjectId: Long?,
    uiState: AcademicUiState,
    onRetry: () -> Unit,
    onStartTimer: (FocusPreset, Int, Int, AcademicSubject, AcademicTopic?, String) -> Unit,
    onPauseResume: () -> Unit,
    onToggleBreakPhase: () -> Unit,
    onCompleteTimer: (String, String) -> Unit,
    onDiscardTimer: () -> Unit,
    onLogManualStudy: (Long, Long?, String, Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is AcademicUiState.Loading -> SystemLoadingView("INITIALIZING FOCUS ENGINE...", modifier)
        is AcademicUiState.Error -> Box(modifier.fillMaxSize().background(SystemColors.ObsidianBlack).padding(20.dp)) {
            SystemErrorState(uiState.message, onRetry)
        }
        is AcademicUiState.Ready -> {
            val activeSubjects = uiState.subjects.filter { !it.isArchived }
            var selectedSubjectId by rememberSaveable(preselectedSubjectId) {
                mutableStateOf(preselectedSubjectId ?: activeSubjects.firstOrNull()?.id)
            }
            var selectedTopicId by rememberSaveable { mutableStateOf<Long?>(null) }
            var selectedPreset by rememberSaveable { mutableStateOf(FocusPreset.POMODORO_25) }
            var customFocusText by rememberSaveable { mutableStateOf("40") }
            var customBreakText by rememberSaveable { mutableStateOf("10") }
            var sessionTitle by rememberSaveable { mutableStateOf("") }
            var sessionNotes by rememberSaveable { mutableStateOf("") }
            var showManualSheet by rememberSaveable { mutableStateOf(false) }

            val timer = uiState.activeFocusTimer
            var tickerNowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
            LaunchedEffect(timer.isActive, timer.isPaused) {
                while (timer.isActive && !timer.isPaused) {
                    tickerNowMs = System.currentTimeMillis()
                    delay(1000L)
                }
            }

            val elapsedSec = timer.currentElapsedSeconds(tickerNowMs)
            val targetMins = if (timer.isBreakPhase) timer.targetBreakMinutes.coerceAtLeast(1) else timer.targetFocusMinutes
            val snapshot = FocusTimerCalculator.calculate(elapsedSec, targetMins)
            val selectedSubject = activeSubjects.find { it.id == selectedSubjectId } ?: activeSubjects.firstOrNull()
            val subjectTopics = uiState.topics.filter { it.subjectId == selectedSubject?.id }

            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(SystemColors.ObsidianBlack),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 760.dp)
                        .testTag("academic_focus_timer_screen"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        SystemSectionTitle(
                            title = "Focus Engine",
                            subtitle = "System Timer verifies in-app study duration — not actual learning mastery",
                            trailing = {
                                OutlinedButton(
                                    onClick = { showManualSheet = true },
                                    enabled = activeSubjects.isNotEmpty(),
                                    modifier = Modifier.testTag("open_manual_study_sheet")
                                ) {
                                    Text("LOG OFFLINE", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        )
                    }

                    item {
                        HolographicPanel(
                            modifier = Modifier.fillMaxWidth(),
                            accentColor = if (timer.isBreakPhase) SystemColors.ArcaneViolet else SystemColors.ElectricCyan
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (!timer.isActive) {
                                    if (activeSubjects.isEmpty()) {
                                        SystemEmptyState(
                                            title = "Create a Subject First",
                                            description = "Add an active subject in Subject Management to bind your focus session."
                                        )
                                    } else {
                                        Text("PRESET BLOCK", style = MaterialTheme.typography.labelMedium, color = SystemColors.MetallicSilver)
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            FocusPreset.entries.forEach { preset ->
                                                FilterChip(
                                                    selected = selectedPreset == preset,
                                                    onClick = { selectedPreset = preset },
                                                    label = { Text(preset.label) }
                                                )
                                            }
                                        }

                                        if (selectedPreset == FocusPreset.CUSTOM) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                OutlinedTextField(
                                                    value = customFocusText,
                                                    onValueChange = { customFocusText = it.filter { c -> c.isDigit() }.take(3) },
                                                    label = { Text("Focus (min)") },
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.weight(1f),
                                                    colors = academicFieldColors()
                                                )
                                                OutlinedTextField(
                                                    value = customBreakText,
                                                    onValueChange = { customBreakText = it.filter { c -> c.isDigit() }.take(2) },
                                                    label = { Text("Break (min)") },
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    modifier = Modifier.weight(1f),
                                                    colors = academicFieldColors()
                                                )
                                            }
                                        }

                                        Text("SUBJECT", style = MaterialTheme.typography.labelMedium, color = SystemColors.MetallicSilver)
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            activeSubjects.forEach { subj ->
                                                FilterChip(
                                                    selected = selectedSubject?.id == subj.id,
                                                    onClick = {
                                                        selectedSubjectId = subj.id
                                                        selectedTopicId = null
                                                    },
                                                    label = { Text(subj.name) }
                                                )
                                            }
                                        }

                                        if (subjectTopics.isNotEmpty()) {
                                            Text("OPTIONAL TOPIC", style = MaterialTheme.typography.labelMedium, color = SystemColors.MetallicSilver)
                                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                subjectTopics.take(8).forEach { topic ->
                                                    FilterChip(
                                                        selected = selectedTopicId == topic.id,
                                                        onClick = {
                                                            selectedTopicId = if (selectedTopicId == topic.id) null else topic.id
                                                        },
                                                        label = { Text(topic.title) }
                                                    )
                                                }
                                            }
                                        }

                                        OutlinedTextField(
                                            value = sessionNotes,
                                            onValueChange = { sessionNotes = it },
                                            label = { Text("Study Intention / Notes") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = academicFieldColors()
                                        )

                                        val focusMins = if (selectedPreset == FocusPreset.CUSTOM) {
                                            (customFocusText.toIntOrNull() ?: 40).coerceIn(5, 180)
                                        } else selectedPreset.focusMinutes
                                        val breakMins = if (selectedPreset == FocusPreset.CUSTOM) {
                                            (customBreakText.toIntOrNull() ?: 10).coerceIn(0, 45)
                                        } else selectedPreset.breakMinutes

                                        Button(
                                            onClick = {
                                                if (selectedSubject != null) {
                                                    val topic = subjectTopics.find { it.id == selectedTopicId }
                                                    onStartTimer(selectedPreset, focusMins, breakMins, selectedSubject, topic, sessionNotes)
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("start_focus_timer_button"),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = SystemColors.ElectricCyan,
                                                contentColor = SystemColors.ObsidianBlack
                                            )
                                        ) {
                                            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("START ${focusMins}M FOCUS BLOCK", style = MaterialTheme.typography.labelLarge)
                                        }
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = if (timer.isBreakPhase) "RECOVERY BREAK PHASE" else "ACTIVE STUDY TIMER",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (timer.isBreakPhase) SystemColors.ArcaneViolet else SystemColors.ElectricCyan
                                            )
                                            Text(
                                                text = timer.subjectName + if (timer.topicTitle.isNotBlank()) " • ${timer.topicTitle}" else "",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = SystemColors.PureIce
                                            )
                                        }
                                        Text(
                                            text = if (timer.isPaused) "PAUSED" else "RUNNING",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (timer.isPaused) SystemColors.WarningAmber else SystemColors.VerifiedEmerald
                                        )
                                    }

                                    Text(
                                        text = FocusTimerCalculator.formatDurationMmSs(snapshot.elapsedSeconds),
                                        style = MaterialTheme.typography.displayLarge,
                                        color = SystemColors.PureIce
                                    )
                                    Text(
                                        text = "Target: ${targetMins}:00 • Remaining: ${FocusTimerCalculator.formatDurationMmSs(snapshot.remainingSeconds)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SystemColors.MutedSilver
                                    )
                                    GlowingProgressBar(progress = snapshot.progressFraction, height = 8.dp)

                                    OutlinedTextField(
                                        value = sessionTitle,
                                        onValueChange = { sessionTitle = it },
                                        label = { Text("Session Title (for completion log)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = academicFieldColors()
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(onClick = onPauseResume, modifier = Modifier.weight(1f)) {
                                            Icon(if (timer.isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (timer.isPaused) "RESUME" else "PAUSE")
                                        }
                                        OutlinedButton(onClick = onToggleBreakPhase, modifier = Modifier.weight(1f)) {
                                            Text(if (timer.isBreakPhase) "STUDY PHASE" else "BREAK (${timer.targetBreakMinutes}m)")
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { onCompleteTimer(sessionTitle, sessionNotes) },
                                            modifier = Modifier.weight(1f).testTag("complete_focus_timer_button"),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = SystemColors.VerifiedEmerald,
                                                contentColor = SystemColors.ObsidianBlack
                                            )
                                        ) {
                                            Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("COMPLETE")
                                        }
                                        OutlinedButton(onClick = onDiscardTimer, modifier = Modifier.weight(1f)) {
                                            Text("DISCARD", color = SystemColors.DangerCrimson)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        SystemSectionTitle(
                            title = "Recent Study & Focus Sessions",
                            subtitle = "${uiState.studySessions.size} logged sessions"
                        )
                    }

                    if (uiState.studySessions.isEmpty()) {
                        item {
                            SystemEmptyState("No Study Sessions Yet", "Complete a focus block or log an offline session to build Academic XP.")
                        }
                    } else {
                        items(uiState.studySessions.take(15), key = { it.id }) { s ->
                            HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.ElectricCyan, contentPadding = 12.dp) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("${s.subjectName}: ${s.title}", style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce, modifier = Modifier.weight(1f))
                                        ProvenanceChip(status = s.verificationStatus)
                                    }
                                    Text(
                                        text = "${s.durationMinutes} min • +${s.xpAwarded} XP • ${formatAcademicDateTime(s.endEpochMs)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SystemColors.MetallicSilver
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (showManualSheet && selectedSubject != null) {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ModalBottomSheet(
                    onDismissRequest = { showManualSheet = false },
                    sheetState = sheetState,
                    containerColor = SystemColors.DeepNavy,
                    contentColor = SystemColors.PureIce
                ) {
                    var title by rememberSaveable { mutableStateOf("") }
                    var minsText by rememberSaveable { mutableStateOf("35") }
                    var notes by rememberSaveable { mutableStateOf("") }
                    val mins = minsText.toIntOrNull() ?: 0
                    Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("LOG OFFLINE STUDY SESSION", style = MaterialTheme.typography.titleLarge, color = SystemColors.ElectricCyan)
                        Text("Subject: ${selectedSubject.name} • Recorded as Self-Reported (0.65x weight).", style = MaterialTheme.typography.bodySmall, color = SystemColors.MutedSilver)
                        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Session Title") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = academicFieldColors())
                        OutlinedTextField(
                            value = minsText,
                            onValueChange = { minsText = it.filter { c -> c.isDigit() }.take(3) },
                            label = { Text("Duration (5–360 min)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            colors = academicFieldColors()
                        )
                        OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), colors = academicFieldColors())
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(onClick = { showManualSheet = false }, modifier = Modifier.weight(1f)) { Text("CANCEL") }
                            Button(
                                onClick = {
                                    onLogManualStudy(selectedSubject.id, selectedTopicId, title, mins, notes)
                                    showManualSheet = false
                                },
                                enabled = title.isNotBlank() && mins in 5..360,
                                modifier = Modifier.weight(1f)
                            ) { Text("RECORD STUDY") }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Screen 6: Study Quest Board (all 7 categories, auto-generation, manual creation, completion, cancellation).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AcademicQuestsScreen(
    uiState: AcademicUiState,
    onRetry: () -> Unit,
    onCreateQuest: (String, String, AcademicQuestCategory, Long?, AcademicQuestDifficulty, String, Int, String) -> Unit,
    onGenerateFromPlan: () -> Unit,
    onCompleteQuest: (Long, String) -> Unit,
    onCancelQuest: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is AcademicUiState.Loading -> SystemLoadingView("LOADING STUDY QUESTS...", modifier)
        is AcademicUiState.Error -> Box(modifier.fillMaxSize().background(SystemColors.ObsidianBlack).padding(20.dp)) {
            SystemErrorState(uiState.message, onRetry)
        }
        is AcademicUiState.Ready -> {
            var statusFilter by rememberSaveable { mutableStateOf(AcademicQuestStatus.ACTIVE) }
            var categoryFilter by rememberSaveable { mutableStateOf<AcademicQuestCategory?>(null) }
            var showCreateSheet by rememberSaveable { mutableStateOf(false) }

            val filtered = uiState.academicQuests.filter {
                it.status == statusFilter && (categoryFilter == null || it.category == categoryFilter)
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
                        .widthIn(max = 760.dp)
                        .testTag("academic_quests_screen"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SystemSectionTitle(
                            title = "Study Quest Board",
                            subtitle = "0 XP for creating quests • Rewards unlock on completion",
                            trailing = {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = onGenerateFromPlan,
                                        modifier = Modifier.testTag("generate_study_quests_button"),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("AUTO-GEN", style = MaterialTheme.typography.labelSmall)
                                    }
                                    Button(
                                        onClick = { showCreateSheet = true },
                                        modifier = Modifier.testTag("create_academic_quest_button"),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("NEW", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        )
                    }

                    item {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            AcademicQuestStatus.entries.forEach { st ->
                                FilterChip(
                                    selected = statusFilter == st,
                                    onClick = { statusFilter = st },
                                    label = { Text("${st.label} (${uiState.academicQuests.count { it.status == st }})") }
                                )
                            }
                        }
                    }

                    item {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(selected = categoryFilter == null, onClick = { categoryFilter = null }, label = { Text("All Categories") })
                            AcademicQuestCategory.entries.forEach { cat ->
                                FilterChip(
                                    selected = categoryFilter == cat,
                                    onClick = { categoryFilter = if (categoryFilter == cat) null else cat },
                                    label = { Text(cat.label) }
                                )
                            }
                        }
                    }

                    if (filtered.isEmpty()) {
                        item {
                            SystemEmptyState("No Quests Match Filter", "Tap AUTO-GEN to derive quests from your subjects and overdue revisions, or create one manually.")
                        }
                    } else {
                        items(filtered, key = { it.id }) { q ->
                            HolographicPanel(
                                modifier = Modifier.fillMaxWidth(),
                                accentColor = if (q.status == AcademicQuestStatus.COMPLETED) SystemColors.VerifiedEmerald else SystemColors.ArcaneViolet
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("${q.category.label.uppercase()} • ${q.difficulty.label}", style = MaterialTheme.typography.labelSmall, color = SystemColors.ElectricCyan)
                                        Text("+${q.xpReward} XP • ${q.primaryAttribute.code}", style = MaterialTheme.typography.labelMedium, color = SystemColors.VerifiedEmerald)
                                    }
                                    Text(q.title, style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
                                    Text(q.description, style = MaterialTheme.typography.bodySmall, color = SystemColors.MetallicSilver)
                                    Text("Objective: ${q.objective} (${q.targetValue} ${q.unit})", style = MaterialTheme.typography.labelSmall, color = SystemColors.MutedSilver)

                                    if (q.status == AcademicQuestStatus.ACTIVE) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = { onCompleteQuest(q.id, "Completed: ${q.objective}") },
                                                modifier = Modifier.weight(1f).testTag("complete_acad_quest_${q.id}"),
                                                colors = ButtonDefaults.buttonColors(containerColor = SystemColors.VerifiedEmerald, contentColor = SystemColors.ObsidianBlack)
                                            ) {
                                                Icon(Icons.Rounded.Done, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("COMPLETE")
                                            }
                                            OutlinedButton(
                                                onClick = { onCancelQuest(q.id) },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("CANCEL")
                                            }
                                        }
                                    } else if (q.completionEvidence.isNotBlank()) {
                                        Text("Evidence: ${q.completionEvidence}", style = MaterialTheme.typography.labelSmall, color = SystemColors.MutedSilver)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showCreateSheet) {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ModalBottomSheet(
                    onDismissRequest = { showCreateSheet = false },
                    sheetState = sheetState,
                    containerColor = SystemColors.DeepNavy,
                    contentColor = SystemColors.PureIce
                ) {
                    var title by rememberSaveable { mutableStateOf("") }
                    var objective by rememberSaveable { mutableStateOf("") }
                    var category by rememberSaveable { mutableStateOf(AcademicQuestCategory.DAILY_STUDY) }
                    var difficulty by rememberSaveable { mutableStateOf(AcademicQuestDifficulty.E_RANK) }
                    var subjectId by rememberSaveable { mutableStateOf<Long?>(uiState.subjects.firstOrNull { !it.isArchived }?.id) }
                    var targetText by rememberSaveable { mutableStateOf("45") }
                    var unit by rememberSaveable { mutableStateOf("min") }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("CREATE STUDY QUEST", style = MaterialTheme.typography.titleLarge, color = SystemColors.ElectricCyan)
                        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Quest Title") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = academicFieldColors())
                        OutlinedTextField(value = objective, onValueChange = { objective = it }, label = { Text("Objective") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = academicFieldColors())
                        Text("CATEGORY", style = MaterialTheme.typography.labelMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            AcademicQuestCategory.entries.forEach { c ->
                                FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c.label) })
                            }
                        }
                        Text("RANK DIFFICULTY", style = MaterialTheme.typography.labelMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            AcademicQuestDifficulty.entries.forEach { d ->
                                FilterChip(selected = difficulty == d, onClick = { difficulty = d }, label = { Text(d.label) })
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = targetText,
                                onValueChange = { targetText = it.filter { ch -> ch.isDigit() }.take(4) },
                                label = { Text("Target") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                colors = academicFieldColors()
                            )
                            OutlinedTextField(
                                value = unit,
                                onValueChange = { unit = it },
                                label = { Text("Unit") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = academicFieldColors()
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(onClick = { showCreateSheet = false }, modifier = Modifier.weight(1f)) { Text("CANCEL") }
                            Button(
                                onClick = {
                                    onCreateQuest(title, objective.ifBlank { title }, category, subjectId, difficulty, objective, targetText.toIntOrNull() ?: 30, unit)
                                    showCreateSheet = false
                                },
                                enabled = title.isNotBlank(),
                                modifier = Modifier.weight(1f)
                            ) { Text("CREATE QUEST") }
                        }
                    }
                }
            }
        }
    }
}
