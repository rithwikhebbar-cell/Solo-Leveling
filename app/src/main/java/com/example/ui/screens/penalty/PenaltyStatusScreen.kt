package com.example.ui.screens.penalty

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.PenaltyRecord
import com.example.domain.model.PenaltySeverity
import com.example.domain.model.PenaltyStatus
import com.example.ui.components.HolographicPanel
import com.example.ui.components.ProvenanceChip
import com.example.ui.components.SystemEmptyState
import com.example.ui.theme.SystemColors
import com.example.ui.viewmodel.PenaltyUiState
import com.example.ui.viewmodel.PenaltyViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PenaltyStatusScreen(
    viewModel: PenaltyViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToRedemption: (() -> Unit)? = null,
    onNavigateToAudit: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "SYSTEM DISCIPLINE & PENALTY",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SystemColors.PureIce
                        )
                        Text(
                            text = "ACCOUNTABILITY & POINT LEDGER",
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
                actions = {
                    if (onNavigateToAudit != null) {
                        IconButton(onClick = onNavigateToAudit) {
                            Icon(
                                imageVector = Icons.rounded.History,
                                contentDescription = "Audit & Analytics Dashboard",
                                tint = SystemColors.ElectricCyan
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.reconcile() }) {
                        Icon(
                            imageVector = Icons.rounded.Refresh,
                            contentDescription = "Reconcile Ledger",
                            tint = SystemColors.ElectricCyan
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
            // 1. Overview Card
            item {
                PenaltyOverviewCard(
                    uiState = uiState,
                    onReconcileClick = { viewModel.reconcile() }
                )
            }

            if (onNavigateToRedemption != null) {
                item {
                    Button(
                        onClick = onNavigateToRedemption,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SystemColors.ArcaneViolet,
                            contentColor = SystemColors.PureIce
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "START REDEMPTION PROTOCOL",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 2. Active Penalties Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ACTIVE PENALTIES (${uiState.activePenalties.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = SystemColors.ElectricCyan
                    )
                    ProvenanceChip(
                        provenance = "IMMUTABLE LEDGER",
                        accentColor = SystemColors.ElectricCyan
                    )
                }
            }

            if (uiState.activePenalties.isEmpty()) {
                item {
                    SystemEmptyState(
                        title = "NO ACTIVE PENALTIES",
                        subtitle = "Compliance records are optimal. Accepted commitments have met all verified requirements."
                    )
                }
            } else {
                items(uiState.activePenalties, key = { it.penaltyId }) { penalty ->
                    PenaltyCard(penalty = penalty)
                }
            }

            // 3. Historical Record Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PENALTY AUDIT HISTORY (${uiState.allPenalties.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = SystemColors.MutedSilver
                    )
                    Icon(
                        imageVector = Icons.rounded.History,
                        contentDescription = null,
                        tint = SystemColors.MutedSilver
                    )
                }
            }

            if (uiState.allPenalties.isEmpty()) {
                item {
                    Text(
                        text = "No historical penalty records recorded.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SystemColors.MutedSilver
                    )
                }
            } else {
                items(uiState.allPenalties, key = { "hist_${it.penaltyId}" }) { penalty ->
                    PenaltyCard(penalty = penalty, isHistorical = true)
                }
            }
        }
    }
}

@Composable
private fun PenaltyOverviewCard(
    uiState: PenaltyUiState,
    onReconcileClick: () -> Unit
) {
    HolographicPanel(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (uiState.accountBalance.currentBalance > 0) SystemColors.ArcaneViolet else SystemColors.ElectricCyan
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
                        text = "CURRENT PENALTY BALANCE",
                        style = MaterialTheme.typography.labelSmall,
                        color = SystemColors.MutedSilver
                    )
                    Text(
                        text = "${uiState.accountBalance.currentBalance} PP",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.accountBalance.currentBalance > 0) Color(0xFFFF5252) else SystemColors.PureIce
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SystemColors.DeepNavy,
                    border = BorderStroke(1.dp, SystemColors.SubtleBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "ACTIVE",
                            style = MaterialTheme.typography.labelSmall,
                            color = SystemColors.MutedSilver
                        )
                        Text(
                            text = "${uiState.accountBalance.activePenaltiesCount}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SystemColors.ElectricCyan
                        )
                    }
                }
            }

            // Severity Distribution Breakdown
            Text(
                text = "SEVERITY DISTRIBUTION",
                style = MaterialTheme.typography.labelSmall,
                color = SystemColors.MutedSilver
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PenaltySeverity.entries.forEach { severity ->
                    val count = uiState.severityDistribution[severity] ?: 0
                    SeverityChip(
                        severity = severity,
                        count = count,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            uiState.statusMessage?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = SystemColors.VerifiedEmerald
                )
            }
        }
    }
}

@Composable
private fun SeverityChip(
    severity: PenaltySeverity,
    count: Int,
    modifier: Modifier = Modifier
) {
    val chipColor = when (severity) {
        PenaltySeverity.P1 -> SystemColors.ElectricCyan
        PenaltySeverity.P2 -> SystemColors.ArcaneViolet
        PenaltySeverity.P3 -> Color(0xFFFFB300)
        PenaltySeverity.P4 -> Color(0xFFFF3D00)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = SystemColors.DeepNavy,
        border = BorderStroke(1.dp, if (count > 0) chipColor else SystemColors.SubtleBorder)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = severity.code,
                style = MaterialTheme.typography.labelSmall,
                color = chipColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$count",
                style = MaterialTheme.typography.bodyMedium,
                color = SystemColors.PureIce,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun PenaltyCard(
    penalty: PenaltyRecord,
    isHistorical: Boolean = false,
    modifier: Modifier = Modifier
) {
    val severityColor = when (penalty.severity) {
        PenaltySeverity.P1 -> SystemColors.ElectricCyan
        PenaltySeverity.P2 -> SystemColors.ArcaneViolet
        PenaltySeverity.P3 -> Color(0xFFFFB300)
        PenaltySeverity.P4 -> Color(0xFFFF3D00)
    }

    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
        .withZone(ZoneId.systemDefault())
    val formattedDate = dateFormatter.format(Instant.ofEpochMilli(penalty.createdAtEpochMs))

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = SystemColors.DeepNavy
        ),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (isHistorical) SystemColors.SubtleBorder else severityColor.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row: Title & Severity Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = penalty.questTitleSnapshot,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SystemColors.PureIce,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = severityColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, severityColor)
                ) {
                    Text(
                        text = "${penalty.severity.code} ${penalty.severity.displayName}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = severityColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            // Calculation Breakdown: Base + Bonus = Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (penalty.escalationBonusPoints > 0) {
                        "${penalty.basePenaltyPoints} Base PP + ${penalty.escalationBonusPoints} Escalation Bonus"
                    } else {
                        "${penalty.basePenaltyPoints} Base PP (Standard)"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = SystemColors.MetallicSilver
                )

                Text(
                    text = "+${penalty.totalPenaltyPoints} PP",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = severityColor
                )
            }

            // Explanatory reason
            Text(
                text = penalty.reason,
                style = MaterialTheme.typography.bodySmall,
                color = SystemColors.PureIce
            )

            // Footer metadata: Policy version, status, confirmed timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$formattedDate  •  ${penalty.policyVersion}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SystemColors.MutedSilver
                )

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (penalty.penaltyStatus == PenaltyStatus.ACTIVE || penalty.penaltyStatus == PenaltyStatus.ESCALATED) {
                        Color(0xFF331A24)
                    } else {
                        SystemColors.ObsidianBlack
                    }
                ) {
                    Text(
                        text = penalty.penaltyStatus.displayName.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (penalty.penaltyStatus.isOutstanding) Color(0xFFFF5252) else SystemColors.VerifiedEmerald,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
