package com.example.domain.model

/**
 * Milestone 12 - Phase 2: Practice Exercise, Conversation Roleplay & Speech Evaluation Models.
 */

enum class PracticeCategory(val label: String, val description: String) {
    CONVERSATION("Conversation", "Interactive dialogue, introductions, and everyday rapport building"),
    ACTIVE_LISTENING("Active Listening", "Comprehension, key detail extraction, and clarifying questions"),
    ARTICULATION("Articulation", "Vocal enunciation, sound precision, and speech clarity"),
    CONFIDENCE("Confidence", "Projection, assertiveness, and overcoming conversational friction"),
    PROFESSIONAL_COMMUNICATION("Professional Communication", "Workplace alignment, executive briefing, and boundary defense"),
    CRITICAL_QUESTIONING("Critical Questioning", "Open-ended inquiry, root-cause digging, and tactical empathy"),
    IMPROMPTU_SPEAKING("Impromptu Speaking", "Quick thinking under pressure without excessive filler words"),
    STRUCTURED_EXPRESSION("Structured Expression", "Clear frameworks (e.g. PREP: Point, Reason, Example, Point)")
}

enum class PracticeDifficulty(val level: Int, val label: String) {
    FOUNDATION(1, "Foundation"),
    BASIC(2, "Basic"),
    DEVELOPING(3, "Developing"),
    INTERMEDIATE(4, "Intermediate"),
    ADVANCED(5, "Advanced"),
    MASTERY(6, "Mastery Practice");

    companion object {
        fun fromLevel(lvl: Int): PracticeDifficulty =
            entries.firstOrNull { it.level == lvl } ?: BASIC
    }
}

enum class PracticeSessionLifecycleState {
    NOT_STARTED,
    IN_PROGRESS,
    PAUSED,
    SUBMITTED,
    EVALUATED,
    COMPLETED,
    CANCELLED
}

data class EvaluationRubricDimension(
    val dimensionName: String,
    val weight: Float, // 0.0 to 1.0 (sum should be 1.0)
    val description: String,
    val criteriaPerScoreTier: Map<Int, String> = emptyMap() // e.g. 90 -> "Exceptional", 70 -> "Sufficient"
)

data class PracticeExercise(
    val exerciseId: String,
    val module: SocialModuleIdentifier,
    val category: PracticeCategory,
    val title: String,
    val description: String,
    val instructions: String,
    val difficulty: PracticeDifficulty,
    val estimatedDurationMinutes: Int,
    val learningObjective: String,
    val promptContent: String,
    val exampleResponse: String? = null,
    val rubricDimensions: List<EvaluationRubricDimension>,
    val minPassingScore: Int = 70,
    val feedbackTips: List<String> = emptyList()
)

data class ConversationDialogueTurn(
    val turnIndex: Int,
    val speakerRole: String, // "NPC" or "PLAYER"
    val messageText: String,
    val intentTag: String? = null,
    val suggestedApproaches: List<String> = emptyList()
)

data class ConversationScenario(
    val scenarioId: String,
    val module: SocialModuleIdentifier,
    val title: String,
    val contextDescription: String,
    val npcName: String,
    val npcPersona: String,
    val openingDialogue: String,
    val objective: String,
    val dialogueScript: List<ConversationDialogueTurn> = emptyList(), // For deterministic offline roleplay
    val targetRounds: Int = 3,
    val difficulty: PracticeDifficulty = PracticeDifficulty.BASIC
)

data class ListeningExercise(
    val exerciseId: String,
    val module: SocialModuleIdentifier,
    val title: String,
    val spokenOrWrittenPassage: String,
    val questionPrompt: String,
    val keyFactsToIdentify: List<String>,
    val perspectiveSummary: String,
    val followUpQuestionCriteria: String,
    val minPassingScore: Int = 70
)

data class ArticulationExercise(
    val exerciseId: String,
    val languageCode: String = "en", // "en", "es", "ja", etc.
    val title: String,
    val targetPhonemesOrSound: String,
    val phraseToArticulate: String,
    val targetPaceWpm: Int = 130,
    val tongueTwisterLevel: PracticeDifficulty = PracticeDifficulty.BASIC
)

data class PracticeEvaluationResult(
    val overallScore: Int, // 0-100
    val dimensionScores: Map<String, Int>, // e.g. "Clarity" -> 85
    val isPassed: Boolean,
    val measurableMetrics: MeasurableSpeechMetrics,
    val strengths: List<String>,
    val improvementAreas: List<String>,
    val qualitativeSummary: String,
    val nextRecommendedExerciseId: String? = null
)

data class MeasurableSpeechMetrics(
    val durationSeconds: Int,
    val wordCount: Int,
    val wordsPerMinute: Int,
    val transcriptAvailable: Boolean,
    val audioRecordingAvailable: Boolean
)
