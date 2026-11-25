package com.example.data.remote

import com.example.domain.model.CloudTaskPayload
import com.example.domain.model.CloudProgressionEventPayload
import com.example.domain.model.CloudSettingsPayload
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

@Serializable
data class CloudHabitPayload(
    val id: Long = 0L,
    val title: String = ""
)

@Serializable
data class CloudHabitCompletionPayload(
    val id: Long = 0L,
    val habitId: Long = 0L,
    val dateEpochDay: Long = 0L
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

fun Any.stableUuid(): String = "uuid_${System.identityHashCode(this)}"

object CloudDataMapper {
    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    fun stableUuid(userId: String, type: Any, id: String): String = "uuid_${userId}_${type}_${id}"

    fun taskToPayload(task: Any, uuid: String): CloudTaskPayload = CloudTaskPayload(
        cloudUuid = uuid,
        localId = 0L,
        title = "",
        categoryName = "GENERAL",
        priorityName = "MEDIUM",
        statusName = "PENDING"
    )
    fun habitToPayload(habit: Any, uuid: String): CloudHabitPayload = CloudHabitPayload()
    fun habitCompletionToPayload(comp: Any, uuid: String): CloudHabitCompletionPayload = CloudHabitCompletionPayload()
    fun habitCompletionToPayload(comp: Any, uuid: String, habitUuid: String): CloudHabitCompletionPayload = CloudHabitCompletionPayload()
    fun joggingToPostgrestRow(userId: String, dto: Any?, uuid: String = ""): JsonObject = JsonObject(emptyMap())
    fun academicSubjectToPostgrestRow(userId: String, dto: Any?, uuid: String = ""): JsonObject = JsonObject(emptyMap())

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

    fun taskPayloadToPostgrestRow(userId: String, dto: Any?): JsonObject = JsonObject(emptyMap())
    fun habitPayloadToPostgrestRow(userId: String, dto: Any?): JsonObject = JsonObject(emptyMap())
    fun habitCompletionPayloadToPostgrestRow(userId: String, dto: Any?): JsonObject = JsonObject(emptyMap())
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
