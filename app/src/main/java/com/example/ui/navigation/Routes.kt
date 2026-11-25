package com.example.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe Navigation Compose routes for all Reality OS modules.
 */
@Serializable
data object HomeRoute

@Serializable
data object StatusRoute

@Serializable
data object PhysicalRoute

@Serializable
data object QuestBoardRoute

@Serializable
data object DungeonGateRoute

@Serializable
data object SkillsRoute

@Serializable
data object AchievementsRoute

@Serializable
data object AiSystemRoute

@Serializable
data object HistoryRoute

@Serializable
data object SettingsRoute

@Serializable
data object ModulesHubRoute

// Milestone 12: Penalty System (Phase 3, 4 & 5)
@Serializable
data object PenaltyStatusRoute

@Serializable
data object RedemptionDashboardRoute

@Serializable
data object PenaltyAuditRoute

// Milestone 3: Academic Development Engine (10 screens)
@Serializable
data object AcademicDashboardRoute

@Serializable
data object AcademicSubjectsRoute

@Serializable
data class AcademicSubjectDetailRoute(val subjectId: Long)

@Serializable
data class AcademicChapterTopicsRoute(
    val subjectId: Long,
    val moduleId: Long,
    val chapterId: Long
)

@Serializable
data class AcademicFocusTimerRoute(val preselectedSubjectId: Long? = null)

@Serializable
data object AcademicQuestsRoute

@Serializable
data object AcademicRevisionsRoute

@Serializable
data object AcademicExamsRoute

@Serializable
data object AcademicAnalyticsRoute

@Serializable
data object AcademicHistoryRoute

// Milestone 5: Life Management & Discipline Engine (14 screens)
@Serializable
data object LifeDashboardRoute

@Serializable
data object RoutineManagerRoute

@Serializable
data object HabitDashboardRoute

@Serializable
data class HabitEditorRoute(val habitId: Long? = null)

@Serializable
data class HabitAnalyticsRoute(val habitId: Long? = null)

@Serializable
data object PersonalTaskManagerRoute

@Serializable
data class TaskDetailsRoute(val taskId: Long)

@Serializable
data object GoalManagementRoute

@Serializable
data class GoalDetailsRoute(val goalId: Long)

@Serializable
data object DailyScheduleRoute

@Serializable
data object WeeklyCalendarRoute

@Serializable
data object MonthlyCalendarRoute

@Serializable
data object DisciplineAnalyticsRoute

@Serializable
data object WeeklyReviewRoute

// Milestone 6: Dungeons, Boss Battles, Skills & RPG Systems (17 screens total; 3 upgrade existing routes above)
@Serializable
data object DungeonDiscoveryRoute

@Serializable
data class DungeonDetailsRoute(val dungeonId: Long)

@Serializable
data class ActiveDungeonRoute(val instanceId: Long)

@Serializable
data object DungeonHistoryRoute

@Serializable
data object BossBattleDashboardRoute

@Serializable
data class BossBattleDetailsRoute(val bossId: Long)

@Serializable
data class ActiveBossBattleRoute(val instanceId: Long)

@Serializable
data class SkillTreeExplorerRoute(val specializationName: String? = null)

@Serializable
data class SkillDetailsRoute(val skillId: String)

@Serializable
data class AchievementDetailsRoute(val achievementId: String)

@Serializable
data object TitleCollectionRoute

@Serializable
data object PlayerInventoryRoute

@Serializable
data object ShadowCollectionRoute

@Serializable
data object RpgRewardHistoryRoute

// Milestone 7: Cloud Infrastructure, Synchronization & Multi-Device Support (15 Screens)
@Serializable
data object AccountGatewayRoute

@Serializable
data object SignUpRoute

@Serializable
data object LoginRoute

@Serializable
data object EmailVerificationRoute

@Serializable
data object ForgotPasswordRoute

@Serializable
data object ResetPasswordRoute

@Serializable
data object AccountSettingsRoute

@Serializable
data object AccountDeletionRoute

@Serializable
data object CloudSyncDashboardRoute

@Serializable
data object PendingChangesRoute

@Serializable
data object SyncHistoryRoute

@Serializable
data object ConflictResolutionRoute

@Serializable
data object RegisteredDevicesRoute

@Serializable
data object CloudBackupRoute

@Serializable
data object RestorePreviewRoute

// Milestone 8: Intelligence, Personalization & Adaptive System (15 Screens)
@Serializable
data object IntelligenceDashboardRoute

@Serializable
data object PlayerIntelligenceProfileRoute

@Serializable
data object StrengthsWeaknessesRoute

@Serializable
data object AdaptiveDailyPlanRoute

@Serializable
data object WorkloadBalanceRoute

@Serializable
data object ConsistencyAnalyticsRoute

@Serializable
data object PersonalizedRecommendationsRoute

@Serializable
data object AcademicIntelligenceRoute

@Serializable
data object PhysicalIntelligenceRoute

@Serializable
data object AdaptiveDungeonsRoute

@Serializable
data object DevelopmentRoadmapRoute

@Serializable
data object DailyIntelligenceReportRoute

@Serializable
data object WeeklyIntelligenceReportRoute

@Serializable
data object MonthlyIntelligenceReportRoute

@Serializable
data object PersonalAdvisorRoute

@Serializable
data object IntelligencePreferencesRoute

// Milestone 9: System Integration, Automation & Reliability (12 Screens)
@Serializable
data object SystemOverviewRoute

@Serializable
data object ModuleIntegrationStatusRoute

@Serializable
data object AutomationDashboardRoute

@Serializable
data object AutomationHistoryRoute

@Serializable
data object DailyExecutionTimelineRoute

@Serializable
data object SystemEventHistoryRoute

@Serializable
data object DataIntegrityDashboardRoute

@Serializable
data object SystemHealthMonitorRoute

@Serializable
data object PerformanceDiagnosticsRoute

@Serializable
data object ErrorRecoveryCenterRoute

@Serializable
data object NotificationManagementRoute

@Serializable
data object SystemControlSettingsRoute

// Milestone 10: The Awakening — Immersive System Experience (5 dedicated screens + overlays)
@Serializable
data object AiCompanionRoute

@Serializable
data object VoiceCommandRoute

@Serializable
data object SystemVoiceSettingsRoute

@Serializable
data object WidgetHubRoute

@Serializable
data object CelebrationHistoryRoute

// ============================================================================
// Milestone 11: THE SHADOW MONARCH — Shadow Army System (14 screens)
// ============================================================================

@Serializable
data object ShadowRealmDashboardRoute

@Serializable
data object MonarchStatusRoute

@Serializable
data object ShadowExtractionRoute

@Serializable
data object ShadowCollectionListRoute

@Serializable
data class ShadowDetailRoute(val shadowId: String)

@Serializable
data class ShadowEvolutionRoute(val shadowId: String)

@Serializable
data class ShadowSkillTreeRoute(val shadowId: String)

@Serializable
data object ShadowDeploymentRoute

@Serializable
data object ArmyFormationManagerRoute

@Serializable
data object MonarchSkillTreeRoute

@Serializable
data object ShadowQuestBoardRoute

@Serializable
data object ShadowArmyAnalyticsRoute

@Serializable
data object ShadowArchiveRoute

@Serializable
data object ShadowSettingsRoute

@Serializable
data object ShadowCommandCenterRoute

@Serializable
data object ShadowDominionRoute

// ============================================================================
// Milestone 12: SOCIAL MASTERY — Social Development Engine
// ============================================================================

@Serializable
data object SocialMasteryDashboardRoute

@Serializable
data object SocialPracticeArenaRoute

@Serializable
data object SocialCoachRoute

@Serializable
data object SocialAnalyticsRoute

@Serializable
data object SocialMissionsRoute

@Serializable
data class SocialMissionDetailRoute(val missionId: String)

@Serializable
data class SocialMissionSubmissionRoute(val missionId: String)

@Serializable
data object SocialSkillAssessmentRoute

// Milestone 14: Mission Control Center
@Serializable
data object MissionControlRoute
