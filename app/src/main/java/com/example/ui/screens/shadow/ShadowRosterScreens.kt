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
    var searchQuery by remember { mutableStateOf("") }
    var selectedClassFilter by remember { mutableStateOf<ShadowClass?>(null) }
    var selectedRankFilter by remember { mutableStateOf<ShadowRank?>(null) }

    val filteredShadows = uiState.allShadows.filter { shadow ->
        (searchQuery.isBlank() || shadow.name.contains(searchQuery, ignoreCase = true)) &&
        (selectedClassFilter == null || shadow.shadowClass == selectedClassFilter) &&
        (selectedRankFilter == null || shadow.currentRank == selectedRankFilter)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SHADOW ROSTER (${uiState.allShadows.size})", style = MaterialTheme.typography.titleMedium, color = ShadowColors.PureIce) },
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
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search shadows by name...", color = ShadowColors.Muted) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = ShadowColors.Muted) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = ShadowColors.PureIce,
                    unfocusedTextColor = ShadowColors.PureIce,
                    focusedBorderColor = ShadowColors.Cyan,
                    unfocusedBorderColor = ShadowColors.PanelSurfaceElevated
                )
            )

            // Class filters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedClassFilter == null,
                    onClick = { selectedClassFilter = null },
                    label = { Text("All Classes", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ShadowColors.Cyan.copy(alpha = 0.2f),
                        selectedLabelColor = ShadowColors.Cyan
                    )
                )
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
            }

            // Rank filters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
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

            Spacer(modifier = Modifier.height(8.dp))

            // Roster list
            if (filteredShadows.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (uiState.allShadows.isEmpty()) "No Shadows recruited yet.\nUse Shadow Extraction to summon your army."
                        else "No shadows match the selected filter.",
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
                    items(filteredShadows) { shadow ->
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
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    ShadowClassBadge(shadow.shadowClass)
                                    ShadowRarityBadge(shadow.rarity)
                                }
                            }
                            ShadowRankBadge(shadow.currentRank)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Level & XP Bar
                        val nextXp = ShadowEvolutionEngine.calculateNextLevelXp(shadow.level)
                        val progress = if (nextXp > 0) (shadow.shadowXp.toFloat() / nextXp).coerceIn(0f, 1f) else 1f

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "LEVEL ${shadow.level}", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = ShadowColors.Cyan)
                            Text(text = "${shadow.shadowXp} / $nextXp XP", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = ShadowColors.PureIce)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = ShadowColors.Cyan,
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
// 3. SHADOW EVOLUTION SCREEN
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SHADOW EVOLUTION", style = MaterialTheme.typography.titleMedium, color = ShadowColors.PureIce) },
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
                // Current Stage vs Next
                Card(
                    colors = CardDefaults.cardColors(containerColor = ShadowColors.PanelSurface),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ShadowColors.Violet.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "EVOLUTION STAGES", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = ShadowColors.Muted)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(text = "CURRENT", style = MaterialTheme.typography.labelSmall, color = ShadowColors.Muted)
                                Text(text = shadow.evolutionStage.label, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = ShadowColors.PureIce)
                                ShadowRankBadge(shadow.currentRank)
                            }
                            Icon(Icons.Rounded.Upgrade, contentDescription = null, tint = ShadowColors.Violet, modifier = Modifier.size(32.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "NEXT STAGE", style = MaterialTheme.typography.labelSmall, color = ShadowColors.Muted)
                                val nextRank = ShadowEvolutionEngine.calculateRank(shadow.level + 10, shadow.shadowXp)
                                val nextStage = ShadowEvolutionEngine.calculateEvolutionStage(shadow.level + 10, nextRank)
                                Text(text = nextStage.label, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = ShadowColors.Cyan)
                                ShadowRankBadge(nextRank)
                            }
                        }
                    }
                }

                // Requirements Checklist
                Text(text = "REQUIREMENTS CHECKLIST", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = ShadowColors.Muted)

                val reqLevel = 10 * ((shadow.level / 10) + 1)
                val levelMet = shadow.level >= reqLevel
                val coreCost = 2
                val coresMet = uiState.evolutionCoresBalance >= coreCost

                Card(
                    colors = CardDefaults.cardColors(containerColor = ShadowColors.PanelSurface),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ShadowColors.PanelSurfaceElevated),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        RequirementItem("Reach Level $reqLevel", "${shadow.level} / $reqLevel", levelMet)
                        RequirementItem("Evolution Cores ($coreCost required)", "${uiState.evolutionCoresBalance} / $coreCost", coresMet)
                        RequirementItem("Monarch Authority Alignment", "Verified", true)
                    }
                }

                // Award XP training button for test / real development progression
                OutlinedButton(
                    onClick = {
                        viewModel.awardShadowXp(shadow.shadowId, 100, "training_session_${System.currentTimeMillis()}")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ShadowColors.Cyan),
                    border = BorderStroke(1.dp, ShadowColors.Cyan.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Rounded.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("TRAIN SHADOW (+100 XP)")
                }
            }
        }
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

