package com.example.domain.engine

import androidx.room.withTransaction
import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.dao.ShadowDao
import com.example.data.local.dao.ShadowSkillUnlockDao
import com.example.data.local.dao.ShadowXpTraceDao
import com.example.data.local.entity.ShadowSkillUnlockEntity
import com.example.data.local.entity.ShadowXpTraceEntity
import com.example.data.local.entity.toDomain
import com.example.data.local.entity.toEntity
import com.example.domain.model.DEFAULT_SHADOW_ACCOUNT_OWNER_ID
import com.example.domain.model.Shadow
import com.example.domain.model.ShadowEvolutionStage
import com.example.domain.model.ShadowRank
import com.example.domain.model.ShadowSkillCatalog
import com.example.domain.model.ShadowSkillUnlock
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

// ============================================================================
// SHADOW EVOLUTION ENGINE (Account-Isolated & Idempotency-Protected)
// Controls shadow XP, leveling, rank progression, and evolution stages.
// ============================================================================

class ShadowEvolutionEngine(
    private val shadowDao: ShadowDao,
    private val shadowSkillUnlockDao: ShadowSkillUnlockDao,
    private val monarchProgressionEngine: MonarchProgressionEngine,
    private val shadowXpTraceDao: ShadowXpTraceDao? = null,
    private val database: SoloLevelingDatabase? = null,
    private val mutex: Mutex = Mutex()
) {

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
    // AWARD SHADOW XP (Owner-Verified & Idempotent)
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
            val newLevel = Shadow.shadowLevelForXp(newXp)
            val newRank = calculateRank(newLevel, newXp)

            val oldEvolutionStage = domain.evolutionStage
            val newEvolutionStage = calculateEvolutionStage(newLevel, newRank)

            val evolutionHistory = if (newEvolutionStage != oldEvolutionStage) {
                val existing = try {
                    Json.decodeFromString<List<String>>(domain.evolutionHistoryJson)
                } catch (_: Exception) { emptyList() }
                (existing + "${newEvolutionStage.name}:$now").let { Json.encodeToString(it) }
            } else {
                domain.evolutionHistoryJson
            }

            val updated = domain.copy(
                accountOwnerId = owner,
                shadowXp = newXp,
                level = newLevel,
                currentRank = newRank,
                evolutionStage = newEvolutionStage,
                evolutionHistoryJson = evolutionHistory
            )
            val updatedRows = shadowDao.updateShadow(updated.toEntity())
            if (updatedRows <= 0) return@withLockedTransaction false

            if (newEvolutionStage != oldEvolutionStage) {
                monarchProgressionEngine.awardMonarchXpLocked(
                    accountOwnerId = owner,
                    sourceTag = "evolution_${shadowId}_${newEvolutionStage.name}",
                    xpAmount = MONARCH_XP_PER_EVOLUTION,
                    sourceDescription = "Shadow ${domain.name} evolved to ${newEvolutionStage.label}"
                )
                monarchProgressionEngine.refreshMonarchProfileLocked(owner)
            }

            true
        }
    }

    // -------------------------------------------------------------------------
    // EVOLUTION ELIGIBILITY
    // -------------------------------------------------------------------------

    suspend fun isEligibleForEvolution(
        shadowId: String,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Pair<Boolean, List<String>> {
        val owner = accountOwnerId.trim()
        if (owner.isBlank()) return Pair(false, listOf("Invalid account owner"))
        val shadow = shadowDao.getShadowByIdForOwner(shadowId, owner)?.toDomain()
            ?: return Pair(false, listOf("Shadow not found"))
        val blockers = mutableListOf<String>()

        val nextStageIdx = shadow.evolutionStage.order
        val nextStage = ShadowEvolutionStage.entries.firstOrNull { it.order == nextStageIdx + 1 }
            ?: return Pair(false, listOf("Shadow is at maximum evolution stage"))

        if (shadow.level < nextStage.minLevel) {
            blockers.add("Shadow must reach Level ${nextStage.minLevel} (currently ${shadow.level})")
        }
        if (shadow.currentRank.order < nextStage.minRank.order) {
            blockers.add("Shadow must reach ${nextStage.minRank.label} (currently ${shadow.currentRank.label})")
        }

        return Pair(blockers.isEmpty(), blockers)
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

            val inserted = shadowSkillUnlockDao.insertUnlock(
                ShadowSkillUnlockEntity(
                    accountOwnerId = owner,
                    shadowId = shadowId,
                    skillId = skillId,
                    unlockedAtEpochMs = System.currentTimeMillis(),
                    evidenceNote = evidenceNote
                )
            )
            if (inserted <= 0L) return@withLockedTransaction false

            val updatedSkills = (shadow.activeSkillIds + skillId).distinct()
            shadowDao.updateShadow(shadow.copy(accountOwnerId = owner, activeSkillIds = updatedSkills).toEntity())

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
}
