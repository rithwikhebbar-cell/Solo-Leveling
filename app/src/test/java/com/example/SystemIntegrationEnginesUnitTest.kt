package com.example

import com.example.domain.engine.DailyCycleManager
import com.example.domain.engine.DailyExecutionOverride
import com.example.domain.engine.EventDeduplicationManager
import com.example.domain.engine.EventProcessingCoordinator
import com.example.domain.engine.IntelligenceTelemetryInput
import com.example.domain.engine.NotificationDeduplicationManager
import com.example.domain.engine.NotificationOrchestrator
import com.example.domain.engine.NotificationPriorityEngine
import com.example.domain.engine.PlannedSystemNotification
import com.example.domain.engine.ProgressionCommand
import com.example.domain.engine.ProgressionEngine
import com.example.domain.engine.ProgressionOrchestrator
import com.example.domain.engine.QuietHoursManager
import com.example.domain.engine.SystemContextAggregator
import com.example.domain.engine.SystemDataIntegrityEngine
import com.example.domain.engine.SystemEventDispatcher
import com.example.domain.engine.SystemHealthEngine
import com.example.domain.engine.SystemHealthInput
import com.example.domain.engine.SystemIntegritySnapshotInput
import com.example.domain.engine.UnifiedDailyExecutionEngine
import com.example.domain.model.AiConsentState
import com.example.domain.model.AttributeType
import com.example.domain.model.AutomationExecutionStatus
import com.example.domain.model.AutomationJobSpec
import com.example.domain.model.AutomationTriggerType
import com.example.domain.model.DailyCycleExecution
import com.example.domain.model.DailyExecutionStatus
import com.example.domain.model.DungeonCompletionRecord
import com.example.domain.model.DungeonDifficulty
import com.example.domain.model.EventDiagnosticsSummary
import com.example.domain.model.EventProcessingStatus
import com.example.domain.model.ExerciseRecord
import com.example.domain.model.GoalMilestone
import com.example.domain.model.HunterRank
import com.example.domain.model.NotificationCategory
import com.example.domain.model.PhysicalDevelopmentState
import com.example.domain.model.PlayerIntelligencePreferences
import com.example.domain.model.PlayerProfile
import com.example.domain.model.RecoveryAssessment
import com.example.domain.model.RecoveryState
import com.example.domain.model.ScheduledActivityItem
import com.example.domain.model.ScheduledSourceType
import com.example.domain.model.SyncSummaryState
import com.example.domain.model.SystemEvent
import com.example.domain.model.SystemEventType
import com.example.domain.model.SystemHealthLevel
import com.example.domain.model.SystemNotification
import com.example.domain.model.SystemNotificationPreferences
import com.example.domain.model.VerificationStatus
import com.example.domain.model.WorkoutCategory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class SystemIntegrationEnginesUnitTest {

    @Test
    fun `EventDeduplicationManager detects duplicates, loop depth overflow, and schema bounds`() {
        val now = 1_750_000_000_000L
        val key = EventDeduplicationManager.buildIdempotencyKey(
            eventType = SystemEventType.WORKOUT_COMPLETED,
            sourceModule = "PHYSICAL",
            relatedEntityId = "42",
            bucketKey = "b1"
        )
        val first = SystemEvent(
            eventType = SystemEventType.WORKOUT_COMPLETED,
            timestampEpochMs = now,
            sourceModule = "PHYSICAL",
            relatedEntityId = "42",
            metadata = mapOf("idempotencyKey" to key),
            processingStatus = EventProcessingStatus.PROCESSED
        )
        val duplicate = first.copy(eventId = "other-id", timestampEpochMs = now + 2_000L)
        assertTrue(EventDeduplicationManager.isDuplicate(duplicate, listOf(first)))

        val deepEvent = first.copy(metadata = mapOf("chainDepth" to "4"))
        assertTrue(EventDeduplicationManager.wouldExceedLoopDepth(deepEvent))

        val futureSchema = first.copy(schemaVersion = 99)
        assertFalse(EventDeduplicationManager.isSchemaCompatible(futureSchema))
    }

    @Test
    fun `SystemEventDispatcher orders events chronologically and escalates failures to DEAD_LETTER`() = runTest {
        val dispatchedOrder = mutableListOf<String>()
        val dispatcher = SystemEventDispatcher()
        dispatcher.register(SystemEventType.ACADEMIC_SESSION_COMPLETED) { ev ->
            dispatchedOrder.add(ev.relatedEntityId)
            if (ev.relatedEntityId == "fail") Result.failure(IllegalStateException("Boom")) else Result.success(Unit)
        }

        val eLate = SystemEvent(
            eventType = SystemEventType.ACADEMIC_SESSION_COMPLETED,
            timestampEpochMs = 2000L,
            relatedEntityId = "second"
        )
        val eEarly = SystemEvent(
            eventType = SystemEventType.ACADEMIC_SESSION_COMPLETED,
            timestampEpochMs = 1000L,
            relatedEntityId = "first"
        )
        val ordered = EventProcessingCoordinator.orderForProcessing(listOf(eLate, eEarly))
        ordered.forEach { dispatcher.dispatch(it) }
        assertEquals(listOf("first", "second"), dispatchedOrder)

        assertEquals(EventProcessingStatus.FAILED, EventProcessingCoordinator.nextStatusAfterFailure(0))
        assertEquals(EventProcessingStatus.FAILED, EventProcessingCoordinator.nextStatusAfterFailure(1))
        assertEquals(EventProcessingStatus.DEAD_LETTER, EventProcessingCoordinator.nextStatusAfterFailure(2))
    }

    @Test
    fun `UnifiedDailyExecutionEngine never completes by elapsed time alone and guards recovery`() {
        val today = 20150L
        val trainingItem = ScheduledActivityItem(
            sourceType = ScheduledSourceType.PHYSICAL_TRAINING,
            sourceRecordId = 10L,
            title = "Interval Sprint",
            subtitle = "Physical",
            dateEpochDay = today,
            startMinuteOfDay = 8 * 60,
            durationMinutes = 30,
            isCompleted = false
        )
        val studyItem = ScheduledActivityItem(
            sourceType = ScheduledSourceType.ACADEMIC_STUDY,
            sourceRecordId = 11L,
            title = "Algorithms Review",
            subtitle = "Academic",
            dateEpochDay = today,
            startMinuteOfDay = 10 * 60,
            durationMinutes = 45,
            isCompleted = false
        )
        val recoveryRest = RecoveryAssessment(
            state = RecoveryState.REST_RECOMMENDED,
            readinessScore = 25,
            measuredLoadMinutes48h = 180,
            measuredLoadMinutes7d = 420,
            measuredSessions7d = 6,
            consecutiveActiveDays = 7,
            selfReportedDiscomfort = true,
            recommendedActions = listOf("Take rest day"),
            summaryMessage = "Rest recommended"
        )

        // Elapsed time past 11:00 -> items become MISSED, NEVER auto-COMPLETED!
        val summary = UnifiedDailyExecutionEngine.buildDailyTimeline(
            targetEpochDay = today,
            scheduledItems = listOf(trainingItem, studyItem),
            overridesByKey = mapOf(
                UnifiedDailyExecutionEngine.keyFor(today, ScheduledSourceType.ACADEMIC_STUDY, 11L) to
                    DailyExecutionOverride(
                        executionKey = UnifiedDailyExecutionEngine.keyFor(today, ScheduledSourceType.ACADEMIC_STUDY, 11L),
                        status = DailyExecutionStatus.COMPLETED,
                        evidenceReference = "" // blank evidence must downgrade from COMPLETED!
                    )
            ),
            recovery = recoveryRest,
            currentMinuteOfDay = 13 * 60,
            todayEpochDay = today
        )

        val phys = summary.items.first { it.sourceRecordId == 10L }
        val study = summary.items.first { it.sourceRecordId == 11L }
        assertTrue(phys.recoveryRestricted)
        assertEquals(DailyExecutionStatus.MISSED, phys.status)
        assertEquals(DailyExecutionStatus.PARTIALLY_COMPLETED, study.status)

        // Explicit validation rejects blank evidence & recovery-blocked training
        assertTrue(UnifiedDailyExecutionEngine.validateCompletionTransition(phys, "PROOF").isFailure)
        assertTrue(UnifiedDailyExecutionEngine.validateCompletionTransition(study.copy(recoveryRestricted = false), "").isFailure)
        assertTrue(UnifiedDailyExecutionEngine.validateCompletionTransition(study.copy(recoveryRestricted = false), "FOCUS_TIMER_99").isSuccess)

        // Conflict reschedule finds alternative slots
        val slots = UnifiedDailyExecutionEngine.findRescheduleSlots(study, summary.items)
        assertTrue(slots.isNotEmpty())
    }

    @Test
    fun `DailyCycleManager is idempotent across DST transitions and counts multi-day gaps without losing history`() {
        val nyZone = ZoneId.of("America/New_York")
        // Spring-forward morning at 05:30 local (past 04:00 reset)
        val springMorningMs = ZonedDateTime.of(2025, 3, 9, 5, 30, 0, 0, nyZone).toInstant().toEpochMilli()
        val plan1 = DailyCycleManager.evaluateCyclePlan(
            nowEpochMs = springMorningMs,
            lastExecutedEpochDay = null,
            preferredTimezoneId = nyZone.id,
            dailyResetHour = 4
        )
        assertTrue(plan1.shouldExecute)

        // Re-evaluating later the same day must idempotently skip
        val sameDayNightMs = ZonedDateTime.of(2025, 3, 9, 23, 45, 0, 0, nyZone).toInstant().toEpochMilli()
        val planSkip = DailyCycleManager.evaluateCyclePlan(
            nowEpochMs = sameDayNightMs,
            lastExecutedEpochDay = plan1.targetEpochDay,
            preferredTimezoneId = nyZone.id,
            dailyResetHour = 4
        )
        assertFalse(planSkip.shouldExecute)
        assertEquals(0, planSkip.missedDaysCount)

        // Jumping 4 days ahead detects 3 missed days cleanly
        val fourDaysLaterMs = ZonedDateTime.of(2025, 3, 13, 8, 0, 0, 0, nyZone).toInstant().toEpochMilli()
        val catchUp = DailyCycleManager.evaluateCyclePlan(
            nowEpochMs = fourDaysLaterMs,
            lastExecutedEpochDay = plan1.targetEpochDay,
            preferredTimezoneId = nyZone.id,
            dailyResetHour = 4
        )
        assertTrue(catchUp.shouldExecute)
        assertEquals(3, catchUp.missedDaysCount)
    }

    @Test
    fun `SystemDataIntegrityEngine detects XP mismatch, orphan exercises, missing audits, and stuck events`() {
        val brokenProfile = PlayerProfile(
            level = 2,
            currentLevelXp = 10,
            xpToNextLevel = 999999, // corrupted threshold
            rank = HunterRank.E_RANK
        )
        val findings = SystemDataIntegrityEngine.scan(
            SystemIntegritySnapshotInput(
                profile = brokenProfile,
                exerciseRecords = listOf(
                    ExerciseRecord(
                        id = 55L,
                        workoutSessionId = 999L, // orphan
                        exerciseId = "pushup",
                        exerciseName = "Push-ups",
                        category = WorkoutCategory.STRENGTH,
                        setsCompleted = 3,
                        repsPerSet = 15,
                        restSeconds = 60,
                        primaryAttribute = AttributeType.STR
                    )
                ),
                goalMilestones = listOf(
                    GoalMilestone(id = 77L, goalId = 888L, title = "Orphan Milestone")
                ),
                dungeonCompletions = listOf(
                    DungeonCompletionRecord(
                        rewardKey = "dun_1_key",
                        instanceId = 1L,
                        dungeonId = 1L,
                        dungeonTitle = "E-Rank Gate",
                        difficulty = DungeonDifficulty.E_RANK,
                        xpAwarded = 80,
                        primaryAttribute = AttributeType.VIT,
                        primaryAttrXp = 30,
                        secondaryAttribute = AttributeType.DIS,
                        secondaryAttrXp = 15,
                        evidenceSummary = "Verified",
                        verificationProvenance = "VERIFIED",
                        completedAtEpochMs = 1_700_000_000_000L
                    )
                ),
                recentEvents = listOf(
                    SystemEvent(
                        eventType = SystemEventType.SYNC_FAILED,
                        processingStatus = EventProcessingStatus.DEAD_LETTER
                    )
                ),
                nowEpochMs = 1_750_000_000_000L
            )
        )

        assertTrue(findings.any { it.affectedTable == "player_profile" && !it.isDestructiveRepair })
        assertTrue(findings.any { it.affectedTable == "exercise_records" && it.isDestructiveRepair })
        assertTrue(findings.any { it.affectedTable == "goal_milestones" && it.isDestructiveRepair })
        assertTrue(findings.any { it.affectedTable == "rpg_reward_audit" && !it.isDestructiveRepair })
        assertTrue(findings.any { it.affectedTable == "system_events" })
    }

    @Test
    fun `SystemHealthEngine reports real CRITICAL or DEGRADED states without fabricated percentages`() {
        val now = 1_750_000_000_000L
        val criticalReport = SystemHealthEngine.evaluate(
            SystemHealthInput(
                databaseVersion = 9,
                eventDiagnostics = EventDiagnosticsSummary(totalCount = 10, processedCount = 9, deadLetterCount = 1),
                automations = listOf(
                    AutomationJobSpec("daily_reset", "Daily Reset", "desc", AutomationTriggerType.PERIODIC_WORKMANAGER, "24h", "none", currentStatus = AutomationExecutionStatus.FAILED)
                ),
                latestDailyCycle = DailyCycleExecution(20150L, "UTC", now, "APP", 3, 0, "COMPLETED", "ok"),
                expectedCurrentEpochDay = 20150L,
                syncSummary = SyncSummaryState(isCloudConfigured = false),
                intelligenceProfile = null,
                openIntegrityFindings = emptyList(),
                notificationsEnabled = true,
                nowEpochMs = now
            )
        )
        assertEquals(SystemHealthLevel.CRITICAL, criticalReport.overallStatus)
        assertEquals(8, criticalReport.components.size)
        assertTrue(criticalReport.components.none { it.evidence.contains("100%") && it.status != SystemHealthLevel.HEALTHY })
    }

    @Test
    fun `ProgressionOrchestrator blocks AI or Automation XP bypass and rejects duplicate keys`() {
        val profile = PlayerProfile()
        val aiCommand = ProgressionCommand(
            idempotencyKey = "ai-hack-1",
            sourceModule = "AI",
            sourceRecordId = "1",
            title = "Free AI XP",
            xpAwarded = 500,
            primaryAttribute = AttributeType.INT,
            primaryAttrXp = 100,
            verificationStatus = VerificationStatus.SELF_REPORTED
        )
        val blocked = ProgressionOrchestrator.applyValidatedReward(profile, aiCommand, emptySet())
        assertTrue(blocked.isFailure)
        assertTrue(blocked.exceptionOrNull() is SecurityException)

        val validCommand = aiCommand.copy(idempotencyKey = "study-session-99", sourceModule = "ACADEMIC", xpAwarded = 60)
        val applied = ProgressionOrchestrator.applyValidatedReward(profile, validCommand, emptySet())
        assertTrue(applied.isSuccess)
        assertEquals(profile.totalXp + 60L, applied.getOrThrow().updatedProfile.totalXp)

        // Replay with same idempotencyKey rejected cleanly
        val replay = ProgressionOrchestrator.applyValidatedReward(
            applied.getOrThrow().updatedProfile,
            validCommand,
            setOf("study-session-99")
        )
        assertTrue(replay.isFailure)
    }

    @Test
    fun `SystemContextAggregator enforces consent filtering and blocks physical AI proposals during rest`() {
        val restAssessment = RecoveryAssessment(
            state = RecoveryState.REST_RECOMMENDED,
            readinessScore = 20,
            measuredLoadMinutes48h = 200,
            measuredLoadMinutes7d = 400,
            measuredSessions7d = 5,
            consecutiveActiveDays = 6,
            recommendedActions = listOf("Rest"),
            summaryMessage = "Rest recommended"
        )
        val physBlocked = SystemContextAggregator.validateAiActionProposal(
            moduleTag = "PHYSICAL",
            title = "HIIT Sprints",
            durationMinutes = 30,
            recovery = restAssessment,
            hasScheduleConflict = false
        )
        assertFalse(physBlocked.allowed)

        val studyAllowed = SystemContextAggregator.validateAiActionProposal(
            moduleTag = "ACADEMIC",
            title = "Flashcard Review",
            durationMinutes = 30,
            recovery = restAssessment,
            hasScheduleConflict = false
        )
        assertTrue(studyAllowed.allowed)
        assertTrue(studyAllowed.requiresUserConfirmation)

        val filtered = SystemContextAggregator.filterTelemetryByConsent(
            input = IntelligenceTelemetryInput(
                profile = PlayerProfile(),
                recovery = restAssessment
            ),
            consent = AiConsentState(sharePhysicalWithAi = false, shareAcademicWithAi = true),
            intelPrefs = PlayerIntelligencePreferences()
        )
        assertTrue(filtered.joggingSessions.isEmpty())
    }

    @Test
    fun `NotificationOrchestrator deduplicates, prioritizes, and honors QuietHoursManager`() {
        val prefs = SystemNotificationPreferences(quietHoursStartHour = 22, quietHoursEndHour = 7)
        val quietTime = ZonedDateTime.of(2025, 3, 10, 23, 15, 0, 0, ZoneId.of("UTC")).toInstant().toEpochMilli()
        assertTrue(QuietHoursManager.isQuietNow(prefs, quietTime, ZoneId.of("UTC")))

        assertTrue(
            NotificationPriorityEngine.priorityOf(NotificationCategory.RECOVERY_WARNING) >
                NotificationPriorityEngine.priorityOf(NotificationCategory.AI_RECOMMENDATION)
        )

        val candidate = PlannedSystemNotification(
            dedupKey = "rev_1",
            category = NotificationCategory.OVERDUE_REVISION,
            title = "Revision Due",
            message = "Linear Algebra Chapter 3 due.",
            deepLinkRoute = "academic_revisions"
        )
        val filtered = NotificationDeduplicationManager.filterDuplicates(
            candidates = listOf(candidate, candidate),
            existingNotifications = emptyList(),
            nowEpochMs = quietTime
        )
        assertEquals(1, filtered.size)

        val blockedByExisting = NotificationDeduplicationManager.filterDuplicates(
            candidates = listOf(candidate),
            existingNotifications = listOf(
                SystemNotification(
                    title = "Revision Due",
                    message = "Linear Algebra Chapter 3 due.",
                    category = NotificationCategory.OVERDUE_REVISION,
                    timestampEpochMs = quietTime - 60_000L
                )
            ),
            nowEpochMs = quietTime
        )
        assertTrue(blockedByExisting.isEmpty())
    }
}
