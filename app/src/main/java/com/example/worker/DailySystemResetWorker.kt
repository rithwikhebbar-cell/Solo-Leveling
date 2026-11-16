package com.example.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.SoloLevelingApplication
import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.entity.toDomain
import com.example.data.local.entity.toEntity
import com.example.domain.engine.DailyCycleManager
import com.example.domain.model.DailyCycleExecution
import com.example.domain.model.WorkoutStatus
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Background worker scheduled via WorkManager to run the timezone-aware, idempotent daily reset.
 */
class DailySystemResetWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return runCatching {
            val app = applicationContext as? SoloLevelingApplication
            if (app != null) {
                val res = app.container.automationOrchestrator.runDailyCycleIdempotent("PERIODIC_WORKMANAGER")
                return if (res.isSuccess) Result.success() else Result.retry()
            }

            // Fallback when running isolated outside SoloLevelingApplication
            val db = SoloLevelingDatabase.getInstance(applicationContext)
            val nowMs = System.currentTimeMillis()
            val tz = db.systemIntegrityHealthDao().getPreferencesOnce()?.preferredTimezoneId ?: ZoneId.systemDefault().id
            val latest = db.dailyCycleDao().getLatestCycleOnce()?.toDomain()
            val plan = DailyCycleManager.evaluateCyclePlan(nowMs, latest?.epochDay, tz)
            if (plan.shouldExecute) {
                val dailyCount = db.questDao().getAllQuestsOnce().count { it.typeName == "DAILY" }
                db.questDao().resetDailyQuests()
                val physical = db.physicalStateDao().getPhysicalStateOnce()?.toDomain()
                if (physical != null) {
                    db.physicalStateDao().upsertPhysicalState(
                        physical.copy(
                            completedJoggingMinutesToday = 0,
                            workoutStatus = WorkoutStatus.PENDING
                        ).toEntity()
                    )
                }
                db.dailyCycleDao().insertCycleIfAbsent(
                    DailyCycleExecution(
                        epochDay = plan.targetEpochDay,
                        timezoneId = DailyCycleManager.resolveZone(tz).id,
                        executedAtEpochMs = nowMs,
                        triggerSource = "PERIODIC_WORKMANAGER_FALLBACK",
                        dailyQuestsResetCount = dailyCount,
                        missedDaysCaughtUp = plan.missedDaysCount,
                        status = "COMPLETED",
                        notes = plan.reason
                    ).toEntity()
                )
            }
            Result.success()
        }.getOrElse {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "solo_leveling_daily_system_reset"

        fun schedule(context: Context) {
            runCatching {
                val appContext = context.applicationContext
                if (!WorkManager.isInitialized()) {
                    WorkManager.initialize(appContext, Configuration.Builder().build())
                }
                val constraints = Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
                val request = PeriodicWorkRequestBuilder<DailySystemResetWorker>(24, TimeUnit.HOURS)
                    .setConstraints(constraints)
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
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
