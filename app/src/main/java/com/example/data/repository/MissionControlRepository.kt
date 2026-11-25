package com.example.data.repository

import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.entity.toDomain
import com.example.data.local.entity.toEntity
import com.example.data.preferences.UserPreferencesRepository
import com.example.domain.engine.GoalEngine
import com.example.domain.engine.MissionPrioritizationEngine
import com.example.domain.engine.RecoveryEngine
import com.example.domain.model.AcademicQuestStatus
import com.example.domain.model.AttributeType
import com.example.domain.model.CommitmentState
import com.example.domain.model.GoalStatus
import com.example.domain.model.GoalTimeframe
import com.example.domain.model.MissionCategory
import com.example.domain.model.MissionControlSnapshot
import com.example.domain.model.MissionMetadataOverride
import com.example.domain.model.MissionMetadataStorePayload
import com.example.domain.model.MissionPriority
import com.example.domain.model.MissionSourceModule
import com.example.domain.model.MissionSourceType
import com.example.domain.model.MissionStatus
import com.example.domain.model.PersonalTaskStatus
import com.example.domain.model.QuestCategory
import com.example.domain.model.QuestType
import com.example.domain.model.SystemEventType
import com.example.domain.model.TaskCategory
import com.example.domain.model.TaskPriority
import com.example.domain.model.TaskRecurrenceType
import com.example.domain.model.UnifiedMissionItem
import com.example.domain.model.WorkoutStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap

@OptIn(ExperimentalCoroutinesApi::class)
class MissionControlRepository(
    private val database: SoloLevelingDatabase,
    private val preferencesRepository: UserPreferencesRepository,
    private val realityOsRepository: RealityOsRepository,
    private val lifeManagementRepository: LifeManagementRepository,
    private val systemEventRepository: SystemEventRepository,
    private val activeAccountOwnerIdFlow: Flow<String>
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val reportedOverdueToday = ConcurrentHashMap.newKeySet<String>()

    private data class CoreRecordsBundle(
        val quests: List<com.example.domain.model.Quest>,
        val commitments: List<com.example.domain.model.QuestCommitment>,
        val tasks: List<com.example.domain.model.PersonalTask>,
        val goals: List<com.example.domain.model.LifeGoal>,
        val milestones: List<com.example.domain.model.GoalMilestone>
    )

    private data class AuxiliaryRecordsBundle(
        val academicQuests: List<com.example.domain.model.AcademicQuest>,
        val physicalState: com.example.domain.model.PhysicalDevelopmentState,
        val recovery: com.example.domain.model.RecoveryAssessment,
        val metadata: Map<String, MissionMetadataOverride>
    )

    private val metadataFlow: Flow<Map<String, MissionMetadataOverride>> =
        activeAccountOwnerIdFlow.flatMapLatest { ownerId ->
            preferencesRepository.observeMissionMetadataJson(ownerId).map { raw ->
                decodeMetadata(raw)
            }
        }

    private val coreBundleFlow: Flow<CoreRecordsBundle> = combine(
        database.questDao().observeAllQuests(),
        activeAccountOwnerIdFlow.flatMapLatest { owner ->
            database.questCommitmentDao().observeAllCommitments(owner)
        },
        database.personalTaskDao().observeAllTasks(),
        database.personalTaskDao().observeAllRecurrences(),
        database.goalDao().observeAllGoals(),
        database.goalDao().observeAllMilestones()
    ) { array ->
        @Suppress("UNCHECKED_CAST")
        val quests = (array[0] as List<com.example.data.local.entity.QuestEntity>).map { it.toDomain() }
        @Suppress("UNCHECKED_CAST")
        val commitments = (array[1] as List<com.example.data.local.entity.QuestCommitmentEntity>).map { it.toDomain() }
        @Suppress("UNCHECKED_CAST")
        val taskEntities = array[2] as List<com.example.data.local.entity.PersonalTaskEntity>
        @Suppress("UNCHECKED_CAST")
        val recurrences = (array[3] as List<com.example.data.local.entity.TaskRecurrenceEntity>)
            .associate { it.taskId to it.toDomain() }
        @Suppress("UNCHECKED_CAST")
        val goals = (array[4] as List<com.example.data.local.entity.LifeGoalEntity>).map { it.toDomain() }
        @Suppress("UNCHECKED_CAST")
        val milestones = (array[5] as List<com.example.data.local.entity.GoalMilestoneEntity>).map { it.toDomain() }

        CoreRecordsBundle(
            quests = quests,
            commitments = commitments,
            tasks = taskEntities.map { it.toDomain(recurrences[it.id]) },
            goals = goals,
            milestones = milestones
        )
    }

    private val auxBundleFlow: Flow<AuxiliaryRecordsBundle> = combine(
        database.academicQuestDao().observeAllAcademicQuests(),
        database.physicalStateDao().observePhysicalState(),
        database.recoveryAssessmentDao().observeLatest(),
        metadataFlow
    ) { acadQuests, physEntity, recEntity, metadata ->
        AuxiliaryRecordsBundle(
            academicQuests = acadQuests.map { it.toDomain() },
            physicalState = physEntity?.toDomain() ?: com.example.domain.model.PhysicalDevelopmentState(),
            recovery = recEntity?.toDomain() ?: RecoveryEngine.assess(0, 0, 0, 0),
            metadata = metadata
        )
    }

    val snapshotFlow: Flow<MissionControlSnapshot> = combine(coreBundleFlow, auxBundleFlow) { core, aux ->
        buildSnapshot(core, aux)
    }

    suspend fun getSnapshotOnce(
        nowEpochMs: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): MissionControlSnapshot {
        val ownerId = activeAccountOwnerIdFlow.firstOrNull()?.ifBlank { "local_player" } ?: "local_player"
        val quests = database.questDao().getAllQuestsOnce().map { it.toDomain() }
        val commitments = database.questCommitmentDao().getAllCommitmentsOnce(ownerId).map { it.toDomain() }
        val recurrences = database.personalTaskDao().observeAllRecurrences().first().associate { it.taskId to it.toDomain() }
        val tasks = database.personalTaskDao().getAllTasksOnce().map { it.toDomain(recurrences[it.id]) }
        val goals = database.goalDao().getAllGoalsOnce().map { it.toDomain() }
        val milestones = database.goalDao().getAllMilestonesOnce().map { it.toDomain() }
        val academicQuests = database.academicQuestDao().getAllAcademicQuestsOnce().map { it.toDomain() }
        val physicalState = database.physicalStateDao().getPhysicalStateOnce()?.toDomain()
            ?: com.example.domain.model.PhysicalDevelopmentState()
        val recovery = database.recoveryAssessmentDao().getLatestOnce()?.toDomain()
            ?: RecoveryEngine.assess(0, 0, 0, 0)
        val metadata = decodeMetadata(preferencesRepository.observeMissionMetadataJson(ownerId).first())

        return buildSnapshot(
            CoreRecordsBundle(quests, commitments, tasks, goals, milestones),
            AuxiliaryRecordsBundle(academicQuests, physicalState, recovery, metadata),
            nowEpochMs = nowEpochMs,
            zoneId = zoneId
        )
    }

    suspend fun createMission(
        title: String,
        description: String,
        category: MissionCategory,
        priority: MissionPriority,
        module: MissionSourceModule,
        targetValue: Int,
        unit: String,
        startDateEpochDay: Long?,
        deadlineEpochDay: Long?,
        prerequisiteMissionIds: List<String>
    ): Result<String> {
        val inputValidation = MissionPrioritizationEngine.validateMissionInput(
            title = title,
            targetValue = targetValue,
            startDateEpochDay = startDateEpochDay,
            deadlineEpochDay = deadlineEpochDay
        )
        if (!inputValidation.isValid) {
            return Result.failure(IllegalArgumentException(inputValidation.errorMessage ?: "Invalid mission parameters."))
        }

        val currentSnapshot = getSnapshotOnce()
        val existingGraph = currentSnapshot.allNonArchived.associate { it.missionId to it.prerequisiteMissionIds }
        val knownIds = currentSnapshot.allNonArchived.map { it.missionId }.toSet()

        val tempId = "pending_new_mission"
        val depCheck = MissionPrioritizationEngine.validateDependencies(
            missionId = tempId,
            proposedPrerequisites = prerequisiteMissionIds,
            existingGraph = existingGraph,
            knownMissionIds = knownIds
        )
        if (!depCheck.isValid) {
            return Result.failure(IllegalArgumentException(depCheck.errorMessage ?: "Invalid dependencies."))
        }

        val today = LocalDate.now().toEpochDay()
        val now = System.currentTimeMillis()
        val effectiveDeadline = deadlineEpochDay ?: when (category) {
            MissionCategory.DAILY -> today
            MissionCategory.WEEKLY -> today + 6L
            MissionCategory.LONG_TERM -> today + 30L
        }
        val effectiveStart = startDateEpochDay ?: today

        val createdMissionId: String = if (category == MissionCategory.LONG_TERM) {
            val targetAttr = when (module) {
                MissionSourceModule.PHYSICAL -> AttributeType.STR
                MissionSourceModule.ACADEMIC -> AttributeType.INT
                MissionSourceModule.LIFE -> AttributeType.DIS
                MissionSourceModule.QUEST, MissionSourceModule.GENERAL -> AttributeType.WIS
            }
            val goalId = lifeManagementRepository.createGoal(
                title = title.trim(),
                description = description.trim(),
                timeframe = GoalTimeframe.LONG_TERM,
                targetAttribute = targetAttr,
                completionCriteria = "Target: $targetValue ${unit.ifBlank { "units" }}",
                startDateEpochDay = effectiveStart,
                targetDateEpochDay = effectiveDeadline,
                initialMilestones = listOf("Milestone 1: Initiate ${title.trim()}")
            )
            "goal:$goalId"
        } else {
            val taskCategory = when (module) {
                MissionSourceModule.ACADEMIC -> TaskCategory.LEARNING
                MissionSourceModule.PHYSICAL, MissionSourceModule.LIFE -> TaskCategory.PERSONAL
                MissionSourceModule.QUEST, MissionSourceModule.GENERAL -> TaskCategory.CAREER
            }
            val taskPriority = when (priority) {
                MissionPriority.LOW -> TaskPriority.LOW
                MissionPriority.MEDIUM -> TaskPriority.MEDIUM
                MissionPriority.HIGH -> TaskPriority.HIGH
                MissionPriority.CRITICAL -> TaskPriority.CRITICAL
            }
            val taskId = lifeManagementRepository.createTask(
                title = title.trim(),
                description = description.trim(),
                category = taskCategory,
                priority = taskPriority,
                dueDateEpochDay = effectiveDeadline,
                scheduledStartMinute = null,
                estimatedMinutes = (targetValue * 5).coerceIn(15, 240),
                linkedGoalId = null,
                isTimeSensitive = priority == MissionPriority.CRITICAL || priority == MissionPriority.HIGH,
                recurrenceType = if (category == MissionCategory.WEEKLY) TaskRecurrenceType.WEEKLY else TaskRecurrenceType.NONE
            )
            "task:$taskId"
        }

        saveMetadataOverride(
            MissionMetadataOverride(
                missionId = createdMissionId,
                prerequisiteMissionIds = prerequisiteMissionIds.distinct(),
                isPaused = false,
                isArchivedOverride = false,
                startDateEpochDay = effectiveStart,
                moduleName = module.name,
                targetValueOverride = targetValue,
                unitOverride = unit.trim().ifBlank { "units" },
                deadlineEpochDayOverride = effectiveDeadline,
                updatedAtEpochMs = now
            )
        )

        systemEventRepository.publishEvent(
            eventType = SystemEventType.MISSION_CREATED,
            relatedEntityId = createdMissionId,
            sourceModule = "MISSION_CONTROL",
            metadata = mapOf("title" to title.trim(), "category" to category.name),
            nowEpochMs = now
        )

        return Result.success(createdMissionId)
    }

    suspend fun updateMission(
        missionId: String,
        title: String,
        description: String,
        priority: MissionPriority,
        module: MissionSourceModule,
        targetValue: Int,
        unit: String,
        startDateEpochDay: Long?,
        deadlineEpochDay: Long?,
        prerequisiteMissionIds: List<String>
    ): Result<Unit> {
        val inputValidation = MissionPrioritizationEngine.validateMissionInput(
            title = title,
            targetValue = targetValue,
            startDateEpochDay = startDateEpochDay,
            deadlineEpochDay = deadlineEpochDay
        )
        if (!inputValidation.isValid) {
            return Result.failure(IllegalArgumentException(inputValidation.errorMessage ?: "Invalid mission parameters."))
        }

        val currentSnapshot = getSnapshotOnce()
        val target = currentSnapshot.allNonArchived.firstOrNull { it.missionId == missionId }
            ?: return Result.failure(IllegalArgumentException("Mission not found."))

        val existingGraph = currentSnapshot.allNonArchived.associate { it.missionId to it.prerequisiteMissionIds }
        val knownIds = currentSnapshot.allNonArchived.map { it.missionId }.toSet()
        val depCheck = MissionPrioritizationEngine.validateDependencies(
            missionId = missionId,
            proposedPrerequisites = prerequisiteMissionIds,
            existingGraph = existingGraph,
            knownMissionIds = knownIds
        )
        if (!depCheck.isValid) {
            return Result.failure(IllegalArgumentException(depCheck.errorMessage ?: "Dependency validation failed."))
        }

        val now = System.currentTimeMillis()
        when (target.sourceType) {
            MissionSourceType.PERSONAL_TASK -> {
                val existing = database.personalTaskDao().getTaskById(target.sourceRecordId)
                    ?: return Result.failure(IllegalStateException("Underlying task missing."))
                val rec = database.personalTaskDao().getRecurrenceForTask(target.sourceRecordId)?.toDomain()
                val mappedPriority = when (priority) {
                    MissionPriority.LOW -> TaskPriority.LOW
                    MissionPriority.MEDIUM -> TaskPriority.MEDIUM
                    MissionPriority.HIGH -> TaskPriority.HIGH
                    MissionPriority.CRITICAL -> TaskPriority.CRITICAL
                }
                lifeManagementRepository.updateTask(
                    existing.toDomain(rec).copy(
                        title = title.trim(),
                        description = description.trim(),
                        priority = mappedPriority,
                        dueDateEpochDay = deadlineEpochDay ?: existing.dueDateEpochDay
                    )
                )
            }
            MissionSourceType.LIFE_GOAL -> {
                val existing = database.goalDao().getGoalById(target.sourceRecordId)
                    ?: return Result.failure(IllegalStateException("Underlying goal missing."))
                database.goalDao().updateGoal(
                    existing.toDomain().copy(
                        title = title.trim(),
                        description = description.trim(),
                        startDateEpochDay = startDateEpochDay ?: existing.startDateEpochDay,
                        targetDateEpochDay = deadlineEpochDay ?: existing.targetDateEpochDay,
                        updatedAtEpochMs = now
                    ).toEntity()
                )
            }
            MissionSourceType.QUEST, MissionSourceType.ACADEMIC_QUEST, MissionSourceType.PHYSICAL_PROTOCOL -> {
                // Keep authoritative system quest metrics intact; allow metadata overrides (dependencies/deadline/module)
            }
        }

        val prevOverride = getMetadataOverride(missionId)
        saveMetadataOverride(
            (prevOverride ?: MissionMetadataOverride(missionId = missionId)).copy(
                prerequisiteMissionIds = prerequisiteMissionIds.distinct(),
                startDateEpochDay = startDateEpochDay,
                moduleName = module.name,
                targetValueOverride = targetValue,
                unitOverride = unit.trim().ifBlank { target.unit },
                deadlineEpochDayOverride = deadlineEpochDay,
                updatedAtEpochMs = now
            )
        )

        systemEventRepository.publishEvent(
            eventType = SystemEventType.MISSION_UPDATED,
            relatedEntityId = missionId,
            sourceModule = "MISSION_CONTROL",
            metadata = mapOf("title" to title.trim()),
            nowEpochMs = now
        )

        return Result.success(Unit)
    }

    suspend fun togglePauseMission(missionId: String): Result<Boolean> {
        val snapshot = getSnapshotOnce()
        val target = snapshot.allNonArchived.firstOrNull { it.missionId == missionId }
            ?: return Result.failure(IllegalArgumentException("Mission not found."))
        if (target.status.isTerminal) {
            return Result.failure(IllegalStateException("Cannot pause a completed or archived mission."))
        }

        val now = System.currentTimeMillis()
        val nowPaused = target.status != MissionStatus.PAUSED

        if (target.sourceType == MissionSourceType.LIFE_GOAL) {
            val goal = database.goalDao().getGoalById(target.sourceRecordId)?.toDomain()
            if (goal != null) {
                database.goalDao().updateGoal(
                    goal.copy(
                        status = if (nowPaused) GoalStatus.PAUSED else GoalStatus.ACTIVE,
                        updatedAtEpochMs = now
                    ).toEntity()
                )
            }
        }

        val prev = getMetadataOverride(missionId) ?: MissionMetadataOverride(missionId = missionId)
        saveMetadataOverride(prev.copy(isPaused = nowPaused, updatedAtEpochMs = now))

        systemEventRepository.publishEvent(
            eventType = SystemEventType.MISSION_UPDATED,
            relatedEntityId = missionId,
            sourceModule = "MISSION_CONTROL",
            metadata = mapOf("paused" to nowPaused.toString()),
            nowEpochMs = now
        )
        return Result.success(nowPaused)
    }

    suspend fun archiveMission(missionId: String): Result<Unit> {
        val snapshot = getSnapshotOnce()
        val target = snapshot.allNonArchived.firstOrNull { it.missionId == missionId }
            ?: return Result.failure(IllegalArgumentException("Mission not found."))

        val now = System.currentTimeMillis()
        when (target.sourceType) {
            MissionSourceType.PERSONAL_TASK -> lifeManagementRepository.archiveTask(target.sourceRecordId)
            MissionSourceType.LIFE_GOAL -> {
                val goal = database.goalDao().getGoalById(target.sourceRecordId)?.toDomain()
                if (goal != null) {
                    database.goalDao().updateGoal(
                        goal.copy(status = GoalStatus.ARCHIVED, updatedAtEpochMs = now).toEntity()
                    )
                }
            }
            MissionSourceType.QUEST, MissionSourceType.ACADEMIC_QUEST, MissionSourceType.PHYSICAL_PROTOCOL -> {
                val prev = getMetadataOverride(missionId) ?: MissionMetadataOverride(missionId = missionId)
                saveMetadataOverride(prev.copy(isArchivedOverride = true, updatedAtEpochMs = now))
            }
        }

        systemEventRepository.publishEvent(
            eventType = SystemEventType.MISSION_UPDATED,
            relatedEntityId = missionId,
            sourceModule = "MISSION_CONTROL",
            metadata = mapOf("archived" to "true"),
            nowEpochMs = now
        )
        return Result.success(Unit)
    }

    suspend fun completeMission(missionId: String): Result<String> {
        val snapshot = getSnapshotOnce()
        val target = snapshot.allNonArchived.firstOrNull { it.missionId == missionId }
            ?: return Result.failure(IllegalArgumentException("Mission not found."))

        val now = System.currentTimeMillis()
        if (target.isBlocked) {
            systemEventRepository.publishEvent(
                eventType = SystemEventType.MISSION_BLOCKED,
                relatedEntityId = missionId,
                sourceModule = "MISSION_CONTROL",
                metadata = mapOf("blockedBy" to target.blockedByTitles.joinToString(", ")),
                nowEpochMs = now
            )
            return Result.failure(
                IllegalStateException("Blocked by incomplete prerequisite: ${target.blockedByTitles.joinToString(", ")}")
            )
        }

        if (target.status == MissionStatus.PAUSED) {
            return Result.failure(IllegalStateException("Resume paused mission before completing."))
        }

        val feedbackMessage: String = when (target.sourceType) {
            MissionSourceType.QUEST -> {
                realityOsRepository.completeQuest(target.sourceRecordId)
                "Quest completed via authoritative Progression Engine."
            }
            MissionSourceType.PERSONAL_TASK -> {
                val res = lifeManagementRepository.completeTask(target.sourceRecordId)
                res.message
            }
            MissionSourceType.LIFE_GOAL -> {
                val milestones = database.goalDao().getMilestonesForGoal(target.sourceRecordId)
                val nextMilestone = milestones.firstOrNull { !it.isCompleted }
                if (nextMilestone != null) {
                    val res = lifeManagementRepository.completeGoalMilestone(nextMilestone.id)
                    systemEventRepository.publishEvent(
                        eventType = SystemEventType.GOAL_MILESTONE_REACHED,
                        relatedEntityId = "goal:${target.sourceRecordId}",
                        sourceModule = "MISSION_CONTROL",
                        metadata = mapOf("milestone" to nextMilestone.title),
                        nowEpochMs = now
                    )
                    res.message
                } else {
                    val goal = database.goalDao().getGoalById(target.sourceRecordId)?.toDomain()
                    if (goal != null && goal.status != GoalStatus.COMPLETED) {
                        database.goalDao().updateGoal(goal.copy(status = GoalStatus.COMPLETED, updatedAtEpochMs = now).toEntity())
                    }
                    "Goal marked completed."
                }
            }
            MissionSourceType.ACADEMIC_QUEST, MissionSourceType.PHYSICAL_PROTOCOL -> {
                return Result.failure(
                    IllegalStateException("Execute this mission inside ${target.module.label} so verified timers/metrics record progression.")
                )
            }
        }

        systemEventRepository.publishEvent(
            eventType = SystemEventType.MISSION_COMPLETED,
            relatedEntityId = missionId,
            sourceModule = "MISSION_CONTROL",
            metadata = mapOf("title" to target.title, "category" to target.category.name),
            nowEpochMs = now
        )
        return Result.success(feedbackMessage)
    }

    suspend fun evaluateOverdueEvents(): Int {
        val snapshot = getSnapshotOnce()
        val today = LocalDate.now().toEpochDay()
        val now = System.currentTimeMillis()
        var count = 0
        snapshot.allNonArchived.filter { it.isOverdue }.forEach { mission ->
            val key = "${mission.missionId}:$today"
            if (reportedOverdueToday.add(key)) {
                systemEventRepository.publishEvent(
                    eventType = SystemEventType.MISSION_OVERDUE,
                    relatedEntityId = mission.missionId,
                    sourceModule = "MISSION_CONTROL",
                    metadata = mapOf("title" to mission.title),
                    nowEpochMs = now
                )
                count++
            }
        }
        return count
    }

    private fun buildSnapshot(
        core: CoreRecordsBundle,
        aux: AuxiliaryRecordsBundle,
        nowEpochMs: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): MissionControlSnapshot {
        val today = Instant.ofEpochMilli(nowEpochMs).atZone(zoneId).toLocalDate().toEpochDay()
        val commitmentByQuestId = core.commitments.groupBy { it.questId }
            .mapValues { (_, list) -> list.maxByOrNull { it.deadlineEpochMs } }

        val items = mutableListOf<UnifiedMissionItem>()

        // 1. System Quests
        core.quests.forEach { quest ->
            val id = "quest:${quest.id}"
            val meta = aux.metadata[id]
            if (meta?.isArchivedOverride == true) {
                items.add(
                    UnifiedMissionItem(
                        missionId = id,
                        sourceType = MissionSourceType.QUEST,
                        sourceRecordId = quest.id,
                        title = quest.title,
                        description = quest.description,
                        category = quest.type.toMissionCategory(),
                        module = quest.category.toMissionModule(),
                        status = MissionStatus.ARCHIVED,
                        priority = MissionPriority.MEDIUM,
                        currentValue = quest.currentValue,
                        targetValue = quest.targetValue,
                        unit = quest.unit,
                        completedAtEpochMs = quest.completedAtEpochMs,
                        prerequisiteMissionIds = meta.prerequisiteMissionIds
                    )
                )
                return@forEach
            }

            val commitment = commitmentByQuestId[quest.id]
            val status = when {
                quest.isCompleted -> MissionStatus.COMPLETED
                commitment?.state == CommitmentState.FAILED -> MissionStatus.FAILED
                meta?.isPaused == true -> MissionStatus.PAUSED
                quest.currentValue > 0 -> MissionStatus.ACTIVE
                else -> MissionStatus.NOT_STARTED
            }
            val deadlineMs = commitment?.deadlineEpochMs
            val deadlineDay = meta?.deadlineEpochDayOverride ?: deadlineMs?.let {
                Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate().toEpochDay()
            } ?: when (quest.type) {
                QuestType.DAILY -> today
                QuestType.WEEKLY -> today + 6L
                QuestType.LONG_TERM -> null
            }
            val module = meta?.moduleName?.let { runCatching { MissionSourceModule.valueOf(it) }.getOrNull() }
                ?: quest.category.toMissionModule()

            items.add(
                UnifiedMissionItem(
                    missionId = id,
                    sourceType = MissionSourceType.QUEST,
                    sourceRecordId = quest.id,
                    title = quest.title,
                    description = quest.description,
                    category = quest.type.toMissionCategory(),
                    module = module,
                    status = status,
                    priority = if (quest.type == QuestType.DAILY) MissionPriority.HIGH else MissionPriority.MEDIUM,
                    currentValue = quest.currentValue,
                    targetValue = meta?.targetValueOverride ?: quest.targetValue,
                    unit = meta?.unitOverride ?: quest.unit,
                    startDateEpochDay = meta?.startDateEpochDay,
                    deadlineEpochDay = deadlineDay,
                    deadlineEpochMs = deadlineMs,
                    completedAtEpochMs = quest.completedAtEpochMs,
                    prerequisiteMissionIds = meta?.prerequisiteMissionIds.orEmpty(),
                    executeRoute = module.executeRoute,
                    canEdit = true,
                    canPause = !status.isTerminal,
                    canCompleteDirectly = !quest.isCompleted,
                    canArchive = true,
                    xpRewardHint = quest.xpReward,
                    isDemandingPhysical = quest.category == QuestCategory.PHYSICAL
                )
            )
        }

        // 2. Personal Tasks
        core.tasks.forEach { task ->
            val id = "task:${task.id}"
            val meta = aux.metadata[id]
            val status = when {
                meta?.isArchivedOverride == true || task.status == PersonalTaskStatus.ARCHIVED -> MissionStatus.ARCHIVED
                task.status == PersonalTaskStatus.COMPLETED -> MissionStatus.COMPLETED
                meta?.isPaused == true -> MissionStatus.PAUSED
                task.status == PersonalTaskStatus.IN_PROGRESS -> MissionStatus.ACTIVE
                task.status == PersonalTaskStatus.PENDING || task.status == PersonalTaskStatus.POSTPONED -> MissionStatus.NOT_STARTED
                else -> MissionStatus.NOT_STARTED
            }
            val deadlineDay = meta?.deadlineEpochDayOverride ?: task.dueDateEpochDay
            val category = when {
                task.recurrence?.recurrenceType == TaskRecurrenceType.WEEKLY -> MissionCategory.WEEKLY
                deadlineDay != null && deadlineDay - today > 7L -> MissionCategory.LONG_TERM
                deadlineDay != null && deadlineDay - today in 2L..7L -> MissionCategory.WEEKLY
                else -> MissionCategory.DAILY
            }
            val module = meta?.moduleName?.let { runCatching { MissionSourceModule.valueOf(it) }.getOrNull() }
                ?: if (task.category == TaskCategory.LEARNING) MissionSourceModule.ACADEMIC else MissionSourceModule.LIFE
            val priority = when (task.priority) {
                TaskPriority.LOW -> MissionPriority.LOW
                TaskPriority.MEDIUM -> MissionPriority.MEDIUM
                TaskPriority.HIGH -> MissionPriority.HIGH
                TaskPriority.CRITICAL -> MissionPriority.CRITICAL
            }
            val targetVal = meta?.targetValueOverride ?: 1
            val currentVal = if (status == MissionStatus.COMPLETED) targetVal else 0
            items.add(
                UnifiedMissionItem(
                    missionId = id,
                    sourceType = MissionSourceType.PERSONAL_TASK,
                    sourceRecordId = task.id,
                    title = task.title,
                    description = task.description.ifBlank { "${task.category.label} • Est ${task.estimatedMinutes}m" },
                    category = category,
                    module = module,
                    status = status,
                    priority = priority,
                    currentValue = currentVal,
                    targetValue = targetVal,
                    unit = meta?.unitOverride ?: "task",
                    startDateEpochDay = meta?.startDateEpochDay,
                    deadlineEpochDay = deadlineDay,
                    completedAtEpochMs = task.completedAtEpochMs,
                    prerequisiteMissionIds = meta?.prerequisiteMissionIds.orEmpty(),
                    executeRoute = if (module == MissionSourceModule.GENERAL) "tasks" else module.executeRoute,
                    canEdit = true,
                    canPause = !status.isTerminal,
                    canCompleteDirectly = status != MissionStatus.COMPLETED && status != MissionStatus.ARCHIVED,
                    canArchive = status != MissionStatus.ARCHIVED,
                    isDemandingPhysical = module == MissionSourceModule.PHYSICAL
                )
            )
        }

        // 3. Life Goals
        core.goals.forEach { goal ->
            val id = "goal:${goal.id}"
            val meta = aux.metadata[id]
            val goalMilestones = core.milestones.filter { it.goalId == goal.id }
            val goalDetails = GoalEngine.buildGoalWithDetails(goal, goalMilestones, emptyList(), core.tasks, today)
            val status = when {
                meta?.isArchivedOverride == true || goal.status == GoalStatus.ARCHIVED -> MissionStatus.ARCHIVED
                goal.status == GoalStatus.COMPLETED -> MissionStatus.COMPLETED
                goal.status == GoalStatus.PAUSED || meta?.isPaused == true -> MissionStatus.PAUSED
                goalDetails.computedProgressPercent > 0 -> MissionStatus.ACTIVE
                else -> MissionStatus.NOT_STARTED
            }
            val category = when (goal.timeframe) {
                GoalTimeframe.DAILY -> MissionCategory.DAILY
                GoalTimeframe.WEEKLY -> MissionCategory.WEEKLY
                else -> MissionCategory.LONG_TERM
            }
            val module = meta?.moduleName?.let { runCatching { MissionSourceModule.valueOf(it) }.getOrNull() }
                ?: MissionSourceModule.LIFE
            val totalMs = goalMilestones.size.coerceAtLeast(1)
            val doneMs = if (goalMilestones.isEmpty()) {
                if (status == MissionStatus.COMPLETED) 1 else 0
            } else {
                goalMilestones.count { it.isCompleted }
            }
            items.add(
                UnifiedMissionItem(
                    missionId = id,
                    sourceType = MissionSourceType.LIFE_GOAL,
                    sourceRecordId = goal.id,
                    title = goal.title,
                    description = goal.description.ifBlank { goal.completionCriteria.ifBlank { "${goal.timeframe.label} goal" } },
                    category = category,
                    module = module,
                    status = status,
                    priority = if (goalDetails.isBehindSchedule) MissionPriority.HIGH else MissionPriority.MEDIUM,
                    currentValue = doneMs,
                    targetValue = totalMs,
                    unit = if (goalMilestones.isEmpty()) "%" else "milestones",
                    startDateEpochDay = meta?.startDateEpochDay ?: goal.startDateEpochDay,
                    deadlineEpochDay = meta?.deadlineEpochDayOverride ?: goal.targetDateEpochDay,
                    prerequisiteMissionIds = meta?.prerequisiteMissionIds.orEmpty(),
                    executeRoute = "goals",
                    canEdit = true,
                    canPause = !status.isTerminal,
                    canCompleteDirectly = status != MissionStatus.COMPLETED && status != MissionStatus.ARCHIVED,
                    canArchive = status != MissionStatus.ARCHIVED,
                    isDemandingPhysical = module == MissionSourceModule.PHYSICAL
                )
            )
        }

        // 4. Academic Quests
        aux.academicQuests.forEach { aq ->
            val id = "acad_quest:${aq.id}"
            val meta = aux.metadata[id]
            val status = when {
                meta?.isArchivedOverride == true || aq.status == AcademicQuestStatus.CANCELLED -> MissionStatus.ARCHIVED
                aq.status == AcademicQuestStatus.COMPLETED -> MissionStatus.COMPLETED
                meta?.isPaused == true -> MissionStatus.PAUSED
                aq.currentValue > 0 -> MissionStatus.ACTIVE
                else -> MissionStatus.NOT_STARTED
            }
            val category = when (aq.category.name) {
                "DAILY_STUDY", "REVISION" -> MissionCategory.DAILY
                "WEEKLY_STUDY" -> MissionCategory.WEEKLY
                else -> MissionCategory.LONG_TERM
            }
            val deadlineDay = meta?.deadlineEpochDayOverride ?: aq.deadlineEpochMs?.let {
                Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate().toEpochDay()
            }
            items.add(
                UnifiedMissionItem(
                    missionId = id,
                    sourceType = MissionSourceType.ACADEMIC_QUEST,
                    sourceRecordId = aq.id,
                    title = aq.title,
                    description = aq.objective.ifBlank { aq.description },
                    category = category,
                    module = MissionSourceModule.ACADEMIC,
                    status = status,
                    priority = MissionPriority.HIGH,
                    currentValue = aq.currentValue,
                    targetValue = aq.targetValue,
                    unit = aq.unit,
                    startDateEpochDay = meta?.startDateEpochDay,
                    deadlineEpochDay = deadlineDay,
                    deadlineEpochMs = aq.deadlineEpochMs,
                    completedAtEpochMs = aq.completedAtEpochMs,
                    prerequisiteMissionIds = meta?.prerequisiteMissionIds.orEmpty(),
                    executeRoute = "academic_quests",
                    canEdit = false,
                    canPause = !status.isTerminal,
                    canCompleteDirectly = false,
                    canArchive = true,
                    xpRewardHint = aq.xpReward,
                    isDemandingPhysical = false
                )
            )
        }

        // 5. Today's Physical Conditioning Protocol (adapter when jogging target > 0)
        val physTarget = aux.physicalState.effectiveJoggingTarget
        if (physTarget > 0) {
            val id = "physical:daily_conditioning"
            val meta = aux.metadata[id]
            if (meta?.isArchivedOverride != true) {
                val physStatus = when (aux.physicalState.workoutStatus) {
                    WorkoutStatus.COMPLETED -> MissionStatus.COMPLETED
                    WorkoutStatus.REST_DAY -> MissionStatus.PAUSED
                    WorkoutStatus.IN_PROGRESS -> MissionStatus.ACTIVE
                    WorkoutStatus.PENDING -> MissionStatus.NOT_STARTED
                }
                items.add(
                    UnifiedMissionItem(
                        missionId = id,
                        sourceType = MissionSourceType.PHYSICAL_PROTOCOL,
                        sourceRecordId = 1L,
                        title = "Daily Aerobic & Strength Protocol",
                        description = "Verified jogging target (${aux.physicalState.completedJoggingMinutesToday}/$physTarget min) & physical training.",
                        category = MissionCategory.DAILY,
                        module = MissionSourceModule.PHYSICAL,
                        status = physStatus,
                        priority = MissionPriority.HIGH,
                        currentValue = aux.physicalState.completedJoggingMinutesToday.coerceAtMost(physTarget),
                        targetValue = physTarget,
                        unit = "min",
                        deadlineEpochDay = today,
                        prerequisiteMissionIds = meta?.prerequisiteMissionIds.orEmpty(),
                        executeRoute = "physical",
                        canEdit = false,
                        canPause = false,
                        canCompleteDirectly = false,
                        canArchive = false,
                        isDemandingPhysical = true
                    )
                )
            }
        }

        return MissionPrioritizationEngine.evaluateSnapshot(
            rawMissions = items,
            recovery = aux.recovery,
            todayEpochDay = today,
            nowEpochMs = nowEpochMs
        )
    }

    private suspend fun getMetadataOverride(missionId: String): MissionMetadataOverride? {
        val ownerId = activeAccountOwnerIdFlow.firstOrNull()?.ifBlank { "local_player" } ?: "local_player"
        val map = decodeMetadata(preferencesRepository.observeMissionMetadataJson(ownerId).first())
        return map[missionId]
    }

    private suspend fun saveMetadataOverride(override: MissionMetadataOverride) {
        val ownerId = activeAccountOwnerIdFlow.firstOrNull()?.ifBlank { "local_player" } ?: "local_player"
        val currentMap = decodeMetadata(preferencesRepository.observeMissionMetadataJson(ownerId).first()).toMutableMap()
        currentMap[override.missionId] = override
        val encoded = json.encodeToString(MissionMetadataStorePayload(currentMap))
        preferencesRepository.saveMissionMetadataJson(ownerId, encoded)
    }

    private fun decodeMetadata(raw: String): Map<String, MissionMetadataOverride> {
        if (raw.isBlank()) return emptyMap()
        return runCatching {
            json.decodeFromString<MissionMetadataStorePayload>(raw).items
        }.getOrDefault(emptyMap())
    }

    private fun QuestType.toMissionCategory(): MissionCategory = when (this) {
        QuestType.DAILY -> MissionCategory.DAILY
        QuestType.WEEKLY -> MissionCategory.WEEKLY
        QuestType.LONG_TERM -> MissionCategory.LONG_TERM
    }

    private fun QuestCategory.toMissionModule(): MissionSourceModule = when (this) {
        QuestCategory.PHYSICAL, QuestCategory.RECOVERY -> MissionSourceModule.PHYSICAL
        QuestCategory.INTELLECT -> MissionSourceModule.ACADEMIC
        QuestCategory.DISCIPLINE, QuestCategory.SOCIAL -> MissionSourceModule.LIFE
    }
}
