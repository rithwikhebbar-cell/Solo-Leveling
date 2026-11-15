package com.example.data.remote

import com.example.data.local.entity.AcademicSubjectEntity
import com.example.data.local.entity.HabitCompletionEntity
import com.example.data.local.entity.HabitEntity
import com.example.data.local.entity.JoggingSessionEntity
import com.example.data.local.entity.PersonalTaskEntity
import com.example.domain.model.CloudProgressionEventPayload
import com.example.domain.model.CloudSettingsPayload
import com.example.domain.model.CloudTaskPayload
import com.example.domain.model.SyncEntityType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.util.UUID

@Serializable
data class CloudHabitPayload(
    val cloudUuid: String,
    val localId: Long = 0,
    val title: String,
    val description: String = "",
    val habitTypeName: String,
    val categoryName: String,
    val targetValue: Double,
    val unit: String,
    val scheduledDaysCsv: String,
    val preferredStartMinute: Int? = null,
    val reminderEnabled: Boolean = true,
    val isRestDayProtected: Boolean = true,
    val isArchived: Boolean = false,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)

@Serializable
data class CloudHabitCompletionPayload(
    val cloudUuid: String,
    val habitUuid: String,
    val localHabitId: Long,
    val dateEpochDay: Long,
    val statusName: String,
    val achievedValue: Double,
    val skipReason: String = "",
    val note: String = "",
    val recordedAtEpochMs: Long
)

@Serializable
data class CloudBackupBundle(
    val schemaVersion: Int,
    val exportedAtEpochMs: Long,
    val playerCodename: String,
    val playerTitle: String,
    val playerLevel: Int,
    val playerCurrentXp: Int,
    val playerTotalXp: Long,
    val playerRankName: String,
    val streakDays: Int,
    val verifiedActivitiesCount: Int,
    val selfReportedActivitiesCount: Int,
    val habits: List<CloudHabitPayload> = emptyList(),
    val habitCompletions: List<CloudHabitCompletionPayload> = emptyList(),
    val tasks: List<CloudTaskPayload> = emptyList(),
    val progressionEvents: List<CloudProgressionEventPayload> = emptyList(),
    val settings: CloudSettingsPayload = CloudSettingsPayload()
)

object CloudDataMapper {
    val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }

    fun stableUuid(userId: String, entityType: SyncEntityType, localKey: String): String {
        val seed = "$userId:${entityType.name}:$localKey"
        return UUID.nameUUIDFromBytes(seed.toByteArray(Charsets.UTF_8)).toString()
    }

    fun taskToPayload(entity: PersonalTaskEntity, cloudUuid: String, deletedAtEpochMs: Long? = null): CloudTaskPayload =
        CloudTaskPayload(
            cloudUuid = cloudUuid,
            localId = entity.id,
            title = entity.title,
            description = entity.description,
            categoryName = entity.categoryName,
            priorityName = entity.priorityName,
            statusName = entity.statusName,
            dueDateEpochDay = entity.dueDateEpochDay,
            estimatedMinutes = entity.estimatedMinutes,
            postponeCount = entity.postponeCount,
            completedAtEpochMs = entity.completedAtEpochMs,
            deletedAtEpochMs = deletedAtEpochMs,
            updatedAtEpochMs = entity.updatedAtEpochMs
        )

    fun habitToPayload(entity: HabitEntity, cloudUuid: String): CloudHabitPayload =
        CloudHabitPayload(
            cloudUuid = cloudUuid,
            localId = entity.id,
            title = entity.title,
            description = entity.description,
            habitTypeName = entity.habitTypeName,
            categoryName = entity.categoryName,
            targetValue = entity.targetValue,
            unit = entity.unit,
            scheduledDaysCsv = entity.scheduledDaysCsv,
            preferredStartMinute = entity.preferredStartMinute,
            reminderEnabled = entity.reminderEnabled,
            isRestDayProtected = entity.isRestDayProtected,
            isArchived = entity.isArchived,
            createdAtEpochMs = entity.createdAtEpochMs,
            updatedAtEpochMs = entity.createdAtEpochMs
        )

    fun habitCompletionToPayload(
        entity: HabitCompletionEntity,
        cloudUuid: String,
        habitUuid: String
    ): CloudHabitCompletionPayload = CloudHabitCompletionPayload(
        cloudUuid = cloudUuid,
        habitUuid = habitUuid,
        localHabitId = entity.habitId,
        dateEpochDay = entity.dateEpochDay,
        statusName = entity.statusName,
        achievedValue = entity.achievedValue,
        skipReason = entity.skipReason,
        note = entity.note,
        recordedAtEpochMs = entity.recordedAtEpochMs
    )

    fun taskPayloadToPostgrestRow(userId: String, payload: CloudTaskPayload): JsonObject = buildJsonObject {
        put("id", payload.cloudUuid)
        put("user_id", userId)
        put("title", payload.title)
        put("description", payload.description)
        put("category", payload.categoryName)
        put("priority", payload.priorityName)
        put("status", payload.statusName)
        payload.dueDateEpochDay?.let { put("due_date_epoch_day", it) }
        put("estimated_minutes", payload.estimatedMinutes)
        put("postpone_count", payload.postponeCount)
        payload.completedAtEpochMs?.let { put("completed_at_epoch_ms", it) }
        payload.deletedAtEpochMs?.let { put("deleted_at", Instant.ofEpochMilli(it).toString()) }
        put("payload_json", json.decodeFromString<JsonObject>(json.encodeToString(payload)))
    }

    fun habitPayloadToPostgrestRow(userId: String, payload: CloudHabitPayload): JsonObject = buildJsonObject {
        put("id", payload.cloudUuid)
        put("user_id", userId)
        put("title", payload.title)
        put("habit_type", payload.habitTypeName)
        put("category", payload.categoryName)
        put("target_value", payload.targetValue)
        put("unit", payload.unit)
        put("is_archived", payload.isArchived)
        put("payload_json", json.decodeFromString<JsonObject>(json.encodeToString(payload)))
    }

    fun habitCompletionPayloadToPostgrestRow(
        userId: String,
        payload: CloudHabitCompletionPayload
    ): JsonObject = buildJsonObject {
        put("id", payload.cloudUuid)
        put("user_id", userId)
        put("habit_id", payload.habitUuid)
        put("date_epoch_day", payload.dateEpochDay)
        put("status", payload.statusName)
        put("achieved_value", payload.achievedValue)
        put("note", payload.note)
        put("recorded_at_epoch_ms", payload.recordedAtEpochMs)
    }

    fun joggingToPostgrestRow(userId: String, entity: JoggingSessionEntity, cloudUuid: String): JsonObject =
        buildJsonObject {
            put("id", cloudUuid)
            put("user_id", userId)
            put("source_id", entity.sourceId)
            put("record_type", "JOGGING")
            put("title", entity.title)
            put("activity_kind", entity.activityKindName)
            put("origin_app", entity.originApp)
            put("verification_status", entity.verificationStatusName)
            put("start_epoch_ms", entity.startEpochMs)
            put("end_epoch_ms", entity.endEpochMs)
            put("duration_minutes", entity.durationMinutes)
            entity.distanceMeters?.let { put("distance_meters", it) }
            entity.avgHeartRateBpm?.let { put("avg_hr_bpm", it) }
            put("xp_awarded", entity.xpAwarded)
        }

    fun academicSubjectToPostgrestRow(userId: String, entity: AcademicSubjectEntity, cloudUuid: String): JsonObject =
        buildJsonObject {
            put("id", cloudUuid)
            put("user_id", userId)
            put("name", entity.name)
            put("code", entity.code)
            put("program_system", entity.programSystem)
            put("priority", entity.priorityName)
            put("is_archived", entity.isArchived)
        }

    fun settingsToPostgrestRow(userId: String, payload: CloudSettingsPayload): JsonObject =
        buildJsonObject {
            put("user_id", userId)
            put("hud_glow_enabled", payload.hudGlowEnabled)
            put("daily_reset_hour", payload.dailyResetHour)
            put("health_connect_sync_enabled", payload.healthConnectSyncEnabled)
            put("supabase_cloud_sync_enabled", payload.supabaseCloudSyncEnabled)
            put("academic_daily_goal_minutes", payload.academicDailyGoalMinutes)
            put("academic_weekly_goal_minutes", payload.academicWeeklyGoalMinutes)
            put("ai_model_id", payload.openRouterModelId)
        }
}
