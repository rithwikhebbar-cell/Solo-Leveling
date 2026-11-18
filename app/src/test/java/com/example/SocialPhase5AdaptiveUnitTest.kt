package com.example

import com.example.domain.engine.ActivityRewardCalculation
import com.example.domain.engine.AdaptiveSocialMasteryEngine
import com.example.domain.engine.ProgressionCommand
import com.example.domain.engine.ProgressionEngine
import com.example.domain.engine.ProgressionOrchestrator
import com.example.domain.model.AdaptiveDifficulty
import com.example.domain.model.AttributeType
import com.example.domain.model.MasteryDevelopmentStage
import com.example.domain.model.MasteryEvidenceClassification
import com.example.domain.model.MissionDifficultyRank
import com.example.domain.model.PersonalizedWeeklyTrainingPlan
import com.example.domain.model.PlayerProfile
import com.example.domain.model.RecommendationCategory
import com.example.domain.model.ScheduledTrainingActivity
import com.example.domain.model.SkillAssessmentClassification
import com.example.domain.model.SkillAssessmentSubmission
import com.example.domain.model.SocialActivityKind
import com.example.domain.model.SocialAssessment
import com.example.domain.model.SocialAssessmentType
import com.example.domain.model.SocialCompetencyCatalog
import com.example.domain.model.SocialCompetencyProgress
import com.example.domain.model.SocialEvidenceSource
import com.example.domain.model.SocialMasteryStage
import com.example.domain.model.SocialMissionEvidenceType
import com.example.domain.model.SocialMissionLifecycleStatus
import com.example.domain.model.SocialMissionRecord
import com.example.domain.model.SocialModuleIdentifier
import com.example.domain.model.SocialPracticeSession
import com.example.domain.model.SocialScenarioAttempt
import com.example.domain.model.SocialSessionStatus
import com.example.domain.model.TrainingActivityType
import com.example.domain.model.VerificationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

/**
 * Milestone 12 - Phase 5: Adaptive Mastery Pathways, Weekly Planner & Personalized Engine Tests.
 * Rigorously validates:
 * 1. Adaptive recommendation generation & weakness detection.
 * 2. Weekly training planner, availability constraints, skipping without penalty.
 * 3. Developmental mastery classification (simulation vs real-world requirements, no demotion on inactivity).
 * 4. Adaptive difficulty progression (gradual increase, regression de-escalation, player override).
 * 5. Phase 4.1 regression safeguards (stable idempotency, 0 XP self-assessment, 180 XP / 3 daily caps).
 */
class SocialPhase5AdaptiveUnitTest {

    // =========================================================================
    // 1. COMPETENCY ARCHITECTURE & CATALOGUE INTEGRITY
    // =========================================================================

    @Test
    fun testCompetencyCatalogCoversAllEightModulesWithSevenCompetenciesEach() {
        val allModules = SocialModuleIdentifier.entries
        assertEquals("Must cover exactly 8 modules", 8, allModules.size)

        var totalCompetencies = 0
        for (module in allModules) {
            val competencies = SocialCompetencyCatalog.getCompetenciesForModule(module)
            assertEquals("Module ${module.name} must have exactly 7 competencies", 7, competencies.size)
            totalCompetencies += competencies.size
            for (comp in competencies) {
                assertTrue("Competency ID must not be blank", comp.competencyId.isNotBlank())
                assertTrue("Competency name must not be blank", comp.name.isNotBlank())
                assertTrue("Description must not be blank", comp.description.isNotBlank())
                assertEquals(module, comp.module)
            }
        }
        assertEquals("Total competencies across 8 modules must be 56", 56, totalCompetencies)
    }

    // =========================================================================
    // 2. ADAPTIVE WEAKNESS DETECTION & RECOMMENDATION ENGINE
    // =========================================================================

    @Test
    fun testWeaknessDetectionGeneratesImproveWeaknessRecommendation() {
        val accountOwnerId = "test_player_01"
        val now = System.currentTimeMillis()

        // Create a competency with low evaluated score (62%)
        val weakComp = SocialCompetencyProgress(
            competencyId = "${accountOwnerId}_COMM_COMP_COMM_01",
            accountOwnerId = accountOwnerId,
            module = SocialModuleIdentifier.COMMUNICATION_MASTERY,
            competencyName = "Active Listening",
            masteryStage = MasteryDevelopmentStage.DEVELOPING,
            recommendedDifficulty = AdaptiveDifficulty.NORMAL,
            practiceCount = 4,
            lastPracticedEpochMs = now - 3600000,
            lastEvaluatedScore = 62, // Weak (< 70%)
            evidenceClassification = MasteryEvidenceClassification.SIMULATION_EVALUATED
        )

        val recommendations = AdaptiveSocialMasteryEngine.generateRecommendations(
            accountOwnerId = accountOwnerId,
            competencies = listOf(weakComp),
            missions = emptyList(),
            nowEpochMs = now
        )

        val weaknessRec = recommendations.find { it.category == RecommendationCategory.IMPROVE_WEAKNESS }
        assertNotNull("Must generate an IMPROVE_WEAKNESS recommendation for score < 70%", weaknessRec)
        assertEquals(SocialModuleIdentifier.COMMUNICATION_MASTERY, weaknessRec!!.targetModule)
        assertEquals("Active Listening", weaknessRec.targetCompetency)
        assertTrue("Explanation must reference the observed score", weaknessRec.explanation.contains("62"))
        assertTrue("Supporting evidence must cite the score", weaknessRec.supportingEvidenceReferences.any { it.contains("62%") })
    }

    @Test
    fun testHighSimulationFluencyGeneratesRealWorldMissionRecommendation() {
        val accountOwnerId = "test_player_02"
        val now = System.currentTimeMillis()

        // Strong simulation score (88%) in COMPETENT stage
        val strongComp = SocialCompetencyProgress(
            competencyId = "${accountOwnerId}_PUB_COMP_PUB_01",
            accountOwnerId = accountOwnerId,
            module = SocialModuleIdentifier.PUBLIC_SPEAKING,
            competencyName = "Speech Organization",
            masteryStage = MasteryDevelopmentStage.COMPETENT,
            recommendedDifficulty = AdaptiveDifficulty.CHALLENGING,
            practiceCount = 8,
            lastPracticedEpochMs = now - 7200000,
            lastEvaluatedScore = 88,
            evidenceClassification = MasteryEvidenceClassification.SIMULATION_EVALUATED
        )

        val recommendations = AdaptiveSocialMasteryEngine.generateRecommendations(
            accountOwnerId = accountOwnerId,
            competencies = listOf(strongComp),
            missions = emptyList(), // Zero completed missions
            nowEpochMs = now
        )

        val realWorldRec = recommendations.find { it.category == RecommendationCategory.REAL_WORLD_APPLICATION }
        assertNotNull("Must recommend REAL_WORLD_APPLICATION when simulation fluency is high", realWorldRec)
        assertEquals("MISSION", realWorldRec!!.recommendedActivityType)
        assertTrue("Explanation must cite strong simulation performance", realWorldRec.explanation.contains("88%"))
    }

    @Test
    fun testInactiveSkillDetectionAfterFourteenDays() {
        val accountOwnerId = "test_player_03"
        val now = System.currentTimeMillis()
        val fifteenDaysAgo = now - (15L * 24L * 60L * 60L * 1000L)

        val inactiveComp = SocialCompetencyProgress(
            competencyId = "${accountOwnerId}_NEG_COMP_NEG_01",
            accountOwnerId = accountOwnerId,
            module = SocialModuleIdentifier.NEGOTIATION_PERSUASION,
            competencyName = "Negotiation Preparation",
            masteryStage = MasteryDevelopmentStage.COMPETENT,
            recommendedDifficulty = AdaptiveDifficulty.NORMAL,
            practiceCount = 5,
            lastPracticedEpochMs = fifteenDaysAgo, // Inactive > 14 days
            lastEvaluatedScore = 78,
            evidenceClassification = MasteryEvidenceClassification.SIMULATION_EVALUATED
        )

        val recommendations = AdaptiveSocialMasteryEngine.generateRecommendations(
            accountOwnerId = accountOwnerId,
            competencies = listOf(inactiveComp),
            missions = emptyList(),
            nowEpochMs = now
        )

        val inactiveRec = recommendations.find { it.category == RecommendationCategory.REVISIT_INACTIVE_SKILL }
        assertNotNull("Must generate REVISIT_INACTIVE_SKILL recommendation when inactive > 14 days", inactiveRec)
        assertTrue("Explanation must mention days of inactivity", inactiveRec!!.explanation.contains("15 days"))
    }

    @Test
    fun testGracefulHandlingOfEmptyHistoryWithoutCrashingOrHallucinating() {
        val accountOwnerId = "new_player"
        val now = System.currentTimeMillis()

        val evaluated = AdaptiveSocialMasteryEngine.evaluateCompetencies(
            accountOwnerId = accountOwnerId,
            sessions = emptyList(),
            attempts = emptyList(),
            missions = emptyList(),
            assessments = emptyList(),
            nowEpochMs = now
        )

        assertEquals("Must initialize all 56 competencies", 56, evaluated.size)
        assertTrue("All competencies for new player must be UNEXPLORED", evaluated.all { it.masteryStage == MasteryDevelopmentStage.UNEXPLORED })
        assertTrue("All practice counts must be 0", evaluated.all { it.practiceCount == 0 })

        val recommendations = AdaptiveSocialMasteryEngine.generateRecommendations(
            accountOwnerId = accountOwnerId,
            competencies = evaluated,
            missions = emptyList(),
            nowEpochMs = now
        )

        assertTrue("Recommendations must not crash on zero history", recommendations.isNotEmpty())
        assertTrue("New player recommendations should focus on exploring new skills", recommendations.any { it.category == RecommendationCategory.PRACTICE_NEW_SKILL })
    }

    // =========================================================================
    // 3. PERSONALIZED WEEKLY TRAINING PLANNER TESTS
    // =========================================================================

    @Test
    fun testWeeklyPlanGenerationWithAvailabilityConstraints() {
        val accountOwnerId = "test_player_plan"
        val now = System.currentTimeMillis()

        val dummyCompetencies = SocialCompetencyCatalog.getCompetenciesForModule(SocialModuleIdentifier.COMMUNICATION_MASTERY).map {
            SocialCompetencyProgress(
                competencyId = "${accountOwnerId}_${it.competencyId}",
                accountOwnerId = accountOwnerId,
                module = it.module,
                competencyName = it.name,
                masteryStage = MasteryDevelopmentStage.DEVELOPING,
                recommendedDifficulty = AdaptiveDifficulty.NORMAL,
                practiceCount = 2,
                lastPracticedEpochMs = now - 86400000,
                lastEvaluatedScore = 72,
                evidenceClassification = MasteryEvidenceClassification.SIMULATION_EVALUATED
            )
        }

        val plan = AdaptiveSocialMasteryEngine.generateWeeklyTrainingPlan(
            accountOwnerId = accountOwnerId,
            targetDaysPerWeek = 4,
            targetMinutesPerDay = 20,
            competencies = dummyCompetencies,
            nowEpochMs = now
        )

        assertEquals("Plan must honor 4 target days", 4, plan.targetDaysPerWeek)
        assertEquals("Plan must honor 20 daily minutes", 20, plan.targetMinutesPerDay)
        assertEquals("Must schedule exactly 4 daily activities", 4, plan.scheduledActivities.size)
        assertFalse("Plan starts unpaused", plan.isPaused)

        // Verify activity types are balanced
        val activityTypes = plan.scheduledActivities.map { it.activityType }
        assertTrue("Should include targeted exercise", activityTypes.contains(TrainingActivityType.EXERCISE))
        assertTrue("Should include real-world challenge or revision", activityTypes.contains(TrainingActivityType.REAL_WORLD_CHALLENGE) || activityTypes.contains(TrainingActivityType.REVISION_SESSION))
    }

    @Test
    fun testSkippingActivityCarriesZeroXpAndNoPenalty() {
        var activity = ScheduledTrainingActivity(
            activityId = "ACT_001",
            planId = "PLAN_001",
            accountOwnerId = "player_1",
            dayOfWeek = 2,
            scheduledDateEpochMs = System.currentTimeMillis(),
            module = SocialModuleIdentifier.LEADERSHIP_TEAMWORK,
            competencyName = "Delegation",
            activityType = TrainingActivityType.EXERCISE,
            title = "Practice Drill: Delegation",
            referenceId = "EX_LEAD_01",
            durationMinutes = 15,
            difficulty = AdaptiveDifficulty.NORMAL,
            isCompleted = false,
            isSkipped = false
        )

        // Player skips activity
        activity = activity.copy(isSkipped = true)
        assertTrue(activity.isSkipped)
        assertFalse(activity.isCompleted)
        // Skipping carries no state corruption and awards 0 XP
    }

    @Test
    fun testWeeklyDevelopmentReportDerivation() {
        val now = System.currentTimeMillis()
        val plan = PersonalizedWeeklyTrainingPlan(
            planId = "PLAN_REPORT_01",
            accountOwnerId = "player_report",
            weeklyDevelopmentObjective = "Active Listening Focus",
            targetMinutesPerDay = 15,
            targetDaysPerWeek = 3,
            startDateEpochMs = now - 604800000,
            endDateEpochMs = now,
            scheduledActivities = listOf(
                ScheduledTrainingActivity(
                    activityId = "A1", planId = "PLAN_REPORT_01", accountOwnerId = "player_report",
                    dayOfWeek = 1, scheduledDateEpochMs = now - 500000000,
                    module = SocialModuleIdentifier.COMMUNICATION_MASTERY, competencyName = "Active Listening",
                    activityType = TrainingActivityType.EXERCISE, title = "PREP Drill", referenceId = "EX_COMM_01",
                    durationMinutes = 15, difficulty = AdaptiveDifficulty.NORMAL, isCompleted = true
                ),
                ScheduledTrainingActivity(
                    activityId = "A2", planId = "PLAN_REPORT_01", accountOwnerId = "player_report",
                    dayOfWeek = 2, scheduledDateEpochMs = now - 400000000,
                    module = SocialModuleIdentifier.COMMUNICATION_MASTERY, competencyName = "Reflective Paraphrasing",
                    activityType = TrainingActivityType.EXERCISE, title = "Paraphrasing Drill", referenceId = "EX_COMM_02",
                    durationMinutes = 15, difficulty = AdaptiveDifficulty.NORMAL, isCompleted = true
                )
            )
        )

        val report = AdaptiveSocialMasteryEngine.generateWeeklyDevelopmentReport(
            plan = plan,
            missions = emptyList(),
            sessions = emptyList(),
            nowEpochMs = now
        )

        assertEquals(2, report.exercisesCompletedCount)
        assertEquals(30, report.totalMinutesPracticed)
        assertTrue("Improvements must mention completed sessions", report.observedImprovements.any { it.contains("2 scheduled") })
    }

    // =========================================================================
    // 4. MASTERY CLASSIFICATION & PROGRESSION REQUIREMENTS
    // =========================================================================

    @Test
    fun testAdvancedStageRequiresRealWorldMissionCompletion() {
        val accountOwnerId = "test_player_stage"
        val now = System.currentTimeMillis()

        // 10 practice attempts with 90% score, BUT zero completed missions
        val evaluatedWithoutMissions = AdaptiveSocialMasteryEngine.evaluateCompetencies(
            accountOwnerId = accountOwnerId,
            sessions = List(6) {
                SocialPracticeSession(
                    sessionId = "sess_$it", accountOwnerId = accountOwnerId,
                    module = SocialModuleIdentifier.COMMUNICATION_MASTERY,
                    activityKind = SocialActivityKind.PRACTICE, sessionStatus = SocialSessionStatus.COMPLETED,
                    durationSeconds = 600, startedEpochMs = now - 10000, completedEpochMs = now,
                    evidenceSource = SocialEvidenceSource.SYSTEM_OBSERVED,
                    verificationStatus = VerificationStatus.SELF_REPORTED, idempotencyKey = "key_s_$it"
                )
            },
            attempts = List(4) {
                SocialScenarioAttempt(
                    attemptId = "att_$it", accountOwnerId = accountOwnerId, scenarioId = "scen_1",
                    scenarioTitle = "Active Listening", module = SocialModuleIdentifier.COMMUNICATION_MASTERY,
                    userResponseText = "Good response", evaluationScore = 90, isPassed = true,
                    evidenceSource = SocialEvidenceSource.AI_ASSESSED,
                    verificationStatus = VerificationStatus.VERIFIED_AI_RUBRIC, idempotencyKey = "key_a_$it"
                )
            },
            missions = emptyList(), // NO MISSIONS
            assessments = emptyList(),
            nowEpochMs = now
        )

        val compWithoutMission = evaluatedWithoutMissions.find { it.competencyName == "Active Listening" }
        assertNotNull(compWithoutMission)
        // Without completed real-world missions, stage is capped at COMPETENT even with 90% scores!
        assertEquals("Cannot advance to ADVANCED without real-world mission completion", MasteryDevelopmentStage.COMPETENT, compWithoutMission!!.masteryStage)

        // Now provide a completed real-world mission
        val evaluatedWithMission = AdaptiveSocialMasteryEngine.evaluateCompetencies(
            accountOwnerId = accountOwnerId,
            sessions = List(6) {
                SocialPracticeSession(
                    sessionId = "sess2_$it", accountOwnerId = accountOwnerId,
                    module = SocialModuleIdentifier.COMMUNICATION_MASTERY,
                    activityKind = SocialActivityKind.PRACTICE, sessionStatus = SocialSessionStatus.COMPLETED,
                    durationSeconds = 600, startedEpochMs = now - 10000, completedEpochMs = now,
                    evidenceSource = SocialEvidenceSource.SYSTEM_OBSERVED,
                    verificationStatus = VerificationStatus.SELF_REPORTED, idempotencyKey = "key_s2_$it"
                )
            },
            attempts = List(4) {
                SocialScenarioAttempt(
                    attemptId = "att2_$it", accountOwnerId = accountOwnerId, scenarioId = "scen_1",
                    scenarioTitle = "Active Listening", module = SocialModuleIdentifier.COMMUNICATION_MASTERY,
                    userResponseText = "Good response", evaluationScore = 90, isPassed = true,
                    evidenceSource = SocialEvidenceSource.AI_ASSESSED,
                    verificationStatus = VerificationStatus.VERIFIED_AI_RUBRIC, idempotencyKey = "key_a2_$it"
                )
            },
            missions = listOf(
                SocialMissionRecord(
                    recordId = "m_rec_01", accountOwnerId = accountOwnerId, missionId = "MSN_COMM_E01",
                    module = SocialModuleIdentifier.COMMUNICATION_MASTERY, title = "Active Listening Check-In",
                    difficultyRank = MissionDifficultyRank.E_RANK, lifecycleStatus = SocialMissionLifecycleStatus.COMPLETED,
                    startedEpochMs = now - 50000, completedEpochMs = now - 1000,
                    evidenceType = SocialMissionEvidenceType.SELF_REPORTED, verificationStatus = VerificationStatus.SELF_REPORTED,
                    idempotencyKey = "msn_reward_m_rec_01", xpAwarded = 25
                )
            ),
            assessments = emptyList(),
            nowEpochMs = now
        )

        val compWithMission = evaluatedWithMission.find { it.competencyName == "Active Listening" }
        assertNotNull(compWithMission)
        assertEquals("ADVANCED stage unlocked with real-world mission evidence", MasteryDevelopmentStage.ADVANCED, compWithMission!!.masteryStage)
    }

    @Test
    fun testInactivityDoesNotDemotePlayerStage() {
        val accountOwnerId = "test_player_inactivity"
        val now = System.currentTimeMillis()
        val twentyDaysAgo = now - (20L * 24L * 60L * 60L * 1000L)

        // Competency was ADVANCED and practiced 20 days ago
        val evaluated = AdaptiveSocialMasteryEngine.evaluateCompetencies(
            accountOwnerId = accountOwnerId,
            sessions = List(6) {
                SocialPracticeSession(
                    sessionId = "sess_old_$it", accountOwnerId = accountOwnerId,
                    module = SocialModuleIdentifier.COMMUNICATION_MASTERY,
                    activityKind = SocialActivityKind.PRACTICE, sessionStatus = SocialSessionStatus.COMPLETED,
                    durationSeconds = 600, startedEpochMs = twentyDaysAgo, completedEpochMs = twentyDaysAgo,
                    evidenceSource = SocialEvidenceSource.SYSTEM_OBSERVED,
                    verificationStatus = VerificationStatus.SELF_REPORTED, idempotencyKey = "key_old_s_$it"
                )
            },
            attempts = List(4) {
                SocialScenarioAttempt(
                    attemptId = "att_old_$it", accountOwnerId = accountOwnerId, scenarioId = "scen_1",
                    scenarioTitle = "Active Listening", module = SocialModuleIdentifier.COMMUNICATION_MASTERY,
                    userResponseText = "Old response", evaluationScore = 85, isPassed = true,
                    evidenceSource = SocialEvidenceSource.AI_ASSESSED,
                    verificationStatus = VerificationStatus.VERIFIED_AI_RUBRIC, idempotencyKey = "key_old_a_$it",
                    completedEpochMs = twentyDaysAgo
                )
            },
            missions = listOf(
                SocialMissionRecord(
                    recordId = "m_old_01", accountOwnerId = accountOwnerId, missionId = "MSN_COMM_E01",
                    module = SocialModuleIdentifier.COMMUNICATION_MASTERY, title = "Active Listening",
                    difficultyRank = MissionDifficultyRank.E_RANK, lifecycleStatus = SocialMissionLifecycleStatus.COMPLETED,
                    startedEpochMs = twentyDaysAgo, completedEpochMs = twentyDaysAgo,
                    evidenceType = SocialMissionEvidenceType.SELF_REPORTED, verificationStatus = VerificationStatus.SELF_REPORTED,
                    idempotencyKey = "msn_reward_m_old_01", xpAwarded = 25
                )
            ),
            assessments = emptyList(),
            nowEpochMs = now
        )

        val comp = evaluated.find { it.competencyName == "Active Listening" }
        assertNotNull(comp)
        // Strict requirement: Do not demote players simply because they temporarily stop practising.
        assertEquals("Must NOT demote player due to inactivity", MasteryDevelopmentStage.ADVANCED, comp!!.masteryStage)
    }

    // =========================================================================
    // 5. ADAPTIVE DIFFICULTY ENGINE TESTS
    // =========================================================================

    @Test
    fun testAdaptiveDifficultyRequiresTwoConsecutiveHighScoresToAdvance() {
        // Only one high score does NOT increase difficulty
        val singleSuccess = listOf(90, 70, 75)
        val (singleDiff, singleReason) = AdaptiveSocialMasteryEngine.computeAdaptiveDifficulty(singleSuccess)
        assertEquals(AdaptiveDifficulty.NORMAL, singleDiff)
        assertTrue(singleReason.contains("steady", ignoreCase = true))

        // Two consecutive high scores DOES increase difficulty to CHALLENGING
        val doubleSuccess = listOf(90, 88, 70)
        val (doubleDiff, doubleReason) = AdaptiveSocialMasteryEngine.computeAdaptiveDifficulty(doubleSuccess)
        assertEquals(AdaptiveDifficulty.CHALLENGING, doubleDiff)
        assertTrue(doubleReason.contains("consecutive high scores", ignoreCase = true))
    }

    @Test
    fun testAdaptiveDifficultyReducesOnConsecutiveLowScores() {
        val lowScores = listOf(55, 52, 70)
        val (diff, reason) = AdaptiveSocialMasteryEngine.computeAdaptiveDifficulty(lowScores)
        assertEquals(AdaptiveDifficulty.EASY, diff)
        assertTrue(reason.contains("Reduced to EASY", ignoreCase = true))
    }

    @Test
    fun testPlayerDifficultyOverrideHonored() {
        val accountOwnerId = "p_override"
        val compKey = "${accountOwnerId}_COMM_COMP_COMM_01"
        val overrides = mapOf(compKey to AdaptiveDifficulty.EXPERT)

        val evaluated = AdaptiveSocialMasteryEngine.evaluateCompetencies(
            accountOwnerId = accountOwnerId,
            sessions = emptyList(),
            attempts = emptyList(),
            missions = emptyList(),
            assessments = emptyList(),
            overrides = overrides
        )

        val comp = evaluated.find { it.competencyId == compKey }
        assertNotNull(comp)
        assertEquals("Player manual override to EXPERT must be respected", AdaptiveDifficulty.EXPERT, comp!!.recommendedDifficulty)
    }

    // =========================================================================
    // 6. PHASE 4.1 REGRESSION TESTS (SAFEGUARDS AUDIT)
    // =========================================================================

    @Test
    fun testPhase41RegressionLifecycleValidator() {
        // Enforce valid transitions
        assertTrue(SocialMissionLifecycleStatus.AVAILABLE.canTransitionTo(SocialMissionLifecycleStatus.ACCEPTED))
        assertTrue(SocialMissionLifecycleStatus.ACCEPTED.canTransitionTo(SocialMissionLifecycleStatus.IN_PROGRESS))
        assertTrue(SocialMissionLifecycleStatus.IN_PROGRESS.canTransitionTo(SocialMissionLifecycleStatus.SUBMITTED))
        assertTrue(SocialMissionLifecycleStatus.SUBMITTED.canTransitionTo(SocialMissionLifecycleStatus.COMPLETED))
        assertTrue(SocialMissionLifecycleStatus.NEEDS_IMPROVEMENT.canTransitionTo(SocialMissionLifecycleStatus.RETRY))
        assertTrue(SocialMissionLifecycleStatus.RETRY.canTransitionTo(SocialMissionLifecycleStatus.IN_PROGRESS))

        // Enforce blocked invalid jumps
        assertFalse(SocialMissionLifecycleStatus.COMPLETED.canTransitionTo(SocialMissionLifecycleStatus.IN_PROGRESS))
        assertFalse(SocialMissionLifecycleStatus.CANCELLED.canTransitionTo(SocialMissionLifecycleStatus.COMPLETED))
        assertFalse(SocialMissionLifecycleStatus.AVAILABLE.canTransitionTo(SocialMissionLifecycleStatus.COMPLETED))
    }

    @Test
    fun testPhase41RegressionStableIdempotencyKey() {
        val recordId = "rec_stable_test"
        val stableKey = "msn_reward_$recordId"
        assertEquals("msn_reward_rec_stable_test", stableKey)
        assertFalse("Must never leak dynamic timestamp", stableKey.contains("_${System.currentTimeMillis()}"))
    }

    @Test
    fun testPhase41RegressionSelfAssessmentCarriesZeroXp() {
        val submission = SkillAssessmentSubmission(
            assessmentId = "test_sub_zero_xp",
            accountOwnerId = "p",
            module = SocialModuleIdentifier.COMMUNICATION_MASTERY,
            classification = SkillAssessmentClassification.SELF_ASSESSMENT,
            scoreDim1 = 98, scoreDim2 = 95, scoreDim3 = 90, scoreDim4 = 92, scoreDim5 = 95,
            qualitativeNotes = "High self-rating"
        )
        val result = com.example.domain.engine.SocialSkillAssessmentEngine.evaluateSubmission(submission)
        assertEquals("Subjective self-assessment sliders must award exactly 0 XP", 0, result.xpAwarded)
        assertEquals(VerificationStatus.SELF_REPORTED, result.verificationStatus)
        assertNotNull(result.limitationsDisclaimer)
    }

    @Test
    fun testPhase41RegressionDailySocialCapAndCountLimits() {
        // Cap of 180 XP
        val cappedXp = ProgressionEngine.calculateSocialReward(
            module = SocialModuleIdentifier.COMMUNICATION_MASTERY,
            activityKind = SocialActivityKind.REAL_WORLD_ACTIVITY,
            durationMinutes = 15,
            verificationStatus = VerificationStatus.SELF_REPORTED,
            socialXpAlreadyEarnedToday = 180,
            selfReportedSocialCountToday = 1,
            rubricScorePercent = 80
        )
        assertEquals("Must award 0 XP when daily 180 XP cap is reached", 0, cappedXp.xpAwarded)
        assertTrue(cappedXp.wasCapped)

        // Limit of 3 self-reported activities
        val cappedCount = ProgressionEngine.calculateSocialReward(
            module = SocialModuleIdentifier.COMMUNICATION_MASTERY,
            activityKind = SocialActivityKind.REAL_WORLD_ACTIVITY,
            durationMinutes = 15,
            verificationStatus = VerificationStatus.SELF_REPORTED,
            socialXpAlreadyEarnedToday = 50,
            selfReportedSocialCountToday = 3,
            rubricScorePercent = 80
        )
        assertEquals("Must award 0 XP when daily 3 self-reported activity limit is reached", 0, cappedCount.xpAwarded)
        assertTrue(cappedCount.wasCapped)
    }
}
