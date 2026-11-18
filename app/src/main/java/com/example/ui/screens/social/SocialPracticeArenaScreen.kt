package com.example.ui.screens.social

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import com.example.domain.model.PracticeEvaluationResult
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ConversationDialogueTurn
import com.example.domain.model.ConversationScenario
import com.example.domain.model.ListeningExercise
import com.example.domain.model.PracticeExercise
import com.example.domain.model.SocialMasteryStage
import com.example.domain.model.SocialPracticeSession
import com.example.ui.viewmodel.SocialMasteryViewModel

// Visual Design Tokens
private val ObsidianBlack = Color(0xFF070A13)
private val DeepNavy = Color(0xFF0D1424)
private val SurfaceNavy = Color(0xFF131B2E)
private val CardSurface = Color(0xFF182238)
private val ElectricCyan = Color(0xFF00E5FF)
private val ArcaneViolet = Color(0xFF8B5CF6)
private val GoldHighlight = Color(0xFFFFD700)
private val BorderCyan = Color(0x3300E5FF)
private val TextWhite = Color(0xFFF8FAFC)
private val TextMuted = Color(0xFF94A3B8)
private val EmeraldSuccess = Color(0xFF10B981)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialPracticeArenaScreen(
    viewModel: SocialMasteryViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val sessionState by viewModel.sessionState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Drills, 1: Roleplay, 2: Listening, 3: History

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "COMMUNICATION ARENA",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                color = ElectricCyan
                            )
                        )
                        Text(
                            text = "OFFLINE PRACTICE & SPEECH REHEARSAL",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextMuted,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (sessionState.currentExercise != null || sessionState.currentScenario != null || sessionState.currentListening != null) {
                            viewModel.resetSessionState()
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ElectricCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ObsidianBlack,
                    titleContentColor = TextWhite
                )
            )
        },
        containerColor = ObsidianBlack
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Mode A: Active Practice Drill Execution
            if (sessionState.currentExercise != null) {
                ActiveExerciseSessionView(
                    viewModel = viewModel,
                    exercise = sessionState.currentExercise!!
                )
            }
            // Mode B: Active Conversation Scenario Roleplay
            else if (sessionState.currentScenario != null) {
                ActiveScenarioRoleplayView(
                    viewModel = viewModel,
                    scenario = sessionState.currentScenario!!
                )
            }
            // Mode C: Active Listening Drill
            else if (sessionState.currentListening != null) {
                ActiveListeningSessionView(
                    viewModel = viewModel,
                    listening = sessionState.currentListening!!
                )
            }
            // Mode D: Arena Hub Dashboard
            else {
                Column(modifier = Modifier.fillMaxSize()) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = DeepNavy,
                        contentColor = ElectricCyan,
                        indicator = { tabPositions ->
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp)
                                    .height(3.dp)
                                    .background(ElectricCyan)
                            )
                        }
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("DRILLS", color = if (selectedTab == 0) ElectricCyan else TextMuted) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("ROLEPLAY", color = if (selectedTab == 1) ElectricCyan else TextMuted) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("LISTENING", color = if (selectedTab == 2) ElectricCyan else TextMuted) }
                        )
                        Tab(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            text = { Text("HISTORY", color = if (selectedTab == 3) ElectricCyan else TextMuted) }
                        )
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        when (selectedTab) {
                            0 -> {
                                items(uiState.availableExercises) { ex ->
                                    PracticeExerciseCard(
                                        exercise = ex,
                                        onStart = { viewModel.startExerciseSession(ex.exerciseId) }
                                    )
                                }
                            }
                            1 -> {
                                items(uiState.conversationScenarios) { sc ->
                                    ConversationScenarioCard(
                                        scenario = sc,
                                        onStart = { viewModel.startConversationScenario(sc.scenarioId) }
                                    )
                                }
                            }
                            2 -> {
                                items(uiState.listeningExercises) { listEx ->
                                    ListeningExerciseCard(
                                        exercise = listEx,
                                        onStart = { viewModel.startListeningExercise(listEx.exerciseId) }
                                    )
                                }
                            }
                            3 -> {
                                if (uiState.recentSessions.isEmpty()) {
                                    item {
                                        Text(
                                            text = "No practice sessions recorded yet. Complete a drill to start your history.",
                                            style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted),
                                            modifier = Modifier.padding(vertical = 32.dp)
                                        )
                                    }
                                } else {
                                    items(uiState.recentSessions) { s ->
                                        PracticeSessionHistoryCard(session = s)
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
fun PracticeExerciseCard(
    exercise: PracticeExercise,
    onStart: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderCyan, RoundedCornerShape(12.dp))
            .clickable(onClick = onStart),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = exercise.title,
                    style = MaterialTheme.typography.titleSmall.copy(color = TextWhite, fontWeight = FontWeight.Bold)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0x3300E5FF))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = exercise.difficulty.label.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(color = ElectricCyan, fontSize = 9.sp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = exercise.description,
                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 12.sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Category: ${exercise.category.label}",
                    style = MaterialTheme.typography.labelSmall.copy(color = ArcaneViolet, fontSize = 11.sp)
                )
                Text(
                    text = "${exercise.estimatedDurationMinutes} min drill",
                    style = MaterialTheme.typography.labelSmall.copy(color = GoldHighlight, fontSize = 11.sp)
                )
            }
        }
    }
}

@Composable
fun ConversationScenarioCard(
    scenario: ConversationScenario,
    onStart: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x338B5CF6), RoundedCornerShape(12.dp))
            .clickable(onClick = onStart),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = scenario.title,
                    style = MaterialTheme.typography.titleSmall.copy(color = TextWhite, fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "NPC: ${scenario.npcName}",
                    style = MaterialTheme.typography.labelSmall.copy(color = ArcaneViolet, fontSize = 11.sp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = scenario.contextDescription,
                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 12.sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Objective: ${scenario.objective}",
                style = MaterialTheme.typography.labelSmall.copy(color = ElectricCyan, fontSize = 11.sp)
            )
        }
    }
}

@Composable
fun ListeningExerciseCard(
    exercise: ListeningExercise,
    onStart: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderCyan, RoundedCornerShape(12.dp))
            .clickable(onClick = onStart),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = exercise.title,
                style = MaterialTheme.typography.titleSmall.copy(color = TextWhite, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Target: Extract factual constraints and formulate proactive follow-ups.",
                style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 12.sp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Min Passing Score: ${exercise.minPassingScore}%",
                style = MaterialTheme.typography.labelSmall.copy(color = GoldHighlight, fontSize = 11.sp)
            )
        }
    }
}

@Composable
fun PracticeSessionHistoryCard(session: SocialPracticeSession) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceNavy),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = session.module.displayName,
                    style = MaterialTheme.typography.titleSmall.copy(color = TextWhite, fontSize = 13.sp)
                )
                Text(
                    text = "${session.activityKind.label} • ${session.durationSeconds}s",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 11.sp)
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0x3310B981))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = session.sessionStatus.name,
                    style = MaterialTheme.typography.labelSmall.copy(color = EmeraldSuccess, fontSize = 10.sp)
                )
            }
        }
    }
}

@Composable
fun ActiveExerciseSessionView(
    viewModel: SocialMasteryViewModel,
    exercise: PracticeExercise
) {
    val sessionState by viewModel.sessionState.collectAsState()
    var responseInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderCyan, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = DeepNavy)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = exercise.title.uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(color = ElectricCyan, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = exercise.instructions,
                        style = MaterialTheme.typography.bodySmall.copy(color = TextWhite)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = exercise.promptContent,
                        style = MaterialTheme.typography.bodyMedium.copy(color = GoldHighlight, fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        }

        if (sessionState.evaluationResult != null) {
            item {
                EvaluationResultCard(
                    eval = sessionState.evaluationResult!!,
                    reward = sessionState.rewardEarned,
                    onDone = { viewModel.resetSessionState() }
                )
            }
        } else {
            item {
                OutlinedTextField(
                    value = responseInput,
                    onValueChange = {
                        responseInput = it
                        viewModel.updateDraftText(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    label = { Text("Type or rehearse your response here...", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = BorderCyan
                    )
                )
            }

            item {
                Button(
                    onClick = { viewModel.submitExerciseForEvaluation() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = responseInput.isNotBlank() && !sessionState.isEvaluating,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                ) {
                    if (sessionState.isEvaluating) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = ObsidianBlack)
                    } else {
                        Text("SUBMIT FOR RUBRIC EVALUATION", color = ObsidianBlack, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ActiveScenarioRoleplayView(
    viewModel: SocialMasteryViewModel,
    scenario: ConversationScenario
) {
    val sessionState by viewModel.sessionState.collectAsState()
    var turnInput by remember { mutableStateOf("") }
    val currentRound = sessionState.scenarioCurrentRound

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "ROLEPLAY: ${scenario.title}",
                style = MaterialTheme.typography.titleMedium.copy(color = ElectricCyan, fontWeight = FontWeight.Bold)
            )
            Text(
                text = "Round $currentRound of ${scenario.targetRounds} • NPC: ${scenario.npcName}",
                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
            )
        }

        // Script Dialogue History
        items(sessionState.scenarioUserTurns.zip(scenario.dialogueScript.take(sessionState.scenarioUserTurns.size))) { (userMsg, npcTurn) ->
            NpcChatBubble(npcName = scenario.npcName, text = npcTurn.messageText)
            PlayerChatBubble(text = userMsg)
        }

        // Active NPC turn
        if (currentRound <= scenario.targetRounds && currentRound <= scenario.dialogueScript.size) {
            val activeNpcTurn = scenario.dialogueScript[currentRound - 1]
            item {
                NpcChatBubble(npcName = scenario.npcName, text = activeNpcTurn.messageText)
            }

            if (activeNpcTurn.suggestedApproaches.isNotEmpty()) {
                item {
                    Text(
                        text = "Suggested Approaches:",
                        style = MaterialTheme.typography.labelSmall.copy(color = ArcaneViolet)
                    )
                    activeNpcTurn.suggestedApproaches.forEach { app ->
                        Text(
                            text = "• $app",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted, fontSize = 11.sp),
                            modifier = Modifier
                                .padding(vertical = 2.dp)
                                .clickable { turnInput = app }
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = turnInput,
                    onValueChange = { turnInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Your response...", color = TextMuted) },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (turnInput.isNotBlank()) {
                                    viewModel.sendConversationResponse(turnInput)
                                    turnInput = ""
                                }
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Send, contentDescription = "Send", tint = ElectricCyan)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = BorderCyan
                    )
                )
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DeepNavy)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "SIMULATION COMPLETED",
                            style = MaterialTheme.typography.titleMedium.copy(color = EmeraldSuccess, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "All dialogue rounds cleared. Scenario marked as successfully completed.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextWhite)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.resetSessionState() },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                        ) {
                            Text("RETURN TO ARENA", color = ObsidianBlack, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NpcChatBubble(npcName: String, text: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .border(1.dp, Color(0x338B5CF6), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CardSurface)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = npcName, style = MaterialTheme.typography.labelSmall.copy(color = ArcaneViolet, fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = text, style = MaterialTheme.typography.bodySmall.copy(color = TextWhite))
        }
    }
}

@Composable
fun PlayerChatBubble(text: String) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .border(1.dp, BorderCyan, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = DeepNavy)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(text = "YOU", style = MaterialTheme.typography.labelSmall.copy(color = ElectricCyan, fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = text, style = MaterialTheme.typography.bodySmall.copy(color = TextWhite))
            }
        }
    }
}

@Composable
fun ActiveListeningSessionView(
    viewModel: SocialMasteryViewModel,
    listening: ListeningExercise
) {
    val sessionState by viewModel.sessionState.collectAsState()
    var summaryInput by remember { mutableStateOf("") }
    var followUpInput by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderCyan, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = DeepNavy)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "LISTENING PASSAGE",
                        style = MaterialTheme.typography.labelSmall.copy(color = ElectricCyan, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = listening.spokenOrWrittenPassage,
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextWhite, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    )
                }
            }
        }

        if (sessionState.evaluationResult != null) {
            item {
                EvaluationResultCard(
                    eval = sessionState.evaluationResult!!,
                    reward = sessionState.rewardEarned,
                    onDone = { viewModel.resetSessionState() }
                )
            }
        } else {
            item {
                Text(
                    text = listening.questionPrompt,
                    style = MaterialTheme.typography.titleSmall.copy(color = GoldHighlight)
                )
            }

            item {
                OutlinedTextField(
                    value = summaryInput,
                    onValueChange = { summaryInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Summarize key facts & constraints...", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = BorderCyan
                    )
                )
            }

            item {
                OutlinedTextField(
                    value = followUpInput,
                    onValueChange = { followUpInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Formulate your follow-up inquiry...", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = BorderCyan
                    )
                )
            }

            item {
                Button(
                    onClick = { viewModel.submitListeningExercise(summaryInput, followUpInput) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = summaryInput.isNotBlank() && !sessionState.isEvaluating,
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                ) {
                    if (sessionState.isEvaluating) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = ObsidianBlack)
                    } else {
                        Text("EVALUATE COMPREHENSION", color = ObsidianBlack, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun EvaluationResultCard(
    eval: PracticeEvaluationResult,
    reward: com.example.domain.engine.ActivityRewardCalculation?,
    onDone: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (eval.isPassed) BorderCyan else Color(0x33EF4444), RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = DeepNavy)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (eval.isPassed) "EVALUATION: PASSED" else "EVALUATION: NEEDS PRACTICE",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = if (eval.isPassed) EmeraldSuccess else Color(0xFFEF4444),
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "${eval.overallScore}/100",
                    style = MaterialTheme.typography.headlineSmall.copy(color = ElectricCyan, fontWeight = FontWeight.Black)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = eval.qualitativeSummary, style = MaterialTheme.typography.bodySmall.copy(color = TextWhite))

            Spacer(modifier = Modifier.height(10.dp))
            Text(text = "Rubric Dimensions:", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted))
            eval.dimensionScores.forEach { (dim, score) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = dim, style = MaterialTheme.typography.bodySmall.copy(color = TextWhite, fontSize = 12.sp))
                    Text(text = "$score%", style = MaterialTheme.typography.bodySmall.copy(color = ElectricCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp))
                }
            }

            if (reward != null && reward.xpAwarded > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x2200E5FF))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "+${reward.xpAwarded} XP Earned • ${reward.primaryAttribute.code} +${reward.primaryAttrXpAwarded} XP",
                        style = MaterialTheme.typography.labelSmall.copy(color = GoldHighlight, fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
            ) {
                Text("DONE", color = ObsidianBlack, fontWeight = FontWeight.Bold)
            }
        }
    }
}
