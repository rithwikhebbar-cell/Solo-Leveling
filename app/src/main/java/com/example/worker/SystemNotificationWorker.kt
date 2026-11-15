package com.example.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Configuration
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.entity.toDomain
import com.example.data.local.entity.toEntity
import com.example.domain.engine.NotificationSchedulerEngine
import com.example.domain.engine.RecoveryEngine
import com.example.domain.model.PhysicalDevelopmentState
import com.example.domain.model.SystemNotification
import com.example.domain.model.SystemNotificationPreferences
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Periodic WorkManager worker that evaluates notification rules (overdue revisions, upcoming exams,
 * daily quests, workout windows, and critical recovery warnings) with quiet-hours & anti-spam rules.
 */
class SystemNotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return runCatching {
            val db = SoloLevelingDatabase.getInstance(applicationContext)
            evaluateAndDispatch(applicationContext, db)
            Result.success()
        }.getOrElse {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "solo_leveling_system_notifications"
        const val CHANNEL_ID = "solo_leveling_system_alerts"
        const val EXTRA_DEEP_LINK_ROUTE = "extra_deep_link_route"

        fun schedule(context: Context) {
            runCatching {
                val appContext = context.applicationContext
                if (!WorkManager.isInitialized()) {
                    WorkManager.initialize(appContext, Configuration.Builder().build())
                }
                val request = PeriodicWorkRequestBuilder<SystemNotificationWorker>(4, TimeUnit.HOURS)
                    .build()
                WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
            }
        }

        suspend fun evaluateAndDispatch(
            context: Context,
            db: SoloLevelingDatabase,
            nowEpochMs: Long = System.currentTimeMillis()
        ): Int {
            val prefs = db.aiConfigDao().getNotificationPreferencesOnce()?.toDomain()
                ?: SystemNotificationPreferences()
            val physical = db.physicalStateDao().getPhysicalStateOnce()?.toDomain()
                ?: PhysicalDevelopmentState()
            val recovery = db.recoveryAssessmentDao().getLatestOnce()?.toDomain()
                ?: RecoveryEngine.assess(0, 0, 0, 0)
            val pendingQuests = db.questDao().getPendingQuestsOnce().map { it.toDomain() }
            val activeSubjects = db.academicSubjectDao().getActiveSubjectsOnce().map { it.id }.toSet()
            val topics = db.academicSubjectDao().getAllTopicsOnce()
                .map { it.toDomain() }
                .filter { it.subjectId in activeSubjects }
            val exams = db.academicAssessmentDao().getAllExamsOnce().map { it.toDomain() }
            val recentNotifications = db.systemNotificationDao()
                .getNotificationsSince(nowEpochMs - 24 * 60 * 60_000L)
                .map { it.toDomain() }

            val todayEpochDay = java.time.LocalDate.now().toEpochDay()
            val habits = db.habitDao().getAllHabitsOnce().map { it.toDomain() }
            val habitCompletionsToday = db.habitDao().getCompletionsSinceOnce(todayEpochDay).map { it.toDomain() }
            val routines = db.routineDao().getAllRoutinesOnce().map { it.toDomain() }
            val tasks = db.personalTaskDao().getAllTasksOnce().map { it.toDomain() }
            val milestones = db.goalDao().observeAllMilestones().first().map { it.toDomain() }

            val planned = NotificationSchedulerEngine.evaluatePendingNotifications(
                prefs = prefs,
                physical = physical,
                recovery = recovery,
                pendingDailyQuests = pendingQuests,
                topics = topics,
                exams = exams,
                existingNotifications = recentNotifications,
                nowEpochMs = nowEpochMs,
                habits = habits,
                habitCompletionsToday = habitCompletionsToday,
                routines = routines,
                tasks = tasks,
                goalMilestones = milestones
            )

            if (planned.isEmpty()) return 0

            ensureChannel(context)
            val canPostOsNotifications = hasNotificationPermission(context)

            planned.forEachIndexed { idx, item ->
                val savedId = db.systemNotificationDao().insertNotification(
                    SystemNotification(
                        title = item.title,
                        message = item.message,
                        category = item.category,
                        deepLinkRoute = item.deepLinkRoute,
                        timestampEpochMs = nowEpochMs + idx
                    ).toEntity()
                )
                if (canPostOsNotifications) {
                    postSystemNotification(
                        context = context,
                        notificationId = (savedId.toInt().takeIf { it > 0 } ?: (1000 + idx)),
                        title = item.title,
                        message = item.message,
                        deepLinkRoute = item.deepLinkRoute
                    )
                }
            }
            return planned.size
        }

        fun hasNotificationPermission(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        }

        private fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    ?: return
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "SOLO LEVELING System Alerts",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Quest reminders, overdue revisions, exam alerts, and recovery safety warnings."
                }
                manager.createNotificationChannel(channel)
            }
        }

        private fun postSystemNotification(
            context: Context,
            notificationId: Int,
            title: String,
            message: String,
            deepLinkRoute: String
        ) {
            runCatching {
                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(EXTRA_DEEP_LINK_ROUTE, deepLinkRoute)
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    notificationId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .build()

                NotificationManagerCompat.from(context).notify(notificationId, notification)
            }
        }
    }
}
