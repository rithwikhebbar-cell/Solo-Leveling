package com.example.domain.engine

import com.example.domain.model.AcademicChapter
import com.example.domain.model.AcademicSubject
import com.example.domain.model.AcademicTopic
import com.example.domain.model.DungeonCompletionRecord
import com.example.domain.model.EventProcessingStatus
import com.example.domain.model.ExerciseRecord
import com.example.domain.model.GoalMilestone
import com.example.domain.model.IntegrityFindingCategory
import com.example.domain.model.IntegritySeverity
import com.example.domain.model.LifeGoal
import com.example.domain.model.PlayerProfile
import com.example.domain.model.RealDeviceVerificationItem
import com.example.domain.model.RpgRewardAuditRecord
import com.example.domain.model.SyncConflict
import com.example.domain.model.SyncQueueItem
import com.example.domain.model.SystemEvent
import com.example.domain.model.SystemIntegrityFinding
import com.example.domain.model.WorkoutSession
import java.security.MessageDigest

data class SystemIntegritySnapshotInput(
    val profile: PlayerProfile,
    val workoutSessions: List<WorkoutSession> = emptyList(),
    val exerciseRecords: List<ExerciseRecord> = emptyList(),
    val subjects: List<AcademicSubject> = emptyList(),
    val chapters: List<AcademicChapter> = emptyList(),
    val topics: List<AcademicTopic> = emptyList(),
    val goals: List<LifeGoal> = emptyList(),
    val goalMilestones: List<GoalMilestone> = emptyList(),
    val dungeonCompletions: List<DungeonCompletionRecord> = emptyList(),
    val rpgRewardAudits: List<RpgRewardAuditRecord> = emptyList(),
    val syncQueueItems: List<SyncQueueItem> = emptyList(),
    val openSyncConflicts: List<SyncConflict> = emptyList(),
    val recentEvents: List<SystemEvent> = emptyList(),
    val nowEpochMs: Long = System.currentTimeMillis()
)

/**
 * Deterministic cross-module integrity checker.
 * Separates safe non-destructive repairs from confirmation-gated structural repairs
 * and never alters player XP/level/rank silently without transparent auditability.
 */
object SystemDataIntegrityEngine {

    private fun stableUuid(key: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(key.toByteArray())
        return digest.take(12).joinToString("") { "%02x".format(it) }
    }

    fun scan(input: SystemIntegritySnapshotInput): List<SystemIntegrityFinding> {
        val findings = mutableListOf<SystemIntegrityFinding>()
        val now = input.nowEpochMs
        val profile = input.profile

        // 1. Player XP & Rank Bounds
        val expectedXpToNext = ProgressionEngine.xpRequiredForLevel(profile.level)
        if (profile.currentLevelXp < 0 || profile.currentLevelXp >= expectedXpToNext || profile.xpToNextLevel != expectedXpToNext) {
            findings.add(
                SystemIntegrityFinding(
                    findingUuid = stableUuid("player_xp_bounds_${profile.level}_${profile.currentLevelXp}"),
                    category = IntegrityFindingCategory.PLAYER_PROGRESSION,
                    severity = IntegritySeverity.WARNING,
                    title = "Player Level XP Curve Mismatch",
                    description = "Current level XP (${profile.currentLevelXp}/${profile.xpToNextLevel}) disagrees with expected curve threshold ($expectedXpToNext) for Level ${profile.level}.",
                    affectedTable = "player_profile",
                    affectedRecordKey = "1",
                    repairProposal = "Recalculate xpToNextLevel cleanly from ProgressionEngine.applyXp without reducing totalXp.",
                    isDestructiveRepair = false,
                    detectedAtEpochMs = now
                )
            )
        }

        val expectedRank = ProgressionEngine.evaluateRank(
            level = profile.level,
            totalAttributeScore = profile.attributes.totalScore,
            verifiedActivitiesCount = profile.verifiedActivitiesCount
        )
        if (profile.rank != expectedRank) {
            findings.add(
                SystemIntegrityFinding(
                    findingUuid = stableUuid("player_rank_${profile.rank.name}_${expectedRank.name}"),
                    category = IntegrityFindingCategory.PLAYER_PROGRESSION,
                    severity = IntegritySeverity.INFO,
                    title = "Hunter Rank Synchronization Needed",
                    description = "Stored rank ${profile.rank.displayName} does not match evaluated eligibility (${expectedRank.displayName}).",
                    affectedTable = "player_profile",
                    affectedRecordKey = "1",
                    repairProposal = "Synchronize player rank to ${expectedRank.displayName} via ProgressionEngine.",
                    isDestructiveRepair = false,
                    detectedAtEpochMs = now
                )
            )
        }

        // 2. Physical: orphaned exercise records
        val workoutIds = input.workoutSessions.map { it.id }.toSet()
        val orphanExercises = input.exerciseRecords.filter { it.workoutSessionId !in workoutIds }
        if (orphanExercises.isNotEmpty()) {
            findings.add(
                SystemIntegrityFinding(
                    findingUuid = stableUuid("orphan_exercises_${orphanExercises.size}"),
                    category = IntegrityFindingCategory.PHYSICAL,
                    severity = IntegritySeverity.WARNING,
                    title = "Orphaned Exercise Records (${orphanExercises.size})",
                    description = "Detected ${orphanExercises.size} exercise record(s) pointing to non-existent workout sessions.",
                    affectedTable = "exercise_records",
                    affectedRecordKey = orphanExercises.take(3).joinToString(",") { it.id.toString() },
                    repairProposal = "Remove unlinked orphaned exercise rows after user confirmation.",
                    isDestructiveRepair = true,
                    detectedAtEpochMs = now
                )
            )
        }

        // 3. Academic: orphaned chapters / topics
        val subjectIds = input.subjects.map { it.id }.toSet()
        val chapterIds = input.chapters.map { it.id }.toSet()
        val orphanTopics = input.topics.filter { it.subjectId !in subjectIds || it.chapterId !in chapterIds }
        if (orphanTopics.isNotEmpty()) {
            findings.add(
                SystemIntegrityFinding(
                    findingUuid = stableUuid("orphan_topics_${orphanTopics.size}"),
                    category = IntegrityFindingCategory.ACADEMIC,
                    severity = IntegritySeverity.WARNING,
                    title = "Orphaned Academic Topics (${orphanTopics.size})",
                    description = "${orphanTopics.size} topic(s) reference missing subjects or chapters.",
                    affectedTable = "academic_topics",
                    affectedRecordKey = orphanTopics.take(3).joinToString(",") { it.id.toString() },
                    repairProposal = "Prune orphaned topic references with confirmation.",
                    isDestructiveRepair = true,
                    detectedAtEpochMs = now
                )
            )
        }

        // 4. Life: orphaned goal milestones
        val goalIds = input.goals.map { it.id }.toSet()
        val orphanMilestones = input.goalMilestones.filter { it.goalId !in goalIds }
        if (orphanMilestones.isNotEmpty()) {
            findings.add(
                SystemIntegrityFinding(
                    findingUuid = stableUuid("orphan_milestones_${orphanMilestones.size}"),
                    category = IntegrityFindingCategory.LIFE_MANAGEMENT,
                    severity = IntegritySeverity.WARNING,
                    title = "Orphaned Goal Milestones (${orphanMilestones.size})",
                    description = "${orphanMilestones.size} milestone(s) point to removed goals.",
                    affectedTable = "goal_milestones",
                    affectedRecordKey = orphanMilestones.take(3).joinToString(",") { it.id.toString() },
                    repairProposal = "Clean up orphaned milestone records with confirmation.",
                    isDestructiveRepair = true,
                    detectedAtEpochMs = now
                )
            )
        }

        // 5. RPG Audit verification
        val auditedKeys = input.rpgRewardAudits.map { it.rewardKey }.toSet()
        val unauditedDungeons = input.dungeonCompletions.filter { it.xpAwarded > 0 && it.rewardKey !in auditedKeys }
        if (unauditedDungeons.isNotEmpty()) {
            findings.add(
                SystemIntegrityFinding(
                    findingUuid = stableUuid("unaudited_dungeons_${unauditedDungeons.size}"),
                    category = IntegrityFindingCategory.RPG_AUDIT,
                    severity = IntegritySeverity.WARNING,
                    title = "Missing RPG Reward Audit Entries (${unauditedDungeons.size})",
                    description = "${unauditedDungeons.size} cleared dungeon(s) lack matching rpg_reward_audit records.",
                    affectedTable = "rpg_reward_audit",
                    affectedRecordKey = unauditedDungeons.first().rewardKey,
                    repairProposal = "Backfill read-only audit records matching cleared dungeon completions (no XP mutation).",
                    isDestructiveRepair = false,
                    detectedAtEpochMs = now
                )
            )
        }

        // 6. Cloud Sync consistency
        val dupSyncKeys = input.syncQueueItems
            .groupBy { it.idempotencyKey }
            .filter { it.key.isNotBlank() && it.value.size > 1 }
        if (dupSyncKeys.isNotEmpty()) {
            findings.add(
                SystemIntegrityFinding(
                    findingUuid = stableUuid("dup_sync_queue_${dupSyncKeys.size}"),
                    category = IntegrityFindingCategory.CLOUD_SYNC,
                    severity = IntegritySeverity.WARNING,
                    title = "Duplicate Sync Queue Entries (${dupSyncKeys.size})",
                    description = "Detected duplicate idempotency keys queued for cloud synchronization.",
                    affectedTable = "sync_queue",
                    affectedRecordKey = dupSyncKeys.keys.take(3).joinToString(","),
                    repairProposal = "Deduplicate redundant queue entries keeping the latest timestamp.",
                    isDestructiveRepair = false,
                    detectedAtEpochMs = now
                )
            )
        }
        if (input.openSyncConflicts.isNotEmpty()) {
            findings.add(
                SystemIntegrityFinding(
                    findingUuid = stableUuid("open_conflicts_${input.openSyncConflicts.size}"),
                    category = IntegrityFindingCategory.CLOUD_SYNC,
                    severity = IntegritySeverity.INFO,
                    title = "Unresolved Sync Conflicts (${input.openSyncConflicts.size})",
                    description = "${input.openSyncConflicts.size} record(s) require manual Keep Local, Keep Cloud, or Merge choice.",
                    affectedTable = "sync_conflicts",
                    affectedRecordKey = input.openSyncConflicts.first().conflictId.toString(),
                    repairProposal = "Review and resolve conflicts inside Cloud Sync Conflict Center.",
                    isDestructiveRepair = false,
                    detectedAtEpochMs = now
                )
            )
        }

        // 7. System Event Queue health
        val stuckOrDeadEvents = input.recentEvents.filter {
            it.processingStatus == EventProcessingStatus.DEAD_LETTER ||
                (it.processingStatus == EventProcessingStatus.PROCESSING && (now - it.timestampEpochMs) > 5 * 60_000L)
        }
        if (stuckOrDeadEvents.isNotEmpty()) {
            findings.add(
                SystemIntegrityFinding(
                    findingUuid = stableUuid("stuck_events_${stuckOrDeadEvents.size}"),
                    category = IntegrityFindingCategory.SYSTEM_EVENTS,
                    severity = IntegritySeverity.CRITICAL,
                    title = "Interrupted or Dead-Letter System Events (${stuckOrDeadEvents.size})",
                    description = "${stuckOrDeadEvents.size} system event(s) stopped mid-processing or exceeded retry caps.",
                    affectedTable = "system_events",
                    affectedRecordKey = stuckOrDeadEvents.first().eventId,
                    repairProposal = "Recover interrupted PROCESSING events to PENDING and requeue recoverable items.",
                    isDestructiveRepair = false,
                    detectedAtEpochMs = now
                )
            )
        }

        return findings
    }
}

object RealDeviceVerificationCatalog {
    val items: List<RealDeviceVerificationItem> = listOf(
        RealDeviceVerificationItem(1, "Health Connect Sync & Permissions", "Physical Device (Health Connect installed)", "Grant read permissions in Android Settings -> Health Connect, perform Manual Sync in Physical tab, verify distance/duration match Health Connect records."),
        RealDeviceVerificationItem(2, "GPS / Motion Jogging Tracking", "Physical Device (Outdoors GPS + Activity Recognition)", "Start live jogging tracker, run outdoors 5+ min, verify pace/distance match hardware telemetry without fabricated fallback."),
        RealDeviceVerificationItem(3, "WorkManager Doze & Background Reset", "Emulator or Physical Device", "Advance system clock past Daily Reset Hour, inspect WorkManager job execution in Automation History."),
        RealDeviceVerificationItem(4, "Reboot Survival of Periodic Workers", "Physical Device or Emulator", "Reboot device after initial setup; verify WorkManager retains periodic automation jobs upon wake."),
        RealDeviceVerificationItem(5, "Timezone Travel & DST Boundary", "Emulator / Physical Device", "Change device timezone (e.g., UTC -> America/New_York during DST transition) and verify DailyCycleManager executes once without double reset."),
        RealDeviceVerificationItem(6, "Multi-Day Missed Cycle Recovery", "Emulator or Physical Device", "Advance device date +3 days offline, open app, verify missed count logged cleanly without erasing historical quests/habits."),
        RealDeviceVerificationItem(7, "Offline Queue & Reconnect Cloud Sync", "Emulator / Physical Device + Live Supabase", "Disable Wi-Fi/Data, complete task & study session, re-enable connectivity, verify SyncQueue flushes with zero duplicates."),
        RealDeviceVerificationItem(8, "Two-Device Multi-Sync & Conflict UI", "Two Devices + Live Supabase", "Edit same task title offline on Device A and Device B, reconnect both, confirm SyncConflict appears cleanly in Conflict Center."),
        RealDeviceVerificationItem(9, "Live AI Game Master Responses", "Emulator / Physical Device + OpenRouter/Firebase AI", "Ask AI Advisor about today's recovery state while REST_RECOMMENDED; verify advice respects rest safety without adding XP."),
        RealDeviceVerificationItem(10, "Quiet Hours & Notification Quota", "Physical Device / Emulator (API 33+ POST_NOTIFICATIONS)", "Set quiet hours covering current hour, trigger evaluation; confirm non-critical alerts are suppressed while Recovery Warning remains gated cleanly."),
        RealDeviceVerificationItem(11, "App Crash Mid-Event Recovery", "Emulator / Physical Device", "Force-stop app while background event or worker is active, relaunch, check Error & Recovery Center for automatic PENDING recovery."),
        RealDeviceVerificationItem(12, "Database v8 -> v9 Upgrade Preservation", "Physical Device / Emulator with pre-existing v8 DB", "Upgrade APK over v8 installation; verify all quests, physical logs, subjects, RPG skills, and intelligence profiles survive intact."),
        RealDeviceVerificationItem(13, "Edge-to-Edge & Font Scale Accessibility", "Physical Device / Tablet", "Enable 1.3x system font scale and gesture navigation; browse all 12 Control Center screens checking 48dp targets and clipping."),
        RealDeviceVerificationItem(14, "Live Cloud Backup & Restore Integrity", "Emulator / Physical Device + Live Supabase", "Create snapshot, clear local cache or test secondary install, preview restore checksum, and apply restore.")
    )
}
