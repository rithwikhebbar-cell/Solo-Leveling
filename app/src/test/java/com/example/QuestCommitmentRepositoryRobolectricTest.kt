package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.entity.QuestEntity
import com.example.data.repository.QuestCommitmentRepository
import com.example.data.repository.SystemEventRepository
import com.example.domain.model.Clock
import com.example.domain.model.CommitmentState
import com.example.domain.model.ExemptionLifecycleState
import com.example.domain.model.ExemptionReasonCategory
import com.example.domain.model.QuestCommitment
import kotlinx.coroutines.test.runTest
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class QuestCommitmentRepositoryRobolectricTest {

    private lateinit var context: Context
    private lateinit var db: SoloLevelingDatabase
    private lateinit var systemEventRepository: SystemEventRepository
    private lateinit var repository: QuestCommitmentRepository
    private var simulatedTime: Long = 1_700_000_000_000L

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, SoloLevelingDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        systemEventRepository = SystemEventRepository(db)
        repository = QuestCommitmentRepository(
            database = db,
            systemEventRepository = systemEventRepository,
            clock = Clock { simulatedTime }
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun insertTestQuest(id: Long, title: String, isCompleted: Boolean = false): Long {
        val entity = QuestEntity(
            id = id,
            title = title,
            description = "Test objective",
            typeName = "DAILY",
            categoryName = "PHYSICAL",
            targetValue = 10,
            currentValue = if (isCompleted) 10 else 0,
            unit = "reps",
            xpReward = 50,
            primaryAttrName = "STR",
            primaryAttrXpReward = 30,
            secondaryAttrName = "VIT",
            secondaryAttrXpReward = 20,
            isCompleted = isCompleted,
            verificationStatusName = "SELF_REPORTED",
            createdAtEpochMs = simulatedTime - 100_000L,
            completedAtEpochMs = if (isCompleted) simulatedTime - 50_000L else null
        )
        return db.questDao().insertQuest(entity)
    }

    @Test
    fun acceptCommitment_persistsAcceptedStateWithDeadlineAndGrace() = runTest {
        val qId = insertTestQuest(1L, "Morning Calisthenics")
        val cycleId = "cycle_100"
        val customDeadline = simulatedTime + 3600_000L

        val result = repository.acceptQuestCommitment(
            questId = qId,
            logicalCycleId = cycleId,
            accountOwnerId = "player_1",
            targetEpochDay = 100L,
            customDeadlineEpochMs = customDeadline,
            gracePeriodMinutes = 30
        )

        assertTrue(result.isSuccess)
        val commitment = result.getOrThrow()
        assertEquals(CommitmentState.ACCEPTED, commitment.state)
        assertEquals(customDeadline, commitment.deadlineEpochMs)
        assertEquals(customDeadline + (30 * 60_000L), commitment.gracePeriodExpiresAtEpochMs)

        val retrieved = repository.getCommitmentForQuestAndCycle(qId, cycleId)
        assertNotNull(retrieved)
        assertEquals(commitment.commitmentId, retrieved!!.commitmentId)
    }

    @Test
    fun rescheduleCommitment_preservesOriginalDeadlineAndAuditsHistory() = runTest {
        val qId = insertTestQuest(2L, "Study Session")
        val cycleId = "cycle_100"
        val originalDeadline = simulatedTime + 3600_000L

        val accepted = repository.acceptQuestCommitment(
            questId = qId,
            logicalCycleId = cycleId,
            accountOwnerId = "player_1",
            targetEpochDay = 100L,
            customDeadlineEpochMs = originalDeadline
        ).getOrThrow()

        val revisedDeadline = originalDeadline + 7200_000L // +2 hours
        val rescheduleResult = repository.requestReschedule(
            commitmentId = accepted.commitmentId,
            revisedDeadlineEpochMs = revisedDeadline,
            reasonCategory = "URGENT_CALL"
        )

        assertTrue(rescheduleResult.isSuccess)
        val updated = rescheduleResult.getOrThrow()
        assertEquals(CommitmentState.RESCHEDULED, updated.state)
        assertEquals(revisedDeadline, updated.deadlineEpochMs)
        assertEquals(originalDeadline, updated.originalDeadlineEpochMs)
        assertEquals(1, updated.rescheduleCount)

        // Second reschedule should succeed
        val secondRevision = revisedDeadline + 3600_000L
        val secondResult = repository.requestReschedule(accepted.commitmentId, secondRevision, "EXTENSION")
        assertTrue(secondResult.isSuccess)
        assertEquals(2, secondResult.getOrThrow().rescheduleCount)

        // Third reschedule exceeds MAX_ALLOWED_RESCHEDULES (2) and must fail
        val thirdResult = repository.requestReschedule(accepted.commitmentId, secondRevision + 3600_000L, "FAIL")
        assertTrue(thirdResult.isFailure)
    }

    @Test
    fun requestAndReviewExemption_pausesAndThenExemptsCommitment() = runTest {
        val qId = insertTestQuest(3L, "Night Run")
        val cycleId = "cycle_100"
        val accepted = repository.acceptQuestCommitment(
            questId = qId,
            logicalCycleId = cycleId,
            accountOwnerId = "player_1",
            targetEpochDay = 100L
        ).getOrThrow()

        val exemption = repository.requestExemption(
            commitmentId = accepted.commitmentId,
            category = ExemptionReasonCategory.INJURY,
            explanation = "Ankle sprain",
            accountOwnerId = "player_1"
        ).getOrThrow()

        assertEquals(ExemptionLifecycleState.PENDING_REVIEW, exemption.lifecycleState)

        // Advance time past grace expiry
        simulatedTime = accepted.gracePeriodExpiresAtEpochMs + 10_000L

        // Evaluation during PENDING_REVIEW should NOT confirm failure
        val failuresWhilePending = repository.evaluateAndRecordFailuresForCycle(cycleId, "TEST_RESET", simulatedTime)
        assertTrue(failuresWhilePending.isEmpty())

        // Approve exemption
        val reviewed = repository.reviewExemption(exemption.exemptionId, isApproved = true, reviewerNotes = "Valid note").getOrThrow()
        assertEquals(ExemptionLifecycleState.APPROVED, reviewed.lifecycleState)

        // Commitment state is now EXEMPTED
        val finalCommitment = repository.getCommitmentById(accepted.commitmentId)
        assertNotNull(finalCommitment)
        assertEquals(CommitmentState.EXEMPTED, finalCommitment!!.state)
    }

    @Test
    fun evaluateAndRecordFailures_confirmsUncompletedCommitmentsPastGraceAndIsIdempotent() = runTest {
        val qId = insertTestQuest(4L, "Core Workout", isCompleted = false)
        val cycleId = "cycle_100"
        val deadline = simulatedTime + 1000L
        val accepted = repository.acceptQuestCommitment(
            questId = qId,
            logicalCycleId = cycleId,
            accountOwnerId = "player_1",
            targetEpochDay = 100L,
            customDeadlineEpochMs = deadline,
            gracePeriodMinutes = 15
        ).getOrThrow()

        // Advance simulated time past grace expiry
        simulatedTime = accepted.gracePeriodExpiresAtEpochMs + 5000L

        val failuresFirstRun = repository.evaluateAndRecordFailuresForCycle(cycleId, "DAILY_RESET", simulatedTime)
        assertEquals(1, failuresFirstRun.size)
        assertEquals(accepted.commitmentId, failuresFirstRun.first().commitmentId)

        val updatedCommitment = repository.getCommitmentById(accepted.commitmentId)
        assertEquals(CommitmentState.FAILED, updatedCommitment?.state)

        // Second run on the same cycle must be idempotent and not create duplicate failures
        val failuresSecondRun = repository.evaluateAndRecordFailuresForCycle(cycleId, "DAILY_RESET_RETRY", simulatedTime)
        assertEquals(0, failuresSecondRun.size)
        assertEquals(1, repository.getFailureCount())
    }

    @Test
    fun recurringCycles_maintainSeparateIdentitiesAndDoNotCollide() = runTest {
        val qId = insertTestQuest(5L, "Recurring Quest", isCompleted = false)

        // Cycle 1 on Day 100
        val commitDay1 = repository.acceptQuestCommitment(
            questId = qId,
            logicalCycleId = "cycle_100",
            accountOwnerId = "player_1",
            targetEpochDay = 100L,
            customDeadlineEpochMs = simulatedTime + 1000L
        ).getOrThrow()

        // Cycle 2 on Day 101 for the same quest row!
        val commitDay2 = repository.acceptQuestCommitment(
            questId = qId,
            logicalCycleId = "cycle_101",
            accountOwnerId = "player_1",
            targetEpochDay = 101L,
            customDeadlineEpochMs = simulatedTime + 86400_000L
        ).getOrThrow()

        assertTrue(commitDay1.commitmentId != commitDay2.commitmentId)

        // Expire cycle 1 only
        simulatedTime = commitDay1.gracePeriodExpiresAtEpochMs + 5000L
        val day1Failures = repository.evaluateAndRecordFailuresForCycle("cycle_100", "RESET_100", simulatedTime)
        assertEquals(1, day1Failures.size)

        // Cycle 2 should remain unaffected and active
        val day2Commitment = repository.getCommitmentById(commitDay2.commitmentId)
        assertEquals(CommitmentState.ACCEPTED, day2Commitment?.state)
    }
}
