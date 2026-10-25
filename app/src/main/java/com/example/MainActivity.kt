package com.example

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainViewModel
import com.example.ui.ScreenTab
import com.example.ui.components.AppBottomNav
import com.example.ui.components.AppTopBar
import com.example.ui.components.VideoPlayerModal
import com.example.ui.screens.*
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainApp(viewModel: MainViewModel) {
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val urlInput by viewModel.urlInput.collectAsStateWithLifecycle()
    val selectedPlatform by viewModel.selectedPlatform.collectAsStateWithLifecycle()
    val isValidating by viewModel.isValidating.collectAsStateWithLifecycle()
    val probeResult by viewModel.probeResult.collectAsStateWithLifecycle()
    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()
    val activePlayerUrl by viewModel.activePlayerUrl.collectAsStateWithLifecycle()
    val historyItems by viewModel.historyItems.collectAsStateWithLifecycle()
    val testCases by viewModel.testCases.collectAsStateWithLifecycle()
    val isRunningTests by viewModel.isRunningTests.collectAsStateWithLifecycle()

    // Notification permission launcher for Android 13+
    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Android Predictive BackHandler: return to SAVER if on other tabs
    if (activeTab != ScreenTab.SAVER) {
        BackHandler {
            viewModel.setTab(ScreenTab.SAVER)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                language = settings.language,
                onToggleLanguage = { viewModel.toggleLanguage() }
            )
        },
        bottomBar = {
            AppBottomNav(
                activeTab = activeTab,
                onTabSelected = { viewModel.setTab(it) },
                language = settings.language
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBackground)
        ) {
            when (activeTab) {
                ScreenTab.SAVER -> {
                    SaverScreen(
                        url = urlInput,
                        onUrlChange = { viewModel.onUrlChange(it) },
                        selectedPlatform = selectedPlatform,
                        onPlatformSelect = { viewModel.setPlatform(it) },
                        isValidating = isValidating,
                        onValidate = { viewModel.validateUrl() },
                        onPaste = { viewModel.pasteFromClipboard() },
                        probeResult = probeResult,
                        downloadProgress = downloadProgress,
                        onDownload = { viewModel.startDownload() },
                        onCancelDownload = { viewModel.cancelDownload() },
                        onPlayPreview = { viewModel.openPlayer(it) },
                        onCopy = { viewModel.copyToClipboard(it) },
                        onNavigateTab = { viewModel.setTab(it) },
                        toastMessage = toastMessage,
                        onDismissToast = { viewModel.clearToast() },
                        language = settings.language
                    )
                }
                ScreenTab.HISTORY -> {
                    HistoryScreen(
                        items = historyItems,
                        onReload = { url ->
                            viewModel.onUrlChange(url)
                            viewModel.setTab(ScreenTab.SAVER)
                            viewModel.validateUrl()
                        },
                        onPlay = { viewModel.openPlayer(it) },
                        onCopy = { viewModel.copyToClipboard(it) },
                        onDelete = { viewModel.deleteHistoryItem(it) },
                        onClearAll = { viewModel.clearAllHistory() },
                        language = settings.language
                    )
                }
                ScreenTab.PWA_HUB -> {
                    PwaHubScreen(
                        language = settings.language
                    )
                }
                ScreenTab.GUIDE -> {
                    GuideScreen(
                        language = settings.language
                    )
                }
                ScreenTab.TESTS -> {
                    TestsScreen(
                        testCases = testCases,
                        isRunning = isRunningTests,
                        onRunTests = { viewModel.runDiagnostics() },
                        onLoadTestUrl = { viewModel.loadTestUrl(it) },
                        language = settings.language
                    )
                }
                ScreenTab.SETTINGS -> {
                    SettingsScreen(
                        settings = settings,
                        onUpdateSettings = { viewModel.updateSettings(it) }
                    )
                }
            }
        }
    }

    // Modal Video Player
    activePlayerUrl?.let { playUrl ->
        VideoPlayerModal(
            videoUrl = playUrl,
            onDismiss = { viewModel.closePlayer() },
            language = settings.language
        )
    }
}
