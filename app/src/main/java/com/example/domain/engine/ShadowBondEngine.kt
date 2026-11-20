package com.example.domain.engine

import androidx.room.withTransaction
import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.dao.ShadowBondDao
import com.example.data.local.dao.ShadowDao
import com.example.data.local.entity.ShadowBondEventEntity
import com.example.data.local.entity.ShadowBondMilestoneEntity
import com.example.data.local.entity.ShadowBondProfileEntity
import com.example.data.local.entity.toDomain
import com.example.data.local.entity.toEntity
import com.example.domain.model.DEFAULT_SHADOW_ACCOUNT_OWNER_ID
import com.example.domain.model.ShadowArmyBondOverview
import com.example.domain.model.ShadowBondActivityType
import com.example.domain.model.ShadowBondConfig
import com.example.domain.model.ShadowBondEvent
import com.example.domain.model.ShadowBondMilestone
import com.example.domain.model.ShadowBondMilestoneType
import com.example.domain.model.ShadowBondProfile
import com.example.domain.model.ShadowBondSummary
import com.example.domain.model.ShadowPersonality
import com.example.domain.model.ShadowRelationshipStage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

// ============================================================================
// MILESTONE 12 — PHASE 6.4: SHADOW BOND & LOYALTY PROGRESSION ENGINE
// Deterministic, account-isolated, transactional engine managing relationship
// progression between the Shadow Monarch and individual Shadows.
//
// Progression Guarantees:
// - Zero leakage into Player XP, Level, Rank, Attributes, or Momentum XP.
// - Purely deterministic math based on centralized configuration (ShadowBondConfig).
// - Stable idempotency keys prevent duplicate event claims.
// - Mutex locking + Room transactions guarantee atomicity.
// - No loyalty decay or arbitrary penalties.
// ============================================================================

class ShadowBondEngine(
    private val shadowBondDao: ShadowBondDao,
    private val shadowDao: ShadowDao,
    private val database: SoloLevelingDatabase? = null,
    private val mutex: Mutex = Mutex()
) {

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
    // GET OR INITIALIZE BOND PROFILE
    // -------------------------------------------------------------------------

    suspend fun getOrCreateBondProfile(
        shadowId: String,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): ShadowBondProfile {
        val owner = accountOwnerId.trim()
        require(owner.isNotBlank()) { "accountOwnerId must not be blank" }
        require(shadowId.isNotBlank()) { "shadowId must not be blank" }

        val existing = shadowBondDao.getBondProfile(shadowId, owner)
        if (existing != null) {
            return existing.toDomain()
        }

        return withLockedTransaction {
            val retryExisting = shadowBondDao.getBondProfile(shadowId, owner)
            if (retryExisting != null) {
                return@withLockedTransaction retryExisting.toDomain()
            }

            val shadow = shadowDao.getShadowByIdForOwner(shadowId, owner)
                ?: throw IllegalArgumentException("Shadow $shadowId does not exist for account $owner")

            val domainShadow = shadow.toDomain()
            val personality = ShadowPersonality.forShadow(domainShadow.shadowClass)
            val now = System.currentTimeMillis()

            val initialProfile = ShadowBondProfileEntity(
                shadowId = shadowId,
                accountOwnerId = owner,
                bondLevel = 1,
                bondPoints = 0L,
                loyaltyRating = 15,
                trustRating = 10,
                relationshipStageName = ShadowRelationshipStage.UNFAMILIAR.name,
                personalityCharacteristicName = personality.name,
                totalActivitiesCount = 0,
                lastBondActivityEpochMs = now,
                updatedAtEpochMs = now
            )

            shadowBondDao.upsertBondProfile(initialProfile)
            initialProfile.toDomain()
        }
    }

    fun observeBondProfile(
        shadowId: String,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Flow<ShadowBondProfile?> {
        return shadowBondDao.observeBondProfile(shadowId, accountOwnerId).map { it?.toDomain() }
    }

    // -------------------------------------------------------------------------
    // RECORD AUTHORITATIVE BOND ACTIVITY
    // -------------------------------------------------------------------------

    suspend fun recordBondActivity(
        shadowId: String,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID,
        activityType: ShadowBondActivityType,
        sourceEventId: String,
        customDescription: String? = null
    ): Result<ShadowBondProfile> {
        return runCatching {
            val owner = accountOwnerId.trim()
            require(owner.isNotBlank()) { "accountOwnerId must not be blank" }
            require(shadowId.isNotBlank()) { "shadowId must not be blank" }
            require(sourceEventId.isNotBlank()) { "sourceEventId must not be blank" }

            val idempotencyKey = "bond_${owner}_${shadowId}_${activityType.code}_${sourceEventId.trim()}"

            withLockedTransaction {
                // Idempotency check: Reject duplicate claims
                val existingEvent = shadowBondDao.getBondEventByIdempotencyKey(idempotencyKey)
                if (existingEvent != null) {
                    // Already processed: return current profile without duplicate rewards
                    val profile = shadowBondDao.getBondProfile(shadowId, owner)
                        ?: getOrCreateBondProfile(shadowId, owner).toEntity()
                    return@withLockedTransaction profile.toDomain()
                }

                // Verify shadow exists for this owner
                val shadow = shadowDao.getShadowByIdForOwner(shadowId, owner)
                    ?: throw IllegalArgumentException("Shadow $shadowId does not exist for account $owner")

                val currentProfile = shadowBondDao.getBondProfile(shadowId, owner)
                    ?: getOrCreateBondProfile(shadowId, owner).toEntity()

                val now = System.currentTimeMillis()
                val newBondPoints = currentProfile.bondPoints + activityType.baseBondPoints
                val newLoyalty = (currentProfile.loyaltyRating + activityType.loyaltyGain)
                    .coerceIn(ShadowBondConfig.MIN_STAT_RATING, ShadowBondConfig.MAX_STAT_RATING)
                val newTrust = (currentProfile.trustRating + activityType.trustGain)
                    .coerceIn(ShadowBondConfig.MIN_STAT_RATING, ShadowBondConfig.MAX_STAT_RATING)
                val newBondLevel = ShadowBondConfig.calculateBondLevel(newBondPoints)

                // Sequential Relationship Stage Progression (prevent stage skipping)
                var currentStage = runCatching { ShadowRelationshipStage.valueOf(currentProfile.relationshipStageName) }
                    .getOrDefault(ShadowRelationshipStage.UNFAMILIAR)

                var canAdvance = true
                while (canAdvance) {
                    val nextStage = currentStage.nextStage()
                    if (nextStage != null &&
                        newBondLevel >= nextStage.minBondLevel &&
                        newBondPoints >= nextStage.minBondPoints &&
                        newLoyalty >= nextStage.minLoyalty &&
                        newTrust >= nextStage.minTrust
                    ) {
                        currentStage = nextStage
                    } else {
                        canAdvance = false
                    }
                }

                val description = customDescription?.takeIf { it.isNotBlank() }
                    ?: "${activityType.descriptionTemplate} with ${shadow.name}"

                // 1. Record event
                val eventEntity = ShadowBondEventEntity(
                    eventId = UUID.randomUUID().toString(),
                    accountOwnerId = owner,
                    shadowId = shadowId,
                    activityTypeName = activityType.name,
                    sourceEventId = sourceEventId,
                    bondPointsAwarded = activityType.baseBondPoints,
                    loyaltyDelta = activityType.loyaltyGain,
                    trustDelta = activityType.trustGain,
                    description = description,
                    timestampEpochMs = now,
                    idempotencyKey = idempotencyKey
                )
                shadowBondDao.insertBondEvent(eventEntity)

                // 2. Check and unlock relevant milestones
                evaluateAndUnlockMilestones(
                    owner = owner,
                    shadowId = shadowId,
                    activityType = activityType,
                    sourceEventId = sourceEventId,
                    loyalty = newLoyalty,
                    trust = newTrust,
                    stage = currentStage,
                    timestamp = now
                )

                // 3. Update profile
                val updatedProfile = currentProfile.copy(
                    bondLevel = newBondLevel,
                    bondPoints = newBondPoints,
                    loyaltyRating = newLoyalty,
                    trustRating = newTrust,
                    relationshipStageName = currentStage.name,
                    totalActivitiesCount = currentProfile.totalActivitiesCount + 1,
                    lastBondActivityEpochMs = now,
                    updatedAtEpochMs = now
                )
                shadowBondDao.upsertBondProfile(updatedProfile)

                updatedProfile.toDomain()
            }
        }
    }

    private suspend fun evaluateAndUnlockMilestones(
        owner: String,
        shadowId: String,
        activityType: ShadowBondActivityType,
        sourceEventId: String,
        loyalty: Int,
        trust: Int,
        stage: ShadowRelationshipStage,
        timestamp: Long
    ) {
        suspend fun unlockIfMissing(type: ShadowBondMilestoneType, eventId: String) {
            val exists = shadowBondDao.hasMilestone(shadowId, owner, type.name)
            if (!exists) {
                shadowBondDao.insertBondMilestone(
                    ShadowBondMilestoneEntity(
                        milestoneId = UUID.randomUUID().toString(),
                        accountOwnerId = owner,
                        shadowId = shadowId,
                        milestoneTypeName = type.name,
                        title = type.title,
                        description = type.description,
                        sourceEventId = eventId,
                        timestampEpochMs = timestamp,
                        verificationStatus = "VERIFIED"
                    )
                )
            }
        }

        when (activityType) {
            ShadowBondActivityType.DEPLOYMENT_SUCCESS -> {
                unlockIfMissing(ShadowBondMilestoneType.FIRST_DEPLOYMENT, sourceEventId)
            }
            ShadowBondActivityType.DEPLOYMENT_RECALL -> {
                unlockIfMissing(ShadowBondMilestoneType.FIRST_MISSION_COMPLETE, sourceEventId)
            }
            ShadowBondActivityType.BOSS_BATTLE -> {
                unlockIfMissing(ShadowBondMilestoneType.FIRST_BOSS_VICTORY, sourceEventId)
            }
            ShadowBondActivityType.SHADOW_EVOLUTION -> {
                unlockIfMissing(ShadowBondMilestoneType.FIRST_EVOLUTION, sourceEventId)
            }
            else -> { /* No direct activity trigger */ }
        }

        if (loyalty >= 80) {
            unlockIfMissing(ShadowBondMilestoneType.LOYALTY_DEVOTION, sourceEventId)
        }
        if (trust >= 80) {
            unlockIfMissing(ShadowBondMilestoneType.TRUST_ABSOLUTE, sourceEventId)
        }
        if (stage == ShadowRelationshipStage.UNBREAKABLE) {
            unlockIfMissing(ShadowBondMilestoneType.UNBREAKABLE_BOND, sourceEventId)
        }
    }

    // -------------------------------------------------------------------------
    // OBSERVABLE HISTORY & MILESTONES
    // -------------------------------------------------------------------------

    fun observeBondEventsForShadow(
        shadowId: String,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Flow<List<ShadowBondEvent>> {
        return shadowBondDao.observeBondEventsForShadow(shadowId, accountOwnerId)
            .map { list -> list.map { it.toDomain() } }
    }

    fun observeBondMilestonesForShadow(
        shadowId: String,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Flow<List<ShadowBondMilestone>> {
        return shadowBondDao.observeBondMilestonesForShadow(shadowId, accountOwnerId)
            .map { list -> list.map { it.toDomain() } }
    }

    suspend fun getBondMilestonesForShadow(
        shadowId: String,
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): List<ShadowBondMilestone> {
        return shadowBondDao.getBondMilestonesForShadow(shadowId, accountOwnerId)
            .map { it.toDomain() }
    }

    // -------------------------------------------------------------------------
    // ARMY-WIDE BOND OVERVIEW (For Collection & Dashboard)
    // -------------------------------------------------------------------------

    fun observeArmyBondOverview(
        accountOwnerId: String = DEFAULT_SHADOW_ACCOUNT_OWNER_ID
    ): Flow<ShadowArmyBondOverview> {
        val owner = accountOwnerId.trim().ifBlank { DEFAULT_SHADOW_ACCOUNT_OWNER_ID }

        return combine(
            shadowDao.observeAllShadowsForOwner(owner),
            shadowBondDao.observeAllBondProfilesForOwner(owner),
            shadowBondDao.observeRecentMilestonesForOwner(owner, 5)
        ) { allShadows, profiles, recentMilestones ->
            val profileMap = profiles.associateBy { it.shadowId }

            val summaries = allShadows.map { shadowEntity ->
                val shadow = shadowEntity.toDomain()
                val profile = profileMap[shadow.shadowId]
                val stage = profile?.let {
                    runCatching { ShadowRelationshipStage.valueOf(it.relationshipStageName) }.getOrNull()
                } ?: ShadowRelationshipStage.UNFAMILIAR

                val personality = profile?.let {
                    runCatching { ShadowPersonality.valueOf(it.personalityCharacteristicName) }.getOrNull()
                } ?: ShadowPersonality.forShadow(shadow.shadowClass)

                ShadowBondSummary(
                    shadowId = shadow.shadowId,
                    shadowName = shadow.name,
                    shadowClass = shadow.shadowClass,
                    bondLevel = profile?.bondLevel ?: 1,
                    relationshipStage = stage,
                    loyaltyRating = profile?.loyaltyRating ?: 15,
                    trustRating = profile?.trustRating ?: 10,
                    personality = personality
                )
            }

            val totalShadows = allShadows.size
            val bondedShadows = summaries.filter { it.bondLevel > 1 || it.loyaltyRating > 15 }
            val averageLevel = if (summaries.isNotEmpty()) {
                summaries.map { it.bondLevel }.average().toFloat()
            } else 1.0f

            val highestStage = summaries.maxByOrNull { it.relationshipStage.order }?.relationshipStage
                ?: ShadowRelationshipStage.UNFAMILIAR

            val distribution = ShadowRelationshipStage.entries.associateWith { stage ->
                summaries.count { it.relationshipStage == stage }
            }

            val mostLoyal = summaries.sortedByDescending { it.loyaltyRating }.take(3)

            ShadowArmyBondOverview(
                totalShadows = totalShadows,
                bondedShadowsCount = bondedShadows.size,
                averageBondLevel = averageLevel,
                highestRelationshipStage = highestStage,
                stageDistribution = distribution,
                mostLoyalShadows = mostLoyal,
                recentMilestones = recentMilestones.map { it.toDomain() }
            )
        }
    }
}
