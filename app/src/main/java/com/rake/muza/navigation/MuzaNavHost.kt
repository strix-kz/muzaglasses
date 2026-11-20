package com.rake.muza.navigation

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.rake.muza.core.LanguageManager
import com.rake.muza.data.ai.MuzaAiRepository
import com.rake.muza.data.firebase.MuzaCloudRepository
import com.rake.muza.data.firebase.authStateFlow
import com.rake.muza.data.firebase.signOutGoogleAccount
import com.rake.muza.data.local.MuzaDatabase
import com.rake.muza.data.local.PhotoHistoryRepository
import com.rake.muza.ui.screens.AiChatScreen
import com.rake.muza.ui.screens.AuthScreen
import com.rake.muza.ui.screens.ConnectedScreen
import com.rake.muza.ui.screens.GlassesSelectionScreen
import com.rake.muza.ui.screens.HistoryScreen
import com.rake.muza.ui.screens.HomeScreen
import com.rake.muza.ui.screens.HowToConnectScreen
import com.rake.muza.ui.screens.MapScreen
import com.rake.muza.ui.screens.ProfileSettingsScreen
import com.rake.muza.ui.screens.RequirementsScreen
import com.rake.muza.ui.screens.SplashScreen
import com.rake.muza.ui.theme.MuzaColors
import com.rake.muza.viewmodel.AiChatViewModel
import com.rake.muza.viewmodel.AppNavigationEvent
import com.rake.muza.viewmodel.MuzaAppViewModel
import kotlinx.serialization.Serializable

@Serializable data object RouteGlassesSelection
@Serializable data object RouteRequirements
@Serializable data object RouteHowToConnect
@Serializable data object RouteConnected
@Serializable data object RouteHome
@Serializable data object RouteMap
@Serializable data object RouteAiChat
@Serializable data object RouteHistory
@Serializable data object RouteProfile

private data class BottomNavTab(
    val route: Any,
    val label: String,
    val icon: ImageVector,
    val testTag: String,
)

@Composable
fun MuzaAppRoot() {
    var showSplash by remember { mutableStateOf(true) }
    val currentUser: FirebaseUser? by remember { Firebase.auth.authStateFlow() }
        .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    if (showSplash) {
        SplashScreen(onFinished = { showSplash = false })
        return
    }

    val signedInUser = currentUser
    if (signedInUser == null) {
        AuthScreen(onSignedIn = {})
    } else {
        key(signedInUser.uid) {
            AuthenticatedMuzaShell(user = signedInUser)
        }
    }
}

@Composable
private fun AuthenticatedMuzaShell(
    user: FirebaseUser,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val app = context.applicationContext as Application
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val cloudRepository = remember(context) { MuzaCloudRepository(context) }
    val photoRepository = remember(context) {
        PhotoHistoryRepository(context, MuzaDatabase.getInstance(context).photoHistoryDao())
    }
    val aiRepository = remember { MuzaAiRepository() }

    val appViewModel: MuzaAppViewModel = viewModel(
        key = "muza_app_vm_${user.uid}",
        factory = MuzaAppViewModel.Factory(
            application = app,
            currentUserId = user.uid,
            cloudRepository = cloudRepository,
            photoRepository = photoRepository,
            aiRepository = aiRepository,
        ),
    )

    val aiChatViewModel: AiChatViewModel = viewModel(
        key = "muza_ai_vm_${user.uid}",
        factory = AiChatViewModel.Factory(
            currentUserId = user.uid,
            aiRepository = aiRepository,
            cloudRepository = cloudRepository,
        ),
    )

    val strings by LanguageManager.strings.collectAsState()
    val connectionState by appViewModel.connectionState.collectAsStateWithLifecycle()
    val cameraState by appViewModel.cameraState.collectAsStateWithLifecycle()
    val hudState by appViewModel.hudState.collectAsStateWithLifecycle()
    val voiceState by appViewModel.voiceState.collectAsStateWithLifecycle()
    val locationState by appViewModel.locationState.collectAsStateWithLifecycle()
    val allPhotos by appViewModel.allPhotos.collectAsStateWithLifecycle()
    val savedPhotos by appViewModel.savedPhotos.collectAsStateWithLifecycle()
    val aiState by aiChatViewModel.state.collectAsStateWithLifecycle()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    var pendingAiPrompt by remember { mutableStateOf<String?>(null) }

    val requiredPermissions = remember {
        buildList {
            add(Manifest.permission.CAMERA)
            add(Manifest.permission.RECORD_AUDIO)
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_CONNECT)
                add(Manifest.permission.BLUETOOTH_SCAN)
            }
        }.toTypedArray()
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        hasCameraPermission = grants[Manifest.permission.CAMERA] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        hasLocationPermission = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasLocationPermission) {
            appViewModel.startGpsTracking()
        }
        if (grants[Manifest.permission.RECORD_AUDIO] == true) {
            appViewModel.voiceManager.startWakeWordListening()
        }
    }

    LaunchedEffect(activity) {
        if (activity != null) {
            appViewModel.initializeToolkit(activity)
        }
        val missing = requiredPermissions.any {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing) {
            permissionsLauncher.launch(requiredPermissions)
        } else {
            hasCameraPermission = true
            hasLocationPermission = true
            appViewModel.startGpsTracking()
            appViewModel.voiceManager.startWakeWordListening()
        }
    }

    val navController = rememberNavController()

    LaunchedEffect(appViewModel) {
        appViewModel.navigationEvents.collect { event ->
            when (event) {
                AppNavigationEvent.OpenHome -> {
                    navController.navigate(RouteHome) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
                AppNavigationEvent.OpenMap -> {
                    navController.navigate(RouteMap) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
                AppNavigationEvent.OpenAiChat -> {
                    navController.navigate(RouteAiChat) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
                is AppNavigationEvent.OpenAiChatWithPrompt -> {
                    pendingAiPrompt = event.prompt
                    navController.navigate(RouteAiChat) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = navBackStackEntry?.destination
    val showBottomBar = currentDest != null && (
        currentDest.hasRoute<RouteHome>() ||
            currentDest.hasRoute<RouteMap>() ||
            currentDest.hasRoute<RouteAiChat>() ||
            currentDest.hasRoute<RouteHistory>() ||
            currentDest.hasRoute<RouteProfile>()
        )

    val bottomTabs = listOf(
        BottomNavTab(RouteHome, strings.navHome, Icons.Rounded.Home, "nav_tab_home"),
        BottomNavTab(RouteMap, strings.navMap, Icons.Rounded.Map, "nav_tab_map"),
        BottomNavTab(RouteAiChat, strings.navStyles, Icons.Rounded.AutoAwesome, "nav_tab_ai"),
        BottomNavTab(RouteHistory, strings.navHistory, Icons.Rounded.History, "nav_tab_history"),
        BottomNavTab(RouteProfile, strings.navProfile, Icons.Rounded.Person, "nav_tab_profile"),
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MuzaColors.Background),
        containerColor = MuzaColors.Background,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .testTag("muza_bottom_nav"),
                    containerColor = MuzaColors.Surface,
                    tonalElevation = 0.dp,
                ) {
                    bottomTabs.forEach { tab ->
                        val selected = when (tab.route) {
                            RouteHome -> currentDest?.hasRoute<RouteHome>() == true
                            RouteMap -> currentDest?.hasRoute<RouteMap>() == true
                            RouteAiChat -> currentDest?.hasRoute<RouteAiChat>() == true
                            RouteHistory -> currentDest?.hasRoute<RouteHistory>() == true
                            RouteProfile -> currentDest?.hasRoute<RouteProfile>() == true
                            else -> false
                        }
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(RouteHome) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            modifier = Modifier.testTag(tab.testTag),
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label,
                                )
                            },
                            label = { Text(text = tab.label, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MuzaColors.BlueGlow,
                                selectedTextColor = MuzaColors.BlueGlow,
                                indicatorColor = MuzaColors.BluePrimary.copy(alpha = 0.18f),
                                unselectedIconColor = MuzaColors.TextSecondary,
                                unselectedTextColor = MuzaColors.TextSecondary,
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
        ) {
            NavHost(
                navController = navController,
                startDestination = RouteGlassesSelection,
            ) {
                composable<RouteGlassesSelection> {
                    GlassesSelectionScreen(
                        selectedModel = connectionState.selectedModel,
                        onSelectModel = appViewModel::selectGlassesModel,
                        onProceed = { navController.navigate(RouteRequirements) },
                        onBack = if (navController.previousBackStackEntry != null) {
                            { navController.popBackStack() }
                        } else null,
                    )
                }

                composable<RouteRequirements> {
                    RequirementsScreen(
                        onContinue = { navController.navigate(RouteHowToConnect) },
                    )
                }

                composable<RouteHowToConnect> {
                    HowToConnectScreen(
                        connectionState = connectionState,
                        onStartScan = appViewModel::startBluetoothScan,
                        onStopScan = appViewModel::stopBluetoothScan,
                        onConnectNow = { pin ->
                            appViewModel.connectSelectedGlasses(pairingPin = pin) {
                                navController.navigate(RouteConnected)
                            }
                        },
                    )
                }

                composable<RouteConnected> {
                    ConnectedScreen(
                        deviceInfo = connectionState.deviceInfo,
                        onStart = {
                            navController.navigate(RouteHome) {
                                popUpTo(RouteGlassesSelection) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                    )
                }

                composable<RouteHome> {
                    HomeScreen(
                        cameraState = cameraState,
                        connectionState = connectionState,
                        hudState = hudState,
                        voiceState = voiceState,
                        hasCameraPermission = hasCameraPermission,
                        formattedRecordingTime = appViewModel.formatDuration(cameraState.recordingSeconds),
                        onRequestCameraPermission = { permissionsLauncher.launch(requiredPermissions) },
                        onRequestAudioPermission = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                                permissionsLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                            }
                        },
                        onFrameAvailable = appViewModel::updateLatestCameraFrame,
                        onToggleFlash = appViewModel::toggleFlash,
                        onToggleScanOverlay = appViewModel::toggleScanOverlay,
                        onToggleParamsSheet = appViewModel::toggleParamsSheet,
                        onBrightnessChange = appViewModel::setBrightness,
                        onFilterModeChange = appViewModel::setFilterMode,
                        onCapturePhoto = { appViewModel.capturePhoto() },
                        onToggleRecording = appViewModel::toggleVideoRecording,
                        onAskAi = {
                            navController.navigate(RouteAiChat) {
                                popUpTo(RouteHome) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onWhatDoYouSee = appViewModel::describeWhatIsInFront,
                        onTranslate = { appViewModel.translateCurrentView() },
                        onToggleVoice = appViewModel::toggleWakeWordListening,
                        onOpenMap = {
                            navController.navigate(RouteMap) {
                                popUpTo(RouteHome) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onDismissHud = { appViewModel.displayManager.clear() },
                    )
                }

                composable<RouteMap> {
                    MapScreen(
                        locationState = locationState,
                        hudState = hudState,
                        hasLocationPermission = hasLocationPermission,
                        onRequestLocationPermission = {
                            permissionsLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                )
                            )
                        },
                        onStartGps = appViewModel::startGpsTracking,
                        onSpeakWhereAmI = appViewModel::speakCurrentLocation,
                        onSendToHud = appViewModel::sendLocationToHud,
                        onDismissHud = { appViewModel.displayManager.clear() },
                    )
                }

                composable<RouteAiChat> {
                    AiChatScreen(
                        uiState = aiState,
                        initialPrompt = pendingAiPrompt,
                        onConsumeInitialPrompt = { pendingAiPrompt = null },
                        onAttachBitmap = aiChatViewModel::attachBitmap,
                        onGrabLiveCameraFrame = appViewModel::getLatestCameraFrameCopy,
                        onVoiceDictationClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                                permissionsLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                            } else {
                                appViewModel.voiceManager.triggerSingleCommandDictation()
                            }
                        },
                        onSendPrompt = { prompt ->
                            aiChatViewModel.sendPrompt(prompt) { reply ->
                                appViewModel.voiceManager.speak(reply.take(360))
                                appViewModel.displayManager.showAiResponse(reply)
                            }
                        },
                        onClearError = aiChatViewModel::clearError,
                    )
                }

                composable<RouteHistory> {
                    HistoryScreen(
                        allPhotos = allPhotos,
                        savedPhotos = savedPhotos,
                        onToggleFavorite = appViewModel::togglePhotoFavorite,
                        onDeletePhoto = appViewModel::deletePhoto,
                    )
                }

                composable<RouteProfile> {
                    ProfileSettingsScreen(
                        displayName = user.displayName ?: "Muza User",
                        email = user.email.orEmpty(),
                        connectionState = connectionState,
                        voiceState = voiceState,
                        displayManager = appViewModel.displayManager,
                        onSelectLanguage = appViewModel::setLanguage,
                        onSelectVoicePreset = appViewModel::selectVoicePreset,
                        onPreviewVoicePreset = appViewModel::previewVoicePreset,
                        onToggleNeuralVoice = appViewModel::setUseNeuralKazakhVoice,
                        onSelectVoiceSpeed = appViewModel::setVoiceSpeed,
                        onTestVoice = appViewModel::testVoice,
                        onManageGlasses = { navController.navigate(RouteGlassesSelection) },
                        onSignOut = {
                            appViewModel.voiceManager.stopWakeWordListening()
                            appViewModel.disconnectGlasses()
                            signOutGoogleAccount(
                                credentialManager = credentialManager,
                                scope = scope,
                            )
                        },
                    )
                }
            }
        }
    }
}
