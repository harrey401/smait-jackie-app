package com.gow.eng192lab.navigation

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.gow.eng192lab.CaeAudioManager
import com.gow.eng192lab.ChassisProxy
import com.gow.eng192lab.data.model.ThemeConfig
import com.gow.eng192lab.jackieApp
import com.gow.eng192lab.ui.conversation.ConversationScreen
import com.gow.eng192lab.ui.conversation.ConversationViewModel
import com.gow.eng192lab.ui.conversation.VideoStreamManager
import com.gow.eng192lab.ui.home.HomeScreen
import com.gow.eng192lab.ui.home.HomeViewModel
import com.gow.eng192lab.ui.navigation_map.NavigationMapScreen
import com.gow.eng192lab.ui.navigation_map.NavigationMapViewModel
import com.gow.eng192lab.ui.settings.SettingsScreen
import com.gow.eng192lab.ui.photobooth.PhotoBoothScreen
import com.gow.eng192lab.follow.FollowController
import com.gow.eng192lab.ui.follow.FollowScreen
import com.gow.eng192lab.ui.labtour.LabTourScreen
import com.gow.eng192lab.ui.labtour.LabTourViewModel
import org.json.JSONObject

private const val TAG = "AppNavigation"

@Composable
fun AppScaffold(
    navController: NavHostController,
    themeConfig: ThemeConfig
) {
    val context = LocalContext.current
    val wsRepo = context.jackieApp.webSocketRepository
    val themeRepo = context.jackieApp.themeRepository

    val homeViewModel: HomeViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                HomeViewModel(themeRepo) as T
        }
    )
    val navMapViewModel: NavigationMapViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                NavigationMapViewModel(wsRepo) as T
        }
    )
    val ttsPlayer = remember { context.jackieApp.ttsAudioPlayer }
    val caeAudioManager = remember { CaeAudioManager(context) }
    val videoStreamManager = remember { VideoStreamManager(wsRepo) }
    val conversationViewModel = remember {
        ConversationViewModel(
            wsRepo = wsRepo,
            ttsPlayer = ttsPlayer,
            caeAudioManager = caeAudioManager,
            videoStreamManager = videoStreamManager
        )
    }
    val followController = remember {
        FollowController(
            chassisSender = { json ->
                context.jackieApp.chassisProxy?.forwardToChassisRaw(json)
                    ?: Log.w(TAG, "FollowController: no ChassisProxy — cmd_vel dropped")
            }
        )
    }
    val labTourViewModel = remember {
        LabTourViewModel(wsRepo = wsRepo, ttsPlayer = ttsPlayer)
    }

    // Auto-connect using saved IP from Settings (if available)
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("eng192lab_settings", Context.MODE_PRIVATE)
        val savedIp = prefs.getString("server_ip", null)
        val savedPort = prefs.getString("server_port", "8765")
        if (savedIp != null) {
            val url = "ws://$savedIp:$savedPort"
            Log.i(TAG, "Auto-connecting WebSocket to $url")
            wsRepo.connect(url)
        } else {
            Log.i(TAG, "No saved server IP — connect via Settings")
        }
    }

    // Start audio + video capture once WebSocket connects
    val isConnected by wsRepo.isConnected.collectAsStateWithLifecycle()
    LaunchedEffect(isConnected) {
        if (isConnected) {
            // Tell server this is the ENG192 lab app — server injects lab knowledge into LLM
            wsRepo.send(JSONObject().apply {
                put("type", "app_mode")
                put("mode", "eng192_lab")
            }.toString())

            caeAudioManager.copyAssetsIfNeeded()
            val ws = wsRepo.currentWebSocket
            if (ws != null) {
                caeAudioManager.start(ws)
                Log.i(TAG, "Started CaeAudioManager for Jackie")
            } else {
                Log.w(TAG, "WebSocket connected but currentWebSocket is null")
            }

            // Start chassis proxy — bridges server ↔ chassis (192.168.20.22:9090)
            val proxy = ChassisProxy(
                chassisUrl = "ws://192.168.20.22:9090",
                serverSender = { json: String -> wsRepo.send(json) }
            )
            proxy.connect()
            context.jackieApp.chassisProxy = proxy
            wsRepo.chassisProxy = proxy
            Log.i(TAG, "Started ChassisProxy")
            videoStreamManager.start(context)
            Log.i(TAG, "Started VideoStreamManager")
        }
    }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            caeAudioManager.stop()
            videoStreamManager.stop()
            wsRepo.disconnect()
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Home,
        modifier = Modifier.fillMaxSize()
    ) {
        composable<Screen.Home> {
            HomeScreen(viewModel = homeViewModel, navController = navController)
        }
        composable<Screen.Chat> {
            ConversationScreen(viewModel = conversationViewModel, navController = navController)
        }
        composable<Screen.Map> {
            NavigationMapScreen(viewModel = navMapViewModel, navController = navController)
        }
        composable<Screen.PhotoBooth> {
            PhotoBoothScreen(
                navController = navController,
                wsRepo = wsRepo,
                videoStreamManager = videoStreamManager,
            )
        }
        composable<Screen.Settings> {
            SettingsScreen(navController = navController)
        }
        composable<Screen.Follow> {
            FollowScreen(wsRepo = wsRepo, navController = navController)
        }
        composable<Screen.LabTour> {
            LabTourScreen(viewModel = labTourViewModel, navController = navController)
        }
    }
}
