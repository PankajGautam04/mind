package com.mind.app.capture

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.content.IntentCompat
import androidx.lifecycle.lifecycleScope
import com.mind.app.core.data.storage.LocalMediaStorage
import com.mind.app.core.domain.usecase.CaptureMemoryUseCase
import com.mind.app.core.model.CaptureMethod
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

/**
 * Receives shared images, audio clips, links, and text from Android's Sharesheet,
 * saves immediately to Mind's local database/storage, and plays the signature save animation.
 */
@AndroidEntryPoint
class ShareReceiverActivity : ComponentActivity() {

    @Inject lateinit var localMediaStorage: LocalMediaStorage
    @Inject lateinit var captureMemoryUseCase: CaptureMemoryUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            val savedShare = withContext(Dispatchers.IO) {
                handleIncomingShare(intent)
            }
            if (savedShare != null) {
                val (savedMemoryId, thumbnailPath) = savedShare
                val overlayIntent = Intent(this@ShareReceiverActivity, SaveOverlayActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    putExtra(SaveOverlayActivity.EXTRA_MEMORY_ID, savedMemoryId)
                    putExtra(SaveOverlayActivity.EXTRA_MESSAGE, "Saved to Mind")
                    thumbnailPath?.let { putExtra(SaveOverlayActivity.EXTRA_THUMBNAIL_PATH, it) }
                }
                startActivity(overlayIntent)
            }
            finish()
        }
    }

    private suspend fun handleIncomingShare(incoming: Intent?): Pair<String, String?>? {
        if (incoming == null) return null
        val action = incoming.action ?: return null
        val mimeType = incoming.type
        val sharedText = incoming.getStringExtra(Intent.EXTRA_TEXT)
        val sharedSubject = incoming.getStringExtra(Intent.EXTRA_SUBJECT)
        val sourcePackage = referrer?.host

        if (action == Intent.ACTION_SEND) {
            val streamUri = IntentCompat.getParcelableExtra(incoming, Intent.EXTRA_STREAM, Uri::class.java)
            if (streamUri != null) {
                val memoryKey = "share_${UUID.randomUUID()}"
                val imported = localMediaStorage.importUriToStorage(contentResolver, streamUri, memoryKey, mimeType)
                if (imported != null) {
                    val saved = captureMemoryUseCase.saveImageMemory(
                        originalFilePath = imported.originalPath,
                        thumbnailPath = imported.thumbnailPath,
                        contentType = imported.contentType,
                        captureMethod = CaptureMethod.SHARESHEET,
                        sourcePackageName = sourcePackage,
                        sourceAppName = resolveAppLabel(sourcePackage),
                        initialTitle = sharedSubject,
                        initialText = sharedText,
                        originalUrl = extractFirstUrl(sharedText),
                    )
                    return saved.id to imported.thumbnailPath
                }
            }

            if (!sharedText.isNullOrBlank()) {
                val url = extractFirstUrl(sharedText)
                val saved = captureMemoryUseCase.saveTextOrUrlMemory(
                    text = sharedText,
                    url = url,
                    title = sharedSubject,
                    captureMethod = CaptureMethod.SHARESHEET,
                    sourcePackageName = sourcePackage,
                    sourceAppName = resolveAppLabel(sourcePackage),
                )
                return saved.id to null
            }
        } else if (action == Intent.ACTION_SEND_MULTIPLE) {
            val uris = IntentCompat.getParcelableArrayListExtra(incoming, Intent.EXTRA_STREAM, Uri::class.java)
                .orEmpty()
            var lastSaved: Pair<String, String?>? = null
            uris.forEach { uri ->
                val memoryKey = "share_${UUID.randomUUID()}"
                val imported = localMediaStorage.importUriToStorage(contentResolver, uri, memoryKey, mimeType)
                if (imported != null) {
                    val saved = captureMemoryUseCase.saveImageMemory(
                        originalFilePath = imported.originalPath,
                        thumbnailPath = imported.thumbnailPath,
                        contentType = imported.contentType,
                        captureMethod = CaptureMethod.SHARESHEET,
                        sourcePackageName = sourcePackage,
                        sourceAppName = resolveAppLabel(sourcePackage),
                        initialTitle = sharedSubject,
                    )
                    lastSaved = saved.id to imported.thumbnailPath
                }
            }
            return lastSaved
        }
        return null
    }

    private fun extractFirstUrl(text: String?): String? {
        if (text.isNullOrBlank()) return null
        return URL_REGEX.find(text)?.value
    }

    private fun resolveAppLabel(packageName: String?): String? {
        if (packageName.isNullOrBlank()) return null
        return runCatching {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        }.getOrNull()
    }

    private companion object {
        val URL_REGEX = Regex("https?://\\S+", RegexOption.IGNORE_CASE)
    }
}
