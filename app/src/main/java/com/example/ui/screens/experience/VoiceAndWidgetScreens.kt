package com.example.ui.screens.experience

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.preferences.VoiceCommandPermissions
import com.example.domain.engine.CommandExecutionStatus
import com.example.domain.engine.VoiceCommandCategory
import com.example.domain.engine.VoiceCommandRegistry
import com.example.domain.engine.VoiceInteractionMode
import com.example.domain.model.SpeechInputUiState
import com.example.domain.model.SystemExperiencePreferences
import com.example.domain.model.VoiceEngineAvailability
import com.example.service.SystemSpeechCommandController
import com.example.ui.components.HolographicPanel
import com.example.ui.components.SystemEmptyState
import com.example.ui.components.SystemErrorState
import com.example.ui.components.SystemLoadingView
import com.example.ui.components.SystemSectionTitle
import com.example.ui.theme.SystemColors
import com.example.ui.viewmodel.SystemExperienceUiState
import com.example.widget.DailyProgressWidgetReceiver
import com.example.widget.DailyQuestWidgetReceiver
import com.example.widget.PlayerStatusWidgetReceiver
import com.example.widget.QuickActionWidgetReceiver
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VoiceCommandScreen(
    uiState: SystemExperienceUiState,
    onRetry: () -> Unit,
    onSubmitTranscript: (String) -> Unit,
    onConfirmPending: () -> Unit,
    onDismissPending: () -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier,
    onSelectVoiceMode: (VoiceInteractionMode) -> Unit = {},
    onStopVoice: () -> Unit = {},
    onEndVoiceSession: () -> Unit = {},
    onOpenAiCompanion: () -> Unit = {},
    onOpenVoiceSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    var typedCommand by rememberSaveable { mutableStateOf("") }
    var permissionDeniedNote by rememberSaveable { mutableStateOf<String?>(null) }
    var showMicRationaleDialog by rememberSaveable { mutableStateOf(false) }
    var selectedCategory by rememberSaveable { mutableStateOf(VoiceCommandCategory.PLAYER_INFO) }

    val speechController = remember(context) {
        SystemSpeechCommandController(context) { transcript ->
            onSubmitTranscript(transcript)
        }
    }
    DisposableEffect(speechController) {
        onDispose { speechController.releaseRecognizer() }
    }
    val speechState by speechController.state.collectAsStateWithLifecycle()
    val partialTranscript by speechController.partialTranscript.collectAsStateWithLifecycle()

    val ready = uiState as? SystemExperienceUiState.Ready
    val localeTag = ready?.preferences?.voiceLocaleTag ?: "en-US"

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionDeniedNote = null
            speechController.startListening(localeTag)
        } else {
            permissionDeniedNote = "Microphone permission declined. You can always type commands using the text box below."
        }
    }

    if (showMicRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showMicRationaleDialog = false },
            containerColor = SystemColors.DeepNavy,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Security, contentDescription = null, tint = SystemColors.ElectricCyan)
                    Text("Microphone Permission", color = SystemColors.ElectricCyan)
                }
            },
            text = {
                Text(
                    text = "SOLO LEVELING uses the microphone ONLY when you explicitly tap 'Speak'. Audio is processed on-demand by Android SpeechRecognizer and is never recorded in the background or stored on disk.",
                    color = SystemColors.PureIce,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showMicRationaleDialog = false
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SystemColors.ElectricCyan,
                        contentColor = SystemColors.ObsidianBlack
                    )
                ) {
                    Text("ALLOW MICROPHONE")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMicRationaleDialog = false }) {
                    Text("NOT NOW", color = SystemColors.MutedSilver)
                }
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SystemColors.ObsidianBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        when (uiState) {
            is SystemExperienceUiState.Loading -> SystemLoadingView()
            is SystemExperienceUiState.Error -> Box(Modifier.padding(20.dp)) {
                SystemErrorState(uiState.message, onRetry)
            }
            is SystemExperienceUiState.Ready -> {
                // Gemini Structured Command Confirmation Dialog
                val pendingCmd = uiState.pendingCommandConfirmation
                val pendingLegacy = uiState.pendingVoiceConfirmation
                if (pendingCmd != null) {
                    val paramsText = pendingCmd.pendingIntent?.parameters
                        ?.takeIf { it.isNotEmpty() }
                        ?.entries
                        ?.joinToString("\n") { (k, v) -> "• $k: $v" }
                    AlertDialog(
                        onDismissRequest = onDismissPending,
                        containerColor = SystemColors.DeepNavy,
                        title = {
                            Text("Confirm Voice Command", color = SystemColors.ElectricCyan)
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "${pendingCmd.commandLabel} (${pendingCmd.category?.label ?: "System"})",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = SystemColors.PureIce
                                )
                                Text(
                                    text = pendingCmd.spokenResponse,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SystemColors.MetallicSilver
                                )
                                if (paramsText != null) {
                                    Surface(
                                        color = SystemColors.ObsidianBlack,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(0.75.dp, SystemColors.PanelBorderCyan)
                                    ) {
                                        Text(
                                            text = paramsText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SystemColors.ElectricCyan,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = onConfirmPending,
                                modifier = Modifier.testTag("voice_confirm_action_btn"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SystemColors.ElectricCyan,
                                    contentColor = SystemColors.ObsidianBlack
                                )
                            ) {
                                Text("CONFIRM")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = onDismissPending) {
                                Text("CANCEL", color = SystemColors.MutedSilver)
                            }
                        }
                    )
                } else if (pendingLegacy != null) {
                    AlertDialog(
                        onDismissRequest = onDismissPending,
                        containerColor = SystemColors.DeepNavy,
                        title = {
                            Text("Confirm System Action", color = SystemColors.ElectricCyan)
                        },
                        text = {
                            Text(
                                text = "Execute allowlisted command '${pendingLegacy.action?.label}'?",
                                color = SystemColors.PureIce
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = onConfirmPending,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SystemColors.ElectricCyan,
                                    contentColor = SystemColors.ObsidianBlack
                                )
                            ) {
                                Text("CONFIRM")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = onDismissPending) {
                                Text("CANCEL", color = SystemColors.MutedSilver)
                            }
                        }
                    )
                }

                val isConversationMode = uiState.voiceMode == VoiceInteractionMode.CONVERSATION
                val isSpeaking = uiState.voiceAvailability == VoiceEngineAvailability.SPEAKING

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 760.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SystemSectionTitle(
                            title = "Gemini Voice Command Console",
                            subtitle = "User-initiated speech • Allowlisted execution • Zero background listening",
                            trailing = {
                                Row {
                                    IconButton(onClick = onOpenAiCompanion) {
                                        Icon(
                                            imageVector = Icons.Rounded.AutoAwesome,
                                            contentDescription = "Open AI Companion",
                                            tint = SystemColors.ElectricCyan
                                        )
                                    }
                                    IconButton(onClick = onOpenVoiceSettings) {
                                        Icon(
                                            imageVector = Icons.Rounded.Settings,
                                            contentDescription = "Voice & Command Permissions",
                                            tint = SystemColors.ArcaneViolet
                                        )
                                    }
                                }
                            }
                        )
                    }

                    // Mode Toggle: Command Mode vs Conversation Mode
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = !isConversationMode,
                                onClick = { onSelectVoiceMode(VoiceInteractionMode.COMMAND) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("voice_mode_command"),
                                leadingIcon = {
                                    Icon(Icons.Rounded.Terminal, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                label = { Text("Command Mode") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SystemColors.ElectricCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = SystemColors.ElectricCyan,
                                    containerColor = SystemColors.DeepNavy,
                                    labelColor = SystemColors.MetallicSilver
                                )
                            )
                            FilterChip(
                                selected = isConversationMode,
                                onClick = { onSelectVoiceMode(VoiceInteractionMode.CONVERSATION) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("voice_mode_conversation"),
                                leadingIcon = {
                                    Icon(Icons.Rounded.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                },
                                label = { Text("Conversation Mode") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SystemColors.ArcaneViolet.copy(alpha = 0.22f),
                                    selectedLabelColor = SystemColors.PureIce,
                                    containerColor = SystemColors.DeepNavy,
                                    labelColor = SystemColors.MetallicSilver
                                )
                            )
                        }
                    }

                    // Main Microphone & Live Waveform Panel
                    item {
                        val listening = speechState is SpeechInputUiState.Listening
                        val rmsDb = (speechState as? SpeechInputUiState.Listening)?.rmsDb ?: 0f
                        HolographicPanel(
                            modifier = Modifier.fillMaxWidth(),
                            accentColor = when {
                                listening -> SystemColors.VerifiedEmerald
                                uiState.isProcessingVoice -> SystemColors.ArcaneViolet
                                isSpeaking -> SystemColors.ElectricCyan
                                else -> SystemColors.ElectricCyan
                            }
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                VoiceWaveformIndicator(
                                    isListening = listening,
                                    isProcessing = uiState.isProcessingVoice,
                                    isSpeaking = isSpeaking,
                                    rmsDb = rmsDb,
                                    reducedMotion = uiState.preferences.reducedMotionEnabled
                                )

                                Icon(
                                    imageVector = if (uiState.preferences.voiceCommandsEnabled) Icons.Rounded.Mic else Icons.Rounded.MicOff,
                                    contentDescription = "Microphone control",
                                    tint = if (listening) SystemColors.VerifiedEmerald else SystemColors.ElectricCyan,
                                    modifier = Modifier.size(34.dp)
                                )

                                Text(
                                    text = when (val s = speechState) {
                                        is SpeechInputUiState.Listening -> if (isConversationMode) {
                                            "LISTENING FOR GEMINI CONVERSATION..."
                                        } else {
                                            "LISTENING... SPEAK COMMAND (dB ${s.rmsDb.toInt()})"
                                        }
                                        is SpeechInputUiState.Processing -> "RECOGNIZING SPEECH..."
                                        is SpeechInputUiState.Unavailable -> s.reason
                                        SpeechInputUiState.Idle -> when {
                                            uiState.isProcessingVoice -> "GEMINI INTERPRETING..."
                                            isSpeaking -> "SYSTEM SPEAKING RESPONSE..."
                                            !uiState.preferences.voiceCommandsEnabled -> "Voice commands disabled in System Voice Settings."
                                            isConversationMode -> "TAP TO SPEAK WITH GEMINI AI COMPANION"
                                            else -> "TAP MICROPHONE TO ISSUE A VOICE COMMAND"
                                        }
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = SystemColors.PureIce
                                )

                                if (listening && partialTranscript.isNotBlank()) {
                                    Surface(
                                        color = SystemColors.ObsidianBlack.copy(alpha = 0.7f),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(0.75.dp, SystemColors.VerifiedEmerald)
                                    ) {
                                        Text(
                                            text = "\"$partialTranscript\"",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = SystemColors.VerifiedEmerald,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }

                                if (permissionDeniedNote != null) {
                                    Text(
                                        text = permissionDeniedNote!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SystemColors.WarningAmber
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            if (listening) {
                                                speechController.stopListening()
                                            } else {
                                                onStopVoice()
                                                val hasPerm = ContextCompat.checkSelfPermission(
                                                    context,
                                                    Manifest.permission.RECORD_AUDIO
                                                ) == PackageManager.PERMISSION_GRANTED
                                                if (hasPerm) {
                                                    speechController.startListening(localeTag)
                                                } else {
                                                    showMicRationaleDialog = true
                                                }
                                            }
                                        },
                                        enabled = uiState.preferences.voiceCommandsEnabled && !uiState.isProcessingVoice,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("voice_mic_tap_button"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (listening) SystemColors.DangerCrimson else SystemColors.ElectricCyan,
                                            contentColor = SystemColors.ObsidianBlack
                                        )
                                    ) {
                                        Text(
                                            text = if (listening) "STOP LISTENING" else "TAP TO SPEAK",
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }

                                    if (isSpeaking || uiState.lastSpokenMessage != null) {
                                        OutlinedButton(
                                            onClick = onStopVoice,
                                            modifier = Modifier.testTag("voice_stop_speaking_btn"),
                                            border = BorderStroke(1.dp, SystemColors.WarningAmber)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Stop,
                                                contentDescription = "Stop speaking",
                                                tint = SystemColors.WarningAmber,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text("MUTE", color = SystemColors.WarningAmber)
                                        }
                                    }
                                }

                                if (isConversationMode && uiState.conversationTurns.isNotEmpty()) {
                                    OutlinedButton(
                                        onClick = onEndVoiceSession,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("voice_end_session_btn"),
                                        border = BorderStroke(1.dp, SystemColors.DangerCrimson)
                                    ) {
                                        Text("END CONVERSATION SESSION", color = SystemColors.DangerCrimson)
                                    }
                                }

                                Text(
                                    text = "Privacy Notice: Microphone activates ONLY when tapped. Raw audio is never stored or recorded in the background.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SystemColors.MutedSilver
                                )
                            }
                        }
                    }

                    // Text Fallback / Voice Simulator Input
                    item {
                        HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.ArcaneViolet) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = if (isConversationMode) "TEXT INPUT (CONVERSATION MODE)" else "TEXT FALLBACK COMMAND",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = SystemColors.ElectricCyan
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = typedCommand,
                                        onValueChange = { typedCommand = it },
                                        placeholder = {
                                            Text(
                                                if (isConversationMode) {
                                                    "Ask Gemini anything about your progress..."
                                                } else {
                                                    "e.g. add task review physics notes"
                                                }
                                            )
                                        },
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("voice_fallback_input"),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = SystemColors.ElectricCyan,
                                            unfocusedBorderColor = SystemColors.PanelBorderCyan,
                                            focusedTextColor = SystemColors.PureIce,
                                            unfocusedTextColor = SystemColors.PureIce
                                        )
                                    )
                                    Button(
                                        onClick = {
                                            val clean = typedCommand.trim()
                                            if (clean.isNotEmpty()) {
                                                onSubmitTranscript(clean)
                                                typedCommand = ""
                                            }
                                        },
                                        enabled = typedCommand.isNotBlank() && !uiState.isProcessingVoice,
                                        modifier = Modifier
                                            .height(52.dp)
                                            .testTag("voice_fallback_run_btn"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = SystemColors.ArcaneViolet,
                                            contentColor = SystemColors.PureIce
                                        )
                                    ) {
                                        Text(if (isConversationMode) "ASK" else "RUN")
                                    }
                                }

                                // Rich Command Execution Feedback (Command Mode)
                                if (!isConversationMode) {
                                    val exec = uiState.lastCommandExecution
                                    if (exec != null) {
                                        val statusColor = when (exec.status) {
                                            CommandExecutionStatus.EXECUTED -> SystemColors.VerifiedEmerald
                                            CommandExecutionStatus.AWAITING_CONFIRMATION -> SystemColors.ElectricCyan
                                            CommandExecutionStatus.NEEDS_CLARIFICATION -> SystemColors.WarningAmber
                                            CommandExecutionStatus.BLOCKED_BY_SAFETY,
                                            CommandExecutionStatus.PERMISSION_DENIED,
                                            CommandExecutionStatus.REJECTED,
                                            CommandExecutionStatus.FAILED -> SystemColors.DangerCrimson
                                            CommandExecutionStatus.DUPLICATE_IGNORED -> SystemColors.MutedSilver
                                        }
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("voice_last_execution_card"),
                                            color = statusColor.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, statusColor.copy(alpha = 0.6f))
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(10.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "${exec.commandLabel} • ${exec.status.label}",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = statusColor
                                                    )
                                                    Text(
                                                        text = exec.source.label,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = SystemColors.ElectricCyan
                                                    )
                                                }
                                                Text(
                                                    text = exec.displayDetails,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = SystemColors.PureIce
                                                )
                                            }
                                        }
                                    } else {
                                        uiState.lastVoiceResolution?.let { res ->
                                            Text(
                                                text = res.responseText,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (res.matched) SystemColors.VerifiedEmerald else SystemColors.WarningAmber
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Conversation Mode Turn Feed
                    if (isConversationMode) {
                        item {
                            SystemSectionTitle(
                                title = "Voice Conversation Transcript",
                                subtitle = "Shared with AI System Companion memory • Spoken aloud via System Voice"
                            )
                        }
                        if (uiState.voiceStreamingReply.isNotBlank()) {
                            item {
                                HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.ElectricCyan) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("GEMINI STREAMING...", style = MaterialTheme.typography.labelSmall, color = SystemColors.ElectricCyan)
                                        Text(uiState.voiceStreamingReply, style = MaterialTheme.typography.bodyMedium, color = SystemColors.PureIce)
                                    }
                                }
                            }
                        }
                        if (uiState.conversationTurns.isEmpty() && uiState.voiceStreamingReply.isBlank()) {
                            item {
                                SystemEmptyState(
                                    title = "Conversation Mode Ready",
                                    description = "Tap Speak or type above to converse naturally with Gemini AI."
                                )
                            }
                        } else {
                            items(uiState.conversationTurns.reversed(), key = { it.id }) { turn ->
                                HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.ArcaneViolet, contentPadding = 12.dp) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("YOU: ${turn.userTranscript}", style = MaterialTheme.typography.labelMedium, color = SystemColors.ElectricCyan)
                                        Text(turn.systemReply, style = MaterialTheme.typography.bodySmall, color = SystemColors.PureIce)
                                    }
                                }
                            }
                        }
                    } else {
                        // Categorized Allowlisted Voice Command Catalog (All 8 Categories)
                        item {
                            SystemSectionTitle(
                                title = "Allowlisted Voice Commands (${VoiceCommandRegistry.definitions.size})",
                                subtitle = "Filter by category and tap any phrase to execute safely"
                            )
                        }
                        item {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                VoiceCommandCategory.entries.forEach { cat ->
                                    val permitted = VoiceCommandRegistry.isCategoryPermitted(cat, uiState.voiceCommandPermissions)
                                    FilterChip(
                                        selected = selectedCategory == cat,
                                        onClick = { selectedCategory = cat },
                                        label = {
                                            Text(
                                                text = if (permitted) cat.label else "${cat.label} (Off)",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = SystemColors.ElectricCyan.copy(alpha = 0.2f),
                                            selectedLabelColor = SystemColors.ElectricCyan,
                                            containerColor = SystemColors.DeepNavy,
                                            labelColor = if (permitted) SystemColors.PureIce else SystemColors.MutedSilver
                                        )
                                    )
                                }
                            }
                        }
                        item {
                            val categoryDefs = remember(selectedCategory) {
                                VoiceCommandRegistry.definitions.filter { it.category == selectedCategory }
                            }
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                categoryDefs.forEach { def ->
                                    val phrase = def.examplePhrases.firstOrNull() ?: def.label
                                    FilterChip(
                                        selected = false,
                                        onClick = { onSubmitTranscript(phrase) },
                                        label = { Text("\"$phrase\"", style = MaterialTheme.typography.labelSmall) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            containerColor = SystemColors.DeepNavy,
                                            labelColor = SystemColors.PureIce
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = false,
                                            borderColor = SystemColors.PanelBorderCyan
                                        )
                                    )
                                }
                            }
                        }

                        // History
                        item {
                            SystemSectionTitle(
                                title = "Voice Command History",
                                subtitle = "${uiState.voiceHistory.size} executed entries",
                                trailing = {
                                    if (uiState.voiceHistory.isNotEmpty()) {
                                        TextButton(onClick = onClearHistory) {
                                            Text("CLEAR LOG")
                                        }
                                    }
                                }
                            )
                        }
                        if (uiState.voiceHistory.isEmpty()) {
                            item { SystemEmptyState("No Voice Commands Yet", "Tap Speak or run a fallback command above.") }
                        } else {
                            val fmt = SimpleDateFormat("MMM dd HH:mm", Locale.getDefault())
                            items(uiState.voiceHistory, key = { it.id }) { item ->
                                HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.MutedSilver, contentPadding = 12.dp) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(item.matchedActionName, style = MaterialTheme.typography.titleSmall, color = SystemColors.PureIce)
                                            Text(item.outcome, style = MaterialTheme.typography.labelSmall, color = SystemColors.ElectricCyan)
                                        }
                                        Text(
                                            text = "\"${item.transcript}\" • ${fmt.format(Date(item.executedAtEpochMs))}",
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
    }
}

@Composable
private fun VoiceWaveformIndicator(
    isListening: Boolean,
    isProcessing: Boolean,
    isSpeaking: Boolean,
    rmsDb: Float,
    reducedMotion: Boolean
) {
    val active = isListening || isProcessing || isSpeaking
    val barColor = when {
        isListening -> SystemColors.VerifiedEmerald
        isProcessing -> SystemColors.ArcaneViolet
        isSpeaking -> SystemColors.ElectricCyan
        else -> SystemColors.PanelBorderCyan
    }
    val infiniteTransition = rememberInfiniteTransition(label = "voice_waveform")
    val animPhase by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 520),
            repeatMode = RepeatMode.Reverse
        ),
        label = "waveform_phase"
    )
    val normalizedRms = ((rmsDb + 2f) / 12f).coerceIn(0.2f, 1.0f)

    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(28.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.GraphicEq,
            contentDescription = null,
            tint = barColor,
            modifier = Modifier.size(18.dp)
        )
        val multipliers = listOf(0.45f, 0.85f, 1.0f, 0.7f, 0.95f, 0.6f, 0.4f)
        multipliers.forEachIndexed { idx, base ->
            val dynamicFactor = when {
                !active -> 0.25f
                reducedMotion -> 0.65f
                isListening -> (normalizedRms * base + if (idx % 2 == 0) animPhase * 0.25f else (1f - animPhase) * 0.25f).coerceIn(0.25f, 1.0f)
                else -> (base * if (idx % 2 == 0) animPhase else (1.15f - animPhase)).coerceIn(0.25f, 1.0f)
            }
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height((26f * dynamicFactor).dp)
                    .background(barColor, RoundedCornerShape(2.dp))
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SystemVoiceSettingsScreen(
    uiState: SystemExperienceUiState,
    onRetry: () -> Unit,
    onUpdatePreferences: ((SystemExperiencePreferences) -> SystemExperiencePreferences) -> Unit,
    onTestVoice: () -> Unit,
    onStopVoice: () -> Unit,
    onMarkAnnouncementRead: (Long) -> Unit,
    onMarkAllRead: () -> Unit,
    modifier: Modifier = Modifier,
    onUpdateVoicePermissions: ((VoiceCommandPermissions) -> VoiceCommandPermissions) -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SystemColors.ObsidianBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        when (uiState) {
            is SystemExperienceUiState.Loading -> SystemLoadingView()
            is SystemExperienceUiState.Error -> Box(Modifier.padding(20.dp)) {
                SystemErrorState(uiState.message, onRetry)
            }
            is SystemExperienceUiState.Ready -> {
                val prefs = uiState.preferences
                val voicePerms = uiState.voiceCommandPermissions
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 760.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SystemSectionTitle(
                            title = "System Voice & Announcements",
                            subtitle = "Engine status: ${uiState.voiceAvailability.label}"
                        )
                    }
                    item {
                        HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.ElectricCyan) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                VoicePrefToggle("Enable System Voice (TTS)", prefs.ttsEnabled, "toggle_tts_enabled") { enabled ->
                                    onUpdatePreferences { it.copy(ttsEnabled = enabled) }
                                }
                                VoicePrefToggle("Enable Voice Commands (Mic)", prefs.voiceCommandsEnabled, "toggle_voice_cmds") { enabled ->
                                    onUpdatePreferences { it.copy(voiceCommandsEnabled = enabled) }
                                }
                                Text("Speech Rate: ${"%.2f".format(prefs.ttsSpeechRate)}x", style = MaterialTheme.typography.labelMedium, color = SystemColors.PureIce)
                                Slider(
                                    value = prefs.ttsSpeechRate,
                                    onValueChange = { rate -> onUpdatePreferences { it.copy(ttsSpeechRate = rate) } },
                                    valueRange = 0.6f..1.6f,
                                    colors = SliderDefaults.colors(thumbColor = SystemColors.ElectricCyan, activeTrackColor = SystemColors.ElectricCyan)
                                )
                                Text("Voice Pitch: ${"%.2f".format(prefs.ttsPitch)}", style = MaterialTheme.typography.labelMedium, color = SystemColors.PureIce)
                                Slider(
                                    value = prefs.ttsPitch,
                                    onValueChange = { pitch -> onUpdatePreferences { it.copy(ttsPitch = pitch) } },
                                    valueRange = 0.7f..1.4f,
                                    colors = SliderDefaults.colors(thumbColor = SystemColors.ArcaneViolet, activeTrackColor = SystemColors.ArcaneViolet)
                                )
                                Text("Voice Locale", style = MaterialTheme.typography.labelMedium, color = SystemColors.PureIce)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("en-US", "en-GB", "ja-JP", "ko-KR").forEach { localeOption ->
                                        FilterChip(
                                            selected = prefs.voiceLocaleTag == localeOption,
                                            onClick = { onUpdatePreferences { it.copy(voiceLocaleTag = localeOption) } },
                                            label = { Text(localeOption) },
                                            modifier = Modifier.testTag("voice_locale_$localeOption"),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = SystemColors.ElectricCyan.copy(alpha = 0.2f),
                                                selectedLabelColor = SystemColors.ElectricCyan,
                                                containerColor = SystemColors.DeepNavy,
                                                labelColor = SystemColors.PureIce
                                            )
                                        )
                                    }
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = onTestVoice,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("voice_test_button"),
                                        colors = ButtonDefaults.buttonColors(containerColor = SystemColors.ElectricCyan, contentColor = SystemColors.ObsidianBlack)
                                    ) {
                                        Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("TEST VOICE")
                                    }
                                    OutlinedButton(
                                        onClick = onStopVoice,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("voice_stop_button"),
                                        border = BorderStroke(1.dp, SystemColors.MutedSilver)
                                    ) {
                                        Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(18.dp), tint = SystemColors.PureIce)
                                        Spacer(Modifier.width(6.dp))
                                        Text("STOP", color = SystemColors.PureIce)
                                    }
                                }
                            }
                        }
                    }

                    // Per-Category Voice Command Permissions (Persisted in DataStore)
                    item {
                        HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.VerifiedEmerald) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "VOICE COMMAND PERMISSIONS",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = SystemColors.VerifiedEmerald
                                )
                                Text(
                                    text = "Control which System domains Gemini Voice Commands are permitted to read or trigger.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SystemColors.MutedSilver
                                )
                                VoicePrefToggle(
                                    "Require Confirmation for Mutating Commands",
                                    voicePerms.requireConfirmationForMutations,
                                    "toggle_voice_perm_confirm"
                                ) { v -> onUpdateVoicePermissions { it.copy(requireConfirmationForMutations = v) } }
                                VoicePrefToggle(
                                    "Player Information (Level, XP, Stats)",
                                    voicePerms.allowPlayerInfo,
                                    "toggle_voice_perm_player"
                                ) { v -> onUpdateVoicePermissions { it.copy(allowPlayerInfo = v) } }
                                VoicePrefToggle(
                                    "Daily Management (Tasks & Schedule)",
                                    voicePerms.allowDailyManagement,
                                    "toggle_voice_perm_daily"
                                ) { v -> onUpdateVoicePermissions { it.copy(allowDailyManagement = v) } }
                                VoicePrefToggle(
                                    "Habits & Routines",
                                    voicePerms.allowHabitsAndRoutines,
                                    "toggle_voice_perm_habits"
                                ) { v -> onUpdateVoicePermissions { it.copy(allowHabitsAndRoutines = v) } }
                                VoicePrefToggle(
                                    "Physical Development (Workouts & Recovery)",
                                    voicePerms.allowPhysicalDevelopment,
                                    "toggle_voice_perm_physical"
                                ) { v -> onUpdateVoicePermissions { it.copy(allowPhysicalDevelopment = v) } }
                                VoicePrefToggle(
                                    "Academic Development (Study & Revisions)",
                                    voicePerms.allowAcademicDevelopment,
                                    "toggle_voice_perm_academic"
                                ) { v -> onUpdateVoicePermissions { it.copy(allowAcademicDevelopment = v) } }
                                VoicePrefToggle(
                                    "Missions, Dungeons & Shadow Army",
                                    voicePerms.allowMissionsAndRpg,
                                    "toggle_voice_perm_rpg"
                                ) { v -> onUpdateVoicePermissions { it.copy(allowMissionsAndRpg = v) } }
                                VoicePrefToggle(
                                    "System Control & Navigation",
                                    voicePerms.allowSystemControl,
                                    "toggle_voice_perm_system"
                                ) { v -> onUpdateVoicePermissions { it.copy(allowSystemControl = v) } }
                                VoicePrefToggle(
                                    "AI Conversation & Reports",
                                    voicePerms.allowAiConversation,
                                    "toggle_voice_perm_ai"
                                ) { v -> onUpdateVoicePermissions { it.copy(allowAiConversation = v) } }
                            }
                        }
                    }

                    item {
                        HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.ArcaneViolet) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("SPOKEN ANNOUNCEMENT CATEGORIES", style = MaterialTheme.typography.titleSmall, color = SystemColors.ArcaneViolet)
                                VoicePrefToggle("Quest Updates & Reminders", prefs.ttsSpeakQuestUpdates, "toggle_tts_quests") { v ->
                                    onUpdatePreferences { it.copy(ttsSpeakQuestUpdates = v) }
                                }
                                VoicePrefToggle("Level-Up & Rank Promotions", prefs.ttsSpeakLevelAndRank, "toggle_tts_level_rank") { v ->
                                    onUpdatePreferences { it.copy(ttsSpeakLevelAndRank = v) }
                                }
                                VoicePrefToggle("Dungeon Gates & Boss Raids", prefs.ttsSpeakDungeonsAndBosses, "toggle_tts_dungeons") { v ->
                                    onUpdatePreferences { it.copy(ttsSpeakDungeonsAndBosses = v) }
                                }
                                VoicePrefToggle("Recovery & Safety Warnings", prefs.ttsSpeakRecoveryWarnings, "toggle_tts_recovery") { v ->
                                    onUpdatePreferences { it.copy(ttsSpeakRecoveryWarnings = v) }
                                }
                                VoicePrefToggle("AI System Recommendations", prefs.ttsSpeakRecommendations, "toggle_tts_recs") { v ->
                                    onUpdatePreferences { it.copy(ttsSpeakRecommendations = v) }
                                }
                                VoicePrefToggle("Daily Summary Reports", prefs.ttsSpeakDailySummary, "toggle_tts_summary") { v ->
                                    onUpdatePreferences { it.copy(ttsSpeakDailySummary = v) }
                                }
                            }
                        }
                    }

                    item {
                        SystemSectionTitle(
                            title = "System Announcement Feed",
                            subtitle = "${uiState.announcements.count { !it.isRead }} unread announcements",
                            trailing = {
                                if (uiState.announcements.any { !it.isRead }) {
                                    TextButton(onClick = onMarkAllRead) {
                                        Text("MARK ALL READ")
                                    }
                                }
                            }
                        )
                    }
                    if (uiState.announcements.isEmpty()) {
                        item { SystemEmptyState("No Announcements Recorded", "System events, level-ups, and recovery alerts appear here.") }
                    } else {
                        items(uiState.announcements.take(25), key = { it.id }) { ann ->
                            HolographicPanel(
                                modifier = Modifier.fillMaxWidth(),
                                accentColor = if (ann.isRead) SystemColors.MutedSilver else SystemColors.ElectricCyan,
                                contentPadding = 12.dp
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text("[${ann.channelSpec.displayName.uppercase()}] ${ann.title}", style = MaterialTheme.typography.titleSmall, color = SystemColors.PureIce)
                                        Text(ann.message, style = MaterialTheme.typography.bodySmall, color = SystemColors.MetallicSilver)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (ann.wasSpoken) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
                                                contentDescription = null,
                                                tint = SystemColors.MutedSilver,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = if (ann.wasSpoken) "Spoken aloud" else "Visual only",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SystemColors.MutedSilver
                                            )
                                        }
                                    }
                                    if (!ann.isRead) {
                                        TextButton(onClick = { onMarkAnnouncementRead(ann.id) }) {
                                            Text("ACK")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WidgetHubScreen(
    uiState: SystemExperienceUiState,
    onRetry: () -> Unit,
    onRefreshWidgets: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appWidgetManager = remember(context) { AppWidgetManager.getInstance(context) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SystemColors.ObsidianBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        when (uiState) {
            is SystemExperienceUiState.Loading -> SystemLoadingView()
            is SystemExperienceUiState.Error -> Box(Modifier.padding(20.dp)) {
                SystemErrorState(uiState.message, onRetry)
            }
            is SystemExperienceUiState.Ready -> {
                val snap = uiState.widgetSnapshot
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 760.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SystemSectionTitle(
                            title = "Home-Screen Widgets",
                            subtitle = "4 Jetpack Glance widgets backed by live Room telemetry",
                            trailing = {
                                OutlinedButton(
                                    onClick = onRefreshWidgets,
                                    modifier = Modifier.testTag("widgets_refresh_btn"),
                                    border = BorderStroke(1.dp, SystemColors.ElectricCyan)
                                ) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = "Sync widgets", tint = SystemColors.ElectricCyan, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("SYNC NOW", color = SystemColors.ElectricCyan)
                                }
                            }
                        )
                    }

                    val widgetSpecs = listOf(
                        Triple("Player Status Widget", "${snap.rankDisplayName} Lv.${snap.level} • ${snap.currentLevelXp}/${snap.xpToNextLevel} XP • ${snap.recoveryLabel}", PlayerStatusWidgetReceiver::class.java),
                        Triple("Daily Quest Widget", "${snap.completedDailyQuests}/${snap.totalDailyQuests} cleared • ${snap.pendingDailyQuests.size} pending in queue", DailyQuestWidgetReceiver::class.java),
                        Triple("Daily Progress Widget", "Jog ${snap.todayJoggingMinutes}/${snap.joggingTargetMinutes}m • Study ${snap.todayStudyMinutes}/${snap.academicGoalMinutes}m • Disc ${snap.disciplineScore}/100", DailyProgressWidgetReceiver::class.java),
                        Triple("Quick Action Widget", "Deep links: Focus Timer • Workout Engine • AI Companion • Status", QuickActionWidgetReceiver::class.java)
                    )

                    items(widgetSpecs) { (title, preview, receiverClass) ->
                        HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.ElectricCyan) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Widgets, contentDescription = title, tint = SystemColors.ElectricCyan, modifier = Modifier.size(22.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(title.uppercase(), style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
                                }
                                Text(preview, style = MaterialTheme.typography.bodySmall, color = SystemColors.MetallicSilver)
                                OutlinedButton(
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && appWidgetManager.isRequestPinAppWidgetSupported) {
                                            appWidgetManager.requestPinAppWidget(ComponentName(context, receiverClass), null, null)
                                        }
                                    },
                                    border = BorderStroke(1.dp, SystemColors.ArcaneViolet)
                                ) {
                                    Text("PIN TO HOME SCREEN", color = SystemColors.ArcaneViolet, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VoicePrefToggle(
    label: String,
    checked: Boolean,
    tag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = SystemColors.PureIce, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag),
            colors = SwitchDefaults.colors(checkedThumbColor = SystemColors.ObsidianBlack, checkedTrackColor = SystemColors.ElectricCyan)
        )
    }
}
