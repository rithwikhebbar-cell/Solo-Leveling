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
import com.example.data.local.dao.SocialRecommendationDao
import com.example.data.local.dao.SocialTrainingPlanDao
import com.example.data.local.dao.SocialCompetencyProgressDao
import com.example.data.local.dao.ConfirmedFailureDao
import com.example.data.local.dao.PenaltyAccountDao
import com.example.data.local.dao.PenaltyDao
import com.example.data.local.dao.PenaltyTransactionDao
import com.example.data.local.dao.QuestCommitmentDao
import com.example.data.local.dao.QuestExemptionDao
import com.example.data.local.dao.QuestRescheduleDao
import com.example.data.local.dao.RedemptionDao
import com.example.data.local.dao.RedemptionVerificationDao
import com.example.data.local.entity.ConfirmedFailureRecordEntity
import com.example.data.local.entity.PenaltyEntity
import com.example.data.local.entity.PenaltyPointAccountEntity
import com.example.data.local.entity.PenaltyTransactionEntity
import com.example.data.local.entity.QuestCommitmentEntity
import com.example.data.local.entity.QuestExemptionEntity
import com.example.data.local.entity.QuestRescheduleEntity
import com.example.data.local.entity.RedemptionEntity
import com.example.data.local.entity.RedemptionVerificationEntity
import com.example.data.local.entity.SocialAssessmentEntity
import com.example.data.local.entity.SocialMissionRecordEntity
import com.example.data.local.entity.SocialModuleProgressEntity
import com.example.data.local.entity.SocialPracticeSessionEntity
import com.example.data.local.entity.SocialProfileEntity
import com.example.data.local.entity.SocialScenarioAttemptEntity
import com.example.data.local.entity.SocialScenarioEntity
import com.example.data.local.entity.SocialAdaptiveRecommendationEntity
import com.example.data.local.entity.SocialWeeklyTrainingPlanEntity
import com.example.data.local.entity.SocialPlannedActivityEntity
import com.example.data.local.entity.SocialCompetencyProgressEntity
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
import com.example.data.local.dao.ShadowXpTraceDao
import com.example.data.local.dao.ShadowEvidenceClaimDao
import com.example.data.local.dao.ShadowResonanceDao
import com.example.data.local.dao.ShadowBondDao
import com.example.data.local.dao.ShadowTacticalDao
import com.example.data.local.dao.ShadowDominionDao
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
import com.example.data.local.entity.ShadowXpTraceEntity
import com.example.data.local.entity.ShadowEvidenceClaimEntity
import com.example.data.local.entity.ShadowResonanceEntity
import com.example.data.local.entity.ShadowBondProfileEntity
import com.example.data.local.entity.ShadowBondEventEntity
import com.example.data.local.entity.ShadowBondMilestoneEntity
import com.example.data.local.entity.ShadowSquadEntity
import com.example.data.local.entity.ShadowSquadMemberEntity
import com.example.data.local.entity.ShadowMissionAssignmentEntity
import com.example.data.local.entity.ShadowDominionEntity
import com.example.data.local.entity.ShadowTerritoryEntity
import com.example.data.local.entity.ShadowInfrastructureEntity
import com.example.data.local.entity.ShadowGarrisonEntity
import com.example.data.local.entity.ShadowDominionResearchEntity
import com.example.data.local.entity.ShadowDominionActivityEntity
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
        ShadowXpTraceEntity::class,
        // Milestone 12 — Social Mastery
        SocialProfileEntity::class,
        SocialModuleProgressEntity::class,
        SocialScenarioEntity::class,
        SocialPracticeSessionEntity::class,
        SocialScenarioAttemptEntity::class,
        SocialAssessmentEntity::class,
        SocialMissionRecordEntity::class,
        SocialAdaptiveRecommendationEntity::class,
        SocialWeeklyTrainingPlanEntity::class,
        SocialPlannedActivityEntity::class,
        SocialCompetencyProgressEntity::class,
        ShadowEvidenceClaimEntity::class,
        ShadowResonanceEntity::class,
        // Milestone 12 — Quest Failure & Commitments
        QuestCommitmentEntity::class,
        ConfirmedFailureRecordEntity::class,
        QuestExemptionEntity::class,
        QuestRescheduleEntity::class,
        // Milestone 12 — Penalty Point System (Phase 3)
        PenaltyEntity::class,
        PenaltyTransactionEntity::class,
        PenaltyPointAccountEntity::class,
        // Milestone 12 — Corrective Activity & Redemption (Phase 4)
        RedemptionEntity::class,
        RedemptionVerificationEntity::class,
        // Milestone 12 — Shadow Bond & Loyalty (Phase 6.4)
        ShadowBondProfileEntity::class,
        ShadowBondEventEntity::class,
        ShadowBondMilestoneEntity::class,
        // Milestone 12 — Shadow Army Intelligence & Tactical Command (Phase 6.5)
        ShadowSquadEntity::class,
        ShadowSquadMemberEntity::class,
        ShadowMissionAssignmentEntity::class,
        // Milestone 12 — Shadow Army Dominion & Realm Expansion (Phase 6.6)
        ShadowDominionEntity::class,
        ShadowTerritoryEntity::class,
        ShadowInfrastructureEntity::class,
        ShadowGarrisonEntity::class,
        ShadowDominionResearchEntity::class,
        ShadowDominionActivityEntity::class
    ],
    version = 23,
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
    abstract fun shadowXpTraceDao(): ShadowXpTraceDao
    abstract fun shadowEvidenceClaimDao(): ShadowEvidenceClaimDao
    abstract fun shadowResonanceDao(): ShadowResonanceDao
    abstract fun shadowBondDao(): ShadowBondDao
    abstract fun shadowTacticalDao(): ShadowTacticalDao
    abstract fun shadowDominionDao(): ShadowDominionDao

    // Milestone 12 — Social Mastery DAOs
    abstract fun socialProfileDao(): SocialProfileDao
    abstract fun socialModuleProgressDao(): SocialModuleProgressDao
    abstract fun socialScenarioDao(): SocialScenarioDao
    abstract fun socialPracticeSessionDao(): SocialPracticeSessionDao
    abstract fun socialScenarioAttemptDao(): SocialScenarioAttemptDao
    abstract fun socialAssessmentDao(): SocialAssessmentDao
    abstract fun socialMissionDao(): SocialMissionDao
    abstract fun socialRecommendationDao(): SocialRecommendationDao
    abstract fun socialTrainingPlanDao(): SocialTrainingPlanDao
    abstract fun socialCompetencyProgressDao(): SocialCompetencyProgressDao

    // Milestone 12 — Quest Failure & Commitments DAOs
    abstract fun questCommitmentDao(): QuestCommitmentDao
    abstract fun confirmedFailureDao(): ConfirmedFailureDao
    abstract fun questExemptionDao(): QuestExemptionDao
    abstract fun questRescheduleDao(): QuestRescheduleDao

    // Milestone 12 — Penalty Point System DAOs (Phase 3)
    abstract fun penaltyDao(): PenaltyDao
    abstract fun penaltyTransactionDao(): PenaltyTransactionDao
    abstract fun penaltyAccountDao(): PenaltyAccountDao

    // Milestone 12 — Corrective Activity & Redemption DAOs (Phase 4)
    abstract fun redemptionDao(): RedemptionDao
    abstract fun redemptionVerificationDao(): RedemptionVerificationDao

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
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_shadows_shadowId` ON `shadows` (`shadowId`)")
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

        val MIGRATION_13_14: Migration = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Adaptive Recommendations (Milestone 12 - Phase 5)
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `social_adaptive_recommendations` (
                        `recommendationId` TEXT NOT NULL,
                        `accountOwnerId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `targetModule` TEXT NOT NULL,
                        `targetCompetency` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `explanation` TEXT NOT NULL,
                        `supportingEvidenceJson` TEXT NOT NULL,
                        `recommendedActivityId` TEXT NOT NULL,
                        `recommendedActivityType` TEXT NOT NULL,
                        `estimatedDurationMinutes` INTEGER NOT NULL,
                        `difficulty` TEXT NOT NULL,
                        `expectedLearningOutcome` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`recommendationId`),
                        FOREIGN KEY(`accountOwnerId`) REFERENCES `social_profiles`(`accountOwnerId`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_social_adaptive_recommendations_accountOwnerId` ON `social_adaptive_recommendations` (`accountOwnerId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_social_adaptive_recommendations_targetModule` ON `social_adaptive_recommendations` (`targetModule`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_social_adaptive_recommendations_category` ON `social_adaptive_recommendations` (`category`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_social_adaptive_recommendations_status` ON `social_adaptive_recommendations` (`status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_social_adaptive_recommendations_createdAtEpochMs` ON `social_adaptive_recommendations` (`createdAtEpochMs`)")

                // Weekly Training Plans (Milestone 12 - Phase 5)
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `social_weekly_training_plans` (
                        `planId` TEXT NOT NULL,
                        `accountOwnerId` TEXT NOT NULL,
                        `weeklyDevelopmentObjective` TEXT NOT NULL,
                        `targetMinutesPerDay` INTEGER NOT NULL,
                        `targetDaysPerWeek` INTEGER NOT NULL,
                        `startDateEpochMs` INTEGER NOT NULL,
                        `endDateEpochMs` INTEGER NOT NULL,
                        `isPaused` INTEGER NOT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`planId`),
                        FOREIGN KEY(`accountOwnerId`) REFERENCES `social_profiles`(`accountOwnerId`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_social_weekly_training_plans_accountOwnerId` ON `social_weekly_training_plans` (`accountOwnerId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_social_weekly_training_plans_startDateEpochMs` ON `social_weekly_training_plans` (`startDateEpochMs`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_social_weekly_training_plans_isPaused` ON `social_weekly_training_plans` (`isPaused`)")

                // Planned Activities (Milestone 12 - Phase 5)
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `social_planned_activities` (
                        `activityId` TEXT NOT NULL,
                        `planId` TEXT NOT NULL,
                        `accountOwnerId` TEXT NOT NULL,
                        `dayOfWeek` INTEGER NOT NULL,
                        `scheduledDateEpochMs` INTEGER NOT NULL,
                        `moduleName` TEXT NOT NULL,
                        `competencyName` TEXT NOT NULL,
                        `activityType` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `referenceId` TEXT NOT NULL,
                        `durationMinutes` INTEGER NOT NULL,
                        `difficulty` TEXT NOT NULL,
                        `isCompleted` INTEGER NOT NULL,
                        `isSkipped` INTEGER NOT NULL,
                        `notes` TEXT,
                        `completedEpochMs` INTEGER,
                        PRIMARY KEY(`activityId`),
                        FOREIGN KEY(`planId`) REFERENCES `social_weekly_training_plans`(`planId`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_social_planned_activities_planId` ON `social_planned_activities` (`planId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_social_planned_activities_accountOwnerId` ON `social_planned_activities` (`accountOwnerId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_social_planned_activities_scheduledDateEpochMs` ON `social_planned_activities` (`scheduledDateEpochMs`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_social_planned_activities_isCompleted` ON `social_planned_activities` (`isCompleted`)")

                // Competency Progress (Milestone 12 - Phase 5)
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `social_competency_progress` (
                        `competencyId` TEXT NOT NULL,
                        `accountOwnerId` TEXT NOT NULL,
                        `moduleName` TEXT NOT NULL,
                        `competencyName` TEXT NOT NULL,
                        `masteryStage` TEXT NOT NULL,
                        `recommendedDifficulty` TEXT NOT NULL,
                        `practiceCount` INTEGER NOT NULL,
                        `lastPracticedEpochMs` INTEGER,
                        `lastEvaluatedScore` INTEGER,
                        `evidenceClassification` TEXT NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`competencyId`),
                        FOREIGN KEY(`accountOwnerId`) REFERENCES `social_profiles`(`accountOwnerId`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_social_competency_progress_accountOwnerId` ON `social_competency_progress` (`accountOwnerId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_social_competency_progress_moduleName` ON `social_competency_progress` (`moduleName`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_social_competency_progress_competencyName` ON `social_competency_progress` (`competencyName`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_social_competency_progress_masteryStage` ON `social_competency_progress` (`masteryStage`)")
            }
        }

        val MIGRATION_14_15: Migration = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Add explicit accountOwnerId + composite index to rpg_reward_audit
                db.execSQL(
                    "ALTER TABLE `rpg_reward_audit` ADD COLUMN `accountOwnerId` TEXT NOT NULL DEFAULT 'local_vault_player_1'"
                )
                db.execSQL(
                    """
                    UPDATE `rpg_reward_audit`
                    SET `accountOwnerId` = substr(
                        `notes`,
                        16,
                        CASE
                            WHEN instr(`notes`, '|') > 16 THEN instr(`notes`, '|') - 16
                            ELSE length(`notes`) - 15
                        END
                    )
                    WHERE `sourceType` = 'SOCIAL_MASTERY'
                      AND `notes` LIKE 'accountOwnerId=%|%'
                      AND instr(`notes`, '|') > 16
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_rpg_reward_audit_accountOwnerId_sourceType_timestampEpochMs` ON `rpg_reward_audit` (`accountOwnerId`, `sourceType`, `timestampEpochMs`)"
                )

                // 2. Add artifact validation metadata columns to social_mission_records; preserve note-only history as ARTIFACT_DESCRIBED
                db.execSQL("ALTER TABLE `social_mission_records` ADD COLUMN `artifactUri` TEXT")
                db.execSQL("ALTER TABLE `social_mission_records` ADD COLUMN `artifactOwnerId` TEXT")
                db.execSQL("ALTER TABLE `social_mission_records` ADD COLUMN `artifactContentHash` TEXT")
                db.execSQL("ALTER TABLE `social_mission_records` ADD COLUMN `artifactStateName` TEXT NOT NULL DEFAULT 'NONE'")
                db.execSQL(
                    """
                    UPDATE `social_mission_records`
                    SET `artifactStateName` = 'ARTIFACT_DESCRIBED',
                        `evidenceType` = CASE
                            WHEN UPPER(`evidenceType`) IN ('ARTIFACT_ATTACHED', 'OPTIONAL_ARTIFACT') THEN 'ARTIFACT_DESCRIBED'
                            ELSE `evidenceType`
                        END
                    WHERE `artifactNote` IS NOT NULL
                      AND trim(`artifactNote`) != ''
                      AND `artifactUri` IS NULL
                    """.trimIndent()
                )

                // 3. Preserve maintenance flags separately on social_competency_progress and remove legacy MAINTENANCE stage without granting unearned mastery
                db.execSQL("ALTER TABLE `social_competency_progress` ADD COLUMN `maintenanceRequired` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `social_competency_progress` ADD COLUMN `maintenanceReason` TEXT")
                db.execSQL(
                    """
                    UPDATE `social_competency_progress`
                    SET `masteryStage` = 'UNEXPLORED',
                        `maintenanceRequired` = 1,
                        `maintenanceReason` = COALESCE(
                            `maintenanceReason`,
                            'Legacy MAINTENANCE status safely downgraded; historical mastery will be recovered from verified activity logs.'
                        )
                    WHERE UPPER(trim(`masteryStage`)) = 'MAINTENANCE'
                    """.trimIndent()
                )
            }
        }

        // =====================================================================
        // MIGRATION 15 → 16: Milestone 12 Phase 6.1 Shadow Foundation Hardening
        // - Isolates all Shadow tables under explicit accountOwnerId
        // - Replaces global singleton profile PK (id = 1) with multi-account PK (accountOwnerId)
        // - Preserves legacy Milestone 11 rows strictly under 'local_vault_player_1'
        // - Adds relational foreign keys, per-account unique constraints, and shadow_xp_traces
        // =====================================================================
        val MIGRATION_15_16: Migration = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val legacyOwner = "local_vault_player_1"

                // 1. Rebuild shadow_monarch_profile with accountOwnerId primary key
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `shadow_monarch_profile_new` (
                        `accountOwnerId` TEXT NOT NULL,
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
                        PRIMARY KEY(`accountOwnerId`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `shadow_monarch_profile_new` (
                        `accountOwnerId`, `monarchLevel`, `monarchXp`, `monarchRankName`,
                        `monarchAuthority`, `armyCapacity`, `activeShadowCount`, `totalShadowsRecruited`,
                        `highestShadowRankName`, `totalEvolutions`, `shadowArmyPowerRating`,
                        `unlockedMonarchSkillIdsJson`, `updatedAtEpochMs`
                    )
                    SELECT
                        '$legacyOwner', `monarchLevel`, MAX(0, `monarchXp`), `monarchRankName`,
                        MAX(0, `monarchAuthority`), MAX(1, `armyCapacity`), MAX(0, `activeShadowCount`),
                        MAX(0, `totalShadowsRecruited`), `highestShadowRankName`, MAX(0, `totalEvolutions`),
                        MAX(0, `shadowArmyPowerRating`), `unlockedMonarchSkillIdsJson`, `updatedAtEpochMs`
                    FROM `shadow_monarch_profile`
                    ORDER BY `id` ASC
                    LIMIT 1
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `shadow_monarch_profile_new` VALUES (
                        '$legacyOwner', 1, 0, 'AWAKENING', 0, 10, 0, 0, 'E_RANK', 0, 0, '["monarch_shadow_extraction"]', 0
                    )
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `shadow_monarch_profile`")
                db.execSQL("ALTER TABLE `shadow_monarch_profile_new` RENAME TO `shadow_monarch_profile`")

                // 2. Rebuild shadows with accountOwnerId + FK
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `shadows_new` (
                        `shadowId` TEXT NOT NULL,
                        `accountOwnerId` TEXT NOT NULL,
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
                        PRIMARY KEY(`shadowId`),
                        FOREIGN KEY(`accountOwnerId`) REFERENCES `shadow_monarch_profile`(`accountOwnerId`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `shadows_new` (
                        `shadowId`, `accountOwnerId`, `name`, `shadowClassName`, `rarityName`,
                        `currentRankName`, `level`, `shadowXp`, `statPower`, `statEndurance`,
                        `statFocus`, `statDiscipline`, `statWisdom`, `statAdaptability`,
                        `evolutionStageName`, `passiveAbility`, `activeSkillIdsJson`,
                        `recruitmentSource`, `extractionEventId`, `extractedAtEpochMs`,
                        `evolutionHistoryJson`, `deploymentStatusName`, `equippedFormationId`,
                        `isRetired`, `notes`
                    )
                    SELECT
                        `shadowId`, '$legacyOwner', `name`, `shadowClassName`, `rarityName`,
                        `currentRankName`, MAX(1, `level`), MAX(0, `shadowXp`), `statPower`, `statEndurance`,
                        `statFocus`, `statDiscipline`, `statWisdom`, `statAdaptability`,
                        `evolutionStageName`, `passiveAbility`, `activeSkillIdsJson`,
                        `recruitmentSource`, `extractionEventId`, `extractedAtEpochMs`,
                        `evolutionHistoryJson`, `deploymentStatusName`, `equippedFormationId`,
                        `isRetired`, `notes`
                    FROM `shadows`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `shadows`")
                db.execSQL("ALTER TABLE `shadows_new` RENAME TO `shadows`")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_shadows_shadowId` ON `shadows` (`shadowId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shadows_accountOwnerId` ON `shadows` (`accountOwnerId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shadows_accountOwnerId_isRetired` ON `shadows` (`accountOwnerId`, `isRetired`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shadows_accountOwnerId_shadowClassName` ON `shadows` (`accountOwnerId`, `shadowClassName`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shadows_accountOwnerId_currentRankName` ON `shadows` (`accountOwnerId`, `currentRankName`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shadows_rarityName` ON `shadows` (`rarityName`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shadows_deploymentStatusName` ON `shadows` (`deploymentStatusName`)")

                // 3. Rebuild shadow_extractions with accountOwnerId + per-owner composite unique index
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `shadow_extractions_new` (
                        `extractionId` TEXT NOT NULL,
                        `accountOwnerId` TEXT NOT NULL,
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
                        PRIMARY KEY(`extractionId`),
                        FOREIGN KEY(`accountOwnerId`) REFERENCES `shadow_monarch_profile`(`accountOwnerId`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `shadow_extractions_new` (
                        `extractionId`, `accountOwnerId`, `sourceTypeName`, `sourceEventId`,
                        `outcomeName`, `extractedShadowId`, `shadowName`, `shadowClassName`,
                        `shadowRarityName`, `determinismSeed`, `evidenceSummary`,
                        `timestampEpochMs`, `isConfirmed`, `monarchXpAwarded`
                    )
                    SELECT
                        `extractionId`, '$legacyOwner', `sourceTypeName`, `sourceEventId`,
                        `outcomeName`, `extractedShadowId`, `shadowName`, `shadowClassName`,
                        `shadowRarityName`, `determinismSeed`, `evidenceSummary`,
                        `timestampEpochMs`, `isConfirmed`, MAX(0, `monarchXpAwarded`)
                    FROM `shadow_extractions`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `shadow_extractions`")
                db.execSQL("ALTER TABLE `shadow_extractions_new` RENAME TO `shadow_extractions`")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_shadow_extractions_extractionId` ON `shadow_extractions` (`extractionId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_shadow_extractions_accountOwnerId_sourceTypeName_sourceEventId` ON `shadow_extractions` (`accountOwnerId`, `sourceTypeName`, `sourceEventId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shadow_extractions_accountOwnerId_timestampEpochMs` ON `shadow_extractions` (`accountOwnerId`, `timestampEpochMs`)")

                // 4. Rebuild shadow_skill_unlocks with accountOwnerId + FKs to shadow_monarch_profile & shadows
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `shadow_skill_unlocks_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `accountOwnerId` TEXT NOT NULL,
                        `shadowId` TEXT NOT NULL,
                        `skillId` TEXT NOT NULL,
                        `unlockedAtEpochMs` INTEGER NOT NULL,
                        `evidenceNote` TEXT NOT NULL,
                        FOREIGN KEY(`accountOwnerId`) REFERENCES `shadow_monarch_profile`(`accountOwnerId`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`shadowId`) REFERENCES `shadows`(`shadowId`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `shadow_skill_unlocks_new` (
                        `id`, `accountOwnerId`, `shadowId`, `skillId`, `unlockedAtEpochMs`, `evidenceNote`
                    )
                    SELECT
                        u.`id`, '$legacyOwner', u.`shadowId`, u.`skillId`, u.`unlockedAtEpochMs`, u.`evidenceNote`
                    FROM `shadow_skill_unlocks` u
                    INNER JOIN `shadows` s ON s.`shadowId` = u.`shadowId`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `shadow_skill_unlocks`")
                db.execSQL("ALTER TABLE `shadow_skill_unlocks_new` RENAME TO `shadow_skill_unlocks`")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_shadow_skill_unlocks_accountOwnerId_shadowId_skillId` ON `shadow_skill_unlocks` (`accountOwnerId`, `shadowId`, `skillId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shadow_skill_unlocks_shadowId` ON `shadow_skill_unlocks` (`shadowId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shadow_skill_unlocks_accountOwnerId` ON `shadow_skill_unlocks` (`accountOwnerId`)")

                // 5. Rebuild shadow_deployments with accountOwnerId + FKs to shadow_monarch_profile & shadows
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `shadow_deployments_new` (
                        `deploymentId` TEXT NOT NULL,
                        `accountOwnerId` TEXT NOT NULL,
                        `shadowId` TEXT NOT NULL,
                        `shadowName` TEXT NOT NULL,
                        `categoryName` TEXT NOT NULL,
                        `role` TEXT NOT NULL,
                        `assignedAtEpochMs` INTEGER NOT NULL,
                        `recalledAtEpochMs` INTEGER,
                        `statusName` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        PRIMARY KEY(`deploymentId`),
                        FOREIGN KEY(`accountOwnerId`) REFERENCES `shadow_monarch_profile`(`accountOwnerId`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`shadowId`) REFERENCES `shadows`(`shadowId`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `shadow_deployments_new` (
                        `deploymentId`, `accountOwnerId`, `shadowId`, `shadowName`, `categoryName`,
                        `role`, `assignedAtEpochMs`, `recalledAtEpochMs`, `statusName`, `notes`
                    )
                    SELECT
                        d.`deploymentId`, '$legacyOwner', d.`shadowId`, d.`shadowName`, d.`categoryName`,
                        d.`role`, d.`assignedAtEpochMs`, d.`recalledAtEpochMs`, d.`statusName`, d.`notes`
                    FROM `shadow_deployments` d
                    INNER JOIN `shadows` s ON s.`shadowId` = d.`shadowId`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `shadow_deployments`")
                db.execSQL("ALTER TABLE `shadow_deployments_new` RENAME TO `shadow_deployments`")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_shadow_deployments_deploymentId` ON `shadow_deployments` (`deploymentId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shadow_deployments_accountOwnerId_statusName` ON `shadow_deployments` (`accountOwnerId`, `statusName`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shadow_deployments_shadowId` ON `shadow_deployments` (`shadowId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shadow_deployments_categoryName` ON `shadow_deployments` (`categoryName`)")

                // 6. Rebuild shadow_formations with accountOwnerId + FK
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `shadow_formations_new` (
                        `formationId` TEXT NOT NULL,
                        `accountOwnerId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `formationTypeName` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `maxSlots` INTEGER NOT NULL,
                        `memberShadowIdsJson` TEXT NOT NULL,
                        `isActive` INTEGER NOT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`formationId`),
                        FOREIGN KEY(`accountOwnerId`) REFERENCES `shadow_monarch_profile`(`accountOwnerId`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `shadow_formations_new` (
                        `formationId`, `accountOwnerId`, `name`, `formationTypeName`, `description`,
                        `maxSlots`, `memberShadowIdsJson`, `isActive`, `createdAtEpochMs`, `updatedAtEpochMs`
                    )
                    SELECT
                        `formationId`, '$legacyOwner', `name`, `formationTypeName`, `description`,
                        `maxSlots`, `memberShadowIdsJson`, `isActive`, `createdAtEpochMs`, `updatedAtEpochMs`
                    FROM `shadow_formations`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `shadow_formations`")
                db.execSQL("ALTER TABLE `shadow_formations_new` RENAME TO `shadow_formations`")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_shadow_formations_formationId` ON `shadow_formations` (`formationId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shadow_formations_accountOwnerId_isActive` ON `shadow_formations` (`accountOwnerId`, `isActive`)")

                // 7. Rebuild monarch_skill_unlocks with accountOwnerId + per-owner unique index
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `monarch_skill_unlocks_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `accountOwnerId` TEXT NOT NULL,
                        `skillId` TEXT NOT NULL,
                        `unlockedAtEpochMs` INTEGER NOT NULL,
                        `evidenceNote` TEXT NOT NULL,
                        FOREIGN KEY(`accountOwnerId`) REFERENCES `shadow_monarch_profile`(`accountOwnerId`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `monarch_skill_unlocks_new` (
                        `id`, `accountOwnerId`, `skillId`, `unlockedAtEpochMs`, `evidenceNote`
                    )
                    SELECT
                        `id`, '$legacyOwner', `skillId`, `unlockedAtEpochMs`, `evidenceNote`
                    FROM `monarch_skill_unlocks`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `monarch_skill_unlocks`")
                db.execSQL("ALTER TABLE `monarch_skill_unlocks_new` RENAME TO `monarch_skill_unlocks`")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_monarch_skill_unlocks_accountOwnerId_skillId` ON `monarch_skill_unlocks` (`accountOwnerId`, `skillId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_monarch_skill_unlocks_accountOwnerId` ON `monarch_skill_unlocks` (`accountOwnerId`)")

                // 8. Rebuild shadow_quest_progress with accountOwnerId + per-owner unique index
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `shadow_quest_progress_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `accountOwnerId` TEXT NOT NULL,
                        `questId` TEXT NOT NULL,
                        `currentValue` INTEGER NOT NULL,
                        `isCompleted` INTEGER NOT NULL,
                        `rewardClaimedAtEpochMs` INTEGER,
                        `completedAtEpochMs` INTEGER,
                        FOREIGN KEY(`accountOwnerId`) REFERENCES `shadow_monarch_profile`(`accountOwnerId`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `shadow_quest_progress_new` (
                        `id`, `accountOwnerId`, `questId`, `currentValue`, `isCompleted`,
                        `rewardClaimedAtEpochMs`, `completedAtEpochMs`
                    )
                    SELECT
                        `id`, '$legacyOwner', `questId`, MAX(0, `currentValue`), `isCompleted`,
                        `rewardClaimedAtEpochMs`, `completedAtEpochMs`
                    FROM `shadow_quest_progress`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `shadow_quest_progress`")
                db.execSQL("ALTER TABLE `shadow_quest_progress_new` RENAME TO `shadow_quest_progress`")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_shadow_quest_progress_accountOwnerId_questId` ON `shadow_quest_progress` (`accountOwnerId`, `questId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shadow_quest_progress_accountOwnerId` ON `shadow_quest_progress` (`accountOwnerId`)")

                // 9. Create dedicated shadow_xp_traces table and migrate legacy '_monarch_xp_trace_' rows
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `shadow_xp_traces` (
                        `traceId` TEXT NOT NULL,
                        `accountOwnerId` TEXT NOT NULL,
                        `traceType` TEXT NOT NULL,
                        `sourceTag` TEXT NOT NULL,
                        `targetEntityId` TEXT NOT NULL,
                        `xpAmount` INTEGER NOT NULL,
                        `description` TEXT NOT NULL,
                        `timestampEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`traceId`),
                        FOREIGN KEY(`accountOwnerId`) REFERENCES `shadow_monarch_profile`(`accountOwnerId`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `shadow_xp_traces` (
                        `traceId`, `accountOwnerId`, `traceType`, `sourceTag`, `targetEntityId`,
                        `xpAmount`, `description`, `timestampEpochMs`
                    )
                    SELECT
                        `transactionId`,
                        '$legacyOwner',
                        'MONARCH_XP',
                        CASE
                            WHEN `sourceEventId` LIKE 'monarch_xp_%' THEN substr(`sourceEventId`, 12)
                            ELSE `sourceEventId`
                        END,
                        '',
                        MAX(0, `amount`),
                        `sourceDescription`,
                        `timestampEpochMs`
                    FROM `shadow_resource_transactions`
                    WHERE `resourceTypeName` = '_monarch_xp_trace_'
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_shadow_xp_traces_traceId` ON `shadow_xp_traces` (`traceId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_shadow_xp_traces_accountOwnerId_traceType_sourceTag` ON `shadow_xp_traces` (`accountOwnerId`, `traceType`, `sourceTag`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shadow_xp_traces_accountOwnerId` ON `shadow_xp_traces` (`accountOwnerId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shadow_xp_traces_timestampEpochMs` ON `shadow_xp_traces` (`timestampEpochMs`)")

                // 10. Rebuild shadow_resource_transactions with accountOwnerId + FK (excluding migrated XP trace rows)
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `shadow_resource_transactions_new` (
                        `transactionId` TEXT NOT NULL,
                        `accountOwnerId` TEXT NOT NULL,
                        `resourceTypeName` TEXT NOT NULL,
                        `amount` INTEGER NOT NULL,
                        `transactionTypeName` TEXT NOT NULL,
                        `sourceEventId` TEXT NOT NULL,
                        `sourceDescription` TEXT NOT NULL,
                        `balanceAfter` INTEGER NOT NULL,
                        `timestampEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`transactionId`),
                        FOREIGN KEY(`accountOwnerId`) REFERENCES `shadow_monarch_profile`(`accountOwnerId`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `shadow_resource_transactions_new` (
                        `transactionId`, `accountOwnerId`, `resourceTypeName`, `amount`,
                        `transactionTypeName`, `sourceEventId`, `sourceDescription`,
                        `balanceAfter`, `timestampEpochMs`
                    )
                    SELECT
                        `transactionId`, '$legacyOwner', `resourceTypeName`, `amount`,
                        `transactionTypeName`, `sourceEventId`, `sourceDescription`,
                        MAX(0, `balanceAfter`), `timestampEpochMs`
                    FROM `shadow_resource_transactions`
                    WHERE `resourceTypeName` != '_monarch_xp_trace_'
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `shadow_resource_transactions`")
                db.execSQL("ALTER TABLE `shadow_resource_transactions_new` RENAME TO `shadow_resource_transactions`")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_shadow_resource_transactions_transactionId` ON `shadow_resource_transactions` (`transactionId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_shadow_resource_transactions_accountOwnerId_sourceEventId_resourceTypeName_transactionTypeName` ON `shadow_resource_transactions` (`accountOwnerId`, `sourceEventId`, `resourceTypeName`, `transactionTypeName`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shadow_resource_transactions_accountOwnerId_resourceTypeName_timestampEpochMs` ON `shadow_resource_transactions` (`accountOwnerId`, `resourceTypeName`, `timestampEpochMs`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shadow_resource_transactions_sourceEventId` ON `shadow_resource_transactions` (`sourceEventId`)")
            }
        }

        // =====================================================================
        // MIGRATION 16 → 17: Milestone 12 Phase 6.2 Shadow Recruitment & Resonance
        // =====================================================================
        val MIGRATION_16_17: Migration = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `shadow_evidence_claims` (
                        `claimId` TEXT NOT NULL,
                        `accountOwnerId` TEXT NOT NULL,
                        `sourceType` TEXT NOT NULL,
                        `sourceRecordId` TEXT NOT NULL,
                        `claimedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`claimId`),
                        FOREIGN KEY(`accountOwnerId`) REFERENCES `shadow_monarch_profile`(`accountOwnerId`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_shadow_evidence_claims_accountOwnerId_sourceType_sourceRecordId` ON `shadow_evidence_claims` (`accountOwnerId`, `sourceType`, `sourceRecordId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shadow_evidence_claims_accountOwnerId` ON `shadow_evidence_claims` (`accountOwnerId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `shadow_resonance_records` (
                        `resonanceId` TEXT NOT NULL,
                        `accountOwnerId` TEXT NOT NULL,
                        `archetypeId` TEXT NOT NULL,
                        `resonanceLevel` INTEGER NOT NULL,
                        `resonanceShards` INTEGER NOT NULL,
                        `lastResonanceEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`resonanceId`),
                        FOREIGN KEY(`accountOwnerId`) REFERENCES `shadow_monarch_profile`(`accountOwnerId`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_shadow_resonance_records_accountOwnerId_archetypeId` ON `shadow_resonance_records` (`accountOwnerId`, `archetypeId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shadow_resonance_records_accountOwnerId` ON `shadow_resonance_records` (`accountOwnerId`)")
            }
        }

        // =====================================================================
        // MIGRATION 17 → 18: Milestone 12 Phase 2 Quest Commitment & Failure Detection
        // =====================================================================
        val MIGRATION_17_18: Migration = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create quest_commitments table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `quest_commitments` (
                        `commitmentId` TEXT NOT NULL,
                        `questId` INTEGER NOT NULL,
                        `accountOwnerId` TEXT NOT NULL,
                        `logicalCycleId` TEXT NOT NULL,
                        `stateName` TEXT NOT NULL,
                        `acceptedAtEpochMs` INTEGER,
                        `deadlineEpochMs` INTEGER NOT NULL,
                        `gracePeriodMinutes` INTEGER NOT NULL,
                        `gracePeriodExpiresAtEpochMs` INTEGER NOT NULL,
                        `rescheduleCount` INTEGER NOT NULL,
                        `originalDeadlineEpochMs` INTEGER NOT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`commitmentId`),
                        FOREIGN KEY(`questId`) REFERENCES `quests`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_quest_commitments_questId_logicalCycleId` ON `quest_commitments` (`questId`, `logicalCycleId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_quest_commitments_accountOwnerId` ON `quest_commitments` (`accountOwnerId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_quest_commitments_stateName` ON `quest_commitments` (`stateName`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_quest_commitments_deadlineEpochMs` ON `quest_commitments` (`deadlineEpochMs`)")

                // 2. Create confirmed_quest_failures table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `confirmed_quest_failures` (
                        `failureId` TEXT NOT NULL,
                        `commitmentId` TEXT NOT NULL,
                        `questId` INTEGER NOT NULL,
                        `logicalCycleId` TEXT NOT NULL,
                        `accountOwnerId` TEXT NOT NULL,
                        `deadlineEpochMs` INTEGER NOT NULL,
                        `gracePeriodExpiresAtEpochMs` INTEGER NOT NULL,
                        `confirmedAtEpochMs` INTEGER NOT NULL,
                        `failureReasonCode` TEXT NOT NULL,
                        `policyVersion` INTEGER NOT NULL,
                        `exemptionEvaluated` INTEGER NOT NULL,
                        PRIMARY KEY(`failureId`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_confirmed_quest_failures_commitmentId` ON `confirmed_quest_failures` (`commitmentId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_confirmed_quest_failures_questId` ON `confirmed_quest_failures` (`questId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_confirmed_quest_failures_logicalCycleId` ON `confirmed_quest_failures` (`logicalCycleId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_confirmed_quest_failures_accountOwnerId` ON `confirmed_quest_failures` (`accountOwnerId`)")

                // 3. Create quest_exemptions table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `quest_exemptions` (
                        `exemptionId` TEXT NOT NULL,
                        `commitmentId` TEXT NOT NULL,
                        `questId` INTEGER NOT NULL,
                        `accountOwnerId` TEXT NOT NULL,
                        `categoryName` TEXT NOT NULL,
                        `lifecycleStateName` TEXT NOT NULL,
                        `explanation` TEXT NOT NULL,
                        `requestedAtEpochMs` INTEGER NOT NULL,
                        `reviewedAtEpochMs` INTEGER,
                        `reviewerNotes` TEXT,
                        PRIMARY KEY(`exemptionId`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_quest_exemptions_commitmentId` ON `quest_exemptions` (`commitmentId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_quest_exemptions_accountOwnerId` ON `quest_exemptions` (`accountOwnerId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_quest_exemptions_lifecycleStateName` ON `quest_exemptions` (`lifecycleStateName`)")

                // 4. Create quest_reschedules table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `quest_reschedules` (
                        `rescheduleId` TEXT NOT NULL,
                        `commitmentId` TEXT NOT NULL,
                        `questId` INTEGER NOT NULL,
                        `previousDeadlineEpochMs` INTEGER NOT NULL,
                        `revisedDeadlineEpochMs` INTEGER NOT NULL,
                        `reasonCategory` TEXT NOT NULL,
                        `rescheduledAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`rescheduleId`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_quest_reschedules_commitmentId` ON `quest_reschedules` (`commitmentId`)")
            }
        }

        val MIGRATION_18_19: Migration = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create penalties table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `penalties` (
                        `penaltyId` TEXT NOT NULL,
                        `failureRecordId` TEXT NOT NULL,
                        `commitmentId` TEXT NOT NULL,
                        `questId` INTEGER NOT NULL,
                        `accountOwnerId` TEXT NOT NULL,
                        `questTitleSnapshot` TEXT NOT NULL,
                        `severityCode` TEXT NOT NULL,
                        `basePenaltyPoints` INTEGER NOT NULL,
                        `escalationBonusPoints` INTEGER NOT NULL,
                        `totalPenaltyPoints` INTEGER NOT NULL,
                        `penaltyStatusName` TEXT NOT NULL,
                        `reason` TEXT NOT NULL,
                        `policyVersion` TEXT NOT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        `resolvedAtEpochMs` INTEGER,
                        PRIMARY KEY(`penaltyId`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_penalties_failureRecordId` ON `penalties` (`failureRecordId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_penalties_commitmentId` ON `penalties` (`commitmentId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_penalties_accountOwnerId` ON `penalties` (`accountOwnerId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_penalties_penaltyStatusName` ON `penalties` (`penaltyStatusName`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_penalties_createdAtEpochMs` ON `penalties` (`createdAtEpochMs`)")

                // 2. Create penalty_transactions table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `penalty_transactions` (
                        `transactionId` TEXT NOT NULL,
                        `accountOwnerId` TEXT NOT NULL,
                        `penaltyId` TEXT NOT NULL,
                        `transactionTypeName` TEXT NOT NULL,
                        `pointsDelta` INTEGER NOT NULL,
                        `balanceAfter` INTEGER NOT NULL,
                        `reason` TEXT NOT NULL,
                        `timestampEpochMs` INTEGER NOT NULL,
                        `idempotencyKey` TEXT NOT NULL,
                        PRIMARY KEY(`transactionId`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_penalty_transactions_idempotencyKey` ON `penalty_transactions` (`idempotencyKey`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_penalty_transactions_accountOwnerId` ON `penalty_transactions` (`accountOwnerId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_penalty_transactions_penaltyId` ON `penalty_transactions` (`penaltyId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_penalty_transactions_timestampEpochMs` ON `penalty_transactions` (`timestampEpochMs`)")

                // 3. Create penalty_point_accounts table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `penalty_point_accounts` (
                        `accountOwnerId` TEXT NOT NULL,
                        `currentBalance` INTEGER NOT NULL,
                        `lifetimePointsAccrued` INTEGER NOT NULL,
                        `activePenaltiesCount` INTEGER NOT NULL,
                        `lastUpdatedEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`accountOwnerId`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_19_20: Migration = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create redemption_activities table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `redemption_activities` (
                        `redemptionId` TEXT NOT NULL,
                        `penaltyId` TEXT NOT NULL,
                        `failureRecordId` TEXT NOT NULL,
                        `questId` INTEGER NOT NULL,
                        `accountOwnerId` TEXT NOT NULL,
                        `activityId` TEXT NOT NULL,
                        `categoryName` TEXT NOT NULL,
                        `activityTitle` TEXT NOT NULL,
                        `instructions` TEXT NOT NULL,
                        `difficultyName` TEXT NOT NULL,
                        `estimatedDurationMinutes` INTEGER NOT NULL,
                        `verificationMethodName` TEXT NOT NULL,
                        `lifecycleStateName` TEXT NOT NULL,
                        `resolutionPoints` INTEGER NOT NULL,
                        `assignedShadowClassName` TEXT NOT NULL,
                        `shadowGuidanceNote` TEXT NOT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        `acceptedAtEpochMs` INTEGER,
                        `completedAtEpochMs` INTEGER,
                        `verifiedAtEpochMs` INTEGER,
                        `verificationNotes` TEXT,
                        PRIMARY KEY(`redemptionId`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_redemption_activities_penaltyId` ON `redemption_activities` (`penaltyId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_redemption_activities_accountOwnerId` ON `redemption_activities` (`accountOwnerId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_redemption_activities_lifecycleStateName` ON `redemption_activities` (`lifecycleStateName`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_redemption_activities_failureRecordId` ON `redemption_activities` (`failureRecordId`)")

                // 2. Create redemption_verifications table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `redemption_verifications` (
                        `verificationId` TEXT NOT NULL,
                        `redemptionId` TEXT NOT NULL,
                        `accountOwnerId` TEXT NOT NULL,
                        `submittedAtEpochMs` INTEGER NOT NULL,
                        `reflectionText` TEXT NOT NULL,
                        `checklistItemsJson` TEXT NOT NULL,
                        `externalSessionRefId` TEXT,
                        `isValid` INTEGER NOT NULL,
                        `pointsResolved` INTEGER NOT NULL,
                        `verifiedAtEpochMs` INTEGER NOT NULL,
                        `notes` TEXT NOT NULL,
                        PRIMARY KEY(`verificationId`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_redemption_verifications_redemptionId` ON `redemption_verifications` (`redemptionId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_redemption_verifications_accountOwnerId` ON `redemption_verifications` (`accountOwnerId`)")
            }
        }

        // =====================================================================
        // MIGRATION 20 → 21: Milestone 12 Phase 6.4 Shadow Bond & Loyalty System
        // =====================================================================
        val MIGRATION_20_21: Migration = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create shadow_bond_profiles table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `shadow_bond_profiles` (
                        `shadowId` TEXT NOT NULL,
                        `accountOwnerId` TEXT NOT NULL,
                        `bondLevel` INTEGER NOT NULL,
                        `bondPoints` INTEGER NOT NULL,
                        `loyaltyRating` INTEGER NOT NULL,
                        `trustRating` INTEGER NOT NULL,
                        `relationshipStageName` TEXT NOT NULL,
                        `personalityCharacteristicName` TEXT NOT NULL,
                        `totalActivitiesCount` INTEGER NOT NULL,
                        `lastBondActivityEpochMs` INTEGER NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`shadowId`),
                        FOREIGN KEY(`accountOwnerId`) REFERENCES `shadow_monarch_profile`(`accountOwnerId`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`shadowId`) REFERENCES `shadows`(`shadowId`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_shadow_bond_profiles_shadowId` ON `shadow_bond_profiles` (`shadowId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shadow_bond_profiles_accountOwnerId` ON `shadow_bond_profiles` (`accountOwnerId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shadow_bond_profiles_accountOwnerId_relationshipStageName` ON `shadow_bond_profiles` (`accountOwnerId`, `relationshipStageName`)")

                // 2. Create shadow_bond_events table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `shadow_bond_events` (
                        `eventId` TEXT NOT NULL,
                        `accountOwnerId` TEXT NOT NULL,
                        `shadowId` TEXT NOT NULL,
                        `activityTypeName` TEXT NOT NULL,
                        `sourceEventId` TEXT NOT NULL,
                        `bondPointsAwarded` INTEGER NOT NULL,
                        `loyaltyDelta` INTEGER NOT NULL,
                        `trustDelta` INTEGER NOT NULL,
                        `description` TEXT NOT NULL,
                        `timestampEpochMs` INTEGER NOT NULL,
                        `idempotencyKey` TEXT NOT NULL,
                        PRIMARY KEY(`eventId`),
                        FOREIGN KEY(`accountOwnerId`) REFERENCES `shadow_monarch_profile`(`accountOwnerId`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`shadowId`) REFERENCES `shadows`(`shadowId`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_shadow_bond_events_eventId` ON `shadow_bond_events` (`eventId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_shadow_bond_events_idempotencyKey` ON `shadow_bond_events` (`idempotencyKey`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shadow_bond_events_accountOwnerId_shadowId_timestampEpochMs` ON `shadow_bond_events` (`accountOwnerId`, `shadowId`, `timestampEpochMs`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shadow_bond_events_shadowId` ON `shadow_bond_events` (`shadowId`)")

                // 3. Create shadow_bond_milestones table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `shadow_bond_milestones` (
                        `milestoneId` TEXT NOT NULL,
                        `accountOwnerId` TEXT NOT NULL,
                        `shadowId` TEXT NOT NULL,
                        `milestoneTypeName` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `sourceEventId` TEXT NOT NULL,
                        `timestampEpochMs` INTEGER NOT NULL,
                        `verificationStatus` TEXT NOT NULL,
                        PRIMARY KEY(`milestoneId`),
                        FOREIGN KEY(`accountOwnerId`) REFERENCES `shadow_monarch_profile`(`accountOwnerId`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`shadowId`) REFERENCES `shadows`(`shadowId`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_shadow_bond_milestones_milestoneId` ON `shadow_bond_milestones` (`milestoneId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_shadow_bond_milestones_accountOwnerId_shadowId_milestoneTypeName` ON `shadow_bond_milestones` (`accountOwnerId`, `shadowId`, `milestoneTypeName`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shadow_bond_milestones_accountOwnerId` ON `shadow_bond_milestones` (`accountOwnerId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shadow_bond_milestones_shadowId` ON `shadow_bond_milestones` (`shadowId`)")
            }
        }

        // =====================================================================
        // MIGRATION 21 → 22: Milestone 12 Phase 6.5 Shadow Tactical Command
        // =====================================================================
        val MIGRATION_21_22: Migration = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create shadow_squads table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `shadow_squads` (
                        `squadId` TEXT NOT NULL,
                        `accountOwnerId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `categoryName` TEXT NOT NULL,
                        `leaderShadowId` TEXT,
                        `isActive` INTEGER NOT NULL,
                        `maxMembers` INTEGER NOT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`squadId`),
                        FOREIGN KEY(`accountOwnerId`) REFERENCES `shadow_monarch_profile`(`accountOwnerId`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_shadow_squads_squadId` ON `shadow_squads` (`squadId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shadow_squads_accountOwnerId_isActive` ON `shadow_squads` (`accountOwnerId`, `isActive`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shadow_squads_accountOwnerId` ON `shadow_squads` (`accountOwnerId`)")

                // 2. Create shadow_squad_members table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `shadow_squad_members` (
                        `squadId` TEXT NOT NULL,
                        `shadowId` TEXT NOT NULL,
                        `accountOwnerId` TEXT NOT NULL,
                        `positionIndex` INTEGER NOT NULL,
                        `assignedTacticalRoleName` TEXT NOT NULL,
                        PRIMARY KEY(`squadId`, `shadowId`),
                        FOREIGN KEY(`squadId`) REFERENCES `shadow_squads`(`squadId`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`shadowId`) REFERENCES `shadows`(`shadowId`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shadow_squad_members_squadId_positionIndex` ON `shadow_squad_members` (`squadId`, `positionIndex`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shadow_squad_members_accountOwnerId_shadowId` ON `shadow_squad_members` (`accountOwnerId`, `shadowId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shadow_squad_members_shadowId` ON `shadow_squad_members` (`shadowId`)")

                // 3. Create shadow_mission_assignments table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `shadow_mission_assignments` (
                        `assignmentId` TEXT NOT NULL,
                        `accountOwnerId` TEXT NOT NULL,
                        `missionId` TEXT NOT NULL,
                        `missionTypeName` TEXT NOT NULL,
                        `squadId` TEXT,
                        `assignedShadowIdsJson` TEXT NOT NULL,
                        `statusName` TEXT NOT NULL,
                        `assignedAtEpochMs` INTEGER NOT NULL,
                        `completedAtEpochMs` INTEGER,
                        PRIMARY KEY(`assignmentId`),
                        FOREIGN KEY(`accountOwnerId`) REFERENCES `shadow_monarch_profile`(`accountOwnerId`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_shadow_mission_assignments_assignmentId` ON `shadow_mission_assignments` (`assignmentId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shadow_mission_assignments_accountOwnerId_missionId` ON `shadow_mission_assignments` (`accountOwnerId`, `missionId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shadow_mission_assignments_accountOwnerId_statusName` ON `shadow_mission_assignments` (`accountOwnerId`, `statusName`)")
            }
        }

        val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. shadow_dominion
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `shadow_dominion` (
                        `dominionId` TEXT NOT NULL,
                        `accountOwnerId` TEXT NOT NULL,
                        `dominionName` TEXT NOT NULL,
                        `dominionLevel` INTEGER NOT NULL,
                        `developmentStageName` TEXT NOT NULL,
                        `citadelLevel` INTEGER NOT NULL,
                        `controlledTerritoriesCount` INTEGER NOT NULL,
                        `infrastructureCount` INTEGER NOT NULL,
                        `totalDevelopmentEvents` INTEGER NOT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`dominionId`),
                        FOREIGN KEY(`accountOwnerId`) REFERENCES `shadow_monarch_profile`(`accountOwnerId`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_shadow_dominion_accountOwnerId` ON `shadow_dominion` (`accountOwnerId`)")

                // 2. shadow_territories
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `shadow_territories` (
                        `territoryId` TEXT NOT NULL,
                        `accountOwnerId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `categoryName` TEXT NOT NULL,
                        `stateName` TEXT NOT NULL,
                        `developmentLevel` INTEGER NOT NULL,
                        `defenseRating` INTEGER NOT NULL,
                        `assignedSquadId` TEXT,
                        `discoveredAtEpochMs` INTEGER,
                        `claimedAtEpochMs` INTEGER,
                        `explorationProgressPercent` INTEGER NOT NULL,
                        `explorationEndsAtEpochMs` INTEGER,
                        PRIMARY KEY(`territoryId`),
                        FOREIGN KEY(`accountOwnerId`) REFERENCES `shadow_monarch_profile`(`accountOwnerId`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shadow_territories_accountOwnerId_territoryId` ON `shadow_territories` (`accountOwnerId`, `territoryId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shadow_territories_accountOwnerId_stateName` ON `shadow_territories` (`accountOwnerId`, `stateName`)")

                // 3. shadow_infrastructures
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `shadow_infrastructures` (
                        `structureId` TEXT NOT NULL,
                        `accountOwnerId` TEXT NOT NULL,
                        `territoryId` TEXT NOT NULL,
                        `structureTypeName` TEXT NOT NULL,
                        `level` INTEGER NOT NULL,
                        `statusName` TEXT NOT NULL,
                        `lastResourceCollectionEpochMs` INTEGER NOT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`structureId`),
                        FOREIGN KEY(`accountOwnerId`) REFERENCES `shadow_monarch_profile`(`accountOwnerId`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shadow_infrastructures_accountOwnerId_territoryId` ON `shadow_infrastructures` (`accountOwnerId`, `territoryId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shadow_infrastructures_accountOwnerId_structureTypeName` ON `shadow_infrastructures` (`accountOwnerId`, `structureTypeName`)")

                // 4. shadow_garrisons
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `shadow_garrisons` (
                        `garrisonId` TEXT NOT NULL,
                        `accountOwnerId` TEXT NOT NULL,
                        `territoryId` TEXT NOT NULL,
                        `squadId` TEXT NOT NULL,
                        `assignedAtEpochMs` INTEGER NOT NULL,
                        `defensiveRating` INTEGER NOT NULL,
                        `readinessStatus` TEXT NOT NULL,
                        PRIMARY KEY(`garrisonId`),
                        FOREIGN KEY(`accountOwnerId`) REFERENCES `shadow_monarch_profile`(`accountOwnerId`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_shadow_garrisons_accountOwnerId_territoryId` ON `shadow_garrisons` (`accountOwnerId`, `territoryId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shadow_garrisons_accountOwnerId_squadId` ON `shadow_garrisons` (`accountOwnerId`, `squadId`)")

                // 5. shadow_dominion_research
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `shadow_dominion_research` (
                        `researchId` TEXT NOT NULL,
                        `accountOwnerId` TEXT NOT NULL,
                        `branchName` TEXT NOT NULL,
                        `projectKey` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `level` INTEGER NOT NULL,
                        `isCompleted` INTEGER NOT NULL,
                        `startedAtEpochMs` INTEGER,
                        `completedAtEpochMs` INTEGER,
                        `unlockedEffectsSummary` TEXT NOT NULL,
                        PRIMARY KEY(`researchId`),
                        FOREIGN KEY(`accountOwnerId`) REFERENCES `shadow_monarch_profile`(`accountOwnerId`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_shadow_dominion_research_accountOwnerId_projectKey` ON `shadow_dominion_research` (`accountOwnerId`, `projectKey`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shadow_dominion_research_accountOwnerId_branchName` ON `shadow_dominion_research` (`accountOwnerId`, `branchName`)")

                // 6. shadow_dominion_activities
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `shadow_dominion_activities` (
                        `activityId` TEXT NOT NULL,
                        `accountOwnerId` TEXT NOT NULL,
                        `activityTypeName` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `timestampEpochMs` INTEGER NOT NULL,
                        `entityReferenceId` TEXT NOT NULL,
                        PRIMARY KEY(`activityId`),
                        FOREIGN KEY(`accountOwnerId`) REFERENCES `shadow_monarch_profile`(`accountOwnerId`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_shadow_dominion_activities_accountOwnerId_timestampEpochMs` ON `shadow_dominion_activities` (`accountOwnerId`, `timestampEpochMs`)")
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
                        MIGRATION_12_13,
                        MIGRATION_13_14,
                        MIGRATION_14_15,
                        MIGRATION_15_16,
                        MIGRATION_16_17,
                        MIGRATION_17_18,
                        MIGRATION_18_19,
                        MIGRATION_19_20,
                        MIGRATION_20_21,
                        MIGRATION_21_22,
                        MIGRATION_22_23
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

            val defaultOwner = "local_vault_player_1"
            // Milestone 11 + Phase 6.1: Ensure default local Shadow Monarch profile exists on fresh install
            if (db.shadowMonarchProfileDao().getProfileForOwnerOnce(defaultOwner) == null) {
                db.shadowMonarchProfileDao().upsertProfile(
                    ShadowMonarchProfileEntity(
                        accountOwnerId = defaultOwner,
                        monarchLevel = 1,
                        monarchXp = 0L,
                        monarchRankName = "AWAKENING",
                        monarchAuthority = 0,
                        armyCapacity = 10,
                        activeShadowCount = 0,
                        totalShadowsRecruited = 0,
                        highestShadowRankName = "E_RANK",
                        totalEvolutions = 0,
                        shadowArmyPowerRating = 0L,
                        unlockedMonarchSkillIdsJson = "[\"monarch_shadow_extraction\"]",
                        updatedAtEpochMs = now
                    )
                )
                db.monarchSkillUnlockDao().insertMonarchSkillUnlock(
                    MonarchSkillUnlockEntity(
                        accountOwnerId = defaultOwner,
                        skillId = com.example.domain.model.MonarchSkillIds.SHADOW_EXTRACTION,
                        unlockedAtEpochMs = now,
                        evidenceNote = "Shadow Extraction unlocked at Monarch Level 1"
                    )
                )
            }

            // Milestone 12: Ensure default local social profile and 8 modules exist on fresh install
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
