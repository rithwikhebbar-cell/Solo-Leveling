package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.engine.ProgressionEngine
import com.example.domain.model.ActivityLog
import com.example.domain.model.AttributeBreakdown
import com.example.domain.model.AttributeType
import com.example.domain.model.HunterRank
import com.example.domain.model.NotificationCategory
import com.example.domain.model.PhysicalDevelopmentState
import com.example.domain.model.PlayerAttributes
import com.example.domain.model.PlayerProfile
import com.example.domain.model.Quest
import com.example.domain.model.QuestCategory
import com.example.domain.model.QuestType
import com.example.domain.model.RecoveryStatus
import com.example.domain.model.SystemNotification
import com.example.domain.model.VerificationStatus
import com.example.domain.model.WorkoutStatus

@Entity(tableName = "player_profile")
data class PlayerEntity(
    @PrimaryKey val id: Int = 1,
    val codename: String,
    val title: String,
    val level: Int,
    val currentLevelXp: Int,
    val totalXp: Long,
    val rankName: String,
    val strVerified: Int,
    val strSelfReported: Int,
    val strProgressXp: Int,
    val vitVerified: Int,
    val vitSelfReported: Int,
    val vitProgressXp: Int,
    val agiVerified: Int,
    val agiSelfReported: Int,
    val agiProgressXp: Int,
    val intVerified: Int,
    val intSelfReported: Int,
    val intProgressXp: Int,
    val disVerified: Int,
    val disSelfReported: Int,
    val disProgressXp: Int,
    val wisVerified: Int,
    val wisSelfReported: Int,
    val wisProgressXp: Int,
    val chaVerified: Int,
    val chaSelfReported: Int,
    val chaProgressXp: Int,
    val streakDays: Int,
    val verifiedActivitiesCount: Int,
    val selfReportedActivitiesCount: Int,
    val recoveryScore: Int,
    val createdAtEpochMs: Long
)

@Entity(tableName = "quests")
data class QuestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val typeName: String,
    val categoryName: String,
    val targetValue: Int,
    val currentValue: Int,
    val unit: String,
    val xpReward: Int,
    val primaryAttrName: String,
    val primaryAttrXpReward: Int,
    val secondaryAttrName: String?,
    val secondaryAttrXpReward: Int,
    val isCompleted: Boolean,
    val verificationStatusName: String,
    val createdAtEpochMs: Long,
    val completedAtEpochMs: Long?
)

@Entity(
    tableName = "activity_logs",
    indices = [Index("timestampEpochMs")]
)
data class ActivityLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val categoryName: String,
    val durationMinutes: Int,
    val verificationStatusName: String,
    val xpAwarded: Int,
    val primaryAttrName: String,
    val primaryAttrXpAwarded: Int,
    val notes: String,
    val timestampEpochMs: Long
)

@Entity(tableName = "physical_state")
data class PhysicalStateEntity(
    @PrimaryKey val id: Int = 1,
    val baseJoggingTargetMinutes: Int,
    val aiRecommendedJoggingMinutes: Int,
    val completedJoggingMinutesToday: Int,
    val workoutStatusName: String,
    val recoveryStatusName: String,
    val weeklySessionsCompleted: Int,
    val lastSessionTimestampMs: Long?,
    val aiTrainingNote: String
)

@Entity(tableName = "system_notifications")
data class SystemNotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val message: String,
    val categoryName: String,
    val isRead: Boolean,
    val timestampEpochMs: Long,
    val deepLinkRoute: String? = null
)

fun PlayerEntity.toDomain(): PlayerProfile {
    val rank = runCatching { HunterRank.valueOf(rankName) }.getOrDefault(HunterRank.E_RANK)
    val attrs = PlayerAttributes(
        str = AttributeBreakdown(AttributeType.STR, verifiedPoints = strVerified, selfReportedPoints = strSelfReported, progressXp = strProgressXp),
        vit = AttributeBreakdown(AttributeType.VIT, verifiedPoints = vitVerified, selfReportedPoints = vitSelfReported, progressXp = vitProgressXp),
        agi = AttributeBreakdown(AttributeType.AGI, verifiedPoints = agiVerified, selfReportedPoints = agiSelfReported, progressXp = agiProgressXp),
        intel = AttributeBreakdown(AttributeType.INT, verifiedPoints = intVerified, selfReportedPoints = intSelfReported, progressXp = intProgressXp),
        dis = AttributeBreakdown(AttributeType.DIS, verifiedPoints = disVerified, selfReportedPoints = disSelfReported, progressXp = disProgressXp),
        wis = AttributeBreakdown(AttributeType.WIS, verifiedPoints = wisVerified, selfReportedPoints = wisSelfReported, progressXp = wisProgressXp),
        cha = AttributeBreakdown(AttributeType.CHA, verifiedPoints = chaVerified, selfReportedPoints = chaSelfReported, progressXp = chaProgressXp)
    )
    return PlayerProfile(
        codename = codename,
        title = title,
        level = level,
        currentLevelXp = currentLevelXp,
        xpToNextLevel = ProgressionEngine.xpRequiredForLevel(level),
        totalXp = totalXp,
        rank = rank,
        attributes = attrs,
        streakDays = streakDays,
        verifiedActivitiesCount = verifiedActivitiesCount,
        selfReportedActivitiesCount = selfReportedActivitiesCount,
        recoveryScore = recoveryScore,
        createdAtEpochMs = createdAtEpochMs
    )
}

fun PlayerProfile.toEntity(): PlayerEntity = PlayerEntity(
    id = 1,
    codename = codename,
    title = title,
    level = level,
    currentLevelXp = currentLevelXp,
    totalXp = totalXp,
    rankName = rank.name,
    strVerified = attributes.str.verifiedPoints,
    strSelfReported = attributes.str.selfReportedPoints,
    strProgressXp = attributes.str.progressXp,
    vitVerified = attributes.vit.verifiedPoints,
    vitSelfReported = attributes.vit.selfReportedPoints,
    vitProgressXp = attributes.vit.progressXp,
    agiVerified = attributes.agi.verifiedPoints,
    agiSelfReported = attributes.agi.selfReportedPoints,
    agiProgressXp = attributes.agi.progressXp,
    intVerified = attributes.intel.verifiedPoints,
    intSelfReported = attributes.intel.selfReportedPoints,
    intProgressXp = attributes.intel.progressXp,
    disVerified = attributes.dis.verifiedPoints,
    disSelfReported = attributes.dis.selfReportedPoints,
    disProgressXp = attributes.dis.progressXp,
    wisVerified = attributes.wis.verifiedPoints,
    wisSelfReported = attributes.wis.selfReportedPoints,
    wisProgressXp = attributes.wis.progressXp,
    chaVerified = attributes.cha.verifiedPoints,
    chaSelfReported = attributes.cha.selfReportedPoints,
    chaProgressXp = attributes.cha.progressXp,
    streakDays = streakDays,
    verifiedActivitiesCount = verifiedActivitiesCount,
    selfReportedActivitiesCount = selfReportedActivitiesCount,
    recoveryScore = recoveryScore,
    createdAtEpochMs = createdAtEpochMs
)

fun QuestEntity.toDomain(): Quest = Quest(
    id = id,
    title = title,
    description = description,
    type = runCatching { QuestType.valueOf(typeName) }.getOrDefault(QuestType.DAILY),
    category = runCatching { QuestCategory.valueOf(categoryName) }.getOrDefault(QuestCategory.DISCIPLINE),
    targetValue = targetValue,
    currentValue = currentValue,
    unit = unit,
    xpReward = xpReward,
    primaryAttribute = runCatching { AttributeType.valueOf(primaryAttrName) }.getOrDefault(AttributeType.DIS),
    primaryAttrXpReward = primaryAttrXpReward,
    secondaryAttribute = secondaryAttrName?.let { name -> runCatching { AttributeType.valueOf(name) }.getOrNull() },
    secondaryAttrXpReward = secondaryAttrXpReward,
    isCompleted = isCompleted,
    verificationStatus = runCatching { VerificationStatus.valueOf(verificationStatusName) }.getOrDefault(VerificationStatus.SELF_REPORTED),
    createdAtEpochMs = createdAtEpochMs,
    completedAtEpochMs = completedAtEpochMs
)

fun Quest.toEntity(): QuestEntity = QuestEntity(
    id = id,
    title = title,
    description = description,
    typeName = type.name,
    categoryName = category.name,
    targetValue = targetValue,
    currentValue = currentValue,
    unit = unit,
    xpReward = xpReward,
    primaryAttrName = primaryAttribute.name,
    primaryAttrXpReward = primaryAttrXpReward,
    secondaryAttrName = secondaryAttribute?.name,
    secondaryAttrXpReward = secondaryAttrXpReward,
    isCompleted = isCompleted,
    verificationStatusName = verificationStatus.name,
    createdAtEpochMs = createdAtEpochMs,
    completedAtEpochMs = completedAtEpochMs
)

fun ActivityLogEntity.toDomain(): ActivityLog = ActivityLog(
    id = id,
    title = title,
    category = runCatching { QuestCategory.valueOf(categoryName) }.getOrDefault(QuestCategory.DISCIPLINE),
    durationMinutes = durationMinutes,
    verificationStatus = runCatching { VerificationStatus.valueOf(verificationStatusName) }.getOrDefault(VerificationStatus.SELF_REPORTED),
    xpAwarded = xpAwarded,
    primaryAttribute = runCatching { AttributeType.valueOf(primaryAttrName) }.getOrDefault(AttributeType.DIS),
    primaryAttrXpAwarded = primaryAttrXpAwarded,
    notes = notes,
    timestampEpochMs = timestampEpochMs
)

fun ActivityLog.toEntity(): ActivityLogEntity = ActivityLogEntity(
    id = id,
    title = title,
    categoryName = category.name,
    durationMinutes = durationMinutes,
    verificationStatusName = verificationStatus.name,
    xpAwarded = xpAwarded,
    primaryAttrName = primaryAttribute.name,
    primaryAttrXpAwarded = primaryAttrXpAwarded,
    notes = notes,
    timestampEpochMs = timestampEpochMs
)

fun PhysicalStateEntity.toDomain(): PhysicalDevelopmentState = PhysicalDevelopmentState(
    baseJoggingTargetMinutes = baseJoggingTargetMinutes,
    aiRecommendedJoggingMinutes = aiRecommendedJoggingMinutes,
    completedJoggingMinutesToday = completedJoggingMinutesToday,
    workoutStatus = runCatching { WorkoutStatus.valueOf(workoutStatusName) }.getOrDefault(WorkoutStatus.PENDING),
    recoveryStatus = runCatching { RecoveryStatus.valueOf(recoveryStatusName) }.getOrDefault(RecoveryStatus.OPTIMAL),
    weeklySessionsCompleted = weeklySessionsCompleted,
    lastSessionTimestampMs = lastSessionTimestampMs,
    aiTrainingNote = aiTrainingNote
)

fun PhysicalDevelopmentState.toEntity(): PhysicalStateEntity = PhysicalStateEntity(
    id = 1,
    baseJoggingTargetMinutes = baseJoggingTargetMinutes,
    aiRecommendedJoggingMinutes = aiRecommendedJoggingMinutes,
    completedJoggingMinutesToday = completedJoggingMinutesToday,
    workoutStatusName = workoutStatus.name,
    recoveryStatusName = recoveryStatus.name,
    weeklySessionsCompleted = weeklySessionsCompleted,
    lastSessionTimestampMs = lastSessionTimestampMs,
    aiTrainingNote = aiTrainingNote
)

fun SystemNotificationEntity.toDomain(): SystemNotification = SystemNotification(
    id = id,
    title = title,
    message = message,
    category = runCatching { NotificationCategory.valueOf(categoryName) }.getOrDefault(NotificationCategory.SYSTEM_ALERT),
    isRead = isRead,
    timestampEpochMs = timestampEpochMs,
    deepLinkRoute = deepLinkRoute
)

fun SystemNotification.toEntity(): SystemNotificationEntity = SystemNotificationEntity(
    id = id,
    title = title,
    message = message,
    categoryName = category.name,
    isRead = isRead,
    timestampEpochMs = timestampEpochMs,
    deepLinkRoute = deepLinkRoute
)
