package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.MonarchRank
import com.example.domain.model.Shadow
import com.example.domain.model.ShadowClass
import com.example.domain.model.ShadowDeploymentCategory
import com.example.domain.model.ShadowDeploymentRecord
import com.example.domain.model.ShadowDeploymentStatus
import com.example.domain.model.ShadowEvolutionStage
import com.example.domain.model.ShadowExtractionEvent
import com.example.domain.model.ShadowExtractionOutcome
import com.example.domain.model.ShadowExtractionSourceType
import com.example.domain.model.ShadowFormation
import com.example.domain.model.ShadowFormationType
import com.example.domain.model.ShadowMonarchProfile
import com.example.domain.model.ShadowQuestProgress
import com.example.domain.model.ShadowRank
import com.example.domain.model.ShadowRarity
import com.example.domain.model.ShadowResourceTransaction
import com.example.domain.model.ShadowResourceTransactionType
import com.example.domain.model.ShadowResourceType
import com.example.domain.model.ShadowSkillUnlock
import com.example.domain.model.ShadowStats

// ============================================================================
// MILESTONE 11 — DATABASE ENTITIES
// ============================================================================

// 1. ShadowMonarchProfileEntity — Singleton (id = 1)
@Entity(tableName = "shadow_monarch_profile")
data class ShadowMonarchProfileEntity(
    @PrimaryKey val id: Int = 1,
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
    val unlockedMonarchSkillIdsJson: String, // JSON array of skillId strings
    val updatedAtEpochMs: Long
)

// 2. ShadowEntity — each recruited shadow
@Entity(
    tableName = "shadows",
    indices = [
        Index(value = ["shadowId"], unique = true),
        Index(value = ["shadowClassName"]),
        Index(value = ["currentRankName"]),
        Index(value = ["rarityName"]),
        Index(value = ["deploymentStatusName"]),
        Index(value = ["isRetired"])
    ]
)
data class ShadowEntity(
    @PrimaryKey val shadowId: String,
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
    val activeSkillIdsJson: String, // JSON array
    val recruitmentSource: String,
    val extractionEventId: String,
    val extractedAtEpochMs: Long,
    val evolutionHistoryJson: String, // JSON array of evolution stage transitions
    val deploymentStatusName: String,
    val equippedFormationId: String?,
    val isRetired: Boolean,
    val notes: String
)

// 3. ShadowExtractionEntity — extraction event record
@Entity(
    tableName = "shadow_extractions",
    indices = [
        Index(value = ["extractionId"], unique = true),
        Index(value = ["sourceTypeName", "sourceEventId"], unique = true), // composite unique constraint: prevent double-extraction same event
        Index(value = ["timestampEpochMs"])
    ]
)
data class ShadowExtractionEntity(
    @PrimaryKey val extractionId: String,
    val sourceTypeName: String,
    val sourceEventId: String,
    val outcomeName: String,
    val extractedShadowId: String?,
    val shadowName: String?,
    val shadowClassName: String?,
    val shadowRarityName: String?,
    val determinismSeed: Long,
    val evidenceSummary: String,
    val timestampEpochMs: Long,
    val isConfirmed: Boolean,
    val monarchXpAwarded: Int
)

// 4. ShadowSkillUnlockEntity — per-shadow skill unlocks
@Entity(
    tableName = "shadow_skill_unlocks",
    indices = [
        Index(value = ["shadowId", "skillId"], unique = true),
        Index(value = ["shadowId"])
    ]
)
data class ShadowSkillUnlockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val shadowId: String,
    val skillId: String,
    val unlockedAtEpochMs: Long,
    val evidenceNote: String
)

// 5. ShadowDeploymentEntity — deployment records
@Entity(
    tableName = "shadow_deployments",
    indices = [
        Index(value = ["deploymentId"], unique = true),
        Index(value = ["shadowId"]),
        Index(value = ["categoryName"]),
        Index(value = ["statusName"])
    ]
)
data class ShadowDeploymentEntity(
    @PrimaryKey val deploymentId: String,
    val shadowId: String,
    val shadowName: String,
    val categoryName: String,
    val role: String,
    val assignedAtEpochMs: Long,
    val recalledAtEpochMs: Long?,
    val statusName: String,
    val notes: String
)

// 6. ShadowFormationEntity — formation configurations
@Entity(
    tableName = "shadow_formations",
    indices = [
        Index(value = ["formationId"], unique = true),
        Index(value = ["isActive"])
    ]
)
data class ShadowFormationEntity(
    @PrimaryKey val formationId: String,
    val name: String,
    val formationTypeName: String,
    val description: String,
    val maxSlots: Int,
    val memberShadowIdsJson: String, // JSON array of shadowId strings
    val isActive: Boolean,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)

// 7. MonarchSkillUnlockEntity — which Monarch skills are unlocked
@Entity(
    tableName = "monarch_skill_unlocks",
    indices = [Index(value = ["skillId"], unique = true)]
)
data class MonarchSkillUnlockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val skillId: String,
    val unlockedAtEpochMs: Long,
    val evidenceNote: String
)

// 8. ShadowQuestProgressEntity — quest progress tracking
@Entity(
    tableName = "shadow_quest_progress",
    indices = [Index(value = ["questId"], unique = true)]
)
data class ShadowQuestProgressEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val questId: String,
    val currentValue: Int,
    val isCompleted: Boolean,
    val rewardClaimedAtEpochMs: Long?,
    val completedAtEpochMs: Long?
)

// 9. ShadowResourceTransactionEntity — resource ledger
@Entity(
    tableName = "shadow_resource_transactions",
    indices = [
        Index(value = ["transactionId"], unique = true),
        Index(value = ["sourceEventId", "resourceTypeName", "transactionTypeName"], unique = true), // enforce single award per source event and transaction type
        Index(value = ["resourceTypeName"]),
        Index(value = ["timestampEpochMs"]),
        Index(value = ["sourceEventId"])
    ]
)
data class ShadowResourceTransactionEntity(
    @PrimaryKey val transactionId: String,
    val resourceTypeName: String,
    val amount: Int,
    val transactionTypeName: String,
    val sourceEventId: String,
    val sourceDescription: String,
    val balanceAfter: Int,
    val timestampEpochMs: Long
)

// ============================================================================
// DOMAIN MAPPERS
// ============================================================================

fun ShadowMonarchProfileEntity.toDomain(): ShadowMonarchProfile {
    val skillIds = try {
        kotlinx.serialization.json.Json.decodeFromString<List<String>>(unlockedMonarchSkillIdsJson)
    } catch (e: Exception) { emptyList() }

    return ShadowMonarchProfile(
        id = id,
        monarchLevel = monarchLevel,
        monarchXp = monarchXp,
        monarchRank = runCatching { MonarchRank.valueOf(monarchRankName) }.getOrDefault(MonarchRank.AWAKENING),
        monarchAuthority = monarchAuthority,
        armyCapacity = armyCapacity,
        activeShadowCount = activeShadowCount,
        totalShadowsRecruited = totalShadowsRecruited,
        highestShadowRank = runCatching { ShadowRank.valueOf(highestShadowRankName) }.getOrDefault(ShadowRank.E_RANK),
        totalEvolutions = totalEvolutions,
        shadowArmyPowerRating = shadowArmyPowerRating,
        unlockedMonarchSkillIds = skillIds,
        updatedAtEpochMs = updatedAtEpochMs
    )
}

fun ShadowMonarchProfile.toEntity(): ShadowMonarchProfileEntity {
    val skillJson = kotlinx.serialization.json.Json.encodeToString(unlockedMonarchSkillIds)
    return ShadowMonarchProfileEntity(
        id = id,
        monarchLevel = monarchLevel,
        monarchXp = monarchXp,
        monarchRankName = monarchRank.name,
        monarchAuthority = monarchAuthority,
        armyCapacity = armyCapacity,
        activeShadowCount = activeShadowCount,
        totalShadowsRecruited = totalShadowsRecruited,
        highestShadowRankName = highestShadowRank.name,
        totalEvolutions = totalEvolutions,
        shadowArmyPowerRating = shadowArmyPowerRating,
        unlockedMonarchSkillIdsJson = skillJson,
        updatedAtEpochMs = updatedAtEpochMs
    )
}

fun ShadowEntity.toDomain(): Shadow {
    val skillIds = try {
        kotlinx.serialization.json.Json.decodeFromString<List<String>>(activeSkillIdsJson)
    } catch (e: Exception) { emptyList() }
    return Shadow(
        shadowId = shadowId,
        name = name,
        shadowClass = runCatching { ShadowClass.valueOf(shadowClassName) }.getOrDefault(ShadowClass.SUPPORT),
        rarity = runCatching { ShadowRarity.valueOf(rarityName) }.getOrDefault(ShadowRarity.COMMON),
        currentRank = runCatching { ShadowRank.valueOf(currentRankName) }.getOrDefault(ShadowRank.E_RANK),
        level = level,
        shadowXp = shadowXp,
        stats = ShadowStats(statPower, statEndurance, statFocus, statDiscipline, statWisdom, statAdaptability),
        evolutionStage = runCatching { ShadowEvolutionStage.valueOf(evolutionStageName) }.getOrDefault(ShadowEvolutionStage.NEWLY_EXTRACTED),
        passiveAbility = passiveAbility,
        activeSkillIds = skillIds,
        recruitmentSource = recruitmentSource,
        extractionEventId = extractionEventId,
        extractedAtEpochMs = extractedAtEpochMs,
        evolutionHistoryJson = evolutionHistoryJson,
        deploymentStatus = runCatching { ShadowDeploymentStatus.valueOf(deploymentStatusName) }.getOrDefault(ShadowDeploymentStatus.IDLE),
        equippedFormationId = equippedFormationId,
        isRetired = isRetired,
        notes = notes
    )
}

fun Shadow.toEntity(): ShadowEntity {
    val skillJson = kotlinx.serialization.json.Json.encodeToString(activeSkillIds)
    return ShadowEntity(
        shadowId = shadowId,
        name = name,
        shadowClassName = shadowClass.name,
        rarityName = rarity.name,
        currentRankName = currentRank.name,
        level = level,
        shadowXp = shadowXp,
        statPower = stats.power,
        statEndurance = stats.endurance,
        statFocus = stats.focus,
        statDiscipline = stats.discipline,
        statWisdom = stats.wisdom,
        statAdaptability = stats.adaptability,
        evolutionStageName = evolutionStage.name,
        passiveAbility = passiveAbility,
        activeSkillIdsJson = skillJson,
        recruitmentSource = recruitmentSource,
        extractionEventId = extractionEventId,
        extractedAtEpochMs = extractedAtEpochMs,
        evolutionHistoryJson = evolutionHistoryJson,
        deploymentStatusName = deploymentStatus.name,
        equippedFormationId = equippedFormationId,
        isRetired = isRetired,
        notes = notes
    )
}

fun ShadowExtractionEntity.toDomain() = ShadowExtractionEvent(
    extractionId = extractionId,
    sourceType = runCatching { ShadowExtractionSourceType.valueOf(sourceTypeName) }.getOrDefault(ShadowExtractionSourceType.DUNGEON_COMPLETION),
    sourceEventId = sourceEventId,
    outcome = runCatching { ShadowExtractionOutcome.valueOf(outcomeName) }.getOrDefault(ShadowExtractionOutcome.UNAVAILABLE),
    extractedShadowId = extractedShadowId,
    shadowName = shadowName,
    shadowClass = shadowClassName?.let { runCatching { ShadowClass.valueOf(it) }.getOrNull() },
    shadowRarity = shadowRarityName?.let { runCatching { ShadowRarity.valueOf(it) }.getOrNull() },
    determinismSeed = determinismSeed,
    evidenceSummary = evidenceSummary,
    timestampEpochMs = timestampEpochMs,
    isConfirmed = isConfirmed,
    monarchXpAwarded = monarchXpAwarded
)

fun ShadowExtractionEvent.toEntity() = ShadowExtractionEntity(
    extractionId = extractionId,
    sourceTypeName = sourceType.name,
    sourceEventId = sourceEventId,
    outcomeName = outcome.name,
    extractedShadowId = extractedShadowId,
    shadowName = shadowName,
    shadowClassName = shadowClass?.name,
    shadowRarityName = shadowRarity?.name,
    determinismSeed = determinismSeed,
    evidenceSummary = evidenceSummary,
    timestampEpochMs = timestampEpochMs,
    isConfirmed = isConfirmed,
    monarchXpAwarded = monarchXpAwarded
)

fun ShadowDeploymentEntity.toDomain() = ShadowDeploymentRecord(
    deploymentId = deploymentId,
    shadowId = shadowId,
    shadowName = shadowName,
    category = runCatching { ShadowDeploymentCategory.valueOf(categoryName) }.getOrDefault(ShadowDeploymentCategory.DISCIPLINE),
    role = role,
    assignedAtEpochMs = assignedAtEpochMs,
    recalledAtEpochMs = recalledAtEpochMs,
    status = runCatching { ShadowDeploymentStatus.valueOf(statusName) }.getOrDefault(ShadowDeploymentStatus.DEPLOYED),
    notes = notes
)

fun ShadowDeploymentRecord.toEntity() = ShadowDeploymentEntity(
    deploymentId = deploymentId,
    shadowId = shadowId,
    shadowName = shadowName,
    categoryName = category.name,
    role = role,
    assignedAtEpochMs = assignedAtEpochMs,
    recalledAtEpochMs = recalledAtEpochMs,
    statusName = status.name,
    notes = notes
)

fun ShadowFormationEntity.toDomain(): ShadowFormation {
    val memberIds = try {
        kotlinx.serialization.json.Json.decodeFromString<List<String>>(memberShadowIdsJson)
    } catch (e: Exception) { emptyList() }
    return ShadowFormation(
        formationId = formationId,
        name = name,
        formationType = runCatching { ShadowFormationType.valueOf(formationTypeName) }.getOrDefault(ShadowFormationType.BALANCED_FORMATION),
        description = description,
        maxSlots = maxSlots,
        memberShadowIds = memberIds,
        isActive = isActive,
        createdAtEpochMs = createdAtEpochMs,
        updatedAtEpochMs = updatedAtEpochMs
    )
}

fun ShadowFormation.toEntity(): ShadowFormationEntity {
    val memberJson = kotlinx.serialization.json.Json.encodeToString(memberShadowIds)
    return ShadowFormationEntity(
        formationId = formationId,
        name = name,
        formationTypeName = formationType.name,
        description = description,
        maxSlots = maxSlots,
        memberShadowIdsJson = memberJson,
        isActive = isActive,
        createdAtEpochMs = createdAtEpochMs,
        updatedAtEpochMs = updatedAtEpochMs
    )
}

fun ShadowSkillUnlockEntity.toDomain() = ShadowSkillUnlock(
    id = id,
    shadowId = shadowId,
    skillId = skillId,
    unlockedAtEpochMs = unlockedAtEpochMs,
    evidenceNote = evidenceNote
)

fun ShadowQuestProgressEntity.toDomain() = ShadowQuestProgress(
    id = id,
    questId = questId,
    currentValue = currentValue,
    isCompleted = isCompleted,
    rewardClaimedAtEpochMs = rewardClaimedAtEpochMs,
    completedAtEpochMs = completedAtEpochMs
)

fun ShadowResourceTransactionEntity.toDomain() = ShadowResourceTransaction(
    transactionId = transactionId,
    resourceType = runCatching { ShadowResourceType.valueOf(resourceTypeName) }.getOrDefault(ShadowResourceType.SHADOW_ESSENCE),
    amount = amount,
    transactionType = runCatching { ShadowResourceTransactionType.valueOf(transactionTypeName) }.getOrDefault(ShadowResourceTransactionType.EARNED),
    sourceEventId = sourceEventId,
    sourceDescription = sourceDescription,
    balanceAfter = balanceAfter,
    timestampEpochMs = timestampEpochMs
)

