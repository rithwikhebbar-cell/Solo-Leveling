package com.example.ui.screens.rpg

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.DungeonCategory
import com.example.domain.model.DungeonDifficulty
import com.example.domain.model.DungeonStatus
import com.example.domain.model.DungeonWithStatus
import com.example.domain.model.ProposalStatus
import com.example.ui.components.GlowingProgressBar
import com.example.ui.components.HolographicPanel
import com.example.ui.components.SystemEmptyState
import com.example.ui.components.SystemSectionTitle
import com.example.ui.theme.SystemColors
import com.example.ui.viewmodel.RpgUiState
import java.text.DateFormat
import java.util.Date

// 1. DUNGEON GATE DASHBOARD
@Composable
fun DungeonGateDashboardScreen(
    uiState: RpgUiState,
    onRetry: () -> Unit,
    onClearFeedback: () -> Unit,
    onOpenDiscovery: () -> Unit,
    onOpenDungeonDetails: (Long) -> Unit,
    onOpenActiveDungeon: (Long) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenBossBattles: () -> Unit,
    onOpenRewardHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    RpgStateHost(uiState = uiState, onRetry = onRetry, modifier = modifier) { ready ->
        val snap = ready.snapshot
        val active = snap.dungeons.filter {
            it.effectiveStatus == DungeonStatus.ACTIVE || it.effectiveStatus == DungeonStatus.PAUSED
        }
        val featured = snap.dungeons.filter {
            it.effectiveStatus == DungeonStatus.AVAILABLE
        }.take(4)

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 760.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    RpgFeedbackBanner(message = ready.feedbackBanner, onDismiss = onClearFeedback)
                }

                item {
                    HolographicPanel(accentColor = SystemColors.ElectricCyan, contentPadding = 0.dp) {
                        Column {
                            Image(
                                painter = painterResource(id = R.drawable.img_dungeon_gate_portal),
                                contentDescription = "Dungeon Gate Portal",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(145.dp)
                                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "DIMENSIONAL GATE NEXUS",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = SystemColors.PureIce
                                )
                                Text(
                                    text = "Real-world endurance, study, and discipline challenges validated exclusively by verified telemetry and logged records. Entering grants 0 XP.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SystemColors.MetallicSilver
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = onOpenDiscovery,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("btn_open_dungeon_discovery"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = SystemColors.ElectricCyan,
                                            contentColor = SystemColors.ObsidianBlack
                                        )
                                    ) {
                                        Icon(Icons.Rounded.Explore, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("DISCOVER")
                                    }
                                    OutlinedButton(
                                        onClick = onOpenBossBattles,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("btn_open_boss_battles")
                                    ) {
                                        Icon(Icons.Rounded.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("BOSS RAIDS")
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        RpgMetricMiniCard("Active Gates", "${active.size}", SystemColors.VerifiedEmerald, Modifier.weight(1f))
                        RpgMetricMiniCard("Cleared", "${snap.completionHistory.size}", SystemColors.MonarchGold, Modifier.weight(1f))
                        RpgMetricMiniCard("Rank Gate", snap.profile.rank.code, SystemColors.rankAccent(snap.profile.rank), Modifier.weight(1f))
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onOpenHistory,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("btn_dungeon_history")
                        ) {
                            Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("GATE HISTORY")
                        }
                        OutlinedButton(
                            onClick = onOpenRewardHistory,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("btn_rpg_reward_history")
                        ) {
                            Text("REWARD LEDGER")
                        }
                    }
                }

                item {
                    SystemSectionTitle(
                        title = "Active Dungeon Instances",
                        subtitle = "Live objective verification from Physical, Academic & Life telemetry"
                    )
                }

                if (active.isEmpty()) {
                    item {
                        SystemEmptyState(
                            title = "No Active Gate Entered",
                            description = "Browse Available Gates below or open Discovery to enter a rank-appropriate dungeon."
                        )
                    }
                } else {
                    items(active, key = { "active_${it.definition.id}" }) { item ->
                        DungeonSummaryCard(
                            dungeon = item,
                            onClick = {
                                val instId = item.latestInstance?.id
                                if (instId != null) onOpenActiveDungeon(instId) else onOpenDungeonDetails(item.definition.id)
                            }
                        )
                    }
                }

                item {
                    SystemSectionTitle(
                        title = "Available Gates",
                        subtitle = "Rank & attribute eligible challenges"
                    )
                }

                items(featured, key = { "avail_${it.definition.id}" }) { item ->
                    DungeonSummaryCard(
                        dungeon = item,
                        onClick = { onOpenDungeonDetails(item.definition.id) }
                    )
                }
            }
        }
    }
}

// 2. DUNGEON DISCOVERY
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DungeonDiscoveryScreen(
    uiState: RpgUiState,
    onRetry: () -> Unit,
    onClearFeedback: () -> Unit,
    onOpenDungeonDetails: (Long) -> Unit,
    onGenerateAiProposals: () -> Unit,
    onApproveAiProposal: (Long) -> Unit,
    onRejectAiProposal: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    RpgStateHost(uiState = uiState, onRetry = onRetry, modifier = modifier) { ready ->
        var selectedCategory by remember { mutableStateOf<DungeonCategory?>(null) }
        var selectedDifficulty by remember { mutableStateOf<DungeonDifficulty?>(null) }

        val filtered = ready.snapshot.dungeons.filter { item ->
            (selectedCategory == null || item.definition.category == selectedCategory) &&
                (selectedDifficulty == null || item.definition.difficulty == selectedDifficulty)
        }
        val pendingAiProposals = ready.snapshot.aiDungeonProposals.filter { it.status == ProposalStatus.PENDING }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 760.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { RpgFeedbackBanner(ready.feedbackBanner, onClearFeedback) }

                item {
                    HolographicPanel(accentColor = SystemColors.ArcaneViolet) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("AI GAME MASTER GATE PROPOSALS", style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = SystemColors.ArcaneViolet)
                            }
                            Text(
                                text = "AI evaluates your current rank, workload, and recovery readiness to propose optional gates. Explicit approval is required.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SystemColors.MetallicSilver
                            )
                            Button(
                                onClick = onGenerateAiProposals,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("btn_generate_ai_dungeons"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SystemColors.ArcaneViolet,
                                    contentColor = SystemColors.PureIce
                                )
                            ) {
                                Text("REQUEST AI GATE PROPOSALS")
                            }
                        }
                    }
                }

                items(pendingAiProposals, key = { "ai_prop_${it.id}" }) { prop ->
                    HolographicPanel(accentColor = SystemColors.ArcaneViolet) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(prop.title, style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
                                RpgTagChip(prop.difficulty.displayName, difficultyAccent(prop.difficulty))
                            }
                            Text(prop.objectiveTitle, style = MaterialTheme.typography.labelMedium, color = SystemColors.ElectricCyan)
                            Text("Rationale: ${prop.reason}", style = MaterialTheme.typography.bodySmall, color = SystemColors.MutedSilver)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onApproveAiProposal(prop.id) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("btn_approve_ai_dungeon_${prop.id}")
                                ) {
                                    Text("APPROVE GATE")
                                }
                                OutlinedButton(
                                    onClick = { onRejectAiProposal(prop.id) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("btn_reject_ai_dungeon_${prop.id}")
                                ) {
                                    Text("DISMISS")
                                }
                            }
                        }
                    }
                }

                item {
                    SystemSectionTitle(
                        title = "All Dungeon Gates",
                        subtitle = "Filter by category or rank tier"
                    )
                }

                item {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("All Types") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SystemColors.ElectricCyan.copy(alpha = 0.2f))
                        )
                        DungeonCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                                label = { Text(cat.label.substringBefore(" ")) }
                            )
                        }
                    }
                }

                item {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        DungeonDifficulty.entries.forEach { diff ->
                            FilterChip(
                                selected = selectedDifficulty == diff,
                                onClick = { selectedDifficulty = if (selectedDifficulty == diff) null else diff },
                                label = { Text(diff.code) }
                            )
                        }
                    }
                }

                items(filtered, key = { "disc_${it.definition.id}" }) { item ->
                    DungeonSummaryCard(
                        dungeon = item,
                        onClick = { onOpenDungeonDetails(item.definition.id) }
                    )
                }
            }
        }
    }
}

// 3. DUNGEON DETAILS
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DungeonDetailsScreen(
    dungeonId: Long,
    uiState: RpgUiState,
    onRetry: () -> Unit,
    onClearFeedback: () -> Unit,
    onEnterDungeon: (Long) -> Unit,
    onOpenActiveDungeon: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    RpgStateHost(uiState = uiState, onRetry = onRetry, modifier = modifier) { ready ->
        val item = ready.snapshot.dungeons.find { it.definition.id == dungeonId }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            if (item == null) {
                Box(modifier = Modifier.padding(16.dp)) {
                    SystemEmptyState("Dungeon Gate Not Found", "This dungeon may have rotated or been removed.")
                }
            } else {
                val def = item.definition
                val accent = difficultyAccent(def.difficulty)
                val activeInst = item.latestInstance?.takeIf {
                    it.status == DungeonStatus.ACTIVE || it.status == DungeonStatus.PAUSED
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 760.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item { RpgFeedbackBanner(ready.feedbackBanner, onClearFeedback) }

                    item {
                        HolographicPanel(accentColor = accent) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    RpgTagChip(def.difficulty.displayName, accent)
                                    RpgTagChip(def.category.label, SystemColors.ArcaneViolet)
                                    RpgTagChip(item.effectiveStatus.label.uppercase(), statusAccent(item.effectiveStatus))
                                }
                                Text(def.title, style = MaterialTheme.typography.headlineSmall, color = SystemColors.PureIce)
                                Text(def.description, style = MaterialTheme.typography.bodyMedium, color = SystemColors.MetallicSilver)
                                Text(
                                    text = "Window: ${def.timeLimitHours}h • Cleared ${item.completedCount} time(s) • Source: ${def.creationSource.label}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SystemColors.MutedSilver
                                )
                            }
                        }
                    }

                    item {
                        HolographicPanel(accentColor = if (item.isEligibleToEnter) SystemColors.VerifiedEmerald else SystemColors.DangerCrimson) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("ENTRY REQUIREMENTS", style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
                                Text(
                                    text = "Required Rank: ${def.requiredRank.displayName} • Min Level: ${def.requiredLevel}" +
                                        (def.requiredAttributeCode?.let { " • $it ${def.requiredAttributeValue}+" } ?: ""),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SystemColors.MetallicSilver
                                )
                                if (item.eligibilityBlockers.isNotEmpty()) {
                                    item.eligibilityBlockers.forEach { blocker ->
                                        Text("• Blocked: $blocker", style = MaterialTheme.typography.bodySmall, color = SystemColors.DangerCrimson)
                                    }
                                } else {
                                    Text("All entry criteria satisfied.", style = MaterialTheme.typography.labelMedium, color = SystemColors.VerifiedEmerald)
                                }
                            }
                        }
                    }

                    item {
                        SystemSectionTitle("Required Clear Objectives", "Tracked deterministically from real records after entry")
                    }

                    items(item.objectives, key = { "obj_${it.id}" }) { obj ->
                        HolographicPanel(accentColor = SystemColors.ElectricCyan) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (obj.requiresVerifiedOnly) Icons.Rounded.VerifiedUser else Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = if (obj.requiresVerifiedOnly) SystemColors.VerifiedEmerald else SystemColors.ElectricCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(obj.title, style = MaterialTheme.typography.titleSmall, color = SystemColors.PureIce)
                                    Text(
                                        text = "Target: ${obj.targetValue} ${obj.evidenceType.unit}" +
                                            if (obj.requiresVerifiedOnly) " • Verified Only" else " • Verified (1.0x) or Logged (0.75x)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SystemColors.MutedSilver
                                    )
                                }
                            }
                        }
                    }

                    item {
                        HolographicPanel(accentColor = SystemColors.MonarchGold) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("BOUNDED COMPLETION REWARD", style = MaterialTheme.typography.titleMedium, color = SystemColors.MonarchGold)
                                Text(
                                    text = "Up to ${def.baseXpReward} Base XP (subject to Daily RPG XP Cap) • +${def.primaryAttrXp} ${def.primaryAttribute.code} XP • +${def.secondaryAttrXp} ${def.secondaryAttribute.code} XP",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SystemColors.PureIce
                                )
                                val extras = listOfNotNull(
                                    def.rewardTitleId?.let { "Title Unlock" },
                                    def.rewardItemId?.let { "Inventory Item" },
                                    def.rewardShadowId?.let { "Shadow Collectible" }
                                )
                                if (extras.isNotEmpty()) {
                                    Text("Drops: ${extras.joinToString(" • ")}", style = MaterialTheme.typography.labelMedium, color = SystemColors.ElectricCyan)
                                }
                            }
                        }
                    }

                    item {
                        if (activeInst != null) {
                            Button(
                                onClick = { onOpenActiveDungeon(activeInst.id) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("btn_open_active_dungeon_from_details"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SystemColors.VerifiedEmerald,
                                    contentColor = SystemColors.ObsidianBlack
                                )
                            ) {
                                Text("OPEN ACTIVE DUNGEON")
                            }
                        } else {
                            Button(
                                onClick = { onEnterDungeon(def.id) },
                                enabled = item.isEligibleToEnter && item.effectiveStatus != DungeonStatus.COMPLETED,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("btn_enter_dungeon_${def.id}"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SystemColors.ElectricCyan,
                                    contentColor = SystemColors.ObsidianBlack
                                )
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (!item.isEligibleToEnter) "LOCKED — MEET RANK CRITERIA"
                                    else if (item.effectiveStatus == DungeonStatus.COMPLETED) "NON-REPEATABLE GATE CLEARED"
                                    else "ENTER DUNGEON GATE (0 XP)"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 4. ACTIVE DUNGEON SCREEN
@Composable
fun ActiveDungeonScreen(
    instanceId: Long,
    uiState: RpgUiState,
    onRetry: () -> Unit,
    onClearFeedback: () -> Unit,
    onPauseResume: (Long) -> Unit,
    onCompleteDungeon: (Long) -> Unit,
    onAbandonDungeon: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    RpgStateHost(uiState = uiState, onRetry = onRetry, modifier = modifier) { ready ->
        val item = ready.snapshot.dungeons.find { it.latestInstance?.id == instanceId }
        val inst = item?.latestInstance
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            if (item == null || inst == null) {
                Box(Modifier.padding(16.dp)) {
                    SystemEmptyState("Active Dungeon Closed", "This dungeon instance is no longer active.")
                }
            } else {
                val allMet = item.objectiveProgress.isNotEmpty() && item.objectiveProgress.all { it.isSatisfied }
                val remainingHours = ((inst.deadlineEpochMs + inst.accumulatedPausedMs - System.currentTimeMillis()) / 3_600_000L).coerceAtLeast(0L)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 760.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item { RpgFeedbackBanner(ready.feedbackBanner, onClearFeedback) }

                    item {
                        HolographicPanel(accentColor = statusAccent(inst.status)) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(item.definition.title, style = MaterialTheme.typography.titleLarge, color = SystemColors.PureIce)
                                    RpgTagChip(inst.status.label.uppercase(), statusAccent(inst.status))
                                }
                                GlowingProgressBar(
                                    progress = item.overallProgressFraction,
                                    height = 10.dp,
                                    startColor = SystemColors.ElectricCyan,
                                    endColor = SystemColors.VerifiedEmerald
                                )
                                Text(
                                    text = "${(item.overallProgressFraction * 100).toInt()}% Validated • ~${remainingHours}h remaining",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SystemColors.MetallicSilver
                                )
                            }
                        }
                    }

                    item {
                        SystemSectionTitle("Live Objective Validation", "Progress updates automatically as you log sessions")
                    }

                    items(item.objectiveProgress, key = { "prog_${it.objective.id}" }) { objProg ->
                        val accent = if (objProg.isSatisfied) SystemColors.VerifiedEmerald else SystemColors.ElectricCyan
                        HolographicPanel(accentColor = accent) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(objProg.objective.title, style = MaterialTheme.typography.titleSmall, color = SystemColors.PureIce)
                                    Text("${objProg.currentValue} / ${objProg.targetValue}", style = MaterialTheme.typography.labelLarge, color = accent)
                                }
                                GlowingProgressBar(progress = objProg.progressFraction, height = 7.dp, startColor = accent, endColor = SystemColors.ArcaneViolet)
                                Text(objProg.verificationNote, style = MaterialTheme.typography.bodySmall, color = SystemColors.MutedSilver)
                            }
                        }
                    }

                    if (inst.status == DungeonStatus.ACTIVE || inst.status == DungeonStatus.PAUSED) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { onCompleteDungeon(inst.id) },
                                    enabled = inst.status == DungeonStatus.ACTIVE && allMet,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("btn_validate_complete_dungeon"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SystemColors.VerifiedEmerald,
                                        contentColor = SystemColors.ObsidianBlack
                                    )
                                ) {
                                    Text(if (allMet) "VALIDATE & CLEAR DUNGEON" else "COMPLETE ALL OBJECTIVES TO CLEAR")
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { onPauseResume(inst.id) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("btn_pause_resume_dungeon")
                                    ) {
                                        Icon(
                                            imageVector = if (inst.status == DungeonStatus.PAUSED) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(if (inst.status == DungeonStatus.PAUSED) "RESUME" else "PAUSE")
                                    }
                                    OutlinedButton(
                                        onClick = { onAbandonDungeon(inst.id) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("btn_abandon_dungeon")
                                    ) {
                                        Text("ABANDON", color = SystemColors.DangerCrimson)
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

// 5. DUNGEON HISTORY
@Composable
fun DungeonHistoryScreen(
    uiState: RpgUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    RpgStateHost(uiState = uiState, onRetry = onRetry, modifier = modifier) { ready ->
        val records = ready.snapshot.completionHistory
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 760.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    SystemSectionTitle("Dungeon Completion Log", "${records.size} verified clear(s) recorded")
                }
                if (records.isEmpty()) {
                    item {
                        SystemEmptyState("No Dungeons Cleared Yet", "Enter an available gate and clear real objectives to build your raid record.")
                    }
                } else {
                    items(records, key = { "hist_${it.id}" }) { rec ->
                        val accent = difficultyAccent(rec.difficulty)
                        HolographicPanel(accentColor = accent) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(rec.dungeonTitle, style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
                                    Text("+${rec.xpAwarded} XP", style = MaterialTheme.typography.labelLarge, color = SystemColors.MonarchGold)
                                }
                                Text(rec.evidenceSummary, style = MaterialTheme.typography.bodySmall, color = SystemColors.MetallicSilver)
                                Text(
                                    text = "${rec.verificationProvenance} • ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(rec.completedAtEpochMs))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SystemColors.ElectricCyan
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DungeonSummaryCard(
    dungeon: DungeonWithStatus,
    onClick: () -> Unit
) {
    val def = dungeon.definition
    val status = dungeon.effectiveStatus
    val accent = if (status == DungeonStatus.LOCKED) SystemColors.MutedSilver else difficultyAccent(def.difficulty)

    HolographicPanel(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("dungeon_card_${def.code}"),
        accentColor = accent
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = if (status == DungeonStatus.LOCKED) Icons.Rounded.Lock else Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(def.title, style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
                }
                RpgTagChip(status.label.uppercase(), statusAccent(status))
            }
            Text(def.description, style = MaterialTheme.typography.bodySmall, color = SystemColors.MetallicSilver)
            if (status == DungeonStatus.ACTIVE || status == DungeonStatus.PAUSED) {
                GlowingProgressBar(progress = dungeon.overallProgressFraction, height = 6.dp)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${def.difficulty.displayName} • ${def.timeLimitHours}h", style = MaterialTheme.typography.labelSmall, color = accent)
                Text("Up to ${def.baseXpReward} XP", style = MaterialTheme.typography.labelSmall, color = SystemColors.MonarchGold)
            }
        }
    }
}
