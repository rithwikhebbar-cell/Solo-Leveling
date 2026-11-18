package com.example.domain.engine

import com.example.data.local.dao.MonarchSkillUnlockDao
import com.example.data.local.dao.ShadowDao
import com.example.data.local.dao.ShadowExtractionDao
import com.example.data.local.dao.ShadowMonarchProfileDao
import com.example.data.local.dao.ShadowQuestProgressDao
import com.example.data.local.dao.ShadowResourceDao
import com.example.data.local.entity.MonarchSkillUnlockEntity
import com.example.data.local.entity.ShadowMonarchProfileEntity
import com.example.data.local.entity.ShadowQuestProgressEntity
import com.example.data.local.entity.ShadowResourceTransactionEntity
import com.example.data.local.entity.toDomain
import com.example.data.local.entity.toEntity
import com.example.domain.model.MonarchRank
import com.example.domain.model.MonarchSkillCatalog
import com.example.domain.model.MonarchSkillIds
import com.example.domain.model.Shadow
import com.example.domain.model.ShadowArmyAnalytics
import com.example.domain.model.ShadowClass
import com.example.domain.model.ShadowMonarchProfile
import com.example.domain.model.ShadowQuestCatalog
import com.example.domain.model.ShadowRank
import com.example.domain.model.ShadowResourceTransaction
import com.example.domain.model.ShadowResourceTransactionType
import com.example.domain.model.ShadowResourceType
import java.util.UUID

// ============================================================================
// MONARCH PROGRESSION ENGINE
// Controls Monarch XP, level, rank, and authority independently from player XP
// ============================================================================

class MonarchProgressionEngine(
    private val monarchProfileDao: ShadowMonarchProfileDao,
    private val monarchSkillUnlockDao: MonarchSkillUnlockDao,
    private val shadowDao: ShadowDao,
    private val shadowResourceDao: ShadowResourceDao,
    private val shadowQuestProgressDao: ShadowQuestProgressDao
) {

    companion object {
        const val BASE_ARMY_CAPACITY = 10
        const val ARMY_EXPANSION_PER_UNLOCK = 5
        const val BASE_AUTHORITY_PER_LEVEL = 3

        fun calculateNextLevelXp(currentLevel: Int): Long {
            return ShadowMonarchProfile.monarchXpRequired(currentLevel + 1)
        }

        // Idempotency guard: Monarch XP events are keyed to prevent double-award
        private fun monarchXpKey(sourceTag: String): String = "monarch_xp_$sourceTag"
    }

    /**
     * Awards Monarch XP from a deterministic eligible source.
     * Returns true if XP was awarded (not a duplicate).
     * Never modifies player XP, rank, or any of the seven attributes.
     */
    suspend fun awardMonarchXp(
        sourceTag: String,
        xpAmount: Int,
        sourceDescription: String
    ): Boolean {
        // Idempotency: check if this exact source event was already rewarded
        val idempotencyKey = monarchXpKey(sourceTag)
        val existing = shadowResourceDao.getTransactionBySourceEvent(idempotencyKey, ShadowResourceType.SHADOW_ESSENCE.name)
        if (existing != null) return false // already processed

        val current = getOrCreateProfile()
        val newXp = current.monarchXp + xpAmount.coerceAtLeast(0)
        val newLevel = ShadowMonarchProfile.monarchLevelForXp(newXp)
        val newRank = MonarchRank.forLevel(newLevel)
        val newAuthority = current.monarchAuthority + (newLevel - current.monarchLevel).coerceAtLeast(0) * BASE_AUTHORITY_PER_LEVEL

        val updated = current.copy(
            monarchXp = newXp,
            monarchLevel = newLevel,
            monarchRank = newRank,
            monarchAuthority = newAuthority.coerceAtLeast(current.monarchAuthority),
            updatedAtEpochMs = System.currentTimeMillis()
        )
        monarchProfileDao.upsertProfile(updated.toEntity())

        // Record the idempotency marker in the resource ledger as a trace
        // (not awarding shadow resources; this is Monarch XP only)
        // Using a zero-amount trace entry for idempotency tracking
        val traceTransaction = ShadowResourceTransactionEntity(
            transactionId = UUID.randomUUID().toString(),
            resourceTypeName = "_monarch_xp_trace_",
            amount = xpAmount,
            transactionTypeName = ShadowResourceTransactionType.EARNED.name,
            sourceEventId = idempotencyKey,
            sourceDescription = sourceDescription,
            balanceAfter = 0,
            timestampEpochMs = System.currentTimeMillis()
        )
        shadowResourceDao.insertTransaction(traceTransaction)

        // Auto-unlock SHADOW_EXTRACTION skill at level 1 if not already unlocked
        if (newLevel >= 1) {
            tryUnlockMonarchSkill(MonarchSkillIds.SHADOW_EXTRACTION, updated, "Monarch Level 1 — Awakening")
        }

        return true
    }

    /**
     * Attempts to unlock a Monarch skill if prerequisites and requirements are met.
     * Does nothing if already unlocked or requirements not satisfied.
     */
    suspend fun tryUnlockMonarchSkill(skillId: String, profile: ShadowMonarchProfile, evidenceNote: String): Boolean {
        val existing = monarchSkillUnlockDao.getMonarchSkillUnlock(skillId)
        if (existing != null) return false // already unlocked

        val definition = MonarchSkillCatalog.findById(skillId) ?: return false

        // Check level requirement
        if (profile.monarchLevel < definition.requiredMonarchLevel) return false

        // Check authority requirement
        if (profile.monarchAuthority < definition.requiredAuthority) return false

        // Check prerequisite skill
        if (definition.prerequisiteSkillId != null) {
            val prereqUnlocked = monarchSkillUnlockDao.getMonarchSkillUnlock(definition.prerequisiteSkillId)
            if (prereqUnlocked == null) return false
        }

        monarchSkillUnlockDao.insertMonarchSkillUnlock(
            MonarchSkillUnlockEntity(
                skillId = skillId,
                unlockedAtEpochMs = System.currentTimeMillis(),
                evidenceNote = evidenceNote
            )
        )

        // Update profile to reflect newly unlocked skill
        val current = getOrCreateProfile()
        val newSkillIds = (current.unlockedMonarchSkillIds + skillId).distinct()
        val updatedCapacity = calculateArmyCapacity(newSkillIds)
        monarchProfileDao.upsertProfile(
            current.copy(
                unlockedMonarchSkillIds = newSkillIds,
                armyCapacity = updatedCapacity,
                updatedAtEpochMs = System.currentTimeMillis()
            ).toEntity()
        )
        return true
    }

    /**
     * Recalculates and refreshes the Monarch profile's computed fields.
     * Calls this after extraction, evolution, or deployment changes.
     */
    suspend fun refreshMonarchProfile() {
        val profile = getOrCreateProfile()
        val activeShadows = shadowDao.getActiveShadowsOnce()
        val allShadows = shadowDao.getAllShadowsOnce()
        val highestRank = allShadows.mapNotNull { entity ->
            runCatching { ShadowRank.valueOf(entity.currentRankName) }.getOrNull()
        }.maxByOrNull { it.order } ?: ShadowRank.E_RANK

        val totalEvolutions = allShadows.sumOf { entity ->
            try {
                val history = kotlinx.serialization.json.Json.decodeFromString<List<String>>(entity.evolutionHistoryJson)
                history.size
            } catch (e: Exception) { 0 }
        }

        val powerRating = activeShadows.sumOf { entity ->
            (entity.statPower + entity.statEndurance + entity.statFocus +
                    entity.statDiscipline + entity.statWisdom + entity.statAdaptability).toLong() * entity.level
        }

        val unlockedSkills = monarchSkillUnlockDao.getAllMonarchSkillUnlocksOnce().map { it.skillId }

        monarchProfileDao.upsertProfile(
            profile.copy(
                activeShadowCount = activeShadows.size,
                totalShadowsRecruited = allShadows.size,
                highestShadowRank = highestRank,
                totalEvolutions = totalEvolutions,
                shadowArmyPowerRating = powerRating,
                unlockedMonarchSkillIds = unlockedSkills,
                armyCapacity = calculateArmyCapacity(unlockedSkills),
                updatedAtEpochMs = System.currentTimeMillis()
            ).toEntity()
        )
    }

    suspend fun getOrCreateProfile(): ShadowMonarchProfile {
        val existing = monarchProfileDao.getProfileOnce()
        if (existing != null) return existing.toDomain()

        val initial = ShadowMonarchProfile()
        monarchProfileDao.upsertProfile(initial.toEntity())
        // Auto-unlock Shadow Extraction at level 1
        monarchSkillUnlockDao.insertMonarchSkillUnlock(
            MonarchSkillUnlockEntity(
                skillId = MonarchSkillIds.SHADOW_EXTRACTION,
                unlockedAtEpochMs = System.currentTimeMillis(),
                evidenceNote = "Shadow Extraction unlocked at Monarch Level 1"
            )
        )
        return initial
    }

    private fun calculateArmyCapacity(unlockedSkillIds: List<String>): Int {
        val expansionCount = if (MonarchSkillIds.ARMY_EXPANSION in unlockedSkillIds) 1 else 0
        return BASE_ARMY_CAPACITY + (expansionCount * ARMY_EXPANSION_PER_UNLOCK)
    }

    suspend fun checkAndUpdateQuestProgress(shadowCount: Int, deployedCount: Int, evolutionCount: Int, aRankShadows: Int, commanderShadows: Int, dungeonCompletions: Int) {
        val allProgress = shadowQuestProgressDao.getAllQuestProgressOnce()
        val progressMap = allProgress.associateBy { it.questId }

        ShadowQuestCatalog.quests.forEach { questDef ->
            val existing = progressMap[questDef.questId]
            if (existing?.isCompleted == true) return@forEach

            val newValue = when (questDef.questId) {
                "sq_first_extraction", "sq_growing_army", "sq_ten_shadows" -> shadowCount
                "sq_first_deployment" -> deployedCount
                "sq_first_evolution" -> evolutionCount
                "sq_elite_command" -> aRankShadows
                "sq_monarch_trial" -> if (dungeonCompletions >= 1 && commanderShadows >= 1) 1 else 0
                else -> existing?.currentValue ?: 0
            }

            val isNowComplete = newValue >= questDef.targetValue

            if (existing == null) {
                shadowQuestProgressDao.insertQuestProgress(
                    ShadowQuestProgressEntity(
                        questId = questDef.questId,
                        currentValue = newValue,
                        isCompleted = isNowComplete,
                        rewardClaimedAtEpochMs = null,
                        completedAtEpochMs = if (isNowComplete) System.currentTimeMillis() else null
                    )
                )
            } else if (newValue != existing.currentValue || isNowComplete != existing.isCompleted) {
                shadowQuestProgressDao.updateQuestProgress(
                    existing.copy(
                        currentValue = newValue,
                        isCompleted = isNowComplete,
                        completedAtEpochMs = if (isNowComplete && existing.completedAtEpochMs == null) System.currentTimeMillis() else existing.completedAtEpochMs
                    )
                )
            }
        }
    }
}

