package com.example

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.DatabaseInitializer
import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.entity.AcademicSubjectEntity
import com.example.data.local.entity.CloudBackupEntity
import com.example.data.local.entity.HabitEntity
import com.example.data.local.entity.JoggingSessionEntity
import com.example.data.local.entity.PersonalTaskEntity
import com.example.data.local.entity.SyncConflictEntity
import com.example.data.local.entity.SyncQueueEntity
import com.example.data.local.entity.SyncRecordMetaEntity
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.remote.CloudCallResult
import com.example.data.remote.CloudDataMapper
import com.example.data.remote.HttpSupabaseClient
import com.example.data.remote.ProgressionValidateResponse
import com.example.data.remote.RemoteBackupDto
import com.example.data.remote.RemoteDeviceDto
import com.example.data.remote.RemotePlayerIdentityDto
import com.example.data.remote.SupabaseApiPort
import com.example.data.remote.SupabaseAuthResponse
import com.example.data.remote.SupabaseDiagnosticReport
import com.example.data.remote.SupabaseUserDto
import com.example.data.remote.TokenStorage
import com.example.data.repository.SyncRepository
import com.example.data.sync.CloudSyncEngine
import com.example.data.sync.SyncQueueManager
import com.example.data.sync.SyncStatusTracker
import com.example.domain.engine.SyncConflictResolver
import com.example.domain.model.AuthStatus
import com.example.domain.model.BackupStatus
import com.example.domain.model.CloudProgressionEventPayload
import com.example.domain.model.CloudTaskPayload
import com.example.domain.model.ConflictResolutionChoice
import com.example.domain.model.ConflictStatus
import com.example.domain.model.PersonalTaskStatus
import com.example.domain.model.SyncEntityType
import com.example.domain.model.SyncOperationType
import com.example.domain.model.SyncState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
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
import java.time.Instant
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CloudSyncRepositoryRobolectricTest {

    private lateinit var context: Context
    private lateinit var prefs: UserPreferencesRepository
    private lateinit var fakeServer: FakeSupabaseServer

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        prefs = UserPreferencesRepository(context)
        fakeServer = FakeSupabaseServer()
    }

    private val openTrackers = mutableListOf<SyncStatusTracker>()
    private val openDbs = mutableListOf<SoloLevelingDatabase>()

    private fun newDb(): SoloLevelingDatabase = Room.inMemoryDatabaseBuilder(
        context,
        SoloLevelingDatabase::class.java
    ).allowMainThreadQueries().build().also { openDbs += it }

    private fun buildRepo(db: SoloLevelingDatabase, storage: FakeTokenStorage): SyncRepository {
        val queueManager = SyncQueueManager(db)
        val tracker = SyncStatusTracker(db) { fakeServer.isConfigured() }
        openTrackers += tracker
        tracker.setOnline(true)
        val engine = CloudSyncEngine(db, fakeServer, storage, queueManager, tracker)
        return SyncRepository(db, prefs, fakeServer, storage, queueManager, tracker, engine)
    }

    @After
    fun tearDown() {
        openTrackers.forEach { it.close() }
        openDbs.forEach { if (it.isOpen) it.close() }
    }

    @Test
    fun authRegistrationLoginVerificationRefreshRecoveryAndExpiredSession() = runTest {
        val db = newDb()
        DatabaseInitializer.ensureSeeded(db)
        val storage = FakeTokenStorage("device-a")
        val repo = buildRepo(db, storage)

        // Sign up starts unverified
        val signUpRes = repo.signUp("jinwoo@hunter.org", "ShadowRank1", "Sung Jinwoo")
        assertTrue(signUpRes.isSuccess)
        val signedUp = signUpRes.getOrThrow()
        assertEquals(AuthStatus.EMAIL_UNVERIFIED, signedUp.authStatus)
        assertFalse(signedUp.emailVerified)

        // Duplicate registration returns clear error
        val dupRes = repo.signUp("jinwoo@hunter.org", "ShadowRank1", "Duplicate")
        assertTrue(dupRes.isFailure)

        // Verify email remotely and refresh verification state
        fakeServer.verifyEmail("jinwoo@hunter.org")
        val verifiedRes = repo.refreshSessionAndVerification()
        assertTrue(verifiedRes.isSuccess)
        assertTrue(verifiedRes.getOrThrow().emailVerified)
        assertEquals(AuthStatus.SIGNED_IN, verifiedRes.getOrThrow().authStatus)

        // Password recovery & update
        assertTrue(repo.sendPasswordRecovery("jinwoo@hunter.org").isSuccess)
        assertTrue(repo.resetPassword("ShadowRank2").isSuccess)

        // Sign out clears tokens and session while keeping local player data intact
        repo.signOut()
        assertNull(storage.getAccessToken())
        assertNull(db.cloudAuthDao().getSessionOnce())
        assertNotNull(db.playerDao().getPlayerOnce())

        // Login again with updated password
        val loginRes = repo.signIn("jinwoo@hunter.org", "ShadowRank2")
        assertTrue(loginRes.isSuccess)
        assertEquals("Sung Jinwoo", loginRes.getOrThrow().displayName)

        // Simulate expired refresh token -> SESSION_EXPIRED without losing data
        fakeServer.failRefreshWithExpired = true
        val expiredRes = repo.refreshSessionAndVerification()
        assertTrue(expiredRes.isFailure)
        assertEquals(AuthStatus.SESSION_EXPIRED, db.cloudAuthDao().getSessionOnce()?.authStatusName?.let { AuthStatus.valueOf(it) })
        db.close()
    }

    @Test
    fun offlineQueueDeduplicationRetryBackoffAndRestartRecovery() = runTest {
        val db = newDb()
        DatabaseInitializer.ensureSeeded(db)
        val storage = FakeTokenStorage("device-offline")
        val repo = buildRepo(db, storage)

        fakeServer.verifyEmail("player@solo.org")
        repo.signUp("player@solo.org", "ShadowRank1", "HunterOne").getOrThrow()
        repo.signIn("player@solo.org", "ShadowRank1").getOrThrow()

        // Create records locally offline
        db.personalTaskDao().insertTask(
            PersonalTaskEntity(
                title = "Offline Task 1",
                description = "Created without network",
                categoryName = "PERSONAL",
                priorityName = "HIGH",
                statusName = PersonalTaskStatus.PENDING.name,
                dueDateEpochDay = 20050L,
                scheduledStartMinute = null,
                estimatedMinutes = 30,
                linkedGoalId = null,
                isTimeSensitive = false,
                postponeCount = 0,
                timezoneId = "UTC",
                completedAtEpochMs = null,
                createdAtEpochMs = 1_700_000_000_000L,
                updatedAtEpochMs = 1_700_000_000_000L
            )
        )
        val uid = db.cloudAuthDao().getSessionOnce()!!.supabaseUserId
        repo.queueManager.scanAndEnqueueLocalChanges(uid, 1_700_000_001_000L)
        val initialCount = db.syncQueueDao().getAllOnce().size
        assertTrue(initialCount >= 1)

        // Scanning again must not create duplicate queue entries
        repo.queueManager.scanAndEnqueueLocalChanges(uid, 1_700_000_002_000L)
        assertEquals(initialCount, db.syncQueueDao().getAllOnce().size)

        // Simulate transient server error -> item transitions to FAILED with backoff & retryCount = 1
        fakeServer.failTableUpsert = true
        val failedRun = repo.syncNow("OFFLINE_RECOVERY_TEST").getOrThrow()
        assertTrue(failedRun.failedOperations >= 1)
        val queuedAfterFail = db.syncQueueDao().getAllOnce().first()
        assertEquals(SyncState.FAILED.name, queuedAfterFail.syncStateName)
        assertEquals(1, queuedAfterFail.retryCount)
        assertTrue(queuedAfterFail.nextRetryAtEpochMs > 1_700_000_002_000L)

        // Simulate app restart by building fresh SyncRepository over the same DB & TokenStorage
        fakeServer.failTableUpsert = false
        val restartedRepo = buildRepo(db, storage)
        restartedRepo.retryQueueItem(queuedAfterFail.queueId)
        val successRun = restartedRepo.syncNow("POST_RESTART").getOrThrow()
        assertEquals(SyncState.SYNCED, successRun.status)
        assertTrue(db.syncQueueDao().getAllOnce().isEmpty())
        db.close()
    }

    @Test
    fun paginatedPullMultiDeviceConvergenceAndDeviceRevocation() = runTest {
        // Device A
        val dbA = newDb()
        DatabaseInitializer.ensureSeeded(dbA)
        val repoA = buildRepo(dbA, FakeTokenStorage("device-A"))

        fakeServer.verifyEmail("multi@solo.org")
        repoA.signUp("multi@solo.org", "ShadowRank1", "MultiHunter").getOrThrow()
        repoA.signIn("multi@solo.org", "ShadowRank1").getOrThrow()

        // Push 115 tasks from Device A to exercise >2 pages (pageSize = 50)
        repeat(115) { i ->
            dbA.personalTaskDao().insertTask(
                PersonalTaskEntity(
                    title = "Dungeon Prep #$i",
                    description = "Desc $i",
                    categoryName = "LIFE",
                    priorityName = "MEDIUM",
                    statusName = PersonalTaskStatus.PENDING.name,
                    dueDateEpochDay = 20050L,
                    scheduledStartMinute = null,
                    estimatedMinutes = 20,
                    linkedGoalId = null,
                    isTimeSensitive = false,
                    postponeCount = 0,
                    timezoneId = "UTC",
                    completedAtEpochMs = null,
                    createdAtEpochMs = 1_700_000_000_000L + i,
                    updatedAtEpochMs = 1_700_000_000_000L + i
                )
            )
        }
        val pushLog = repoA.syncNow("DEVICE_A_PUSH").getOrThrow()
        assertEquals(115, pushLog.recordsUploaded)

        // Device B signs in and pulls incrementally in paginated batches
        val dbB = newDb()
        DatabaseInitializer.ensureSeeded(dbB)
        val storageB = FakeTokenStorage("device-B")
        val repoB = buildRepo(dbB, storageB)
        repoB.signIn("multi@solo.org", "ShadowRank1").getOrThrow()

        val pullLogB = repoB.syncNow("DEVICE_B_PULL").getOrThrow()
        assertEquals(115, pullLogB.recordsDownloaded)
        assertEquals(115, dbB.personalTaskDao().getAllTasksOnce().size)
        assertNotNull(dbB.syncMetaDao().getCursor(SyncEntityType.PERSONAL_TASK.name))

        // Device A lists devices (sees Device A and Device B) and revokes Device B
        val devicesOnA = repoA.refreshDevices().getOrThrow()
        assertEquals(2, devicesOnA.size)
        assertTrue(repoA.revokeDevice("device-B").isSuccess)

        // Next sync attempt by Device B must detect revocation and invalidate local tokens safely
        val revokedSync = repoB.syncNow("DEVICE_B_AFTER_REVOKE")
        assertTrue(revokedSync.isFailure)
        assertNull(storageB.getAccessToken())
        assertEquals(AuthStatus.SESSION_EXPIRED.name, dbB.cloudAuthDao().getSessionOnce()?.authStatusName)

        dbA.close()
        dbB.close()
    }

    @Test
    fun conflictResolutionKeepLocalKeepCloudAndMergeSafeWorkEndToEnd() = runTest {
        val db = newDb()
        DatabaseInitializer.ensureSeeded(db)
        val repo = buildRepo(db, FakeTokenStorage("device-conflict"))

        val taskId = db.personalTaskDao().insertTask(
            PersonalTaskEntity(
                title = "Local Title",
                description = "Local notes",
                categoryName = "WORK",
                priorityName = "HIGH",
                statusName = PersonalTaskStatus.PENDING.name,
                dueDateEpochDay = 20050L,
                scheduledStartMinute = null,
                estimatedMinutes = 45,
                linkedGoalId = null,
                isTimeSensitive = false,
                postponeCount = 1,
                timezoneId = "UTC",
                completedAtEpochMs = null,
                createdAtEpochMs = 1000L,
                updatedAtEpochMs = 2000L
            )
        )
        val localPayload = CloudTaskPayload("uuid-1", taskId, "Local Title", "Local notes", "WORK", "HIGH", "PENDING", 20050L, 45, 1, null, null, 2000L)
        val cloudPayload = CloudTaskPayload("uuid-1", taskId, "Cloud Title", "Cloud description", "WORK", "URGENT", "PENDING", 20050L, 45, 3, null, null, 2100L)

        val conflictId = db.syncConflictDao().insert(
            SyncConflictEntity(
                entityTypeName = SyncEntityType.PERSONAL_TASK.name,
                localKey = taskId.toString(),
                cloudUuid = "uuid-1",
                title = "Local Title",
                conflictReason = "Concurrent edit",
                localSummary = "Local Title",
                cloudSummary = "Cloud Title",
                localPayloadJson = CloudDataMapper.json.encodeToString(localPayload),
                cloudPayloadJson = CloudDataMapper.json.encodeToString(cloudPayload),
                localUpdatedAtEpochMs = 2000L,
                cloudUpdatedAtEpochMs = 2100L,
                resolutionStrategy = "USER_DECISION_REQUIRED",
                statusName = ConflictStatus.OPEN.name,
                resolvedAtEpochMs = null,
                createdAtEpochMs = 2200L
            )
        )

        val resolveMerge = repo.resolveConflict(conflictId, ConflictResolutionChoice.MERGE_SAFE)
        assertTrue(resolveMerge.isSuccess)
        assertEquals(ConflictStatus.RESOLVED_MERGED.name, db.syncConflictDao().getById(conflictId)?.statusName)
        val updatedTask = db.personalTaskDao().getTaskById(taskId)!!
        assertEquals("Cloud description", updatedTask.description)
        assertEquals(3, updatedTask.postponeCount)
        assertTrue(db.syncQueueDao().getAllOnce().any { it.localKey == taskId.toString() })
        db.close()
    }

    @Test
    fun backupSnapshotPreviewTamperRejectionSchemaRejectionAndRestore() = runTest {
        val db = newDb()
        DatabaseInitializer.ensureSeeded(db)
        val repo = buildRepo(db, FakeTokenStorage("device-backup"))

        fakeServer.verifyEmail("backup@solo.org")
        repo.signUp("backup@solo.org", "ShadowRank1", "BackupMonarch").getOrThrow()
        repo.signIn("backup@solo.org", "ShadowRank1").getOrThrow()

        db.habitDao().insertHabit(
            HabitEntity(
                title = "Cold Shower",
                description = "",
                habitTypeName = "BOOLEAN",
                categoryName = "DISCIPLINE",
                targetValue = 1.0,
                unit = "check",
                scheduledDaysCsv = "1,2,3,4,5,6,7",
                preferredStartMinute = null,
                reminderEnabled = false,
                isRestDayProtected = true,
                isArchived = false,
                createdAtEpochMs = 1000L
            )
        )

        val snapshot = repo.createCloudBackupSnapshot().getOrThrow()
        assertEquals(BackupStatus.UPLOADED, snapshot.status)
        assertEquals(64, snapshot.progressionChecksum.length)

        val preview = repo.buildRestorePreview(snapshot.backupUuid).getOrThrow()
        assertTrue(preview.isChecksumValid)
        assertTrue(preview.isSchemaCompatible)

        // Tampered checksum must abort restore safely
        val stored = db.cloudBackupDao().getByUuid(snapshot.backupUuid)!!
        db.cloudBackupDao().upsert(stored.copy(progressionChecksum = "deadbeef_tampered"))
        val tamperedPreview = repo.buildRestorePreview(snapshot.backupUuid).getOrThrow()
        assertFalse(tamperedPreview.isChecksumValid)
        assertTrue(repo.executeRestore(snapshot.backupUuid).isFailure)

        // Restore original checksum -> restore succeeds
        db.cloudBackupDao().upsert(stored)
        val exec = repo.executeRestore(snapshot.backupUuid)
        assertTrue(exec.isSuccess)
        assertEquals(BackupStatus.RESTORED.name, db.cloudBackupDao().getByUuid(snapshot.backupUuid)?.statusName)

        // Incompatible schema v3 must be blocked
        val oldBundleJson = CloudDataMapper.json.encodeToString(
            CloudDataMapper.json.decodeFromString<com.example.data.remote.CloudBackupBundle>(stored.payloadJson).copy(schemaVersion = 3)
        )
        db.cloudBackupDao().upsert(
            CloudBackupEntity(
                backupUuid = "legacy-v3",
                userId = "u1",
                schemaVersion = 3,
                progressionChecksum = SyncConflictResolver.computeSha256(oldBundleJson),
                summaryText = "Old",
                payloadJson = oldBundleJson,
                totalXpSnapshot = 0L,
                playerLevelSnapshot = 1,
                recordCount = 0,
                statusName = BackupStatus.CREATED.name,
                createdAtEpochMs = 500L,
                uploadedAtEpochMs = null
            )
        )
        val legacyPreview = repo.buildRestorePreview("legacy-v3").getOrThrow()
        assertFalse(legacyPreview.isSchemaCompatible)
        assertTrue(repo.executeRestore("legacy-v3").isFailure)
        db.close()
    }

    @Test
    fun accountDeletionAndHttpClientSecurityChecks() = runTest {
        // Placeholder config must never activate live requests
        val unconfiguredHttp = HttpSupabaseClient(
            supabaseUrl = "https://YOUR_PROJECT_REF.supabase.co",
            anonKey = "YOUR_SUPABASE_ANON_KEY"
        )
        assertFalse(unconfiguredHttp.isConfigured())

        val db = newDb()
        DatabaseInitializer.ensureSeeded(db)
        val storage = FakeTokenStorage("device-del")
        val repo = buildRepo(db, storage)

        fakeServer.verifyEmail("delete@solo.org")
        repo.signUp("delete@solo.org", "ShadowRank1", "DeleteMe").getOrThrow()
        repo.signIn("delete@solo.org", "ShadowRank1").getOrThrow()
        repo.createCloudBackupSnapshot().getOrThrow()

        // Wrong confirmation phrase fails
        assertTrue(repo.deleteAccount("WRONG PHRASE", "ShadowRank1").isFailure)

        // Wrong password re-authentication fails
        assertTrue(repo.deleteAccount("DELETE MY ACCOUNT", "WrongPw99").isFailure)

        // Exact phrase + valid password deletes remote & wipes local cloud tables safely
        val delRes = repo.deleteAccount("DELETE MY ACCOUNT", "ShadowRank1")
        assertTrue(delRes.isSuccess)
        assertNull(storage.getAccessToken())
        assertNull(db.cloudAuthDao().getSessionOnce())
        assertTrue(db.registeredDeviceDao().getDevicesOnce().isEmpty())
        assertTrue(db.syncQueueDao().getAllOnce().isEmpty())
        assertTrue(db.cloudBackupDao().observeBackups().first().isEmpty())
        assertTrue(fakeServer.deletedUsers.isNotEmpty())
        db.close()
    }

    @Test
    fun migration6To7PreservesV6DataAndCreatesAll8CloudTables() = runTest {
        val dbName = "migration_6_to_7_test.db"
        context.deleteDatabase(dbName)

        // Build real SQLite database at v6 via MIGRATION_1_2 .. MIGRATION_5_6
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(6) {
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

                        db.execSQL(
                            """
                            INSERT INTO `player_profile` VALUES (
                                1, 'Ashborn', 'Shadow Monarch', 24, 150, 9200, 'B_RANK',
                                30, 5, 10, 32, 6, 10, 28, 3, 5, 29, 4, 5, 34, 8, 10, 24, 2, 5, 18, 1, 0,
                                28, 30, 12, 99, 1700000000000
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            INSERT INTO `personal_tasks` (
                                `title`, `description`, `categoryName`, `priorityName`, `statusName`,
                                `dueDateEpochDay`, `scheduledStartMinute`, `estimatedMinutes`, `linkedGoalId`,
                                `isTimeSensitive`, `postponeCount`, `timezoneId`, `completedAtEpochMs`,
                                `createdAtEpochMs`, `updatedAtEpochMs`
                            ) VALUES (
                                'Legacy v6 Task', 'Preserved across v7', 'PERSONAL', 'HIGH', 'PENDING',
                                20050, NULL, 30, NULL, 0, 0, 'UTC', NULL, 1700000000000, 1700000000000
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
                SoloLevelingDatabase.MIGRATION_16_17,
                SoloLevelingDatabase.MIGRATION_17_18
            )
            .allowMainThreadQueries()
            .build()

        val preservedPlayer = migratedDb.playerDao().getPlayerOnce()!!
        assertEquals("Ashborn", preservedPlayer.codename)
        assertEquals(24, preservedPlayer.level)
        assertEquals(9200L, preservedPlayer.totalXp)

        val preservedTasks = migratedDb.personalTaskDao().getAllTasksOnce()
        assertEquals(1, preservedTasks.size)
        assertEquals("Legacy v6 Task", preservedTasks.first().title)

        // All 8 new v7 cloud tables are cleanly queryable
        assertNull(migratedDb.cloudAuthDao().getSessionOnce())
        assertTrue(migratedDb.registeredDeviceDao().getDevicesOnce().isEmpty())
        assertTrue(migratedDb.syncQueueDao().getAllOnce().isEmpty())
        assertTrue(migratedDb.syncConflictDao().observeConflicts().first().isEmpty())
        assertTrue(migratedDb.syncDiagnosticsDao().observeLogs().first().isEmpty())
        assertTrue(migratedDb.cloudBackupDao().observeBackups().first().isEmpty())
        assertNull(migratedDb.syncMetaDao().getCursor("PERSONAL_TASK"))

        migratedDb.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun academicSubjectsRlsUpsertSevenPendingRecoveryAndIsolation() = runTest {
        val db = newDb()
        DatabaseInitializer.ensureSeeded(db)
        val storage = FakeTokenStorage("device-acad")
        val repo = buildRepo(db, storage)

        fakeServer.verifyEmail("scholar@solo.org")
        repo.signUp("scholar@solo.org", "ShadowRank1", "ScholarMonarch").getOrThrow()
        val identity = repo.signIn("scholar@solo.org", "ShadowRank1").getOrThrow()
        val userId = identity.supabaseUserId

        // Seed 7 academic subjects locally and simulate 7 pre-existing FAILED queue items with empty "{}" payloads
        val subjectIds = (1..7).map { idx ->
            db.academicSubjectDao().insertSubject(
                AcademicSubjectEntity(
                    name = "Subject $idx",
                    code = "SUBJ-10$idx",
                    programSystem = "University",
                    colorTokenName = "CYAN",
                    iconKeyName = "BOOK",
                    priorityName = "HIGH",
                    notes = "",
                    isArchived = false,
                    createdAtEpochMs = 1_700_000_000_000L + idx
                )
            )
        }
        subjectIds.forEach { id ->
            val key = id.toString()
            val legacyBrokenUuid = "uuid_${userId}_ACADEMIC_SUBJECT_${key}"
            db.syncMetaDao().upsert(
                SyncRecordMetaEntity(
                    compositeKey = "${SyncEntityType.ACADEMIC_SUBJECT.name}:$key",
                    entityType = SyncEntityType.ACADEMIC_SUBJECT.name,
                    localKey = key,
                    cloudUuid = legacyBrokenUuid,
                    userId = userId,
                    version = 1,
                    updatedAtEpochMs = 1_700_000_000_000L,
                    deletedAtEpochMs = null,
                    syncStateName = SyncState.FAILED.name,
                    lastSyncedHash = "",
                    lastError = "new row violates row-level security policy for table \"academic_subjects\""
                )
            )
            db.syncQueueDao().insertIgnore(
                SyncQueueEntity(
                    entityTypeName = SyncEntityType.ACADEMIC_SUBJECT.name,
                    localKey = key,
                    cloudUuid = legacyBrokenUuid,
                    operationName = SyncOperationType.UPSERT.name,
                    payloadJson = "{}",
                    idempotencyKey = "ACADEMIC_SUBJECT:$legacyBrokenUuid:UPSERT:legacy",
                    syncStateName = SyncState.FAILED.name,
                    retryCount = 1,
                    nextRetryAtEpochMs = 0L,
                    lastError = "new row violates row-level security policy for table \"academic_subjects\"",
                    createdAtEpochMs = 1_700_000_000_000L,
                    updatedAtEpochMs = 1_700_000_000_000L
                )
            )
        }
        assertEquals(7, db.syncQueueDao().getAllOnce().size)

        // Retry sync: all 7 pending records are preserved, repaired in-place, and uploaded with valid UUID + user_id
        val syncLog = repo.syncNow("ACADEMIC_RECOVERY").getOrThrow()
        assertEquals(SyncState.SYNCED, syncLog.status)
        assertEquals(0, syncLog.failedOperations)
        assertEquals(7, syncLog.recordsUploaded)
        assertTrue(db.syncQueueDao().getAllOnce().isEmpty())

        val remoteRows = fakeServer.getTableRows("academic_subjects")
        assertEquals(7, remoteRows.size)
        assertTrue(remoteRows.values.all { it["user_id"]?.jsonPrimitive?.contentOrNull == userId })
        assertTrue(remoteRows.values.all { CloudDataMapper.isValidUuid(it["id"]?.jsonPrimitive?.contentOrNull) })

        // Duplicate sync must not create duplicate records
        val secondSync = repo.syncNow("ACADEMIC_IDEMPOTENT").getOrThrow()
        assertEquals(0, secondSync.recordsUploaded)
        assertEquals(7, fakeServer.getTableRows("academic_subjects").size)

        // Updating an owned subject performs UPSERT without duplicating rows
        val firstSubject = db.academicSubjectDao().getSubjectById(subjectIds.first())!!
        db.academicSubjectDao().updateSubject(firstSubject.copy(name = "Subject 1 Updated"))
        val updateSync = repo.syncNow("ACADEMIC_UPSERT_UPDATE").getOrThrow()
        assertEquals(1, updateSync.recordsUploaded)
        assertEquals(7, fakeServer.getTableRows("academic_subjects").size)
        assertTrue(fakeServer.getTableRows("academic_subjects").values.any {
            it["name"]?.jsonPrimitive?.contentOrNull == "Subject 1 Updated"
        })

        // Another user cannot retrieve or overwrite scholar@solo.org's records
        val dbOther = newDb()
        DatabaseInitializer.ensureSeeded(dbOther)
        val repoOther = buildRepo(dbOther, FakeTokenStorage("device-other"))
        fakeServer.verifyEmail("other@solo.org")
        repoOther.signUp("other@solo.org", "ShadowRank1", "OtherHunter").getOrThrow()
        val otherIdentity = repoOther.signIn("other@solo.org", "ShadowRank1").getOrThrow()
        val otherToken = fakeServer.latestTokenFor(otherIdentity.supabaseUserId)!!
        val otherFetch = fakeServer.fetchTableChanges(otherToken, "academic_subjects", otherIdentity.supabaseUserId, "1970-01-01T00:00:00Z")
        assertTrue((otherFetch as CloudCallResult.Success).data.isEmpty())

        // Attempting to submit a row with another user's user_id is rejected by RLS
        val spoofedRow = buildJsonObject {
            put("id", UUID.randomUUID().toString())
            put("user_id", userId)
            put("name", "Spoofed")
            put("code", "SPF-1")
            put("program_system", "University")
            put("priority", "HIGH")
            put("is_archived", false)
        }
        val spoofedRes = fakeServer.upsertTableRecords(otherToken, "academic_subjects", "[$spoofedRow]")
        assertTrue(spoofedRes is CloudCallResult.Failure)
        assertEquals(403, (spoofedRes as CloudCallResult.Failure).code)

        dbOther.close()
        db.close()
    }

    private class FakeTokenStorage(private val fixedDeviceId: String) : TokenStorage {
        private var access: String? = null
        private var refresh: String? = null
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

        override fun getOrCreateDeviceUuid(): String = fixedDeviceId
    }

    private class FakeSupabaseServer : SupabaseApiPort {
        private data class UserRecord(
            val userId: String,
            val email: String,
            var password: String,
            var displayName: String,
            var verified: Boolean = false
        )

        private val usersByEmail = linkedMapOf<String, UserRecord>()
        private val tokensToUserId = linkedMapOf<String, String>()
        private val devicesByUser = linkedMapOf<String, MutableMap<String, RemoteDeviceDto>>()
        private val tables = linkedMapOf<String, MutableMap<String, JsonObject>>()
        private val backups = linkedMapOf<String, RemoteBackupDto>()
        private val acceptedProgressionKeys = linkedSetOf<String>()
        val deletedUsers = mutableListOf<String>()

        var failRefreshWithExpired: Boolean = false
        var failTableUpsert: Boolean = false

        fun verifyEmail(email: String) {
            usersByEmail[email]?.verified = true
        }

        fun getTableRows(tableName: String): Map<String, JsonObject> =
            tables[tableName]?.toMap().orEmpty()

        fun latestTokenFor(userId: String): String? =
            tokensToUserId.entries.lastOrNull { it.value == userId }?.key

        override fun isConfigured(): Boolean = true

        override fun generateDiagnosticReport(accessToken: String?): SupabaseDiagnosticReport =
            SupabaseDiagnosticReport("YES", "YES", "YES", "VERIFIED", "YES", "SAFE")

        override fun buildOAuthAuthorizeUrl(provider: String, redirectUri: String): String =
            "https://fake.supabase.co/auth/v1/authorize?provider=$provider"

        override suspend fun signUp(email: String, password: String, displayName: String): CloudCallResult<SupabaseAuthResponse> {
            if (usersByEmail.containsKey(email)) {
                return CloudCallResult.Failure(422, "AUTH", "User already registered")
            }
            val uid = UUID.randomUUID().toString()
            val rec = UserRecord(uid, email, password, displayName, verified = false)
            usersByEmail[email] = rec
            val access = "access-$uid"
            tokensToUserId[access] = uid
            return CloudCallResult.Success(
                SupabaseAuthResponse(
                    accessToken = access,
                    refreshToken = "refresh-$uid",
                    expiresInSeconds = 3600L,
                    user = SupabaseUserDto(uid, email, null)
                )
            )
        }

        override suspend fun signIn(email: String, password: String): CloudCallResult<SupabaseAuthResponse> {
            val rec = usersByEmail[email]
            if (rec == null || rec.password != password) {
                return CloudCallResult.Failure(400, "AUTH", "Invalid login credentials")
            }
            val access = "access-${rec.userId}-${System.nanoTime()}"
            tokensToUserId[access] = rec.userId
            return CloudCallResult.Success(
                SupabaseAuthResponse(
                    accessToken = access,
                    refreshToken = "refresh-${rec.userId}",
                    expiresInSeconds = 3600L,
                    user = SupabaseUserDto(
                        id = rec.userId,
                        email = rec.email,
                        emailConfirmedAt = if (rec.verified) "2026-11-01T00:00:00Z" else null
                    )
                )
            )
        }

        override suspend fun refreshSession(refreshToken: String): CloudCallResult<SupabaseAuthResponse> {
            if (failRefreshWithExpired) {
                return CloudCallResult.Failure(401, "AUTH_EXPIRED", "Refresh token expired", isSessionExpired = true)
            }
            val uid = refreshToken.removePrefix("refresh-")
            val rec = usersByEmail.values.find { it.userId == uid }
                ?: return CloudCallResult.Failure(401, "AUTH_EXPIRED", "Unknown token", isSessionExpired = true)
            val access = "access-$uid-refreshed"
            tokensToUserId[access] = uid
            return CloudCallResult.Success(
                SupabaseAuthResponse(
                    accessToken = access,
                    refreshToken = refreshToken,
                    expiresInSeconds = 3600L,
                    user = SupabaseUserDto(uid, rec.email, if (rec.verified) "2026-11-01T00:00:00Z" else null)
                )
            )
        }

        override suspend fun resendVerificationEmail(email: String): CloudCallResult<Unit> = CloudCallResult.Success(Unit)

        override suspend fun sendPasswordRecovery(email: String): CloudCallResult<Unit> = CloudCallResult.Success(Unit)

        override suspend fun updatePassword(accessToken: String, newPassword: String): CloudCallResult<SupabaseUserDto> {
            val uid = tokensToUserId[accessToken] ?: return CloudCallResult.Failure(401, "AUTH", "Unauthorized", isSessionExpired = true)
            val rec = usersByEmail.values.first { it.userId == uid }
            rec.password = newPassword
            return CloudCallResult.Success(SupabaseUserDto(uid, rec.email, "2026-11-01T00:00:00Z"))
        }

        override suspend fun fetchUser(accessToken: String): CloudCallResult<SupabaseUserDto> {
            val uid = tokensToUserId[accessToken] ?: return CloudCallResult.Failure(401, "AUTH", "Unauthorized", isSessionExpired = true)
            val rec = usersByEmail.values.first { it.userId == uid }
            return CloudCallResult.Success(SupabaseUserDto(uid, rec.email, if (rec.verified) "2026-11-01T00:00:00Z" else null))
        }

        override suspend fun signOut(accessToken: String): CloudCallResult<Unit> {
            tokensToUserId.remove(accessToken)
            return CloudCallResult.Success(Unit)
        }

        override suspend fun fetchOrInitializePlayerIdentity(
            accessToken: String,
            userId: String,
            displayName: String
        ): CloudCallResult<RemotePlayerIdentityDto> {
            val rec = usersByEmail.values.find { it.userId == userId }
            return CloudCallResult.Success(
                RemotePlayerIdentityDto(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    displayName = rec?.displayName ?: displayName
                )
            )
        }

        override suspend fun updateDisplayName(
            accessToken: String,
            userId: String,
            displayName: String
        ): CloudCallResult<RemotePlayerIdentityDto> {
            usersByEmail.values.find { it.userId == userId }?.displayName = displayName
            return CloudCallResult.Success(RemotePlayerIdentityDto(id = userId, userId = userId, displayName = displayName))
        }

        override suspend fun upsertDevice(accessToken: String, device: RemoteDeviceDto): CloudCallResult<Unit> {
            devicesByUser.getOrPut(device.userId) { linkedMapOf() }[device.id] = device
            return CloudCallResult.Success(Unit)
        }

        override suspend fun listDevices(accessToken: String, userId: String): CloudCallResult<List<RemoteDeviceDto>> =
            CloudCallResult.Success(devicesByUser[userId]?.values?.toList().orEmpty())

        override suspend fun revokeDevice(accessToken: String, userId: String, deviceId: String): CloudCallResult<Unit> {
            val map = devicesByUser[userId]
            map?.get(deviceId)?.let { map[deviceId] = it.copy(isRevoked = true) }
            return CloudCallResult.Success(Unit)
        }

        override suspend fun upsertTableRecords(
            accessToken: String,
            tableName: String,
            rowsJsonArray: String
        ): CloudCallResult<Unit> {
            if (failTableUpsert) return CloudCallResult.Failure(503, "TRANSIENT", "Simulated 503 outage")
            val authedUid = tokensToUserId[accessToken]
                ?: return CloudCallResult.Failure(401, "AUTH_EXPIRED", "Unauthorized", isSessionExpired = true)
            val arr = CloudDataMapper.json.parseToJsonElement(rowsJsonArray).jsonArray
            val tableMap = tables.getOrPut(tableName) { linkedMapOf() }
            arr.forEach { el ->
                val obj = el.jsonObject
                val rowUserId = obj["user_id"]?.jsonPrimitive?.contentOrNull
                if (rowUserId.isNullOrBlank() || rowUserId != authedUid) {
                    return CloudCallResult.Failure(
                        403,
                        "RLS_FORBIDDEN",
                        "new row violates row-level security policy for table \"$tableName\""
                    )
                }
                val id = obj["id"]?.jsonPrimitive?.contentOrNull
                if (id.isNullOrBlank() || !CloudDataMapper.isValidUuid(id)) {
                    return CloudCallResult.Failure(400, "CLIENT_ERROR", "invalid input syntax for type uuid")
                }
                val enriched = buildJsonObject {
                    obj.forEach { (k, v) -> put(k, v) }
                    put("updated_at", Instant.now().toString())
                }
                tableMap[id] = enriched
            }
            return CloudCallResult.Success(Unit)
        }

        override suspend fun fetchTableChanges(
            accessToken: String,
            tableName: String,
            userId: String,
            updatedSinceIso: String,
            limit: Int,
            offset: Int
        ): CloudCallResult<List<JsonObject>> {
            val all = tables[tableName]?.values
                ?.filter { it["user_id"]?.jsonPrimitive?.contentOrNull == userId }
                ?.toList()
                .orEmpty()
            val page = all.drop(offset).take(limit)
            return CloudCallResult.Success(page)
        }

        override suspend fun validateProgressionEvents(
            accessToken: String,
            deviceId: String,
            events: List<CloudProgressionEventPayload>
        ): CloudCallResult<ProgressionValidateResponse> {
            val accepted = mutableListOf<String>()
            val dups = mutableListOf<String>()
            events.forEach { ev ->
                if (acceptedProgressionKeys.add(ev.idempotencyKey)) {
                    accepted += ev.idempotencyKey
                } else {
                    dups += ev.idempotencyKey
                }
            }
            return CloudCallResult.Success(ProgressionValidateResponse(accepted, dups))
        }

        override suspend fun uploadCloudBackup(accessToken: String, backup: RemoteBackupDto): CloudCallResult<Unit> {
            backups[backup.id] = backup
            return CloudCallResult.Success(Unit)
        }

        override suspend fun listCloudBackups(accessToken: String, userId: String): CloudCallResult<List<RemoteBackupDto>> =
            CloudCallResult.Success(backups.values.filter { it.userId == userId })

        override suspend fun validateCloudRestore(accessToken: String, backupId: String): CloudCallResult<Unit> =
            CloudCallResult.Success(Unit)

        override suspend fun requestAccountDeletion(accessToken: String, confirmationPhrase: String): CloudCallResult<Unit> {
            if (confirmationPhrase != "DELETE MY ACCOUNT") {
                return CloudCallResult.Failure(400, "VALIDATION", "Bad phrase")
            }
            val uid = tokensToUserId[accessToken] ?: return CloudCallResult.Failure(401, "AUTH", "Unauthorized")
            deletedUsers += uid
            usersByEmail.entries.removeIf { it.value.userId == uid }
            devicesByUser.remove(uid)
            tokensToUserId.entries.removeIf { it.value == uid }
            return CloudCallResult.Success(Unit)
        }
    }
}
