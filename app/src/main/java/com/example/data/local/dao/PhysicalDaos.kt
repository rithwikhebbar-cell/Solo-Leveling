package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.ExerciseRecordEntity
import com.example.data.local.entity.HealthConnectSyncStateEntity
import com.example.data.local.entity.ImportedActivityEntity
import com.example.data.local.entity.JoggingSessionEntity
import com.example.data.local.entity.RecoveryAssessmentEntity
import com.example.data.local.entity.WorkoutSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JoggingSessionDao {
    @Query("SELECT * FROM jogging_sessions ORDER BY startEpochMs DESC")
    fun observeAll(): Flow<List<JoggingSessionEntity>>

    @Query("SELECT * FROM jogging_sessions WHERE startEpochMs >= :sinceEpochMs ORDER BY startEpochMs DESC")
    suspend fun getSessionsSince(sinceEpochMs: Long): List<JoggingSessionEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM jogging_sessions WHERE sourceId = :sourceId)")
    suspend fun existsBySourceId(sourceId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(session: JoggingSessionEntity): Long
}

@Dao
interface WorkoutSessionDao {
    @Query("SELECT * FROM workout_sessions ORDER BY startEpochMs DESC")
    fun observeAllWorkouts(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE startEpochMs >= :sinceEpochMs ORDER BY startEpochMs DESC")
    suspend fun getWorkoutsSince(sinceEpochMs: Long): List<WorkoutSessionEntity>

    @Query("SELECT * FROM exercise_records ORDER BY timestampEpochMs DESC")
    fun observeAllExerciseRecords(): Flow<List<ExerciseRecordEntity>>

    @Query("SELECT * FROM exercise_records")
    suspend fun getAllExerciseRecordsOnce(): List<ExerciseRecordEntity>

    @Query("DELETE FROM exercise_records WHERE id = :id")
    suspend fun deleteExerciseById(id: Long)

    @Query("SELECT * FROM exercise_records WHERE workoutSessionId = :sessionId")
    suspend fun getExercisesForSession(sessionId: Long): List<ExerciseRecordEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWorkout(session: WorkoutSessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(records: List<ExerciseRecordEntity>)
}

@Dao
interface RecoveryAssessmentDao {
    @Query("SELECT * FROM recovery_assessments ORDER BY timestampEpochMs DESC LIMIT 1")
    fun observeLatest(): Flow<RecoveryAssessmentEntity?>

    @Query("SELECT * FROM recovery_assessments ORDER BY timestampEpochMs DESC LIMIT 1")
    suspend fun getLatestOnce(): RecoveryAssessmentEntity?

    @Query("SELECT * FROM recovery_assessments ORDER BY timestampEpochMs DESC LIMIT 15")
    fun observeHistory(): Flow<List<RecoveryAssessmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(assessment: RecoveryAssessmentEntity): Long
}

@Dao
interface ImportedActivityDao {
    @Query("SELECT * FROM imported_activities ORDER BY startEpochMs DESC")
    fun observeAllImported(): Flow<List<ImportedActivityEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM imported_activities WHERE sourceId = :sourceId)")
    suspend fun existsBySourceId(sourceId: String): Boolean

    @Query(
        "SELECT EXISTS(SELECT 1 FROM imported_activities " +
            "WHERE validationOutcomeName != 'INVALID_REJECTED' " +
            "AND startEpochMs < :endEpochMs AND endEpochMs > :startEpochMs)"
    )
    suspend fun hasOverlappingSession(startEpochMs: Long, endEpochMs: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(activity: ImportedActivityEntity): Long

    @Query("SELECT * FROM health_connect_sync_state WHERE id = 1")
    fun observeSyncState(): Flow<HealthConnectSyncStateEntity?>

    @Query("SELECT * FROM health_connect_sync_state WHERE id = 1")
    suspend fun getSyncStateOnce(): HealthConnectSyncStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSyncState(state: HealthConnectSyncStateEntity)
}

@Dao
interface PhysicalAuditDao {
    @Query(
        "SELECT COALESCE(SUM(xpAwarded), 0) FROM activity_logs " +
            "WHERE categoryName = 'PHYSICAL' AND timestampEpochMs >= :dayStartEpochMs"
    )
    suspend fun getPhysicalXpEarnedSince(dayStartEpochMs: Long): Int

    @Query(
        "SELECT COUNT(*) FROM activity_logs " +
            "WHERE categoryName = 'PHYSICAL' AND verificationStatusName = 'SELF_REPORTED' " +
            "AND timestampEpochMs >= :dayStartEpochMs"
    )
    suspend fun getSelfReportedPhysicalCountSince(dayStartEpochMs: Long): Int
}
