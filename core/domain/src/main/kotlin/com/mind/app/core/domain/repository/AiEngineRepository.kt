package com.mind.app.core.domain.repository

import com.mind.app.core.model.AiConnectionTestResult
import com.mind.app.core.model.AskMindAnswer
import com.mind.app.core.model.GeminiModelInfo
import com.mind.app.core.model.Memory

/**
 * Contract for background and on-demand Gemini understanding, semantic vector search,
 * Ask Mind Q&A, and model discovery.
 */
interface AiEngineRepository {
    suspend fun enqueueAnalysis(memoryId: String)
    suspend fun enqueueLocalOcr(memoryId: String)
    suspend fun scheduleRetryWhenOnline()
    suspend fun analyzeNow(memoryId: String, markFailedOnError: Boolean = true): Result<Unit>
    suspend fun cancelAnalysis(memoryId: String)
    suspend fun retryFailedAnalyses(): Int
    suspend fun semanticSearch(query: String, limit: Int = 25): Result<List<Memory>>
    suspend fun askMind(question: String): Result<AskMindAnswer>
    suspend fun askAboutMemory(memoryId: String, question: String): Result<String>
    suspend fun fetchAvailableModels(): Result<List<GeminiModelInfo>>
    suspend fun testConnection(): AiConnectionTestResult
}
