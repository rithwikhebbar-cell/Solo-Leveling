package com.example.ui.screens.ai

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
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
import com.example.domain.model.AiChatMessage
import com.example.domain.model.AiChatRole
import com.example.ui.components.HolographicPanel
import com.example.ui.theme.SystemColors
import com.example.ui.viewmodel.AiSystemUiState

private val SuggestedPrompts = listOf(
    "What is holding back my rank progression?",
    "Am I ready to work out today?",
    "Which academic subject needs focus?",
    "Plan my day around overdue revisions"
)

@Composable
fun AiChatSection(
    state: AiSystemUiState.Ready,
    onSelectConversation: (Long) -> Unit,
    onNewConversation: () -> Unit,
    onSendMessage: (String) -> Unit,
    onRetryLastPrompt: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by rememberSaveable { mutableStateOf("") }

    Column(modifier = modifier.fillMaxSize()) {
        // Conversation switcher row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                AssistChip(
                    onClick = onNewConversation,
                    label = { Text("New Chat") },
                    leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = SystemColors.ElectricCyan.copy(alpha = 0.14f),
                        labelColor = SystemColors.ElectricCyan,
                        leadingIconContentColor = SystemColors.ElectricCyan
                    ),
                    modifier = Modifier.testTag("ai_new_conversation_button")
                )
            }
            items(state.conversations, key = { it.id }) { conv ->
                val selected = conv.id == state.activeConversationId
                FilterChip(
                    selected = selected,
                    onClick = { onSelectConversation(conv.id) },
                    label = { Text(conv.title.take(22), maxLines = 1) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SystemColors.ArcaneViolet.copy(alpha = 0.28f),
                        selectedLabelColor = SystemColors.PureIce,
                        containerColor = SystemColors.DeepNavy,
                        labelColor = SystemColors.MutedSilver
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Message stream
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            if (state.messages.isEmpty() && !state.isSendingChat) {
                item {
                    HolographicPanel(
                        modifier = Modifier.fillMaxWidth(),
                        accentColor = SystemColors.SystemBlue
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "GAME MASTER READY",
                                style = MaterialTheme.typography.titleSmall,
                                color = SystemColors.ElectricCyan
                            )
                            Text(
                                text = "Ask about your rank gaps, recovery readiness, overdue revisions, exam preparation, or weekly balance. Offline deterministic diagnostics run automatically if cloud AI is disabled or unreachable.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SystemColors.MetallicSilver
                            )
                        }
                    }
                }
            }

            items(state.messages, key = { it.id }) { msg ->
                ChatMessageBubble(msg)
            }

            if (state.isSendingChat) {
                item {
                    Surface(
                        color = SystemColors.DeepNavy,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SystemColors.ArcaneViolet.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = SystemColors.ElectricCyan
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SYSTEM ANALYZING...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SystemColors.ElectricCyan
                                )
                            }
                            if (state.streamingPreview.isNotBlank()) {
                                Text(
                                    text = state.streamingPreview,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SystemColors.PureIce
                                )
                            }
                        }
                    }
                }
            }
        }

        if (state.lastErrorDetail != null && state.lastUserPromptForRetry != null) {
            Surface(
                color = SystemColors.DangerCrimson.copy(alpha = 0.14f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(0.75.dp, SystemColors.DangerCrimson.copy(alpha = 0.55f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${state.connectionStatus.label}: ${state.lastErrorDetail}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SystemColors.PureIce,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = onRetryLastPrompt,
                        modifier = Modifier.testTag("ai_retry_prompt_button")
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Retry", color = SystemColors.ElectricCyan)
                    }
                }
            }
        }

        // Suggested prompts
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(vertical = 6.dp)
        ) {
            items(SuggestedPrompts) { suggestion ->
                AssistChip(
                    onClick = {
                        inputText = ""
                        onSendMessage(suggestion)
                    },
                    label = { Text(suggestion, style = MaterialTheme.typography.labelSmall) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = SystemColors.DeepNavy,
                        labelColor = SystemColors.MetallicSilver
                    )
                )
            }
        }

        // Prompt composer
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Ask the System Administrator...") },
                singleLine = false,
                maxLines = 3,
                enabled = !state.isSendingChat,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SystemColors.ElectricCyan,
                    unfocusedBorderColor = SystemColors.SubtleGridLine,
                    focusedTextColor = SystemColors.PureIce,
                    unfocusedTextColor = SystemColors.PureIce,
                    focusedContainerColor = SystemColors.DeepNavy,
                    unfocusedContainerColor = SystemColors.DeepNavy
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_chat_input")
            )
            IconButton(
                onClick = {
                    val toSend = inputText.trim()
                    if (toSend.isNotBlank()) {
                        inputText = ""
                        onSendMessage(toSend)
                    }
                },
                enabled = !state.isSendingChat && inputText.isNotBlank(),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = SystemColors.ElectricCyan,
                    contentColor = SystemColors.ObsidianBlack
                ),
                modifier = Modifier
                    .size(48.dp)
                    .testTag("ai_send_chat_button")
            ) {
                Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send prompt")
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(message: AiChatMessage) {
    val isUser = message.role == AiChatRole.USER
    val accent = if (isUser) SystemColors.ElectricCyan else SystemColors.ArcaneViolet
    Surface(
        color = if (isUser) SystemColors.ElectricCyan.copy(alpha = 0.12f) else SystemColors.DeepNavy,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.9.dp, accent.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isUser) "PLAYER" else "SYSTEM GAME MASTER",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent
                )
                if (!isUser) {
                    Text(
                        text = if (message.isFallback) "OFFLINE DETERMINISTIC" else message.modelUsed.substringAfterLast('/'),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (message.isFallback) SystemColors.SystemBlue else SystemColors.VerifiedEmerald
                    )
                }
            }
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = SystemColors.PureIce
            )
        }
    }
}

@Composable
fun AiInsightsSection(
    state: AiSystemUiState.Ready,
    onRefreshAnalysis: () -> Unit,
    modifier: Modifier = Modifier
) {
    val report = state.analysisReport
    val snapshot = state.contextSnapshot

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.ElectricCyan) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = report?.headline ?: "PERSONAL DEVELOPMENT ANALYSIS",
                        style = MaterialTheme.typography.titleMedium,
                        color = SystemColors.ElectricCyan
                    )
                    Text(
                        text = report?.overallSummary ?: "Analyzing current Physical, Academic, and Quest state...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SystemColors.PureIce
                    )
                    Text(
                        text = report?.provenanceDistinctionNote ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = SystemColors.MutedSilver
                    )
                    Button(
                        onClick = onRefreshAnalysis,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SystemColors.ArcaneViolet,
                            contentColor = SystemColors.PureIce
                        ),
                        modifier = Modifier.testTag("ai_reanalyze_button")
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("REFRESH DIAGNOSTIC")
                    }
                }
            }
        }

        if (report != null) {
            item { InsightListPanel("STRENGTHS", report.strengths, SystemColors.VerifiedEmerald) }
            item { InsightListPanel("WEAKNESSES & GAPS", report.weaknesses, SystemColors.MonarchGold) }
            item { InsightListPanel("RANK & PROGRESSION BOTTLENECKS", report.bottlenecks, SystemColors.DangerCrimson) }
            item { InsightListPanel("RECOMMENDED NEXT ACTIONS", report.recommendedNextActions, SystemColors.ElectricCyan) }
        }

        if (snapshot != null && snapshot.dataCompletenessNotes.isNotEmpty()) {
            item {
                InsightListPanel(
                    title = "UNRECORDED / MISSING DATA NOTICE",
                    items = snapshot.dataCompletenessNotes,
                    accent = SystemColors.MutedSilver
                )
            }
        }
    }
}

@Composable
private fun InsightListPanel(
    title: String,
    items: List<String>,
    accent: androidx.compose.ui.graphics.Color
) {
    HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = accent) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = accent)
            items.forEach { line ->
                Text(
                    text = "• $line",
                    style = MaterialTheme.typography.bodySmall,
                    color = SystemColors.PureIce
                )
            }
        }
    }
}
