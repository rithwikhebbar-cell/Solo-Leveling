package com.example.ui.screens.experience

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.domain.model.AiChatRole
import com.example.domain.model.AiConnectionStatus
import com.example.domain.model.AiConversation
import com.example.domain.model.SupportedAiModels
import com.example.ui.components.HolographicPanel
import com.example.ui.components.SystemErrorState
import com.example.ui.components.SystemLoadingView
import com.example.ui.theme.SystemColors
import com.example.ui.viewmodel.AiSystemUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val COMPANION_SUGGESTIONS = listOf(
    "Why can't I rank up yet?",
    "Should I train or recover today?",
    "Analyze my weakest attribute",
    "What should I study next?",
    "Evaluate my Shadow Army readiness"
)

/**
 * Dedicated Gemini-powered AI System Companion Screen backed by AiSystemRepository & SystemVoiceEngine.
 * Strictly advisory: respects consent switches, displays offline fallback cleanly, supports multi-session
 * memory, streaming stop/regenerate, Markdown/code formatting, copy, and never grants XP directly.
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
    modifier: Modifier = Modifier,
    onSelectConversation: (Long) -> Unit = {},
    onDeleteConversation: (Long) -> Unit = {},
    onStopGeneration: () -> Unit = {},
    onRegenerateResponse: () -> Unit = {},
    onEnableCloudConsent: () -> Unit = {}
) {
    var inputText by rememberSaveable { mutableStateOf("") }
    var showHistoryDialog by rememberSaveable { mutableStateOf(false) }
    var copiedNoticeMessageId by remember { mutableStateOf<Long?>(null) }
    val clipboardManager = LocalClipboardManager.current
    val listState = rememberLazyListState()
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

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
                val selectedModel = remember(uiState.modelPreferences.selectedModelId) {
                    SupportedAiModels.findById(uiState.modelPreferences.selectedModelId)
                }
                val lastAssistantIndex = uiState.messages.indexOfLast { it.role == AiChatRole.ASSISTANT }

                // Auto-scroll to latest message or streaming chunk
                LaunchedEffect(uiState.messages.size, uiState.isSendingChat, uiState.streamingPreview.length) {
                    val totalItems = uiState.messages.size + if (uiState.isSendingChat) 1 else 0
                    if (totalItems > 0) {
                        listState.animateScrollToItem(totalItems - 1)
                    }
                }

                if (showHistoryDialog) {
                    ConversationHistoryDialog(
                        conversations = uiState.conversations,
                        activeConversationId = uiState.activeConversationId,
                        onSelect = { id ->
                            onSelectConversation(id)
                            showHistoryDialog = false
                        },
                        onDelete = onDeleteConversation,
                        onNewSession = {
                            onNewConversation()
                            showHistoryDialog = false
                        },
                        onClearAll = {
                            onClearAllConversations()
                            showHistoryDialog = false
                        },
                        onDismiss = { showHistoryDialog = false }
                    )
                }

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
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                        Text(
                                            text = "AI SYSTEM COMPANION",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = SystemColors.PureIce
                                        )
                                        Text(
                                            text = if (uiState.consentState.cloudAiConsentGranted) {
                                                "${selectedModel.displayName} • ${uiState.connectionStatus.label}"
                                            } else {
                                                "Offline Deterministic Advisor • Cloud Consent Off"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (uiState.connectionStatus == AiConnectionStatus.CONNECTED ||
                                                uiState.connectionStatus == AiConnectionStatus.STREAMING
                                            ) {
                                                SystemColors.VerifiedEmerald
                                            } else {
                                                SystemColors.ElectricCyan
                                            },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { showHistoryDialog = true },
                                        modifier = Modifier.testTag("companion_history_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.History,
                                            contentDescription = "Conversation sessions",
                                            tint = SystemColors.PureIce
                                        )
                                    }
                                    IconButton(
                                        onClick = onOpenVoiceConsole,
                                        modifier = Modifier.testTag("companion_voice_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Mic,
                                            contentDescription = "Voice Command Console",
                                            tint = SystemColors.ElectricCyan
                                        )
                                    }
                                    IconButton(
                                        onClick = onClearAllConversations,
                                        modifier = Modifier.testTag("companion_clear_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteSweep,
                                            contentDescription = "Clear conversation history",
                                            tint = SystemColors.MutedSilver
                                        )
                                    }
                                    IconButton(
                                        onClick = onOpenAiSettings,
                                        modifier = Modifier.testTag("companion_settings_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Settings,
                                            contentDescription = "AI & Consent Settings",
                                            tint = SystemColors.ArcaneViolet
                                        )
                                    }
                                }
                            }

                            if (!uiState.consentState.cloudAiConsentGranted) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Privacy Guard: Cloud Gemini AI is paused. Local deterministic analysis is active.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SystemColors.MetallicSilver,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    AssistChip(
                                        onClick = onEnableCloudConsent,
                                        modifier = Modifier.testTag("companion_enable_gemini_btn"),
                                        label = { Text("ENABLE GEMINI", style = MaterialTheme.typography.labelSmall) },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = SystemColors.VerifiedEmerald.copy(alpha = 0.18f),
                                            labelColor = SystemColors.VerifiedEmerald
                                        ),
                                        border = BorderStroke(0.75.dp, SystemColors.VerifiedEmerald)
                                    )
                                }
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
                                onClick = { if (!uiState.isSendingChat) onSendChatMessage(prompt) },
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
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag("companion_message_list"),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (uiState.messages.isEmpty() && !uiState.isSendingChat) {
                            item {
                                HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.ArcaneViolet) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = "GEMINI SYSTEM COMPANION ONLINE",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = SystemColors.ElectricCyan
                                        )
                                        Text(
                                            text = "Ask about your rank bottlenecks, training readiness, academic study cadence, habit streaks, Shadow Army status, or dungeon readiness. Telemetry provenance (Measured vs Self-Reported vs Estimated) is preserved strictly.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SystemColors.MetallicSilver
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            TextButton(onClick = onNewConversation) {
                                                Text("INITIALIZE NEW SESSION", color = SystemColors.ArcaneViolet)
                                            }
                                            if (uiState.conversations.isNotEmpty()) {
                                                TextButton(onClick = { showHistoryDialog = true }) {
                                                    Text("BROWSE SESSIONS (${uiState.conversations.size})", color = SystemColors.ElectricCyan)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        itemsIndexed(uiState.messages, key = { _, msg -> msg.id }) { index, msg ->
                            val isUser = msg.role == AiChatRole.USER
                            val accent = if (isUser) SystemColors.ElectricCyan else SystemColors.ArcaneViolet
                            val timestamp = remember(msg.timestampEpochMs) {
                                timeFormatter.format(Date(msg.timestampEpochMs))
                            }
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = if (isUser) SystemColors.ElectricCyan.copy(alpha = 0.08f) else SystemColors.DeepNavy,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = if (isUser) {
                                                    "PLAYER"
                                                } else if (msg.isFallback) {
                                                    "SYSTEM ADVISORY (OFFLINE)"
                                                } else {
                                                    "GEMINI COMPANION"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = accent
                                            )
                                            Text(
                                                text = "• $timestamp",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SystemColors.MutedSilver
                                            )
                                        }

                                        if (!isUser) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(
                                                    onClick = {
                                                        clipboardManager.setText(AnnotatedString(msg.content))
                                                        copiedNoticeMessageId = msg.id
                                                    },
                                                    modifier = Modifier
                                                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                                        .testTag("companion_copy_msg_${msg.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.ContentCopy,
                                                        contentDescription = "Copy response",
                                                        tint = if (copiedNoticeMessageId == msg.id) SystemColors.VerifiedEmerald else SystemColors.MutedSilver,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                if (index == lastAssistantIndex && !uiState.isSendingChat) {
                                                    IconButton(
                                                        onClick = onRegenerateResponse,
                                                        modifier = Modifier
                                                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                                            .testTag("companion_regenerate_btn")
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Rounded.Refresh,
                                                            contentDescription = "Regenerate response",
                                                            tint = SystemColors.ArcaneViolet,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                }
                                                if (ttsEnabled) {
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
                                        }
                                    }

                                    if (isUser) {
                                        Text(
                                            text = msg.content,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = SystemColors.PureIce
                                        )
                                    } else {
                                        MarkdownMessageContent(content = msg.content)
                                    }
                                }
                            }
                        }

                        if (uiState.isSendingChat) {
                            item {
                                StreamingTypingPanel(
                                    streamingPreview = uiState.streamingPreview,
                                    onStopGeneration = onStopGeneration
                                )
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
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
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
                                            Icon(
                                                Icons.Rounded.Refresh,
                                                contentDescription = "Retry",
                                                tint = SystemColors.ElectricCyan,
                                                modifier = Modifier.size(16.dp)
                                            )
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
                            placeholder = { Text("Ask Gemini System Companion...") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("companion_input_field"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = SystemColors.ElectricCyan,
                                unfocusedBorderColor = SystemColors.PanelBorderCyan,
                                focusedTextColor = SystemColors.PureIce,
                                unfocusedTextColor = SystemColors.PureIce
                            )
                        )
                        if (uiState.isSendingChat) {
                            Button(
                                onClick = onStopGeneration,
                                modifier = Modifier
                                    .height(52.dp)
                                    .testTag("companion_stop_btn"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SystemColors.DangerCrimson,
                                    contentColor = SystemColors.PureIce
                                )
                            ) {
                                Icon(Icons.Rounded.Stop, contentDescription = "Stop generating response")
                            }
                        } else {
                            Button(
                                onClick = {
                                    val text = inputText.trim()
                                    if (text.isNotEmpty()) {
                                        onSendChatMessage(text)
                                        inputText = ""
                                    }
                                },
                                enabled = inputText.isNotBlank(),
                                modifier = Modifier
                                    .height(52.dp)
                                    .testTag("companion_send_button"),
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
}

@Composable
private fun StreamingTypingPanel(
    streamingPreview: String,
    onStopGeneration: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "companion_typing_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("companion_typing_indicator"),
        color = SystemColors.DeepNavy,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, SystemColors.ArcaneViolet)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .alpha(pulseAlpha)
                            .background(SystemColors.ElectricCyan, CircleShape)
                    )
                    Text(
                        text = "GEMINI ANALYZING TELEMETRY...",
                        style = MaterialTheme.typography.labelSmall,
                        color = SystemColors.ElectricCyan
                    )
                }
                TextButton(
                    onClick = onStopGeneration,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Stop,
                        contentDescription = "Stop",
                        tint = SystemColors.DangerCrimson,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("STOP", style = MaterialTheme.typography.labelSmall, color = SystemColors.DangerCrimson)
                }
            }
            if (streamingPreview.isNotBlank()) {
                MarkdownMessageContent(content = streamingPreview)
            } else {
                Text(
                    text = "Synthesizing player telemetry and authorized advisory context...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SystemColors.MetallicSilver
                )
            }
        }
    }
}

private sealed interface MarkdownBlock {
    data class TextParagraph(val text: String) : MarkdownBlock
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock
}

private fun parseMarkdownBlocks(raw: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val regex = Regex("```([a-zA-Z0-9_+-]*)\\n?([\\s\\S]*?)```")
    var cursor = 0
    for (match in regex.findAll(raw)) {
        val before = raw.substring(cursor, match.range.first).trim()
        if (before.isNotEmpty()) {
            blocks.add(MarkdownBlock.TextParagraph(before))
        }
        val lang = match.groupValues[1].trim()
        val code = match.groupValues[2].trimEnd()
        blocks.add(MarkdownBlock.CodeBlock(lang, code))
        cursor = match.range.last + 1
    }
    if (cursor < raw.length) {
        val tail = raw.substring(cursor).trim()
        if (tail.isNotEmpty()) {
            blocks.add(MarkdownBlock.TextParagraph(tail))
        }
    }
    if (blocks.isEmpty()) {
        blocks.add(MarkdownBlock.TextParagraph(raw))
    }
    return blocks
}

private fun buildStyledInlineMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    val lines = text.lines()
    lines.forEachIndexed { idx, rawLine ->
        val line = rawLine.trimEnd()
        val isHeading = line.startsWith("### ") || line.startsWith("## ") || line.startsWith("# ")
        val normalizedLine = when {
            line.startsWith("### ") -> line.removePrefix("### ")
            line.startsWith("## ") -> line.removePrefix("## ")
            line.startsWith("# ") -> line.removePrefix("# ")
            line.startsWith("- ") || line.startsWith("* ") -> "• ${line.drop(2)}"
            else -> line
        }
        if (isHeading) {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = SystemColors.ElectricCyan)) {
                append(normalizedLine)
            }
        } else {
            appendInlineSegments(normalizedLine)
        }
        if (idx < lines.lastIndex) append("\n")
    }
}

private fun AnnotatedString.Builder.appendInlineSegments(line: String) {
    val tokenRegex = Regex("(\\*\\*[^*]+\\*\\*|`[^`]+`)")
    var cursor = 0
    for (match in tokenRegex.findAll(line)) {
        if (match.range.first > cursor) {
            append(line.substring(cursor, match.range.first))
        }
        val token = match.value
        when {
            token.startsWith("**") && token.endsWith("**") -> {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = SystemColors.PureIce)) {
                    append(token.removeSurrounding("**"))
                }
            }
            token.startsWith("`") && token.endsWith("`") -> {
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        color = SystemColors.ElectricCyan,
                        background = SystemColors.ObsidianBlack
                    )
                ) {
                    append(" ${token.removeSurrounding("`")} ")
                }
            }
            else -> append(token)
        }
        cursor = match.range.last + 1
    }
    if (cursor < line.length) {
        append(line.substring(cursor))
    }
}

@Composable
private fun MarkdownMessageContent(content: String) {
    val blocks = remember(content) { parseMarkdownBlocks(content) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.TextParagraph -> {
                    val annotated = remember(block.text) { buildStyledInlineMarkdown(block.text) }
                    Text(
                        text = annotated,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SystemColors.PureIce
                    )
                }
                is MarkdownBlock.CodeBlock -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = SystemColors.ObsidianBlack,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(0.75.dp, SystemColors.PanelBorderCyan)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (block.language.isNotBlank()) {
                                Text(
                                    text = block.language.uppercase(Locale.US),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SystemColors.ArcaneViolet
                                )
                            }
                            Text(
                                text = block.code,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = SystemColors.ElectricCyan,
                                modifier = Modifier.horizontalScroll(rememberScrollState())
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationHistoryDialog(
    conversations: List<AiConversation>,
    activeConversationId: Long?,
    onSelect: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onNewSession: () -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val dateFmt = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SystemColors.DeepNavy,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("CONVERSATION SESSIONS", color = SystemColors.ElectricCyan, style = MaterialTheme.typography.titleMedium)
                OutlinedButton(
                    onClick = onNewSession,
                    border = BorderStroke(1.dp, SystemColors.ElectricCyan),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, tint = SystemColors.ElectricCyan, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("NEW", color = SystemColors.ElectricCyan, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        text = {
            if (conversations.isEmpty()) {
                Text(
                    text = "No saved conversations yet. Start a prompt to initialize persistent Room memory.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SystemColors.MetallicSilver
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(conversations, key = { it.id }) { conv ->
                        val isActive = conv.id == activeConversationId
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(conv.id) },
                            color = if (isActive) SystemColors.ElectricCyan.copy(alpha = 0.14f) else SystemColors.ObsidianBlack.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (isActive) SystemColors.ElectricCyan else SystemColors.SubtleGridLine)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = conv.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (isActive) SystemColors.ElectricCyan else SystemColors.PureIce,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = dateFmt.format(Date(conv.updatedAtEpochMs)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SystemColors.MutedSilver
                                    )
                                }
                                IconButton(
                                    onClick = { onDelete(conv.id) },
                                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = "Delete session",
                                        tint = SystemColors.DangerCrimson,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = SystemColors.ElectricCyan)
            }
        },
        dismissButton = {
            if (conversations.isNotEmpty()) {
                TextButton(onClick = onClearAll) {
                    Text("CLEAR ALL", color = SystemColors.DangerCrimson)
                }
            }
        }
    )
}
