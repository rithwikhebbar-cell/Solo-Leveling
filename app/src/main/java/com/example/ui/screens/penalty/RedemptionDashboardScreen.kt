package com.example.ui.screens.penalty

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.PenaltyRecord
import com.example.domain.model.RedemptionLifecycleState
import com.example.domain.model.RedemptionPlan
import com.example.domain.model.ShadowClass
import com.example.ui.components.GlowingProgressBar
import com.example.ui.components.HolographicPanel
import com.example.ui.components.ProvenanceChip
import com.example.ui.components.SystemEmptyState
import com.example.ui.theme.SystemColors
import com.example.ui.viewmodel.RedemptionUiState
import com.example.ui.viewmodel.RedemptionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedemptionDashboardScreen(
    viewModel: RedemptionViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "SYSTEM REDEMPTION PROTOCOL",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SystemColors.PureIce
                        )
                        Text(
                            text = "CORRECTIVE RESTORATION & POINT RESOLUTION",
                            style = MaterialTheme.typography.labelSmall,
                            color = SystemColors.ElectricCyan
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = SystemColors.PureIce
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SystemColors.ObsidianBlack
                )
            )
        },
        containerColor = SystemColors.ObsidianBlack,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // 1. Overview Summary
            item {
                RedemptionOverviewCard(uiState = uiState)
            }

            // 2. Outstanding Penalties Ready for Recovery
            if (uiState.activePenalties.isNotEmpty()) {
                item {
                    Text(
                        text = "OUTSTANDING PENALTIES ELIGIBLE FOR REDEMPTION",
                        style = MaterialTheme.typography.titleSmall,
                        color = SystemColors.ArcaneViolet
                    )
                }

                items(uiState.activePenalties, key = { "pen_${it.penaltyId}" }) { penalty ->
                    val hasExistingPlan = uiState.allRedemptions.any { it.penaltyId == penalty.penaltyId && !it.lifecycleState.isTerminal }
                    PenaltyRedemptionGatewayCard(
                        penalty = penalty,
                        hasExistingPlan = hasExistingPlan,
                        onInitiatePlan = { viewModel.generatePlanForPenalty(penalty.penaltyId) }
                    )
                }
            }

            // 3. Active Redemption Commitments
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ACTIVE REDEMPTION TRIALS (${uiState.activeRedemptions.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = SystemColors.ElectricCyan
                    )
                    ProvenanceChip(
                        provenance = "VERIFIED RECOVERY",
                        accentColor = SystemColors.ElectricCyan
                    )
                }
            }

            if (uiState.activeRedemptions.isEmpty()) {
                item {
                    SystemEmptyState(
                        title = "NO ACTIVE REDEMPTION TRIALS",
                        subtitle = "Select an outstanding penalty above to initiate a safe, structured recovery activity."
                    )
                }
            } else {
                items(uiState.activeRedemptions, key = { it.redemptionId }) { plan ->
                    RedemptionActivityCard(
                        plan = plan,
                        onAccept = { viewModel.acceptPlan(plan.redemptionId) },
                        onRequestAlternative = { viewModel.requestAlternative(plan.redemptionId) },
                        onPause = { viewModel.pausePlan(plan.redemptionId) },
                        onSubmitVerification = { viewModel.selectPlanForVerification(plan) }
                    )
                }
            }

            // 4. Completed Redemptions History
            val completedList = uiState.allRedemptions.filter { it.lifecycleState == RedemptionLifecycleState.VERIFIED_COMPLETED }
            if (completedList.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "VERIFIED REDEMPTIONS (${completedList.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = SystemColors.VerifiedEmerald
                    )
                }

                items(completedList, key = { "comp_${it.redemptionId}" }) { plan ->
                    RedemptionActivityCard(
                        plan = plan,
                        onAccept = {},
                        onRequestAlternative = {},
                        onPause = {},
                        onSubmitVerification = {},
                        isCompleted = true
                    )
                }
            }
        }

        // Verification Dialog
        uiState.selectedPlanForVerification?.let { plan ->
            RedemptionVerificationDialog(
                plan = plan,
                onDismiss = { viewModel.selectPlanForVerification(null) },
                onConfirm = { reflection, checklists ->
                    viewModel.submitVerification(plan.redemptionId, reflection, checklists)
                }
            )
        }
    }
}

@Composable
private fun RedemptionOverviewCard(uiState: RedemptionUiState) {
    val total = uiState.outstandingPenaltyPoints + uiState.totalPointsResolved
    val progressFraction = if (total > 0) uiState.totalPointsResolved.toFloat() / total else 1f

    HolographicPanel(
        modifier = Modifier.fillMaxWidth(),
        borderColor = SystemColors.ElectricCyan
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "OUTSTANDING PENALTY BALANCE",
                        style = MaterialTheme.typography.labelSmall,
                        color = SystemColors.MutedSilver
                    )
                    Text(
                        text = "${uiState.outstandingPenaltyPoints} PP",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.outstandingPenaltyPoints > 0) Color(0xFFFF5252) else SystemColors.PureIce
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "POINTS RESOLVED",
                        style = MaterialTheme.typography.labelSmall,
                        color = SystemColors.MutedSilver
                    )
                    Text(
                        text = "+${uiState.totalPointsResolved} PP",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = SystemColors.VerifiedEmerald
                    )
                }
            }

            GlowingProgressBar(
                progress = progressFraction,
                height = 6.dp,
                startColor = SystemColors.ElectricCyan,
                endColor = SystemColors.VerifiedEmerald
            )

            uiState.feedbackMessage?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = SystemColors.ElectricCyan
                )
            }
        }
    }
}

@Composable
private fun PenaltyRedemptionGatewayCard(
    penalty: PenaltyRecord,
    hasExistingPlan: Boolean,
    onInitiatePlan: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SystemColors.DeepNavy),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, SystemColors.SubtleBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = penalty.questTitleSnapshot,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = SystemColors.PureIce
                )
                Text(
                    text = "Outstanding: ${penalty.totalPenaltyPoints} PP • ${penalty.severity.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SystemColors.MetallicSilver
                )
            }

            if (!hasExistingPlan) {
                Button(
                    onClick = onInitiatePlan,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SystemColors.ArcaneViolet,
                        contentColor = SystemColors.PureIce
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("START RECOVERY", style = MaterialTheme.typography.labelSmall)
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SystemColors.ObsidianBlack
                ) {
                    Text(
                        text = "TRIAL ACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = SystemColors.ElectricCyan,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RedemptionActivityCard(
    plan: RedemptionPlan,
    onAccept: () -> Unit,
    onRequestAlternative: () -> Unit,
    onPause: () -> Unit,
    onSubmitVerification: () -> Unit,
    isCompleted: Boolean = false
) {
    val stateColor = when (plan.lifecycleState) {
        RedemptionLifecycleState.PENDING_ACCEPTANCE, RedemptionLifecycleState.GENERATED -> SystemColors.MonarchGold
        RedemptionLifecycleState.ACTIVE, RedemptionLifecycleState.ACCEPTED -> SystemColors.ElectricCyan
        RedemptionLifecycleState.VERIFIED_COMPLETED -> SystemColors.VerifiedEmerald
        RedemptionLifecycleState.PAUSED -> SystemColors.MutedSilver
        else -> SystemColors.MetallicSilver
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SystemColors.DeepNavy),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, stateColor.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Category & State
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = plan.category.displayName.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = SystemColors.MetallicSilver,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = stateColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, stateColor)
                ) {
                    Text(
                        text = plan.lifecycleState.label.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = stateColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            // Title & Points
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = plan.activityTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SystemColors.PureIce,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "-${plan.resolutionPoints} PP",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = SystemColors.VerifiedEmerald
                )
            }

            // Instructions
            Text(
                text = plan.instructions,
                style = MaterialTheme.typography.bodySmall,
                color = SystemColors.PureIce
            )

            // Duration & Difficulty tag
            Text(
                text = "Estimated Time: ${plan.estimatedDurationMinutes} mins  •  Difficulty: ${plan.difficulty.displayName}",
                style = MaterialTheme.typography.labelSmall,
                color = SystemColors.MutedSilver
            )

            // Shadow Guidance Callout Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = SystemColors.ObsidianBlack.copy(alpha = 0.7f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, SystemColors.ArcaneViolet.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = SystemColors.ArcaneViolet,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = plan.shadowGuidanceNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = SystemColors.MetallicSilver
                    )
                }
            }

            // Actions
            if (!isCompleted) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (plan.isPendingAcceptance) {
                        Button(
                            onClick = onAccept,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SystemColors.ElectricCyan,
                                contentColor = SystemColors.ObsidianBlack
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("ACCEPT TRIAL", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onRequestAlternative,
                            border = BorderStroke(1.dp, SystemColors.SubtleBorder),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("ALTERNATIVE", style = MaterialTheme.typography.labelSmall, color = SystemColors.MutedSilver)
                        }
                    } else if (plan.isActive) {
                        Button(
                            onClick = onSubmitVerification,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SystemColors.VerifiedEmerald,
                                contentColor = SystemColors.ObsidianBlack
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("SUBMIT VERIFICATION", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onPause,
                            border = BorderStroke(1.dp, SystemColors.SubtleBorder),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("PAUSE", style = MaterialTheme.typography.labelSmall, color = SystemColors.MutedSilver)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RedemptionVerificationDialog(
    plan: RedemptionPlan,
    onDismiss: () -> Unit,
    onConfirm: (String, List<String>) -> Unit
) {
    var reflectionText by remember { mutableStateOf("") }
    val confirmedCheckpoints = remember { mutableStateListOf<String>() }

    val defaultCheckpoints = listOf(
        "Identified underlying roadblock leading to failure",
        "Realigned schedule or environment to prevent recurrence",
        "Confirmed complete compliance with corrective protocol"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SystemColors.DeepNavy,
        shape = RoundedCornerShape(12.dp),
        title = {
            Text(
                text = "VERIFY REDEMPTION COMPLETION",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = SystemColors.ElectricCyan
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Activity: ${plan.activityTitle}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SystemColors.MetallicSilver
                )

                Text(
                    text = "Checklist Verification:",
                    style = MaterialTheme.typography.labelSmall,
                    color = SystemColors.PureIce,
                    fontWeight = FontWeight.Bold
                )

                defaultCheckpoints.forEach { checkpoint ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (checkpoint in confirmedCheckpoints) confirmedCheckpoints.remove(checkpoint)
                                else confirmedCheckpoints.add(checkpoint)
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checkpoint in confirmedCheckpoints,
                            onCheckedChange = { isChecked ->
                                if (isChecked) confirmedCheckpoints.add(checkpoint)
                                else confirmedCheckpoints.remove(checkpoint)
                            },
                            colors = CheckboxDefaults.colors(checkedColor = SystemColors.ElectricCyan)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = checkpoint,
                            style = MaterialTheme.typography.bodySmall,
                            color = SystemColors.PureIce
                        )
                    }
                }

                Text(
                    text = "Reflective Assessment (Minimum 20 chars):",
                    style = MaterialTheme.typography.labelSmall,
                    color = SystemColors.PureIce,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = reflectionText,
                    onValueChange = { reflectionText = it },
                    placeholder = { Text("Describe the adjustment made to resolve this commitment friction...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SystemColors.ElectricCyan,
                        unfocusedBorderColor = SystemColors.SubtleBorder,
                        focusedTextColor = SystemColors.PureIce,
                        unfocusedTextColor = SystemColors.PureIce
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(reflectionText, confirmedCheckpoints.toList()) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = SystemColors.VerifiedEmerald,
                    contentColor = SystemColors.ObsidianBlack
                )
            ) {
                Text("VERIFY & RESOLVE PP", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", style = MaterialTheme.typography.labelSmall, color = SystemColors.MutedSilver)
            }
        }
    )
}
