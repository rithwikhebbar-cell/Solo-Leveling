package com.example.domain.engine

import com.example.data.integration.BackendAiGateway
import com.example.data.integration.BackendAiProxyRequest
import com.example.data.integration.FirebaseGeminiAiGateway
import com.example.data.integration.HttpBackendAiGateway
import com.example.domain.model.AiConnectionStatus
import com.example.domain.model.SupportedAiModels
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.util.Locale

@Serializable
private data class GeminiIntentPayload(
    val commandId: String = "UNKNOWN",
    val confidence: Double = 0.0,
    val parametersJson: String? = null,
    val requiresClarification: Boolean = false,
    val clarificationQuestion: String? = null,
    val spokenSummary: String? = null
)

/**
 * Gemini-powered Natural Language Intent Understanding Engine with strict schema validation,
 * security guardrails, ambiguity clarification, and deterministic offline fallback.
 */
class GeminiIntentEngine(
    private val backendAiGateway: BackendAiGateway
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun interpret(
        rawTranscript: String,
        contextSummary: String = "",
        cloudAiAllowed: Boolean = false,
        backendBaseUrl: String = "",
        preferredModelId: String = SupportedAiModels.FAST_INTENT_MODEL_ID
    ): StructuredVoiceIntent {
        val clean = rawTranscript.trim()
        if (clean.isBlank()) {
            return StructuredVoiceIntent(
                commandId = null,
                category = null,
                confidence = 0.0,
                requiresClarification = true,
                clarificationQuestion = "No speech detected. Please state a command such as 'show my tasks' or 'what is my rank?'.",
                source = IntentResolutionSource.OFFLINE_LOCAL,
                rawTranscript = rawTranscript
            )
        }

        // Security guardrail: reject prompt injection or unauthorized stat/XP mutation attempts immediately
        if (isUnauthorizedMutationOrInjection(clean)) {
            return StructuredVoiceIntent(
                commandId = null,
                category = null,
                confidence = 1.0,
                source = IntentResolutionSource.REJECTED,
                rawTranscript = rawTranscript,
                rejectionReason = "Security Guard: Voice commands cannot bypass progression rules, grant XP, alter ranks/attributes, or override safety checks."
            )
        }

        val sanitizedTranscript = PromptSanitizer.sanitize(clean, maxChars = 320)
        val canUseCloud = cloudAiAllowed &&
            (backendAiGateway.supportsDirectCloud || HttpBackendAiGateway.normalizeProxyEndpoint(backendBaseUrl) != null)

        if (canUseCloud) {
            val geminiResult = interpretWithGemini(
                sanitizedTranscript = sanitizedTranscript,
                rawTranscript = clean,
                contextSummary = contextSummary,
                backendBaseUrl = backendBaseUrl,
                modelId = preferredModelId
            )
            if (geminiResult != null) {
                return geminiResult
            }
        }

        return matchLocally(clean)
    }

    private suspend fun interpretWithGemini(
        sanitizedTranscript: String,
        rawTranscript: String,
        contextSummary: String,
        backendBaseUrl: String,
        modelId: String
    ): StructuredVoiceIntent? {
        val response = backendAiGateway.requestStructured(
            backendBaseUrl = backendBaseUrl,
            request = BackendAiProxyRequest(
                model = SupportedAiModels.resolveGeminiModelId(modelId),
                requestType = "VOICE_INTENT",
                systemPrompt = INTENT_SYSTEM_PROMPT + "\n\n" + VoiceCommandRegistry.formatPromptCatalog(),
                contextSummary = contextSummary.take(1200),
                userPrompt = "Player spoken transcript: \"$sanitizedTranscript\"",
                stream = false,
                temperature = 0.1f,
                maxOutputTokens = 400,
                responseSchemaType = FirebaseGeminiAiGateway.SCHEMA_VOICE_INTENT
            )
        )
        if (response.status != AiConnectionStatus.CONNECTED || response.fullText.isBlank()) {
            return null
        }
        return parseAndValidateGeminiJson(response.fullText, rawTranscript)
    }

    internal fun parseAndValidateGeminiJson(
        rawJson: String,
        rawTranscript: String
    ): StructuredVoiceIntent? {
        val payload = runCatching {
            json.decodeFromString<GeminiIntentPayload>(rawJson.trim())
        }.getOrNull() ?: return null

        val confidence = payload.confidence.coerceIn(0.0, 1.0)
        val params = parseParametersMap(payload.parametersJson)
        val rawCommand = payload.commandId.trim().uppercase(Locale.US)

        if (rawCommand == "AMBIGUOUS" || payload.requiresClarification) {
            return StructuredVoiceIntent(
                commandId = SystemCommandId.fromWireId(rawCommand),
                category = SystemCommandId.fromWireId(rawCommand)?.let { VoiceCommandRegistry.findById(it).category },
                confidence = confidence,
                parameters = params,
                requiresClarification = true,
                clarificationQuestion = payload.clarificationQuestion?.takeIf { it.isNotBlank() }
                    ?: "Could you clarify what action you want the System to perform?",
                source = IntentResolutionSource.GEMINI_AI,
                rawTranscript = rawTranscript
            )
        }

        if (rawCommand == "CONVERSATION" || rawCommand == SystemCommandId.AI_CONVERSATION_QUERY.wireId) {
            val def = VoiceCommandRegistry.findById(SystemCommandId.AI_CONVERSATION_QUERY)
            return StructuredVoiceIntent(
                commandId = def.id,
                category = def.category,
                confidence = confidence.coerceAtLeast(0.75),
                parameters = params + ("query" to rawTranscript),
                requiresConfirmation = false,
                spokenSummary = payload.spokenSummary?.takeIf { it.isNotBlank() },
                source = IntentResolutionSource.GEMINI_AI,
                rawTranscript = rawTranscript
            )
        }

        val commandId = SystemCommandId.fromWireId(rawCommand)
        if (commandId == null) {
            // Strict schema enforcement: reject any hallucinated/unregistered command ID
            return StructuredVoiceIntent(
                commandId = null,
                category = null,
                confidence = confidence,
                requiresClarification = true,
                clarificationQuestion = payload.spokenSummary?.takeIf { it.isNotBlank() }
                    ?: "Command not recognized in the System registry. Try asking for your status, tasks, habits, workout, study session, or quests.",
                source = IntentResolutionSource.GEMINI_AI,
                rawTranscript = rawTranscript
            )
        }

        val def = VoiceCommandRegistry.findById(commandId)

        // Low-confidence safeguard: ask clarification instead of guessing
        if (confidence < MIN_EXECUTION_CONFIDENCE) {
            return StructuredVoiceIntent(
                commandId = commandId,
                category = def.category,
                confidence = confidence,
                parameters = params,
                requiresClarification = true,
                clarificationQuestion = payload.clarificationQuestion?.takeIf { it.isNotBlank() }
                    ?: "Did you mean '${def.label}'? Please confirm or rephrase.",
                source = IntentResolutionSource.GEMINI_AI,
                rawTranscript = rawTranscript
            )
        }

        // Validate required parameters
        val paramError = VoiceCommandRegistry.validateParameters(def, params)
        if (paramError != null) {
            return StructuredVoiceIntent(
                commandId = commandId,
                category = def.category,
                confidence = confidence,
                parameters = params,
                requiresClarification = true,
                clarificationQuestion = payload.clarificationQuestion?.takeIf { it.isNotBlank() }
                    ?: "$paramError Please specify the missing details.",
                source = IntentResolutionSource.GEMINI_AI,
                rawTranscript = rawTranscript
            )
        }

        return StructuredVoiceIntent(
            commandId = commandId,
            category = def.category,
            confidence = confidence,
            parameters = params,
            requiresConfirmation = def.requiresConfirmation,
            requiresClarification = false,
            spokenSummary = payload.spokenSummary?.takeIf { it.isNotBlank() },
            source = IntentResolutionSource.GEMINI_AI,
            rawTranscript = rawTranscript
        )
    }

    private fun parseParametersMap(parametersJson: String?): Map<String, String> {
        if (parametersJson.isNullOrBlank()) return emptyMap()
        val obj = runCatching { json.parseToJsonElement(parametersJson) as? JsonObject }.getOrNull()
            ?: return emptyMap()
        return obj.entries.mapNotNull { (k, v) ->
            val strVal = (v as? JsonPrimitive)?.contentOrNull?.trim()
            if (strVal.isNullOrBlank()) null else k to PromptSanitizer.sanitize(strVal, maxChars = 120)
        }.toMap()
    }

    /**
     * Deterministic offline intent matcher supporting navigation, telemetry queries, and core commands
     * without requiring internet access.
     */
    fun matchLocally(rawTranscript: String): StructuredVoiceIntent {
        val clean = rawTranscript.trim()
        val lower = clean.lowercase(Locale.US)
            .replace(Regex("[^a-z0-9'\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        if (isUnauthorizedMutationOrInjection(clean)) {
            return StructuredVoiceIntent(
                commandId = null,
                category = null,
                confidence = 1.0,
                source = IntentResolutionSource.REJECTED,
                rawTranscript = rawTranscript,
                rejectionReason = "Security Guard: Voice commands cannot grant XP, alter ranks/attributes, or override safety guards."
            )
        }

        val extractedDuration = Regex("\\b(\\d{1,3})\\s*(min|minute|minutes|hr|hour|hours)\\b")
            .find(lower)?.let { m ->
                val n = m.groupValues[1].toIntOrNull() ?: return@let null
                if (m.groupValues[2].startsWith("hr") || m.groupValues[2].startsWith("hour")) (n * 60).toString() else n.toString()
            }

        fun buildIntent(
            id: SystemCommandId,
            params: Map<String, String> = emptyMap(),
            confidence: Double = 0.88
        ): StructuredVoiceIntent {
            val def = VoiceCommandRegistry.findById(id)
            val paramErr = VoiceCommandRegistry.validateParameters(def, params)
            return StructuredVoiceIntent(
                commandId = id,
                category = def.category,
                confidence = confidence,
                parameters = params,
                requiresConfirmation = def.requiresConfirmation,
                requiresClarification = paramErr != null,
                clarificationQuestion = paramErr,
                source = IntentResolutionSource.OFFLINE_LOCAL,
                rawTranscript = rawTranscript
            )
        }

        return when {
            // System Control
            "awakening" in lower || "replay intro" in lower ->
                buildIntent(SystemCommandId.REPLAY_AWAKENING)
            ("open" in lower || "go to" in lower) && ("dashboard" in lower || "home" in lower) ->
                buildIntent(SystemCommandId.NAVIGATE_DASHBOARD)
            ("open" in lower || "go to" in lower) && ("ai companion" in lower || "companion" in lower || "chatbot" in lower) ->
                buildIntent(SystemCommandId.NAVIGATE_AI_COMPANION)
            ("open" in lower || "go to" in lower) && ("settings" in lower || "voice settings" in lower) ->
                buildIntent(SystemCommandId.NAVIGATE_SETTINGS)
            ("open" in lower || "go to" in lower) && ("shadow realm" in lower || "shadow army" in lower) ->
                buildIntent(SystemCommandId.NAVIGATE_SHADOW_REALM)
            ("open" in lower || "go to" in lower) && ("profile" in lower || "status window" in lower || lower == "open status") ->
                buildIntent(SystemCommandId.NAVIGATE_PROFILE)
            ("open" in lower || "go to" in lower) && ("workout" in lower || "physical" in lower) ->
                buildIntent(SystemCommandId.NAVIGATE_WORKOUT)

            // Academic Development — revision task must precede generic task catch-all
            ("create" in lower || "add" in lower) && "revision" in lower -> {
                val topic = clean.replace(Regex("(?i)\\b(create|add|a|an|revision|task|quest|for|on)\\b"), "")
                    .replace(Regex("\\s+"), " ")
                    .trim()
                buildIntent(
                    SystemCommandId.CREATE_REVISION_TASK,
                    params = buildMap {
                        if (topic.isNotBlank()) put("topic", topic)
                        if (extractedDuration != null) put("durationMinutes", extractedDuration)
                    }
                )
            }

            // Daily Management — create / complete task
            (lower.startsWith("add a task") || lower.startsWith("add task") || lower.startsWith("create a task") || lower.startsWith("create task")) -> {
                val title = clean.replace(Regex("(?i)^(add|create)\\s+(a\\s+)?task\\s*(to\\s+|for\\s+)?"), "").trim()
                buildIntent(
                    SystemCommandId.CREATE_TASK,
                    params = if (title.isNotBlank()) mapOf("title" to title) else emptyMap()
                )
            }
            ("complete" in lower || "mark" in lower || "finish" in lower) && "habit" in lower -> {
                val habitName = clean.replace(Regex("(?i)\\b(complete|mark|finish|done|my|the|habit|as|today)\\b"), "")
                    .replace(Regex("\\s+"), " ")
                    .trim()
                buildIntent(
                    SystemCommandId.COMPLETE_HABIT,
                    params = if (habitName.isNotBlank()) mapOf("habitName" to habitName) else emptyMap()
                )
            }
            ("complete" in lower || "mark" in lower || "finish" in lower) && ("task" in lower || "routine" in lower) -> {
                val title = clean.replace(Regex("(?i)\\b(complete|mark|finish|done|my|the|task|as)\\b"), "")
                    .replace(Regex("\\s+"), " ")
                    .trim()
                buildIntent(
                    SystemCommandId.COMPLETE_TASK,
                    params = if (title.isNotBlank()) mapOf("title" to title) else emptyMap()
                )
            }
            "schedule" in lower -> buildIntent(SystemCommandId.VIEW_DAILY_SCHEDULE)
            "task" in lower -> buildIntent(SystemCommandId.VIEW_TODAY_TASKS)

            // Habits & Routines
            "streak" in lower -> buildIntent(SystemCommandId.GET_HABIT_STREAK)
            "habit" in lower -> buildIntent(SystemCommandId.VIEW_TODAY_HABITS)

            // Physical Development
            "recovery" in lower || "fatigue" in lower || "ready to train" in lower ->
                buildIntent(SystemCommandId.GET_RECOVERY_STATUS)
            ("start" in lower || "begin" in lower) && ("jog" in lower || "run" in lower) ->
                buildIntent(
                    SystemCommandId.START_JOGGING,
                    params = extractedDuration?.let { mapOf("durationMinutes" to it) } ?: emptyMap()
                )
            ("start" in lower || "begin" in lower) && ("workout" in lower || "exercise" in lower || "training" in lower) ->
                buildIntent(SystemCommandId.START_WORKOUT)
            "workout plan" in lower || "today s workout" in lower ->
                buildIntent(SystemCommandId.VIEW_WORKOUT_PLAN)
            "fitness" in lower || "weekly workout" in lower || "physical progress" in lower ->
                buildIntent(SystemCommandId.GET_FITNESS_PROGRESS)

            // Academic Development (other)
            ("start" in lower || "begin" in lower || "open" in lower) && ("study" in lower || "focus" in lower || "pomodoro" in lower) ->
                buildIntent(
                    SystemCommandId.START_STUDY_SESSION,
                    params = extractedDuration?.let { mapOf("durationMinutes" to it) } ?: emptyMap()
                )
            ("hours" in lower || "time" in lower || "how much" in lower) && "study" in lower ->
                buildIntent(SystemCommandId.GET_WEEKLY_STUDY_HOURS)
            "academic" in lower || "study progress" in lower || "revision" in lower || "syllabus" in lower ->
                buildIntent(SystemCommandId.GET_ACADEMIC_PROGRESS)

            // Missions & RPG
            "shadow" in lower || "monarch" in lower ->
                buildIntent(SystemCommandId.GET_SHADOW_ARMY_STATUS)
            "dungeon" in lower || "gate" in lower || "boss" in lower ->
                buildIntent(SystemCommandId.GET_DUNGEON_PROGRESS)
            "achievement" in lower ->
                buildIntent(SystemCommandId.GET_LATEST_ACHIEVEMENT)
            "pending quest" in lower ->
                buildIntent(SystemCommandId.VIEW_PENDING_QUESTS)
            "quest" in lower || "mission" in lower ->
                buildIntent(SystemCommandId.VIEW_ACTIVE_MISSIONS)

            // Reports & Player Information
            "weekly report" in lower || "daily report" in lower || "system report" in lower ->
                buildIntent(SystemCommandId.AI_GENERATE_WEEKLY_REPORT)
            "attribute" in lower || "stats" in lower ->
                buildIntent(SystemCommandId.GET_PLAYER_ATTRIBUTES)
            "xp" in lower || "experience" in lower ->
                buildIntent(SystemCommandId.GET_PLAYER_XP)
            "level" in lower || "rank" in lower ->
                buildIntent(SystemCommandId.GET_PLAYER_LEVEL_RANK)
            "summarize my progress" in lower || "daily progress" in lower || "how am i doing" in lower ->
                buildIntent(SystemCommandId.GET_DAILY_PROGRESS)

            // Conversational questions routed to AI Conversation
            lower.startsWith("why ") || lower.startsWith("how ") || lower.startsWith("what ") ||
                lower.startsWith("should ") || lower.startsWith("analyze") || lower.startsWith("advise") ||
                lower.startsWith("help me") || lower.startsWith("suggest") ->
                buildIntent(SystemCommandId.AI_CONVERSATION_QUERY, params = mapOf("query" to clean), confidence = 0.78)

            else -> StructuredVoiceIntent(
                commandId = null,
                category = null,
                confidence = 0.25,
                requiresClarification = true,
                clarificationQuestion = "I didn't recognize '$clean' as a System command. Try 'What is my rank?', 'Add a task to review notes', 'Start a 45 minute study session', or switch to Conversation Mode.",
                source = IntentResolutionSource.OFFLINE_LOCAL,
                rawTranscript = rawTranscript
            )
        }
    }

    companion object {
        private const val MIN_EXECUTION_CONFIDENCE = 0.55

        private val UNSAFE_MUTATION_REGEX = Regex(
            "(?i)\\b(grant|cheat|hack|infinite|unlock all|modify xp|set xp|add\\s+(\\d+\\s+)?xp|bonus xp|" +
                "boost\\s+(strength|vitality|agility|intelligence|discipline|wisdom|charisma|stats?|attributes?)|" +
                "set\\s+(my\\s+)?(level|rank)|bypass recovery|override safety|delete database|wipe all)\\b"
        )

        fun isUnauthorizedMutationOrInjection(text: String): Boolean {
            val lower = text.lowercase(Locale.US)
            return PromptSanitizer.containsPromptInjection(text) ||
                UNSAFE_MUTATION_REGEX.containsMatchIn(lower) ||
                ("give me" in lower && ("xp" in lower || "level" in lower || "rank" in lower || "shadow" in lower))
        }

        private const val INTENT_SYSTEM_PROMPT =
            "You are the SOLO LEVELING Reality OS Voice Intent Parser. " +
                "Map the player's spoken command strictly to ONE allowlisted commandId from the catalog below, OR 'CONVERSATION' for open-ended coaching/Q&A, OR 'AMBIGUOUS' when unclear, OR 'UNKNOWN' if unsupported. " +
                "NEVER invent unregistered command IDs. NEVER output commands that mutate XP, rank, attributes, or bypass recovery safety. " +
                "Extract clean parameter values into parametersJson (JSON string). If a required parameter is missing, set requiresClarification=true and provide a short clarificationQuestion."
    }
}
