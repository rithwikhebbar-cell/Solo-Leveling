package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.AcademicActivityKind
import com.example.domain.model.AcademicAssessment
import com.example.domain.model.AcademicChapter
import com.example.domain.model.AcademicExam
import com.example.domain.model.AcademicModule
import com.example.domain.model.AcademicPriority
import com.example.domain.model.AcademicProgressionRecord
import com.example.domain.model.AcademicQuest
import com.example.domain.model.AcademicQuestCategory
import com.example.domain.model.AcademicQuestDifficulty
import com.example.domain.model.AcademicQuestStatus
import com.example.domain.model.AcademicSubject
import com.example.domain.model.AcademicTopic
import com.example.domain.model.AssessmentType
import com.example.domain.model.AttributeType
import com.example.domain.model.ChapterDifficulty
import com.example.domain.model.MarkingSystem
import com.example.domain.model.RevisionRecord
import com.example.domain.model.StudySession
import com.example.domain.model.SubjectColorToken
import com.example.domain.model.SubjectIconKey
import com.example.domain.model.VerificationStatus

@Entity(tableName = "academic_subjects")
data class AcademicSubjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val code: String,
    val programSystem: String,
    val colorTokenName: String,
    val iconKeyName: String,
    val priorityName: String,
    val notes: String,
    val isArchived: Boolean,
    val createdAtEpochMs: Long
)

@Entity(
    tableName = "academic_modules",
    foreignKeys = [
        ForeignKey(
            entity = AcademicSubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["subjectId"])]
)
data class AcademicModuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val title: String,
    val description: String,
    val orderIndex: Int
)

@Entity(
    tableName = "academic_chapters",
    foreignKeys = [
        ForeignKey(
            entity = AcademicModuleEntity::class,
            parentColumns = ["id"],
            childColumns = ["moduleId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AcademicSubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["moduleId"]), Index(value = ["subjectId"])]
)
data class AcademicChapterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val moduleId: Long,
    val subjectId: Long,
    val title: String,
    val difficultyName: String,
    val estimatedMinutes: Int,
    val isCompleted: Boolean,
    val completedAtEpochMs: Long?,
    val notes: String,
    val orderIndex: Int
)

@Entity(
    tableName = "academic_topics",
    foreignKeys = [
        ForeignKey(
            entity = AcademicChapterEntity::class,
            parentColumns = ["id"],
            childColumns = ["chapterId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AcademicSubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["chapterId"]),
        Index(value = ["subjectId"]),
        Index(value = ["nextRevisionDueEpochMs"])
    ]
)
data class AcademicTopicEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chapterId: Long,
    val subjectId: Long,
    val title: String,
    val isCompleted: Boolean,
    val confidenceEstimate: Int,
    val revisionCount: Int,
    val lastRevisedEpochMs: Long?,
    val nextRevisionDueEpochMs: Long?,
    val notes: String
)

@Entity(
    tableName = "study_sessions",
    foreignKeys = [
        ForeignKey(
            entity = AcademicSubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sourceId"], unique = true),
        Index(value = ["subjectId"]),
        Index(value = ["startEpochMs"])
    ]
)
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceId: String,
    val subjectId: Long,
    val subjectName: String,
    val topicId: Long?,
    val topicTitle: String,
    val title: String,
    val activityKindName: String,
    val startEpochMs: Long,
    val endEpochMs: Long,
    val durationMinutes: Int,
    val breakMinutes: Int,
    val verificationStatusName: String,
    val xpAwarded: Int,
    val notes: String
)

@Entity(
    tableName = "revision_records",
    foreignKeys = [
        ForeignKey(
            entity = AcademicTopicEntity::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AcademicSubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["topicId"]),
        Index(value = ["subjectId"]),
        Index(value = ["revisedAtEpochMs"])
    ]
)
data class RevisionRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val topicId: Long,
    val subjectId: Long,
    val topicTitle: String,
    val subjectName: String,
    val confidenceAfter: Int,
    val intervalDaysScheduled: Int,
    val nextDueEpochMs: Long,
    val durationMinutes: Int,
    val verificationStatusName: String,
    val xpAwarded: Int,
    val notes: String,
    val revisedAtEpochMs: Long
)

@Entity(
    tableName = "academic_exams",
    foreignKeys = [
        ForeignKey(
            entity = AcademicSubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["subjectId"]), Index(value = ["examDateEpochMs"])]
)
data class AcademicExamEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val subjectName: String,
    val title: String,
    val examDateEpochMs: Long,
    val markingSystemName: String,
    val targetScoreDisplay: String,
    val targetNormalizedRatio: Float,
    val actualScoreDisplay: String?,
    val actualNormalizedRatio: Float?,
    val syllabusCoveragePercent: Int,
    val isCompleted: Boolean,
    val notes: String
)

@Entity(
    tableName = "academic_assessments",
    foreignKeys = [
        ForeignKey(
            entity = AcademicSubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["subjectId"]),
        Index(value = ["examId"]),
        Index(value = ["timestampEpochMs"])
    ]
)
data class AcademicAssessmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val subjectName: String,
    val examId: Long?,
    val topicId: Long?,
    val title: String,
    val assessmentTypeName: String,
    val markingSystemName: String,
    val scoredMarks: Double,
    val maxMarks: Double,
    val gradeLabel: String,
    val verificationStatusName: String,
    val xpAwarded: Int,
    val notes: String,
    val timestampEpochMs: Long
)

@Entity(
    tableName = "academic_quests",
    indices = [
        Index(value = ["statusName"]),
        Index(value = ["categoryName"]),
        Index(value = ["subjectId"])
    ]
)
data class AcademicQuestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val categoryName: String,
    val subjectId: Long?,
    val subjectName: String,
    val topicId: Long?,
    val topicTitle: String,
    val difficultyName: String,
    val objective: String,
    val targetValue: Int,
    val currentValue: Int,
    val unit: String,
    val deadlineEpochMs: Long?,
    val statusName: String,
    val xpReward: Int,
    val primaryAttrName: String,
    val primaryAttrXpReward: Int,
    val secondaryAttrName: String?,
    val secondaryAttrXpReward: Int,
    val completionEvidence: String,
    val verificationStatusName: String,
    val createdAtEpochMs: Long,
    val completedAtEpochMs: Long?
)

@Entity(
    tableName = "academic_progression_records",
    indices = [
        Index(value = ["rewardKey"], unique = true),
        Index(value = ["timestampEpochMs"]),
        Index(value = ["subjectId"])
    ]
)
data class AcademicProgressionRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rewardKey: String,
    val activityKindName: String,
    val title: String,
    val subjectId: Long?,
    val verificationStatusName: String,
    val xpAwarded: Int,
    val intXpAwarded: Int,
    val wisXpAwarded: Int,
    val disXpAwarded: Int,
    val notes: String,
    val timestampEpochMs: Long
)

fun AcademicSubjectEntity.toDomain() = AcademicSubject(
    id = id,
    name = name,
    code = code,
    programSystem = programSystem,
    colorToken = runCatching { SubjectColorToken.valueOf(colorTokenName) }.getOrDefault(SubjectColorToken.CYAN),
    iconKey = runCatching { SubjectIconKey.valueOf(iconKeyName) }.getOrDefault(SubjectIconKey.BOOK),
    priority = runCatching { AcademicPriority.valueOf(priorityName) }.getOrDefault(AcademicPriority.MEDIUM),
    notes = notes,
    isArchived = isArchived,
    createdAtEpochMs = createdAtEpochMs
)

fun AcademicSubject.toEntity() = AcademicSubjectEntity(
    id = id,
    name = name,
    code = code,
    programSystem = programSystem,
    colorTokenName = colorToken.name,
    iconKeyName = iconKey.name,
    priorityName = priority.name,
    notes = notes,
    isArchived = isArchived,
    createdAtEpochMs = createdAtEpochMs
)

fun AcademicModuleEntity.toDomain() = AcademicModule(
    id = id,
    subjectId = subjectId,
    title = title,
    description = description,
    orderIndex = orderIndex
)

fun AcademicModule.toEntity() = AcademicModuleEntity(
    id = id,
    subjectId = subjectId,
    title = title,
    description = description,
    orderIndex = orderIndex
)

fun AcademicChapterEntity.toDomain() = AcademicChapter(
    id = id,
    moduleId = moduleId,
    subjectId = subjectId,
    title = title,
    difficulty = runCatching { ChapterDifficulty.valueOf(difficultyName) }.getOrDefault(ChapterDifficulty.INTERMEDIATE),
    estimatedMinutes = estimatedMinutes,
    isCompleted = isCompleted,
    completedAtEpochMs = completedAtEpochMs,
    notes = notes,
    orderIndex = orderIndex
)

fun AcademicChapter.toEntity() = AcademicChapterEntity(
    id = id,
    moduleId = moduleId,
    subjectId = subjectId,
    title = title,
    difficultyName = difficulty.name,
    estimatedMinutes = estimatedMinutes,
    isCompleted = isCompleted,
    completedAtEpochMs = completedAtEpochMs,
    notes = notes,
    orderIndex = orderIndex
)

fun AcademicTopicEntity.toDomain() = AcademicTopic(
    id = id,
    chapterId = chapterId,
    subjectId = subjectId,
    title = title,
    isCompleted = isCompleted,
    confidenceEstimate = confidenceEstimate,
    revisionCount = revisionCount,
    lastRevisedEpochMs = lastRevisedEpochMs,
    nextRevisionDueEpochMs = nextRevisionDueEpochMs,
    notes = notes
)

fun AcademicTopic.toEntity() = AcademicTopicEntity(
    id = id,
    chapterId = chapterId,
    subjectId = subjectId,
    title = title,
    isCompleted = isCompleted,
    confidenceEstimate = confidenceEstimate,
    revisionCount = revisionCount,
    lastRevisedEpochMs = lastRevisedEpochMs,
    nextRevisionDueEpochMs = nextRevisionDueEpochMs,
    notes = notes
)

fun StudySessionEntity.toDomain() = StudySession(
    id = id,
    sourceId = sourceId,
    subjectId = subjectId,
    subjectName = subjectName,
    topicId = topicId,
    topicTitle = topicTitle,
    title = title,
    activityKind = runCatching { AcademicActivityKind.valueOf(activityKindName) }.getOrDefault(AcademicActivityKind.FOCUS_SESSION),
    startEpochMs = startEpochMs,
    endEpochMs = endEpochMs,
    durationMinutes = durationMinutes,
    breakMinutes = breakMinutes,
    verificationStatus = runCatching { VerificationStatus.valueOf(verificationStatusName) }.getOrDefault(VerificationStatus.SELF_REPORTED),
    xpAwarded = xpAwarded,
    notes = notes
)

fun StudySession.toEntity() = StudySessionEntity(
    id = id,
    sourceId = sourceId,
    subjectId = subjectId,
    subjectName = subjectName,
    topicId = topicId,
    topicTitle = topicTitle,
    title = title,
    activityKindName = activityKind.name,
    startEpochMs = startEpochMs,
    endEpochMs = endEpochMs,
    durationMinutes = durationMinutes,
    breakMinutes = breakMinutes,
    verificationStatusName = verificationStatus.name,
    xpAwarded = xpAwarded,
    notes = notes
)

fun RevisionRecordEntity.toDomain() = RevisionRecord(
    id = id,
    topicId = topicId,
    subjectId = subjectId,
    topicTitle = topicTitle,
    subjectName = subjectName,
    confidenceAfter = confidenceAfter,
    intervalDaysScheduled = intervalDaysScheduled,
    nextDueEpochMs = nextDueEpochMs,
    durationMinutes = durationMinutes,
    verificationStatus = runCatching { VerificationStatus.valueOf(verificationStatusName) }.getOrDefault(VerificationStatus.SELF_REPORTED),
    xpAwarded = xpAwarded,
    notes = notes,
    revisedAtEpochMs = revisedAtEpochMs
)

fun RevisionRecord.toEntity() = RevisionRecordEntity(
    id = id,
    topicId = topicId,
    subjectId = subjectId,
    topicTitle = topicTitle,
    subjectName = subjectName,
    confidenceAfter = confidenceAfter,
    intervalDaysScheduled = intervalDaysScheduled,
    nextDueEpochMs = nextDueEpochMs,
    durationMinutes = durationMinutes,
    verificationStatusName = verificationStatus.name,
    xpAwarded = xpAwarded,
    notes = notes,
    revisedAtEpochMs = revisedAtEpochMs
)

fun AcademicExamEntity.toDomain() = AcademicExam(
    id = id,
    subjectId = subjectId,
    subjectName = subjectName,
    title = title,
    examDateEpochMs = examDateEpochMs,
    markingSystem = runCatching { MarkingSystem.valueOf(markingSystemName) }.getOrDefault(MarkingSystem.PERCENTAGE),
    targetScoreDisplay = targetScoreDisplay,
    targetNormalizedRatio = targetNormalizedRatio,
    actualScoreDisplay = actualScoreDisplay,
    actualNormalizedRatio = actualNormalizedRatio,
    syllabusCoveragePercent = syllabusCoveragePercent,
    isCompleted = isCompleted,
    notes = notes
)

fun AcademicExam.toEntity() = AcademicExamEntity(
    id = id,
    subjectId = subjectId,
    subjectName = subjectName,
    title = title,
    examDateEpochMs = examDateEpochMs,
    markingSystemName = markingSystem.name,
    targetScoreDisplay = targetScoreDisplay,
    targetNormalizedRatio = targetNormalizedRatio,
    actualScoreDisplay = actualScoreDisplay,
    actualNormalizedRatio = actualNormalizedRatio,
    syllabusCoveragePercent = syllabusCoveragePercent,
    isCompleted = isCompleted,
    notes = notes
)

fun AcademicAssessmentEntity.toDomain() = AcademicAssessment(
    id = id,
    subjectId = subjectId,
    subjectName = subjectName,
    examId = examId,
    topicId = topicId,
    title = title,
    assessmentType = runCatching { AssessmentType.valueOf(assessmentTypeName) }.getOrDefault(AssessmentType.PRACTICE_QUIZ),
    markingSystem = runCatching { MarkingSystem.valueOf(markingSystemName) }.getOrDefault(MarkingSystem.PERCENTAGE),
    scoredMarks = scoredMarks,
    maxMarks = maxMarks,
    gradeLabel = gradeLabel,
    verificationStatus = runCatching { VerificationStatus.valueOf(verificationStatusName) }.getOrDefault(VerificationStatus.SELF_REPORTED),
    xpAwarded = xpAwarded,
    notes = notes,
    timestampEpochMs = timestampEpochMs
)

fun AcademicAssessment.toEntity() = AcademicAssessmentEntity(
    id = id,
    subjectId = subjectId,
    subjectName = subjectName,
    examId = examId,
    topicId = topicId,
    title = title,
    assessmentTypeName = assessmentType.name,
    markingSystemName = markingSystem.name,
    scoredMarks = scoredMarks,
    maxMarks = maxMarks,
    gradeLabel = gradeLabel,
    verificationStatusName = verificationStatus.name,
    xpAwarded = xpAwarded,
    notes = notes,
    timestampEpochMs = timestampEpochMs
)

fun AcademicQuestEntity.toDomain() = AcademicQuest(
    id = id,
    title = title,
    description = description,
    category = runCatching { AcademicQuestCategory.valueOf(categoryName) }.getOrDefault(AcademicQuestCategory.DAILY_STUDY),
    subjectId = subjectId,
    subjectName = subjectName,
    topicId = topicId,
    topicTitle = topicTitle,
    difficulty = runCatching { AcademicQuestDifficulty.valueOf(difficultyName) }.getOrDefault(AcademicQuestDifficulty.E_RANK),
    objective = objective,
    targetValue = targetValue,
    currentValue = currentValue,
    unit = unit,
    deadlineEpochMs = deadlineEpochMs,
    status = runCatching { AcademicQuestStatus.valueOf(statusName) }.getOrDefault(AcademicQuestStatus.ACTIVE),
    xpReward = xpReward,
    primaryAttribute = runCatching { AttributeType.valueOf(primaryAttrName) }.getOrDefault(AttributeType.INT),
    primaryAttrXpReward = primaryAttrXpReward,
    secondaryAttribute = secondaryAttrName?.let { runCatching { AttributeType.valueOf(it) }.getOrNull() },
    secondaryAttrXpReward = secondaryAttrXpReward,
    completionEvidence = completionEvidence,
    verificationStatus = runCatching { VerificationStatus.valueOf(verificationStatusName) }.getOrDefault(VerificationStatus.SELF_REPORTED),
    createdAtEpochMs = createdAtEpochMs,
    completedAtEpochMs = completedAtEpochMs
)

fun AcademicQuest.toEntity() = AcademicQuestEntity(
    id = id,
    title = title,
    description = description,
    categoryName = category.name,
    subjectId = subjectId,
    subjectName = subjectName,
    topicId = topicId,
    topicTitle = topicTitle,
    difficultyName = difficulty.name,
    objective = objective,
    targetValue = targetValue,
    currentValue = currentValue,
    unit = unit,
    deadlineEpochMs = deadlineEpochMs,
    statusName = status.name,
    xpReward = xpReward,
    primaryAttrName = primaryAttribute.name,
    primaryAttrXpReward = primaryAttrXpReward,
    secondaryAttrName = secondaryAttribute?.name,
    secondaryAttrXpReward = secondaryAttrXpReward,
    completionEvidence = completionEvidence,
    verificationStatusName = verificationStatus.name,
    createdAtEpochMs = createdAtEpochMs,
    completedAtEpochMs = completedAtEpochMs
)

fun AcademicProgressionRecordEntity.toDomain() = AcademicProgressionRecord(
    id = id,
    rewardKey = rewardKey,
    activityKind = runCatching { AcademicActivityKind.valueOf(activityKindName) }.getOrDefault(AcademicActivityKind.STUDY_SESSION),
    title = title,
    subjectId = subjectId,
    verificationStatus = runCatching { VerificationStatus.valueOf(verificationStatusName) }.getOrDefault(VerificationStatus.SELF_REPORTED),
    xpAwarded = xpAwarded,
    intXpAwarded = intXpAwarded,
    wisXpAwarded = wisXpAwarded,
    disXpAwarded = disXpAwarded,
    notes = notes,
    timestampEpochMs = timestampEpochMs
)

fun AcademicProgressionRecord.toEntity() = AcademicProgressionRecordEntity(
    id = id,
    rewardKey = rewardKey,
    activityKindName = activityKind.name,
    title = title,
    subjectId = subjectId,
    verificationStatusName = verificationStatus.name,
    xpAwarded = xpAwarded,
    intXpAwarded = intXpAwarded,
    wisXpAwarded = wisXpAwarded,
    disXpAwarded = disXpAwarded,
    notes = notes,
    timestampEpochMs = timestampEpochMs
)
