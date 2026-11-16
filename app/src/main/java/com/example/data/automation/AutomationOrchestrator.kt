package com.example.data.automation

import android.content.Context
import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.entity.toDomain
import com.example.data.local.entity.toEntity
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.IntelligenceRepository
import com.example.data.repository.SyncRepository
import com.example.data.repository.SystemEventRepository
import com.example.domain.engine.DailyCycleManager
import com.example.domain.model.AutomationExecutionRecord
import com.example.domain.model.AutomationExecutionStatus
import com.example.domain.model.AutomationJobSpec
import com.example.domain.model.AutomationTriggerType
import com.example.domain.model.DailyCycleExecution
import com.example.domain.model.SystemEventType
import com.example.domain.model.WorkoutStatus
import com.example.worker.CloudSyncWorker
import com.example.worker.DailySystemResetWorker
import com.example.worker.IntelligenceRefreshWorker
import com.example.worker.SystemNotificationWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Registry, Scheduler, Execution Tracker, Retry Manager, and Health Monitor
 * unifying all Milestone 0-9 background automations without WorkManager duplication or overlaps.
 */
class AutomationOrchestrator(
    private val context: Context,
    private val database: SoloLevelingDatabase,
    private val preferencesRepository: UserPreferencesRepository,
    private val systemEventRepository: SystemEventRepository,
    private val intelligenceRepository: IntelligenceRepository,
    private val syncRepository: SyncRepository
) {
    private val jobLocks = ConcurrentHashMap<String, Mutex>()

    private fun lockFor(automationId: String): Mutex =
        jobLocks.getOrPut(automationId) { Mutex() }

    fun observeJobs(): Flow<List<AutomationJobSpec>> =
        database.automationDao().observeJobs().map { list -> list.map { it.toDomain() } }

    fun observeExecutionLogs(limit: Int = 60): Flow<List<AutomationExecutionRecord>> =
        database.automationDao().observeExecutionLogs(limit).map { list -> list.map { it.toDomain() } }

    suspend fun ensureRegistrySeeded() {
        val existing = database.automationDao().getJobsOnce().associateBy { it.automationId }
        val defaults = defaultRegistrySpecs()
        val missingOrRefreshed = defaults.filter { it.automationId !in existing }
        if (missingOrRefreshed.isNotEmpty()) {
            database.automationDao().upsertJobs(missingOrRefreshed.map { it.toEntity() })
        }
    }

    fun defaultRegistrySpecs(): List<AutomationJobSpec> = listOf(
        AutomationJobSpec(
            automationId = JOB_DAILY_RESET,
            name = "Daily Cycle & Quest Reset",
            description = "Idempotent timezone-aware daily quest and physical recovery window transition.",
            triggerType = AutomationTriggerType.PERIODIC_WORKMANAGER,
            frequencyLabel = "Every 24h (catch-up on launch)",
            constraintsSummary = "Battery not low; epoch-day guarded"
        ),
        AutomationJobSpec(
            automationId = JOB_NOTIFICATIONS,
            name = "System Notification Evaluation",
            description = "Evaluates overdue revisions, upcoming exams, habits, routines, and safety warnings within quiet hours.",
            triggerType = AutomationTriggerType.PERIODIC_WORKMANAGER,
            frequencyLabel = "Every 4h",
            constraintsSummary = "Quiet hours & daily quota enforced"
        ),
        AutomationJobSpec(
            automationId = JOB_INTELLIGENCE_REFRESH,
            name = "Intelligence Telemetry Refresh",
            description = "Refreshes fingerprint-guarded Player Intelligence profile, adaptive plan, and reports.",
            triggerType = AutomationTriggerType.PERIODIC_WORKMANAGER,
            frequencyLabel = "Every 12h + coalesced events",
            constraintsSummary = "Battery not low; short-circuits if fingerprint unchanged"
        ),
        AutomationJobSpec(
            automationId = JOB_CLOUD_SYNC,
            name = "Cloud Incremental Synchronization",
            description = "Flushes SyncQueue & pulls delta records from Supabase when authenticated and consented.",
            triggerType = AutomationTriggerType.CONNECTIVITY_RESTORED,
            frequencyLabel = "Every 6h + network restore",
            constraintsSummary = "Connected network + Battery not low + Auth"
        )
    )

    suspend fun scheduleAllEnabledWorkers() {
        ensureRegistrySeeded()
        val sysPrefs = database.systemIntegrityHealthDao().getPreferencesOnce()?.toDomain()
        val jobs = database.automationDao().getJobsOnce().associateBy { it.automationId }

        if ((sysPrefs?.enableDailyCycleAutomation != false) && jobs[JOB_DAILY_RESET]?.isEnabled != false) {
            DailySystemResetWorker.schedule(context)
        }
        if ((sysPrefs?.enableBackgroundNotifications != false) && jobs[JOB_NOTIFICATIONS]?.isEnabled != false) {
            SystemNotificationWorker.schedule(context)
        }
        if ((sysPrefs?.enableCloudAutoSync != false) && jobs[JOB_CLOUD_SYNC]?.isEnabled != false) {
            CloudSyncWorker.schedulePeriodic(context)
        }
        if ((sysPrefs?.enableAutoIntelligenceRefresh != false) && jobs[JOB_INTELLIGENCE_REFRESH]?.isEnabled != false) {
            IntelligenceRefreshWorker.schedulePeriodic(context)
        }
    }

    suspend fun setJobEnabled(automationId: String, enabled: Boolean) {
        ensureRegistrySeeded()
        val status = if (enabled) AutomationExecutionStatus.IDLE else AutomationExecutionStatus.DISABLED
        database.automationDao().updateJobEnabled(automationId, enabled, status.name)
        if (enabled) {
            scheduleAllEnabledWorkers()
        }
    }

    suspend fun runDailyCycleIdempotent(
        triggerSource: String,
        nowEpochMs: Long = System.currentTimeMillis()
    ): Result<DailyCycleExecution?> {
        return runTrackedJob(
            automationId = JOB_DAILY_RESET,
            triggerSource = triggerSource,
            nowEpochMs = nowEpochMs
        ) {
            val userPrefs = preferencesRepository.preferencesFlow.first()
            val sysPrefs = database.systemIntegrityHealthDao().getPreferencesOnce()?.toDomain()
            val tz = sysPrefs?.preferredTimezoneId ?: java.time.ZoneId.systemDefault().id
            val latest = database.dailyCycleDao().getLatestCycleOnce()?.toDomain()

            val plan = DailyCycleManager.evaluateCyclePlan(
                nowEpochMs = nowEpochMs,
                lastExecutedEpochDay = latest?.epochDay,
                preferredTimezoneId = tz,
                dailyResetHour = userPrefs.dailyResetHour
            )

            if (!plan.shouldExecute) {
                return@runTrackedJob null to plan.reason
            }

            val dailyQuests = database.questDao().getAllQuestsOnce()
                .count { it.typeName == "DAILY" }
            database.questDao().resetDailyQuests()

            val physical = database.physicalStateDao().getPhysicalStateOnce()?.toDomain()
            if (physical != null) {
                database.physicalStateDao().upsertPhysicalState(
                    physical.copy(
                        completedJoggingMinutesToday = 0,
                        workoutStatus = WorkoutStatus.PENDING
                    ).toEntity()
                )
            }

            val execution = DailyCycleExecution(
                epochDay = plan.targetEpochDay,
                timezoneId = DailyCycleManager.resolveZone(tz).id,
                executedAtEpochMs = nowEpochMs,
                triggerSource = triggerSource,
                dailyQuestsResetCount = dailyQuests,
                missedDaysCaughtUp = plan.missedDaysCount,
                status = "COMPLETED",
                notes = plan.reason
            )
            database.dailyCycleDao().insertCycleIfAbsent(execution.toEntity())

            systemEventRepository.publishEvent(
                eventType = SystemEventType.DAILY_RESET_COMPLETED,
                relatedEntityId = execution.epochDay.toString(),
                sourceModule = "SYSTEM",
                metadata = mapOf(
                    "epochDay" to execution.epochDay.toString(),
                    "missedDays" to execution.missedDaysCaughtUp.toString(),
                    "idempotencyKey" to "daily_reset_${execution.epochDay}"
                ),
                processImmediately = false,
                nowEpochMs = nowEpochMs
            )

            execution to plan.reason
        }
    }

    suspend fun runJobNow(
        automationId: String,
        triggerSource: String = "MANUAL_USER_RUN",
        nowEpochMs: Long = System.currentTimeMillis()
    ): Result<String> {
        ensureRegistrySeeded()
        val job = database.automationDao().getJobById(automationId)?.toDomain()
            ?: return Result.failure(IllegalArgumentException("Unknown automation: $automationId"))
        if (!job.isEnabled && triggerSource != "MANUAL_USER_RUN") {
            return Result.success("Skipped ($automationId disabled by user).")
        }
        return when (automationId) {
            JOB_DAILY_RESET -> runDailyCycleIdempotent(triggerSource, nowEpochMs)
                .map { if (it == null) "Daily cycle already up-to-date (idempotent skip)." else "Daily cycle executed for epochDay=${it.epochDay}." }
            JOB_NOTIFICATIONS -> runTrackedJob(JOB_NOTIFICATIONS, triggerSource, nowEpochMs) {
                val count = SystemNotificationWorker.evaluateAndDispatch(context, database, nowEpochMs)
                "Dispatched $count alert(s)" to "Evaluated notifications: $count emitted respecting quiet hours and quota."
            }
            JOB_INTELLIGENCE_REFRESH -> runTrackedJob(JOB_INTELLIGENCE_REFRESH, triggerSource, nowEpochMs) {
                val updated = intelligenceRepository.refreshIntelligenceIfNeeded(
                    force = triggerSource == "MANUAL_USER_RUN",
                    nowEpochMs = nowEpochMs
                )
                systemEventRepository.publishEvent(
                    eventType = SystemEventType.INTELLIGENCE_REFRESHED,
                    sourceModule = "INTELLIGENCE",
                    processImmediately = false,
                    nowEpochMs = nowEpochMs
                )
                if (updated) {
                    "Refreshed" to "Intelligence telemetry refreshed cleanly."
                } else {
                    "Up-to-date (Cached)" to "Telemetry fingerprint unchanged; cached intelligence retained."
                }
            }
            JOB_CLOUD_SYNC -> runTrackedJob(JOB_CLOUD_SYNC, triggerSource, nowEpochMs) {
                if (!syncRepository.isCloudConfigured()) {
                    "Local vault" to "Cloud sync skipped: Supabase not configured (offline vault preserved)."
                } else {
                    val outcome = syncRepository.syncNow(triggerSource)
                    if (outcome.isFailure) throw outcome.exceptionOrNull() ?: IllegalStateException("Cloud sync failed")
                    val report = outcome.getOrThrow()
                    "Synced" to report.safeMessage.ifBlank { "Incremental sync finished." }
                }
            }
            else -> Result.failure(IllegalArgumentException("Unsupported automationId=$automationId"))
        }
    }

    private suspend fun <T> runTrackedJob(
        automationId: String,
        triggerSource: String,
        nowEpochMs: Long,
        block: suspend () -> Pair<T, String>
    ): Result<T> {
        ensureRegistrySeeded()
        val mutex = lockFor(automationId)
        if (mutex.isLocked) {
            return Result.failure(IllegalStateException("Automation '$automationId' is already running."))
        }
        return mutex.withLock {
            val current = database.automationDao().getJobById(automationId)?.toDomain()
                ?: defaultRegistrySpecs().first { it.automationId == automationId }
            database.automationDao().upsertJob(
                current.copy(currentStatus = AutomationExecutionStatus.RUNNING).toEntity()
            )
            val startMs = nowEpochMs
            runCatching { block() }.fold(
                onSuccess = { (value, summary) ->
                    val finishedMs = System.currentTimeMillis().coerceAtLeast(startMs)
                    val finalStatus = if (current.isEnabled) AutomationExecutionStatus.SUCCEEDED else AutomationExecutionStatus.DISABLED
                    database.automationDao().upsertJob(
                        current.copy(
                            lastExecutionEpochMs = finishedMs,
                            currentStatus = finalStatus,
                            retryCount = 0,
                            lastFailureReason = null,
                            executionCount = current.executionCount + 1
                        ).toEntity()
                    )
                    database.automationDao().insertExecutionLog(
                        AutomationExecutionRecord(
                            automationId = automationId,
                            triggerSource = triggerSource,
                            startedAtEpochMs = startMs,
                            completedAtEpochMs = finishedMs,
                            status = AutomationExecutionStatus.SUCCEEDED,
                            summaryMessage = summary,
                            retryCount = current.retryCount
                        ).toEntity()
                    )
                    Result.success(value)
                },
                onFailure = { err ->
                    val finishedMs = System.currentTimeMillis().coerceAtLeast(startMs)
                    val nextRetries = current.retryCount + 1
                    val nextStatus = if (nextRetries >= 3) {
                        AutomationExecutionStatus.FAILED
                    } else {
                        AutomationExecutionStatus.RETRY_SCHEDULED
                    }
                    val reason = (err.message ?: "Worker execution error").take(240)
                    database.automationDao().upsertJob(
                        current.copy(
                            lastExecutionEpochMs = finishedMs,
                            currentStatus = nextStatus,
                            retryCount = nextRetries,
                            lastFailureReason = reason
                        ).toEntity()
                    )
                    database.automationDao().insertExecutionLog(
                        AutomationExecutionRecord(
                            automationId = automationId,
                            triggerSource = triggerSource,
                            startedAtEpochMs = startMs,
                            completedAtEpochMs = finishedMs,
                            status = nextStatus,
                            summaryMessage = "Execution failed: $reason",
                            retryCount = nextRetries,
                            failureReason = reason
                        ).toEntity()
                    )
                    systemEventRepository.publishEvent(
                        eventType = SystemEventType.AUTOMATION_FAILED,
                        relatedEntityId = automationId,
                        sourceModule = "AUTOMATION",
                        metadata = mapOf("reason" to reason),
                        processImmediately = false,
                        nowEpochMs = finishedMs
                    )
                    Result.failure(err)
                }
            )
        }
    }

    companion object {
        const val JOB_DAILY_RESET = "daily_reset"
        const val JOB_NOTIFICATIONS = "system_notifications"
        const val JOB_INTELLIGENCE_REFRESH = "intelligence_refresh"
        const val JOB_CLOUD_SYNC = "cloud_sync"
    }
}
