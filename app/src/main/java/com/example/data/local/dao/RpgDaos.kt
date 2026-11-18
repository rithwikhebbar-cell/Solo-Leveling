package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.AchievementDefinitionEntity
import com.example.data.local.entity.AchievementUnlockRecordEntity
import com.example.data.local.entity.AiDungeonProposalEntity
import com.example.data.local.entity.BossBattleInstanceEntity
import com.example.data.local.entity.BossBattlePhaseEntity
import com.example.data.local.entity.BossDefinitionEntity
import com.example.data.local.entity.DungeonCompletionRecordEntity
import com.example.data.local.entity.DungeonDefinitionEntity
import com.example.data.local.entity.DungeonInstanceEntity
import com.example.data.local.entity.DungeonObjectiveEntity
import com.example.data.local.entity.InventoryItemEntity
import com.example.data.local.entity.PlayerInventoryRecordEntity
import com.example.data.local.entity.PlayerTitleEntity
import com.example.data.local.entity.RpgRewardAuditEntity
import com.example.data.local.entity.ShadowCollectionRecordEntity
import com.example.data.local.entity.SkillDefinitionEntity
import com.example.data.local.entity.SkillEffectConfigEntity
import com.example.data.local.entity.SkillTreeDependencyEntity
import com.example.data.local.entity.SkillUnlockRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DungeonDao {
    @Query("SELECT * FROM dungeon_definitions WHERE isActiveCatalog = 1 ORDER BY id ASC")
    fun observeDefinitions(): Flow<List<DungeonDefinitionEntity>>

    @Query("SELECT * FROM dungeon_definitions")
    suspend fun getAllDefinitionsOnce(): List<DungeonDefinitionEntity>

    @Query("SELECT * FROM dungeon_definitions WHERE id = :id")
    suspend fun getDefinitionById(id: Long): DungeonDefinitionEntity?

    @Query("SELECT * FROM dungeon_definitions WHERE code = :code LIMIT 1")
    suspend fun getDefinitionByCode(code: String): DungeonDefinitionEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefinition(entity: DungeonDefinitionEntity): Long

    @Query("SELECT * FROM dungeon_objectives ORDER BY orderIndex ASC")
    fun observeAllObjectives(): Flow<List<DungeonObjectiveEntity>>

    @Query("SELECT * FROM dungeon_objectives WHERE dungeonId = :dungeonId ORDER BY orderIndex ASC")
    suspend fun getObjectivesForDungeonOnce(dungeonId: Long): List<DungeonObjectiveEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObjectives(entities: List<DungeonObjectiveEntity>)

    @Query("SELECT * FROM dungeon_instances ORDER BY id DESC")
    fun observeAllInstances(): Flow<List<DungeonInstanceEntity>>

    @Query("SELECT * FROM dungeon_instances")
    suspend fun getAllInstancesOnce(): List<DungeonInstanceEntity>

    @Query("SELECT * FROM dungeon_instances WHERE id = :id")
    suspend fun getInstanceById(id: Long): DungeonInstanceEntity?

    @Query("SELECT * FROM dungeon_instances WHERE dungeonId = :dungeonId AND statusName IN ('ACTIVE', 'PAUSED') LIMIT 1")
    suspend fun getActiveOrPausedInstanceForDungeon(dungeonId: Long): DungeonInstanceEntity?

    @Insert
    suspend fun insertInstance(entity: DungeonInstanceEntity): Long

    @Update
    suspend fun updateInstance(entity: DungeonInstanceEntity)

    @Query("SELECT * FROM dungeon_completions ORDER BY completedAtEpochMs DESC")
    fun observeCompletionHistory(): Flow<List<DungeonCompletionRecordEntity>>

    @Query("SELECT * FROM dungeon_completions")
    suspend fun getAllCompletionsOnce(): List<DungeonCompletionRecordEntity>

    @Query("SELECT COUNT(*) FROM dungeon_completions")
    suspend fun getTotalClearedCountOnce(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCompletionRecord(entity: DungeonCompletionRecordEntity): Long

    @Query("SELECT * FROM ai_dungeon_proposals ORDER BY createdAtEpochMs DESC")
    fun observeDungeonProposals(): Flow<List<AiDungeonProposalEntity>>

    @Query("SELECT * FROM ai_dungeon_proposals")
    suspend fun getAllDungeonProposalsOnce(): List<AiDungeonProposalEntity>

    @Query("SELECT * FROM ai_dungeon_proposals WHERE id = :id")
    suspend fun getDungeonProposalById(id: Long): AiDungeonProposalEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDungeonProposals(entities: List<AiDungeonProposalEntity>)

    @Update
    suspend fun updateDungeonProposal(entity: AiDungeonProposalEntity)
}

@Dao
interface BossBattleDao {
    @Query("SELECT * FROM boss_definitions ORDER BY id ASC")
    fun observeBossDefinitions(): Flow<List<BossDefinitionEntity>>

    @Query("SELECT * FROM boss_definitions")
    suspend fun getAllBossDefinitionsOnce(): List<BossDefinitionEntity>

    @Query("SELECT * FROM boss_definitions WHERE id = :id")
    suspend fun getBossById(id: Long): BossDefinitionEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBossDefinition(entity: BossDefinitionEntity): Long

    @Query("SELECT * FROM boss_phases ORDER BY bossId ASC, phaseNumber ASC")
    fun observeAllPhases(): Flow<List<BossBattlePhaseEntity>>

    @Query("SELECT * FROM boss_phases WHERE bossId = :bossId ORDER BY phaseNumber ASC")
    suspend fun getPhasesForBossOnce(bossId: Long): List<BossBattlePhaseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhases(entities: List<BossBattlePhaseEntity>)

    @Query("SELECT * FROM boss_instances ORDER BY id DESC")
    fun observeAllBossInstances(): Flow<List<BossBattleInstanceEntity>>

    @Query("SELECT * FROM boss_instances")
    suspend fun getAllBossInstancesOnce(): List<BossBattleInstanceEntity>

    @Query("SELECT * FROM boss_instances WHERE id = :id")
    suspend fun getBossInstanceById(id: Long): BossBattleInstanceEntity?

    @Query("SELECT * FROM boss_instances WHERE bossId = :bossId AND statusName = 'ACTIVE' LIMIT 1")
    suspend fun getActiveInstanceForBoss(bossId: Long): BossBattleInstanceEntity?

    @Insert
    suspend fun insertBossInstance(entity: BossBattleInstanceEntity): Long

    @Update
    suspend fun updateBossInstance(entity: BossBattleInstanceEntity)
}

@Dao
interface SkillDao {
    @Query("SELECT * FROM skill_definitions")
    fun observeSkillDefinitions(): Flow<List<SkillDefinitionEntity>>

    @Query("SELECT * FROM skill_definitions")
    suspend fun getAllDefinitionsOnce(): List<SkillDefinitionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSkillDefinitions(entities: List<SkillDefinitionEntity>)

    @Query("SELECT * FROM skill_effect_configs")
    fun observeSkillEffects(): Flow<List<SkillEffectConfigEntity>>

    @Query("SELECT * FROM skill_effect_configs")
    suspend fun getAllEffectsOnce(): List<SkillEffectConfigEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSkillEffects(entities: List<SkillEffectConfigEntity>)

    @Query("SELECT * FROM skill_tree_dependencies")
    fun observeDependencies(): Flow<List<SkillTreeDependencyEntity>>

    @Query("SELECT * FROM skill_tree_dependencies")
    suspend fun getAllDependenciesOnce(): List<SkillTreeDependencyEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDependencies(entities: List<SkillTreeDependencyEntity>)

    @Query("SELECT * FROM skill_unlock_records")
    fun observeSkillUnlocks(): Flow<List<SkillUnlockRecordEntity>>

    @Query("SELECT * FROM skill_unlock_records")
    suspend fun getAllUnlocksOnce(): List<SkillUnlockRecordEntity>

    @Query("SELECT * FROM skill_unlock_records WHERE skillId = :skillId LIMIT 1")
    suspend fun getUnlockBySkillId(skillId: String): SkillUnlockRecordEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSkillUnlock(entity: SkillUnlockRecordEntity): Long

    @Update
    suspend fun updateSkillUnlock(entity: SkillUnlockRecordEntity)
}

@Dao
interface AchievementTitleDao {
    @Query("SELECT * FROM achievement_definitions")
    fun observeAchievementDefinitions(): Flow<List<AchievementDefinitionEntity>>

    @Query("SELECT * FROM achievement_definitions")
    suspend fun getAllAchievementDefinitionsOnce(): List<AchievementDefinitionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievementDefinitions(entities: List<AchievementDefinitionEntity>)

    @Query("SELECT * FROM achievement_unlock_records ORDER BY unlockedAtEpochMs DESC")
    fun observeAchievementUnlocks(): Flow<List<AchievementUnlockRecordEntity>>

    @Query("SELECT * FROM achievement_unlock_records")
    suspend fun getAllAchievementUnlocksOnce(): List<AchievementUnlockRecordEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAchievementUnlock(entity: AchievementUnlockRecordEntity): Long

    @Query("SELECT * FROM player_titles ORDER BY isUnlocked DESC, rarityName ASC")
    fun observePlayerTitles(): Flow<List<PlayerTitleEntity>>

    @Query("SELECT * FROM player_titles")
    suspend fun getAllPlayerTitlesOnce(): List<PlayerTitleEntity>

    @Query("SELECT * FROM player_titles WHERE id = :id")
    suspend fun getTitleById(id: String): PlayerTitleEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTitles(entities: List<PlayerTitleEntity>)

    @Query("UPDATE player_titles SET isEquipped = 0")
    suspend fun clearEquippedTitle()

    @Query("UPDATE player_titles SET isUnlocked = 1, unlockedAtEpochMs = COALESCE(unlockedAtEpochMs, :timestampMs) WHERE id = :titleId")
    suspend fun unlockTitle(titleId: String, timestampMs: Long)

    @Query("UPDATE player_titles SET isEquipped = 1 WHERE id = :titleId AND isUnlocked = 1")
    suspend fun equipUnlockedTitle(titleId: String)
}

@Dao
interface InventoryShadowDao {
    @Query("SELECT * FROM inventory_items")
    fun observeInventoryItems(): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM inventory_items")
    suspend fun getAllInventoryItemsOnce(): List<InventoryItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryItems(entities: List<InventoryItemEntity>)

    @Query("SELECT * FROM player_inventory_records ORDER BY acquiredAtEpochMs DESC")
    fun observePlayerInventory(): Flow<List<PlayerInventoryRecordEntity>>

    @Query("SELECT * FROM player_inventory_records")
    suspend fun getAllPlayerInventoryOnce(): List<PlayerInventoryRecordEntity>

    @Query("SELECT * FROM player_inventory_records WHERE itemId = :itemId LIMIT 1")
    suspend fun getInventoryRecordByItemId(itemId: String): PlayerInventoryRecordEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertInventoryRecord(entity: PlayerInventoryRecordEntity): Long

    @Update
    suspend fun updateInventoryRecord(entity: PlayerInventoryRecordEntity)

    @Query("SELECT * FROM shadow_collection_records ORDER BY unlockedAtEpochMs DESC")
    fun observeShadowCollection(): Flow<List<ShadowCollectionRecordEntity>>

    @Query("SELECT * FROM shadow_collection_records")
    suspend fun getAllShadowCollectionOnce(): List<ShadowCollectionRecordEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertShadowRecord(entity: ShadowCollectionRecordEntity): Long
}

@Dao
interface RpgAuditDao {
    @Query("SELECT * FROM rpg_reward_audit ORDER BY timestampEpochMs DESC")
    fun observeRewardAudit(): Flow<List<RpgRewardAuditEntity>>

    @Query("SELECT * FROM rpg_reward_audit ORDER BY timestampEpochMs DESC")
    suspend fun getAllOnce(): List<RpgRewardAuditEntity>

    @Query("SELECT * FROM rpg_reward_audit WHERE accountOwnerId = :accountOwnerId ORDER BY timestampEpochMs DESC")
    suspend fun getAllForOwnerOnce(accountOwnerId: String): List<RpgRewardAuditEntity>

    @Query("SELECT * FROM rpg_reward_audit WHERE rewardKey = :rewardKey LIMIT 1")
    suspend fun getByRewardKey(rewardKey: String): RpgRewardAuditEntity?

    @Query("SELECT COALESCE(SUM(xpAwarded), 0) FROM rpg_reward_audit WHERE timestampEpochMs >= :sinceEpochMs")
    suspend fun sumRpgXpSince(sinceEpochMs: Long): Int

    @Query("""
        SELECT COALESCE(SUM(xpAwarded), 0)
        FROM rpg_reward_audit
        WHERE sourceType = :sourceType
          AND accountOwnerId = :accountOwnerId
          AND timestampEpochMs >= :dayStartEpochMs
          AND timestampEpochMs < :dayEndEpochMs
    """)
    suspend fun sumSocialXpInWindow(
        sourceType: String,
        accountOwnerId: String,
        dayStartEpochMs: Long,
        dayEndEpochMs: Long
    ): Int

    @Query("""
        SELECT COUNT(*)
        FROM rpg_reward_audit
        WHERE sourceType = :sourceType
          AND accountOwnerId = :accountOwnerId
          AND verificationProvenance IN ('SELF_REPORTED', 'PENDING_REVIEW')
          AND xpAwarded > 0
          AND timestampEpochMs >= :dayStartEpochMs
          AND timestampEpochMs < :dayEndEpochMs
    """)
    suspend fun countSocialSelfReportedInWindow(
        sourceType: String,
        accountOwnerId: String,
        dayStartEpochMs: Long,
        dayEndEpochMs: Long
    ): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAudit(entity: RpgRewardAuditEntity): Long
}
