package com.mind.app.core.data.ai

import com.mind.app.core.model.UserPreferences
import kotlinx.coroutines.delay
import java.io.IOException
import java.net.ConnectException
import java.net.UnknownHostException

/**
 * Coordinates automatic transient retries, live Gemini model discovery, and persistent
 * model switching whenever the configured Gemini model is unavailable, rate-limited,
 * overloaded, or returns malformed output.
 */
object GeminiModelFallbackExecutor {

    private const val TRANSIENT_RETRY_DELAY_MS = 900L
    private const val MAX_FALLBACK_MODELS = 5

    private val CURATED_FALLBACK_MODELS = listOf(
        "gemini-3.5-flash",
        "gemini-3.8-flash",
        "gemini-3.5-flash-lite",
        "gemini-2.5-flash",
        "gemini-2.5-flash-lite",
        "gemini-3.5-pro",
        "gemini-2.5-pro",
    )

    data class ExecutionOutcome<T>(
        val value: T,
        val usedModel: String,
        val switchedFrom: String? = null,
    )

    suspend fun <T> execute(
        preferredModel: String,
        fetchLiveModels: suspend () -> Result<List<String>>,
        onModelSwitched: suspend (newModelId: String) -> Unit,
        delayMillis: suspend (Long) -> Unit = { delay(it) },
        attempt: suspend (modelId: String) -> Result<T>,
    ): Result<ExecutionOutcome<T>> {
        val primary = preferredModel.removePrefix("models/").trim()
            .ifEmpty { UserPreferences.DEFAULT_GEMINI_MODEL }

        val primaryResult = runWithTransientRetry(
            modelId = primary,
            delayMillis = delayMillis,
            attempt = attempt,
        )
        if (primaryResult.isSuccess) {
            return Result.success(
                ExecutionOutcome(
                    value = primaryResult.getOrThrow(),
                    usedModel = primary,
                    switchedFrom = null,
                )
            )
        }

        val primaryError = primaryResult.exceptionOrNull()!!
        if (isFatalAuthError(primaryError)) {
            return Result.failure(primaryError)
        }

        val liveModelsResult = fetchLiveModels()
        val liveModels = liveModelsResult.getOrNull()?.filter(::isSuitableAnalysisModel).orEmpty()
        val candidates = buildCandidateQueue(primary = primary, liveModels = liveModels)

        var lastError: Throwable = primaryError
        for (candidate in candidates) {
            val candidateResult = runCatchingOrFailure { attempt(candidate) }
            if (candidateResult.isSuccess) {
                val shouldSwitch = shouldSwitchSavedModel(
                    primaryError = primaryError,
                    primaryModel = primary,
                    liveModels = if (liveModelsResult.isSuccess) liveModels else null,
                )
                if (shouldSwitch) {
                    runCatching { onModelSwitched(candidate) }
                }
                return Result.success(
                    ExecutionOutcome(
                        value = candidateResult.getOrThrow(),
                        usedModel = candidate,
                        switchedFrom = if (shouldSwitch) primary else null,
                    )
                )
            }
            val candidateError = candidateResult.exceptionOrNull()!!
            if (isFatalAuthError(candidateError)) {
                return Result.failure(candidateError)
            }
            lastError = candidateError
        }

        return Result.failure(lastError)
    }

    internal fun isSuitableAnalysisModel(modelId: String): Boolean {
        val normalized = modelId.removePrefix("models/").lowercase().trim()
        if (!normalized.startsWith("gemini")) return false
        val excludedTokens = listOf(
            "embedding",
            "imagen",
            "veo",
            "aqa",
            "tts",
            "robotics",
            "computer-use",
            "native-audio",
            "dialog",
            "live",
            "bisect",
            "image-generation",
        )
        return excludedTokens.none { token -> token in normalized }
    }

    private suspend fun <T> runWithTransientRetry(
        modelId: String,
        delayMillis: suspend (Long) -> Unit,
        attempt: suspend (modelId: String) -> Result<T>,
    ): Result<T> {
        val first = runCatchingOrFailure { attempt(modelId) }
        if (first.isSuccess) return first
        val firstError = first.exceptionOrNull()!!
        if (!isTransientError(firstError)) return first

        delayMillis(TRANSIENT_RETRY_DELAY_MS)
        return runCatchingOrFailure { attempt(modelId) }
    }

    private suspend inline fun <T> runCatchingOrFailure(
        block: suspend () -> Result<T>,
    ): Result<T> = try {
        block()
    } catch (t: Throwable) {
        Result.failure(t)
    }

    private fun buildCandidateQueue(primary: String, liveModels: List<String>): List<String> {
        val combined = LinkedHashSet<String>()
        liveModels.forEach { id ->
            val clean = id.removePrefix("models/").trim()
            if (clean.isNotEmpty() && clean != primary) {
                combined.add(clean)
            }
        }
        CURATED_FALLBACK_MODELS.forEach { fallback ->
            if (fallback != primary) {
                combined.add(fallback)
            }
        }
        return combined.take(MAX_FALLBACK_MODELS)
    }

    private fun isFatalAuthError(error: Throwable): Boolean {
        if (error !is GeminiHttpException) return false
        if (error.statusCode == 401) return true
        if (error.statusCode == 403) {
            val msg = error.message.orEmpty().lowercase()
            return "api key" in msg || "api_key" in msg || ("permission" in msg && "model" !in msg)
        }
        return false
    }

    private fun isTransientError(error: Throwable): Boolean {
        if (error is IOException) return true
        if (error is GeminiHttpException) {
            return error.statusCode in setOf(408, 429, 500, 502, 503, 504)
        }
        val msg = error.message.orEmpty().lowercase()
        return "no candidates" in msg ||
            "empty content" in msg ||
            "valid json" in msg ||
            "timeout" in msg
    }

    private fun shouldSwitchSavedModel(
        primaryError: Throwable,
        primaryModel: String,
        liveModels: List<String>?,
    ): Boolean {
        if (liveModels != null && liveModels.isNotEmpty() && primaryModel !in liveModels) {
            return true
        }
        if (primaryError is GeminiHttpException) {
            return primaryError.statusCode in setOf(400, 403, 404, 410, 429, 500, 502, 503, 504)
        }
        if (primaryError is UnknownHostException || primaryError is ConnectException) {
            return false
        }
        val msg = primaryError.message.orEmpty().lowercase()
        return "model" in msg || "not found" in msg || "unsupported" in msg || "quota" in msg ||
            "no candidates" in msg || "valid json" in msg
    }
}
