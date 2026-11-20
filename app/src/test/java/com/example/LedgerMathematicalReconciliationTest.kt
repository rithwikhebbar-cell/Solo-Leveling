package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.entity.PenaltyEntity
import com.example.data.local.entity.PenaltyPointAccountEntity
import com.example.data.local.entity.PenaltyTransactionEntity
import com.example.data.repository.PenaltyRepository
import com.example.data.repository.SystemEventRepository
import com.example.domain.model.PenaltySeverity
import com.example.domain.model.PenaltyStatus
import com.example.domain.model.PenaltyTransactionType
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LedgerMathematicalReconciliationTest {

    private lateinit var context: Context
    private lateinit var db: SoloLevelingDatabase
    private lateinit var systemEventRepository: SystemEventRepository
    private lateinit var penaltyRepository: PenaltyRepository

    private val ownerId = "player_ledger_recon_test"
    private val now = 1_700_000_000_000L

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, SoloLevelingDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        systemEventRepository = SystemEventRepository(db)
        penaltyRepository = PenaltyRepository(
            database = db,
            systemEventRepository = systemEventRepository
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun ledgerReconciliation_mathematicalInvariantsHoldAcrossTransactions() = runTest {
        // 1. Create two penalties: P1 = 10 PP, P2 = 20 PP
        db.penaltyDao().insertPenalty(
            PenaltyEntity(
                penaltyId = "p1", failureRecordId = "f1", commitmentId = "c1", questId = 1L, accountOwnerId = ownerId,
                questTitleSnapshot = "Quest 1", severityCode = PenaltySeverity.P2_MODERATE.code, basePenaltyPoints = 10,
                escalationBonusPoints = 0, totalPenaltyPoints = 10, penaltyStatusName = PenaltyStatus.ACTIVE.name,
                reason = "Overdue", policyVersion = "1.0", createdAtEpochMs = now, resolvedAtEpochMs = null
            )
        )
        db.penaltyDao().insertPenalty(
            PenaltyEntity(
                penaltyId = "p2", failureRecordId = "f2", commitmentId = "c2", questId = 2L, accountOwnerId = ownerId,
                questTitleSnapshot = "Quest 2", severityCode = PenaltySeverity.P3_MAJOR.code, basePenaltyPoints = 20,
                escalationBonusPoints = 0, totalPenaltyPoints = 20, penaltyStatusName = PenaltyStatus.ACTIVE.name,
                reason = "Overdue", policyVersion = "1.0", createdAtEpochMs = now, resolvedAtEpochMs = null
            )
        )

        // 2. Insert corresponding allocation transactions
        db.penaltyTransactionDao().insertTransaction(
            PenaltyTransactionEntity(
                transactionId = "tx1", accountOwnerId = ownerId, penaltyId = "p1",
                transactionTypeName = PenaltyTransactionType.ALLOCATION.name, pointsDelta = 10, balanceAfter = 10,
                reason = "Allocation P1", idempotencyKey = "k1", timestampEpochMs = now
            )
        )
        db.penaltyTransactionDao().insertTransaction(
            PenaltyTransactionEntity(
                transactionId = "tx2", accountOwnerId = ownerId, penaltyId = "p2",
                transactionTypeName = PenaltyTransactionType.ALLOCATION.name, pointsDelta = 20, balanceAfter = 30,
                reason = "Allocation P2", idempotencyKey = "k2", timestampEpochMs = now + 100
            )
        )

        // 3. Insert redemption resolution for P1 (-10 PP)
        db.penaltyTransactionDao().insertTransaction(
            PenaltyTransactionEntity(
                transactionId = "tx3", accountOwnerId = ownerId, penaltyId = "p1",
                transactionTypeName = PenaltyTransactionType.REDEMPTION_RESOLUTION.name, pointsDelta = 10, balanceAfter = 20,
                reason = "Redemption P1", idempotencyKey = "k3", timestampEpochMs = now + 200
            )
        )
        db.penaltyDao().updatePenaltyStatus("p1", PenaltyStatus.RESOLVED.name, now + 200)

        // 4. Insert partial exemption reversal for P2 (-5 PP)
        db.penaltyTransactionDao().insertTransaction(
            PenaltyTransactionEntity(
                transactionId = "tx4", accountOwnerId = ownerId, penaltyId = "p2",
                transactionTypeName = PenaltyTransactionType.REVERSAL.name, pointsDelta = 5, balanceAfter = 15,
                reason = "Partial reversal P2", idempotencyKey = "k4", timestampEpochMs = now + 300
            )
        )

        // Current transactions delta: +10 +20 -10 -5 = 15 PP
        val transactions = db.penaltyTransactionDao().getAllTransactionsOnce(ownerId)
        val computedFromLedger = transactions.sumOf { tx ->
            val type = PenaltyTransactionType.fromName(tx.transactionTypeName)
            if (type.isDebit) -tx.pointsDelta else tx.pointsDelta
        }
        assertEquals(15, computedFromLedger)

        // 5. Run repository reconcileAccountBalance
        val reconciledBalance = penaltyRepository.reconcileAccountBalance(ownerId)
        assertEquals(15, reconciledBalance)

        val account = db.penaltyAccountDao().getAccountOnce(ownerId)
        assertEquals(15, account!!.currentBalance)
        assertEquals(30, account.lifetimePointsAccrued) // 10 + 20 allocated
        assertEquals(1, account.activePenaltiesCount) // p2 is still active
    }

    @Test
    fun ledgerReconciliation_recoversFromDriftedAccountState() = runTest {
        // Intentionally seed corrupted account balance (999 PP)
        db.penaltyAccountDao().upsertAccount(
            PenaltyPointAccountEntity(
                accountOwnerId = ownerId,
                currentBalance = 999,
                lifetimePointsAccrued = 999,
                activePenaltiesCount = 5,
                lastUpdatedEpochMs = now
            )
        )

        // Insert actual true transactions (+5 PP, -5 PP = 0 PP)
        db.penaltyTransactionDao().insertTransaction(
            PenaltyTransactionEntity(
                transactionId = "tx_t1", accountOwnerId = ownerId, penaltyId = "p_temp",
                transactionTypeName = PenaltyTransactionType.ALLOCATION.name, pointsDelta = 5, balanceAfter = 5,
                reason = "Allocation", idempotencyKey = "kt1", timestampEpochMs = now
            )
        )
        db.penaltyTransactionDao().insertTransaction(
            PenaltyTransactionEntity(
                transactionId = "tx_t2", accountOwnerId = ownerId, penaltyId = "p_temp",
                transactionTypeName = PenaltyTransactionType.REDEMPTION_RESOLUTION.name, pointsDelta = 5, balanceAfter = 0,
                reason = "Redemption", idempotencyKey = "kt2", timestampEpochMs = now + 100
            )
        )

        // Reconcile must overwrite the corrupted balance with mathematically verified 0 PP
        val reconciled = penaltyRepository.reconcileAccountBalance(ownerId)
        assertEquals(0, reconciled)

        val account = db.penaltyAccountDao().getAccountOnce(ownerId)
        assertEquals(0, account!!.currentBalance)
        assertEquals(5, account.lifetimePointsAccrued)
        assertEquals(0, account.activePenaltiesCount)
    }
}
