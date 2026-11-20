package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.entity.ConfirmedFailureRecordEntity
import com.example.data.local.entity.PenaltyEntity
import com.example.data.local.entity.PenaltyPointAccountEntity
import com.example.data.local.entity.PenaltyTransactionEntity
import com.example.data.local.entity.QuestCommitmentEntity
import com.example.data.local.entity.QuestEntity
import com.example.data.repository.PenaltyRepository
import com.example.data.repository.QuestCommitmentRepository
import com.example.data.repository.SystemEventRepository
import com.example.domain.model.Clock
import com.example.domain.model.CommitmentState
import com.example.domain.model.ExemptionLifecycleState
import com.example.domain.model.ExemptionReasonCategory
import com.example.domain.model.PenaltySeverity
import com.example.domain.model.PenaltyStatus
import com.example.domain.model.PenaltyTransactionType
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
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class EmergencyExemptionGatewayTest {

    private lateinit var context: Context
    private lateinit var db: SoloLevelingDatabase
    private lateinit var systemEventRepository: SystemEventRepository
    private lateinit var commitmentRepository: QuestCommitmentRepository
    private lateinit var penaltyRepository: PenaltyRepository

    private val now = 1_700_000_000_000L
    private val ownerId = "player_exemption_test"
    private val fakeClock = Clock { now }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, SoloLevelingDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        systemEventRepository = SystemEventRepository(db)
        commitmentRepository = QuestCommitmentRepository(
            database = db,
            systemEventRepository = systemEventRepository,
            clock = fakeClock
        )
        penaltyRepository = PenaltyRepository(
            database = db,
            systemEventRepository = systemEventRepository
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun seedCommitment(
        questId: Long = 100L,
        state: CommitmentState = CommitmentState.ACTIVE
    ): String {
        val qEntity = QuestEntity(
            id = questId,
            title = "Physical Trial",
            description = "Desc",
            typeName = "DAILY",
            categoryName = "PHYSICAL",
            targetValue = 30,
            currentValue = 0,
            unit = "min",
            xpReward = 45,
            primaryAttribute = "VIT",
            primaryAttrXpReward = 40,
            isCompleted = false,
            verificationStatusName = "SELF_REPORTED",
            createdAtEpochMs = now - 3600_000L
        )
        db.questDao().insertQuests(listOf(qEntity))

        val commitmentId = "commit_${questId}_cy1"
        val cEntity = QuestCommitmentEntity(
            commitmentId = commitmentId,
            questId = questId,
            accountOwnerId = ownerId,
            logicalCycleId = "cy1",
            stateName = state.name,
            acceptedAtEpochMs = now - 3600_000L,
            deadlineEpochMs = now + 1800_000L,
            gracePeriodMinutes = 30,
            gracePeriodExpiresAtEpochMs = now + 3600_000L,
            rescheduleCount = 0,
            originalDeadlineEpochMs = now + 1800_000L,
            completedAtEpochMs = null
        )
        db.questCommitmentDao().upsertCommitment(cEntity)
        return commitmentId
    }

    @Test
    fun createExemptionRequest_transitionsToPendingReview() = runTest {
        val commitmentId = seedCommitment()

        val result = commitmentRepository.requestExemption(
            commitmentId = commitmentId,
            category = ExemptionReasonCategory.INJURY,
            explanation = "Sprained ankle during training session.",
            accountOwnerId = ownerId
        )

        assertTrue(result.isSuccess)
        val exemption = result.getOrThrow()
        assertEquals(ExemptionLifecycleState.PENDING_REVIEW, exemption.lifecycleState)
        assertEquals(ExemptionReasonCategory.INJURY, exemption.category)
        assertEquals("Sprained ankle during training session.", exemption.explanation)
    }

    @Test
    fun withdrawExemption_transitionsToWithdrawn() = runTest {
        val commitmentId = seedCommitment()
        val created = commitmentRepository.requestExemption(
            commitmentId = commitmentId,
            category = ExemptionReasonCategory.ILLNESS,
            explanation = "Acute fever",
            accountOwnerId = ownerId
        ).getOrThrow()

        val withdrawResult = commitmentRepository.withdrawExemption(created.exemptionId)
        assertTrue(withdrawResult.isSuccess)
        val withdrawn = withdrawResult.getOrThrow()
        assertEquals(ExemptionLifecycleState.WITHDRAWN, withdrawn.lifecycleState)
    }

    @Test
    fun withdrawFinalizedExemption_fails() = runTest {
        val commitmentId = seedCommitment()
        val created = commitmentRepository.requestExemption(
            commitmentId = commitmentId,
            category = ExemptionReasonCategory.SYSTEM_OUTAGE,
            explanation = "App freeze",
            accountOwnerId = ownerId
        ).getOrThrow()

        // Approve it first
        commitmentRepository.reviewExemption(created.exemptionId, isApproved = true)

        // Attempting to withdraw an approved exemption must fail
        val withdrawResult = commitmentRepository.withdrawExemption(created.exemptionId)
        assertFalse(withdrawResult.isSuccess)
    }

    @Test
    fun reviewExemption_approvalWithoutPriorPenalty_updatesCommitmentToExempted() = runTest {
        val commitmentId = seedCommitment()
        val created = commitmentRepository.requestExemption(
            commitmentId = commitmentId,
            category = ExemptionReasonCategory.RECOVERY_REQUIREMENT,
            explanation = "Severe strain",
            accountOwnerId = ownerId
        ).getOrThrow()

        val reviewResult = commitmentRepository.reviewExemption(created.exemptionId, isApproved = true)
        assertTrue(reviewResult.isSuccess)
        val updated = reviewResult.getOrThrow()
        assertEquals(ExemptionLifecycleState.APPROVED, updated.lifecycleState)

        val commitment = db.questCommitmentDao().getCommitmentById(commitmentId)
        assertNotNull(commitment)
        assertEquals(CommitmentState.EXEMPTED.name, commitment!!.stateName)
    }

    @Test
    fun reviewExemption_retroactiveApproval_reversesAllocatedPenaltyPoints() = runTest {
        val commitmentId = seedCommitment(state = CommitmentState.FAILED)
        val penaltyId = "pen_$commitmentId"

        // Seed initial 10 PP penalty and 10 PP account balance
        db.penaltyDao().insertPenalty(
            PenaltyEntity(
                penaltyId = penaltyId,
                failureRecordId = "fail_$commitmentId",
                commitmentId = commitmentId,
                questId = 100L,
                accountOwnerId = ownerId,
                questTitleSnapshot = "Physical Trial",
                severityCode = PenaltySeverity.P2_MODERATE.code,
                basePenaltyPoints = 10,
                escalationBonusPoints = 0,
                totalPenaltyPoints = 10,
                penaltyStatusName = PenaltyStatus.ACTIVE.name,
                reason = "Overdue",
                policyVersion = "1.0",
                createdAtEpochMs = now - 1000L,
                resolvedAtEpochMs = null
            )
        )
        db.penaltyTransactionDao().insertTransaction(
            PenaltyTransactionEntity(
                transactionId = "tx_alloc_1",
                accountOwnerId = ownerId,
                penaltyId = penaltyId,
                transactionTypeName = PenaltyTransactionType.ALLOCATION.name,
                pointsDelta = 10,
                balanceAfter = 10,
                reason = "Failure allocation",
                idempotencyKey = "k_alloc_1",
                timestampEpochMs = now - 1000L
            )
        )
        db.penaltyAccountDao().upsertAccount(
            PenaltyPointAccountEntity(
                accountOwnerId = ownerId,
                currentBalance = 10,
                lifetimePointsAccrued = 10,
                activePenaltiesCount = 1,
                lastUpdatedEpochMs = now - 1000L
            )
        )

        // Submit retroactive exemption
        val created = commitmentRepository.requestExemption(
            commitmentId = commitmentId,
            category = ExemptionReasonCategory.EMERGENCY,
            explanation = "Hospitalization after sudden accident",
            accountOwnerId = ownerId
        ).getOrThrow()

        // Review and approve
        val reviewResult = commitmentRepository.reviewExemption(created.exemptionId, isApproved = true)
        assertTrue(reviewResult.isSuccess)

        // Check account balance is reduced to 0
        val account = db.penaltyAccountDao().getAccountOnce(ownerId)
        assertNotNull(account)
        assertEquals(0, account!!.currentBalance)
        assertEquals(0, account.activePenaltiesCount)

        // Check penalty status is EXEMPTED
        val penalty = db.penaltyDao().getPenaltyById(penaltyId)
        assertNotNull(penalty)
        assertEquals(PenaltyStatus.EXEMPTED.name, penalty!!.penaltyStatusName)

        // Check ledger contains REVERSAL transaction
        val transactions = db.penaltyTransactionDao().getTransactionsForPenalty(penaltyId)
        val reversalTx = transactions.find { it.transactionTypeName == PenaltyTransactionType.REVERSAL.name }
        assertNotNull(reversalTx)
        assertEquals(10, reversalTx!!.pointsDelta)
        assertEquals(0, reversalTx.balanceAfter)
    }

    @Test
    fun reviewExemption_retroactiveApprovalOnPartiallyRedeemedPenalty_reversesOnlyRemainingPoints() = runTest {
        val commitmentId = seedCommitment(state = CommitmentState.FAILED)
        val penaltyId = "pen_partially_redeemed"

        // Total was 20 PP, but 10 PP was already resolved via redemption
        db.penaltyDao().insertPenalty(
            PenaltyEntity(
                penaltyId = penaltyId,
                failureRecordId = "fail_$commitmentId",
                commitmentId = commitmentId,
                questId = 100L,
                accountOwnerId = ownerId,
                questTitleSnapshot = "Physical Trial",
                severityCode = PenaltySeverity.P3_MAJOR.code,
                basePenaltyPoints = 20,
                escalationBonusPoints = 0,
                totalPenaltyPoints = 20,
                penaltyStatusName = PenaltyStatus.ACTIVE.name,
                reason = "Overdue",
                policyVersion = "1.0",
                createdAtEpochMs = now - 2000L,
                resolvedAtEpochMs = null
            )
        )
        db.penaltyTransactionDao().insertTransaction(
            PenaltyTransactionEntity(
                transactionId = "tx_alloc_part",
                accountOwnerId = ownerId,
                penaltyId = penaltyId,
                transactionTypeName = PenaltyTransactionType.ALLOCATION.name,
                pointsDelta = 20,
                balanceAfter = 20,
                reason = "Allocation",
                idempotencyKey = "k_alloc_part",
                timestampEpochMs = now - 2000L
            )
        )
        // 10 PP redeemed
        db.penaltyTransactionDao().insertTransaction(
            PenaltyTransactionEntity(
                transactionId = "tx_red_part",
                accountOwnerId = ownerId,
                penaltyId = penaltyId,
                transactionTypeName = PenaltyTransactionType.REDEMPTION_RESOLUTION.name,
                pointsDelta = 10,
                balanceAfter = 10,
                reason = "Redemption resolution",
                idempotencyKey = "k_red_part",
                timestampEpochMs = now - 1000L
            )
        )
        db.penaltyAccountDao().upsertAccount(
            PenaltyPointAccountEntity(
                accountOwnerId = ownerId,
                currentBalance = 10, // 10 remaining
                lifetimePointsAccrued = 20,
                activePenaltiesCount = 1,
                lastUpdatedEpochMs = now - 1000L
            )
        )

        // Submit and approve retroactive exemption
        val created = commitmentRepository.requestExemption(
            commitmentId = commitmentId,
            category = ExemptionReasonCategory.INJURY,
            explanation = "Injury verified by clinic",
            accountOwnerId = ownerId
        ).getOrThrow()

        val reviewResult = commitmentRepository.reviewExemption(created.exemptionId, isApproved = true)
        assertTrue(reviewResult.isSuccess)

        // Outstanding was 20 - 10 = 10 PP. Reversal should deduct only 10 PP, not 20 PP!
        val account = db.penaltyAccountDao().getAccountOnce(ownerId)
        assertEquals(0, account!!.currentBalance)

        val transactions = db.penaltyTransactionDao().getTransactionsForPenalty(penaltyId)
        val reversalTx = transactions.find { it.transactionTypeName == PenaltyTransactionType.REVERSAL.name }
        assertNotNull(reversalTx)
        assertEquals(10, reversalTx!!.pointsDelta) // Exactly remaining points
    }

    @Test
    fun reviewExemption_retroactiveApprovalOnFullyResolvedPenalty_doesNotInsertUnnecessaryReversal() = runTest {
        val commitmentId = seedCommitment(state = CommitmentState.FAILED)
        val penaltyId = "pen_fully_resolved"

        // Total was 10 PP, all 10 resolved
        db.penaltyDao().insertPenalty(
            PenaltyEntity(
                penaltyId = penaltyId,
                failureRecordId = "fail_$commitmentId",
                commitmentId = commitmentId,
                questId = 100L,
                accountOwnerId = ownerId,
                questTitleSnapshot = "Physical Trial",
                severityCode = PenaltySeverity.P2_MODERATE.code,
                basePenaltyPoints = 10,
                escalationBonusPoints = 0,
                totalPenaltyPoints = 10,
                penaltyStatusName = PenaltyStatus.RESOLVED.name,
                reason = "Overdue",
                policyVersion = "1.0",
                createdAtEpochMs = now - 2000L,
                resolvedAtEpochMs = now - 1000L
            )
        )
        db.penaltyTransactionDao().insertTransaction(
            PenaltyTransactionEntity(
                transactionId = "tx_alloc_full",
                accountOwnerId = ownerId,
                penaltyId = penaltyId,
                transactionTypeName = PenaltyTransactionType.ALLOCATION.name,
                pointsDelta = 10,
                balanceAfter = 10,
                reason = "Allocation",
                idempotencyKey = "k_alloc_full",
                timestampEpochMs = now - 2000L
            )
        )
        db.penaltyTransactionDao().insertTransaction(
            PenaltyTransactionEntity(
                transactionId = "tx_red_full",
                accountOwnerId = ownerId,
                penaltyId = penaltyId,
                transactionTypeName = PenaltyTransactionType.REDEMPTION_RESOLUTION.name,
                pointsDelta = 10,
                balanceAfter = 0,
                reason = "Redemption resolution",
                idempotencyKey = "k_red_full",
                timestampEpochMs = now - 1000L
            )
        )
        db.penaltyAccountDao().upsertAccount(
            PenaltyPointAccountEntity(
                accountOwnerId = ownerId,
                currentBalance = 0,
                lifetimePointsAccrued = 10,
                activePenaltiesCount = 0,
                lastUpdatedEpochMs = now - 1000L
            )
        )

        val created = commitmentRepository.requestExemption(
            commitmentId = commitmentId,
            category = ExemptionReasonCategory.FORCE_MAJEURE,
            explanation = "Late documented exemption",
            accountOwnerId = ownerId
        ).getOrThrow()

        val reviewResult = commitmentRepository.reviewExemption(created.exemptionId, isApproved = true)
        assertTrue(reviewResult.isSuccess)

        // Verify balance remains 0 and no new reversal transaction was created
        val account = db.penaltyAccountDao().getAccountOnce(ownerId)
        assertEquals(0, account!!.currentBalance)

        val transactions = db.penaltyTransactionDao().getTransactionsForPenalty(penaltyId)
        val reversalTx = transactions.find { it.transactionTypeName == PenaltyTransactionType.REVERSAL.name }
        assertEquals(null, reversalTx)
    }
}
