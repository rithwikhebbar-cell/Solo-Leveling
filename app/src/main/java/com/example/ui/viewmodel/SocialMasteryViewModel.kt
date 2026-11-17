package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.SocialRepository
import com.example.domain.engine.AdvancedScenarioLibrary
import com.example.domain.engine.CommunicationPracticeEngine
import com.example.domain.engine.SocialMasteryAnalyticsEngine
import com.example.domain.engine.SocialPracticeCatalog
import com.example.domain.model.ActivityRewardCalculation
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val errorMessage: String? = null
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
    private val accountOwnerId: String = "local_vault_player_1"
) : ViewModel() {

    private val _uiState = MutableStateFlow(SocialDashboardUiState())
    val uiState: StateFlow<SocialDashboardUiState> = _uiState.asStateFlow()

    private val _sessionState = MutableStateFlow(PracticeActiveSessionUiState())
    val sessionState: StateFlow<PracticeActiveSessionUiState> = _sessionState.asStateFlow()

    private val _aiCoachState = MutableStateFlow(AiCoachUiState())
    val aiCoachState: StateFlow<AiCoachUiState> = _aiCoachState.asStateFlow()

    init {
        loadSocialFoundation()
        observeAiCoachStreams()
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
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            runCatching {
                socialRepository.getOrCreateProfile(accountOwnerId)
                socialRepository.initializeMissingModuleProgress(accountOwnerId)
                socialRepository.seedInitialScenariosIfEmpty()
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(errorMessage = err.localizedMessage)
            }

            // Load Phase 2 + Phase 3 + Phase 4 Catalogues
            val analytics = runCatching { analyticsEngine.computeAnalyticsSummary(accountOwnerId) }.getOrNull()

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
        }

        socialRepository.observeProfile(accountOwnerId)
            .onEach { prof ->
                _uiState.value = _uiState.value.copy(profile = prof, isLoading = false)
            }
            .launchIn(viewModelScope)

        socialRepository.observeMissionRecords(accountOwnerId)
            .onEach { records ->
                val active = records.filter { it.lifecycleStatus in listOf(
                    SocialMissionLifecycleStatus.ACCEPTED,
                    SocialMissionLifecycleStatus.IN_PROGRESS,
                    SocialMissionLifecycleStatus.SUBMITTED,
                    SocialMissionLifecycleStatus.UNDER_REVIEW
                ) }
                _uiState.value = _uiState.value.copy(activeMissionRecords = active)
            }
            .launchIn(viewModelScope)

        socialRepository.observeAllModuleProgress(accountOwnerId)
            .onEach { progress ->
                val mapped = SocialModuleIdentifier.entries.map { mod ->
                    progress.find { it.module == mod } ?: SocialModuleProgress(
                        accountOwnerId = accountOwnerId,
                        module = mod,
                        masteryStage = SocialMasteryStage.NOVICE
                    )
                }
                _uiState.value = _uiState.value.copy(moduleProgressList = mapped, isLoading = false)
            }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            socialRepository.observeRecentSessions(accountOwnerId, 20)
                .onEach { sessions ->
                    _uiState.value = _uiState.value.copy(recentSessions = sessions)
                }
                .launchIn(viewModelScope)
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
            accountOwnerId = accountOwnerId,
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

        if (nextRound > scenario.targetRounds) {
            viewModelScope.launch {
                practiceEngine.recordConversationScenarioComplete(
                    accountOwnerId = accountOwnerId,
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
            accountOwnerId = accountOwnerId,
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
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmittingMission = true)
            socialRepository.startMission(accountOwnerId, missionId)
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
        viewModelScope.launch {
            socialRepository.postponeMission(recordId, accountOwnerId)
                .onSuccess { record ->
                    _uiState.value = _uiState.value.copy(
                        selectedMissionRecord = record,
                        missionSubmissionMessage = "Mission postponed without penalty."
                    )
                }
        }
    }

    fun cancelMission(recordId: String) {
        viewModelScope.launch {
            socialRepository.cancelMission(recordId, accountOwnerId)
                .onSuccess { record ->
                    _uiState.value = _uiState.value.copy(
                        selectedMissionRecord = record,
                        missionSubmissionMessage = "Mission cancelled."
                    )
                }
        }
    }

    fun retryMission(recordId: String) {
        viewModelScope.launch {
            socialRepository.retryMission(recordId, accountOwnerId)
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
        aiFeedbackNotes: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmittingMission = true)
            socialRepository.submitMissionReflection(
                recordId = recordId,
                accountOwnerId = accountOwnerId,
                evidenceType = evidenceType,
                reflectionText = reflectionText,
                humanConfirmingRole = humanConfirmingRole,
                artifactNote = artifactNote,
                aiFeedbackNotes = aiFeedbackNotes
            ).onSuccess { (record, reward) ->
                _uiState.value = _uiState.value.copy(
                    isSubmittingMission = false,
                    selectedMissionRecord = record,
                    missionSubmissionMessage = "Mission Cleared! +${reward.xpAwarded} XP awarded."
                )
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
        viewModelScope.launch {
            val submission = SkillAssessmentSubmission(
                assessmentId = UUID.randomUUID().toString(),
                accountOwnerId = accountOwnerId,
                module = module,
                scoreDim1 = s1,
                scoreDim2 = s2,
                scoreDim3 = s3,
                scoreDim4 = s4,
                scoreDim5 = s5,
                qualitativeNotes = notes
            )
            socialRepository.recordSkillAssessment(submission)
                .onSuccess { (assessment, reward) ->
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

    override fun onCleared() {
        super.onCleared()
        voiceCoachController?.release()
    }
}
