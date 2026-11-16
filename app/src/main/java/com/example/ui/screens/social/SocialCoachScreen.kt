package com.example.ui.screens.social

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AiCoachingMessage
import com.example.domain.model.AiCoachingMode
import com.example.ui.viewmodel.SocialMasteryViewModel

private val ObsidianBlack = Color(0xFF070A13)
private val DeepNavy = Color(0xFF0D1424)
private val CardSurface = Color(0xFF182238)
private val ElectricCyan = Color(0xFF00E5FF)
private val ArcaneViolet = Color(0xFF8B5CF6)
private val BorderCyan = Color(0x3300E5FF)
private val TextWhite = Color(0xFFF8FAFC)
private val TextMuted = Color(0xFF94A3B8)
private val GoldHighlight = Color(0xFFFFD700)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialCoachScreen(
    viewModel: SocialMasteryViewModel,
    onNavigateBack: () -> Unit
) {
    val coachState by viewModel.aiCoachState.collectAsState()
    var promptInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "AI COMMUNICATION COACH",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                color = ElectricCyan
                            )
                        )
                        Text(
                            text = "VOICE-ENABLED SOCIAL ADVISORY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextMuted,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ElectricCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianBlack)
            )
        },
        containerColor = ObsidianBlack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Coaching Modes Tab Row
            ScrollableTabRow(
                selectedTabIndex = coachState.selectedMode.ordinal,
                containerColor = DeepNavy,
                contentColor = ElectricCyan,
                edgePadding = 16.dp
            ) {
                AiCoachingMode.entries.forEach { mode ->
                    Tab(
                        selected = coachState.selectedMode == mode,
                        onClick = { viewModel.setAiCoachMode(mode) },
                        text = {
                            Text(
                                text = mode.label.uppercase(),
                                color = if (coachState.selectedMode == mode) ElectricCyan else TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )
                }
            }

            // Status & Voice Indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = coachState.statusText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (coachState.isVoiceListening) GoldHighlight else TextMuted,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    if (coachState.isVoiceListening) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = GoldHighlight)
                    }
                }
            }

            // Message Stream
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (coachState.messages.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            colors = CardDefaults.cardColors(containerColor = CardSurface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = coachState.selectedMode.label.uppercase(),
                                    style = MaterialTheme.typography.titleSmall.copy(color = ElectricCyan, fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = coachState.selectedMode.description,
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextWhite)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Tap the microphone icon or type below to ask for communication guidance, presentation critique, or negotiation tactics.",
                                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                                )
                            }
                        }
                    }
                } else {
                    items(coachState.messages) { msg ->
                        CoachChatBubble(message = msg)
                    }
                }
            }

            // Bottom Input Bar
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DeepNavy),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Voice Mic Button
                    IconButton(
                        onClick = {
                            if (coachState.isVoiceListening) {
                                viewModel.stopVoiceAndSubmit(speakAloud = true)
                            } else {
                                viewModel.startVoiceListening()
                            }
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (coachState.isVoiceListening) Color(0xFFEF4444) else Color(0x2200E5FF))
                    ) {
                        Icon(
                            imageVector = if (coachState.isVoiceListening) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = "Voice Input",
                            tint = if (coachState.isVoiceListening) TextWhite else ElectricCyan
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedTextField(
                        value = promptInput,
                        onValueChange = { promptInput = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ask your communication coach...", color = TextMuted, fontSize = 13.sp) },
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = BorderCyan
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (promptInput.isNotBlank()) {
                                viewModel.sendAiCoachMessage(promptInput)
                                promptInput = ""
                            }
                        },
                        enabled = promptInput.isNotBlank() && !coachState.isGenerating
                    ) {
                        if (coachState.isGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = ElectricCyan)
                        } else {
                            Icon(imageVector = Icons.Default.Send, contentDescription = "Send", tint = ElectricCyan)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CoachChatBubble(message: AiCoachingMessage) {
    val isUser = message.role.equals("USER", ignoreCase = true)
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .border(
                    1.dp,
                    if (isUser) BorderCyan else Color(0x338B5CF6),
                    RoundedCornerShape(12.dp)
                ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) Color(0xFF131B2E) else CardSurface
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = if (isUser) "YOU" else "SYSTEM COACH",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isUser) ElectricCyan else ArcaneViolet,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodySmall.copy(color = TextWhite, lineHeight = 18.sp)
                )
            }
        }
    }
}
