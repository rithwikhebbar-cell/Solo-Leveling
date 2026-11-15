package com.example.domain.model

enum class AcademicPriority(val label: String, val weight: Int) {
    CRITICAL("Critical", 4),
    HIGH("High", 3),
    MEDIUM("Medium", 2),
    LOW("Low", 1)
}

enum class ChapterDifficulty(val label: String, val weight: Float) {
    FOUNDATION("Foundation", 1.0f),
    INTERMEDIATE("Intermediate", 1.15f),
    ADVANCED("Advanced", 1.3f),
    BOSS("Boss Topic", 1.5f)
}

enum class SubjectIconKey(val label: String) {
    BOOK("Theory"),
    CODE("Computing"),
    SCIENCE("Science"),
    MATH("Mathematics"),
    LAW("Law & Policy"),
    MEDICAL("Clinical"),
    LANGUAGE("Language"),
    HISTORY("Humanities")
}

enum class SubjectColorToken(val label: String) {
    CYAN("Electric Cyan"),
    VIOLET("Arcane Violet"),
    EMERALD("Emerald"),
    AMBER("Amber"),
    BLUE("System Blue"),
    CRIMSON("Crimson"),
    GOLD("Monarch Gold"),
    SILVER("Metallic Silver")
}

enum class MarkingSystem(val label: String, val exampleHint: String) {
    PERCENTAGE("Percentage (%)", "e.g., 85 / 100"),
    POINTS_OUT_OF_MAX("Points / Marks", "e.g., 142 / 150"),
    GPA_4("4.0 GPA Scale", "e.g., 3.7 / 4.0"),
    LETTER_GRADE("Letter / Grade", "e.g., A / A-"),
    PASS_FAIL("Pass / Fail", "1 = Pass, 0 = Fail"),
    BAND_SCORE("Band / Rank Score", "e.g., 8.0 / 9.0")
}

enum class AcademicActivityKind(val label: String) {
    STUDY_SESSION("Study Session"),
    FOCUS_SESSION("Focus Timer Block"),
    REVISION("Spaced Revision"),
    CHAPTER_COMPLETION("Chapter Completion"),
    PRACTICE_ASSESSMENT("Practice Assessment"),
    EXAM_ASSESSMENT("Graded Examination"),
    ACADEMIC_QUEST("Academic Quest")
}

enum class AcademicQuestCategory(val label: String) {
    DAILY_STUDY("Daily Study"),
    WEEKLY_STUDY("Weekly Study"),
    CHAPTER_COMPLETION("Chapter Clear"),
    REVISION("Revision"),
    PRACTICE("Practice"),
    EXAM_PREP("Exam Prep"),
    LONG_TERM_MISSION("Long-Term Mission")
}

enum class AcademicQuestStatus(val label: String) {
    ACTIVE("Active"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled")
}

enum class AcademicQuestDifficulty(val label: String, val xpBonus: Int) {
    E_RANK("E-Rank", 25),
    D_RANK("D-Rank", 40),
    C_RANK("C-Rank", 60),
    B_RANK("B-Rank", 85),
    A_RANK("A-Rank", 110),
    S_RANK("S-Rank", 150)
}

enum class AssessmentType(val label: String, val supportsVerifiedEvidence: Boolean) {
    PRACTICE_QUIZ("Practice Quiz", false),
    MOCK_EXAM("Mock Examination", false),
    ASSIGNMENT("Graded Assignment", true),
    LAB_EVALUATION("Lab / Practical Evaluation", true),
    GRADED_EXAM("Official Exam Result", true)
}

enum class RevisionUrgency(val label: String) {
    OVERDUE("Overdue"),
    DUE_TODAY("Due Today"),
    UPCOMING("Upcoming")
}

enum class FocusPreset(val label: String, val focusMinutes: Int, val breakMinutes: Int) {
    POMODORO_25("25 / 5 Sprint", 25, 5),
    DEEP_50("50 / 10 Block", 50, 10),
    MONARCH_90("90 / 15 Deep Work", 90, 15),
    CUSTOM("Custom", 40, 10)
}

data class ActiveFocusTimerState(
    val isActive: Boolean = false,
    val isPaused: Boolean = false,
    val isBreakPhase: Boolean = false,
    val preset: FocusPreset = FocusPreset.POMODORO_25,
    val targetFocusMinutes: Int = 25,
    val targetBreakMinutes: Int = 5,
    val subjectId: Long? = null,
    val subjectName: String = "",
    val topicId: Long? = null,
    val topicTitle: String = "",
    val notes: String = "",
    val startEpochMs: Long = 0L,
    val accumulatedElapsedMs: Long = 0L,
    val lastResumedAtMs: Long = 0L
) {
    fun currentElapsedMs(nowEpochMs: Long = System.currentTimeMillis()): Long {
        if (!isActive) return 0L
        val running = if (isPaused || lastResumedAtMs <= 0L) 0L else (nowEpochMs - lastResumedAtMs).coerceAtLeast(0L)
        return accumulatedElapsedMs + running
    }

    fun currentElapsedSeconds(nowEpochMs: Long = System.currentTimeMillis()): Long =
        currentElapsedMs(nowEpochMs) / 1000L

    fun currentElapsedMinutes(nowEpochMs: Long = System.currentTimeMillis()): Int =
        ((currentElapsedSeconds(nowEpochMs) + 59L) / 60L).toInt().coerceAtLeast(1)
}

data class AcademicSubject(
    val id: Long = 0,
    val name: String,
    val code: String = "",
    val programSystem: String = "Flexible Programme",
    val colorToken: SubjectColorToken = SubjectColorToken.CYAN,
    val iconKey: SubjectIconKey = SubjectIconKey.BOOK,
    val priority: AcademicPriority = AcademicPriority.MEDIUM,
    val notes: String = "",
    val isArchived: Boolean = false,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

data class AcademicModule(
    val id: Long = 0,
    val subjectId: Long,
    val title: String,
    val description: String = "",
    val orderIndex: Int = 0
)

data class AcademicChapter(
    val id: Long = 0,
    val moduleId: Long,
    val subjectId: Long,
    val title: String,
    val difficulty: ChapterDifficulty = ChapterDifficulty.INTERMEDIATE,
    val estimatedMinutes: Int = 60,
    val isCompleted: Boolean = false,
    val completedAtEpochMs: Long? = null,
    val notes: String = "",
    val orderIndex: Int = 0
)

data class AcademicTopic(
    val id: Long = 0,
    val chapterId: Long,
    val subjectId: Long,
    val title: String,
    val isCompleted: Boolean = false,
    val confidenceEstimate: Int = 3,
    val revisionCount: Int = 0,
    val lastRevisedEpochMs: Long? = null,
    val nextRevisionDueEpochMs: Long? = null,
    val notes: String = ""
)

data class StudySession(
    val id: Long = 0,
    val sourceId: String,
    val subjectId: Long,
    val subjectName: String = "",
    val topicId: Long? = null,
    val topicTitle: String = "",
    val title: String,
    val activityKind: AcademicActivityKind = AcademicActivityKind.FOCUS_SESSION,
    val startEpochMs: Long,
    val endEpochMs: Long,
    val durationMinutes: Int,
    val breakMinutes: Int = 0,
    val verificationStatus: VerificationStatus,
    val xpAwarded: Int,
    val notes: String = ""
)

data class RevisionRecord(
    val id: Long = 0,
    val topicId: Long,
    val subjectId: Long,
    val topicTitle: String = "",
    val subjectName: String = "",
    val confidenceAfter: Int,
    val intervalDaysScheduled: Int,
    val nextDueEpochMs: Long,
    val durationMinutes: Int,
    val verificationStatus: VerificationStatus,
    val xpAwarded: Int,
    val notes: String = "",
    val revisedAtEpochMs: Long = System.currentTimeMillis()
)

data class ScheduledRevisionItem(
    val topic: AcademicTopic,
    val subjectName: String,
    val chapterDifficulty: ChapterDifficulty,
    val urgency: RevisionUrgency,
    val retentionEstimatePercent: Int,
    val retentionIsEstimateLabel: String = "Self-assessed retention estimate (not measured memory)"
)

data class AcademicExam(
    val id: Long = 0,
    val subjectId: Long,
    val subjectName: String = "",
    val title: String,
    val examDateEpochMs: Long,
    val markingSystem: MarkingSystem = MarkingSystem.PERCENTAGE,
    val targetScoreDisplay: String = "80%",
    val targetNormalizedRatio: Float = 0.80f,
    val actualScoreDisplay: String? = null,
    val actualNormalizedRatio: Float? = null,
    val syllabusCoveragePercent: Int = 0,
    val isCompleted: Boolean = false,
    val notes: String = ""
)

data class AcademicAssessment(
    val id: Long = 0,
    val subjectId: Long,
    val subjectName: String = "",
    val examId: Long? = null,
    val topicId: Long? = null,
    val title: String,
    val assessmentType: AssessmentType,
    val markingSystem: MarkingSystem,
    val scoredMarks: Double,
    val maxMarks: Double,
    val gradeLabel: String = "",
    val verificationStatus: VerificationStatus,
    val xpAwarded: Int,
    val notes: String = "",
    val timestampEpochMs: Long = System.currentTimeMillis()
) {
    val normalizedRatio: Float
        get() = if (maxMarks <= 0.0) 0f else (scoredMarks / maxMarks).toFloat().coerceIn(0f, 1f)
}

data class AcademicQuest(
    val id: Long = 0,
    val title: String,
    val description: String,
    val category: AcademicQuestCategory,
    val subjectId: Long? = null,
    val subjectName: String = "",
    val topicId: Long? = null,
    val topicTitle: String = "",
    val difficulty: AcademicQuestDifficulty = AcademicQuestDifficulty.E_RANK,
    val objective: String,
    val targetValue: Int,
    val currentValue: Int = 0,
    val unit: String = "min",
    val deadlineEpochMs: Long? = null,
    val status: AcademicQuestStatus = AcademicQuestStatus.ACTIVE,
    val xpReward: Int,
    val primaryAttribute: AttributeType = AttributeType.INT,
    val primaryAttrXpReward: Int,
    val secondaryAttribute: AttributeType? = AttributeType.DIS,
    val secondaryAttrXpReward: Int = 0,
    val completionEvidence: String = "",
    val verificationStatus: VerificationStatus = VerificationStatus.SELF_REPORTED,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val completedAtEpochMs: Long? = null
) {
    val progressFraction: Float
        get() = if (targetValue <= 0) 0f else (currentValue.toFloat() / targetValue).coerceIn(0f, 1f)
}

data class AcademicProgressionRecord(
    val id: Long = 0,
    val rewardKey: String,
    val activityKind: AcademicActivityKind,
    val title: String,
    val subjectId: Long? = null,
    val verificationStatus: VerificationStatus,
    val xpAwarded: Int,
    val intXpAwarded: Int,
    val wisXpAwarded: Int,
    val disXpAwarded: Int,
    val notes: String = "",
    val timestampEpochMs: Long = System.currentTimeMillis()
)

data class AcademicStudyPlanProposal(
    val recommendedDailyMinutes: Int,
    val currentDailyMinutes: Int,
    val focusSubjectName: String,
    val focusTopicTitle: String?,
    val rationale: String,
    val sourceLabel: String = "Academic Intelligence Advisory"
)

data class SubjectProgressSummary(
    val subject: AcademicSubject,
    val moduleCount: Int,
    val chapterCount: Int,
    val completedChapters: Int,
    val topicCount: Int,
    val completedTopics: Int,
    val syllabusCompletionPercent: Int,
    val totalStudyMinutes: Int,
    val estimatedRemainingMinutes: Int,
    val latestAssessmentRatio: Float?
)

data class ExamReadinessSummary(
    val exam: AcademicExam,
    val daysRemaining: Long,
    val readinessPercent: Int,
    val statusLabel: String
)

data class AcademicDashboardSummary(
    val totalStudyMinutes: Int = 0,
    val todayStudyMinutes: Int = 0,
    val weeklyStudyMinutes: Int = 0,
    val dailyStudyGoalMinutes: Int = 60,
    val weeklyStudyGoalMinutes: Int = 360,
    val activeSubjectsCount: Int = 0,
    val overallSyllabusCompletionPercent: Int = 0,
    val pendingRevisionsCount: Int = 0,
    val overdueRevisionsCount: Int = 0,
    val upcomingExamsCount: Int = 0,
    val completedAcademicQuestsCount: Int = 0,
    val totalAcademicXp: Int = 0,
    val intPoints: Int = 10,
    val wisPoints: Int = 10,
    val disPoints: Int = 10,
    val studyConsistencyDays7d: Int = 0
)
