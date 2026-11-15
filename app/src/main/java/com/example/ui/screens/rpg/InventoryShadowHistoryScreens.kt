package com.example.ui.screens.rpg

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import com.example.domain.engine.ProgressionEngine
import com.example.domain.model.InventoryCategory
import com.example.domain.model.ShadowCollectionCategory
import com.example.ui.components.HolographicPanel
import com.example.ui.components.SystemSectionTitle
import com.example.ui.theme.SystemColors
import com.example.ui.viewmodel.RpgUiState
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

// ============================================================================
// SCREEN 15: PLAYER INVENTORY SCREEN (7 Categories)
// ============================================================================
@Composable
fun PlayerInventoryScreen(
    uiState: RpgUiState,
    onRetry: () -> Unit,
    onClearFeedback: () -> Unit,
    onToggleEquip: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<InventoryCategory?>(null) }

    RpgStateHost(uiState = uiState, onRetry = onRetry, modifier = modifier) { ready ->
        val items = ready.snapshot.inventoryItems
        val filtered = remember(items, selectedCategory) {
            if (selectedCategory == null) items
            else items.filter { it.definition.category == selectedCategory }
        }
        val ownedCount = items.count { it.isOwned }
        val equippedCount = items.count { it.isEquipped }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 800.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                RpgFeedbackBanner(message = ready.feedbackBanner, onDismiss = onClearFeedback)
            }
            item {
                SystemSectionTitle(
                    title = "Digital Inventory",
                    subtitle = "$ownedCount/${items.size} unlocked • $equippedCount equipped (cosmetic & HUD presets)"
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("All") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SystemColors.ElectricCyan.copy(alpha = 0.2f),
                                selectedLabelColor = SystemColors.ElectricCyan
                            )
                        )
                    }
                    items(InventoryCategory.entries) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = {
                                selectedCategory = if (selectedCategory == category) null else category
                            },
                            label = { Text(category.label) },
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
                            text = "No inventory items found in ${selectedCategory?.label ?: "this category"}.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SystemColors.MutedSilver
                        )
                    }
                }
            } else {
                items(filtered, key = { it.definition.id }) { item ->
                    val accent = when {
                        item.isEquipped -> SystemColors.MonarchGold
                        item.isOwned -> rarityAccent(item.definition.rarity)
                        else -> SystemColors.MutedSilver
                    }
                    HolographicPanel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("inventory_card_${item.definition.id}"),
                        accentColor = accent
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when {
                                        item.isEquipped -> Icons.Rounded.CheckCircle
                                        item.isOwned -> Icons.Rounded.Inventory2
                                        else -> Icons.Rounded.Lock
                                    },
                                    contentDescription = item.definition.name,
                                    tint = accent
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.definition.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (item.isOwned) SystemColors.PureIce else SystemColors.MutedSilver
                                    )
                                    Text(
                                        text = item.definition.category.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = accent
                                    )
                                }
                                RpgTagChip(
                                    label = if (item.isOwned) item.definition.rarity.label else "LOCKED",
                                    accent = accent
                                )
                            }
                            Text(
                                text = item.definition.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = SystemColors.MetallicSilver
                            )
                            Text(
                                text = "Source: ${item.record?.sourceDescription?.ifBlank { item.definition.acquisitionSource } ?: item.definition.acquisitionSource}",
                                style = MaterialTheme.typography.labelSmall,
                                color = SystemColors.MutedSilver
                            )
                            if (item.isOwned && item.definition.canEquip) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    if (item.isEquipped) {
                                        OutlinedButton(
                                            onClick = { onToggleEquip(item.definition.id) },
                                            modifier = Modifier.testTag("unequip_item_${item.definition.id}")
                                        ) {
                                            Text("Unequip", color = SystemColors.MonarchGold)
                                        }
                                    } else {
                                        Button(
                                            onClick = { onToggleEquip(item.definition.id) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = SystemColors.ElectricCyan,
                                                contentColor = SystemColors.ObsidianBlack
                                            ),
                                            modifier = Modifier.testTag("equip_item_${item.definition.id}")
                                        ) {
                                            Text("Equip")
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
}

// ============================================================================
// SCREEN 16: SHADOW ARMY COLLECTION SCREEN (4 Original Abstract Categories)
// ============================================================================
@Composable
fun ShadowCollectionScreen(
    uiState: RpgUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<ShadowCollectionCategory?>(null) }
    val dateFmt = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    RpgStateHost(uiState = uiState, onRetry = onRetry, modifier = modifier) { ready ->
        val collection = ready.snapshot.shadowCollection
        val filtered = remember(collection, selectedCategory) {
            if (selectedCategory == null) collection
            else collection.filter { it.definition.category == selectedCategory }
        }
        val unlockedCount = collection.count { it.isUnlocked }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 800.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Image(
                    painter = painterResource(id = R.drawable.img_shadow_collection_crest),
                    contentDescription = "Original geometric obsidian shadow vault crest",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }
            item {
                SystemSectionTitle(
                    title = "Shadow Vault Collection",
                    subtitle = "$unlockedCount/${collection.size} original geometric constructs & milestone guardians extracted"
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("All") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SystemColors.ArcaneViolet.copy(alpha = 0.2f),
                                selectedLabelColor = SystemColors.ArcaneViolet
                            )
                        )
                    }
                    items(ShadowCollectionCategory.entries) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = {
                                selectedCategory = if (selectedCategory == category) null else category
                            },
                            label = { Text(category.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SystemColors.ArcaneViolet.copy(alpha = 0.2f),
                                selectedLabelColor = SystemColors.ArcaneViolet
                            )
                        )
                    }
                }
            }
            items(filtered, key = { it.definition.id }) { shadow ->
                val accent = if (shadow.isUnlocked) rarityAccent(shadow.definition.rarity) else SystemColors.MutedSilver
                HolographicPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("shadow_card_${shadow.definition.id}"),
                    accentColor = accent
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (shadow.isUnlocked) Icons.Rounded.AutoAwesome else Icons.Rounded.Lock,
                                contentDescription = shadow.definition.codename,
                                tint = accent
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = shadow.definition.codename,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (shadow.isUnlocked) SystemColors.PureIce else SystemColors.MutedSilver
                                )
                                Text(
                                    text = shadow.definition.category.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accent
                                )
                            }
                            RpgTagChip(
                                label = if (shadow.isUnlocked) "${shadow.definition.rarity.label} T${shadow.record?.rankTier ?: 1}" else "SEALED",
                                accent = accent
                            )
                        }
                        Text(
                            text = "Motif: ${shadow.definition.visualMotif}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SystemColors.ElectricCyan
                        )
                        Text(
                            text = shadow.definition.loreDescription,
                            style = MaterialTheme.typography.bodySmall,
                            color = SystemColors.MetallicSilver
                        )
                        Text(
                            text = "Unlock Criterion: ${shadow.definition.unlockSource}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SystemColors.MutedSilver
                        )
                        shadow.record?.let { rec ->
                            Text(
                                text = "Extracted ${dateFmt.format(Date(rec.unlockedAtEpochMs))} • ${rec.evidenceSource}",
                                style = MaterialTheme.typography.labelSmall,
                                color = SystemColors.VerifiedEmerald
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// SCREEN 17: RPG REWARD HISTORY & ANTI-FARMING AUDIT SCREEN
// ============================================================================
@Composable
fun RpgRewardHistoryScreen(
    uiState: RpgUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFmt = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    RpgStateHost(uiState = uiState, onRetry = onRetry, modifier = modifier) { ready ->
        val history = ready.snapshot.rewardAuditHistory
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        val xpToday = history.filter {
            Instant.ofEpochMilli(it.timestampEpochMs).atZone(zone).toLocalDate() == today
        }.sumOf { it.xpAwarded }
        val capFraction = (xpToday.toFloat() / ProgressionEngine.DAILY_RPG_XP_CAP).coerceIn(0f, 1f)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 800.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SystemSectionTitle(
                    title = "RPG Reward Audit Ledger",
                    subtitle = "Anti-farming cap enforcement & verified reward provenance"
                )
            }
            item {
                HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.ElectricCyan) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "DAILY RPG XP CAP",
                                style = MaterialTheme.typography.labelLarge,
                                color = SystemColors.ElectricCyan
                            )
                            Text(
                                text = "$xpToday / ${ProgressionEngine.DAILY_RPG_XP_CAP} XP",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (xpToday >= ProgressionEngine.DAILY_RPG_XP_CAP) SystemColors.WarningAmber else SystemColors.PureIce
                            )
                        }
                        LinearProgressIndicator(
                            progress = { capFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = if (xpToday >= ProgressionEngine.DAILY_RPG_XP_CAP) SystemColors.WarningAmber else SystemColors.ElectricCyan,
                            trackColor = SystemColors.PanelSurfaceElevated
                        )
                        Text(
                            text = "Every dungeon, boss, and achievement grant records a unique rewardKey to prevent duplicate payouts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SystemColors.MutedSilver
                        )
                    }
                }
            }
            if (history.isEmpty()) {
                item {
                    HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.MutedSilver) {
                        Text(
                            text = "No RPG rewards recorded yet. Clear a Dungeon Gate, defeat a Boss, or unlock an Achievement with real evidence.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SystemColors.MutedSilver
                        )
                    }
                }
            } else {
                items(history, key = { it.id }) { audit ->
                    val accent = if (audit.wasCapped) SystemColors.WarningAmber else SystemColors.VerifiedEmerald
                    HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = accent) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = audit.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = SystemColors.PureIce,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "+${audit.xpAwarded} XP",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = SystemColors.MonarchGold
                                )
                            }
                            Text(
                                text = "${audit.sourceType} • ${audit.verificationProvenance} • ${dateFmt.format(Date(audit.timestampEpochMs))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = accent
                            )
                            val attrLine = buildString {
                                append("+${audit.primaryAttrXp} ${audit.primaryAttribute.code}")
                                if (audit.secondaryAttribute != null && audit.secondaryAttrXp > 0) {
                                    append("  •  +${audit.secondaryAttrXp} ${audit.secondaryAttribute.code}")
                                }
                            }
                            Text(
                                text = attrLine,
                                style = MaterialTheme.typography.bodySmall,
                                color = SystemColors.ElectricCyan
                            )
                            if (audit.wasCapped || audit.notes.isNotBlank()) {
                                Text(
                                    text = audit.notes.ifBlank { "Daily RPG XP cap applied." },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SystemColors.WarningAmber
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
