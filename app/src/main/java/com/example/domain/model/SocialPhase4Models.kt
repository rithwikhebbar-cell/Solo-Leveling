package com.example.domain.model

/**
 * Milestone 12 - Phase 4.1: Real-World Missions, Evidence Verification & Skill Assessment Models.
 * Audited and hardened to enforce:
 * - Deterministic, non-bypassable mission lifecycle state transitions.
 * - Distinct evidence classifications separating subjective self-reports from independently confirmed evidence.
 * - Stable, replay-resistant idempotency keys.
 * - Separation of player self-assessment sliders from demonstrated competence.
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
    RETRY("Retry"),
    COMPLETED("Completed"),
    POSTPONED("Postponed"),
    CANCELLED("Cancelled");

    fun canTransitionTo(next: SocialMissionLifecycleStatus): Boolean {
        return when (this) {
            AVAILABLE -> next in listOf(ACCEPTED, CANCELLED)
            ACCEPTED -> next in listOf(IN_PROGRESS, POSTPONED, CANCELLED)
            IN_PROGRESS -> next in listOf(SUBMITTED, POSTPONED, CANCELLED)
            POSTPONED -> next in listOf(IN_PROGRESS, CANCELLED)
            SUBMITTED -> next in listOf(UNDER_REVIEW, NEEDS_IMPROVEMENT, COMPLETED)
            UNDER_REVIEW -> next in listOf(APPROVED, NEEDS_IMPROVEMENT, COMPLETED)
            APPROVED -> next in listOf(COMPLETED)
            NEEDS_IMPROVEMENT -> next in listOf(RETRY, CANCELLED)
            RETRY -> next in listOf(IN_PROGRESS, SUBMITTED, CANCELLED)
            COMPLETED -> false // Terminal: cannot be re-completed
            CANCELLED -> next == AVAILABLE // May be restarted from scratch
        }
    }

    companion object {
        fun fromString(value: String): SocialMissionLifecycleStatus {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: AVAILABLE
        }
    }
}

enum class SocialMissionEvidenceType(
    val label: String,
    val verificationTier: VerificationStatus,
    val isIndependentlyVerified: Boolean,
    val explanation: String
) {
    SELF_REPORTED(
        label = "Self-Reported Activity",
        verificationTier = VerificationStatus.SELF_REPORTED,
        isIndependentlyVerified = false,
        explanation = "Player reports completing the real-world activity through subjective reflection."
    ),
    SIMULATION_EVALUATED(
        label = "Simulation-Evaluated Practice",
        verificationTier = VerificationStatus.VERIFIED_AI_RUBRIC,
        isIndependentlyVerified = false,
        explanation = "In-app practice simulation evaluated with deterministic text/speech rubrics; does not prove real-world execution."
    ),
    ARTIFACT_ATTACHED(
        label = "Artifact-Referenced Material",
        verificationTier = VerificationStatus.SELF_REPORTED,
        isIndependentlyVerified = false,
        explanation = "Player provided title or summary of supporting artifact; does not verify interpersonal interaction occurred."
    ),
    HUMAN_CONFIRMATION_REPORTED(
        label = "Human-Confirmation Reported",
        verificationTier = VerificationStatus.SELF_REPORTED,
        isIndependentlyVerified = false,
        explanation = "Player reports receiving verbal or written confirmation from a peer, mentor, or colleague; recorded as self-reported claim without independent signature."
    ),
    HUMAN_CONFIRMED(
        label = "Independently Human-Confirmed (Unsupported)",
        verificationTier = VerificationStatus.SELF_REPORTED,
        isIndependentlyVerified = false,
        explanation = "Direct cryptographic or authorized third-party human confirmation is unsupported in current standalone local vault architecture."
    );

    companion object {
        fun fromString(value: String): SocialMissionEvidenceType {
            return when (value.uppercase()) {
                "SELF_REFLECTION", "SELF_REPORTED" -> SELF_REPORTED
                "PRACTICE_EVALUATION", "SIMULATION_EVALUATED" -> SIMULATION_EVALUATED
                "OPTIONAL_ARTIFACT", "ARTIFACT_ATTACHED" -> ARTIFACT_ATTACHED
                "HUMAN_CONFIRMATION_REPORTED" -> HUMAN_CONFIRMATION_REPORTED
                "HUMAN_CONFIRMED" -> HUMAN_CONFIRMED
                else -> entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: SELF_REPORTED
            }
        }
    }
}

enum class SkillAssessmentClassification(
    val label: String,
    val scoringMethod: String,
    val evidenceBasis: String,
    val limitationsDisclaimer: String
) {
    SELF_ASSESSMENT(
        label = "Subjective Self-Assessment",
        scoringMethod = "Player self-reported dimensional sliders (0–100%)",
        evidenceBasis = "Personal perception and perceived conversational confidence",
        limitationsDisclaimer = "Subjective self-rating only. Does not constitute objective psychological measurement, diagnostic precision, or demonstrated interpersonal mastery."
    ),
    SIMULATION_EVALUATED(
        label = "Simulation Rubric Assessment",
        scoringMethod = "Deterministic algorithmic rubric evaluation against transcripts/WPM/PREP tokens",
        evidenceBasis = "In-app practice drills and conversational turn simulations",
        limitationsDisclaimer = "Measures measurable textual structure and pacing in controlled simulations. Does not capture real-world non-verbal cues or live social dynamics."
    ),
    EVIDENCE_SUPPORTED(
        label = "Evidence-Supported Real-World Assessment",
        scoringMethod = "Structured post-interaction reflection combined with completion criteria review",
        evidenceBasis = "Documented real-world mission execution and concrete situational takeaways",
        limitationsDisclaimer = "Derived from self-reported mission reflections. Validates completion effort, not certified third-party observation."
    ),
    INDEPENDENTLY_CONFIRMED(
        label = "Independently Confirmed Assessment (Unsupported)",
        scoringMethod = "Authorized third-party or accredited evaluator audit",
        evidenceBasis = "External credential or signed assessment token",
        limitationsDisclaimer = "Independent external evaluation is not supported in the offline local vault environment."
    );

    companion object {
        fun fromString(value: String): SkillAssessmentClassification {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: SELF_ASSESSMENT
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
    val classification: SkillAssessmentClassification = SkillAssessmentClassification.SELF_ASSESSMENT,
    val scoreDim1: Int,
    val scoreDim2: Int,
    val scoreDim3: Int,
    val scoreDim4: Int,
    val scoreDim5: Int,
    val qualitativeNotes: String,
    val assessedEpochMs: Long = System.currentTimeMillis()
)

data class SkillAssessmentEvaluationResult(
    val assessmentId: String,
    val module: SocialModuleIdentifier,
    val classification: SkillAssessmentClassification,
    val overallScore: Int,
    val dimensionScores: Map<String, Int>,
    val masteryStage: SocialMasteryStage,
    val qualitativeSummary: String,
    val actionableRecommendations: List<String>,
    val xpAwarded: Int,
    val verificationStatus: VerificationStatus,
    val assessmentDateEpochMs: Long,
    val scoringMethod: String,
    val evidenceBasis: String,
    val limitationsDisclaimer: String
)
