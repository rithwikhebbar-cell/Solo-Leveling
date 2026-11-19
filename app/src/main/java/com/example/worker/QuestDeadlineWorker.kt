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
import com.example.data.repository.QuestCommitmentRepository
import com.example.data.repository.SystemEventRepository
import java.util.concurrent.TimeUnit

/**
 * Background WorkManager worker responsible for periodic evaluation of quest commitments
 * whose deadlines and grace periods expire throughout the day.
 * Re-evaluates authoritative persisted Room state to prevent race conditions or false failures.
 */
class QuestDeadlineWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return runCatching {
            val app = applicationContext as? SoloLevelingApplication
            val repo = if (app != null) {
                app.container.questCommitmentRepository
            } else {
                val db = SoloLevelingDatabase.getInstance(applicationContext)
                QuestCommitmentRepository(
                    database = db,
                    systemEventRepository = SystemEventRepository(db)
                )
            }

            val confirmedCount = repo.evaluateOverdueCommitments()
            Result.success()
        }.getOrElse {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "solo_leveling_quest_deadline_evaluation"

        fun schedule(context: Context) {
            runCatching {
                val appContext = context.applicationContext
                if (!WorkManager.isInitialized()) {
                    WorkManager.initialize(appContext, Configuration.Builder().build())
                }
                val constraints = Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()

                val request = PeriodicWorkRequestBuilder<QuestDeadlineWorker>(15, TimeUnit.MINUTES)
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
