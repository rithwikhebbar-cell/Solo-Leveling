package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.entity.QuestCommitmentEntity
import com.example.data.local.entity.QuestEntity
import com.example.data.repository.PenaltyRepository
import com.example.data.repository.QuestCommitmentRepository
import com.example.data.repository.RedemptionRepository
import com.example.data.repository.SystemEventRepository
import com.example.domain.model.Clock
import com.example.domain.model.CommitmentState
import com.example.domain.model.ExemptionReasonCategory
import com.example.ui.viewmodel.PenaltyAuditViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PenaltyAuditViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var db: SoloLevelingDatabase
    private lateinit var systemEventRepository: SystemEventRepository
    private lateinit var commitmentRepository: QuestCommitmentRepository
    private lateinit var penaltyRepository: PenaltyRepository
    private lateinit var redemptionRepository: RedemptionRepository
    private lateinit var viewModel: PenaltyAuditViewModel

    private val now = 1_700_000_000_000L
    private val ownerId = "player_audit_vm_test"

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, SoloLevelingDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        systemEventRepository = SystemEventRepository(db)
        commitmentRepository = QuestCommitmentRepository(
            database = db,
            systemEventRepository = systemEventRepository,
            clock = Clock { now }
        )
        penaltyRepository = PenaltyRepository(
            database = db,
            systemEventRepository = systemEventRepository
        )
        redemptionRepository = RedemptionRepository(
            database = db,
            systemEventRepository = systemEventRepository
        )

        viewModel = PenaltyAuditViewModel(
            penaltyRepository = penaltyRepository,
            questCommitmentRepository = commitmentRepository,
            redemptionRepository = redemptionRepository,
            database = db,
            backendAiGateway = null,
            accountOwnerId = ownerId
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun initialState_loadsCleanly() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(ownerId, state.accountOwnerId)
        assertEquals(0, state.overview.currentBalance)
        assertEquals(0, state.selectedTab)
    }

    @Test
    fun setSelectedTab_updatesState() = runTest {
        viewModel.setSelectedTab(2)
        assertEquals(2, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun submitExemption_emptyExplanation_setsErrorMessage() = runTest {
        viewModel.submitExemption(
            commitmentId = "c1",
            category = ExemptionReasonCategory.ILLNESS,
            explanation = "   "
        )
        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun submitExemption_valid_submitsSuccessfully() = runTest {
        // Seed quest and commitment
        db.questDao().insertQuests(
            listOf(
                QuestEntity(
                    id = 1L, title = "Quest 1", description = "D", typeName = "DAILY", categoryName = "PHYSICAL",
                    targetValue = 10, currentValue = 0, unit = "min", xpReward = 10, primaryAttribute = "VIT",
                    primaryAttrXpReward = 10, isCompleted = false, verificationStatusName = "SELF_REPORTED", createdAtEpochMs = now
                )
            )
        )
        db.questCommitmentDao().upsertCommitment(
            QuestCommitmentEntity(
                commitmentId = "c1", questId = 1L, accountOwnerId = ownerId, logicalCycleId = "cy1",
                stateName = CommitmentState.ACTIVE.name, acceptedAtEpochMs = now, deadlineEpochMs = now + 1000,
                gracePeriodMinutes = 30, gracePeriodExpiresAtEpochMs = now + 2000, rescheduleCount = 0,
                originalDeadlineEpochMs = now + 1000, completedAtEpochMs = null
            )
        )

        viewModel.submitExemption(
            commitmentId = "c1",
            category = ExemptionReasonCategory.EMERGENCY,
            explanation = "Valid emergency documented"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.statusMessage)
        assertTrue(viewModel.uiState.value.statusMessage!!.contains("submitted"))
    }

    @Test
    fun reconcileLedger_executesAndUpdatesStatus() = runTest {
        viewModel.reconcileLedger()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.statusMessage)
        assertTrue(viewModel.uiState.value.statusMessage!!.contains("reconciled"))
    }

    @Test
    fun requestAiAdvisoryExplanation_offlineFallback_providesConstructiveGuidance() = runTest {
        viewModel.requestAiAdvisoryExplanation()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.aiAdvisoryExplanation)
        assertTrue(viewModel.uiState.value.aiAdvisoryExplanation!!.contains("Deterministic pattern insights"))
    }
}
