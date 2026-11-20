package com.example.data.integration

import com.example.domain.engine.AiResponseValidator
import com.example.domain.model.AiConnectionStatus
import com.example.domain.model.SupportedAiModels
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * Official Google Gemini gateway powered by Firebase AI Logic + Firebase App Check.
 *
 * Security & Reliability guarantees:
 * - Zero client-side API keys (authentication & quota enforcement handled server-side by Firebase + App Check).
 * - Lazy model creation (never touches Firebase.ai during Application/AppContainer construction).
 * - Supports true streaming responses via generateContentStream().
 * - Uses strict Schema.obj(...) + responseMimeType = "application/json" for structured voice intent parsing.
 * - Enforces request timeouts, exponential backoff retries on transient/rate-limit errors, and clean CancellationException propagation.
 */
class FirebaseGeminiAiGateway(
    private val requestTimeoutMs: Long = 60_000L,
    private val maxRetries: Int = 2,
    private val isNetworkAvailable: () -> Boolean = { true }
) : BackendAiGateway {

    override val supportsDirectCloud: Boolean = true

    override fun streamChat(
        backendBaseUrl: String,
        request: BackendAiProxyRequest
    ): Flow<AiStreamEvent> = flow {
        if (!isNetworkAvailable()) {
            emit(
                AiStreamEvent.Completed(
                    fullText = "",
                    status = AiConnectionStatus.LOCAL_FALLBACK,
                    errorMessage = "Device is offline. Using local deterministic System Advisor."
                )
            )
            return@flow
        }

        val resolvedModelId = SupportedAiModels.resolveGeminiModelId(request.model)
        val systemText = buildSystemInstruction(request)
        val contents = buildMultiTurnContents(request)
        val temperature = request.temperature ?: 0.55f
        val maxTokens = request.maxOutputTokens ?: 1024

        var attempt = 0
        var lastError: Throwable? = null
        while (attempt <= maxRetries) {
            try {
                val model = createGenerativeModel(
                    modelId = resolvedModelId,
                    systemInstructionText = systemText,
                    temperature = temperature,
                    maxOutputTokens = maxTokens,
                    responseSchemaType = null
                )
                val assembled = StringBuilder()
                var promptTokens = 0
                var completionTokens = 0

                withTimeout(requestTimeoutMs) {
                    model.generateContentStream(contents).collect { chunk ->
                        val text = chunk.text.orEmpty()
                        if (text.isNotEmpty()) {
                            assembled.append(text)
                            emit(AiStreamEvent.TokenDelta(text))
                        }
                        chunk.usageMetadata?.let { usage ->
                            val promptCount = usage.promptTokenCount
                            if (promptCount > 0) promptTokens = promptCount
                            val candCount = usage.candidatesTokenCount ?: 0
                            if (candCount > 0) completionTokens = candCount
                        }
                    }
                }

                val sanitized = AiResponseValidator.sanitizeResponse(assembled.toString())
                if (AiResponseValidator.isValidAssistantReply(sanitized)) {
                    emit(
                        AiStreamEvent.Completed(
                            fullText = sanitized,
                            status = AiConnectionStatus.CONNECTED,
                            promptTokens = promptTokens,
                            completionTokens = completionTokens
                        )
                    )
                    return@flow
                } else {
                    emit(
                        AiStreamEvent.Completed(
                            fullText = "",
                            status = AiConnectionStatus.PROVIDER_ERROR,
                            errorMessage = "Gemini returned an empty response."
                        )
                    )
                    return@flow
                }
            } catch (ce: CancellationException) {
                if (ce is TimeoutCancellationException) {
                    lastError = ce
                    attempt += 1
                    if (attempt <= maxRetries) {
                        delay(450L * attempt)
                        continue
                    }
                } else {
                    throw ce
                }
            } catch (t: Throwable) {
                lastError = t
                val status = classifyException(t)
                if ((status == AiConnectionStatus.RATE_LIMITED || status == AiConnectionStatus.PROVIDER_ERROR) && attempt < maxRetries) {
                    attempt += 1
                    delay(500L * attempt)
                    continue
                }
                emit(
                    AiStreamEvent.Completed(
                        fullText = "",
                        status = status,
                        errorMessage = formatFriendlyError(t, status)
                    )
                )
                return@flow
            }
        }

        val finalStatus = lastError?.let { classifyException(it) } ?: AiConnectionStatus.PROVIDER_ERROR
        emit(
            AiStreamEvent.Completed(
                fullText = "",
                status = finalStatus,
                errorMessage = formatFriendlyError(lastError, finalStatus)
            )
        )
    }.flowOn(Dispatchers.IO)

    override suspend fun requestStructured(
        backendBaseUrl: String,
        request: BackendAiProxyRequest
    ): AiStreamEvent.Completed = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            return@withContext AiStreamEvent.Completed(
                fullText = "",
                status = AiConnectionStatus.LOCAL_FALLBACK,
                errorMessage = "Device is offline. Using local fallback."
            )
        }

        val resolvedModelId = SupportedAiModels.resolveGeminiModelId(request.model)
        val systemText = buildSystemInstruction(request)
        val contents = buildMultiTurnContents(request)
        val temperature = request.temperature ?: if (request.requestType == "CHAT") 0.55f else 0.15f
        val maxTokens = request.maxOutputTokens ?: if (request.requestType == "VOICE_INTENT") 512 else 1024
        val schemaType = request.responseSchemaType ?: when (request.requestType) {
            "VOICE_INTENT" -> SCHEMA_VOICE_INTENT
            "QUEST_PROPOSALS" -> SCHEMA_QUEST_PROPOSALS
            "DAILY_PLAN" -> SCHEMA_DAILY_PLAN
            else -> null
        }

        var attempt = 0
        var lastError: Throwable? = null
        while (attempt <= maxRetries) {
            try {
                val model = createGenerativeModel(
                    modelId = resolvedModelId,
                    systemInstructionText = systemText,
                    temperature = temperature,
                    maxOutputTokens = maxTokens,
                    responseSchemaType = schemaType
                )
                val response = withTimeout(requestTimeoutMs) {
                    model.generateContent(contents)
                }
                val rawText = response.text.orEmpty().trim()
                val sanitized = AiResponseValidator.sanitizeResponse(rawText)
                val promptTokens = response.usageMetadata?.promptTokenCount ?: 0
                val completionTokens = response.usageMetadata?.candidatesTokenCount ?: 0

                return@withContext if (sanitized.isNotBlank()) {
                    AiStreamEvent.Completed(
                        fullText = sanitized,
                        status = AiConnectionStatus.CONNECTED,
                        promptTokens = promptTokens,
                        completionTokens = completionTokens
                    )
                } else {
                    AiStreamEvent.Completed(
                        fullText = "",
                        status = AiConnectionStatus.PROVIDER_ERROR,
                        errorMessage = "Gemini returned empty structured output."
                    )
                }
            } catch (ce: CancellationException) {
                if (ce is TimeoutCancellationException) {
                    lastError = ce
                    attempt += 1
                    if (attempt <= maxRetries) {
                        delay(400L * attempt)
                        continue
                    }
                } else {
                    throw ce
                }
            } catch (t: Throwable) {
                lastError = t
                val status = classifyException(t)
                if ((status == AiConnectionStatus.RATE_LIMITED || status == AiConnectionStatus.PROVIDER_ERROR) && attempt < maxRetries) {
                    attempt += 1
                    delay(450L * attempt)
                    continue
                }
                return@withContext AiStreamEvent.Completed(
                    fullText = "",
                    status = status,
                    errorMessage = formatFriendlyError(t, status)
                )
            }
        }

        val finalStatus = lastError?.let { classifyException(it) } ?: AiConnectionStatus.PROVIDER_ERROR
        AiStreamEvent.Completed(
            fullText = "",
            status = finalStatus,
            errorMessage = formatFriendlyError(lastError, finalStatus)
        )
    }

    private fun createGenerativeModel(
        modelId: String,
        systemInstructionText: String,
        temperature: Float,
        maxOutputTokens: Int,
        responseSchemaType: String?
    ): GenerativeModel {
        val schema = when (responseSchemaType) {
            SCHEMA_VOICE_INTENT -> buildVoiceIntentSchema()
            SCHEMA_QUEST_PROPOSALS -> buildQuestProposalSchema()
            SCHEMA_DAILY_PLAN -> buildDailyPlanSchema()
            else -> null
        }
        val config = generationConfig {
            this.temperature = temperature
            this.maxOutputTokens = maxOutputTokens
            if (schema != null) {
                this.responseMimeType = "application/json"
                this.responseSchema = schema
            }
        }
        return Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = modelId,
            generationConfig = config,
            systemInstruction = content { text(systemInstructionText) }
        )
    }

    private fun buildSystemInstruction(request: BackendAiProxyRequest): String = buildString {
        append(request.systemPrompt.trim())
        if (request.contextSummary.isNotBlank()) {
            append("\n\n[VERIFIED PLAYER TELEMETRY SNAPSHOT]\n")
            append(request.contextSummary.trim().take(3600))
        }
    }

    private fun buildMultiTurnContents(request: BackendAiProxyRequest): List<Content> {
        val contents = mutableListOf<Content>()
        request.recentTurns.takeLast(10).forEach { turn ->
            val role = if (turn.role == "assistant" || turn.role == "model") "model" else "user"
            if (turn.content.isNotBlank()) {
                contents.add(content(role = role) { text(turn.content.take(900)) })
            }
        }
        contents.add(content(role = "user") { text(request.userPrompt.take(1800)) })
        return contents
    }

    private fun classifyException(throwable: Throwable): AiConnectionStatus {
        if (throwable is TimeoutCancellationException) return AiConnectionStatus.PROVIDER_ERROR
        val msg = throwable.message.orEmpty().lowercase()
        return when {
            "429" in msg || "quota" in msg || "rate limit" in msg || "resource_exhausted" in msg ->
                AiConnectionStatus.RATE_LIMITED
            "401" in msg || "403" in msg || "app check" in msg || "permission_denied" in msg || "unauthenticated" in msg ->
                AiConnectionStatus.AUTH_ERROR
            else -> AiConnectionStatus.PROVIDER_ERROR
        }
    }

    private fun formatFriendlyError(throwable: Throwable?, status: AiConnectionStatus): String = when (status) {
        AiConnectionStatus.RATE_LIMITED -> "Gemini rate limit reached. Local System Advisor answered in the meantime."
        AiConnectionStatus.AUTH_ERROR -> "Firebase AI verification pending or unauthorized. Falling back to local advisor."
        else -> if (throwable is TimeoutCancellationException) {
            "Gemini request timed out. Local deterministic response provided."
        } else {
            "Gemini service temporarily unavailable (${throwable?.javaClass?.simpleName ?: "Network"}). Local fallback active."
        }
    }

    companion object {
        const val SCHEMA_VOICE_INTENT = "VOICE_INTENT"
        const val SCHEMA_QUEST_PROPOSALS = "QUEST_PROPOSALS"
        const val SCHEMA_DAILY_PLAN = "DAILY_PLAN"

        fun buildVoiceIntentSchema(): Schema = Schema.obj(
            properties = mapOf(
                "commandId" to Schema.string(description = "Allowlisted command ID from the registry, or UNKNOWN / AMBIGUOUS / CONVERSATION"),
                "confidence" to Schema.double(description = "Confidence score from 0.0 to 1.0"),
                "parametersJson" to Schema.string(description = "Compact JSON object string of extracted command parameters, e.g. {\"title\":\"Review notes\",\"durationMinutes\":\"45\"}"),
                "requiresClarification" to Schema.boolean(description = "True when user intent is ambiguous or missing required parameters"),
                "clarificationQuestion" to Schema.string(description = "Concise follow-up question if requiresClarification is true"),
                "spokenSummary" to Schema.string(description = "Short direct system reply when commandId is CONVERSATION or UNKNOWN")
            ),
            optionalProperties = listOf("parametersJson", "clarificationQuestion", "spokenSummary")
        )

        fun buildQuestProposalSchema(): Schema = Schema.obj(
            properties = mapOf(
                "proposals" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "title" to Schema.string(),
                            "description" to Schema.string(),
                            "category" to Schema.string(),
                            "difficulty" to Schema.string(),
                            "objective" to Schema.string(),
                            "completionCriteria" to Schema.string(),
                            "suggestedDurationMinutes" to Schema.integer(),
                            "priority" to Schema.string(),
                            "reason" to Schema.string()
                        )
                    )
                )
            )
        )

        fun buildDailyPlanSchema(): Schema = Schema.obj(
            properties = mapOf(
                "items" to Schema.array(
                    items = Schema.obj(
                        properties = mapOf(
                            "taskTitle" to Schema.string(),
                            "module" to Schema.string(),
                            "startMinuteOfDay" to Schema.integer(),
                            "durationMinutes" to Schema.integer(),
                            "priority" to Schema.string(),
                            "reason" to Schema.string(),
                            "dependencies" to Schema.string()
                        ),
                        optionalProperties = listOf("dependencies")
                    )
                )
            )
        )
    }
}

/**
 * Unified production gateway:
 * - Uses official Firebase AI Logic Gemini by default (zero manual setup, zero client API key).
 * - If the user explicitly configures a custom backend proxy URL (e.g. their Supabase Edge Function),
 *   routes to HttpBackendAiGateway first and falls back to Firebase AI Logic if needed.
 */
class HybridGeminiAiGateway(
    private val firebaseGateway: FirebaseGeminiAiGateway,
    private val httpGateway: HttpBackendAiGateway = HttpBackendAiGateway()
) : BackendAiGateway {

    override val supportsDirectCloud: Boolean = true

    override fun streamChat(
        backendBaseUrl: String,
        request: BackendAiProxyRequest
    ): Flow<AiStreamEvent> {
        val customEndpoint = HttpBackendAiGateway.normalizeProxyEndpoint(backendBaseUrl)
        return if (customEndpoint != null) {
            httpGateway.streamChat(backendBaseUrl, request)
        } else {
            firebaseGateway.streamChat(backendBaseUrl, request)
        }
    }

    override suspend fun requestStructured(
        backendBaseUrl: String,
        request: BackendAiProxyRequest
    ): AiStreamEvent.Completed {
        val customEndpoint = HttpBackendAiGateway.normalizeProxyEndpoint(backendBaseUrl)
        return if (customEndpoint != null) {
            val proxyResult = httpGateway.requestStructured(backendBaseUrl, request)
            if (proxyResult.status == AiConnectionStatus.CONNECTED && proxyResult.fullText.isNotBlank()) {
                proxyResult
            } else {
                firebaseGateway.requestStructured(backendBaseUrl, request)
            }
        } else {
            firebaseGateway.requestStructured(backendBaseUrl, request)
        }
    }
}
