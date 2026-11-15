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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Unarchive
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import com.example.domain.model.AcademicPriority
import com.example.domain.model.AcademicSubject
import com.example.domain.model.SubjectColorToken
import com.example.domain.model.SubjectIconKey
import com.example.ui.components.GlowingProgressBar
import com.example.ui.components.HolographicPanel
import com.example.ui.components.SystemEmptyState
import com.example.ui.components.SystemErrorState
import com.example.ui.components.SystemLoadingView
import com.example.ui.components.SystemSectionTitle
import com.example.ui.theme.SystemColors
import com.example.ui.viewmodel.AcademicUiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AcademicSubjectsScreen(
    uiState: AcademicUiState,
    onRetry: () -> Unit,
    onCreateSubject: (String, String, String, SubjectColorToken, SubjectIconKey, AcademicPriority, String) -> Unit,
    onUpdateSubject: (AcademicSubject) -> Unit,
    onSetArchived: (Long, Boolean) -> Unit,
    onOpenSubjectDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is AcademicUiState.Loading -> SystemLoadingView("LOADING SUBJECT MATRIX...", modifier)
        is AcademicUiState.Error -> Box(modifier.fillMaxSize().background(SystemColors.ObsidianBlack).padding(20.dp)) {
            SystemErrorState(uiState.message, onRetry)
        }
        is AcademicUiState.Ready -> {
            var showArchived by rememberSaveable { mutableStateOf(false) }
            var showCreateSheet by rememberSaveable { mutableStateOf(false) }
            var editingSubjectId by rememberSaveable { mutableStateOf<Long?>(null) }

            val visibleSummaries = uiState.subjectSummaries.filter { it.subject.isArchived == showArchived }
            val editingSubject = editingSubjectId?.let { id -> uiState.subjects.find { it.id == id } }

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
                        .testTag("academic_subjects_screen"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SystemSectionTitle(
                            title = "Subject Management",
                            subtitle = "Flexible syllabus builder • 0 XP awarded for setup",
                            trailing = {
                                Button(
                                    onClick = { showCreateSheet = true },
                                    modifier = Modifier.testTag("create_subject_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SystemColors.ElectricCyan,
                                        contentColor = SystemColors.ObsidianBlack
                                    )
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("NEW SUBJECT", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        )
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = !showArchived,
                                onClick = { showArchived = false },
                                label = { Text("Active (${uiState.subjectSummaries.count { !it.subject.isArchived }})") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SystemColors.ElectricCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = SystemColors.ElectricCyan
                                )
                            )
                            FilterChip(
                                selected = showArchived,
                                onClick = { showArchived = true },
                                label = { Text("Archived (${uiState.subjectSummaries.count { it.subject.isArchived }})") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SystemColors.ArcaneViolet.copy(alpha = 0.2f),
                                    selectedLabelColor = SystemColors.ArcaneViolet
                                )
                            )
                        }
                    }

                    if (visibleSummaries.isEmpty()) {
                        item {
                            SystemEmptyState(
                                title = if (showArchived) "No Archived Subjects" else "No Active Subjects",
                                description = if (showArchived) {
                                    "Archived subjects will appear here and retain all logged history."
                                } else {
                                    "Create a subject tailored to any school, university, exam, or self-study track."
                                }
                            )
                        }
                    } else {
                        items(visibleSummaries, key = { it.subject.id }) { summary ->
                            val subj = summary.subject
                            val accent = subj.colorToken.toHudColor()
                            HolographicPanel(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenSubjectDetail(subj.id) }
                                    .testTag("subject_card_${subj.id}"),
                                accentColor = accent
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = accent.copy(alpha = 0.14f),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
                                        ) {
                                            Icon(
                                                imageVector = subj.iconKey.toVector(),
                                                contentDescription = subj.name,
                                                tint = accent,
                                                modifier = Modifier
                                                    .padding(8.dp)
                                                    .size(22.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (subj.code.isNotBlank()) "${subj.code} • ${subj.name}" else subj.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = SystemColors.PureIce
                                            )
                                            Text(
                                                text = "${subj.programSystem} • ${subj.priority.label} Priority",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = SystemColors.MutedSilver
                                            )
                                        }
                                        IconButton(onClick = { editingSubjectId = subj.id }) {
                                            Icon(Icons.Rounded.Edit, contentDescription = "Edit subject", tint = SystemColors.ElectricCyan)
                                        }
                                        IconButton(onClick = { onSetArchived(subj.id, !subj.isArchived) }) {
                                            Icon(
                                                imageVector = if (subj.isArchived) Icons.Rounded.Unarchive else Icons.Rounded.Archive,
                                                contentDescription = if (subj.isArchived) "Restore subject" else "Archive subject",
                                                tint = SystemColors.MetallicSilver
                                            )
                                        }
                                    }

                                    GlowingProgressBar(
                                        progress = summary.syllabusCompletionPercent / 100f,
                                        height = 6.dp,
                                        startColor = accent
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${summary.moduleCount} mod • ${summary.completedChapters}/${summary.chapterCount} ch • ${summary.completedTopics}/${summary.topicCount} topics",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SystemColors.MetallicSilver
                                        )
                                        Text(
                                            text = "${summary.totalStudyMinutes}m logged",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = accent
                                        )
                                    }
                                    if (subj.notes.isNotBlank()) {
                                        Text(
                                            text = subj.notes,
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

            if (showCreateSheet || editingSubject != null) {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ModalBottomSheet(
                    onDismissRequest = {
                        showCreateSheet = false
                        editingSubjectId = null
                    },
                    sheetState = sheetState,
                    containerColor = SystemColors.DeepNavy,
                    contentColor = SystemColors.PureIce
                ) {
                    SubjectEditorSheet(
                        initial = editingSubject,
                        onDismiss = {
                            showCreateSheet = false
                            editingSubjectId = null
                        },
                        onSave = { name, code, program, color, icon, priority, notes ->
                            if (editingSubject != null) {
                                onUpdateSubject(
                                    editingSubject.copy(
                                        name = name,
                                        code = code,
                                        programSystem = program.ifBlank { "Flexible Programme" },
                                        colorToken = color,
                                        iconKey = icon,
                                        priority = priority,
                                        notes = notes
                                    )
                                )
                            } else {
                                onCreateSubject(name, code, program, color, icon, priority, notes)
                            }
                            showCreateSheet = false
                            editingSubjectId = null
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubjectEditorSheet(
    initial: AcademicSubject?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, SubjectColorToken, SubjectIconKey, AcademicPriority, String) -> Unit
) {
    var name by rememberSaveable(initial?.id) { mutableStateOf(initial?.name.orEmpty()) }
    var code by rememberSaveable(initial?.id) { mutableStateOf(initial?.code.orEmpty()) }
    var program by rememberSaveable(initial?.id) { mutableStateOf(initial?.programSystem ?: "Flexible Programme") }
    var selectedColor by rememberSaveable(initial?.id) { mutableStateOf(initial?.colorToken ?: SubjectColorToken.CYAN) }
    var selectedIcon by rememberSaveable(initial?.id) { mutableStateOf(initial?.iconKey ?: SubjectIconKey.BOOK) }
    var priority by rememberSaveable(initial?.id) { mutableStateOf(initial?.priority ?: AcademicPriority.MEDIUM) }
    var notes by rememberSaveable(initial?.id) { mutableStateOf(initial?.notes.orEmpty()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = if (initial == null) "CREATE SUBJECT" else "EDIT SUBJECT",
            style = MaterialTheme.typography.titleLarge,
            color = SystemColors.ElectricCyan
        )
        Text(
            text = "Flexible across any school, university, certification, or self-study track. Creating subjects awards 0 XP.",
            style = MaterialTheme.typography.bodySmall,
            color = SystemColors.MutedSilver
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Subject Title (e.g., Organic Chemistry, Constitutional Law)") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("subject_name_input"),
            colors = academicFieldColors()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text("Code (Optional)") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = academicFieldColors()
            )
            OutlinedTextField(
                value = program,
                onValueChange = { program = it },
                label = { Text("Programme / Exam System") },
                singleLine = true,
                modifier = Modifier.weight(1.4f),
                colors = academicFieldColors()
            )
        }

        Text("PRIORITY", style = MaterialTheme.typography.labelMedium, color = SystemColors.MetallicSilver)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            AcademicPriority.entries.forEach { p ->
                FilterChip(
                    selected = priority == p,
                    onClick = { priority = p },
                    label = { Text(p.label) }
                )
            }
        }

        Text("ACCENT TOKEN", style = MaterialTheme.typography.labelMedium, color = SystemColors.MetallicSilver)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SubjectColorToken.entries.forEach { token ->
                val accent = token.toHudColor()
                FilterChip(
                    selected = selectedColor == token,
                    onClick = { selectedColor = token },
                    label = { Text(token.label, color = accent) }
                )
            }
        }

        Text("DISCIPLINE ICON", style = MaterialTheme.typography.labelMedium, color = SystemColors.MetallicSilver)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SubjectIconKey.entries.forEach { key ->
                FilterChip(
                    selected = selectedIcon == key,
                    onClick = { selectedIcon = key },
                    leadingIcon = { Icon(key.toVector(), contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text(key.label) }
                )
            }
        }

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Syllabus / Personal Notes") },
            modifier = Modifier.fillMaxWidth(),
            colors = academicFieldColors()
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                Text("CANCEL")
            }
            Button(
                onClick = { onSave(name, code, program, selectedColor, selectedIcon, priority, notes) },
                enabled = name.isNotBlank(),
                modifier = Modifier
                    .weight(1f)
                    .testTag("save_subject_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SystemColors.ElectricCyan,
                    contentColor = SystemColors.ObsidianBlack
                )
            ) {
                Text("SAVE SUBJECT")
            }
        }
    }
}
