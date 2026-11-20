package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.entity.PenaltyPointAccountEntity
import com.example.data.local.entity.PenaltyTransactionEntity
import com.example.data.local.entity.toDomain
import com.example.data.local.entity.toEntity
import com.example.domain.engine.DeadlineCalculator
import com.example.domain.engine.GracePeriodEngine
import com.example.domain.engine.QuestFailureDetectionEngine
import com.example.domain.model.Clock
import com.example.domain.model.CommitmentState
import com.example.domain.model.ConfirmedFailureRecord
import com.example.domain.model.ExemptionLifecycleState
import com.example.domain.model.ExemptionReasonCategory
import com.example.domain.model.FailureDecisionOutcome
import com.example.domain.model.PenaltyStatus
import com.example.domain.model.PenaltyTransactionType
import com.example.domain.model.QuestCategory
import com.example.domain.model.QuestCommitment
import com.example.domain.model.QuestExemption
import com.example.domain.model.QuestRescheduleRecord
import com.example.domain.model.QuestType
import com.example.domain.model.RecoveryState
import com.example.domain.model.SystemClock
import com.example.domain.model.SystemEventType
import com.example.domain.model.VerificationStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.ZoneId
import java.util.UUID

/**
 * Repository coordinating quest commitments, explicit acceptance, deadlines,
 * grace periods, exemptions, rescheduling, and persistent failure detection.
 */
class QuestCommitmentRepository(
    private val database: SoloLevelingDatabase,
    private val systemEventRepository: SystemEventRepository,
    private val clock: Clock = SystemClock
) {
    private val commitmentDao = database.questCommitmentDao()
    private val failureDao = database.confirmedFailureDao()
    private val exemptionDao = database.questExemptionDao()
    private val rescheduleDao = database.questRescheduleDao()
    private val questDao = database.questDao()

    fun observeCommitmentsForCycle(logicalCycleId: String): Flow<List<QuestCommitment>> {
        return commitmentDao.observeCommitmentsForCycle(logicalCycleId).map { list ->
            list.map { it.toDomain() }
        }
    }

    fun observeAllCommitments(accountOwnerId: String): Flow<List<QuestCommitment>> {
        return commitmentDao.observeAllCommitments(accountOwnerId).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun getAllCommitmentsOnce(accountOwnerId: String): List<QuestCommitment> {
        return commitmentDao.getAllCommitmentsOnce(accountOwnerId).map { it.toDomain() }
    }

    suspend fun getCommitmentForQuestAndCycle(questId: Long, logicalCycleId: String): QuestCommitment? {
        return commitmentDao.getCommitmentForQuestAndCycle(questId, logicalCycleId)?.toDomain()
    }

    suspend fun getCommitmentById(commitmentId: String): QuestCommitment? {
        return commitmentDao.getCommitmentById(commitmentId)?.toDomain()
    }

    fun observeAllFailures(): Flow<List<ConfirmedFailureRecord>> {
        return failureDao.observeAllFailures().map { list -> list.map { it.toDomain() } }
    }

    suspend fun getFailuresForCycle(logicalCycleId: String): List<ConfirmedFailureRecord> {
        return failureDao.getFailuresForCycle(logicalCycleId).map { it.toDomain() }
    }

    suspend fun getFailureCount(): Int = failureDao.getFailureCount()

    /**
     * Explicitly accepts a quest commitment for a specific logical cycle.
     * Prevents accidental or background acceptance.
     */
    suspend fun acceptQuestCommitment(
        questId: Long,
        logicalCycleId: String,
        accountOwnerId: String,
        targetEpochDay: Long,
        dailyResetHour: Int = 4,
        preferredTimezoneId: String = ZoneId.systemDefault().id,
        gracePeriodMinutes: Int = QuestCommitment.DEFAULT_GRACE_PERIOD_MINUTES,
        customDeadlineEpochMs: Long? = null
    ): Result<QuestCommitment> {
        return database.withTransaction {
            val quest = questDao.getQuestById(questId)
                ?: return@withTransaction Result.failure(IllegalArgumentException("Quest $questId not found"))

            val existing = commitmentDao.getCommitmentForQuestAndCycle(questId, logicalCycleId)
            if (existing != null && existing.stateName != CommitmentState.NOT_ACCEPTED.name) {
                return@withTransaction Result.success(existing.toDomain())
            }

            val zone = runCatching { ZoneId.of(preferredTimezoneId) }.getOrElse { ZoneId.systemDefault() }
            val qType = runCatching { QuestType.valueOf(quest.typeName) }.getOrDefault(QuestType.DAILY)
            val deadline = customDeadlineEpochMs ?: DeadlineCalculator.calculateDeadlineForType(
                type = qType,
                targetEpochDay = targetEpochDay,
                dailyResetHour = dailyResetHour,
                zoneId = zone
            )
            val safeGraceMinutes = gracePeriodMinutes.coerceIn(
                QuestCommitment.MIN_GRACE_PERIOD_MINUTES,
                QuestCommitment.MAX_GRACE_PERIOD_MINUTES
            )
            val graceExpiry = GracePeriodEngine.calculateGraceExpiry(deadline, safeGraceMinutes)
            val now = clock.nowEpochMs()

            val commitmentId = existing?.commitmentId ?: QuestCommitment.buildCommitmentId(questId, logicalCycleId)
            val commitment = QuestCommitment(
                commitmentId = commitmentId,
                questId = questId,
                accountOwnerId = accountOwnerId,
                logicalCycleId = logicalCycleId,
                state = CommitmentState.ACCEPTED,
                acceptedAtEpochMs = now,
                deadlineEpochMs = deadline,
                gracePeriodMinutes = safeGraceMinutes,
                gracePeriodExpiresAtEpochMs = graceExpiry,
                rescheduleCount = existing?.rescheduleCount ?: 0,
                originalDeadlineEpochMs = existing?.originalDeadlineEpochMs ?: deadline
            )

            commitmentDao.upsertCommitment(commitment.toEntity())
            Result.success(commitment)
        }
    }

    /**
     * Reschedules an accepted commitment to a revised deadline.
     * Preserves original deadline in auditable history and respects MAX_ALLOWED_RESCHEDULES.
     */
    suspend fun requestReschedule(
        commitmentId: String,
        revisedDeadlineEpochMs: Long,
        reasonCategory: String
    ): Result<QuestCommitment> {
        return database.withTransaction {
            val entity = commitmentDao.getCommitmentById(commitmentId)
                ?: return@withTransaction Result.failure(IllegalArgumentException("Commitment $commitmentId not found"))

            if (entity.stateName == CommitmentState.COMPLETED.name) {
                return@withTransaction Result.failure(IllegalStateException("Completed commitment cannot be rescheduled"))
            }
            if (entity.stateName == CommitmentState.FAILED.name) {
                return@withTransaction Result.failure(IllegalStateException("Failed commitment cannot be retroactively rescheduled"))
            }
            if (entity.rescheduleCount >= QuestCommitment.MAX_ALLOWED_RESCHEDULES) {
                return@withTransaction Result.failure(IllegalStateException("Maximum reschedules (${QuestCommitment.MAX_ALLOWED_RESCHEDULES}) reached"))
            }
            val now = clock.nowEpochMs()
            if (revisedDeadlineEpochMs <= now) {
                return@withTransaction Result.failure(IllegalArgumentException("Revised deadline must be in the future"))
            }

            val newGraceExpiry = GracePeriodEngine.calculateGraceExpiry(revisedDeadlineEpochMs, entity.gracePeriodMinutes)
            val newCount = entity.rescheduleCount + 1

            // 1. Record auditable reschedule history
            val record = QuestRescheduleRecord(
                rescheduleId = UUID.randomUUID().toString(),
                commitmentId = commitmentId,
                questId = entity.questId,
                previousDeadlineEpochMs = entity.deadlineEpochMs,
                revisedDeadlineEpochMs = revisedDeadlineEpochMs,
                reasonCategory = reasonCategory.trim().ifBlank { "PLAYER_REQUESTED" },
                rescheduledAtEpochMs = now
            )
            rescheduleDao.insertReschedule(record.toEntity())

            // 2. Update commitment
            commitmentDao.updateCommitmentDeadline(
                commitmentId = commitmentId,
                newDeadlineEpochMs = revisedDeadlineEpochMs,
                newGraceExpiryEpochMs = newGraceExpiry,
                rescheduleCount = newCount,
                stateName = CommitmentState.RESCHEDULED.name,
                updatedAtEpochMs = now
            )

            val updated = commitmentDao.getCommitmentById(commitmentId)!!.toDomain()
            Result.success(updated)
        }
    }

    /**
     * Requests an exemption for an active commitment.
     * Transitions exemption to PENDING_REVIEW which pauses failure confirmation.
     */
    /**
     * Submits an emergency exemption request (preventive or retroactive).
     * Transitions exemption to PENDING_REVIEW which pauses failure confirmation.
     */
    suspend fun requestExemption(
        commitmentId: String,
        category: ExemptionReasonCategory,
        explanation: String,
        accountOwnerId: String
    ): Result<QuestExemption> {
        val now = clock.nowEpochMs()
        val result = database.withTransaction {
            val commitment = commitmentDao.getCommitmentById(commitmentId)
                ?: return@withTransaction Result.failure(IllegalArgumentException("Commitment $commitmentId not found"))

            if (commitment.stateName == CommitmentState.COMPLETED.name) {
                return@withTransaction Result.failure(IllegalStateException("Completed commitments do not require exemption"))
            }

            val exemption = QuestExemption(
                exemptionId = UUID.randomUUID().toString(),
                commitmentId = commitmentId,
                questId = commitment.questId,
                accountOwnerId = accountOwnerId,
                category = category,
                lifecycleState = ExemptionLifecycleState.PENDING_REVIEW,
                explanation = explanation.trim(),
                requestedAtEpochMs = now
            )
            exemptionDao.insertExemption(exemption.toEntity())
            Result.success(exemption)
        }

        if (result.isSuccess) {
            val exemption = result.getOrThrow()
            systemEventRepository.publishEvent(
                eventType = SystemEventType.EXEMPTION_REQUEST_CREATED,
                relatedEntityId = exemption.exemptionId,
                sourceModule = "PENALTY",
                metadata = mapOf(
                    "commitmentId" to commitmentId,
                    "questId" to exemption.questId.toString(),
                    "category" to category.name,
                    "accountOwnerId" to accountOwnerId
                ),
                processImmediately = false,
                nowEpochMs = now
            )
        }
        return result
    }

    /**
     * Withdraws an active exemption request if still under review or requested.
     */
    suspend fun withdrawExemption(
        exemptionId: String
    ): Result<QuestExemption> {
        val now = clock.nowEpochMs()
        val result = database.withTransaction {
            val exemptionEntity = exemptionDao.getExemptionById(exemptionId)
                ?: return@withTransaction Result.failure(IllegalArgumentException("Exemption $exemptionId not found"))

            if (exemptionEntity.lifecycleStateName in setOf(ExemptionLifecycleState.APPROVED.name, ExemptionLifecycleState.DENIED.name, ExemptionLifecycleState.CLOSED.name)) {
                return@withTransaction Result.failure(IllegalStateException("Cannot withdraw a finalized exemption (${exemptionEntity.lifecycleStateName})"))
            }

            exemptionDao.updateExemptionStatus(
                exemptionId = exemptionId,
                lifecycleStateName = ExemptionLifecycleState.WITHDRAWN.name,
                reviewedAtEpochMs = now,
                reviewerNotes = "Withdrawn by player"
            )

            val updated = exemptionDao.getExemptionById(exemptionId)!!.toDomain()
            Result.success(updated)
        }

        if (result.isSuccess) {
            val exemption = result.getOrThrow()
            systemEventRepository.publishEvent(
                eventType = SystemEventType.EXEMPTION_WITHDRAWN,
                relatedEntityId = exemption.exemptionId,
                sourceModule = "PENALTY",
                metadata = mapOf(
                    "commitmentId" to exemption.commitmentId,
                    "questId" to exemption.questId.toString(),
                    "accountOwnerId" to exemption.accountOwnerId
                ),
                processImmediately = false,
                nowEpochMs = now
            )
        }
        return result
    }

    /**
     * Reviews and decides on an exemption request.
     * If approved, updates commitment state to EXEMPTED, and if an associated penalty was
     * already allocated (retroactive exemption), executes an auditable REVERSAL transaction
     * on the double-entry ledger within the same atomic Room transaction.
     */
    suspend fun reviewExemption(
        exemptionId: String,
        isApproved: Boolean,
        reviewerNotes: String? = null
    ): Result<QuestExemption> {
        val now = clock.nowEpochMs()
        var penaltyReversed = false
        var reversedPenaltyId = ""
        var reversedPoints = 0
        var newAccountBal = 0
        var penaltyOwnerId = ""

        val result = database.withTransaction {
            val exemptionEntity = exemptionDao.getExemptionById(exemptionId)
                ?: return@withTransaction Result.failure(IllegalArgumentException("Exemption $exemptionId not found"))

            val newState = if (isApproved) ExemptionLifecycleState.APPROVED else ExemptionLifecycleState.DENIED
            exemptionDao.updateExemptionStatus(
                exemptionId = exemptionId,
                lifecycleStateName = newState.name,
                reviewedAtEpochMs = now,
                reviewerNotes = reviewerNotes
            )

            if (isApproved) {
                commitmentDao.updateCommitmentState(
                    commitmentId = exemptionEntity.commitmentId,
                    stateName = CommitmentState.EXEMPTED.name,
                    updatedAtEpochMs = now
                )

                // Check for retroactive penalty reversal
                val penalty = database.penaltyDao().getPenaltyByCommitmentId(exemptionEntity.commitmentId)
                if (penalty != null && penalty.penaltyStatusName != PenaltyStatus.EXEMPTED.name) {
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

                    val account = database.penaltyAccountDao().getAccountOnce(penalty.accountOwnerId)
                    val currentBal = account?.currentBalance ?: 0
                    val newBal = (currentBal - outstandingPoints).coerceAtLeast(0)
                    val isCurrentlyActive = penalty.penaltyStatusName in setOf(PenaltyStatus.ACTIVE.name, PenaltyStatus.ESCALATED.name)
                    val newActiveCount = if (isCurrentlyActive) ((account?.activePenaltiesCount ?: 1) - 1).coerceAtLeast(0) else (account?.activePenaltiesCount ?: 0)

                    if (outstandingPoints > 0) {
                        val tx = PenaltyTransactionEntity(
                            transactionId = UUID.randomUUID().toString(),
                            accountOwnerId = penalty.accountOwnerId,
                            penaltyId = penalty.penaltyId,
                            transactionTypeName = PenaltyTransactionType.REVERSAL.name,
                            pointsDelta = outstandingPoints,
                            balanceAfter = newBal,
                            reason = "Reversal due to approved retroactive exemption: ${exemptionEntity.explanation}",
                            idempotencyKey = "tx_reversal_${penalty.penaltyId}_${exemptionEntity.exemptionId}",
                            timestampEpochMs = now
                        )
                        database.penaltyTransactionDao().insertTransaction(tx)

                        penaltyReversed = true
                        reversedPenaltyId = penalty.penaltyId
                        reversedPoints = outstandingPoints
                        newAccountBal = newBal
                        penaltyOwnerId = penalty.accountOwnerId
                    }

                    if (account != null) {
                        database.penaltyAccountDao().upsertAccount(
                            account.copy(
                                currentBalance = newBal,
                                activePenaltiesCount = newActiveCount,
                                lastUpdatedEpochMs = now
                            )
                        )
                    }

                    database.penaltyDao().updatePenaltyStatus(
                        penaltyId = penalty.penaltyId,
                        statusName = PenaltyStatus.EXEMPTED.name,
                        resolvedAtEpochMs = now
                    )
                }
            }

            val updated = exemptionDao.getExemptionById(exemptionId)!!.toDomain()
            Result.success(updated)
        }

        if (result.isSuccess) {
            val updated = result.getOrThrow()
            if (isApproved) {
                systemEventRepository.publishEvent(
                    eventType = SystemEventType.EXEMPTION_APPROVED,
                    relatedEntityId = updated.exemptionId,
                    sourceModule = "PENALTY",
                    metadata = mapOf(
                        "commitmentId" to updated.commitmentId,
                        "questId" to updated.questId.toString(),
                        "category" to updated.category.name,
                        "accountOwnerId" to updated.accountOwnerId
                    ),
                    processImmediately = false,
                    nowEpochMs = now
                )
                systemEventRepository.publishEvent(
                    eventType = SystemEventType.PENALTY_EXEMPTED,
                    relatedEntityId = updated.commitmentId,
                    sourceModule = "PENALTY",
                    metadata = mapOf(
                        "commitmentId" to updated.commitmentId,
                        "questId" to updated.questId.toString(),
                        "category" to updated.category.name,
                        "idempotencyKey" to "exemption_${updated.commitmentId}"
                    ),
                    processImmediately = false,
                    nowEpochMs = now
                )

                if (penaltyReversed) {
                    systemEventRepository.publishEvent(
                        eventType = SystemEventType.PENALTY_REVERSED_DUE_TO_EXEMPTION,
                        relatedEntityId = reversedPenaltyId,
                        sourceModule = "PENALTY",
                        metadata = mapOf(
                            "accountOwnerId" to penaltyOwnerId,
                            "exemptionId" to updated.exemptionId,
                            "reversedPoints" to reversedPoints.toString(),
                            "newBalance" to newAccountBal.toString()
                        ),
                        processImmediately = false,
                        nowEpochMs = now
                    )
                    systemEventRepository.publishEvent(
                        eventType = SystemEventType.PENALTY_BALANCE_UPDATED,
                        relatedEntityId = penaltyOwnerId,
                        sourceModule = "PENALTY",
                        metadata = mapOf(
                            "newBalance" to newAccountBal.toString()
                        ),
                        processImmediately = false,
                        nowEpochMs = now
                    )
                }
            } else {
                systemEventRepository.publishEvent(
                    eventType = SystemEventType.EXEMPTION_DENIED,
                    relatedEntityId = updated.exemptionId,
                    sourceModule = "PENALTY",
                    metadata = mapOf(
                        "commitmentId" to updated.commitmentId,
                        "questId" to updated.questId.toString(),
                        "accountOwnerId" to updated.accountOwnerId,
                        "reviewerNotes" to (reviewerNotes ?: "")
                    ),
                    processImmediately = false,
                    nowEpochMs = now
                )
            }
        }

        return result
    }

    /**
     * Observes all exemption requests for an account.
     */
    fun observeAllExemptions(accountOwnerId: String): Flow<List<QuestExemption>> {
        return exemptionDao.observeAllExemptions(accountOwnerId).map { list ->
            list.map { it.toDomain() }
        }
    }

    /**
     * Retrieves all exemption requests for an account snapshot.
     */
    suspend fun getAllExemptionsOnce(accountOwnerId: String): List<QuestExemption> {
        return exemptionDao.getAllExemptionsOnce(accountOwnerId).map { it.toDomain() }
    }

    /**
     * Evaluates and confirms failures for all commitments of a closing logical cycle.
     * MUST execute atomically before daily reset wipes progress.
     */
    suspend fun evaluateAndRecordFailuresForCycle(
        logicalCycleId: String,
        triggerSource: String,
        nowEpochMs: Long = clock.nowEpochMs()
    ): List<ConfirmedFailureRecord> {
        return database.withTransaction {
            val commitments = commitmentDao.getCommitmentsForCycleOnce(logicalCycleId)
            val recoveryAssessment = database.recoveryAssessmentDao().getLatestOnce()
            val recoveryState = recoveryAssessment?.let {
                runCatching { RecoveryState.valueOf(it.stateName) }.getOrNull()
            }
            val confirmedFailures = mutableListOf<ConfirmedFailureRecord>()

            for (entity in commitments) {
                val quest = questDao.getQuestById(entity.questId)
                val isCompleted = quest?.isCompleted == true
                val verificationStatus = quest?.let {
                    runCatching { VerificationStatus.valueOf(it.verificationStatusName) }.getOrDefault(VerificationStatus.SELF_REPORTED)
                } ?: VerificationStatus.SELF_REPORTED
                val isPhysical = quest?.categoryName == QuestCategory.PHYSICAL.name

                val activeExemption = exemptionDao.getActiveExemptionForCommitment(entity.commitmentId)?.toDomain()
                val commitment = entity.toDomain(verificationStatus)

                val eval = QuestFailureDetectionEngine.evaluateCommitment(
                    commitment = commitment,
                    isQuestCompleted = isCompleted,
                    verificationStatus = verificationStatus,
                    recoveryState = recoveryState,
                    activeExemption = activeExemption,
                    isPhysicalQuest = isPhysical,
                    clock = { nowEpochMs }
                )

                when (eval.decision) {
                    FailureDecisionOutcome.FAILURE_CONFIRMED -> {
                        val failureRecord = ConfirmedFailureRecord(
                            failureId = UUID.randomUUID().toString(),
                            commitmentId = commitment.commitmentId,
                            questId = commitment.questId,
                            logicalCycleId = commitment.logicalCycleId,
                            accountOwnerId = commitment.accountOwnerId,
                            deadlineEpochMs = commitment.deadlineEpochMs,
                            gracePeriodExpiresAtEpochMs = commitment.gracePeriodExpiresAtEpochMs,
                            confirmedAtEpochMs = nowEpochMs,
                            failureReasonCode = eval.reasonCode,
                            policyVersion = 1,
                            exemptionEvaluated = activeExemption != null
                        )

                        // Idempotent write guarded by unique constraint on commitmentId
                        val rowId = failureDao.insertFailure(failureRecord.toEntity())
                        if (rowId != -1L) {
                            commitmentDao.updateCommitmentState(
                                commitmentId = commitment.commitmentId,
                                stateName = CommitmentState.FAILED.name,
                                updatedAtEpochMs = nowEpochMs
                            )
                            confirmedFailures.add(failureRecord)

                            systemEventRepository.publishEvent(
                                eventType = SystemEventType.QUEST_FAILURE_CONFIRMED,
                                relatedEntityId = commitment.commitmentId,
                                sourceModule = "PENALTY",
                                metadata = mapOf(
                                    "commitmentId" to commitment.commitmentId,
                                    "questId" to commitment.questId.toString(),
                                    "logicalCycleId" to commitment.logicalCycleId,
                                    "reason" to eval.reasonCode,
                                    "triggerSource" to triggerSource,
                                    "idempotencyKey" to "fail_${commitment.commitmentId}"
                                ),
                                processImmediately = false,
                                nowEpochMs = nowEpochMs
                            )
                        }
                    }
                    FailureDecisionOutcome.COMPLETED -> {
                        if (entity.stateName != CommitmentState.COMPLETED.name) {
                            commitmentDao.updateCommitmentState(
                                commitmentId = commitment.commitmentId,
                                stateName = CommitmentState.COMPLETED.name,
                                updatedAtEpochMs = nowEpochMs
                            )
                        }
                    }
                    FailureDecisionOutcome.EXEMPTED -> {
                        if (entity.stateName != CommitmentState.EXEMPTED.name) {
                            commitmentDao.updateCommitmentState(
                                commitmentId = commitment.commitmentId,
                                stateName = CommitmentState.EXEMPTED.name,
                                updatedAtEpochMs = nowEpochMs
                            )
                        }
                    }
                    FailureDecisionOutcome.GRACE_PERIOD_ACTIVE -> {
                        if (entity.stateName != CommitmentState.OVERDUE.name) {
                            commitmentDao.updateCommitmentState(
                                commitmentId = commitment.commitmentId,
                                stateName = CommitmentState.OVERDUE.name,
                                updatedAtEpochMs = nowEpochMs
                            )
                            systemEventRepository.publishEvent(
                                eventType = SystemEventType.QUEST_OVERDUE,
                                relatedEntityId = commitment.commitmentId,
                                sourceModule = "PENALTY",
                                metadata = mapOf(
                                    "commitmentId" to commitment.commitmentId,
                                    "questId" to commitment.questId.toString(),
                                    "graceExpiresAt" to commitment.gracePeriodExpiresAtEpochMs.toString(),
                                    "idempotencyKey" to "overdue_${commitment.commitmentId}"
                                ),
                                processImmediately = false,
                                nowEpochMs = nowEpochMs
                            )
                        }
                    }
                    else -> Unit
                }
            }
            confirmedFailures
        }
    }

    /**
     * Evaluates overdue commitments whose grace period expired during the day.
     * Can be invoked periodically by WorkManager.
     */
    suspend fun evaluateOverdueCommitments(nowEpochMs: Long = clock.nowEpochMs()): Int {
        return database.withTransaction {
            val overdueEntities = commitmentDao.getOverdueCommitmentsEligibleForFailure(nowEpochMs)
            var count = 0
            for (entity in overdueEntities) {
                val cycleList = evaluateAndRecordFailuresForCycle(
                    logicalCycleId = entity.logicalCycleId,
                    triggerSource = "PERIODIC_CHECK",
                    nowEpochMs = nowEpochMs
                )
                count += cycleList.size
            }
            count
        }
    }
}
