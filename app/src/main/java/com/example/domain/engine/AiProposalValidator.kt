package com.example.domain.engine

import com.example.domain.model.AiDailyPlanItem
import com.example.domain.model.AiDungeonProposal
import com.example.domain.model.AiQuestProposal
import com.example.domain.model.AiQuestProposalCategory
import com.example.domain.model.DungeonCategory
import com.example.domain.model.DungeonDifficulty
import com.example.domain.model.DungeonEvidenceType
import com.example.domain.model.PlanMode
import com.example.domain.model.PlayerContextSnapshot
import com.example.domain.model.ProposalStatus
import com.example.domain.model.RecoveryState
import com.example.domain.model.SchedulePriority
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
private data class WireQuestProposalList(
    val proposals: List<WireQuestProposal> = emptyList()
)

@Serializable
private data class WireQuestProposal(
    val title: String = "",
    val description: String = "",
    val category: String = "DISCIPLINE",
    val difficulty: String = "E-Rank",
    val objective: String = "",
    val completionCriteria: String = "",
    val suggestedDurationMinutes: Int = 30,
    val priority: String = "MEDIUM",
    val reason: String = ""
)

@Serializable
private data class WireDailyPlanList(
    val items: List<WireDailyPlanItem> = emptyList()
)

@Serializable
private data class WireDailyPlanItem(
    val taskTitle: String = "",
    val module: String = "ACADEMIC",
    val startMinuteOfDay: Int = 540,
    val durationMinutes: Int = 30,
    val priority: String = "MEDIUM",
    val reason: String = "",
    val dependencies: String = ""
)

/**
 * Validates, deduplicates, safety-guards, and resolves overlaps for AI-proposed quests and schedules.
 * Proposals NEVER grant XP directly and NEVER bypass recovery safety rules.
 */
object AiProposalValidator {

    private val lenientJson = Json { ignoreUnknownKeys = true; isLenient = true }

    fun generateDeterministicQuestProposals(
        snapshot: PlayerContextSnapshot,
        existingActiveTitles: Set<String>
    ): List<AiQuestProposal> {
        val proposals = mutableListOf<AiQuestProposal>()
        val phys = snapshot.physicalSummary
        val acad = snapshot.academicSummary

        // Physical / Recovery proposal constrained by recovery safety state
        if (phys != null) {
            when (phys.recoveryState) {
                RecoveryState.REST_RECOMMENDED, RecoveryState.RECOVERY_RECOMMENDED -> {
                    proposals.add(
                        AiQuestProposal(
                            proposalUuid = UUID.randomUUID().toString(),
                            title = "Guided Recovery & Mobility Reset",
                            description = "Protect readiness (${phys.readinessScore}/100) with gentle mobility instead of high-strain exertion.",
                            category = AiQuestProposalCategory.RECOVERY,
                            difficultyLabel = "E-Rank",
                            objective = "Complete 15 minutes of mobility stretching or restorative walk",
                            completionCriteria = "Log recovery session in Physical Engine",
                            suggestedDurationMinutes = 15,
                            priority = SchedulePriority.CRITICAL,
                            reason = "Recovery state ${phys.recoveryState.code} blocks high-intensity training."
                        )
                    )
                }
                RecoveryState.MODERATE, RecoveryState.READY -> {
                    val remainingJog = (phys.joggingTargetMinutes - phys.completedJoggingTodayMinutes).coerceAtLeast(15)
                    proposals.add(
                        AiQuestProposal(
                            proposalUuid = UUID.randomUUID().toString(),
                            title = "Steady Aerobic Jog (${remainingJog}m)",
                            description = "Advance VIT and AGI while maintaining consistent aerobic cadence.",
                            category = AiQuestProposalCategory.PHYSICAL,
                            difficultyLabel = if (remainingJog >= 35) "D-Rank" else "E-Rank",
                            objective = "Jog or run for $remainingJog minutes",
                            completionCriteria = "Complete live timer or sync via Health Connect",
                            suggestedDurationMinutes = remainingJog,
                            priority = SchedulePriority.HIGH,
                            reason = "Daily jogging progress is ${phys.completedJoggingTodayMinutes}/${phys.joggingTargetMinutes}m."
                        )
                    )
                }
            }
        }

        // Academic proposals grounded in real backlog
        if (acad != null) {
            if (acad.overdueRevisionsCount > 0 || acad.dueTodayRevisionsCount > 0) {
                proposals.add(
                    AiQuestProposal(
                        proposalUuid = UUID.randomUUID().toString(),
                        title = "Spaced Recall Clearance (${acad.overdueRevisionsCount + acad.dueTodayRevisionsCount} due)",
                        description = "Review due topics in Revision Centre to reinforce WIS and recall stability.",
                        category = AiQuestProposalCategory.ACADEMIC,
                        difficultyLabel = "D-Rank",
                        objective = "Complete 2 spaced revision reviews",
                        completionCriteria = "Record confidence ratings in Revision Centre",
                        suggestedDurationMinutes = 25,
                        priority = SchedulePriority.HIGH,
                        reason = "${acad.overdueRevisionsCount} overdue and ${acad.dueTodayRevisionsCount} due-today revisions detected."
                    )
                )
            } else if (acad.activeSubjectsCount > 0) {
                val targetFocus = (acad.dailyGoalMinutes - acad.todayStudyMinutes).coerceIn(25, 60)
                proposals.add(
                    AiQuestProposal(
                        proposalUuid = UUID.randomUUID().toString(),
                        title = "Deep Focus Block (${targetFocus}m)",
                        description = "Targeted study session toward your daily ${acad.dailyGoalMinutes}m goal.",
                        category = AiQuestProposalCategory.ACADEMIC,
                        difficultyLabel = "E-Rank",
                        objective = "Complete a ${targetFocus}-minute uninterrupted focus block",
                        completionCriteria = "Complete Focus Timer block in Academic Engine",
                        suggestedDurationMinutes = targetFocus,
                        priority = SchedulePriority.MEDIUM,
                        reason = "Today's study total is ${acad.todayStudyMinutes}/${acad.dailyGoalMinutes}m."
                    )
                )
            }
        }

        snapshot.lifeSummary?.let { life ->
            if (life.overdueTasksCount > 0) {
                proposals.add(
                    AiQuestProposal(
                        proposalUuid = UUID.randomUUID().toString(),
                        title = "Clear ${life.overdueTasksCount.coerceAtMost(3)} Overdue Personal Task(s)",
                        description = "Resolve overdue commitments or realistically reschedule them in Personal Tasks.",
                        category = AiQuestProposalCategory.DISCIPLINE,
                        difficultyLabel = "E-Rank",
                        objective = "Triage and resolve overdue personal tasks",
                        completionCriteria = "Complete or reschedule overdue tasks in Life Engine",
                        suggestedDurationMinutes = 30,
                        priority = SchedulePriority.HIGH,
                        reason = "${life.overdueTasksCount} personal task(s) currently overdue."
                    )
                )
            } else if (life.activeHabitsCount > 0 && life.completedHabitsToday < life.activeHabitsCount) {
                proposals.add(
                    AiQuestProposal(
                        proposalUuid = UUID.randomUUID().toString(),
                        title = "Complete Daily Habit Protocol",
                        description = "Execute remaining active habits while honoring rest-day protections.",
                        category = AiQuestProposalCategory.PERSONAL,
                        difficultyLabel = "E-Rank",
                        objective = "Complete active daily habits scheduled for today",
                        completionCriteria = "Log completion in Habit Dashboard",
                        suggestedDurationMinutes = 20,
                        priority = SchedulePriority.MEDIUM,
                        reason = "${life.completedHabitsToday}/${life.activeHabitsCount} active habits logged today."
                    )
                )
            }
        }

        if (snapshot.profileSummary.verifiedActivitiesCount == 0) {
            proposals.add(
                AiQuestProposal(
                    proposalUuid = UUID.randomUUID().toString(),
                    title = "First Verified System Session",
                    description = "Build rank promotion eligibility with a timer-verified or Health Connect verified session.",
                    category = AiQuestProposalCategory.DISCIPLINE,
                    difficultyLabel = "E-Rank",
                    objective = "Complete 1 verified physical or focus timer session",
                    completionCriteria = "Verified provenance tag recorded on activity log",
                    suggestedDurationMinutes = 25,
                    priority = SchedulePriority.MEDIUM,
                    reason = "Higher ranks require verified activities rather than self-reported logs alone."
                )
            )
        }

        return sanitizeAndFilterProposals(
            candidates = proposals,
            snapshot = snapshot,
            existingActiveTitles = existingActiveTitles
        )
    }

    fun parseQuestProposalsJson(
        rawJson: String,
        snapshot: PlayerContextSnapshot,
        existingActiveTitles: Set<String>
    ): List<AiQuestProposal> {
        val cleaned = extractJsonObject(rawJson) ?: return emptyList()
        val parsed = runCatching { lenientJson.decodeFromString<WireQuestProposalList>(cleaned) }.getOrNull()
            ?: return emptyList()
        val mapped = parsed.proposals.mapNotNull { w ->
            val title = PromptSanitizer.sanitize(w.title, 70)
            if (title.isBlank()) return@mapNotNull null
            val cat = runCatching { AiQuestProposalCategory.valueOf(w.category.uppercase()) }
                .getOrDefault(AiQuestProposalCategory.DISCIPLINE)
            val prio = runCatching { SchedulePriority.valueOf(w.priority.uppercase()) }
                .getOrDefault(SchedulePriority.MEDIUM)
            AiQuestProposal(
                proposalUuid = UUID.randomUUID().toString(),
                title = title,
                description = PromptSanitizer.sanitize(w.description.ifBlank { w.objective }, 180),
                category = cat,
                difficultyLabel = PromptSanitizer.sanitize(w.difficulty.ifBlank { "E-Rank" }, 16),
                objective = PromptSanitizer.sanitize(w.objective.ifBlank { title }, 120),
                completionCriteria = PromptSanitizer.sanitize(w.completionCriteria.ifBlank { "Verify completion in module" }, 120),
                suggestedDurationMinutes = w.suggestedDurationMinutes.coerceIn(10, 120),
                priority = prio,
                reason = PromptSanitizer.sanitize(w.reason.ifBlank { "Aligned with player development snapshot" }, 140)
            )
        }
        return sanitizeAndFilterProposals(mapped, snapshot, existingActiveTitles)
    }

    fun sanitizeAndFilterProposals(
        candidates: List<AiQuestProposal>,
        snapshot: PlayerContextSnapshot,
        existingActiveTitles: Set<String>
    ): List<AiQuestProposal> {
        val recoveryState = snapshot.physicalSummary?.recoveryState
        val blockIntensePhysical = recoveryState == RecoveryState.REST_RECOMMENDED ||
            recoveryState == RecoveryState.RECOVERY_RECOMMENDED ||
            snapshot.physicalSummary?.selfReportedDiscomfort == true

        val seenNormalized = existingActiveTitles.map { normalizeTitle(it) }.toMutableSet()
        val result = mutableListOf<AiQuestProposal>()

        for (candidate in candidates) {
            if (candidate.title.isBlank()) continue
            if (candidate.suggestedDurationMinutes !in 5..120) continue

            // Safety guardrail: block physical strain quests when rest/recovery is required
            if (blockIntensePhysical && candidate.category == AiQuestProposalCategory.PHYSICAL) {
                continue
            }

            val norm = normalizeTitle(candidate.title)
            val isDuplicate = seenNormalized.any { existing ->
                existing == norm || (norm.length >= 8 && existing.contains(norm)) || (existing.length >= 8 && norm.contains(existing))
            }
            if (isDuplicate) continue

            seenNormalized.add(norm)
            result.add(candidate.copy(status = ProposalStatus.PENDING))
            if (result.size >= 4) break // prevent unrealistic workload
        }
        return result
    }

    fun generateDeterministicPlan(
        mode: PlanMode,
        snapshot: PlayerContextSnapshot
    ): List<AiDailyPlanItem> {
        val batchId = UUID.randomUUID().toString()
        val phys = snapshot.physicalSummary
        val acad = snapshot.academicSummary
        val items = mutableListOf<AiDailyPlanItem>()

        when (mode) {
            PlanMode.RECOVERY_PLAN -> {
                items.add(
                    AiDailyPlanItem(
                        batchId = batchId, planMode = mode,
                        taskTitle = "Light Mobility & Stretching",
                        module = "PHYSICAL", startMinuteOfDay = 8 * 60 + 30, durationMinutes = 15,
                        priority = SchedulePriority.HIGH,
                        reason = "Safe gentle movement respecting recovery score (${phys?.readinessScore ?: 70}/100)."
                    )
                )
                items.add(
                    AiDailyPlanItem(
                        batchId = batchId, planMode = mode,
                        taskTitle = "Hydration & Early Sleep Wind-Down",
                        module = "RECOVERY", startMinuteOfDay = 21 * 60, durationMinutes = 30,
                        priority = SchedulePriority.CRITICAL,
                        reason = "Supports nervous system recovery without physical overload."
                    )
                )
            }
            PlanMode.REVISION_SCHEDULE, PlanMode.EXAM_PREP -> {
                val overdue = acad?.overdueRevisionsCount ?: 0
                items.add(
                    AiDailyPlanItem(
                        batchId = batchId, planMode = mode,
                        taskTitle = if (overdue > 0) "Clear Overdue Spaced Revisions ($overdue due)" else "Active Recall Review Block",
                        module = "ACADEMIC", startMinuteOfDay = 9 * 60, durationMinutes = 35,
                        priority = SchedulePriority.CRITICAL,
                        reason = "Prioritize lowest-confidence topics first."
                    )
                )
                items.add(
                    AiDailyPlanItem(
                        batchId = batchId, planMode = mode,
                        taskTitle = "Timed Practice Problems / Past Paper",
                        module = "ACADEMIC", startMinuteOfDay = 15 * 60, durationMinutes = 50,
                        priority = SchedulePriority.HIGH,
                        reason = "Builds assessment familiarity and WIS/INT progression.",
                        dependencies = "Complete morning recall review"
                    )
                )
            }
            PlanMode.WORKOUT_SCHEDULE -> {
                val restRequired = phys?.recoveryState == RecoveryState.REST_RECOMMENDED || phys?.selfReportedDiscomfort == true
                items.add(
                    AiDailyPlanItem(
                        batchId = batchId, planMode = mode,
                        taskTitle = if (restRequired) "Penalty-Free Rest / Mobility Only" else "Aerobic Jog (${phys?.joggingTargetMinutes ?: 30}m)",
                        module = "PHYSICAL", startMinuteOfDay = 7 * 60 + 30,
                        durationMinutes = if (restRequired) 15 else (phys?.joggingTargetMinutes ?: 30),
                        priority = SchedulePriority.HIGH,
                        reason = if (restRequired) "Recovery safety rule prevents high-load training." else "Maintains daily aerobic cadence."
                    )
                )
            }
            PlanMode.DAILY, PlanMode.WEEKLY -> {
                val restRequired = phys?.recoveryState == RecoveryState.REST_RECOMMENDED ||
                    phys?.recoveryState == RecoveryState.RECOVERY_RECOMMENDED ||
                    phys?.selfReportedDiscomfort == true
                items.add(
                    AiDailyPlanItem(
                        batchId = batchId, planMode = mode,
                        taskTitle = if (restRequired) "Recovery Mobility Walk" else "Morning Jogging Session",
                        module = "PHYSICAL", startMinuteOfDay = 7 * 60 + 30,
                        durationMinutes = if (restRequired) 15 else (phys?.joggingTargetMinutes ?: 30),
                        priority = SchedulePriority.HIGH,
                        reason = if (restRequired) "Adjusted to protect recovery readiness." else "Daily aerobic target baseline."
                    )
                )
                items.add(
                    AiDailyPlanItem(
                        batchId = batchId, planMode = mode,
                        taskTitle = "Focused Study Block (${acad?.subjectNamesAndCompletion?.firstOrNull()?.substringBefore(" (") ?: "Primary Subject"})",
                        module = "ACADEMIC", startMinuteOfDay = 10 * 60, durationMinutes = 50,
                        priority = SchedulePriority.HIGH,
                        reason = "Advances daily study target (${acad?.dailyGoalMinutes ?: 60}m) and INT."
                    )
                )
                items.add(
                    AiDailyPlanItem(
                        batchId = batchId, planMode = mode,
                        taskTitle = "Evening Spaced Revision & Daily Audit",
                        module = "ACADEMIC", startMinuteOfDay = 18 * 60 + 30, durationMinutes = 25,
                        priority = SchedulePriority.MEDIUM,
                        reason = "Reinforces recall and closes active daily quests.",
                        dependencies = "Complete Focused Study Block"
                    )
                )
            }
        }
        return resolveNonOverlappingSchedule(items)
    }

    fun parsePlanJson(rawJson: String, mode: PlanMode): List<AiDailyPlanItem> {
        val cleaned = extractJsonObject(rawJson) ?: return emptyList()
        val parsed = runCatching { lenientJson.decodeFromString<WireDailyPlanList>(cleaned) }.getOrNull()
            ?: return emptyList()
        val batchId = UUID.randomUUID().toString()
        val mapped = parsed.items.mapNotNull { w ->
            val title = PromptSanitizer.sanitize(w.taskTitle, 70)
            if (title.isBlank()) return@mapNotNull null
            AiDailyPlanItem(
                batchId = batchId,
                planMode = mode,
                taskTitle = title,
                module = PromptSanitizer.sanitize(w.module.uppercase().ifBlank { "GENERAL" }, 20),
                startMinuteOfDay = w.startMinuteOfDay.coerceIn(6 * 60, 22 * 60),
                durationMinutes = w.durationMinutes.coerceIn(10, 120),
                priority = runCatching { SchedulePriority.valueOf(w.priority.uppercase()) }.getOrDefault(SchedulePriority.MEDIUM),
                reason = PromptSanitizer.sanitize(w.reason.ifBlank { "AI scheduled block" }, 140),
                dependencies = PromptSanitizer.sanitize(w.dependencies, 80)
            )
        }
        return resolveNonOverlappingSchedule(mapped)
    }

    /**
     * Guarantees no overlapping proposed time blocks in the schedule.
     */
    fun resolveNonOverlappingSchedule(items: List<AiDailyPlanItem>): List<AiDailyPlanItem> {
        val sorted = items.sortedBy { it.startMinuteOfDay }
        val resolved = mutableListOf<AiDailyPlanItem>()
        var cursorEnd = 0
        for (item in sorted) {
            val safeStart = maxOf(item.startMinuteOfDay.coerceIn(6 * 60, 22 * 60), cursorEnd)
            val buffered = item.copy(startMinuteOfDay = safeStart)
            resolved.add(buffered)
            cursorEnd = buffered.startMinuteOfDay + buffered.durationMinutes + 10 // 10 min transition buffer
        }
        return resolved
    }

    fun generateDeterministicDungeonProposals(
        snapshot: PlayerContextSnapshot,
        existingTitles: Set<String>
    ): List<AiDungeonProposal> {
        val phys = snapshot.physicalSummary
        val acad = snapshot.academicSummary
        val rankCode = snapshot.profileSummary.rankCode
        val suggestedDiff = when (rankCode) {
            "S" -> DungeonDifficulty.B_RANK
            "A", "B" -> DungeonDifficulty.C_RANK
            "C", "D" -> DungeonDifficulty.D_RANK
            else -> DungeonDifficulty.E_RANK
        }
        val candidates = mutableListOf<AiDungeonProposal>()
        val restNeeded = phys?.recoveryState == RecoveryState.REST_RECOMMENDED ||
            phys?.recoveryState == RecoveryState.RECOVERY_RECOMMENDED ||
            phys?.selfReportedDiscomfort == true

        if (restNeeded) {
            candidates.add(
                AiDungeonProposal(
                    proposalUuid = UUID.randomUUID().toString(),
                    title = "AI Gate: Restorative Focus & Mobility Sanctuary",
                    description = "Personalized low-strain dungeon respecting current recovery limits.",
                    category = DungeonCategory.RECOVERY_WELLBEING,
                    difficulty = DungeonDifficulty.SPECIAL,
                    objectiveTitle = "Complete 30 minutes of calm academic study",
                    targetEvidenceType = DungeonEvidenceType.ACADEMIC_STUDY_MINUTES,
                    targetValue = 30,
                    timeLimitHours = 24,
                    requiresVerifiedOnly = false,
                    reason = "Recovery state (${phys?.recoveryState?.code}) requires avoiding strenuous physical exertion.",
                    isBossCampaign = false
                )
            )
        } else {
            candidates.add(
                AiDungeonProposal(
                    proposalUuid = UUID.randomUUID().toString(),
                    title = "AI Gate: Verified Aerobic Endurance Trial",
                    description = "Personalized endurance trial matched to your ${snapshot.profileSummary.rankName} conditioning.",
                    category = DungeonCategory.PHYSICAL,
                    difficulty = suggestedDiff,
                    objectiveTitle = "Complete ${(phys?.joggingTargetMinutes ?: 30).coerceIn(25, 60)} min of jogging",
                    targetEvidenceType = DungeonEvidenceType.PHYSICAL_JOG_MINUTES,
                    targetValue = (phys?.joggingTargetMinutes ?: 30).coerceIn(25, 60),
                    timeLimitHours = 48,
                    requiresVerifiedOnly = false,
                    reason = "Supports VIT/AGI development while reinforcing weekly training consistency.",
                    isBossCampaign = false
                )
            )
        }

        if (acad != null && acad.activeSubjectsCount > 0) {
            val overdue = acad.overdueRevisionsCount
            candidates.add(
                AiDungeonProposal(
                    proposalUuid = UUID.randomUUID().toString(),
                    title = if (overdue > 0) "AI Gate: Memory Reclamation Vault" else "AI Gate: Deep Focus Citadel",
                    description = "Personalized academic gate targeting recall stability and uninterrupted study blocks.",
                    category = DungeonCategory.ACADEMIC,
                    difficulty = suggestedDiff,
                    objectiveTitle = if (overdue > 0) "Clear 3 spaced revisions" else "Complete 60 min of timer-verified study",
                    targetEvidenceType = if (overdue > 0) DungeonEvidenceType.ACADEMIC_REVISIONS_COMPLETED else DungeonEvidenceType.ACADEMIC_STUDY_MINUTES,
                    targetValue = if (overdue > 0) 3 else 60,
                    timeLimitHours = 48,
                    requiresVerifiedOnly = overdue == 0,
                    reason = if (overdue > 0) "$overdue overdue revisions detected." else "Builds verified focus discipline.",
                    isBossCampaign = false
                )
            )
        }

        val normalizedExisting = existingTitles.map { normalizeTitle(it) }.toSet()
        return candidates.filter { normalizeTitle(it.title) !in normalizedExisting }.take(3)
    }

    private fun normalizeTitle(raw: String): String =
        raw.lowercase().replace(Regex("[^a-z0-9]"), "")

    private fun extractJsonObject(raw: String): String? {
        val trimmed = raw.trim()
        val firstBrace = trimmed.indexOf('{')
        val lastBrace = trimmed.lastIndexOf('}')
        if (firstBrace < 0 || lastBrace <= firstBrace) return null
        return trimmed.substring(firstBrace, lastBrace + 1)
    }
}
