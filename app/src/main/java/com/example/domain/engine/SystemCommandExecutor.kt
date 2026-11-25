package com.example.domain.engine

import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.entity.toDomain
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.preferences.VoiceCommandPermissions
import com.example.data.repository.AcademicRepository
import com.example.data.repository.AiSystemRepository
import com.example.data.repository.LifeManagementRepository
import com.example.data.repository.RealityOsRepository
import com.example.data.repository.ShadowArmyRepository
import com.example.domain.model.AcademicQuestCategory
import com.example.domain.model.AcademicQuestDifficulty
import com.example.domain.model.DEFAULT_SHADOW_ACCOUNT_OWNER_ID
import com.example.domain.model.HabitDayStatus
import com.example.domain.model.PersonalTaskStatus
import com.example.domain.model.PhysicalDevelopmentState
import com.example.domain.model.PlayerProfile
import com.example.domain.model.RecoveryState
import com.example.domain.model.TaskCategory
import com.example.domain.model.TaskPriority
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

enum class CommandExecutionStatus(val label: String) {
    EXECUTED("Executed"),
    AWAITING_CONFIRMATION("Confirmation Required"),
    NEEDS_CLARIFICATION("Clarification Required"),
    PERMISSION_DENIED("Permission Disabled"),
    BLOCKED_BY_SAFETY("Blocked by Safety Guard"),
    REJECTED("Rejected"),
    DUPLICATE_IGNORED("Duplicate Ignored"),
    FAILED("Execution Failed")
}

data class CommandExecutionResult(
    val status: CommandExecutionStatus,
    val commandId: SystemCommandId? = null,
    val category: VoiceCommandCategory? = null,
    val commandLabel: String = "Unrecognized",
    val spokenResponse: String,
    val displayDetails: String = spokenResponse,
    val deepLinkRoute: String? = null,
    val pendingIntent: StructuredVoiceIntent? = null,
    val source: IntentResolutionSource = IntentResolutionSource.OFFLINE_LOCAL
)

/**
 * Deterministic, permission-checked executor for validated StructuredVoiceIntent commands.
 *
 * Security & Integrity Guarantees:
 * - Never directly mutates XP, rank, 7 attributes, verified workouts, recovery state, achievements, or shadows.
 * - All mutations delegate strictly to existing domain repositories (LifeManagementRepository, AcademicRepository, etc.).
 * - Enforces per-category VoiceCommandPermissions from UserPreferencesRepository.
 * - Enforces explicit user confirmation before executing mutating or sensitive actions.
 * - Enforces Recovery Safety Guard before starting workouts/jogging.
 * - Deduplicates rapid accidental re-execution of mutating commands.
 */
class SystemCommandExecutor(
    private val database: SoloLevelingDatabase,
    private val preferencesRepository: UserPreferencesRepository,
    private val realityOsRepository: RealityOsRepository,
    private val lifeManagementRepository: LifeManagementRepository,
    private val academicRepository: AcademicRepository,
    private val aiSystemRepository: AiSystemRepository,
    private val shadowArmyRepository: ShadowArmyRepository? = null,
    private val activeAccountOwnerIdProvider: suspend () -> String = { DEFAULT_SHADOW_ACCOUNT_OWNER_ID }
) {
    private val recentMutationFingerprints = ConcurrentHashMap<String, Long>()

    suspend fun execute(
        intent: StructuredVoiceIntent,
        confirmed: Boolean = false,
        nowEpochMs: Long = System.currentTimeMillis()
    ): CommandExecutionResult {
        if (intent.source == IntentResolutionSource.REJECTED || intent.rejectionReason != null) {
            return CommandExecutionResult(
                status = CommandExecutionStatus.REJECTED,
                commandLabel = "Security Guard",
                spokenResponse = intent.rejectionReason ?: "Command rejected by System Security Guard.",
                source = IntentResolutionSource.REJECTED
            )
        }

        if (intent.requiresClarification || intent.commandId == null) {
            return CommandExecutionResult(
                status = CommandExecutionStatus.NEEDS_CLARIFICATION,
                commandId = intent.commandId,
                category = intent.category,
                commandLabel = intent.commandId?.let { VoiceCommandRegistry.findById(it).label } ?: "Clarification",
                spokenResponse = intent.clarificationQuestion ?: "Please clarify your command.",
                source = intent.source
            )
        }

        val def = VoiceCommandRegistry.findById(intent.commandId)

        // Parameter validation re-check (defense in depth)
        val paramError = VoiceCommandRegistry.validateParameters(def, intent.parameters)
        if (paramError != null) {
            return CommandExecutionResult(
                status = CommandExecutionStatus.NEEDS_CLARIFICATION,
                commandId = def.id,
                category = def.category,
                commandLabel = def.label,
                spokenResponse = paramError,
                source = intent.source
            )
        }

        // Per-category permission check
        val perms: VoiceCommandPermissions = preferencesRepository.voiceCommandPermissionsFlow.first()
        if (!VoiceCommandRegistry.isCategoryPermitted(def.category, perms)) {
            return CommandExecutionResult(
                status = CommandExecutionStatus.PERMISSION_DENIED,
                commandId = def.id,
                category = def.category,
                commandLabel = def.label,
                spokenResponse = "Voice command category '${def.category.label}' is disabled in Command Permission Settings.",
                source = intent.source
            )
        }

        // Confirmation policy for mutating/sensitive commands
        val needsConfirmation = def.requiresConfirmation && (
            perms.requireConfirmationForMutations || def.id == SystemCommandId.REPLAY_AWAKENING
            )
        if (needsConfirmation && !confirmed) {
            val paramDesc = if (intent.parameters.isEmpty()) "" else {
                " (" + intent.parameters.entries.joinToString(", ") { "${it.key}: ${it.value}" } + ")"
            }
            return CommandExecutionResult(
                status = CommandExecutionStatus.AWAITING_CONFIRMATION,
                commandId = def.id,
                category = def.category,
                commandLabel = def.label,
                spokenResponse = "Confirmation required: ${def.label}$paramDesc. Please confirm to execute.",
                pendingIntent = intent,
                deepLinkRoute = def.deepLinkRoute,
                source = intent.source
            )
        }

        // Duplicate mutation guard (5-second window for mutating commands)
        if (def.requiresConfirmation) {
            val fingerprint = "${def.id.wireId}|${intent.parameters.toSortedMap()}"
            val lastExec = recentMutationFingerprints[fingerprint]
            if (lastExec != null && nowEpochMs - lastExec < DUPLICATE_GUARD_WINDOW_MS) {
                return CommandExecutionResult(
                    status = CommandExecutionStatus.DUPLICATE_IGNORED,
                    commandId = def.id,
                    category = def.category,
                    commandLabel = def.label,
                    spokenResponse = "${def.label} was just executed. Duplicate action prevented.",
                    source = intent.source
                )
            }
            recentMutationFingerprints[fingerprint] = nowEpochMs
        }

        return runCatching {
            dispatchCommand(def, intent)
        }.getOrElse { err ->
            CommandExecutionResult(
                status = CommandExecutionStatus.FAILED,
                commandId = def.id,
                category = def.category,
                commandLabel = def.label,
                spokenResponse = "Unable to execute ${def.label}: ${err.message ?: "unexpected error"}.",
                source = intent.source
            )
        }
    }

    private suspend fun dispatchCommand(
        def: CommandDefinition,
        intent: StructuredVoiceIntent
    ): CommandExecutionResult {
        val player = database.playerDao().getPlayerOnce()?.toDomain() ?: PlayerProfile()
        val now = System.currentTimeMillis()
        val todayEpochDay = LocalDate.now().toEpochDay()

        return when (def.id) {
            // 1. Player Information
            SystemCommandId.GET_PLAYER_LEVEL_RANK -> {
                val msg = "Player ${player.codename}: ${player.rank.displayName}, Level ${player.level}, Title '${player.title}'."
                CommandExecutionResult(CommandExecutionStatus.EXECUTED, def.id, def.category, def.label, msg, msg, def.deepLinkRoute, source = intent.source)
            }
            SystemCommandId.GET_PLAYER_XP -> {
                val remaining = (player.xpToNextLevel - player.currentLevelXp).coerceAtLeast(0)
                val msg = "Total XP is ${player.totalXp}. Current level progress: ${player.currentLevelXp} of ${player.xpToNextLevel} XP ($remaining XP needed for Level ${player.level + 1})."
                CommandExecutionResult(CommandExecutionStatus.EXECUTED, def.id, def.category, def.label, msg, msg, def.deepLinkRoute, source = intent.source)
            }
            SystemCommandId.GET_PLAYER_ATTRIBUTES -> {
                val attrs = player.attributes.asList().joinToString(", ") { "${it.type.code} ${it.totalValue}" }
                val msg = "Current Player Attributes: $attrs. Combined score: ${player.attributes.totalScore}."
                CommandExecutionResult(CommandExecutionStatus.EXECUTED, def.id, def.category, def.label, msg, msg, def.deepLinkRoute, source = intent.source)
            }
            SystemCommandId.GET_DAILY_PROGRESS -> {
                val phys = database.physicalStateDao().getPhysicalStateOnce()?.toDomain() ?: PhysicalDevelopmentState()
                val quests = database.questDao().getAllQuestsOnce().map { it.toDomain() }
                val completedQuests = quests.count { it.isCompleted }
                val todayStart = now - (now % (24 * 60 * 60_000L))
                val todayStudy = database.studySessionDao().observeAllSessions().first()
                    .filter { it.startEpochMs >= todayStart }.sumOf { it.durationMinutes }
                val msg = "Today's progress: Jogging ${phys.completedJoggingMinutesToday}/${phys.effectiveJoggingTarget} min, Study $todayStudy min, Quests $completedQuests/${quests.size} cleared."
                CommandExecutionResult(CommandExecutionStatus.EXECUTED, def.id, def.category, def.label, msg, msg, source = intent.source)
            }

            // 2. Daily Management
            SystemCommandId.VIEW_TODAY_TASKS -> {
                val tasks = database.personalTaskDao().getAllTasksOnce().map { it.toDomain(null) }
                val pending = tasks.filter { it.status == PersonalTaskStatus.PENDING || it.status == PersonalTaskStatus.IN_PROGRESS }
                val msg = if (pending.isEmpty()) {
                    "You have no pending personal tasks today. Opening Personal Tasks."
                } else {
                    "You have ${pending.size} pending task(s): ${pending.take(3).joinToString(", ") { it.title }}."
                }
                CommandExecutionResult(CommandExecutionStatus.EXECUTED, def.id, def.category, def.label, msg, msg, def.deepLinkRoute, source = intent.source)
            }
            SystemCommandId.CREATE_TASK -> {
                val title = PromptSanitizer.sanitize(intent.parameters["title"].orEmpty(), maxChars = 80)
                val estMinutes = intent.parameters["estimatedMinutes"]?.toIntOrNull()?.coerceIn(5, 240) ?: 30
                val priority = when (intent.parameters["priority"]?.uppercase()) {
                    "HIGH", "CRITICAL" -> TaskPriority.HIGH
                    "LOW" -> TaskPriority.LOW
                    else -> TaskPriority.MEDIUM
                }
                val createdId = lifeManagementRepository.createTask(
                    title = title,
                    description = "Created via System Voice Command",
                    category = TaskCategory.PERSONAL,
                    priority = priority,
                    dueDateEpochDay = todayEpochDay,
                    scheduledStartMinute = null,
                    estimatedMinutes = estMinutes,
                    linkedGoalId = null,
                    isTimeSensitive = false
                )
                val msg = "Task '$title' created for today ($estMinutes min, 0 XP until completed)."
                CommandExecutionResult(CommandExecutionStatus.EXECUTED, def.id, def.category, def.label, msg, "$msg (Task #$createdId)", def.deepLinkRoute, source = intent.source)
            }
            SystemCommandId.COMPLETE_TASK -> {
                val queryTitle = intent.parameters["title"].orEmpty().trim()
                val pending = database.personalTaskDao().getAllTasksOnce()
                    .map { it.toDomain(null) }
                    .filter { it.status != PersonalTaskStatus.COMPLETED }
                val match = pending.firstOrNull { it.title.contains(queryTitle, ignoreCase = true) }
                    ?: pending.firstOrNull { queryTitle.contains(it.title, ignoreCase = true) }
                if (match == null) {
                    CommandExecutionResult(
                        status = CommandExecutionStatus.NEEDS_CLARIFICATION,
                        commandId = def.id,
                        category = def.category,
                        commandLabel = def.label,
                        spokenResponse = "No pending task matched '$queryTitle'. Open Personal Tasks to select one directly.",
                        deepLinkRoute = def.deepLinkRoute,
                        source = intent.source
                    )
                } else {
                    val feedback = lifeManagementRepository.completeTask(match.id)
                    val msg = "Completed task '${match.title}'. ${feedback.message}"
                    CommandExecutionResult(CommandExecutionStatus.EXECUTED, def.id, def.category, def.label, msg, msg, def.deepLinkRoute, source = intent.source)
                }
            }
            SystemCommandId.VIEW_DAILY_SCHEDULE -> {
                val refs = database.scheduleDao().getReferencesInRangeOnce(todayEpochDay, todayEpochDay)
                val msg = "Opening today's Daily Schedule. ${refs.size} custom schedule reference(s) stored."
                CommandExecutionResult(CommandExecutionStatus.EXECUTED, def.id, def.category, def.label, msg, msg, def.deepLinkRoute, source = intent.source)
            }

            // 3. Habits & Routines
            SystemCommandId.VIEW_TODAY_HABITS -> {
                val habits = database.habitDao().getAllHabitsOnce().map { it.toDomain() }.filter { !it.isArchived }
                val msg = if (habits.isEmpty()) {
                    "No active habits configured yet. Opening Habit Engine."
                } else {
                    "You have ${habits.size} active habit(s): ${habits.take(3).joinToString(", ") { it.title }}."
                }
                CommandExecutionResult(CommandExecutionStatus.EXECUTED, def.id, def.category, def.label, msg, msg, def.deepLinkRoute, source = intent.source)
            }
            SystemCommandId.COMPLETE_HABIT -> {
                val queryName = intent.parameters["habitName"].orEmpty().trim()
                val habits = database.habitDao().getAllHabitsOnce().map { it.toDomain() }.filter { !it.isArchived }
                val match = habits.firstOrNull { it.title.contains(queryName, ignoreCase = true) }
                    ?: habits.firstOrNull { queryName.contains(it.title, ignoreCase = true) }
                if (match == null) {
                    CommandExecutionResult(
                        status = CommandExecutionStatus.NEEDS_CLARIFICATION,
                        commandId = def.id,
                        category = def.category,
                        commandLabel = def.label,
                        spokenResponse = "No active habit matched '$queryName'. Opening Habit Engine.",
                        deepLinkRoute = def.deepLinkRoute,
                        source = intent.source
                    )
                } else {
                    val feedback = lifeManagementRepository.logHabitProgress(
                        habitId = match.id,
                        epochDay = todayEpochDay,
                        explicitStatus = HabitDayStatus.COMPLETED,
                        note = "Completed via System Voice Command"
                    )
                    val msg = "Habit '${match.title}' logged as completed. ${feedback.message}"
                    CommandExecutionResult(CommandExecutionStatus.EXECUTED, def.id, def.category, def.label, msg, msg, def.deepLinkRoute, source = intent.source)
                }
            }
            SystemCommandId.GET_HABIT_STREAK -> {
                val habits = database.habitDao().getAllHabitsOnce().map { it.toDomain() }.filter { !it.isArchived }
                val comp = database.habitDao().getCompletionsSinceOnce(todayEpochDay - 180L).map { it.toDomain() }
                val best = habits.map { HabitEngine.buildAnalyticsSummary(it, comp, todayEpochDay) }
                    .maxByOrNull { it.currentStreakDays }
                val msg = if (best != null && best.currentStreakDays > 0) {
                    "Your strongest active habit streak is '${best.habit.title}' at ${best.currentStreakDays} day(s). Overall Player streak: ${player.streakDays} day(s)."
                } else {
                    "No active multi-day habit streak yet. Overall Player streak is ${player.streakDays} day(s)."
                }
                CommandExecutionResult(CommandExecutionStatus.EXECUTED, def.id, def.category, def.label, msg, msg, def.deepLinkRoute, source = intent.source)
            }

            // 4. Physical Development (Recovery Safety Guard Enforced!)
            SystemCommandId.START_WORKOUT, SystemCommandId.START_JOGGING -> {
                val recovery = database.recoveryAssessmentDao().getLatestOnce()?.toDomain()
                if (recovery != null && (recovery.state == RecoveryState.REST_RECOMMENDED || recovery.selfReportedDiscomfort)) {
                    val reason = if (recovery.selfReportedDiscomfort) {
                        "self-reported physical discomfort"
                    } else {
                        "Recovery State: ${recovery.state.label} (readiness ${recovery.readinessScore}/100)"
                    }
                    CommandExecutionResult(
                        status = CommandExecutionStatus.BLOCKED_BY_SAFETY,
                        commandId = def.id,
                        category = def.category,
                        commandLabel = def.label,
                        spokenResponse = "Recovery Safety Guard active due to $reason. Intense training is blocked; prioritize rest or light mobility.",
                        deepLinkRoute = "physical",
                        source = intent.source
                    )
                } else {
                    val label = if (def.id == SystemCommandId.START_JOGGING) "Jogging Tracker" else "Physical Workout Engine"
                    val msg = "Recovery readiness verified. Opening $label so you can record a verified session."
                    CommandExecutionResult(CommandExecutionStatus.EXECUTED, def.id, def.category, def.label, msg, msg, def.deepLinkRoute, source = intent.source)
                }
            }
            SystemCommandId.VIEW_WORKOUT_PLAN -> {
                val phys = database.physicalStateDao().getPhysicalStateOnce()?.toDomain() ?: PhysicalDevelopmentState()
                val msg = "Today's physical plan: ${phys.effectiveJoggingTarget} min jogging target (${phys.completedJoggingMinutesToday} min completed). Workout status: ${phys.workoutStatus.label}."
                CommandExecutionResult(CommandExecutionStatus.EXECUTED, def.id, def.category, def.label, msg, msg, def.deepLinkRoute, source = intent.source)
            }
            SystemCommandId.GET_FITNESS_PROGRESS -> {
                val weekAgo = now - 7 * 24 * 60 * 60_000L
                val jogs = database.joggingSessionDao().getSessionsSince(weekAgo)
                val workouts = database.workoutSessionDao().getWorkoutsSince(weekAgo)
                val msg = "Past 7 days fitness telemetry: ${jogs.sumOf { it.durationMinutes }} jogging minutes across ${jogs.size} run(s) and ${workouts.size} structured workout(s)."
                CommandExecutionResult(CommandExecutionStatus.EXECUTED, def.id, def.category, def.label, msg, msg, def.deepLinkRoute, source = intent.source)
            }
            SystemCommandId.GET_RECOVERY_STATUS -> {
                val recovery = database.recoveryAssessmentDao().getLatestOnce()?.toDomain()
                val msg = if (recovery == null) {
                    "No recovery check-in logged yet. Baseline readiness score is ${player.recoveryScore}/100."
                } else {
                    "Recovery status: ${recovery.state.label}. Readiness score ${recovery.readinessScore}/100. Discomfort reported: ${if (recovery.selfReportedDiscomfort) "Yes" else "No"}."
                }
                CommandExecutionResult(CommandExecutionStatus.EXECUTED, def.id, def.category, def.label, msg, msg, def.deepLinkRoute, source = intent.source)
            }

            // 5. Academic Development
            SystemCommandId.START_STUDY_SESSION -> {
                val mins = intent.parameters["durationMinutes"]?.toIntOrNull()?.coerceIn(10, 180)
                val subj = intent.parameters["subject"]?.takeIf { it.isNotBlank() }
                val detail = buildString {
                    append("Opening Academic Focus Timer")
                    if (mins != null) append(" for a $mins-minute session")
                    if (subj != null) append(" on $subj")
                    append(".")
                }
                CommandExecutionResult(CommandExecutionStatus.EXECUTED, def.id, def.category, def.label, detail, detail, def.deepLinkRoute, source = intent.source)
            }
            SystemCommandId.GET_ACADEMIC_PROGRESS -> {
                val subjects = database.academicSubjectDao().getActiveSubjectsOnce()
                val weekAgo = now - 7 * 24 * 60 * 60_000L
                val weeklyMins = database.studySessionDao().observeAllSessions().first()
                    .filter { it.startEpochMs >= weekAgo }.sumOf { it.durationMinutes }
                val msg = "Academic status: ${subjects.size} active subject(s), $weeklyMins study minutes logged over the last 7 days."
                CommandExecutionResult(CommandExecutionStatus.EXECUTED, def.id, def.category, def.label, msg, msg, def.deepLinkRoute, source = intent.source)
            }
            SystemCommandId.CREATE_REVISION_TASK -> {
                val topic = PromptSanitizer.sanitize(intent.parameters["topic"].orEmpty(), maxChars = 70)
                val duration = intent.parameters["durationMinutes"]?.toIntOrNull()?.coerceIn(15, 180) ?: 30
                val firstSubject = database.academicSubjectDao().getActiveSubjectsOnce().firstOrNull()
                academicRepository.createAcademicQuest(
                    title = "Revision: $topic",
                    description = "Created via System Voice Command",
                    category = AcademicQuestCategory.REVISION,
                    subjectId = firstSubject?.id,
                    topicId = null,
                    difficulty = AcademicQuestDifficulty.E_RANK,
                    objective = "Complete active recall revision for $topic",
                    targetValue = duration,
                    unit = "min",
                    deadlineEpochMs = now + 24 * 60 * 60_000L
                )
                val msg = "Created revision quest 'Revision: $topic' ($duration min, 0 XP until completed)."
                CommandExecutionResult(CommandExecutionStatus.EXECUTED, def.id, def.category, def.label, msg, msg, def.deepLinkRoute, source = intent.source)
            }
            SystemCommandId.GET_WEEKLY_STUDY_HOURS -> {
                val weekAgo = now - 7 * 24 * 60 * 60_000L
                val weeklyMins = database.studySessionDao().observeAllSessions().first()
                    .filter { it.startEpochMs >= weekAgo }.sumOf { it.durationMinutes }
                val hours = weeklyMins / 60.0
                val msg = "You have logged ${"%.1f".format(hours)} hours ($weeklyMins minutes) of study over the past 7 days."
                CommandExecutionResult(CommandExecutionStatus.EXECUTED, def.id, def.category, def.label, msg, msg, source = intent.source)
            }

            // 6. Missions & RPG
            SystemCommandId.VIEW_ACTIVE_MISSIONS, SystemCommandId.VIEW_PENDING_QUESTS -> {
                val pending = database.questDao().getPendingQuestsOnce().map { it.toDomain() }
                val msg = if (pending.isEmpty()) {
                    "All daily quests are currently cleared. Opening Quest Board."
                } else {
                    "You have ${pending.size} pending quest(s): ${pending.take(3).joinToString(", ") { it.title }}."
                }
                CommandExecutionResult(CommandExecutionStatus.EXECUTED, def.id, def.category, def.label, msg, msg, def.deepLinkRoute, source = intent.source)
            }
            SystemCommandId.GET_DUNGEON_PROGRESS -> {
                val active = database.dungeonDao().getAllInstancesOnce()
                    .filter { it.statusName == "ACTIVE" || it.statusName == "PAUSED" }
                val cleared = database.dungeonDao().getTotalClearedCountOnce()
                val msg = "Dungeon telemetry: ${active.size} active gate(s), $cleared gate(s) cleared total. Opening Dungeon Gate Hub."
                CommandExecutionResult(CommandExecutionStatus.EXECUTED, def.id, def.category, def.label, msg, msg, def.deepLinkRoute, source = intent.source)
            }
            SystemCommandId.GET_LATEST_ACHIEVEMENT -> {
                val unlocks = database.achievementTitleDao().getAllAchievementUnlocksOnce()
                val msg = if (unlocks.isEmpty()) {
                    "No achievements unlocked yet. Complete verified workouts, study blocks, or quests to unlock achievements."
                } else {
                    val latest = unlocks.maxByOrNull { it.unlockedAtEpochMs }!!
                    "You have unlocked ${unlocks.size} achievement(s). Latest unlock ID: ${latest.achievementId}."
                }
                CommandExecutionResult(CommandExecutionStatus.EXECUTED, def.id, def.category, def.label, msg, msg, source = intent.source)
            }
            SystemCommandId.GET_SHADOW_ARMY_STATUS -> {
                val ownerId = activeAccountOwnerIdProvider()
                val monarch = shadowArmyRepository?.let { runCatching { it.getMonarchProfileOnce(ownerId) }.getOrNull() }
                val shadows = database.shadowDao().getAllShadowsForOwnerOnce(ownerId).filter { !it.isRetired }
                val deployments = database.shadowDeploymentDao().getActiveDeploymentsForOwnerOnce(ownerId)
                val msg = if (monarch != null) {
                    "Shadow Monarch Level ${monarch.monarchLevel} (${monarch.monarchRank.label}). Active shadows: ${shadows.size}/${monarch.armyCapacity}, deployed: ${deployments.size}."
                } else {
                    "Shadow Army roster contains ${shadows.size} active shadow(s) and ${deployments.size} active deployment(s)."
                }
                CommandExecutionResult(CommandExecutionStatus.EXECUTED, def.id, def.category, def.label, msg, msg, def.deepLinkRoute, source = intent.source)
            }

            // 7. System Control
            SystemCommandId.NAVIGATE_DASHBOARD ->
                CommandExecutionResult(CommandExecutionStatus.EXECUTED, def.id, def.category, def.label, "Opening Home Dashboard.", "Opening Home Dashboard.", def.deepLinkRoute, source = intent.source)
            SystemCommandId.NAVIGATE_WORKOUT ->
                CommandExecutionResult(CommandExecutionStatus.EXECUTED, def.id, def.category, def.label, "Opening Physical Development screen.", "Opening Physical Development screen.", def.deepLinkRoute, source = intent.source)
            SystemCommandId.NAVIGATE_PROFILE ->
                CommandExecutionResult(CommandExecutionStatus.EXECUTED, def.id, def.category, def.label, "Opening Player Status Window.", "Opening Player Status Window.", def.deepLinkRoute, source = intent.source)
            SystemCommandId.NAVIGATE_AI_COMPANION ->
                CommandExecutionResult(CommandExecutionStatus.EXECUTED, def.id, def.category, def.label, "Opening AI System Companion.", "Opening AI System Companion.", def.deepLinkRoute, source = intent.source)
            SystemCommandId.NAVIGATE_SETTINGS ->
                CommandExecutionResult(CommandExecutionStatus.EXECUTED, def.id, def.category, def.label, "Opening System Voice and Command Settings.", "Opening System Voice and Command Settings.", def.deepLinkRoute, source = intent.source)
            SystemCommandId.NAVIGATE_SHADOW_REALM ->
                CommandExecutionResult(CommandExecutionStatus.EXECUTED, def.id, def.category, def.label, "Opening The Shadow Realm.", "Opening The Shadow Realm.", def.deepLinkRoute, source = intent.source)
            SystemCommandId.REPLAY_AWAKENING ->
                CommandExecutionResult(CommandExecutionStatus.EXECUTED, def.id, def.category, def.label, "Replaying System Awakening sequence.", "Replaying System Awakening sequence.", def.deepLinkRoute, source = intent.source)

            // 8. AI Conversation
            SystemCommandId.AI_CONVERSATION_QUERY -> {
                val query = intent.parameters["query"]?.takeIf { it.isNotBlank() } ?: intent.rawTranscript
                val dispatch = aiSystemRepository.sendChatMessage(conversationId = null, userPrompt = query)
                val reply = dispatch.assistantMessage.content
                CommandExecutionResult(CommandExecutionStatus.EXECUTED, def.id, def.category, def.label, reply, reply, source = intent.source)
            }
            SystemCommandId.AI_GENERATE_WEEKLY_REPORT -> {
                val (_, report) = aiSystemRepository.analyzeDevelopment()
                val msg = "${report.headline}. ${report.overallSummary}"
                CommandExecutionResult(CommandExecutionStatus.EXECUTED, def.id, def.category, def.label, msg, msg, def.deepLinkRoute, source = intent.source)
            }
        }
    }

    companion object {
        private const val DUPLICATE_GUARD_WINDOW_MS = 5_000L
    }
}
