package com.example.domain.engine

import com.example.domain.model.DailyExecutionItem
import com.example.domain.model.DailyExecutionStatus
import com.example.domain.model.DailyExecutionSummary
import com.example.domain.model.RecoveryAssessment
import com.example.domain.model.RecoveryState
import com.example.domain.model.ScheduledActivityItem
import com.example.domain.model.ScheduledSourceType
import com.example.domain.model.TimeSlotSuggestion

data class DailyExecutionOverride(
    val executionKey: String,
    val status: DailyExecutionStatus,
    val actualStartEpochMs: Long? = null,
    val actualCompletionEpochMs: Long? = null,
    val completedMinutes: Int = 0,
    val plannedStartMinuteOverride: Int? = null,
    val evidenceReference: String = "",
    val notes: String = ""
)

/**
 * Coordinates the unified daily execution timeline across Physical, Academic, Life, and Adaptive plans.
 * INVARIANT: Never marks an item COMPLETED simply because scheduled time has elapsed.
 * Completion requires explicit valid evidence (source completion or recorded evidence reference).
 */
object UnifiedDailyExecutionEngine {

    fun keyFor(epochDay: Long, sourceType: ScheduledSourceType, sourceRecordId: Long): String =
        "${epochDay}_${sourceType.name}_$sourceRecordId"

    fun buildDailyTimeline(
        targetEpochDay: Long,
        scheduledItems: List<ScheduledActivityItem>,
        overridesByKey: Map<String, DailyExecutionOverride>,
        recovery: RecoveryAssessment,
        currentMinuteOfDay: Int,
        todayEpochDay: Long
    ): DailyExecutionSummary {
        val seenKeys = mutableSetOf<String>()
        val isRestMode = recovery.state == RecoveryState.REST_RECOMMENDED ||
            recovery.state == RecoveryState.RECOVERY_RECOMMENDED ||
            recovery.selfReportedDiscomfort

        val items = scheduledItems.mapNotNull { scheduled ->
            val key = keyFor(targetEpochDay, scheduled.sourceType, scheduled.sourceRecordId)
            if (!seenKeys.add(key)) return@mapNotNull null

            val override = overridesByKey[key]
            val plannedStart = override?.plannedStartMinuteOverride ?: scheduled.startMinuteOfDay
            val duration = scheduled.durationMinutes.coerceAtLeast(10)
            val plannedEnd = (plannedStart + duration).coerceAtMost(24 * 60)
            val module = scheduled.sourceType.moduleTag
            val recoveryRestricted = isRestMode &&
                scheduled.sourceType == ScheduledSourceType.PHYSICAL_TRAINING &&
                !scheduled.isCompleted

            val status = resolveStatus(
                sourceCompleted = scheduled.isCompleted,
                override = override,
                targetEpochDay = targetEpochDay,
                todayEpochDay = todayEpochDay,
                plannedStart = plannedStart,
                plannedEnd = plannedEnd,
                currentMinuteOfDay = currentMinuteOfDay,
                isAllDay = scheduled.isAllDay
            )

            DailyExecutionItem(
                executionKey = key,
                sourceModule = module,
                activityType = scheduled.sourceType,
                sourceRecordId = scheduled.sourceRecordId,
                title = scheduled.title,
                subtitle = scheduled.subtitle,
                dateEpochDay = targetEpochDay,
                plannedStartMinute = plannedStart,
                plannedEndMinute = plannedEnd,
                durationMinutes = duration,
                actualStartEpochMs = override?.actualStartEpochMs,
                actualCompletionEpochMs = override?.actualCompletionEpochMs,
                completedMinutes = if (status == DailyExecutionStatus.COMPLETED) duration else (override?.completedMinutes ?: 0),
                status = status,
                priorityWeight = scheduled.priorityWeight,
                isAllDay = scheduled.isAllDay,
                hasConflict = scheduled.hasConflict,
                recoveryRestricted = recoveryRestricted,
                evidenceReference = override?.evidenceReference ?: if (scheduled.isCompleted) "SOURCE_VERIFIED" else "",
                notes = override?.notes.orEmpty()
            )
        }.sortedWith(
            compareBy<DailyExecutionItem> { !it.isAllDay }
                .thenBy { it.plannedStartMinute }
                .thenByDescending { it.priorityWeight }
        )

        return DailyExecutionSummary(
            dateEpochDay = targetEpochDay,
            items = items,
            completedCount = items.count { it.status == DailyExecutionStatus.COMPLETED },
            partialCount = items.count { it.status == DailyExecutionStatus.PARTIALLY_COMPLETED },
            activeOrReadyCount = items.count {
                it.status in setOf(
                    DailyExecutionStatus.READY,
                    DailyExecutionStatus.IN_PROGRESS,
                    DailyExecutionStatus.PAUSED,
                    DailyExecutionStatus.SCHEDULED
                )
            },
            missedCount = items.count { it.status == DailyExecutionStatus.MISSED },
            conflictCount = items.count { it.hasConflict },
            recoveryRestrictedCount = items.count { it.recoveryRestricted }
        )
    }

    private fun resolveStatus(
        sourceCompleted: Boolean,
        override: DailyExecutionOverride?,
        targetEpochDay: Long,
        todayEpochDay: Long,
        plannedStart: Int,
        plannedEnd: Int,
        currentMinuteOfDay: Int,
        isAllDay: Boolean
    ): DailyExecutionStatus {
        if (sourceCompleted) return DailyExecutionStatus.COMPLETED
        if (override != null) {
            // Only honor COMPLETED if valid non-blank completion evidence was supplied
            if (override.status == DailyExecutionStatus.COMPLETED && override.evidenceReference.isBlank()) {
                return DailyExecutionStatus.PARTIALLY_COMPLETED
            }
            return override.status
        }
        return when {
            targetEpochDay < todayEpochDay -> DailyExecutionStatus.MISSED
            targetEpochDay > todayEpochDay -> DailyExecutionStatus.SCHEDULED
            isAllDay -> DailyExecutionStatus.READY
            currentMinuteOfDay > plannedEnd -> DailyExecutionStatus.MISSED
            currentMinuteOfDay >= plannedStart - 15 -> DailyExecutionStatus.READY
            else -> DailyExecutionStatus.SCHEDULED
        }
    }

    fun validateCompletionTransition(
        item: DailyExecutionItem,
        evidenceReference: String
    ): Result<DailyExecutionStatus> {
        if (item.recoveryRestricted) {
            return Result.failure(IllegalStateException("Blocked by recovery safety guardrail (${item.title})."))
        }
        if (evidenceReference.isBlank()) {
            return Result.failure(IllegalArgumentException("Valid completion evidence reference is required; elapsed time alone cannot complete an activity."))
        }
        return Result.success(DailyExecutionStatus.COMPLETED)
    }

    fun findRescheduleSlots(
        item: DailyExecutionItem,
        allItems: List<DailyExecutionItem>,
        targetEpochDay: Long = item.dateEpochDay
    ): List<TimeSlotSuggestion> {
        val scheduledEquivalent = allItems
            .filter { it.executionKey != item.executionKey && it.status != DailyExecutionStatus.CANCELLED && it.status != DailyExecutionStatus.RESCHEDULED }
            .map {
                ScheduledActivityItem(
                    sourceType = it.activityType,
                    sourceRecordId = it.sourceRecordId,
                    title = it.title,
                    subtitle = it.subtitle,
                    dateEpochDay = it.dateEpochDay,
                    startMinuteOfDay = it.plannedStartMinute,
                    durationMinutes = it.durationMinutes,
                    isAllDay = it.isAllDay
                )
            }
        return ScheduleCalendarEngine.suggestAlternativeSlots(
            durationMinutes = item.durationMinutes,
            targetEpochDay = targetEpochDay,
            existingItems = scheduledEquivalent
        )
    }
}
