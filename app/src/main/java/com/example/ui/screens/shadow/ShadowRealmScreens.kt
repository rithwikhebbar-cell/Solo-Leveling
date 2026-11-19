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

import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import com.example.domain.model.ShadowEvidenceEligibility
import com.example.domain.model.ShadowExtractionEvidenceItem
import com.example.domain.model.ShadowResonanceConfig
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
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = ShadowColors.PureIce)
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
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = ShadowColors.PureIce)
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
                    AttributeRow("Highest Shadow Rank", profile.highestShadowRank.label)
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
    val evidenceList by viewModel.eligibleEvidence.collectAsState()
    val selectedItem by viewModel.selectedEvidenceItem.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var sourceFilter by remember { mutableStateOf<ShadowExtractionSourceType?>(null) }
    var showOnlyEligible by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    LaunchedEffect(Unit) {
        viewModel.loadEligibleEvidence()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "alpha"
    )

    val filteredEvidence = remember(evidenceList, sourceFilter, showOnlyEligible) {
        evidenceList.filter { item ->
            (sourceFilter == null || item.sourceType == sourceFilter) &&
            (!showOnlyEligible || item.eligibility == ShadowEvidenceEligibility.ELIGIBLE)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SHADOW RECRUITMENT & EXTRACTION", style = MaterialTheme.typography.titleMedium, color = ShadowColors.PureIce) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = ShadowColors.PureIce)
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
            // A. Recruitment Hub Overview Card
            HolographicPanel(
                accentColor = ShadowColors.Cyan,
                secondaryColor = ShadowColors.Violet,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RECRUITMENT HUB",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 2.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = ShadowColors.Cyan
                        )
                        val eligibleCount = evidenceList.count { it.eligibility == ShadowEvidenceEligibility.ELIGIBLE }
                        Text(
                            text = "$eligibleCount Available Remnant${if (eligibleCount == 1) "" else "s"}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (eligibleCount > 0) ShadowColors.Emerald else ShadowColors.Muted
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Army Capacity: ${uiState.activeShadows.size} / ${uiState.monarchProfile?.armyCapacity ?: 10} Shadows",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = ShadowColors.PureIce
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Only authoritative completed activities (Dungeons, Monarch Raids, Achievements, Social Missions) grant verified extraction remnants. Arbitrary manual entry is strictly prohibited by System Law.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ShadowColors.Muted
                    )
                }
            }

            // Extraction Result Panel (if active)
            val lastResult = extractionState.lastResult
            if (lastResult != null) {
                val isSuccess = lastResult.outcome == ShadowExtractionOutcome.SUCCESS
                val isDuplicate = lastResult.outcome == ShadowExtractionOutcome.DUPLICATE_RESOLVED
                val resultColor = when {
                    isSuccess -> ShadowColors.Emerald
                    isDuplicate -> ShadowColors.Cyan
                    else -> ShadowColors.Crimson
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = resultColor.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, resultColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isSuccess || isDuplicate) Icons.Rounded.CheckCircle else Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = resultColor,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when {
                                    isSuccess -> "SHADOW RECRUITED"
                                    isDuplicate -> "DUPLICATE RESONANCE RESOLVED"
                                    else -> "EXTRACTION ${lastResult.outcome.name}"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = ShadowColors.PureIce
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (isSuccess) {
                            Text(
                                text = "${lastResult.shadowName} has answered your call and arisen to join your Shadow Army! Awarded ${lastResult.monarchXpAwarded} Monarch XP and 25 Shadow Essence.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ShadowColors.PureIce
                            )
                        } else if (isDuplicate) {
                            Text(
                                text = "${lastResult.shadowName} is already bound to your army. Duplicate remnant successfully converted into +${ShadowResonanceConfig.SHARDS_PER_DUPLICATE} Resonance Shards for this archetype's Resonance Core. Army capacity preserved.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ShadowColors.PureIce
                            )
                        } else {
                            Text(
                                text = "Extraction rejected: ${lastResult.outcome.name}. No army capacity or resources were consumed.",
                                style = MaterialTheme.typography.bodySmall,
                                color = ShadowColors.Muted
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.clearSelectedEvidence() },
                            colors = ButtonDefaults.buttonColors(containerColor = resultColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("CONTINUE", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }

            // Confirmation / Preparation Panel (if item is selected & pending extraction)
            val pending = extractionState.pendingExtraction
            if (selectedItem != null && pending != null && pending.outcome == ShadowExtractionOutcome.PENDING_CONFIRMATION && lastResult == null) {
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
                            text = "EXTRACTION RITUAL PREPARATION",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, letterSpacing = 2.sp),
                            color = ShadowColors.Cyan
                        )
                        Text(
                            text = "SHADOW REMNANT LOCATED",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = ShadowColors.PureIce
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(40.dp))
                                .background(ShadowColors.VioletDeep.copy(alpha = 0.5f))
                                .border(2.dp, ShadowColors.Cyan.copy(alpha = alphaAnim), RoundedCornerShape(40.dp))
                        ) {
                            Icon(Icons.Rounded.Bolt, contentDescription = null, tint = ShadowColors.Cyan, modifier = Modifier.size(44.dp))
                        }

                        Spacer(modifier = Modifier.height(10.dp))

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

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Harvested from: ${selectedItem?.title}",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = ShadowColors.Muted,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "If already recruited, resonance will automatically grant +${ShadowResonanceConfig.SHARDS_PER_DUPLICATE} Resonance Shards without consuming army capacity.",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
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
                            onClick = { viewModel.clearSelectedEvidence() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ShadowColors.Muted)
                        ) {
                            Text("CANCEL SELECTION")
                        }
                    }
                }
            }

            // B. Evidence Filter Chips
            Text(
                text = "VERIFIED EXTRACTION EVIDENCE",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = ShadowColors.Muted
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = sourceFilter == null,
                    onClick = { sourceFilter = null },
                    label = { Text("All (${evidenceList.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ShadowColors.Cyan.copy(alpha = 0.2f),
                        selectedLabelColor = ShadowColors.Cyan
                    )
                )
                FilterChip(
                    selected = sourceFilter == ShadowExtractionSourceType.DUNGEON_COMPLETION,
                    onClick = { sourceFilter = if (sourceFilter == ShadowExtractionSourceType.DUNGEON_COMPLETION) null else ShadowExtractionSourceType.DUNGEON_COMPLETION },
                    label = { Text("Dungeon") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ShadowColors.Cyan.copy(alpha = 0.2f),
                        selectedLabelColor = ShadowColors.Cyan
                    )
                )
                FilterChip(
                    selected = sourceFilter == ShadowExtractionSourceType.BOSS_BATTLE_COMPLETION,
                    onClick = { sourceFilter = if (sourceFilter == ShadowExtractionSourceType.BOSS_BATTLE_COMPLETION) null else ShadowExtractionSourceType.BOSS_BATTLE_COMPLETION },
                    label = { Text("Boss Raid") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ShadowColors.Cyan.copy(alpha = 0.2f),
                        selectedLabelColor = ShadowColors.Cyan
                    )
                )
                FilterChip(
                    selected = sourceFilter == ShadowExtractionSourceType.ACHIEVEMENT_UNLOCK,
                    onClick = { sourceFilter = if (sourceFilter == ShadowExtractionSourceType.ACHIEVEMENT_UNLOCK) null else ShadowExtractionSourceType.ACHIEVEMENT_UNLOCK },
                    label = { Text("Achievement") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ShadowColors.Cyan.copy(alpha = 0.2f),
                        selectedLabelColor = ShadowColors.Cyan
                    )
                )
                FilterChip(
                    selected = sourceFilter == ShadowExtractionSourceType.CONSISTENCY_MILESTONE,
                    onClick = { sourceFilter = if (sourceFilter == ShadowExtractionSourceType.CONSISTENCY_MILESTONE) null else ShadowExtractionSourceType.CONSISTENCY_MILESTONE },
                    label = { Text("Consistency") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ShadowColors.Cyan.copy(alpha = 0.2f),
                        selectedLabelColor = ShadowColors.Cyan
                    )
                )
                FilterChip(
                    selected = showOnlyEligible,
                    onClick = { showOnlyEligible = !showOnlyEligible },
                    label = { Text("Eligible Only") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ShadowColors.Emerald.copy(alpha = 0.2f),
                        selectedLabelColor = ShadowColors.Emerald
                    )
                )
            }

            // C. Evidence Items List
            if (filteredEvidence.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ShadowColors.PanelSurface),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ShadowColors.PanelSurfaceElevated),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = ShadowColors.Muted,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "NO EXTRACTION EVIDENCE AVAILABLE",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = ShadowColors.PureIce
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Complete Dungeons, defeat Monarch Raid Bosses, earn Achievements, or verify Social Missions to harvest eligible remnants.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ShadowColors.Muted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                filteredEvidence.forEach { item ->
                    val isEligible = item.eligibility == ShadowEvidenceEligibility.ELIGIBLE
                    val isSelected = selectedItem?.sourceRecordId == item.sourceRecordId

                    val cardBorder = when {
                        isSelected -> BorderStroke(2.dp, ShadowColors.Cyan)
                        isEligible -> BorderStroke(1.dp, ShadowColors.Emerald.copy(alpha = 0.5f))
                        else -> BorderStroke(1.dp, ShadowColors.PanelSurfaceElevated)
                    }

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) ShadowColors.Cyan.copy(alpha = 0.1f) else ShadowColors.PanelSurface
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = cardBorder,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = isEligible) {
                                viewModel.selectEvidenceItem(item)
                            }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isEligible) ShadowColors.PureIce else ShadowColors.Muted,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                // Eligibility Badge
                                val (badgeText, badgeColor) = when (item.eligibility) {
                                    ShadowEvidenceEligibility.ELIGIBLE -> "ELIGIBLE" to ShadowColors.Emerald
                                    ShadowEvidenceEligibility.ALREADY_EXTRACTED -> "CLAIMED" to ShadowColors.Muted
                                    ShadowEvidenceEligibility.NOT_VERIFIED -> "NOT VERIFIED" to ShadowColors.Crimson
                                    ShadowEvidenceEligibility.INCOMPLETE -> "INCOMPLETE" to ShadowColors.Gold
                                    ShadowEvidenceEligibility.WRONG_ACCOUNT -> "WRONG ACCOUNT" to ShadowColors.Crimson
                                    ShadowEvidenceEligibility.SOURCE_UNAVAILABLE -> "UNAVAILABLE" to ShadowColors.Muted
                                }
                                Surface(
                                    color = badgeColor.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp),
                                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = badgeText,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp
                                        ),
                                        color = badgeColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Source: ${item.sourceType.name}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ShadowColors.Muted
                                )
                                Text(
                                    text = dateFormat.format(Date(item.timestampEpochMs)),
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                                    color = ShadowColors.Muted
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Verification: ${item.verificationStatus}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = if (item.verificationStatus.contains("VERIFIED")) ShadowColors.Cyan else ShadowColors.Muted
                                )
                                if (isEligible) {
                                    Text(
                                        text = if (isSelected) "SELECTED" else "TAP TO EXTRACT",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) ShadowColors.Cyan else ShadowColors.Emerald
                                        )
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
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadAnalytics()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ARMY ANALYTICS", style = MaterialTheme.typography.titleMedium, color = ShadowColors.PureIce) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = ShadowColors.PureIce)
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
                    AnalyticsMetricCard("TOTAL ARMY", "${data.activeShadows}", ShadowColors.Cyan, Modifier.weight(1f))
                    AnalyticsMetricCard("DEPLOYED", "${uiState.activeDeployments.size}", ShadowColors.Emerald, Modifier.weight(1f))
                    AnalyticsMetricCard("POWER", "${data.monarchProfile.shadowArmyPowerRating}", ShadowColors.Gold, Modifier.weight(1f))
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
                                val progress = if (data.activeShadows > 0) count.toFloat() / data.activeShadows else 0f
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
                                val progress = if (data.activeShadows > 0) count.toFloat() / data.activeShadows else 0f
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
    val extractions = uiState.extractionHistory
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SHADOW ARCHIVE & LOGS", style = MaterialTheme.typography.titleMedium, color = ShadowColors.PureIce) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = ShadowColors.PureIce)
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
                                text = when (record.outcome) {
                                    ShadowExtractionOutcome.SUCCESS -> "RECRUITED"
                                    ShadowExtractionOutcome.DUPLICATE_RESOLVED -> "RESONANCE RESOLVED"
                                    ShadowExtractionOutcome.ARMY_CAPACITY_REACHED -> "CAPACITY REACHED"
                                    ShadowExtractionOutcome.ALREADY_CLAIMED -> "ALREADY EXTRACTED"
                                    else -> record.outcome.name
                                },
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = when (record.outcome) {
                                    ShadowExtractionOutcome.SUCCESS -> ShadowColors.Emerald
                                    ShadowExtractionOutcome.DUPLICATE_RESOLVED -> ShadowColors.Cyan
                                    else -> ShadowColors.Crimson
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        if (record.outcome == ShadowExtractionOutcome.DUPLICATE_RESOLVED) {
                            Text(
                                text = "Duplicate Remnant Converted: +${com.example.domain.model.ShadowResonanceConfig.SHARDS_PER_DUPLICATE} Resonance Shards bound to core.",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = ShadowColors.Cyan
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                        }
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
    val settings by viewModel.settingsState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SHADOW REALM SETTINGS", style = MaterialTheme.typography.titleMedium, color = ShadowColors.PureIce) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = ShadowColors.PureIce)
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
                            checked = settings.extractionAlertsEnabled,
                            onCheckedChange = { viewModel.updateExtractionAlerts(it) },
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
                            checked = settings.evolutionAlertsEnabled,
                            onCheckedChange = { viewModel.updateEvolutionAlerts(it) },
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
                            checked = settings.autoRecallOnRest,
                            onCheckedChange = { viewModel.updateAutoRecallOnRest(it) },
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

