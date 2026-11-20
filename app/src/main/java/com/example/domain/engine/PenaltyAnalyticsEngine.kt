package com.example.domain.engine

import com.example.domain.model.CommitmentState
import com.example.domain.model.ConsistencyMetrics
import com.example.domain.model.ConsistencyTrend
import com.example.domain.model.CorrectiveActivityLifecycleState
import com.example.domain.model.CorrectiveActivityRecord
import com.example.domain.model.FailureTrendAnalysis
import com.example.domain.model.PenaltyAuditTimelineItem
import com.example.domain.model.PenaltyOverviewSummary
import com.example.domain.model.PenaltyRecord
import com.example.domain.model.PenaltyStatus
import com.example.domain.model.PenaltyTimelineType
import com.example.domain.model.PenaltyTransaction
import com.example.domain.model.QuestCommitment
import com.example.domain.model.ConfirmedFailureRecord
import com.example.domain.model.ExemptionLifecycleState
import com.example.domain.model.QuestExemption
import com.example.domain.model.RedemptionPerformanceMetrics
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/**
 * Pure, deterministic analytics engine for the Solo Leveling Penalty System.
 *
 * Guarantees:
 * - Read-only operations (zero mutations to database, progression, or state).
 * - Complete mathematical consistency with double-entry ledger transactions.
 * - Offline-capable and fast calculation.
 */
object PenaltyAnalyticsEngine {

    /**
     * Calculates the high-level KPI overview for the player's Penalty Point balance.
     */
    fun calculateOverviewSummary(
        accountOwnerId: String,
        currentBalance: Int,
        lifetimeAccrued: Int,
        transactions: List<PenaltyTransaction>,
        penalties: List<PenaltyRecord>
    ): PenaltyOverviewSummary {
        val lifetimeDeducted = transactions
            .filter { it.transactionType.isDebit }
            .sumOf { it.pointsDelta }

        val activeCount = penalties.count {
            it.status == PenaltyStatus.ACTIVE || it.status == PenaltyStatus.ESCALATED
        }
        val resolvedCount = penalties.count { it.status == PenaltyStatus.RESOLVED }
        val exemptedCount = penalties.count { it.status == PenaltyStatus.EXEMPTED }

        val totalDecided = resolvedCount + exemptedCount + activeCount
        val recoveryRate = if (totalDecided > 0) {
            val successfulRecovery = resolvedCount + exemptedCount
            ((successfulRecovery.toDouble() / totalDecided.toDouble()) * 100.0)
                .coerceIn(0.0, 100.0)
        } else {
            100.0
        }

        return PenaltyOverviewSummary(
            accountOwnerId = accountOwnerId,
            currentBalance = currentBalance.coerceAtLeast(0),
            lifetimePointsAccrued = lifetimeAccrued.coerceAtLeast(0),
            lifetimePointsDeducted = lifetimeDeducted.coerceAtLeast(0),
            activePenaltiesCount = activeCount.coerceAtLeast(0),
            resolvedPenaltiesCount = resolvedCount.coerceAtLeast(0),
            exemptedPenaltiesCount = exemptedCount.coerceAtLeast(0),
            recoveryRatePercent = (recoveryRate * 10.0).roundToInt() / 10.0
        )
    }

    /**
     * Calculates deterministic failure trends, category clustering, and temporal distribution.
     */
    fun calculateFailureTrends(
        commitments: List<QuestCommitment>,
        failures: List<ConfirmedFailureRecord>,
        questCategoryMap: Map<Long, String> = emptyMap(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): FailureTrendAnalysis {
        val total = commitments.size
        val successful = commitments.count { it.state == CommitmentState.COMPLETED }
        val failed = failures.size
        val exempted = commitments.count { it.state == CommitmentState.EXEMPTED }

        val completionRate = if (total > 0) {
            ((successful.toDouble() / total.toDouble()) * 100.0).coerceIn(0.0, 100.0)
        } else 0.0

        val failureRate = if (total > 0) {
            ((failed.toDouble() / total.toDouble()) * 100.0).coerceIn(0.0, 100.0)
        } else 0.0

        val byCategory = mutableMapOf<String, Int>()
        val byTimeOfDay = mutableMapOf(
            "Morning (04-12)" to 0,
            "Afternoon (12-17)" to 0,
            "Evening (17-22)" to 0,
            "Night (22-04)" to 0
        )
        val byDayOfWeek = mutableMapOf<String, Int>()

        for (f in failures) {
            val cat = questCategoryMap[f.questId] ?: "GENERAL"
            byCategory[cat] = (byCategory[cat] ?: 0) + 1

            val zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(f.failedAtEpochMs), zoneId)
            val hour = zdt.hour
            val timeSlot = when (hour) {
                in 4..11 -> "Morning (04-12)"
                in 12..16 -> "Afternoon (12-17)"
                in 17..21 -> "Evening (17-22)"
                else -> "Night (22-04)"
            }
            byTimeOfDay[timeSlot] = (byTimeOfDay[timeSlot] ?: 0) + 1

            val dow = zdt.dayOfWeek.name
            byDayOfWeek[dow] = (byDayOfWeek[dow] ?: 0) + 1
        }

        return FailureTrendAnalysis(
            totalCommitments = total,
            successfulCommitments = successful,
            failedCommitments = failed,
            exemptedCommitments = exempted,
            completionRatePercent = (completionRate * 10.0).roundToInt() / 10.0,
            failureRatePercent = (failureRate * 10.0).roundToInt() / 10.0,
            failuresByCategory = byCategory,
            failuresByTimeOfDay = byTimeOfDay,
            failuresByDayOfWeek = byDayOfWeek
        )
    }

    /**
     * Analyzes performance and resolution velocity across corrective redemption activities.
     */
    fun calculateRedemptionMetrics(
        activities: List<CorrectiveActivityRecord>
    ): RedemptionPerformanceMetrics {
        val total = activities.size
        val completedList = activities.filter {
            it.lifecycleState == CorrectiveActivityLifecycleState.COMPLETED ||
                    it.lifecycleState == CorrectiveActivityLifecycleState.VERIFIED
        }
        val completedCount = completedList.size
        val rescheduledCount = activities.count { it.rescheduleCount > 0 }
        val expiredCount = activities.count {
            it.lifecycleState == CorrectiveActivityLifecycleState.EXPIRED
        }

        val successRate = if (total > 0) {
            ((completedCount.toDouble() / total.toDouble()) * 100.0).coerceIn(0.0, 100.0)
        } else 100.0

        val totalResolutionHours = completedList.mapNotNull {
            val completedTime = it.completedAtEpochMs ?: it.verifiedAtEpochMs
            if (completedTime != null && completedTime >= it.createdAtEpochMs) {
                (completedTime - it.createdAtEpochMs).toDouble() / (1000.0 * 60.0 * 60.0)
            } else null
        }
        val avgHours = if (totalResolutionHours.isNotEmpty()) {
            totalResolutionHours.average()
        } else 0.0

        return RedemptionPerformanceMetrics(
            activitiesAssigned = total,
            activitiesCompleted = completedCount,
            activitiesRescheduled = rescheduledCount,
            activitiesExpired = expiredCount,
            redemptionSuccessRatePercent = (successRate * 10.0).roundToInt() / 10.0,
            averageResolutionHours = (avgHours * 10.0).roundToInt() / 10.0
        )
    }

    /**
     * Calculates clean streak consistency metrics.
     */
    fun calculateConsistencyMetrics(
        commitments: List<QuestCommitment>,
        failures: List<ConfirmedFailureRecord>,
        nowEpochMs: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): ConsistencyMetrics {
        if (failures.isEmpty()) {
            val daysActive = if (commitments.isNotEmpty()) {
                val oldest = commitments.minOf { it.deadlineEpochMs }
                val days = ChronoUnit.DAYS.between(
                    Instant.ofEpochMilli(oldest).atZone(zoneId).toLocalDate(),
                    Instant.ofEpochMilli(nowEpochMs).atZone(zoneId).toLocalDate()
                ).toInt().coerceAtLeast(1)
                days
            } else 0
            return ConsistencyMetrics(
                currentCleanStreakDays = daysActive,
                longestCleanStreakDays = daysActive,
                lastFailureEpochMs = null,
                streakTrend = ConsistencyTrend.IMPROVING
            )
        }

        val sortedFailures = failures.sortedByDescending { it.failedAtEpochMs }
        val lastFailure = sortedFailures.first().failedAtEpochMs

        val daysSinceLastFailure = ChronoUnit.DAYS.between(
            Instant.ofEpochMilli(lastFailure).atZone(zoneId).toLocalDate(),
            Instant.ofEpochMilli(nowEpochMs).atZone(zoneId).toLocalDate()
        ).toInt().coerceAtLeast(0)

        // Calculate longest streak between failures
        var longestStreak = daysSinceLastFailure
        val ascFailures = failures.sortedBy { it.failedAtEpochMs }
        for (i in 0 until ascFailures.size - 1) {
            val gap = ChronoUnit.DAYS.between(
                Instant.ofEpochMilli(ascFailures[i].failedAtEpochMs).atZone(zoneId).toLocalDate(),
                Instant.ofEpochMilli(ascFailures[i + 1].failedAtEpochMs).atZone(zoneId).toLocalDate()
            ).toInt().coerceAtLeast(0)
            if (gap > longestStreak) {
                longestStreak = gap
            }
        }

        val trend = when {
            daysSinceLastFailure >= 7 -> ConsistencyTrend.IMPROVING
            daysSinceLastFailure in 3..6 -> ConsistencyTrend.STABLE
            daysSinceLastFailure in 1..2 -> ConsistencyTrend.RECOVERING
            else -> ConsistencyTrend.AT_RISK
        }

        return ConsistencyMetrics(
            currentCleanStreakDays = daysSinceLastFailure,
            longestCleanStreakDays = longestStreak,
            lastFailureEpochMs = lastFailure,
            streakTrend = trend
        )
    }

    /**
     * Builds a comprehensive, unified chronological audit timeline
     * linking commitments, failures, penalty allocations, redemptions, exemptions, and reversals.
     */
    fun buildAuditTimeline(
        commitments: List<QuestCommitment>,
        failures: List<ConfirmedFailureRecord>,
        penalties: List<PenaltyRecord>,
        transactions: List<PenaltyTransaction>,
        redemptions: List<CorrectiveActivityRecord>,
        exemptions: List<QuestExemption>
    ): List<PenaltyAuditTimelineItem> {
        val items = mutableListOf<PenaltyAuditTimelineItem>()

        for (c in commitments) {
            c.acceptedAtEpochMs?.let { acceptedAt ->
                items.add(
                    PenaltyAuditTimelineItem(
                        id = "commit_${c.commitmentId}",
                        timestampEpochMs = acceptedAt,
                        type = PenaltyTimelineType.COMMITMENT,
                        title = "Quest Commitment Accepted",
                        detail = "Cycle: ${c.logicalCycleId} • Grace Period: ${c.gracePeriodMinutes}m",
                        statusTag = c.state.name
                    )
                )
            }
        }

        for (f in failures) {
            items.add(
                PenaltyAuditTimelineItem(
                    id = "fail_${f.failureId}",
                    timestampEpochMs = f.failedAtEpochMs,
                    type = PenaltyTimelineType.FAILURE,
                    title = "Quest Failure Confirmed",
                    detail = "Reason: ${f.reasonCode} • Trigger: ${f.triggerSource}",
                    statusTag = "CONFIRMED"
                )
            )
        }

        for (p in penalties) {
            items.add(
                PenaltyAuditTimelineItem(
                    id = "pen_${p.penaltyId}",
                    timestampEpochMs = p.createdAtEpochMs,
                    type = PenaltyTimelineType.ALLOCATION,
                    title = "Penalty Allocated: +${p.totalPenaltyPoints} PP",
                    detail = "${p.questTitleSnapshot} • Severity: ${p.severity.code}",
                    statusTag = p.status.name
                )
            )
            if (p.escalationBonusPoints > 0) {
                items.add(
                    PenaltyAuditTimelineItem(
                        id = "esc_${p.penaltyId}",
                        timestampEpochMs = p.createdAtEpochMs + 1,
                        type = PenaltyTimelineType.ESCALATION,
                        title = "Escalation Bonus: +${p.escalationBonusPoints} PP",
                        detail = "Consecutive failure escalation threshold reached",
                        statusTag = "ESCALATED"
                    )
                )
            }
            if (p.status == PenaltyStatus.RESOLVED && p.resolvedAtEpochMs != null) {
                items.add(
                    PenaltyAuditTimelineItem(
                        id = "res_${p.penaltyId}",
                        timestampEpochMs = p.resolvedAtEpochMs,
                        type = PenaltyTimelineType.PENALTY_RESOLUTION,
                        title = "Penalty Resolved",
                        detail = "Corrective recovery obligations completed",
                        statusTag = "RESOLVED"
                    )
                )
            }
        }

        for (r in redemptions) {
            items.add(
                PenaltyAuditTimelineItem(
                    id = "red_start_${r.activityId}",
                    timestampEpochMs = r.createdAtEpochMs,
                    type = PenaltyTimelineType.REDEMPTION_START,
                    title = "Redemption Activity: ${r.title}",
                    detail = "Value: ${r.pointValue} PP • Focus: ${r.category.name}",
                    statusTag = r.lifecycleState.name
                )
            )
            r.verifiedAtEpochMs?.let { verifiedAt ->
                items.add(
                    PenaltyAuditTimelineItem(
                        id = "red_ver_${r.activityId}",
                        timestampEpochMs = verifiedAt,
                        type = PenaltyTimelineType.REDEMPTION_VERIFICATION,
                        title = "Redemption Verified: ${r.title}",
                        detail = "Verification method: ${r.verificationMethod.name}",
                        statusTag = "VERIFIED"
                    )
                )
            }
        }

        for (ex in exemptions) {
            items.add(
                PenaltyAuditTimelineItem(
                    id = "ex_req_${ex.exemptionId}",
                    timestampEpochMs = ex.requestedAtEpochMs,
                    type = PenaltyTimelineType.EXEMPTION_REQUEST,
                    title = "Exemption Requested: ${ex.category.name}",
                    detail = ex.explanation.take(80),
                    statusTag = ex.lifecycleState.name
                )
            )
            if (ex.lifecycleState == ExemptionLifecycleState.APPROVED && ex.reviewedAtEpochMs != null) {
                items.add(
                    PenaltyAuditTimelineItem(
                        id = "ex_app_${ex.exemptionId}",
                        timestampEpochMs = ex.reviewedAtEpochMs,
                        type = PenaltyTimelineType.EXEMPTION_APPROVAL,
                        title = "Exemption Approved",
                        detail = ex.reviewerNotes ?: "Approved under ${ex.category.label}",
                        statusTag = "APPROVED"
                    )
                )
            } else if (ex.lifecycleState == ExemptionLifecycleState.DENIED && ex.reviewedAtEpochMs != null) {
                items.add(
                    PenaltyAuditTimelineItem(
                        id = "ex_den_${ex.exemptionId}",
                        timestampEpochMs = ex.reviewedAtEpochMs,
                        type = PenaltyTimelineType.EXEMPTION_DENIAL,
                        title = "Exemption Denied",
                        detail = ex.reviewerNotes ?: "Standard requirements upheld",
                        statusTag = "DENIED"
                    )
                )
            }
        }

        for (tx in transactions) {
            if (tx.transactionType == com.example.domain.model.PenaltyTransactionType.REVERSAL) {
                items.add(
                    PenaltyAuditTimelineItem(
                        id = "rev_${tx.transactionId}",
                        timestampEpochMs = tx.timestampEpochMs,
                        type = PenaltyTimelineType.REVERSAL,
                        title = "Ledger Reversal: -${tx.pointsDelta} PP",
                        detail = tx.reason,
                        statusTag = "REVERSED"
                    )
                )
            }
        }

        return items.sortedByDescending { it.timestampEpochMs }
    }
}
