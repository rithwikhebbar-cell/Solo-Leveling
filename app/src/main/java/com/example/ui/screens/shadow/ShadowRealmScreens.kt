package com.example.ui.screens.shadow

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Token
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Upgrade
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.engine.MonarchProgressionEngine
import com.example.domain.model.MonarchRank
import com.example.domain.model.ShadowClass
import com.example.domain.model.ShadowExtractionOutcome
import com.example.domain.model.ShadowExtractionSourceType
import com.example.domain.model.ShadowRank
import com.example.domain.model.ShadowRarity
import com.example.domain.model.ShadowResourceType
import com.example.ui.components.HolographicPanel
import com.example.ui.viewmodel.ShadowExtractionUiState
import com.example.ui.viewmodel.ShadowRealmUiState
import com.example.ui.viewmodel.ShadowRealmViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ============================================================================
// 1. SHADOW REALM DASHBOARD SCREEN
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShadowRealmDashboardScreen(
    viewModel: ShadowRealmViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToMonarchStatus: () -> Unit,
    onNavigateToExtraction: () -> Unit,
    onNavigateToCollection: () -> Unit,
    onNavigateToDeployment: () -> Unit,
    onNavigateToFormations: () -> Unit,
    onNavigateToMonarchSkills: () -> Unit,
    onNavigateToQuests: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToArchive: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val profile = uiState.monarchProfile

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "THE SHADOW REALM",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 2.sp
                        ),
                        color = ShadowColors.PureIce
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("nav_back")) {
                        Icon(Icons.AutoMirrored.rounded.ArrowBack, contentDescription = "Back", tint = ShadowColors.PureIce)
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = ShadowColors.Muted)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ShadowColors.Obsidian)
            )
        },
        containerColor = ShadowColors.Obsidian
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Monarch Hero Banner
            if (profile != null) {
                HolographicPanel(
                    accentColor = ShadowColors.Violet,
                    secondaryColor = ShadowColors.Cyan,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateToMonarchStatus)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "SHADOW MONARCH",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 2.sp
                                    ),
                                    color = ShadowColors.Cyan
                                )
                                Text(
                                    text = "Level ${profile.monarchLevel}",
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                    color = ShadowColors.PureIce
                                )
                            }
                            MonarchRankBadge(profile.monarchRank)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val nextLevelXp = MonarchProgressionEngine.calculateNextLevelXp(profile.monarchLevel)
                        val progress = if (nextLevelXp > 0) (profile.monarchXp.toFloat() / nextLevelXp).coerceIn(0f, 1f) else 1f

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "MONARCH XP",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    color = ShadowColors.Muted
                                )
                                Text(
                                    text = "${profile.monarchXp} / $nextLevelXp",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    color = ShadowColors.PureIce
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = ShadowColors.Violet,
                                trackColor = ShadowColors.PanelSurfaceElevated
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Army Capacity: ${profile.activeShadowCount}/${profile.armyCapacity}",
                                style = MaterialTheme.typography.bodySmall,
                                color = ShadowColors.PureIce
                            )
                            Text(
                                text = "Power Rating: ${profile.shadowArmyPowerRating}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = ShadowColors.Gold
                            )
                        }
                    }
                }
            }

            // Resource Vault Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ResourcePill(ShadowResourceType.SHADOW_ESSENCE, uiState.shadowEssenceBalance, modifier = Modifier.weight(1f))
                ResourcePill(ShadowResourceType.MONARCH_FRAGMENTS, uiState.monarchFragmentsBalance, modifier = Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ResourcePill(ShadowResourceType.EVOLUTION_CORES, uiState.evolutionCoresBalance, modifier = Modifier.weight(1f))
                ResourcePill(ShadowResourceType.SHADOW_CRYSTALS, uiState.shadowCrystalsBalance, modifier = Modifier.weight(1f))
            }

            // PRIMARY CTA: SHADOW EXTRACTION
            Card(
                colors = CardDefaults.cardColors(containerColor = ShadowColors.VioletDeep.copy(alpha = 0.35f)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.5.dp, ShadowColors.Cyan),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToExtraction)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(ShadowColors.Cyan.copy(alpha = 0.2f))
                    ) {
                        Icon(Icons.Rounded.Bolt, contentDescription = null, tint = ShadowColors.Cyan, modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SHADOW EXTRACTION",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                            color = ShadowColors.PureIce
                        )
                        Text(
                            text = "Extract shadows from completed dungeons, bosses & achievements",
                            style = MaterialTheme.typography.bodySmall,
                            color = ShadowColors.Muted
                        )
                    }
                }
            }

            // Quick Operations Grid
            Text(
                text = "ARMY COMMAND",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, letterSpacing = 1.5.sp),
                color = ShadowColors.Muted
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CommandCard(
                    title = "ROSTER",
                    subtitle = "${uiState.allShadows.size} Shadows",
                    icon = Icons.Rounded.Groups,
                    color = ShadowColors.Cyan,
                    onClick = onNavigateToCollection,
                    modifier = Modifier.weight(1f)
                )
                CommandCard(
                    title = "DEPLOYMENT",
                    subtitle = "${uiState.activeDeployments.size} Active",
                    icon = Icons.Rounded.Shield,
                    color = ShadowColors.Emerald,
                    onClick = onNavigateToDeployment,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CommandCard(
                    title = "FORMATIONS",
                    subtitle = uiState.activeFormation?.name ?: "None Active",
                    icon = Icons.Rounded.Tune,
                    color = ShadowColors.Violet,
                    onClick = onNavigateToFormations,
                    modifier = Modifier.weight(1f)
                )
                CommandCard(
                    title = "MONARCH SKILLS",
                    subtitle = "${uiState.unlockedMonarchSkills.size}/8 Unlocked",
                    icon = Icons.Rounded.Star,
                    color = ShadowColors.Gold,
                    onClick = onNavigateToMonarchSkills,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CommandCard(
                    title = "MONARCH QUESTS",
                    subtitle = "${uiState.questProgress.count { it.second?.isCompleted == true }}/8 Done",
                    icon = Icons.Rounded.MilitaryTech,
                    color = ShadowColors.Crimson,
                    onClick = onNavigateToQuests,
                    modifier = Modifier.weight(1f)
                )
                CommandCard(
                    title = "ANALYTICS",
                    subtitle = "Army Stats",
                    icon = Icons.Rounded.AutoGraph,
                    color = Color(0xFF38BDF8),
                    onClick = onNavigateToAnalytics,
                    modifier = Modifier.weight(1f)
                )
            }

            // Archive link
            OutlinedButton(
                onClick = onNavigateToArchive,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ShadowColors.Muted),
                border = BorderStroke(1.dp, ShadowColors.PanelSurfaceElevated)
            ) {
                Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "VIEW SHADOW ARCHIVE & LOGS", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun CommandCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ShadowColors.PanelSurface),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                color = ShadowColors.PureIce
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = ShadowColors.Muted
            )
        }
    }
}

// ============================================================================
// 2. MONARCH STATUS SCREEN
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonarchStatusScreen(
    viewModel: ShadowRealmViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val profile = uiState.monarchProfile ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MONARCH STATUS", style = MaterialTheme.typography.titleMedium, color = ShadowColors.PureIce) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.rounded.ArrowBack, contentDescription = "Back", tint = ShadowColors.PureIce)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ShadowColors.Obsidian)
            )
        },
        containerColor = ShadowColors.Obsidian
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Card
            HolographicPanel(accentColor = ShadowColors.Violet, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(36.dp))
                            .background(ShadowColors.Violet.copy(alpha = 0.2f))
                            .border(2.dp, ShadowColors.Cyan, RoundedCornerShape(36.dp))
                    ) {
                        Icon(Icons.Rounded.Star, contentDescription = null, tint = ShadowColors.Gold, modifier = Modifier.size(40.dp))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "THE SHADOW MONARCH",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                        color = ShadowColors.PureIce
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    MonarchRankBadge(profile.monarchRank)
                }
            }

            // Stats grid
            Text(text = "MONARCH ATTRIBUTES", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = ShadowColors.Muted)

            Card(
                colors = CardDefaults.cardColors(containerColor = ShadowColors.PanelSurface),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, ShadowColors.PanelSurfaceElevated),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    AttributeRow("Monarch Level", "${profile.monarchLevel}")
                    AttributeRow("Monarch Authority", "${profile.monarchAuthority} pts")
                    AttributeRow("Army Capacity", "${profile.activeShadowCount} / ${profile.armyCapacity}")
                    AttributeRow("Total Recruited", "${profile.totalShadowsRecruited}")
                    AttributeRow("Total Evolutions", "${profile.totalEvolutions}")
                    AttributeRow("Highest Shadow Rank", profile.highestShadowRank.displayName)
                    AttributeRow("Shadow Army Power", "${profile.shadowArmyPowerRating}")
                }
            }

            // Unlocked skills
            Text(text = "UNLOCKED MONARCH SKILLS (${uiState.unlockedMonarchSkills.size})", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = ShadowColors.Muted)

            if (uiState.unlockedMonarchSkills.isEmpty()) {
                Surface(
                    color = ShadowColors.PanelSurface,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No Monarch Skills unlocked yet. Progress through Monarch Levels to unlock skills.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ShadowColors.Muted,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                uiState.unlockedMonarchSkills.forEach { skill ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ShadowColors.PanelSurface),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, ShadowColors.Cyan.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = ShadowColors.Emerald, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = skill.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = ShadowColors.PureIce)
                                Text(text = skill.description, style = MaterialTheme.typography.bodySmall, color = ShadowColors.Muted)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttributeRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = ShadowColors.Muted)
        Text(text = value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace), color = ShadowColors.PureIce)
    }
}

// ============================================================================
// 3. SHADOW EXTRACTION SCREEN
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShadowExtractionScreen(
    viewModel: ShadowRealmViewModel,
    onNavigateBack: () -> Unit
) {
    val extractionState by viewModel.extractionUiState.collectAsState()
    var selectedSourceType by remember { mutableStateOf(ShadowExtractionSourceType.DUNGEON_COMPLETION) }
    var sourceEventIdInput by remember { mutableStateOf("dungeon_complete_001") }
    var evidenceInput by remember { mutableStateOf("Cleared C-Rank Dungeon: Frost Cave") }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "alpha"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SHADOW EXTRACTION", style = MaterialTheme.typography.titleMedium, color = ShadowColors.PureIce) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.rounded.ArrowBack, contentDescription = "Back", tint = ShadowColors.PureIce)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ShadowColors.Obsidian)
            )
        },
        containerColor = ShadowColors.Obsidian
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Source selection
            Text(text = "EXTRACTION SOURCE", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = ShadowColors.Muted)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SourceTypeChip(
                    label = "Dungeon",
                    selected = selectedSourceType == ShadowExtractionSourceType.DUNGEON_COMPLETION,
                    onClick = {
                        selectedSourceType = ShadowExtractionSourceType.DUNGEON_COMPLETION
                        sourceEventIdInput = "dungeon_complete_${System.currentTimeMillis() / 60000}"
                        evidenceInput = "Completed Dungeon challenge verified"
                    },
                    modifier = Modifier.weight(1f)
                )
                SourceTypeChip(
                    label = "Boss",
                    selected = selectedSourceType == ShadowExtractionSourceType.BOSS_BATTLE_COMPLETION,
                    onClick = {
                        selectedSourceType = ShadowExtractionSourceType.BOSS_BATTLE_COMPLETION
                        sourceEventIdInput = "boss_defeat_${System.currentTimeMillis() / 60000}"
                        evidenceInput = "Defeated Monarch Raid Boss"
                    },
                    modifier = Modifier.weight(1f)
                )
                SourceTypeChip(
                    label = "Discipline",
                    selected = selectedSourceType == ShadowExtractionSourceType.DISCIPLINE_MILESTONE,
                    onClick = {
                        selectedSourceType = ShadowExtractionSourceType.DISCIPLINE_MILESTONE
                        sourceEventIdInput = "discipline_streak_${System.currentTimeMillis() / 86400000}"
                        evidenceInput = "Maintained 7-day verified discipline streak"
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = sourceEventIdInput,
                onValueChange = { sourceEventIdInput = it },
                label = { Text("Source Event ID") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = ShadowColors.PureIce,
                    unfocusedTextColor = ShadowColors.PureIce,
                    focusedBorderColor = ShadowColors.Cyan,
                    unfocusedBorderColor = ShadowColors.PanelSurfaceElevated
                )
            )

            OutlinedTextField(
                value = evidenceInput,
                onValueChange = { evidenceInput = it },
                label = { Text("Evidence Description") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = ShadowColors.PureIce,
                    unfocusedTextColor = ShadowColors.PureIce,
                    focusedBorderColor = ShadowColors.Cyan,
                    unfocusedBorderColor = ShadowColors.PanelSurfaceElevated
                )
            )

            Button(
                onClick = {
                    viewModel.evaluateExtraction(selectedSourceType, sourceEventIdInput, evidenceInput)
                },
                enabled = !extractionState.isEvaluating && sourceEventIdInput.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = ShadowColors.Violet)
            ) {
                if (extractionState.isEvaluating) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = ShadowColors.PureIce)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("ANALYZE SHADOW REMNANT", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
            }

            // Cinematic extraction display
            val pending = extractionState.pendingExtraction
            if (pending != null && pending.outcome == ShadowExtractionOutcome.PENDING_CONFIRMATION) {
                HolographicPanel(
                    accentColor = ShadowColors.Cyan,
                    secondaryColor = ShadowColors.Violet,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "TARGET IDENTIFIED.",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, letterSpacing = 2.sp),
                            color = ShadowColors.Cyan
                        )
                        Text(
                            text = "SHADOW EXTRACTION AVAILABLE",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = ShadowColors.PureIce
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Holographic Shadow preview
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(45.dp))
                                .background(ShadowColors.VioletDeep.copy(alpha = 0.5f))
                                .border(2.dp, ShadowColors.Cyan.copy(alpha = alphaAnim), RoundedCornerShape(45.dp))
                        ) {
                            Icon(Icons.Rounded.Bolt, contentDescription = null, tint = ShadowColors.Cyan, modifier = Modifier.size(48.dp))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = pending.shadowName ?: "Shadow Soldier",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = ShadowColors.PureIce
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            pending.shadowClass?.let { ShadowClassBadge(it) }
                            pending.shadowRarity?.let { ShadowRarityBadge(it) }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "THE SHADOW HAS ACCEPTED YOUR AUTHORITY.",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = ShadowColors.Gold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { viewModel.confirmExtraction() },
                            enabled = !extractionState.isConfirming,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ShadowColors.Cyan)
                        ) {
                            if (extractionState.isConfirming) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = ShadowColors.Obsidian)
                            } else {
                                Text(
                                    text = "ARISE.",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 4.sp
                                    ),
                                    color = ShadowColors.Obsidian
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { viewModel.cancelExtraction() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ShadowColors.Muted)
                        ) {
                            Text("DISMISS")
                        }
                    }
                }
            }

            // Result display
            val lastResult = extractionState.lastResult
            if (lastResult != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (lastResult.outcome == ShadowExtractionOutcome.SUCCESS) ShadowColors.Emerald.copy(alpha = 0.15f)
                        else ShadowColors.Crimson.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        if (lastResult.outcome == ShadowExtractionOutcome.SUCCESS) ShadowColors.Emerald else ShadowColors.Crimson
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (lastResult.outcome == ShadowExtractionOutcome.SUCCESS) Icons.Rounded.CheckCircle else Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = if (lastResult.outcome == ShadowExtractionOutcome.SUCCESS) ShadowColors.Emerald else ShadowColors.Crimson,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "EXTRACTION OUTCOME: ${lastResult.outcome.name}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = ShadowColors.PureIce
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (lastResult.outcome == ShadowExtractionOutcome.SUCCESS) {
                            Text(
                                text = "${lastResult.shadowName} has joined your Shadow Army! Awarded ${lastResult.monarchXpAwarded} Monarch XP and 25 Shadow Essence.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ShadowColors.PureIce
                            )
                        } else {
                            Text(
                                text = "Extraction could not proceed: ${lastResult.outcome.name}. Deterministic check completed.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ShadowColors.Muted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceTypeChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        color = if (selected) ShadowColors.Cyan.copy(alpha = 0.2f) else ShadowColors.PanelSurface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (selected) ShadowColors.Cyan else ShadowColors.PanelSurfaceElevated),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal),
            color = if (selected) ShadowColors.Cyan else ShadowColors.Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
        )
    }
}

// ============================================================================
// 4. SHADOW ARMY ANALYTICS SCREEN
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShadowArmyAnalyticsScreen(
    viewModel: ShadowRealmViewModel,
    onNavigateBack: () -> Unit
) {
    val analytics by viewModel.analytics.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadAnalytics()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ARMY ANALYTICS", style = MaterialTheme.typography.titleMedium, color = ShadowColors.PureIce) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.rounded.ArrowBack, contentDescription = "Back", tint = ShadowColors.PureIce)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ShadowColors.Obsidian)
            )
        },
        containerColor = ShadowColors.Obsidian
    ) { padding ->
        val data = analytics
        if (data == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ShadowColors.Cyan)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // High-level cards
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AnalyticsMetricCard("TOTAL ARMY", "${data.totalShadows}", ShadowColors.Cyan, Modifier.weight(1f))
                    AnalyticsMetricCard("DEPLOYED", "${data.deployedCount}", ShadowColors.Emerald, Modifier.weight(1f))
                    AnalyticsMetricCard("POWER", "${data.totalArmyPowerRating}", ShadowColors.Gold, Modifier.weight(1f))
                }

                // Class breakdown
                Text(text = "CLASS DISTRIBUTION", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = ShadowColors.Muted)
                Card(
                    colors = CardDefaults.cardColors(containerColor = ShadowColors.PanelSurface),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ShadowColors.PanelSurfaceElevated),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        data.distributionByClass.forEach { (cls, count) ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                ShadowClassBadge(cls, modifier = Modifier.width(90.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                val progress = if (data.totalShadows > 0) count.toFloat() / data.totalShadows else 0f
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = ShadowColors.classColor(cls),
                                    trackColor = ShadowColors.PanelSurfaceElevated
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(text = "$count", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = ShadowColors.PureIce)
                            }
                        }
                    }
                }

                // Rank breakdown
                Text(text = "RANK DISTRIBUTION", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = ShadowColors.Muted)
                Card(
                    colors = CardDefaults.cardColors(containerColor = ShadowColors.PanelSurface),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ShadowColors.PanelSurfaceElevated),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        data.distributionByRank.forEach { (rank, count) ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                ShadowRankBadge(rank, modifier = Modifier.width(90.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                val progress = if (data.totalShadows > 0) count.toFloat() / data.totalShadows else 0f
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = ShadowColors.rankColor(rank),
                                    trackColor = ShadowColors.PanelSurfaceElevated
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(text = "$count", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = ShadowColors.PureIce)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnalyticsMetricCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ShadowColors.PanelSurface),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = ShadowColors.Muted)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace), color = color)
        }
    }
}

// ============================================================================
// 5. SHADOW ARCHIVE SCREEN
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShadowArchiveScreen(
    viewModel: ShadowRealmViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val extractions by viewModel.repository.observeExtractionHistory().collectAsState(initial = emptyList())
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SHADOW ARCHIVE & LOGS", style = MaterialTheme.typography.titleMedium, color = ShadowColors.PureIce) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.rounded.ArrowBack, contentDescription = "Back", tint = ShadowColors.PureIce)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ShadowColors.Obsidian)
            )
        },
        containerColor = ShadowColors.Obsidian
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = "DETERMINISTIC EXTRACTION LOGS (${extractions.size})",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = ShadowColors.Muted
                )
            }

            if (extractions.isEmpty()) {
                item {
                    Text(text = "No extraction records yet.", style = MaterialTheme.typography.bodySmall, color = ShadowColors.Muted)
                }
            }

            items(extractions) { record ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = ShadowColors.PanelSurface),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, ShadowColors.PanelSurfaceElevated),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = record.shadowName ?: "Extraction Attempt",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = ShadowColors.PureIce
                            )
                            Text(
                                text = record.outcome.name,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (record.outcome == ShadowExtractionOutcome.SUCCESS) ShadowColors.Emerald else ShadowColors.Crimson
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Source: ${record.sourceType.name} (${record.sourceEventId})",
                            style = MaterialTheme.typography.bodySmall,
                            color = ShadowColors.Muted
                        )
                        Text(
                            text = "Seed: ${record.determinismSeed} | ${dateFormat.format(Date(record.timestampEpochMs))}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                            color = ShadowColors.Muted
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// 6. SHADOW SETTINGS SCREEN
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShadowSettingsScreen(
    viewModel: ShadowRealmViewModel,
    onNavigateBack: () -> Unit
) {
    var extractionNotifs by remember { mutableStateOf(true) }
    var evolutionNotifs by remember { mutableStateOf(true) }
    var autoRecallOnRest by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SHADOW REALM SETTINGS", style = MaterialTheme.typography.titleMedium, color = ShadowColors.PureIce) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.rounded.ArrowBack, contentDescription = "Back", tint = ShadowColors.PureIce)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ShadowColors.Obsidian)
            )
        },
        containerColor = ShadowColors.Obsidian
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "NOTIFICATION PREFERENCES", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = ShadowColors.Muted)

            Card(
                colors = CardDefaults.cardColors(containerColor = ShadowColors.PanelSurface),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, ShadowColors.PanelSurfaceElevated),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Extraction Alerts", style = MaterialTheme.typography.bodyMedium, color = ShadowColors.PureIce)
                            Text(text = "Announce when new shadows are ready to be extracted", style = MaterialTheme.typography.bodySmall, color = ShadowColors.Muted)
                        }
                        Switch(
                            checked = extractionNotifs,
                            onCheckedChange = { extractionNotifs = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = ShadowColors.Cyan, checkedTrackColor = ShadowColors.Violet)
                        )
                    }

                    HorizontalDivider(color = ShadowColors.PanelSurfaceElevated)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Evolution Readiness", style = MaterialTheme.typography.bodyMedium, color = ShadowColors.PureIce)
                            Text(text = "Notify when a shadow reaches level requirement to evolve", style = MaterialTheme.typography.bodySmall, color = ShadowColors.Muted)
                        }
                        Switch(
                            checked = evolutionNotifs,
                            onCheckedChange = { evolutionNotifs = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = ShadowColors.Cyan, checkedTrackColor = ShadowColors.Violet)
                        )
                    }

                    HorizontalDivider(color = ShadowColors.PanelSurfaceElevated)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Recovery Alignment", style = MaterialTheme.typography.bodyMedium, color = ShadowColors.PureIce)
                            Text(text = "Respect player rest days; shadows reduce intensity during rest", style = MaterialTheme.typography.bodySmall, color = ShadowColors.Muted)
                        }
                        Switch(
                            checked = autoRecallOnRest,
                            onCheckedChange = { autoRecallOnRest = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = ShadowColors.Cyan, checkedTrackColor = ShadowColors.Violet)
                        )
                    }
                }
            }

            Text(text = "SYSTEM INTEGRITY", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = ShadowColors.Muted)

            Card(
                colors = CardDefaults.cardColors(containerColor = ShadowColors.PanelSurface),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, ShadowColors.PanelSurfaceElevated),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Deterministic Ledger Active", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = ShadowColors.Emerald)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "All extractions, evolutions, and skill unlocks are cryptographically recorded and verifiable offline. Player progression boundaries are strictly enforced.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ShadowColors.Muted
                    )
                }
            }
        }
    }
}

