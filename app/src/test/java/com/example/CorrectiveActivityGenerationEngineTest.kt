package com.example

import com.example.domain.engine.CorrectiveActivityGenerationEngine
import com.example.domain.model.AttributeType
import com.example.domain.model.CorrectiveCategory
import com.example.domain.model.CorrectiveDifficulty
import com.example.domain.model.PenaltyRecord
import com.example.domain.model.PenaltySeverity
import com.example.domain.model.PenaltyStatus
import com.example.domain.model.Quest
import com.example.domain.model.QuestCategory
import com.example.domain.model.QuestType
import com.example.domain.model.RecoveryStatus
import com.example.domain.model.ShadowClass
import com.example.domain.model.VerificationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CorrectiveActivityGenerationEngineTest {

    private fun createPenalty(
        severity: PenaltySeverity = PenaltySeverity.P2,
        points: Int = 10,
        questId: Long = 1L
    ) = PenaltyRecord(
        penaltyId = "pen_1",
        failureRecordId = "fail_1",
        commitmentId = "commit_1",
        questId = questId,
        accountOwnerId = "player_1",
        questTitleSnapshot = "Target Objective",
        severity = severity,
        basePenaltyPoints = points,
        escalationBonusPoints = 0,
        totalPenaltyPoints = points,
        penaltyStatus = PenaltyStatus.ACTIVE,
        reason = "Deadline missed",
        policyVersion = "POLICY_V1_2026"
    )

    private fun createQuest(
        category: QuestCategory,
        title: String = "Test Quest"
    ) = Quest(
        id = 1L,
        title = title,
        description = "Description",
        type = QuestType.DAILY,
        category = category,
        targetValue = 30,
        currentValue = 0,
        unit = "min",
        xpReward = 45,
        primaryAttribute = AttributeType.STR,
        primaryAttrXpReward = 40,
        isCompleted = false,
        verificationStatus = VerificationStatus.SELF_REPORTED
    )

    @Test
    fun academicFailure_generatesAcademicCorrectiveActivity() {
        val quest = createQuest(category = QuestCategory.INTELLECT)
        val penalty = createPenalty(severity = PenaltySeverity.P1)

        val plan = CorrectiveActivityGenerationEngine.generatePlan(
            penalty = penalty,
            quest = quest,
            recoveryStatus = RecoveryStatus.OPTIMAL
        )

        assertEquals(CorrectiveCategory.ACADEMIC, plan.category)
        assertEquals(CorrectiveDifficulty.LIGHT, plan.difficulty)
        assertEquals(ShadowClass.RANGER, plan.assignedShadowClass)
    }

    @Test
    fun disciplineFailure_generatesDisciplineCorrectiveActivity() {
        val quest = createQuest(category = QuestCategory.DISCIPLINE)
        val penalty = createPenalty(severity = PenaltySeverity.P2)

        val plan = CorrectiveActivityGenerationEngine.generatePlan(
            penalty = penalty,
            quest = quest,
            recoveryStatus = RecoveryStatus.OPTIMAL
        )

        assertEquals(CorrectiveCategory.DISCIPLINE, plan.category)
        assertEquals(CorrectiveDifficulty.MANAGEABLE, plan.difficulty)
        assertTrue(plan.assignedShadowClass in setOf(ShadowClass.TACTICIAN, ShadowClass.SUPPORT, ShadowClass.ARCANIST))
    }

    @Test
    fun physicalFailure_underOptimalRecovery_generatesGentlePhysicalOrRecoveryActivity() {
        val quest = createQuest(category = QuestCategory.PHYSICAL)
        val penalty = createPenalty(severity = PenaltySeverity.P1)

        val plan = CorrectiveActivityGenerationEngine.generatePlan(
            penalty = penalty,
            quest = quest,
            recoveryStatus = RecoveryStatus.OPTIMAL
        )

        assertEquals(CorrectiveCategory.PHYSICAL, plan.category)
        assertEquals(ShadowClass.GUARDIAN, plan.assignedShadowClass)
        // Strictly gentle, no punishment workout
        assertTrue(plan.activityTitle.contains("Schedule Realignment") || plan.activityTitle.contains("Mobility"))
    }

    @Test
    fun physicalFailure_underRecoveryRequired_strictlyForbidsPhysicalAndGeneratesStrategicPlanning() {
        val quest = createQuest(category = QuestCategory.PHYSICAL)
        val penalty = createPenalty(severity = PenaltySeverity.P3)

        // SAFETY TEST: When recovery is required, player must NEVER receive a physical corrective task
        val plan = CorrectiveActivityGenerationEngine.generatePlan(
            penalty = penalty,
            quest = quest,
            recoveryStatus = RecoveryStatus.RECOVERY_REQUIRED
        )

        assertNotEquals("Must not prescribe physical activity during required recovery", CorrectiveCategory.PHYSICAL, plan.category)
        assertEquals(CorrectiveCategory.STRATEGIC_PLANNING, plan.category)
    }

    @Test
    fun heavyWorkloadToday_throttlesDifficultyDownToLight() {
        val quest = createQuest(category = QuestCategory.DISCIPLINE)
        val penalty = createPenalty(severity = PenaltySeverity.P4)

        // Player already has 45 minutes of corrective work today
        val plan = CorrectiveActivityGenerationEngine.generatePlan(
            penalty = penalty,
            quest = quest,
            workloadMinutesToday = 45
        )

        // Must scale down to LIGHT to prevent exhaustion
        assertEquals(CorrectiveDifficulty.LIGHT, plan.difficulty)
    }

    @Test
    fun recentActivities_areDeduplicated() {
        val quest = createQuest(category = QuestCategory.ACADEMIC)
        val penalty = createPenalty(severity = PenaltySeverity.P1)

        // Exclude the first light academic activity
        val recentIds = setOf("acad_concept_review_10")
        val plan = CorrectiveActivityGenerationEngine.generatePlan(
            penalty = penalty,
            quest = quest,
            recentActivityIds = recentIds
        )

        assertNotEquals("acad_concept_review_10", plan.activityId)
    }
}
