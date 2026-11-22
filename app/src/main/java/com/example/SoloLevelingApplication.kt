package com.example

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.example.data.automation.AutomationOrchestrator
import com.example.data.healthconnect.AndroidHealthConnectDataSource
import com.example.data.healthconnect.HealthConnectDataSource
import com.example.data.integration.AndroidHealthConnectAdapter
import com.example.data.integration.BackendAiGateway
import com.example.data.integration.DeterministicSystemAdvisor
import com.example.data.integration.FirebaseGeminiAiGateway
import com.example.data.integration.HttpBackendAiGateway
import com.example.data.integration.HybridGeminiAiGateway
import com.example.data.integration.SupabaseSyncAdapter
import com.example.data.local.SoloLevelingDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.remote.HttpSupabaseClient
import com.example.data.remote.SecureTokenStorage
import com.example.data.remote.SupabaseApiPort
import com.example.data.remote.TokenStorage
import com.example.data.repository.AcademicRepository
import com.example.data.repository.AiSystemRepository
import com.example.data.repository.IntelligenceRepository
import com.example.data.repository.LifeManagementRepository
import com.example.data.repository.PenaltyRepository
import com.example.data.repository.QuestCommitmentRepository
import com.example.data.repository.RedemptionRepository
import com.example.data.repository.RealityOsRepository
import com.example.data.repository.RpgRepository
import com.example.data.repository.SyncRepository
import com.example.data.repository.SystemControlRepository
import com.example.data.repository.SystemEventRepository
import com.example.data.repository.SystemExperienceRepository
import com.example.data.repository.ShadowArmyRepository
import com.example.data.repository.SocialRepository
import com.example.data.repository.SocialRepositoryImpl
import com.example.worker.QuestDeadlineWorker
import com.example.domain.engine.CommunicationPracticeEngine
import com.example.domain.engine.SocialMasteryAnalyticsEngine
import com.example.service.AudioLifecycleManager
import com.example.service.SocialArtifactStorageManager
import com.example.service.SocialMasteryAiCoach
import com.example.service.SocialPrivacyService
import com.example.service.SocialVoiceCoachController
import com.example.domain.engine.GeminiIntentEngine
import com.example.domain.engine.MonarchProgressionEngine
import com.example.domain.engine.ShadowDeploymentEngine
import com.example.domain.engine.ShadowEvolutionEngine
import com.example.domain.engine.ShadowExtractionEngine
import com.example.domain.engine.SystemCommandExecutor
import com.example.data.sync.CloudSyncEngine
import com.example.data.sync.SyncQueueManager
import com.example.data.sync.SyncStatusTracker
import com.example.domain.model.AuthStatus
import com.example.domain.model.SystemEventType
import com.example.service.AndroidSystemVoiceEngine
import com.example.worker.CloudSyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

class AppContainer(application: Application) {
    companion object {
        const val DEFAULT_LOCAL_ACCOUNT_OWNER_ID = "local_vault_player_1"
    }

    private val database by lazy { SoloLevelingDatabase.getInstance(application) }

    val activeAccountOwnerIdFlow: Flow<String> by lazy {
        database.cloudAuthDao().observeSession()
            .map { sess ->
                if (sess != null &&
                    sess.supabaseUserId.isNotBlank() &&
                    sess.authStatusName != AuthStatus.SIGNED_OUT.name
                ) {
                    sess.supabaseUserId
                } else {
                    DEFAULT_LOCAL_ACCOUNT_OWNER_ID
                }
            }
            .distinctUntilChanged()
    }
    val preferencesRepository by lazy { UserPreferencesRepository(application) }
    private val healthConnectPort by lazy { AndroidHealthConnectAdapter(application) }
    val healthConnectDataSource: HealthConnectDataSource by lazy { AndroidHealthConnectDataSource(application) }
    val tokenStorage: TokenStorage by lazy { SecureTokenStorage(application) }
    val supabaseApiPort: SupabaseApiPort by lazy { HttpSupabaseClient() }
    val syncQueueManager: SyncQueueManager by lazy { SyncQueueManager(database) }
    val syncStatusTracker: SyncStatusTracker by lazy {
        SyncStatusTracker(database) { supabaseApiPort.isConfigured() }
    }
    val systemEventRepository: SystemEventRepository by lazy {
        SystemEventRepository(database = database)
    }
    val cloudSyncEngine: CloudSyncEngine by lazy {
        CloudSyncEngine(
            database = database,
            supabaseApi = supabaseApiPort,
            tokenStorage = tokenStorage,
            queueManager = syncQueueManager,
            statusTracker = syncStatusTracker
        )
    }
    val syncRepository: SyncRepository by lazy {
        SyncRepository(
            database = database,
            preferencesRepository = preferencesRepository,
            supabaseApi = supabaseApiPort,
            tokenStorage = tokenStorage,
            queueManager = syncQueueManager,
            statusTracker = syncStatusTracker,
            syncEngine = cloudSyncEngine,
            systemEventRepository = systemEventRepository
        )
    }
    private val cloudSyncPort by lazy { SupabaseSyncAdapter { supabaseApiPort.isConfigured() } }
    val aiGameMasterPort by lazy { DeterministicSystemAdvisor() }
    val firebaseGeminiAiGateway: FirebaseGeminiAiGateway by lazy {
        FirebaseGeminiAiGateway(
            isNetworkAvailable = {
                runCatching {
                    val cm = application.getSystemService(Application.CONNECTIVITY_SERVICE) as? ConnectivityManager
                    val caps = cm?.activeNetwork?.let { cm.getNetworkCapabilities(it) }
                    caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
                }.getOrDefault(true)
            }
        )
    }
    val backendAiGateway: BackendAiGateway by lazy {
        HybridGeminiAiGateway(
            firebaseGateway = firebaseGeminiAiGateway,
            httpGateway = HttpBackendAiGateway()
        )
    }

    val repository: RealityOsRepository by lazy {
        RealityOsRepository(
            database = database,
            preferencesRepository = preferencesRepository,
            healthConnectPort = healthConnectPort,
            healthConnectDataSource = healthConnectDataSource,
            cloudSyncPort = cloudSyncPort,
            aiGameMasterPort = aiGameMasterPort,
            systemEventRepository = systemEventRepository
        )
    }

    val academicRepository: AcademicRepository by lazy {
        AcademicRepository(
            database = database,
            preferencesRepository = preferencesRepository,
            systemEventRepository = systemEventRepository
        )
    }

    val lifeManagementRepository: LifeManagementRepository by lazy {
        LifeManagementRepository(
            database = database,
            preferencesRepository = preferencesRepository,
            systemEventRepository = systemEventRepository
        )
    }

    val rpgRepository: RpgRepository by lazy {
        RpgRepository(
            database = database,
            preferencesRepository = preferencesRepository,
            systemEventRepository = systemEventRepository
        )
    }

    val aiSystemRepository: AiSystemRepository by lazy {
        AiSystemRepository(
            database = database,
            preferencesRepository = preferencesRepository,
            realityOsRepository = repository,
            academicRepository = academicRepository,
            backendAiGateway = backendAiGateway,
            activeAccountOwnerIdProvider = {
                activeAccountOwnerIdFlow.firstOrNull() ?: DEFAULT_LOCAL_ACCOUNT_OWNER_ID
            }
        )
    }

    val intelligenceRepository: IntelligenceRepository by lazy {
        IntelligenceRepository(
            database = database,
            preferencesRepository = preferencesRepository,
            backendAiGateway = backendAiGateway
        )
    }

    val questCommitmentRepository: QuestCommitmentRepository by lazy {
        QuestCommitmentRepository(
            database = database,
            systemEventRepository = systemEventRepository
        )
    }

    val penaltyRepository: PenaltyRepository by lazy {
        PenaltyRepository(
            database = database,
            systemEventRepository = systemEventRepository
        )
    }

    val redemptionRepository: RedemptionRepository by lazy {
        RedemptionRepository(
            database = database,
            systemEventRepository = systemEventRepository
        )
    }

    val automationOrchestrator: AutomationOrchestrator by lazy {
        AutomationOrchestrator(
            context = application,
            database = database,
            preferencesRepository = preferencesRepository,
            systemEventRepository = systemEventRepository,
            intelligenceRepository = intelligenceRepository,
            syncRepository = syncRepository,
            questCommitmentRepository = questCommitmentRepository
        )
    }

    val systemControlRepository: SystemControlRepository by lazy {
        SystemControlRepository(
            database = database,
            preferencesRepository = preferencesRepository,
            systemEventRepository = systemEventRepository,
            automationOrchestrator = automationOrchestrator,
            syncStatusTracker = syncStatusTracker
        )
    }

    val voiceEngine: AndroidSystemVoiceEngine by lazy {
        AndroidSystemVoiceEngine(application)
    }

    val geminiIntentEngine: GeminiIntentEngine by lazy {
        GeminiIntentEngine(backendAiGateway = backendAiGateway)
    }

    val systemCommandExecutor: SystemCommandExecutor by lazy {
        SystemCommandExecutor(
            database = database,
            preferencesRepository = preferencesRepository,
            realityOsRepository = repository,
            lifeManagementRepository = lifeManagementRepository,
            academicRepository = academicRepository,
            aiSystemRepository = aiSystemRepository,
            shadowArmyRepository = shadowArmyRepository,
            activeAccountOwnerIdProvider = {
                activeAccountOwnerIdFlow.firstOrNull() ?: DEFAULT_LOCAL_ACCOUNT_OWNER_ID
            }
        )
    }

    val systemExperienceRepository: SystemExperienceRepository by lazy {
        SystemExperienceRepository(
            context = application,
            database = database,
            preferencesRepository = preferencesRepository,
            voiceEngine = voiceEngine,
            geminiIntentEngine = geminiIntentEngine,
            systemCommandExecutor = systemCommandExecutor,
            aiSystemRepository = aiSystemRepository
        )
    }

    val shadowArmyRepository: ShadowArmyRepository by lazy {
        val shadowMutex = Mutex()
        val monarchProgressionEngine = MonarchProgressionEngine(
            monarchProfileDao = database.shadowMonarchProfileDao(),
            monarchSkillUnlockDao = database.monarchSkillUnlockDao(),
            shadowDao = database.shadowDao(),
            shadowResourceDao = database.shadowResourceDao(),
            shadowQuestProgressDao = database.shadowQuestProgressDao(),
            shadowXpTraceDao = database.shadowXpTraceDao(),
            database = database,
            mutex = shadowMutex
        )
        val bondEngine = com.example.domain.engine.ShadowBondEngine(
            shadowBondDao = database.shadowBondDao(),
            shadowDao = database.shadowDao(),
            database = database,
            mutex = shadowMutex
        )
        val extractionEngine = ShadowExtractionEngine(
            database = database,
            shadowDao = database.shadowDao(),
            extractionDao = database.shadowExtractionDao(),
            monarchProfileDao = database.shadowMonarchProfileDao(),
            monarchProgressionEngine = monarchProgressionEngine,
            mutex = shadowMutex
        )
        val evolutionEngine = ShadowEvolutionEngine(
            shadowDao = database.shadowDao(),
            shadowSkillUnlockDao = database.shadowSkillUnlockDao(),
            monarchProgressionEngine = monarchProgressionEngine,
            shadowXpTraceDao = database.shadowXpTraceDao(),
            shadowBondEngine = bondEngine,
            database = database,
            mutex = shadowMutex
        )
        val deploymentEngine = ShadowDeploymentEngine(
            shadowDao = database.shadowDao(),
            deploymentDao = database.shadowDeploymentDao(),
            formationDao = database.shadowFormationDao(),
            monarchProgressionEngine = monarchProgressionEngine,
            shadowBondEngine = bondEngine,
            database = database,
            mutex = shadowMutex
        )
        ShadowArmyRepository(
            monarchProfileDao = database.shadowMonarchProfileDao(),
            shadowDao = database.shadowDao(),
            extractionDao = database.shadowExtractionDao(),
            skillUnlockDao = database.shadowSkillUnlockDao(),
            deploymentDao = database.shadowDeploymentDao(),
            formationDao = database.shadowFormationDao(),
            monarchSkillUnlockDao = database.monarchSkillUnlockDao(),
            questProgressDao = database.shadowQuestProgressDao(),
            resourceDao = database.shadowResourceDao(),
            shadowBondDao = database.shadowBondDao(),
            shadowTacticalDao = database.shadowTacticalDao(),
            shadowDominionDao = database.shadowDominionDao(),
            monarchProgressionEngine = monarchProgressionEngine,
            extractionEngine = extractionEngine,
            evolutionEngine = evolutionEngine,
            deploymentEngine = deploymentEngine,
            shadowBondEngine = bondEngine,
            database = database,
            mutex = shadowMutex
        )
    }

    val audioLifecycleManager: AudioLifecycleManager by lazy {
        AudioLifecycleManager(application, database)
    }

    val socialArtifactStorageManager: SocialArtifactStorageManager by lazy {
        SocialArtifactStorageManager(application)
    }

    val socialPrivacyService: SocialPrivacyService by lazy {
        SocialPrivacyService(preferencesRepository)
    }

    val socialRepository: SocialRepository by lazy {
        SocialRepositoryImpl(
            database = database,
            audioLifecycleManager = audioLifecycleManager,
            artifactStorageManager = socialArtifactStorageManager
        )
    }

    val socialAiCoach: SocialMasteryAiCoach by lazy {
        SocialMasteryAiCoach(
            backendAiGateway = backendAiGateway,
            socialPrivacyService = socialPrivacyService
        )
    }

    val socialVoiceCoachController: SocialVoiceCoachController by lazy {
        SocialVoiceCoachController(
            context = application,
            aiCoach = socialAiCoach,
            voiceEngine = voiceEngine,
            coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        )
    }

    val socialAnalyticsEngine: SocialMasteryAnalyticsEngine by lazy {
        SocialMasteryAnalyticsEngine(socialRepository)
    }
}

class SoloLevelingApplication : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        registerCrossModuleEventHandlers()
        appScope.launch {
            container.repository.ensureInitialized()
            container.rpgRepository.ensureRpgSeededAndSynced()
            container.systemExperienceRepository.ensureInitialized()
            container.systemEventRepository.recoverInterruptedEvents()
            container.automationOrchestrator.ensureRegistrySeeded()
            container.automationOrchestrator.runDailyCycleIdempotent("APP_STARTUP")
            container.systemControlRepository.evaluateSystemHealthAndPerformance()
            container.automationOrchestrator.scheduleAllEnabledWorkers()
            container.systemExperienceRepository.syncCelebrationsFromPlayerProfile()
            val activeOwnerId = container.activeAccountOwnerIdFlow.firstOrNull()
                ?: AppContainer.DEFAULT_LOCAL_ACCOUNT_OWNER_ID
            container.shadowArmyRepository.getMonarchProfile(activeOwnerId)
            container.socialRepository.seedInitialScenariosIfEmpty()
            container.socialRepository.getOrCreateProfile(activeOwnerId)
            container.socialRepository.initializeMissingModuleProgress(activeOwnerId)
            container.audioLifecycleManager.reconcileOrphanFiles(activeOwnerId)
        }
        CloudSyncWorker.enqueueImmediateSync(this, "APP_STARTUP")
        QuestDeadlineWorker.schedule(this)
        observeNetworkRestoration()
    }

    private fun registerCrossModuleEventHandlers() {
        val busRepo = container.systemEventRepository
        val refreshTypes = listOf(
            SystemEventType.WORKOUT_COMPLETED,
            SystemEventType.JOGGING_COMPLETED,
            SystemEventType.PHYSICAL_ACTIVITY_RECORDED,
            SystemEventType.ACADEMIC_SESSION_COMPLETED,
            SystemEventType.REVISION_COMPLETED,
            SystemEventType.ASSESSMENT_RECORDED,
            SystemEventType.HABIT_COMPLETED,
            SystemEventType.ROUTINE_COMPLETED,
            SystemEventType.TASK_COMPLETED,
            SystemEventType.GOAL_MILESTONE_COMPLETED,
            SystemEventType.DUNGEON_COMPLETED,
            SystemEventType.BOSS_BATTLE_COMPLETED
        )
        refreshTypes.forEach { type ->
            busRepo.registerHandler(type) {
                // Coalesced intelligence refresh (fingerprint short-circuits when unchanged)
                container.intelligenceRepository.refreshIntelligenceIfNeeded(force = false)
                Result.success(Unit)
            }
        }
        busRepo.registerHandler(SystemEventType.DAILY_RESET_COMPLETED) {
            container.systemControlRepository.evaluateSystemHealthAndPerformance()
            Result.success(Unit)
        }
        busRepo.registerHandler(SystemEventType.DUNGEON_COMPLETED) {
            val activeOwnerId = container.activeAccountOwnerIdFlow.firstOrNull()
                ?: AppContainer.DEFAULT_LOCAL_ACCOUNT_OWNER_ID
            container.shadowArmyRepository.syncShadowQuestProgress(
                dungeonCompletions = 1,
                accountOwnerId = activeOwnerId
            )
            Result.success(Unit)
        }
        busRepo.registerHandler(SystemEventType.BOSS_BATTLE_COMPLETED) {
            val activeOwnerId = container.activeAccountOwnerIdFlow.firstOrNull()
                ?: AppContainer.DEFAULT_LOCAL_ACCOUNT_OWNER_ID
            container.shadowArmyRepository.syncShadowQuestProgress(
                dungeonCompletions = 1,
                accountOwnerId = activeOwnerId
            )
            Result.success(Unit)
        }
        SystemEventType.entries.forEach { eventType ->
            busRepo.registerHandler(eventType) { event ->
                container.systemExperienceRepository.onSystemEventReceived(event)
                Result.success(Unit)
            }
        }
    }

    private fun observeNetworkRestoration() {
        runCatching {
            val cm = getSystemService(CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
            val caps = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) }
            val initialOnline = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            container.syncStatusTracker.setOnline(initialOnline)

            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    container.syncStatusTracker.setOnline(true)
                    CloudSyncWorker.enqueueImmediateSync(this@SoloLevelingApplication, "NETWORK_RESTORED")
                }

                override fun onLost(network: Network) {
                    container.syncStatusTracker.setOnline(false)
                }
            })
        }
    }
}
