package com.example.domain.engine

import com.example.domain.model.AcademicAssessment
import com.example.domain.model.AcademicChapter
import com.example.domain.model.AttributeType
import com.example.domain.model.BossBattleDefinition
import com.example.domain.model.BossBattleInstance
import com.example.domain.model.BossBattleWithPhases
import com.example.domain.model.BossPhase
import com.example.domain.model.BossPhaseProgress
import com.example.domain.model.BossStatus
import com.example.domain.model.DungeonCategory
import com.example.domain.model.DungeonCreationSource
import com.example.domain.model.DungeonDefinition
import com.example.domain.model.DungeonDifficulty
import com.example.domain.model.DungeonEvidenceType
import com.example.domain.model.DungeonInstance
import com.example.domain.model.DungeonObjective
import com.example.domain.model.DungeonObjectiveProgress
import com.example.domain.model.DungeonStatus
import com.example.domain.model.DungeonWithStatus
import com.example.domain.model.GoalMilestone
import com.example.domain.model.HabitCompletionRecord
import com.example.domain.model.HabitDayStatus
import com.example.domain.model.HunterRank
import com.example.domain.model.JoggingSession
import com.example.domain.model.PersonalTask
import com.example.domain.model.PersonalTaskStatus
import com.example.domain.model.PlayerProfile
import com.example.domain.model.RecoveryState
import com.example.domain.model.RevisionRecord
import com.example.domain.model.RoutineActivityStatus
import com.example.domain.model.RoutineCompletionRecord
import com.example.domain.model.StudySession
import com.example.domain.model.WorkoutSession
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Unified read-only snapshot of verified & logged user telemetry across Physical, Academic,
 * Life Management, and Core systems used by deterministic Milestone 6 engines.
 */
data class PlayerTelemetrySnapshot(
    val profile: PlayerProfile,
    val recoveryState: RecoveryState = RecoveryState.READY,
    val todayEpochDay: Long = LocalDate.now().toEpochDay(),
    val nowEpochMs: Long = System.currentTimeMillis(),
    val jogs: List<JoggingSession> = emptyList(),
    val workouts: List<WorkoutSession> = emptyList(),
    val studySessions: List<StudySession> = emptyList(),
    val chapters: List<AcademicChapter> = emptyList(),
    val revisions: List<RevisionRecord> = emptyList(),
    val assessments: List<AcademicAssessment> = emptyList(),
    val habitCompletions: List<HabitCompletionRecord> = emptyList(),
    val routineCompletions: List<RoutineCompletionRecord> = emptyList(),
    val completedTasks: List<PersonalTask> = emptyList(),
    val completedMilestones: List<GoalMilestone> = emptyList(),
    val completedQuestsCount: Int = 0,
    val completedDungeonsCount: Int = 0,
    val completedBossesCount: Int = 0,
    val unlockedSkillsCount: Int = 0,
    val activeWorkloadCount: Int = 0,
    val exemptRestDaysCount: Int = 0
)

data class DungeonSeedSpec(
    val definition: DungeonDefinition,
    val objectives: List<DungeonObjective>
)

data class BossSeedSpec(
    val definition: BossBattleDefinition,
    val phases: List<BossPhase>
)

data class DungeonValidationResult(
    val canComplete: Boolean,
    val evidenceSummary: String,
    val verificationProvenance: String,
    val hasVerifiedEvidence: Boolean,
    val failureReason: String? = null
)

/**
 * Deterministic Dungeon Gate discovery, eligibility, adaptive Daily/Weekly generation,
 * and real-record completion validation. Never awards XP for entering or browsing.
 */
object DungeonEngine {

    fun defaultCatalog(): List<DungeonSeedSpec> = listOf(
        DungeonSeedSpec(
            definition = DungeonDefinition(
                code = "sys_e_first_stride",
                title = "Gate E-01: Endurance Awakening",
                description = "Complete 30 minutes of logged or verified aerobic training and 1 focus study block.",
                category = DungeonCategory.MULTI_DOMAIN_CHALLENGE,
                difficulty = DungeonDifficulty.E_RANK,
                requiredRank = HunterRank.E_RANK,
                requiredLevel = 1,
                timeLimitHours = 48,
                baseXpReward = 65,
                primaryAttribute = AttributeType.VIT,
                primaryAttrXp = 28,
                secondaryAttribute = AttributeType.INT,
                secondaryAttrXp = 18,
                rewardTitleId = "title_gate_runner",
                rewardItemId = "item_badge_cyan_initiate",
                isRepeatable = true
            ),
            objectives = listOf(
                DungeonObjective(dungeonId = 0, title = "Log 30 minutes of jogging or running", evidenceType = DungeonEvidenceType.PHYSICAL_JOG_MINUTES, targetValue = 30, orderIndex = 0),
                DungeonObjective(dungeonId = 0, title = "Complete 25 minutes of focused study", evidenceType = DungeonEvidenceType.ACADEMIC_STUDY_MINUTES, targetValue = 25, orderIndex = 1)
            )
        ),
        DungeonSeedSpec(
            definition = DungeonDefinition(
                code = "sys_e_discipline_crypt",
                title = "Gate E-02: Vault of Order",
                description = "Execute daily routines, complete habits, and clear pending responsibilities.",
                category = DungeonCategory.DISCIPLINE,
                difficulty = DungeonDifficulty.E_RANK,
                requiredRank = HunterRank.E_RANK,
                requiredLevel = 1,
                timeLimitHours = 48,
                baseXpReward = 60,
                primaryAttribute = AttributeType.DIS,
                primaryAttrXp = 30,
                secondaryAttribute = AttributeType.WIS,
                secondaryAttrXp = 15,
                rewardItemId = "item_frame_obsidian_grid",
                isRepeatable = true
            ),
            objectives = listOf(
                DungeonObjective(dungeonId = 0, title = "Complete 2 habit check-ins", evidenceType = DungeonEvidenceType.LIFE_HABIT_COMPLETIONS, targetValue = 2, orderIndex = 0),
                DungeonObjective(dungeonId = 0, title = "Complete 2 personal tasks", evidenceType = DungeonEvidenceType.LIFE_TASK_COMPLETIONS, targetValue = 2, orderIndex = 1)
            )
        ),
        DungeonSeedSpec(
            definition = DungeonDefinition(
                code = "sys_d_verified_gauntlet",
                title = "Gate D-01: Verified Conditioning Trial",
                description = "Demonstrate authentic physical output using Health Connect or verified workouts.",
                category = DungeonCategory.PHYSICAL,
                difficulty = DungeonDifficulty.D_RANK,
                requiredRank = HunterRank.D_RANK,
                requiredLevel = 5,
                requiredAttributeCode = "VIT",
                requiredAttributeValue = 12,
                timeLimitHours = 72,
                baseXpReward = 95,
                primaryAttribute = AttributeType.STR,
                primaryAttrXp = 38,
                secondaryAttribute = AttributeType.VIT,
                secondaryAttrXp = 25,
                rewardTitleId = "title_iron_vanguard",
                rewardShadowId = "shadow_obsidian_sentinel",
                isRepeatable = true
            ),
            objectives = listOf(
                DungeonObjective(dungeonId = 0, title = "Complete 2 verified physical sessions", evidenceType = DungeonEvidenceType.VERIFIED_PHYSICAL_SESSIONS, targetValue = 2, requiresVerifiedOnly = true, orderIndex = 0),
                DungeonObjective(dungeonId = 0, title = "Accumulate 45 jogging minutes", evidenceType = DungeonEvidenceType.PHYSICAL_JOG_MINUTES, targetValue = 45, orderIndex = 1)
            )
        ),
        DungeonSeedSpec(
            definition = DungeonDefinition(
                code = "sys_d_arcane_archive",
                title = "Gate D-02: Arcane Scholar Archive",
                description = "Advance academic mastery via timer-verified focus sessions and spaced revisions.",
                category = DungeonCategory.ACADEMIC,
                difficulty = DungeonDifficulty.D_RANK,
                requiredRank = HunterRank.D_RANK,
                requiredLevel = 4,
                requiredAttributeCode = "INT",
                requiredAttributeValue = 12,
                timeLimitHours = 72,
                baseXpReward = 90,
                primaryAttribute = AttributeType.INT,
                primaryAttrXp = 40,
                secondaryAttribute = AttributeType.WIS,
                secondaryAttrXp = 22,
                rewardItemId = "item_theme_arcane_citadel",
                rewardShadowId = "shadow_astral_prism",
                isRepeatable = true
            ),
            objectives = listOf(
                DungeonObjective(dungeonId = 0, title = "Complete 90 minutes of timer-verified study", evidenceType = DungeonEvidenceType.ACADEMIC_STUDY_MINUTES, targetValue = 90, requiresVerifiedOnly = true, orderIndex = 0),
                DungeonObjective(dungeonId = 0, title = "Finish 3 spaced revision sessions", evidenceType = DungeonEvidenceType.ACADEMIC_REVISIONS_COMPLETED, targetValue = 3, orderIndex = 1)
            )
        ),
        DungeonSeedSpec(
            definition = DungeonDefinition(
                code = "sys_c_sovereign_trial",
                title = "Gate C-01: Vanguard Crucible",
                description = "High-intensity multi-domain gate balancing verified workouts, study chapters, and goal milestones.",
                category = DungeonCategory.MULTI_DOMAIN_CHALLENGE,
                difficulty = DungeonDifficulty.C_RANK,
                requiredRank = HunterRank.C_RANK,
                requiredLevel = 12,
                requiredAttributeCode = "DIS",
                requiredAttributeValue = 16,
                timeLimitHours = 120,
                baseXpReward = 135,
                primaryAttribute = AttributeType.DIS,
                primaryAttrXp = 48,
                secondaryAttribute = AttributeType.STR,
                secondaryAttrXp = 30,
                rewardTitleId = "title_vanguard_commander",
                rewardShadowId = "shadow_aegis_monolith",
                isRepeatable = true
            ),
            objectives = listOf(
                DungeonObjective(dungeonId = 0, title = "Complete 3 verified physical sessions", evidenceType = DungeonEvidenceType.VERIFIED_PHYSICAL_SESSIONS, targetValue = 3, requiresVerifiedOnly = true, orderIndex = 0),
                DungeonObjective(dungeonId = 0, title = "Pass 1 verified/recorded assessment (>=60%)", evidenceType = DungeonEvidenceType.ACADEMIC_ASSESSMENT_PASS, targetValue = 1, orderIndex = 1),
                DungeonObjective(dungeonId = 0, title = "Clear 1 goal milestone", evidenceType = DungeonEvidenceType.LIFE_GOAL_MILESTONES, targetValue = 1, orderIndex = 2)
            )
        ),
        DungeonSeedSpec(
            definition = DungeonDefinition(
                code = "sys_recovery_sanctuary",
                title = "Sanctuary Gate: Recovery & Reflection",
                description = "Safe wellbeing dungeon honoring recovery limits through routines, study organization, and protected rest.",
                category = DungeonCategory.RECOVERY_WELLBEING,
                difficulty = DungeonDifficulty.SPECIAL,
                requiredRank = HunterRank.E_RANK,
                requiredLevel = 1,
                timeLimitHours = 24,
                baseXpReward = 55,
                primaryAttribute = AttributeType.WIS,
                primaryAttrXp = 25,
                secondaryAttribute = AttributeType.VIT,
                secondaryAttrXp = 18,
                rewardItemId = "item_cosmetic_emerald_aura",
                isRepeatable = true
            ),
            objectives = listOf(
                DungeonObjective(dungeonId = 0, title = "Complete 2 routine steps or rest check-ins", evidenceType = DungeonEvidenceType.RECOVERY_REST_DAY, targetValue = 1, orderIndex = 0),
                DungeonObjective(dungeonId = 0, title = "Complete 20 minutes of gentle study or revision", evidenceType = DungeonEvidenceType.ACADEMIC_STUDY_MINUTES, targetValue = 20, orderIndex = 1)
            )
        ),
        DungeonSeedSpec(
            definition = DungeonDefinition(
                code = "sys_b_architect_rift",
                title = "Gate B-01: Sovereign Consistency Rift",
                description = "Sustained elite performance across physical conditioning, knowledge retention, and execution.",
                category = DungeonCategory.CONSISTENCY,
                difficulty = DungeonDifficulty.B_RANK,
                requiredRank = HunterRank.B_RANK,
                requiredLevel = 22,
                requiredAttributeCode = "DIS",
                requiredAttributeValue = 20,
                timeLimitHours = 168,
                baseXpReward = 170,
                primaryAttribute = AttributeType.DIS,
                primaryAttrXp = 55,
                secondaryAttribute = AttributeType.WIS,
                secondaryAttrXp = 35,
                rewardTitleId = "title_chronos_warden",
                rewardShadowId = "shadow_chronos_spire",
                isRepeatable = true
            ),
            objectives = listOf(
                DungeonObjective(dungeonId = 0, title = "Maintain 7+ day consistency streak", evidenceType = DungeonEvidenceType.PLAYER_STREAK_DAYS, targetValue = 7, orderIndex = 0),
                DungeonObjective(dungeonId = 0, title = "Log 180 minutes of timer-verified study", evidenceType = DungeonEvidenceType.ACADEMIC_STUDY_MINUTES, targetValue = 180, requiresVerifiedOnly = true, orderIndex = 1),
                DungeonObjective(dungeonId = 0, title = "Complete 4 verified physical sessions", evidenceType = DungeonEvidenceType.VERIFIED_PHYSICAL_SESSIONS, targetValue = 4, requiresVerifiedOnly = true, orderIndex = 2)
            )
        ),
        DungeonSeedSpec(
            definition = DungeonDefinition(
                code = "sys_a_red_nexus",
                title = "Gate A-01: Apex Crimson Nexus",
                description = "High-tier A-Rank challenge demanding verified physical output, completed chapters, and milestone execution.",
                category = DungeonCategory.MULTI_DOMAIN_CHALLENGE,
                difficulty = DungeonDifficulty.A_RANK,
                requiredRank = HunterRank.A_RANK,
                requiredLevel = 35,
                requiredAttributeCode = "STR",
                requiredAttributeValue = 26,
                timeLimitHours = 168,
                baseXpReward = 210,
                primaryAttribute = AttributeType.STR,
                primaryAttrXp = 65,
                secondaryAttribute = AttributeType.INT,
                secondaryAttrXp = 45,
                rewardTitleId = "title_apex_challenger",
                rewardItemId = "item_frame_monarch_gold",
                isRepeatable = true
            ),
            objectives = listOf(
                DungeonObjective(dungeonId = 0, title = "Complete 5 verified physical sessions", evidenceType = DungeonEvidenceType.VERIFIED_PHYSICAL_SESSIONS, targetValue = 5, requiresVerifiedOnly = true, orderIndex = 0),
                DungeonObjective(dungeonId = 0, title = "Clear 2 syllabus chapters", evidenceType = DungeonEvidenceType.ACADEMIC_CHAPTERS_COMPLETED, targetValue = 2, orderIndex = 1),
                DungeonObjective(dungeonId = 0, title = "Complete 2 goal milestones", evidenceType = DungeonEvidenceType.LIFE_GOAL_MILESTONES, targetValue = 2, orderIndex = 2)
            )
        ),
        DungeonSeedSpec(
            definition = DungeonDefinition(
                code = "sys_s_monarch_throne",
                title = "Gate S-01: Monarch's Citadel",
                description = "Pinnacle S-Rank multi-domain dungeon testing true mastery across verified fitness, scholarship, and discipline.",
                category = DungeonCategory.MULTI_DOMAIN_CHALLENGE,
                difficulty = DungeonDifficulty.S_RANK,
                requiredRank = HunterRank.S_RANK,
                requiredLevel = 50,
                requiredAttributeCode = "DIS",
                requiredAttributeValue = 35,
                timeLimitHours = 240,
                baseXpReward = 260,
                primaryAttribute = AttributeType.DIS,
                primaryAttrXp = 85,
                secondaryAttribute = AttributeType.CHA,
                secondaryAttrXp = 50,
                rewardTitleId = "title_shadow_sovereign",
                rewardShadowId = "shadow_apex_sovereign",
                isRepeatable = true
            ),
            objectives = listOf(
                DungeonObjective(dungeonId = 0, title = "Complete 6 verified physical sessions", evidenceType = DungeonEvidenceType.VERIFIED_PHYSICAL_SESSIONS, targetValue = 6, requiresVerifiedOnly = true, orderIndex = 0),
                DungeonObjective(dungeonId = 0, title = "Complete 240 minutes of timer-verified study", evidenceType = DungeonEvidenceType.ACADEMIC_STUDY_MINUTES, targetValue = 240, requiresVerifiedOnly = true, orderIndex = 1),
                DungeonObjective(dungeonId = 0, title = "Maintain 14+ day consistency streak", evidenceType = DungeonEvidenceType.PLAYER_STREAK_DAYS, targetValue = 14, orderIndex = 2)
            )
        )
    )

    fun checkEligibility(
        definition: DungeonDefinition,
        profile: PlayerProfile
    ): Pair<Boolean, List<String>> {
        val blockers = mutableListOf<String>()
        if (profile.rank.ordinal < definition.requiredRank.ordinal) {
            blockers.add("Requires ${definition.requiredRank.displayName} (current: ${profile.rank.displayName})")
        }
        if (profile.level < definition.requiredLevel) {
            blockers.add("Requires Level ${definition.requiredLevel} (current: Lv.${profile.level})")
        }
        if (!definition.requiredAttributeCode.isNullOrBlank() && definition.requiredAttributeValue > 0) {
            val attrVal = profile.attributes.asList()
                .find { it.type.code.equals(definition.requiredAttributeCode, ignoreCase = true) }
                ?.totalValue ?: 0
            if (attrVal < definition.requiredAttributeValue) {
                blockers.add("Requires ${definition.requiredAttributeCode} ${definition.requiredAttributeValue} (current: $attrVal)")
            }
        }
        return blockers.isEmpty() to blockers
    }

    fun evaluateObjective(
        objective: DungeonObjective,
        sinceEpochMs: Long,
        telemetry: PlayerTelemetrySnapshot
    ): DungeonObjectiveProgress {
        val windowStartMs = if (sinceEpochMs > 0L) sinceEpochMs else telemetry.nowEpochMs
        val windowStartDay = Instant.ofEpochMilli(windowStartMs).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()

        val (current, note) = when (objective.evidenceType) {
            DungeonEvidenceType.PHYSICAL_JOG_MINUTES -> {
                val eligible = telemetry.jogs.filter {
                    it.endEpochMs >= windowStartMs && (!objective.requiresVerifiedOnly || it.verificationStatus.isVerified)
                }
                val sum = eligible.sumOf { it.durationMinutes }
                val label = if (objective.requiresVerifiedOnly) "Verified Health Connect minutes" else "Logged/verified jogging minutes"
                sum to "$label since gate entry: $sum min"
            }
            DungeonEvidenceType.PHYSICAL_WORKOUT_SESSIONS -> {
                val eligible = telemetry.workouts.count {
                    it.endEpochMs >= windowStartMs && (!objective.requiresVerifiedOnly || it.verificationStatus.isVerified)
                }
                eligible to "Workouts since entry: $eligible"
            }
            DungeonEvidenceType.VERIFIED_PHYSICAL_SESSIONS -> {
                val vJogs = telemetry.jogs.count { it.endEpochMs >= windowStartMs && it.verificationStatus.isVerified }
                val vWorkouts = telemetry.workouts.count { it.endEpochMs >= windowStartMs && it.verificationStatus.isVerified }
                val total = vJogs + vWorkouts
                total to "Verified Health Connect sessions since entry: $total"
            }
            DungeonEvidenceType.ACADEMIC_STUDY_MINUTES -> {
                val mins = telemetry.studySessions.filter {
                    it.endEpochMs >= windowStartMs && (!objective.requiresVerifiedOnly || it.verificationStatus.isVerified)
                }.sumOf { it.durationMinutes }
                val tag = if (objective.requiresVerifiedOnly) "Timer-verified study" else "Total study"
                mins to "$tag since entry: $mins min"
            }
            DungeonEvidenceType.ACADEMIC_CHAPTERS_COMPLETED -> {
                val count = telemetry.chapters.count { it.isCompleted && (it.completedAtEpochMs ?: 0L) >= windowStartMs }
                count to "Chapters completed since entry: $count"
            }
            DungeonEvidenceType.ACADEMIC_REVISIONS_COMPLETED -> {
                val count = telemetry.revisions.count { it.revisedAtEpochMs >= windowStartMs }
                count to "Revisions logged since entry: $count"
            }
            DungeonEvidenceType.ACADEMIC_ASSESSMENT_PASS -> {
                val count = telemetry.assessments.count {
                    it.timestampEpochMs >= windowStartMs &&
                        it.normalizedRatio >= 0.60f &&
                        (!objective.requiresVerifiedOnly || it.verificationStatus.isVerified)
                }
                count to "Qualifying assessments (>=60%) since entry: $count"
            }
            DungeonEvidenceType.LIFE_HABIT_COMPLETIONS -> {
                val count = telemetry.habitCompletions.count {
                    it.status == HabitDayStatus.COMPLETED && it.dateEpochDay >= windowStartDay
                }
                count to "Habits completed since gate start: $count"
            }
            DungeonEvidenceType.LIFE_ROUTINE_COMPLETIONS -> {
                val count = telemetry.routineCompletions.count {
                    it.status == RoutineActivityStatus.COMPLETED && it.dateEpochDay >= windowStartDay
                }
                count to "Routine steps completed since gate start: $count"
            }
            DungeonEvidenceType.LIFE_TASK_COMPLETIONS -> {
                val count = telemetry.completedTasks.count {
                    it.status == PersonalTaskStatus.COMPLETED && (it.completedAtEpochMs ?: 0L) >= windowStartMs
                }
                count to "Personal tasks completed since entry: $count"
            }
            DungeonEvidenceType.LIFE_GOAL_MILESTONES -> {
                val count = telemetry.completedMilestones.count {
                    it.isCompleted && (it.completedAtEpochMs ?: 0L) >= windowStartMs
                }
                count to "Goal milestones reached since entry: $count"
            }
            DungeonEvidenceType.PLAYER_STREAK_DAYS -> {
                val streak = telemetry.profile.streakDays
                streak to "Current verified/consistent streak: $streak day(s)"
            }
            DungeonEvidenceType.RECOVERY_REST_DAY -> {
                val routineSteps = telemetry.routineCompletions.count {
                    it.status == RoutineActivityStatus.COMPLETED && it.dateEpochDay >= windowStartDay
                }
                val restChecks = (telemetry.exemptRestDaysCount + if (routineSteps >= 1) 1 else 0).coerceAtLeast(
                    if (telemetry.recoveryState == RecoveryState.REST_RECOMMENDED && routineSteps >= 1) 1 else 0
                )
                restChecks to "Recovery & routine balance checks: $restChecks"
            }
        }

        val cappedCurrent = current.coerceAtLeast(0)
        return DungeonObjectiveProgress(
            objective = objective,
            currentValue = cappedCurrent,
            targetValue = objective.targetValue.coerceAtLeast(1),
            isSatisfied = cappedCurrent >= objective.targetValue.coerceAtLeast(1),
            verificationNote = note
        )
    }

    fun evaluateDungeonWithStatus(
        definition: DungeonDefinition,
        objectives: List<DungeonObjective>,
        latestInstance: DungeonInstance?,
        completedCount: Int,
        telemetry: PlayerTelemetrySnapshot
    ): DungeonWithStatus {
        val (eligible, blockers) = checkEligibility(definition, telemetry.profile)
        val anchorEpochMs = when {
            latestInstance != null && (latestInstance.status == DungeonStatus.ACTIVE || latestInstance.status == DungeonStatus.PAUSED) ->
                latestInstance.startedAtEpochMs
            latestInstance?.status == DungeonStatus.COMPLETED && !definition.isRepeatable ->
                latestInstance.startedAtEpochMs
            else -> telemetry.nowEpochMs
        }

        val progresses = objectives.sortedBy { it.orderIndex }.map { obj ->
            if (latestInstance != null && (latestInstance.status == DungeonStatus.ACTIVE || latestInstance.status == DungeonStatus.PAUSED || latestInstance.status == DungeonStatus.COMPLETED)) {
                evaluateObjective(obj, anchorEpochMs, telemetry)
            } else {
                DungeonObjectiveProgress(
                    objective = obj,
                    currentValue = 0,
                    targetValue = obj.targetValue,
                    isSatisfied = false,
                    verificationNote = "Enter gate to begin real-time objective tracking."
                )
            }
        }
        val overallFraction = if (progresses.isEmpty()) 0f else progresses.map { it.progressFraction }.average().toFloat()
        return DungeonWithStatus(
            definition = definition,
            objectives = objectives,
            latestInstance = latestInstance,
            isEligibleToEnter = eligible,
            eligibilityBlockers = blockers,
            objectiveProgress = progresses,
            overallProgressFraction = overallFraction,
            completedCount = completedCount
        )
    }

    fun validateCompletion(
        withStatus: DungeonWithStatus,
        instance: DungeonInstance,
        telemetry: PlayerTelemetrySnapshot
    ): DungeonValidationResult {
        if (instance.status != DungeonStatus.ACTIVE) {
            return DungeonValidationResult(
                canComplete = false,
                evidenceSummary = "",
                verificationProvenance = "",
                hasVerifiedEvidence = false,
                failureReason = "Only ACTIVE dungeons can be cleared (current status: ${instance.status.label})."
            )
        }
        val effectiveDeadline = instance.deadlineEpochMs + instance.accumulatedPausedMs
        if (instance.deadlineEpochMs > 0L && telemetry.nowEpochMs > effectiveDeadline) {
            return DungeonValidationResult(
                canComplete = false,
                evidenceSummary = "",
                verificationProvenance = "",
                hasVerifiedEvidence = false,
                failureReason = "Dungeon gate time limit expired. Abandon and re-enter a fresh gate."
            )
        }
        val progresses = withStatus.objectiveProgress
        if (progresses.isEmpty()) {
            return DungeonValidationResult(
                canComplete = false,
                evidenceSummary = "",
                verificationProvenance = "",
                hasVerifiedEvidence = false,
                failureReason = "Dungeon has no verifiable objectives."
            )
        }
        val unmet = progresses.filter { !it.isSatisfied }
        if (unmet.isNotEmpty()) {
            val missingText = unmet.joinToString("; ") { "${it.objective.title} (${it.currentValue}/${it.targetValue})" }
            return DungeonValidationResult(
                canComplete = false,
                evidenceSummary = "",
                verificationProvenance = "",
                hasVerifiedEvidence = false,
                failureReason = "Objectives incomplete: $missingText"
            )
        }

        val hasVerified = progresses.any { p ->
            when (p.objective.evidenceType) {
                DungeonEvidenceType.VERIFIED_PHYSICAL_SESSIONS -> true
                DungeonEvidenceType.PHYSICAL_JOG_MINUTES ->
                    telemetry.jogs.any { it.endEpochMs >= instance.startedAtEpochMs && it.verificationStatus.isVerified }
                DungeonEvidenceType.PHYSICAL_WORKOUT_SESSIONS ->
                    telemetry.workouts.any { it.endEpochMs >= instance.startedAtEpochMs && it.verificationStatus.isVerified }
                DungeonEvidenceType.ACADEMIC_STUDY_MINUTES ->
                    telemetry.studySessions.any { it.endEpochMs >= instance.startedAtEpochMs && it.verificationStatus.isVerified }
                DungeonEvidenceType.ACADEMIC_ASSESSMENT_PASS ->
                    telemetry.assessments.any { it.timestampEpochMs >= instance.startedAtEpochMs && it.verificationStatus.isVerified }
                else -> p.objective.requiresVerifiedOnly
            }
        }
        val provenance = if (hasVerified) "Verified Telemetry Included" else "Logged Real Records"
        val summary = progresses.joinToString(" | ") { "${it.objective.title}: ${it.currentValue}/${it.targetValue}" }

        return DungeonValidationResult(
            canComplete = true,
            evidenceSummary = summary,
            verificationProvenance = provenance,
            hasVerifiedEvidence = hasVerified
        )
    }

    /**
     * Deterministically creates adaptive Daily & Weekly dungeon specs matched to current rank,
     * recovery state, and active workload without punishing skipped/missed cycles.
     */
    fun buildAdaptiveDailyAndWeeklyDungeons(
        telemetry: PlayerTelemetrySnapshot,
        existingCodes: Set<String>
    ): List<DungeonSeedSpec> {
        val specs = mutableListOf<DungeonSeedSpec>()
        val today = LocalDate.ofEpochDay(telemetry.todayEpochDay)
        val weekFields = WeekFields.of(Locale.getDefault())
        val weekNum = today.get(weekFields.weekOfWeekBasedYear())
        val weekYear = today.get(weekFields.weekBasedYear())

        val dailyCode = "daily_gate_${telemetry.todayEpochDay}"
        if (dailyCode !in existingCodes) {
            val workloadScale = if (telemetry.activeWorkloadCount >= 10) 0.75f else 1.0f
            val recoveryLimited = telemetry.recoveryState == RecoveryState.REST_RECOMMENDED ||
                telemetry.recoveryState == RecoveryState.RECOVERY_RECOMMENDED

            val (category, title, desc, objectives, primaryAttr) = if (recoveryLimited) {
                DailyBlueprint(
                    category = DungeonCategory.RECOVERY_WELLBEING,
                    title = "Daily Recovery & Focus Gate",
                    desc = "Adaptive daily gate respecting elevated fatigue: calm focus study and routine consistency without intense physical load.",
                    objectives = listOf(
                        DungeonObjective(dungeonId = 0, title = "Study for ${(25 * workloadScale).roundToInt().coerceAtLeast(15)} min", evidenceType = DungeonEvidenceType.ACADEMIC_STUDY_MINUTES, targetValue = (25 * workloadScale).roundToInt().coerceAtLeast(15), orderIndex = 0),
                        DungeonObjective(dungeonId = 0, title = "Complete 1 routine step or protected rest", evidenceType = DungeonEvidenceType.RECOVERY_REST_DAY, targetValue = 1, orderIndex = 1)
                    ),
                    primaryAttr = AttributeType.WIS
                )
            } else {
                val jogTarget = ((20 + telemetry.profile.rank.ordinal * 5) * workloadScale).roundToInt().coerceIn(15, 45)
                val studyTarget = ((30 + telemetry.profile.rank.ordinal * 5) * workloadScale).roundToInt().coerceIn(20, 60)
                DailyBlueprint(
                    category = DungeonCategory.MULTI_DOMAIN_CHALLENGE,
                    title = "Daily Ascension Gate (${today})",
                    desc = "Adaptive daily challenge scaled to ${telemetry.profile.rank.displayName} and current workload.",
                    objectives = listOf(
                        DungeonObjective(dungeonId = 0, title = "Log $jogTarget min aerobic movement", evidenceType = DungeonEvidenceType.PHYSICAL_JOG_MINUTES, targetValue = jogTarget, orderIndex = 0),
                        DungeonObjective(dungeonId = 0, title = "Log $studyTarget min focused study", evidenceType = DungeonEvidenceType.ACADEMIC_STUDY_MINUTES, targetValue = studyTarget, orderIndex = 1),
                        DungeonObjective(dungeonId = 0, title = "Complete 1 daily habit", evidenceType = DungeonEvidenceType.LIFE_HABIT_COMPLETIONS, targetValue = 1, orderIndex = 2)
                    ),
                    primaryAttr = AttributeType.DIS
                )
            }

            specs.add(
                DungeonSeedSpec(
                    definition = DungeonDefinition(
                        code = dailyCode,
                        title = title,
                        description = desc,
                        category = category,
                        difficulty = DungeonDifficulty.E_RANK,
                        requiredRank = HunterRank.E_RANK,
                        requiredLevel = 1,
                        timeLimitHours = 24,
                        baseXpReward = 60,
                        primaryAttribute = primaryAttr,
                        primaryAttrXp = 26,
                        secondaryAttribute = AttributeType.VIT,
                        secondaryAttrXp = 14,
                        isRepeatable = false,
                        creationSource = DungeonCreationSource.DAILY_GENERATED
                    ),
                    objectives = objectives
                )
            )
        }

        val weeklyCode = "weekly_gate_${weekYear}_w${weekNum}"
        if (weeklyCode !in existingCodes) {
            val isRestPriority = telemetry.recoveryState == RecoveryState.REST_RECOMMENDED
            val workoutTarget = if (isRestPriority) 1 else (2 + telemetry.profile.rank.ordinal.coerceAtMost(2))
            val studyTarget = (90 + telemetry.profile.rank.ordinal * 30).coerceIn(90, 240)
            specs.add(
                DungeonSeedSpec(
                    definition = DungeonDefinition(
                        code = weeklyCode,
                        title = "Weekly Citadel Gate (W$weekNum)",
                        description = "Weekly multi-domain raid integrating training consistency, study volume, and task completion.",
                        category = DungeonCategory.CONSISTENCY,
                        difficulty = if (telemetry.profile.rank.ordinal >= 1) DungeonDifficulty.D_RANK else DungeonDifficulty.E_RANK,
                        requiredRank = HunterRank.E_RANK,
                        requiredLevel = 1,
                        timeLimitHours = 168,
                        baseXpReward = 115,
                        primaryAttribute = AttributeType.DIS,
                        primaryAttrXp = 42,
                        secondaryAttribute = AttributeType.INT,
                        secondaryAttrXp = 28,
                        rewardItemId = "item_badge_weekly_conqueror",
                        isRepeatable = false,
                        creationSource = DungeonCreationSource.WEEKLY_GENERATED
                    ),
                    objectives = listOf(
                        DungeonObjective(dungeonId = 0, title = "Complete $workoutTarget structured workout(s)", evidenceType = DungeonEvidenceType.PHYSICAL_WORKOUT_SESSIONS, targetValue = workoutTarget, orderIndex = 0),
                        DungeonObjective(dungeonId = 0, title = "Accumulate $studyTarget study minutes", evidenceType = DungeonEvidenceType.ACADEMIC_STUDY_MINUTES, targetValue = studyTarget, orderIndex = 1),
                        DungeonObjective(dungeonId = 0, title = "Complete 3 personal tasks", evidenceType = DungeonEvidenceType.LIFE_TASK_COMPLETIONS, targetValue = 3, orderIndex = 2)
                    )
                )
            )
        }
        return specs
    }

    private data class DailyBlueprint(
        val category: DungeonCategory,
        val title: String,
        val desc: String,
        val objectives: List<DungeonObjective>,
        val primaryAttr: AttributeType
    )
}

/**
 * Deterministic Multi-Stage Boss Battle Engine.
 * Boss health bars diminish strictly through verified/qualifying phase completion progress.
 */
object BossBattleEngine {

    fun defaultBossCatalog(): List<BossSeedSpec> = listOf(
        BossSeedSpec(
            definition = BossBattleDefinition(
                code = "boss_academic_sentinel",
                title = "Monolith of Knowledge",
                subtitle = "Academic Module Mastery Campaign",
                description = "Conquer a full study campaign through deep focus, syllabus completion, spaced revisions, and verified assessment accuracy.",
                category = DungeonCategory.ACADEMIC,
                difficulty = DungeonDifficulty.E_RANK,
                requiredRank = HunterRank.E_RANK,
                requiredLevel = 1,
                xpReward = 160,
                primaryAttribute = AttributeType.INT,
                primaryAttrXp = 55,
                secondaryAttribute = AttributeType.WIS,
                secondaryAttrXp = 35,
                rewardTitleId = "title_monolith_breaker",
                rewardShadowId = "shadow_astral_prism",
                totalEstimatedDays = 10
            ),
            phases = listOf(
                BossPhase(bossId = 0, phaseNumber = 1, title = "Phase I: Focus Foundation", description = "Complete 90 minutes of focused study", evidenceType = DungeonEvidenceType.ACADEMIC_STUDY_MINUTES, targetValue = 90, requiresVerifiedOnly = false, weight = 25),
                BossPhase(bossId = 0, phaseNumber = 2, title = "Phase II: Syllabus Penetration", description = "Complete 2 syllabus chapters", evidenceType = DungeonEvidenceType.ACADEMIC_CHAPTERS_COMPLETED, targetValue = 2, requiresVerifiedOnly = false, weight = 25),
                BossPhase(bossId = 0, phaseNumber = 3, title = "Phase III: Memory Consolidation", description = "Execute 3 spaced revision sessions", evidenceType = DungeonEvidenceType.ACADEMIC_REVISIONS_COMPLETED, targetValue = 3, requiresVerifiedOnly = false, weight = 25),
                BossPhase(bossId = 0, phaseNumber = 4, title = "Phase IV: Examination Strike", description = "Score >=60% on 1 assessment", evidenceType = DungeonEvidenceType.ACADEMIC_ASSESSMENT_PASS, targetValue = 1, requiresVerifiedOnly = false, weight = 25)
            )
        ),
        BossSeedSpec(
            definition = BossBattleDefinition(
                code = "boss_discipline_warden",
                title = "Iron Consistency Warden",
                subtitle = "Routine & Habit Discipline Campaign",
                description = "Dismantle procrastination by sustaining daily habits, structured routines, and personal task execution.",
                category = DungeonCategory.DISCIPLINE,
                difficulty = DungeonDifficulty.E_RANK,
                requiredRank = HunterRank.E_RANK,
                requiredLevel = 1,
                xpReward = 150,
                primaryAttribute = AttributeType.DIS,
                primaryAttrXp = 55,
                secondaryAttribute = AttributeType.CHA,
                secondaryAttrXp = 25,
                rewardTitleId = "title_iron_will",
                rewardItemId = "item_badge_warden_sigil",
                totalEstimatedDays = 14
            ),
            phases = listOf(
                BossPhase(bossId = 0, phaseNumber = 1, title = "Phase I: Habit Anchor", description = "Complete 5 habit check-ins", evidenceType = DungeonEvidenceType.LIFE_HABIT_COMPLETIONS, targetValue = 5, weight = 30),
                BossPhase(bossId = 0, phaseNumber = 2, title = "Phase II: Routine Cadence", description = "Complete 5 routine steps", evidenceType = DungeonEvidenceType.LIFE_ROUTINE_COMPLETIONS, targetValue = 5, weight = 30),
                BossPhase(bossId = 0, phaseNumber = 3, title = "Phase III: Task Execution", description = "Clear 3 personal tasks", evidenceType = DungeonEvidenceType.LIFE_TASK_COMPLETIONS, targetValue = 3, weight = 20),
                BossPhase(bossId = 0, phaseNumber = 4, title = "Phase IV: Streak Lock", description = "Reach a 5-day streak", evidenceType = DungeonEvidenceType.PLAYER_STREAK_DAYS, targetValue = 5, weight = 20)
            )
        ),
        BossSeedSpec(
            definition = BossBattleDefinition(
                code = "boss_endurance_colossus",
                title = "Colossus of Endurance",
                subtitle = "Verified Physical Conditioning Raid",
                description = "A rank-gated physical endurance campaign requiring Health Connect verified sessions and aerobic persistence.",
                category = DungeonCategory.PHYSICAL,
                difficulty = DungeonDifficulty.D_RANK,
                requiredRank = HunterRank.D_RANK,
                requiredLevel = 5,
                xpReward = 210,
                primaryAttribute = AttributeType.VIT,
                primaryAttrXp = 65,
                secondaryAttribute = AttributeType.STR,
                secondaryAttrXp = 45,
                rewardTitleId = "title_endurance_hunter",
                rewardShadowId = "shadow_obsidian_sentinel",
                totalEstimatedDays = 14
            ),
            phases = listOf(
                BossPhase(bossId = 0, phaseNumber = 1, title = "Phase I: Verified Awakening", description = "Complete 3 verified physical sessions", evidenceType = DungeonEvidenceType.VERIFIED_PHYSICAL_SESSIONS, targetValue = 3, requiresVerifiedOnly = true, weight = 40),
                BossPhase(bossId = 0, phaseNumber = 2, title = "Phase II: Aerobic Surge", description = "Accumulate 90 jogging minutes", evidenceType = DungeonEvidenceType.PHYSICAL_JOG_MINUTES, targetValue = 90, requiresVerifiedOnly = false, weight = 30),
                BossPhase(bossId = 0, phaseNumber = 3, title = "Phase III: Strength Trial", description = "Complete 2 structured workouts", evidenceType = DungeonEvidenceType.PHYSICAL_WORKOUT_SESSIONS, targetValue = 2, requiresVerifiedOnly = false, weight = 30)
            )
        ),
        BossSeedSpec(
            definition = BossBattleDefinition(
                code = "boss_sovereign_crucible",
                title = "Sovereign Crucible",
                subtitle = "Elite Multi-Domain Mastery Campaign",
                description = "Master-tier campaign uniting verified physical training, timer-verified study, assessments, and life goals.",
                category = DungeonCategory.MULTI_DOMAIN_CHALLENGE,
                difficulty = DungeonDifficulty.C_RANK,
                requiredRank = HunterRank.C_RANK,
                requiredLevel = 12,
                xpReward = 280,
                primaryAttribute = AttributeType.DIS,
                primaryAttrXp = 85,
                secondaryAttribute = AttributeType.WIS,
                secondaryAttrXp = 55,
                rewardTitleId = "title_crucible_monarch",
                rewardShadowId = "shadow_vanguard_construct",
                totalEstimatedDays = 21
            ),
            phases = listOf(
                BossPhase(bossId = 0, phaseNumber = 1, title = "Phase I: Deep Cognitive Load", description = "Log 240 timer-verified study minutes", evidenceType = DungeonEvidenceType.ACADEMIC_STUDY_MINUTES, targetValue = 240, requiresVerifiedOnly = true, weight = 25),
                BossPhase(bossId = 0, phaseNumber = 2, title = "Phase II: Physical Proof", description = "Complete 4 verified physical sessions", evidenceType = DungeonEvidenceType.VERIFIED_PHYSICAL_SESSIONS, targetValue = 4, requiresVerifiedOnly = true, weight = 25),
                BossPhase(bossId = 0, phaseNumber = 3, title = "Phase III: Goal Milestones", description = "Clear 3 goal milestones", evidenceType = DungeonEvidenceType.LIFE_GOAL_MILESTONES, targetValue = 3, weight = 25),
                BossPhase(bossId = 0, phaseNumber = 4, title = "Phase IV: Assessment Mastery", description = "Pass 2 assessments (>=60%)", evidenceType = DungeonEvidenceType.ACADEMIC_ASSESSMENT_PASS, targetValue = 2, weight = 25)
            )
        )
    )

    fun checkEligibility(
        definition: BossBattleDefinition,
        profile: PlayerProfile
    ): Pair<Boolean, List<String>> {
        val blockers = mutableListOf<String>()
        if (profile.rank.ordinal < definition.requiredRank.ordinal) {
            blockers.add("Requires ${definition.requiredRank.displayName} (current: ${profile.rank.displayName})")
        }
        if (profile.level < definition.requiredLevel) {
            blockers.add("Requires Level ${definition.requiredLevel} (current: Lv.${profile.level})")
        }
        return blockers.isEmpty() to blockers
    }

    fun evaluateBossWithPhases(
        definition: BossBattleDefinition,
        phases: List<BossPhase>,
        latestInstance: BossBattleInstance?,
        telemetry: PlayerTelemetrySnapshot
    ): BossBattleWithPhases {
        val (eligible, blockers) = checkEligibility(definition, telemetry.profile)
        val orderedPhases = phases.sortedBy { it.phaseNumber }

        if (latestInstance == null || latestInstance.status == BossStatus.ABANDONED) {
            return BossBattleWithPhases(
                definition = definition,
                phases = orderedPhases,
                latestInstance = latestInstance,
                phaseProgress = orderedPhases.map {
                    BossPhaseProgress(it, currentValue = 0, isCompleted = false, verificationNote = "Start battle to track phase progress.")
                },
                isEligible = eligible,
                eligibilityBlockers = blockers,
                healthRemainingFraction = 1f,
                healthRemainingPercent = 100
            )
        }

        val phaseProgresses = orderedPhases.map { phase ->
            val obj = DungeonObjective(
                dungeonId = 0L,
                title = phase.title,
                evidenceType = phase.evidenceType,
                targetValue = phase.targetValue,
                requiresVerifiedOnly = phase.requiresVerifiedOnly,
                orderIndex = phase.phaseNumber
            )
            val evaluated = DungeonEngine.evaluateObjective(obj, latestInstance.startedAtEpochMs, telemetry)
            BossPhaseProgress(
                phase = phase,
                currentValue = evaluated.currentValue,
                isCompleted = evaluated.isSatisfied,
                verificationNote = evaluated.verificationNote
            )
        }

        val totalWeight = orderedPhases.sumOf { it.weight.coerceAtLeast(1) }.coerceAtLeast(1)
        val completedWeight = phaseProgresses.sumOf { pp ->
            (pp.phase.weight.coerceAtLeast(1) * pp.progressFraction).toDouble()
        }.toFloat()

        val remainingFraction = if (latestInstance.status == BossStatus.COMPLETED) {
            0f
        } else {
            (1f - (completedWeight / totalWeight.toFloat())).coerceIn(0f, 1f)
        }
        val remainingPercent = (remainingFraction * 100f).roundToInt().coerceIn(0, 100)

        return BossBattleWithPhases(
            definition = definition,
            phases = orderedPhases,
            latestInstance = latestInstance,
            phaseProgress = phaseProgresses,
            isEligible = eligible,
            eligibilityBlockers = blockers,
            healthRemainingFraction = remainingFraction,
            healthRemainingPercent = remainingPercent
        )
    }
}
