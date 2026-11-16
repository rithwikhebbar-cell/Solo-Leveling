package com.example.domain.model

/**
 * Milestone 12 - Phase 3: AI Coaching, Voice Interaction, Advanced Simulations,
 * Analytics & Weakness Detection Models.
 */

enum class AiCoachingMode(val label: String, val description: String) {
    PERSONAL_COACH("Personal Coach", "Ask direct questions, receive custom communication strategy and habit tips"),
    PRACTICE_COACH("Practice Coach", "Detailed structural feedback, conciseness coaching, and sentence improvements"),
    ROLEPLAY_COACH("Roleplay Coach", "Dynamic AI conversational partner acting in custom scenarios"),
    PROFESSIONAL_COACH("Professional Coach", "Executive presence, workplace politics navigation, and presentation prep")
}

data class AiCoachingMessage(
    val messageId: String,
    val role: String, // "USER", "COACH", "SYSTEM"
    val content: String,
    val suggestedFollowUpQuestions: List<String> = emptyList(),
    val timestampEpochMs: Long = System.currentTimeMillis()
)

enum class AdvancedScenarioCategory(val label: String) {
    COLLEGE_ACADEMIC("College & Academic Life"),
    PROFESSIONAL_NETWORKING("Professional Networking"),
    PROFESSIONAL_COMMUNICATION("Professional Communication"),
    INTERPERSONAL_RELATIONSHIPS("Personal & Everyday Communication"),
    PUBLIC_SPEAKING_ARENA("Public Speaking Arena"),
    NEGOTIATION_SIMULATOR("Negotiation Simulator"),
    INTERVIEW_PREPARATION("Interview Preparation"),
    LEADERSHIP_DECISIONS("Leadership Decision Simulator"),
    ADVANCED_LANGUAGE("Advanced Language Coach")
}

data class AdvancedScenario(
    val scenarioId: String,
    val category: AdvancedScenarioCategory,
    val module: SocialModuleIdentifier,
    val title: String,
    val description: String,
    val contextPrompt: String,
    val npcName: String,
    val npcRole: String,
    val learningObjective: String,
    val difficulty: PracticeDifficulty = PracticeDifficulty.BASIC,
    val timeLimitSeconds: Int = 180,
    val dialogueScript: List<ConversationDialogueTurn> = emptyList(),
    val evaluationDimensions: List<String> = listOf("Clarity", "Strategy", "Empathy", "Outcome"),
    val minPassingScore: Int = 75
)

data class InterviewQuestion(
    val questionId: String,
    val interviewType: String, // "HR", "TECHNICAL", "BEHAVIORAL", "LEADERSHIP"
    val questionText: String,
    val starFrameworkFocus: String, // e.g. "Situation & Task", "Action & Result"
    val targetTimeSeconds: Int = 120,
    val tips: List<String> = emptyList()
)

data class LeadershipDecisionScenario(
    val scenarioId: String,
    val title: String,
    val backgroundContext: String,
    val stakeholders: List<String>,
    val constraints: List<String>,
    val options: List<LeadershipOption>,
    val reflectionPrompt: String
)

data class LeadershipOption(
    val optionId: String,
    val title: String,
    val actionDescription: String,
    val tradeOffs: String,
    val expectedOutcomeScore: Int // 0-100
)

data class WeaknessPattern(
    val patternId: String,
    val title: String,
    val description: String,
    val affectedModule: SocialModuleIdentifier,
    val frequencyDetected: Int,
    val supportingEvidenceNotes: List<String>,
    val recommendedExerciseIds: List<String>,
    val recommendedDifficulty: PracticeDifficulty
)

data class AdaptiveSocialRecommendation(
    val recommendationId: String,
    val title: String,
    val reasonExplanation: String,
    val targetModule: SocialModuleIdentifier,
    val recommendedExerciseId: String,
    val priorityScore: Int, // 1 to 100
    val isAccepted: Boolean = false,
    val isDismissed: Boolean = false
)

data class SocialAnalyticsSummary(
    val overallMasteryStage: SocialMasteryStage,
    val totalPracticeMinutes: Int,
    val exercisesCompleted: Int,
    val scenariosCleared: Int,
    val assessmentsCount: Int,
    val averageScore: Float,
    val moduleScoreMap: Map<SocialModuleIdentifier, Float>,
    val modulePracticeMinutesMap: Map<SocialModuleIdentifier, Int>,
    val detectedWeaknesses: List<WeaknessPattern>,
    val activeRecommendations: List<AdaptiveSocialRecommendation>
)
