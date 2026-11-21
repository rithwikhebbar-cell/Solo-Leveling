package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.domain.model.HunterRank
import com.example.ui.components.HolographicPanel
import com.example.ui.components.SystemEmptyState
import com.example.ui.components.SystemSectionTitle
import com.example.ui.screens.modules.ModuleCard
import com.example.ui.screens.modules.ModuleCategory
import com.example.ui.screens.modules.ModuleCommandHeader
import com.example.ui.screens.modules.ModuleDetailBottomSheet
import com.example.ui.screens.modules.ModuleOperationalStatus
import com.example.ui.screens.modules.ModuleRegistry
import com.example.ui.screens.modules.ModuleSpec
import com.example.ui.theme.SystemColors
import com.example.ui.theme.SystemSpacing
import com.example.ui.viewmodel.RealityOsUiState

@Composable
fun ModulesHubScreen(
    onOpenAcademicEngine: () -> Unit,
    onOpenDungeonGate: () -> Unit,
    onOpenSkills: () -> Unit,
    onOpenAchievements: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAiSystem: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenLifeEngine: () -> Unit = {},
    onOpenBossBattles: () -> Unit = {},
    onOpenTitles: () -> Unit = {},
    onOpenInventory: () -> Unit = {},
    onOpenShadowCollection: () -> Unit = {},
    onOpenRpgRewardHistory: () -> Unit = {},
    onOpenCloudSync: () -> Unit = {},
    onOpenIntelligenceCenter: () -> Unit = {},
    onOpenSystemControl: () -> Unit = {},
    onOpenAiCompanion: () -> Unit = {},
    onOpenVoiceConsole: () -> Unit = {},
    onOpenSystemVoiceSettings: () -> Unit = {},
    onOpenWidgetHub: () -> Unit = {},
    onOpenCelebrationVault: () -> Unit = {},
    onOpenShadowRealm: () -> Unit = {},
    onOpenSocialMastery: () -> Unit = {},
    onOpenPenaltyStatus: () -> Unit = {},
    onOpenPenaltyAudit: () -> Unit = {}
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf(ModuleCategory.ALL) }
    var inspectingModule by remember { mutableStateOf<ModuleSpec?>(null) }

    val allModules = remember {
        ModuleRegistry.buildInventory(
            onOpenAcademicEngine = onOpenAcademicEngine,
            onOpenDungeonGate = onOpenDungeonGate,
            onOpenSkills = onOpenSkills,
            onOpenAchievements = onOpenAchievements,
            onOpenHistory = onOpenHistory,
            onOpenSettings = onOpenSettings,
            onOpenAiSystem = onOpenAiSystem,
            onOpenLifeEngine = onOpenLifeEngine,
            onOpenBossBattles = onOpenBossBattles,
            onOpenTitles = onOpenTitles,
            onOpenInventory = onOpenInventory,
            onOpenShadowCollection = onOpenShadowCollection,
            onOpenRpgRewardHistory = onOpenRpgRewardHistory,
            onOpenCloudSync = onOpenCloudSync,
            onOpenIntelligenceCenter = onOpenIntelligenceCenter,
            onOpenSystemControl = onOpenSystemControl,
            onOpenAiCompanion = onOpenAiCompanion,
            onOpenVoiceConsole = onOpenVoiceConsole,
            onOpenSystemVoiceSettings = onOpenSystemVoiceSettings,
            onOpenWidgetHub = onOpenWidgetHub,
            onOpenCelebrationVault = onOpenCelebrationVault,
            onOpenShadowRealm = onOpenShadowRealm,
            onOpenSocialMastery = onOpenSocialMastery,
            onOpenPenaltyStatus = onOpenPenaltyStatus,
            onOpenPenaltyAudit = onOpenPenaltyAudit
        )
    }

    val activeCount = remember(allModules) { allModules.count { it.status == ModuleOperationalStatus.ACTIVE } }
    val availableCount = remember(allModules) { allModules.count { it.status == ModuleOperationalStatus.AVAILABLE } }

    val filteredModules = remember(searchQuery, selectedCategory, allModules) {
        allModules.filter { mod ->
            val matchesCategory = selectedCategory == ModuleCategory.ALL || mod.category == selectedCategory
            val matchesSearch = searchQuery.isBlank() ||
                mod.title.contains(searchQuery, ignoreCase = true) ||
                mod.description.contains(searchQuery, ignoreCase = true) ||
                mod.capabilitySummary.contains(searchQuery, ignoreCase = true) ||
                mod.metricHighlight.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SystemColors.ObsidianBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = SystemSpacing.maxContentWidth),
            contentPadding = PaddingValues(
                horizontal = SystemSpacing.screenHorizontal,
                vertical = SystemSpacing.screenVertical
            ),
            verticalArrangement = Arrangement.spacedBy(SystemSpacing.sectionSpacing)
        ) {
            // 1. Cinematic Module Header & Search & Category Selector
            item {
                ModuleCommandHeader(
                    totalCount = allModules.size,
                    activeCount = activeCount,
                    availableCount = availableCount,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it }
                )
            }

            // 2. Filtered Subsystem Directory Cards
            if (filteredModules.isEmpty()) {
                item {
                    SystemEmptyState(
                        title = "No Matching Subsystems",
                        description = "No system modules found matching \"$searchQuery\". Try a different search term or select another category tab."
                    )
                }
            } else {
                items(filteredModules, key = { it.id }) { mod ->
                    ModuleCard(
                        mod = mod,
                        onCardClick = { inspectingModule = mod },
                        onDirectLaunch = mod.onClick
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(SystemSpacing.verticalPageSpacing))
            }
        }
    }

    // Module Detail Modal Bottom Sheet
    if (inspectingModule != null) {
        ModuleDetailBottomSheet(
            mod = inspectingModule,
            onLaunch = { inspectingModule?.onClick?.invoke() },
            onDismiss = { inspectingModule = null }
        )
    }
}

@Composable
fun DungeonGateScreen(
    state: RealityOsUiState.Ready,
    modifier: Modifier = Modifier
) {
    val gates = listOf(
        Triple("E-Rank Gate: Consistency Trial", HunterRank.E_RANK, "Complete 3 consecutive days of 30-min jogging or active quests."),
        Triple("D-Rank Gate: Iron Stamina", HunterRank.D_RANK, "Requires D-Rank (Lv.5, 85 total attr, 3 verified sessions)."),
        Triple("C-Rank Gate: Vanguard Boss Raid", HunterRank.C_RANK, "Requires C-Rank (Lv.12, 115 total attr, 10 verified sessions)."),
        Triple("B-Rank Gate: Sovereign Endurance", HunterRank.B_RANK, "Requires B-Rank (Lv.22, 160 total attr, 25 verified sessions).")
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SystemColors.ObsidianBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SystemSectionTitle(
                    title = "Dungeon Gates",
                    subtitle = "Rank-gated real-world endurance trials"
                )
            }
            items(gates) { (name, requiredRank, desc) ->
                val unlocked = state.player.rank.ordinal >= requiredRank.ordinal
                val accent = if (unlocked) SystemColors.rankAccent(requiredRank) else SystemColors.MutedSilver
                HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = accent, contentPadding = 16.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (unlocked) Icons.Rounded.Shield else Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (unlocked) SystemColors.PureIce else SystemColors.MutedSilver
                            )
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodyMedium,
                                color = SystemColors.MutedSilver
                            )
                        }
                        Surface(
                            color = accent.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, accent.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = if (unlocked) "OPEN" else "LOCKED",
                                style = MaterialTheme.typography.labelMedium,
                                color = accent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SkillsScreen(
    state: RealityOsUiState.Ready,
    modifier: Modifier = Modifier
) {
    val player = state.player
    val skills = listOf(
        SkillSpec("Aerobic Cadence", "Unlocks at VIT 10 (Baseline)", player.attributes.vit.totalValue >= 10, "Supports steady-state 30-min jogging recovery pacing."),
        SkillSpec("Deep Work Focus", "Unlocks at INT 12", player.attributes.intel.totalValue >= 12, "Unlocks advanced study block quest scaling."),
        SkillSpec("Iron Routine", "Unlocks at DIS 14", player.attributes.dis.totalValue >= 14, "Protects streak multiplier during moderate recovery days."),
        SkillSpec("Monarch's Extraction", "Unlocks at C-Rank & Lv.12", player.rank.ordinal >= HunterRank.C_RANK.ordinal, "Advanced multi-week boss battle campaign orchestration.")
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SystemColors.ObsidianBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SystemSectionTitle(
                    title = "Skills & Abilities",
                    subtitle = "Evaluated dynamically from real player attributes"
                )
            }
            items(skills) { skill ->
                val accent = if (skill.unlocked) SystemColors.ElectricCyan else SystemColors.MutedSilver
                HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = accent, contentPadding = 16.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(skill.name, style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
                            Surface(
                                color = accent.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, accent.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = if (skill.unlocked) "UNLOCKED" else "LOCKED",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = accent,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Text(skill.requirement, style = MaterialTheme.typography.labelSmall, color = accent)
                        Text(skill.description, style = MaterialTheme.typography.bodyMedium, color = SystemColors.MutedSilver)
                    }
                }
            }
        }
    }
}

private data class SkillSpec(
    val name: String,
    val requirement: String,
    val unlocked: Boolean,
    val description: String
)

@Composable
fun AchievementsScreen(
    state: RealityOsUiState.Ready,
    modifier: Modifier = Modifier
) {
    val completedQuests = state.quests.count { it.isCompleted }
    val milestones = listOf(
        Triple("First Step", "Log or complete at least 1 real-world activity.", state.player.totalActivitiesCount >= 1),
        Triple("Quest Clearer", "Complete 3 quests on the Quest Board.", completedQuests >= 3),
        Triple("Verified Proof", "Record at least 1 Health Connect or System Verified session.", state.player.verifiedActivitiesCount >= 1),
        Triple("D-Rank Ascension", "Achieve D-Rank through deterministic progression.", state.player.rank.ordinal >= HunterRank.D_RANK.ordinal)
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SystemColors.ObsidianBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SystemSectionTitle(
                    title = "Achievements",
                    subtitle = "${milestones.count { it.third }} / ${milestones.size} unlocked"
                )
            }
            items(milestones) { (title, desc, achieved) ->
                val accent = if (achieved) SystemColors.MonarchGold else SystemColors.MutedSilver
                HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = accent, contentPadding = 16.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (achieved) Icons.Rounded.EmojiEvents else Icons.Rounded.Lock,
                            contentDescription = title,
                            tint = accent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(title, style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
                            Text(desc, style = MaterialTheme.typography.bodyMedium, color = SystemColors.MutedSilver)
                        }
                        Surface(
                            color = accent.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, accent.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = if (achieved) "EARNED" else "PENDING",
                                style = MaterialTheme.typography.labelMedium,
                                color = accent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
