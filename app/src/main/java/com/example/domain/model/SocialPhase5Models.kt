package com.example.domain.model

/**
 * Milestone 12 - Phase 5: Adaptive Mastery Pathways & Personalized Development Engine Models.
 * Models for the eight personalized mastery pathways, adaptive recommendation engine,
 * personalized weekly planner, competency progression classifications, and adaptive difficulty.
 */

// =========================================================================
// 1. COMPETENCY ARCHITECTURE FOR ALL EIGHT MODULES
// =========================================================================

data class SocialCompetency(
    val competencyId: String,
    val module: SocialModuleIdentifier,
    val name: String,
    val description: String,
    val defaultDifficulty: AdaptiveDifficulty = AdaptiveDifficulty.NORMAL
)

object SocialCompetencyCatalog {
    val allCompetencies: Map<SocialModuleIdentifier, List<SocialCompetency>> = mapOf(
        // Module 1 — Communication Mastery
        SocialModuleIdentifier.COMMUNICATION_MASTERY to listOf(
            SocialCompetency("COMP_COMM_01", SocialModuleIdentifier.COMMUNICATION_MASTERY, "Active Listening", "Attentive, non-defensive reception and parsing of spoken messages."),
            SocialCompetency("COMP_COMM_02", SocialModuleIdentifier.COMMUNICATION_MASTERY, "Clear Expression", "Unambiguous articulation of thoughts without obfuscation or filler."),
            SocialCompetency("COMP_COMM_03", SocialModuleIdentifier.COMMUNICATION_MASTERY, "Structured Communication", "Logical message progression using frameworks like PREP or BLUF."),
            SocialCompetency("COMP_COMM_04", SocialModuleIdentifier.COMMUNICATION_MASTERY, "Asking Effective Questions", "Formulating precise, open-ended inquiries that surface root causes."),
            SocialCompetency("COMP_COMM_05", SocialModuleIdentifier.COMMUNICATION_MASTERY, "Giving and Receiving Feedback", "Delivering radical candor without hostility; receiving critique with composure."),
            SocialCompetency("COMP_COMM_06", SocialModuleIdentifier.COMMUNICATION_MASTERY, "Verbal Clarity", "Pacing, diaphragmatic projection, and elimination of verbal stumbling."),
            SocialCompetency("COMP_COMM_07", SocialModuleIdentifier.COMMUNICATION_MASTERY, "Reflective Paraphrasing", "Validating the speaker's core intent through concise cognitive restatement.")
        ),
        // Module 2 — Public Speaking & Presentation
        SocialModuleIdentifier.PUBLIC_SPEAKING to listOf(
            SocialCompetency("COMP_PUB_01", SocialModuleIdentifier.PUBLIC_SPEAKING, "Speech Organization", "Definite introduction, structured thesis body, and impactful conclusion."),
            SocialCompetency("COMP_PUB_02", SocialModuleIdentifier.PUBLIC_SPEAKING, "Presentation Structure", "Slide coordination, logical narrative arc, and visual hierarchy."),
            SocialCompetency("COMP_PUB_03", SocialModuleIdentifier.PUBLIC_SPEAKING, "Audience Engagement", "Eye contact distribution, dynamic rhetorical questions, and room presence."),
            SocialCompetency("COMP_PUB_04", SocialModuleIdentifier.PUBLIC_SPEAKING, "Impromptu Speaking", "Off-the-cuff coherence under pressure without nervous hesitation."),
            SocialCompetency("COMP_PUB_05", SocialModuleIdentifier.PUBLIC_SPEAKING, "Speech Delivery", "Vocal variety, purposeful pauses, dynamic cadence, and stage posture."),
            SocialCompetency("COMP_PUB_06", SocialModuleIdentifier.PUBLIC_SPEAKING, "Verbal Clarity", "Audible articulation, crisp enunciation, and elimination of filler tokens."),
            SocialCompetency("COMP_PUB_07", SocialModuleIdentifier.PUBLIC_SPEAKING, "Presentation Preparation", "Dry runs, timing discipline, and anticipating audience skepticism.")
        ),
        // Module 3 — Negotiation & Persuasion
        SocialModuleIdentifier.NEGOTIATION_PERSUASION to listOf(
            SocialCompetency("COMP_NEG_01", SocialModuleIdentifier.NEGOTIATION_PERSUASION, "Negotiation Preparation", "Researching market precedents, alternatives, and anchoring limits."),
            SocialCompetency("COMP_NEG_02", SocialModuleIdentifier.NEGOTIATION_PERSUASION, "Identifying Interests", "Separating stated positions from underlying psychological and business constraints."),
            SocialCompetency("COMP_NEG_03", SocialModuleIdentifier.NEGOTIATION_PERSUASION, "Persuasive Communication", "Framing value propositions objectively using data-backed criteria."),
            SocialCompetency("COMP_NEG_04", SocialModuleIdentifier.NEGOTIATION_PERSUASION, "Conflict Resolution", "De-escalating emotionally charged deadlocks with objective criteria."),
            SocialCompetency("COMP_NEG_05", SocialModuleIdentifier.NEGOTIATION_PERSUASION, "Handling Objections", "Diplomatic reframing of pushback without defensiveness or surrender."),
            SocialCompetency("COMP_NEG_06", SocialModuleIdentifier.NEGOTIATION_PERSUASION, "Mutual Agreement", "Synthesizing win-win tradeoffs and confirming explicit commitments."),
            SocialCompetency("COMP_NEG_07", SocialModuleIdentifier.NEGOTIATION_PERSUASION, "Negotiation Strategy", "Formulating disciplined BATNA and multi-issue concession packages.")
        ),
        // Module 4 — Leadership & Teamwork
        SocialModuleIdentifier.LEADERSHIP_TEAMWORK to listOf(
            SocialCompetency("COMP_LEAD_01", SocialModuleIdentifier.LEADERSHIP_TEAMWORK, "Delegation", "Explicit assignment of ownership, success criteria, and verification deadlines."),
            SocialCompetency("COMP_LEAD_02", SocialModuleIdentifier.LEADERSHIP_TEAMWORK, "Decision-Making", "Decisive resolution in ambiguous scenarios with transparent communication."),
            SocialCompetency("COMP_LEAD_03", SocialModuleIdentifier.LEADERSHIP_TEAMWORK, "Team Communication", "Establishing psychological safety, proactive status updates, and open dialogue."),
            SocialCompetency("COMP_LEAD_04", SocialModuleIdentifier.LEADERSHIP_TEAMWORK, "Conflict Management", "Addressing interpersonal frictions promptly before operational disruption."),
            SocialCompetency("COMP_LEAD_05", SocialModuleIdentifier.LEADERSHIP_TEAMWORK, "Accountability", "Honoring commitments, owning setbacks, and fostering team reliability."),
            SocialCompetency("COMP_LEAD_06", SocialModuleIdentifier.LEADERSHIP_TEAMWORK, "Collaborative Problem-Solving", "Synthesizing diverse perspectives into cohesive architectural solutions."),
            SocialCompetency("COMP_LEAD_07", SocialModuleIdentifier.LEADERSHIP_TEAMWORK, "Leadership Responsibility", "Protecting team capacity, shielding from noise, and empowering subordinates.")
        ),
        // Module 5 — Networking & Relationships
        SocialModuleIdentifier.NETWORKING_RELATIONSHIPS to listOf(
            SocialCompetency("COMP_NET_01", SocialModuleIdentifier.NETWORKING_RELATIONSHIPS, "Starting Conversations", "Low-friction opening introductions and confident personal presence."),
            SocialCompetency("COMP_NET_02", SocialModuleIdentifier.NETWORKING_RELATIONSHIPS, "Professional Introductions", "Polite, substantive self-positioning without transactional awkwardness."),
            SocialCompetency("COMP_NET_03", SocialModuleIdentifier.NETWORKING_RELATIONSHIPS, "Relationship Maintenance", "Periodically checking in with authentic interest without asking for favors."),
            SocialCompetency("COMP_NET_04", SocialModuleIdentifier.NETWORKING_RELATIONSHIPS, "Follow-Up Communication", "Punctual, personalized post-meeting follow-ups highlighting action items."),
            SocialCompetency("COMP_NET_05", SocialModuleIdentifier.NETWORKING_RELATIONSHIPS, "Building Meaningful Connections", "Deep listening into another person's craft, challenges, and goals."),
            SocialCompetency("COMP_NET_06", SocialModuleIdentifier.NETWORKING_RELATIONSHIPS, "Professional Networking", "Attending industry forums with clear networking objectives and graceful exits."),
            SocialCompetency("COMP_NET_07", SocialModuleIdentifier.NETWORKING_RELATIONSHIPS, "Maintaining Boundaries", "Polite boundary assertion, time respect, and conversational etiquette.")
        ),
        // Module 6 — Emotional & Social Intelligence
        SocialModuleIdentifier.EMOTIONAL_INTELLIGENCE to listOf(
            SocialCompetency("COMP_EQ_01", SocialModuleIdentifier.EMOTIONAL_INTELLIGENCE, "Perspective-Taking", "Cognitive empathy: accurately validating another's viewpoint before reacting."),
            SocialCompetency("COMP_EQ_02", SocialModuleIdentifier.EMOTIONAL_INTELLIGENCE, "Emotional Awareness", "Noticing somatic stress signals and personal emotional triggers in real time."),
            SocialCompetency("COMP_EQ_03", SocialModuleIdentifier.EMOTIONAL_INTELLIGENCE, "Empathetic Listening", "Providing emotional holding space without rushing into unsolicited fixes."),
            SocialCompetency("COMP_EQ_04", SocialModuleIdentifier.EMOTIONAL_INTELLIGENCE, "Constructive Responses", "Deliberate pause between stimulus and response to select a constructive reaction."),
            SocialCompetency("COMP_EQ_05", SocialModuleIdentifier.EMOTIONAL_INTELLIGENCE, "Managing Difficult Conversations", "Navigating high-friction conversations while maintaining mutual dignity."),
            SocialCompetency("COMP_EQ_06", SocialModuleIdentifier.EMOTIONAL_INTELLIGENCE, "Self-Reflection", "Honest post-interaction debriefing on personal bias and communicative tone."),
            SocialCompetency("COMP_EQ_07", SocialModuleIdentifier.EMOTIONAL_INTELLIGENCE, "Social Awareness", "Attunement to room dynamics, non-verbal posture shifts, and collective mood.")
        ),
        // Module 7 — Professional Excellence
        SocialModuleIdentifier.PROFESSIONAL_EXCELLENCE to listOf(
            SocialCompetency("COMP_PROF_01", SocialModuleIdentifier.PROFESSIONAL_EXCELLENCE, "Workplace Communication", "Clear, concise operational updates aligned with institutional standards."),
            SocialCompetency("COMP_PROF_02", SocialModuleIdentifier.PROFESSIONAL_EXCELLENCE, "Meeting Participation", "Preparation, punctuality, vocal contribution, and adherence to meeting agendas."),
            SocialCompetency("COMP_PROF_03", SocialModuleIdentifier.PROFESSIONAL_EXCELLENCE, "Professional Etiquette", "Respectful discourse, professional hygiene, and institutional diplomacy."),
            SocialCompetency("COMP_PROF_04", SocialModuleIdentifier.PROFESSIONAL_EXCELLENCE, "Written Communication", "Crisp emails, tickets, and PR descriptions with zero ambiguous assumptions."),
            SocialCompetency("COMP_PROF_05", SocialModuleIdentifier.PROFESSIONAL_EXCELLENCE, "Time Management", "Disciplined prioritization and realistic delivery schedule commitments."),
            SocialCompetency("COMP_PROF_06", SocialModuleIdentifier.PROFESSIONAL_EXCELLENCE, "Responsibility", "Proactive early alerts on blockers and proactive mitigation proposals."),
            SocialCompetency("COMP_PROF_07", SocialModuleIdentifier.PROFESSIONAL_EXCELLENCE, "Professional Conduct", "Calm composure under crisis conditions and ethical interpersonal behavior.")
        ),
        // Module 8 — Language Development
        SocialModuleIdentifier.LANGUAGE_DEVELOPMENT to listOf(
            SocialCompetency("COMP_LANG_01", SocialModuleIdentifier.LANGUAGE_DEVELOPMENT, "Vocabulary", "Accurate integration of precise, domain-appropriate vocabulary."),
            SocialCompetency("COMP_LANG_02", SocialModuleIdentifier.LANGUAGE_DEVELOPMENT, "Grammar", "Syntactical accuracy, correct clause relationships, and clear tense consistency."),
            SocialCompetency("COMP_LANG_03", SocialModuleIdentifier.LANGUAGE_DEVELOPMENT, "Fluency", "Smooth conversational flow without awkward pauses or excessive hedging."),
            SocialCompetency("COMP_LANG_04", SocialModuleIdentifier.LANGUAGE_DEVELOPMENT, "Pronunciation Practice", "Clear phonetic enunciation and natural syllabic intonation."),
            SocialCompetency("COMP_LANG_05", SocialModuleIdentifier.LANGUAGE_DEVELOPMENT, "Comprehension", "Rapid parsing of complex spoken arguments and nuanced idiomatic phrasing."),
            SocialCompetency("COMP_LANG_06", SocialModuleIdentifier.LANGUAGE_DEVELOPMENT, "Sentence Construction", "Variety in sentence length, compound structures, and rhetorical cadence."),
            SocialCompetency("COMP_LANG_07", SocialModuleIdentifier.LANGUAGE_DEVELOPMENT, "Verbal Expression", "Comfortable, expressive oral communication across professional contexts.")
        )
    )

    fun getCompetenciesForModule(module: SocialModuleIdentifier): List<SocialCompetency> {
        return allCompetencies[module] ?: emptyList()
    }
}

// =========================================================================
// 2. MASTERY DEVELOPMENT STAGES & EVIDENCE DISTINCTIONS
// =========================================================================

enum class MasteryDevelopmentStage(
    val label: String,
    val ordinalValue: Int,
    val description: String
) {
    UNEXPLORED("Unexplored", 0, "No practice or assessment records logged yet."),
    BEGINNER("Beginner", 1, "Early exposure to fundamental concepts and basic practice drills."),
    DEVELOPING("Developing", 2, "Actively acquiring structured habits through repeated simulations."),
    COMPETENT("Competent", 3, "Consistent passing performance across standard simulation rubrics."),
    ADVANCED("Advanced", 4, "High proficiency in simulations supported by documented real-world missions."),
    MASTERY_CANDIDATE("Mastery Candidate", 5, "Sustained high performance across complex simulations and real-world execution."),
    MAINTENANCE("Maintenance", 6, "Mastery achieved; engaging in periodic refresher practice to retain sharp execution.");

    companion object {
        fun fromString(value: String): MasteryDevelopmentStage {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: UNEXPLORED
        }
    }
}

enum class MasteryEvidenceClassification(
    val label: String,
    val isIndependentlyVerified: Boolean,
    val description: String
) {
    SELF_REPORTED("Self-Reported", false, "Personal logging without external observation."),
    PRACTICE_OBSERVED("Practice Observed", false, "Measured elapsed rehearsal time in app."),
    SIMULATION_EVALUATED("Simulation Evaluated", false, "Deterministic rubric score from scenario or speech simulation."),
    ARTIFACT_SUPPORTED("Artifact Supported", false, "Supported by linked notes, outlines, or deliverables."),
    HUMAN_CONFIRMED("Human Confirmed (Unsupported in Local Vault)", false, "Direct external confirmation is unsupported in offline local vault.");

    companion object {
        fun fromString(value: String): MasteryEvidenceClassification {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: SELF_REPORTED
        }
    }
}

// =========================================================================
// 3. ADAPTIVE DIFFICULTY LEVELS
// =========================================================================

enum class AdaptiveDifficulty(
    val label: String,
    val levelNumber: Int,
    val targetPassingScore: Int,
    val description: String
) {
    EASY("Easy", 1, 65, "Gentle pacing, generous time limits, and explicit hints."),
    NORMAL("Normal", 2, 70, "Standard conversational pacing and realistic simulated constraints."),
    CHALLENGING("Challenging", 3, 75, "Elevated counterparty resistance, tighter timing, and complex friction."),
    ADVANCED("Advanced", 4, 80, "High stakes, multi-turn objections, and rigorous evaluation rubrics."),
    EXPERT("Expert", 5, 85, "Zero room for filler; unpredictable dynamic dialogue and executive rigor.");

    companion object {
        fun fromString(value: String): AdaptiveDifficulty {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: NORMAL
        }
    }
}

// =========================================================================
// 4. RECOMMENDATION CATEGORIES & STATUS
// =========================================================================

enum class RecommendationCategory(val label: String) {
    IMPROVE_WEAKNESS("Improve Weakness"),
    MAINTAIN_STRENGTH("Maintain Strength"),
    PRACTICE_NEW_SKILL("Practice New Skill"),
    REAL_WORLD_APPLICATION("Real-World Application"),
    REVISIT_INACTIVE_SKILL("Revisit Inactive Skill"),
    ADVANCE_MASTERY_LEVEL("Advance Mastery Level"),
    REASSESS_SKILL("Reassess Skill");

    companion object {
        fun fromString(value: String): RecommendationCategory {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: IMPROVE_WEAKNESS
        }
    }
}

enum class RecommendationStatus {
    NEW, ACCEPTED, DISMISSED, COMPLETED, EXPIRED;

    companion object {
        fun fromString(value: String): RecommendationStatus {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: NEW
        }
    }
}

data class PersonalizedSocialRecommendation(
    val recommendationId: String,
    val accountOwnerId: String,
    val title: String,
    val targetModule: SocialModuleIdentifier,
    val targetCompetency: String,
    val category: RecommendationCategory,
    val explanation: String,
    val supportingEvidenceReferences: List<String>,
    val recommendedActivityId: String,
    val recommendedActivityType: String, // "EXERCISE", "MISSION", "SCENARIO", "ASSESSMENT"
    val estimatedDurationMinutes: Int,
    val difficulty: AdaptiveDifficulty,
    val expectedLearningOutcome: String,
    val status: RecommendationStatus = RecommendationStatus.NEW,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

// =========================================================================
// 5. PERSONALIZED WEEKLY TRAINING PLAN
// =========================================================================

enum class TrainingActivityType(val label: String) {
    EXERCISE("Targeted Exercise"),
    REAL_WORLD_CHALLENGE("Real-World Mission"),
    REVISION_SESSION("Revision Session"),
    MAINTENANCE("Maintenance Drill"),
    WEEKLY_REVIEW("Weekly Progress Review");

    companion object {
        fun fromString(value: String): TrainingActivityType {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: EXERCISE
        }
    }
}

data class ScheduledTrainingActivity(
    val activityId: String,
    val planId: String,
    val accountOwnerId: String,
    val dayOfWeek: Int, // 1 = Monday, ..., 7 = Sunday
    val scheduledDateEpochMs: Long,
    val module: SocialModuleIdentifier,
    val competencyName: String,
    val activityType: TrainingActivityType,
    val title: String,
    val referenceId: String, // EX_..., MSN_..., etc.
    val durationMinutes: Int,
    val difficulty: AdaptiveDifficulty,
    val isCompleted: Boolean = false,
    val isSkipped: Boolean = false,
    val notes: String? = null,
    val completedEpochMs: Long? = null
)

data class PersonalizedWeeklyTrainingPlan(
    val planId: String,
    val accountOwnerId: String,
    val weeklyDevelopmentObjective: String,
    val targetMinutesPerDay: Int = 15,
    val targetDaysPerWeek: Int = 5,
    val startDateEpochMs: Long,
    val endDateEpochMs: Long,
    val isPaused: Boolean = false,
    val scheduledActivities: List<ScheduledTrainingActivity> = emptyList(),
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

// =========================================================================
// 6. MASTERY PATHWAY & COMPETENCY PROGRESS
// =========================================================================

data class SocialCompetencyProgress(
    val competencyId: String,
    val accountOwnerId: String,
    val module: SocialModuleIdentifier,
    val competencyName: String,
    val masteryStage: MasteryDevelopmentStage,
    val recommendedDifficulty: AdaptiveDifficulty,
    val practiceCount: Int,
    val lastPracticedEpochMs: Long?,
    val lastEvaluatedScore: Int?,
    val evidenceClassification: MasteryEvidenceClassification,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

data class SocialMasteryPathway(
    val module: SocialModuleIdentifier,
    val competencies: List<SocialCompetencyProgress>,
    val currentDevelopmentStage: MasteryDevelopmentStage,
    val recommendedNextActivityTitle: String,
    val recommendedNextActivityId: String,
    val lastPracticedEpochMs: Long?,
    val primaryLearningObjective: String
)

// =========================================================================
// 7. WEEKLY DEVELOPMENT REPORT
// =========================================================================

data class SocialWeeklyDevelopmentReport(
    val weekStartDateEpochMs: Long,
    val weekEndDateEpochMs: Long,
    val exercisesCompletedCount: Int,
    val missionsCompletedCount: Int,
    val totalMinutesPracticed: Int,
    val competenciesPracticed: List<String>,
    val observedImprovements: List<String>,
    val areasRequiringAttention: List<String>,
    val nextWeekPriorities: List<String>
)
