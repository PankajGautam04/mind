package com.mind.app.core.data.ai

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mind.app.core.database.dao.MemoryDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Runs offline ML Kit OCR right after image/screenshot capture so captured text is searchable
 * immediately via FTS4 even without internet or when local-only mode is enabled.
 */
@HiltWorker
class LocalOcrWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val memoryDao: MemoryDao,
    private val localTextRecognizer: LocalTextRecognizer,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val memoryId = inputData.getString(KEY_MEMORY_ID) ?: return Result.success()
        val entity = memoryDao.getMemoryEntityById(memoryId) ?: return Result.success()
        if (entity.isDeleted || !entity.extractedText.isNullOrBlank()) {
            return Result.success()
        }

        val extracted = localTextRecognizer.extractText(entity.originalFilePath)
            ?: return Result.success()

        memoryDao.updateExtractedTextIfBlank(
            memoryId = memoryId,
            extractedText = extracted,
            updatedAt = System.currentTimeMillis(),
        )
        return Result.success()
    }

    companion object {
        const val KEY_MEMORY_ID = "memory_id"
    }
}
