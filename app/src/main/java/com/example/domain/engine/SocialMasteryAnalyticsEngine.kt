package com.example.domain.engine

import com.example.data.repository.SocialRepository
import com.example.domain.model.AdaptiveSocialRecommendation
import com.example.domain.model.PracticeDifficulty
import com.example.domain.model.SocialAnalyticsSummary
import com.example.domain.model.SocialMasteryStage
import com.example.domain.model.SocialModuleIdentifier
import com.example.domain.model.WeaknessPattern
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

/**
 * Milestone 12 - Phase 3: Social Mastery Analytics & Weakness Detection Engine.
 * Responsibilities:
 * - Computes accurate aggregations derived strictly from persisted records.
 * - Detects recurring performance weaknesses (e.g. excessive response length, low clarity scores).
 * - Generates adaptive, evidence-based training recommendations.
 * - Non-punitive, explainable, and fully functional offline.
 */
class SocialMasteryAnalyticsEngine(
    private val socialRepository: SocialRepository
) {
    suspend fun computeAnalyticsSummary(accountOwnerId: String): SocialAnalyticsSummary {
        val profile = socialRepository.getOrCreateProfile(accountOwnerId)
        val moduleProgressList = socialRepository.observeAllModuleProgress(accountOwnerId).firstOrNull() ?: emptyList()
        val recentSessions = socialRepository.observeRecentSessions(accountOwnerId, 50).firstOrNull() ?: emptyList()

        val moduleScoreMap = mutableMapOf<SocialModuleIdentifier, Float>()
        val moduleMinutesMap = mutableMapOf<SocialModuleIdentifier, Int>()

        for (prog in moduleProgressList) {
            moduleScoreMap[prog.module] = prog.rollingAverageScore
            moduleMinutesMap[prog.module] = prog.practiceMinutes
        }

        val weaknesses = detectWeaknesses(recentSessions, moduleProgressList)
        val recommendations = generateAdaptiveRecommendations(weaknesses, moduleProgressList)

        val averageScore = if (profile.totalAssessmentsCompleted > 0) profile.averageRubricScore else 0.0f

        return SocialAnalyticsSummary(
            overallMasteryStage = profile.overallSocialMasteryStage,
            totalPracticeMinutes = profile.totalPracticeMinutes,
            exercisesCompleted = recentSessions.size,
            scenariosCleared = profile.totalScenariosCleared,
            assessmentsCount = profile.totalAssessmentsCompleted,
            averageScore = averageScore,
            moduleScoreMap = moduleScoreMap,
            modulePracticeMinutesMap = moduleMinutesMap,
            detectedWeaknesses = weaknesses,
            activeRecommendations = recommendations
        )
    }

    private fun detectWeaknesses(
        sessions: List<com.example.domain.model.SocialPracticeSession>,
        progressList: List<com.example.domain.model.SocialModuleProgress>
    ): List<WeaknessPattern> {
        val list = mutableListOf<WeaknessPattern>()

        // 1. Check for modules with practice stagnation or low average score (requires at least 2 assessments)
        for (prog in progressList) {
            if (prog.assessmentsCompleted >= 2 && prog.rollingAverageScore < 70.0f) {
                list.add(
                    WeaknessPattern(
                        patternId = "WEAK_LOW_AVG_${prog.module.code}",
                        title = "Low Scoring Consistency in ${prog.module.displayName}",
                        description = "Average rubric score (${prog.rollingAverageScore.toInt()}%) is below the 70% proficiency target.",
                        affectedModule = prog.module,
                        frequencyDetected = prog.assessmentsCompleted,
                        supportingEvidenceNotes = listOf("Recorded across ${prog.assessmentsCompleted} assessments."),
                        recommendedExerciseIds = listOf("EX_COMM_01", "EX_PUB_01"),
                        recommendedDifficulty = PracticeDifficulty.BASIC
                    )
                )
            }
        }

        // 2. Check for brief response attempts (Heuristic signal; requires at least 2 brief sessions)
        val briefSessions = sessions.filter {
            val text = it.transcriptText?.trim().orEmpty()
            text.isNotEmpty() && text.length <= 40
        }
        if (briefSessions.size >= 2) {
            list.add(
                WeaknessPattern(
                    patternId = "WEAK_UNDERDEVELOPED_RESPONSES",
                    title = "Underdeveloped Practice Responses",
                    description = "Heuristic observation: Multiple responses were shorter than 40 characters. Consider developing arguments with the PREP framework (Point, Reason, Example, Point).",
                    affectedModule = SocialModuleIdentifier.COMMUNICATION_MASTERY,
                    frequencyDetected = briefSessions.size,
                    supportingEvidenceNotes = listOf("Heuristic signal detected in ${briefSessions.size} recent practice sessions."),
                    recommendedExerciseIds = listOf("EX_COMM_01"), // PREP framework drill
                    recommendedDifficulty = PracticeDifficulty.FOUNDATION
                )
            )
        }

        // 3. Stale modules (no practice in > 30 days with prior assessment history)
        val staleProg = progressList.filter { it.isStale && it.assessmentsCompleted > 0 }
        for (stale in staleProg) {
            list.add(
                WeaknessPattern(
                    patternId = "WEAK_STALE_${stale.module.code}",
                    title = "Maintenance Recommended in ${stale.module.displayName}",
                    description = "Extended inactivity detected without demoting your earned stage. Complete two quick refresher drills to maintain sharp execution.",
                    affectedModule = stale.module,
                    frequencyDetected = 1,
                    supportingEvidenceNotes = listOf("Module flagged for maintenance refresher due to extended inactivity."),
                    recommendedExerciseIds = listOf("EX_COMM_02"),
                    recommendedDifficulty = PracticeDifficulty.BASIC
                )
            )
        }

        return list
    }

    private fun generateAdaptiveRecommendations(
        weaknesses: List<WeaknessPattern>,
        progressList: List<com.example.domain.model.SocialModuleProgress>
    ): List<AdaptiveSocialRecommendation> {
        val recommendations = mutableListOf<AdaptiveSocialRecommendation>()

        // Recommendation 1: Prioritize detected weakness
        weaknesses.firstOrNull()?.let { weak ->
            recommendations.add(
                AdaptiveSocialRecommendation(
                    recommendationId = "REC_WEAK_${weak.patternId}",
                    title = "Targeted Correction: ${weak.title}",
                    reasonExplanation = weak.description,
                    targetModule = weak.affectedModule,
                    recommendedExerciseId = weak.recommendedExerciseIds.firstOrNull() ?: "EX_COMM_01",
                    priorityScore = 95
                )
            )
        }

        // Recommendation 2: Recommend unstarted modules
        val unstarted = progressList.filter { it.practiceSeconds == 0L && it.assessmentsCompleted == 0 }
        unstarted.firstOrNull()?.let { mod ->
            recommendations.add(
                AdaptiveSocialRecommendation(
                    recommendationId = "REC_UNSTARTED_${mod.module.code}",
                    title = "Initialize ${mod.module.displayName}",
                    reasonExplanation = "This core social module has zero recorded practice. Complete a 5-minute foundational drill.",
                    targetModule = mod.module,
                    recommendedExerciseId = "EX_PUB_01",
                    priorityScore = 80
                )
            )
        }

        // Recommendation 3: Default active listening reinforcement
        if (recommendations.isEmpty()) {
            recommendations.add(
                AdaptiveSocialRecommendation(
                    recommendationId = "REC_DAILY_MAINTENANCE",
                    title = "Active Listening Maintenance",
                    reasonExplanation = "Keep your conversational responsiveness sharp with a quick 5-minute reflective paraphrasing exercise.",
                    targetModule = SocialModuleIdentifier.COMMUNICATION_MASTERY,
                    recommendedExerciseId = "EX_COMM_02",
                    priorityScore = 70
                )
            )
        }

        return recommendations
    }
}
