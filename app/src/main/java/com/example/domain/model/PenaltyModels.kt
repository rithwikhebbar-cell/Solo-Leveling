package com.example.domain.model

import java.util.UUID

/**
 * Deterministic severity classifications for confirmed quest failures.
 */
enum class PenaltySeverity(
    val code: String,
    val displayName: String,
    val basePoints: Int,
    val level: Int
) {
    P1("P1", "MINOR", 5, 1),
    P2("P2", "MODERATE", 10, 2),
    P3("P3", "SEVERE", 20, 3),
    P4("P4", "CRITICAL", 30, 4);

    fun nextLevel(): PenaltySeverity = when (this) {
        P1 -> P2
        P2 -> P3
        P3 -> P4
        P4 -> P4
    }

    companion object {
        fun fromCode(code: String): PenaltySeverity =
            entries.find { it.code.equals(code, ignoreCase = true) } ?: P1
    }
}

/**
 * Status lifecycle of an individual penalty record.
 */
enum class PenaltyStatus(val displayName: String, val isOutstanding: Boolean) {
    ACTIVE("Active", true),
    ESCALATED("Escalated", true),
    RESOLVED("Resolved", false),
    WAIVED("Waived", false),
    EXEMPTED("Exempted", false);

    companion object {
        fun fromName(name: String): PenaltyStatus =
            entries.find { it.name.equals(name, ignoreCase = true) } ?: ACTIVE
    }
}

/**
 * Auditable transaction types in the Penalty Point ledger.
 */
enum class PenaltyTransactionType(val displayName: String, val isDebit: Boolean) {
    ALLOCATION("Allocation", false),
    RESOLUTION("Resolution", true),
    REDEMPTION_RESOLUTION("Redemption Resolution", true),
    REVERSAL("Reversal", true),
    ADJUSTMENT("Adjustment", false);

    companion object {
        fun fromName(name: String): PenaltyTransactionType =
            entries.find { it.name.equals(name, ignoreCase = true) } ?: ALLOCATION
    }
}

/**
 * Centralized, versioned configuration for penalty evaluation and escalation.
 * Modifying a future policy does not recalculate historical penalties.
 */
data class PenaltyPolicy(
    val policyVersion: String = "POLICY_V1_2026",
    val basePointsBySeverity: Map<PenaltySeverity, Int> = mapOf(
        PenaltySeverity.P1 to 5,
        PenaltySeverity.P2 to 10,
        PenaltySeverity.P3 to 20,
        PenaltySeverity.P4 to 30
    ),
    val rollingHistoryWindowDays: Int = 14,
    val escalationThresholdFailures: Int = 2,
    val maxEscalationBonusPoints: Int = 15,
    val bonusPerEscalationStep: Int = 5,
    val maxSeverity: PenaltySeverity = PenaltySeverity.P4,
    val maxCumulativeBalance: Int = 1000,
    val effectiveTimestampEpochMs: Long = 0L
) {
    val rollingHistoryWindowMs: Long
        get() = rollingHistoryWindowDays * 86_400_000L
}

val DefaultPenaltyPolicy = PenaltyPolicy()

/**
 * Detailed result of evaluating a failure through the deterministic calculation engine.
 */
data class PenaltyCalculationResult(
    val severity: PenaltySeverity,
    val basePenaltyPoints: Int,
    val escalationBonusPoints: Int,
    val totalPenaltyPoints: Int,
    val reason: String,
    val policyVersion: String,
    val isEscalated: Boolean,
    val escalationReason: String? = null
)

/**
 * Result of attempting to allocate and ledger a penalty for a confirmed failure.
 */
data class PenaltyAllocationResult(
    val success: Boolean,
    val penalty: PenaltyRecord? = null,
    val transaction: PenaltyTransaction? = null,
    val newBalance: Int = 0,
    val message: String = "",
    val isDuplicate: Boolean = false
)

/**
 * Independent penalty balance entity representation for the player account.
 */
data class PenaltyPointBalance(
    val accountOwnerId: String,
    val currentBalance: Int = 0,
    val lifetimePointsAccrued: Int = 0,
    val activePenaltiesCount: Int = 0,
    val lastUpdatedEpochMs: Long = System.currentTimeMillis()
)

/**
 * Domain representation of an auditable, immutable penalty record.
 */
data class PenaltyRecord(
    val penaltyId: String = UUID.randomUUID().toString(),
    val failureRecordId: String,
    val commitmentId: String,
    val questId: Long,
    val accountOwnerId: String,
    val questTitleSnapshot: String,
    val severity: PenaltySeverity,
    val basePenaltyPoints: Int,
    val escalationBonusPoints: Int,
    val totalPenaltyPoints: Int,
    val penaltyStatus: PenaltyStatus = PenaltyStatus.ACTIVE,
    val reason: String,
    val policyVersion: String,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val resolvedAtEpochMs: Long? = null
)

/**
 * Auditable transaction entry in the independent Penalty Point ledger.
 */
data class PenaltyTransaction(
    val transactionId: String = UUID.randomUUID().toString(),
    val accountOwnerId: String,
    val penaltyId: String,
    val transactionType: PenaltyTransactionType = PenaltyTransactionType.ALLOCATION,
    val pointsDelta: Int,
    val balanceAfter: Int,
    val reason: String,
    val timestampEpochMs: Long = System.currentTimeMillis(),
    val idempotencyKey: String
)

/**
 * Audit record of an escalation decision.
 */
data class PenaltyEscalationRecord(
    val evaluationTimestampEpochMs: Long,
    val rollingWindowStartEpochMs: Long,
    val eligibleFailuresInWindow: Int,
    val originalSeverity: PenaltySeverity,
    val escalatedSeverity: PenaltySeverity,
    val escalationBonusPoints: Int,
    val reason: String
)
