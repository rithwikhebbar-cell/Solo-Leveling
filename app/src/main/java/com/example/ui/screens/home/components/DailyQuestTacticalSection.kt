package com.example.ui.screens.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.CommitmentState
import com.example.domain.model.ExemptionReasonCategory
import com.example.domain.model.Quest
import com.example.domain.model.QuestCommitment
import com.example.ui.components.GlowingProgressBar
import com.example.ui.components.HolographicPanel
import com.example.ui.components.ProvenanceChip
import com.example.ui.components.SystemEmptyState
import com.example.ui.components.SystemSectionTitle
import com.example.ui.theme.SystemColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun DailyQuestTacticalSection(
    dailyQuests: List<Quest>,
    completedCount: Int,
    pendingCount: Int,
    progressFraction: Float,
    onCompleteQuest: (Long) -> Unit,
    onOpenQuestBoard: () -> Unit,
    onOpenPenaltyStatus: (() -> Unit)? = null,
    commitments: Map<Long, QuestCommitment> = emptyMap(),
    onAcceptCommitment: ((Long) -> Unit)? = null,
    onRequestReschedule: ((String, Long, String) -> Unit)? = null,
    onRequestExemption: ((String, ExemptionReasonCategory, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var pendingQuestIds by remember { mutableStateOf(setOf<Long>()) }
    var acceptingQuest by remember { mutableStateOf<Quest?>(null) }
    var reschedulingCommitment by remember { mutableStateOf<QuestCommitment?>(null) }
    var exemptingCommitment by remember { mutableStateOf<QuestCommitment?>(null) }

    // Clean up completed quests from pending set
    val activePendingIds = remember(dailyQuests, pendingQuestIds) {
        val activeIds = dailyQuests.filter { !it.isCompleted }.map { it.id }.toSet()
        pendingQuestIds.intersect(activeIds)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SystemSectionTitle(
            title = "Daily Quests",
            subtitle = "$completedCount completed • $pendingCount pending",
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onOpenPenaltyStatus != null) {
                        TextButton(
                            onClick = onOpenPenaltyStatus,
                            modifier = Modifier.testTag("open_penalty_status_button")
                        ) {
                            Text(
                                text = "PENALTIES",
                                style = MaterialTheme.typography.labelSmall,
                                color = SystemColors.ArcaneViolet
                            )
                        }
                    }
                    TextButton(
                        onClick = onOpenQuestBoard,
                        modifier = Modifier.testTag("open_quest_board_button")
                    ) {
                        Text(
                            text = "QUEST BOARD",
                            style = MaterialTheme.typography.labelSmall,
                            color = SystemColors.ElectricCyan
                        )
                    }
                }
            }
        )

        HolographicPanel(
            modifier = Modifier.fillMaxWidth(),
            accentColor = SystemColors.ElectricCyan,
            secondaryColor = SystemColors.ArcaneViolet,
            contentPadding = 14.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Header progress summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DAILY COMPLETION PROTOCOL",
                        style = MaterialTheme.typography.labelMedium,
                        color = SystemColors.MetallicSilver
                    )
                    Text(
                        text = "${(progressFraction * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = SystemColors.ElectricCyan
                    )
                }

                GlowingProgressBar(
                    progress = progressFraction,
                    height = 7.dp,
                    startColor = SystemColors.ElectricCyan,
                    endColor = SystemColors.VerifiedEmerald
                )

                if (dailyQuests.isEmpty()) {
                    SystemEmptyState(
                        title = "No Daily Objectives",
                        description = "All daily quests cleared or none currently assigned. Check Quest Board for active campaigns."
                    )
                } else {
                    dailyQuests.forEach { quest ->
                        val commitment = commitments[quest.id]
                        TacticalQuestCard(
                            quest = quest,
                            commitment = commitment,
                            isProcessing = quest.id in activePendingIds,
                            onComplete = {
                                if (quest.id !in activePendingIds && !quest.isCompleted) {
                                    pendingQuestIds = pendingQuestIds + quest.id
                                    onCompleteQuest(quest.id)
                                }
                            },
                            onAcceptClick = { acceptingQuest = quest },
                            onRescheduleClick = { if (commitment != null) reschedulingCommitment = commitment },
                            onExemptionClick = { if (commitment != null) exemptingCommitment = commitment }
                        )
                    }
                }
            }
        }
    }

    // 1. Explicit Acceptance Confirmation Dialog
    acceptingQuest?.let { q ->
        AcceptCommitmentConfirmationDialog(
            quest = q,
            onConfirm = {
                onAcceptCommitment?.invoke(q.id)
                acceptingQuest = null
            },
            onDismiss = { acceptingQuest = null }
        )
    }

    // 2. Reschedule Request Dialog
    reschedulingCommitment?.let { c ->
        RescheduleRequestDialog(
            commitment = c,
            onConfirm = { revisedDeadline, reason ->
                onRequestReschedule?.invoke(c.commitmentId, revisedDeadline, reason)
                reschedulingCommitment = null
            },
            onDismiss = { reschedulingCommitment = null }
        )
    }

    // 3. Exemption Request Dialog
    exemptingCommitment?.let { c ->
        ExemptionRequestDialog(
            commitment = c,
            onConfirm = { category, explanation ->
                onRequestExemption?.invoke(c.commitmentId, category, explanation)
                exemptingCommitment = null
            },
            onDismiss = { exemptingCommitment = null }
        )
    }
}

@Composable
private fun TacticalQuestCard(
    quest: Quest,
    commitment: QuestCommitment?,
    isProcessing: Boolean,
    onComplete: () -> Unit,
    onAcceptClick: () -> Unit,
    onRescheduleClick: () -> Unit,
    onExemptionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAccepted = commitment != null && commitment.state != CommitmentState.NOT_ACCEPTED
    val state = commitment?.state ?: CommitmentState.NOT_ACCEPTED

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = SystemColors.ObsidianBlack.copy(alpha = 0.60f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(
            width = 0.75.dp,
            color = when {
                quest.isCompleted -> SystemColors.VerifiedEmerald.copy(alpha = 0.45f)
                state == CommitmentState.FAILED -> SystemColors.DangerCrimson.copy(alpha = 0.60f)
                state == CommitmentState.OVERDUE -> SystemColors.WarningAmber.copy(alpha = 0.60f)
                isAccepted -> SystemColors.ElectricCyan.copy(alpha = 0.45f)
                else -> SystemColors.SubtleBorder
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Icon
                Icon(
                    imageVector = when {
                        quest.isCompleted -> Icons.Rounded.CheckCircle
                        state == CommitmentState.FAILED -> Icons.Rounded.Warning
                        else -> Icons.Rounded.RadioButtonUnchecked
                    },
                    contentDescription = if (quest.isCompleted) "Completed" else "Pending",
                    tint = when {
                        quest.isCompleted -> SystemColors.VerifiedEmerald
                        state == CommitmentState.FAILED -> SystemColors.DangerCrimson
                        state == CommitmentState.OVERDUE -> SystemColors.WarningAmber
                        isAccepted -> SystemColors.ElectricCyan
                        else -> SystemColors.MutedSilver
                    },
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Quest details
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = quest.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (quest.isCompleted) SystemColors.MutedSilver else SystemColors.PureIce,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        CommitmentStateBadge(state = state)
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${quest.targetValue} ${quest.unit} • +${quest.xpReward} XP • ${quest.primaryAttribute.code}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SystemColors.MutedSilver
                        )
                        ProvenanceChip(status = quest.verificationStatus)
                    }

                    if (commitment != null && isAccepted && !quest.isCompleted) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatCommitmentDeadlineSummary(commitment),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (state == CommitmentState.OVERDUE) SystemColors.WarningAmber else SystemColors.MetallicSilver
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Primary Action Button
                if (!quest.isCompleted && state != CommitmentState.FAILED) {
                    OutlinedButton(
                        onClick = onComplete,
                        enabled = !isProcessing,
                        modifier = Modifier.testTag("complete_quest_${quest.id}"),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isProcessing) SystemColors.SubtleBorder else SystemColors.ElectricCyan
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = SystemColors.ElectricCyan,
                            disabledContentColor = SystemColors.MutedSilver
                        )
                    ) {
                        Text(
                            text = if (isProcessing) "CLEARING..." else "COMPLETE",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                } else if (quest.isCompleted) {
                    Text(
                        text = "CLEARED",
                        style = MaterialTheme.typography.labelSmall,
                        color = SystemColors.VerifiedEmerald
                    )
                } else {
                    Text(
                        text = "FAILED",
                        style = MaterialTheme.typography.labelSmall,
                        color = SystemColors.DangerCrimson
                    )
                }
            }

            // Secondary Commitment Action Bar
            if (!quest.isCompleted) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isAccepted) {
                        TextButton(
                            onClick = onAcceptClick,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Shield,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = SystemColors.ElectricCyan
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ACCEPT COMMITMENT",
                                style = MaterialTheme.typography.labelSmall,
                                color = SystemColors.ElectricCyan
                            )
                        }
                    } else if (state !in setOf(CommitmentState.FAILED, CommitmentState.COMPLETED)) {
                        TextButton(
                            onClick = onRescheduleClick,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = SystemColors.MetallicSilver
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "RESCHEDULE (${commitment?.rescheduleCount ?: 0}/2)",
                                style = MaterialTheme.typography.labelSmall,
                                color = SystemColors.MetallicSilver
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        TextButton(
                            onClick = onExemptionClick,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "EXEMPTION",
                                style = MaterialTheme.typography.labelSmall,
                                color = SystemColors.ArcaneViolet
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommitmentStateBadge(state: CommitmentState) {
    val (label, bg, text) = when (state) {
        CommitmentState.NOT_ACCEPTED -> Triple("OPTIONAL", SystemColors.SecondarySurface, SystemColors.MutedSilver)
        CommitmentState.ACCEPTED -> Triple("COMMITTED", SystemColors.ElectricCyan.copy(alpha = 0.2f), SystemColors.ElectricCyan)
        CommitmentState.ACTIVE -> Triple("ACTIVE", SystemColors.ElectricCyan.copy(alpha = 0.2f), SystemColors.ElectricCyan)
        CommitmentState.COMPLETED -> Triple("CLEARED", SystemColors.VerifiedEmerald.copy(alpha = 0.2f), SystemColors.VerifiedEmerald)
        CommitmentState.OVERDUE -> Triple("GRACE ACTIVE", SystemColors.WarningAmber.copy(alpha = 0.2f), SystemColors.WarningAmber)
        CommitmentState.FAILED -> Triple("FAILED", SystemColors.DangerCrimson.copy(alpha = 0.2f), SystemColors.DangerCrimson)
        CommitmentState.EXEMPTED -> Triple("EXEMPTED", SystemColors.VerifiedEmerald.copy(alpha = 0.2f), SystemColors.VerifiedEmerald)
        CommitmentState.PENDING_REVIEW -> Triple("PENDING REVIEW", SystemColors.ArcaneViolet.copy(alpha = 0.2f), SystemColors.ArcaneViolet)
        CommitmentState.RESCHEDULED -> Triple("RESCHEDULED", SystemColors.SystemBlue.copy(alpha = 0.2f), SystemColors.SystemBlue)
        CommitmentState.CANCELLED -> Triple("CANCELLED", SystemColors.SecondarySurface, SystemColors.MutedSilver)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = text
        )
    }
}

private fun formatCommitmentDeadlineSummary(commitment: QuestCommitment): String {
    val formatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
    val deadlineStr = formatter.format(Instant.ofEpochMilli(commitment.deadlineEpochMs))
    val graceStr = formatter.format(Instant.ofEpochMilli(commitment.gracePeriodExpiresAtEpochMs))
    return "Deadline $deadlineStr • Grace until $graceStr (${commitment.gracePeriodMinutes}m)"
}

@Composable
private fun AcceptCommitmentConfirmationDialog(
    quest: Quest,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SystemColors.DeepNavy,
        shape = RoundedCornerShape(12.dp),
        title = {
            Text(
                text = "CONFIRM QUEST COMMITMENT",
                style = MaterialTheme.typography.titleMedium,
                color = SystemColors.ElectricCyan
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "You are committing to: \"${quest.title}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SystemColors.PureIce
                )
                Text(
                    text = "• Deadline: Closes at the daily cycle reset.\n• Grace Period: 30 minutes following deadline.\n• Accountability: Once accepted, uncompleted commitments without legitimate exemption record an immutable failure record.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SystemColors.MetallicSilver
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SystemColors.ElectricCyan,
                    contentColor = SystemColors.ObsidianBlack
                )
            ) {
                Text("CONFIRM COMMITMENT", style = MaterialTheme.typography.labelSmall)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", style = MaterialTheme.typography.labelSmall, color = SystemColors.MutedSilver)
            }
        }
    )
}

@Composable
private fun RescheduleRequestDialog(
    commitment: QuestCommitment,
    onConfirm: (Long, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedExtensionHours by remember { mutableStateOf(2) }
    var reasonText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SystemColors.DeepNavy,
        shape = RoundedCornerShape(12.dp),
        title = {
            Text(
                text = "REQUEST RESCHEDULE",
                style = MaterialTheme.typography.titleMedium,
                color = SystemColors.ElectricCyan
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Select revised deadline extension (Remaining reschedules: ${QuestCommitment.MAX_ALLOWED_RESCHEDULES - commitment.rescheduleCount}):",
                    style = MaterialTheme.typography.bodySmall,
                    color = SystemColors.MetallicSilver
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1 to "+1 Hour", 2 to "+2 Hours", 4 to "+4 Hours").forEach { (hrs, label) ->
                        OutlinedButton(
                            onClick = { selectedExtensionHours = hrs },
                            border = BorderStroke(
                                1.dp,
                                if (selectedExtensionHours == hrs) SystemColors.ElectricCyan else SystemColors.SubtleBorder
                            ),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selectedExtensionHours == hrs) SystemColors.ElectricCyan else SystemColors.MutedSilver
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = reasonText,
                    onValueChange = { reasonText = it },
                    label = { Text("Reason (optional)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SystemColors.ElectricCyan,
                        unfocusedBorderColor = SystemColors.SubtleBorder,
                        focusedTextColor = SystemColors.PureIce,
                        unfocusedTextColor = SystemColors.PureIce
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val revised = commitment.deadlineEpochMs + (selectedExtensionHours * 3600_000L)
                    onConfirm(revised, reasonText.ifBlank { "PLAYER_EXTENSION_${selectedExtensionHours}H" })
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = SystemColors.ElectricCyan,
                    contentColor = SystemColors.ObsidianBlack
                )
            ) {
                Text("RESCHEDULE", style = MaterialTheme.typography.labelSmall)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", style = MaterialTheme.typography.labelSmall, color = SystemColors.MutedSilver)
            }
        }
    )
}

@Composable
private fun ExemptionRequestDialog(
    commitment: QuestCommitment,
    onConfirm: (ExemptionReasonCategory, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf(ExemptionReasonCategory.ILLNESS) }
    var explanationText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SystemColors.DeepNavy,
        shape = RoundedCornerShape(12.dp),
        title = {
            Text(
                text = "REQUEST EXEMPTION",
                style = MaterialTheme.typography.titleMedium,
                color = SystemColors.ArcaneViolet
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Logging an exemption request immediately pauses failure evaluation pending review.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SystemColors.MetallicSilver
                )
                ExemptionReasonCategory.entries.forEach { cat ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedCategory = cat },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            colors = RadioButtonDefaults.colors(selectedColor = SystemColors.ArcaneViolet)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = cat.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = SystemColors.PureIce
                        )
                    }
                }
                OutlinedTextField(
                    value = explanationText,
                    onValueChange = { explanationText = it },
                    label = { Text("Brief Explanation") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SystemColors.ArcaneViolet,
                        unfocusedBorderColor = SystemColors.SubtleBorder,
                        focusedTextColor = SystemColors.PureIce,
                        unfocusedTextColor = SystemColors.PureIce
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedCategory, explanationText) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = SystemColors.ArcaneViolet,
                    contentColor = SystemColors.PureIce
                )
            ) {
                Text("SUBMIT EXEMPTION", style = MaterialTheme.typography.labelSmall)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", style = MaterialTheme.typography.labelSmall, color = SystemColors.MutedSilver)
            }
        }
    )
}
