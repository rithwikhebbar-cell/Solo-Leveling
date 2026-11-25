package com.example

import com.example.domain.engine.MissionPrioritizationEngine
import com.example.domain.model.MissionCategory
import com.example.domain.model.MissionPriority
import com.example.domain.model.MissionSourceModule
import com.example.domain.model.MissionSourceType
import com.example.domain.model.MissionStatus
import com.example.domain.model.RecoveryAssessment
import com.example.domain.model.RecoveryState
import com.example.domain.model.UnifiedMissionItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MissionPrioritizationEngineTest {

    private val todayDay = 20150L
    private val nowMs = todayDay * 24 * 60 * 60_000L + 10 * 3600_000L

    private fun readyRecovery() = RecoveryAssessment(
        state = RecoveryState.READY,
        readinessScore = 88,
        measuredLoadMinutes48h = 30,
        measuredLoadMinutes7d = 120,
        measuredSessions7d = 3,
        consecutiveActiveDays = 2,
        selfReportedDiscomfort = false,
        recommendedActions = listOf("Proceed safely"),
        summaryMessage = "Ready"
    )

    private fun mission(
        id: String,
        title: String = "Mission $id",
        category: MissionCategory = MissionCategory.DAILY,
        module: MissionSourceModule = MissionSourceModule.LIFE,
        status: MissionStatus = MissionStatus.NOT_STARTED,
        priority: MissionPriority = MissionPriority.MEDIUM,
        currentValue: Int = 0,
        targetValue: Int = 1,
        deadlineDay: Long? = todayDay,
        prerequisites: List<String> = emptyList(),
        isDemandingPhysical: Boolean = false
    ) = UnifiedMissionItem(
        missionId = id,
        sourceType = MissionSourceType.PERSONAL_TASK,
        sourceRecordId = id.hashCode().toLong(),
        title = title,
        description = "Test objective $id",
        category = category,
        module = module,
        status = status,
        priority = priority,
        currentValue = currentValue,
        targetValue = targetValue,
        unit = "unit",
        deadlineEpochDay = deadlineDay,
        prerequisiteMissionIds = prerequisites,
        isDemandingPhysical = isDemandingPhysical
    )

    @Test
    fun `validateMissionInput rejects blank title, invalid target, and inverted dates`() {
        assertFalse(MissionPrioritizationEngine.validateMissionInput("   ", 1, todayDay, todayDay).isValid)
        assertFalse(MissionPrioritizationEngine.validateMissionInput("Valid", 0, todayDay, todayDay).isValid)
        assertFalse(MissionPrioritizationEngine.validateMissionInput("Valid", 5, todayDay + 3, todayDay + 1).isValid)
        assertTrue(MissionPrioritizationEngine.validateMissionInput("Complete Review", 2, todayDay, todayDay + 2).isValid)
    }

    @Test
    fun `validateDependencies rejects self, missing ids, and transitive cycles while allowing valid DAG`() {
        val known = setOf("m1", "m2", "m3", "m4")
        val graph = mapOf(
            "m1" to emptyList(),
            "m2" to listOf("m1"),
            "m3" to listOf("m2"),
            "m4" to emptyList()
        )

        val selfResult = MissionPrioritizationEngine.validateDependencies("m1", listOf("m1"), graph, known)
        assertFalse(selfResult.isValid)
        assertTrue(selfResult.errorMessage!!.contains("itself", ignoreCase = true))

        val missingResult = MissionPrioritizationEngine.validateDependencies("m4", listOf("m999"), graph, known)
        assertFalse(missingResult.isValid)

        // Making m1 depend on m3 forms cycle m1 -> m3 -> m2 -> m1
        val cycleResult = MissionPrioritizationEngine.validateDependencies("m1", listOf("m3"), graph, known)
        assertFalse(cycleResult.isValid)
        assertTrue(cycleResult.errorMessage!!.contains("Circular", ignoreCase = true))

        val validResult = MissionPrioritizationEngine.validateDependencies("m4", listOf("m3"), graph, known)
        assertTrue(validResult.isValid)
    }

    @Test
    fun `evaluateSnapshot flags overdue items and orders unblocked priority cleanly`() {
        val overdue = mission("m1", title = "Overdue Report", deadlineDay = todayDay - 1, priority = MissionPriority.HIGH)
        val dueToday = mission("m2", title = "Due Today Task", deadlineDay = todayDay, priority = MissionPriority.HIGH)
        val future = mission("m3", title = "Next Week Prep", category = MissionCategory.WEEKLY, deadlineDay = todayDay + 5, priority = MissionPriority.LOW)
        val completed = mission("m4", title = "Finished Objective", status = MissionStatus.COMPLETED, currentValue = 1, targetValue = 1)

        val snapshot = MissionPrioritizationEngine.evaluateSnapshot(
            rawMissions = listOf(future, completed, dueToday, overdue),
            recovery = readyRecovery(),
            todayEpochDay = todayDay,
            nowEpochMs = nowMs
        )

        assertEquals(1, snapshot.overview.overdueCount)
        assertEquals(1, snapshot.overview.completedCount)
        assertEquals(3, snapshot.overview.activeCount)
        assertEquals("m1", snapshot.priorityQueue.first().missionId)
        assertTrue(snapshot.priorityQueue.first().isOverdue)
        assertEquals(0, snapshot.allNonArchived.first { it.missionId == "m4" }.priorityScore)
    }

    @Test
    fun `evaluateSnapshot blocks dependent missions until prerequisites are completed`() {
        val prereqPending = mission("p1", title = "Study Chapter 1", status = MissionStatus.ACTIVE)
        val dependent = mission(
            id = "d1",
            title = "Attempt Practice Quiz",
            priority = MissionPriority.CRITICAL,
            prerequisites = listOf("p1")
        )

        val blockedSnapshot = MissionPrioritizationEngine.evaluateSnapshot(
            rawMissions = listOf(prereqPending, dependent),
            recovery = readyRecovery(),
            todayEpochDay = todayDay,
            nowEpochMs = nowMs
        )
        val evalBlocked = blockedSnapshot.allNonArchived.first { it.missionId == "d1" }
        assertTrue(evalBlocked.isBlocked)
        assertEquals(listOf("Study Chapter 1"), evalBlocked.blockedByTitles)
        assertEquals(1, blockedSnapshot.overview.blockedCount)
        assertEquals("p1", blockedSnapshot.priorityQueue.first().missionId)

        val prereqDone = prereqPending.copy(status = MissionStatus.COMPLETED, currentValue = 1)
        val unblockedSnapshot = MissionPrioritizationEngine.evaluateSnapshot(
            rawMissions = listOf(prereqDone, dependent),
            recovery = readyRecovery(),
            todayEpochDay = todayDay,
            nowEpochMs = nowMs
        )
        val evalUnblocked = unblockedSnapshot.allNonArchived.first { it.missionId == "d1" }
        assertFalse(evalUnblocked.isBlocked)
        assertTrue(evalUnblocked.blockedByTitles.isEmpty())
    }

    @Test
    fun `evaluateSnapshot restricts demanding physical missions under REST_RECOMMENDED`() {
        val restRecovery = readyRecovery().copy(
            state = RecoveryState.REST_RECOMMENDED,
            readinessScore = 25,
            selfReportedDiscomfort = true
        )
        val runMission = mission(
            id = "phys1",
            title = "Tempo 5K Run",
            module = MissionSourceModule.PHYSICAL,
            priority = MissionPriority.CRITICAL,
            isDemandingPhysical = true
        )
        val studyMission = mission(
            id = "acad1",
            title = "Systems Review",
            module = MissionSourceModule.ACADEMIC,
            priority = MissionPriority.MEDIUM
        )

        val snapshot = MissionPrioritizationEngine.evaluateSnapshot(
            rawMissions = listOf(runMission, studyMission),
            recovery = restRecovery,
            todayEpochDay = todayDay,
            nowEpochMs = nowMs
        )

        val evaluatedRun = snapshot.allNonArchived.first { it.missionId == "phys1" }
        assertTrue(evaluatedRun.isRecoveryRestricted)
        assertTrue(evaluatedRun.priorityReason.contains("Recovery", ignoreCase = true))
        assertEquals("acad1", snapshot.priorityQueue.first().missionId)
    }
}
