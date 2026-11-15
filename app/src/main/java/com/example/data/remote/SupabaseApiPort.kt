package com.example.data.remote

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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
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

interface SupabaseApiPort {
    fun isConfigured(): Boolean
    fun buildOAuthAuthorizeUrl(provider: String, redirectUri: String): String?

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
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()
) : SupabaseApiPort {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true }
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    override fun isConfigured(): Boolean {
        val cleanUrl = supabaseUrl.trim().trimEnd('/')
        val cleanKey = anonKey.trim()
        return cleanUrl.startsWith("https://") &&
            !cleanUrl.contains("YOUR_PROJECT_REF") &&
            cleanKey.isNotEmpty() &&
            !cleanKey.contains("YOUR_SUPABASE_ANON_KEY")
    }

    override fun buildOAuthAuthorizeUrl(provider: String, redirectUri: String): String? {
        if (!isConfigured()) return null
        val encodedRedirect = URLEncoder.encode(redirectUri, "UTF-8")
        return "${baseUrl()}/auth/v1/authorize?provider=$provider&redirect_to=$encodedRedirect"
    }

    private fun baseUrl(): String = supabaseUrl.trim().trimEnd('/')

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
        url = "${baseUrl()}/auth/v1/signup",
        accessToken = null,
        bodyJson = buildJsonObject {
            put("email", email.trim())
            put("password", password)
            put("data", buildJsonObject { put("display_name", displayName.trim()) })
        }.toString()
    ) { raw -> json.decodeFromString(raw) }

    override suspend fun signIn(
        email: String,
        password: String
    ): CloudCallResult<SupabaseAuthResponse> = executeJson(
        method = "POST",
        url = "${baseUrl()}/auth/v1/token?grant_type=password",
        accessToken = null,
        bodyJson = buildJsonObject {
            put("email", email.trim())
            put("password", password)
        }.toString()
    ) { raw -> json.decodeFromString(raw) }

    override suspend fun refreshSession(refreshToken: String): CloudCallResult<SupabaseAuthResponse> =
        executeJson(
            method = "POST",
            url = "${baseUrl()}/auth/v1/token?grant_type=refresh_token",
            accessToken = null,
            bodyJson = buildJsonObject { put("refresh_token", refreshToken) }.toString()
        ) { raw -> json.decodeFromString(raw) }

    override suspend fun resendVerificationEmail(email: String): CloudCallResult<Unit> = executeJson(
        method = "POST",
        url = "${baseUrl()}/auth/v1/resend",
        accessToken = null,
        bodyJson = buildJsonObject {
            put("type", "signup")
            put("email", email.trim())
        }.toString()
    ) { }

    override suspend fun sendPasswordRecovery(email: String): CloudCallResult<Unit> = executeJson(
        method = "POST",
        url = "${baseUrl()}/auth/v1/recover",
        accessToken = null,
        bodyJson = buildJsonObject { put("email", email.trim()) }.toString()
    ) { }

    override suspend fun updatePassword(
        accessToken: String,
        newPassword: String
    ): CloudCallResult<SupabaseUserDto> = executeJson(
        method = "PUT",
        url = "${baseUrl()}/auth/v1/user",
        accessToken = accessToken,
        bodyJson = buildJsonObject { put("password", newPassword) }.toString()
    ) { raw -> json.decodeFromString(raw) }

    override suspend fun fetchUser(accessToken: String): CloudCallResult<SupabaseUserDto> = executeJson(
        method = "GET",
        url = "${baseUrl()}/auth/v1/user",
        accessToken = accessToken,
        bodyJson = null
    ) { raw -> json.decodeFromString(raw) }

    override suspend fun signOut(accessToken: String): CloudCallResult<Unit> = executeJson(
        method = "POST",
        url = "${baseUrl()}/auth/v1/logout",
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
            url = "${baseUrl()}/rest/v1/player_identities?user_id=eq.$userId&select=*",
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
                        url = "${baseUrl()}/rest/v1/player_identities",
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
        url = "${baseUrl()}/rest/v1/player_identities?user_id=eq.$userId",
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
        url = "${baseUrl()}/rest/v1/player_devices?user_id=eq.$userId&order=last_active_at.desc",
        accessToken = accessToken,
        bodyJson = null
    ) { raw -> json.decodeFromString(raw) }

    override suspend fun revokeDevice(
        accessToken: String,
        userId: String,
        deviceId: String
    ): CloudCallResult<Unit> = executeJson(
        method = "PATCH",
        url = "${baseUrl()}/rest/v1/player_devices?id=eq.$deviceId&user_id=eq.$userId",
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
        url = "${baseUrl()}/rest/v1/$tableName",
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
    ): CloudCallResult<List<JsonObject>> {
        val encodedTs = URLEncoder.encode(updatedSinceIso, "UTF-8")
        return executeJson(
            method = "GET",
            url = "${baseUrl()}/rest/v1/$tableName?user_id=eq.$userId&updated_at=gt.$encodedTs&order=updated_at.asc&limit=$limit&offset=$offset",
            accessToken = accessToken,
            bodyJson = null
        ) { raw -> json.decodeFromString(raw) }
    }

    override suspend fun validateProgressionEvents(
        accessToken: String,
        deviceId: String,
        events: List<CloudProgressionEventPayload>
    ): CloudCallResult<ProgressionValidateResponse> = executeJson(
        method = "POST",
        url = "${baseUrl()}/functions/v1/progression-validate",
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
        url = "${baseUrl()}/rest/v1/cloud_backups",
        accessToken = accessToken,
        extraHeaders = mapOf("Prefer" to "resolution=merge-duplicates,return=minimal"),
        bodyJson = json.encodeToString(listOf(backup))
    ) { }

    override suspend fun listCloudBackups(
        accessToken: String,
        userId: String
    ): CloudCallResult<List<RemoteBackupDto>> = executeJson(
        method = "GET",
        url = "${baseUrl()}/rest/v1/cloud_backups?user_id=eq.$userId&order=created_at.desc&limit=20",
        accessToken = accessToken,
        bodyJson = null
    ) { raw -> json.decodeFromString(raw) }

    override suspend fun validateCloudRestore(
        accessToken: String,
        backupId: String
    ): CloudCallResult<Unit> = executeJson(
        method = "POST",
        url = "${baseUrl()}/functions/v1/backup-restore",
        accessToken = accessToken,
        bodyJson = buildJsonObject { put("backupId", backupId) }.toString()
    ) { }

    override suspend fun requestAccountDeletion(
        accessToken: String,
        confirmationPhrase: String
    ): CloudCallResult<Unit> = executeJson(
        method = "POST",
        url = "${baseUrl()}/functions/v1/account-delete",
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

        val builder = Request.Builder()
            .url(url)
            .addHeader("apikey", anonKey.trim())
            .addHeader("Accept", "application/json")
        if (!accessToken.isNullOrBlank()) {
            builder.addHeader("Authorization", "Bearer $accessToken")
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

        return@withContext runCatching {
            httpClient.newCall(builder.build()).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    val safeMsg = AuthValidator.sanitizeLogMessage(
                        extractErrorMessage(raw, resp.code)
                    )
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
            CloudCallResult.Failure(
                code = -1,
                category = "NETWORK_ERROR",
                message = AuthValidator.sanitizeLogMessage(err.message ?: "Network unreachable")
            )
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
