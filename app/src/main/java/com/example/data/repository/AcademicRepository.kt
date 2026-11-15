package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.entity.toDomain
import com.example.data.local.entity.toEntity
import com.example.data.preferences.UserPreferencesRepository
import com.example.domain.engine.ProgressionEngine
import com.example.domain.engine.RevisionEngine
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
import com.example.domain.model.ActivityLog
import com.example.domain.model.AssessmentType
import com.example.domain.model.AttributeType
import com.example.domain.model.ChapterDifficulty
import com.example.domain.model.MarkingSystem
import com.example.domain.model.NotificationCategory
import com.example.domain.model.PlayerProfile
import com.example.domain.model.QuestCategory
import com.example.domain.model.RevisionRecord
import com.example.domain.model.StudySession
import com.example.domain.model.SubjectColorToken
import com.example.domain.model.SubjectIconKey
import com.example.domain.model.SystemNotification
import com.example.domain.model.VerificationStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class AcademicRepository(
    private val database: SoloLevelingDatabase,
    private val preferencesRepository: UserPreferencesRepository
) {
    private val playerDao = database.playerDao()
    private val activityLogDao = database.activityLogDao()
    private val notificationDao = database.systemNotificationDao()
    private val subjectDao = database.academicSubjectDao()
    private val studySessionDao = database.studySessionDao()
    private val revisionRecordDao = database.revisionRecordDao()
    private val assessmentDao = database.academicAssessmentDao()
    private val academicQuestDao = database.academicQuestDao()
    private val academicProgressionDao = database.academicProgressionDao()

    val subjects: Flow<List<AcademicSubject>> =
        subjectDao.observeAllSubjects().map { list -> list.map { it.toDomain() } }

    val modules: Flow<List<AcademicModule>> =
        subjectDao.observeAllModules().map { list -> list.map { it.toDomain() } }

    val chapters: Flow<List<AcademicChapter>> =
        subjectDao.observeAllChapters().map { list -> list.map { it.toDomain() } }

    val topics: Flow<List<AcademicTopic>> =
        subjectDao.observeAllTopics().map { list -> list.map { it.toDomain() } }

    val studySessions: Flow<List<StudySession>> =
        studySessionDao.observeAllSessions().map { list -> list.map { it.toDomain() } }

    val revisionRecords: Flow<List<RevisionRecord>> =
        revisionRecordDao.observeAllRevisions().map { list -> list.map { it.toDomain() } }

    val exams: Flow<List<AcademicExam>> =
        assessmentDao.observeAllExams().map { list -> list.map { it.toDomain() } }

    val assessments: Flow<List<AcademicAssessment>> =
        assessmentDao.observeAllAssessments().map { list -> list.map { it.toDomain() } }

    val academicQuests: Flow<List<AcademicQuest>> =
        academicQuestDao.observeAllAcademicQuests().map { list -> list.map { it.toDomain() } }

    val progressionRecords: Flow<List<AcademicProgressionRecord>> =
        academicProgressionDao.observeAllProgressionRecords().map { list -> list.map { it.toDomain() } }

    // Creating subjects, modules, chapters, topics NEVER awards XP.
    suspend fun createSubject(
        name: String,
        code: String,
        programSystem: String,
        colorToken: SubjectColorToken,
        iconKey: SubjectIconKey,
        priority: AcademicPriority,
        notes: String
    ): Result<AcademicSubject> {
        val cleanName = name.trim()
        if (cleanName.isBlank()) {
            return Result.failure(IllegalArgumentException("Subject name cannot be blank."))
        }
        val subject = AcademicSubject(
            name = cleanName,
            code = code.trim(),
            programSystem = programSystem.trim().ifBlank { "Flexible Programme" },
            colorToken = colorToken,
            iconKey = iconKey,
            priority = priority,
            notes = notes.trim()
        )
        var savedId = 0L
        database.withTransaction {
            savedId = subjectDao.insertSubject(subject.toEntity())
            subjectDao.insertModule(
                AcademicModule(
                    subjectId = savedId,
                    title = "Module 1 — Core Syllabus",
                    description = "Primary syllabus units for $cleanName",
                    orderIndex = 0
                ).toEntity()
            )
        }
        return Result.success(subject.copy(id = savedId))
    }

    suspend fun updateSubject(subject: AcademicSubject): Result<Unit> {
        if (subject.name.isBlank()) {
            return Result.failure(IllegalArgumentException("Subject name cannot be blank."))
        }
        subjectDao.updateSubject(subject.copy(name = subject.name.trim(), code = subject.code.trim()).toEntity())
        return Result.success(Unit)
    }

    suspend fun setSubjectArchived(subjectId: Long, archived: Boolean) {
        val existing = subjectDao.getSubjectById(subjectId)?.toDomain() ?: return
        subjectDao.updateSubject(existing.copy(isArchived = archived).toEntity())
    }

    suspend fun addModule(subjectId: Long, title: String, description: String): Result<Long> {
        val clean = title.trim()
        if (clean.isBlank()) return Result.failure(IllegalArgumentException("Module title cannot be blank."))
        val existingCount = subjectDao.getModulesForSubject(subjectId).size
        val id = subjectDao.insertModule(
            AcademicModule(
                subjectId = subjectId,
                title = clean,
                description = description.trim(),
                orderIndex = existingCount
            ).toEntity()
        )
        return Result.success(id)
    }

    suspend fun addChapter(
        moduleId: Long,
        subjectId: Long,
        title: String,
        difficulty: ChapterDifficulty,
        estimatedMinutes: Int,
        notes: String
    ): Result<Long> {
        val clean = title.trim()
        if (clean.isBlank()) return Result.failure(IllegalArgumentException("Chapter title cannot be blank."))
        val order = subjectDao.getChaptersForModule(moduleId).size
        val id = subjectDao.insertChapter(
            AcademicChapter(
                moduleId = moduleId,
                subjectId = subjectId,
                title = clean,
                difficulty = difficulty,
                estimatedMinutes = estimatedMinutes.coerceIn(10, 600),
                notes = notes.trim(),
                orderIndex = order
            ).toEntity()
        )
        return Result.success(id)
    }

    suspend fun addTopic(
        chapterId: Long,
        subjectId: Long,
        title: String,
        confidenceEstimate: Int,
        notes: String
    ): Result<Long> {
        val clean = title.trim()
        if (clean.isBlank()) return Result.failure(IllegalArgumentException("Topic title cannot be blank."))
        val now = System.currentTimeMillis()
        val id = subjectDao.insertTopic(
            AcademicTopic(
                chapterId = chapterId,
                subjectId = subjectId,
                title = clean,
                confidenceEstimate = confidenceEstimate.coerceIn(1, 5),
                nextRevisionDueEpochMs = now,
                notes = notes.trim()
            ).toEntity()
        )
        return Result.success(id)
    }

    suspend fun toggleTopicCompleted(topicId: Long, completed: Boolean) {
        val topic = subjectDao.getTopicById(topicId)?.toDomain() ?: return
        val now = System.currentTimeMillis()
        val updatedDue = if (completed && topic.nextRevisionDueEpochMs == null) now + 24 * 60 * 60_000L else topic.nextRevisionDueEpochMs
        subjectDao.updateTopic(topic.copy(isCompleted = completed, nextRevisionDueEpochMs = updatedDue).toEntity())
    }

    /**
     * Toggles chapter completion. Progression XP is strictly guarded by unique rewardKey
     * ("chapter_${chapterId}") so unchecking/rechecking cannot repeatedly farm XP.
     */
    suspend fun setChapterCompleted(chapterId: Long, completed: Boolean): Result<Int> {
        var awardedXp = 0
        database.withTransaction {
            val chapter = subjectDao.getChapterById(chapterId)?.toDomain() ?: return@withTransaction
            val now = System.currentTimeMillis()
            subjectDao.updateChapter(
                chapter.copy(
                    isCompleted = completed,
                    completedAtEpochMs = if (completed) (chapter.completedAtEpochMs ?: now) else null
                ).toEntity()
            )
            if (completed) {
                val rewardKey = "chapter_$chapterId"
                if (!academicProgressionDao.existsByRewardKey(rewardKey)) {
                    val subject = subjectDao.getSubjectById(chapter.subjectId)?.toDomain()
                    val dayStart = startOfDayEpochMs(now)
                    val xpToday = academicProgressionDao.getAcademicXpEarnedSince(dayStart)
                    val selfCount = academicProgressionDao.getSelfReportedAcademicCountSince(dayStart)
                    val reward = ProgressionEngine.calculateAcademicReward(
                        kind = AcademicActivityKind.CHAPTER_COMPLETION,
                        durationMinutes = chapter.estimatedMinutes.coerceAtMost(120),
                        verificationStatus = VerificationStatus.SELF_REPORTED,
                        academicXpAlreadyEarnedToday = xpToday,
                        selfReportedAcademicCountToday = selfCount,
                        difficultyWeight = chapter.difficulty.weight
                    )
                    awardedXp = reward.xpAwarded
                    recordAcademicProgressionLocked(
                        rewardKey = rewardKey,
                        activityKind = AcademicActivityKind.CHAPTER_COMPLETION,
                        title = "Chapter Clear: ${chapter.title}",
                        subjectId = chapter.subjectId,
                        subjectName = subject?.name ?: "Subject",
                        durationMinutes = chapter.estimatedMinutes,
                        verificationStatus = VerificationStatus.SELF_REPORTED,
                        xpAwarded = reward.xpAwarded,
                        primaryAttr = reward.primaryAttribute,
                        primaryAttrXp = reward.primaryAttrXpAwarded,
                        secondaryAttr = reward.secondaryAttribute,
                        secondaryAttrXp = reward.secondaryAttrXpAwarded,
                        notes = reward.validationNote,
                        nowEpochMs = now
                    )
                }
            }
        }
        return Result.success(awardedXp)
    }

    /**
     * Records a study or focus session. System-timed focus blocks receive VERIFIED_SYSTEM_TIMER
     * (verifies elapsed app time, NOT verified learning). Manual entries receive SELF_REPORTED.
     */
    suspend fun recordStudySession(
        subjectId: Long,
        topicId: Long?,
        title: String,
        startEpochMs: Long,
        endEpochMs: Long,
        durationMinutes: Int,
        breakMinutes: Int,
        isSystemTimer: Boolean,
        notes: String,
        sourceId: String = "study_${UUID.randomUUID()}"
    ): Result<StudySession> {
        val now = System.currentTimeMillis()
        val validation = ProgressionEngine.validateStudySession(
            title = title,
            subjectId = subjectId,
            startEpochMs = startEpochMs,
            endEpochMs = endEpochMs,
            durationMinutes = durationMinutes,
            isSystemTimer = isSystemTimer,
            nowEpochMs = now
        )
        if (!validation.isValid) {
            return Result.failure(IllegalArgumentException(validation.rejectionReason ?: "Invalid study session."))
        }

        if (studySessionDao.existsBySourceId(sourceId) || academicProgressionDao.existsByRewardKey("study_$sourceId")) {
            return Result.failure(IllegalStateException("Duplicate study session detected; reward skipped."))
        }

        val verification = if (isSystemTimer) VerificationStatus.VERIFIED_SYSTEM_TIMER else VerificationStatus.SELF_REPORTED
        val kind = if (isSystemTimer) AcademicActivityKind.FOCUS_SESSION else AcademicActivityKind.STUDY_SESSION
        var savedSession: StudySession? = null

        database.withTransaction {
            val subject = subjectDao.getSubjectById(subjectId)?.toDomain()
                ?: throw IllegalArgumentException("Subject not found.")
            val topic = topicId?.let { subjectDao.getTopicById(it)?.toDomain() }

            val dayStart = startOfDayEpochMs(now)
            val xpToday = academicProgressionDao.getAcademicXpEarnedSince(dayStart)
            val selfCount = academicProgressionDao.getSelfReportedAcademicCountSince(dayStart)

            val reward = ProgressionEngine.calculateAcademicReward(
                kind = kind,
                durationMinutes = durationMinutes,
                verificationStatus = verification,
                academicXpAlreadyEarnedToday = xpToday,
                selfReportedAcademicCountToday = selfCount
            )

            val session = StudySession(
                sourceId = sourceId,
                subjectId = subjectId,
                subjectName = subject.name,
                topicId = topic?.id,
                topicTitle = topic?.title.orEmpty(),
                title = title.trim(),
                activityKind = kind,
                startEpochMs = startEpochMs,
                endEpochMs = endEpochMs,
                durationMinutes = durationMinutes,
                breakMinutes = breakMinutes.coerceAtLeast(0),
                verificationStatus = verification,
                xpAwarded = reward.xpAwarded,
                notes = notes.trim()
            )
            val insertedId = studySessionDao.insertSession(session.toEntity())
            savedSession = session.copy(id = insertedId)

            recordAcademicProgressionLocked(
                rewardKey = "study_$sourceId",
                activityKind = kind,
                title = "${subject.name}: ${session.title}",
                subjectId = subjectId,
                subjectName = subject.name,
                durationMinutes = durationMinutes,
                verificationStatus = verification,
                xpAwarded = reward.xpAwarded,
                primaryAttr = reward.primaryAttribute,
                primaryAttrXp = reward.primaryAttrXpAwarded,
                secondaryAttr = reward.secondaryAttribute,
                secondaryAttrXp = reward.secondaryAttrXpAwarded,
                notes = listOf(notes.trim(), reward.validationNote).filter { it.isNotBlank() }.joinToString(" • "),
                nowEpochMs = now
            )
        }

        return Result.success(savedSession!!)
    }

    suspend fun completeTopicRevision(
        topicId: Long,
        confidenceAfter: Int,
        durationMinutes: Int,
        notes: String
    ): Result<RevisionRecord> {
        if (durationMinutes !in 1..240) {
            return Result.failure(IllegalArgumentException("Revision duration must be between 1 and 240 minutes."))
        }
        val safeConfidence = confidenceAfter.coerceIn(1, 5)
        val now = System.currentTimeMillis()
        var savedRecord: RevisionRecord? = null

        database.withTransaction {
            val topic = subjectDao.getTopicById(topicId)?.toDomain()
                ?: throw IllegalArgumentException("Topic not found.")
            val subject = subjectDao.getSubjectById(topic.subjectId)?.toDomain()
                ?: throw IllegalArgumentException("Subject not found.")
            val chapter = subjectDao.getChapterById(topic.chapterId)?.toDomain()
            val latestAssessment = assessmentDao.getAssessmentsForSubject(subject.id).firstOrNull()?.toDomain()

            val schedule = RevisionEngine.scheduleNextRevision(
                nowEpochMs = now,
                previousRevisionCount = topic.revisionCount,
                selfReportedConfidence = safeConfidence,
                chapterDifficulty = chapter?.difficulty ?: ChapterDifficulty.INTERMEDIATE,
                latestAssessmentRatio = latestAssessment?.normalizedRatio
            )

            subjectDao.updateTopic(
                topic.copy(
                    confidenceEstimate = safeConfidence,
                    revisionCount = topic.revisionCount + 1,
                    lastRevisedEpochMs = now,
                    nextRevisionDueEpochMs = schedule.nextDueEpochMs
                ).toEntity()
            )

            val dayStart = startOfDayEpochMs(now)
            val xpToday = academicProgressionDao.getAcademicXpEarnedSince(dayStart)
            val selfCount = academicProgressionDao.getSelfReportedAcademicCountSince(dayStart)

            val reward = ProgressionEngine.calculateAcademicReward(
                kind = AcademicActivityKind.REVISION,
                durationMinutes = durationMinutes,
                verificationStatus = VerificationStatus.SELF_REPORTED,
                academicXpAlreadyEarnedToday = xpToday,
                selfReportedAcademicCountToday = selfCount
            )

            val record = RevisionRecord(
                topicId = topic.id,
                subjectId = subject.id,
                topicTitle = topic.title,
                subjectName = subject.name,
                confidenceAfter = safeConfidence,
                intervalDaysScheduled = schedule.nextIntervalDays,
                nextDueEpochMs = schedule.nextDueEpochMs,
                durationMinutes = durationMinutes,
                verificationStatus = VerificationStatus.SELF_REPORTED,
                xpAwarded = reward.xpAwarded,
                notes = notes.trim(),
                revisedAtEpochMs = now
            )
            val insertedId = revisionRecordDao.insertRevision(record.toEntity())
            savedRecord = record.copy(id = insertedId)

            recordAcademicProgressionLocked(
                rewardKey = "revision_${insertedId}_$now",
                activityKind = AcademicActivityKind.REVISION,
                title = "Revision: ${topic.title}",
                subjectId = subject.id,
                subjectName = subject.name,
                durationMinutes = durationMinutes,
                verificationStatus = VerificationStatus.SELF_REPORTED,
                xpAwarded = reward.xpAwarded,
                primaryAttr = reward.primaryAttribute,
                primaryAttrXp = reward.primaryAttrXpAwarded,
                secondaryAttr = reward.secondaryAttribute,
                secondaryAttrXp = reward.secondaryAttrXpAwarded,
                notes = "Next interval: ${schedule.nextIntervalDays}d • ${reward.validationNote}",
                nowEpochMs = now
            )
        }

        return Result.success(savedRecord!!)
    }

    suspend fun createExam(
        subjectId: Long,
        title: String,
        examDateEpochMs: Long,
        markingSystem: MarkingSystem,
        targetScoreDisplay: String,
        targetNormalizedRatio: Float,
        syllabusCoveragePercent: Int,
        notes: String
    ): Result<AcademicExam> {
        val clean = title.trim()
        if (clean.isBlank()) return Result.failure(IllegalArgumentException("Exam title cannot be blank."))
        val subject = subjectDao.getSubjectById(subjectId)?.toDomain()
            ?: return Result.failure(IllegalArgumentException("Subject not found."))

        val exam = AcademicExam(
            subjectId = subjectId,
            subjectName = subject.name,
            title = clean,
            examDateEpochMs = examDateEpochMs,
            markingSystem = markingSystem,
            targetScoreDisplay = targetScoreDisplay.trim().ifBlank { "80%" },
            targetNormalizedRatio = targetNormalizedRatio.coerceIn(0.1f, 1.0f),
            syllabusCoveragePercent = syllabusCoveragePercent.coerceIn(0, 100),
            notes = notes.trim()
        )
        val id = assessmentDao.insertExam(exam.toEntity())
        return Result.success(exam.copy(id = id))
    }

    suspend fun generateExamPrepQuests(examId: Long): Result<Int> {
        val exam = assessmentDao.getExamById(examId)?.toDomain()
            ?: return Result.failure(IllegalArgumentException("Exam not found."))
        val now = System.currentTimeMillis()
        val quests = listOf(
            AcademicQuest(
                title = "${exam.subjectName}: Mock Paper Drill",
                description = "Complete timed practice problems for ${exam.title}.",
                category = AcademicQuestCategory.EXAM_PREP,
                subjectId = exam.subjectId,
                subjectName = exam.subjectName,
                difficulty = AcademicQuestDifficulty.B_RANK,
                objective = "Complete 1 timed mock assessment",
                targetValue = 60,
                unit = "min",
                deadlineEpochMs = exam.examDateEpochMs,
                xpReward = 75,
                primaryAttribute = AttributeType.INT,
                primaryAttrXpReward = 65,
                secondaryAttribute = AttributeType.DIS,
                secondaryAttrXpReward = 35,
                createdAtEpochMs = now
            ),
            AcademicQuest(
                title = "${exam.subjectName}: High-Priority Revision Sweep",
                description = "Review overdue and low-confidence topics ahead of ${exam.title}.",
                category = AcademicQuestCategory.REVISION,
                subjectId = exam.subjectId,
                subjectName = exam.subjectName,
                difficulty = AcademicQuestDifficulty.C_RANK,
                objective = "Revise 3 core chapters",
                targetValue = 3,
                unit = "topics",
                deadlineEpochMs = exam.examDateEpochMs,
                xpReward = 60,
                primaryAttribute = AttributeType.WIS,
                primaryAttrXpReward = 55,
                secondaryAttribute = AttributeType.INT,
                secondaryAttrXpReward = 30,
                createdAtEpochMs = now - 500
            )
        )
        academicQuestDao.insertQuests(quests.map { it.toEntity() })
        return Result.success(quests.size)
    }

    suspend fun recordAssessmentResult(
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
    ): Result<AcademicAssessment> {
        val validation = ProgressionEngine.validateAssessmentResult(
            title = title,
            subjectId = subjectId,
            scoredMarks = scoredMarks,
            maxMarks = maxMarks
        )
        if (!validation.isValid) {
            return Result.failure(IllegalArgumentException(validation.rejectionReason ?: "Invalid assessment."))
        }

        val verification = if (hasVerifiedEvidence && assessmentType.supportsVerifiedEvidence) {
            VerificationStatus.VERIFIED_ASSESSMENT
        } else {
            VerificationStatus.SELF_REPORTED
        }
        val kind = if (assessmentType == AssessmentType.GRADED_EXAM) {
            AcademicActivityKind.EXAM_ASSESSMENT
        } else {
            AcademicActivityKind.PRACTICE_ASSESSMENT
        }

        val now = System.currentTimeMillis()
        var saved: AcademicAssessment? = null

        database.withTransaction {
            val subject = subjectDao.getSubjectById(subjectId)?.toDomain()
                ?: throw IllegalArgumentException("Subject not found.")

            val ratio = (scoredMarks / maxMarks).toFloat().coerceIn(0f, 1f)
            val dayStart = startOfDayEpochMs(now)
            val xpToday = academicProgressionDao.getAcademicXpEarnedSince(dayStart)
            val selfCount = academicProgressionDao.getSelfReportedAcademicCountSince(dayStart)

            // Anti-farming guard: linked graded exam can only reward exam assessment XP once.
            val examRewardKey = examId?.let { if (assessmentType == AssessmentType.GRADED_EXAM) "exam_result_$it" else null }
            val alreadyRewardedExam = examRewardKey != null && academicProgressionDao.existsByRewardKey(examRewardKey)

            val reward = if (alreadyRewardedExam) {
                ProgressionEngine.calculateAcademicReward(kind, 20, verification, ProgressionEngine.DAILY_ACADEMIC_XP_CAP, selfCount, ratio)
            } else {
                ProgressionEngine.calculateAcademicReward(
                    kind = kind,
                    durationMinutes = 30,
                    verificationStatus = verification,
                    academicXpAlreadyEarnedToday = xpToday,
                    selfReportedAcademicCountToday = selfCount,
                    assessmentRatio = ratio
                )
            }

            val assessment = AcademicAssessment(
                subjectId = subjectId,
                subjectName = subject.name,
                examId = examId,
                topicId = topicId,
                title = title.trim(),
                assessmentType = assessmentType,
                markingSystem = markingSystem,
                scoredMarks = scoredMarks,
                maxMarks = maxMarks,
                gradeLabel = gradeLabel.trim(),
                verificationStatus = verification,
                xpAwarded = reward.xpAwarded,
                notes = notes.trim(),
                timestampEpochMs = now
            )
            val insertedId = assessmentDao.insertAssessment(assessment.toEntity())
            saved = assessment.copy(id = insertedId)

            if (examId != null && assessmentType == AssessmentType.GRADED_EXAM) {
                assessmentDao.getExamById(examId)?.toDomain()?.let { exam ->
                    val scoreText = gradeLabel.trim().ifBlank {
                        if (markingSystem == MarkingSystem.PERCENTAGE) "${(ratio * 100).toInt()}%" else "$scoredMarks / $maxMarks"
                    }
                    assessmentDao.updateExam(
                        exam.copy(
                            actualScoreDisplay = scoreText,
                            actualNormalizedRatio = ratio,
                            isCompleted = true
                        ).toEntity()
                    )
                }
            }

            val rewardKey = examRewardKey ?: "assessment_$insertedId"
            if (!academicProgressionDao.existsByRewardKey(rewardKey)) {
                recordAcademicProgressionLocked(
                    rewardKey = rewardKey,
                    activityKind = kind,
                    title = "${subject.name}: ${assessment.title}",
                    subjectId = subjectId,
                    subjectName = subject.name,
                    durationMinutes = 30,
                    verificationStatus = verification,
                    xpAwarded = reward.xpAwarded,
                    primaryAttr = reward.primaryAttribute,
                    primaryAttrXp = reward.primaryAttrXpAwarded,
                    secondaryAttr = reward.secondaryAttribute,
                    secondaryAttrXp = reward.secondaryAttrXpAwarded,
                    notes = reward.validationNote,
                    nowEpochMs = now
                )
            }
        }

        return Result.success(saved!!)
    }

    // Creating quests NEVER awards XP.
    suspend fun createAcademicQuest(
        title: String,
        description: String,
        category: AcademicQuestCategory,
        subjectId: Long?,
        topicId: Long?,
        difficulty: AcademicQuestDifficulty,
        objective: String,
        targetValue: Int,
        unit: String,
        deadlineEpochMs: Long?
    ): Result<Long> {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) return Result.failure(IllegalArgumentException("Quest title cannot be blank."))
        val subject = subjectId?.let { subjectDao.getSubjectById(it)?.toDomain() }
        val topic = topicId?.let { subjectDao.getTopicById(it)?.toDomain() }

        val (primary, secondary) = when (category) {
            AcademicQuestCategory.REVISION -> AttributeType.WIS to AttributeType.INT
            AcademicQuestCategory.DAILY_STUDY,
            AcademicQuestCategory.WEEKLY_STUDY,
            AcademicQuestCategory.LONG_TERM_MISSION -> AttributeType.DIS to AttributeType.INT
            else -> AttributeType.INT to AttributeType.DIS
        }

        val quest = AcademicQuest(
            title = cleanTitle,
            description = description.trim().ifBlank { "Structured academic objective." },
            category = category,
            subjectId = subject?.id,
            subjectName = subject?.name.orEmpty(),
            topicId = topic?.id,
            topicTitle = topic?.title.orEmpty(),
            difficulty = difficulty,
            objective = objective.trim().ifBlank { cleanTitle },
            targetValue = targetValue.coerceAtLeast(1),
            unit = unit.trim().ifBlank { "min" },
            deadlineEpochMs = deadlineEpochMs,
            xpReward = difficulty.xpBonus,
            primaryAttribute = primary,
            primaryAttrXpReward = (difficulty.xpBonus * 0.9f).toInt(),
            secondaryAttribute = secondary,
            secondaryAttrXpReward = (difficulty.xpBonus * 0.45f).toInt()
        )
        val id = academicQuestDao.insertQuest(quest.toEntity())
        return Result.success(id)
    }

    suspend fun generateQuestsFromStudyPlan(): Int {
        val now = System.currentTimeMillis()
        val created = mutableListOf<AcademicQuest>()
        database.withTransaction {
            val activeSubjects = subjectDao.getActiveSubjectsOnce().map { it.toDomain() }
            val allTopics = subjectDao.getAllTopicsOnce().map { it.toDomain() }
            val topSubject = activeSubjects.maxByOrNull { it.priority.weight }
            if (topSubject != null) {
                created.add(
                    AcademicQuest(
                        title = "${topSubject.name}: Daily Focus Trial",
                        description = "Complete a 50-minute focus block in priority subject ${topSubject.name}.",
                        category = AcademicQuestCategory.DAILY_STUDY,
                        subjectId = topSubject.id,
                        subjectName = topSubject.name,
                        difficulty = AcademicQuestDifficulty.E_RANK,
                        objective = "Study ${topSubject.name} for 50 min",
                        targetValue = 50,
                        unit = "min",
                        deadlineEpochMs = now + 24 * 60 * 60_000L,
                        xpReward = AcademicQuestDifficulty.E_RANK.xpBonus,
                        primaryAttribute = AttributeType.INT,
                        primaryAttrXpReward = 26,
                        secondaryAttribute = AttributeType.DIS,
                        secondaryAttrXpReward = 14,
                        createdAtEpochMs = now
                    )
                )
            }
            val dueTopic = allTopics.firstOrNull { (it.nextRevisionDueEpochMs ?: now) <= now }
            if (dueTopic != null) {
                val subj = activeSubjects.find { it.id == dueTopic.subjectId }
                created.add(
                    AcademicQuest(
                        title = "Spaced Recall: ${dueTopic.title}",
                        description = "Complete active recall revision for ${dueTopic.title} and update confidence.",
                        category = AcademicQuestCategory.REVISION,
                        subjectId = dueTopic.subjectId,
                        subjectName = subj?.name.orEmpty(),
                        topicId = dueTopic.id,
                        topicTitle = dueTopic.title,
                        difficulty = AcademicQuestDifficulty.D_RANK,
                        objective = "Complete revision session",
                        targetValue = 20,
                        unit = "min",
                        deadlineEpochMs = now + 24 * 60 * 60_000L,
                        xpReward = AcademicQuestDifficulty.D_RANK.xpBonus,
                        primaryAttribute = AttributeType.WIS,
                        primaryAttrXpReward = 35,
                        secondaryAttribute = AttributeType.INT,
                        secondaryAttrXpReward = 18,
                        createdAtEpochMs = now - 250
                    )
                )
            }
            if (created.isNotEmpty()) {
                academicQuestDao.insertQuests(created.map { it.toEntity() })
            }
        }
        return created.size
    }

    suspend fun generateQuestsForSubject(subjectId: Long): Result<Int> {
        val subject = subjectDao.getSubjectById(subjectId)?.toDomain()
            ?: return Result.failure(IllegalArgumentException("Subject not found."))
        val now = System.currentTimeMillis()
        val modules = subjectDao.getModulesForSubject(subjectId)
        val firstPendingChapter = modules
            .flatMap { subjectDao.getChaptersForModule(it.id) }
            .map { it.toDomain() }
            .firstOrNull { !it.isCompleted }

        val quests = buildList {
            add(
                AcademicQuest(
                    title = "${subject.name}: 50-Min Focus Block",
                    description = "Execute an uninterrupted 50-minute study focus session for ${subject.name}.",
                    category = AcademicQuestCategory.DAILY_STUDY,
                    subjectId = subject.id,
                    subjectName = subject.name,
                    difficulty = AcademicQuestDifficulty.E_RANK,
                    objective = "Complete 50 minutes of focused study",
                    targetValue = 50,
                    unit = "min",
                    deadlineEpochMs = now + 24 * 60 * 60_000L,
                    xpReward = AcademicQuestDifficulty.E_RANK.xpBonus,
                    primaryAttribute = AttributeType.INT,
                    primaryAttrXpReward = 26,
                    secondaryAttribute = AttributeType.DIS,
                    secondaryAttrXpReward = 14,
                    createdAtEpochMs = now
                )
            )
            if (firstPendingChapter != null) {
                add(
                    AcademicQuest(
                        title = "Clear Chapter: ${firstPendingChapter.title}",
                        description = "Study and master all topics inside ${firstPendingChapter.title}.",
                        category = AcademicQuestCategory.CHAPTER_COMPLETION,
                        subjectId = subject.id,
                        subjectName = subject.name,
                        difficulty = AcademicQuestDifficulty.D_RANK,
                        objective = "Complete ${firstPendingChapter.title}",
                        targetValue = 1,
                        unit = "chapter",
                        xpReward = AcademicQuestDifficulty.D_RANK.xpBonus,
                        primaryAttribute = AttributeType.INT,
                        primaryAttrXpReward = 38,
                        secondaryAttribute = AttributeType.WIS,
                        secondaryAttrXpReward = 20,
                        createdAtEpochMs = now - 500
                    )
                )
            }
        }
        academicQuestDao.insertQuests(quests.map { it.toEntity() })
        return Result.success(quests.size)
    }

    suspend fun completeAcademicQuest(questId: Long, evidenceNote: String = ""): Result<Int> {
        var awardedXp = 0
        database.withTransaction {
            val quest = academicQuestDao.getQuestById(questId)?.toDomain() ?: return@withTransaction
            if (quest.status != AcademicQuestStatus.ACTIVE) return@withTransaction

            val rewardKey = "acad_quest_$questId"
            if (academicProgressionDao.existsByRewardKey(rewardKey)) return@withTransaction

            val now = System.currentTimeMillis()
            val evidence = evidenceNote.trim().ifBlank { "Player confirmed quest completion." }
            academicQuestDao.updateQuest(
                quest.copy(
                    currentValue = quest.targetValue,
                    status = AcademicQuestStatus.COMPLETED,
                    completionEvidence = evidence,
                    completedAtEpochMs = now
                ).toEntity()
            )

            val dayStart = startOfDayEpochMs(now)
            val xpToday = academicProgressionDao.getAcademicXpEarnedSince(dayStart)
            val selfCount = academicProgressionDao.getSelfReportedAcademicCountSince(dayStart)

            val reward = ProgressionEngine.calculateAcademicReward(
                kind = AcademicActivityKind.ACADEMIC_QUEST,
                durationMinutes = if (quest.unit == "min") quest.targetValue else 30,
                verificationStatus = quest.verificationStatus,
                academicXpAlreadyEarnedToday = xpToday,
                selfReportedAcademicCountToday = selfCount
            )
            awardedXp = reward.xpAwarded

            recordAcademicProgressionLocked(
                rewardKey = rewardKey,
                activityKind = AcademicActivityKind.ACADEMIC_QUEST,
                title = "Quest: ${quest.title}",
                subjectId = quest.subjectId,
                subjectName = quest.subjectName.ifBlank { "Academic" },
                durationMinutes = if (quest.unit == "min") quest.targetValue else 25,
                verificationStatus = quest.verificationStatus,
                xpAwarded = reward.xpAwarded,
                primaryAttr = reward.primaryAttribute,
                primaryAttrXp = reward.primaryAttrXpAwarded,
                secondaryAttr = reward.secondaryAttribute,
                secondaryAttrXp = reward.secondaryAttrXpAwarded,
                notes = "$evidence • ${reward.validationNote}",
                nowEpochMs = now
            )
        }
        return Result.success(awardedXp)
    }

    suspend fun cancelAcademicQuest(questId: Long) {
        val quest = academicQuestDao.getQuestById(questId)?.toDomain() ?: return
        if (quest.status == AcademicQuestStatus.ACTIVE) {
            academicQuestDao.updateQuest(quest.copy(status = AcademicQuestStatus.CANCELLED).toEntity())
        }
    }

    suspend fun approveAcademicStudyGoal(dailyMinutes: Int) {
        val safeDaily = dailyMinutes.coerceIn(15, 360)
        preferencesRepository.setAcademicGoals(safeDaily, safeDaily * 6)
    }

    private suspend fun recordAcademicProgressionLocked(
        rewardKey: String,
        activityKind: AcademicActivityKind,
        title: String,
        subjectId: Long?,
        subjectName: String,
        durationMinutes: Int,
        verificationStatus: VerificationStatus,
        xpAwarded: Int,
        primaryAttr: AttributeType,
        primaryAttrXp: Int,
        secondaryAttr: AttributeType,
        secondaryAttrXp: Int,
        notes: String,
        nowEpochMs: Long
    ) {
        val intXp = (if (primaryAttr == AttributeType.INT) primaryAttrXp else 0) +
            (if (secondaryAttr == AttributeType.INT) secondaryAttrXp else 0)
        val wisXp = (if (primaryAttr == AttributeType.WIS) primaryAttrXp else 0) +
            (if (secondaryAttr == AttributeType.WIS) secondaryAttrXp else 0)
        val disXp = (if (primaryAttr == AttributeType.DIS) primaryAttrXp else 0) +
            (if (secondaryAttr == AttributeType.DIS) secondaryAttrXp else 0)

        academicProgressionDao.insertRecord(
            AcademicProgressionRecord(
                rewardKey = rewardKey,
                activityKind = activityKind,
                title = title,
                subjectId = subjectId,
                verificationStatus = verificationStatus,
                xpAwarded = xpAwarded,
                intXpAwarded = intXp,
                wisXpAwarded = wisXp,
                disXpAwarded = disXp,
                notes = notes,
                timestampEpochMs = nowEpochMs
            ).toEntity()
        )

        val currentProfile = playerDao.getPlayerOnce()?.toDomain() ?: PlayerProfile()
        val levelResult = ProgressionEngine.applyXp(
            currentLevel = currentProfile.level,
            currentLevelXp = currentProfile.currentLevelXp,
            totalXp = currentProfile.totalXp,
            gainedXp = xpAwarded
        )
        val attrDeltas = buildMap<AttributeType, Int> {
            put(primaryAttr, primaryAttrXp)
            put(secondaryAttr, (get(secondaryAttr) ?: 0) + secondaryAttrXp)
        }
        val newAttrs = ProgressionEngine.applyAttributeXp(
            attributes = currentProfile.attributes,
            deltas = attrDeltas,
            isVerified = verificationStatus.isVerified
        )
        val newVerified = currentProfile.verifiedActivitiesCount + if (verificationStatus.isVerified) 1 else 0
        val newSelf = currentProfile.selfReportedActivitiesCount + if (!verificationStatus.isVerified) 1 else 0
        val newRank = ProgressionEngine.evaluateRank(
            level = levelResult.newLevel,
            totalAttributeScore = newAttrs.totalScore,
            verifiedActivitiesCount = newVerified
        )

        playerDao.upsertPlayer(
            currentProfile.copy(
                level = levelResult.newLevel,
                currentLevelXp = levelResult.newCurrentLevelXp,
                xpToNextLevel = levelResult.newXpToNextLevel,
                totalXp = levelResult.newTotalXp,
                rank = newRank,
                title = newRank.title,
                attributes = newAttrs,
                verifiedActivitiesCount = newVerified,
                selfReportedActivitiesCount = newSelf
            ).toEntity()
        )

        activityLogDao.insertLog(
            ActivityLog(
                title = title,
                category = QuestCategory.INTELLECT,
                durationMinutes = durationMinutes,
                verificationStatus = verificationStatus,
                xpAwarded = xpAwarded,
                primaryAttribute = primaryAttr,
                primaryAttrXpAwarded = primaryAttrXp,
                notes = "$subjectName • $notes",
                timestampEpochMs = nowEpochMs
            ).toEntity()
        )

        notificationDao.insertNotification(
            SystemNotification(
                title = "Academic Progression (+${xpAwarded} XP)",
                message = "$title (${verificationStatus.label}). ${primaryAttr.code} +$primaryAttrXp XP.",
                category = NotificationCategory.QUEST_UPDATE,
                timestampEpochMs = nowEpochMs
            ).toEntity()
        )
    }

    private fun startOfDayEpochMs(epochMs: Long): Long {
        val zone = ZoneId.systemDefault()
        val date: LocalDate = Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate()
        return date.atStartOfDay(zone).toInstant().toEpochMilli()
    }
}
