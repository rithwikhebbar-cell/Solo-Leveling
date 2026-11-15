package com.example.domain.engine

import com.example.domain.model.JoggingTargetProposal
import com.example.domain.model.RecoveryAssessment
import com.example.domain.model.RecoveryState

/**
 * Deterministic Recovery Intelligence Engine.
 * Clearly distinguishes measured telemetry (training volume, frequency, Health Connect sleep/HR)
 * from self-reported signals (fatigue 1-5, discomfort flag).
 * Never diagnoses medical conditions and never encourages training through pain or illness.
 */
object RecoveryEngine {

    fun assess(
        measuredLoadMinutes48h: Int,
        measuredLoadMinutes7d: Int,
        measuredSessions7d: Int,
        consecutiveActiveDays: Int,
        measuredSleepHoursLastNight: Double? = null,
        measuredRestingHrBpm: Int? = null,
        selfReportedFatigue: Int? = null,
        selfReportedDiscomfort: Boolean = false,
        discomfortNotes: String = ""
    ): RecoveryAssessment {
        var score = 100
        val actions = mutableListOf<String>()

        // 1. Measured physical load penalties
        when {
            measuredLoadMinutes48h >= 150 -> score -= 35
            measuredLoadMinutes48h >= 95 -> score -= 20
            measuredLoadMinutes48h >= 60 -> score -= 10
        }

        when {
            consecutiveActiveDays >= 6 -> score -= 25
            consecutiveActiveDays >= 4 -> score -= 12
        }

        if (measuredLoadMinutes7d >= 360) {
            score -= 15
        }

        // 2. Measured Health Connect recovery indicators (only applied when present)
        if (measuredSleepHoursLastNight != null) {
            when {
                measuredSleepHoursLastNight < 5.5 -> {
                    score -= 30
                    actions.add("Measured sleep was ${"%.1f".format(measuredSleepHoursLastNight)}h. Prioritize sleep restoration tonight.")
                }
                measuredSleepHoursLastNight < 6.8 -> {
                    score -= 15
                    actions.add("Sleep duration slightly low (${"%.1f".format(measuredSleepHoursLastNight)}h). Keep pace aerobic.")
                }
                else -> {
                    actions.add("Sleep recovery (${"%.1f".format(measuredSleepHoursLastNight)}h) supports normal training.")
                }
            }
        }

        if (measuredRestingHrBpm != null && measuredRestingHrBpm >= 90) {
            score -= 15
            actions.add("Elevated heart rate baseline (${measuredRestingHrBpm} bpm) noted; favor low-intensity pacing.")
        }

        // 3. Self-reported fatigue & discomfort (safety-first, non-medical)
        if (selfReportedFatigue != null) {
            when (selfReportedFatigue.coerceIn(1, 5)) {
                5 -> score -= 40
                4 -> score -= 25
                3 -> score -= 10
                else -> Unit
            }
        }

        if (selfReportedDiscomfort) {
            score = (score - 45).coerceAtMost(35)
        }

        val clampedScore = score.coerceIn(5, 100)

        val state = when {
            selfReportedDiscomfort || clampedScore < 40 -> RecoveryState.REST_RECOMMENDED
            clampedScore < 62 -> RecoveryState.RECOVERY_RECOMMENDED
            clampedScore < 82 -> RecoveryState.MODERATE
            else -> RecoveryState.READY
        }

        when (state) {
            RecoveryState.READY -> {
                actions.add("Complete your 30-minute aerobic jogging target or scheduled strength work.")
                actions.add("Keep 5 minutes of cooldown mobility after your session.")
            }
            RecoveryState.MODERATE -> {
                actions.add("Maintain moderate conversational pace and avoid maximal sprint efforts.")
                actions.add("Schedule hydration and 10 minutes of mobility post-session.")
            }
            RecoveryState.RECOVERY_RECOMMENDED -> {
                actions.add("Reduce jogging volume to 15–20 minutes easy pace or switch to Mobility & Walking.")
                actions.add("Do not push intensity while cumulative load is elevated.")
            }
            RecoveryState.REST_RECOMMENDED -> {
                if (selfReportedDiscomfort) {
                    actions.add("Discomfort reported — take a penalty-free rest day and avoid loading painful areas.")
                } else {
                    actions.add("Take a penalty-free rest day focused on hydration, sleep, and gentle walking only if comfortable.")
                }
                actions.add("If persistent pain or illness is present, consult a qualified healthcare professional.")
            }
        }

        val summary = when (state) {
            RecoveryState.READY ->
                "Readiness $clampedScore/100. Recent 48h load (${measuredLoadMinutes48h}m) supports full training."
            RecoveryState.MODERATE ->
                "Readiness $clampedScore/100. Moderate 48h load (${measuredLoadMinutes48h}m); maintain controlled effort."
            RecoveryState.RECOVERY_RECOMMENDED ->
                "Readiness $clampedScore/100. High recent load (${measuredLoadMinutes48h}m / ${consecutiveActiveDays}d streak); lighter volume advised."
            RecoveryState.REST_RECOMMENDED ->
                "Readiness $clampedScore/100. Rest is recommended today — rest days carry zero progression penalty."
        }

        return RecoveryAssessment(
            state = state,
            readinessScore = clampedScore,
            measuredLoadMinutes48h = measuredLoadMinutes48h,
            measuredLoadMinutes7d = measuredLoadMinutes7d,
            measuredSessions7d = measuredSessions7d,
            consecutiveActiveDays = consecutiveActiveDays,
            measuredSleepHoursLastNight = measuredSleepHoursLastNight,
            measuredRestingHrBpm = measuredRestingHrBpm,
            selfReportedFatigue = selfReportedFatigue,
            selfReportedDiscomfort = selfReportedDiscomfort,
            discomfortNotes = discomfortNotes.trim(),
            recommendedActions = actions.distinct(),
            summaryMessage = summary
        )
    }

    /**
     * Generates an advisory jogging target adjustment requiring explicit player approval.
     * Never forces targets above safe recovery limits.
     */
    fun proposeJoggingTargetAdjustment(
        currentTargetMinutes: Int,
        baseTargetMinutes: Int,
        recoveryState: RecoveryState,
        weeklyJoggingDays: Int
    ): JoggingTargetProposal? {
        val proposed = when (recoveryState) {
            RecoveryState.REST_RECOMMENDED -> 10
            RecoveryState.RECOVERY_RECOMMENDED -> (baseTargetMinutes * 0.65f).toInt().coerceIn(15, 22)
            RecoveryState.MODERATE -> baseTargetMinutes.coerceIn(20, 30)
            RecoveryState.READY -> if (weeklyJoggingDays >= 5) {
                (baseTargetMinutes + 5).coerceAtMost(45)
            } else {
                baseTargetMinutes.coerceAtLeast(30)
            }
        }

        if (proposed == currentTargetMinutes) return null

        val rationale = when (recoveryState) {
            RecoveryState.REST_RECOMMENDED ->
                "Recovery intelligence recommends resting today (optional 10-min gentle walk if feeling well). Approve to lower today's target."
            RecoveryState.RECOVERY_RECOMMENDED ->
                "Elevated load detected. Reducing today's target to $proposed min protects long-term consistency."
            RecoveryState.MODERATE ->
                "Returning target to $proposed min steady aerobic baseline while managing moderate load."
            RecoveryState.READY ->
                if (proposed > currentTargetMinutes) {
                    "Consistent week ($weeklyJoggingDays active days) with READY recovery. Optional progression to $proposed min."
                } else {
                    "Readiness is READY. Restore standard ${proposed}-min jogging protocol."
                }
        }

        return JoggingTargetProposal(
            proposedMinutes = proposed,
            currentMinutes = currentTargetMinutes,
            rationale = rationale,
            sourceLabel = "Recovery Intelligence Advisory"
        )
    }
}
