package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.healthconnect.AndroidHealthConnectDataSource
import com.example.data.healthconnect.HealthConnectSdkAvailability
import com.example.domain.model.ExerciseRecord
import com.example.domain.model.JoggingSession
import com.example.domain.model.PhysicalActivityKind
import com.example.domain.model.RecoveryState
import com.example.domain.model.WorkoutCategory
import com.example.domain.model.WorkoutSession
import com.example.ui.components.GlowingProgressBar
import com.example.ui.components.HolographicPanel
import com.example.ui.components.ProvenanceChip
import com.example.ui.components.SystemEmptyState
import com.example.ui.components.SystemSectionTitle
import com.example.ui.theme.SystemColors
import com.example.ui.viewmodel.RealityOsUiState
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import java.util.Locale

private enum class PhysicalSubTab(val label: String) {
    OVERVIEW("Overview"),
    JOGGING("Jogging"),
    WORKOUTS("Workouts"),
    RECOVERY("Recovery"),
    HEALTH_CONNECT("Health Connect")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PhysicalDevelopmentScreen(
    state: RealityOsUiState.Ready,
    onStartActiveJogging: () -> Unit,
    onPauseResumeActiveJogging: () -> Unit,
    onDiscardActiveJogging: () -> Unit,
    onCompleteActiveJogging: (String, String, String) -> Unit,
    onLogManualJogging: (String, Int, String, String) -> Unit,
    onRecordStructuredWorkout: (String, WorkoutCategory, Int, List<ExerciseRecord>, Boolean, String) -> Unit,
    onSubmitRecoveryCheckIn: (Int, Boolean, String) -> Unit,
    onMarkRestDay: () -> Unit,
    onAcceptProposal: (Int) -> Unit,
    onDismissProposal: (Int) -> Unit,
    onRefreshPermissions: () -> Unit,
    onSyncHealthConnect: () -> Unit,
    onClearFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableStateOf(PhysicalSubTab.OVERVIEW) }
    var showActiveJogSheet by rememberSaveable { mutableStateOf(false) }
    var showManualJogSheet by rememberSaveable { mutableStateOf(false) }
    var workoutCategoryToOpen by rememberSaveable { mutableStateOf<WorkoutCategory?>(null) }
    var showRecoveryCheckInSheet by rememberSaveable { mutableStateOf(false) }

    val hcPermissionLauncher = rememberLauncherForActivityResult(
        contract = AndroidHealthConnectDataSource.createPermissionRequestContract()
    ) {
        onRefreshPermissions()
    }

    val bundle = state.physicalBundle
    val timeFormatter = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SystemColors.ObsidianBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .testTag("physical_development_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SystemSectionTitle(
                    title = "Physical Development Engine",
                    subtitle = "Verified telemetry, jogging, workouts & recovery"
                )
            }

            if (bundle.syncFeedbackMessage != null) {
                item {
                    HolographicPanel(
                        modifier = Modifier.fillMaxWidth(),
                        accentColor = SystemColors.ElectricCyan,
                        contentPadding = 12.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = bundle.syncFeedbackMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = SystemColors.PureIce,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onClearFeedback) {
                                Text("OK", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            if (bundle.activeJoggingSession.isActive) {
                item {
                    HolographicPanel(
                        modifier = Modifier.fillMaxWidth(),
                        accentColor = SystemColors.VerifiedEmerald
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "LIVE JOGGING SESSION IN PROGRESS",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SystemColors.VerifiedEmerald
                                )
                                Text(
                                    text = if (bundle.activeJoggingSession.isPaused) "Timer paused" else "Recording elapsed time",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SystemColors.MetallicSilver
                                )
                            }
                            Button(
                                onClick = { showActiveJogSheet = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SystemColors.VerifiedEmerald,
                                    contentColor = SystemColors.ObsidianBlack
                                )
                            ) {
                                Text("OPEN TIMER", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PhysicalSubTab.entries.forEach { tab ->
                        FilterChip(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            label = { Text(tab.label) },
                            modifier = Modifier.testTag("physical_tab_${tab.name.lowercase()}"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SystemColors.ElectricCyan.copy(alpha = 0.22f),
                                selectedLabelColor = SystemColors.ElectricCyan
                            )
                        )
                    }
                }
            }

            when (selectedTab) {
                PhysicalSubTab.OVERVIEW -> {
                    item {
                        PhysicalOverviewTab(
                            state = state,
                            onOpenJogging = { selectedTab = PhysicalSubTab.JOGGING },
                            onOpenWorkouts = { selectedTab = PhysicalSubTab.WORKOUTS },
                            onOpenRecovery = { selectedTab = PhysicalSubTab.RECOVERY },
                            onOpenHealthConnect = { selectedTab = PhysicalSubTab.HEALTH_CONNECT }
                        )
                    }
                }
                PhysicalSubTab.JOGGING -> {
                    item {
                        JoggingDashboardHeader(
                            state = state,
                            onStartLive = {
                                if (!bundle.activeJoggingSession.isActive) {
                                    onStartActiveJogging()
                                }
                                showActiveJogSheet = true
                            },
                            onOpenManual = { showManualJogSheet = true },
                            onAcceptProposal = onAcceptProposal,
                            onDismissProposal = onDismissProposal
                        )
                    }
                    if (bundle.joggingSessions.isEmpty()) {
                        item {
                            SystemEmptyState(
                                title = "No Jogging Sessions Recorded",
                                description = "Start a live jogging timer, log a session manually, or sync Health Connect workouts."
                            )
                        }
                    } else {
                        items(bundle.joggingSessions, key = { it.id }) { session ->
                            JoggingHistoryCard(session = session, formattedDate = timeFormatter.format(Date(session.startEpochMs)))
                        }
                    }
                }
                PhysicalSubTab.WORKOUTS -> {
                    item {
                        WorkoutDashboardHeader(
                            advisoryCategory = bundle.advisoryWorkoutCategory,
                            onSelectCategoryToStart = { cat -> workoutCategoryToOpen = cat }
                        )
                    }
                    if (bundle.workoutSessions.isEmpty()) {
                        item {
                            SystemEmptyState(
                                title = "No Workouts Recorded",
                                description = "Select a category above to run a structured sets/reps/rest workout or sync from Health Connect."
                            )
                        }
                    } else {
                        items(bundle.workoutSessions, key = { it.id }) { workout ->
                            WorkoutHistoryCard(workout = workout, formattedDate = timeFormatter.format(Date(workout.startEpochMs)))
                        }
                    }
                }
                PhysicalSubTab.RECOVERY -> {
                    item {
                        RecoveryDashboardTab(
                            state = state,
                            onOpenCheckIn = { showRecoveryCheckInSheet = true },
                            onMarkRestDay = onMarkRestDay
                        )
                    }
                }
                PhysicalSubTab.HEALTH_CONNECT -> {
                    item {
                        HealthConnectDashboardTab(
                            state = state,
                            timeFormatter = timeFormatter,
                            onRequestPermissions = {
                                hcPermissionLauncher.launch(AndroidHealthConnectDataSource.ALL_REQUIRED_PERMISSIONS)
                            },
                            onOpenSettings = {
                                runCatching {
                                    context.startActivity(AndroidHealthConnectDataSource.createHealthConnectSettingsIntent())
                                }
                            },
                            onSyncNow = onSyncHealthConnect
                        )
                    }
                    if (bundle.importedHealthActivities.isNotEmpty()) {
                        items(bundle.importedHealthActivities, key = { it.sourceId }) { imported ->
                            HolographicPanel(
                                modifier = Modifier.fillMaxWidth(),
                                accentColor = SystemColors.VerifiedEmerald,
                                contentPadding = 12.dp
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(imported.title, style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
                                        ProvenanceChip(status = imported.verificationStatus)
                                    }
                                    Text(
                                        text = "${imported.activityType} • ${imported.durationMinutes} min • +${imported.xpAwarded} XP",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = SystemColors.ElectricCyan
                                    )
                                    Text(
                                        text = "Source: ${imported.originPackage} • ${imported.validationOutcome.label}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SystemColors.MutedSilver
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showActiveJogSheet) {
        ModalBottomSheet(
            onDismissRequest = { showActiveJogSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = SystemColors.DeepNavy
        ) {
            ActiveJoggingSheetContent(
                activeState = bundle.activeJoggingSession,
                onPauseResume = onPauseResumeActiveJogging,
                onDiscard = {
                    onDiscardActiveJogging()
                    showActiveJogSheet = false
                },
                onComplete = { title, dist, notes ->
                    onCompleteActiveJogging(title, dist, notes)
                    showActiveJogSheet = false
                }
            )
        }
    }

    if (showManualJogSheet) {
        ModalBottomSheet(
            onDismissRequest = { showManualJogSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = SystemColors.DeepNavy
        ) {
            ManualJoggingSheetContent(
                onDismiss = { showManualJogSheet = false },
                onSubmit = { title, mins, dist, notes ->
                    onLogManualJogging(title, mins, dist, notes)
                    showManualJogSheet = false
                }
            )
        }
    }

    workoutCategoryToOpen?.let { category ->
        ModalBottomSheet(
            onDismissRequest = { workoutCategoryToOpen = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = SystemColors.DeepNavy
        ) {
            WorkoutBuilderSheetContent(
                initialCategory = category,
                onDismiss = { workoutCategoryToOpen = null },
                onCompleteWorkout = { title, cat, mins, exercises, notes ->
                    onRecordStructuredWorkout(title, cat, mins, exercises, false, notes)
                    workoutCategoryToOpen = null
                }
            )
        }
    }

    if (showRecoveryCheckInSheet) {
        ModalBottomSheet(
            onDismissRequest = { showRecoveryCheckInSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = SystemColors.DeepNavy
        ) {
            RecoveryCheckInSheetContent(
                initialFatigue = bundle.recoveryAssessment.selfReportedFatigue ?: 2,
                initialDiscomfort = bundle.recoveryAssessment.selfReportedDiscomfort,
                initialNotes = bundle.recoveryAssessment.discomfortNotes,
                onDismiss = { showRecoveryCheckInSheet = false },
                onSubmit = { fatigue, discomfort, notes ->
                    onSubmitRecoveryCheckIn(fatigue, discomfort, notes)
                    showRecoveryCheckInSheet = false
                }
            )
        }
    }
}

@Composable
private fun PhysicalOverviewTab(
    state: RealityOsUiState.Ready,
    onOpenJogging: () -> Unit,
    onOpenWorkouts: () -> Unit,
    onOpenRecovery: () -> Unit,
    onOpenHealthConnect: () -> Unit
) {
    val bundle = state.physicalBundle
    val recovery = bundle.recoveryAssessment
    val player = state.player

    val physicalPowerScore = player.attributes.str.totalValue +
        player.attributes.vit.totalValue +
        player.attributes.agi.totalValue +
        player.attributes.dis.totalValue

    val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60_000L
    val zone = ZoneId.systemDefault()
    val activeDays7d = (bundle.joggingSessions.filter { it.startEpochMs >= sevenDaysAgo }
        .map { Instant.ofEpochMilli(it.startEpochMs).atZone(zone).toLocalDate() } +
        bundle.workoutSessions.filter { it.startEpochMs >= sevenDaysAgo }
            .map { Instant.ofEpochMilli(it.startEpochMs).atZone(zone).toLocalDate() }).toSet().size

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        HolographicPanel(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("PHYSICAL OVERVIEW", style = MaterialTheme.typography.labelLarge, color = SystemColors.ElectricCyan)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OverviewMetric("ATTR POWER", "$physicalPowerScore", Modifier.weight(1f))
                    OverviewMetric("CONSISTENCY", "$activeDays7d / 7d", Modifier.weight(1f))
                    OverviewMetric("7D LOAD", "${recovery.measuredLoadMinutes7d}m", Modifier.weight(1f))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RECOVERY: ${recovery.state.label.uppercase()} (${recovery.readinessScore}/100)",
                        style = MaterialTheme.typography.labelMedium,
                        color = recoveryColor(recovery.state)
                    )
                    TextButton(onClick = onOpenRecovery) {
                        Text("CHECK IN")
                    }
                }
                Text(recovery.summaryMessage, style = MaterialTheme.typography.bodySmall, color = SystemColors.MetallicSilver)
            }
        }

        HolographicPanel(
            modifier = Modifier.fillMaxWidth(),
            accentColor = SystemColors.VerifiedEmerald
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("DAILY JOGGING PROGRESS", style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
                    Text(
                        "${state.physicalState.completedJoggingMinutesToday} / ${state.physicalState.effectiveJoggingTarget} min",
                        style = MaterialTheme.typography.labelLarge,
                        color = SystemColors.ElectricCyan
                    )
                }
                GlowingProgressBar(progress = state.physicalState.joggingProgressFraction, height = 8.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onOpenJogging,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SystemColors.ElectricCyan,
                            contentColor = SystemColors.ObsidianBlack
                        )
                    ) {
                        Text("JOGGING HUB", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(onClick = onOpenWorkouts, modifier = Modifier.weight(1f)) {
                        Text("WORKOUTS", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        HolographicPanel(
            modifier = Modifier.fillMaxWidth(),
            accentColor = SystemColors.ArcaneViolet
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("HEALTH CONNECT TELEMETRY", style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
                    Text(
                        text = "${bundle.healthConnectAvailability.label} • ${bundle.healthConnectPermissions.grantedCount}/${bundle.healthConnectPermissions.totalCount} permissions",
                        style = MaterialTheme.typography.bodySmall,
                        color = SystemColors.MutedSilver
                    )
                }
                OutlinedButton(onClick = onOpenHealthConnect) {
                    Text("SYNC HUB", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun JoggingDashboardHeader(
    state: RealityOsUiState.Ready,
    onStartLive: () -> Unit,
    onOpenManual: () -> Unit,
    onAcceptProposal: (Int) -> Unit,
    onDismissProposal: (Int) -> Unit
) {
    val physical = state.physicalState
    val bundle = state.physicalBundle
    val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60_000L
    val weeklyJogs = bundle.joggingSessions.filter {
        it.startEpochMs >= sevenDaysAgo && it.activityKind != PhysicalActivityKind.WALKING
    }
    val weeklyMinutes = weeklyJogs.sumOf { it.durationMinutes }
    val zone = ZoneId.systemDefault()
    val weeklyDays = weeklyJogs.map { Instant.ofEpochMilli(it.startEpochMs).atZone(zone).toLocalDate() }.toSet().size

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HolographicPanel(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.DirectionsRun, contentDescription = null, tint = SystemColors.ElectricCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("TODAY'S JOGGING TARGET", style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
                    }
                    Text(
                        text = "${physical.completedJoggingMinutesToday} / ${physical.effectiveJoggingTarget} min (${(physical.joggingProgressFraction * 100).toInt()}%)",
                        style = MaterialTheme.typography.labelLarge,
                        color = SystemColors.ElectricCyan
                    )
                }

                GlowingProgressBar(progress = physical.joggingProgressFraction, height = 9.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OverviewMetric("7D MINUTES", "${weeklyMinutes}m", Modifier.weight(1f))
                    OverviewMetric("7D SESSIONS", "${weeklyJogs.size}", Modifier.weight(1f))
                    OverviewMetric("CONSISTENCY", "$weeklyDays / 7d", Modifier.weight(1f))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onStartLive,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("start_live_jog_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SystemColors.ElectricCyan,
                            contentColor = SystemColors.ObsidianBlack
                        )
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("START SESSION", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = onOpenManual,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("open_manual_jog_button")
                    ) {
                        Icon(Icons.Rounded.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("MANUAL LOG", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        bundle.pendingJoggingProposal?.let { proposal ->
            HolographicPanel(
                modifier = Modifier.fillMaxWidth(),
                accentColor = SystemColors.ArcaneViolet
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = SystemColors.ArcaneViolet, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ADVISORY TARGET PROPOSAL: ${proposal.proposedMinutes} MIN",
                            style = MaterialTheme.typography.titleMedium,
                            color = SystemColors.ArcaneViolet
                        )
                    }
                    Text(proposal.rationale, style = MaterialTheme.typography.bodySmall, color = SystemColors.MetallicSilver)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { onAcceptProposal(proposal.proposedMinutes) },
                            modifier = Modifier.testTag("accept_jog_proposal_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SystemColors.ArcaneViolet,
                                contentColor = SystemColors.PureIce
                            )
                        ) {
                            Text("APPROVE ${proposal.proposedMinutes} MIN", style = MaterialTheme.typography.labelMedium)
                        }
                        OutlinedButton(onClick = { onDismissProposal(proposal.proposedMinutes) }) {
                            Text("KEEP ${proposal.currentMinutes} MIN", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JoggingHistoryCard(session: JoggingSession, formattedDate: String) {
    val distanceLabel = session.distanceKm?.let { "${"%.2f".format(it)} km" } ?: "Distance —"
    val paceLabel = session.avgPaceSecPerKm?.let { sec -> "${sec / 60}:${(sec % 60).toString().padStart(2, '0')} /km" } ?: "Pace —"
    val hrLabel = session.avgHeartRateBpm?.let { "$it bpm" }

    HolographicPanel(modifier = Modifier.fillMaxWidth(), contentPadding = 12.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${session.activityKind.label.uppercase()} • $formattedDate", style = MaterialTheme.typography.labelSmall, color = SystemColors.MutedSilver)
                ProvenanceChip(status = session.verificationStatus)
            }
            Text(session.title, style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
            Text(
                text = listOfNotNull("${session.durationMinutes} min", distanceLabel, paceLabel, hrLabel, "+${session.xpAwarded} XP").joinToString(" • "),
                style = MaterialTheme.typography.labelMedium,
                color = SystemColors.ElectricCyan
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorkoutDashboardHeader(
    advisoryCategory: Pair<WorkoutCategory, String>,
    onSelectCategoryToStart: (WorkoutCategory) -> Unit
) {
    HolographicPanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.FitnessCenter, contentDescription = null, tint = SystemColors.ElectricCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text("WORKOUT CATEGORIES", style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
            }
            Text(
                text = "System Suggestion (${advisoryCategory.first.label}): ${advisoryCategory.second}",
                style = MaterialTheme.typography.bodySmall,
                color = SystemColors.MetallicSilver
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WorkoutCategory.entries.forEach { cat ->
                    OutlinedButton(
                        onClick = { onSelectCategoryToStart(cat) },
                        modifier = Modifier.testTag("start_workout_${cat.name.lowercase()}"),
                        border = BorderStroke(1.dp, SystemColors.ElectricCyan)
                    ) {
                        Text("START ${cat.label.uppercase()}", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutHistoryCard(workout: WorkoutSession, formattedDate: String) {
    HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.ArcaneViolet, contentPadding = 12.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${workout.category.label.uppercase()} • $formattedDate", style = MaterialTheme.typography.labelSmall, color = SystemColors.MutedSilver)
                ProvenanceChip(status = workout.verificationStatus)
            }
            Text(workout.title, style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
            Text(
                text = "${workout.durationMinutes} min • ${workout.totalSets} sets / ${workout.totalReps} reps • +${workout.xpAwarded} XP",
                style = MaterialTheme.typography.labelMedium,
                color = SystemColors.ElectricCyan
            )
            if (workout.exercises.isNotEmpty()) {
                Text(
                    text = workout.exercises.joinToString(", ") { "${it.exerciseName} (${it.setsCompleted}x${it.repsPerSet})" },
                    style = MaterialTheme.typography.bodySmall,
                    color = SystemColors.MetallicSilver
                )
            }
        }
    }
}

@Composable
private fun RecoveryDashboardTab(
    state: RealityOsUiState.Ready,
    onOpenCheckIn: () -> Unit,
    onMarkRestDay: () -> Unit
) {
    val rec = state.physicalBundle.recoveryAssessment
    val accent = recoveryColor(rec.state)

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = accent) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("RECOVERY STATUS", style = MaterialTheme.typography.labelSmall, color = accent)
                        Text(rec.state.label.uppercase(), style = MaterialTheme.typography.headlineMedium, color = SystemColors.PureIce)
                    }
                    Text("${rec.readinessScore}/100", style = MaterialTheme.typography.displayLarge, color = accent)
                }
                Text(rec.summaryMessage, style = MaterialTheme.typography.bodyMedium, color = SystemColors.MetallicSilver)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OverviewMetric("48H LOAD", "${rec.measuredLoadMinutes48h}m", Modifier.weight(1f))
                    OverviewMetric("7D SESSIONS", "${rec.measuredSessions7d}", Modifier.weight(1f))
                    OverviewMetric("STREAK DAYS", "${rec.consecutiveActiveDays}d", Modifier.weight(1f))
                }

                Text(
                    text = "Measured Sleep: ${rec.measuredSleepHoursLastNight?.let { "${"%.1f".format(it)}h" } ?: "Not available"} • " +
                        "Resting HR: ${rec.measuredRestingHrBpm?.let { "$it bpm" } ?: "Not available"} • " +
                        "Self-Reported Fatigue: ${rec.selfReportedFatigue?.let { "$it/5" } ?: "Not logged"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SystemColors.MutedSilver
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onOpenCheckIn,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("open_recovery_checkin_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SystemColors.ElectricCyan,
                            contentColor = SystemColors.ObsidianBlack
                        )
                    ) {
                        Text("REPORT FATIGUE", style = MaterialTheme.typography.labelMedium)
                    }
                    OutlinedButton(
                        onClick = onMarkRestDay,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("rest_day_button")
                    ) {
                        Icon(Icons.Rounded.Bedtime, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("REST DAY", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.SystemBlue) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("RECOMMENDED ACTIONS", style = MaterialTheme.typography.titleMedium, color = SystemColors.SystemBlue)
                rec.recommendedActions.forEach { action ->
                    Text("• $action", style = MaterialTheme.typography.bodySmall, color = SystemColors.PureIce)
                }
            }
        }
    }
}

@Composable
private fun HealthConnectDashboardTab(
    state: RealityOsUiState.Ready,
    timeFormatter: SimpleDateFormat,
    onRequestPermissions: () -> Unit,
    onOpenSettings: () -> Unit,
    onSyncNow: () -> Unit
) {
    val bundle = state.physicalBundle
    val perms = bundle.healthConnectPermissions
    val sync = bundle.healthConnectSyncState
    val isAvailable = bundle.healthConnectAvailability == HealthConnectSdkAvailability.AVAILABLE

    HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.ElectricCyan) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.HealthAndSafety, contentDescription = null, tint = SystemColors.ElectricCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("GOOGLE HEALTH CONNECT", style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
                    Text(bundle.healthConnectAvailability.label, style = MaterialTheme.typography.labelSmall, color = SystemColors.ElectricCyan)
                }
            }

            Text(bundle.healthConnectAvailability.detail, style = MaterialTheme.typography.bodySmall, color = SystemColors.MetallicSilver)

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                PermissionRow("Exercise Sessions (Required)", perms.exerciseReadGranted)
                PermissionRow("Distance Records", perms.distanceReadGranted)
                PermissionRow("Heart Rate Telemetry", perms.heartRateReadGranted)
                PermissionRow("Sleep Recovery Sessions", perms.sleepReadGranted)
            }

            Text(
                text = "Last Sync: ${sync.lastSyncEpochMs?.let { timeFormatter.format(Date(it)) } ?: "Never"} • ${sync.lastSyncStatus}",
                style = MaterialTheme.typography.labelSmall,
                color = SystemColors.MutedSilver
            )
            if (sync.lastErrorMessage != null) {
                Text(sync.lastErrorMessage, style = MaterialTheme.typography.bodySmall, color = SystemColors.WarningAmber)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onRequestPermissions,
                    enabled = isAvailable,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("grant_hc_permissions_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SystemColors.ElectricCyan,
                        contentColor = SystemColors.ObsidianBlack
                    )
                ) {
                    Icon(Icons.Rounded.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("PERMISSIONS", style = MaterialTheme.typography.labelMedium)
                }

                Button(
                    onClick = onSyncNow,
                    enabled = isAvailable && !bundle.isSyncingHealthConnect,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sync_health_connect_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SystemColors.VerifiedEmerald,
                        contentColor = SystemColors.ObsidianBlack
                    )
                ) {
                    if (bundle.isSyncingHealthConnect) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = SystemColors.ObsidianBlack)
                    } else {
                        Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SYNC NOW", style = MaterialTheme.typography.labelMedium)
                }
            }

            OutlinedButton(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("MANAGE IN HEALTH CONNECT SETTINGS", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = SystemColors.PureIce)
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (granted) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = "Granted", tint = SystemColors.VerifiedEmerald, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = if (granted) "GRANTED" else "MISSING",
                style = MaterialTheme.typography.labelSmall,
                color = if (granted) SystemColors.VerifiedEmerald else SystemColors.WarningAmber
            )
        }
    }
}

@Composable
private fun OverviewMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = SystemColors.ObsidianBlack.copy(alpha = 0.65f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(0.75.dp, SystemColors.SubtleGridLine)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = SystemColors.MutedSilver)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
        }
    }
}

private fun recoveryColor(state: RecoveryState) = when (state) {
    RecoveryState.READY -> SystemColors.VerifiedEmerald
    RecoveryState.MODERATE -> SystemColors.ElectricCyan
    RecoveryState.RECOVERY_RECOMMENDED -> SystemColors.WarningAmber
    RecoveryState.REST_RECOMMENDED -> SystemColors.DangerCrimson
}
