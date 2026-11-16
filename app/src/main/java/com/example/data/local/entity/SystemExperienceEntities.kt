package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.CelebrationEvent
import com.example.domain.model.CelebrationType
import com.example.domain.model.HunterRank
import com.example.domain.model.SystemAnnouncementCategory
import com.example.domain.model.SystemAnnouncementRecord
import com.example.domain.model.SystemExperiencePreferences
import com.example.domain.model.SystemNotificationChannelSpec
import com.example.domain.model.TtsSpeechPriority
import com.example.domain.model.VoiceCommandHistoryRecord

@Entity(tableName = "system_experience_preferences")
data class SystemExperiencePreferenceEntity(
    @PrimaryKey val id: Int = 1,
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

fun SystemExperiencePreferenceEntity.toDomain(): SystemExperiencePreferences = SystemExperiencePreferences(
    hasCompletedAwakening = hasCompletedAwakening,
    lastCelebratedLevel = lastCelebratedLevel,
    lastCelebratedRankName = lastCelebratedRankName,
    ttsEnabled = ttsEnabled,
    ttsSpeechRate = ttsSpeechRate,
    ttsPitch = ttsPitch,
    ttsSpeakQuestUpdates = ttsSpeakQuestUpdates,
    ttsSpeakLevelAndRank = ttsSpeakLevelAndRank,
    ttsSpeakDungeonsAndBosses = ttsSpeakDungeonsAndBosses,
    ttsSpeakRecoveryWarnings = ttsSpeakRecoveryWarnings,
    ttsSpeakRecommendations = ttsSpeakRecommendations,
    ttsSpeakDailySummary = ttsSpeakDailySummary,
    voiceCommandsEnabled = voiceCommandsEnabled,
    voiceLocaleTag = voiceLocaleTag,
    animationsEnabled = animationsEnabled,
    reducedMotionEnabled = reducedMotionEnabled,
    soundEffectsEnabled = soundEffectsEnabled,
    hapticFeedbackEnabled = hapticFeedbackEnabled,
    updatedAtEpochMs = updatedAtEpochMs
)

fun SystemExperiencePreferences.toEntity(): SystemExperiencePreferenceEntity = SystemExperiencePreferenceEntity(
    id = 1,
    hasCompletedAwakening = hasCompletedAwakening,
    lastCelebratedLevel = lastCelebratedLevel,
    lastCelebratedRankName = lastCelebratedRankName,
    ttsEnabled = ttsEnabled,
    ttsSpeechRate = ttsSpeechRate.coerceIn(0.6f, 1.8f),
    ttsPitch = ttsPitch.coerceIn(0.6f, 1.6f),
    ttsSpeakQuestUpdates = ttsSpeakQuestUpdates,
    ttsSpeakLevelAndRank = ttsSpeakLevelAndRank,
    ttsSpeakDungeonsAndBosses = ttsSpeakDungeonsAndBosses,
    ttsSpeakRecoveryWarnings = ttsSpeakRecoveryWarnings,
    ttsSpeakRecommendations = ttsSpeakRecommendations,
    ttsSpeakDailySummary = ttsSpeakDailySummary,
    voiceCommandsEnabled = voiceCommandsEnabled,
    voiceLocaleTag = voiceLocaleTag,
    animationsEnabled = animationsEnabled,
    reducedMotionEnabled = reducedMotionEnabled,
    soundEffectsEnabled = soundEffectsEnabled,
    hapticFeedbackEnabled = hapticFeedbackEnabled,
    updatedAtEpochMs = updatedAtEpochMs
)

@Entity(
    tableName = "system_announcement_history",
    indices = [
        Index(value = ["announcementKey"], unique = true),
        Index(value = ["createdAtEpochMs"])
    ]
)
data class SystemAnnouncementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val announcementKey: String,
    val eventId: String,
    val categoryName: String,
    val channelId: String,
    val title: String,
    val message: String,
    val voiceSummary: String,
    val deepLinkRoute: String,
    val actionLabel: String,
    val priorityName: String,
    val wasSpoken: Boolean,
    val wasDispatchedOs: Boolean,
    val isRead: Boolean,
    val createdAtEpochMs: Long
)

fun SystemAnnouncementEntity.toDomain(): SystemAnnouncementRecord = SystemAnnouncementRecord(
    id = id,
    announcementKey = announcementKey,
    eventId = eventId,
    category = SystemAnnouncementCategory.entries.firstOrNull { it.name == categoryName }
        ?: SystemAnnouncementCategory.SYSTEM_ALERT,
    channelSpec = SystemNotificationChannelSpec.entries.firstOrNull { it.channelId == channelId }
        ?: SystemNotificationChannelSpec.SYSTEM_ALERT,
    title = title,
    message = message,
    voiceSummary = voiceSummary,
    deepLinkRoute = deepLinkRoute,
    actionLabel = actionLabel,
    priority = TtsSpeechPriority.entries.firstOrNull { it.name == priorityName } ?: TtsSpeechPriority.NORMAL,
    wasSpoken = wasSpoken,
    wasDispatchedOs = wasDispatchedOs,
    isRead = isRead,
    createdAtEpochMs = createdAtEpochMs
)

fun SystemAnnouncementRecord.toEntity(): SystemAnnouncementEntity = SystemAnnouncementEntity(
    id = id,
    announcementKey = announcementKey,
    eventId = eventId,
    categoryName = category.name,
    channelId = channelSpec.channelId,
    title = title,
    message = message,
    voiceSummary = voiceSummary,
    deepLinkRoute = deepLinkRoute,
    actionLabel = actionLabel,
    priorityName = priority.name,
    wasSpoken = wasSpoken,
    wasDispatchedOs = wasDispatchedOs,
    isRead = isRead,
    createdAtEpochMs = createdAtEpochMs
)

@Entity(
    tableName = "system_celebration_history",
    indices = [Index(value = ["createdAtEpochMs"])]
)
data class SystemCelebrationEntity(
    @PrimaryKey val celebrationId: String,
    val typeName: String,
    val previousLevel: Int,
    val newLevel: Int,
    val previousRankName: String,
    val newRankName: String,
    val titleUnlocked: String,
    val attributeDeltaSummary: String,
    val headline: String,
    val message: String,
    val isAcknowledged: Boolean,
    val createdAtEpochMs: Long
)

fun SystemCelebrationEntity.toDomain(): CelebrationEvent = CelebrationEvent(
    celebrationId = celebrationId,
    type = CelebrationType.entries.firstOrNull { it.name == typeName } ?: CelebrationType.LEVEL_UP,
    previousLevel = previousLevel,
    newLevel = newLevel,
    previousRankName = previousRankName,
    newRankName = newRankName,
    titleUnlocked = titleUnlocked,
    attributeDeltaSummary = attributeDeltaSummary,
    headline = headline,
    message = message,
    isAcknowledged = isAcknowledged,
    createdAtEpochMs = createdAtEpochMs
)

fun CelebrationEvent.toEntity(): SystemCelebrationEntity = SystemCelebrationEntity(
    celebrationId = celebrationId,
    typeName = type.name,
    previousLevel = previousLevel,
    newLevel = newLevel,
    previousRankName = previousRankName,
    newRankName = newRankName,
    titleUnlocked = titleUnlocked,
    attributeDeltaSummary = attributeDeltaSummary,
    headline = headline,
    message = message,
    isAcknowledged = isAcknowledged,
    createdAtEpochMs = createdAtEpochMs
)

@Entity(
    tableName = "voice_command_history",
    indices = [Index(value = ["executedAtEpochMs"])]
)
data class VoiceCommandHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transcript: String,
    val matchedActionName: String,
    val outcome: String,
    val executedAtEpochMs: Long
)

fun VoiceCommandHistoryEntity.toDomain(): VoiceCommandHistoryRecord = VoiceCommandHistoryRecord(
    id = id,
    transcript = transcript,
    matchedActionName = matchedActionName,
    outcome = outcome,
    executedAtEpochMs = executedAtEpochMs
)

fun VoiceCommandHistoryRecord.toEntity(): VoiceCommandHistoryEntity = VoiceCommandHistoryEntity(
    id = id,
    transcript = transcript,
    matchedActionName = matchedActionName,
    outcome = outcome,
    executedAtEpochMs = executedAtEpochMs
)
