package com.example.domain.engine

import com.example.domain.model.ConfidenceLevel
import com.example.domain.model.ConsistencyPrediction
import com.example.domain.model.DataProvenanceTag
import com.example.domain.model.FindingCategory
import com.example.domain.model.FindingKind
import com.example.domain.model.GoalStatus
import com.example.domain.model.HabitDayStatus
import com.example.domain.model.PlayerIntelligenceProfile
import com.example.domain.model.RecoveryState
import com.example.domain.model.StrengthWeaknessFinding
import com.example.domain.model.TrendDataPoint
import java.time.Instant
import java.util.UUID
import kotlin.math.roundToInt

/**
 * Identifies structured strengths, weaknesses, bottlenecks, and risks grounded strictly in stored data.
 * Enforces explicit provenance tags, confidence ratings, and insufficient-data guards.
 */
object StrengthsAndWeaknessesEngine {

    private const val DAY_MS = 24 * 60 * 60_000L

    fun analyze(
        input: IntelligenceTelemetryInput,
        profile: PlayerIntelligenceProfile
    ): List<StrengthWeaknessFinding> {
        val findings = mutableListOf<StrengthWeaknessFinding>()
        val now = input.nowEpochMs
        val sevenDaysAgo = now - 7 * DAY_MS

        if (!profile.hasMinimumBaseline) {
            findings.add(
                StrengthWeaknessFinding(
                    findingUuid = UUID.randomUUID().toString(),
                    kind = FindingKind.RISK,
                    category = FindingCategory.DATA_COMPLETENESS,
                    title = "Insufficient Activity Baseline",
                    description = "Fewer than 3 active days or sessions are recorded. Patterns and predictions remain preliminary until more real sessions are logged.",
                    evidenceSummary = "${profile.activeDaysLast30} active day(s) observed in the last 30 days.",
                    dateRangeLabel = "Last 30 days",
                    provenance = DataProvenanceTag.MEASURED,
                    confidence = ConfidenceLevel.INSUFFICIENT_DATA,
                    priorityRank = 1,
                    insufficientData = true,
                    createdAtEpochMs = now
                )
            )
        }

        // Physical & Recovery findings
        val physMins7d = input.joggingSessions.filter { it.startEpochMs >= sevenDaysAgo }.sumOf { it.durationMinutes } +
            input.workoutSessions.filter { it.startEpochMs >= sevenDaysAgo }.sumOf { it.durationMinutes }
        val verifiedPhysCount = input.joggingSessions.count { it.verificationStatus.isVerified } +
            input.workoutSessions.count { it.verificationStatus.isVerified }

        if (input.recovery.state == RecoveryState.REST_RECOMMENDED || input.recovery.selfReportedDiscomfort) {
            findings.add(
                StrengthWeaknessFinding(
                    findingUuid = UUID.randomUUID().toString(),
                    kind = FindingKind.RISK,
                    category = FindingCategory.RECOVERY,
                    title = "Recovery Priority Required",
                    description = "Readiness score is ${input.recovery.readinessScore}/100 (${input.recovery.state.code}). Rest is prioritized without progression penalty.",
                    evidenceSummary = "48h load=${input.recovery.measuredLoadMinutes48h}m, discomfort=${input.recovery.selfReportedDiscomfort}.",
                    dateRangeLabel = "Last 48 hours",
                    provenance = if (input.recovery.selfReportedDiscomfort) DataProvenanceTag.SELF_REPORTED else DataProvenanceTag.MEASURED,
                    confidence = ConfidenceLevel.HIGH,
                    priorityRank = 1,
                    createdAtEpochMs = now
                )
            )
        } else if (physMins7d >= 90) {
            findings.add(
                StrengthWeaknessFinding(
                    findingUuid = UUID.randomUUID().toString(),
                    kind = FindingKind.STRENGTH,
                    category = FindingCategory.PHYSICAL,
                    title = "Consistent Physical Output",
                    description = "Accumulated $physMins7d active minutes across jogging and workouts in the last 7 days while maintaining ${input.recovery.state.code} readiness.",
                    evidenceSummary = "${verifiedPhysCount} verified physical record(s); 7d total=${physMins7d}m.",
                    dateRangeLabel = "Last 7 days",
                    provenance = if (verifiedPhysCount > 0) DataProvenanceTag.MEASURED else DataProvenanceTag.SELF_REPORTED,
                    confidence = ConfidenceLevel.HIGH,
                    priorityRank = 2,
                    createdAtEpochMs = now
                )
            )
        }

        // Verification provenance bottleneck
        val totalActivities = input.profile.verifiedActivitiesCount + input.profile.selfReportedActivitiesCount
        if (totalActivities > 0 && input.profile.verifiedActivitiesCount == 0) {
            findings.add(
                StrengthWeaknessFinding(
                    findingUuid = UUID.randomUUID().toString(),
                    kind = FindingKind.BOTTLENECK,
                    category = FindingCategory.RPG,
                    title = "Zero Verified Sessions Blocking Rank Ascension",
                    description = "Only self-reported activities are recorded. Higher Hunter Ranks require Health Connect, Focus Timer, or Assessment verified records.",
                    evidenceSummary = "Verified=${input.profile.verifiedActivitiesCount}, Self-Reported=${input.profile.selfReportedActivitiesCount}.",
                    dateRangeLabel = "All time",
                    provenance = DataProvenanceTag.MEASURED,
                    confidence = ConfidenceLevel.HIGH,
                    priorityRank = 1,
                    createdAtEpochMs = now
                )
            )
        }

        // Academic findings
        val overdueRevs = input.topics.count { (it.nextRevisionDueEpochMs ?: Long.MAX_VALUE) < now }
        if (overdueRevs > 0) {
            findings.add(
                StrengthWeaknessFinding(
                    findingUuid = UUID.randomUUID().toString(),
                    kind = FindingKind.WEAKNESS,
                    category = FindingCategory.ACADEMIC,
                    title = "Spaced Revision Backlog ($overdueRevs topic(s) overdue)",
                    description = "Overdue topics show declining estimated familiarity in Revision Centre. Scheduling short recall blocks stabilizes retention.",
                    evidenceSummary = "$overdueRevs topic(s) past scheduled nextRevisionDueEpochMs.",
                    dateRangeLabel = "Current revision queue",
                    provenance = DataProvenanceTag.ESTIMATE,
                    confidence = ConfidenceLevel.MODERATE,
                    priorityRank = 2,
                    createdAtEpochMs = now
                )
            )
        }

        val studyMins7d = input.studySessions.filter { it.startEpochMs >= sevenDaysAgo }.sumOf { it.durationMinutes }
        if (studyMins7d >= input.dailyAcademicGoalMinutes * 4) {
            findings.add(
                StrengthWeaknessFinding(
                    findingUuid = UUID.randomUUID().toString(),
                    kind = FindingKind.STRENGTH,
                    category = FindingCategory.ACADEMIC,
                    title = "Strong Weekly Focus Cadence",
                    description = "Logged $studyMins7d study minutes over the past 7 days toward your ${input.dailyAcademicGoalMinutes}m daily academic target.",
                    evidenceSummary = "${input.studySessions.count { it.startEpochMs >= sevenDaysAgo }} study sessions logged.",
                    dateRangeLabel = "Last 7 days",
                    provenance = DataProvenanceTag.MEASURED,
                    confidence = ConfidenceLevel.HIGH,
                    priorityRank = 3,
                    createdAtEpochMs = now
                )
            )
        }

        // Discipline & tasks findings
        val overdueTasks = input.tasks.count { it.isOverdue(input.todayEpochDay) }
        if (overdueTasks >= 2) {
            findings.add(
                StrengthWeaknessFinding(
                    findingUuid = UUID.randomUUID().toString(),
                    kind = FindingKind.BOTTLENECK,
                    category = FindingCategory.DISCIPLINE,
                    title = "Overdue Personal Task Queue ($overdueTasks)",
                    description = "Multiple tasks passed their target day. Triage or reschedule them cleanly rather than overloading a single day.",
                    evidenceSummary = "$overdueTasks non-archived tasks with dueDate < today.",
                    dateRangeLabel = "Current backlog",
                    provenance = DataProvenanceTag.MEASURED,
                    confidence = ConfidenceLevel.HIGH,
                    priorityRank = 2,
                    createdAtEpochMs = now
                )
            )
        }

        if (input.profile.streakDays >= 5) {
            findings.add(
                StrengthWeaknessFinding(
                    findingUuid = UUID.randomUUID().toString(),
                    kind = FindingKind.STRENGTH,
                    category = FindingCategory.DISCIPLINE,
                    title = "Active Consistency Streak (${input.profile.streakDays}d)",
                    description = "Sustained multi-day cadence across scheduled activities without breaking streak eligibility.",
                    evidenceSummary = "Player streakDays=${input.profile.streakDays}.",
                    dateRangeLabel = "Current streak",
                    provenance = DataProvenanceTag.MEASURED,
                    confidence = ConfidenceLevel.HIGH,
                    priorityRank = 2,
                    createdAtEpochMs = now
                )
            )
        }

        val sortedAttrs = input.profile.attributes.asList().sortedByDescending { it.totalValue }
        val topAttr = sortedAttrs.firstOrNull()
        val lowAttr = sortedAttrs.lastOrNull()
        if (topAttr != null && lowAttr != null && topAttr.totalValue - lowAttr.totalValue >= 5) {
            findings.add(
                StrengthWeaknessFinding(
                    findingUuid = UUID.randomUUID().toString(),
                    kind = FindingKind.WEAKNESS,
                    category = FindingCategory.RPG,
                    title = "Attribute Spread Imbalance (${lowAttr.type.code} vs ${topAttr.type.code})",
                    description = "${lowAttr.type.code} (${lowAttr.totalValue}) lags behind leading ${topAttr.type.code} (${topAttr.totalValue}). Balanced rank progression benefits from multi-domain habits.",
                    evidenceSummary = "Derived from deterministic attribute ledger.",
                    dateRangeLabel = "Current profile",
                    provenance = DataProvenanceTag.MEASURED,
                    confidence = ConfidenceLevel.MODERATE,
                    priorityRank = 3,
                    createdAtEpochMs = now
                )
            )
        }

        return findings.sortedBy { it.priorityRank }
    }
}

/**
 * Transparent statistical consistency trend & goal-risk predictor.
 * Clearly marks outputs as PREDICTION, attaches confidence & limitations, and blocks overconfident forecasting.
 */
object PredictiveConsistencyEngine {

    private const val DAY_MS = 24 * 60 * 60_000L

    fun evaluate(input: IntelligenceTelemetryInput): List<ConsistencyPrediction> {
        val todayDay = input.todayEpochDay
        val assessmentsByDay = input.disciplineAssessments.associateBy { it.dateEpochDay }

        // Build 14-day daily activity score series (strictly from real records)
        val dailyScores = (13 downTo 0).map { offset ->
            val day = todayDay - offset
            val explicit = assessmentsByDay[day]?.disciplineScore
            if (explicit != null) {
                day to explicit
            } else {
                val hasHabit = input.habitCompletions.any { it.dateEpochDay == day && it.status == HabitDayStatus.COMPLETED }
                val hasRoutine = input.routineCompletions.any { it.dateEpochDay == day }
                val hasJog = input.joggingSessions.any {
                    Instant.ofEpochMilli(it.startEpochMs).atZone(input.zoneId).toLocalDate().toEpochDay() == day
                }
                val hasStudy = input.studySessions.any {
                    Instant.ofEpochMilli(it.startEpochMs).atZone(input.zoneId).toLocalDate().toEpochDay() == day
                }
                val count = listOf(hasHabit, hasRoutine, hasJog, hasStudy).count { it }
                day to (count * 25).coerceAtMost(100)
            }
        }

        val activeObservedDays = dailyScores.count { it.second > 0 }
        val insufficient = activeObservedDays < 3

        val prevWeekAvg = dailyScores.take(7).map { it.second }.average()
        val currWeekAvg = dailyScores.drop(7).map { it.second }.average()
        val currentConsistency = currWeekAvg.roundToInt().coerceIn(0, 100)
        val slopePerWeek = ((currWeekAvg - prevWeekAvg).toFloat()).let {
            if (insufficient) 0f else (it * 10f).roundToInt() / 10f
        }
        val predicted7d = if (insufficient) {
            currentConsistency
        } else {
            (currWeekAvg + slopePerWeek * 0.5).roundToInt().coerceIn(5, 98)
        }
        val predicted30d = if (insufficient) {
            currentConsistency
        } else {
            (currWeekAvg + slopePerWeek * 1.1).roundToInt().coerceIn(5, 96)
        }

        val atRiskGoals = input.goalsWithDetails
            .filter { it.goal.status == GoalStatus.ACTIVE }
            .filter { g ->
                val remainingDays = (g.goal.targetDateEpochDay - todayDay).coerceAtLeast(0L)
                g.isBehindSchedule || (remainingDays in 0..7 && g.computedProgressPercent < 70)
            }
            .map { "${it.goal.title} (${it.computedProgressPercent}% done, ${((it.goal.targetDateEpochDay - todayDay).coerceAtLeast(0))}d left)" }

        val confidence = when {
            activeObservedDays >= 10 -> ConfidenceLevel.HIGH
            activeObservedDays >= 5 -> ConfidenceLevel.MODERATE
            activeObservedDays >= 3 -> ConfidenceLevel.LOW
            else -> ConfidenceLevel.INSUFFICIENT_DATA
        }

        val riskLabel = when {
            insufficient -> "Insufficient History"
            atRiskGoals.isNotEmpty() || slopePerWeek <= -12f -> "Goal / Cadence Drift Risk"
            predicted7d >= 65 -> "Stable Trajectory"
            else -> "Needs Simplification"
        }

        val limitation = if (insufficient) {
            "Fewer than 3 active days observed in the last 14 days; linear trend projection paused to avoid overconfident extrapolation."
        } else {
            "Linear trend estimate from last 14 days of recorded sessions and discipline snapshots ($activeObservedDays active days). Future external events are unmodeled."
        }

        val series7d = dailyScores.drop(7).mapIndexed { idx, (_, score) ->
            TrendDataPoint(label = "D-${6 - idx}", valuePercent = score, isEstimate = false)
        } + TrendDataPoint(label = "Proj+7", valuePercent = predicted7d, isEstimate = true)

        val series30d = listOf(
            TrendDataPoint("Wk-2", prevWeekAvg.roundToInt().coerceIn(0, 100), isEstimate = false),
            TrendDataPoint("Wk-1", currentConsistency, isEstimate = false),
            TrendDataPoint("Proj+14", ((currentConsistency + predicted30d) / 2), isEstimate = true),
            TrendDataPoint("Proj+30", predicted30d, isEstimate = true)
        )

        val now = input.nowEpochMs
        return listOf(
            ConsistencyPrediction(
                predictionUuid = UUID.randomUUID().toString(),
                domainLabel = "Overall Cross-Domain Consistency",
                horizonDays = 7,
                currentConsistencyPercent = currentConsistency,
                predictedConsistencyPercent = predicted7d,
                slopePerWeek = slopePerWeek,
                confidence = confidence,
                insufficientData = insufficient,
                limitationNote = limitation,
                atRiskGoals = atRiskGoals,
                riskLevelLabel = riskLabel,
                trendSeries = series7d,
                provenance = DataProvenanceTag.PREDICTION,
                evaluatedAtEpochMs = now
            ),
            ConsistencyPrediction(
                predictionUuid = UUID.randomUUID().toString(),
                domainLabel = "30-Day Habit & Goal Sustainability",
                horizonDays = 30,
                currentConsistencyPercent = currentConsistency,
                predictedConsistencyPercent = predicted30d,
                slopePerWeek = slopePerWeek,
                confidence = if (confidence == ConfidenceLevel.HIGH) ConfidenceLevel.MODERATE else confidence,
                insufficientData = insufficient,
                limitationNote = limitation,
                atRiskGoals = atRiskGoals,
                riskLevelLabel = riskLabel,
                trendSeries = series30d,
                provenance = DataProvenanceTag.PREDICTION,
                evaluatedAtEpochMs = now
            )
        )
    }
}
