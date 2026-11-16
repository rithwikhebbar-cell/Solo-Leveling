package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.DisciplineAssessmentEntity
import com.example.data.local.entity.GoalMilestoneEntity
import com.example.data.local.entity.GoalProgressRecordEntity
import com.example.data.local.entity.HabitCompletionEntity
import com.example.data.local.entity.HabitEntity
import com.example.data.local.entity.LifeGoalEntity
import com.example.data.local.entity.LifeProgressionRecordEntity
import com.example.data.local.entity.LifeRoutineEntity
import com.example.data.local.entity.PersonalTaskEntity
import com.example.data.local.entity.RoutineActivityEntity
import com.example.data.local.entity.RoutineCompletionEntity
import com.example.data.local.entity.ScheduledActivityReferenceEntity
import com.example.data.local.entity.TaskRecurrenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    @Query("SELECT * FROM life_routines ORDER BY startMinuteOfDay ASC, id ASC")
    fun observeAllRoutines(): Flow<List<LifeRoutineEntity>>

    @Query("SELECT * FROM life_routines ORDER BY startMinuteOfDay ASC, id ASC")
    suspend fun getAllRoutinesOnce(): List<LifeRoutineEntity>

    @Query("SELECT * FROM life_routines WHERE id = :id LIMIT 1")
    suspend fun getRoutineById(id: Long): LifeRoutineEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(entity: LifeRoutineEntity): Long

    @Update
    suspend fun updateRoutine(entity: LifeRoutineEntity)

    @Query("DELETE FROM life_routines WHERE id = :id")
    suspend fun deleteRoutine(id: Long)

    @Query("SELECT * FROM routine_activities ORDER BY routineId ASC, orderIndex ASC, id ASC")
    fun observeAllActivities(): Flow<List<RoutineActivityEntity>>

    @Query("SELECT * FROM routine_activities ORDER BY routineId ASC, orderIndex ASC, id ASC")
    suspend fun getAllActivitiesOnce(): List<RoutineActivityEntity>

    @Query("SELECT * FROM routine_activities WHERE routineId = :routineId ORDER BY orderIndex ASC, id ASC")
    suspend fun getActivitiesForRoutine(routineId: Long): List<RoutineActivityEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(entity: RoutineActivityEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivities(entities: List<RoutineActivityEntity>)

    @Update
    suspend fun updateActivities(entities: List<RoutineActivityEntity>)

    @Query("DELETE FROM routine_activities WHERE id = :activityId")
    suspend fun deleteActivity(activityId: Long)

    @Query("SELECT * FROM routine_completions WHERE dateEpochDay >= :sinceEpochDay ORDER BY dateEpochDay DESC")
    fun observeCompletionsSince(sinceEpochDay: Long): Flow<List<RoutineCompletionEntity>>

    @Query("SELECT * FROM routine_completions WHERE dateEpochDay >= :sinceEpochDay ORDER BY dateEpochDay DESC")
    suspend fun getCompletionsSinceOnce(sinceEpochDay: Long): List<RoutineCompletionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCompletion(entity: RoutineCompletionEntity): Long

    @Query("DELETE FROM routine_completions WHERE routineId = :routineId AND activityId = :activityId AND dateEpochDay = :dateEpochDay")
    suspend fun deleteCompletion(routineId: Long, activityId: Long, dateEpochDay: Long)
}

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY isArchived ASC, createdAtEpochMs DESC")
    fun observeAllHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits ORDER BY isArchived ASC, createdAtEpochMs DESC")
    suspend fun getAllHabitsOnce(): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
    suspend fun getHabitById(id: Long): HabitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(entity: HabitEntity): Long

    @Update
    suspend fun updateHabit(entity: HabitEntity)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabit(id: Long)

    @Query("SELECT * FROM habit_completions WHERE dateEpochDay >= :sinceEpochDay ORDER BY dateEpochDay DESC")
    fun observeCompletionsSince(sinceEpochDay: Long): Flow<List<HabitCompletionEntity>>

    @Query("SELECT * FROM habit_completions WHERE dateEpochDay >= :sinceEpochDay ORDER BY dateEpochDay DESC")
    suspend fun getCompletionsSinceOnce(sinceEpochDay: Long): List<HabitCompletionEntity>

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId AND dateEpochDay = :dateEpochDay LIMIT 1")
    suspend fun getCompletionForDay(habitId: Long, dateEpochDay: Long): HabitCompletionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCompletion(entity: HabitCompletionEntity): Long
}

@Dao
interface PersonalTaskDao {
    @Query("SELECT * FROM personal_tasks ORDER BY dueDateEpochDay IS NULL ASC, dueDateEpochDay ASC, createdAtEpochMs DESC")
    fun observeAllTasks(): Flow<List<PersonalTaskEntity>>

    @Query("SELECT * FROM personal_tasks ORDER BY dueDateEpochDay IS NULL ASC, dueDateEpochDay ASC, createdAtEpochMs DESC")
    suspend fun getAllTasksOnce(): List<PersonalTaskEntity>

    @Query("SELECT * FROM personal_tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Long): PersonalTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(entity: PersonalTaskEntity): Long

    @Update
    suspend fun updateTask(entity: PersonalTaskEntity)

    @Query("DELETE FROM personal_tasks WHERE id = :id")
    suspend fun deleteTask(id: Long)

    @Query("SELECT * FROM task_recurrences")
    fun observeAllRecurrences(): Flow<List<TaskRecurrenceEntity>>

    @Query("SELECT * FROM task_recurrences WHERE taskId = :taskId LIMIT 1")
    suspend fun getRecurrenceForTask(taskId: Long): TaskRecurrenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecurrence(entity: TaskRecurrenceEntity): Long

    @Query("DELETE FROM task_recurrences WHERE taskId = :taskId")
    suspend fun deleteRecurrenceForTask(taskId: Long)
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM life_goals ORDER BY targetDateEpochDay ASC, id DESC")
    fun observeAllGoals(): Flow<List<LifeGoalEntity>>

    @Query("SELECT * FROM life_goals ORDER BY targetDateEpochDay ASC, id DESC")
    suspend fun getAllGoalsOnce(): List<LifeGoalEntity>

    @Query("SELECT * FROM life_goals WHERE id = :id LIMIT 1")
    suspend fun getGoalById(id: Long): LifeGoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(entity: LifeGoalEntity): Long

    @Update
    suspend fun updateGoal(entity: LifeGoalEntity)

    @Query("DELETE FROM life_goals WHERE id = :id")
    suspend fun deleteGoal(id: Long)

    @Query("SELECT * FROM goal_milestones ORDER BY goalId ASC, orderIndex ASC, id ASC")
    fun observeAllMilestones(): Flow<List<GoalMilestoneEntity>>

    @Query("SELECT * FROM goal_milestones")
    suspend fun getAllMilestonesOnce(): List<GoalMilestoneEntity>

    @Query("SELECT * FROM goal_milestones WHERE goalId = :goalId ORDER BY orderIndex ASC, id ASC")
    suspend fun getMilestonesForGoal(goalId: Long): List<GoalMilestoneEntity>

    @Query("SELECT * FROM goal_milestones WHERE id = :milestoneId LIMIT 1")
    suspend fun getMilestoneById(milestoneId: Long): GoalMilestoneEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMilestone(entity: GoalMilestoneEntity): Long

    @Update
    suspend fun updateMilestone(entity: GoalMilestoneEntity)

    @Query("DELETE FROM goal_milestones WHERE id = :milestoneId")
    suspend fun deleteMilestone(milestoneId: Long)

    @Query("SELECT * FROM goal_progress_records ORDER BY createdAtEpochMs DESC")
    fun observeAllProgressRecords(): Flow<List<GoalProgressRecordEntity>>

    @Query("SELECT * FROM goal_progress_records WHERE goalId = :goalId ORDER BY createdAtEpochMs DESC")
    suspend fun getProgressRecordsForGoal(goalId: Long): List<GoalProgressRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgressRecord(entity: GoalProgressRecordEntity): Long
}

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM scheduled_activity_references WHERE dateEpochDay BETWEEN :startEpochDay AND :endEpochDay ORDER BY dateEpochDay ASC, startMinuteOfDay ASC")
    fun observeReferencesInRange(startEpochDay: Long, endEpochDay: Long): Flow<List<ScheduledActivityReferenceEntity>>

    @Query("SELECT * FROM scheduled_activity_references ORDER BY dateEpochDay ASC, startMinuteOfDay ASC")
    fun observeAllReferences(): Flow<List<ScheduledActivityReferenceEntity>>

    @Query("SELECT * FROM scheduled_activity_references WHERE dateEpochDay BETWEEN :startEpochDay AND :endEpochDay ORDER BY dateEpochDay ASC, startMinuteOfDay ASC")
    suspend fun getReferencesInRangeOnce(startEpochDay: Long, endEpochDay: Long): List<ScheduledActivityReferenceEntity>

    @Query("SELECT * FROM scheduled_activity_references WHERE id = :id LIMIT 1")
    suspend fun getReferenceById(id: Long): ScheduledActivityReferenceEntity?

    @Query("SELECT * FROM scheduled_activity_references WHERE sourceTypeName = :sourceTypeName AND sourceRecordId = :sourceRecordId LIMIT 1")
    suspend fun getReferenceBySource(sourceTypeName: String, sourceRecordId: Long): ScheduledActivityReferenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReference(entity: ScheduledActivityReferenceEntity): Long

    @Query("DELETE FROM scheduled_activity_references WHERE id = :id")
    suspend fun deleteReference(id: Long)

    @Query("DELETE FROM scheduled_activity_references WHERE sourceTypeName = :sourceTypeName AND sourceRecordId = :sourceRecordId")
    suspend fun deleteBySource(sourceTypeName: String, sourceRecordId: Long)
}

@Dao
interface DisciplineDao {
    @Query("SELECT * FROM discipline_assessments ORDER BY dateEpochDay DESC LIMIT :limit")
    fun observeRecentAssessments(limit: Int = 30): Flow<List<DisciplineAssessmentEntity>>

    @Query("SELECT * FROM discipline_assessments WHERE dateEpochDay = :dateEpochDay LIMIT 1")
    suspend fun getAssessmentForDay(dateEpochDay: Long): DisciplineAssessmentEntity?

    @Query("SELECT * FROM discipline_assessments ORDER BY dateEpochDay DESC LIMIT :limit")
    suspend fun getRecentAssessmentsOnce(limit: Int = 30): List<DisciplineAssessmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAssessment(entity: DisciplineAssessmentEntity): Long
}

@Dao
interface LifeProgressionDao {
    @Query("SELECT COALESCE(SUM(xpAwarded), 0) FROM life_progression_records WHERE timestampEpochMs >= :sinceEpochMs")
    suspend fun getLifeXpSince(sinceEpochMs: Long): Int

    @Query("SELECT COALESCE(SUM(xpAwarded), 0) FROM life_progression_records WHERE timestampEpochMs >= :sinceEpochMs")
    fun observeLifeXpSince(sinceEpochMs: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM life_progression_records WHERE verificationStatusName = 'SELF_REPORTED' AND timestampEpochMs >= :sinceEpochMs")
    suspend fun getSelfReportedCountSince(sinceEpochMs: Long): Int

    @Query("SELECT EXISTS(SELECT 1 FROM life_progression_records WHERE rewardKey = :rewardKey)")
    suspend fun hasRewardKey(rewardKey: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRecord(entity: LifeProgressionRecordEntity): Long
}
