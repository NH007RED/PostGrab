package com.postgrab.app.presentation.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import android.provider.MediaStore
import com.postgrab.app.presentation.ImportState
import com.postgrab.app.presentation.MainViewModel
import com.postgrab.app.presentation.NavEvent
import com.postgrab.app.presentation.downloads.DownloadsScreen
import com.postgrab.app.presentation.downloads.DownloadsViewModel
import com.postgrab.app.presentation.history.HistoryScreen
import com.postgrab.app.presentation.history.HistoryViewModel
import com.postgrab.app.presentation.home.*
import com.postgrab.app.presentation.components.rememberPickers
import com.postgrab.app.presentation.preview.PreviewScreen
import com.postgrab.app.presentation.preview.ViewerScreen
import com.postgrab.app.presentation.settings.AboutScreen
import com.postgrab.app.presentation.settings.SettingsScreen
import com.postgrab.app.presentation.settings.SettingsViewModel
import com.postgrab.app.utils.ShareUtils
import kotlinx.coroutines.launch

private data class Tab(val route: String, val label: String, val icon: ImageVector)

@Composable
fun PostGrabRoot(main: MainViewModel, factory: ViewModelProvider.Factory) {
    val nav = rememberNavController()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val mainState by main.state.collectAsStateWithLifecycle()
    var ready by remember { mutableStateOf(false) }

    val tabs = remember {
        listOf(
            Tab(Routes.HOME, "Home", Icons.Rounded.Home),
            Tab(Routes.DOWNLOADS, "Downloads", Icons.Rounded.Download),
            Tab(Routes.HISTORY, "History", Icons.Rounded.History),
            Tab(Routes.SETTINGS, "Settings", Icons.Rounded.Settings)
        )
    }
    val route = nav.currentBackStackEntryAsState().value?.destination?.route

    LaunchedEffect(ready) {
        if (ready) main.events.collect { e ->
            if (e is NavEvent.ToProgress) nav.navigate(Routes.PROGRESS) { launchSingleTop = true }
        }
    }

    val pickers = rememberPickers { uris, type -> main.importUris(uris, type) }
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    fun startDownload(all: Boolean, allowDuplicate: Boolean = false) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        main.download(all = all, allowDuplicate = allowDuplicate)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (tabs.any { it.route == route }) NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = route == tab.route,
                        onClick = {
                            nav.navigate(tab.route) {
                                popUpTo(Routes.HOME) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, tab.label) },
                        label = { Text(tab.label) },
                        alwaysShowLabel = true
                    )
                }
            }
        }
    ) { inner ->
        NavHost(
            navController = nav,
            startDestination = Routes.SPLASH,
            modifier = Modifier.padding(inner).consumeWindowInsets(inner),
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() },
            popEnterTransition = { fadeIn() },
            popExitTransition = { fadeOut() }
        ) {
            composable(Routes.SPLASH) {
                SplashScreen {
                    nav.navigate(Routes.HOME) { popUpTo(Routes.SPLASH) { inclusive = true } }
                    ready = true
                }
            }
            composable(Routes.HOME) {
                val history: HistoryViewModel = viewModel(factory = factory)
                val recent by history.recent.collectAsStateWithLifecycle()
                HomeScreen(
                    state = mainState, recent = recent,
                    onUrlChange = main::onUrlChange, onAnalyze = main::analyzeUrl,
                    onPhoto = pickers.photo, onFiles = pickers.files,
                    onOpenDownloads = {
                        nav.navigate(Routes.DOWNLOADS) { popUpTo(Routes.HOME) { saveState = true }; launchSingleTop = true; restoreState = true }
                    },
                    onOpenImport = { nav.navigate(Routes.IMPORT) },
                    onOpenViewer = { nav.navigate(Routes.viewer(it)) }
                )
            }
            composable(Routes.IMPORT) {
                ImportScreen(
                    url = mainState.url, onUrlChange = main::onUrlChange, onAnalyze = main::analyzeUrl,
                    onPhoto = pickers.photo, onFiles = pickers.files
                )
            }
            composable(Routes.PROGRESS) {
                ProgressScreen(
                    state = mainState.importState,
                    onCancel = main::cancelImport,
                    onBack = { nav.popBackStack() },
                    onReady = { nav.navigate(Routes.PREVIEW) { popUpTo(Routes.PROGRESS) { inclusive = true } } }
                )
            }
            composable(Routes.PREVIEW) {
                PreviewScreen(
                    state = mainState,
                    onBack = { nav.popBackStack() },
                    onSelect = main::selectIndex,
                    onDownload = { startDownload(all = false) },
                    onDownloadAll = { startDownload(all = true) },
                    onCancelSave = main::cancelSave,
                    onConfirmDuplicate = { startDownload(all = false, allowDuplicate = true) },
                    onDismissDuplicate = main::consumeSave,
                    onConsumeSave = main::consumeSave,
                    onFullscreen = { nav.navigate(Routes.viewer(Uri.fromFile(it.file).toString())) },
                    onShare = { ShareUtils.share(ctx, listOf(ShareUtils.fileUri(ctx, it.file)), it.mime) }
                )
            }
            composable(
                Routes.VIEWER, arguments = listOf(navArgument("uri") { type = NavType.StringType })
            ) { entry ->
                val raw = entry.arguments?.getString("uri").orEmpty()
                val uri = if (raw.startsWith("content") || raw.startsWith("file")) raw else Uri.decode(raw)
                ViewerScreen(
                    uri = uri, onBack = { nav.popBackStack() },
                    onShare = if (uri.startsWith("content")) ({ ShareUtils.share(ctx, listOf(Uri.parse(uri))) }) else null
                )
            }
            composable(Routes.DOWNLOADS) {
                val vm: DownloadsViewModel = viewModel(factory = factory)
                val state by vm.state.collectAsStateWithLifecycle()
                val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { vm.refresh() }
                DownloadsScreen(
                    state = state,
                    onRefresh = vm::refresh, onToggle = vm::toggle, onSelectAll = vm::selectAll,
                    onClearSelection = vm::clearSelection,
                    onOpen = { nav.navigate(Routes.viewer(it.uri.toString())) },
                    onShare = { ShareUtils.share(ctx, it) },
                    onDelete = { uris ->
                        scope.launch {
                            val failed = vm.delete(uris)
                            if (failed.isNotEmpty() && Build.VERSION.SDK_INT >= 30) {
                                val pi = MediaStore.createDeleteRequest(ctx.contentResolver, failed)
                                deleteLauncher.launch(IntentSenderRequest.Builder(pi.intentSender).build())
                            }
                        }
                    },
                    onGoImport = {
                        nav.navigate(Routes.HOME) { popUpTo(Routes.HOME) { saveState = true }; launchSingleTop = true; restoreState = true }
                    }
                )
            }
            composable(Routes.HISTORY) {
                val vm: HistoryViewModel = viewModel(factory = factory)
                val items by vm.items.collectAsStateWithLifecycle()
                HistoryScreen(
                    items = items,
                    onRetry = main::retry, onRemove = vm::remove, onClear = vm::clear,
                    onOpenViewer = { nav.navigate(Routes.viewer(it)) }
                )
            }
            composable(Routes.SETTINGS) {
                val vm: SettingsViewModel = viewModel(factory = factory)
                val settings by vm.settings.collectAsStateWithLifecycle()
                SettingsScreen(
                    settings = settings, onTheme = vm::setTheme, onDynamic = vm::setDynamic,
                    onFormat = vm::setFormat, onQuality = vm::setQuality,
                    onAbout = { nav.navigate(Routes.ABOUT) }
                )
            }
            composable(Routes.ABOUT) { AboutScreen(onBack = { nav.popBackStack() }) }
        }
    }
}
