package com.example.ui.screens.social

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.engine.SocialMissionCatalog
import com.example.domain.model.RealWorldSocialMission
import com.example.domain.model.SocialMissionLifecycleStatus
import com.example.ui.viewmodel.SocialMasteryViewModel

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
fun SocialMissionDetailScreen(
    missionId: String,
    viewModel: SocialMasteryViewModel,
    onNavigateBack: () -> Unit,
    onOpenSubmission: (String) -> Unit
) {
    val mission = SocialMissionCatalog.getMissionById(missionId)
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "QUEST BRIEFING",
                        color = TextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ElectricCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianBlack)
            )
        },
        containerColor = ObsidianBlack
    ) { paddingValues ->
        if (mission == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Mission not found.", color = TextMuted)
            }
        } else {
            val activeRecord = uiState.activeMissionRecords.find { it.missionId == mission.missionId }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Rank & Module Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ArcaneViolet.copy(alpha = 0.2f))
                            .border(1.dp, ArcaneViolet, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = mission.difficulty.label,
                            color = ArcaneViolet,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "${mission.estimatedDurationMinutes} MIN ESTIMATED",
                        color = ElectricCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Title & Description
                Text(
                    text = mission.title,
                    color = TextWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = mission.description,
                    color = TextMuted,
                    fontSize = 14.sp
                )

                // Objective Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = DeepNavy),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(ElectricCyan, ArcaneViolet))),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "LEARNING OBJECTIVE",
                            color = ElectricCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = mission.learningObjective,
                            color = TextWhite,
                            fontSize = 13.sp
                        )
                    }
                }

                // Step-by-Step Instructions
                Text(
                    text = "EXECUTION PROTOCOL",
                    color = ElectricCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                mission.stepByStepInstructions.forEachIndexed { index, step ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "${index + 1}.",
                            color = ElectricCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(24.dp)
                        )
                        Text(
                            text = step,
                            color = TextWhite,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Completion Criteria
                Text(
                    text = "COMPLETION CRITERIA",
                    color = ElectricCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                mission.completionCriteria.forEach { criteria ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ArcaneViolet, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = criteria,
                            color = TextWhite,
                            fontSize = 13.sp
                        )
                    }
                }

                // Reflection Prompts
                Text(
                    text = "MANDATORY REFLECTION PROMPTS",
                    color = ElectricCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                mission.reflectionQuestions.forEach { question ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceNavy),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = question,
                            color = TextWhite,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // AI Coaching Guidance Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceNavy),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = GoldHighlight, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "TACTICAL AI COACHING ADVICE",
                                color = GoldHighlight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = mission.aiCoachingTip,
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { viewModel.requestAiMissionPreparation(mission) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricCyan)
                        ) {
                            Text("ASK AI COACH FOR PREP TIPS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Status alert if needs improvement
                if (activeRecord?.lifecycleStatus == SocialMissionLifecycleStatus.NEEDS_IMPROVEMENT) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DeepNavy),
                        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(Color(0xFFEF4444), GoldHighlight))),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("STATUS: NEEDS IMPROVEMENT", color = Color(0xFFEF4444), fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(activeRecord.aiFeedbackNotes ?: "Reflection was too brief or rejected. Please retry and submit a detailed reflection.", color = TextWhite, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.retryMission(activeRecord.recordId) },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldHighlight, contentColor = ObsidianBlack),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("RETRY MISSION", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Action Buttons
                Spacer(modifier = Modifier.height(8.dp))
                if (activeRecord == null) {
                    Button(
                        onClick = {
                            viewModel.startMission(mission.missionId)
                            onNavigateBack()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = ObsidianBlack)
                    ) {
                        Text("ACCEPT QUEST & BEGIN", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = { onOpenSubmission(mission.missionId) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = ObsidianBlack)
                    ) {
                        Text("PROCEED TO REFLECTION & SUBMIT", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.postponeMission(activeRecord.recordId)
                            onNavigateBack()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted)
                    ) {
                        Text("POSTPONE WITHOUT PENALTY", fontSize = 12.sp)
                    }
                }

                // Alternative mission if available
                mission.alternativeMissionId?.let { altId ->
                    OutlinedButton(
                        onClick = { viewModel.selectMission(altId) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ArcaneViolet)
                    ) {
                        Text("SWITCH TO PRIVACY ALTERNATIVE MISSION", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
