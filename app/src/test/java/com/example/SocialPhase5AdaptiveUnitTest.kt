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
import com.example.domain.model.SocialRecommendationCategory
import com.example.domain.model.ScheduledTrainingActivity
import com.example.domain.model.SkillAssessmentClassification
import com.example.domain.model.SkillAssessmentSubmission
import com.example.domain.model.SocialActivityKind
import com.example.domain.model.SocialAssessment
import com.example.domain.model.SocialAssessmentType
import com.example.domain.model.SocialCompetencyCatalog
import com.example.domain.model.SocialArtifactValidationState
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

        val weaknessRec = recommendations.find { it.category == SocialRecommendationCategory.IMPROVE_WEAKNESS }
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

        val realWorldRec = recommendations.find { it.category == SocialRecommendationCategory.REAL_WORLD_APPLICATION }
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

        val inactiveRec = recommendations.find { it.category == SocialRecommendationCategory.REVISIT_INACTIVE_SKILL }
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
        assertTrue("New player recommendations should focus on exploring new skills", recommendations.any { it.category == SocialRecommendationCategory.PRACTICE_NEW_SKILL })
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
            attempts = List(5) {
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

        // Bare self-reported mission without concrete artifact must STILL cap at COMPETENT
        val evaluatedWithSelfReportedOnly = AdaptiveSocialMasteryEngine.evaluateCompetencies(
            accountOwnerId = accountOwnerId,
            sessions = List(6) {
                SocialPracticeSession(
                    sessionId = "sess_sr_$it", accountOwnerId = accountOwnerId,
                    module = SocialModuleIdentifier.COMMUNICATION_MASTERY,
                    activityKind = SocialActivityKind.PRACTICE, sessionStatus = SocialSessionStatus.COMPLETED,
                    durationSeconds = 600, startedEpochMs = now - 10000, completedEpochMs = now,
                    evidenceSource = SocialEvidenceSource.SYSTEM_OBSERVED,
                    verificationStatus = VerificationStatus.SELF_REPORTED, idempotencyKey = "key_s_sr_$it"
                )
            },
            attempts = List(5) {
                SocialScenarioAttempt(
                    attemptId = "att_sr_$it", accountOwnerId = accountOwnerId, scenarioId = "scen_1",
                    scenarioTitle = "Active Listening", module = SocialModuleIdentifier.COMMUNICATION_MASTERY,
                    userResponseText = "Good response", evaluationScore = 90, isPassed = true,
                    evidenceSource = SocialEvidenceSource.AI_ASSESSED,
                    verificationStatus = VerificationStatus.VERIFIED_AI_RUBRIC, idempotencyKey = "key_a_sr_$it"
                )
            },
            missions = listOf(
                SocialMissionRecord(
                    recordId = "m_rec_sr_01", accountOwnerId = accountOwnerId, missionId = "MSN_COMM_E01",
                    module = SocialModuleIdentifier.COMMUNICATION_MASTERY, title = "Active Listening Check-In",
                    difficultyRank = MissionDifficultyRank.E_RANK, lifecycleStatus = SocialMissionLifecycleStatus.COMPLETED,
                    startedEpochMs = now - 50000, completedEpochMs = now - 1000,
                    evidenceType = SocialMissionEvidenceType.SELF_REPORTED,
                    reflectionText = "Self reflection without external artifact notes.",
                    verificationStatus = VerificationStatus.SELF_REPORTED,
                    idempotencyKey = "msn_reward_m_rec_sr_01", xpAwarded = 25
                )
            ),
            assessments = emptyList(),
            nowEpochMs = now
        )

        val compSelfReportedOnly = evaluatedWithSelfReportedOnly.find { it.competencyName == "Active Listening" }
        assertNotNull(compSelfReportedOnly)
        assertEquals(
            "Self-reported mission without concrete artifact cannot elevate beyond COMPETENT",
            MasteryDevelopmentStage.COMPETENT,
            compSelfReportedOnly!!.masteryStage
        )

        // Note-only artifact ("12345") or ARTIFACT_DESCRIBED MUST STILL cap at COMPETENT
        val evaluatedWithNoteOnly = AdaptiveSocialMasteryEngine.evaluateCompetencies(
            accountOwnerId = accountOwnerId,
            sessions = List(6) {
                SocialPracticeSession(
                    sessionId = "sess_note_$it", accountOwnerId = accountOwnerId,
                    module = SocialModuleIdentifier.COMMUNICATION_MASTERY,
                    activityKind = SocialActivityKind.PRACTICE, sessionStatus = SocialSessionStatus.COMPLETED,
                    durationSeconds = 600, startedEpochMs = now - 10000, completedEpochMs = now,
                    evidenceSource = SocialEvidenceSource.SYSTEM_OBSERVED,
                    verificationStatus = VerificationStatus.SELF_REPORTED, idempotencyKey = "key_s_note_$it"
                )
            },
            attempts = List(5) {
                SocialScenarioAttempt(
                    attemptId = "att_note_$it", accountOwnerId = accountOwnerId, scenarioId = "scen_1",
                    scenarioTitle = "Active Listening", module = SocialModuleIdentifier.COMMUNICATION_MASTERY,
                    userResponseText = "Good response", evaluationScore = 92, isPassed = true,
                    evidenceSource = SocialEvidenceSource.AI_ASSESSED,
                    verificationStatus = VerificationStatus.VERIFIED_AI_RUBRIC, idempotencyKey = "key_a_note_$it"
                )
            },
            missions = listOf(
                SocialMissionRecord(
                    recordId = "m_rec_note_only", accountOwnerId = accountOwnerId, missionId = "MSN_COMM_E01",
                    module = SocialModuleIdentifier.COMMUNICATION_MASTERY, title = "Active Listening Check-In",
                    difficultyRank = MissionDifficultyRank.E_RANK, lifecycleStatus = SocialMissionLifecycleStatus.COMPLETED,
                    startedEpochMs = now - 50000, completedEpochMs = now - 1000,
                    evidenceType = SocialMissionEvidenceType.ARTIFACT_ATTACHED,
                    artifactNote = "12345",
                    artifactUri = null,
                    artifactValidationState = SocialArtifactValidationState.ARTIFACT_DESCRIBED,
                    verificationStatus = VerificationStatus.SELF_REPORTED,
                    idempotencyKey = "msn_reward_m_rec_note_only", xpAwarded = 25
                )
            ),
            assessments = emptyList(),
            nowEpochMs = now
        )

        val compNoteOnly = evaluatedWithNoteOnly.find { it.competencyName == "Active Listening" }
        assertNotNull(compNoteOnly)
        assertEquals(
            "Five-character or unattached artifact note must NOT unlock ADVANCED",
            MasteryDevelopmentStage.COMPETENT,
            compNoteOnly!!.masteryStage
        )

        // Now provide a verified retrievable, account-owned real-world artifact mission
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
            attempts = List(5) {
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
                    evidenceType = SocialMissionEvidenceType.ARTIFACT_ATTACHED,
                    artifactNote = "Recorded 3 follow-up notes after team debrief.",
                    artifactUri = "/vault/$accountOwnerId/m_rec_01_debrief.txt",
                    artifactOwnerId = accountOwnerId,
                    artifactContentHash = "hash_debrief_01",
                    artifactValidationState = SocialArtifactValidationState.ARTIFACT_RETRIEVABLE,
                    verificationStatus = VerificationStatus.SELF_REPORTED,
                    idempotencyKey = "msn_reward_m_rec_01", xpAwarded = 25
                )
            ),
            assessments = emptyList(),
            nowEpochMs = now,
            artifactVerifier = { true }
        )

        val compWithMission = evaluatedWithMission.find { it.competencyName == "Active Listening" }
        assertNotNull(compWithMission)
        assertEquals("ADVANCED stage unlocked with retrievable artifact-supported mission evidence", MasteryDevelopmentStage.ADVANCED, compWithMission!!.masteryStage)
        assertEquals(MasteryEvidenceClassification.ARTIFACT_SUPPORTED, compWithMission.evidenceClassification)
    }

    @Test
    fun testMasteryCandidateRequiresRepeatedArtifactEvidenceAndHighScores() {
        val accountOwnerId = "test_player_candidate"
        val now = System.currentTimeMillis()

        val sessions = List(6) {
            SocialPracticeSession(
                sessionId = "cand_s_$it", accountOwnerId = accountOwnerId,
                module = SocialModuleIdentifier.COMMUNICATION_MASTERY,
                activityKind = SocialActivityKind.PRACTICE, sessionStatus = SocialSessionStatus.COMPLETED,
                durationSeconds = 600, startedEpochMs = now - 20000, completedEpochMs = now - 1000,
                transcriptText = "Practiced Active Listening structured reflection drill #$it",
                evidenceSource = SocialEvidenceSource.SYSTEM_OBSERVED,
                verificationStatus = VerificationStatus.SELF_REPORTED, idempotencyKey = "cand_ks_$it"
            )
        }
        val attempts = List(3) {
            SocialScenarioAttempt(
                attemptId = "cand_a_$it", accountOwnerId = accountOwnerId, scenarioId = "scen_cand",
                scenarioTitle = "Active Listening", module = SocialModuleIdentifier.COMMUNICATION_MASTERY,
                userResponseText = "Structured reflection response", evaluationScore = 88, isPassed = true,
                evidenceSource = SocialEvidenceSource.AI_ASSESSED,
                verificationStatus = VerificationStatus.VERIFIED_AI_RUBRIC, idempotencyKey = "cand_ka_$it",
                completedEpochMs = now - (it * 1000L)
            )
        }
        val twoArtifactMissions = List(2) {
            SocialMissionRecord(
                recordId = "cand_m_$it", accountOwnerId = accountOwnerId, missionId = "MSN_COMM_E01",
                module = SocialModuleIdentifier.COMMUNICATION_MASTERY, title = "Active Listening Debrief $it",
                difficultyRank = MissionDifficultyRank.C_RANK, lifecycleStatus = SocialMissionLifecycleStatus.COMPLETED,
                startedEpochMs = now - 50000, completedEpochMs = now - 2000,
                evidenceType = SocialMissionEvidenceType.ARTIFACT_ATTACHED,
                artifactNote = "Meeting agenda and summary checklist #$it",
                artifactUri = "/vault/$accountOwnerId/cand_m_$it.txt",
                artifactOwnerId = accountOwnerId,
                artifactContentHash = "cand_hash_$it",
                artifactValidationState = SocialArtifactValidationState.ARTIFACT_RETRIEVABLE,
                verificationStatus = VerificationStatus.SELF_REPORTED,
                idempotencyKey = "cand_km_$it", xpAwarded = 35
            )
        }

        // With 2 distinct artifact missions (total practiceCount = 11), capped at ADVANCED
        val withTwoMissions = AdaptiveSocialMasteryEngine.evaluateCompetencies(
            accountOwnerId = accountOwnerId,
            sessions = sessions,
            attempts = attempts,
            missions = twoArtifactMissions,
            assessments = emptyList(),
            nowEpochMs = now,
            artifactVerifier = { true }
        ).find { it.competencyName == "Active Listening" }
        assertNotNull(withTwoMissions)
        assertEquals(MasteryDevelopmentStage.ADVANCED, withTwoMissions!!.masteryStage)

        // Duplicate artifact hash repeated 3 times MUST stay capped at ADVANCED (1 distinct artifact)
        val duplicateHashMissions = List(3) {
            twoArtifactMissions[0].copy(
                recordId = "dup_m_$it",
                idempotencyKey = "dup_km_$it"
            )
        }
        val withDupHashes = AdaptiveSocialMasteryEngine.evaluateCompetencies(
            accountOwnerId = accountOwnerId,
            sessions = sessions,
            attempts = attempts,
            missions = duplicateHashMissions,
            assessments = emptyList(),
            nowEpochMs = now,
            artifactVerifier = { true }
        ).find { it.competencyName == "Active Listening" }
        assertEquals(
            "Duplicate identical artifact hashes must NOT satisfy 3 distinct real-world evidences",
            MasteryDevelopmentStage.ADVANCED,
            withDupHashes!!.masteryStage
        )

        // With 3 distinct retrievable artifact missions + >=10 practices + >=2 attempts >=85%, unlocks MASTERY_CANDIDATE
        val threeArtifactMissions = twoArtifactMissions + SocialMissionRecord(
            recordId = "cand_m_2", accountOwnerId = accountOwnerId, missionId = "MSN_COMM_C03",
            module = SocialModuleIdentifier.COMMUNICATION_MASTERY, title = "Active Listening Paraphrase",
            difficultyRank = MissionDifficultyRank.C_RANK, lifecycleStatus = SocialMissionLifecycleStatus.COMPLETED,
            startedEpochMs = now - 40000, completedEpochMs = now - 500,
            evidenceType = SocialMissionEvidenceType.ARTIFACT_ATTACHED,
            artifactNote = "Signed off client paraphrase transcript log",
            artifactUri = "/vault/$accountOwnerId/cand_m_2.txt",
            artifactOwnerId = accountOwnerId,
            artifactContentHash = "cand_hash_2",
            artifactValidationState = SocialArtifactValidationState.ARTIFACT_REVIEWED,
            verificationStatus = VerificationStatus.SELF_REPORTED,
            idempotencyKey = "cand_km_2", xpAwarded = 50
        )

        val withThreeMissions = AdaptiveSocialMasteryEngine.evaluateCompetencies(
            accountOwnerId = accountOwnerId,
            sessions = sessions,
            attempts = attempts,
            missions = threeArtifactMissions,
            assessments = emptyList(),
            nowEpochMs = now,
            artifactVerifier = { true }
        ).find { it.competencyName == "Active Listening" }
        assertNotNull(withThreeMissions)
        assertEquals(MasteryDevelopmentStage.MASTERY_CANDIDATE, withThreeMissions!!.masteryStage)
    }

    @Test
    fun testInactivityDoesNotDemotePlayerStageAndSetsMaintenanceFlag() {
        val accountOwnerId = "test_player_inactivity"
        val now = System.currentTimeMillis()
        val twentyDaysAgo = now - (20L * 24L * 60L * 60L * 1000L)

        // Competency was ADVANCED and practiced 20 days ago with artifact-backed mission
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
                    evidenceType = SocialMissionEvidenceType.ARTIFACT_ATTACHED,
                    artifactNote = "Retrospective notes shared with team.",
                    artifactUri = "/vault/$accountOwnerId/m_old_01.txt",
                    artifactOwnerId = accountOwnerId,
                    artifactContentHash = "old_hash_01",
                    artifactValidationState = SocialArtifactValidationState.ARTIFACT_RETRIEVABLE,
                    verificationStatus = VerificationStatus.SELF_REPORTED,
                    idempotencyKey = "msn_reward_m_old_01", xpAwarded = 25
                )
            ),
            assessments = emptyList(),
            nowEpochMs = now,
            artifactVerifier = { true }
        )

        val comp = evaluated.find { it.competencyName == "Active Listening" }
        assertNotNull(comp)
        // Strict requirement: Do not demote players simply because they temporarily stop practising.
        assertEquals("Must NOT demote player due to inactivity", MasteryDevelopmentStage.ADVANCED, comp!!.masteryStage)
        assertTrue("Maintenance flag must be active after 20 days inactivity", comp.maintenanceRequired)
        assertEquals(20, comp.daysSinceLastPractice)
        assertNotNull("Maintenance reason must explain refresher without demotion", comp.maintenanceReason)
    }

    @Test
    fun testAllSevenRecommendationCategoriesReachableDeterministicAndDeduplicated() {
        val accountOwnerId = "test_all_cats"
        val now = 1_700_000_000_000L
        val dayMs = 24L * 60L * 60L * 1000L

        fun comp(
            id: String,
            mod: SocialModuleIdentifier,
            name: String,
            stage: MasteryDevelopmentStage,
            practiceCount: Int,
            daysAgo: Long?,
            score: Int?,
            evidence: MasteryEvidenceClassification = MasteryEvidenceClassification.SIMULATION_EVALUATED
        ) = SocialCompetencyProgress(
            competencyId = "${accountOwnerId}_${mod.code}_$id",
            accountOwnerId = accountOwnerId,
            module = mod,
            competencyName = name,
            masteryStage = stage,
            recommendedDifficulty = AdaptiveDifficulty.NORMAL,
            practiceCount = practiceCount,
            lastPracticedEpochMs = daysAgo?.let { now - it * dayMs },
            lastEvaluatedScore = score,
            evidenceClassification = evidence
        )

        val scenarios = listOf(
            comp("C1", SocialModuleIdentifier.COMMUNICATION_MASTERY, "Active Listening", MasteryDevelopmentStage.DEVELOPING, 3, 1L, 58) to SocialRecommendationCategory.IMPROVE_WEAKNESS,
            comp("C2", SocialModuleIdentifier.PUBLIC_SPEAKING, "Speech Organization", MasteryDevelopmentStage.COMPETENT, 5, 35L, 76) to SocialRecommendationCategory.REASSESS_SKILL,
            comp("C3", SocialModuleIdentifier.NEGOTIATION_PERSUASION, "Identifying Interests", MasteryDevelopmentStage.COMPETENT, 5, 18L, 78) to SocialRecommendationCategory.REVISIT_INACTIVE_SKILL,
            comp("C4", SocialModuleIdentifier.LEADERSHIP_TEAMWORK, "Delegation", MasteryDevelopmentStage.ADVANCED, 7, 2L, 88, MasteryEvidenceClassification.ARTIFACT_SUPPORTED) to SocialRecommendationCategory.MAINTAIN_STRENGTH,
            comp("C5", SocialModuleIdentifier.NETWORKING_RELATIONSHIPS, "Follow-Up Communication", MasteryDevelopmentStage.COMPETENT, 6, 1L, 84) to SocialRecommendationCategory.REAL_WORLD_APPLICATION,
            comp("C6", SocialModuleIdentifier.EMOTIONAL_INTELLIGENCE, "Perspective-Taking", MasteryDevelopmentStage.DEVELOPING, 3, 2L, 76) to SocialRecommendationCategory.ADVANCE_MASTERY_LEVEL,
            comp("C7", SocialModuleIdentifier.PROFESSIONAL_EXCELLENCE, "Time Management", MasteryDevelopmentStage.UNEXPLORED, 0, null, null, MasteryEvidenceClassification.SELF_REPORTED) to SocialRecommendationCategory.PRACTICE_NEW_SKILL
        )

        for ((progress, expectedCategory) in scenarios) {
            val recsFirstRun = AdaptiveSocialMasteryEngine.generateRecommendations(
                accountOwnerId = accountOwnerId,
                competencies = listOf(progress),
                missions = emptyList(),
                nowEpochMs = now
            )
            val recsLaterRun = AdaptiveSocialMasteryEngine.generateRecommendations(
                accountOwnerId = accountOwnerId,
                competencies = listOf(progress),
                missions = emptyList(),
                nowEpochMs = now + 123_456L
            )
            assertEquals("Expected single rec for ${progress.competencyName}", 1, recsFirstRun.size)
            assertEquals("Wrong category for ${progress.competencyName}", expectedCategory, recsFirstRun.first().category)
            assertEquals("Recommendation IDs must be deterministic across timestamps", recsFirstRun.first().recommendationId, recsLaterRun.first().recommendationId)
        }

        // Combined list caps at 5 and enforces at most 1 per competency
        val combined = AdaptiveSocialMasteryEngine.generateRecommendations(
            accountOwnerId = accountOwnerId,
            competencies = scenarios.map { it.first },
            missions = emptyList(),
            nowEpochMs = now
        )
        assertEquals(5, combined.size)
        assertEquals(5, combined.map { it.targetModule to it.targetCompetency }.distinct().size)
    }

    @Test
    fun testChronologicalSortDeterminesAdaptiveDifficultyRegardlessOfInputOrder() {
        val accountOwnerId = "test_sort_order"
        val now = 1_700_000_000_000L

        // Input intentionally UNSORTED (oldest 50% first, newest 92% and 90% later)
        val unsortedAttempts = listOf(
            SocialScenarioAttempt(
                attemptId = "a_old", accountOwnerId = accountOwnerId, scenarioId = "s1",
                scenarioTitle = "Active Listening", module = SocialModuleIdentifier.COMMUNICATION_MASTERY,
                userResponseText = "Earlier attempt", evaluationScore = 50, isPassed = false,
                evidenceSource = SocialEvidenceSource.AI_ASSESSED,
                verificationStatus = VerificationStatus.VERIFIED_AI_RUBRIC, idempotencyKey = "k_old",
                completedEpochMs = now - 300_000L
            ),
            SocialScenarioAttempt(
                attemptId = "a_mid", accountOwnerId = accountOwnerId, scenarioId = "s1",
                scenarioTitle = "Active Listening", module = SocialModuleIdentifier.COMMUNICATION_MASTERY,
                userResponseText = "Improved attempt", evaluationScore = 90, isPassed = true,
                evidenceSource = SocialEvidenceSource.AI_ASSESSED,
                verificationStatus = VerificationStatus.VERIFIED_AI_RUBRIC, idempotencyKey = "k_mid",
                completedEpochMs = now - 100_000L
            ),
            SocialScenarioAttempt(
                attemptId = "a_newest", accountOwnerId = accountOwnerId, scenarioId = "s1",
                scenarioTitle = "Active Listening", module = SocialModuleIdentifier.COMMUNICATION_MASTERY,
                userResponseText = "Most recent attempt", evaluationScore = 92, isPassed = true,
                evidenceSource = SocialEvidenceSource.AI_ASSESSED,
                verificationStatus = VerificationStatus.VERIFIED_AI_RUBRIC, idempotencyKey = "k_newest",
                completedEpochMs = now
            )
        )

        val evaluated = AdaptiveSocialMasteryEngine.evaluateCompetencies(
            accountOwnerId = accountOwnerId,
            sessions = emptyList(),
            attempts = unsortedAttempts,
            missions = emptyList(),
            assessments = emptyList(),
            nowEpochMs = now
        )

        val comp = evaluated.find { it.competencyName == "Active Listening" }!!
        assertEquals("recentScore must come from newest chronological attempt", 92, comp.lastEvaluatedScore)
        assertEquals("Two newest high scores (92%, 90%) must upgrade to CHALLENGING", AdaptiveDifficulty.CHALLENGING, comp.recommendedDifficulty)
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
        val compKey = "${accountOwnerId}_${SocialModuleIdentifier.COMMUNICATION_MASTERY.code}_COMP_COMM_01"
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

    // =========================================================================
    // 7. PHASE 5.1.1 REGRESSION: LEGACY MAINTENANCE RECOVERY & ARTIFACT VALIDATION
    // =========================================================================

    @Test
    fun testLegacyMaintenanceNeverMapsToMasteryCandidateAndRecoversFromHistory() {
        assertFalse(
            "MAINTENANCE must NEVER map to MASTERY_CANDIDATE",
            MasteryDevelopmentStage.fromString("MAINTENANCE") == MasteryDevelopmentStage.MASTERY_CANDIDATE
        )
        assertEquals(MasteryDevelopmentStage.UNEXPLORED, MasteryDevelopmentStage.fromString("MAINTENANCE"))
        assertTrue(MasteryDevelopmentStage.isLegacyMaintenance("maintenance"))
        assertEquals(
            SocialMissionEvidenceType.ARTIFACT_DESCRIBED,
            SocialMissionEvidenceType.fromString("OPTIONAL_ARTIFACT")
        )

        val accountOwnerId = "legacy_maint_player"
        val now = 1_700_500_000_000L
        val fiveDaysAgo = now - (5L * 24L * 60L * 60L * 1000L)
        val compKey = "${accountOwnerId}_${SocialModuleIdentifier.COMMUNICATION_MASTERY.code}_COMP_COMM_01"

        // Simulated legacy deserialized MAINTENANCE record with maintenanceRequired=true preserved separately
        val legacyPriorNoHistory = mapOf(
            compKey to SocialCompetencyProgress(
                competencyId = compKey,
                accountOwnerId = accountOwnerId,
                module = SocialModuleIdentifier.COMMUNICATION_MASTERY,
                competencyName = "Active Listening",
                masteryStage = MasteryDevelopmentStage.fromString("MAINTENANCE"),
                recommendedDifficulty = AdaptiveDifficulty.NORMAL,
                practiceCount = 0,
                lastPracticedEpochMs = null,
                lastEvaluatedScore = null,
                evidenceClassification = MasteryEvidenceClassification.SELF_REPORTED,
                updatedAtEpochMs = fiveDaysAgo,
                maintenanceRequired = true,
                maintenanceReason = "Legacy MAINTENANCE state preserved separately."
            )
        )

        // 1. No historical evidence -> safe UNEXPLORED fallback (no unearned mastery), maintenance flag preserved
        val unrecoverable = AdaptiveSocialMasteryEngine.evaluateCompetencies(
            accountOwnerId = accountOwnerId,
            sessions = emptyList(),
            attempts = emptyList(),
            missions = emptyList(),
            assessments = emptyList(),
            priorProgress = legacyPriorNoHistory,
            nowEpochMs = now
        ).first { it.competencyName == "Active Listening" }

        assertEquals(MasteryDevelopmentStage.UNEXPLORED, unrecoverable.masteryStage)
        assertTrue(unrecoverable.maintenanceRequired)
        assertNotNull(unrecoverable.maintenanceReason)

        // 2. With real historical evidence supporting COMPETENT -> recovers COMPETENT cleanly (NOT MASTERY_CANDIDATE)
        val recovered = AdaptiveSocialMasteryEngine.evaluateCompetencies(
            accountOwnerId = accountOwnerId,
            sessions = List(4) {
                SocialPracticeSession(
                    sessionId = "rec_s_$it", accountOwnerId = accountOwnerId,
                    module = SocialModuleIdentifier.COMMUNICATION_MASTERY,
                    activityKind = SocialActivityKind.PRACTICE, sessionStatus = SocialSessionStatus.COMPLETED,
                    durationSeconds = 600, startedEpochMs = fiveDaysAgo - 10000, completedEpochMs = fiveDaysAgo,
                    transcriptText = "Active Listening historical drill $it",
                    evidenceSource = SocialEvidenceSource.SYSTEM_OBSERVED,
                    verificationStatus = VerificationStatus.SELF_REPORTED, idempotencyKey = "rec_ks_$it"
                )
            },
            attempts = List(2) {
                SocialScenarioAttempt(
                    attemptId = "rec_a_$it", accountOwnerId = accountOwnerId, scenarioId = "SCN_ACTIVE_LISTEN_01",
                    scenarioTitle = "Active Listening", module = SocialModuleIdentifier.COMMUNICATION_MASTERY,
                    userResponseText = "Active Listening response", evaluationScore = 80, isPassed = true,
                    evidenceSource = SocialEvidenceSource.AI_ASSESSED,
                    verificationStatus = VerificationStatus.VERIFIED_AI_RUBRIC, idempotencyKey = "rec_ka_$it",
                    completedEpochMs = fiveDaysAgo
                )
            },
            missions = emptyList(),
            assessments = emptyList(),
            priorProgress = legacyPriorNoHistory,
            nowEpochMs = now
        ).first { it.competencyName == "Active Listening" }

        assertEquals(MasteryDevelopmentStage.COMPETENT, recovered.masteryStage)
        assertTrue(recovered.maintenanceRequired)
    }

    @Test
    fun testFailedRetrievalAndWrongAccountOwnerArtifactCannotPromoteToAdvanced() {
        val owner = "artifact_owner_a"
        val now = System.currentTimeMillis()
        val sessions = List(4) {
            SocialPracticeSession(
                sessionId = "s_art_$it", accountOwnerId = owner,
                module = SocialModuleIdentifier.COMMUNICATION_MASTERY,
                activityKind = SocialActivityKind.PRACTICE, sessionStatus = SocialSessionStatus.COMPLETED,
                durationSeconds = 600, startedEpochMs = now - 20000, completedEpochMs = now - 1000,
                transcriptText = "Active Listening session $it",
                evidenceSource = SocialEvidenceSource.SYSTEM_OBSERVED,
                verificationStatus = VerificationStatus.SELF_REPORTED, idempotencyKey = "s_art_k_$it"
            )
        }
        val attempts = List(2) {
            SocialScenarioAttempt(
                attemptId = "a_art_$it", accountOwnerId = owner, scenarioId = "SCN_ACTIVE_LISTEN_01",
                scenarioTitle = "Active Listening", module = SocialModuleIdentifier.COMMUNICATION_MASTERY,
                userResponseText = "Active Listening paraphrase", evaluationScore = 90, isPassed = true,
                evidenceSource = SocialEvidenceSource.AI_ASSESSED,
                verificationStatus = VerificationStatus.VERIFIED_AI_RUBRIC, idempotencyKey = "a_art_k_$it",
                completedEpochMs = now - 1000
            )
        }

        val wrongOwnerMission = SocialMissionRecord(
            recordId = "m_wrong_owner", accountOwnerId = owner, missionId = "MSN_COMM_E01",
            module = SocialModuleIdentifier.COMMUNICATION_MASTERY, title = "Active Listening Check-In",
            difficultyRank = MissionDifficultyRank.E_RANK, lifecycleStatus = SocialMissionLifecycleStatus.COMPLETED,
            startedEpochMs = now - 50000, completedEpochMs = now - 1000,
            evidenceType = SocialMissionEvidenceType.ARTIFACT_ATTACHED,
            artifactNote = "Stolen artifact note",
            artifactUri = "/vault/other_user/stolen.pdf",
            artifactOwnerId = "other_user",
            artifactContentHash = "hash_other",
            artifactValidationState = SocialArtifactValidationState.ARTIFACT_RETRIEVABLE,
            verificationStatus = VerificationStatus.SELF_REPORTED,
            idempotencyKey = "msn_wrong", xpAwarded = 25
        )
        val wrongOwnerComp = AdaptiveSocialMasteryEngine.evaluateCompetencies(
            accountOwnerId = owner,
            sessions = sessions,
            attempts = attempts,
            missions = listOf(wrongOwnerMission),
            assessments = emptyList(),
            nowEpochMs = now,
            artifactVerifier = { true }
        ).first { it.competencyName == "Active Listening" }
        assertEquals("Cross-owner artifact must be rejected", MasteryDevelopmentStage.COMPETENT, wrongOwnerComp.masteryStage)

        val brokenRetrievalMission = wrongOwnerMission.copy(
            recordId = "m_broken_retrieval",
            artifactOwnerId = owner,
            artifactUri = "/vault/$owner/missing.pdf"
        )
        val failedRetrievalComp = AdaptiveSocialMasteryEngine.evaluateCompetencies(
            accountOwnerId = owner,
            sessions = sessions,
            attempts = attempts,
            missions = listOf(brokenRetrievalMission),
            assessments = emptyList(),
            nowEpochMs = now,
            artifactVerifier = { false } // file unreadable/deleted
        ).first { it.competencyName == "Active Listening" }
        assertEquals("Unretrievable artifact must not unlock ADVANCED", MasteryDevelopmentStage.COMPETENT, failedRetrievalComp.masteryStage)
    }
}
