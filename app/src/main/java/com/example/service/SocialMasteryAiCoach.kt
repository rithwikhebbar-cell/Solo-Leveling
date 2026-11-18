package com.example.service

import com.example.data.integration.BackendAiGateway
import com.example.data.integration.BackendAiProxyRequest
import com.example.data.integration.OpenRouterMessage
import com.example.domain.model.AiCoachingMessage
import com.example.domain.model.AiCoachingMode
import com.example.domain.model.PersonalizedSocialRecommendation
import com.example.domain.model.PersonalizedWeeklyTrainingPlan
import com.example.domain.model.SocialProfile
import com.example.domain.model.SocialWeeklyDevelopmentReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Milestone 12 - Phase 3: AI Communication Coach Service.
 * Responsibilities:
 * - Integrates with existing BackendAiGateway proxy.
 * - Supports 4 specialized coaching modes: Personal, Practice, Roleplay, Professional.
 * - Provides sanitized prompts, conversation history, and structured improvement advice.
 * - Enforces zero-progression authority (coaching suggestions only; cannot award XP).
 * - Full offline fallback when network or backend proxy is unavailable.
 */
class SocialMasteryAiCoach(
    private val backendAiGateway: BackendAiGateway,
    private val socialPrivacyService: SocialPrivacyService
) {
    companion object {
        const val MAX_STORED_MESSAGES = 20
        const val MAX_RECENT_TURNS_FOR_LLM = 6
    }

    private val _messages = MutableStateFlow<List<AiCoachingMessage>>(emptyList())
    val messages: StateFlow<List<AiCoachingMessage>> = _messages.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    fun clearSession() {
        _messages.value = emptyList()
    }

    suspend fun sendMessage(
        userPrompt: String,
        mode: AiCoachingMode,
        profile: SocialProfile?,
        playerName: String? = null,
        backendBaseUrl: String = "https://default.supabase.co"
    ): Result<String> {
        val cleanUserText = socialPrivacyService.sanitizeTranscriptForAi(userPrompt, playerName)
        val userMsg = AiCoachingMessage(
            messageId = UUID.randomUUID().toString(),
            role = "USER",
            content = cleanUserText
        )
        _messages.value = (_messages.value + userMsg).takeLast(MAX_STORED_MESSAGES)
        _isGenerating.value = true

        val systemPrompt = buildSystemPrompt(mode, profile)
        val recentTurns = _messages.value.takeLast(MAX_RECENT_TURNS_FOR_LLM).map {
            OpenRouterMessage(role = it.role.lowercase(), content = it.content)
        }

        val request = BackendAiProxyRequest(
            model = "google/gemini-2.5-flash",
            requestType = "CHAT",
            systemPrompt = systemPrompt,
            contextSummary = "Player rank: ${profile?.overallSocialMasteryStage?.name ?: "NOVICE"}, practice minutes: ${profile?.totalPracticeMinutes ?: 0}",
            recentTurns = recentTurns,
            userPrompt = cleanUserText,
            stream = false
        )

        return runCatching {
            val response = backendAiGateway.requestStructured(backendBaseUrl, request)
            val coachReply = response.fullText.ifBlank {
                generateOfflineFallback(cleanUserText, mode)
            }

            val coachMsg = AiCoachingMessage(
                messageId = UUID.randomUUID().toString(),
                role = "COACH",
                content = coachReply,
                suggestedFollowUpQuestions = generateFollowUpPrompts(mode)
            )
            _messages.value = (_messages.value + coachMsg).takeLast(MAX_STORED_MESSAGES)
            _isGenerating.value = false
            coachReply
        }.recover { err ->
            _isGenerating.value = false
            val fallback = generateOfflineFallback(cleanUserText, mode)
            val fallbackMsg = AiCoachingMessage(
                messageId = UUID.randomUUID().toString(),
                role = "COACH",
                content = fallback,
                suggestedFollowUpQuestions = listOf("Try typing another practice question offline.")
            )
            _messages.value = (_messages.value + fallbackMsg).takeLast(MAX_STORED_MESSAGES)
            fallback
        }
    }

    // =========================================================================
    // PHASE 5: ADAPTIVE COACHING EXTENSIONS
    // =========================================================================

    suspend fun explainRecommendation(
        rec: PersonalizedSocialRecommendation,
        profile: SocialProfile?,
        backendBaseUrl: String = "https://default.supabase.co"
    ): Result<String> {
        val prompt = "Why was this recommendation generated for me?\n" +
                "Title: ${rec.title}\n" +
                "Module: ${rec.targetModule.displayName}\n" +
                "Competency: ${rec.targetCompetency}\n" +
                "Category: ${rec.category.label}\n" +
                "Persisted Context: ${rec.explanation}\n" +
                "Please explain the behavioral rationale and how this activity accelerates my interpersonal mastery."
        return sendMessage(prompt, AiCoachingMode.PERSONAL_COACH, profile, backendBaseUrl = backendBaseUrl)
    }

    suspend fun explainWeeklyTrainingPlan(
        plan: PersonalizedWeeklyTrainingPlan,
        profile: SocialProfile?,
        backendBaseUrl: String = "https://default.supabase.co"
    ): Result<String> {
        val activitySummary = plan.scheduledActivities.joinToString("; ") { "Day ${it.dayOfWeek}: ${it.title} (${it.durationMinutes}m)" }
        val prompt = "Review my personalized weekly training plan:\n" +
                "Objective: ${plan.weeklyDevelopmentObjective}\n" +
                "Schedule (${plan.targetDaysPerWeek} days, ${plan.targetMinutesPerDay}m/day): $activitySummary\n" +
                "Explain how this weekly progression balances skill acquisition, simulation, and real-world application."
        return sendMessage(prompt, AiCoachingMode.PERSONAL_COACH, profile, backendBaseUrl = backendBaseUrl)
    }

    suspend fun provideWeeklyReviewSummary(
        report: SocialWeeklyDevelopmentReport,
        profile: SocialProfile?,
        backendBaseUrl: String = "https://default.supabase.co"
    ): Result<String> {
        val prompt = "Provide an executive weekly debrief on my communication performance:\n" +
                "Total Practice Minutes: ${report.totalMinutesPracticed}\n" +
                "Exercises Completed: ${report.exercisesCompletedCount}\n" +
                "Real-World Missions Cleared: ${report.missionsCompletedCount}\n" +
                "Competencies Practiced: ${report.competenciesPracticed.joinToString(", ")}\n" +
                "Observed Improvements: ${report.observedImprovements.joinToString("; ")}\n" +
                "Areas to Watch: ${report.areasRequiringAttention.joinToString("; ")}\n" +
                "What is my highest-leverage priority for next week?"
        return sendMessage(prompt, AiCoachingMode.PROFESSIONAL_COACH, profile, backendBaseUrl = backendBaseUrl)
    }

    suspend fun provideCompetencyGuidance(
        competencyName: String,
        stage: String,
        profile: SocialProfile?,
        backendBaseUrl: String = "https://default.supabase.co"
    ): Result<String> {
        val prompt = "Give me tactical guidance on mastering competency '$competencyName'. My current developmental classification is $stage. What 2 micro-habits will advance my execution?"
        return sendMessage(prompt, AiCoachingMode.PRACTICE_COACH, profile, backendBaseUrl = backendBaseUrl)
    }

    private fun buildSystemPrompt(mode: AiCoachingMode, profile: SocialProfile?): String {
        val base = "You are the Social Monarch Communication Coach in SOLO LEVELING - REALITY OS. " +
                "You guide players to master real-world interpersonal communication, public speaking, negotiation, and leadership presence. " +
                "Maintain a sharp, empowering, constructive tone inspired by tactical executive coaching. " +
                "Never invent psychological diagnoses. Keep advice direct, structured, and immediately actionable."

        return when (mode) {
            AiCoachingMode.PERSONAL_COACH ->
                "$base Mode: PERSONAL COACH. Answer the player's questions about social dynamics, overcoming hesitation, and conversational strategies."
            AiCoachingMode.PRACTICE_COACH ->
                "$base Mode: PRACTICE COACH. Analyze the user's supplied response. Point out structural strengths, identify rambling or filler words, and rewrite their thought with maximum clarity."
            AiCoachingMode.ROLEPLAY_COACH ->
                "$base Mode: ROLEPLAY PARTNER. Act dynamically in character for the scenario requested. Respond realistically and challenge the player with realistic conversational tension."
            AiCoachingMode.PROFESSIONAL_COACH ->
                "$base Mode: PROFESSIONAL COACH. Focus strictly on executive presence, meeting navigation, upward management, salary negotiation, and boundary setting."
        }
    }

    private fun generateOfflineFallback(userText: String, mode: AiCoachingMode): String {
        return when (mode) {
            AiCoachingMode.PRACTICE_COACH ->
                "[Offline Advisory] Reviewing your response: 1. Keep your primary thesis in the first sentence. 2. Remove filler phrases like 'I think maybe' or 'sort of'. 3. End with an open-ended question to sustain engagement."
            AiCoachingMode.PROFESSIONAL_COACH ->
                "[Offline Advisory] For professional situations, structure your response using BLUF (Bottom Line Up Front): State the core decision or need, explain the business justification, and propose concrete next steps."
            AiCoachingMode.ROLEPLAY_COACH ->
                "[Offline Partner] 'I appreciate your input. Could you elaborate on how your proposed timeline mitigates our risk for Friday's delivery?'"
            else ->
                "[Offline Coach] Communication is a muscle forged through friction. Focus on diaphragmatic breathing, deliberate sentence pacing, and reflective active listening."
        }
    }

    private fun generateFollowUpPrompts(mode: AiCoachingMode): List<String> {
        return when (mode) {
            AiCoachingMode.PRACTICE_COACH -> listOf(
                "How can I make this 50% shorter?",
                "Rewrite this in a more assertive tone.",
                "How do I handle an interruption here?"
            )
            AiCoachingMode.PROFESSIONAL_COACH -> listOf(
                "How do I say 'no' without sounding uncooperative?",
                "How do I brief my manager on an unexpected delay?",
                "What is the best way to ask for a compensation review?"
            )
            else -> listOf(
                "Can you give me a 60-second exercise for this?",
                "What is the most common mistake here?",
                "How do I stay calm under confrontational tension?"
            )
        }
    }
}
