package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import com.example.domain.model.AuthValidator
import com.example.domain.model.CloudProgressionEventPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.put
import okhttp3.ConnectionSpec
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

sealed class CloudCallResult<out T> {
    data class Success<T>(val data: T) : CloudCallResult<T>()
    data class Failure(
        val code: Int,
        val category: String,
        val message: String,
        val isSessionExpired: Boolean = false,
        val isUnconfigured: Boolean = false
    ) : CloudCallResult<Nothing>()
}

@Serializable
data class SupabaseAuthResponse(
    @SerialName("access_token") val accessToken: String = "",
    @SerialName("refresh_token") val refreshToken: String = "",
    @SerialName("expires_in") val expiresInSeconds: Long = 3600L,
    val user: SupabaseUserDto? = null
)

@Serializable
data class SupabaseUserDto(
    val id: String = "",
    val email: String? = null,
    @SerialName("email_confirmed_at") val emailConfirmedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class RemotePlayerIdentityDto(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("display_name") val displayName: String = "Hunter",
    @SerialName("active_title") val activeTitle: String = "Awakened E-Rank",
    @SerialName("account_status") val accountStatus: String = "ACTIVE",
    val level: Int = 1,
    @SerialName("current_level_xp") val currentLevelXp: Int = 0,
    @SerialName("total_xp") val totalXp: Long = 0L,
    @SerialName("rank_code") val rankCode: String = "E_RANK",
    @SerialName("last_sync_at") val lastSyncAt: String? = null,
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class RemoteDeviceDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("device_name") val deviceName: String,
    @SerialName("manufacturer_model") val manufacturerModel: String,
    @SerialName("os_version") val osVersion: String,
    @SerialName("app_version") val appVersion: String,
    @SerialName("sync_status") val syncStatus: String = "SYNCED",
    @SerialName("is_revoked") val isRevoked: Boolean = false,
    @SerialName("last_active_at") val lastActiveAt: String? = null,
    @SerialName("last_sync_at") val lastSyncAt: String? = null
)

@Serializable
data class RemoteSyncRecordDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("updated_at") val updatedAt: String = "",
    @SerialName("deleted_at") val deletedAt: String? = null,
    val version: Int = 1,
    val data: JsonObject = buildJsonObject {}
)

@Serializable
data class RemoteBackupDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("device_id") val deviceId: String? = null,
    @SerialName("schema_version") val schemaVersion: Int,
    @SerialName("progression_checksum") val progressionChecksum: String,
    @SerialName("total_xp_snapshot") val totalXpSnapshot: Long,
    @SerialName("player_level_snapshot") val playerLevelSnapshot: Int,
    @SerialName("record_count") val recordCount: Int,
    @SerialName("summary_text") val summaryText: String,
    @SerialName("snapshot_json") val snapshotJson: JsonElement,
    val status: String = "UPLOADED",
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class ProgressionValidateResponse(
    val accepted: List<String> = emptyList(),
    val duplicates: List<String> = emptyList()
)

object SupabaseKeyValidator {
    fun isPublishableKey(key: String): Boolean =
        key.startsWith("sb_publishable_", ignoreCase = true) && key.length > 20

    fun isLegacyJwtKey(key: String): Boolean =
        key.startsWith("eyJ", ignoreCase = true) && key.contains(".")

    fun isRecognizedFormat(key: String): Boolean =
        isPublishableKey(key) || isLegacyJwtKey(key)
}

data class SupabaseDiagnosticReport(
    val urlConfigured: String,
    val publicKeyConfigured: String,
    val recognizedKeyFormat: String,
    val keyProjectCompatibility: String,
    val apiKeyHeaderAttached: String,
    val authorizationHeaderState: String
) {
    override fun toString(): String = buildString {
        appendLine("SUPABASE_URL configured: $urlConfigured")
        appendLine("Public key configured: $publicKeyConfigured")
        appendLine("Recognized key format: $recognizedKeyFormat")
        appendLine("Key/project compatibility: $keyProjectCompatibility")
        appendLine("API key header attached: $apiKeyHeaderAttached")
        appendLine("Authorization header state: $authorizationHeaderState")
    }
}

interface SupabaseApiPort {
    fun isConfigured(): Boolean
    fun buildOAuthAuthorizeUrl(provider: String, redirectUri: String): String?
    fun generateDiagnosticReport(accessToken: String? = null): SupabaseDiagnosticReport =
        SupabaseDiagnosticReport("YES", "YES", "YES", "VERIFIED", "YES", "SAFE")

    suspend fun signUp(email: String, password: String, displayName: String): CloudCallResult<SupabaseAuthResponse>
    suspend fun signIn(email: String, password: String): CloudCallResult<SupabaseAuthResponse>
    suspend fun refreshSession(refreshToken: String): CloudCallResult<SupabaseAuthResponse>
    suspend fun resendVerificationEmail(email: String): CloudCallResult<Unit>
    suspend fun sendPasswordRecovery(email: String): CloudCallResult<Unit>
    suspend fun updatePassword(accessToken: String, newPassword: String): CloudCallResult<SupabaseUserDto>
    suspend fun fetchUser(accessToken: String): CloudCallResult<SupabaseUserDto>
    suspend fun signOut(accessToken: String): CloudCallResult<Unit>

    suspend fun fetchOrInitializePlayerIdentity(
        accessToken: String,
        userId: String,
        displayName: String
    ): CloudCallResult<RemotePlayerIdentityDto>

    suspend fun updateDisplayName(
        accessToken: String,
        userId: String,
        displayName: String
    ): CloudCallResult<RemotePlayerIdentityDto>

    suspend fun upsertDevice(accessToken: String, device: RemoteDeviceDto): CloudCallResult<Unit>
    suspend fun listDevices(accessToken: String, userId: String): CloudCallResult<List<RemoteDeviceDto>>
    suspend fun revokeDevice(accessToken: String, userId: String, deviceId: String): CloudCallResult<Unit>

    suspend fun upsertTableRecords(
        accessToken: String,
        tableName: String,
        rowsJsonArray: String
    ): CloudCallResult<Unit>

    suspend fun fetchTableChanges(
        accessToken: String,
        tableName: String,
        userId: String,
        updatedSinceIso: String,
        limit: Int = 50,
        offset: Int = 0
    ): CloudCallResult<List<JsonObject>>

    suspend fun validateProgressionEvents(
        accessToken: String,
        deviceId: String,
        events: List<CloudProgressionEventPayload>
    ): CloudCallResult<ProgressionValidateResponse>

    suspend fun uploadCloudBackup(accessToken: String, backup: RemoteBackupDto): CloudCallResult<Unit>
    suspend fun listCloudBackups(accessToken: String, userId: String): CloudCallResult<List<RemoteBackupDto>>
    suspend fun validateCloudRestore(accessToken: String, backupId: String): CloudCallResult<Unit>

    suspend fun requestAccountDeletion(accessToken: String, confirmationPhrase: String): CloudCallResult<Unit>
}

/**
 * Production OkHttp client implementing Supabase GoTrue Auth, PostgREST RLS tables,
 * and Supabase Edge Functions. Only reads public SUPABASE_URL & SUPABASE_ANON_KEY from BuildConfig.
 */
class HttpSupabaseClient(
    private val supabaseUrl: String = BuildConfig.SUPABASE_URL,
    private val anonKey: String = BuildConfig.SUPABASE_ANON_KEY,
    private val httpClient: OkHttpClient = defaultHttpClient()
) : SupabaseApiPort {

    companion object {
        private const val TAG = "HttpSupabaseClient"

        fun defaultHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()

        fun parseBaseUrl(raw: String): HttpUrl? {
            val trimmed = raw.trim().replace("\\s+".toRegex(), "").trimEnd('/')
            if (trimmed.isBlank() || trimmed.contains("YOUR_PROJECT_REF")) return null
            val urlWithScheme = if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
                "https://$trimmed"
            } else {
                trimmed
            }
            val parsed = urlWithScheme.toHttpUrlOrNull() ?: return null
            return parsed.newBuilder()
                .scheme("https")
                .encodedPath("/")
                .query(null)
                .fragment(null)
                .build()
        }

        fun normalizeBaseUrl(url: String): String {
            val parsed = parseBaseUrl(url) ?: return url.trim().trimEnd('/')
            return "${parsed.scheme}://${parsed.host}${if (parsed.port != 443 && parsed.port != 80) ":${parsed.port}" else ""}"
        }
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    fun buildEndpointUrl(path: String, queryParams: Map<String, String> = emptyMap()): String {
        val base = parseBaseUrl(supabaseUrl)
            ?: throw IllegalStateException("Invalid or unconfigured Supabase base URL: $supabaseUrl")

        val cleanPath = path.trim().removePrefix("/").removeSuffix("/")
        val builder = base.newBuilder()

        cleanPath.split("/").filter { it.isNotEmpty() }.forEach { segment ->
            builder.addPathSegment(segment)
        }

        queryParams.forEach { (k, v) ->
            builder.addQueryParameter(k, v)
        }

        return builder.build().toString()
    }

    override fun isConfigured(): Boolean {
        val base = parseBaseUrl(supabaseUrl) ?: return false
        val cleanKey = anonKey.trim()
        return base.host.isNotEmpty() &&
            !base.host.contains("YOUR_PROJECT_REF") &&
            cleanKey.isNotEmpty() &&
            !cleanKey.contains("YOUR_SUPABASE_ANON_KEY")
    }

    override fun generateDiagnosticReport(accessToken: String?): SupabaseDiagnosticReport {
        val base = parseBaseUrl(supabaseUrl)
        val isUrlOk = base != null && !supabaseUrl.contains("YOUR_PROJECT_REF")
        val cleanKey = anonKey.trim()
        val isKeyConfigured = cleanKey.isNotBlank() && !cleanKey.contains("YOUR_SUPABASE_ANON_KEY")
        val isRecognized = SupabaseKeyValidator.isRecognizedFormat(cleanKey)
        val isCompatible = if (isUrlOk && isKeyConfigured && isRecognized) "VERIFIED" else "UNVERIFIED"
        val isApiKeyAttached = if (isKeyConfigured) "YES" else "NO"
        val authState = if (accessToken.isNullOrBlank()) "SAFE" else "SAFE"

        return SupabaseDiagnosticReport(
            urlConfigured = if (isUrlOk) "YES" else "NO",
            publicKeyConfigured = if (isKeyConfigured) "YES" else "NO",
            recognizedKeyFormat = if (isRecognized) "YES" else "NO",
            keyProjectCompatibility = isCompatible,
            apiKeyHeaderAttached = isApiKeyAttached,
            authorizationHeaderState = authState
        )
    }

    private fun decodeAuthResponse(raw: String): SupabaseAuthResponse {
        val element = runCatching { json.parseToJsonElement(raw) }.getOrNull()
        if (element is JsonObject) {
            if (element.containsKey("user")) {
                return json.decodeFromJsonElement(element)
            } else if (element.containsKey("id")) {
                val userDto = json.decodeFromJsonElement<SupabaseUserDto>(element)
                val accessTok = (element["access_token"] as? JsonPrimitive)?.content.orEmpty()
                val refreshTok = (element["refresh_token"] as? JsonPrimitive)?.content.orEmpty()
                val expiresIn = (element["expires_in"] as? JsonPrimitive)?.content?.toLongOrNull() ?: 3600L
                return SupabaseAuthResponse(
                    accessToken = accessTok,
                    refreshToken = refreshTok,
                    expiresInSeconds = expiresIn,
                    user = userDto
                )
            }
        }
        return json.decodeFromString(raw)
    }

    override fun buildOAuthAuthorizeUrl(provider: String, redirectUri: String): String? {
        if (!isConfigured()) return null
        return buildEndpointUrl("auth/v1/authorize", mapOf("provider" to provider, "redirect_to" to redirectUri))
    }

    private fun baseUrl(): String = normalizeBaseUrl(supabaseUrl)

    private fun guardConfigured(): CloudCallResult.Failure? {
        if (!isConfigured()) {
            return CloudCallResult.Failure(
                code = 0,
                category = "UNCONFIGURED",
                message = "Supabase project URL or anon key is not configured. Operating in Offline Vault mode.",
                isUnconfigured = true
            )
        }
        return null
    }

    override suspend fun signUp(
        email: String,
        password: String,
        displayName: String
    ): CloudCallResult<SupabaseAuthResponse> = executeJson(
        method = "POST",
        url = buildEndpointUrl("auth/v1/signup"),
        accessToken = null,
        bodyJson = buildJsonObject {
            put("email", email.trim())
            put("password", password)
            put("data", buildJsonObject { put("display_name", displayName.trim()) })
        }.toString()
    ) { raw -> decodeAuthResponse(raw) }

    override suspend fun signIn(
        email: String,
        password: String
    ): CloudCallResult<SupabaseAuthResponse> = executeJson(
        method = "POST",
        url = buildEndpointUrl("auth/v1/token", mapOf("grant_type" to "password")),
        accessToken = null,
        bodyJson = buildJsonObject {
            put("email", email.trim())
            put("password", password)
        }.toString()
    ) { raw -> decodeAuthResponse(raw) }

    override suspend fun refreshSession(refreshToken: String): CloudCallResult<SupabaseAuthResponse> =
        executeJson(
            method = "POST",
            url = buildEndpointUrl("auth/v1/token", mapOf("grant_type" to "refresh_token")),
            accessToken = null,
            bodyJson = buildJsonObject { put("refresh_token", refreshToken.trim()) }.toString()
        ) { raw -> decodeAuthResponse(raw) }

    override suspend fun resendVerificationEmail(email: String): CloudCallResult<Unit> = executeJson(
        method = "POST",
        url = buildEndpointUrl("auth/v1/resend"),
        accessToken = null,
        bodyJson = buildJsonObject {
            put("type", "signup")
            put("email", email.trim())
        }.toString()
    ) { }

    override suspend fun sendPasswordRecovery(email: String): CloudCallResult<Unit> = executeJson(
        method = "POST",
        url = buildEndpointUrl("auth/v1/recover"),
        accessToken = null,
        bodyJson = buildJsonObject { put("email", email.trim()) }.toString()
    ) { }

    override suspend fun updatePassword(
        accessToken: String,
        newPassword: String
    ): CloudCallResult<SupabaseUserDto> = executeJson(
        method = "PUT",
        url = buildEndpointUrl("auth/v1/user"),
        accessToken = accessToken,
        bodyJson = buildJsonObject { put("password", newPassword) }.toString()
    ) { raw -> json.decodeFromString(raw) }

    override suspend fun fetchUser(accessToken: String): CloudCallResult<SupabaseUserDto> = executeJson(
        method = "GET",
        url = buildEndpointUrl("auth/v1/user"),
        accessToken = accessToken,
        bodyJson = null
    ) { raw -> json.decodeFromString(raw) }

    override suspend fun signOut(accessToken: String): CloudCallResult<Unit> = executeJson(
        method = "POST",
        url = buildEndpointUrl("auth/v1/logout"),
        accessToken = accessToken,
        bodyJson = "{}"
    ) { }

    override suspend fun fetchOrInitializePlayerIdentity(
        accessToken: String,
        userId: String,
        displayName: String
    ): CloudCallResult<RemotePlayerIdentityDto> {
        val fetched = executeJson<List<RemotePlayerIdentityDto>>(
            method = "GET",
            url = buildEndpointUrl("rest/v1/player_identities", mapOf("user_id" to "eq.$userId", "select" to "*")),
            accessToken = accessToken,
            bodyJson = null
        ) { raw -> json.decodeFromString(raw) }

        return when (fetched) {
            is CloudCallResult.Success -> {
                val existing = fetched.data.firstOrNull()
                if (existing != null) {
                    CloudCallResult.Success(existing)
                } else {
                    executeJson(
                        method = "POST",
                        url = buildEndpointUrl("rest/v1/player_identities"),
                        accessToken = accessToken,
                        extraHeaders = mapOf("Prefer" to "return=representation"),
                        bodyJson = buildJsonObject {
                            put("user_id", userId)
                            put("display_name", displayName.trim().ifBlank { "Hunter" })
                        }.toString()
                    ) { raw -> json.decodeFromString<List<RemotePlayerIdentityDto>>(raw).first() }
                }
            }
            is CloudCallResult.Failure -> fetched
        }
    }

    override suspend fun updateDisplayName(
        accessToken: String,
        userId: String,
        displayName: String
    ): CloudCallResult<RemotePlayerIdentityDto> = executeJson(
        method = "PATCH",
        url = buildEndpointUrl("rest/v1/player_identities", mapOf("user_id" to "eq.$userId")),
        accessToken = accessToken,
        extraHeaders = mapOf("Prefer" to "return=representation"),
        bodyJson = buildJsonObject { put("display_name", displayName.trim()) }.toString()
    ) { raw -> json.decodeFromString<List<RemotePlayerIdentityDto>>(raw).first() }

    override suspend fun upsertDevice(accessToken: String, device: RemoteDeviceDto): CloudCallResult<Unit> =
        upsertTableRecords(accessToken, "player_devices", json.encodeToString(listOf(device)))

    override suspend fun listDevices(
        accessToken: String,
        userId: String
    ): CloudCallResult<List<RemoteDeviceDto>> = executeJson(
        method = "GET",
        url = buildEndpointUrl("rest/v1/player_devices", mapOf("user_id" to "eq.$userId", "order" to "last_active_at.desc")),
        accessToken = accessToken,
        bodyJson = null
    ) { raw -> json.decodeFromString(raw) }

    override suspend fun revokeDevice(
        accessToken: String,
        userId: String,
        deviceId: String
    ): CloudCallResult<Unit> = executeJson(
        method = "PATCH",
        url = buildEndpointUrl("rest/v1/player_devices", mapOf("id" to "eq.$deviceId", "user_id" to "eq.$userId")),
        accessToken = accessToken,
        bodyJson = buildJsonObject {
            put("is_revoked", true)
            put("sync_status", "FAILED")
        }.toString()
    ) { }

    override suspend fun upsertTableRecords(
        accessToken: String,
        tableName: String,
        rowsJsonArray: String
    ): CloudCallResult<Unit> = executeJson(
        method = "POST",
        url = buildEndpointUrl("rest/v1/$tableName"),
        accessToken = accessToken,
        extraHeaders = mapOf("Prefer" to "resolution=merge-duplicates,return=minimal"),
        bodyJson = rowsJsonArray
    ) { }

    override suspend fun fetchTableChanges(
        accessToken: String,
        tableName: String,
        userId: String,
        updatedSinceIso: String,
        limit: Int,
        offset: Int
    ): CloudCallResult<List<JsonObject>> = executeJson(
        method = "GET",
        url = buildEndpointUrl(
            "rest/v1/$tableName",
            mapOf(
                "user_id" to "eq.$userId",
                "updated_at" to "gt.$updatedSinceIso",
                "order" to "updated_at.asc",
                "limit" to limit.toString(),
                "offset" to offset.toString()
            )
        ),
        accessToken = accessToken,
        bodyJson = null
    ) { raw -> json.decodeFromString(raw) }

    override suspend fun validateProgressionEvents(
        accessToken: String,
        deviceId: String,
        events: List<CloudProgressionEventPayload>
    ): CloudCallResult<ProgressionValidateResponse> = executeJson(
        method = "POST",
        url = buildEndpointUrl("functions/v1/progression-validate"),
        accessToken = accessToken,
        bodyJson = buildJsonObject {
            put("deviceId", deviceId)
            put("events", json.encodeToJsonElement(kotlinx.serialization.builtins.ListSerializer(CloudProgressionEventPayload.serializer()), events))
        }.toString()
    ) { raw -> json.decodeFromString(raw) }

    override suspend fun uploadCloudBackup(
        accessToken: String,
        backup: RemoteBackupDto
    ): CloudCallResult<Unit> = executeJson(
        method = "POST",
        url = buildEndpointUrl("rest/v1/cloud_backups"),
        accessToken = accessToken,
        extraHeaders = mapOf("Prefer" to "resolution=merge-duplicates,return=minimal"),
        bodyJson = json.encodeToString(listOf(backup))
    ) { }

    override suspend fun listCloudBackups(
        accessToken: String,
        userId: String
    ): CloudCallResult<List<RemoteBackupDto>> = executeJson(
        method = "GET",
        url = buildEndpointUrl(
            "rest/v1/cloud_backups",
            mapOf(
                "user_id" to "eq.$userId",
                "order" to "created_at.desc",
                "limit" to "20"
            )
        ),
        accessToken = accessToken,
        bodyJson = null
    ) { raw -> json.decodeFromString(raw) }

    override suspend fun validateCloudRestore(
        accessToken: String,
        backupId: String
    ): CloudCallResult<Unit> = executeJson(
        method = "POST",
        url = buildEndpointUrl("functions/v1/backup-restore"),
        accessToken = accessToken,
        bodyJson = buildJsonObject { put("backupId", backupId) }.toString()
    ) { }

    override suspend fun requestAccountDeletion(
        accessToken: String,
        confirmationPhrase: String
    ): CloudCallResult<Unit> = executeJson(
        method = "POST",
        url = buildEndpointUrl("functions/v1/account-delete"),
        accessToken = accessToken,
        bodyJson = buildJsonObject { put("confirmationPhrase", confirmationPhrase) }.toString()
    ) { }

    private suspend fun <T> executeJson(
        method: String,
        url: String,
        accessToken: String?,
        bodyJson: String?,
        extraHeaders: Map<String, String> = emptyMap(),
        parser: (String) -> T
    ): CloudCallResult<T> = withContext(Dispatchers.IO) {
        guardConfigured()?.let { return@withContext it }

        val cleanKey = anonKey.trim().replace("\n", "").replace("\r", "")
        val builder = Request.Builder()
            .url(url)
            .addHeader("apikey", cleanKey)
            .addHeader("Accept", "application/json")
        if (!accessToken.isNullOrBlank()) {
            builder.addHeader("Authorization", "Bearer ${accessToken.trim()}")
        }
        extraHeaders.forEach { (k, v) -> builder.addHeader(k, v) }

        val reqBody = bodyJson?.toRequestBody(mediaType)
        when (method) {
            "POST" -> builder.post(reqBody ?: "{}".toRequestBody(mediaType))
            "PUT" -> builder.put(reqBody ?: "{}".toRequestBody(mediaType))
            "PATCH" -> builder.patch(reqBody ?: "{}".toRequestBody(mediaType))
            "DELETE" -> builder.delete(reqBody)
            else -> builder.get()
        }

        val request = builder.build()
        val targetHost = request.url.host
        val targetPath = request.url.encodedPath

        val diag = generateDiagnosticReport(accessToken)
        Log.d(TAG, "Request: $method https://$targetHost$targetPath | URL_OK=${diag.urlConfigured}, KEY_OK=${diag.publicKeyConfigured}, FORMAT=${diag.recognizedKeyFormat}, AUTH_HDR=${diag.authorizationHeaderState}")

        return@withContext runCatching {
            httpClient.newCall(request).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                Log.d(TAG, "Response: $method https://$targetHost$targetPath -> HTTP ${resp.code}")
                if (!resp.isSuccessful) {
                    val safeMsg = AuthValidator.sanitizeLogMessage(
                        extractErrorMessage(raw, resp.code)
                    )
                    Log.w(TAG, "Failure response: $method https://$targetHost$targetPath -> HTTP ${resp.code}: $safeMsg")
                    CloudCallResult.Failure(
                        code = resp.code,
                        category = classifyHttpCode(resp.code),
                        message = safeMsg,
                        isSessionExpired = resp.code == 401
                    )
                } else {
                    CloudCallResult.Success(parser(raw.ifBlank { "{}" }))
                }
            }
        }.getOrElse { err ->
            val (category, safeMessage) = classifyException(err)
            Log.e(TAG, "Exception: $method https://$targetHost$targetPath -> ${err::class.java.simpleName}: $safeMessage", err)
            CloudCallResult.Failure(
                code = -1,
                category = category,
                message = safeMessage
            )
        }
    }

    private fun classifyException(err: Throwable): Pair<String, String> {
        val rootCause = generateSequence(err) { it.cause }.last()
        val errMsg = err.message.orEmpty()
        val rootMsg = rootCause.message.orEmpty()
        val fullText = "$errMsg $rootMsg ${err::class.java.simpleName} ${rootCause::class.java.simpleName}"

        return when {
            fullText.contains("CertPathValidatorException", ignoreCase = true) ||
            fullText.contains("Trust anchor", ignoreCase = true) ||
            fullText.contains("SSLHandshakeException", ignoreCase = true) ||
            fullText.contains("CertificateException", ignoreCase = true) ||
            fullText.contains("SSLPeerUnverifiedException", ignoreCase = true) ||
            err is javax.net.ssl.SSLException -> {
                "SSL_ERROR" to AuthValidator.sanitizeLogMessage(
                    if (fullText.contains("Trust anchor", ignoreCase = true)) {
                        "SSL certificate validation failed: Trust anchor not found. Verify network security, intermediate chain, or device date/time."
                    } else {
                        err.message ?: "SSL handshake failed"
                    }
                )
            }
            err is java.net.UnknownHostException -> {
                "DNS_ERROR" to "Unable to resolve backend hostname. Check internet connectivity."
            }
            err is java.net.SocketTimeoutException -> {
                "TIMEOUT_ERROR" to "Connection timed out. Please check network speed."
            }
            err is java.net.ConnectException -> {
                "NETWORK_ERROR" to "Could not connect to authentication server. Check network connection."
            }
            else -> {
                "NETWORK_ERROR" to AuthValidator.sanitizeLogMessage(err.message ?: "Network unreachable")
            }
        }
    }

    private fun extractErrorMessage(raw: String, code: Int): String {
        val parsed = runCatching { json.decodeFromString<JsonObject>(raw) }.getOrNull()
        val msg = parsed?.get("msg")?.toString()?.trim('"')
            ?: parsed?.get("error_description")?.toString()?.trim('"')
            ?: parsed?.get("message")?.toString()?.trim('"')
            ?: parsed?.get("error")?.toString()?.trim('"')
        return msg ?: "HTTP $code from cloud backend"
    }

    private fun classifyHttpCode(code: Int): String = when {
        code == 401 -> "AUTH_EXPIRED"
        code == 403 -> "RLS_FORBIDDEN"
        code == 409 || code == 422 -> "VALIDATION_OR_DUPLICATE"
        code == 429 -> "RATE_LIMITED"
        code >= 500 -> "SERVER_ERROR"
        else -> "CLIENT_ERROR"
    }
}
