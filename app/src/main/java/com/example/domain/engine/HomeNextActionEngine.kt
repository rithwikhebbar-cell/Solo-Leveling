package com.example.domain.engine

import com.example.domain.model.AcademicDashboardSummary
import com.example.domain.model.LifeDashboardSummary
import com.example.domain.model.PhysicalDevelopmentState
import com.example.domain.model.Quest
import com.example.domain.model.QuestType
import com.example.domain.model.RecoveryState
import com.example.domain.model.UnifiedMissionItem
import com.example.domain.model.WorkoutStatus

data class HomeNextAction(
    val id: String,
    val category: String,
    val title: String,
    val description: String,
    val rationale: String,
    val actionLabel: String,
    val destinationRoute: String,
    val priorityScore: Int,
    val isSafetyOverride: Boolean = false,
    val isSessionContinuation: Boolean = false
)

object HomeNextActionEngine {

    fun evaluate(
        physicalState: PhysicalDevelopmentState,
        quests: List<Quest>,
        academicSummary: AcademicDashboardSummary,
        lifeSummary: LifeDashboardSummary,
        topUnblockedMission: UnifiedMissionItem? = null
    ): HomeNextAction {
        // 1. ACTIVE RUNNING SESSION (Highest priority — must offer CONTINUE)
        val activeJog = physicalState.activeJoggingSession
        if (activeJog != null) {
            return HomeNextAction(
                id = "continue_active_jog",
                category = "PHYSICAL",
                title = "Aerobic Conditioning In Progress",
                description = "Active jogging session is running. Resume tracking your pace and distance.",
                rationale = "An uncommitted jogging session is currently recorded by the system.",
                actionLabel = "CONTINUE JOGGING",
                destinationRoute = "physical",
                priorityScore = 100,
                isSessionContinuation = true
            )
        }

        if (physicalState.workoutStatus == WorkoutStatus.IN_PROGRESS) {
            return HomeNextAction(
                id = "continue_active_workout",
                category = "PHYSICAL",
                title = "Training Workout In Progress",
                description = "Active strength workout is ongoing. Conclude remaining sets to claim rewards.",
                rationale = "Today's scheduled workout was started but not yet finalized.",
                actionLabel = "CONTINUE WORKOUT",
                destinationRoute = "physical",
                priorityScore = 100,
                isSessionContinuation = true
            )
        }

        // 2. RECOVERY REST SAFETY OVERRIDE (Must NEVER recommend starting workout)
        val recovery = physicalState.recoveryAssessment
        val isRestRecommended = recovery.state == RecoveryState.REST_RECOMMENDED ||
            recovery.state == RecoveryState.RECOVERY_RECOMMENDED ||
            recovery.readinessScore <= 45

        if (isRestRecommended) {
            return HomeNextAction(
                id = "prioritize_recovery_rest",
                category = "RECOVERY",
                title = "Prioritize Physiological Recovery",
                description = "Readiness score is ${recovery.readinessScore}/100 (${recovery.state.label}). Focus on restorative sleep, hydration, and light mobility today.",
                rationale = "Prevents systemic fatigue accumulation and preserves streak protection without penalty.",
                actionLabel = "VIEW RECOVERY",
                destinationRoute = "physical",
                priorityScore = 95,
                isSafetyOverride = true
            )
        }

        // 3. OVERDUE ACADEMIC REVISIONS
        if (academicSummary.overdueRevisionsCount > 0) {
            return HomeNextAction(
                id = "clear_overdue_revisions",
                category = "ACADEMIC",
                title = "Clear ${academicSummary.overdueRevisionsCount} Overdue Revision(s)",
                description = "Spaced repetition intervals have matured. Complete a focused review session to reinforce topic mastery.",
                rationale = "Prevents memory decay on core subjects and earns verified INT / WIS XP.",
                actionLabel = "OPEN REVISIONS",
                destinationRoute = "academic",
                priorityScore = 85
            )
        }

        // 4. HIGHEST-PRIORITY MISSION CONTROL DIRECTIVE (Unblocked & Recovery-Safe)
        if (topUnblockedMission != null && !topUnblockedMission.isBlocked && !topUnblockedMission.isRecoveryRestricted) {
            return HomeNextAction(
                id = "mission_${topUnblockedMission.missionId}",
                category = topUnblockedMission.module.label.uppercase(),
                title = topUnblockedMission.title,
                description = topUnblockedMission.description.ifBlank {
                    "Progress: ${topUnblockedMission.currentValue}/${topUnblockedMission.targetValue} ${topUnblockedMission.unit}"
                },
                rationale = topUnblockedMission.priorityReason,
                actionLabel = "OPEN MISSION",
                destinationRoute = "mission_control",
                priorityScore = topUnblockedMission.priorityScore.coerceIn(75, 92)
            )
        }

        // 5. INCOMPLETE DAILY QUEST / PROTOCOL
        val dailyQuests = quests.filter { it.type == QuestType.DAILY }
        val primaryPendingDaily = dailyQuests.firstOrNull { !it.isCompleted }
        if (primaryPendingDaily != null) {
            return HomeNextAction(
                id = "complete_daily_quest_${primaryPendingDaily.id}",
                category = "MISSION",
                title = "Execute Daily Protocol: ${primaryPendingDaily.title}",
                description = primaryPendingDaily.description.ifBlank {
                    "Advance your daily objective (${primaryPendingDaily.currentValue}/${primaryPendingDaily.targetValue} ${primaryPendingDaily.unit})."
                },
                rationale = "Daily directives reset at midnight. Fulfilling them averts Penalty Quests.",
                actionLabel = "OPEN MISSION",
                destinationRoute = "quests",
                priorityScore = 80
            )
        }

        // 5. OPTIMAL READINESS — START SCHEDULED WORKOUT
        val isWorkoutCompleted = physicalState.workoutStatus == WorkoutStatus.COMPLETED
        if (!isWorkoutCompleted && (recovery.state == RecoveryState.READY || recovery.readinessScore >= 70)) {
            val workoutTitle = physicalState.todayWorkout?.title ?: "Today's Conditioning Workout"
            return HomeNextAction(
                id = "start_scheduled_workout",
                category = "PHYSICAL",
                title = "Execute: $workoutTitle",
                description = "Readiness is optimal (${recovery.readinessScore}/100). Log your planned training for verified attribute progression.",
                rationale = "High physical readiness enables maximum training adaptation without injury risk.",
                actionLabel = "START WORKOUT",
                destinationRoute = "physical",
                priorityScore = 75
            )
        }

        // 6. LIFE & DISCIPLINE ROUTINES
        if (lifeSummary.overdueTasksCount > 0) {
            return HomeNextAction(
                id = "clear_overdue_tasks",
                category = "DISCIPLINE",
                title = "Resolve ${lifeSummary.overdueTasksCount} Overdue Task(s)",
                description = "Pending personal tasks require attention. Clear overdue items to maintain your discipline rating.",
                rationale = "Directly improves your life discipline score and preserves active streaks.",
                actionLabel = "OPEN LIFE HUB",
                destinationRoute = "life",
                priorityScore = 70
            )
        }

        val remainingHabits = (lifeSummary.activeHabitsCount - lifeSummary.completedHabitsToday).coerceAtLeast(0)
        if (remainingHabits > 0) {
            return HomeNextAction(
                id = "log_daily_habits",
                category = "DISCIPLINE",
                title = "Log Daily Habits ($remainingHabits remaining)",
                description = "${lifeSummary.completedHabitsToday} of ${lifeSummary.activeHabitsCount} habits logged today. Maintain consistency to boost Discipline score.",
                rationale = "Consistent habit execution reinforces Discipline (DIS) attributes.",
                actionLabel = "LOG HABITS",
                destinationRoute = "life",
                priorityScore = 65
            )
        }

        // 7. ALL CORE OBJECTIVES CLEARED — STEADY STATE
        return HomeNextAction(
            id = "all_objectives_cleared",
            category = "STATUS",
            title = "All Directives Fulfilled",
            description = "Today's primary missions and conditioning are accomplished. Inspect player evolution or explore the Shadow Realm.",
            rationale = "Zero pending penalties, all daily protocols satisfied.",
            actionLabel = "INSPECT STATUS",
            destinationRoute = "status",
            priorityScore = 50
        )
    }
}
