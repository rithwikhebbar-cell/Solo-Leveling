package com.example.domain.engine

import com.example.domain.model.MissionCategory
import com.example.domain.model.MissionControlSnapshot
import com.example.domain.model.MissionOverviewMetrics
import com.example.domain.model.MissionPriority
import com.example.domain.model.MissionSourceModule
import com.example.domain.model.MissionStatus
import com.example.domain.model.MissionValidationResult
import com.example.domain.model.RecoveryAssessment
import com.example.domain.model.RecoveryState
import com.example.domain.model.UnifiedMissionItem

/**
 * Deterministic Mission Prioritization, Dependency Graph Validation, and Recovery Guardrails.
 * Advisory/orchestration only: never mutates XP, attributes, rank, or auto-completes objectives.
 */
object MissionPrioritizationEngine {

    fun validateMissionInput(
        title: String,
        targetValue: Int,
        startDateEpochDay: Long?,
        deadlineEpochDay: Long?
    ): MissionValidationResult {
        if (title.trim().isBlank()) {
            return MissionValidationResult(false, "Mission title cannot be empty.")
        }
        if (title.trim().length > 120) {
            return MissionValidationResult(false, "Mission title must be 120 characters or fewer.")
        }
        if (targetValue <= 0) {
            return MissionValidationResult(false, "Target value must be at least 1.")
        }
        if (startDateEpochDay != null && deadlineEpochDay != null && deadlineEpochDay < startDateEpochDay) {
            return MissionValidationResult(false, "Deadline cannot precede start date.")
        }
        return MissionValidationResult(true)
    }

    /**
     * Prevents self-dependencies, unknown prerequisites, and cycles in the mission dependency DAG.
     */
    fun validateDependencies(
        missionId: String,
        proposedPrerequisites: List<String>,
        existingGraph: Map<String, List<String>>,
        knownMissionIds: Set<String>
    ): MissionValidationResult {
        val cleanProposed = proposedPrerequisites.distinct().filter { it.isNotBlank() }
        if (missionId in cleanProposed) {
            return MissionValidationResult(false, "A mission cannot depend on itself.")
        }
        val missing = cleanProposed.firstOrNull { it !in knownMissionIds }
        if (missing != null) {
            return MissionValidationResult(false, "Selected prerequisite mission no longer exists.")
        }

        // Check if any proposed prerequisite transitively depends back on missionId
        val visited = mutableSetOf<String>()
        val stack = ArrayDeque<String>()
        cleanProposed.forEach { stack.addLast(it) }
        while (stack.isNotEmpty()) {
            val current = stack.removeLast()
            if (current == missionId) {
                return MissionValidationResult(false, "Circular mission dependency detected.")
            }
            if (visited.add(current)) {
                existingGraph[current].orEmpty().forEach { dep ->
                    stack.addLast(dep)
                }
            }
        }
        return MissionValidationResult(true)
    }

    fun evaluateSnapshot(
        rawMissions: List<UnifiedMissionItem>,
        recovery: RecoveryAssessment,
        todayEpochDay: Long,
        nowEpochMs: Long
    ): MissionControlSnapshot {
        val byId = rawMissions.associateBy { it.missionId }
        val recoveryRestrictedNow = recovery.state == RecoveryState.REST_RECOMMENDED ||
            recovery.state == RecoveryState.RECOVERY_RECOMMENDED ||
            recovery.selfReportedDiscomfort

        val enriched = rawMissions.map { item ->
            val unresolvedPrereqs = item.prerequisiteMissionIds
                .mapNotNull { byId[it] }
                .filter { it.status != MissionStatus.COMPLETED }
            val isBlocked = unresolvedPrereqs.isNotEmpty() && !item.status.isTerminal
            val blockedByTitles = unresolvedPrereqs.map { it.title }

            val overdueByDay = item.deadlineEpochDay != null && item.deadlineEpochDay < todayEpochDay
            val overdueByMs = item.deadlineEpochMs != null && item.deadlineEpochMs < nowEpochMs
            val isOverdue = !item.status.isTerminal &&
                item.status != MissionStatus.PAUSED &&
                (overdueByDay || overdueByMs)

            val isRecoveryRestricted = !item.status.isTerminal &&
                recoveryRestrictedNow &&
                (item.module == MissionSourceModule.PHYSICAL || item.isDemandingPhysical)

            val (score, reason) = computePriorityScoreAndReason(
                item = item,
                isBlocked = isBlocked,
                blockedByTitles = blockedByTitles,
                isOverdue = isOverdue,
                isRecoveryRestricted = isRecoveryRestricted,
                recovery = recovery,
                todayEpochDay = todayEpochDay
            )

            item.copy(
                blockedByTitles = blockedByTitles,
                isBlocked = isBlocked,
                isOverdue = isOverdue,
                isRecoveryRestricted = isRecoveryRestricted,
                priorityScore = score,
                priorityReason = reason
            )
        }

        val activeMissions = enriched
            .filter { it.status == MissionStatus.ACTIVE || it.status == MissionStatus.NOT_STARTED }
            .sortedWith(
                compareByDescending<UnifiedMissionItem> { !it.isBlocked && !it.isRecoveryRestricted }
                    .thenByDescending { it.priorityScore }
                    .thenBy { it.deadlineEpochDay ?: Long.MAX_VALUE }
            )

        val priorityQueue = activeMissions
            .sortedWith(
                compareByDescending<UnifiedMissionItem> { !it.isBlocked && !it.isRecoveryRestricted }
                    .thenByDescending { it.isOverdue }
                    .thenByDescending { it.priorityScore }
            )
            .take(8)

        val dailyMissions = enriched
            .filter { it.category == MissionCategory.DAILY && it.status != MissionStatus.ARCHIVED }
            .sortedWith(
                compareBy<UnifiedMissionItem> { it.status.isTerminal }
                    .thenByDescending { it.priorityScore }
            )

        val weeklyMissions = enriched
            .filter { it.category == MissionCategory.WEEKLY && it.status != MissionStatus.ARCHIVED }
            .sortedWith(
                compareBy<UnifiedMissionItem> { it.status.isTerminal }
                    .thenByDescending { it.priorityScore }
            )

        val longTermMissions = enriched
            .filter { it.category == MissionCategory.LONG_TERM && it.status != MissionStatus.ARCHIVED }
            .sortedWith(
                compareBy<UnifiedMissionItem> { it.status.isTerminal }
                    .thenByDescending { it.priorityScore }
            )

        val historyMissions = enriched
            .filter { it.status == MissionStatus.COMPLETED || it.status == MissionStatus.FAILED || it.status == MissionStatus.ARCHIVED }
            .sortedByDescending { it.completedAtEpochMs ?: it.deadlineEpochMs ?: 0L }

        val dailyCompleted = dailyMissions.count { it.status == MissionStatus.COMPLETED }
        val dailyTotal = dailyMissions.size
        val dailyRate = if (dailyTotal == 0) 0 else ((dailyCompleted * 100f) / dailyTotal).toInt().coerceIn(0, 100)

        val weeklyCompleted = weeklyMissions.count { it.status == MissionStatus.COMPLETED }
        val weeklyTotal = weeklyMissions.size
        val weeklyProgress = if (weeklyTotal == 0) {
            0
        } else {
            weeklyMissions.map { it.progressPercent }.average().toInt().coerceIn(0, 100)
        }

        val upcomingDeadlines = enriched
            .filter { !it.status.isTerminal && it.status != MissionStatus.PAUSED && it.deadlineEpochDay != null }
            .sortedBy { it.deadlineEpochDay ?: Long.MAX_VALUE }
            .take(5)

        val overview = MissionOverviewMetrics(
            activeCount = activeMissions.size,
            completedCount = enriched.count { it.status == MissionStatus.COMPLETED },
            overdueCount = enriched.count { it.isOverdue },
            pausedCount = enriched.count { it.status == MissionStatus.PAUSED },
            blockedCount = activeMissions.count { it.isBlocked },
            dailyCompletedCount = dailyCompleted,
            dailyTotalCount = dailyTotal,
            dailyCompletionRatePercent = dailyRate,
            weeklyCompletedCount = weeklyCompleted,
            weeklyTotalCount = weeklyTotal,
            weeklyProgressPercent = weeklyProgress,
            upcomingDeadlines = upcomingDeadlines
        )

        return MissionControlSnapshot(
            overview = overview,
            priorityQueue = priorityQueue,
            activeMissions = activeMissions,
            dailyMissions = dailyMissions,
            weeklyMissions = weeklyMissions,
            longTermMissions = longTermMissions,
            historyMissions = historyMissions,
            allNonArchived = enriched.filter { it.status != MissionStatus.ARCHIVED },
            recoveryState = recovery.state,
            evaluationEpochMs = nowEpochMs
        )
    }

    private fun computePriorityScoreAndReason(
        item: UnifiedMissionItem,
        isBlocked: Boolean,
        blockedByTitles: List<String>,
        isOverdue: Boolean,
        isRecoveryRestricted: Boolean,
        recovery: RecoveryAssessment,
        todayEpochDay: Long
    ): Pair<Int, String> {
        if (item.status == MissionStatus.COMPLETED) return 0 to "Completed"
        if (item.status == MissionStatus.ARCHIVED) return 0 to "Archived"
        if (item.status == MissionStatus.FAILED) return 0 to "Expired / Failed"
        if (item.status == MissionStatus.PAUSED) return 5 to "Paused by player"

        var score = when (item.priority) {
            MissionPriority.CRITICAL -> 42
            MissionPriority.HIGH -> 30
            MissionPriority.MEDIUM -> 18
            MissionPriority.LOW -> 8
        }

        val reasons = mutableListOf<String>()

        if (isOverdue) {
            score += 34
            reasons.add("Overdue deadline")
        } else if (item.deadlineEpochDay != null) {
            val daysLeft = item.deadlineEpochDay - todayEpochDay
            when {
                daysLeft == 0L -> {
                    score += 26
                    reasons.add("Due today")
                }
                daysLeft in 1L..2L -> {
                    score += 18
                    reasons.add("Due in ${daysLeft}d")
                }
                daysLeft in 3L..7L -> {
                    score += 10
                    reasons.add("Due in ${daysLeft}d")
                }
            }
        }

        when (item.category) {
            MissionCategory.DAILY -> {
                score += 12
                if (reasons.isEmpty()) reasons.add("Daily directive")
            }
            MissionCategory.WEEKLY -> {
                score += 8
                if (reasons.isEmpty()) reasons.add("Weekly cadence")
            }
            MissionCategory.LONG_TERM -> {
                score += 4
                if (reasons.isEmpty()) reasons.add("Long-term objective")
            }
        }

        if (item.progressFraction >= 0.5f) {
            score += 8
            reasons.add("${item.progressPercent}% complete")
        }

        if (isBlocked) {
            score = (score - 45).coerceAtLeast(1)
            reasons.add(0, "Blocked by: ${blockedByTitles.firstOrNull() ?: "prerequisite"}")
        }

        if (isRecoveryRestricted) {
            score = (score - 50).coerceAtLeast(1)
            reasons.add(0, "Recovery ${recovery.state.code}: defer strenuous physical exertion")
        }

        if (reasons.isEmpty()) {
            reasons.add("${item.priority.label} priority")
        }

        return score.coerceIn(1, 100) to reasons.joinToString(" • ")
    }
}
