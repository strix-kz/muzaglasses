package com.rake.muza.core

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.InlineDataPart
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

enum class VoiceGender(val code: String) {
    FEMALE("FEMALE"),
    MALE("MALE");

    companion object {
        fun fromCode(raw: String?): VoiceGender =
            entries.firstOrNull { it.code.equals(raw, ignoreCase = true) } ?: FEMALE
    }
}

enum class VoiceSpeed(val code: String, val rate: Float) {
    SLOW("SLOW", 0.82f),
    MEDIUM("MEDIUM", 1.0f),
    FAST("FAST", 1.24f);

    companion object {
        fun fromCode(raw: String?): VoiceSpeed =
            entries.firstOrNull { it.code.equals(raw, ignoreCase = true) } ?: MEDIUM
    }
}

/**
 * 6 Distinct selectable Kazakh / Multilingual voice personas for Muza Glasses.
 * Each preset configures both Gemini Neural TTS style and local Android TTS timbre.
 */
enum class MuzaVoicePreset(
    val id: String,
    val displayName: String,
    val gender: VoiceGender,
    val roleKk: String,
    val roleRu: String,
    val roleEn: String,
    val geminiVoiceStyle: String,
    val localPitch: Float,
    val localRateFactor: Float,
    val localVoiceIndexHint: Int,
) {
    ARUZHAN(
        id = "ARUZHAN",
        displayName = "Аружан",
        gender = VoiceGender.FEMALE,
        roleKk = "Жылы әрі нәзік әйел даусы",
        roleRu = "Тёплый мягкий женский голос",
        roleEn = "Warm & friendly female voice",
        geminiVoiceStyle = "Speak with a warm, gentle, friendly female Kazakh voice",
        localPitch = 1.12f,
        localRateFactor = 0.98f,
        localVoiceIndexHint = 0,
    ),
    AIGERIM(
        id = "AIGERIM",
        displayName = "Айгерім",
        gender = VoiceGender.FEMALE,
        roleKk = "Анық студиялық диктор даусы",
        roleRu = "Чёткий студийный женский голос",
        roleEn = "Crisp studio anchor female voice",
        geminiVoiceStyle = "Speak with a crisp, articulate, professional female broadcaster voice",
        localPitch = 1.04f,
        localRateFactor = 1.03f,
        localVoiceIndexHint = 1,
    ),
    DANA(
        id = "DANA",
        displayName = "Дана",
        gender = VoiceGender.FEMALE,
        roleKk = "Сергек әрі көңілді AI көмекші",
        roleRu = "Бодрый энергичный женский голос",
        roleEn = "Upbeat & energetic assistant voice",
        geminiVoiceStyle = "Speak with a bright, cheerful, upbeat young female voice",
        localPitch = 1.22f,
        localRateFactor = 1.07f,
        localVoiceIndexHint = 2,
    ),
    NURLAN(
        id = "NURLAN",
        displayName = "Нұрлан",
        gender = VoiceGender.MALE,
        roleKk = "Сенімді әрі терең ер даусы",
        roleRu = "Уверенный глубокий мужской голос",
        roleEn = "Calm & deep confident male voice",
        geminiVoiceStyle = "Speak with a calm, deep, confident male Kazakh voice",
        localPitch = 0.78f,
        localRateFactor = 0.95f,
        localVoiceIndexHint = 0,
    ),
    ARMAN(
        id = "ARMAN",
        displayName = "Арман",
        gender = VoiceGender.MALE,
        roleKk = "Достық әрі табиғи ер даусы",
        roleRu = "Дружелюбный естественный мужской голос",
        roleEn = "Friendly conversational male voice",
        geminiVoiceStyle = "Speak with a warm, friendly, conversational male voice",
        localPitch = 0.88f,
        localRateFactor = 1.02f,
        localVoiceIndexHint = 1,
    ),
    TEMIR(
        id = "TEMIR",
        displayName = "Темір",
        gender = VoiceGender.MALE,
        roleKk = "Нақты навигатор және нұсқаушы",
        roleRu = "Чёткий голос навигатора",
        roleEn = "Clear studio navigator male voice",
        geminiVoiceStyle = "Speak with a steady, clear, crisp male navigator voice",
        localPitch = 0.72f,
        localRateFactor = 1.05f,
        localVoiceIndexHint = 2,
    );

    fun subtitle(lang: AppLanguage): String = when (lang) {
        AppLanguage.KK -> roleKk
        AppLanguage.RU -> roleRu
        AppLanguage.EN -> roleEn
    }

    fun sampleSentence(lang: AppLanguage): String = when (lang) {
        AppLanguage.KK -> "Сәлем! Мен $displayName, Muza Glasses дауыстық көмекшісімін. Көзілдірікті тағып, кез келген сұрақ қоя аласыз!"
        AppLanguage.RU -> "Привет! Я $displayName, голосовой помощник Muza Glasses. Надевайте очки и спрашивайте что угодно!"
        AppLanguage.EN -> "Hello! I am $displayName, your Muza Glasses voice assistant. Put on your glasses and ask me anything!"
    }

    companion object {
        fun fromId(raw: String?): MuzaVoicePreset =
            entries.firstOrNull { it.id.equals(raw, ignoreCase = true) } ?: ARUZHAN
    }
}

sealed interface MuzaVoiceCommand {
    data object WakeAcknowledge : MuzaVoiceCommand
    data object HowAreYou : MuzaVoiceCommand
    data object StartCamera : MuzaVoiceCommand
    data object StartVideo : MuzaVoiceCommand
    data object TakePhoto : MuzaVoiceCommand
    data object DescribeFront : MuzaVoiceCommand
    data class Translate(val textToTranslate: String?) : MuzaVoiceCommand
    data object StopRecording : MuzaVoiceCommand
    data object OpenMapAndGps : MuzaVoiceCommand
    data object WhereAmI : MuzaVoiceCommand
    data class GeneralAiQuery(val query: String) : MuzaVoiceCommand
}

data class VoiceUiState(
    val isListening: Boolean = false,
    val isAwake: Boolean = false,
    val isSpeaking: Boolean = false,
    val isGlassesAudioRouted: Boolean = false,
    val useNeuralKazakhVoice: Boolean = true,
    val activePreset: MuzaVoicePreset = MuzaVoicePreset.ARUZHAN,
    val gender: VoiceGender = VoiceGender.FEMALE,
    val speed: VoiceSpeed = VoiceSpeed.MEDIUM,
    val lastHeardText: String = "",
    val lastSpokenText: String = "",
    val statusNote: String = "",
)

class MuzaVoiceManager(
    context: Context,
    private val onCommandDetected: (MuzaVoiceCommand) -> Unit,
) {
    companion object {
        private const val TAG = "MuzaVoiceManager"
        private const val PREFS = "muza_voice_prefs"
        private const val NEURAL_TTS_TIMEOUT_MS = 14_000L
    }

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val initialPreset = MuzaVoicePreset.fromId(prefs.getString("voice_preset", MuzaVoicePreset.ARUZHAN.id))
    private val _state = MutableStateFlow(
        VoiceUiState(
            useNeuralKazakhVoice = prefs.getBoolean("use_neural_tts", true),
            activePreset = initialPreset,
            gender = initialPreset.gender,
            speed = VoiceSpeed.fromCode(prefs.getString("voice_speed", VoiceSpeed.MEDIUM.code)),
        )
    )
    val state: StateFlow<VoiceUiState> = _state.asStateFlow()

    private val neuralTtsModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.1-flash-tts-preview",
            generationConfig = generationConfig {
                responseModalities = listOf(ResponseModality.AUDIO)
            },
        )
    }

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var hasNativeKazakhTts = false
    private var speechRecognizer: SpeechRecognizer? = null
    private var continuousMode = false
    private var awaitingFollowUp = false
    private var speakJob: Job? = null
    private var activeAudioTrack: AudioTrack? = null
    private var activeMediaPlayer: MediaPlayer? = null

    init {
        initTts()
    }

    private fun initTts() {
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _state.update { it.copy(isSpeaking = true) }
                    }

                    override fun onDone(utteranceId: String?) {
                        onSpeechPlaybackFinished()
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        onSpeechPlaybackFinished()
                    }
                })
                applyVoiceConfiguration()
            } else {
                Log.w(TAG, "TextToSpeech init failed with status=$status")
            }
        }
    }

    /**
     * Routes microphone & speaker audio to connected Ray-Ban / Meta Bluetooth glasses (HFP/SCO/BLE)
     * so speaking "Муза" while wearing the glasses listens & replies directly through the glasses.
     */
    fun setGlassesAudioRouting(enable: Boolean) {
        val am = audioManager ?: return
        runCatching {
            if (enable) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val btDevice = am.availableCommunicationDevices.firstOrNull { device ->
                        device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                            device.type == AudioDeviceInfo.TYPE_BLE_HEADSET
                    }
                    if (btDevice != null) {
                        am.mode = AudioManager.MODE_IN_COMMUNICATION
                        val ok = am.setCommunicationDevice(btDevice)
                        _state.update { it.copy(isGlassesAudioRouted = ok) }
                        Log.i(TAG, "Routed audio to Bluetooth glasses: ${btDevice.productName} (ok=$ok)")
                    } else {
                        _state.update { it.copy(isGlassesAudioRouted = false) }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    if (am.isBluetoothScoAvailableOffCall) {
                        am.mode = AudioManager.MODE_IN_COMMUNICATION
                        am.startBluetoothSco()
                        am.isBluetoothScoOn = true
                        _state.update { it.copy(isGlassesAudioRouted = true) }
                    }
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    am.clearCommunicationDevice()
                } else {
                    @Suppress("DEPRECATION")
                    am.stopBluetoothSco()
                    @Suppress("DEPRECATION")
                    am.isBluetoothScoOn = false
                }
                am.mode = AudioManager.MODE_NORMAL
                _state.update { it.copy(isGlassesAudioRouted = false) }
            }
        }.onFailure { e ->
            Log.w(TAG, "Glasses Bluetooth audio routing skipped: ${e.message}")
        }
    }

    fun setVoicePreset(preset: MuzaVoicePreset) {
        prefs.edit()
            .putString("voice_preset", preset.id)
            .putString("voice_gender", preset.gender.code)
            .apply()
        _state.update { it.copy(activePreset = preset, gender = preset.gender) }
        applyVoiceConfiguration()
    }

    fun setVoiceGender(gender: VoiceGender) {
        val currentPreset = _state.value.activePreset
        val matchingPreset = if (currentPreset.gender == gender) {
            currentPreset
        } else {
            MuzaVoicePreset.entries.first { it.gender == gender }
        }
        setVoicePreset(matchingPreset)
    }

    fun setVoiceSpeed(speed: VoiceSpeed) {
        prefs.edit().putString("voice_speed", speed.code).apply()
        _state.update { it.copy(speed = speed) }
        applyVoiceConfiguration()
    }

    fun setUseNeuralKazakhVoice(enabled: Boolean) {
        prefs.edit().putBoolean("use_neural_tts", enabled).apply()
        _state.update { it.copy(useNeuralKazakhVoice = enabled) }
    }

    fun syncLanguage() {
        applyVoiceConfiguration()
        if (continuousMode && _state.value.isListening) {
            restartListeningDelayed(350L)
        }
    }

    private fun applyVoiceConfiguration() {
        val engine = tts ?: return
        if (!ttsReady) return
        val lang = LanguageManager.currentLanguage
        val preset = _state.value.activePreset

        val result = engine.setLanguage(lang.ttsLocale)
        hasNativeKazakhTts = (lang == AppLanguage.KK) &&
            result != TextToSpeech.LANG_MISSING_DATA &&
            result != TextToSpeech.LANG_NOT_SUPPORTED

        val targetLocale = when {
            result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED -> lang.ttsLocale
            lang == AppLanguage.KK -> Locale("ru", "RU").also { engine.setLanguage(it) }
            else -> Locale.US.also { engine.setLanguage(it) }
        }

        // Select distinct installed voice matching gender & persona hint whenever available
        runCatching {
            val matchingVoices = engine.voices.orEmpty()
                .filter { v ->
                    !v.isNetworkConnectionRequired &&
                        v.locale.language.equals(targetLocale.language, ignoreCase = true)
                }
                .sortedBy { it.name }

            val genderFiltered = matchingVoices.filter { v ->
                val lower = v.name.lowercase(Locale.ROOT)
                if (preset.gender == VoiceGender.FEMALE) {
                    lower.contains("female") || lower.contains("-f") || lower.contains("sfb") ||
                        lower.contains("x-ruf") || lower.contains("ruc") || lower.contains("rue")
                } else {
                    lower.contains("male") || lower.contains("-m") || lower.contains("smb") ||
                        lower.contains("x-rud") || lower.contains("rub")
                }
            }
            val pool = genderFiltered.ifEmpty { matchingVoices }
            if (pool.isNotEmpty()) {
                val chosen = pool[preset.localVoiceIndexHint % pool.size]
                engine.voice = chosen
            }
        }

        val combinedRate = (_state.value.speed.rate * preset.localRateFactor).coerceIn(0.65f, 1.55f)
        engine.setSpeechRate(combinedRate)
        engine.setPitch(preset.localPitch)
    }

    fun speak(
        text: String,
        force: Boolean = false,
        overridePreset: MuzaVoicePreset? = null,
    ) {
        if (!force && !MuzaFeatures.voiceSpokenReply) return
        val cleanText = text.replace(Regex("[*_#`~]"), "").trim()
        if (cleanText.isBlank()) return

        val preset = overridePreset ?: _state.value.activePreset
        val lang = LanguageManager.currentLanguage

        // Pause mic briefly while speaking so Muza never self-triggers on its own reply
        mainHandler.post {
            runCatching {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
            }
        }
        stopActivePlayback()
        _state.update { it.copy(isSpeaking = true, isListening = false, lastSpokenText = cleanText) }

        val shouldTryNeural = _state.value.useNeuralKazakhVoice && cleanText.length <= 420
        if (shouldTryNeural) {
            speakJob = ioScope.launch {
                val playedNeural = trySpeakWithGeminiNeuralTts(cleanText, preset, lang)
                if (!playedNeural) {
                    speakWithLocalTts(cleanText, preset, lang)
                } else {
                    onSpeechPlaybackFinished()
                }
            }
        } else {
            speakWithLocalTts(cleanText, preset, lang)
        }
    }

    private suspend fun trySpeakWithGeminiNeuralTts(
        text: String,
        preset: MuzaVoicePreset,
        lang: AppLanguage,
    ): Boolean {
        return try {
            val langLabel = when (lang) {
                AppLanguage.KK -> "authentic native Kazakh (Қазақ тілі)"
                AppLanguage.RU -> "natural Russian"
                AppLanguage.EN -> "natural English"
            }
            val speedHint = when (_state.value.speed) {
                VoiceSpeed.SLOW -> "at a calm slow pace"
                VoiceSpeed.MEDIUM -> "at a natural conversational pace"
                VoiceSpeed.FAST -> "at a brisk lively pace"
            }
            val prompt = "${preset.geminiVoiceStyle} in $langLabel $speedHint. " +
                "Read ONLY the following text aloud naturally without adding anything:\n$text"

            val response = withTimeout(NEURAL_TTS_TIMEOUT_MS) {
                neuralTtsModel.generateContent(content { text(prompt) })
            }
            val audioPart = response.candidates.firstOrNull()
                ?.content?.parts
                ?.filterIsInstance<InlineDataPart>()
                ?.firstOrNull { it.mimeType.startsWith("audio/", ignoreCase = true) }
                ?: return false

            playAudioBytes(audioPart.inlineData, audioPart.mimeType)
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Gemini neural TTS fallback to local TTS: ${e.message}")
            false
        }
    }

    private suspend fun playAudioBytes(bytes: ByteArray, mimeType: String) = withContext(Dispatchers.IO) {
        if (bytes.isEmpty()) return@withContext
        val lowerMime = mimeType.lowercase(Locale.ROOT)
        if (lowerMime.contains("pcm") || lowerMime.contains("l16")) {
            val rateMatch = Regex("rate=(\\d+)").find(lowerMime)
            val sampleRate = rateMatch?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 24_000
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
            ).coerceAtLeast(4096)

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBuf)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            activeAudioTrack = track
            try {
                track.play()
                var offset = 0
                val chunkSize = 4096
                while (offset < bytes.size) {
                    val len = minOf(chunkSize, bytes.size - offset)
                    val written = track.write(bytes, offset, len)
                    if (written <= 0) break
                    offset += written
                }
                track.stop()
            } finally {
                runCatching { track.release() }
                if (activeAudioTrack === track) activeAudioTrack = null
            }
        } else {
            val ext = when {
                lowerMime.contains("wav") -> "wav"
                lowerMime.contains("ogg") -> "ogg"
                else -> "mp3"
            }
            val tempFile = File(appContext.cacheDir, "muza_tts_${System.currentTimeMillis()}.$ext")
            FileOutputStream(tempFile).use { it.write(bytes) }
            val latch = kotlinx.coroutines.CompletableDeferred<Unit>()
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(tempFile.absolutePath)
                setOnCompletionListener { latch.complete(Unit) }
                setOnErrorListener { _, _, _ ->
                    latch.complete(Unit)
                    true
                }
                prepare()
                start()
            }
            activeMediaPlayer = player
            try {
                latch.await()
            } finally {
                runCatching { player.release() }
                if (activeMediaPlayer === player) activeMediaPlayer = null
                runCatching { tempFile.delete() }
            }
        }
    }

    private fun speakWithLocalTts(text: String, preset: MuzaVoicePreset, lang: AppLanguage) {
        val speakable = if (lang == AppLanguage.KK && !hasNativeKazakhTts) {
            normalizeKazakhForCyrillicTts(text)
        } else {
            text
        }
        mainHandler.post {
            val engine = tts
            if (engine == null || !ttsReady) {
                onSpeechPlaybackFinished()
                return@post
            }
            applyVoiceConfiguration()
            engine.setPitch(preset.localPitch)
            engine.setSpeechRate((_state.value.speed.rate * preset.localRateFactor).coerceIn(0.65f, 1.55f))
            val uttId = "muza_utt_${System.currentTimeMillis()}"
            engine.speak(speakable, TextToSpeech.QUEUE_FLUSH, null, uttId)
        }
    }

    /**
     * When an Android phone lacks a native kk-KZ TTS pack and falls back to ru-RU,
     * mapping uniquely Kazakh Cyrillic glyphs ensures every syllable is pronounced clearly
     * instead of skipped by the Russian synthesizer.
     */
    private fun normalizeKazakhForCyrillicTts(input: String): String =
        input
            .replace('Ә', 'А').replace('ә', 'а')
            .replace('Ғ', 'Г').replace('ғ', 'г')
            .replace('Қ', 'К').replace('қ', 'к')
            .replace('Ң', 'Н').replace('ң', 'н')
            .replace('Ө', 'О').replace('ө', 'о')
            .replace('Ұ', 'У').replace('ұ', 'у')
            .replace('Ү', 'У').replace('ү', 'у')
            .replace('Һ', 'Х').replace('һ', 'х')
            .replace('І', 'И').replace('і', 'и')

    private fun onSpeechPlaybackFinished() {
        _state.update { it.copy(isSpeaking = false) }
        if (continuousMode && MuzaFeatures.voiceWakeWordMuza) {
            restartListeningDelayed(380L)
        }
    }

    private fun stopActivePlayback() {
        speakJob?.cancel()
        speakJob = null
        runCatching { tts?.stop() }
        runCatching {
            activeAudioTrack?.stop()
            activeAudioTrack?.release()
        }
        activeAudioTrack = null
        runCatching {
            activeMediaPlayer?.stop()
            activeMediaPlayer?.release()
        }
        activeMediaPlayer = null
    }

    fun startWakeWordListening() {
        if (!MuzaFeatures.voiceWakeWordMuza) {
            _state.update { it.copy(isListening = false, statusNote = LanguageManager.currentStrings.wakeWordPaused) }
            return
        }
        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            _state.update { it.copy(isListening = false, statusNote = "RECORD_AUDIO required") }
            return
        }
        continuousMode = true
        mainHandler.post { startRecognizerSession() }
    }

    fun stopWakeWordListening() {
        continuousMode = false
        awaitingFollowUp = false
        mainHandler.post {
            runCatching {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
            }
            _state.update { it.copy(isListening = false, isAwake = false) }
        }
    }

    fun triggerSingleCommandDictation() {
        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        stopActivePlayback()
        awaitingFollowUp = true
        _state.update { it.copy(isAwake = true, isSpeaking = false) }
        mainHandler.post { startRecognizerSession() }
    }

    private fun startRecognizerSession() {
        if (_state.value.isSpeaking) return
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            _state.update { it.copy(isListening = false, statusNote = "SpeechRecognizer unavailable") }
            return
        }
        try {
            speechRecognizer?.destroy()
            val recognizer = SpeechRecognizer.createSpeechRecognizer(appContext)
            speechRecognizer = recognizer
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _state.update { it.copy(isListening = true, statusNote = LanguageManager.currentStrings.wakeWordListening) }
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}

                override fun onError(error: Int) {
                    _state.update { it.copy(isListening = false) }
                    if (continuousMode && MuzaFeatures.voiceWakeWordMuza && !_state.value.isSpeaking) {
                        val backoffMs = if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) 1200L else 600L
                        restartListeningDelayed(backoffMs)
                    }
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                    val best = matches.firstOrNull().orEmpty().trim()
                    if (best.isNotBlank()) {
                        processTranscript(best)
                    }
                    if (continuousMode && MuzaFeatures.voiceWakeWordMuza && !_state.value.isSpeaking) {
                        restartListeningDelayed(550L)
                    } else {
                        _state.update { it.copy(isListening = false) }
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                    if (partial.isNotBlank()) {
                        _state.update { it.copy(lastHeardText = partial) }
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, LanguageManager.currentLanguage.speechLocaleTag)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, LanguageManager.currentLanguage.speechLocaleTag)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            recognizer.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start SpeechRecognizer", e)
            _state.update { it.copy(isListening = false) }
        }
    }

    private fun restartListeningDelayed(delayMs: Long) {
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.postDelayed({
            if (continuousMode && MuzaFeatures.voiceWakeWordMuza && !_state.value.isSpeaking) {
                startRecognizerSession()
            }
        }, delayMs)
    }

    fun processTranscript(rawText: String, bypassWakeWord: Boolean = false) {
        val cleaned = rawText.trim()
        if (cleaned.isEmpty()) return
        val lower = cleaned.lowercase(Locale.ROOT)
        _state.update { it.copy(lastHeardText = cleaned) }

        val wakeVariants = listOf("muza", "муза", "мұза", "муса", "musa", "моза", "музза")
        val containsWakeWord = wakeVariants.any { lower.contains(it) }

        if (!containsWakeWord && !awaitingFollowUp && !bypassWakeWord) {
            return
        }

        // Strip wake word & polite greeting prefixes
        var stripped = lower
        wakeVariants.forEach { w -> stripped = stripped.replace(w, "") }
        stripped = stripped
            .replace("эй ", "")
            .replace("хей ", "")
            .replace("hey ", "")
            .replace("сәлем", "")
            .replace("салем", "")
            .trim(',', '.', ' ', '!', '?')

        if (stripped.isEmpty()) {
            awaitingFollowUp = true
            _state.update { it.copy(isAwake = true) }
            speak(LanguageManager.currentStrings.wakeResponse)
            onCommandDetected(MuzaVoiceCommand.WakeAcknowledge)
            return
        }

        awaitingFollowUp = false
        _state.update { it.copy(isAwake = true) }

        val cleanOriginalQuery = cleaned
            .replace(Regex("(?i)\\b(эй|хей|hey|muza|муза|мұза|муса|musa|моза)\\b"), "")
            .trim(',', ' ', '!', '?')
            .ifBlank { cleaned }

        val command = parseCommand(stripped, cleanOriginalQuery)
        onCommandDetected(command)
    }

    private fun parseCommand(lower: String, cleanQuery: String): MuzaVoiceCommand = when {
        lower.contains("қалайсың") || lower.contains("калайсын") ||
            lower.contains("халің қалай") || lower.contains("как дела") ||
            lower.contains("how are you") ->
            MuzaVoiceCommand.HowAreYou

        lower.contains("алдымда") || lower.contains("не тұр") || lower.contains("не тур") ||
            lower.contains("не көріп") || lower.contains("не корип") || lower.contains("көргеніңді айт") ||
            lower.contains("передо мной") || lower.contains("что видишь") ||
            lower.contains("in front") || lower.contains("what do you see") ->
            MuzaVoiceCommand.DescribeFront

        lower.contains("аудар") || lower.contains("перевед") || lower.contains("перевод") ||
            lower.contains("translate") -> {
            val payload = cleanQuery
                .replace(Regex("(?i)(мынаны аудар|сөзді аудар|аудар|переведи|переводта|перевод|translate)"), "")
                .trim(',', ' ', ':', '?')
                .ifBlank { null }
            MuzaVoiceCommand.Translate(payload)
        }

        lower.contains("қайдамын") || lower.contains("кайдамын") ||
            lower.contains("қайда жүр") || lower.contains("кайда жур") ||
            lower.contains("где я") || lower.contains("where am i") ->
            MuzaVoiceCommand.WhereAmI

        lower.contains("gps") || lower.contains("джипиэс") ||
            lower.contains("карта") || lower.contains("навигац") ||
            lower.contains("маршрут") || lower.contains("map") ->
            MuzaVoiceCommand.OpenMapAndGps

        lower.contains("видео") || lower.contains("бейне") || lower.contains("video") || lower.contains("запиш") ->
            MuzaVoiceCommand.StartVideo

        lower.contains("камера") || lower.contains("camera") ->
            MuzaVoiceCommand.StartCamera

        lower.contains("фото") || lower.contains("сурет") || lower.contains("photo") || lower.contains("picture") ->
            MuzaVoiceCommand.TakePhoto

        lower.contains("тоқта") || lower.contains("токта") || lower.contains("стоп") || lower.contains("stop") ->
            MuzaVoiceCommand.StopRecording

        else -> MuzaVoiceCommand.GeneralAiQuery(cleanQuery)
    }

    fun release() {
        stopWakeWordListening()
        stopActivePlayback()
        setGlassesAudioRouting(false)
        mainHandler.post {
            runCatching { speechRecognizer?.destroy() }
            speechRecognizer = null
        }
        runCatching { tts?.shutdown() }
        tts = null
    }
}
