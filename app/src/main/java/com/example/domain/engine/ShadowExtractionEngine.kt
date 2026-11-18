package com.example.domain.engine

import androidx.room.withTransaction
import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.dao.ShadowDao
import com.example.data.local.dao.ShadowExtractionDao
import com.example.data.local.dao.ShadowMonarchProfileDao
import com.example.data.local.entity.toDomain
import com.example.data.local.entity.toEntity
import com.example.domain.model.DEFAULT_SHADOW_ACCOUNT_OWNER_ID
import com.example.domain.model.Shadow
import com.example.domain.model.ShadowClass
import com.example.domain.model.ShadowDeploymentStatus
import com.example.domain.model.ShadowEvolutionStage
import com.example.domain.model.ShadowExtractionEvent
import com.example.domain.model.ShadowExtractionOutcome
import com.example.domain.model.ShadowExtractionSourceType
import com.example.domain.model.ShadowRank
import com.example.domain.model.ShadowRarity
import com.example.domain.model.ShadowStats
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

// ============================================================================
// SHADOW EXTRACTION ENGINE (Account-Isolated & Transactionally Hardened)
// ============================================================================

class ShadowExtractionEngine(
    private val database: SoloLevelingDatabase,
    private val shadowDao: ShadowDao = database.shadowDao(),
    private val extractionDao: ShadowExtractionDao = database.shadowExtractionDao(),
    private val monarchProfileDao: ShadowMonarchProfileDao = database.shadowMonarchProfileDao(),
    private val monarchProgressionEngine: MonarchProgressionEngine,
    private val mutex: Mutex = Mutex()
) {

    companion object {
        const val MONARCH_XP_PER_EXTRACTION = 100

        fun buildDeterminismSeed(sourceType: ShadowExtractionSourceType, sourceEventId: String): Long {
            val raw = "${sourceType.name}::$sourceEventId"
            var seed = 0L
            raw.forEach { c -> seed = seed * 31L + c.code.toLong() }
            return seed.let { if (it < 0) -it else it }
        }

        fun selectClass(seed: Long): ShadowClass {
            val classes = ShadowClass.entries
            return classes[(seed % classes.size).toInt()]
        }

        fun selectRarity(seed: Long, sourceType: ShadowExtractionSourceType): ShadowRarity {
            val rarityIndex = when (sourceType) {
                ShadowExtractionSourceType.DUNGEON_COMPLETION -> {
                    val r = seed % 100
                    when {
                        r < 40 -> 0
                        r < 65 -> 1
                        r < 82 -> 2
                        r < 93 -> 3
                        r < 99 -> 4
                        else -> 5
                    }
                }
                ShadowExtractionSourceType.BOSS_BATTLE_COMPLETION -> {
                    val r = seed % 100
                    when {
                        r < 15 -> 0
                        r < 40 -> 1
                        r < 65 -> 2
                        r < 85 -> 3
                        r < 97 -> 4
                        else -> 5
                    }
                }
                ShadowExtractionSourceType.ACHIEVEMENT_UNLOCK -> {
                    val r = seed % 100
                    when {
                        r < 50 -> 0
                        r < 75 -> 1
                        r < 90 -> 2
                        r < 98 -> 3
                        else -> 4
                    }
                }
                else -> {
                    val r = seed % 100
                    when {
                        r < 55 -> 0
                        r < 78 -> 1
                        r < 93 -> 2
                        r < 99 -> 3
                        else -> 4
                    }
                }
            }
            return ShadowRarity.entries[rarityIndex.toInt().coerceIn(0, ShadowRarity.entries.size - 1)]
        }

        fun selectName(shadowClass: ShadowClass, seed: Long): String {
            val names = when (shadowClass) {
                ShadowClass.VANGUARD -> listOf(
                    "Iron Striker", "Siege Breaker", "Storm Vanguard", "Blade Sentinel",
                    "Assault Commander", "Fury Peak", "Vanguard Prime", "Shadow Spear"
                )
                ShadowClass.GUARDIAN -> listOf(
                    "Stone Bastion", "Iron Shield", "Eternal Guardian", "Ward Protector",
                    "Steadfast Aegis", "Silent Bulwark", "Citadel Shadow", "Enduring Wall"
                )
                ShadowClass.RANGER -> listOf(
                    "Precision Hawk", "Silent Arrow", "Shadow Scout", "Swift Ranger",
                    "Focus Eye", "Night Stalker", "Still Mark", "Keen Observer"
                )
                ShadowClass.ARCANIST -> listOf(
                    "Void Scholar", "Arcane Sage", "Shadow Mystic", "Knowledge Weaver",
                    "Mind Sculptor", "Rune Keeper", "Insight Oracle", "Deep Thinker"
                )
                ShadowClass.TACTICIAN -> listOf(
                    "Strategic Mastermind", "Shadow Planner", "Command Matrix", "Order Architect",
                    "Tactical Ghost", "Pattern Analyst", "Grid Commander", "Plan Weaver"
                )
                ShadowClass.SUPPORT -> listOf(
                    "Discipline Anchor", "Habit Keeper", "Routine Guardian", "Consistency Core",
                    "Shadow Steward", "Steady Flame", "Resolve Sentinel", "Persistence Echo"
                )
            }
            val idx = (seed % names.size).toInt().let { if (it < 0) -it else it }
            return names[idx]
        }

        fun calculateInitialStats(shadowClass: ShadowClass, rarity: ShadowRarity, seed: Long): ShadowStats {
            val base = 8
            val rarityBonus = ((rarity.baseStatMultiplier - 1f) * 10).toInt()
            val variance = (seed % 5).toInt()

            return when (shadowClass) {
                ShadowClass.VANGUARD -> ShadowStats(
                    power = base + rarityBonus + variance,
                    endurance = base + rarityBonus,
                    focus = base,
                    discipline = base,
                    wisdom = base - 2,
                    adaptability = base
                )
                ShadowClass.GUARDIAN -> ShadowStats(
                    power = base,
                    endurance = base + rarityBonus + variance,
                    focus = base,
                    discipline = base + rarityBonus,
                    wisdom = base,
                    adaptability = base + 2
                )
                ShadowClass.RANGER -> ShadowStats(
                    power = base,
                    endurance = base,
                    focus = base + rarityBonus + variance,
                    discipline = base + rarityBonus,
                    wisdom = base,
                    adaptability = base + 2
                )
                ShadowClass.ARCANIST -> ShadowStats(
                    power = base - 2,
                    endurance = base,
                    focus = base + rarityBonus,
                    discipline = base,
                    wisdom = base + rarityBonus + variance,
                    adaptability = base + 3
                )
                ShadowClass.TACTICIAN -> ShadowStats(
                    power = base,
                    endurance = base,
                    focus = base + rarityBonus,
                    discipline = base + rarityBonus,
                    wisdom = base + variance,
                    adaptability = base + 2
                )
                ShadowClass.SUPPORT -> ShadowStats(
                    power = base - 2,
                    endurance = base + rarityBonus,
                    focus = base,
                    discipline = base + rarityBonus + variance,
                    wisdom = base + 2,
                    adaptability = base + 3
                )
            }
        }
    }

    // -------------------------------------------------------------------------
    // SOURCE EVIDENCE VALIDATION
    // -------------------------------------------------------------------------

    suspend fun validateOriginatingSourceRecord(
        sourceType: ShadowExtractionSourceType,
        sourceEventId: String,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Boolean {
        if (sourceEventId.isBlank() || accountOwnerId.isBlank()) return false
        return try {
            when (sourceType) {
                ShadowExtractionSourceType.DUNGEON_COMPLETION -> {
                    val completionMatch = database.dungeonDao().getAllCompletionsOnce().any {
                        it.id.toString() == sourceEventId || it.dungeonId.toString() == sourceEventId
                    }
                    if (completionMatch) return true
                    database.dungeonDao().getAllInstancesOnce().any {
                        (it.id.toString() == sourceEventId || it.dungeonId.toString() == sourceEventId) &&
                            it.statusName.equals("CLEARED", ignoreCase = true)
                    }
                }
                ShadowExtractionSourceType.BOSS_BATTLE_COMPLETION -> {
                    val bossId = sourceEventId.toLongOrNull()
                    if (bossId != null) {
                        val instance = database.bossBattleDao().getBossInstanceById(bossId)
                        if (instance != null && (instance.statusName == "DEFEATED" || instance.statusName == "COMPLETED")) {
                            return true
                        }
                    }
                    database.bossBattleDao().getAllBossInstancesOnce().any {
                        it.id.toString() == sourceEventId &&
                            (it.statusName == "DEFEATED" || it.statusName == "COMPLETED")
                    }
                }
                ShadowExtractionSourceType.ACHIEVEMENT_UNLOCK -> {
                    database.achievementTitleDao().getAllAchievementUnlocksOnce().any {
                        it.id.toString() == sourceEventId || it.achievementId == sourceEventId
                    }
                }
                ShadowExtractionSourceType.PHYSICAL_MILESTONE -> {
                    val workoutMatch = database.workoutSessionDao().getWorkoutsSince(0).any {
                        it.id.toString() == sourceEventId
                    }
                    if (workoutMatch) return true
                    database.joggingSessionDao().getSessionsSince(0).any {
                        it.id.toString() == sourceEventId
                    }
                }
                ShadowExtractionSourceType.ACADEMIC_MILESTONE -> {
                    database.academicQuestDao().getAllAcademicQuestsOnce().any {
                        it.id.toString() == sourceEventId && (it.statusName == "COMPLETED" || it.statusName == "VERIFIED")
                    }
                }
                ShadowExtractionSourceType.DISCIPLINE_MILESTONE,
                ShadowExtractionSourceType.CONSISTENCY_MILESTONE -> {
                    database.habitDao().getCompletionsSinceOnce(0).any {
                        it.id.toString() == sourceEventId || "${it.habitId}_${it.dateEpochDay}" == sourceEventId
                    }
                }
                ShadowExtractionSourceType.MONARCH_QUEST -> {
                    database.shadowQuestProgressDao()
                        .getQuestProgressForOwner(accountOwnerId, sourceEventId)?.isCompleted == true
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    // -------------------------------------------------------------------------
    // EVALUATE EXTRACTION ELIGIBILITY (Account-Isolated)
    // -------------------------------------------------------------------------

    suspend fun evaluateExtractionEligibility(
        sourceType: ShadowExtractionSourceType,
        sourceEventId: String,
        evidenceSummary: String,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): ShadowExtractionEvent {
        val owner = accountOwnerId.trim().ifBlank { DEFAULT_SHADOW_ACCOUNT_OWNER_ID }
        val extractionId = UUID.randomUUID().toString()
        val seed = buildDeterminismSeed(sourceType, sourceEventId)

        if (accountOwnerId.isBlank() ||
            sourceEventId.isBlank() ||
            evidenceSummary.isBlank() ||
            !validateOriginatingSourceRecord(sourceType, sourceEventId, owner)
        ) {
            return ShadowExtractionEvent(
                extractionId = extractionId,
                accountOwnerId = owner,
                sourceType = sourceType,
                sourceEventId = sourceEventId,
                outcome = ShadowExtractionOutcome.SOURCE_INVALID,
                extractedShadowId = null,
                shadowName = null,
                shadowClass = null,
                shadowRarity = null,
                determinismSeed = seed,
                evidenceSummary = evidenceSummary
            )
        }

        val existing = extractionDao.getExtractionBySourceForOwner(owner, sourceType.name, sourceEventId)
        if (existing != null) {
            return ShadowExtractionEvent(
                extractionId = existing.extractionId,
                accountOwnerId = owner,
                sourceType = sourceType,
                sourceEventId = sourceEventId,
                outcome = ShadowExtractionOutcome.ALREADY_CLAIMED,
                extractedShadowId = existing.extractedShadowId,
                shadowName = existing.shadowName,
                shadowClass = existing.shadowClassName?.let { runCatching { ShadowClass.valueOf(it) }.getOrNull() },
                shadowRarity = existing.shadowRarityName?.let { runCatching { ShadowRarity.valueOf(it) }.getOrNull() },
                determinismSeed = seed,
                evidenceSummary = evidenceSummary
            )
        }

        val profile = monarchProgressionEngine.getOrCreateProfile(owner)
        val activeShadows = shadowDao.getActiveShadowCountForOwner(owner)
        if (activeShadows >= profile.armyCapacity) {
            return ShadowExtractionEvent(
                extractionId = extractionId,
                accountOwnerId = owner,
                sourceType = sourceType,
                sourceEventId = sourceEventId,
                outcome = ShadowExtractionOutcome.ARMY_CAPACITY_REACHED,
                extractedShadowId = null,
                shadowName = null,
                shadowClass = null,
                shadowRarity = null,
                determinismSeed = seed,
                evidenceSummary = evidenceSummary
            )
        }

        val shadowClass = selectClass(seed)
        val shadowRarity = selectRarity(seed, sourceType)
        val shadowName = selectName(shadowClass, seed)

        return ShadowExtractionEvent(
            extractionId = extractionId,
            accountOwnerId = owner,
            sourceType = sourceType,
            sourceEventId = sourceEventId,
            outcome = ShadowExtractionOutcome.PENDING_CONFIRMATION,
            extractedShadowId = null,
            shadowName = shadowName,
            shadowClass = shadowClass,
            shadowRarity = shadowRarity,
            determinismSeed = seed,
            evidenceSummary = evidenceSummary
        )
    }

    // -------------------------------------------------------------------------
    // CONFIRM EXTRACTION (Player Confirmation, Atomic & Owner-Verified)
    // -------------------------------------------------------------------------

    suspend fun confirmExtraction(
        pendingEvent: ShadowExtractionEvent,
        accountOwnerId: String = pendingEvent.accountOwnerId
    ): Result<ShadowExtractionEvent> {
        return runCatching {
            val owner = accountOwnerId.trim()
            require(owner.isNotBlank()) { "accountOwnerId must not be blank" }
            require(pendingEvent.accountOwnerId == owner) {
                "Cross-account extraction confirmation rejected for owner $owner"
            }
            if (pendingEvent.outcome != ShadowExtractionOutcome.PENDING_CONFIRMATION) {
                throw IllegalStateException("Cannot confirm extraction with outcome ${pendingEvent.outcome}")
            }
            if (pendingEvent.shadowClass == null || pendingEvent.shadowRarity == null || pendingEvent.shadowName == null) {
                throw IllegalStateException("Missing shadow parameters in pending event")
            }

            mutex.withLock {
                database.withTransaction {
                    if (!validateOriginatingSourceRecord(pendingEvent.sourceType, pendingEvent.sourceEventId, owner)) {
                        return@withTransaction pendingEvent.copy(outcome = ShadowExtractionOutcome.SOURCE_INVALID)
                    }

                    val profile = monarchProgressionEngine.getOrCreateProfileLocked(owner)
                    val activeShadows = shadowDao.getActiveShadowCountForOwner(owner)
                    if (activeShadows >= profile.armyCapacity) {
                        return@withTransaction pendingEvent.copy(outcome = ShadowExtractionOutcome.ARMY_CAPACITY_REACHED)
                    }

                    val alreadyExtracted = extractionDao.getExtractionBySourceForOwner(
                        owner,
                        pendingEvent.sourceType.name,
                        pendingEvent.sourceEventId
                    )
                    if (alreadyExtracted != null) {
                        return@withTransaction pendingEvent.copy(
                            outcome = ShadowExtractionOutcome.ALREADY_CLAIMED,
                            extractedShadowId = alreadyExtracted.extractedShadowId
                        )
                    }

                    val shadowId = "shadow_${pendingEvent.extractionId.take(12)}_${UUID.randomUUID().toString().take(4)}"
                    val seed = pendingEvent.determinismSeed
                    val stats = calculateInitialStats(pendingEvent.shadowClass, pendingEvent.shadowRarity, seed)

                    val newShadow = Shadow(
                        shadowId = shadowId,
                        accountOwnerId = owner,
                        name = pendingEvent.shadowName,
                        shadowClass = pendingEvent.shadowClass,
                        rarity = pendingEvent.shadowRarity,
                        currentRank = ShadowRank.E_RANK,
                        level = 1,
                        shadowXp = 0L,
                        stats = stats,
                        evolutionStage = ShadowEvolutionStage.NEWLY_EXTRACTED,
                        passiveAbility = pendingEvent.shadowClass.passiveAbilityName,
                        activeSkillIds = emptyList(),
                        recruitmentSource = pendingEvent.evidenceSummary,
                        extractionEventId = pendingEvent.extractionId,
                        extractedAtEpochMs = System.currentTimeMillis(),
                        evolutionHistoryJson = "[]",
                        deploymentStatus = ShadowDeploymentStatus.IDLE,
                        equippedFormationId = null,
                        isRetired = false
                    )

                    val shadowInsertRow = shadowDao.insertShadow(newShadow.toEntity())
                    if (shadowInsertRow <= 0L) {
                        throw IllegalStateException("Failed to insert extracted shadow (duplicate shadowId conflict)")
                    }

                    val confirmedEvent = pendingEvent.copy(
                        accountOwnerId = owner,
                        extractedShadowId = shadowId,
                        outcome = ShadowExtractionOutcome.SUCCESS,
                        isConfirmed = true,
                        monarchXpAwarded = MONARCH_XP_PER_EXTRACTION
                    )
                    val insertedRow = extractionDao.insertExtraction(confirmedEvent.toEntity())
                    if (insertedRow <= 0L) {
                        throw IllegalStateException("Failed to insert extraction record: unique index conflict. Rolling back transaction.")
                    }

                    monarchProgressionEngine.awardMonarchXpLocked(
                        accountOwnerId = owner,
                        sourceTag = "extraction_${pendingEvent.extractionId}",
                        xpAmount = MONARCH_XP_PER_EXTRACTION,
                        sourceDescription = "Shadow extracted: ${pendingEvent.shadowName}"
                    )
                    monarchProgressionEngine.refreshMonarchProfileLocked(owner)

                    confirmedEvent
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // QUERY HELPERS
    // -------------------------------------------------------------------------

    suspend fun getExtractionHistory(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): List<ShadowExtractionEvent> {
        return extractionDao.getAllExtractionsForOwnerOnce(accountOwnerId).map { it.toDomain() }
    }

    suspend fun getConfirmedExtractionCount(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Int {
        return extractionDao.getConfirmedExtractionCountForOwner(accountOwnerId)
    }
}
