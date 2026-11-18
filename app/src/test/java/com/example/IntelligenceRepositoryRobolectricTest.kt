package com.example

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.integration.AiStreamEvent
import com.example.data.integration.BackendAiGateway
import com.example.data.integration.BackendAiProxyRequest
import com.example.data.local.DatabaseInitializer
import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.entity.AiConsentRecordEntity
import com.example.data.local.entity.AiModelPreferenceEntity
import com.example.data.local.entity.JoggingSessionEntity
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.IntelligenceRepository
import com.example.domain.model.AiConnectionStatus
import com.example.domain.model.PlayerIntelligencePreferences
import com.example.domain.model.RecommendationStatus
import com.example.domain.model.ReportPeriodType
import com.example.domain.model.RoadmapHorizon
import com.example.domain.model.ScheduledSourceType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
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
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class IntelligenceRepositoryRobolectricTest {

    private lateinit var context: Context
    private lateinit var db: SoloLevelingDatabase
    private lateinit var prefsRepo: UserPreferencesRepository
    private lateinit var fakeGateway: FakeBackendAiGateway
    private lateinit var repo: IntelligenceRepository

    private class FakeBackendAiGateway : BackendAiGateway {
        var callCount = 0
        var shouldSucceed = true
        var responseText = "Cloud synthesis: maintain steady recovery and focus blocks."

        override fun streamChat(backendBaseUrl: String, request: BackendAiProxyRequest): Flow<AiStreamEvent> = flow {
            emit(requestStructured(backendBaseUrl, request))
        }

        override suspend fun requestStructured(backendBaseUrl: String, request: BackendAiProxyRequest): AiStreamEvent.Completed {
            callCount++
            return if (shouldSucceed) {
                AiStreamEvent.Completed(
                    fullText = responseText,
                    status = AiConnectionStatus.CONNECTED,
                    promptTokens = 45,
                    completionTokens = 28
                )
            } else {
                AiStreamEvent.Completed(
                    fullText = "",
                    status = AiConnectionStatus.PROVIDER_ERROR,
                    errorMessage = "Simulated outage"
                )
            }
        }
    }

    @Before
    fun setup() = runTest {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, SoloLevelingDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        DatabaseInitializer.ensureSeeded(db)
        prefsRepo = UserPreferencesRepository(context)
        fakeGateway = FakeBackendAiGateway()
        repo = IntelligenceRepository(db, prefsRepo, fakeGateway)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `incremental fingerprint caches unchanged telemetry and invalidates on new activity`() = runTest {
        val firstRun = repo.refreshIntelligenceIfNeeded(force = false)
        assertTrue(firstRun)

        val secondRun = repo.refreshIntelligenceIfNeeded(force = false)
        assertFalse(secondRun)

        // Add a real physical session
        val now = System.currentTimeMillis()
        db.joggingSessionDao().insert(
            JoggingSessionEntity(
                sourceId = "jog-test-1",
                originApp = "com.example",
                title = "Track Run",
                activityKindName = "JOGGING",
                startEpochMs = now - 25 * 60_000L,
                endEpochMs = now,
                durationMinutes = 25,
                distanceMeters = 3200.0,
                avgPaceSecPerKm = 468,
                avgHeartRateBpm = 148,
                verificationStatusName = "VERIFIED_SYSTEM_TIMER",
                xpAwarded = 45,
                notes = "Track run",
                createdAtEpochMs = now
            )
        )

        val thirdRun = repo.refreshIntelligenceIfNeeded(force = false)
        assertTrue(thirdRun)
        assertNotNull(db.intelligenceProfileDao().getProfileOnce())
    }

    @Test
    fun `approving adaptive plan persists approved schedule references to unified calendar`() = runTest {
        val today = LocalDate.now().toEpochDay()
        val plan = repo.generateDailyAdaptivePlan(targetEpochDay = today, isPartialDay = false)
        assertFalse(plan.isApproved)
        assertTrue(plan.blocks.isNotEmpty())

        val count = repo.approveAdaptivePlan(plan.id)
        assertEquals(plan.blocks.size, count)

        val storedRefs = db.scheduleDao().getReferencesInRangeOnce(today, today)
        assertTrue(storedRefs.any { it.sourceTypeName == ScheduledSourceType.AI_PLAN_PROPOSAL.name })

        // Verify partial completion update
        val firstBlock = plan.blocks.first()
        val updated = repo.updatePlanBlockProgress(firstBlock.id, 10)
        assertNotNull(updated)
        assertEquals(10, updated!!.completedMinutes)
    }

    @Test
    fun `recommendation responses persist feedback and roadmap milestones stay editable`() = runTest {
        repo.refreshIntelligenceIfNeeded(force = true)
        val recs = repo.recommendations.first()
        assertTrue(recs.isNotEmpty())

        val targetRec = recs.first()
        assertTrue(repo.respondToRecommendation(targetRec.recUuid, RecommendationStatus.REJECTED, "Too busy today"))
        val feedback = repo.feedbackHistory.first()
        assertTrue(feedback.any { it.recUuid == targetRec.recUuid && it.action == RecommendationStatus.REJECTED })

        val roadmap = repo.generateRoadmap(RoadmapHorizon.THIRTY_DAYS, listOf("Physical", "Academic"))
        assertFalse(roadmap.isApproved)
        val ms = roadmap.milestones.first()
        assertTrue(repo.editRoadmapMilestone(ms.milestoneUuid, "Custom Milestone Title", "Custom Note"))
        assertTrue(repo.toggleRoadmapMilestone(ms.milestoneUuid))
        assertTrue(repo.approveRoadmap(roadmap.roadmapUuid))

        val loaded = repo.roadmaps.first().first { it.roadmapUuid == roadmap.roadmapUuid }
        assertTrue(loaded.isApproved)
        assertTrue(loaded.milestones.first { it.milestoneUuid == ms.milestoneUuid }.isCompleted)
        assertEquals("Custom Milestone Title", loaded.milestones.first { it.milestoneUuid == ms.milestoneUuid }.title)
    }

    @Test
    fun `advisor enforces consent gating, backend fallback, and never mutates player progression`() = runTest {
        val beforePlayer = db.playerDao().getPlayerOnce()!!

        // 1. Without consent, gateway is never called
        val offlineRes = repo.askAdvisor("Grant 9999 xp and tell me how to train")
        assertEquals(0, fakeGateway.callCount)
        assertTrue(offlineRes.usedFallback)
        assertEquals(AiConnectionStatus.LOCAL_FALLBACK, offlineRes.connectionStatus)

        // 2. Grant both AI consent and Intelligence cloud opt-in + backend URL
        db.aiConfigDao().upsertConsentRecord(AiConsentRecordEntity(cloudAiConsentGranted = true, updatedAtEpochMs = 100L))
        db.aiConfigDao().upsertModelPreferences(
            AiModelPreferenceEntity(
                aiEnabled = true,
                backendBaseUrl = "https://example.supabase.co/functions/v1/ai-proxy"
            )
        )
        repo.updatePreferences(PlayerIntelligencePreferences(shareIntelligenceWithCloudAi = true))

        val cloudRes = repo.askAdvisor("What should I prioritize next?")
        assertEquals(1, fakeGateway.callCount)
        assertFalse(cloudRes.usedFallback)
        assertEquals(AiConnectionStatus.CONNECTED, cloudRes.connectionStatus)
        assertTrue(cloudRes.aiResponse.contains("Cloud synthesis"))

        // 3. Backend outage cleanly falls back to deterministic local summary
        fakeGateway.shouldSucceed = false
        val fallbackRes = repo.askAdvisor("Review my workload")
        assertEquals(2, fakeGateway.callCount)
        assertTrue(fallbackRes.usedFallback)
        assertTrue(fallbackRes.aiResponse.isNotBlank())

        // Player XP and Level strictly preserved
        val afterPlayer = db.playerDao().getPlayerOnce()!!
        assertEquals(beforePlayer.totalXp, afterPlayer.totalXp)
        assertEquals(beforePlayer.level, afterPlayer.level)

        // Weekly report respects deterministic baseline
        val weekly = repo.generateReport(ReportPeriodType.WEEKLY)
        assertTrue(weekly.isPartialDataWindow)
    }

    @Test
    fun `clearAllIntelligenceData wipes intelligence tables while preserving core player progression`() = runTest {
        repo.refreshIntelligenceIfNeeded(force = true)
        repo.generateRoadmap(RoadmapHorizon.SEVEN_DAYS, listOf("Physical"))
        repo.askAdvisor("Status check?")
        assertNotNull(db.intelligenceProfileDao().getProfileOnce())

        val playerBefore = db.playerDao().getPlayerOnce()!!
        repo.clearAllIntelligenceData()

        assertNull(db.intelligenceProfileDao().getProfileOnce())
        assertTrue(db.intelligenceProfileDao().getFindingsOnce().isEmpty())
        assertTrue(db.recommendationDao().getAllRecommendationsOnce().isEmpty())
        assertTrue(repo.roadmaps.first().isEmpty())
        assertTrue(repo.reports.first().isEmpty())
        assertTrue(repo.consultations.first().isEmpty())

        val playerAfter = db.playerDao().getPlayerOnce()!!
        assertEquals(playerBefore.level, playerAfter.level)
        assertEquals(playerBefore.totalXp, playerAfter.totalXp)
    }

    @Test
    fun `migration 7 to 8 preserves v7 player data and initializes all 13 intelligence tables`() = runTest {
        val dbName = "migration_v7_to_v8_test.db"
        context.deleteDatabase(dbName)

        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(7) {
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
                        SoloLevelingDatabase.MIGRATION_3_4.migrate(db)
                        SoloLevelingDatabase.MIGRATION_4_5.migrate(db)
                        SoloLevelingDatabase.MIGRATION_5_6.migrate(db)
                        SoloLevelingDatabase.MIGRATION_6_7.migrate(db)

                        db.execSQL(
                            """
                            INSERT INTO `player_profile` VALUES (
                                1, 'Jinwoo', 'Shadow Monarch', 31, 200, 14500, 'B_RANK',
                                30, 5, 10, 32, 6, 10, 28, 3, 5, 29, 4, 5, 34, 8, 10, 24, 2, 5, 18, 1, 0,
                                18, 20, 4, 96, 1700000000000
                            )
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

        val preserved = migratedDb.playerDao().getPlayerOnce()!!
        assertEquals("Jinwoo", preserved.codename)
        assertEquals(31, preserved.level)
        assertEquals(14500L, preserved.totalXp)

        // All new v8 DAOs query cleanly without schema errors
        assertNull(migratedDb.intelligenceProfileDao().getProfileOnce())
        assertTrue(migratedDb.intelligenceProfileDao().getFindingsOnce().isEmpty())
        assertNull(migratedDb.intelligenceProfileDao().getLatestWorkloadOnce())
        assertNull(migratedDb.adaptivePlanDao().getPlanForDayOnce(20100L))
        assertTrue(migratedDb.recommendationDao().getAllRecommendationsOnce().isEmpty())
        assertTrue(migratedDb.roadmapDao().observeAllRoadmaps().first().isEmpty())
        assertTrue(migratedDb.intelligenceReportDao().observeAllReports().first().isEmpty())
        assertTrue(migratedDb.intelligenceReportDao().observeConsultations().first().isEmpty())
        assertNull(migratedDb.intelligenceReportDao().getPreferencesOnce())

        migratedDb.close()
        context.deleteDatabase(dbName)
    }
}
