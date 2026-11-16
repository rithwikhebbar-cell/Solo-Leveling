package com.example.ui.screens.experience

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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.AiChatRole
import com.example.domain.model.AiConnectionStatus
import com.example.ui.components.HolographicPanel
import com.example.ui.components.SystemErrorState
import com.example.ui.components.SystemLoadingView
import com.example.ui.theme.SystemColors
import com.example.ui.viewmodel.AiSystemUiState

private val COMPANION_SUGGESTIONS = listOf(
    "Why can't I rank up yet?",
    "Should I train or recover today?",
    "Analyze my weakest attribute",
    "What should I study next?",
    "Summarize today's life discipline"
)

/**
 * Dedicated AI System Companion Screen backed by AiSystemRepository & SystemVoiceEngine.
 * Strictly advisory: respects consent switches, displays offline fallback cleanly, and never grants XP.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiCompanionScreen(
    uiState: AiSystemUiState,
    ttsEnabled: Boolean,
    onRetryInit: () -> Unit,
    onSendChatMessage: (String) -> Unit,
    onRetryLastPrompt: () -> Unit,
    onNewConversation: () -> Unit,
    onClearAllConversations: () -> Unit,
    onSpeakMessage: (String) -> Unit,
    onOpenVoiceConsole: () -> Unit,
    onOpenAiSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by rememberSaveable { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SystemColors.ObsidianBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        when (uiState) {
            is AiSystemUiState.Loading -> SystemLoadingView()
            is AiSystemUiState.Error -> Box(Modifier.padding(20.dp)) {
                SystemErrorState(message = uiState.message, onRetry = onRetryInit)
            }
            is AiSystemUiState.Ready -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 820.dp)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header Banner
                    HolographicPanel(
                        modifier = Modifier.fillMaxWidth(),
                        accentColor = SystemColors.ElectricCyan,
                        secondaryColor = SystemColors.ArcaneViolet,
                        contentPadding = 12.dp
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = "System Companion",
                                        tint = SystemColors.ElectricCyan,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text("AI SYSTEM COMPANION", style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
                                        Text(
                                            text = if (uiState.consentState.cloudAiConsentGranted) {
                                                "${uiState.connectionStatus.label} • Advisory Only"
                                            } else {
                                                "Offline Deterministic System Advisor • Consent Local-Only"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (uiState.connectionStatus == AiConnectionStatus.CONNECTED) SystemColors.VerifiedEmerald else SystemColors.ElectricCyan
                                        )
                                    }
                                }
                                Row {
                                    IconButton(onClick = onOpenVoiceConsole, modifier = Modifier.testTag("companion_voice_btn")) {
                                        Icon(Icons.Rounded.Mic, contentDescription = "Voice Command Console", tint = SystemColors.ElectricCyan)
                                    }
                                    IconButton(onClick = onClearAllConversations, modifier = Modifier.testTag("companion_clear_btn")) {
                                        Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear conversation history", tint = SystemColors.MutedSilver)
                                    }
                                    IconButton(onClick = onOpenAiSettings, modifier = Modifier.testTag("companion_settings_btn")) {
                                        Icon(Icons.Rounded.Settings, contentDescription = "AI & Consent Settings", tint = SystemColors.ArcaneViolet)
                                    }
                                }
                            }

                            if (!uiState.consentState.cloudAiConsentGranted) {
                                Text(
                                    text = "Privacy Guard: Cloud sharing is disabled. Responses use local deterministic telemetry without external data transmission.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SystemColors.MetallicSilver
                                )
                            }
                        }
                    }

                    // Quick Prompt Chips
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        COMPANION_SUGGESTIONS.forEach { prompt ->
                            AssistChip(
                                onClick = { onSendChatMessage(prompt) },
                                label = { Text(prompt, style = MaterialTheme.typography.labelSmall) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = SystemColors.DeepNavy,
                                    labelColor = SystemColors.ElectricCyan
                                ),
                                border = BorderStroke(0.75.dp, SystemColors.PanelBorderCyan)
                            )
                        }
                    }

                    // Conversation Stream
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth().testTag("companion_message_list"),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (uiState.messages.isEmpty() && !uiState.isSendingChat) {
                            item {
                                HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.ArcaneViolet) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("SYSTEM COMPANION ONLINE", style = MaterialTheme.typography.titleSmall, color = SystemColors.ElectricCyan)
                                        Text(
                                            text = "Ask about your rank bottlenecks, training readiness, academic study cadence, habit streaks, or dungeon readiness. Telemetry provenance (Measured vs Self-Reported vs Estimated) is preserved strictly.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SystemColors.MetallicSilver
                                        )
                                        TextButton(onClick = onNewConversation) {
                                            Text("INITIALIZE NEW SESSION", color = SystemColors.ArcaneViolet)
                                        }
                                    }
                                }
                            }
                        }

                        items(uiState.messages, key = { it.id }) { msg ->
                            val isUser = msg.role == AiChatRole.USER
                            val accent = if (isUser) SystemColors.ElectricCyan else SystemColors.ArcaneViolet
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = if (isUser) SystemColors.ElectricCyan.copy(alpha = 0.08f) else SystemColors.DeepNavy,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isUser) "PLAYER" else if (msg.isFallback) "SYSTEM ADVISORY (OFFLINE)" else "SYSTEM COMPANION",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = accent
                                        )
                                        if (!isUser && ttsEnabled) {
                                            IconButton(
                                                onClick = { onSpeakMessage(msg.content) },
                                                modifier = Modifier
                                                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                                    .testTag("companion_speak_msg_${msg.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                                                    contentDescription = "Read response aloud",
                                                    tint = SystemColors.ElectricCyan,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = msg.content,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SystemColors.PureIce
                                    )
                                }
                            }
                        }

                        if (uiState.isSendingChat) {
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth().testTag("companion_typing_indicator"),
                                    color = SystemColors.DeepNavy,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, SystemColors.ArcaneViolet)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("SYSTEM ANALYZING TELEMETRY...", style = MaterialTheme.typography.labelSmall, color = SystemColors.ElectricCyan)
                                        Text(
                                            text = uiState.streamingPreview.ifBlank { "Synthesizing deterministic and authorized advisory context..." },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = SystemColors.MetallicSilver
                                        )
                                    }
                                }
                            }
                        }

                        if (uiState.lastErrorDetail != null && uiState.lastUserPromptForRetry != null) {
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = SystemColors.DangerCrimson.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, SystemColors.DangerCrimson)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = uiState.lastErrorDetail,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SystemColors.PureIce,
                                            modifier = Modifier.weight(1f)
                                        )
                                        TextButton(onClick = onRetryLastPrompt) {
                                            Icon(Icons.Rounded.Refresh, contentDescription = "Retry", tint = SystemColors.ElectricCyan, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("RETRY", color = SystemColors.ElectricCyan)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Input Composer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("Ask System Companion...") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("companion_input_field"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SystemColors.ElectricCyan,
                                unfocusedBorderColor = SystemColors.PanelBorderCyan,
                                focusedTextColor = SystemColors.PureIce,
                                unfocusedTextColor = SystemColors.PureIce
                            )
                        )
                        Button(
                            onClick = {
                                val text = inputText.trim()
                                if (text.isNotEmpty()) {
                                    onSendChatMessage(text)
                                    inputText = ""
                                }
                            },
                            enabled = !uiState.isSendingChat && inputText.isNotBlank(),
                            modifier = Modifier.height(52.dp).testTag("companion_send_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SystemColors.ElectricCyan,
                                contentColor = SystemColors.ObsidianBlack
                            )
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send prompt")
                        }
                    }
                }
            }
        }
    }
}
