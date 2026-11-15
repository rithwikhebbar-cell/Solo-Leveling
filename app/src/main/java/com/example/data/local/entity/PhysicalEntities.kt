package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.ActivityValidationOutcome
import com.example.domain.model.AttributeType
import com.example.domain.model.ExerciseRecord
import com.example.domain.model.HealthConnectSyncState
import com.example.domain.model.ImportedHealthActivity
import com.example.domain.model.JoggingSession
import com.example.domain.model.PhysicalActivityKind
import com.example.domain.model.RecoveryAssessment
import com.example.domain.model.RecoveryState
import com.example.domain.model.VerificationStatus
import com.example.domain.model.WorkoutCategory
import com.example.domain.model.WorkoutSession

@Entity(
    tableName = "jogging_sessions",
    indices = [Index(value = ["sourceId"], unique = true), Index(value = ["startEpochMs"])]
)
data class JoggingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceId: String,
    val originApp: String,
    val title: String,
    val activityKindName: String,
    val startEpochMs: Long,
    val endEpochMs: Long,
    val durationMinutes: Int,
    val distanceMeters: Double?,
    val avgPaceSecPerKm: Int?,
    val avgHeartRateBpm: Int?,
    val verificationStatusName: String,
    val xpAwarded: Int,
    val notes: String,
    val createdAtEpochMs: Long
)

@Entity(
    tableName = "workout_sessions",
    indices = [Index(value = ["sourceId"], unique = true), Index(value = ["startEpochMs"])]
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceId: String,
    val originApp: String,
    val title: String,
    val categoryName: String,
    val startEpochMs: Long,
    val endEpochMs: Long,
    val durationMinutes: Int,
    val totalSets: Int,
    val totalReps: Int,
    val avgHeartRateBpm: Int?,
    val verificationStatusName: String,
    val xpAwarded: Int,
    val notes: String
)

@Entity(
    tableName = "exercise_records",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutSessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["workoutSessionId"]), Index(value = ["exerciseId"])]
)
data class ExerciseRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutSessionId: Long,
    val exerciseId: String,
    val exerciseName: String,
    val categoryName: String,
    val setsCompleted: Int,
    val repsPerSet: Int,
    val weightKg: Double?,
    val durationSeconds: Int?,
    val restSeconds: Int,
    val primaryAttrName: String,
    val timestampEpochMs: Long
)

@Entity(tableName = "recovery_assessments")
data class RecoveryAssessmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stateName: String,
    val readinessScore: Int,
    val measuredLoadMinutes48h: Int,
    val measuredLoadMinutes7d: Int,
    val measuredSessions7d: Int,
    val consecutiveActiveDays: Int,
    val measuredSleepHoursLastNight: Double?,
    val measuredRestingHrBpm: Int?,
    val selfReportedFatigue: Int?,
    val selfReportedDiscomfort: Boolean,
    val discomfortNotes: String,
    val recommendedActionsJoined: String,
    val summaryMessage: String,
    val timestampEpochMs: Long
)

@Entity(
    tableName = "imported_activities",
    indices = [Index(value = ["startEpochMs", "endEpochMs"])]
)
data class ImportedActivityEntity(
    @PrimaryKey val sourceId: String,
    val originPackage: String,
    val activityType: String,
    val title: String,
    val startEpochMs: Long,
    val endEpochMs: Long,
    val durationMinutes: Int,
    val distanceMeters: Double?,
    val avgHeartRateBpm: Int?,
    val verificationStatusName: String,
    val validationOutcomeName: String,
    val xpAwarded: Int,
    val importedAtEpochMs: Long
)

@Entity(tableName = "health_connect_sync_state")
data class HealthConnectSyncStateEntity(
    @PrimaryKey val id: Int = 1,
    val lastSyncEpochMs: Long?,
    val lastSyncStatus: String,
    val lastImportedCount: Int,
    val lastDuplicateCount: Int,
    val lastRejectedCount: Int,
    val lastErrorMessage: String?
)

fun JoggingSessionEntity.toDomain(): JoggingSession = JoggingSession(
    id = id,
    sourceId = sourceId,
    originApp = originApp,
    title = title,
    activityKind = runCatching { PhysicalActivityKind.valueOf(activityKindName) }
        .getOrDefault(PhysicalActivityKind.JOGGING),
    startEpochMs = startEpochMs,
    endEpochMs = endEpochMs,
    durationMinutes = durationMinutes,
    distanceMeters = distanceMeters,
    avgPaceSecPerKm = avgPaceSecPerKm,
    avgHeartRateBpm = avgHeartRateBpm,
    verificationStatus = runCatching { VerificationStatus.valueOf(verificationStatusName) }
        .getOrDefault(VerificationStatus.SELF_REPORTED),
    xpAwarded = xpAwarded,
    notes = notes,
    createdAtEpochMs = createdAtEpochMs
)

fun JoggingSession.toEntity(): JoggingSessionEntity = JoggingSessionEntity(
    id = id,
    sourceId = sourceId,
    originApp = originApp,
    title = title,
    activityKindName = activityKind.name,
    startEpochMs = startEpochMs,
    endEpochMs = endEpochMs,
    durationMinutes = durationMinutes,
    distanceMeters = distanceMeters,
    avgPaceSecPerKm = avgPaceSecPerKm,
    avgHeartRateBpm = avgHeartRateBpm,
    verificationStatusName = verificationStatus.name,
    xpAwarded = xpAwarded,
    notes = notes,
    createdAtEpochMs = createdAtEpochMs
)

fun ExerciseRecordEntity.toDomain(): ExerciseRecord = ExerciseRecord(
    id = id,
    workoutSessionId = workoutSessionId,
    exerciseId = exerciseId,
    exerciseName = exerciseName,
    category = runCatching { WorkoutCategory.valueOf(categoryName) }.getOrDefault(WorkoutCategory.STRENGTH),
    setsCompleted = setsCompleted,
    repsPerSet = repsPerSet,
    weightKg = weightKg,
    durationSeconds = durationSeconds,
    restSeconds = restSeconds,
    primaryAttribute = runCatching { AttributeType.valueOf(primaryAttrName) }.getOrDefault(AttributeType.STR),
    timestampEpochMs = timestampEpochMs
)

fun ExerciseRecord.toEntity(sessionId: Long): ExerciseRecordEntity = ExerciseRecordEntity(
    id = id,
    workoutSessionId = sessionId,
    exerciseId = exerciseId,
    exerciseName = exerciseName,
    categoryName = category.name,
    setsCompleted = setsCompleted,
    repsPerSet = repsPerSet,
    weightKg = weightKg,
    durationSeconds = durationSeconds,
    restSeconds = restSeconds,
    primaryAttrName = primaryAttribute.name,
    timestampEpochMs = timestampEpochMs
)

fun WorkoutSessionEntity.toDomain(exercises: List<ExerciseRecord> = emptyList()): WorkoutSession = WorkoutSession(
    id = id,
    sourceId = sourceId,
    originApp = originApp,
    title = title,
    category = runCatching { WorkoutCategory.valueOf(categoryName) }.getOrDefault(WorkoutCategory.STRENGTH),
    startEpochMs = startEpochMs,
    endEpochMs = endEpochMs,
    durationMinutes = durationMinutes,
    totalSets = totalSets,
    totalReps = totalReps,
    avgHeartRateBpm = avgHeartRateBpm,
    verificationStatus = runCatching { VerificationStatus.valueOf(verificationStatusName) }
        .getOrDefault(VerificationStatus.SELF_REPORTED),
    xpAwarded = xpAwarded,
    notes = notes,
    exercises = exercises
)

fun WorkoutSession.toEntity(): WorkoutSessionEntity = WorkoutSessionEntity(
    id = id,
    sourceId = sourceId,
    originApp = originApp,
    title = title,
    categoryName = category.name,
    startEpochMs = startEpochMs,
    endEpochMs = endEpochMs,
    durationMinutes = durationMinutes,
    totalSets = totalSets,
    totalReps = totalReps,
    avgHeartRateBpm = avgHeartRateBpm,
    verificationStatusName = verificationStatus.name,
    xpAwarded = xpAwarded,
    notes = notes
)

fun RecoveryAssessmentEntity.toDomain(): RecoveryAssessment = RecoveryAssessment(
    id = id,
    state = runCatching { RecoveryState.valueOf(stateName) }.getOrDefault(RecoveryState.READY),
    readinessScore = readinessScore,
    measuredLoadMinutes48h = measuredLoadMinutes48h,
    measuredLoadMinutes7d = measuredLoadMinutes7d,
    measuredSessions7d = measuredSessions7d,
    consecutiveActiveDays = consecutiveActiveDays,
    measuredSleepHoursLastNight = measuredSleepHoursLastNight,
    measuredRestingHrBpm = measuredRestingHrBpm,
    selfReportedFatigue = selfReportedFatigue,
    selfReportedDiscomfort = selfReportedDiscomfort,
    discomfortNotes = discomfortNotes,
    recommendedActions = recommendedActionsJoined.split("||").filter { it.isNotBlank() },
    summaryMessage = summaryMessage,
    timestampEpochMs = timestampEpochMs
)

fun RecoveryAssessment.toEntity(): RecoveryAssessmentEntity = RecoveryAssessmentEntity(
    id = id,
    stateName = state.name,
    readinessScore = readinessScore,
    measuredLoadMinutes48h = measuredLoadMinutes48h,
    measuredLoadMinutes7d = measuredLoadMinutes7d,
    measuredSessions7d = measuredSessions7d,
    consecutiveActiveDays = consecutiveActiveDays,
    measuredSleepHoursLastNight = measuredSleepHoursLastNight,
    measuredRestingHrBpm = measuredRestingHrBpm,
    selfReportedFatigue = selfReportedFatigue,
    selfReportedDiscomfort = selfReportedDiscomfort,
    discomfortNotes = discomfortNotes,
    recommendedActionsJoined = recommendedActions.joinToString("||"),
    summaryMessage = summaryMessage,
    timestampEpochMs = timestampEpochMs
)

fun ImportedActivityEntity.toDomain(): ImportedHealthActivity = ImportedHealthActivity(
    sourceId = sourceId,
    originPackage = originPackage,
    activityType = activityType,
    title = title,
    startEpochMs = startEpochMs,
    endEpochMs = endEpochMs,
    durationMinutes = durationMinutes,
    distanceMeters = distanceMeters,
    avgHeartRateBpm = avgHeartRateBpm,
    verificationStatus = runCatching { VerificationStatus.valueOf(verificationStatusName) }
        .getOrDefault(VerificationStatus.VERIFIED_HEALTH_CONNECT),
    validationOutcome = runCatching { ActivityValidationOutcome.valueOf(validationOutcomeName) }
        .getOrDefault(ActivityValidationOutcome.VALID_REWARDED),
    xpAwarded = xpAwarded,
    importedAtEpochMs = importedAtEpochMs
)

fun ImportedHealthActivity.toEntity(): ImportedActivityEntity = ImportedActivityEntity(
    sourceId = sourceId,
    originPackage = originPackage,
    activityType = activityType,
    title = title,
    startEpochMs = startEpochMs,
    endEpochMs = endEpochMs,
    durationMinutes = durationMinutes,
    distanceMeters = distanceMeters,
    avgHeartRateBpm = avgHeartRateBpm,
    verificationStatusName = verificationStatus.name,
    validationOutcomeName = validationOutcome.name,
    xpAwarded = xpAwarded,
    importedAtEpochMs = importedAtEpochMs
)

fun HealthConnectSyncStateEntity.toDomain(): HealthConnectSyncState = HealthConnectSyncState(
    lastSyncEpochMs = lastSyncEpochMs,
    lastSyncStatus = lastSyncStatus,
    lastImportedCount = lastImportedCount,
    lastDuplicateCount = lastDuplicateCount,
    lastRejectedCount = lastRejectedCount,
    lastErrorMessage = lastErrorMessage
)

fun HealthConnectSyncState.toEntity(): HealthConnectSyncStateEntity = HealthConnectSyncStateEntity(
    id = 1,
    lastSyncEpochMs = lastSyncEpochMs,
    lastSyncStatus = lastSyncStatus,
    lastImportedCount = lastImportedCount,
    lastDuplicateCount = lastDuplicateCount,
    lastRejectedCount = lastRejectedCount,
    lastErrorMessage = lastErrorMessage
)
