package com.example.ui.screens.experience

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import com.example.domain.model.SpeechInputUiState
import com.example.domain.model.SystemExperiencePreferences
import com.example.domain.model.VoiceCommandActionType
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var typedCommand by rememberSaveable { mutableStateOf("") }
    var permissionDeniedNote by rememberSaveable { mutableStateOf<String?>(null) }

    val speechController = remember(context) {
        SystemSpeechCommandController(context) { transcript ->
            onSubmitTranscript(transcript)
        }
    }
    DisposableEffect(speechController) {
        onDispose { speechController.releaseRecognizer() }
    }
    val speechState by speechController.state.collectAsStateWithLifecycle()

    val ready = uiState as? SystemExperienceUiState.Ready
    val localeTag = ready?.preferences?.voiceLocaleTag ?: "en-US"

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionDeniedNote = null
            speechController.startListening(localeTag)
        } else {
            permissionDeniedNote = "Microphone permission declined. You can always type commands using the fallback box below."
        }
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
                if (uiState.pendingVoiceConfirmation != null) {
                    AlertDialog(
                        onDismissRequest = onDismissPending,
                        containerColor = SystemColors.DeepNavy,
                        title = {
                            Text("Confirm System Action", color = SystemColors.ElectricCyan)
                        },
                        text = {
                            Text(
                                text = "Execute allowlisted command '${uiState.pendingVoiceConfirmation.action?.label}'?",
                                color = SystemColors.PureIce
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = onConfirmPending,
                                colors = ButtonDefaults.buttonColors(containerColor = SystemColors.ElectricCyan, contentColor = SystemColors.ObsidianBlack)
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

                LazyColumn(
                    modifier = Modifier.fillMaxSize().widthIn(max = 760.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SystemSectionTitle(
                            title = "Voice Command Console",
                            subtitle = "User-initiated microphone commands • Zero background listening"
                        )
                    }

                    item {
                        val listening = speechState is SpeechInputUiState.Listening
                        HolographicPanel(
                            modifier = Modifier.fillMaxWidth(),
                            accentColor = if (listening) SystemColors.VerifiedEmerald else SystemColors.ElectricCyan
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (uiState.preferences.voiceCommandsEnabled) Icons.Rounded.Mic else Icons.Rounded.MicOff,
                                    contentDescription = "Microphone control",
                                    tint = if (listening) SystemColors.VerifiedEmerald else SystemColors.ElectricCyan,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = when (val s = speechState) {
                                        is SpeechInputUiState.Listening -> "LISTENING... SPEAK ALLOWLISTED COMMAND (dB ${s.rmsDb.toInt()})"
                                        is SpeechInputUiState.Processing -> "PROCESSING SPEECH..."
                                        is SpeechInputUiState.Unavailable -> s.reason
                                        SpeechInputUiState.Idle -> if (uiState.preferences.voiceCommandsEnabled) {
                                            "TAP MICROPHONE TO ISSUE ONE VOICE COMMAND"
                                        } else {
                                            "Voice commands disabled in System Voice Settings."
                                        }
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = SystemColors.PureIce
                                )

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
                                                val hasPerm = ContextCompat.checkSelfPermission(
                                                    context,
                                                    Manifest.permission.RECORD_AUDIO
                                                ) == PackageManager.PERMISSION_GRANTED
                                                if (hasPerm) {
                                                    speechController.startListening(localeTag)
                                                } else {
                                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                }
                                            }
                                        },
                                        enabled = uiState.preferences.voiceCommandsEnabled,
                                        modifier = Modifier.weight(1f).testTag("voice_mic_tap_button"),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (listening) SystemColors.DangerCrimson else SystemColors.ElectricCyan,
                                            contentColor = SystemColors.ObsidianBlack
                                        )
                                    ) {
                                        Text(if (listening) "STOP LISTENING" else "TAP TO SPEAK", style = MaterialTheme.typography.labelLarge)
                                    }
                                }

                                Text(
                                    text = "Privacy Notice: Audio activates ONLY while you tap Speak. Raw audio is never persisted or transmitted outside Android SpeechRecognizer.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SystemColors.MutedSilver
                                )
                            }
                        }
                    }

                    // Fallback / Offline Text Command Input
                    item {
                        HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.ArcaneViolet) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("TEXT FALLBACK COMMAND", style = MaterialTheme.typography.titleSmall, color = SystemColors.ElectricCyan)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = typedCommand,
                                        onValueChange = { typedCommand = it },
                                        placeholder = { Text("e.g. show today's quests") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f).testTag("voice_fallback_input"),
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
                                        enabled = typedCommand.isNotBlank(),
                                        modifier = Modifier.height(52.dp).testTag("voice_fallback_run_btn"),
                                        colors = ButtonDefaults.buttonColors(containerColor = SystemColors.ArcaneViolet, contentColor = SystemColors.PureIce)
                                    ) {
                                        Text("RUN")
                                    }
                                }
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

                    // Safe Allowlist Phrases
                    item {
                        SystemSectionTitle(
                            title = "Allowlisted Voice Intents",
                            subtitle = "Tap any command to test intent resolution safely"
                        )
                    }
                    item {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            VoiceCommandActionType.entries.forEach { action ->
                                val phrase = action.examplePhrases.firstOrNull() ?: action.label
                                FilterChip(
                                    selected = false,
                                    onClick = { onSubmitTranscript(phrase) },
                                    label = { Text("\"$phrase\"") },
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
                                    Text("\"${item.transcript}\" • ${fmt.format(Date(item.executedAtEpochMs))}", style = MaterialTheme.typography.bodySmall, color = SystemColors.MutedSilver)
                                }
                            }
                        }
                    }
                }
            }
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
    modifier: Modifier = Modifier
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize().widthIn(max = 760.dp),
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
                                        modifier = Modifier.weight(1f).testTag("voice_test_button"),
                                        colors = ButtonDefaults.buttonColors(containerColor = SystemColors.ElectricCyan, contentColor = SystemColors.ObsidianBlack)
                                    ) {
                                        Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("TEST VOICE")
                                    }
                                    OutlinedButton(
                                        onClick = onStopVoice,
                                        modifier = Modifier.weight(1f).testTag("voice_stop_button"),
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
                    modifier = Modifier.fillMaxSize().widthIn(max = 760.dp),
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
