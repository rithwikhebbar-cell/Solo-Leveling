package com.example

import com.example.domain.engine.FailurePatternAnalysisEngine
import com.example.domain.model.CommitmentState
import com.example.domain.model.ConfirmedFailureRecord
import com.example.domain.model.FailurePatternType
import com.example.domain.model.PatternSeverity
import com.example.domain.model.QuestCommitment
import com.example.domain.model.RecoveryState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class FailurePatternAnalysisEngineTest {

    private val baseEpochMs = 1_700_000_000_000L

    @Test
    fun analyze_insufficientEvidence_returnsEmptyList() {
        val patterns = FailurePatternAnalysisEngine.analyzePatterns(
            commitments = emptyList(),
            failures = emptyList()
        )
        assertTrue(patterns.isEmpty())
    }

    @Test
    fun analyze_excessiveCommitmentsDetected_whenOverloadedCycleFails() {
        // Cycle with 5 commitments and 2 failures
        val commitments = (1..5).map { id ->
            QuestCommitment(
                commitmentId = "c$id",
                questId = id.toLong(),
                accountOwnerId = "p1",
                logicalCycleId = "cycle_overloaded",
                state = if (id <= 2) CommitmentState.FAILED else CommitmentState.COMPLETED,
                deadlineEpochMs = baseEpochMs
            )
        }
        val failures = (1..2).map { id ->
            ConfirmedFailureRecord(
                failureId = "f$id",
                commitmentId = "c$id",
                questId = id.toLong(),
                accountOwnerId = "p1",
                logicalCycleId = "cycle_overloaded",
                failedAtEpochMs = baseEpochMs,
                reasonCode = "OVERDUE",
                triggerSource = "TEST"
            )
        }

        val patterns = FailurePatternAnalysisEngine.analyzePatterns(
            commitments = commitments,
            failures = failures
        )

        val found = patterns.find { it.patternType == FailurePatternType.EXCESSIVE_DAILY_COMMITMENTS }
        assertTrue(found != null)
        assertEquals(1, found!!.evidenceCount)
        assertEquals(PatternSeverity.MEDIUM, found.severity)
        assertTrue(found.actionableRecommendation.contains("3 core quests"))
    }

    @Test
    fun analyze_repeatedMissedDeadlines_detectsLateNightClustering() {
        val utcZone = ZoneId.of("UTC")
        // Create 3 failures at 23:00 UTC (late night)
        val lateNightEpoch = ZonedDateTime.of(2023, 11, 15, 23, 30, 0, 0, utcZone)
            .toInstant().toEpochMilli()

        val failures = (1..3).map { id ->
            ConfirmedFailureRecord(
                failureId = "f$id",
                commitmentId = "c$id",
                questId = id.toLong(),
                accountOwnerId = "p1",
                logicalCycleId = "cy$id",
                failedAtEpochMs = lateNightEpoch,
                reasonCode = "OVERDUE",
                triggerSource = "TEST"
            )
        }

        val patterns = FailurePatternAnalysisEngine.analyzePatterns(
            commitments = emptyList(),
            failures = failures,
            zoneId = utcZone
        )

        val found = patterns.find { it.patternType == FailurePatternType.REPEATED_MISSED_DEADLINES }
        assertTrue(found != null)
        assertEquals(3, found!!.evidenceCount)
        assertTrue(found.actionableRecommendation.contains("earlier in the day"))
    }

    @Test
    fun analyze_repeatedRescheduling_detectsEstimationFriction() {
        val commitments = listOf(
            QuestCommitment(commitmentId = "c1", questId = 1L, accountOwnerId = "p1", logicalCycleId = "cy1", state = CommitmentState.FAILED, rescheduleCount = 2, deadlineEpochMs = baseEpochMs),
            QuestCommitment(commitmentId = "c2", questId = 2L, accountOwnerId = "p1", logicalCycleId = "cy2", state = CommitmentState.FAILED, rescheduleCount = 3, deadlineEpochMs = baseEpochMs)
        )

        val patterns = FailurePatternAnalysisEngine.analyzePatterns(
            commitments = commitments,
            failures = emptyList()
        )

        val found = patterns.find { it.patternType == FailurePatternType.REPEATED_RESCHEDULING }
        assertTrue(found != null)
        assertEquals(2, found!!.evidenceCount)
        assertTrue(found.actionableRecommendation.contains("smaller, bite-sized tasks"))
    }

    @Test
    fun analyze_academicRecoveryConflict_detectsDualDomainFailures() {
        val failures = (1..4).map { id ->
            ConfirmedFailureRecord(
                failureId = "f$id",
                commitmentId = "c$id",
                questId = id.toLong(),
                accountOwnerId = "p1",
                logicalCycleId = "cy$id",
                failedAtEpochMs = baseEpochMs,
                reasonCode = "OVERDUE",
                triggerSource = "TEST"
            )
        }
        val questCategoryMap = mapOf(
            1L to "PHYSICAL", 2L to "PHYSICAL",
            3L to "ACADEMIC", 4L to "ACADEMIC"
        )

        val patterns = FailurePatternAnalysisEngine.analyzePatterns(
            commitments = emptyList(),
            failures = failures,
            questCategoryMap = questCategoryMap
        )

        val found = patterns.find { it.patternType == FailurePatternType.ACADEMIC_RECOVERY_CONFLICT }
        assertTrue(found != null)
        assertEquals(4, found!!.evidenceCount)
        assertTrue(found.actionableRecommendation.contains("heavy workout days with light revision"))
    }

    @Test
    fun analyze_highFatigueOverexertion_flagsRecoveryStateConflict() {
        val now = baseEpochMs
        val failures = listOf(
            ConfirmedFailureRecord(
                failureId = "f1",
                commitmentId = "c1",
                questId = 1L,
                accountOwnerId = "p1",
                logicalCycleId = "cy1",
                failedAtEpochMs = now - 3600_000L, // 1 hr ago
                reasonCode = "OVERDUE",
                triggerSource = "TEST"
            )
        )

        val patterns = FailurePatternAnalysisEngine.analyzePatterns(
            commitments = emptyList(),
            failures = failures,
            recentRecoveryState = RecoveryState.REST_RECOMMENDED,
            nowEpochMs = now
        )

        val found = patterns.find { it.patternType == FailurePatternType.HIGH_FATIGUE_OVEREXERTION }
        assertTrue(found != null)
        assertEquals(PatternSeverity.HIGH, found!!.severity)
        assertTrue(found.actionableRecommendation.contains("Recovery Requirement exemption"))
    }
}
