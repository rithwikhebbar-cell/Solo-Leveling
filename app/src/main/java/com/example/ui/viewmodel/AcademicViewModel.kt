package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.integration.AiGameMasterPort
import com.example.data.repository.AcademicRepository
import com.example.data.repository.RealityOsRepository
import com.example.domain.engine.ExamCountdownCalculator
import com.example.domain.engine.RevisionEngine
import com.example.domain.model.AcademicAssessment
import com.example.domain.model.AcademicChapter
import com.example.domain.model.AcademicDashboardSummary
import com.example.domain.model.AcademicExam
import com.example.domain.model.AcademicModule
import com.example.domain.model.AcademicPriority
import com.example.domain.model.AcademicProgressionRecord
import com.example.domain.model.AcademicQuest
import com.example.domain.model.AcademicQuestCategory
import com.example.domain.model.AcademicQuestDifficulty
import com.example.domain.model.AcademicQuestStatus
import com.example.domain.model.AcademicStudyPlanProposal
import com.example.domain.model.AcademicSubject
import com.example.domain.model.AcademicTopic
import com.example.domain.model.ActiveFocusTimerState
import com.example.domain.model.AssessmentType
import com.example.domain.model.ChapterDifficulty
import com.example.domain.model.ExamReadinessSummary
import com.example.domain.model.FocusPreset
import com.example.domain.model.MarkingSystem
import com.example.domain.model.PlayerProfile
import com.example.domain.model.RevisionRecord
import com.example.domain.model.RevisionUrgency
import com.example.domain.model.ScheduledRevisionItem
import com.example.domain.model.StudySession
import com.example.domain.model.SubjectColorToken
import com.example.domain.model.SubjectIconKey
import com.example.domain.model.SubjectProgressSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

sealed interface AcademicUiState {
    data object Loading : AcademicUiState
    data class Error(val message: String) : AcademicUiState
    data class Ready(
        val player: PlayerProfile,
        val summary: AcademicDashboardSummary,
        val subjects: List<AcademicSubject>,
        val subjectSummaries: List<SubjectProgressSummary>,
        val modules: List<AcademicModule>,
        val chapters: List<AcademicChapter>,
        val topics: List<AcademicTopic>,
        val studySessions: List<StudySession>,
        val revisionQueue: List<ScheduledRevisionItem>,
        val revisionRecords: List<RevisionRecord>,
        val exams: List<AcademicExam>,
        val examReadiness: List<ExamReadinessSummary>,
        val assessments: List<AcademicAssessment>,
        val academicQuests: List<AcademicQuest>,
        val progressionRecords: List<AcademicProgressionRecord>,
        val activeFocusTimer: ActiveFocusTimerState,
        val studyPlanProposal: AcademicStudyPlanProposal?,
        val openRouterPreviewPrompt: String,
        val feedbackMessage: String?
    ) : AcademicUiState
}

private data class CurriculumBundle(
    val subjects: List<AcademicSubject>,
    val modules: List<AcademicModule>,
    val chapters: List<AcademicChapter>,
    val topics: List<AcademicTopic>
)

private data class AcademicActivityBundle(
    val sessions: List<StudySession>,
    val revisions: List<RevisionRecord>,
    val exams: List<AcademicExam>,
    val assessments: List<AcademicAssessment>,
    val quests: List<AcademicQuest>,
    val progression: List<AcademicProgressionRecord>
)

private data class AcademicTransientState(
    val focusTimer: ActiveFocusTimerState = ActiveFocusTimerState(),
    val feedbackMessage: String? = null,
    val dismissedProposalGoal: Int? = null
)

class AcademicViewModel(
    private val academicRepository: AcademicRepository,
    private val realityOsRepository: RealityOsRepository,
    private val aiGameMasterPort: AiGameMasterPort
) : ViewModel() {

    private val transientState = MutableStateFlow(AcademicTransientState())

    private val curriculumFlow = combine(
        academicRepository.subjects,
        academicRepository.modules,
        academicRepository.chapters,
        academicRepository.topics
    ) { subjects, modules, chapters, topics ->
        CurriculumBundle(subjects, modules, chapters, topics)
    }

    private val activityFlow = combine(
        academicRepository.studySessions,
        academicRepository.revisionRecords,
        academicRepository.exams,
        academicRepository.assessments,
        academicRepository.academicQuests
    ) { sessions, revisions, exams, assessments, quests ->
        AcademicActivityBundle(sessions, revisions, exams, assessments, quests, emptyList())
    }.combine(academicRepository.progressionRecords) { bundle, progression ->
        bundle.copy(progression = progression)
    }

    val uiState: StateFlow<AcademicUiState> = combine(
        realityOsRepository.playerProfile,
        realityOsRepository.userPreferences,
        curriculumFlow,
        activityFlow,
        transientState
    ) { player, prefs, curriculum, activity, transient ->
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val todayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val sevenDaysAgo = now - 7 * 24 * 60 * 60_000L

        val subjectNamesById = curriculum.subjects.associate { it.id to it.name }
        val difficultyByChapterId = curriculum.chapters.associate { it.id to it.difficulty }
        val latestRatioBySubjectId = activity.assessments
            .groupBy { it.subjectId }
            .mapValues { (_, list) -> list.first().normalizedRatio }

        val subjectSummaries = curriculum.subjects.map { subject ->
            val mods = curriculum.modules.filter { it.subjectId == subject.id }
            val chaps = curriculum.chapters.filter { it.subjectId == subject.id }
            val tops = curriculum.topics.filter { it.subjectId == subject.id }
            val completedChaps = chaps.count { it.isCompleted }
            val completedTops = tops.count { it.isCompleted }
            val syllabusPct = when {
                tops.isNotEmpty() -> ((completedTops * 100f) / tops.size).toInt()
                chaps.isNotEmpty() -> ((completedChaps * 100f) / chaps.size).toInt()
                else -> 0
            }
            val studyMins = activity.sessions.filter { it.subjectId == subject.id }.sumOf { it.durationMinutes }
            val remainingMins = chaps.filter { !it.isCompleted }.sumOf { it.estimatedMinutes }
            SubjectProgressSummary(
                subject = subject,
                moduleCount = mods.size,
                chapterCount = chaps.size,
                completedChapters = completedChaps,
                topicCount = tops.size,
                completedTopics = completedTops,
                syllabusCompletionPercent = syllabusPct,
                totalStudyMinutes = studyMins,
                estimatedRemainingMinutes = remainingMins,
                latestAssessmentRatio = latestRatioBySubjectId[subject.id]
            )
        }

        val activeSubjects = curriculum.subjects.filter { !it.isArchived }
        val activeSubjectIds = activeSubjects.map { it.id }.toSet()
        val activeTopics = curriculum.topics.filter { it.subjectId in activeSubjectIds }

        val revisionQueue = RevisionEngine.buildQueue(
            topics = activeTopics,
            subjectNamesById = subjectNamesById,
            difficultyByChapterId = difficultyByChapterId,
            assessmentRatioBySubjectId = latestRatioBySubjectId,
            nowEpochMs = now
        )

        val examReadiness = activity.exams.map { exam ->
            val subjSummary = subjectSummaries.find { it.subject.id == exam.subjectId }
            val avgConf = activeTopics.filter { it.subjectId == exam.subjectId }
                .takeIf { it.isNotEmpty() }
                ?.map { it.confidenceEstimate.toFloat() }
                ?.average()
                ?.toFloat()
            ExamCountdownCalculator.evaluateReadiness(
                exam = exam,
                subjectSyllabusPercent = subjSummary?.syllabusCompletionPercent ?: 0,
                avgSubjectConfidenceEstimate = avgConf,
                recentAssessments = activity.assessments,
                nowEpochMs = now
            )
        }

        val todayMinutes = activity.sessions.filter { it.startEpochMs >= todayStart }.sumOf { it.durationMinutes } +
            activity.revisions.filter { it.revisedAtEpochMs >= todayStart }.sumOf { it.durationMinutes }
        val weeklyMinutes = activity.sessions.filter { it.startEpochMs >= sevenDaysAgo }.sumOf { it.durationMinutes } +
            activity.revisions.filter { it.revisedAtEpochMs >= sevenDaysAgo }.sumOf { it.durationMinutes }
        val totalMinutes = activity.sessions.sumOf { it.durationMinutes } +
            activity.revisions.sumOf { it.durationMinutes }

        val consistencyDays = (activity.sessions.filter { it.startEpochMs >= sevenDaysAgo }
            .map { Instant.ofEpochMilli(it.startEpochMs).atZone(zone).toLocalDate() } +
            activity.revisions.filter { it.revisedAtEpochMs >= sevenDaysAgo }
                .map { Instant.ofEpochMilli(it.revisedAtEpochMs).atZone(zone).toLocalDate() }).toSet().size

        val activeSummaries = subjectSummaries.filter { !it.subject.isArchived }
        val overallSyllabusPct = if (activeSummaries.isEmpty()) 0 else {
            activeSummaries.map { it.syllabusCompletionPercent }.average().toInt()
        }

        val dueCount = revisionQueue.count { it.urgency == RevisionUrgency.DUE_TODAY || it.urgency == RevisionUrgency.OVERDUE }
        val overdueCount = revisionQueue.count { it.urgency == RevisionUrgency.OVERDUE }
        val upcomingExamsCount = examReadiness.count { !it.exam.isCompleted && it.daysRemaining >= 0L }
        val completedQuestsCount = activity.quests.count { it.status == AcademicQuestStatus.COMPLETED }
        val totalAcademicXp = activity.progression.sumOf { it.xpAwarded }

        val summary = AcademicDashboardSummary(
            totalStudyMinutes = totalMinutes,
            todayStudyMinutes = todayMinutes,
            weeklyStudyMinutes = weeklyMinutes,
            dailyStudyGoalMinutes = prefs.academicDailyGoalMinutes,
            weeklyStudyGoalMinutes = prefs.academicWeeklyGoalMinutes,
            activeSubjectsCount = activeSubjects.size,
            overallSyllabusCompletionPercent = overallSyllabusPct,
            pendingRevisionsCount = dueCount,
            overdueRevisionsCount = overdueCount,
            upcomingExamsCount = upcomingExamsCount,
            completedAcademicQuestsCount = completedQuestsCount,
            totalAcademicXp = totalAcademicXp,
            intPoints = player.attributes.intel.totalValue,
            wisPoints = player.attributes.wis.totalValue,
            disPoints = player.attributes.dis.totalValue,
            studyConsistencyDays7d = consistencyDays
        )

        val nearestExam = examReadiness.firstOrNull { !it.exam.isCompleted && it.daysRemaining >= 0L }
        val rawProposal = aiGameMasterPort.generateAcademicStudyPlanProposal(
            profile = player,
            currentDailyGoalMinutes = prefs.academicDailyGoalMinutes,
            todayStudyMinutes = todayMinutes,
            dueRevisions = revisionQueue.filter { it.urgency != RevisionUrgency.UPCOMING },
            nearestExamTitle = nearestExam?.exam?.title,
            nearestExamDays = nearestExam?.daysRemaining
        )
        val proposal = rawProposal?.takeIf { it.recommendedDailyMinutes != transient.dismissedProposalGoal }

        val weakTopicLabels = revisionQueue.filter { it.retentionEstimatePercent < 65 }.take(3).map { it.topic.title }
        val examLabels = examReadiness.filter { !it.exam.isCompleted }.take(3).map { "${it.exam.title} (${it.daysRemaining}d)" }
        val openRouterPrompt = aiGameMasterPort.buildOpenRouterAcademicContextPrompt(
            profile = player,
            todayStudyMinutes = todayMinutes,
            dailyGoalMinutes = prefs.academicDailyGoalMinutes,
            weakTopics = weakTopicLabels,
            upcomingExams = examLabels
        ).messages.lastOrNull()?.content.orEmpty()

        AcademicUiState.Ready(
            player = player,
            summary = summary,
            subjects = curriculum.subjects,
            subjectSummaries = subjectSummaries,
            modules = curriculum.modules,
            chapters = curriculum.chapters,
            topics = curriculum.topics,
            studySessions = activity.sessions,
            revisionQueue = revisionQueue,
            revisionRecords = activity.revisions,
            exams = activity.exams,
            examReadiness = examReadiness,
            assessments = activity.assessments,
            academicQuests = activity.quests,
            progressionRecords = activity.progression,
            activeFocusTimer = transient.focusTimer,
            studyPlanProposal = proposal,
            openRouterPreviewPrompt = openRouterPrompt,
            feedbackMessage = transient.feedbackMessage
        ) as AcademicUiState
    }.catch { throwable ->
        emit(AcademicUiState.Error(throwable.message ?: "Unable to load Academic Engine state."))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AcademicUiState.Loading
    )

    fun clearFeedback() {
        transientState.value = transientState.value.copy(feedbackMessage = null)
    }

    fun createSubject(
        name: String,
        code: String,
        programSystem: String,
        colorToken: SubjectColorToken,
        iconKey: SubjectIconKey,
        priority: AcademicPriority,
        notes: String
    ) {
        viewModelScope.launch {
            val res = academicRepository.createSubject(name, code, programSystem, colorToken, iconKey, priority, notes)
            postFeedback(res.fold(
                onSuccess = { "Subject '${it.name}' created (no XP awarded for setup)." },
                onFailure = { it.message ?: "Unable to create subject." }
            ))
        }
    }

    fun updateSubject(subject: AcademicSubject) {
        viewModelScope.launch {
            val res = academicRepository.updateSubject(subject)
            postFeedback(res.fold(onSuccess = { "Subject updated." }, onFailure = { it.message ?: "Update failed." }))
        }
    }

    fun setSubjectArchived(subjectId: Long, archived: Boolean) {
        viewModelScope.launch {
            academicRepository.setSubjectArchived(subjectId, archived)
            postFeedback(if (archived) "Subject archived." else "Subject restored.")
        }
    }

    fun addModule(subjectId: Long, title: String, description: String) {
        viewModelScope.launch {
            val res = academicRepository.addModule(subjectId, title, description)
            postFeedback(res.fold(onSuccess = { "Module added." }, onFailure = { it.message ?: "Invalid module." }))
        }
    }

    fun addChapter(
        moduleId: Long,
        subjectId: Long,
        title: String,
        difficulty: ChapterDifficulty,
        estimatedMinutes: Int,
        notes: String
    ) {
        viewModelScope.launch {
            val res = academicRepository.addChapter(moduleId, subjectId, title, difficulty, estimatedMinutes, notes)
            postFeedback(res.fold(onSuccess = { "Chapter added." }, onFailure = { it.message ?: "Invalid chapter." }))
        }
    }

    fun setChapterCompleted(chapterId: Long, completed: Boolean) {
        viewModelScope.launch {
            val res = academicRepository.setChapterCompleted(chapterId, completed)
            val xp = res.getOrDefault(0)
            val msg = when {
                !completed -> "Chapter marked pending (earned rewards stay audited)."
                xp > 0 -> "Chapter completed (+$xp XP)."
                else -> "Chapter completed (already rewarded or daily cap reached)."
            }
            postFeedback(msg)
        }
    }

    fun addTopic(chapterId: Long, subjectId: Long, title: String, confidence: Int, notes: String) {
        viewModelScope.launch {
            val res = academicRepository.addTopic(chapterId, subjectId, title, confidence, notes)
            postFeedback(res.fold(onSuccess = { "Topic added to revision queue." }, onFailure = { it.message ?: "Invalid topic." }))
        }
    }

    fun toggleTopicCompleted(topicId: Long, completed: Boolean) {
        viewModelScope.launch {
            academicRepository.toggleTopicCompleted(topicId, completed)
        }
    }

    fun startFocusTimer(
        preset: FocusPreset,
        focusMinutes: Int,
        breakMinutes: Int,
        subject: AcademicSubject,
        topic: AcademicTopic?,
        notes: String
    ) {
        val now = System.currentTimeMillis()
        transientState.value = transientState.value.copy(
            focusTimer = ActiveFocusTimerState(
                isActive = true,
                isPaused = false,
                isBreakPhase = false,
                preset = preset,
                targetFocusMinutes = focusMinutes.coerceIn(1, 180),
                targetBreakMinutes = breakMinutes.coerceIn(0, 45),
                subjectId = subject.id,
                subjectName = subject.name,
                topicId = topic?.id,
                topicTitle = topic?.title.orEmpty(),
                notes = notes.trim(),
                startEpochMs = now,
                accumulatedElapsedMs = 0L,
                lastResumedAtMs = now
            ),
            feedbackMessage = "Focus timer started for ${subject.name}. Merely running a timer earns 0 XP until valid completion."
        )
    }

    fun pauseOrResumeFocusTimer() {
        val current = transientState.value.focusTimer
        if (!current.isActive) return
        val now = System.currentTimeMillis()
        val updated = if (current.isPaused) {
            current.copy(isPaused = false, lastResumedAtMs = now)
        } else {
            val delta = (now - current.lastResumedAtMs).coerceAtLeast(0L)
            current.copy(
                isPaused = true,
                accumulatedElapsedMs = current.accumulatedElapsedMs + delta,
                lastResumedAtMs = 0L
            )
        }
        transientState.value = transientState.value.copy(focusTimer = updated)
    }

    fun toggleFocusBreakPhase() {
        val current = transientState.value.focusTimer
        if (!current.isActive) return
        transientState.value = transientState.value.copy(
            focusTimer = current.copy(isBreakPhase = !current.isBreakPhase)
        )
    }

    fun discardFocusTimer() {
        transientState.value = transientState.value.copy(
            focusTimer = ActiveFocusTimerState(),
            feedbackMessage = "Focus session discarded without XP."
        )
    }

    fun completeFocusTimer(sessionTitle: String, notes: String) {
        val timer = transientState.value.focusTimer
        val subjectId = timer.subjectId ?: return
        if (!timer.isActive) return
        val now = System.currentTimeMillis()
        val elapsedSeconds = timer.currentElapsedSeconds(now)
        val durationMinutes = ((elapsedSeconds + 59L) / 60L).toInt().coerceAtLeast(1)

        viewModelScope.launch {
            val res = academicRepository.recordStudySession(
                subjectId = subjectId,
                topicId = timer.topicId,
                title = sessionTitle.ifBlank { "${timer.subjectName} Focus Block" },
                startEpochMs = timer.startEpochMs,
                endEpochMs = now,
                durationMinutes = durationMinutes,
                breakMinutes = timer.targetBreakMinutes,
                isSystemTimer = true,
                notes = notes.ifBlank { timer.notes }
            )
            transientState.value = transientState.value.copy(
                focusTimer = ActiveFocusTimerState(),
                feedbackMessage = res.fold(
                    onSuccess = { "Focus block recorded as Timer Verified (+${it.xpAwarded} XP)." },
                    onFailure = { it.message ?: "Session discarded." }
                )
            )
        }
    }

    fun logManualStudySession(
        subjectId: Long,
        topicId: Long?,
        title: String,
        durationMinutes: Int,
        notes: String
    ) {
        val endMs = System.currentTimeMillis()
        val startMs = endMs - durationMinutes.coerceAtLeast(1) * 60_000L
        viewModelScope.launch {
            val res = academicRepository.recordStudySession(
                subjectId = subjectId,
                topicId = topicId,
                title = title,
                startEpochMs = startMs,
                endEpochMs = endMs,
                durationMinutes = durationMinutes,
                breakMinutes = 0,
                isSystemTimer = false,
                notes = notes
            )
            postFeedback(res.fold(
                onSuccess = { "Self-reported study logged (+${it.xpAwarded} XP)." },
                onFailure = { it.message ?: "Invalid study log." }
            ))
        }
    }

    fun completeTopicRevision(topicId: Long, confidenceAfter: Int, durationMinutes: Int, notes: String) {
        viewModelScope.launch {
            val res = academicRepository.completeTopicRevision(topicId, confidenceAfter, durationMinutes, notes)
            postFeedback(res.fold(
                onSuccess = { "Revision recorded (+${it.xpAwarded} XP). Next review in ${it.intervalDaysScheduled}d." },
                onFailure = { it.message ?: "Unable to record revision." }
            ))
        }
    }

    fun createExam(
        subjectId: Long,
        title: String,
        daysFromNow: Int,
        markingSystem: MarkingSystem,
        targetScoreDisplay: String,
        targetNormalizedRatio: Float,
        syllabusCoveragePercent: Int,
        notes: String
    ) {
        val examEpochMs = System.currentTimeMillis() + daysFromNow.coerceIn(0, 730) * 24 * 60 * 60_000L
        viewModelScope.launch {
            val res = academicRepository.createExam(
                subjectId = subjectId,
                title = title,
                examDateEpochMs = examEpochMs,
                markingSystem = markingSystem,
                targetScoreDisplay = targetScoreDisplay,
                targetNormalizedRatio = targetNormalizedRatio,
                syllabusCoveragePercent = syllabusCoveragePercent,
                notes = notes
            )
            postFeedback(res.fold(
                onSuccess = { "Exam '${it.title}' scheduled." },
                onFailure = { it.message ?: "Unable to schedule exam." }
            ))
        }
    }

    fun generateExamPrepQuests(examId: Long) {
        viewModelScope.launch {
            val res = academicRepository.generateExamPrepQuests(examId)
            postFeedback(res.fold(
                onSuccess = { "Generated $it exam prep quests (complete them to earn XP)." },
                onFailure = { it.message ?: "Unable to generate prep quests." }
            ))
        }
    }

    fun recordAssessmentResult(
        subjectId: Long,
        examId: Long?,
        topicId: Long?,
        title: String,
        assessmentType: AssessmentType,
        markingSystem: MarkingSystem,
        scoredMarks: Double,
        maxMarks: Double,
        gradeLabel: String,
        hasVerifiedEvidence: Boolean,
        notes: String
    ) {
        viewModelScope.launch {
            val res = academicRepository.recordAssessmentResult(
                subjectId = subjectId,
                examId = examId,
                topicId = topicId,
                title = title,
                assessmentType = assessmentType,
                markingSystem = markingSystem,
                scoredMarks = scoredMarks,
                maxMarks = maxMarks,
                gradeLabel = gradeLabel,
                hasVerifiedEvidence = hasVerifiedEvidence,
                notes = notes
            )
            postFeedback(res.fold(
                onSuccess = { "Assessment recorded (${it.verificationStatus.label}, +${it.xpAwarded} XP)." },
                onFailure = { it.message ?: "Invalid assessment." }
            ))
        }
    }

    fun createAcademicQuest(
        title: String,
        description: String,
        category: AcademicQuestCategory,
        subjectId: Long?,
        difficulty: AcademicQuestDifficulty,
        objective: String,
        targetValue: Int,
        unit: String
    ) {
        viewModelScope.launch {
            val res = academicRepository.createAcademicQuest(
                title = title,
                description = description,
                category = category,
                subjectId = subjectId,
                topicId = null,
                difficulty = difficulty,
                objective = objective,
                targetValue = targetValue,
                unit = unit,
                deadlineEpochMs = null
            )
            postFeedback(res.fold(
                onSuccess = { "Academic quest added (0 XP for creation)." },
                onFailure = { it.message ?: "Unable to create quest." }
            ))
        }
    }

    fun generateQuestsFromStudyPlan() {
        viewModelScope.launch {
            val count = academicRepository.generateQuestsFromStudyPlan()
            postFeedback(if (count > 0) "Generated $count study quest(s) from your plan." else "Add an active subject first to generate study quests.")
        }
    }

    fun generateQuestsForSubject(subjectId: Long) {
        viewModelScope.launch {
            val res = academicRepository.generateQuestsForSubject(subjectId)
            postFeedback(res.fold(
                onSuccess = { "Generated $it study quest(s)." },
                onFailure = { it.message ?: "Unable to generate quests." }
            ))
        }
    }

    fun completeAcademicQuest(questId: Long, evidence: String = "") {
        viewModelScope.launch {
            val res = academicRepository.completeAcademicQuest(questId, evidence)
            postFeedback("Academic quest cleared (+${res.getOrDefault(0)} XP).")
        }
    }

    fun cancelAcademicQuest(questId: Long) {
        viewModelScope.launch {
            academicRepository.cancelAcademicQuest(questId)
            postFeedback("Quest cancelled.")
        }
    }

    fun approveStudyPlanProposal(minutes: Int) {
        viewModelScope.launch {
            academicRepository.approveAcademicStudyGoal(minutes)
            transientState.value = transientState.value.copy(
                dismissedProposalGoal = null,
                feedbackMessage = "Daily academic study goal updated to $minutes min."
            )
        }
    }

    fun dismissStudyPlanProposal(minutes: Int) {
        transientState.value = transientState.value.copy(dismissedProposalGoal = minutes)
    }

    private fun postFeedback(msg: String) {
        transientState.value = transientState.value.copy(feedbackMessage = msg)
    }

    companion object {
        fun factory(
            academicRepository: AcademicRepository,
            realityOsRepository: RealityOsRepository,
            aiGameMasterPort: AiGameMasterPort
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AcademicViewModel(academicRepository, realityOsRepository, aiGameMasterPort) as T
            }
        }
    }
}
