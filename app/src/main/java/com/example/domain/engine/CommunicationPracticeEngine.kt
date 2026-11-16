package com.example.domain.engine

import com.example.data.repository.SocialRepository
import com.example.domain.model.ActivityRewardCalculation
import com.example.domain.model.ConversationDialogueTurn
import com.example.domain.model.ConversationScenario
import com.example.domain.model.ListeningExercise
import com.example.domain.model.PracticeCategory
import com.example.domain.model.PracticeDifficulty
import com.example.domain.model.PracticeEvaluationResult
import com.example.domain.model.PracticeExercise
import com.example.domain.model.PracticeSessionLifecycleState
import com.example.domain.model.SocialActivityKind
import com.example.domain.model.SocialAssessment
import com.example.domain.model.SocialAssessmentType
import com.example.domain.model.SocialEvidenceSource
import com.example.domain.model.SocialModuleIdentifier
import com.example.domain.model.SocialPracticeSession
import com.example.domain.model.SocialScenarioAttempt
import com.example.domain.model.SocialSessionStatus
import com.example.domain.model.VerificationStatus
import com.example.service.AudioLifecycleManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.UUID

/**
 * Milestone 12 - Phase 2: Communication Practice Engine.
 * Coordinates exercises, conversation simulations, listening drills,
 * speech rehearsal lifecycle, evaluation, and progression integration.
 */
class CommunicationPracticeEngine(
    private val socialRepository: SocialRepository,
    private val audioLifecycleManager: AudioLifecycleManager? = null
) {
    private val _activeLifecycleState = MutableStateFlow(PracticeSessionLifecycleState.NOT_STARTED)
    val activeLifecycleState: StateFlow<PracticeSessionLifecycleState> = _activeLifecycleState.asStateFlow()

    // 1. Catalogue Queries
    fun getExercisesByCategory(category: PracticeCategory): List<PracticeExercise> {
        return SocialPracticeCatalog.exercises.filter { it.category == category }
    }

    fun getExercisesByModule(module: SocialModuleIdentifier): List<PracticeExercise> {
        return SocialPracticeCatalog.exercises.filter { it.module == module }
    }

    fun getExerciseById(id: String): PracticeExercise? {
        return SocialPracticeCatalog.exercises.find { it.exerciseId == id }
    }

    fun getConversationScenarios(): List<ConversationScenario> {
        return SocialPracticeCatalog.conversationScenarios
    }

    fun getConversationScenarioById(id: String): ConversationScenario? {
        return SocialPracticeCatalog.conversationScenarios.find { it.scenarioId == id }
    }

    fun getListeningExercises(): List<ListeningExercise> {
        return SocialPracticeCatalog.listeningExercises
    }

    // 2. Session Lifecycle Management
    fun createPracticeSession(
        accountOwnerId: String,
        module: SocialModuleIdentifier,
        activityKind: SocialActivityKind,
        durationSeconds: Int = 0,
        hasAudioRecording: Boolean = false,
        audioFileName: String? = null,
        transcriptText: String? = null
    ): SocialPracticeSession {
        _activeLifecycleState.value = PracticeSessionLifecycleState.IN_PROGRESS
        val sessionId = UUID.randomUUID().toString()
        val idempotencyKey = "practice_session_${accountOwnerId}_${sessionId}"

        return SocialPracticeSession(
            sessionId = sessionId,
            accountOwnerId = accountOwnerId,
            module = module,
            activityKind = activityKind,
            sessionStatus = SocialSessionStatus.IN_PROGRESS,
            durationSeconds = durationSeconds,
            startedEpochMs = System.currentTimeMillis(),
            transcriptText = transcriptText,
            hasAudioRecording = hasAudioRecording,
            audioFileName = audioFileName,
            isCloudTranscriptSyncAllowed = false,
            evidenceSource = if (hasAudioRecording || durationSeconds > 0) SocialEvidenceSource.SYSTEM_OBSERVED else SocialEvidenceSource.SELF_REPORTED,
            verificationStatus = if (hasAudioRecording || durationSeconds > 0) VerificationStatus.VERIFIED_SYSTEM_TIMER else VerificationStatus.SELF_REPORTED,
            idempotencyKey = idempotencyKey
        )
    }

    fun pauseSession() {
        if (_activeLifecycleState.value == PracticeSessionLifecycleState.IN_PROGRESS) {
            _activeLifecycleState.value = PracticeSessionLifecycleState.PAUSED
        }
    }

    fun resumeSession() {
        if (_activeLifecycleState.value == PracticeSessionLifecycleState.PAUSED) {
            _activeLifecycleState.value = PracticeSessionLifecycleState.IN_PROGRESS
        }
    }

    // 3. Audio Rehearsal Integration
    suspend fun prepareSpeechRecording(accountOwnerId: String, sessionId: String): Result<File>? {
        return audioLifecycleManager?.prepareNewRecordingFile(accountOwnerId, sessionId)
    }

    suspend fun finalizeSpeechRecording(
        accountOwnerId: String,
        file: File,
        durationSeconds: Int
    ): Result<String>? {
        return audioLifecycleManager?.validateAndFinalizeRecording(accountOwnerId, file, durationSeconds)
    }

    suspend fun deleteSpeechRecording(accountOwnerId: String, fileName: String?): Boolean {
        return audioLifecycleManager?.deleteRecording(accountOwnerId, fileName) ?: true
    }

    // 4. Session Evaluation & Progression Completion
    suspend fun completeAndEvaluateExercise(
        session: SocialPracticeSession,
        exercise: PracticeExercise,
        userResponseText: String
    ): Result<Pair<PracticeEvaluationResult, ActivityRewardCalculation>> {
        _activeLifecycleState.value = PracticeSessionLifecycleState.SUBMITTED

        // Step 1: Run deterministic speech/text evaluation
        val evalResult = SpeechEvaluationEngine.evaluatePracticeTextResponse(
            exercise = exercise,
            userResponseText = userResponseText,
            durationSeconds = session.durationSeconds,
            hasAudioRecording = session.hasAudioRecording
        )
        _activeLifecycleState.value = PracticeSessionLifecycleState.EVALUATED

        // Step 2: Atomic session completion & progression reward via SocialRepository
        val completedSession = session.copy(
            transcriptText = userResponseText,
            sessionStatus = SocialSessionStatus.COMPLETED
        )

        val completionResult = socialRepository.completePracticeSession(
            session = completedSession,
            rubricScorePercent = evalResult.overallScore
        )

        return completionResult.map { (_, rewardCalc) ->
            _activeLifecycleState.value = PracticeSessionLifecycleState.COMPLETED
            
            // Record structured assessment for the evaluation history
            socialRepository.recordAssessment(
                SocialAssessment(
                    assessmentId = UUID.randomUUID().toString(),
                    accountOwnerId = session.accountOwnerId,
                    module = session.module,
                    assessmentType = SocialAssessmentType.COMPREHENSIVE_RUBRIC,
                    overallScore = evalResult.overallScore,
                    clarityScore = evalResult.dimensionScores["Clarity"] ?: 70,
                    empathyScore = evalResult.dimensionScores["Confidence"] ?: 70,
                    assertivenessScore = evalResult.dimensionScores["Confidence"] ?: 70,
                    coherenceScore = evalResult.dimensionScores["Conciseness"] ?: 70,
                    qualitativeAnalysis = evalResult.qualitativeSummary,
                    actionableRecommendations = evalResult.improvementAreas
                )
            )

            evalResult to rewardCalc
        }
    }

    suspend fun completeAndEvaluateListening(
        session: SocialPracticeSession,
        exercise: ListeningExercise,
        userSummaryText: String,
        identifiedQuestionsText: String
    ): Result<Pair<PracticeEvaluationResult, ActivityRewardCalculation>> {
        _activeLifecycleState.value = PracticeSessionLifecycleState.SUBMITTED

        val evalResult = SpeechEvaluationEngine.evaluateListeningResponse(
            exercise = exercise,
            userSummaryText = userSummaryText,
            identifiedQuestionsText = identifiedQuestionsText
        )
        _activeLifecycleState.value = PracticeSessionLifecycleState.EVALUATED

        val completedSession = session.copy(
            transcriptText = "Summary: $userSummaryText\nQuestions: $identifiedQuestionsText",
            sessionStatus = SocialSessionStatus.COMPLETED
        )

        val completionResult = socialRepository.completePracticeSession(
            session = completedSession,
            rubricScorePercent = evalResult.overallScore
        )

        return completionResult.map { (_, rewardCalc) ->
            _activeLifecycleState.value = PracticeSessionLifecycleState.COMPLETED
            evalResult to rewardCalc
        }
    }

    suspend fun recordConversationScenarioComplete(
        accountOwnerId: String,
        scenario: ConversationScenario,
        userTurns: List<String>,
        score: Int = 85
    ): Result<SocialScenarioAttempt> {
        val attemptId = UUID.randomUUID().toString()
        val idempotencyKey = "sim_attempt_${accountOwnerId}_${scenario.scenarioId}_${System.currentTimeMillis()}"

        val attempt = SocialScenarioAttempt(
            attemptId = attemptId,
            accountOwnerId = accountOwnerId,
            scenarioId = scenario.scenarioId,
            scenarioTitle = scenario.title,
            module = scenario.module,
            userResponseText = userTurns.joinToString(" | "),
            evaluationScore = score,
            isPassed = score >= 70,
            strengthsFeedback = listOf("Successfully navigated all roleplay rounds", "Maintained positive conversational rapport"),
            improvementAreas = listOf("Try exploring alternative approaches in the next attempt"),
            evidenceSource = SocialEvidenceSource.SYSTEM_OBSERVED,
            verificationStatus = VerificationStatus.VERIFIED_ASSESSMENT,
            idempotencyKey = idempotencyKey
        )

        return socialRepository.recordScenarioAttempt(attempt)
    }
}
