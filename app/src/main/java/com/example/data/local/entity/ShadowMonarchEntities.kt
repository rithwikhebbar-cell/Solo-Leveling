package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.DEFAULT_SHADOW_ACCOUNT_OWNER_ID
import com.example.domain.model.MonarchRank
import com.example.domain.model.MonarchSkillUnlock
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
// MILESTONE 11 + MILESTONE 12 PHASE 6.1 — ACCOUNT-ISOLATED SHADOW ENTITIES
// ============================================================================

// 1. ShadowMonarchProfileEntity — Multi-account (PK = accountOwnerId)
@Entity(tableName = "shadow_monarch_profile")
data class ShadowMonarchProfileEntity(
    @PrimaryKey val accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
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

// 2. ShadowEntity — each recruited shadow, strictly owned by accountOwnerId
@Entity(
    tableName = "shadows",
    foreignKeys = [
        ForeignKey(
            entity = ShadowMonarchProfileEntity::class,
            parentColumns = ["accountOwnerId"],
            childColumns = ["accountOwnerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["shadowId"], unique = true),
        Index(value = ["accountOwnerId"]),
        Index(value = ["accountOwnerId", "isRetired"]),
        Index(value = ["accountOwnerId", "shadowClassName"]),
        Index(value = ["accountOwnerId", "currentRankName"]),
        Index(value = ["rarityName"]),
        Index(value = ["deploymentStatusName"])
    ]
)
data class ShadowEntity(
    @PrimaryKey val shadowId: String,
    val accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
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

// 3. ShadowExtractionEntity — extraction event record, unique per (accountOwnerId, sourceTypeName, sourceEventId)
@Entity(
    tableName = "shadow_extractions",
    foreignKeys = [
        ForeignKey(
            entity = ShadowMonarchProfileEntity::class,
            parentColumns = ["accountOwnerId"],
            childColumns = ["accountOwnerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["extractionId"], unique = true),
        Index(value = ["accountOwnerId", "sourceTypeName", "sourceEventId"], unique = true),
        Index(value = ["accountOwnerId", "timestampEpochMs"])
    ]
)
data class ShadowExtractionEntity(
    @PrimaryKey val extractionId: String,
    val accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
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

// 4. ShadowSkillUnlockEntity — per-shadow skill unlocks with shadow & account FKs
@Entity(
    tableName = "shadow_skill_unlocks",
    foreignKeys = [
        ForeignKey(
            entity = ShadowMonarchProfileEntity::class,
            parentColumns = ["accountOwnerId"],
            childColumns = ["accountOwnerId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ShadowEntity::class,
            parentColumns = ["shadowId"],
            childColumns = ["shadowId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["accountOwnerId", "shadowId", "skillId"], unique = true),
        Index(value = ["shadowId"]),
        Index(value = ["accountOwnerId"])
    ]
)
data class ShadowSkillUnlockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
    val shadowId: String,
    val skillId: String,
    val unlockedAtEpochMs: Long,
    val evidenceNote: String
)

// 5. ShadowDeploymentEntity — deployment records with shadow & account FKs
@Entity(
    tableName = "shadow_deployments",
    foreignKeys = [
        ForeignKey(
            entity = ShadowMonarchProfileEntity::class,
            parentColumns = ["accountOwnerId"],
            childColumns = ["accountOwnerId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ShadowEntity::class,
            parentColumns = ["shadowId"],
            childColumns = ["shadowId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["deploymentId"], unique = true),
        Index(value = ["accountOwnerId", "statusName"]),
        Index(value = ["shadowId"]),
        Index(value = ["categoryName"])
    ]
)
data class ShadowDeploymentEntity(
    @PrimaryKey val deploymentId: String,
    val accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
    val shadowId: String,
    val shadowName: String,
    val categoryName: String,
    val role: String,
    val assignedAtEpochMs: Long,
    val recalledAtEpochMs: Long?,
    val statusName: String,
    val notes: String
)

// 6. ShadowFormationEntity — formation configurations per account
@Entity(
    tableName = "shadow_formations",
    foreignKeys = [
        ForeignKey(
            entity = ShadowMonarchProfileEntity::class,
            parentColumns = ["accountOwnerId"],
            childColumns = ["accountOwnerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["formationId"], unique = true),
        Index(value = ["accountOwnerId", "isActive"])
    ]
)
data class ShadowFormationEntity(
    @PrimaryKey val formationId: String,
    val accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
    val name: String,
    val formationTypeName: String,
    val description: String,
    val maxSlots: Int,
    val memberShadowIdsJson: String, // JSON array of shadowId strings
    val isActive: Boolean,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)

// 7. MonarchSkillUnlockEntity — per-account Monarch skill unlocks
@Entity(
    tableName = "monarch_skill_unlocks",
    foreignKeys = [
        ForeignKey(
            entity = ShadowMonarchProfileEntity::class,
            parentColumns = ["accountOwnerId"],
            childColumns = ["accountOwnerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["accountOwnerId", "skillId"], unique = true),
        Index(value = ["accountOwnerId"])
    ]
)
data class MonarchSkillUnlockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
    val skillId: String,
    val unlockedAtEpochMs: Long,
    val evidenceNote: String
)

// 8. ShadowQuestProgressEntity — per-account quest progress tracking
@Entity(
    tableName = "shadow_quest_progress",
    foreignKeys = [
        ForeignKey(
            entity = ShadowMonarchProfileEntity::class,
            parentColumns = ["accountOwnerId"],
            childColumns = ["accountOwnerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["accountOwnerId", "questId"], unique = true),
        Index(value = ["accountOwnerId"])
    ]
)
data class ShadowQuestProgressEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
    val questId: String,
    val currentValue: Int,
    val isCompleted: Boolean,
    val rewardClaimedAtEpochMs: Long?,
    val completedAtEpochMs: Long?
)

// 9. ShadowResourceTransactionEntity — per-account resource ledger
@Entity(
    tableName = "shadow_resource_transactions",
    foreignKeys = [
        ForeignKey(
            entity = ShadowMonarchProfileEntity::class,
            parentColumns = ["accountOwnerId"],
            childColumns = ["accountOwnerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["transactionId"], unique = true),
        Index(
            value = ["accountOwnerId", "sourceEventId", "resourceTypeName", "transactionTypeName"],
            unique = true
        ),
        Index(value = ["accountOwnerId", "resourceTypeName", "timestampEpochMs"]),
        Index(value = ["sourceEventId"])
    ]
)
data class ShadowResourceTransactionEntity(
    @PrimaryKey val transactionId: String,
    val accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
    val resourceTypeName: String,
    val amount: Int,
    val transactionTypeName: String,
    val sourceEventId: String,
    val sourceDescription: String,
    val balanceAfter: Int,
    val timestampEpochMs: Long
)

// 10. ShadowXpTraceEntity — dedicated idempotency ledger for Monarch XP and Shadow XP awards
@Entity(
    tableName = "shadow_xp_traces",
    foreignKeys = [
        ForeignKey(
            entity = ShadowMonarchProfileEntity::class,
            parentColumns = ["accountOwnerId"],
            childColumns = ["accountOwnerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["traceId"], unique = true),
        Index(value = ["accountOwnerId", "traceType", "sourceTag"], unique = true),
        Index(value = ["accountOwnerId"]),
        Index(value = ["timestampEpochMs"])
    ]
)
data class ShadowXpTraceEntity(
    @PrimaryKey val traceId: String,
    val accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
    val traceType: String, // "MONARCH_XP" or "SHADOW_XP"
    val sourceTag: String,
    val targetEntityId: String = "",
    val xpAmount: Int,
    val description: String,
    val timestampEpochMs: Long
)

// ============================================================================
// DOMAIN MAPPERS
// ============================================================================

fun ShadowMonarchProfileEntity.toDomain(): ShadowMonarchProfile {
    val skillIds = try {
        kotlinx.serialization.json.Json.decodeFromString<List<String>>(unlockedMonarchSkillIdsJson)
    } catch (_: Exception) { emptyList() }

    return ShadowMonarchProfile(
        id = 1,
        accountOwnerId = accountOwnerId,
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
        accountOwnerId = accountOwnerId,
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
    } catch (_: Exception) { emptyList() }
    return Shadow(
        shadowId = shadowId,
        accountOwnerId = accountOwnerId,
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
        accountOwnerId = accountOwnerId,
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
    accountOwnerId = accountOwnerId,
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
    accountOwnerId = accountOwnerId,
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
    accountOwnerId = accountOwnerId,
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
    accountOwnerId = accountOwnerId,
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
    } catch (_: Exception) { emptyList() }
    return ShadowFormation(
        formationId = formationId,
        accountOwnerId = accountOwnerId,
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
        accountOwnerId = accountOwnerId,
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
    accountOwnerId = accountOwnerId,
    shadowId = shadowId,
    skillId = skillId,
    unlockedAtEpochMs = unlockedAtEpochMs,
    evidenceNote = evidenceNote
)

fun MonarchSkillUnlockEntity.toDomain() = MonarchSkillUnlock(
    id = id,
    accountOwnerId = accountOwnerId,
    skillId = skillId,
    unlockedAtEpochMs = unlockedAtEpochMs,
    evidenceNote = evidenceNote
)

fun ShadowQuestProgressEntity.toDomain() = ShadowQuestProgress(
    id = id,
    accountOwnerId = accountOwnerId,
    questId = questId,
    currentValue = currentValue,
    isCompleted = isCompleted,
    rewardClaimedAtEpochMs = rewardClaimedAtEpochMs,
    completedAtEpochMs = completedAtEpochMs
)

fun ShadowResourceTransactionEntity.toDomain() = ShadowResourceTransaction(
    transactionId = transactionId,
    accountOwnerId = accountOwnerId,
    resourceType = runCatching { ShadowResourceType.valueOf(resourceTypeName) }.getOrDefault(ShadowResourceType.SHADOW_ESSENCE),
    amount = amount,
    transactionType = runCatching { ShadowResourceTransactionType.valueOf(transactionTypeName) }.getOrDefault(ShadowResourceTransactionType.EARNED),
    sourceEventId = sourceEventId,
    sourceDescription = sourceDescription,
    balanceAfter = balanceAfter,
    timestampEpochMs = timestampEpochMs
)


@Entity(
    tableName = "shadow_evidence_claims",
    foreignKeys = [
        ForeignKey(
            entity = ShadowMonarchProfileEntity::class,
            parentColumns = ["accountOwnerId"],
            childColumns = ["accountOwnerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("accountOwnerId"),
        Index(value = ["accountOwnerId", "sourceType", "sourceRecordId"], unique = true)
    ]
)
data class ShadowEvidenceClaimEntity(
    @PrimaryKey
    val claimId: String,
    val accountOwnerId: String,
    val sourceType: String,
    val sourceRecordId: String,
    val claimedAtEpochMs: Long
)

@Entity(
    tableName = "shadow_resonance_records",
    foreignKeys = [
        ForeignKey(
            entity = ShadowMonarchProfileEntity::class,
            parentColumns = ["accountOwnerId"],
            childColumns = ["accountOwnerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("accountOwnerId"),
        Index(value = ["accountOwnerId", "archetypeId"], unique = true)
    ]
)
data class ShadowResonanceEntity(
    @PrimaryKey
    val resonanceId: String,
    val accountOwnerId: String,
    val archetypeId: String,
    val resonanceLevel: Int,
    val resonanceShards: Int,
    val lastResonanceEpochMs: Long
)
