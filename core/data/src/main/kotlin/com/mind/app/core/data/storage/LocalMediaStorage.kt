package com.mind.app.core.data.storage

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import com.mind.app.core.database.MindDatabase
import com.mind.app.core.model.ContentType
import com.mind.app.core.model.StorageStats
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.roundToInt

data class ImportedMedia(
    val originalPath: String,
    val thumbnailPath: String?,
    val contentType: ContentType,
)

/**
 * Manages internal app storage directories for captured screenshots, imported images,
 * voice recordings, and compressed thumbnails so all media stays private to Mind.
 */
@Singleton
class LocalMediaStorage @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val memoriesRootDir: File
        get() = File(context.filesDir, "memories").apply { mkdirs() }

    fun originalsDir(): File = File(memoriesRootDir, "originals").apply { mkdirs() }
    fun thumbnailsDir(): File = File(memoriesRootDir, "thumbnails").apply { mkdirs() }
    fun audioDir(): File = File(memoriesRootDir, "audio").apply { mkdirs() }

    fun allocateVoiceFile(memoryKey: String): File =
        File(audioDir(), "$memoryKey.m4a")

    /**
     * Compresses a captured screen/photo bitmap cleanly (preserving text legibility up to 2560px)
     * and generates a lightweight 480px thumbnail for rapid grid/list scrolling.
     */
    fun saveBitmapAndThumbnail(bitmap: Bitmap, memoryKey: String): Pair<String, String> {
        val softwareBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            bitmap.config == Bitmap.Config.HARDWARE
        ) {
            bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: bitmap
        } else {
            bitmap
        }
        val optimized = scaleDownIfNeeded(softwareBitmap, MAX_ORIGINAL_DIMENSION)
        val originalFile = File(originalsDir(), "$memoryKey.jpg")
        FileOutputStream(originalFile).use { out ->
            optimized.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        if (optimized !== softwareBitmap) optimized.recycle()

        val thumbBitmap = scaleDownIfNeeded(softwareBitmap, MAX_THUMBNAIL_DIMENSION)
        val thumbFile = File(thumbnailsDir(), "${memoryKey}_thumb.jpg")
        FileOutputStream(thumbFile).use { out ->
            thumbBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        if (thumbBitmap !== softwareBitmap) thumbBitmap.recycle()
        if (softwareBitmap !== bitmap) softwareBitmap.recycle()

        return originalFile.absolutePath to thumbFile.absolutePath
    }

    /**
     * Copies shared or picked content from a content:// Uri into Mind's private storage.
     */
    fun importUriToStorage(
        contentResolver: ContentResolver,
        uri: Uri,
        memoryKey: String,
        explicitMimeType: String? = null,
    ): ImportedMedia? {
        val mime = explicitMimeType ?: contentResolver.getType(uri).orEmpty()
        return when {
            mime.startsWith("image/") -> {
                val decoded = contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                } ?: return null
                val (origPath, thumbPath) = saveBitmapAndThumbnail(decoded, memoryKey)
                decoded.recycle()
                ImportedMedia(
                    originalPath = origPath,
                    thumbnailPath = thumbPath,
                    contentType = ContentType.IMAGE,
                )
            }
            mime.startsWith("audio/") -> {
                val ext = mime.substringAfter('/', "m4a").substringBefore(';').ifBlank { "m4a" }
                val dest = File(audioDir(), "$memoryKey.$ext")
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(dest).use { output -> input.copyTo(output) }
                } ?: return null
                ImportedMedia(
                    originalPath = dest.absolutePath,
                    thumbnailPath = null,
                    contentType = ContentType.VOICE_NOTE,
                )
            }
            else -> {
                val dest = File(originalsDir(), "$memoryKey.bin")
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(dest).use { output -> input.copyTo(output) }
                } ?: return null
                ImportedMedia(
                    originalPath = dest.absolutePath,
                    thumbnailPath = null,
                    contentType = ContentType.DOCUMENT,
                )
            }
        }
    }

    fun calculateStorageStats(totalMemoryCount: Int): StorageStats {
        val originalsBytes = dirSizeBytes(originalsDir()) + dirSizeBytes(audioDir())
        val thumbnailsBytes = dirSizeBytes(thumbnailsDir())
        val dbFile = context.getDatabasePath(MindDatabase.DATABASE_NAME)
        val dbBytes = (dbFile?.length() ?: 0L) +
            (File("${dbFile?.absolutePath}-wal").takeIf { it.exists() }?.length() ?: 0L)
        val cacheBytes = dirSizeBytes(context.cacheDir)

        return StorageStats(
            totalMemoryCount = totalMemoryCount,
            imageBytes = originalsBytes,
            thumbnailBytes = thumbnailsBytes,
            databaseBytes = dbBytes,
            cacheBytes = cacheBytes,
        )
    }

    fun clearCache() {
        context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
    }

    /**
     * Removes files belonging strictly within Mind's internal memories directory.
     */
    fun deleteStoredMedia(originalPath: String?, thumbnailPath: String?) {
        deleteIfInsideMemoriesDir(originalPath)
        deleteIfInsideMemoriesDir(thumbnailPath)
    }

    fun deleteAllStoredMedia() {
        memoriesRootDir.deleteRecursively()
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val largest = max(bitmap.width, bitmap.height)
        if (largest <= maxDimension) return bitmap
        val ratio = maxDimension.toFloat() / largest.toFloat()
        val targetW = (bitmap.width * ratio).roundToInt().coerceAtLeast(1)
        val targetH = (bitmap.height * ratio).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }

    private fun dirSizeBytes(dir: File): Long {
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    private fun deleteIfInsideMemoriesDir(path: String?) {
        if (path.isNullOrBlank()) return
        val file = File(path)
        val rootCanonical = runCatching { memoriesRootDir.canonicalPath }.getOrNull() ?: return
        val fileCanonical = runCatching { file.canonicalPath }.getOrNull() ?: return
        if (fileCanonical.startsWith(rootCanonical) && file.exists()) {
            file.delete()
        }
    }

    private companion object {
        const val MAX_ORIGINAL_DIMENSION = 2560
        const val MAX_THUMBNAIL_DIMENSION = 1080
    }
}
