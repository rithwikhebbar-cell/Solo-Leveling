package com.example.domain.engine

import com.example.domain.model.SkillAssessmentClassification
import com.example.domain.model.SkillAssessmentEvaluationResult
import com.example.domain.model.SkillAssessmentRubric
import com.example.domain.model.SkillAssessmentSubmission
import com.example.domain.model.SocialMasteryStage
import com.example.domain.model.SocialModuleIdentifier
import com.example.domain.model.VerificationStatus
import kotlin.math.roundToInt

/**
 * Milestone 12 - Phase 4.1: Social Skill Assessment Engine.
 * Evaluates observable, task-specific skill criteria across all 8 Social Mastery modules.
 * Strictly separates player self-assessments (which carry zero XP) from demonstrated competence.
 * Non-punitive, explainable, and includes explicit scientific limitations disclosures.
 */
object SocialSkillAssessmentEngine {

    val rubrics: Map<SocialModuleIdentifier, SkillAssessmentRubric> = mapOf(
        SocialModuleIdentifier.COMMUNICATION_MASTERY to SkillAssessmentRubric(
            module = SocialModuleIdentifier.COMMUNICATION_MASTERY,
            title = "Communication Mastery Rubric",
            description = "Evaluates structural clarity, conciseness, and active conversational responsiveness.",
            dimension1Name = "Clarity",
            dimension1Desc = "Clear, unambiguous thesis statement delivered in opening remarks.",
            dimension2Name = "Structure",
            dimension2Desc = "Logical organization (e.g. PREP or chronological progression).",
            dimension3Name = "Listening",
            dimension3Desc = "Active comprehension and reflective paraphrasing of the other party.",
            dimension4Name = "Conciseness",
            dimension4Desc = "Elimination of filler words, redundant phrases, and rambling tangents.",
            dimension5Name = "Context Awareness",
            dimension5Desc = "Adaptation of vocabulary and tone to the specific conversational environment."
        ),
        SocialModuleIdentifier.PUBLIC_SPEAKING to SkillAssessmentRubric(
            module = SocialModuleIdentifier.PUBLIC_SPEAKING,
            title = "Public Speaking & Presentation Rubric",
            description = "Evaluates delivery cadence, rhetorical organization, and audience engagement.",
            dimension1Name = "Organization",
            dimension1Desc = "Introduction with clear roadmap, structured body, and definite conclusion.",
            dimension2Name = "Delivery Preparation",
            dimension2Desc = "Poise, slide coordination, and mastery of core talking points.",
            dimension3Name = "Audience Awareness",
            dimension3Desc = "Eye contact across room sections and responsive pacing to room reactions.",
            dimension4Name = "Clarity & Projection",
            dimension4Desc = "Audible diaphragmatic projection, clear enunciation, and purposeful vocal pauses.",
            dimension5Name = "Time Management",
            dimension5Desc = "Adherence to allocated schedule and disciplined pacing through Q&A."
        ),
        SocialModuleIdentifier.NEGOTIATION_PERSUASION to SkillAssessmentRubric(
            module = SocialModuleIdentifier.NEGOTIATION_PERSUASION,
            title = "Negotiation & Persuasion Rubric",
            description = "Evaluates principled negotiation, trade-off analysis, and objection handling.",
            dimension1Name = "Preparation & BATNA",
            dimension1Desc = "Pre-negotiation research on market rates, alternatives, and walk-away threshold.",
            dimension2Name = "Reasoning & Value",
            dimension2Desc = "Articulation of demonstrated value and objective criteria rather than emotional demands.",
            dimension3Name = "Active Listening",
            dimension3Desc = "Identification of the counterparty's underlying constraints and unspoken priorities.",
            dimension4Name = "Objection Handling",
            dimension4Desc = "Diplomatic de-escalation of disagreements without defensive antagonism.",
            dimension5Name = "Agreement Clarity",
            dimension5Desc = "Explicit confirmation of mutually agreed deliverables, tradeoffs, and milestones."
        ),
        SocialModuleIdentifier.LEADERSHIP_TEAMWORK to SkillAssessmentRubric(
            module = SocialModuleIdentifier.LEADERSHIP_TEAMWORK,
            title = "Leadership & Teamwork Rubric",
            description = "Evaluates proactive operational leadership, delegation, and constructive feedback.",
            dimension1Name = "Delegation",
            dimension1Desc = "Clear assignment of ownership, success criteria, and completion deadlines.",
            dimension2Name = "Accountability",
            dimension2Desc = "Taking responsibility for team outcomes and honoring personal commitments.",
            dimension3Name = "Collaboration",
            dimension3Desc = "Fostering psychological safety, welcoming input, and supporting peers.",
            dimension4Name = "Decision-Making",
            dimension4Desc = "Decisiveness under uncertainty and communicating clear rationale to the team.",
            dimension5Name = "Constructive Feedback",
            dimension5Desc = "Radical candor: caring personally while challenging directly in private."
        ),
        SocialModuleIdentifier.NETWORKING_RELATIONSHIPS to SkillAssessmentRubric(
            module = SocialModuleIdentifier.NETWORKING_RELATIONSHIPS,
            title = "Networking & Relationships Rubric",
            description = "Evaluates relationship building, outreach etiquette, and long-term network nurturing.",
            dimension1Name = "Introduction",
            dimension1Desc = "Confidence, posture, and low-friction opening introductions.",
            dimension2Name = "Conversation Initiation",
            dimension2Desc = "Asking engaging, non-transactional open-ended questions.",
            dimension3Name = "Thoughtful Questioning",
            dimension3Desc = "Inquiring substantively into the other person's craft or specific challenges.",
            dimension4Name = "Follow-Up Quality",
            dimension4Desc = "Timely, personalized follow-ups referencing shared conversation points.",
            dimension5Name = "Professional Boundaries",
            dimension5Desc = "Respect for the other person's time and appropriate communication frequency."
        ),
        SocialModuleIdentifier.EMOTIONAL_INTELLIGENCE to SkillAssessmentRubric(
            module = SocialModuleIdentifier.EMOTIONAL_INTELLIGENCE,
            title = "Emotional & Social Intelligence Rubric",
            description = "Evaluates self-regulation, cognitive empathy, and interpersonal de-escalation.",
            dimension1Name = "Perspective-Taking",
            dimension1Desc = "Ability to articulate and validate opposing perspectives before defending self.",
            dimension2Name = "Emotional Reflection",
            dimension2Desc = "Accurate identification of personal emotional triggers and bodily stress signals.",
            dimension3Name = "Response Regulation",
            dimension3Desc = "Pausing between stimulus and response to choose a constructive reaction.",
            dimension4Name = "Boundary Awareness",
            dimension4Desc = "Setting firm, polite boundaries without hostility, guilt, or over-apologizing.",
            dimension5Name = "Conflict Handling",
            dimension5Desc = "Remaining solution-oriented and composed during confrontational tension."
        ),
        SocialModuleIdentifier.PROFESSIONAL_EXCELLENCE to SkillAssessmentRubric(
            module = SocialModuleIdentifier.PROFESSIONAL_EXCELLENCE,
            title = "Professional Excellence Rubric",
            description = "Evaluates executive hygiene, proactive communication, and meeting etiquette.",
            dimension1Name = "Written Communication",
            dimension1Desc = "BLUF format, crisp sentence structure, and zero ambiguity in emails and tickets.",
            dimension2Name = "Reliability",
            dimension2Desc = "Consistent follow-through on verbal and written commitments.",
            dimension3Name = "Accountability",
            dimension3Desc = "Proactive delay communication 24+ hours in advance with mitigation plans.",
            dimension4Name = "Meeting Communication",
            dimension4Desc = "Punctuality, adherence to agendas, and disciplined operational focus.",
            dimension5Name = "Professional Conduct",
            dimension5Desc = "Diplomatic composure, respect for colleagues, and constructive attitude."
        ),
        SocialModuleIdentifier.LANGUAGE_DEVELOPMENT to SkillAssessmentRubric(
            module = SocialModuleIdentifier.LANGUAGE_DEVELOPMENT,
            title = "Language Development Rubric",
            description = "Evaluates grammatical precision, domain vocabulary, and spontaneous oral fluency.",
            dimension1Name = "Vocabulary Application",
            dimension1Desc = "Accurate integration of specialized, domain-specific terminology.",
            dimension2Name = "Sentence Construction",
            dimension2Desc = "Syntactical variety, correct grammatical clauses, and structural coherence.",
            dimension3Name = "Comprehension",
            dimension3Desc = "Rapid, effortless understanding of complex prompts and dialogue turns.",
            dimension4Name = "Pronunciation & Cadence",
            dimension4Desc = "Clear phonetics, steady syllable pacing, and natural intonation.",
            dimension5Name = "Speaking Confidence",
            dimension5Desc = "Circumlocution around unfamiliar words without halting or reverting to native tongue."
        )
    )

    fun evaluateSubmission(submission: SkillAssessmentSubmission): SkillAssessmentEvaluationResult {
        val rubric = rubrics[submission.module] ?: rubrics.values.first()

        val s1 = submission.scoreDim1.coerceIn(0, 100)
        val s2 = submission.scoreDim2.coerceIn(0, 100)
        val s3 = submission.scoreDim3.coerceIn(0, 100)
        val s4 = submission.scoreDim4.coerceIn(0, 100)
        val s5 = submission.scoreDim5.coerceIn(0, 100)

        val overallScore = ((s1 + s2 + s3 + s4 + s5) / 5f).roundToInt()

        val stage = when {
            overallScore >= 90 -> SocialMasteryStage.MASTER
            overallScore >= 80 -> SocialMasteryStage.ADVANCED
            overallScore >= 70 -> SocialMasteryStage.PROFICIENT
            overallScore >= 55 -> SocialMasteryStage.COMPETENT
            else -> SocialMasteryStage.NOVICE
        }

        val dimMap = mapOf(
            rubric.dimension1Name to s1,
            rubric.dimension2Name to s2,
            rubric.dimension3Name to s3,
            rubric.dimension4Name to s4,
            rubric.dimension5Name to s5
        )

        // Generate qualitative summary & recommendations based on lowest dimension
        val lowestEntry = dimMap.minByOrNull { it.value }
        val recommendations = mutableListOf<String>()

        if (lowestEntry != null && lowestEntry.value < 70) {
            recommendations.add("Priority Focus: Reinforce ${lowestEntry.key} (${lowestEntry.value}%). Practice targeted PREP or speaking drills.")
        }
        if (overallScore >= 80) {
            recommendations.add("Strong performance across ${submission.module.displayName}. Advance to higher-tier mission applications.")
        } else {
            recommendations.add("Complete foundational D-Rank real-world missions to reinforce consistency.")
        }

        // Summary and qualitative reflection
        val summary = if (submission.classification == SkillAssessmentClassification.SELF_ASSESSMENT) {
            "Self-assessment completed for ${submission.module.displayName}. Subjective perceived rating: $overallScore% (${stage.label}). " +
            "Subjective self-ratings carry 0 XP and reflect perceived confidence, not verified mastery."
        } else {
            "Assessment completed for ${submission.module.displayName}. Overall score: $overallScore% (${stage.label}). " +
            "Evaluated across 5 observable dimensions without speculative psychological assumptions."
        }

        // Progression rules: Self-assessment sliders carry 0 XP to prevent progression inflation.
        // Demonstrated simulation rubrics or evidence-supported evaluations carry standard social XP.
        val xp = when (submission.classification) {
            SkillAssessmentClassification.SELF_ASSESSMENT -> 0
            SkillAssessmentClassification.SIMULATION_EVALUATED -> if (overallScore >= 70) 35 else 20
            SkillAssessmentClassification.EVIDENCE_SUPPORTED -> if (overallScore >= 70) 30 else 15
            SkillAssessmentClassification.INDEPENDENTLY_CONFIRMED -> 0
        }

        val verification = when (submission.classification) {
            SkillAssessmentClassification.SELF_ASSESSMENT -> VerificationStatus.SELF_REPORTED
            SkillAssessmentClassification.SIMULATION_EVALUATED -> VerificationStatus.VERIFIED_AI_RUBRIC
            SkillAssessmentClassification.EVIDENCE_SUPPORTED -> VerificationStatus.SELF_REPORTED
            SkillAssessmentClassification.INDEPENDENTLY_CONFIRMED -> VerificationStatus.PENDING_REVIEW
        }

        return SkillAssessmentEvaluationResult(
            assessmentId = submission.assessmentId,
            module = submission.module,
            classification = submission.classification,
            overallScore = overallScore,
            dimensionScores = dimMap,
            masteryStage = stage,
            qualitativeSummary = summary,
            actionableRecommendations = recommendations,
            xpAwarded = xp,
            verificationStatus = verification,
            assessmentDateEpochMs = submission.assessedEpochMs,
            scoringMethod = submission.classification.scoringMethod,
            evidenceBasis = submission.classification.evidenceBasis,
            limitationsDisclaimer = submission.classification.limitationsDisclaimer
        )
    }
}
