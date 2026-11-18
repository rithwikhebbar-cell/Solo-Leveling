package com.example.domain.engine

import androidx.room.withTransaction
import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.dao.ShadowDao
import com.example.data.local.dao.ShadowExtractionDao
import com.example.data.local.dao.ShadowMonarchProfileDao
import com.example.data.local.entity.ShadowEntity
import com.example.data.local.entity.ShadowExtractionEntity
import com.example.data.local.entity.toDomain
import com.example.data.local.entity.toEntity
import com.example.data.local.entity.toEntity
import com.example.domain.model.Shadow
import com.example.domain.model.ShadowClass
import com.example.domain.model.ShadowDeploymentStatus
import com.example.domain.model.ShadowEvolutionStage
import com.example.domain.model.ShadowExtractionEvent
import com.example.domain.model.ShadowExtractionOutcome
import com.example.domain.model.ShadowExtractionSourceType
import com.example.domain.model.ShadowMonarchProfile
import com.example.domain.model.ShadowRank
import com.example.domain.model.ShadowRarity
import com.example.domain.model.ShadowStats
import java.util.UUID

// ============================================================================
// SHADOW EXTRACTION ENGINE
// ============================================================================
//
// SAFETY GUARANTEES:
//  - Source evidence validated against actual persisted records in originating DAOs.
//  - Each source event ID may only produce ONE extraction (composite unique index on DB level).
//  - Extraction uses a deterministic algorithm seeded from source event data.
//  - No randomness in rarity/class determination for the same event.
//  - Army capacity is validated before extraction and within the transaction.
//  - Extraction occurs inside an atomic Room database transaction (database.withTransaction).
//  - Extraction never awards player XP or modifies player attributes.
//  - Extraction requires explicit player confirmation before persisting the shadow.
//
// DETERMINISM RULE:
//  Given the same sourceType + sourceEventId, the algorithm ALWAYS produces
//  the same shadow class, rarity, and name. No uncontrolled entropy.
// ============================================================================

class ShadowExtractionEngine(
    private val database: SoloLevelingDatabase,
    private val shadowDao: ShadowDao = database.shadowDao(),
    private val extractionDao: ShadowExtractionDao = database.shadowExtractionDao(),
    private val monarchProfileDao: ShadowMonarchProfileDao = database.shadowMonarchProfileDao(),
    private val monarchProgressionEngine: MonarchProgressionEngine
) {

    companion object {
        // XP awarded to Monarch for successful extraction confirmation
        const val MONARCH_XP_PER_EXTRACTION = 100

        /**
         * Deterministic seed from source event identifiers.
         * Same inputs always produce the same seed.
         */
        fun buildDeterminismSeed(sourceType: ShadowExtractionSourceType, sourceEventId: String): Long {
            val raw = "${sourceType.name}::$sourceEventId"
            var seed = 0L
            raw.forEach { c -> seed = seed * 31L + c.code.toLong() }
            return seed.let { if (it < 0) -it else it }
        }

        /**
         * Deterministic class selection from seed.
         */
        fun selectClass(seed: Long): ShadowClass {
            val classes = ShadowClass.entries
            return classes[(seed % classes.size).toInt()]
        }

        /**
         * Deterministic rarity selection based on source type and seed.
         * Higher-difficulty sources have higher probability of better rarity.
         */
        fun selectRarity(seed: Long, sourceType: ShadowExtractionSourceType): ShadowRarity {
            val rarityIndex = when (sourceType) {
                ShadowExtractionSourceType.DUNGEON_COMPLETION -> {
                    val r = seed % 100
                    when {
                        r < 40 -> 0 // COMMON
                        r < 65 -> 1 // UNCOMMON
                        r < 82 -> 2 // RARE
                        r < 93 -> 3 // EPIC
                        r < 99 -> 4 // LEGENDARY
                        else -> 5    // MYTHIC
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
                else -> { // PHYSICAL, ACADEMIC, DISCIPLINE, CONSISTENCY, MONARCH milestones
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

        /**
         * Deterministic shadow name generation.
         * Names are assigned from a fixed catalog based on class + seed.
         */
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

        /**
         * Deterministic initial stats based on class, rarity, and seed.
         */
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

    /**
     * Validates that the source record genuinely exists in the originating database tables
     * and represents a verified, completed achievement.
     */
    suspend fun validateOriginatingSourceRecord(
        sourceType: ShadowExtractionSourceType,
        sourceEventId: String
    ): Boolean {
        if (sourceEventId.isBlank()) return false
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
                    database.shadowQuestProgressDao().getQuestProgress(sourceEventId)?.isCompleted == true
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    // -------------------------------------------------------------------------
    // EVALUATE EXTRACTION ELIGIBILITY
    // -------------------------------------------------------------------------

    /**
     * Evaluates whether extraction is available for the given source event.
     * Returns a pending extraction event (not yet persisted or confirmed).
     */
    suspend fun evaluateExtractionEligibility(
        sourceType: ShadowExtractionSourceType,
        sourceEventId: String,
        evidenceSummary: String
    ): ShadowExtractionEvent {
        val extractionId = UUID.randomUUID().toString()
        val seed = buildDeterminismSeed(sourceType, sourceEventId)

        // Check: valid non-blank source evidence AND genuine persisted record in originating table
        if (sourceEventId.isBlank() || evidenceSummary.isBlank() || !validateOriginatingSourceRecord(sourceType, sourceEventId)) {
            return ShadowExtractionEvent(
                extractionId = extractionId,
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

        // Check: already extracted from this composite source event
        val existing = extractionDao.getExtractionBySource(sourceType.name, sourceEventId)
        if (existing != null) {
            return ShadowExtractionEvent(
                extractionId = existing.extractionId,
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

        // Check: army capacity
        val profile = monarchProgressionEngine.getOrCreateProfile()
        val activeShadows = shadowDao.getActiveShadowCount()
        if (activeShadows >= profile.armyCapacity) {
            return ShadowExtractionEvent(
                extractionId = extractionId,
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

        // All checks passed: determine extraction result deterministically
        val shadowClass = selectClass(seed)
        val shadowRarity = selectRarity(seed, sourceType)
        val shadowName = selectName(shadowClass, seed)

        return ShadowExtractionEvent(
            extractionId = extractionId,
            sourceType = sourceType,
            sourceEventId = sourceEventId,
            outcome = ShadowExtractionOutcome.PENDING_CONFIRMATION,
            extractedShadowId = null, // null until confirmed
            shadowName = shadowName,
            shadowClass = shadowClass,
            shadowRarity = shadowRarity,
            determinismSeed = seed,
            evidenceSummary = evidenceSummary
        )
    }

    // -------------------------------------------------------------------------
    // CONFIRM EXTRACTION (player confirmation required, atomic Room transaction)
    // -------------------------------------------------------------------------

    /**
     * Confirms and persists a pending extraction within an atomic database transaction.
     * This is the ONLY path that creates a new Shadow entity.
     * If extraction insertion fails, the entire transaction is rolled back.
     * Returns the confirmed extraction event with the shadow ID populated.
     */
    suspend fun confirmExtraction(pendingEvent: ShadowExtractionEvent): Result<ShadowExtractionEvent> {
        return runCatching {
            if (pendingEvent.outcome != ShadowExtractionOutcome.PENDING_CONFIRMATION) {
                throw IllegalStateException("Cannot confirm extraction with outcome ${pendingEvent.outcome}")
            }
            if (pendingEvent.shadowClass == null || pendingEvent.shadowRarity == null || pendingEvent.shadowName == null) {
                throw IllegalStateException("Missing shadow parameters in pending event")
            }

            database.withTransaction {
                // 1. Re-validate persisted source evidence inside transaction
                if (!validateOriginatingSourceRecord(pendingEvent.sourceType, pendingEvent.sourceEventId)) {
                    return@withTransaction pendingEvent.copy(outcome = ShadowExtractionOutcome.SOURCE_INVALID)
                }

                // 2. Double-check capacity inside transaction
                val profile = monarchProgressionEngine.getOrCreateProfile()
                val activeShadows = shadowDao.getActiveShadowCount()
                if (activeShadows >= profile.armyCapacity) {
                    return@withTransaction pendingEvent.copy(outcome = ShadowExtractionOutcome.ARMY_CAPACITY_REACHED)
                }

                // 3. Double-check idempotency on composite key inside transaction
                val alreadyExtracted = extractionDao.getExtractionBySource(pendingEvent.sourceType.name, pendingEvent.sourceEventId)
                if (alreadyExtracted != null) {
                    return@withTransaction pendingEvent.copy(
                        outcome = ShadowExtractionOutcome.ALREADY_CLAIMED,
                        extractedShadowId = alreadyExtracted.extractedShadowId
                    )
                }

                val shadowId = "shadow_${pendingEvent.extractionId.take(12)}"
                val seed = pendingEvent.determinismSeed
                val stats = calculateInitialStats(pendingEvent.shadowClass, pendingEvent.shadowRarity, seed)

                val newShadow = Shadow(
                    shadowId = shadowId,
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

                // Persist shadow entity inside transaction
                shadowDao.insertShadow(newShadow.toEntity())

                // Record extraction event as confirmed inside transaction
                val confirmedEvent = pendingEvent.copy(
                    extractedShadowId = shadowId,
                    outcome = ShadowExtractionOutcome.SUCCESS,
                    isConfirmed = true,
                    monarchXpAwarded = MONARCH_XP_PER_EXTRACTION
                )
                val insertedRow = extractionDao.insertExtraction(confirmedEvent.toEntity())
                if (insertedRow <= 0) {
                    // Trigger Room transaction rollback so staged shadow is discarded
                    throw IllegalStateException("Failed to insert extraction record: unique index constraint conflict. Rolling back transaction.")
                }

                // Award Monarch XP (idempotent) inside transaction
                monarchProgressionEngine.awardMonarchXp(
                    sourceTag = "extraction_${pendingEvent.extractionId}",
                    xpAmount = MONARCH_XP_PER_EXTRACTION,
                    sourceDescription = "Shadow extracted: ${pendingEvent.shadowName}"
                )

                // Refresh monarch profile computed fields inside transaction
                monarchProgressionEngine.refreshMonarchProfile()

                confirmedEvent
            }
        }
    }

    // -------------------------------------------------------------------------
    // QUERY HELPERS
    // -------------------------------------------------------------------------

    suspend fun getExtractionHistory(): List<ShadowExtractionEvent> {
        return extractionDao.getAllExtractionsOnce().map { it.toDomain() }
    }

    suspend fun getConfirmedExtractionCount(): Int {
        return extractionDao.getConfirmedExtractionCount()
    }
}

