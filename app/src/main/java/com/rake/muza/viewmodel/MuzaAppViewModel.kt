package com.rake.muza.viewmodel

import android.app.Activity
import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import com.rake.muza.core.AppLanguage
import com.rake.muza.core.ConnectionUiState
import com.rake.muza.core.DisplayHudPayload
import com.rake.muza.core.DisplayManager
import com.rake.muza.core.LanguageManager
import com.rake.muza.core.MuzaFeatures
import com.rake.muza.core.MuzaLocationManager
import com.rake.muza.core.MuzaLocationState
import com.rake.muza.core.MuzaVoiceCommand
import com.rake.muza.core.MuzaVoiceManager
import com.rake.muza.core.MuzaVoicePreset
import com.rake.muza.core.RayBanConnectionManager
import com.rake.muza.core.RayBanModel
import com.rake.muza.core.VoiceGender
import com.rake.muza.core.VoiceSpeed
import com.rake.muza.core.VoiceUiState
import com.rake.muza.data.ai.MuzaAiRepository
import com.rake.muza.data.firebase.MuzaCloudRepository
import com.rake.muza.data.firebase.UserProfile
import com.rake.muza.data.local.PhotoHistoryEntity
import com.rake.muza.data.local.PhotoHistoryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

enum class CameraFilterMode {
    NATURAL,
    WARM,
    CONTRAST,
}

data class HomeCameraUiState(
    val isLiveCameraActive: Boolean = true,
    val isFlashOn: Boolean = false,
    val isScanOverlayActive: Boolean = true,
    val showParamsSheet: Boolean = false,
    val brightness: Float = 0.70f,
    val filterMode: CameraFilterMode = CameraFilterMode.NATURAL,
    val isRecording: Boolean = false,
    val recordingSeconds: Int = 0,
    val isAnalyzingVision: Boolean = false,
    val lastVisionSummary: String? = null,
    val lastTranslation: Pair<String, String>? = null,
    val statusBanner: String? = null,
    val locationLabel: String = "Алматы, Абай көшесі",
    val frameSizeLabel: String = "52-18-140",
)

sealed interface AppNavigationEvent {
    data object OpenAiChat : AppNavigationEvent
    data class OpenAiChatWithPrompt(val prompt: String) : AppNavigationEvent
    data object OpenHome : AppNavigationEvent
    data object OpenMap : AppNavigationEvent
}

class MuzaAppViewModel(
    application: Application,
    private val currentUserId: String,
    private val cloudRepository: MuzaCloudRepository,
    private val photoRepository: PhotoHistoryRepository,
    private val aiRepository: MuzaAiRepository,
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "MuzaAppViewModel"
    }

    val connectionManager = RayBanConnectionManager(application)
    val displayManager = DisplayManager()
    val voiceManager = MuzaVoiceManager(application, ::handleVoiceCommand)
    val locationManager = MuzaLocationManager(application)

    val connectionState: StateFlow<ConnectionUiState> = connectionManager.state
    val hudState: StateFlow<DisplayHudPayload> = displayManager.hudState
    val voiceState: StateFlow<VoiceUiState> = voiceManager.state
    val locationState: StateFlow<MuzaLocationState> = locationManager.state

    val allPhotos: StateFlow<List<PhotoHistoryEntity>> = photoRepository.allPhotos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    val savedPhotos: StateFlow<List<PhotoHistoryEntity>> = photoRepository.savedPhotos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    private val _cameraState = MutableStateFlow(
        HomeCameraUiState(locationLabel = LanguageManager.currentStrings.locationDefault)
    )
    val cameraState: StateFlow<HomeCameraUiState> = _cameraState.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<AppNavigationEvent>(extraBufferCapacity = 4)
    val navigationEvents: SharedFlow<AppNavigationEvent> = _navigationEvents.asSharedFlow()

    @Volatile
    private var latestCameraFrame: Bitmap? = null
    private var recordingJob: Job? = null
    private var bannerJob: Job? = null

    init {
        bootstrapUserProfile()
        observeGpsIntoCameraBadge()
    }

    private fun observeGpsIntoCameraBadge() {
        viewModelScope.launch {
            locationManager.state.collect { loc ->
                if (loc.streetAddress.isNotBlank()) {
                    _cameraState.update { it.copy(locationLabel = loc.streetAddress) }
                }
            }
        }
    }

    private fun bootstrapUserProfile() {
        val fbUser = Firebase.auth.currentUser
        viewModelScope.launch {
            cloudRepository.ensureUserProfile(
                displayName = fbUser?.displayName ?: "Muza User",
                email = fbUser?.email.orEmpty(),
                languageCode = LanguageManager.currentLanguage.code,
                voiceGenderCode = voiceManager.state.value.gender.code,
                voiceSpeedCode = voiceManager.state.value.speed.code,
                selectedGlassesModel = connectionManager.state.value.selectedModel.id,
            )
        }

        viewModelScope.launch {
            cloudRepository.observeUserProfile(currentUserId)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    Log.w(TAG, "Profile sync warning: ${firestoreError?.message ?: error.message}")
                }
                .collect { profile ->
                    _userProfile.value = profile
                    if (profile != null) {
                        val lang = AppLanguage.fromCode(profile.preferredLanguage)
                        if (lang != LanguageManager.currentLanguage) {
                            LanguageManager.setLanguage(lang)
                            voiceManager.syncLanguage()
                        }
                        connectionManager.selectModel(RayBanModel.fromId(profile.selectedGlassesModel))
                        voiceManager.setVoiceSpeed(VoiceSpeed.fromCode(profile.voiceSpeed))
                    }
                }
        }
    }

    fun initializeToolkit(activity: Activity) {
        connectionManager.initializeToolkit(activity)
    }

    fun startGpsTracking() {
        locationManager.startTracking()
    }

    fun refreshGpsLocation() {
        locationManager.refreshNow()
        val summary = locationManager.spokenLocationSummary()
        showBanner(summary)
    }

    fun speakCurrentLocation() {
        locationManager.startTracking()
        val summary = locationManager.spokenLocationSummary()
        voiceManager.speak(summary)
        sendLocationToHud()
        showBanner(summary)
    }

    fun sendLocationToHud() {
        val loc = locationState.value
        val coords = String.format(Locale.US, "%.4f, %.4f", loc.latitude, loc.longitude)
        val speedNote = if (loc.speedKmh > 0.5f) {
            String.format(Locale.US, "%.1f km/h • %s", loc.speedKmh, coords)
        } else {
            coords
        }
        displayManager.showNavigation(
            locationTitle = loc.streetAddress,
            directionText = speedNote,
            batteryPercent = connectionState.value.deviceInfo.batteryPercent,
        )
    }

    fun updateLatestCameraFrame(bitmap: Bitmap) {
        latestCameraFrame = bitmap
    }

    fun getLatestCameraFrameCopy(): Bitmap? {
        val src = latestCameraFrame ?: return null
        return runCatching {
            if (!src.isRecycled) src.copy(Bitmap.Config.ARGB_8888, false) else null
        }.getOrNull()
    }

    fun selectGlassesModel(model: RayBanModel) {
        connectionManager.selectModel(model)
        viewModelScope.launch {
            cloudRepository.updatePreferences(selectedGlassesModel = model.id)
        }
    }

    fun startBluetoothScan() {
        connectionManager.startBluetoothScan()
    }

    fun stopBluetoothScan() {
        connectionManager.stopBluetoothScan()
    }

    fun connectSelectedGlasses(pairingPin: String = "884210", onConnected: () -> Unit = {}) {
        val model = connectionState.value.selectedModel
        connectionManager.connect(model = model, pairingPin = pairingPin) { session ->
            displayManager.attachToSession(session)
            // Route hands-free glasses microphone & speaker and enable "Muza" wake-word immediately
            voiceManager.setGlassesAudioRouting(true)
            voiceManager.startWakeWordListening()
            val connectedMsg = "${model.title} ${LanguageManager.currentStrings.connectedSuffix}"
            displayManager.showText(
                title = "Muza Glasses",
                message = connectedMsg,
                autoHideMs = 4_000L,
            )
            voiceManager.speak("$connectedMsg. ${LanguageManager.currentStrings.wakeResponse}")
            onConnected()
        }
        viewModelScope.launch {
            cloudRepository.updatePreferences(selectedGlassesModel = model.id)
        }
    }

    fun disconnectGlasses() {
        voiceManager.setGlassesAudioRouting(false)
        displayManager.detach()
        connectionManager.disconnect()
    }

    fun setLanguage(lang: AppLanguage) {
        LanguageManager.setLanguage(lang)
        voiceManager.syncLanguage()
        if (!locationState.value.hasGpsFix) {
            _cameraState.update { it.copy(locationLabel = LanguageManager.currentStrings.locationDefault) }
        }
        viewModelScope.launch {
            cloudRepository.updatePreferences(languageCode = lang.code)
        }
    }

    fun selectVoicePreset(preset: MuzaVoicePreset) {
        voiceManager.setVoicePreset(preset)
        viewModelScope.launch {
            cloudRepository.updatePreferences(voiceGenderCode = preset.gender.code)
        }
    }

    fun previewVoicePreset(preset: MuzaVoicePreset) {
        voiceManager.speak(
            text = preset.sampleSentence(LanguageManager.currentLanguage),
            force = true,
            overridePreset = preset,
        )
    }

    fun setUseNeuralKazakhVoice(enabled: Boolean) {
        voiceManager.setUseNeuralKazakhVoice(enabled)
    }

    fun setVoiceGender(gender: VoiceGender) {
        voiceManager.setVoiceGender(gender)
        viewModelScope.launch {
            cloudRepository.updatePreferences(voiceGenderCode = gender.code)
        }
    }

    fun setVoiceSpeed(speed: VoiceSpeed) {
        voiceManager.setVoiceSpeed(speed)
        viewModelScope.launch {
            cloudRepository.updatePreferences(voiceSpeedCode = speed.code)
        }
    }

    fun testVoice() {
        val preset = voiceState.value.activePreset
        voiceManager.speak(preset.sampleSentence(LanguageManager.currentLanguage), force = true)
    }

    fun toggleWakeWordListening() {
        if (!MuzaFeatures.voiceWakeWordMuza) {
            showBanner(LanguageManager.currentStrings.featureDisabledNotice)
            return
        }
        if (voiceState.value.isListening) {
            voiceManager.stopWakeWordListening()
        } else {
            if (connectionState.value.isConnected) {
                voiceManager.setGlassesAudioRouting(true)
            }
            voiceManager.startWakeWordListening()
        }
    }

    fun submitQuickVoiceCommand(phrase: String) {
        voiceManager.processTranscript(phrase, bypassWakeWord = true)
    }

    fun toggleFlash() {
        _cameraState.update { it.copy(isFlashOn = !it.isFlashOn) }
    }

    fun toggleScanOverlay() {
        _cameraState.update { it.copy(isScanOverlayActive = !it.isScanOverlayActive) }
    }

    fun toggleParamsSheet() {
        _cameraState.update { it.copy(showParamsSheet = !it.showParamsSheet) }
    }

    fun setBrightness(value: Float) {
        _cameraState.update { it.copy(brightness = value.coerceIn(0.2f, 1.0f)) }
    }

    fun setFilterMode(mode: CameraFilterMode) {
        _cameraState.update { it.copy(filterMode = mode) }
    }

    fun capturePhoto(capturedBitmap: Bitmap? = null) {
        if (!MuzaFeatures.cameraPhoto) {
            showBanner(LanguageManager.currentStrings.featureDisabledNotice)
            return
        }
        viewModelScope.launch {
            val bitmap = capturedBitmap ?: getLatestCameraFrameCopy() ?: createFallbackFrameBitmap()
            val city = locationState.value.city.ifBlank {
                _cameraState.value.locationLabel.substringBefore(",").trim().ifBlank { "Алматы" }
            }
            val modelName = connectionState.value.deviceInfo.deviceName
            val initialDescription = when (LanguageManager.currentLanguage) {
                AppLanguage.KK -> "$modelName камерасымен түсірілді • $city"
                AppLanguage.RU -> "Снято на камеру $modelName • $city"
                AppLanguage.EN -> "Captured with $modelName • $city"
            }

            val savedEntity = photoRepository.saveCapturedPhoto(
                bitmap = bitmap,
                city = city,
                description = initialDescription,
                glassesModel = modelName,
            )

            displayManager.showPhotoCaptured()
            showBanner(LanguageManager.currentStrings.photoSavedToast)
            voiceManager.speak(LanguageManager.currentStrings.photoTakenResponse)

            cloudRepository.syncPhotoRecord(
                docId = savedEntity.id,
                localMediaUri = savedEntity.mediaUri,
                city = savedEntity.city,
                dateLabel = savedEntity.dateLabel,
                description = savedEntity.description,
                favorite = savedEntity.isFavorite,
                glassesModel = savedEntity.glassesModel,
            )

            if (MuzaFeatures.aiVisionDescribe) {
                aiRepository.describeCameraScene(
                    bitmap = bitmap,
                    locationContext = _cameraState.value.locationLabel,
                ).onSuccess { caption ->
                    photoRepository.updateDescription(savedEntity.id, caption)
                    cloudRepository.syncPhotoRecord(
                        docId = savedEntity.id,
                        localMediaUri = savedEntity.mediaUri,
                        city = savedEntity.city,
                        dateLabel = savedEntity.dateLabel,
                        description = caption,
                        favorite = savedEntity.isFavorite,
                        glassesModel = savedEntity.glassesModel,
                    )
                }
            }
        }
    }

    fun toggleVideoRecording() {
        if (!MuzaFeatures.cameraVideo) {
            showBanner(LanguageManager.currentStrings.featureDisabledNotice)
            return
        }
        if (_cameraState.value.isRecording) {
            stopVideoRecording()
        } else {
            startVideoRecording()
        }
    }

    fun startVideoRecording() {
        if (!MuzaFeatures.cameraVideo || _cameraState.value.isRecording) return
        _cameraState.update { it.copy(isRecording = true, recordingSeconds = 0) }
        showBanner(LanguageManager.currentStrings.recordingStartedToast)
        voiceManager.speak(LanguageManager.currentStrings.videoOnResponse)
        recordingJob?.cancel()
        recordingJob = viewModelScope.launch {
            var secs = 0
            while (isActive) {
                val formatted = formatDuration(secs)
                displayManager.showRecordingStatus(formatted)
                delay(1_000L)
                secs++
                _cameraState.update { it.copy(recordingSeconds = secs) }
            }
        }
    }

    fun stopVideoRecording() {
        if (!_cameraState.value.isRecording) return
        recordingJob?.cancel()
        recordingJob = null
        _cameraState.update { it.copy(isRecording = false, recordingSeconds = 0) }
        displayManager.clear()
        showBanner(LanguageManager.currentStrings.recordingStoppedToast)
        voiceManager.speak(LanguageManager.currentStrings.stoppedResponse)
    }

    fun describeWhatIsInFront() {
        if (!MuzaFeatures.aiVisionDescribe) {
            showBanner(LanguageManager.currentStrings.featureDisabledNotice)
            return
        }
        if (_cameraState.value.isAnalyzingVision) return
        viewModelScope.launch {
            _cameraState.update { it.copy(isAnalyzingVision = true) }
            val frame = getLatestCameraFrameCopy()
            val result = aiRepository.describeCameraScene(
                bitmap = frame,
                locationContext = _cameraState.value.locationLabel,
            )
            result.onSuccess { description ->
                _cameraState.update {
                    it.copy(
                        isAnalyzingVision = false,
                        lastVisionSummary = description,
                    )
                }
                displayManager.showAiResponse(description)
                voiceManager.speak(description)
                showBanner(description)
                cloudRepository.saveAiMessage(
                    role = "user",
                    text = LanguageManager.currentStrings.whatDoYouSee,
                    languageCode = LanguageManager.currentLanguage.code,
                    hasAttachment = frame != null,
                )
                cloudRepository.saveAiMessage(
                    role = "model",
                    text = description,
                    languageCode = LanguageManager.currentLanguage.code,
                    hasAttachment = false,
                )
            }.onFailure { error ->
                _cameraState.update { it.copy(isAnalyzingVision = false) }
                val msg = error.localizedMessage ?: "AI analysis failed"
                showBanner(msg)
                voiceManager.speak(msg)
            }
        }
    }

    fun translateCurrentView(sourceText: String? = null) {
        if (!MuzaFeatures.aiTranslation) {
            showBanner(LanguageManager.currentStrings.featureDisabledNotice)
            return
        }
        if (_cameraState.value.isAnalyzingVision) return
        viewModelScope.launch {
            _cameraState.update { it.copy(isAnalyzingVision = true) }
            val frame = if (sourceText.isNullOrBlank()) getLatestCameraFrameCopy() else null
            val result = aiRepository.translateSceneOrText(
                sourceText = sourceText,
                bitmap = frame,
                targetLanguage = LanguageManager.currentLanguage,
            )
            result.onSuccess { pair ->
                _cameraState.update {
                    it.copy(
                        isAnalyzingVision = false,
                        lastTranslation = pair,
                    )
                }
                val targetCode = LanguageManager.currentLanguage.code
                displayManager.showTranslation(
                    originalText = pair.first,
                    translatedText = pair.second,
                    languagePair = "AUTO → $targetCode",
                )
                val spoken = when (LanguageManager.currentLanguage) {
                    AppLanguage.KK -> "Аудармасы: ${pair.second}"
                    AppLanguage.RU -> "Перевод: ${pair.second}"
                    AppLanguage.EN -> "Translation: ${pair.second}"
                }
                voiceManager.speak(spoken)
                showBanner("${pair.first} → ${pair.second}")
            }.onFailure { error ->
                _cameraState.update { it.copy(isAnalyzingVision = false) }
                showBanner(error.localizedMessage ?: "Translation failed")
            }
        }
    }

    private fun answerSpokenAiQuestion(question: String) {
        if (!MuzaFeatures.aiConversation) {
            showBanner(LanguageManager.currentStrings.featureDisabledNotice)
            return
        }
        viewModelScope.launch {
            _cameraState.update { it.copy(isAnalyzingVision = true) }
            val lang = LanguageManager.currentLanguage
            cloudRepository.saveAiMessage(
                role = "user",
                text = question,
                languageCode = lang.code,
                hasAttachment = false,
            )
            val result = aiRepository.answerVoiceQuestion(
                question = question,
                cameraContext = null,
                locationContext = _cameraState.value.locationLabel,
                language = lang,
            )
            result.onSuccess { answer ->
                _cameraState.update { it.copy(isAnalyzingVision = false) }
                displayManager.showAiResponse(answer)
                voiceManager.speak(answer)
                showBanner(answer)
                cloudRepository.saveAiMessage(
                    role = "model",
                    text = answer,
                    languageCode = lang.code,
                    hasAttachment = false,
                )
            }.onFailure { error ->
                _cameraState.update { it.copy(isAnalyzingVision = false) }
                showBanner(error.localizedMessage ?: "AI request failed")
            }
        }
    }

    fun togglePhotoFavorite(photo: PhotoHistoryEntity) {
        viewModelScope.launch {
            val next = photoRepository.toggleFavorite(photo)
            cloudRepository.updateCloudPhotoFavorite(photo.id, next)
        }
    }

    fun deletePhoto(photo: PhotoHistoryEntity) {
        viewModelScope.launch {
            photoRepository.deletePhoto(photo)
            cloudRepository.deleteCloudPhotoRecord(photo.id)
        }
    }

    private fun handleVoiceCommand(command: MuzaVoiceCommand) {
        when (command) {
            MuzaVoiceCommand.WakeAcknowledge -> {
                showBanner(LanguageManager.currentStrings.wakeResponse)
            }
            MuzaVoiceCommand.HowAreYou -> {
                val reply = LanguageManager.currentStrings.howAreYouResponse
                voiceManager.speak(reply)
                displayManager.showAiResponse(reply)
                showBanner(reply)
            }
            MuzaVoiceCommand.StartCamera -> {
                _cameraState.update { it.copy(isLiveCameraActive = true) }
                _navigationEvents.tryEmit(AppNavigationEvent.OpenHome)
                voiceManager.speak(LanguageManager.currentStrings.cameraOnResponse)
                showBanner(LanguageManager.currentStrings.cameraOnResponse)
            }
            MuzaVoiceCommand.StartVideo -> {
                _navigationEvents.tryEmit(AppNavigationEvent.OpenHome)
                startVideoRecording()
            }
            MuzaVoiceCommand.TakePhoto -> {
                capturePhoto()
            }
            MuzaVoiceCommand.DescribeFront -> {
                describeWhatIsInFront()
            }
            is MuzaVoiceCommand.Translate -> {
                translateCurrentView(command.textToTranslate)
            }
            MuzaVoiceCommand.StopRecording -> {
                stopVideoRecording()
            }
            MuzaVoiceCommand.OpenMapAndGps -> {
                locationManager.startTracking()
                _navigationEvents.tryEmit(AppNavigationEvent.OpenMap)
                val summary = "${LanguageManager.currentStrings.mapOpenedResponse} ${locationManager.spokenLocationSummary()}"
                voiceManager.speak(summary)
                sendLocationToHud()
                showBanner(summary)
            }
            MuzaVoiceCommand.WhereAmI -> {
                locationManager.startTracking()
                _navigationEvents.tryEmit(AppNavigationEvent.OpenMap)
                speakCurrentLocation()
            }
            is MuzaVoiceCommand.GeneralAiQuery -> {
                answerSpokenAiQuestion(command.query)
            }
        }
    }

    fun showBanner(message: String) {
        bannerJob?.cancel()
        _cameraState.update { it.copy(statusBanner = message) }
        bannerJob = viewModelScope.launch {
            delay(5_500L)
            _cameraState.update { it.copy(statusBanner = null) }
        }
    }

    fun formatDuration(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return String.format(Locale.US, "%02d:%02d", m, s)
    }

    private fun createFallbackFrameBitmap(): Bitmap {
        val w = 720
        val h = 960
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, w.toFloat(), h.toFloat(),
                intArrayOf(Color.parseColor("#0A0D18"), Color.parseColor("#132042"), Color.parseColor("#0A0A0F")),
                null,
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#60A5FA")
            textSize = 44f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Muza Glasses • HD Capture", w / 2f, h / 2f - 20f, textPaint)
        textPaint.color = Color.parseColor("#CBD5E1")
        textPaint.textSize = 28f
        canvas.drawText(connectionState.value.deviceInfo.deviceName, w / 2f, h / 2f + 36f, textPaint)
        return bmp
    }

    override fun onCleared() {
        super.onCleared()
        recordingJob?.cancel()
        bannerJob?.cancel()
        locationManager.stopTracking()
        voiceManager.release()
        displayManager.detach()
        connectionManager.stopBluetoothScan()
    }

    class Factory(
        private val application: Application,
        private val currentUserId: String,
        private val cloudRepository: MuzaCloudRepository,
        private val photoRepository: PhotoHistoryRepository,
        private val aiRepository: MuzaAiRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MuzaAppViewModel(
                application = application,
                currentUserId = currentUserId,
                cloudRepository = cloudRepository,
                photoRepository = photoRepository,
                aiRepository = aiRepository,
            ) as T
        }
    }
}
