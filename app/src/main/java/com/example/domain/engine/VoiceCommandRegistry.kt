package com.example.domain.engine

import com.example.data.preferences.VoiceCommandPermissions

enum class VoiceInteractionMode(val label: String) {
    COMMAND("Command Mode"),
    CONVERSATION("Conversation Mode")
}

enum class VoiceCommandCategory(val label: String) {
    PLAYER_INFO("Player Information"),
    DAILY_MANAGEMENT("Daily Management"),
    HABITS_AND_ROUTINES("Habits & Routines"),
    PHYSICAL_DEVELOPMENT("Physical Development"),
    ACADEMIC_DEVELOPMENT("Academic Development"),
    MISSIONS_AND_RPG("Missions & RPG"),
    SYSTEM_CONTROL("System Control"),
    AI_CONVERSATION("AI Conversation")
}

enum class SystemCommandId(val wireId: String) {
    // 1. Player Information
    GET_PLAYER_LEVEL_RANK("GET_PLAYER_LEVEL_RANK"),
    GET_PLAYER_XP("GET_PLAYER_XP"),
    GET_PLAYER_ATTRIBUTES("GET_PLAYER_ATTRIBUTES"),
    GET_DAILY_PROGRESS("GET_DAILY_PROGRESS"),

    // 2. Daily Management
    VIEW_TODAY_TASKS("VIEW_TODAY_TASKS"),
    CREATE_TASK("CREATE_TASK"),
    COMPLETE_TASK("COMPLETE_TASK"),
    VIEW_DAILY_SCHEDULE("VIEW_DAILY_SCHEDULE"),

    // 3. Habits & Routines
    VIEW_TODAY_HABITS("VIEW_TODAY_HABITS"),
    COMPLETE_HABIT("COMPLETE_HABIT"),
    GET_HABIT_STREAK("GET_HABIT_STREAK"),

    // 4. Physical Development
    START_WORKOUT("START_WORKOUT"),
    START_JOGGING("START_JOGGING"),
    VIEW_WORKOUT_PLAN("VIEW_WORKOUT_PLAN"),
    GET_FITNESS_PROGRESS("GET_FITNESS_PROGRESS"),
    GET_RECOVERY_STATUS("GET_RECOVERY_STATUS"),

    // 5. Academic Development
    START_STUDY_SESSION("START_STUDY_SESSION"),
    GET_ACADEMIC_PROGRESS("GET_ACADEMIC_PROGRESS"),
    CREATE_REVISION_TASK("CREATE_REVISION_TASK"),
    GET_WEEKLY_STUDY_HOURS("GET_WEEKLY_STUDY_HOURS"),

    // 6. Missions & RPG
    VIEW_ACTIVE_MISSIONS("VIEW_ACTIVE_MISSIONS"),
    VIEW_PENDING_QUESTS("VIEW_PENDING_QUESTS"),
    GET_DUNGEON_PROGRESS("GET_DUNGEON_PROGRESS"),
    GET_LATEST_ACHIEVEMENT("GET_LATEST_ACHIEVEMENT"),
    GET_SHADOW_ARMY_STATUS("GET_SHADOW_ARMY_STATUS"),

    // 7. System Control
    NAVIGATE_DASHBOARD("NAVIGATE_DASHBOARD"),
    NAVIGATE_WORKOUT("NAVIGATE_WORKOUT"),
    NAVIGATE_PROFILE("NAVIGATE_PROFILE"),
    NAVIGATE_AI_COMPANION("NAVIGATE_AI_COMPANION"),
    NAVIGATE_SETTINGS("NAVIGATE_SETTINGS"),
    NAVIGATE_SHADOW_REALM("NAVIGATE_SHADOW_REALM"),
    REPLAY_AWAKENING("REPLAY_AWAKENING"),

    // 8. AI Conversation
    AI_CONVERSATION_QUERY("AI_CONVERSATION_QUERY"),
    AI_GENERATE_WEEKLY_REPORT("AI_GENERATE_WEEKLY_REPORT");

    companion object {
        fun fromWireId(raw: String): SystemCommandId? =
            entries.firstOrNull { it.wireId.equals(raw.trim(), ignoreCase = true) }
    }
}

data class CommandDefinition(
    val id: SystemCommandId,
    val category: VoiceCommandCategory,
    val label: String,
    val description: String,
    val requiredParameters: List<String> = emptyList(),
    val optionalParameters: List<String> = emptyList(),
    val requiresConfirmation: Boolean = false,
    val supportsOffline: Boolean = true,
    val deepLinkRoute: String? = null,
    val examplePhrases: List<String> = emptyList()
)

enum class IntentResolutionSource(val label: String) {
    GEMINI_AI("Gemini AI"),
    OFFLINE_LOCAL("Local Command Engine"),
    REJECTED("Security Guard")
}

data class StructuredVoiceIntent(
    val commandId: SystemCommandId?,
    val category: VoiceCommandCategory?,
    val confidence: Double,
    val parameters: Map<String, String> = emptyMap(),
    val requiresConfirmation: Boolean = false,
    val requiresClarification: Boolean = false,
    val clarificationQuestion: String? = null,
    val spokenSummary: String? = null,
    val source: IntentResolutionSource = IntentResolutionSource.OFFLINE_LOCAL,
    val rawTranscript: String = "",
    val rejectionReason: String? = null
)

/**
 * Central, strictly allowlisted Command Registry for AI Voice Commands.
 * Gemini may ONLY select command IDs registered here; arbitrary code or direct XP/stat mutations do not exist.
 */
object VoiceCommandRegistry {

    val definitions: List<CommandDefinition> = listOf(
        // 1. Player Information
        CommandDefinition(
            id = SystemCommandId.GET_PLAYER_LEVEL_RANK,
            category = VoiceCommandCategory.PLAYER_INFO,
            label = "Player Level & Rank",
            description = "Read current Hunter level, rank, and title.",
            deepLinkRoute = "status",
            examplePhrases = listOf("What is my current level?", "What is my rank?")
        ),
        CommandDefinition(
            id = SystemCommandId.GET_PLAYER_XP,
            category = VoiceCommandCategory.PLAYER_INFO,
            label = "Player XP Status",
            description = "Read total XP and XP remaining to the next level.",
            deepLinkRoute = "status",
            examplePhrases = listOf("How much XP do I have?", "Show my XP")
        ),
        CommandDefinition(
            id = SystemCommandId.GET_PLAYER_ATTRIBUTES,
            category = VoiceCommandCategory.PLAYER_INFO,
            label = "Player Attributes",
            description = "Read all 7 Player Attributes (STR, VIT, AGI, INT, DIS, WIS, CHA).",
            deepLinkRoute = "status",
            examplePhrases = listOf("Show my attributes", "What are my stats?")
        ),
        CommandDefinition(
            id = SystemCommandId.GET_DAILY_PROGRESS,
            category = VoiceCommandCategory.PLAYER_INFO,
            label = "Daily Progress Summary",
            description = "Summarize today's jogging, study minutes, quests, and discipline score.",
            examplePhrases = listOf("Summarize my progress today", "How am I doing today?")
        ),

        // 2. Daily Management
        CommandDefinition(
            id = SystemCommandId.VIEW_TODAY_TASKS,
            category = VoiceCommandCategory.DAILY_MANAGEMENT,
            label = "Today's Tasks",
            description = "List pending tasks and open Personal Tasks screen.",
            deepLinkRoute = "tasks",
            examplePhrases = listOf("What are my tasks for today?", "Show my tasks")
        ),
        CommandDefinition(
            id = SystemCommandId.CREATE_TASK,
            category = VoiceCommandCategory.DAILY_MANAGEMENT,
            label = "Create Personal Task",
            description = "Create a new personal task in Life Management.",
            requiredParameters = listOf("title"),
            optionalParameters = listOf("priority", "estimatedMinutes"),
            requiresConfirmation = true,
            deepLinkRoute = "tasks",
            examplePhrases = listOf("Add a task to review physics notes", "Create task buy running shoes")
        ),
        CommandDefinition(
            id = SystemCommandId.COMPLETE_TASK,
            category = VoiceCommandCategory.DAILY_MANAGEMENT,
            label = "Complete Personal Task",
            description = "Mark an existing personal task as completed through LifeManagementRepository.",
            requiredParameters = listOf("title"),
            requiresConfirmation = true,
            deepLinkRoute = "tasks",
            examplePhrases = listOf("Mark my morning routine complete", "Complete task review notes")
        ),
        CommandDefinition(
            id = SystemCommandId.VIEW_DAILY_SCHEDULE,
            category = VoiceCommandCategory.DAILY_MANAGEMENT,
            label = "Daily Schedule",
            description = "Show today's scheduled blocks and open Daily Schedule.",
            deepLinkRoute = "daily_schedule",
            examplePhrases = listOf("Show my schedule", "What is on my schedule today?")
        ),

        // 3. Habits & Routines
        CommandDefinition(
            id = SystemCommandId.VIEW_TODAY_HABITS,
            category = VoiceCommandCategory.HABITS_AND_ROUTINES,
            label = "Today's Habits",
            description = "Summarize today's habits and open Habit Engine.",
            deepLinkRoute = "habits",
            examplePhrases = listOf("Show my habits today", "What habits are left?")
        ),
        CommandDefinition(
            id = SystemCommandId.COMPLETE_HABIT,
            category = VoiceCommandCategory.HABITS_AND_ROUTINES,
            label = "Complete Habit",
            description = "Log completion of an existing habit through LifeManagementRepository.",
            requiredParameters = listOf("habitName"),
            requiresConfirmation = true,
            deepLinkRoute = "habits",
            examplePhrases = listOf("Complete my hydration habit", "Mark reading habit complete")
        ),
        CommandDefinition(
            id = SystemCommandId.GET_HABIT_STREAK,
            category = VoiceCommandCategory.HABITS_AND_ROUTINES,
            label = "Habit Streak",
            description = "Read current best habit streak and active streaks.",
            deepLinkRoute = "habits",
            examplePhrases = listOf("What is my current habit streak?", "Check my streak")
        ),

        // 4. Physical Development
        CommandDefinition(
            id = SystemCommandId.START_WORKOUT,
            category = VoiceCommandCategory.PHYSICAL_DEVELOPMENT,
            label = "Start Workout",
            description = "Open Physical Workout Engine after checking Recovery Safety Guard.",
            optionalParameters = listOf("workoutType"),
            requiresConfirmation = true,
            deepLinkRoute = "physical",
            examplePhrases = listOf("Start a workout", "Begin strength training")
        ),
        CommandDefinition(
            id = SystemCommandId.START_JOGGING,
            category = VoiceCommandCategory.PHYSICAL_DEVELOPMENT,
            label = "Start Jogging Session",
            description = "Open Jogging Tracker after checking Recovery Safety Guard.",
            optionalParameters = listOf("durationMinutes"),
            requiresConfirmation = true,
            deepLinkRoute = "physical",
            examplePhrases = listOf("Start jogging", "Begin a 20 minute run")
        ),
        CommandDefinition(
            id = SystemCommandId.VIEW_WORKOUT_PLAN,
            category = VoiceCommandCategory.PHYSICAL_DEVELOPMENT,
            label = "Today's Workout Plan",
            description = "Read today's jogging target and physical status.",
            deepLinkRoute = "physical",
            examplePhrases = listOf("Show today's workout plan", "What is my workout today?")
        ),
        CommandDefinition(
            id = SystemCommandId.GET_FITNESS_PROGRESS,
            category = VoiceCommandCategory.PHYSICAL_DEVELOPMENT,
            label = "Fitness Progress",
            description = "Summarize weekly jogging minutes and completed workouts.",
            deepLinkRoute = "physical",
            examplePhrases = listOf("How is my fitness progress?", "Show weekly workouts")
        ),
        CommandDefinition(
            id = SystemCommandId.GET_RECOVERY_STATUS,
            category = VoiceCommandCategory.PHYSICAL_DEVELOPMENT,
            label = "Recovery Status",
            description = "Read current recovery readiness score and safety guidance.",
            deepLinkRoute = "physical",
            examplePhrases = listOf("Check my recovery status", "Am I ready to train?")
        ),

        // 5. Academic Development
        CommandDefinition(
            id = SystemCommandId.START_STUDY_SESSION,
            category = VoiceCommandCategory.ACADEMIC_DEVELOPMENT,
            label = "Start Study Focus Session",
            description = "Open Academic Focus Timer for a verified study session.",
            optionalParameters = listOf("durationMinutes", "subject"),
            deepLinkRoute = "academic_focus",
            examplePhrases = listOf("Start a 45 minute study session", "Open focus timer")
        ),
        CommandDefinition(
            id = SystemCommandId.GET_ACADEMIC_PROGRESS,
            category = VoiceCommandCategory.ACADEMIC_DEVELOPMENT,
            label = "Academic Progress",
            description = "Read study progress, active subjects, and overdue revisions.",
            deepLinkRoute = "academic_revisions",
            examplePhrases = listOf("Show my academic progress", "How are my studies going?")
        ),
        CommandDefinition(
            id = SystemCommandId.CREATE_REVISION_TASK,
            category = VoiceCommandCategory.ACADEMIC_DEVELOPMENT,
            label = "Create Revision Quest",
            description = "Create a revision study quest through AcademicRepository.",
            requiredParameters = listOf("topic"),
            optionalParameters = listOf("durationMinutes"),
            requiresConfirmation = true,
            deepLinkRoute = "academic_revisions",
            examplePhrases = listOf("Create a revision task for Calculus", "Add revision for organic chemistry")
        ),
        CommandDefinition(
            id = SystemCommandId.GET_WEEKLY_STUDY_HOURS,
            category = VoiceCommandCategory.ACADEMIC_DEVELOPMENT,
            label = "Weekly Study Hours",
            description = "Calculate total verified study time over the last 7 days.",
            examplePhrases = listOf("How many hours did I study this week?", "Weekly study time")
        ),

        // 6. Missions & RPG
        CommandDefinition(
            id = SystemCommandId.VIEW_ACTIVE_MISSIONS,
            category = VoiceCommandCategory.MISSIONS_AND_RPG,
            label = "Active Missions & Quests",
            description = "Read active missions and open Quest Board.",
            deepLinkRoute = "quest_board",
            examplePhrases = listOf("Show active missions", "Show today's quests")
        ),
        CommandDefinition(
            id = SystemCommandId.VIEW_PENDING_QUESTS,
            category = VoiceCommandCategory.MISSIONS_AND_RPG,
            label = "Pending Quests",
            description = "Read count and titles of pending daily quests.",
            deepLinkRoute = "quest_board",
            examplePhrases = listOf("What quests are pending?", "Check pending quests")
        ),
        CommandDefinition(
            id = SystemCommandId.GET_DUNGEON_PROGRESS,
            category = VoiceCommandCategory.MISSIONS_AND_RPG,
            label = "Dungeon Gate Progress",
            description = "Check active dungeon gates and cleared dungeon count.",
            deepLinkRoute = "dungeon_gate",
            examplePhrases = listOf("Check dungeon progress", "Open dungeon gates")
        ),
        CommandDefinition(
            id = SystemCommandId.GET_LATEST_ACHIEVEMENT,
            category = VoiceCommandCategory.MISSIONS_AND_RPG,
            label = "Latest Achievement",
            description = "Read unlocked achievement count and latest unlocked achievement.",
            examplePhrases = listOf("What is my latest achievement?", "Show achievements")
        ),
        CommandDefinition(
            id = SystemCommandId.GET_SHADOW_ARMY_STATUS,
            category = VoiceCommandCategory.MISSIONS_AND_RPG,
            label = "Shadow Army Status",
            description = "Read Shadow Monarch level, shadow roster count, and active deployments.",
            deepLinkRoute = "shadow_realm",
            examplePhrases = listOf("Check my shadow army", "Show shadow monarch status")
        ),

        // 7. System Control
        CommandDefinition(
            id = SystemCommandId.NAVIGATE_DASHBOARD,
            category = VoiceCommandCategory.SYSTEM_CONTROL,
            label = "Open Dashboard",
            description = "Navigate to Home Dashboard.",
            deepLinkRoute = "home",
            examplePhrases = listOf("Open dashboard", "Go home")
        ),
        CommandDefinition(
            id = SystemCommandId.NAVIGATE_WORKOUT,
            category = VoiceCommandCategory.SYSTEM_CONTROL,
            label = "Open Workout Screen",
            description = "Navigate to Physical Development screen.",
            deepLinkRoute = "physical",
            examplePhrases = listOf("Open workout screen", "Go to physical")
        ),
        CommandDefinition(
            id = SystemCommandId.NAVIGATE_PROFILE,
            category = VoiceCommandCategory.SYSTEM_CONTROL,
            label = "Open Player Profile",
            description = "Navigate to Player Status Window.",
            deepLinkRoute = "status",
            examplePhrases = listOf("Open profile", "Open status window")
        ),
        CommandDefinition(
            id = SystemCommandId.NAVIGATE_AI_COMPANION,
            category = VoiceCommandCategory.SYSTEM_CONTROL,
            label = "Open AI Companion",
            description = "Navigate to AI System Companion chatbot.",
            deepLinkRoute = "ai_companion",
            examplePhrases = listOf("Open AI Companion", "Open chatbot")
        ),
        CommandDefinition(
            id = SystemCommandId.NAVIGATE_SETTINGS,
            category = VoiceCommandCategory.SYSTEM_CONTROL,
            label = "Open Settings",
            description = "Navigate to System Control & Voice Settings.",
            deepLinkRoute = "system_voice",
            examplePhrases = listOf("Open settings", "Open voice settings")
        ),
        CommandDefinition(
            id = SystemCommandId.NAVIGATE_SHADOW_REALM,
            category = VoiceCommandCategory.SYSTEM_CONTROL,
            label = "Open Shadow Realm",
            description = "Navigate to The Shadow Realm dashboard.",
            deepLinkRoute = "shadow_realm",
            examplePhrases = listOf("Open shadow realm", "Go to shadow army")
        ),
        CommandDefinition(
            id = SystemCommandId.REPLAY_AWAKENING,
            category = VoiceCommandCategory.SYSTEM_CONTROL,
            label = "Replay System Awakening",
            description = "Replay the cinematic System Awakening sequence.",
            requiresConfirmation = true,
            deepLinkRoute = "awakening",
            examplePhrases = listOf("Replay awakening", "Restart awakening")
        ),

        // 8. AI Conversation
        CommandDefinition(
            id = SystemCommandId.AI_CONVERSATION_QUERY,
            category = VoiceCommandCategory.AI_CONVERSATION,
            label = "Ask AI Companion",
            description = "Answer a general question, coaching request, or analysis using Gemini AI + Player Context.",
            optionalParameters = listOf("query"),
            supportsOffline = true,
            examplePhrases = listOf("What should I focus on next?", "Analyze my weakest stat")
        ),
        CommandDefinition(
            id = SystemCommandId.AI_GENERATE_WEEKLY_REPORT,
            category = VoiceCommandCategory.AI_CONVERSATION,
            label = "Generate Weekly Report",
            description = "Generate a concise weekly intelligence report across all modules.",
            deepLinkRoute = "weekly_intelligence",
            supportsOffline = true,
            examplePhrases = listOf("Generate my weekly report", "Read daily system report")
        )
    )

    private val byId: Map<SystemCommandId, CommandDefinition> = definitions.associateBy { it.id }

    fun findById(id: SystemCommandId): CommandDefinition = byId.getValue(id)

    fun isCategoryPermitted(category: VoiceCommandCategory, perms: VoiceCommandPermissions): Boolean = when (category) {
        VoiceCommandCategory.PLAYER_INFO -> perms.allowPlayerInfo
        VoiceCommandCategory.DAILY_MANAGEMENT -> perms.allowDailyManagement
        VoiceCommandCategory.HABITS_AND_ROUTINES -> perms.allowHabitsAndRoutines
        VoiceCommandCategory.PHYSICAL_DEVELOPMENT -> perms.allowPhysicalDevelopment
        VoiceCommandCategory.ACADEMIC_DEVELOPMENT -> perms.allowAcademicDevelopment
        VoiceCommandCategory.MISSIONS_AND_RPG -> perms.allowMissionsAndRpg
        VoiceCommandCategory.SYSTEM_CONTROL -> perms.allowSystemControl
        VoiceCommandCategory.AI_CONVERSATION -> perms.allowAiConversation
    }

    fun validateParameters(def: CommandDefinition, params: Map<String, String>): String? {
        for (req in def.requiredParameters) {
            val v = params[req]?.trim().orEmpty()
            if (v.isBlank()) {
                return "Missing required parameter '$req' for ${def.label}."
            }
        }
        params["durationMinutes"]?.let { raw ->
            val mins = raw.toIntOrNull()
            if (mins != null && (mins < 5 || mins > 300)) {
                return "Duration must be between 5 and 300 minutes."
            }
        }
        return null
    }

    fun formatPromptCatalog(): String = buildString {
        appendLine("ALLOWLISTED COMMANDS (choose ONLY from these IDs, or CONVERSATION / AMBIGUOUS / UNKNOWN):")
        definitions.forEach { d ->
            val req = if (d.requiredParameters.isEmpty()) "none" else d.requiredParameters.joinToString(",")
            val opt = if (d.optionalParameters.isEmpty()) "" else " opt=[${d.optionalParameters.joinToString(",")}]"
            appendLine("- ${d.id.wireId} (${d.category.name}): ${d.description} req=[$req]$opt")
        }
    }
}
