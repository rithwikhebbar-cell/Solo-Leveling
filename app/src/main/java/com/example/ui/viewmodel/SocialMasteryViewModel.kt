package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.SocialRepository
import com.example.domain.engine.AdvancedScenarioLibrary
import com.example.domain.engine.CommunicationPracticeEngine
import com.example.domain.engine.SocialMasteryAnalyticsEngine
import com.example.domain.engine.SocialPracticeCatalog
import com.example.domain.engine.ActivityRewardCalculation
import com.example.domain.model.AdaptiveSocialRecommendation
import com.example.domain.model.AdvancedScenario
import com.example.domain.model.AiCoachingMessage
import com.example.domain.model.AiCoachingMode
import com.example.domain.model.ConversationDialogueTurn
import com.example.domain.model.ConversationScenario
import com.example.domain.model.InterviewQuestion
import com.example.domain.model.LeadershipDecisionScenario
import com.example.domain.model.ListeningExercise
import com.example.domain.model.PracticeCategory
import com.example.domain.model.PracticeEvaluationResult
import com.example.domain.model.PracticeExercise
import com.example.domain.model.SocialActivityKind
import com.example.domain.model.SocialAnalyticsSummary
import com.example.domain.model.SocialMasteryStage
import com.example.domain.model.SocialModuleIdentifier
import com.example.domain.model.SocialModuleProgress
import com.example.domain.model.SocialPracticeSession
import com.example.domain.model.SocialProfile
import com.example.domain.model.WeaknessPattern
import com.example.service.SocialMasteryAiCoach
import com.example.service.SocialVoiceCoachController
import com.example.domain.engine.SocialMissionCatalog
import com.example.domain.engine.SocialSkillAssessmentEngine
import com.example.domain.model.MissionDifficultyRank
import com.example.domain.model.RealWorldSocialMission
import com.example.domain.model.SkillAssessmentEvaluationResult
import com.example.domain.model.SkillAssessmentSubmission
import com.example.domain.model.SocialMissionEvidenceType
import com.example.domain.model.SocialMissionLifecycleStatus
import com.example.domain.model.SocialMissionRecord
import com.example.domain.model.AdaptiveDifficulty
import com.example.domain.model.PersonalizedSocialRecommendation
import com.example.domain.model.PersonalizedWeeklyTrainingPlan
import com.example.domain.model.SocialRecommendationStatus
import com.example.domain.model.ScheduledTrainingActivity
import com.example.domain.model.SocialCompetencyProgress
import com.example.domain.model.SocialMasteryPathway
import com.example.domain.model.SocialWeeklyDevelopmentReport
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class SocialDashboardUiState(
    val profile: SocialProfile? = null,
    val moduleProgressList: List<SocialModuleProgress> = emptyList(),
    val availableExercises: List<PracticeExercise> = emptyList(),
    val conversationScenarios: List<ConversationScenario> = emptyList(),
    val listeningExercises: List<ListeningExercise> = emptyList(),
    val advancedScenarios: List<AdvancedScenario> = emptyList(),
    val interviewQuestions: List<InterviewQuestion> = emptyList(),
    val leadershipScenarios: List<LeadershipDecisionScenario> = emptyList(),
    val availableMissions: List<RealWorldSocialMission> = emptyList(),
    val activeMissionRecords: List<SocialMissionRecord> = emptyList(),
    val dailySocialMission: RealWorldSocialMission? = null,
    val selectedMission: RealWorldSocialMission? = null,
    val selectedMissionRecord: SocialMissionRecord? = null,
    val latestAssessmentResult: SkillAssessmentEvaluationResult? = null,
    val recentSessions: List<SocialPracticeSession> = emptyList(),
    val analyticsSummary: SocialAnalyticsSummary? = null,
    val isLoading: Boolean = true,
    val isSubmittingMission: Boolean = false,
    val missionSubmissionMessage: String? = null,
    val errorMessage: String? = null,
    // Milestone 12 — Phase 5: Adaptive Mastery Pathways & Weekly Planner
    val activeRecommendations: List<PersonalizedSocialRecommendation> = emptyList(),
    val currentTrainingPlan: PersonalizedWeeklyTrainingPlan? = null,
    val masteryPathways: List<SocialMasteryPathway> = emptyList(),
    val weeklyDevelopmentReport: SocialWeeklyDevelopmentReport? = null,
    val selectedCompetency: SocialCompetencyProgress? = null,
    val isGeneratingPlan: Boolean = false
)

data class PracticeActiveSessionUiState(
    val currentExercise: PracticeExercise? = null,
    val currentScenario: ConversationScenario? = null,
    val currentAdvancedScenario: AdvancedScenario? = null,
    val currentListening: ListeningExercise? = null,
    val currentInterviewQuestion: InterviewQuestion? = null,
    val currentLeadershipScenario: LeadershipDecisionScenario? = null,
    val activeSession: SocialPracticeSession? = null,
    val userDraftText: String = "",
    val userListeningQuestionsDraft: String = "",
    val scenarioCurrentRound: Int = 1,
    val scenarioUserTurns: List<String> = emptyList(),
    val isRecordingAudio: Boolean = false,
    val recordedAudioDurationSecs: Int = 0,
    val audioFile: File? = null,
    val evaluationResult: PracticeEvaluationResult? = null,
    val rewardEarned: ActivityRewardCalculation? = null,
    val isEvaluating: Boolean = false,
    val sessionCompleteMessage: String? = null
)

data class AiCoachUiState(
    val selectedMode: AiCoachingMode = AiCoachingMode.PERSONAL_COACH,
    val messages: List<AiCoachingMessage> = emptyList(),
    val isGenerating: Boolean = false,
    val isVoiceListening: Boolean = false,
    val voiceTranscriptDraft: String = "",
    val statusText: String = "Ready to guide your communication progression."
)

class SocialMasteryViewModel(
    private val socialRepository: SocialRepository,
    private val practiceEngine: CommunicationPracticeEngine = CommunicationPracticeEngine(socialRepository),
    private val analyticsEngine: SocialMasteryAnalyticsEngine = SocialMasteryAnalyticsEngine(socialRepository),
    private val aiCoach: SocialMasteryAiCoach? = null,
    private val voiceCoachController: SocialVoiceCoachController? = null,
    private val accountOwnerId: String = "local_vault_player_1",
    private val accountOwnerIdFlow: Flow<String>? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(SocialDashboardUiState())
    val uiState: StateFlow<SocialDashboardUiState> = _uiState.asStateFlow()

    private val _sessionState = MutableStateFlow(PracticeActiveSessionUiState())
    val sessionState: StateFlow<PracticeActiveSessionUiState> = _sessionState.asStateFlow()

    private val _aiCoachState = MutableStateFlow(AiCoachUiState())
    val aiCoachState: StateFlow<AiCoachUiState> = _aiCoachState.asStateFlow()

    @Volatile
    private var activeAccountOwnerId: String = accountOwnerId
    private var ownerObserverJob: Job? = null

    init {
        observeAiCoachStreams()
        if (accountOwnerIdFlow != null) {
            accountOwnerIdFlow
                .distinctUntilChanged()
                .onEach { nextOwnerId ->
                    val resolvedOwner = nextOwnerId.ifBlank { accountOwnerId }
                    if (resolvedOwner != activeAccountOwnerId || ownerObserverJob == null) {
                        activeAccountOwnerId = resolvedOwner
                        _uiState.value = SocialDashboardUiState()
                        _sessionState.value = PracticeActiveSessionUiState()
                        aiCoach?.clearSession()
                        loadSocialFoundationFor(resolvedOwner)
                    }
                }
                .launchIn(viewModelScope)
        } else {
            loadSocialFoundation()
        }
    }

    private fun observeAiCoachStreams() {
        aiCoach?.messages?.onEach { msgs ->
            _aiCoachState.value = _aiCoachState.value.copy(messages = msgs)
        }?.launchIn(viewModelScope)

        aiCoach?.isGenerating?.onEach { gen ->
            _aiCoachState.value = _aiCoachState.value.copy(isGenerating = gen)
        }?.launchIn(viewModelScope)

        voiceCoachController?.isVoiceActive?.onEach { active ->
            _aiCoachState.value = _aiCoachState.value.copy(isVoiceListening = active)
        }?.launchIn(viewModelScope)

        voiceCoachController?.transcriptText?.onEach { txt ->
            _aiCoachState.value = _aiCoachState.value.copy(voiceTranscriptDraft = txt)
        }?.launchIn(viewModelScope)

        voiceCoachController?.voiceStatusMessage?.onEach { msg ->
            _aiCoachState.value = _aiCoachState.value.copy(statusText = msg)
        }?.launchIn(viewModelScope)
    }

    fun loadSocialFoundation() {
        loadSocialFoundationFor(activeAccountOwnerId)
    }

    private fun loadSocialFoundationFor(ownerId: String) {
        ownerObserverJob?.cancel()
        ownerObserverJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                socialRepository.getOrCreateProfile(ownerId)
                socialRepository.initializeMissingModuleProgress(ownerId)
                socialRepository.seedInitialScenariosIfEmpty()
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(errorMessage = err.localizedMessage)
            }

            // Load Phase 2 + Phase 3 + Phase 4 Catalogues
            val analytics = runCatching { analyticsEngine.computeAnalyticsSummary(ownerId) }.getOrNull()

            _uiState.value = _uiState.value.copy(
                availableExercises = SocialPracticeCatalog.exercises,
                conversationScenarios = SocialPracticeCatalog.conversationScenarios,
                listeningExercises = SocialPracticeCatalog.listeningExercises,
                advancedScenarios = AdvancedScenarioLibrary.scenarios,
                interviewQuestions = AdvancedScenarioLibrary.interviewQuestions,
                leadershipScenarios = AdvancedScenarioLibrary.leadershipScenarios,
                availableMissions = SocialMissionCatalog.missions,
                dailySocialMission = SocialMissionCatalog.missions.firstOrNull(),
                analyticsSummary = analytics,
                isLoading = false
            )

            socialRepository.observeProfile(ownerId)
                .onEach { prof ->
                    _uiState.value = _uiState.value.copy(profile = prof, isLoading = false)
                }
                .launchIn(this)

            socialRepository.observeMissionRecords(ownerId)
                .onEach { records ->
                    val active = records.filter {
                        it.lifecycleStatus in listOf(
                            SocialMissionLifecycleStatus.ACCEPTED,
                            SocialMissionLifecycleStatus.IN_PROGRESS,
                            SocialMissionLifecycleStatus.SUBMITTED,
                            SocialMissionLifecycleStatus.UNDER_REVIEW
                        )
                    }
                    _uiState.value = _uiState.value.copy(activeMissionRecords = active)
                }
                .launchIn(this)

            socialRepository.observeAllModuleProgress(ownerId)
                .onEach { progress ->
                    val mapped = SocialModuleIdentifier.entries.map { mod ->
                        progress.find { it.module == mod } ?: SocialModuleProgress(
                            accountOwnerId = ownerId,
                            module = mod,
                            masteryStage = SocialMasteryStage.NOVICE
                        )
                    }
                    _uiState.value = _uiState.value.copy(moduleProgressList = mapped, isLoading = false)
                }
                .launchIn(this)

            socialRepository.observeRecentSessions(ownerId, 20)
                .onEach { sessions ->
                    _uiState.value = _uiState.value.copy(recentSessions = sessions)
                }
                .launchIn(this)

            // Milestone 12 — Phase 5: Observers for Recommendations, Training Plan & Pathways
            socialRepository.observeAdaptiveRecommendations(ownerId)
                .onEach { recs ->
                    _uiState.value = _uiState.value.copy(activeRecommendations = recs)
                }
                .launchIn(this)

            socialRepository.observeWeeklyTrainingPlan(ownerId)
                .onEach { plan ->
                    _uiState.value = _uiState.value.copy(currentTrainingPlan = plan)
                }
                .launchIn(this)

            socialRepository.observeAllPathways(ownerId)
                .onEach { pathways ->
                    _uiState.value = _uiState.value.copy(masteryPathways = pathways)
                }
                .launchIn(this)
        }
    }

    // AI Coaching Actions
    fun setAiCoachMode(mode: AiCoachingMode) {
        _aiCoachState.value = _aiCoachState.value.copy(selectedMode = mode)
    }

    fun sendAiCoachMessage(prompt: String) {
        val coach = aiCoach ?: return
        if (prompt.isBlank()) return
        viewModelScope.launch {
            coach.sendMessage(prompt, _aiCoachState.value.selectedMode, _uiState.value.profile)
        }
    }

    fun startVoiceListening() {
        voiceCoachController?.startListening()
    }

    fun stopVoiceAndSubmit(speakAloud: Boolean = true) {
        voiceCoachController?.stopListeningAndSubmit(
            mode = _aiCoachState.value.selectedMode,
            profile = _uiState.value.profile,
            speakReplyAloud = speakAloud
        )
    }

    fun cancelVoiceSession() {
        voiceCoachController?.cancelSession()
    }

    // Practice Session Execution
    fun startExerciseSession(exerciseId: String) {
        val exercise = practiceEngine.getExerciseById(exerciseId) ?: return
        val session = practiceEngine.createPracticeSession(
            accountOwnerId = activeAccountOwnerId,
            module = exercise.module,
            activityKind = when (exercise.category) {
                PracticeCategory.IMPROMPTU_SPEAKING,
                PracticeCategory.ARTICULATION -> SocialActivityKind.SPEAKING_REHEARSAL
                PracticeCategory.ACTIVE_LISTENING -> SocialActivityKind.LISTENING_EXERCISE
                else -> SocialActivityKind.PRACTICE
            }
        )

        _sessionState.value = PracticeActiveSessionUiState(
            currentExercise = exercise,
            activeSession = session
        )
    }

    fun updateDraftText(text: String) {
        _sessionState.value = _sessionState.value.copy(userDraftText = text)
    }

    fun submitExerciseForEvaluation() {
        val ex = _sessionState.value.currentExercise ?: return
        val session = _sessionState.value.activeSession ?: return
        val response = _sessionState.value.userDraftText

        viewModelScope.launch {
            _sessionState.value = _sessionState.value.copy(isEvaluating = true)
            practiceEngine.completeAndEvaluateExercise(
                session = session.copy(durationSeconds = 120),
                exercise = ex,
                userResponseText = response
            ).onSuccess { (eval, reward) ->
                _sessionState.value = _sessionState.value.copy(
                    isEvaluating = false,
                    evaluationResult = eval,
                    rewardEarned = reward,
                    sessionCompleteMessage = if (eval.isPassed) "Exercise Complete! XP and Attribute rewards applied." else "Exercise logged. Practice again to increase rubric score."
                )
            }.onFailure { err ->
                _sessionState.value = _sessionState.value.copy(
                    isEvaluating = false,
                    sessionCompleteMessage = "Submission error: ${err.localizedMessage}"
                )
            }
        }
    }

    // Conversation Simulation Execution
    fun startConversationScenario(scenarioId: String) {
        val scenario = practiceEngine.getConversationScenarioById(scenarioId) ?: return
        _sessionState.value = PracticeActiveSessionUiState(
            currentScenario = scenario,
            scenarioCurrentRound = 1,
            scenarioUserTurns = emptyList()
        )
    }

    fun sendConversationResponse(responseText: String) {
        val scenario = _sessionState.value.currentScenario ?: return
        val updatedTurns = _sessionState.value.scenarioUserTurns + responseText
        val nextRound = _sessionState.value.scenarioCurrentRound + 1
        val ownerId = activeAccountOwnerId

        if (nextRound > scenario.targetRounds) {
            viewModelScope.launch {
                practiceEngine.recordConversationScenarioComplete(
                    accountOwnerId = ownerId,
                    scenario = scenario,
                    userTurns = updatedTurns,
                    score = 88
                )
                _sessionState.value = _sessionState.value.copy(
                    scenarioUserTurns = updatedTurns,
                    scenarioCurrentRound = nextRound,
                    sessionCompleteMessage = "Simulation Complete! Scenario marked as cleared."
                )
            }
        } else {
            _sessionState.value = _sessionState.value.copy(
                scenarioUserTurns = updatedTurns,
                scenarioCurrentRound = nextRound,
                userDraftText = ""
            )
        }
    }

    // Listening Practice Execution
    fun startListeningExercise(exerciseId: String) {
        val ex = SocialPracticeCatalog.listeningExercises.find { it.exerciseId == exerciseId } ?: return
        val session = practiceEngine.createPracticeSession(
            accountOwnerId = activeAccountOwnerId,
            module = ex.module,
            activityKind = SocialActivityKind.LISTENING_EXERCISE
        )
        _sessionState.value = PracticeActiveSessionUiState(
            currentListening = ex,
            activeSession = session
        )
    }

    fun submitListeningExercise(summaryText: String, followUpQuestionsText: String) {
        val ex = _sessionState.value.currentListening ?: return
        val session = _sessionState.value.activeSession ?: return

        viewModelScope.launch {
            _sessionState.value = _sessionState.value.copy(isEvaluating = true)
            practiceEngine.completeAndEvaluateListening(
                session = session.copy(durationSeconds = 180),
                exercise = ex,
                userSummaryText = summaryText,
                identifiedQuestionsText = followUpQuestionsText
            ).onSuccess { (eval, reward) ->
                _sessionState.value = _sessionState.value.copy(
                    isEvaluating = false,
                    evaluationResult = eval,
                    rewardEarned = reward,
                    sessionCompleteMessage = "Listening Evaluation Completed."
                )
            }.onFailure { err ->
                _sessionState.value = _sessionState.value.copy(
                    isEvaluating = false,
                    sessionCompleteMessage = "Error: ${err.localizedMessage}"
                )
            }
        }
    }

    // Phase 3: Advanced Scenarios & Simulators
    fun startAdvancedScenario(scenarioId: String) {
        val adv = AdvancedScenarioLibrary.scenarios.find { it.scenarioId == scenarioId } ?: return
        _sessionState.value = PracticeActiveSessionUiState(
            currentAdvancedScenario = adv,
            scenarioCurrentRound = 1,
            scenarioUserTurns = emptyList()
        )
    }

    fun startInterviewPractice(questionId: String) {
        val q = AdvancedScenarioLibrary.interviewQuestions.find { it.questionId == questionId } ?: return
        _sessionState.value = PracticeActiveSessionUiState(
            currentInterviewQuestion = q
        )
    }

    fun startLeadershipDecision(scenarioId: String) {
        val lead = AdvancedScenarioLibrary.leadershipScenarios.find { it.scenarioId == scenarioId } ?: return
        _sessionState.value = PracticeActiveSessionUiState(
            currentLeadershipScenario = lead
        )
    }

    fun resetSessionState() {
        _sessionState.value = PracticeActiveSessionUiState()
    }

    // Phase 4: Real-World Social Missions
    fun selectMission(missionId: String) {
        val mission = SocialMissionCatalog.getMissionById(missionId) ?: return
        val activeRecord = _uiState.value.activeMissionRecords.find { it.missionId == missionId }
        _uiState.value = _uiState.value.copy(
            selectedMission = mission,
            selectedMissionRecord = activeRecord,
            missionSubmissionMessage = null
        )
    }

    fun startMission(missionId: String) {
        val ownerId = activeAccountOwnerId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmittingMission = true)
            socialRepository.startMission(ownerId, missionId)
                .onSuccess { record ->
                    val mission = SocialMissionCatalog.getMissionById(missionId)
                    _uiState.value = _uiState.value.copy(
                        isSubmittingMission = false,
                        selectedMission = mission,
                        selectedMissionRecord = record,
                        missionSubmissionMessage = "Mission Accepted: ${record.title}"
                    )
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(
                        isSubmittingMission = false,
                        missionSubmissionMessage = "Error starting mission: ${err.localizedMessage}"
                    )
                }
        }
    }

    fun postponeMission(recordId: String) {
        val ownerId = activeAccountOwnerId
        viewModelScope.launch {
            socialRepository.postponeMission(recordId, ownerId)
                .onSuccess { record ->
                    _uiState.value = _uiState.value.copy(
                        selectedMissionRecord = record,
                        missionSubmissionMessage = "Mission postponed without penalty."
                    )
                }
        }
    }

    fun cancelMission(recordId: String) {
        val ownerId = activeAccountOwnerId
        viewModelScope.launch {
            socialRepository.cancelMission(recordId, ownerId)
                .onSuccess { record ->
                    _uiState.value = _uiState.value.copy(
                        selectedMissionRecord = record,
                        missionSubmissionMessage = "Mission cancelled."
                    )
                }
        }
    }

    fun retryMission(recordId: String) {
        val ownerId = activeAccountOwnerId
        viewModelScope.launch {
            socialRepository.retryMission(recordId, ownerId)
                .onSuccess { record ->
                    _uiState.value = _uiState.value.copy(
                        selectedMissionRecord = record,
                        missionSubmissionMessage = "Mission ready for retry. You can execute and submit an updated reflection."
                    )
                }.onFailure { err ->
                    _uiState.value = _uiState.value.copy(
                        missionSubmissionMessage = "Retry failed: ${err.localizedMessage}"
                    )
                }
        }
    }

    fun submitMissionReflection(
        recordId: String,
        evidenceType: SocialMissionEvidenceType,
        reflectionText: String,
        humanConfirmingRole: String? = null,
        artifactNote: String? = null,
        aiFeedbackNotes: String? = null,
        artifactAttachmentUri: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        val ownerId = activeAccountOwnerId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmittingMission = true, missionSubmissionMessage = null)
            socialRepository.submitMissionReflection(
                recordId = recordId,
                accountOwnerId = ownerId,
                evidenceType = evidenceType,
                reflectionText = reflectionText,
                humanConfirmingRole = humanConfirmingRole,
                artifactNote = artifactNote,
                aiFeedbackNotes = aiFeedbackNotes,
                artifactAttachmentUri = artifactAttachmentUri
            ).onSuccess { (record, reward) ->
                _uiState.value = _uiState.value.copy(
                    isSubmittingMission = false,
                    selectedMissionRecord = record,
                    missionSubmissionMessage = "Mission Cleared! +${reward.xpAwarded} XP awarded."
                )
                onSuccess()
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isSubmittingMission = false,
                    missionSubmissionMessage = "Submission failed: ${err.localizedMessage}"
                )
            }
        }
    }

    // Phase 4: Skill Assessments
    fun submitSkillAssessment(
        module: SocialModuleIdentifier,
        s1: Int,
        s2: Int,
        s3: Int,
        s4: Int,
        s5: Int,
        notes: String
    ) {
        val ownerId = activeAccountOwnerId
        viewModelScope.launch {
            val submission = SkillAssessmentSubmission(
                assessmentId = UUID.randomUUID().toString(),
                accountOwnerId = ownerId,
                module = module,
                scoreDim1 = s1,
                scoreDim2 = s2,
                scoreDim3 = s3,
                scoreDim4 = s4,
                scoreDim5 = s5,
                qualitativeNotes = notes
            )
            socialRepository.recordSkillAssessment(submission)
                .onSuccess { (_, reward) ->
                    val evalResult = SocialSkillAssessmentEngine.evaluateSubmission(submission)
                    _uiState.value = _uiState.value.copy(
                        latestAssessmentResult = evalResult,
                        missionSubmissionMessage = "Skill Assessment recorded. Overall Score: ${evalResult.overallScore}%. +${reward.xpAwarded} XP."
                    )
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(
                        missionSubmissionMessage = "Assessment failed: ${err.localizedMessage}"
                    )
                }
        }
    }

    fun requestAiMissionPreparation(mission: RealWorldSocialMission) {
        val coach = aiCoach ?: return
        viewModelScope.launch {
            val prompt = "I am preparing for real-world mission '${mission.title}' (${mission.difficulty.label}). " +
                    "Objective: ${mission.learningObjective}. What are 2 tactical mental preparation tips to overcome hesitation?"
            coach.sendMessage(prompt, AiCoachingMode.PRACTICE_COACH, _uiState.value.profile)
        }
    }

    fun requestAiReflectionReview(mission: RealWorldSocialMission, reflectionText: String) {
        val coach = aiCoach ?: return
        viewModelScope.launch {
            val prompt = "I completed real-world mission '${mission.title}'. Here is my reflection: '$reflectionText'. " +
                    "Please provide 2 constructive improvement observations."
            coach.sendMessage(prompt, AiCoachingMode.PERSONAL_COACH, _uiState.value.profile)
        }
    }

    fun clearMissionMessage() {
        _uiState.value = _uiState.value.copy(missionSubmissionMessage = null)
    }

    // =========================================================================
    // PHASE 5: ADAPTIVE RECOMMENDATIONS & TRAINING PLAN ACTIONS
    // =========================================================================

    fun refreshRecommendations() {
        val ownerId = activeAccountOwnerId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            socialRepository.generateAndSaveRecommendations(ownerId)
                .onSuccess { recs ->
                    _uiState.value = _uiState.value.copy(
                        activeRecommendations = recs,
                        isLoading = false,
                        missionSubmissionMessage = "Generated ${recs.size} personalized recommendations."
                    )
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Recommendation error: ${err.localizedMessage}"
                    )
                }
        }
    }

    fun acceptRecommendation(recommendationId: String) {
        val ownerId = activeAccountOwnerId
        viewModelScope.launch {
            socialRepository.updateRecommendationStatus(recommendationId, ownerId, SocialRecommendationStatus.ACCEPTED)
        }
    }

    fun dismissRecommendation(recommendationId: String) {
        val ownerId = activeAccountOwnerId
        viewModelScope.launch {
            socialRepository.updateRecommendationStatus(recommendationId, ownerId, SocialRecommendationStatus.DISMISSED)
        }
    }

    fun generateWeeklyPlan(targetDays: Int = 5, targetMinutes: Int = 15) {
        val ownerId = activeAccountOwnerId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGeneratingPlan = true)
            socialRepository.generateWeeklyTrainingPlan(ownerId, targetDays, targetMinutes)
                .onSuccess { plan ->
                    _uiState.value = _uiState.value.copy(
                        currentTrainingPlan = plan,
                        isGeneratingPlan = false,
                        missionSubmissionMessage = "Personalized weekly training plan created (${plan.targetDaysPerWeek} days, ${plan.targetMinutesPerDay}m/day)."
                    )
                }
                .onFailure { err ->
                    _uiState.value = _uiState.value.copy(
                        isGeneratingPlan = false,
                        errorMessage = "Plan generation failed: ${err.localizedMessage}"
                    )
                }
        }
    }

    fun completePlannedActivity(activityId: String) {
        val ownerId = activeAccountOwnerId
        viewModelScope.launch {
            socialRepository.updateActivityStatus(activityId, ownerId, isCompleted = true, isSkipped = false)
        }
    }

    fun skipPlannedActivity(activityId: String) {
        val ownerId = activeAccountOwnerId
        viewModelScope.launch {
            socialRepository.updateActivityStatus(activityId, ownerId, isCompleted = false, isSkipped = true)
        }
    }

    fun reschedulePlannedActivity(activityId: String, newDateEpochMs: Long, newDayOfWeek: Int) {
        val ownerId = activeAccountOwnerId
        viewModelScope.launch {
            socialRepository.rescheduleActivity(activityId, ownerId, newDateEpochMs, newDayOfWeek)
        }
    }

    fun togglePauseTrainingPlan(planId: String, isPaused: Boolean) {
        val ownerId = activeAccountOwnerId
        viewModelScope.launch {
            socialRepository.pauseOrResumeTrainingPlan(planId, ownerId, isPaused)
        }
    }

    fun setCompetencyDifficultyOverride(competencyId: String, difficulty: AdaptiveDifficulty) {
        val ownerId = activeAccountOwnerId
        viewModelScope.launch {
            socialRepository.updateCompetencyDifficultyOverride(competencyId, ownerId, difficulty)
        }
    }

    fun loadWeeklyReport() {
        val ownerId = activeAccountOwnerId
        viewModelScope.launch {
            socialRepository.generateWeeklyReport(ownerId)
                .onSuccess { report ->
                    _uiState.value = _uiState.value.copy(weeklyDevelopmentReport = report)
                }
        }
    }

    // AI Coach Advisory Actions (0 XP)
    fun requestAiRecommendationExplanation(rec: PersonalizedSocialRecommendation) {
        val coach = aiCoach ?: return
        viewModelScope.launch {
            coach.explainRecommendation(rec, _uiState.value.profile)
        }
    }

    fun requestAiPlanExplanation(plan: PersonalizedWeeklyTrainingPlan) {
        val coach = aiCoach ?: return
        viewModelScope.launch {
            coach.explainWeeklyTrainingPlan(plan, _uiState.value.profile)
        }
    }

    fun requestAiWeeklyReview(report: SocialWeeklyDevelopmentReport) {
        val coach = aiCoach ?: return
        viewModelScope.launch {
            coach.provideWeeklyReviewSummary(report, _uiState.value.profile)
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceCoachController?.release()
    }
}
