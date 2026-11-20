package com.example.domain.engine

import com.example.domain.model.PenaltyRecord
import com.example.domain.model.PenaltyStatus
import kotlin.math.max
import kotlin.math.min

/**
 * Pure Kotlin, deterministic engine for calculating penalty point resolution amounts
 * and state transitions following verified redemption.
 *
 * Guarantees:
 * - Resolution cannot exceed the outstanding penalty value.
 * - Account balance is strictly non-negative (>= 0).
 * - Full resolution marks the penalty status as RESOLVED.
 * - Partial resolution leaves penalty ACTIVE with reduced points.
 */
object PenaltyResolutionEngine {

    data class ResolutionCalculation(
        val pointsToDeduct: Int,
        val remainingPenaltyPoints: Int,
        val newAccountBalance: Int,
        val newPenaltyStatus: PenaltyStatus,
        val isFullyResolved: Boolean
    )

    fun calculateResolution(
        penalty: PenaltyRecord,
        verifiedPoints: Int,
        currentAccountBalance: Int
    ): ResolutionCalculation {
        require(verifiedPoints > 0) { "Verified points for resolution must be positive." }

        // The maximum deductible from this penalty is its total points
        val effectivePoints = min(verifiedPoints, penalty.totalPenaltyPoints)

        // Deduct from account balance, ensuring non-negative balance
        val newBalance = max(0, currentAccountBalance - effectivePoints)

        val remainingPenalty = max(0, penalty.totalPenaltyPoints - effectivePoints)
        val fullyResolved = remainingPenalty == 0

        val newStatus = if (fullyResolved) {
            PenaltyStatus.RESOLVED
        } else {
            penalty.penaltyStatus // Remains ACTIVE or ESCALATED
        }

        return ResolutionCalculation(
            pointsToDeduct = effectivePoints,
            remainingPenaltyPoints = remainingPenalty,
            newAccountBalance = newBalance,
            newPenaltyStatus = newStatus,
            isFullyResolved = fullyResolved
        )
    }
}
