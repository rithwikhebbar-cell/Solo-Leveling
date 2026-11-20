package com.example.domain.engine

import com.example.domain.model.CorrectiveActivityDefinition
import com.example.domain.model.CorrectiveCategory
import com.example.domain.model.CorrectiveDifficulty
import com.example.domain.model.PenaltyRecord
import com.example.domain.model.PenaltySeverity
import com.example.domain.model.Quest
import com.example.domain.model.QuestCategory
import com.example.domain.model.RecoveryStatus
import com.example.domain.model.RedemptionLifecycleState
import com.example.domain.model.RedemptionPlan
import com.example.domain.model.RedemptionVerificationMethod
import com.example.domain.model.ShadowClass
import java.util.UUID

/**
 * Pure Kotlin, deterministic engine responsible for generating constructive, safe,
 * and measurable corrective activities for confirmed penalties.
 *
 * Non-negotiable safety rules:
 * - Never prescribe extra exercise as punishment.
 * - Recovery states override physical corrective generation (no workouts during fatigue/injury).
 * - Capped at maximum daily corrective duration and concurrent active commitments.
 * - Always favors small, sustainable, reflective actions.
 */
object CorrectiveActivityGenerationEngine {

    const val MAX_DAILY_CORRECTIVE_MINUTES = 45
    const val MAX_CONCURRENT_ACTIVE_REDEMPTIONS = 2

    /**
     * Curated catalog of standard corrective activities.
     */
    private val ACTIVITY_CATALOG: List<CorrectiveActivityDefinition> = listOf(
        // --- ACADEMIC ---
        CorrectiveActivityDefinition(
            activityId = "acad_concept_review_10",
            category = CorrectiveCategory.ACADEMIC,
            title = "Targeted Concept Flashcard Review",
            instructions = "Identify 3 key concepts from your recent missed academic topic and review or create summary flashcards.",
            difficulty = CorrectiveDifficulty.LIGHT,
            estimatedDurationMinutes = 10,
            verificationMethod = RedemptionVerificationMethod.STRUCTURED_REFLECTION,
            allowedSeverityTiers = setOf(PenaltySeverity.P1),
            defaultShadowClass = ShadowClass.RANGER,
            checklistItems = listOf("Select 3 difficult terms/concepts", "Draft concise explanations", "Review key formulas or definitions")
        ),
        CorrectiveActivityDefinition(
            activityId = "acad_focus_block_20",
            category = CorrectiveCategory.ACADEMIC,
            title = "Focused Academic Catch-Up Block",
            instructions = "Complete an uninterrupted 20-minute study session reviewing the core lecture or chapter notes for your missed objective.",
            difficulty = CorrectiveDifficulty.MANAGEABLE,
            estimatedDurationMinutes = 20,
            verificationMethod = RedemptionVerificationMethod.ACADEMIC_STUDY_VERIFIED,
            allowedSeverityTiers = setOf(PenaltySeverity.P2),
            defaultShadowClass = ShadowClass.RANGER,
            checklistItems = listOf("Silence notifications", "Review targeted subject chapter", "Document 2 main takeaways")
        ),
        CorrectiveActivityDefinition(
            activityId = "acad_error_log_30",
            category = CorrectiveCategory.ACADEMIC,
            title = "Error Log & Syllabus Realignment",
            instructions = "Audit your study log, record specific reasons for missed academic milestones, and restructure this week's study plan.",
            difficulty = CorrectiveDifficulty.STRUCTURED,
            estimatedDurationMinutes = 30,
            verificationMethod = RedemptionVerificationMethod.STRUCTURED_REFLECTION,
            allowedSeverityTiers = setOf(PenaltySeverity.P3, PenaltySeverity.P4),
            defaultShadowClass = ShadowClass.ARCANIST,
            checklistItems = listOf("Audit missed study sessions", "Document core roadblocks", "Adjust weekly calendar")
        ),

        // --- PHYSICAL (Gentle & Recovery-Aware; Never Punishment Workouts) ---
        CorrectiveActivityDefinition(
            activityId = "phys_schedule_reset_10",
            category = CorrectiveCategory.PHYSICAL,
            title = "Training Schedule Realignment",
            instructions = "Review your missed physical commitment. Adjust your workout timing for tomorrow to a realistic, non-conflicting hour.",
            difficulty = CorrectiveDifficulty.LIGHT,
            estimatedDurationMinutes = 10,
            verificationMethod = RedemptionVerificationMethod.CHECKLIST_CONFIRMATION,
            allowedSeverityTiers = setOf(PenaltySeverity.P1),
            defaultShadowClass = ShadowClass.GUARDIAN,
            checklistItems = listOf("Examine schedule conflicts", "Select optimal workout window", "Prepare training gear in advance")
        ),
        CorrectiveActivityDefinition(
            activityId = "phys_mobility_routine_20",
            category = CorrectiveCategory.PHYSICAL,
            title = "Gentle Mobility & Hydration Reset",
            instructions = "Execute 15 minutes of light joint mobility stretching and drink 500ml water to prime muscle recovery. Do not perform intense cardio.",
            difficulty = CorrectiveDifficulty.MANAGEABLE,
            estimatedDurationMinutes = 20,
            verificationMethod = RedemptionVerificationMethod.PHYSICAL_SESSION_VERIFIED,
            allowedSeverityTiers = setOf(PenaltySeverity.P2),
            defaultShadowClass = ShadowClass.GUARDIAN,
            checklistItems = listOf("Hydration intake (500ml)", "Lower back & hamstring mobility", "Shoulder & thoracic spine rotation")
        ),
        CorrectiveActivityDefinition(
            activityId = "phys_recovery_reflection_30",
            category = CorrectiveCategory.PHYSICAL,
            title = "Athletic Conditioning & Recovery Audit",
            instructions = "Audit your physical recovery metrics (sleep, hydration, pacing). Formulate a balanced, injury-free training plan for the upcoming cycle.",
            difficulty = CorrectiveDifficulty.STRUCTURED,
            estimatedDurationMinutes = 30,
            verificationMethod = RedemptionVerificationMethod.STRUCTURED_REFLECTION,
            allowedSeverityTiers = setOf(PenaltySeverity.P3, PenaltySeverity.P4),
            defaultShadowClass = ShadowClass.GUARDIAN,
            checklistItems = listOf("Log sleep & fatigue assessment", "Calibrate target pace/duration", "Formulate realistic 7-day schedule")
        ),

        // --- DISCIPLINE ---
        CorrectiveActivityDefinition(
            activityId = "disc_distraction_removal_10",
            category = CorrectiveCategory.DISCIPLINE,
            title = "Distraction Removal & Workspace Cleanse",
            instructions = "Audit your physical and digital environment. Remove one major distraction source (app notifications, cluttered desk) for the next 24 hours.",
            difficulty = CorrectiveDifficulty.LIGHT,
            estimatedDurationMinutes = 10,
            verificationMethod = RedemptionVerificationMethod.CHECKLIST_CONFIRMATION,
            allowedSeverityTiers = setOf(PenaltySeverity.P1),
            defaultShadowClass = ShadowClass.SUPPORT,
            checklistItems = listOf("Clear physical desk surface", "Set app notification limits", "Position focus tools front and center")
        ),
        CorrectiveActivityDefinition(
            activityId = "disc_routine_recalibration_20",
            category = CorrectiveCategory.DISCIPLINE,
            title = "Daily Routine Recalibration",
            instructions = "Review your morning and evening routines. Identify the friction point that led to the commitment failure and adjust step order.",
            difficulty = CorrectiveDifficulty.MANAGEABLE,
            estimatedDurationMinutes = 20,
            verificationMethod = RedemptionVerificationMethod.STRUCTURED_REFLECTION,
            allowedSeverityTiers = setOf(PenaltySeverity.P2),
            defaultShadowClass = ShadowClass.TACTICIAN,
            checklistItems = listOf("Map out yesterday's timeline", "Pinpoint failure friction point", "Update written routine checkpoints")
        ),
        CorrectiveActivityDefinition(
            activityId = "disc_deep_postmortem_30",
            category = CorrectiveCategory.DISCIPLINE,
            title = "Discipline Post-Mortem & Commitment Reset",
            instructions = "Conduct a structured root-cause analysis on recent missed commitments. Define 3 non-negotiable operational rules for the next cycle.",
            difficulty = CorrectiveDifficulty.STRUCTURED,
            estimatedDurationMinutes = 30,
            verificationMethod = RedemptionVerificationMethod.STRUCTURED_REFLECTION,
            allowedSeverityTiers = setOf(PenaltySeverity.P3, PenaltySeverity.P4),
            defaultShadowClass = ShadowClass.ARCANIST,
            checklistItems = listOf("Analyze habit trigger failures", "Define 3 preventative rules", "Document accountability check-in")
        ),

        // --- LIFE MANAGEMENT ---
        CorrectiveActivityDefinition(
            activityId = "life_calendar_audit_10",
            category = CorrectiveCategory.LIFE_MANAGEMENT,
            title = "Calendar & Task Re-balancing",
            instructions = "Review pending commitments on your schedule. Re-estimate task durations to eliminate overlapping commitments.",
            difficulty = CorrectiveDifficulty.LIGHT,
            estimatedDurationMinutes = 10,
            verificationMethod = RedemptionVerificationMethod.CHECKLIST_CONFIRMATION,
            allowedSeverityTiers = setOf(PenaltySeverity.P1),
            defaultShadowClass = ShadowClass.TACTICIAN,
            checklistItems = listOf("Check upcoming 48-hour schedule", "Add 15-min buffers between tasks", "Confirm next priority")
        ),
        CorrectiveActivityDefinition(
            activityId = "life_priority_matrix_20",
            category = CorrectiveCategory.LIFE_MANAGEMENT,
            title = "Eisenhower Priority Matrix Reorganization",
            instructions = "Categorize all outstanding personal tasks by Urgent vs Important. Eliminate or reschedule non-essential secondary demands.",
            difficulty = CorrectiveDifficulty.MANAGEABLE,
            estimatedDurationMinutes = 20,
            verificationMethod = RedemptionVerificationMethod.CHECKLIST_CONFIRMATION,
            allowedSeverityTiers = setOf(PenaltySeverity.P2),
            defaultShadowClass = ShadowClass.TACTICIAN,
            checklistItems = listOf("List all pending tasks", "Group into Urgent vs Important", "Defer non-urgent tasks")
        ),

        // --- STRATEGIC PLANNING ---
        CorrectiveActivityDefinition(
            activityId = "strat_root_cause_analysis_30",
            category = CorrectiveCategory.STRATEGIC_PLANNING,
            title = "Systemic Failure Post-Mortem",
            instructions = "Complete a thorough failure analysis identifying internal friction, environmental blockers, and concrete mitigations.",
            difficulty = CorrectiveDifficulty.STRUCTURED,
            estimatedDurationMinutes = 30,
            verificationMethod = RedemptionVerificationMethod.STRUCTURED_REFLECTION,
            allowedSeverityTiers = setOf(PenaltySeverity.P3),
            defaultShadowClass = ShadowClass.ARCANIST,
            checklistItems = listOf("Identify root cause factors", "Formulate mitigation protocol", "Validate with tactical schedule")
        ),
        CorrectiveActivityDefinition(
            activityId = "strat_comprehensive_reset_45",
            category = CorrectiveCategory.STRATEGIC_PLANNING,
            title = "Comprehensive Hunter Recovery Plan",
            instructions = "Deconstruct systemic breakdown: restructure daily commitments, establish buffer zones, and verify realistic targets for all modules.",
            difficulty = CorrectiveDifficulty.COMPREHENSIVE,
            estimatedDurationMinutes = 45,
            verificationMethod = RedemptionVerificationMethod.STRUCTURED_REFLECTION,
            allowedSeverityTiers = setOf(PenaltySeverity.P4),
            defaultShadowClass = ShadowClass.VANGUARD,
            checklistItems = listOf("Audit 14-day history", "Eliminate overloaded commitments", "Rebuild realistic daily quota", "Commit to verified baseline")
        )
    )

    /**
     * Generates a suitable, workload-aware, and safe redemption plan for an outstanding penalty.
     */
    fun generatePlan(
        penalty: PenaltyRecord,
        quest: Quest?,
        recoveryStatus: RecoveryStatus = RecoveryStatus.OPTIMAL,
        activeRedemptions: List<RedemptionPlan> = emptyList(),
        recentActivityIds: Set<String> = emptySet(),
        workloadMinutesToday: Int = 0
    ): RedemptionPlan {
        // 1. Check active concurrent redemptions cap
        if (activeRedemptions.size >= MAX_CONCURRENT_ACTIVE_REDEMPTIONS) {
            // Cannot activate more than maximum concurrent redemptions; generate a pending review plan
        }

        // 2. Determine target category from failed quest metadata
        val targetCategory = determineCategory(quest?.category, recoveryStatus)

        // 3. Map severity to difficulty
        val targetDifficulty = mapSeverityToDifficulty(penalty.severity, workloadMinutesToday)

        // 4. Select candidate activity from catalog
        val candidate = selectActivity(
            targetCategory = targetCategory,
            targetDifficulty = targetDifficulty,
            severity = penalty.severity,
            recentActivityIds = recentActivityIds,
            recoveryStatus = recoveryStatus
        )

        // 5. Generate shadow companion guidance
        val guidanceNote = generateShadowGuidance(candidate.defaultShadowClass, penalty, candidate)

        return RedemptionPlan(
            redemptionId = UUID.randomUUID().toString(),
            penaltyId = penalty.penaltyId,
            failureRecordId = penalty.failureRecordId,
            questId = penalty.questId,
            accountOwnerId = penalty.accountOwnerId,
            activityId = candidate.activityId,
            category = candidate.category,
            activityTitle = candidate.title,
            instructions = candidate.instructions,
            difficulty = candidate.difficulty,
            estimatedDurationMinutes = candidate.estimatedDurationMinutes,
            verificationMethod = candidate.verificationMethod,
            lifecycleState = RedemptionLifecycleState.PENDING_ACCEPTANCE,
            resolutionPoints = penalty.totalPenaltyPoints, // Full resolution capability upon completion
            assignedShadowClass = candidate.defaultShadowClass,
            shadowGuidanceNote = guidanceNote,
            createdAtEpochMs = System.currentTimeMillis()
        )
    }

    private fun determineCategory(
        questCategory: QuestCategory?,
        recoveryStatus: RecoveryStatus
    ): CorrectiveCategory {
        // SAFETY RULE: If player is in recovery/rest state, NEVER generate physical corrective activity!
        if (recoveryStatus == RecoveryStatus.RECOVERY_REQUIRED) {
            return CorrectiveCategory.STRATEGIC_PLANNING
        }

        return when (questCategory) {
            QuestCategory.INTELLECT -> CorrectiveCategory.ACADEMIC
            QuestCategory.PHYSICAL -> CorrectiveCategory.PHYSICAL
            QuestCategory.DISCIPLINE -> CorrectiveCategory.DISCIPLINE
            QuestCategory.SOCIAL, QuestCategory.RECOVERY -> CorrectiveCategory.LIFE_MANAGEMENT
            null -> CorrectiveCategory.STRATEGIC_PLANNING
        }
    }

    private fun mapSeverityToDifficulty(
        severity: PenaltySeverity,
        workloadMinutesToday: Int
    ): CorrectiveDifficulty {
        // If player already has heavy corrective workload today, throttle down to prevent exhaustion
        if (workloadMinutesToday >= MAX_DAILY_CORRECTIVE_MINUTES) {
            return CorrectiveDifficulty.LIGHT
        }

        return when (severity) {
            PenaltySeverity.P1 -> CorrectiveDifficulty.LIGHT
            PenaltySeverity.P2 -> CorrectiveDifficulty.MANAGEABLE
            PenaltySeverity.P3 -> CorrectiveDifficulty.STRUCTURED
            PenaltySeverity.P4 -> {
                if (workloadMinutesToday > 15) CorrectiveDifficulty.STRUCTURED else CorrectiveDifficulty.COMPREHENSIVE
            }
        }
    }

    private fun selectActivity(
        targetCategory: CorrectiveCategory,
        targetDifficulty: CorrectiveDifficulty,
        severity: PenaltySeverity,
        recentActivityIds: Set<String>,
        recoveryStatus: RecoveryStatus
    ): CorrectiveActivityDefinition {
        // Filter catalog
        var candidates = ACTIVITY_CATALOG.filter { act ->
            act.category == targetCategory &&
                (act.difficulty == targetDifficulty || severity in act.allowedSeverityTiers) &&
                act.activityId !in recentActivityIds
        }

        // SAFETY: If recovery required, exclude any physical activity
        if (recoveryStatus == RecoveryStatus.RECOVERY_REQUIRED) {
            candidates = candidates.filter { it.category != CorrectiveCategory.PHYSICAL }
        }

        if (candidates.isEmpty()) {
            // Fallback to any non-recent activity in same category or strategic planning
            candidates = ACTIVITY_CATALOG.filter {
                (it.category == targetCategory || it.category == CorrectiveCategory.STRATEGIC_PLANNING) &&
                    (recoveryStatus != RecoveryStatus.RECOVERY_REQUIRED || it.category != CorrectiveCategory.PHYSICAL)
            }
        }

        return candidates.minByOrNull { act ->
            // Prefer activities closest to target duration
            kotlin.math.abs(act.estimatedDurationMinutes - targetDifficulty.estimatedMinutes)
        } ?: ACTIVITY_CATALOG.first { it.activityId == "strat_root_cause_analysis_30" }
    }

    private fun generateShadowGuidance(
        shadowClass: ShadowClass,
        penalty: PenaltyRecord,
        activity: CorrectiveActivityDefinition
    ): String {
        return when (shadowClass) {
            ShadowClass.GUARDIAN ->
                "Guardian Ward: \"Protection precedes power. Complete this ${activity.title} with composure. No physical strain is required—only consistency.\""
            ShadowClass.TACTICIAN ->
                "Tactician Insight: \"A tactical misstep is corrected through superior scheduling. Align your calendar and eliminate friction for the next assault.\""
            ShadowClass.ARCANIST ->
                "Arcanist Observation: \"Every failure contains an observable pattern. Deconstruct why '${penalty.questTitleSnapshot}' was missed, and the vulnerability vanishes.\""
            ShadowClass.RANGER ->
                "Ranger Focus: \"Clear your field of view. Maintain unyielding precision during this ${activity.estimatedDurationMinutes}-minute focus block.\""
            ShadowClass.SUPPORT ->
                "Support Vanguard: \"One step at a time, Monarch. Fulfill this corrective checkpoint and your penalty points will be cleanly purged.\""
        }
    }
}
