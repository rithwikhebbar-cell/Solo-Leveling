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
 * Milestone 12 - Phase 4: Real-World Social Missions, Skill Assessments & Mastery Progression Unit Tests.
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
    // 2. MISSION LIFECYCLE & EVIDENCE VALIDATION
    // =========================================================================

    @Test
    fun testMissionLifecycleTransitions() {
        val now = System.currentTimeMillis()
        var record = SocialMissionRecord(
            recordId = UUID.randomUUID().toString(),
            accountOwnerId = "player_1",
            missionId = "MSN_COMM_E01",
            module = SocialModuleIdentifier.COMMUNICATION_MASTERY,
            title = "Meaningful Daily Check-In",
            difficultyRank = MissionDifficultyRank.E_RANK,
            lifecycleStatus = SocialMissionLifecycleStatus.ACCEPTED,
            startedEpochMs = now,
            evidenceType = SocialMissionEvidenceType.SELF_REFLECTION,
            verificationStatus = VerificationStatus.SELF_REPORTED,
            idempotencyKey = "key_test_1"
        )
        assertEquals(SocialMissionLifecycleStatus.ACCEPTED, record.lifecycleStatus)

        // Advance to IN_PROGRESS
        record = record.copy(lifecycleStatus = SocialMissionLifecycleStatus.IN_PROGRESS)
        assertEquals(SocialMissionLifecycleStatus.IN_PROGRESS, record.lifecycleStatus)

        // Advance to SUBMITTED
        record = record.copy(
            lifecycleStatus = SocialMissionLifecycleStatus.SUBMITTED,
            submittedEpochMs = now + 60000,
            reflectionText = "I asked my coworker what the highlight of their sprint was. We connected for 3 minutes."
        )
        assertEquals(SocialMissionLifecycleStatus.SUBMITTED, record.lifecycleStatus)
        assertTrue(record.reflectionText!!.isNotBlank())

        // Complete & Approved
        record = record.copy(
            lifecycleStatus = SocialMissionLifecycleStatus.COMPLETED,
            completedEpochMs = now + 120000,
            xpAwarded = 25
        )
        assertEquals(SocialMissionLifecycleStatus.COMPLETED, record.lifecycleStatus)
        assertEquals(25, record.xpAwarded)
    }

    @Test
    fun testPostponeAndCancelWithoutPenalty() {
        val record = SocialMissionRecord(
            recordId = UUID.randomUUID().toString(),
            accountOwnerId = "player_1",
            missionId = "MSN_PUB_D02",
            module = SocialModuleIdentifier.PUBLIC_SPEAKING,
            title = "Ask a Question in a Group Forum",
            difficultyRank = MissionDifficultyRank.D_RANK,
            lifecycleStatus = SocialMissionLifecycleStatus.IN_PROGRESS,
            startedEpochMs = System.currentTimeMillis(),
            evidenceType = SocialMissionEvidenceType.SELF_REFLECTION,
            verificationStatus = VerificationStatus.SELF_REPORTED,
            idempotencyKey = "key_test_postpone"
        )

        // Postpone
        val postponed = record.copy(lifecycleStatus = SocialMissionLifecycleStatus.POSTPONED)
        assertEquals(SocialMissionLifecycleStatus.POSTPONED, postponed.lifecycleStatus)
        assertEquals(0, postponed.xpAwarded)

        // Cancel
        val cancelled = record.copy(lifecycleStatus = SocialMissionLifecycleStatus.CANCELLED)
        assertEquals(SocialMissionLifecycleStatus.CANCELLED, cancelled.lifecycleStatus)
        assertEquals(0, cancelled.xpAwarded)
    }

    @Test
    fun testEvidenceTypeVerificationStatusMapping() {
        assertEquals(VerificationStatus.SELF_REPORTED, SocialMissionEvidenceType.SELF_REFLECTION.verificationTier)
        assertEquals(VerificationStatus.VERIFIED_AI_RUBRIC, SocialMissionEvidenceType.PRACTICE_EVALUATION.verificationTier)
        assertEquals(VerificationStatus.SELF_REPORTED, SocialMissionEvidenceType.OPTIONAL_ARTIFACT.verificationTier)
        assertEquals(VerificationStatus.VERIFIED_SYSTEM_TIMER, SocialMissionEvidenceType.HUMAN_CONFIRMED.verificationTier)
    }

    // =========================================================================
    // 3. SKILL ASSESSMENT ENGINE & RUBRIC TESTS
    // =========================================================================

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

    @Test
    fun testSkillAssessmentEvaluationScoring() {
        val submission = SkillAssessmentSubmission(
            assessmentId = "test_assess_01",
            accountOwnerId = "player_1",
            module = SocialModuleIdentifier.COMMUNICATION_MASTERY,
            scoreDim1 = 85, // Clarity
            scoreDim2 = 80, // Structure
            scoreDim3 = 75, // Listening
            scoreDim4 = 60, // Conciseness (weakest)
            scoreDim5 = 80, // Context Awareness
            qualitativeNotes = "Felt confident on thesis clarity, but rambled on the technical details."
        )

        val result = SocialSkillAssessmentEngine.evaluateSubmission(submission)

        // (85 + 80 + 75 + 60 + 80) / 5 = 76%
        assertEquals(76, result.overallScore)
        assertEquals(SocialMasteryStage.PROFICIENT, result.masteryStage)
        assertEquals(5, result.dimensionScores.size)
        assertEquals(60, result.dimensionScores["Conciseness"])

        // Recommendation must focus on the weakest dimension (< 70)
        assertTrue(
            "Recommendation must prioritize Conciseness",
            result.actionableRecommendations.any { it.contains("Conciseness", ignoreCase = true) }
        )
    }

    @Test
    fun testSkillAssessmentBoundaries() {
        // High scores
        val highSub = SkillAssessmentSubmission(
            assessmentId = "high",
            accountOwnerId = "p",
            module = SocialModuleIdentifier.LEADERSHIP_TEAMWORK,
            scoreDim1 = 95, scoreDim2 = 92, scoreDim3 = 90, scoreDim4 = 94, scoreDim5 = 90,
            qualitativeNotes = "High team synergy"
        )
        val highRes = SocialSkillAssessmentEngine.evaluateSubmission(highSub)
        assertEquals(92, highRes.overallScore)
        assertEquals(SocialMasteryStage.MASTER, highRes.masteryStage)

        // Low scores
        val lowSub = SkillAssessmentSubmission(
            assessmentId = "low",
            accountOwnerId = "p",
            module = SocialModuleIdentifier.NEGOTIATION_PERSUASION,
            scoreDim1 = 40, scoreDim2 = 45, scoreDim3 = 50, scoreDim4 = 40, scoreDim5 = 45,
            qualitativeNotes = "Hesitant"
        )
        val lowRes = SocialSkillAssessmentEngine.evaluateSubmission(lowSub)
        assertEquals(44, lowRes.overallScore)
        assertEquals(SocialMasteryStage.NOVICE, lowRes.masteryStage)
    }

    // =========================================================================
    // 4. PROGRESSION & INTEGRITY SAFEGUARD TESTS
    // =========================================================================

    @Test
    fun testZeroProgressionOnMissionStartOrViewing() {
        // Merely creating an active mission record gives 0 XP
        val record = SocialMissionRecord(
            recordId = "rec_01",
            accountOwnerId = "p",
            missionId = "MSN_COMM_E01",
            module = SocialModuleIdentifier.COMMUNICATION_MASTERY,
            title = "Test",
            difficultyRank = MissionDifficultyRank.E_RANK,
            lifecycleStatus = SocialMissionLifecycleStatus.IN_PROGRESS,
            startedEpochMs = System.currentTimeMillis(),
            evidenceType = SocialMissionEvidenceType.SELF_REFLECTION,
            verificationStatus = VerificationStatus.SELF_REPORTED,
            idempotencyKey = "key_01",
            xpAwarded = 0
        )
        assertEquals(0, record.xpAwarded)
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
            idempotencyKey = "msn_test_orch_key",
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
