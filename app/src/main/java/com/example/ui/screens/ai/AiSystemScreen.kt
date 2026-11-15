package com.example.ui.screens.ai

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.AiConnectionStatus
import com.example.domain.model.PlanMode
import com.example.domain.model.SupportedAiModels
import com.example.domain.model.SystemNotificationPreferences
import com.example.ui.components.HolographicPanel
import com.example.ui.components.SystemErrorState
import com.example.ui.components.SystemLoadingView
import com.example.ui.theme.SystemColors
import com.example.ui.viewmodel.AiSystemUiState

enum class AiAdminTab(val label: String) {
    GAME_MASTER("Game Master"),
    INSIGHTS("Insights"),
    QUESTS("Quest Proposals"),
    PLANNER("Daily Plan"),
    SETTINGS("AI & Alerts")
}

@Composable
fun AiSystemScreen(
    uiState: AiSystemUiState,
    onRetryInit: () -> Unit,
    onRefreshAnalysis: () -> Unit,
    onSelectConversation: (Long) -> Unit,
    onNewConversation: () -> Unit,
    onDeleteConversation: (Long) -> Unit,
    onClearAllConversations: () -> Unit,
    onSendChatMessage: (String) -> Unit,
    onRetryLastPrompt: () -> Unit,
    onGenerateQuestProposals: () -> Unit,
    onApproveQuestProposal: (Long, String?, String?, Int?) -> Unit,
    onRejectQuestProposal: (Long) -> Unit,
    onSelectPlanMode: (PlanMode) -> Unit,
    onGeneratePlan: () -> Unit,
    onApprovePlanItem: (Long, String?, Int?, Int?) -> Unit,
    onRejectPlanItem: (Long) -> Unit,
    onSelectModel: (String) -> Unit,
    onSaveBackendUrl: (String) -> Unit,
    onSetAiEnabled: (Boolean) -> Unit,
    onSetStreamingEnabled: (Boolean) -> Unit,
    onUpdateConsent: (Boolean?, Boolean?, Boolean?, Boolean?) -> Unit,
    onUpdateNotificationPrefs: ((SystemNotificationPreferences) -> SystemNotificationPreferences) -> Unit,
    onClearFeedback: () -> Unit,
    onTriggerNotificationEvaluation: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableStateOf(AiAdminTab.GAME_MASTER) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SystemColors.ObsidianBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        when (uiState) {
            is AiSystemUiState.Loading -> SystemLoadingView()
            is AiSystemUiState.Error -> Box(modifier = Modifier.padding(20.dp)) {
                SystemErrorState(message = uiState.message, onRetry = onRetryInit)
            }
            is AiSystemUiState.Ready -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 820.dp)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Section A: System Header
                    AiSystemHeader(
                        state = uiState,
                        onRefreshAnalysis = onRefreshAnalysis,
                        onClearFeedback = onClearFeedback
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(AiAdminTab.entries) { tab ->
                            val selected = tab == selectedTab
                            FilterChip(
                                selected = selected,
                                onClick = { selectedTab = tab },
                                label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SystemColors.ArcaneViolet.copy(alpha = 0.25f),
                                    selectedLabelColor = SystemColors.ElectricCyan,
                                    containerColor = SystemColors.DeepNavy,
                                    labelColor = SystemColors.MutedSilver
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selected,
                                    borderColor = SystemColors.SubtleGridLine,
                                    selectedBorderColor = SystemColors.ElectricCyan
                                ),
                                modifier = Modifier.testTag("ai_tab_${tab.name.lowercase()}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (selectedTab) {
                            AiAdminTab.GAME_MASTER -> AiChatSection(
                                state = uiState,
                                onSelectConversation = onSelectConversation,
                                onNewConversation = onNewConversation,
                                onSendMessage = onSendChatMessage,
                                onRetryLastPrompt = onRetryLastPrompt
                            )
                            AiAdminTab.INSIGHTS -> AiInsightsSection(
                                state = uiState,
                                onRefreshAnalysis = onRefreshAnalysis
                            )
                            AiAdminTab.QUESTS -> AiQuestProposalsSection(
                                state = uiState,
                                onGenerateProposals = onGenerateQuestProposals,
                                onApproveProposal = onApproveQuestProposal,
                                onRejectProposal = onRejectQuestProposal
                            )
                            AiAdminTab.PLANNER -> AiDailyPlannerSection(
                                state = uiState,
                                onSelectPlanMode = onSelectPlanMode,
                                onGeneratePlan = onGeneratePlan,
                                onApprovePlanItem = onApprovePlanItem,
                                onRejectPlanItem = onRejectPlanItem
                            )
                            AiAdminTab.SETTINGS -> {
                                LocalContext.current
                                AiSettingsSection(
                                    state = uiState,
                                    onSelectModel = onSelectModel,
                                    onSaveBackendUrl = onSaveBackendUrl,
                                    onSetAiEnabled = onSetAiEnabled,
                                    onSetStreamingEnabled = onSetStreamingEnabled,
                                    onUpdateConsent = onUpdateConsent,
                                    onDeleteConversation = onDeleteConversation,
                                    onClearAllConversations = onClearAllConversations,
                                    onUpdateNotificationPrefs = onUpdateNotificationPrefs,
                                    onTriggerNotificationEvaluation = onTriggerNotificationEvaluation
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AiSystemHeader(
    state: AiSystemUiState.Ready,
    onRefreshAnalysis: () -> Unit,
    onClearFeedback: () -> Unit
) {
    val modelOption = SupportedAiModels.findById(state.modelPreferences.selectedModelId)
    val statusColor = when (state.connectionStatus) {
        AiConnectionStatus.CONNECTED, AiConnectionStatus.STREAMING -> SystemColors.VerifiedEmerald
        AiConnectionStatus.CONNECTING -> SystemColors.ElectricCyan
        AiConnectionStatus.AUTH_ERROR, AiConnectionStatus.PROVIDER_ERROR -> SystemColors.DangerCrimson
        AiConnectionStatus.RATE_LIMITED -> SystemColors.MonarchGold
        else -> SystemColors.SystemBlue
    }

    HolographicPanel(
        modifier = Modifier.fillMaxWidth(),
        accentColor = SystemColors.ArcaneViolet,
        secondaryColor = SystemColors.ElectricCyan
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = "System Administrator",
                        tint = SystemColors.ElectricCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "SYSTEM ADMINISTRATOR",
                            style = MaterialTheme.typography.titleMedium,
                            color = SystemColors.PureIce
                        )
                        Text(
                            text = "${modelOption.displayName} • Advisory Only (0 direct XP control)",
                            style = MaterialTheme.typography.bodySmall,
                            color = SystemColors.MutedSilver,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(
                    onClick = onRefreshAnalysis,
                    modifier = Modifier.testTag("ai_refresh_analysis_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Refresh analysis",
                        tint = SystemColors.ElectricCyan
                    )
                }
            }

            Surface(
                color = statusColor.copy(alpha = 0.14f),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(0.75.dp, statusColor.copy(alpha = 0.55f))
            ) {
                Text(
                    text = "${state.connectionStatus.label} • Consent: ${if (state.consentState.cloudAiConsentGranted) "Cloud Enabled" else "Local Only"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }

            if (state.feedbackMessage != null) {
                Surface(
                    color = SystemColors.ElectricCyan.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(0.75.dp, SystemColors.ElectricCyan.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = state.feedbackMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = SystemColors.PureIce,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onClearFeedback, modifier = Modifier.size(24.dp)) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Dismiss feedback",
                                tint = SystemColors.MutedSilver,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
