package com.example

import com.example.domain.engine.AwakeningScriptEngine
import com.example.domain.engine.SystemAnnouncementEngine
import com.example.domain.engine.SystemCelebrationEngine
import com.example.domain.engine.SystemNotificationChannelMapper
import com.example.domain.engine.SystemPresentationCoordinator
import com.example.domain.engine.VoiceCommandProcessor
import com.example.domain.model.AwakeningStage
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class SystemExperienceEnginesUnitTest {

    @Test
    fun `AwakeningScriptEngine substitutes player identity placeholders deterministically`() {
        val player = PlayerProfile(
            codename = "SungJinwoo",
            level = 7,
            rank = HunterRank.D_RANK
        )
        val identBody = AwakeningScriptEngine.buildFormattedBody(AwakeningStage.PLAYER_IDENTIFICATION, player)
        val rankBody = AwakeningScriptEngine.buildFormattedBody(AwakeningStage.RANK_CALIBRATION, player)
        val identVoice = AwakeningScriptEngine.buildFormattedVoice(AwakeningStage.PLAYER_IDENTIFICATION, player)

        assertTrue(identBody.contains("SungJinwoo"))
        assertTrue(rankBody.contains("D-Rank"))
        assertTrue(rankBody.contains("Level 7"))
        assertTrue(identVoice.contains("SungJinwoo"))
        assertEquals(5, AwakeningStage.entries.size)
    }

    @Test
    fun `SystemNotificationChannelMapper covers all 9 Android notification channels with deep links and actions`() {
        assertEquals(9, SystemNotificationChannelSpec.entries.size)

        val mappedSpecs = NotificationCategory.entries.map {
            SystemNotificationChannelMapper.resolve(it)
        }.map { it.channel }.toSet()
        assertEquals(SystemNotificationChannelSpec.entries.toSet(), mappedSpecs)

        val levelUpSpec = SystemNotificationChannelMapper.resolve(NotificationCategory.LEVEL_UP)
        assertEquals(SystemNotificationChannelSpec.LEVEL_UP, levelUpSpec.channel)
        assertEquals("status", levelUpSpec.deepLinkRoute)
        assertEquals(TtsSpeechPriority.CRITICAL, levelUpSpec.priority)

        val recoverySpec = SystemNotificationChannelMapper.resolve(NotificationCategory.RECOVERY_WARNING)
        assertEquals(SystemNotificationChannelSpec.RECOVERY_WARNING, recoverySpec.channel)
        assertEquals("physical", recoverySpec.deepLinkRoute)
        assertEquals("Review Recovery", recoverySpec.actionLabel)

        val questSpec = SystemNotificationChannelMapper.resolve(NotificationCategory.OVERDUE_REVISION)
        assertEquals(SystemNotificationChannelSpec.QUEST_AVAILABLE, questSpec.channel)
        assertEquals("academic_revisions", questSpec.deepLinkRoute)
        assertEquals("Start Focus Session", questSpec.actionLabel)
    }

    @Test
    fun `SystemAnnouncementEngine translates notifications and system events safely`() {
        val notif = SystemNotification(
            title = "QUEST CLEARED // Algorithms",
            message = "Study objective completed.",
            category = NotificationCategory.QUEST_UPDATE,
            timestampEpochMs = 1_750_000_000_000L
        )
        val ann = SystemAnnouncementEngine.fromNotification(notif, wasDispatchedOs = true)
        assertEquals(SystemAnnouncementCategory.QUEST_COMPLETED, ann.category)
        assertEquals(SystemNotificationChannelSpec.QUEST_COMPLETED, ann.channelSpec)
        assertTrue(ann.wasDispatchedOs)
        assertTrue(ann.voiceSummary.contains("QUEST CLEARED"))

        val recoveryEvent = SystemEvent(
            eventType = SystemEventType.RECOVERY_STATE_CHANGED,
            metadata = mapOf("recoveryState" to "REST_RECOMMENDED"),
            timestampEpochMs = 1_750_000_001_000L
        )
        val recAnn = SystemAnnouncementEngine.fromSystemEvent(recoveryEvent)
        assertEquals(SystemAnnouncementCategory.RECOVERY, recAnn?.category)
        assertEquals(TtsSpeechPriority.CRITICAL, recAnn?.priority)
    }

    @Test
    fun `SystemPresentationCoordinator enforces quiet hours, deduplication, and reduced motion rules`() {
        val zone = ZoneId.of("UTC")
        val quietTimestamp = ZonedDateTime.of(2025, 3, 12, 23, 30, 0, 0, zone).toInstant().toEpochMilli()
        val activeTimestamp = ZonedDateTime.of(2025, 3, 12, 14, 0, 0, 0, zone).toInstant().toEpochMilli()

        val notifPrefs = SystemNotificationPreferences(quietHoursStartHour = 22, quietHoursEndHour = 7)
        val expPrefs = SystemExperiencePreferences(
            ttsEnabled = true,
            ttsSpeakQuestUpdates = true,
            soundEffectsEnabled = true,
            hapticFeedbackEnabled = true,
            reducedMotionEnabled = false
        )

        val questRecord = SystemAnnouncementRecord(
            announcementKey = "QUEST|daily_1|100",
            category = SystemAnnouncementCategory.QUEST_AVAILABLE,
            channelSpec = SystemNotificationChannelSpec.QUEST_AVAILABLE,
            title = "Daily Quest Ready",
            message = "Complete 30m jogging",
            voiceSummary = "Daily Quest Ready",
            priority = TtsSpeechPriority.HIGH
        )
        // Quiet hours mute standard TTS & chimes
        val quietDecision = SystemPresentationCoordinator.evaluatePresentation(
            record = questRecord,
            experiencePrefs = expPrefs,
            notificationPrefs = notifPrefs,
            recentKeys = emptySet(),
            nowEpochMs = quietTimestamp,
            zoneId = zone
        )
        assertTrue(quietDecision.shouldStoreAnnouncement)
        assertFalse(quietDecision.shouldSpeak)
        assertFalse(quietDecision.shouldPlaySound)

        // Critical safety recovery warnings override quiet hours for safety
        val recoveryRecord = questRecord.copy(
            announcementKey = "REC|101",
            category = SystemAnnouncementCategory.RECOVERY,
            channelSpec = SystemNotificationChannelSpec.RECOVERY_WARNING,
            priority = TtsSpeechPriority.CRITICAL
        )
        val safetyDecision = SystemPresentationCoordinator.evaluatePresentation(
            record = recoveryRecord,
            experiencePrefs = expPrefs,
            notificationPrefs = notifPrefs,
            recentKeys = emptySet(),
            nowEpochMs = quietTimestamp,
            zoneId = zone
        )
        assertTrue(safetyDecision.shouldSpeak)
        assertTrue(safetyDecision.shouldPlaySound)

        // Duplicate keys blocked completely
        val dupDecision = SystemPresentationCoordinator.evaluatePresentation(
            record = questRecord,
            experiencePrefs = expPrefs,
            notificationPrefs = notifPrefs,
            recentKeys = setOf("QUEST|daily_1|100"),
            nowEpochMs = activeTimestamp,
            zoneId = zone
        )
        assertFalse(dupDecision.shouldStoreAnnouncement)
        assertFalse(dupDecision.shouldSpeak)

        // Reduced motion disables banner, sound chime, and haptic feedback
        val reducedDecision = SystemPresentationCoordinator.evaluatePresentation(
            record = questRecord,
            experiencePrefs = expPrefs.copy(reducedMotionEnabled = true),
            notificationPrefs = notifPrefs,
            recentKeys = emptySet(),
            nowEpochMs = activeTimestamp,
            zoneId = zone
        )
        assertFalse(reducedDecision.shouldShowBanner)
        assertFalse(reducedDecision.shouldPlaySound)
        assertFalse(reducedDecision.shouldTriggerHaptic)
    }

    @Test
    fun `SystemCelebrationEngine detects genuine upgrades idempotently without baseline false positives`() {
        val baseline = PlayerProfile(level = 1, rank = HunterRank.E_RANK)
        val noEvents = SystemCelebrationEngine.evaluateTransitions(
            currentProfile = baseline,
            lastCelebratedLevel = 1,
            lastCelebratedRank = HunterRank.E_RANK,
            existingCelebrationIds = emptySet()
        )
        assertTrue(noEvents.isEmpty())

        val promoted = PlayerProfile(
            level = 5,
            rank = HunterRank.D_RANK,
            totalXp = 920L,
            verifiedActivitiesCount = 5
        )
        val events = SystemCelebrationEngine.evaluateTransitions(
            currentProfile = promoted,
            lastCelebratedLevel = 1,
            lastCelebratedRank = HunterRank.E_RANK,
            existingCelebrationIds = emptySet(),
            nowEpochMs = 1_750_000_000_000L
        )
        assertEquals(2, events.size)
        assertEquals(CelebrationType.LEVEL_UP, events[0].type)
        assertEquals("LEVEL_5", events[0].celebrationId)
        assertEquals(CelebrationType.RANK_PROMOTION, events[1].type)
        assertEquals("RANK_D_RANK", events[1].celebrationId)
        assertEquals(HunterRank.D_RANK.title, events[1].titleUnlocked)

        // Idempotent re-evaluation suppresses duplicates
        val repeated = SystemCelebrationEngine.evaluateTransitions(
            currentProfile = promoted,
            lastCelebratedLevel = 1,
            lastCelebratedRank = HunterRank.E_RANK,
            existingCelebrationIds = setOf("LEVEL_5", "RANK_D_RANK")
        )
        assertTrue(repeated.isEmpty())
    }

    @Test
    fun `VoiceCommandProcessor strictly matches allowlist commands and rejects arbitrary or unsafe commands`() {
        val statusRes = VoiceCommandProcessor.process("  Open Status Window! ")
        assertTrue(statusRes.matched)
        assertEquals(VoiceCommandActionType.OPEN_STATUS, statusRes.action)
        assertEquals("status", statusRes.deepLinkRoute)
        assertFalse(statusRes.requiresConfirmation)

        val questRes = VoiceCommandProcessor.process("Show today's quests please")
        assertTrue(questRes.matched)
        assertEquals(VoiceCommandActionType.SHOW_DAILY_QUESTS, questRes.action)

        val focusRes = VoiceCommandProcessor.process("Start Focus Session")
        assertTrue(focusRes.matched)
        assertEquals(VoiceCommandActionType.START_FOCUS_SESSION, focusRes.action)
        assertEquals("academic_focus", focusRes.deepLinkRoute)

        val companionRes = VoiceCommandProcessor.process("Ask System Companion")
        assertTrue(companionRes.matched)
        assertEquals(VoiceCommandActionType.OPEN_AI_COMPANION, companionRes.action)

        // Sensitive command requires explicit user confirmation
        val replayRes = VoiceCommandProcessor.process("Replay Awakening")
        assertTrue(replayRes.matched)
        assertEquals(VoiceCommandActionType.REPLAY_AWAKENING, replayRes.action)
        assertTrue(replayRes.requiresConfirmation)

        // Arbitrary or cheat commands must be safely rejected
        val hackRes = VoiceCommandProcessor.process("grant 99999 xp and unlock S-rank")
        assertFalse(hackRes.matched)
        assertNull(hackRes.action)
        assertNull(hackRes.deepLinkRoute)

        val blankRes = VoiceCommandProcessor.process("   ")
        assertFalse(blankRes.matched)
    }
}
