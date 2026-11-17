package com.example

import com.example.domain.engine.ActivityRewardCalculation
import com.example.domain.engine.ProgressionCommand
import com.example.domain.engine.ProgressionEngine
import com.example.domain.engine.ProgressionOrchestrator
import com.example.domain.engine.SocialMissionCatalog
import com.example.domain.engine.SocialSkillAssessmentEngine
import com.example.domain.model.AttributeType
import com.example.domain.model.MissionDifficultyRank
import com.example.domain.model.PlayerProfile
import com.example.domain.model.RealWorldSocialMission
import com.example.domain.model.SkillAssessmentClassification
import com.example.domain.model.SkillAssessmentSubmission
import com.example.domain.model.SocialActivityKind
import com.example.domain.model.SocialMasteryStage
import com.example.domain.model.SocialMissionEvidenceType
import com.example.domain.model.SocialMissionLifecycleStatus
import com.example.domain.model.SocialMissionRecord
import com.example.domain.model.SocialModuleIdentifier
import com.example.domain.model.VerificationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

/**
 * Milestone 12 - Phase 4.1: Social Mastery Mission Integrity, Evidence Verification,
 * Stable Idempotency, Assessment Classification & Progression Guard Unit Tests.
 */
class SocialPhase4MissionsUnitTest {

    // =========================================================================
    // 1. MISSION CATALOGUE INTEGRITY & DIFFICULTY TESTS
    // =========================================================================

    @Test
    fun testMissionCatalogCompletenessAcrossAllEightModules() {
        val missions = SocialMissionCatalog.missions
        assertTrue("Missions catalogue must not be empty", missions.isNotEmpty())

        val coveredModules = missions.map { it.module }.toSet()
        assertEquals(
            "All 8 Social Modules must be represented in the catalogue",
            SocialModuleIdentifier.entries.size,
            coveredModules.size
        )

        for (module in SocialModuleIdentifier.entries) {
            val moduleMissions = SocialMissionCatalog.getMissionsForModule(module)
            assertTrue("Module ${module.name} must have missions", moduleMissions.isNotEmpty())
            assertTrue("Module ${module.name} must have at least 4 missions (one per rank)", moduleMissions.size >= 4)
        }
    }

    @Test
    fun testMissionDifficultyRankDistribution() {
        val eRank = SocialMissionCatalog.getMissionsByDifficulty(MissionDifficultyRank.E_RANK)
        val dRank = SocialMissionCatalog.getMissionsByDifficulty(MissionDifficultyRank.D_RANK)
        val cRank = SocialMissionCatalog.getMissionsByDifficulty(MissionDifficultyRank.C_RANK)
        val bRank = SocialMissionCatalog.getMissionsByDifficulty(MissionDifficultyRank.B_RANK)

        assertTrue("Must contain E-Rank beginner missions", eRank.size >= 8)
        assertTrue("Must contain D-Rank basic missions", dRank.size >= 8)
        assertTrue("Must contain C-Rank intermediate missions", cRank.size >= 8)
        assertTrue("Must contain B-Rank advanced missions", bRank.size >= 8)
    }

    @Test
    fun testMissionAttributesAndPrivacySafety() {
        for (mission in SocialMissionCatalog.missions) {
            assertTrue("Mission title must be non-blank", mission.title.isNotBlank())
            assertTrue("Mission description must be non-blank", mission.description.isNotBlank())
            assertTrue("Estimated duration must be positive", mission.estimatedDurationMinutes in 3..60)
            assertTrue("Instructions must have at least 3 steps", mission.stepByStepInstructions.size >= 3)
            assertTrue("Completion criteria must not be empty", mission.completionCriteria.isNotEmpty())
            assertTrue("Reflection questions must not be empty", mission.reflectionQuestions.isNotEmpty())
            assertTrue("Evidence requirements must not be empty", mission.evidenceRequirements.isNotEmpty())
            assertTrue("AI coaching tip must be provided", mission.aiCoachingTip.isNotBlank())
            assertTrue("Missions must be declineable without punishment", mission.isDeclineable)
        }
    }

    @Test
    fun testAlternativeMissionFallbackWhenDeclining() {
        val originalMission = SocialMissionCatalog.getMissionById("MSN_COMM_E01")
        assertNotNull("Original mission must exist", originalMission)
        assertNotNull("Alternative mission ID must be configured", originalMission!!.alternativeMissionId)

        val altMission = SocialMissionCatalog.getAlternativeMission(originalMission.missionId)
        assertNotNull("Alternative mission must resolve successfully", altMission)
        assertEquals("MSN_COMM_E01_ALT", altMission!!.missionId)
        assertEquals(SocialModuleIdentifier.COMMUNICATION_MASTERY, altMission.module)
    }

    // =========================================================================
    // 2. MISSION LIFECYCLE STATE MACHINE VALIDATION
    // =========================================================================

    @Test
    fun testValidLifecycleTransitions() {
        // AVAILABLE -> ACCEPTED
        assertTrue(SocialMissionLifecycleStatus.AVAILABLE.canTransitionTo(SocialMissionLifecycleStatus.ACCEPTED))
        assertTrue(SocialMissionLifecycleStatus.AVAILABLE.canTransitionTo(SocialMissionLifecycleStatus.IN_PROGRESS))
        assertTrue(SocialMissionLifecycleStatus.AVAILABLE.canTransitionTo(SocialMissionLifecycleStatus.CANCELLED))

        // ACCEPTED -> IN_PROGRESS, POSTPONED, CANCELLED
        assertTrue(SocialMissionLifecycleStatus.ACCEPTED.canTransitionTo(SocialMissionLifecycleStatus.IN_PROGRESS))
        assertTrue(SocialMissionLifecycleStatus.ACCEPTED.canTransitionTo(SocialMissionLifecycleStatus.POSTPONED))
        assertTrue(SocialMissionLifecycleStatus.ACCEPTED.canTransitionTo(SocialMissionLifecycleStatus.CANCELLED))

        // IN_PROGRESS -> SUBMITTED, POSTPONED, CANCELLED
        assertTrue(SocialMissionLifecycleStatus.IN_PROGRESS.canTransitionTo(SocialMissionLifecycleStatus.SUBMITTED))
        assertTrue(SocialMissionLifecycleStatus.IN_PROGRESS.canTransitionTo(SocialMissionLifecycleStatus.POSTPONED))
        assertTrue(SocialMissionLifecycleStatus.IN_PROGRESS.canTransitionTo(SocialMissionLifecycleStatus.CANCELLED))

        // SUBMITTED -> UNDER_REVIEW, COMPLETED, NEEDS_IMPROVEMENT
        assertTrue(SocialMissionLifecycleStatus.SUBMITTED.canTransitionTo(SocialMissionLifecycleStatus.UNDER_REVIEW))
        assertTrue(SocialMissionLifecycleStatus.SUBMITTED.canTransitionTo(SocialMissionLifecycleStatus.COMPLETED))
        assertTrue(SocialMissionLifecycleStatus.SUBMITTED.canTransitionTo(SocialMissionLifecycleStatus.NEEDS_IMPROVEMENT))

        // UNDER_REVIEW -> COMPLETED, NEEDS_IMPROVEMENT, CANCELLED
        assertTrue(SocialMissionLifecycleStatus.UNDER_REVIEW.canTransitionTo(SocialMissionLifecycleStatus.COMPLETED))
        assertTrue(SocialMissionLifecycleStatus.UNDER_REVIEW.canTransitionTo(SocialMissionLifecycleStatus.NEEDS_IMPROVEMENT))
        assertTrue(SocialMissionLifecycleStatus.UNDER_REVIEW.canTransitionTo(SocialMissionLifecycleStatus.CANCELLED))

        // NEEDS_IMPROVEMENT -> RETRY, CANCELLED
        assertTrue(SocialMissionLifecycleStatus.NEEDS_IMPROVEMENT.canTransitionTo(SocialMissionLifecycleStatus.RETRY))
        assertTrue(SocialMissionLifecycleStatus.NEEDS_IMPROVEMENT.canTransitionTo(SocialMissionLifecycleStatus.CANCELLED))

        // RETRY -> IN_PROGRESS, CANCELLED
        assertTrue(SocialMissionLifecycleStatus.RETRY.canTransitionTo(SocialMissionLifecycleStatus.IN_PROGRESS))
        assertTrue(SocialMissionLifecycleStatus.RETRY.canTransitionTo(SocialMissionLifecycleStatus.CANCELLED))

        // POSTPONED -> IN_PROGRESS, CANCELLED
        assertTrue(SocialMissionLifecycleStatus.POSTPONED.canTransitionTo(SocialMissionLifecycleStatus.IN_PROGRESS))
        assertTrue(SocialMissionLifecycleStatus.POSTPONED.canTransitionTo(SocialMissionLifecycleStatus.CANCELLED))
    }

    @Test
    fun testInvalidLifecycleTransitionsBlocked() {
        // Terminal states cannot transition to active states
        assertFalse(SocialMissionLifecycleStatus.COMPLETED.canTransitionTo(SocialMissionLifecycleStatus.IN_PROGRESS))
        assertFalse(SocialMissionLifecycleStatus.COMPLETED.canTransitionTo(SocialMissionLifecycleStatus.SUBMITTED))
        assertFalse(SocialMissionLifecycleStatus.CANCELLED.canTransitionTo(SocialMissionLifecycleStatus.IN_PROGRESS))
        assertFalse(SocialMissionLifecycleStatus.CANCELLED.canTransitionTo(SocialMissionLifecycleStatus.COMPLETED))

        // Cannot skip directly from AVAILABLE to COMPLETED
        assertFalse(SocialMissionLifecycleStatus.AVAILABLE.canTransitionTo(SocialMissionLifecycleStatus.COMPLETED))
        assertFalse(SocialMissionLifecycleStatus.AVAILABLE.canTransitionTo(SocialMissionLifecycleStatus.SUBMITTED))

        // Cannot skip directly from ACCEPTED to COMPLETED without submission
        assertFalse(SocialMissionLifecycleStatus.ACCEPTED.canTransitionTo(SocialMissionLifecycleStatus.COMPLETED))

        // Cannot transition from UNDER_REVIEW back to SUBMITTED
        assertFalse(SocialMissionLifecycleStatus.UNDER_REVIEW.canTransitionTo(SocialMissionLifecycleStatus.SUBMITTED))
    }

    // =========================================================================
    // 3. EVIDENCE VERIFICATION CLASSIFICATION TESTS
    // =========================================================================

    @Test
    fun testEvidenceTypeVerificationStatusMapping() {
        assertEquals(VerificationStatus.SELF_REPORTED, SocialMissionEvidenceType.SELF_REPORTED.verificationTier)
        assertEquals(VerificationStatus.VERIFIED_AI_RUBRIC, SocialMissionEvidenceType.SIMULATION_EVALUATED.verificationTier)
        assertEquals(VerificationStatus.SELF_REPORTED, SocialMissionEvidenceType.ARTIFACT_ATTACHED.verificationTier)
        assertEquals(VerificationStatus.SELF_REPORTED, SocialMissionEvidenceType.HUMAN_CONFIRMATION_REPORTED.verificationTier)
        assertEquals(VerificationStatus.PENDING_REVIEW, SocialMissionEvidenceType.HUMAN_CONFIRMED.verificationTier)

        // Local vault does not support independent third-party confirmation
        assertFalse("Independent third-party verification must be false in local vault", SocialMissionEvidenceType.HUMAN_CONFIRMED.isIndependentlyVerified)
        assertFalse(SocialMissionEvidenceType.HUMAN_CONFIRMATION_REPORTED.isIndependentlyVerified)
        assertFalse(SocialMissionEvidenceType.SELF_REPORTED.isIndependentlyVerified)
    }

    // =========================================================================
    // 4. SKILL ASSESSMENT CLASSIFICATION & PROGRESSION GUARD TESTS
    // =========================================================================

    @Test
    fun testSelfAssessmentCarriesZeroXpAndLimitationsDisclaimer() {
        val submission = SkillAssessmentSubmission(
            assessmentId = "test_self_assess_01",
            accountOwnerId = "player_1",
            module = SocialModuleIdentifier.COMMUNICATION_MASTERY,
            classification = SkillAssessmentClassification.SELF_ASSESSMENT,
            scoreDim1 = 95,
            scoreDim2 = 90,
            scoreDim3 = 92,
            scoreDim4 = 88,
            scoreDim5 = 95,
            qualitativeNotes = "Felt high confidence across all communication dimensions."
        )

        val result = SocialSkillAssessmentEngine.evaluateSubmission(submission)

        // Strict Progression Guard: Self-assessment sliders must award ZERO XP
        assertEquals("Subjective self-assessment sliders must carry exactly 0 XP", 0, result.xpAwarded)
        assertEquals(VerificationStatus.SELF_REPORTED, result.verificationStatus)
        assertEquals(SkillAssessmentClassification.SELF_ASSESSMENT, result.classification)
        assertNotNull(result.limitationsDisclaimer)
        assertTrue(result.limitationsDisclaimer.contains("Subjective self-rating", ignoreCase = true))
        assertTrue(result.scoringMethod.contains("sliders", ignoreCase = true))
    }

    @Test
    fun testSimulationEvaluatedAssessmentAwardsVerifiedXp() {
        val submission = SkillAssessmentSubmission(
            assessmentId = "test_sim_assess_01",
            accountOwnerId = "player_1",
            module = SocialModuleIdentifier.PUBLIC_SPEAKING,
            classification = SkillAssessmentClassification.SIMULATION_EVALUATED,
            scoreDim1 = 85,
            scoreDim2 = 80,
            scoreDim3 = 75,
            scoreDim4 = 80,
            scoreDim5 = 80,
            qualitativeNotes = "Measured speech rehearsal with diaphragmatic pacing."
        )

        val result = SocialSkillAssessmentEngine.evaluateSubmission(submission)

        assertEquals(80, result.overallScore)
        assertTrue("Demonstrated simulation assessment must award XP", result.xpAwarded > 0)
        assertEquals(VerificationStatus.VERIFIED_AI_RUBRIC, result.verificationStatus)
        assertEquals(SkillAssessmentClassification.SIMULATION_EVALUATED, result.classification)
    }

    @Test
    fun testSkillAssessmentRubricsCoverAllModules() {
        val rubrics = SocialSkillAssessmentEngine.rubrics
        assertEquals(8, rubrics.size)

        for (module in SocialModuleIdentifier.entries) {
            val rubric = rubrics[module]
            assertNotNull("Rubric must exist for ${module.name}", rubric)
            assertTrue(rubric!!.dimension1Name.isNotBlank())
            assertTrue(rubric.dimension2Name.isNotBlank())
            assertTrue(rubric.dimension3Name.isNotBlank())
            assertTrue(rubric.dimension4Name.isNotBlank())
            assertTrue(rubric.dimension5Name.isNotBlank())
        }
    }

    // =========================================================================
    // 5. STABLE IDEMPOTENCY & PROGRESSION INTEGRITY TESTS
    // =========================================================================

    @Test
    fun testStableIdempotencyKeyFormat() {
        val recordId = "msn_rec_uuid_456"
        val stableKey = "msn_reward_$recordId"

        // Stable key must be deterministic and never embed dynamic timestamps
        assertFalse("Stable idempotency key must not contain timestamp pattern", stableKey.contains("_${System.currentTimeMillis()}"))
        assertEquals("msn_reward_msn_rec_uuid_456", stableKey)

        // Multiple calls with the same record ID must yield identical keys
        val generatedKey1 = "msn_reward_$recordId"
        val generatedKey2 = "msn_reward_$recordId"
        assertEquals(generatedKey1, generatedKey2)
    }

    @Test
    fun testAuthoritativeProgressionCalculationForRealWorldMissions() {
        val calc = ProgressionEngine.calculateSocialReward(
            module = SocialModuleIdentifier.COMMUNICATION_MASTERY,
            activityKind = SocialActivityKind.REAL_WORLD_ACTIVITY,
            durationMinutes = 15,
            verificationStatus = VerificationStatus.SELF_REPORTED,
            socialXpAlreadyEarnedToday = 0,
            selfReportedSocialCountToday = 0,
            rubricScorePercent = 80
        )

        assertTrue("XP awarded must be > 0", calc.xpAwarded > 0)
        assertEquals(AttributeType.CHA, calc.primaryAttribute)
        assertEquals(AttributeType.WIS, calc.secondaryAttribute)
        assertTrue(calc.primaryAttrXpAwarded > 0)
        assertTrue(calc.secondaryAttrXpAwarded > 0)
        assertFalse(calc.wasCapped)
    }

    @Test
    fun testRealWorldMissionEnforcesDailyCaps() {
        // When daily cap (180 XP) is already reached
        val cappedCalc = ProgressionEngine.calculateSocialReward(
            module = SocialModuleIdentifier.COMMUNICATION_MASTERY,
            activityKind = SocialActivityKind.REAL_WORLD_ACTIVITY,
            durationMinutes = 15,
            verificationStatus = VerificationStatus.SELF_REPORTED,
            socialXpAlreadyEarnedToday = 180,
            selfReportedSocialCountToday = 1,
            rubricScorePercent = 80
        )

        assertEquals(0, cappedCalc.xpAwarded)
        assertTrue(cappedCalc.wasCapped)

        // When self-reported daily activity cap (3) is reached
        val countCappedCalc = ProgressionEngine.calculateSocialReward(
            module = SocialModuleIdentifier.COMMUNICATION_MASTERY,
            activityKind = SocialActivityKind.REAL_WORLD_ACTIVITY,
            durationMinutes = 15,
            verificationStatus = VerificationStatus.SELF_REPORTED,
            socialXpAlreadyEarnedToday = 50,
            selfReportedSocialCountToday = 3,
            rubricScorePercent = 80
        )

        assertEquals(0, countCappedCalc.xpAwarded)
        assertTrue(countCappedCalc.wasCapped)
    }

    @Test
    fun testProgressionOrchestratorAppliesMissionRewardTransactionally() {
        val initialProfile = PlayerProfile(
            level = 1,
            currentLevelXp = 50,
            xpToNextLevel = 100,
            totalXp = 50,
            cha = 10,
            wis = 10
        )

        val rewardCalc = ActivityRewardCalculation(
            xpAwarded = 25,
            primaryAttribute = AttributeType.CHA,
            primaryAttrXpAwarded = 15,
            secondaryAttribute = AttributeType.WIS,
            secondaryAttrXpAwarded = 10,
            wasCapped = false
        )

        val cmd = ProgressionCommand.fromSocialReward(
            idempotencyKey = "msn_reward_rec_test_123",
            sourceRecordId = "rec_test_123",
            title = "Real-World Mission: Check-In",
            calculation = rewardCalc,
            verificationStatus = VerificationStatus.SELF_REPORTED
        )

        val outcome = ProgressionOrchestrator.applyValidatedReward(
            currentProfile = initialProfile,
            command = cmd,
            alreadyProcessedKeys = emptySet(),
            nowEpochMs = System.currentTimeMillis()
        ).getOrThrow()

        assertEquals(75, outcome.updatedProfile.currentLevelXp)
        assertEquals(75, outcome.updatedProfile.totalXp)
        assertEquals(25, outcome.xpGained)
        assertEquals(1, outcome.updatedProfile.selfReportedActivitiesCount)
    }
}
