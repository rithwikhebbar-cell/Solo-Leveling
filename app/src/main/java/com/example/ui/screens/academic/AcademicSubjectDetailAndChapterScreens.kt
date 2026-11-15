package com.example.ui.screens.academic

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.model.ChapterDifficulty
import com.example.domain.model.RevisionUrgency
import com.example.ui.components.GlowingProgressBar
import com.example.ui.components.HolographicPanel
import com.example.ui.components.SystemEmptyState
import com.example.ui.components.SystemErrorState
import com.example.ui.components.SystemLoadingView
import com.example.ui.components.SystemSectionTitle
import com.example.ui.theme.SystemColors
import com.example.ui.viewmodel.AcademicUiState

/**
 * Screen 3: Subject Details (modules, chapters, focus quick-launch, quests, linked exams).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AcademicSubjectDetailScreen(
    subjectId: Long,
    uiState: AcademicUiState,
    onRetry: () -> Unit,
    onAddModule: (Long, String, String) -> Unit,
    onAddChapter: (Long, Long, String, ChapterDifficulty, Int, String) -> Unit,
    onToggleChapterCompleted: (Long, Boolean) -> Unit,
    onOpenChapterTopics: (Long, Long, Long) -> Unit,
    onStartFocusForSubject: (Long) -> Unit,
    onGenerateQuestsForSubject: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is AcademicUiState.Loading -> SystemLoadingView("LOADING SUBJECT DETAILS...", modifier)
        is AcademicUiState.Error -> Box(modifier.fillMaxSize().background(SystemColors.ObsidianBlack).padding(20.dp)) {
            SystemErrorState(uiState.message, onRetry)
        }
        is AcademicUiState.Ready -> {
            val summary = uiState.subjectSummaries.find { it.subject.id == subjectId }
            if (summary == null) {
                Box(modifier.fillMaxSize().background(SystemColors.ObsidianBlack).padding(20.dp)) {
                    SystemEmptyState("Subject Not Found", "This subject may have been removed.")
                }
                return
            }

            val subject = summary.subject
            val accent = subject.colorToken.toHudColor()
            val modules = uiState.modules.filter { it.subjectId == subjectId }
            val chapters = uiState.chapters.filter { it.subjectId == subjectId }
            val linkedExams = uiState.examReadiness.filter { it.exam.subjectId == subjectId }

            var showAddModuleSheet by rememberSaveable { mutableStateOf(false) }
            var addChapterModuleId by rememberSaveable { mutableStateOf<Long?>(null) }

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
                        .testTag("academic_subject_detail_screen"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = accent) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(subject.iconKey.toVector(), contentDescription = subject.name, tint = accent, modifier = Modifier.size(26.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(subject.name, style = MaterialTheme.typography.headlineMedium, color = SystemColors.PureIce)
                                        Text("${subject.programSystem} • ${subject.priority.label} Priority", style = MaterialTheme.typography.bodySmall, color = SystemColors.MutedSilver)
                                    }
                                    Text("${summary.syllabusCompletionPercent}%", style = MaterialTheme.typography.titleLarge, color = accent)
                                }
                                GlowingProgressBar(progress = summary.syllabusCompletionPercent / 100f, height = 8.dp, startColor = accent)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AcademicMetricTile("CHAPTERS", "${summary.completedChapters}/${summary.chapterCount}", Modifier.weight(1f), accent)
                                    AcademicMetricTile("TOPICS", "${summary.completedTopics}/${summary.topicCount}", Modifier.weight(1f))
                                    AcademicMetricTile("LOGGED", "${summary.totalStudyMinutes}m", Modifier.weight(1f))
                                    AcademicMetricTile("EST. LEFT", "${summary.estimatedRemainingMinutes}m", Modifier.weight(1f))
                                }
                                if (subject.notes.isNotBlank()) {
                                    Text(subject.notes, style = MaterialTheme.typography.bodySmall, color = SystemColors.MetallicSilver)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { onStartFocusForSubject(subjectId) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = SystemColors.ObsidianBlack)
                                    ) {
                                        Icon(Icons.Rounded.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("FOCUS", style = MaterialTheme.typography.labelMedium)
                                    }
                                    OutlinedButton(
                                        onClick = { onGenerateQuestsForSubject(subjectId) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("GEN QUESTS", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }

                    if (linkedExams.isNotEmpty()) {
                        item {
                            SystemSectionTitle(title = "Linked Examinations", subtitle = "${linkedExams.size} scheduled")
                        }
                        items(linkedExams, key = { it.exam.id }) { item ->
                            HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.WarningAmber, contentPadding = 12.dp) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.exam.title, style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
                                        Text("${item.statusLabel} • Target ${item.exam.targetScoreDisplay} • Readiness ${item.readinessPercent}%", style = MaterialTheme.typography.bodySmall, color = SystemColors.MutedSilver)
                                    }
                                    Text(if (item.daysRemaining >= 0) "${item.daysRemaining}d" else "Past", style = MaterialTheme.typography.labelLarge, color = SystemColors.WarningAmber)
                                }
                            }
                        }
                    }

                    item {
                        SystemSectionTitle(
                            title = "Modules & Chapters",
                            subtitle = "Completion XP guarded once per chapter",
                            trailing = {
                                TextButton(
                                    onClick = { showAddModuleSheet = true },
                                    modifier = Modifier.testTag("add_module_button")
                                ) {
                                    Text("+ MODULE", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        )
                    }

                    items(modules, key = { it.id }) { mod ->
                        val modChapters = chapters.filter { it.moduleId == mod.id }
                        HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.SystemBlue) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(mod.title, style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
                                        if (mod.description.isNotBlank()) {
                                            Text(mod.description, style = MaterialTheme.typography.bodySmall, color = SystemColors.MutedSilver)
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = { addChapterModuleId = mod.id },
                                        modifier = Modifier.testTag("add_chapter_mod_${mod.id}"),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("CHAPTER", style = MaterialTheme.typography.labelSmall)
                                    }
                                }

                                if (modChapters.isEmpty()) {
                                    Text("No chapters yet. Tap + CHAPTER to add units.", style = MaterialTheme.typography.bodySmall, color = SystemColors.MutedSilver)
                                } else {
                                    modChapters.forEach { chap ->
                                        val topicCount = uiState.topics.count { it.chapterId == chap.id }
                                        val doneTopics = uiState.topics.count { it.chapterId == chap.id && it.isCompleted }
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { onOpenChapterTopics(subjectId, mod.id, chap.id) }
                                                .testTag("chapter_row_${chap.id}"),
                                            color = SystemColors.ObsidianBlack.copy(alpha = 0.6f),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(0.75.dp, if (chap.isCompleted) SystemColors.VerifiedEmerald.copy(alpha = 0.5f) else SystemColors.SubtleGridLine)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = chap.isCompleted,
                                                    onCheckedChange = { onToggleChapterCompleted(chap.id, it) }
                                                )
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(chap.title, style = MaterialTheme.typography.titleSmall, color = SystemColors.PureIce)
                                                    Text(
                                                        "${chap.difficulty.label} • Est ${chap.estimatedMinutes}m • $doneTopics/$topicCount topics",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = SystemColors.MutedSilver
                                                    )
                                                }
                                                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Open topics", tint = SystemColors.ElectricCyan, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showAddModuleSheet) {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ModalBottomSheet(
                    onDismissRequest = { showAddModuleSheet = false },
                    sheetState = sheetState,
                    containerColor = SystemColors.DeepNavy,
                    contentColor = SystemColors.PureIce
                ) {
                    var modTitle by rememberSaveable { mutableStateOf("") }
                    var modDesc by rememberSaveable { mutableStateOf("") }
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("ADD MODULE", style = MaterialTheme.typography.titleLarge, color = SystemColors.ElectricCyan)
                        OutlinedTextField(
                            value = modTitle,
                            onValueChange = { modTitle = it },
                            label = { Text("Module Title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = academicFieldColors()
                        )
                        OutlinedTextField(
                            value = modDesc,
                            onValueChange = { modDesc = it },
                            label = { Text("Description (optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = academicFieldColors()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(onClick = { showAddModuleSheet = false }, modifier = Modifier.weight(1f)) { Text("CANCEL") }
                            Button(
                                onClick = {
                                    onAddModule(subjectId, modTitle, modDesc)
                                    showAddModuleSheet = false
                                },
                                enabled = modTitle.isNotBlank(),
                                modifier = Modifier.weight(1f)
                            ) { Text("ADD MODULE") }
                        }
                    }
                }
            }

            addChapterModuleId?.let { modId ->
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ModalBottomSheet(
                    onDismissRequest = { addChapterModuleId = null },
                    sheetState = sheetState,
                    containerColor = SystemColors.DeepNavy,
                    contentColor = SystemColors.PureIce
                ) {
                    var chapTitle by rememberSaveable { mutableStateOf("") }
                    var difficulty by rememberSaveable { mutableStateOf(ChapterDifficulty.INTERMEDIATE) }
                    var estText by rememberSaveable { mutableStateOf("60") }
                    var notes by rememberSaveable { mutableStateOf("") }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("ADD CHAPTER", style = MaterialTheme.typography.titleLarge, color = SystemColors.ElectricCyan)
                        OutlinedTextField(
                            value = chapTitle,
                            onValueChange = { chapTitle = it },
                            label = { Text("Chapter Title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("chapter_title_input"),
                            colors = academicFieldColors()
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            ChapterDifficulty.entries.forEach { d ->
                                FilterChip(selected = difficulty == d, onClick = { difficulty = d }, label = { Text(d.label) })
                            }
                        }
                        OutlinedTextField(
                            value = estText,
                            onValueChange = { estText = it.filter { c -> c.isDigit() }.take(3) },
                            label = { Text("Estimated Study Minutes (10–600)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            colors = academicFieldColors()
                        )
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Chapter Notes") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = academicFieldColors()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(onClick = { addChapterModuleId = null }, modifier = Modifier.weight(1f)) { Text("CANCEL") }
                            Button(
                                onClick = {
                                    onAddChapter(modId, subjectId, chapTitle, difficulty, estText.toIntOrNull() ?: 60, notes)
                                    addChapterModuleId = null
                                },
                                enabled = chapTitle.isNotBlank(),
                                modifier = Modifier.weight(1f).testTag("save_chapter_button")
                            ) { Text("SAVE CHAPTER") }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Screen 4: Chapter & Topic Management (granular topics, confidence rating, spaced-revision launch).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AcademicChapterTopicsScreen(
    subjectId: Long,
    chapterId: Long,
    uiState: AcademicUiState,
    onRetry: () -> Unit,
    onToggleChapterCompleted: (Long, Boolean) -> Unit,
    onAddTopic: (Long, Long, String, Int, String) -> Unit,
    onToggleTopicCompleted: (Long, Boolean) -> Unit,
    onReviseTopic: (Long, Int, Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is AcademicUiState.Loading -> SystemLoadingView("LOADING CHAPTER TOPICS...", modifier)
        is AcademicUiState.Error -> Box(modifier.fillMaxSize().background(SystemColors.ObsidianBlack).padding(20.dp)) {
            SystemErrorState(uiState.message, onRetry)
        }
        is AcademicUiState.Ready -> {
            val chapter = uiState.chapters.find { it.id == chapterId }
            val subject = uiState.subjects.find { it.id == subjectId }
            if (chapter == null || subject == null) {
                Box(modifier.fillMaxSize().background(SystemColors.ObsidianBlack).padding(20.dp)) {
                    SystemEmptyState("Chapter Not Found", "Unable to locate this syllabus chapter.")
                }
                return
            }

            val topics = uiState.topics.filter { it.chapterId == chapterId }
            var showAddTopicSheet by rememberSaveable { mutableStateOf(false) }
            var reviseTopicId by rememberSaveable { mutableStateOf<Long?>(null) }

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
                        .testTag("academic_chapter_topics_screen"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = subject.colorToken.toHudColor()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(subject.name.uppercase(), style = MaterialTheme.typography.labelSmall, color = subject.colorToken.toHudColor())
                                Text(chapter.title, style = MaterialTheme.typography.headlineMedium, color = SystemColors.PureIce)
                                Text(
                                    "${chapter.difficulty.label} • Est. ${chapter.estimatedMinutes} min • Single completion XP reward guard active",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SystemColors.MutedSilver
                                )
                                if (chapter.notes.isNotBlank()) {
                                    Text(chapter.notes, style = MaterialTheme.typography.bodySmall, color = SystemColors.MetallicSilver)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(
                                        onClick = { onToggleChapterCompleted(chapter.id, !chapter.isCompleted) },
                                        modifier = Modifier.weight(1f).testTag("toggle_chapter_complete_button"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (chapter.isCompleted) SystemColors.VerifiedEmerald else SystemColors.ElectricCyan,
                                            contentColor = SystemColors.ObsidianBlack
                                        )
                                    ) {
                                        Text(if (chapter.isCompleted) "CHAPTER CLEARED" else "MARK CHAPTER CLEAR")
                                    }
                                    OutlinedButton(
                                        onClick = { showAddTopicSheet = true },
                                        modifier = Modifier.weight(1f).testTag("add_topic_button")
                                    ) {
                                        Text("+ TOPIC")
                                    }
                                }
                            }
                        }
                    }

                    item {
                        SystemSectionTitle(
                            title = "Topics (${topics.size})",
                            subtitle = "Topics enroll automatically in Revision Intelligence Queue"
                        )
                    }

                    if (topics.isEmpty()) {
                        item {
                            SystemEmptyState("No Topics Added", "Break this chapter down into atomic topics for confidence & spaced revision tracking.")
                        }
                    } else {
                        items(topics, key = { it.id }) { topic ->
                            val revItem = uiState.revisionQueue.find { it.topic.id == topic.id }
                            val urgColor = when (revItem?.urgency) {
                                RevisionUrgency.OVERDUE -> SystemColors.DangerCrimson
                                RevisionUrgency.DUE_TODAY -> SystemColors.WarningAmber
                                else -> SystemColors.ElectricCyan
                            }
                            HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = urgColor, contentPadding = 12.dp) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (topic.isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                        contentDescription = if (topic.isCompleted) "Topic completed" else "Topic pending",
                                        tint = if (topic.isCompleted) SystemColors.VerifiedEmerald else urgColor,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clickable { onToggleTopicCompleted(topic.id, !topic.isCompleted) }
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(topic.title, style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
                                        Text(
                                            "Confidence ${topic.confidenceEstimate}/5 • Rev #${topic.revisionCount} • Est. familiarity ${revItem?.retentionEstimatePercent ?: 60}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SystemColors.MutedSilver
                                        )
                                        if (topic.notes.isNotBlank()) {
                                            Text(topic.notes, style = MaterialTheme.typography.bodySmall, color = SystemColors.MetallicSilver)
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = { reviseTopicId = topic.id },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Rounded.Psychology, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("REVISE", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showAddTopicSheet) {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ModalBottomSheet(
                    onDismissRequest = { showAddTopicSheet = false },
                    sheetState = sheetState,
                    containerColor = SystemColors.DeepNavy,
                    contentColor = SystemColors.PureIce
                ) {
                    var title by rememberSaveable { mutableStateOf("") }
                    var confidence by rememberSaveable { mutableIntStateOf(3) }
                    var notes by rememberSaveable { mutableStateOf("") }
                    Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("ADD TOPIC", style = MaterialTheme.typography.titleLarge, color = SystemColors.ElectricCyan)
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Topic Title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("topic_title_input"),
                            colors = academicFieldColors()
                        )
                        Text("Initial Self-Assessed Confidence (1–5)", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            (1..5).forEach { level ->
                                FilterChip(selected = confidence == level, onClick = { confidence = level }, label = { Text("$level") })
                            }
                        }
                        OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), colors = academicFieldColors())
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(onClick = { showAddTopicSheet = false }, modifier = Modifier.weight(1f)) { Text("CANCEL") }
                            Button(
                                onClick = {
                                    onAddTopic(chapterId, subjectId, title, confidence, notes)
                                    showAddTopicSheet = false
                                },
                                enabled = title.isNotBlank(),
                                modifier = Modifier.weight(1f).testTag("save_topic_button")
                            ) { Text("SAVE TOPIC") }
                        }
                    }
                }
            }

            reviseTopicId?.let { tId ->
                val topic = topics.find { it.id == tId }
                if (topic != null) {
                    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                    ModalBottomSheet(
                        onDismissRequest = { reviseTopicId = null },
                        sheetState = sheetState,
                        containerColor = SystemColors.DeepNavy,
                        contentColor = SystemColors.PureIce
                    ) {
                        RevisionLogSheet(
                            topicTitle = topic.title,
                            initialConfidence = topic.confidenceEstimate,
                            onDismiss = { reviseTopicId = null },
                            onSubmit = { conf, mins, notes ->
                                onReviseTopic(topic.id, conf, mins, notes)
                                reviseTopicId = null
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RevisionLogSheet(
    topicTitle: String,
    initialConfidence: Int,
    onDismiss: () -> Unit,
    onSubmit: (Int, Int, String) -> Unit
) {
    var confidence by rememberSaveable(topicTitle) { mutableIntStateOf(initialConfidence.coerceIn(1, 5)) }
    var durationText by rememberSaveable(topicTitle) { mutableStateOf("20") }
    var notes by rememberSaveable(topicTitle) { mutableStateOf("") }
    val mins = durationText.toIntOrNull() ?: 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("RECORD SPACED REVISION", style = MaterialTheme.typography.titleLarge, color = SystemColors.ElectricCyan)
        Text(topicTitle, style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
        Text(
            "Self-assessed confidence schedules your next deterministic interval (SELF_REPORTED Wisdom XP).",
            style = MaterialTheme.typography.bodySmall,
            color = SystemColors.MutedSilver
        )
        Text("Confidence After Recall (1 = Fragile, 5 = Solid)", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..5).forEach { lvl ->
                FilterChip(selected = confidence == lvl, onClick = { confidence = lvl }, label = { Text("$lvl") })
            }
        }
        OutlinedTextField(
            value = durationText,
            onValueChange = { durationText = it.filter { c -> c.isDigit() }.take(3) },
            label = { Text("Revision Duration (minutes)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            colors = academicFieldColors()
        )
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Recall Notes (optional)") },
            modifier = Modifier.fillMaxWidth(),
            colors = academicFieldColors()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("CANCEL") }
            Button(
                onClick = { onSubmit(confidence, mins, notes) },
                enabled = mins in 1..240,
                modifier = Modifier.weight(1f).testTag("submit_revision_button")
            ) { Text("SAVE REVISION") }
        }
    }
}
