package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ActivityLogEntity
import com.example.data.local.entity.PhysicalStateEntity
import com.example.data.local.entity.PlayerEntity
import com.example.data.local.entity.QuestEntity
import com.example.data.local.entity.SystemNotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Query("SELECT * FROM player_profile WHERE id = 1")
    fun observePlayer(): Flow<PlayerEntity?>

    @Query("SELECT * FROM player_profile WHERE id = 1")
    suspend fun getPlayerOnce(): PlayerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlayer(player: PlayerEntity)
}

@Dao
interface QuestDao {
    @Query("SELECT * FROM quests ORDER BY isCompleted ASC, createdAtEpochMs DESC")
    fun observeAllQuests(): Flow<List<QuestEntity>>

    @Query("SELECT * FROM quests WHERE id = :questId")
    suspend fun getQuestById(questId: Long): QuestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuest(quest: QuestEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuests(quests: List<QuestEntity>)

    @Update
    suspend fun updateQuest(quest: QuestEntity)

    @Query("UPDATE quests SET isCompleted = 0, currentValue = 0, completedAtEpochMs = NULL WHERE typeName = 'DAILY'")
    suspend fun resetDailyQuests()

    @Query("SELECT * FROM quests WHERE isCompleted = 0 ORDER BY createdAtEpochMs DESC")
    suspend fun getPendingQuestsOnce(): List<QuestEntity>

    @Query("SELECT * FROM quests ORDER BY isCompleted ASC, createdAtEpochMs DESC")
    suspend fun getAllQuestsOnce(): List<QuestEntity>

    @Query("SELECT COUNT(*) FROM quests")
    suspend fun getQuestCount(): Int
}

@Dao
interface ActivityLogDao {
    @Query("SELECT * FROM activity_logs ORDER BY timestampEpochMs DESC")
    fun observeAllLogs(): Flow<List<ActivityLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ActivityLogEntity): Long
}

@Dao
interface PhysicalStateDao {
    @Query("SELECT * FROM physical_state WHERE id = 1")
    fun observePhysicalState(): Flow<PhysicalStateEntity?>

    @Query("SELECT * FROM physical_state WHERE id = 1")
    suspend fun getPhysicalStateOnce(): PhysicalStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPhysicalState(state: PhysicalStateEntity)
}

@Dao
interface SystemNotificationDao {
    @Query("SELECT * FROM system_notifications ORDER BY isRead ASC, timestampEpochMs DESC")
    fun observeNotifications(): Flow<List<SystemNotificationEntity>>

    @Query("SELECT * FROM system_notifications WHERE timestampEpochMs >= :sinceEpochMs ORDER BY timestampEpochMs DESC")
    suspend fun getNotificationsSince(sinceEpochMs: Long): List<SystemNotificationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: SystemNotificationEntity): Long

    @Query("UPDATE system_notifications SET isRead = 1 WHERE id = :id")
    suspend fun markRead(id: Long)

    @Query("DELETE FROM system_notifications WHERE isRead = 1")
    suspend fun deleteReadNotifications()
}
