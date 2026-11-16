package com.example.domain.model

/**
 * Milestone 12: Social Mastery Domain Models & Enums.
 * Defines the 8 development modules, mastery stages, activity kinds,
 * evidence sources, and assessment/scenario specifications.
 */

enum class SocialModuleIdentifier(
    val code: String,
    val displayName: String,
    val description: String,
    val primaryAttribute: AttributeType,
    val primaryRatio: Float,
    val secondaryAttribute: AttributeType,
    val secondaryRatio: Float,
    val relatedSkillIds: List<String>
) {
    COMMUNICATION_MASTERY(
        code = "COMMUNICATION_MASTERY",
        displayName = "Communication Mastery",
        description = "Active listening, clear verbal articulation, asking effective questions, concise and constructive conversation.",
        primaryAttribute = AttributeType.CHA,
        primaryRatio = 0.65f,
        secondaryAttribute = AttributeType.WIS,
        secondaryRatio = 0.35f,
        relatedSkillIds = listOf("soc_communication")
    ),
    PUBLIC_SPEAKING(
        code = "PUBLIC_SPEAKING",
        displayName = "Public Speaking & Presentation",
        description = "Speech pacing, presentation structure, audience engagement, confidence, and eliminating unnecessary filler words.",
        primaryAttribute = AttributeType.CHA,
        primaryRatio = 0.60f,
        secondaryAttribute = AttributeType.DIS,
        secondaryRatio = 0.40f,
        relatedSkillIds = listOf("soc_confidence")
    ),
    NEGOTIATION_PERSUASION(
        code = "NEGOTIATION_PERSUASION",
        displayName = "Negotiation & Persuasion",
        description = "Principled bargaining, understanding opposing positions, objection handling, boundary establishment, and win-win solutions.",
        primaryAttribute = AttributeType.CHA,
        primaryRatio = 0.50f,
        secondaryAttribute = AttributeType.INT,
        secondaryRatio = 0.50f,
        relatedSkillIds = listOf("soc_communication", "wis_strategic_planning")
    ),
    LEADERSHIP_TEAMWORK(
        code = "LEADERSHIP_TEAMWORK",
        displayName = "Leadership & Teamwork",
        description = "Team coordination, task delegation, radical candor feedback, conflict resolution, mentoring, and collaborative synergy.",
        primaryAttribute = AttributeType.CHA,
        primaryRatio = 0.60f,
        secondaryAttribute = AttributeType.DIS,
        secondaryRatio = 0.40f,
        relatedSkillIds = listOf("soc_leadership", "soc_collaboration")
    ),
    NETWORKING_RELATIONSHIPS(
        code = "NETWORKING_RELATIONSHIPS",
        displayName = "Networking & Relationships",
        description = "Professional outreach, genuine rapport building, maintaining meaningful connections, mentorship, and follow-up discipline.",
        primaryAttribute = AttributeType.CHA,
        primaryRatio = 0.70f,
        secondaryAttribute = AttributeType.WIS,
        secondaryRatio = 0.30f,
        relatedSkillIds = listOf("soc_communication")
    ),
    EMOTIONAL_INTELLIGENCE(
        code = "EMOTIONAL_INTELLIGENCE",
        displayName = "Emotional & Social Intelligence",
        description = "Emotional awareness, empathy, social self-regulation, cognitive reframing, active emotional listening, and conflict de-escalation.",
        primaryAttribute = AttributeType.WIS,
        primaryRatio = 0.60f,
        secondaryAttribute = AttributeType.CHA,
        secondaryRatio = 0.40f,
        relatedSkillIds = listOf("wis_self_awareness", "wis_reflection")
    ),
    PROFESSIONAL_EXCELLENCE(
        code = "PROFESSIONAL_EXCELLENCE",
        displayName = "Professional Excellence",
        description = "Workplace etiquette, executive presence, meeting facilitation, boundary defense, and stakeholder communication.",
        primaryAttribute = AttributeType.DIS,
        primaryRatio = 0.50f,
        secondaryAttribute = AttributeType.CHA,
        secondaryRatio = 0.50f,
        relatedSkillIds = listOf("disc_routine_mastery", "soc_confidence")
    ),
    LANGUAGE_DEVELOPMENT(
        code = "LANGUAGE_DEVELOPMENT",
        displayName = "Language Development",
        description = "Vocabulary precision, grammar, pronunciation, conversational fluency, reading/speaking drills, and linguistic confidence.",
        primaryAttribute = AttributeType.INT,
        primaryRatio = 0.50f,
        secondaryAttribute = AttributeType.CHA,
        secondaryRatio = 0.50f,
        relatedSkillIds = listOf("acad_rapid_learning", "soc_communication")
    );

    companion object {
        fun fromCode(code: String): SocialModuleIdentifier {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: COMMUNICATION_MASTERY
        }
    }
}

enum class SocialMasteryStage(val ordinalValue: Int, val label: String) {
    NOVICE(0, "Novice"),
    COMPETENT(1, "Competent"),
    PROFICIENT(2, "Proficient"),
    ADVANCED(3, "Advanced"),
    MASTER(4, "Master");

    companion object {
        fun fromOrdinal(ordinal: Int): SocialMasteryStage {
            return entries.firstOrNull { it.ordinalValue == ordinal } ?: NOVICE
        }

        fun fromString(name: String): SocialMasteryStage {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: NOVICE
        }
    }
}

enum class SocialDifficultyTier(val label: String) {
    BASIC("Basic"),
    INTERMEDIATE("Intermediate"),
    ADVANCED("Advanced"),
    ELITE("Elite")
}

enum class SocialActivityKind(val label: String) {
    PRACTICE("General Practice"),
    SPEAKING_REHEARSAL("Speaking Rehearsal"),
    LISTENING_EXERCISE("Listening Exercise"),
    REFLECTION("Self-Reflection"),
    SCENARIO("Scenario Simulation"),
    NEGOTIATION_EXERCISE("Negotiation Drill"),
    PRESENTATION("Presentation Rehearsal"),
    LANGUAGE_PRACTICE("Language Practice"),
    REAL_WORLD_ACTIVITY("Real-World Interaction")
}

enum class SocialEvidenceSource(val label: String) {
    SYSTEM_OBSERVED("In-App System Timer"),
    AI_ASSESSED("AI Rubric Evaluation"),
    SELF_REPORTED("Self-Reported Action"),
    EXTERNALLY_SUPPORTED("Externally Supported"),
    ESTIMATED("Unverified / Estimated")
}

enum class SocialSessionStatus(val label: String) {
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    INTERRUPTED("Interrupted"),
    CANCELLED("Cancelled")
}

enum class SocialAssessmentType(val label: String) {
    CLARITY_CONCISENESS("Clarity & Conciseness"),
    PACING_VOCAL_DELIVERY("Pacing & Vocal Delivery"),
    EMPATHY_ACTIVE_LISTENING("Empathy & Active Listening"),
    ASSERTIVENESS_BOUNDARIES("Assertiveness & Boundaries"),
    NEGOTIATION_TACTICS("Negotiation Tactics"),
    EXECUTIVE_PRESENCE("Executive Presence"),
    PRONUNCIATION_FLUENCY("Pronunciation & Fluency"),
    COMPREHENSIVE_RUBRIC("Comprehensive Rubric")
}

enum class SocialScenarioCategory(val label: String) {
    WORKPLACE_COLLABORATION("Workplace Collaboration"),
    PUBLIC_SPEAKING_FORUM("Public Speaking & Presentation"),
    HIGH_STAKES_NEGOTIATION("High-Stakes Negotiation"),
    LEADERSHIP_FEEDBACK("Leadership & Radical Candor"),
    NETWORKING_OUTREACH("Networking & Outreach"),
    INTERPERSONAL_TENSION("Interpersonal Tension De-escalation"),
    EXECUTIVE_BRIEFING("Executive Briefing"),
    CONVERSATIONAL_FLUENCY("Conversational Fluency")
}

data class SocialProfile(
    val accountOwnerId: String,
    val overallSocialMasteryStage: SocialMasteryStage = SocialMasteryStage.NOVICE,
    val totalPracticeMinutes: Int = 0,
    val totalScenariosCleared: Int = 0,
    val totalAssessmentsCompleted: Int = 0,
    val averageRubricScore: Float = 0.0f,
    val lastActiveEpochMs: Long = System.currentTimeMillis(),
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis(),
    val syncStatus: String = "SYNCED",
    val lastSyncedEpochMs: Long? = null
)

data class SocialModuleProgress(
    val accountOwnerId: String,
    val module: SocialModuleIdentifier,
    val masteryStage: SocialMasteryStage = SocialMasteryStage.NOVICE,
    val practiceSeconds: Long = 0L,
    val assessmentsCompleted: Int = 0,
    val rollingAverageScore: Float = 0.0f,
    val lastScoreStandardDeviation: Float = 0.0f,
    val consecutivePassingScores: Int = 0,
    val isStale: Boolean = false,
    val decayedFromStage: SocialMasteryStage? = null,
    val consecutiveRestorationPassingScores: Int = 0,
    val lastAssessedEpochMs: Long = 0L,
    val lastDecayEpochMs: Long? = null,
    val nextDecayEligibleEpochMs: Long? = null,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
) {
    val practiceMinutes: Int get() = (practiceSeconds / 60).toInt()
}

data class SocialScenario(
    val scenarioId: String,
    val module: SocialModuleIdentifier,
    val title: String,
    val description: String,
    val difficultyTier: SocialDifficultyTier,
    val contextPrompt: String,
    val npcRole: String,
    val targetLearningObjective: String,
    val minPassingScore: Int = 70,
    val isDeprecated: Boolean = false,
    val version: Int = 1
)

data class SocialPracticeSession(
    val sessionId: String,
    val accountOwnerId: String,
    val module: SocialModuleIdentifier,
    val activityKind: SocialActivityKind,
    val sessionStatus: SocialSessionStatus,
    val durationSeconds: Int,
    val startedEpochMs: Long,
    val completedEpochMs: Long? = null,
    val transcriptText: String? = null,
    val hasAudioRecording: Boolean = false,
    val audioFileName: String? = null,
    val isCloudTranscriptSyncAllowed: Boolean = false,
    val evidenceSource: SocialEvidenceSource,
    val verificationStatus: VerificationStatus,
    val idempotencyKey: String,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

data class SocialScenarioAttempt(
    val attemptId: String,
    val accountOwnerId: String,
    val scenarioId: String,
    val scenarioTitle: String,
    val module: SocialModuleIdentifier,
    val userResponseText: String,
    val evaluationScore: Int,
    val isPassed: Boolean,
    val strengthsFeedback: List<String> = emptyList(),
    val improvementAreas: List<String> = emptyList(),
    val evidenceSource: SocialEvidenceSource,
    val verificationStatus: VerificationStatus,
    val idempotencyKey: String,
    val completedEpochMs: Long = System.currentTimeMillis()
)

data class SocialAssessment(
    val assessmentId: String,
    val accountOwnerId: String,
    val module: SocialModuleIdentifier,
    val assessmentType: SocialAssessmentType,
    val overallScore: Int,
    val clarityScore: Int,
    val empathyScore: Int,
    val assertivenessScore: Int,
    val coherenceScore: Int,
    val qualitativeAnalysis: String,
    val actionableRecommendations: List<String> = emptyList(),
    val assessedEpochMs: Long = System.currentTimeMillis()
)
