package com.example.domain.engine

import com.example.domain.model.AcademicAssessment
import com.example.domain.model.AcademicExam
import com.example.domain.model.AcademicTopic
import com.example.domain.model.ChapterDifficulty
import com.example.domain.model.ExamReadinessSummary
import com.example.domain.model.RevisionUrgency
import com.example.domain.model.ScheduledRevisionItem
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

data class RevisionScheduleResult(
    val nextIntervalDays: Int,
    val nextDueEpochMs: Long,
    val retentionEstimatePercent: Int,
    val retentionDisclaimer: String = "Estimated familiarity — not scientifically measured memory retention"
)

data class FocusTimerSnapshot(
    val elapsedSeconds: Long,
    val targetSeconds: Long,
    val remainingSeconds: Long,
    val progressFraction: Float,
    val isTargetReached: Boolean
)

/**
 * Offline deterministic Revision Intelligence Engine, Focus Timer calculator,
 * and Examination countdown/readiness calculator.
 */
object RevisionEngine {

    private const val DAY_MS = 24 * 60 * 60_000L

    fun scheduleNextRevision(
        nowEpochMs: Long,
        previousRevisionCount: Int,
        selfReportedConfidence: Int,
        chapterDifficulty: ChapterDifficulty,
        latestAssessmentRatio: Float? = null
    ): RevisionScheduleResult {
        val confidence = selfReportedConfidence.coerceIn(1, 5)
        val baseInterval = when (previousRevisionCount.coerceAtLeast(0)) {
            0 -> 1
            1 -> 3
            2 -> 7
            3 -> 14
            4 -> 25
            else -> 35
        }

        val confidenceMultiplier = when (confidence) {
            1 -> 0.45f
            2 -> 0.7f
            3 -> 1.0f
            4 -> 1.25f
            else -> 1.45f
        }

        val difficultyMultiplier = when (chapterDifficulty) {
            ChapterDifficulty.FOUNDATION -> 1.15f
            ChapterDifficulty.INTERMEDIATE -> 1.0f
            ChapterDifficulty.ADVANCED -> 0.85f
            ChapterDifficulty.BOSS -> 0.7f
        }

        val assessmentMultiplier = when {
            latestAssessmentRatio == null -> 1.0f
            latestAssessmentRatio < 0.50f -> 0.6f
            latestAssessmentRatio < 0.75f -> 0.85f
            else -> 1.15f
        }

        val computedDays = (baseInterval * confidenceMultiplier * difficultyMultiplier * assessmentMultiplier)
            .roundToInt()
            .coerceIn(1, 60)

        val nextDue = nowEpochMs + computedDays * DAY_MS
        val estimate = estimateRetentionPercent(
            nowEpochMs = nowEpochMs,
            lastRevisedEpochMs = nowEpochMs,
            nextDueEpochMs = nextDue,
            confidenceEstimate = confidence,
            revisionCount = previousRevisionCount + 1,
            latestAssessmentRatio = latestAssessmentRatio
        )

        return RevisionScheduleResult(
            nextIntervalDays = computedDays,
            nextDueEpochMs = nextDue,
            retentionEstimatePercent = estimate
        )
    }

    /**
     * Estimates current familiarity percentage (strictly labelled as an estimate, never measured memory).
     */
    fun estimateRetentionPercent(
        nowEpochMs: Long,
        lastRevisedEpochMs: Long?,
        nextDueEpochMs: Long?,
        confidenceEstimate: Int,
        revisionCount: Int,
        latestAssessmentRatio: Float? = null
    ): Int {
        val baseFromConfidence = when (confidenceEstimate.coerceIn(1, 5)) {
            1 -> 32
            2 -> 48
            3 -> 65
            4 -> 80
            else -> 92
        }
        val repBonus = (revisionCount.coerceAtMost(5)) * 2
        val assessmentAdj = when {
            latestAssessmentRatio == null -> 0
            latestAssessmentRatio < 0.5f -> -10
            latestAssessmentRatio >= 0.85f -> 5
            else -> 0
        }

        val overduePenalty = if (nextDueEpochMs != null && nowEpochMs > nextDueEpochMs) {
            val daysOverdue = ((nowEpochMs - nextDueEpochMs) / DAY_MS).toInt().coerceAtLeast(0)
            (daysOverdue * 6).coerceAtMost(35)
        } else if (lastRevisedEpochMs == null && revisionCount == 0) {
            12
        } else {
            0
        }

        return (baseFromConfidence + repBonus + assessmentAdj - overduePenalty).coerceIn(15, 98)
    }

    fun classifyUrgency(
        topic: AcademicTopic,
        nowEpochMs: Long
    ): RevisionUrgency {
        val due = topic.nextRevisionDueEpochMs ?: nowEpochMs
        val zone = ZoneId.systemDefault()
        val today = Instant.ofEpochMilli(nowEpochMs).atZone(zone).toLocalDate()
        val dueDate = Instant.ofEpochMilli(due).atZone(zone).toLocalDate()

        return when {
            dueDate.isBefore(today) -> RevisionUrgency.OVERDUE
            dueDate.isEqual(today) -> RevisionUrgency.DUE_TODAY
            else -> RevisionUrgency.UPCOMING
        }
    }

    fun buildQueue(
        topics: List<AcademicTopic>,
        subjectNamesById: Map<Long, String>,
        difficultyByChapterId: Map<Long, ChapterDifficulty>,
        assessmentRatioBySubjectId: Map<Long, Float>,
        nowEpochMs: Long = System.currentTimeMillis()
    ): List<ScheduledRevisionItem> {
        return topics.map { topic ->
            val difficulty = difficultyByChapterId[topic.chapterId] ?: ChapterDifficulty.INTERMEDIATE
            val assessmentRatio = assessmentRatioBySubjectId[topic.subjectId]
            val urgency = classifyUrgency(topic, nowEpochMs)
            val retention = estimateRetentionPercent(
                nowEpochMs = nowEpochMs,
                lastRevisedEpochMs = topic.lastRevisedEpochMs,
                nextDueEpochMs = topic.nextRevisionDueEpochMs,
                confidenceEstimate = topic.confidenceEstimate,
                revisionCount = topic.revisionCount,
                latestAssessmentRatio = assessmentRatio
            )
            ScheduledRevisionItem(
                topic = topic,
                subjectName = subjectNamesById[topic.subjectId] ?: "Subject",
                chapterDifficulty = difficulty,
                urgency = urgency,
                retentionEstimatePercent = retention
            )
        }.sortedWith(
            compareBy<ScheduledRevisionItem> { it.urgency.ordinal }
                .thenBy { it.topic.nextRevisionDueEpochMs ?: Long.MAX_VALUE }
                .thenBy { it.retentionEstimatePercent }
        )
    }
}

object FocusTimerCalculator {

    fun calculate(
        elapsedSeconds: Long,
        targetMinutes: Int
    ): FocusTimerSnapshot {
        val safeElapsed = elapsedSeconds.coerceAtLeast(0L)
        val targetSeconds = (targetMinutes.coerceAtLeast(1) * 60L)
        val remaining = (targetSeconds - safeElapsed).coerceAtLeast(0L)
        val progress = (safeElapsed.toFloat() / targetSeconds.toFloat()).coerceIn(0f, 1f)
        return FocusTimerSnapshot(
            elapsedSeconds = safeElapsed,
            targetSeconds = targetSeconds,
            remainingSeconds = remaining,
            progressFraction = progress,
            isTargetReached = safeElapsed >= targetSeconds
        )
    }

    fun formatDurationMmSs(totalSeconds: Long): String {
        val safe = totalSeconds.coerceAtLeast(0L)
        val mins = safe / 60L
        val secs = safe % 60L
        return "%02d:%02d".format(mins, secs)
    }
}

object ExamCountdownCalculator {

    fun daysRemaining(examDateEpochMs: Long, nowEpochMs: Long = System.currentTimeMillis()): Long {
        val zone = ZoneId.systemDefault()
        val today = Instant.ofEpochMilli(nowEpochMs).atZone(zone).toLocalDate()
        val examDate = Instant.ofEpochMilli(examDateEpochMs).atZone(zone).toLocalDate()
        return ChronoUnit.DAYS.between(today, examDate)
    }

    fun evaluateReadiness(
        exam: AcademicExam,
        subjectSyllabusPercent: Int,
        avgSubjectConfidenceEstimate: Float?,
        recentAssessments: List<AcademicAssessment>,
        nowEpochMs: Long = System.currentTimeMillis()
    ): ExamReadinessSummary {
        val days = daysRemaining(exam.examDateEpochMs, nowEpochMs)
        val syllabusComponent = ((exam.syllabusCoveragePercent.coerceAtLeast(subjectSyllabusPercent)).coerceIn(0, 100) * 0.45f)
        val confidenceComponent = (((avgSubjectConfidenceEstimate ?: 3f) / 5f).coerceIn(0f, 1f) * 100f) * 0.25f
        val assessmentRatio = recentAssessments
            .filter { it.subjectId == exam.subjectId }
            .take(5)
            .takeIf { it.isNotEmpty() }
            ?.map { it.normalizedRatio }
            ?.average()
            ?.toFloat()
            ?: 0.55f
        val assessmentComponent = (assessmentRatio * 100f) * 0.30f

        val readiness = (syllabusComponent + confidenceComponent + assessmentComponent)
            .roundToInt()
            .coerceIn(5, 100)

        val statusLabel = when {
            exam.isCompleted -> "Exam Completed"
            days < 0L -> "Past Date — Record Result"
            readiness >= 80 -> "Exam Ready"
            readiness >= 55 -> "On Track"
            days <= 7L -> "Urgent Prep Needed"
            else -> "Building Foundation"
        }

        return ExamReadinessSummary(
            exam = exam,
            daysRemaining = days,
            readinessPercent = readiness,
            statusLabel = statusLabel
        )
    }
}
