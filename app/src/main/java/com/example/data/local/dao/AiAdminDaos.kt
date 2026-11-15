package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.AiConsentRecordEntity
import com.example.data.local.entity.AiConversationEntity
import com.example.data.local.entity.AiDailyPlanProposalEntity
import com.example.data.local.entity.AiMessageEntity
import com.example.data.local.entity.AiModelPreferenceEntity
import com.example.data.local.entity.AiQuestProposalEntity
import com.example.data.local.entity.AiRecommendationEntity
import com.example.data.local.entity.AiRequestAuditEntity
import com.example.data.local.entity.NotificationPreferenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiConversationDao {
    @Query("SELECT * FROM ai_conversations ORDER BY updatedAtEpochMs DESC")
    fun observeConversations(): Flow<List<AiConversationEntity>>

    @Query("SELECT * FROM ai_conversations ORDER BY updatedAtEpochMs DESC")
    suspend fun getConversationsOnce(): List<AiConversationEntity>

    @Query("SELECT * FROM ai_conversations WHERE id = :conversationId")
    suspend fun getConversationById(conversationId: Long): AiConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: AiConversationEntity): Long

    @Query("UPDATE ai_conversations SET updatedAtEpochMs = :updatedAtEpochMs, title = :title WHERE id = :id")
    suspend fun updateConversationMeta(id: Long, title: String, updatedAtEpochMs: Long)

    @Query("DELETE FROM ai_conversations WHERE id = :conversationId")
    suspend fun deleteConversation(conversationId: Long)

    @Query("DELETE FROM ai_conversations")
    suspend fun clearAllConversations()

    @Query("SELECT * FROM ai_messages WHERE conversationId = :conversationId ORDER BY timestampEpochMs ASC, id ASC")
    fun observeMessagesForConversation(conversationId: Long): Flow<List<AiMessageEntity>>

    @Query("SELECT * FROM ai_messages WHERE conversationId = :conversationId ORDER BY timestampEpochMs ASC, id ASC")
    suspend fun getMessagesForConversationOnce(conversationId: Long): List<AiMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: AiMessageEntity): Long
}

@Dao
interface AiProposalDao {
    @Query("SELECT * FROM ai_recommendations WHERE isDismissed = 0 ORDER BY createdAtEpochMs DESC LIMIT 20")
    fun observeActiveRecommendations(): Flow<List<AiRecommendationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecommendations(items: List<AiRecommendationEntity>)

    @Query("UPDATE ai_recommendations SET isDismissed = 1 WHERE id = :id")
    suspend fun dismissRecommendation(id: Long)

    @Query("SELECT * FROM ai_quest_proposals ORDER BY CASE WHEN statusName = 'PENDING' THEN 0 ELSE 1 END, createdAtEpochMs DESC LIMIT 40")
    fun observeQuestProposals(): Flow<List<AiQuestProposalEntity>>

    @Query("SELECT * FROM ai_quest_proposals WHERE id = :id")
    suspend fun getQuestProposalById(id: Long): AiQuestProposalEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertQuestProposals(proposals: List<AiQuestProposalEntity>)

    @Update
    suspend fun updateQuestProposal(proposal: AiQuestProposalEntity)

    @Query("UPDATE ai_quest_proposals SET statusName = :statusName WHERE id = :id")
    suspend fun updateQuestProposalStatus(id: Long, statusName: String)

    @Query("SELECT * FROM ai_daily_plan_proposals ORDER BY startMinuteOfDay ASC, id ASC")
    fun observeDailyPlanProposals(): Flow<List<AiDailyPlanProposalEntity>>

    @Query("SELECT * FROM ai_daily_plan_proposals WHERE id = :id")
    suspend fun getDailyPlanById(id: Long): AiDailyPlanProposalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyPlanItems(items: List<AiDailyPlanProposalEntity>)

    @Update
    suspend fun updateDailyPlanItem(item: AiDailyPlanProposalEntity)

    @Query("DELETE FROM ai_daily_plan_proposals WHERE planModeName = :planModeName AND statusName = 'PENDING'")
    suspend fun clearPendingPlanForMode(planModeName: String)
}

@Dao
interface AiConfigDao {
    @Query("SELECT * FROM ai_model_preferences WHERE id = 1")
    fun observeModelPreferences(): Flow<AiModelPreferenceEntity?>

    @Query("SELECT * FROM ai_model_preferences WHERE id = 1")
    suspend fun getModelPreferencesOnce(): AiModelPreferenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertModelPreferences(pref: AiModelPreferenceEntity)

    @Query("SELECT * FROM ai_consent_records WHERE id = 1")
    fun observeConsentRecord(): Flow<AiConsentRecordEntity?>

    @Query("SELECT * FROM ai_consent_records WHERE id = 1")
    suspend fun getConsentRecordOnce(): AiConsentRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConsentRecord(consent: AiConsentRecordEntity)

    @Query("SELECT * FROM notification_preferences WHERE id = 1")
    fun observeNotificationPreferences(): Flow<NotificationPreferenceEntity?>

    @Query("SELECT * FROM notification_preferences WHERE id = 1")
    suspend fun getNotificationPreferencesOnce(): NotificationPreferenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNotificationPreferences(prefs: NotificationPreferenceEntity)
}

@Dao
interface AiRequestAuditDao {
    @Query("SELECT * FROM ai_request_audit ORDER BY timestampEpochMs DESC LIMIT 30")
    fun observeRecentAudits(): Flow<List<AiRequestAuditEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudit(record: AiRequestAuditEntity): Long
}
