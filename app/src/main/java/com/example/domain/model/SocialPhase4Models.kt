package com.example.domain.model

/**
 * Milestone 12 - Phase 4: Real-World Missions, Skill Assessments & Mastery Progression Models.
 */

enum class MissionDifficultyRank(val label: String, val levelRequired: Int, val xpMultiplier: Float) {
    E_RANK("E-Rank (Beginner)", 1, 1.0f),
    D_RANK("D-Rank (Basic)", 3, 1.25f),
    C_RANK("C-Rank (Intermediate)", 6, 1.5f),
    B_RANK("B-Rank (Advanced)", 10, 2.0f);

    companion object {
        fun fromString(value: String): MissionDifficultyRank {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: E_RANK
        }
    }
}

enum class SocialMissionLifecycleStatus(val label: String) {
    AVAILABLE("Available"),
    ACCEPTED("Accepted"),
    IN_PROGRESS("In Progress"),
    SUBMITTED("Submitted"),
    UNDER_REVIEW("Under Review"),
    APPROVED("Approved"),
    NEEDS_IMPROVEMENT("Needs Improvement"),
    COMPLETED("Completed"),
    POSTPONED("Postponed"),
    CANCELLED("Cancelled");

    companion object {
        fun fromString(value: String): SocialMissionLifecycleStatus {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: AVAILABLE
        }
    }
}

enum class SocialMissionEvidenceType(val label: String, val verificationTier: VerificationStatus) {
    SELF_REFLECTION("Self-Reflection", VerificationStatus.SELF_REPORTED),
    PRACTICE_EVALUATION("Practice Evidence", VerificationStatus.VERIFIED_AI_RUBRIC),
    OPTIONAL_ARTIFACT("Supporting Artifact Reference", VerificationStatus.SELF_REPORTED),
    HUMAN_CONFIRMED("Human-Confirmed Interaction", VerificationStatus.VERIFIED_SYSTEM_TIMER);

    companion object {
        fun fromString(value: String): SocialMissionEvidenceType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: SELF_REFLECTION
        }
    }
}

data class RealWorldSocialMission(
    val missionId: String,
    val module: SocialModuleIdentifier,
    val difficulty: MissionDifficultyRank,
    val title: String,
    val description: String,
    val estimatedDurationMinutes: Int,
    val learningObjective: String,
    val stepByStepInstructions: List<String>,
    val completionCriteria: List<String>,
    val reflectionQuestions: List<String>,
    val evidenceRequirements: List<String>,
    val xpEligibilityRules: String,
    val aiCoachingTip: String,
    val isDeclineable: Boolean = true,
    val alternativeMissionId: String? = null
)

data class SocialMissionRecord(
    val recordId: String,
    val accountOwnerId: String,
    val missionId: String,
    val module: SocialModuleIdentifier,
    val title: String,
    val difficultyRank: MissionDifficultyRank,
    val lifecycleStatus: SocialMissionLifecycleStatus,
    val startedEpochMs: Long,
    val submittedEpochMs: Long? = null,
    val completedEpochMs: Long? = null,
    val evidenceType: SocialMissionEvidenceType,
    val reflectionText: String? = null,
    val humanConfirmingRole: String? = null,
    val artifactNote: String? = null,
    val aiFeedbackNotes: String? = null,
    val evaluationRubricScore: Int? = null,
    val xpAwarded: Int = 0,
    val verificationStatus: VerificationStatus,
    val idempotencyKey: String,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

data class SkillAssessmentRubric(
    val module: SocialModuleIdentifier,
    val title: String,
    val description: String,
    val dimension1Name: String,
    val dimension1Desc: String,
    val dimension2Name: String,
    val dimension2Desc: String,
    val dimension3Name: String,
    val dimension3Desc: String,
    val dimension4Name: String,
    val dimension4Desc: String,
    val dimension5Name: String,
    val dimension5Desc: String
)

data class SkillAssessmentSubmission(
    val assessmentId: String,
    val accountOwnerId: String,
    val module: SocialModuleIdentifier,
    val scoreDim1: Int,
    val scoreDim2: Int,
    val scoreDim3: Int,
    val scoreDim4: Int,
    val scoreDim5: Int,
    val qualitativeNotes: String,
    val isSelfAssessment: Boolean = true,
    val assessedEpochMs: Long = System.currentTimeMillis()
)

data class SkillAssessmentEvaluationResult(
    val assessmentId: String,
    val module: SocialModuleIdentifier,
    val overallScore: Int,
    val dimensionScores: Map<String, Int>,
    val masteryStage: SocialMasteryStage,
    val qualitativeSummary: String,
    val actionableRecommendations: List<String>,
    val xpAwarded: Int,
    val verificationStatus: VerificationStatus
)
