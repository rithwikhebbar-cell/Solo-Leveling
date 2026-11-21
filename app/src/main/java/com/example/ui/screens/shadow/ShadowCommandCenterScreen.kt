package com.example.ui.screens.shadow

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.FormationTacticalAssessment
import com.example.domain.model.MissionSuitabilityEvaluation
import com.example.domain.model.MissionTacticalRequirement
import com.example.domain.model.MissionType
import com.example.domain.model.Shadow
import com.example.domain.model.ShadowDeploymentStatus
import com.example.domain.model.ShadowSquad
import com.example.domain.model.ShadowSquadCategory
import com.example.domain.model.ShadowTacticalAnalytics
import com.example.domain.model.ShadowTacticalRole
import com.example.domain.model.SquadDetail
import com.example.domain.model.TacticalAlert
import com.example.domain.model.TacticalAlertSeverity
import com.example.ui.components.HolographicPanel
import com.example.ui.viewmodel.ShadowRealmViewModel
import java.util.Locale

// ============================================================================
// PHASE 6.5 — SHADOW MONARCH COMMAND CENTER SCREEN
// Premium Tactical Military Interface with Solo Leveling Aesthetic
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShadowCommandCenterScreen(
    viewModel: ShadowRealmViewModel,
    onNavigateBack: () -> Unit,
    onSelectShadow: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val allSquads by viewModel.allSquads.collectAsState()
    val activeSquad by viewModel.activeSquad.collectAsState()
    val formationAssessment by viewModel.formationAssessment.collectAsState()
    val availableMissions by viewModel.availableMissions.collectAsState()
    val selectedMissionEval by viewModel.selectedMissionEvaluation.collectAsState()
    val tacticalAnalytics by viewModel.tacticalAnalytics.collectAsState()
    val tacticalAlerts by viewModel.tacticalAlerts.collectAsState()

    var showCreateSquadDialog by remember { mutableStateOf(false) }
    var selectedMissionForDeploy by remember { mutableStateOf<MissionTacticalRequirement?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadTacticalCommandCenter()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Security, contentDescription = null, tint = ShadowColors.Cyan, modifier = Modifier.size(20.dp))
                        Text(
                            text = "COMMAND CENTER",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 2.sp
                            ),
                            color = ShadowColors.PureIce
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = ShadowColors.PureIce)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ShadowColors.Obsidian)
            )
        },
        containerColor = ShadowColors.Obsidian
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // A. Army Readiness & Tactical Overview Card
            ArmyTacticalOverviewCard(
                allShadows = uiState.allShadows,
                allSquads = allSquads,
                activeFormationAssessment = formationAssessment
            )

            // B. Tactical Alerts Banner
            if (tacticalAlerts.isNotEmpty()) {
                TacticalAlertsSection(alerts = tacticalAlerts)
            }

            // C. Squad Management Deck
            SquadManagementDeck(
                squads = allSquads,
                onCreateSquadClick = { showCreateSquadDialog = true },
                onActivateSquad = { viewModel.setActiveSquad(it) },
                onDuplicateSquad = { viewModel.duplicateSquad(it) },
                onDeleteSquad = { viewModel.deleteSquad(it) },
                onSelectShadow = onSelectShadow
            )

            // D. Formation Intelligence Deck
            FormationIntelligenceDeck(assessment = formationAssessment)

            // E. Mission Command Deck
            MissionCommandDeck(
                missions = availableMissions,
                selectedEvaluation = selectedMissionEval,
                squads = allSquads.map { it.squad },
                onSelectMission = { mission, squad ->
                    viewModel.evaluateMission(mission, squad)
                },
                onDeployClick = { mission ->
                    selectedMissionForDeploy = mission
                }
            )

            // F. Tactical Performance Analytics
            TacticalAnalyticsCard(analytics = tacticalAnalytics)
        }
    }

    // Dialog: Create Squad
    if (showCreateSquadDialog) {
        CreateSquadDialog(
            availableShadows = uiState.allShadows,
            onDismiss = { showCreateSquadDialog = false },
            onConfirm = { name, category, memberIds, leaderId ->
                viewModel.createSquad(name, category, memberIds, leaderId)
                showCreateSquadDialog = false
            }
        )
    }

    // Dialog: Confirm Mission Deployment
    if (selectedMissionForDeploy != null) {
        val mission = selectedMissionForDeploy!!
        val eval = selectedMissionEval
        val squad = allSquads.find { it.squad.squadId == eval?.recommendedSquadId }
        val shadowIds = squad?.squad?.memberShadowIds ?: emptyList()

        AlertDialog(
            onDismissRequest = { selectedMissionForDeploy = null },
            title = { Text("Deploy Squad to ${mission.missionTitle}", color = ShadowColors.PureIce) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Difficulty: ${mission.difficultyRank} | Recommended PWR: ${mission.recommendedPower}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ShadowColors.Cyan
                    )
                    Text(
                        text = "Assigned Squad: ${squad?.squad?.name ?: "Autonomous Strike Force"}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = ShadowColors.PureIce
                    )
                    Text(
                        text = "Deploying ${shadowIds.size} shadows through the authoritative Shadow Deployment Engine.",
                        style = MaterialTheme.typography.bodySmall,
                        color = ShadowColors.Muted
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.confirmAndDeployMission(
                            missionId = mission.missionId,
                            missionType = mission.missionType,
                            squadId = squad?.squad?.squadId,
                            shadowIdsToDeploy = shadowIds
                        )
                        selectedMissionForDeploy = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ShadowColors.Cyan, contentColor = ShadowColors.Obsidian)
                ) {
                    Text("AUTHORIZE DEPLOYMENT")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { selectedMissionForDeploy = null }) {
                    Text("CANCEL")
                }
            },
            containerColor = ShadowColors.PanelSurface
        )
    }
}

// ============================================================================
// COMPONENT SECTIONS
// ============================================================================

@Composable
fun ArmyTacticalOverviewCard(
    allShadows: List<Shadow>,
    allSquads: List<SquadDetail>,
    activeFormationAssessment: FormationTacticalAssessment?,
    modifier: Modifier = Modifier
) {
    val total = allShadows.size
    val deployed = allShadows.count { it.deploymentStatus == ShadowDeploymentStatus.DEPLOYED }
    val available = allShadows.count { it.deploymentStatus == ShadowDeploymentStatus.AVAILABLE && !it.isRetired }
    val assignedToSquads = allSquads.flatMap { it.squad.memberShadowIds }.distinct().size
    val activeSquadsCount = allSquads.count { it.squad.isActive }
    val formationName = activeFormationAssessment?.formationName ?: "None"
    val strength = activeFormationAssessment?.strengthScore ?: 0

    HolographicPanel(
        accentColor = ShadowColors.Cyan,
        secondaryColor = ShadowColors.Violet,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ARMY READINESS & COMMAND OVERVIEW",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = ShadowColors.Cyan
                )
                Surface(
                    color = ShadowColors.Violet.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, ShadowColors.Violet)
                ) {
                    Text(
                        text = "READINESS: $strength%",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                        color = ShadowColors.Gold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Metrics grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Total Army", style = MaterialTheme.typography.labelSmall, color = ShadowColors.Muted)
                    Text("$total Shadows", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = ShadowColors.PureIce)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Available", style = MaterialTheme.typography.labelSmall, color = ShadowColors.Muted)
                    Text("$available", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = ShadowColors.Emerald)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Deployed", style = MaterialTheme.typography.labelSmall, color = ShadowColors.Muted)
                    Text("$deployed", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = ShadowColors.Cyan)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("In Squads", style = MaterialTheme.typography.labelSmall, color = ShadowColors.Muted)
                    Text("$assignedToSquads", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = ShadowColors.Gold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = ShadowColors.PanelSurfaceElevated, thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "Active Formation: $formationName",
                    style = MaterialTheme.typography.bodySmall,
                    color = ShadowColors.PureIce
                )
                Text(
                    text = "Active Squads: $activeSquadsCount",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = ShadowColors.Cyan
                )
            }
        }
    }
}

@Composable
fun TacticalAlertsSection(
    alerts: List<TacticalAlert>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "TACTICAL ALERTS (${alerts.size})",
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            color = ShadowColors.Muted
        )
        alerts.forEach { alert ->
            val color = when (alert.severity) {
                TacticalAlertSeverity.CRITICAL -> ShadowColors.Crimson
                TacticalAlertSeverity.HIGH -> ShadowColors.Amber
                TacticalAlertSeverity.WARNING -> ShadowColors.Gold
                TacticalAlertSeverity.INFO -> ShadowColors.Cyan
            }
            Surface(
                color = color.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Warning, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = alert.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = ShadowColors.PureIce)
                        Text(text = alert.message, style = MaterialTheme.typography.bodySmall, color = ShadowColors.Muted)
                    }
                }
            }
        }
    }
}

@Composable
fun SquadManagementDeck(
    squads: List<SquadDetail>,
    onCreateSquadClick: () -> Unit,
    onActivateSquad: (String) -> Unit,
    onDuplicateSquad: (String) -> Unit,
    onDeleteSquad: (String) -> Unit,
    onSelectShadow: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TACTICAL SQUADS (${squads.size})",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                color = ShadowColors.Muted
            )
            Button(
                onClick = onCreateSquadClick,
                colors = ButtonDefaults.buttonColors(containerColor = ShadowColors.Cyan, contentColor = ShadowColors.Obsidian)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("CREATE SQUAD", fontSize = 11.sp)
            }
        }

        if (squads.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = ShadowColors.PanelSurface),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, ShadowColors.PanelSurfaceElevated),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No tactical squads created yet.\nForm assault, defense, or ranged squads to coordinate operations.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ShadowColors.Muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(24.dp)
                )
            }
        } else {
            squads.forEach { detail ->
                SquadCard(
                    detail = detail,
                    onActivate = { onActivateSquad(detail.squad.squadId) },
                    onDuplicate = { onDuplicateSquad(detail.squad.squadId) },
                    onDelete = { onDeleteSquad(detail.squad.squadId) },
                    onSelectShadow = onSelectShadow
                )
            }
        }
    }
}

@Composable
fun SquadCard(
    detail: SquadDetail,
    onActivate: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onSelectShadow: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val squad = detail.squad
    val synergy = detail.synergyReport

    Card(
        colors = CardDefaults.cardColors(containerColor = ShadowColors.PanelSurface),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (squad.isActive) ShadowColors.Cyan else ShadowColors.PanelSurfaceElevated),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = squad.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = ShadowColors.PureIce
                        )
                        if (squad.isActive) {
                            Surface(color = ShadowColors.Emerald.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                                Text("ACTIVE", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp), color = ShadowColors.Emerald, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }
                    }
                    Text(
                        text = squad.category.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = ShadowColors.Cyan
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Synergy: ${synergy.synergyScore}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                        color = ShadowColors.Gold
                    )
                    Text(
                        text = "Leader: ${detail.leader?.name ?: "None"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ShadowColors.Muted
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Member chips with roles
            Text("MEMBERS & ROLES", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = ShadowColors.Muted)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                detail.members.forEach { m ->
                    Surface(
                        color = ShadowColors.PanelSurfaceElevated,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, if (m.isLeader) ShadowColors.Gold else ShadowColors.Cyan.copy(alpha = 0.4f)),
                        modifier = Modifier.clickable { onSelectShadow(m.shadow.shadowId) }
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text(
                                text = m.shadow.name + if (m.isLeader) " (★)" else "",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                color = if (m.isLeader) ShadowColors.Gold else ShadowColors.PureIce
                            )
                            Text(
                                text = m.assignedRole.name,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontFamily = FontFamily.Monospace),
                                color = ShadowColors.Cyan
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = ShadowColors.PanelSurfaceElevated, thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Action row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!squad.isActive) {
                    Button(
                        onClick = onActivate,
                        colors = ButtonDefaults.buttonColors(containerColor = ShadowColors.Violet),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("SET ACTIVE", fontSize = 11.sp)
                    }
                }
                OutlinedButton(
                    onClick = onDuplicate,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ShadowColors.Cyan),
                    border = BorderStroke(1.dp, ShadowColors.Cyan.copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("DUPLICATE", fontSize = 11.sp)
                }
                OutlinedButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ShadowColors.Crimson),
                    border = BorderStroke(1.dp, ShadowColors.Crimson.copy(alpha = 0.5f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("DELETE", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun FormationIntelligenceDeck(
    assessment: FormationTacticalAssessment?,
    modifier: Modifier = Modifier
) {
    val a = assessment ?: return

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("FORMATION INTELLIGENCE", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = ShadowColors.Muted)

        Card(
            colors = CardDefaults.cardColors(containerColor = ShadowColors.PanelSurface),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, ShadowColors.Violet.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(a.formationName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = ShadowColors.PureIce)
                        Text(a.tacticalBalance, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = ShadowColors.Cyan)
                    }
                    Text("Strength: ${a.strengthScore}/100", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold), color = ShadowColors.Gold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Role coverage badges
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    a.roleCoverage.forEach { (role, count) ->
                        Surface(
                            color = if (count > 0) ShadowColors.Cyan.copy(alpha = 0.15f) else ShadowColors.PanelSurfaceElevated,
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, if (count > 0) ShadowColors.Cyan else ShadowColors.Muted.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "${role.name}: $count",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = if (count > 0) ShadowColors.PureIce else ShadowColors.Muted,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (a.weaknessDetection.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("TACTICAL WEAKNESSES:", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = ShadowColors.Amber)
                    a.weaknessDetection.forEach { w ->
                        Text("• $w", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = ShadowColors.Muted)
                    }
                }
            }
        }
    }
}

@Composable
fun MissionCommandDeck(
    missions: List<MissionTacticalRequirement>,
    selectedEvaluation: MissionSuitabilityEvaluation?,
    squads: List<ShadowSquad>,
    onSelectMission: (MissionTacticalRequirement, ShadowSquad?) -> Unit,
    onDeployClick: (MissionTacticalRequirement) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("MISSION TACTICAL COMMAND (${missions.size})", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = ShadowColors.Muted)

        missions.forEach { mission ->
            val isEvaluated = selectedEvaluation?.mission?.missionId == mission.missionId

            Card(
                colors = CardDefaults.cardColors(containerColor = ShadowColors.PanelSurface),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if (isEvaluated) ShadowColors.Cyan else ShadowColors.PanelSurfaceElevated),
                modifier = Modifier.fillMaxWidth().clickable {
                    onSelectMission(mission, squads.firstOrNull())
                }
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text(mission.missionTitle, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = ShadowColors.PureIce)
                            Text("Difficulty: ${mission.difficultyRank} | Power Req: ${mission.recommendedPower}", style = MaterialTheme.typography.labelSmall, color = ShadowColors.Cyan)
                        }
                        Button(
                            onClick = { onDeployClick(mission) },
                            colors = ButtonDefaults.buttonColors(containerColor = ShadowColors.Cyan, contentColor = ShadowColors.Obsidian)
                        ) {
                            Text("DEPLOY", fontSize = 11.sp)
                        }
                    }

                    if (isEvaluated && selectedEvaluation != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = ShadowColors.PanelSurfaceElevated, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Suitability: ${selectedEvaluation.suitabilityScore}% | Match: ${selectedEvaluation.recommendedSquadName ?: "None"}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold),
                            color = ShadowColors.Gold
                        )
                        Text(
                            text = selectedEvaluation.tacticalAdvice,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = ShadowColors.PureIce
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TacticalAnalyticsCard(
    analytics: ShadowTacticalAnalytics?,
    modifier: Modifier = Modifier
) {
    val a = analytics ?: return

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("TACTICAL PERFORMANCE ANALYTICS", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), color = ShadowColors.Muted)

        Card(
            colors = CardDefaults.cardColors(containerColor = ShadowColors.PanelSurface),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, ShadowColors.PanelSurfaceElevated),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Total Deployments", style = MaterialTheme.typography.labelSmall, color = ShadowColors.Muted)
                        Text("${a.totalDeployments}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = ShadowColors.PureIce)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Success Rate", style = MaterialTheme.typography.labelSmall, color = ShadowColors.Muted)
                        Text("${"%.1f".format(a.deploymentSuccessRate * 100)}%", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = ShadowColors.Emerald)
                    }
                }

                if (a.mostDeployedShadows.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("MOST DEPLOYED SHADOWS", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = ShadowColors.Muted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        a.mostDeployedShadows.forEach { (name, count) ->
                            Surface(
                                color = ShadowColors.PanelSurfaceElevated,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text("$name: $count", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = ShadowColors.Cyan, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreateSquadDialog(
    availableShadows: List<Shadow>,
    onDismiss: () -> Unit,
    onConfirm: (String, ShadowSquadCategory, List<String>, String?) -> Unit
) {
    var squadName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(ShadowSquadCategory.ASSAULT_SQUAD) }
    var selectedShadowIds by remember { mutableStateOf(setOf<String>()) }
    var selectedLeaderId by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Tactical Squad", color = ShadowColors.PureIce) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = squadName,
                    onValueChange = { squadName = it },
                    label = { Text("Squad Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Squad Category:", style = MaterialTheme.typography.labelSmall, color = ShadowColors.Muted)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ShadowSquadCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.label, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ShadowColors.Cyan.copy(alpha = 0.2f),
                                selectedLabelColor = ShadowColors.Cyan
                            )
                        )
                    }
                }

                Text("Select Members (${selectedShadowIds.size}/5):", style = MaterialTheme.typography.labelSmall, color = ShadowColors.Muted)
                availableShadows.take(15).forEach { shadow ->
                    val isChecked = selectedShadowIds.contains(shadow.shadowId)
                    Surface(
                        color = if (isChecked) ShadowColors.Cyan.copy(alpha = 0.15f) else ShadowColors.PanelSurfaceElevated,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth().clickable {
                            if (isChecked) {
                                selectedShadowIds = selectedShadowIds - shadow.shadowId
                                if (selectedLeaderId == shadow.shadowId) selectedLeaderId = null
                            } else if (selectedShadowIds.size < 5) {
                                selectedShadowIds = selectedShadowIds + shadow.shadowId
                                if (selectedLeaderId == null) selectedLeaderId = shadow.shadowId
                            }
                        }
                    ) {
                        Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(shadow.name, style = MaterialTheme.typography.bodySmall, color = ShadowColors.PureIce)
                            Text(shadow.shadowClass.label, style = MaterialTheme.typography.labelSmall, color = ShadowColors.Muted)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (squadName.isNotBlank()) {
                        onConfirm(squadName, selectedCategory, selectedShadowIds.toList(), selectedLeaderId)
                    }
                },
                enabled = squadName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ShadowColors.Cyan, contentColor = ShadowColors.Obsidian)
            ) {
                Text("CREATE SQUAD")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("CANCEL") }
        },
        containerColor = ShadowColors.PanelSurface
    )
}
