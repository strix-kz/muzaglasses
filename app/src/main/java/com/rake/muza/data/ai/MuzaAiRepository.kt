package com.rake.muza.data.ai

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import com.rake.muza.core.AppLanguage
import com.rake.muza.core.LanguageManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

private const val TAG = "MuzaAiRepository"
private const val AI_TIMEOUT_MS = 60_000L

class MuzaAiRepository {

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    "You are Muza, an intelligent, warm, fluent smart-glasses AI assistant inside Muza Glasses. " +
                        "When Kazakh is selected, speak rich, idiomatic, grammatically accurate Kazakh (қазақ тілінде еркін, көркем әрі анық сөйле), " +
                        "never mixing awkward robotic translations. Give complete, genuinely helpful answers that sound great when spoken aloud " +
                        "through smart glasses speakers, without markdown asterisks or bullet symbols."
                )
            }
        )
    }

    private fun languageInstruction(lang: AppLanguage = LanguageManager.currentLanguage): String =
        when (lang) {
            AppLanguage.KK -> "Жауапты тек таза, сауатты, табиғи қазақ тілінде бер. Жұлдызша (*) немесе markdown белгілерін қолданба."
            AppLanguage.RU -> "Отвечай на естественном, грамотном русском языке без символов markdown."
            AppLanguage.EN -> "Reply clearly and naturally in spoken English without markdown symbols."
        }

    fun streamChatReply(
        prompt: String,
        attachedImage: Bitmap? = null,
        language: AppLanguage = LanguageManager.currentLanguage,
    ): Flow<String> = flow {
        val fullPrompt = "${languageInstruction(language)}\n\nUser: $prompt"
        val accumulated = StringBuilder()
        try {
            withTimeout(AI_TIMEOUT_MS) {
                val inputContent = content {
                    if (attachedImage != null) {
                        image(attachedImage)
                    }
                    text(fullPrompt)
                }
                model.generateContentStream(inputContent).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(accumulated.toString())
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("AI returned an empty response.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI streaming timed out", e)
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI streaming failed", e)
            throw e
        }
    }

    /**
     * Answers any spoken question from the smart glasses user via Gemini 3.8 Flash
     * with a complete, clear spoken answer (plus optional live camera context).
     */
    suspend fun answerVoiceQuestion(
        question: String,
        cameraContext: Bitmap? = null,
        locationContext: String = "",
        language: AppLanguage = LanguageManager.currentLanguage,
    ): Result<String> = try {
        val response = withTimeout(AI_TIMEOUT_MS) {
            val locNote = if (locationContext.isNotBlank()) " User GPS location: $locationContext." else ""
            val prompt = "${languageInstruction(language)}$locNote " +
                "Answer the user's spoken question completely and clearly in 2-4 natural spoken sentences.\n" +
                "Question: $question"
            model.generateContent(
                content {
                    if (cameraContext != null) image(cameraContext)
                    text(prompt)
                }
            )
        }
        val text = response.text?.trim()
        if (!text.isNullOrBlank()) {
            Result.success(text)
        } else {
            Result.failure(IllegalStateException("AI returned an empty answer."))
        }
    } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "Voice question timed out", e)
        Result.failure(e)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "Voice question failed", e)
        Result.failure(e)
    }

    suspend fun describeCameraScene(
        bitmap: Bitmap?,
        locationContext: String,
        language: AppLanguage = LanguageManager.currentLanguage,
    ): Result<String> = try {
        val response = withTimeout(AI_TIMEOUT_MS) {
            val prompt = if (bitmap != null) {
                "${languageInstruction(language)} " +
                    "You are looking through the user's smart glasses camera right now. " +
                    "Describe thoroughly yet naturally in 2-3 clear spoken sentences what is directly in front of the user: " +
                    "main objects, people, surroundings, colors, and any visible text or signs. " +
                    "GPS context: $locationContext."
            } else {
                "${languageInstruction(language)} " +
                    "The user asked what is in front of them while wearing Muza Glasses at $locationContext, " +
                    "but the camera feed is still initializing. Politely tell them in 1-2 sentences to point the glasses camera forward and try again."
            }
            model.generateContent(
                content {
                    if (bitmap != null) image(bitmap)
                    text(prompt)
                }
            )
        }
        val text = response.text?.trim()
        if (!text.isNullOrBlank()) {
            Result.success(text)
        } else {
            Result.failure(IllegalStateException("Model returned an empty response."))
        }
    } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "Vision description timed out", e)
        Result.failure(e)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "Vision description failed", e)
        Result.failure(e)
    }

    suspend fun translateSceneOrText(
        sourceText: String?,
        bitmap: Bitmap? = null,
        targetLanguage: AppLanguage = LanguageManager.currentLanguage,
    ): Result<Pair<String, String>> = try {
        val targetName = when (targetLanguage) {
            AppLanguage.KK -> "natural, accurate Kazakh (қазақ тілі)"
            AppLanguage.RU -> "natural Russian"
            AppLanguage.EN -> "natural English"
        }
        val response = withTimeout(AI_TIMEOUT_MS) {
            val instruction = if (!sourceText.isNullOrBlank()) {
                "Translate the following spoken text into $targetName. Reply in exactly two lines without markdown:\n" +
                    "ORIGINAL: <original text>\nTRANSLATED: <clear translation>\nText: $sourceText"
            } else if (bitmap != null) {
                "Read any visible sign, label, menu, screen text, or object in front of the smart glasses camera and translate it into $targetName. " +
                    "If there is visible text, translate all key text clearly. If no text is visible, name the main object and translate it. " +
                    "Reply in exactly two lines without markdown:\nORIGINAL: <detected text or object>\nTRANSLATED: <clear translation>"
            } else {
                "Translate the phrase 'Welcome to Muza Glasses' into $targetName. Reply in exactly two lines:\n" +
                    "ORIGINAL: Welcome to Muza Glasses\nTRANSLATED: <translation>"
            }
            model.generateContent(
                content {
                    if (bitmap != null) image(bitmap)
                    text(instruction)
                }
            )
        }
        val raw = response.text?.trim().orEmpty()
        if (raw.isBlank()) {
            Result.failure(IllegalStateException("Empty translation response."))
        } else {
            val lines = raw.lines().map { it.trim() }.filter { it.isNotBlank() }
            val orig = lines.firstOrNull { it.startsWith("ORIGINAL:", true) }
                ?.substringAfter(":")?.trim()
                ?: sourceText.orEmpty().ifBlank { "Detected" }
            val trans = lines.firstOrNull { it.startsWith("TRANSLATED:", true) }
                ?.substringAfter(":")?.trim()
                ?: raw
            Result.success(orig to trans)
        }
    } catch (e: TimeoutCancellationException) {
        Log.e(TAG, "Translation timed out", e)
        Result.failure(e)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.e(TAG, "Translation failed", e)
        Result.failure(e)
    }
}
