package com.example

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SoloLevelingDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.AcademicRepository
import com.example.data.repository.RealityOsRepository
import com.example.data.integration.DeterministicSystemAdvisor
import com.example.data.integration.HealthConnectPort
import com.example.data.integration.HealthConnectReadiness
import com.example.data.integration.SupabaseSyncAdapter
import com.example.data.healthconnect.HealthConnectDataSource
import com.example.data.healthconnect.HealthConnectPermissionState
import com.example.data.healthconnect.HealthConnectSdkAvailability
import com.example.data.healthconnect.HealthConnectSyncFetchResult
import com.example.domain.model.AcademicPriority
import com.example.domain.model.AcademicQuestCategory
import com.example.domain.model.AcademicQuestDifficulty
import com.example.domain.model.AcademicQuestStatus
import com.example.domain.model.AssessmentType
import com.example.domain.model.ChapterDifficulty
import com.example.domain.model.MarkingSystem
import com.example.domain.model.SubjectColorToken
import com.example.domain.model.SubjectIconKey
import com.example.domain.model.VerificationStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AcademicRepositoryRobolectricTest {

    private lateinit var database: SoloLevelingDatabase
    private lateinit var realityOsRepository: RealityOsRepository
    private lateinit var academicRepository: AcademicRepository

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
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun subjectCreationAwardsZeroXp_chapterCompletionRewardsOnceOnly() = runTest {
        val initialPlayer = realityOsRepository.playerProfile.first()
        val subject = academicRepository.createSubject(
            name = "Distributed Systems",
            code = "CS402",
            programSystem = "BSc Computer Science",
            colorToken = SubjectColorToken.CYAN,
            iconKey = SubjectIconKey.CODE,
            priority = AcademicPriority.HIGH,
            notes = "Consensus and replication"
        ).getOrThrow()

        // Zero XP for creating subject, module, chapter, or topic
        val modId = academicRepository.modules.first().first { it.subjectId == subject.id }.id
        val chapId = academicRepository.addChapter(modId, subject.id, "Raft Consensus", ChapterDifficulty.ADVANCED, 90, "").getOrThrow()
        academicRepository.addTopic(chapId, subject.id, "Leader Election", 3, "").getOrThrow()

        assertEquals(initialPlayer.totalXp, realityOsRepository.playerProfile.first().totalXp)

        // First completion awards XP once
        val firstXp = academicRepository.setChapterCompleted(chapId, true).getOrThrow()
        assertTrue(firstXp > 0)
        val xpAfterFirst = realityOsRepository.playerProfile.first().totalXp

        // Unchecking and re-checking MUST award 0 duplicate XP
        academicRepository.setChapterCompleted(chapId, false).getOrThrow()
        val secondXp = academicRepository.setChapterCompleted(chapId, true).getOrThrow()
        assertEquals(0, secondXp)
        assertEquals(xpAfterFirst, realityOsRepository.playerProfile.first().totalXp)
    }

    @Test
    fun studySessionPersistence_distinguishesTimerAndManualAndBlocksDuplicateSourceId() = runTest {
        val subject = academicRepository.createSubject(
            name = "Pathology",
            code = "MED21",
            programSystem = "USMLE Step 1",
            colorToken = SubjectColorToken.EMERALD,
            iconKey = SubjectIconKey.MEDICAL,
            priority = AcademicPriority.CRITICAL,
            notes = ""
        ).getOrThrow()

        val now = System.currentTimeMillis()
        val focusRes = academicRepository.recordStudySession(
            subjectId = subject.id,
            topicId = null,
            title = "Pomodoro Block 1",
            startEpochMs = now - 25 * 60_000L,
            endEpochMs = now,
            durationMinutes = 25,
            breakMinutes = 5,
            isSystemTimer = true,
            notes = "Focused pass",
            sourceId = "focus_unique_1"
        )
        assertTrue(focusRes.isSuccess)
        assertEquals(VerificationStatus.VERIFIED_SYSTEM_TIMER, focusRes.getOrThrow().verificationStatus)

        // Duplicate sourceId is blocked
        val dupRes = academicRepository.recordStudySession(
            subjectId = subject.id,
            topicId = null,
            title = "Pomodoro Block 1",
            startEpochMs = now - 25 * 60_000L,
            endEpochMs = now,
            durationMinutes = 25,
            breakMinutes = 5,
            isSystemTimer = true,
            notes = "Duplicate attempt",
            sourceId = "focus_unique_1"
        )
        assertTrue(dupRes.isFailure)

        // Manual log is SELF_REPORTED
        val manualRes = academicRepository.recordStudySession(
            subjectId = subject.id,
            topicId = null,
            title = "Library Reading",
            startEpochMs = now - 90 * 60_000L,
            endEpochMs = now - 60 * 60_000L,
            durationMinutes = 30,
            breakMinutes = 0,
            isSystemTimer = false,
            notes = "Offline reading"
        )
        assertTrue(manualRes.isSuccess)
        assertEquals(VerificationStatus.SELF_REPORTED, manualRes.getOrThrow().verificationStatus)
        assertEquals(2, academicRepository.studySessions.first().size)
    }

    @Test
    fun revisionsAssessmentsAndQuests_persistAndPreventDoubleReward() = runTest {
        val subject = academicRepository.createSubject(
            name = "Constitutional Law",
            code = "LAW101",
            programSystem = "Bar Prep",
            colorToken = SubjectColorToken.GOLD,
            iconKey = SubjectIconKey.LAW,
            priority = AcademicPriority.HIGH,
            notes = ""
        ).getOrThrow()
        val modId = academicRepository.modules.first().first { it.subjectId == subject.id }.id
        val chapId = academicRepository.addChapter(modId, subject.id, "Judicial Review", ChapterDifficulty.BOSS, 60, "").getOrThrow()
        val topicId = academicRepository.addTopic(chapId, subject.id, "Standing Doctrine", 2, "").getOrThrow()

        // Complete revision
        val rev = academicRepository.completeTopicRevision(topicId, confidenceAfter = 4, durationMinutes = 25, notes = "Recalled key tests").getOrThrow()
        assertTrue(rev.xpAwarded > 0)
        assertTrue(rev.intervalDaysScheduled >= 1)

        // Exam + Assessment evidence
        val exam = academicRepository.createExam(
            subjectId = subject.id,
            title = "Midterm Paper",
            examDateEpochMs = System.currentTimeMillis() + 10 * 24 * 60 * 60_000L,
            markingSystem = MarkingSystem.GPA_4,
            targetScoreDisplay = "3.7 / 4.0",
            targetNormalizedRatio = 0.92f,
            syllabusCoveragePercent = 70,
            notes = ""
        ).getOrThrow()

        val graded = academicRepository.recordAssessmentResult(
            subjectId = subject.id,
            examId = exam.id,
            topicId = topicId,
            title = "Midterm Paper",
            assessmentType = AssessmentType.GRADED_EXAM,
            markingSystem = MarkingSystem.GPA_4,
            scoredMarks = 3.8,
            maxMarks = 4.0,
            gradeLabel = "A",
            hasVerifiedEvidence = true,
            notes = "Official transcript"
        ).getOrThrow()
        assertEquals(VerificationStatus.VERIFIED_ASSESSMENT, graded.verificationStatus)
        assertTrue(graded.xpAwarded > 0)

        // Second graded exam submission on same examId must yield 0 extra XP
        val dupGraded = academicRepository.recordAssessmentResult(
            subjectId = subject.id,
            examId = exam.id,
            topicId = topicId,
            title = "Midterm Regrade",
            assessmentType = AssessmentType.GRADED_EXAM,
            markingSystem = MarkingSystem.GPA_4,
            scoredMarks = 3.9,
            maxMarks = 4.0,
            gradeLabel = "A",
            hasVerifiedEvidence = true,
            notes = "Duplicate exam farm test"
        ).getOrThrow()
        assertEquals(0, dupGraded.xpAwarded)

        // Quest lifecycle: creation 0 XP, complete awards once, cancel transitions status
        val xpBeforeQuest = realityOsRepository.playerProfile.first().totalXp
        val q1 = academicRepository.createAcademicQuest(
            title = "Practice Case Briefs",
            description = "Write 3 case briefs",
            category = AcademicQuestCategory.PRACTICE,
            subjectId = subject.id,
            topicId = topicId,
            difficulty = AcademicQuestDifficulty.D_RANK,
            objective = "Complete 3 briefs",
            targetValue = 3,
            unit = "briefs",
            deadlineEpochMs = null
        ).getOrThrow()
        val q2 = academicRepository.createAcademicQuest(
            title = "Optional Extra Reading",
            description = "Skim commentary",
            category = AcademicQuestCategory.LONG_TERM_MISSION,
            subjectId = subject.id,
            topicId = null,
            difficulty = AcademicQuestDifficulty.E_RANK,
            objective = "Read 20 pages",
            targetValue = 20,
            unit = "pages",
            deadlineEpochMs = null
        ).getOrThrow()
        assertEquals(xpBeforeQuest, realityOsRepository.playerProfile.first().totalXp)

        val questXp = academicRepository.completeAcademicQuest(q1, "3 briefs written").getOrThrow()
        assertTrue(questXp > 0)
        assertEquals(0, academicRepository.completeAcademicQuest(q1, "re-submit").getOrThrow())

        academicRepository.cancelAcademicQuest(q2)
        val quests = academicRepository.academicQuests.first()
        assertEquals(AcademicQuestStatus.COMPLETED, quests.first { it.id == q1 }.status)
        assertEquals(AcademicQuestStatus.CANCELLED, quests.first { it.id == q2 }.status)
    }

    @Test
    fun migration2To3_preservesV2DataAndCreatesAcademicTables() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration_test_v2_v3.db"
        context.deleteDatabase(dbName)

        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(2) {
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
                        db.execSQL(
                            """
                            INSERT INTO `player_profile` VALUES (
                                1, 'Sung', 'Iron Hunter', 6, 40, 790, 'D_RANK',
                                14, 2, 10, 16, 1, 20, 12, 0, 5, 11, 0, 0, 14, 0, 0, 10, 0, 0, 10, 0, 0,
                                5, 4, 2, 88, 1700000000000
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
        assertEquals("Sung", preservedPlayer?.codename)
        assertEquals(6, preservedPlayer?.level)
        assertEquals(0, migratedDb.academicSubjectDao().getActiveSubjectsOnce().size)
        assertEquals(0, migratedDb.studySessionDao().getSessionsSince(0L).size)
        assertEquals(0, migratedDb.academicProgressionDao().getAcademicXpEarnedSince(0L))

        migratedDb.close()
        context.deleteDatabase(dbName)
    }
}
