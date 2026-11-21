package com.example.data.remote

import com.example.data.local.entity.AcademicSubjectEntity
import com.example.data.local.entity.HabitCompletionEntity
import com.example.data.local.entity.HabitEntity
import com.example.data.local.entity.JoggingSessionEntity
import com.example.data.local.entity.MonarchSkillUnlockEntity
import com.example.data.local.entity.PersonalTaskEntity
import com.example.data.local.entity.ShadowDeploymentEntity
import com.example.data.local.entity.ShadowEntity
import com.example.data.local.entity.ShadowExtractionEntity
import com.example.data.local.entity.ShadowFormationEntity
import com.example.data.local.entity.ShadowMonarchProfileEntity
import com.example.data.local.entity.ShadowQuestProgressEntity
import com.example.data.local.entity.ShadowResourceTransactionEntity
import com.example.data.local.entity.ShadowSkillUnlockEntity
import com.example.data.local.entity.ShadowEvidenceClaimEntity
import com.example.data.local.entity.ShadowResonanceEntity
import com.example.data.local.entity.ShadowBondProfileEntity
import com.example.data.local.entity.ShadowBondEventEntity
import com.example.data.local.entity.ShadowBondMilestoneEntity
import com.example.data.local.entity.ShadowSquadEntity
import com.example.data.local.entity.ShadowSquadMemberEntity
import com.example.data.local.entity.ShadowMissionAssignmentEntity
import com.example.domain.model.CloudProgressionEventPayload
import com.example.domain.model.CloudSettingsPayload
import com.example.domain.model.CloudTaskPayload
import com.example.domain.model.SyncEntityType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
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
data class CloudShadowMonarchProfilePayload(
    val cloudUuid: String,
    val accountOwnerId: String,
    val monarchLevel: Int,
    val monarchXp: Long,
    val monarchRankName: String,
    val monarchAuthority: Int,
    val armyCapacity: Int,
    val activeShadowCount: Int,
    val totalShadowsRecruited: Int,
    val highestShadowRankName: String,
    val totalEvolutions: Int,
    val shadowArmyPowerRating: Long,
    val unlockedMonarchSkillIdsJson: String,
    val updatedAtEpochMs: Long
)

@Serializable
data class CloudShadowPayload(
    val cloudUuid: String,
    val accountOwnerId: String,
    val shadowId: String,
    val name: String,
    val shadowClassName: String,
    val rarityName: String,
    val currentRankName: String,
    val level: Int,
    val shadowXp: Long,
    val statPower: Int,
    val statEndurance: Int,
    val statFocus: Int,
    val statDiscipline: Int,
    val statWisdom: Int,
    val statAdaptability: Int,
    val evolutionStageName: String,
    val passiveAbility: String,
    val activeSkillIdsJson: String,
    val recruitmentSource: String,
    val extractionEventId: String,
    val extractedAtEpochMs: Long,
    val evolutionHistoryJson: String,
    val deploymentStatusName: String,
    val equippedFormationId: String? = null,
    val isRetired: Boolean = false,
    val notes: String = ""
)

@Serializable
data class CloudShadowExtractionPayload(
    val cloudUuid: String,
    val accountOwnerId: String,
    val extractionId: String,
    val sourceTypeName: String,
    val sourceEventId: String,
    val outcomeName: String,
    val extractedShadowId: String? = null,
    val shadowName: String? = null,
    val shadowClassName: String? = null,
    val shadowRarityName: String? = null,
    val determinismSeed: Long,
    val evidenceSummary: String,
    val timestampEpochMs: Long,
    val isConfirmed: Boolean,
    val monarchXpAwarded: Int
)

@Serializable
data class CloudShadowDeploymentPayload(
    val cloudUuid: String,
    val accountOwnerId: String,
    val deploymentId: String,
    val shadowId: String,
    val shadowName: String,
    val categoryName: String,
    val role: String,
    val assignedAtEpochMs: Long,
    val recalledAtEpochMs: Long? = null,
    val statusName: String,
    val notes: String = ""
)

@Serializable
data class CloudShadowFormationPayload(
    val cloudUuid: String,
    val accountOwnerId: String,
    val formationId: String,
    val name: String,
    val formationTypeName: String,
    val description: String,
    val maxSlots: Int,
    val memberShadowIdsJson: String,
    val isActive: Boolean,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)

@Serializable
data class CloudShadowSkillUnlockPayload(
    val cloudUuid: String,
    val accountOwnerId: String,
    val shadowId: String,
    val skillId: String,
    val unlockedAtEpochMs: Long,
    val evidenceNote: String = ""
)

@Serializable
data class CloudMonarchSkillUnlockPayload(
    val cloudUuid: String,
    val accountOwnerId: String,
    val skillId: String,
    val unlockedAtEpochMs: Long,
    val evidenceNote: String = ""
)

@Serializable
data class CloudShadowQuestProgressPayload(
    val cloudUuid: String,
    val accountOwnerId: String,
    val questId: String,
    val currentValue: Int,
    val isCompleted: Boolean,
    val rewardClaimedAtEpochMs: Long? = null,
    val completedAtEpochMs: Long? = null
)

@Serializable
data class CloudShadowResourceTransactionPayload(
    val cloudUuid: String,
    val accountOwnerId: String,
    val transactionId: String,
    val resourceTypeName: String,
    val amount: Int,
    val transactionTypeName: String,
    val sourceEventId: String,
    val sourceDescription: String,
    val balanceAfter: Int,
    val timestampEpochMs: Long
)

@Serializable
data class CloudShadowEvidenceClaimPayload(
    val cloudUuid: String,
    val accountOwnerId: String,
    val claimId: String,
    val sourceType: String,
    val sourceRecordId: String,
    val claimedAtEpochMs: Long
)

@Serializable
data class CloudShadowResonancePayload(
    val cloudUuid: String,
    val accountOwnerId: String,
    val resonanceId: String,
    val archetypeId: String,
    val resonanceLevel: Int,
    val resonanceShards: Int,
    val lastResonanceEpochMs: Long
)

@Serializable
data class CloudShadowBondProfilePayload(
    val cloudUuid: String,
    val accountOwnerId: String,
    val shadowId: String,
    val bondLevel: Int,
    val bondPoints: Long,
    val loyaltyRating: Int,
    val trustRating: Int,
    val relationshipStageName: String,
    val personalityCharacteristicName: String,
    val totalActivitiesCount: Int,
    val lastBondActivityEpochMs: Long,
    val updatedAtEpochMs: Long
)

@Serializable
data class CloudShadowBondEventPayload(
    val cloudUuid: String,
    val accountOwnerId: String,
    val eventId: String,
    val shadowId: String,
    val activityTypeName: String,
    val sourceEventId: String,
    val bondPointsAwarded: Int,
    val loyaltyDelta: Int,
    val trustDelta: Int,
    val description: String,
    val timestampEpochMs: Long,
    val idempotencyKey: String
)

@Serializable
data class CloudShadowBondMilestonePayload(
    val cloudUuid: String,
    val accountOwnerId: String,
    val milestoneId: String,
    val shadowId: String,
    val milestoneTypeName: String,
    val title: String,
    val description: String,
    val sourceEventId: String,
    val timestampEpochMs: Long,
    val verificationStatus: String
)

@Serializable
data class CloudShadowSquadPayload(
    val cloudUuid: String,
    val accountOwnerId: String,
    val squadId: String,
    val name: String,
    val categoryName: String,
    val formationTypeName: String,
    val leaderShadowId: String? = null,
    val maxMembers: Int = 6,
    val isActive: Boolean = false,
    val notes: String = "",
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)

@Serializable
data class CloudShadowSquadMemberPayload(
    val cloudUuid: String,
    val accountOwnerId: String,
    val squadId: String,
    val shadowId: String,
    val positionIndex: Int = 0,
    val isLeader: Boolean = false,
    val assignedAtEpochMs: Long
)

@Serializable
data class CloudShadowMissionAssignmentPayload(
    val cloudUuid: String,
    val accountOwnerId: String,
    val assignmentId: String,
    val missionId: String,
    val missionTitle: String,
    val missionTypeName: String,
    val squadId: String? = null,
    val assignedShadowIdsJson: String,
    val recommendedStrategy: String,
    val suitabilityScore: Int,
    val statusName: String,
    val assignedAtEpochMs: Long,
    val completedAtEpochMs: Long? = null,
    val successRateProjected: Int,
    val resultOutcome: String? = null
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

    private fun safeJsonStringArray(rawJson: String): JsonArray {
        val list = runCatching { json.decodeFromString<List<String>>(rawJson) }.getOrDefault(emptyList())
        return buildJsonArray {
            list.forEach { add(JsonPrimitive(it)) }
        }
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

    fun shadowMonarchProfileToPayload(
        entity: ShadowMonarchProfileEntity,
        cloudUuid: String
    ): CloudShadowMonarchProfilePayload = CloudShadowMonarchProfilePayload(
        cloudUuid = cloudUuid,
        accountOwnerId = entity.accountOwnerId,
        monarchLevel = entity.monarchLevel,
        monarchXp = entity.monarchXp,
        monarchRankName = entity.monarchRankName,
        monarchAuthority = entity.monarchAuthority,
        armyCapacity = entity.armyCapacity,
        activeShadowCount = entity.activeShadowCount,
        totalShadowsRecruited = entity.totalShadowsRecruited,
        highestShadowRankName = entity.highestShadowRankName,
        totalEvolutions = entity.totalEvolutions,
        shadowArmyPowerRating = entity.shadowArmyPowerRating,
        unlockedMonarchSkillIdsJson = entity.unlockedMonarchSkillIdsJson,
        updatedAtEpochMs = entity.updatedAtEpochMs
    )

    fun shadowToPayload(entity: ShadowEntity, cloudUuid: String): CloudShadowPayload = CloudShadowPayload(
        cloudUuid = cloudUuid,
        accountOwnerId = entity.accountOwnerId,
        shadowId = entity.shadowId,
        name = entity.name,
        shadowClassName = entity.shadowClassName,
        rarityName = entity.rarityName,
        currentRankName = entity.currentRankName,
        level = entity.level,
        shadowXp = entity.shadowXp,
        statPower = entity.statPower,
        statEndurance = entity.statEndurance,
        statFocus = entity.statFocus,
        statDiscipline = entity.statDiscipline,
        statWisdom = entity.statWisdom,
        statAdaptability = entity.statAdaptability,
        evolutionStageName = entity.evolutionStageName,
        passiveAbility = entity.passiveAbility,
        activeSkillIdsJson = entity.activeSkillIdsJson,
        recruitmentSource = entity.recruitmentSource,
        extractionEventId = entity.extractionEventId,
        extractedAtEpochMs = entity.extractedAtEpochMs,
        evolutionHistoryJson = entity.evolutionHistoryJson,
        deploymentStatusName = entity.deploymentStatusName,
        equippedFormationId = entity.equippedFormationId,
        isRetired = entity.isRetired,
        notes = entity.notes
    )

    fun shadowExtractionToPayload(
        entity: ShadowExtractionEntity,
        cloudUuid: String
    ): CloudShadowExtractionPayload = CloudShadowExtractionPayload(
        cloudUuid = cloudUuid,
        accountOwnerId = entity.accountOwnerId,
        extractionId = entity.extractionId,
        sourceTypeName = entity.sourceTypeName,
        sourceEventId = entity.sourceEventId,
        outcomeName = entity.outcomeName,
        extractedShadowId = entity.extractedShadowId,
        shadowName = entity.shadowName,
        shadowClassName = entity.shadowClassName,
        shadowRarityName = entity.shadowRarityName,
        determinismSeed = entity.determinismSeed,
        evidenceSummary = entity.evidenceSummary,
        timestampEpochMs = entity.timestampEpochMs,
        isConfirmed = entity.isConfirmed,
        monarchXpAwarded = entity.monarchXpAwarded
    )

    fun shadowDeploymentToPayload(
        entity: ShadowDeploymentEntity,
        cloudUuid: String
    ): CloudShadowDeploymentPayload = CloudShadowDeploymentPayload(
        cloudUuid = cloudUuid,
        accountOwnerId = entity.accountOwnerId,
        deploymentId = entity.deploymentId,
        shadowId = entity.shadowId,
        shadowName = entity.shadowName,
        categoryName = entity.categoryName,
        role = entity.role,
        assignedAtEpochMs = entity.assignedAtEpochMs,
        recalledAtEpochMs = entity.recalledAtEpochMs,
        statusName = entity.statusName,
        notes = entity.notes
    )

    fun shadowFormationToPayload(
        entity: ShadowFormationEntity,
        cloudUuid: String
    ): CloudShadowFormationPayload = CloudShadowFormationPayload(
        cloudUuid = cloudUuid,
        accountOwnerId = entity.accountOwnerId,
        formationId = entity.formationId,
        name = entity.name,
        formationTypeName = entity.formationTypeName,
        description = entity.description,
        maxSlots = entity.maxSlots,
        memberShadowIdsJson = entity.memberShadowIdsJson,
        isActive = entity.isActive,
        createdAtEpochMs = entity.createdAtEpochMs,
        updatedAtEpochMs = entity.updatedAtEpochMs
    )

    fun shadowSkillUnlockToPayload(
        entity: ShadowSkillUnlockEntity,
        cloudUuid: String
    ): CloudShadowSkillUnlockPayload = CloudShadowSkillUnlockPayload(
        cloudUuid = cloudUuid,
        accountOwnerId = entity.accountOwnerId,
        shadowId = entity.shadowId,
        skillId = entity.skillId,
        unlockedAtEpochMs = entity.unlockedAtEpochMs,
        evidenceNote = entity.evidenceNote
    )

    fun monarchSkillUnlockToPayload(
        entity: MonarchSkillUnlockEntity,
        cloudUuid: String
    ): CloudMonarchSkillUnlockPayload = CloudMonarchSkillUnlockPayload(
        cloudUuid = cloudUuid,
        accountOwnerId = entity.accountOwnerId,
        skillId = entity.skillId,
        unlockedAtEpochMs = entity.unlockedAtEpochMs,
        evidenceNote = entity.evidenceNote
    )

    fun shadowQuestProgressToPayload(
        entity: ShadowQuestProgressEntity,
        cloudUuid: String
    ): CloudShadowQuestProgressPayload = CloudShadowQuestProgressPayload(
        cloudUuid = cloudUuid,
        accountOwnerId = entity.accountOwnerId,
        questId = entity.questId,
        currentValue = entity.currentValue,
        isCompleted = entity.isCompleted,
        rewardClaimedAtEpochMs = entity.rewardClaimedAtEpochMs,
        completedAtEpochMs = entity.completedAtEpochMs
    )

    fun shadowResourceTransactionToPayload(
        entity: ShadowResourceTransactionEntity,
        cloudUuid: String
    ): CloudShadowResourceTransactionPayload = CloudShadowResourceTransactionPayload(
        cloudUuid = cloudUuid,
        accountOwnerId = entity.accountOwnerId,
        transactionId = entity.transactionId,
        resourceTypeName = entity.resourceTypeName,
        amount = entity.amount,
        transactionTypeName = entity.transactionTypeName,
        sourceEventId = entity.sourceEventId,
        sourceDescription = entity.sourceDescription,
        balanceAfter = entity.balanceAfter,
        timestampEpochMs = entity.timestampEpochMs
    )

    fun shadowEvidenceClaimToPayload(
        entity: ShadowEvidenceClaimEntity,
        cloudUuid: String
    ): CloudShadowEvidenceClaimPayload = CloudShadowEvidenceClaimPayload(
        cloudUuid = cloudUuid,
        accountOwnerId = entity.accountOwnerId,
        claimId = entity.claimId,
        sourceType = entity.sourceType,
        sourceRecordId = entity.sourceRecordId,
        claimedAtEpochMs = entity.claimedAtEpochMs
    )

    fun shadowResonanceToPayload(
        entity: ShadowResonanceEntity,
        cloudUuid: String
    ): CloudShadowResonancePayload = CloudShadowResonancePayload(
        cloudUuid = cloudUuid,
        accountOwnerId = entity.accountOwnerId,
        resonanceId = entity.resonanceId,
        archetypeId = entity.archetypeId,
        resonanceLevel = entity.resonanceLevel,
        resonanceShards = entity.resonanceShards,
        lastResonanceEpochMs = entity.lastResonanceEpochMs
    )

    fun shadowBondProfileToPayload(
        entity: ShadowBondProfileEntity,
        cloudUuid: String
    ): CloudShadowBondProfilePayload = CloudShadowBondProfilePayload(
        cloudUuid = cloudUuid,
        accountOwnerId = entity.accountOwnerId,
        shadowId = entity.shadowId,
        bondLevel = entity.bondLevel,
        bondPoints = entity.bondPoints,
        loyaltyRating = entity.loyaltyRating,
        trustRating = entity.trustRating,
        relationshipStageName = entity.relationshipStageName,
        personalityCharacteristicName = entity.personalityCharacteristicName,
        totalActivitiesCount = entity.totalActivitiesCount,
        lastBondActivityEpochMs = entity.lastBondActivityEpochMs,
        updatedAtEpochMs = entity.updatedAtEpochMs
    )

    fun shadowBondEventToPayload(
        entity: ShadowBondEventEntity,
        cloudUuid: String
    ): CloudShadowBondEventPayload = CloudShadowBondEventPayload(
        cloudUuid = cloudUuid,
        accountOwnerId = entity.accountOwnerId,
        eventId = entity.eventId,
        shadowId = entity.shadowId,
        activityTypeName = entity.activityTypeName,
        sourceEventId = entity.sourceEventId,
        bondPointsAwarded = entity.bondPointsAwarded,
        loyaltyDelta = entity.loyaltyDelta,
        trustDelta = entity.trustDelta,
        description = entity.description,
        timestampEpochMs = entity.timestampEpochMs,
        idempotencyKey = entity.idempotencyKey
    )

    fun shadowBondMilestoneToPayload(
        entity: ShadowBondMilestoneEntity,
        cloudUuid: String
    ): CloudShadowBondMilestonePayload = CloudShadowBondMilestonePayload(
        cloudUuid = cloudUuid,
        accountOwnerId = entity.accountOwnerId,
        milestoneId = entity.milestoneId,
        shadowId = entity.shadowId,
        milestoneTypeName = entity.milestoneTypeName,
        title = entity.title,
        description = entity.description,
        sourceEventId = entity.sourceEventId,
        timestampEpochMs = entity.timestampEpochMs,
        verificationStatus = entity.verificationStatus
    )

    fun shadowSquadToPayload(
        entity: ShadowSquadEntity,
        cloudUuid: String
    ): CloudShadowSquadPayload = CloudShadowSquadPayload(
        cloudUuid = cloudUuid,
        accountOwnerId = entity.accountOwnerId,
        squadId = entity.squadId,
        name = entity.name,
        categoryName = entity.categoryName,
        formationTypeName = entity.formationTypeName,
        leaderShadowId = entity.leaderShadowId,
        maxMembers = entity.maxMembers,
        isActive = entity.isActive,
        notes = entity.notes,
        createdAtEpochMs = entity.createdAtEpochMs,
        updatedAtEpochMs = entity.updatedAtEpochMs
    )

    fun shadowSquadMemberToPayload(
        entity: ShadowSquadMemberEntity,
        cloudUuid: String
    ): CloudShadowSquadMemberPayload = CloudShadowSquadMemberPayload(
        cloudUuid = cloudUuid,
        accountOwnerId = entity.accountOwnerId,
        squadId = entity.squadId,
        shadowId = entity.shadowId,
        positionIndex = entity.positionIndex,
        isLeader = entity.isLeader,
        assignedAtEpochMs = entity.assignedAtEpochMs
    )

    fun shadowMissionAssignmentToPayload(
        entity: ShadowMissionAssignmentEntity,
        cloudUuid: String
    ): CloudShadowMissionAssignmentPayload = CloudShadowMissionAssignmentPayload(
        cloudUuid = cloudUuid,
        accountOwnerId = entity.accountOwnerId,
        assignmentId = entity.assignmentId,
        missionId = entity.missionId,
        missionTitle = entity.missionTitle,
        missionTypeName = entity.missionTypeName,
        squadId = entity.squadId,
        assignedShadowIdsJson = entity.assignedShadowIdsJson,
        recommendedStrategy = entity.recommendedStrategy,
        suitabilityScore = entity.suitabilityScore,
        statusName = entity.statusName,
        assignedAtEpochMs = entity.assignedAtEpochMs,
        completedAtEpochMs = entity.completedAtEpochMs,
        successRateProjected = entity.successRateProjected,
        resultOutcome = entity.resultOutcome
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

    fun shadowMonarchProfileToPostgrestRow(
        userId: String,
        payload: CloudShadowMonarchProfilePayload
    ): JsonObject = buildJsonObject {
        put("id", payload.cloudUuid)
        put("user_id", userId)
        put("monarch_level", payload.monarchLevel)
        put("monarch_xp", payload.monarchXp)
        put("monarch_rank_name", payload.monarchRankName)
        put("monarch_authority", payload.monarchAuthority)
        put("army_capacity", payload.armyCapacity)
        put("active_shadow_count", payload.activeShadowCount)
        put("total_shadows_recruited", payload.totalShadowsRecruited)
        put("highest_shadow_rank_name", payload.highestShadowRankName)
        put("total_evolutions", payload.totalEvolutions)
        put("shadow_army_power_rating", payload.shadowArmyPowerRating)
        put("unlocked_monarch_skill_ids_json", safeJsonStringArray(payload.unlockedMonarchSkillIdsJson))
    }

    fun shadowToPostgrestRow(userId: String, payload: CloudShadowPayload): JsonObject = buildJsonObject {
        put("id", payload.cloudUuid)
        put("user_id", userId)
        put("shadow_id", payload.shadowId)
        put("name", payload.name)
        put("shadow_class_name", payload.shadowClassName)
        put("rarity_name", payload.rarityName)
        put("current_rank_name", payload.currentRankName)
        put("level", payload.level)
        put("shadow_xp", payload.shadowXp)
        put("stat_power", payload.statPower)
        put("stat_endurance", payload.statEndurance)
        put("stat_focus", payload.statFocus)
        put("stat_discipline", payload.statDiscipline)
        put("stat_wisdom", payload.statWisdom)
        put("stat_adaptability", payload.statAdaptability)
        put("evolution_stage_name", payload.evolutionStageName)
        put("passive_ability", payload.passiveAbility)
        put("active_skill_ids_json", safeJsonStringArray(payload.activeSkillIdsJson))
        put("recruitment_source", payload.recruitmentSource)
        put("extraction_event_id", payload.extractionEventId)
        put("extracted_at_epoch_ms", payload.extractedAtEpochMs)
        put("evolution_history_json", safeJsonStringArray(payload.evolutionHistoryJson))
        put("deployment_status_name", payload.deploymentStatusName)
        payload.equippedFormationId?.let { put("equipped_formation_id", it) }
        put("is_retired", payload.isRetired)
        put("notes", payload.notes)
    }

    fun shadowExtractionToPostgrestRow(
        userId: String,
        payload: CloudShadowExtractionPayload
    ): JsonObject = buildJsonObject {
        put("id", payload.cloudUuid)
        put("user_id", userId)
        put("extraction_id", payload.extractionId)
        put("source_type_name", payload.sourceTypeName)
        put("source_event_id", payload.sourceEventId)
        put("outcome_name", payload.outcomeName)
        payload.extractedShadowId?.let { put("extracted_shadow_id", it) }
        payload.shadowName?.let { put("shadow_name", it) }
        payload.shadowClassName?.let { put("shadow_class_name", it) }
        payload.shadowRarityName?.let { put("shadow_rarity_name", it) }
        put("determinism_seed", payload.determinismSeed)
        put("evidence_summary", payload.evidenceSummary)
        put("timestamp_epoch_ms", payload.timestampEpochMs)
        put("is_confirmed", payload.isConfirmed)
        put("monarch_xp_awarded", payload.monarchXpAwarded)
    }

    fun shadowDeploymentToPostgrestRow(
        userId: String,
        payload: CloudShadowDeploymentPayload
    ): JsonObject = buildJsonObject {
        put("id", payload.cloudUuid)
        put("user_id", userId)
        put("deployment_id", payload.deploymentId)
        put("shadow_id", payload.shadowId)
        put("shadow_name", payload.shadowName)
        put("category_name", payload.categoryName)
        put("role", payload.role)
        put("assigned_at_epoch_ms", payload.assignedAtEpochMs)
        payload.recalledAtEpochMs?.let { put("recalled_at_epoch_ms", it) }
        put("status_name", payload.statusName)
        put("notes", payload.notes)
    }

    fun shadowFormationToPostgrestRow(
        userId: String,
        payload: CloudShadowFormationPayload
    ): JsonObject = buildJsonObject {
        put("id", payload.cloudUuid)
        put("user_id", userId)
        put("formation_id", payload.formationId)
        put("name", payload.name)
        put("formation_type_name", payload.formationTypeName)
        put("description", payload.description)
        put("max_slots", payload.maxSlots)
        put("member_shadow_ids_json", safeJsonStringArray(payload.memberShadowIdsJson))
        put("is_active", payload.isActive)
        put("created_at_epoch_ms", payload.createdAtEpochMs)
        put("updated_at_epoch_ms", payload.updatedAtEpochMs)
    }

    fun shadowSkillUnlockToPostgrestRow(
        userId: String,
        payload: CloudShadowSkillUnlockPayload
    ): JsonObject = buildJsonObject {
        put("id", payload.cloudUuid)
        put("user_id", userId)
        put("shadow_id", payload.shadowId)
        put("skill_id", payload.skillId)
        put("unlocked_at_epoch_ms", payload.unlockedAtEpochMs)
        put("evidence_note", payload.evidenceNote)
    }

    fun monarchSkillUnlockToPostgrestRow(
        userId: String,
        payload: CloudMonarchSkillUnlockPayload
    ): JsonObject = buildJsonObject {
        put("id", payload.cloudUuid)
        put("user_id", userId)
        put("skill_id", payload.skillId)
        put("unlocked_at_epoch_ms", payload.unlockedAtEpochMs)
        put("evidence_note", payload.evidenceNote)
    }

    fun shadowQuestProgressToPostgrestRow(
        userId: String,
        payload: CloudShadowQuestProgressPayload
    ): JsonObject = buildJsonObject {
        put("id", payload.cloudUuid)
        put("user_id", userId)
        put("quest_id", payload.questId)
        put("current_value", payload.currentValue)
        put("is_completed", payload.isCompleted)
        payload.rewardClaimedAtEpochMs?.let { put("reward_claimed_at_epoch_ms", it) }
        payload.completedAtEpochMs?.let { put("completed_at_epoch_ms", it) }
    }

    fun shadowResourceTransactionToPostgrestRow(
        userId: String,
        payload: CloudShadowResourceTransactionPayload
    ): JsonObject = buildJsonObject {
        put("id", payload.cloudUuid)
        put("user_id", userId)
        put("transaction_id", payload.transactionId)
        put("resource_type_name", payload.resourceTypeName)
        put("amount", payload.amount)
        put("transaction_type_name", payload.transactionTypeName)
        put("source_event_id", payload.sourceEventId)
        put("source_description", payload.sourceDescription)
        put("balance_after", payload.balanceAfter)
        put("timestamp_epoch_ms", payload.timestampEpochMs)
    }

    fun shadowEvidenceClaimToPostgrestRow(
        userId: String,
        payload: CloudShadowEvidenceClaimPayload
    ): JsonObject = buildJsonObject {
        put("id", payload.cloudUuid)
        put("user_id", userId)
        put("claim_id", payload.claimId)
        put("source_type", payload.sourceType)
        put("source_record_id", payload.sourceRecordId)
        put("claimed_at_epoch_ms", payload.claimedAtEpochMs)
    }

    fun shadowResonanceToPostgrestRow(
        userId: String,
        payload: CloudShadowResonancePayload
    ): JsonObject = buildJsonObject {
        put("id", payload.cloudUuid)
        put("user_id", userId)
        put("resonance_id", payload.resonanceId)
        put("archetype_id", payload.archetypeId)
        put("resonance_level", payload.resonanceLevel)
        put("resonance_shards", payload.resonanceShards)
        put("last_resonance_epoch_ms", payload.lastResonanceEpochMs)
    }

    fun shadowBondProfileToPostgrestRow(
        userId: String,
        payload: CloudShadowBondProfilePayload
    ): JsonObject = buildJsonObject {
        put("id", payload.cloudUuid)
        put("user_id", userId)
        put("shadow_id", payload.shadowId)
        put("bond_level", payload.bondLevel)
        put("bond_points", payload.bondPoints)
        put("loyalty_rating", payload.loyaltyRating)
        put("trust_rating", payload.trustRating)
        put("relationship_stage", payload.relationshipStageName)
        put("personality", payload.personalityCharacteristicName)
        put("total_activities_count", payload.totalActivitiesCount)
        put("last_bond_activity_epoch_ms", payload.lastBondActivityEpochMs)
        put("updated_at_epoch_ms", payload.updatedAtEpochMs)
    }

    fun shadowBondEventToPostgrestRow(
        userId: String,
        payload: CloudShadowBondEventPayload
    ): JsonObject = buildJsonObject {
        put("id", payload.cloudUuid)
        put("user_id", userId)
        put("event_id", payload.eventId)
        put("shadow_id", payload.shadowId)
        put("activity_type", payload.activityTypeName)
        put("source_event_id", payload.sourceEventId)
        put("bond_points_awarded", payload.bondPointsAwarded)
        put("loyalty_delta", payload.loyaltyDelta)
        put("trust_delta", payload.trustDelta)
        put("description", payload.description)
        put("timestamp_epoch_ms", payload.timestampEpochMs)
        put("idempotency_key", payload.idempotencyKey)
    }

    fun shadowBondMilestoneToPostgrestRow(
        userId: String,
        payload: CloudShadowBondMilestonePayload
    ): JsonObject = buildJsonObject {
        put("id", payload.cloudUuid)
        put("user_id", userId)
        put("milestone_id", payload.milestoneId)
        put("shadow_id", payload.shadowId)
        put("milestone_type", payload.milestoneTypeName)
        put("title", payload.title)
        put("description", payload.description)
        put("source_event_id", payload.sourceEventId)
        put("timestamp_epoch_ms", payload.timestampEpochMs)
        put("verification_status", payload.verificationStatus)
    }

    fun shadowSquadToPostgrestRow(
        userId: String,
        payload: CloudShadowSquadPayload
    ): JsonObject = buildJsonObject {
        put("id", payload.cloudUuid)
        put("user_id", userId)
        put("squad_id", payload.squadId)
        put("name", payload.name)
        put("category", payload.categoryName)
        put("formation_type", payload.formationTypeName)
        payload.leaderShadowId?.let { put("leader_shadow_id", it) }
        put("max_members", payload.maxMembers)
        put("is_active", payload.isActive)
        put("notes", payload.notes)
        put("created_at_epoch_ms", payload.createdAtEpochMs)
        put("updated_at_epoch_ms", payload.updatedAtEpochMs)
    }

    fun shadowSquadMemberToPostgrestRow(
        userId: String,
        payload: CloudShadowSquadMemberPayload
    ): JsonObject = buildJsonObject {
        put("id", payload.cloudUuid)
        put("user_id", userId)
        put("squad_id", payload.squadId)
        put("shadow_id", payload.shadowId)
        put("position_index", payload.positionIndex)
        put("is_leader", payload.isLeader)
        put("assigned_at_epoch_ms", payload.assignedAtEpochMs)
    }

    fun shadowMissionAssignmentToPostgrestRow(
        userId: String,
        payload: CloudShadowMissionAssignmentPayload
    ): JsonObject = buildJsonObject {
        put("id", payload.cloudUuid)
        put("user_id", userId)
        put("assignment_id", payload.assignmentId)
        put("mission_id", payload.missionId)
        put("mission_title", payload.missionTitle)
        put("mission_type", payload.missionTypeName)
        payload.squadId?.let { put("squad_id", it) }
        put("assigned_shadow_ids_json", payload.assignedShadowIdsJson)
        put("recommended_strategy", payload.recommendedStrategy)
        put("suitability_score", payload.suitabilityScore)
        put("status", payload.statusName)
        put("assigned_at_epoch_ms", payload.assignedAtEpochMs)
        payload.completedAtEpochMs?.let { put("completed_at_epoch_ms", it) }
        put("success_rate_projected", payload.successRateProjected)
        payload.resultOutcome?.let { put("result_outcome", it) }
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
