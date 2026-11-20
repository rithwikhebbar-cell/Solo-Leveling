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
import com.example.data.local.entity.AiDungeonProposalEntity
import com.example.data.local.entity.JoggingSessionEntity
import com.example.data.local.entity.RecoveryAssessmentEntity
import com.example.data.local.entity.StudySessionEntity
import com.example.data.local.entity.toEntity
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.AcademicRepository
import com.example.data.repository.AiSystemRepository
import com.example.data.repository.RealityOsRepository
import com.example.data.repository.RpgRepository
import com.example.domain.model.AcademicActivityKind
import com.example.domain.model.AcademicPriority
import com.example.domain.model.AiConnectionStatus
import com.example.domain.model.AssessmentType
import com.example.domain.model.AttributeBreakdown
import com.example.domain.model.AttributeType
import com.example.domain.model.BossStatus
import com.example.domain.model.ChapterDifficulty
import com.example.domain.model.DungeonCategory
import com.example.domain.model.DungeonDifficulty
import com.example.domain.model.DungeonEvidenceType
import com.example.domain.model.HunterRank
import com.example.domain.model.MarkingSystem
import com.example.domain.model.PhysicalActivityKind
import com.example.domain.model.PlayerAttributes
import com.example.domain.model.PlayerProfile
import com.example.domain.model.ProposalStatus
import com.example.domain.model.RecoveryState
import com.example.domain.model.SubjectColorToken
import com.example.domain.model.SubjectIconKey
import com.example.domain.model.VerificationStatus
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
class RpgSystemRepositoryRobolectricTest {

    private lateinit var database: SoloLevelingDatabase
    private lateinit var prefs: UserPreferencesRepository
    private lateinit var realityOsRepository: RealityOsRepository
    private lateinit var academicRepository: AcademicRepository
    private lateinit var aiSystemRepository: AiSystemRepository
    private lateinit var rpgRepository: RpgRepository

    private val stubHc = object : HealthConnectDataSource {
        override fun getAvailability() = HealthConnectSdkAvailability.AVAILABLE
        override suspend fun getPermissionState() = HealthConnectPermissionState()
        override suspend fun fetchRecentActivity(sinceEpochMs: Long, untilEpochMs: Long) =
            HealthConnectSyncFetchResult(HealthConnectPermissionState(), emptyList())
    }

    private val stubGateway = object : BackendAiGateway {
        override fun streamChat(backendBaseUrl: String, request: BackendAiProxyRequest): Flow<AiStreamEvent> = flow { }
        override suspend fun requestStructured(backendBaseUrl: String, request: BackendAiProxyRequest) =
            AiStreamEvent.Completed(fullText = "", status = AiConnectionStatus.LOCAL_FALLBACK)
    }

    @Before
    fun setup() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SoloLevelingDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        prefs = UserPreferencesRepository(context)
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
        aiSystemRepository = AiSystemRepository(
            database = database,
            preferencesRepository = prefs,
            realityOsRepository = realityOsRepository,
            academicRepository = academicRepository,
            backendAiGateway = stubGateway
        )
        rpgRepository = RpgRepository(database, prefs)
        rpgRepository.ensureRpgSeededAndSynced()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun dungeonLifecycle_zeroXpOnEntry_validatesRealEvidence_preventsDuplicateClaim_andAllowsPenaltyFreeAbandon() = runTest {
        val initialSnap = rpgRepository.rpgStateFlow.first()
        assertTrue(initialSnap.dungeons.any { it.definition.code == "sys_e_first_stride" })
        assertTrue(initialSnap.dungeons.any { it.definition.code.startsWith("daily_gate_") })
        val xpBefore = initialSnap.profile.totalXp

        val gate = initialSnap.dungeons.first { it.definition.code == "sys_e_first_stride" }
        val t0 = 1_700_000_000_000L
        val instId = rpgRepository.enterDungeon(gate.definition.id, nowEpochMs = t0).getOrThrow()

        // Entering a dungeon grants 0 XP
        assertEquals(xpBefore, rpgRepository.rpgStateFlow.first().profile.totalXp)

        // Attempting to clear with zero logged progress must fail
        val failClear = rpgRepository.completeDungeon(instId, nowEpochMs = t0 + 1_000L)
        assertTrue(failClear.isFailure)

        // Log real jog & study sessions AFTER entry
        val subj = academicRepository.createSubject(
            name = "Systems",
            code = "CS201",
            programSystem = "CS",
            colorToken = SubjectColorToken.CYAN,
            iconKey = SubjectIconKey.CODE,
            priority = AcademicPriority.HIGH,
            notes = ""
        ).getOrThrow()

        database.joggingSessionDao().insert(
            JoggingSessionEntity(
                sourceId = "jog_dungeon_test_1",
                originApp = "HealthConnect",
                title = "Real Run",
                activityKindName = PhysicalActivityKind.JOGGING.name,
                startEpochMs = t0 + 5_000L,
                endEpochMs = t0 + 35 * 60_000L,
                durationMinutes = 35,
                distanceMeters = 4500.0,
                avgPaceSecPerKm = 460,
                avgHeartRateBpm = 148,
                verificationStatusName = VerificationStatus.VERIFIED_HEALTH_CONNECT.name,
                xpAwarded = 55,
                notes = "Real run",
                createdAtEpochMs = t0 + 35 * 60_000L
            )
        )
        database.studySessionDao().insertSession(
            StudySessionEntity(
                sourceId = "study_dungeon_test_1",
                subjectId = subj.id,
                subjectName = subj.name,
                topicId = null,
                topicTitle = "",
                title = "System Focus",
                activityKindName = AcademicActivityKind.FOCUS_SESSION.name,
                startEpochMs = t0 + 40 * 60_000L,
                endEpochMs = t0 + 70 * 60_000L,
                durationMinutes = 30,
                breakMinutes = 0,
                verificationStatusName = VerificationStatus.VERIFIED_SYSTEM_TIMER.name,
                xpAwarded = 40,
                notes = "Deep study"
            )
        )

        val clearRes = rpgRepository.completeDungeon(instId, nowEpochMs = t0 + 75 * 60_000L)
        assertTrue(clearRes.isSuccess)

        val afterSnap = rpgRepository.rpgStateFlow.first()
        assertTrue(afterSnap.profile.totalXp > xpBefore)
        assertTrue(afterSnap.completionHistory.any { it.dungeonId == gate.definition.id })
        assertTrue(afterSnap.titles.first { it.id == "title_gate_runner" }.isUnlocked)
        assertTrue(afterSnap.inventoryItems.first { it.definition.id == "item_badge_cyan_initiate" }.isOwned)

        // Duplicate claim on the same instance must be rejected
        val duplicateClaim = rpgRepository.completeDungeon(instId, nowEpochMs = t0 + 80 * 60_000L)
        assertTrue(duplicateClaim.isFailure)

        // Entering another gate & abandoning incurs 0 XP penalty
        val vaultGate = afterSnap.dungeons.first { it.definition.code == "sys_e_discipline_crypt" }
        val vaultInst = rpgRepository.enterDungeon(vaultGate.definition.id, nowEpochMs = t0 + 90 * 60_000L).getOrThrow()
        val xpBeforeAbandon = rpgRepository.rpgStateFlow.first().profile.totalXp
        assertTrue(rpgRepository.abandonDungeon(vaultInst).isSuccess)
        assertEquals(xpBeforeAbandon, rpgRepository.rpgStateFlow.first().profile.totalXp)
    }

    @Test
    fun bossBattle_reducesHpFromRealProgress_allowsVictoryWhenComplete_andRetreatWithoutPenalty() = runTest {
        val snap = rpgRepository.rpgStateFlow.first()
        val wardenBoss = snap.bossBattles.first { it.definition.code == "boss_discipline_warden" }
        val t0 = 1_700_000_000_000L
        val bossInstId = rpgRepository.startBossBattle(wardenBoss.definition.id, nowEpochMs = t0).getOrThrow()

        // Progress not yet complete -> claim keeps battle ACTIVE and returns progress status
        val partialMsg = rpgRepository.refreshAndClaimBossBattle(bossInstId, nowEpochMs = t0 + 1_000L).getOrThrow()
        assertTrue(partialMsg.contains("HP at 100%"))

        // Retreat incurs zero XP penalty
        val xpBeforeRetreat = rpgRepository.rpgStateFlow.first().profile.totalXp
        assertTrue(rpgRepository.abandonBossBattle(bossInstId, nowEpochMs = t0 + 2_000L).isSuccess)
        assertEquals(xpBeforeRetreat, rpgRepository.rpgStateFlow.first().profile.totalXp)

        // Start a fresh Academic Sentinel battle and complete all 4 phases with real telemetry
        val academicBoss = snap.bossBattles.first { it.definition.code == "boss_academic_sentinel" }
        val acadInstId = rpgRepository.startBossBattle(academicBoss.definition.id, nowEpochMs = t0 + 10_000L).getOrThrow()

        val subj = academicRepository.createSubject(
            name = "Algorithms",
            code = "CS301",
            programSystem = "Computer Science",
            colorToken = SubjectColorToken.VIOLET,
            iconKey = SubjectIconKey.CODE,
            priority = AcademicPriority.CRITICAL,
            notes = ""
        ).getOrThrow()
        val modId = academicRepository.addModule(subj.id, "Graphs", "").getOrThrow()
        val ch1 = academicRepository.addChapter(modId, subj.id, "BFS & DFS", ChapterDifficulty.INTERMEDIATE, 60, "").getOrThrow()
        val ch2 = academicRepository.addChapter(modId, subj.id, "Shortest Paths", ChapterDifficulty.ADVANCED, 60, "").getOrThrow()
        academicRepository.setChapterCompleted(ch1, true)
        academicRepository.setChapterCompleted(ch2, true)
        val topicId = academicRepository.addTopic(ch1, subj.id, "Dijkstra", 4, "").getOrThrow()
        academicRepository.completeTopicRevision(topicId, confidenceAfter = 5, durationMinutes = 20, notes = "Pass 1")
        academicRepository.completeTopicRevision(topicId, confidenceAfter = 5, durationMinutes = 20, notes = "Pass 2")
        academicRepository.completeTopicRevision(topicId, confidenceAfter = 5, durationMinutes = 20, notes = "Pass 3")
        val exam = academicRepository.createExam(
            subjectId = subj.id,
            title = "Midterm",
            examDateEpochMs = System.currentTimeMillis() + 86_400_000L,
            markingSystem = MarkingSystem.PERCENTAGE,
            targetScoreDisplay = "80%",
            targetNormalizedRatio = 0.8f,
            syllabusCoveragePercent = 100,
            notes = ""
        ).getOrThrow()
        academicRepository.recordAssessmentResult(
            subjectId = subj.id,
            examId = exam.id,
            topicId = topicId,
            title = "Mock Exam",
            assessmentType = AssessmentType.MOCK_EXAM,
            markingSystem = MarkingSystem.PERCENTAGE,
            scoredMarks = 88.0,
            maxMarks = 100.0,
            gradeLabel = "A",
            hasVerifiedEvidence = false,
            notes = ""
        )
        database.studySessionDao().insertSession(
            StudySessionEntity(
                sourceId = "boss_acad_study_1",
                subjectId = subj.id,
                subjectName = subj.name,
                topicId = topicId,
                topicTitle = "Dijkstra",
                title = "Boss Study Block",
                activityKindName = AcademicActivityKind.FOCUS_SESSION.name,
                startEpochMs = t0 + 20_000L,
                endEpochMs = t0 + 20_000L + 100 * 60_000L,
                durationMinutes = 100,
                breakMinutes = 0,
                verificationStatusName = VerificationStatus.VERIFIED_SYSTEM_TIMER.name,
                xpAwarded = 80,
                notes = ""
            )
        )

        val victoryRes = rpgRepository.refreshAndClaimBossBattle(acadInstId, nowEpochMs = t0 + 110 * 60_000L)
        assertTrue(victoryRes.isSuccess)
        assertTrue(victoryRes.getOrThrow().contains("Boss Conquered"))

        val postBossSnap = rpgRepository.rpgStateFlow.first()
        assertEquals(BossStatus.COMPLETED, postBossSnap.bossBattles.first { it.definition.id == academicBoss.definition.id }.effectiveStatus)
        assertTrue(postBossSnap.titles.first { it.id == "title_monolith_breaker" }.isUnlocked)
        assertTrue(postBossSnap.shadowCollection.first { it.definition.id == "shadow_astral_prism" }.isUnlocked)
    }

    @Test
    fun skillsTitlesInventoryAndAiSafetyGuard_enforceRulesAccurately() = runTest {
        // Locked skill must fail when attributes are too low
        assertTrue(rpgRepository.unlockOrUpgradeSkill("acad_retention").isFailure)

        // Elevate player profile to qualify for Tier 1 & Tier 3 Academic skills
        val boosted = PlayerProfile(
            codename = "Scholar",
            level = 8,
            rank = HunterRank.D_RANK,
            verifiedActivitiesCount = 5,
            streakDays = 7,
            attributes = PlayerAttributes(
                str = AttributeBreakdown(AttributeType.STR, verifiedPoints = 4),
                vit = AttributeBreakdown(AttributeType.VIT, verifiedPoints = 4),
                intel = AttributeBreakdown(AttributeType.INT, verifiedPoints = 8),
                dis = AttributeBreakdown(AttributeType.DIS, verifiedPoints = 6),
                wis = AttributeBreakdown(AttributeType.WIS, verifiedPoints = 5),
                cha = AttributeBreakdown(AttributeType.CHA, verifiedPoints = 2)
            )
        )
        database.playerDao().upsertPlayer(boosted.toEntity())

        assertTrue(rpgRepository.unlockOrUpgradeSkill("acad_deep_focus").isSuccess)
        assertTrue(rpgRepository.unlockOrUpgradeSkill("acad_retention").isSuccess)

        val snapWithSkills = rpgRepository.rpgStateFlow.first()
        assertTrue(60 in snapWithSkills.activeSkillEffects.unlockedFocusPresetsMinutes)
        assertTrue(90 in snapWithSkills.activeSkillEffects.unlockedFocusPresetsMinutes)

        // Evaluate auto unlocks (consistency 7d -> title_iron_will, rank D -> item_frame_monarch_gold)
        rpgRepository.evaluateAutoUnlocksNow()
        assertTrue(rpgRepository.equipTitle("title_iron_will").isSuccess)
        val afterEquip = rpgRepository.rpgStateFlow.first()
        assertEquals("The Iron Will", afterEquip.profile.title)

        // Inventory frame exclusivity
        assertTrue(rpgRepository.toggleEquipInventoryItem("item_frame_obsidian_default").isSuccess)
        assertTrue(rpgRepository.toggleEquipInventoryItem("item_frame_monarch_gold").isSuccess)
        val afterFrame = rpgRepository.rpgStateFlow.first().inventoryItems
        assertFalse(afterFrame.first { it.definition.id == "item_frame_obsidian_default" }.isEquipped)
        assertTrue(afterFrame.first { it.definition.id == "item_frame_monarch_gold" }.isEquipped)

        // AI Safety Guard blocks physical proposal approval when recovery is REST_RECOMMENDED
        database.recoveryAssessmentDao().insert(
            RecoveryAssessmentEntity(
                stateName = RecoveryState.REST_RECOMMENDED.name,
                readinessScore = 25,
                measuredLoadMinutes48h = 180,
                measuredLoadMinutes7d = 420,
                measuredSessions7d = 7,
                consecutiveActiveDays = 7,
                measuredSleepHoursLastNight = 5.0,
                measuredRestingHrBpm = 72,
                selfReportedFatigue = 5,
                selfReportedDiscomfort = true,
                discomfortNotes = "Sore calves",
                recommendedActionsJoined = "Take rest day",
                summaryMessage = "Rest recommended",
                timestampEpochMs = System.currentTimeMillis()
            )
        )
        database.dungeonDao().insertDungeonProposals(
            listOf(
                AiDungeonProposalEntity(
                    proposalUuid = "prop-phys-1",
                    title = "High Tempo Run",
                    description = "Push hard",
                    categoryName = DungeonCategory.PHYSICAL.name,
                    difficultyName = DungeonDifficulty.D_RANK.name,
                    objectiveTitle = "Run 45 min",
                    targetEvidenceTypeName = DungeonEvidenceType.PHYSICAL_JOG_MINUTES.name,
                    targetValue = 45,
                    timeLimitHours = 24,
                    requiresVerifiedOnly = false,
                    reason = "Test",
                    isBossCampaign = false,
                    statusName = ProposalStatus.PENDING.name,
                    createdAtEpochMs = System.currentTimeMillis()
                )
            )
        )
        val physicalProposalId = database.dungeonDao().getAllDungeonProposalsOnce().first { it.proposalUuid == "prop-phys-1" }.id
        assertTrue(rpgRepository.approveAiDungeonProposal(physicalProposalId).isFailure)

        // Safe recovery/academic AI proposals approve with 0 XP granted
        val xpBeforeProposal = rpgRepository.rpgStateFlow.first().profile.totalXp
        val gen = aiSystemRepository.generateDungeonProposals()
        assertTrue(gen.count >= 1)
        val pendingSafe = rpgRepository.rpgStateFlow.first().aiDungeonProposals.first {
            it.status == ProposalStatus.PENDING && it.category != DungeonCategory.PHYSICAL
        }
        assertTrue(rpgRepository.approveAiDungeonProposal(pendingSafe.id).isSuccess)
        assertEquals(xpBeforeProposal, rpgRepository.rpgStateFlow.first().profile.totalXp)
    }

    @Test
    fun databaseMigration_v5_to_v6_preservesAllExistingDataAndCreatesRpgTables() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration_v5_to_v6_test.db"
        context.deleteDatabase(dbName)

        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(5) {
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

                        db.execSQL(
                            """
                            INSERT INTO `player_profile` VALUES (
                                1, 'Beru', 'Shadow Vanguard', 15, 100, 4200, 'C_RANK',
                                24, 1, 10, 22, 1, 10, 21, 0, 5, 25, 1, 5, 20, 1, 10, 18, 0, 5, 14, 0, 0,
                                14, 18, 10, 98, 1700000000000
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            INSERT INTO `notification_preferences` VALUES (
                                1, 1, 1, 1, 1, 1, 1, 1, 1, 22, 7, 10, 1, 1, 1, 1, 1, 1, 1, 1700000000000
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            INSERT INTO `ai_consent_records` VALUES (
                                1, 1, 1, 1, 1, 1, 1, 1700000000000
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
        assertEquals("Beru", preservedPlayer?.codename)
        assertEquals(15, preservedPlayer?.level)

        val prefs = migratedDb.aiConfigDao().getNotificationPreferencesOnce()!!
        assertTrue(prefs.enableRpgAlerts)

        val consent = migratedDb.aiConfigDao().getConsentRecordOnce()!!
        assertTrue(consent.shareRpgWithAi)

        // New v6 tables are accessible and cleanly queried
        assertEquals(0, migratedDb.dungeonDao().getAllDefinitionsOnce().size)
        assertEquals(0, migratedDb.bossBattleDao().getAllBossDefinitionsOnce().size)
        assertEquals(0, migratedDb.skillDao().getAllDefinitionsOnce().size)
        assertEquals(0, migratedDb.achievementTitleDao().getAllAchievementDefinitionsOnce().size)
        assertEquals(0, migratedDb.inventoryShadowDao().getAllInventoryItemsOnce().size)
        assertEquals(0, migratedDb.rpgAuditDao().sumRpgXpSince(0L))

        migratedDb.close()
        context.deleteDatabase(dbName)
    }
}
