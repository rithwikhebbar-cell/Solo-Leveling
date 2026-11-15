package com.example.worker

import android.content.Context
import androidx.work.Configuration
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.entity.toDomain
import com.example.data.local.entity.toEntity
import com.example.domain.model.WorkoutStatus
import java.util.concurrent.TimeUnit

/**
 * Background worker scheduled via WorkManager to reset daily quests
 * and re-initialize the player's daily physical recovery window.
 */
class DailySystemResetWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return runCatching {
            val db = SoloLevelingDatabase.getInstance(applicationContext)
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
            Result.success()
        }.getOrElse {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "solo_leveling_daily_system_reset"

        fun schedule(context: Context) {
            runCatching {
                val appContext = context.applicationContext
                if (!WorkManager.isInitialized()) {
                    WorkManager.initialize(appContext, Configuration.Builder().build())
                }
                val request = PeriodicWorkRequestBuilder<DailySystemResetWorker>(24, TimeUnit.HOURS)
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
