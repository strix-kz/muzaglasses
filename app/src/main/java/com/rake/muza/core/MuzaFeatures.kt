package com.rake.muza.core

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class MuzaFeatureSnapshot(
    // Camera modules
    val cameraPhoto: Boolean = true,
    val cameraVideo: Boolean = true,
    val cameraLivePreview: Boolean = true,
    // Voice modules
    val voiceWakeWordMuza: Boolean = true,
    val voiceSpokenReply: Boolean = true,
    // Display HUD modules
    val displayTranslation: Boolean = true,
    val displayNavigation: Boolean = true,
    val displayNotifications: Boolean = true,
    val displayTime: Boolean = true,
    val displayAiAnswer: Boolean = true,
    // AI modules
    val aiVisionDescribe: Boolean = true,
    val aiTranslation: Boolean = true,
    val aiConversation: Boolean = true,
    // Glasses compatibility module
    val glassesAllRayBanVersions: Boolean = true,
)

/**
 * Modular feature registry for Muza Glasses.
 * Every capability is ready out of the box and executes only when its flag is true.
 */
object MuzaFeatures {
    private const val PREFS_NAME = "muza_features_prefs"
    private var prefs: SharedPreferences? = null

    private val _state = MutableStateFlow(MuzaFeatureSnapshot())
    val state: StateFlow<MuzaFeatureSnapshot> = _state.asStateFlow()

    val cameraPhoto: Boolean get() = _state.value.cameraPhoto
    val cameraVideo: Boolean get() = _state.value.cameraVideo
    val cameraLivePreview: Boolean get() = _state.value.cameraLivePreview

    val voiceWakeWordMuza: Boolean get() = _state.value.voiceWakeWordMuza
    val voiceSpokenReply: Boolean get() = _state.value.voiceSpokenReply

    val displayTranslation: Boolean get() = _state.value.displayTranslation
    val displayNavigation: Boolean get() = _state.value.displayNavigation
    val displayNotifications: Boolean get() = _state.value.displayNotifications
    val displayTime: Boolean get() = _state.value.displayTime
    val displayAiAnswer: Boolean get() = _state.value.displayAiAnswer

    val aiVisionDescribe: Boolean get() = _state.value.aiVisionDescribe
    val aiTranslation: Boolean get() = _state.value.aiTranslation
    val aiConversation: Boolean get() = _state.value.aiConversation

    val glassesAllRayBanVersions: Boolean get() = _state.value.glassesAllRayBanVersions

    fun init(context: Context) {
        val sp = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = sp
        _state.value = MuzaFeatureSnapshot(
            cameraPhoto = sp.getBoolean("camera_photo", true),
            cameraVideo = sp.getBoolean("camera_video", true),
            cameraLivePreview = sp.getBoolean("camera_live", true),
            voiceWakeWordMuza = sp.getBoolean("voice_wake_word", true),
            voiceSpokenReply = sp.getBoolean("voice_spoken_reply", true),
            displayTranslation = sp.getBoolean("display_translation", true),
            displayNavigation = sp.getBoolean("display_navigation", true),
            displayNotifications = sp.getBoolean("display_notifications", true),
            displayTime = sp.getBoolean("display_time", true),
            displayAiAnswer = sp.getBoolean("display_ai_answer", true),
            aiVisionDescribe = sp.getBoolean("ai_vision_describe", true),
            aiTranslation = sp.getBoolean("ai_translation", true),
            aiConversation = sp.getBoolean("ai_conversation", true),
            glassesAllRayBanVersions = sp.getBoolean("glasses_all_rayban", true),
        )
    }

    fun update(transform: (MuzaFeatureSnapshot) -> MuzaFeatureSnapshot) {
        _state.update(transform)
        persist(_state.value)
    }

    private fun persist(s: MuzaFeatureSnapshot) {
        prefs?.edit()
            ?.putBoolean("camera_photo", s.cameraPhoto)
            ?.putBoolean("camera_video", s.cameraVideo)
            ?.putBoolean("camera_live", s.cameraLivePreview)
            ?.putBoolean("voice_wake_word", s.voiceWakeWordMuza)
            ?.putBoolean("voice_spoken_reply", s.voiceSpokenReply)
            ?.putBoolean("display_translation", s.displayTranslation)
            ?.putBoolean("display_navigation", s.displayNavigation)
            ?.putBoolean("display_notifications", s.displayNotifications)
            ?.putBoolean("display_time", s.displayTime)
            ?.putBoolean("display_ai_answer", s.displayAiAnswer)
            ?.putBoolean("ai_vision_describe", s.aiVisionDescribe)
            ?.putBoolean("ai_translation", s.aiTranslation)
            ?.putBoolean("ai_conversation", s.aiConversation)
            ?.putBoolean("glasses_all_rayban", s.glassesAllRayBanVersions)
            ?.apply()
    }
}
