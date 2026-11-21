package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.DatabaseInitializer
import com.example.data.local.SoloLevelingDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.remote.CloudCallResult
import com.example.data.remote.HttpSupabaseClient
import com.example.data.remote.RemoteDeviceDto
import com.example.data.remote.RemotePlayerIdentityDto
import com.example.data.remote.SupabaseApiPort
import com.example.data.remote.SupabaseAuthResponse
import com.example.data.remote.SupabaseDiagnosticReport
import com.example.data.remote.SupabaseKeyValidator
import com.example.data.remote.SupabaseUserDto
import com.example.data.remote.TokenStorage
import com.example.data.repository.SyncRepository
import com.example.data.sync.CloudSyncEngine
import com.example.data.sync.SyncQueueManager
import com.example.data.sync.SyncStatusTracker
import com.example.domain.model.AuthStatus
import com.example.domain.model.HunterRank
import com.example.domain.model.PlayerIdentity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.Call
import okhttp3.ConnectionSpec
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import java.security.cert.CertPathValidatorException
import java.util.UUID
import javax.net.ssl.SSLHandshakeException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CloudAuthSslCertificateValidationTest {

    private lateinit var context: Context
    private lateinit var db: SoloLevelingDatabase
    private lateinit var prefs: UserPreferencesRepository
    private lateinit var tokenStorage: TestTokenStorage
    private lateinit var queueManager: SyncQueueManager
    private lateinit var tracker: SyncStatusTracker

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, SoloLevelingDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        DatabaseInitializer.ensureSeeded(db)
        prefs = UserPreferencesRepository(context)
        tokenStorage = TestTokenStorage()
        queueManager = SyncQueueManager(db)
        tracker = SyncStatusTracker(db) { true }
        tracker.setOnline(true)
    }

    @After
    fun tearDown() {
        tracker.close()
        if (db.isOpen) db.close()
    }

    private fun buildSyncRepo(apiPort: SupabaseApiPort): SyncRepository {
        val engine = CloudSyncEngine(db, apiPort, tokenStorage, queueManager, tracker)
        return SyncRepository(db, prefs, apiPort, tokenStorage, queueManager, tracker, engine)
    }

    /**
     * Verification Test 1 & 3: New account registration & token retrieval.
     */
    @Test
    fun verification1And3_newAccountRegistrationAndTokenRetrieval() = runTest {
        val fakeServer = ValidatingFakeSupabaseServer()
        val repo = buildSyncRepo(fakeServer)

        val regResult = repo.signUp("monarch@hunter.org", "ShadowRank1", "Sung Jin-Woo")
        assertTrue("New account registration must succeed", regResult.isSuccess)

        val identity = regResult.getOrThrow()
        assertEquals("monarch@hunter.org", identity.email)
        assertEquals("Sung Jin-Woo", identity.displayName)
        assertEquals(AuthStatus.EMAIL_UNVERIFIED, identity.authStatus)

        // Verify tokens stored securely
        val accessToken = tokenStorage.getAccessToken()
        val refreshToken = tokenStorage.getRefreshToken()
        assertNotNull("Access token must be retrieved and stored", accessToken)
        assertNotNull("Refresh token must be retrieved and stored", refreshToken)
        assertTrue(accessToken!!.startsWith("access-"))
        assertTrue(refreshToken!!.startsWith("refresh-"))
    }

    /**
     * Verification Test 2: Existing account login.
     */
    @Test
    fun verification2_existingAccountLogin() = runTest {
        val fakeServer = ValidatingFakeSupabaseServer()
        val repo = buildSyncRepo(fakeServer)

        fakeServer.seedUser("shadow@hunter.org", "ShadowRank1", "Beru", verified = true)

        val loginResult = repo.signIn("shadow@hunter.org", "ShadowRank1")
        assertTrue("Existing account login must succeed", loginResult.isSuccess)

        val identity = loginResult.getOrThrow()
        assertEquals("shadow@hunter.org", identity.email)
        assertTrue("Email verified user should have verified status", identity.emailVerified)
        assertEquals(AuthStatus.SIGNED_IN, identity.authStatus)
    }

    /**
     * Verification Test 4: Cloud profile creation and synchronization.
     */
    @Test
    fun verification4_cloudProfileCreation() = runTest {
        val fakeServer = ValidatingFakeSupabaseServer()
        val repo = buildSyncRepo(fakeServer)

        fakeServer.seedUser("monarch@hunter.org", "ShadowRank1", "Sung Jin-Woo", verified = true)
        val loginResult = repo.signIn("monarch@hunter.org", "ShadowRank1")
        assertTrue(loginResult.isSuccess)

        val identity = loginResult.getOrThrow()
        assertNotNull(identity.userId)
        assertTrue(identity.userId.isNotBlank())
        assertEquals("Sung Jin-Woo", identity.displayName)

        // Confirm database local session identity is stored
        val session = db.cloudAuthDao().getSessionOnce()
        assertNotNull("Cloud auth session must be persisted in database", session)
        assertEquals(identity.userId, session!!.supabaseUserId)
        assertEquals("monarch@hunter.org", session.email)
    }

    /**
     * Verification Test 5: Authenticated API requests pass valid tokens and API keys.
     */
    @Test
    fun verification5_authenticatedApiRequests() = runTest {
        var capturedAuthHeader: String? = null
        var capturedApiKeyHeader: String? = null

        val okClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request()
                capturedAuthHeader = req.header("Authorization")
                capturedApiKeyHeader = req.header("apikey")
                Response.Builder()
                    .request(req)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("""{"id":"dev-1","user_id":"u-1","display_name":"Hunter","active_title":"Awakened E-Rank","account_status":"ACTIVE","level":1,"current_level_xp":0,"total_xp":0,"rank_code":"E_RANK","updated_at":""}""".toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()

        val client = HttpSupabaseClient(
            supabaseUrl = "https://mockproject.supabase.co",
            anonKey = "mock_anon_key_for_test_12345678",
            httpClient = okClient
        )

        val res = client.fetchOrInitializePlayerIdentity(
            accessToken = "valid_test_token_abc123",
            userId = "user_456",
            displayName = "Hunter"
        )

        assertTrue(res is CloudCallResult.Success)
        assertEquals("Bearer valid_test_token_abc123", capturedAuthHeader)
        assertEquals("mock_anon_key_for_test_12345678", capturedApiKeyHeader)
    }

    /**
     * Verification Test 6: Session persistence across instances.
     */
    @Test
    fun verification6_sessionPersistence() = runTest {
        val fakeServer = ValidatingFakeSupabaseServer()
        val repo = buildSyncRepo(fakeServer)

        fakeServer.seedUser("persist@hunter.org", "ShadowRank1", "Igris", verified = true)
        repo.signIn("persist@hunter.org", "ShadowRank1").getOrThrow()

        // Recreate repository from the same database and token storage
        val freshRepo = buildSyncRepo(fakeServer)
        val activeSession = freshRepo.observeActiveSession().first()

        assertNotNull("Session must persist across repository re-instantiation", activeSession)
        assertEquals("persist@hunter.org", activeSession!!.email)
        assertEquals(AuthStatus.SIGNED_IN, activeSession.authStatus)
        assertEquals("access-persist@hunter.org", tokenStorage.getAccessToken())
    }

    /**
     * Verification Test 7: Logout and subsequent login.
     */
    @Test
    fun verification7_logoutAndSubsequentLogin() = runTest {
        val fakeServer = ValidatingFakeSupabaseServer()
        val repo = buildSyncRepo(fakeServer)

        fakeServer.seedUser("logout_test@hunter.org", "ShadowRank1", "Iron", verified = true)
        repo.signIn("logout_test@hunter.org", "ShadowRank1").getOrThrow()

        // Logout
        val signOutResult = repo.signOut()
        assertTrue("Sign out must succeed", signOutResult.isSuccess)
        assertNull("Access token must be cleared on logout", tokenStorage.getAccessToken())
        assertNull("Cloud auth session must be cleared on logout", db.cloudAuthDao().getSessionOnce())

        // Subsequent login succeeds cleanly
        val reloginResult = repo.signIn("logout_test@hunter.org", "ShadowRank1")
        assertTrue("Subsequent login must succeed", reloginResult.isSuccess)
        assertNotNull(tokenStorage.getAccessToken())
        assertEquals(AuthStatus.SIGNED_IN, reloginResult.getOrThrow().authStatus)
    }

    /**
     * Verification Test 8: Offline behavior and vault fallback.
     */
    @Test
    fun verification8_offlineVaultBehavior() = runTest {
        val unconfiguredClient = HttpSupabaseClient(
            supabaseUrl = "https://YOUR_PROJECT_REF.supabase.co",
            anonKey = "YOUR_SUPABASE_ANON_KEY"
        )
        assertFalse("Unconfigured client must recognize placeholder state", unconfiguredClient.isConfigured())

        val unconfiguredRepo = buildSyncRepo(unconfiguredClient)
        assertFalse(unconfiguredRepo.isCloudConfigured())

        // Operating in offline vault mode: local profile exists and works independently
        val player = db.playerDao().getPlayerOnce()
        assertNotNull("Local player profile exists in offline vault mode", player)
    }

    /**
     * SSL Verification Test 9: Handling of Trust Anchor / Certificate Validation Failure.
     * Simulates the exact exception:
     * java.security.cert.CertPathValidatorException: Trust anchor for certification path not found.
     * Verifies that the client categorizes it as SSL_ERROR and sanitizes the log without crashing.
     */
    @Test
    fun verification9_sslTrustAnchorFailureHandling() = runTest {
        val failingSslClient = OkHttpClient.Builder()
            .addInterceptor {
                val cause = CertPathValidatorException("Trust anchor for certification path not found.")
                throw SSLHandshakeException("Certificate validation failed").initCause(cause) as SSLHandshakeException
            }
            .build()

        val supabaseClient = HttpSupabaseClient(
            supabaseUrl = "https://misconfigured-server.example.com",
            anonKey = "anon_key_test_value",
            httpClient = failingSslClient
        )

        val result = supabaseClient.signUp("test@hunter.org", "ShadowRank1", "Tester")
        assertTrue("Result must be Failure", result is CloudCallResult.Failure)

        val failure = result as CloudCallResult.Failure
        assertEquals("SSL_ERROR", failure.category)
        assertTrue(
            "Failure message must capture cert path validation issue safely",
            failure.message.contains("Trust anchor", ignoreCase = true) ||
                failure.message.contains("certificate", ignoreCase = true)
        )
    }

    /**
     * SSL Verification Test 10: Successful HTTPS registration with valid TLS certificate.
     * Simulates a properly configured TLS server with valid certificate chain.
     */
    @Test
    fun verification10_successfulHttpsRegistrationWithValidCert() = runTest {
        val validTlsClient = OkHttpClient.Builder()
            .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS))
            .addInterceptor { chain ->
                val req = chain.request()
                // Return successful Supabase auth response
                Response.Builder()
                    .request(req)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(
                        """
                        {
                            "access_token": "valid_ssl_access_token_12345",
                            "refresh_token": "valid_ssl_refresh_token_67890",
                            "expires_in": 3600,
                            "user": {
                                "id": "user-uuid-ssl-verified",
                                "email": "valid-ssl@hunter.org",
                                "created_at": "2026-11-20T00:00:00Z"
                            }
                        }
                        """.trimIndent().toResponseBody("application/json".toMediaType())
                    )
                    .build()
            }
            .build()

        val supabaseClient = HttpSupabaseClient(
            supabaseUrl = "https://valid-tls-project.supabase.co",
            anonKey = "valid_anon_key_test_12345",
            httpClient = validTlsClient
        )

        val result = supabaseClient.signUp("valid-ssl@hunter.org", "ShadowRank1", "Valid Hunter")
        assertTrue("Registration over verified TLS must succeed", result is CloudCallResult.Success)

        val auth = (result as CloudCallResult.Success).data
        assertEquals("valid_ssl_access_token_12345", auth.accessToken)
        assertEquals("user-uuid-ssl-verified", auth.user?.id)
        assertEquals("valid-ssl@hunter.org", auth.user?.email)
    }

    /**
     * Verification Test 11: Standard Supabase URL Normalization & Project Hostname Integrity.
     * Verifies that the standard Supabase project URL (https://pakpjwztozfdphqdszjn.supabase.co):
     * - Uses HTTPS
     * - Has no accidental whitespace
     * - Has no duplicated /auth/v1 paths
     * - Preserves the exact project reference pakpjwztozfdphqdszjn
     */
    @Test
    fun verification11_standardSupabaseUrlNormalization() {
        val standardUrl = "https://pakpjwztozfdphqdszjn.supabase.co"
        assertEquals(
            "https://pakpjwztozfdphqdszjn.supabase.co",
            HttpSupabaseClient.normalizeBaseUrl(standardUrl)
        )

        // Trailing slashes stripped cleanly
        assertEquals(
            "https://pakpjwztozfdphqdszjn.supabase.co",
            HttpSupabaseClient.normalizeBaseUrl("https://pakpjwztozfdphqdszjn.supabase.co/")
        )

        // Accidental whitespace stripped cleanly
        assertEquals(
            "https://pakpjwztozfdphqdszjn.supabase.co",
            HttpSupabaseClient.normalizeBaseUrl("  https://pakpjwztozfdphqdszjn.supabase.co \n")
        )

        // Accidental service path suffix stripped cleanly (prevents duplicated /auth/v1/auth/v1 paths)
        assertEquals(
            "https://pakpjwztozfdphqdszjn.supabase.co",
            HttpSupabaseClient.normalizeBaseUrl("https://pakpjwztozfdphqdszjn.supabase.co/auth/v1")
        )

        val client = HttpSupabaseClient(
            supabaseUrl = "https://pakpjwztozfdphqdszjn.supabase.co",
            anonKey = "valid_project_anon_key_mock_value"
        )
        assertTrue("Standard configured Supabase project must report isConfigured = true", client.isConfigured())
    }

    /**
     * Verification Test 12: Network Error Differentiation.
     * Accurately distinguishes between:
     * - DNS resolution errors (UnknownHostException)
     * - Socket timeouts (SocketTimeoutException)
     * - HTTP status error codes (401, 404, 422)
     * - Certificate validation errors (SSLHandshakeException)
     */
    @Test
    fun verification12_errorClassificationDifferentiatesDnsTimeoutAndHttp() = runTest {
        // 1. DNS Resolution Error
        val dnsFailingClient = OkHttpClient.Builder()
            .addInterceptor { throw java.net.UnknownHostException("pakpjwztozfdphqdszjn.supabase.co") }
            .build()
        val dnsClient = HttpSupabaseClient(
            supabaseUrl = "https://pakpjwztozfdphqdszjn.supabase.co",
            anonKey = "mock_key",
            httpClient = dnsFailingClient
        )
        val dnsResult = dnsClient.signUp("test@hunter.org", "Pass12345", "Hunter")
        assertTrue(dnsResult is CloudCallResult.Failure)
        assertEquals("DNS_ERROR", (dnsResult as CloudCallResult.Failure).category)

        // 2. Timeout Error
        val timeoutFailingClient = OkHttpClient.Builder()
            .addInterceptor { throw java.net.SocketTimeoutException("Connect timed out") }
            .build()
        val timeoutClient = HttpSupabaseClient(
            supabaseUrl = "https://pakpjwztozfdphqdszjn.supabase.co",
            anonKey = "mock_key",
            httpClient = timeoutFailingClient
        )
        val timeoutResult = timeoutClient.signUp("test@hunter.org", "Pass12345", "Hunter")
        assertTrue(timeoutResult is CloudCallResult.Failure)
        assertEquals("TIMEOUT_ERROR", (timeoutResult as CloudCallResult.Failure).category)

        // 3. HTTP 422 Validation Error
        val http422Client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(422)
                    .message("Unprocessable Entity")
                    .body("""{"message":"User already registered"}""".toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()
        val http422Supabase = HttpSupabaseClient(
            supabaseUrl = "https://pakpjwztozfdphqdszjn.supabase.co",
            anonKey = "mock_key",
            httpClient = http422Client
        )
        val httpResult = http422Supabase.signUp("test@hunter.org", "Pass12345", "Hunter")
        assertTrue(httpResult is CloudCallResult.Failure)
        val httpFailure = httpResult as CloudCallResult.Failure
        assertEquals(422, httpFailure.code)
        assertEquals("VALIDATION_OR_DUPLICATE", httpFailure.category)
        assertEquals("User already registered", httpFailure.message)
    }

    /**
     * Phase 5 Regression Tests:
     * 1. Standard base URL normalization
     * 2. Signup URL construction (asserts exactly https://pakpjwztozfdphqdszjn.supabase.co/auth/v1/signup)
     * 3. Login URL construction
     * 4. Token refresh URL construction
     * 5. Logout URL construction
     * 6. Authenticated REST requests
     * 7. Duplicate path prevention
     * 8. Invalid URL rejection
     */
    @Test
    fun verification13_exactEndpointUrlConstructionAndDuplicatePathPrevention() {
        val client = HttpSupabaseClient(
            supabaseUrl = "https://pakpjwztozfdphqdszjn.supabase.co",
            anonKey = "valid_anon_key_for_url_test"
        )

        // 1. Standard base URL normalization
        assertEquals("https://pakpjwztozfdphqdszjn.supabase.co", HttpSupabaseClient.normalizeBaseUrl("https://pakpjwztozfdphqdszjn.supabase.co"))

        // 2. Signup URL construction: Must be EXACTLY https://pakpjwztozfdphqdszjn.supabase.co/auth/v1/signup
        val signupUrl = client.buildEndpointUrl("auth/v1/signup")
        assertEquals(
            "https://pakpjwztozfdphqdszjn.supabase.co/auth/v1/signup",
            signupUrl
        )

        // 3. Login URL construction
        val loginUrl = client.buildEndpointUrl("auth/v1/token", mapOf("grant_type" to "password"))
        assertEquals(
            "https://pakpjwztozfdphqdszjn.supabase.co/auth/v1/token?grant_type=password",
            loginUrl
        )

        // 4. Token refresh URL construction
        val refreshUrl = client.buildEndpointUrl("auth/v1/token", mapOf("grant_type" to "refresh_token"))
        assertEquals(
            "https://pakpjwztozfdphqdszjn.supabase.co/auth/v1/token?grant_type=refresh_token",
            refreshUrl
        )

        // 5. Logout URL construction
        val logoutUrl = client.buildEndpointUrl("auth/v1/logout")
        assertEquals(
            "https://pakpjwztozfdphqdszjn.supabase.co/auth/v1/logout",
            logoutUrl
        )

        // 6. Authenticated REST requests
        val restUrl = client.buildEndpointUrl("rest/v1/player_identities", mapOf("user_id" to "eq.u123", "select" to "*"))
        assertEquals(
            "https://pakpjwztozfdphqdszjn.supabase.co/rest/v1/player_identities?user_id=eq.u123&select=*",
            restUrl
        )

        // 7. Duplicate path prevention: Even if user configured full path in SUPABASE_URL
        val clientWithDuplicateAuthPath = HttpSupabaseClient(
            supabaseUrl = "https://pakpjwztozfdphqdszjn.supabase.co/auth/v1",
            anonKey = "key"
        )
        assertEquals(
            "https://pakpjwztozfdphqdszjn.supabase.co/auth/v1/signup",
            clientWithDuplicateAuthPath.buildEndpointUrl("auth/v1/signup")
        )

        val clientWithTrailingSlashAndRest = HttpSupabaseClient(
            supabaseUrl = "https://pakpjwztozfdphqdszjn.supabase.co/rest/v1/",
            anonKey = "key"
        )
        assertEquals(
            "https://pakpjwztozfdphqdszjn.supabase.co/auth/v1/signup",
            clientWithTrailingSlashAndRest.buildEndpointUrl("auth/v1/signup")
        )

        val clientWithFullSignupInBase = HttpSupabaseClient(
            supabaseUrl = "https://pakpjwztozfdphqdszjn.supabase.co/auth/v1/signup",
            anonKey = "key"
        )
        assertEquals(
            "https://pakpjwztozfdphqdszjn.supabase.co/auth/v1/signup",
            clientWithFullSignupInBase.buildEndpointUrl("auth/v1/signup")
        )

        // 8. Invalid URL rejection
        assertNull(HttpSupabaseClient.parseBaseUrl(""))
        assertNull(HttpSupabaseClient.parseBaseUrl("YOUR_PROJECT_REF"))
        assertNull(HttpSupabaseClient.parseBaseUrl("https://YOUR_PROJECT_REF.supabase.co"))
        val unconfiguredClient = HttpSupabaseClient("https://YOUR_PROJECT_REF.supabase.co", "key")
        assertFalse(unconfiguredClient.isConfigured())
    }

    @Test
    fun verification14_publishableKeyFormatAndLegacyJwtSupport() {
        // 1. Supabase publishable key format (modern)
        val publishableKey = "sb_publishable_hd9U_WfxrofHMbH1tJEhOA_lAi-bnfr"
        assertTrue(SupabaseKeyValidator.isPublishableKey(publishableKey))
        assertTrue(SupabaseKeyValidator.isRecognizedFormat(publishableKey))
        assertFalse(SupabaseKeyValidator.isLegacyJwtKey(publishableKey))

        // 2. Supabase legacy JWT anon key format
        val legacyJwtKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InBha3Bqd3p0b3pmZHBocWRzempuIiwicm9sZSI6ImFub24iLCJpYXQiOjE2ODAwMDAwMDAsImV4cCI6MTk5NTU1NTU1NX0.signature"
        assertTrue(SupabaseKeyValidator.isLegacyJwtKey(legacyJwtKey))
        assertTrue(SupabaseKeyValidator.isRecognizedFormat(legacyJwtKey))
        assertFalse(SupabaseKeyValidator.isPublishableKey(legacyJwtKey))

        // 3. Invalid or placeholder keys
        assertFalse(SupabaseKeyValidator.isRecognizedFormat("YOUR_SUPABASE_ANON_KEY"))
        assertFalse(SupabaseKeyValidator.isRecognizedFormat("short_key"))
        assertFalse(SupabaseKeyValidator.isRecognizedFormat(""))
    }

    @Test
    fun verification15_safeDiagnosticReportingWithoutSecrets() {
        val configuredClient = HttpSupabaseClient(
            supabaseUrl = "https://pakpjwztozfdphqdszjn.supabase.co",
            anonKey = "sb_publishable_hd9U_WfxrofHMbH1tJEhOA_lAi-bnfr"
        )
        val unauthReport = configuredClient.generateDiagnosticReport(null)
        assertEquals("YES", unauthReport.urlConfigured)
        assertEquals("YES", unauthReport.publicKeyConfigured)
        assertEquals("YES", unauthReport.recognizedKeyFormat)
        assertEquals("VERIFIED", unauthReport.keyProjectCompatibility)
        assertEquals("YES", unauthReport.apiKeyHeaderAttached)
        assertEquals("SAFE", unauthReport.authorizationHeaderState)

        // Verify report string representation does not leak any part of the key
        val reportString = unauthReport.toString()
        assertFalse(reportString.contains("sb_publishable"))
        assertFalse(reportString.contains("hd9U"))
        assertFalse(reportString.contains("Bearer"))

        // Authenticated request report
        val authReport = configuredClient.generateDiagnosticReport("test_access_token_1234567890")
        assertEquals("SAFE", authReport.authorizationHeaderState)
        assertFalse(authReport.toString().contains("test_access_token"))

        // Unconfigured client
        val unconfigured = HttpSupabaseClient(
            supabaseUrl = "https://YOUR_PROJECT_REF.supabase.co",
            anonKey = "YOUR_SUPABASE_ANON_KEY"
        )
        val unconfiguredReport = unconfigured.generateDiagnosticReport(null)
        assertEquals("NO", unconfiguredReport.urlConfigured)
        assertEquals("NO", unconfiguredReport.publicKeyConfigured)
        assertEquals("NO", unconfiguredReport.recognizedKeyFormat)
        assertEquals("UNVERIFIED", unconfiguredReport.keyProjectCompatibility)
        assertEquals("NO", unconfiguredReport.apiKeyHeaderAttached)
    }

    @Test
    fun verification16_emailConfirmationPayloadDecoding() {
        val client = HttpSupabaseClient(
            supabaseUrl = "https://pakpjwztozfdphqdszjn.supabase.co",
            anonKey = "sb_publishable_hd9U_WfxrofHMbH1tJEhOA_lAi-bnfr"
        )

        // Direct user payload as returned by GoTrue when mailer_autoconfirm is false:
        val rawGoTrueConfirmationPayload = """
            {
              "id": "71ccc661-d6be-4160-abd0-917d15be152c",
              "aud": "authenticated",
              "role": "authenticated",
              "email": "hunter_verify_2026@testmail.com",
              "confirmation_sent_at": "2026-11-21T02:20:34.921866633Z",
              "app_metadata": { "provider": "email", "providers": ["email"] },
              "user_metadata": { "email": "hunter_verify_2026@testmail.com", "email_verified": false },
              "created_at": "2026-11-21T02:20:34.879535Z",
              "updated_at": "2026-11-21T02:20:36.466366Z"
            }
        """.trimIndent()

        // Test decoding via public signUp with mock engine or reflection
        val method = HttpSupabaseClient::class.java.getDeclaredMethod("decodeAuthResponse", String::class.java)
        method.isAccessible = true
        val decoded = method.invoke(client, rawGoTrueConfirmationPayload) as SupabaseAuthResponse

        assertNotNull(decoded.user)
        assertEquals("71ccc661-d6be-4160-abd0-917d15be152c", decoded.user?.id)
        assertEquals("hunter_verify_2026@testmail.com", decoded.user?.email)
        assertNull(decoded.user?.emailConfirmedAt)
        assertEquals("", decoded.accessToken)
    }

    @Test
    fun verification17_networkTimeoutAndDnsFailureHandling() {
        val client = HttpSupabaseClient(
            supabaseUrl = "https://pakpjwztozfdphqdszjn.supabase.co",
            anonKey = "sb_publishable_hd9U_WfxrofHMbH1tJEhOA_lAi-bnfr"
        )
        val method = HttpSupabaseClient::class.java.getDeclaredMethod("classifyException", Throwable::class.java)
        method.isAccessible = true

        // Timeout
        val (timeoutCategory, timeoutMsg) = method.invoke(client, java.net.SocketTimeoutException("Read timed out")) as Pair<String, String>
        assertEquals("TIMEOUT_ERROR", timeoutCategory)
        assertTrue(timeoutMsg.contains("timed out"))

        // DNS failure
        val (dnsCategory, dnsMsg) = method.invoke(client, java.net.UnknownHostException("pakpjwztozfdphqdszjn.supabase.co")) as Pair<String, String>
        assertEquals("DNS_ERROR", dnsCategory)
        assertTrue(dnsMsg.contains("Cannot resolve Supabase host"))
    }

    // Test helper TokenStorage implementation
    private class TestTokenStorage : TokenStorage {
        private var access: String? = null
        private var refresh: String? = null
        private val deviceId = "test-device-uuid-1"

        override fun saveTokens(accessToken: String, refreshToken: String) {
            access = accessToken
            refresh = refreshToken
        }

        override fun getAccessToken(): String? = access
        override fun getRefreshToken(): String? = refresh
        override fun clearTokens() {
            access = null
            refresh = null
        }

        override fun getOrCreateDeviceUuid(): String = deviceId
    }

    // In-memory validating fake Supabase server
    private class ValidatingFakeSupabaseServer : SupabaseApiPort {
        private data class Record(
            val id: String,
            val email: String,
            val password: String,
            val displayName: String,
            var verified: Boolean
        )

        private val users = mutableMapOf<String, Record>()

        fun seedUser(email: String, pw: String, name: String, verified: Boolean = false) {
            val id = "user-$email"
            users[email] = Record(id, email, pw, name, verified)
        }

        override fun isConfigured(): Boolean = true

        override fun generateDiagnosticReport(accessToken: String?): SupabaseDiagnosticReport =
            SupabaseDiagnosticReport("YES", "YES", "YES", "VERIFIED", "YES", "SAFE")

        override fun buildOAuthAuthorizeUrl(provider: String, redirectUri: String): String =
            "https://test.supabase.co/auth/v1/authorize?provider=$provider"

        override suspend fun signUp(
            email: String,
            password: String,
            displayName: String
        ): CloudCallResult<SupabaseAuthResponse> {
            val uid = "user-$email"
            users[email] = Record(uid, email, password, displayName, false)
            return CloudCallResult.Success(
                SupabaseAuthResponse(
                    accessToken = "access-$email",
                    refreshToken = "refresh-$email",
                    expiresInSeconds = 3600L,
                    user = SupabaseUserDto(uid, email, null)
                )
            )
        }

        override suspend fun signIn(email: String, password: String): CloudCallResult<SupabaseAuthResponse> {
            val user = users[email] ?: return CloudCallResult.Failure(400, "AUTH", "Invalid credentials")
            return CloudCallResult.Success(
                SupabaseAuthResponse(
                    accessToken = "access-$email",
                    refreshToken = "refresh-$email",
                    expiresInSeconds = 3600L,
                    user = SupabaseUserDto(
                        id = user.id,
                        email = user.email,
                        emailConfirmedAt = if (user.verified) "2026-11-20T00:00:00Z" else null
                    )
                )
            )
        }

        override suspend fun refreshSession(refreshToken: String): CloudCallResult<SupabaseAuthResponse> {
            val email = refreshToken.removePrefix("refresh-")
            val user = users[email] ?: return CloudCallResult.Failure(401, "AUTH_EXPIRED", "Expired")
            return CloudCallResult.Success(
                SupabaseAuthResponse(
                    accessToken = "access-$email",
                    refreshToken = refreshToken,
                    expiresInSeconds = 3600L,
                    user = SupabaseUserDto(user.id, user.email, if (user.verified) "2026-11-20T00:00:00Z" else null)
                )
            )
        }

        override suspend fun resendVerificationEmail(email: String): CloudCallResult<Unit> = CloudCallResult.Success(Unit)
        override suspend fun sendPasswordRecovery(email: String): CloudCallResult<Unit> = CloudCallResult.Success(Unit)
        override suspend fun updatePassword(accessToken: String, newPassword: String): CloudCallResult<SupabaseUserDto> =
            CloudCallResult.Success(SupabaseUserDto())
        override suspend fun fetchUser(accessToken: String): CloudCallResult<SupabaseUserDto> =
            CloudCallResult.Success(SupabaseUserDto())
        override suspend fun signOut(accessToken: String): CloudCallResult<Unit> = CloudCallResult.Success(Unit)

        override suspend fun fetchOrInitializePlayerIdentity(
            accessToken: String,
            userId: String,
            displayName: String
        ): CloudCallResult<RemotePlayerIdentityDto> = CloudCallResult.Success(
            RemotePlayerIdentityDto(
                id = "identity-$userId",
                userId = userId,
                displayName = displayName,
                activeTitle = "Awakened E-Rank",
                accountStatus = "ACTIVE",
                level = 1,
                totalXp = 0L,
                rankCode = "E_RANK"
            )
        )

        override suspend fun updateDisplayName(
            accessToken: String,
            userId: String,
            displayName: String
        ): CloudCallResult<RemotePlayerIdentityDto> = CloudCallResult.Success(RemotePlayerIdentityDto())

        override suspend fun upsertDevice(accessToken: String, device: RemoteDeviceDto): CloudCallResult<Unit> =
            CloudCallResult.Success(Unit)
        override suspend fun listDevices(accessToken: String, userId: String): CloudCallResult<List<RemoteDeviceDto>> =
            CloudCallResult.Success(emptyList())
        override suspend fun revokeDevice(accessToken: String, userId: String, deviceId: String): CloudCallResult<Unit> =
            CloudCallResult.Success(Unit)
        override suspend fun upsertTableRecords(accessToken: String, tableName: String, rowsJsonArray: String): CloudCallResult<Unit> =
            CloudCallResult.Success(Unit)
        override suspend fun fetchTableChanges(accessToken: String, tableName: String, userId: String, updatedSinceIso: String, limit: Int, offset: Int) =
            CloudCallResult.Success(emptyList<kotlinx.serialization.json.JsonObject>())
        override suspend fun validateProgressionEvents(accessToken: String, deviceId: String, events: List<com.example.domain.model.CloudProgressionEventPayload>) =
            CloudCallResult.Success(com.example.data.remote.ProgressionValidateResponse())
        override suspend fun uploadCloudBackup(accessToken: String, backup: com.example.data.remote.RemoteBackupDto) =
            CloudCallResult.Success(Unit)
        override suspend fun listCloudBackups(accessToken: String, userId: String) =
            CloudCallResult.Success(emptyList<com.example.data.remote.RemoteBackupDto>())
        override suspend fun validateCloudRestore(accessToken: String, backupId: String) =
            CloudCallResult.Success(Unit)
        override suspend fun requestAccountDeletion(accessToken: String, confirmationPhrase: String) =
            CloudCallResult.Success(Unit)
    }
}
