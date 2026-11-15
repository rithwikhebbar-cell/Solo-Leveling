package com.example.data.integration

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.example.domain.engine.RecoveryEngine
import com.example.domain.model.AcademicStudyPlanProposal
import com.example.domain.model.PhysicalDevelopmentState
import com.example.domain.model.PlayerProfile
import com.example.domain.model.RecoveryAssessment
import com.example.domain.model.RecoveryState
import com.example.domain.model.ScheduledRevisionItem
import com.example.domain.model.WorkoutCategory
import com.example.domain.model.WorkoutPlanTemplate
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * Legacy Milestone 1 summary check retained for Settings compatibility.
 */
enum class HealthConnectReadiness(val label: String, val detail: String) {
    AVAILABLE("Provider Detected", "Health Connect framework detected on device. Ready for permission binding."),
    UPDATE_REQUIRED("Setup Required", "Android 14+ or Health Connect app required to sync verified workouts."),
    NOT_SUPPORTED("Unsupported SDK", "Device API level is below Health Connect minimum requirements.")
}

interface HealthConnectPort {
    fun checkReadiness(): HealthConnectReadiness
}

class AndroidHealthConnectAdapter(private val context: Context) : HealthConnectPort {
    override fun checkReadiness(): HealthConnectReadiness {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return HealthConnectReadiness.AVAILABLE
        }
        val installed = runCatching {
            context.packageManager.getPackageInfo("com.google.android.apps.healthdata", PackageManager.PackageInfoFlags.of(0))
            true
        }.getOrElse {
            runCatching {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo("com.google.android.apps.healthdata", 0)
                true
            }.getOrDefault(false)
        }
        return if (installed) HealthConnectReadiness.AVAILABLE else HealthConnectReadiness.UPDATE_REQUIRED
    }
}

/**
 * Supabase-Ready Cloud Synchronization Architecture.
 */
@Serializable
data class PlayerSyncSnapshot(
    val codename: String,
    val level: Int,
    val totalXp: Long,
    val rankCode: String,
    val totalAttributes: Int,
    val verifiedActivitiesCount: Int,
    val syncedAtEpochMs: Long
)

enum class CloudSyncStatus(val label: String) {
    LOCAL_ONLY("Local Vault Active"),
    READY_UNCONFIGURED("Adapter Ready (Awaiting Project URL)"),
    SYNCED("Synchronized")
}

interface CloudSyncPort {
    fun getSyncStatus(): CloudSyncStatus
    fun buildSnapshot(profile: PlayerProfile): PlayerSyncSnapshot
}

class SupabaseSyncAdapter(
    private val isConfiguredProvider: () -> Boolean = { false }
) : CloudSyncPort {
    override fun getSyncStatus(): CloudSyncStatus =
        if (isConfiguredProvider()) CloudSyncStatus.SYNCED else CloudSyncStatus.READY_UNCONFIGURED

    override fun buildSnapshot(profile: PlayerProfile): PlayerSyncSnapshot = PlayerSyncSnapshot(
        codename = profile.codename,
        level = profile.level,
        totalXp = profile.totalXp,
        rankCode = profile.rank.code,
        totalAttributes = profile.attributes.totalScore,
        verifiedActivitiesCount = profile.verifiedActivitiesCount,
        syncedAtEpochMs = System.currentTimeMillis()
    )
}

/**
 * OpenRouter-Ready AI Game Master Architecture.
 * Recommendations & workout templates remain strictly advisory; never mutates XP, attributes, or ranks.
 */
@Serializable
data class OpenRouterMessage(
    val role: String,
    val content: String
)

@Serializable
data class OpenRouterChatRequest(
    val model: String,
    val messages: List<OpenRouterMessage>,
    val max_tokens: Int = 350
)

@Serializable
data class OpenRouterChoice(
    val message: OpenRouterMessage
)

@Serializable
data class OpenRouterChatResponse(
    val id: String? = null,
    val choices: List<OpenRouterChoice> = emptyList()
)

interface OpenRouterApiService {
    @POST("api/v1/chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") bearerToken: String,
        @Body request: OpenRouterChatRequest
    ): OpenRouterChatResponse
}

data class SystemAdvisory(
    val headline: String,
    val recommendation: String,
    val suggestedJoggingMinutes: Int,
    val sourceLabel: String
)

interface AiGameMasterPort {
    fun generateLocalAdvisory(profile: PlayerProfile, physical: PhysicalDevelopmentState): SystemAdvisory
    fun buildOpenRouterPhysicalContextPrompt(
        profile: PlayerProfile,
        physical: PhysicalDevelopmentState,
        recovery: RecoveryAssessment
    ): OpenRouterChatRequest

    fun suggestAdvisoryWorkoutCategory(recovery: RecoveryAssessment): Pair<WorkoutCategory, String>

    fun generateAcademicStudyPlanProposal(
        profile: PlayerProfile,
        currentDailyGoalMinutes: Int,
        todayStudyMinutes: Int,
        dueRevisions: List<ScheduledRevisionItem>,
        nearestExamTitle: String?,
        nearestExamDays: Long?
    ): AcademicStudyPlanProposal?

    fun buildOpenRouterAcademicContextPrompt(
        profile: PlayerProfile,
        todayStudyMinutes: Int,
        dailyGoalMinutes: Int,
        weakTopics: List<String>,
        upcomingExams: List<String>
    ): OpenRouterChatRequest
}

class DeterministicSystemAdvisor : AiGameMasterPort {
    override fun generateLocalAdvisory(
        profile: PlayerProfile,
        physical: PhysicalDevelopmentState
    ): SystemAdvisory {
        val suggestedMinutes = when (physical.recoveryStatus) {
            com.example.domain.model.RecoveryStatus.RECOVERY_REQUIRED -> 15
            com.example.domain.model.RecoveryStatus.MODERATE_LOAD -> 25
            com.example.domain.model.RecoveryStatus.OPTIMAL ->
                if (profile.streakDays >= 7) 35 else physical.baseJoggingTargetMinutes
        }
        val headline = when (physical.recoveryStatus) {
            com.example.domain.model.RecoveryStatus.RECOVERY_REQUIRED -> "Active Recovery Protocol Recommended"
            com.example.domain.model.RecoveryStatus.MODERATE_LOAD -> "Paced Endurance Calibration"
            com.example.domain.model.RecoveryStatus.OPTIMAL -> "Baseline Progression Window Open"
        }
        val recommendation = when (physical.recoveryStatus) {
            com.example.domain.model.RecoveryStatus.RECOVERY_REQUIRED ->
                "High training volume detected (${physical.weeklySessionsCompleted} weekly sessions). Cap jogging at $suggestedMinutes min light walk/jog and focus on sleep."
            com.example.domain.model.RecoveryStatus.MODERATE_LOAD ->
                "Maintain form with a controlled ${suggestedMinutes}-min aerobic session. Avoid pushing maximum intensity today."
            com.example.domain.model.RecoveryStatus.OPTIMAL ->
                "Readiness at ${profile.recoveryScore}%. Target ${suggestedMinutes}-min steady jogging plus 1 Intellect or Discipline quest for balanced attribute growth."
        }
        return SystemAdvisory(
            headline = headline,
            recommendation = recommendation,
            suggestedJoggingMinutes = suggestedMinutes,
            sourceLabel = "Local System Heuristic (OpenRouter Ready)"
        )
    }

    override fun buildOpenRouterPhysicalContextPrompt(
        profile: PlayerProfile,
        physical: PhysicalDevelopmentState,
        recovery: RecoveryAssessment
    ): OpenRouterChatRequest {
        val systemPrompt = "You are the Solo Leveling Reality OS Game Master. " +
            "Provide concise, safe, non-medical physical training and recovery advice. " +
            "Never encourage training through pain or illness, and never promise XP overrides."
        val userPrompt = "Player Level ${profile.level} (${profile.rank.displayName}). " +
            "Recovery=${recovery.state.code} (score=${recovery.readinessScore}/100, " +
            "48hLoad=${recovery.measuredLoadMinutes48h}m, 7dLoad=${recovery.measuredLoadMinutes7d}m, " +
            "sleep=${recovery.measuredSleepHoursLastNight ?: "unknown"}h, " +
            "fatigue=${recovery.selfReportedFatigue ?: "none"}, discomfort=${recovery.selfReportedDiscomfort}). " +
            "Jogging today: ${physical.completedJoggingMinutesToday}/${physical.effectiveJoggingTarget} min. " +
            "Suggest today's advisory focus."

        return OpenRouterChatRequest(
            model = "google/gemini-2.5-flash",
            messages = listOf(
                OpenRouterMessage("system", systemPrompt),
                OpenRouterMessage("user", userPrompt)
            )
        )
    }

    override fun suggestAdvisoryWorkoutCategory(recovery: RecoveryAssessment): Pair<WorkoutCategory, String> {
        return when (recovery.state) {
            RecoveryState.REST_RECOMMENDED ->
                WorkoutCategory.MOBILITY to "Rest recommended today. Only gentle mobility or a full rest day is advised."
            RecoveryState.RECOVERY_RECOMMENDED ->
                WorkoutCategory.MOBILITY to "Recovery recommended. Light mobility and stretching preserve consistency safely."
            RecoveryState.MODERATE ->
                WorkoutCategory.CALISTHENICS to "Moderate readiness. Controlled bodyweight work avoids excessive systemic fatigue."
            RecoveryState.READY ->
                WorkoutCategory.STRENGTH to "Readiness optimal. Cleared for structured Strength or Conditioning."
        }
    }

    override fun generateAcademicStudyPlanProposal(
        profile: PlayerProfile,
        currentDailyGoalMinutes: Int,
        todayStudyMinutes: Int,
        dueRevisions: List<ScheduledRevisionItem>,
        nearestExamTitle: String?,
        nearestExamDays: Long?
    ): AcademicStudyPlanProposal? {
        val topDue = dueRevisions.firstOrNull()
        val recommendedMinutes = when {
            nearestExamDays != null && nearestExamDays in 0L..7L ->
                (currentDailyGoalMinutes + 20).coerceIn(45, 180)
            dueRevisions.size >= 4 ->
                (currentDailyGoalMinutes + 15).coerceIn(45, 150)
            todayStudyMinutes >= currentDailyGoalMinutes && currentDailyGoalMinutes < 90 ->
                currentDailyGoalMinutes + 15
            else -> currentDailyGoalMinutes
        }

        val focusSubject = topDue?.subjectName ?: "Active Curriculum"
        val focusTopic = topDue?.topic?.title
        val rationale = buildString {
            if (nearestExamTitle != null && nearestExamDays != null && nearestExamDays in 0L..14L) {
                append("$nearestExamTitle is in $nearestExamDays day(s). ")
            }
            if (topDue != null) {
                append("Prioritize spaced revision for '${topDue.topic.title}' (est. familiarity ${topDue.retentionEstimatePercent}%). ")
            }
            if (recommendedMinutes != currentDailyGoalMinutes) {
                append("System proposes adjusting your daily study target to $recommendedMinutes min (approval required).")
            } else {
                append("Maintain $currentDailyGoalMinutes min/day with structured focus blocks and revision checkpoints.")
            }
        }

        return AcademicStudyPlanProposal(
            recommendedDailyMinutes = recommendedMinutes,
            currentDailyMinutes = currentDailyGoalMinutes,
            focusSubjectName = focusSubject,
            focusTopicTitle = focusTopic,
            rationale = rationale.trim()
        )
    }

    override fun buildOpenRouterAcademicContextPrompt(
        profile: PlayerProfile,
        todayStudyMinutes: Int,
        dailyGoalMinutes: Int,
        weakTopics: List<String>,
        upcomingExams: List<String>
    ): OpenRouterChatRequest {
        val systemPrompt = "You are the Solo Leveling Reality OS Academic Game Master. " +
            "Provide structured, evidence-based study planning and spaced-revision guidance. " +
            "Never fabricate mastery scores and never promise XP overrides."
        val userPrompt = "Player Level ${profile.level} (INT=${profile.attributes.intel.totalValue}, " +
            "WIS=${profile.attributes.wis.totalValue}, DIS=${profile.attributes.dis.totalValue}). " +
            "Today study: $todayStudyMinutes/$dailyGoalMinutes min. " +
            "Weakest estimated topics: ${weakTopics.ifEmpty { listOf("None flagged") }.joinToString(", ")}. " +
            "Upcoming exams: ${upcomingExams.ifEmpty { listOf("None scheduled") }.joinToString(", ")}."
        return OpenRouterChatRequest(
            model = "google/gemini-2.5-flash",
            messages = listOf(
                OpenRouterMessage("system", systemPrompt),
                OpenRouterMessage("user", userPrompt)
            )
        )
    }
}
