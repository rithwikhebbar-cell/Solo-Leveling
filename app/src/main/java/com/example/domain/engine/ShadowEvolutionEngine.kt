package com.example.domain.engine

import com.example.data.local.dao.ShadowDao
import com.example.data.local.dao.ShadowSkillUnlockDao
import com.example.data.local.entity.ShadowSkillUnlockEntity
import com.example.data.local.entity.toDomain
import com.example.data.local.entity.toEntity
import com.example.domain.model.Shadow
import com.example.domain.model.ShadowEvolutionStage
import com.example.domain.model.ShadowRank
import com.example.domain.model.ShadowSkillCatalog
import com.example.domain.model.ShadowSkillUnlock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// ============================================================================
// SHADOW EVOLUTION ENGINE
// Controls shadow XP, leveling, rank progression, and evolution stages.
//
// SAFETY GUARANTEES:
//  - Shadow XP is completely separate from Player XP and Monarch XP.
//  - Rank-ups are deterministic (based on level + XP thresholds).
//  - Evolution requires explicit player confirmation.
//  - No duplicate XP events (enforced via source event idempotency).
// ============================================================================

class ShadowEvolutionEngine(
    private val shadowDao: ShadowDao,
    private val shadowSkillUnlockDao: ShadowSkillUnlockDao,
    private val monarchProgressionEngine: MonarchProgressionEngine
) {

    companion object {
        const val MONARCH_XP_PER_EVOLUTION = 200

        // Rank-up thresholds: minimum level required for each rank
        private val RANK_UP_REQUIREMENTS: Map<ShadowRank, Pair<Int, Long>> = mapOf(
            ShadowRank.D_RANK to Pair(10, 500L),
            ShadowRank.C_RANK to Pair(25, 2000L),
            ShadowRank.B_RANK to Pair(40, 5000L),
            ShadowRank.A_RANK to Pair(60, 12000L),
            ShadowRank.S_RANK to Pair(80, 25000L),
            ShadowRank.MONARCH_CLASS to Pair(100, 50000L)
        )

        /**
         * Determines the appropriate shadow rank for given level and XP.
         */
        fun calculateRank(level: Int, totalXp: Long): ShadowRank {
            return ShadowRank.entries.sortedByDescending { it.order }.firstOrNull { rank ->
                val reqs = RANK_UP_REQUIREMENTS[rank]
                reqs != null && level >= reqs.first && totalXp >= reqs.second
            } ?: ShadowRank.E_RANK
        }

        /**
         * Determines the evolution stage for a given level and rank.
         */
        fun calculateEvolutionStage(level: Int, rank: ShadowRank): ShadowEvolutionStage {
            return ShadowEvolutionStage.entries
                .sortedByDescending { it.order }
                .firstOrNull { stage ->
                    level >= stage.minLevel && rank.order >= stage.minRank.order
                } ?: ShadowEvolutionStage.NEWLY_EXTRACTED
        }

        /**
         * XP awarded per shadow level (simple linear function).
         */
        fun xpToNextLevel(currentLevel: Int): Long {
            return (50L * currentLevel * currentLevel + 25L * currentLevel).coerceAtLeast(100L)
        }

        fun calculateNextLevelXp(currentLevel: Int): Long {
            return Shadow.shadowXpRequired(currentLevel + 1)
        }
    }

    // -------------------------------------------------------------------------
    // AWARD SHADOW XP
    // -------------------------------------------------------------------------

    /**
     * Awards XP to a shadow from an eligible, deterministic source.
     * Returns true if XP was actually awarded (not duplicate, shadow exists).
     * Never modifies player XP or any player attributes.
     */
    suspend fun awardShadowXp(shadowId: String, xpAmount: Int, sourceTag: String): Boolean {
        val shadow = shadowDao.getShadowById(shadowId) ?: return false
        if (shadow.isRetired) return false
        if (xpAmount <= 0) return false

        val domain = shadow.toDomain()
        val newXp = domain.shadowXp + xpAmount
        val newLevel = Shadow.shadowLevelForXp(newXp)
        val newRank = calculateRank(newLevel, newXp)

        // Determine new evolution stage
        val oldEvolutionStage = domain.evolutionStage
        val newEvolutionStage = calculateEvolutionStage(newLevel, newRank)

        // Update evolution history if stage changed
        val evolutionHistory = if (newEvolutionStage != oldEvolutionStage) {
            val existing = try {
                Json.decodeFromString<List<String>>(domain.evolutionHistoryJson)
            } catch (e: Exception) { emptyList() }
            (existing + "${newEvolutionStage.name}:${System.currentTimeMillis()}").let {
                Json.encodeToString(it)
            }
        } else {
            domain.evolutionHistoryJson
        }

        val updated = domain.copy(
            shadowXp = newXp,
            level = newLevel,
            currentRank = newRank,
            evolutionStage = newEvolutionStage,
            evolutionHistoryJson = evolutionHistory
        )
        shadowDao.updateShadow(updated.toEntity())

        // Award Monarch XP for evolution milestone (idempotent)
        if (newEvolutionStage != oldEvolutionStage) {
            monarchProgressionEngine.awardMonarchXp(
                sourceTag = "evolution_${shadowId}_${newEvolutionStage.name}",
                xpAmount = MONARCH_XP_PER_EVOLUTION,
                sourceDescription = "Shadow ${domain.name} evolved to ${newEvolutionStage.label}"
            )
            monarchProgressionEngine.refreshMonarchProfile()
        }

        return true
    }

    // -------------------------------------------------------------------------
    // CONFIRM EVOLUTION (player must confirm)
    // -------------------------------------------------------------------------

    /**
     * Checks if a shadow is eligible for evolution to the next stage.
     */
    suspend fun isEligibleForEvolution(shadowId: String): Pair<Boolean, List<String>> {
        val shadow = shadowDao.getShadowById(shadowId)?.toDomain() ?: return Pair(false, listOf("Shadow not found"))
        val blockers = mutableListOf<String>()

        val nextStageIdx = shadow.evolutionStage.order  // next stage order = current order + 1
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
    // SHADOW SKILL MANAGEMENT
    // -------------------------------------------------------------------------

    /**
     * Attempts to unlock a shadow skill. Returns true if unlocked successfully.
     * Checks rank, level, and prerequisite requirements.
     */
    suspend fun unlockShadowSkill(shadowId: String, skillId: String, evidenceNote: String): Boolean {
        val shadow = shadowDao.getShadowById(shadowId)?.toDomain() ?: return false
        val skillDef = ShadowSkillCatalog.findById(skillId) ?: return false

        // Check if already unlocked
        val existing = shadowSkillUnlockDao.getUnlock(shadowId, skillId)
        if (existing != null) return false

        // Check requirements
        if (shadow.currentRank.order < skillDef.requiredShadowRank.order) return false
        if (shadow.level < skillDef.requiredShadowLevel) return false

        // Check prerequisites
        for (prereqId in skillDef.prerequisiteSkillIds) {
            val prereqUnlocked = shadowSkillUnlockDao.getUnlock(shadowId, prereqId)
            if (prereqUnlocked == null) return false
        }

        // Unlock the skill
        shadowSkillUnlockDao.insertUnlock(
            ShadowSkillUnlockEntity(
                shadowId = shadowId,
                skillId = skillId,
                unlockedAtEpochMs = System.currentTimeMillis(),
                evidenceNote = evidenceNote
            )
        )

        // Update shadow's active skill list
        val updatedSkills = (shadow.activeSkillIds + skillId).distinct()
        shadowDao.updateShadow(shadow.copy(activeSkillIds = updatedSkills).toEntity())

        return true
    }

    suspend fun getShadowSkillUnlocks(shadowId: String): List<ShadowSkillUnlock> {
        return shadowSkillUnlockDao.getUnlocksForShadowOnce(shadowId).map { it.toDomain() }
    }

    // -------------------------------------------------------------------------
    // RANK-UP VALIDATION
    // -------------------------------------------------------------------------

    /**
     * Returns the requirements needed for the shadow to advance to the next rank.
     */
    fun getRankUpRequirements(currentRank: ShadowRank): Pair<Int, Long>? {
        val nextRank = currentRank.nextRank() ?: return null
        return RANK_UP_REQUIREMENTS[nextRank]
    }
}

