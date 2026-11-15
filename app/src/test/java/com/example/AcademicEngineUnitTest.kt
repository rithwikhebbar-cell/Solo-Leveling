package com.example

import com.example.domain.engine.ExamCountdownCalculator
import com.example.domain.engine.FocusTimerCalculator
import com.example.domain.engine.ProgressionEngine
import com.example.domain.engine.RevisionEngine
import com.example.domain.model.AcademicActivityKind
import com.example.domain.model.AcademicAssessment
import com.example.domain.model.AcademicExam
import com.example.domain.model.AcademicTopic
import com.example.domain.model.AssessmentType
import com.example.domain.model.AttributeType
import com.example.domain.model.ChapterDifficulty
import com.example.domain.model.MarkingSystem
import com.example.domain.model.RevisionUrgency
import com.example.domain.model.VerificationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AcademicEngineUnitTest {

    @Test
    fun focusTimerCalculator_computesElapsedRemainingAndFormatting() {
        val mid = FocusTimerCalculator.calculate(elapsedSeconds = 600L, targetMinutes = 25)
        assertEquals(600L, mid.elapsedSeconds)
        assertEquals(1500L, mid.targetSeconds)
        assertEquals(900L, mid.remainingSeconds)
        assertEquals(0.4f, mid.progressFraction, 0.001f)
        assertFalse(mid.isTargetReached)
        assertEquals("10:00", FocusTimerCalculator.formatDurationMmSs(mid.elapsedSeconds))

        val done = FocusTimerCalculator.calculate(elapsedSeconds = 1600L, targetMinutes = 25)
        assertEquals(0L, done.remainingSeconds)
        assertEquals(1f, done.progressFraction, 0.001f)
        assertTrue(done.isTargetReached)
    }

    @Test
    fun studySessionValidation_rejectsShortTimerAndInvalidDurations() {
        val now = 1_700_000_000_000L
        val shortTimer = ProgressionEngine.validateStudySession(
            title = "Instant Quit",
            subjectId = 1L,
            startEpochMs = now - 20_000L,
            endEpochMs = now,
            durationMinutes = 1,
            isSystemTimer = true,
            nowEpochMs = now
        )
        assertFalse(shortTimer.isValid)

        val validTimer = ProgressionEngine.validateStudySession(
            title = "Valid Pomodoro",
            subjectId = 1L,
            startEpochMs = now - 25 * 60_000L,
            endEpochMs = now,
            durationMinutes = 25,
            isSystemTimer = true,
            nowEpochMs = now
        )
        assertTrue(validTimer.isValid)

        val noSubject = ProgressionEngine.validateStudySession(
            title = "Orphan Study",
            subjectId = 0L,
            startEpochMs = now - 25 * 60_000L,
            endEpochMs = now,
            durationMinutes = 25,
            isSystemTimer = false,
            nowEpochMs = now
        )
        assertFalse(noSubject.isValid)
    }

    @Test
    fun assessmentValidation_rejectsOutOfRangeOrZeroMaxMarks() {
        assertFalse(
            ProgressionEngine.validateAssessmentResult(
                title = "Quiz",
                subjectId = 1L,
                scoredMarks = 105.0,
                maxMarks = 100.0
            ).isValid
        )
        assertFalse(
            ProgressionEngine.validateAssessmentResult(
                title = "Quiz",
                subjectId = 1L,
                scoredMarks = 10.0,
                maxMarks = 0.0
            ).isValid
        )
        assertTrue(
            ProgressionEngine.validateAssessmentResult(
                title = "Midterm Exam",
                subjectId = 1L,
                scoredMarks = 88.0,
                maxMarks = 100.0
            ).isValid
        )
    }

    @Test
    fun academicRewardCalculation_appliesProvenanceWeightAttributeRoutingAndDailyCaps() {
        val verifiedExam = ProgressionEngine.calculateAcademicReward(
            kind = AcademicActivityKind.EXAM_ASSESSMENT,
            durationMinutes = 30,
            verificationStatus = VerificationStatus.VERIFIED_ASSESSMENT,
            academicXpAlreadyEarnedToday = 0,
            selfReportedAcademicCountToday = 0,
            assessmentRatio = 0.9f
        )
        val selfExam = ProgressionEngine.calculateAcademicReward(
            kind = AcademicActivityKind.EXAM_ASSESSMENT,
            durationMinutes = 30,
            verificationStatus = VerificationStatus.SELF_REPORTED,
            academicXpAlreadyEarnedToday = 0,
            selfReportedAcademicCountToday = 0,
            assessmentRatio = 0.9f
        )
        assertEquals(AttributeType.INT, verifiedExam.primaryAttribute)
        assertEquals(AttributeType.DIS, verifiedExam.secondaryAttribute)
        assertTrue(verifiedExam.xpAwarded > selfExam.xpAwarded)

        val revision = ProgressionEngine.calculateAcademicReward(
            kind = AcademicActivityKind.REVISION,
            durationMinutes = 30,
            verificationStatus = VerificationStatus.SELF_REPORTED,
            academicXpAlreadyEarnedToday = 0,
            selfReportedAcademicCountToday = 0
        )
        assertEquals(AttributeType.WIS, revision.primaryAttribute)

        val cappedSelf = ProgressionEngine.calculateAcademicReward(
            kind = AcademicActivityKind.STUDY_SESSION,
            durationMinutes = 45,
            verificationStatus = VerificationStatus.SELF_REPORTED,
            academicXpAlreadyEarnedToday = 0,
            selfReportedAcademicCountToday = ProgressionEngine.DAILY_SELF_REPORTED_ACADEMIC_LIMIT
        )
        assertEquals(0, cappedSelf.xpAwarded)
        assertTrue(cappedSelf.wasCapped)

        val cappedDailyXp = ProgressionEngine.calculateAcademicReward(
            kind = AcademicActivityKind.FOCUS_SESSION,
            durationMinutes = 60,
            verificationStatus = VerificationStatus.VERIFIED_SYSTEM_TIMER,
            academicXpAlreadyEarnedToday = ProgressionEngine.DAILY_ACADEMIC_XP_CAP,
            selfReportedAcademicCountToday = 0
        )
        assertEquals(0, cappedDailyXp.xpAwarded)
        assertTrue(cappedDailyXp.wasCapped)
    }

    @Test
    fun revisionEngine_adaptsIntervalsByConfidenceDifficultyAndAssessmentAndLabelsEstimate() {
        val now = 1_700_000_000_000L
        val strugglingBoss = RevisionEngine.scheduleNextRevision(
            nowEpochMs = now,
            previousRevisionCount = 2,
            selfReportedConfidence = 1,
            chapterDifficulty = ChapterDifficulty.BOSS,
            latestAssessmentRatio = 0.40f
        )
        val confidentFoundation = RevisionEngine.scheduleNextRevision(
            nowEpochMs = now,
            previousRevisionCount = 2,
            selfReportedConfidence = 5,
            chapterDifficulty = ChapterDifficulty.FOUNDATION,
            latestAssessmentRatio = 0.92f
        )

        assertTrue(confidentFoundation.nextIntervalDays > strugglingBoss.nextIntervalDays)
        assertTrue(strugglingBoss.retentionDisclaimer.contains("not scientifically measured", ignoreCase = true))

        val dayMs = 24 * 60 * 60_000L
        val overdueTopic = AcademicTopic(id = 1, chapterId = 1, subjectId = 1, title = "Eigenvalues", nextRevisionDueEpochMs = now - 2 * dayMs)
        val futureTopic = AcademicTopic(id = 2, chapterId = 1, subjectId = 1, title = "Integrals", nextRevisionDueEpochMs = now + 5 * dayMs)

        assertEquals(RevisionUrgency.OVERDUE, RevisionEngine.classifyUrgency(overdueTopic, now))
        assertEquals(RevisionUrgency.UPCOMING, RevisionEngine.classifyUrgency(futureTopic, now))
    }

    @Test
    fun examCountdownCalculator_computesDaysAndReadiness() {
        val now = 1_700_000_000_000L
        val examDate = now + 5 * 24 * 60 * 60_000L
        val exam = AcademicExam(
            id = 1,
            subjectId = 10L,
            subjectName = "Algorithms",
            title = "Final Exam",
            examDateEpochMs = examDate,
            markingSystem = MarkingSystem.PERCENTAGE,
            syllabusCoveragePercent = 80
        )
        val assessments = listOf(
            AcademicAssessment(
                id = 1,
                subjectId = 10L,
                title = "Mock 1",
                assessmentType = AssessmentType.MOCK_EXAM,
                markingSystem = MarkingSystem.PERCENTAGE,
                scoredMarks = 88.0,
                maxMarks = 100.0,
                verificationStatus = VerificationStatus.SELF_REPORTED,
                xpAwarded = 30
            )
        )
        val readiness = ExamCountdownCalculator.evaluateReadiness(
            exam = exam,
            subjectSyllabusPercent = 85,
            avgSubjectConfidenceEstimate = 4.5f,
            recentAssessments = assessments,
            nowEpochMs = now
        )
        assertEquals(5L, readiness.daysRemaining)
        assertTrue(readiness.readinessPercent >= 80)
        assertEquals("Exam Ready", readiness.statusLabel)
    }
}
