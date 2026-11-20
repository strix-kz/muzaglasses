package com.rake.muza.data.local

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Entity(tableName = "photo_history")
data class PhotoHistoryEntity(
    @PrimaryKey val id: String = "photo_${UUID.randomUUID().toString().replace("-", "").take(12)}",
    val mediaUri: String,
    val localFilePath: String,
    val dateLabel: String,
    val city: String,
    val description: String,
    val glassesModel: String,
    val isFavorite: Boolean = false,
    val capturedAtMillis: Long = System.currentTimeMillis(),
)

@Dao
interface PhotoHistoryDao {
    @Query("SELECT * FROM photo_history ORDER BY capturedAtMillis DESC")
    fun observeAllPhotos(): Flow<List<PhotoHistoryEntity>>

    @Query("SELECT * FROM photo_history WHERE isFavorite = 1 ORDER BY capturedAtMillis DESC")
    fun observeSavedPhotos(): Flow<List<PhotoHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: PhotoHistoryEntity)

    @Query("UPDATE photo_history SET isFavorite = :favorite WHERE id = :photoId")
    suspend fun setFavorite(photoId: String, favorite: Boolean)

    @Query("UPDATE photo_history SET description = :description WHERE id = :photoId")
    suspend fun updateDescription(photoId: String, description: String)

    @Query("DELETE FROM photo_history WHERE id = :photoId")
    suspend fun deleteById(photoId: String)
}

@Database(entities = [PhotoHistoryEntity::class], version = 1, exportSchema = false)
abstract class MuzaDatabase : RoomDatabase() {
    abstract fun photoHistoryDao(): PhotoHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: MuzaDatabase? = null

        fun getInstance(context: Context): MuzaDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MuzaDatabase::class.java,
                    "muza_glasses.db"
                ).build().also { INSTANCE = it }
            }
    }
}

data class SavedMediaResult(
    val mediaUri: String,
    val localFilePath: String,
)

object MediaStorePhotoSaver {
    private const val TAG = "MediaStorePhotoSaver"

    suspend fun saveBitmap(context: Context, bitmap: Bitmap): SavedMediaResult = withContext(Dispatchers.IO) {
        val fileName = "MUZA_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"

        // 1. Always save a private copy so the photo loads reliably in History even if removed externally
        val photosDir = File(context.filesDir, "photos").apply { mkdirs() }
        val localFile = File(photosDir, fileName)
        FileOutputStream(localFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }

        // 2. Also export to Android MediaStore (Pictures/MuzaGlasses) — zero storage permissions on API 29+
        var mediaStoreUriString = ""
        runCatching {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/MuzaGlasses")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val resolver = context.contentResolver
            val insertedUri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (insertedUri != null) {
                resolver.openOutputStream(insertedUri)?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 92, stream)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(insertedUri, contentValues, null, null)
                }
                mediaStoreUriString = insertedUri.toString()
            }
        }.onFailure { e ->
            Log.w(TAG, "MediaStore export skipped: ${e.message}")
        }

        val fallbackUri = runCatching {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", localFile).toString()
        }.getOrDefault(Uri.fromFile(localFile).toString())

        SavedMediaResult(
            mediaUri = mediaStoreUriString.ifBlank { fallbackUri },
            localFilePath = localFile.absolutePath,
        )
    }
}

class PhotoHistoryRepository(
    private val context: Context,
    private val dao: PhotoHistoryDao,
) {
    val allPhotos: Flow<List<PhotoHistoryEntity>> = dao.observeAllPhotos()
    val savedPhotos: Flow<List<PhotoHistoryEntity>> = dao.observeSavedPhotos()

    suspend fun saveCapturedPhoto(
        bitmap: Bitmap,
        city: String,
        description: String,
        glassesModel: String,
    ): PhotoHistoryEntity {
        val saved = MediaStorePhotoSaver.saveBitmap(context, bitmap)
        val dateLabel = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(Date())
        val entity = PhotoHistoryEntity(
            mediaUri = saved.mediaUri,
            localFilePath = saved.localFilePath,
            dateLabel = dateLabel,
            city = city.ifBlank { "Алматы" },
            description = description,
            glassesModel = glassesModel,
            isFavorite = false,
            capturedAtMillis = System.currentTimeMillis(),
        )
        dao.insertPhoto(entity)
        return entity
    }

    suspend fun toggleFavorite(photo: PhotoHistoryEntity): Boolean {
        val next = !photo.isFavorite
        dao.setFavorite(photo.id, next)
        return next
    }

    suspend fun updateDescription(photoId: String, description: String) {
        dao.updateDescription(photoId, description)
    }

    suspend fun deletePhoto(photo: PhotoHistoryEntity) = withContext(Dispatchers.IO) {
        dao.deleteById(photo.id)
        runCatching {
            if (photo.localFilePath.isNotBlank()) {
                File(photo.localFilePath).delete()
            }
        }
    }
}
