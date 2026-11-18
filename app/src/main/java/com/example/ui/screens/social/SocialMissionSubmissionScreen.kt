package com.example.ui.screens.social

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.engine.SocialMissionCatalog
import com.example.domain.model.SocialMissionEvidenceType
import com.example.ui.viewmodel.SocialMasteryViewModel

private val ObsidianBlack = Color(0xFF070A13)
private val DeepNavy = Color(0xFF0D1424)
private val SurfaceNavy = Color(0xFF1E293B)
private val ElectricCyan = Color(0xFF00E5FF)
private val ArcaneViolet = Color(0xFF8B5CF6)
private val BorderCyan = Color(0x3300E5FF)
private val TextWhite = Color(0xFFF8FAFC)
private val TextMuted = Color(0xFF94A3B8)
private val GoldHighlight = Color(0xFFFFD700)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialMissionSubmissionScreen(
    missionId: String,
    viewModel: SocialMasteryViewModel,
    onNavigateBack: () -> Unit,
    onSubmissionSuccess: () -> Unit
) {
    val mission = SocialMissionCatalog.getMissionById(missionId)
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    val activeRecord = uiState.activeMissionRecords.find { it.missionId == missionId }

    var selectedEvidenceType by remember { mutableStateOf(SocialMissionEvidenceType.SELF_REPORTED) }
    var reflectionText by remember { mutableStateOf("") }
    var humanConfirmingRole by remember { mutableStateOf("") }
    var artifactNote by remember { mutableStateOf("") }
    var attachedArtifactUri by remember { mutableStateOf<Uri?>(null) }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            attachedArtifactUri = uri
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SUBMIT MISSION EVIDENCE",
                        color = TextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ElectricCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianBlack)
            )
        },
        containerColor = ObsidianBlack
    ) { paddingValues ->
        if (mission == null || activeRecord == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No active mission record found. Please accept the mission first.",
                    color = TextMuted,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Mission Header
                Card(
                    colors = CardDefaults.cardColors(containerColor = DeepNavy),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = mission.difficulty.label,
                            color = ArcaneViolet,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = mission.title,
                            color = TextWhite,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Privacy Guarantees Notice
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceNavy),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Privacy Guarantee: Never submit private third-party data, secret recordings, or phone numbers. Self-reflections remain strictly local.",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }

                // Evidence Category Selector
                Text(
                    text = "SELECT EVIDENCE CATEGORY",
                    color = ElectricCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                SocialMissionEvidenceType.entries.forEach { type ->
                    val isSelected = selectedEvidenceType == type
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) DeepNavy else SurfaceNavy)
                            .border(1.dp, if (isSelected) ElectricCyan else Color.Transparent, RoundedCornerShape(8.dp))
                            .testTag("evidence_type_${type.name.lowercase()}")
                            .clickable { selectedEvidenceType = type }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) ElectricCyan else TextMuted)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = type.label,
                                color = TextWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = type.explanation,
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Verification Tier: ${type.verificationTier.name.replace("_", " ")}",
                                color = if (type.isIndependentlyVerified) ElectricCyan else GoldHighlight,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Conditional inputs based on evidence type
                if (selectedEvidenceType == SocialMissionEvidenceType.HUMAN_CONFIRMATION_REPORTED ||
                    selectedEvidenceType == SocialMissionEvidenceType.HUMAN_CONFIRMED) {
                    OutlinedTextField(
                        value = humanConfirmingRole,
                        onValueChange = { humanConfirmingRole = it },
                        label = { Text("Confirming Person's Role (e.g. Mentor, Teammate)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("human_confirming_role_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = BorderCyan,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        )
                    )
                }

                if (selectedEvidenceType == SocialMissionEvidenceType.ARTIFACT_DESCRIBED ||
                    selectedEvidenceType == SocialMissionEvidenceType.ARTIFACT_ATTACHED) {
                    OutlinedTextField(
                        value = artifactNote,
                        onValueChange = { artifactNote = it },
                        label = {
                            Text(
                                if (selectedEvidenceType == SocialMissionEvidenceType.ARTIFACT_DESCRIBED) {
                                    "Artifact Note / Title (History only — does not unlock ADVANCED)"
                                } else {
                                    "Artifact Description (Optional context for attached file)"
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("artifact_note_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = BorderCyan,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite
                        )
                    )
                }

                if (selectedEvidenceType == SocialMissionEvidenceType.ARTIFACT_ATTACHED) {
                    OutlinedButton(
                        onClick = {
                            documentPickerLauncher.launch(arrayOf("*/*"))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("artifact_attach_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricCyan)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Attach real-world evidence file",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (attachedArtifactUri != null) {
                                "FILE SELECTED: ${attachedArtifactUri?.lastPathSegment ?: "Document"} ✓"
                            } else {
                                "SELECT & ATTACH EVIDENCE FILE (REQUIRED)"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (attachedArtifactUri == null) {
                        Text(
                            text = "Note-only descriptions cannot qualify as attached evidence. Attach a readable local file or switch to ARTIFACT_DESCRIBED.",
                            color = GoldHighlight,
                            fontSize = 11.sp
                        )
                    }
                }

                // Written Reflection
                Text(
                    text = "STRUCTURED REFLECTION",
                    color = ElectricCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                OutlinedTextField(
                    value = reflectionText,
                    onValueChange = { reflectionText = it },
                    placeholder = {
                        Text(
                            "Describe what you did, the other person's response, challenges faced, and what you would refine next time...",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = BorderCyan,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val charCount = reflectionText.trim().length
                    val meetsRequirement = charCount >= 20
                    Text(
                        text = "Chars: $charCount / 20 min ${if (meetsRequirement) "✓" else "(Needs 20+ chars)"}",
                        color = if (meetsRequirement) ElectricCyan else GoldHighlight,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    OutlinedButton(
                        onClick = { viewModel.requestAiReflectionReview(mission, reflectionText) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldHighlight)
                    ) {
                        Text("REQUEST AI ADVISORY", fontSize = 10.sp)
                    }
                }

                uiState.missionSubmissionMessage?.let { statusMessage ->
                    Text(
                        text = statusMessage,
                        color = if (statusMessage.startsWith("Submission failed", ignoreCase = true)) GoldHighlight else ElectricCyan,
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                val hasRequiredEvidenceInputs = when (selectedEvidenceType) {
                    SocialMissionEvidenceType.ARTIFACT_ATTACHED -> attachedArtifactUri != null
                    SocialMissionEvidenceType.ARTIFACT_DESCRIBED -> artifactNote.trim().isNotEmpty()
                    else -> true
                }

                Button(
                    onClick = {
                        viewModel.submitMissionReflection(
                            recordId = activeRecord.recordId,
                            evidenceType = selectedEvidenceType,
                            reflectionText = reflectionText,
                            humanConfirmingRole = humanConfirmingRole.ifBlank { null },
                            artifactNote = artifactNote.ifBlank { null },
                            artifactAttachmentUri = attachedArtifactUri?.toString(),
                            onSuccess = onSubmissionSuccess
                        )
                    },
                    enabled = reflectionText.trim().length >= 20 && hasRequiredEvidenceInputs && !uiState.isSubmittingMission,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("submit_mission_reflection_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan, contentColor = ObsidianBlack)
                ) {
                    if (uiState.isSubmittingMission) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = ObsidianBlack)
                    } else {
                        Text("SUBMIT MISSION REFLECTION", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
