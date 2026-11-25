package com.example.data.repository

import com.example.data.integration.AiStreamEvent
import com.example.data.integration.BackendAiGateway
import com.example.data.integration.BackendAiProxyRequest
import com.example.data.integration.HttpBackendAiGateway
import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.entity.AiRequestAuditEntity
import com.example.data.local.entity.IntelligencePreferencesEntity
import com.example.data.local.entity.ScheduledActivityReferenceEntity
import com.example.data.local.entity.toDomain
import com.example.data.local.entity.toEntity
import com.example.data.preferences.UserPreferencesRepository
import com.example.domain.engine.AcademicLearningIntelligenceEngine
import com.example.domain.engine.AdaptiveDungeonDifficultyEngine
import com.example.domain.engine.AdaptivePlanningEngine
import com.example.domain.engine.GoalEngine
import com.example.domain.engine.IntelligenceReportEngine
import com.example.domain.engine.IntelligenceTelemetryInput
import com.example.domain.engine.LongTermDevelopmentPlanner
import com.example.domain.engine.PersonalDevelopmentAdvisor
import com.example.domain.engine.PhysicalDevelopmentIntelligenceEngine
import com.example.domain.engine.PlayerIntelligenceEngine
import com.example.domain.engine.PredictiveConsistencyEngine
import com.example.domain.engine.RecommendationEngine
import com.example.domain.engine.RecoveryEngine
import com.example.domain.engine.ScheduleCalendarEngine
import com.example.domain.engine.StrengthsAndWeaknessesEngine
import com.example.domain.engine.WorkloadBalanceEngine
import com.example.domain.model.AcademicLearningIntelligence
import com.example.domain.model.AdaptiveDailyPlan
import com.example.domain.model.AdaptiveDungeonRecommendation
import com.example.domain.model.AdaptivePlanBlock
import com.example.domain.model.AdaptivePlanBlockStatus
import com.example.domain.model.AdvisorConsultationResult
import com.example.domain.model.AiConnectionStatus
import com.example.domain.model.AiConsentState
import com.example.domain.model.AiModelPreferenceState
import com.example.domain.model.ConsistencyPrediction
import com.example.domain.model.DevelopmentRoadmap
import com.example.domain.model.GoalAnalysisInsight
import com.example.domain.model.IntelligenceReport
import com.example.domain.model.PersonalizedRecommendation
import com.example.domain.model.PhysicalDevelopmentIntelligence
import com.example.domain.model.PhysicalDevelopmentState
import com.example.domain.model.PlayerIntelligencePreferences
import com.example.domain.model.PlayerIntelligenceProfile
import com.example.domain.model.PlayerProfile
import com.example.domain.model.RecommendationFeedbackRecord
import com.example.domain.model.RecommendationStatus
import com.example.domain.model.ReportPeriodType
import com.example.domain.model.RoadmapHorizon
import com.example.domain.model.ScheduledSourceType
import com.example.domain.model.StrengthWeaknessFinding
import com.example.domain.model.SystemEventType
import com.example.domain.model.WeeklyComparisonDelta
import com.example.domain.model.WorkloadAssessment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

data class IntelligenceDomainInsights(
    val academic: AcademicLearningIntelligence = AcademicLearningIntelligence(),
    val physical: PhysicalDevelopmentIntelligence = PhysicalDevelopmentIntelligence(),
    val adaptiveDungeons: List<AdaptiveDungeonRecommendation> = emptyList(),
    val weeklyComparison: WeeklyComparisonDelta = WeeklyComparisonDelta(),
    val goalAnalysis: List<GoalAnalysisInsight> = emptyList()
)

/**
 * Milestone 8/14 Repository: Intelligence, Personalization & Adaptive System.
 *
 * INVARIANTS:
 * 1. Read-only consumer of Milestones 0-7 & 14 Mission data; NEVER mutates player XP, level, rank, or attributes.
 * 2. 100% deterministic offline operation; cloud AI is strictly optional, consent-gated, cached, and backend-proxied.
 * 3. Incremental fingerprint check prevents unnecessary recalculations when telemetry is unchanged.
 * 4. Adaptive plans and roadmaps require explicit user approval before entering the unified schedule.
 */
class IntelligenceRepository(
    private val database: SoloLevelingDatabase,
    private val preferencesRepository: UserPreferencesRepository,
    private val backendAiGateway: BackendAiGateway,
    private val systemEventRepository: SystemEventRepository? = null
) {
    private val profileDao = database.intelligenceProfileDao()
    private val planDao = database.adaptivePlanDao()
    private val recDao = database.recommendationDao()
    private val roadmapDao = database.roadmapDao()
    private val reportDao = database.intelligenceReportDao()
    private val aiConfigDao = database.aiConfigDao()
    private val aiAuditDao = database.aiRequestAuditDao()

    private val aiReportNarrativeCache = ConcurrentHashMap<String, String>()

    private val _domainInsights = MutableStateFlow(IntelligenceDomainInsights())
    val domainInsights: StateFlow<IntelligenceDomainInsights> = _domainInsights.asStateFlow()

    val profile: Flow<PlayerIntelligenceProfile?> =
        profileDao.observeProfile().map { it?.toDomain() }

    val findings: Flow<List<StrengthWeaknessFinding>> =
        profileDao.observeFindings().map { list -> list.map { it.toDomain() } }

    val latestWorkload: Flow<WorkloadAssessment?> =
        profileDao.observeLatestWorkload().map { it?.toDomain() }

    val predictions: Flow<List<ConsistencyPrediction>> =
        profileDao.observePredictions().map { list -> list.map { it.toDomain() } }

    val latestPlan: Flow<AdaptiveDailyPlan?> =
        combine(
            planDao.observeLatestPlan(),
            planDao.observeAllBlocks()
        ) { plan, blocks ->
            plan?.let { p ->
                p.toDomain(blocks.filter { it.planUuid == p.planUuid }.map { it.toDomain() })
            }
        }

    val recommendations: Flow<List<PersonalizedRecommendation>> =
        recDao.observeAllRecommendations().map { list -> list.map { it.toDomain() } }

    val feedbackHistory: Flow<List<RecommendationFeedbackRecord>> =
        recDao.observeFeedbackHistory().map { list -> list.map { it.toDomain() } }

    val roadmaps: Flow<List<DevelopmentRoadmap>> =
        combine(
            roadmapDao.observeAllRoadmaps(),
            roadmapDao.observeAllMilestones()
        ) { roadmaps, milestones ->
            roadmaps.map { r ->
                r.toDomain(
                    milestones.filter { it.roadmapUuid == r.roadmapUuid }.map { it.toDomain() }
                )
            }
        }

    val reports: Flow<List<IntelligenceReport>> =
        reportDao.observeAllReports().map { list -> list.map { it.toDomain() } }

    val preferences: Flow<PlayerIntelligencePreferences> =
        reportDao.observePreferences().map { (it ?: IntelligencePreferencesEntity()).toDomain() }

    val consultations: Flow<List<AdvisorConsultationResult>> =
        reportDao.observeConsultations().map { list -> list.map { it.toDomain() } }

    suspend fun getPreferencesOnce(): PlayerIntelligencePreferences {
        val existing = reportDao.getPreferencesOnce()
        if (existing == null) {
            val defaultEntity = IntelligencePreferencesEntity()
            reportDao.upsertPreferences(defaultEntity)
            return defaultEntity.toDomain()
        }
        return existing.toDomain()
    }

    suspend fun collectTelemetryInput(
        nowEpochMs: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): IntelligenceTelemetryInput {
        val todayEpochDay = Instant.ofEpochMilli(nowEpochMs).atZone(zoneId).toLocalDate().toEpochDay()
        val userPrefs = preferencesRepository.preferencesFlow.first()
        val player = database.playerDao().getPlayerOnce()?.toDomain() ?: PlayerProfile()
        val physical = database.physicalStateDao().getPhysicalStateOnce()?.toDomain() ?: PhysicalDevelopmentState()
        val recovery = database.recoveryAssessmentDao().getLatestOnce()?.toDomain()
            ?: RecoveryEngine.assess(0, 0, 0, 0)

        val jogs = database.joggingSessionDao().getSessionsSince(0L).map { it.toDomain() }
        val workouts = database.workoutSessionDao().getWorkoutsSince(0L).map { it.toDomain() }

        val subjects = database.academicSubjectDao().observeAllSubjects().first().map { it.toDomain() }
        val chapters = database.academicSubjectDao().observeAllChapters().first().map { it.toDomain() }
        val topics = database.academicSubjectDao().getAllTopicsOnce().map { it.toDomain() }
        val studySessions = database.studySessionDao().observeAllSessions().first().map { it.toDomain() }
        val revisions = database.revisionRecordDao().observeAllRevisions().first().map { it.toDomain() }
        val assessments = database.academicAssessmentDao().observeAllAssessments().first().map { it.toDomain() }
        val exams = database.academicAssessmentDao().getAllExamsOnce().map { it.toDomain() }
        val generalQuests = database.questDao().getAllQuestsOnce().map { it.toDomain() }
        val academicQuests = database.academicQuestDao().getAllAcademicQuestsOnce().map { it.toDomain() }

        val routines = database.routineDao().getAllRoutinesOnce().map { it.toDomain() }
        val routineActs = database.routineDao().observeAllActivities().first().map { it.toDomain() }
        val routineComp = database.routineDao().getCompletionsSinceOnce(todayEpochDay - 90L).map { it.toDomain() }
        val habits = database.habitDao().getAllHabitsOnce().map { it.toDomain() }.filter { !it.isArchived }
        val habitComp = database.habitDao().getCompletionsSinceOnce(todayEpochDay - 180L).map { it.toDomain() }
        val recurrences = database.personalTaskDao().observeAllRecurrences().first().associate { it.taskId to it.toDomain() }
        val tasks = database.personalTaskDao().getAllTasksOnce().map { it.toDomain(recurrences[it.id]) }
        val goals = database.goalDao().getAllGoalsOnce().map { it.toDomain() }
        val goalMilestones = database.goalDao().observeAllMilestones().first().map { it.toDomain() }
        val goalRecords = database.goalDao().observeAllProgressRecords().first().map { it.toDomain() }
        val goalsWithDetails = goals.map { GoalEngine.buildGoalWithDetails(it, goalMilestones, goalRecords, tasks, todayEpochDay) }
        val disciplineAssessments = database.disciplineDao().getRecentAssessmentsOnce(30).map { it.toDomain() }

        val storedRefsToday = database.scheduleDao().getReferencesInRangeOnce(todayEpochDay, todayEpochDay).map { it.toDomain() }
        val scheduledToday = ScheduleCalendarEngine.buildUnifiedItemsForDay(
            targetEpochDay = todayEpochDay,
            routines = routines,
            routineActivities = routineActs,
            routineCompletions = routineComp,
            habits = habits,
            habitCompletions = habitComp,
            tasks = tasks,
            goals = goals,
            goalMilestones = goalMilestones,
            storedReferences = storedRefsToday,
            studySessions = studySessions,
            exams = exams,
            revisionQueue = emptyList(),
            jogSessions = jogs,
            workoutSessions = workouts,
            todayEpochDay = todayEpochDay
        )

        val dungeonDefs = database.dungeonDao().getAllDefinitionsOnce().map { it.toDomain() }
        val dungeonInstances = database.dungeonDao().getAllInstancesOnce().map { it.toDomain() }
        val dungeonCompletions = database.dungeonDao().getAllCompletionsOnce().map { it.toDomain() }
        val unlockedSkills = database.skillDao().getAllUnlocksOnce().map { it.toDomain() }
        val unlockedAchievementsCount = database.achievementTitleDao().getAllAchievementUnlocksOnce().size

        return IntelligenceTelemetryInput(
            profile = player,
            recovery = recovery,
            joggingSessions = jogs,
            workoutSessions = workouts,
            subjects = subjects,
            chapters = chapters,
            topics = topics,
            studySessions = studySessions,
            revisionRecords = revisions,
            assessments = assessments,
            exams = exams,
            generalQuests = generalQuests,
            academicQuests = academicQuests,
            habits = habits,
            habitCompletions = habitComp,
            routines = routines,
            routineCompletions = routineComp,
            tasks = tasks,
            goalsWithDetails = goalsWithDetails,
            disciplineAssessments = disciplineAssessments,
            scheduledItemsToday = scheduledToday,
            dungeonDefinitions = dungeonDefs,
            dungeonInstances = dungeonInstances,
            dungeonCompletions = dungeonCompletions,
            unlockedSkills = unlockedSkills,
            unlockedAchievementsCount = unlockedAchievementsCount,
            dailyAcademicGoalMinutes = userPrefs.academicDailyGoalMinutes,
            dailyJoggingTargetMinutes = physical.effectiveJoggingTarget,
            nowEpochMs = nowEpochMs,
            todayEpochDay = todayEpochDay,
            zoneId = zoneId
        )
    }

    /**
     * Evaluates source fingerprint; returns false without heavy DB writes when underlying telemetry is unchanged.
     */
    suspend fun refreshIntelligenceIfNeeded(
        force: Boolean = false,
        nowEpochMs: Long = System.currentTimeMillis()
    ): Boolean {
        val prefs = getPreferencesOnce()
        val input = collectTelemetryInput(nowEpochMs = nowEpochMs)
        val fingerprint = PlayerIntelligenceEngine.computeSourceFingerprint(input)
        val cachedProfile = profileDao.getProfileOnce()

        val workloadNow = WorkloadBalanceEngine.assess(input, prefs)
        _domainInsights.value = IntelligenceDomainInsights(
            academic = AcademicLearningIntelligenceEngine.analyze(input),
            physical = PhysicalDevelopmentIntelligenceEngine.analyze(input),
            adaptiveDungeons = if (prefs.enableAdaptiveDungeons) {
                AdaptiveDungeonDifficultyEngine.recommend(input, workloadNow)
            } else {
                emptyList()
            },
            weeklyComparison = IntelligenceReportEngine.computeWeeklyComparison(input),
            goalAnalysis = IntelligenceReportEngine.computeGoalAnalysis(input)
        )

        if (!force && cachedProfile != null && cachedProfile.sourceFingerprint == fingerprint) {
            return false
        }

        val newProfile = PlayerIntelligenceEngine.buildProfile(
            input = input,
            sharePhysical = prefs.sharePhysicalWithIntelligence,
            shareAcademic = prefs.shareAcademicWithIntelligence,
            shareLife = prefs.shareLifeWithIntelligence,
            shareRpg = prefs.shareRpgWithIntelligence
        )
        profileDao.upsertProfile(newProfile.toEntity())

        val newFindings = StrengthsAndWeaknessesEngine.analyze(input, newProfile)
        profileDao.clearFindings()
        if (newFindings.isNotEmpty()) {
            profileDao.insertFindings(newFindings.map { it.toEntity() })
        }

        profileDao.insertWorkload(workloadNow.toEntity())

        val newPredictions = PredictiveConsistencyEngine.evaluate(input)
        profileDao.clearPredictions()
        if (newPredictions.isNotEmpty()) {
            profileDao.insertPredictions(newPredictions.map { it.toEntity() })
        }

        recDao.expireStaleRecommendations(nowEpochMs)
        val existingRecs = recDao.getAllRecommendationsOnce().map { it.toDomain() }
        val feedback = recDao.getFeedbackHistoryOnce().map { it.toDomain() }
        val freshRecs = RecommendationEngine.generate(
            input = input,
            profile = newProfile,
            workload = workloadNow,
            existingRecommendations = existingRecs,
            feedbackHistory = feedback,
            prefs = prefs
        )
        if (freshRecs.isNotEmpty()) {
            recDao.insertRecommendations(freshRecs.map { it.toEntity() })
        }

        if (prefs.enableProactivePlanning && planDao.getPlanForDayOnce(input.todayEpochDay) == null) {
            val draftPlan = AdaptivePlanningEngine.generateDailyPlan(
                input = input,
                workload = workloadNow,
                prefs = prefs,
                targetEpochDay = input.todayEpochDay,
                existingScheduledItems = input.scheduledItemsToday
            )
            planDao.insertPlan(draftPlan.toEntity())
            if (draftPlan.blocks.isNotEmpty()) {
                planDao.insertBlocks(draftPlan.blocks.map { it.toEntity() })
            }
        }

        return true
    }

    suspend fun generateDailyAdaptivePlan(
        targetEpochDay: Long = LocalDate.now().toEpochDay(),
        isPartialDay: Boolean = false,
        currentMinuteOfDay: Int = 8 * 60
    ): AdaptiveDailyPlan {
        val now = System.currentTimeMillis()
        val prefs = getPreferencesOnce()
        val input = collectTelemetryInput(nowEpochMs = now)
        val workload = WorkloadBalanceEngine.assess(input, prefs)
        val plan = AdaptivePlanningEngine.generateDailyPlan(
            input = input,
            workload = workload,
            prefs = prefs,
            targetEpochDay = targetEpochDay,
            existingScheduledItems = input.scheduledItemsToday,
            isPartialDay = isPartialDay,
            currentMinuteOfDay = currentMinuteOfDay
        )
        val rowId = planDao.insertPlan(plan.toEntity())
        if (plan.blocks.isNotEmpty()) {
            planDao.insertBlocks(plan.blocks.map { it.toEntity() })
        }
        val persistedBlocks = planDao.getBlocksForPlanOnce(plan.planUuid).map { it.toDomain() }
        return plan.copy(id = rowId, blocks = persistedBlocks)
    }

    suspend fun approveAdaptivePlan(planId: Long): Int {
        val planEntity = planDao.getPlanById(planId) ?: return 0
        val now = System.currentTimeMillis()
        val zoneIdStr = ZoneId.systemDefault().id
        val blocks = planDao.getBlocksForPlanOnce(planEntity.planUuid)
        var scheduledCount = 0

        blocks.forEach { blockEntity ->
            val updatedBlock = if (blockEntity.statusName == AdaptivePlanBlockStatus.PENDING.name) {
                blockEntity.copy(statusName = AdaptivePlanBlockStatus.APPROVED.name)
            } else {
                blockEntity
            }
            planDao.updateBlock(updatedBlock)

            val stableSourceId = if (blockEntity.id > 0L) blockEntity.id else abs(blockEntity.blockUuid.hashCode().toLong()) + 1L
            val existingRef = database.scheduleDao().getReferenceBySource(
                ScheduledSourceType.AI_PLAN_PROPOSAL.name,
                stableSourceId
            )
            database.scheduleDao().upsertReference(
                ScheduledActivityReferenceEntity(
                    id = existingRef?.id ?: 0L,
                    sourceTypeName = ScheduledSourceType.AI_PLAN_PROPOSAL.name,
                    sourceRecordId = stableSourceId,
                    title = blockEntity.title,
                    subtitle = "${blockEntity.module} • Adaptive Plan",
                    dateEpochDay = blockEntity.scheduledDateEpochDay,
                    startMinuteOfDay = blockEntity.startMinuteOfDay,
                    durationMinutes = blockEntity.durationMinutes,
                    isAllDay = false,
                    isRecurringInstance = false,
                    isCompletedSnapshot = blockEntity.statusName == AdaptivePlanBlockStatus.COMPLETED.name,
                    timezoneId = zoneIdStr,
                    notes = blockEntity.priorityReason,
                    createdAtEpochMs = existingRef?.createdAtEpochMs ?: now
                )
            )
            scheduledCount++
        }

        planDao.updatePlan(planEntity.copy(isApproved = true))
        return scheduledCount
    }

    suspend fun updatePlanBlockProgress(blockId: Long, completedMinutes: Int): AdaptivePlanBlock? {
        val existing = planDao.getBlockById(blockId) ?: return null
        val updated = AdaptivePlanningEngine.recordPartialCompletion(existing.toDomain(), completedMinutes)
        planDao.updateBlock(updated.toEntity())
        return updated
    }

    suspend fun reschedulePlanBlock(blockId: Long, newStartMinute: Int): AdaptivePlanBlock? {
        val target = planDao.getBlockById(blockId) ?: return null
        val siblings = planDao.getBlocksForPlanOnce(target.planUuid).map { it.toDomain() }
        val input = collectTelemetryInput()
        val rescheduled = AdaptivePlanningEngine.rescheduleBlock(
            block = target.toDomain(),
            newStartMinute = newStartMinute,
            otherBlocks = siblings,
            existingScheduledItems = input.scheduledItemsToday
        )
        planDao.updateBlock(rescheduled.toEntity())
        return rescheduled
    }

    suspend fun respondToRecommendation(
        recUuid: String,
        action: RecommendationStatus,
        feedbackNote: String = ""
    ): Boolean {
        val entity = recDao.getByUuid(recUuid) ?: return false
        val now = System.currentTimeMillis()
        val cooldown = if (action == RecommendationStatus.REJECTED) {
            now + 5 * 24 * 60 * 60_000L
        } else {
            entity.cooldownUntilEpochMs
        }
        recDao.updateRecommendation(
            entity.copy(
                statusName = action.name,
                respondedAtEpochMs = now,
                cooldownUntilEpochMs = cooldown
            )
        )
        val cat = runCatching {
            com.example.domain.model.RecommendationCategory.valueOf(entity.categoryName)
        }.getOrDefault(com.example.domain.model.RecommendationCategory.PERSONAL_DEVELOPMENT)
        recDao.insertFeedback(
            RecommendationFeedbackRecord(
                recUuid = recUuid,
                category = cat,
                action = action,
                feedbackNote = feedbackNote.trim(),
                timestampEpochMs = now
            ).toEntity()
        )
        return true
    }

    suspend fun generateRoadmap(
        horizon: RoadmapHorizon,
        focusDomains: List<String>
    ): DevelopmentRoadmap {
        val prefs = getPreferencesOnce()
        val input = collectTelemetryInput()
        val profile = PlayerIntelligenceEngine.buildProfile(
            input = input,
            sharePhysical = prefs.sharePhysicalWithIntelligence,
            shareAcademic = prefs.shareAcademicWithIntelligence,
            shareLife = prefs.shareLifeWithIntelligence,
            shareRpg = prefs.shareRpgWithIntelligence
        )
        val roadmap = LongTermDevelopmentPlanner.generateRoadmap(horizon, focusDomains, input, profile)
        val id = roadmapDao.insertRoadmap(roadmap.toEntity())
        roadmapDao.insertMilestones(roadmap.milestones.map { it.toEntity() })
        return roadmap.copy(id = id)
    }

    suspend fun approveRoadmap(roadmapUuid: String): Boolean {
        val roadmap = roadmapDao.getRoadmapByUuid(roadmapUuid) ?: return false
        roadmapDao.updateRoadmap(roadmap.copy(isApproved = true, updatedAtEpochMs = System.currentTimeMillis()))
        return true
    }

    suspend fun toggleRoadmapMilestone(milestoneUuid: String): Boolean {
        val ms = roadmapDao.getMilestoneByUuid(milestoneUuid) ?: return false
        roadmapDao.updateMilestone(ms.copy(isCompleted = !ms.isCompleted))
        return true
    }

    suspend fun editRoadmapMilestone(
        milestoneUuid: String,
        newTitle: String,
        newNotes: String
    ): Boolean {
        val ms = roadmapDao.getMilestoneByUuid(milestoneUuid) ?: return false
        val cleanTitle = newTitle.trim()
        if (cleanTitle.isBlank()) return false
        roadmapDao.updateMilestone(
            ms.copy(
                title = cleanTitle,
                notes = newNotes.trim(),
                isUserEdited = true
            )
        )
        return true
    }

    suspend fun generateReport(
        periodType: ReportPeriodType,
        includeAiNarrativeIfConsented: Boolean = true
    ): IntelligenceReport {
        val now = System.currentTimeMillis()
        val prefs = getPreferencesOnce()
        val input = collectTelemetryInput(nowEpochMs = now)
        val baseReport = IntelligenceReportEngine.generateReport(periodType, input)
        val fingerprint = PlayerIntelligenceEngine.computeSourceFingerprint(input)
        val cacheKey = "${periodType.name}:$fingerprint"
        _domainInsights.value = _domainInsights.value.copy(
            weeklyComparison = IntelligenceReportEngine.computeWeeklyComparison(input),
            goalAnalysis = IntelligenceReportEngine.computeGoalAnalysis(input)
        )

        val consent = aiConfigDao.getConsentRecordOnce()?.toDomain() ?: AiConsentState()
        val modelPrefs = aiConfigDao.getModelPreferencesOnce()?.toDomain() ?: AiModelPreferenceState()
        val endpointValid = backendAiGateway.supportsDirectCloud ||
            HttpBackendAiGateway.normalizeProxyEndpoint(modelPrefs.backendBaseUrl) != null
        val canUseCloud = includeAiNarrativeIfConsented &&
            prefs.shareIntelligenceWithCloudAi &&
            consent.cloudAiConsentGranted &&
            modelPrefs.aiEnabled &&
            endpointValid

        var aiNarrative: String? = aiReportNarrativeCache[cacheKey]
        var usedFallback = aiNarrative == null

        if (canUseCloud && aiNarrative == null) {
            val startMs = System.currentTimeMillis()
            val statsPrompt = buildString {
                appendLine("Period: ${baseReport.periodType.label} (${baseReport.periodLabel})")
                appendLine("Active days observed: ${baseReport.activeDaysObserved} (partial=${baseReport.isPartialDataWindow})")
                appendLine(baseReport.measuredStatsSummary)
                appendLine(baseReport.selfReportedStatsSummary)
                appendLine("Workouts=${baseReport.workoutsCount}, JogMin=${baseReport.joggingMinutes}, StudyMin=${baseReport.studyMinutes}, Revisions=${baseReport.revisionsCount}, Habits=${baseReport.habitsCompletedCount}, Tasks=${baseReport.tasksCompletedCount}, Dungeons=${baseReport.dungeonsClearedCount}")
                appendLine("Highlights: ${baseReport.deterministicHighlights.joinToString("; ")}")
                appendLine("Risks: ${baseReport.deterministicRisks.joinToString("; ")}")
            }
            val result = backendAiGateway.requestStructured(
                backendBaseUrl = modelPrefs.backendBaseUrl,
                request = BackendAiProxyRequest(
                    model = modelPrefs.selectedModelId,
                    requestType = "INTELLIGENCE_REPORT",
                    systemPrompt = ADVISORY_SYSTEM_PROMPT,
                    contextSummary = statsPrompt,
                    userPrompt = "Write a concise 3-sentence coaching reflection strictly grounded in the measured statistics above. Do not invent numbers or promise XP.",
                    stream = false
                )
            )
            val latency = System.currentTimeMillis() - startMs
            val safe = PersonalDevelopmentAdvisor.validateCloudAdvice(
                rawAiText = result.fullText,
                recoveryState = input.recovery.state,
                selfReportedDiscomfort = input.recovery.selfReportedDiscomfort
            )
            val success = result.status == AiConnectionStatus.CONNECTED && safe.isNotBlank()
            if (success) {
                aiNarrative = safe
                usedFallback = false
                aiReportNarrativeCache[cacheKey] = safe
            }
            aiAuditDao.insertAudit(
                AiRequestAuditEntity(
                    requestType = "INTELLIGENCE_REPORT",
                    modelId = modelPrefs.selectedModelId,
                    usedCloudBackend = true,
                    success = success,
                    promptTokens = result.promptTokens,
                    completionTokens = result.completionTokens,
                    latencyMs = latency,
                    errorCode = if (success) null else result.errorMessage ?: result.status.name,
                    timestampEpochMs = System.currentTimeMillis()
                )
            )
        }

        val finalReport = baseReport.copy(
            aiNarrativeSummary = aiNarrative,
            aiUsedFallback = usedFallback
        )
        val id = reportDao.insertReport(finalReport.toEntity())
        if (periodType == ReportPeriodType.WEEKLY) {
            systemEventRepository?.publishEvent(
                eventType = SystemEventType.WEEKLY_REPORT_GENERATED,
                relatedEntityId = finalReport.reportUuid,
                sourceModule = "INTELLIGENCE",
                metadata = mapOf("periodLabel" to finalReport.periodLabel),
                nowEpochMs = now
            )
        }
        return finalReport.copy(id = id)
    }

    suspend fun askAdvisor(rawQuestion: String): AdvisorConsultationResult {
        val now = System.currentTimeMillis()
        val prefs = getPreferencesOnce()
        val input = collectTelemetryInput(nowEpochMs = now)
        val profile = PlayerIntelligenceEngine.buildProfile(
            input = input,
            sharePhysical = prefs.sharePhysicalWithIntelligence,
            shareAcademic = prefs.shareAcademicWithIntelligence,
            shareLife = prefs.shareLifeWithIntelligence,
            shareRpg = prefs.shareRpgWithIntelligence
        )
        val workload = WorkloadBalanceEngine.assess(input, prefs)
        val cleanQuestion = PersonalDevelopmentAdvisor.sanitizeQuestion(rawQuestion)

        val consent = aiConfigDao.getConsentRecordOnce()?.toDomain() ?: AiConsentState()
        val modelPrefs = aiConfigDao.getModelPreferencesOnce()?.toDomain() ?: AiModelPreferenceState()
        val endpointValid = backendAiGateway.supportsDirectCloud ||
            HttpBackendAiGateway.normalizeProxyEndpoint(modelPrefs.backendBaseUrl) != null
        val canUseCloud = prefs.shareIntelligenceWithCloudAi &&
            consent.cloudAiConsentGranted &&
            modelPrefs.aiEnabled &&
            endpointValid &&
            cleanQuestion.isNotBlank()

        var cloudReply: String? = null
        var status = when {
            !prefs.shareIntelligenceWithCloudAi || !consent.cloudAiConsentGranted -> AiConnectionStatus.LOCAL_FALLBACK
            !endpointValid -> AiConnectionStatus.BACKEND_UNCONFIGURED
            else -> AiConnectionStatus.LOCAL_FALLBACK
        }

        if (canUseCloud) {
            val startMs = System.currentTimeMillis()
            val summaryContext = buildString {
                appendLine("Lv.${profile.playerLevel} ${profile.hunterRank.displayName}")
                appendLine("Scores: Phys=${profile.physicalActivityScore}, Acad=${profile.academicActivityScore}, Disc=${profile.disciplineConsistencyScore}, RPG=${profile.rpgEngagementScore}")
                appendLine("MeasuredRatio=${profile.measuredDataRatioPercent}%, ActiveDays30d=${profile.activeDaysLast30}")
                appendLine("Recovery=${input.recovery.state.code} (${input.recovery.readinessScore}/100), Discomfort=${input.recovery.selfReportedDiscomfort}")
                appendLine("Workload=${workload.workloadLevel.label}: ${workload.adjustmentDirective}")
            }
            val event = backendAiGateway.requestStructured(
                backendBaseUrl = modelPrefs.backendBaseUrl,
                request = BackendAiProxyRequest(
                    model = modelPrefs.selectedModelId,
                    requestType = "INTELLIGENCE_ADVISOR",
                    systemPrompt = ADVISORY_SYSTEM_PROMPT,
                    contextSummary = summaryContext,
                    userPrompt = cleanQuestion,
                    stream = false
                )
            )
            val latency = System.currentTimeMillis() - startMs
            status = event.status
            val success = event.status == AiConnectionStatus.CONNECTED && event.fullText.isNotBlank()
            if (success) {
                cloudReply = event.fullText
            }
            aiAuditDao.insertAudit(
                AiRequestAuditEntity(
                    requestType = "INTELLIGENCE_ADVISOR",
                    modelId = modelPrefs.selectedModelId,
                    usedCloudBackend = true,
                    success = success,
                    promptTokens = event.promptTokens,
                    completionTokens = event.completionTokens,
                    latencyMs = latency,
                    errorCode = if (success) null else event.errorMessage ?: event.status.name,
                    timestampEpochMs = System.currentTimeMillis()
                )
            )
        } else {
            aiAuditDao.insertAudit(
                AiRequestAuditEntity(
                    requestType = "INTELLIGENCE_ADVISOR",
                    modelId = "deterministic-local-engine",
                    usedCloudBackend = false,
                    success = true,
                    promptTokens = 0,
                    completionTokens = 0,
                    latencyMs = 1L,
                    errorCode = null,
                    timestampEpochMs = now
                )
            )
        }

        val result = PersonalDevelopmentAdvisor.buildConsultationResult(
            question = cleanQuestion.ifBlank { "Summarize my development readiness and priorities." },
            input = input,
            profile = profile,
            workload = workload,
            cloudReply = cloudReply,
            connectionStatus = status
        )
        val id = reportDao.insertConsultation(result.toEntity())
        return result.copy(id = id)
    }

    suspend fun updatePreferences(updated: PlayerIntelligencePreferences) {
        reportDao.upsertPreferences(updated.copy(updatedAtEpochMs = System.currentTimeMillis()).toEntity())
        refreshIntelligenceIfNeeded(force = true)
    }

    /**
     * Deletes all Milestone 8 intelligence artifacts and resets intelligence preferences to defaults,
     * preserving all Milestone 0-7 core player, quest, physical, academic, life, and RPG records.
     */
    suspend fun clearAllIntelligenceData() {
        profileDao.clearProfile()
        profileDao.clearFindings()
        profileDao.clearWorkloads()
        profileDao.clearPredictions()
        planDao.clearAllPlans()
        recDao.clearRecommendations()
        recDao.clearFeedback()
        roadmapDao.clearRoadmaps()
        reportDao.clearReports()
        reportDao.clearConsultations()
        reportDao.upsertPreferences(IntelligencePreferencesEntity())
        _domainInsights.value = IntelligenceDomainInsights()
    }

    companion object {
        private const val ADVISORY_SYSTEM_PROMPT =
            "You are the read-only System Intelligence Advisor. Provide practical, non-medical personal development coaching strictly from provided user statistics. Never claim to grant XP, alter Hunter Rank, unlock skills, or auto-complete quests, and never advise training through REST_RECOMMENDED or discomfort."
    }
}
