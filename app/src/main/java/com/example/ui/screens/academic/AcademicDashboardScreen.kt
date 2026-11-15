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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.AssignmentTurnedIn
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.components.GlowingProgressBar
import com.example.ui.components.HolographicPanel
import com.example.ui.components.SystemEmptyState
import com.example.ui.components.SystemErrorState
import com.example.ui.components.SystemLoadingView
import com.example.ui.components.SystemSectionTitle
import com.example.ui.theme.SystemColors
import com.example.ui.viewmodel.AcademicUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AcademicDashboardScreen(
    uiState: AcademicUiState,
    onRetry: () -> Unit,
    onClearFeedback: () -> Unit,
    onOpenSubjects: () -> Unit,
    onOpenSubjectDetail: (Long) -> Unit,
    onOpenFocusTimer: () -> Unit,
    onOpenQuests: () -> Unit,
    onOpenRevisions: () -> Unit,
    onOpenExams: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onOpenHistory: () -> Unit,
    onApproveProposal: (Int) -> Unit,
    onDismissProposal: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is AcademicUiState.Loading -> SystemLoadingView("INITIALIZING ACADEMIC ENGINE...", modifier)
        is AcademicUiState.Error -> Box(
            modifier = modifier
                .fillMaxSize()
                .background(SystemColors.ObsidianBlack)
                .padding(20.dp)
        ) {
            SystemErrorState(message = uiState.message, onRetry = onRetry)
        }
        is AcademicUiState.Ready -> {
            val summary = uiState.summary
            val dailyFraction = (summary.todayStudyMinutes.toFloat() / summary.dailyStudyGoalMinutes.coerceAtLeast(1)).coerceIn(0f, 1f)

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
                        .testTag("academic_dashboard_screen"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        SystemSectionTitle(
                            title = "Academic Development",
                            subtitle = "Intelligence, Wisdom & Discipline progression"
                        )
                    }

                    if (uiState.feedbackMessage != null) {
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
                                        text = uiState.feedbackMessage,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SystemColors.PureIce,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = onClearFeedback) {
                                        Text("OK", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        HolographicPanel(
                            modifier = Modifier.fillMaxWidth(),
                            accentColor = SystemColors.ElectricCyan,
                            secondaryColor = SystemColors.ArcaneViolet
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("ACADEMIC PROGRESSION", style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
                                    Text("+${summary.totalAcademicXp} XP", style = MaterialTheme.typography.labelLarge, color = SystemColors.VerifiedEmerald)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AcademicMetricTile("INT", "${summary.intPoints}", Modifier.weight(1f), SystemColors.ElectricCyan)
                                    AcademicMetricTile("WIS", "${summary.wisPoints}", Modifier.weight(1f), SystemColors.ArcaneViolet)
                                    AcademicMetricTile("DIS", "${summary.disPoints}", Modifier.weight(1f), SystemColors.VerifiedEmerald)
                                    AcademicMetricTile("SYLLABUS", "${summary.overallSyllabusCompletionPercent}%", Modifier.weight(1f))
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Today's Study Target", style = MaterialTheme.typography.bodySmall, color = SystemColors.MetallicSilver)
                                    Text(
                                        "${summary.todayStudyMinutes} / ${summary.dailyStudyGoalMinutes} min",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = SystemColors.ElectricCyan
                                    )
                                }
                                GlowingProgressBar(progress = dailyFraction, height = 8.dp)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AcademicMetricTile("7D STUDY", "${summary.weeklyStudyMinutes}m", Modifier.weight(1f))
                                    AcademicMetricTile("TOTAL", "${summary.totalStudyMinutes}m", Modifier.weight(1f))
                                    AcademicMetricTile("REVISIONS", "${summary.pendingRevisionsCount}", Modifier.weight(1f),
                                        if (summary.overdueRevisionsCount > 0) SystemColors.WarningAmber else SystemColors.PureIce)
                                    AcademicMetricTile("EXAMS", "${summary.upcomingExamsCount}", Modifier.weight(1f))
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(
                                        onClick = onOpenFocusTimer,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("academic_open_focus_button"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = SystemColors.ElectricCyan,
                                            contentColor = SystemColors.ObsidianBlack
                                        )
                                    ) {
                                        Icon(Icons.Rounded.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("FOCUS TIMER", style = MaterialTheme.typography.labelMedium)
                                    }
                                    OutlinedButton(
                                        onClick = onOpenSubjects,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("academic_open_subjects_button")
                                    ) {
                                        Icon(Icons.Rounded.Book, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("SUBJECTS (${summary.activeSubjectsCount})", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }

                    // Subsystem Matrix (Access to all 10 screens cleanly)
                    item {
                        SystemSectionTitle(
                            title = "Academic Subsystems",
                            subtitle = "Quests (${summary.completedAcademicQuestsCount} cleared) • Telemetry & Exams"
                        )
                    }

                    item {
                        val navCards = listOf(
                            AcademicNavSpec("Subjects", "${summary.activeSubjectsCount} active", Icons.Rounded.Book, SystemColors.ElectricCyan, "acad_nav_subjects", onOpenSubjects),
                            AcademicNavSpec("Focus Engine", if (uiState.activeFocusTimer.isActive) "Session Active" else "25 / 50 / 90m", Icons.Rounded.Timer, SystemColors.VerifiedEmerald, "acad_nav_focus", onOpenFocusTimer),
                            AcademicNavSpec("Study Quests", "${uiState.academicQuests.count { it.status.name == "ACTIVE" }} active", Icons.Rounded.AssignmentTurnedIn, SystemColors.ArcaneViolet, "acad_nav_quests", onOpenQuests),
                            AcademicNavSpec("Revision Centre", "${summary.pendingRevisionsCount} due (${summary.overdueRevisionsCount} overdue)", Icons.Rounded.Psychology, SystemColors.WarningAmber, "acad_nav_revisions", onOpenRevisions),
                            AcademicNavSpec("Exam Centre", "${summary.upcomingExamsCount} upcoming", Icons.Rounded.EventNote, SystemColors.DangerCrimson, "acad_nav_exams", onOpenExams),
                            AcademicNavSpec("Analytics", "${summary.studyConsistencyDays7d}/7d streak", Icons.Rounded.Analytics, SystemColors.SystemBlue, "acad_nav_analytics", onOpenAnalytics),
                            AcademicNavSpec("History", "${uiState.progressionRecords.size} records", Icons.Rounded.History, SystemColors.MetallicSilver, "acad_nav_history", onOpenHistory)
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            maxItemsInEachRow = 2
                        ) {
                            navCards.forEach { card ->
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable(onClick = card.onClick)
                                        .testTag(card.testTag),
                                    color = SystemColors.DeepNavy,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, card.accent.copy(alpha = 0.45f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(card.icon, contentDescription = card.title, tint = card.accent, modifier = Modifier.size(22.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(card.title.uppercase(), style = MaterialTheme.typography.labelLarge, color = SystemColors.PureIce)
                                            Text(card.subtitle, style = MaterialTheme.typography.bodySmall, color = SystemColors.MutedSilver)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // AI-Ready Academic Study Plan Proposal (strictly advisory)
                    uiState.studyPlanProposal?.let { proposal ->
                        item {
                            HolographicPanel(
                                modifier = Modifier.fillMaxWidth(),
                                accentColor = SystemColors.ArcaneViolet
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = SystemColors.ArcaneViolet, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "AI ACADEMIC ADVISOR (OPENROUTER READY)",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = SystemColors.ArcaneViolet
                                        )
                                    }
                                    Text(proposal.rationale, style = MaterialTheme.typography.bodySmall, color = SystemColors.MetallicSilver)
                                    if (proposal.recommendedDailyMinutes != proposal.currentDailyMinutes) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Button(
                                                onClick = { onApproveProposal(proposal.recommendedDailyMinutes) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = SystemColors.ArcaneViolet,
                                                    contentColor = SystemColors.PureIce
                                                )
                                            ) {
                                                Text("APPROVE ${proposal.recommendedDailyMinutes} MIN/DAY", style = MaterialTheme.typography.labelMedium)
                                            }
                                            OutlinedButton(onClick = { onDismissProposal(proposal.recommendedDailyMinutes) }) {
                                                Text("KEEP ${proposal.currentDailyMinutes} MIN", style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Active Subjects Quick List
                    item {
                        SystemSectionTitle(
                            title = "Active Subjects",
                            subtitle = "Tap a subject to manage modules, chapters & exams"
                        )
                    }

                    val activeSummaries = uiState.subjectSummaries.filter { !it.subject.isArchived }
                    if (activeSummaries.isEmpty()) {
                        item {
                            SystemEmptyState(
                                title = "No Subjects Configured",
                                description = "Create your first subject in Subject Management to build chapters, topics, focus sessions, and exam targets."
                            )
                        }
                    } else {
                        items(activeSummaries.size) { idx ->
                            val item = activeSummaries[idx]
                            val accent = item.subject.colorToken.toHudColor()
                            HolographicPanel(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenSubjectDetail(item.subject.id) },
                                accentColor = accent,
                                contentPadding = 12.dp
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Icon(item.subject.iconKey.toVector(), contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(item.subject.name, style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
                                                Text(
                                                    "${item.subject.programSystem} • ${item.subject.priority.label} Priority",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = SystemColors.MutedSilver
                                                )
                                            }
                                        }
                                        Text("${item.syllabusCompletionPercent}%", style = MaterialTheme.typography.labelLarge, color = accent)
                                    }
                                    GlowingProgressBar(progress = item.syllabusCompletionPercent / 100f, height = 6.dp, startColor = accent)
                                    Text(
                                        text = "${item.completedChapters}/${item.chapterCount} chapters • ${item.completedTopics}/${item.topicCount} topics • ${item.totalStudyMinutes} min logged",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SystemColors.MetallicSilver
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

private data class AcademicNavSpec(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color,
    val testTag: String,
    val onClick: () -> Unit
)
