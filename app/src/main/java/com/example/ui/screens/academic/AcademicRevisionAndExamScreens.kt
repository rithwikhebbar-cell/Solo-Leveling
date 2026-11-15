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
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.Grade
import androidx.compose.material.icons.rounded.Psychology
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.example.domain.model.AssessmentType
import com.example.domain.model.MarkingSystem
import com.example.domain.model.RevisionUrgency
import com.example.ui.components.GlowingProgressBar
import com.example.ui.components.HolographicPanel
import com.example.ui.components.ProvenanceChip
import com.example.ui.components.SystemEmptyState
import com.example.ui.components.SystemErrorState
import com.example.ui.components.SystemLoadingView
import com.example.ui.components.SystemSectionTitle
import com.example.ui.theme.SystemColors
import com.example.ui.viewmodel.AcademicUiState

/**
 * Screen 7: Revision Intelligence Centre (spaced queue, overdue/today/upcoming, heuristic retention estimate notice).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AcademicRevisionsScreen(
    uiState: AcademicUiState,
    onRetry: () -> Unit,
    onCompleteRevision: (Long, Int, Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is AcademicUiState.Loading -> SystemLoadingView("LOADING REVISION QUEUE...", modifier)
        is AcademicUiState.Error -> Box(modifier.fillMaxSize().background(SystemColors.ObsidianBlack).padding(20.dp)) {
            SystemErrorState(uiState.message, onRetry)
        }
        is AcademicUiState.Ready -> {
            var urgencyFilter by rememberSaveable { mutableStateOf<RevisionUrgency?>(null) }
            var selectedTopicId by rememberSaveable { mutableStateOf<Long?>(null) }

            val visibleQueue = uiState.revisionQueue.filter { urgencyFilter == null || it.urgency == urgencyFilter }

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
                        .testTag("academic_revisions_screen"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SystemSectionTitle(
                            title = "Revision Intelligence",
                            subtitle = "Deterministic offline spaced-recall scheduling"
                        )
                    }

                    item {
                        HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.WarningAmber, contentPadding = 12.dp) {
                            Text(
                                text = "Retention figures below are heuristic familiarity estimates derived from confidence, difficulty, intervals, and assessment history — not scientifically measured memory.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SystemColors.MetallicSilver
                            )
                        }
                    }

                    item {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = urgencyFilter == null,
                                onClick = { urgencyFilter = null },
                                label = { Text("All (${uiState.revisionQueue.size})") }
                            )
                            RevisionUrgency.entries.forEach { urg ->
                                FilterChip(
                                    selected = urgencyFilter == urg,
                                    onClick = { urgencyFilter = if (urgencyFilter == urg) null else urg },
                                    label = { Text("${urg.label} (${uiState.revisionQueue.count { it.urgency == urg }})") }
                                )
                            }
                        }
                    }

                    if (visibleQueue.isEmpty()) {
                        item {
                            SystemEmptyState(
                                title = "No Topics In Queue",
                                description = "Add topics inside Subject → Module → Chapter to populate your spaced revision queue."
                            )
                        }
                    } else {
                        items(visibleQueue, key = { it.topic.id }) { item ->
                            val urgAccent = when (item.urgency) {
                                RevisionUrgency.OVERDUE -> SystemColors.DangerCrimson
                                RevisionUrgency.DUE_TODAY -> SystemColors.WarningAmber
                                RevisionUrgency.UPCOMING -> SystemColors.ElectricCyan
                            }
                            HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = urgAccent) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("${item.subjectName.uppercase()} • ${item.urgency.label.uppercase()}", style = MaterialTheme.typography.labelSmall, color = urgAccent)
                                        Text("Est. Familiarity ${item.retentionEstimatePercent}%", style = MaterialTheme.typography.labelMedium, color = SystemColors.PureIce)
                                    }
                                    Text(item.topic.title, style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
                                    Text(
                                        "Difficulty: ${item.chapterDifficulty.label} • Confidence: ${item.topic.confidenceEstimate}/5 • Revisions: ${item.topic.revisionCount}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SystemColors.MutedSilver
                                    )
                                    GlowingProgressBar(
                                        progress = item.retentionEstimatePercent / 100f,
                                        height = 5.dp,
                                        startColor = urgAccent
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = item.topic.nextRevisionDueEpochMs?.let { "Due: ${formatAcademicDate(it)}" } ?: "Due immediately",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SystemColors.MutedSilver
                                        )
                                        Button(
                                            onClick = { selectedTopicId = item.topic.id },
                                            modifier = Modifier.testTag("revise_topic_${item.topic.id}"),
                                            colors = ButtonDefaults.buttonColors(containerColor = urgAccent, contentColor = SystemColors.ObsidianBlack)
                                        ) {
                                            Icon(Icons.Rounded.Psychology, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("REVISE TOPIC", style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (uiState.revisionRecords.isNotEmpty()) {
                        item {
                            SystemSectionTitle(
                                title = "Revision Log",
                                subtitle = "${uiState.revisionRecords.size} completed intervals"
                            )
                        }
                        items(uiState.revisionRecords.take(10), key = { it.id }) { rec ->
                            HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.ArcaneViolet, contentPadding = 12.dp) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${rec.subjectName}: ${rec.topicTitle}", style = MaterialTheme.typography.titleSmall, color = SystemColors.PureIce)
                                        Text("Confidence ${rec.confidenceAfter}/5 • Next in +${rec.intervalDaysScheduled}d • +${rec.xpAwarded} XP", style = MaterialTheme.typography.labelSmall, color = SystemColors.MutedSilver)
                                    }
                                    ProvenanceChip(status = rec.verificationStatus)
                                }
                            }
                        }
                    }
                }
            }

            selectedTopicId?.let { tId ->
                val topic = uiState.topics.find { it.id == tId }
                if (topic != null) {
                    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                    ModalBottomSheet(
                        onDismissRequest = { selectedTopicId = null },
                        sheetState = sheetState,
                        containerColor = SystemColors.DeepNavy,
                        contentColor = SystemColors.PureIce
                    ) {
                        RevisionLogSheet(
                            topicTitle = topic.title,
                            initialConfidence = topic.confidenceEstimate,
                            onDismiss = { selectedTopicId = null },
                            onSubmit = { conf, mins, notes ->
                                onCompleteRevision(topic.id, conf, mins, notes)
                                selectedTopicId = null
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Screen 8: Examination & Assessment Centre (flexible marking systems, countdown, readiness, prep quests, graded evidence).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AcademicExamsScreen(
    uiState: AcademicUiState,
    onRetry: () -> Unit,
    onCreateExam: (Long, String, Int, MarkingSystem, String, Float, Int, String) -> Unit,
    onGeneratePrepQuests: (Long) -> Unit,
    onRecordAssessment: (Long, Long?, Long?, String, AssessmentType, MarkingSystem, Double, Double, String, Boolean, String) -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is AcademicUiState.Loading -> SystemLoadingView("LOADING EXAM CENTRE...", modifier)
        is AcademicUiState.Error -> Box(modifier.fillMaxSize().background(SystemColors.ObsidianBlack).padding(20.dp)) {
            SystemErrorState(uiState.message, onRetry)
        }
        is AcademicUiState.Ready -> {
            val activeSubjects = uiState.subjects.filter { !it.isArchived }
            var showCreateExamSheet by rememberSaveable { mutableStateOf(false) }
            var showAssessmentSheet by rememberSaveable { mutableStateOf(false) }
            var prelinkedExamId by rememberSaveable { mutableStateOf<Long?>(null) }

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
                        .testTag("academic_exams_screen"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SystemSectionTitle(
                            title = "Examination & Assessment",
                            subtitle = "Flexible grading systems • Readiness & evidence tracking",
                            trailing = {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            prelinkedExamId = null
                                            showAssessmentSheet = true
                                        },
                                        enabled = activeSubjects.isNotEmpty(),
                                        modifier = Modifier.testTag("record_assessment_button"),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Rounded.Grade, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("LOG SCORE", style = MaterialTheme.typography.labelSmall)
                                    }
                                    Button(
                                        onClick = { showCreateExamSheet = true },
                                        enabled = activeSubjects.isNotEmpty(),
                                        modifier = Modifier.testTag("create_exam_button"),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("EXAM", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        )
                    }

                    if (uiState.examReadiness.isEmpty()) {
                        item {
                            SystemEmptyState(
                                title = "No Examinations Scheduled",
                                description = "Add an upcoming test, mock exam, certification, or module assessment to track countdowns and readiness."
                            )
                        }
                    } else {
                        items(uiState.examReadiness, key = { it.exam.id }) { er ->
                            val exam = er.exam
                            val accent = when {
                                exam.isCompleted -> SystemColors.VerifiedEmerald
                                er.daysRemaining <= 7 -> SystemColors.DangerCrimson
                                er.readinessPercent >= 75 -> SystemColors.ElectricCyan
                                else -> SystemColors.WarningAmber
                            }
                            HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = accent) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(exam.subjectName.uppercase(), style = MaterialTheme.typography.labelSmall, color = accent)
                                            Text(exam.title, style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
                                        }
                                        Surface(
                                            color = accent.copy(alpha = 0.16f),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, accent.copy(alpha = 0.5f))
                                        ) {
                                            Text(
                                                text = if (exam.isCompleted) "GRADED" else if (er.daysRemaining >= 0) "${er.daysRemaining}d LEFT" else "PAST DUE",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = accent,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "${er.statusLabel} • Marking: ${exam.markingSystem.label} • Target: ${exam.targetScoreDisplay}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SystemColors.MutedSilver
                                    )
                                    if (exam.actualScoreDisplay != null) {
                                        Text(
                                            text = "Actual Result: ${exam.actualScoreDisplay}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = SystemColors.VerifiedEmerald
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("READINESS ESTIMATE", style = MaterialTheme.typography.labelSmall, color = SystemColors.MetallicSilver)
                                        Text("${er.readinessPercent}%", style = MaterialTheme.typography.labelLarge, color = accent)
                                    }
                                    GlowingProgressBar(progress = er.readinessPercent / 100f, height = 6.dp, startColor = accent)

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = { onGeneratePrepQuests(exam.id) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("PREP QUESTS", style = MaterialTheme.typography.labelSmall)
                                        }
                                        Button(
                                            onClick = {
                                                prelinkedExamId = exam.id
                                                showAssessmentSheet = true
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Rounded.EventNote, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("LOG RESULT", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        SystemSectionTitle(
                            title = "Assessment Evidence Log",
                            subtitle = "${uiState.assessments.size} recorded scores"
                        )
                    }

                    if (uiState.assessments.isEmpty()) {
                        item {
                            SystemEmptyState("No Assessments Logged", "Record practice quizzes, mock exams, or verified official scores.")
                        }
                    } else {
                        items(uiState.assessments.take(20), key = { it.id }) { a ->
                            HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.SystemBlue, contentPadding = 12.dp) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("${a.subjectName}: ${a.title}", style = MaterialTheme.typography.titleSmall, color = SystemColors.PureIce, modifier = Modifier.weight(1f))
                                        ProvenanceChip(status = a.verificationStatus)
                                    }
                                    val pct = (a.normalizedRatio * 100).toInt()
                                    val gradeText = if (a.gradeLabel.isNotBlank()) " (${a.gradeLabel})" else ""
                                    Text(
                                        text = "${a.assessmentType.label} • ${a.scoredMarks}/${a.maxMarks}$gradeText • $pct% • +${a.xpAwarded} XP",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SystemColors.MetallicSilver
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (showCreateExamSheet && activeSubjects.isNotEmpty()) {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ModalBottomSheet(
                    onDismissRequest = { showCreateExamSheet = false },
                    sheetState = sheetState,
                    containerColor = SystemColors.DeepNavy,
                    contentColor = SystemColors.PureIce
                ) {
                    var subjectId by rememberSaveable { mutableStateOf(activeSubjects.first().id) }
                    var title by rememberSaveable { mutableStateOf("") }
                    var daysText by rememberSaveable { mutableStateOf("14") }
                    var marking by rememberSaveable { mutableStateOf(MarkingSystem.PERCENTAGE) }
                    var targetScore by rememberSaveable { mutableStateOf("85%") }
                    var coverageText by rememberSaveable { mutableStateOf("50") }
                    var notes by rememberSaveable { mutableStateOf("") }

                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("SCHEDULE EXAMINATION", style = MaterialTheme.typography.titleLarge, color = SystemColors.ElectricCyan)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            activeSubjects.forEach { s ->
                                FilterChip(selected = subjectId == s.id, onClick = { subjectId = s.id }, label = { Text(s.name) })
                            }
                        }
                        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Exam Title (e.g., Midterm, Paper 2)") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("exam_title_input"), colors = academicFieldColors())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = daysText,
                                onValueChange = { daysText = it.filter { c -> c.isDigit() }.take(3) },
                                label = { Text("Days Until Exam") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                colors = academicFieldColors()
                            )
                            OutlinedTextField(
                                value = coverageText,
                                onValueChange = { coverageText = it.filter { c -> c.isDigit() }.take(3) },
                                label = { Text("Syllabus Covered %") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                colors = academicFieldColors()
                            )
                        }
                        Text("MARKING SYSTEM", style = MaterialTheme.typography.labelMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            MarkingSystem.entries.forEach { ms ->
                                FilterChip(selected = marking == ms, onClick = { marking = ms }, label = { Text(ms.label) })
                            }
                        }
                        OutlinedTextField(
                            value = targetScore,
                            onValueChange = { targetScore = it },
                            label = { Text("Target Grade / Score (${marking.exampleHint})") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = academicFieldColors()
                        )
                        OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Exam Scope Notes") }, modifier = Modifier.fillMaxWidth(), colors = academicFieldColors())
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(onClick = { showCreateExamSheet = false }, modifier = Modifier.weight(1f)) { Text("CANCEL") }
                            Button(
                                onClick = {
                                    onCreateExam(
                                        subjectId,
                                        title,
                                        daysText.toIntOrNull() ?: 14,
                                        marking,
                                        targetScore,
                                        0.85f,
                                        coverageText.toIntOrNull() ?: 0,
                                        notes
                                    )
                                    showCreateExamSheet = false
                                },
                                enabled = title.isNotBlank(),
                                modifier = Modifier.weight(1f).testTag("save_exam_button")
                            ) { Text("SAVE EXAM") }
                        }
                    }
                }
            }

            if (showAssessmentSheet && activeSubjects.isNotEmpty()) {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ModalBottomSheet(
                    onDismissRequest = { showAssessmentSheet = false },
                    sheetState = sheetState,
                    containerColor = SystemColors.DeepNavy,
                    contentColor = SystemColors.PureIce
                ) {
                    val linkedExam = prelinkedExamId?.let { id -> uiState.exams.find { it.id == id } }
                    var subjectId by rememberSaveable(prelinkedExamId) { mutableStateOf(linkedExam?.subjectId ?: activeSubjects.first().id) }
                    var title by rememberSaveable(prelinkedExamId) { mutableStateOf(linkedExam?.title ?: "") }
                    var type by rememberSaveable(prelinkedExamId) {
                        mutableStateOf(if (linkedExam != null) AssessmentType.GRADED_EXAM else AssessmentType.PRACTICE_QUIZ)
                    }
                    var marking by rememberSaveable(prelinkedExamId) { mutableStateOf(linkedExam?.markingSystem ?: MarkingSystem.PERCENTAGE) }
                    var scoredText by rememberSaveable { mutableStateOf("82") }
                    var maxText by rememberSaveable { mutableStateOf("100") }
                    var gradeLabel by rememberSaveable { mutableStateOf("") }
                    var hasEvidence by rememberSaveable(prelinkedExamId) { mutableStateOf(linkedExam != null) }
                    var notes by rememberSaveable { mutableStateOf("") }

                    val scoredVal = scoredText.toDoubleOrNull() ?: -1.0
                    val maxVal = maxText.toDoubleOrNull() ?: 0.0
                    val canSave = title.isNotBlank() && maxVal > 0.0 && scoredVal in 0.0..maxVal

                    Column(
                        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("RECORD ASSESSMENT RESULT", style = MaterialTheme.typography.titleLarge, color = SystemColors.ElectricCyan)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            activeSubjects.forEach { s ->
                                FilterChip(selected = subjectId == s.id, onClick = { subjectId = s.id }, label = { Text(s.name) })
                            }
                        }
                        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Assessment Title") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("assessment_title_input"), colors = academicFieldColors())
                        Text("ASSESSMENT TYPE", style = MaterialTheme.typography.labelMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            AssessmentType.entries.forEach { at ->
                                FilterChip(selected = type == at, onClick = {
                                    type = at
                                    if (!at.supportsVerifiedEvidence) hasEvidence = false
                                }, label = { Text(at.label) })
                            }
                        }
                        Text("MARKING SYSTEM", style = MaterialTheme.typography.labelMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            MarkingSystem.entries.forEach { ms ->
                                FilterChip(selected = marking == ms, onClick = { marking = ms }, label = { Text(ms.label) })
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = scoredText,
                                onValueChange = { scoredText = it },
                                label = { Text("Scored Marks") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                colors = academicFieldColors()
                            )
                            OutlinedTextField(
                                value = maxText,
                                onValueChange = { maxText = it },
                                label = { Text("Max Marks") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                colors = academicFieldColors()
                            )
                        }
                        OutlinedTextField(value = gradeLabel, onValueChange = { gradeLabel = it }, label = { Text("Optional Grade / Band Label (e.g., A-, Pass)") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = academicFieldColors())

                        if (type.supportsVerifiedEvidence) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Backed by Official Graded Evidence", style = MaterialTheme.typography.bodySmall, color = SystemColors.PureIce, modifier = Modifier.weight(1f))
                                Switch(checked = hasEvidence, onCheckedChange = { hasEvidence = it })
                            }
                        }
                        OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Reflection / Error Analysis Notes") }, modifier = Modifier.fillMaxWidth(), colors = academicFieldColors())

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(onClick = { showAssessmentSheet = false }, modifier = Modifier.weight(1f)) { Text("CANCEL") }
                            Button(
                                onClick = {
                                    onRecordAssessment(subjectId, linkedExam?.id, null, title, type, marking, scoredVal, maxVal, gradeLabel, hasEvidence, notes)
                                    showAssessmentSheet = false
                                },
                                enabled = canSave,
                                modifier = Modifier.weight(1f).testTag("save_assessment_button")
                            ) { Text("SAVE RESULT") }
                        }
                    }
                }
            }
        }
    }
}
