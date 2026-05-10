package com.smait.jackie.navigation

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
import com.smait.jackie.CaeAudioManager
import com.smait.jackie.ChassisProxy
import com.smait.jackie.data.model.ThemeConfig
import com.smait.jackie.jackieApp
import com.smait.jackie.ui.conversation.ConversationScreen
import com.smait.jackie.ui.conversation.ConversationViewModel
import com.smait.jackie.ui.conversation.VideoStreamManager
import com.smait.jackie.ui.home.HomeScreen
import com.smait.jackie.ui.website.WebsiteScreen
import com.smait.jackie.ui.home.HomeViewModel
import com.smait.jackie.ui.navigation_map.NavigationMapScreen
import com.smait.jackie.ui.navigation_map.NavigationMapViewModel
import com.smait.jackie.ui.photobooth.PhotoBoothScreen
import com.smait.jackie.ui.settings.SettingsScreen

private const val TAG = "AppNavigation"

@Composable
fun AppScaffold(
    navController: NavHostController,
    themeConfig: ThemeConfig
) {
    val context = LocalContext.current
    val wsRepo = context.jackieApp.webSocketRepository
    val themeRepo = context.jackieApp.themeRepository
    val tourRepo = context.jackieApp.tourRepository

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
                NavigationMapViewModel(wsRepo, tourRepo) as T
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

    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("smait_settings", Context.MODE_PRIVATE)
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

    val isConnected by wsRepo.isConnected.collectAsStateWithLifecycle()
    LaunchedEffect(isConnected) {
        if (isConnected) {
            caeAudioManager.copyAssetsIfNeeded()
            val ws = wsRepo.currentWebSocket
            if (ws != null) {
                caeAudioManager.start(ws)
                Log.i(TAG, "Started CaeAudioManager")
            } else {
                Log.w(TAG, "WebSocket connected but currentWebSocket is null")
            }

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
        composable<Screen.Photo> {
            PhotoBoothScreen(navController = navController, wsRepo = wsRepo)
        }
        composable<Screen.Website> {
            WebsiteScreen(navController = navController)
        }
        composable<Screen.Settings> {
            SettingsScreen(navController = navController)
        }
    }
}
