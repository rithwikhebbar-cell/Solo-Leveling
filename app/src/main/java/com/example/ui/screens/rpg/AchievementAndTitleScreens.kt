package com.example.ui.screens.rpg

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import com.example.domain.model.AchievementCategory
import com.example.domain.model.AchievementWithStatus
import com.example.ui.components.HolographicPanel
import com.example.ui.components.SystemSectionTitle
import com.example.ui.theme.SystemColors
import com.example.ui.viewmodel.RpgUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ============================================================================
// SCREEN 12: ACHIEVEMENT GALLERY SCREEN (10 Categories)
// ============================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AchievementGalleryScreen(
    uiState: RpgUiState,
    onRetry: () -> Unit,
    onClearFeedback: () -> Unit,
    onOpenAchievementDetails: (String) -> Unit,
    onOpenTitles: () -> Unit,
    onOpenInventory: () -> Unit,
    onOpenShadows: () -> Unit,
    onOpenRewardHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<AchievementCategory?>(null) }

    RpgStateHost(uiState = uiState, onRetry = onRetry, modifier = modifier) { ready ->
        val snapshot = ready.snapshot
        val achievements = snapshot.achievements
        val unlockedCount = achievements.count { it.isUnlocked }
        val filtered = remember(achievements, selectedCategory) {
            if (selectedCategory == null) achievements
            else achievements.filter { it.definition.category == selectedCategory }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 820.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                RpgFeedbackBanner(message = ready.feedbackBanner, onDismiss = onClearFeedback)
            }
            item {
                SystemSectionTitle(
                    title = "Achievement Vault",
                    subtitle = "$unlockedCount / ${achievements.size} milestones verified across 10 categories"
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RpgMetricMiniCard(
                        label = "Unlocked",
                        value = "$unlockedCount/${achievements.size}",
                        accent = SystemColors.MonarchGold,
                        modifier = Modifier.weight(1f)
                    )
                    RpgMetricMiniCard(
                        label = "Equipped Title",
                        value = snapshot.profile.title,
                        accent = SystemColors.ElectricCyan,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenTitles,
                        modifier = Modifier.testTag("ach_nav_titles")
                    ) {
                        Icon(Icons.Rounded.MilitaryTech, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Titles (${snapshot.titles.count { it.isUnlocked }})")
                    }
                    OutlinedButton(
                        onClick = onOpenInventory,
                        modifier = Modifier.testTag("ach_nav_inventory")
                    ) {
                        Icon(Icons.Rounded.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Inventory (${snapshot.inventoryItems.count { it.isOwned }})")
                    }
                    OutlinedButton(
                        onClick = onOpenShadows,
                        modifier = Modifier.testTag("ach_nav_shadows")
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Shadows (${snapshot.shadowCollection.count { it.isUnlocked }})")
                    }
                    OutlinedButton(
                        onClick = onOpenRewardHistory,
                        modifier = Modifier.testTag("ach_nav_ledger")
                    ) {
                        Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("XP Ledger")
                    }
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("All (10)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SystemColors.MonarchGold.copy(alpha = 0.2f),
                                selectedLabelColor = SystemColors.MonarchGold
                            )
                        )
                    }
                    items(AchievementCategory.entries) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = {
                                selectedCategory = if (selectedCategory == category) null else category
                            },
                            label = { Text(category.label.removeSuffix(" Achievements")) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SystemColors.ElectricCyan.copy(alpha = 0.2f),
                                selectedLabelColor = SystemColors.ElectricCyan
                            )
                        )
                    }
                }
            }
            if (filtered.isEmpty()) {
                item {
                    HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.MutedSilver) {
                        Text(
                            text = "No achievements registered in ${selectedCategory?.label ?: "this filter"} yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SystemColors.MutedSilver
                        )
                    }
                }
            } else {
                items(filtered, key = { it.definition.id }) { item ->
                    AchievementCard(
                        item = item,
                        onClick = { onOpenAchievementDetails(item.definition.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AchievementCard(
    item: AchievementWithStatus,
    onClick: () -> Unit
) {
    val accent = if (item.isUnlocked) rarityAccent(item.definition.rarity) else SystemColors.MutedSilver
    HolographicPanel(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("achievement_card_${item.definition.id}"),
        accentColor = accent
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (item.isUnlocked) Icons.Rounded.EmojiEvents else Icons.Rounded.Lock,
                    contentDescription = item.definition.title,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.definition.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (item.isUnlocked) SystemColors.PureIce else SystemColors.MetallicSilver
                    )
                    Text(
                        text = item.definition.category.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = SystemColors.MutedSilver
                    )
                }
                RpgTagChip(
                    label = if (item.isUnlocked) "${item.definition.rarity.label} ✓" else item.definition.rarity.label,
                    accent = accent
                )
            }
            Text(
                text = item.definition.description,
                style = MaterialTheme.typography.bodySmall,
                color = SystemColors.MetallicSilver
            )
            LinearProgressIndicator(
                progress = { item.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = accent,
                trackColor = SystemColors.PanelSurfaceElevated
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${item.currentValue.coerceAtMost(item.definition.targetValue)} / ${item.definition.targetValue} (${item.definition.criterionDescription})",
                    style = MaterialTheme.typography.labelSmall,
                    color = SystemColors.MutedSilver
                )
                Text(
                    text = "+${item.definition.xpBonus} XP",
                    style = MaterialTheme.typography.labelSmall,
                    color = SystemColors.MonarchGold
                )
            }
        }
    }
}

// ============================================================================
// SCREEN 13: ACHIEVEMENT DETAILS SCREEN
// ============================================================================
@Composable
fun AchievementDetailsScreen(
    achievementId: String,
    uiState: RpgUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFmt = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    RpgStateHost(uiState = uiState, onRetry = onRetry, modifier = modifier) { ready ->
        val snapshot = ready.snapshot
        val achievement = snapshot.achievements.find { it.definition.id == achievementId }
        if (achievement == null) {
            HolographicPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                accentColor = SystemColors.DangerCrimson
            ) {
                Text("Achievement not found.", color = SystemColors.DangerCrimson)
            }
            return@RpgStateHost
        }

        val def = achievement.definition
        val accent = if (achievement.isUnlocked) rarityAccent(def.rarity) else SystemColors.ElectricCyan
        val linkedTitle = def.rewardTitleId?.let { tid -> snapshot.titles.find { it.id == tid }?.name }
        val linkedItem = def.rewardItemId?.let { iid -> snapshot.inventoryItems.find { it.definition.id == iid }?.definition?.name }
        val linkedShadow = def.rewardShadowId?.let { sid -> snapshot.shadowCollection.find { it.definition.id == sid }?.definition?.codename }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = accent) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RpgTagChip(label = def.rarity.label, accent = accent)
                            RpgTagChip(
                                label = if (achievement.isUnlocked) "VERIFIED UNLOCK" else "IN PROGRESS",
                                accent = if (achievement.isUnlocked) SystemColors.VerifiedEmerald else SystemColors.WarningAmber
                            )
                        }
                        Text(
                            text = def.title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = SystemColors.PureIce
                        )
                        Text(
                            text = def.category.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = accent
                        )
                        Text(
                            text = def.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = SystemColors.MetallicSilver
                        )
                    }
                }
            }
            item {
                HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.ElectricCyan) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "VERIFICATION PROGRESS",
                            style = MaterialTheme.typography.labelLarge,
                            color = SystemColors.ElectricCyan
                        )
                        LinearProgressIndicator(
                            progress = { achievement.progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = accent,
                            trackColor = SystemColors.PanelSurfaceElevated
                        )
                        Text(
                            text = "${achievement.currentValue} / ${def.targetValue} — ${def.criterionDescription}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SystemColors.PureIce
                        )
                        achievement.unlockRecord?.let { rec ->
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Evidence Reference: ${rec.evidenceReference}",
                                style = MaterialTheme.typography.bodySmall,
                                color = SystemColors.VerifiedEmerald
                            )
                            Text(
                                text = "Unlocked: ${dateFmt.format(Date(rec.unlockedAtEpochMs))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = SystemColors.MutedSilver
                            )
                        }
                    }
                }
            }
            item {
                HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.MonarchGold) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "MILESTONE REWARDS",
                            style = MaterialTheme.typography.labelLarge,
                            color = SystemColors.MonarchGold
                        )
                        Text(
                            text = "• Progression Bonus: +${def.xpBonus} XP (subject to Daily RPG XP Cap)",
                            style = MaterialTheme.typography.bodySmall,
                            color = SystemColors.PureIce
                        )
                        linkedTitle?.let {
                            Text("• Unlocks Title: $it", style = MaterialTheme.typography.bodySmall, color = SystemColors.MonarchGold)
                        }
                        linkedItem?.let {
                            Text("• Unlocks Item: $it", style = MaterialTheme.typography.bodySmall, color = SystemColors.ElectricCyan)
                        }
                        linkedShadow?.let {
                            Text("• Unlocks Shadow Collectible: $it", style = MaterialTheme.typography.bodySmall, color = SystemColors.ArcaneViolet)
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// SCREEN 14: TITLE COLLECTION SCREEN
// ============================================================================
@Composable
fun TitleCollectionScreen(
    uiState: RpgUiState,
    onRetry: () -> Unit,
    onClearFeedback: () -> Unit,
    onEquipTitle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    RpgStateHost(uiState = uiState, onRetry = onRetry, modifier = modifier) { ready ->
        val titles = ready.snapshot.titles
        val equipped = titles.find { it.isEquipped }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 780.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                RpgFeedbackBanner(message = ready.feedbackBanner, onDismiss = onClearFeedback)
            }
            item {
                HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.MonarchGold) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "EQUIPPED PLAYER TITLE",
                            style = MaterialTheme.typography.labelMedium,
                            color = SystemColors.MonarchGold
                        )
                        Text(
                            text = equipped?.name ?: ready.snapshot.profile.title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = SystemColors.PureIce
                        )
                        Text(
                            text = equipped?.description ?: "Active title displayed on your Status Window.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SystemColors.MetallicSilver
                        )
                    }
                }
            }
            items(titles, key = { it.id }) { title ->
                val accent = when {
                    title.isEquipped -> SystemColors.MonarchGold
                    title.isUnlocked -> rarityAccent(title.rarity)
                    else -> SystemColors.MutedSilver
                }
                HolographicPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("title_card_${title.id}"),
                    accentColor = accent
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = title.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (title.isUnlocked) SystemColors.PureIce else SystemColors.MutedSilver
                                )
                                RpgTagChip(label = title.rarity.label, accent = accent)
                            }
                            Text(
                                text = title.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = SystemColors.MetallicSilver
                            )
                            Text(
                                text = "Requirement: ${title.unlockRequirement}",
                                style = MaterialTheme.typography.labelSmall,
                                color = accent
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        when {
                            title.isEquipped -> {
                                Surface(
                                    color = SystemColors.MonarchGold.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, SystemColors.MonarchGold)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Rounded.CheckCircle, contentDescription = "Equipped", tint = SystemColors.MonarchGold, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("ACTIVE", style = MaterialTheme.typography.labelMedium, color = SystemColors.MonarchGold)
                                    }
                                }
                            }
                            title.isUnlocked -> {
                                Button(
                                    onClick = { onEquipTitle(title.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = SystemColors.ElectricCyan, contentColor = SystemColors.ObsidianBlack),
                                    modifier = Modifier.testTag("equip_title_${title.id}")
                                ) {
                                    Text("Equip")
                                }
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Rounded.Lock,
                                    contentDescription = "Locked title",
                                    tint = SystemColors.MutedSilver
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
