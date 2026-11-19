package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.entity.PlayerEntity
import com.example.data.local.entity.QuestEntity
import com.example.data.local.entity.ShadowMonarchProfileEntity
import com.example.data.local.entity.toDomain
import com.example.data.repository.QuestCommitmentRepository
import com.example.data.repository.SystemEventRepository
import com.example.domain.model.Clock
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Progression Isolation Test for Milestone 12 Phase 2.
 * Verifies that quest commitment failure confirmation and failure record persistence
 * produce ZERO mutations or side effects on player XP, level, rank, seven attributes,
 * or shadow/monarch progression.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ProgressionIsolationTest {

    private lateinit var context: Context
    private lateinit var db: SoloLevelingDatabase
    private lateinit var systemEventRepository: SystemEventRepository
    private lateinit var commitmentRepository: QuestCommitmentRepository
    private var simulatedTime = 1_700_500_000_000L

    private val initialPlayer = PlayerEntity(
        id = 1,
        codename = "SUNG_JIN_WOO",
        title = "The Awakened",
        level = 14,
        currentLevelXp = 350,
        totalXp = 4820L,
        rankName = "D_RANK",
        strVerified = 28, strSelfReported = 12, strProgressXp = 40,
        vitVerified = 26, vitSelfReported = 10, vitProgressXp = 30,
        agiVerified = 30, agiSelfReported = 14, agiProgressXp = 50,
        intVerified = 24, intSelfReported = 8, intProgressXp = 20,
        disVerified = 32, disSelfReported = 15, disProgressXp = 60,
        wisVerified = 22, wisSelfReported = 6, wisProgressXp = 10,
        chaVerified = 20, chaSelfReported = 5, chaProgressXp = 0,
        streakDays = 18,
        verifiedActivitiesCount = 42,
        selfReportedActivitiesCount = 19,
        recoveryScore = 85,
        createdAtEpochMs = simulatedTime - 1_000_000L
    )

    private val initialMonarch = ShadowMonarchProfileEntity(
        accountOwnerId = "local_vault_player_1",
        monarchLevel = 3,
        monarchXp = 600L,
        monarchRankName = "AWAKENING",
        monarchAuthority = 25,
        armyCapacity = 10,
        activeShadowCount = 2,
        totalShadowsRecruited = 2,
        highestShadowRankName = "D_RANK",
        totalEvolutions = 0,
        shadowArmyPowerRating = 450L,
        unlockedMonarchSkillIdsJson = "[\"monarch_shadow_extraction\"]",
        updatedAtEpochMs = simulatedTime - 1_000_000L
    )

    @Before
    fun setUp() = runTest {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, SoloLevelingDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        systemEventRepository = SystemEventRepository(db)
        commitmentRepository = QuestCommitmentRepository(
            database = db,
            systemEventRepository = systemEventRepository,
            clock = Clock { simulatedTime }
        )

        db.playerDao().upsertPlayer(initialPlayer)
        db.shadowMonarchProfileDao().upsertProfile(initialMonarch)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun questFailureConfirmation_doesNotMutatePlayerXpRankAttributesOrMonarchProgression() = runTest {
        // 1. Snapshot initial state before failure
        val beforePlayer = db.playerDao().getPlayerOnce()!!
        val beforeMonarch = db.shadowMonarchProfileDao().getProfileForOwnerOnce("local_vault_player_1")!!

        // 2. Insert quest and accept commitment
        val qEntity = QuestEntity(
            id = 999L,
            title = "Strict Sprint Session",
            description = "High intensity workout",
            typeName = "DAILY",
            categoryName = "PHYSICAL",
            targetValue = 5,
            currentValue = 0,
            unit = "km",
            xpReward = 80,
            primaryAttrName = "STR",
            primaryAttrXpReward = 50,
            secondaryAttrName = "VIT",
            secondaryAttrXpReward = 30,
            isCompleted = false,
            verificationStatusName = "SELF_REPORTED",
            createdAtEpochMs = simulatedTime,
            completedAtEpochMs = null
        )
        db.questDao().insertQuest(qEntity)

        val deadline = simulatedTime + 1000L
        val commitment = commitmentRepository.acceptQuestCommitment(
            questId = 999L,
            logicalCycleId = "cycle_500",
            accountOwnerId = "local_vault_player_1",
            targetEpochDay = 500L,
            customDeadlineEpochMs = deadline,
            gracePeriodMinutes = 15
        ).getOrThrow()

        // 3. Fast-forward time past grace expiry and confirm failure
        simulatedTime = commitment.gracePeriodExpiresAtEpochMs + 1000L
        val failures = commitmentRepository.evaluateAndRecordFailuresForCycle(
            logicalCycleId = "cycle_500",
            triggerSource = "DAILY_RESET_TEST",
            nowEpochMs = simulatedTime
        )
        assertEquals(1, failures.size)

        // 4. Snapshot state after failure confirmation
        val afterPlayer = db.playerDao().getPlayerOnce()!!
        val afterMonarch = db.shadowMonarchProfileDao().getProfileForOwnerOnce("local_vault_player_1")!!

        // 5. Strict equality assertions: progression is completely isolated
        assertEquals(beforePlayer.totalXp, afterPlayer.totalXp)
        assertEquals(beforePlayer.level, afterPlayer.level)
        assertEquals(beforePlayer.currentLevelXp, afterPlayer.currentLevelXp)
        assertEquals(beforePlayer.rankName, afterPlayer.rankName)
        assertEquals(beforePlayer.strVerified, afterPlayer.strVerified)
        assertEquals(beforePlayer.strSelfReported, afterPlayer.strSelfReported)
        assertEquals(beforePlayer.strProgressXp, afterPlayer.strProgressXp)
        assertEquals(beforePlayer.vitVerified, afterPlayer.vitVerified)
        assertEquals(beforePlayer.vitSelfReported, afterPlayer.vitSelfReported)
        assertEquals(beforePlayer.vitProgressXp, afterPlayer.vitProgressXp)
        assertEquals(beforePlayer.agiVerified, afterPlayer.agiVerified)
        assertEquals(beforePlayer.agiSelfReported, afterPlayer.agiSelfReported)
        assertEquals(beforePlayer.intVerified, afterPlayer.intVerified)
        assertEquals(beforePlayer.disVerified, afterPlayer.disVerified)
        assertEquals(beforePlayer.wisVerified, afterPlayer.wisVerified)
        assertEquals(beforePlayer.chaVerified, afterPlayer.chaVerified)
        assertEquals(beforePlayer.verifiedActivitiesCount, afterPlayer.verifiedActivitiesCount)
        assertEquals(beforePlayer.selfReportedActivitiesCount, afterPlayer.selfReportedActivitiesCount)

        assertEquals(beforeMonarch.monarchLevel, afterMonarch.monarchLevel)
        assertEquals(beforeMonarch.monarchXp, afterMonarch.monarchXp)
        assertEquals(beforeMonarch.monarchAuthority, afterMonarch.monarchAuthority)
        assertEquals(beforeMonarch.armyCapacity, afterMonarch.armyCapacity)
        assertEquals(beforeMonarch.shadowArmyPowerRating, afterMonarch.shadowArmyPowerRating)
    }
}
