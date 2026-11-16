package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.HunterRank
import com.example.ui.components.HolographicPanel
import com.example.ui.components.SystemSectionTitle
import com.example.ui.theme.SystemColors
import com.example.ui.viewmodel.RealityOsUiState

@OptIn(ExperimentalLayoutApi::class)
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
    onOpenSocialMastery: () -> Unit = {}
) {
    val modules = listOf(
        ModuleEntry("Social Mastery", "8 development modules, speech rehearsal, scenarios & interpersonal progression", Icons.Rounded.RecordVoiceOver, SystemColors.ElectricCyan, onOpenSocialMastery),
        ModuleEntry("The Shadow Realm", "Shadow Monarch progression, Shadow Extraction, army deployment & formations", Icons.Rounded.Bolt, SystemColors.ArcaneViolet, onOpenShadowRealm),
        ModuleEntry("AI System Companion", "Holographic interactive guidance, voice output & privacy-safe Q&A", Icons.Rounded.AutoAwesome, SystemColors.ElectricCyan, onOpenAiCompanion),
        ModuleEntry("Voice Command Console", "Tap-to-speak allowlisted System navigation & confirmation", Icons.Rounded.Bolt, SystemColors.ElectricCyan, onOpenVoiceConsole),
        ModuleEntry("System Voice & Announcements", "Native Android TTS engine, priority alerts & speech controls", Icons.Rounded.Psychology, SystemColors.ArcaneViolet, onOpenSystemVoiceSettings),
        ModuleEntry("Home-Screen Widgets", "4 Jetpack Glance widgets & live telemetry snapshots", Icons.Rounded.Shield, SystemColors.VerifiedEmerald, onOpenWidgetHub),
        ModuleEntry("Celebration Vault", "Replayable verified Level-Up and Rank Promotion milestones", Icons.Rounded.EmojiEvents, SystemColors.MonarchGold, onOpenCelebrationVault),
        ModuleEntry("System Control Center", "Health, automation workers, daily timeline, integrity & recovery", Icons.Rounded.Shield, SystemColors.ElectricCyan, onOpenSystemControl),
        ModuleEntry("Intelligence Center", "Adaptive planning, predictions, roadmaps, reports & Q&A", Icons.Rounded.Psychology, SystemColors.ElectricCyan, onOpenIntelligenceCenter),
        ModuleEntry("Cloud Sync & Account", "Supabase identity, multi-device sync, conflicts & backups", Icons.Rounded.CloudSync, SystemColors.ElectricCyan, onOpenCloudSync),
        ModuleEntry("Life & Discipline OS", "Routines, Habits, Tasks, Goals & Unified Calendar", Icons.Rounded.CalendarToday, SystemColors.VerifiedEmerald, onOpenLifeEngine),
        ModuleEntry("Academic Engine", "Subjects, Focus Timer, Revisions & Exams", Icons.Rounded.School, SystemColors.ElectricCyan, onOpenAcademicEngine),
        ModuleEntry("Dungeon Gate", "Adaptive daily/weekly gates, discovery & live validation", Icons.Rounded.Security, SystemColors.DangerCrimson, onOpenDungeonGate),
        ModuleEntry("Boss Battles", "Multi-stage raid campaigns with deterministic HP", Icons.Rounded.Shield, SystemColors.DangerCrimson, onOpenBossBattles),
        ModuleEntry("Skills & Trees", "5 specialization skill trees & real gameplay perks", Icons.Rounded.Bolt, SystemColors.SystemBlue, onOpenSkills),
        ModuleEntry("Achievements", "10 milestone categories with verified evidence tracking", Icons.Rounded.EmojiEvents, SystemColors.MonarchGold, onOpenAchievements),
        ModuleEntry("Player Titles", "Equip unlocked honorifics synced to Status Window", Icons.Rounded.MilitaryTech, SystemColors.MonarchGold, onOpenTitles),
        ModuleEntry("Inventory", "Cosmetics, HUD frames, themes, badges & artifacts", Icons.Rounded.Inventory2, SystemColors.ElectricCyan, onOpenInventory),
        ModuleEntry("Shadow Vault", "Original geometric constructs & milestone guardians", Icons.Rounded.AutoAwesome, SystemColors.ArcaneViolet, onOpenShadowCollection),
        ModuleEntry("RPG Reward Ledger", "Daily RPG XP cap meter & anti-farming audit log", Icons.Rounded.ReceiptLong, SystemColors.VerifiedEmerald, onOpenRpgRewardHistory),
        ModuleEntry("AI System", "Game Master advisory & OpenRouter port", Icons.Rounded.AutoAwesome, SystemColors.ArcaneViolet, onOpenAiSystem),
        ModuleEntry("History", "Chronological XP & activity audit", Icons.Rounded.History, SystemColors.VerifiedEmerald, onOpenHistory),
        ModuleEntry("Settings", "Codename, DataStore & sync ports", Icons.Rounded.Settings, SystemColors.MetallicSilver, onOpenSettings)
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
                    title = "Reality OS Modules",
                    subtitle = "All core system & academic subsystems"
                )
            }
            items(modules) { mod ->
                HolographicPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = mod.onClick)
                        .testTag("module_card_${mod.title.lowercase().replace(" ", "_")}"),
                    accentColor = mod.accent
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = mod.accent.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, mod.accent.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = mod.icon,
                                contentDescription = mod.title,
                                tint = mod.accent,
                                modifier = Modifier
                                    .padding(10.dp)
                                    .size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mod.title.uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                color = SystemColors.PureIce
                            )
                            Text(
                                text = mod.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = SystemColors.MutedSilver
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class ModuleEntry(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color,
    val onClick: () -> Unit
)

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
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = accent) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (unlocked) Icons.Rounded.Shield else Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (unlocked) SystemColors.PureIce else SystemColors.MutedSilver
                            )
                            Text(
                                text = desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = SystemColors.MutedSilver
                            )
                        }
                        Text(
                            text = if (unlocked) "OPEN" else "LOCKED",
                            style = MaterialTheme.typography.labelMedium,
                            color = accent
                        )
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SystemSectionTitle(
                    title = "Skills & Abilities",
                    subtitle = "Evaluated dynamically from real player attributes"
                )
            }
            items(skills) { skill ->
                val accent = if (skill.unlocked) SystemColors.ElectricCyan else SystemColors.MutedSilver
                HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = accent) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(skill.name, style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
                            Text(
                                text = if (skill.unlocked) "UNLOCKED" else "LOCKED",
                                style = MaterialTheme.typography.labelMedium,
                                color = accent
                            )
                        }
                        Text(skill.requirement, style = MaterialTheme.typography.labelSmall, color = accent)
                        Text(skill.description, style = MaterialTheme.typography.bodySmall, color = SystemColors.MetallicSilver)
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SystemSectionTitle(
                    title = "Achievements",
                    subtitle = "${milestones.count { it.third }} / ${milestones.size} unlocked"
                )
            }
            items(milestones) { (title, desc, achieved) ->
                val accent = if (achieved) SystemColors.MonarchGold else SystemColors.MutedSilver
                HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = accent) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (achieved) Icons.Rounded.EmojiEvents else Icons.Rounded.Lock,
                            contentDescription = title,
                            tint = accent,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(title, style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
                            Text(desc, style = MaterialTheme.typography.bodySmall, color = SystemColors.MetallicSilver)
                        }
                        Text(
                            text = if (achieved) "EARNED" else "PENDING",
                            style = MaterialTheme.typography.labelMedium,
                            color = accent
                        )
                    }
                }
            }
        }
    }
}
