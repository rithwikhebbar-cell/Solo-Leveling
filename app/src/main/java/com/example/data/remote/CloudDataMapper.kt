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
    val cloudUuid: String = "",
    val localId: Long = 0L,
    val title: String = "",
    val description: String = "",
    val habitTypeName: String = "BOOLEAN",
    val categoryName: String = "GENERAL",
    val targetValue: Double = 1.0,
    val unit: String = "",
    val scheduledDaysCsv: String = "1,2,3,4,5,6,7",
    val preferredStartMinute: Int? = null,
    val reminderEnabled: Boolean = true,
    val isRestDayProtected: Boolean = true,
    val isArchived: Boolean = false,
    val createdAtEpochMs: Long = 0L,
    val updatedAtEpochMs: Long = 0L
)

@Serializable
data class CloudHabitCompletionPayload(
    val cloudUuid: String = "",
    val habitUuid: String = "",
    val localHabitId: Long = 0L,
    val dateEpochDay: Long = 0L,
    val statusName: String = "COMPLETED",
    val achievedValue: Double = 0.0,
    val skipReason: String = "",
    val note: String = "",
    val recordedAtEpochMs: Long = 0L
)

@Serializable
data class CloudBackupBundle(
    val schemaVersion: Int = 7,
    val exportedAtEpochMs: Long = 0L,
    val playerCodename: String = "Shadow Monarch",
    val playerTitle: String = "Awakened E-Rank",
    val playerLevel: Int = 1,
    val playerCurrentXp: Int = 0,
    val playerTotalXp: Long = 0L,
    val playerRankName: String = "E",
    val streakDays: Int = 0,
    val verifiedActivitiesCount: Int = 0,
    val selfReportedActivitiesCount: Int = 0,
    val habits: List<CloudHabitPayload> = emptyList(),
    val habitCompletions: List<CloudHabitCompletionPayload> = emptyList(),
    val tasks: List<CloudTaskPayload> = emptyList(),
    val progressionEvents: List<CloudProgressionEventPayload> = emptyList(),
    val settings: CloudSettingsPayload = CloudSettingsPayload()
)

@Serializable
data class CloudShadowMonarchProfilePayload(val accountOwnerId: String = "")
@Serializable
data class CloudShadowPayload(val accountOwnerId: String = "")
@Serializable
data class CloudShadowExtractionPayload(val accountOwnerId: String = "")
@Serializable
data class CloudShadowSkillUnlockPayload(val accountOwnerId: String = "")
@Serializable
data class CloudShadowDeploymentPayload(val accountOwnerId: String = "")
@Serializable
data class CloudShadowFormationPayload(val accountOwnerId: String = "")
@Serializable
data class CloudMonarchSkillUnlockPayload(val accountOwnerId: String = "")
@Serializable
data class CloudShadowQuestProgressPayload(val accountOwnerId: String = "")
@Serializable
data class CloudShadowResourceTransactionPayload(val accountOwnerId: String = "")
@Serializable
data class CloudShadowEvidenceClaimPayload(val accountOwnerId: String = "")
@Serializable
data class CloudShadowResonancePayload(val accountOwnerId: String = "")
@Serializable
data class CloudShadowBondProfilePayload(val accountOwnerId: String = "")
@Serializable
data class CloudShadowBondEventPayload(val accountOwnerId: String = "")
@Serializable
data class CloudShadowBondMilestonePayload(val accountOwnerId: String = "")
@Serializable
data class CloudShadowSquadPayload(val accountOwnerId: String = "")
@Serializable
data class CloudShadowSquadMemberPayload(val accountOwnerId: String = "")
@Serializable
data class CloudShadowMissionAssignmentPayload(val accountOwnerId: String = "")

fun Any.stableUuid(): String = UUID.nameUUIDFromBytes(this.toString().toByteArray(Charsets.UTF_8)).toString()

object CloudDataMapper {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        coerceInputValues = true
    }

    fun isValidUuid(raw: String?): Boolean {
        if (raw.isNullOrBlank()) return false
        return runCatching { UUID.fromString(raw.trim()) }.isSuccess
    }

    fun stableUuid(userId: String, entityType: SyncEntityType, localKey: String): String {
        val seed = "$userId:${entityType.name}:$localKey"
        return UUID.nameUUIDFromBytes(seed.toByteArray(Charsets.UTF_8)).toString()
    }

    fun stableUuid(userId: String, type: Any, id: String): String {
        val typeName = if (type is SyncEntityType) type.name else type.toString()
        val seed = "$userId:$typeName:$id"
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
        habitUuid: String = ""
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
            val resolvedUuid = if (isValidUuid(cloudUuid)) cloudUuid else stableUuid(userId, SyncEntityType.PHYSICAL_RECORD, entity.sourceId)
            put("id", resolvedUuid)
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

    fun academicSubjectToPostgrestRow(
        userId: String,
        entity: AcademicSubjectEntity,
        cloudUuid: String
    ): JsonObject {
        val resolvedUuid = if (isValidUuid(cloudUuid)) {
            cloudUuid
        } else {
            stableUuid(userId, SyncEntityType.ACADEMIC_SUBJECT, entity.id.toString())
        }
        return buildJsonObject {
            put("id", resolvedUuid)
            put("user_id", userId)
            put("name", entity.name)
            put("code", entity.code)
            put("program_system", entity.programSystem)
            put("priority", entity.priorityName)
            put("is_archived", entity.isArchived)
            put(
                "payload_json",
                buildJsonObject {
                    put("localId", entity.id)
                    put("colorTokenName", entity.colorTokenName)
                    put("iconKeyName", entity.iconKeyName)
                    put("notes", entity.notes)
                    put("createdAtEpochMs", entity.createdAtEpochMs)
                }
            )
        }
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

    fun shadowMonarchProfileToPayload(obj: Any, uuid: String): Any = obj
    fun shadowToPayload(obj: Any, uuid: String): Any = obj
    fun shadowExtractionToPayload(obj: Any, uuid: String): Any = obj
    fun shadowSkillUnlockToPayload(obj: Any, uuid: String): Any = obj
    fun shadowDeploymentToPayload(obj: Any, uuid: String): Any = obj
    fun shadowFormationToPayload(obj: Any, uuid: String): Any = obj
    fun monarchSkillUnlockToPayload(obj: Any, uuid: String): Any = obj
    fun shadowQuestProgressToPayload(obj: Any, uuid: String): Any = obj
    fun shadowResourceTransactionToPayload(obj: Any, uuid: String): Any = obj
    fun shadowEvidenceClaimToPayload(obj: Any, uuid: String): Any = obj
    fun shadowResonanceToPayload(obj: Any, uuid: String): Any = obj
    fun shadowBondProfileToPayload(obj: Any, uuid: String): Any = obj
    fun shadowSquadToPayload(obj: Any, uuid: String): Any = obj
    fun shadowSquadMemberToPayload(obj: Any, uuid: String): Any = obj
    fun shadowMissionAssignmentToPayload(obj: Any, uuid: String): Any = obj

    fun shadowMonarchProfileToPostgrestRow(userId: String, dto: Any?): JsonObject = JsonObject(emptyMap())
    fun shadowToPostgrestRow(userId: String, dto: Any?): JsonObject = JsonObject(emptyMap())
    fun shadowExtractionToPostgrestRow(userId: String, dto: Any?): JsonObject = JsonObject(emptyMap())
    fun shadowSkillUnlockToPostgrestRow(userId: String, dto: Any?): JsonObject = JsonObject(emptyMap())
    fun shadowDeploymentToPostgrestRow(userId: String, dto: Any?): JsonObject = JsonObject(emptyMap())
    fun shadowFormationToPostgrestRow(userId: String, dto: Any?): JsonObject = JsonObject(emptyMap())
    fun monarchSkillUnlockToPostgrestRow(userId: String, dto: Any?): JsonObject = JsonObject(emptyMap())
    fun shadowQuestProgressToPostgrestRow(userId: String, dto: Any?): JsonObject = JsonObject(emptyMap())
    fun shadowResourceTransactionToPostgrestRow(userId: String, dto: Any?): JsonObject = JsonObject(emptyMap())
    fun shadowEvidenceClaimToPostgrestRow(userId: String, dto: Any?): JsonObject = JsonObject(emptyMap())
    fun shadowResonanceToPostgrestRow(userId: String, dto: Any?): JsonObject = JsonObject(emptyMap())
    fun shadowBondProfileToPostgrestRow(userId: String, dto: Any?): JsonObject = JsonObject(emptyMap())
    fun shadowBondEventToPostgrestRow(userId: String, dto: Any?): JsonObject = JsonObject(emptyMap())
    fun shadowBondMilestoneToPostgrestRow(userId: String, dto: Any?): JsonObject = JsonObject(emptyMap())
    fun shadowSquadToPostgrestRow(userId: String, dto: Any?): JsonObject = JsonObject(emptyMap())
    fun shadowSquadMemberToPostgrestRow(userId: String, dto: Any?): JsonObject = JsonObject(emptyMap())
    fun shadowMissionAssignmentToPostgrestRow(userId: String, dto: Any?): JsonObject = JsonObject(emptyMap())
}
