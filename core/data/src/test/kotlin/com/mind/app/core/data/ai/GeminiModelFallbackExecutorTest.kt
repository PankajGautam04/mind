package com.mind.app.core.data.ai

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiModelFallbackExecutorTest {

    @Test
    fun transientErrorRetriesSameModelBeforeSwitching() = runTest {
        var attempts = 0
        var switchedTo: String? = null
        val delays = mutableListOf<Long>()

        val outcome = GeminiModelFallbackExecutor.execute(
            preferredModel = "gemini-3.5-flash",
            fetchLiveModels = { error("Should not fetch models when primary retry succeeds") },
            onModelSwitched = { switchedTo = it },
            delayMillis = { delays.add(it) },
            attempt = { model ->
                attempts += 1
                assertEquals("gemini-3.5-flash", model)
                if (attempts == 1) {
                    Result.failure(GeminiHttpException(503, "Service overloaded"))
                } else {
                    Result.success("ok-after-retry")
                }
            },
        ).getOrThrow()

        assertEquals(2, attempts)
        assertEquals("ok-after-retry", outcome.value)
        assertEquals("gemini-3.5-flash", outcome.usedModel)
        assertNull(outcome.switchedFrom)
        assertNull(switchedTo)
        assertEquals(listOf(900L), delays)
    }

    @Test
    fun unavailableModelDiscoversLiveModelsSwitchesAndCompletes() = runTest {
        val triedModels = mutableListOf<String>()
        var switchedTo: String? = null

        val outcome = GeminiModelFallbackExecutor.execute(
            preferredModel = "gemini-3.8-flash",
            fetchLiveModels = {
                Result.success(
                    listOf(
                        "gemini-2.5-flash-tts",
                        "gemini-3.5-flash",
                        "gemini-2.5-flash",
                    )
                )
            },
            onModelSwitched = { switchedTo = it },
            delayMillis = { },
            attempt = { model ->
                triedModels.add(model)
                when (model) {
                    "gemini-3.8-flash" -> Result.failure(GeminiHttpException(404, "Model not found"))
                    "gemini-3.5-flash" -> Result.success("parsed-result")
                    else -> Result.failure(IllegalStateException("Unexpected model $model"))
                }
            },
        ).getOrThrow()

        assertEquals(listOf("gemini-3.8-flash", "gemini-3.5-flash"), triedModels)
        assertEquals("parsed-result", outcome.value)
        assertEquals("gemini-3.5-flash", outcome.usedModel)
        assertEquals("gemini-3.8-flash", outcome.switchedFrom)
        assertEquals("gemini-3.5-flash", switchedTo)
    }

    @Test
    fun fatalAuthErrorDoesNotCycleModelsOrOverwritePreference() = runTest {
        val triedModels = mutableListOf<String>()
        var switchedTo: String? = null
        var fetchedLiveModels = false

        val result = GeminiModelFallbackExecutor.execute<String>(
            preferredModel = "gemini-3.5-flash",
            fetchLiveModels = {
                fetchedLiveModels = true
                Result.success(listOf("gemini-2.5-flash"))
            },
            onModelSwitched = { switchedTo = it },
            delayMillis = { },
            attempt = { model ->
                triedModels.add(model)
                Result.failure(GeminiHttpException(401, "API key invalid"))
            },
        )

        assertTrue(result.isFailure)
        assertFalse(fetchedLiveModels)
        assertNull(switchedTo)
        assertEquals(listOf("gemini-3.5-flash"), triedModels)
    }

    @Test
    fun malformedJsonOrParseFailureTriggersFallbackSwitch() = runTest {
        val triedModels = mutableListOf<String>()
        var switchedTo: String? = null

        val outcome = GeminiModelFallbackExecutor.execute(
            preferredModel = "gemini-bad-json",
            fetchLiveModels = { Result.success(listOf("gemini-3.5-flash")) },
            onModelSwitched = { switchedTo = it },
            delayMillis = { },
            attempt = { model ->
                triedModels.add(model)
                if (model == "gemini-bad-json") {
                    Result.failure(IllegalStateException("Gemini did not return valid JSON"))
                } else {
                    Result.success("valid-analysis")
                }
            },
        ).getOrThrow()

        assertEquals(listOf("gemini-bad-json", "gemini-bad-json", "gemini-3.5-flash"), triedModels)
        assertEquals("gemini-3.5-flash", switchedTo)
        assertEquals("valid-analysis", outcome.value)
    }

    @Test
    fun suitableModelFilterExcludesNonAnalysisModels() {
        assertTrue(GeminiModelFallbackExecutor.isSuitableAnalysisModel("gemini-3.5-flash"))
        assertTrue(GeminiModelFallbackExecutor.isSuitableAnalysisModel("gemini-3.8-flash"))
        assertTrue(GeminiModelFallbackExecutor.isSuitableAnalysisModel("gemini-2.5-flash-lite"))
        assertFalse(GeminiModelFallbackExecutor.isSuitableAnalysisModel("gemini-2.5-flash-tts"))
        assertFalse(GeminiModelFallbackExecutor.isSuitableAnalysisModel("gemini-embedding-001"))
        assertFalse(GeminiModelFallbackExecutor.isSuitableAnalysisModel("gemini-2.0-flash-live-001"))
        assertFalse(GeminiModelFallbackExecutor.isSuitableAnalysisModel("imagen-3.0-generate-002"))
    }
}
