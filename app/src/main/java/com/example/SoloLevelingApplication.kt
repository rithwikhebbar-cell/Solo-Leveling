package com.example

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
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
import com.example.data.repository.LifeManagementRepository
import com.example.data.repository.RealityOsRepository
import com.example.data.repository.RpgRepository
import com.example.data.repository.SyncRepository
import com.example.data.sync.CloudSyncEngine
import com.example.data.sync.SyncQueueManager
import com.example.data.sync.SyncStatusTracker
import com.example.worker.CloudSyncWorker
import com.example.worker.DailySystemResetWorker
import com.example.worker.SystemNotificationWorker
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
            syncEngine = cloudSyncEngine
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
            aiGameMasterPort = aiGameMasterPort
        )
    }

    val academicRepository: AcademicRepository by lazy {
        AcademicRepository(
            database = database,
            preferencesRepository = preferencesRepository
        )
    }

    val lifeManagementRepository: LifeManagementRepository by lazy {
        LifeManagementRepository(
            database = database,
            preferencesRepository = preferencesRepository
        )
    }

    val rpgRepository: RpgRepository by lazy {
        RpgRepository(
            database = database,
            preferencesRepository = preferencesRepository
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
}

class SoloLevelingApplication : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        appScope.launch {
            container.repository.ensureInitialized()
            container.rpgRepository.ensureRpgSeededAndSynced()
        }
        DailySystemResetWorker.schedule(this)
        SystemNotificationWorker.schedule(this)
        CloudSyncWorker.schedulePeriodic(this)
        CloudSyncWorker.enqueueImmediateSync(this, "APP_STARTUP")
        observeNetworkRestoration()
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
