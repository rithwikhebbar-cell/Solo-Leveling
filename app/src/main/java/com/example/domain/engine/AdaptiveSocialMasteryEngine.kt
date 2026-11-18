package com.example.domain.engine

import com.example.domain.model.AdaptiveDifficulty
import com.example.domain.model.MasteryDevelopmentStage
import com.example.domain.model.MasteryEvidenceClassification
import com.example.domain.model.PersonalizedSocialRecommendation
import com.example.domain.model.PersonalizedWeeklyTrainingPlan
import com.example.domain.model.SocialRecommendationCategory
import com.example.domain.model.SocialRecommendationStatus
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

    private const val DAY_MS = 24L * 60L * 60L * 1000L
    private const val MAINTAIN_STRENGTH_INTERVAL_MS = 7L * DAY_MS // 7 days
    private const val INACTIVITY_THRESHOLD_MS = 14L * DAY_MS // 14 days
    private const val STALE_ASSESSMENT_THRESHOLD_MS = 30L * DAY_MS // 30 days
    private const val MAX_ACTIVE_RECOMMENDATIONS = 5

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
        priorProgress: Map<String, SocialCompetencyProgress> = emptyMap(),
        nowEpochMs: Long = System.currentTimeMillis(),
        artifactVerifier: (SocialMissionRecord) -> Boolean = { defaultArtifactMetadataVerifier(it) }
    ): List<SocialCompetencyProgress> {
        val result = mutableListOf<SocialCompetencyProgress>()

        for (module in SocialModuleIdentifier.entries) {
            val moduleCompetencies = SocialCompetencyCatalog.getCompetenciesForModule(module)
            val moduleSessions = sessions.filter { it.module == module && it.sessionStatus == com.example.domain.model.SocialSessionStatus.COMPLETED }
            val moduleAttempts = attempts.filter { it.module == module }
            val moduleMissions = missions.filter {
                it.module == module && it.lifecycleStatus == SocialMissionLifecycleStatus.COMPLETED
            }
            val moduleAssessments = assessments.filter { it.module == module }

            // Assign transcript-matched sessions directly; distribute untagged sessions round-robin
            val explicitlyMatchedSessionIds = mutableMapOf<String, MutableList<SocialPracticeSession>>()
            val generalSessions = mutableListOf<SocialPracticeSession>()
            for (sess in moduleSessions) {
                val matchedComp = moduleCompetencies.firstOrNull { comp ->
                    sess.transcriptText?.contains(comp.name, ignoreCase = true) == true
                }
                if (matchedComp != null) {
                    explicitlyMatchedSessionIds.getOrPut(matchedComp.competencyId) { mutableListOf() }.add(sess)
                } else {
                    generalSessions.add(sess)
                }
            }

            for ((index, comp) in moduleCompetencies.withIndex()) {
                val compKey = "${accountOwnerId}_${comp.module.code}_${comp.competencyId}"

                val distributedGeneralSessions = generalSessions.filterIndexed { i, _ ->
                    i % moduleCompetencies.size.coerceAtLeast(1) == index
                }
                val relevantSessions = (explicitlyMatchedSessionIds[comp.competencyId].orEmpty() + distributedGeneralSessions)

                // Separate evaluated simulation/practice attempts from subjective self-assessments
                val relevantAttempts = moduleAttempts
                    .filter {
                        it.scenarioTitle.contains(comp.name, ignoreCase = true) ||
                            it.userResponseText.contains(comp.name, ignoreCase = true)
                    }
                    .sortedByDescending { it.completedEpochMs }

                val relevantMissions = moduleMissions.filter {
                    it.title.contains(comp.name, ignoreCase = true) ||
                        it.reflectionText?.contains(comp.name, ignoreCase = true) == true ||
                        it.artifactNote?.contains(comp.name, ignoreCase = true) == true
                }

                // Meaningful real-world application evidence requires an attached, stored, account-owned,
                // retrievable artifact (never note-only ARTIFACT_DESCRIBED or simulation attempts),
                // and distinct artifacts are required for MASTERY_CANDIDATE.
                val meaningfulRealWorldMissions = relevantMissions
                    .filter { missionRecord ->
                        isMeaningfulRealWorldEvidence(missionRecord, artifactVerifier)
                    }
                    .distinctBy { it.artifactContentHash }

                val practiceCount = relevantSessions.size + relevantAttempts.size + relevantMissions.size

                // Strictly only evaluated rubric scores (simulations/scenarios); NEVER subjective self-assessment sliders.
                val recentScore = relevantAttempts.firstOrNull()?.evaluationScore

                val lastSessionTime = relevantSessions.maxOfOrNull { it.completedEpochMs ?: it.startedEpochMs }
                val lastAttemptTime = relevantAttempts.maxOfOrNull { it.completedEpochMs }
                val lastMissionTime = relevantMissions.maxOfOrNull { it.completedEpochMs ?: it.startedEpochMs }
                val lastPracticed = listOfNotNull(lastSessionTime, lastAttemptTime, lastMissionTime).maxOrNull()

                // Honest Evidence Classification (never promotes self-assessment or note-only artifacts)
                val evidenceClassification = when {
                    meaningfulRealWorldMissions.isNotEmpty() -> MasteryEvidenceClassification.ARTIFACT_SUPPORTED
                    relevantAttempts.isNotEmpty() -> MasteryEvidenceClassification.SIMULATION_EVALUATED
                    relevantSessions.isNotEmpty() -> MasteryEvidenceClassification.PRACTICE_OBSERVED
                    relevantMissions.isNotEmpty() -> MasteryEvidenceClassification.SELF_REPORTED
                    moduleAssessments.isNotEmpty() -> MasteryEvidenceClassification.SELF_REPORTED
                    else -> MasteryEvidenceClassification.SELF_REPORTED
                }

                // Strict Evidence-Aware Progression Hierarchy:
                // - Evaluated top-down from highest qualifying stage down to UNEXPLORED.
                // - Simulation attempts are strictly capped at COMPETENT.
                // - ADVANCED requires >= 1 verified retrievable real-world artifact mission.
                // - MASTERY_CANDIDATE requires >= 3 distinct verified retrievable artifact missions,
                //   >= 2 attempts >= 85%, recent >= 85%, and practiceCount >= 10.
                // - Prior earned stages are monotonically preserved (legacy MAINTENANCE never acts as a floor).
                val prior = priorProgress[compKey]
                val priorStage = prior?.masteryStage ?: MasteryDevelopmentStage.UNEXPLORED
                val qualifyingHighSimScoresCount = relevantAttempts.count { it.evaluationScore >= 85 }

                val computedStage = when {
                    practiceCount >= 10 &&
                        qualifyingHighSimScoresCount >= 2 &&
                        recentScore != null && recentScore >= 85 &&
                        meaningfulRealWorldMissions.size >= 3 ->
                        MasteryDevelopmentStage.MASTERY_CANDIDATE

                    practiceCount >= 5 &&
                        recentScore != null && recentScore >= 80 &&
                        meaningfulRealWorldMissions.isNotEmpty() ->
                        MasteryDevelopmentStage.ADVANCED

                    practiceCount >= 5 &&
                        recentScore != null && recentScore >= 75 ->
                        MasteryDevelopmentStage.COMPETENT

                    practiceCount >= 3 ||
                        (practiceCount >= 1 && relevantAttempts.isNotEmpty()) ->
                        MasteryDevelopmentStage.DEVELOPING

                    practiceCount in 1..2 ->
                        MasteryDevelopmentStage.BEGINNER

                    else ->
                        MasteryDevelopmentStage.UNEXPLORED
                }

                val preservedStage = if (priorStage.ordinalValue > computedStage.ordinalValue) {
                    priorStage
                } else {
                    computedStage
                }

                // Adaptive Difficulty determination from chronologically sorted (newest-first) simulation scores
                val autoDiff = computeAdaptiveDifficulty(relevantAttempts.map { it.evaluationScore })
                val finalDiff = overrides[compKey] ?: autoDiff.first

                val baseProgress = SocialCompetencyProgress(
                    competencyId = compKey,
                    accountOwnerId = accountOwnerId,
                    module = comp.module,
                    competencyName = comp.name,
                    masteryStage = preservedStage,
                    recommendedDifficulty = finalDiff,
                    practiceCount = practiceCount,
                    lastPracticedEpochMs = lastPracticed,
                    lastEvaluatedScore = recentScore,
                    evidenceClassification = evidenceClassification,
                    updatedAtEpochMs = nowEpochMs,
                    maintenanceRequired = prior?.maintenanceRequired == true,
                    maintenanceReason = prior?.maintenanceReason
                )

                result.add(
                    enrichWithMaintenanceState(
                        progress = baseProgress,
                        nowEpochMs = nowEpochMs,
                        priorMaintenanceUpdatedAtEpochMs = prior?.updatedAtEpochMs
                    )
                )
            }
        }

        return result
    }

    fun defaultArtifactMetadataVerifier(mission: SocialMissionRecord): Boolean {
        if (mission.lifecycleStatus != SocialMissionLifecycleStatus.COMPLETED) return false
        if (mission.evidenceType != com.example.domain.model.SocialMissionEvidenceType.ARTIFACT_ATTACHED) return false
        if (!mission.artifactValidationState.qualifiesForRealWorldMastery) return false
        if (mission.artifactUri.isNullOrBlank()) return false
        if (mission.artifactContentHash.isNullOrBlank()) return false
        if (mission.artifactOwnerId.isNullOrBlank() || mission.artifactOwnerId != mission.accountOwnerId) return false
        return true
    }

    fun isMeaningfulRealWorldEvidence(
        mission: SocialMissionRecord,
        artifactVerifier: (SocialMissionRecord) -> Boolean = { defaultArtifactMetadataVerifier(it) }
    ): Boolean {
        if (!defaultArtifactMetadataVerifier(mission)) return false
        return artifactVerifier(mission)
    }

    /**
     * Computes independent maintenance advisory status without ever altering or demoting earned masteryStage.
     * Preserves prior maintenance flags when no newer practice has occurred since the flag was recorded.
     */
    fun enrichWithMaintenanceState(
        progress: SocialCompetencyProgress,
        nowEpochMs: Long = System.currentTimeMillis(),
        priorMaintenanceUpdatedAtEpochMs: Long? = null
    ): SocialCompetencyProgress {
        val lastPracticed = progress.lastPracticedEpochMs
        val preserveLegacyMaintenance = progress.maintenanceRequired &&
            (lastPracticed == null || priorMaintenanceUpdatedAtEpochMs == null || lastPracticed <= priorMaintenanceUpdatedAtEpochMs)

        if (lastPracticed == null || progress.practiceCount <= 0 || progress.masteryStage == MasteryDevelopmentStage.UNEXPLORED) {
            return progress.copy(
                maintenanceRequired = preserveLegacyMaintenance,
                maintenanceReason = if (preserveLegacyMaintenance) progress.maintenanceReason else null,
                daysSinceLastPractice = lastPracticed?.let { ((nowEpochMs - it).coerceAtLeast(0L) / DAY_MS).toInt() }
            )
        }

        val elapsedMs = (nowEpochMs - lastPracticed).coerceAtLeast(0L)
        val daysSince = (elapsedMs / DAY_MS).toInt()
        val needsMaintenance = elapsedMs >= INACTIVITY_THRESHOLD_MS || preserveLegacyMaintenance

        val reason = when {
            elapsedMs >= INACTIVITY_THRESHOLD_MS -> when (progress.masteryStage) {
                MasteryDevelopmentStage.ADVANCED,
                MasteryDevelopmentStage.MASTERY_CANDIDATE ->
                    "$daysSince days since last practice. Periodic refresher keeps ${progress.masteryStage.label} sharpness without affecting earned stage."
                else ->
                    "$daysSince days since last practice. Complete a short refresher to rebuild momentum without losing earned progress."
            }
            preserveLegacyMaintenance -> progress.maintenanceReason
            else -> null
        }

        return progress.copy(
            maintenanceRequired = needsMaintenance,
            maintenanceReason = reason,
            daysSinceLastPractice = daysSince
        )
    }

    // =========================================================================
    // 2. ADAPTIVE DIFFICULTY ENGINE
    // =========================================================================

    /**
     * Determines difficulty adjustment deterministically based on historical scores (newest-first).
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

    private data class ScoredCandidateRecommendation(
        val priorityRank: Int,
        val sortTieBreaker: Int,
        val recommendation: PersonalizedSocialRecommendation
    )

    /**
     * Generates deterministic, prioritized, deduplicated recommendations covering all 7 categories
     * with at most one active recommendation per competency.
     *
     * Priority hierarchy per competency:
     * 1. IMPROVE_WEAKNESS (score < 70%)
     * 2. REASSESS_SKILL (>= 30 days stale)
     * 3. REVISIT_INACTIVE_SKILL (14..29 days inactive)
     * 4. MAINTAIN_STRENGTH (ADVANCED / MASTERY_CANDIDATE stage or high score >= 85% overdue 7+ days)
     * 5. REAL_WORLD_APPLICATION (COMPETENT with score >= 80% lacking artifact-supported real-world evidence)
     * 6. ADVANCE_MASTERY_LEVEL (BEGINNER / DEVELOPING / COMPETENT / ADVANCED ready to advance)
     * 7. PRACTICE_NEW_SKILL (UNEXPLORED)
     */
    fun generateRecommendations(
        accountOwnerId: String,
        competencies: List<SocialCompetencyProgress>,
        missions: List<SocialMissionRecord>,
        nowEpochMs: Long = System.currentTimeMillis()
    ): List<PersonalizedSocialRecommendation> {
        val candidates = mutableListOf<ScoredCandidateRecommendation>()

        for (comp in competencies) {
            val compSanitized = comp.competencyName.uppercase().replace(Regex("[^A-Z0-9]+"), "_").trim('_')
            val elapsedMs = comp.lastPracticedEpochMs?.let { (nowEpochMs - it).coerceAtLeast(0L) }
            val daysAgo = elapsedMs?.let { (it / DAY_MS).toInt() }

            val matchingMeaningfulMissions = missions.filter { m ->
                m.module == comp.module &&
                    isMeaningfulRealWorldEvidence(m) &&
                    (m.title.contains(comp.competencyName, ignoreCase = true) ||
                        m.reflectionText?.contains(comp.competencyName, ignoreCase = true) == true ||
                        m.artifactNote?.contains(comp.competencyName, ignoreCase = true) == true)
            }

            val defaultExercise = SocialPracticeCatalog.exercises.firstOrNull { it.module == comp.module }
            val defaultExerciseId = defaultExercise?.exerciseId ?: "EX_COMM_01"
            val defaultMission = SocialMissionCatalog.getMissionsForModule(comp.module).firstOrNull()

            val selected: Pair<Int, PersonalizedSocialRecommendation>? = when {
                // Priority 1: Improve Weakness (< 70% evaluated score)
                comp.lastEvaluatedScore != null && comp.lastEvaluatedScore < 70 -> {
                    val recId = "REC_${accountOwnerId}_${comp.module.code}_${compSanitized}_${SocialRecommendationCategory.IMPROVE_WEAKNESS.name}"
                    1 to PersonalizedSocialRecommendation(
                        recommendationId = recId,
                        accountOwnerId = accountOwnerId,
                        title = "Reinforce ${comp.competencyName}",
                        targetModule = comp.module,
                        targetCompetency = comp.competencyName,
                        category = SocialRecommendationCategory.IMPROVE_WEAKNESS,
                        explanation = "Observed rubric score was ${comp.lastEvaluatedScore}%. Targeted practice addresses friction before advancing difficulty.",
                        supportingEvidenceReferences = listOf(
                            "Evaluated rubric score: ${comp.lastEvaluatedScore}% in ${comp.module.displayName}",
                            "Stage preserved at ${comp.masteryStage.label} (${comp.evidenceClassification.label})"
                        ),
                        recommendedActivityId = defaultExerciseId,
                        recommendedActivityType = "EXERCISE",
                        estimatedDurationMinutes = 10,
                        difficulty = AdaptiveDifficulty.EASY,
                        expectedLearningOutcome = "Raise rubric precision above 75% in ${comp.competencyName}.",
                        status = SocialRecommendationStatus.NEW,
                        createdAtEpochMs = nowEpochMs,
                        updatedAtEpochMs = nowEpochMs
                    )
                }

                // Priority 2: Reassess Skill (practice history >= 30 days stale)
                elapsedMs != null && elapsedMs >= STALE_ASSESSMENT_THRESHOLD_MS -> {
                    val recId = "REC_${accountOwnerId}_${comp.module.code}_${compSanitized}_${SocialRecommendationCategory.REASSESS_SKILL.name}"
                    2 to PersonalizedSocialRecommendation(
                        recommendationId = recId,
                        accountOwnerId = accountOwnerId,
                        title = "Reassess ${comp.competencyName}",
                        targetModule = comp.module,
                        targetCompetency = comp.competencyName,
                        category = SocialRecommendationCategory.REASSESS_SKILL,
                        explanation = "Last recorded activity was ${daysAgo ?: 30} days ago. Complete a simulation check-in to re-establish baseline accuracy (earned stage preserved).",
                        supportingEvidenceReferences = listOf(
                            "Last practiced ${daysAgo ?: 30} days ago in ${comp.module.displayName}",
                            "Current stage: ${comp.masteryStage.label} (unaffected by inactivity)"
                        ),
                        recommendedActivityId = "ASSESS_${comp.module.code}",
                        recommendedActivityType = "ASSESSMENT",
                        estimatedDurationMinutes = 10,
                        difficulty = comp.recommendedDifficulty,
                        expectedLearningOutcome = "Verify current communication fluency across 5 observable rubric dimensions.",
                        status = SocialRecommendationStatus.NEW,
                        createdAtEpochMs = nowEpochMs,
                        updatedAtEpochMs = nowEpochMs
                    )
                }

                // Priority 3: Revisit Inactive Skill (14..29 days without practice)
                comp.practiceCount > 0 && elapsedMs != null && elapsedMs >= INACTIVITY_THRESHOLD_MS -> {
                    val recId = "REC_${accountOwnerId}_${comp.module.code}_${compSanitized}_${SocialRecommendationCategory.REVISIT_INACTIVE_SKILL.name}"
                    3 to PersonalizedSocialRecommendation(
                        recommendationId = recId,
                        accountOwnerId = accountOwnerId,
                        title = "Refresher: ${comp.competencyName}",
                        targetModule = comp.module,
                        targetCompetency = comp.competencyName,
                        category = SocialRecommendationCategory.REVISIT_INACTIVE_SKILL,
                        explanation = "No practice recorded in ${daysAgo ?: 14} days. A 5-minute refresher maintains behavioral fluency without demoting earned stage.",
                        supportingEvidenceReferences = listOf(
                            "Last active ${daysAgo ?: 14} days ago in ${comp.module.displayName}",
                            "Maintenance advisory active; earned ${comp.masteryStage.label} preserved"
                        ),
                        recommendedActivityId = defaultExerciseId,
                        recommendedActivityType = "EXERCISE",
                        estimatedDurationMinutes = 5,
                        difficulty = AdaptiveDifficulty.EASY,
                        expectedLearningOutcome = "Reactivate structured habits in ${comp.competencyName}.",
                        status = SocialRecommendationStatus.NEW,
                        createdAtEpochMs = nowEpochMs,
                        updatedAtEpochMs = nowEpochMs
                    )
                }

                // Priority 4: Maintain Strength (ADVANCED / MASTERY_CANDIDATE or high mastery >= 85% overdue 7+ days)
                comp.masteryStage == MasteryDevelopmentStage.MASTERY_CANDIDATE ||
                    comp.masteryStage == MasteryDevelopmentStage.ADVANCED ||
                    (comp.lastEvaluatedScore != null && comp.lastEvaluatedScore >= 85 && elapsedMs != null && elapsedMs >= MAINTAIN_STRENGTH_INTERVAL_MS) -> {
                    val recId = "REC_${accountOwnerId}_${comp.module.code}_${compSanitized}_${SocialRecommendationCategory.MAINTAIN_STRENGTH.name}"
                    4 to PersonalizedSocialRecommendation(
                        recommendationId = recId,
                        accountOwnerId = accountOwnerId,
                        title = "Maintain Peak: ${comp.competencyName}",
                        targetModule = comp.module,
                        targetCompetency = comp.competencyName,
                        category = SocialRecommendationCategory.MAINTAIN_STRENGTH,
                        explanation = "Established strength in ${comp.competencyName} (${comp.masteryStage.label}). Periodic high-rigor drills sustain executive sharpness.",
                        supportingEvidenceReferences = listOf(
                            "Stage: ${comp.masteryStage.label} (${comp.evidenceClassification.label})",
                            comp.lastEvaluatedScore?.let { "Recent rubric score: $it%" } ?: "Practice sessions logged: ${comp.practiceCount}"
                        ),
                        recommendedActivityId = defaultExerciseId,
                        recommendedActivityType = "EXERCISE",
                        estimatedDurationMinutes = 5,
                        difficulty = comp.recommendedDifficulty,
                        expectedLearningOutcome = "Preserve consistency and zero-filler composure in ${comp.competencyName}.",
                        status = SocialRecommendationStatus.NEW,
                        createdAtEpochMs = nowEpochMs,
                        updatedAtEpochMs = nowEpochMs
                    )
                }

                // Priority 5: Real-World Application (COMPETENT with strong sim >= 80% lacking artifact-supported real-world evidence)
                comp.masteryStage == MasteryDevelopmentStage.COMPETENT &&
                    (comp.lastEvaluatedScore ?: 0) >= 80 &&
                    matchingMeaningfulMissions.isEmpty() &&
                    defaultMission != null -> {
                    val recId = "REC_${accountOwnerId}_${comp.module.code}_${compSanitized}_${SocialRecommendationCategory.REAL_WORLD_APPLICATION.name}"
                    5 to PersonalizedSocialRecommendation(
                        recommendationId = recId,
                        accountOwnerId = accountOwnerId,
                        title = "Apply in Reality: ${defaultMission!!.title}",
                        targetModule = comp.module,
                        targetCompetency = comp.competencyName,
                        category = SocialRecommendationCategory.REAL_WORLD_APPLICATION,
                        explanation = "Strong simulation mastery (${comp.lastEvaluatedScore}%). Document artifact-supported real-world execution to unlock ADVANCED stage.",
                        supportingEvidenceReferences = listOf(
                            "High simulation performance (${comp.lastEvaluatedScore}%) in ${comp.competencyName}",
                            "0 artifact-supported real-world missions completed for ${comp.competencyName}"
                        ),
                        recommendedActivityId = defaultMission.missionId,
                        recommendedActivityType = "MISSION",
                        estimatedDurationMinutes = defaultMission.estimatedDurationMinutes,
                        difficulty = AdaptiveDifficulty.CHALLENGING,
                        expectedLearningOutcome = "Execute ${defaultMission.title} in reality with concrete artifact summary notes.",
                        status = SocialRecommendationStatus.NEW,
                        createdAtEpochMs = nowEpochMs,
                        updatedAtEpochMs = nowEpochMs
                    )
                }

                // Priority 6: Advance Mastery Level (actively progressing beginner/developing/competent competencies)
                comp.masteryStage in setOf(
                    MasteryDevelopmentStage.BEGINNER,
                    MasteryDevelopmentStage.DEVELOPING,
                    MasteryDevelopmentStage.COMPETENT
                ) -> {
                    val nextStageLabel = when (comp.masteryStage) {
                        MasteryDevelopmentStage.BEGINNER -> MasteryDevelopmentStage.DEVELOPING.label
                        MasteryDevelopmentStage.DEVELOPING -> MasteryDevelopmentStage.COMPETENT.label
                        MasteryDevelopmentStage.COMPETENT -> MasteryDevelopmentStage.ADVANCED.label
                        else -> MasteryDevelopmentStage.MASTERY_CANDIDATE.label
                    }
                    val recId = "REC_${accountOwnerId}_${comp.module.code}_${compSanitized}_${SocialRecommendationCategory.ADVANCE_MASTERY_LEVEL.name}"
                    6 to PersonalizedSocialRecommendation(
                        recommendationId = recId,
                        accountOwnerId = accountOwnerId,
                        title = "Advance ${comp.competencyName} -> $nextStageLabel",
                        targetModule = comp.module,
                        targetCompetency = comp.competencyName,
                        category = SocialRecommendationCategory.ADVANCE_MASTERY_LEVEL,
                        explanation = "Currently at ${comp.masteryStage.label} (${comp.practiceCount} sessions logged). Completing evaluated simulations will advance you toward $nextStageLabel.",
                        supportingEvidenceReferences = listOf(
                            "Current stage: ${comp.masteryStage.label} (${comp.practiceCount} logged practices)",
                            "Evidence basis: ${comp.evidenceClassification.label}"
                        ),
                        recommendedActivityId = defaultExerciseId,
                        recommendedActivityType = "EXERCISE",
                        estimatedDurationMinutes = 10,
                        difficulty = comp.recommendedDifficulty,
                        expectedLearningOutcome = "Build verified rubric reps required to qualify for $nextStageLabel.",
                        status = SocialRecommendationStatus.NEW,
                        createdAtEpochMs = nowEpochMs,
                        updatedAtEpochMs = nowEpochMs
                    )
                }

                // Priority 7: Practice New Skill (UNEXPLORED)
                comp.masteryStage == MasteryDevelopmentStage.UNEXPLORED -> {
                    val recId = "REC_${accountOwnerId}_${comp.module.code}_${compSanitized}_${SocialRecommendationCategory.PRACTICE_NEW_SKILL.name}"
                    7 to PersonalizedSocialRecommendation(
                        recommendationId = recId,
                        accountOwnerId = accountOwnerId,
                        title = "Explore ${comp.competencyName}",
                        targetModule = comp.module,
                        targetCompetency = comp.competencyName,
                        category = SocialRecommendationCategory.PRACTICE_NEW_SKILL,
                        explanation = "Unexplored skill territory. Establishing foundational mechanics expands your social repertoire across modules.",
                        supportingEvidenceReferences = listOf(
                            "Unexplored developmental status in ${comp.module.displayName}"
                        ),
                        recommendedActivityId = defaultExerciseId,
                        recommendedActivityType = "EXERCISE",
                        estimatedDurationMinutes = 5,
                        difficulty = AdaptiveDifficulty.NORMAL,
                        expectedLearningOutcome = "Complete opening primer exercise for ${comp.competencyName}.",
                        status = SocialRecommendationStatus.NEW,
                        createdAtEpochMs = nowEpochMs,
                        updatedAtEpochMs = nowEpochMs
                    )
                }

                else -> null
            }

            if (selected != null) {
                candidates.add(
                    ScoredCandidateRecommendation(
                        priorityRank = selected.first,
                        sortTieBreaker = comp.lastEvaluatedScore ?: (comp.masteryStage.ordinalValue * 10),
                        recommendation = selected.second
                    )
                )
            }
        }

        // Deduplicate: at most 1 per (module, competencyName), pick highest priority across modules
        return candidates
            .distinctBy { it.recommendation.targetModule to it.recommendation.targetCompetency }
            .sortedWith(
                compareBy<ScoredCandidateRecommendation> { it.priorityRank }
                    .thenBy { it.sortTieBreaker }
                    .thenBy { it.recommendation.recommendationId }
            )
            .take(MAX_ACTIVE_RECOMMENDATIONS)
            .map { it.recommendation }
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

            val maintenanceCount = moduleComps.count { it.maintenanceRequired }
            val maintenanceSummary = if (maintenanceCount > 0) {
                "$maintenanceCount ${if (maintenanceCount == 1) "competency" else "competencies"} due for refresher (earned stages preserved)"
            } else {
                null
            }

            SocialMasteryPathway(
                module = module,
                competencies = moduleComps,
                currentDevelopmentStage = moduleStage,
                recommendedNextActivityTitle = nextRec?.title ?: "Foundational Rehearsal in ${module.displayName}",
                recommendedNextActivityId = nextRec?.recommendedActivityId ?: "EX_${module.code}_01",
                lastPracticedEpochMs = moduleComps.mapNotNull { it.lastPracticedEpochMs }.maxOrNull(),
                primaryLearningObjective = "Systematic progression through ${moduleComps.size} observable competencies.",
                maintenanceRequiredCount = maintenanceCount,
                maintenanceSummary = maintenanceSummary
            )
        }
    }
}
