package com.example.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.example.data.local.SoloLevelingDatabase
import com.example.data.local.entity.toDomain
import com.example.domain.model.PhysicalDevelopmentState
import com.example.domain.model.PlayerProfile
import com.example.domain.model.QuestType
import com.example.domain.model.WidgetQuestSummary
import com.example.domain.model.WidgetSnapshot
import java.time.LocalDate
import java.time.ZoneId

/**
 * Refreshes WidgetSnapshot from authoritative Room state and notifies all 4 Jetpack Glance widgets safely.
 */
object WidgetUpdateCoordinator {

    suspend fun buildAndStoreSnapshot(
        context: Context,
        db: SoloLevelingDatabase,
        academicGoalMinutes: Int = 60,
        nowEpochMs: Long = System.currentTimeMillis()
    ): WidgetSnapshot {
        val player = db.playerDao().getPlayerOnce()?.toDomain() ?: PlayerProfile()
        val physical = db.physicalStateDao().getPhysicalStateOnce()?.toDomain() ?: PhysicalDevelopmentState()
        val quests = db.questDao().getAllQuestsOnce().map { it.toDomain() }
        val dailyQuests = quests.filter { it.type == QuestType.DAILY }
        val pendingDaily = dailyQuests.filter { !it.isCompleted }.take(3).map { q ->
            WidgetQuestSummary(
                id = q.id,
                title = q.title,
                targetLabel = "${q.currentValue}/${q.targetValue} ${q.unit}",
                isCompleted = q.isCompleted
            )
        }
        val equippedTitle = db.achievementTitleDao().getAllPlayerTitlesOnce()
            .firstOrNull { it.isEquipped }?.name ?: player.title

        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val todayStartMs = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val todayEpochDay = today.toEpochDay()

        val studyMinutesToday = db.studySessionDao().getSessionsSince(todayStartMs)
            .sumOf { it.durationMinutes }

        val habits = db.habitDao().getAllHabitsOnce()
            .filter { !it.isArchived }
        val completedHabitsToday = db.habitDao().getCompletionsSinceOnce(todayEpochDay)
            .filter { it.dateEpochDay == todayEpochDay && it.statusName == "COMPLETED" }
            .map { it.habitId }
            .toSet()

        val persistedAssessment = db.disciplineDao().getAssessmentForDay(todayEpochDay)
        val disciplineScore = persistedAssessment?.disciplineScore ?: if (habits.isEmpty()) {
            0
        } else {
            ((habits.count { it.id in completedHabitsToday } * 100) / habits.size).coerceIn(0, 100)
        }

        val snapshot = WidgetSnapshot(
            playerCodename = player.codename,
            level = player.level,
            rankName = player.rank.name,
            rankDisplayName = player.rank.displayName,
            equippedTitle = equippedTitle,
            currentLevelXp = player.currentLevelXp,
            xpToNextLevel = player.xpToNextLevel,
            totalXp = player.totalXp.toInt(),
            recoveryLabel = physical.recoveryStatus.label,
            pendingDailyQuests = pendingDaily,
            completedDailyQuests = dailyQuests.count { it.isCompleted },
            totalDailyQuests = dailyQuests.size,
            todayJoggingMinutes = physical.completedJoggingMinutesToday,
            joggingTargetMinutes = physical.effectiveJoggingTarget,
            todayStudyMinutes = studyMinutesToday,
            academicGoalMinutes = academicGoalMinutes,
            disciplineScore = disciplineScore,
            updatedAtEpochMs = nowEpochMs
        )
        WidgetSnapshotStore.saveSnapshot(context, snapshot)
        return snapshot
    }

    suspend fun refreshAllWidgets(
        context: Context,
        db: SoloLevelingDatabase,
        academicGoalMinutes: Int = 60
    ): WidgetSnapshot {
        val snapshot = buildAndStoreSnapshot(context, db, academicGoalMinutes)
        runCatching { PlayerStatusWidget().updateAll(context.applicationContext) }
        runCatching { DailyQuestWidget().updateAll(context.applicationContext) }
        runCatching { DailyProgressWidget().updateAll(context.applicationContext) }
        runCatching { QuickActionWidget().updateAll(context.applicationContext) }
        return snapshot
    }
}
