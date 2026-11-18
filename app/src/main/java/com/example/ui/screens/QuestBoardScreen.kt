package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Quest
import com.example.domain.model.QuestCategory
import com.example.domain.model.QuestType
import com.example.ui.components.GlowingProgressBar
import com.example.ui.components.HolographicPanel
import com.example.ui.components.PrimaryActionButton
import com.example.ui.components.ProvenanceChip
import com.example.ui.components.StatusStatPill
import com.example.ui.components.SystemEmptyState
import com.example.ui.components.SystemSectionTitle
import com.example.ui.theme.SystemColors
import com.example.ui.viewmodel.RealityOsUiState

private enum class QuestStatusFilter(val label: String) {
    ALL("All"),
    ACTIVE("Active"),
    COMPLETED("Completed")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuestBoardScreen(
    state: RealityOsUiState.Ready,
    onCompleteQuest: (Long) -> Unit,
    onCreateQuest: (String, String, QuestType, QuestCategory, Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedStatus by rememberSaveable { mutableStateOf(QuestStatusFilter.ALL) }
    var selectedType by rememberSaveable { mutableStateOf<QuestType?>(null) }
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }

    val activeCount = remember(state.quests) { state.quests.count { !it.isCompleted } }
    val completedCount = remember(state.quests) { state.quests.count { it.isCompleted } }
    val totalXpAvailable = remember(state.quests) {
        state.quests.filter { !it.isCompleted }.sumOf { it.xpReward }
    }

    val filteredQuests = remember(state.quests, selectedStatus, selectedType) {
        state.quests.filter { quest ->
            val matchesStatus = when (selectedStatus) {
                QuestStatusFilter.ALL -> true
                QuestStatusFilter.ACTIVE -> !quest.isCompleted
                QuestStatusFilter.COMPLETED -> quest.isCompleted
            }
            val matchesType = selectedType == null || quest.type == selectedType
            matchesStatus && matchesType
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
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                SystemSectionTitle(
                    title = "Quest Board",
                    subtitle = "$activeCount active • $completedCount completed",
                    trailing = {
                        Button(
                            onClick = { showCreateDialog = true },
                            modifier = Modifier
                                .heightIn(min = 44.dp)
                                .testTag("new_quest_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SystemColors.ElectricCyan,
                                contentColor = SystemColors.ObsidianBlack
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("NEW QUEST", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                )
            }

            // Overview Metric Summary
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatusStatPill(
                        label = "ACTIVE",
                        value = "$activeCount",
                        modifier = Modifier.weight(1f),
                        accentColor = SystemColors.ElectricCyan
                    )
                    StatusStatPill(
                        label = "CLEARED",
                        value = "$completedCount",
                        modifier = Modifier.weight(1f),
                        accentColor = SystemColors.VerifiedEmerald
                    )
                    StatusStatPill(
                        label = "XP POOL",
                        value = "+$totalXpAvailable",
                        modifier = Modifier.weight(1f),
                        accentColor = SystemColors.MonarchGold
                    )
                }
            }

            // Status Filter Tabs & Type Chips
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Status row (All / Active / Completed)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuestStatusFilter.entries.forEach { status ->
                            val isSelected = selectedStatus == status
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clickable { selectedStatus = status },
                                color = if (isSelected) SystemColors.ElectricCyan.copy(alpha = 0.2f) else SystemColors.SecondarySurface,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) SystemColors.ElectricCyan else SystemColors.SubtleBorder
                                )
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = status.label.uppercase(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isSelected) SystemColors.ElectricCyan else SystemColors.MutedSilver,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    // Type filter chips
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedType == null,
                            onClick = { selectedType = null },
                            label = { Text("All Types") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SystemColors.ElectricCyan.copy(alpha = 0.2f),
                                selectedLabelColor = SystemColors.ElectricCyan,
                                containerColor = SystemColors.SecondarySurface,
                                labelColor = SystemColors.MutedSilver
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (selectedType == null) SystemColors.ElectricCyan else SystemColors.SubtleBorder
                            )
                        )
                        QuestType.entries.forEach { type ->
                            val count = state.quests.count { it.type == type }
                            val isSelected = selectedType == type
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedType = if (isSelected) null else type },
                                label = { Text("${type.label} ($count)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SystemColors.ElectricCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = SystemColors.ElectricCyan,
                                    containerColor = SystemColors.SecondarySurface,
                                    labelColor = SystemColors.MutedSilver
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) SystemColors.ElectricCyan else SystemColors.SubtleBorder
                                )
                            )
                        }
                    }
                }
            }

            // Quests List
            if (filteredQuests.isEmpty()) {
                item {
                    SystemEmptyState(
                        title = "No Quests Found",
                        description = "Create a custom quest or adjust filters to view system objectives."
                    )
                }
            } else {
                items(filteredQuests, key = { it.id }) { quest ->
                    QuestBoardCard(
                        quest = quest,
                        onComplete = { onCompleteQuest(quest.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showCreateDialog) {
        CreateQuestDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { title, desc, type, category, target, unit ->
                onCreateQuest(title, desc, type, category, target, unit)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun QuestBoardCard(
    quest: Quest,
    onComplete: () -> Unit
) {
    val accent = if (quest.isCompleted) SystemColors.VerifiedEmerald else SystemColors.ElectricCyan
    val categoryColor = when (quest.category) {
        QuestCategory.PHYSICAL -> SystemColors.VerifiedEmerald
        QuestCategory.ACADEMIC -> SystemColors.ArcaneViolet
        QuestCategory.DISCIPLINE -> SystemColors.SystemBlue
        QuestCategory.CAREER -> SystemColors.MonarchGold
        QuestCategory.CREATIVE -> Color(0xFFEC4899)
        QuestCategory.SOCIAL -> SystemColors.ElectricCyan
        QuestCategory.RECOVERY -> SystemColors.WarningAmber
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (quest.isCompleted) 0.72f else 1f),
        color = SystemColors.SecondarySurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (quest.isCompleted) SystemColors.VerifiedEmerald.copy(alpha = 0.35f)
            else SystemColors.SubtleBorder
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Type, Category Pill, Provenance Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = categoryColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, categoryColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = quest.category.label.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = categoryColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = "• ${quest.type.label.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SystemColors.MutedSilver
                    )
                }
                ProvenanceChip(status = quest.verificationStatus)
            }

            // Title & Description
            Text(
                text = quest.title,
                style = MaterialTheme.typography.titleLarge,
                color = SystemColors.PureIce
            )

            if (quest.description.isNotBlank()) {
                Text(
                    text = quest.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SystemColors.MutedSilver
                )
            }

            // Progress Bar
            GlowingProgressBar(
                progress = if (quest.isCompleted) 1f else quest.progressFraction,
                height = 6.dp,
                startColor = accent,
                endColor = SystemColors.ElectricCyan
            )

            // Bottom Row: Rewards & Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = SystemColors.ElectricCyan.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "+${quest.xpReward} XP",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = SystemColors.ElectricCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = "${quest.primaryAttribute.code} +${quest.primaryAttrXpReward}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SystemColors.attributeAccent(quest.primaryAttribute)
                    )
                }

                if (quest.isCompleted) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Completed",
                            tint = SystemColors.VerifiedEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "CLEARED",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = SystemColors.VerifiedEmerald
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = onComplete,
                        modifier = Modifier
                            .heightIn(min = 44.dp)
                            .testTag("quest_board_complete_${quest.id}"),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, SystemColors.ElectricCyan),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = SystemColors.ElectricCyan
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text("COMPLETE", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateQuestDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, QuestType, QuestCategory, Int, String) -> Unit
) {
    var title by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var selectedType by rememberSaveable { mutableStateOf(QuestType.DAILY) }
    var selectedCategory by rememberSaveable { mutableStateOf(QuestCategory.DISCIPLINE) }
    var targetText by rememberSaveable { mutableStateOf("30") }
    var unit by rememberSaveable { mutableStateOf("min") }

    val targetValue = targetText.toIntOrNull() ?: 0
    val isValid = title.isNotBlank() && targetValue > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SystemColors.DeepNavy,
        titleContentColor = SystemColors.PureIce,
        textContentColor = SystemColors.MetallicSilver,
        shape = RoundedCornerShape(14.dp),
        title = {
            Text(
                text = "CREATE SYSTEM QUEST",
                style = MaterialTheme.typography.titleLarge,
                color = SystemColors.ElectricCyan
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Quest Title (e.g. Read 20 Pages)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SystemColors.ElectricCyan,
                        unfocusedBorderColor = SystemColors.PanelBorderCyan,
                        focusedTextColor = SystemColors.PureIce,
                        unfocusedTextColor = SystemColors.PureIce
                    )
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Objective Description (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SystemColors.ElectricCyan,
                        unfocusedBorderColor = SystemColors.PanelBorderCyan,
                        focusedTextColor = SystemColors.PureIce,
                        unfocusedTextColor = SystemColors.PureIce
                    )
                )

                Text("Quest Schedule:", style = MaterialTheme.typography.labelSmall, color = SystemColors.MutedSilver)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    QuestType.entries.forEach { t ->
                        FilterChip(
                            selected = selectedType == t,
                            onClick = { selectedType = t },
                            label = { Text(t.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SystemColors.ElectricCyan.copy(alpha = 0.2f),
                                selectedLabelColor = SystemColors.ElectricCyan
                            )
                        )
                    }
                }

                Text("Attribute Category:", style = MaterialTheme.typography.labelSmall, color = SystemColors.MutedSilver)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    QuestCategory.entries.forEach { c ->
                        FilterChip(
                            selected = selectedCategory == c,
                            onClick = { selectedCategory = c },
                            label = { Text(c.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SystemColors.ElectricCyan.copy(alpha = 0.2f),
                                selectedLabelColor = SystemColors.ElectricCyan
                            )
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = targetText,
                        onValueChange = { targetText = it.filter { ch -> ch.isDigit() }.take(4) },
                        label = { Text("Target Value") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SystemColors.ElectricCyan,
                            unfocusedBorderColor = SystemColors.PanelBorderCyan,
                            focusedTextColor = SystemColors.PureIce,
                            unfocusedTextColor = SystemColors.PureIce
                        )
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it.take(10) },
                        label = { Text("Unit") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SystemColors.ElectricCyan,
                            unfocusedBorderColor = SystemColors.PanelBorderCyan,
                            focusedTextColor = SystemColors.PureIce,
                            unfocusedTextColor = SystemColors.PureIce
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, description, selectedType, selectedCategory, targetValue, unit) },
                enabled = isValid,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SystemColors.ElectricCyan,
                    contentColor = SystemColors.ObsidianBlack
                )
            ) {
                Text("ADD QUEST", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", style = MaterialTheme.typography.labelMedium)
            }
        }
    )
}
