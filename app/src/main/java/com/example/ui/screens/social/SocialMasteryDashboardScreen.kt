package com.example.ui.screens.social

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.SocialMasteryStage
import com.example.domain.model.SocialModuleProgress
import com.example.ui.viewmodel.SocialMasteryViewModel

// Visual Design Tokens
private val ObsidianBlack = Color(0xFF0A0C10)
private val DeepNavy = Color(0xFF0F172A)
private val SurfaceNavy = Color(0xFF1E293B)
private val ElectricCyan = Color(0xFF00E5FF)
private val ArcaneViolet = Color(0xFF9D4EDD)
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
                        modifier = Modifier.fillMaxWidth(),
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
                            modifier = Modifier.weight(1f),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = DeepNavy),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderCyan),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("REAL-WORLD MISSIONS", color = ElectricCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        androidx.compose.material3.Button(
                            onClick = onOpenAssessment,
                            modifier = Modifier.weight(1f),
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
                            modifier = Modifier.weight(1f),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = ArcaneViolet),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("AI VOICE COACH", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        androidx.compose.material3.Button(
                            onClick = onOpenAnalytics,
                            modifier = Modifier.weight(1f),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = CardDefaults.cardColors().containerColor),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderCyan),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("ANALYTICS RADAR", color = ElectricCyan, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
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
