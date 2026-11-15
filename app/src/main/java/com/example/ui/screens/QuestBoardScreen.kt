package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.model.Quest
import com.example.domain.model.QuestCategory
import com.example.domain.model.QuestType
import com.example.ui.components.GlowingProgressBar
import com.example.ui.components.HolographicPanel
import com.example.ui.components.ProvenanceChip
import com.example.ui.components.SystemEmptyState
import com.example.ui.components.SystemSectionTitle
import com.example.ui.theme.SystemColors
import com.example.ui.viewmodel.RealityOsUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuestBoardScreen(
    state: RealityOsUiState.Ready,
    onCompleteQuest: (Long) -> Unit,
    onCreateQuest: (String, String, QuestType, QuestCategory, Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedType by rememberSaveable { mutableStateOf<QuestType?>(null) }
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }

    val filteredQuests = remember(state.quests, selectedType) {
        if (selectedType == null) state.quests
        else state.quests.filter { it.type == selectedType }
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
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SystemSectionTitle(
                    title = "Quest Board",
                    subtitle = "${state.quests.count { !it.isCompleted }} active • ${state.quests.count { it.isCompleted }} completed",
                    trailing = {
                        Button(
                            onClick = { showCreateDialog = true },
                            modifier = Modifier.testTag("new_quest_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SystemColors.ElectricCyan,
                                contentColor = SystemColors.ObsidianBlack
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("NEW QUEST", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                )
            }

            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedType == null,
                        onClick = { selectedType = null },
                        label = { Text("All (${state.quests.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SystemColors.ElectricCyan.copy(alpha = 0.2f),
                            selectedLabelColor = SystemColors.ElectricCyan
                        )
                    )
                    QuestType.entries.forEach { type ->
                        val count = state.quests.count { it.type == type }
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text("${type.label} ($count)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SystemColors.ElectricCyan.copy(alpha = 0.2f),
                                selectedLabelColor = SystemColors.ElectricCyan
                            )
                        )
                    }
                }
            }

            if (filteredQuests.isEmpty()) {
                item {
                    SystemEmptyState(
                        title = "No Quests In This Category",
                        description = "Create a custom quest to start earning deterministic XP and attribute progression."
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
    HolographicPanel(
        modifier = Modifier.fillMaxWidth(),
        accentColor = accent
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${quest.type.label.uppercase()} • ${quest.category.label.uppercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent
                )
                ProvenanceChip(status = quest.verificationStatus)
            }

            Text(
                text = quest.title,
                style = MaterialTheme.typography.titleLarge,
                color = SystemColors.PureIce
            )
            Text(
                text = quest.description,
                style = MaterialTheme.typography.bodyMedium,
                color = SystemColors.MetallicSilver
            )

            GlowingProgressBar(
                progress = if (quest.isCompleted) 1f else quest.progressFraction,
                height = 6.dp,
                startColor = accent
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Reward: +${quest.xpReward} XP • ${quest.primaryAttribute.code} +${quest.primaryAttrXpReward}",
                    style = MaterialTheme.typography.labelMedium,
                    color = SystemColors.ElectricCyan
                )
                if (quest.isCompleted) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Completed",
                            tint = SystemColors.VerifiedEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "CLEARED",
                            style = MaterialTheme.typography.labelMedium,
                            color = SystemColors.VerifiedEmerald
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = onComplete,
                        modifier = Modifier.testTag("quest_board_complete_${quest.id}"),
                        border = BorderStroke(1.dp, SystemColors.ElectricCyan)
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
        title = { Text("CREATE QUEST", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Quest Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Objective Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    QuestType.entries.forEach { t ->
                        FilterChip(
                            selected = selectedType == t,
                            onClick = { selectedType = t },
                            label = { Text(t.label) }
                        )
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    QuestCategory.entries.forEach { c ->
                        FilterChip(
                            selected = selectedCategory == c,
                            onClick = { selectedCategory = c },
                            label = { Text(c.label) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = targetText,
                        onValueChange = { targetText = it.filter { ch -> ch.isDigit() }.take(4) },
                        label = { Text("Target") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it.take(10) },
                        label = { Text("Unit") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, description, selectedType, selectedCategory, targetValue, unit) },
                enabled = isValid
            ) {
                Text("ADD QUEST")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL")
            }
        }
    )
}
