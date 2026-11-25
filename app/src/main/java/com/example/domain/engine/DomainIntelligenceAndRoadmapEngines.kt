package com.example.domain.engine

import com.example.domain.model.AcademicLearningIntelligence
import com.example.domain.model.AdaptiveDungeonRecommendation
import com.example.domain.model.AdvisorConsultationResult
import com.example.domain.model.AiConnectionStatus
import com.example.domain.model.ConfidenceLevel
import com.example.domain.model.DataProvenanceTag
import com.example.domain.model.DevelopmentRoadmap
import com.example.domain.model.DungeonCategory
import com.example.domain.model.DungeonDifficulty
import com.example.domain.model.DungeonStatus
import com.example.domain.model.GoalAnalysisInsight
import com.example.domain.model.GoalStatus
import com.example.domain.model.HabitDayStatus
import com.example.domain.model.IntelligenceReport
import com.example.domain.model.PersonalTaskStatus
import com.example.domain.model.PhysicalDevelopmentIntelligence
import com.example.domain.model.PlayerIntelligencePreferences
import com.example.domain.model.PlayerIntelligenceProfile
import com.example.domain.model.RecoveryState
import com.example.domain.model.ReportPeriodType
import com.example.domain.model.RoadmapHorizon
import com.example.domain.model.RoadmapMilestone
import com.example.domain.model.SubjectMasteryInsight
import com.example.domain.model.WeeklyComparisonDelta
import com.example.domain.model.WorkloadAssessment
import com.example.domain.model.WorkloadLevel
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.math.roundToInt

/**
 * Recommends Dungeon Gates matched to rank/level/attribute eligibility, completion history,
 * workload balance, and physical recovery safeguards.
 */
object AdaptiveDungeonDifficultyEngine {

    fun recommend(
        input: IntelligenceTelemetryInput,
        workload: WorkloadAssessment
    ): List<AdaptiveDungeonRecommendation> {
        val totalAttempts = input.dungeonInstances.count {
            it.status == DungeonStatus.COMPLETED || it.status == DungeonStatus.ABANDONED
        }
        val cleared = input.dungeonInstances.count { it.status == DungeonStatus.COMPLETED }
        val completionHistoryRate = if (totalAttempts == 0) null else ((cleared * 100) / totalAttempts).coerceIn(0, 100)
        val recoveryRestricted = input.recovery.state == RecoveryState.REST_RECOMMENDED ||
            input.recovery.state == RecoveryState.RECOVERY_RECOMMENDED ||
            input.recovery.selfReportedDiscomfort

        return input.dungeonDefinitions
            .filter { it.isActiveCatalog }
            .map { def ->
                val (eligible, blockers) = DungeonEngine.checkEligibility(def, input.profile)
                val isPhysicalGate = def.category == DungeonCategory.PHYSICAL
                val isRecoveryGate = def.category == DungeonCategory.RECOVERY_WELLBEING
                val recoverySafe = !(recoveryRestricted && isPhysicalGate)
                val workloadSafe = !(workload.workloadLevel == WorkloadLevel.OVERLOADED && def.difficulty.rankWeight >= 3)

                var score = 50
                if (eligible) score += 20 else score -= 45
                if (recoverySafe) score += 15 else score -= 50
                if (workloadSafe) score += 10 else score -= 18
                if (recoveryRestricted && isRecoveryGate) score += 25
                if (!recoveryRestricted && def.difficulty.minRank == input.profile.rank) score += 12
                if (completionHistoryRate != null) {
                    if (completionHistoryRate < 40 && def.difficulty.rankWeight >= 3) score -= 15
                    if (completionHistoryRate >= 80 && def.difficulty.minRank == input.profile.rank) score += 10
                }
                if (input.profile.streakDays >= 5 && def.category == DungeonCategory.CONSISTENCY) score += 8

                val label = when {
                    !eligible -> "Locked (Rank/Stat Requirement)"
                    !recoverySafe -> "Blocked by Recovery Guardrail"
                    isRecoveryGate && recoveryRestricted -> "Recommended (Recovery Safe)"
                    !workloadSafe -> "Heavy for Current Workload"
                    score >= 85 -> "Optimal Match"
                    else -> "Eligible Match"
                }

                val rationale = buildString {
                    if (!eligible) {
                        append(blockers.joinToString("; "))
                    } else if (!recoverySafe) {
                        append("Recovery state (${input.recovery.state.code}) advises avoiding strenuous physical gates.")
                    } else {
                        append("Aligned with ${input.profile.rank.displayName} eligibility, ${workload.workloadLevel.label} workload, and ${input.profile.streakDays}d consistency.")
                    }
                }

                AdaptiveDungeonRecommendation(
                    dungeonId = def.id,
                    code = def.code,
                    title = def.title,
                    difficulty = def.difficulty,
                    category = def.category,
                    isEligible = eligible,
                    eligibilityBlockers = blockers,
                    suitabilityScore = score.coerceIn(0, 100),
                    recommendationLabel = label,
                    rationale = rationale,
                    recoverySafe = recoverySafe,
                    workloadSafe = workloadSafe,
                    completionRateHistoryPercent = completionHistoryRate
                )
            }
            .sortedByDescending { it.suitabilityScore }
    }
}

/**
 * Academic intelligence separating measured study/assessment telemetry from self-reported confidence
 * and heuristic familiarity estimates.
 */
object AcademicLearningIntelligenceEngine {

    private const val DAY_MS = 24 * 60 * 60_000L

    fun analyze(input: IntelligenceTelemetryInput): AcademicLearningIntelligence {
        val now = input.nowEpochMs
        val sevenDaysAgo = now - 7 * DAY_MS
        val activeSubjects = input.subjects.filter { !it.isArchived }
        val study7d = input.studySessions.filter { it.startEpochMs >= sevenDaysAgo }
        val totalStudy7d = study7d.sumOf { it.durationMinutes }
        val verifiedMins7d = study7d.filter { it.verificationStatus.isVerified }.sumOf { it.durationMinutes }
        val verifiedRatio = if (totalStudy7d == 0) 0 else ((verifiedMins7d * 100) / totalStudy7d).coerceIn(0, 100)
        val revisions7d = input.revisionRecords.count { it.revisedAtEpochMs >= sevenDaysAgo }

        val subjectNames = activeSubjects.associate { it.id to it.name }
        val chapterDiffs = input.chapters.associate { it.id to it.difficulty }
        val assessRatioBySubject = input.assessments
            .groupBy { it.subjectId }
            .mapValues { (_, list) -> list.map { it.normalizedRatio }.average().toFloat() }

        val revisionQueue = RevisionEngine.buildQueue(
            topics = input.topics,
            subjectNamesById = subjectNames,
            difficultyByChapterId = chapterDiffs,
            assessmentRatioBySubjectId = assessRatioBySubject,
            nowEpochMs = now
        )
        val overdueCount = revisionQueue.count { it.urgency.name == "OVERDUE" }
        val avgRetentionEst = if (revisionQueue.isEmpty()) 0 else {
            revisionQueue.map { it.retentionEstimatePercent }.average().roundToInt()
        }
        val avgConfidence = input.topics.takeIf { it.isNotEmpty() }
            ?.map { it.confidenceEstimate }?.average()?.toFloat()
        val avgAssessmentPct = input.assessments.takeIf { it.isNotEmpty() }
            ?.map { (it.normalizedRatio * 100f) }?.average()?.roundToInt()

        val subjectInsights = activeSubjects.map { subj ->
            val mins = input.studySessions.filter { it.subjectId == subj.id }.sumOf { it.durationMinutes }
            val subjAssess = input.assessments.filter { it.subjectId == subj.id }
            val subjTopics = input.topics.filter { it.subjectId == subj.id }
            val subjQueue = revisionQueue.filter { it.topic.subjectId == subj.id }
            val subjOverdue = subjQueue.count { it.urgency.name == "OVERDUE" }
            val subjDueToday = subjQueue.count { it.urgency.name == "DUE_TODAY" }
            val assessPct = subjAssess.takeIf { it.isNotEmpty() }?.map { it.normalizedRatio * 100f }?.average()?.roundToInt()
            val confAvg = subjTopics.takeIf { it.isNotEmpty() }?.map { it.confidenceEstimate }?.average()?.toFloat()
            val estFam = if (subjQueue.isEmpty()) 0 else subjQueue.map { it.retentionEstimatePercent }.average().roundToInt()

            val gap = when {
                subjOverdue > 0 -> "$subjOverdue overdue revision(s); prioritize active recall"
                assessPct != null && assessPct < 65 -> "Assessment average ${assessPct}% indicates targeted practice need"
                mins < 60 -> "Low measured focus volume (${mins}m logged)"
                else -> "Steady progress across topics"
            }
            SubjectMasteryInsight(
                subjectId = subj.id,
                subjectName = subj.name,
                measuredStudyMinutes = mins,
                assessmentAveragePercent = assessPct,
                selfReportedConfidenceAvg = confAvg,
                estimatedFamiliarityPercent = estFam,
                overdueRevisionsCount = subjOverdue,
                dueTodayRevisionsCount = subjDueToday,
                gapSummary = gap
            )
        }

        val limitations = buildList {
            if (activeSubjects.isEmpty()) add("No active academic subjects created yet.")
            if (input.assessments.isEmpty()) add("No measured assessment results recorded; relying on study time and self-reported confidence.")
            add("Familiarity percentages are heuristic spaced-repetition estimates, not directly measured memory retention.")
        }

        val examCountdowns = input.exams.filter { !it.isCompleted }.take(5).map { ex ->
            val days = ExamCountdownCalculator.daysRemaining(ex.examDateEpochMs, now)
            "${ex.subjectName}: ${ex.title} (${days}d, ${ex.syllabusCoveragePercent}% covered)"
        }

        val priorities = revisionQueue.take(5).map {
            "${it.subjectName} — ${it.topic.title} (${it.urgency.label}, ~${it.retentionEstimatePercent}% est. familiarity)"
        }

        return AcademicLearningIntelligence(
            generatedAtEpochMs = now,
            totalStudyMinutes7d = totalStudy7d,
            totalRevisions7d = revisions7d,
            overdueRevisionsCount = overdueCount,
            verifiedStudyRatioPercent = verifiedRatio,
            measuredAssessmentAvgPercent = avgAssessmentPct,
            selfReportedConfidenceAvg = avgConfidence,
            estimatedRetentionAvgPercent = avgRetentionEst,
            examCountdowns = examCountdowns,
            subjectInsights = subjectInsights,
            recommendedRevisionPriorities = priorities,
            limitations = limitations
        )
    }
}

/**
 * Physical development intelligence enforcing non-medical boundaries and recovery protection.
 */
object PhysicalDevelopmentIntelligenceEngine {

    private const val DAY_MS = 24 * 60 * 60_000L

    fun analyze(input: IntelligenceTelemetryInput): PhysicalDevelopmentIntelligence {
        val now = input.nowEpochMs
        val sevenDaysAgo = now - 7 * DAY_MS
        val prevSevenDaysAgo = now - 14 * DAY_MS

        val jog7d = input.joggingSessions.filter { it.startEpochMs >= sevenDaysAgo }
        val work7d = input.workoutSessions.filter { it.startEpochMs >= sevenDaysAgo }
        val jogPrev = input.joggingSessions.filter { it.startEpochMs in prevSevenDaysAgo until sevenDaysAgo }
        val workPrev = input.workoutSessions.filter { it.startEpochMs in prevSevenDaysAgo until sevenDaysAgo }

        val jogMins = jog7d.sumOf { it.durationMinutes }
        val total7dMins = jogMins + work7d.sumOf { it.durationMinutes }
        val prev7dMins = jogPrev.sumOf { it.durationMinutes } + workPrev.sumOf { it.durationMinutes }
        val distanceKm = jog7d.sumOf { it.distanceMeters ?: 0.0 } / 1000.0
        val hrValues = (jog7d.mapNotNull { it.avgHeartRateBpm } + work7d.mapNotNull { it.avgHeartRateBpm })
        val avgHr = if (hrValues.isEmpty()) null else hrValues.average().roundToInt()

        val restEnforced = input.recovery.state == RecoveryState.REST_RECOMMENDED ||
            input.recovery.state == RecoveryState.RECOVERY_RECOMMENDED ||
            input.recovery.selfReportedDiscomfort

        val trendLabel = when {
            total7dMins == 0 && prev7dMins == 0 -> "No Training Volume Logged"
            total7dMins > prev7dMins + 20 -> "Volume Increasing (+${total7dMins - prev7dMins}m vs prior 7d)"
            total7dMins + 20 < prev7dMins -> "Volume Tapering (-${prev7dMins - total7dMins}m vs prior 7d)"
            else -> "Steady Weekly Volume"
        }

        val intensitySuggestion = when {
            input.recovery.selfReportedDiscomfort ->
                "Discomfort flagged: stop high-strain movement, rest without penalty, and seek qualified care if symptoms persist."
            input.recovery.state == RecoveryState.REST_RECOMMENDED ->
                "REST_RECOMMENDED: Prioritize sleep, hydration, and rest today. High-intensity workouts are locked out of proposals."
            input.recovery.state == RecoveryState.RECOVERY_RECOMMENDED ->
                "RECOVERY_RECOMMENDED: Limit movement to 15–20m gentle mobility or easy walking."
            input.recovery.state == RecoveryState.MODERATE ->
                "MODERATE readiness: Keep aerobic runs conversational and avoid maximal lifts."
            else ->
                "READY: Cleared for progressive jogging or structured strength within your target duration."
        }

        val limitations = buildList {
            if (avgHr == null) add("No heart rate telemetry attached to recent sessions.")
            if (input.recovery.measuredSleepHoursLastNight == null) add("Sleep duration unavailable (connect Health Connect Sleep permission for sleep-aware readiness).")
            if (jog7d.isEmpty() && work7d.isEmpty()) add("No physical sessions recorded in the last 7 days.")
        }

        return PhysicalDevelopmentIntelligence(
            generatedAtEpochMs = now,
            measuredLoadMinutes48h = input.recovery.measuredLoadMinutes48h,
            measuredLoadMinutes7d = input.recovery.measuredLoadMinutes7d,
            weeklyJoggingMinutes = jogMins,
            weeklyDistanceKm = (distanceKm * 10.0).roundToInt() / 10.0,
            weeklyWorkoutCount = work7d.size,
            avgHeartRateBpm = avgHr,
            measuredSleepHoursLastNight = input.recovery.measuredSleepHoursLastNight,
            recoveryState = input.recovery.state,
            readinessScore = input.recovery.readinessScore,
            trainingTrendLabel = trendLabel,
            intensitySuggestion = intensitySuggestion,
            restEnforcementActive = restEnforced,
            dataLimitations = limitations
        )
    }
}

/**
 * Generates editable, approval-gated multi-horizon personal development roadmaps (7d to 1y).
 */
object LongTermDevelopmentPlanner {

    fun generateRoadmap(
        horizon: RoadmapHorizon,
        focusDomains: List<String>,
        input: IntelligenceTelemetryInput,
        profile: PlayerIntelligenceProfile
    ): DevelopmentRoadmap {
        val roadmapUuid = UUID.randomUUID().toString()
        val domains = focusDomains.ifEmpty { listOf("Physical", "Academic", "Discipline", "RPG") }
        val today = input.todayEpochDay
        val nextRank = ProgressionEngine.evaluateNextRankProgress(input.profile).nextRank

        val milestoneSpecs = mutableListOf<Triple<String, String, Int>>()
        when (horizon) {
            RoadmapHorizon.SEVEN_DAYS -> {
                if ("Physical" in domains) milestoneSpecs.add(Triple("Complete 3 recovery-safe conditioning sessions", "Physical", 4))
                if ("Academic" in domains) milestoneSpecs.add(Triple("Log ${input.dailyAcademicGoalMinutes * 5}m focused study & clear overdue revisions", "Academic", 5))
                if ("Discipline" in domains) milestoneSpecs.add(Triple("Maintain 80%+ scheduled routine & habit adherence", "Discipline", 6))
                if ("RPG" in domains) milestoneSpecs.add(Triple("Clear 1 eligibility-matched Dungeon Gate cleanly", "RPG", 7))
            }
            RoadmapHorizon.THIRTY_DAYS -> {
                if ("Physical" in domains) milestoneSpecs.add(Triple("Build 12+ verified aerobic or strength sessions with rest-day protection", "Physical", 14))
                if ("Academic" in domains) milestoneSpecs.add(Triple("Complete 2 syllabus chapters & stabilize lowest-confidence topics", "Academic", 21))
                if ("Discipline" in domains) milestoneSpecs.add(Triple("Reach 14-day structured routine & habit consistency", "Discipline", 25))
                if ("RPG" in domains) milestoneSpecs.add(Triple("Unlock 2 specialization skills through verified logs", "RPG", 30))
            }
            RoadmapHorizon.NINETY_DAYS -> {
                milestoneSpecs.add(Triple("Establish reliable weekly aerobic + strength rhythm", "Physical", 30))
                milestoneSpecs.add(Triple("Complete full subject module & mock assessment cycle", "Academic", 60))
                milestoneSpecs.add(Triple("Close 2 life goals via sequential weekly milestones", "Discipline", 75))
                milestoneSpecs.add(Triple("Advance toward ${nextRank?.displayName ?: "Apex Rank"} promotion gates", "RPG", 90))
            }
            RoadmapHorizon.SIX_MONTHS, RoadmapHorizon.ONE_YEAR -> {
                val quarter = (horizon.days / 4).coerceAtLeast(15)
                milestoneSpecs.add(Triple("Quarter 1: Lock foundational verified habits & study cadence", "Discipline", quarter))
                milestoneSpecs.add(Triple("Quarter 2: Raise weakest attribute & expand endurance baseline", "Physical", quarter * 2))
                milestoneSpecs.add(Triple("Quarter 3: Master core academic curriculum & timed assessments", "Academic", quarter * 3))
                milestoneSpecs.add(Triple("Final Arc: Sustain multi-domain balance & Boss Campaigns", "RPG", horizon.days))
            }
        }

        val milestones = milestoneSpecs.mapIndexed { idx, (title, domain, dayOffset) ->
            RoadmapMilestone(
                milestoneUuid = UUID.randomUUID().toString(),
                roadmapUuid = roadmapUuid,
                title = title,
                domain = domain,
                targetDayOffset = dayOffset,
                targetDateEpochDay = today + dayOffset,
                orderIndex = idx
            )
        }

        return DevelopmentRoadmap(
            roadmapUuid = roadmapUuid,
            title = "${horizon.label} Mastery Roadmap",
            horizon = horizon,
            visionSummary = "Balances ${domains.joinToString(", ")} around peak window (${profile.behavioralPeakWindow}) and verified progression rules.",
            focusDomains = domains,
            dependenciesSummary = "Verified physical & timer-verified academic sessions unlock rank gates; rest days remain penalty-free.",
            riskNotes = if (input.recovery.state == RecoveryState.REST_RECOMMENDED) {
                "Immediate recovery priority active; delay intense physical milestones until readiness improves."
            } else {
                "Avoid scheduling stacked milestones without weekly review checkpoints."
            },
            nextReviewDateEpochDay = today + minOf(7, horizon.days),
            isApproved = false,
            createdAtEpochMs = input.nowEpochMs,
            updatedAtEpochMs = input.nowEpochMs,
            milestones = milestones
        )
    }
}

/**
 * Generates Daily, Weekly & Monthly intelligence reports separating deterministic real-world statistics
 * from optional AI narrative synthesis. Guards partial/incomplete date windows transparently.
 */
object IntelligenceReportEngine {

    private const val DAY_MS = 24 * 60 * 60_000L

    fun generateReport(
        periodType: ReportPeriodType,
        input: IntelligenceTelemetryInput,
        aiNarrativeSummary: String? = null,
        aiUsedFallback: Boolean = true
    ): IntelligenceReport {
        val windowDays = when (periodType) {
            ReportPeriodType.DAILY -> 1
            ReportPeriodType.WEEKLY -> 7
            ReportPeriodType.MONTHLY -> 30
        }
        val minDaysForFull = when (periodType) {
            ReportPeriodType.DAILY -> 1
            ReportPeriodType.WEEKLY -> 4
            ReportPeriodType.MONTHLY -> 10
        }
        val endDay = input.todayEpochDay
        val startDay = endDay - (windowDays - 1L)
        val startMs = if (periodType == ReportPeriodType.DAILY) {
            LocalDate.ofEpochDay(endDay).atStartOfDay(input.zoneId).toInstant().toEpochMilli()
        } else {
            input.nowEpochMs - (windowDays * DAY_MS)
        }

        val jogs = input.joggingSessions.filter { it.startEpochMs >= startMs }
        val workouts = input.workoutSessions.filter { it.startEpochMs >= startMs }
        val study = input.studySessions.filter { it.startEpochMs >= startMs }
        val revisions = input.revisionRecords.filter { it.revisedAtEpochMs >= startMs }
        val habitDone = input.habitCompletions.count { it.dateEpochDay in startDay..endDay && it.status == HabitDayStatus.COMPLETED }
        val tasksDone = input.tasks.count {
            it.status == PersonalTaskStatus.COMPLETED && (it.completedAtEpochMs ?: 0L) >= startMs
        }
        val questsDone = input.generalQuests.count {
            it.isCompleted && (it.completedAtEpochMs ?: 0L) >= startMs
        }
        val dungeonsCleared = input.dungeonCompletions.count { it.completedAtEpochMs >= startMs }

        val jogMinutes = jogs.sumOf { it.durationMinutes }
        val studyMinutes = study.sumOf { it.durationMinutes }
        val xpWindow = jogs.sumOf { it.xpAwarded } +
            workouts.sumOf { it.xpAwarded } +
            study.sumOf { it.xpAwarded } +
            revisions.sumOf { it.xpAwarded } +
            input.dungeonCompletions.filter { it.completedAtEpochMs >= startMs }.sumOf { it.xpAwarded }

        val activeDays = buildSet {
            jogs.forEach { add(Instant.ofEpochMilli(it.startEpochMs).atZone(input.zoneId).toLocalDate().toEpochDay()) }
            workouts.forEach { add(Instant.ofEpochMilli(it.startEpochMs).atZone(input.zoneId).toLocalDate().toEpochDay()) }
            study.forEach { add(Instant.ofEpochMilli(it.startEpochMs).atZone(input.zoneId).toLocalDate().toEpochDay()) }
            input.habitCompletions.filter { it.dateEpochDay in startDay..endDay && it.status == HabitDayStatus.COMPLETED }.forEach { add(it.dateEpochDay) }
            input.tasks.filter { it.status == PersonalTaskStatus.COMPLETED && (it.completedAtEpochMs ?: 0L) >= startMs }
                .forEach { task ->
                    task.completedAtEpochMs?.let { add(Instant.ofEpochMilli(it).atZone(input.zoneId).toLocalDate().toEpochDay()) }
                }
        }

        val verifiedSessions = jogs.count { it.verificationStatus.isVerified } +
            workouts.count { it.verificationStatus.isVerified } +
            study.count { it.verificationStatus.isVerified }
        val totalSessions = jogs.size + workouts.size + study.size
        val selfReportedSessions = (totalSessions - verifiedSessions).coerceAtLeast(0)

        val isPartial = activeDays.size < minDaysForFull
        val highlights = buildList {
            if (jogMinutes > 0 || workouts.isNotEmpty()) {
                add("Physical output: ${workouts.size} workout(s), ${jogMinutes}m jogging.")
            }
            if (studyMinutes > 0 || revisions.isNotEmpty()) {
                add("Academic output: ${studyMinutes}m focus study, ${revisions.size} revision review(s).")
            }
            if (habitDone > 0 || tasksDone > 0 || questsDone > 0) {
                add("Mission & habit execution: $questsDone quest(s), $tasksDone task(s), $habitDone habit check-ins.")
            }
            if (dungeonsCleared > 0) {
                add("RPG milestones: $dungeonsCleared Dungeon Gate(s) cleared.")
            }
            val activeGoals = input.goalsWithDetails.filter { it.goal.status == GoalStatus.ACTIVE }
            if (activeGoals.isNotEmpty()) {
                val avgGoalPct = activeGoals.map { it.computedProgressPercent }.average().toInt()
                add("Active long-term goals: ${activeGoals.size} tracked (avg ${avgGoalPct}% milestone completion).")
            }
            if (isEmpty()) {
                add("No completed sessions recorded in this ${windowDays}-day window yet.")
            }
        }

        val risks = buildList {
            if (isPartial) {
                add("Partial data window: only ${activeDays.size} active day(s) recorded out of $windowDays day(s).")
            }
            if (input.recovery.state == RecoveryState.REST_RECOMMENDED || input.recovery.state == RecoveryState.RECOVERY_RECOMMENDED) {
                add("Current readiness (${input.recovery.readinessScore}/100) requires rest priority before resuming high-strain workouts.")
            }
            val overdueTasks = input.tasks.count { it.isOverdue(endDay) }
            if (overdueTasks > 0) {
                add("$overdueTasks task/mission item(s) remain overdue; triage in Mission Control.")
            }
            val behindGoals = input.goalsWithDetails.count { it.isBehindSchedule && it.goal.status == GoalStatus.ACTIVE }
            if (behindGoals > 0) {
                add("$behindGoals active goal(s) are trailing expected milestone pace.")
            }
        }

        val periodLabel = if (periodType == ReportPeriodType.DAILY) {
            LocalDate.ofEpochDay(endDay).toString()
        } else {
            "${LocalDate.ofEpochDay(startDay)} → ${LocalDate.ofEpochDay(endDay)}"
        }

        return IntelligenceReport(
            reportUuid = UUID.randomUUID().toString(),
            periodType = periodType,
            periodLabel = periodLabel,
            periodStartEpochDay = startDay,
            periodEndEpochDay = endDay,
            activeDaysObserved = activeDays.size,
            isPartialDataWindow = isPartial,
            measuredStatsSummary = "Measured/Timer Verified Sessions: $verifiedSessions | Active Days Observed: ${activeDays.size}/$windowDays",
            selfReportedStatsSummary = "Self-Reported Logs: $selfReportedSessions | Habits: $habitDone | Tasks & Quests: ${tasksDone + questsDone}",
            workoutsCount = workouts.size,
            joggingMinutes = jogMinutes,
            studyMinutes = studyMinutes,
            revisionsCount = revisions.size,
            habitsCompletedCount = habitDone,
            tasksCompletedCount = tasksDone + questsDone,
            dungeonsClearedCount = dungeonsCleared,
            xpGainedWindow = xpWindow,
            deterministicHighlights = highlights,
            deterministicRisks = risks,
            aiNarrativeSummary = aiNarrativeSummary,
            aiUsedFallback = aiUsedFallback,
            generatedAtEpochMs = input.nowEpochMs
        )
    }

    fun computeWeeklyComparison(input: IntelligenceTelemetryInput): WeeklyComparisonDelta {
        val currentEndMs = input.nowEpochMs
        val currentStartMs = currentEndMs - (7 * DAY_MS)
        val prevStartMs = currentEndMs - (14 * DAY_MS)

        val currWorkouts = input.workoutSessions.count { it.startEpochMs in currentStartMs..currentEndMs }
        val prevWorkouts = input.workoutSessions.count { it.startEpochMs in prevStartMs until currentStartMs }

        val currJogs = input.joggingSessions.filter { it.startEpochMs in currentStartMs..currentEndMs }
        val prevJogs = input.joggingSessions.filter { it.startEpochMs in prevStartMs until currentStartMs }

        val currStudy = input.studySessions.filter { it.startEpochMs in currentStartMs..currentEndMs }
        val prevStudy = input.studySessions.filter { it.startEpochMs in prevStartMs until currentStartMs }

        val currTasks = input.tasks.count { it.status == PersonalTaskStatus.COMPLETED && (it.completedAtEpochMs ?: 0L) in currentStartMs..currentEndMs }
        val prevTasks = input.tasks.count { it.status == PersonalTaskStatus.COMPLETED && (it.completedAtEpochMs ?: 0L) in prevStartMs until currentStartMs }

        val prevRevs = input.revisionRecords.filter { it.revisedAtEpochMs in prevStartMs until currentStartMs }
        val prevDungeons = input.dungeonCompletions.filter { it.completedAtEpochMs in prevStartMs until currentStartMs }
        val prevHabits = input.habitCompletions.count {
            it.dateEpochDay in (input.todayEpochDay - 13L)..(input.todayEpochDay - 7L) && it.status == HabitDayStatus.COMPLETED
        }

        val hasPreviousBaseline = prevWorkouts > 0 || prevJogs.isNotEmpty() || prevStudy.isNotEmpty() ||
            prevTasks > 0 || prevRevs.isNotEmpty() || prevDungeons.isNotEmpty() || prevHabits > 0

        val currentLabel = "${LocalDate.ofEpochDay(input.todayEpochDay - 6L)} → ${LocalDate.ofEpochDay(input.todayEpochDay)}"
        val previousLabel = "${LocalDate.ofEpochDay(input.todayEpochDay - 13L)} → ${LocalDate.ofEpochDay(input.todayEpochDay - 7L)}"

        if (!hasPreviousBaseline) {
            return WeeklyComparisonDelta(
                hasPreviousBaseline = false,
                currentWindowLabel = currentLabel,
                previousWindowLabel = previousLabel,
                summaryNote = "No recorded sessions in the prior 7-day window ($previousLabel). Continue logging real activities to unlock week-over-week comparisons."
            )
        }

        val currXp = currJogs.sumOf { it.xpAwarded } +
            input.workoutSessions.filter { it.startEpochMs in currentStartMs..currentEndMs }.sumOf { it.xpAwarded } +
            currStudy.sumOf { it.xpAwarded } +
            input.revisionRecords.filter { it.revisedAtEpochMs in currentStartMs..currentEndMs }.sumOf { it.xpAwarded } +
            input.dungeonCompletions.filter { it.completedAtEpochMs in currentStartMs..currentEndMs }.sumOf { it.xpAwarded }

        val prevXp = prevJogs.sumOf { it.xpAwarded } +
            input.workoutSessions.filter { it.startEpochMs in prevStartMs until currentStartMs }.sumOf { it.xpAwarded } +
            prevStudy.sumOf { it.xpAwarded } +
            prevRevs.sumOf { it.xpAwarded } +
            prevDungeons.sumOf { it.xpAwarded }

        val studyDelta = currStudy.sumOf { it.durationMinutes } - prevStudy.sumOf { it.durationMinutes }
        val jogDelta = currJogs.sumOf { it.durationMinutes } - prevJogs.sumOf { it.durationMinutes }
        val workoutsDelta = currWorkouts - prevWorkouts
        val tasksDelta = currTasks - prevTasks
        val xpDelta = currXp - prevXp

        return WeeklyComparisonDelta(
            hasPreviousBaseline = true,
            currentWindowLabel = currentLabel,
            previousWindowLabel = previousLabel,
            workoutsDelta = workoutsDelta,
            joggingMinutesDelta = jogDelta,
            studyMinutesDelta = studyDelta,
            tasksCompletedDelta = tasksDelta,
            xpDelta = xpDelta,
            summaryNote = "Compared against $previousLabel recorded activity."
        )
    }

    fun computeGoalAnalysis(input: IntelligenceTelemetryInput): List<GoalAnalysisInsight> {
        return input.goalsWithDetails
            .filter { it.goal.status != GoalStatus.ARCHIVED }
            .map { details ->
                val daysRemaining = details.goal.targetDateEpochDay - input.todayEpochDay
                val pace = when {
                    details.goal.status == GoalStatus.COMPLETED -> "Completed"
                    details.goal.status == GoalStatus.PAUSED -> "Paused"
                    daysRemaining < 0 -> "Overdue by ${-daysRemaining}d"
                    details.isBehindSchedule -> "Behind schedule (${details.computedProgressPercent}%)"
                    else -> "On track (${details.computedProgressPercent}%)"
                }
                GoalAnalysisInsight(
                    goalId = details.goal.id,
                    title = details.goal.title,
                    progressPercent = details.computedProgressPercent,
                    statusLabel = details.goal.status.label,
                    daysRemaining = daysRemaining,
                    completedMilestones = details.completedMilestonesCount,
                    totalMilestones = details.totalMilestonesCount,
                    paceAssessment = pace
                )
            }
    }
}

/**
 * Advisory Q&A guardrail & offline deterministic synthesis layer.
 * Strips prompt injection, refuses progression tampering, honors recovery safety rules, and distinguishes data provenance.
 */
object PersonalDevelopmentAdvisor {

    private val progressionBypassPattern = Regex(
        "(?i)(grant\\s+\\d+\\s+xp|add\\s+xp|set\\s+level|set\\s+rank|unlock\\s+all|complete\\s+quest\\s+automatically|bypass\\s+recovery)"
    )

    fun sanitizeQuestion(rawQuestion: String): String =
        PromptSanitizer.sanitize(rawQuestion, maxChars = 400)

    fun buildDeterministicSummary(
        question: String,
        input: IntelligenceTelemetryInput,
        profile: PlayerIntelligenceProfile,
        workload: WorkloadAssessment
    ): String {
        val clean = sanitizeQuestion(question).lowercase()
        return buildString {
            if (progressionBypassPattern.containsMatchIn(question)) {
                appendLine("SYSTEM INTEGRITY GUARD: Intelligence Advisor is read-only and advisory. XP, attributes, Hunter Rank, skills, and quests advance strictly from verified or logged real-world records.")
                appendLine()
            }
            when {
                "workout" in clean || "jog" in clean || "rest" in clean || "recovery" in clean || "physical" in clean -> {
                    appendLine("PHYSICAL & RECOVERY DIAGNOSTIC (${input.recovery.state.code}, Readiness ${input.recovery.readinessScore}/100)")
                    appendLine("• Measured 48h Load: ${input.recovery.measuredLoadMinutes48h}m | 7d Load: ${input.recovery.measuredLoadMinutes7d}m")
                    appendLine("• Workload Classification: ${workload.workloadLevel.label}")
                    appendLine("• Directive: ${workload.adjustmentDirective}")
                }
                "study" in clean || "exam" in clean || "subject" in clean || "revision" in clean -> {
                    val acad = AcademicLearningIntelligenceEngine.analyze(input)
                    appendLine("ACADEMIC INTELLIGENCE SUMMARY (Last 7d: ${acad.totalStudyMinutes7d}m studied, ${acad.verifiedStudyRatioPercent}% verified)")
                    appendLine("• Overdue Revisions: ${acad.overdueRevisionsCount} | Estimated Avg Familiarity: ${acad.estimatedRetentionAvgPercent}% (heuristic estimate)")
                    appendLine("• Exam Countdowns: ${acad.examCountdowns.ifEmpty { listOf("None scheduled") }.joinToString("; ")}")
                }
                "plan" in clean || "schedule" in clean || "today" in clean || "tomorrow" in clean -> {
                    appendLine("ADAPTIVE PLANNING GUIDANCE (${workload.workloadLevel.label})")
                    appendLine("• Behavioral Peak Window: ${profile.behavioralPeakWindow}")
                    appendLine("• Open Commitments: ${workload.activeCommitmentsCount} (${workload.overdueItemsCount} overdue, ${workload.scheduleConflictCount} schedule conflicts)")
                    appendLine("• Directive: ${workload.adjustmentDirective}")
                }
                else -> {
                    appendLine("INTELLIGENCE DIAGNOSTIC — Lv.${profile.playerLevel} ${profile.hunterRank.displayName}")
                    appendLine("• Domain Scores (0–100): Physical=${profile.physicalActivityScore}, Academic=${profile.academicActivityScore}, Discipline=${profile.disciplineConsistencyScore}, RPG=${profile.rpgEngagementScore}")
                    appendLine("• Measured Verification Ratio: ${profile.measuredDataRatioPercent}% | Active Days (30d): ${profile.activeDaysLast30}")
                    appendLine("• Priority Guidance: ${workload.adjustmentDirective}")
                }
            }
            if (!profile.hasMinimumBaseline) {
                appendLine("• Data Limitation: Fewer than 3 active days observed; recommendations carry conservative confidence.")
            }
        }.trim()
    }

    /**
     * Validates external AI response to guarantee it never claims XP/level/rank mutations
     * or instructs the player to train through REST_RECOMMENDED / pain.
     */
    fun validateCloudAdvice(
        rawAiText: String,
        recoveryState: RecoveryState,
        selfReportedDiscomfort: Boolean
    ): String {
        val sanitized = rawAiText.trim()
        if (sanitized.isBlank()) return ""
        val warnings = mutableListOf<String>()
        if (progressionBypassPattern.containsMatchIn(sanitized)) {
            warnings.add("[System Note: Advisory text cannot grant XP, alter rank, or auto-complete quests.]")
        }
        val restRequired = recoveryState == RecoveryState.REST_RECOMMENDED || selfReportedDiscomfort
        if (restRequired && Regex("(?i)(push\\s+harder|ignore\\s+pain|train\\s+through|sprint|max\\s+weight)").containsMatchIn(sanitized)) {
            warnings.add("[Recovery Safety Override: REST_RECOMMENDED is active. Do not train through fatigue or discomfort.]")
        }
        return if (warnings.isEmpty()) sanitized else "${warnings.joinToString(" ")}\n\n$sanitized"
    }

    fun buildConsultationResult(
        question: String,
        input: IntelligenceTelemetryInput,
        profile: PlayerIntelligenceProfile,
        workload: WorkloadAssessment,
        cloudReply: String?,
        connectionStatus: AiConnectionStatus
    ): AdvisorConsultationResult {
        val detSummary = buildDeterministicSummary(question, input, profile, workload)
        val safeCloud = cloudReply?.let {
            validateCloudAdvice(it, input.recovery.state, input.recovery.selfReportedDiscomfort)
        }?.takeIf { it.isNotBlank() }

        val usedFallback = safeCloud == null
        val limitations = buildList {
            if (!profile.hasMinimumBaseline) add("Limited historical baseline (<3 active days).")
            if (input.recovery.measuredSleepHoursLastNight == null) add("Sleep telemetry not recorded.")
            if (usedFallback) add("Generated offline via deterministic intelligence engine.")
        }

        return AdvisorConsultationResult(
            consultationUuid = UUID.randomUUID().toString(),
            question = sanitizeQuestion(question),
            deterministicSummary = detSummary,
            aiResponse = safeCloud ?: detSummary,
            provenanceTags = listOf(DataProvenanceTag.MEASURED, DataProvenanceTag.ESTIMATE),
            confidence = if (profile.hasMinimumBaseline) ConfidenceLevel.MODERATE else ConfidenceLevel.LOW,
            connectionStatus = connectionStatus,
            usedFallback = usedFallback,
            limitationNotes = limitations,
            timestampEpochMs = input.nowEpochMs
        )
    }
}
