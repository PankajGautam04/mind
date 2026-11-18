package com.mind.app.assist

import android.service.voice.VoiceInteractionService

/**
 * Top-level Android Digital Assistant service entry point for Mind.
 * Remains completely idle in the background (no always-on hotword microphone listening).
 */
class MindVoiceInteractionService : VoiceInteractionService() {
    override fun onReady() {
        super.onReady()
        // Explicitly allow OS screen context & screenshot delivery whenever permitted by user settings.
        disabledShowContext = 0
    }
}
