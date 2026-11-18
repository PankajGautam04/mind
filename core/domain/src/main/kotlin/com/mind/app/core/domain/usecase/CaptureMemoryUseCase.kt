package com.mind.app.core.domain.usecase

import com.mind.app.core.domain.repository.AiEngineRepository
import com.mind.app.core.domain.repository.MemoryRepository
import com.mind.app.core.domain.repository.PreferencesRepository
import com.mind.app.core.domain.repository.SourceAppRepository
import com.mind.app.core.model.AiProcessingStatus
import com.mind.app.core.model.CaptureMethod
import com.mind.app.core.model.Category
import com.mind.app.core.model.ContentType
import com.mind.app.core.model.Memory
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject

/**
 * Saves any captured item (screen, image, voice memo, text/quote, URL) locally in Room
 * immediately, then enqueues background Gemini understanding when Cloud AI is enabled
 * and the source app is not excluded for privacy.
 */
class CaptureMemoryUseCase @Inject constructor(
    private val memoryRepository: MemoryRepository,
    private val preferencesRepository: PreferencesRepository,
    private val sourceAppRepository: SourceAppRepository,
    private val aiEngineRepository: AiEngineRepository,
) {
    suspend fun saveImageMemory(
        originalFilePath: String,
        thumbnailPath: String?,
        contentType: ContentType = ContentType.SCREENSHOT,
        captureMethod: CaptureMethod = CaptureMethod.IN_APP,
        sourcePackageName: String? = null,
        sourceAppName: String? = null,
        initialTitle: String? = null,
        initialText: String? = null,
        originalUrl: String? = null,
    ): Memory {
        val now = System.currentTimeMillis()
        val shouldAnalyze = shouldAnalyzeWithAi(sourcePackageName)

        val memory = Memory(
            id = "mem_${UUID.randomUUID()}",
            originalFilePath = originalFilePath,
            thumbnailPath = thumbnailPath ?: originalFilePath,
            contentType = contentType,
            aiTitle = initialTitle?.takeIf { it.isNotBlank() } ?: defaultTitleFor(contentType),
            extractedText = initialText?.takeIf { it.isNotBlank() },
            primaryCategory = Category(id = UNCATEGORIZED_ID, name = "Uncategorized", isSystemDefault = true),
            sourcePackageName = sourcePackageName,
            sourceAppName = sourceAppName,
            originalUrl = originalUrl,
            capturedAtEpochMillis = now,
            updatedAtEpochMillis = now,
            aiStatus = if (shouldAnalyze) AiProcessingStatus.PENDING else AiProcessingStatus.SKIPPED_LOCAL_ONLY,
            captureMethod = captureMethod,
        )

        memoryRepository.upsertMemory(memory)
        if (initialText.isNullOrBlank()) {
            aiEngineRepository.enqueueLocalOcr(memory.id)
        }
        if (shouldAnalyze) {
            aiEngineRepository.enqueueAnalysis(memory.id)
        }
        return memory
    }

    suspend fun saveVoiceMemory(
        audioFilePath: String,
        durationSeconds: Int,
        captureMethod: CaptureMethod = CaptureMethod.IN_APP,
    ): Memory {
        val now = System.currentTimeMillis()
        val shouldAnalyze = shouldAnalyzeWithAi(sourcePackageName = null)

        val memory = Memory(
            id = "mem_${UUID.randomUUID()}",
            originalFilePath = audioFilePath,
            contentType = ContentType.VOICE_NOTE,
            aiTitle = "Voice Memory (${formatDuration(durationSeconds)})",
            description = if (shouldAnalyze) {
                "Waiting for Gemini transcription and summary..."
            } else {
                "Voice recording (${formatDuration(durationSeconds)}) saved locally."
            },
            primaryCategory = Category(id = PERSONAL_NOTES_CATEGORY_ID, name = "Personal", isSystemDefault = true),
            subcategory = Category(
                id = PERSONAL_NOTES_SUBCATEGORY_ID,
                name = "Notes",
                parentId = PERSONAL_NOTES_CATEGORY_ID,
                isSystemDefault = true,
            ),
            capturedAtEpochMillis = now,
            updatedAtEpochMillis = now,
            aiStatus = if (shouldAnalyze) AiProcessingStatus.PENDING else AiProcessingStatus.SKIPPED_LOCAL_ONLY,
            captureMethod = captureMethod,
        )

        memoryRepository.upsertMemory(memory)
        if (shouldAnalyze) {
            aiEngineRepository.enqueueAnalysis(memory.id)
        }
        return memory
    }

    suspend fun saveTextOrUrlMemory(
        text: String,
        url: String? = null,
        title: String? = null,
        isQuote: Boolean = false,
        captureMethod: CaptureMethod = CaptureMethod.IN_APP,
        sourcePackageName: String? = null,
        sourceAppName: String? = null,
    ): Memory {
        val now = System.currentTimeMillis()
        val shouldAnalyze = shouldAnalyzeWithAi(sourcePackageName)
        val trimmedText = text.trim()
        val trimmedUrl = url?.trim()?.takeIf { it.isNotBlank() }
        val resolvedType = when {
            isQuote -> ContentType.QUOTE
            trimmedUrl != null -> ContentType.URL
            else -> ContentType.TEXT
        }
        val fallbackTitle = title?.trim()?.takeIf { it.isNotBlank() }
            ?: trimmedText.lineSequence().firstOrNull()?.take(72)?.takeIf { it.isNotBlank() }
            ?: trimmedUrl
            ?: defaultTitleFor(resolvedType)

        val memory = Memory(
            id = "mem_${UUID.randomUUID()}",
            contentType = resolvedType,
            aiTitle = fallbackTitle,
            extractedText = trimmedText.takeIf { it.isNotBlank() },
            originalUrl = trimmedUrl,
            primaryCategory = Category(id = UNCATEGORIZED_ID, name = "Uncategorized", isSystemDefault = true),
            sourcePackageName = sourcePackageName,
            sourceAppName = sourceAppName,
            capturedAtEpochMillis = now,
            updatedAtEpochMillis = now,
            aiStatus = if (shouldAnalyze) AiProcessingStatus.PENDING else AiProcessingStatus.SKIPPED_LOCAL_ONLY,
            captureMethod = captureMethod,
        )

        memoryRepository.upsertMemory(memory)
        if (shouldAnalyze) {
            aiEngineRepository.enqueueAnalysis(memory.id)
        }
        return memory
    }

    private suspend fun shouldAnalyzeWithAi(sourcePackageName: String?): Boolean {
        val prefs = preferencesRepository.observePreferences().first()
        if (!prefs.cloudAiEnabled || prefs.localOnlyMode) return false
        if (!sourcePackageName.isNullOrBlank()) {
            val excluded = sourceAppRepository.observeExcludedPackages().first()
            if (sourcePackageName in excluded) return false
        }
        return true
    }

    private fun defaultTitleFor(type: ContentType): String = when (type) {
        ContentType.SCREENSHOT -> "Captured Screen"
        ContentType.IMAGE -> "Saved Image"
        ContentType.VOICE_NOTE -> "Voice Memory"
        ContentType.URL -> "Saved Link"
        ContentType.QUOTE -> "Saved Quote"
        else -> "Quick Note"
    }

    private fun formatDuration(seconds: Int): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return "%d:%02d".format(mins, secs)
    }

    companion object {
        private const val UNCATEGORIZED_ID = "cat_uncategorized"
        private const val PERSONAL_NOTES_CATEGORY_ID = "cat_personal"
        private const val PERSONAL_NOTES_SUBCATEGORY_ID = "cat_personal_notes"
    }
}
