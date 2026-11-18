package com.example.domain.engine

import androidx.room.withTransaction
import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.dao.MonarchSkillUnlockDao
import com.example.data.local.dao.ShadowDao
import com.example.data.local.dao.ShadowMonarchProfileDao
import com.example.data.local.dao.ShadowQuestProgressDao
import com.example.data.local.dao.ShadowResourceDao
import com.example.data.local.dao.ShadowXpTraceDao
import com.example.data.local.entity.MonarchSkillUnlockEntity
import com.example.data.local.entity.ShadowQuestProgressEntity
import com.example.data.local.entity.ShadowResourceTransactionEntity
import com.example.data.local.entity.ShadowXpTraceEntity
import com.example.data.local.entity.toDomain
import com.example.data.local.entity.toEntity
import com.example.domain.model.DEFAULT_SHADOW_ACCOUNT_OWNER_ID
import com.example.domain.model.MonarchRank
import com.example.domain.model.MonarchSkillCatalog
import com.example.domain.model.MonarchSkillIds
import com.example.domain.model.ShadowMonarchProfile
import com.example.domain.model.ShadowQuestCatalog
import com.example.domain.model.ShadowRank
import com.example.domain.model.ShadowResourceTransactionType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

// ============================================================================
// MONARCH PROGRESSION ENGINE (Account-Isolated & Transactionally Hardened)
// Controls Monarch XP, level, rank, and authority independently from player XP.
// Never mutates PlayerProfile or awards player / momentum XP.
// ============================================================================

class MonarchProgressionEngine(
    private val monarchProfileDao: ShadowMonarchProfileDao,
    private val monarchSkillUnlockDao: MonarchSkillUnlockDao,
    private val shadowDao: ShadowDao,
    private val shadowResourceDao: ShadowResourceDao,
    private val shadowQuestProgressDao: ShadowQuestProgressDao,
    private val shadowXpTraceDao: ShadowXpTraceDao? = null,
    private val database: SoloLevelingDatabase? = null,
    private val mutex: Mutex = Mutex()
) {

    companion object {
        const val BASE_ARMY_CAPACITY = 10
        const val ARMY_EXPANSION_PER_UNLOCK = 5
        const val BASE_AUTHORITY_PER_LEVEL = 3
        const val TRACE_TYPE_MONARCH_XP = "MONARCH_XP"

        fun calculateNextLevelXp(currentLevel: Int): Long {
            return ShadowMonarchProfile.monarchXpRequired(currentLevel + 1)
        }

        private fun monarchXpKey(sourceTag: String): String = "monarch_xp_$sourceTag"
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

    /**
     * Awards Monarch XP from a deterministic eligible source under account isolation.
     * Returns true if XP was awarded (not a duplicate).
     * Never modifies player XP, rank, or any of the seven attributes.
     */
    suspend fun awardMonarchXp(
        sourceTag: String,
        xpAmount: Int,
        sourceDescription: String,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Boolean {
        val owner = accountOwnerId.trim()
        if (owner.isBlank() || sourceTag.isBlank() || xpAmount <= 0) return false
        return withLockedTransaction {
            awardMonarchXpLocked(
                accountOwnerId = owner,
                sourceTag = sourceTag.trim(),
                xpAmount = xpAmount,
                sourceDescription = sourceDescription
            )
        }
    }

    internal suspend fun awardMonarchXpLocked(
        accountOwnerId: String,
        sourceTag: String,
        xpAmount: Int,
        sourceDescription: String
    ): Boolean {
        if (accountOwnerId.isBlank() || sourceTag.isBlank() || xpAmount <= 0) return false
        val current = getOrCreateProfileLocked(accountOwnerId)

        val now = System.currentTimeMillis()
        val traceDao = shadowXpTraceDao
        if (traceDao != null) {
            if (traceDao.findTrace(accountOwnerId, TRACE_TYPE_MONARCH_XP, sourceTag) != null) {
                return false
            }
            val inserted = traceDao.insertTraceIgnore(
                ShadowXpTraceEntity(
                    traceId = UUID.randomUUID().toString(),
                    accountOwnerId = accountOwnerId,
                    traceType = TRACE_TYPE_MONARCH_XP,
                    sourceTag = sourceTag,
                    targetEntityId = "",
                    xpAmount = xpAmount,
                    description = sourceDescription,
                    timestampEpochMs = now
                )
            )
            if (inserted == -1L) return false
        } else {
            val idempotencyKey = monarchXpKey(sourceTag)
            if (shadowResourceDao.getTransactionBySourceEventForOwner(accountOwnerId, idempotencyKey, "_monarch_xp_trace_") != null) {
                return false
            }
            val inserted = shadowResourceDao.insertTransaction(
                ShadowResourceTransactionEntity(
                    transactionId = UUID.randomUUID().toString(),
                    accountOwnerId = accountOwnerId,
                    resourceTypeName = "_monarch_xp_trace_",
                    amount = xpAmount,
                    transactionTypeName = ShadowResourceTransactionType.EARNED.name,
                    sourceEventId = idempotencyKey,
                    sourceDescription = sourceDescription,
                    balanceAfter = 0,
                    timestampEpochMs = now
                )
            )
            if (inserted == -1L) return false
        }

        val newXp = current.monarchXp + xpAmount
        val newLevel = ShadowMonarchProfile.monarchLevelForXp(newXp)
        val newRank = MonarchRank.forLevel(newLevel)
        val newAuthority = current.monarchAuthority +
            (newLevel - current.monarchLevel).coerceAtLeast(0) * BASE_AUTHORITY_PER_LEVEL

        val updated = current.copy(
            accountOwnerId = accountOwnerId,
            monarchXp = newXp,
            monarchLevel = newLevel,
            monarchRank = newRank,
            monarchAuthority = newAuthority.coerceAtLeast(current.monarchAuthority),
            updatedAtEpochMs = now
        )
        monarchProfileDao.upsertProfile(updated.toEntity())

        if (newLevel >= 1) {
            tryUnlockMonarchSkillLocked(
                accountOwnerId = accountOwnerId,
                skillId = MonarchSkillIds.SHADOW_EXTRACTION,
                evidenceNote = "Monarch Level 1 — Awakening"
            )
        }

        return true
    }

    /**
     * Attempts to unlock a Monarch skill if prerequisites and requirements are met.
     * Enforces account ownership and verifies requirements against persisted DB state.
     */
    suspend fun tryUnlockMonarchSkill(
        skillId: String,
        profile: ShadowMonarchProfile,
        evidenceNote: String,
        accountOwnerId: String = profile.accountOwnerId
    ): Boolean {
        val owner = accountOwnerId.trim()
        if (owner.isBlank() || profile.accountOwnerId != owner) return false
        return withLockedTransaction {
            tryUnlockMonarchSkillLocked(owner, skillId, evidenceNote)
        }
    }

    internal suspend fun tryUnlockMonarchSkillLocked(
        accountOwnerId: String,
        skillId: String,
        evidenceNote: String
    ): Boolean {
        val existing = monarchSkillUnlockDao.getMonarchSkillUnlockForOwner(accountOwnerId, skillId)
        if (existing != null) return false

        val definition = MonarchSkillCatalog.findById(skillId) ?: return false
        val current = getOrCreateProfileLocked(accountOwnerId)

        if (current.monarchLevel < definition.requiredMonarchLevel) return false
        if (current.monarchAuthority < definition.requiredAuthority) return false

        if (definition.prerequisiteSkillId != null) {
            val prereqUnlocked = monarchSkillUnlockDao.getMonarchSkillUnlockForOwner(
                accountOwnerId,
                definition.prerequisiteSkillId
            )
            if (prereqUnlocked == null) return false
        }

        val now = System.currentTimeMillis()
        val inserted = monarchSkillUnlockDao.insertMonarchSkillUnlock(
            MonarchSkillUnlockEntity(
                accountOwnerId = accountOwnerId,
                skillId = skillId,
                unlockedAtEpochMs = now,
                evidenceNote = evidenceNote
            )
        )
        if (inserted <= 0L) return false

        val allUnlocked = monarchSkillUnlockDao.getAllMonarchSkillUnlocksForOwnerOnce(accountOwnerId)
            .map { it.skillId }
            .distinct()
        val updatedCapacity = calculateArmyCapacity(allUnlocked)
        monarchProfileDao.upsertProfile(
            current.copy(
                accountOwnerId = accountOwnerId,
                unlockedMonarchSkillIds = allUnlocked,
                armyCapacity = updatedCapacity,
                updatedAtEpochMs = now
            ).toEntity()
        )
        return true
    }

    /**
     * Recalculates and refreshes the Monarch profile's computed fields for the given account.
     */
    suspend fun refreshMonarchProfile(accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID) {
        val owner = accountOwnerId.trim()
        if (owner.isBlank()) return
        withLockedTransaction {
            refreshMonarchProfileLocked(owner)
        }
    }

    internal suspend fun refreshMonarchProfileLocked(accountOwnerId: String) {
        val profile = getOrCreateProfileLocked(accountOwnerId)
        val activeShadows = shadowDao.getActiveShadowsForOwnerOnce(accountOwnerId)
        val allShadows = shadowDao.getAllShadowsForOwnerOnce(accountOwnerId)
        val highestRank = allShadows.mapNotNull { entity ->
            runCatching { ShadowRank.valueOf(entity.currentRankName) }.getOrNull()
        }.maxByOrNull { it.order } ?: ShadowRank.E_RANK

        val totalEvolutions = allShadows.sumOf { entity ->
            try {
                kotlinx.serialization.json.Json.decodeFromString<List<String>>(entity.evolutionHistoryJson).size
            } catch (_: Exception) { 0 }
        }

        val powerRating = activeShadows.sumOf { entity ->
            (entity.statPower + entity.statEndurance + entity.statFocus +
                entity.statDiscipline + entity.statWisdom + entity.statAdaptability).toLong() * entity.level
        }

        val unlockedSkills = monarchSkillUnlockDao.getAllMonarchSkillUnlocksForOwnerOnce(accountOwnerId)
            .map { it.skillId }
            .distinct()

        monarchProfileDao.upsertProfile(
            profile.copy(
                accountOwnerId = accountOwnerId,
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

    suspend fun getOrCreateProfile(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): ShadowMonarchProfile {
        val owner = accountOwnerId.trim().also {
            require(it.isNotBlank()) { "accountOwnerId must not be blank" }
        }
        return withLockedTransaction {
            getOrCreateProfileLocked(owner)
        }
    }

    internal suspend fun getOrCreateProfileLocked(accountOwnerId: String): ShadowMonarchProfile {
        val existing = monarchProfileDao.getProfileForOwnerOnce(accountOwnerId)
        if (existing != null) return existing.toDomain()

        val now = System.currentTimeMillis()
        val initial = ShadowMonarchProfile(
            accountOwnerId = accountOwnerId,
            unlockedMonarchSkillIds = listOf(MonarchSkillIds.SHADOW_EXTRACTION),
            updatedAtEpochMs = now
        )
        monarchProfileDao.insertProfileIgnore(initial.toEntity())
        monarchSkillUnlockDao.insertMonarchSkillUnlock(
            MonarchSkillUnlockEntity(
                accountOwnerId = accountOwnerId,
                skillId = MonarchSkillIds.SHADOW_EXTRACTION,
                unlockedAtEpochMs = now,
                evidenceNote = "Shadow Extraction unlocked at Monarch Level 1"
            )
        )
        return (monarchProfileDao.getProfileForOwnerOnce(accountOwnerId) ?: initial.toEntity()).toDomain()
    }

    private fun calculateArmyCapacity(unlockedSkillIds: List<String>): Int {
        val expansionCount = if (MonarchSkillIds.ARMY_EXPANSION in unlockedSkillIds) 1 else 0
        return BASE_ARMY_CAPACITY + (expansionCount * ARMY_EXPANSION_PER_UNLOCK)
    }

    suspend fun checkAndUpdateQuestProgress(
        shadowCount: Int,
        deployedCount: Int,
        evolutionCount: Int,
        aRankShadows: Int,
        commanderShadows: Int,
        dungeonCompletions: Int,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ) {
        val owner = accountOwnerId.trim()
        if (owner.isBlank()) return
        withLockedTransaction {
            checkAndUpdateQuestProgressLocked(
                accountOwnerId = owner,
                shadowCount = shadowCount,
                deployedCount = deployedCount,
                evolutionCount = evolutionCount,
                aRankShadows = aRankShadows,
                commanderShadows = commanderShadows,
                dungeonCompletions = dungeonCompletions
            )
        }
    }

    internal suspend fun checkAndUpdateQuestProgressLocked(
        accountOwnerId: String,
        shadowCount: Int,
        deployedCount: Int,
        evolutionCount: Int,
        aRankShadows: Int,
        commanderShadows: Int,
        dungeonCompletions: Int
    ) {
        getOrCreateProfileLocked(accountOwnerId)
        val allProgress = shadowQuestProgressDao.getAllQuestProgressForOwnerOnce(accountOwnerId)
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
                        accountOwnerId = accountOwnerId,
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
                        completedAtEpochMs = if (isNowComplete && existing.completedAtEpochMs == null) {
                            System.currentTimeMillis()
                        } else {
                            existing.completedAtEpochMs
                        }
                    )
                )
            }
        }
    }
}
