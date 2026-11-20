package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.PenaltyPointBalance
import com.example.domain.model.PenaltyRecord
import com.example.domain.model.PenaltySeverity
import com.example.domain.model.PenaltyStatus
import com.example.domain.model.PenaltyTransaction
import com.example.domain.model.PenaltyTransactionType

@Entity(
    tableName = "penalties",
    indices = [
        Index(value = ["failureRecordId"], unique = true),
        Index(value = ["commitmentId"]),
        Index(value = ["accountOwnerId"]),
        Index(value = ["penaltyStatusName"]),
        Index(value = ["createdAtEpochMs"])
    ]
)
data class PenaltyEntity(
    @PrimaryKey val penaltyId: String,
    val failureRecordId: String,
    val commitmentId: String,
    val questId: Long,
    val accountOwnerId: String,
    val questTitleSnapshot: String,
    val severityCode: String,
    val basePenaltyPoints: Int,
    val escalationBonusPoints: Int,
    val totalPenaltyPoints: Int,
    val penaltyStatusName: String,
    val reason: String,
    val policyVersion: String,
    val createdAtEpochMs: Long,
    val resolvedAtEpochMs: Long?
)

@Entity(
    tableName = "penalty_transactions",
    indices = [
        Index(value = ["idempotencyKey"], unique = true),
        Index(value = ["accountOwnerId"]),
        Index(value = ["penaltyId"]),
        Index(value = ["timestampEpochMs"])
    ]
)
data class PenaltyTransactionEntity(
    @PrimaryKey val transactionId: String,
    val accountOwnerId: String,
    val penaltyId: String,
    val transactionTypeName: String,
    val pointsDelta: Int,
    val balanceAfter: Int,
    val reason: String,
    val timestampEpochMs: Long,
    val idempotencyKey: String
)

@Entity(
    tableName = "penalty_point_accounts"
)
data class PenaltyPointAccountEntity(
    @PrimaryKey val accountOwnerId: String,
    val currentBalance: Int,
    val lifetimePointsAccrued: Int,
    val activePenaltiesCount: Int,
    val lastUpdatedEpochMs: Long
)

// Extension mappers
fun PenaltyEntity.toDomain(): PenaltyRecord = PenaltyRecord(
    penaltyId = penaltyId,
    failureRecordId = failureRecordId,
    commitmentId = commitmentId,
    questId = questId,
    accountOwnerId = accountOwnerId,
    questTitleSnapshot = questTitleSnapshot,
    severity = PenaltySeverity.fromCode(severityCode),
    basePenaltyPoints = basePenaltyPoints,
    escalationBonusPoints = escalationBonusPoints,
    totalPenaltyPoints = totalPenaltyPoints,
    penaltyStatus = PenaltyStatus.fromName(penaltyStatusName),
    reason = reason,
    policyVersion = policyVersion,
    createdAtEpochMs = createdAtEpochMs,
    resolvedAtEpochMs = resolvedAtEpochMs
)

fun PenaltyRecord.toEntity(): PenaltyEntity = PenaltyEntity(
    penaltyId = penaltyId,
    failureRecordId = failureRecordId,
    commitmentId = commitmentId,
    questId = questId,
    accountOwnerId = accountOwnerId,
    questTitleSnapshot = questTitleSnapshot,
    severityCode = severity.code,
    basePenaltyPoints = basePenaltyPoints,
    escalationBonusPoints = escalationBonusPoints,
    totalPenaltyPoints = totalPenaltyPoints,
    penaltyStatusName = penaltyStatus.name,
    reason = reason,
    policyVersion = policyVersion,
    createdAtEpochMs = createdAtEpochMs,
    resolvedAtEpochMs = resolvedAtEpochMs
)

fun PenaltyTransactionEntity.toDomain(): PenaltyTransaction = PenaltyTransaction(
    transactionId = transactionId,
    accountOwnerId = accountOwnerId,
    penaltyId = penaltyId,
    transactionType = PenaltyTransactionType.fromName(transactionTypeName),
    pointsDelta = pointsDelta,
    balanceAfter = balanceAfter,
    reason = reason,
    timestampEpochMs = timestampEpochMs,
    idempotencyKey = idempotencyKey
)

fun PenaltyTransaction.toEntity(): PenaltyTransactionEntity = PenaltyTransactionEntity(
    transactionId = transactionId,
    accountOwnerId = accountOwnerId,
    penaltyId = penaltyId,
    transactionTypeName = transactionType.name,
    pointsDelta = pointsDelta,
    balanceAfter = balanceAfter,
    reason = reason,
    timestampEpochMs = timestampEpochMs,
    idempotencyKey = idempotencyKey
)

fun PenaltyPointAccountEntity.toDomain(): PenaltyPointBalance = PenaltyPointBalance(
    accountOwnerId = accountOwnerId,
    currentBalance = currentBalance,
    lifetimePointsAccrued = lifetimePointsAccrued,
    activePenaltiesCount = activePenaltiesCount,
    lastUpdatedEpochMs = lastUpdatedEpochMs
)

fun PenaltyPointBalance.toEntity(): PenaltyPointAccountEntity = PenaltyPointAccountEntity(
    accountOwnerId = accountOwnerId,
    currentBalance = currentBalance,
    lifetimePointsAccrued = lifetimePointsAccrued,
    activePenaltiesCount = activePenaltiesCount,
    lastUpdatedEpochMs = lastUpdatedEpochMs
)
