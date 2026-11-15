package com.example.ui.screens.academic

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.AcademicActivityKind
import com.example.domain.model.VerificationStatus
import com.example.ui.components.GlowingProgressBar
import com.example.ui.components.HolographicPanel
import com.example.ui.components.ProvenanceChip
import com.example.ui.components.SystemEmptyState
import com.example.ui.components.SystemErrorState
import com.example.ui.components.SystemLoadingView
import com.example.ui.components.SystemSectionTitle
import com.example.ui.theme.SystemColors
import com.example.ui.viewmodel.AcademicUiState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

private data class ChartBarDatum(
    val label: String,
    val value: Float,
    val color: Color
)

/**
 * Screen 9: Academic Performance Analytics (Compose Canvas charts + explicit provenance separation).
 */
@Composable
fun AcademicAnalyticsScreen(
    uiState: AcademicUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is AcademicUiState.Loading -> SystemLoadingView("COMPUTING ACADEMIC TELEMETRY...", modifier)
        is AcademicUiState.Error -> Box(modifier.fillMaxSize().background(SystemColors.ObsidianBlack).padding(20.dp)) {
            SystemErrorState(uiState.message, onRetry)
        }
        is AcademicUiState.Ready -> {
            val summary = uiState.summary
            val timerVerifiedCount = uiState.progressionRecords.count { it.verificationStatus == VerificationStatus.VERIFIED_SYSTEM_TIMER }
            val assessmentVerifiedCount = uiState.progressionRecords.count { it.verificationStatus == VerificationStatus.VERIFIED_ASSESSMENT }
            val selfReportedCount = uiState.progressionRecords.count { it.verificationStatus == VerificationStatus.SELF_REPORTED }
            val avgFocusMinutes = if (uiState.studySessions.isEmpty()) 0 else {
                uiState.studySessions.map { it.durationMinutes }.average().toInt()
            }

            val subjectBars = uiState.subjectSummaries
                .filter { !it.subject.isArchived || it.totalStudyMinutes > 0 }
                .take(6)
                .map {
                    ChartBarDatum(
                        label = it.subject.name.take(10),
                        value = it.totalStudyMinutes.toFloat(),
                        color = it.subject.colorToken.toHudColor()
                    )
                }

            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val dailyBars = (6 downTo 0).map { daysAgo ->
                val date = today.minusDays(daysAgo.toLong())
                val startMs = date.atStartOfDay(zone).toInstant().toEpochMilli()
                val endMs = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val mins = uiState.studySessions
                    .filter { it.startEpochMs in startMs until endMs }
                    .sumOf { it.durationMinutes } +
                    uiState.revisionRecords
                        .filter { it.revisedAtEpochMs in startMs until endMs }
                        .sumOf { it.durationMinutes }
                ChartBarDatum(
                    label = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    value = mins.toFloat(),
                    color = SystemColors.ElectricCyan
                )
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
                        .testTag("academic_analytics_screen"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        SystemSectionTitle(
                            title = "Academic Analytics",
                            subtitle = "Distinguishing self-reported logs, timer blocks & verified assessments"
                        )
                    }

                    item {
                        HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.ElectricCyan) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AcademicMetricTile("7D STREAK", "${summary.studyConsistencyDays7d}/7d", Modifier.weight(1f), SystemColors.ElectricCyan)
                                    AcademicMetricTile("AVG BLOCK", "${avgFocusMinutes}m", Modifier.weight(1f), SystemColors.ArcaneViolet)
                                    AcademicMetricTile("REVISIONS", "${uiState.revisionRecords.size}", Modifier.weight(1f), SystemColors.VerifiedEmerald)
                                    AcademicMetricTile("QUESTS", "${summary.completedAcademicQuestsCount}", Modifier.weight(1f), SystemColors.MonarchGold)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AcademicMetricTile("TIMER VERIFIED", "$timerVerifiedCount", Modifier.weight(1f), SystemColors.ElectricCyan)
                                    AcademicMetricTile("ASSESSMENT VER", "$assessmentVerifiedCount", Modifier.weight(1f), SystemColors.VerifiedEmerald)
                                    AcademicMetricTile("SELF-REPORTED", "$selfReportedCount", Modifier.weight(1f), SystemColors.WarningAmber)
                                }
                            }
                        }
                    }

                    item {
                        HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.SystemBlue) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("7-DAY STUDY CONSISTENCY (MINUTES)", style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
                                AcademicBarChart(data = dailyBars, modifier = Modifier.fillMaxWidth().height(150.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    dailyBars.forEach { d ->
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("${d.value.toInt()}m", style = MaterialTheme.typography.labelSmall, color = SystemColors.ElectricCyan)
                                            Text(d.label, style = MaterialTheme.typography.labelSmall, color = SystemColors.MutedSilver)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (subjectBars.isNotEmpty()) {
                        item {
                            HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.ArcaneViolet) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("STUDY TIME BY SUBJECT (MINUTES)", style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
                                    AcademicBarChart(data = subjectBars, modifier = Modifier.fillMaxWidth().height(140.dp))
                                    subjectBars.forEach { b ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(b.label, style = MaterialTheme.typography.bodySmall, color = b.color)
                                            Text("${b.value.toInt()} min", style = MaterialTheme.typography.labelMedium, color = SystemColors.PureIce)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.VerifiedEmerald) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("ASSESSMENT & SYLLABUS SNAPSHOT", style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
                                if (uiState.assessments.isEmpty()) {
                                    Text("Record quizzes or official exam scores to view objective assessment performance.", style = MaterialTheme.typography.bodySmall, color = SystemColors.MutedSilver)
                                } else {
                                    uiState.assessments.take(6).forEach { a ->
                                        val pct = (a.normalizedRatio * 100).toInt()
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("${a.subjectName} • ${a.title}", style = MaterialTheme.typography.bodySmall, color = SystemColors.PureIce)
                                                GlowingProgressBar(progress = a.normalizedRatio, height = 5.dp, startColor = SystemColors.VerifiedEmerald)
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text("$pct%", style = MaterialTheme.typography.labelLarge, color = SystemColors.VerifiedEmerald)
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
private fun AcademicBarChart(
    data: List<ChartBarDatum>,
    modifier: Modifier = Modifier
) {
    val gridColor = SystemColors.SubtleGridLine
    Canvas(modifier = modifier) {
        if (data.isEmpty()) return@Canvas
        val maxVal = (data.maxOfOrNull { it.value } ?: 0f).coerceAtLeast(30f)
        val count = data.size
        val slotWidth = size.width / count
        val barWidth = (slotWidth * 0.56f).coerceAtLeast(10f)

        // Baseline grid line
        drawLine(
            color = gridColor,
            start = Offset(0f, size.height - 2f),
            end = Offset(size.width, size.height - 2f),
            strokeWidth = 2f
        )

        data.forEachIndexed { idx, datum ->
            val ratio = (datum.value / maxVal).coerceIn(0f, 1f)
            val barHeight = (size.height - 12f) * ratio
            val left = idx * slotWidth + (slotWidth - barWidth) / 2f
            val top = size.height - 2f - barHeight
            drawRect(
                color = datum.color,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight.coerceAtLeast(3f))
            )
        }
    }
}

private enum class AcademicHistoryFilter(val label: String) {
    ALL("All"),
    FOCUS("Study & Focus"),
    REVISIONS("Revisions"),
    ASSESSMENTS("Assessments"),
    QUESTS("Quests & Chapters")
}

/**
 * Screen 10: Academic History & Progression Audit Ledger.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AcademicHistoryScreen(
    uiState: AcademicUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is AcademicUiState.Loading -> SystemLoadingView("LOADING ACADEMIC LEDGER...", modifier)
        is AcademicUiState.Error -> Box(modifier.fillMaxSize().background(SystemColors.ObsidianBlack).padding(20.dp)) {
            SystemErrorState(uiState.message, onRetry)
        }
        is AcademicUiState.Ready -> {
            var filter by rememberSaveable { mutableStateOf(AcademicHistoryFilter.ALL) }
            val filtered = uiState.progressionRecords.filter { rec ->
                when (filter) {
                    AcademicHistoryFilter.ALL -> true
                    AcademicHistoryFilter.FOCUS -> rec.activityKind == AcademicActivityKind.FOCUS_SESSION || rec.activityKind == AcademicActivityKind.STUDY_SESSION
                    AcademicHistoryFilter.REVISIONS -> rec.activityKind == AcademicActivityKind.REVISION
                    AcademicHistoryFilter.ASSESSMENTS -> rec.activityKind == AcademicActivityKind.EXAM_ASSESSMENT || rec.activityKind == AcademicActivityKind.PRACTICE_ASSESSMENT
                    AcademicHistoryFilter.QUESTS -> rec.activityKind == AcademicActivityKind.ACADEMIC_QUEST || rec.activityKind == AcademicActivityKind.CHAPTER_COMPLETION
                }
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
                        .testTag("academic_history_screen"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SystemSectionTitle(
                            title = "Academic History",
                            subtitle = "Full audit log with INT / WIS / DIS & provenance tracking"
                        )
                    }

                    item {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            AcademicHistoryFilter.entries.forEach { f ->
                                FilterChip(
                                    selected = filter == f,
                                    onClick = { filter = f },
                                    label = { Text(f.label) }
                                )
                            }
                        }
                    }

                    if (filtered.isEmpty()) {
                        item {
                            SystemEmptyState("No Academic Records", "Completed study sessions, revisions, chapter clears, and assessments appear here.")
                        }
                    } else {
                        items(filtered, key = { it.id }) { rec ->
                            HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.ElectricCyan, contentPadding = 12.dp) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(rec.title, style = MaterialTheme.typography.titleSmall, color = SystemColors.PureIce, modifier = Modifier.weight(1f))
                                        ProvenanceChip(status = rec.verificationStatus)
                                    }
                                    Text(
                                        text = "${rec.activityKind.label} • +${rec.xpAwarded} XP (INT +${rec.intXpAwarded}, WIS +${rec.wisXpAwarded}, DIS +${rec.disXpAwarded})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SystemColors.VerifiedEmerald
                                    )
                                    Text(
                                        text = "${formatAcademicDateTime(rec.timestampEpochMs)} • ${rec.notes}",
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
