package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.entity.ConfirmedFailureRecordEntity
import com.example.data.local.entity.QuestCommitmentEntity
import com.example.data.local.entity.QuestEntity
import com.example.data.repository.PenaltyRepository
import com.example.data.repository.SystemEventRepository
import com.example.domain.model.DefaultPenaltyPolicy
import com.example.domain.model.PenaltySeverity
import com.example.domain.model.PenaltyStatus
import kotlinx.coroutines.flow.first
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PenaltyLedgerIntegrationTest {

    private lateinit var context: Context
    private lateinit var db: SoloLevelingDatabase
    private lateinit var systemEventRepository: SystemEventRepository
    private lateinit var repository: PenaltyRepository
    private val now = 1_700_000_000_000L
    private val ownerId = "local_vault_player_1"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, SoloLevelingDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        systemEventRepository = SystemEventRepository(db)
        repository = PenaltyRepository(
            database = db,
            systemEventRepository = systemEventRepository,
            policy = DefaultPenaltyPolicy
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun seedTestData(questId: Long, cycleId: String): String {
        // 1. Insert quest
        db.questDao().insertQuests(
            listOf(
                QuestEntity(
                    id = questId,
                    title = "Daily Strength Protocol",
                    description = "Execute workout",
                    typeName = "DAILY",
                    categoryName = "PHYSICAL",
                    targetValue = 30,
                    currentValue = 0,
                    unit = "min",
                    xpReward = 45,
                    primaryAttrName = "STR",
                    primaryAttrXpReward = 40,
                    secondaryAttrName = "VIT",
                    secondaryAttrXpReward = 20,
                    isCompleted = false,
                    verificationStatusName = "SELF_REPORTED",
                    createdAtEpochMs = now - 86_400_000L,
                    completedAtEpochMs = null
                )
            )
        )

        // 2. Insert commitment
        val commitmentId = "commit_${questId}_${cycleId}"
        db.questCommitmentDao().upsertCommitment(
            QuestCommitmentEntity(
                commitmentId = commitmentId,
                questId = questId,
                accountOwnerId = ownerId,
                logicalCycleId = cycleId,
                stateName = "ACCEPTED",
                acceptedAtEpochMs = now - 3600_000L,
                deadlineEpochMs = now - 1800_000L,
                gracePeriodMinutes = 30,
                gracePeriodExpiresAtEpochMs = now,
                rescheduleCount = 0,
                originalDeadlineEpochMs = now - 1800_000L,
                createdAtEpochMs = now - 3600_000L,
                updatedAtEpochMs = now - 3600_000L
            )
        )

        // 3. Insert confirmed failure record
        val failureId = "fail_${questId}_${cycleId}"
        db.confirmedFailureDao().insertFailure(
            ConfirmedFailureRecordEntity(
                failureId = failureId,
                commitmentId = commitmentId,
                questId = questId,
                logicalCycleId = cycleId,
                accountOwnerId = ownerId,
                deadlineEpochMs = now - 1800_000L,
                gracePeriodExpiresAtEpochMs = now,
                confirmedAtEpochMs = now,
                failureReasonCode = "GRACE_PERIOD_EXPIRED",
                policyVersion = 1,
                exemptionEvaluated = false
            )
        )

        return failureId
    }

    @Test
    fun initialBalance_isZero() = runTest {
        val balance = repository.observeAccountBalance(ownerId).first()
        assertEquals(0, balance.currentBalance)
        assertEquals(0, balance.activePenaltiesCount)
    }

    @Test
    fun allocatePenalty_createsPenaltyRecord_ledgerTransaction_andUpdatesBalance() = runTest {
        val failureId = seedTestData(101L, "cycle_100")

        val result = repository.allocatePenaltyForConfirmedFailure(failureId, nowEpochMs = now)

        assertTrue("Allocation must succeed", result.success)
        assertFalse("Initial allocation must not be duplicate", result.isDuplicate)
        assertNotNull(result.penalty)
        assertNotNull(result.transaction)

        val penalty = result.penalty!!
        assertEquals(10, penalty.totalPenaltyPoints) // P2 base points
        assertEquals(PenaltySeverity.P2, penalty.severity)
        assertEquals(PenaltyStatus.ACTIVE, penalty.penaltyStatus)
        assertEquals(DefaultPenaltyPolicy.policyVersion, penalty.policyVersion)

        // Check account balance updated to 10
        assertEquals(10, result.newBalance)
        val balance = repository.observeAccountBalance(ownerId).first()
        assertEquals(10, balance.currentBalance)
        assertEquals(10, balance.lifetimePointsAccrued)
        assertEquals(1, balance.activePenaltiesCount)

        // Check ledger transaction
        val transactions = repository.observeRecentTransactions(ownerId).first()
        assertEquals(1, transactions.size)
        assertEquals("alloc_$failureId", transactions[0].idempotencyKey)
        assertEquals(10, transactions[0].pointsDelta)
        assertEquals(10, transactions[0].balanceAfter)
    }

    @Test
    fun duplicateAllocation_isIdempotentAndSafe() = runTest {
        val failureId = seedTestData(102L, "cycle_101")

        // First allocation
        val firstResult = repository.allocatePenaltyForConfirmedFailure(failureId, nowEpochMs = now)
        assertTrue(firstResult.success)
        assertFalse(firstResult.isDuplicate)
        assertEquals(10, firstResult.newBalance)

        // Second allocation with identical failureId (e.g. WorkManager retry or re-evaluation)
        val secondResult = repository.allocatePenaltyForConfirmedFailure(failureId, nowEpochMs = now + 5000L)
        assertTrue("Duplicate call must return success = true", secondResult.success)
        assertTrue("Duplicate call must report isDuplicate = true", secondResult.isDuplicate)
        assertEquals("Balance must not change on duplicate allocation", 10, secondResult.newBalance)

        // Verify only 1 penalty and 1 transaction exist in database
        val allPenalties = repository.observeAllPenalties(ownerId).first()
        assertEquals(1, allPenalties.size)

        val allTransactions = repository.observeRecentTransactions(ownerId).first()
        assertEquals(1, allTransactions.size)
    }

    @Test
    fun reconcileAccountBalance_computesAccurateLedgerSum() = runTest {
        val failureId1 = seedTestData(103L, "cycle_102")
        val failureId2 = seedTestData(104L, "cycle_103")

        repository.allocatePenaltyForConfirmedFailure(failureId1, nowEpochMs = now)
        repository.allocatePenaltyForConfirmedFailure(failureId2, nowEpochMs = now + 1000L)

        // Total balance = 10 + 10 = 20 PP
        val reconciled = repository.reconcileAccountBalance(ownerId)
        assertEquals(20, reconciled)

        val balance = repository.observeAccountBalance(ownerId).first()
        assertEquals(20, balance.currentBalance)
        assertEquals(20, balance.lifetimePointsAccrued)
        assertEquals(2, balance.activePenaltiesCount)
    }
}
