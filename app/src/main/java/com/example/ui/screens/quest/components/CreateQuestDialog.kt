package com.example.ui.screens.quest.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.model.QuestCategory
import com.example.domain.model.QuestType
import com.example.ui.theme.SystemColors
import com.example.ui.theme.SystemSpacing

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateQuestDialog(
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
        shape = RoundedCornerShape(SystemSpacing.cornerLarge),
        modifier = Modifier.testTag("create_quest_dialog"),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "SYSTEM // CREATE DIRECTIVE",
                    style = MaterialTheme.typography.labelSmall,
                    color = SystemColors.ElectricCyan
                )
                Text(
                    text = "NEW QUEST OBJECTIVE",
                    style = MaterialTheme.typography.titleLarge,
                    color = SystemColors.PureIce
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Quest Title (e.g. Read 20 Pages)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quest_title_input"),
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
                    label = { Text("Objective Briefing (optional)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quest_desc_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SystemColors.ElectricCyan,
                        unfocusedBorderColor = SystemColors.PanelBorderCyan,
                        focusedTextColor = SystemColors.PureIce,
                        unfocusedTextColor = SystemColors.PureIce
                    )
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "DIRECTIVE TIMEFRAME:",
                        style = MaterialTheme.typography.labelSmall,
                        color = SystemColors.MutedSilver
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        QuestType.entries.forEach { t ->
                            FilterChip(
                                selected = selectedType == t,
                                onClick = { selectedType = t },
                                label = { Text(t.label.uppercase()) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SystemColors.ElectricCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = SystemColors.ElectricCyan,
                                    containerColor = SystemColors.SecondarySurface,
                                    labelColor = SystemColors.MutedSilver
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (selectedType == t) SystemColors.ElectricCyan else SystemColors.SubtleBorder
                                )
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "ATTRIBUTE MATRIX CATEGORY:",
                        style = MaterialTheme.typography.labelSmall,
                        color = SystemColors.MutedSilver
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        QuestCategory.entries.forEach { c ->
                            FilterChip(
                                selected = selectedCategory == c,
                                onClick = { selectedCategory = c },
                                label = { Text(c.label.uppercase()) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SystemColors.ElectricCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = SystemColors.ElectricCyan,
                                    containerColor = SystemColors.SecondarySurface,
                                    labelColor = SystemColors.MutedSilver
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (selectedCategory == c) SystemColors.ElectricCyan else SystemColors.SubtleBorder
                                )
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = targetText,
                        onValueChange = { targetText = it.filter { ch -> ch.isDigit() }.take(4) },
                        label = { Text("Target") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quest_target_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SystemColors.ElectricCyan,
                            unfocusedBorderColor = SystemColors.PanelBorderCyan,
                            focusedTextColor = SystemColors.PureIce,
                            unfocusedTextColor = SystemColors.PureIce
                        )
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it.take(12) },
                        label = { Text("Unit") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quest_unit_input"),
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
                onClick = {
                    onConfirm(title.trim(), description.trim(), selectedType, selectedCategory, targetValue, unit.trim())
                },
                enabled = isValid,
                modifier = Modifier
                    .heightIn(min = 44.dp)
                    .testTag("create_quest_confirm_button"),
                shape = RoundedCornerShape(SystemSpacing.cornerSmall),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SystemColors.ElectricCyan,
                    contentColor = SystemColors.ObsidianBlack,
                    disabledContainerColor = SystemColors.ElectricCyan.copy(alpha = 0.4f),
                    disabledContentColor = SystemColors.ObsidianBlack.copy(alpha = 0.6f)
                )
            ) {
                Text(
                    text = "INITIALIZE DIRECTIVE",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 44.dp)
            ) {
                Text(
                    text = "CANCEL",
                    style = MaterialTheme.typography.labelMedium,
                    color = SystemColors.MutedSilver
                )
            }
        }
    )
}
