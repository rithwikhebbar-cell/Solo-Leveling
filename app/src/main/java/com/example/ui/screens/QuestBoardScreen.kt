package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.domain.model.Quest
import com.example.domain.model.QuestCategory
import com.example.domain.model.QuestType
import com.example.ui.components.SystemEmptyState
import com.example.ui.screens.quest.components.CreateQuestDialog
import com.example.ui.screens.quest.components.DailyQuestCommandSection
import com.example.ui.screens.quest.components.QuestCard
import com.example.ui.screens.quest.components.QuestCommandHeader
import com.example.ui.screens.quest.components.QuestDetailBottomSheet
import com.example.ui.screens.quest.components.QuestTabFilter
import com.example.ui.theme.SystemColors
import com.example.ui.theme.SystemSpacing
import com.example.ui.viewmodel.RealityOsUiState

@Composable
fun QuestBoardScreen(
    state: RealityOsUiState.Ready,
    onCompleteQuest: (Long) -> Unit,
    onCreateQuest: (String, String, QuestType, QuestCategory, Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableStateOf(QuestTabFilter.ALL) }
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var inspectingQuest by remember { mutableStateOf<Quest?>(null) }
    var pendingQuestIds by remember { mutableStateOf(setOf<Long>()) }

    // Prune completed quests from pending IDs
    val activePendingIds = remember(state.quests, pendingQuestIds) {
        val activeIds = state.quests.filter { !it.isCompleted }.map { it.id }.toSet()
        pendingQuestIds.intersect(activeIds)
    }

    // Keep inspecting quest up to date with live state changes
    val liveInspectingQuest = remember(state.quests, inspectingQuest) {
        inspectingQuest?.let { q -> state.quests.find { it.id == q.id } ?: q }
    }

    // Safe completion dispatcher with duplicate-tap guard
    val handleCompleteQuest: (Long) -> Unit = { questId ->
        val quest = state.quests.find { it.id == questId }
        if (quest != null && !quest.isCompleted && !activePendingIds.contains(questId)) {
            pendingQuestIds = pendingQuestIds + questId
            onCompleteQuest(questId)
        }
    }

    // Metrics
    val activeCount = remember(state.quests) { state.quests.count { !it.isCompleted } }
    val completedCount = remember(state.quests) { state.quests.count { it.isCompleted } }
    val totalXpAvailable = remember(state.quests) {
        state.quests.filter { !it.isCompleted }.sumOf { it.xpReward }
    }
    val dailyQuests = remember(state.quests) {
        state.quests.filter { it.type == QuestType.DAILY }
    }

    // Tab filtering
    val filteredQuests = remember(state.quests, selectedTab) {
        when (selectedTab) {
            QuestTabFilter.ALL -> state.quests
            QuestTabFilter.DAILY -> state.quests.filter { it.type == QuestType.DAILY }
            QuestTabFilter.WEEKLY -> state.quests.filter { it.type == QuestType.WEEKLY }
            QuestTabFilter.LONG_TERM -> state.quests.filter { it.type == QuestType.LONG_TERM }
            QuestTabFilter.COMPLETED -> state.quests.filter { it.isCompleted }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SystemColors.ObsidianBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = SystemSpacing.maxContentWidth),
            contentPadding = PaddingValues(
                horizontal = SystemSpacing.screenHorizontal,
                vertical = SystemSpacing.screenVertical
            ),
            verticalArrangement = Arrangement.spacedBy(SystemSpacing.sectionSpacing)
        ) {
            // 1. Mission Control Header & Tactical Selector
            item {
                QuestCommandHeader(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    activeCount = activeCount,
                    completedCount = completedCount,
                    xpPoolAvailable = totalXpAvailable,
                    streakDays = state.player.streakDays,
                    onNewQuest = { showCreateDialog = true }
                )
            }

            // 2. Daily Protocol Highlight (Shown on ALL and DAILY tabs)
            if ((selectedTab == QuestTabFilter.ALL || selectedTab == QuestTabFilter.DAILY) && dailyQuests.isNotEmpty()) {
                item {
                    DailyQuestCommandSection(
                        dailyQuests = dailyQuests,
                        pendingQuestIds = activePendingIds,
                        onCompleteQuest = handleCompleteQuest,
                        onQuestClick = { inspectingQuest = it }
                    )
                }
            }

            // 3. Quests List
            if (filteredQuests.isEmpty()) {
                item {
                    SystemEmptyState(
                        title = when (selectedTab) {
                            QuestTabFilter.COMPLETED -> "No Cleared Quests"
                            QuestTabFilter.DAILY -> "No Daily Objectives"
                            QuestTabFilter.WEEKLY -> "No Weekly Directives"
                            QuestTabFilter.LONG_TERM -> "No Long-Term Milestones"
                            QuestTabFilter.ALL -> "No Active Missions"
                        },
                        description = when (selectedTab) {
                            QuestTabFilter.COMPLETED -> "Complete system objectives to populate your cleared mission archive."
                            else -> "Initialize a new custom quest or await system synchronization."
                        }
                    )
                }
            } else {
                items(filteredQuests, key = { it.id }) { quest ->
                    QuestCard(
                        quest = quest,
                        isCompleting = activePendingIds.contains(quest.id),
                        onComplete = { handleCompleteQuest(quest.id) },
                        onCardClick = { inspectingQuest = quest }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(SystemSpacing.verticalPageSpacing))
            }
        }
    }

    // Modal Bottom Sheet for Quest Inspection
    if (liveInspectingQuest != null) {
        QuestDetailBottomSheet(
            quest = liveInspectingQuest,
            isCompleting = activePendingIds.contains(liveInspectingQuest.id),
            onComplete = { questId ->
                handleCompleteQuest(questId)
            },
            onDismiss = { inspectingQuest = null }
        )
    }

    // Create Quest Dialog
    if (showCreateDialog) {
        CreateQuestDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { title, desc, type, category, target, unit ->
                onCreateQuest(title, desc, type, category, target, unit)
                showCreateDialog = false
            }
        )
    }
}
