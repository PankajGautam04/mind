package com.mind.app.core.model

/**
 * Available Gemini model descriptor returned from configuration or live model discovery.
 */
data class GeminiModelInfo(
    val id: String,
    val displayName: String,
    val description: String = "",
)

/**
 * Grounded conversational answer produced by Ask Mind using only retrieved user memories.
 */
data class AskMindAnswer(
    val question: String,
    val answerText: String,
    val citedMemories: List<Memory>,
)

/**
 * Result of testing the active Gemini API key / backend proxy and selected model.
 */
data class AiConnectionTestResult(
    val success: Boolean,
    val message: String,
    val latencyMs: Long = 0L,
)
