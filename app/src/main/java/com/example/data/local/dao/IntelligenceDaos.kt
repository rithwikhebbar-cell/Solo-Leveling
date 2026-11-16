package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.AdaptiveDailyPlanEntity
import com.example.data.local.entity.AdaptivePlanBlockEntity
import com.example.data.local.entity.AdvisorConsultationEntity
import com.example.data.local.entity.ConsistencyPredictionEntity
import com.example.data.local.entity.DevelopmentRoadmapEntity
import com.example.data.local.entity.IntelligencePreferencesEntity
import com.example.data.local.entity.IntelligenceProfileEntity
import com.example.data.local.entity.IntelligenceReportEntity
import com.example.data.local.entity.PersonalizedRecommendationEntity
import com.example.data.local.entity.RecommendationFeedbackEntity
import com.example.data.local.entity.RoadmapMilestoneEntity
import com.example.data.local.entity.StrengthWeaknessFindingEntity
import com.example.data.local.entity.WorkloadAssessmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IntelligenceProfileDao {
    @Query("SELECT * FROM intelligence_profiles WHERE id = 1")
    fun observeProfile(): Flow<IntelligenceProfileEntity?>

    @Query("SELECT * FROM intelligence_profiles WHERE id = 1")
    suspend fun getProfileOnce(): IntelligenceProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(entity: IntelligenceProfileEntity)

    @Query("DELETE FROM intelligence_profiles")
    suspend fun clearProfile()

    @Query("SELECT * FROM strength_weakness_findings ORDER BY priorityRank ASC, id ASC")
    fun observeFindings(): Flow<List<StrengthWeaknessFindingEntity>>

    @Query("SELECT * FROM strength_weakness_findings ORDER BY priorityRank ASC, id ASC")
    suspend fun getFindingsOnce(): List<StrengthWeaknessFindingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFindings(items: List<StrengthWeaknessFindingEntity>)

    @Query("DELETE FROM strength_weakness_findings")
    suspend fun clearFindings()

    @Query("SELECT * FROM workload_assessments ORDER BY evaluatedAtEpochMs DESC LIMIT 1")
    fun observeLatestWorkload(): Flow<WorkloadAssessmentEntity?>

    @Query("SELECT * FROM workload_assessments ORDER BY evaluatedAtEpochMs DESC LIMIT 1")
    suspend fun getLatestWorkloadOnce(): WorkloadAssessmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkload(entity: WorkloadAssessmentEntity): Long

    @Query("DELETE FROM workload_assessments")
    suspend fun clearWorkloads()

    @Query("SELECT * FROM consistency_predictions ORDER BY horizonDays ASC, id ASC")
    fun observePredictions(): Flow<List<ConsistencyPredictionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPredictions(items: List<ConsistencyPredictionEntity>)

    @Query("DELETE FROM consistency_predictions")
    suspend fun clearPredictions()
}

@Dao
interface AdaptivePlanDao {
    @Query("SELECT * FROM adaptive_daily_plans ORDER BY targetDateEpochDay DESC, generatedAtEpochMs DESC LIMIT 1")
    fun observeLatestPlan(): Flow<AdaptiveDailyPlanEntity?>

    @Query("SELECT * FROM adaptive_daily_plans WHERE targetDateEpochDay = :dateEpochDay ORDER BY generatedAtEpochMs DESC LIMIT 1")
    suspend fun getPlanForDayOnce(dateEpochDay: Long): AdaptiveDailyPlanEntity?

    @Query("SELECT * FROM adaptive_daily_plans WHERE id = :planId LIMIT 1")
    suspend fun getPlanById(planId: Long): AdaptiveDailyPlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: AdaptiveDailyPlanEntity): Long

    @Update
    suspend fun updatePlan(plan: AdaptiveDailyPlanEntity)

    @Query("DELETE FROM adaptive_daily_plans")
    suspend fun clearAllPlans()

    @Query("SELECT * FROM adaptive_plan_blocks ORDER BY startMinuteOfDay ASC, id ASC")
    fun observeAllBlocks(): Flow<List<AdaptivePlanBlockEntity>>

    @Query("SELECT * FROM adaptive_plan_blocks WHERE planUuid = :planUuid ORDER BY startMinuteOfDay ASC, id ASC")
    suspend fun getBlocksForPlanOnce(planUuid: String): List<AdaptivePlanBlockEntity>

    @Query("SELECT * FROM adaptive_plan_blocks WHERE id = :blockId LIMIT 1")
    suspend fun getBlockById(blockId: Long): AdaptivePlanBlockEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlocks(blocks: List<AdaptivePlanBlockEntity>)

    @Update
    suspend fun updateBlock(block: AdaptivePlanBlockEntity)
}

@Dao
interface RecommendationDao {
    @Query("SELECT * FROM personalized_recommendations ORDER BY CASE WHEN statusName = 'NEW' THEN 0 WHEN statusName = 'ACCEPTED' THEN 1 ELSE 2 END, priorityScore DESC, createdAtEpochMs DESC")
    fun observeAllRecommendations(): Flow<List<PersonalizedRecommendationEntity>>

    @Query("SELECT * FROM personalized_recommendations")
    suspend fun getAllRecommendationsOnce(): List<PersonalizedRecommendationEntity>

    @Query("SELECT * FROM personalized_recommendations WHERE recUuid = :recUuid LIMIT 1")
    suspend fun getByUuid(recUuid: String): PersonalizedRecommendationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecommendations(items: List<PersonalizedRecommendationEntity>)

    @Update
    suspend fun updateRecommendation(item: PersonalizedRecommendationEntity)

    @Query("UPDATE personalized_recommendations SET statusName = 'EXPIRED' WHERE statusName = 'NEW' AND expiresAtEpochMs <= :nowEpochMs")
    suspend fun expireStaleRecommendations(nowEpochMs: Long)

    @Query("DELETE FROM personalized_recommendations")
    suspend fun clearRecommendations()

    @Query("SELECT * FROM recommendation_feedback ORDER BY timestampEpochMs DESC")
    fun observeFeedbackHistory(): Flow<List<RecommendationFeedbackEntity>>

    @Query("SELECT * FROM recommendation_feedback ORDER BY timestampEpochMs DESC")
    suspend fun getFeedbackHistoryOnce(): List<RecommendationFeedbackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedback(entity: RecommendationFeedbackEntity): Long

    @Query("DELETE FROM recommendation_feedback")
    suspend fun clearFeedback()
}

@Dao
interface RoadmapDao {
    @Query("SELECT * FROM development_roadmaps ORDER BY updatedAtEpochMs DESC")
    fun observeAllRoadmaps(): Flow<List<DevelopmentRoadmapEntity>>

    @Query("SELECT * FROM development_roadmaps WHERE roadmapUuid = :roadmapUuid LIMIT 1")
    suspend fun getRoadmapByUuid(roadmapUuid: String): DevelopmentRoadmapEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoadmap(entity: DevelopmentRoadmapEntity): Long

    @Update
    suspend fun updateRoadmap(entity: DevelopmentRoadmapEntity)

    @Query("DELETE FROM development_roadmaps")
    suspend fun clearRoadmaps()

    @Query("SELECT * FROM roadmap_milestones ORDER BY orderIndex ASC, id ASC")
    fun observeAllMilestones(): Flow<List<RoadmapMilestoneEntity>>

    @Query("SELECT * FROM roadmap_milestones WHERE milestoneUuid = :milestoneUuid LIMIT 1")
    suspend fun getMilestoneByUuid(milestoneUuid: String): RoadmapMilestoneEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMilestones(items: List<RoadmapMilestoneEntity>)

    @Update
    suspend fun updateMilestone(entity: RoadmapMilestoneEntity)
}

@Dao
interface IntelligenceReportDao {
    @Query("SELECT * FROM intelligence_reports ORDER BY generatedAtEpochMs DESC")
    fun observeAllReports(): Flow<List<IntelligenceReportEntity>>

    @Query("SELECT * FROM intelligence_reports WHERE periodTypeName = :periodTypeName ORDER BY generatedAtEpochMs DESC LIMIT 1")
    suspend fun getLatestByTypeOnce(periodTypeName: String): IntelligenceReportEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(entity: IntelligenceReportEntity): Long

    @Query("DELETE FROM intelligence_reports")
    suspend fun clearReports()

    @Query("SELECT * FROM intelligence_preferences WHERE id = 1")
    fun observePreferences(): Flow<IntelligencePreferencesEntity?>

    @Query("SELECT * FROM intelligence_preferences WHERE id = 1")
    suspend fun getPreferencesOnce(): IntelligencePreferencesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPreferences(entity: IntelligencePreferencesEntity)

    @Query("SELECT * FROM advisor_consultations ORDER BY timestampEpochMs DESC LIMIT 30")
    fun observeConsultations(): Flow<List<AdvisorConsultationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConsultation(entity: AdvisorConsultationEntity): Long

    @Query("DELETE FROM advisor_consultations")
    suspend fun clearConsultations()
}
