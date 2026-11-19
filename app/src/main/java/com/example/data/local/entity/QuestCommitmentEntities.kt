package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.CommitmentState
import com.example.domain.model.ConfirmedFailureRecord
import com.example.domain.model.ExemptionLifecycleState
import com.example.domain.model.ExemptionReasonCategory
import com.example.domain.model.QuestCommitment
import com.example.domain.model.QuestExemption
import com.example.domain.model.QuestRescheduleRecord
import com.example.domain.model.VerificationStatus

@Entity(
    tableName = "quest_commitments",
    foreignKeys = [
        ForeignKey(
            entity = QuestEntity::class,
            parentColumns = ["id"],
            childColumns = ["questId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["questId", "logicalCycleId"], unique = true),
        Index(value = ["accountOwnerId"]),
        Index(value = ["stateName"]),
        Index(value = ["deadlineEpochMs"])
    ]
)
data class QuestCommitmentEntity(
    @PrimaryKey val commitmentId: String,
    val questId: Long,
    val accountOwnerId: String,
    val logicalCycleId: String,
    val stateName: String,
    val acceptedAtEpochMs: Long?,
    val deadlineEpochMs: Long,
    val gracePeriodMinutes: Int,
    val gracePeriodExpiresAtEpochMs: Long,
    val rescheduleCount: Int,
    val originalDeadlineEpochMs: Long,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)

@Entity(
    tableName = "confirmed_quest_failures",
    indices = [
        Index(value = ["commitmentId"], unique = true),
        Index(value = ["questId"]),
        Index(value = ["logicalCycleId"]),
        Index(value = ["accountOwnerId"])
    ]
)
data class ConfirmedFailureRecordEntity(
    @PrimaryKey val failureId: String,
    val commitmentId: String,
    val questId: Long,
    val logicalCycleId: String,
    val accountOwnerId: String,
    val deadlineEpochMs: Long,
    val gracePeriodExpiresAtEpochMs: Long,
    val confirmedAtEpochMs: Long,
    val failureReasonCode: String,
    val policyVersion: Int,
    val exemptionEvaluated: Boolean
)

@Entity(
    tableName = "quest_exemptions",
    indices = [
        Index(value = ["commitmentId"]),
        Index(value = ["accountOwnerId"]),
        Index(value = ["lifecycleStateName"])
    ]
)
data class QuestExemptionEntity(
    @PrimaryKey val exemptionId: String,
    val commitmentId: String,
    val questId: Long,
    val accountOwnerId: String,
    val categoryName: String,
    val lifecycleStateName: String,
    val explanation: String,
    val requestedAtEpochMs: Long,
    val reviewedAtEpochMs: Long?,
    val reviewerNotes: String?
)

@Entity(
    tableName = "quest_reschedules",
    indices = [
        Index(value = ["commitmentId"])
    ]
)
data class QuestRescheduleEntity(
    @PrimaryKey val rescheduleId: String,
    val commitmentId: String,
    val questId: Long,
    val previousDeadlineEpochMs: Long,
    val revisedDeadlineEpochMs: Long,
    val reasonCategory: String,
    val rescheduledAtEpochMs: Long
)

// Extension mappers
fun QuestCommitmentEntity.toDomain(verificationStatus: VerificationStatus = VerificationStatus.SELF_REPORTED): QuestCommitment =
    QuestCommitment(
        commitmentId = commitmentId,
        questId = questId,
        accountOwnerId = accountOwnerId,
        logicalCycleId = logicalCycleId,
        state = runCatching { CommitmentState.valueOf(stateName) }.getOrDefault(CommitmentState.NOT_ACCEPTED),
        acceptedAtEpochMs = acceptedAtEpochMs,
        deadlineEpochMs = deadlineEpochMs,
        gracePeriodMinutes = gracePeriodMinutes,
        gracePeriodExpiresAtEpochMs = gracePeriodExpiresAtEpochMs,
        rescheduleCount = rescheduleCount,
        originalDeadlineEpochMs = originalDeadlineEpochMs,
        verificationStatus = verificationStatus
    )

fun QuestCommitment.toEntity(): QuestCommitmentEntity =
    QuestCommitmentEntity(
        commitmentId = commitmentId,
        questId = questId,
        accountOwnerId = accountOwnerId,
        logicalCycleId = logicalCycleId,
        stateName = state.name,
        acceptedAtEpochMs = acceptedAtEpochMs,
        deadlineEpochMs = deadlineEpochMs,
        gracePeriodMinutes = gracePeriodMinutes,
        gracePeriodExpiresAtEpochMs = gracePeriodExpiresAtEpochMs,
        rescheduleCount = rescheduleCount,
        originalDeadlineEpochMs = originalDeadlineEpochMs,
        createdAtEpochMs = acceptedAtEpochMs ?: System.currentTimeMillis(),
        updatedAtEpochMs = System.currentTimeMillis()
    )

fun ConfirmedFailureRecordEntity.toDomain(): ConfirmedFailureRecord =
    ConfirmedFailureRecord(
        failureId = failureId,
        commitmentId = commitmentId,
        questId = questId,
        logicalCycleId = logicalCycleId,
        accountOwnerId = accountOwnerId,
        deadlineEpochMs = deadlineEpochMs,
        gracePeriodExpiresAtEpochMs = gracePeriodExpiresAtEpochMs,
        confirmedAtEpochMs = confirmedAtEpochMs,
        failureReasonCode = failureReasonCode,
        policyVersion = policyVersion,
        exemptionEvaluated = exemptionEvaluated
    )

fun ConfirmedFailureRecord.toEntity(): ConfirmedFailureRecordEntity =
    ConfirmedFailureRecordEntity(
        failureId = failureId,
        commitmentId = commitmentId,
        questId = questId,
        logicalCycleId = logicalCycleId,
        accountOwnerId = accountOwnerId,
        deadlineEpochMs = deadlineEpochMs,
        gracePeriodExpiresAtEpochMs = gracePeriodExpiresAtEpochMs,
        confirmedAtEpochMs = confirmedAtEpochMs,
        failureReasonCode = failureReasonCode,
        policyVersion = policyVersion,
        exemptionEvaluated = exemptionEvaluated
    )

fun QuestExemptionEntity.toDomain(): QuestExemption =
    QuestExemption(
        exemptionId = exemptionId,
        commitmentId = commitmentId,
        questId = questId,
        accountOwnerId = accountOwnerId,
        category = runCatching { ExemptionReasonCategory.valueOf(categoryName) }.getOrDefault(ExemptionReasonCategory.EMERGENCY),
        lifecycleState = runCatching { ExemptionLifecycleState.valueOf(lifecycleStateName) }.getOrDefault(ExemptionLifecycleState.PENDING_REVIEW),
        explanation = explanation,
        requestedAtEpochMs = requestedAtEpochMs,
        reviewedAtEpochMs = reviewedAtEpochMs,
        reviewerNotes = reviewerNotes
    )

fun QuestExemption.toEntity(): QuestExemptionEntity =
    QuestExemptionEntity(
        exemptionId = exemptionId,
        commitmentId = commitmentId,
        questId = questId,
        accountOwnerId = accountOwnerId,
        categoryName = category.name,
        lifecycleStateName = lifecycleState.name,
        explanation = explanation,
        requestedAtEpochMs = requestedAtEpochMs,
        reviewedAtEpochMs = reviewedAtEpochMs,
        reviewerNotes = reviewerNotes
    )

fun QuestRescheduleEntity.toDomain(): QuestRescheduleRecord =
    QuestRescheduleRecord(
        rescheduleId = rescheduleId,
        commitmentId = commitmentId,
        questId = questId,
        previousDeadlineEpochMs = previousDeadlineEpochMs,
        revisedDeadlineEpochMs = revisedDeadlineEpochMs,
        reasonCategory = reasonCategory,
        rescheduledAtEpochMs = rescheduledAtEpochMs
    )

fun QuestRescheduleRecord.toEntity(): QuestRescheduleEntity =
    QuestRescheduleEntity(
        rescheduleId = rescheduleId,
        commitmentId = commitmentId,
        questId = questId,
        previousDeadlineEpochMs = previousDeadlineEpochMs,
        revisedDeadlineEpochMs = revisedDeadlineEpochMs,
        reasonCategory = reasonCategory,
        rescheduledAtEpochMs = rescheduledAtEpochMs
    )
