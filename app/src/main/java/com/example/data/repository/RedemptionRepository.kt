package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.entity.PenaltyPointAccountEntity
import com.example.data.local.entity.PenaltyTransactionEntity
import com.example.data.local.entity.RedemptionEntity
import com.example.data.local.entity.RedemptionVerificationEntity
import com.example.data.local.entity.toDomain
import com.example.data.local.entity.toEntity
import com.example.domain.engine.CorrectiveActivityGenerationEngine
import com.example.domain.engine.PenaltyResolutionEngine
import com.example.domain.engine.RedemptionVerificationEngine
import com.example.domain.model.PenaltyRecord
import com.example.domain.model.PenaltyResolutionResult
import com.example.domain.model.PenaltyStatus
import com.example.domain.model.PenaltyTransactionType
import com.example.domain.model.RecoveryStatus
import com.example.domain.model.RedemptionLifecycleState
import com.example.domain.model.RedemptionPlan
import com.example.domain.model.RedemptionVerificationEvidence
import com.example.domain.model.SystemEventType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * Authoritative repository coordinating corrective activity generation,
 * redemption verification, and transactional penalty ledger resolution.
 *
 * Guarantees:
 * - Deterministic activity generation and verification.
 * - Recovery status strictly prevents punitive workouts.
 * - Atomic database operations via Room withTransaction.
 * - Complete progression isolation (0 mutations to XP, level, rank, or attributes).
 * - Full system event dispatching.
 */
class RedemptionRepository(
    private val database: SoloLevelingDatabase,
    private val systemEventRepository: SystemEventRepository
) {

    /**
     * Generates or retrieves an existing redemption plan for an active penalty.
     */
    suspend fun generateOrGetRedemptionPlan(
        penaltyId: String,
        accountOwnerId: String
    ): RedemptionPlan? {
        // 1. Check if an active redemption plan already exists for this penalty
        val existingEntity = database.redemptionDao().getActiveRedemptionForPenalty(penaltyId)
        if (existingEntity != null) {
            return existingEntity.toDomain()
        }

        // 2. Retrieve penalty
        val penaltyEntity = database.penaltyDao().getPenaltyById(penaltyId) ?: return null
        val penalty = penaltyEntity.toDomain()

        // If penalty is already resolved, waived, or exempted, cannot generate redemption
        if (!penalty.penaltyStatus.isOutstanding) {
            return null
        }

        // 3. Retrieve original quest
        val questEntity = database.questDao().getQuestById(penalty.questId)
        val quest = questEntity?.toDomain()

        // 4. Retrieve recovery status to enforce safety protections
        val physicalState = database.physicalStateDao().getPhysicalStateOnce()
        val recoveryStatus = physicalState?.let {
            RecoveryStatus.fromName(it.recoveryStatusName)
        } ?: RecoveryStatus.OPTIMAL

        // 5. Retrieve active redemptions and recent activity history
        val activeRedemptions = database.redemptionDao().getActiveRedemptionsOnce(accountOwnerId)
            .map { it.toDomain() }
        val allRedemptions = database.redemptionDao().getAllRedemptionsOnce(accountOwnerId)
        val recentActivityIds = allRedemptions.map { it.activityId }.toSet()

        // 6. Generate deterministic plan
        val plan = CorrectiveActivityGenerationEngine.generatePlan(
            penalty = penalty,
            quest = quest,
            recoveryStatus = recoveryStatus,
            activeRedemptions = activeRedemptions,
            recentActivityIds = recentActivityIds
        )

        // 7. Persist redemption plan
        database.redemptionDao().insertRedemption(plan.toEntity())

        // 8. Publish system event
        systemEventRepository.publishEvent(
            eventType = SystemEventType.CORRECTIVE_ACTIVITY_GENERATED,
            relatedEntityId = plan.redemptionId,
            metadata = mapOf(
                "penaltyId" to penaltyId,
                "activityId" to plan.activityId,
                "activityTitle" to plan.activityTitle,
                "category" to plan.category.name,
                "shadowClass" to plan.assignedShadowClass.name
            )
        )

        return plan
    }

    /**
     * Explicitly accepts a proposed redemption activity.
     */
    suspend fun acceptRedemption(redemptionId: String): Boolean {
        val plan = database.redemptionDao().getRedemptionById(redemptionId) ?: return false
        if (plan.lifecycleStateName != RedemptionLifecycleState.PENDING_ACCEPTANCE.name &&
            plan.lifecycleStateName != RedemptionLifecycleState.GENERATED.name
        ) {
            return false
        }

        val now = System.currentTimeMillis()
        database.redemptionDao().updateAcceptance(
            redemptionId = redemptionId,
            stateName = RedemptionLifecycleState.ACTIVE.name,
            acceptedAtEpochMs = now
        )

        systemEventRepository.publishEvent(
            eventType = SystemEventType.CORRECTIVE_ACTIVITY_ACCEPTED,
            relatedEntityId = redemptionId,
            metadata = mapOf("penaltyId" to plan.penaltyId)
        )
        systemEventRepository.publishEvent(
            eventType = SystemEventType.REDEMPTION_STARTED,
            relatedEntityId = redemptionId,
            metadata = mapOf("activityTitle" to plan.activityTitle)
        )

        return true
    }

    /**
     * Rejects an unsuitable activity and generates a safe alternative.
     */
    suspend fun requestAlternativeActivity(redemptionId: String): RedemptionPlan? {
        val planEntity = database.redemptionDao().getRedemptionById(redemptionId) ?: return null
        database.redemptionDao().updateState(redemptionId, RedemptionLifecycleState.REJECTED.name)

        return generateOrGetRedemptionPlan(planEntity.penaltyId, planEntity.accountOwnerId)
    }

    /**
     * Pauses a redemption activity due to fatigue, illness, or emergency.
     */
    suspend fun pauseRedemption(redemptionId: String, reason: String): Boolean {
        val plan = database.redemptionDao().getRedemptionById(redemptionId) ?: return false
        database.redemptionDao().updateState(redemptionId, RedemptionLifecycleState.PAUSED.name)

        systemEventRepository.publishEvent(
            eventType = SystemEventType.REDEMPTION_PAUSED,
            relatedEntityId = redemptionId,
            metadata = mapOf("reason" to reason)
        )
        return true
    }

    /**
     * Submits completion evidence, verifies compliance, and executes transactional penalty point resolution.
     */
    suspend fun submitAndVerifyRedemption(
        redemptionId: String,
        evidence: RedemptionVerificationEvidence
    ): PenaltyResolutionResult {
        val planEntity = database.redemptionDao().getRedemptionById(redemptionId)
            ?: return PenaltyResolutionResult(
                success = false,
                penaltyId = "",
                redemptionId = redemptionId,
                pointsResolved = 0,
                remainingPenaltyBalance = 0,
                newAccountBalance = 0,
                penaltyFullyResolved = false,
                message = "Redemption plan not found: $redemptionId"
            )
        val plan = planEntity.toDomain()

        // 1. Verify evidence via deterministic engine
        val verification = RedemptionVerificationEngine.verify(plan, evidence)
        if (!verification.isApproved) {
            systemEventRepository.publishEvent(
                eventType = SystemEventType.REDEMPTION_REJECTED,
                relatedEntityId = redemptionId,
                metadata = mapOf(
                    "reason" to (verification.rejectionReason ?: "Failed verification criteria")
                )
            )
            return PenaltyResolutionResult(
                success = false,
                penaltyId = plan.penaltyId,
                redemptionId = redemptionId,
                pointsResolved = 0,
                remainingPenaltyBalance = plan.resolutionPoints,
                newAccountBalance = 0,
                penaltyFullyResolved = false,
                message = verification.rejectionReason ?: "Evidence rejected by verification engine."
            )
        }

        val now = System.currentTimeMillis()

        // 2. Execute atomic ledger transaction
        return database.withTransaction {
            val penaltyEntity = database.penaltyDao().getPenaltyById(plan.penaltyId)
                ?: return@withTransaction PenaltyResolutionResult(
                    success = false,
                    penaltyId = plan.penaltyId,
                    redemptionId = redemptionId,
                    pointsResolved = 0,
                    remainingPenaltyBalance = 0,
                    newAccountBalance = 0,
                    penaltyFullyResolved = false,
                    message = "Associated penalty record not found."
                )
            val penalty = penaltyEntity.toDomain()

            val account = database.penaltyAccountDao().getAccountOnce(plan.accountOwnerId)
            val currentAccountBalance = account?.currentBalance ?: 0

            // 3. Calculate resolution amounts
            val resolution = PenaltyResolutionEngine.calculateResolution(
                penalty = penalty,
                verifiedPoints = verification.pointsResolved,
                currentAccountBalance = currentAccountBalance
            )

            // 4. Update redemption status to completed and verified
            database.redemptionDao().updateCompletion(
                redemptionId = redemptionId,
                stateName = RedemptionLifecycleState.VERIFIED_COMPLETED.name,
                completedAtEpochMs = now,
                verifiedAtEpochMs = now,
                verificationNotes = verification.auditNotes
            )

            // 5. Insert verification record
            database.redemptionVerificationDao().insertVerification(
                RedemptionVerificationEntity(
                    verificationId = UUID.randomUUID().toString(),
                    redemptionId = redemptionId,
                    accountOwnerId = plan.accountOwnerId,
                    submittedAtEpochMs = evidence.submittedAtEpochMs,
                    reflectionText = evidence.reflectionText,
                    checklistItemsJson = evidence.checklistItemsConfirmed.joinToString(";"),
                    externalSessionRefId = evidence.externalSessionRefId,
                    isValid = true,
                    pointsResolved = resolution.pointsToDeduct,
                    verifiedAtEpochMs = now,
                    notes = verification.auditNotes
                )
            )

            // 6. Record resolution transaction in penalty ledger
            val txId = UUID.randomUUID().toString()
            val idempotencyKey = "res_${redemptionId}"
            database.penaltyTransactionDao().insertTransaction(
                PenaltyTransactionEntity(
                    transactionId = txId,
                    accountOwnerId = plan.accountOwnerId,
                    penaltyId = penalty.penaltyId,
                    transactionTypeName = PenaltyTransactionType.REDEMPTION_RESOLUTION.name,
                    pointsDelta = resolution.pointsToDeduct,
                    balanceAfter = resolution.newAccountBalance,
                    reason = "Redemption verified: ${plan.activityTitle}. Points resolved: ${resolution.pointsToDeduct} PP.",
                    timestampEpochMs = now,
                    idempotencyKey = idempotencyKey
                )
            )

            // 7. Update penalty point account
            val currentLifetime = account?.lifetimePointsAccrued ?: 0
            val currentActive = account?.activePenaltiesCount ?: 1
            val newActiveCount = if (resolution.isFullyResolved) kotlin.math.max(0, currentActive - 1) else currentActive

            database.penaltyAccountDao().upsertAccount(
                PenaltyPointAccountEntity(
                    accountOwnerId = plan.accountOwnerId,
                    currentBalance = resolution.newAccountBalance,
                    lifetimePointsAccrued = currentLifetime,
                    activePenaltiesCount = newActiveCount,
                    lastUpdatedEpochMs = now
                )
            )

            // 8. Update penalty status
            database.penaltyDao().updatePenaltyStatus(
                penaltyId = penalty.penaltyId,
                statusName = resolution.newPenaltyStatus.name,
                resolvedAtEpochMs = if (resolution.isFullyResolved) now else null
            )

            // 9. Dispatch events
            systemEventRepository.publishEvent(
                eventType = SystemEventType.REDEMPTION_VERIFIED,
                relatedEntityId = redemptionId,
                metadata = mapOf(
                    "pointsResolved" to resolution.pointsToDeduct.toString(),
                    "activityTitle" to plan.activityTitle
                )
            )

            val resolutionEventType = if (resolution.isFullyResolved) {
                SystemEventType.PENALTY_RESOLVED
            } else {
                SystemEventType.PENALTY_PARTIALLY_RESOLVED
            }
            systemEventRepository.publishEvent(
                eventType = resolutionEventType,
                relatedEntityId = penalty.penaltyId,
                metadata = mapOf(
                    "pointsResolved" to resolution.pointsToDeduct.toString(),
                    "remainingPenaltyPoints" to resolution.remainingPenaltyPoints.toString()
                )
            )

            systemEventRepository.publishEvent(
                eventType = SystemEventType.PENALTY_BALANCE_UPDATED,
                relatedEntityId = plan.accountOwnerId,
                metadata = mapOf("newBalance" to resolution.newAccountBalance.toString())
            )

            PenaltyResolutionResult(
                success = true,
                penaltyId = penalty.penaltyId,
                redemptionId = redemptionId,
                pointsResolved = resolution.pointsToDeduct,
                remainingPenaltyBalance = resolution.remainingPenaltyPoints,
                newAccountBalance = resolution.newAccountBalance,
                penaltyFullyResolved = resolution.isFullyResolved,
                message = "Successfully resolved ${resolution.pointsToDeduct} PP through verified redemption."
            )
        }
    }

    fun observeActiveRedemptions(accountOwnerId: String): Flow<List<RedemptionPlan>> {
        return database.redemptionDao().observeActiveRedemptions(accountOwnerId).map { list ->
            list.map { it.toDomain() }
        }
    }

    fun observeAllRedemptions(accountOwnerId: String): Flow<List<RedemptionPlan>> {
        return database.redemptionDao().observeAllRedemptions(accountOwnerId).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun getRedemptionById(redemptionId: String): RedemptionPlan? {
        return database.redemptionDao().getRedemptionById(redemptionId)?.toDomain()
    }
}
