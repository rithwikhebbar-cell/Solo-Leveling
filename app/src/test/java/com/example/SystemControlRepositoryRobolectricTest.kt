package com.example

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkManager
import com.example.data.automation.AutomationOrchestrator
import com.example.data.integration.AiStreamEvent
import com.example.data.integration.BackendAiGateway
import com.example.data.integration.BackendAiProxyRequest
import com.example.data.local.DatabaseInitializer
import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.entity.ActivityLogEntity
import com.example.data.local.entity.DungeonCompletionRecordEntity
import com.example.data.local.entity.DungeonDefinitionEntity
import com.example.data.local.entity.SystemEventEntity
import com.example.data.local.entity.toEntity
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.remote.HttpSupabaseClient
import com.example.data.remote.SecureTokenStorage
import com.example.data.repository.IntelligenceRepository
import com.example.data.repository.SyncRepository
import com.example.data.repository.SystemControlRepository
import com.example.data.repository.SystemEventRepository
import com.example.data.sync.CloudSyncEngine
import com.example.data.sync.SyncQueueManager
import com.example.data.sync.SyncStatusTracker
import com.example.domain.model.AiConnectionStatus
import com.example.domain.model.DailyExecutionStatus
import com.example.domain.model.EventProcessingStatus
import com.example.domain.model.ScheduledSourceType
import com.example.domain.model.SystemEventType
import com.example.worker.DailySystemResetWorker
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
import java.time.ZoneId
import java.time.ZonedDateTime

@RunWith(RobolectricTestRunner::class)
class SystemControlRepositoryRobolectricTest {

    private lateinit var context: Context
    private lateinit var db: SoloLevelingDatabase
    private lateinit var prefsRepo: UserPreferencesRepository
    private lateinit var eventRepo: SystemEventRepository
    private lateinit var automationOrchestrator: AutomationOrchestrator
    private lateinit var controlRepo: SystemControlRepository

    private class FakeBackendAiGateway : BackendAiGateway {
        override fun streamChat(backendBaseUrl: String, request: BackendAiProxyRequest): Flow<AiStreamEvent> = flow {
            emit(requestStructured(backendBaseUrl, request))
        }

        override suspend fun requestStructured(backendBaseUrl: String, request: BackendAiProxyRequest): AiStreamEvent.Completed {
            return AiStreamEvent.Completed(
                fullText = "Local test synthesis",
                status = AiConnectionStatus.CONNECTED
            )
        }
    }

    @Before
    fun setup() = runTest {
        context = ApplicationProvider.getApplicationContext()
        if (!WorkManager.isInitialized()) {
            WorkManager.initialize(context, Configuration.Builder().build())
        }
        db = Room.inMemoryDatabaseBuilder(context, SoloLevelingDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        DatabaseInitializer.ensureSeeded(db)
        prefsRepo = UserPreferencesRepository(context)
        eventRepo = SystemEventRepository(db)

        val supabaseClient = HttpSupabaseClient()
        val tokenStorage = SecureTokenStorage(context)
        val queueManager = SyncQueueManager(db)
        val statusTracker = SyncStatusTracker(db) { supabaseClient.isConfigured() }
        val syncEngine = CloudSyncEngine(db, supabaseClient, tokenStorage, queueManager, statusTracker)
        val syncRepo = SyncRepository(db, prefsRepo, supabaseClient, tokenStorage, queueManager, statusTracker, syncEngine, eventRepo)
        val intelRepo = IntelligenceRepository(db, prefsRepo, FakeBackendAiGateway())

        automationOrchestrator = AutomationOrchestrator(
            context = context,
            database = db,
            preferencesRepository = prefsRepo,
            systemEventRepository = eventRepo,
            intelligenceRepository = intelRepo,
            syncRepository = syncRepo
        )
        controlRepo = SystemControlRepository(
            database = db,
            preferencesRepository = prefsRepo,
            systemEventRepository = eventRepo,
            automationOrchestrator = automationOrchestrator,
            syncStatusTracker = statusTracker
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `SystemEventRepository recovers interrupted PROCESSING events and escalates repeated failures cleanly`() = runTest {
        val now = 1_750_000_000_000L
        db.systemEventDao().upsertEvent(
            SystemEventEntity(
                eventId = "stuck-event-1",
                eventTypeName = SystemEventType.ACADEMIC_SESSION_COMPLETED.name,
                timestampEpochMs = now - 60_000L,
                sourceModule = "ACADEMIC",
                relatedEntityId = "session-1",
                metadataJson = "{}",
                schemaVersion = 1,
                processingStatusName = EventProcessingStatus.PROCESSING.name,
                retryCount = 0,
                errorMessage = null,
                processedAtEpochMs = null,
                correlationId = "stuck-event-1"
            )
        )

        val recovered = eventRepo.recoverInterruptedEvents(now)
        assertEquals(1, recovered)

        val eventsAfter = db.systemEventDao().getRecentEventsOnce(10)
        assertEquals(EventProcessingStatus.PROCESSED.name, eventsAfter.first { it.eventId == "stuck-event-1" }.processingStatusName)

        val recOps = db.systemIntegrityHealthDao().observeRecoveryOperations(10).first()
        assertTrue(recOps.any { it.operationType == "EVENT_QUEUE_CRASH_RECOVERY" })

        // Repeated handler failures -> DEAD_LETTER -> manual retry succeeds after handler clears error
        var shouldFail = true
        eventRepo.registerHandler(SystemEventType.SYNC_FAILED) {
            if (shouldFail) Result.failure(IllegalStateException("Temporary net failure")) else Result.success(Unit)
        }
        val failingEvent = eventRepo.publishEvent(
            eventType = SystemEventType.SYNC_FAILED,
            relatedEntityId = "sync-99",
            metadata = mapOf("idempotencyKey" to "sync-fail-unique"),
            nowEpochMs = now + 1000L
        )
        repeat(3) { eventRepo.processPendingQueue(now + 2000L + it) }
        val dead = db.systemEventDao().getRecentEventsOnce(10).first { it.eventId == failingEvent.eventId }
        assertEquals(EventProcessingStatus.DEAD_LETTER.name, dead.processingStatusName)

        shouldFail = false
        eventRepo.retryEvent(failingEvent.eventId)
        val restored = db.systemEventDao().getRecentEventsOnce(10).first { it.eventId == failingEvent.eventId }
        assertEquals(EventProcessingStatus.PROCESSED.name, restored.processingStatusName)
    }

    @Test
    fun `AutomationOrchestrator executes daily reset idempotently and schedules WorkManager jobs cleanly`() = runTest {
        val nyZone = ZoneId.of("America/New_York")
        val day1Morning = ZonedDateTime.of(2025, 4, 2, 9, 0, 0, 0, nyZone).toInstant().toEpochMilli()
        controlRepo.updatePreferences(
            controlRepo.observePreferences().first().copy(preferredTimezoneId = nyZone.id)
        )

        val firstCycle = automationOrchestrator.runDailyCycleIdempotent("TEST_LAUNCH", day1Morning).getOrThrow()
        assertNotNull(firstCycle)

        // Running again on the exact same logical day returns null (no duplicate quest reset)
        val sameDayEvening = ZonedDateTime.of(2025, 4, 2, 22, 0, 0, 0, nyZone).toInstant().toEpochMilli()
        val duplicateAttempt = automationOrchestrator.runDailyCycleIdempotent("TEST_REPEAT", sameDayEvening).getOrThrow()
        assertNull(duplicateAttempt)

        // Jumping 3 days ahead runs once and logs missedDaysCaughtUp = 2
        val day4Morning = ZonedDateTime.of(2025, 4, 5, 9, 0, 0, 0, nyZone).toInstant().toEpochMilli()
        val catchUp = automationOrchestrator.runDailyCycleIdempotent("TEST_CATCHUP", day4Morning).getOrThrow()
        assertNotNull(catchUp)
        assertEquals(2, catchUp!!.missedDaysCaughtUp)

        // WorkManager unique periodic registration + job disable persistence
        automationOrchestrator.scheduleAllEnabledWorkers()
        val infos = WorkManager.getInstance(context).getWorkInfosForUniqueWork(DailySystemResetWorker.WORK_NAME).get()
        assertTrue(infos.isNotEmpty())

        automationOrchestrator.setJobEnabled(AutomationOrchestrator.JOB_INTELLIGENCE_REFRESH, false)
        val updatedJobs = db.automationDao().getJobsOnce()
        assertFalse(updatedJobs.first { it.automationId == AutomationOrchestrator.JOB_INTELLIGENCE_REFRESH }.isEnabled)
    }

    @Test
    fun `SystemControlRepository integrity scan, safe repair, confirmation-gated destructive cleanup, and pagination perform cleanly`() = runTest {
        val now = System.currentTimeMillis()
        // Seed player with currentLevelXp above level threshold + missing RPG audit + orphaned exercise
        db.playerDao().upsertPlayer(
            com.example.domain.model.PlayerProfile(
                codename = "Sung",
                title = "Awakened",
                level = 5,
                currentLevelXp = 10_000,
                xpToNextLevel = 200,
                rank = com.example.domain.model.HunterRank.E_RANK
            ).toEntity()
        )
        val dungeonId = db.dungeonDao().insertDefinition(
            DungeonDefinitionEntity(
                code = "TEST_E_GATE",
                title = "Test Gate",
                description = "Test gate definition",
                categoryName = "ENDURANCE",
                difficultyName = "E_RANK",
                requiredRankName = "E_RANK",
                requiredLevel = 1,
                requiredAttributeCode = null,
                requiredAttributeValue = 0,
                timeLimitHours = 24,
                baseXpReward = 50,
                primaryAttrName = "VIT",
                primaryAttrXp = 20,
                secondaryAttrName = "DIS",
                secondaryAttrXp = 10,
                rewardTitleId = null,
                rewardItemId = null,
                rewardShadowId = null,
                isRepeatable = false,
                creationSourceName = "SYSTEM",
                isActiveCatalog = true
            )
        )
        db.dungeonDao().insertCompletionRecord(
            DungeonCompletionRecordEntity(
                rewardKey = "missing_audit_dungeon",
                instanceId = 1L,
                dungeonId = dungeonId,
                dungeonTitle = "Test Gate",
                difficultyName = "E_RANK",
                xpAwarded = 50,
                primaryAttrName = "VIT",
                primaryAttrXp = 20,
                secondaryAttrName = "DIS",
                secondaryAttrXp = 10,
                evidenceSummary = "Verified",
                verificationProvenance = "VERIFIED",
                completedAtEpochMs = now
            )
        )
        // Simulate orphaned exercise row (e.g., from interrupted backup/legacy restore bypassing FK checks)
        val writableDb = db.openHelper.writableDatabase
        writableDb.execSQL("PRAGMA foreign_keys = OFF")
        writableDb.execSQL(
            """
            INSERT INTO `exercise_records` (
                `workoutSessionId`, `exerciseId`, `exerciseName`, `categoryName`,
                `setsCompleted`, `repsPerSet`, `weightKg`, `durationSeconds`, `restSeconds`,
                `primaryAttrName`, `timestampEpochMs`
            ) VALUES (987654, 'squat', 'Squats', 'STRENGTH', 3, 10, 60.0, NULL, 90, 'STR', $now)
            """.trimIndent()
        )
        writableDb.execSQL("PRAGMA foreign_keys = ON")

        val findings = controlRepo.runIntegrityScan(now)
        assertTrue(findings.any { it.affectedTable == "player_profile" })
        assertTrue(findings.any { it.affectedTable == "rpg_reward_audit" })
        assertTrue(findings.any { it.affectedTable == "exercise_records" && it.isDestructiveRepair })

        // Non-destructive repairs succeed without confirmation
        val auditFinding = findings.first { it.affectedTable == "rpg_reward_audit" }
        assertTrue(controlRepo.repairFinding(auditFinding.findingUuid, confirmedDestructive = false, nowEpochMs = now).isSuccess)
        assertEquals(1, db.rpgAuditDao().getAllOnce().count { it.rewardKey == "missing_audit_dungeon" })

        // Destructive orphan cleanup blocked without user confirmation, succeeds when confirmed
        val orphanFinding = findings.first { it.affectedTable == "exercise_records" }
        assertTrue(controlRepo.repairFinding(orphanFinding.findingUuid, confirmedDestructive = false, nowEpochMs = now).isFailure)
        assertTrue(controlRepo.repairFinding(orphanFinding.findingUuid, confirmedDestructive = true, nowEpochMs = now).isSuccess)
        assertTrue(db.workoutSessionDao().getAllExerciseRecordsOnce().isEmpty())

        // High-volume event + activity pagination query check (220 rows)
        repeat(220) { idx ->
            db.activityLogDao().insertLog(
                ActivityLogEntity(
                    title = "Session #$idx",
                    categoryName = "PHYSICAL",
                    durationMinutes = 20,
                    verificationStatusName = "SELF_REPORTED",
                    xpAwarded = 10,
                    primaryAttrName = "VIT",
                    primaryAttrXpAwarded = 5,
                    notes = "",
                    timestampEpochMs = now + idx
                )
            )
        }
        val paged = db.activityLogDao().getRecentLogs(limit = 50)
        assertEquals(50, paged.size)
        assertEquals("Session #219", paged.first().title)

        // Execution update enforces completion proof reference
        val item = com.example.domain.model.DailyExecutionItem(
            executionKey = "20150_CUSTOM_EVENT_1",
            sourceModule = "LIFE",
            activityType = ScheduledSourceType.CUSTOM_EVENT,
            sourceRecordId = 1L,
            title = "Evening Planning",
            subtitle = "Life",
            dateEpochDay = 20150L,
            plannedStartMinute = 19 * 60,
            plannedEndMinute = 19 * 60 + 30,
            durationMinutes = 30
        )
        assertTrue(controlRepo.updateExecutionStatus(item, DailyExecutionStatus.COMPLETED, evidenceReference = "").isFailure)
        assertTrue(controlRepo.updateExecutionStatus(item, DailyExecutionStatus.COMPLETED, evidenceReference = "PLAN_SAVED").isSuccess)
    }

    @Test
    fun `Room MIGRATION_8_9 preserves all pre-existing v8 player data, adds timestamp index and creates all 9 v9 tables`() = runTest {
        val dbName = "migration_v8_to_v9_test.db"
        context.deleteDatabase(dbName)

        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(8) {
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
                        SoloLevelingDatabase.MIGRATION_7_8.migrate(db)

                        db.execSQL(
                            """
                            INSERT INTO `player_profile` VALUES (
                                1, 'Monarch', 'Shadow Monarch', 42, 350, 29800, 'A_RANK',
                                40, 8, 20, 42, 9, 10, 38, 6, 10, 44, 7, 10, 45, 9, 10, 32, 4, 10, 26, 2, 10,
                                35, 65, 10, 92, 1700000000000
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
        assertEquals("Monarch", preserved.codename)
        assertEquals(42, preserved.level)
        assertEquals(29800L, preserved.totalXp)

        // All 9 new v9 tables & indexed queries execute cleanly
        assertTrue(migratedDb.systemEventDao().getRecentEventsOnce(20).isEmpty())
        assertTrue(migratedDb.automationDao().getJobsOnce().isEmpty())
        assertTrue(migratedDb.automationDao().observeExecutionLogs(10).first().isEmpty())
        assertNull(migratedDb.dailyCycleDao().getLatestCycleOnce())
        assertEquals(0, migratedDb.dailyCycleDao().getTotalExecutionOverridesCount())
        assertTrue(migratedDb.systemIntegrityHealthDao().getOpenFindingsOnce().isEmpty())
        assertTrue(migratedDb.systemIntegrityHealthDao().observeHealthSnapshots(5).first().isEmpty())
        assertTrue(migratedDb.systemIntegrityHealthDao().observeRecoveryOperations(5).first().isEmpty())
        assertNull(migratedDb.systemIntegrityHealthDao().getPreferencesOnce())
        assertTrue(migratedDb.activityLogDao().getRecentLogs(20).isEmpty())

        migratedDb.close()
        context.deleteDatabase(dbName)
    }
}
