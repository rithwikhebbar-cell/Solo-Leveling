package com.example.domain.engine

import com.example.domain.model.AwakeningStage
import com.example.domain.model.CelebrationEvent
import com.example.domain.model.CelebrationType
import com.example.domain.model.HunterRank
import com.example.domain.model.NotificationCategory
import com.example.domain.model.PlayerProfile
import com.example.domain.model.SystemAnnouncementCategory
import com.example.domain.model.SystemAnnouncementRecord
import com.example.domain.model.SystemEvent
import com.example.domain.model.SystemEventType
import com.example.domain.model.SystemExperiencePreferences
import com.example.domain.model.SystemNotification
import com.example.domain.model.SystemNotificationChannelSpec
import com.example.domain.model.SystemNotificationPreferences
import com.example.domain.model.TtsSpeechPriority
import com.example.domain.model.VoiceCommandActionType
import com.example.domain.model.VoiceCommandResolution
import java.time.ZoneId
import java.util.Locale

data class NotificationPresentationSpec(
    val channel: SystemNotificationChannelSpec,
    val announcementCategory: SystemAnnouncementCategory,
    val deepLinkRoute: String,
    val actionLabel: String,
    val priority: TtsSpeechPriority
)

data class PresentationDecision(
    val shouldStoreAnnouncement: Boolean,
    val shouldSpeak: Boolean,
    val shouldShowBanner: Boolean,
    val shouldPlaySound: Boolean,
    val shouldTriggerHaptic: Boolean,
    val speechPriority: TtsSpeechPriority,
    val blockReason: String? = null
)

object AwakeningScriptEngine {
    fun buildFormattedBody(stage: AwakeningStage, profile: PlayerProfile): String =
        stage.bodyTemplate
            .replace("%CODENAME%", profile.codename)
            .replace("%RANK%", profile.rank.displayName)
            .replace("%LEVEL%", profile.level.toString())

    fun buildFormattedVoice(stage: AwakeningStage, profile: PlayerProfile): String =
        stage.voiceScript
            .replace("%CODENAME%", profile.codename)
            .replace("%RANK%", profile.rank.displayName)
            .replace("%LEVEL%", profile.level.toString())
}

object SystemNotificationChannelMapper {
    fun resolve(category: NotificationCategory, existingDeepLink: String? = null): NotificationPresentationSpec {
        return when (category) {
            NotificationCategory.LEVEL_UP -> NotificationPresentationSpec(
                channel = SystemNotificationChannelSpec.LEVEL_UP,
                announcementCategory = SystemAnnouncementCategory.LEVEL_UP,
                deepLinkRoute = existingDeepLink ?: "status",
                actionLabel = "Open Dashboard",
                priority = TtsSpeechPriority.CRITICAL
            )
            NotificationCategory.RANK_PROMOTION -> NotificationPresentationSpec(
                channel = SystemNotificationChannelSpec.RANK_UP,
                announcementCategory = SystemAnnouncementCategory.RANK_UP,
                deepLinkRoute = existingDeepLink ?: "status",
                actionLabel = "Open Dashboard",
                priority = TtsSpeechPriority.CRITICAL
            )
            NotificationCategory.RECOVERY_WARNING -> NotificationPresentationSpec(
                channel = SystemNotificationChannelSpec.RECOVERY_WARNING,
                announcementCategory = SystemAnnouncementCategory.RECOVERY,
                deepLinkRoute = existingDeepLink ?: "physical",
                actionLabel = "Review Recovery",
                priority = TtsSpeechPriority.CRITICAL
            )
            NotificationCategory.DAILY_QUEST_REMINDER,
            NotificationCategory.TASK_DEADLINE,
            NotificationCategory.OVERDUE_TASK,
            NotificationCategory.HABIT_REMINDER,
            NotificationCategory.ROUTINE_REMINDER,
            NotificationCategory.WORKOUT_REMINDER,
            NotificationCategory.OVERDUE_REVISION,
            NotificationCategory.UPCOMING_EXAM -> NotificationPresentationSpec(
                channel = SystemNotificationChannelSpec.QUEST_AVAILABLE,
                announcementCategory = SystemAnnouncementCategory.QUEST_AVAILABLE,
                deepLinkRoute = existingDeepLink ?: when (category) {
                    NotificationCategory.WORKOUT_REMINDER -> "physical"
                    NotificationCategory.OVERDUE_REVISION -> "academic_revisions"
                    NotificationCategory.UPCOMING_EXAM -> "academic_exams"
                    NotificationCategory.HABIT_REMINDER -> "habits"
                    NotificationCategory.ROUTINE_REMINDER -> "routines"
                    NotificationCategory.TASK_DEADLINE, NotificationCategory.OVERDUE_TASK -> "tasks"
                    else -> "quest_board"
                },
                actionLabel = if (category == NotificationCategory.OVERDUE_REVISION) "Start Focus Session" else "View Quest",
                priority = TtsSpeechPriority.HIGH
            )
            NotificationCategory.QUEST_UPDATE,
            NotificationCategory.ACHIEVEMENT,
            NotificationCategory.SKILL_UNLOCKED,
            NotificationCategory.GOAL_MILESTONE -> NotificationPresentationSpec(
                channel = SystemNotificationChannelSpec.QUEST_COMPLETED,
                announcementCategory = SystemAnnouncementCategory.QUEST_COMPLETED,
                deepLinkRoute = existingDeepLink ?: "quest_board",
                actionLabel = "View Quest",
                priority = TtsSpeechPriority.NORMAL
            )
            NotificationCategory.DUNGEON_UPDATE -> NotificationPresentationSpec(
                channel = SystemNotificationChannelSpec.DUNGEON_AVAILABLE,
                announcementCategory = SystemAnnouncementCategory.DUNGEON,
                deepLinkRoute = existingDeepLink ?: "dungeon_gate",
                actionLabel = "View Gate",
                priority = TtsSpeechPriority.HIGH
            )
            NotificationCategory.BOSS_PHASE -> NotificationPresentationSpec(
                channel = SystemNotificationChannelSpec.BOSS_BATTLE,
                announcementCategory = SystemAnnouncementCategory.BOSS,
                deepLinkRoute = existingDeepLink ?: "boss_battles",
                actionLabel = "View Raid",
                priority = TtsSpeechPriority.HIGH
            )
            NotificationCategory.AI_RECOMMENDATION -> NotificationPresentationSpec(
                channel = SystemNotificationChannelSpec.DAILY_REPORT,
                announcementCategory = SystemAnnouncementCategory.RECOMMENDATION,
                deepLinkRoute = existingDeepLink ?: "ai_companion",
                actionLabel = "Review Recommendation",
                priority = TtsSpeechPriority.LOW
            )
            NotificationCategory.DAILY_SUMMARY,
            NotificationCategory.DAILY_PLANNING_REMINDER,
            NotificationCategory.WEEKLY_REVIEW_REMINDER -> NotificationPresentationSpec(
                channel = SystemNotificationChannelSpec.DAILY_REPORT,
                announcementCategory = SystemAnnouncementCategory.DAILY_SUMMARY,
                deepLinkRoute = existingDeepLink ?: "intelligence_dashboard",
                actionLabel = "Open Dashboard",
                priority = TtsSpeechPriority.NORMAL
            )
            NotificationCategory.SYSTEM_ALERT -> NotificationPresentationSpec(
                channel = SystemNotificationChannelSpec.SYSTEM_ALERT,
                announcementCategory = SystemAnnouncementCategory.SYSTEM_ALERT,
                deepLinkRoute = existingDeepLink ?: "system_control",
                actionLabel = "Open Dashboard",
                priority = TtsSpeechPriority.NORMAL
            )
        }
    }
}

object SystemAnnouncementEngine {

    fun fromNotification(
        notification: SystemNotification,
        wasDispatchedOs: Boolean = false
    ): SystemAnnouncementRecord {
        val spec = SystemNotificationChannelMapper.resolve(notification.category, notification.deepLinkRoute)
        val bucketKey = notification.timestampEpochMs / 60_000L
        val key = "${notification.category.name}|${notification.title.trim()}|$bucketKey"
        return SystemAnnouncementRecord(
            announcementKey = key,
            category = spec.announcementCategory,
            channelSpec = spec.channel,
            title = notification.title,
            message = notification.message,
            voiceSummary = "${notification.title.replace("//", ",")}. ${notification.message}",
            deepLinkRoute = spec.deepLinkRoute,
            actionLabel = spec.actionLabel,
            priority = spec.priority,
            wasDispatchedOs = wasDispatchedOs,
            isRead = notification.isRead,
            createdAtEpochMs = notification.timestampEpochMs
        )
    }

    fun fromSystemEvent(event: SystemEvent): SystemAnnouncementRecord? {
        return when (event.eventType) {
            SystemEventType.DUNGEON_COMPLETED -> SystemAnnouncementRecord(
                announcementKey = "EVENT|${event.eventId}",
                eventId = event.eventId,
                category = SystemAnnouncementCategory.DUNGEON,
                channelSpec = SystemNotificationChannelSpec.DUNGEON_AVAILABLE,
                title = "Dungeon Gate Cleared",
                message = "Verified completion recorded for gate ${event.relatedEntityId}.",
                voiceSummary = "Dungeon gate cleared. Verified completion recorded.",
                deepLinkRoute = "dungeon_gate",
                actionLabel = "View Gate",
                priority = TtsSpeechPriority.HIGH,
                createdAtEpochMs = event.timestampEpochMs
            )
            SystemEventType.BOSS_BATTLE_COMPLETED -> SystemAnnouncementRecord(
                announcementKey = "EVENT|${event.eventId}",
                eventId = event.eventId,
                category = SystemAnnouncementCategory.BOSS,
                channelSpec = SystemNotificationChannelSpec.BOSS_BATTLE,
                title = "Boss Campaign Cleared",
                message = "Boss raid ${event.relatedEntityId} defeated through verified effort.",
                voiceSummary = "Boss campaign victory confirmed.",
                deepLinkRoute = "boss_battles",
                actionLabel = "View Raid",
                priority = TtsSpeechPriority.HIGH,
                createdAtEpochMs = event.timestampEpochMs
            )
            SystemEventType.RECOVERY_STATE_CHANGED -> {
                val stateLabel = event.metadata["recoveryState"] ?: "Updated"
                if (stateLabel.contains("REST", ignoreCase = true) || stateLabel.contains("RECOVERY", ignoreCase = true)) {
                    SystemAnnouncementRecord(
                        announcementKey = "EVENT|${event.eventId}",
                        eventId = event.eventId,
                        category = SystemAnnouncementCategory.RECOVERY,
                        channelSpec = SystemNotificationChannelSpec.RECOVERY_WARNING,
                        title = "Recovery Safety Warning",
                        message = "System detected elevated strain ($stateLabel). Prioritize rest or light mobility.",
                        voiceSummary = "Recovery warning. Elevated fatigue detected. Prioritize rest today.",
                        deepLinkRoute = "physical",
                        actionLabel = "Open Dashboard",
                        priority = TtsSpeechPriority.CRITICAL,
                        createdAtEpochMs = event.timestampEpochMs
                    )
                } else null
            }
            SystemEventType.DAILY_RESET_COMPLETED -> SystemAnnouncementRecord(
                announcementKey = "EVENT|${event.eventId}",
                eventId = event.eventId,
                category = SystemAnnouncementCategory.DAILY_SUMMARY,
                channelSpec = SystemNotificationChannelSpec.DAILY_REPORT,
                title = "Daily System Cycle Reset",
                message = "Today's daily quests and schedule cadence have been prepared.",
                voiceSummary = "Daily cycle initialized. Today's quests are active.",
                deepLinkRoute = "quest_board",
                actionLabel = "View Quest",
                priority = TtsSpeechPriority.NORMAL,
                createdAtEpochMs = event.timestampEpochMs
            )
            else -> null
        }
    }
}

object SystemCelebrationEngine {

    /**
     * Evaluates genuine deterministic progression transitions without ever modifying XP or fabricating rank jumps.
     */
    fun evaluateTransitions(
        currentProfile: PlayerProfile,
        lastCelebratedLevel: Int,
        lastCelebratedRank: HunterRank,
        existingCelebrationIds: Set<String>,
        attributeDeltaSummary: String = "",
        nowEpochMs: Long = System.currentTimeMillis()
    ): List<CelebrationEvent> {
        val celebrations = mutableListOf<CelebrationEvent>()

        if (currentProfile.level > lastCelebratedLevel && currentProfile.level > 1) {
            val id = "LEVEL_${currentProfile.level}"
            if (id !in existingCelebrationIds) {
                celebrations.add(
                    CelebrationEvent(
                        celebrationId = id,
                        type = CelebrationType.LEVEL_UP,
                        previousLevel = lastCelebratedLevel,
                        newLevel = currentProfile.level,
                        previousRankName = lastCelebratedRank.name,
                        newRankName = currentProfile.rank.name,
                        titleUnlocked = if (currentProfile.rank != lastCelebratedRank) currentProfile.rank.title else "",
                        attributeDeltaSummary = attributeDeltaSummary.ifBlank {
                            "Total Attributes: ${currentProfile.attributes.totalScore} • Total XP: ${currentProfile.totalXp}"
                        },
                        headline = "LEVEL UP // LV. ${currentProfile.level}",
                        message = "Deterministic progression reached Level ${currentProfile.level}. Next threshold: ${currentProfile.xpToNextLevel} XP.",
                        createdAtEpochMs = nowEpochMs
                    )
                )
            }
        }

        if (currentProfile.rank.ordinal > lastCelebratedRank.ordinal) {
            val id = "RANK_${currentProfile.rank.name}"
            if (id !in existingCelebrationIds) {
                celebrations.add(
                    CelebrationEvent(
                        celebrationId = id,
                        type = CelebrationType.RANK_PROMOTION,
                        previousLevel = lastCelebratedLevel,
                        newLevel = currentProfile.level,
                        previousRankName = lastCelebratedRank.name,
                        newRankName = currentProfile.rank.name,
                        titleUnlocked = currentProfile.rank.title,
                        attributeDeltaSummary = attributeDeltaSummary.ifBlank {
                            "${currentProfile.verifiedActivitiesCount} verified sessions • ${currentProfile.attributes.totalScore} attr score"
                        },
                        headline = "RANK PROMOTION // ${currentProfile.rank.displayName.uppercase()}",
                        message = "Promotion verified: ${lastCelebratedRank.displayName} → ${currentProfile.rank.displayName}. Title granted: ${currentProfile.rank.title}.",
                        createdAtEpochMs = nowEpochMs + 1
                    )
                )
            }
        }

        return celebrations
    }
}

object VoiceCommandProcessor {

    /**
     * Strict allowlist intent matcher. Never executes arbitrary text, system calls, or XP mutations.
     */
    fun process(rawTranscript: String): VoiceCommandResolution {
        val clean = rawTranscript.trim()
        val normalized = clean.lowercase(Locale.US)
            .replace(Regex("[^a-z0-9'\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        if (normalized.isBlank()) {
            return VoiceCommandResolution(
                matched = false,
                spokenTranscript = rawTranscript,
                normalizedCommand = normalized,
                confidenceLabel = "Empty",
                responseText = "No speech detected. Say a command like 'open status' or 'show quests'."
            )
        }

        val unsafeMutationRegex = Regex("\\b(grant|cheat|hack|infinite|unlock|modify|bypass|override)\\b")
        val unsafePhrase = listOf("give me", "add xp", "bonus xp", "set level", "set rank")
            .any { it in normalized }
        if (unsafeMutationRegex.containsMatchIn(normalized) || unsafePhrase) {
            return VoiceCommandResolution(
                matched = false,
                action = null,
                spokenTranscript = rawTranscript,
                normalizedCommand = normalized,
                confidenceLabel = "Rejected",
                responseText = "Rejected: voice commands only trigger allowlisted navigation and never modify XP, level, or rank."
            )
        }

        val action: VoiceCommandActionType? = when {
            "awakening" in normalized || "replay intro" in normalized ->
                VoiceCommandActionType.REPLAY_AWAKENING
            "companion" in normalized || "ask ai" in normalized || "ask system" in normalized || "advisor" in normalized ->
                VoiceCommandActionType.OPEN_AI_COMPANION
            "focus" in normalized || "timer" in normalized || "pomodoro" in normalized ->
                VoiceCommandActionType.START_FOCUS_SESSION
            "workout" in normalized || "jog" in normalized || "exercise" in normalized || "physical" in normalized ->
                VoiceCommandActionType.OPEN_WORKOUT
            "academic" in normalized || "study" in normalized || "subject" in normalized || "revision" in normalized ->
                VoiceCommandActionType.SHOW_ACADEMIC_PROGRESS
            "daily report" in normalized || "daily summary" in normalized || "system report" in normalized || "weekly report" in normalized ->
                VoiceCommandActionType.READ_DAILY_REPORT
            "quest" in normalized ->
                VoiceCommandActionType.SHOW_DAILY_QUESTS
            "dungeon" in normalized || "gate" in normalized || "boss" in normalized ->
                VoiceCommandActionType.START_DUNGEON_HUB
            "rank" in normalized || "level" in normalized ->
                VoiceCommandActionType.SHOW_RANK
            "status" in normalized || "profile" in normalized || "stats" in normalized ->
                VoiceCommandActionType.OPEN_STATUS
            else -> null
        }

        if (action == null) {
            return VoiceCommandResolution(
                matched = false,
                action = null,
                spokenTranscript = rawTranscript,
                normalizedCommand = normalized,
                confidenceLabel = "Unrecognized",
                responseText = "Command '$clean' did not match an allowlisted System action. Try 'open status', 'show quests', 'start focus session', or 'open companion'."
            )
        }

        val response = when (action) {
            VoiceCommandActionType.OPEN_STATUS -> "Opening Player Status Window."
            VoiceCommandActionType.SHOW_DAILY_QUESTS -> "Opening Quest Board for today's objectives."
            VoiceCommandActionType.SHOW_RANK -> "Displaying Rank and Level progression status."
            VoiceCommandActionType.START_FOCUS_SESSION -> "Opening Academic Focus Timer."
            VoiceCommandActionType.OPEN_WORKOUT -> "Opening Physical Training Engine."
            VoiceCommandActionType.SHOW_ACADEMIC_PROGRESS -> "Opening Academic Engine overview."
            VoiceCommandActionType.READ_DAILY_REPORT -> "Opening System Intelligence Report."
            VoiceCommandActionType.OPEN_AI_COMPANION -> "Opening AI System Companion."
            VoiceCommandActionType.START_DUNGEON_HUB -> "Opening Dungeon Gate Hub."
            VoiceCommandActionType.REPLAY_AWAKENING -> "Confirmation required before replaying System Awakening."
        }

        return VoiceCommandResolution(
            matched = true,
            action = action,
            spokenTranscript = rawTranscript,
            normalizedCommand = normalized,
            confidenceLabel = "Allowlist Match",
            requiresConfirmation = action.requiresConfirmation,
            deepLinkRoute = action.deepLinkRoute,
            responseText = response
        )
    }
}

object SystemPresentationCoordinator {

    fun evaluatePresentation(
        record: SystemAnnouncementRecord,
        experiencePrefs: SystemExperiencePreferences,
        notificationPrefs: SystemNotificationPreferences,
        recentKeys: Set<String>,
        nowEpochMs: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): PresentationDecision {
        if (record.announcementKey in recentKeys) {
            return PresentationDecision(
                shouldStoreAnnouncement = false,
                shouldSpeak = false,
                shouldShowBanner = false,
                shouldPlaySound = false,
                shouldTriggerHaptic = false,
                speechPriority = record.priority,
                blockReason = "Deduplicated announcement key"
            )
        }

        val inQuietHours = QuietHoursManager.isQuietNow(notificationPrefs, nowEpochMs, zoneId)
        val isCriticalSafety = record.category == SystemAnnouncementCategory.RECOVERY

        val speakCategoryAllowed = when (record.category) {
            SystemAnnouncementCategory.QUEST_AVAILABLE,
            SystemAnnouncementCategory.QUEST_COMPLETED -> experiencePrefs.ttsSpeakQuestUpdates
            SystemAnnouncementCategory.LEVEL_UP,
            SystemAnnouncementCategory.RANK_UP -> experiencePrefs.ttsSpeakLevelAndRank
            SystemAnnouncementCategory.DUNGEON,
            SystemAnnouncementCategory.BOSS -> experiencePrefs.ttsSpeakDungeonsAndBosses
            SystemAnnouncementCategory.RECOVERY -> experiencePrefs.ttsSpeakRecoveryWarnings
            SystemAnnouncementCategory.RECOMMENDATION -> experiencePrefs.ttsSpeakRecommendations
            SystemAnnouncementCategory.DAILY_SUMMARY -> experiencePrefs.ttsSpeakDailySummary
            SystemAnnouncementCategory.SYSTEM_ALERT -> true
        }

        val shouldSpeak = experiencePrefs.ttsEnabled &&
            speakCategoryAllowed &&
            (!inQuietHours || isCriticalSafety)

        val shouldPlaySound = experiencePrefs.soundEffectsEnabled &&
            !experiencePrefs.reducedMotionEnabled &&
            (!inQuietHours || isCriticalSafety)

        val shouldTriggerHaptic = experiencePrefs.hapticFeedbackEnabled &&
            !experiencePrefs.reducedMotionEnabled

        val shouldShowBanner = record.priority.weight >= TtsSpeechPriority.HIGH.weight &&
            !experiencePrefs.reducedMotionEnabled

        return PresentationDecision(
            shouldStoreAnnouncement = true,
            shouldSpeak = shouldSpeak,
            shouldShowBanner = shouldShowBanner,
            shouldPlaySound = shouldPlaySound,
            shouldTriggerHaptic = shouldTriggerHaptic,
            speechPriority = record.priority,
            blockReason = if (inQuietHours && !isCriticalSafety) "Quiet hours active (audio muted)" else null
        )
    }
}
