package com.example.ui.screens.intelligence

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.DataProvenanceTag
import com.example.domain.model.PersonalizedRecommendation
import com.example.domain.model.RecommendationStatus
import com.example.domain.model.TrendDataPoint
import com.example.ui.components.HolographicPanel
import com.example.ui.components.SystemEmptyState
import com.example.ui.components.SystemSectionTitle
import com.example.ui.theme.SystemColors
import com.example.ui.viewmodel.IntelligenceUiState

// SCREEN 6: CONSISTENCY & PREDICTIVE ANALYTICS
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConsistencyAnalyticsScreen(
    uiState: IntelligenceUiState,
    onRefresh: () -> Unit
) {
    var selectedHorizon by rememberSaveable { mutableIntStateOf(7) }

    IntelligenceStateContainer(uiState = uiState, onRetry = onRefresh) { state ->
        item {
            HolographicPanel(accentColor = SystemColors.ArcaneViolet) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SystemSectionTitle("Predictive Consistency Horizons")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(7, 14, 30).forEach { horizon ->
                            FilterChip(
                                selected = selectedHorizon == horizon,
                                onClick = { selectedHorizon = horizon },
                                label = { Text("${horizon}-Day Forecast") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SystemColors.ArcaneViolet.copy(alpha = 0.24f)
                                )
                            )
                        }
                    }
                    Text(
                        text = "Forecasts use linear trend slopes from stored activity days and never predict future performance without historical evidence.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SystemColors.MutedSilver
                    )
                }
            }
        }

        val visible = state.predictions.filter { it.horizonDays == selectedHorizon }
            .ifEmpty { state.predictions }

        if (visible.isEmpty()) {
            item {
                SystemEmptyState(
                    title = "No Predictions Evaluated",
                    description = "Refresh Intelligence Diagnostics to compute 7d, 14d, and 30d consistency forecasts."
                )
            }
        } else {
            items(visible, key = { it.predictionUuid }) { pred ->
                HolographicPanel(accentColor = SystemColors.ElectricCyan) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ProvenanceBadgeChip(pred.provenance)
                            ConfidenceBadgeChip(pred.confidence)
                            IntelligencePill("RISK: ${pred.riskLevelLabel.uppercase()}", SystemColors.WarningAmber)
                        }
                        Text(
                            text = "${pred.domainLabel} (${pred.horizonDays}d Forecast)",
                            style = MaterialTheme.typography.titleMedium,
                            color = SystemColors.PureIce
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Current: ${pred.currentConsistencyPercent}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SystemColors.VerifiedEmerald
                            )
                            Text(
                                text = "Projected: ${pred.predictedConsistencyPercent}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SystemColors.ArcaneViolet
                            )
                            Text(
                                text = "Slope: ${"%+.1f".format(pred.slopePerWeek)}%/wk",
                                style = MaterialTheme.typography.labelMedium,
                                color = SystemColors.ElectricCyan
                            )
                        }
                        if (pred.trendSeries.isNotEmpty()) {
                            ConsistencyTrendChart(points = pred.trendSeries)
                        }
                        if (pred.atRiskGoals.isNotEmpty()) {
                            Text(
                                text = "At-Risk Goals: ${pred.atRiskGoals.joinToString("; ")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = SystemColors.WarningAmber
                            )
                        }
                        Text(
                            text = pred.limitationNote,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (pred.insufficientData) SystemColors.DangerCrimson else SystemColors.MutedSilver
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConsistencyTrendChart(points: List<TrendDataPoint>) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .padding(vertical = 4.dp)
    ) {
        if (points.size < 2) return@Canvas
        val w = size.width
        val h = size.height
        val stepX = w / (points.size - 1).coerceAtLeast(1)

        // Horizontal grid lines
        listOf(0.25f, 0.5f, 0.75f).forEach { frac ->
            drawLine(
                color = SystemColors.SubtleGridLine,
                start = Offset(0f, h * frac),
                end = Offset(w, h * frac),
                strokeWidth = 1.dp.toPx()
            )
        }

        val path = Path()
        points.forEachIndexed { idx, pt ->
            val x = idx * stepX
            val y = h - ((pt.valuePercent.coerceIn(0, 100) / 100f) * h)
            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
            drawCircle(
                color = if (pt.isEstimate) SystemColors.ArcaneViolet else SystemColors.ElectricCyan,
                radius = 4.dp.toPx(),
                center = Offset(x, y)
            )
        }
        drawPath(
            path = path,
            color = SystemColors.ElectricCyan,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

// SCREEN 7: PERSONALIZED RECOMMENDATIONS
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PersonalizedRecommendationsScreen(
    uiState: IntelligenceUiState,
    onRefresh: () -> Unit,
    onClearFeedback: () -> Unit,
    onRespond: (String, RecommendationStatus, String) -> Unit,
    onNavigateDeepLink: (String) -> Unit
) {
    var filterStatus by rememberSaveable { mutableStateOf<RecommendationStatus?>(null) }
    var rejectingRec by remember { mutableStateOf<PersonalizedRecommendation?>(null) }
    var rejectNote by rememberSaveable { mutableStateOf("") }

    IntelligenceStateContainer(uiState = uiState, onRetry = onRefresh, onClearFeedback = onClearFeedback) { state ->
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = filterStatus == null,
                    onClick = { filterStatus = null },
                    label = { Text("All (${state.recommendations.size})") }
                )
                listOf(
                    RecommendationStatus.NEW,
                    RecommendationStatus.ACCEPTED,
                    RecommendationStatus.COMPLETED,
                    RecommendationStatus.REJECTED
                ).forEach { st ->
                    FilterChip(
                        selected = filterStatus == st,
                        onClick = { filterStatus = st },
                        label = { Text(st.label) }
                    )
                }
            }
        }

        val visible = state.recommendations.filter { filterStatus == null || it.status == filterStatus }
        if (visible.isEmpty()) {
            item {
                SystemEmptyState(
                    title = "No Recommendations Found",
                    description = "All active suggestions are cleared or filtered out."
                )
            }
        } else {
            items(visible, key = { it.recUuid }) { rec ->
                HolographicPanel(accentColor = SystemColors.ElectricCyan) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            IntelligencePill("${rec.category.label.uppercase()} • P${rec.priorityScore}", SystemColors.ElectricCyan)
                            IntelligencePill(rec.status.label.uppercase(), SystemColors.ArcaneViolet)
                            ProvenanceBadgeChip(rec.provenance)
                            ConfidenceBadgeChip(rec.confidence)
                        }
                        Text(rec.title, style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
                        Text(rec.description, style = MaterialTheme.typography.bodySmall, color = SystemColors.MetallicSilver)
                        Text("Rationale: ${rec.rationale}", style = MaterialTheme.typography.labelSmall, color = SystemColors.MutedSilver)
                        Text("Impact: ${rec.expectedImpact}", style = MaterialTheme.typography.labelSmall, color = SystemColors.VerifiedEmerald)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (rec.status == RecommendationStatus.NEW) {
                                Button(
                                    onClick = { onRespond(rec.recUuid, RecommendationStatus.ACCEPTED, "") },
                                    modifier = Modifier.weight(1f).testTag("accept_rec_${rec.recUuid}"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SystemColors.VerifiedEmerald,
                                        contentColor = SystemColors.ObsidianBlack
                                    )
                                ) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("ACCEPT")
                                }
                                OutlinedButton(
                                    onClick = {
                                        rejectNote = ""
                                        rejectingRec = rec
                                    },
                                    modifier = Modifier.weight(1f).testTag("reject_rec_${rec.recUuid}")
                                ) {
                                    Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("REJECT")
                                }
                            } else if (rec.status == RecommendationStatus.ACCEPTED) {
                                Button(
                                    onClick = { onRespond(rec.recUuid, RecommendationStatus.COMPLETED, "") },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("MARK DONE")
                                }
                            }
                            OutlinedButton(
                                onClick = { onNavigateDeepLink(rec.deepLinkRoute) },
                                border = BorderStroke(1.dp, SystemColors.ElectricCyan)
                            ) {
                                Icon(Icons.Rounded.OpenInNew, contentDescription = rec.actionLabel, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(rec.actionLabel.uppercase(), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }

    val targetReject = rejectingRec
    if (targetReject != null) {
        AlertDialog(
            onDismissRequest = { rejectingRec = null },
            containerColor = SystemColors.DeepNavy,
            title = { Text("Dismiss Recommendation", color = SystemColors.PureIce) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(targetReject.title, style = MaterialTheme.typography.bodySmall, color = SystemColors.ElectricCyan)
                    OutlinedTextField(
                        value = rejectNote,
                        onValueChange = { rejectNote = it },
                        label = { Text("Optional feedback reason (helps adapt future weighting)") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onRespond(targetReject.recUuid, RecommendationStatus.REJECTED, rejectNote)
                    rejectingRec = null
                }) { Text("REJECT") }
            },
            dismissButton = {
                TextButton(onClick = { rejectingRec = null }) { Text("CANCEL") }
            }
        )
    }
}

// SCREEN 8: ACADEMIC LEARNING INTELLIGENCE
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AcademicIntelligenceScreen(
    uiState: IntelligenceUiState,
    onRefresh: () -> Unit
) {
    IntelligenceStateContainer(uiState = uiState, onRetry = onRefresh) { state ->
        val acad = state.domainInsights.academic
        item {
            HolographicPanel(accentColor = SystemColors.ElectricCyan) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SystemSectionTitle("Academic Intelligence Telemetry")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ProvenanceBadgeChip(DataProvenanceTag.MEASURED)
                        ProvenanceBadgeChip(DataProvenanceTag.SELF_REPORTED)
                        ProvenanceBadgeChip(DataProvenanceTag.ESTIMATE)
                    }
                    MetricLine("Measured Study (7d)", "${acad.totalStudyMinutes7d} min (${acad.verifiedStudyRatioPercent}% timer-verified)")
                    MetricLine("Measured Assessments Avg", acad.measuredAssessmentAvgPercent?.let { "$it%" } ?: "None recorded")
                    MetricLine("Self-Reported Topic Confidence", acad.selfReportedConfidenceAvg?.let { "%.1f / 5.0".format(it) } ?: "N/A")
                    MetricLine("Estimated Spaced Familiarity", "${acad.estimatedRetentionAvgPercent}% (heuristic estimate)")
                    MetricLine("Overdue Revisions / 7d Reviews", "${acad.overdueRevisionsCount} overdue / ${acad.totalRevisions7d} done")
                }
            }
        }
        if (acad.examCountdowns.isNotEmpty()) {
            item {
                HolographicPanel(accentColor = SystemColors.WarningAmber) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SystemSectionTitle("Exam Countdowns")
                        acad.examCountdowns.forEach {
                            Text("• $it", style = MaterialTheme.typography.bodySmall, color = SystemColors.PureIce)
                        }
                    }
                }
            }
        }
        if (acad.recommendedRevisionPriorities.isNotEmpty()) {
            item {
                HolographicPanel(accentColor = SystemColors.ArcaneViolet) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SystemSectionTitle("Top Revision Priorities")
                        acad.recommendedRevisionPriorities.forEach {
                            Text("• $it", style = MaterialTheme.typography.bodySmall, color = SystemColors.PureIce)
                        }
                    }
                }
            }
        }
        items(acad.subjectInsights, key = { it.subjectId }) { sub ->
            HolographicPanel(accentColor = SystemColors.SystemBlue) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(sub.subjectName, style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
                    MetricLine("Measured Study", "${sub.measuredStudyMinutes} min")
                    MetricLine("Assessment Avg (Measured)", sub.assessmentAveragePercent?.let { "$it%" } ?: "N/A")
                    MetricLine("Estimated Familiarity", "${sub.estimatedFamiliarityPercent}% (${sub.overdueRevisionsCount} overdue)")
                    Text(sub.gapSummary, style = MaterialTheme.typography.bodySmall, color = SystemColors.ElectricCyan)
                }
            }
        }
        item {
            HolographicPanel(accentColor = SystemColors.MutedSilver) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SystemSectionTitle("Model & Telemetry Limitations")
                    acad.limitations.forEach {
                        Text("• $it", style = MaterialTheme.typography.labelSmall, color = SystemColors.MutedSilver)
                    }
                }
            }
        }
    }
}

// SCREEN 9: PHYSICAL DEVELOPMENT INTELLIGENCE
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PhysicalIntelligenceScreen(
    uiState: IntelligenceUiState,
    onRefresh: () -> Unit
) {
    IntelligenceStateContainer(uiState = uiState, onRetry = onRefresh) { state ->
        val phys = state.domainInsights.physical
        item {
            HolographicPanel(accentColor = SystemColors.WarningAmber) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.HealthAndSafety,
                        contentDescription = "Non-medical guidance disclaimer",
                        tint = SystemColors.WarningAmber,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = phys.safetyDisclaimer,
                        style = MaterialTheme.typography.bodySmall,
                        color = SystemColors.PureIce
                    )
                }
            }
        }
        item {
            HolographicPanel(accentColor = if (phys.restEnforcementActive) SystemColors.DangerCrimson else SystemColors.VerifiedEmerald) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SystemSectionTitle("Training Load & Readiness")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        IntelligencePill("STATE: ${phys.recoveryState.code}", if (phys.restEnforcementActive) SystemColors.DangerCrimson else SystemColors.VerifiedEmerald)
                        IntelligencePill("READINESS: ${phys.readinessScore}/100", SystemColors.ElectricCyan)
                        if (phys.restEnforcementActive) {
                            IntelligencePill("HIGH-STRAIN PROPOSALS LOCKED", SystemColors.DangerCrimson)
                        }
                    }
                    Text(phys.trainingTrendLabel, style = MaterialTheme.typography.titleSmall, color = SystemColors.PureIce)
                    Text(phys.intensitySuggestion, style = MaterialTheme.typography.bodySmall, color = SystemColors.MetallicSilver)
                    MetricLine("48h Physical Load", "${phys.measuredLoadMinutes48h} min")
                    MetricLine("7d Physical Load", "${phys.measuredLoadMinutes7d} min")
                    MetricLine("7d Jogging", "${phys.weeklyJoggingMinutes} min (${phys.weeklyDistanceKm} km)")
                    MetricLine("7d Structured Workouts", "${phys.weeklyWorkoutCount}")
                    MetricLine("Avg Heart Rate", phys.avgHeartRateBpm?.let { "$it bpm" } ?: "Unavailable")
                    MetricLine("Last Night Sleep", phys.measuredSleepHoursLastNight?.let { "%.1f h".format(it) } ?: "Unavailable")
                }
            }
        }
        if (phys.dataLimitations.isNotEmpty()) {
            item {
                HolographicPanel(accentColor = SystemColors.MutedSilver) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SystemSectionTitle("Sensor & Recovery Limitations")
                        phys.dataLimitations.forEach {
                            Text("• $it", style = MaterialTheme.typography.labelSmall, color = SystemColors.MutedSilver)
                        }
                    }
                }
            }
        }
    }
}

// SCREEN 10: ADAPTIVE DUNGEONS
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdaptiveDungeonsScreen(
    uiState: IntelligenceUiState,
    onRefresh: () -> Unit,
    onOpenDungeonDetails: (Long) -> Unit
) {
    IntelligenceStateContainer(uiState = uiState, onRetry = onRefresh) { state ->
        val items = state.domainInsights.adaptiveDungeons
        if (items.isEmpty()) {
            item {
                SystemEmptyState(
                    title = "No Adaptive Dungeon Recommendations",
                    description = "Enable Adaptive Dungeons in Intelligence Preferences or refresh diagnostics."
                )
            }
        } else {
            items(items, key = { it.dungeonId }) { d ->
                val accent = when {
                    !d.isEligible -> SystemColors.MutedSilver
                    !d.recoverySafe -> SystemColors.DangerCrimson
                    d.suitabilityScore >= 80 -> SystemColors.VerifiedEmerald
                    else -> SystemColors.ElectricCyan
                }
                HolographicPanel(accentColor = accent) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${d.code} • ${d.title}", style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce, modifier = Modifier.weight(1f))
                            IntelligencePill("MATCH ${d.suitabilityScore}%", accent)
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            IntelligencePill(d.recommendationLabel.uppercase(), accent)
                            IntelligencePill(d.difficulty.name, SystemColors.ArcaneViolet)
                            if (!d.recoverySafe) IntelligencePill("RECOVERY LOCKED", SystemColors.DangerCrimson)
                        }
                        Text(d.rationale, style = MaterialTheme.typography.bodySmall, color = SystemColors.MetallicSilver)
                        d.completionRateHistoryPercent?.let {
                            Text("Historical Dungeon Completion Rate: $it%", style = MaterialTheme.typography.labelSmall, color = SystemColors.ElectricCyan)
                        }
                        OutlinedButton(
                            onClick = { onOpenDungeonDetails(d.dungeonId) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(if (d.isEligible) Icons.Rounded.Security else Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (d.isEligible) "INSPECT GATE BRIEFING" else "VIEW LOCK REQUIREMENTS")
                        }
                    }
                }
            }
        }
    }
}
