package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import com.example.domain.model.ExerciseRecord
import com.example.domain.model.WorkoutCategory
import com.example.ui.components.HolographicPanel
import com.example.ui.screens.physical.components.HealthConnectSection
import com.example.ui.screens.physical.components.JoggingDashboardSection
import com.example.ui.screens.physical.components.PhysicalCommandHeader
import com.example.ui.screens.physical.components.PhysicalOverviewDashboard
import com.example.ui.screens.physical.components.PhysicalSubTab
import com.example.ui.screens.physical.components.RecoveryCentreSection
import com.example.ui.screens.physical.components.WorkoutDashboardSection
import com.example.ui.theme.SystemColors
import com.example.ui.theme.SystemSpacing
import com.example.ui.viewmodel.RealityOsUiState
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
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
    val player = state.player
    val timeFormatter = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SystemColors.ObsidianBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = SystemSpacing.maxContentWidth)
                .testTag("physical_development_screen"),
            contentPadding = PaddingValues(
                horizontal = SystemSpacing.screenHorizontal,
                vertical = SystemSpacing.screenVertical
            ),
            verticalArrangement = Arrangement.spacedBy(SystemSpacing.sectionSpacing)
        ) {
            // 1. COMMAND HEADER & TACTICAL NAVIGATION
            item {
                PhysicalCommandHeader(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    recoveryState = bundle.recoveryAssessment.state,
                    physicalPowerScore = physicalPowerScore,
                    activeDays7d = activeDays7d
                )
            }

            // 2. SYNC FEEDBACK BANNER (IF PRESENT)
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
                                style = MaterialTheme.typography.bodyMedium,
                                color = SystemColors.PureIce,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onClearFeedback) {
                                Text("OK", style = MaterialTheme.typography.labelMedium, color = SystemColors.ElectricCyan)
                            }
                        }
                    }
                }
            }

            // 3. TAB CONTENT
            when (selectedTab) {
                PhysicalSubTab.OVERVIEW -> {
                    item {
                        PhysicalOverviewDashboard(
                            state = state,
                            onOpenJogging = { selectedTab = PhysicalSubTab.JOGGING },
                            onOpenWorkouts = { selectedTab = PhysicalSubTab.WORKOUTS },
                            onOpenRecovery = { selectedTab = PhysicalSubTab.RECOVERY },
                            onOpenHealthConnect = { selectedTab = PhysicalSubTab.HEALTH_CONNECT },
                            onStartJoggingLive = {
                                if (!bundle.activeJoggingSession.isActive) {
                                    onStartActiveJogging()
                                }
                                showActiveJogSheet = true
                            },
                            onOpenLiveJogSheet = { showActiveJogSheet = true },
                            onStartWorkoutCategory = { cat -> workoutCategoryToOpen = cat },
                            physicalPowerScore = physicalPowerScore,
                            activeDays7d = activeDays7d
                        )
                    }
                }

                PhysicalSubTab.JOGGING -> {
                    item {
                        JoggingDashboardSection(
                            completedMinutesToday = state.physicalState.completedJoggingMinutesToday,
                            effectiveTargetMinutes = state.physicalState.effectiveJoggingTarget,
                            progressFraction = state.physicalState.joggingProgressFraction,
                            joggingSessions = bundle.joggingSessions,
                            pendingProposal = bundle.pendingJoggingProposal,
                            timeFormatter = timeFormatter,
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
                }

                PhysicalSubTab.WORKOUTS -> {
                    item {
                        WorkoutDashboardSection(
                            advisoryCategory = bundle.advisoryWorkoutCategory,
                            recoveryState = bundle.recoveryAssessment.state,
                            workoutSessions = bundle.workoutSessions,
                            timeFormatter = timeFormatter,
                            onSelectCategoryToStart = { cat -> workoutCategoryToOpen = cat }
                        )
                    }
                }

                PhysicalSubTab.RECOVERY -> {
                    item {
                        RecoveryCentreSection(
                            recovery = bundle.recoveryAssessment,
                            onOpenCheckIn = { showRecoveryCheckInSheet = true },
                            onMarkRestDay = onMarkRestDay
                        )
                    }
                }

                PhysicalSubTab.HEALTH_CONNECT -> {
                    item {
                        HealthConnectSection(
                            availability = bundle.healthConnectAvailability,
                            permissions = bundle.healthConnectPermissions,
                            syncState = bundle.healthConnectSyncState,
                            isSyncing = bundle.isSyncingHealthConnect,
                            importedActivities = bundle.importedHealthActivities,
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
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // ACTIVE SESSIONS & MODAL BOTTOM SHEETS
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
