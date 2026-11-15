package com.example.data.integration

import com.example.domain.model.AiChatMessage
import com.example.domain.model.AiChatRole
import com.example.domain.model.AiConnectionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

@Serializable
data class BackendAiProxyRequest(
    val model: String,
    val requestType: String, // "CHAT", "QUEST_PROPOSALS", "DAILY_PLAN"
    val systemPrompt: String,
    val contextSummary: String,
    val recentTurns: List<OpenRouterMessage> = emptyList(),
    val userPrompt: String,
    val stream: Boolean = false
)

@Serializable
data class BackendAiProxyResponse(
    val content: String = "",
    val modelUsed: String = "",
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val error: String? = null
)

@Serializable
private data class StreamChunkDelta(
    val choices: List<StreamChoice> = emptyList()
)

@Serializable
private data class StreamChoice(
    val delta: StreamDeltaContent? = null,
    val message: OpenRouterMessage? = null
)

@Serializable
private data class StreamDeltaContent(
    val content: String? = null
)

sealed interface AiStreamEvent {
    data class TokenDelta(val delta: String) : AiStreamEvent
    data class Completed(
        val fullText: String,
        val status: AiConnectionStatus,
        val promptTokens: Int = 0,
        val completionTokens: Int = 0,
        val errorMessage: String? = null
    ) : AiStreamEvent
}

interface BackendAiGateway {
    fun streamChat(
        backendBaseUrl: String,
        request: BackendAiProxyRequest
    ): Flow<AiStreamEvent>

    suspend fun requestStructured(
        backendBaseUrl: String,
        request: BackendAiProxyRequest
    ): AiStreamEvent.Completed
}

/**
 * Secure Android client boundary for the Supabase Edge Function / Backend OpenRouter proxy.
 * CRITICAL SECURITY INVARIANT: This class NEVER accepts, stores, reads, or transmits an OpenRouter API key.
 * All OpenRouter credentials live strictly in server-side backend environment variables.
 */
class HttpBackendAiGateway(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
) : BackendAiGateway {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    override fun streamChat(
        backendBaseUrl: String,
        request: BackendAiProxyRequest
    ): Flow<AiStreamEvent> = flow {
        val url = normalizeProxyEndpoint(backendBaseUrl)
        if (url == null) {
            emit(
                AiStreamEvent.Completed(
                    fullText = "",
                    status = AiConnectionStatus.BACKEND_UNCONFIGURED,
                    errorMessage = "Configure your Supabase Edge Function or Backend AI Proxy URL in AI Settings to enable cloud OpenRouter calls."
                )
            )
            return@flow
        }

        val httpRequest = buildPostRequest(url, request.copy(stream = true))
        val result = runCatching {
            client.newCall(httpRequest).execute().use { response ->
                val code = response.code
                if (!response.isSuccessful) {
                    val mappedStatus = mapHttpStatus(code)
                    val errBody = response.body?.string()?.take(200).orEmpty()
                    return@use AiStreamEvent.Completed(
                        fullText = "",
                        status = mappedStatus,
                        errorMessage = "Backend HTTP $code${if (errBody.isNotBlank()) ": $errBody" else ""}"
                    )
                }

                val body = response.body ?: return@use AiStreamEvent.Completed(
                    fullText = "",
                    status = AiConnectionStatus.PROVIDER_ERROR,
                    errorMessage = "Empty response body from backend proxy."
                )

                val contentType = response.header("Content-Type").orEmpty()
                if (contentType.contains("text/event-stream", ignoreCase = true)) {
                    val assembled = StringBuilder()
                    BufferedReader(InputStreamReader(body.byteStream())).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            val current = line?.trim().orEmpty()
                            if (!current.startsWith("data:")) continue
                            val payload = current.removePrefix("data:").trim()
                            if (payload == "[DONE]" || payload.isBlank()) continue

                            // Support either backend-simplified {"delta":"..."} or OpenRouter SSE chunk
                            val deltaText = runCatching {
                                val chunk = json.decodeFromString<StreamChunkDelta>(payload)
                                chunk.choices.firstOrNull()?.delta?.content
                                    ?: chunk.choices.firstOrNull()?.message?.content
                            }.getOrNull()
                            if (!deltaText.isNullOrEmpty()) {
                                assembled.append(deltaText)
                                emit(AiStreamEvent.TokenDelta(deltaText))
                            }
                        }
                    }
                    AiStreamEvent.Completed(
                        fullText = assembled.toString(),
                        status = AiConnectionStatus.CONNECTED
                    )
                } else {
                    val raw = body.string()
                    parseCompletedJson(raw)
                }
            }
        }.getOrElse { throwable ->
            AiStreamEvent.Completed(
                fullText = "",
                status = AiConnectionStatus.PROVIDER_ERROR,
                errorMessage = if (throwable is SocketTimeoutException) {
                    "Backend AI request timed out."
                } else {
                    throwable.message ?: "Network error contacting AI backend."
                }
            )
        }
        emit(result)
    }.flowOn(Dispatchers.IO)

    override suspend fun requestStructured(
        backendBaseUrl: String,
        request: BackendAiProxyRequest
    ): AiStreamEvent.Completed {
        var finalEvent = AiStreamEvent.Completed("", AiConnectionStatus.BACKEND_UNCONFIGURED)
        val url = normalizeProxyEndpoint(backendBaseUrl) ?: return finalEvent.copy(
            errorMessage = "Backend AI Proxy URL is not configured."
        )
        val httpRequest = buildPostRequest(url, request.copy(stream = false))
        finalEvent = kotlinx.coroutines.withContext(Dispatchers.IO) {
            runCatching {
                client.newCall(httpRequest).execute().use { response ->
                    if (!response.isSuccessful) {
                        val mappedStatus = mapHttpStatus(response.code)
                        val bodyMsg = response.body?.string()?.take(200).orEmpty()
                        return@use AiStreamEvent.Completed(
                            fullText = "",
                            status = mappedStatus,
                            errorMessage = "Backend HTTP ${response.code}${if (bodyMsg.isNotBlank()) ": $bodyMsg" else ""}"
                        )
                    }
                    val raw = response.body?.string().orEmpty()
                    parseCompletedJson(raw)
                }
            }.getOrElse { throwable ->
                AiStreamEvent.Completed(
                    fullText = "",
                    status = AiConnectionStatus.PROVIDER_ERROR,
                    errorMessage = throwable.message ?: "Request failed."
                )
            }
        }
        return finalEvent
    }

    private fun buildPostRequest(endpointUrl: String, payload: BackendAiProxyRequest): Request {
        val bodyStr = json.encodeToString(payload)
        return Request.Builder()
            .url(endpointUrl)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json, text/event-stream")
            .post(bodyStr.toRequestBody(mediaType))
            .build()
    }

    private fun parseCompletedJson(raw: String): AiStreamEvent.Completed {
        val direct = runCatching { json.decodeFromString<BackendAiProxyResponse>(raw) }.getOrNull()
        if (direct != null && direct.content.isNotBlank()) {
            return AiStreamEvent.Completed(
                fullText = direct.content,
                status = AiConnectionStatus.CONNECTED,
                promptTokens = direct.promptTokens,
                completionTokens = direct.completionTokens
            )
        }
        val openRouter = runCatching { json.decodeFromString<OpenRouterChatResponse>(raw) }.getOrNull()
        val msg = openRouter?.choices?.firstOrNull()?.message?.content.orEmpty()
        return if (msg.isNotBlank()) {
            AiStreamEvent.Completed(fullText = msg, status = AiConnectionStatus.CONNECTED)
        } else {
            AiStreamEvent.Completed(
                fullText = "",
                status = AiConnectionStatus.PROVIDER_ERROR,
                errorMessage = direct?.error ?: "Malformed or empty JSON payload from AI backend."
            )
        }
    }

    private fun mapHttpStatus(code: Int): AiConnectionStatus = when (code) {
        401, 403 -> AiConnectionStatus.AUTH_ERROR
        429 -> AiConnectionStatus.RATE_LIMITED
        else -> AiConnectionStatus.PROVIDER_ERROR
    }

    companion object {
        fun normalizeProxyEndpoint(rawUrl: String): String? {
            val trimmed = rawUrl.trim()
            if (trimmed.isBlank()) return null
            val isAllowedScheme = trimmed.startsWith("https://", ignoreCase = true) ||
                trimmed.startsWith("http://localhost", ignoreCase = true) ||
                trimmed.startsWith("http://10.0.2.2", ignoreCase = true) ||
                trimmed.startsWith("http://127.0.0.1", ignoreCase = true)
            if (!isAllowedScheme) return null
            return if (trimmed.endsWith("/ai-game-master") || trimmed.endsWith("/chat")) {
                trimmed
            } else {
                trimmed.trimEnd('/') + "/functions/v1/ai-game-master"
            }
        }

        fun buildBoundedTurns(history: List<AiChatMessage>, maxTurns: Int = 6): List<OpenRouterMessage> {
            return history
                .filter { it.role != AiChatRole.SYSTEM }
                .takeLast(maxTurns)
                .map { OpenRouterMessage(it.role.wireValue, it.content.take(500)) }
        }
    }
}
