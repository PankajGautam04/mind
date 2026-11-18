package com.mind.app.core.data.ai

import android.util.Base64
import com.mind.app.core.model.GeminiModelInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Typed HTTP exception carrying the status code returned by Gemini or a custom proxy.
 */
class GeminiHttpException(
    val statusCode: Int,
    message: String,
) : IllegalStateException(message)

/**
 * Direct REST client for Google Gemini v1beta (or a user-configured secure backend proxy).
 */
@Singleton
class GeminiRestClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun generateStructuredAnalysis(
        baseUrl: String,
        apiKey: String?,
        modelId: String,
        prompt: String,
        mediaFile: File?,
        mediaMimeType: String?,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val normalizedBase = normalizeBaseUrl(baseUrl)
            val cleanModel = modelId.removePrefix("models/").trim()
            val url = "${normalizedBase}v1beta/models/$cleanModel:generateContent"

            val payload = buildContentPayload(
                prompt = prompt,
                mediaFile = mediaFile,
                mediaMimeType = mediaMimeType,
                temperature = 0.2,
                responseMimeType = "application/json",
            )

            executeGenerateContent(url, apiKey, payload)
        }
    }

    suspend fun generateTextAnswer(
        baseUrl: String,
        apiKey: String?,
        modelId: String,
        prompt: String,
        mediaFile: File? = null,
        mediaMimeType: String? = null,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val normalizedBase = normalizeBaseUrl(baseUrl)
            val cleanModel = modelId.removePrefix("models/").trim()
            val url = "${normalizedBase}v1beta/models/$cleanModel:generateContent"

            val payload = buildContentPayload(
                prompt = prompt,
                mediaFile = mediaFile,
                mediaMimeType = mediaMimeType,
                temperature = 0.3,
                responseMimeType = null,
            )

            executeGenerateContent(url, apiKey, payload)
        }
    }

    private fun buildContentPayload(
        prompt: String,
        mediaFile: File?,
        mediaMimeType: String?,
        temperature: Double,
        responseMimeType: String?,
    ): String {
        val partsArray = buildJsonArray {
            add(buildJsonObject { put("text", prompt) })
            if (mediaFile != null && mediaFile.exists() && !mediaMimeType.isNullOrBlank()) {
                val bytes = mediaFile.readBytes()
                val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
                add(
                    buildJsonObject {
                        put(
                            "inline_data",
                            buildJsonObject {
                                put("mime_type", mediaMimeType)
                                put("data", encoded)
                            }
                        )
                    }
                )
            }
        }

        val payload = buildJsonObject {
            put(
                "contents",
                buildJsonArray {
                    add(buildJsonObject { put("parts", partsArray) })
                }
            )
            put(
                "generationConfig",
                buildJsonObject {
                    put("temperature", temperature)
                    if (responseMimeType != null) {
                        put("responseMimeType", responseMimeType)
                    }
                }
            )
        }
        return payload.toString()
    }

    suspend fun embedText(
        baseUrl: String,
        apiKey: String?,
        text: String,
    ): Result<Pair<String, FloatArray>> = withContext(Dispatchers.IO) {
        val cleanText = text.trim().take(4000)
        if (cleanText.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Cannot embed blank text"))
        }

        val models = listOf("gemini-embedding-2", "gemini-embedding-001")
        var lastError: Throwable? = null
        for (embModel in models) {
            val attempt = runCatching {
                val normalizedBase = normalizeBaseUrl(baseUrl)
                val url = "${normalizedBase}v1beta/models/$embModel:embedContent"
                val payload = buildJsonObject {
                    put("model", "models/$embModel")
                    put(
                        "content",
                        buildJsonObject {
                            put(
                                "parts",
                                buildJsonArray {
                                    add(buildJsonObject { put("text", cleanText) })
                                }
                            )
                        }
                    )
                }
                val body = payload.toString().toRequestBody(JSON_MEDIA_TYPE)
                val builder = Request.Builder()
                    .url(url)
                    .post(body)
                    .header("Content-Type", "application/json")
                if (!apiKey.isNullOrBlank()) {
                    builder.header("x-goog-api-key", apiKey)
                }

                okHttpClient.newCall(builder.build()).execute().use { response ->
                    val responseText = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        throw GeminiHttpException(response.code, parseApiError(response.code, responseText))
                    }
                    val root = json.parseToJsonElement(responseText).jsonObject
                    val valuesArray = root["embedding"]?.jsonObject?.get("values")?.jsonArray
                        ?: throw IllegalStateException("Embedding response missing vector values")
                    val floats = FloatArray(valuesArray.size) { idx ->
                        valuesArray[idx].jsonPrimitive.floatOrNull ?: 0f
                    }
                    if (floats.isEmpty()) {
                        throw IllegalStateException("Empty embedding vector")
                    }
                    embModel to floats
                }
            }
            if (attempt.isSuccess) return@withContext attempt
            lastError = attempt.exceptionOrNull()
        }

        Result.failure(lastError ?: IllegalStateException("Embedding failed"))
    }

    suspend fun listGenerateContentModels(
        baseUrl: String,
        apiKey: String?,
    ): Result<List<GeminiModelInfo>> = withContext(Dispatchers.IO) {
        runCatching {
            val normalizedBase = normalizeBaseUrl(baseUrl)
            val url = "${normalizedBase}v1beta/models"
            val builder = Request.Builder().url(url).get()
            if (!apiKey.isNullOrBlank()) {
                builder.header("x-goog-api-key", apiKey)
            }

            okHttpClient.newCall(builder.build()).execute().use { response ->
                val bodyString = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw GeminiHttpException(response.code, parseApiError(response.code, bodyString))
                }
                val root = json.parseToJsonElement(bodyString).jsonObject
                val models = root["models"]?.jsonArray ?: JsonArray(emptyList())
                models.mapNotNull { elem ->
                    val obj = elem.jsonObject
                    val methods = obj["supportedGenerationMethods"]?.jsonArray
                        ?.map { it.jsonPrimitive.content }
                        .orEmpty()
                    if ("generateContent" !in methods) return@mapNotNull null
                    val rawName = obj["name"]?.jsonPrimitive?.content ?: return@mapNotNull null
                    val id = rawName.removePrefix("models/")
                    if (!id.startsWith("gemini")) return@mapNotNull null
                    GeminiModelInfo(
                        id = id,
                        displayName = obj["displayName"]?.jsonPrimitive?.content ?: id,
                        description = obj["description"]?.jsonPrimitive?.content.orEmpty(),
                    )
                }.sortedBy { modelSortPriority(it.id) }
            }
        }
    }

    private fun executeGenerateContent(
        url: String,
        apiKey: String?,
        jsonBody: String,
    ): String {
        val body = jsonBody.toRequestBody(JSON_MEDIA_TYPE)
        val builder = Request.Builder()
            .url(url)
            .post(body)
            .header("Content-Type", "application/json")
        if (!apiKey.isNullOrBlank()) {
            builder.header("x-goog-api-key", apiKey)
        }

        okHttpClient.newCall(builder.build()).execute().use { response ->
            val responseText = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw GeminiHttpException(response.code, parseApiError(response.code, responseText))
            }
            val root = json.parseToJsonElement(responseText).jsonObject
            val candidates = root["candidates"]?.jsonArray
            val firstCandidate = candidates?.firstOrNull()?.jsonObject
                ?: throw IllegalStateException("Gemini returned no candidates")
            val parts = firstCandidate["content"]?.jsonObject?.get("parts")?.jsonArray
                ?: throw IllegalStateException("Gemini returned empty content parts")
            return parts.mapNotNull { part ->
                (part as? JsonObject)?.get("text")?.let { it as? JsonPrimitive }?.content
            }.joinToString("\n")
        }
    }

    private fun parseApiError(httpCode: Int, rawBody: String): String {
        val message = runCatching {
            json.parseToJsonElement(rawBody).jsonObject["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
        }.getOrNull()
        return if (!message.isNullOrBlank()) {
            "Gemini API ($httpCode): $message"
        } else {
            "Gemini API request failed with HTTP $httpCode"
        }
    }

    private fun normalizeBaseUrl(customUrl: String): String {
        val trimmed = customUrl.trim()
        val effective = if (trimmed.isEmpty()) DEFAULT_BASE_URL else trimmed
        return if (effective.endsWith('/')) effective else "$effective/"
    }

    private fun modelSortPriority(id: String): Int = when {
        id == "gemini-3.8-flash" -> 0
        id == "gemini-3.5-flash" -> 1
        id == "gemini-3.5-flash-lite" -> 2
        id.startsWith("gemini-4-") -> 3
        id.startsWith("gemini-3.") && "flash" in id -> 4
        id.startsWith("gemini-2.5-flash-lite") -> 5
        id.startsWith("gemini-2.5-flash") -> 6
        id.startsWith("gemini-3.") -> 7
        id.startsWith("gemini-2.5-") -> 8
        id.startsWith("gemini-2.") -> 9
        else -> 10
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com/"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
