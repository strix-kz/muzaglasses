package com.rake.muza.core

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
    SLOW("SLOW", 0.78f),
    MEDIUM("MEDIUM", 1.0f),
    FAST("FAST", 1.28f);

    companion object {
        fun fromCode(raw: String?): VoiceSpeed =
            entries.firstOrNull { it.code.equals(raw, ignoreCase = true) } ?: MEDIUM
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
    data class GeneralAiQuery(val query: String) : MuzaVoiceCommand
}

data class VoiceUiState(
    val isListening: Boolean = false,
    val isAwake: Boolean = false,
    val isSpeaking: Boolean = false,
    val gender: VoiceGender = VoiceGender.FEMALE,
    val speed: VoiceSpeed = VoiceSpeed.MEDIUM,
    val lastHeardText: String = "",
    val lastSpokenText: String = "",
    val statusNote: String = "",
)

class MuzaVoiceManager(
    private val context: Context,
    private val onCommandDetected: (MuzaVoiceCommand) -> Unit,
) {
    companion object {
        private const val TAG = "MuzaVoiceManager"
        private const val PREFS = "muza_voice_prefs"
    }

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(
        VoiceUiState(
            gender = VoiceGender.fromCode(prefs.getString("voice_gender", VoiceGender.FEMALE.code)),
            speed = VoiceSpeed.fromCode(prefs.getString("voice_speed", VoiceSpeed.MEDIUM.code)),
        )
    )
    val state: StateFlow<VoiceUiState> = _state.asStateFlow()

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var speechRecognizer: SpeechRecognizer? = null
    private var continuousMode = false
    private var awaitingFollowUp = false

    init {
        initTts()
    }

    private fun initTts() {
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
                applyVoiceConfiguration()
            } else {
                Log.w(TAG, "TextToSpeech init failed with status=$status")
            }
        }
    }

    fun setVoiceGender(gender: VoiceGender) {
        prefs.edit().putString("voice_gender", gender.code).apply()
        _state.update { it.copy(gender = gender) }
        applyVoiceConfiguration()
    }

    fun setVoiceSpeed(speed: VoiceSpeed) {
        prefs.edit().putString("voice_speed", speed.code).apply()
        _state.update { it.copy(speed = speed) }
        applyVoiceConfiguration()
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
        val result = engine.setLanguage(lang.ttsLocale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Fallback for devices without kk-KZ TTS pack: ru-RU reads Cyrillic clearly
            val fallback = if (lang == AppLanguage.KK) Locale("ru", "RU") else Locale.US
            engine.setLanguage(fallback)
        }
        val current = _state.value
        engine.setSpeechRate(current.speed.rate)
        engine.setPitch(if (current.gender == VoiceGender.FEMALE) 1.12f else 0.82f)
    }

    fun speak(text: String, force: Boolean = false) {
        if (!force && !MuzaFeatures.voiceSpokenReply) return
        if (text.isBlank()) return
        _state.update { it.copy(isSpeaking = true, lastSpokenText = text) }
        mainHandler.post {
            applyVoiceConfiguration()
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "muza_utterance_${System.currentTimeMillis()}")
            mainHandler.postDelayed({
                _state.update { it.copy(isSpeaking = false) }
            }, (text.length * 55L).coerceIn(1200L, 6500L))
        }
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
        awaitingFollowUp = true
        _state.update { it.copy(isAwake = true) }
        mainHandler.post { startRecognizerSession() }
    }

    private fun startRecognizerSession() {
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
                    if (continuousMode && MuzaFeatures.voiceWakeWordMuza) {
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
                    if (continuousMode && MuzaFeatures.voiceWakeWordMuza) {
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
            if (continuousMode && MuzaFeatures.voiceWakeWordMuza) {
                startRecognizerSession()
            }
        }, delayMs)
    }

    fun processTranscript(rawText: String) {
        val cleaned = rawText.trim()
        val lower = cleaned.lowercase(Locale.ROOT)
        _state.update { it.copy(lastHeardText = cleaned) }

        val containsWakeWord = lower.contains("muza") ||
            lower.contains("муза") ||
            lower.contains("муса") ||
            lower.contains("musa")

        if (!containsWakeWord && !awaitingFollowUp) {
            return
        }

        // Strip wake word prefix if present
        val stripped = lower
            .replace("muza", "")
            .replace("муза", "")
            .replace("муса", "")
            .replace("musa", "")
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

        val command = parseCommand(stripped, cleaned)
        onCommandDetected(command)
    }

    private fun parseCommand(lower: String, original: String): MuzaVoiceCommand = when {
        lower.contains("қалайсың") || lower.contains("калайсын") ||
            lower.contains("как дела") || lower.contains("how are you") ->
            MuzaVoiceCommand.HowAreYou

        lower.contains("камера") || lower.contains("camera") ->
            MuzaVoiceCommand.StartCamera

        lower.contains("видео") || lower.contains("video") || lower.contains("запиш") ->
            MuzaVoiceCommand.StartVideo

        lower.contains("фото") || lower.contains("сурет") || lower.contains("photo") || lower.contains("picture") ->
            MuzaVoiceCommand.TakePhoto

        lower.contains("алдымда") || lower.contains("не тұр") || lower.contains("не көріп") ||
            lower.contains("передо мной") || lower.contains("что видишь") ||
            lower.contains("in front") || lower.contains("what do you see") ->
            MuzaVoiceCommand.DescribeFront

        lower.contains("аудар") || lower.contains("перевед") || lower.contains("translate") -> {
            val payload = original
                .replace(Regex("(?i)(muza|муза|мынаны аудар|аудар|переведи|translate)"), "")
                .trim(',', ' ', ':')
                .ifBlank { null }
            MuzaVoiceCommand.Translate(payload)
        }

        lower.contains("тоқта") || lower.contains("токта") || lower.contains("стоп") || lower.contains("stop") ->
            MuzaVoiceCommand.StopRecording

        else -> MuzaVoiceCommand.GeneralAiQuery(original)
    }

    fun release() {
        stopWakeWordListening()
        mainHandler.post {
            runCatching { speechRecognizer?.destroy() }
            speechRecognizer = null
        }
        runCatching {
            tts?.stop()
            tts?.shutdown()
        }
        tts = null
    }
}
