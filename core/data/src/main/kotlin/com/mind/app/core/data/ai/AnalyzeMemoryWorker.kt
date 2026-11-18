package com.mind.app.core.data.ai

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mind.app.core.domain.repository.AiEngineRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Deferrable background WorkManager job that sends a captured memory to Gemini without
 * blocking capture speed or the UI thread.
 */
@HiltWorker
class AnalyzeMemoryWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val aiEngineRepository: AiEngineRepository,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val memoryId = inputData.getString(KEY_MEMORY_ID) ?: return Result.failure()
        val isRecoveryRetry = inputData.getBoolean(KEY_IS_RECOVERY_RETRY, false)
        val hasMoreAttempts = runAttemptCount < MAX_ATTEMPTS
        val outcome = aiEngineRepository.analyzeNow(
            memoryId = memoryId,
            markFailedOnError = !hasMoreAttempts,
        )
        return if (outcome.isSuccess) {
            Result.success()
        } else if (hasMoreAttempts) {
            Result.retry()
        } else {
            val error = outcome.exceptionOrNull()
            if (!isRecoveryRetry && isRetryableRecoveryError(error)) {
                aiEngineRepository.scheduleRetryWhenOnline()
            }
            Result.failure()
        }
    }

    private fun isRetryableRecoveryError(error: Throwable?): Boolean {
        val code = (error as? GeminiHttpException)?.statusCode
        if (code in setOf(400, 401, 403, 404)) return false
        val msg = error?.message.orEmpty()
        if (msg.contains("API key", ignoreCase = true) || msg.contains("disabled", ignoreCase = true)) {
            return false
        }
        return true
    }

    companion object {
        const val KEY_MEMORY_ID = "memory_id"
        const val KEY_IS_RECOVERY_RETRY = "is_recovery_retry"
        private const val MAX_ATTEMPTS = 3
    }
}
