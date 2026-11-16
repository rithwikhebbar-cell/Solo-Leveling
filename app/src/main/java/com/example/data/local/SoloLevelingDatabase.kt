package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.SocialAssessmentDao
import com.example.data.local.dao.SocialMissionDao
import com.example.data.local.dao.SocialModuleProgressDao
import com.example.data.local.dao.SocialPracticeSessionDao
import com.example.data.local.dao.SocialProfileDao
import com.example.data.local.dao.SocialScenarioAttemptDao
import com.example.data.local.dao.SocialScenarioDao
import com.example.data.local.entity.SocialAssessmentEntity
import com.example.data.local.entity.SocialMissionRecordEntity
import com.example.data.local.entity.SocialModuleProgressEntity
import com.example.data.local.entity.SocialPracticeSessionEntity
import com.example.data.local.entity.SocialProfileEntity
import com.example.data.local.entity.SocialScenarioAttemptEntity
import com.example.data.local.entity.SocialScenarioEntity
import com.example.data.local.dao.AcademicAssessmentDao
import com.example.data.local.dao.AcademicProgressionDao
import com.example.data.local.dao.AcademicQuestDao
import com.example.data.local.dao.AcademicSubjectDao
import com.example.data.local.dao.AchievementTitleDao
import com.example.data.local.dao.ActivityLogDao
import com.example.data.local.dao.AiConfigDao
import com.example.data.local.dao.AiConversationDao
import com.example.data.local.dao.AiProposalDao
import com.example.data.local.dao.AiRequestAuditDao
import com.example.data.local.dao.AutomationDao
import com.example.data.local.dao.BossBattleDao
import com.example.data.local.dao.CloudAuthDao
import com.example.data.local.dao.CloudBackupDao
import com.example.data.local.dao.DailyCycleDao
import com.example.data.local.dao.DisciplineDao
import com.example.data.local.dao.DungeonDao
import com.example.data.local.dao.GoalDao
import com.example.data.local.dao.HabitDao
import com.example.data.local.dao.AdaptivePlanDao
import com.example.data.local.dao.ImportedActivityDao
import com.example.data.local.dao.IntelligenceProfileDao
import com.example.data.local.dao.IntelligenceReportDao
import com.example.data.local.dao.InventoryShadowDao
import com.example.data.local.dao.JoggingSessionDao
import com.example.data.local.dao.LifeProgressionDao
import com.example.data.local.dao.MonarchSkillUnlockDao
import com.example.data.local.dao.PersonalTaskDao
import com.example.data.local.dao.PhysicalAuditDao
import com.example.data.local.dao.PhysicalStateDao
import com.example.data.local.dao.PlayerDao
import com.example.data.local.dao.QuestDao
import com.example.data.local.dao.RecommendationDao
import com.example.data.local.dao.RecoveryAssessmentDao
import com.example.data.local.dao.RegisteredDeviceDao
import com.example.data.local.dao.RevisionRecordDao
import com.example.data.local.dao.RoadmapDao
import com.example.data.local.dao.RoutineDao
import com.example.data.local.dao.RpgAuditDao
import com.example.data.local.dao.ScheduleDao
import com.example.data.local.dao.ShadowDao
import com.example.data.local.dao.ShadowDeploymentDao
import com.example.data.local.dao.ShadowExtractionDao
import com.example.data.local.dao.ShadowFormationDao
import com.example.data.local.dao.ShadowMonarchProfileDao
import com.example.data.local.dao.ShadowQuestProgressDao
import com.example.data.local.dao.ShadowResourceDao
import com.example.data.local.dao.ShadowSkillUnlockDao
import com.example.data.local.dao.SkillDao
import com.example.data.local.dao.StudySessionDao
import com.example.data.local.dao.SyncConflictDao
import com.example.data.local.dao.SyncDiagnosticsDao
import com.example.data.local.dao.SyncMetaDao
import com.example.data.local.dao.SyncQueueDao
import com.example.data.local.dao.SystemEventDao
import com.example.data.local.dao.SystemExperienceDao
import com.example.data.local.dao.SystemIntegrityHealthDao
import com.example.data.local.dao.SystemNotificationDao
import com.example.data.local.dao.WorkoutSessionDao
import com.example.data.local.entity.AdaptiveDailyPlanEntity
import com.example.data.local.entity.AdaptivePlanBlockEntity
import com.example.data.local.entity.AdvisorConsultationEntity
import com.example.data.local.entity.AcademicAssessmentEntity
import com.example.data.local.entity.MonarchSkillUnlockEntity
import com.example.data.local.entity.ShadowDeploymentEntity
import com.example.data.local.entity.ShadowEntity
import com.example.data.local.entity.ShadowExtractionEntity
import com.example.data.local.entity.ShadowFormationEntity
import com.example.data.local.entity.ShadowMonarchProfileEntity
import com.example.data.local.entity.ShadowQuestProgressEntity
import com.example.data.local.entity.ShadowResourceTransactionEntity
import com.example.data.local.entity.ShadowSkillUnlockEntity
import com.example.data.local.entity.AcademicChapterEntity
import com.example.data.local.entity.AcademicExamEntity
import com.example.data.local.entity.AcademicModuleEntity
import com.example.data.local.entity.AcademicProgressionRecordEntity
import com.example.data.local.entity.AcademicQuestEntity
import com.example.data.local.entity.AcademicSubjectEntity
import com.example.data.local.entity.AcademicTopicEntity
import com.example.data.local.entity.AchievementDefinitionEntity
import com.example.data.local.entity.AchievementUnlockRecordEntity
import com.example.data.local.entity.ActivityLogEntity
import com.example.data.local.entity.AiConsentRecordEntity
import com.example.data.local.entity.AiConversationEntity
import com.example.data.local.entity.AiDailyPlanProposalEntity
import com.example.data.local.entity.AiDungeonProposalEntity
import com.example.data.local.entity.AiMessageEntity
import com.example.data.local.entity.AiModelPreferenceEntity
import com.example.data.local.entity.AiQuestProposalEntity
import com.example.data.local.entity.AiRecommendationEntity
import com.example.data.local.entity.AiRequestAuditEntity
import com.example.data.local.entity.BossBattleInstanceEntity
import com.example.data.local.entity.BossBattlePhaseEntity
import com.example.data.local.entity.BossDefinitionEntity
import com.example.data.local.entity.CloudAuthSessionEntity
import com.example.data.local.entity.CloudBackupEntity
import com.example.data.local.entity.CloudSyncCursorEntity
import com.example.data.local.entity.ConsistencyPredictionEntity
import com.example.data.local.entity.DevelopmentRoadmapEntity
import com.example.data.local.entity.DisciplineAssessmentEntity
import com.example.data.local.entity.DungeonCompletionRecordEntity
import com.example.data.local.entity.DungeonDefinitionEntity
import com.example.data.local.entity.DungeonInstanceEntity
import com.example.data.local.entity.DungeonObjectiveEntity
import com.example.data.local.entity.ExerciseRecordEntity
import com.example.data.local.entity.GoalMilestoneEntity
import com.example.data.local.entity.GoalProgressRecordEntity
import com.example.data.local.entity.HabitCompletionEntity
import com.example.data.local.entity.HabitEntity
import com.example.data.local.entity.HealthConnectSyncStateEntity
import com.example.data.local.entity.ImportedActivityEntity
import com.example.data.local.entity.IntelligencePreferencesEntity
import com.example.data.local.entity.IntelligenceProfileEntity
import com.example.data.local.entity.IntelligenceReportEntity
import com.example.data.local.entity.InventoryItemEntity
import com.example.data.local.entity.JoggingSessionEntity
import com.example.data.local.entity.LifeGoalEntity
import com.example.data.local.entity.LifeProgressionRecordEntity
import com.example.data.local.entity.LifeRoutineEntity
import com.example.data.local.entity.NotificationPreferenceEntity
import com.example.data.local.entity.PersonalTaskEntity
import com.example.data.local.entity.PersonalizedRecommendationEntity
import com.example.data.local.entity.PhysicalStateEntity
import com.example.data.local.entity.PlayerEntity
import com.example.data.local.entity.PlayerInventoryRecordEntity
import com.example.data.local.entity.PlayerTitleEntity
import com.example.data.local.entity.QuestEntity
import com.example.data.local.entity.RecommendationFeedbackEntity
import com.example.data.local.entity.RecoveryAssessmentEntity
import com.example.data.local.entity.RegisteredDeviceEntity
import com.example.data.local.entity.RevisionRecordEntity
import com.example.data.local.entity.RoadmapMilestoneEntity
import com.example.data.local.entity.RoutineActivityEntity
import com.example.data.local.entity.RoutineCompletionEntity
import com.example.data.local.entity.RpgRewardAuditEntity
import com.example.data.local.entity.ScheduledActivityReferenceEntity
import com.example.data.local.entity.ShadowCollectionRecordEntity
import com.example.data.local.entity.SkillDefinitionEntity
import com.example.data.local.entity.SkillEffectConfigEntity
import com.example.data.local.entity.SkillTreeDependencyEntity
import com.example.data.local.entity.SkillUnlockRecordEntity
import com.example.data.local.entity.StrengthWeaknessFindingEntity
import com.example.data.local.entity.StudySessionEntity
import com.example.data.local.entity.SyncConflictEntity
import com.example.data.local.entity.SyncOperationLogEntity
import com.example.data.local.entity.SyncQueueEntity
import com.example.data.local.entity.SyncRecordMetaEntity
import com.example.data.local.entity.SystemNotificationEntity
import com.example.data.local.entity.TaskRecurrenceEntity
import com.example.data.local.entity.WorkloadAssessmentEntity
import com.example.data.local.entity.WorkoutSessionEntity
import com.example.data.local.entity.SystemEventEntity
import com.example.data.local.entity.AutomationJobEntity
import com.example.data.local.entity.AutomationExecutionLogEntity
import com.example.data.local.entity.DailyCycleExecutionEntity
import com.example.data.local.entity.DailyExecutionItemEntity
import com.example.data.local.entity.SystemIntegrityFindingEntity
import com.example.data.local.entity.SystemHealthSnapshotEntity
import com.example.data.local.entity.RecoveryOperationRecordEntity
import com.example.data.local.entity.SystemAnnouncementEntity
import com.example.data.local.entity.SystemCelebrationEntity
import com.example.data.local.entity.SystemExperiencePreferenceEntity
import com.example.data.local.entity.SystemPreferencesEntity
import com.example.data.local.entity.VoiceCommandHistoryEntity
import com.example.data.local.entity.toEntity
import com.example.domain.engine.RecoveryEngine
import com.example.domain.model.AttributeType
import com.example.domain.model.HealthConnectSyncState
import com.example.domain.model.NotificationCategory
import com.example.domain.model.PhysicalDevelopmentState
import com.example.domain.model.PlayerProfile
import com.example.domain.model.Quest
import com.example.domain.model.QuestCategory
import com.example.domain.model.QuestType
import com.example.domain.model.SystemNotification
import com.example.domain.model.VerificationStatus

@Database(
    entities = [
        PlayerEntity::class,
        QuestEntity::class,
        ActivityLogEntity::class,
        PhysicalStateEntity::class,
        SystemNotificationEntity::class,
        JoggingSessionEntity::class,
        WorkoutSessionEntity::class,
        ExerciseRecordEntity::class,
        RecoveryAssessmentEntity::class,
        ImportedActivityEntity::class,
        HealthConnectSyncStateEntity::class,
        AcademicSubjectEntity::class,
        AcademicModuleEntity::class,
        AcademicChapterEntity::class,
        AcademicTopicEntity::class,
        StudySessionEntity::class,
        RevisionRecordEntity::class,
        AcademicExamEntity::class,
        AcademicAssessmentEntity::class,
        AcademicQuestEntity::class,
        AcademicProgressionRecordEntity::class,
        AiConversationEntity::class,
        AiMessageEntity::class,
        AiRecommendationEntity::class,
        AiQuestProposalEntity::class,
        AiDailyPlanProposalEntity::class,
        AiModelPreferenceEntity::class,
        NotificationPreferenceEntity::class,
        AiConsentRecordEntity::class,
        AiRequestAuditEntity::class,
        LifeRoutineEntity::class,
        RoutineActivityEntity::class,
        RoutineCompletionEntity::class,
        HabitEntity::class,
        HabitCompletionEntity::class,
        PersonalTaskEntity::class,
        TaskRecurrenceEntity::class,
        LifeGoalEntity::class,
        GoalMilestoneEntity::class,
        GoalProgressRecordEntity::class,
        ScheduledActivityReferenceEntity::class,
        DisciplineAssessmentEntity::class,
        LifeProgressionRecordEntity::class,
        DungeonDefinitionEntity::class,
        DungeonObjectiveEntity::class,
        DungeonInstanceEntity::class,
        DungeonCompletionRecordEntity::class,
        BossDefinitionEntity::class,
        BossBattleInstanceEntity::class,
        BossBattlePhaseEntity::class,
        SkillDefinitionEntity::class,
        SkillUnlockRecordEntity::class,
        SkillEffectConfigEntity::class,
        SkillTreeDependencyEntity::class,
        AchievementDefinitionEntity::class,
        AchievementUnlockRecordEntity::class,
        PlayerTitleEntity::class,
        InventoryItemEntity::class,
        PlayerInventoryRecordEntity::class,
        ShadowCollectionRecordEntity::class,
        RpgRewardAuditEntity::class,
        AiDungeonProposalEntity::class,
        CloudAuthSessionEntity::class,
        RegisteredDeviceEntity::class,
        SyncRecordMetaEntity::class,
        SyncQueueEntity::class,
        SyncConflictEntity::class,
        SyncOperationLogEntity::class,
        CloudBackupEntity::class,
        CloudSyncCursorEntity::class,
        IntelligenceProfileEntity::class,
        StrengthWeaknessFindingEntity::class,
        AdaptiveDailyPlanEntity::class,
        AdaptivePlanBlockEntity::class,
        WorkloadAssessmentEntity::class,
        ConsistencyPredictionEntity::class,
        PersonalizedRecommendationEntity::class,
        RecommendationFeedbackEntity::class,
        DevelopmentRoadmapEntity::class,
        RoadmapMilestoneEntity::class,
        IntelligenceReportEntity::class,
        IntelligencePreferencesEntity::class,
        AdvisorConsultationEntity::class,
        SystemEventEntity::class,
        AutomationJobEntity::class,
        AutomationExecutionLogEntity::class,
        DailyCycleExecutionEntity::class,
        DailyExecutionItemEntity::class,
        SystemIntegrityFindingEntity::class,
        SystemHealthSnapshotEntity::class,
        RecoveryOperationRecordEntity::class,
        SystemPreferencesEntity::class,
        SystemExperiencePreferenceEntity::class,
        SystemAnnouncementEntity::class,
        SystemCelebrationEntity::class,
        VoiceCommandHistoryEntity::class,
        // Milestone 11 — Shadow Monarch
        ShadowMonarchProfileEntity::class,
        ShadowEntity::class,
        ShadowExtractionEntity::class,
        ShadowSkillUnlockEntity::class,
        ShadowDeploymentEntity::class,
        ShadowFormationEntity::class,
        MonarchSkillUnlockEntity::class,
        ShadowQuestProgressEntity::class,
        ShadowResourceTransactionEntity::class,
        // Milestone 12 — Social Mastery
        SocialProfileEntity::class,
        SocialModuleProgressEntity::class,
        SocialScenarioEntity::class,
        SocialPracticeSessionEntity::class,
        SocialScenarioAttemptEntity::class,
        SocialAssessmentEntity::class,
        SocialMissionRecordEntity::class
    ],
    version = 13,
    exportSchema = false
)
abstract class SoloLevelingDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao
    abstract fun questDao(): QuestDao
    abstract fun activityLogDao(): ActivityLogDao
    abstract fun physicalStateDao(): PhysicalStateDao
    abstract fun systemNotificationDao(): SystemNotificationDao
    abstract fun joggingSessionDao(): JoggingSessionDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun recoveryAssessmentDao(): RecoveryAssessmentDao
    abstract fun importedActivityDao(): ImportedActivityDao
    abstract fun physicalAuditDao(): PhysicalAuditDao
    abstract fun academicSubjectDao(): AcademicSubjectDao
    abstract fun studySessionDao(): StudySessionDao
    abstract fun revisionRecordDao(): RevisionRecordDao
    abstract fun academicAssessmentDao(): AcademicAssessmentDao
    abstract fun academicQuestDao(): AcademicQuestDao
    abstract fun academicProgressionDao(): AcademicProgressionDao
    abstract fun aiConversationDao(): AiConversationDao
    abstract fun aiProposalDao(): AiProposalDao
    abstract fun aiConfigDao(): AiConfigDao
    abstract fun aiRequestAuditDao(): AiRequestAuditDao
    abstract fun routineDao(): RoutineDao
    abstract fun habitDao(): HabitDao
    abstract fun personalTaskDao(): PersonalTaskDao
    abstract fun goalDao(): GoalDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun disciplineDao(): DisciplineDao
    abstract fun lifeProgressionDao(): LifeProgressionDao
    abstract fun dungeonDao(): DungeonDao
    abstract fun bossBattleDao(): BossBattleDao
    abstract fun skillDao(): SkillDao
    abstract fun achievementTitleDao(): AchievementTitleDao
    abstract fun inventoryShadowDao(): InventoryShadowDao
    abstract fun rpgAuditDao(): RpgAuditDao
    abstract fun cloudAuthDao(): CloudAuthDao
    abstract fun registeredDeviceDao(): RegisteredDeviceDao
    abstract fun syncMetaDao(): SyncMetaDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun syncConflictDao(): SyncConflictDao
    abstract fun syncDiagnosticsDao(): SyncDiagnosticsDao
    abstract fun cloudBackupDao(): CloudBackupDao
    abstract fun intelligenceProfileDao(): IntelligenceProfileDao
    abstract fun adaptivePlanDao(): AdaptivePlanDao
    abstract fun recommendationDao(): RecommendationDao
    abstract fun roadmapDao(): RoadmapDao
    abstract fun intelligenceReportDao(): IntelligenceReportDao
    abstract fun systemEventDao(): SystemEventDao
    abstract fun automationDao(): AutomationDao
    abstract fun dailyCycleDao(): DailyCycleDao
    abstract fun systemIntegrityHealthDao(): SystemIntegrityHealthDao
    abstract fun systemExperienceDao(): SystemExperienceDao

    // Milestone 11 — Shadow Monarch DAOs
    abstract fun shadowMonarchProfileDao(): ShadowMonarchProfileDao
    abstract fun shadowDao(): ShadowDao
    abstract fun shadowExtractionDao(): ShadowExtractionDao
    abstract fun shadowSkillUnlockDao(): ShadowSkillUnlockDao
    abstract fun shadowDeploymentDao(): ShadowDeploymentDao
    abstract fun shadowFormationDao(): ShadowFormationDao
    abstract fun monarchSkillUnlockDao(): MonarchSkillUnlockDao
    abstract fun shadowQuestProgressDao(): ShadowQuestProgressDao
    abstract fun shadowResourceDao(): ShadowResourceDao

    // Milestone 12 — Social Mastery DAOs
    abstract fun socialProfileDao(): SocialProfileDao
    abstract fun socialModuleProgressDao(): SocialModuleProgressDao
    abstract fun socialScenarioDao(): SocialScenarioDao
    abstract fun socialPracticeSessionDao(): SocialPracticeSessionDao
    abstract fun socialScenarioAttemptDao(): SocialScenarioAttemptDao
    abstract fun socialAssessmentDao(): SocialAssessmentDao
    abstract fun socialMissionDao(): SocialMissionDao

    companion object {
        @Volatile
        private var INSTANCE: SoloLevelingDatabase? = null

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `jogging_sessions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `sourceId` TEXT NOT NULL,
                        `originApp` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `activityKindName` TEXT NOT NULL,
                        `startEpochMs` INTEGER NOT NULL,
                        `endEpochMs` INTEGER NOT NULL,
                        `durationMinutes` INTEGER NOT NULL,
                        `distanceMeters` REAL,
                        `avgPaceSecPerKm` INTEGER,
                        `avgHeartRateBpm` INTEGER,
                        `verificationStatusName` TEXT NOT NULL,
                        `xpAwarded` INTEGER NOT NULL,
                        `notes` TEXT NOT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_jogging_sessions_sourceId` ON `jogging_sessions` (`sourceId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_jogging_sessions_startEpochMs` ON `jogging_sessions` (`startEpochMs`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `workout_sessions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `sourceId` TEXT NOT NULL,
                        `originApp` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `categoryName` TEXT NOT NULL,
                        `startEpochMs` INTEGER NOT NULL,
                        `endEpochMs` INTEGER NOT NULL,
                        `durationMinutes` INTEGER NOT NULL,
                        `totalSets` INTEGER NOT NULL,
                        `totalReps` INTEGER NOT NULL,
                        `avgHeartRateBpm` INTEGER,
                        `verificationStatusName` TEXT NOT NULL,
                        `xpAwarded` INTEGER NOT NULL,
                        `notes` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_workout_sessions_sourceId` ON `workout_sessions` (`sourceId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_sessions_startEpochMs` ON `workout_sessions` (`startEpochMs`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `exercise_records` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `workoutSessionId` INTEGER NOT NULL,
                        `exerciseId` TEXT NOT NULL,
                        `exerciseName` TEXT NOT NULL,
                        `categoryName` TEXT NOT NULL,
                        `setsCompleted` INTEGER NOT NULL,
                        `repsPerSet` INTEGER NOT NULL,
                        `weightKg` REAL,
                        `durationSeconds` INTEGER,
                        `restSeconds` INTEGER NOT NULL,
                        `primaryAttrName` TEXT NOT NULL,
                        `timestampEpochMs` INTEGER NOT NULL,
                        FOREIGN KEY(`workoutSessionId`) REFERENCES `workout_sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_records_workoutSessionId` ON `exercise_records` (`workoutSessionId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_records_exerciseId` ON `exercise_records` (`exerciseId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `recovery_assessments` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `stateName` TEXT NOT NULL,
                        `readinessScore` INTEGER NOT NULL,
                        `measuredLoadMinutes48h` INTEGER NOT NULL,
                        `measuredLoadMinutes7d` INTEGER NOT NULL,
                        `measuredSessions7d` INTEGER NOT NULL,
                        `consecutiveActiveDays` INTEGER NOT NULL,
                        `measuredSleepHoursLastNight` REAL,
                        `measuredRestingHrBpm` INTEGER,
                        `selfReportedFatigue` INTEGER,
                        `selfReportedDiscomfort` INTEGER NOT NULL,
                        `discomfortNotes` TEXT NOT NULL,
                        `recommendedActionsJoined` TEXT NOT NULL,
                        `summaryMessage` TEXT NOT NULL,
                        `timestampEpochMs` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `imported_activities` (
                        `sourceId` TEXT NOT NULL,
                        `originPackage` TEXT NOT NULL,
                        `activityType` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `startEpochMs` INTEGER NOT NULL,
                        `endEpochMs` INTEGER NOT NULL,
                        `durationMinutes` INTEGER NOT NULL,
                        `distanceMeters` REAL,
                        `avgHeartRateBpm` INTEGER,
                        `verificationStatusName` TEXT NOT NULL,
                        `validationOutcomeName` TEXT NOT NULL,
                        `xpAwarded` INTEGER NOT NULL,
                        `importedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`sourceId`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_imported_activities_startEpochMs_endEpochMs` ON `imported_activities` (`startEpochMs`, `endEpochMs`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `health_connect_sync_state` (
                        `id` INTEGER NOT NULL,
                        `lastSyncEpochMs` INTEGER,
                        `lastSyncStatus` TEXT NOT NULL,
                        `lastImportedCount` INTEGER NOT NULL,
                        `lastDuplicateCount` INTEGER NOT NULL,
                        `lastRejectedCount` INTEGER NOT NULL,
                        `lastErrorMessage` TEXT,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `academic_subjects` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `code` TEXT NOT NULL,
                        `programSystem` TEXT NOT NULL,
                        `colorTokenName` TEXT NOT NULL,
                        `iconKeyName` TEXT NOT NULL,
                        `priorityName` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `isArchived` INTEGER NOT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `academic_modules` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `subjectId` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `orderIndex` INTEGER NOT NULL,
                        FOREIGN KEY(`subjectId`) REFERENCES `academic_subjects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_academic_modules_subjectId` ON `academic_modules` (`subjectId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `academic_chapters` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `moduleId` INTEGER NOT NULL,
                        `subjectId` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `difficultyName` TEXT NOT NULL,
                        `estimatedMinutes` INTEGER NOT NULL,
                        `isCompleted` INTEGER NOT NULL,
                        `completedAtEpochMs` INTEGER,
                        `notes` TEXT NOT NULL,
                        `orderIndex` INTEGER NOT NULL,
                        FOREIGN KEY(`moduleId`) REFERENCES `academic_modules`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`subjectId`) REFERENCES `academic_subjects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_academic_chapters_moduleId` ON `academic_chapters` (`moduleId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_academic_chapters_subjectId` ON `academic_chapters` (`subjectId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `academic_topics` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `chapterId` INTEGER NOT NULL,
                        `subjectId` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `isCompleted` INTEGER NOT NULL,
                        `confidenceEstimate` INTEGER NOT NULL,
                        `revisionCount` INTEGER NOT NULL,
                        `lastRevisedEpochMs` INTEGER,
                        `nextRevisionDueEpochMs` INTEGER,
                        `notes` TEXT NOT NULL,
                        FOREIGN KEY(`chapterId`) REFERENCES `academic_chapters`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`subjectId`) REFERENCES `academic_subjects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_academic_topics_chapterId` ON `academic_topics` (`chapterId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_academic_topics_subjectId` ON `academic_topics` (`subjectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_academic_topics_nextRevisionDueEpochMs` ON `academic_topics` (`nextRevisionDueEpochMs`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `study_sessions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `sourceId` TEXT NOT NULL,
                        `subjectId` INTEGER NOT NULL,
                        `subjectName` TEXT NOT NULL,
                        `topicId` INTEGER,
                        `topicTitle` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `activityKindName` TEXT NOT NULL,
                        `startEpochMs` INTEGER NOT NULL,
                        `endEpochMs` INTEGER NOT NULL,
                        `durationMinutes` INTEGER NOT NULL,
                        `breakMinutes` INTEGER NOT NULL,
                        `verificationStatusName` TEXT NOT NULL,
                        `xpAwarded` INTEGER NOT NULL,
                        `notes` TEXT NOT NULL,
                        FOREIGN KEY(`subjectId`) REFERENCES `academic_subjects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_study_sessions_sourceId` ON `study_sessions` (`sourceId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_study_sessions_subjectId` ON `study_sessions` (`subjectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_study_sessions_startEpochMs` ON `study_sessions` (`startEpochMs`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `revision_records` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `topicId` INTEGER NOT NULL,
                        `subjectId` INTEGER NOT NULL,
                        `topicTitle` TEXT NOT NULL,
                        `subjectName` TEXT NOT NULL,
                        `confidenceAfter` INTEGER NOT NULL,
                        `intervalDaysScheduled` INTEGER NOT NULL,
                        `nextDueEpochMs` INTEGER NOT NULL,
                        `durationMinutes` INTEGER NOT NULL,
                        `verificationStatusName` TEXT NOT NULL,
                        `xpAwarded` INTEGER NOT NULL,
                        `notes` TEXT NOT NULL,
                        `revisedAtEpochMs` INTEGER NOT NULL,
                        FOREIGN KEY(`topicId`) REFERENCES `academic_topics`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`subjectId`) REFERENCES `academic_subjects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_revision_records_topicId` ON `revision_records` (`topicId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_revision_records_subjectId` ON `revision_records` (`subjectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_revision_records_revisedAtEpochMs` ON `revision_records` (`revisedAtEpochMs`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `academic_exams` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `subjectId` INTEGER NOT NULL,
                        `subjectName` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `examDateEpochMs` INTEGER NOT NULL,
                        `markingSystemName` TEXT NOT NULL,
                        `targetScoreDisplay` TEXT NOT NULL,
                        `targetNormalizedRatio` REAL NOT NULL,
                        `actualScoreDisplay` TEXT,
                        `actualNormalizedRatio` REAL,
                        `syllabusCoveragePercent` INTEGER NOT NULL,
                        `isCompleted` INTEGER NOT NULL,
                        `notes` TEXT NOT NULL,
                        FOREIGN KEY(`subjectId`) REFERENCES `academic_subjects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_academic_exams_subjectId` ON `academic_exams` (`subjectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_academic_exams_examDateEpochMs` ON `academic_exams` (`examDateEpochMs`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `academic_assessments` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `subjectId` INTEGER NOT NULL,
                        `subjectName` TEXT NOT NULL,
                        `examId` INTEGER,
                        `topicId` INTEGER,
                        `title` TEXT NOT NULL,
                        `assessmentTypeName` TEXT NOT NULL,
                        `markingSystemName` TEXT NOT NULL,
                        `scoredMarks` REAL NOT NULL,
                        `maxMarks` REAL NOT NULL,
                        `gradeLabel` TEXT NOT NULL,
                        `verificationStatusName` TEXT NOT NULL,
                        `xpAwarded` INTEGER NOT NULL,
                        `notes` TEXT NOT NULL,
                        `timestampEpochMs` INTEGER NOT NULL,
                        FOREIGN KEY(`subjectId`) REFERENCES `academic_subjects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_academic_assessments_subjectId` ON `academic_assessments` (`subjectId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_academic_assessments_examId` ON `academic_assessments` (`examId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_academic_assessments_timestampEpochMs` ON `academic_assessments` (`timestampEpochMs`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `academic_quests` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `categoryName` TEXT NOT NULL,
                        `subjectId` INTEGER,
                        `subjectName` TEXT NOT NULL,
                        `topicId` INTEGER,
                        `topicTitle` TEXT NOT NULL,
                        `difficultyName` TEXT NOT NULL,
                        `objective` TEXT NOT NULL,
                        `targetValue` INTEGER NOT NULL,
                        `currentValue` INTEGER NOT NULL,
                        `unit` TEXT NOT NULL,
                        `deadlineEpochMs` INTEGER,
                        `statusName` TEXT NOT NULL,
                        `xpReward` INTEGER NOT NULL,
                        `primaryAttrName` TEXT NOT NULL,
                        `primaryAttrXpReward` INTEGER NOT NULL,
                        `secondaryAttrName` TEXT,
                        `secondaryAttrXpReward` INTEGER NOT NULL,
                        `completionEvidence` TEXT NOT NULL,
                        `verificationStatusName` TEXT NOT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        `completedAtEpochMs` INTEGER
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_academic_quests_statusName` ON `academic_quests` (`statusName`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_academic_quests_categoryName` ON `academic_quests` (`categoryName`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_academic_quests_subjectId` ON `academic_quests` (`subjectId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `academic_progression_records` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `rewardKey` TEXT NOT NULL,
                        `activityKindName` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `subjectId` INTEGER,
                        `verificationStatusName` TEXT NOT NULL,
                        `xpAwarded` INTEGER NOT NULL,
                        `intXpAwarded` INTEGER NOT NULL,
                        `wisXpAwarded` INTEGER NOT NULL,
                        `disXpAwarded` INTEGER NOT NULL,
                        `notes` TEXT NOT NULL,
                        `timestampEpochMs` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_academic_progression_records_rewardKey` ON `academic_progression_records` (`rewardKey`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_academic_progression_records_timestampEpochMs` ON `academic_progression_records` (`timestampEpochMs`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_academic_progression_records_subjectId` ON `academic_progression_records` (`subjectId`)")
            }
        }

        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `system_notifications` ADD COLUMN `deepLinkRoute` TEXT")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `ai_conversations` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `associatedModule` TEXT NOT NULL,
                        `modelUsed` TEXT NOT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_conversations_updatedAtEpochMs` ON `ai_conversations` (`updatedAtEpochMs`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `ai_messages` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `conversationId` INTEGER NOT NULL,
                        `roleName` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `associatedModule` TEXT NOT NULL,
                        `modelUsed` TEXT NOT NULL,
                        `isFallback` INTEGER NOT NULL,
                        `timestampEpochMs` INTEGER NOT NULL,
                        FOREIGN KEY(`conversationId`) REFERENCES `ai_conversations`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_messages_conversationId` ON `ai_messages` (`conversationId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_messages_timestampEpochMs` ON `ai_messages` (`timestampEpochMs`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `ai_recommendations` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `module` TEXT NOT NULL,
                        `headline` TEXT NOT NULL,
                        `detail` TEXT NOT NULL,
                        `rationale` TEXT NOT NULL,
                        `confidenceNote` TEXT NOT NULL,
                        `isDismissed` INTEGER NOT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_recommendations_createdAtEpochMs` ON `ai_recommendations` (`createdAtEpochMs`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `ai_quest_proposals` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `proposalUuid` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `categoryName` TEXT NOT NULL,
                        `difficultyLabel` TEXT NOT NULL,
                        `objective` TEXT NOT NULL,
                        `completionCriteria` TEXT NOT NULL,
                        `suggestedDurationMinutes` INTEGER NOT NULL,
                        `deadlineEpochMs` INTEGER,
                        `priorityName` TEXT NOT NULL,
                        `reason` TEXT NOT NULL,
                        `targetSubjectId` INTEGER,
                        `statusName` TEXT NOT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_ai_quest_proposals_proposalUuid` ON `ai_quest_proposals` (`proposalUuid`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_quest_proposals_statusName` ON `ai_quest_proposals` (`statusName`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_quest_proposals_createdAtEpochMs` ON `ai_quest_proposals` (`createdAtEpochMs`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `ai_daily_plan_proposals` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `batchId` TEXT NOT NULL,
                        `planModeName` TEXT NOT NULL,
                        `taskTitle` TEXT NOT NULL,
                        `module` TEXT NOT NULL,
                        `startMinuteOfDay` INTEGER NOT NULL,
                        `durationMinutes` INTEGER NOT NULL,
                        `priorityName` TEXT NOT NULL,
                        `reason` TEXT NOT NULL,
                        `dependencies` TEXT NOT NULL,
                        `optionalDeadlineEpochMs` INTEGER,
                        `statusName` TEXT NOT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_daily_plan_proposals_batchId` ON `ai_daily_plan_proposals` (`batchId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_daily_plan_proposals_statusName` ON `ai_daily_plan_proposals` (`statusName`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_daily_plan_proposals_startMinuteOfDay` ON `ai_daily_plan_proposals` (`startMinuteOfDay`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `ai_model_preferences` (
                        `id` INTEGER NOT NULL,
                        `selectedModelId` TEXT NOT NULL,
                        `backendBaseUrl` TEXT NOT NULL,
                        `aiEnabled` INTEGER NOT NULL,
                        `streamingEnabled` INTEGER NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `notification_preferences` (
                        `id` INTEGER NOT NULL,
                        `notificationsEnabled` INTEGER NOT NULL,
                        `enableDailyQuestReminders` INTEGER NOT NULL,
                        `enableOverdueRevisions` INTEGER NOT NULL,
                        `enableUpcomingExams` INTEGER NOT NULL,
                        `enableWorkoutReminders` INTEGER NOT NULL,
                        `enableRecoveryWarnings` INTEGER NOT NULL,
                        `enableAiRecommendations` INTEGER NOT NULL,
                        `enableDailySummary` INTEGER NOT NULL,
                        `quietHoursStartHour` INTEGER NOT NULL,
                        `quietHoursEndHour` INTEGER NOT NULL,
                        `maxNotificationsPerDay` INTEGER NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `ai_consent_records` (
                        `id` INTEGER NOT NULL,
                        `cloudAiConsentGranted` INTEGER NOT NULL,
                        `sharePhysicalWithAi` INTEGER NOT NULL,
                        `shareAcademicWithAi` INTEGER NOT NULL,
                        `shareQuestsWithAi` INTEGER NOT NULL,
                        `consentVersion` INTEGER NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `ai_request_audit` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `requestType` TEXT NOT NULL,
                        `modelId` TEXT NOT NULL,
                        `usedCloudBackend` INTEGER NOT NULL,
                        `success` INTEGER NOT NULL,
                        `promptTokens` INTEGER NOT NULL,
                        `completionTokens` INTEGER NOT NULL,
                        `latencyMs` INTEGER NOT NULL,
                        `errorCode` TEXT,
                        `timestampEpochMs` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_request_audit_timestampEpochMs` ON `ai_request_audit` (`timestampEpochMs`)")
            }
        }

        val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `notification_preferences` ADD COLUMN `enableHabitReminders` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `notification_preferences` ADD COLUMN `enableRoutineReminders` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `notification_preferences` ADD COLUMN `enableTaskDeadlines` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `notification_preferences` ADD COLUMN `enableOverdueTasks` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `notification_preferences` ADD COLUMN `enableGoalMilestones` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `notification_preferences` ADD COLUMN `enableDailyPlanningReminders` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `notification_preferences` ADD COLUMN `enableWeeklyReviewReminders` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `ai_consent_records` ADD COLUMN `shareLifeWithAi` INTEGER NOT NULL DEFAULT 1")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `life_routines` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `timeSlotName` TEXT NOT NULL,
                        `startMinuteOfDay` INTEGER NOT NULL,
                        `scheduledDaysCsv` TEXT NOT NULL,
                        `isEnabled` INTEGER NOT NULL,
                        `targetDurationMinutes` INTEGER NOT NULL,
                        `reminderEnabled` INTEGER NOT NULL,
                        `isRestExemptAllowed` INTEGER NOT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_life_routines_timeSlotName` ON `life_routines` (`timeSlotName`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `routine_activities` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `routineId` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `durationMinutes` INTEGER NOT NULL,
                        `orderIndex` INTEGER NOT NULL,
                        `isOptional` INTEGER NOT NULL,
                        FOREIGN KEY(`routineId`) REFERENCES `life_routines`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_routine_activities_routineId` ON `routine_activities` (`routineId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `routine_completions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `routineId` INTEGER NOT NULL,
                        `activityId` INTEGER NOT NULL,
                        `dateEpochDay` INTEGER NOT NULL,
                        `statusName` TEXT NOT NULL,
                        `note` TEXT NOT NULL,
                        `completedAtEpochMs` INTEGER NOT NULL,
                        FOREIGN KEY(`routineId`) REFERENCES `life_routines`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_routine_completions_routineId` ON `routine_completions` (`routineId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_routine_completions_dateEpochDay` ON `routine_completions` (`dateEpochDay`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_routine_completions_routineId_activityId_dateEpochDay` ON `routine_completions` (`routineId`, `activityId`, `dateEpochDay`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `habits` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `habitTypeName` TEXT NOT NULL,
                        `categoryName` TEXT NOT NULL,
                        `targetValue` REAL NOT NULL,
                        `unit` TEXT NOT NULL,
                        `scheduledDaysCsv` TEXT NOT NULL,
                        `preferredStartMinute` INTEGER,
                        `reminderEnabled` INTEGER NOT NULL,
                        `isRestDayProtected` INTEGER NOT NULL,
                        `isArchived` INTEGER NOT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_habits_isArchived` ON `habits` (`isArchived`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `habit_completions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `habitId` INTEGER NOT NULL,
                        `dateEpochDay` INTEGER NOT NULL,
                        `statusName` TEXT NOT NULL,
                        `achievedValue` REAL NOT NULL,
                        `skipReason` TEXT NOT NULL,
                        `note` TEXT NOT NULL,
                        `recordedAtEpochMs` INTEGER NOT NULL,
                        FOREIGN KEY(`habitId`) REFERENCES `habits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_completions_habitId` ON `habit_completions` (`habitId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_completions_dateEpochDay` ON `habit_completions` (`dateEpochDay`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_habit_completions_habitId_dateEpochDay` ON `habit_completions` (`habitId`, `dateEpochDay`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `personal_tasks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `categoryName` TEXT NOT NULL,
                        `priorityName` TEXT NOT NULL,
                        `statusName` TEXT NOT NULL,
                        `dueDateEpochDay` INTEGER,
                        `scheduledStartMinute` INTEGER,
                        `estimatedMinutes` INTEGER NOT NULL,
                        `linkedGoalId` INTEGER,
                        `isTimeSensitive` INTEGER NOT NULL,
                        `postponeCount` INTEGER NOT NULL,
                        `timezoneId` TEXT NOT NULL,
                        `completedAtEpochMs` INTEGER,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_personal_tasks_statusName` ON `personal_tasks` (`statusName`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_personal_tasks_dueDateEpochDay` ON `personal_tasks` (`dueDateEpochDay`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_personal_tasks_linkedGoalId` ON `personal_tasks` (`linkedGoalId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `task_recurrences` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `taskId` INTEGER NOT NULL,
                        `recurrenceTypeName` TEXT NOT NULL,
                        `intervalDays` INTEGER NOT NULL,
                        `selectedDaysCsv` TEXT NOT NULL,
                        `endDateEpochDay` INTEGER,
                        FOREIGN KEY(`taskId`) REFERENCES `personal_tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_task_recurrences_taskId` ON `task_recurrences` (`taskId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `life_goals` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `timeframeName` TEXT NOT NULL,
                        `statusName` TEXT NOT NULL,
                        `targetAttributeName` TEXT NOT NULL,
                        `completionCriteria` TEXT NOT NULL,
                        `startDateEpochDay` INTEGER NOT NULL,
                        `targetDateEpochDay` INTEGER NOT NULL,
                        `linkedModule` TEXT NOT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_life_goals_statusName` ON `life_goals` (`statusName`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_life_goals_timeframeName` ON `life_goals` (`timeframeName`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `goal_milestones` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `goalId` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `weight` INTEGER NOT NULL,
                        `dueDateEpochDay` INTEGER,
                        `isCompleted` INTEGER NOT NULL,
                        `completedAtEpochMs` INTEGER,
                        `orderIndex` INTEGER NOT NULL,
                        FOREIGN KEY(`goalId`) REFERENCES `life_goals`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_goal_milestones_goalId` ON `goal_milestones` (`goalId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `goal_progress_records` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `goalId` INTEGER NOT NULL,
                        `note` TEXT NOT NULL,
                        `recordedProgressPercent` INTEGER NOT NULL,
                        `adjustmentSummary` TEXT NOT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        FOREIGN KEY(`goalId`) REFERENCES `life_goals`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_goal_progress_records_goalId` ON `goal_progress_records` (`goalId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_goal_progress_records_createdAtEpochMs` ON `goal_progress_records` (`createdAtEpochMs`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `scheduled_activity_references` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `sourceTypeName` TEXT NOT NULL,
                        `sourceRecordId` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `subtitle` TEXT NOT NULL,
                        `dateEpochDay` INTEGER NOT NULL,
                        `startMinuteOfDay` INTEGER NOT NULL,
                        `durationMinutes` INTEGER NOT NULL,
                        `isAllDay` INTEGER NOT NULL,
                        `isRecurringInstance` INTEGER NOT NULL,
                        `isCompletedSnapshot` INTEGER NOT NULL,
                        `timezoneId` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_scheduled_activity_references_dateEpochDay` ON `scheduled_activity_references` (`dateEpochDay`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_scheduled_activity_references_sourceTypeName_sourceRecordId` ON `scheduled_activity_references` (`sourceTypeName`, `sourceRecordId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `discipline_assessments` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `dateEpochDay` INTEGER NOT NULL,
                        `disciplineScore` INTEGER NOT NULL,
                        `routineConsistencyPercent` INTEGER NOT NULL,
                        `habitConsistencyPercent` INTEGER NOT NULL,
                        `taskCompletionPercent` INTEGER NOT NULL,
                        `scheduleAdherencePercent` INTEGER NOT NULL,
                        `goalProgressPercent` INTEGER NOT NULL,
                        `strongestArea` TEXT NOT NULL,
                        `weakestArea` TEXT NOT NULL,
                        `missedCommitmentsCount` INTEGER NOT NULL,
                        `exemptRestDaysCount` INTEGER NOT NULL,
                        `reflectionSummary` TEXT NOT NULL,
                        `calculatedAtEpochMs` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_discipline_assessments_dateEpochDay` ON `discipline_assessments` (`dateEpochDay`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `life_progression_records` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `rewardKey` TEXT NOT NULL,
                        `activityKindName` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `verificationStatusName` TEXT NOT NULL,
                        `xpAwarded` INTEGER NOT NULL,
                        `disXpAwarded` INTEGER NOT NULL,
                        `wisXpAwarded` INTEGER NOT NULL,
                        `chaXpAwarded` INTEGER NOT NULL,
                        `intXpAwarded` INTEGER NOT NULL,
                        `vitXpAwarded` INTEGER NOT NULL,
                        `notes` TEXT NOT NULL,
                        `timestampEpochMs` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_life_progression_records_rewardKey` ON `life_progression_records` (`rewardKey`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_life_progression_records_timestampEpochMs` ON `life_progression_records` (`timestampEpochMs`)")
            }
        }

        val MIGRATION_5_6: Migration = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `notification_preferences` ADD COLUMN `enableRpgAlerts` INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE `ai_consent_records` ADD COLUMN `shareRpgWithAi` INTEGER NOT NULL DEFAULT 1")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `dungeon_definitions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `code` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `categoryName` TEXT NOT NULL,
                        `difficultyName` TEXT NOT NULL,
                        `requiredRankName` TEXT NOT NULL,
                        `requiredLevel` INTEGER NOT NULL,
                        `requiredAttributeCode` TEXT,
                        `requiredAttributeValue` INTEGER NOT NULL,
                        `timeLimitHours` INTEGER NOT NULL,
                        `baseXpReward` INTEGER NOT NULL,
                        `primaryAttrName` TEXT NOT NULL,
                        `primaryAttrXp` INTEGER NOT NULL,
                        `secondaryAttrName` TEXT NOT NULL,
                        `secondaryAttrXp` INTEGER NOT NULL,
                        `rewardTitleId` TEXT,
                        `rewardItemId` TEXT,
                        `rewardShadowId` TEXT,
                        `isRepeatable` INTEGER NOT NULL,
                        `creationSourceName` TEXT NOT NULL,
                        `isActiveCatalog` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_dungeon_definitions_code` ON `dungeon_definitions` (`code`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `dungeon_objectives` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `dungeonId` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `evidenceTypeName` TEXT NOT NULL,
                        `targetValue` INTEGER NOT NULL,
                        `requiresVerifiedOnly` INTEGER NOT NULL,
                        `orderIndex` INTEGER NOT NULL,
                        FOREIGN KEY(`dungeonId`) REFERENCES `dungeon_definitions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_dungeon_objectives_dungeonId` ON `dungeon_objectives` (`dungeonId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `dungeon_instances` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `dungeonId` INTEGER NOT NULL,
                        `statusName` TEXT NOT NULL,
                        `creationSourceName` TEXT NOT NULL,
                        `cadenceKey` TEXT NOT NULL,
                        `startedAtEpochMs` INTEGER NOT NULL,
                        `pausedAtEpochMs` INTEGER,
                        `accumulatedPausedMs` INTEGER NOT NULL,
                        `deadlineEpochMs` INTEGER NOT NULL,
                        `completedAtEpochMs` INTEGER,
                        `notes` TEXT NOT NULL,
                        FOREIGN KEY(`dungeonId`) REFERENCES `dungeon_definitions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_dungeon_instances_dungeonId` ON `dungeon_instances` (`dungeonId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_dungeon_instances_statusName` ON `dungeon_instances` (`statusName`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_dungeon_instances_cadenceKey` ON `dungeon_instances` (`cadenceKey`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `dungeon_completions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `rewardKey` TEXT NOT NULL,
                        `instanceId` INTEGER NOT NULL,
                        `dungeonId` INTEGER NOT NULL,
                        `dungeonTitle` TEXT NOT NULL,
                        `difficultyName` TEXT NOT NULL,
                        `xpAwarded` INTEGER NOT NULL,
                        `primaryAttrName` TEXT NOT NULL,
                        `primaryAttrXp` INTEGER NOT NULL,
                        `secondaryAttrName` TEXT NOT NULL,
                        `secondaryAttrXp` INTEGER NOT NULL,
                        `evidenceSummary` TEXT NOT NULL,
                        `verificationProvenance` TEXT NOT NULL,
                        `completedAtEpochMs` INTEGER NOT NULL,
                        FOREIGN KEY(`dungeonId`) REFERENCES `dungeon_definitions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_dungeon_completions_rewardKey` ON `dungeon_completions` (`rewardKey`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_dungeon_completions_dungeonId` ON `dungeon_completions` (`dungeonId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_dungeon_completions_completedAtEpochMs` ON `dungeon_completions` (`completedAtEpochMs`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `boss_definitions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `code` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `subtitle` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `categoryName` TEXT NOT NULL,
                        `difficultyName` TEXT NOT NULL,
                        `requiredRankName` TEXT NOT NULL,
                        `requiredLevel` INTEGER NOT NULL,
                        `xpReward` INTEGER NOT NULL,
                        `primaryAttrName` TEXT NOT NULL,
                        `primaryAttrXp` INTEGER NOT NULL,
                        `secondaryAttrName` TEXT NOT NULL,
                        `secondaryAttrXp` INTEGER NOT NULL,
                        `rewardTitleId` TEXT,
                        `rewardItemId` TEXT,
                        `rewardShadowId` TEXT,
                        `totalEstimatedDays` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_boss_definitions_code` ON `boss_definitions` (`code`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `boss_instances` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `bossId` INTEGER NOT NULL,
                        `statusName` TEXT NOT NULL,
                        `startedAtEpochMs` INTEGER NOT NULL,
                        `completedAtEpochMs` INTEGER,
                        `abandonedAtEpochMs` INTEGER,
                        FOREIGN KEY(`bossId`) REFERENCES `boss_definitions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_boss_instances_bossId` ON `boss_instances` (`bossId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_boss_instances_statusName` ON `boss_instances` (`statusName`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `boss_phases` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `bossId` INTEGER NOT NULL,
                        `phaseNumber` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `evidenceTypeName` TEXT NOT NULL,
                        `targetValue` INTEGER NOT NULL,
                        `requiresVerifiedOnly` INTEGER NOT NULL,
                        `weight` INTEGER NOT NULL,
                        FOREIGN KEY(`bossId`) REFERENCES `boss_definitions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_boss_phases_bossId` ON `boss_phases` (`bossId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `skill_definitions` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `specializationName` TEXT NOT NULL,
                        `tierName` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `requiredRankName` TEXT NOT NULL,
                        `requiredLevel` INTEGER NOT NULL,
                        `requiredAttrName` TEXT NOT NULL,
                        `requiredAttributeValue` INTEGER NOT NULL,
                        `requiredVerifiedCount` INTEGER NOT NULL,
                        `requiredStreakDays` INTEGER NOT NULL,
                        `maxMasteryLevel` INTEGER NOT NULL,
                        `treeColumn` INTEGER NOT NULL,
                        `treeRow` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `skill_unlock_records` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `skillId` TEXT NOT NULL,
                        `masteryLevel` INTEGER NOT NULL,
                        `unlockedAtEpochMs` INTEGER NOT NULL,
                        `lastUpgradedAtEpochMs` INTEGER NOT NULL,
                        `evidenceNote` TEXT NOT NULL,
                        FOREIGN KEY(`skillId`) REFERENCES `skill_definitions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_skill_unlock_records_skillId` ON `skill_unlock_records` (`skillId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `skill_effect_configs` (
                        `effectCode` TEXT NOT NULL,
                        `skillId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `featureTag` TEXT NOT NULL,
                        PRIMARY KEY(`effectCode`),
                        FOREIGN KEY(`skillId`) REFERENCES `skill_definitions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_skill_effect_configs_skillId` ON `skill_effect_configs` (`skillId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `skill_tree_dependencies` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `skillId` TEXT NOT NULL,
                        `prerequisiteSkillId` TEXT NOT NULL,
                        `minPrerequisiteMastery` INTEGER NOT NULL,
                        FOREIGN KEY(`skillId`) REFERENCES `skill_definitions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`prerequisiteSkillId`) REFERENCES `skill_definitions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_skill_tree_dependencies_skillId_prerequisiteSkillId` ON `skill_tree_dependencies` (`skillId`, `prerequisiteSkillId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_skill_tree_dependencies_prerequisiteSkillId` ON `skill_tree_dependencies` (`prerequisiteSkillId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `achievement_definitions` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `categoryName` TEXT NOT NULL,
                        `rarityName` TEXT NOT NULL,
                        `criterionDescription` TEXT NOT NULL,
                        `targetValue` INTEGER NOT NULL,
                        `xpBonus` INTEGER NOT NULL,
                        `rewardTitleId` TEXT,
                        `rewardItemId` TEXT,
                        `rewardShadowId` TEXT,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `achievement_unlock_records` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `achievementId` TEXT NOT NULL,
                        `evidenceReference` TEXT NOT NULL,
                        `unlockedAtEpochMs` INTEGER NOT NULL,
                        FOREIGN KEY(`achievementId`) REFERENCES `achievement_definitions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_achievement_unlock_records_achievementId` ON `achievement_unlock_records` (`achievementId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `player_titles` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `rarityName` TEXT NOT NULL,
                        `unlockRequirement` TEXT NOT NULL,
                        `isUnlocked` INTEGER NOT NULL,
                        `isEquipped` INTEGER NOT NULL,
                        `unlockedAtEpochMs` INTEGER,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_player_titles_isUnlocked` ON `player_titles` (`isUnlocked`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_player_titles_isEquipped` ON `player_titles` (`isEquipped`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `inventory_items` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `categoryName` TEXT NOT NULL,
                        `rarityName` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `acquisitionSource` TEXT NOT NULL,
                        `canEquip` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `player_inventory_records` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `itemId` TEXT NOT NULL,
                        `quantity` INTEGER NOT NULL,
                        `isEquipped` INTEGER NOT NULL,
                        `sourceDescription` TEXT NOT NULL,
                        `acquiredAtEpochMs` INTEGER NOT NULL,
                        FOREIGN KEY(`itemId`) REFERENCES `inventory_items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_player_inventory_records_itemId` ON `player_inventory_records` (`itemId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `shadow_collection_records` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `collectibleId` TEXT NOT NULL,
                        `rankTier` INTEGER NOT NULL,
                        `evidenceSource` TEXT NOT NULL,
                        `unlockedAtEpochMs` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_shadow_collection_records_collectibleId` ON `shadow_collection_records` (`collectibleId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `rpg_reward_audit` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `rewardKey` TEXT NOT NULL,
                        `sourceType` TEXT NOT NULL,
                        `sourceId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `xpAwarded` INTEGER NOT NULL,
                        `primaryAttrName` TEXT NOT NULL,
                        `primaryAttrXp` INTEGER NOT NULL,
                        `secondaryAttrName` TEXT,
                        `secondaryAttrXp` INTEGER NOT NULL,
                        `verificationProvenance` TEXT NOT NULL,
                        `wasCapped` INTEGER NOT NULL,
                        `notes` TEXT NOT NULL,
                        `timestampEpochMs` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_rpg_reward_audit_rewardKey` ON `rpg_reward_audit` (`rewardKey`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_rpg_reward_audit_timestampEpochMs` ON `rpg_reward_audit` (`timestampEpochMs`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `ai_dungeon_proposals` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `proposalUuid` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `categoryName` TEXT NOT NULL,
                        `difficultyName` TEXT NOT NULL,
                        `objectiveTitle` TEXT NOT NULL,
                        `targetEvidenceTypeName` TEXT NOT NULL,
                        `targetValue` INTEGER NOT NULL,
                        `timeLimitHours` INTEGER NOT NULL,
                        `requiresVerifiedOnly` INTEGER NOT NULL,
                        `reason` TEXT NOT NULL,
                        `isBossCampaign` INTEGER NOT NULL,
                        `statusName` TEXT NOT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_ai_dungeon_proposals_proposalUuid` ON `ai_dungeon_proposals` (`proposalUuid`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_dungeon_proposals_statusName` ON `ai_dungeon_proposals` (`statusName`)")
            }
        }

        val MIGRATION_6_7: Migration = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cloud_auth_sessions` (
                        `id` INTEGER NOT NULL,
                        `supabaseUserId` TEXT NOT NULL,
                        `playerIdentityUuid` TEXT NOT NULL,
                        `email` TEXT NOT NULL,
                        `displayName` TEXT NOT NULL,
                        `activeTitle` TEXT NOT NULL,
                        `hunterRankName` TEXT NOT NULL,
                        `level` INTEGER NOT NULL,
                        `totalXp` INTEGER NOT NULL,
                        `accountStatusName` TEXT NOT NULL,
                        `authStatusName` TEXT NOT NULL,
                        `emailVerified` INTEGER NOT NULL,
                        `autoSyncEnabled` INTEGER NOT NULL,
                        `cloudSyncConsentGranted` INTEGER NOT NULL,
                        `expiresAtEpochMs` INTEGER,
                        `lastSyncAtEpochMs` INTEGER,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `registered_devices` (
                        `deviceUuid` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `deviceName` TEXT NOT NULL,
                        `manufacturerModel` TEXT NOT NULL,
                        `osVersion` TEXT NOT NULL,
                        `appVersion` TEXT NOT NULL,
                        `lastActiveEpochMs` INTEGER NOT NULL,
                        `lastSyncEpochMs` INTEGER,
                        `syncStatusName` TEXT NOT NULL,
                        `isCurrentDevice` INTEGER NOT NULL,
                        `isRevoked` INTEGER NOT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`deviceUuid`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_registered_devices_userId` ON `registered_devices` (`userId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_registered_devices_lastActiveEpochMs` ON `registered_devices` (`lastActiveEpochMs`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `sync_records_meta` (
                        `compositeKey` TEXT NOT NULL,
                        `entityType` TEXT NOT NULL,
                        `localKey` TEXT NOT NULL,
                        `cloudUuid` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `version` INTEGER NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        `deletedAtEpochMs` INTEGER,
                        `syncStateName` TEXT NOT NULL,
                        `lastSyncedHash` TEXT NOT NULL,
                        `lastError` TEXT,
                        PRIMARY KEY(`compositeKey`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_sync_records_meta_entityType_localKey` ON `sync_records_meta` (`entityType`, `localKey`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_records_meta_cloudUuid` ON `sync_records_meta` (`cloudUuid`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_records_meta_syncStateName` ON `sync_records_meta` (`syncStateName`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `sync_queue` (
                        `queueId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `entityTypeName` TEXT NOT NULL,
                        `localKey` TEXT NOT NULL,
                        `cloudUuid` TEXT NOT NULL,
                        `operationName` TEXT NOT NULL,
                        `payloadJson` TEXT NOT NULL,
                        `idempotencyKey` TEXT NOT NULL,
                        `syncStateName` TEXT NOT NULL,
                        `retryCount` INTEGER NOT NULL,
                        `nextRetryAtEpochMs` INTEGER NOT NULL,
                        `lastError` TEXT,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_sync_queue_idempotencyKey` ON `sync_queue` (`idempotencyKey`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_queue_syncStateName` ON `sync_queue` (`syncStateName`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_queue_nextRetryAtEpochMs` ON `sync_queue` (`nextRetryAtEpochMs`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `sync_conflicts` (
                        `conflictId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `entityTypeName` TEXT NOT NULL,
                        `localKey` TEXT NOT NULL,
                        `cloudUuid` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `conflictReason` TEXT NOT NULL,
                        `localSummary` TEXT NOT NULL,
                        `cloudSummary` TEXT NOT NULL,
                        `localPayloadJson` TEXT NOT NULL,
                        `cloudPayloadJson` TEXT NOT NULL,
                        `localUpdatedAtEpochMs` INTEGER NOT NULL,
                        `cloudUpdatedAtEpochMs` INTEGER NOT NULL,
                        `resolutionStrategy` TEXT NOT NULL,
                        `statusName` TEXT NOT NULL,
                        `resolvedAtEpochMs` INTEGER,
                        `createdAtEpochMs` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_conflicts_statusName` ON `sync_conflicts` (`statusName`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_conflicts_entityTypeName_localKey` ON `sync_conflicts` (`entityTypeName`, `localKey`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `sync_operation_logs` (
                        `operationUuid` TEXT NOT NULL,
                        `triggerSource` TEXT NOT NULL,
                        `startedAtEpochMs` INTEGER NOT NULL,
                        `completedAtEpochMs` INTEGER,
                        `statusName` TEXT NOT NULL,
                        `recordsUploaded` INTEGER NOT NULL,
                        `recordsDownloaded` INTEGER NOT NULL,
                        `recordsSkipped` INTEGER NOT NULL,
                        `conflictsDetected` INTEGER NOT NULL,
                        `failedOperations` INTEGER NOT NULL,
                        `retryCount` INTEGER NOT NULL,
                        `errorCategory` TEXT,
                        `safeMessage` TEXT NOT NULL,
                        PRIMARY KEY(`operationUuid`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_sync_operation_logs_startedAtEpochMs` ON `sync_operation_logs` (`startedAtEpochMs`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cloud_backups` (
                        `backupUuid` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `schemaVersion` INTEGER NOT NULL,
                        `progressionChecksum` TEXT NOT NULL,
                        `summaryText` TEXT NOT NULL,
                        `payloadJson` TEXT NOT NULL,
                        `totalXpSnapshot` INTEGER NOT NULL,
                        `playerLevelSnapshot` INTEGER NOT NULL,
                        `recordCount` INTEGER NOT NULL,
                        `statusName` TEXT NOT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        `uploadedAtEpochMs` INTEGER,
                        PRIMARY KEY(`backupUuid`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_cloud_backups_userId` ON `cloud_backups` (`userId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_cloud_backups_createdAtEpochMs` ON `cloud_backups` (`createdAtEpochMs`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cloud_sync_cursor` (
                        `entityTypeName` TEXT NOT NULL,
                        `lastPulledUpdatedAtIso` TEXT NOT NULL,
                        `lastPulledEpochMs` INTEGER NOT NULL,
                        `lastPageOffset` INTEGER NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`entityTypeName`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_7_8: Migration = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `intelligence_profiles` (
                        `id` INTEGER NOT NULL,
                        `sourceFingerprint` TEXT NOT NULL,
                        `evaluatedAtEpochMs` INTEGER NOT NULL,
                        `playerLevel` INTEGER NOT NULL,
                        `hunterRankName` TEXT NOT NULL,
                        `physicalActivityScore` INTEGER NOT NULL,
                        `academicActivityScore` INTEGER NOT NULL,
                        `disciplineConsistencyScore` INTEGER NOT NULL,
                        `rpgEngagementScore` INTEGER NOT NULL,
                        `behavioralPeakWindow` TEXT NOT NULL,
                        `averageSessionMinutes` INTEGER NOT NULL,
                        `measuredDataRatioPercent` INTEGER NOT NULL,
                        `activeDaysLast30` INTEGER NOT NULL,
                        `hasMinimumBaseline` INTEGER NOT NULL,
                        `dataCoverageNotesJoined` TEXT NOT NULL,
                        `dataCompletenessPercent` INTEGER NOT NULL,
                        `provenanceSummary` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `strength_weakness_findings` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `findingUuid` TEXT NOT NULL,
                        `kindName` TEXT NOT NULL,
                        `categoryName` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `evidenceSummary` TEXT NOT NULL,
                        `dateRangeLabel` TEXT NOT NULL,
                        `provenanceName` TEXT NOT NULL,
                        `confidenceName` TEXT NOT NULL,
                        `priorityRank` INTEGER NOT NULL,
                        `insufficientData` INTEGER NOT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_strength_weakness_findings_findingUuid` ON `strength_weakness_findings` (`findingUuid`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_strength_weakness_findings_priorityRank` ON `strength_weakness_findings` (`priorityRank`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `adaptive_daily_plans` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `planUuid` TEXT NOT NULL,
                        `targetDateEpochDay` INTEGER NOT NULL,
                        `generatedAtEpochMs` INTEGER NOT NULL,
                        `workloadLevelName` TEXT NOT NULL,
                        `totalPlannedMinutes` INTEGER NOT NULL,
                        `availableMinutes` INTEGER NOT NULL,
                        `isApproved` INTEGER NOT NULL,
                        `isPartialDay` INTEGER NOT NULL,
                        `notes` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_adaptive_daily_plans_planUuid` ON `adaptive_daily_plans` (`planUuid`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_adaptive_daily_plans_targetDateEpochDay` ON `adaptive_daily_plans` (`targetDateEpochDay`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `adaptive_plan_blocks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `blockUuid` TEXT NOT NULL,
                        `planUuid` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `module` TEXT NOT NULL,
                        `scheduledDateEpochDay` INTEGER NOT NULL,
                        `startMinuteOfDay` INTEGER NOT NULL,
                        `durationMinutes` INTEGER NOT NULL,
                        `priorityWeight` INTEGER NOT NULL,
                        `priorityScore` INTEGER NOT NULL,
                        `priorityReason` TEXT NOT NULL,
                        `dependencies` TEXT NOT NULL,
                        `statusName` TEXT NOT NULL,
                        `completedMinutes` INTEGER NOT NULL,
                        `isLocked` INTEGER NOT NULL,
                        `linkedSourceTypeName` TEXT NOT NULL,
                        `linkedSourceId` INTEGER NOT NULL,
                        `isRecoveryProtected` INTEGER NOT NULL,
                        FOREIGN KEY(`planUuid`) REFERENCES `adaptive_daily_plans`(`planUuid`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_adaptive_plan_blocks_blockUuid` ON `adaptive_plan_blocks` (`blockUuid`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_adaptive_plan_blocks_planUuid` ON `adaptive_plan_blocks` (`planUuid`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_adaptive_plan_blocks_scheduledDateEpochDay_startMinuteOfDay` ON `adaptive_plan_blocks` (`scheduledDateEpochDay`, `startMinuteOfDay`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `workload_assessments` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `assessmentUuid` TEXT NOT NULL,
                        `dateEpochDay` INTEGER NOT NULL,
                        `workloadLevelName` TEXT NOT NULL,
                        `loadScore` INTEGER NOT NULL,
                        `physicalLoadMinutes48h` INTEGER NOT NULL,
                        `academicLoadMinutesToday` INTEGER NOT NULL,
                        `activeCommitmentsCount` INTEGER NOT NULL,
                        `overdueItemsCount` INTEGER NOT NULL,
                        `scheduleConflictCount` INTEGER NOT NULL,
                        `recoveryStateName` TEXT NOT NULL,
                        `adjustmentDirective` TEXT NOT NULL,
                        `preservesRestWithoutPunishment` INTEGER NOT NULL,
                        `provenanceName` TEXT NOT NULL,
                        `evaluatedAtEpochMs` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_workload_assessments_assessmentUuid` ON `workload_assessments` (`assessmentUuid`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_workload_assessments_dateEpochDay` ON `workload_assessments` (`dateEpochDay`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `consistency_predictions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `predictionUuid` TEXT NOT NULL,
                        `domainLabel` TEXT NOT NULL,
                        `horizonDays` INTEGER NOT NULL,
                        `currentConsistencyPercent` INTEGER NOT NULL,
                        `predictedConsistencyPercent` INTEGER NOT NULL,
                        `slopePerWeek` REAL NOT NULL,
                        `confidenceName` TEXT NOT NULL,
                        `insufficientData` INTEGER NOT NULL,
                        `limitationNote` TEXT NOT NULL,
                        `atRiskGoalsJoined` TEXT NOT NULL,
                        `riskLevelLabel` TEXT NOT NULL,
                        `trendSeriesEncoded` TEXT NOT NULL,
                        `provenanceName` TEXT NOT NULL,
                        `evaluatedAtEpochMs` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_consistency_predictions_predictionUuid` ON `consistency_predictions` (`predictionUuid`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `personalized_recommendations` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `recUuid` TEXT NOT NULL,
                        `categoryName` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `rationale` TEXT NOT NULL,
                        `expectedImpact` TEXT NOT NULL,
                        `actionLabel` TEXT NOT NULL,
                        `deepLinkRoute` TEXT NOT NULL,
                        `priorityScore` INTEGER NOT NULL,
                        `statusName` TEXT NOT NULL,
                        `provenanceName` TEXT NOT NULL,
                        `confidenceName` TEXT NOT NULL,
                        `expiresAtEpochMs` INTEGER NOT NULL,
                        `cooldownUntilEpochMs` INTEGER NOT NULL,
                        `sourceFingerprint` TEXT NOT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        `respondedAtEpochMs` INTEGER
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_personalized_recommendations_recUuid` ON `personalized_recommendations` (`recUuid`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_personalized_recommendations_statusName` ON `personalized_recommendations` (`statusName`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_personalized_recommendations_priorityScore` ON `personalized_recommendations` (`priorityScore`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `recommendation_feedback` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `recUuid` TEXT NOT NULL,
                        `categoryName` TEXT NOT NULL,
                        `actionName` TEXT NOT NULL,
                        `feedbackNote` TEXT NOT NULL,
                        `timestampEpochMs` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recommendation_feedback_recUuid` ON `recommendation_feedback` (`recUuid`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recommendation_feedback_timestampEpochMs` ON `recommendation_feedback` (`timestampEpochMs`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `development_roadmaps` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `roadmapUuid` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `horizonName` TEXT NOT NULL,
                        `visionSummary` TEXT NOT NULL,
                        `focusDomainsJoined` TEXT NOT NULL,
                        `dependenciesSummary` TEXT NOT NULL,
                        `riskNotes` TEXT NOT NULL,
                        `nextReviewDateEpochDay` INTEGER NOT NULL,
                        `isApproved` INTEGER NOT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_development_roadmaps_roadmapUuid` ON `development_roadmaps` (`roadmapUuid`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_development_roadmaps_updatedAtEpochMs` ON `development_roadmaps` (`updatedAtEpochMs`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `roadmap_milestones` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `milestoneUuid` TEXT NOT NULL,
                        `roadmapUuid` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `domain` TEXT NOT NULL,
                        `targetDayOffset` INTEGER NOT NULL,
                        `targetDateEpochDay` INTEGER NOT NULL,
                        `isCompleted` INTEGER NOT NULL,
                        `isUserEdited` INTEGER NOT NULL,
                        `orderIndex` INTEGER NOT NULL,
                        `notes` TEXT NOT NULL,
                        FOREIGN KEY(`roadmapUuid`) REFERENCES `development_roadmaps`(`roadmapUuid`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_roadmap_milestones_milestoneUuid` ON `roadmap_milestones` (`milestoneUuid`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_roadmap_milestones_roadmapUuid` ON `roadmap_milestones` (`roadmapUuid`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `intelligence_reports` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `reportUuid` TEXT NOT NULL,
                        `periodTypeName` TEXT NOT NULL,
                        `periodLabel` TEXT NOT NULL,
                        `periodStartEpochDay` INTEGER NOT NULL,
                        `periodEndEpochDay` INTEGER NOT NULL,
                        `activeDaysObserved` INTEGER NOT NULL,
                        `isPartialDataWindow` INTEGER NOT NULL,
                        `measuredStatsSummary` TEXT NOT NULL,
                        `selfReportedStatsSummary` TEXT NOT NULL,
                        `workoutsCount` INTEGER NOT NULL,
                        `joggingMinutes` INTEGER NOT NULL,
                        `studyMinutes` INTEGER NOT NULL,
                        `revisionsCount` INTEGER NOT NULL,
                        `habitsCompletedCount` INTEGER NOT NULL,
                        `tasksCompletedCount` INTEGER NOT NULL,
                        `dungeonsClearedCount` INTEGER NOT NULL,
                        `xpGainedWindow` INTEGER NOT NULL,
                        `highlightsJoined` TEXT NOT NULL,
                        `risksJoined` TEXT NOT NULL,
                        `aiNarrativeSummary` TEXT,
                        `aiUsedFallback` INTEGER NOT NULL,
                        `generatedAtEpochMs` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_intelligence_reports_reportUuid` ON `intelligence_reports` (`reportUuid`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_intelligence_reports_periodTypeName_generatedAtEpochMs` ON `intelligence_reports` (`periodTypeName`, `generatedAtEpochMs`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `intelligence_preferences` (
                        `id` INTEGER NOT NULL,
                        `planningStyleName` TEXT NOT NULL,
                        `preferredStudyHour` INTEGER NOT NULL,
                        `preferredWorkoutHour` INTEGER NOT NULL,
                        `maxDailyFocusMinutes` INTEGER NOT NULL,
                        `maxDailyWorkoutMinutes` INTEGER NOT NULL,
                        `maxRecommendationsPerDay` INTEGER NOT NULL,
                        `notificationIntensityName` TEXT NOT NULL,
                        `enableProactivePlanning` INTEGER NOT NULL,
                        `enableWeeklyReports` INTEGER NOT NULL,
                        `enableMonthlyReports` INTEGER NOT NULL,
                        `enableAdaptiveDungeons` INTEGER NOT NULL,
                        `shareIntelligenceWithCloudAi` INTEGER NOT NULL,
                        `sharePhysicalWithIntelligence` INTEGER NOT NULL,
                        `shareAcademicWithIntelligence` INTEGER NOT NULL,
                        `shareLifeWithIntelligence` INTEGER NOT NULL,
                        `shareRpgWithIntelligence` INTEGER NOT NULL,
                        `restDaysCsv` TEXT NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `advisor_consultations` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `consultationUuid` TEXT NOT NULL,
                        `question` TEXT NOT NULL,
                        `deterministicSummary` TEXT NOT NULL,
                        `aiResponse` TEXT NOT NULL,
                        `provenanceTagsJoined` TEXT NOT NULL,
                        `confidenceName` TEXT NOT NULL,
                        `connectionStatusName` TEXT NOT NULL,
                        `usedFallback` INTEGER NOT NULL,
                        `limitationNotesJoined` TEXT NOT NULL,
                        `timestampEpochMs` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_advisor_consultations_consultationUuid` ON `advisor_consultations` (`consultationUuid`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_advisor_consultations_timestampEpochMs` ON `advisor_consultations` (`timestampEpochMs`)")
            }
        }

        val MIGRATION_8_9: Migration = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_logs_timestampEpochMs` ON `activity_logs` (`timestampEpochMs`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `system_events` (
                        `eventId` TEXT NOT NULL,
                        `eventTypeName` TEXT NOT NULL,
                        `timestampEpochMs` INTEGER NOT NULL,
                        `sourceModule` TEXT NOT NULL,
                        `relatedEntityId` TEXT NOT NULL,
                        `metadataJson` TEXT NOT NULL,
                        `schemaVersion` INTEGER NOT NULL,
                        `processingStatusName` TEXT NOT NULL,
                        `retryCount` INTEGER NOT NULL,
                        `errorMessage` TEXT,
                        `processedAtEpochMs` INTEGER,
                        `correlationId` TEXT NOT NULL,
                        PRIMARY KEY(`eventId`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_system_events_processingStatusName` ON `system_events` (`processingStatusName`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_system_events_timestampEpochMs` ON `system_events` (`timestampEpochMs`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_system_events_eventTypeName` ON `system_events` (`eventTypeName`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `automation_jobs` (
                        `automationId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `triggerTypeName` TEXT NOT NULL,
                        `frequencyLabel` TEXT NOT NULL,
                        `constraintsSummary` TEXT NOT NULL,
                        `isEnabled` INTEGER NOT NULL,
                        `lastExecutionEpochMs` INTEGER,
                        `nextEligibleEpochMs` INTEGER,
                        `currentStatusName` TEXT NOT NULL,
                        `retryCount` INTEGER NOT NULL,
                        `lastFailureReason` TEXT,
                        `executionCount` INTEGER NOT NULL,
                        PRIMARY KEY(`automationId`)
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `automation_execution_logs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `executionUuid` TEXT NOT NULL,
                        `automationId` TEXT NOT NULL,
                        `triggerSource` TEXT NOT NULL,
                        `startedAtEpochMs` INTEGER NOT NULL,
                        `completedAtEpochMs` INTEGER NOT NULL,
                        `statusName` TEXT NOT NULL,
                        `summaryMessage` TEXT NOT NULL,
                        `retryCount` INTEGER NOT NULL,
                        `failureReason` TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_automation_execution_logs_automationId` ON `automation_execution_logs` (`automationId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_automation_execution_logs_startedAtEpochMs` ON `automation_execution_logs` (`startedAtEpochMs`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `daily_cycle_executions` (
                        `epochDay` INTEGER NOT NULL,
                        `timezoneId` TEXT NOT NULL,
                        `executedAtEpochMs` INTEGER NOT NULL,
                        `triggerSource` TEXT NOT NULL,
                        `dailyQuestsResetCount` INTEGER NOT NULL,
                        `missedDaysCaughtUp` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        PRIMARY KEY(`epochDay`)
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `daily_execution_items` (
                        `executionKey` TEXT NOT NULL,
                        `dateEpochDay` INTEGER NOT NULL,
                        `activityTypeName` TEXT NOT NULL,
                        `sourceRecordId` INTEGER NOT NULL,
                        `statusName` TEXT NOT NULL,
                        `actualStartEpochMs` INTEGER,
                        `actualCompletionEpochMs` INTEGER,
                        `completedMinutes` INTEGER NOT NULL,
                        `plannedStartMinuteOverride` INTEGER,
                        `evidenceReference` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`executionKey`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_daily_execution_items_dateEpochDay` ON `daily_execution_items` (`dateEpochDay`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `system_integrity_findings` (
                        `findingUuid` TEXT NOT NULL,
                        `categoryName` TEXT NOT NULL,
                        `severityName` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `affectedTable` TEXT NOT NULL,
                        `affectedRecordKey` TEXT NOT NULL,
                        `repairProposal` TEXT NOT NULL,
                        `isDestructiveRepair` INTEGER NOT NULL,
                        `statusName` TEXT NOT NULL,
                        `detectedAtEpochMs` INTEGER NOT NULL,
                        `resolvedAtEpochMs` INTEGER,
                        PRIMARY KEY(`findingUuid`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_system_integrity_findings_statusName` ON `system_integrity_findings` (`statusName`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `system_health_snapshots` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `overallStatusName` TEXT NOT NULL,
                        `componentsSummaryJson` TEXT NOT NULL,
                        `evaluatedAtEpochMs` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_system_health_snapshots_evaluatedAtEpochMs` ON `system_health_snapshots` (`evaluatedAtEpochMs`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `recovery_operation_records` (
                        `operationUuid` TEXT NOT NULL,
                        `operationType` TEXT NOT NULL,
                        `reason` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `details` TEXT NOT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        `completedAtEpochMs` INTEGER,
                        PRIMARY KEY(`operationUuid`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recovery_operation_records_createdAtEpochMs` ON `recovery_operation_records` (`createdAtEpochMs`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `system_preferences` (
                        `id` INTEGER NOT NULL,
                        `enableAutoProgressionEvents` INTEGER NOT NULL,
                        `enableAutoIntelligenceRefresh` INTEGER NOT NULL,
                        `enableDailyCycleAutomation` INTEGER NOT NULL,
                        `enableCloudAutoSync` INTEGER NOT NULL,
                        `enablePerformanceCaching` INTEGER NOT NULL,
                        `enableBackgroundNotifications` INTEGER NOT NULL,
                        `preferredTimezoneId` TEXT NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_9_10: Migration = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `system_experience_preferences` (
                        `id` INTEGER NOT NULL,
                        `hasCompletedAwakening` INTEGER NOT NULL,
                        `lastCelebratedLevel` INTEGER NOT NULL,
                        `lastCelebratedRankName` TEXT NOT NULL,
                        `ttsEnabled` INTEGER NOT NULL,
                        `ttsSpeechRate` REAL NOT NULL,
                        `ttsPitch` REAL NOT NULL,
                        `ttsSpeakQuestUpdates` INTEGER NOT NULL,
                        `ttsSpeakLevelAndRank` INTEGER NOT NULL,
                        `ttsSpeakDungeonsAndBosses` INTEGER NOT NULL,
                        `ttsSpeakRecoveryWarnings` INTEGER NOT NULL,
                        `ttsSpeakRecommendations` INTEGER NOT NULL,
                        `ttsSpeakDailySummary` INTEGER NOT NULL,
                        `voiceCommandsEnabled` INTEGER NOT NULL,
                        `voiceLocaleTag` TEXT NOT NULL,
                        `animationsEnabled` INTEGER NOT NULL,
                        `reducedMotionEnabled` INTEGER NOT NULL,
                        `soundEffectsEnabled` INTEGER NOT NULL,
                        `hapticFeedbackEnabled` INTEGER NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `system_announcement_history` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `announcementKey` TEXT NOT NULL,
                        `eventId` TEXT NOT NULL,
                        `categoryName` TEXT NOT NULL,
                        `channelId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `message` TEXT NOT NULL,
                        `voiceSummary` TEXT NOT NULL,
                        `deepLinkRoute` TEXT NOT NULL,
                        `actionLabel` TEXT NOT NULL,
                        `priorityName` TEXT NOT NULL,
                        `wasSpoken` INTEGER NOT NULL,
                        `wasDispatchedOs` INTEGER NOT NULL,
                        `isRead` INTEGER NOT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_system_announcement_history_announcementKey` ON `system_announcement_history` (`announcementKey`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_system_announcement_history_createdAtEpochMs` ON `system_announcement_history` (`createdAtEpochMs`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `system_celebration_history` (
                        `celebrationId` TEXT NOT NULL,
                        `typeName` TEXT NOT NULL,
                        `previousLevel` INTEGER NOT NULL,
                        `newLevel` INTEGER NOT NULL,
                        `previousRankName` TEXT NOT NULL,
                        `newRankName` TEXT NOT NULL,
                        `titleUnlocked` TEXT NOT NULL,
                        `attributeDeltaSummary` TEXT NOT NULL,
                        `headline` TEXT NOT NULL,
                        `message` TEXT NOT NULL,
                        `isAcknowledged` INTEGER NOT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`celebrationId`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_system_celebration_history_createdAtEpochMs` ON `system_celebration_history` (`createdAtEpochMs`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `voice_command_history` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `transcript` TEXT NOT NULL,
                        `matchedActionName` TEXT NOT NULL,
                        `outcome` TEXT NOT NULL,
                        `executedAtEpochMs` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_voice_command_history_executedAtEpochMs` ON `voice_command_history` (`executedAtEpochMs`)")

                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `system_experience_preferences` (
                        `id`, `hasCompletedAwakening`, `lastCelebratedLevel`, `lastCelebratedRankName`,
                        `ttsEnabled`, `ttsSpeechRate`, `ttsPitch`,
                        `ttsSpeakQuestUpdates`, `ttsSpeakLevelAndRank`, `ttsSpeakDungeonsAndBosses`,
                        `ttsSpeakRecoveryWarnings`, `ttsSpeakRecommendations`, `ttsSpeakDailySummary`,
                        `voiceCommandsEnabled`, `voiceLocaleTag`, `animationsEnabled`, `reducedMotionEnabled`,
                        `soundEffectsEnabled`, `hapticFeedbackEnabled`, `updatedAtEpochMs`
                    )
                    SELECT 1, 0, COALESCE(`level`, 1), COALESCE(`rankName`, 'E_RANK'),
                           1, 1.0, 1.0,
                           1, 1, 1,
                           1, 0, 1,
                           1, 'en-US', 1, 0,
                           1, 1, 0
                    FROM `player_profile` WHERE `id` = 1
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `system_experience_preferences` VALUES (
                        1, 0, 1, 'E_RANK',
                        1, 1.0, 1.0,
                        1, 1, 1,
                        1, 0, 1,
                        1, 'en-US', 1, 0,
                        1, 1, 0
                    )
                    """.trimIndent()
                )
            }
        }

        // =====================================================================
        // MIGRATION 10 → 11: Shadow Monarch System
        // =====================================================================
        val MIGRATION_10_11: Migration = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Shadow Monarch Profile (singleton, id = 1)
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `shadow_monarch_profile` (
                        `id` INTEGER NOT NULL,
                        `monarchLevel` INTEGER NOT NULL,
                        `monarchXp` INTEGER NOT NULL,
                        `monarchRankName` TEXT NOT NULL,
                        `monarchAuthority` INTEGER NOT NULL,
                        `armyCapacity` INTEGER NOT NULL,
                        `activeShadowCount` INTEGER NOT NULL,
                        `totalShadowsRecruited` INTEGER NOT NULL,
                        `highestShadowRankName` TEXT NOT NULL,
                        `totalEvolutions` INTEGER NOT NULL,
                        `shadowArmyPowerRating` INTEGER NOT NULL,
                        `unlockedMonarchSkillIdsJson` TEXT NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                // Seed default profile
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `shadow_monarch_profile` VALUES (
                        1, 1, 0, 'AWAKENING', 0, 10, 0, 0, 'E_RANK', 0, 0, '["monarch_shadow_extraction"]', 0
                    )
                    """.trimIndent()
                )

                // 2. Shadows (recruited shadow army members)
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `shadows` (
                        `shadowId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `shadowClassName` TEXT NOT NULL,
                        `rarityName` TEXT NOT NULL,
                        `currentRankName` TEXT NOT NULL,
                        `level` INTEGER NOT NULL,
                        `shadowXp` INTEGER NOT NULL,
                        `statPower` INTEGER NOT NULL,
                        `statEndurance` INTEGER NOT NULL,
                        `statFocus` INTEGER NOT NULL,
                        `statDiscipline` INTEGER NOT NULL,
                        `statWisdom` INTEGER NOT NULL,
                        `statAdaptability` INTEGER NOT NULL,
                        `evolutionStageName` TEXT NOT NULL,
                        `passiveAbility` TEXT NOT NULL,
                        `activeSkillIdsJson` TEXT NOT NULL,
                        `recruitmentSource` TEXT NOT NULL,
                        `extractionEventId` TEXT NOT NULL,
                        `extractedAtEpochMs` INTEGER NOT NULL,
                        `evolutionHistoryJson` TEXT NOT NULL,
                        `deploymentStatusName` TEXT NOT NULL,
                        `equippedFormationId` TEXT,
                        `isRetired` INTEGER NOT NULL,
                        `notes` TEXT NOT NULL,
                        PRIMARY KEY(`shadowId`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shadows_shadowClassName` ON `shadows` (`shadowClassName`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shadows_currentRankName` ON `shadows` (`currentRankName`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shadows_rarityName` ON `shadows` (`rarityName`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shadows_deploymentStatusName` ON `shadows` (`deploymentStatusName`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shadows_isRetired` ON `shadows` (`isRetired`)")

                // 3. Shadow Extractions (audit log, idempotency guard)
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `shadow_extractions` (
                        `extractionId` TEXT NOT NULL,
                        `sourceTypeName` TEXT NOT NULL,
                        `sourceEventId` TEXT NOT NULL,
                        `outcomeName` TEXT NOT NULL,
                        `extractedShadowId` TEXT,
                        `shadowName` TEXT,
                        `shadowClassName` TEXT,
                        `shadowRarityName` TEXT,
                        `determinismSeed` INTEGER NOT NULL,
                        `evidenceSummary` TEXT NOT NULL,
                        `timestampEpochMs` INTEGER NOT NULL,
                        `isConfirmed` INTEGER NOT NULL,
                        `monarchXpAwarded` INTEGER NOT NULL,
                        PRIMARY KEY(`extractionId`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_shadow_extractions_extractionId` ON `shadow_extractions` (`extractionId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_shadow_extractions_sourceTypeName_sourceEventId` ON `shadow_extractions` (`sourceTypeName`, `sourceEventId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shadow_extractions_timestampEpochMs` ON `shadow_extractions` (`timestampEpochMs`)")

                // 4. Shadow Skill Unlocks (per-shadow)
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `shadow_skill_unlocks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `shadowId` TEXT NOT NULL,
                        `skillId` TEXT NOT NULL,
                        `unlockedAtEpochMs` INTEGER NOT NULL,
                        `evidenceNote` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_shadow_skill_unlocks_shadowId_skillId` ON `shadow_skill_unlocks` (`shadowId`, `skillId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shadow_skill_unlocks_shadowId` ON `shadow_skill_unlocks` (`shadowId`)")

                // 5. Shadow Deployments
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `shadow_deployments` (
                        `deploymentId` TEXT NOT NULL,
                        `shadowId` TEXT NOT NULL,
                        `shadowName` TEXT NOT NULL,
                        `categoryName` TEXT NOT NULL,
                        `role` TEXT NOT NULL,
                        `assignedAtEpochMs` INTEGER NOT NULL,
                        `recalledAtEpochMs` INTEGER,
                        `statusName` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        PRIMARY KEY(`deploymentId`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_shadow_deployments_deploymentId` ON `shadow_deployments` (`deploymentId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shadow_deployments_shadowId` ON `shadow_deployments` (`shadowId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shadow_deployments_categoryName` ON `shadow_deployments` (`categoryName`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shadow_deployments_statusName` ON `shadow_deployments` (`statusName`)")

                // 6. Shadow Formations
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `shadow_formations` (
                        `formationId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `formationTypeName` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `maxSlots` INTEGER NOT NULL,
                        `memberShadowIdsJson` TEXT NOT NULL,
                        `isActive` INTEGER NOT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`formationId`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_shadow_formations_formationId` ON `shadow_formations` (`formationId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shadow_formations_isActive` ON `shadow_formations` (`isActive`)")

                // 7. Monarch Skill Unlocks
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `monarch_skill_unlocks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `skillId` TEXT NOT NULL,
                        `unlockedAtEpochMs` INTEGER NOT NULL,
                        `evidenceNote` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_monarch_skill_unlocks_skillId` ON `monarch_skill_unlocks` (`skillId`)")
                // Auto-seed Shadow Extraction skill as unlocked (always available from level 1)
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `monarch_skill_unlocks` (`skillId`, `unlockedAtEpochMs`, `evidenceNote`)
                    VALUES ('monarch_shadow_extraction', ${System.currentTimeMillis()}, 'Shadow Extraction unlocked at Monarch Level 1')
                    """.trimIndent()
                )

                // 8. Shadow Quest Progress
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `shadow_quest_progress` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `questId` TEXT NOT NULL,
                        `currentValue` INTEGER NOT NULL,
                        `isCompleted` INTEGER NOT NULL,
                        `rewardClaimedAtEpochMs` INTEGER,
                        `completedAtEpochMs` INTEGER
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_shadow_quest_progress_questId` ON `shadow_quest_progress` (`questId`)")

                // 9. Shadow Resource Transactions (ledger)
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `shadow_resource_transactions` (
                        `transactionId` TEXT NOT NULL,
                        `resourceTypeName` TEXT NOT NULL,
                        `amount` INTEGER NOT NULL,
                        `transactionTypeName` TEXT NOT NULL,
                        `sourceEventId` TEXT NOT NULL,
                        `sourceDescription` TEXT NOT NULL,
                        `balanceAfter` INTEGER NOT NULL,
                        `timestampEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`transactionId`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_shadow_resource_transactions_transactionId` ON `shadow_resource_transactions` (`transactionId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_shadow_resource_transactions_sourceEventId_resourceTypeName_transactionTypeName` ON `shadow_resource_transactions` (`sourceEventId`, `resourceTypeName`, `transactionTypeName`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shadow_resource_transactions_resourceTypeName` ON `shadow_resource_transactions` (`resourceTypeName`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shadow_resource_transactions_timestampEpochMs` ON `shadow_resource_transactions` (`timestampEpochMs`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shadow_resource_transactions_sourceEventId` ON `shadow_resource_transactions` (`sourceEventId`)")
            }
        }

        val MIGRATION_11_12: Migration = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Social Profiles
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `social_profiles` (
                        `accountOwnerId` TEXT NOT NULL,
                        `overallSocialMasteryStage` TEXT NOT NULL,
                        `totalPracticeMinutes` INTEGER NOT NULL DEFAULT 0,
                        `totalScenariosCleared` INTEGER NOT NULL DEFAULT 0,
                        `totalAssessmentsCompleted` INTEGER NOT NULL DEFAULT 0,
                        `averageRubricScore` REAL NOT NULL DEFAULT 0.0,
                        `lastActiveEpochMs` INTEGER NOT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        `syncStatus` TEXT NOT NULL DEFAULT 'SYNCED',
                        `lastSyncedEpochMs` INTEGER,
                        PRIMARY KEY(`accountOwnerId`)
                    )
                    """.trimIndent()
                )

                // 2. Social Module Progress
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `social_module_progress` (
                        `accountOwnerId` TEXT NOT NULL,
                        `moduleName` TEXT NOT NULL,
                        `masteryStage` TEXT NOT NULL,
                        `masteryStageOrdinal` INTEGER NOT NULL DEFAULT 0,
                        `practiceSeconds` INTEGER NOT NULL DEFAULT 0,
                        `assessmentsCompleted` INTEGER NOT NULL DEFAULT 0,
                        `rollingAverageScore` REAL NOT NULL DEFAULT 0.0,
                        `lastScoreStandardDeviation` REAL NOT NULL DEFAULT 0.0,
                        `consecutivePassingScores` INTEGER NOT NULL DEFAULT 0,
                        `isStale` INTEGER NOT NULL DEFAULT 0,
                        `decayedFromStage` TEXT,
                        `consecutiveRestorationPassingScores` INTEGER NOT NULL DEFAULT 0,
                        `lastAssessedEpochMs` INTEGER NOT NULL,
                        `lastDecayEpochMs` INTEGER,
                        `nextDecayEligibleEpochMs` INTEGER,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`accountOwnerId`, `moduleName`),
                        FOREIGN KEY(`accountOwnerId`) REFERENCES `social_profiles`(`accountOwnerId`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_social_module_progress_accountOwnerId` ON `social_module_progress` (`accountOwnerId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_social_module_progress_moduleName` ON `social_module_progress` (`moduleName`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_social_module_progress_masteryStageOrdinal` ON `social_module_progress` (`masteryStageOrdinal`)")

                // 3. Social Scenarios (Static Catalogue)
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `social_scenarios` (
                        `scenarioId` TEXT NOT NULL,
                        `moduleName` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `difficultyTier` TEXT NOT NULL,
                        `contextPrompt` TEXT NOT NULL,
                        `npcRole` TEXT NOT NULL,
                        `targetLearningObjective` TEXT NOT NULL,
                        `minPassingScore` INTEGER NOT NULL DEFAULT 70,
                        `isDeprecated` INTEGER NOT NULL DEFAULT 0,
                        `version` INTEGER NOT NULL DEFAULT 1,
                        PRIMARY KEY(`scenarioId`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_social_scenarios_moduleName` ON `social_scenarios` (`moduleName`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_social_scenarios_difficultyTier` ON `social_scenarios` (`difficultyTier`)")

                // 4. Social Practice Sessions
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `social_practice_sessions` (
                        `sessionId` TEXT NOT NULL,
                        `accountOwnerId` TEXT NOT NULL,
                        `moduleName` TEXT NOT NULL,
                        `activityKind` TEXT NOT NULL,
                        `sessionStatus` TEXT NOT NULL,
                        `durationSeconds` INTEGER NOT NULL,
                        `startedEpochMs` INTEGER NOT NULL,
                        `completedEpochMs` INTEGER,
                        `transcriptText` TEXT,
                        `hasAudioRecording` INTEGER NOT NULL DEFAULT 0,
                        `audioFileName` TEXT,
                        `isCloudTranscriptSyncAllowed` INTEGER NOT NULL DEFAULT 0,
                        `evidenceSourceType` TEXT NOT NULL,
                        `verificationStatus` TEXT NOT NULL,
                        `idempotencyKey` TEXT NOT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`sessionId`),
                        FOREIGN KEY(`accountOwnerId`) REFERENCES `social_profiles`(`accountOwnerId`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_social_practice_sessions_accountOwnerId` ON `social_practice_sessions` (`accountOwnerId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_social_practice_sessions_moduleName` ON `social_practice_sessions` (`moduleName`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_social_practice_sessions_idempotencyKey` ON `social_practice_sessions` (`idempotencyKey`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_social_practice_sessions_startedEpochMs` ON `social_practice_sessions` (`startedEpochMs`)")

                // 5. Social Scenario Attempts
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `social_scenario_attempts` (
                        `attemptId` TEXT NOT NULL,
                        `accountOwnerId` TEXT NOT NULL,
                        `scenarioId` TEXT NOT NULL,
                        `scenarioTitle` TEXT NOT NULL,
                        `moduleName` TEXT NOT NULL,
                        `userResponseText` TEXT NOT NULL,
                        `evaluationScore` INTEGER NOT NULL,
                        `isPassed` INTEGER NOT NULL,
                        `strengthsFeedbackJson` TEXT NOT NULL,
                        `improvementAreasJson` TEXT NOT NULL,
                        `evidenceSourceType` TEXT NOT NULL,
                        `verificationStatus` TEXT NOT NULL,
                        `idempotencyKey` TEXT NOT NULL,
                        `completedEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`attemptId`),
                        FOREIGN KEY(`accountOwnerId`) REFERENCES `social_profiles`(`accountOwnerId`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`scenarioId`) REFERENCES `social_scenarios`(`scenarioId`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_social_scenario_attempts_accountOwnerId` ON `social_scenario_attempts` (`accountOwnerId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_social_scenario_attempts_scenarioId` ON `social_scenario_attempts` (`scenarioId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_social_scenario_attempts_moduleName` ON `social_scenario_attempts` (`moduleName`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_social_scenario_attempts_idempotencyKey` ON `social_scenario_attempts` (`idempotencyKey`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_social_scenario_attempts_completedEpochMs` ON `social_scenario_attempts` (`completedEpochMs`)")

                // 6. Social Assessments
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `social_assessments` (
                        `assessmentId` TEXT NOT NULL,
                        `accountOwnerId` TEXT NOT NULL,
                        `moduleName` TEXT NOT NULL,
                        `assessmentType` TEXT NOT NULL,
                        `overallScore` INTEGER NOT NULL,
                        `clarityScore` INTEGER NOT NULL,
                        `empathyScore` INTEGER NOT NULL,
                        `assertivenessScore` INTEGER NOT NULL,
                        `coherenceScore` INTEGER NOT NULL,
                        `qualitativeAnalysis` TEXT NOT NULL,
                        `actionableRecommendationsJson` TEXT NOT NULL,
                        `assessedEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`assessmentId`),
                        FOREIGN KEY(`accountOwnerId`) REFERENCES `social_profiles`(`accountOwnerId`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_social_assessments_accountOwnerId` ON `social_assessments` (`accountOwnerId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_social_assessments_moduleName` ON `social_assessments` (`moduleName`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_social_assessments_assessedEpochMs` ON `social_assessments` (`assessedEpochMs`)")
            }
        }

        val MIGRATION_12_13: Migration = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Real-World Social Mission Records (Milestone 12 - Phase 4)
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `social_mission_records` (
                        `recordId` TEXT NOT NULL,
                        `accountOwnerId` TEXT NOT NULL,
                        `missionId` TEXT NOT NULL,
                        `moduleName` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `difficultyRank` TEXT NOT NULL,
                        `lifecycleStatus` TEXT NOT NULL,
                        `startedEpochMs` INTEGER NOT NULL,
                        `submittedEpochMs` INTEGER,
                        `completedEpochMs` INTEGER,
                        `evidenceType` TEXT NOT NULL,
                        `reflectionText` TEXT,
                        `humanConfirmingRole` TEXT,
                        `artifactNote` TEXT,
                        `aiFeedbackNotes` TEXT,
                        `evaluationRubricScore` INTEGER,
                        `xpAwarded` INTEGER NOT NULL DEFAULT 0,
                        `verificationStatus` TEXT NOT NULL,
                        `idempotencyKey` TEXT NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`recordId`),
                        FOREIGN KEY(`accountOwnerId`) REFERENCES `social_profiles`(`accountOwnerId`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_social_mission_records_accountOwnerId` ON `social_mission_records` (`accountOwnerId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_social_mission_records_missionId` ON `social_mission_records` (`missionId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_social_mission_records_moduleName` ON `social_mission_records` (`moduleName`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_social_mission_records_lifecycleStatus` ON `social_mission_records` (`lifecycleStatus`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_social_mission_records_idempotencyKey` ON `social_mission_records` (`idempotencyKey`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_social_mission_records_startedEpochMs` ON `social_mission_records` (`startedEpochMs`)")
            }
        }

        fun getInstance(context: Context): SoloLevelingDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SoloLevelingDatabase::class.java,
                    "solo_leveling_reality_os.db"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10,
                        MIGRATION_10_11,
                        MIGRATION_11_12,
                        MIGRATION_12_13
                    )
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

object DatabaseInitializer {
    suspend fun ensureSeeded(db: SoloLevelingDatabase) {
        val existingPlayer = db.playerDao().getPlayerOnce()
        if (existingPlayer == null) {
            db.playerDao().upsertPlayer(PlayerProfile().toEntity())
        }

        val existingPhysical = db.physicalStateDao().getPhysicalStateOnce()
        if (existingPhysical == null) {
            db.physicalStateDao().upsertPhysicalState(PhysicalDevelopmentState().toEntity())
        }

        if (db.importedActivityDao().getSyncStateOnce() == null) {
            db.importedActivityDao().upsertSyncState(HealthConnectSyncState().toEntity())
        }

        if (db.aiConfigDao().getModelPreferencesOnce() == null) {
            db.aiConfigDao().upsertModelPreferences(AiModelPreferenceEntity())
        }
        if (db.aiConfigDao().getConsentRecordOnce() == null) {
            db.aiConfigDao().upsertConsentRecord(AiConsentRecordEntity())
        }
        if (db.aiConfigDao().getNotificationPreferencesOnce() == null) {
            db.aiConfigDao().upsertNotificationPreferences(NotificationPreferenceEntity())
        }
        if (db.intelligenceReportDao().getPreferencesOnce() == null) {
            db.intelligenceReportDao().upsertPreferences(IntelligencePreferencesEntity())
        }
        if (db.systemIntegrityHealthDao().getPreferencesOnce() == null) {
            db.systemIntegrityHealthDao().upsertPreferences(SystemPreferencesEntity())
        }
        if (db.systemExperienceDao().getPreferencesOnce() == null) {
            db.systemExperienceDao().upsertPreferences(SystemExperiencePreferenceEntity())
        }

        if (db.recoveryAssessmentDao().getLatestOnce() == null) {
            val initialAssessment = RecoveryEngine.assess(
                measuredLoadMinutes48h = 0,
                measuredLoadMinutes7d = 0,
                measuredSessions7d = 0,
                consecutiveActiveDays = 0
            )
            db.recoveryAssessmentDao().insert(initialAssessment.toEntity())
        }

        if (db.questDao().getQuestCount() == 0) {
            val now = System.currentTimeMillis()
            val initialQuests = listOf(
                Quest(
                    title = "Daily Jogging Protocol",
                    description = "Complete your 30-minute aerobic jogging target at steady pace.",
                    type = QuestType.DAILY,
                    category = QuestCategory.PHYSICAL,
                    targetValue = 30,
                    currentValue = 0,
                    unit = "min",
                    xpReward = 45,
                    primaryAttribute = AttributeType.VIT,
                    primaryAttrXpReward = 40,
                    secondaryAttribute = AttributeType.AGI,
                    secondaryAttrXpReward = 20,
                    verificationStatus = VerificationStatus.SELF_REPORTED,
                    createdAtEpochMs = now
                ),
                Quest(
                    title = "Deep Focus Study Block",
                    description = "Execute 45 minutes of uninterrupted technical or academic study.",
                    type = QuestType.DAILY,
                    category = QuestCategory.INTELLECT,
                    targetValue = 45,
                    currentValue = 0,
                    unit = "min",
                    xpReward = 40,
                    primaryAttribute = AttributeType.INT,
                    primaryAttrXpReward = 45,
                    secondaryAttribute = AttributeType.DIS,
                    secondaryAttrXpReward = 20,
                    verificationStatus = VerificationStatus.SELF_REPORTED,
                    createdAtEpochMs = now - 1000
                ),
                Quest(
                    title = "Hydration & Mobility Reset",
                    description = "Log 15 minutes of mobility stretching and recovery hydration.",
                    type = QuestType.DAILY,
                    category = QuestCategory.RECOVERY,
                    targetValue = 15,
                    currentValue = 0,
                    unit = "min",
                    xpReward = 25,
                    primaryAttribute = AttributeType.WIS,
                    primaryAttrXpReward = 25,
                    secondaryAttribute = AttributeType.VIT,
                    secondaryAttrXpReward = 15,
                    verificationStatus = VerificationStatus.SELF_REPORTED,
                    createdAtEpochMs = now - 2000
                ),
                Quest(
                    title = "Weekly Endurance Threshold",
                    description = "Accumulate 150 minutes of verified or logged cardio training this week.",
                    type = QuestType.WEEKLY,
                    category = QuestCategory.PHYSICAL,
                    targetValue = 150,
                    currentValue = 0,
                    unit = "min",
                    xpReward = 120,
                    primaryAttribute = AttributeType.STR,
                    primaryAttrXpReward = 50,
                    secondaryAttribute = AttributeType.VIT,
                    secondaryAttrXpReward = 50,
                    verificationStatus = VerificationStatus.SELF_REPORTED,
                    createdAtEpochMs = now - 3000
                ),
                Quest(
                    title = "D-Rank Promotion Readiness",
                    description = "Reach Level 5, 85 total attributes, and 3 verified training sessions.",
                    type = QuestType.LONG_TERM,
                    category = QuestCategory.DISCIPLINE,
                    targetValue = 5,
                    currentValue = 1,
                    unit = "Lv",
                    xpReward = 200,
                    primaryAttribute = AttributeType.DIS,
                    primaryAttrXpReward = 80,
                    secondaryAttribute = AttributeType.CHA,
                    secondaryAttrXpReward = 35,
                    verificationStatus = VerificationStatus.VERIFIED_SYSTEM_TIMER,
                    createdAtEpochMs = now - 4000
                )
            )
            db.questDao().insertQuests(initialQuests.map { it.toEntity() })

            db.systemNotificationDao().insertNotification(
                SystemNotification(
                    title = "System Awakening Initialized",
                    message = "Welcome, Player. Complete real-world quests or connect Health Connect to earn verified XP toward D-Rank.",
                    category = NotificationCategory.SYSTEM_ALERT,
                    timestampEpochMs = now
                ).toEntity()
            )
            db.systemNotificationDao().insertNotification(
                SystemNotification(
                    title = "AI Game Master Advisory",
                    message = "Baseline 30-minute jogging protocol set. Prioritize form and steady cadence while building your streak.",
                    category = NotificationCategory.AI_RECOMMENDATION,
                    timestampEpochMs = now - 500
                ).toEntity()
            )

            // Milestone 11: Ensure Shadow Monarch profile is seeded on fresh install
            if (db.shadowMonarchProfileDao().getProfileOnce() == null) {
                db.shadowMonarchProfileDao().insertProfile(
                    ShadowMonarchProfileEntity(
                        id = 1,
                        monarchLevel = 1,
                        monarchXp = 0L,
                        monarchRankName = "AWAKENING",
                        monarchAuthority = 10,
                        armyCapacity = 5,
                        activeShadowCount = 0,
                        totalShadowsRecruited = 0,
                        highestShadowRankName = "E_RANK",
                        totalEvolutions = 0,
                        shadowArmyPowerRating = 0L,
                        unlockedMonarchSkillIdsJson = "[]",
                        updatedAtEpochMs = now
                    )
                )
            }

            // Milestone 12: Ensure default local social profile and 8 modules exist on fresh install
            val defaultOwner = "local_vault_player_1"
            if (db.socialProfileDao().getProfile(defaultOwner) == null) {
                db.socialProfileDao().insertOrUpdateProfile(
                    SocialProfileEntity(
                        accountOwnerId = defaultOwner,
                        overallSocialMasteryStage = "NOVICE",
                        lastActiveEpochMs = now,
                        createdAtEpochMs = now,
                        updatedAtEpochMs = now
                    )
                )
                val allModules = com.example.domain.model.SocialModuleIdentifier.entries.map { mod ->
                    SocialModuleProgressEntity(
                        accountOwnerId = defaultOwner,
                        moduleName = mod.code,
                        masteryStage = "NOVICE",
                        masteryStageOrdinal = 0,
                        lastAssessedEpochMs = now,
                        updatedAtEpochMs = now
                    )
                }
                db.socialModuleProgressDao().insertAllIgnore(allModules)
            }
        }
    }
}
