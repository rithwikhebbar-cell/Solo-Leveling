package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.entity.PenaltyEntity
import com.example.data.local.entity.PenaltyPointAccountEntity
import com.example.data.local.entity.PenaltyTransactionEntity
import com.example.data.local.entity.toDomain
import com.example.data.local.entity.toEntity
import com.example.domain.engine.PenaltyCalculationEngine
import com.example.domain.model.DefaultPenaltyPolicy
import com.example.domain.model.PenaltyAllocationResult
import com.example.domain.model.PenaltyPointBalance
import com.example.domain.model.PenaltyPolicy
import com.example.domain.model.PenaltyRecord
import com.example.domain.model.PenaltyStatus
import com.example.domain.model.PenaltyTransaction
import com.example.domain.model.PenaltyTransactionType
import com.example.domain.model.SystemEventType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

/**
 * Authoritative repository coordinating penalty calculation, point allocation,
 * and double-entry transaction ledgers for the Solo Leveling Penalty System.
 *
 * Guarantees:
 * - Deterministic, auditable execution.
 * - Idempotent allocation per confirmed failure record.
 * - Atomic database transactions via Room withTransaction.
 * - Complete progression isolation (0 mutations to player XP, rank, level, or attributes).
 * - Full system event telemetry.
 */
class PenaltyRepository(
    private val database: SoloLevelingDatabase,
    private val systemEventRepository: SystemEventRepository,
    private val policy: PenaltyPolicy = DefaultPenaltyPolicy
) {

    /**
     * Allocates penalty points for an eligible confirmed failure record.
     * This operation is strictly atomic and idempotent.
     */
    suspend fun allocatePenaltyForConfirmedFailure(
        failureRecordId: String,
        nowEpochMs: Long = System.currentTimeMillis()
    ): PenaltyAllocationResult {
        // 1. Idempotency Check: Check if penalty is already allocated for this failure
        val existingPenalty = database.penaltyDao().getPenaltyByFailureRecordId(failureRecordId)
        if (existingPenalty != null) {
            val account = database.penaltyAccountDao().getAccountOnce(existingPenalty.accountOwnerId)
            return PenaltyAllocationResult(
                success = true,
                penalty = existingPenalty.toDomain(),
                newBalance = account?.currentBalance ?: 0,
                message = "Penalty already allocated for failure $failureRecordId (idempotent replay).",
                isDuplicate = true
            )
        }

        // 2. Retrieve failure record
        val failureRecordEntity = database.confirmedFailureDao().getAllFailuresOnce()
            .find { it.failureId == failureRecordId }
            ?: return PenaltyAllocationResult(
                success = false,
                message = "Confirmed failure record not found: $failureRecordId."
            )
        val failureRecord = failureRecordEntity.toDomain()

        // 3. Retrieve commitment and target quest
        val commitmentEntity = database.questCommitmentDao().getCommitmentById(failureRecord.commitmentId)
            ?: return PenaltyAllocationResult(
                success = false,
                message = "Associated commitment not found: ${failureRecord.commitmentId}."
            )
        val commitment = commitmentEntity.toDomain()

        val questEntity = database.questDao().getQuestById(failureRecord.questId)
            ?: return PenaltyAllocationResult(
                success = false,
                message = "Associated quest not found: ${failureRecord.questId}."
            )
        val quest = questEntity.toDomain()

        // 4. Verify eligibility
        val eligibility = PenaltyCalculationEngine.checkEligibility(commitment, quest, failureRecord)
        if (eligibility is PenaltyCalculationEngine.EligibilityResult.Ineligible) {
            return PenaltyAllocationResult(
                success = false,
                message = "Failure is not eligible for penalty allocation: ${eligibility.reason}"
            )
        }

        // 5. Fetch rolling failure history for account
        val allFailures = database.confirmedFailureDao().getAllFailuresOnce()
            .filter { it.accountOwnerId == failureRecord.accountOwnerId }
            .map { it.toDomain() }

        // 6. Calculate deterministic severity and points
        val calculation = PenaltyCalculationEngine.calculate(
            failureRecord = failureRecord,
            commitment = commitment,
            quest = quest,
            failureHistory = allFailures,
            policy = policy
        )

        // Publish PENALTY_CALCULATED event
        systemEventRepository.publishEvent(
            eventType = SystemEventType.PENALTY_CALCULATED,
            relatedEntityId = failureRecordId,
            metadata = mapOf(
                "questId" to quest.id.toString(),
                "severity" to calculation.severity.code,
                "basePoints" to calculation.basePenaltyPoints.toString(),
                "escalationBonus" to calculation.escalationBonusPoints.toString(),
                "totalPoints" to calculation.totalPenaltyPoints.toString(),
                "policyVersion" to calculation.policyVersion
            )
        )

        val penaltyId = UUID.randomUUID().toString()
        val idempotencyKey = "alloc_${failureRecordId}"

        val penaltyEntity = PenaltyEntity(
            penaltyId = penaltyId,
            failureRecordId = failureRecord.failureId,
            commitmentId = commitment.commitmentId,
            questId = quest.id,
            accountOwnerId = failureRecord.accountOwnerId,
            questTitleSnapshot = quest.title,
            severityCode = calculation.severity.code,
            basePenaltyPoints = calculation.basePenaltyPoints,
            escalationBonusPoints = calculation.escalationBonusPoints,
            totalPenaltyPoints = calculation.totalPenaltyPoints,
            penaltyStatusName = if (calculation.isEscalated) PenaltyStatus.ESCALATED.name else PenaltyStatus.ACTIVE.name,
            reason = calculation.reason,
            policyVersion = calculation.policyVersion,
            createdAtEpochMs = nowEpochMs,
            resolvedAtEpochMs = null
        )

        // 7. Execute atomic database transaction
        val allocationResult: PenaltyAllocationResult = database.withTransaction {
            // Re-verify inside transaction to eliminate race conditions
            val doubleCheck = database.penaltyDao().getPenaltyByFailureRecordId(failureRecordId)
            if (doubleCheck != null) {
                val acc = database.penaltyAccountDao().getAccountOnce(doubleCheck.accountOwnerId)
                return@withTransaction PenaltyAllocationResult(
                    success = true,
                    penalty = doubleCheck.toDomain(),
                    newBalance = acc?.currentBalance ?: 0,
                    message = "Concurrent duplicate allocation prevented.",
                    isDuplicate = true
                )
            }

            // Insert penalty
            database.penaltyDao().insertPenalty(penaltyEntity)

            // Retrieve or initialize account
            val account = database.penaltyAccountDao().getAccountOnce(failureRecord.accountOwnerId)
            val currentBalance = account?.currentBalance ?: 0
            val lifetimeAccrued = account?.lifetimePointsAccrued ?: 0
            val activeCount = account?.activePenaltiesCount ?: 0

            val newBalance = currentBalance + calculation.totalPenaltyPoints
            val newLifetime = lifetimeAccrued + calculation.totalPenaltyPoints
            val newActiveCount = activeCount + 1

            // Insert transaction record
            val transactionEntity = PenaltyTransactionEntity(
                transactionId = UUID.randomUUID().toString(),
                accountOwnerId = failureRecord.accountOwnerId,
                penaltyId = penaltyId,
                transactionTypeName = PenaltyTransactionType.ALLOCATION.name,
                pointsDelta = calculation.totalPenaltyPoints,
                balanceAfter = newBalance,
                reason = calculation.reason,
                timestampEpochMs = nowEpochMs,
                idempotencyKey = idempotencyKey
            )
            database.penaltyTransactionDao().insertTransaction(transactionEntity)

            // Update account
            database.penaltyAccountDao().upsertAccount(
                PenaltyPointAccountEntity(
                    accountOwnerId = failureRecord.accountOwnerId,
                    currentBalance = newBalance,
                    lifetimePointsAccrued = newLifetime,
                    activePenaltiesCount = newActiveCount,
                    lastUpdatedEpochMs = nowEpochMs
                )
            )

            PenaltyAllocationResult(
                success = true,
                penalty = penaltyEntity.toDomain(),
                transaction = transactionEntity.toDomain(),
                newBalance = newBalance,
                message = "Penalty of ${calculation.totalPenaltyPoints} PP successfully allocated."
            )
        }

        // 8. Publish system events post-transaction
        if (allocationResult.success && !allocationResult.isDuplicate) {
            systemEventRepository.publishEvent(
                eventType = SystemEventType.PENALTY_ALLOCATED,
                relatedEntityId = penaltyId,
                metadata = mapOf(
                    "accountOwnerId" to failureRecord.accountOwnerId,
                    "points" to calculation.totalPenaltyPoints.toString(),
                    "severity" to calculation.severity.code,
                    "questTitle" to quest.title
                )
            )

            if (calculation.isEscalated) {
                systemEventRepository.publishEvent(
                    eventType = SystemEventType.PENALTY_ESCALATED,
                    relatedEntityId = penaltyId,
                    metadata = mapOf(
                        "bonusPoints" to calculation.escalationBonusPoints.toString(),
                        "escalationReason" to (calculation.escalationReason ?: "")
                    )
                )
            }

            systemEventRepository.publishEvent(
                eventType = SystemEventType.PENALTY_BALANCE_UPDATED,
                relatedEntityId = failureRecord.accountOwnerId,
                metadata = mapOf(
                    "newBalance" to allocationResult.newBalance.toString()
                )
            )
        }

        return allocationResult
    }

    /**
     * Observes the real-time Penalty Point balance for the account.
     */
    fun observeAccountBalance(accountOwnerId: String): Flow<PenaltyPointBalance> {
        return database.penaltyAccountDao().observeAccount(accountOwnerId).map { entity ->
            entity?.toDomain() ?: PenaltyPointBalance(accountOwnerId = accountOwnerId)
        }
    }

    /**
     * Observes active (outstanding) penalties for the account.
     */
    fun observeActivePenalties(accountOwnerId: String): Flow<List<PenaltyRecord>> {
        return database.penaltyDao().observeActivePenalties(accountOwnerId).map { list ->
            list.map { it.toDomain() }
        }
    }

    /**
     * Observes all historical penalties for the account.
     */
    fun observeAllPenalties(accountOwnerId: String): Flow<List<PenaltyRecord>> {
        return database.penaltyDao().observeAllPenalties(accountOwnerId).map { list ->
            list.map { it.toDomain() }
        }
    }

    /**
     * Observes recent ledger transactions.
     */
    fun observeRecentTransactions(accountOwnerId: String, limit: Int = 50): Flow<List<PenaltyTransaction>> {
        return database.penaltyTransactionDao().observeRecentTransactions(accountOwnerId, limit).map { list ->
            list.map { it.toDomain() }
        }
    }

    /**
     * Retrieves an active or historical penalty record associated with a specific commitment.
     */
    suspend fun getPenaltyByCommitmentId(commitmentId: String): PenaltyRecord? {
        return database.penaltyDao().getPenaltyByCommitmentId(commitmentId)?.toDomain()
    }

    /**
     * Atomically reverses an allocated penalty due to an approved retroactive exemption.
     * Inserts an auditable REVERSAL transaction into the double-entry ledger,
     * updates the account balance without falling below 0, decrements active penalty count,
     * and transitions the penalty record status to EXEMPTED.
     * Emits PENALTY_REVERSED_DUE_TO_EXEMPTION and PENALTY_BALANCE_UPDATED.
     */
    suspend fun reversePenaltyDueToExemption(
        penaltyId: String,
        exemptionId: String,
        reason: String,
        nowEpochMs: Long = System.currentTimeMillis()
    ): Result<PenaltyTransaction> {
        val penalty = database.penaltyDao().getPenaltyById(penaltyId)
            ?: return Result.failure(IllegalArgumentException("Penalty $penaltyId not found"))

        if (penalty.penaltyStatusName == PenaltyStatus.EXEMPTED.name) {
            val existingTx = database.penaltyTransactionDao().getTransactionsForPenalty(penaltyId)
                .find { it.transactionTypeName == PenaltyTransactionType.REVERSAL.name }
            if (existingTx != null) {
                return Result.success(existingTx.toDomain())
            }
        }

        val idempotencyKey = "tx_reversal_${penalty.penaltyId}_${exemptionId}"
        val existingTx = database.penaltyTransactionDao().getTransactionByIdempotencyKey(idempotencyKey)
        if (existingTx != null) {
            return Result.success(existingTx.toDomain())
        }

        var resultingTransaction: PenaltyTransaction? = null
        var newAccountBalance = 0
        var actualPointsDeducted = 0

        database.withTransaction {
            val account = database.penaltyAccountDao().getAccountOnce(penalty.accountOwnerId)
                ?: PenaltyPointAccountEntity(
                    accountOwnerId = penalty.accountOwnerId,
                    currentBalance = 0,
                    lifetimePointsAccrued = 0,
                    activePenaltiesCount = 0,
                    lastUpdatedEpochMs = nowEpochMs
                )

            // Calculate actual remaining outstanding points for this penalty
            val penaltyTransactions = database.penaltyTransactionDao().getTransactionsForPenalty(penalty.penaltyId)
            val allocatedPoints = penaltyTransactions
                .filter { it.transactionTypeName == PenaltyTransactionType.ALLOCATION.name }
                .sumOf { it.pointsDelta }
            val alreadyDeductedPoints = penaltyTransactions
                .filter {
                    it.transactionTypeName == PenaltyTransactionType.REDEMPTION_RESOLUTION.name ||
                            it.transactionTypeName == PenaltyTransactionType.REVERSAL.name
                }
                .sumOf { it.pointsDelta }
            val outstandingPoints = (allocatedPoints - alreadyDeductedPoints).coerceAtLeast(0)

            actualPointsDeducted = outstandingPoints
            val newBalance = (account.currentBalance - outstandingPoints).coerceAtLeast(0)
            val isCurrentlyActive = penalty.penaltyStatusName in setOf(PenaltyStatus.ACTIVE.name, PenaltyStatus.ESCALATED.name)
            val newActiveCount = if (isCurrentlyActive) (account.activePenaltiesCount - 1).coerceAtLeast(0) else account.activePenaltiesCount

            if (outstandingPoints > 0) {
                val transactionEntity = PenaltyTransactionEntity(
                    transactionId = UUID.randomUUID().toString(),
                    accountOwnerId = penalty.accountOwnerId,
                    penaltyId = penalty.penaltyId,
                    transactionTypeName = PenaltyTransactionType.REVERSAL.name,
                    pointsDelta = outstandingPoints,
                    balanceAfter = newBalance,
                    reason = "Reversal due to approved exemption: $reason",
                    idempotencyKey = idempotencyKey,
                    timestampEpochMs = nowEpochMs
                )
                database.penaltyTransactionDao().insertTransaction(transactionEntity)
                resultingTransaction = transactionEntity.toDomain()
            }

            database.penaltyAccountDao().upsertAccount(
                account.copy(
                    currentBalance = newBalance,
                    activePenaltiesCount = newActiveCount,
                    lastUpdatedEpochMs = nowEpochMs
                )
            )

            database.penaltyDao().updatePenaltyStatus(
                penaltyId = penalty.penaltyId,
                statusName = PenaltyStatus.EXEMPTED.name,
                resolvedAtEpochMs = nowEpochMs
            )

            newAccountBalance = newBalance
        }

        // Post-transaction events
        if (actualPointsDeducted > 0) {
            systemEventRepository.publishEvent(
                eventType = SystemEventType.PENALTY_REVERSED_DUE_TO_EXEMPTION,
                relatedEntityId = penalty.penaltyId,
                metadata = mapOf(
                    "accountOwnerId" to penalty.accountOwnerId,
                    "exemptionId" to exemptionId,
                    "reversedPoints" to actualPointsDeducted.toString(),
                    "newBalance" to newAccountBalance.toString(),
                    "reason" to reason
                )
            )

            systemEventRepository.publishEvent(
                eventType = SystemEventType.PENALTY_BALANCE_UPDATED,
                relatedEntityId = penalty.accountOwnerId,
                metadata = mapOf(
                    "newBalance" to newAccountBalance.toString()
                )
            )
        }

        return Result.success(resultingTransaction ?: PenaltyTransaction(
            transactionId = "noop_reversal_${penalty.penaltyId}",
            accountOwnerId = penalty.accountOwnerId,
            penaltyId = penalty.penaltyId,
            transactionType = PenaltyTransactionType.REVERSAL,
            pointsDelta = 0,
            balanceAfter = newAccountBalance,
            reason = "Penalty already resolved or 0 outstanding points.",
            idempotencyKey = idempotencyKey,
            timestampEpochMs = nowEpochMs
        ))
    }

    /**
     * Reconciles the account balance against the full ledger transaction history.
     * Guarantees mathematical ledger integrity.
     */
    suspend fun reconcileAccountBalance(accountOwnerId: String): Int {
        val transactions = database.penaltyTransactionDao().getAllTransactionsOnce(accountOwnerId)
        val computedBalance = transactions.sumOf { tx ->
            val type = PenaltyTransactionType.fromName(tx.transactionTypeName)
            if (type.isDebit) -tx.pointsDelta else tx.pointsDelta
        }.coerceAtLeast(0)

        val activeCount = database.penaltyDao().getActivePenaltiesCount(accountOwnerId)
        val lifetimeAccrued = transactions.filter {
            PenaltyTransactionType.fromName(it.transactionTypeName) == PenaltyTransactionType.ALLOCATION
        }.sumOf { it.pointsDelta }

        database.penaltyAccountDao().upsertAccount(
            PenaltyPointAccountEntity(
                accountOwnerId = accountOwnerId,
                currentBalance = computedBalance,
                lifetimePointsAccrued = lifetimeAccrued,
                activePenaltiesCount = activeCount,
                lastUpdatedEpochMs = System.currentTimeMillis()
            )
        )
        return computedBalance
    }
}
