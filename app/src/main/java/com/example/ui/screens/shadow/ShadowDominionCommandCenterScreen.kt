package com.example.ui.screens.shadow

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.domain.engine.ShadowDominionResearchEngine
import com.example.domain.model.CitadelUpgradeType
import com.example.domain.model.DominionAiBriefing
import com.example.domain.model.DominionOverview
import com.example.domain.model.DominionTerritoryDetail
import com.example.domain.model.ShadowDominionActivity
import com.example.domain.model.ShadowDominionConfig
import com.example.domain.model.ShadowDominionResearch
import com.example.domain.model.ShadowDominionStage
import com.example.domain.model.ShadowGarrison
import com.example.domain.model.ShadowInfrastructure
import com.example.domain.model.ShadowInfrastructureType
import com.example.domain.model.ShadowResearchBranch
import com.example.domain.model.ShadowResourceType
import com.example.domain.model.ShadowSquad
import com.example.domain.model.ShadowTerritory
import com.example.domain.model.ShadowTerritoryState
import com.example.ui.viewmodel.ShadowRealmViewModel

// ============================================================================
// PHASE 6.6 — SHADOW MONARCH DOMINION COMMAND CENTER SCREEN
// ============================================================================

private val DominionObsidian = Color(0xFF070A13)
private val DominionNavy = Color(0xFF0D1424)
private val DominionCardBg = Color(0xFF111A30)
private val DominionCyan = Color(0xFF00E5FF)
private val DominionViolet = Color(0xFF8B5CF6)
private val DominionGold = Color(0xFFFFD700)
private val DominionCrimson = Color(0xFFFF1744)
private val DominionGreen = Color(0xFF00E676)
private val DominionSurfaceMuted = Color(0xFF1E293B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShadowDominionCommandCenterScreen(
    viewModel: ShadowRealmViewModel,
    onNavigateBack: () -> Unit
) {
    val overview by viewModel.dominionOverview.collectAsState()
    val territories by viewModel.allTerritories.collectAsState()
    val selectedTerritoryDetail by viewModel.selectedTerritoryDetail.collectAsState()
    val infrastructures by viewModel.allInfrastructures.collectAsState()
    val garrisons by viewModel.allGarrisons.collectAsState()
    val researchList by viewModel.allResearch.collectAsState()
    val activities by viewModel.dominionActivities.collectAsState()
    val aiBriefing by viewModel.dominionAiBriefing.collectAsState()
    val allSquads by viewModel.allSquads.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("REALM MAP", "CITADEL", "INFRASTRUCTURE", "GARRISONS", "RESEARCH", "ACTIVITY LOG")

    var showExploreDialog by remember { mutableStateOf<ShadowTerritory?>(null) }
    var showConstructDialog by remember { mutableStateOf<ShadowTerritory?>(null) }
    var showGarrisonDialog by remember { mutableStateOf<ShadowTerritory?>(null) }
    var showCitadelUpgradeDialog by remember { mutableStateOf<CitadelUpgradeType?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadDominionOverview()
        if (territories.isNotEmpty() && selectedTerritoryDetail == null) {
            viewModel.selectTerritory(territories.first().territoryId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "SHADOW DOMINION",
                                color = DominionCyan,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            overview?.dominion?.let { dom ->
                                Box(
                                    modifier = Modifier
                                        .background(DominionViolet.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                        .border(1.dp, DominionViolet.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = dom.developmentStage.stageName.uppercase(),
                                        color = DominionViolet,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                        overview?.dominion?.let { dom ->
                            Text(
                                text = "Citadel Level ${dom.citadelLevel} • Controlled: ${dom.controlledTerritoriesCount}/${territories.size} Territories",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = DominionCyan)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.initializeDominion() }) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Refresh", tint = DominionCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DominionObsidian)
            )
        },
        containerColor = DominionObsidian
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Resource Bar
            DominionResourceHeader(overview = overview)

            // Sub-tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = DominionNavy,
                contentColor = DominionCyan,
                edgePadding = 12.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = DominionCyan
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 11.sp,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                fontFamily = FontFamily.Monospace,
                                color = if (selectedTabIndex == index) DominionCyan else Color.White.copy(alpha = 0.6f)
                            )
                        }
                    )
                }
            }

            // Tab Content
            when (selectedTabIndex) {
                0 -> RealmMapTab(
                    territories = territories,
                    selectedTerritoryDetail = selectedTerritoryDetail,
                    aiBriefing = aiBriefing,
                    onSelectTerritory = { viewModel.selectTerritory(it) },
                    onExplore = { showExploreDialog = it },
                    onClaim = { viewModel.claimTerritory(it.territoryId) },
                    onStationGarrison = { showGarrisonDialog = it },
                    onConstruct = { showConstructDialog = it },
                    onDiscover = { viewModel.discoverTerritory(it.territoryId) }
                )
                1 -> CitadelTab(
                    overview = overview,
                    onUpgradeWing = { showCitadelUpgradeDialog = it }
                )
                2 -> InfrastructureTab(
                    infrastructures = infrastructures,
                    territories = territories,
                    onCollect = { infra ->
                        viewModel.collectResources(infra.structureId, infra.territoryId)
                    },
                    onUpgrade = { infra ->
                        viewModel.upgradeInfrastructure(infra.structureId, infra.territoryId)
                    }
                )
                3 -> GarrisonsTab(
                    garrisons = garrisons,
                    territories = territories,
                    allSquads = allSquads,
                    overview = overview,
                    onRecall = { garrison ->
                        viewModel.recallGarrison(garrison.garrisonId, garrison.territoryId)
                    }
                )
                4 -> ResearchTab(
                    researchList = researchList,
                    onConduct = { projectKey ->
                        viewModel.conductResearch(projectKey)
                    }
                )
                5 -> ActivityLogTab(activities = activities)
            }
        }
    }

    // Explore Dialog
    showExploreDialog?.let { territory ->
        AlertDialog(
            onDismissRequest = { showExploreDialog = null },
            containerColor = DominionNavy,
            title = {
                Text(
                    text = "DISPATCH EXPEDITION",
                    color = DominionCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            },
            text = {
                Column {
                    Text(
                        text = "Assign an available squad to scout and clear ${territory.name}.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (allSquads.isEmpty()) {
                        Text(
                            text = "No squads available in army. Create a squad in the Command Center first.",
                            color = DominionCrimson,
                            fontSize = 12.sp
                        )
                    } else {
                        Text(
                            text = "Select Squad:",
                            color = DominionCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        allSquads.forEach { squadDetail ->
                            OutlinedButton(
                                onClick = {
                                    viewModel.startTerritoryExploration(territory.territoryId, squadDetail.squad.squadId)
                                    showExploreDialog = null
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                border = BorderStroke(1.dp, DominionCyan.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "${squadDetail.squad.name} (${squadDetail.members.size} units)",
                                    color = Color.White,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showExploreDialog = null }) {
                    Text("CANCEL", color = Color.White.copy(alpha = 0.6f))
                }
            }
        )
    }

    // Construct Infrastructure Dialog
    showConstructDialog?.let { territory ->
        AlertDialog(
            onDismissRequest = { showConstructDialog = null },
            containerColor = DominionNavy,
            title = {
                Text(
                    text = "CONSTRUCT INFRASTRUCTURE",
                    color = DominionCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            },
            text = {
                LazyColumn(modifier = Modifier.height(300.dp)) {
                    items(ShadowInfrastructureType.entries) { type ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    viewModel.constructInfrastructure(territory.territoryId, type)
                                    showConstructDialog = null
                                },
                            colors = CardDefaults.cardColors(containerColor = DominionCardBg),
                            border = BorderStroke(1.dp, DominionCyan.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = type.label,
                                    color = DominionCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = type.description,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showConstructDialog = null }) {
                    Text("CANCEL", color = Color.White.copy(alpha = 0.6f))
                }
            }
        )
    }

    // Station Garrison Dialog
    showGarrisonDialog?.let { territory ->
        AlertDialog(
            onDismissRequest = { showGarrisonDialog = null },
            containerColor = DominionNavy,
            title = {
                Text(
                    text = "STATION DEFENSIVE GARRISON",
                    color = DominionCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            },
            text = {
                Column {
                    Text(
                        text = "Station a tactical squad to establish total territorial defense over ${territory.name}.",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (allSquads.isEmpty()) {
                        Text(
                            text = "No squads available in army. Create a squad first.",
                            color = DominionCrimson,
                            fontSize = 12.sp
                        )
                    } else {
                        allSquads.forEach { squadDetail ->
                            OutlinedButton(
                                onClick = {
                                    viewModel.stationGarrison(territory.territoryId, squadDetail.squad.squadId)
                                    showGarrisonDialog = null
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                border = BorderStroke(1.dp, DominionGreen.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "${squadDetail.squad.name} (${squadDetail.members.size} Shadows)",
                                    color = Color.White,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showGarrisonDialog = null }) {
                    Text("CANCEL", color = Color.White.copy(alpha = 0.6f))
                }
            }
        )
    }

    // Upgrade Citadel Dialog
    showCitadelUpgradeDialog?.let { wing ->
        val costs = overview?.dominion?.let { dom ->
            ShadowDominionConfig.getCitadelUpgradeCost(wing, dom.citadelLevel)
        } ?: emptyMap()

        AlertDialog(
            onDismissRequest = { showCitadelUpgradeDialog = null },
            containerColor = DominionNavy,
            title = {
                Text(
                    text = "UPGRADE ${wing.label.uppercase()}",
                    color = DominionCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            },
            text = {
                Column {
                    Text(
                        text = wing.description,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Required Resources:",
                        color = DominionCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    costs.forEach { (res, amt) ->
                        val current = overview?.resourceBalances?.get(res) ?: 0
                        val hasEnough = current >= amt
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(res.label, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                            Text(
                                text = "$current / $amt",
                                color = if (hasEnough) DominionGreen else DominionCrimson,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.upgradeCitadel(wing)
                        showCitadelUpgradeDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DominionCyan)
                ) {
                    Text("CONFIRM UPGRADE", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCitadelUpgradeDialog = null }) {
                    Text("CANCEL", color = Color.White.copy(alpha = 0.6f))
                }
            }
        )
    }
}

// ============================================================================
// COMPONENT 1: DOMINION RESOURCE HEADER
// ============================================================================

@Composable
private fun DominionResourceHeader(overview: DominionOverview?) {
    val balances = overview?.resourceBalances ?: emptyMap()

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(DominionObsidian)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(ShadowResourceType.entries) { res ->
            val amt = balances[res] ?: 0
            Box(
                modifier = Modifier
                    .background(DominionNavy, RoundedCornerShape(6.dp))
                    .border(1.dp, DominionCyan.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Paid,
                        contentDescription = null,
                        tint = when (res) {
                            ShadowResourceType.SHADOW_ESSENCE -> DominionCyan
                            ShadowResourceType.MONARCH_FRAGMENTS -> DominionViolet
                            ShadowResourceType.EVOLUTION_CORES -> DominionGold
                            ShadowResourceType.SHADOW_CRYSTALS -> Color(0xFF64B5F6)
                            ShadowResourceType.ABYSSAL_MATERIALS -> Color(0xFFE040FB)
                        },
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${res.label}: $amt",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ============================================================================
// COMPONENT 2: REALM MAP TAB
// ============================================================================

@Composable
private fun RealmMapTab(
    territories: List<ShadowTerritory>,
    selectedTerritoryDetail: DominionTerritoryDetail?,
    aiBriefing: DominionAiBriefing?,
    onSelectTerritory: (String) -> Unit,
    onExplore: (ShadowTerritory) -> Unit,
    onClaim: (ShadowTerritory) -> Unit,
    onStationGarrison: (ShadowTerritory) -> Unit,
    onConstruct: (ShadowTerritory) -> Unit,
    onDiscover: (ShadowTerritory) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // AI Advisor Briefing
        aiBriefing?.let { brief ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DominionCardBg),
                    border = BorderStroke(1.dp, DominionViolet.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = DominionViolet, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "DOMINION ADVISOR BRIEFING",
                                color = DominionViolet,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = brief.summary, color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Priority: ${brief.strategicPriority}",
                            color = DominionCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Interactive Map Nodes Deck
        item {
            Text(
                text = "REALM TERRITORIES",
                color = DominionCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }

        items(territories) { territory ->
            val isSelected = selectedTerritoryDetail?.territory?.territoryId == territory.territoryId
            val stateColor = when (territory.state) {
                ShadowTerritoryState.CONTROLLED -> DominionCyan
                ShadowTerritoryState.FORTIFIED -> DominionGreen
                ShadowTerritoryState.DEVELOPING -> DominionGold
                ShadowTerritoryState.EXPLORING -> Color(0xFFFF9100)
                ShadowTerritoryState.CONTESTED -> DominionCrimson
                ShadowTerritoryState.DISCOVERED -> Color(0xFF64B5F6)
                ShadowTerritoryState.UNKNOWN -> Color.White.copy(alpha = 0.3f)
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectTerritory(territory.territoryId) },
                colors = CardDefaults.cardColors(containerColor = if (isSelected) DominionCardBg else DominionNavy),
                border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) DominionCyan else stateColor.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(stateColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = territory.name,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "${territory.category.label} • State: ${territory.state.label}",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 10.sp
                        )
                        if (territory.state == ShadowTerritoryState.EXPLORING) {
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { territory.explorationProgressPercent / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp),
                                color = DominionCyan,
                                trackColor = Color.White.copy(alpha = 0.1f)
                            )
                        }
                    }

                    // Defense Rating Badge
                    Box(
                        modifier = Modifier
                            .background(DominionSurfaceMuted, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "DEF: ${territory.defenseRating}",
                            color = DominionCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Selected Territory Action Panel
        selectedTerritoryDetail?.let { detail ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DominionCardBg),
                    border = BorderStroke(1.dp, DominionCyan.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TERRITORY COMMAND: ${detail.territory.name}",
                                color = DominionCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = detail.defenseAssessment.securityLevel,
                                color = when (detail.defenseAssessment.securityLevel) {
                                    "FORTIFIED" -> DominionGreen
                                    "SECURE" -> DominionCyan
                                    "DEFENDED" -> DominionGold
                                    else -> DominionCrimson
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = detail.territory.description, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Territory Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            when (detail.territory.state) {
                                ShadowTerritoryState.UNKNOWN -> {
                                    Button(
                                        onClick = { onDiscover(detail.territory) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = DominionCyan)
                                    ) {
                                        Text("SCOUT REGION", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                ShadowTerritoryState.DISCOVERED -> {
                                    Button(
                                        onClick = { onExplore(detail.territory) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = DominionCyan)
                                    ) {
                                        Text("EXPLORE", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                ShadowTerritoryState.EXPLORING, ShadowTerritoryState.CONTESTED -> {
                                    Button(
                                        onClick = { onClaim(detail.territory) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = DominionGreen)
                                    ) {
                                        Text("CLAIM TERRITORY", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                ShadowTerritoryState.CONTROLLED, ShadowTerritoryState.DEVELOPING, ShadowTerritoryState.FORTIFIED -> {
                                    OutlinedButton(
                                        onClick = { onConstruct(detail.territory) },
                                        modifier = Modifier.weight(1f),
                                        border = BorderStroke(1.dp, DominionCyan)
                                    ) {
                                        Text("BUILD", color = DominionCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = { onStationGarrison(detail.territory) },
                                        modifier = Modifier.weight(1f),
                                        border = BorderStroke(1.dp, DominionGreen)
                                    ) {
                                        Text("GARRISON", color = DominionGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

// ============================================================================
// COMPONENT 3: CITADEL TAB
// ============================================================================

@Composable
private fun CitadelTab(
    overview: DominionOverview?,
    onUpgradeWing: (CitadelUpgradeType) -> Unit
) {
    val dominion = overview?.dominion

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DominionCardBg),
                border = BorderStroke(1.dp, DominionCyan.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "SHADOW MONARCH CITADEL",
                        color = DominionCyan,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Central administrative core governing territorial defense and army mobilization.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Citadel Level: ${dominion?.citadelLevel ?: 1}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Realm Authority: ${dominion?.dominionLevel ?: 1}", color = DominionGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            Text(
                text = "CITADEL ADMINISTRATIVE WINGS",
                color = DominionCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        items(CitadelUpgradeType.entries) { wing ->
            val curTier = dominion?.citadelLevel ?: 1
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DominionNavy),
                border = BorderStroke(1.dp, DominionCyan.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = wing.label, color = DominionCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(text = wing.description, color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Current Tier: $curTier / 5", color = DominionGold, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }

                    Button(
                        onClick = { onUpgradeWing(wing) },
                        colors = ButtonDefaults.buttonColors(containerColor = DominionCyan)
                    ) {
                        Text("UPGRADE", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ============================================================================
// COMPONENT 4: INFRASTRUCTURE TAB
// ============================================================================

@Composable
private fun InfrastructureTab(
    infrastructures: List<ShadowInfrastructure>,
    territories: List<ShadowTerritory>,
    onCollect: (ShadowInfrastructure) -> Unit,
    onUpgrade: (ShadowInfrastructure) -> Unit
) {
    val territoryMap = territories.associateBy { it.territoryId }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (infrastructures.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No infrastructure erected yet. Secure territories and construct structures from the Realm Map.",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        items(infrastructures) { infra ->
            val territoryName = territoryMap[infra.territoryId]?.name ?: infra.territoryId
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DominionNavy),
                border = BorderStroke(1.dp, DominionCyan.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${infra.structureType.label} (Lv ${infra.level})",
                            color = DominionCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Location: $territoryName",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 10.sp
                        )
                    }
                    Text(
                        text = infra.structureType.description,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (infra.structureType == ShadowInfrastructureType.RESOURCE_EXTRACTOR) {
                            Button(
                                onClick = { onCollect(infra) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = DominionGold)
                            ) {
                                Text("HARVEST", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        OutlinedButton(
                            onClick = { onUpgrade(infra) },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, DominionCyan)
                        ) {
                            Text("UPGRADE", color = DominionCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// COMPONENT 5: GARRISONS TAB
// ============================================================================

@Composable
private fun GarrisonsTab(
    garrisons: List<ShadowGarrison>,
    territories: List<ShadowTerritory>,
    allSquads: List<com.example.domain.model.SquadDetail>,
    overview: DominionOverview?,
    onRecall: (ShadowGarrison) -> Unit
) {
    val territoryMap = territories.associateBy { it.territoryId }
    val squadMap = allSquads.associateBy { it.squad.squadId }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        overview?.alerts?.let { alerts ->
            if (alerts.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DominionCardBg),
                        border = BorderStroke(1.dp, DominionCrimson.copy(alpha = 0.6f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = DominionCrimson, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "TERRITORIAL DEFENSE WARNINGS",
                                    color = DominionCrimson,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            alerts.forEach { alert ->
                                Text(text = "• $alert", color = Color.White.copy(alpha = 0.9f), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "ACTIVE GARRISONS (${garrisons.size})",
                color = DominionCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        if (garrisons.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No garrisons currently stationed. Assign tactical squads from the Realm Map.",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        items(garrisons) { garrison ->
            val tName = territoryMap[garrison.territoryId]?.name ?: garrison.territoryId
            val squadDetail = squadMap[garrison.squadId]

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DominionNavy),
                border = BorderStroke(1.dp, DominionGreen.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = squadDetail?.squad?.name ?: "Defensive Garrison",
                            color = DominionGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Stationed at $tName",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Defense Rating: ${garrison.defensiveRating} • Members: ${squadDetail?.members?.size ?: 0}",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { onRecall(garrison) },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, DominionCrimson)
                    ) {
                        Text("RECALL GARRISON", color = DominionCrimson, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ============================================================================
// COMPONENT 6: RESEARCH TAB
// ============================================================================

@Composable
private fun ResearchTab(
    researchList: List<ShadowDominionResearch>,
    onConduct: (String) -> Unit
) {
    val completedKeys = researchList.filter { it.isCompleted }.map { it.projectKey }.toSet()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "DOMINION RESEARCH & DEVELOPMENT",
                color = DominionCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        items(ShadowDominionResearchEngine.CATALOG) { def ->
            val isCompleted = def.projectKey in completedKeys
            val prereqMet = def.prerequisiteKey == null || def.prerequisiteKey in completedKeys

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DominionNavy),
                border = BorderStroke(
                    1.dp,
                    if (isCompleted) DominionGreen.copy(alpha = 0.6f) else DominionCyan.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = def.title, color = if (isCompleted) DominionGreen else DominionCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Lv ${def.level}", color = DominionGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(text = def.description, color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = def.unlockedEffectsSummary, color = DominionViolet, fontSize = 10.sp)
                    }

                    if (isCompleted) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = DominionGreen, modifier = Modifier.size(24.dp))
                    } else if (!prereqMet) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
                    } else {
                        Button(
                            onClick = { onConduct(def.projectKey) },
                            colors = ButtonDefaults.buttonColors(containerColor = DominionCyan)
                        ) {
                            Text("RESEARCH", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// COMPONENT 7: ACTIVITY LOG TAB
// ============================================================================

@Composable
private fun ActivityLogTab(activities: List<ShadowDominionActivity>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (activities.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "No recorded Dominion activities yet.", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                }
            }
        }

        items(activities) { act ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DominionNavy),
                border = BorderStroke(1.dp, DominionCyan.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = act.title, color = DominionCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(text = act.activityType.label, color = DominionViolet, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = act.description, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                }
            }
        }
    }
}
