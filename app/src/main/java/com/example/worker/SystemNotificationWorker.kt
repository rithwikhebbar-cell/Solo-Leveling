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
import androidx.work.BackoffPolicy
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.SoloLevelingApplication
import com.example.data.automation.AutomationOrchestrator
import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.entity.toDomain
import com.example.data.local.entity.toEntity
import com.example.domain.engine.NotificationOrchestrator
import com.example.domain.engine.RecoveryEngine
import com.example.domain.engine.SystemAnnouncementEngine
import com.example.domain.engine.SystemNotificationChannelMapper
import com.example.domain.model.PhysicalDevelopmentState
import com.example.domain.model.SystemNotification
import com.example.domain.model.SystemNotificationChannelSpec
import com.example.domain.model.SystemNotificationPreferences
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Periodic WorkManager worker that evaluates notification rules via NotificationOrchestrator
 * with quiet-hours, deduplication, priority ranking, multi-channel routing, and deep-link actions.
 */
class SystemNotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return runCatching {
            val app = applicationContext as? SoloLevelingApplication
            if (app != null) {
                val res = app.container.automationOrchestrator.runJobNow(
                    automationId = AutomationOrchestrator.JOB_NOTIFICATIONS,
                    triggerSource = "PERIODIC_WORKMANAGER"
                )
                return if (res.isSuccess) Result.success() else Result.retry()
            }
            val db = SoloLevelingDatabase.getInstance(applicationContext)
            evaluateAndDispatch(applicationContext, db)
            Result.success()
        }.getOrElse {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "solo_leveling_system_notifications"
        const val CHANNEL_ID = "sl_system_alert"
        const val EXTRA_DEEP_LINK_ROUTE = "extra_deep_link_route"

        fun schedule(context: Context) {
            runCatching {
                val appContext = context.applicationContext
                if (!WorkManager.isInitialized()) {
                    WorkManager.initialize(appContext, Configuration.Builder().build())
                }
                val constraints = Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
                val request = PeriodicWorkRequestBuilder<SystemNotificationWorker>(4, TimeUnit.HOURS)
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

            val planned = NotificationOrchestrator.planNotifications(
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

            ensureAllChannels(context)
            val canPostOsNotifications = hasNotificationPermission(context)

            planned.forEachIndexed { idx, item ->
                val notificationModel = SystemNotification(
                    title = item.title,
                    message = item.message,
                    category = item.category,
                    deepLinkRoute = item.deepLinkRoute,
                    timestampEpochMs = nowEpochMs + idx
                )
                val savedId = db.systemNotificationDao().insertNotification(notificationModel.toEntity())
                val presentationSpec = SystemNotificationChannelMapper.resolve(
                    category = item.category,
                    existingDeepLink = item.deepLinkRoute
                )
                val announcement = SystemAnnouncementEngine.fromNotification(
                    notification = notificationModel,
                    wasDispatchedOs = canPostOsNotifications
                )
                db.systemExperienceDao().insertAnnouncement(announcement.toEntity())

                if (canPostOsNotifications) {
                    postSystemNotification(
                        context = context,
                        notificationId = (savedId.toInt().takeIf { it > 0 } ?: (1000 + idx)),
                        channelId = presentationSpec.channel.channelId,
                        priorityWeight = presentationSpec.channel.importanceWeight,
                        title = item.title,
                        message = item.message,
                        deepLinkRoute = presentationSpec.deepLinkRoute,
                        actionLabel = presentationSpec.actionLabel
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

        fun ensureAllChannels(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    ?: return
                val channels = SystemNotificationChannelSpec.entries.map { spec ->
                    val importance = when (spec.importanceWeight) {
                        4 -> NotificationManager.IMPORTANCE_HIGH
                        3 -> NotificationManager.IMPORTANCE_DEFAULT
                        else -> NotificationManager.IMPORTANCE_LOW
                    }
                    NotificationChannel(spec.channelId, spec.displayName, importance).apply {
                        description = spec.descriptionText
                    }
                }
                manager.createNotificationChannels(channels)
            }
        }

        fun dispatchDirectNotification(
            context: Context,
            notificationId: Int,
            notification: SystemNotification
        ): Boolean {
            ensureAllChannels(context)
            if (!hasNotificationPermission(context)) return false
            val spec = SystemNotificationChannelMapper.resolve(notification.category, notification.deepLinkRoute)
            postSystemNotification(
                context = context,
                notificationId = notificationId,
                channelId = spec.channel.channelId,
                priorityWeight = spec.channel.importanceWeight,
                title = notification.title,
                message = notification.message,
                deepLinkRoute = spec.deepLinkRoute,
                actionLabel = spec.actionLabel
            )
            return true
        }

        private fun postSystemNotification(
            context: Context,
            notificationId: Int,
            channelId: String,
            priorityWeight: Int,
            title: String,
            message: String,
            deepLinkRoute: String,
            actionLabel: String
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
                val compatPriority = when (priorityWeight) {
                    4 -> NotificationCompat.PRIORITY_HIGH
                    3 -> NotificationCompat.PRIORITY_DEFAULT
                    else -> NotificationCompat.PRIORITY_LOW
                }
                val notification = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                    .setContentIntent(pendingIntent)
                    .addAction(0, actionLabel, pendingIntent)
                    .setAutoCancel(true)
                    .setPriority(compatPriority)
                    .build()

                NotificationManagerCompat.from(context).notify(notificationId, notification)
            }
        }
    }
}
