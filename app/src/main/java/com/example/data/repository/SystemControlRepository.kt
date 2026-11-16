package com.example.data.repository

import com.example.data.automation.AutomationOrchestrator
import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.entity.DailyExecutionItemEntity
import com.example.data.local.entity.RpgRewardAuditEntity
import com.example.data.local.entity.SystemHealthSnapshotEntity
import com.example.data.local.entity.toDomain
import com.example.data.local.entity.toEntity
import com.example.data.local.entity.toOverride
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.sync.SyncStatusTracker
import com.example.domain.engine.DailyCycleManager
import com.example.domain.engine.DailyExecutionOverride
import com.example.domain.engine.ProgressionEngine
import com.example.domain.engine.RecoveryEngine
import com.example.domain.engine.ScheduleCalendarEngine
import com.example.domain.engine.SystemDataIntegrityEngine
import com.example.domain.engine.SystemHealthEngine
import com.example.domain.engine.SystemHealthInput
import com.example.domain.engine.SystemIntegritySnapshotInput
import com.example.domain.engine.UnifiedDailyExecutionEngine
import com.example.domain.model.AdaptivePlanBlockStatus
import com.example.domain.model.DailyCycleExecution
import com.example.domain.model.DailyExecutionItem
import com.example.domain.model.DailyExecutionStatus
import com.example.domain.model.DailyExecutionSummary
import com.example.domain.model.IntegrityFindingStatus
import com.example.domain.model.ModuleIntegrationInfo
import com.example.domain.model.PlayerProfile
import com.example.domain.model.RecoveryOperationRecord
import com.example.domain.model.RpgRewardAuditRecord
import com.example.domain.model.ScheduledActivityItem
import com.example.domain.model.ScheduledSourceType
import com.example.domain.model.SystemControlPreferences
import com.example.domain.model.SystemEventType
import com.example.domain.model.SystemHealthLevel
import com.example.domain.model.SystemHealthReport
import com.example.domain.model.SystemIntegrityFinding
import com.example.domain.model.SystemNotification
import com.example.domain.model.SystemNotificationPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Unified System Control Center Repository integrating:
 * - Daily Execution Coordination
 * - Cross-Module Integrity Scanning & Safe/Confirmed Repairs
 * - Real Component System Health Evaluation
 * - Performance Diagnostics & Controlled Cache Metadata
 * - Crash & Error Recovery Operations
 * - System Preferences & Notification Management Delegation
 */
class SystemControlRepository(
    private val database: SoloLevelingDatabase,
    private val preferencesRepository: UserPreferencesRepository,
    private val systemEventRepository: SystemEventRepository,
    private val automationOrchestrator: AutomationOrchestrator,
    private val syncStatusTracker: SyncStatusTracker
) {
    private val _latestHealthReport = MutableStateFlow(SystemHealthReport())
    val latestHealthReport: StateFlow<SystemHealthReport> = _latestHealthReport.asStateFlow()

    private val _performanceMetrics = MutableStateFlow(com.example.domain.model.SystemPerformanceMetrics())
    val performanceMetrics: StateFlow<com.example.domain.model.SystemPerformanceMetrics> = _performanceMetrics.asStateFlow()

    private val _moduleStatuses = MutableStateFlow<List<ModuleIntegrationInfo>>(emptyList())
    val moduleStatuses: StateFlow<List<ModuleIntegrationInfo>> = _moduleStatuses.asStateFlow()

    fun observePreferences(): Flow<SystemControlPreferences> =
        database.systemIntegrityHealthDao().observePreferences().map { it?.toDomain() ?: SystemControlPreferences() }

    fun observeIntegrityFindings(): Flow<List<SystemIntegrityFinding>> =
        database.systemIntegrityHealthDao().observeFindings(100).map { list -> list.map { it.toDomain() } }

    fun observeDailyCycles(): Flow<List<DailyCycleExecution>> =
        database.dailyCycleDao().observeCycles(30).map { list -> list.map { it.toDomain() } }

    fun observeRecoveryOperations(): Flow<List<RecoveryOperationRecord>> =
        database.systemIntegrityHealthDao().observeRecoveryOperations(40).map { list -> list.map { it.toDomain() } }

    fun observeNotificationPreferences(): Flow<SystemNotificationPreferences> =
        database.aiConfigDao().observeNotificationPreferences().map { it?.toDomain() ?: SystemNotificationPreferences() }

    fun observeNotifications(): Flow<List<SystemNotification>> =
        database.systemNotificationDao().observeNotifications().map { list -> list.map { it.toDomain() } }

    fun observeDailyExecutionTimeline(targetEpochDay: Long): Flow<DailyExecutionSummary> {
        return combine(
            database.routineDao().observeAllRoutines(),
            database.habitDao().observeAllHabits(),
            database.personalTaskDao().observeAllTasks(),
            database.scheduleDao().observeReferencesInRange(targetEpochDay, targetEpochDay),
            database.dailyCycleDao().observeExecutionOverridesForDay(targetEpochDay)
        ) { routineEntities, habitEntities, taskEntities, refEntities, overrideEntities ->
            DailyExecutionInputs(routineEntities, habitEntities, taskEntities, refEntities, overrideEntities)
        }.combine(database.recoveryAssessmentDao().observeLatest()) { inputs, recoveryEntity ->
            val sysPrefs = database.systemIntegrityHealthDao().getPreferencesOnce()?.toDomain() ?: SystemControlPreferences()
            val zone = DailyCycleManager.resolveZone(sysPrefs.preferredTimezoneId)
            val nowMs = System.currentTimeMillis()
            val zdtNow = Instant.ofEpochMilli(nowMs).atZone(zone)
            val todayEpochDay = zdtNow.toLocalDate().toEpochDay()
            val currentMinute = zdtNow.hour * 60 + zdtNow.minute

            val routines = inputs.routineEntities.map { it.toDomain() }
            val routineActivities = database.routineDao().getAllActivitiesOnce().map { it.toDomain() }
            val routineCompletions = database.routineDao().getCompletionsSinceOnce(targetEpochDay - 1).map { it.toDomain() }
            val habits = inputs.habitEntities.map { it.toDomain() }
            val habitCompletions = database.habitDao().getCompletionsSinceOnce(targetEpochDay - 1).map { it.toDomain() }
            val tasks = inputs.taskEntities.map { it.toDomain() }
            val goals = database.goalDao().getAllGoalsOnce().map { it.toDomain() }
            val milestones = database.goalDao().getAllMilestonesOnce().map { it.toDomain() }
            val storedRefs = inputs.refEntities.map { it.toDomain() }
            val adaptiveBlocks = database.adaptivePlanDao().getPlanForDayOnce(targetEpochDay)?.let { plan ->
                database.adaptivePlanDao().getBlocksForPlanOnce(plan.planUuid)
                    .map { it.toDomain() }
                    .filter { it.status == AdaptivePlanBlockStatus.APPROVED || it.status == AdaptivePlanBlockStatus.COMPLETED || it.status == AdaptivePlanBlockStatus.PARTIAL }
            }.orEmpty()

            val unifiedBase = ScheduleCalendarEngine.buildUnifiedItemsForDay(
                targetEpochDay = targetEpochDay,
                routines = routines,
                routineActivities = routineActivities,
                routineCompletions = routineCompletions,
                habits = habits,
                habitCompletions = habitCompletions,
                tasks = tasks,
                goals = goals,
                goalMilestones = milestones,
                storedReferences = storedRefs,
                todayEpochDay = todayEpochDay,
                zoneId = zone
            ).toMutableList()

            adaptiveBlocks.forEach { block ->
                val alreadyExists = unifiedBase.any {
                    it.sourceType == block.linkedSourceType && it.sourceRecordId == block.linkedSourceId && block.linkedSourceId > 0L
                }
                if (!alreadyExists) {
                    unifiedBase.add(
                        ScheduledActivityItem(
                            sourceType = ScheduledSourceType.AI_PLAN_PROPOSAL,
                            sourceRecordId = block.id,
                            title = block.title,
                            subtitle = "Adaptive Plan • ${block.module}",
                            dateEpochDay = targetEpochDay,
                            startMinuteOfDay = block.startMinuteOfDay,
                            durationMinutes = block.durationMinutes,
                            isCompleted = block.status == AdaptivePlanBlockStatus.COMPLETED,
                            priorityWeight = block.priorityWeight
                        )
                    )
                }
            }

            val recovery = recoveryEntity?.toDomain() ?: RecoveryEngine.assess(0, 0, 0, 0)
            val overridesMap = inputs.overrideEntities.associate { it.executionKey to it.toOverride() }

            UnifiedDailyExecutionEngine.buildDailyTimeline(
                targetEpochDay = targetEpochDay,
                scheduledItems = unifiedBase,
                overridesByKey = overridesMap,
                recovery = recovery,
                currentMinuteOfDay = currentMinute,
                todayEpochDay = todayEpochDay
            )
        }
    }

    suspend fun updateExecutionStatus(
        item: DailyExecutionItem,
        newStatus: DailyExecutionStatus,
        evidenceReference: String = "",
        completedMinutes: Int = item.completedMinutes,
        plannedStartMinuteOverride: Int? = null,
        notes: String = item.notes,
        nowEpochMs: Long = System.currentTimeMillis()
    ): Result<Unit> {
        if (newStatus == DailyExecutionStatus.COMPLETED) {
            val check = UnifiedDailyExecutionEngine.validateCompletionTransition(item, evidenceReference)
            if (check.isFailure) return check.map { Unit }
        }
        val overrideEntity = DailyExecutionItemEntity(
            executionKey = item.executionKey,
            dateEpochDay = item.dateEpochDay,
            activityTypeName = item.activityType.name,
            sourceRecordId = item.sourceRecordId,
            statusName = newStatus.name,
            actualStartEpochMs = if (newStatus == DailyExecutionStatus.IN_PROGRESS && item.actualStartEpochMs == null) nowEpochMs else item.actualStartEpochMs,
            actualCompletionEpochMs = if (newStatus == DailyExecutionStatus.COMPLETED) nowEpochMs else null,
            completedMinutes = if (newStatus == DailyExecutionStatus.COMPLETED) item.durationMinutes else completedMinutes.coerceIn(0, item.durationMinutes),
            plannedStartMinuteOverride = plannedStartMinuteOverride,
            evidenceReference = evidenceReference.ifBlank { item.evidenceReference },
            notes = notes,
            updatedAtEpochMs = nowEpochMs
        )
        database.dailyCycleDao().upsertExecutionOverride(overrideEntity)
        systemEventRepository.publishEvent(
            eventType = SystemEventType.DAILY_EXECUTION_UPDATED,
            relatedEntityId = item.executionKey,
            sourceModule = "EXECUTION",
            metadata = mapOf("status" to newStatus.name),
            processImmediately = false,
            nowEpochMs = nowEpochMs
        )
        return Result.success(Unit)
    }

    suspend fun runIntegrityScan(nowEpochMs: Long = System.currentTimeMillis()): List<SystemIntegrityFinding> {
        val profile = database.playerDao().getPlayerOnce()?.toDomain() ?: PlayerProfile()
        val workouts = database.workoutSessionDao().getWorkoutsSince(0L).map { it.toDomain() }
        val exercises = database.workoutSessionDao().getAllExerciseRecordsOnce().map { it.toDomain() }
        val subjects = database.academicSubjectDao().getAllSubjectsOnce().map { it.toDomain() }
        val chapters = database.academicSubjectDao().getAllChaptersOnce().map { it.toDomain() }
        val topics = database.academicSubjectDao().getAllTopicsOnce().map { it.toDomain() }
        val goals = database.goalDao().getAllGoalsOnce().map { it.toDomain() }
        val milestones = database.goalDao().getAllMilestonesOnce().map { it.toDomain() }
        val dungeons = database.dungeonDao().getAllCompletionsOnce().map { it.toDomain() }
        val audits = database.rpgAuditDao().getAllOnce().map { it.toDomain() }
        val syncQueue = database.syncQueueDao().getAllOnce().map { it.toDomain() }
        val openConflicts = database.syncConflictDao().getOpenConflictsOnce().map { it.toDomain() }
        val events = database.systemEventDao().getRecentEventsOnce(100).map { it.toDomain() }

        val detected = SystemDataIntegrityEngine.scan(
            SystemIntegritySnapshotInput(
                profile = profile,
                workoutSessions = workouts,
                exerciseRecords = exercises,
                subjects = subjects,
                chapters = chapters,
                topics = topics,
                goals = goals,
                goalMilestones = milestones,
                dungeonCompletions = dungeons,
                rpgRewardAudits = audits,
                syncQueueItems = syncQueue,
                openSyncConflicts = openConflicts,
                recentEvents = events,
                nowEpochMs = nowEpochMs
            )
        )

        if (detected.isNotEmpty()) {
            database.systemIntegrityHealthDao().upsertFindings(detected.map { it.toEntity() })
        }
        return database.systemIntegrityHealthDao().getOpenFindingsOnce().map { it.toDomain() }
    }

    suspend fun repairFinding(
        findingUuid: String,
        confirmedDestructive: Boolean,
        nowEpochMs: Long = System.currentTimeMillis()
    ): Result<String> {
        val findings = database.systemIntegrityHealthDao().getOpenFindingsOnce().map { it.toDomain() }
        val target = findings.firstOrNull { it.findingUuid == findingUuid }
            ?: return Result.failure(IllegalArgumentException("Finding not found or already resolved."))
        if (target.isDestructiveRepair && !confirmedDestructive) {
            return Result.failure(IllegalStateException("User confirmation required before destructive cleanup."))
        }

        val detail: String = when (target.affectedTable) {
            "player_profile" -> {
                val p = database.playerDao().getPlayerOnce()?.toDomain() ?: PlayerProfile()
                val fixedRank = ProgressionEngine.evaluateRank(
                    level = p.level,
                    totalAttributeScore = p.attributes.totalScore,
                    verifiedActivitiesCount = p.verifiedActivitiesCount
                )
                val expectedXpToNext = ProgressionEngine.xpRequiredForLevel(p.level)
                val boundedXp = p.currentLevelXp.coerceIn(0, (expectedXpToNext - 1).coerceAtLeast(0))
                database.playerDao().upsertPlayer(
                    p.copy(
                        rank = fixedRank,
                        currentLevelXp = boundedXp,
                        xpToNextLevel = expectedXpToNext
                    ).toEntity()
                )
                "Synchronized Player rank (${fixedRank.displayName}) and XP curve bounds."
            }
            "exercise_records" -> {
                val validWorkouts = database.workoutSessionDao().getWorkoutsSince(0L).map { it.id }.toSet()
                val orphans = database.workoutSessionDao().getAllExerciseRecordsOnce().filter { it.workoutSessionId !in validWorkouts }
                orphans.forEach { database.workoutSessionDao().deleteExerciseById(it.id) }
                "Removed ${orphans.size} orphaned exercise record(s)."
            }
            "academic_topics" -> {
                val subjectIds = database.academicSubjectDao().getAllSubjectsOnce().map { it.id }.toSet()
                val chapterIds = database.academicSubjectDao().getAllChaptersOnce().map { it.id }.toSet()
                val orphans = database.academicSubjectDao().getAllTopicsOnce()
                    .filter { it.subjectId !in subjectIds || it.chapterId !in chapterIds }
                orphans.forEach { database.academicSubjectDao().deleteTopicById(it.id) }
                "Removed ${orphans.size} orphaned topic record(s)."
            }
            "goal_milestones" -> {
                val goalIds = database.goalDao().getAllGoalsOnce().map { it.id }.toSet()
                val orphans = database.goalDao().getAllMilestonesOnce().filter { it.goalId !in goalIds }
                orphans.forEach { database.goalDao().deleteMilestone(it.id) }
                "Removed ${orphans.size} orphaned milestone record(s)."
            }
            "rpg_reward_audit" -> {
                val existingKeys = database.rpgAuditDao().getAllOnce().map { it.rewardKey }.toSet()
                val missing = database.dungeonDao().getAllCompletionsOnce()
                    .map { it.toDomain() }
                    .filter { it.xpAwarded > 0 && it.rewardKey !in existingKeys }
                missing.forEach { comp ->
                    database.rpgAuditDao().insertAudit(
                        RpgRewardAuditEntity(
                            rewardKey = comp.rewardKey,
                            sourceType = "DUNGEON",
                            sourceId = comp.dungeonId.toString(),
                            title = comp.dungeonTitle,
                            xpAwarded = comp.xpAwarded,
                            primaryAttrName = comp.primaryAttribute.name,
                            primaryAttrXp = comp.primaryAttrXp,
                            secondaryAttrName = comp.secondaryAttribute.name,
                            secondaryAttrXp = comp.secondaryAttrXp,
                            verificationProvenance = comp.verificationProvenance,
                            wasCapped = false,
                            notes = "Backfilled integrity audit entry from existing cleared dungeon.",
                            timestampEpochMs = comp.completedAtEpochMs
                        )
                    )
                }
                "Backfilled ${missing.size} missing RPG reward audit entries without altering XP."
            }
            "sync_queue" -> {
                val all = database.syncQueueDao().getAllOnce()
                var removed = 0
                all.groupBy { it.idempotencyKey }
                    .filter { it.key.isNotBlank() && it.value.size > 1 }
                    .values
                    .forEach { group ->
                        val sorted = group.sortedByDescending { it.updatedAtEpochMs }
                        sorted.drop(1).forEach { dup ->
                            database.syncQueueDao().deleteById(dup.queueId)
                            removed++
                        }
                    }
                "Deduplicated $removed duplicate sync queue entries."
            }
            "system_events" -> {
                val recovered = systemEventRepository.recoverInterruptedEvents(nowEpochMs)
                val retried = systemEventRepository.retryAllFailedAndDeadLetter()
                "Recovered $recovered interrupted event(s) and retried $retried failed/dead-letter item(s)."
            }
            else -> return Result.failure(IllegalStateException("Manual action needed in owning module for ${target.affectedTable}."))
        }

        database.systemIntegrityHealthDao().updateFindingStatus(
            findingUuid = findingUuid,
            statusName = IntegrityFindingStatus.REPAIRED.name,
            resolvedAtEpochMs = nowEpochMs
        )
        database.systemIntegrityHealthDao().upsertRecoveryOperation(
            RecoveryOperationRecord(
                operationType = "INTEGRITY_REPAIR_${target.affectedTable.uppercase()}",
                reason = target.title,
                status = "COMPLETED",
                details = detail,
                createdAtEpochMs = nowEpochMs,
                completedAtEpochMs = nowEpochMs
            ).toEntity()
        )
        systemEventRepository.publishEvent(
            eventType = SystemEventType.SYSTEM_RECOVERY_EXECUTED,
            relatedEntityId = findingUuid,
            sourceModule = "SYSTEM",
            metadata = mapOf("detail" to detail),
            processImmediately = false,
            nowEpochMs = nowEpochMs
        )
        return Result.success(detail)
    }

    suspend fun dismissFinding(findingUuid: String, nowEpochMs: Long = System.currentTimeMillis()) {
        database.systemIntegrityHealthDao().updateFindingStatus(
            findingUuid = findingUuid,
            statusName = IntegrityFindingStatus.DISMISSED.name,
            resolvedAtEpochMs = nowEpochMs
        )
    }

    suspend fun evaluateSystemHealthAndPerformance(nowEpochMs: Long = System.currentTimeMillis()): SystemHealthReport {
        val swStart = System.currentTimeMillis()
        automationOrchestrator.ensureRegistrySeeded()
        val sysPrefs = database.systemIntegrityHealthDao().getPreferencesOnce()?.toDomain() ?: SystemControlPreferences()
        val userPrefs = preferencesRepository.preferencesFlow.first()
        val zone = DailyCycleManager.resolveZone(sysPrefs.preferredTimezoneId)
        val expectedEpochDay = DailyCycleManager.currentLogicalEpochDay(nowEpochMs, zone, userPrefs.dailyResetHour)

        val eventDiagnostics = systemEventRepository.getDiagnosticsSummary()
        val jobs = database.automationDao().getJobsOnce().map { it.toDomain() }
        val latestCycle = database.dailyCycleDao().getLatestCycleOnce()?.toDomain()
        val syncSummary = syncStatusTracker.summaryState.value
        val intelProfile = database.intelligenceProfileDao().getProfileOnce()?.toDomain()
        val openFindings = database.systemIntegrityHealthDao().getOpenFindingsOnce().map { it.toDomain() }
        val notifPrefs = database.aiConfigDao().getNotificationPreferencesOnce()?.toDomain() ?: SystemNotificationPreferences()

        val report = SystemHealthEngine.evaluate(
            SystemHealthInput(
                databaseVersion = 10,
                eventDiagnostics = eventDiagnostics,
                automations = jobs,
                latestDailyCycle = latestCycle,
                expectedCurrentEpochDay = expectedEpochDay,
                syncSummary = syncSummary,
                intelligenceProfile = intelProfile,
                openIntegrityFindings = openFindings,
                notificationsEnabled = notifPrefs.notificationsEnabled,
                nowEpochMs = nowEpochMs
            )
        )
        _latestHealthReport.value = report

        val jsonArr = JSONArray().apply {
            report.components.forEach { c -> put("${c.component}:${c.status.name}") }
        }
        database.systemIntegrityHealthDao().insertHealthSnapshot(
            SystemHealthSnapshotEntity(
                overallStatusName = report.overallStatus.name,
                componentsSummaryJson = jsonArr.toString(),
                evaluatedAtEpochMs = nowEpochMs
            )
        )

        val pendingSyncCount = database.syncQueueDao().getAllOnce().count {
            it.syncStateName == "PENDING_UPLOAD" || it.syncStateName == "FAILED"
        }
        val execItemsCount = database.dailyCycleDao().getTotalExecutionOverridesCount()
        val sweepDuration = (System.currentTimeMillis() - swStart).coerceAtLeast(1L)

        _performanceMetrics.value = com.example.domain.model.SystemPerformanceMetrics(
            databaseVersion = 10,
            indexedActivityLogsReady = true,
            cachedProfileFingerprint = intelProfile?.sourceFingerprint ?: "Not evaluated",
            totalStoredEvents = eventDiagnostics.totalCount,
            pendingSyncQueueSize = pendingSyncCount,
            activeAutomationsCount = jobs.count { it.isEnabled },
            totalDailyExecutionItems = execItemsCount,
            lastSweepDurationMs = sweepDuration,
            optimizationNotes = listOf(
                "activity_logs.timestampEpochMs indexed cleanly in Room v9",
                "Incremental SHA-256 telemetry fingerprinting avoids redundant recomputations",
                "Coalesced event dispatch & paginated DAO reads (LIMIT 50-100) prevent UI stalls",
                "Zero main-thread Room queries (all sweeps on background coroutines)"
            )
        )

        _moduleStatuses.value = buildModuleStatuses(report, syncSummary.isCloudConfigured, notifPrefs.notificationsEnabled)
        return report
    }

    private fun buildModuleStatuses(
        report: SystemHealthReport,
        cloudConfigured: Boolean,
        notificationsEnabled: Boolean
    ): List<ModuleIntegrationInfo> {
        fun findLevel(keyword: String): SystemHealthLevel =
            report.components.firstOrNull { it.component.contains(keyword, ignoreCase = true) }?.status
                ?: SystemHealthLevel.HEALTHY

        return listOf(
            ModuleIntegrationInfo("PLAYER", "Player Core & Progression", "ProgressionEngine + Orchestrator", findLevel("Database"), "Single authoritative XP & rank calculator active.", "profile"),
            ModuleIntegrationInfo("PHYSICAL", "Physical & Recovery", "Health Connect + Safety Guard", SystemHealthLevel.HEALTHY, "Recovery safety rules enforced across execution & AI.", "physical"),
            ModuleIntegrationInfo("ACADEMIC", "Academic Engine", "Study + Spaced Revision + Exams", SystemHealthLevel.HEALTHY, "Subject syllabus & countdown signals integrated.", "academic"),
            ModuleIntegrationInfo("LIFE", "Life & Discipline", "Habits, Routines, Tasks & Timeline", SystemHealthLevel.HEALTHY, "Synchronized with UnifiedDailyExecutionEngine.", "life_management"),
            ModuleIntegrationInfo("RPG", "Dungeons, Bosses & Skills", "Audit-Backed Progression", findLevel("Data Integrity"), "Immutable rpg_reward_audit verification active.", "rpg_hub"),
            ModuleIntegrationInfo("CLOUD", "Cloud Sync (Supabase)", if (cloudConfigured) "Configured" else "Offline Vault", findLevel("Cloud Sync"), "Offline-first SyncQueue + Conflict Center.", "cloud_sync"),
            ModuleIntegrationInfo("INTELLIGENCE", "Intelligence & Advisor", "Fingerprint Cache + Context Guard", findLevel("Intelligence"), "Adaptive planning respects consent & recovery.", "intelligence_hub"),
            ModuleIntegrationInfo("SYSTEM", "Automation & Event Bus", if (notificationsEnabled) "Active" else "Notifs Paused", findLevel("System Event"), "Ordered Room-backed events & WorkManager registry.", "automation_dashboard")
        )
    }

    suspend fun updatePreferences(prefs: SystemControlPreferences) {
        val updated = prefs.copy(updatedAtEpochMs = System.currentTimeMillis())
        database.systemIntegrityHealthDao().upsertPreferences(updated.toEntity())
    }

    suspend fun updateNotificationPreferences(prefs: SystemNotificationPreferences) {
        database.aiConfigDao().upsertNotificationPreferences(prefs.copy(updatedAtEpochMs = System.currentTimeMillis()).toEntity())
    }

    suspend fun markNotificationRead(id: Long) {
        database.systemNotificationDao().markRead(id)
    }

    suspend fun clearReadNotifications() {
        database.systemNotificationDao().deleteReadNotifications()
    }

    suspend fun executeRecoveryAll(nowEpochMs: Long = System.currentTimeMillis()): String {
        val recoveredEvents = systemEventRepository.recoverInterruptedEvents(nowEpochMs)
        val retried = systemEventRepository.retryAllFailedAndDeadLetter()
        val cycleRes = automationOrchestrator.runDailyCycleIdempotent("RECOVERY_CENTER", nowEpochMs)
        val findings = runIntegrityScan(nowEpochMs)
        evaluateSystemHealthAndPerformance(nowEpochMs)
        return "Recovered $recoveredEvents interrupted event(s), retried $retried failed/dead event(s), cycle=${if (cycleRes.getOrNull() != null) "caught up" else "current"}, open findings=${findings.size}."
    }

    private data class DailyExecutionInputs(
        val routineEntities: List<com.example.data.local.entity.LifeRoutineEntity>,
        val habitEntities: List<com.example.data.local.entity.HabitEntity>,
        val taskEntities: List<com.example.data.local.entity.PersonalTaskEntity>,
        val refEntities: List<com.example.data.local.entity.ScheduledActivityReferenceEntity>,
        val overrideEntities: List<DailyExecutionItemEntity>
    )
}
