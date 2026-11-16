package com.example.domain.engine

import com.example.domain.model.ListeningExercise
import com.example.domain.model.MeasurableSpeechMetrics
import com.example.domain.model.PracticeExercise
import com.example.domain.model.PracticeEvaluationResult
import kotlin.math.roundToInt

/**
 * Milestone 12 - Phase 2: Deterministic Speech & Text Evaluation Engine.
 * Responsibilities:
 * - Computes objective measurable metrics (WPM, duration, word count, transcript density).
 * - Evaluates open-ended responses against structured rubrics deterministically offline.
 * - Scores listening comprehension against key factual tokens.
 * - Distinguishes measurable signals from subjective assumptions.
 * - Provides non-punitive, constructive feedback.
 */
object SpeechEvaluationEngine {

    fun evaluatePracticeTextResponse(
        exercise: PracticeExercise,
        userResponseText: String,
        durationSeconds: Int = 0,
        hasAudioRecording: Boolean = false
    ): PracticeEvaluationResult {
        val cleanText = userResponseText.trim()
        val words = if (cleanText.isBlank()) emptyList() else cleanText.split(Regex("\\s+"))
        val wordCount = words.size
        val durationMins = if (durationSeconds > 0) durationSeconds / 60f else (wordCount / 130f).coerceAtLeast(0.5f)
        val wpm = if (durationMins > 0f) (wordCount / durationMins).roundToInt() else 0

        val measurableMetrics = MeasurableSpeechMetrics(
            durationSeconds = durationSeconds,
            wordCount = wordCount,
            wordsPerMinute = wpm,
            transcriptAvailable = cleanText.isNotBlank(),
            audioRecordingAvailable = hasAudioRecording
        )

        if (cleanText.length < 15 || wordCount < 4) {
            return PracticeEvaluationResult(
                overallScore = 30,
                dimensionScores = mapOf(
                    "Clarity" to 30,
                    "Relevance" to 30,
                    "Conciseness" to 30,
                    "Confidence" to 30
                ),
                isPassed = false,
                measurableMetrics = measurableMetrics,
                strengths = listOf("Submission attempted"),
                improvementAreas = listOf("Response was too brief to evaluate thoroughly. Aim for at least 3-4 complete sentences."),
                qualitativeSummary = "Insufficient length for evaluation. Practice delivering a complete structured thought."
            )
        }

        // 1. Clarity Dimension (Structure, sentence count, punctuation)
        val sentences = cleanText.split(Regex("[.!?]+")).filter { it.isNotBlank() }
        val avgSentenceLength = if (sentences.isNotEmpty()) wordCount.toFloat() / sentences.size else 0f
        val clarityScore = when {
            avgSentenceLength in 8f..22f && sentences.size >= 2 -> 88
            avgSentenceLength in 5f..30f -> 75
            else -> 60
        }

        // 2. Conciseness Dimension (Avoid repetitive rambling or excessive length)
        val uniqueWords = words.map { it.lowercase() }.toSet()
        val lexicalDiversity = if (wordCount > 0) uniqueWords.size.toFloat() / wordCount else 0f
        val concisenessScore = when {
            lexicalDiversity >= 0.70f && wordCount in 30..180 -> 90
            lexicalDiversity >= 0.55f && wordCount in 20..250 -> 78
            else -> 65
        }

        // 3. Relevance & Objective Alignment (Keyword and intent matching)
        val objectiveTokens = exercise.learningObjective.lowercase().split(Regex("[\\s,.]+")).filter { it.length > 3 }
        val matchedTokens = objectiveTokens.count { token -> cleanText.lowercase().contains(token) }
        val relevanceScore = when {
            matchedTokens >= 3 -> 92
            matchedTokens >= 1 -> 80
            wordCount >= 40 -> 75
            else -> 68
        }

        // 4. Confidence / Decisive Tone (Lack of excessive hedging words)
        val hedgeWords = listOf("maybe", "perhaps", "i guess", "i think maybe", "sort of", "kinda", "um", "uh")
        val hedgeCount = hedgeWords.count { cleanText.lowercase().contains(it) }
        val confidenceScore = when {
            hedgeCount == 0 -> 90
            hedgeCount <= 2 -> 78
            else -> 65
        }

        val dimensionScores = mutableMapOf(
            "Clarity" to clarityScore,
            "Relevance" to relevanceScore,
            "Conciseness" to concisenessScore,
            "Confidence" to confidenceScore
        )

        // Calculate weighted overall score
        val overallScore = (
            (clarityScore * 0.30f) +
            (relevanceScore * 0.25f) +
            (concisenessScore * 0.25f) +
            (confidenceScore * 0.20f)
        ).roundToInt().coerceIn(0, 100)

        val isPassed = overallScore >= exercise.minPassingScore

        val strengths = mutableListOf<String>()
        val improvements = mutableListOf<String>()

        if (clarityScore >= 80) strengths.add("Clear sentence structures with balanced pacing.")
        else improvements.add("Vary sentence length to create natural rhetorical momentum.")

        if (concisenessScore >= 80) strengths.add("Strong lexical diversity and direct phrasing.")
        else improvements.add("Eliminate repeated words and tighten your core arguments.")

        if (confidenceScore >= 80) strengths.add("Decisive, assertive tone free of unnecessary hedging.")
        else improvements.add("Avoid hedging phrases like 'I guess' or 'maybe' when stating recommendations.")

        if (relevanceScore >= 80) strengths.add("Well aligned with the target learning objective.")
        else improvements.add("Focus more directly on the scenario objective.")

        val qualitativeSummary = if (isPassed) {
            "Strong execution. The response demonstrated clarity and alignment with ${exercise.module.displayName} objectives."
        } else {
            "Good foundation, but requires refinement in conciseness and decisive expression to meet the ${exercise.minPassingScore}% passing threshold."
        }

        return PracticeEvaluationResult(
            overallScore = overallScore,
            dimensionScores = dimensionScores,
            isPassed = isPassed,
            measurableMetrics = measurableMetrics,
            strengths = strengths,
            improvementAreas = improvements,
            qualitativeSummary = qualitativeSummary,
            nextRecommendedExerciseId = if (isPassed) "EX_PUB_01" else exercise.exerciseId
        )
    }

    fun evaluateListeningResponse(
        exercise: ListeningExercise,
        userSummaryText: String,
        identifiedQuestionsText: String
    ): PracticeEvaluationResult {
        val combinedText = "$userSummaryText $identifiedQuestionsText".lowercase()
        val words = combinedText.split(Regex("\\s+")).filter { it.isNotBlank() }
        val wordCount = words.size

        // Token match against key facts
        val matchedFactsCount = exercise.keyFactsToIdentify.count { fact ->
            val keywords = fact.lowercase().split(Regex("[\\s,/]+")).filter { it.length > 3 }
            keywords.any { combinedText.contains(it) }
        }

        val factRatio = if (exercise.keyFactsToIdentify.isNotEmpty()) {
            matchedFactsCount.toFloat() / exercise.keyFactsToIdentify.size
        } else 1.0f

        val accuracyScore = (factRatio * 100).roundToInt().coerceIn(30, 95)
        val followUpScore = if (identifiedQuestionsText.trim().length >= 15) 85 else 55

        val overallScore = ((accuracyScore * 0.65f) + (followUpScore * 0.35f)).roundToInt().coerceIn(0, 100)
        val isPassed = overallScore >= exercise.minPassingScore

        val strengths = mutableListOf<String>()
        val improvements = mutableListOf<String>()

        if (matchedFactsCount >= 2) {
            strengths.add("Extracted critical constraints and dependencies accurately.")
        } else {
            improvements.add("Review the passage again for specific deadlines and blocked team members.")
        }

        if (followUpScore >= 75) {
            strengths.add("Formulated a proactive follow-up inquiry.")
        } else {
            improvements.add("Include a concrete clarifying question targeted at root-cause unblocking.")
        }

        return PracticeEvaluationResult(
            overallScore = overallScore,
            dimensionScores = mapOf(
                "Fact Extraction" to accuracyScore,
                "Inquiry Quality" to followUpScore
            ),
            isPassed = isPassed,
            measurableMetrics = MeasurableSpeechMetrics(
                durationSeconds = 0,
                wordCount = wordCount,
                wordsPerMinute = 0,
                transcriptAvailable = true,
                audioRecordingAvailable = false
            ),
            strengths = strengths,
            improvementAreas = improvements,
            qualitativeSummary = if (isPassed) "Comprehension target achieved." else "Key situational details were missed.",
            nextRecommendedExerciseId = exercise.exerciseId
        )
    }
}
