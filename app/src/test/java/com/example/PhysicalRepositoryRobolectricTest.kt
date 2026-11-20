package com.example

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.healthconnect.ExternalExerciseSample
import com.example.data.healthconnect.HealthConnectDataSource
import com.example.data.healthconnect.HealthConnectPermissionState
import com.example.data.healthconnect.HealthConnectSdkAvailability
import com.example.data.healthconnect.HealthConnectSyncFetchResult
import com.example.data.integration.DeterministicSystemAdvisor
import com.example.data.integration.HealthConnectPort
import com.example.data.integration.HealthConnectReadiness
import com.example.data.integration.SupabaseSyncAdapter
import com.example.data.local.SoloLevelingDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.RealityOsRepository
import com.example.domain.catalog.ExerciseCatalog
import com.example.domain.model.ActivityValidationOutcome
import com.example.domain.model.ExerciseRecord
import com.example.domain.model.PhysicalActivityKind
import com.example.domain.model.RecoveryState
import com.example.domain.model.VerificationStatus
import com.example.domain.model.WorkoutCategory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PhysicalRepositoryRobolectricTest {

    private lateinit var database: SoloLevelingDatabase
    private lateinit var fakeHealthConnect: FakeHealthConnectDataSource
    private lateinit var repository: RealityOsRepository

    private class FakeHealthConnectDataSource : HealthConnectDataSource {
        var sdkAvailability = HealthConnectSdkAvailability.AVAILABLE
        var permissionState = HealthConnectPermissionState(
            exerciseReadGranted = true,
            distanceReadGranted = true,
            heartRateReadGranted = true,
            sleepReadGranted = true
        )
        var samples: List<ExternalExerciseSample> = emptyList()
        var sleepHours: Double? = null
        var restingHr: Int? = null
        var error: String? = null

        override fun getAvailability(): HealthConnectSdkAvailability = sdkAvailability
        override suspend fun getPermissionState(): HealthConnectPermissionState = permissionState
        override suspend fun fetchRecentActivity(sinceEpochMs: Long, untilEpochMs: Long): HealthConnectSyncFetchResult {
            return HealthConnectSyncFetchResult(
                permissionState = permissionState,
                exerciseSamples = samples,
                sleepHoursLast24h = sleepHours,
                restingHeartRateEstimateBpm = restingHr,
                errorMessage = error
            )
        }
    }

    @Before
    fun setup() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SoloLevelingDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        fakeHealthConnect = FakeHealthConnectDataSource()
        val prefs = UserPreferencesRepository(context)
        val legacyPort = object : HealthConnectPort {
            override fun checkReadiness() = HealthConnectReadiness.AVAILABLE
        }
        repository = RealityOsRepository(
            database = database,
            preferencesRepository = prefs,
            healthConnectPort = legacyPort,
            healthConnectDataSource = fakeHealthConnect,
            cloudSyncPort = SupabaseSyncAdapter(),
            aiGameMasterPort = DeterministicSystemAdvisor()
        )
        repository.ensureInitialized()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun healthConnectIngestion_importsVerifiedSessionsAndPreventsDuplicates() = runTest {
        val now = System.currentTimeMillis()
        val sample = ExternalExerciseSample(
            sourceId = "hc-run-001",
            originPackage = "com.google.android.apps.fitness",
            exerciseTypeCode = 56,
            activityKind = PhysicalActivityKind.RUNNING,
            title = "Evening Park Run",
            startEpochMs = now - 35 * 60_000L,
            endEpochMs = now - 5 * 60_000L,
            distanceMeters = 5000.0,
            avgHeartRateBpm = 152
        )
        fakeHealthConnect.samples = listOf(sample)
        fakeHealthConnect.sleepHours = 7.5
        fakeHealthConnect.restingHr = 62

        val firstSync = repository.syncHealthConnectHistory()
        assertEquals(1, firstSync.importedCount)
        assertEquals(0, firstSync.duplicateCount)
        assertTrue(firstSync.totalXpAwarded > 0)

        val jogs = repository.joggingSessions.first()
        assertEquals(1, jogs.size)
        assertEquals(VerificationStatus.VERIFIED_HEALTH_CONNECT, jogs.first().verificationStatus)
        assertEquals("com.google.android.apps.fitness", jogs.first().originApp)
        assertEquals(5000.0, jogs.first().distanceMeters ?: 0.0, 0.1)
        assertEquals(152, jogs.first().avgHeartRateBpm)

        val profile = repository.playerProfile.first()
        assertEquals(1, profile.verifiedActivitiesCount)
        assertEquals(0, profile.selfReportedActivitiesCount)

        // Second sync with identical sourceId must skip duplicate
        val secondSync = repository.syncHealthConnectHistory()
        assertEquals(0, secondSync.importedCount)
        assertEquals(1, secondSync.duplicateCount)
        assertEquals(0, secondSync.totalXpAwarded)

        // Overlapping timestamp with different sourceId must also be skipped
        fakeHealthConnect.samples = listOf(sample.copy(sourceId = "hc-run-overlap"))
        val overlapSync = repository.syncHealthConnectHistory()
        assertEquals(0, overlapSync.importedCount)
        assertEquals(1, overlapSync.duplicateCount)
    }

    @Test
    fun healthConnectIngestion_rejectsInvalidSessionsWithoutReward() = runTest {
        val now = System.currentTimeMillis()
        fakeHealthConnect.samples = listOf(
            ExternalExerciseSample(
                sourceId = "hc-invalid-zero",
                originPackage = "com.example.tracker",
                exerciseTypeCode = 56,
                activityKind = PhysicalActivityKind.JOGGING,
                title = "Broken Session",
                startEpochMs = now - 1000L,
                endEpochMs = now - 2000L // invalid end < start
            )
        )

        val summary = repository.syncHealthConnectHistory()
        assertEquals(0, summary.importedCount)
        assertEquals(1, summary.rejectedCount)
        assertEquals(0, summary.totalXpAwarded)

        val imported = repository.importedHealthActivities.first()
        assertEquals(1, imported.size)
        assertEquals(ActivityValidationOutcome.INVALID_REJECTED, imported.first().validationOutcome)
    }

    @Test
    fun selfReportedJoggingAndStructuredWorkouts_persistWithoutFabricatingMetrics() = runTest {
        val now = System.currentTimeMillis()
        val jogResult = repository.recordJoggingSession(
            title = "Manual Jog",
            startEpochMs = now - 30 * 60_000L,
            endEpochMs = now,
            durationMinutes = 30,
            distanceMeters = null,
            isLiveSystemTimer = false,
            notes = "Neighborhood loop"
        )
        assertTrue(jogResult.isSuccess)
        val savedJog = jogResult.getOrThrow()
        assertEquals(VerificationStatus.SELF_REPORTED, savedJog.verificationStatus)
        assertNull(savedJog.distanceMeters)
        assertNull(savedJog.avgPaceSecPerKm)
        assertNull(savedJog.avgHeartRateBpm)

        val squat = ExerciseCatalog.allExercises.first { it.id == "barbell_squat" }
        val workoutResult = repository.recordStructuredWorkout(
            title = "Lower Body Strength",
            category = WorkoutCategory.STRENGTH,
            durationMinutes = 40,
            exercises = listOf(
                ExerciseRecord(
                    exerciseId = squat.id,
                    exerciseName = squat.name,
                    category = squat.category,
                    setsCompleted = 4,
                    repsPerSet = 8,
                    weightKg = 60.0,
                    restSeconds = 90,
                    primaryAttribute = squat.primaryAttribute
                )
            ),
            isLiveSystemTimer = false,
            notes = "Clean reps"
        )
        assertTrue(workoutResult.isSuccess)

        val workouts = repository.workoutSessions.first()
        assertEquals(1, workouts.size)
        assertEquals(4, workouts.first().totalSets)
        assertEquals(32, workouts.first().totalReps)
        assertEquals(1, workouts.first().exercises.size)

        val profile = repository.playerProfile.first()
        assertEquals(0, profile.verifiedActivitiesCount)
        assertEquals(2, profile.selfReportedActivitiesCount)
    }

    @Test
    fun recoveryCheckIn_updatesPersistedAssessmentInDatabase() = runTest {
        repository.submitRecoveryCheckIn(
            fatigueLevel = 5,
            hasDiscomfort = true,
            discomfortNotes = "Achilles tightness"
        )

        val assessment = repository.latestRecoveryAssessment.first()
        assertEquals(RecoveryState.REST_RECOMMENDED, assessment.state)
        assertTrue(assessment.selfReportedDiscomfort)
        assertEquals(5, assessment.selfReportedFatigue)
    }

    @Test
    fun migration1To2_preservesV1PlayerDataAndCreatesPhysicalTables() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration_test_v1_v2.db"
        context.deleteDatabase(dbName)

        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
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
                        db.execSQL(
                            """
                            INSERT INTO `player_profile` VALUES (
                                1, 'Jinwoo', 'Awakened Novice', 3, 25, 275, 'E_RANK',
                                12, 0, 0, 14, 0, 0, 11, 0, 0, 10, 0, 0, 13, 0, 0, 10, 0, 0, 10, 0, 0,
                                2, 1, 2, 90, 1700000000000
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

        val preservedPlayer = migratedDb.playerDao().getPlayerOnce()
        assertEquals("Jinwoo", preservedPlayer?.codename)
        assertEquals(3, preservedPlayer?.level)
        assertEquals(0, migratedDb.joggingSessionDao().getSessionsSince(0L).size)
        assertEquals(0, migratedDb.workoutSessionDao().getWorkoutsSince(0L).size)

        migratedDb.close()
        context.deleteDatabase(dbName)
    }
}
