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
import com.example.data.local.DatabaseInitializer
import com.example.data.local.SoloLevelingDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.AcademicRepository
import com.example.data.repository.AiSystemRepository
import com.example.data.repository.LifeManagementRepository
import com.example.data.repository.RealityOsRepository
import com.example.domain.model.AcademicPriority
import com.example.domain.model.AiConnectionStatus
import com.example.domain.model.AttributeType
import com.example.domain.model.GoalTimeframe
import com.example.domain.model.HabitCategory
import com.example.domain.model.HabitDayStatus
import com.example.domain.model.HabitType
import com.example.domain.model.PersonalTaskStatus
import com.example.domain.model.RoutineActivityStatus
import com.example.domain.model.RoutineTimeSlot
import com.example.domain.model.ScheduledSourceType
import com.example.domain.model.SubjectColorToken
import com.example.domain.model.SubjectIconKey
import com.example.domain.model.TaskCategory
import com.example.domain.model.TaskPriority
import com.example.domain.model.TaskRecurrenceType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LifeManagementRepositoryRobolectricTest {

    private lateinit var database: SoloLevelingDatabase
    private lateinit var repository: LifeManagementRepository
    private lateinit var realityOsRepository: RealityOsRepository
    private lateinit var academicRepository: AcademicRepository
    private lateinit var aiRepository: AiSystemRepository

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
        DatabaseInitializer.ensureSeeded(database)
        val prefs = UserPreferencesRepository(context)
        repository = LifeManagementRepository(database = database, preferencesRepository = prefs)
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
        academicRepository = AcademicRepository(database = database, preferencesRepository = prefs)
        val fakeGateway = object : BackendAiGateway {
            override fun streamChat(backendBaseUrl: String, request: BackendAiProxyRequest): Flow<AiStreamEvent> = flow {
                emit(AiStreamEvent.Completed(fullText = "", status = AiConnectionStatus.BACKEND_UNCONFIGURED))
            }

            override suspend fun requestStructured(backendBaseUrl: String, request: BackendAiProxyRequest) =
                AiStreamEvent.Completed(fullText = "", status = AiConnectionStatus.BACKEND_UNCONFIGURED)
        }
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
    fun `routines persist reorder steps and award xp once per cycle with anti-farming`() = runTest {
        val routineId = repository.createRoutine(
            title = "Morning Awakening",
            description = "Core morning flow",
            timeSlot = RoutineTimeSlot.MORNING,
            startMinuteOfDay = 7 * 60,
            scheduledDays = DayOfWeek.entries.toSet(),
            initialActivities = listOf("Cold Water" to 5, "Mobility" to 10)
        )
        val activities = repository.observeRoutineActivities().first().filter { it.routineId == routineId }
        assertEquals(2, activities.size)
        assertEquals("Cold Water", activities[0].title)

        // Move second activity up
        repository.moveRoutineActivity(routineId, activities[1].id, moveUp = true)
        val reordered = repository.observeRoutineActivities().first().filter { it.routineId == routineId }
        assertEquals("Mobility", reordered[0].title)

        // No XP before completing all required activities
        val initialXp = database.playerDao().getPlayerOnce()!!.totalXp
        val step1 = repository.setRoutineActivityStatus(routineId, reordered[0].id, RoutineActivityStatus.COMPLETED)
        assertEquals(0, step1.xpAwarded)
        assertEquals(initialXp, database.playerDao().getPlayerOnce()!!.totalXp)

        // Completing final required activity awards XP
        val step2 = repository.setRoutineActivityStatus(routineId, reordered[1].id, RoutineActivityStatus.COMPLETED)
        assertTrue(step2.xpAwarded > 0)
        val xpAfterCompletion = database.playerDao().getPlayerOnce()!!.totalXp
        assertTrue(xpAfterCompletion > initialXp)

        // Resetting and re-completing same day must not double award XP
        repository.setRoutineActivityStatus(routineId, reordered[1].id, RoutineActivityStatus.PENDING)
        val replay = repository.setRoutineActivityStatus(routineId, reordered[1].id, RoutineActivityStatus.COMPLETED)
        assertEquals(0, replay.xpAwarded)
        assertEquals(xpAfterCompletion, database.playerDao().getPlayerOnce()!!.totalXp)
    }

    @Test
    fun `habits support partial skip rest exemption without spurious xp`() = runTest {
        val habitId = repository.createHabit(
            title = "Meditation",
            description = "Breath control",
            habitType = HabitType.DURATION,
            category = HabitCategory.MINDSET,
            targetValue = 20.0,
            unit = "min",
            scheduledDays = DayOfWeek.entries.toSet()
        )
        val initialXp = database.playerDao().getPlayerOnce()!!.totalXp

        // Partial log gives 0 XP
        val partialFeedback = repository.logHabitProgress(habitId, achievedValue = 10.0)
        assertEquals(0, partialFeedback.xpAwarded)
        assertEquals(initialXp, database.playerDao().getPlayerOnce()!!.totalXp)

        // Skip log gives 0 XP
        val skipFeedback = repository.logHabitProgress(
            habitId = habitId,
            explicitStatus = HabitDayStatus.SKIPPED,
            skipReason = "Rest day"
        )
        assertEquals(0, skipFeedback.xpAwarded)

        // Full completion awards WIS & DIS
        val completeFeedback = repository.logHabitProgress(habitId, achievedValue = 20.0)
        assertTrue(completeFeedback.xpAwarded > 0)
        val player = database.playerDao().getPlayerOnce()!!
        assertTrue(database.lifeProgressionDao().getLifeXpSince(0L) > 0)
        assertTrue(player.wisSelfReported > 0 || player.wisProgressXp > 0)
        assertTrue(player.disSelfReported > 0 || player.disProgressXp > 0)
    }

    @Test
    fun `creating and postponing tasks yields zero xp while completing recurring task creates next occurrence`() = runTest {
        val today = LocalDate.now().toEpochDay()
        val startXp = database.playerDao().getPlayerOnce()!!.totalXp

        val taskId = repository.createTask(
            title = "Weekly Desk Reset",
            description = "Organize workspace and clear inbox",
            category = TaskCategory.HOUSEHOLD,
            priority = TaskPriority.MEDIUM,
            dueDateEpochDay = today,
            scheduledStartMinute = 18 * 60,
            estimatedMinutes = 25,
            linkedGoalId = null,
            isTimeSensitive = false,
            recurrenceType = TaskRecurrenceType.WEEKLY,
            selectedDays = setOf(LocalDate.now().dayOfWeek)
        )
        assertEquals(startXp, database.playerDao().getPlayerOnce()!!.totalXp)

        repository.postponeTask(taskId, days = 1L)
        assertEquals(startXp, database.playerDao().getPlayerOnce()!!.totalXp)
        val postponed = repository.observeTasks().first().first { it.id == taskId }
        assertEquals(PersonalTaskStatus.POSTPONED, postponed.status)
        assertEquals(1, postponed.postponeCount)

        val completeRes = repository.completeTask(taskId)
        assertTrue(completeRes.xpAwarded > 0)

        val allTasks = repository.observeTasks().first()
        assertEquals(2, allTasks.size) // Original completed + next recurring PENDING
        assertTrue(allTasks.any { it.id != taskId && it.status == PersonalTaskStatus.PENDING })
    }

    @Test
    fun `goals track milestone completions and protect against arbitrary progress inflation`() = runTest {
        val today = LocalDate.now().toEpochDay()
        val goalId = repository.createGoal(
            title = "Financial Discipline Q3",
            description = "Zero late fees and weekly budget audit",
            timeframe = GoalTimeframe.QUARTERLY,
            targetAttribute = AttributeType.WIS,
            completionCriteria = "Complete 2 milestones",
            startDateEpochDay = today,
            targetDateEpochDay = today + 60L,
            initialMilestones = listOf("Setup automated savings", "Audit subscriptions")
        )

        var details = repository.observeGoalsWithDetails().first().first { it.goal.id == goalId }
        assertEquals(0, details.computedProgressPercent)

        val firstMilestoneId = details.milestones.first().id
        val res = repository.completeGoalMilestone(firstMilestoneId)
        assertTrue(res.xpAwarded > 0)

        details = repository.observeGoalsWithDetails().first().first { it.goal.id == goalId }
        assertEquals(50, details.computedProgressPercent)
        assertEquals(1, details.completedMilestonesCount)

        val reviewRes = repository.recordGoalReview(goalId, "Mid-quarter review", "Adjusted savings target")
        assertTrue(reviewRes.xpAwarded > 0)
    }

    @Test
    fun `unified calendar integrates academic study by reference and allows rescheduling life events only`() = runTest {
        val today = LocalDate.now().toEpochDay()
        val startXp = database.playerDao().getPlayerOnce()!!.totalXp

        // Create Academic subject + study session today
        val subject = academicRepository.createSubject(
            name = "Systems Engineering",
            code = "SYS201",
            programSystem = "BSc",
            colorToken = SubjectColorToken.CYAN,
            iconKey = SubjectIconKey.CODE,
            priority = AcademicPriority.HIGH,
            notes = ""
        ).getOrThrow()
        val nowMs = System.currentTimeMillis()
        academicRepository.recordStudySession(
            subjectId = subject.id,
            topicId = null,
            title = "Kernel Notes",
            startEpochMs = nowMs - 40 * 60_000L,
            endEpochMs = nowMs - 10 * 60_000L,
            durationMinutes = 30,
            breakMinutes = 0,
            isSystemTimer = true,
            notes = ""
        ).getOrThrow()

        // Create custom schedule event
        val (refId, _) = repository.createCustomScheduledEvent(
            title = "Deep Cleaning",
            subtitle = "Kitchen",
            dateEpochDay = today,
            startMinuteOfDay = 15 * 60,
            durationMinutes = 45
        )
        assertTrue(refId > 0L)

        val dayItems = repository.buildUnifiedItemsForRange(today, today)[today].orEmpty()
        val studyItem = dayItems.firstOrNull { it.sourceType == ScheduledSourceType.ACADEMIC_STUDY }
        val customItem = dayItems.first { it.sourceType == ScheduledSourceType.CUSTOM_EVENT }
        assertNotNull(studyItem)
        assertTrue(studyItem!!.isReadOnlyExternal)
        assertFalse(customItem.isReadOnlyExternal)

        // Attempt to reschedule read-only academic session -> rejected cleanly
        val xpBeforeReschedule = database.playerDao().getPlayerOnce()!!.totalXp
        val externalMsg = repository.rescheduleItem(studyItem, today + 1L, 10 * 60)
        assertEquals(0, externalMsg.xpAwarded)
        assertTrue(externalMsg.message.contains("cannot be altered", ignoreCase = true))

        // Reschedule custom event -> succeeds with 0 XP
        val customRes = repository.rescheduleItem(customItem, today + 1L, 11 * 60)
        assertEquals(0, customRes.xpAwarded)
        assertEquals(xpBeforeReschedule, database.playerDao().getPlayerOnce()!!.totalXp)
        assertTrue(xpBeforeReschedule >= startXp)

        val nextDayItems = repository.buildUnifiedItemsForRange(today + 1L, today + 1L)[today + 1L].orEmpty()
        assertTrue(nextDayItems.any { it.title == "Deep Cleaning" && it.startMinuteOfDay == 11 * 60 })
    }

    @Test
    fun `ai system context and development analyst incorporate milestone 5 life telemetry`() = runTest {
        val today = LocalDate.now().toEpochDay()
        repository.createTask(
            title = "Overdue Vehicle Inspection",
            description = "Safety check",
            category = TaskCategory.ADMINISTRATIVE,
            priority = TaskPriority.HIGH,
            dueDateEpochDay = today - 2L,
            scheduledStartMinute = null,
            estimatedMinutes = 40,
            linkedGoalId = null,
            isTimeSensitive = true
        )
        repository.evaluateAndPersistTodayDiscipline()

        val snapshot = aiRepository.buildContextSnapshot()
        assertEquals(1, snapshot.lifeSummary?.overdueTasksCount)

        val (_, report) = aiRepository.analyzeDevelopment()
        assertTrue(
            report.weaknesses.any { it.contains("overdue", ignoreCase = true) } ||
                report.recommendedNextActions.any { it.contains("overdue", ignoreCase = true) }
        )

        val proposals = aiRepository.generateQuestProposals()
        assertTrue(proposals.count > 0)
    }

    @Test
    fun `migration 4 to 5 preserves v4 data and initializes milestone 5 tables and notification flags`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "solo_leveling_v4_to_v5_migration_test.db"
        context.deleteDatabase(dbName)

        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(4) {
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

                        db.execSQL(
                            """
                            INSERT INTO `player_profile` VALUES (
                                1, 'Igris', 'Commander', 12, 50, 2600, 'C_RANK',
                                20, 1, 10, 20, 1, 10, 18, 0, 5, 19, 1, 5, 22, 1, 10, 16, 0, 5, 13, 0, 0,
                                12, 12, 6, 95, 1700000000000
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            INSERT INTO `notification_preferences` VALUES (
                                1, 1, 1, 1, 1, 1, 1, 1, 1, 22, 7, 10, 1700000000000
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            INSERT INTO `ai_consent_records` VALUES (
                                1, 1, 1, 1, 1, 1, 1700000000000
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

        runTest {
            val player = migratedDb.playerDao().getPlayerOnce()
            assertEquals("Igris", player?.codename)
            assertEquals(12, player?.level)

            val notifPrefs = migratedDb.aiConfigDao().getNotificationPreferencesOnce()!!
            assertTrue(notifPrefs.enableHabitReminders)
            assertTrue(notifPrefs.enableRoutineReminders)
            assertTrue(notifPrefs.enableTaskDeadlines)
            assertTrue(notifPrefs.enableOverdueTasks)
            assertTrue(notifPrefs.enableGoalMilestones)
            assertTrue(notifPrefs.enableDailyPlanningReminders)
            assertTrue(notifPrefs.enableWeeklyReviewReminders)

            val consent = migratedDb.aiConfigDao().getConsentRecordOnce()!!
            assertTrue(consent.shareLifeWithAi)

            assertEquals(0, migratedDb.routineDao().getAllRoutinesOnce().size)
            assertEquals(0, migratedDb.habitDao().getAllHabitsOnce().size)
            assertEquals(0, migratedDb.personalTaskDao().getAllTasksOnce().size)
            assertEquals(0, migratedDb.goalDao().getAllGoalsOnce().size)
        }

        migratedDb.close()
        context.deleteDatabase(dbName)
    }
}
