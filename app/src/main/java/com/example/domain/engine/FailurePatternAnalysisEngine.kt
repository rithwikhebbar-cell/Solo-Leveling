package com.example.domain.engine

import com.example.domain.model.CommitmentState
import com.example.domain.model.ConfirmedFailureRecord
import com.example.domain.model.DetectedFailurePattern
import com.example.domain.model.FailurePatternType
import com.example.domain.model.PatternSeverity
import com.example.domain.model.QuestCommitment
import com.example.domain.model.RecoveryState
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Deterministic engine analyzing quest commitments, failure records, and player context
 * to identify recurring structural friction patterns.
 *
 * Guarantees:
 * - Read-only analysis (strictly 0 mutations).
 * - Actionable, constructive, non-punitive recommendations.
 * - Explains the root cause of friction rather than penalizing difficulty.
 */
object FailurePatternAnalysisEngine {

    /**
     * Evaluates all historical commitments and failures to detect actionable friction patterns.
     */
    fun analyzePatterns(
        commitments: List<QuestCommitment>,
        failures: List<ConfirmedFailureRecord>,
        questCategoryMap: Map<Long, String> = emptyMap(),
        recentRecoveryState: RecoveryState? = null,
        zoneId: ZoneId = ZoneId.systemDefault(),
        nowEpochMs: Long = System.currentTimeMillis()
    ): List<DetectedFailurePattern> {
        val detected = mutableListOf<DetectedFailurePattern>()

        // 1. Detect Excessive Daily Commitments
        val commitmentsByCycle = commitments.groupBy { it.logicalCycleId }
        val failuresByCycle = failures.groupBy { it.logicalCycleId }

        var excessiveCommitmentCycles = 0
        var totalExcessiveFailures = 0
        for ((cycleId, cycleCommits) in commitmentsByCycle) {
            val cycleFails = failuresByCycle[cycleId]?.size ?: 0
            if (cycleCommits.size >= 5 && cycleFails >= 2) {
                excessiveCommitmentCycles++
                totalExcessiveFailures += cycleFails
            }
        }
        if (excessiveCommitmentCycles >= 1) {
            detected.add(
                DetectedFailurePattern(
                    patternType = FailurePatternType.EXCESSIVE_DAILY_COMMITMENTS,
                    severity = if (totalExcessiveFailures >= 4) PatternSeverity.HIGH else PatternSeverity.MEDIUM,
                    evidenceCount = excessiveCommitmentCycles,
                    title = "Overloaded Daily Quest Roster",
                    description = "Detected $excessiveCommitmentCycles daily cycles where 5+ quests were simultaneously accepted, resulting in $totalExcessiveFailures confirmed failures.",
                    actionableRecommendation = "Limit active commitments to a maximum of 3 core quests per cycle. Prioritize foundational consistency over sheer volume.",
                    detectedAtEpochMs = nowEpochMs
                )
            )
        }

        // 2. Detect Repeated Missed Deadlines at specific times of day
        val lateNightFailures = failures.count { f ->
            val hour = ZonedDateTime.ofInstant(Instant.ofEpochMilli(f.failedAtEpochMs), zoneId).hour
            hour >= 22 || hour < 4
        }
        if (lateNightFailures >= 3) {
            detected.add(
                DetectedFailurePattern(
                    patternType = FailurePatternType.REPEATED_MISSED_DEADLINES,
                    severity = if (lateNightFailures >= 5) PatternSeverity.HIGH else PatternSeverity.MEDIUM,
                    evidenceCount = lateNightFailures,
                    title = "Late-Night Deadline Expiration Clustering",
                    description = "Detected $lateNightFailures quest failures expiring between 22:00 and 04:00, indicating late-day exhaustion and procrastination friction.",
                    actionableRecommendation = "Shift target quest completion deadlines to earlier in the day (e.g. 19:00). Allow the grace period to absorb unexpected delays without risking sleep loss.",
                    detectedAtEpochMs = nowEpochMs
                )
            )
        }

        // 3. Detect Repeated Rescheduling Preceding Failure
        val rescheduledThenFailed = commitments.count { c ->
            c.rescheduleCount >= 2 && c.state == CommitmentState.FAILED
        }
        val highRescheduleCount = commitments.count { it.rescheduleCount >= 2 }
        if (rescheduledThenFailed >= 2 || highRescheduleCount >= 3) {
            val count = maxOf(rescheduledThenFailed, highRescheduleCount)
            detected.add(
                DetectedFailurePattern(
                    patternType = FailurePatternType.REPEATED_RESCHEDULING,
                    severity = PatternSeverity.MEDIUM,
                    evidenceCount = count,
                    title = "Chronic Deadline Rescheduling",
                    description = "Detected $count commitments that underwent multiple reschedules before final resolution, signaling inaccurate task duration estimation.",
                    actionableRecommendation = "Break complex objectives into smaller, bite-sized tasks. Commit to a realistic completion window instead of repeatedly extending deadlines.",
                    detectedAtEpochMs = nowEpochMs
                )
            )
        }

        // 4. Detect Academic / Recovery Conflicts
        val academicFailures = failures.count { f ->
            questCategoryMap[f.questId]?.equals("ACADEMIC", ignoreCase = true) == true
        }
        val physicalFailures = failures.count { f ->
            questCategoryMap[f.questId]?.equals("PHYSICAL", ignoreCase = true) == true
        }
        if (academicFailures >= 2 && physicalFailures >= 2) {
            detected.add(
                DetectedFailurePattern(
                    patternType = FailurePatternType.ACADEMIC_RECOVERY_CONFLICT,
                    severity = PatternSeverity.MEDIUM,
                    evidenceCount = academicFailures + physicalFailures,
                    title = "Academic & Physical Load Imbalance",
                    description = "Detected recurrent concurrent failures across both Academic ($academicFailures) and Physical ($physicalFailures) domains.",
                    actionableRecommendation = "Alternate intensity across days: pair heavy workout days with light revision, and save intensive study blocks for physical recovery days.",
                    detectedAtEpochMs = nowEpochMs
                )
            )
        }

        // 5. Detect High Fatigue Overexertion
        if (recentRecoveryState != null &&
            (recentRecoveryState == RecoveryState.REST_RECOMMENDED || recentRecoveryState == RecoveryState.EXHAUSTED)
        ) {
            val recentFailures = failures.count {
                nowEpochMs - it.failedAtEpochMs <= 48 * 3600 * 1000L
            }
            if (recentFailures >= 1) {
                detected.add(
                    DetectedFailurePattern(
                        patternType = FailurePatternType.HIGH_FATIGUE_OVEREXERTION,
                        severity = PatternSeverity.HIGH,
                        evidenceCount = recentFailures,
                        title = "Overexertion Under Elevated Fatigue",
                        description = "Systemic recovery assessment indicates elevated strain (${recentRecoveryState.name}), coinciding with $recentFailures recent failures.",
                        actionableRecommendation = "Submit a Recovery Requirement exemption for high-intensity physical commitments. Allow physiological restoration before resuming high-impact quests.",
                        detectedAtEpochMs = nowEpochMs
                    )
                )
            }
        }

        return detected.sortedByDescending { it.severity.ordinal }
    }
}
