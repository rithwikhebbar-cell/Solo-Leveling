package com.example

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.healthconnect.HealthConnectDataSource
import com.example.data.healthconnect.HealthConnectPermissionState
import com.example.data.healthconnect.HealthConnectSdkAvailability
import com.example.data.healthconnect.HealthConnectSyncFetchResult
import com.example.data.integration.AiStreamEvent
import com.example.data.integration.BackendAiGateway
import com.example.data.integration.BackendAiProxyRequest
import com.example.data.integration.DeterministicSystemAdvisor
import com.example.data.integration.HealthConnectPort
import com.example.data.integration.HealthConnectReadiness
import com.example.data.integration.SupabaseSyncAdapter
import com.example.data.local.SoloLevelingDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.AcademicRepository
import com.example.data.repository.AiSystemRepository
import com.example.data.repository.RealityOsRepository
import com.example.domain.model.AiConnectionStatus
import com.example.domain.model.PlanMode
import com.example.domain.model.ProposalStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AiSystemRepositoryRobolectricTest {

    private lateinit var database: SoloLevelingDatabase
    private lateinit var realityOsRepository: RealityOsRepository
    private lateinit var academicRepository: AcademicRepository
    private lateinit var fakeGateway: FakeBackendAiGateway
    private lateinit var aiRepository: AiSystemRepository

    private class FakeBackendAiGateway : BackendAiGateway {
        var nextStreamTokens: List<String> = emptyList()
        var nextStatus: AiConnectionStatus = AiConnectionStatus.CONNECTED
        var nextStructuredText: String = ""
        var nextErrorMessage: String? = null
        var callsCount: Int = 0

        override fun streamChat(
            backendBaseUrl: String,
            request: BackendAiProxyRequest
        ): Flow<AiStreamEvent> = flow {
            callsCount += 1
            if (nextStatus == AiConnectionStatus.CONNECTED) {
                val combined = StringBuilder()
                nextStreamTokens.forEach { token ->
                    combined.append(token)
                    emit(AiStreamEvent.TokenDelta(token))
                }
                emit(
                    AiStreamEvent.Completed(
                        fullText = combined.toString(),
                        status = AiConnectionStatus.CONNECTED,
                        promptTokens = 120,
                        completionTokens = 45
                    )
                )
            } else {
                emit(
                    AiStreamEvent.Completed(
                        fullText = "",
                        status = nextStatus,
                        errorMessage = nextErrorMessage ?: "Simulated failure"
                    )
                )
            }
        }

        override suspend fun requestStructured(
            backendBaseUrl: String,
            request: BackendAiProxyRequest
        ): AiStreamEvent.Completed {
            callsCount += 1
            return AiStreamEvent.Completed(
                fullText = nextStructuredText,
                status = nextStatus,
                promptTokens = 95,
                completionTokens = 80,
                errorMessage = nextErrorMessage
            )
        }
    }

    private val stubHc = object : HealthConnectDataSource {
        override fun getAvailability() = HealthConnectSdkAvailability.AVAILABLE
        override suspend fun getPermissionState() = HealthConnectPermissionState()
        override suspend fun fetchRecentActivity(sinceEpochMs: Long, untilEpochMs: Long) =
            HealthConnectSyncFetchResult(HealthConnectPermissionState(), emptyList())
    }

    @Before
    fun setup() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SoloLevelingDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val prefs = UserPreferencesRepository(context)
        realityOsRepository = RealityOsRepository(
            database = database,
            preferencesRepository = prefs,
            healthConnectPort = object : HealthConnectPort {
                override fun checkReadiness() = HealthConnectReadiness.AVAILABLE
            },
            healthConnectDataSource = stubHc,
            cloudSyncPort = SupabaseSyncAdapter(),
            aiGameMasterPort = DeterministicSystemAdvisor()
        )
        realityOsRepository.ensureInitialized()
        academicRepository = AcademicRepository(database, prefs)
        fakeGateway = FakeBackendAiGateway()
        aiRepository = AiSystemRepository(
            database = database,
            preferencesRepository = prefs,
            realityOsRepository = realityOsRepository,
            academicRepository = academicRepository,
            backendAiGateway = fakeGateway
        )
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun chatPersistenceAndFallbacks_handleOffline_authError_rateLimit_outage_andStreaming() = runTest {
        // 1. Offline / unconfigured backend uses deterministic fallback cleanly without calling gateway
        val offlineRes = aiRepository.sendChatMessage(null, "What is holding back my rank?")
        assertEquals(0, fakeGateway.callsCount)
        assertTrue(offlineRes.assistantMessage.isFallback)
        assertTrue(offlineRes.assistantMessage.content.contains("RANK PROGRESSION DIAGNOSTIC"))

        // Configure backend URL and grant consent
        aiRepository.updateModelPreference { it.copy(backendBaseUrl = "https://demo.supabase.co") }
        aiRepository.updateConsent { it.copy(cloudAiConsentGranted = true) }

        // 2. Streaming cloud success
        fakeGateway.nextStatus = AiConnectionStatus.CONNECTED
        fakeGateway.nextStreamTokens = listOf("Focus on ", "verified sessions ", "for D-Rank.")
        val streamedTokens = mutableListOf<String>()
        val cloudRes = aiRepository.sendChatMessage(offlineRes.conversationId, "How to reach D-Rank?") {
            streamedTokens.add(it)
        }
        assertEquals(1, fakeGateway.callsCount)
        assertEquals(AiConnectionStatus.CONNECTED, cloudRes.connectionStatus)
        assertFalse(cloudRes.assistantMessage.isFallback)
        assertEquals("Focus on verified sessions for D-Rank.", cloudRes.assistantMessage.content)
        assertEquals(3, streamedTokens.size)

        // 3. Backend auth failure (401) falls back gracefully & reports AUTH_ERROR
        fakeGateway.nextStatus = AiConnectionStatus.AUTH_ERROR
        fakeGateway.nextErrorMessage = "Backend HTTP 401"
        val authFailRes = aiRepository.sendChatMessage(offlineRes.conversationId, "Am I ready to workout?")
        assertEquals(AiConnectionStatus.AUTH_ERROR, authFailRes.connectionStatus)
        assertTrue(authFailRes.assistantMessage.isFallback)

        // 4. Backend rate limit (429) falls back gracefully & reports RATE_LIMITED
        fakeGateway.nextStatus = AiConnectionStatus.RATE_LIMITED
        fakeGateway.nextErrorMessage = "Backend HTTP 429"
        val rateLimitRes = aiRepository.sendChatMessage(offlineRes.conversationId, "Status check")
        assertEquals(AiConnectionStatus.RATE_LIMITED, rateLimitRes.connectionStatus)
        assertTrue(rateLimitRes.assistantMessage.isFallback)

        // 5. Provider error / timeout (502) falls back gracefully
        fakeGateway.nextStatus = AiConnectionStatus.PROVIDER_ERROR
        fakeGateway.nextErrorMessage = "Timed out"
        val outageRes = aiRepository.sendChatMessage(offlineRes.conversationId, "Exam prep advice")
        assertEquals(AiConnectionStatus.PROVIDER_ERROR, outageRes.connectionStatus)
        assertTrue(outageRes.assistantMessage.isFallback)

        val persistedMsgs = aiRepository.observeMessages(offlineRes.conversationId).first()
        assertEquals(10, persistedMsgs.size) // 5 user + 5 assistant

        // Delete & clear history
        aiRepository.deleteConversation(offlineRes.conversationId)
        assertTrue(aiRepository.conversations.first().isEmpty())
    }

    @Test
    fun aiQuestProposalsAndPermissionBoundary_neverGrantDirectXpAndRequireApproval() = runTest {
        val playerBefore = realityOsRepository.playerProfile.first()
        val initialQuestsCount = realityOsRepository.quests.first().size

        // Malformed JSON from cloud falls back deterministically
        aiRepository.updateModelPreference { it.copy(backendBaseUrl = "https://demo.supabase.co") }
        aiRepository.updateConsent { it.copy(cloudAiConsentGranted = true) }
        fakeGateway.nextStatus = AiConnectionStatus.CONNECTED
        fakeGateway.nextStructuredText = "{malformed-json"

        val genResult = aiRepository.generateQuestProposals()
        assertTrue(genResult.usedFallback)
        assertTrue(genResult.count > 0)

        // Generating proposals & plans NEVER modifies XP, level, attributes, or rank
        val playerAfterGen = realityOsRepository.playerProfile.first()
        assertEquals(playerBefore.totalXp, playerAfterGen.totalXp)
        assertEquals(playerBefore.level, playerAfterGen.level)
        assertEquals(playerBefore.rank, playerAfterGen.rank)
        assertEquals(playerBefore.attributes, playerAfterGen.attributes)
        assertEquals(initialQuestsCount, realityOsRepository.quests.first().size)

        val proposals = aiRepository.questProposals.first()
        assertTrue(proposals.isNotEmpty())
        val first = proposals.first()

        // Approve first proposal -> creates real quest on Quest Board (or Academic) with 0 XP awarded
        val approveRes = aiRepository.approveQuestProposal(
            proposalId = first.id,
            editedTitle = "Edited AI Quest",
            editedDurationMinutes = 25
        )
        assertTrue(approveRes.isSuccess)
        val updatedProposals = aiRepository.questProposals.first()
        assertEquals(ProposalStatus.APPROVED, updatedProposals.first { it.id == first.id }.status)
        assertEquals(playerBefore.totalXp, realityOsRepository.playerProfile.first().totalXp)

        // Reject second proposal if present
        if (proposals.size > 1) {
            aiRepository.rejectQuestProposal(proposals[1].id)
            assertEquals(ProposalStatus.REJECTED, aiRepository.questProposals.first().first { it.id == proposals[1].id }.status)
        }
    }

    @Test
    fun recoverySafetyGuard_blocksApprovingPhysicalProposalWhenRestRecommended() = runTest {
        aiRepository.generateQuestProposals()
        val physicalProposal = aiRepository.questProposals.first().firstOrNull {
            it.category == com.example.domain.model.AiQuestProposalCategory.PHYSICAL
        }
        assertTrue(physicalProposal != null)

        // Now player reports severe fatigue + discomfort
        realityOsRepository.submitRecoveryCheckIn(
            fatigueLevel = 5,
            hasDiscomfort = true,
            discomfortNotes = "Knee pain"
        )

        val blocked = aiRepository.approveQuestProposal(physicalProposal!!.id)
        assertTrue(blocked.isFailure)
        assertEquals(ProposalStatus.PENDING, aiRepository.questProposals.first().first { it.id == physicalProposal.id }.status)
    }

    @Test
    fun dailyPlanGeneration_approvalsAndAuditsPersistCleanly() = runTest {
        val planRes = aiRepository.generatePlanProposals(PlanMode.DAILY)
        assertTrue(planRes.count >= 2)
        val items = aiRepository.dailyPlanProposals.first()
        assertTrue(items.isNotEmpty())

        aiRepository.approvePlanItem(items.first().id, editedTitle = "Custom Morning Block", editedStartMinute = 480, editedDurationMinutes = 40)
        val refreshed = aiRepository.dailyPlanProposals.first().first { it.id == items.first().id }
        assertEquals(ProposalStatus.APPROVED, refreshed.status)
        assertEquals("Custom Morning Block", refreshed.taskTitle)

        if (items.size > 1) {
            aiRepository.rejectPlanItem(items[1].id)
            assertEquals(ProposalStatus.REJECTED, aiRepository.dailyPlanProposals.first().first { it.id == items[1].id }.status)
        }
        assertTrue(aiRepository.recentAudits.first().isNotEmpty())
    }

    @Test
    fun migration3To4_preservesV3DataAndCreatesAiAndNotificationTables() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration_test_v3_v4.db"
        context.deleteDatabase(dbName)

        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(3) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `player_profile` (
                                `id` INTEGER NOT NULL,
                                `codename` TEXT NOT NULL,
                                `title` TEXT NOT NULL,
                                `level` INTEGER NOT NULL,
                                `currentLevelXp` INTEGER NOT NULL,
                                `totalXp` INTEGER NOT NULL,
                                `rankName` TEXT NOT NULL,
                                `strVerified` INTEGER NOT NULL,
                                `strSelfReported` INTEGER NOT NULL,
                                `strProgressXp` INTEGER NOT NULL,
                                `vitVerified` INTEGER NOT NULL,
                                `vitSelfReported` INTEGER NOT NULL,
                                `vitProgressXp` INTEGER NOT NULL,
                                `agiVerified` INTEGER NOT NULL,
                                `agiSelfReported` INTEGER NOT NULL,
                                `agiProgressXp` INTEGER NOT NULL,
                                `intVerified` INTEGER NOT NULL,
                                `intSelfReported` INTEGER NOT NULL,
                                `intProgressXp` INTEGER NOT NULL,
                                `disVerified` INTEGER NOT NULL,
                                `disSelfReported` INTEGER NOT NULL,
                                `disProgressXp` INTEGER NOT NULL,
                                `wisVerified` INTEGER NOT NULL,
                                `wisSelfReported` INTEGER NOT NULL,
                                `wisProgressXp` INTEGER NOT NULL,
                                `chaVerified` INTEGER NOT NULL,
                                `chaSelfReported` INTEGER NOT NULL,
                                `chaProgressXp` INTEGER NOT NULL,
                                `streakDays` INTEGER NOT NULL,
                                `verifiedActivitiesCount` INTEGER NOT NULL,
                                `selfReportedActivitiesCount` INTEGER NOT NULL,
                                `recoveryScore` INTEGER NOT NULL,
                                `createdAtEpochMs` INTEGER NOT NULL,
                                PRIMARY KEY(`id`)
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `quests` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `title` TEXT NOT NULL,
                                `description` TEXT NOT NULL,
                                `typeName` TEXT NOT NULL,
                                `categoryName` TEXT NOT NULL,
                                `targetValue` INTEGER NOT NULL,
                                `currentValue` INTEGER NOT NULL,
                                `unit` TEXT NOT NULL,
                                `xpReward` INTEGER NOT NULL,
                                `primaryAttrName` TEXT NOT NULL,
                                `primaryAttrXpReward` INTEGER NOT NULL,
                                `secondaryAttrName` TEXT,
                                `secondaryAttrXpReward` INTEGER NOT NULL,
                                `isCompleted` INTEGER NOT NULL,
                                `verificationStatusName` TEXT NOT NULL,
                                `createdAtEpochMs` INTEGER NOT NULL,
                                `completedAtEpochMs` INTEGER
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `activity_logs` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `title` TEXT NOT NULL,
                                `categoryName` TEXT NOT NULL,
                                `durationMinutes` INTEGER NOT NULL,
                                `verificationStatusName` TEXT NOT NULL,
                                `xpAwarded` INTEGER NOT NULL,
                                `primaryAttrName` TEXT NOT NULL,
                                `primaryAttrXpAwarded` INTEGER NOT NULL,
                                `notes` TEXT NOT NULL,
                                `timestampEpochMs` INTEGER NOT NULL
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `physical_state` (
                                `id` INTEGER NOT NULL,
                                `baseJoggingTargetMinutes` INTEGER NOT NULL,
                                `aiRecommendedJoggingMinutes` INTEGER NOT NULL,
                                `completedJoggingMinutesToday` INTEGER NOT NULL,
                                `workoutStatusName` TEXT NOT NULL,
                                `recoveryStatusName` TEXT NOT NULL,
                                `weeklySessionsCompleted` INTEGER NOT NULL,
                                `lastSessionTimestampMs` INTEGER,
                                `aiTrainingNote` TEXT NOT NULL,
                                PRIMARY KEY(`id`)
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `system_notifications` (
                                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                `title` TEXT NOT NULL,
                                `message` TEXT NOT NULL,
                                `categoryName` TEXT NOT NULL,
                                `isRead` INTEGER NOT NULL,
                                `timestampEpochMs` INTEGER NOT NULL
                            )
                            """.trimIndent()
                        )
                        SoloLevelingDatabase.MIGRATION_1_2.migrate(db)
                        SoloLevelingDatabase.MIGRATION_2_3.migrate(db)

                        db.execSQL(
                            """
                            INSERT INTO `player_profile` VALUES (
                                1, 'ShadowMonarch', 'Vanguard', 9, 100, 1850, 'D_RANK',
                                18, 2, 10, 19, 1, 20, 15, 0, 5, 17, 1, 15, 18, 0, 10, 15, 0, 5, 12, 0, 0,
                                8, 7, 3, 92, 1700000000000
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            INSERT INTO `system_notifications` (`title`, `message`, `categoryName`, `isRead`, `timestampEpochMs`)
                            VALUES ('Legacy Notice', 'Preserved from v3', 'SYSTEM_ALERT', 0, 1700000001000)
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            INSERT INTO `academic_subjects` (`name`, `code`, `programSystem`, `colorTokenName`, `iconKeyName`, `priorityName`, `notes`, `isArchived`, `createdAtEpochMs`)
                            VALUES ('Quantum Mechanics', 'PHYS301', 'BSc Physics', 'CYAN', 'SCIENCE', 'HIGH', 'Preserved subject', 0, 1700000002000)
                            """.trimIndent()
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        helper.writableDatabase.close()

        val migratedDb = Room.databaseBuilder(context, SoloLevelingDatabase::class.java, dbName)
            .addMigrations(
                SoloLevelingDatabase.MIGRATION_1_2,
                SoloLevelingDatabase.MIGRATION_2_3,
                SoloLevelingDatabase.MIGRATION_3_4,
                SoloLevelingDatabase.MIGRATION_4_5,
                SoloLevelingDatabase.MIGRATION_5_6,
                SoloLevelingDatabase.MIGRATION_6_7,
                SoloLevelingDatabase.MIGRATION_7_8,
                SoloLevelingDatabase.MIGRATION_8_9,
                SoloLevelingDatabase.MIGRATION_9_10,
                SoloLevelingDatabase.MIGRATION_10_11,
                SoloLevelingDatabase.MIGRATION_11_12,
                SoloLevelingDatabase.MIGRATION_12_13,
                SoloLevelingDatabase.MIGRATION_13_14,
                SoloLevelingDatabase.MIGRATION_14_15,
                SoloLevelingDatabase.MIGRATION_15_16,
                SoloLevelingDatabase.MIGRATION_16_17
            )
            .allowMainThreadQueries()
            .build()

        val preservedPlayer = migratedDb.playerDao().getPlayerOnce()
        assertEquals("ShadowMonarch", preservedPlayer?.codename)
        assertEquals(9, preservedPlayer?.level)

        val preservedNotifs = migratedDb.systemNotificationDao().getNotificationsSince(0L)
        assertEquals(1, preservedNotifs.size)
        assertEquals("Legacy Notice", preservedNotifs.first().title)
        assertEquals(null, preservedNotifs.first().deepLinkRoute)

        val preservedSubjects = migratedDb.academicSubjectDao().getActiveSubjectsOnce()
        assertEquals(1, preservedSubjects.size)
        assertEquals("Quantum Mechanics", preservedSubjects.first().name)

        // Verify all new v4 tables are queryable & writable
        assertEquals(0, migratedDb.aiConversationDao().getConversationsOnce().size)
        assertEquals(null, migratedDb.aiConfigDao().getModelPreferencesOnce())

        migratedDb.close()
        context.deleteDatabase(dbName)
    }
}
