package com.mind.app.assist

import android.os.Bundle
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import com.mind.app.core.data.storage.LocalMediaStorage
import com.mind.app.core.domain.repository.PreferencesRepository
import com.mind.app.core.domain.usecase.CaptureMemoryUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import javax.inject.Inject

/**
 * Creates per-invocation [MindVoiceInteractionSession] instances when the user long-presses
 * the Samsung Side Button (Digital Assistant) or triggers the system assistant gesture.
 */
@AndroidEntryPoint
class MindVoiceInteractionSessionService : VoiceInteractionSessionService() {

    @Inject lateinit var localMediaStorage: LocalMediaStorage
    @Inject lateinit var captureMemoryUseCase: CaptureMemoryUseCase
    @Inject lateinit var preferencesRepository: PreferencesRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewSession(args: Bundle?): VoiceInteractionSession {
        return MindVoiceInteractionSession(
            context = this,
            localMediaStorage = localMediaStorage,
            captureMemoryUseCase = captureMemoryUseCase,
            preferencesRepository = preferencesRepository,
            serviceScope = serviceScope,
        )
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
