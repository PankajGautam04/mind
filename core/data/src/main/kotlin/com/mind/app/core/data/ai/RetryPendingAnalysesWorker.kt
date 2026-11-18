package com.mind.app.core.data.ai

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mind.app.core.domain.repository.PreferencesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Re-queues failed memory analyses once network connectivity returns.
 */
@HiltWorker
class RetryPendingAnalysesWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val preferencesRepository: PreferencesRepository,
    private val geminiAiEngineRepository: GeminiAiEngineRepository,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val prefs = preferencesRepository.observePreferences().first()
        if (prefs.localOnlyMode || !prefs.cloudAiEnabled) {
            return Result.success()
        }
        geminiAiEngineRepository.enqueueFailedRecoveryRetries()
        return Result.success()
    }
}
