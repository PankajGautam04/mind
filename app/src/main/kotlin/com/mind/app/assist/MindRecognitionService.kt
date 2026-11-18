package com.mind.app.assist

import android.content.Intent
import android.speech.RecognitionService
import android.speech.SpeechRecognizer

/**
 * Companion RecognitionService required alongside VoiceInteractionService by Android's
 * VoiceInteractionServiceInfo validator so Mind qualifies as a Default Digital Assistant app.
 */
class MindRecognitionService : RecognitionService() {
    override fun onStartListening(recognizerIntent: Intent?, listener: Callback?) {
        runCatching { listener?.error(SpeechRecognizer.ERROR_CLIENT) }
    }

    override fun onCancel(listener: Callback?) = Unit

    override fun onStopListening(listener: Callback?) {
        runCatching { listener?.endOfSpeech() }
    }
}
