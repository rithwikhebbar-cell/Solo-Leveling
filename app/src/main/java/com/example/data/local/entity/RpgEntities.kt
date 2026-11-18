package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.AchievementCategory
import com.example.domain.model.AchievementDefinition
import com.example.domain.model.AchievementUnlockRecord
import com.example.domain.model.AiDungeonProposal
import com.example.domain.model.AttributeType
import com.example.domain.model.BossBattleDefinition
import com.example.domain.model.BossBattleInstance
import com.example.domain.model.BossPhase
import com.example.domain.model.BossStatus
import com.example.domain.model.DungeonCategory
import com.example.domain.model.DungeonCompletionRecord
import com.example.domain.model.DungeonCreationSource
import com.example.domain.model.DungeonDefinition
import com.example.domain.model.DungeonDifficulty
import com.example.domain.model.DungeonEvidenceType
import com.example.domain.model.DungeonInstance
import com.example.domain.model.DungeonObjective
import com.example.domain.model.DungeonStatus
import com.example.domain.model.HunterRank
import com.example.domain.model.InventoryCategory
import com.example.domain.model.InventoryItemDefinition
import com.example.domain.model.PlayerInventoryRecord
import com.example.domain.model.PlayerTitleDefinition
import com.example.domain.model.ProposalStatus
import com.example.domain.model.RpgRarity
import com.example.domain.model.RpgRewardAuditRecord
import com.example.domain.model.ShadowCollectionRecord
import com.example.domain.model.SkillDefinition
import com.example.domain.model.SkillEffectSpec
import com.example.domain.model.SkillSpecialization
import com.example.domain.model.SkillTier
import com.example.domain.model.SkillTreeDependency
import com.example.domain.model.SkillUnlockRecord

// 1. DungeonDefinitionEntity
@Entity(
    tableName = "dungeon_definitions",
    indices = [Index(value = ["code"], unique = true)]
)
data class DungeonDefinitionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val title: String,
    val description: String,
    val categoryName: String,
    val difficultyName: String,
    val requiredRankName: String,
    val requiredLevel: Int,
    val requiredAttributeCode: String?,
    val requiredAttributeValue: Int,
    val timeLimitHours: Int,
    val baseXpReward: Int,
    val primaryAttrName: String,
    val primaryAttrXp: Int,
    val secondaryAttrName: String,
    val secondaryAttrXp: Int,
    val rewardTitleId: String?,
    val rewardItemId: String?,
    val rewardShadowId: String?,
    val isRepeatable: Boolean,
    val creationSourceName: String,
    val isActiveCatalog: Boolean
)

// 2. DungeonObjectiveEntity
@Entity(
    tableName = "dungeon_objectives",
    foreignKeys = [
        ForeignKey(
            entity = DungeonDefinitionEntity::class,
            parentColumns = ["id"],
            childColumns = ["dungeonId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["dungeonId"])]
)
data class DungeonObjectiveEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dungeonId: Long,
    val title: String,
    val evidenceTypeName: String,
    val targetValue: Int,
    val requiresVerifiedOnly: Boolean,
    val orderIndex: Int
)

// 3. DungeonInstanceEntity
@Entity(
    tableName = "dungeon_instances",
    foreignKeys = [
        ForeignKey(
            entity = DungeonDefinitionEntity::class,
            parentColumns = ["id"],
            childColumns = ["dungeonId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["dungeonId"]),
        Index(value = ["statusName"]),
        Index(value = ["cadenceKey"])
    ]
)
data class DungeonInstanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dungeonId: Long,
    val statusName: String,
    val creationSourceName: String,
    val cadenceKey: String,
    val startedAtEpochMs: Long,
    val pausedAtEpochMs: Long?,
    val accumulatedPausedMs: Long,
    val deadlineEpochMs: Long,
    val completedAtEpochMs: Long?,
    val notes: String
)

// 4. DungeonCompletionRecordEntity
@Entity(
    tableName = "dungeon_completions",
    foreignKeys = [
        ForeignKey(
            entity = DungeonDefinitionEntity::class,
            parentColumns = ["id"],
            childColumns = ["dungeonId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["rewardKey"], unique = true),
        Index(value = ["dungeonId"]),
        Index(value = ["completedAtEpochMs"])
    ]
)
data class DungeonCompletionRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rewardKey: String,
    val instanceId: Long,
    val dungeonId: Long,
    val dungeonTitle: String,
    val difficultyName: String,
    val xpAwarded: Int,
    val primaryAttrName: String,
    val primaryAttrXp: Int,
    val secondaryAttrName: String,
    val secondaryAttrXp: Int,
    val evidenceSummary: String,
    val verificationProvenance: String,
    val completedAtEpochMs: Long
)

// 5. BossDefinitionEntity
@Entity(
    tableName = "boss_definitions",
    indices = [Index(value = ["code"], unique = true)]
)
data class BossDefinitionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val categoryName: String,
    val difficultyName: String,
    val requiredRankName: String,
    val requiredLevel: Int,
    val xpReward: Int,
    val primaryAttrName: String,
    val primaryAttrXp: Int,
    val secondaryAttrName: String,
    val secondaryAttrXp: Int,
    val rewardTitleId: String?,
    val rewardItemId: String?,
    val rewardShadowId: String?,
    val totalEstimatedDays: Int
)

// 6. BossBattleInstanceEntity
@Entity(
    tableName = "boss_instances",
    foreignKeys = [
        ForeignKey(
            entity = BossDefinitionEntity::class,
            parentColumns = ["id"],
            childColumns = ["bossId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["bossId"]),
        Index(value = ["statusName"])
    ]
)
data class BossBattleInstanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bossId: Long,
    val statusName: String,
    val startedAtEpochMs: Long,
    val completedAtEpochMs: Long?,
    val abandonedAtEpochMs: Long?
)

// 7. BossBattlePhaseEntity
@Entity(
    tableName = "boss_phases",
    foreignKeys = [
        ForeignKey(
            entity = BossDefinitionEntity::class,
            parentColumns = ["id"],
            childColumns = ["bossId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["bossId"])]
)
data class BossBattlePhaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bossId: Long,
    val phaseNumber: Int,
    val title: String,
    val description: String,
    val evidenceTypeName: String,
    val targetValue: Int,
    val requiresVerifiedOnly: Boolean,
    val weight: Int
)

// 8. SkillDefinitionEntity
@Entity(tableName = "skill_definitions")
data class SkillDefinitionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val specializationName: String,
    val tierName: String,
    val description: String,
    val requiredRankName: String,
    val requiredLevel: Int,
    val requiredAttrName: String,
    val requiredAttributeValue: Int,
    val requiredVerifiedCount: Int,
    val requiredStreakDays: Int,
    val maxMasteryLevel: Int,
    val treeColumn: Int,
    val treeRow: Int
)

// 9. SkillUnlockRecordEntity
@Entity(
    tableName = "skill_unlock_records",
    foreignKeys = [
        ForeignKey(
            entity = SkillDefinitionEntity::class,
            parentColumns = ["id"],
            childColumns = ["skillId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["skillId"], unique = true)]
)
data class SkillUnlockRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val skillId: String,
    val masteryLevel: Int,
    val unlockedAtEpochMs: Long,
    val lastUpgradedAtEpochMs: Long,
    val evidenceNote: String
)

// 10. SkillEffectConfigEntity
@Entity(
    tableName = "skill_effect_configs",
    foreignKeys = [
        ForeignKey(
            entity = SkillDefinitionEntity::class,
            parentColumns = ["id"],
            childColumns = ["skillId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["skillId"])]
)
data class SkillEffectConfigEntity(
    @PrimaryKey val effectCode: String,
    val skillId: String,
    val title: String,
    val description: String,
    val featureTag: String
)

// 11. SkillTreeDependencyEntity
@Entity(
    tableName = "skill_tree_dependencies",
    foreignKeys = [
        ForeignKey(
            entity = SkillDefinitionEntity::class,
            parentColumns = ["id"],
            childColumns = ["skillId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SkillDefinitionEntity::class,
            parentColumns = ["id"],
            childColumns = ["prerequisiteSkillId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["skillId", "prerequisiteSkillId"], unique = true),
        Index(value = ["prerequisiteSkillId"])
    ]
)
data class SkillTreeDependencyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val skillId: String,
    val prerequisiteSkillId: String,
    val minPrerequisiteMastery: Int
)

// 12. AchievementDefinitionEntity
@Entity(tableName = "achievement_definitions")
data class AchievementDefinitionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val categoryName: String,
    val rarityName: String,
    val criterionDescription: String,
    val targetValue: Int,
    val xpBonus: Int,
    val rewardTitleId: String?,
    val rewardItemId: String?,
    val rewardShadowId: String?
)

// 13. AchievementUnlockRecordEntity
@Entity(
    tableName = "achievement_unlock_records",
    foreignKeys = [
        ForeignKey(
            entity = AchievementDefinitionEntity::class,
            parentColumns = ["id"],
            childColumns = ["achievementId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["achievementId"], unique = true)]
)
data class AchievementUnlockRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val achievementId: String,
    val evidenceReference: String,
    val unlockedAtEpochMs: Long
)

// 14. PlayerTitleEntity
@Entity(
    tableName = "player_titles",
    indices = [
        Index(value = ["isUnlocked"]),
        Index(value = ["isEquipped"])
    ]
)
data class PlayerTitleEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val rarityName: String,
    val unlockRequirement: String,
    val isUnlocked: Boolean,
    val isEquipped: Boolean,
    val unlockedAtEpochMs: Long?
)

// 15. InventoryItemEntity
@Entity(tableName = "inventory_items")
data class InventoryItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val categoryName: String,
    val rarityName: String,
    val description: String,
    val acquisitionSource: String,
    val canEquip: Boolean
)

// 16. PlayerInventoryRecordEntity
@Entity(
    tableName = "player_inventory_records",
    foreignKeys = [
        ForeignKey(
            entity = InventoryItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["itemId"], unique = true)]
)
data class PlayerInventoryRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: String,
    val quantity: Int,
    val isEquipped: Boolean,
    val sourceDescription: String,
    val acquiredAtEpochMs: Long
)

// 17. ShadowCollectionRecordEntity
@Entity(
    tableName = "shadow_collection_records",
    indices = [Index(value = ["collectibleId"], unique = true)]
)
data class ShadowCollectionRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val collectibleId: String,
    val rankTier: Int,
    val evidenceSource: String,
    val unlockedAtEpochMs: Long
)

// 18. RpgRewardAuditEntity
@Entity(
    tableName = "rpg_reward_audit",
    indices = [
        Index(value = ["rewardKey"], unique = true),
        Index(value = ["timestampEpochMs"]),
        Index(value = ["accountOwnerId", "sourceType", "timestampEpochMs"])
    ]
)
data class RpgRewardAuditEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rewardKey: String,
    val sourceType: String,
    val sourceId: String,
    val title: String,
    val xpAwarded: Int,
    val primaryAttrName: String,
    val primaryAttrXp: Int,
    val secondaryAttrName: String?,
    val secondaryAttrXp: Int,
    val verificationProvenance: String,
    val wasCapped: Boolean,
    val notes: String,
    val timestampEpochMs: Long,
    val accountOwnerId: String = "local_vault_player_1"
)

// 19. AiDungeonProposalEntity
@Entity(
    tableName = "ai_dungeon_proposals",
    indices = [
        Index(value = ["proposalUuid"], unique = true),
        Index(value = ["statusName"])
    ]
)
data class AiDungeonProposalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val proposalUuid: String,
    val title: String,
    val description: String,
    val categoryName: String,
    val difficultyName: String,
    val objectiveTitle: String,
    val targetEvidenceTypeName: String,
    val targetValue: Int,
    val timeLimitHours: Int,
    val requiresVerifiedOnly: Boolean,
    val reason: String,
    val isBossCampaign: Boolean,
    val statusName: String,
    val createdAtEpochMs: Long
)

// ============================================================================
// DOMAIN MAPPERS
// ============================================================================

fun DungeonDefinitionEntity.toDomain() = DungeonDefinition(
    id = id,
    code = code,
    title = title,
    description = description,
    category = runCatching { DungeonCategory.valueOf(categoryName) }.getOrDefault(DungeonCategory.MULTI_DOMAIN_CHALLENGE),
    difficulty = runCatching { DungeonDifficulty.valueOf(difficultyName) }.getOrDefault(DungeonDifficulty.E_RANK),
    requiredRank = runCatching { HunterRank.valueOf(requiredRankName) }.getOrDefault(HunterRank.E_RANK),
    requiredLevel = requiredLevel,
    requiredAttributeCode = requiredAttributeCode,
    requiredAttributeValue = requiredAttributeValue,
    timeLimitHours = timeLimitHours,
    baseXpReward = baseXpReward,
    primaryAttribute = runCatching { AttributeType.valueOf(primaryAttrName) }.getOrDefault(AttributeType.DIS),
    primaryAttrXp = primaryAttrXp,
    secondaryAttribute = runCatching { AttributeType.valueOf(secondaryAttrName) }.getOrDefault(AttributeType.VIT),
    secondaryAttrXp = secondaryAttrXp,
    rewardTitleId = rewardTitleId,
    rewardItemId = rewardItemId,
    rewardShadowId = rewardShadowId,
    isRepeatable = isRepeatable,
    creationSource = runCatching { DungeonCreationSource.valueOf(creationSourceName) }.getOrDefault(DungeonCreationSource.SYSTEM),
    isActiveCatalog = isActiveCatalog
)

fun DungeonDefinition.toEntity() = DungeonDefinitionEntity(
    id = id,
    code = code,
    title = title,
    description = description,
    categoryName = category.name,
    difficultyName = difficulty.name,
    requiredRankName = requiredRank.name,
    requiredLevel = requiredLevel,
    requiredAttributeCode = requiredAttributeCode,
    requiredAttributeValue = requiredAttributeValue,
    timeLimitHours = timeLimitHours,
    baseXpReward = baseXpReward,
    primaryAttrName = primaryAttribute.name,
    primaryAttrXp = primaryAttrXp,
    secondaryAttrName = secondaryAttribute.name,
    secondaryAttrXp = secondaryAttrXp,
    rewardTitleId = rewardTitleId,
    rewardItemId = rewardItemId,
    rewardShadowId = rewardShadowId,
    isRepeatable = isRepeatable,
    creationSourceName = creationSource.name,
    isActiveCatalog = isActiveCatalog
)

fun DungeonObjectiveEntity.toDomain() = DungeonObjective(
    id = id,
    dungeonId = dungeonId,
    title = title,
    evidenceType = runCatching { DungeonEvidenceType.valueOf(evidenceTypeName) }.getOrDefault(DungeonEvidenceType.PHYSICAL_JOG_MINUTES),
    targetValue = targetValue,
    requiresVerifiedOnly = requiresVerifiedOnly,
    orderIndex = orderIndex
)

fun DungeonObjective.toEntity(overrideDungeonId: Long = dungeonId) = DungeonObjectiveEntity(
    id = id,
    dungeonId = overrideDungeonId,
    title = title,
    evidenceTypeName = evidenceType.name,
    targetValue = targetValue,
    requiresVerifiedOnly = requiresVerifiedOnly,
    orderIndex = orderIndex
)

fun DungeonInstanceEntity.toDomain() = DungeonInstance(
    id = id,
    dungeonId = dungeonId,
    status = runCatching { DungeonStatus.valueOf(statusName) }.getOrDefault(DungeonStatus.AVAILABLE),
    creationSource = runCatching { DungeonCreationSource.valueOf(creationSourceName) }.getOrDefault(DungeonCreationSource.SYSTEM),
    cadenceKey = cadenceKey,
    startedAtEpochMs = startedAtEpochMs,
    pausedAtEpochMs = pausedAtEpochMs,
    accumulatedPausedMs = accumulatedPausedMs,
    deadlineEpochMs = deadlineEpochMs,
    completedAtEpochMs = completedAtEpochMs,
    notes = notes
)

fun DungeonInstance.toEntity() = DungeonInstanceEntity(
    id = id,
    dungeonId = dungeonId,
    statusName = status.name,
    creationSourceName = creationSource.name,
    cadenceKey = cadenceKey,
    startedAtEpochMs = startedAtEpochMs,
    pausedAtEpochMs = pausedAtEpochMs,
    accumulatedPausedMs = accumulatedPausedMs,
    deadlineEpochMs = deadlineEpochMs,
    completedAtEpochMs = completedAtEpochMs,
    notes = notes
)

fun DungeonCompletionRecordEntity.toDomain() = DungeonCompletionRecord(
    id = id,
    rewardKey = rewardKey,
    instanceId = instanceId,
    dungeonId = dungeonId,
    dungeonTitle = dungeonTitle,
    difficulty = runCatching { DungeonDifficulty.valueOf(difficultyName) }.getOrDefault(DungeonDifficulty.E_RANK),
    xpAwarded = xpAwarded,
    primaryAttribute = runCatching { AttributeType.valueOf(primaryAttrName) }.getOrDefault(AttributeType.DIS),
    primaryAttrXp = primaryAttrXp,
    secondaryAttribute = runCatching { AttributeType.valueOf(secondaryAttrName) }.getOrDefault(AttributeType.VIT),
    secondaryAttrXp = secondaryAttrXp,
    evidenceSummary = evidenceSummary,
    verificationProvenance = verificationProvenance,
    completedAtEpochMs = completedAtEpochMs
)

fun BossDefinitionEntity.toDomain() = BossBattleDefinition(
    id = id,
    code = code,
    title = title,
    subtitle = subtitle,
    description = description,
    category = runCatching { DungeonCategory.valueOf(categoryName) }.getOrDefault(DungeonCategory.MULTI_DOMAIN_CHALLENGE),
    difficulty = runCatching { DungeonDifficulty.valueOf(difficultyName) }.getOrDefault(DungeonDifficulty.E_RANK),
    requiredRank = runCatching { HunterRank.valueOf(requiredRankName) }.getOrDefault(HunterRank.E_RANK),
    requiredLevel = requiredLevel,
    xpReward = xpReward,
    primaryAttribute = runCatching { AttributeType.valueOf(primaryAttrName) }.getOrDefault(AttributeType.DIS),
    primaryAttrXp = primaryAttrXp,
    secondaryAttribute = runCatching { AttributeType.valueOf(secondaryAttrName) }.getOrDefault(AttributeType.WIS),
    secondaryAttrXp = secondaryAttrXp,
    rewardTitleId = rewardTitleId,
    rewardItemId = rewardItemId,
    rewardShadowId = rewardShadowId,
    totalEstimatedDays = totalEstimatedDays
)

fun BossBattleDefinition.toEntity() = BossDefinitionEntity(
    id = id,
    code = code,
    title = title,
    subtitle = subtitle,
    description = description,
    categoryName = category.name,
    difficultyName = difficulty.name,
    requiredRankName = requiredRank.name,
    requiredLevel = requiredLevel,
    xpReward = xpReward,
    primaryAttrName = primaryAttribute.name,
    primaryAttrXp = primaryAttrXp,
    secondaryAttrName = secondaryAttribute.name,
    secondaryAttrXp = secondaryAttrXp,
    rewardTitleId = rewardTitleId,
    rewardItemId = rewardItemId,
    rewardShadowId = rewardShadowId,
    totalEstimatedDays = totalEstimatedDays
)

fun BossBattlePhaseEntity.toDomain() = BossPhase(
    id = id,
    bossId = bossId,
    phaseNumber = phaseNumber,
    title = title,
    description = description,
    evidenceType = runCatching { DungeonEvidenceType.valueOf(evidenceTypeName) }.getOrDefault(DungeonEvidenceType.ACADEMIC_STUDY_MINUTES),
    targetValue = targetValue,
    requiresVerifiedOnly = requiresVerifiedOnly,
    weight = weight
)

fun BossPhase.toEntity(overrideBossId: Long = bossId) = BossBattlePhaseEntity(
    id = id,
    bossId = overrideBossId,
    phaseNumber = phaseNumber,
    title = title,
    description = description,
    evidenceTypeName = evidenceType.name,
    targetValue = targetValue,
    requiresVerifiedOnly = requiresVerifiedOnly,
    weight = weight
)

fun BossBattleInstanceEntity.toDomain() = BossBattleInstance(
    id = id,
    bossId = bossId,
    status = runCatching { BossStatus.valueOf(statusName) }.getOrDefault(BossStatus.AVAILABLE),
    startedAtEpochMs = startedAtEpochMs,
    completedAtEpochMs = completedAtEpochMs,
    abandonedAtEpochMs = abandonedAtEpochMs
)

fun BossBattleInstance.toEntity() = BossBattleInstanceEntity(
    id = id,
    bossId = bossId,
    statusName = status.name,
    startedAtEpochMs = startedAtEpochMs,
    completedAtEpochMs = completedAtEpochMs,
    abandonedAtEpochMs = abandonedAtEpochMs
)

fun SkillDefinitionEntity.toDomain() = SkillDefinition(
    id = id,
    name = name,
    specialization = runCatching { SkillSpecialization.valueOf(specializationName) }.getOrDefault(SkillSpecialization.PHYSICAL_DEVELOPMENT),
    tier = runCatching { SkillTier.valueOf(tierName) }.getOrDefault(SkillTier.COMMON),
    description = description,
    requiredRank = runCatching { HunterRank.valueOf(requiredRankName) }.getOrDefault(HunterRank.E_RANK),
    requiredLevel = requiredLevel,
    requiredAttribute = runCatching { AttributeType.valueOf(requiredAttrName) }.getOrDefault(AttributeType.STR),
    requiredAttributeValue = requiredAttributeValue,
    requiredVerifiedCount = requiredVerifiedCount,
    requiredStreakDays = requiredStreakDays,
    maxMasteryLevel = maxMasteryLevel,
    treeColumn = treeColumn,
    treeRow = treeRow
)

fun SkillDefinition.toEntity() = SkillDefinitionEntity(
    id = id,
    name = name,
    specializationName = specialization.name,
    tierName = tier.name,
    description = description,
    requiredRankName = requiredRank.name,
    requiredLevel = requiredLevel,
    requiredAttrName = requiredAttribute.name,
    requiredAttributeValue = requiredAttributeValue,
    requiredVerifiedCount = requiredVerifiedCount,
    requiredStreakDays = requiredStreakDays,
    maxMasteryLevel = maxMasteryLevel,
    treeColumn = treeColumn,
    treeRow = treeRow
)

fun SkillUnlockRecordEntity.toDomain() = SkillUnlockRecord(
    id = id,
    skillId = skillId,
    masteryLevel = masteryLevel,
    unlockedAtEpochMs = unlockedAtEpochMs,
    lastUpgradedAtEpochMs = lastUpgradedAtEpochMs,
    evidenceNote = evidenceNote
)

fun SkillEffectConfigEntity.toDomain() = SkillEffectSpec(
    skillId = skillId,
    effectCode = effectCode,
    title = title,
    description = description,
    featureTag = featureTag
)

fun SkillEffectSpec.toEntity() = SkillEffectConfigEntity(
    effectCode = effectCode,
    skillId = skillId,
    title = title,
    description = description,
    featureTag = featureTag
)

fun SkillTreeDependencyEntity.toDomain() = SkillTreeDependency(
    skillId = skillId,
    prerequisiteSkillId = prerequisiteSkillId,
    minPrerequisiteMastery = minPrerequisiteMastery
)

fun AchievementDefinitionEntity.toDomain() = AchievementDefinition(
    id = id,
    title = title,
    description = description,
    category = runCatching { AchievementCategory.valueOf(categoryName) }.getOrDefault(AchievementCategory.PERSONAL_MILESTONES),
    rarity = runCatching { RpgRarity.valueOf(rarityName) }.getOrDefault(RpgRarity.COMMON),
    criterionDescription = criterionDescription,
    targetValue = targetValue,
    xpBonus = xpBonus,
    rewardTitleId = rewardTitleId,
    rewardItemId = rewardItemId,
    rewardShadowId = rewardShadowId
)

fun AchievementDefinition.toEntity() = AchievementDefinitionEntity(
    id = id,
    title = title,
    description = description,
    categoryName = category.name,
    rarityName = rarity.name,
    criterionDescription = criterionDescription,
    targetValue = targetValue,
    xpBonus = xpBonus,
    rewardTitleId = rewardTitleId,
    rewardItemId = rewardItemId,
    rewardShadowId = rewardShadowId
)

fun AchievementUnlockRecordEntity.toDomain() = AchievementUnlockRecord(
    id = id,
    achievementId = achievementId,
    evidenceReference = evidenceReference,
    unlockedAtEpochMs = unlockedAtEpochMs
)

fun PlayerTitleEntity.toDomain() = PlayerTitleDefinition(
    id = id,
    name = name,
    description = description,
    rarity = runCatching { RpgRarity.valueOf(rarityName) }.getOrDefault(RpgRarity.COMMON),
    unlockRequirement = unlockRequirement,
    isUnlocked = isUnlocked,
    isEquipped = isEquipped,
    unlockedAtEpochMs = unlockedAtEpochMs
)

fun PlayerTitleDefinition.toEntity() = PlayerTitleEntity(
    id = id,
    name = name,
    description = description,
    rarityName = rarity.name,
    unlockRequirement = unlockRequirement,
    isUnlocked = isUnlocked,
    isEquipped = isEquipped,
    unlockedAtEpochMs = unlockedAtEpochMs
)

fun InventoryItemEntity.toDomain() = InventoryItemDefinition(
    id = id,
    name = name,
    category = runCatching { InventoryCategory.valueOf(categoryName) }.getOrDefault(InventoryCategory.COLLECTIBLES),
    rarity = runCatching { RpgRarity.valueOf(rarityName) }.getOrDefault(RpgRarity.COMMON),
    description = description,
    acquisitionSource = acquisitionSource,
    canEquip = canEquip
)

fun InventoryItemDefinition.toEntity() = InventoryItemEntity(
    id = id,
    name = name,
    categoryName = category.name,
    rarityName = rarity.name,
    description = description,
    acquisitionSource = acquisitionSource,
    canEquip = canEquip
)

fun PlayerInventoryRecordEntity.toDomain() = PlayerInventoryRecord(
    id = id,
    itemId = itemId,
    quantity = quantity,
    isEquipped = isEquipped,
    sourceDescription = sourceDescription,
    acquiredAtEpochMs = acquiredAtEpochMs
)

fun ShadowCollectionRecordEntity.toDomain() = ShadowCollectionRecord(
    id = id,
    collectibleId = collectibleId,
    rankTier = rankTier,
    evidenceSource = evidenceSource,
    unlockedAtEpochMs = unlockedAtEpochMs
)

fun RpgRewardAuditEntity.toDomain() = RpgRewardAuditRecord(
    id = id,
    rewardKey = rewardKey,
    sourceType = sourceType,
    sourceId = sourceId,
    title = title,
    xpAwarded = xpAwarded,
    primaryAttribute = runCatching { AttributeType.valueOf(primaryAttrName) }.getOrDefault(AttributeType.DIS),
    primaryAttrXp = primaryAttrXp,
    secondaryAttribute = secondaryAttrName?.let { runCatching { AttributeType.valueOf(it) }.getOrNull() },
    secondaryAttrXp = secondaryAttrXp,
    verificationProvenance = verificationProvenance,
    wasCapped = wasCapped,
    notes = notes,
    timestampEpochMs = timestampEpochMs
)

fun AiDungeonProposalEntity.toDomain() = AiDungeonProposal(
    id = id,
    proposalUuid = proposalUuid,
    title = title,
    description = description,
    category = runCatching { DungeonCategory.valueOf(categoryName) }.getOrDefault(DungeonCategory.MULTI_DOMAIN_CHALLENGE),
    difficulty = runCatching { DungeonDifficulty.valueOf(difficultyName) }.getOrDefault(DungeonDifficulty.E_RANK),
    objectiveTitle = objectiveTitle,
    targetEvidenceType = runCatching { DungeonEvidenceType.valueOf(targetEvidenceTypeName) }.getOrDefault(DungeonEvidenceType.PHYSICAL_JOG_MINUTES),
    targetValue = targetValue,
    timeLimitHours = timeLimitHours,
    requiresVerifiedOnly = requiresVerifiedOnly,
    reason = reason,
    isBossCampaign = isBossCampaign,
    status = runCatching { ProposalStatus.valueOf(statusName) }.getOrDefault(ProposalStatus.PENDING),
    createdAtEpochMs = createdAtEpochMs
)

fun AiDungeonProposal.toEntity() = AiDungeonProposalEntity(
    id = id,
    proposalUuid = proposalUuid,
    title = title,
    description = description,
    categoryName = category.name,
    difficultyName = difficulty.name,
    objectiveTitle = objectiveTitle,
    targetEvidenceTypeName = targetEvidenceType.name,
    targetValue = targetValue,
    timeLimitHours = timeLimitHours,
    requiresVerifiedOnly = requiresVerifiedOnly,
    reason = reason,
    isBossCampaign = isBossCampaign,
    statusName = status.name,
    createdAtEpochMs = createdAtEpochMs
)
