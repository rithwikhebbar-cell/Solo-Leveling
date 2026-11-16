package com.example.domain.engine

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class DailyCyclePlan(
    val shouldExecute: Boolean,
    val targetEpochDay: Long,
    val missedDaysCount: Int,
    val isMonthBoundary: Boolean,
    val isYearBoundary: Boolean,
    val reason: String
)

/**
 * Deterministic, timezone-aware, idempotent daily cycle calculator.
 * Handles DST transitions, timezone changes, and multi-day absences without erasing history or double-resetting.
 */
object DailyCycleManager {

    fun resolveZone(preferredTimezoneId: String): ZoneId {
        return runCatching { ZoneId.of(preferredTimezoneId) }.getOrElse { ZoneId.systemDefault() }
    }

    fun currentLogicalEpochDay(
        nowEpochMs: Long,
        zoneId: ZoneId,
        dailyResetHour: Int = 4
    ): Long {
        val zoned = Instant.ofEpochMilli(nowEpochMs).atZone(zoneId)
        val safeResetHour = dailyResetHour.coerceIn(0, 23)
        // Subtract reset offset hours via LocalDate/hour so DST transitions remain calendar-accurate
        val logicalDate = if (zoned.hour < safeResetHour) {
            zoned.toLocalDate().minusDays(1)
        } else {
            zoned.toLocalDate()
        }
        return logicalDate.toEpochDay()
    }

    fun evaluateCyclePlan(
        nowEpochMs: Long,
        lastExecutedEpochDay: Long?,
        preferredTimezoneId: String,
        dailyResetHour: Int = 4
    ): DailyCyclePlan {
        val zone = resolveZone(preferredTimezoneId)
        val logicalDay = currentLogicalEpochDay(nowEpochMs, zone, dailyResetHour)
        val logicalDate = LocalDate.ofEpochDay(logicalDay)

        if (lastExecutedEpochDay != null && lastExecutedEpochDay >= logicalDay) {
            return DailyCyclePlan(
                shouldExecute = false,
                targetEpochDay = logicalDay,
                missedDaysCount = 0,
                isMonthBoundary = logicalDate.dayOfMonth == 1,
                isYearBoundary = logicalDate.dayOfYear == 1,
                reason = "Cycle for epochDay=$logicalDay already executed (idempotent skip)."
            )
        }

        val missedDays = if (lastExecutedEpochDay == null) {
            0
        } else {
            (logicalDay - lastExecutedEpochDay - 1L).coerceAtLeast(0L).toInt()
        }

        return DailyCyclePlan(
            shouldExecute = true,
            targetEpochDay = logicalDay,
            missedDaysCount = missedDays,
            isMonthBoundary = logicalDate.dayOfMonth == 1,
            isYearBoundary = logicalDate.dayOfYear == 1,
            reason = if (missedDays > 0) {
                "Advancing to $logicalDate across $missedDays missed day(s) while preserving history."
            } else {
                "Executing daily cycle for $logicalDate ($zone)."
            }
        )
    }

    fun weekBoundsFor(epochDay: Long): Pair<Long, Long> {
        val date = LocalDate.ofEpochDay(epochDay)
        val start = date.with(java.time.DayOfWeek.MONDAY).toEpochDay()
        val end = start + 6L
        return start to end
    }

    fun monthBoundsFor(epochDay: Long): Pair<Long, Long> {
        val date = LocalDate.ofEpochDay(epochDay)
        val start = date.withDayOfMonth(1).toEpochDay()
        val end = date.withDayOfMonth(date.lengthOfMonth()).toEpochDay()
        return start to end
    }
}
