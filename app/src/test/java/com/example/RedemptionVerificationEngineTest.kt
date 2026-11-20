package com.example

import com.example.domain.engine.RedemptionVerificationEngine
import com.example.domain.model.CorrectiveCategory
import com.example.domain.model.CorrectiveDifficulty
import com.example.domain.model.RedemptionLifecycleState
import com.example.domain.model.RedemptionPlan
import com.example.domain.model.RedemptionVerificationEvidence
import com.example.domain.model.RedemptionVerificationMethod
import com.example.domain.model.ShadowClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RedemptionVerificationEngineTest {

    private fun createPlan(
        method: RedemptionVerificationMethod = RedemptionVerificationMethod.STRUCTURED_REFLECTION,
        points: Int = 10
    ) = RedemptionPlan(
        redemptionId = "red_1",
        penaltyId = "pen_1",
        failureRecordId = "fail_1",
        questId = 1L,
        accountOwnerId = "player_1",
        activityId = "strat_root_cause_analysis_30",
        category = CorrectiveCategory.STRATEGIC_PLANNING,
        activityTitle = "Post-Mortem",
        instructions = "Reflect on cause",
        difficulty = CorrectiveDifficulty.MANAGEABLE,
        estimatedDurationMinutes = 20,
        verificationMethod = method,
        lifecycleState = RedemptionLifecycleState.ACTIVE,
        resolutionPoints = points,
        assignedShadowClass = ShadowClass.ARCANIST,
        shadowGuidanceNote = "Focus on the cause."
    )

    @Test
    fun validStructuredReflection_approvesVerificationAndResolvesPoints() {
        val plan = createPlan(method = RedemptionVerificationMethod.STRUCTURED_REFLECTION, points = 15)
        val evidence = RedemptionVerificationEvidence(
            redemptionId = plan.redemptionId,
            accountOwnerId = "player_1",
            reflectionText = "The missed commitment was due to unexpected meetings; I have scheduled a dedicated morning buffer.",
            userConfirmedCompliance = true
        )

        val outcome = RedemptionVerificationEngine.verify(plan, evidence)

        assertTrue(outcome.isApproved)
        assertEquals(15, outcome.pointsResolved)
        assertTrue(outcome.auditNotes.contains("Structured reflection verified"))
    }

    @Test
    fun shortReflection_isRejected() {
        val plan = createPlan(method = RedemptionVerificationMethod.STRUCTURED_REFLECTION)
        val evidence = RedemptionVerificationEvidence(
            redemptionId = plan.redemptionId,
            accountOwnerId = "player_1",
            reflectionText = "Too short",
            userConfirmedCompliance = true
        )

        val outcome = RedemptionVerificationEngine.verify(plan, evidence)

        assertFalse(outcome.isApproved)
        assertEquals(0, outcome.pointsResolved)
        assertTrue(outcome.rejectionReason?.contains("at least 20 characters") == true)
    }

    @Test
    fun placeholderReflection_isRejected() {
        val plan = createPlan(method = RedemptionVerificationMethod.STRUCTURED_REFLECTION)
        val evidence = RedemptionVerificationEvidence(
            redemptionId = plan.redemptionId,
            accountOwnerId = "player_1",
            reflectionText = "done done done done done done done done done",
            userConfirmedCompliance = true
        )

        val outcome = RedemptionVerificationEngine.verify(plan, evidence)

        assertFalse(outcome.isApproved)
        assertEquals(0, outcome.pointsResolved)
    }

    @Test
    fun checklistConfirmation_withCheckedItems_approvesVerification() {
        val plan = createPlan(method = RedemptionVerificationMethod.CHECKLIST_CONFIRMATION, points = 10)
        val evidence = RedemptionVerificationEvidence(
            redemptionId = plan.redemptionId,
            accountOwnerId = "player_1",
            checklistItemsConfirmed = listOf("Removed app notifications", "Cleaned workspace"),
            userConfirmedCompliance = true
        )

        val outcome = RedemptionVerificationEngine.verify(plan, evidence)

        assertTrue(outcome.isApproved)
        assertEquals(10, outcome.pointsResolved)
    }

    @Test
    fun checklistConfirmation_withEmptyList_isRejected() {
        val plan = createPlan(method = RedemptionVerificationMethod.CHECKLIST_CONFIRMATION)
        val evidence = RedemptionVerificationEvidence(
            redemptionId = plan.redemptionId,
            accountOwnerId = "player_1",
            checklistItemsConfirmed = emptyList(),
            userConfirmedCompliance = true
        )

        val outcome = RedemptionVerificationEngine.verify(plan, evidence)

        assertFalse(outcome.isApproved)
        assertEquals(0, outcome.pointsResolved)
    }

    @Test
    fun unconfirmedCompliance_isRejected() {
        val plan = createPlan()
        val evidence = RedemptionVerificationEvidence(
            redemptionId = plan.redemptionId,
            accountOwnerId = "player_1",
            reflectionText = "A completely valid and detailed reflection about schedule conflict resolution.",
            userConfirmedCompliance = false
        )

        val outcome = RedemptionVerificationEngine.verify(plan, evidence)

        assertFalse(outcome.isApproved)
        assertEquals(0, outcome.pointsResolved)
    }
}
