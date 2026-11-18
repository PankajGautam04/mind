package com.mind.app.core.data.ai

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * On-device ML Kit text recognizer that extracts text from saved screenshots/images
 * immediately without requiring network access or cloud AI.
 */
@Singleton
class LocalTextRecognizer @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    suspend fun extractText(filePath: String?): String? = withContext(Dispatchers.IO) {
        if (filePath.isNullOrBlank()) return@withContext null
        val file = File(filePath)
        if (!file.exists()) return@withContext null

        runCatching {
            val image = InputImage.fromFilePath(context, Uri.fromFile(file))
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).use { client ->
                suspendCancellableCoroutine<String> { continuation ->
                    client.process(image)
                        .addOnSuccessListener { result ->
                            if (continuation.isActive) {
                                continuation.resume(result.text)
                            }
                        }
                        .addOnFailureListener { error ->
                            if (continuation.isActive) {
                                continuation.resumeWithException(error)
                            }
                        }
                }
            }
        }.getOrNull()
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }
}
