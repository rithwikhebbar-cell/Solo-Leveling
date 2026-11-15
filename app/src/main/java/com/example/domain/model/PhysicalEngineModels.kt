package com.example.domain.model

enum class RecoveryState(
    val code: String,
    val label: String,
    val summaryGuidance: String
) {
    READY(
        code = "READY",
        label = "Ready",
        summaryGuidance = "System readiness optimal. Cleared for full baseline training."
    ),
    MODERATE(
        code = "MODERATE",
        label = "Moderate",
        summaryGuidance = "Moderate cumulative load detected. Keep intensity controlled and hydrate."
    ),
    RECOVERY_RECOMMENDED(
        code = "RECOVERY_RECOMMENDED",
        label = "Recovery Recommended",
        summaryGuidance = "Elevated fatigue or load detected. Reduce duration and favor low-impact mobility."
    ),
    REST_RECOMMENDED(
        code = "REST_RECOMMENDED",
        label = "Rest Recommended",
        summaryGuidance = "High fatigue, discomfort, or low sleep detected. Take a penalty-free rest day."
    );

    fun toLegacyStatus(): RecoveryStatus = when (this) {
        READY -> RecoveryStatus.OPTIMAL
        MODERATE -> RecoveryStatus.MODERATE_LOAD
        RECOVERY_RECOMMENDED, REST_RECOMMENDED -> RecoveryStatus.RECOVERY_REQUIRED
    }
}

enum class PhysicalActivityKind(val label: String) {
    JOGGING("Jogging"),
    RUNNING("Running"),
    WALKING("Walking"),
    STRENGTH_TRAINING("Strength Training"),
    CALISTHENICS("Calisthenics"),
    CONDITIONING("Conditioning"),
    MOBILITY("Mobility & Recovery")
}

data class JoggingSession(
    val id: Long = 0,
    val sourceId: String,
    val originApp: String,
    val title: String,
    val activityKind: PhysicalActivityKind = PhysicalActivityKind.JOGGING,
    val startEpochMs: Long,
    val endEpochMs: Long,
    val durationMinutes: Int,
    val distanceMeters: Double? = null,
    val avgPaceSecPerKm: Int? = null,
    val avgHeartRateBpm: Int? = null,
    val verificationStatus: VerificationStatus,
    val xpAwarded: Int,
    val notes: String = "",
    val createdAtEpochMs: Long = System.currentTimeMillis()
) {
    val distanceKm: Double?
        get() = distanceMeters?.let { it / 1000.0 }
}

enum class WorkoutCategory(val label: String) {
    STRENGTH("Strength"),
    HYPERTROPHY("Hypertrophy"),
    CALISTHENICS("Calisthenics"),
    CONDITIONING("Conditioning"),
    MOBILITY("Mobility")
}

data class ExerciseDefinition(
    val id: String,
    val name: String,
    val category: WorkoutCategory,
    val primaryAttribute: AttributeType,
    val secondaryAttribute: AttributeType,
    val defaultSets: Int,
    val defaultReps: Int,
    val defaultRestSeconds: Int,
    val isTimedSeconds: Boolean = false,
    val cue: String
)

data class ExerciseRecord(
    val id: Long = 0,
    val workoutSessionId: Long = 0,
    val exerciseId: String,
    val exerciseName: String,
    val category: WorkoutCategory,
    val setsCompleted: Int,
    val repsPerSet: Int,
    val weightKg: Double? = null,
    val durationSeconds: Int? = null,
    val restSeconds: Int,
    val primaryAttribute: AttributeType,
    val timestampEpochMs: Long = System.currentTimeMillis()
)

data class WorkoutSession(
    val id: Long = 0,
    val sourceId: String,
    val originApp: String,
    val title: String,
    val category: WorkoutCategory,
    val startEpochMs: Long,
    val endEpochMs: Long,
    val durationMinutes: Int,
    val totalSets: Int,
    val totalReps: Int,
    val avgHeartRateBpm: Int? = null,
    val verificationStatus: VerificationStatus,
    val xpAwarded: Int,
    val notes: String = "",
    val exercises: List<ExerciseRecord> = emptyList()
)

/**
 * Contract prepared for future OpenRouter AI Workout Generation.
 * AI produces plans; the player executes and ProgressionEngine evaluates rewards.
 */
data class WorkoutPlanTemplate(
    val title: String,
    val category: WorkoutCategory,
    val targetExercises: List<ExerciseDefinition>,
    val recommendedRestSeconds: Int,
    val rationale: String
)

data class RecoveryAssessment(
    val id: Long = 0,
    val state: RecoveryState,
    val readinessScore: Int,
    val measuredLoadMinutes48h: Int,
    val measuredLoadMinutes7d: Int,
    val measuredSessions7d: Int,
    val consecutiveActiveDays: Int,
    val measuredSleepHoursLastNight: Double? = null,
    val measuredRestingHrBpm: Int? = null,
    val selfReportedFatigue: Int? = null,
    val selfReportedDiscomfort: Boolean = false,
    val discomfortNotes: String = "",
    val recommendedActions: List<String>,
    val summaryMessage: String,
    val timestampEpochMs: Long = System.currentTimeMillis()
)

enum class ActivityValidationOutcome(val label: String) {
    VALID_REWARDED("Validated & Rewarded"),
    VALID_CAPPED("Validated (Daily Cap Applied)"),
    DUPLICATE_SKIPPED("Duplicate Skipped"),
    INVALID_REJECTED("Invalid Session Rejected")
}

data class ImportedHealthActivity(
    val sourceId: String,
    val originPackage: String,
    val activityType: String,
    val title: String,
    val startEpochMs: Long,
    val endEpochMs: Long,
    val durationMinutes: Int,
    val distanceMeters: Double? = null,
    val avgHeartRateBpm: Int? = null,
    val verificationStatus: VerificationStatus,
    val validationOutcome: ActivityValidationOutcome,
    val xpAwarded: Int,
    val importedAtEpochMs: Long = System.currentTimeMillis()
)

data class HealthConnectSyncState(
    val lastSyncEpochMs: Long? = null,
    val lastSyncStatus: String = "Never Synchronized",
    val lastImportedCount: Int = 0,
    val lastDuplicateCount: Int = 0,
    val lastRejectedCount: Int = 0,
    val lastErrorMessage: String? = null
)

data class JoggingTargetProposal(
    val proposedMinutes: Int,
    val currentMinutes: Int,
    val rationale: String,
    val sourceLabel: String
)

data class ActiveJoggingSessionState(
    val isActive: Boolean = false,
    val isPaused: Boolean = false,
    val startEpochMs: Long = 0L,
    val accumulatedElapsedMs: Long = 0L,
    val lastResumedAtMs: Long = 0L
) {
    fun currentElapsedSeconds(nowMs: Long = System.currentTimeMillis()): Long {
        if (!isActive) return 0L
        val runningDelta = if (!isPaused && lastResumedAtMs > 0L) (nowMs - lastResumedAtMs).coerceAtLeast(0L) else 0L
        return ((accumulatedElapsedMs + runningDelta) / 1000L).coerceAtLeast(0L)
    }
}
