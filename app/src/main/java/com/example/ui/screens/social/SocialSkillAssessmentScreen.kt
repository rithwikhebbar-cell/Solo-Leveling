package com.example.ui.screens.social

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.engine.SocialSkillAssessmentEngine
import com.example.domain.model.SocialMasteryStage
import com.example.domain.model.SocialModuleIdentifier
import com.example.ui.viewmodel.SocialMasteryViewModel
import kotlin.math.roundToInt

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
fun SocialSkillAssessmentScreen(
    viewModel: SocialMasteryViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    var selectedModule by remember { mutableStateOf(SocialModuleIdentifier.COMMUNICATION_MASTERY) }
    val rubric = SocialSkillAssessmentEngine.rubrics[selectedModule] ?: SocialSkillAssessmentEngine.rubrics.values.first()

    var score1 by remember { mutableFloatStateOf(70f) }
    var score2 by remember { mutableFloatStateOf(70f) }
    var score3 by remember { mutableFloatStateOf(70f) }
    var score4 by remember { mutableFloatStateOf(70f) }
    var score5 by remember { mutableFloatStateOf(70f) }
    var notes by remember { mutableStateOf("") }

    val calculatedAvg = ((score1 + score2 + score3 + score4 + score5) / 5f).roundToInt()
    val currentStage = when {
        calculatedAvg >= 90 -> SocialMasteryStage.MASTER
        calculatedAvg >= 80 -> SocialMasteryStage.ADVANCED
        calculatedAvg >= 70 -> SocialMasteryStage.PROFICIENT
        calculatedAvg >= 55 -> SocialMasteryStage.COMPETENT
        else -> SocialMasteryStage.NOVICE
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SKILL ASSESSMENT",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Module Selector Chips
            Text(
                text = "SELECT MODULE TO ASSESS",
                color = ElectricCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(SocialModuleIdentifier.entries) { mod ->
                    val isSelected = mod == selectedModule
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) ElectricCyan else SurfaceNavy)
                            .clickable { selectedModule = mod }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = mod.displayName,
                            color = if (isSelected) ObsidianBlack else TextWhite,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Rubric Description
            Card(
                colors = CardDefaults.cardColors(containerColor = DeepNavy),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(ElectricCyan, ArcaneViolet))),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = rubric.title,
                        color = TextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = rubric.description,
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }

            // Current Average Indicator Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceNavy),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("RUBRIC SCORE ESTIMATE", color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Text("$calculatedAvg%", color = ElectricCyan, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DeepNavy)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = currentStage.label.uppercase(),
                            color = ArcaneViolet,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Dimension 1
            AssessmentSliderItem(
                name = rubric.dimension1Name,
                desc = rubric.dimension1Desc,
                value = score1,
                onValueChange = { score1 = it }
            )

            // Dimension 2
            AssessmentSliderItem(
                name = rubric.dimension2Name,
                desc = rubric.dimension2Desc,
                value = score2,
                onValueChange = { score2 = it }
            )

            // Dimension 3
            AssessmentSliderItem(
                name = rubric.dimension3Name,
                desc = rubric.dimension3Desc,
                value = score3,
                onValueChange = { score3 = it }
            )

            // Dimension 4
            AssessmentSliderItem(
                name = rubric.dimension4Name,
                desc = rubric.dimension4Desc,
                value = score4,
                onValueChange = { score4 = it }
            )

            // Dimension 5
            AssessmentSliderItem(
                name = rubric.dimension5Name,
                desc = rubric.dimension5Desc,
                value = score5,
                onValueChange = { score5 = it }
            )

            // Qualitative Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Qualitative Notes (Optional context, observed friction)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = BorderCyan,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite
                )
            )

            // Submit Button
            Button(
                onClick = {
                    viewModel.submitSkillAssessment(
                        module = selectedModule,
                        s1 = score1.roundToInt(),
                        s2 = score2.roundToInt(),
                        s3 = score3.roundToInt(),
                        s4 = score4.roundToInt(),
                        s5 = score5.roundToInt(),
                        notes = notes
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = ObsidianBlack)
            ) {
                Text("RECORD SKILL ASSESSMENT", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            // Latest Result Display
            uiState.latestAssessmentResult?.let { res ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DeepNavy),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(GoldHighlight, ArcaneViolet)))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GoldHighlight, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ASSESSMENT RESULT RECORDED",
                                color = GoldHighlight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(res.qualitativeSummary, color = TextWhite, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        res.actionableRecommendations.forEach { rec ->
                            Text("• $rec", color = ElectricCyan, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AssessmentSliderItem(
    name: String,
    desc: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceNavy),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(name, color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("${value.roundToInt()}%", color = ElectricCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
            Text(desc, color = TextMuted, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = 0f..100f,
                steps = 19,
                colors = SliderDefaults.colors(
                    thumbColor = ElectricCyan,
                    activeTrackColor = ElectricCyan,
                    inactiveTrackColor = DeepNavy
                )
            )
        }
    }
}
