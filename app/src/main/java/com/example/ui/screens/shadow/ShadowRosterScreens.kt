package com.example.ui.screens.shadow

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Upgrade
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.engine.ShadowEvolutionEngine
import com.example.domain.model.MonarchSkillCatalog
import com.example.domain.model.Shadow
import com.example.domain.model.ShadowClass
import com.example.domain.model.ShadowDeploymentCategory
import com.example.domain.model.ShadowDeploymentStatus
import com.example.domain.model.ShadowEvolutionStage
import com.example.domain.model.ShadowFormationType
import com.example.domain.model.ShadowQuestCatalog
import com.example.domain.model.ShadowRank
import com.example.domain.model.ShadowRarity
import com.example.domain.model.ShadowResourceType
import com.example.domain.model.ShadowSkillCatalog
import com.example.ui.components.HolographicPanel
import com.example.ui.viewmodel.ShadowRealmViewModel

import com.example.domain.model.ShadowSortOption
import com.example.domain.model.ShadowCollectionOverview
import com.example.domain.model.ShadowResonanceConfig
import com.example.domain.model.ShadowGrowthConfig
import com.example.domain.model.ShadowEvolutionState
import com.example.domain.model.ShadowEvolutionEvaluation
import com.example.domain.model.ShadowGrowthOverview
import com.example.domain.model.ShadowBondConfig
import com.example.domain.model.ShadowBondProfile
import com.example.domain.model.ShadowBondEvent
import com.example.domain.model.ShadowBondMilestone
import com.example.domain.model.ShadowArmyBondOverview
import com.example.domain.model.ShadowPersonality
import com.example.domain.model.ShadowRelationshipStage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ============================================================================
// 1. SHADOW COLLECTION SCREEN
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShadowCollectionScreen(
    viewModel: ShadowRealmViewModel,
    onNavigateBack: () -> Unit,
    onSelectShadow: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val collectionOverview by viewModel.collectionOverview.collectAsState()
    val growthOverview by viewModel.growthOverview.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedClassFilter by remember { mutableStateOf<ShadowClass?>(null) }
    var selectedRarityFilter by remember { mutableStateOf<ShadowRarity?>(null) }
    var selectedRankFilter by remember { mutableStateOf<ShadowRank?>(null) }
    var selectedStageFilter by remember { mutableStateOf<ShadowEvolutionStage?>(null) }
    var selectedDeploymentFilter by remember { mutableStateOf<ShadowDeploymentStatus?>(null) }
    var selectedSortOption by remember { mutableStateOf(ShadowSortOption.RECENTLY_RECRUITED) }
    var showDistributions by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadCollectionOverview()
        viewModel.loadGrowthOverview()
    }

    val filteredShadows = remember(
        uiState.allShadows,
        searchQuery,
        selectedClassFilter,
        selectedRarityFilter,
        selectedRankFilter,
        selectedStageFilter,
        selectedDeploymentFilter,
        selectedSortOption
    ) {
        val filtered = uiState.allShadows.filter { shadow ->
            (searchQuery.isBlank() || shadow.name.contains(searchQuery, ignoreCase = true)) &&
            (selectedClassFilter == null || shadow.shadowClass == selectedClassFilter) &&
            (selectedRarityFilter == null || shadow.rarity == selectedRarityFilter) &&
            (selectedRankFilter == null || shadow.currentRank == selectedRankFilter) &&
            (selectedStageFilter == null || shadow.evolutionStage == selectedStageFilter) &&
            (selectedDeploymentFilter == null || shadow.deploymentStatus == selectedDeploymentFilter)
        }

        when (selectedSortOption) {
            ShadowSortOption.RECENTLY_RECRUITED -> filtered.sortedByDescending { it.extractedAtEpochMs }
            ShadowSortOption.RARITY -> filtered.sortedByDescending { it.rarity.ordinal }
            ShadowSortOption.LEVEL -> filtered.sortedByDescending { it.level }
            ShadowSortOption.RANK -> filtered.sortedByDescending { it.currentRank.order }
            ShadowSortOption.NAME -> filtered.sortedBy { it.name.lowercase() }
        }
    }

    val totalOwned = collectionOverview?.totalOwned ?: uiState.allShadows.size
    val uniqueArchetypes = collectionOverview?.uniqueArchetypesCollected ?: uiState.allShadows.map { it.name }.distinct().size
    val totalCatalog = collectionOverview?.totalArchetypesInCatalog ?: 24
    val completionPct = if (totalCatalog > 0) ((uniqueArchetypes * 100f) / totalCatalog) else 0f
    val resonanceShards = collectionOverview?.duplicateResonanceCount ?: 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SHADOW COLLECTION ($totalOwned)", style = MaterialTheme.typography.titleMedium, color = ShadowColors.PureIce) },
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
                .padding(horizontal = 16.dp)
        ) {
            // Error banner if any
            if (uiState.error != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ShadowColors.Crimson.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, ShadowColors.Crimson),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Error: ${uiState.error}",
                            style = MaterialTheme.typography.bodySmall,
                            color = ShadowColors.PureIce,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearError() }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = ShadowColors.PureIce, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Collection Dashboard Overview Card
            HolographicPanel(
                accentColor = ShadowColors.Cyan,
                secondaryColor = ShadowColors.Violet,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CATALOG PROGRESS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 2.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = ShadowColors.Cyan
                        )
                        Text(
                            text = "${"%.1f".format(completionPct)}% COMPLETE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = ShadowColors.Gold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { (completionPct / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = ShadowColors.Cyan,
                        trackColor = ShadowColors.PanelSurfaceElevated
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Unique Archetypes", style = MaterialTheme.typography.labelSmall, color = ShadowColors.Muted)
                            Text(
                                text = "$uniqueArchetypes / $totalCatalog",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = ShadowColors.PureIce
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "Army Roster", style = MaterialTheme.typography.labelSmall, color = ShadowColors.Muted)
                            Text(
                                text = "$totalOwned Shadows",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = ShadowColors.PureIce
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Resonance Shards", style = MaterialTheme.typography.labelSmall, color = ShadowColors.Muted)
                            Text(
                                text = "$resonanceShards Shards",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = ShadowColors.Cyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Toggle distributions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDistributions = !showDistributions },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (showDistributions) "▲ HIDE DISTRIBUTIONS" else "▼ VIEW CLASS & RARITY METRICS",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                            color = ShadowColors.Muted
                        )
                    }

                    AnimatedVisibility(visible = showDistributions) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            HorizontalDivider(color = ShadowColors.PanelSurfaceElevated, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(6.dp))

                            Text(text = "CLASS BREAKDOWN", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = ShadowColors.Muted)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ShadowClass.entries.forEach { cls ->
                                    val count = collectionOverview?.classDistribution?.get(cls) ?: uiState.allShadows.count { it.shadowClass == cls }
                                    Surface(
                                        color = ShadowColors.classColor(cls).copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(4.dp),
                                        border = BorderStroke(1.dp, ShadowColors.classColor(cls).copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = "${cls.label}: $count",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                                            color = ShadowColors.classColor(cls),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "RARITY BREAKDOWN", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = ShadowColors.Muted)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ShadowRarity.entries.forEach { r ->
                                    val count = collectionOverview?.rarityDistribution?.get(r) ?: uiState.allShadows.count { it.rarity == r }
                                    Surface(
                                        color = ShadowColors.rarityColor(r).copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(4.dp),
                                        border = BorderStroke(1.dp, ShadowColors.rarityColor(r).copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = "${r.label}: $count",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                                            color = ShadowColors.rarityColor(r),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Phase 6.3 — Legion Growth Overview Card
            ShadowGrowthOverviewCard(
                growthOverview = growthOverview,
                shadows = uiState.allShadows
            )

            // Search input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search shadows by name...", color = ShadowColors.Muted) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = ShadowColors.Muted) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = ShadowColors.PureIce,
                    unfocusedTextColor = ShadowColors.PureIce,
                    focusedBorderColor = ShadowColors.Cyan,
                    unfocusedBorderColor = ShadowColors.PanelSurfaceElevated
                )
            )

            // Sorting Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Sort:", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = ShadowColors.Muted)
                ShadowSortOption.entries.forEach { opt ->
                    FilterChip(
                        selected = selectedSortOption == opt,
                        onClick = { selectedSortOption = opt },
                        label = { Text(opt.label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ShadowColors.Cyan.copy(alpha = 0.25f),
                            selectedLabelColor = ShadowColors.Cyan
                        )
                    )
                }
            }

            // Filter Rows (Class, Rarity, Rank, Deployment)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedClassFilter == null && selectedRarityFilter == null && selectedRankFilter == null && selectedDeploymentFilter == null,
                    onClick = {
                        selectedClassFilter = null
                        selectedRarityFilter = null
                        selectedRankFilter = null
                        selectedDeploymentFilter = null
                        selectedStageFilter = null
                    },
                    label = { Text("All Filters", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ShadowColors.Cyan.copy(alpha = 0.2f),
                        selectedLabelColor = ShadowColors.Cyan
                    )
                )

                // Deployment Filter
                FilterChip(
                    selected = selectedDeploymentFilter == ShadowDeploymentStatus.DEPLOYED,
                    onClick = {
                        selectedDeploymentFilter = if (selectedDeploymentFilter == ShadowDeploymentStatus.DEPLOYED) null else ShadowDeploymentStatus.DEPLOYED
                    },
                    label = { Text("Deployed Only", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ShadowColors.Emerald.copy(alpha = 0.2f),
                        selectedLabelColor = ShadowColors.Emerald
                    )
                )

                // Class filter chips
                ShadowClass.entries.forEach { cls ->
                    FilterChip(
                        selected = selectedClassFilter == cls,
                        onClick = { selectedClassFilter = if (selectedClassFilter == cls) null else cls },
                        label = { Text(cls.label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ShadowColors.classColor(cls).copy(alpha = 0.2f),
                            selectedLabelColor = ShadowColors.classColor(cls)
                        )
                    )
                }

                // Rarity filter chips
                ShadowRarity.entries.forEach { r ->
                    FilterChip(
                        selected = selectedRarityFilter == r,
                        onClick = { selectedRarityFilter = if (selectedRarityFilter == r) null else r },
                        label = { Text(r.label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ShadowColors.rarityColor(r).copy(alpha = 0.2f),
                            selectedLabelColor = ShadowColors.rarityColor(r)
                        )
                    )
                }

                // Rank filter chips
                ShadowRank.entries.forEach { rank ->
                    FilterChip(
                        selected = selectedRankFilter == rank,
                        onClick = { selectedRankFilter = if (selectedRankFilter == rank) null else rank },
                        label = { Text(rank.label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ShadowColors.rankColor(rank).copy(alpha = 0.2f),
                            selectedLabelColor = ShadowColors.rankColor(rank)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Roster list / Loading / Empty
            if (uiState.isLoading && uiState.allShadows.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ShadowColors.Cyan)
                }
            } else if (filteredShadows.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (uiState.allShadows.isEmpty()) "No Shadows recruited yet.\nUse Shadow Extraction to summon your army."
                        else "No shadows match the selected search or filter criteria.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ShadowColors.Muted,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredShadows, key = { it.shadowId }) { shadow ->
                        ShadowCard(
                            shadow = shadow,
                            onClick = { onSelectShadow(shadow.shadowId) }
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// PHASE 6.3 — SHADOW GROWTH OVERVIEW COMPONENT
// ============================================================================

@Composable
fun ShadowGrowthOverviewCard(
    growthOverview: ShadowGrowthOverview?,
    shadows: List<Shadow>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val total = growthOverview?.totalShadows ?: shadows.size
    val avgLevel = growthOverview?.averageLevel ?: (if (shadows.isNotEmpty()) shadows.map { it.level }.average().toFloat() else 1f)
    val readyToEvolve = growthOverview?.readyToEvolveCount ?: 0

    HolographicPanel(
        accentColor = ShadowColors.Violet,
        secondaryColor = ShadowColors.Cyan,
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LEGION GROWTH DASHBOARD",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = ShadowColors.Violet
                )
                Text(
                    text = if (readyToEvolve > 0) "$readyToEvolve READY" else "OPTIMAL",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (readyToEvolve > 0) ShadowColors.Emerald else ShadowColors.Muted
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Total Legion", style = MaterialTheme.typography.labelSmall, color = ShadowColors.Muted)
                    Text(
                        text = "$total Units",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = ShadowColors.PureIce
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Average Level", style = MaterialTheme.typography.labelSmall, color = ShadowColors.Muted)
                    Text(
                        text = "Lv. ${"%.1f".format(Locale.getDefault(), avgLevel)}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = ShadowColors.Cyan
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Ready to Evolve", style = MaterialTheme.typography.labelSmall, color = ShadowColors.Muted)
                    Text(
                        text = "$readyToEvolve Shadows",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (readyToEvolve > 0) ShadowColors.Emerald else ShadowColors.PureIce
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (expanded) "▲ HIDE STAGE DISTRIBUTION" else "▼ VIEW EVOLUTION STAGES & RECENT ASCENSIONS",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                    color = ShadowColors.Muted
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    HorizontalDivider(color = ShadowColors.PanelSurfaceElevated, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(6.dp))

                    Text(text = "STAGE DISTRIBUTION", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = ShadowColors.Muted)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ShadowEvolutionStage.entries.forEach { stage ->
                            val count = growthOverview?.stageDistribution?.get(stage)
                                ?: shadows.count { it.evolutionStage == stage }
                            Surface(
                                color = ShadowColors.Violet.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, ShadowColors.Violet.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "${stage.label}: $count",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                                    color = ShadowColors.PureIce,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    val evolvedShadows = shadows.filter { it.evolutionHistoryJson != "[]" }
                    if (evolvedShadows.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "RECENTLY EVOLVED SHADOWS", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = ShadowColors.Muted)
                        Spacer(modifier = Modifier.height(4.dp))
                        evolvedShadows.take(3).forEach { shadow ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = shadow.name, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = ShadowColors.PureIce)
                                Text(text = shadow.evolutionStage.label, style = MaterialTheme.typography.labelSmall, color = ShadowColors.Cyan)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 2. SHADOW DETAIL SCREEN
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShadowDetailScreen(
    shadowId: String,
    viewModel: ShadowRealmViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToEvolution: (String) -> Unit,
    onNavigateToSkillTree: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val shadow = uiState.allShadows.find { it.shadowId == shadowId }
    var showDeployDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(shadow?.name ?: "SHADOW PROFILE", style = MaterialTheme.typography.titleMedium, color = ShadowColors.PureIce) },
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
        if (shadow == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Shadow not found", color = ShadowColors.Muted)
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
                // Hero card
                HolographicPanel(
                    accentColor = ShadowColors.rankColor(shadow.currentRank),
                    secondaryColor = ShadowColors.classColor(shadow.shadowClass),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val stageCap = ShadowGrowthConfig.getStageLevelCap(shadow.evolutionStage)
                        val nextStage = shadow.evolutionStage.nextStage()
                        val isMaxStage = nextStage == null
                        val isStageCapReached = shadow.level >= stageCap && !isMaxStage
                        val isReadyToEvolve = !isMaxStage && shadow.level >= nextStage!!.minLevel && shadow.currentRank.order >= nextStage.minRank.order

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = shadow.name,
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    color = ShadowColors.PureIce
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    ShadowClassBadge(shadow.shadowClass)
                                    ShadowRarityBadge(shadow.rarity)
                                    if (isMaxStage) {
                                        Surface(
                                            color = ShadowColors.Gold.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp),
                                            border = BorderStroke(1.dp, ShadowColors.Gold)
                                        ) {
                                            Text(
                                                text = "MAX STAGE",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                                color = ShadowColors.Gold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    } else if (isReadyToEvolve) {
                                        Surface(
                                            color = ShadowColors.Emerald.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp),
                                            border = BorderStroke(1.dp, ShadowColors.Emerald)
                                        ) {
                                            Text(
                                                text = "READY TO EVOLVE",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                                color = ShadowColors.Emerald,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    } else if (isStageCapReached) {
                                        Surface(
                                            color = ShadowColors.Amber.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp),
                                            border = BorderStroke(1.dp, ShadowColors.Amber)
                                        ) {
                                            Text(
                                                text = "STAGE CAP REACHED",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                                color = ShadowColors.Amber,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            ShadowRankBadge(shadow.currentRank)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Level & XP Bar with Stage Cap
                        val nextXp = ShadowEvolutionEngine.calculateNextLevelXp(shadow.level)
                        val progress = if (nextXp > 0) (shadow.shadowXp.toFloat() / nextXp).coerceIn(0f, 1f) else 1f

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "LEVEL ${shadow.level} (Cap: Lv. $stageCap)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = ShadowColors.Cyan)
                            Text(text = "${shadow.shadowXp} / $nextXp XP", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = ShadowColors.PureIce)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = if (isStageCapReached) ShadowColors.Amber else ShadowColors.Cyan,
                            trackColor = ShadowColors.PanelSurfaceElevated
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Stage: ${shadow.evolutionStage.label}", style = MaterialTheme.typography.bodySmall, color = ShadowColors.Muted)
                            Text(text = "Power: ${shadow.calculatePowerRating()} PWR", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = ShadowColors.Gold)
                        }
                    }
                }

                // Passive Ability
                Text(text = "PASSIVE ABILITY", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = ShadowColors.Muted)
                Card(
                    colors = CardDefaults.cardColors(containerColor = ShadowColors.PanelSurface),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ShadowColors.Cyan.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = shadow.passiveAbility, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = ShadowColors.Cyan)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Class passive granting strategic assistance during real-world sessions.", style = MaterialTheme.typography.bodySmall, color = ShadowColors.Muted)
                    }
                }

                // 6 Stats Radar Breakdown
                Text(text = "SHADOW ATTRIBUTES", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = ShadowColors.Muted)
                Card(
                    colors = CardDefaults.cardColors(containerColor = ShadowColors.PanelSurface),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ShadowColors.PanelSurfaceElevated),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ShadowStatsDisplay(shadow.stats, modifier = Modifier.padding(16.dp))
                }

                // Recruitment Provenance Card
                Text(text = "RECRUITMENT PROVENANCE", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = ShadowColors.Muted)
                Card(
                    colors = CardDefaults.cardColors(containerColor = ShadowColors.PanelSurface),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ShadowColors.Cyan.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Recruitment Timestamp", style = MaterialTheme.typography.bodySmall, color = ShadowColors.Muted)
                            Text(
                                text = dateFormat.format(Date(shadow.extractedAtEpochMs)),
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = ShadowColors.PureIce
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Extraction Event ID", style = MaterialTheme.typography.bodySmall, color = ShadowColors.Muted)
                            Text(
                                text = shadow.extractionEventId,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
                                color = ShadowColors.Cyan
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Initial Rarity", style = MaterialTheme.typography.bodySmall, color = ShadowColors.Muted)
                            ShadowRarityBadge(shadow.rarity)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Active Deployment", style = MaterialTheme.typography.bodySmall, color = ShadowColors.Muted)
                            Text(
                                text = shadow.deploymentStatus.name,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (shadow.deploymentStatus == ShadowDeploymentStatus.DEPLOYED) ShadowColors.Emerald else ShadowColors.Muted
                            )
                        }
                    }
                }

                // Evolution & Ascension History Card
                val historyList = remember(shadow.evolutionHistoryJson) {
                    try {
                        kotlinx.serialization.json.Json.decodeFromString<List<String>>(shadow.evolutionHistoryJson)
                    } catch (_: Exception) { emptyList() }
                }
                if (historyList.isNotEmpty()) {
                    Text(text = "EVOLUTION & ASCENSION HISTORY", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = ShadowColors.Muted)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ShadowColors.PanelSurface),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ShadowColors.Violet.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
                            historyList.forEach { entry ->
                                val parts = entry.split(":")
                                val stageName = parts.getOrNull(0) ?: "UNKNOWN"
                                val timestamp = parts.getOrNull(1)?.toLongOrNull() ?: 0L
                                val stageLabel = ShadowEvolutionStage.entries.find { it.name == stageName }?.label ?: stageName
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = "Ascended to $stageLabel", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = ShadowColors.Cyan)
                                    if (timestamp > 0L) {
                                        Text(text = dateFormat.format(Date(timestamp)), style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp), color = ShadowColors.Muted)
                                    }
                                }
                            }
                        }
                    }
                }

                // Action buttons
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { onNavigateToEvolution(shadow.shadowId) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = ShadowColors.Violet)
                    ) {
                        Icon(Icons.Rounded.Upgrade, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("EVOLVE / RANK UP")
                    }

                    Button(
                        onClick = { onNavigateToSkillTree(shadow.shadowId) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ShadowColors.Cyan,
                            contentColor = ShadowColors.Obsidian
                        )
                    ) {
                        Icon(Icons.Rounded.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SKILL TREE")
                    }
                }

                // Quick Deploy / Recall
                if (shadow.deploymentStatus == ShadowDeploymentStatus.DEPLOYED) {
                    val activeDeployment = uiState.activeDeployments.find { it.shadowId == shadow.shadowId }
                    OutlinedButton(
                        onClick = {
                            activeDeployment?.let { viewModel.recallShadow(it.deploymentId) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ShadowColors.Crimson),
                        border = BorderStroke(1.dp, ShadowColors.Crimson.copy(alpha = 0.5f))
                    ) {
                        Text("RECALL FROM DEPLOYMENT")
                    }
                } else {
                    OutlinedButton(
                        onClick = { showDeployDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ShadowColors.Emerald),
                        border = BorderStroke(1.dp, ShadowColors.Emerald.copy(alpha = 0.5f))
                    ) {
                        Text("DEPLOY SHADOW")
                    }
                }

                // Deployment dialog
                if (showDeployDialog) {
                    DeployShadowDialog(
                        shadow = shadow,
                        onDismiss = { showDeployDialog = false },
                        onDeploy = { category, role ->
                            viewModel.deployShadow(shadow.shadowId, category, role)
                            showDeployDialog = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DeployShadowDialog(
    shadow: Shadow,
    onDismiss: () -> Unit,
    onDeploy: (ShadowDeploymentCategory, String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(ShadowDeploymentCategory.DISCIPLINE) }
    var roleInput by remember { mutableStateOf("Strategic Supporter") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Deploy ${shadow.name}", color = ShadowColors.PureIce) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Select Strategic Category:", style = MaterialTheme.typography.bodySmall, color = ShadowColors.Muted)
                ShadowDeploymentCategory.entries.forEach { category ->
                    Surface(
                        color = if (selectedCategory == category) ShadowColors.Cyan.copy(alpha = 0.2f) else ShadowColors.PanelSurface,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, if (selectedCategory == category) ShadowColors.Cyan else ShadowColors.PanelSurfaceElevated),
                        modifier = Modifier.fillMaxWidth().clickable { selectedCategory = category }
                    ) {
                        Text(
                            text = category.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (selectedCategory == category) ShadowColors.Cyan else ShadowColors.PureIce,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = roleInput,
                    onValueChange = { roleInput = it },
                    label = { Text("Strategic Role") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onDeploy(selectedCategory, roleInput) },
                colors = ButtonDefaults.buttonColors(containerColor = ShadowColors.Cyan, contentColor = ShadowColors.Obsidian)
            ) {
                Text("CONFIRM DEPLOYMENT")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("CANCEL") }
        },
        containerColor = ShadowColors.PanelSurface
    )
}

// ============================================================================
// 3. SHADOW EVOLUTION SCREEN (Phase 6.3 Evolution Chamber)
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShadowEvolutionScreen(
    shadowId: String,
    viewModel: ShadowRealmViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val shadow = uiState.allShadows.find { it.shadowId == shadowId }
    val evaluation by viewModel.evolutionEvaluation.collectAsState()
    val isEvolving by viewModel.isEvolving.collectAsState()
    var showConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(shadowId) {
        viewModel.loadEvolutionEvaluation(shadowId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EVOLUTION CHAMBER", style = MaterialTheme.typography.titleMedium, color = ShadowColors.PureIce) },
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
        if (shadow == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Shadow not found", color = ShadowColors.Muted)
            }
        } else {
            val eval = evaluation
            val targetStage = eval?.targetStage
            val isMaxStage = eval?.state == ShadowEvolutionState.MAX_STAGE_REACHED

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Current Stage vs Target Stage Hero Card
                HolographicPanel(
                    accentColor = ShadowColors.Violet,
                    secondaryColor = ShadowColors.Cyan,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = shadow.name,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = ShadowColors.PureIce
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    ShadowClassBadge(shadow.shadowClass)
                                    ShadowRarityBadge(shadow.rarity)
                                }
                            }
                            ShadowRankBadge(shadow.currentRank)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "CURRENT STAGE", style = MaterialTheme.typography.labelSmall, color = ShadowColors.Muted)
                                Text(
                                    text = shadow.evolutionStage.label,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = ShadowColors.PureIce
                                )
                                Text(text = "Lv. ${shadow.level}", style = MaterialTheme.typography.bodySmall, color = ShadowColors.Cyan)
                            }

                            Icon(
                                Icons.Rounded.Upgrade,
                                contentDescription = null,
                                tint = if (eval?.isEligible == true) ShadowColors.Emerald else ShadowColors.Violet,
                                modifier = Modifier.size(36.dp)
                            )

                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "TARGET STAGE", style = MaterialTheme.typography.labelSmall, color = ShadowColors.Muted)
                                Text(
                                    text = targetStage?.label ?: "MAX ASCENDED",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (targetStage != null) ShadowColors.Cyan else ShadowColors.Gold
                                )
                                if (targetStage != null) {
                                    Text(
                                        text = "Req. Lv. ${targetStage.minLevel} / ${targetStage.minRank.label}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ShadowColors.Muted
                                    )
                                }
                            }
                        }
                    }
                }

                // Status Banner
                val bannerColor = when {
                    isMaxStage -> ShadowColors.Gold
                    eval?.isEligible == true -> ShadowColors.Emerald
                    else -> ShadowColors.Amber
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = bannerColor.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, bannerColor.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when {
                                isMaxStage -> Icons.Rounded.Star
                                eval?.isEligible == true -> Icons.Rounded.CheckCircle
                                else -> Icons.Rounded.Lock
                            },
                            contentDescription = null,
                            tint = bannerColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = when {
                                    isMaxStage -> "MAXIMUM STAGE ACHIEVED"
                                    eval?.isEligible == true -> "READY TO EVOLVE"
                                    else -> eval?.state?.label ?: "EVOLUTION REQUIREMENTS"
                                },
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = bannerColor
                            )
                            if (eval != null && eval.blockers.isNotEmpty() && !isMaxStage && !eval.isEligible) {
                                Text(
                                    text = eval.blockers.joinToString(" • "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ShadowColors.PureIce.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                // Requirements Checklist
                if (!isMaxStage && eval != null) {
                    Text(
                        text = "EVOLUTION REQUIREMENTS",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, letterSpacing = 1.sp),
                        color = ShadowColors.Muted
                    )

                    Card(
                        colors = CardDefaults.cardColors(containerColor = ShadowColors.PanelSurface),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ShadowColors.PanelSurfaceElevated),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Level requirement
                            val levelMet = shadow.level >= eval.requiredLevel
                            RequirementItem(
                                title = "Reach Level ${eval.requiredLevel}",
                                value = "Lv. ${shadow.level} / ${eval.requiredLevel}",
                                isMet = levelMet
                            )

                            // Rank requirement
                            val rankMet = shadow.currentRank.order >= eval.requiredRank.order
                            RequirementItem(
                                title = "Rank ${eval.requiredRank.label}",
                                value = "${shadow.currentRank.label} (Req: ${eval.requiredRank.label})",
                                isMet = rankMet
                            )

                            HorizontalDivider(color = ShadowColors.PanelSurfaceElevated)

                            // Resource costs
                            eval.resourceCosts.forEach { (resourceType, cost) ->
                                val balance = eval.currentBalances[resourceType] ?: 0
                                val resourceMet = balance >= cost
                                RequirementItem(
                                    title = resourceType.label,
                                    value = "$balance / $cost",
                                    isMet = resourceMet
                                )
                            }
                        }
                    }

                    // Predicted Attribute Increases Card
                    if (eval.statPreview != null) {
                        Text(
                            text = "PREDICTED ATTRIBUTE ASCENSION",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, letterSpacing = 1.sp),
                            color = ShadowColors.Muted
                        )

                        Card(
                            colors = CardDefaults.cardColors(containerColor = ShadowColors.PanelSurface),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, ShadowColors.Cyan.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                val current = shadow.stats
                                val preview = eval.statPreview
                                val bonus = ShadowGrowthConfig.getStageStatBonusIncrement(eval.targetStage!!)

                                Text(
                                    text = "Stage Ascension Bonus (+${bonus.power} to all attributes)",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = ShadowColors.Cyan
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    StatAscensionItem("PWR", current.power, preview.power)
                                    StatAscensionItem("END", current.endurance, preview.endurance)
                                    StatAscensionItem("FOC", current.focus, preview.focus)
                                    StatAscensionItem("DIS", current.discipline, preview.discipline)
                                    StatAscensionItem("WIS", current.wisdom, preview.wisdom)
                                    StatAscensionItem("ADP", current.adaptability, preview.adaptability)
                                }
                            }
                        }
                    }
                }

                // Evolution Action Button
                if (!isMaxStage) {
                    Button(
                        onClick = { showConfirmDialog = true },
                        enabled = (eval?.isEligible == true) && !isEvolving,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ShadowColors.Violet,
                            disabledContainerColor = ShadowColors.PanelSurfaceElevated,
                            contentColor = ShadowColors.PureIce,
                            disabledContentColor = ShadowColors.Muted
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isEvolving) {
                            CircularProgressIndicator(color = ShadowColors.PureIce, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("COMMUNING WITH VOID...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Rounded.Upgrade, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (eval?.isEligible == true) "CONFIRM SHADOW EVOLUTION" else "EVOLUTION REQUIREMENTS INCOMPLETE",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Explicit Confirmation Dialog
                if (showConfirmDialog && eval?.targetStage != null) {
                    AlertDialog(
                        onDismissRequest = { if (!isEvolving) showConfirmDialog = false },
                        title = {
                            Text(
                                "CONFIRM SHADOW EVOLUTION",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = ShadowColors.Cyan
                            )
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Ascend ${shadow.name} (${shadow.shadowClass.label}) to ${eval.targetStage.label}?",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ShadowColors.PureIce
                                )
                                HorizontalDivider(color = ShadowColors.PanelSurfaceElevated)
                                Text(
                                    text = "Resource Investment:",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = ShadowColors.Muted
                                )
                                eval.resourceCosts.forEach { (res, cost) ->
                                    Text(
                                        text = "• ${res.label}: $cost",
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                        color = ShadowColors.Cyan
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                val bonus = ShadowGrowthConfig.getStageStatBonusIncrement(eval.targetStage)
                                Text(
                                    text = "Permanent Attribute Gain: +${bonus.power} to all stats",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = ShadowColors.Emerald
                                )
                                Text(
                                    text = "Monarch XP Reward: +200 XP",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = ShadowColors.Gold
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.confirmEvolution(shadow.shadowId) { success, _ ->
                                        if (success) {
                                            showConfirmDialog = false
                                        }
                                    }
                                },
                                enabled = !isEvolving,
                                colors = ButtonDefaults.buttonColors(containerColor = ShadowColors.Violet)
                            ) {
                                if (isEvolving) {
                                    CircularProgressIndicator(color = ShadowColors.PureIce, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Text("ARISE & EVOLVE")
                                }
                            }
                        },
                        dismissButton = {
                            OutlinedButton(
                                onClick = { showConfirmDialog = false },
                                enabled = !isEvolving
                            ) {
                                Text("CANCEL")
                            }
                        },
                        containerColor = ShadowColors.PanelSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun StatAscensionItem(label: String, current: Int, preview: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = ShadowColors.Muted)
        Text(text = "$current", style = MaterialTheme.typography.bodySmall, color = ShadowColors.PureIce)
        Text(text = "→ $preview", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = ShadowColors.Emerald)
    }
}

@Composable
private fun RequirementItem(title: String, value: String, isMet: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isMet) Icons.Rounded.CheckCircle else Icons.Rounded.Lock,
                contentDescription = null,
                tint = if (isMet) ShadowColors.Emerald else ShadowColors.Muted,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = title, style = MaterialTheme.typography.bodyMedium, color = ShadowColors.PureIce)
        }
        Text(text = value, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = if (isMet) ShadowColors.Emerald else ShadowColors.Muted)
    }
}

// ============================================================================
// 4. SHADOW SKILL TREE SCREEN
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShadowSkillTreeScreen(
    shadowId: String,
    viewModel: ShadowRealmViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val shadow = uiState.allShadows.find { it.shadowId == shadowId }
    val catalog = viewModel.getShadowSkillCatalog()
    val classSkills = catalog

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${shadow?.name ?: "SHADOW"} SKILL TREE", style = MaterialTheme.typography.titleMedium, color = ShadowColors.PureIce) },
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
            items(classSkills) { skill ->
                val isUnlocked = shadow?.activeSkillIds?.contains(skill.skillId) == true
                val canUnlock = !isUnlocked &&
                    (shadow?.level ?: 0) >= skill.requiredShadowLevel &&
                    (shadow?.currentRank?.order ?: 0) >= skill.requiredShadowRank.order &&
                    skill.prerequisiteSkillIds.all { it in (shadow?.activeSkillIds ?: emptyList()) }

                Card(
                    colors = CardDefaults.cardColors(containerColor = ShadowColors.PanelSurface),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isUnlocked) ShadowColors.Emerald.copy(alpha = 0.6f)
                        else if (canUnlock) ShadowColors.Cyan.copy(alpha = 0.4f)
                        else ShadowColors.PanelSurfaceElevated
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isUnlocked) ShadowColors.Emerald.copy(alpha = 0.15f) else ShadowColors.PanelSurfaceElevated)
                        ) {
                            Icon(
                                imageVector = if (isUnlocked) Icons.Rounded.CheckCircle else Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = if (isUnlocked) ShadowColors.Emerald else ShadowColors.Muted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = skill.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = ShadowColors.PureIce)
                            Text(text = skill.description, style = MaterialTheme.typography.bodySmall, color = ShadowColors.Muted)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(text = "Requires ${skill.requiredShadowRank.label} • Lv. ${skill.requiredShadowLevel}", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = ShadowColors.Cyan)
                        }
                        if (canUnlock && shadow != null) {
                            Button(
                                onClick = { viewModel.unlockShadowSkill(shadow.shadowId, skill.skillId) },
                                colors = ButtonDefaults.buttonColors(containerColor = ShadowColors.Cyan, contentColor = ShadowColors.Obsidian)
                            ) {
                                Text("UNLOCK", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 5. SHADOW DEPLOYMENT SCREEN
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShadowDeploymentScreen(
    viewModel: ShadowRealmViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SHADOW DEPLOYMENT", style = MaterialTheme.typography.titleMedium, color = ShadowColors.PureIce) },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "ACTIVE DEPLOYMENTS (${uiState.activeDeployments.size})",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = ShadowColors.Muted
                )
            }

            if (uiState.activeDeployments.isEmpty()) {
                item {
                    Surface(
                        color = ShadowColors.PanelSurface,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No shadows currently deployed. Assign idle shadows to strategic development categories.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ShadowColors.Muted,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(uiState.activeDeployments) { deployment ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ShadowColors.PanelSurface),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ShadowColors.Emerald.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = deployment.shadowName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = ShadowColors.PureIce)
                                Text(text = "Category: ${deployment.category.label}", style = MaterialTheme.typography.bodySmall, color = ShadowColors.Emerald)
                                Text(text = "Role: ${deployment.role}", style = MaterialTheme.typography.bodySmall, color = ShadowColors.Muted)
                            }
                            Button(
                                onClick = { viewModel.recallShadow(deployment.deploymentId) },
                                colors = ButtonDefaults.buttonColors(containerColor = ShadowColors.PanelSurfaceElevated, contentColor = ShadowColors.Crimson)
                            ) {
                                Text("RECALL", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 6. ARMY FORMATION MANAGER SCREEN
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArmyFormationManagerScreen(
    viewModel: ShadowRealmViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ARMY FORMATIONS", style = MaterialTheme.typography.titleMedium, color = ShadowColors.PureIce) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = ShadowColors.PureIce)
                    }
                },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = "Create Formation", tint = ShadowColors.Cyan)
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "CONFIGURED FORMATIONS (${uiState.formations.size})",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = ShadowColors.Muted
                )
            }

            if (uiState.formations.isEmpty()) {
                item {
                    Surface(
                        color = ShadowColors.PanelSurface,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No custom formations configured. Tap '+' to create your first tactical formation.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ShadowColors.Muted,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(uiState.formations) { formation ->
                    val isActive = formation.isActive
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ShadowColors.PanelSurface),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (isActive) ShadowColors.Cyan else ShadowColors.PanelSurfaceElevated),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(text = formation.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = ShadowColors.PureIce)
                                    Text(text = formation.formationType.label, style = MaterialTheme.typography.bodySmall, color = ShadowColors.Cyan)
                                }
                                if (isActive) {
                                    Surface(
                                        color = ShadowColors.Cyan.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp),
                                        border = BorderStroke(1.dp, ShadowColors.Cyan)
                                    ) {
                                        Text("ACTIVE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = ShadowColors.Cyan, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                } else {
                                    Button(
                                        onClick = { viewModel.setActiveFormation(formation.formationId) },
                                        colors = ButtonDefaults.buttonColors(containerColor = ShadowColors.PanelSurfaceElevated)
                                    ) {
                                        Text("ACTIVATE", fontSize = 11.sp, color = ShadowColors.PureIce)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = formation.description, style = MaterialTheme.typography.bodySmall, color = ShadowColors.Muted)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = "Members: ${formation.memberShadowIds.size} / ${formation.maxSlots} slots", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = ShadowColors.Muted)
                        }
                    }
                }
            }
        }

        if (showCreateDialog) {
            CreateFormationDialog(
                onDismiss = { showCreateDialog = false },
                onCreate = { name, type, desc ->
                    viewModel.createFormation(name, type, desc, emptyList())
                    showCreateDialog = false
                }
            )
        }
    }
}

@Composable
private fun CreateFormationDialog(
    onDismiss: () -> Unit,
    onCreate: (String, ShadowFormationType, String) -> Unit
) {
    var name by remember { mutableStateOf("Vanguard Strike Team") }
    var selectedType by remember { mutableStateOf(ShadowFormationType.BALANCED_FORMATION) }
    var description by remember { mutableStateOf("Balanced physical and focus army formation") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Formation", color = ShadowColors.PureIce) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Formation Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name, selectedType, description) },
                colors = ButtonDefaults.buttonColors(containerColor = ShadowColors.Cyan, contentColor = ShadowColors.Obsidian)
            ) {
                Text("CREATE")
            }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("CANCEL") } },
        containerColor = ShadowColors.PanelSurface
    )
}

// ============================================================================
// 7. MONARCH SKILL TREE SCREEN
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonarchSkillTreeScreen(
    viewModel: ShadowRealmViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val skills = viewModel.getMonarchSkillCatalog()
    val unlockedIds = uiState.unlockedMonarchSkills.map { it.skillId }.toSet()
    val monarchLevel = uiState.monarchProfile?.monarchLevel ?: 1
    val monarchAuthority = uiState.monarchProfile?.monarchAuthority ?: 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MONARCH SKILL TREE", style = MaterialTheme.typography.titleMedium, color = ShadowColors.PureIce) },
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
                    text = "SOVEREIGN MONARCH ABILITIES (8)",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = ShadowColors.Muted
                )
            }

            items(skills) { skill ->
                val isUnlocked = skill.skillId in unlockedIds
                val canUnlock = !isUnlocked &&
                    monarchLevel >= skill.requiredMonarchLevel &&
                    monarchAuthority >= skill.requiredAuthority &&
                    (skill.prerequisiteSkillId == null || skill.prerequisiteSkillId in unlockedIds)

                Card(
                    colors = CardDefaults.cardColors(containerColor = ShadowColors.PanelSurface),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isUnlocked) ShadowColors.Gold.copy(alpha = 0.6f)
                        else if (canUnlock) ShadowColors.Cyan.copy(alpha = 0.5f)
                        else ShadowColors.PanelSurfaceElevated
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isUnlocked) Icons.Rounded.CheckCircle else Icons.Rounded.Lock,
                                    contentDescription = null,
                                    tint = if (isUnlocked) ShadowColors.Gold else ShadowColors.Muted,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = skill.name, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = ShadowColors.PureIce)
                            }
                            if (isUnlocked) {
                                Surface(color = ShadowColors.Gold.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                    Text("UNLOCKED", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = ShadowColors.Gold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            } else if (canUnlock) {
                                Button(
                                    onClick = { viewModel.unlockMonarchSkill(skill.skillId) },
                                    colors = ButtonDefaults.buttonColors(containerColor = ShadowColors.Gold, contentColor = ShadowColors.Obsidian)
                                ) {
                                    Text("AWAKEN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = skill.description, style = MaterialTheme.typography.bodySmall, color = ShadowColors.Muted)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Requires Monarch Lv. ${skill.requiredMonarchLevel} | Authority: ${skill.requiredAuthority} pts",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = ShadowColors.Cyan
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// 8. SHADOW QUEST BOARD SCREEN
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShadowQuestBoardScreen(
    viewModel: ShadowRealmViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val questPairs = uiState.questProgress

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MONARCH QUESTS", style = MaterialTheme.typography.titleMedium, color = ShadowColors.PureIce) },
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
                    text = "DETERMINISTIC MONARCH QUESTS (${questPairs.size})",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = ShadowColors.Muted
                )
            }

            items(questPairs) { (quest, progress) ->
                val current = progress?.currentValue ?: 0
                val target = quest.targetValue
                val isCompleted = progress?.isCompleted == true
                val progressFraction = (current.toFloat() / target).coerceIn(0f, 1f)

                Card(
                    colors = CardDefaults.cardColors(containerColor = ShadowColors.PanelSurface),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (isCompleted) ShadowColors.Emerald.copy(alpha = 0.5f) else ShadowColors.PanelSurfaceElevated),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(text = quest.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = ShadowColors.PureIce)
                            if (isCompleted) {
                                Surface(color = ShadowColors.Emerald.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                    Text("COMPLETED", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = ShadowColors.Emerald, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = quest.description, style = MaterialTheme.typography.bodySmall, color = ShadowColors.Muted)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "PROGRESS: $current / $target", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = ShadowColors.Muted)
                            Text(text = "+${quest.monarchXpReward} Monarch XP | +${quest.shadowEssenceReward} Essence", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = ShadowColors.Gold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = if (isCompleted) ShadowColors.Emerald else ShadowColors.Violet,
                            trackColor = ShadowColors.PanelSurfaceElevated
                        )
                    }
                }
            }
        }
    }
}

