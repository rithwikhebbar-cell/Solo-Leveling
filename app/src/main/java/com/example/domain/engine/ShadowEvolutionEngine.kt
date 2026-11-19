package com.example.domain.engine

import androidx.room.withTransaction
import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.dao.ShadowDao
import com.example.data.local.dao.ShadowResourceDao
import com.example.data.local.dao.ShadowSkillUnlockDao
import com.example.data.local.dao.ShadowXpTraceDao
import com.example.data.local.dao.SystemExperienceDao
import com.example.data.local.entity.ShadowResourceTransactionEntity
import com.example.data.local.entity.ShadowSkillUnlockEntity
import com.example.data.local.entity.ShadowXpTraceEntity
import com.example.data.local.entity.SystemAnnouncementEntity
import com.example.data.local.entity.toDomain
import com.example.data.local.entity.toEntity
import com.example.domain.model.DEFAULT_SHADOW_ACCOUNT_OWNER_ID
import com.example.domain.model.Shadow
import com.example.domain.model.ShadowEvolutionEvaluation
import com.example.domain.model.ShadowEvolutionStage
import com.example.domain.model.ShadowEvolutionState
import com.example.domain.model.ShadowExtractionSourceType
import com.example.domain.model.ShadowGrowthConfig
import com.example.domain.model.ShadowRank
import com.example.domain.model.ShadowResourceType
import com.example.domain.model.ShadowResourceTransactionType
import com.example.domain.model.ShadowSkillCatalog
import com.example.domain.model.ShadowSkillUnlock
import com.example.domain.model.ShadowStats
import com.example.domain.model.SystemAnnouncementCategory
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

// ============================================================================
// SHADOW EVOLUTION ENGINE (Account-Isolated & Transactional)
// Controls shadow XP progression, deterministic leveling, stage caps,
// transactional evolution with explicit player confirmation, and skill growth.
// ============================================================================

class ShadowEvolutionEngine(
    private val shadowDao: ShadowDao,
    private val shadowSkillUnlockDao: ShadowSkillUnlockDao,
    private val monarchProgressionEngine: MonarchProgressionEngine,
    private val shadowXpTraceDao: ShadowXpTraceDao? = null,
    private val shadowResourceDao: ShadowResourceDao? = null,
    private val systemExperienceDao: SystemExperienceDao? = null,
    private val database: SoloLevelingDatabase? = null,
    private val mutex: Mutex = Mutex()
) {

    private val resourceDao: ShadowResourceDao?
        get() = shadowResourceDao ?: database?.shadowResourceDao()

    private val announcementDao: SystemExperienceDao?
        get() = systemExperienceDao ?: database?.systemExperienceDao()

    companion object {
        const val MONARCH_XP_PER_EVOLUTION = 200
        const val TRACE_TYPE_SHADOW_XP = "SHADOW_XP"

        private val RANK_UP_REQUIREMENTS: Map<ShadowRank, Pair<Int, Long>> = mapOf(
            ShadowRank.D_RANK to Pair(10, 500L),
            ShadowRank.C_RANK to Pair(25, 2000L),
            ShadowRank.B_RANK to Pair(40, 5000L),
            ShadowRank.A_RANK to Pair(60, 12000L),
            ShadowRank.S_RANK to Pair(80, 25000L),
            ShadowRank.MONARCH_CLASS to Pair(100, 50000L)
        )

        fun calculateRank(level: Int, totalXp: Long): ShadowRank {
            return ShadowRank.entries.sortedByDescending { it.order }.firstOrNull { rank ->
                val reqs = RANK_UP_REQUIREMENTS[rank]
                reqs != null && level >= reqs.first && totalXp >= reqs.second
            } ?: ShadowRank.E_RANK
        }

        fun calculateEvolutionStage(level: Int, rank: ShadowRank): ShadowEvolutionStage {
            return ShadowEvolutionStage.entries
                .sortedByDescending { it.order }
                .firstOrNull { stage ->
                    level >= stage.minLevel && rank.order >= stage.minRank.order
                } ?: ShadowEvolutionStage.NEWLY_EXTRACTED
        }

        fun xpToNextLevel(currentLevel: Int): Long {
            return (50L * currentLevel * currentLevel + 25L * currentLevel).coerceAtLeast(100L)
        }

        fun calculateNextLevelXp(currentLevel: Int): Long {
            return Shadow.shadowXpRequired(currentLevel + 1)
        }
    }

    private suspend fun <T> withLockedTransaction(block: suspend () -> T): T {
        return mutex.withLock {
            val db = database
            if (db != null) {
                db.withTransaction { block() }
            } else {
                block()
            }
        }
    }

    // -------------------------------------------------------------------------
    // AWARD SHADOW XP (Account-Isolated, Idempotent, Stage-Capped)
    // -------------------------------------------------------------------------

    suspend fun awardShadowXp(
        shadowId: String,
        xpAmount: Int,
        sourceTag: String,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Boolean {
        val owner = accountOwnerId.trim()
        val tag = sourceTag.trim()
        if (owner.isBlank() || shadowId.isBlank() || tag.isBlank() || xpAmount <= 0) return false

        return withLockedTransaction {
            val shadow = shadowDao.getShadowByIdForOwner(shadowId, owner) ?: return@withLockedTransaction false
            if (shadow.isRetired) return@withLockedTransaction false

            val now = System.currentTimeMillis()
            val compositeTag = "${shadowId}::$tag"
            if (shadowXpTraceDao != null) {
                if (shadowXpTraceDao.findTrace(owner, TRACE_TYPE_SHADOW_XP, compositeTag) != null) {
                    return@withLockedTransaction false
                }
                val inserted = shadowXpTraceDao.insertTraceIgnore(
                    ShadowXpTraceEntity(
                        traceId = UUID.randomUUID().toString(),
                        accountOwnerId = owner,
                        traceType = TRACE_TYPE_SHADOW_XP,
                        sourceTag = compositeTag,
                        targetEntityId = shadowId,
                        xpAmount = xpAmount,
                        description = "Shadow XP award ($tag)",
                        timestampEpochMs = now
                    )
                )
                if (inserted == -1L) return@withLockedTransaction false
            }

            val domain = shadow.toDomain()
            val newXp = domain.shadowXp + xpAmount
            val rawLevel = Shadow.shadowLevelForXp(newXp)
            val stageCap = ShadowGrowthConfig.getStageLevelCap(domain.evolutionStage)
            val newLevel = minOf(rawLevel, stageCap)
            val newRank = calculateRank(newLevel, newXp)

            val levelDiff = (newLevel - domain.level).coerceAtLeast(0)
            val newStats = if (levelDiff > 0) {
                val growth = ShadowGrowthConfig.getStatGrowthPerLevel(domain.shadowClass)
                domain.stats.copy(
                    power = domain.stats.power + (growth.power * levelDiff),
                    endurance = domain.stats.endurance + (growth.endurance * levelDiff),
                    focus = domain.stats.focus + (growth.focus * levelDiff),
                    discipline = domain.stats.discipline + (growth.discipline * levelDiff),
                    wisdom = domain.stats.wisdom + (growth.wisdom * levelDiff),
                    adaptability = domain.stats.adaptability + (growth.adaptability * levelDiff)
                )
            } else {
                domain.stats
            }

            // Note: evolutionStage is NOT modified during XP leveling.
            // Evolution requires explicit player confirmation and transactional resource consumption.
            val updated = domain.copy(
                accountOwnerId = owner,
                shadowXp = newXp,
                level = newLevel,
                currentRank = newRank,
                stats = newStats
            )
            val updatedRows = shadowDao.updateShadow(updated.toEntity())
            if (updatedRows <= 0) return@withLockedTransaction false

            if (newLevel > domain.level) {
                postAnnouncement(
                    announcementKey = "shadow_lvl_${shadowId}_${newLevel}_$now",
                    category = SystemAnnouncementCategory.LEVEL_UP,
                    title = "SHADOW LEVEL UP",
                    message = "Shadow ${domain.name} reached Level $newLevel.",
                    deepLinkRoute = "shadow_realm"
                )
            }

            true
        }
    }

    // -------------------------------------------------------------------------
    // AUTHORITATIVE SHADOW XP SOURCES (Dungeons, Boss Battles, Quests)
    // -------------------------------------------------------------------------

    suspend fun awardShadowXpFromEvent(
        shadowId: String,
        sourceType: ShadowExtractionSourceType,
        eventId: String,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Boolean {
        val xpAmount = when (sourceType) {
            ShadowExtractionSourceType.DUNGEON_COMPLETION -> ShadowGrowthConfig.XP_DUNGEON_COMPLETION
            ShadowExtractionSourceType.BOSS_BATTLE_COMPLETION -> ShadowGrowthConfig.XP_BOSS_DEFEAT
            ShadowExtractionSourceType.MONARCH_QUEST -> ShadowGrowthConfig.XP_QUEST_COMPLETION
            else -> ShadowGrowthConfig.XP_MILESTONE
        }
        val tag = "${sourceType.name.lowercase()}_$eventId"
        return awardShadowXp(shadowId, xpAmount, tag, accountOwnerId)
    }

    suspend fun awardShadowXpFromDungeon(
        shadowId: String,
        dungeonId: String,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Boolean = awardShadowXp(
        shadowId = shadowId,
        xpAmount = ShadowGrowthConfig.XP_DUNGEON_COMPLETION,
        sourceTag = "dungeon_$dungeonId",
        accountOwnerId = accountOwnerId
    )

    suspend fun awardShadowXpFromBoss(
        shadowId: String,
        bossId: String,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Boolean = awardShadowXp(
        shadowId = shadowId,
        xpAmount = ShadowGrowthConfig.XP_BOSS_DEFEAT,
        sourceTag = "boss_$bossId",
        accountOwnerId = accountOwnerId
    )

    suspend fun awardShadowXpFromQuest(
        shadowId: String,
        questId: String,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Boolean = awardShadowXp(
        shadowId = shadowId,
        xpAmount = ShadowGrowthConfig.XP_QUEST_COMPLETION,
        sourceTag = "quest_$questId",
        accountOwnerId = accountOwnerId
    )

    // -------------------------------------------------------------------------
    // EVOLUTION EVALUATION (Non-destructive Requirements & Balance Check)
    // -------------------------------------------------------------------------

    suspend fun evaluateEvolution(
        shadowId: String,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): ShadowEvolutionEvaluation {
        val owner = accountOwnerId.trim()
        if (owner.isBlank()) {
            return createLockedEvaluation(shadowId, "Invalid account owner")
        }
        val shadow = shadowDao.getShadowByIdForOwner(shadowId, owner)?.toDomain()
            ?: return createLockedEvaluation(shadowId, "Shadow not found")

        val currentStage = shadow.evolutionStage
        val nextStage = currentStage.nextStage()
        if (nextStage == null) {
            return ShadowEvolutionEvaluation(
                shadowId = shadowId,
                shadowName = shadow.name,
                currentStage = currentStage,
                targetStage = null,
                state = ShadowEvolutionState.MAX_STAGE_REACHED,
                isEligible = false,
                currentLevel = shadow.level,
                requiredLevel = shadow.level,
                currentRank = shadow.currentRank,
                requiredRank = shadow.currentRank,
                resourceCosts = emptyMap(),
                currentBalances = emptyMap(),
                blockers = listOf("Shadow has achieved maximum evolution stage: ${currentStage.label}"),
                statPreview = null
            )
        }

        val blockers = mutableListOf<String>()
        if (shadow.level < nextStage.minLevel) {
            blockers.add("Reach Level ${nextStage.minLevel} (currently Level ${shadow.level})")
        }
        if (shadow.currentRank.order < nextStage.minRank.order) {
            blockers.add("Reach ${nextStage.minRank.label} (currently ${shadow.currentRank.label})")
        }

        val costs = ShadowGrowthConfig.getEvolutionCosts(nextStage)
        val resDao = resourceDao
        val currentBalances = ShadowResourceType.entries.associateWith { type ->
            resDao?.getCurrentBalanceForOwner(owner, type.name) ?: 0
        }

        for ((resourceType, cost) in costs) {
            val balance = currentBalances[resourceType] ?: 0
            if (balance < cost) {
                blockers.add("Insufficient ${resourceType.label}: $balance / $cost")
            }
        }

        val state = when {
            shadow.level < nextStage.minLevel -> ShadowEvolutionState.INSUFFICIENT_LEVEL
            shadow.currentRank.order < nextStage.minRank.order -> ShadowEvolutionState.INSUFFICIENT_RANK
            costs.any { (res, cost) -> (currentBalances[res] ?: 0) < cost } -> ShadowEvolutionState.INSUFFICIENT_RESOURCES
            blockers.isNotEmpty() -> ShadowEvolutionState.REQUIREMENTS_NOT_MET
            else -> ShadowEvolutionState.READY_TO_EVOLVE
        }

        val statBonus = ShadowGrowthConfig.getStageStatBonusIncrement(nextStage)
        val statPreview = shadow.stats.copy(
            power = shadow.stats.power + statBonus.power,
            endurance = shadow.stats.endurance + statBonus.endurance,
            focus = shadow.stats.focus + statBonus.focus,
            discipline = shadow.stats.discipline + statBonus.discipline,
            wisdom = shadow.stats.wisdom + statBonus.wisdom,
            adaptability = shadow.stats.adaptability + statBonus.adaptability
        )

        return ShadowEvolutionEvaluation(
            shadowId = shadowId,
            shadowName = shadow.name,
            currentStage = currentStage,
            targetStage = nextStage,
            state = state,
            isEligible = state == ShadowEvolutionState.READY_TO_EVOLVE,
            currentLevel = shadow.level,
            requiredLevel = nextStage.minLevel,
            currentRank = shadow.currentRank,
            requiredRank = nextStage.minRank,
            resourceCosts = costs,
            currentBalances = currentBalances,
            blockers = blockers,
            statPreview = statPreview
        )
    }

    private fun createLockedEvaluation(shadowId: String, reason: String): ShadowEvolutionEvaluation {
        return ShadowEvolutionEvaluation(
            shadowId = shadowId,
            shadowName = "Unknown",
            currentStage = ShadowEvolutionStage.NEWLY_EXTRACTED,
            targetStage = null,
            state = ShadowEvolutionState.LOCKED,
            isEligible = false,
            currentLevel = 1,
            requiredLevel = 1,
            currentRank = ShadowRank.E_RANK,
            requiredRank = ShadowRank.E_RANK,
            resourceCosts = emptyMap(),
            currentBalances = emptyMap(),
            blockers = listOf(reason),
            statPreview = null
        )
    }

    suspend fun isEligibleForEvolution(
        shadowId: String,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Pair<Boolean, List<String>> {
        val eval = evaluateEvolution(shadowId, accountOwnerId)
        return Pair(eval.isEligible, eval.blockers)
    }

    // -------------------------------------------------------------------------
    // CONFIRM EVOLUTION (Explicit Player Confirmation, Transactional Resources)
    // -------------------------------------------------------------------------

    suspend fun confirmEvolution(
        shadowId: String,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Result<Shadow> {
        return runCatching {
            val owner = accountOwnerId.trim()
            require(owner.isNotBlank()) { "accountOwnerId must not be blank" }
            require(shadowId.isNotBlank()) { "shadowId must not be blank" }

            withLockedTransaction {
                val shadowEntity = shadowDao.getShadowByIdForOwner(shadowId, owner)
                    ?: throw IllegalStateException("Shadow not found: $shadowId")
                val shadow = shadowEntity.toDomain()
                if (shadow.isRetired) {
                    throw IllegalStateException("Retired shadows cannot undergo evolution.")
                }

                val currentStage = shadow.evolutionStage
                val nextStage = currentStage.nextStage()
                    ?: throw IllegalStateException("Shadow is already at maximum evolution stage: ${currentStage.label}")

                if (shadow.level < nextStage.minLevel) {
                    throw IllegalStateException("Shadow level ${shadow.level} < required level ${nextStage.minLevel}")
                }
                if (shadow.currentRank.order < nextStage.minRank.order) {
                    throw IllegalStateException("Shadow rank ${shadow.currentRank.label} < required rank ${nextStage.minRank.label}")
                }

                val costs = ShadowGrowthConfig.getEvolutionCosts(nextStage)
                val resDao = resourceDao
                    ?: throw IllegalStateException("Resource DAO not available for transactional evolution")

                // Pre-verify all balances before deducting
                for ((resType, cost) in costs) {
                    val balance = resDao.getCurrentBalanceForOwner(owner, resType.name)
                    if (balance < cost) {
                        throw IllegalStateException("Insufficient ${resType.label}: balance $balance < cost $cost")
                    }
                }

                val now = System.currentTimeMillis()

                // Atomically deduct resources
                for ((resType, cost) in costs) {
                    val balance = resDao.getCurrentBalanceForOwner(owner, resType.name)
                    val newBalance = balance - cost
                    val txKey = "evolution_${shadowId}_${nextStage.name}_${resType.name}_$now"
                    val inserted = resDao.insertTransaction(
                        ShadowResourceTransactionEntity(
                            transactionId = UUID.randomUUID().toString(),
                            accountOwnerId = owner,
                            resourceTypeName = resType.name,
                            amount = -cost,
                            transactionTypeName = ShadowResourceTransactionType.CONSUMED.name,
                            sourceEventId = txKey,
                            sourceDescription = "Evolution of ${shadow.name} to ${nextStage.label}",
                            balanceAfter = newBalance,
                            timestampEpochMs = now
                        )
                    )
                    if (inserted <= 0L) {
                        throw IllegalStateException("Failed to record resource deduction for ${resType.name}")
                    }
                }

                // Apply stage stat bonus
                val stageBonus = ShadowGrowthConfig.getStageStatBonusIncrement(nextStage)
                val updatedStats = shadow.stats.copy(
                    power = shadow.stats.power + stageBonus.power,
                    endurance = shadow.stats.endurance + stageBonus.endurance,
                    focus = shadow.stats.focus + stageBonus.focus,
                    discipline = shadow.stats.discipline + stageBonus.discipline,
                    wisdom = shadow.stats.wisdom + stageBonus.wisdom,
                    adaptability = shadow.stats.adaptability + stageBonus.adaptability
                )

                // Append to evolution history
                val existingHistory = try {
                    Json.decodeFromString<List<String>>(shadow.evolutionHistoryJson)
                } catch (_: Exception) { emptyList() }
                val updatedHistoryJson = Json.encodeToString(existingHistory + "${nextStage.name}:$now")

                val evolved = shadow.copy(
                    accountOwnerId = owner,
                    evolutionStage = nextStage,
                    stats = updatedStats,
                    evolutionHistoryJson = updatedHistoryJson
                )

                val updatedRows = shadowDao.updateShadow(evolved.toEntity())
                if (updatedRows <= 0) {
                    throw IllegalStateException("Failed to update evolved shadow in database")
                }

                // Award Monarch XP
                monarchProgressionEngine.awardMonarchXpLocked(
                    accountOwnerId = owner,
                    sourceTag = "evolution_${shadowId}_${nextStage.name}",
                    xpAmount = MONARCH_XP_PER_EVOLUTION,
                    sourceDescription = "Shadow ${shadow.name} evolved to ${nextStage.label}"
                )
                monarchProgressionEngine.refreshMonarchProfileLocked(owner)

                // Post announcement
                postAnnouncement(
                    announcementKey = "shadow_evolved_${shadowId}_${nextStage.name}_$now",
                    category = SystemAnnouncementCategory.RANK_UP,
                    title = "SHADOW EVOLVED",
                    message = "Shadow ${shadow.name} has evolved to ${nextStage.label}!",
                    deepLinkRoute = "shadow_realm"
                )

                evolved
            }
        }
    }

    // -------------------------------------------------------------------------
    // SHADOW SKILL MANAGEMENT (Owner-Verified & Transactional)
    // -------------------------------------------------------------------------

    suspend fun unlockShadowSkill(
        shadowId: String,
        skillId: String,
        evidenceNote: String,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Boolean {
        val owner = accountOwnerId.trim()
        if (owner.isBlank() || shadowId.isBlank() || skillId.isBlank()) return false

        return withLockedTransaction {
            val shadow = shadowDao.getShadowByIdForOwner(shadowId, owner)?.toDomain()
                ?: return@withLockedTransaction false
            if (shadow.isRetired) return@withLockedTransaction false
            val skillDef = ShadowSkillCatalog.findById(skillId) ?: return@withLockedTransaction false

            val existing = shadowSkillUnlockDao.getUnlockForOwner(owner, shadowId, skillId)
            if (existing != null) return@withLockedTransaction false

            if (shadow.currentRank.order < skillDef.requiredShadowRank.order) return@withLockedTransaction false
            if (shadow.level < skillDef.requiredShadowLevel) return@withLockedTransaction false

            for (prereqId in skillDef.prerequisiteSkillIds) {
                val prereqUnlocked = shadowSkillUnlockDao.getUnlockForOwner(owner, shadowId, prereqId)
                if (prereqUnlocked == null) return@withLockedTransaction false
            }

            val now = System.currentTimeMillis()
            val inserted = shadowSkillUnlockDao.insertUnlock(
                ShadowSkillUnlockEntity(
                    accountOwnerId = owner,
                    shadowId = shadowId,
                    skillId = skillId,
                    unlockedAtEpochMs = now,
                    evidenceNote = evidenceNote
                )
            )
            if (inserted <= 0L) return@withLockedTransaction false

            val updatedSkills = (shadow.activeSkillIds + skillId).distinct()
            shadowDao.updateShadow(shadow.copy(accountOwnerId = owner, activeSkillIds = updatedSkills).toEntity())

            postAnnouncement(
                announcementKey = "shadow_skill_unlock_${shadowId}_${skillId}_$now",
                category = SystemAnnouncementCategory.SYSTEM_ALERT,
                title = "NEW SHADOW SKILL UNLOCKED",
                message = "Shadow ${shadow.name} unlocked: ${skillDef.name}.",
                deepLinkRoute = "shadow_realm"
            )

            true
        }
    }

    suspend fun getShadowSkillUnlocks(
        shadowId: String,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): List<ShadowSkillUnlock> {
        return shadowSkillUnlockDao.getUnlocksForShadowForOwnerOnce(accountOwnerId, shadowId)
            .map { it.toDomain() }
    }

    fun getRankUpRequirements(currentRank: ShadowRank): Pair<Int, Long>? {
        val nextRank = currentRank.nextRank() ?: return null
        return RANK_UP_REQUIREMENTS[nextRank]
    }

    private suspend fun postAnnouncement(
        announcementKey: String,
        category: SystemAnnouncementCategory,
        title: String,
        message: String,
        deepLinkRoute: String
    ) {
        val annDao = announcementDao ?: return
        try {
            annDao.insertAnnouncement(
                SystemAnnouncementEntity(
                    announcementKey = announcementKey,
                    eventId = announcementKey,
                    categoryName = category.name,
                    channelId = "system_announcements",
                    title = title,
                    message = message,
                    voiceSummary = message,
                    deepLinkRoute = deepLinkRoute,
                    actionLabel = "View Legion",
                    priorityName = "NORMAL",
                    wasSpoken = false,
                    wasDispatchedOs = false,
                    isRead = false,
                    createdAtEpochMs = System.currentTimeMillis()
                )
            )
        } catch (_: Exception) {
            // Announcement insertion is best-effort and non-blocking
        }
    }
}
