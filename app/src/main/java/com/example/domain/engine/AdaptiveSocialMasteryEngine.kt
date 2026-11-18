package com.example.domain.engine

import com.example.domain.model.AdaptiveDifficulty
import com.example.domain.model.MasteryDevelopmentStage
import com.example.domain.model.MasteryEvidenceClassification
import com.example.domain.model.PersonalizedSocialRecommendation
import com.example.domain.model.PersonalizedWeeklyTrainingPlan
import com.example.domain.model.RecommendationCategory
import com.example.domain.model.RecommendationStatus
import com.example.domain.model.ScheduledTrainingActivity
import com.example.domain.model.SocialAssessment
import com.example.domain.model.SocialCompetency
import com.example.domain.model.SocialCompetencyCatalog
import com.example.domain.model.SocialCompetencyProgress
import com.example.domain.model.SocialMasteryPathway
import com.example.domain.model.SocialMissionLifecycleStatus
import com.example.domain.model.SocialMissionRecord
import com.example.domain.model.SocialModuleIdentifier
import com.example.domain.model.SocialPracticeSession
import com.example.domain.model.SocialScenarioAttempt
import com.example.domain.model.SocialWeeklyDevelopmentReport
import com.example.domain.model.TrainingActivityType
import java.util.UUID

/**
 * Milestone 12 - Phase 5: Adaptive Social Mastery & Personalized Development Engine.
 * Responsibilities:
 * - Deterministic competency evaluation across all 8 Social Mastery modules.
 * - Evidence-based mastery progression guard (prevents promoting on self-ratings or XP alone).
 * - Adaptive difficulty engine with gradual transitions and explainable recommendations.
 * - Personalized weekly training planner (offline, non-punitive, and customizable).
 * - Weekly development analytics and explainable reporting.
 */
object AdaptiveSocialMasteryEngine {

    private const val INACTIVITY_THRESHOLD_MS = 14L * 24L * 60L * 60L * 1000L // 14 days
    private const val STALE_ASSESSMENT_THRESHOLD_MS = 30L * 24L * 60L * 60L * 1000L // 30 days

    // =========================================================================
    // 1. COMPETENCY PROGRESS EVALUATION
    // =========================================================================

    fun evaluateCompetencies(
        accountOwnerId: String,
        sessions: List<SocialPracticeSession>,
        attempts: List<SocialScenarioAttempt>,
        missions: List<SocialMissionRecord>,
        assessments: List<SocialAssessment>,
        overrides: Map<String, AdaptiveDifficulty> = emptyMap(),
        nowEpochMs: Long = System.currentTimeMillis()
    ): List<SocialCompetencyProgress> {
        val result = mutableListOf<SocialCompetencyProgress>()

        for (module in SocialModuleIdentifier.entries) {
            val moduleCompetencies = SocialCompetencyCatalog.getCompetenciesForModule(module)
            val moduleSessions = sessions.filter { it.module == module }
            val moduleAttempts = attempts.filter { it.module == module }
            val moduleMissions = missions.filter { it.module == module && it.lifecycleStatus == SocialMissionLifecycleStatus.COMPLETED }
            val moduleAssessments = assessments.filter { it.module == module }

            for ((index, comp) in moduleCompetencies.withIndex()) {
                val compKey = "${accountOwnerId}_${comp.module.code}_${comp.competencyId}"

                // Associate matching scenario attempts, practice sessions, or assessments
                val relevantAttempts = moduleAttempts.filter {
                    it.scenarioTitle.contains(comp.name, ignoreCase = true) ||
                            it.userResponseText.contains(comp.name, ignoreCase = true)
                }
                val relevantMissions = moduleMissions.filter {
                    it.title.contains(comp.name, ignoreCase = true) ||
                            it.reflectionText?.contains(comp.name, ignoreCase = true) == true
                }

                val practiceCount = (moduleSessions.size / moduleCompetencies.size.coerceAtLeast(1)) +
                        relevantAttempts.size + relevantMissions.size

                val recentScore = relevantAttempts.firstOrNull()?.evaluationScore
                    ?: moduleAssessments.firstOrNull()?.overallScore

                val lastSessionTime = moduleSessions.maxOfOrNull { it.completedEpochMs ?: it.startedEpochMs }
                val lastAttemptTime = relevantAttempts.maxOfOrNull { it.completedEpochMs }
                val lastMissionTime = relevantMissions.maxOfOrNull { it.completedEpochMs ?: it.startedEpochMs }
                val lastPracticed = listOfNotNull(lastSessionTime, lastAttemptTime, lastMissionTime).maxOrNull()

                // Evidence Classification
                val evidenceClassification = when {
                    relevantMissions.any { it.artifactNote != null } -> MasteryEvidenceClassification.ARTIFACT_SUPPORTED
                    relevantAttempts.isNotEmpty() -> MasteryEvidenceClassification.SIMULATION_EVALUATED
                    moduleSessions.isNotEmpty() -> MasteryEvidenceClassification.PRACTICE_OBSERVED
                    moduleAssessments.isNotEmpty() -> MasteryEvidenceClassification.SELF_REPORTED
                    else -> MasteryEvidenceClassification.SELF_REPORTED
                }

                // Mastery Development Stage determination
                // Strict rule: ADVANCED and MASTERY_CANDIDATE require real-world mission completion.
                // Inactivity (> 14 days) does NOT demote player.
                val stage = when {
                    practiceCount == 0 -> MasteryDevelopmentStage.UNEXPLORED
                    practiceCount in 1..2 -> MasteryDevelopmentStage.BEGINNER
                    practiceCount in 3..4 || (recentScore != null && recentScore in 70..74) -> MasteryDevelopmentStage.DEVELOPING
                    practiceCount >= 5 && moduleMissions.isEmpty() -> MasteryDevelopmentStage.COMPETENT
                    practiceCount >= 5 && moduleMissions.isNotEmpty() && (recentScore ?: 75) >= 80 -> MasteryDevelopmentStage.ADVANCED
                    practiceCount >= 10 && moduleMissions.size >= 3 && (recentScore ?: 80) >= 85 -> MasteryDevelopmentStage.MASTERY_CANDIDATE
                    practiceCount >= 15 && moduleMissions.size >= 4 -> MasteryDevelopmentStage.MAINTENANCE
                    else -> MasteryDevelopmentStage.COMPETENT
                }

                // Adaptive Difficulty determination
                val autoDiff = computeAdaptiveDifficulty(relevantAttempts.map { it.evaluationScore })
                val finalDiff = overrides[compKey] ?: autoDiff.first

                result.add(
                    SocialCompetencyProgress(
                        competencyId = compKey,
                        accountOwnerId = accountOwnerId,
                        module = comp.module,
                        competencyName = comp.name,
                        masteryStage = stage,
                        recommendedDifficulty = finalDiff,
                        practiceCount = practiceCount,
                        lastPracticedEpochMs = lastPracticed,
                        lastEvaluatedScore = recentScore,
                        evidenceClassification = evidenceClassification,
                        updatedAtEpochMs = nowEpochMs
                    )
                )
            }
        }

        return result
    }

    // =========================================================================
    // 2. ADAPTIVE DIFFICULTY ENGINE
    // =========================================================================

    /**
     * Determines difficulty adjustment deterministically based on historical scores.
     * Rules:
     * - Requires at least 2 consecutive scores >= 85% to advance difficulty.
     * - Reduces difficulty if 2 consecutive scores < 60%.
     * - Never jumps more than 1 level at a time.
     */
    fun computeAdaptiveDifficulty(recentScores: List<Int>): Pair<AdaptiveDifficulty, String> {
        if (recentScores.size < 2) {
            return AdaptiveDifficulty.NORMAL to "Baseline difficulty maintained while establishing performance history."
        }

        val first = recentScores[0]
        val second = recentScores[1]

        return when {
            first >= 85 && second >= 85 -> {
                AdaptiveDifficulty.CHALLENGING to "Upgraded to CHALLENGING based on two consecutive high scores ($first%, $second%)."
            }
            first < 60 && second < 60 -> {
                AdaptiveDifficulty.EASY to "Reduced to EASY to reinforce foundational mechanics after two scores below 60% ($first%, $second%)."
            }
            first >= 75 -> {
                AdaptiveDifficulty.NORMAL to "Maintaining NORMAL difficulty based on steady passing score ($first%)."
            }
            else -> {
                AdaptiveDifficulty.NORMAL to "Maintaining standard difficulty level."
            }
        }
    }

    // =========================================================================
    // 3. PERSONALIZED RECOMMENDATION GENERATION
    // =========================================================================

    fun generateRecommendations(
        accountOwnerId: String,
        competencies: List<SocialCompetencyProgress>,
        missions: List<SocialMissionRecord>,
        nowEpochMs: Long = System.currentTimeMillis()
    ): List<PersonalizedSocialRecommendation> {
        val recommendations = mutableListOf<PersonalizedSocialRecommendation>()

        // 1. Weakness Detection (< 70% evaluated score)
        val weakCompetencies = competencies.filter {
            it.lastEvaluatedScore != null && it.lastEvaluatedScore < 70
        }.sortedBy { it.lastEvaluatedScore }

        for (weak in weakCompetencies.take(2)) {
            val exercise = SocialPracticeCatalog.exercises.firstOrNull { it.module == weak.module }
            val exerciseId = exercise?.exerciseId ?: "EX_COMM_01"
            recommendations.add(
                PersonalizedSocialRecommendation(
                    recommendationId = "REC_WEAK_${weak.module.code}_${System.currentTimeMillis()}_${recommendations.size}",
                    accountOwnerId = accountOwnerId,
                    title = "Reinforce ${weak.competencyName}",
                    targetModule = weak.module,
                    targetCompetency = weak.competencyName,
                    category = RecommendationCategory.IMPROVE_WEAKNESS,
                    explanation = "Observed rubric score was ${weak.lastEvaluatedScore}%. Targeted practice will address frictional gaps before advanced scenarios.",
                    supportingEvidenceReferences = listOf("Persisted assessment score: ${weak.lastEvaluatedScore}% in ${weak.module.displayName}"),
                    recommendedActivityId = exerciseId,
                    recommendedActivityType = "EXERCISE",
                    estimatedDurationMinutes = 10,
                    difficulty = AdaptiveDifficulty.NORMAL,
                    expectedLearningOutcome = "Reinforce core structure and eliminate conversational hesitation in ${weak.competencyName}.",
                    status = RecommendationStatus.NEW,
                    createdAtEpochMs = nowEpochMs
                )
            )
        }

        // 2. Real-World Application (High competence without recent mission)
        val highCompetencies = competencies.filter {
            (it.lastEvaluatedScore ?: 0) >= 80 &&
                    (it.masteryStage == MasteryDevelopmentStage.COMPETENT || it.masteryStage == MasteryDevelopmentStage.ADVANCED)
        }
        for (high in highCompetencies.take(1)) {
            val mission = SocialMissionCatalog.getMissionsForModule(high.module).firstOrNull()
            if (mission != null) {
                recommendations.add(
                    PersonalizedSocialRecommendation(
                        recommendationId = "REC_REAL_${high.module.code}_${System.currentTimeMillis()}_${recommendations.size}",
                        accountOwnerId = accountOwnerId,
                        title = "Apply in Reality: ${mission.title}",
                        targetModule = high.module,
                        targetCompetency = high.competencyName,
                        category = RecommendationCategory.REAL_WORLD_APPLICATION,
                        explanation = "Strong simulation mastery (${high.lastEvaluatedScore}%). Ground simulated fluency into genuine interpersonal evidence.",
                        supportingEvidenceReferences = listOf("High simulation performance (${high.lastEvaluatedScore}%) in ${high.competencyName}"),
                        recommendedActivityId = mission.missionId,
                        recommendedActivityType = "MISSION",
                        estimatedDurationMinutes = mission.estimatedDurationMinutes,
                        difficulty = AdaptiveDifficulty.CHALLENGING,
                        expectedLearningOutcome = "Execute ${mission.title} in real-world dialogue and capture structured reflection.",
                        status = RecommendationStatus.NEW,
                        createdAtEpochMs = nowEpochMs
                    )
                )
            }
        }

        // 3. Revisit Inactive Skills (> 14 days without practice)
        val inactiveCompetencies = competencies.filter {
            it.practiceCount > 0 &&
                    it.lastPracticedEpochMs != null &&
                    (nowEpochMs - it.lastPracticedEpochMs) >= INACTIVITY_THRESHOLD_MS
        }
        for (inactive in inactiveCompetencies.take(1)) {
            val exercise = SocialPracticeCatalog.exercises.firstOrNull { it.module == inactive.module }
            val exerciseId = exercise?.exerciseId ?: "EX_COMM_02"
            val daysAgo = (nowEpochMs - inactive.lastPracticedEpochMs!!) / (24 * 60 * 60 * 1000)
            recommendations.add(
                PersonalizedSocialRecommendation(
                    recommendationId = "REC_INACTIVE_${inactive.module.code}_${System.currentTimeMillis()}_${recommendations.size}",
                    accountOwnerId = accountOwnerId,
                    title = "Refresher: ${inactive.competencyName}",
                    targetModule = inactive.module,
                    targetCompetency = inactive.competencyName,
                    category = RecommendationCategory.REVISIT_INACTIVE_SKILL,
                    explanation = "No practice recorded in $daysAgo days. A 5-minute refresher prevents behavioral decay without affecting existing rank.",
                    supportingEvidenceReferences = listOf("Last active $daysAgo days ago in ${inactive.module.displayName}"),
                    recommendedActivityId = exerciseId,
                    recommendedActivityType = "EXERCISE",
                    estimatedDurationMinutes = 5,
                    difficulty = AdaptiveDifficulty.EASY,
                    expectedLearningOutcome = "Reactivate behavioral muscle memory in ${inactive.competencyName}.",
                    status = RecommendationStatus.NEW,
                    createdAtEpochMs = nowEpochMs
                )
            )
        }

        // 4. Practice New Skill (Unexplored competencies)
        val unexplored = competencies.filter { it.masteryStage == MasteryDevelopmentStage.UNEXPLORED }
        for (unex in unexplored.take(1)) {
            val exercise = SocialPracticeCatalog.exercises.firstOrNull { it.module == unex.module }
            val exerciseId = exercise?.exerciseId ?: "EX_COMM_01"
            recommendations.add(
                PersonalizedSocialRecommendation(
                    recommendationId = "REC_NEW_${unex.module.code}_${System.currentTimeMillis()}_${recommendations.size}",
                    accountOwnerId = accountOwnerId,
                    title = "Explore ${unex.competencyName}",
                    targetModule = unex.module,
                    targetCompetency = unex.competencyName,
                    category = RecommendationCategory.PRACTICE_NEW_SKILL,
                    explanation = "Unexplored skill territory. Establishing foundational mechanics expands your social repertoire across modules.",
                    supportingEvidenceReferences = listOf("Unexplored developmental status in ${unex.module.displayName}"),
                    recommendedActivityId = exerciseId,
                    recommendedActivityType = "EXERCISE",
                    estimatedDurationMinutes = 5,
                    difficulty = AdaptiveDifficulty.NORMAL,
                    expectedLearningOutcome = "Complete opening primer exercise for ${unex.competencyName}.",
                    status = RecommendationStatus.NEW,
                    createdAtEpochMs = nowEpochMs
                )
            )
        }

        // 5. Stale Assessment Check (> 30 days old)
        val staleAssessments = competencies.filter {
            it.lastPracticedEpochMs != null &&
                    (nowEpochMs - it.lastPracticedEpochMs) >= STALE_ASSESSMENT_THRESHOLD_MS
        }
        for (stale in staleAssessments.take(1)) {
            recommendations.add(
                PersonalizedSocialRecommendation(
                    recommendationId = "REC_REASSESS_${stale.module.code}_${System.currentTimeMillis()}_${recommendations.size}",
                    accountOwnerId = accountOwnerId,
                    title = "Reassess ${stale.module.displayName}",
                    targetModule = stale.module,
                    targetCompetency = stale.competencyName,
                    category = RecommendationCategory.REASSESS_SKILL,
                    explanation = "Performance data is over 30 days old. Reassessment verifies current baseline accuracy.",
                    supportingEvidenceReferences = listOf("Stale evaluation metrics in ${stale.module.displayName}"),
                    recommendedActivityId = "ASSESS_${stale.module.code}",
                    recommendedActivityType = "ASSESSMENT",
                    estimatedDurationMinutes = 10,
                    difficulty = AdaptiveDifficulty.NORMAL,
                    expectedLearningOutcome = "Update observable metrics across 5 rubric dimensions.",
                    status = RecommendationStatus.NEW,
                    createdAtEpochMs = nowEpochMs
                )
            )
        }

        return recommendations
    }

    // =========================================================================
    // 4. PERSONALIZED WEEKLY TRAINING PLANNER
    // =========================================================================

    fun generateWeeklyTrainingPlan(
        accountOwnerId: String,
        targetDaysPerWeek: Int = 5,
        targetMinutesPerDay: Int = 15,
        competencies: List<SocialCompetencyProgress>,
        nowEpochMs: Long = System.currentTimeMillis()
    ): PersonalizedWeeklyTrainingPlan {
        val safeDays = targetDaysPerWeek.coerceIn(3, 7)
        val safeMinutes = targetMinutesPerDay.coerceIn(5, 60)
        val planId = "PLAN_${accountOwnerId}_${nowEpochMs / (7 * 24 * 60 * 60 * 1000)}"

        val sortedByNeed = competencies.sortedWith(
            compareBy<SocialCompetencyProgress> { it.masteryStage.ordinalValue }
                .thenBy { it.lastEvaluatedScore ?: 100 }
        )

        val scheduledActivities = mutableListOf<ScheduledTrainingActivity>()
        val dayMillis = 24L * 60L * 60L * 1000L

        for (dayIndex in 1..safeDays) {
            val scheduledDate = nowEpochMs + (dayIndex - 1) * dayMillis
            val targetComp = sortedByNeed[(dayIndex - 1) % sortedByNeed.size]

            val activityType = when (dayIndex) {
                1 -> TrainingActivityType.EXERCISE
                2 -> TrainingActivityType.REVISION_SESSION
                3 -> TrainingActivityType.REAL_WORLD_CHALLENGE
                4 -> TrainingActivityType.EXERCISE
                5 -> TrainingActivityType.MAINTENANCE
                6 -> TrainingActivityType.WEEKLY_REVIEW
                else -> TrainingActivityType.MAINTENANCE
            }

            val title = when (activityType) {
                TrainingActivityType.EXERCISE -> "Practice Drill: ${targetComp.competencyName}"
                TrainingActivityType.REAL_WORLD_CHALLENGE -> "Mission Focus: ${targetComp.module.displayName}"
                TrainingActivityType.REVISION_SESSION -> "Reflective Review: ${targetComp.competencyName}"
                TrainingActivityType.MAINTENANCE -> "Quick Primer: ${targetComp.module.displayName}"
                TrainingActivityType.WEEKLY_REVIEW -> "Weekly Synthesis & Review"
            }

            val refId = when (activityType) {
                TrainingActivityType.REAL_WORLD_CHALLENGE -> {
                    SocialMissionCatalog.getMissionsForModule(targetComp.module).firstOrNull()?.missionId ?: "MSN_COMM_E01"
                }
                else -> {
                    SocialPracticeCatalog.exercises.firstOrNull { it.module == targetComp.module }?.exerciseId ?: "EX_COMM_01"
                }
            }

            scheduledActivities.add(
                ScheduledTrainingActivity(
                    activityId = "ACT_${planId}_D${dayIndex}_${targetComp.competencyName.replace(" ", "_")}",
                    planId = planId,
                    accountOwnerId = accountOwnerId,
                    dayOfWeek = dayIndex,
                    scheduledDateEpochMs = scheduledDate,
                    module = targetComp.module,
                    competencyName = targetComp.competencyName,
                    activityType = activityType,
                    title = title,
                    referenceId = refId,
                    durationMinutes = safeMinutes,
                    difficulty = targetComp.recommendedDifficulty,
                    isCompleted = false,
                    isSkipped = false
                )
            )
        }

        return PersonalizedWeeklyTrainingPlan(
            planId = planId,
            accountOwnerId = accountOwnerId,
            weeklyDevelopmentObjective = "Build consistent execution in ${sortedByNeed.firstOrNull()?.module?.displayName ?: "Communication"} through deliberate micro-drills.",
            targetMinutesPerDay = safeMinutes,
            targetDaysPerWeek = safeDays,
            startDateEpochMs = nowEpochMs,
            endDateEpochMs = nowEpochMs + (7L * dayMillis),
            isPaused = false,
            scheduledActivities = scheduledActivities,
            createdAtEpochMs = nowEpochMs,
            updatedAtEpochMs = nowEpochMs
        )
    }

    // =========================================================================
    // 5. WEEKLY DEVELOPMENT REPORT
    // =========================================================================

    fun generateWeeklyDevelopmentReport(
        plan: PersonalizedWeeklyTrainingPlan?,
        missions: List<SocialMissionRecord>,
        sessions: List<SocialPracticeSession>,
        nowEpochMs: Long = System.currentTimeMillis()
    ): SocialWeeklyDevelopmentReport {
        val sevenDaysAgo = nowEpochMs - (7L * 24L * 60L * 60L * 1000L)

        val completedPlanned = plan?.scheduledActivities?.filter { it.isCompleted } ?: emptyList()
        val recentMissions = missions.filter {
            it.lifecycleStatus == SocialMissionLifecycleStatus.COMPLETED &&
                    (it.completedEpochMs ?: it.startedEpochMs) >= sevenDaysAgo
        }
        val recentSessions = sessions.filter {
            (it.completedEpochMs ?: it.startedEpochMs) >= sevenDaysAgo
        }

        val totalMinutes = (recentSessions.sumOf { it.durationSeconds } / 60) +
                completedPlanned.sumOf { it.durationMinutes } +
                (recentMissions.size * 15)

        val practicedCompetencies = completedPlanned.map { it.competencyName }.distinct()

        val improvements = mutableListOf<String>()
        if (completedPlanned.isNotEmpty()) {
            improvements.add("Completed ${completedPlanned.size} scheduled training sessions this week.")
        }
        if (recentMissions.isNotEmpty()) {
            improvements.add("Successfully executed ${recentMissions.size} real-world social missions.")
        }
        if (improvements.isEmpty()) {
            improvements.add("Established baseline practice log for upcoming structured cycle.")
        }

        val areasToWatch = mutableListOf<String>()
        val skippedCount = plan?.scheduledActivities?.count { it.isSkipped } ?: 0
        if (skippedCount > 0) {
            areasToWatch.add("$skippedCount sessions were skipped without penalty. Consider adjusting daily duration.")
        } else {
            areasToWatch.add("Maintain consistency: aim for at least 1 real-world mission execution next week.")
        }

        val nextPriorities = listOf(
            "Reinforce active listening in conversational simulations",
            "Transition passing simulation scores into real-world mission execution",
            "Maintain 15-minute daily practice rhythm"
        )

        return SocialWeeklyDevelopmentReport(
            weekStartDateEpochMs = sevenDaysAgo,
            weekEndDateEpochMs = nowEpochMs,
            exercisesCompletedCount = recentSessions.size + completedPlanned.size,
            missionsCompletedCount = recentMissions.size,
            totalMinutesPracticed = totalMinutes,
            competenciesPracticed = practicedCompetencies,
            observedImprovements = improvements,
            areasRequiringAttention = areasToWatch,
            nextWeekPriorities = nextPriorities
        )
    }

    // =========================================================================
    // 6. PATHWAY AGGREGATOR FOR ALL EIGHT MODULES
    // =========================================================================

    fun buildMasteryPathways(
        competencies: List<SocialCompetencyProgress>,
        recommendations: List<PersonalizedSocialRecommendation>
    ): List<SocialMasteryPathway> {
        return SocialModuleIdentifier.entries.map { module ->
            val moduleComps = competencies.filter { it.module == module }
            val topComp = moduleComps.minByOrNull { it.masteryStage.ordinalValue }
            val nextRec = recommendations.firstOrNull { it.targetModule == module }

            val moduleStage = when {
                moduleComps.all { it.masteryStage == MasteryDevelopmentStage.UNEXPLORED } -> MasteryDevelopmentStage.UNEXPLORED
                moduleComps.any { it.masteryStage == MasteryDevelopmentStage.MASTERY_CANDIDATE } -> MasteryDevelopmentStage.MASTERY_CANDIDATE
                moduleComps.any { it.masteryStage == MasteryDevelopmentStage.ADVANCED } -> MasteryDevelopmentStage.ADVANCED
                moduleComps.any { it.masteryStage == MasteryDevelopmentStage.COMPETENT } -> MasteryDevelopmentStage.COMPETENT
                moduleComps.any { it.masteryStage == MasteryDevelopmentStage.DEVELOPING } -> MasteryDevelopmentStage.DEVELOPING
                else -> MasteryDevelopmentStage.BEGINNER
            }

            SocialMasteryPathway(
                module = module,
                competencies = moduleComps,
                currentDevelopmentStage = moduleStage,
                recommendedNextActivityTitle = nextRec?.title ?: "Foundational Rehearsal in ${module.displayName}",
                recommendedNextActivityId = nextRec?.recommendedActivityId ?: "EX_${module.code}_01",
                lastPracticedEpochMs = moduleComps.mapNotNull { it.lastPracticedEpochMs }.maxOrNull(),
                primaryLearningObjective = "Systematic progression through ${moduleComps.size} observable competencies."
            )
        }
    }
}
