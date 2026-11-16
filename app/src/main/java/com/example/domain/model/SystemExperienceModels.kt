package com.example.domain.model

import kotlinx.serialization.Serializable

enum class AwakeningStage(
    val stepIndex: Int,
    val codeLabel: String,
    val headline: String,
    val bodyTemplate: String,
    val voiceScript: String
) {
    INITIALIZATION(
        stepIndex = 0,
        codeLabel = "BOOT // SEQUENCE 01",
        headline = "SYSTEM INITIALIZATION",
        bodyTemplate = "Synchronizing deterministic telemetry, biometric channels, and neural interface.",
        voiceScript = "System initializing. Calibrating Reality Operating System core."
    ),
    PLAYER_IDENTIFICATION(
        stepIndex = 1,
        codeLabel = "IDENT // SEQUENCE 02",
        headline = "PLAYER DETECTED",
        bodyTemplate = "Host profile verified: %CODENAME%. Eligibility confirmed for real-world progression tracking.",
        voiceScript = "Player identified. Welcome, %CODENAME%."
    ),
    CORE_BRIEFING(
        stepIndex = 2,
        codeLabel = "PROTOCOL // SEQUENCE 03",
        headline = "REALITY RULEBOOK",
        bodyTemplate = "Seven attributes grow exclusively from verified physical training, study focus, habits, and disciplined execution.",
        voiceScript = "All progression reflects verified real-world action."
    ),
    RANK_CALIBRATION(
        stepIndex = 3,
        codeLabel = "CALIBRATION // SEQUENCE 04",
        headline = "HUNTER RANK ASSIGNED",
        bodyTemplate = "Current classification: %RANK% — Level %LEVEL%. Advance through consistent verified sessions and recovery stewardship.",
        voiceScript = "Current classification: %RANK%, Level %LEVEL%."
    ),
    WELCOME_DIRECTIVE(
        stepIndex = 4,
        codeLabel = "ONLINE // SEQUENCE 05",
        headline = "THE AWAKENING COMPLETE",
        bodyTemplate = "System HUD, Daily Quests, AI Companion, and Adaptive Planning are now online.",
        voiceScript = "Welcome to Solo Leveling Reality OS. Ready for today's objectives."
    )
}

enum class TtsSpeechPriority(val weight: Int) {
    LOW(1),
    NORMAL(2),
    HIGH(3),
    CRITICAL(4)
}

enum class SystemNotificationChannelSpec(
    val channelId: String,
    val displayName: String,
    val descriptionText: String,
    val importanceWeight: Int
) {
    QUEST_AVAILABLE(
        channelId = "sl_quest_available",
        displayName = "Quest Available",
        descriptionText = "New daily quests, habit/routine prompts, and training reminders.",
        importanceWeight = 3
    ),
    QUEST_COMPLETED(
        channelId = "sl_quest_completed",
        displayName = "Quest Completed",
        descriptionText = "Confirmations when objectives and milestones are cleared.",
        importanceWeight = 2
    ),
    LEVEL_UP(
        channelId = "sl_level_up",
        displayName = "Level Up",
        descriptionText = "Deterministic level advancement notifications.",
        importanceWeight = 4
    ),
    RANK_UP(
        channelId = "sl_rank_up",
        displayName = "Rank Promotion",
        descriptionText = "Hunter Rank promotion milestones and title unlocks.",
        importanceWeight = 4
    ),
    DUNGEON_AVAILABLE(
        channelId = "sl_dungeon_available",
        displayName = "Dungeon Gates",
        descriptionText = "Dungeon gate discovery, progression, and completion updates.",
        importanceWeight = 3
    ),
    BOSS_BATTLE(
        channelId = "sl_boss_battle",
        displayName = "Boss Battles",
        descriptionText = "Multi-stage raid phase transitions and campaign victories.",
        importanceWeight = 3
    ),
    RECOVERY_WARNING(
        channelId = "sl_recovery_warning",
        displayName = "Recovery & Safety Warnings",
        descriptionText = "High fatigue, elevated training load, and mandatory rest advisories.",
        importanceWeight = 4
    ),
    DAILY_REPORT(
        channelId = "sl_daily_report",
        displayName = "Daily Summary & Advisories",
        descriptionText = "Daily development summaries, plans, and AI Companion briefings.",
        importanceWeight = 2
    ),
    SYSTEM_ALERT(
        channelId = "sl_system_alert",
        displayName = "System Alerts",
        descriptionText = "Sync updates, integrity advisories, and core System alerts.",
        importanceWeight = 3
    )
}

enum class SystemAnnouncementCategory(val label: String) {
    QUEST_AVAILABLE("Quest Available"),
    QUEST_COMPLETED("Quest Completed"),
    LEVEL_UP("Level Up"),
    RANK_UP("Rank Promotion"),
    DUNGEON("Dungeon Gate"),
    BOSS("Boss Raid"),
    RECOVERY("Recovery Warning"),
    RECOMMENDATION("System Recommendation"),
    DAILY_SUMMARY("Daily Summary"),
    SYSTEM_ALERT("System Alert")
}

data class SystemAnnouncementRecord(
    val id: Long = 0,
    val announcementKey: String,
    val eventId: String = "",
    val category: SystemAnnouncementCategory,
    val channelSpec: SystemNotificationChannelSpec,
    val title: String,
    val message: String,
    val voiceSummary: String,
    val deepLinkRoute: String = "home",
    val actionLabel: String = "Open Dashboard",
    val priority: TtsSpeechPriority = TtsSpeechPriority.NORMAL,
    val wasSpoken: Boolean = false,
    val wasDispatchedOs: Boolean = false,
    val isRead: Boolean = false,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

enum class CelebrationType(val label: String) {
    LEVEL_UP("LEVEL UP"),
    RANK_PROMOTION("RANK PROMOTION")
}

data class CelebrationEvent(
    val celebrationId: String,
    val type: CelebrationType,
    val previousLevel: Int,
    val newLevel: Int,
    val previousRankName: String,
    val newRankName: String,
    val titleUnlocked: String = "",
    val attributeDeltaSummary: String = "",
    val headline: String,
    val message: String,
    val isAcknowledged: Boolean = false,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

enum class VoiceCommandActionType(
    val label: String,
    val deepLinkRoute: String,
    val requiresConfirmation: Boolean = false,
    val examplePhrases: List<String> = emptyList()
) {
    OPEN_STATUS(
        label = "Open Status Window",
        deepLinkRoute = "status",
        examplePhrases = listOf("open status", "status window", "show status")
    ),
    SHOW_DAILY_QUESTS(
        label = "Show Daily Quests",
        deepLinkRoute = "quest_board",
        examplePhrases = listOf("show today's quests", "open quests", "daily quests")
    ),
    SHOW_RANK(
        label = "Check Rank & Level",
        deepLinkRoute = "status",
        examplePhrases = listOf("check rank", "what is my rank", "show my level")
    ),
    START_FOCUS_SESSION(
        label = "Open Focus Timer",
        deepLinkRoute = "academic_focus",
        examplePhrases = listOf("start focus session", "open focus timer", "start studying")
    ),
    OPEN_WORKOUT(
        label = "Open Workout Engine",
        deepLinkRoute = "physical",
        examplePhrases = listOf("start workout", "open physical", "start jogging")
    ),
    SHOW_ACADEMIC_PROGRESS(
        label = "Show Academic Progress",
        deepLinkRoute = "academic_dashboard",
        examplePhrases = listOf("show academic progress", "study progress", "open academic")
    ),
    READ_DAILY_REPORT(
        label = "Read Daily System Report",
        deepLinkRoute = "weekly_intelligence",
        examplePhrases = listOf("read daily report", "daily summary", "read system summary")
    ),
    OPEN_AI_COMPANION(
        label = "Open AI System Companion",
        deepLinkRoute = "ai_companion",
        examplePhrases = listOf("open companion", "ask system", "open ai companion")
    ),
    START_DUNGEON_HUB(
        label = "Open Dungeon Gates",
        deepLinkRoute = "dungeon_gate",
        examplePhrases = listOf("open dungeons", "show dungeon gate", "boss battle")
    ),
    REPLAY_AWAKENING(
        label = "Replay System Awakening",
        deepLinkRoute = "awakening",
        requiresConfirmation = true,
        examplePhrases = listOf("replay awakening", "restart awakening", "system awakening")
    )
}

data class VoiceCommandResolution(
    val matched: Boolean,
    val action: VoiceCommandActionType? = null,
    val spokenTranscript: String,
    val normalizedCommand: String,
    val confidenceLabel: String,
    val requiresConfirmation: Boolean = false,
    val deepLinkRoute: String? = null,
    val responseText: String
)

data class VoiceCommandHistoryRecord(
    val id: Long = 0,
    val transcript: String,
    val matchedActionName: String,
    val outcome: String,
    val executedAtEpochMs: Long = System.currentTimeMillis()
)

data class SystemExperiencePreferences(
    val hasCompletedAwakening: Boolean = false,
    val lastCelebratedLevel: Int = 1,
    val lastCelebratedRankName: String = HunterRank.E_RANK.name,
    val ttsEnabled: Boolean = true,
    val ttsSpeechRate: Float = 1.0f,
    val ttsPitch: Float = 1.0f,
    val ttsSpeakQuestUpdates: Boolean = true,
    val ttsSpeakLevelAndRank: Boolean = true,
    val ttsSpeakDungeonsAndBosses: Boolean = true,
    val ttsSpeakRecoveryWarnings: Boolean = true,
    val ttsSpeakRecommendations: Boolean = false,
    val ttsSpeakDailySummary: Boolean = true,
    val voiceCommandsEnabled: Boolean = true,
    val voiceLocaleTag: String = "en-US",
    val animationsEnabled: Boolean = true,
    val reducedMotionEnabled: Boolean = false,
    val soundEffectsEnabled: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

enum class VoiceEngineAvailability(val label: String) {
    INITIALIZING("Initializing TTS"),
    READY("Voice Engine Ready"),
    SPEAKING("Speaking"),
    UNAVAILABLE("TTS Unavailable / Missing Engine"),
    MUTED("Muted by User")
}

sealed interface SpeechInputUiState {
    data object Idle : SpeechInputUiState
    data class Listening(val rmsDb: Float = 0f) : SpeechInputUiState
    data object Processing : SpeechInputUiState
    data class Unavailable(val reason: String) : SpeechInputUiState
}

@Serializable
data class WidgetQuestSummary(
    val id: Long,
    val title: String,
    val targetLabel: String,
    val isCompleted: Boolean
)

@Serializable
data class WidgetSnapshot(
    val playerCodename: String = "Player One",
    val level: Int = 1,
    val rankName: String = HunterRank.E_RANK.name,
    val rankDisplayName: String = HunterRank.E_RANK.displayName,
    val equippedTitle: String = HunterRank.E_RANK.title,
    val currentLevelXp: Int = 0,
    val xpToNextLevel: Int = 100,
    val totalXp: Int = 0,
    val recoveryLabel: String = "Optimal Readiness",
    val pendingDailyQuests: List<WidgetQuestSummary> = emptyList(),
    val completedDailyQuests: Int = 0,
    val totalDailyQuests: Int = 0,
    val todayJoggingMinutes: Int = 0,
    val joggingTargetMinutes: Int = 30,
    val todayStudyMinutes: Int = 0,
    val academicGoalMinutes: Int = 60,
    val disciplineScore: Int = 0,
    val updatedAtEpochMs: Long = 0L
)
