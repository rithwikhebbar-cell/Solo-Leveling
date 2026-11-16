package com.example.ui.screens.intelligence

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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Balance
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.AdaptivePlanBlock
import com.example.domain.model.AdaptivePlanBlockStatus
import com.example.domain.model.ConfidenceLevel
import com.example.domain.model.DataProvenanceTag
import com.example.domain.model.FindingKind
import com.example.domain.model.WorkloadLevel
import com.example.ui.components.GlowingProgressBar
import com.example.ui.components.HolographicPanel
import com.example.ui.components.SystemEmptyState
import com.example.ui.components.SystemErrorState
import com.example.ui.components.SystemLoadingView
import com.example.ui.components.SystemSectionTitle
import com.example.ui.theme.SystemColors
import com.example.ui.viewmodel.IntelligenceUiState

@Composable
internal fun IntelligenceStateContainer(
    uiState: IntelligenceUiState,
    onRetry: () -> Unit,
    onClearFeedback: () -> Unit = {},
    content: LazyListScope.(IntelligenceUiState.Ready) -> Unit
) {
    when (uiState) {
        is IntelligenceUiState.Loading -> SystemLoadingView()
        is IntelligenceUiState.Error -> Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SystemColors.ObsidianBlack)
                .padding(20.dp)
        ) {
            SystemErrorState(message = uiState.message, onRetry = onRetry)
        }
        is IntelligenceUiState.Ready -> Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SystemColors.ObsidianBlack),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 760.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (uiState.isBusy) {
                    item {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = SystemColors.ElectricCyan,
                            trackColor = SystemColors.PanelSurface
                        )
                    }
                }
                val msg = uiState.feedbackMessage
                if (!msg.isNullOrBlank()) {
                    item {
                        HolographicPanel(accentColor = SystemColors.ElectricCyan) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SystemColors.PureIce,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = onClearFeedback,
                                    modifier = Modifier.testTag("dismiss_intelligence_feedback")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Dismiss notification",
                                        tint = SystemColors.ElectricCyan
                                    )
                                }
                            }
                        }
                    }
                }
                content(uiState)
            }
        }
    }
}

@Composable
internal fun ProvenanceBadgeChip(tag: DataProvenanceTag) {
    val color = when (tag) {
        DataProvenanceTag.MEASURED -> SystemColors.VerifiedEmerald
        DataProvenanceTag.SELF_REPORTED -> SystemColors.SystemBlue
        DataProvenanceTag.ESTIMATE -> SystemColors.WarningAmber
        DataProvenanceTag.PREDICTION -> SystemColors.ArcaneViolet
    }
    IntelligencePill(label = tag.label.uppercase(), color = color)
}

@Composable
internal fun ConfidenceBadgeChip(confidence: ConfidenceLevel) {
    val color = when (confidence) {
        ConfidenceLevel.HIGH -> SystemColors.VerifiedEmerald
        ConfidenceLevel.MODERATE -> SystemColors.ElectricCyan
        ConfidenceLevel.LOW -> SystemColors.WarningAmber
        ConfidenceLevel.INSUFFICIENT_DATA -> SystemColors.DangerCrimson
    }
    IntelligencePill(label = confidence.label.uppercase(), color = color)
}

@Composable
internal fun IntelligencePill(label: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(0.75.dp, color.copy(alpha = 0.55f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

// SCREEN 1: INTELLIGENCE DASHBOARD
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IntelligenceDashboardScreen(
    uiState: IntelligenceUiState,
    onRefresh: () -> Unit,
    onClearFeedback: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenStrengths: () -> Unit,
    onOpenDailyPlan: () -> Unit,
    onOpenWorkload: () -> Unit,
    onOpenConsistency: () -> Unit,
    onOpenRecommendations: () -> Unit,
    onOpenAcademicIntel: () -> Unit,
    onOpenPhysicalIntel: () -> Unit,
    onOpenAdaptiveDungeons: () -> Unit,
    onOpenRoadmap: () -> Unit,
    onOpenWeeklyReport: () -> Unit,
    onOpenMonthlyReport: () -> Unit,
    onOpenAdvisor: () -> Unit,
    onOpenPreferences: () -> Unit
) {
    IntelligenceStateContainer(uiState = uiState, onRetry = onRefresh, onClearFeedback = onClearFeedback) { state ->
        val profile = state.profile
        val workload = state.workload

        item {
            HolographicPanel(
                accentColor = SystemColors.ElectricCyan,
                secondaryColor = SystemColors.ArcaneViolet
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ADAPTIVE INTELLIGENCE CORE",
                                style = MaterialTheme.typography.labelSmall,
                                color = SystemColors.ElectricCyan
                            )
                            Text(
                                text = if (profile != null) {
                                    "Lv.${profile.playerLevel} ${profile.hunterRank.displayName}"
                                } else {
                                    "Initializing Baseline Profile"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                color = SystemColors.PureIce
                            )
                        }
                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier.testTag("refresh_intelligence_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Refresh Intelligence Diagnostics",
                                tint = SystemColors.ElectricCyan
                            )
                        }
                    }

                    if (profile != null) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            IntelligencePill("MEASURED ${profile.measuredDataRatioPercent}%", SystemColors.VerifiedEmerald)
                            IntelligencePill("ACTIVE ${profile.activeDaysLast30}D / 30D", SystemColors.SystemBlue)
                            if (!profile.hasMinimumBaseline) {
                                IntelligencePill("PRELIMINARY BASELINE (<3D)", SystemColors.WarningAmber)
                            }
                        }
                        Text(
                            text = "Peak Window: ${profile.behavioralPeakWindow} • Avg Session: ${profile.averageSessionMinutes} min",
                            style = MaterialTheme.typography.bodySmall,
                            color = SystemColors.MetallicSilver
                        )
                    }
                    if (workload != null) {
                        val wlColor = workloadColor(workload.workloadLevel)
                        Surface(
                            color = wlColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(0.75.dp, wlColor.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "WORKLOAD: ${workload.workloadLevel.label.uppercase()} (${workload.loadScore}/100)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = wlColor
                                )
                                Text(
                                    text = workload.adjustmentDirective,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SystemColors.PureIce
                                )
                            }
                        }
                    }
                    Text(
                        text = "Advisory Only: Progression, XP, and Rank remain strictly governed by verified activity records.",
                        style = MaterialTheme.typography.labelSmall,
                        color = SystemColors.MutedSilver
                    )
                }
            }
        }

        if (profile != null) {
            item {
                HolographicPanel(accentColor = SystemColors.ArcaneViolet) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SystemSectionTitle("Domain Telemetry Matrix")
                        ScoreRow("Physical Conditioning", profile.physicalActivityScore, SystemColors.VerifiedEmerald)
                        ScoreRow("Academic Mastery", profile.academicActivityScore, SystemColors.ElectricCyan)
                        ScoreRow("Discipline Consistency", profile.disciplineConsistencyScore, SystemColors.MonarchGold)
                        ScoreRow("RPG Progression", profile.rpgEngagementScore, SystemColors.ArcaneViolet)
                    }
                }
            }
        }

        item {
            SystemSectionTitle(
                title = "Intelligence Subsystems",
                subtitle = "14 dedicated advisory, planning, and analytics views"
            )
        }

        val tiles = listOf(
            Triple("Player Profile", "Scores, peak window & coverage", Icons.Rounded.Insights to onOpenProfile),
            Triple("Strengths & Risks", "${state.findings.size} grounded findings", Icons.Rounded.Warning to onOpenStrengths),
            Triple("Adaptive Daily Plan", "${state.latestPlan?.blocks?.size ?: 0} planned blocks", Icons.Rounded.EventNote to onOpenDailyPlan),
            Triple("Workload Balance", state.workload?.workloadLevel?.label ?: "Assess load", Icons.Rounded.Balance to onOpenWorkload),
            Triple("Consistency Forecast", "Trend slopes & risk goals", Icons.Rounded.Timeline to onOpenConsistency),
            Triple("Recommendations", "${state.recommendations.count { it.status.name == "NEW" }} new actions", Icons.Rounded.Lightbulb to onOpenRecommendations),
            Triple("Academic Intel", "Study vs recall gap", Icons.Rounded.School to onOpenAcademicIntel),
            Triple("Physical Intel", "Recovery-guarded guidance", Icons.Rounded.FitnessCenter to onOpenPhysicalIntel),
            Triple("Adaptive Dungeons", "${state.domainInsights.adaptiveDungeons.size} ranked gates", Icons.Rounded.Security to onOpenAdaptiveDungeons),
            Triple("Dev Roadmap", "${state.roadmaps.size} saved roadmaps", Icons.Rounded.CalendarMonth to onOpenRoadmap),
            Triple("Weekly Report", "7-day synthesis & review", Icons.Rounded.VerifiedUser to onOpenWeeklyReport),
            Triple("Monthly Report", "30-day trend synthesis", Icons.Rounded.CalendarMonth to onOpenMonthlyReport),
            Triple("AI Advisor Q&A", "Consent-gated coaching", Icons.Rounded.Psychology to onOpenAdvisor),
            Triple("Intel Preferences", "Bounds, rest days & privacy", Icons.Rounded.Tune to onOpenPreferences)
        )

        items(tiles.chunked(2)) { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                pair.forEach { (title, subtitle, iconAndClick) ->
                    HolographicPanel(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = iconAndClick.second)
                            .testTag("intel_tile_${title.lowercase().replace(" ", "_")}"),
                        accentColor = SystemColors.ElectricCyan,
                        contentPadding = 12.dp
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = iconAndClick.first,
                                contentDescription = title,
                                tint = SystemColors.ElectricCyan,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall,
                                color = SystemColors.PureIce
                            )
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = SystemColors.MutedSilver
                            )
                        }
                    }
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ScoreRow(label: String, score: Int, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = SystemColors.PureIce)
            Text("$score / 100", style = MaterialTheme.typography.labelMedium, color = color)
        }
        GlowingProgressBar(
            progress = (score / 100f).coerceIn(0f, 1f),
            height = 7.dp,
            startColor = color,
            endColor = SystemColors.ElectricCyan
        )
    }
}

// SCREEN 2: PLAYER INTELLIGENCE PROFILE
@Composable
fun PlayerIntelligenceProfileScreen(
    uiState: IntelligenceUiState,
    onRefresh: () -> Unit
) {
    IntelligenceStateContainer(uiState = uiState, onRetry = onRefresh) { state ->
        val profile = state.profile
        if (profile == null) {
            item {
                SystemEmptyState(
                    title = "No Profile Computed Yet",
                    description = "Run Refresh Diagnostics to build your cross-domain intelligence profile."
                )
            }
        } else {
            item {
                HolographicPanel(accentColor = SystemColors.ElectricCyan) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SystemSectionTitle("Behavioral Telemetry Profile")
                        Text(
                            text = "Hunter Rank: ${profile.hunterRank.displayName} • Level ${profile.playerLevel}",
                            style = MaterialTheme.typography.titleMedium,
                            color = SystemColors.PureIce
                        )
                        Text(
                            text = "Behavioral Peak Window: ${profile.behavioralPeakWindow}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SystemColors.ElectricCyan
                        )
                        Text(
                            text = "Average Session Duration: ${profile.averageSessionMinutes} min • Active Days (30d): ${profile.activeDaysLast30}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SystemColors.MetallicSilver
                        )
                        ScoreRow("Measured Telemetry Ratio", profile.measuredDataRatioPercent, SystemColors.VerifiedEmerald)
                        ScoreRow("Data Completeness", profile.dataCompletenessPercent, SystemColors.SystemBlue)
                        Text(
                            text = profile.provenanceSummary,
                            style = MaterialTheme.typography.labelSmall,
                            color = SystemColors.MutedSilver
                        )
                    }
                }
            }
            item {
                HolographicPanel(accentColor = SystemColors.ArcaneViolet) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SystemSectionTitle("Domain Scores (0–100)")
                        ScoreRow("Physical", profile.physicalActivityScore, SystemColors.VerifiedEmerald)
                        ScoreRow("Academic", profile.academicActivityScore, SystemColors.ElectricCyan)
                        ScoreRow("Discipline", profile.disciplineConsistencyScore, SystemColors.MonarchGold)
                        ScoreRow("RPG Engagement", profile.rpgEngagementScore, SystemColors.ArcaneViolet)
                    }
                }
            }
            item {
                HolographicPanel(accentColor = SystemColors.WarningAmber) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SystemSectionTitle("Data Coverage & Limitations")
                        if (profile.dataCoverageNotes.isEmpty()) {
                            Text(
                                text = "Baseline threshold met with active telemetry across core modules.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SystemColors.VerifiedEmerald
                            )
                        } else {
                            profile.dataCoverageNotes.forEach { note ->
                                Text("• $note", style = MaterialTheme.typography.bodySmall, color = SystemColors.MetallicSilver)
                            }
                        }
                    }
                }
            }
        }
    }
}

// SCREEN 3: STRENGTHS & WEAKNESSES
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StrengthsWeaknessesScreen(
    uiState: IntelligenceUiState,
    onRefresh: () -> Unit
) {
    var selectedKind by rememberSaveable { mutableStateOf<FindingKind?>(null) }

    IntelligenceStateContainer(uiState = uiState, onRetry = onRefresh) { state ->
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedKind == null,
                    onClick = { selectedKind = null },
                    label = { Text("All (${state.findings.size})") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SystemColors.ElectricCyan.copy(alpha = 0.2f))
                )
                FindingKind.entries.forEach { kind ->
                    FilterChip(
                        selected = selectedKind == kind,
                        onClick = { selectedKind = kind },
                        label = { Text(kind.label) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SystemColors.ElectricCyan.copy(alpha = 0.2f))
                    )
                }
            }
        }

        val visible = state.findings.filter { selectedKind == null || it.kind == selectedKind }
        if (visible.isEmpty()) {
            item {
                SystemEmptyState(
                    title = "No Findings For Filter",
                    description = "Adjust your filter or log more sessions to inspect domain patterns."
                )
            }
        } else {
            items(visible, key = { it.findingUuid }) { finding ->
                val accent = when (finding.kind) {
                    FindingKind.STRENGTH -> SystemColors.VerifiedEmerald
                    FindingKind.WEAKNESS -> SystemColors.WarningAmber
                    FindingKind.BOTTLENECK -> SystemColors.ArcaneViolet
                    FindingKind.RISK -> SystemColors.DangerCrimson
                }
                HolographicPanel(accentColor = accent) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            IntelligencePill("${finding.kind.label.uppercase()} • ${finding.category.label}", accent)
                            ProvenanceBadgeChip(finding.provenance)
                            ConfidenceBadgeChip(finding.confidence)
                        }
                        Text(
                            text = finding.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = SystemColors.PureIce
                        )
                        Text(
                            text = finding.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = SystemColors.MetallicSilver
                        )
                        Text(
                            text = "Evidence (${finding.dateRangeLabel}): ${finding.evidenceSummary}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SystemColors.ElectricCyan
                        )
                    }
                }
            }
        }
    }
}

// SCREEN 4: ADAPTIVE DAILY PLAN
@Composable
fun AdaptiveDailyPlanScreen(
    uiState: IntelligenceUiState,
    onClearFeedback: () -> Unit,
    onGeneratePlan: (Boolean) -> Unit,
    onApprovePlan: (Long) -> Unit,
    onUpdateBlockProgress: (Long, Int) -> Unit,
    onRescheduleBlock: (Long, Int) -> Unit
) {
    var rescheduleBlock by remember { mutableStateOf<AdaptivePlanBlock?>(null) }
    var rescheduleHourText by rememberSaveable { mutableStateOf("16") }

    IntelligenceStateContainer(uiState = uiState, onRetry = { onGeneratePlan(false) }, onClearFeedback = onClearFeedback) { state ->
        val plan = state.latestPlan
        item {
            HolographicPanel(accentColor = SystemColors.ElectricCyan) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SystemSectionTitle(
                        title = "Adaptive Daily Plan",
                        subtitle = "Conflict-free, recovery-guarded schedule proposal"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onGeneratePlan(false) },
                            modifier = Modifier.weight(1f).testTag("generate_full_plan_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SystemColors.ElectricCyan,
                                contentColor = SystemColors.ObsidianBlack
                            )
                        ) {
                            Text("FULL DAY DRAFT")
                        }
                        OutlinedButton(
                            onClick = { onGeneratePlan(true) },
                            modifier = Modifier.weight(1f).testTag("generate_partial_plan_button"),
                            border = BorderStroke(1.dp, SystemColors.ArcaneViolet)
                        ) {
                            Text("PARTIAL DAY", color = SystemColors.ArcaneViolet)
                        }
                    }
                    if (plan != null) {
                        Text(
                            text = "Workload: ${plan.workloadLevel.label} • Planned: ${plan.totalPlannedMinutes}m • Remaining Free Window: ${plan.availableMinutes}m",
                            style = MaterialTheme.typography.bodySmall,
                            color = SystemColors.PureIce
                        )
                        Text(
                            text = plan.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = SystemColors.MetallicSilver
                        )
                        if (!plan.isApproved && plan.blocks.isNotEmpty()) {
                            Button(
                                onClick = { onApprovePlan(plan.id) },
                                modifier = Modifier.fillMaxWidth().testTag("approve_adaptive_plan_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SystemColors.VerifiedEmerald,
                                    contentColor = SystemColors.ObsidianBlack
                                )
                            ) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("APPROVE & SYNC TO CALENDAR")
                            }
                        } else if (plan.isApproved) {
                            IntelligencePill("APPROVED & SYNCED TO CALENDAR", SystemColors.VerifiedEmerald)
                        }
                    }
                }
            }
        }

        if (plan == null || plan.blocks.isEmpty()) {
            item {
                SystemEmptyState(
                    title = "No Active Plan Draft",
                    description = "Tap Full Day Draft or Partial Day to compute conflict-free blocks around your schedule."
                )
            }
        } else {
            items(plan.blocks, key = { it.blockUuid }) { block ->
                val accent = if (block.isRecoveryProtected) SystemColors.VerifiedEmerald else SystemColors.ElectricCyan
                HolographicPanel(accentColor = accent) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${block.formattedWindow()} (${block.durationMinutes}m)",
                                style = MaterialTheme.typography.labelLarge,
                                color = accent
                            )
                            IntelligencePill("${block.module} • ${block.status.label.uppercase()}", accent)
                        }
                        Text(
                            text = block.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = SystemColors.PureIce
                        )
                        Text(
                            text = "Priority ${block.priorityScore}/100: ${block.priorityReason}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SystemColors.MetallicSilver
                        )
                        if (block.isRecoveryProtected) {
                            IntelligencePill("RECOVERY SAFETY PROTECTED", SystemColors.VerifiedEmerald)
                        }
                        if (block.completedMinutes > 0) {
                            Text(
                                text = "Logged Block Progress: ${block.completedMinutes}/${block.durationMinutes} min",
                                style = MaterialTheme.typography.labelSmall,
                                color = SystemColors.VerifiedEmerald
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val half = (block.durationMinutes / 2).coerceAtLeast(10)
                                    onUpdateBlockProgress(block.id, half)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("50% DONE", style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = { onUpdateBlockProgress(block.id, block.durationMinutes) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("COMPLETE", style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = {
                                    rescheduleHourText = ((block.startMinuteOfDay / 60 + 1).coerceIn(6, 21)).toString()
                                    rescheduleBlock = block
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.Schedule, contentDescription = "Reschedule", modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("MOVE", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }

    val editing = rescheduleBlock
    if (editing != null) {
        AlertDialog(
            onDismissRequest = { rescheduleBlock = null },
            containerColor = SystemColors.DeepNavy,
            title = { Text("Reschedule Block", color = SystemColors.PureIce) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(editing.title, style = MaterialTheme.typography.bodyMedium, color = SystemColors.ElectricCyan)
                    OutlinedTextField(
                        value = rescheduleHourText,
                        onValueChange = { rescheduleHourText = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("Target Start Hour (6–22)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val hr = rescheduleHourText.toIntOrNull()?.coerceIn(6, 22) ?: 16
                    onRescheduleBlock(editing.id, hr * 60)
                    rescheduleBlock = null
                }) { Text("CONFIRM") }
            },
            dismissButton = {
                TextButton(onClick = { rescheduleBlock = null }) { Text("CANCEL") }
            }
        )
    }
}

// SCREEN 5: WORKLOAD BALANCE
@Composable
fun WorkloadBalanceScreen(
    uiState: IntelligenceUiState,
    onRefresh: () -> Unit
) {
    IntelligenceStateContainer(uiState = uiState, onRetry = onRefresh) { state ->
        val wl = state.workload
        if (wl == null) {
            item {
                SystemEmptyState(
                    title = "No Workload Assessment",
                    description = "Refresh diagnostics to inspect physical and cognitive workload load."
                )
            }
        } else {
            val accent = workloadColor(wl.workloadLevel)
            item {
                HolographicPanel(accentColor = accent) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = wl.workloadLevel.label.uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                                color = accent
                            )
                            ProvenanceBadgeChip(wl.provenance)
                        }
                        ScoreRow("Composite Load Index", wl.loadScore, accent)
                        Text(
                            text = wl.adjustmentDirective,
                            style = MaterialTheme.typography.bodyMedium,
                            color = SystemColors.PureIce
                        )
                        if (wl.preservesRestWithoutPunishment) {
                            IntelligencePill("PENALTY-FREE REST RULE ACTIVE", SystemColors.VerifiedEmerald)
                        }
                    }
                }
            }
            item {
                HolographicPanel(accentColor = SystemColors.ElectricCyan) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SystemSectionTitle("Load Breakdown")
                        MetricLine("48h Measured Physical Load", "${wl.physicalLoadMinutes48h} min")
                        MetricLine("Today Academic Study", "${wl.academicLoadMinutesToday} min")
                        MetricLine("Active Daily Commitments", "${wl.activeCommitmentsCount}")
                        MetricLine("Overdue Commitments", "${wl.overdueItemsCount}")
                        MetricLine("Schedule Overlaps Detected", "${wl.scheduleConflictCount}")
                        MetricLine("Recovery Readiness State", wl.recoveryState.code)
                    }
                }
            }
        }
    }
}

@Composable
internal fun MetricLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = SystemColors.MutedSilver)
        Text(value, style = MaterialTheme.typography.labelLarge, color = SystemColors.PureIce, fontWeight = FontWeight.SemiBold)
    }
}

private fun workloadColor(level: WorkloadLevel): Color = when (level) {
    WorkloadLevel.UNDERLOADED -> SystemColors.SystemBlue
    WorkloadLevel.BALANCED -> SystemColors.VerifiedEmerald
    WorkloadLevel.HIGH_WORKLOAD -> SystemColors.WarningAmber
    WorkloadLevel.OVERLOADED -> SystemColors.DangerCrimson
    WorkloadLevel.RECOVERY_PRIORITY -> SystemColors.ArcaneViolet
}
