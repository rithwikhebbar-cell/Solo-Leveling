package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.healthconnect.HealthConnectDataSource
import com.example.data.healthconnect.HealthConnectPermissionState
import com.example.data.healthconnect.HealthConnectSdkAvailability
import com.example.data.integration.AiGameMasterPort
import com.example.data.integration.CloudSyncPort
import com.example.data.integration.HealthConnectPort
import com.example.data.integration.SystemAdvisory
import com.example.data.local.DatabaseInitializer
import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.entity.toDomain
import com.example.data.local.entity.toEntity
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.domain.engine.ProgressionEngine
import com.example.domain.engine.RecoveryEngine
import com.example.domain.model.ActivityLog
import com.example.domain.model.ActivityValidationOutcome
import com.example.domain.model.AttributeType
import com.example.domain.model.ExerciseRecord
import com.example.domain.model.HealthConnectSyncState
import com.example.domain.model.ImportedHealthActivity
import com.example.domain.model.JoggingSession
import com.example.domain.model.NotificationCategory
import com.example.domain.model.PhysicalActivityKind
import com.example.domain.model.PhysicalDevelopmentState
import com.example.domain.model.PlayerProfile
import com.example.domain.model.Quest
import com.example.domain.model.QuestCategory
import com.example.domain.model.QuestType
import com.example.domain.model.RecoveryAssessment
import com.example.domain.model.SystemNotification
import com.example.domain.model.VerificationStatus
import com.example.domain.model.WorkoutCategory
import com.example.domain.model.WorkoutSession
import com.example.domain.model.WorkoutStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

data class HealthConnectSyncSummary(
    val importedCount: Int,
    val duplicateCount: Int,
    val rejectedCount: Int,
    val totalXpAwarded: Int,
    val errorMessage: String? = null
)

class RealityOsRepository(
    private val database: SoloLevelingDatabase,
    private val preferencesRepository: UserPreferencesRepository,
    val healthConnectPort: HealthConnectPort,
    private val healthConnectDataSource: HealthConnectDataSource,
    val cloudSyncPort: CloudSyncPort,
    val aiGameMasterPort: AiGameMasterPort
) {
    private val playerDao = database.playerDao()
    private val questDao = database.questDao()
    private val activityLogDao = database.activityLogDao()
    private val physicalStateDao = database.physicalStateDao()
    private val notificationDao = database.systemNotificationDao()
    private val joggingSessionDao = database.joggingSessionDao()
    private val workoutSessionDao = database.workoutSessionDao()
    private val recoveryAssessmentDao = database.recoveryAssessmentDao()
    private val importedActivityDao = database.importedActivityDao()
    private val physicalAuditDao = database.physicalAuditDao()

    val userPreferences: Flow<UserPreferences> = preferencesRepository.preferencesFlow

    val playerProfile: Flow<PlayerProfile> = playerDao.observePlayer().map { entity ->
        entity?.toDomain() ?: PlayerProfile()
    }

    val quests: Flow<List<Quest>> = questDao.observeAllQuests().map { list ->
        list.map { it.toDomain() }
    }

    val physicalState: Flow<PhysicalDevelopmentState> = physicalStateDao.observePhysicalState().map { entity ->
        entity?.toDomain() ?: PhysicalDevelopmentState()
    }

    val notifications: Flow<List<SystemNotification>> = notificationDao.observeNotifications().map { list ->
        list.map { it.toDomain() }
    }

    val activityLogs: Flow<List<ActivityLog>> = activityLogDao.observeAllLogs().map { list ->
        list.map { it.toDomain() }
    }

    val joggingSessions: Flow<List<JoggingSession>> = joggingSessionDao.observeAll().map { list ->
        list.map { it.toDomain() }
    }

    val workoutSessions: Flow<List<WorkoutSession>> = workoutSessionDao.observeAllWorkouts().map { list ->
        list.map { sessionEntity ->
            val exercises = workoutSessionDao.getExercisesForSession(sessionEntity.id).map { it.toDomain() }
            sessionEntity.toDomain(exercises)
        }
    }

    val exerciseRecords: Flow<List<ExerciseRecord>> = workoutSessionDao.observeAllExerciseRecords().map { list ->
        list.map { it.toDomain() }
    }

    val latestRecoveryAssessment: Flow<RecoveryAssessment> = recoveryAssessmentDao.observeLatest().map { entity ->
        entity?.toDomain() ?: RecoveryEngine.assess(0, 0, 0, 0)
    }

    val recoveryHistory: Flow<List<RecoveryAssessment>> = recoveryAssessmentDao.observeHistory().map { list ->
        list.map { it.toDomain() }
    }

    val importedHealthActivities: Flow<List<ImportedHealthActivity>> =
        importedActivityDao.observeAllImported().map { list -> list.map { it.toDomain() } }

    val healthConnectSyncState: Flow<HealthConnectSyncState> =
        importedActivityDao.observeSyncState().map { it?.toDomain() ?: HealthConnectSyncState() }

    suspend fun ensureInitialized() {
        DatabaseInitializer.ensureSeeded(database)
    }

    fun getHealthConnectAvailability(): HealthConnectSdkAvailability =
        healthConnectDataSource.getAvailability()

    suspend fun getHealthConnectPermissionState(): HealthConnectPermissionState =
        healthConnectDataSource.getPermissionState()

    suspend fun completeQuest(questId: Long) {
        database.withTransaction {
            val questEntity = questDao.getQuestById(questId) ?: return@withTransaction
            if (questEntity.isCompleted) return@withTransaction

            val quest = questEntity.toDomain()
            val player = playerDao.getPlayerOnce()?.toDomain() ?: PlayerProfile()
            val now = System.currentTimeMillis()

            val updatedQuest = quest.copy(
                currentValue = quest.targetValue,
                isCompleted = true,
                completedAtEpochMs = now
            )
            questDao.updateQuest(updatedQuest.toEntity())

            val attrDeltas = buildMap {
                put(quest.primaryAttribute, quest.primaryAttrXpReward)
                quest.secondaryAttribute?.let { sec ->
                    if (quest.secondaryAttrXpReward > 0) {
                        put(sec, (get(sec) ?: 0) + quest.secondaryAttrXpReward)
                    }
                }
            }

            applyProgressionAndNotify(
                currentProfile = player,
                xpGained = quest.xpReward,
                attrDeltas = attrDeltas,
                verificationStatus = quest.verificationStatus,
                activityTitle = "Quest: ${quest.title}",
                activityCategory = quest.category,
                durationMinutes = if (quest.unit == "min") quest.targetValue else 20,
                primaryAttr = quest.primaryAttribute,
                primaryAttrXp = quest.primaryAttrXpReward,
                notes = "Completed ${quest.type.label} quest",
                timestamp = now
            )
        }
    }

    suspend fun createCustomQuest(
        title: String,
        description: String,
        type: QuestType,
        category: QuestCategory,
        targetValue: Int,
        unit: String
    ) {
        val sanitizedTitle = title.trim()
        if (sanitizedTitle.isBlank()) return

        val (primaryAttr, secondaryAttr) = when (category) {
            QuestCategory.PHYSICAL -> AttributeType.STR to AttributeType.VIT
            QuestCategory.INTELLECT -> AttributeType.INT to AttributeType.DIS
            QuestCategory.DISCIPLINE -> AttributeType.DIS to AttributeType.WIS
            QuestCategory.RECOVERY -> AttributeType.WIS to AttributeType.VIT
            QuestCategory.SOCIAL -> AttributeType.CHA to AttributeType.WIS
        }

        val xpReward = when (type) {
            QuestType.DAILY -> 35
            QuestType.WEEKLY -> 95
            QuestType.LONG_TERM -> 180
        }

        val quest = Quest(
            title = sanitizedTitle,
            description = description.trim().ifBlank { "Player-defined ${type.label.lowercase()} objective." },
            type = type,
            category = category,
            targetValue = targetValue.coerceAtLeast(1),
            currentValue = 0,
            unit = unit.trim().ifBlank { "reps" },
            xpReward = xpReward,
            primaryAttribute = primaryAttr,
            primaryAttrXpReward = (xpReward * 0.9f).toInt(),
            secondaryAttribute = secondaryAttr,
            secondaryAttrXpReward = (xpReward * 0.45f).toInt(),
            verificationStatus = VerificationStatus.SELF_REPORTED
        )
        questDao.insertQuest(quest.toEntity())
    }

    /**
     * Milestone 1 manual log path preserved & routed through physical tracking + caps when PHYSICAL.
     */
    suspend fun logManualActivity(
        title: String,
        category: QuestCategory,
        durationMinutes: Int,
        isJoggingSession: Boolean,
        notes: String
    ) {
        val sanitizedTitle = title.trim()
        if (sanitizedTitle.isBlank() || durationMinutes <= 0) return

        if (category == QuestCategory.PHYSICAL && isJoggingSession) {
            val endMs = System.currentTimeMillis()
            val startMs = endMs - durationMinutes * 60_000L
            recordJoggingSession(
                title = sanitizedTitle,
                startEpochMs = startMs,
                endEpochMs = endMs,
                durationMinutes = durationMinutes,
                distanceMeters = null,
                isLiveSystemTimer = false,
                notes = notes
            )
            return
        }

        database.withTransaction {
            val player = playerDao.getPlayerOnce()?.toDomain() ?: PlayerProfile()
            val now = System.currentTimeMillis()
            val verification = VerificationStatus.SELF_REPORTED
            val dayStart = startOfTodayEpochMs(now)

            val reward = if (category == QuestCategory.PHYSICAL) {
                val xpToday = physicalAuditDao.getPhysicalXpEarnedSince(dayStart)
                val selfCountToday = physicalAuditDao.getSelfReportedPhysicalCountSince(dayStart)
                ProgressionEngine.calculatePhysicalReward(
                    kind = PhysicalActivityKind.CONDITIONING,
                    durationMinutes = durationMinutes,
                    verificationStatus = verification,
                    xpAlreadyEarnedToday = xpToday,
                    selfReportedPhysicalCountToday = selfCountToday
                )
            } else {
                ProgressionEngine.calculateActivityReward(
                    category = category,
                    durationMinutes = durationMinutes,
                    verificationStatus = verification
                )
            }

            val attrDeltas = mapOf(
                reward.primaryAttribute to reward.primaryAttrXpAwarded,
                reward.secondaryAttribute to reward.secondaryAttrXpAwarded
            )

            applyProgressionAndNotify(
                currentProfile = player,
                xpGained = reward.xpAwarded,
                attrDeltas = attrDeltas,
                verificationStatus = verification,
                activityTitle = sanitizedTitle,
                activityCategory = category,
                durationMinutes = durationMinutes,
                primaryAttr = reward.primaryAttribute,
                primaryAttrXp = reward.primaryAttrXpAwarded,
                notes = listOf(notes.trim(), reward.validationNote).filter { it.isNotBlank() }.joinToString(" • "),
                timestamp = now
            )

            if (category == QuestCategory.PHYSICAL) {
                recalculatePhysicalAndRecoveryLocked(now = now)
            }
        }
    }

    suspend fun recordJoggingSession(
        title: String,
        startEpochMs: Long,
        endEpochMs: Long,
        durationMinutes: Int,
        distanceMeters: Double?,
        isLiveSystemTimer: Boolean,
        notes: String
    ): Result<JoggingSession> {
        val now = System.currentTimeMillis()
        val validation = ProgressionEngine.validateSession(
            title = title,
            startEpochMs = startEpochMs,
            endEpochMs = endEpochMs,
            durationMinutes = durationMinutes,
            distanceMeters = distanceMeters,
            nowEpochMs = now
        )
        if (!validation.isValid) {
            return Result.failure(IllegalArgumentException(validation.rejectionReason ?: "Invalid jogging session."))
        }

        val cleanTitle = title.trim()
        val verification = if (isLiveSystemTimer) {
            VerificationStatus.VERIFIED_SYSTEM_TIMER
        } else {
            VerificationStatus.SELF_REPORTED
        }

        val paceSecPerKm = if (distanceMeters != null && distanceMeters >= 100.0) {
            val totalSeconds = ((endEpochMs - startEpochMs) / 1000.0).coerceAtLeast(durationMinutes * 60.0)
            (totalSeconds / (distanceMeters / 1000.0)).toInt()
        } else null

        val sourceId = "local_jog_${UUID.randomUUID()}"
        var savedSession: JoggingSession? = null

        database.withTransaction {
            val player = playerDao.getPlayerOnce()?.toDomain() ?: PlayerProfile()
            val dayStart = startOfTodayEpochMs(now)
            val xpToday = physicalAuditDao.getPhysicalXpEarnedSince(dayStart)
            val selfCountToday = physicalAuditDao.getSelfReportedPhysicalCountSince(dayStart)

            val reward = ProgressionEngine.calculatePhysicalReward(
                kind = PhysicalActivityKind.JOGGING,
                durationMinutes = durationMinutes,
                verificationStatus = verification,
                xpAlreadyEarnedToday = xpToday,
                selfReportedPhysicalCountToday = selfCountToday
            )

            val session = JoggingSession(
                sourceId = sourceId,
                originApp = "com.aistudio.sololeveling",
                title = cleanTitle,
                activityKind = PhysicalActivityKind.JOGGING,
                startEpochMs = startEpochMs,
                endEpochMs = endEpochMs,
                durationMinutes = durationMinutes,
                distanceMeters = distanceMeters,
                avgPaceSecPerKm = paceSecPerKm,
                avgHeartRateBpm = null,
                verificationStatus = verification,
                xpAwarded = reward.xpAwarded,
                notes = notes.trim(),
                createdAtEpochMs = now
            )
            val insertedId = joggingSessionDao.insert(session.toEntity())
            savedSession = session.copy(id = insertedId)

            val attrDeltas = mapOf(
                reward.primaryAttribute to reward.primaryAttrXpAwarded,
                reward.secondaryAttribute to reward.secondaryAttrXpAwarded
            )

            applyProgressionAndNotify(
                currentProfile = player,
                xpGained = reward.xpAwarded,
                attrDeltas = attrDeltas,
                verificationStatus = verification,
                activityTitle = cleanTitle,
                activityCategory = QuestCategory.PHYSICAL,
                durationMinutes = durationMinutes,
                primaryAttr = reward.primaryAttribute,
                primaryAttrXp = reward.primaryAttrXpAwarded,
                notes = listOf(notes.trim(), reward.validationNote).filter { it.isNotBlank() }.joinToString(" • "),
                timestamp = now
            )

            recalculatePhysicalAndRecoveryLocked(now = now)
        }

        return Result.success(savedSession!!)
    }

    suspend fun recordStructuredWorkout(
        title: String,
        category: WorkoutCategory,
        durationMinutes: Int,
        exercises: List<ExerciseRecord>,
        isLiveSystemTimer: Boolean,
        notes: String
    ): Result<WorkoutSession> {
        if (exercises.isEmpty()) {
            return Result.failure(IllegalArgumentException("Select at least one exercise to record a workout."))
        }
        val now = System.currentTimeMillis()
        val startMs = now - durationMinutes.coerceAtLeast(1) * 60_000L
        val validation = ProgressionEngine.validateSession(
            title = title,
            startEpochMs = startMs,
            endEpochMs = now,
            durationMinutes = durationMinutes,
            nowEpochMs = now
        )
        if (!validation.isValid) {
            return Result.failure(IllegalArgumentException(validation.rejectionReason ?: "Invalid workout."))
        }

        val verification = if (isLiveSystemTimer) {
            VerificationStatus.VERIFIED_SYSTEM_TIMER
        } else {
            VerificationStatus.SELF_REPORTED
        }

        val kind = when (category) {
            WorkoutCategory.STRENGTH, WorkoutCategory.HYPERTROPHY -> PhysicalActivityKind.STRENGTH_TRAINING
            WorkoutCategory.CALISTHENICS -> PhysicalActivityKind.CALISTHENICS
            WorkoutCategory.CONDITIONING -> PhysicalActivityKind.CONDITIONING
            WorkoutCategory.MOBILITY -> PhysicalActivityKind.MOBILITY
        }

        val totalSets = exercises.sumOf { it.setsCompleted }
        val totalReps = exercises.sumOf { it.setsCompleted * it.repsPerSet }
        val sourceId = "local_workout_${UUID.randomUUID()}"
        var savedWorkout: WorkoutSession? = null

        database.withTransaction {
            val player = playerDao.getPlayerOnce()?.toDomain() ?: PlayerProfile()
            val dayStart = startOfTodayEpochMs(now)
            val xpToday = physicalAuditDao.getPhysicalXpEarnedSince(dayStart)
            val selfCountToday = physicalAuditDao.getSelfReportedPhysicalCountSince(dayStart)

            val reward = ProgressionEngine.calculatePhysicalReward(
                kind = kind,
                durationMinutes = durationMinutes,
                verificationStatus = verification,
                xpAlreadyEarnedToday = xpToday,
                selfReportedPhysicalCountToday = selfCountToday,
                totalVolumeSets = totalSets
            )

            val workout = WorkoutSession(
                sourceId = sourceId,
                originApp = "com.aistudio.sololeveling",
                title = title.trim(),
                category = category,
                startEpochMs = startMs,
                endEpochMs = now,
                durationMinutes = durationMinutes,
                totalSets = totalSets,
                totalReps = totalReps,
                avgHeartRateBpm = null,
                verificationStatus = verification,
                xpAwarded = reward.xpAwarded,
                notes = notes.trim(),
                exercises = exercises
            )

            val sessionId = workoutSessionDao.insertWorkout(workout.toEntity())
            workoutSessionDao.insertExercises(exercises.map { it.toEntity(sessionId) })
            savedWorkout = workout.copy(id = sessionId)

            val attrDeltas = mapOf(
                reward.primaryAttribute to reward.primaryAttrXpAwarded,
                reward.secondaryAttribute to reward.secondaryAttrXpAwarded
            )

            applyProgressionAndNotify(
                currentProfile = player,
                xpGained = reward.xpAwarded,
                attrDeltas = attrDeltas,
                verificationStatus = verification,
                activityTitle = "${category.label}: ${title.trim()}",
                activityCategory = QuestCategory.PHYSICAL,
                durationMinutes = durationMinutes,
                primaryAttr = reward.primaryAttribute,
                primaryAttrXp = reward.primaryAttrXpAwarded,
                notes = "$totalSets sets / $totalReps reps • ${reward.validationNote}",
                timestamp = now
            )

            recalculatePhysicalAndRecoveryLocked(now = now)
        }

        return Result.success(savedWorkout!!)
    }

    /**
     * Synchronizes historical records from Health Connect (up to 14 days or since last sync),
     * validates every session, prevents duplicates by sourceId & overlapping intervals,
     * persists locally with VERIFIED_HEALTH_CONNECT provenance, and never fabricates metrics.
     */
    suspend fun syncHealthConnectHistory(daysBack: Int = 14): HealthConnectSyncSummary {
        val now = System.currentTimeMillis()
        val windowStart = now - daysBack.coerceIn(1, 30) * 24 * 60 * 60_000L

        val fetchResult = healthConnectDataSource.fetchRecentActivity(
            sinceEpochMs = windowStart,
            untilEpochMs = now
        )

        if (fetchResult.errorMessage != null) {
            importedActivityDao.upsertSyncState(
                HealthConnectSyncState(
                    lastSyncEpochMs = now,
                    lastSyncStatus = "Attention Required",
                    lastImportedCount = 0,
                    lastDuplicateCount = 0,
                    lastRejectedCount = 0,
                    lastErrorMessage = fetchResult.errorMessage
                ).toEntity()
            )
            return HealthConnectSyncSummary(
                importedCount = 0,
                duplicateCount = 0,
                rejectedCount = 0,
                totalXpAwarded = 0,
                errorMessage = fetchResult.errorMessage
            )
        }

        var importedCount = 0
        var duplicateCount = 0
        var rejectedCount = 0
        var totalXpAwarded = 0

        database.withTransaction {
            var currentPlayer = playerDao.getPlayerOnce()?.toDomain() ?: PlayerProfile()

            for (sample in fetchResult.exerciseSamples) {
                if (sample.sourceId.isBlank() || sample.originPackage.isBlank()) {
                    rejectedCount += 1
                    continue
                }

                // 1. Strict sourceId duplicate check
                if (importedActivityDao.existsBySourceId(sample.sourceId) ||
                    joggingSessionDao.existsBySourceId(sample.sourceId)
                ) {
                    duplicateCount += 1
                    continue
                }

                // 2. Overlapping session duplicate check
                if (importedActivityDao.hasOverlappingSession(sample.startEpochMs, sample.endEpochMs)) {
                    duplicateCount += 1
                    continue
                }

                val durationMinutes = ((sample.endEpochMs - sample.startEpochMs) / 60_000L).toInt()
                val validation = ProgressionEngine.validateSession(
                    title = sample.title,
                    startEpochMs = sample.startEpochMs,
                    endEpochMs = sample.endEpochMs,
                    durationMinutes = durationMinutes,
                    distanceMeters = sample.distanceMeters,
                    nowEpochMs = now
                )

                if (!validation.isValid) {
                    importedActivityDao.insert(
                        ImportedHealthActivity(
                            sourceId = sample.sourceId,
                            originPackage = sample.originPackage,
                            activityType = sample.activityKind.label,
                            title = sample.title.ifBlank { "Invalid Record" },
                            startEpochMs = sample.startEpochMs,
                            endEpochMs = sample.endEpochMs,
                            durationMinutes = durationMinutes.coerceAtLeast(0),
                            distanceMeters = sample.distanceMeters,
                            avgHeartRateBpm = sample.avgHeartRateBpm,
                            verificationStatus = VerificationStatus.VERIFIED_HEALTH_CONNECT,
                            validationOutcome = ActivityValidationOutcome.INVALID_REJECTED,
                            xpAwarded = 0,
                            importedAtEpochMs = now
                        ).toEntity()
                    )
                    rejectedCount += 1
                    continue
                }

                val sessionDayStart = startOfDayEpochMs(sample.startEpochMs)
                val xpOnSessionDay = physicalAuditDao.getPhysicalXpEarnedSince(sessionDayStart)
                val reward = ProgressionEngine.calculatePhysicalReward(
                    kind = sample.activityKind,
                    durationMinutes = durationMinutes,
                    verificationStatus = VerificationStatus.VERIFIED_HEALTH_CONNECT,
                    xpAlreadyEarnedToday = xpOnSessionDay,
                    selfReportedPhysicalCountToday = 0
                )

                val outcome = if (reward.wasCapped) {
                    ActivityValidationOutcome.VALID_CAPPED
                } else {
                    ActivityValidationOutcome.VALID_REWARDED
                }

                importedActivityDao.insert(
                    ImportedHealthActivity(
                        sourceId = sample.sourceId,
                        originPackage = sample.originPackage,
                        activityType = sample.activityKind.label,
                        title = sample.title,
                        startEpochMs = sample.startEpochMs,
                        endEpochMs = sample.endEpochMs,
                        durationMinutes = durationMinutes,
                        distanceMeters = sample.distanceMeters,
                        avgHeartRateBpm = sample.avgHeartRateBpm,
                        verificationStatus = VerificationStatus.VERIFIED_HEALTH_CONNECT,
                        validationOutcome = outcome,
                        xpAwarded = reward.xpAwarded,
                        importedAtEpochMs = now
                    ).toEntity()
                )

                val paceSecPerKm = if (sample.distanceMeters != null && sample.distanceMeters >= 100.0) {
                    val totalSeconds = ((sample.endEpochMs - sample.startEpochMs) / 1000.0).coerceAtLeast(durationMinutes * 60.0)
                    (totalSeconds / (sample.distanceMeters / 1000.0)).toInt()
                } else null

                when (sample.activityKind) {
                    PhysicalActivityKind.JOGGING,
                    PhysicalActivityKind.RUNNING,
                    PhysicalActivityKind.WALKING -> {
                        joggingSessionDao.insert(
                            JoggingSession(
                                sourceId = sample.sourceId,
                                originApp = sample.originPackage,
                                title = sample.title,
                                activityKind = sample.activityKind,
                                startEpochMs = sample.startEpochMs,
                                endEpochMs = sample.endEpochMs,
                                durationMinutes = durationMinutes,
                                distanceMeters = sample.distanceMeters,
                                avgPaceSecPerKm = paceSecPerKm,
                                avgHeartRateBpm = sample.avgHeartRateBpm,
                                verificationStatus = VerificationStatus.VERIFIED_HEALTH_CONNECT,
                                xpAwarded = reward.xpAwarded,
                                notes = sample.notes,
                                createdAtEpochMs = sample.startEpochMs
                            ).toEntity()
                        )
                    }
                    PhysicalActivityKind.STRENGTH_TRAINING,
                    PhysicalActivityKind.CALISTHENICS,
                    PhysicalActivityKind.CONDITIONING,
                    PhysicalActivityKind.MOBILITY -> {
                        val workoutCategory = when (sample.activityKind) {
                            PhysicalActivityKind.CALISTHENICS -> WorkoutCategory.CALISTHENICS
                            PhysicalActivityKind.CONDITIONING -> WorkoutCategory.CONDITIONING
                            PhysicalActivityKind.MOBILITY -> WorkoutCategory.MOBILITY
                            else -> WorkoutCategory.STRENGTH
                        }
                        workoutSessionDao.insertWorkout(
                            WorkoutSession(
                                sourceId = sample.sourceId,
                                originApp = sample.originPackage,
                                title = sample.title,
                                category = workoutCategory,
                                startEpochMs = sample.startEpochMs,
                                endEpochMs = sample.endEpochMs,
                                durationMinutes = durationMinutes,
                                totalSets = 0,
                                totalReps = 0,
                                avgHeartRateBpm = sample.avgHeartRateBpm,
                                verificationStatus = VerificationStatus.VERIFIED_HEALTH_CONNECT,
                                xpAwarded = reward.xpAwarded,
                                notes = sample.notes
                            ).toEntity()
                        )
                    }
                }

                val attrDeltas = mapOf(
                    reward.primaryAttribute to reward.primaryAttrXpAwarded,
                    reward.secondaryAttribute to reward.secondaryAttrXpAwarded
                )

                currentPlayer = applyProgressionAndNotify(
                    currentProfile = currentPlayer,
                    xpGained = reward.xpAwarded,
                    attrDeltas = attrDeltas,
                    verificationStatus = VerificationStatus.VERIFIED_HEALTH_CONNECT,
                    activityTitle = sample.title,
                    activityCategory = QuestCategory.PHYSICAL,
                    durationMinutes = durationMinutes,
                    primaryAttr = reward.primaryAttribute,
                    primaryAttrXp = reward.primaryAttrXpAwarded,
                    notes = "Imported from ${sample.originPackage}",
                    timestamp = sample.startEpochMs
                )

                importedCount += 1
                totalXpAwarded += reward.xpAwarded
            }

            importedActivityDao.upsertSyncState(
                HealthConnectSyncState(
                    lastSyncEpochMs = now,
                    lastSyncStatus = if (importedCount > 0) "Synced ($importedCount new)" else "Up to Date",
                    lastImportedCount = importedCount,
                    lastDuplicateCount = duplicateCount,
                    lastRejectedCount = rejectedCount,
                    lastErrorMessage = null
                ).toEntity()
            )

            recalculatePhysicalAndRecoveryLocked(
                now = now,
                measuredSleepHours = fetchResult.sleepHoursLast24h,
                measuredRestingHr = fetchResult.restingHeartRateEstimateBpm
            )
        }

        return HealthConnectSyncSummary(
            importedCount = importedCount,
            duplicateCount = duplicateCount,
            rejectedCount = rejectedCount,
            totalXpAwarded = totalXpAwarded
        )
    }

    suspend fun submitRecoveryCheckIn(
        fatigueLevel: Int,
        hasDiscomfort: Boolean,
        discomfortNotes: String
    ) {
        val now = System.currentTimeMillis()
        database.withTransaction {
            val latest = recoveryAssessmentDao.getLatestOnce()?.toDomain()
            recalculatePhysicalAndRecoveryLocked(
                now = now,
                measuredSleepHours = latest?.measuredSleepHoursLastNight,
                measuredRestingHr = latest?.measuredRestingHrBpm,
                selfReportedFatigue = fatigueLevel.coerceIn(1, 5),
                selfReportedDiscomfort = hasDiscomfort,
                discomfortNotes = discomfortNotes
            )
        }
    }

    suspend fun markPenaltyFreeRestDay() {
        val now = System.currentTimeMillis()
        database.withTransaction {
            val currentPhysical = physicalStateDao.getPhysicalStateOnce()?.toDomain() ?: PhysicalDevelopmentState()
            physicalStateDao.upsertPhysicalState(
                currentPhysical.copy(
                    workoutStatus = WorkoutStatus.REST_DAY,
                    aiTrainingNote = "Rest day logged with zero progression penalty. Focus on sleep, hydration, and recovery."
                ).toEntity()
            )
            notificationDao.insertNotification(
                SystemNotification(
                    title = "Recovery Rest Day Activated",
                    message = "System honored a penalty-free rest day to protect long-term physical consistency.",
                    category = NotificationCategory.SYSTEM_ALERT,
                    timestampEpochMs = now
                ).toEntity()
            )
        }
    }

    suspend fun acceptJoggingTargetProposal(proposedMinutes: Int) {
        val clamped = proposedMinutes.coerceIn(10, 90)
        val current = physicalStateDao.getPhysicalStateOnce()?.toDomain() ?: PhysicalDevelopmentState()
        physicalStateDao.upsertPhysicalState(
            current.copy(
                aiRecommendedJoggingMinutes = clamped,
                aiTrainingNote = "Player approved adjusted daily jogging target: $clamped min."
            ).toEntity()
        )
    }

    private suspend fun recalculatePhysicalAndRecoveryLocked(
        now: Long,
        measuredSleepHours: Double? = null,
        measuredRestingHr: Int? = null,
        selfReportedFatigue: Int? = null,
        selfReportedDiscomfort: Boolean? = null,
        discomfortNotes: String? = null
    ) {
        val todayStart = startOfTodayEpochMs(now)
        val fortyEightHoursAgo = now - 48 * 60 * 60_000L
        val sevenDaysAgo = now - 7 * 24 * 60 * 60_000L

        val recentJogs = joggingSessionDao.getSessionsSince(sevenDaysAgo)
        val recentWorkouts = workoutSessionDao.getWorkoutsSince(sevenDaysAgo)

        val jogMinutesToday = recentJogs
            .filter { it.startEpochMs >= todayStart && it.activityKindName != PhysicalActivityKind.WALKING.name }
            .sumOf { it.durationMinutes }

        val load48h = recentJogs.filter { it.startEpochMs >= fortyEightHoursAgo }.sumOf { it.durationMinutes } +
            recentWorkouts.filter { it.startEpochMs >= fortyEightHoursAgo }.sumOf { it.durationMinutes }

        val load7d = recentJogs.sumOf { it.durationMinutes } + recentWorkouts.sumOf { it.durationMinutes }
        val sessions7d = recentJogs.size + recentWorkouts.size

        val zone = ZoneId.systemDefault()
        val activeDates = (recentJogs.map { Instant.ofEpochMilli(it.startEpochMs).atZone(zone).toLocalDate() } +
            recentWorkouts.map { Instant.ofEpochMilli(it.startEpochMs).atZone(zone).toLocalDate() }).toSet()

        var consecutiveDays = 0
        var cursorDate = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        while (cursorDate in activeDates) {
            consecutiveDays += 1
            cursorDate = cursorDate.minusDays(1)
        }

        val previousAssessment = recoveryAssessmentDao.getLatestOnce()?.toDomain()
        val effectiveSleep = measuredSleepHours ?: previousAssessment?.measuredSleepHoursLastNight
        val effectiveHr = measuredRestingHr ?: previousAssessment?.measuredRestingHrBpm
        val effectiveFatigue = selfReportedFatigue ?: previousAssessment?.selfReportedFatigue
        val effectiveDiscomfort = selfReportedDiscomfort ?: previousAssessment?.selfReportedDiscomfort ?: false
        val effectiveNotes = discomfortNotes ?: previousAssessment?.discomfortNotes.orEmpty()

        val assessment = RecoveryEngine.assess(
            measuredLoadMinutes48h = load48h,
            measuredLoadMinutes7d = load7d,
            measuredSessions7d = sessions7d,
            consecutiveActiveDays = consecutiveDays,
            measuredSleepHoursLastNight = effectiveSleep,
            measuredRestingHrBpm = effectiveHr,
            selfReportedFatigue = effectiveFatigue,
            selfReportedDiscomfort = effectiveDiscomfort,
            discomfortNotes = effectiveNotes
        )
        recoveryAssessmentDao.insert(assessment.toEntity())

        val currentPlayer = playerDao.getPlayerOnce()?.toDomain() ?: PlayerProfile()
        playerDao.upsertPlayer(currentPlayer.copy(recoveryScore = assessment.readinessScore).toEntity())

        val currentPhysical = physicalStateDao.getPhysicalStateOnce()?.toDomain() ?: PhysicalDevelopmentState()
        val newWorkoutStatus = when {
            currentPhysical.workoutStatus == WorkoutStatus.REST_DAY && jogMinutesToday == 0 -> WorkoutStatus.REST_DAY
            jogMinutesToday >= currentPhysical.effectiveJoggingTarget -> WorkoutStatus.COMPLETED
            sessions7d > 0 && jogMinutesToday > 0 -> WorkoutStatus.IN_PROGRESS
            else -> WorkoutStatus.PENDING
        }

        physicalStateDao.upsertPhysicalState(
            currentPhysical.copy(
                completedJoggingMinutesToday = jogMinutesToday,
                workoutStatus = newWorkoutStatus,
                recoveryStatus = assessment.state.toLegacyStatus(),
                weeklySessionsCompleted = sessions7d,
                lastSessionTimestampMs = now,
                aiTrainingNote = assessment.summaryMessage
            ).toEntity()
        )
    }

    private suspend fun applyProgressionAndNotify(
        currentProfile: PlayerProfile,
        xpGained: Int,
        attrDeltas: Map<AttributeType, Int>,
        verificationStatus: VerificationStatus,
        activityTitle: String,
        activityCategory: QuestCategory,
        durationMinutes: Int,
        primaryAttr: AttributeType,
        primaryAttrXp: Int,
        notes: String,
        timestamp: Long
    ): PlayerProfile {
        val levelResult = ProgressionEngine.applyXp(
            currentLevel = currentProfile.level,
            currentLevelXp = currentProfile.currentLevelXp,
            totalXp = currentProfile.totalXp,
            gainedXp = xpGained
        )

        val newAttributes = ProgressionEngine.applyAttributeXp(
            attributes = currentProfile.attributes,
            deltas = attrDeltas,
            isVerified = verificationStatus.isVerified
        )

        val newVerifiedCount = currentProfile.verifiedActivitiesCount + if (verificationStatus.isVerified) 1 else 0
        val newSelfReportedCount = currentProfile.selfReportedActivitiesCount + if (!verificationStatus.isVerified) 1 else 0

        val newRank = ProgressionEngine.evaluateRank(
            level = levelResult.newLevel,
            totalAttributeScore = newAttributes.totalScore,
            verifiedActivitiesCount = newVerifiedCount
        )

        val updatedProfile = currentProfile.copy(
            level = levelResult.newLevel,
            currentLevelXp = levelResult.newCurrentLevelXp,
            xpToNextLevel = levelResult.newXpToNextLevel,
            totalXp = levelResult.newTotalXp,
            rank = newRank,
            title = newRank.title,
            attributes = newAttributes,
            verifiedActivitiesCount = newVerifiedCount,
            selfReportedActivitiesCount = newSelfReportedCount
        )

        playerDao.upsertPlayer(updatedProfile.toEntity())

        activityLogDao.insertLog(
            ActivityLog(
                title = activityTitle,
                category = activityCategory,
                durationMinutes = durationMinutes,
                verificationStatus = verificationStatus,
                xpAwarded = xpGained,
                primaryAttribute = primaryAttr,
                primaryAttrXpAwarded = primaryAttrXp,
                notes = notes,
                timestampEpochMs = timestamp
            ).toEntity()
        )

        notificationDao.insertNotification(
            SystemNotification(
                title = "Activity Recorded (+${xpGained} XP)",
                message = "$activityTitle logged as ${verificationStatus.label}. ${primaryAttr.code} +$primaryAttrXp attr XP.",
                category = NotificationCategory.QUEST_UPDATE,
                timestampEpochMs = timestamp
            ).toEntity()
        )

        if (levelResult.levelsGained > 0) {
            notificationDao.insertNotification(
                SystemNotification(
                    title = "LEVEL UP // LV. ${levelResult.newLevel}",
                    message = "You advanced to Level ${levelResult.newLevel}. Deterministic thresholds recalculated.",
                    category = NotificationCategory.LEVEL_UP,
                    timestampEpochMs = timestamp + 1
                ).toEntity()
            )
        }

        if (newRank != currentProfile.rank) {
            notificationDao.insertNotification(
                SystemNotification(
                    title = "RANK PROMOTION // ${newRank.displayName}",
                    message = "System verified eligibility for ${newRank.displayName} (${newRank.title}).",
                    category = NotificationCategory.RANK_PROMOTION,
                    timestampEpochMs = timestamp + 2
                ).toEntity()
            )
        }

        return updatedProfile
    }

    suspend fun refreshAiTrainingAdvisory(profile: PlayerProfile, physical: PhysicalDevelopmentState): SystemAdvisory {
        val advisory = aiGameMasterPort.generateLocalAdvisory(profile, physical)
        physicalStateDao.upsertPhysicalState(
            physical.copy(
                aiTrainingNote = "${advisory.recommendation} (Suggested target: ${advisory.suggestedJoggingMinutes} min — review in Physical > Jogging to apply)"
            ).toEntity()
        )
        notificationDao.insertNotification(
            SystemNotification(
                title = "AI Game Master: ${advisory.headline}",
                message = advisory.recommendation,
                category = NotificationCategory.AI_RECOMMENDATION,
                timestampEpochMs = System.currentTimeMillis()
            ).toEntity()
        )
        return advisory
    }

    suspend fun acknowledgeNotification(id: Long) {
        notificationDao.markRead(id)
    }

    suspend fun clearReadNotifications() {
        notificationDao.deleteReadNotifications()
    }

    suspend fun updateCodename(codename: String) {
        val clean = codename.trim()
        if (clean.isBlank()) return
        val current = playerDao.getPlayerOnce()?.toDomain() ?: PlayerProfile()
        playerDao.upsertPlayer(current.copy(codename = clean).toEntity())
    }

    suspend fun updateJoggingBaseline(minutes: Int) {
        val clamped = minutes.coerceIn(10, 120)
        val current = physicalStateDao.getPhysicalStateOnce()?.toDomain() ?: PhysicalDevelopmentState()
        physicalStateDao.upsertPhysicalState(
            current.copy(
                baseJoggingTargetMinutes = clamped,
                aiRecommendedJoggingMinutes = clamped
            ).toEntity()
        )
    }

    suspend fun setHudGlowEnabled(enabled: Boolean) = preferencesRepository.setHudGlowEnabled(enabled)
    suspend fun setHealthConnectSyncEnabled(enabled: Boolean) = preferencesRepository.setHealthConnectSyncEnabled(enabled)
    suspend fun setSupabaseCloudSyncEnabled(enabled: Boolean) = preferencesRepository.setSupabaseCloudSyncEnabled(enabled)

    private fun startOfTodayEpochMs(nowEpochMs: Long): Long = startOfDayEpochMs(nowEpochMs)

    private fun startOfDayEpochMs(epochMs: Long): Long {
        val zone = ZoneId.systemDefault()
        val date: LocalDate = Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate()
        return date.atStartOfDay(zone).toInstant().toEpochMilli()
    }
}
