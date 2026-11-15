package com.example

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.SoloLevelingApp
import com.example.ui.viewmodel.AcademicViewModel
import com.example.ui.viewmodel.AiSystemViewModel
import com.example.ui.viewmodel.CloudSyncViewModel
import com.example.ui.viewmodel.LifeManagementViewModel
import com.example.ui.viewmodel.RealityOsViewModel
import com.example.ui.viewmodel.RpgViewModel
import com.example.worker.SystemNotificationWorker

class MainActivity : ComponentActivity() {
    private var deepLinkRouteState by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deepLinkRouteState = intent?.getStringExtra(SystemNotificationWorker.EXTRA_DEEP_LINK_ROUTE)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        val app = application as SoloLevelingApplication
        setContent {
            val viewModel: RealityOsViewModel = viewModel(
                factory = RealityOsViewModel.factory(app.container.repository)
            )
            val academicViewModel: AcademicViewModel = viewModel(
                factory = AcademicViewModel.factory(
                    academicRepository = app.container.academicRepository,
                    realityOsRepository = app.container.repository,
                    aiGameMasterPort = app.container.aiGameMasterPort
                )
            )
            val aiSystemViewModel: AiSystemViewModel = viewModel(
                factory = AiSystemViewModel.factory(
                    aiSystemRepository = app.container.aiSystemRepository,
                    realityOsRepository = app.container.repository
                )
            )
            val lifeManagementViewModel: LifeManagementViewModel = viewModel(
                factory = LifeManagementViewModel.factory(
                    lifeRepository = app.container.lifeManagementRepository
                )
            )
            val rpgViewModel: RpgViewModel = viewModel(
                factory = RpgViewModel.factory(
                    rpgRepository = app.container.rpgRepository,
                    aiSystemRepository = app.container.aiSystemRepository
                )
            )
            val cloudSyncViewModel: CloudSyncViewModel = viewModel(
                factory = CloudSyncViewModel.factory(
                    syncRepository = app.container.syncRepository
                )
            )
            SoloLevelingApp(
                viewModel = viewModel,
                academicViewModel = academicViewModel,
                aiSystemViewModel = aiSystemViewModel,
                lifeManagementViewModel = lifeManagementViewModel,
                rpgViewModel = rpgViewModel,
                cloudSyncViewModel = cloudSyncViewModel,
                deepLinkRoute = deepLinkRouteState,
                onDeepLinkConsumed = { deepLinkRouteState = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val route = intent.getStringExtra(SystemNotificationWorker.EXTRA_DEEP_LINK_ROUTE)
        if (!route.isNullOrBlank()) {
            deepLinkRouteState = route
        }
    }
}
