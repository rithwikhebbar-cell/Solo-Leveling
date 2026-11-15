package com.example.ui.screens.rpg

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.SkillSpecialization
import com.example.domain.model.SkillWithStatus
import com.example.ui.components.HolographicPanel
import com.example.ui.components.SystemEmptyState
import com.example.ui.components.SystemSectionTitle
import com.example.ui.theme.SystemColors
import com.example.ui.viewmodel.RpgUiState

// 9. SKILL DASHBOARD
@Composable
fun SkillDashboardScreen(
    uiState: RpgUiState,
    onRetry: () -> Unit,
    onClearFeedback: () -> Unit,
    onOpenTreeExplorer: (SkillSpecialization?) -> Unit,
    onOpenSkillDetails: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    RpgStateHost(uiState = uiState, onRetry = onRetry, modifier = modifier) { ready ->
        val snap = ready.snapshot
        val effects = snap.activeSkillEffects
        val readyToUnlock = snap.skills.filter { !it.isUnlocked && it.canUnlockOrUpgrade }

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
                    HolographicPanel(accentColor = SystemColors.ElectricCyan) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("SKILL & SPECIALIZATION ENGINE", style = MaterialTheme.typography.titleLarge, color = SystemColors.PureIce)
                                RpgTagChip("${effects.unlockedSkillCount}/${snap.skills.size} UNLOCKED", SystemColors.ElectricCyan)
                            }
                            Text(
                                text = "Skills unlock deterministically from verified attributes, rank, and tree prerequisites—never purchasable and never altering health/XP safety bounds.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SystemColors.MetallicSilver
                            )
                            Button(
                                onClick = { onOpenTreeExplorer(null) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("btn_open_skill_tree_explorer"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SystemColors.ElectricCyan,
                                    contentColor = SystemColors.ObsidianBlack
                                )
                            ) {
                                Icon(Icons.Rounded.AccountTree, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("OPEN SKILL TREE EXPLORER")
                            }
                        }
                    }
                }

                item {
                    HolographicPanel(accentColor = SystemColors.ArcaneViolet) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("ACTIVE SKILL EFFECT ENGINE PERKS", style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
                            Text("Focus Presets: ${effects.unlockedFocusPresetsMinutes.joinToString { "${it}m" }}", style = MaterialTheme.typography.labelMedium, color = SystemColors.ElectricCyan)
                            Text("Challenge Tiers: ${effects.unlockedChallengeTiers.joinToString()}", style = MaterialTheme.typography.labelMedium, color = SystemColors.VerifiedEmerald)
                            if (effects.activeEffectExplanations.isEmpty()) {
                                Text("No special perks active yet. Unlock Tier 1 skills below as your attributes rise.", style = MaterialTheme.typography.bodySmall, color = SystemColors.MutedSilver)
                            } else {
                                effects.activeEffectExplanations.take(4).forEach { line ->
                                    Text("• $line", style = MaterialTheme.typography.bodySmall, color = SystemColors.MetallicSilver)
                                }
                            }
                        }
                    }
                }

                if (readyToUnlock.isNotEmpty()) {
                    item { SystemSectionTitle("Ready to Unlock", "${readyToUnlock.size} skill(s) meet all criteria") }
                    items(readyToUnlock, key = { "ready_${it.definition.id}" }) { sk ->
                        SkillNodeCard(skill = sk, onClick = { onOpenSkillDetails(sk.definition.id) })
                    }
                }

                item { SystemSectionTitle("5 Specialization Trees", "Tap a specialization to explore its dependency tree") }

                items(SkillSpecialization.entries, key = { it.name }) { spec ->
                    val treeSkills = snap.skills.filter { it.definition.specialization == spec }
                    val unlockedInTree = treeSkills.count { it.isUnlocked }
                    val accent = SystemColors.attributeAccent(spec.primaryAttr)
                    HolographicPanel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenTreeExplorer(spec) }
                            .testTag("skill_tree_card_${spec.name}"),
                        accentColor = accent
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(spec.label, style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
                                RpgTagChip("$unlockedInTree / ${treeSkills.size}", accent)
                            }
                            Text(spec.description, style = MaterialTheme.typography.bodySmall, color = SystemColors.MetallicSilver)
                        }
                    }
                }
            }
        }
    }
}

// 10. SKILL TREE EXPLORER
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SkillTreeExplorerScreen(
    initialSpecializationName: String?,
    uiState: RpgUiState,
    onRetry: () -> Unit,
    onClearFeedback: () -> Unit,
    onOpenSkillDetails: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    RpgStateHost(uiState = uiState, onRetry = onRetry, modifier = modifier) { ready ->
        val defaultSpec = initialSpecializationName?.let {
            runCatching { SkillSpecialization.valueOf(it) }.getOrNull()
        } ?: SkillSpecialization.PHYSICAL_DEVELOPMENT
        var selectedSpec by remember { mutableStateOf(defaultSpec) }

        val treeSkills = ready.snapshot.skills
            .filter { it.definition.specialization == selectedSpec }
            .sortedWith(compareBy({ it.definition.treeRow }, { it.definition.treeColumn }))

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
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SkillSpecialization.entries.forEach { spec ->
                            FilterChip(
                                selected = selectedSpec == spec,
                                onClick = { selectedSpec = spec },
                                label = { Text(spec.label.substringBefore(" ")) }
                            )
                        }
                    }
                }

                item {
                    SystemSectionTitle(selectedSpec.label, selectedSpec.description)
                }

                items(treeSkills, key = { "tree_${it.definition.id}" }) { skill ->
                    SkillNodeCard(
                        skill = skill,
                        onClick = { onOpenSkillDetails(skill.definition.id) }
                    )
                }
            }
        }
    }
}

// 11. SKILL DETAILS
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SkillDetailsScreen(
    skillId: String,
    uiState: RpgUiState,
    onRetry: () -> Unit,
    onClearFeedback: () -> Unit,
    onUnlockOrUpgrade: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    RpgStateHost(uiState = uiState, onRetry = onRetry, modifier = modifier) { ready ->
        val skill = ready.snapshot.skills.find { it.definition.id == skillId }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            if (skill == null) {
                Box(Modifier.padding(16.dp)) {
                    SystemEmptyState("Skill Not Found", "Selected skill node is unavailable.")
                }
            } else {
                val def = skill.definition
                val accent = SystemColors.attributeAccent(def.requiredAttribute)
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
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    RpgTagChip(def.specialization.label, accent)
                                    RpgTagChip(def.tier.label, SystemColors.ArcaneViolet)
                                    RpgTagChip(
                                        if (skill.isUnlocked) "MASTERY LV.${skill.currentMastery}/${def.maxMasteryLevel}" else "LOCKED",
                                        if (skill.isUnlocked) SystemColors.VerifiedEmerald else SystemColors.MutedSilver
                                    )
                                }
                                Text(def.name, style = MaterialTheme.typography.headlineSmall, color = SystemColors.PureIce)
                                Text(def.description, style = MaterialTheme.typography.bodyMedium, color = SystemColors.MetallicSilver)
                            }
                        }
                    }

                    item {
                        HolographicPanel(accentColor = SystemColors.ElectricCyan) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("UNLOCK & UPGRADE CRITERIA", style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
                                Text(
                                    text = "Base: ${def.requiredRank.displayName} • Lv.${def.requiredLevel} • ${def.requiredAttribute.code} ${def.requiredAttributeValue}+" +
                                        (if (def.requiredVerifiedCount > 0) " • ${def.requiredVerifiedCount} verified activities" else "") +
                                        (if (def.requiredStreakDays > 0) " • ${def.requiredStreakDays}d streak" else ""),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SystemColors.MetallicSilver
                                )
                                if (skill.missingRequirements.isEmpty()) {
                                    Text("All criteria met for unlock / mastery upgrade!", style = MaterialTheme.typography.labelMedium, color = SystemColors.VerifiedEmerald)
                                } else {
                                    skill.missingRequirements.forEach { req ->
                                        Text("• $req", style = MaterialTheme.typography.bodySmall, color = SystemColors.DangerCrimson)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        HolographicPanel(accentColor = SystemColors.ArcaneViolet) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("SYSTEM PERKS (SKILLEFFECTENGINE)", style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
                                skill.effects.forEach { eff ->
                                    Text("• ${eff.title}: ${eff.description}", style = MaterialTheme.typography.bodySmall, color = SystemColors.MetallicSilver)
                                }
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = { onUnlockOrUpgrade(def.id) },
                            enabled = skill.canUnlockOrUpgrade,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("btn_unlock_upgrade_skill_${def.id}"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (skill.isUnlocked) SystemColors.ArcaneViolet else SystemColors.VerifiedEmerald,
                                contentColor = SystemColors.ObsidianBlack
                            )
                        ) {
                            Text(
                                when {
                                    skill.currentMastery >= def.maxMasteryLevel -> "MAX MASTERY REACHED"
                                    skill.isUnlocked -> "UPGRADE MASTERY (LV.${skill.currentMastery + 1})"
                                    skill.canUnlockOrUpgrade -> "UNLOCK SKILL"
                                    else -> "REQUIREMENTS NOT MET"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SkillNodeCard(
    skill: SkillWithStatus,
    onClick: () -> Unit
) {
    val def = skill.definition
    val accent = when {
        skill.isUnlocked -> SystemColors.VerifiedEmerald
        skill.canUnlockOrUpgrade -> SystemColors.ElectricCyan
        else -> SystemColors.MutedSilver
    }
    HolographicPanel(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("skill_card_${def.id}"),
        accentColor = accent
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = when {
                            skill.isUnlocked -> Icons.Rounded.CheckCircle
                            skill.canUnlockOrUpgrade -> Icons.Rounded.Bolt
                            else -> Icons.Rounded.Lock
                        },
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(def.name, style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
                }
                RpgTagChip(
                    label = if (skill.isUnlocked) "LV.${skill.currentMastery}/${def.maxMasteryLevel}"
                    else if (skill.canUnlockOrUpgrade) "READY"
                    else "LOCKED",
                    accent = accent
                )
            }
            Text(def.description, style = MaterialTheme.typography.bodySmall, color = SystemColors.MetallicSilver)
            if (skill.prerequisites.isNotEmpty()) {
                Text(
                    text = "Prerequisite: ${skill.prerequisites.joinToString { it.prerequisiteSkillId }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SystemColors.ArcaneViolet
                )
            }
        }
    }
}
