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
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.BossBattleWithPhases
import com.example.domain.model.BossStatus
import com.example.ui.components.GlowingProgressBar
import com.example.ui.components.HolographicPanel
import com.example.ui.components.SystemEmptyState
import com.example.ui.components.SystemSectionTitle
import com.example.ui.theme.SystemColors
import com.example.ui.viewmodel.RpgUiState

// 6. BOSS BATTLE DASHBOARD
@Composable
fun BossBattleDashboardScreen(
    uiState: RpgUiState,
    onRetry: () -> Unit,
    onClearFeedback: () -> Unit,
    onOpenBossDetails: (Long) -> Unit,
    onOpenActiveBoss: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    RpgStateHost(uiState = uiState, onRetry = onRetry, modifier = modifier) { ready ->
        val bosses = ready.snapshot.bossBattles
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 760.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { RpgFeedbackBanner(ready.feedbackBanner, onClearFeedback) }

                item {
                    HolographicPanel(accentColor = SystemColors.DangerCrimson, contentPadding = 0.dp) {
                        Column {
                            Image(
                                painter = painterResource(id = R.drawable.img_boss_citadel_banner),
                                contentDescription = "Boss Citadel Campaign Banner",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(145.dp)
                                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("BOSS RAID CAMPAIGNS", style = MaterialTheme.typography.titleLarge, color = SystemColors.PureIce)
                                Text(
                                    text = "Multi-stage real-world development campaigns. Boss health drops deterministically only as verified & logged milestone phases complete.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SystemColors.MetallicSilver
                                )
                            }
                        }
                    }
                }

                item {
                    SystemSectionTitle("Campaign Targets", "${bosses.count { it.effectiveStatus == BossStatus.COMPLETED }} / ${bosses.size} conquered")
                }

                items(bosses, key = { "boss_${it.definition.id}" }) { boss ->
                    BossSummaryCard(
                        boss = boss,
                        onClick = {
                            val activeInstId = boss.latestInstance?.takeIf { it.status == BossStatus.ACTIVE }?.id
                            if (activeInstId != null) onOpenActiveBoss(activeInstId)
                            else onOpenBossDetails(boss.definition.id)
                        }
                    )
                }
            }
        }
    }
}

// 7. BOSS BATTLE DETAILS
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BossBattleDetailsScreen(
    bossId: Long,
    uiState: RpgUiState,
    onRetry: () -> Unit,
    onClearFeedback: () -> Unit,
    onStartBossBattle: (Long) -> Unit,
    onOpenActiveBoss: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    RpgStateHost(uiState = uiState, onRetry = onRetry, modifier = modifier) { ready ->
        val boss = ready.snapshot.bossBattles.find { it.definition.id == bossId }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            if (boss == null) {
                Box(Modifier.padding(16.dp)) {
                    SystemEmptyState("Boss Campaign Not Found", "Unable to locate selected campaign.")
                }
            } else {
                val def = boss.definition
                val activeInst = boss.latestInstance?.takeIf { it.status == BossStatus.ACTIVE }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 760.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item { RpgFeedbackBanner(ready.feedbackBanner, onClearFeedback) }

                    item {
                        HolographicPanel(accentColor = SystemColors.DangerCrimson) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    RpgTagChip(def.difficulty.displayName, difficultyAccent(def.difficulty))
                                    RpgTagChip(boss.effectiveStatus.label.uppercase(), SystemColors.DangerCrimson)
                                }
                                Text(def.title, style = MaterialTheme.typography.headlineSmall, color = SystemColors.PureIce)
                                Text(def.subtitle, style = MaterialTheme.typography.labelLarge, color = SystemColors.ElectricCyan)
                                Text(def.description, style = MaterialTheme.typography.bodyMedium, color = SystemColors.MetallicSilver)
                            }
                        }
                    }

                    item {
                        HolographicPanel(accentColor = if (boss.isEligible) SystemColors.VerifiedEmerald else SystemColors.DangerCrimson) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("CAMPAIGN ELIGIBILITY", style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
                                Text("Required Rank: ${def.requiredRank.displayName} • Min Level: ${def.requiredLevel}", style = MaterialTheme.typography.bodySmall, color = SystemColors.MetallicSilver)
                                boss.eligibilityBlockers.forEach { b ->
                                    Text("• $b", style = MaterialTheme.typography.bodySmall, color = SystemColors.DangerCrimson)
                                }
                            }
                        }
                    }

                    item { SystemSectionTitle("Campaign Phases", "All phases contribute deterministically to Boss HP") }

                    items(boss.phases, key = { "phase_${it.id}" }) { phase ->
                        HolographicPanel(accentColor = SystemColors.ArcaneViolet) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (phase.requiresVerifiedOnly) Icons.Rounded.VerifiedUser else Icons.Rounded.Bolt,
                                    contentDescription = null,
                                    tint = SystemColors.ArcaneViolet,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(phase.title, style = MaterialTheme.typography.titleSmall, color = SystemColors.PureIce)
                                    Text(phase.description, style = MaterialTheme.typography.bodySmall, color = SystemColors.MetallicSilver)
                                }
                                RpgTagChip("${phase.weight}% HP", SystemColors.DangerCrimson)
                            }
                        }
                    }

                    item {
                        HolographicPanel(accentColor = SystemColors.MonarchGold) {
                            Text(
                                text = "Victory Rewards: Up to ${def.xpReward} XP • +${def.primaryAttrXp} ${def.primaryAttribute.code} XP • +${def.secondaryAttrXp} ${def.secondaryAttribute.code} XP" +
                                    (def.rewardTitleId?.let { " + Exclusive Title" } ?: "") +
                                    (def.rewardShadowId?.let { " + Shadow Collectible" } ?: ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = SystemColors.MonarchGold
                            )
                        }
                    }

                    item {
                        if (activeInst != null) {
                            Button(
                                onClick = { onOpenActiveBoss(activeInst.id) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("btn_open_active_boss"),
                                colors = ButtonDefaults.buttonColors(containerColor = SystemColors.DangerCrimson)
                            ) {
                                Text("OPEN ACTIVE BOSS BATTLE")
                            }
                        } else {
                            Button(
                                onClick = { onStartBossBattle(def.id) },
                                enabled = boss.isEligible && boss.effectiveStatus != BossStatus.COMPLETED,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("btn_engage_boss_${def.id}"),
                                colors = ButtonDefaults.buttonColors(containerColor = SystemColors.DangerCrimson)
                            ) {
                                Text(
                                    if (!boss.isEligible) "CAMPAIGN LOCKED"
                                    else if (boss.effectiveStatus == BossStatus.COMPLETED) "CAMPAIGN CONQUERED"
                                    else "ENGAGE BOSS CAMPAIGN"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 8. ACTIVE BOSS BATTLE SCREEN
@Composable
fun ActiveBossBattleScreen(
    instanceId: Long,
    uiState: RpgUiState,
    onRetry: () -> Unit,
    onClearFeedback: () -> Unit,
    onRefreshAndClaim: (Long) -> Unit,
    onAbandonBoss: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    RpgStateHost(uiState = uiState, onRetry = onRetry, modifier = modifier) { ready ->
        val boss = ready.snapshot.bossBattles.find { it.latestInstance?.id == instanceId }
        val inst = boss?.latestInstance
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            if (boss == null || inst == null) {
                Box(Modifier.padding(16.dp)) {
                    SystemEmptyState("Boss Instance Closed", "Return to Boss Raid Dashboard to select another campaign.")
                }
            } else {
                val allPhasesComplete = boss.phaseProgress.isNotEmpty() && boss.phaseProgress.all { it.isCompleted }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 760.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item { RpgFeedbackBanner(ready.feedbackBanner, onClearFeedback) }

                    item {
                        HolographicPanel(accentColor = SystemColors.DangerCrimson) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(boss.definition.title, style = MaterialTheme.typography.titleLarge, color = SystemColors.PureIce)
                                    Text("HP ${boss.healthRemainingPercent}%", style = MaterialTheme.typography.titleMedium, color = SystemColors.DangerCrimson)
                                }
                                GlowingProgressBar(
                                    progress = boss.healthRemainingFraction,
                                    height = 14.dp,
                                    startColor = SystemColors.DangerCrimson,
                                    endColor = SystemColors.ArcaneViolet
                                )
                                Text(
                                    text = "Deterministic Boss Health — reduced purely by verified/qualifying phase completion.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SystemColors.MutedSilver
                                )
                            }
                        }
                    }

                    item { SystemSectionTitle("Phase Breakdown", "Progress updates automatically from Physical, Academic & Life logs") }

                    items(boss.phaseProgress, key = { "bp_${it.phase.id}" }) { pp ->
                        val accent = if (pp.isCompleted) SystemColors.VerifiedEmerald else SystemColors.DangerCrimson
                        HolographicPanel(accentColor = accent) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        if (pp.isCompleted) {
                                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = SystemColors.VerifiedEmerald, modifier = Modifier.size(18.dp))
                                            Spacer(Modifier.width(6.dp))
                                        }
                                        Text(pp.phase.title, style = MaterialTheme.typography.titleSmall, color = SystemColors.PureIce)
                                    }
                                    Text("${pp.currentValue} / ${pp.phase.targetValue}", style = MaterialTheme.typography.labelLarge, color = accent)
                                }
                                GlowingProgressBar(progress = pp.progressFraction, height = 7.dp, startColor = accent, endColor = SystemColors.MonarchGold)
                                Text(pp.verificationNote, style = MaterialTheme.typography.bodySmall, color = SystemColors.MutedSilver)
                            }
                        }
                    }

                    if (inst.status == BossStatus.ACTIVE) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { onRefreshAndClaim(inst.id) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("btn_claim_boss_victory"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (allPhasesComplete) SystemColors.MonarchGold else SystemColors.ElectricCyan,
                                        contentColor = SystemColors.ObsidianBlack
                                    )
                                ) {
                                    Text(if (allPhasesComplete) "CLAIM VICTORY & REWARDS" else "CHECK PHASE TELEMETRY")
                                }
                                OutlinedButton(
                                    onClick = { onAbandonBoss(inst.id) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("btn_retreat_boss")
                                ) {
                                    Text("RETREAT FROM CAMPAIGN", color = SystemColors.DangerCrimson)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BossSummaryCard(
    boss: BossBattleWithPhases,
    onClick: () -> Unit
) {
    val accent = when (boss.effectiveStatus) {
        BossStatus.ACTIVE -> SystemColors.DangerCrimson
        BossStatus.COMPLETED -> SystemColors.MonarchGold
        BossStatus.LOCKED -> SystemColors.MutedSilver
        else -> SystemColors.ArcaneViolet
    }
    HolographicPanel(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("boss_card_${boss.definition.code}"),
        accentColor = accent
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = if (boss.effectiveStatus == BossStatus.LOCKED) Icons.Rounded.Lock else Icons.Rounded.Bolt,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(boss.definition.title, style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
                }
                RpgTagChip(boss.effectiveStatus.label.uppercase(), accent)
            }
            Text(boss.definition.subtitle, style = MaterialTheme.typography.labelMedium, color = SystemColors.ElectricCyan)
            if (boss.effectiveStatus == BossStatus.ACTIVE) {
                GlowingProgressBar(
                    progress = boss.healthRemainingFraction,
                    height = 8.dp,
                    startColor = SystemColors.DangerCrimson,
                    endColor = SystemColors.ArcaneViolet
                )
                Text("Remaining Boss HP: ${boss.healthRemainingPercent}%", style = MaterialTheme.typography.labelSmall, color = SystemColors.DangerCrimson)
            } else {
                Text("${boss.phases.size} phases • Up to ${boss.definition.xpReward} XP", style = MaterialTheme.typography.labelSmall, color = SystemColors.MutedSilver)
            }
        }
    }
}
