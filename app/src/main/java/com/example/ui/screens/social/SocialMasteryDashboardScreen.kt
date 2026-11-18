package com.example.ui.screens.social

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AdaptiveDifficulty
import com.example.domain.model.MasteryDevelopmentStage
import com.example.domain.model.MasteryEvidenceClassification
import com.example.domain.model.PersonalizedSocialRecommendation
import com.example.domain.model.PersonalizedWeeklyTrainingPlan
import com.example.domain.model.SocialCompetencyProgress
import com.example.domain.model.SocialMasteryPathway
import com.example.domain.model.SocialMasteryStage
import com.example.domain.model.SocialModuleProgress
import com.example.domain.model.SocialRecommendationCategory
import com.example.domain.model.SocialRecommendationStatus
import com.example.domain.model.SocialWeeklyDevelopmentReport
import com.example.ui.viewmodel.SocialMasteryViewModel

// Visual Design Tokens (Aligned with Social Suite Dark-Fantasy Palette)
private val ObsidianBlack = Color(0xFF070A13)
private val DeepNavy = Color(0xFF0D1424)
private val SurfaceNavy = Color(0xFF1E293B)
private val ElectricCyan = Color(0xFF00E5FF)
private val ArcaneViolet = Color(0xFF8B5CF6)
private val BorderCyan = Color(0x3300E5FF)
private val TextWhite = Color(0xFFF8FAFC)
private val TextMuted = Color(0xFF94A3B8)
private val GoldHighlight = Color(0xFFFFD700)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialMasteryDashboardScreen(
    viewModel: SocialMasteryViewModel,
    onNavigateBack: () -> Unit,
    onOpenPracticeArena: () -> Unit = {},
    onOpenCoach: () -> Unit = {},
    onOpenAnalytics: () -> Unit = {},
    onOpenMissions: () -> Unit = {},
    onOpenAssessment: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    var showWeeklyReportDialog by remember { mutableStateOf(false) }

    if (showWeeklyReportDialog) {
        SocialWeeklyReportDialog(
            report = state.weeklyDevelopmentReport,
            onDismiss = { showWeeklyReportDialog = false },
            onAskAi = { report -> viewModel.requestAiWeeklyReview(report) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "SOCIAL MASTERY",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                color = ElectricCyan
                            )
                        )
                        Text(
                            text = "INTERPERSONAL MONARCH PROGRESSION",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextMuted,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ElectricCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ObsidianBlack,
                    titleContentColor = TextWhite
                )
            )
        },
        containerColor = ObsidianBlack
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ElectricCyan)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    SocialSummaryHeroCard(
                        overallStage = state.profile?.overallSocialMasteryStage ?: SocialMasteryStage.NOVICE,
                        totalPracticeMinutes = state.profile?.totalPracticeMinutes ?: 0,
                        scenariosCleared = state.profile?.totalScenariosCleared ?: 0,
                        assessmentsCompleted = state.profile?.totalAssessmentsCompleted ?: 0
                    )
                }

                item {
                    androidx.compose.material3.Button(
                        onClick = onOpenPracticeArena,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("social_open_practice_arena_button"),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "COMMUNICATION PRACTICE ARENA",
                            color = ObsidianBlack,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        androidx.compose.material3.Button(
                            onClick = onOpenMissions,
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("social_open_missions_button"),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = DeepNavy),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderCyan),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("REAL-WORLD MISSIONS", color = ElectricCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        androidx.compose.material3.Button(
                            onClick = onOpenAssessment,
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("social_open_assessment_button"),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = DeepNavy),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ArcaneViolet),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("SKILL ASSESSMENT", color = ArcaneViolet, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        androidx.compose.material3.Button(
                            onClick = onOpenCoach,
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("social_open_coach_button"),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = ArcaneViolet),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("AI VOICE COACH", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        androidx.compose.material3.Button(
                            onClick = onOpenAnalytics,
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("social_open_analytics_button"),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = DeepNavy),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderCyan),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("ANALYTICS RADAR", color = ElectricCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }

                // Milestone 12 — Phase 5: Weekly Training Plan Section
                item {
                    WeeklyTrainingPlanDashboardCard(
                        plan = state.currentTrainingPlan,
                        onGeneratePlan = { viewModel.generateWeeklyPlan() },
                        onCompleteActivity = { actId -> viewModel.completePlannedActivity(actId) },
                        onSkipActivity = { actId -> viewModel.skipPlannedActivity(actId) },
                        onTogglePause = { planId, isPaused -> viewModel.togglePauseTrainingPlan(planId, isPaused) }
                    )
                }

                // Milestone 12 — Phase 5: Adaptive Recommendations Section
                item {
                    AdaptiveRecommendationsDashboardSection(
                        recommendations = state.activeRecommendations,
                        onRefresh = { viewModel.refreshRecommendations() },
                        onAccept = { recId -> viewModel.acceptRecommendation(recId) },
                        onDismiss = { recId -> viewModel.dismissRecommendation(recId) },
                        onAskAi = { rec -> viewModel.requestAiRecommendationExplanation(rec) }
                    )
                }

                // Milestone 12 — Phase 5.1: Competency Mastery Pathways Section
                item {
                    CompetencyMasteryPathwaysSection(
                        pathways = state.masteryPathways,
                        onOverrideDifficulty = { competencyId, diff ->
                            viewModel.setCompetencyDifficultyOverride(competencyId, diff)
                        }
                    )
                }

                // Milestone 12 — Phase 5: Weekly Development Report Action Button
                item {
                    Button(
                        onClick = {
                            viewModel.loadWeeklyReport()
                            showWeeklyReportDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("social_weekly_report_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepNavy),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ArcaneViolet),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("VIEW WEEKLY DEVELOPMENT REPORT", color = ArcaneViolet, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CORE SOCIAL MODULES (8)",
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = TextWhite,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = "CHA / WIS / DIS / INT",
                            style = MaterialTheme.typography.labelSmall.copy(color = ArcaneViolet)
                        )
                    }
                }

                items(state.moduleProgressList) { moduleProgress ->
                    SocialModuleCard(moduleProgress = moduleProgress)
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun SocialSummaryHeroCard(
    overallStage: SocialMasteryStage,
    totalPracticeMinutes: Int,
    scenariosCleared: Int,
    assessmentsCompleted: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderCyan, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = DeepNavy),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "OVERALL MASTERY RANK",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextMuted,
                            letterSpacing = 1.sp
                        )
                    )
                    Text(
                        text = overallStage.label.uppercase(),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = when (overallStage) {
                                SocialMasteryStage.MASTER -> GoldHighlight
                                SocialMasteryStage.ADVANCED -> ArcaneViolet
                                SocialMasteryStage.PROFICIENT -> ElectricCyan
                                else -> TextWhite
                            },
                            letterSpacing = 1.5.sp
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(ArcaneViolet, ElectricCyan))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = ObsidianBlack,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryMetricItem(label = "Practice Time", value = "${totalPracticeMinutes}m")
                SummaryMetricItem(label = "Scenarios Cleared", value = "$scenariosCleared")
                SummaryMetricItem(label = "Assessments", value = "$assessmentsCompleted")
            }
        }
    }
}

@Composable
fun SummaryMetricItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = ElectricCyan
            )
        )
    }
}

@Composable
fun SocialModuleCard(moduleProgress: SocialModuleProgress) {
    val module = moduleProgress.module
    val isZeroState = moduleProgress.practiceSeconds == 0L && moduleProgress.assessmentsCompleted == 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isZeroState) Color(0x22FFFFFF) else BorderCyan,
                RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isZeroState) Color(0xFF131B2E) else SurfaceNavy
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = module.displayName,
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = TextWhite,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isZeroState) Color(0x3394A3B8) else Color(0x3300E5FF)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = moduleProgress.masteryStage.label.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isZeroState) TextMuted else ElectricCyan,
                            fontSize = 10.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = module.description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Attributes: ",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 11.sp)
                    )
                    Text(
                        text = "${module.primaryAttribute.code} (${(module.primaryRatio * 100).toInt()}%)",
                        style = MaterialTheme.typography.labelSmall.copy(color = ElectricCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    )
                    Text(
                        text = " • ${module.secondaryAttribute.code} (${(module.secondaryRatio * 100).toInt()}%)",
                        style = MaterialTheme.typography.labelSmall.copy(color = ArcaneViolet, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    )
                }

                Text(
                    text = if (isZeroState) "Not Started" else "${moduleProgress.practiceMinutes}m Practice",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isZeroState) Color(0xFF64748B) else GoldHighlight,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

// =========================================================================
// PHASE 5: WEEKLY TRAINING PLAN DASHBOARD CARD
// =========================================================================

@Composable
fun WeeklyTrainingPlanDashboardCard(
    plan: PersonalizedWeeklyTrainingPlan?,
    onGeneratePlan: () -> Unit,
    onCompleteActivity: (String) -> Unit,
    onSkipActivity: (String) -> Unit,
    onTogglePause: (String, Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderCyan, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = DeepNavy),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "WEEKLY TRAINING PLAN",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }

                if (plan != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (plan.isPaused) Color(0xFF64748B) else ElectricCyan.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (plan.isPaused) "PAUSED" else "ACTIVE (${plan.targetDaysPerWeek}D / ${plan.targetMinutesPerDay}M)",
                            color = if (plan.isPaused) TextWhite else ElectricCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (plan == null) {
                Text(
                    text = "No active weekly training schedule. Generate an adaptive plan tailored to your skill progression.",
                    color = TextMuted,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onGeneratePlan,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = ObsidianBlack),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("GENERATE ADAPTIVE PLAN (5 Days, 15m/day)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Text(
                    text = plan.weeklyDevelopmentObjective,
                    color = TextWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Find the next incomplete activity
                val nextActivity = plan.scheduledActivities.firstOrNull { !it.isCompleted && !it.isSkipped }
                if (nextActivity != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceNavy),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "DAY ${nextActivity.dayOfWeek} • ${nextActivity.activityType.label.uppercase()}",
                                    color = ArcaneViolet,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "${nextActivity.durationMinutes} MIN • ${nextActivity.difficulty.label}",
                                    color = ElectricCyan,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(nextActivity.title, color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onCompleteActivity(nextActivity.activityId) },
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = ObsidianBlack),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("MARK COMPLETE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { onSkipActivity(nextActivity.activityId) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("SKIP (NO PENALTY)", fontSize = 10.sp)
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "All scheduled activities completed for this cycle. Outstanding consistency!",
                        color = GoldHighlight,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = { onTogglePause(plan.planId, !plan.isPaused) }) {
                        Text(if (plan.isPaused) "RESUME PLAN" else "PAUSE PLAN", color = TextMuted, fontSize = 11.sp)
                    }
                    TextButton(onClick = onGeneratePlan) {
                        Text("REGENERATE SCHEDULE", color = ElectricCyan, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

// =========================================================================
// PHASE 5: ADAPTIVE RECOMMENDATIONS DASHBOARD SECTION
// =========================================================================

@Composable
fun AdaptiveRecommendationsDashboardSection(
    recommendations: List<PersonalizedSocialRecommendation>,
    onRefresh: () -> Unit,
    onAccept: (String) -> Unit,
    onDismiss: (String) -> Unit,
    onAskAi: (PersonalizedSocialRecommendation) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderCyan, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = DeepNavy),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = GoldHighlight, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ADAPTIVE RECOMMENDATIONS (${recommendations.size})",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = TextWhite,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }

                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("adaptive_recommendations_refresh_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh adaptive recommendations",
                        tint = ElectricCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (recommendations.isEmpty()) {
                Text(
                    text = "No active recommendations. Click refresh to evaluate historical sessions and generate targeted micro-drills.",
                    color = TextMuted,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onRefresh,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricCyan),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("adaptive_recommendations_generate_button")
                ) {
                    Text("EVALUATE & GENERATE RECOMMENDATIONS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                recommendations.take(3).forEach { rec ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceNavy),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("recommendation_card_${rec.recommendationId}")
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = rec.category.label.uppercase(),
                                    color = when (rec.category) {
                                        SocialRecommendationCategory.IMPROVE_WEAKNESS -> GoldHighlight
                                        SocialRecommendationCategory.REAL_WORLD_APPLICATION -> ElectricCyan
                                        SocialRecommendationCategory.REVISIT_INACTIVE_SKILL -> ArcaneViolet
                                        else -> TextWhite
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                if (rec.status == SocialRecommendationStatus.ACCEPTED) {
                                                    ElectricCyan.copy(alpha = 0.22f)
                                                } else {
                                                    ArcaneViolet.copy(alpha = 0.22f)
                                                }
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = rec.status.name,
                                            color = if (rec.status == SocialRecommendationStatus.ACCEPTED) ElectricCyan else ArcaneViolet,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Text(
                                        text = "${rec.estimatedDurationMinutes}M • ${rec.difficulty.label}",
                                        color = TextMuted,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(rec.title, color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(rec.explanation, color = TextMuted, fontSize = 11.sp)

                            if (rec.supportingEvidenceReferences.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                rec.supportingEvidenceReferences.forEach { evidence ->
                                    Text(
                                        text = "• $evidence",
                                        color = ElectricCyan.copy(alpha = 0.85f),
                                        fontSize = 10.sp,
                                        lineHeight = 14.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = { onAccept(rec.recommendationId) },
                                    enabled = rec.status != SocialRecommendationStatus.ACCEPTED,
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = ObsidianBlack),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag("accept_rec_button_${rec.recommendationId}")
                                ) {
                                    Text(
                                        text = if (rec.status == SocialRecommendationStatus.ACCEPTED) "ACCEPTED" else "ACCEPT",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                OutlinedButton(
                                    onClick = { onAskAi(rec) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ArcaneViolet),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag("ask_ai_rec_button_${rec.recommendationId}")
                                ) {
                                    Text("ASK AI WHY", fontSize = 10.sp)
                                }
                                TextButton(
                                    onClick = { onDismiss(rec.recommendationId) },
                                    modifier = Modifier
                                        .weight(0.75f)
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag("dismiss_rec_button_${rec.recommendationId}")
                                ) {
                                    Text("DISMISS", color = TextMuted, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// PHASE 5.1: COMPETENCY MASTERY PATHWAYS DASHBOARD SECTION
// =========================================================================

@Composable
fun CompetencyMasteryPathwaysSection(
    pathways: List<SocialMasteryPathway>,
    onOverrideDifficulty: (String, AdaptiveDifficulty) -> Unit
) {
    var expandedModuleCode by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderCyan, RoundedCornerShape(14.dp))
            .testTag("competency_mastery_pathways_section"),
        colors = CardDefaults.cardColors(containerColor = DeepNavy),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "COMPETENCY MASTERY PATHWAYS",
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                )
                Text(
                    text = "${pathways.size} MODULES",
                    color = ElectricCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (pathways.isEmpty()) {
                Text(
                    text = "Evaluate sessions above to populate your 56 competency progression pathways and difficulty tuning.",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            } else {
                pathways.forEach { pathway ->
                    val isExpanded = expandedModuleCode == pathway.module.code
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceNavy),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .border(
                                1.dp,
                                if (pathway.maintenanceRequiredCount > 0) GoldHighlight.copy(alpha = 0.45f) else BorderCyan,
                                RoundedCornerShape(10.dp)
                            )
                            .testTag("pathway_module_card_${pathway.module.code}")
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 48.dp)
                                    .clickable {
                                        expandedModuleCode = if (isExpanded) null else pathway.module.code
                                    }
                                    .testTag("pathway_module_toggle_${pathway.module.code}"),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = pathway.module.displayName,
                                        color = TextWhite,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(ElectricCyan.copy(alpha = 0.18f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = pathway.currentDevelopmentStage.label.uppercase(),
                                                color = ElectricCyan,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        if (pathway.maintenanceRequiredCount > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(GoldHighlight.copy(alpha = 0.2f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "MAINTENANCE DUE (${pathway.maintenanceRequiredCount})",
                                                    color = GoldHighlight,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isExpanded) "Collapse ${pathway.module.displayName} competencies" else "Expand ${pathway.module.displayName} competencies",
                                    tint = ElectricCyan
                                )
                            }

                            pathway.maintenanceSummary?.let { summary ->
                                Text(
                                    text = summary,
                                    color = GoldHighlight.copy(alpha = 0.9f),
                                    fontSize = 10.sp
                                )
                            }

                            AnimatedVisibility(visible = isExpanded) {
                                Column(
                                    modifier = Modifier.padding(top = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    pathway.competencies.forEach { comp ->
                                        CompetencyPathwayDetailRow(
                                            comp = comp,
                                            onOverrideDifficulty = onOverrideDifficulty
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
}

@Composable
private fun CompetencyPathwayDetailRow(
    comp: SocialCompetencyProgress,
    onOverrideDifficulty: (String, AdaptiveDifficulty) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(ObsidianBlack.copy(alpha = 0.55f))
            .padding(10.dp)
            .testTag("competency_row_${comp.competencyId}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = comp.competencyName,
                color = TextWhite,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = comp.masteryStage.label.uppercase(),
                color = when (comp.masteryStage) {
                    MasteryDevelopmentStage.MASTERY_CANDIDATE,
                    MasteryDevelopmentStage.ADVANCED -> GoldHighlight
                    MasteryDevelopmentStage.COMPETENT -> ElectricCyan
                    else -> TextMuted
                },
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val evidenceColor = when (comp.evidenceClassification) {
                MasteryEvidenceClassification.ARTIFACT_SUPPORTED -> ElectricCyan
                MasteryEvidenceClassification.SIMULATION_EVALUATED -> ArcaneViolet
                MasteryEvidenceClassification.PRACTICE_OBSERVED -> GoldHighlight
                MasteryEvidenceClassification.SELF_REPORTED,
                MasteryEvidenceClassification.HUMAN_CONFIRMED -> TextMuted
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(evidenceColor.copy(alpha = 0.16f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = comp.evidenceClassification.label,
                    color = evidenceColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            val scoreLabel = comp.lastEvaluatedScore?.let { "$it%" } ?: "N/A"
            Text(
                text = "Reps: ${comp.practiceCount} • Score: $scoreLabel",
                color = TextMuted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        if (comp.maintenanceRequired && !comp.maintenanceReason.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(GoldHighlight.copy(alpha = 0.14f))
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "MAINTENANCE DUE: ${comp.maintenanceReason}",
                    color = GoldHighlight,
                    fontSize = 9.sp,
                    lineHeight = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Difficulty Override:",
            color = TextMuted,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AdaptiveDifficulty.entries.forEach { diff ->
                val selected = comp.recommendedDifficulty == diff
                FilterChip(
                    selected = selected,
                    onClick = { onOverrideDifficulty(comp.competencyId, diff) },
                    label = {
                        Text(
                            text = diff.label,
                            fontSize = 10.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("difficulty_chip_${comp.competencyId}_${diff.name}"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricCyan.copy(alpha = 0.22f),
                        selectedLabelColor = ElectricCyan,
                        labelColor = TextMuted
                    )
                )
            }
        }
    }
}

// =========================================================================
// PHASE 5: WEEKLY DEVELOPMENT REPORT DIALOG
// =========================================================================

@Composable
fun SocialWeeklyReportDialog(
    report: SocialWeeklyDevelopmentReport?,
    onDismiss: () -> Unit,
    onAskAi: (SocialWeeklyDevelopmentReport) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "WEEKLY DEVELOPMENT REPORT",
                color = ElectricCyan,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                fontFamily = FontFamily.Monospace
            )
        },
        text = {
            if (report == null) {
                Text("Loading weekly metrics...", color = TextMuted, fontSize = 12.sp)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• Practice Time: ${report.totalMinutesPracticed} minutes logged", color = TextWhite, fontSize = 12.sp)
                    Text("• Exercises Completed: ${report.exercisesCompletedCount}", color = TextWhite, fontSize = 12.sp)
                    Text("• Missions Cleared: ${report.missionsCompletedCount}", color = TextWhite, fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Observed Improvements:", color = GoldHighlight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    report.observedImprovements.forEach { imp ->
                        Text("  ✓ $imp", color = TextWhite, fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Areas Requiring Attention:", color = ArcaneViolet, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    report.areasRequiringAttention.forEach { area ->
                        Text("  ⚠ $area", color = TextMuted, fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Next Week Priorities:", color = ElectricCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    report.nextWeekPriorities.forEach { prio ->
                        Text("  → $prio", color = TextWhite, fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    report?.let { onAskAi(it) }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = ObsidianBlack)
            ) {
                Text("DISCUSS WITH AI COACH", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = TextMuted, fontSize = 11.sp)
            }
        },
        containerColor = DeepNavy
    )
}
