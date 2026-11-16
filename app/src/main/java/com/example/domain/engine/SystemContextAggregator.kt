package com.example.domain.engine

import com.example.domain.model.AiConsentState
import com.example.domain.model.AiContextScope
import com.example.domain.model.PlayerContextSnapshot
import com.example.domain.model.PlayerIntelligencePreferences
import com.example.domain.model.RecoveryAssessment
import com.example.domain.model.RecoveryState
import com.example.domain.model.ScheduledSourceType

data class ProposalSafetyCheckResult(
    val allowed: Boolean,
    val requiresUserConfirmation: Boolean = true,
    val rejectionReason: String? = null
)

/**
 * Unifies AI Game Master context assembly, consent/privacy minimization, and proposal safety checks
 * so AI recommendations never bypass recovery restrictions or deterministic ProgressionEngine rules.
 */
object SystemContextAggregator {

    fun filterTelemetryByConsent(
        input: IntelligenceTelemetryInput,
        consent: AiConsentState,
        intelPrefs: PlayerIntelligencePreferences
    ): IntelligenceTelemetryInput {
        val allowPhysical = consent.sharePhysicalWithAi && intelPrefs.sharePhysicalWithIntelligence
        val allowAcademic = consent.shareAcademicWithAi && intelPrefs.shareAcademicWithIntelligence
        val allowLife = consent.shareLifeWithAi && intelPrefs.shareLifeWithIntelligence
        val allowRpg = consent.shareRpgWithAi && intelPrefs.shareRpgWithIntelligence

        return input.copy(
            joggingSessions = if (allowPhysical) input.joggingSessions else emptyList(),
            workoutSessions = if (allowPhysical) input.workoutSessions else emptyList(),
            subjects = if (allowAcademic) input.subjects else emptyList(),
            chapters = if (allowAcademic) input.chapters else emptyList(),
            topics = if (allowAcademic) input.topics else emptyList(),
            studySessions = if (allowAcademic) input.studySessions else emptyList(),
            revisionRecords = if (allowAcademic) input.revisionRecords else emptyList(),
            assessments = if (allowAcademic) input.assessments else emptyList(),
            exams = if (allowAcademic) input.exams else emptyList(),
            habits = if (allowLife) input.habits else emptyList(),
            habitCompletions = if (allowLife) input.habitCompletions else emptyList(),
            routines = if (allowLife) input.routines else emptyList(),
            routineCompletions = if (allowLife) input.routineCompletions else emptyList(),
            tasks = if (allowLife) input.tasks else emptyList(),
            goalsWithDetails = if (allowLife) input.goalsWithDetails else emptyList(),
            disciplineAssessments = if (allowLife) input.disciplineAssessments else emptyList(),
            dungeonDefinitions = if (allowRpg) input.dungeonDefinitions else emptyList(),
            dungeonInstances = if (allowRpg) input.dungeonInstances else emptyList(),
            dungeonCompletions = if (allowRpg) input.dungeonCompletions else emptyList(),
            unlockedSkills = if (allowRpg) input.unlockedSkills else emptyList()
        )
    }

    /**
     * Enforces AI pipeline safety rules:
     * AI -> Structured Proposal -> Validation -> Safety Check -> Conflict Check -> User Confirmation -> Execution.
     */
    fun validateAiActionProposal(
        moduleTag: String,
        title: String,
        durationMinutes: Int,
        recovery: RecoveryAssessment,
        hasScheduleConflict: Boolean
    ): ProposalSafetyCheckResult {
        if (title.isBlank()) {
            return ProposalSafetyCheckResult(false, rejectionReason = "Proposal title cannot be blank.")
        }
        if (durationMinutes !in 5..ProgressionEngine.MAX_SESSION_MINUTES) {
            return ProposalSafetyCheckResult(false, rejectionReason = "Duration must be between 5 and ${ProgressionEngine.MAX_SESSION_MINUTES} minutes.")
        }
        val isRestActive = recovery.state == RecoveryState.REST_RECOMMENDED ||
            recovery.state == RecoveryState.RECOVERY_RECOMMENDED ||
            recovery.selfReportedDiscomfort
        if (isRestActive && (moduleTag.equals("PHYSICAL", ignoreCase = true) || moduleTag.equals(ScheduledSourceType.PHYSICAL_TRAINING.name, ignoreCase = true))) {
            return ProposalSafetyCheckResult(
                allowed = false,
                rejectionReason = "Recovery safety guard active (${recovery.state.code}): physical load proposals are blocked."
            )
        }
        if (hasScheduleConflict) {
            return ProposalSafetyCheckResult(
                allowed = false,
                rejectionReason = "Proposal collides with an existing approved timeline block."
            )
        }
        return ProposalSafetyCheckResult(allowed = true, requiresUserConfirmation = true)
    }

    fun inferEffectiveScope(prompt: String): AiContextScope = PlayerContextEngine.inferScope(prompt)

    fun summarizeSafetyState(snapshot: PlayerContextSnapshot): String {
        val p = snapshot.profileSummary
        val rest = snapshot.physicalSummary?.recoveryState?.code ?: "UNKNOWN"
        val cloudConsent = snapshot.preferencesSummary?.cloudAiConsentGranted ?: false
        return "Lv.${p.level} (${p.rankCode}) | Recovery=$rest | CloudConsent=$cloudConsent"
    }
}
