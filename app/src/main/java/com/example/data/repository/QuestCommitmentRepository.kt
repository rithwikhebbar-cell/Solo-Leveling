package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.SoloLevelingDatabase
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
    suspend fun requestExemption(
        commitmentId: String,
        category: ExemptionReasonCategory,
        explanation: String,
        accountOwnerId: String
    ): Result<QuestExemption> {
        return database.withTransaction {
            val commitment = commitmentDao.getCommitmentById(commitmentId)
                ?: return@withTransaction Result.failure(IllegalArgumentException("Commitment $commitmentId not found"))

            if (commitment.stateName == CommitmentState.COMPLETED.name) {
                return@withTransaction Result.failure(IllegalStateException("Completed commitments do not require exemption"))
            }
            if (commitment.stateName == CommitmentState.FAILED.name) {
                return@withTransaction Result.failure(IllegalStateException("Cannot request exemption for already confirmed failure"))
            }

            val exemption = QuestExemption(
                exemptionId = UUID.randomUUID().toString(),
                commitmentId = commitmentId,
                questId = commitment.questId,
                accountOwnerId = accountOwnerId,
                category = category,
                lifecycleState = ExemptionLifecycleState.PENDING_REVIEW,
                explanation = explanation.trim(),
                requestedAtEpochMs = clock.nowEpochMs()
            )
            exemptionDao.insertExemption(exemption.toEntity())
            Result.success(exemption)
        }
    }

    /**
     * Reviews and decides on an exemption request.
     */
    suspend fun reviewExemption(
        exemptionId: String,
        isApproved: Boolean,
        reviewerNotes: String? = null
    ): Result<QuestExemption> {
        return database.withTransaction {
            val exemptionEntity = exemptionDao.getExemptionById(exemptionId)
                ?: return@withTransaction Result.failure(IllegalArgumentException("Exemption $exemptionId not found"))

            val now = clock.nowEpochMs()
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
                systemEventRepository.publishEvent(
                    eventType = SystemEventType.PENALTY_EXEMPTED,
                    relatedEntityId = exemptionEntity.commitmentId,
                    sourceModule = "PENALTY",
                    metadata = mapOf(
                        "commitmentId" to exemptionEntity.commitmentId,
                        "questId" to exemptionEntity.questId.toString(),
                        "category" to exemptionEntity.categoryName,
                        "idempotencyKey" to "exemption_${exemptionEntity.commitmentId}"
                    ),
                    processImmediately = false,
                    nowEpochMs = now
                )
            }

            val updated = exemptionDao.getExemptionById(exemptionId)!!.toDomain()
            Result.success(updated)
        }
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
