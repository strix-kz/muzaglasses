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
                    "You are Muza, a helpful, concise smart-glasses AI assistant inside the Muza Glasses app. " +
                        "Always reply in the user's selected language (Kazakh, Russian, or English). " +
                        "Keep replies natural, warm, practical, and easy to read aloud via Text-to-Speech " +
                        "or show on an AR glasses corner display."
                )
            }
        )
    }

    private fun languageInstruction(lang: AppLanguage = LanguageManager.currentLanguage): String =
        when (lang) {
            AppLanguage.KK -> "Жауапты тек таза әрі табиғи қазақ тілінде бер."
            AppLanguage.RU -> "Отвечай на естественном и грамотном русском языке."
            AppLanguage.EN -> "Reply clearly and naturally in English."
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

    suspend fun describeCameraScene(
        bitmap: Bitmap?,
        locationContext: String,
        language: AppLanguage = LanguageManager.currentLanguage,
    ): Result<String> = try {
        val response = withTimeout(AI_TIMEOUT_MS) {
            val prompt = if (bitmap != null) {
                "${languageInstruction(language)} " +
                    "Describe clearly in 1-2 short sentences what is in front of the smart glasses user in this photo. " +
                    "Location context: $locationContext."
            } else {
                "${languageInstruction(language)} " +
                    "The user asked what is in front of them while wearing Muza Glasses at $locationContext. " +
                    "Give a concise, helpful 1-2 sentence response."
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
            AppLanguage.KK -> "Kazakh"
            AppLanguage.RU -> "Russian"
            AppLanguage.EN -> "English"
        }
        val response = withTimeout(AI_TIMEOUT_MS) {
            val instruction = if (!sourceText.isNullOrBlank()) {
                "Translate the following text into $targetName. Reply in exactly two lines:\n" +
                    "ORIGINAL: <original text>\nTRANSLATED: <translated text>\nText: $sourceText"
            } else if (bitmap != null) {
                "Read any visible sign or text in this image and translate it into $targetName. " +
                    "If no text is visible, identify the main object and translate its name. " +
                    "Reply in exactly two lines:\nORIGINAL: <detected text>\nTRANSLATED: <translated text>"
            } else {
                "Translate the sign 'OPEN' into $targetName. Reply in exactly two lines:\n" +
                    "ORIGINAL: OPEN\nTRANSLATED: <translated text>"
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
