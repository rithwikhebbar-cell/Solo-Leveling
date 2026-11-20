package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.domain.model.AcademicDashboardSummary
import com.example.domain.model.LifeDashboardSummary
import com.example.ui.components.SystemErrorState
import com.example.ui.components.SystemLoadingView
import com.example.ui.navigation.AcademicAnalyticsRoute
import com.example.ui.navigation.AcademicChapterTopicsRoute
import com.example.ui.navigation.AcademicDashboardRoute
import com.example.ui.navigation.AcademicExamsRoute
import com.example.ui.navigation.AcademicFocusTimerRoute
import com.example.ui.navigation.AcademicHistoryRoute
import com.example.ui.navigation.AcademicIntelligenceRoute
import com.example.ui.navigation.AcademicQuestsRoute
import com.example.ui.navigation.AcademicRevisionsRoute
import com.example.ui.navigation.AcademicSubjectDetailRoute
import com.example.ui.navigation.AcademicSubjectsRoute
import com.example.ui.navigation.AccountDeletionRoute
import com.example.ui.navigation.AccountGatewayRoute
import com.example.ui.navigation.AccountSettingsRoute
import com.example.ui.navigation.AchievementDetailsRoute
import com.example.ui.navigation.AchievementsRoute
import com.example.ui.navigation.ActiveBossBattleRoute
import com.example.ui.navigation.ActiveDungeonRoute
import com.example.ui.navigation.AdaptiveDailyPlanRoute
import com.example.ui.navigation.AdaptiveDungeonsRoute
import com.example.ui.navigation.AiCompanionRoute
import com.example.ui.navigation.AiSystemRoute
import com.example.ui.navigation.AutomationDashboardRoute
import com.example.ui.navigation.AutomationHistoryRoute
import com.example.ui.navigation.BossBattleDashboardRoute
import com.example.ui.navigation.BossBattleDetailsRoute
import com.example.ui.navigation.CelebrationHistoryRoute
import com.example.ui.navigation.CloudBackupRoute
import com.example.ui.navigation.CloudSyncDashboardRoute
import com.example.ui.navigation.ConflictResolutionRoute
import com.example.ui.navigation.ConsistencyAnalyticsRoute
import com.example.ui.navigation.DailyExecutionTimelineRoute
import com.example.ui.navigation.DailyScheduleRoute
import com.example.ui.navigation.DataIntegrityDashboardRoute
import com.example.ui.navigation.DevelopmentRoadmapRoute
import com.example.ui.navigation.DisciplineAnalyticsRoute
import com.example.ui.navigation.DungeonDetailsRoute
import com.example.ui.navigation.DungeonDiscoveryRoute
import com.example.ui.navigation.DungeonGateRoute
import com.example.ui.navigation.DungeonHistoryRoute
import com.example.ui.navigation.EmailVerificationRoute
import com.example.ui.navigation.ErrorRecoveryCenterRoute
import com.example.ui.navigation.ForgotPasswordRoute
import com.example.ui.navigation.GoalDetailsRoute
import com.example.ui.navigation.GoalManagementRoute
import com.example.ui.navigation.HabitAnalyticsRoute
import com.example.ui.navigation.HabitDashboardRoute
import com.example.ui.navigation.HabitEditorRoute
import com.example.ui.navigation.HistoryRoute
import com.example.ui.navigation.HomeRoute
import com.example.ui.navigation.IntelligenceDashboardRoute
import com.example.ui.navigation.IntelligencePreferencesRoute
import com.example.ui.navigation.LifeDashboardRoute
import com.example.ui.navigation.LoginRoute
import com.example.ui.navigation.ModuleIntegrationStatusRoute
import com.example.ui.navigation.ModulesHubRoute
import com.example.ui.navigation.MonthlyCalendarRoute
import com.example.ui.navigation.MonthlyIntelligenceReportRoute
import com.example.ui.navigation.NotificationManagementRoute
import com.example.ui.navigation.PendingChangesRoute
import com.example.ui.navigation.PerformanceDiagnosticsRoute
import com.example.ui.navigation.PersonalAdvisorRoute
import com.example.ui.navigation.PersonalTaskManagerRoute
import com.example.ui.navigation.PersonalizedRecommendationsRoute
import com.example.ui.navigation.PhysicalIntelligenceRoute
import com.example.ui.navigation.PhysicalRoute
import com.example.ui.navigation.PlayerIntelligenceProfileRoute
import com.example.ui.navigation.PlayerInventoryRoute
import com.example.ui.navigation.QuestBoardRoute
import com.example.ui.navigation.RegisteredDevicesRoute
import com.example.ui.navigation.ResetPasswordRoute
import com.example.ui.navigation.RestorePreviewRoute
import com.example.ui.navigation.RoutineManagerRoute
import com.example.ui.navigation.RpgRewardHistoryRoute
import com.example.ui.navigation.SettingsRoute
import com.example.ui.navigation.ShadowCollectionRoute
import com.example.ui.navigation.SignUpRoute
import com.example.ui.navigation.SkillDetailsRoute
import com.example.ui.navigation.SkillTreeExplorerRoute
import com.example.ui.navigation.SkillsRoute
import com.example.ui.navigation.StatusRoute
import com.example.ui.navigation.StrengthsWeaknessesRoute
import com.example.ui.navigation.SyncHistoryRoute
import com.example.ui.navigation.SystemControlSettingsRoute
import com.example.ui.navigation.SystemEventHistoryRoute
import com.example.ui.navigation.SystemHealthMonitorRoute
import com.example.ui.navigation.SystemOverviewRoute
import com.example.ui.navigation.SystemVoiceSettingsRoute
import com.example.ui.navigation.TaskDetailsRoute
import com.example.ui.navigation.TitleCollectionRoute
import com.example.ui.navigation.VoiceCommandRoute
import com.example.ui.navigation.WeeklyCalendarRoute
import com.example.ui.navigation.WeeklyIntelligenceReportRoute
import com.example.ui.navigation.WeeklyReviewRoute
import com.example.ui.navigation.WidgetHubRoute
import com.example.ui.navigation.WorkloadBalanceRoute
import com.example.ui.navigation.ShadowRealmDashboardRoute
import com.example.ui.navigation.MonarchStatusRoute
import com.example.ui.navigation.ShadowExtractionRoute
import com.example.ui.navigation.ShadowCollectionListRoute
import com.example.ui.navigation.ShadowDetailRoute
import com.example.ui.navigation.ShadowEvolutionRoute
import com.example.ui.navigation.ShadowSkillTreeRoute
import com.example.ui.navigation.ShadowDeploymentRoute
import com.example.ui.navigation.ArmyFormationManagerRoute
import com.example.ui.navigation.MonarchSkillTreeRoute
import com.example.ui.navigation.ShadowQuestBoardRoute
import com.example.ui.navigation.ShadowArmyAnalyticsRoute
import com.example.ui.navigation.ShadowArchiveRoute
import com.example.ui.navigation.ShadowSettingsRoute
import com.example.ui.viewmodel.ShadowRealmViewModel
import com.example.ui.screens.shadow.ShadowRealmDashboardScreen
import com.example.ui.screens.shadow.MonarchStatusScreen
import com.example.ui.screens.shadow.ShadowExtractionScreen
import com.example.ui.screens.shadow.ShadowArmyAnalyticsScreen
import com.example.ui.screens.shadow.ShadowArchiveScreen
import com.example.ui.screens.shadow.ShadowSettingsScreen
import com.example.ui.screens.shadow.ShadowCollectionScreen as ShadowMonarchCollectionScreen
import com.example.ui.screens.shadow.ShadowDetailScreen
import com.example.ui.screens.shadow.ShadowEvolutionScreen
import com.example.ui.screens.shadow.ShadowSkillTreeScreen
import com.example.ui.screens.shadow.ShadowDeploymentScreen
import com.example.ui.screens.shadow.ArmyFormationManagerScreen
import com.example.ui.screens.shadow.MonarchSkillTreeScreen
import com.example.ui.screens.shadow.ShadowQuestBoardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ModulesHubScreen
import com.example.ui.screens.PhysicalDevelopmentScreen
import com.example.ui.screens.QuestBoardScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatusScreen
import com.example.ui.screens.academic.AcademicAnalyticsScreen
import com.example.ui.screens.academic.AcademicChapterTopicsScreen
import com.example.ui.screens.academic.AcademicDashboardScreen
import com.example.ui.screens.academic.AcademicExamsScreen
import com.example.ui.screens.academic.AcademicFocusTimerScreen
import com.example.ui.screens.academic.AcademicHistoryScreen
import com.example.ui.screens.academic.AcademicQuestsScreen
import com.example.ui.screens.academic.AcademicRevisionsScreen
import com.example.ui.screens.academic.AcademicSubjectDetailScreen
import com.example.ui.screens.academic.AcademicSubjectsScreen
import com.example.ui.screens.ai.AiSystemScreen
import com.example.ui.screens.cloud.AccountDeletionScreen
import com.example.ui.screens.cloud.AccountGatewayScreen
import com.example.ui.screens.cloud.AccountSettingsScreen
import com.example.ui.screens.cloud.CloudBackupScreen
import com.example.ui.screens.cloud.CloudSyncDashboardScreen
import com.example.ui.screens.cloud.ConflictResolutionScreen
import com.example.ui.screens.cloud.EmailVerificationScreen
import com.example.ui.screens.cloud.ForgotPasswordScreen
import com.example.ui.screens.cloud.LoginScreen
import com.example.ui.screens.cloud.PendingChangesScreen
import com.example.ui.screens.cloud.RegisteredDevicesScreen
import com.example.ui.screens.cloud.ResetPasswordScreen
import com.example.ui.screens.cloud.RestorePreviewScreen
import com.example.ui.screens.cloud.SignUpScreen
import com.example.ui.screens.cloud.SyncHistoryScreen
import com.example.ui.screens.life.DailyScheduleScreen
import com.example.ui.screens.life.DisciplineAnalyticsScreen
import com.example.ui.screens.life.GoalDetailsScreen
import com.example.ui.screens.life.GoalManagementScreen
import com.example.ui.screens.life.HabitAnalyticsScreen
import com.example.ui.screens.life.HabitDashboardScreen
import com.example.ui.screens.life.HabitEditorScreen
import com.example.ui.screens.life.LifeDashboardScreen
import com.example.ui.screens.life.MonthlyCalendarScreen
import com.example.ui.screens.life.PersonalTaskManagerScreen
import com.example.ui.screens.life.RoutineManagerScreen
import com.example.ui.screens.life.TaskDetailsScreen
import com.example.ui.screens.life.WeeklyCalendarScreen
import com.example.ui.screens.life.WeeklyReviewScreen
import com.example.ui.screens.rpg.AchievementDetailsScreen
import com.example.ui.screens.rpg.AchievementGalleryScreen
import com.example.ui.screens.rpg.ActiveBossBattleScreen
import com.example.ui.screens.rpg.ActiveDungeonScreen
import com.example.ui.screens.rpg.BossBattleDashboardScreen
import com.example.ui.screens.rpg.BossBattleDetailsScreen
import com.example.ui.screens.rpg.DungeonDetailsScreen
import com.example.ui.screens.rpg.DungeonDiscoveryScreen
import com.example.ui.screens.rpg.DungeonGateDashboardScreen
import com.example.ui.screens.rpg.DungeonHistoryScreen
import com.example.ui.screens.rpg.PlayerInventoryScreen
import com.example.ui.screens.rpg.RpgRewardHistoryScreen
import com.example.ui.screens.rpg.ShadowCollectionScreen
import com.example.ui.screens.rpg.SkillDashboardScreen
import com.example.ui.screens.rpg.SkillDetailsScreen
import com.example.ui.screens.rpg.SkillTreeExplorerScreen
import com.example.ui.screens.rpg.TitleCollectionScreen
import com.example.ui.screens.intelligence.AcademicIntelligenceScreen
import com.example.ui.screens.intelligence.AdaptiveDailyPlanScreen
import com.example.ui.screens.intelligence.AdaptiveDungeonsScreen
import com.example.ui.screens.intelligence.ConsistencyAnalyticsScreen
import com.example.ui.screens.intelligence.DevelopmentRoadmapScreen
import com.example.ui.screens.intelligence.IntelligenceDashboardScreen
import com.example.ui.screens.intelligence.IntelligencePreferencesScreen
import com.example.ui.screens.intelligence.MonthlyIntelligenceReportScreen
import com.example.ui.screens.intelligence.PersonalAdvisorScreen
import com.example.ui.screens.intelligence.PersonalizedRecommendationsScreen
import com.example.ui.screens.intelligence.PhysicalIntelligenceScreen
import com.example.ui.screens.intelligence.PlayerIntelligenceProfileScreen
import com.example.ui.screens.intelligence.StrengthsWeaknessesScreen
import com.example.ui.screens.intelligence.WeeklyIntelligenceReportScreen
import com.example.ui.screens.intelligence.WorkloadBalanceScreen
import com.example.ui.screens.system.AutomationDashboardScreen
import com.example.ui.screens.system.AutomationHistoryScreen
import com.example.ui.screens.system.DailyExecutionTimelineScreen
import com.example.ui.screens.system.DataIntegrityDashboardScreen
import com.example.ui.screens.system.ErrorRecoveryCenterScreen
import com.example.ui.screens.system.ModuleIntegrationStatusScreen
import com.example.ui.screens.system.NotificationManagementScreen
import com.example.ui.screens.system.PerformanceDiagnosticsScreen
import com.example.ui.screens.system.SystemControlSettingsScreen
import com.example.ui.screens.system.SystemEventHistoryScreen
import com.example.ui.screens.system.SystemHealthMonitorScreen
import com.example.ui.screens.system.SystemOverviewScreen
import com.example.ui.screens.experience.AiCompanionScreen
import com.example.ui.screens.experience.AwakeningScreen
import com.example.ui.screens.experience.CelebrationHistoryScreen
import com.example.ui.screens.experience.CelebrationModalOverlay
import com.example.ui.screens.experience.SystemVoiceSettingsScreen
import com.example.ui.screens.experience.VoiceCommandScreen
import com.example.ui.screens.experience.WidgetHubScreen
import com.example.ui.theme.SoloLevelingTheme
import com.example.ui.theme.SystemColors
import com.example.ui.viewmodel.AcademicUiState
import com.example.ui.viewmodel.AcademicViewModel
import com.example.ui.viewmodel.AiSystemUiState
import com.example.ui.viewmodel.AiSystemViewModel
import com.example.ui.viewmodel.CloudSyncUiState
import com.example.ui.viewmodel.CloudSyncViewModel
import com.example.ui.viewmodel.IntelligenceUiState
import com.example.ui.viewmodel.IntelligenceViewModel
import com.example.ui.viewmodel.LifeManagementUiState
import com.example.ui.viewmodel.LifeManagementViewModel
import com.example.ui.viewmodel.RealityOsUiState
import com.example.ui.viewmodel.RealityOsViewModel
import com.example.ui.viewmodel.RpgUiState
import com.example.ui.viewmodel.RpgViewModel
import com.example.ui.viewmodel.SystemControlUiState
import com.example.ui.viewmodel.SystemControlViewModel
import com.example.ui.viewmodel.SystemExperienceUiState
import com.example.ui.viewmodel.SystemExperienceViewModel

private data class TopLevelNavItem(
    val label: String,
    val route: Any,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val TopLevelItems = listOf(
    TopLevelNavItem("Home", HomeRoute, Icons.Rounded.Dashboard, Icons.Outlined.Dashboard, "nav_home"),
    TopLevelNavItem("Status", StatusRoute, Icons.Rounded.Person, Icons.Outlined.Person, "nav_status"),
    TopLevelNavItem("Physical", PhysicalRoute, Icons.Rounded.FitnessCenter, Icons.Outlined.FitnessCenter, "nav_physical"),
    TopLevelNavItem("Quests", QuestBoardRoute, Icons.Rounded.Assignment, Icons.Outlined.Assignment, "nav_quests"),
    TopLevelNavItem("Modules", ModulesHubRoute, Icons.Rounded.GridView, Icons.Outlined.GridView, "nav_modules")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoloLevelingApp(
    viewModel: RealityOsViewModel,
    academicViewModel: AcademicViewModel,
    aiSystemViewModel: AiSystemViewModel,
    lifeManagementViewModel: LifeManagementViewModel,
    rpgViewModel: RpgViewModel,
    cloudSyncViewModel: CloudSyncViewModel,
    intelligenceViewModel: IntelligenceViewModel,
    systemControlViewModel: SystemControlViewModel,
    systemExperienceViewModel: SystemExperienceViewModel,
    shadowRealmViewModel: ShadowRealmViewModel,
    deepLinkRoute: String? = null,
    onDeepLinkConsumed: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val academicUiState by academicViewModel.uiState.collectAsStateWithLifecycle()
    val aiSystemUiState by aiSystemViewModel.uiState.collectAsStateWithLifecycle()
    val lifeUiState by lifeManagementViewModel.uiState.collectAsStateWithLifecycle()
    val rpgUiState by rpgViewModel.uiState.collectAsStateWithLifecycle()
    val cloudSyncUiState by cloudSyncViewModel.uiState.collectAsStateWithLifecycle()
    val intelligenceUiState by intelligenceViewModel.uiState.collectAsStateWithLifecycle()
    val systemControlUiState by systemControlViewModel.uiState.collectAsStateWithLifecycle()
    val systemExperienceUiState by systemExperienceViewModel.uiState.collectAsStateWithLifecycle()
    val expReady = systemExperienceUiState as? SystemExperienceUiState.Ready
    val hudGlowEnabled = (uiState as? RealityOsUiState.Ready)?.preferences?.hudGlowEnabled ?: true

    SoloLevelingTheme(hudGlowEnabled = hudGlowEnabled) {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = backStackEntry?.destination

        LaunchedEffect(deepLinkRoute) {
            if (!deepLinkRoute.isNullOrBlank()) {
                handleDeepLinkRoute(
                    route = deepLinkRoute,
                    navController = navController,
                    experienceViewModel = systemExperienceViewModel
                )
                onDeepLinkConsumed()
            }
        }

        val secondaryTitle = when {
            currentDestination?.hasRoute<DungeonGateRoute>() == true -> "DUNGEON GATE"
            currentDestination?.hasRoute<DungeonDiscoveryRoute>() == true -> "GATE DISCOVERY"
            currentDestination?.hasRoute<DungeonDetailsRoute>() == true -> "GATE BRIEFING"
            currentDestination?.hasRoute<ActiveDungeonRoute>() == true -> "ACTIVE DUNGEON"
            currentDestination?.hasRoute<DungeonHistoryRoute>() == true -> "DUNGEON HISTORY"
            currentDestination?.hasRoute<BossBattleDashboardRoute>() == true -> "BOSS RAIDS"
            currentDestination?.hasRoute<BossBattleDetailsRoute>() == true -> "BOSS CAMPAIGN"
            currentDestination?.hasRoute<ActiveBossBattleRoute>() == true -> "ACTIVE BOSS BATTLE"
            currentDestination?.hasRoute<SkillsRoute>() == true -> "SKILLS & PERKS"
            currentDestination?.hasRoute<SkillTreeExplorerRoute>() == true -> "SKILL TREES"
            currentDestination?.hasRoute<SkillDetailsRoute>() == true -> "SKILL NODE"
            currentDestination?.hasRoute<AchievementsRoute>() == true -> "ACHIEVEMENT VAULT"
            currentDestination?.hasRoute<AchievementDetailsRoute>() == true -> "ACHIEVEMENT DETAILS"
            currentDestination?.hasRoute<TitleCollectionRoute>() == true -> "PLAYER TITLES"
            currentDestination?.hasRoute<PlayerInventoryRoute>() == true -> "DIGITAL INVENTORY"
            currentDestination?.hasRoute<ShadowCollectionRoute>() == true -> "SHADOW VAULT"
            currentDestination?.hasRoute<RpgRewardHistoryRoute>() == true -> "RPG REWARD LEDGER"
            currentDestination?.hasRoute<AiSystemRoute>() == true -> "AI GAME MASTER"
            currentDestination?.hasRoute<HistoryRoute>() == true -> "ACTIVITY HISTORY"
            currentDestination?.hasRoute<SettingsRoute>() == true -> "SYSTEM SETTINGS"
            currentDestination?.hasRoute<AcademicDashboardRoute>() == true -> "ACADEMIC ENGINE"
            currentDestination?.hasRoute<AcademicSubjectsRoute>() == true -> "SUBJECT MANAGEMENT"
            currentDestination?.hasRoute<AcademicSubjectDetailRoute>() == true -> "SUBJECT DETAILS"
            currentDestination?.hasRoute<AcademicChapterTopicsRoute>() == true -> "CHAPTER & TOPICS"
            currentDestination?.hasRoute<AcademicFocusTimerRoute>() == true -> "FOCUS TIMER"
            currentDestination?.hasRoute<AcademicQuestsRoute>() == true -> "STUDY QUEST BOARD"
            currentDestination?.hasRoute<AcademicRevisionsRoute>() == true -> "REVISION CENTRE"
            currentDestination?.hasRoute<AcademicExamsRoute>() == true -> "EXAMINATION CENTRE"
            currentDestination?.hasRoute<AcademicAnalyticsRoute>() == true -> "ACADEMIC ANALYTICS"
            currentDestination?.hasRoute<AcademicHistoryRoute>() == true -> "ACADEMIC HISTORY"
            currentDestination?.hasRoute<LifeDashboardRoute>() == true -> "LIFE & DISCIPLINE OS"
            currentDestination?.hasRoute<RoutineManagerRoute>() == true -> "ROUTINE MANAGER"
            currentDestination?.hasRoute<HabitDashboardRoute>() == true -> "HABIT ENGINE"
            currentDestination?.hasRoute<HabitEditorRoute>() == true -> "HABIT CONFIGURATION"
            currentDestination?.hasRoute<HabitAnalyticsRoute>() == true -> "HABIT ANALYTICS"
            currentDestination?.hasRoute<PersonalTaskManagerRoute>() == true -> "PERSONAL TASKS"
            currentDestination?.hasRoute<TaskDetailsRoute>() == true -> "TASK DETAILS"
            currentDestination?.hasRoute<GoalManagementRoute>() == true -> "GOALS & MILESTONES"
            currentDestination?.hasRoute<GoalDetailsRoute>() == true -> "GOAL DETAILS"
            currentDestination?.hasRoute<DailyScheduleRoute>() == true -> "DAILY SCHEDULE"
            currentDestination?.hasRoute<WeeklyCalendarRoute>() == true -> "WEEKLY CALENDAR"
            currentDestination?.hasRoute<MonthlyCalendarRoute>() == true -> "MONTHLY CALENDAR"
            currentDestination?.hasRoute<DisciplineAnalyticsRoute>() == true -> "DISCIPLINE ANALYTICS"
            currentDestination?.hasRoute<WeeklyReviewRoute>() == true -> "WEEKLY REVIEW"
            currentDestination?.hasRoute<AccountGatewayRoute>() == true -> "CLOUD GATEWAY"
            currentDestination?.hasRoute<SignUpRoute>() == true -> "CREATE ACCOUNT"
            currentDestination?.hasRoute<LoginRoute>() == true -> "SIGN IN"
            currentDestination?.hasRoute<EmailVerificationRoute>() == true -> "EMAIL VERIFICATION"
            currentDestination?.hasRoute<ForgotPasswordRoute>() == true -> "FORGOT PASSWORD"
            currentDestination?.hasRoute<ResetPasswordRoute>() == true -> "RESET PASSWORD"
            currentDestination?.hasRoute<AccountSettingsRoute>() == true -> "ACCOUNT MANAGEMENT"
            currentDestination?.hasRoute<AccountDeletionRoute>() == true -> "DELETE ACCOUNT"
            currentDestination?.hasRoute<CloudSyncDashboardRoute>() == true -> "CLOUD SYNC"
            currentDestination?.hasRoute<PendingChangesRoute>() == true -> "PENDING CHANGES"
            currentDestination?.hasRoute<SyncHistoryRoute>() == true -> "SYNC HISTORY"
            currentDestination?.hasRoute<ConflictResolutionRoute>() == true -> "CONFLICTS"
            currentDestination?.hasRoute<RegisteredDevicesRoute>() == true -> "DEVICES"
            currentDestination?.hasRoute<CloudBackupRoute>() == true -> "CLOUD BACKUPS"
            currentDestination?.hasRoute<RestorePreviewRoute>() == true -> "RESTORE PREVIEW"
            currentDestination?.hasRoute<IntelligenceDashboardRoute>() == true -> "INTELLIGENCE CENTER"
            currentDestination?.hasRoute<PlayerIntelligenceProfileRoute>() == true -> "INTELLIGENCE PROFILE"
            currentDestination?.hasRoute<StrengthsWeaknessesRoute>() == true -> "STRENGTHS & RISKS"
            currentDestination?.hasRoute<AdaptiveDailyPlanRoute>() == true -> "ADAPTIVE DAILY PLAN"
            currentDestination?.hasRoute<WorkloadBalanceRoute>() == true -> "WORKLOAD BALANCE"
            currentDestination?.hasRoute<ConsistencyAnalyticsRoute>() == true -> "CONSISTENCY FORECAST"
            currentDestination?.hasRoute<PersonalizedRecommendationsRoute>() == true -> "RECOMMENDATIONS"
            currentDestination?.hasRoute<AcademicIntelligenceRoute>() == true -> "ACADEMIC INTELLIGENCE"
            currentDestination?.hasRoute<PhysicalIntelligenceRoute>() == true -> "PHYSICAL INTELLIGENCE"
            currentDestination?.hasRoute<AdaptiveDungeonsRoute>() == true -> "ADAPTIVE DUNGEONS"
            currentDestination?.hasRoute<DevelopmentRoadmapRoute>() == true -> "DEVELOPMENT ROADMAP"
            currentDestination?.hasRoute<WeeklyIntelligenceReportRoute>() == true -> "WEEKLY REPORT"
            currentDestination?.hasRoute<MonthlyIntelligenceReportRoute>() == true -> "MONTHLY REPORT"
            currentDestination?.hasRoute<PersonalAdvisorRoute>() == true -> "PERSONAL ADVISOR"
            currentDestination?.hasRoute<IntelligencePreferencesRoute>() == true -> "INTEL PREFERENCES"
            currentDestination?.hasRoute<SystemOverviewRoute>() == true -> "SYSTEM CONTROL CENTER"
            currentDestination?.hasRoute<ModuleIntegrationStatusRoute>() == true -> "MODULE INTEGRATION"
            currentDestination?.hasRoute<AutomationDashboardRoute>() == true -> "AUTOMATION WORKERS"
            currentDestination?.hasRoute<AutomationHistoryRoute>() == true -> "AUTOMATION HISTORY"
            currentDestination?.hasRoute<DailyExecutionTimelineRoute>() == true -> "EXECUTION TIMELINE"
            currentDestination?.hasRoute<SystemEventHistoryRoute>() == true -> "EVENT BUS HISTORY"
            currentDestination?.hasRoute<DataIntegrityDashboardRoute>() == true -> "DATA INTEGRITY"
            currentDestination?.hasRoute<SystemHealthMonitorRoute>() == true -> "SYSTEM HEALTH"
            currentDestination?.hasRoute<PerformanceDiagnosticsRoute>() == true -> "PERFORMANCE"
            currentDestination?.hasRoute<ErrorRecoveryCenterRoute>() == true -> "RECOVERY CENTER"
            currentDestination?.hasRoute<NotificationManagementRoute>() == true -> "NOTIFICATIONS"
            currentDestination?.hasRoute<SystemControlSettingsRoute>() == true -> "CONTROL SETTINGS"
            currentDestination?.hasRoute<AiCompanionRoute>() == true -> "AI COMPANION"
            currentDestination?.hasRoute<VoiceCommandRoute>() == true -> "VOICE CONSOLE"
            currentDestination?.hasRoute<SystemVoiceSettingsRoute>() == true -> "SYSTEM VOICE"
            currentDestination?.hasRoute<WidgetHubRoute>() == true -> "HOME WIDGETS"
            currentDestination?.hasRoute<CelebrationHistoryRoute>() == true -> "CELEBRATION VAULT"
            currentDestination?.hasRoute<ShadowRealmDashboardRoute>() == true -> "THE SHADOW REALM"
            currentDestination?.hasRoute<MonarchStatusRoute>() == true -> "MONARCH STATUS"
            currentDestination?.hasRoute<ShadowExtractionRoute>() == true -> "SHADOW EXTRACTION"
            currentDestination?.hasRoute<ShadowCollectionListRoute>() == true -> "SHADOW ROSTER"
            currentDestination?.hasRoute<ShadowDetailRoute>() == true -> "SHADOW PROFILE"
            currentDestination?.hasRoute<ShadowEvolutionRoute>() == true -> "SHADOW EVOLUTION"
            currentDestination?.hasRoute<ShadowSkillTreeRoute>() == true -> "SHADOW SKILL TREE"
            currentDestination?.hasRoute<ShadowDeploymentRoute>() == true -> "SHADOW DEPLOYMENT"
            currentDestination?.hasRoute<ArmyFormationManagerRoute>() == true -> "ARMY FORMATIONS"
            currentDestination?.hasRoute<MonarchSkillTreeRoute>() == true -> "MONARCH SKILLS"
            currentDestination?.hasRoute<ShadowQuestBoardRoute>() == true -> "MONARCH QUESTS"
            currentDestination?.hasRoute<ShadowArmyAnalyticsRoute>() == true -> "ARMY ANALYTICS"
            currentDestination?.hasRoute<ShadowArchiveRoute>() == true -> "SHADOW ARCHIVE"
            currentDestination?.hasRoute<ShadowSettingsRoute>() == true -> "SHADOW SETTINGS"
            else -> null
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(SystemColors.ObsidianBlack)
        ) {
            val useRail = maxWidth >= 600.dp

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = SystemColors.ObsidianBlack,
                contentWindowInsets = WindowInsets.safeDrawing,
                topBar = {
                    if (secondaryTitle != null) {
                        TopAppBar(
                            title = {
                                Text(
                                    text = secondaryTitle,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = SystemColors.PureIce
                                )
                            },
                            navigationIcon = {
                                IconButton(
                                    onClick = { navController.popBackStack() },
                                    modifier = Modifier.testTag("top_bar_back")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = "Navigate back",
                                        tint = SystemColors.ElectricCyan
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = SystemColors.DeepNavy
                            )
                        )
                    }
                },
                bottomBar = {
                    if (!useRail && secondaryTitle == null) {
                        ColumnNavigationBar(
                            navController = navController,
                            currentDestination = currentDestination
                        )
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useRail) {
                        NavigationRail(
                            containerColor = SystemColors.DeepNavy,
                            contentColor = SystemColors.PureIce
                        ) {
                            TopLevelItems.forEach { item ->
                                val selected = isRouteSelected(currentDestination, item.route)
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateTopLevel(navController, item.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label
                                        )
                                    },
                                    label = {
                                        Text(item.label, style = MaterialTheme.typography.labelSmall)
                                    },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = SystemColors.ElectricCyan,
                                        selectedTextColor = SystemColors.ElectricCyan,
                                        indicatorColor = SystemColors.ElectricCyan.copy(alpha = 0.16f),
                                        unselectedIconColor = SystemColors.MutedSilver,
                                        unselectedTextColor = SystemColors.MutedSilver
                                    ),
                                    modifier = Modifier.testTag(item.testTag)
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        when (val current = uiState) {
                            is RealityOsUiState.Loading -> SystemLoadingView()
                            is RealityOsUiState.Error -> Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp)
                            ) {
                                SystemErrorState(
                                    message = current.message,
                                    onRetry = viewModel::retryInitialization
                                )
                            }
                            is RealityOsUiState.Ready -> RealityOsNavHost(
                                navController = navController,
                                state = current,
                                viewModel = viewModel,
                                academicState = academicUiState,
                                academicViewModel = academicViewModel,
                                aiSystemState = aiSystemUiState,
                                aiSystemViewModel = aiSystemViewModel,
                                lifeState = lifeUiState,
                                lifeViewModel = lifeManagementViewModel,
                                rpgState = rpgUiState,
                                rpgViewModel = rpgViewModel,
                                cloudSyncState = cloudSyncUiState,
                                cloudSyncViewModel = cloudSyncViewModel,
                                intelligenceState = intelligenceUiState,
                                intelligenceViewModel = intelligenceViewModel,
                                systemControlState = systemControlUiState,
                                systemControlViewModel = systemControlViewModel,
                                systemExperienceState = systemExperienceUiState,
                                systemExperienceViewModel = systemExperienceViewModel,
                                shadowRealmViewModel = shadowRealmViewModel
                            )
                        }
                    }
                }
            }

            // Milestone 10: Cinematic Awakening Sequence (first launch or replay)
            if (expReady != null && !expReady.preferences.hasCompletedAwakening) {
                BackHandler {
                    systemExperienceViewModel.completeAwakening()
                }
                AwakeningScreen(
                    player = expReady.player,
                    animationsEnabled = expReady.preferences.animationsEnabled,
                    reducedMotionEnabled = expReady.preferences.reducedMotionEnabled,
                    onStageShown = { stage ->
                        systemExperienceViewModel.speakAwakeningStage(stage, expReady.player)
                    },
                    onComplete = systemExperienceViewModel::completeAwakening
                )
            }

            // Milestone 10: Event-Driven Level-Up / Rank-Promotion Celebration Overlay
            val activeCelebration = expReady?.activeReplayCelebration
                ?: expReady?.unacknowledgedCelebrations?.firstOrNull()
            if (activeCelebration != null && expReady != null && expReady.preferences.hasCompletedAwakening) {
                CelebrationModalOverlay(
                    event = activeCelebration,
                    animationsEnabled = expReady.preferences.animationsEnabled,
                    reducedMotionEnabled = expReady.preferences.reducedMotionEnabled,
                    onAcknowledge = {
                        systemExperienceViewModel.acknowledgeCelebration(activeCelebration.celebrationId)
                    },
                    onOpenStatus = {
                        navigateTopLevel(navController, StatusRoute)
                    }
                )
            }
        }
    }
}

@Composable
private fun ColumnNavigationBar(
    navController: NavHostController,
    currentDestination: androidx.navigation.NavDestination?
) {
    androidx.compose.foundation.layout.Column {
        HorizontalDivider(thickness = 1.dp, color = SystemColors.SubtleBorder)
        NavigationBar(
            containerColor = SystemColors.DeepNavy,
            tonalElevation = 6.dp
        ) {
            TopLevelItems.forEach { item ->
                val selected = isRouteSelected(currentDestination, item.route)
                NavigationBarItem(
                    selected = selected,
                    onClick = { navigateTopLevel(navController, item.route) },
                    icon = {
                        Icon(
                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.label,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = item.label.uppercase(),
                            style = if (selected) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Normal,
                            maxLines = 1
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SystemColors.ElectricCyan,
                        selectedTextColor = SystemColors.ElectricCyan,
                        indicatorColor = SystemColors.ElectricCyan.copy(alpha = 0.18f),
                        unselectedIconColor = SystemColors.MutedSilver,
                        unselectedTextColor = SystemColors.MutedSilver
                    ),
                    modifier = Modifier.testTag(item.testTag)
                )
            }
        }
    }
}

@Composable
private fun RealityOsNavHost(
    navController: NavHostController,
    state: RealityOsUiState.Ready,
    viewModel: RealityOsViewModel,
    academicState: AcademicUiState,
    academicViewModel: AcademicViewModel,
    aiSystemState: AiSystemUiState,
    aiSystemViewModel: AiSystemViewModel,
    lifeState: LifeManagementUiState,
    lifeViewModel: LifeManagementViewModel,
    rpgState: RpgUiState,
    rpgViewModel: RpgViewModel,
    cloudSyncState: CloudSyncUiState,
    cloudSyncViewModel: CloudSyncViewModel,
    intelligenceState: IntelligenceUiState,
    intelligenceViewModel: IntelligenceViewModel,
    systemControlState: SystemControlUiState,
    systemControlViewModel: SystemControlViewModel,
    systemExperienceState: SystemExperienceUiState,
    systemExperienceViewModel: SystemExperienceViewModel,
    shadowRealmViewModel: ShadowRealmViewModel
) {
    val context = LocalContext.current
    val shadowRealmUiState by shadowRealmViewModel.uiState.collectAsStateWithLifecycle()
    val academicSummary = (academicState as? AcademicUiState.Ready)?.summary ?: AcademicDashboardSummary()
    val lifeSummary = (lifeState as? LifeManagementUiState.Ready)?.dashboard ?: LifeDashboardSummary()
    val rpgReady = rpgState as? RpgUiState.Ready
    val unlockedTitles = rpgReady?.snapshot?.titles?.filter { it.isUnlocked }.orEmpty()
    val unlockedAchievements = rpgReady?.snapshot?.achievements?.filter { it.isUnlocked }.orEmpty()
    val expReady = systemExperienceState as? SystemExperienceUiState.Ready
    val intelReady = intelligenceState as? IntelligenceUiState.Ready
    val intelligenceHeadline = intelReady?.workload?.adjustmentDirective
        ?: intelReady?.profile?.let { "Measured ${it.measuredDataRatioPercent}% • Peak ${it.behavioralPeakWindow}" }
        ?: "Adaptive planning, predictions & personalized coaching ready"
    val systemHealthStatusLabel = (systemControlState as? SystemControlUiState.Ready)
        ?.healthReport?.overallStatus?.label ?: "Active"
    NavHost(
        navController = navController,
        startDestination = HomeRoute
    ) {
        composable<HomeRoute> {
            HomeScreen(
                state = state,
                onNavigateToStatus = { navigateTopLevel(navController, StatusRoute) },
                onNavigateToPhysical = { navigateTopLevel(navController, PhysicalRoute) },
                onNavigateToQuestBoard = { navigateTopLevel(navController, QuestBoardRoute) },
                onNavigateToAiSystem = { navController.navigate(AiSystemRoute) },
                onCompleteQuest = viewModel::completeQuest,
                onLogManualActivity = viewModel::logManualActivity,
                onAcknowledgeNotification = viewModel::acknowledgeNotification,
                onClearReadNotifications = viewModel::clearReadNotifications,
                academicSummary = academicSummary,
                onNavigateToAcademic = { navController.navigate(AcademicDashboardRoute) },
                lifeSummary = lifeSummary,
                onNavigateToLife = { navController.navigate(LifeDashboardRoute) },
                intelligenceHeadline = intelligenceHeadline,
                onNavigateToIntelligence = { navController.navigate(IntelligenceDashboardRoute) },
                systemHealthStatusLabel = systemHealthStatusLabel,
                onNavigateToSystemControl = { navController.navigate(SystemOverviewRoute) },
                onNavigateToAiCompanion = { navController.navigate(AiCompanionRoute) },
                onNavigateToVoiceConsole = { navController.navigate(VoiceCommandRoute) },
                announcements = expReady?.announcements.orEmpty(),
                onNavigateToShadowRealm = { navController.navigate(ShadowRealmDashboardRoute) },
                monarchProfile = shadowRealmUiState.monarchProfile,
                shadowEssenceBalance = shadowRealmUiState.shadowEssenceBalance,
                onNavigateRoute = { routeKey ->
                    handleDeepLinkRoute(routeKey, navController, systemExperienceViewModel)
                },
                reducedMotionEnabled = expReady?.preferences?.reducedMotionEnabled == true
            )
        }
        composable<StatusRoute> {
            StatusScreen(
                state = state,
                academicSummary = academicSummary,
                lifeSummary = lifeSummary,
                unlockedTitles = unlockedTitles,
                unlockedAchievements = unlockedAchievements,
                onOpenTitles = { navController.navigate(TitleCollectionRoute) },
                onOpenAchievements = { navController.navigate(AchievementsRoute) },
                onNavigateToPhysical = { navigateTopLevel(navController, PhysicalRoute) },
                onNavigateToAcademic = { navController.navigate(AcademicDashboardRoute) },
                onNavigateToLife = { navController.navigate(LifeDashboardRoute) }
            )
        }
        composable<PhysicalRoute> {
            PhysicalDevelopmentScreen(
                state = state,
                onStartActiveJogging = viewModel::startActiveJogging,
                onPauseResumeActiveJogging = viewModel::pauseOrResumeActiveJogging,
                onDiscardActiveJogging = viewModel::discardActiveJogging,
                onCompleteActiveJogging = viewModel::completeActiveJogging,
                onLogManualJogging = viewModel::logManualJoggingSession,
                onRecordStructuredWorkout = viewModel::recordStructuredWorkout,
                onSubmitRecoveryCheckIn = viewModel::submitRecoveryCheckIn,
                onMarkRestDay = viewModel::markPenaltyFreeRestDay,
                onAcceptProposal = viewModel::acceptJoggingTargetProposal,
                onDismissProposal = viewModel::dismissJoggingTargetProposal,
                onRefreshPermissions = viewModel::refreshHealthConnectPermissions,
                onSyncHealthConnect = viewModel::syncHealthConnect,
                onClearFeedback = viewModel::clearSyncFeedback
            )
        }
        composable<QuestBoardRoute> {
            QuestBoardScreen(
                state = state,
                onCompleteQuest = viewModel::completeQuest,
                onCreateQuest = viewModel::createCustomQuest
            )
        }
        composable<AiSystemRoute> {
            AiSystemScreen(
                uiState = aiSystemState,
                onRetryInit = viewModel::retryInitialization,
                onRefreshAnalysis = aiSystemViewModel::refreshAnalysis,
                onSelectConversation = aiSystemViewModel::selectConversation,
                onNewConversation = aiSystemViewModel::startNewConversation,
                onDeleteConversation = aiSystemViewModel::deleteConversation,
                onClearAllConversations = aiSystemViewModel::clearAllConversations,
                onSendChatMessage = aiSystemViewModel::sendChatMessage,
                onRetryLastPrompt = aiSystemViewModel::retryLastPrompt,
                onGenerateQuestProposals = aiSystemViewModel::generateQuestProposals,
                onApproveQuestProposal = aiSystemViewModel::approveQuestProposal,
                onRejectQuestProposal = aiSystemViewModel::rejectQuestProposal,
                onSelectPlanMode = aiSystemViewModel::selectPlanMode,
                onGeneratePlan = aiSystemViewModel::generatePlanForSelectedMode,
                onApprovePlanItem = aiSystemViewModel::approvePlanItem,
                onRejectPlanItem = aiSystemViewModel::rejectPlanItem,
                onSelectModel = aiSystemViewModel::selectModel,
                onSaveBackendUrl = aiSystemViewModel::updateBackendBaseUrl,
                onSetAiEnabled = aiSystemViewModel::setAiEnabled,
                onSetStreamingEnabled = aiSystemViewModel::setStreamingEnabled,
                onUpdateConsent = aiSystemViewModel::updateConsent,
                onUpdateNotificationPrefs = aiSystemViewModel::updateNotificationPrefs,
                onClearFeedback = aiSystemViewModel::clearFeedback,
                onTriggerNotificationEvaluation = {
                    aiSystemViewModel.triggerNotificationEvaluation(context)
                }
            )
        }
        composable<ModulesHubRoute> {
            ModulesHubScreen(
                onOpenAcademicEngine = { navController.navigate(AcademicDashboardRoute) },
                onOpenDungeonGate = { navController.navigate(DungeonGateRoute) },
                onOpenSkills = { navController.navigate(SkillsRoute) },
                onOpenAchievements = { navController.navigate(AchievementsRoute) },
                onOpenHistory = { navController.navigate(HistoryRoute) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
                onOpenAiSystem = { navController.navigate(AiSystemRoute) },
                onOpenLifeEngine = { navController.navigate(LifeDashboardRoute) },
                onOpenBossBattles = { navController.navigate(BossBattleDashboardRoute) },
                onOpenTitles = { navController.navigate(TitleCollectionRoute) },
                onOpenInventory = { navController.navigate(PlayerInventoryRoute) },
                onOpenShadowCollection = { navController.navigate(ShadowCollectionRoute) },
                onOpenRpgRewardHistory = { navController.navigate(RpgRewardHistoryRoute) },
                onOpenCloudSync = {
                    if (cloudSyncState.identity != null) {
                        navController.navigate(CloudSyncDashboardRoute)
                    } else {
                        navController.navigate(AccountGatewayRoute)
                    }
                },
                onOpenIntelligenceCenter = { navController.navigate(IntelligenceDashboardRoute) },
                onOpenSystemControl = { navController.navigate(SystemOverviewRoute) },
                onOpenAiCompanion = { navController.navigate(AiCompanionRoute) },
                onOpenVoiceConsole = { navController.navigate(VoiceCommandRoute) },
                onOpenSystemVoiceSettings = { navController.navigate(SystemVoiceSettingsRoute) },
                onOpenWidgetHub = { navController.navigate(WidgetHubRoute) },
                onOpenCelebrationVault = { navController.navigate(CelebrationHistoryRoute) },
                onOpenShadowRealm = { navController.navigate(ShadowRealmDashboardRoute) },
                onOpenSocialMastery = { navController.navigate(com.example.ui.navigation.SocialMasteryDashboardRoute) }
            )
        }
        // Milestone 6: Dungeons, Boss Battles, Skills & RPG Systems (17 Screens)
        composable<DungeonGateRoute> {
            DungeonGateDashboardScreen(
                uiState = rpgState,
                onRetry = rpgViewModel::refreshAndSync,
                onClearFeedback = rpgViewModel::clearFeedback,
                onOpenDiscovery = { navController.navigate(DungeonDiscoveryRoute) },
                onOpenDungeonDetails = { dId -> navController.navigate(DungeonDetailsRoute(dId)) },
                onOpenActiveDungeon = { instId -> navController.navigate(ActiveDungeonRoute(instId)) },
                onOpenHistory = { navController.navigate(DungeonHistoryRoute) },
                onOpenBossBattles = { navController.navigate(BossBattleDashboardRoute) },
                onOpenRewardHistory = { navController.navigate(RpgRewardHistoryRoute) }
            )
        }
        composable<DungeonDiscoveryRoute> {
            DungeonDiscoveryScreen(
                uiState = rpgState,
                onRetry = rpgViewModel::refreshAndSync,
                onClearFeedback = rpgViewModel::clearFeedback,
                onOpenDungeonDetails = { dId -> navController.navigate(DungeonDetailsRoute(dId)) },
                onGenerateAiProposals = rpgViewModel::generateAiDungeonProposals,
                onApproveAiProposal = rpgViewModel::approveAiDungeonProposal,
                onRejectAiProposal = rpgViewModel::rejectAiDungeonProposal
            )
        }
        composable<DungeonDetailsRoute> { entry ->
            val args = entry.toRoute<DungeonDetailsRoute>()
            DungeonDetailsScreen(
                dungeonId = args.dungeonId,
                uiState = rpgState,
                onRetry = rpgViewModel::refreshAndSync,
                onClearFeedback = rpgViewModel::clearFeedback,
                onEnterDungeon = { dId ->
                    rpgViewModel.enterDungeon(dId) { instId ->
                        navController.navigate(ActiveDungeonRoute(instId))
                    }
                },
                onOpenActiveDungeon = { instId -> navController.navigate(ActiveDungeonRoute(instId)) }
            )
        }
        composable<ActiveDungeonRoute> { entry ->
            val args = entry.toRoute<ActiveDungeonRoute>()
            ActiveDungeonScreen(
                instanceId = args.instanceId,
                uiState = rpgState,
                onRetry = rpgViewModel::refreshAndSync,
                onClearFeedback = rpgViewModel::clearFeedback,
                onPauseResume = rpgViewModel::pauseOrResumeDungeon,
                onCompleteDungeon = rpgViewModel::completeDungeon,
                onAbandonDungeon = { instId ->
                    rpgViewModel.abandonDungeon(instId) {
                        navController.popBackStack()
                    }
                }
            )
        }
        composable<DungeonHistoryRoute> {
            DungeonHistoryScreen(
                uiState = rpgState,
                onRetry = rpgViewModel::refreshAndSync
            )
        }
        composable<BossBattleDashboardRoute> {
            BossBattleDashboardScreen(
                uiState = rpgState,
                onRetry = rpgViewModel::refreshAndSync,
                onClearFeedback = rpgViewModel::clearFeedback,
                onOpenBossDetails = { bId -> navController.navigate(BossBattleDetailsRoute(bId)) },
                onOpenActiveBoss = { instId -> navController.navigate(ActiveBossBattleRoute(instId)) }
            )
        }
        composable<BossBattleDetailsRoute> { entry ->
            val args = entry.toRoute<BossBattleDetailsRoute>()
            BossBattleDetailsScreen(
                bossId = args.bossId,
                uiState = rpgState,
                onRetry = rpgViewModel::refreshAndSync,
                onClearFeedback = rpgViewModel::clearFeedback,
                onStartBossBattle = { bId ->
                    rpgViewModel.startBossBattle(bId) { instId ->
                        navController.navigate(ActiveBossBattleRoute(instId))
                    }
                },
                onOpenActiveBoss = { instId -> navController.navigate(ActiveBossBattleRoute(instId)) }
            )
        }
        composable<ActiveBossBattleRoute> { entry ->
            val args = entry.toRoute<ActiveBossBattleRoute>()
            ActiveBossBattleScreen(
                instanceId = args.instanceId,
                uiState = rpgState,
                onRetry = rpgViewModel::refreshAndSync,
                onClearFeedback = rpgViewModel::clearFeedback,
                onRefreshAndClaim = rpgViewModel::refreshAndClaimBossBattle,
                onAbandonBoss = { instId ->
                    rpgViewModel.abandonBossBattle(instId) {
                        navController.popBackStack()
                    }
                }
            )
        }
        composable<SkillsRoute> {
            SkillDashboardScreen(
                uiState = rpgState,
                onRetry = rpgViewModel::refreshAndSync,
                onClearFeedback = rpgViewModel::clearFeedback,
                onOpenTreeExplorer = { spec -> navController.navigate(SkillTreeExplorerRoute(spec?.name)) },
                onOpenSkillDetails = { sId -> navController.navigate(SkillDetailsRoute(sId)) }
            )
        }
        composable<SkillTreeExplorerRoute> { entry ->
            val args = entry.toRoute<SkillTreeExplorerRoute>()
            SkillTreeExplorerScreen(
                initialSpecializationName = args.specializationName,
                uiState = rpgState,
                onRetry = rpgViewModel::refreshAndSync,
                onClearFeedback = rpgViewModel::clearFeedback,
                onOpenSkillDetails = { sId -> navController.navigate(SkillDetailsRoute(sId)) }
            )
        }
        composable<SkillDetailsRoute> { entry ->
            val args = entry.toRoute<SkillDetailsRoute>()
            SkillDetailsScreen(
                skillId = args.skillId,
                uiState = rpgState,
                onRetry = rpgViewModel::refreshAndSync,
                onClearFeedback = rpgViewModel::clearFeedback,
                onUnlockOrUpgrade = rpgViewModel::unlockOrUpgradeSkill
            )
        }
        composable<AchievementsRoute> {
            AchievementGalleryScreen(
                uiState = rpgState,
                onRetry = rpgViewModel::refreshAndSync,
                onClearFeedback = rpgViewModel::clearFeedback,
                onOpenAchievementDetails = { aId -> navController.navigate(AchievementDetailsRoute(aId)) },
                onOpenTitles = { navController.navigate(TitleCollectionRoute) },
                onOpenInventory = { navController.navigate(PlayerInventoryRoute) },
                onOpenShadows = { navController.navigate(ShadowCollectionRoute) },
                onOpenRewardHistory = { navController.navigate(RpgRewardHistoryRoute) }
            )
        }
        composable<AchievementDetailsRoute> { entry ->
            val args = entry.toRoute<AchievementDetailsRoute>()
            AchievementDetailsScreen(
                achievementId = args.achievementId,
                uiState = rpgState,
                onRetry = rpgViewModel::refreshAndSync
            )
        }
        composable<TitleCollectionRoute> {
            TitleCollectionScreen(
                uiState = rpgState,
                onRetry = rpgViewModel::refreshAndSync,
                onClearFeedback = rpgViewModel::clearFeedback,
                onEquipTitle = rpgViewModel::equipTitle
            )
        }
        composable<PlayerInventoryRoute> {
            PlayerInventoryScreen(
                uiState = rpgState,
                onRetry = rpgViewModel::refreshAndSync,
                onClearFeedback = rpgViewModel::clearFeedback,
                onToggleEquip = rpgViewModel::toggleEquipInventoryItem
            )
        }
        composable<ShadowCollectionRoute> {
            ShadowCollectionScreen(
                uiState = rpgState,
                onRetry = rpgViewModel::refreshAndSync
            )
        }
        composable<RpgRewardHistoryRoute> {
            RpgRewardHistoryScreen(
                uiState = rpgState,
                onRetry = rpgViewModel::refreshAndSync
            )
        }
        composable<HistoryRoute> {
            HistoryScreen(state = state)
        }
        composable<SettingsRoute> {
            SettingsScreen(
                state = state,
                onUpdateCodename = viewModel::updateCodename,
                onUpdateJoggingBaseline = viewModel::updateJoggingBaseline,
                onSetHudGlow = viewModel::setHudGlowEnabled,
                onSetHealthConnectSync = viewModel::setHealthConnectSyncEnabled,
                onSetSupabaseSync = viewModel::setSupabaseCloudSyncEnabled,
                syncSummary = cloudSyncState.summary,
                onOpenCloudGateway = { navController.navigate(AccountGatewayRoute) },
                onOpenCloudSyncDashboard = { navController.navigate(CloudSyncDashboardRoute) },
                onOpenSystemVoiceSettings = { navController.navigate(SystemVoiceSettingsRoute) },
                onOpenWidgetHub = { navController.navigate(WidgetHubRoute) },
                onReplayAwakening = systemExperienceViewModel::replayAwakening
            )
        }

        // Milestone 3: Academic Development Engine (10 Screens)
        composable<AcademicDashboardRoute> {
            AcademicDashboardScreen(
                uiState = academicState,
                onRetry = viewModel::retryInitialization,
                onClearFeedback = academicViewModel::clearFeedback,
                onOpenSubjects = { navController.navigate(AcademicSubjectsRoute) },
                onOpenSubjectDetail = { id -> navController.navigate(AcademicSubjectDetailRoute(id)) },
                onOpenFocusTimer = { navController.navigate(AcademicFocusTimerRoute()) },
                onOpenQuests = { navController.navigate(AcademicQuestsRoute) },
                onOpenRevisions = { navController.navigate(AcademicRevisionsRoute) },
                onOpenExams = { navController.navigate(AcademicExamsRoute) },
                onOpenAnalytics = { navController.navigate(AcademicAnalyticsRoute) },
                onOpenHistory = { navController.navigate(AcademicHistoryRoute) },
                onApproveProposal = academicViewModel::approveStudyPlanProposal,
                onDismissProposal = academicViewModel::dismissStudyPlanProposal
            )
        }
        composable<AcademicSubjectsRoute> {
            AcademicSubjectsScreen(
                uiState = academicState,
                onRetry = viewModel::retryInitialization,
                onCreateSubject = academicViewModel::createSubject,
                onUpdateSubject = academicViewModel::updateSubject,
                onSetArchived = academicViewModel::setSubjectArchived,
                onOpenSubjectDetail = { id -> navController.navigate(AcademicSubjectDetailRoute(id)) }
            )
        }
        composable<AcademicSubjectDetailRoute> { entry ->
            val args = entry.toRoute<AcademicSubjectDetailRoute>()
            AcademicSubjectDetailScreen(
                subjectId = args.subjectId,
                uiState = academicState,
                onRetry = viewModel::retryInitialization,
                onAddModule = academicViewModel::addModule,
                onAddChapter = academicViewModel::addChapter,
                onToggleChapterCompleted = academicViewModel::setChapterCompleted,
                onOpenChapterTopics = { sId, mId, cId ->
                    navController.navigate(AcademicChapterTopicsRoute(sId, mId, cId))
                },
                onStartFocusForSubject = { sId ->
                    navController.navigate(AcademicFocusTimerRoute(preselectedSubjectId = sId))
                },
                onGenerateQuestsForSubject = academicViewModel::generateQuestsForSubject
            )
        }
        composable<AcademicChapterTopicsRoute> { entry ->
            val args = entry.toRoute<AcademicChapterTopicsRoute>()
            AcademicChapterTopicsScreen(
                subjectId = args.subjectId,
                chapterId = args.chapterId,
                uiState = academicState,
                onRetry = viewModel::retryInitialization,
                onToggleChapterCompleted = academicViewModel::setChapterCompleted,
                onAddTopic = academicViewModel::addTopic,
                onToggleTopicCompleted = academicViewModel::toggleTopicCompleted,
                onReviseTopic = academicViewModel::completeTopicRevision
            )
        }
        composable<AcademicFocusTimerRoute> { entry ->
            val args = entry.toRoute<AcademicFocusTimerRoute>()
            AcademicFocusTimerScreen(
                preselectedSubjectId = args.preselectedSubjectId,
                uiState = academicState,
                onRetry = viewModel::retryInitialization,
                onStartTimer = academicViewModel::startFocusTimer,
                onPauseResume = academicViewModel::pauseOrResumeFocusTimer,
                onToggleBreakPhase = academicViewModel::toggleFocusBreakPhase,
                onCompleteTimer = academicViewModel::completeFocusTimer,
                onDiscardTimer = academicViewModel::discardFocusTimer,
                onLogManualStudy = academicViewModel::logManualStudySession
            )
        }
        composable<AcademicQuestsRoute> {
            AcademicQuestsScreen(
                uiState = academicState,
                onRetry = viewModel::retryInitialization,
                onCreateQuest = academicViewModel::createAcademicQuest,
                onGenerateFromPlan = academicViewModel::generateQuestsFromStudyPlan,
                onCompleteQuest = academicViewModel::completeAcademicQuest,
                onCancelQuest = academicViewModel::cancelAcademicQuest
            )
        }
        composable<AcademicRevisionsRoute> {
            AcademicRevisionsScreen(
                uiState = academicState,
                onRetry = viewModel::retryInitialization,
                onCompleteRevision = academicViewModel::completeTopicRevision
            )
        }
        composable<AcademicExamsRoute> {
            AcademicExamsScreen(
                uiState = academicState,
                onRetry = viewModel::retryInitialization,
                onCreateExam = academicViewModel::createExam,
                onGeneratePrepQuests = academicViewModel::generateExamPrepQuests,
                onRecordAssessment = academicViewModel::recordAssessmentResult
            )
        }
        composable<AcademicAnalyticsRoute> {
            AcademicAnalyticsScreen(
                uiState = academicState,
                onRetry = viewModel::retryInitialization
            )
        }
        composable<AcademicHistoryRoute> {
            AcademicHistoryScreen(
                uiState = academicState,
                onRetry = viewModel::retryInitialization
            )
        }

        // Milestone 5: Life Management & Discipline Engine (14 Screens)
        composable<LifeDashboardRoute> {
            LifeDashboardScreen(
                uiState = lifeState,
                onRetry = lifeViewModel::refreshScheduleAndReview,
                onClearFeedback = lifeViewModel::clearFeedback,
                onToggleRestDay = lifeViewModel::toggleExemptRestDay,
                onOpenRoutines = { navController.navigate(RoutineManagerRoute) },
                onOpenHabits = { navController.navigate(HabitDashboardRoute) },
                onOpenTasks = { navController.navigate(PersonalTaskManagerRoute) },
                onOpenGoals = { navController.navigate(GoalManagementRoute) },
                onOpenDailySchedule = { navController.navigate(DailyScheduleRoute) },
                onOpenWeeklyCalendar = { navController.navigate(WeeklyCalendarRoute) },
                onOpenMonthlyCalendar = { navController.navigate(MonthlyCalendarRoute) },
                onOpenDisciplineAnalytics = { navController.navigate(DisciplineAnalyticsRoute) },
                onOpenWeeklyReview = { navController.navigate(WeeklyReviewRoute) }
            )
        }
        composable<RoutineManagerRoute> {
            RoutineManagerScreen(
                uiState = lifeState,
                onRetry = lifeViewModel::refreshScheduleAndReview,
                onCreateRoutine = lifeViewModel::createRoutine,
                onToggleEnabled = lifeViewModel::toggleRoutineEnabled,
                onDeleteRoutine = lifeViewModel::deleteRoutine,
                onAddStep = lifeViewModel::addRoutineStep,
                onMoveStep = lifeViewModel::moveRoutineStep,
                onSetStepStatus = lifeViewModel::setRoutineStepStatus
            )
        }
        composable<HabitDashboardRoute> {
            HabitDashboardScreen(
                uiState = lifeState,
                onRetry = lifeViewModel::refreshScheduleAndReview,
                onLogHabit = lifeViewModel::logHabit,
                onOpenHabitEditor = { hId -> navController.navigate(HabitEditorRoute(hId)) },
                onOpenHabitAnalytics = { hId -> navController.navigate(HabitAnalyticsRoute(hId)) },
                onArchiveHabit = lifeViewModel::archiveHabit
            )
        }
        composable<HabitEditorRoute> { entry ->
            val args = entry.toRoute<HabitEditorRoute>()
            HabitEditorScreen(
                habitId = args.habitId,
                uiState = lifeState,
                onRetry = lifeViewModel::refreshScheduleAndReview,
                onSaveHabit = lifeViewModel::saveHabit,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable<HabitAnalyticsRoute> { entry ->
            val args = entry.toRoute<HabitAnalyticsRoute>()
            HabitAnalyticsScreen(
                habitId = args.habitId,
                uiState = lifeState,
                onRetry = lifeViewModel::refreshScheduleAndReview
            )
        }
        composable<PersonalTaskManagerRoute> {
            PersonalTaskManagerScreen(
                uiState = lifeState,
                onRetry = lifeViewModel::refreshScheduleAndReview,
                onCreateTask = lifeViewModel::createTask,
                onCompleteTask = lifeViewModel::completeTask,
                onPostponeTask = lifeViewModel::postponeTask,
                onArchiveTask = lifeViewModel::archiveTask,
                onDeleteTask = lifeViewModel::deleteTask,
                onOpenTaskDetails = { tId -> navController.navigate(TaskDetailsRoute(tId)) }
            )
        }
        composable<TaskDetailsRoute> { entry ->
            val args = entry.toRoute<TaskDetailsRoute>()
            TaskDetailsScreen(
                taskId = args.taskId,
                uiState = lifeState,
                onRetry = lifeViewModel::refreshScheduleAndReview,
                onCompleteTask = lifeViewModel::completeTask,
                onPostponeTask = lifeViewModel::postponeTask,
                onRescheduleTask = lifeViewModel::rescheduleTask,
                onArchiveTask = lifeViewModel::archiveTask,
                onDeleteTask = lifeViewModel::deleteTask,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable<GoalManagementRoute> {
            GoalManagementScreen(
                uiState = lifeState,
                onRetry = lifeViewModel::refreshScheduleAndReview,
                onCreateGoal = lifeViewModel::createGoal,
                onOpenGoalDetails = { gId -> navController.navigate(GoalDetailsRoute(gId)) }
            )
        }
        composable<GoalDetailsRoute> { entry ->
            val args = entry.toRoute<GoalDetailsRoute>()
            GoalDetailsScreen(
                goalId = args.goalId,
                uiState = lifeState,
                onRetry = lifeViewModel::refreshScheduleAndReview,
                onAddMilestone = lifeViewModel::addMilestone,
                onCompleteMilestone = lifeViewModel::completeMilestone,
                onSubmitReview = lifeViewModel::submitGoalReview,
                onDeleteGoal = lifeViewModel::deleteGoal,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable<DailyScheduleRoute> {
            DailyScheduleScreen(
                uiState = lifeState,
                onRetry = lifeViewModel::refreshScheduleAndReview,
                onSelectDate = lifeViewModel::selectDate,
                onAddScheduledEvent = lifeViewModel::addScheduledEvent,
                onRescheduleItem = lifeViewModel::rescheduleItem,
                onLoadSuggestions = lifeViewModel::loadSuggestions
            )
        }
        composable<WeeklyCalendarRoute> {
            WeeklyCalendarScreen(
                uiState = lifeState,
                onRetry = lifeViewModel::refreshScheduleAndReview,
                onSelectDateAndOpenDaily = { epochDay ->
                    lifeViewModel.selectDate(epochDay)
                    navController.navigate(DailyScheduleRoute)
                }
            )
        }
        composable<MonthlyCalendarRoute> {
            MonthlyCalendarScreen(
                uiState = lifeState,
                onRetry = lifeViewModel::refreshScheduleAndReview,
                onSelectDateAndOpenDaily = { epochDay ->
                    lifeViewModel.selectDate(epochDay)
                    navController.navigate(DailyScheduleRoute)
                }
            )
        }
        composable<DisciplineAnalyticsRoute> {
            DisciplineAnalyticsScreen(
                uiState = lifeState,
                onRetry = lifeViewModel::refreshScheduleAndReview,
                onSaveReview = lifeViewModel::saveDisciplineReview
            )
        }
        composable<WeeklyReviewRoute> {
            WeeklyReviewScreen(
                uiState = lifeState,
                onRetry = lifeViewModel::refreshScheduleAndReview,
                onRefreshReview = lifeViewModel::refreshScheduleAndReview
            )
        }

        // Milestone 7: Cloud Infrastructure, Auth, Sync & Multi-Device Support (15 Screens)
        composable<AccountGatewayRoute> {
            AccountGatewayScreen(
                uiState = cloudSyncState,
                onOpenSignUp = { navController.navigate(SignUpRoute) },
                onOpenLogin = { navController.navigate(LoginRoute) },
                onOpenSyncDashboard = { navController.navigate(CloudSyncDashboardRoute) },
                onOpenAccountSettings = { navController.navigate(AccountSettingsRoute) },
                onContinueOffline = { navController.popBackStack() }
            )
        }
        composable<SignUpRoute> {
            SignUpScreen(
                uiState = cloudSyncState,
                onSignUp = { email, password, displayName ->
                    cloudSyncViewModel.signUp(email, password, displayName) { identity ->
                        if (identity.emailVerified) {
                            navController.navigate(CloudSyncDashboardRoute)
                        } else {
                            navController.navigate(EmailVerificationRoute)
                        }
                    }
                },
                onOpenLogin = { navController.navigate(LoginRoute) }
            )
        }
        composable<LoginRoute> {
            LoginScreen(
                uiState = cloudSyncState,
                onLogin = { email, password ->
                    cloudSyncViewModel.signIn(email, password, context.applicationContext) { identity ->
                        if (identity.emailVerified) {
                            navController.navigate(CloudSyncDashboardRoute)
                        } else {
                            navController.navigate(EmailVerificationRoute)
                        }
                    }
                },
                onOpenForgotPassword = { navController.navigate(ForgotPasswordRoute) },
                onOpenSignUp = { navController.navigate(SignUpRoute) }
            )
        }
        composable<EmailVerificationRoute> {
            EmailVerificationScreen(
                uiState = cloudSyncState,
                onResendEmail = cloudSyncViewModel::resendVerificationEmail,
                onRefreshStatus = {
                    cloudSyncViewModel.refreshVerificationStatus {
                        navController.navigate(CloudSyncDashboardRoute)
                    }
                },
                onContinueOffline = { navController.popBackStack() }
            )
        }
        composable<ForgotPasswordRoute> {
            ForgotPasswordScreen(
                uiState = cloudSyncState,
                onSendRecovery = { email ->
                    cloudSyncViewModel.sendPasswordRecovery(email) {}
                },
                onOpenResetPassword = { navController.navigate(ResetPasswordRoute) }
            )
        }
        composable<ResetPasswordRoute> {
            ResetPasswordScreen(
                uiState = cloudSyncState,
                onResetPassword = { newPassword ->
                    cloudSyncViewModel.resetPassword(newPassword) {
                        navController.popBackStack()
                    }
                }
            )
        }
        composable<AccountSettingsRoute> {
            AccountSettingsScreen(
                uiState = cloudSyncState,
                onUpdateDisplayName = cloudSyncViewModel::updateDisplayName,
                onToggleCloudConsent = cloudSyncViewModel::setCloudSyncConsent,
                onToggleAutoSync = cloudSyncViewModel::setAutoSyncEnabled,
                onOpenSyncDashboard = { navController.navigate(CloudSyncDashboardRoute) },
                onOpenDevices = { navController.navigate(RegisteredDevicesRoute) },
                onOpenBackups = { navController.navigate(CloudBackupRoute) },
                onOpenResetPassword = { navController.navigate(ResetPasswordRoute) },
                onOpenDeleteAccount = { navController.navigate(AccountDeletionRoute) },
                onSignOut = {
                    cloudSyncViewModel.signOut {
                        navController.navigate(AccountGatewayRoute)
                    }
                },
                onOpenLogin = { navController.navigate(LoginRoute) }
            )
        }
        composable<AccountDeletionRoute> {
            AccountDeletionScreen(
                uiState = cloudSyncState,
                onConfirmDelete = { phrase, password ->
                    cloudSyncViewModel.deleteAccount(phrase, password) {
                        navController.navigate(AccountGatewayRoute)
                    }
                }
            )
        }
        composable<CloudSyncDashboardRoute> {
            CloudSyncDashboardScreen(
                uiState = cloudSyncState,
                onSyncNow = cloudSyncViewModel::syncNow,
                onOpenPendingChanges = { navController.navigate(PendingChangesRoute) },
                onOpenSyncHistory = { navController.navigate(SyncHistoryRoute) },
                onOpenConflicts = { navController.navigate(ConflictResolutionRoute) },
                onOpenDevices = { navController.navigate(RegisteredDevicesRoute) },
                onOpenBackup = { navController.navigate(CloudBackupRoute) },
                onOpenAccountSettings = { navController.navigate(AccountSettingsRoute) },
                onOpenAuthGateway = { navController.navigate(AccountGatewayRoute) }
            )
        }
        composable<PendingChangesRoute> {
            PendingChangesScreen(
                uiState = cloudSyncState,
                onRetryItem = cloudSyncViewModel::retryQueueItem,
                onSyncNow = cloudSyncViewModel::syncNow
            )
        }
        composable<SyncHistoryRoute> {
            SyncHistoryScreen(uiState = cloudSyncState)
        }
        composable<ConflictResolutionRoute> {
            ConflictResolutionScreen(
                uiState = cloudSyncState,
                onResolveConflict = cloudSyncViewModel::resolveConflict
            )
        }
        composable<RegisteredDevicesRoute> {
            RegisteredDevicesScreen(
                uiState = cloudSyncState,
                onRefreshDevices = cloudSyncViewModel::refreshDevices,
                onRevokeDevice = cloudSyncViewModel::revokeDevice
            )
        }
        composable<CloudBackupRoute> {
            CloudBackupScreen(
                uiState = cloudSyncState,
                onCreateBackup = cloudSyncViewModel::createBackupSnapshot,
                onInspectBackupForRestore = { backupUuid ->
                    cloudSyncViewModel.loadRestorePreview(backupUuid) {
                        navController.navigate(RestorePreviewRoute)
                    }
                }
            )
        }
        composable<RestorePreviewRoute> {
            RestorePreviewScreen(
                uiState = cloudSyncState,
                onExecuteRestore = { backupUuid ->
                    cloudSyncViewModel.executeRestore(backupUuid) {
                        navController.popBackStack()
                    }
                }
            )
        }

        // Milestone 8: Intelligence, Personalization & Adaptive System (15 Screens)
        composable<IntelligenceDashboardRoute> {
            IntelligenceDashboardScreen(
                uiState = intelligenceState,
                onRefresh = { intelligenceViewModel.refreshIntelligence(force = true) },
                onClearFeedback = intelligenceViewModel::clearFeedback,
                onOpenProfile = { navController.navigate(PlayerIntelligenceProfileRoute) },
                onOpenStrengths = { navController.navigate(StrengthsWeaknessesRoute) },
                onOpenDailyPlan = { navController.navigate(AdaptiveDailyPlanRoute) },
                onOpenWorkload = { navController.navigate(WorkloadBalanceRoute) },
                onOpenConsistency = { navController.navigate(ConsistencyAnalyticsRoute) },
                onOpenRecommendations = { navController.navigate(PersonalizedRecommendationsRoute) },
                onOpenAcademicIntel = { navController.navigate(AcademicIntelligenceRoute) },
                onOpenPhysicalIntel = { navController.navigate(PhysicalIntelligenceRoute) },
                onOpenAdaptiveDungeons = { navController.navigate(AdaptiveDungeonsRoute) },
                onOpenRoadmap = { navController.navigate(DevelopmentRoadmapRoute) },
                onOpenWeeklyReport = { navController.navigate(WeeklyIntelligenceReportRoute) },
                onOpenMonthlyReport = { navController.navigate(MonthlyIntelligenceReportRoute) },
                onOpenAdvisor = { navController.navigate(PersonalAdvisorRoute) },
                onOpenPreferences = { navController.navigate(IntelligencePreferencesRoute) }
            )
        }
        composable<PlayerIntelligenceProfileRoute> {
            PlayerIntelligenceProfileScreen(
                uiState = intelligenceState,
                onRefresh = { intelligenceViewModel.refreshIntelligence(force = true) }
            )
        }
        composable<StrengthsWeaknessesRoute> {
            StrengthsWeaknessesScreen(
                uiState = intelligenceState,
                onRefresh = { intelligenceViewModel.refreshIntelligence(force = true) }
            )
        }
        composable<AdaptiveDailyPlanRoute> {
            AdaptiveDailyPlanScreen(
                uiState = intelligenceState,
                onClearFeedback = intelligenceViewModel::clearFeedback,
                onGeneratePlan = intelligenceViewModel::generateDailyPlan,
                onApprovePlan = intelligenceViewModel::approveDailyPlan,
                onUpdateBlockProgress = intelligenceViewModel::updateBlockProgress,
                onRescheduleBlock = intelligenceViewModel::rescheduleBlock
            )
        }
        composable<WorkloadBalanceRoute> {
            WorkloadBalanceScreen(
                uiState = intelligenceState,
                onRefresh = { intelligenceViewModel.refreshIntelligence(force = true) }
            )
        }
        composable<ConsistencyAnalyticsRoute> {
            ConsistencyAnalyticsScreen(
                uiState = intelligenceState,
                onRefresh = { intelligenceViewModel.refreshIntelligence(force = true) }
            )
        }
        composable<PersonalizedRecommendationsRoute> {
            PersonalizedRecommendationsScreen(
                uiState = intelligenceState,
                onRefresh = { intelligenceViewModel.refreshIntelligence(force = true) },
                onClearFeedback = intelligenceViewModel::clearFeedback,
                onRespond = intelligenceViewModel::respondToRecommendation,
                onNavigateDeepLink = { target ->
                    when (target) {
                        "physical" -> navigateTopLevel(navController, PhysicalRoute)
                        "academic_revisions" -> navController.navigate(AcademicRevisionsRoute)
                        "academic_dashboard" -> navController.navigate(AcademicDashboardRoute)
                        "daily_schedule" -> navController.navigate(DailyScheduleRoute)
                        "dungeon_discovery" -> navController.navigate(DungeonDiscoveryRoute)
                        "tasks" -> navController.navigate(PersonalTaskManagerRoute)
                        else -> navController.navigate(IntelligenceDashboardRoute)
                    }
                }
            )
        }
        composable<AcademicIntelligenceRoute> {
            AcademicIntelligenceScreen(
                uiState = intelligenceState,
                onRefresh = { intelligenceViewModel.refreshIntelligence(force = true) }
            )
        }
        composable<PhysicalIntelligenceRoute> {
            PhysicalIntelligenceScreen(
                uiState = intelligenceState,
                onRefresh = { intelligenceViewModel.refreshIntelligence(force = true) }
            )
        }
        composable<AdaptiveDungeonsRoute> {
            AdaptiveDungeonsScreen(
                uiState = intelligenceState,
                onRefresh = { intelligenceViewModel.refreshIntelligence(force = true) },
                onOpenDungeonDetails = { dId -> navController.navigate(DungeonDetailsRoute(dId)) }
            )
        }
        composable<DevelopmentRoadmapRoute> {
            DevelopmentRoadmapScreen(
                uiState = intelligenceState,
                onClearFeedback = intelligenceViewModel::clearFeedback,
                onGenerateRoadmap = intelligenceViewModel::generateRoadmap,
                onApproveRoadmap = intelligenceViewModel::approveRoadmap,
                onToggleMilestone = intelligenceViewModel::toggleRoadmapMilestone,
                onEditMilestone = intelligenceViewModel::editRoadmapMilestone
            )
        }
        composable<WeeklyIntelligenceReportRoute> {
            WeeklyIntelligenceReportScreen(
                uiState = intelligenceState,
                onClearFeedback = intelligenceViewModel::clearFeedback,
                onGenerateReport = { intelligenceViewModel.generateReport(com.example.domain.model.ReportPeriodType.WEEKLY) }
            )
        }
        composable<MonthlyIntelligenceReportRoute> {
            MonthlyIntelligenceReportScreen(
                uiState = intelligenceState,
                onClearFeedback = intelligenceViewModel::clearFeedback,
                onGenerateReport = { intelligenceViewModel.generateReport(com.example.domain.model.ReportPeriodType.MONTHLY) }
            )
        }
        composable<PersonalAdvisorRoute> {
            PersonalAdvisorScreen(
                uiState = intelligenceState,
                onClearFeedback = intelligenceViewModel::clearFeedback,
                onAskAdvisor = intelligenceViewModel::askAdvisor
            )
        }
        composable<IntelligencePreferencesRoute> {
            IntelligencePreferencesScreen(
                uiState = intelligenceState,
                onClearFeedback = intelligenceViewModel::clearFeedback,
                onSavePreferences = intelligenceViewModel::updatePreferences,
                onClearAllData = intelligenceViewModel::clearAllIntelligenceData
            )
        }

        // Milestone 9: System Integration, Automation & Reliability (12 Screens)
        composable<SystemOverviewRoute> {
            SystemOverviewScreen(
                uiState = systemControlState,
                onRetry = { systemControlViewModel.refreshSystemDiagnostics(silent = false) },
                onClearFeedback = systemControlViewModel::clearFeedback,
                onOpenModulesStatus = { navController.navigate(ModuleIntegrationStatusRoute) },
                onOpenAutomations = { navController.navigate(AutomationDashboardRoute) },
                onOpenAutomationHistory = { navController.navigate(AutomationHistoryRoute) },
                onOpenExecutionTimeline = { navController.navigate(DailyExecutionTimelineRoute) },
                onOpenEventHistory = { navController.navigate(SystemEventHistoryRoute) },
                onOpenDataIntegrity = { navController.navigate(DataIntegrityDashboardRoute) },
                onOpenHealthMonitor = { navController.navigate(SystemHealthMonitorRoute) },
                onOpenPerformance = { navController.navigate(PerformanceDiagnosticsRoute) },
                onOpenRecoveryCenter = { navController.navigate(ErrorRecoveryCenterRoute) },
                onOpenNotifications = { navController.navigate(NotificationManagementRoute) },
                onOpenSettings = { navController.navigate(SystemControlSettingsRoute) },
                onOpenAiCompanion = { navController.navigate(AiCompanionRoute) },
                onOpenVoiceConsole = { navController.navigate(VoiceCommandRoute) },
                onOpenSystemVoiceSettings = { navController.navigate(SystemVoiceSettingsRoute) },
                onOpenWidgetHub = { navController.navigate(WidgetHubRoute) },
                onOpenCelebrationVault = { navController.navigate(CelebrationHistoryRoute) },
                onOpenShadowRealm = { navController.navigate(ShadowRealmDashboardRoute) },
                onOpenSocialMastery = { navController.navigate(com.example.ui.navigation.SocialMasteryDashboardRoute) }
            )
        }
        composable<ModuleIntegrationStatusRoute> {
            ModuleIntegrationStatusScreen(
                uiState = systemControlState,
                onRetry = { systemControlViewModel.refreshSystemDiagnostics(silent = false) },
                onClearFeedback = systemControlViewModel::clearFeedback,
                onNavigateToModule = { routeKey ->
                    when (routeKey) {
                        "profile" -> navigateTopLevel(navController, StatusRoute)
                        "physical" -> navigateTopLevel(navController, PhysicalRoute)
                        "academic" -> navController.navigate(AcademicDashboardRoute)
                        "life_management" -> navController.navigate(LifeDashboardRoute)
                        "rpg_hub" -> navController.navigate(DungeonGateRoute)
                        "cloud_sync" -> navController.navigate(CloudSyncDashboardRoute)
                        "intelligence_hub" -> navController.navigate(IntelligenceDashboardRoute)
                        "automation_dashboard" -> navController.navigate(AutomationDashboardRoute)
                        else -> navController.navigate(SystemOverviewRoute)
                    }
                }
            )
        }
        composable<AutomationDashboardRoute> {
            AutomationDashboardScreen(
                uiState = systemControlState,
                onRetry = { systemControlViewModel.refreshSystemDiagnostics(silent = false) },
                onClearFeedback = systemControlViewModel::clearFeedback,
                onToggleJob = systemControlViewModel::toggleAutomationEnabled,
                onRunJobNow = systemControlViewModel::runAutomationNow,
                onRunDailyCycleNow = systemControlViewModel::runDailyCycleNow,
                onOpenHistory = { navController.navigate(AutomationHistoryRoute) }
            )
        }
        composable<AutomationHistoryRoute> {
            AutomationHistoryScreen(
                uiState = systemControlState,
                onRetry = { systemControlViewModel.refreshSystemDiagnostics(silent = false) },
                onClearFeedback = systemControlViewModel::clearFeedback
            )
        }
        composable<DailyExecutionTimelineRoute> {
            DailyExecutionTimelineScreen(
                uiState = systemControlState,
                onRetry = { systemControlViewModel.refreshSystemDiagnostics(silent = false) },
                onClearFeedback = systemControlViewModel::clearFeedback,
                onUpdateStatus = systemControlViewModel::updateExecutionStatus,
                onLoadRescheduleSlots = systemControlViewModel::loadRescheduleSlots,
                onClearReschedule = systemControlViewModel::clearRescheduleSelection
            )
        }
        composable<SystemEventHistoryRoute> {
            SystemEventHistoryScreen(
                uiState = systemControlState,
                onRetry = { systemControlViewModel.refreshSystemDiagnostics(silent = false) },
                onClearFeedback = systemControlViewModel::clearFeedback,
                onRetryEvent = systemControlViewModel::retryEvent
            )
        }
        composable<DataIntegrityDashboardRoute> {
            DataIntegrityDashboardScreen(
                uiState = systemControlState,
                onRetry = { systemControlViewModel.refreshSystemDiagnostics(silent = false) },
                onClearFeedback = systemControlViewModel::clearFeedback,
                onRunScan = systemControlViewModel::runIntegrityScan,
                onRepairFinding = systemControlViewModel::repairIntegrityFinding,
                onDismissFinding = systemControlViewModel::dismissIntegrityFinding
            )
        }
        composable<SystemHealthMonitorRoute> {
            SystemHealthMonitorScreen(
                uiState = systemControlState,
                onRetry = { systemControlViewModel.refreshSystemDiagnostics(silent = false) },
                onClearFeedback = systemControlViewModel::clearFeedback,
                onRunFullRecovery = systemControlViewModel::executeFullRecovery
            )
        }
        composable<PerformanceDiagnosticsRoute> {
            PerformanceDiagnosticsScreen(
                uiState = systemControlState,
                onRetry = { systemControlViewModel.refreshSystemDiagnostics(silent = false) },
                onClearFeedback = systemControlViewModel::clearFeedback
            )
        }
        composable<ErrorRecoveryCenterRoute> {
            ErrorRecoveryCenterScreen(
                uiState = systemControlState,
                onRetry = { systemControlViewModel.refreshSystemDiagnostics(silent = false) },
                onClearFeedback = systemControlViewModel::clearFeedback,
                onExecuteFullRecovery = systemControlViewModel::executeFullRecovery
            )
        }
        composable<NotificationManagementRoute> {
            NotificationManagementScreen(
                uiState = systemControlState,
                onRetry = { systemControlViewModel.refreshSystemDiagnostics(silent = false) },
                onClearFeedback = systemControlViewModel::clearFeedback,
                onUpdateNotificationPrefs = systemControlViewModel::updateNotificationPreferences,
                onMarkRead = systemControlViewModel::markNotificationRead,
                onClearRead = systemControlViewModel::clearReadNotifications
            )
        }
        composable<SystemControlSettingsRoute> {
            SystemControlSettingsScreen(
                uiState = systemControlState,
                onRetry = { systemControlViewModel.refreshSystemDiagnostics(silent = false) },
                onClearFeedback = systemControlViewModel::clearFeedback,
                onUpdateSystemPreferences = systemControlViewModel::updateSystemPreferences,
                experiencePrefs = expReady?.preferences ?: com.example.domain.model.SystemExperiencePreferences(),
                onUpdateExperiencePrefs = systemExperienceViewModel::updatePreferences,
                onReplayAwakening = systemExperienceViewModel::replayAwakening,
                onOpenSystemVoiceSettings = { navController.navigate(SystemVoiceSettingsRoute) },
                onOpenVoiceConsole = { navController.navigate(VoiceCommandRoute) },
                onOpenWidgetHub = { navController.navigate(WidgetHubRoute) },
                onOpenCelebrationVault = { navController.navigate(CelebrationHistoryRoute) },
                onOpenShadowRealm = { navController.navigate(ShadowRealmDashboardRoute) },
                onOpenSocialMastery = { navController.navigate(com.example.ui.navigation.SocialMasteryDashboardRoute) }
            )
        }

        // Milestone 10: The Awakening — Immersive System Experience (5 Screens)
        composable<AiCompanionRoute> {
            AiCompanionScreen(
                uiState = aiSystemState,
                ttsEnabled = expReady?.preferences?.ttsEnabled ?: true,
                onRetryInit = viewModel::retryInitialization,
                onSendChatMessage = aiSystemViewModel::sendChatMessage,
                onRetryLastPrompt = aiSystemViewModel::retryLastPrompt,
                onNewConversation = aiSystemViewModel::startNewConversation,
                onClearAllConversations = aiSystemViewModel::clearAllConversations,
                onSpeakMessage = { msg -> systemExperienceViewModel.speakText(msg) },
                onOpenVoiceConsole = { navController.navigate(VoiceCommandRoute) },
                onOpenAiSettings = { navController.navigate(AiSystemRoute) },
                onSelectConversation = aiSystemViewModel::selectConversation,
                onDeleteConversation = aiSystemViewModel::deleteConversation,
                onStopGeneration = aiSystemViewModel::stopGeneration,
                onRegenerateResponse = aiSystemViewModel::regenerateLastResponse,
                onEnableCloudConsent = { aiSystemViewModel.updateConsent(cloudConsent = true) }
            )
        }
        composable<VoiceCommandRoute> {
            VoiceCommandScreen(
                uiState = systemExperienceState,
                onRetry = viewModel::retryInitialization,
                onSubmitTranscript = { transcript ->
                    systemExperienceViewModel.submitVoiceTranscript(transcript) { route ->
                        handleDeepLinkRoute(route, navController, systemExperienceViewModel)
                    }
                },
                onConfirmPending = {
                    systemExperienceViewModel.confirmPendingVoiceCommand { route ->
                        handleDeepLinkRoute(route, navController, systemExperienceViewModel)
                    }
                },
                onDismissPending = systemExperienceViewModel::dismissPendingVoiceConfirmation,
                onClearHistory = systemExperienceViewModel::clearVoiceHistory,
                onSelectVoiceMode = systemExperienceViewModel::setVoiceMode,
                onStopVoice = systemExperienceViewModel::stopVoice,
                onEndVoiceSession = systemExperienceViewModel::endVoiceConversationSession,
                onOpenAiCompanion = { navController.navigate(AiCompanionRoute) },
                onOpenVoiceSettings = { navController.navigate(SystemVoiceSettingsRoute) }
            )
        }
        composable<SystemVoiceSettingsRoute> {
            SystemVoiceSettingsScreen(
                uiState = systemExperienceState,
                onRetry = viewModel::retryInitialization,
                onUpdatePreferences = systemExperienceViewModel::updatePreferences,
                onTestVoice = systemExperienceViewModel::testSystemVoice,
                onStopVoice = systemExperienceViewModel::stopVoice,
                onMarkAnnouncementRead = systemExperienceViewModel::markAnnouncementRead,
                onMarkAllRead = systemExperienceViewModel::markAllAnnouncementsRead,
                onUpdateVoicePermissions = systemExperienceViewModel::updateVoiceCommandPermissions
            )
        }
        composable<WidgetHubRoute> {
            WidgetHubScreen(
                uiState = systemExperienceState,
                onRetry = viewModel::retryInitialization,
                onRefreshWidgets = systemExperienceViewModel::refreshWidgets
            )
        }
        composable<CelebrationHistoryRoute> {
            CelebrationHistoryScreen(
                celebrations = expReady?.celebrationHistory.orEmpty(),
                onReplayCelebration = systemExperienceViewModel::replayCelebration
            )
        }

        // Milestone 11: The Shadow Monarch — Shadow Army System (14 Screens)
        composable<ShadowRealmDashboardRoute> {
            ShadowRealmDashboardScreen(
                viewModel = shadowRealmViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToMonarchStatus = { navController.navigate(MonarchStatusRoute) },
                onNavigateToExtraction = { navController.navigate(ShadowExtractionRoute) },
                onNavigateToCollection = { navController.navigate(ShadowCollectionListRoute) },
                onNavigateToDeployment = { navController.navigate(ShadowDeploymentRoute) },
                onNavigateToFormations = { navController.navigate(ArmyFormationManagerRoute) },
                onNavigateToMonarchSkills = { navController.navigate(MonarchSkillTreeRoute) },
                onNavigateToQuests = { navController.navigate(ShadowQuestBoardRoute) },
                onNavigateToAnalytics = { navController.navigate(ShadowArmyAnalyticsRoute) },
                onNavigateToArchive = { navController.navigate(ShadowArchiveRoute) },
                onNavigateToSettings = { navController.navigate(ShadowSettingsRoute) }
            )
        }
        composable<MonarchStatusRoute> {
            MonarchStatusScreen(
                viewModel = shadowRealmViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable<ShadowExtractionRoute> {
            ShadowExtractionScreen(
                viewModel = shadowRealmViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable<ShadowCollectionListRoute> {
            ShadowMonarchCollectionScreen(
                viewModel = shadowRealmViewModel,
                onNavigateBack = { navController.popBackStack() },
                onSelectShadow = { shadowId -> navController.navigate(ShadowDetailRoute(shadowId)) }
            )
        }
        composable<ShadowDetailRoute> { backStackEntry ->
            val args = backStackEntry.toRoute<ShadowDetailRoute>()
            ShadowDetailScreen(
                shadowId = args.shadowId,
                viewModel = shadowRealmViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEvolution = { sId -> navController.navigate(ShadowEvolutionRoute(sId)) },
                onNavigateToSkillTree = { sId -> navController.navigate(ShadowSkillTreeRoute(sId)) }
            )
        }
        composable<ShadowEvolutionRoute> { backStackEntry ->
            val args = backStackEntry.toRoute<ShadowEvolutionRoute>()
            ShadowEvolutionScreen(
                shadowId = args.shadowId,
                viewModel = shadowRealmViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable<ShadowSkillTreeRoute> { backStackEntry ->
            val args = backStackEntry.toRoute<ShadowSkillTreeRoute>()
            ShadowSkillTreeScreen(
                shadowId = args.shadowId,
                viewModel = shadowRealmViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable<ShadowDeploymentRoute> {
            ShadowDeploymentScreen(
                viewModel = shadowRealmViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable<ArmyFormationManagerRoute> {
            ArmyFormationManagerScreen(
                viewModel = shadowRealmViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable<MonarchSkillTreeRoute> {
            MonarchSkillTreeScreen(
                viewModel = shadowRealmViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable<ShadowQuestBoardRoute> {
            ShadowQuestBoardScreen(
                viewModel = shadowRealmViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable<ShadowArmyAnalyticsRoute> {
            ShadowArmyAnalyticsScreen(
                viewModel = shadowRealmViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable<ShadowArchiveRoute> {
            ShadowArchiveScreen(
                viewModel = shadowRealmViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable<ShadowSettingsRoute> {
            ShadowSettingsScreen(
                viewModel = shadowRealmViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable<com.example.ui.navigation.SocialMasteryDashboardRoute> {
            val app = LocalContext.current.applicationContext as com.example.SoloLevelingApplication
            val socialViewModel = androidx.lifecycle.viewmodel.compose.viewModel {
                com.example.ui.viewmodel.SocialMasteryViewModel(
                    socialRepository = app.container.socialRepository,
                    analyticsEngine = app.container.socialAnalyticsEngine,
                    aiCoach = app.container.socialAiCoach,
                    voiceCoachController = app.container.socialVoiceCoachController,
                    accountOwnerIdFlow = app.container.activeAccountOwnerIdFlow
                )
            }
            com.example.ui.screens.social.SocialMasteryDashboardScreen(
                viewModel = socialViewModel,
                onNavigateBack = { navController.popBackStack() },
                onOpenPracticeArena = { navController.navigate(com.example.ui.navigation.SocialPracticeArenaRoute) },
                onOpenCoach = { navController.navigate(com.example.ui.navigation.SocialCoachRoute) },
                onOpenAnalytics = { navController.navigate(com.example.ui.navigation.SocialAnalyticsRoute) },
                onOpenMissions = { navController.navigate(com.example.ui.navigation.SocialMissionsRoute) },
                onOpenAssessment = { navController.navigate(com.example.ui.navigation.SocialSkillAssessmentRoute) }
            )
        }
        composable<com.example.ui.navigation.SocialPracticeArenaRoute> {
            val app = LocalContext.current.applicationContext as com.example.SoloLevelingApplication
            val socialViewModel = androidx.lifecycle.viewmodel.compose.viewModel {
                com.example.ui.viewmodel.SocialMasteryViewModel(
                    socialRepository = app.container.socialRepository,
                    analyticsEngine = app.container.socialAnalyticsEngine,
                    aiCoach = app.container.socialAiCoach,
                    voiceCoachController = app.container.socialVoiceCoachController,
                    accountOwnerIdFlow = app.container.activeAccountOwnerIdFlow
                )
            }
            com.example.ui.screens.social.SocialPracticeArenaScreen(
                viewModel = socialViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable<com.example.ui.navigation.SocialCoachRoute> {
            val app = LocalContext.current.applicationContext as com.example.SoloLevelingApplication
            val socialViewModel = androidx.lifecycle.viewmodel.compose.viewModel {
                com.example.ui.viewmodel.SocialMasteryViewModel(
                    socialRepository = app.container.socialRepository,
                    analyticsEngine = app.container.socialAnalyticsEngine,
                    aiCoach = app.container.socialAiCoach,
                    voiceCoachController = app.container.socialVoiceCoachController,
                    accountOwnerIdFlow = app.container.activeAccountOwnerIdFlow
                )
            }
            com.example.ui.screens.social.SocialCoachScreen(
                viewModel = socialViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable<com.example.ui.navigation.SocialAnalyticsRoute> {
            val app = LocalContext.current.applicationContext as com.example.SoloLevelingApplication
            val socialViewModel = androidx.lifecycle.viewmodel.compose.viewModel {
                com.example.ui.viewmodel.SocialMasteryViewModel(
                    socialRepository = app.container.socialRepository,
                    analyticsEngine = app.container.socialAnalyticsEngine,
                    aiCoach = app.container.socialAiCoach,
                    voiceCoachController = app.container.socialVoiceCoachController,
                    accountOwnerIdFlow = app.container.activeAccountOwnerIdFlow
                )
            }
            com.example.ui.screens.social.SocialAnalyticsScreen(
                viewModel = socialViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable<com.example.ui.navigation.SocialMissionsRoute> {
            val app = LocalContext.current.applicationContext as com.example.SoloLevelingApplication
            val socialViewModel = androidx.lifecycle.viewmodel.compose.viewModel {
                com.example.ui.viewmodel.SocialMasteryViewModel(
                    socialRepository = app.container.socialRepository,
                    analyticsEngine = app.container.socialAnalyticsEngine,
                    aiCoach = app.container.socialAiCoach,
                    voiceCoachController = app.container.socialVoiceCoachController,
                    accountOwnerIdFlow = app.container.activeAccountOwnerIdFlow
                )
            }
            com.example.ui.screens.social.SocialMissionsScreen(
                viewModel = socialViewModel,
                onNavigateBack = { navController.popBackStack() },
                onOpenMissionDetail = { mId -> navController.navigate(com.example.ui.navigation.SocialMissionDetailRoute(mId)) },
                onOpenSubmission = { mId -> navController.navigate(com.example.ui.navigation.SocialMissionSubmissionRoute(mId)) }
            )
        }
        composable<com.example.ui.navigation.SocialMissionDetailRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<com.example.ui.navigation.SocialMissionDetailRoute>()
            val app = LocalContext.current.applicationContext as com.example.SoloLevelingApplication
            val socialViewModel = androidx.lifecycle.viewmodel.compose.viewModel {
                com.example.ui.viewmodel.SocialMasteryViewModel(
                    socialRepository = app.container.socialRepository,
                    analyticsEngine = app.container.socialAnalyticsEngine,
                    aiCoach = app.container.socialAiCoach,
                    voiceCoachController = app.container.socialVoiceCoachController,
                    accountOwnerIdFlow = app.container.activeAccountOwnerIdFlow
                )
            }
            com.example.ui.screens.social.SocialMissionDetailScreen(
                missionId = route.missionId,
                viewModel = socialViewModel,
                onNavigateBack = { navController.popBackStack() },
                onOpenSubmission = { mId -> navController.navigate(com.example.ui.navigation.SocialMissionSubmissionRoute(mId)) }
            )
        }
        composable<com.example.ui.navigation.SocialMissionSubmissionRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<com.example.ui.navigation.SocialMissionSubmissionRoute>()
            val app = LocalContext.current.applicationContext as com.example.SoloLevelingApplication
            val socialViewModel = androidx.lifecycle.viewmodel.compose.viewModel {
                com.example.ui.viewmodel.SocialMasteryViewModel(
                    socialRepository = app.container.socialRepository,
                    analyticsEngine = app.container.socialAnalyticsEngine,
                    aiCoach = app.container.socialAiCoach,
                    voiceCoachController = app.container.socialVoiceCoachController,
                    accountOwnerIdFlow = app.container.activeAccountOwnerIdFlow
                )
            }
            com.example.ui.screens.social.SocialMissionSubmissionScreen(
                missionId = route.missionId,
                viewModel = socialViewModel,
                onNavigateBack = { navController.popBackStack() },
                onSubmissionSuccess = { navController.popBackStack() }
            )
        }
        composable<com.example.ui.navigation.SocialSkillAssessmentRoute> {
            val app = LocalContext.current.applicationContext as com.example.SoloLevelingApplication
            val socialViewModel = androidx.lifecycle.viewmodel.compose.viewModel {
                com.example.ui.viewmodel.SocialMasteryViewModel(
                    socialRepository = app.container.socialRepository,
                    analyticsEngine = app.container.socialAnalyticsEngine,
                    aiCoach = app.container.socialAiCoach,
                    voiceCoachController = app.container.socialVoiceCoachController,
                    accountOwnerIdFlow = app.container.activeAccountOwnerIdFlow
                )
            }
            com.example.ui.screens.social.SocialSkillAssessmentScreen(
                viewModel = socialViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

private fun handleDeepLinkRoute(
    route: String,
    navController: NavHostController,
    experienceViewModel: SystemExperienceViewModel
) {
    when (route) {
        "home" -> navigateTopLevel(navController, HomeRoute)
        "status" -> navigateTopLevel(navController, StatusRoute)
        "physical" -> navigateTopLevel(navController, PhysicalRoute)
        "quest_board" -> navigateTopLevel(navController, QuestBoardRoute)
        "academic_revisions" -> navController.navigate(AcademicRevisionsRoute)
        "academic_exams" -> navController.navigate(AcademicExamsRoute)
        "academic_focus" -> navController.navigate(AcademicFocusTimerRoute())
        "ai_system" -> navController.navigate(AiSystemRoute)
        "ai_companion" -> navController.navigate(AiCompanionRoute)
        "voice_commands" -> navController.navigate(VoiceCommandRoute)
        "system_voice" -> navController.navigate(SystemVoiceSettingsRoute)
        "widgets" -> navController.navigate(WidgetHubRoute)
        "celebrations" -> navController.navigate(CelebrationHistoryRoute)
        "life_dashboard" -> navController.navigate(LifeDashboardRoute)
        "routines" -> navController.navigate(RoutineManagerRoute)
        "habits" -> navController.navigate(HabitDashboardRoute)
        "tasks" -> navController.navigate(PersonalTaskManagerRoute)
        "goals" -> navController.navigate(GoalManagementRoute)
        "daily_schedule" -> navController.navigate(DailyScheduleRoute)
        "weekly_review" -> navController.navigate(WeeklyReviewRoute)
        "dungeon_gate" -> navController.navigate(DungeonGateRoute)
        "boss_battles" -> navController.navigate(BossBattleDashboardRoute)
        "cloud_sync" -> navController.navigate(CloudSyncDashboardRoute)
        "account_gateway" -> navController.navigate(AccountGatewayRoute)
        "intelligence_dashboard" -> navController.navigate(IntelligenceDashboardRoute)
        "intelligence_daily_plan" -> navController.navigate(AdaptiveDailyPlanRoute)
        "intelligence_roadmap" -> navController.navigate(DevelopmentRoadmapRoute)
        "intelligence_dungeons" -> navController.navigate(AdaptiveDungeonsRoute)
        "weekly_intelligence" -> navController.navigate(WeeklyIntelligenceReportRoute)
        "system_control" -> navController.navigate(SystemOverviewRoute)
        "automation_dashboard" -> navController.navigate(AutomationDashboardRoute)
        "shadow_realm" -> navController.navigate(ShadowRealmDashboardRoute)
        "shadow_extraction" -> navController.navigate(ShadowExtractionRoute)
        "shadow_roster" -> navController.navigate(ShadowCollectionListRoute)
        "social_mastery" -> navController.navigate(com.example.ui.navigation.SocialMasteryDashboardRoute)
        "social_missions" -> navController.navigate(com.example.ui.navigation.SocialMissionsRoute)
        "social_assessment" -> navController.navigate(com.example.ui.navigation.SocialSkillAssessmentRoute)
        "awakening" -> experienceViewModel.replayAwakening()
        else -> Unit
    }
}

private fun isRouteSelected(
    destination: androidx.navigation.NavDestination?,
    route: Any
): Boolean {
    return when (route) {
        is HomeRoute -> destination?.hasRoute<HomeRoute>() == true
        is StatusRoute -> destination?.hasRoute<StatusRoute>() == true
        is PhysicalRoute -> destination?.hasRoute<PhysicalRoute>() == true
        is QuestBoardRoute -> destination?.hasRoute<QuestBoardRoute>() == true
        is ModulesHubRoute -> destination?.hasRoute<ModulesHubRoute>() == true
        else -> false
    }
}

private fun navigateTopLevel(navController: NavHostController, route: Any) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
