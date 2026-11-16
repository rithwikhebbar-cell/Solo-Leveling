package com.example.ui.screens.social

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AdaptiveSocialRecommendation
import com.example.domain.model.SocialAnalyticsSummary
import com.example.domain.model.WeaknessPattern
import com.example.ui.viewmodel.SocialMasteryViewModel

private val ObsidianBlack = Color(0xFF070A13)
private val DeepNavy = Color(0xFF0D1424)
private val CardSurface = Color(0xFF182238)
private val ElectricCyan = Color(0xFF00E5FF)
private val ArcaneViolet = Color(0xFF8B5CF6)
private val GoldHighlight = Color(0xFFFFD700)
private val DangerCrimson = Color(0xFFEF4444)
private val TextWhite = Color(0xFFF8FAFC)
private val TextMuted = Color(0xFF94A3B8)
private val BorderCyan = Color(0x3300E5FF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialAnalyticsScreen(
    viewModel: SocialMasteryViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val summary = uiState.analyticsSummary

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "SOCIAL ANALYTICS & WEAKNESS RADAR",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                color = ElectricCyan
                            )
                        )
                        Text(
                            text = "DETERMINISTIC HISTORICAL PERFORMANCE",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, letterSpacing = 1.sp)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ElectricCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianBlack)
            )
        },
        containerColor = ObsidianBlack
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Overview Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderCyan, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = DeepNavy)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "OVERVIEW METRICS",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, letterSpacing = 1.sp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Total Practice", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted))
                                Text("${summary?.totalPracticeMinutes ?: 0} min", style = MaterialTheme.typography.titleMedium.copy(color = ElectricCyan, fontWeight = FontWeight.Bold))
                            }
                            Column {
                                Text("Exercises Run", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted))
                                Text("${summary?.exercisesCompleted ?: 0}", style = MaterialTheme.typography.titleMedium.copy(color = ElectricCyan, fontWeight = FontWeight.Bold))
                            }
                            Column {
                                Text("Scenarios Cleared", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted))
                                Text("${summary?.scenariosCleared ?: 0}", style = MaterialTheme.typography.titleMedium.copy(color = ElectricCyan, fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }

            // Adaptive Recommendations Section
            item {
                Text(
                    text = "ADAPTIVE TRAINING RECOMMENDATIONS",
                    style = MaterialTheme.typography.titleSmall.copy(color = TextWhite, fontWeight = FontWeight.Bold)
                )
            }

            if (summary?.activeRecommendations.isNullOrEmpty()) {
                item {
                    Text(
                        text = "Complete practice drills to generate adaptive recommendations.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )
                }
            } else {
                items(summary!!.activeRecommendations) { rec ->
                    RecommendationCard(
                        rec = rec,
                        onAccept = { viewModel.startExerciseSession(rec.recommendedExerciseId) }
                    )
                }
            }

            // Weakness Radar Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = GoldHighlight, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "IDENTIFIED FRICTION & WEAKNESS PATTERNS",
                        style = MaterialTheme.typography.titleSmall.copy(color = GoldHighlight, fontWeight = FontWeight.Bold)
                    )
                }
            }

            if (summary?.detectedWeaknesses.isNullOrEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = CardSurface)
                    ) {
                        Box(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Zero critical weaknesses detected. Response length, pacing, and scores remain within optimal thresholds.",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextWhite)
                            )
                        }
                    }
                }
            } else {
                items(summary!!.detectedWeaknesses) { weak ->
                    WeaknessCard(weakness = weak)
                }
            }
        }
    }
}

@Composable
fun RecommendationCard(
    rec: AdaptiveSocialRecommendation,
    onAccept: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x338B5CF6), RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = CardSurface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = rec.title,
                    style = MaterialTheme.typography.titleSmall.copy(color = TextWhite, fontWeight = FontWeight.Bold)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0x338B5CF6))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "PRIORITY ${rec.priorityScore}",
                        style = MaterialTheme.typography.labelSmall.copy(color = ArcaneViolet, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = rec.reasonExplanation,
                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 12.sp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onAccept,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
            ) {
                Text("START RECOMMENDED DRILL", color = ObsidianBlack, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun WeaknessCard(weakness: WeaknessPattern) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x33EF4444), RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = DeepNavy)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = weakness.title,
                style = MaterialTheme.typography.titleSmall.copy(color = DangerCrimson, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = weakness.description,
                style = MaterialTheme.typography.bodySmall.copy(color = TextWhite, fontSize = 12.sp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            weakness.supportingEvidenceNotes.forEach { note ->
                Text(
                    text = "• $note",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 11.sp)
                )
            }
        }
    }
}
