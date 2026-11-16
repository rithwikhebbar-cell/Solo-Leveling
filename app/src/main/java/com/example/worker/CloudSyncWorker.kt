package com.example.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.SoloLevelingApplication
import com.example.data.local.SoloLevelingDatabase
import java.util.concurrent.TimeUnit

/**
 * Battery-conscious, network-constrained WorkManager worker for incremental Supabase synchronization.
 * Enforces unique work, exponential backoff retries, authentication checks, and zero polling.
 */
class CloudSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? SoloLevelingApplication
        val db = SoloLevelingDatabase.getInstance(applicationContext)
        val session = db.cloudAuthDao().getSessionOnce() ?: return Result.success()
        if (!session.autoSyncEnabled || !session.cloudSyncConsentGranted) {
            return Result.success()
        }

        val syncRepo = app?.container?.syncRepository ?: return Result.success()
        if (!syncRepo.isCloudConfigured()) return Result.success()

        val trigger = inputData.getString(KEY_TRIGGER) ?: "BACKGROUND_WORKER"
        val outcome = app.container.automationOrchestrator.runJobNow(
            automationId = com.example.data.automation.AutomationOrchestrator.JOB_CLOUD_SYNC,
            triggerSource = trigger
        )
        return if (outcome.isSuccess) {
            Result.success()
        } else {
            val msg = outcome.exceptionOrNull()?.message.orEmpty()
            if (msg.contains("expired", ignoreCase = true) || msg.contains("revoked", ignoreCase = true)) {
                Result.failure()
            } else {
                Result.retry()
            }
        }
    }

    companion object {
        private const val PERIODIC_WORK_NAME = "solo_leveling_cloud_sync_periodic"
        private const val ONE_TIME_WORK_NAME = "solo_leveling_cloud_sync_immediate"
        const val KEY_TRIGGER = "trigger_source"

        private fun connectedConstraints(): Constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        fun schedulePeriodic(context: Context) {
            runCatching {
                val appContext = context.applicationContext
                if (!WorkManager.isInitialized()) {
                    WorkManager.initialize(appContext, Configuration.Builder().build())
                }
                val request = PeriodicWorkRequestBuilder<CloudSyncWorker>(6, TimeUnit.HOURS)
                    .setConstraints(connectedConstraints())
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                    .setInputData(workDataOf(KEY_TRIGGER to "PERIODIC_WORK"))
                    .build()
                WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
                    PERIODIC_WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
            }
        }

        fun enqueueImmediateSync(context: Context, triggerSource: String) {
            runCatching {
                val appContext = context.applicationContext
                if (!WorkManager.isInitialized()) {
                    WorkManager.initialize(appContext, Configuration.Builder().build())
                }
                val request = OneTimeWorkRequestBuilder<CloudSyncWorker>()
                    .setConstraints(
                        Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build()
                    )
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                    .setInputData(workDataOf(KEY_TRIGGER to triggerSource))
                    .build()
                WorkManager.getInstance(appContext).enqueueUniqueWork(
                    ONE_TIME_WORK_NAME,
                    ExistingWorkPolicy.KEEP,
                    request
                )
            }
        }
    }
}
