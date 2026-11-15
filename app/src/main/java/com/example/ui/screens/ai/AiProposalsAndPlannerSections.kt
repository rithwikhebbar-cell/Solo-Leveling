package com.example.ui.screens.ai

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.AiDailyPlanItem
import com.example.domain.model.AiQuestProposal
import com.example.domain.model.PlanMode
import com.example.domain.model.ProposalStatus
import com.example.ui.components.HolographicPanel
import com.example.ui.theme.SystemColors
import com.example.ui.viewmodel.AiSystemUiState

@Composable
fun AiQuestProposalsSection(
    state: AiSystemUiState.Ready,
    onGenerateProposals: () -> Unit,
    onApproveProposal: (Long, String?, String?, Int?) -> Unit,
    onRejectProposal: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var editingProposal by remember { mutableStateOf<AiQuestProposal?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.ArcaneViolet) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "AI QUEST GENERATOR",
                        style = MaterialTheme.typography.titleMedium,
                        color = SystemColors.PureIce
                    )
                    Text(
                        text = "Proposals adapt to your recovery readiness, syllabus gaps, and active backlog. Nothing enters your Quest Board or awards XP without approval.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SystemColors.MutedSilver
                    )
                    Button(
                        onClick = onGenerateProposals,
                        enabled = !state.isGeneratingProposals,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SystemColors.ArcaneViolet,
                            contentColor = SystemColors.PureIce
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_generate_quests_button")
                    ) {
                        if (state.isGeneratingProposals) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = SystemColors.PureIce)
                        } else {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("GENERATE PERSONALIZED QUESTS")
                    }
                }
            }
        }

        if (state.questProposals.isEmpty()) {
            item {
                HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.SystemBlue) {
                    Text(
                        text = "No quest proposals generated yet. Tap the button above to analyze your current gaps.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SystemColors.MetallicSilver
                    )
                }
            }
        }

        items(state.questProposals, key = { it.id }) { proposal ->
            val accent = when (proposal.status) {
                ProposalStatus.APPROVED -> SystemColors.VerifiedEmerald
                ProposalStatus.REJECTED -> SystemColors.MutedSilver
                ProposalStatus.PENDING -> SystemColors.ElectricCyan
            }
            HolographicPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_quest_proposal_card_${proposal.id}"),
                accentColor = accent
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = accent.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(0.75.dp, accent.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "${proposal.category.label} • ${proposal.difficultyLabel}",
                                style = MaterialTheme.typography.labelSmall,
                                color = accent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Text(
                            text = proposal.status.label.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = accent
                        )
                    }

                    Text(
                        text = proposal.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = SystemColors.PureIce
                    )
                    Text(
                        text = proposal.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = SystemColors.MetallicSilver
                    )
                    Text(
                        text = "Objective: ${proposal.objective} (${proposal.suggestedDurationMinutes} min) • Why: ${proposal.reason}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SystemColors.MutedSilver
                    )

                    if (proposal.status == ProposalStatus.PENDING) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onApproveProposal(proposal.id, null, null, null) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SystemColors.VerifiedEmerald,
                                    contentColor = SystemColors.ObsidianBlack
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("ai_approve_quest_${proposal.id}")
                            ) {
                                Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Accept")
                            }
                            OutlinedButton(
                                onClick = { editingProposal = proposal },
                                modifier = Modifier.testTag("ai_edit_quest_${proposal.id}")
                            ) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Edit proposal", modifier = Modifier.size(16.dp))
                            }
                            OutlinedButton(
                                onClick = { onRejectProposal(proposal.id) },
                                modifier = Modifier.testTag("ai_reject_quest_${proposal.id}")
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = "Reject proposal", tint = SystemColors.DangerCrimson, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    editingProposal?.let { target ->
        EditQuestProposalDialog(
            proposal = target,
            onDismiss = { editingProposal = null },
            onConfirmApprove = { title, desc, mins ->
                onApproveProposal(target.id, title, desc, mins)
                editingProposal = null
            }
        )
    }
}

@Composable
private fun EditQuestProposalDialog(
    proposal: AiQuestProposal,
    onDismiss: () -> Unit,
    onConfirmApprove: (String, String, Int) -> Unit
) {
    var title by remember { mutableStateOf(proposal.title) }
    var description by remember { mutableStateOf(proposal.description) }
    var durationText by remember { mutableStateOf(proposal.suggestedDurationMinutes.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SystemColors.DeepNavy,
        title = { Text("Customize Proposed Quest", color = SystemColors.PureIce) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Quest Title") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    maxLines = 3
                )
                OutlinedTextField(
                    value = durationText,
                    onValueChange = { durationText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Target Minutes") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val mins = durationText.toIntOrNull()?.coerceIn(10, 180) ?: proposal.suggestedDurationMinutes
                    onConfirmApprove(title, description, mins)
                }
            ) {
                Text("Save & Accept")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AiDailyPlannerSection(
    state: AiSystemUiState.Ready,
    onSelectPlanMode: (PlanMode) -> Unit,
    onGeneratePlan: () -> Unit,
    onApprovePlanItem: (Long, String?, Int?, Int?) -> Unit,
    onRejectPlanItem: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var editingPlanItem by remember { mutableStateOf<AiDailyPlanItem?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.ElectricCyan) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "INTELLIGENT SCHEDULE PLANNER",
                        style = MaterialTheme.typography.titleMedium,
                        color = SystemColors.ElectricCyan
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(PlanMode.entries) { mode ->
                            val selected = mode == state.selectedPlanMode
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectPlanMode(mode) },
                                label = { Text(mode.label, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SystemColors.ElectricCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = SystemColors.ElectricCyan,
                                    containerColor = SystemColors.ObsidianBlack,
                                    labelColor = SystemColors.MutedSilver
                                )
                            )
                        }
                    }
                    Button(
                        onClick = onGeneratePlan,
                        enabled = !state.isGeneratingPlan,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SystemColors.ElectricCyan,
                            contentColor = SystemColors.ObsidianBlack
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_generate_plan_button")
                    ) {
                        if (state.isGeneratingPlan) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = SystemColors.ObsidianBlack)
                        } else {
                            Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("BUILD NON-OVERLAPPING ${state.selectedPlanMode.label.uppercase()}")
                    }
                }
            }
        }

        items(state.dailyPlanItems, key = { it.id }) { item ->
            val accent = when (item.status) {
                ProposalStatus.APPROVED -> SystemColors.VerifiedEmerald
                ProposalStatus.REJECTED -> SystemColors.MutedSilver
                ProposalStatus.PENDING -> SystemColors.SystemBlue
            }
            HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = accent) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${item.formattedWindow()} (${item.durationMinutes}m)",
                            style = MaterialTheme.typography.labelMedium,
                            color = SystemColors.ElectricCyan
                        )
                        Text(
                            text = "${item.module} • ${item.priority.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = accent
                        )
                    }
                    Text(
                        text = item.taskTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = SystemColors.PureIce
                    )
                    Text(
                        text = item.reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = SystemColors.MetallicSilver
                    )
                    if (item.dependencies.isNotBlank()) {
                        Text(
                            text = "Depends on: ${item.dependencies}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SystemColors.MutedSilver
                        )
                    }
                    if (item.status == ProposalStatus.PENDING) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { onApprovePlanItem(item.id, null, null, null) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SystemColors.VerifiedEmerald,
                                    contentColor = SystemColors.ObsidianBlack
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Approve Block")
                            }
                            OutlinedButton(onClick = { editingPlanItem = item }) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Edit schedule block", modifier = Modifier.size(16.dp))
                            }
                            OutlinedButton(onClick = { onRejectPlanItem(item.id) }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Reject schedule block", tint = SystemColors.DangerCrimson, modifier = Modifier.size(16.dp))
                            }
                        }
                    } else {
                        Text(
                            text = item.status.label.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = accent
                        )
                    }
                }
            }
        }
    }

    editingPlanItem?.let { target ->
        EditPlanItemDialog(
            item = target,
            onDismiss = { editingPlanItem = null },
            onConfirm = { title, dur ->
                onApprovePlanItem(target.id, title, target.startMinuteOfDay, dur)
                editingPlanItem = null
            }
        )
    }
}

@Composable
private fun EditPlanItemDialog(
    item: AiDailyPlanItem,
    onDismiss: () -> Unit,
    onConfirm: (String, Int) -> Unit
) {
    var title by remember { mutableStateOf(item.taskTitle) }
    var duration by remember { mutableStateOf(item.durationMinutes.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SystemColors.DeepNavy,
        title = { Text("Adjust Scheduled Block", color = SystemColors.PureIce) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Task Title") }, singleLine = true)
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Duration (min)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(title, duration.toIntOrNull()?.coerceIn(10, 180) ?: item.durationMinutes) }) {
                Text("Save & Approve")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
