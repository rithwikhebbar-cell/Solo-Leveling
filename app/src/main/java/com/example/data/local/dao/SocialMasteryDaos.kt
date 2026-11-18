package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.SocialAssessmentEntity
import com.example.data.local.entity.SocialModuleProgressEntity
import com.example.data.local.entity.SocialPracticeSessionEntity
import com.example.data.local.entity.SocialProfileEntity
import com.example.data.local.entity.SocialScenarioAttemptEntity
import com.example.data.local.entity.SocialScenarioEntity
import kotlinx.coroutines.flow.Flow

/**
 * Milestone 12: Room DAOs for Social Mastery Subsystem.
 */

@Dao
interface SocialProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: SocialProfileEntity)

    @Query("SELECT * FROM social_profiles WHERE accountOwnerId = :accountOwnerId LIMIT 1")
    suspend fun getProfile(accountOwnerId: String): SocialProfileEntity?

    @Query("SELECT * FROM social_profiles WHERE accountOwnerId = :accountOwnerId LIMIT 1")
    fun observeProfile(accountOwnerId: String): Flow<SocialProfileEntity?>

    @Query("""
        UPDATE social_profiles 
        SET totalPracticeMinutes = :practiceMinutes,
            totalScenariosCleared = :scenariosCleared,
            totalAssessmentsCompleted = :assessmentsCompleted,
            averageRubricScore = :avgScore,
            overallSocialMasteryStage = :overallStage,
            lastActiveEpochMs = :nowEpochMs,
            updatedAtEpochMs = :nowEpochMs
        WHERE accountOwnerId = :accountOwnerId
    """)
    suspend fun updateSummaryFields(
        accountOwnerId: String,
        practiceMinutes: Int,
        scenariosCleared: Int,
        assessmentsCompleted: Int,
        avgScore: Float,
        overallStage: String,
        nowEpochMs: Long
    )

    @Query("DELETE FROM social_profiles WHERE accountOwnerId = :accountOwnerId")
    suspend fun deleteProfile(accountOwnerId: String)
}

@Dao
interface SocialModuleProgressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProgress(progress: SocialModuleProgressEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(progressList: List<SocialModuleProgressEntity>)

    @Query("SELECT * FROM social_module_progress WHERE accountOwnerId = :accountOwnerId AND moduleName = :moduleName LIMIT 1")
    suspend fun getProgress(accountOwnerId: String, moduleName: String): SocialModuleProgressEntity?

    @Query("SELECT * FROM social_module_progress WHERE accountOwnerId = :accountOwnerId ORDER BY masteryStageOrdinal DESC, moduleName ASC")
    fun observeAllModuleProgress(accountOwnerId: String): Flow<List<SocialModuleProgressEntity>>

    @Query("SELECT * FROM social_module_progress WHERE accountOwnerId = :accountOwnerId")
    suspend fun getAllForOwner(accountOwnerId: String): List<SocialModuleProgressEntity>

    @Query("""
        UPDATE social_module_progress 
        SET masteryStage = :stage,
            masteryStageOrdinal = :ordinal,
            updatedAtEpochMs = :nowEpochMs
        WHERE accountOwnerId = :accountOwnerId AND moduleName = :moduleName
    """)
    suspend fun updateMasteryStage(
        accountOwnerId: String,
        moduleName: String,
        stage: String,
        ordinal: Int,
        nowEpochMs: Long
    )

    @Query("""
        UPDATE social_module_progress 
        SET practiceSeconds = practiceSeconds + :deltaSeconds,
            updatedAtEpochMs = :nowEpochMs
        WHERE accountOwnerId = :accountOwnerId AND moduleName = :moduleName
    """)
    suspend fun addPracticeSeconds(
        accountOwnerId: String,
        moduleName: String,
        deltaSeconds: Long,
        nowEpochMs: Long
    )

    @Query("DELETE FROM social_module_progress WHERE accountOwnerId = :accountOwnerId")
    suspend fun deleteAllForOwner(accountOwnerId: String)
}

@Dao
interface SocialScenarioDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateScenario(scenario: SocialScenarioEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(scenarios: List<SocialScenarioEntity>)

    @Query("SELECT * FROM social_scenarios WHERE isDeprecated = 0 ORDER BY moduleName ASC, difficultyTier ASC")
    fun observeActiveScenarios(): Flow<List<SocialScenarioEntity>>

    @Query("SELECT * FROM social_scenarios WHERE moduleName = :moduleName AND isDeprecated = 0 ORDER BY difficultyTier ASC")
    suspend fun getScenariosByModule(moduleName: String): List<SocialScenarioEntity>

    @Query("SELECT * FROM social_scenarios WHERE difficultyTier = :tier AND isDeprecated = 0")
    suspend fun getScenariosByDifficulty(tier: String): List<SocialScenarioEntity>

    @Query("SELECT * FROM social_scenarios WHERE scenarioId = :scenarioId LIMIT 1")
    suspend fun getScenarioById(scenarioId: String): SocialScenarioEntity?

    @Query("UPDATE social_scenarios SET isDeprecated = 1 WHERE scenarioId = :scenarioId")
    suspend fun deprecateScenario(scenarioId: String)

    @Query("SELECT COUNT(*) FROM social_scenarios")
    suspend fun getScenarioCount(): Int
}

@Dao
interface SocialPracticeSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SocialPracticeSessionEntity)

    @Update
    suspend fun updateSession(session: SocialPracticeSessionEntity)

    @Query("SELECT * FROM social_practice_sessions WHERE sessionId = :sessionId AND accountOwnerId = :accountOwnerId LIMIT 1")
    suspend fun getSessionById(sessionId: String, accountOwnerId: String): SocialPracticeSessionEntity?

    @Query("SELECT * FROM social_practice_sessions WHERE accountOwnerId = :accountOwnerId ORDER BY startedEpochMs DESC LIMIT :limit")
    fun observeRecentSessions(accountOwnerId: String, limit: Int = 30): Flow<List<SocialPracticeSessionEntity>>

    @Query("SELECT * FROM social_practice_sessions WHERE accountOwnerId = :accountOwnerId AND moduleName = :moduleName ORDER BY startedEpochMs DESC")
    suspend fun getSessionsByModule(accountOwnerId: String, moduleName: String): List<SocialPracticeSessionEntity>

    @Query("""
        SELECT COALESCE(SUM(durationSeconds), 0) 
        FROM social_practice_sessions 
        WHERE accountOwnerId = :accountOwnerId AND sessionStatus = 'COMPLETED'
    """)
    suspend fun getTotalCompletedPracticeSeconds(accountOwnerId: String): Long

    @Query("""
        SELECT COALESCE(SUM(durationSeconds), 0) 
        FROM social_practice_sessions 
        WHERE accountOwnerId = :accountOwnerId AND moduleName = :moduleName AND sessionStatus = 'COMPLETED'
    """)
    suspend fun getModuleCompletedPracticeSeconds(accountOwnerId: String, moduleName: String): Long

    @Query("SELECT COUNT(*) FROM social_practice_sessions WHERE idempotencyKey = :idempotencyKey")
    suspend fun countByIdempotencyKey(idempotencyKey: String): Int

    @Query("SELECT * FROM social_practice_sessions WHERE accountOwnerId = :accountOwnerId AND hasAudioRecording = 1")
    suspend fun getSessionsWithAudio(accountOwnerId: String): List<SocialPracticeSessionEntity>

    @Query("DELETE FROM social_practice_sessions WHERE sessionId = :sessionId AND accountOwnerId = :accountOwnerId")
    suspend fun deleteSession(sessionId: String, accountOwnerId: String)
}

@Dao
interface SocialScenarioAttemptDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempt(attempt: SocialScenarioAttemptEntity)

    @Query("SELECT * FROM social_scenario_attempts WHERE accountOwnerId = :accountOwnerId AND moduleName = :moduleName ORDER BY completedEpochMs DESC")
    suspend fun getAttemptsByModule(accountOwnerId: String, moduleName: String): List<SocialScenarioAttemptEntity>

    @Query("SELECT * FROM social_scenario_attempts WHERE accountOwnerId = :accountOwnerId AND scenarioId = :scenarioId ORDER BY completedEpochMs DESC")
    suspend fun getAttemptsByScenario(accountOwnerId: String, scenarioId: String): List<SocialScenarioAttemptEntity>

    @Query("SELECT * FROM social_scenario_attempts WHERE accountOwnerId = :accountOwnerId ORDER BY completedEpochMs DESC")
    fun observeAttemptsByAccount(accountOwnerId: String): Flow<List<SocialScenarioAttemptEntity>>

    @Query("SELECT evaluationScore FROM social_scenario_attempts WHERE accountOwnerId = :accountOwnerId AND moduleName = :moduleName ORDER BY completedEpochMs DESC LIMIT :limit")
    suspend fun getRecentScores(accountOwnerId: String, moduleName: String, limit: Int = 10): List<Int>

    @Query("SELECT COUNT(*) FROM social_scenario_attempts WHERE idempotencyKey = :idempotencyKey")
    suspend fun countByIdempotencyKey(idempotencyKey: String): Int

    @Query("SELECT COUNT(DISTINCT scenarioId) FROM social_scenario_attempts WHERE accountOwnerId = :accountOwnerId AND isPassed = 1")
    suspend fun getUniquePassedScenariosCount(accountOwnerId: String): Int

    @Query("SELECT COUNT(*) FROM social_scenario_attempts WHERE accountOwnerId = :accountOwnerId AND isPassed = 1 AND completedEpochMs >= :dayStartEpochMs")
    suspend fun countPassedToday(accountOwnerId: String, dayStartEpochMs: Long): Int
}

@Dao
interface SocialAssessmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssessment(assessment: SocialAssessmentEntity)

    @Query("SELECT * FROM social_assessments WHERE assessmentId = :assessmentId LIMIT 1")
    suspend fun getAssessmentById(assessmentId: String): SocialAssessmentEntity?

    @Query("SELECT * FROM social_assessments WHERE accountOwnerId = :accountOwnerId ORDER BY assessedEpochMs DESC")
    suspend fun getAssessmentHistory(accountOwnerId: String): List<SocialAssessmentEntity>

    @Query("SELECT * FROM social_assessments WHERE accountOwnerId = :accountOwnerId AND moduleName = :moduleName ORDER BY assessedEpochMs DESC LIMIT :limit")
    suspend fun getLatestAssessmentsByModule(accountOwnerId: String, moduleName: String, limit: Int = 10): List<SocialAssessmentEntity>

    @Query("SELECT * FROM social_assessments WHERE accountOwnerId = :accountOwnerId ORDER BY assessedEpochMs DESC")
    fun observeAssessments(accountOwnerId: String): Flow<List<SocialAssessmentEntity>>

    @Query("SELECT COUNT(*) FROM social_assessments WHERE accountOwnerId = :accountOwnerId")
    suspend fun getAssessmentCount(accountOwnerId: String): Int

    @Query("SELECT COALESCE(AVG(overallScore), 0.0) FROM social_assessments WHERE accountOwnerId = :accountOwnerId")
    suspend fun getAverageOverallScore(accountOwnerId: String): Float
}

@Dao
interface SocialMissionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: com.example.data.local.entity.SocialMissionRecordEntity)

    @Update
    suspend fun updateRecord(record: com.example.data.local.entity.SocialMissionRecordEntity)

    @Query("SELECT * FROM social_mission_records WHERE recordId = :recordId AND accountOwnerId = :accountOwnerId LIMIT 1")
    suspend fun getRecordById(recordId: String, accountOwnerId: String): com.example.data.local.entity.SocialMissionRecordEntity?

    @Query("SELECT * FROM social_mission_records WHERE accountOwnerId = :accountOwnerId AND lifecycleStatus IN ('ACCEPTED', 'IN_PROGRESS', 'SUBMITTED', 'UNDER_REVIEW') ORDER BY startedEpochMs DESC")
    suspend fun getActiveRecords(accountOwnerId: String): List<com.example.data.local.entity.SocialMissionRecordEntity>

    @Query("SELECT * FROM social_mission_records WHERE accountOwnerId = :accountOwnerId ORDER BY startedEpochMs DESC")
    fun observeRecords(accountOwnerId: String): Flow<List<com.example.data.local.entity.SocialMissionRecordEntity>>

    @Query("SELECT * FROM social_mission_records WHERE accountOwnerId = :accountOwnerId AND lifecycleStatus = :status ORDER BY startedEpochMs DESC")
    suspend fun getRecordsByStatus(accountOwnerId: String, status: String): List<com.example.data.local.entity.SocialMissionRecordEntity>

    @Query("SELECT COUNT(*) FROM social_mission_records WHERE idempotencyKey = :idempotencyKey")
    suspend fun countByIdempotencyKey(idempotencyKey: String): Int

    @Query("SELECT COUNT(*) FROM social_mission_records WHERE accountOwnerId = :accountOwnerId AND lifecycleStatus = 'COMPLETED' AND completedEpochMs >= :dayStartEpochMs")
    suspend fun countCompletedToday(accountOwnerId: String, dayStartEpochMs: Long): Int

    @Query("SELECT COALESCE(SUM(xpAwarded), 0) FROM social_mission_records WHERE accountOwnerId = :accountOwnerId AND completedEpochMs >= :dayStartEpochMs")
    suspend fun getSocialXpEarnedToday(accountOwnerId: String, dayStartEpochMs: Long): Int

    @Query("SELECT COUNT(*) FROM social_mission_records WHERE accountOwnerId = :accountOwnerId AND completedEpochMs >= :dayStartEpochMs AND verificationStatus = 'SELF_REPORTED'")
    suspend fun countSelfReportedCompletedToday(accountOwnerId: String, dayStartEpochMs: Long): Int

    @Query("SELECT COUNT(*) FROM social_mission_records WHERE accountOwnerId = :accountOwnerId AND lifecycleStatus = 'COMPLETED'")
    suspend fun countCompletedMissions(accountOwnerId: String): Int
}
