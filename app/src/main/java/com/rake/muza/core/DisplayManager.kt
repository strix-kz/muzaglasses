package com.rake.muza.core

import android.os.Build
import android.util.Log
import com.meta.wearable.dat.core.session.DeviceSession
import com.meta.wearable.dat.display.Display
import com.meta.wearable.dat.display.addDisplay
import com.meta.wearable.dat.display.removeDisplay
import com.meta.wearable.dat.display.views.Direction
import com.meta.wearable.dat.display.views.FlexBoxBackground
import com.meta.wearable.dat.display.views.TextColor
import com.meta.wearable.dat.display.views.TextStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class DisplayHudType {
    IDLE,
    TEXT,
    NAVIGATION,
    TRANSLATION,
    AI_ANSWER,
    MESSAGE,
    RECORDING,
    PHOTO_CAPTURED,
}

data class DisplayHudPayload(
    val type: DisplayHudType = DisplayHudType.IDLE,
    val header: String = "",
    val primaryLine: String = "",
    val secondaryLine: String = "",
    val footerMeta: String = "",
    val isVisible: Boolean = false,
)

/**
 * Meta Display Glasses HUD Controller.
 * Renders compact, low-strain corner overlays (short text + arrow + icon, never full videos)
 * both on the phone's live HUD mirror and on connected Meta Display Glasses via DAT Display SDK.
 */
class DisplayManager {
    companion object {
        private const val TAG = "MuzaDisplayManager"
        private const val AI_AUTO_HIDE_MS = 5_000L
        private const val PHOTO_AUTO_HIDE_MS = 3_500L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _hudState = MutableStateFlow(DisplayHudPayload())
    val hudState: StateFlow<DisplayHudPayload> = _hudState.asStateFlow()

    private var activeSession: DeviceSession? = null
    private var attachedDisplay: Display? = null
    private var autoHideJob: Job? = null

    fun attachToSession(session: DeviceSession?) {
        if (session == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        activeSession = session
        runCatching {
            session.addDisplay().onSuccess { display ->
                attachedDisplay = display
                Log.i(TAG, "DAT Display attached successfully")
            }.onFailure { err, _ ->
                Log.w(TAG, "DAT Display attach skipped (non-display model or mock): ${err.description}")
            }
        }
    }

    fun detach() {
        autoHideJob?.cancel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching { activeSession?.removeDisplay() }
        }
        attachedDisplay = null
        activeSession = null
    }

    fun showText(title: String, message: String, autoHideMs: Long? = AI_AUTO_HIDE_MS) {
        val payload = DisplayHudPayload(
            type = DisplayHudType.TEXT,
            header = title,
            primaryLine = message.take(120),
            isVisible = true,
        )
        publish(payload, autoHideMs)
    }

    fun showNavigation(
        locationTitle: String = "Alinda Almaty • Abay koshesi",
        directionText: String = "→ 200 m right • 4 min",
        batteryPercent: Int = 78,
    ) {
        if (!MuzaFeatures.displayNavigation) return
        val batteryMeta = "${LanguageManager.currentStrings.batteryLabel} $batteryPercent%"
        val payload = DisplayHudPayload(
            type = DisplayHudType.NAVIGATION,
            header = "🧭 $locationTitle",
            primaryLine = directionText,
            footerMeta = batteryMeta,
            isVisible = true,
        )
        publish(payload, autoHideMs = null)
    }

    fun showTranslation(
        originalText: String,
        translatedText: String,
        languagePair: String = "EN → KK",
    ) {
        if (!MuzaFeatures.displayTranslation) return
        val payload = DisplayHudPayload(
            type = DisplayHudType.TRANSLATION,
            header = "Live Translation • $languagePair",
            primaryLine = "Original: ${originalText.take(64)}",
            secondaryLine = "Translated: ${translatedText.take(80)}",
            isVisible = true,
        )
        publish(payload, autoHideMs = 8_000L)
    }

    fun showAiResponse(shortSummary: String) {
        if (!MuzaFeatures.displayAiAnswer) return
        val payload = DisplayHudPayload(
            type = DisplayHudType.AI_ANSWER,
            header = "✨ Muza AI",
            primaryLine = shortSummary.take(110),
            footerMeta = "5s HUD",
            isVisible = true,
        )
        publish(payload, autoHideMs = AI_AUTO_HIDE_MS)
    }

    fun showMessage(sender: String, body: String, appName: String = "WhatsApp") {
        if (!MuzaFeatures.displayNotifications) return
        val payload = DisplayHudPayload(
            type = DisplayHudType.MESSAGE,
            header = "💬 $appName • $sender",
            primaryLine = body.take(96),
            isVisible = true,
        )
        publish(payload, autoHideMs = 6_000L)
    }

    fun showRecordingStatus(formattedTime: String) {
        val label = when (LanguageManager.currentLanguage) {
            AppLanguage.KK -> "🔴 Видео жазылып жатыр: $formattedTime"
            AppLanguage.RU -> "🔴 Идет запись видео: $formattedTime"
            AppLanguage.EN -> "🔴 Recording video: $formattedTime"
        }
        val payload = DisplayHudPayload(
            type = DisplayHudType.RECORDING,
            header = "LIVE REC",
            primaryLine = label,
            isVisible = true,
        )
        publish(payload, autoHideMs = null)
    }

    fun showPhotoCaptured() {
        val label = when (LanguageManager.currentLanguage) {
            AppLanguage.KK -> "📸 Фото түсірілді!"
            AppLanguage.RU -> "📸 Фото снято!"
            AppLanguage.EN -> "📸 Photo captured!"
        }
        val payload = DisplayHudPayload(
            type = DisplayHudType.PHOTO_CAPTURED,
            header = "Muza Camera",
            primaryLine = label,
            isVisible = true,
        )
        publish(payload, autoHideMs = PHOTO_AUTO_HIDE_MS)
    }

    fun clear() {
        autoHideJob?.cancel()
        _hudState.value = DisplayHudPayload(isVisible = false)
    }

    private fun publish(payload: DisplayHudPayload, autoHideMs: Long?) {
        autoHideJob?.cancel()
        _hudState.value = payload
        pushToDatDisplay(payload)

        if (autoHideMs != null) {
            autoHideJob = scope.launch {
                delay(autoHideMs)
                _hudState.value = DisplayHudPayload(isVisible = false)
            }
        }
    }

    private fun pushToDatDisplay(payload: DisplayHudPayload) {
        val display = attachedDisplay ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        scope.launch {
            runCatching {
                display.sendContent {
                    flexBox(direction = Direction.COLUMN, gap = 8, padding = 16, background = FlexBoxBackground.CARD) {
                        if (payload.header.isNotBlank()) {
                            text(payload.header, style = TextStyle.META, color = TextColor.SECONDARY)
                        }
                        if (payload.primaryLine.isNotBlank()) {
                            text(payload.primaryLine, style = TextStyle.BODY)
                        }
                        if (payload.secondaryLine.isNotBlank()) {
                            text(payload.secondaryLine, style = TextStyle.BODY)
                        }
                        if (payload.footerMeta.isNotBlank()) {
                            text(payload.footerMeta, style = TextStyle.META, color = TextColor.SECONDARY)
                        }
                    }
                }
            }.onFailure { e ->
                Log.w(TAG, "DAT sendContent skipped: ${e.message}")
            }
        }
    }
}
