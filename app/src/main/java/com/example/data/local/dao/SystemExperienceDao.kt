package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.SystemAnnouncementEntity
import com.example.data.local.entity.SystemCelebrationEntity
import com.example.data.local.entity.SystemExperiencePreferenceEntity
import com.example.data.local.entity.VoiceCommandHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SystemExperienceDao {

    @Query("SELECT * FROM system_experience_preferences WHERE id = 1")
    fun observePreferences(): Flow<SystemExperiencePreferenceEntity?>

    @Query("SELECT * FROM system_experience_preferences WHERE id = 1")
    suspend fun getPreferencesOnce(): SystemExperiencePreferenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPreferences(entity: SystemExperiencePreferenceEntity)

    @Query("SELECT * FROM system_announcement_history ORDER BY createdAtEpochMs DESC LIMIT :limit")
    fun observeAnnouncements(limit: Int = 60): Flow<List<SystemAnnouncementEntity>>

    @Query("SELECT announcementKey FROM system_announcement_history")
    suspend fun getAllAnnouncementKeysOnce(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAnnouncement(entity: SystemAnnouncementEntity): Long

    @Query("UPDATE system_announcement_history SET isRead = 1 WHERE id = :id")
    suspend fun markAnnouncementRead(id: Long)

    @Query("UPDATE system_announcement_history SET isRead = 1")
    suspend fun markAllAnnouncementsRead()

    @Query("SELECT * FROM system_celebration_history ORDER BY createdAtEpochMs DESC LIMIT :limit")
    fun observeCelebrations(limit: Int = 50): Flow<List<SystemCelebrationEntity>>

    @Query("SELECT * FROM system_celebration_history WHERE isAcknowledged = 0 ORDER BY createdAtEpochMs ASC")
    fun observeUnacknowledgedCelebrations(): Flow<List<SystemCelebrationEntity>>

    @Query("SELECT celebrationId FROM system_celebration_history")
    suspend fun getAllCelebrationIdsOnce(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCelebration(entity: SystemCelebrationEntity): Long

    @Query("UPDATE system_celebration_history SET isAcknowledged = 1 WHERE celebrationId = :celebrationId")
    suspend fun acknowledgeCelebration(celebrationId: String)

    @Query("SELECT * FROM voice_command_history ORDER BY executedAtEpochMs DESC LIMIT :limit")
    fun observeVoiceHistory(limit: Int = 30): Flow<List<VoiceCommandHistoryEntity>>

    @Insert
    suspend fun insertVoiceHistory(entity: VoiceCommandHistoryEntity): Long

    @Query("DELETE FROM voice_command_history")
    suspend fun clearVoiceHistory()
}
