package com.example.worker

import android.content.Context
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.SoloLevelingApplication
import java.util.concurrent.TimeUnit

/**
 * Battery-aware periodic background refresh worker for Milestone 8 Intelligence Engine.
 * Relies on incremental fingerprinting so unchanged telemetry short-circuits cleanly.
 */
class IntelligenceRefreshWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return runCatching {
            val app = applicationContext as? SoloLevelingApplication
            if (app != null) {
                app.container.automationOrchestrator.runJobNow(
                    automationId = com.example.data.automation.AutomationOrchestrator.JOB_INTELLIGENCE_REFRESH,
                    triggerSource = "PERIODIC_WORKMANAGER"
                )
            }
            Result.success()
        }.getOrElse {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "solo_leveling_intelligence_refresh"

        fun schedulePeriodic(context: Context) {
            runCatching {
                val appContext = context.applicationContext
                if (!WorkManager.isInitialized()) {
                    WorkManager.initialize(appContext, Configuration.Builder().build())
                }
                val constraints = Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
                val request = PeriodicWorkRequestBuilder<IntelligenceRefreshWorker>(12, TimeUnit.HOURS)
                    .setConstraints(constraints)
                    .build()
                WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
            }
        }
    }
}
