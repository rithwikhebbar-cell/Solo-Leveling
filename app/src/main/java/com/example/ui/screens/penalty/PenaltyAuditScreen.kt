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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.DetectedFailurePattern
import com.example.domain.model.ExemptionLifecycleState
import com.example.domain.model.ExemptionReasonCategory
import com.example.domain.model.PatternSeverity
import com.example.domain.model.PenaltyAuditTimelineItem
import com.example.domain.model.PenaltyTimelineType
import com.example.domain.model.PenaltyTransaction
import com.example.domain.model.QuestExemption
import com.example.ui.components.HolographicPanel
import com.example.ui.components.ProvenanceChip
import com.example.ui.components.SystemEmptyState
import com.example.ui.theme.SystemColors
import com.example.ui.viewmodel.PenaltyAuditUiState
import com.example.ui.viewmodel.PenaltyAuditViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PenaltyAuditScreen(
    viewModel: PenaltyAuditViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.statusMessage, uiState.errorMessage) {
        val msg = uiState.statusMessage ?: uiState.errorMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "SYSTEM AUDIT & INTEGRITY",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SystemColors.PureIce
                        )
                        Text(
                            text = "ANALYTICS, LEDGER & EXEMPTIONS",
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
                    IconButton(
                        onClick = { viewModel.reconcileLedger() },
                        enabled = !uiState.isReconciling
                    ) {
                        if (uiState.isReconciling) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(20.dp).width(20.dp),
                                color = SystemColors.ElectricCyan,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.rounded.Refresh,
                                contentDescription = "Reconcile Ledger",
                                tint = SystemColors.ElectricCyan
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SystemColors.DeepAbyss
                )
            )
        },
        containerColor = SystemColors.DeepAbyss,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tabs
            ScrollableTabRow(
                selectedTabIndex = uiState.selectedTab,
                containerColor = SystemColors.AbyssalNavy,
                contentColor = SystemColors.ElectricCyan,
                edgePadding = 16.dp
            ) {
                Tab(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.setSelectedTab(0) },
                    text = { Text("Overview & Patterns") }
                )
                Tab(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.setSelectedTab(1) },
                    text = { Text("Audit Timeline") }
                )
                Tab(
                    selected = uiState.selectedTab == 2,
                    onClick = { viewModel.setSelectedTab(2) },
                    text = { Text("Ledger History") }
                )
                Tab(
                    selected = uiState.selectedTab == 3,
                    onClick = { viewModel.setSelectedTab(3) },
                    text = { Text("Exemptions (${uiState.exemptions.size})") }
                )
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = SystemColors.ElectricCyan)
                }
            } else {
                when (uiState.selectedTab) {
                    0 -> OverviewAndPatternsTab(uiState = uiState, onGenerateAiAdvisory = { viewModel.requestAiAdvisoryExplanation() })
                    1 -> TimelineTab(uiState = uiState)
                    2 -> LedgerTab(uiState = uiState)
                    3 -> ExemptionsTab(
                        uiState = uiState,
                        onSubmitExemption = { cId, cat, exp -> viewModel.submitExemption(cId, cat, exp) },
                        onWithdrawExemption = { exId -> viewModel.withdrawExemption(exId) },
                        onReviewExemption = { exId, approved, notes -> viewModel.reviewExemption(exId, approved, notes) }
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewAndPatternsTab(
    uiState: PenaltyAuditUiState,
    onGenerateAiAdvisory: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top KPI Cards
        item {
            HolographicPanel(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ACCOUNTABILITY METRICS",
                        style = MaterialTheme.typography.labelSmall,
                        color = SystemColors.ElectricCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        KpiItem(
                            label = "Outstanding PP",
                            value = "${uiState.overview.currentBalance} PP",
                            color = if (uiState.overview.currentBalance > 0) SystemColors.CrimsonAlert else SystemColors.ElectricCyan
                        )
                        KpiItem(
                            label = "Lifetime Accrued",
                            value = "${uiState.overview.lifetimePointsAccrued} PP",
                            color = SystemColors.PureIce
                        )
                        KpiItem(
                            label = "Lifetime Deducted",
                            value = "${uiState.overview.lifetimePointsDeducted} PP",
                            color = SystemColors.BioSuccess
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        KpiItem(
                            label = "Active Penalties",
                            value = "${uiState.overview.activePenaltiesCount}",
                            color = if (uiState.overview.activePenaltiesCount > 0) SystemColors.CrimsonAlert else SystemColors.PureIce
                        )
                        KpiItem(
                            label = "Resolved",
                            value = "${uiState.overview.resolvedPenaltiesCount}",
                            color = SystemColors.BioSuccess
                        )
                        KpiItem(
                            label = "Recovery Rate",
                            value = "${uiState.overview.recoveryRatePercent}%",
                            color = SystemColors.ElectricCyan
                        )
                    }
                }
            }
        }

        // Consistency Streak Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SystemColors.AbyssalNavy),
                border = BorderStroke(1.dp, SystemColors.MidnightCardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertangement
                ) {
                    Icon(
                        imageVector = Icons.rounded.Shield,
                        contentDescription = null,
                        tint = SystemColors.ElectricCyan,
                        modifier = Modifier.height(32.dp).width(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Clean Commitment Streak",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = SystemColors.PureIce
                        )
                        Text(
                            text = "${uiState.consistencyMetrics.currentCleanStreakDays} Consecutive Days Clean (Record: ${uiState.consistencyMetrics.longestCleanStreakDays}d) • Trend: ${uiState.consistencyMetrics.streakTrend.label}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SystemColors.DimText
                        )
                    }
                }
            }
        }

        // Detected Failure Patterns Section
        item {
            Text(
                text = "FRICTION & FAILURE PATTERN ANALYSIS",
                style = MaterialTheme.typography.labelSmall,
                color = SystemColors.ElectricCyan,
                fontWeight = FontWeight.Bold
            )
        }

        if (uiState.detectedPatterns.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SystemColors.AbyssalNavy),
                    border = BorderStroke(1.dp, SystemColors.MidnightCardBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.rounded.CheckCircle,
                            contentDescription = null,
                            tint = SystemColors.BioSuccess
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "No structural friction patterns detected. Commitment execution is stable.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SystemColors.PureIce
                        )
                    }
                }
            }
        } else {
            items(uiState.detectedPatterns) { pattern ->
                PatternCard(pattern = pattern)
            }
        }

        // AI Strategic Advisory Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SystemColors.AbyssalNavy),
                border = BorderStroke(1.dp, SystemColors.MidnightCardBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.rounded.AutoAwesome,
                                contentDescription = null,
                                tint = SystemColors.ElectricCyan
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SYSTEM ADVISORY (ZERO AUTHORITY)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SystemColors.PureIce
                            )
                        }
                        if (uiState.isGeneratingAdvisory) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(16.dp).width(16.dp),
                                color = SystemColors.ElectricCyan,
                                strokeWidth = 2.dp
                            )
                        } else {
                            TextButton(onClick = onGenerateAiAdvisory) {
                                Text("Ask Advice", color = SystemColors.ElectricCyan)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.aiAdvisoryExplanation
                            ?: "Request a non-authoritative strategic analysis of your habit friction and commitment balance.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SystemColors.DimText
                    )
                }
            }
        }
    }
}

@Composable
private fun PatternCard(pattern: DetectedFailurePattern) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SystemColors.AbyssalNavy),
        border = BorderStroke(
            1.dp,
            if (pattern.severity == PatternSeverity.HIGH) SystemColors.CrimsonAlert else SystemColors.MidnightCardBorder
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = pattern.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = SystemColors.PureIce
                )
                ProvenanceChip(
                    text = "SEVERITY: ${pattern.severity.name}",
                    isVerified = pattern.severity != PatternSeverity.HIGH
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Observed Evidence: ${pattern.description}",
                style = MaterialTheme.typography.bodySmall,
                color = SystemColors.DimText
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SystemColors.DeepAbyss.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.rounded.Lightbulb,
                    contentDescription = null,
                    tint = SystemColors.ElectricCyan,
                    modifier = Modifier.height(18.dp).width(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = pattern.actionableRecommendation,
                    style = MaterialTheme.typography.bodySmall,
                    color = SystemColors.PureIce
                )
            }
        }
    }
}

@Composable
private fun TimelineTab(uiState: PenaltyAuditUiState) {
    if (uiState.timelineItems.isEmpty()) {
        SystemEmptyState(
            title = "NO AUDIT TIMELINE RECORDS",
            message = "All commitments and penalty events will appear chronologically here.",
            icon = Icons.rounded.Timeline
        )
    } else {
        val formatter = remember {
            DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
                .withZone(ZoneId.systemDefault())
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(uiState.timelineItems, key = { it.id }) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SystemColors.AbyssalNavy),
                    border = BorderStroke(1.dp, SystemColors.MidnightCardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = SystemColors.PureIce
                            )
                            ProvenanceChip(
                                text = item.statusTag,
                                isVerified = item.type != PenaltyTimelineType.FAILURE
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = SystemColors.DimText
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = formatter.format(Instant.ofEpochMilli(item.timestampEpochMs)),
                            style = MaterialTheme.typography.labelSmall,
                            color = SystemColors.ElectricCyan.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LedgerTab(uiState: PenaltyAuditUiState) {
    if (uiState.transactions.isEmpty()) {
        SystemEmptyState(
            title = "LEDGER IS CLEAN",
            message = "No transactions have been recorded in the penalty ledger.",
            icon = Icons.rounded.ReceiptLong
        )
    } else {
        val formatter = remember {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault())
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SystemColors.DeepAbyss),
                    border = BorderStroke(1.dp, SystemColors.MidnightCardBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "IMMUTABLE AUDIT TRAIL",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SystemColors.ElectricCyan
                        )
                        Text(
                            text = "Every credit (allocation) and debit (redemption resolution / reversal) is immutably recorded.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SystemColors.DimText
                        )
                    }
                }
            }

            items(uiState.transactions, key = { it.transactionId }) { tx ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SystemColors.AbyssalNavy),
                    border = BorderStroke(1.dp, SystemColors.MidnightCardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tx.transactionType.label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = SystemColors.PureIce
                            )
                            val deltaPrefix = if (tx.transactionType.isDebit) "-" else "+"
                            val deltaColor = if (tx.transactionType.isDebit) SystemColors.BioSuccess else SystemColors.CrimsonAlert
                            Text(
                                text = "$deltaPrefix${tx.pointsDelta} PP",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = deltaColor
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = tx.reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = SystemColors.DimText
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Balance After: ${tx.balanceAfter} PP",
                                style = MaterialTheme.typography.labelSmall,
                                color = SystemColors.ElectricCyan
                            )
                            Text(
                                text = formatter.format(Instant.ofEpochMilli(tx.timestampEpochMs)),
                                style = MaterialTheme.typography.labelSmall,
                                color = SystemColors.DimText
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExemptionsTab(
    uiState: PenaltyAuditUiState,
    onSubmitExemption: (commitmentId: String, category: ExemptionReasonCategory, explanation: String) -> Unit,
    onWithdrawExemption: (exemptionId: String) -> Unit,
    onReviewExemption: (exemptionId: String, isApproved: Boolean, notes: String?) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedCommitmentId by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ExemptionReasonCategory.EMERGENCY) }
    var explanationText by remember { mutableStateOf("") }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var commitmentDropdownExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HolographicPanel(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "EMERGENCY EXEMPTION GATEWAY",
                                style = MaterialTheme.typography.labelSmall,
                                color = SystemColors.ElectricCyan,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Non-punitive safety protection for legitimate exceptions",
                                style = MaterialTheme.typography.bodySmall,
                                color = SystemColors.DimText
                            )
                        }
                        OutlinedButton(
                            onClick = { showCreateDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SystemColors.ElectricCyan),
                            border = BorderStroke(1.dp, SystemColors.ElectricCyan)
                        ) {
                            Text("Request Exemption")
                        }
                    }
                }
            }
        }

        if (uiState.exemptions.isEmpty()) {
            item {
                SystemEmptyState(
                    title = "NO EXEMPTION REQUESTS",
                    message = "Exemptions requested for illness, injury, or emergencies will appear here.",
                    icon = Icons.rounded.HealthAndSafety
                )
            }
        } else {
            items(uiState.exemptions, key = { it.exemptionId }) { ex ->
                ExemptionCard(
                    exemption = ex,
                    onWithdraw = { onWithdrawExemption(ex.exemptionId) },
                    onApprove = { onReviewExemption(ex.exemptionId, true, "Approved under safety review") },
                    onDeny = { onReviewExemption(ex.exemptionId, false, "Standard requirements upheld") }
                )
            }
        }
    }

    if (showCreateDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = {
                Text(
                    text = "Request Emergency Exemption",
                    style = MaterialTheme.typography.titleMedium,
                    color = SystemColors.PureIce,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Select Commitment:",
                        style = MaterialTheme.typography.labelSmall,
                        color = SystemColors.ElectricCyan
                    )
                    ExposedDropdownMenuBox(
                        expanded = commitmentDropdownExpanded,
                        onExpandedChange = { commitmentDropdownExpanded = !commitmentDropdownExpanded }
                    ) {
                        val selectedTitle = uiState.questTitleMap[
                            uiState.eligibleCommitmentsForExemption.find { it.commitmentId == selectedCommitmentId }?.questId
                        ] ?: selectedCommitmentId.ifBlank { "Select Quest Commitment" }

                        OutlinedTextField(
                            value = selectedTitle,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = commitmentDropdownExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = SystemColors.PureIce,
                                unfocusedTextColor = SystemColors.PureIce
                            ),
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = commitmentDropdownExpanded,
                            onDismissRequest = { commitmentDropdownExpanded = false }
                        ) {
                            uiState.eligibleCommitmentsForExemption.forEach { commit ->
                                val qTitle = uiState.questTitleMap[commit.questId] ?: "Quest #${commit.questId}"
                                DropdownMenuItem(
                                    text = { Text("$qTitle (${commit.state.name})") },
                                    onClick = {
                                        selectedCommitmentId = commit.commitmentId
                                        commitmentDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Text(
                        text = "Exemption Category:",
                        style = MaterialTheme.typography.labelSmall,
                        color = SystemColors.ElectricCyan
                    )
                    ExposedDropdownMenuBox(
                        expanded = categoryDropdownExpanded,
                        onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory.label,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = SystemColors.PureIce,
                                unfocusedTextColor = SystemColors.PureIce
                            ),
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = categoryDropdownExpanded,
                            onDismissRequest = { categoryDropdownExpanded = false }
                        ) {
                            ExemptionReasonCategory.entries.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.label) },
                                    onClick = {
                                        selectedCategory = cat
                                        categoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = explanationText,
                        onValueChange = { explanationText = it },
                        label = { Text("Reason / Explanation") },
                        placeholder = { Text("Explain the circumstances (e.g. medical recovery, system outage)...") },
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = SystemColors.PureIce,
                            unfocusedTextColor = SystemColors.PureIce
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (selectedCommitmentId.isNotBlank() && explanationText.isNotBlank()) {
                            onSubmitExemption(selectedCommitmentId, selectedCategory, explanationText)
                            showCreateDialog = false
                            explanationText = ""
                        }
                    },
                    enabled = selectedCommitmentId.isNotBlank() && explanationText.isNotBlank()
                ) {
                    Text("Submit", color = SystemColors.ElectricCyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = SystemColors.DimText)
                }
            },
            containerColor = SystemColors.AbyssalNavy
        )
    }
}

@Composable
private fun ExemptionCard(
    exemption: QuestExemption,
    onWithdraw: () -> Unit,
    onApprove: () -> Unit,
    onDeny: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SystemColors.AbyssalNavy),
        border = BorderStroke(1.dp, SystemColors.MidnightCardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = exemption.category.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = SystemColors.PureIce
                )
                ProvenanceChip(
                    text = exemption.lifecycleState.label,
                    isVerified = exemption.lifecycleState == ExemptionLifecycleState.APPROVED
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = exemption.explanation,
                style = MaterialTheme.typography.bodySmall,
                color = SystemColors.PureIce
            )
            exemption.reviewerNotes?.let { notes ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Review Decision: $notes",
                    style = MaterialTheme.typography.labelSmall,
                    color = SystemColors.ElectricCyan
                )
            }
            if (exemption.lifecycleState == ExemptionLifecycleState.PENDING_REVIEW ||
                exemption.lifecycleState == ExemptionLifecycleState.REQUESTED
            ) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onWithdraw) {
                        Text("Withdraw", color = SystemColors.DimText)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onDeny) {
                        Text("Deny", color = SystemColors.CrimsonAlert)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onApprove) {
                        Text("Approve", color = SystemColors.BioSuccess)
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiItem(
    label: String,
    value: String,
    color: Color
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = SystemColors.DimText
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
