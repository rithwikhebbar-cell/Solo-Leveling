package com.example.data.repository

import android.content.Context
import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.entity.SystemExperiencePreferenceEntity
import com.example.data.local.entity.toDomain
import com.example.data.local.entity.toEntity
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.preferences.VoiceCommandPermissions
import com.example.domain.engine.AwakeningScriptEngine
import com.example.domain.engine.CommandExecutionResult
import com.example.domain.engine.CommandExecutionStatus
import com.example.domain.engine.GeminiIntentEngine
import com.example.domain.engine.IntentResolutionSource
import com.example.domain.engine.PlayerContextEngine
import com.example.domain.engine.StructuredVoiceIntent
import com.example.domain.engine.SystemAnnouncementEngine
import com.example.domain.engine.SystemCelebrationEngine
import com.example.domain.engine.SystemCommandExecutor
import com.example.domain.engine.SystemPresentationCoordinator
import com.example.domain.engine.VoiceCommandProcessor
import com.example.domain.model.AiConsentState
import com.example.domain.model.AiContextScope
import com.example.domain.model.AiModelPreferenceState
import com.example.domain.model.AwakeningStage
import com.example.domain.model.CelebrationEvent
import com.example.domain.model.HunterRank
import com.example.domain.model.PlayerProfile
import com.example.domain.model.SupportedAiModels
import com.example.domain.model.SystemAnnouncementCategory
import com.example.domain.model.SystemAnnouncementRecord
import com.example.domain.model.SystemEvent
import com.example.domain.model.SystemEventType
import com.example.domain.model.SystemExperiencePreferences
import com.example.domain.model.SystemNotification
import com.example.domain.model.SystemNotificationChannelSpec
import com.example.domain.model.SystemNotificationPreferences
import com.example.domain.model.TtsSpeechPriority
import com.example.domain.model.VoiceCommandHistoryRecord
import com.example.domain.model.VoiceCommandResolution
import com.example.domain.model.VoiceEngineAvailability
import com.example.domain.model.WidgetSnapshot
import com.example.service.SystemAudioHapticFeedback
import com.example.service.SystemCueType
import com.example.service.VoiceOutputPort
import com.example.widget.WidgetSnapshotStore
import com.example.widget.WidgetUpdateCoordinator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class SystemExperienceRepository(
    private val context: Context,
    private val database: SoloLevelingDatabase,
    private val preferencesRepository: UserPreferencesRepository,
    val voiceEngine: VoiceOutputPort,
    private val geminiIntentEngine: GeminiIntentEngine? = null,
    private val systemCommandExecutor: SystemCommandExecutor? = null,
    private val aiSystemRepository: AiSystemRepository? = null
) {
    private val experienceDao = database.systemExperienceDao()
    private val playerDao = database.playerDao()
    private val aiConfigDao = database.aiConfigDao()

    private val _widgetSnapshot = MutableStateFlow(WidgetSnapshotStore.loadSnapshot(context))
    val widgetSnapshot: StateFlow<WidgetSnapshot> = _widgetSnapshot.asStateFlow()

    val voiceAvailability: StateFlow<VoiceEngineAvailability> = voiceEngine.availability
    val lastSpokenMessage: StateFlow<String?> = voiceEngine.lastSpokenMessage

    fun observePreferences(): Flow<SystemExperiencePreferences> =
        experienceDao.observePreferences().map { it?.toDomain() ?: SystemExperiencePreferences() }

    fun observeAnnouncements(): Flow<List<SystemAnnouncementRecord>> =
        experienceDao.observeAnnouncements(60).map { list -> list.map { it.toDomain() } }

    fun observeCelebrations(): Flow<List<CelebrationEvent>> =
        experienceDao.observeCelebrations(50).map { list -> list.map { it.toDomain() } }

    fun observeUnacknowledgedCelebrations(): Flow<List<CelebrationEvent>> =
        experienceDao.observeUnacknowledgedCelebrations().map { list -> list.map { it.toDomain() } }

    fun observeVoiceHistory(): Flow<List<VoiceCommandHistoryRecord>> =
        experienceDao.observeVoiceHistory(30).map { list -> list.map { it.toDomain() } }

    fun observeVoiceCommandPermissions(): Flow<VoiceCommandPermissions> =
        preferencesRepository.voiceCommandPermissionsFlow

    suspend fun updateVoiceCommandPermissions(
        transform: (VoiceCommandPermissions) -> VoiceCommandPermissions
    ) {
        preferencesRepository.updateVoiceCommandPermissions(transform)
    }

    suspend fun ensureInitialized() {
        val prefs = experienceDao.getPreferencesOnce()
        if (prefs == null) {
            val player = playerDao.getPlayerOnce()?.toDomain() ?: PlayerProfile()
            experienceDao.upsertPreferences(
                SystemExperiencePreferenceEntity(
                    hasCompletedAwakening = false,
                    lastCelebratedLevel = player.level,
                    lastCelebratedRankName = player.rank.name
                )
            )
        }
        val activePrefs = experienceDao.getPreferencesOnce()?.toDomain() ?: SystemExperiencePreferences()
        voiceEngine.updateMuted(!activePrefs.ttsEnabled)
        refreshWidgets()
    }

    suspend fun updatePreferences(transform: (SystemExperiencePreferences) -> SystemExperiencePreferences): SystemExperiencePreferences {
        val current = experienceDao.getPreferencesOnce()?.toDomain() ?: SystemExperiencePreferences()
        val updated = transform(current).copy(updatedAtEpochMs = System.currentTimeMillis())
        experienceDao.upsertPreferences(updated.toEntity())
        voiceEngine.updateMuted(!updated.ttsEnabled)
        return updated
    }

    suspend fun completeAwakening() {
        val prefs = updatePreferences { it.copy(hasCompletedAwakening = true) }
        val player = playerDao.getPlayerOnce()?.toDomain() ?: PlayerProfile()
        val finalLine = AwakeningScriptEngine.buildFormattedVoice(AwakeningStage.WELCOME_DIRECTIVE, player)
        if (prefs.ttsEnabled) {
            voiceEngine.speak(
                text = finalLine,
                priority = TtsSpeechPriority.NORMAL,
                speechRate = prefs.ttsSpeechRate,
                pitch = prefs.ttsPitch,
                localeTag = prefs.voiceLocaleTag
            )
        }
        SystemAudioHapticFeedback.triggerCue(
            context = context,
            cue = SystemCueType.AWAKENING_CHIME,
            soundEnabled = prefs.soundEffectsEnabled,
            hapticEnabled = prefs.hapticFeedbackEnabled,
            reducedMotionEnabled = prefs.reducedMotionEnabled
        )
    }

    suspend fun triggerAwakeningReplay() {
        updatePreferences { it.copy(hasCompletedAwakening = false) }
    }

    suspend fun speakAwakeningStage(stage: AwakeningStage, profile: PlayerProfile) {
        val prefs = experienceDao.getPreferencesOnce()?.toDomain() ?: SystemExperiencePreferences()
        val line = AwakeningScriptEngine.buildFormattedVoice(stage, profile)
        if (prefs.ttsEnabled && !prefs.reducedMotionEnabled) {
            voiceEngine.speak(
                text = line,
                priority = TtsSpeechPriority.NORMAL,
                speechRate = prefs.ttsSpeechRate,
                pitch = prefs.ttsPitch,
                localeTag = prefs.voiceLocaleTag,
                interruptCurrent = true
            )
        }
    }

    /**
     * Observes genuine progression transitions from persisted PlayerProfile without ever modifying XP.
     */
    suspend fun syncCelebrationsFromPlayerProfile(
        nowEpochMs: Long = System.currentTimeMillis()
    ): List<CelebrationEvent> {
        val player = playerDao.getPlayerOnce()?.toDomain() ?: return emptyList()
        val prefs = experienceDao.getPreferencesOnce()?.toDomain() ?: SystemExperiencePreferences()
        val lastRank = HunterRank.entries.firstOrNull { it.name == prefs.lastCelebratedRankName }
            ?: HunterRank.E_RANK
        val existingIds = experienceDao.getAllCelebrationIdsOnce().toSet()

        val newEvents = SystemCelebrationEngine.evaluateTransitions(
            currentProfile = player,
            lastCelebratedLevel = prefs.lastCelebratedLevel,
            lastCelebratedRank = lastRank,
            existingCelebrationIds = existingIds,
            nowEpochMs = nowEpochMs
        )

        if (newEvents.isEmpty()) {
            // Keep baseline synchronized downwards only if profile was reset cleanly
            if (player.level < prefs.lastCelebratedLevel || player.rank.ordinal < lastRank.ordinal) {
                updatePreferences {
                    it.copy(lastCelebratedLevel = player.level, lastCelebratedRankName = player.rank.name)
                }
            }
            return emptyList()
        }

        newEvents.forEach { celebration ->
            experienceDao.insertCelebration(celebration.toEntity())
            val announcement = SystemAnnouncementRecord(
                announcementKey = "CELEBRATION|${celebration.celebrationId}",
                category = if (celebration.type.name == "RANK_PROMOTION") {
                    SystemAnnouncementCategory.RANK_UP
                } else {
                    SystemAnnouncementCategory.LEVEL_UP
                },
                channelSpec = if (celebration.type.name == "RANK_PROMOTION") {
                    SystemNotificationChannelSpec.RANK_UP
                } else {
                    SystemNotificationChannelSpec.LEVEL_UP
                },
                title = celebration.headline,
                message = celebration.message,
                voiceSummary = celebration.message,
                deepLinkRoute = "status",
                actionLabel = "Open Dashboard",
                priority = TtsSpeechPriority.CRITICAL,
                createdAtEpochMs = celebration.createdAtEpochMs
            )
            recordAndPresentAnnouncement(announcement, triggerAudioHaptic = true)
        }

        updatePreferences {
            it.copy(
                lastCelebratedLevel = maxOf(it.lastCelebratedLevel, player.level),
                lastCelebratedRankName = if (player.rank.ordinal > lastRank.ordinal) player.rank.name else it.lastCelebratedRankName
            )
        }
        refreshWidgets()
        return newEvents
    }

    suspend fun acknowledgeCelebration(celebrationId: String) {
        experienceDao.acknowledgeCelebration(celebrationId)
    }

    suspend fun recordNotificationAnnouncement(notification: SystemNotification, dispatchedOs: Boolean = false) {
        val record = SystemAnnouncementEngine.fromNotification(notification, wasDispatchedOs = dispatchedOs)
        recordAndPresentAnnouncement(record, triggerAudioHaptic = true)
    }

    suspend fun onSystemEventReceived(event: SystemEvent) {
        val record = SystemAnnouncementEngine.fromSystemEvent(event)
        if (record != null) {
            recordAndPresentAnnouncement(record, triggerAudioHaptic = true)
        }
        when (event.eventType) {
            SystemEventType.PLAYER_PROFILE_UPDATED,
            SystemEventType.PHYSICAL_ACTIVITY_RECORDED,
            SystemEventType.WORKOUT_COMPLETED,
            SystemEventType.JOGGING_COMPLETED,
            SystemEventType.ACADEMIC_SESSION_COMPLETED,
            SystemEventType.REVISION_COMPLETED,
            SystemEventType.ASSESSMENT_RECORDED,
            SystemEventType.HABIT_COMPLETED,
            SystemEventType.ROUTINE_COMPLETED,
            SystemEventType.TASK_COMPLETED,
            SystemEventType.GOAL_MILESTONE_COMPLETED,
            SystemEventType.DUNGEON_COMPLETED,
            SystemEventType.BOSS_BATTLE_COMPLETED,
            SystemEventType.DAILY_RESET_COMPLETED -> {
                syncCelebrationsFromPlayerProfile(event.timestampEpochMs)
                refreshWidgets()
            }
            else -> Unit
        }
    }

    suspend fun recordAndPresentAnnouncement(
        record: SystemAnnouncementRecord,
        triggerAudioHaptic: Boolean = true
    ): Boolean {
        val expPrefs = experienceDao.getPreferencesOnce()?.toDomain() ?: SystemExperiencePreferences()
        val notifPrefs = aiConfigDao.getNotificationPreferencesOnce()?.toDomain() ?: SystemNotificationPreferences()
        val recentKeys = experienceDao.getAllAnnouncementKeysOnce().toSet()

        val effectiveNow = if (record.createdAtEpochMs > 0L) record.createdAtEpochMs else System.currentTimeMillis()
        val decision = SystemPresentationCoordinator.evaluatePresentation(
            record = record,
            experiencePrefs = expPrefs,
            notificationPrefs = notifPrefs,
            recentKeys = recentKeys,
            nowEpochMs = effectiveNow
        )
        if (!decision.shouldStoreAnnouncement) return false

        val insertedId = experienceDao.insertAnnouncement(
            record.copy(wasSpoken = decision.shouldSpeak && triggerAudioHaptic).toEntity()
        )
        if (insertedId <= 0L) return false

        if (triggerAudioHaptic) {
            if (decision.shouldSpeak) {
                voiceEngine.speak(
                    text = record.voiceSummary,
                    priority = decision.speechPriority,
                    speechRate = expPrefs.ttsSpeechRate,
                    pitch = expPrefs.ttsPitch,
                    localeTag = expPrefs.voiceLocaleTag,
                    interruptCurrent = decision.speechPriority == TtsSpeechPriority.CRITICAL
                )
            }
            val cue = when (record.category) {
                SystemAnnouncementCategory.RANK_UP -> SystemCueType.RANK_PROMOTION
                SystemAnnouncementCategory.LEVEL_UP -> SystemCueType.LEVEL_UP
                SystemAnnouncementCategory.RECOVERY -> SystemCueType.ALERT_WARNING
                else -> SystemCueType.QUEST_COMPLETED
            }
            SystemAudioHapticFeedback.triggerCue(
                context = context,
                cue = cue,
                soundEnabled = decision.shouldPlaySound,
                hapticEnabled = decision.shouldTriggerHaptic,
                reducedMotionEnabled = expPrefs.reducedMotionEnabled
            )
        }
        return true
    }

    suspend fun markAnnouncementRead(id: Long) {
        experienceDao.markAnnouncementRead(id)
    }

    suspend fun markAllAnnouncementsRead() {
        experienceDao.markAllAnnouncementsRead()
    }

    suspend fun executeVoiceTranscript(
        transcript: String,
        confirmedSensitiveAction: Boolean = false
    ): VoiceCommandResolution {
        val expPrefs = experienceDao.getPreferencesOnce()?.toDomain() ?: SystemExperiencePreferences()
        val resolution = VoiceCommandProcessor.process(transcript)
        val outcomeLabel = when {
            !resolution.matched -> "UNRECOGNIZED"
            resolution.requiresConfirmation && !confirmedSensitiveAction -> "AWAITING_CONFIRMATION"
            else -> "EXECUTED:${resolution.action?.name}"
        }
        experienceDao.insertVoiceHistory(
            VoiceCommandHistoryRecord(
                transcript = transcript.trim(),
                matchedActionName = resolution.action?.label ?: "No Match",
                outcome = outcomeLabel,
                executedAtEpochMs = System.currentTimeMillis()
            ).toEntity()
        )
        if (expPrefs.ttsEnabled && (!resolution.requiresConfirmation || confirmedSensitiveAction)) {
            voiceEngine.speak(
                text = resolution.responseText,
                priority = TtsSpeechPriority.HIGH,
                speechRate = expPrefs.ttsSpeechRate,
                pitch = expPrefs.ttsPitch,
                localeTag = expPrefs.voiceLocaleTag,
                interruptCurrent = true
            )
        }
        if (resolution.matched) {
            SystemAudioHapticFeedback.triggerCue(
                context = context,
                cue = SystemCueType.VOICE_COMMAND_ACK,
                soundEnabled = expPrefs.soundEffectsEnabled,
                hapticEnabled = expPrefs.hapticFeedbackEnabled,
                reducedMotionEnabled = expPrefs.reducedMotionEnabled
            )
        }
        return resolution
    }

    /**
     * Gemini-powered natural language voice command pipeline:
     * Transcript -> GeminiIntentEngine (schema-validated) -> SystemCommandExecutor (permission/safety/confirmation checked) -> TTS feedback.
     */
    suspend fun executeGeminiVoiceCommand(
        transcript: String,
        confirmedIntent: StructuredVoiceIntent? = null
    ): CommandExecutionResult {
        val expPrefs = experienceDao.getPreferencesOnce()?.toDomain() ?: SystemExperiencePreferences()
        val intentEngine = geminiIntentEngine
        val executor = systemCommandExecutor
        if (intentEngine == null || executor == null) {
            val legacy = executeVoiceTranscript(transcript, confirmedSensitiveAction = confirmedIntent != null)
            return CommandExecutionResult(
                status = when {
                    !legacy.matched -> CommandExecutionStatus.NEEDS_CLARIFICATION
                    legacy.requiresConfirmation && confirmedIntent == null -> CommandExecutionStatus.AWAITING_CONFIRMATION
                    else -> CommandExecutionStatus.EXECUTED
                },
                commandLabel = legacy.action?.label ?: "Legacy Voice Command",
                spokenResponse = legacy.responseText,
                displayDetails = legacy.responseText,
                deepLinkRoute = legacy.deepLinkRoute,
                source = IntentResolutionSource.OFFLINE_LOCAL
            )
        }

        val intent = if (confirmedIntent != null) {
            confirmedIntent
        } else {
            val modelPrefs = aiConfigDao.getModelPreferencesOnce()?.toDomain() ?: AiModelPreferenceState()
            val consent = aiConfigDao.getConsentRecordOnce()?.toDomain() ?: AiConsentState()
            val contextBrief = runCatching {
                aiSystemRepository?.buildContextSnapshot(AiContextScope.FULL)?.let {
                    PlayerContextEngine.formatCompactPromptContext(it)
                }.orEmpty()
            }.getOrDefault("")
            intentEngine.interpret(
                rawTranscript = transcript,
                contextSummary = contextBrief,
                cloudAiAllowed = modelPrefs.aiEnabled && consent.cloudAiConsentGranted,
                backendBaseUrl = modelPrefs.backendBaseUrl,
                preferredModelId = SupportedAiModels.FAST_INTENT_MODEL_ID
            )
        }

        val result = executor.execute(
            intent = intent,
            confirmed = confirmedIntent != null
        )

        experienceDao.insertVoiceHistory(
            VoiceCommandHistoryRecord(
                transcript = transcript.trim(),
                matchedActionName = "${result.commandLabel} [${result.source.label}]",
                outcome = "${result.status.name}:${result.commandId?.wireId ?: "NONE"}",
                executedAtEpochMs = System.currentTimeMillis()
            ).toEntity()
        )

        if (expPrefs.ttsEnabled && result.spokenResponse.isNotBlank()) {
            voiceEngine.speak(
                text = stripMarkdownForSpeech(result.spokenResponse),
                priority = TtsSpeechPriority.HIGH,
                speechRate = expPrefs.ttsSpeechRate,
                pitch = expPrefs.ttsPitch,
                localeTag = expPrefs.voiceLocaleTag,
                interruptCurrent = true
            )
        }
        if (result.status == CommandExecutionStatus.EXECUTED) {
            SystemAudioHapticFeedback.triggerCue(
                context = context,
                cue = SystemCueType.VOICE_COMMAND_ACK,
                soundEnabled = expPrefs.soundEffectsEnabled,
                hapticEnabled = expPrefs.hapticFeedbackEnabled,
                reducedMotionEnabled = expPrefs.reducedMotionEnabled
            )
        }
        return result
    }

    /**
     * Multi-turn Voice Conversation Mode turn backed by AiSystemRepository (shares chat history with AI Companion)
     * and SystemVoiceEngine (TTS).
     */
    suspend fun executeVoiceConversationTurn(
        transcript: String,
        conversationId: Long?,
        onStreamToken: (String) -> Unit = {}
    ): Pair<Long?, String> {
        val aiRepo = aiSystemRepository ?: return null to "AI Companion service unavailable."
        val expPrefs = experienceDao.getPreferencesOnce()?.toDomain() ?: SystemExperiencePreferences()
        val dispatch = aiRepo.sendChatMessage(
            conversationId = conversationId,
            userPrompt = transcript,
            onStreamToken = onStreamToken
        )
        val reply = dispatch.assistantMessage.content

        experienceDao.insertVoiceHistory(
            VoiceCommandHistoryRecord(
                transcript = transcript.trim(),
                matchedActionName = "Voice Conversation [${dispatch.assistantMessage.modelUsed}]",
                outcome = "CONVERSATION:${dispatch.connectionStatus.name}",
                executedAtEpochMs = System.currentTimeMillis()
            ).toEntity()
        )

        if (expPrefs.ttsEnabled && reply.isNotBlank()) {
            voiceEngine.speak(
                text = stripMarkdownForSpeech(reply).take(600),
                priority = TtsSpeechPriority.HIGH,
                speechRate = expPrefs.ttsSpeechRate,
                pitch = expPrefs.ttsPitch,
                localeTag = expPrefs.voiceLocaleTag,
                interruptCurrent = true
            )
        }
        return dispatch.conversationId to reply
    }

    private fun stripMarkdownForSpeech(raw: String): String = raw
        .replace(Regex("```[\\s\\S]*?```"), " code block omitted ")
        .replace(Regex("[*_`#>]"), "")
        .replace(Regex("\\s+"), " ")
        .trim()

    suspend fun clearVoiceHistory() {
        experienceDao.clearVoiceHistory()
    }

    suspend fun speakCustomText(
        text: String,
        priority: TtsSpeechPriority = TtsSpeechPriority.NORMAL,
        interruptCurrent: Boolean = false
    ) {
        val expPrefs = experienceDao.getPreferencesOnce()?.toDomain() ?: SystemExperiencePreferences()
        if (!expPrefs.ttsEnabled) return
        voiceEngine.speak(
            text = text,
            priority = priority,
            speechRate = expPrefs.ttsSpeechRate,
            pitch = expPrefs.ttsPitch,
            localeTag = expPrefs.voiceLocaleTag,
            interruptCurrent = interruptCurrent
        )
    }

    suspend fun testSystemVoice() {
        val player = playerDao.getPlayerOnce()?.toDomain() ?: PlayerProfile()
        val expPrefs = experienceDao.getPreferencesOnce()?.toDomain() ?: SystemExperiencePreferences()
        voiceEngine.updateMuted(false)
        voiceEngine.speak(
            text = "System voice calibration online. Player ${player.codename}, ${player.rank.displayName}, Level ${player.level}.",
            priority = TtsSpeechPriority.HIGH,
            speechRate = expPrefs.ttsSpeechRate,
            pitch = expPrefs.ttsPitch,
            localeTag = expPrefs.voiceLocaleTag,
            interruptCurrent = true
        )
    }

    fun stopVoice() {
        voiceEngine.stop()
    }

    suspend fun refreshWidgets(): WidgetSnapshot {
        val userPrefs = preferencesRepository.preferencesFlow.first()
        val snapshot = WidgetUpdateCoordinator.refreshAllWidgets(
            context = context,
            db = database,
            academicGoalMinutes = userPrefs.academicDailyGoalMinutes
        )
        _widgetSnapshot.value = snapshot
        return snapshot
    }
}
