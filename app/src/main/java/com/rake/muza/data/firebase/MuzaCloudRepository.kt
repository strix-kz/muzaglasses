package com.rake.muza.data.firebase

import android.content.Context
import androidx.annotation.Keep
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.snapshots
import com.rake.muza.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val preferredLanguage: String = "KK",
    val voiceGender: String = "FEMALE",
    val voiceSpeed: String = "MEDIUM",
    val selectedGlassesModel: String = "RAYBAN_META_GEN_2",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class AiChatRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val role: String = "user",
    val text: String = "",
    val language: String = "KK",
    val hasAttachment: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class CloudPhotoRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val localMediaUri: String = "",
    val city: String = "Алматы",
    val dateLabel: String = "",
    val description: String = "",
    val favorite: Boolean = false,
    val glassesModel: String = "Ray-Ban Meta Gen 2",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

class MuzaCloudRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val docRef = db.collection("users").document(userId)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureUserProfile(
        displayName: String,
        email: String,
        languageCode: String,
        voiceGenderCode: String,
        voiceSpeedCode: String,
        selectedGlassesModel: String,
    ): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
            path = docRef.path
            val snapshot = docRef.get().await()
            val safeName = displayName.trim().ifBlank { "Muza User" }.take(80)
            val safeEmail = email.trim().take(120)
            if (!snapshot.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "email" to safeEmail,
                    "preferredLanguage" to languageCode,
                    "voiceGender" to voiceGenderCode,
                    "voiceSpeed" to voiceSpeedCode,
                    "selectedGlassesModel" to selectedGlassesModel.take(60),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updatePreferences(
        languageCode: String? = null,
        voiceGenderCode: String? = null,
        voiceSpeedCode: String? = null,
        selectedGlassesModel: String? = null,
    ): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
            path = docRef.path
            val updates = mutableMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
            if (languageCode != null) updates["preferredLanguage"] = languageCode
            if (voiceGenderCode != null) updates["voiceGender"] = voiceGenderCode
            if (voiceSpeedCode != null) updates["voiceSpeed"] = voiceSpeedCode
            if (selectedGlassesModel != null) updates["selectedGlassesModel"] = selectedGlassesModel.take(60)
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeAiMessages(userId: String): Flow<List<AiChatRecord>> = flow {
        val colPath = "users/$userId/ai_messages"
        emitAll(
            db.collection("users").document(userId).collection("ai_messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(AiChatRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.LIST, colPath)
                    }
                    throw error
                }
        )
    }

    suspend fun saveAiMessage(
        role: String,
        text: String,
        languageCode: String,
        hasAttachment: Boolean,
    ): Result<Unit> {
        var path = "users/ai_messages"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("ai_messages").document()
            path = docRef.path
            val safeText = text.trim().take(4000)
            if (safeText.isBlank()) return Result.success(Unit)
            val payload = mapOf(
                "userId" to uid,
                "role" to if (role == "user") "user" else "model",
                "text" to safeText,
                "language" to languageCode,
                "hasAttachment" to hasAttachment,
                "createdAt" to FieldValue.serverTimestamp(),
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun syncPhotoRecord(
        docId: String,
        localMediaUri: String,
        city: String,
        dateLabel: String,
        description: String,
        favorite: Boolean,
        glassesModel: String,
    ): Result<Unit> {
        var path = "users/photo_records"
        return try {
            val uid = requireUserId()
            val safeDocId = docId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
            val docRef = db.collection("users").document(uid).collection("photo_records").document(safeDocId)
            path = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "localMediaUri" to localMediaUri.take(500),
                "city" to city.ifBlank { "Алматы" }.take(100),
                "dateLabel" to dateLabel.ifBlank { "Today" }.take(60),
                "description" to description.take(2000),
                "favorite" to favorite,
                "glassesModel" to glassesModel.ifBlank { "Ray-Ban Meta Gen 2" }.take(80),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateCloudPhotoFavorite(docId: String, favorite: Boolean): Result<Unit> {
        var path = "users/photo_records"
        return try {
            val uid = requireUserId()
            val safeDocId = docId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
            val docRef = db.collection("users").document(uid).collection("photo_records").document(safeDocId)
            path = docRef.path
            docRef.update(
                mapOf(
                    "favorite" to favorite,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCloudPhotoRecord(docId: String): Result<Unit> {
        var path = "users/photo_records"
        return try {
            val uid = requireUserId()
            val safeDocId = docId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
            val docRef = db.collection("users").document(uid).collection("photo_records").document(safeDocId)
            path = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }
}
