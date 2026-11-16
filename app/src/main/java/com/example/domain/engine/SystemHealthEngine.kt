package com.example.domain.engine

import com.example.domain.model.AutomationExecutionStatus
import com.example.domain.model.AutomationJobSpec
import com.example.domain.model.DailyCycleExecution
import com.example.domain.model.EventDiagnosticsSummary
import com.example.domain.model.IntegrityFindingStatus
import com.example.domain.model.IntegritySeverity
import com.example.domain.model.PlayerIntelligenceProfile
import com.example.domain.model.SyncSummaryState
import com.example.domain.model.SystemHealthComponentStatus
import com.example.domain.model.SystemHealthLevel
import com.example.domain.model.SystemHealthReport
import com.example.domain.model.SystemIntegrityFinding

data class SystemHealthInput(
    val databaseVersion: Int,
    val eventDiagnostics: EventDiagnosticsSummary,
    val automations: List<AutomationJobSpec>,
    val latestDailyCycle: DailyCycleExecution?,
    val expectedCurrentEpochDay: Long,
    val syncSummary: SyncSummaryState,
    val intelligenceProfile: PlayerIntelligenceProfile?,
    val openIntegrityFindings: List<SystemIntegrityFinding>,
    val notificationsEnabled: Boolean,
    val nowEpochMs: Long = System.currentTimeMillis()
)

/**
 * Evaluates real component telemetry into transparent HEALTHY/WARNING/DEGRADED/CRITICAL states.
 * Never fabricates synthetic percentage health scores or hides failures.
 */
object SystemHealthEngine {

    fun evaluate(input: SystemHealthInput): SystemHealthReport {
        val now = input.nowEpochMs
        val components = mutableListOf<SystemHealthComponentStatus>()

        // 1. Database & Schema Health
        val dbHealthy = input.databaseVersion >= 9
        components.add(
            SystemHealthComponentStatus(
                component = "Database & Schema",
                status = if (dbHealthy) SystemHealthLevel.HEALTHY else SystemHealthLevel.WARNING,
                evidence = "Room DB v${input.databaseVersion} active (indexed activity_logs + v9 control tables).",
                timestampEpochMs = now,
                severity = if (dbHealthy) IntegritySeverity.INFO else IntegritySeverity.WARNING,
                recommendedAction = if (dbHealthy) "No action required." else "Complete schema migration to v9."
            )
        )

        // 2. Event Bus & Queue Health
        val ed = input.eventDiagnostics
        val eventLevel = when {
            ed.deadLetterCount > 0 -> SystemHealthLevel.CRITICAL
            ed.failedCount > 0 -> SystemHealthLevel.DEGRADED
            ed.pendingCount > 25 -> SystemHealthLevel.WARNING
            else -> SystemHealthLevel.HEALTHY
        }
        components.add(
            SystemHealthComponentStatus(
                component = "System Event Queue",
                status = eventLevel,
                evidence = "Processed=${ed.processedCount}, Pending=${ed.pendingCount}, Failed=${ed.failedCount}, DeadLetter=${ed.deadLetterCount}, Dedup=${ed.deduplicatedCount}.",
                timestampEpochMs = now,
                severity = when (eventLevel) {
                    SystemHealthLevel.CRITICAL -> IntegritySeverity.CRITICAL
                    SystemHealthLevel.DEGRADED, SystemHealthLevel.WARNING -> IntegritySeverity.WARNING
                    SystemHealthLevel.HEALTHY -> IntegritySeverity.INFO
                },
                recommendedAction = if (ed.deadLetterCount > 0 || ed.failedCount > 0) {
                    "Open Error & Recovery Center to retry failed or dead-letter events."
                } else "Event dispatch pipeline nominal."
            )
        )

        // 3. Background Automation Health
        val failedAutomations = input.automations.filter { it.currentStatus == AutomationExecutionStatus.FAILED }
        val disabledCritical = input.automations.filter { !it.isEnabled && it.automationId == "daily_reset" }
        val autoLevel = when {
            failedAutomations.isNotEmpty() -> SystemHealthLevel.DEGRADED
            disabledCritical.isNotEmpty() -> SystemHealthLevel.WARNING
            else -> SystemHealthLevel.HEALTHY
        }
        components.add(
            SystemHealthComponentStatus(
                component = "Automation Workers",
                status = autoLevel,
                evidence = "${input.automations.count { it.isEnabled }}/${input.automations.size} jobs enabled; ${failedAutomations.size} failed.",
                timestampEpochMs = now,
                severity = if (autoLevel == SystemHealthLevel.HEALTHY) IntegritySeverity.INFO else IntegritySeverity.WARNING,
                recommendedAction = if (failedAutomations.isNotEmpty()) {
                    "Inspect ${failedAutomations.joinToString { it.name }} in Automation Dashboard."
                } else "WorkManager schedules nominal."
            )
        )

        // 4. Daily Cycle Currency
        val lastDay = input.latestDailyCycle?.epochDay
        val cycleLevel = when {
            lastDay == null -> SystemHealthLevel.WARNING
            lastDay < input.expectedCurrentEpochDay -> SystemHealthLevel.WARNING
            else -> SystemHealthLevel.HEALTHY
        }
        components.add(
            SystemHealthComponentStatus(
                component = "Daily Reset Cycle",
                status = cycleLevel,
                evidence = if (lastDay == null) {
                    "No daily cycle execution recorded yet."
                } else {
                    "Last cycle epochDay=$lastDay vs current=${input.expectedCurrentEpochDay}."
                },
                timestampEpochMs = now,
                severity = if (cycleLevel == SystemHealthLevel.HEALTHY) IntegritySeverity.INFO else IntegritySeverity.WARNING,
                recommendedAction = if (cycleLevel == SystemHealthLevel.HEALTHY) {
                    "Daily cycle idempotently synchronized."
                } else "Run Daily Cycle Catch-Up in Automation Dashboard."
            )
        )

        // 5. Cloud Synchronization Health
        val sync = input.syncSummary
        val syncLevel = when {
            sync.openConflictsCount > 0 || sync.failedItemsCount > 0 -> SystemHealthLevel.DEGRADED
            !sync.isCloudConfigured -> SystemHealthLevel.WARNING
            sync.pendingUploadsCount > 10 -> SystemHealthLevel.WARNING
            else -> SystemHealthLevel.HEALTHY
        }
        components.add(
            SystemHealthComponentStatus(
                component = "Cloud Sync & Offline Queue",
                status = syncLevel,
                evidence = "State=${sync.overallState.label}, PendingUp=${sync.pendingUploadsCount}, Conflicts=${sync.openConflictsCount}, Failed=${sync.failedItemsCount}, CloudConfigured=${sync.isCloudConfigured}.",
                timestampEpochMs = now,
                severity = if (syncLevel == SystemHealthLevel.DEGRADED) IntegritySeverity.WARNING else IntegritySeverity.INFO,
                recommendedAction = when {
                    !sync.isCloudConfigured -> "Offline-first active; configure Supabase URL/Anon key in Secrets for cloud backup."
                    sync.openConflictsCount > 0 -> "Resolve conflicts in Cloud Sync Center."
                    sync.failedItemsCount > 0 -> "Retry failed sync queue items when online."
                    else -> "Cloud queue healthy."
                }
            )
        )

        // 6. Intelligence Engine Freshness
        val intelAgeHours = input.intelligenceProfile?.let { (now - it.evaluatedAtEpochMs) / 3_600_000L }
        val intelLevel = when {
            input.intelligenceProfile == null -> SystemHealthLevel.WARNING
            intelAgeHours != null && intelAgeHours >= 48L -> SystemHealthLevel.WARNING
            else -> SystemHealthLevel.HEALTHY
        }
        components.add(
            SystemHealthComponentStatus(
                component = "Intelligence Pipeline",
                status = intelLevel,
                evidence = if (input.intelligenceProfile == null) {
                    "Intelligence profile not yet evaluated."
                } else {
                    "Last evaluated ${intelAgeHours}h ago (Data completeness ${input.intelligenceProfile.dataCompletenessPercent}%)."
                },
                timestampEpochMs = now,
                severity = if (intelLevel == SystemHealthLevel.HEALTHY) IntegritySeverity.INFO else IntegritySeverity.WARNING,
                recommendedAction = if (intelLevel == SystemHealthLevel.HEALTHY) "Telemetry profile fresh." else "Trigger Intelligence Refresh."
            )
        )

        // 7. Data Integrity Findings
        val openFindings = input.openIntegrityFindings.filter { it.status == IntegrityFindingStatus.OPEN }
        val criticalFindings = openFindings.count { it.severity == IntegritySeverity.CRITICAL }
        val integrityLevel = when {
            criticalFindings > 0 -> SystemHealthLevel.CRITICAL
            openFindings.isNotEmpty() -> SystemHealthLevel.WARNING
            else -> SystemHealthLevel.HEALTHY
        }
        components.add(
            SystemHealthComponentStatus(
                component = "Data Integrity Audit",
                status = integrityLevel,
                evidence = "Open findings=${openFindings.size} ($criticalFindings critical).",
                timestampEpochMs = now,
                severity = if (criticalFindings > 0) IntegritySeverity.CRITICAL else if (openFindings.isNotEmpty()) IntegritySeverity.WARNING else IntegritySeverity.INFO,
                recommendedAction = if (openFindings.isEmpty()) "Zero cross-module anomalies." else "Inspect anomalies in Data Integrity Dashboard."
            )
        )

        // 8. Notification Delivery Status
        components.add(
            SystemHealthComponentStatus(
                component = "Notification Orchestrator",
                status = if (input.notificationsEnabled) SystemHealthLevel.HEALTHY else SystemHealthLevel.WARNING,
                evidence = if (input.notificationsEnabled) "Notifications active with quiet hours & daily quota guards." else "System notifications paused by user preference.",
                timestampEpochMs = now,
                severity = IntegritySeverity.INFO,
                recommendedAction = if (input.notificationsEnabled) "Operational." else "Re-enable in Notification Management if reminders are desired."
            )
        )

        val overall = components.maxByOrNull { levelRank(it.status) }?.status ?: SystemHealthLevel.HEALTHY

        return SystemHealthReport(
            overallStatus = overall,
            components = components,
            evaluatedAtEpochMs = now
        )
    }

    private fun levelRank(level: SystemHealthLevel): Int = when (level) {
        SystemHealthLevel.HEALTHY -> 0
        SystemHealthLevel.WARNING -> 1
        SystemHealthLevel.DEGRADED -> 2
        SystemHealthLevel.CRITICAL -> 3
    }
}
