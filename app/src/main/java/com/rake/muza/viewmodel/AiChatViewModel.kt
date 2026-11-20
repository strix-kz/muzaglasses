package com.rake.muza.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestoreException
import com.rake.muza.core.LanguageManager
import com.rake.muza.core.MuzaFeatures
import com.rake.muza.data.ai.MuzaAiRepository
import com.rake.muza.data.firebase.AiChatRecord
import com.rake.muza.data.firebase.MuzaCloudRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatMessageUi(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val hasAttachment: Boolean = false,
    val timestampLabel: String = "",
)

data class AiChatUiState(
    val messages: List<ChatMessageUi> = emptyList(),
    val streamingReply: String = "",
    val isGenerating: Boolean = false,
    val attachedBitmap: Bitmap? = null,
    val errorMessage: String? = null,
)

class AiChatViewModel(
    private val currentUserId: String,
    private val aiRepository: MuzaAiRepository,
    private val cloudRepository: MuzaCloudRepository,
) : ViewModel() {

    companion object {
        private const val TAG = "AiChatViewModel"
    }

    private val _state = MutableStateFlow(AiChatUiState())
    val state: StateFlow<AiChatUiState> = _state.asStateFlow()

    private var activeStreamJob: Job? = null

    init {
        observeCloudHistory()
    }

    private fun observeCloudHistory() {
        viewModelScope.launch {
            cloudRepository.observeAiMessages(currentUserId)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    Log.w(TAG, "AI history observer warning: ${firestoreError?.message ?: error.message}")
                    _state.update {
                        it.copy(errorMessage = firestoreError?.localizedMessage ?: error.localizedMessage)
                    }
                }
                .collect { records: List<AiChatRecord> ->
                    val mapped = records.map { rec ->
                        val timeText = rec.createdAt?.toDate()?.let { date ->
                            android.text.format.DateFormat.format("HH:mm", date).toString()
                        }.orEmpty()
                        ChatMessageUi(
                            id = rec.id.ifBlank { "${rec.role}_${rec.text.hashCode()}" },
                            isUser = rec.role == "user",
                            text = rec.text,
                            hasAttachment = rec.hasAttachment,
                            timestampLabel = timeText,
                        )
                    }
                    _state.update { it.copy(messages = mapped) }
                }
        }
    }

    fun attachBitmap(bitmap: Bitmap?) {
        _state.update { it.copy(attachedBitmap = bitmap, errorMessage = null) }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun sendPrompt(
        rawPrompt: String,
        overrideBitmap: Bitmap? = null,
        onReplyCompleted: (String) -> Unit = {},
    ) {
        val prompt = rawPrompt.trim()
        if (prompt.isEmpty() || _state.value.isGenerating) return
        if (!MuzaFeatures.aiConversation) {
            _state.update { it.copy(errorMessage = LanguageManager.currentStrings.featureDisabledNotice) }
            return
        }

        val bitmapToSend = overrideBitmap ?: _state.value.attachedBitmap
        val hasImage = bitmapToSend != null
        val lang = LanguageManager.currentLanguage

        _state.update {
            it.copy(
                isGenerating = true,
                streamingReply = "",
                attachedBitmap = null,
                errorMessage = null,
            )
        }

        activeStreamJob?.cancel()
        activeStreamJob = viewModelScope.launch {
            cloudRepository.saveAiMessage(
                role = "user",
                text = prompt,
                languageCode = lang.code,
                hasAttachment = hasImage,
            )

            var finalReply = ""
            try {
                aiRepository.streamChatReply(
                    prompt = prompt,
                    attachedImage = bitmapToSend,
                    language = lang,
                ).collect { accumulated ->
                    finalReply = accumulated
                    _state.update { it.copy(streamingReply = accumulated) }
                }

                if (finalReply.isNotBlank()) {
                    cloudRepository.saveAiMessage(
                        role = "model",
                        text = finalReply,
                        languageCode = lang.code,
                        hasAttachment = false,
                    )
                    onReplyCompleted(finalReply)
                }
                _state.update {
                    it.copy(
                        isGenerating = false,
                        streamingReply = "",
                    )
                }
            } catch (e: CancellationException) {
                _state.update { it.copy(isGenerating = false, streamingReply = "") }
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Gemini chat error", e)
                _state.update {
                    it.copy(
                        isGenerating = false,
                        streamingReply = "",
                        errorMessage = e.localizedMessage ?: "AI request failed. Please try again.",
                    )
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        activeStreamJob?.cancel()
    }

    class Factory(
        private val currentUserId: String,
        private val aiRepository: MuzaAiRepository,
        private val cloudRepository: MuzaCloudRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AiChatViewModel(currentUserId, aiRepository, cloudRepository) as T
        }
    }
}
