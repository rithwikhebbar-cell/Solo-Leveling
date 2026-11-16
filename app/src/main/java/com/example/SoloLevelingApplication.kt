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
import com.example.data.integration.HttpBackendAiGateway
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
import com.example.data.repository.RealityOsRepository
import com.example.data.repository.RpgRepository
import com.example.data.repository.SyncRepository
import com.example.data.repository.SystemControlRepository
import com.example.data.repository.SystemEventRepository
import com.example.data.repository.SystemExperienceRepository
import com.example.data.repository.ShadowArmyRepository
import com.example.data.repository.SocialRepository
import com.example.data.repository.SocialRepositoryImpl
import com.example.domain.engine.CommunicationPracticeEngine
import com.example.domain.engine.SocialMasteryAnalyticsEngine
import com.example.service.AudioLifecycleManager
import com.example.service.SocialMasteryAiCoach
import com.example.service.SocialPrivacyService
import com.example.service.SocialVoiceCoachController
import com.example.domain.engine.MonarchProgressionEngine
import com.example.domain.engine.ShadowDeploymentEngine
import com.example.domain.engine.ShadowEvolutionEngine
import com.example.domain.engine.ShadowExtractionEngine
import com.example.data.sync.CloudSyncEngine
import com.example.data.sync.SyncQueueManager
import com.example.data.sync.SyncStatusTracker
import com.example.domain.model.SystemEventType
import com.example.service.AndroidSystemVoiceEngine
import com.example.worker.CloudSyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(application: Application) {
    private val database by lazy { SoloLevelingDatabase.getInstance(application) }
    private val preferencesRepository by lazy { UserPreferencesRepository(application) }
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
    val backendAiGateway: BackendAiGateway by lazy { HttpBackendAiGateway() }

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
            backendAiGateway = backendAiGateway
        )
    }

    val intelligenceRepository: IntelligenceRepository by lazy {
        IntelligenceRepository(
            database = database,
            preferencesRepository = preferencesRepository,
            backendAiGateway = backendAiGateway
        )
    }

    val automationOrchestrator: AutomationOrchestrator by lazy {
        AutomationOrchestrator(
            context = application,
            database = database,
            preferencesRepository = preferencesRepository,
            systemEventRepository = systemEventRepository,
            intelligenceRepository = intelligenceRepository,
            syncRepository = syncRepository
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

    val systemExperienceRepository: SystemExperienceRepository by lazy {
        SystemExperienceRepository(
            context = application,
            database = database,
            preferencesRepository = preferencesRepository,
            voiceEngine = voiceEngine
        )
    }

    val shadowArmyRepository: ShadowArmyRepository by lazy {
        val monarchProgressionEngine = MonarchProgressionEngine(
            monarchProfileDao = database.shadowMonarchProfileDao(),
            monarchSkillUnlockDao = database.monarchSkillUnlockDao(),
            shadowDao = database.shadowDao(),
            shadowResourceDao = database.shadowResourceDao(),
            shadowQuestProgressDao = database.shadowQuestProgressDao()
        )
        val extractionEngine = ShadowExtractionEngine(
            database = database,
            shadowDao = database.shadowDao(),
            extractionDao = database.shadowExtractionDao(),
            monarchProfileDao = database.shadowMonarchProfileDao(),
            monarchProgressionEngine = monarchProgressionEngine
        )
        val evolutionEngine = ShadowEvolutionEngine(
            shadowDao = database.shadowDao(),
            shadowSkillUnlockDao = database.shadowSkillUnlockDao(),
            monarchProgressionEngine = monarchProgressionEngine
        )
        val deploymentEngine = ShadowDeploymentEngine(
            shadowDao = database.shadowDao(),
            deploymentDao = database.shadowDeploymentDao(),
            formationDao = database.shadowFormationDao()
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
            monarchProgressionEngine = monarchProgressionEngine,
            extractionEngine = extractionEngine,
            evolutionEngine = evolutionEngine,
            deploymentEngine = deploymentEngine
        )
    }

    val audioLifecycleManager: AudioLifecycleManager by lazy {
        AudioLifecycleManager(application, database)
    }

    val socialPrivacyService: SocialPrivacyService by lazy {
        SocialPrivacyService(preferencesRepository)
    }

    val socialRepository: SocialRepository by lazy {
        SocialRepositoryImpl(
            database = database,
            audioLifecycleManager = audioLifecycleManager
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
            container.socialRepository.seedInitialScenariosIfEmpty()
            container.socialRepository.initializeMissingModuleProgress("local_vault_player_1")
            container.audioLifecycleManager.reconcileOrphanFiles("local_vault_player_1")
        }
        CloudSyncWorker.enqueueImmediateSync(this, "APP_STARTUP")
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
        busRepo.registerHandler(SystemEventType.DUNGEON_COMPLETED) { event ->
            container.shadowArmyRepository.monarchProgressionEngine.checkAndUpdateQuestProgress(
                event.sourceEventId.ifBlank { event.eventId }
            )
            Result.success(Unit)
        }
        busRepo.registerHandler(SystemEventType.BOSS_BATTLE_COMPLETED) { event ->
            container.shadowArmyRepository.monarchProgressionEngine.checkAndUpdateQuestProgress(
                event.sourceEventId.ifBlank { event.eventId }
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
