package com.example.data.repository

import android.content.Context
import com.example.data.integration.AiStreamEvent
import com.example.data.integration.BackendAiGateway
import com.example.data.integration.BackendAiProxyRequest
import com.example.data.integration.HttpBackendAiGateway
import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.entity.AiConsentRecordEntity
import com.example.data.local.entity.AiConversationEntity
import com.example.data.local.entity.AiMessageEntity
import com.example.data.local.entity.AiModelPreferenceEntity
import com.example.data.local.entity.AiRequestAuditEntity
import com.example.data.local.entity.NotificationPreferenceEntity
import com.example.data.local.entity.toDomain
import com.example.data.local.entity.toEntity
import com.example.data.preferences.UserPreferencesRepository
import com.example.domain.engine.AiProposalValidator
import com.example.domain.engine.BossBattleEngine
import com.example.domain.engine.DisciplineEngine
import com.example.domain.engine.GoalEngine
import com.example.domain.engine.HabitEngine
import com.example.domain.engine.PersonalDevelopmentAnalyst
import com.example.domain.engine.PlayerContextEngine
import com.example.domain.engine.PlayerTelemetrySnapshot
import com.example.domain.engine.PromptSanitizer
import com.example.domain.engine.RecoveryEngine
import com.example.domain.engine.RevisionEngine
import com.example.domain.engine.RoutineEngine
import com.example.domain.engine.ScheduleCalendarEngine
import com.example.domain.engine.SkillCatalog
import com.example.domain.model.AcademicQuestCategory
import com.example.domain.model.AcademicQuestDifficulty
import com.example.domain.model.AcademicQuestStatus
import com.example.domain.model.AiChatMessage
import com.example.domain.model.AiChatRole
import com.example.domain.model.AiConnectionStatus
import com.example.domain.model.AiConsentState
import com.example.domain.model.AiContextScope
import com.example.domain.model.AiConversation
import com.example.domain.model.AiDailyPlanItem
import com.example.domain.model.AiModelPreferenceState
import com.example.domain.model.AiQuestProposal
import com.example.domain.model.AiQuestProposalCategory
import com.example.domain.model.AiRecommendation
import com.example.domain.model.AiRequestAuditRecord
import com.example.domain.model.BossStatus
import com.example.domain.model.DevelopmentAnalysisReport
import com.example.domain.model.DungeonStatus
import com.example.domain.model.GoalStatus
import com.example.domain.model.HabitDayStatus
import com.example.domain.model.LifeContextSummary
import com.example.domain.model.PersonalTaskStatus
import com.example.domain.model.PhysicalDevelopmentState
import com.example.domain.model.PlanMode
import com.example.domain.model.PlayerContextSnapshot
import com.example.domain.model.PlayerProfile
import com.example.domain.model.ProposalStatus
import com.example.domain.model.QuestCategory
import com.example.domain.model.QuestType
import com.example.domain.model.RecoveryState
import com.example.domain.model.RpgContextSummary
import com.example.domain.model.SubjectProgressSummary
import com.example.domain.model.SupportedAiModels
import com.example.domain.model.SystemNotificationPreferences
import com.example.worker.SystemNotificationWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class ChatDispatchResult(
    val conversationId: Long,
    val connectionStatus: AiConnectionStatus,
    val assistantMessage: AiChatMessage,
    val errorDetail: String? = null
)

data class ProposalGenerationResult(
    val count: Int,
    val connectionStatus: AiConnectionStatus,
    val usedFallback: Boolean,
    val note: String
)

/**
 * Central AI System Administrator & Game Master Repository.
 *
 * SECURITY & ARCHITECTURE INVARIANTS:
 * 1. Zero client-side API keys — communicates solely via BackendAiGateway proxy.
 * 2. Progression authority belongs exclusively to ProgressionEngine / RealityOsRepository / AcademicRepository.
 *    This repository NEVER mutates player XP, level, rank, or attributes directly.
 * 3. Proposals require explicit player confirmation before entering Quest Board / Academic Quests.
 * 4. Honest deterministic fallback activates automatically whenever backend URL is missing, offline,
 *    consent-denied, auth-rejected, rate-limited, or malformed.
 */
class AiSystemRepository(
    private val database: SoloLevelingDatabase,
    private val preferencesRepository: UserPreferencesRepository,
    private val realityOsRepository: RealityOsRepository,
    private val academicRepository: AcademicRepository,
    private val backendAiGateway: BackendAiGateway
) {
    private val conversationDao = database.aiConversationDao()
    private val proposalDao = database.aiProposalDao()
    private val configDao = database.aiConfigDao()
    private val auditDao = database.aiRequestAuditDao()

    val conversations: Flow<List<AiConversation>> =
        conversationDao.observeConversations().map { list -> list.map { it.toDomain() } }

    fun observeMessages(conversationId: Long): Flow<List<AiChatMessage>> =
        conversationDao.observeMessagesForConversation(conversationId).map { list -> list.map { it.toDomain() } }

    val modelPreferences: Flow<AiModelPreferenceState> =
        configDao.observeModelPreferences().map { it?.toDomain() ?: AiModelPreferenceState() }

    val consentState: Flow<AiConsentState> =
        configDao.observeConsentRecord().map { it?.toDomain() ?: AiConsentState() }

    val notificationPreferences: Flow<SystemNotificationPreferences> =
        configDao.observeNotificationPreferences().map { it?.toDomain() ?: SystemNotificationPreferences() }

    val questProposals: Flow<List<AiQuestProposal>> =
        proposalDao.observeQuestProposals().map { list -> list.map { it.toDomain() } }

    val dailyPlanProposals: Flow<List<AiDailyPlanItem>> =
        proposalDao.observeDailyPlanProposals().map { list -> list.map { it.toDomain() } }

    val recommendations: Flow<List<AiRecommendation>> =
        proposalDao.observeActiveRecommendations().map { list -> list.map { it.toDomain() } }

    val recentAudits: Flow<List<AiRequestAuditRecord>> =
        auditDao.observeRecentAudits().map { list -> list.map { it.toDomain() } }

    suspend fun buildContextSnapshot(scope: AiContextScope = AiContextScope.FULL): PlayerContextSnapshot {
        val now = System.currentTimeMillis()
        val sevenDaysAgo = now - 7 * 24 * 60 * 60_000L

        val modelPrefs = configDao.getModelPreferencesOnce()?.toDomain() ?: AiModelPreferenceState()
        val consent = configDao.getConsentRecordOnce()?.toDomain() ?: AiConsentState()
        val userPrefs = preferencesRepository.preferencesFlow.first()

        val player = database.playerDao().getPlayerOnce()?.toDomain() ?: PlayerProfile()
        val physical = database.physicalStateDao().getPhysicalStateOnce()?.toDomain() ?: PhysicalDevelopmentState()
        val recovery = database.recoveryAssessmentDao().getLatestOnce()?.toDomain()
            ?: RecoveryEngine.assess(0, 0, 0, 0)
        val recentJogs = database.joggingSessionDao().getSessionsSince(sevenDaysAgo).map { it.toDomain() }
        val recentWorkouts = database.workoutSessionDao().getWorkoutsSince(sevenDaysAgo).map { it.toDomain() }

        val subjects = database.academicSubjectDao().observeAllSubjects().first().map { it.toDomain() }
        val modules = database.academicSubjectDao().observeAllModules().first().map { it.toDomain() }
        val chapters = database.academicSubjectDao().observeAllChapters().first().map { it.toDomain() }
        val topics = database.academicSubjectDao().getAllTopicsOnce().map { it.toDomain() }
        val studySessions = database.studySessionDao().observeAllSessions().first().map { it.toDomain() }
        val exams = database.academicAssessmentDao().getAllExamsOnce().map { it.toDomain() }
        val assessments = database.academicAssessmentDao().observeAllAssessments().first().map { it.toDomain() }
        val generalQuests = database.questDao().getAllQuestsOnce().map { it.toDomain() }
        val academicQuests = database.academicQuestDao().getAllAcademicQuestsOnce().map { it.toDomain() }

        val latestRatioBySubject = assessments.groupBy { it.subjectId }.mapValues { it.value.first().normalizedRatio }
        val subjectSummaries = subjects.map { subject ->
            val mods = modules.filter { it.subjectId == subject.id }
            val chaps = chapters.filter { it.subjectId == subject.id }
            val tops = topics.filter { it.subjectId == subject.id }
            val pct = when {
                tops.isNotEmpty() -> (tops.count { it.isCompleted } * 100) / tops.size
                chaps.isNotEmpty() -> (chaps.count { it.isCompleted } * 100) / chaps.size
                else -> 0
            }
            SubjectProgressSummary(
                subject = subject,
                moduleCount = mods.size,
                chapterCount = chaps.size,
                completedChapters = chaps.count { it.isCompleted },
                topicCount = tops.size,
                completedTopics = tops.count { it.isCompleted },
                syllabusCompletionPercent = pct,
                totalStudyMinutes = studySessions.filter { it.subjectId == subject.id }.sumOf { it.durationMinutes },
                estimatedRemainingMinutes = chaps.filter { !it.isCompleted }.sumOf { it.estimatedMinutes },
                latestAssessmentRatio = latestRatioBySubject[subject.id]
            )
        }

        val activeSubjectIds = subjects.filter { !it.isArchived }.map { it.id }.toSet()
        val revisionQueue = RevisionEngine.buildQueue(
            topics = topics.filter { it.subjectId in activeSubjectIds },
            subjectNamesById = subjects.associate { it.id to it.name },
            difficultyByChapterId = chapters.associate { it.id to it.difficulty },
            assessmentRatioBySubjectId = latestRatioBySubject,
            nowEpochMs = now
        )

        val todayEpochDay = java.time.LocalDate.now().toEpochDay()
        val routines = database.routineDao().getAllRoutinesOnce().map { it.toDomain() }
        val routineActs = database.routineDao().observeAllActivities().first().map { it.toDomain() }
        val routineComp = database.routineDao().getCompletionsSinceOnce(todayEpochDay - 35L).map { it.toDomain() }
        val routineSummaries = routines.map { RoutineEngine.summarizeRoutineForDay(it, routineActs, routineComp, todayEpochDay) }
        val habits = database.habitDao().getAllHabitsOnce().map { it.toDomain() }.filter { !it.isArchived }
        val habitComp = database.habitDao().getCompletionsSinceOnce(todayEpochDay - 180L).map { it.toDomain() }
        val habitSummaries = habits.map { HabitEngine.buildAnalyticsSummary(it, habitComp, todayEpochDay) }
        val recurrences = database.personalTaskDao().observeAllRecurrences().first().associate { it.taskId to it.toDomain() }
        val personalTasks = database.personalTaskDao().getAllTasksOnce().map { it.toDomain(recurrences[it.id]) }
        val goals = database.goalDao().getAllGoalsOnce().map { it.toDomain() }
        val milestones = database.goalDao().observeAllMilestones().first().map { it.toDomain() }
        val goalRecords = database.goalDao().observeAllProgressRecords().first().map { it.toDomain() }
        val goalsWithDetails = goals.map { GoalEngine.buildGoalWithDetails(it, milestones, goalRecords, personalTasks, todayEpochDay) }
        val storedRefs = database.scheduleDao().getReferencesInRangeOnce(todayEpochDay, todayEpochDay).map { it.toDomain() }
        val dayItems = ScheduleCalendarEngine.buildUnifiedItemsForDay(
            targetEpochDay = todayEpochDay,
            routines = routines,
            routineActivities = routineActs,
            routineCompletions = routineComp,
            habits = habits,
            habitCompletions = habitComp,
            tasks = personalTasks,
            goals = goals,
            goalMilestones = milestones,
            storedReferences = storedRefs,
            studySessions = studySessions,
            exams = exams,
            revisionQueue = revisionQueue,
            jogSessions = recentJogs,
            workoutSessions = recentWorkouts,
            todayEpochDay = todayEpochDay
        )
        val conflicts = ScheduleCalendarEngine.detectConflicts(dayItems)
        val discipline = DisciplineEngine.assessDay(
            targetEpochDay = todayEpochDay,
            routineSummaries = routineSummaries,
            habitSummaries = habitSummaries,
            tasks = personalTasks,
            goalsWithDetails = goalsWithDetails,
            scheduledItemsToday = dayItems,
            conflictsToday = conflicts
        )
        val lifeSummary = LifeContextSummary(
            disciplineScore = discipline.disciplineScore,
            activeRoutinesCount = routines.count { it.isEnabled },
            completedRoutinesToday = routineSummaries.count { it.isScheduledToday && it.completionFraction >= 1f },
            activeHabitsCount = habits.size,
            completedHabitsToday = habitSummaries.count { it.todayStatus == HabitDayStatus.COMPLETED },
            bestHabitStreakDays = habitSummaries.maxOfOrNull { it.currentStreakDays } ?: 0,
            pendingTasksCount = personalTasks.count { it.status == PersonalTaskStatus.PENDING || it.status == PersonalTaskStatus.IN_PROGRESS || it.status == PersonalTaskStatus.POSTPONED },
            overdueTasksCount = personalTasks.count { it.isOverdue(todayEpochDay) },
            activeGoalsCount = goals.count { it.status == GoalStatus.ACTIVE },
            behindScheduleGoalsCount = goalsWithDetails.count { it.isBehindSchedule },
            scheduledBlocksToday = dayItems.size,
            scheduleConflictsToday = conflicts.size,
            strongestArea = discipline.strongestArea,
            weakestArea = discipline.weakestArea
        )

        val dungeonDefs = database.dungeonDao().getAllDefinitionsOnce().associateBy { it.id }
        val activeDungeonInsts = database.dungeonDao().getAllInstancesOnce()
            .filter { it.statusName == DungeonStatus.ACTIVE.name || it.statusName == DungeonStatus.PAUSED.name }
        val clearedDungeonsCount = database.dungeonDao().getTotalClearedCountOnce()
        val bossDefs = database.bossBattleDao().getAllBossDefinitionsOnce().associateBy { it.id }
        val activeBossInst = database.bossBattleDao().getAllBossInstancesOnce()
            .firstOrNull { it.statusName == BossStatus.ACTIVE.name }
        val activeBossHpPercent = if (activeBossInst != null) {
            val bDef = bossDefs[activeBossInst.bossId]?.toDomain()
            val bPhases = database.bossBattleDao().getPhasesForBossOnce(activeBossInst.bossId).map { it.toDomain() }
            if (bDef != null) {
                val quickTelemetry = PlayerTelemetrySnapshot(
                    profile = player,
                    recoveryState = recovery.state,
                    nowEpochMs = now,
                    jogs = recentJogs,
                    workouts = recentWorkouts,
                    studySessions = studySessions,
                    chapters = chapters,
                    revisions = database.revisionRecordDao().observeAllRevisions().first().map { it.toDomain() },
                    assessments = assessments,
                    habitCompletions = habitComp,
                    routineCompletions = routineComp,
                    completedTasks = personalTasks,
                    completedMilestones = milestones
                )
                BossBattleEngine.evaluateBossWithPhases(bDef, bPhases, activeBossInst.toDomain(), quickTelemetry).healthRemainingPercent
            } else null
        } else null
        val rpgSummary = RpgContextSummary(
            activeDungeonsCount = activeDungeonInsts.size,
            activeDungeonTitles = activeDungeonInsts.mapNotNull { dungeonDefs[it.dungeonId]?.title },
            clearedDungeonsCount = clearedDungeonsCount,
            activeBossTitle = activeBossInst?.let { bossDefs[it.bossId]?.title },
            activeBossHpPercent = activeBossHpPercent,
            unlockedSkillsCount = database.skillDao().getAllUnlocksOnce().size,
            totalSkillsCount = SkillCatalog.skills.size,
            unlockedAchievementsCount = database.achievementTitleDao().getAllAchievementUnlocksOnce().size,
            equippedTitle = player.title,
            shadowCollectionCount = database.inventoryShadowDao().getAllShadowCollectionOnce().size
        )

        return PlayerContextEngine.buildContextSnapshot(
            scope = scope,
            consent = consent,
            profile = player,
            physicalState = physical,
            recoveryAssessment = recovery,
            recentJogs = recentJogs,
            recentWorkouts = recentWorkouts,
            subjectSummaries = subjectSummaries,
            revisionQueue = revisionQueue,
            studySessions = studySessions,
            exams = exams,
            assessments = assessments,
            generalQuests = generalQuests,
            academicQuests = academicQuests,
            dailyResetHour = userPrefs.dailyResetHour,
            dailyAcademicGoalMin = userPrefs.academicDailyGoalMinutes,
            weeklyAcademicGoalMin = userPrefs.academicWeeklyGoalMinutes,
            selectedAiModelId = modelPrefs.selectedModelId,
            nowEpochMs = now,
            lifeSummaryInput = lifeSummary,
            rpgSummaryInput = rpgSummary
        )
    }

    suspend fun analyzeDevelopment(scope: AiContextScope = AiContextScope.FULL): Pair<PlayerContextSnapshot, DevelopmentAnalysisReport> {
        val snapshot = buildContextSnapshot(scope)
        val report = PersonalDevelopmentAnalyst.analyze(snapshot)
        return snapshot to report
    }

    suspend fun createConversation(
        title: String = "System Briefing",
        module: String = "GENERAL"
    ): Long {
        val now = System.currentTimeMillis()
        val modelPrefs = configDao.getModelPreferencesOnce()?.toDomain() ?: AiModelPreferenceState()
        return conversationDao.insertConversation(
            AiConversationEntity(
                title = title.trim().ifBlank { "System Briefing" },
                associatedModule = module,
                modelUsed = modelPrefs.selectedModelId,
                createdAtEpochMs = now,
                updatedAtEpochMs = now
            )
        )
    }

    suspend fun deleteConversation(conversationId: Long) {
        conversationDao.deleteConversation(conversationId)
    }

    suspend fun clearAllConversations() {
        conversationDao.clearAllConversations()
    }

    suspend fun sendChatMessage(
        conversationId: Long?,
        userPrompt: String,
        onStreamToken: (String) -> Unit = {}
    ): ChatDispatchResult {
        val cleanPrompt = userPrompt.trim()
        val now = System.currentTimeMillis()
        val modelPrefs = configDao.getModelPreferencesOnce()?.toDomain() ?: AiModelPreferenceState()
        val consent = configDao.getConsentRecordOnce()?.toDomain() ?: AiConsentState()
        val modelOption = SupportedAiModels.findById(modelPrefs.selectedModelId)

        val scope = PlayerContextEngine.inferScope(cleanPrompt)
        val snapshot = buildContextSnapshot(scope)
        val report = PersonalDevelopmentAnalyst.analyze(snapshot)

        val activeConvId = conversationId ?: createConversation(
            title = cleanPrompt.take(40).ifBlank { "Game Master Session" },
            module = scope.name
        )

        val historyBefore = conversationDao.getMessagesForConversationOnce(activeConvId).map { it.toDomain() }

        conversationDao.insertMessage(
            AiMessageEntity(
                conversationId = activeConvId,
                roleName = AiChatRole.USER.name,
                content = cleanPrompt,
                associatedModule = scope.name,
                modelUsed = modelPrefs.selectedModelId,
                isFallback = false,
                timestampEpochMs = now
            )
        )

        if (historyBefore.isEmpty()) {
            conversationDao.updateConversationMeta(
                id = activeConvId,
                title = cleanPrompt.take(42).ifBlank { "Game Master Session" },
                updatedAtEpochMs = now
            )
        }

        val startMs = System.currentTimeMillis()
        var finalText = ""
        var finalStatus = AiConnectionStatus.LOCAL_FALLBACK
        var usedCloud = false
        var promptTokens = 0
        var completionTokens = 0
        var errorMessage: String? = null

        val endpointValid = HttpBackendAiGateway.normalizeProxyEndpoint(modelPrefs.backendBaseUrl) != null
        val canUseCloud = modelPrefs.aiEnabled && consent.cloudAiConsentGranted && endpointValid

        if (canUseCloud) {
            usedCloud = true
            val proxyRequest = BackendAiProxyRequest(
                model = modelOption.id,
                requestType = "CHAT",
                systemPrompt = GAME_MASTER_SYSTEM_PROMPT,
                contextSummary = PlayerContextEngine.formatCompactPromptContext(snapshot),
                recentTurns = HttpBackendAiGateway.buildBoundedTurns(historyBefore, maxTurns = 6),
                userPrompt = PromptSanitizer.sanitize(cleanPrompt, maxChars = 600),
                stream = modelPrefs.streamingEnabled && modelOption.supportsStreaming
            )

            if (proxyRequest.stream) {
                backendAiGateway.streamChat(modelPrefs.backendBaseUrl, proxyRequest).collect { event ->
                    when (event) {
                        is AiStreamEvent.TokenDelta -> onStreamToken(event.delta)
                        is AiStreamEvent.Completed -> {
                            finalText = event.fullText.trim()
                            finalStatus = event.status
                            promptTokens = event.promptTokens
                            completionTokens = event.completionTokens
                            errorMessage = event.errorMessage
                        }
                    }
                }
            } else {
                val completed = backendAiGateway.requestStructured(modelPrefs.backendBaseUrl, proxyRequest)
                finalText = completed.fullText.trim()
                finalStatus = completed.status
                promptTokens = completed.promptTokens
                completionTokens = completed.completionTokens
                errorMessage = completed.errorMessage
            }
        } else {
            finalStatus = when {
                !modelPrefs.aiEnabled -> AiConnectionStatus.LOCAL_FALLBACK
                !consent.cloudAiConsentGranted -> AiConnectionStatus.LOCAL_FALLBACK
                else -> AiConnectionStatus.BACKEND_UNCONFIGURED
            }
        }

        val isFallback = finalText.isBlank() || finalStatus != AiConnectionStatus.CONNECTED
        if (isFallback) {
            finalText = PersonalDevelopmentAnalyst.answerNaturalPrompt(
                userMessage = cleanPrompt,
                snapshot = snapshot,
                report = report
            )
        }

        val replyTime = System.currentTimeMillis()
        val assistantMsg = AiChatMessage(
            conversationId = activeConvId,
            role = AiChatRole.ASSISTANT,
            content = finalText,
            associatedModule = scope.name,
            modelUsed = if (isFallback) "deterministic-local" else modelOption.id,
            isFallback = isFallback,
            timestampEpochMs = replyTime
        )
        val savedMsgId = conversationDao.insertMessage(assistantMsg.toEntity())
        val existingConv = conversationDao.getConversationById(activeConvId)
        conversationDao.updateConversationMeta(
            id = activeConvId,
            title = existingConv?.title ?: cleanPrompt.take(40),
            updatedAtEpochMs = replyTime
        )

        auditDao.insertAudit(
            AiRequestAuditEntity(
                requestType = "CHAT",
                modelId = if (isFallback) "deterministic-local" else modelOption.id,
                usedCloudBackend = usedCloud,
                success = !isFallback,
                promptTokens = promptTokens,
                completionTokens = completionTokens,
                latencyMs = (replyTime - startMs).coerceAtLeast(0),
                errorCode = if (isFallback) finalStatus.name else null,
                timestampEpochMs = replyTime
            )
        )

        return ChatDispatchResult(
            conversationId = activeConvId,
            connectionStatus = finalStatus,
            assistantMessage = assistantMsg.copy(id = savedMsgId),
            errorDetail = errorMessage
        )
    }

    suspend fun generateQuestProposals(): ProposalGenerationResult {
        val startMs = System.currentTimeMillis()
        val modelPrefs = configDao.getModelPreferencesOnce()?.toDomain() ?: AiModelPreferenceState()
        val consent = configDao.getConsentRecordOnce()?.toDomain() ?: AiConsentState()
        val modelOption = SupportedAiModels.findById(modelPrefs.selectedModelId)

        val snapshot = buildContextSnapshot(AiContextScope.FULL)
        val activeGeneral = database.questDao().getPendingQuestsOnce().map { it.title }
        val activeAcad = database.academicQuestDao().getAllAcademicQuestsOnce()
            .filter { it.statusName == AcademicQuestStatus.ACTIVE.name }
            .map { it.title }
        val pendingProposals = proposalDao.observeQuestProposals().first()
            .filter { it.statusName == ProposalStatus.PENDING.name }
            .map { it.title }
        val existingActiveTitles = (activeGeneral + activeAcad + pendingProposals).toSet()

        var status = AiConnectionStatus.LOCAL_FALLBACK
        var usedCloud = false
        var proposals: List<AiQuestProposal> = emptyList()
        var promptTokens = 0
        var completionTokens = 0

        val canUseCloud = modelPrefs.aiEnabled &&
            consent.cloudAiConsentGranted &&
            HttpBackendAiGateway.normalizeProxyEndpoint(modelPrefs.backendBaseUrl) != null

        if (canUseCloud) {
            usedCloud = true
            val response = backendAiGateway.requestStructured(
                backendBaseUrl = modelPrefs.backendBaseUrl,
                request = BackendAiProxyRequest(
                    model = modelOption.id,
                    requestType = "QUEST_PROPOSALS",
                    systemPrompt = QUEST_JSON_SYSTEM_PROMPT,
                    contextSummary = PlayerContextEngine.formatCompactPromptContext(snapshot),
                    userPrompt = "Propose up to 3 realistic personalized quests based strictly on gaps in the snapshot. Do not duplicate: ${existingActiveTitles.take(10).joinToString(", ")}"
                )
            )
            status = response.status
            promptTokens = response.promptTokens
            completionTokens = response.completionTokens
            if (response.status == AiConnectionStatus.CONNECTED && response.fullText.isNotBlank()) {
                proposals = AiProposalValidator.parseQuestProposalsJson(
                    rawJson = response.fullText,
                    snapshot = snapshot,
                    existingActiveTitles = existingActiveTitles
                )
            }
        }

        val usedFallback = proposals.isEmpty()
        if (usedFallback) {
            proposals = AiProposalValidator.generateDeterministicQuestProposals(snapshot, existingActiveTitles)
            if (status == AiConnectionStatus.CONNECTED) {
                status = AiConnectionStatus.PROVIDER_ERROR
            }
        }

        if (proposals.isNotEmpty()) {
            proposalDao.insertQuestProposals(proposals.map { it.toEntity() })
        }

        val endMs = System.currentTimeMillis()
        auditDao.insertAudit(
            AiRequestAuditEntity(
                requestType = "QUEST_PROPOSALS",
                modelId = if (usedFallback) "deterministic-local" else modelOption.id,
                usedCloudBackend = usedCloud,
                success = !usedFallback,
                promptTokens = promptTokens,
                completionTokens = completionTokens,
                latencyMs = (endMs - startMs).coerceAtLeast(0),
                errorCode = if (usedFallback) status.name else null,
                timestampEpochMs = endMs
            )
        )

        return ProposalGenerationResult(
            count = proposals.size,
            connectionStatus = status,
            usedFallback = usedFallback,
            note = if (proposals.isEmpty()) {
                "All suggested quests already match active items on your Quest Board."
            } else {
                "Generated ${proposals.size} quest proposal(s) for review."
            }
        )
    }

    suspend fun approveQuestProposal(
        proposalId: Long,
        editedTitle: String? = null,
        editedDescription: String? = null,
        editedDurationMinutes: Int? = null
    ): Result<String> {
        val entity = proposalDao.getQuestProposalById(proposalId)
            ?: return Result.failure(IllegalArgumentException("Proposal not found."))
        val proposal = entity.toDomain()
        if (proposal.status != ProposalStatus.PENDING) {
            return Result.failure(IllegalStateException("Proposal was already processed."))
        }

        val finalTitle = (editedTitle ?: proposal.title).trim().ifBlank { proposal.title }
        val finalDesc = (editedDescription ?: proposal.description).trim().ifBlank { proposal.description }
        val finalDuration = (editedDurationMinutes ?: proposal.suggestedDurationMinutes).coerceIn(10, 180)

        // Safety check against physical recovery state
        val latestRecovery = database.recoveryAssessmentDao().getLatestOnce()?.toDomain()
        val unsafePhysical = proposal.category == AiQuestProposalCategory.PHYSICAL &&
            (latestRecovery?.state == RecoveryState.REST_RECOMMENDED || latestRecovery?.selfReportedDiscomfort == true)

        if (unsafePhysical) {
            return Result.failure(IllegalStateException("Blocked by Recovery Safety Guard: current state requires rest instead of physical training."))
        }

        when (proposal.category) {
            AiQuestProposalCategory.ACADEMIC -> {
                val subjects = database.academicSubjectDao().getActiveSubjectsOnce()
                val subjectId = proposal.targetSubjectId ?: subjects.firstOrNull()?.id
                val cat = if ("revision" in finalTitle.lowercase() || "recall" in finalTitle.lowercase()) {
                    AcademicQuestCategory.REVISION
                } else {
                    AcademicQuestCategory.DAILY_STUDY
                }
                academicRepository.createAcademicQuest(
                    title = finalTitle,
                    description = finalDesc,
                    category = cat,
                    subjectId = subjectId,
                    topicId = null,
                    difficulty = if (proposal.difficultyLabel.contains("D")) AcademicQuestDifficulty.D_RANK else AcademicQuestDifficulty.E_RANK,
                    objective = proposal.objective,
                    targetValue = finalDuration,
                    unit = "min",
                    deadlineEpochMs = proposal.deadlineEpochMs
                )
            }
            else -> {
                val mappedCategory = when (proposal.category) {
                    AiQuestProposalCategory.PHYSICAL -> QuestCategory.PHYSICAL
                    AiQuestProposalCategory.RECOVERY -> QuestCategory.RECOVERY
                    AiQuestProposalCategory.PERSONAL -> QuestCategory.SOCIAL
                    AiQuestProposalCategory.DISCIPLINE,
                    AiQuestProposalCategory.LONG_TERM -> QuestCategory.DISCIPLINE
                    AiQuestProposalCategory.ACADEMIC -> QuestCategory.INTELLECT
                }
                val mappedType = if (proposal.category == AiQuestProposalCategory.LONG_TERM) {
                    QuestType.LONG_TERM
                } else {
                    QuestType.DAILY
                }
                realityOsRepository.createCustomQuest(
                    title = finalTitle,
                    description = finalDesc,
                    type = mappedType,
                    category = mappedCategory,
                    targetValue = finalDuration,
                    unit = "min"
                )
            }
        }

        proposalDao.updateQuestProposal(
            proposal.copy(
                title = finalTitle,
                description = finalDesc,
                suggestedDurationMinutes = finalDuration,
                status = ProposalStatus.APPROVED
            ).toEntity()
        )
        return Result.success("Approved '$finalTitle' and added to active quests (0 XP until completed).")
    }

    suspend fun rejectQuestProposal(proposalId: Long) {
        proposalDao.updateQuestProposalStatus(proposalId, ProposalStatus.REJECTED.name)
    }

    suspend fun generatePlanProposals(mode: PlanMode): ProposalGenerationResult {
        val startMs = System.currentTimeMillis()
        val modelPrefs = configDao.getModelPreferencesOnce()?.toDomain() ?: AiModelPreferenceState()
        val consent = configDao.getConsentRecordOnce()?.toDomain() ?: AiConsentState()
        val modelOption = SupportedAiModels.findById(modelPrefs.selectedModelId)

        val snapshot = buildContextSnapshot(AiContextScope.PLANNING)
        var status = AiConnectionStatus.LOCAL_FALLBACK
        var usedCloud = false
        var planItems: List<AiDailyPlanItem> = emptyList()
        var promptTokens = 0
        var completionTokens = 0

        val canUseCloud = modelPrefs.aiEnabled &&
            consent.cloudAiConsentGranted &&
            HttpBackendAiGateway.normalizeProxyEndpoint(modelPrefs.backendBaseUrl) != null

        if (canUseCloud) {
            usedCloud = true
            val response = backendAiGateway.requestStructured(
                backendBaseUrl = modelPrefs.backendBaseUrl,
                request = BackendAiProxyRequest(
                    model = modelOption.id,
                    requestType = "DAILY_PLAN",
                    systemPrompt = PLAN_JSON_SYSTEM_PROMPT,
                    contextSummary = PlayerContextEngine.formatCompactPromptContext(snapshot),
                    userPrompt = "Generate a realistic non-overlapping ${mode.label} respecting recovery state and overdue revisions."
                )
            )
            status = response.status
            promptTokens = response.promptTokens
            completionTokens = response.completionTokens
            if (response.status == AiConnectionStatus.CONNECTED && response.fullText.isNotBlank()) {
                planItems = AiProposalValidator.parsePlanJson(response.fullText, mode)
            }
        }

        val usedFallback = planItems.isEmpty()
        if (usedFallback) {
            planItems = AiProposalValidator.generateDeterministicPlan(mode, snapshot)
            if (status == AiConnectionStatus.CONNECTED) {
                status = AiConnectionStatus.PROVIDER_ERROR
            }
        }

        proposalDao.clearPendingPlanForMode(mode.name)
        if (planItems.isNotEmpty()) {
            proposalDao.insertDailyPlanItems(planItems.map { it.toEntity() })
        }

        val endMs = System.currentTimeMillis()
        auditDao.insertAudit(
            AiRequestAuditEntity(
                requestType = "DAILY_PLAN_${mode.name}",
                modelId = if (usedFallback) "deterministic-local" else modelOption.id,
                usedCloudBackend = usedCloud,
                success = !usedFallback,
                promptTokens = promptTokens,
                completionTokens = completionTokens,
                latencyMs = (endMs - startMs).coerceAtLeast(0),
                errorCode = if (usedFallback) status.name else null,
                timestampEpochMs = endMs
            )
        )

        return ProposalGenerationResult(
            count = planItems.size,
            connectionStatus = status,
            usedFallback = usedFallback,
            note = "Generated ${planItems.size} non-overlapping block(s) for ${mode.label}."
        )
    }

    suspend fun approvePlanItem(
        itemId: Long,
        editedTitle: String? = null,
        editedStartMinute: Int? = null,
        editedDurationMinutes: Int? = null
    ) {
        val existing = proposalDao.getDailyPlanById(itemId)?.toDomain() ?: return
        proposalDao.updateDailyPlanItem(
            existing.copy(
                taskTitle = (editedTitle ?: existing.taskTitle).trim().ifBlank { existing.taskTitle },
                startMinuteOfDay = (editedStartMinute ?: existing.startMinuteOfDay).coerceIn(5 * 60, 22 * 60),
                durationMinutes = (editedDurationMinutes ?: existing.durationMinutes).coerceIn(10, 180),
                status = ProposalStatus.APPROVED
            ).toEntity()
        )
    }

    suspend fun rejectPlanItem(itemId: Long) {
        val existing = proposalDao.getDailyPlanById(itemId)?.toDomain() ?: return
        proposalDao.updateDailyPlanItem(existing.copy(status = ProposalStatus.REJECTED).toEntity())
    }

    suspend fun updateModelPreference(transform: (AiModelPreferenceState) -> AiModelPreferenceState) {
        val current = configDao.getModelPreferencesOnce()?.toDomain() ?: AiModelPreferenceState()
        val updated = transform(current).copy(updatedAtEpochMs = System.currentTimeMillis())
        configDao.upsertModelPreferences(updated.toEntity())
    }

    suspend fun updateConsent(transform: (AiConsentState) -> AiConsentState) {
        val current = configDao.getConsentRecordOnce()?.toDomain() ?: AiConsentState()
        val updated = transform(current).copy(updatedAtEpochMs = System.currentTimeMillis())
        configDao.upsertConsentRecord(updated.toEntity())
    }

    suspend fun updateNotificationPreferences(transform: (SystemNotificationPreferences) -> SystemNotificationPreferences) {
        val current = configDao.getNotificationPreferencesOnce()?.toDomain() ?: SystemNotificationPreferences()
        val updated = transform(current).copy(updatedAtEpochMs = System.currentTimeMillis())
        configDao.upsertNotificationPreferences(updated.toEntity())
    }

    suspend fun generateDungeonProposals(): ProposalGenerationResult {
        val snapshot = buildContextSnapshot(AiContextScope.FULL)
        val existingTitles = (
            database.dungeonDao().getAllDefinitionsOnce().map { it.title } +
                database.dungeonDao().getAllDungeonProposalsOnce().filter { it.statusName == ProposalStatus.PENDING.name }.map { it.title }
            ).toSet()
        val generated = AiProposalValidator.generateDeterministicDungeonProposals(snapshot, existingTitles)
        if (generated.isNotEmpty()) {
            database.dungeonDao().insertDungeonProposals(generated.map { it.toEntity() })
        }
        return ProposalGenerationResult(
            count = generated.size,
            connectionStatus = AiConnectionStatus.LOCAL_FALLBACK,
            usedFallback = true,
            note = if (generated.isEmpty()) {
                "No new dungeon proposals needed — matching gates already exist."
            } else {
                "Generated ${generated.size} AI Dungeon proposal(s) awaiting player approval."
            }
        )
    }

    suspend fun evaluateAndTriggerNotifications(context: Context): Int =
        SystemNotificationWorker.evaluateAndDispatch(context, database)

    companion object {
        private const val GAME_MASTER_SYSTEM_PROMPT =
            "You are the SOLO LEVELING Reality OS AI System Administrator & Game Master. " +
                "Base every statement strictly on the provided player telemetry snapshot. " +
                "Clearly separate measured data, self-reported entries, and heuristic familiarity estimates. " +
                "Never fabricate unrecorded sessions, never claim to modify XP/attributes/ranks directly, " +
                "and never encourage training through REST_RECOMMENDED or self-reported discomfort."

        private const val QUEST_JSON_SYSTEM_PROMPT =
            "Return ONLY valid JSON: {\"proposals\":[{\"title\":\"\",\"description\":\"\",\"category\":\"PHYSICAL|ACADEMIC|DISCIPLINE|PERSONAL|RECOVERY|LONG_TERM\",\"difficulty\":\"E-Rank|D-Rank|C-Rank\",\"objective\":\"\",\"completionCriteria\":\"\",\"suggestedDurationMinutes\":30,\"priority\":\"LOW|MEDIUM|HIGH|CRITICAL\",\"reason\":\"\"}]}. " +
                "Do not suggest intense physical quests if recovery state is REST_RECOMMENDED or RECOVERY_RECOMMENDED."

        private const val PLAN_JSON_SYSTEM_PROMPT =
            "Return ONLY valid JSON: {\"items\":[{\"taskTitle\":\"\",\"module\":\"PHYSICAL|ACADEMIC|RECOVERY|DISCIPLINE\",\"startMinuteOfDay\":450,\"durationMinutes\":30,\"priority\":\"LOW|MEDIUM|HIGH|CRITICAL\",\"reason\":\"\",\"dependencies\":\"\"}]}."
    }
}
