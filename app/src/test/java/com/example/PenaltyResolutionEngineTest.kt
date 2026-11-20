package com.example

import com.example.domain.engine.PenaltyResolutionEngine
import com.example.domain.model.PenaltyRecord
import com.example.domain.model.PenaltySeverity
import com.example.domain.model.PenaltyStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PenaltyResolutionEngineTest {

    private fun createPenalty(points: Int = 20) = PenaltyRecord(
        penaltyId = "pen_100",
        failureRecordId = "fail_100",
        commitmentId = "commit_100",
        questId = 1L,
        accountOwnerId = "player_1",
        questTitleSnapshot = "Difficult Quest",
        severity = PenaltySeverity.P3,
        basePenaltyPoints = 20,
        escalationBonusPoints = 0,
        totalPenaltyPoints = points,
        penaltyStatus = PenaltyStatus.ACTIVE,
        reason = "Grace period expired",
        policyVersion = "POLICY_V1_2026"
    )

    @Test
    fun fullResolution_marksPenaltyResolvedAndDeductsAccurately() {
        val penalty = createPenalty(points = 20)

        val result = PenaltyResolutionEngine.calculateResolution(
            penalty = penalty,
            verifiedPoints = 20,
            currentAccountBalance = 20
        )

        assertEquals(20, result.pointsToDeduct)
        assertEquals(0, result.remainingPenaltyPoints)
        assertEquals(0, result.newAccountBalance)
        assertEquals(PenaltyStatus.RESOLVED, result.newPenaltyStatus)
        assertTrue(result.isFullyResolved)
    }

    @Test
    fun partialResolution_leavesPenaltyActiveWithReducedPoints() {
        val penalty = createPenalty(points = 30)

        val result = PenaltyResolutionEngine.calculateResolution(
            penalty = penalty,
            verifiedPoints = 15,
            currentAccountBalance = 30
        )

        assertEquals(15, result.pointsToDeduct)
        assertEquals(15, result.remainingPenaltyPoints)
        assertEquals(15, result.newAccountBalance)
        assertEquals(PenaltyStatus.ACTIVE, result.newPenaltyStatus)
        assertFalse(result.isFullyResolved)
    }

    @Test
    fun resolutionCannotExceedPenaltyTotalPoints() {
        val penalty = createPenalty(points = 10)

        // Attempting to resolve 25 points on a 10 PP penalty
        val result = PenaltyResolutionEngine.calculateResolution(
            penalty = penalty,
            verifiedPoints = 25,
            currentAccountBalance = 10
        )

        // Must cap at 10 PP
        assertEquals(10, result.pointsToDeduct)
        assertEquals(0, result.remainingPenaltyPoints)
        assertEquals(0, result.newAccountBalance)
        assertTrue(result.isFullyResolved)
    }

    @Test
    fun accountBalance_strictlyCannotBecomeNegative() {
        val penalty = createPenalty(points = 20)

        // Account balance is currently 10 due to previous adjustments
        val result = PenaltyResolutionEngine.calculateResolution(
            penalty = penalty,
            verifiedPoints = 20,
            currentAccountBalance = 10
        )

        assertEquals(20, result.pointsToDeduct)
        assertEquals(0, result.newAccountBalance) // Coerced to at least 0
    }
}
