package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.AcademicAssessmentEntity
import com.example.data.local.entity.AcademicChapterEntity
import com.example.data.local.entity.AcademicExamEntity
import com.example.data.local.entity.AcademicModuleEntity
import com.example.data.local.entity.AcademicProgressionRecordEntity
import com.example.data.local.entity.AcademicQuestEntity
import com.example.data.local.entity.AcademicSubjectEntity
import com.example.data.local.entity.AcademicTopicEntity
import com.example.data.local.entity.RevisionRecordEntity
import com.example.data.local.entity.StudySessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AcademicSubjectDao {
    @Query("SELECT * FROM academic_subjects ORDER BY isArchived ASC, createdAtEpochMs DESC")
    fun observeAllSubjects(): Flow<List<AcademicSubjectEntity>>

    @Query("SELECT * FROM academic_subjects WHERE id = :subjectId")
    suspend fun getSubjectById(subjectId: Long): AcademicSubjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: AcademicSubjectEntity): Long

    @Update
    suspend fun updateSubject(subject: AcademicSubjectEntity)

    @Query("SELECT * FROM academic_modules ORDER BY orderIndex ASC, id ASC")
    fun observeAllModules(): Flow<List<AcademicModuleEntity>>

    @Query("SELECT * FROM academic_modules WHERE subjectId = :subjectId ORDER BY orderIndex ASC, id ASC")
    suspend fun getModulesForSubject(subjectId: Long): List<AcademicModuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModule(module: AcademicModuleEntity): Long

    @Query("SELECT * FROM academic_chapters ORDER BY orderIndex ASC, id ASC")
    fun observeAllChapters(): Flow<List<AcademicChapterEntity>>

    @Query("SELECT * FROM academic_chapters WHERE id = :chapterId")
    suspend fun getChapterById(chapterId: Long): AcademicChapterEntity?

    @Query("SELECT * FROM academic_chapters WHERE moduleId = :moduleId ORDER BY orderIndex ASC, id ASC")
    suspend fun getChaptersForModule(moduleId: Long): List<AcademicChapterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: AcademicChapterEntity): Long

    @Update
    suspend fun updateChapter(chapter: AcademicChapterEntity)

    @Query("SELECT * FROM academic_subjects WHERE isArchived = 0 ORDER BY createdAtEpochMs DESC")
    suspend fun getActiveSubjectsOnce(): List<AcademicSubjectEntity>

    @Query("SELECT * FROM academic_subjects")
    suspend fun getAllSubjectsOnce(): List<AcademicSubjectEntity>

    @Query("SELECT * FROM academic_chapters")
    suspend fun getAllChaptersOnce(): List<AcademicChapterEntity>

    @Query("SELECT * FROM academic_topics ORDER BY id ASC")
    fun observeAllTopics(): Flow<List<AcademicTopicEntity>>

    @Query("SELECT * FROM academic_topics ORDER BY id ASC")
    suspend fun getAllTopicsOnce(): List<AcademicTopicEntity>

    @Query("SELECT * FROM academic_topics WHERE id = :topicId")
    suspend fun getTopicById(topicId: Long): AcademicTopicEntity?

    @Query("SELECT * FROM academic_topics WHERE chapterId = :chapterId ORDER BY id ASC")
    suspend fun getTopicsForChapter(chapterId: Long): List<AcademicTopicEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: AcademicTopicEntity): Long

    @Update
    suspend fun updateTopic(topic: AcademicTopicEntity)

    @Query("DELETE FROM academic_topics WHERE id = :topicId")
    suspend fun deleteTopicById(topicId: Long)
}

@Dao
interface StudySessionDao {
    @Query("SELECT * FROM study_sessions ORDER BY startEpochMs DESC")
    fun observeAllSessions(): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM study_sessions WHERE startEpochMs >= :sinceEpochMs ORDER BY startEpochMs DESC")
    suspend fun getSessionsSince(sinceEpochMs: Long): List<StudySessionEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM study_sessions WHERE sourceId = :sourceId)")
    suspend fun existsBySourceId(sourceId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSession(session: StudySessionEntity): Long
}

@Dao
interface RevisionRecordDao {
    @Query("SELECT * FROM revision_records ORDER BY revisedAtEpochMs DESC")
    fun observeAllRevisions(): Flow<List<RevisionRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRevision(record: RevisionRecordEntity): Long
}

@Dao
interface AcademicAssessmentDao {
    @Query("SELECT * FROM academic_exams ORDER BY isCompleted ASC, examDateEpochMs ASC")
    fun observeAllExams(): Flow<List<AcademicExamEntity>>

    @Query("SELECT * FROM academic_exams ORDER BY isCompleted ASC, examDateEpochMs ASC")
    suspend fun getAllExamsOnce(): List<AcademicExamEntity>

    @Query("SELECT * FROM academic_exams WHERE id = :examId")
    suspend fun getExamById(examId: Long): AcademicExamEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: AcademicExamEntity): Long

    @Update
    suspend fun updateExam(exam: AcademicExamEntity)

    @Query("SELECT * FROM academic_assessments ORDER BY timestampEpochMs DESC")
    fun observeAllAssessments(): Flow<List<AcademicAssessmentEntity>>

    @Query("SELECT * FROM academic_assessments WHERE subjectId = :subjectId ORDER BY timestampEpochMs DESC")
    suspend fun getAssessmentsForSubject(subjectId: Long): List<AcademicAssessmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssessment(assessment: AcademicAssessmentEntity): Long
}

@Dao
interface AcademicQuestDao {
    @Query("SELECT * FROM academic_quests ORDER BY statusName ASC, createdAtEpochMs DESC")
    fun observeAllAcademicQuests(): Flow<List<AcademicQuestEntity>>

    @Query("SELECT * FROM academic_quests ORDER BY statusName ASC, createdAtEpochMs DESC")
    suspend fun getAllAcademicQuestsOnce(): List<AcademicQuestEntity>

    @Query("SELECT * FROM academic_quests WHERE id = :questId")
    suspend fun getQuestById(questId: Long): AcademicQuestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuest(quest: AcademicQuestEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuests(quests: List<AcademicQuestEntity>)

    @Update
    suspend fun updateQuest(quest: AcademicQuestEntity)
}

@Dao
interface AcademicProgressionDao {
    @Query("SELECT * FROM academic_progression_records ORDER BY timestampEpochMs DESC")
    fun observeAllProgressionRecords(): Flow<List<AcademicProgressionRecordEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM academic_progression_records WHERE rewardKey = :rewardKey)")
    suspend fun existsByRewardKey(rewardKey: String): Boolean

    @Query("SELECT COALESCE(SUM(xpAwarded), 0) FROM academic_progression_records WHERE timestampEpochMs >= :dayStartEpochMs")
    suspend fun getAcademicXpEarnedSince(dayStartEpochMs: Long): Int

    @Query(
        "SELECT COUNT(*) FROM academic_progression_records " +
            "WHERE verificationStatusName = 'SELF_REPORTED' AND timestampEpochMs >= :dayStartEpochMs"
    )
    suspend fun getSelfReportedAcademicCountSince(dayStartEpochMs: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRecord(record: AcademicProgressionRecordEntity): Long
}
