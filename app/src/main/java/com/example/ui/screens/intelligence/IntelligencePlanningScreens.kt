package com.example.ui.screens.intelligence

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.DataProvenanceTag
import com.example.domain.model.NotificationIntensity
import com.example.domain.model.PlanningStyle
import com.example.domain.model.PlayerIntelligencePreferences
import com.example.domain.model.ReportPeriodType
import com.example.domain.model.RoadmapHorizon
import com.example.domain.model.RoadmapMilestone
import com.example.ui.components.HolographicPanel
import com.example.ui.components.SystemEmptyState
import com.example.ui.components.SystemSectionTitle
import com.example.ui.theme.SystemColors
import com.example.ui.viewmodel.IntelligenceUiState
import java.time.DayOfWeek
import java.time.LocalDate

// SCREEN 11: LONG-TERM DEVELOPMENT ROADMAP
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DevelopmentRoadmapScreen(
    uiState: IntelligenceUiState,
    onClearFeedback: () -> Unit,
    onGenerateRoadmap: (RoadmapHorizon, List<String>) -> Unit,
    onApproveRoadmap: (String) -> Unit,
    onToggleMilestone: (String) -> Unit,
    onEditMilestone: (String, String, String) -> Unit
) {
    var selectedHorizon by rememberSaveable { mutableStateOf(RoadmapHorizon.THIRTY_DAYS) }
    var selectedDomains by remember { mutableStateOf(setOf("Physical", "Academic", "Discipline", "RPG")) }
    var editingMilestone by remember { mutableStateOf<RoadmapMilestone?>(null) }
    var editTitle by rememberSaveable { mutableStateOf("") }
    var editNotes by rememberSaveable { mutableStateOf("") }

    IntelligenceStateContainer(
        uiState = uiState,
        onRetry = { onGenerateRoadmap(selectedHorizon, selectedDomains.toList()) },
        onClearFeedback = onClearFeedback
    ) { state ->
        item {
            HolographicPanel(accentColor = SystemColors.ElectricCyan) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SystemSectionTitle("Long-Term Roadmap Architect")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        RoadmapHorizon.entries.forEach { horizon ->
                            FilterChip(
                                selected = selectedHorizon == horizon,
                                onClick = { selectedHorizon = horizon },
                                label = { Text(horizon.label) },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SystemColors.ElectricCyan.copy(alpha = 0.2f))
                            )
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Physical", "Academic", "Discipline", "RPG").forEach { domain ->
                            val active = domain in selectedDomains
                            FilterChip(
                                selected = active,
                                onClick = {
                                    selectedDomains = if (active && selectedDomains.size > 1) selectedDomains - domain else selectedDomains + domain
                                },
                                label = { Text(domain) }
                            )
                        }
                    }
                    Button(
                        onClick = { onGenerateRoadmap(selectedHorizon, selectedDomains.toList()) },
                        modifier = Modifier.fillMaxWidth().testTag("generate_roadmap_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SystemColors.ElectricCyan,
                            contentColor = SystemColors.ObsidianBlack
                        )
                    ) {
                        Text("GENERATE DRAFT ROADMAP")
                    }
                }
            }
        }

        if (state.roadmaps.isEmpty()) {
            item {
                SystemEmptyState(
                    title = "No Roadmaps Saved",
                    description = "Select a planning horizon above and generate an editable multi-week roadmap."
                )
            }
        } else {
            items(state.roadmaps, key = { it.roadmapUuid }) { roadmap ->
                HolographicPanel(accentColor = if (roadmap.isApproved) SystemColors.VerifiedEmerald else SystemColors.ArcaneViolet) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(roadmap.title, style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce, modifier = Modifier.weight(1f))
                            IntelligencePill(if (roadmap.isApproved) "APPROVED" else "DRAFT", if (roadmap.isApproved) SystemColors.VerifiedEmerald else SystemColors.WarningAmber)
                        }
                        Text(roadmap.visionSummary, style = MaterialTheme.typography.bodySmall, color = SystemColors.MetallicSilver)
                        Text("Risks: ${roadmap.riskNotes}", style = MaterialTheme.typography.labelSmall, color = SystemColors.WarningAmber)
                        Text("Next Review: ${LocalDate.ofEpochDay(roadmap.nextReviewDateEpochDay)}", style = MaterialTheme.typography.labelSmall, color = SystemColors.ElectricCyan)

                        roadmap.milestones.forEach { ms ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = ms.isCompleted,
                                    onCheckedChange = { onToggleMilestone(ms.milestoneUuid) }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${ms.domain} (Day +${ms.targetDayOffset}): ${ms.title}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (ms.isCompleted) SystemColors.VerifiedEmerald else SystemColors.PureIce
                                    )
                                    if (ms.notes.isNotBlank()) {
                                        Text(ms.notes, style = MaterialTheme.typography.labelSmall, color = SystemColors.MutedSilver)
                                    }
                                }
                                IconButton(onClick = {
                                    editTitle = ms.title
                                    editNotes = ms.notes
                                    editingMilestone = ms
                                }) {
                                    Icon(Icons.Rounded.Edit, contentDescription = "Edit Milestone", tint = SystemColors.ElectricCyan, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        if (!roadmap.isApproved) {
                            Button(
                                onClick = { onApproveRoadmap(roadmap.roadmapUuid) },
                                modifier = Modifier.fillMaxWidth().testTag("approve_roadmap_${roadmap.roadmapUuid}"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SystemColors.VerifiedEmerald,
                                    contentColor = SystemColors.ObsidianBlack
                                )
                            ) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("APPROVE ROADMAP")
                            }
                        }
                    }
                }
            }
        }
    }

    val msToEdit = editingMilestone
    if (msToEdit != null) {
        AlertDialog(
            onDismissRequest = { editingMilestone = null },
            containerColor = SystemColors.DeepNavy,
            title = { Text("Edit Roadmap Milestone", color = SystemColors.PureIce) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Milestone Title") }
                    )
                    OutlinedTextField(
                        value = editNotes,
                        onValueChange = { editNotes = it },
                        label = { Text("Notes / Target Metrics") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onEditMilestone(msToEdit.milestoneUuid, editTitle, editNotes)
                    editingMilestone = null
                }) { Text("SAVE") }
            },
            dismissButton = {
                TextButton(onClick = { editingMilestone = null }) { Text("CANCEL") }
            }
        )
    }
}

// SCREEN 12 & 13: WEEKLY & MONTHLY INTELLIGENCE REPORTS
@Composable
fun WeeklyIntelligenceReportScreen(
    uiState: IntelligenceUiState,
    onClearFeedback: () -> Unit,
    onGenerateReport: () -> Unit
) = IntelligenceReportScreenTemplate(
    periodType = ReportPeriodType.WEEKLY,
    uiState = uiState,
    onClearFeedback = onClearFeedback,
    onGenerateReport = onGenerateReport
)

@Composable
fun MonthlyIntelligenceReportScreen(
    uiState: IntelligenceUiState,
    onClearFeedback: () -> Unit,
    onGenerateReport: () -> Unit
) = IntelligenceReportScreenTemplate(
    periodType = ReportPeriodType.MONTHLY,
    uiState = uiState,
    onClearFeedback = onClearFeedback,
    onGenerateReport = onGenerateReport
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IntelligenceReportScreenTemplate(
    periodType: ReportPeriodType,
    uiState: IntelligenceUiState,
    onClearFeedback: () -> Unit,
    onGenerateReport: () -> Unit
) {
    IntelligenceStateContainer(uiState = uiState, onRetry = onGenerateReport, onClearFeedback = onClearFeedback) { state ->
        item {
            HolographicPanel(accentColor = SystemColors.ElectricCyan) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SystemSectionTitle(
                        title = periodType.label,
                        subtitle = "Deterministic verified summary + optional consented AI coaching"
                    )
                    Button(
                        onClick = onGenerateReport,
                        modifier = Modifier.fillMaxWidth().testTag("generate_${periodType.name.lowercase()}_report"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SystemColors.ElectricCyan,
                            contentColor = SystemColors.ObsidianBlack
                        )
                    ) {
                        Text("GENERATE LATEST ${periodType.label.uppercase()}")
                    }
                }
            }
        }

        val reports = state.reports.filter { it.periodType == periodType }
        if (reports.isEmpty()) {
            item {
                SystemEmptyState(
                    title = "No ${periodType.label} Generated Yet",
                    description = "Generate a report above to compute measured vs self-reported performance."
                )
            }
        } else {
            items(reports, key = { it.reportUuid }) { report ->
                HolographicPanel(accentColor = SystemColors.ArcaneViolet) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            IntelligencePill(report.periodLabel, SystemColors.ElectricCyan)
                            ProvenanceBadgeChip(DataProvenanceTag.MEASURED)
                            if (report.isPartialDataWindow) {
                                IntelligencePill("PARTIAL DATA WINDOW (${report.activeDaysObserved}D)", SystemColors.WarningAmber)
                            }
                        }
                        Text(report.measuredStatsSummary, style = MaterialTheme.typography.bodySmall, color = SystemColors.VerifiedEmerald)
                        Text(report.selfReportedStatsSummary, style = MaterialTheme.typography.bodySmall, color = SystemColors.SystemBlue)
                        MetricLine("Workouts / Jogging", "${report.workoutsCount} sessions / ${report.joggingMinutes} min")
                        MetricLine("Study / Revisions", "${report.studyMinutes} min / ${report.revisionsCount} reviews")
                        MetricLine("Habits / Tasks / Dungeons", "${report.habitsCompletedCount} / ${report.tasksCompletedCount} / ${report.dungeonsClearedCount}")
                        MetricLine("Window XP Observed", "${report.xpGainedWindow} XP")

                        if (report.deterministicHighlights.isNotEmpty()) {
                            Text("HIGHLIGHTS", style = MaterialTheme.typography.labelMedium, color = SystemColors.ElectricCyan)
                            report.deterministicHighlights.forEach {
                                Text("• $it", style = MaterialTheme.typography.bodySmall, color = SystemColors.PureIce)
                            }
                        }
                        if (report.deterministicRisks.isNotEmpty()) {
                            Text("RISKS & LIMITATIONS", style = MaterialTheme.typography.labelMedium, color = SystemColors.WarningAmber)
                            report.deterministicRisks.forEach {
                                Text("• $it", style = MaterialTheme.typography.bodySmall, color = SystemColors.WarningAmber)
                            }
                        }
                        if (!report.aiNarrativeSummary.isNullOrBlank()) {
                            Text("AI COACHING REFLECTION (${if (report.aiUsedFallback) "OFFLINE" else "CLOUD"})", style = MaterialTheme.typography.labelMedium, color = SystemColors.ArcaneViolet)
                            Text(report.aiNarrativeSummary, style = MaterialTheme.typography.bodySmall, color = SystemColors.MetallicSilver)
                        } else {
                            Text(
                                text = "AI Narrative: Offline deterministic mode active (Cloud AI opt-in disabled or unavailable).",
                                style = MaterialTheme.typography.labelSmall,
                                color = SystemColors.MutedSilver
                            )
                        }
                    }
                }
            }
        }
    }
}

// SCREEN 14: AI PERSONAL DEVELOPMENT ADVISOR
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PersonalAdvisorScreen(
    uiState: IntelligenceUiState,
    onClearFeedback: () -> Unit,
    onAskAdvisor: (String) -> Unit
) {
    var question by rememberSaveable { mutableStateOf("") }

    IntelligenceStateContainer(uiState = uiState, onRetry = {}, onClearFeedback = onClearFeedback) { state ->
        item {
            HolographicPanel(accentColor = SystemColors.ArcaneViolet) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SystemSectionTitle(
                        title = "Personal Development Advisor",
                        subtitle = "Read-only advisory Q&A with prompt-injection & recovery safety guardrails"
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "How should I train today given recovery?",
                            "What study topics need urgent revision?",
                            "How balanced is my current workload?"
                        ).forEach { quick ->
                            FilterChip(
                                selected = false,
                                onClick = { onAskAdvisor(quick) },
                                label = { Text(quick) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = question,
                        onValueChange = { question = it },
                        label = { Text("Ask the System Advisor...") },
                        modifier = Modifier.fillMaxWidth().testTag("advisor_question_input")
                    )
                    Button(
                        onClick = {
                            if (question.isNotBlank()) {
                                onAskAdvisor(question)
                                question = ""
                            }
                        },
                        enabled = question.isNotBlank() && !state.isBusy,
                        modifier = Modifier.fillMaxWidth().testTag("ask_advisor_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SystemColors.ArcaneViolet,
                            contentColor = SystemColors.PureIce
                        )
                    ) {
                        Icon(Icons.Rounded.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("CONSULT ADVISOR")
                    }
                }
            }
        }

        if (state.consultations.isEmpty()) {
            item {
                SystemEmptyState(
                    title = "No Consultations Logged",
                    description = "Ask a question above to run deterministic diagnostics (or cloud AI if consented)."
                )
            }
        } else {
            items(state.consultations, key = { it.consultationUuid }) { c ->
                HolographicPanel(accentColor = SystemColors.ElectricCyan) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            c.provenanceTags.forEach { ProvenanceBadgeChip(it) }
                            ConfidenceBadgeChip(c.confidence)
                            IntelligencePill(
                                if (c.usedFallback) "OFFLINE DETERMINISTIC" else c.connectionStatus.label.uppercase(),
                                if (c.usedFallback) SystemColors.SystemBlue else SystemColors.VerifiedEmerald
                            )
                        }
                        Text("Q: ${c.question}", style = MaterialTheme.typography.titleSmall, color = SystemColors.ElectricCyan)
                        Text(c.aiResponse, style = MaterialTheme.typography.bodySmall, color = SystemColors.PureIce)
                        if (c.limitationNotes.isNotEmpty()) {
                            Text(
                                text = "Limitations: ${c.limitationNotes.joinToString(" | ")}",
                                style = MaterialTheme.typography.labelSmall,
                                color = SystemColors.MutedSilver
                            )
                        }
                    }
                }
            }
        }
    }
}

// SCREEN 15: INTELLIGENCE PREFERENCES & PRIVACY
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IntelligencePreferencesScreen(
    uiState: IntelligenceUiState,
    onClearFeedback: () -> Unit,
    onSavePreferences: (PlayerIntelligencePreferences) -> Unit,
    onClearAllData: () -> Unit
) {
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    val p = (uiState as? IntelligenceUiState.Ready)?.preferences ?: PlayerIntelligencePreferences()
    var style by remember(p) { mutableStateOf(p.planningStyle) }
    var notif by remember(p) { mutableStateOf(p.notificationIntensity) }
    var studyHour by remember(p) { mutableIntStateOf(p.preferredStudyHour) }
    var workoutHour by remember(p) { mutableIntStateOf(p.preferredWorkoutHour) }
    var maxFocus by remember(p) { mutableIntStateOf(p.maxDailyFocusMinutes) }
    var maxWorkout by remember(p) { mutableIntStateOf(p.maxDailyWorkoutMinutes) }
    var shareCloud by remember(p) { mutableStateOf(p.shareIntelligenceWithCloudAi) }
    var sharePhys by remember(p) { mutableStateOf(p.sharePhysicalWithIntelligence) }
    var shareAcad by remember(p) { mutableStateOf(p.shareAcademicWithIntelligence) }
    var shareLife by remember(p) { mutableStateOf(p.shareLifeWithIntelligence) }
    var shareRpg by remember(p) { mutableStateOf(p.shareRpgWithIntelligence) }
    var restDays by remember(p) { mutableStateOf(p.restDays) }

    IntelligenceStateContainer(uiState = uiState, onRetry = {}, onClearFeedback = onClearFeedback) { _ ->

        item {
            HolographicPanel(accentColor = SystemColors.ElectricCyan) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SystemSectionTitle("Planning Style & Daily Bounds")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PlanningStyle.entries.forEach { st ->
                            FilterChip(
                                selected = style == st,
                                onClick = { style = st },
                                label = { Text(st.label) }
                            )
                        }
                    }
                    Text("Notification Intensity", style = MaterialTheme.typography.labelMedium, color = SystemColors.PureIce)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NotificationIntensity.entries.forEach { ni ->
                            FilterChip(
                                selected = notif == ni,
                                onClick = { notif = ni },
                                label = { Text(ni.label) }
                            )
                        }
                    }
                    StepperRow("Preferred Study Hour", "${studyHour}:00", { studyHour = (studyHour - 1).coerceIn(5, 22) }, { studyHour = (studyHour + 1).coerceIn(5, 22) })
                    StepperRow("Preferred Workout Hour", "${workoutHour}:00", { workoutHour = (workoutHour - 1).coerceIn(5, 22) }, { workoutHour = (workoutHour + 1).coerceIn(5, 22) })
                    StepperRow("Max Daily Focus", "$maxFocus min", { maxFocus = (maxFocus - 30).coerceIn(60, 360) }, { maxFocus = (maxFocus + 30).coerceIn(60, 360) })
                    StepperRow("Max Daily Workout", "$maxWorkout min", { maxWorkout = (maxWorkout - 15).coerceIn(30, 180) }, { maxWorkout = (maxWorkout + 15).coerceIn(30, 180) })

                    Text("Protected Penalty-Free Rest Days", style = MaterialTheme.typography.labelMedium, color = SystemColors.VerifiedEmerald)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        DayOfWeek.entries.forEach { day ->
                            val selected = day in restDays
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    restDays = if (selected) restDays - day else restDays + day
                                },
                                label = { Text(day.name.take(3)) }
                            )
                        }
                    }
                }
            }
        }

        item {
            HolographicPanel(accentColor = SystemColors.ArcaneViolet) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SystemSectionTitle("Domain Telemetry & Privacy Controls")
                    ToggleLine("Allow Cloud AI Synthesis (Consent Required)", shareCloud) { shareCloud = it }
                    ToggleLine("Include Physical Telemetry", sharePhys) { sharePhys = it }
                    ToggleLine("Include Academic Telemetry", shareAcad) { shareAcad = it }
                    ToggleLine("Include Life & Discipline Telemetry", shareLife) { shareLife = it }
                    ToggleLine("Include RPG Progression Telemetry", shareRpg) { shareRpg = it }

                    Button(
                        onClick = {
                            onSavePreferences(
                                p.copy(
                                    planningStyle = style,
                                    notificationIntensity = notif,
                                    preferredStudyHour = studyHour,
                                    preferredWorkoutHour = workoutHour,
                                    maxDailyFocusMinutes = maxFocus,
                                    maxDailyWorkoutMinutes = maxWorkout,
                                    shareIntelligenceWithCloudAi = shareCloud,
                                    sharePhysicalWithIntelligence = sharePhys,
                                    shareAcademicWithIntelligence = shareAcad,
                                    shareLifeWithIntelligence = shareLife,
                                    shareRpgWithIntelligence = shareRpg,
                                    restDays = restDays
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth().testTag("save_intelligence_preferences_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SystemColors.ElectricCyan,
                            contentColor = SystemColors.ObsidianBlack
                        )
                    ) {
                        Text("SAVE PREFERENCES & RE-SYNC")
                    }

                    OutlinedButton(
                        onClick = { confirmClear = true },
                        modifier = Modifier.fillMaxWidth().testTag("clear_intelligence_data_button"),
                        border = BorderStroke(1.dp, SystemColors.DangerCrimson)
                    ) {
                        Icon(Icons.Rounded.DeleteForever, contentDescription = null, tint = SystemColors.DangerCrimson, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("RESET / DELETE INTELLIGENCE DATA", color = SystemColors.DangerCrimson)
                    }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            containerColor = SystemColors.DeepNavy,
            title = { Text("Delete Stored Intelligence Data?", color = SystemColors.DangerCrimson) },
            text = {
                Text(
                    "This removes cached profiles, findings, daily plan drafts, recommendations, roadmaps, reports, and advisor history. Your Player Level, XP, Quests, Physical, Academic, Life, and RPG records remain untouched.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SystemColors.PureIce
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllData()
                        confirmClear = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SystemColors.DangerCrimson)
                ) { Text("DELETE") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("CANCEL") }
            }
        )
    }
}

@Composable
private fun StepperRow(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = SystemColors.PureIce, modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onMinus) { Text("-") }
            Text(value, style = MaterialTheme.typography.labelLarge, color = SystemColors.ElectricCyan)
            OutlinedButton(onClick = onPlus) { Text("+") }
        }
    }
}

@Composable
private fun ToggleLine(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = SystemColors.PureIce, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
