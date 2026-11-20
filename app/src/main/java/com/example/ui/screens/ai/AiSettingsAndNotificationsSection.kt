package com.example.ui.screens.ai

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.domain.model.SupportedAiModels
import com.example.domain.model.SystemNotificationPreferences
import com.example.ui.components.HolographicPanel
import com.example.ui.theme.SystemColors
import com.example.ui.viewmodel.AiSystemUiState
import com.example.worker.SystemNotificationWorker

@Composable
fun AiSettingsSection(
    state: AiSystemUiState.Ready,
    onSelectModel: (String) -> Unit,
    onSaveBackendUrl: (String) -> Unit,
    onSetAiEnabled: (Boolean) -> Unit,
    onSetStreamingEnabled: (Boolean) -> Unit,
    onUpdateConsent: (Boolean?, Boolean?, Boolean?, Boolean?) -> Unit,
    onDeleteConversation: (Long) -> Unit,
    onClearAllConversations: () -> Unit,
    onUpdateNotificationPrefs: ((SystemNotificationPreferences) -> SystemNotificationPreferences) -> Unit,
    onTriggerNotificationEvaluation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var backendUrlInput by rememberSaveable(state.modelPreferences.backendBaseUrl) {
        mutableStateOf(state.modelPreferences.backendBaseUrl)
    }
    var customModelInput by rememberSaveable { mutableStateOf("") }
    var notifPermissionGranted by remember {
        mutableStateOf(SystemNotificationWorker.hasNotificationPermission(context))
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notifPermissionGranted = granted || SystemNotificationWorker.hasNotificationPermission(context)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        // 1. Google Gemini AI Engine & Optional Custom Proxy URL
        item {
            HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.ArcaneViolet) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("GEMINI AI ENGINE & SECURE PROXY", style = MaterialTheme.typography.titleMedium, color = SystemColors.PureIce)
                    Text(
                        text = "Powered by Google Gemini via Firebase AI Logic + App Check with zero client-side API keys. Optionally specify a self-hosted Supabase Edge Function URL to route through your own Gemini backend proxy.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SystemColors.MutedSilver
                    )

                    SettingSwitchRow(
                        label = "Enable AI Assistant",
                        checked = state.modelPreferences.aiEnabled,
                        onCheckedChange = onSetAiEnabled,
                        testTag = "ai_master_toggle"
                    )
                    SettingSwitchRow(
                        label = "Streaming Responses",
                        checked = state.modelPreferences.streamingEnabled,
                        onCheckedChange = onSetStreamingEnabled,
                        testTag = "ai_streaming_toggle"
                    )

                    OutlinedTextField(
                        value = backendUrlInput,
                        onValueChange = { backendUrlInput = it },
                        label = { Text("Optional Custom Proxy URL (leave blank for Firebase AI Logic)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("ai_backend_url_input")
                    )
                    Button(
                        onClick = { onSaveBackendUrl(backendUrlInput) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SystemColors.ElectricCyan,
                            contentColor = SystemColors.ObsidianBlack
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("ai_save_backend_url_button")
                    ) {
                        Text("Save Backend Proxy URL")
                    }

                    Text("Select Google Gemini Model", style = MaterialTheme.typography.labelLarge, color = SystemColors.ElectricCyan)
                    SupportedAiModels.catalog.forEach { model ->
                        val selected = SupportedAiModels.resolveGeminiModelId(state.modelPreferences.selectedModelId) == model.id ||
                            state.modelPreferences.selectedModelId == model.id
                        Surface(
                            color = if (selected) SystemColors.ArcaneViolet.copy(alpha = 0.16f) else SystemColors.ObsidianBlack.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(0.75.dp, if (selected) SystemColors.ElectricCyan else SystemColors.SubtleGridLine),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectModel(model.id) }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selected,
                                    onClick = { onSelectModel(model.id) },
                                    colors = RadioButtonDefaults.colors(selectedColor = SystemColors.ElectricCyan)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(model.displayName, style = MaterialTheme.typography.titleSmall, color = SystemColors.PureIce)
                                    Text(
                                        text = "${model.id} • ${model.provider}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SystemColors.ElectricCyan
                                    )
                                    Text(
                                        text = model.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SystemColors.MutedSilver
                                    )
                                }
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = customModelInput,
                            onValueChange = { customModelInput = it },
                            label = { Text("Custom Proxy Model ID") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick = {
                                if (customModelInput.isNotBlank()) {
                                    onSelectModel(customModelInput)
                                    customModelInput = ""
                                }
                            }
                        ) {
                            Text("Use")
                        }
                    }
                }
            }
        }

        // 2. Privacy & Consent Governance
        item {
            HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.VerifiedEmerald) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("PRIVACY & TELEMETRY CONSENT", style = MaterialTheme.typography.titleMedium, color = SystemColors.VerifiedEmerald)
                    SettingSwitchRow(
                        label = "Allow Cloud AI Requests (via Backend)",
                        checked = state.consentState.cloudAiConsentGranted,
                        onCheckedChange = { onUpdateConsent(it, null, null, null) },
                        testTag = "ai_cloud_consent_toggle"
                    )
                    SettingSwitchRow(
                        label = "Share Physical & Recovery Summary",
                        checked = state.consentState.sharePhysicalWithAi,
                        onCheckedChange = { onUpdateConsent(null, it, null, null) },
                        testTag = "ai_share_physical_toggle"
                    )
                    SettingSwitchRow(
                        label = "Share Academic & Revision Summary",
                        checked = state.consentState.shareAcademicWithAi,
                        onCheckedChange = { onUpdateConsent(null, null, it, null) },
                        testTag = "ai_share_academic_toggle"
                    )
                    SettingSwitchRow(
                        label = "Share Quests Summary",
                        checked = state.consentState.shareQuestsWithAi,
                        onCheckedChange = { onUpdateConsent(null, null, null, it) },
                        testTag = "ai_share_quests_toggle"
                    )
                }
            }
        }

        // 3. System Notifications & Quiet Hours
        item {
            val np = state.notificationPreferences
            HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.ElectricCyan) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("SYSTEM NOTIFICATIONS & QUIET HOURS", style = MaterialTheme.typography.titleMedium, color = SystemColors.ElectricCyan)

                    if (!notifPermissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Button(
                            onClick = { notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SystemColors.MonarchGold,
                                contentColor = SystemColors.ObsidianBlack
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("grant_notification_permission_button")
                        ) {
                            Icon(Icons.Rounded.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Grant Android Notification Permission")
                        }
                    }

                    SettingSwitchRow("System Notifications Enabled", np.notificationsEnabled, { onUpdateNotificationPrefs { cur -> cur.copy(notificationsEnabled = it) } })
                    SettingSwitchRow("Recovery Warnings (Safety Priority)", np.enableRecoveryWarnings, { onUpdateNotificationPrefs { cur -> cur.copy(enableRecoveryWarnings = it) } })
                    SettingSwitchRow("Daily Quest Reminders", np.enableDailyQuestReminders, { onUpdateNotificationPrefs { cur -> cur.copy(enableDailyQuestReminders = it) } })
                    SettingSwitchRow("Overdue Revision Alerts", np.enableOverdueRevisions, { onUpdateNotificationPrefs { cur -> cur.copy(enableOverdueRevisions = it) } })
                    SettingSwitchRow("Upcoming Exam Alerts", np.enableUpcomingExams, { onUpdateNotificationPrefs { cur -> cur.copy(enableUpcomingExams = it) } })
                    SettingSwitchRow("Workout Window Reminders", np.enableWorkoutReminders, { onUpdateNotificationPrefs { cur -> cur.copy(enableWorkoutReminders = it) } })
                    SettingSwitchRow("AI Advisory Recommendations", np.enableAiRecommendations, { onUpdateNotificationPrefs { cur -> cur.copy(enableAiRecommendations = it) } })
                    SettingSwitchRow("Daily Summary Alerts", np.enableDailySummary, { onUpdateNotificationPrefs { cur -> cur.copy(enableDailySummary = it) } })
                    SettingSwitchRow("Habit Reminders", np.enableHabitReminders, { onUpdateNotificationPrefs { cur -> cur.copy(enableHabitReminders = it) } })
                    SettingSwitchRow("Routine Reminders", np.enableRoutineReminders, { onUpdateNotificationPrefs { cur -> cur.copy(enableRoutineReminders = it) } })
                    SettingSwitchRow("Personal Task Deadlines", np.enableTaskDeadlines, { onUpdateNotificationPrefs { cur -> cur.copy(enableTaskDeadlines = it) } })
                    SettingSwitchRow("Overdue Task Alerts", np.enableOverdueTasks, { onUpdateNotificationPrefs { cur -> cur.copy(enableOverdueTasks = it) } })
                    SettingSwitchRow("Goal Milestone Deadlines", np.enableGoalMilestones, { onUpdateNotificationPrefs { cur -> cur.copy(enableGoalMilestones = it) } })
                    SettingSwitchRow("Daily Planning Reminder", np.enableDailyPlanningReminders, { onUpdateNotificationPrefs { cur -> cur.copy(enableDailyPlanningReminders = it) } })
                    SettingSwitchRow("Weekly Review Reminder", np.enableWeeklyReviewReminders, { onUpdateNotificationPrefs { cur -> cur.copy(enableWeeklyReviewReminders = it) } })

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Quiet Hours: %02d:00 – %02d:00".format(np.quietHoursStartHour, np.quietHoursEndHour),
                            style = MaterialTheme.typography.bodyMedium,
                            color = SystemColors.PureIce
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = {
                                onUpdateNotificationPrefs { cur -> cur.copy(quietHoursStartHour = (cur.quietHoursStartHour + 1) % 24) }
                            }) { Text("Start +1h") }
                            OutlinedButton(onClick = {
                                onUpdateNotificationPrefs { cur -> cur.copy(quietHoursEndHour = (cur.quietHoursEndHour + 1) % 24) }
                            }) { Text("End +1h") }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Daily Quota Cap: ${np.maxNotificationsPerDay}/day",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SystemColors.PureIce
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = {
                                onUpdateNotificationPrefs { cur -> cur.copy(maxNotificationsPerDay = (cur.maxNotificationsPerDay - 1).coerceAtLeast(1)) }
                            }) { Text("-") }
                            OutlinedButton(onClick = {
                                onUpdateNotificationPrefs { cur -> cur.copy(maxNotificationsPerDay = (cur.maxNotificationsPerDay + 1).coerceAtMost(15)) }
                            }) { Text("+") }
                        }
                    }

                    Button(
                        onClick = onTriggerNotificationEvaluation,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SystemColors.SystemBlue,
                            contentColor = SystemColors.PureIce
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("ai_evaluate_notifications_button")
                    ) {
                        Text("Evaluate & Dispatch System Alerts Now")
                    }
                }
            }
        }

        // 4. Conversation History Management & Audits
        item {
            HolographicPanel(modifier = Modifier.fillMaxWidth(), accentColor = SystemColors.MutedSilver) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("CONVERSATION MEMORY (${state.conversations.size})", style = MaterialTheme.typography.titleSmall, color = SystemColors.PureIce)
                        if (state.conversations.isNotEmpty()) {
                            OutlinedButton(onClick = onClearAllConversations, modifier = Modifier.testTag("ai_clear_history_button")) {
                                Text("Clear All", color = SystemColors.DangerCrimson)
                            }
                        }
                    }
                    state.conversations.forEach { conv ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(conv.title, style = MaterialTheme.typography.bodySmall, color = SystemColors.MetallicSilver, modifier = Modifier.weight(1f))
                            IconButton(onClick = { onDeleteConversation(conv.id) }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Delete conversation", tint = SystemColors.DangerCrimson, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    if (state.recentAudits.isNotEmpty()) {
                        Text(
                            text = "Recent AI Requests: ${state.recentAudits.take(3).joinToString(" | ") { "${it.requestType}:${if (it.usedCloudBackend) "Cloud" else "Local"}(${it.latencyMs}ms)" }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SystemColors.MutedSilver
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = SystemColors.PureIce, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = SystemColors.ElectricCyan, checkedThumbColor = SystemColors.ObsidianBlack),
            modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier
        )
    }
}
