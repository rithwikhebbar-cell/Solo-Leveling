package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.SocialAdaptiveRecommendationEntity
import com.example.data.local.entity.SocialCompetencyProgressEntity
import com.example.data.local.entity.SocialPlannedActivityEntity
import com.example.data.local.entity.SocialWeeklyTrainingPlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SocialRecommendationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecommendations(recommendations: List<SocialAdaptiveRecommendationEntity>)

    @Update
    suspend fun updateRecommendation(recommendation: SocialAdaptiveRecommendationEntity)

    @Query("SELECT * FROM social_adaptive_recommendations WHERE accountOwnerId = :accountOwnerId AND status NOT IN ('DISMISSED', 'COMPLETED') ORDER BY createdAtEpochMs DESC")
    fun observeActiveRecommendations(accountOwnerId: String): Flow<List<SocialAdaptiveRecommendationEntity>>

    @Query("SELECT * FROM social_adaptive_recommendations WHERE accountOwnerId = :accountOwnerId AND status = :status ORDER BY createdAtEpochMs DESC")
    suspend fun getRecommendationsByStatus(accountOwnerId: String, status: String): List<SocialAdaptiveRecommendationEntity>

    @Query("SELECT * FROM social_adaptive_recommendations WHERE recommendationId = :recommendationId AND accountOwnerId = :accountOwnerId LIMIT 1")
    suspend fun getRecommendationById(recommendationId: String, accountOwnerId: String): SocialAdaptiveRecommendationEntity?

    @Query("UPDATE social_adaptive_recommendations SET status = :newStatus, updatedAtEpochMs = :nowEpochMs WHERE recommendationId = :recommendationId AND accountOwnerId = :accountOwnerId")
    suspend fun updateStatus(recommendationId: String, accountOwnerId: String, newStatus: String, nowEpochMs: Long)
}

@Dao
interface SocialTrainingPlanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: SocialWeeklyTrainingPlanEntity)

    @Update
    suspend fun updatePlan(plan: SocialWeeklyTrainingPlanEntity)

    @Query("SELECT * FROM social_weekly_training_plans WHERE accountOwnerId = :accountOwnerId ORDER BY startDateEpochMs DESC LIMIT 1")
    fun observeLatestPlan(accountOwnerId: String): Flow<SocialWeeklyTrainingPlanEntity?>

    @Query("SELECT * FROM social_weekly_training_plans WHERE accountOwnerId = :accountOwnerId ORDER BY startDateEpochMs DESC LIMIT 1")
    suspend fun getLatestPlan(accountOwnerId: String): SocialWeeklyTrainingPlanEntity?

    @Query("SELECT * FROM social_weekly_training_plans WHERE planId = :planId AND accountOwnerId = :accountOwnerId LIMIT 1")
    suspend fun getPlanById(planId: String, accountOwnerId: String): SocialWeeklyTrainingPlanEntity?

    @Query("UPDATE social_weekly_training_plans SET isPaused = :isPaused, updatedAtEpochMs = :nowEpochMs WHERE planId = :planId AND accountOwnerId = :accountOwnerId")
    suspend fun setPlanPaused(planId: String, accountOwnerId: String, isPaused: Boolean, nowEpochMs: Long)

    // Planned Activities
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivities(activities: List<SocialPlannedActivityEntity>)

    @Update
    suspend fun updateActivity(activity: SocialPlannedActivityEntity)

    @Query("SELECT * FROM social_planned_activities WHERE planId = :planId ORDER BY scheduledDateEpochMs ASC, dayOfWeek ASC")
    fun observeActivitiesForPlan(planId: String): Flow<List<SocialPlannedActivityEntity>>

    @Query("SELECT * FROM social_planned_activities WHERE planId = :planId ORDER BY scheduledDateEpochMs ASC, dayOfWeek ASC")
    suspend fun getActivitiesForPlan(planId: String): List<SocialPlannedActivityEntity>

    @Query("SELECT * FROM social_planned_activities WHERE activityId = :activityId AND accountOwnerId = :accountOwnerId LIMIT 1")
    suspend fun getActivityById(activityId: String, accountOwnerId: String): SocialPlannedActivityEntity?

    @Query("UPDATE social_planned_activities SET isCompleted = :isCompleted, isSkipped = :isSkipped, completedEpochMs = :completedEpochMs WHERE activityId = :activityId AND accountOwnerId = :accountOwnerId")
    suspend fun updateActivityCompletion(activityId: String, accountOwnerId: String, isCompleted: Boolean, isSkipped: Boolean, completedEpochMs: Long?)

    @Query("UPDATE social_planned_activities SET scheduledDateEpochMs = :newDateEpochMs, dayOfWeek = :newDayOfWeek WHERE activityId = :activityId AND accountOwnerId = :accountOwnerId")
    suspend fun rescheduleActivity(activityId: String, accountOwnerId: String, newDateEpochMs: Long, newDayOfWeek: Int)
}

@Dao
interface SocialCompetencyProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(list: List<SocialCompetencyProgressEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: SocialCompetencyProgressEntity)

    @Query("SELECT * FROM social_competency_progress WHERE accountOwnerId = :accountOwnerId ORDER BY moduleName ASC, competencyName ASC")
    fun observeAllProgress(accountOwnerId: String): Flow<List<SocialCompetencyProgressEntity>>

    @Query("SELECT * FROM social_competency_progress WHERE accountOwnerId = :accountOwnerId ORDER BY moduleName ASC, competencyName ASC")
    suspend fun getAllProgress(accountOwnerId: String): List<SocialCompetencyProgressEntity>

    @Query("SELECT * FROM social_competency_progress WHERE accountOwnerId = :accountOwnerId AND moduleName = :moduleName ORDER BY competencyName ASC")
    suspend fun getProgressForModule(accountOwnerId: String, moduleName: String): List<SocialCompetencyProgressEntity>

    @Query("SELECT * FROM social_competency_progress WHERE competencyId = :competencyId AND accountOwnerId = :accountOwnerId LIMIT 1")
    suspend fun getProgressById(competencyId: String, accountOwnerId: String): SocialCompetencyProgressEntity?

    @Query("UPDATE social_competency_progress SET recommendedDifficulty = :difficulty, updatedAtEpochMs = :nowEpochMs WHERE competencyId = :competencyId AND accountOwnerId = :accountOwnerId")
    suspend fun updateDifficultyOverride(competencyId: String, accountOwnerId: String, difficulty: String, nowEpochMs: Long)
}
