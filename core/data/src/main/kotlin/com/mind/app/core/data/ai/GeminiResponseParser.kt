package com.mind.app.core.data.ai

import com.mind.app.core.model.ContentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Validated structured metadata extracted from Gemini's JSON response.
 */
data class ParsedMemoryAnalysis(
    val title: String?,
    val contentType: ContentType?,
    val primarySubject: String?,
    val categoryName: String?,
    val subcategoryName: String?,
    val tags: List<String>,
    val description: String?,
    val entities: List<String>,
    val objects: List<String>,
    val extractedText: String?,
    val mainTopic: String?,
    val contextSummary: String?,
    val confidence: Float?,
    val suggestedCollection: String?,
)

@Serializable
private data class GeminiAnalysisJson(
    val title: String? = null,
    @SerialName("content_type")
    val contentType: String? = null,
    @SerialName("primary_subject")
    val primarySubject: String? = null,
    val category: String? = null,
    val subcategory: String? = null,
    val tags: List<String> = emptyList(),
    val description: String? = null,
    val entities: List<String> = emptyList(),
    val objects: List<String> = emptyList(),
    @SerialName("extracted_text")
    val extractedText: String? = null,
    @SerialName("main_topic")
    val mainTopic: String? = null,
    val context: String? = null,
    val confidence: Float? = null,
    @SerialName("suggested_collection")
    val suggestedCollection: String? = null,
)

/**
 * Defensively extracts and validates structured JSON from Gemini outputs, handling code fences,
 * missing fields, duplicates, and malformed values safely.
 */
object GeminiResponseParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    fun parseAnalysis(rawText: String): Result<ParsedMemoryAnalysis> {
        val cleaned = extractJsonObject(rawText)
            ?: return Result.failure(IllegalArgumentException("Gemini did not return a JSON object"))

        return runCatching {
            val dto = json.decodeFromString<GeminiAnalysisJson>(cleaned)
            ParsedMemoryAnalysis(
                title = dto.title?.trim()?.takeIf { it.isNotEmpty() }?.take(120),
                contentType = mapContentType(dto.contentType),
                primarySubject = dto.primarySubject?.trim()?.takeIf { it.isNotEmpty() },
                categoryName = dto.category?.trim()?.takeIf { it.isNotEmpty() },
                subcategoryName = dto.subcategory?.trim()?.takeIf { it.isNotEmpty() },
                tags = cleanList(dto.tags, maxItems = 12),
                description = dto.description?.trim()?.takeIf { it.isNotEmpty() },
                entities = cleanList(dto.entities, maxItems = 12),
                objects = cleanList(dto.objects, maxItems = 12),
                extractedText = dto.extractedText?.trim()?.takeIf { it.isNotEmpty() },
                mainTopic = dto.mainTopic?.trim()?.takeIf { it.isNotEmpty() },
                contextSummary = dto.context?.trim()?.takeIf { it.isNotEmpty() },
                confidence = dto.confidence?.coerceIn(0f, 1f),
                suggestedCollection = dto.suggestedCollection?.trim()?.takeIf { it.isNotEmpty() },
            )
        }
    }

    internal fun extractJsonObject(raw: String): String? {
        val trimmed = raw.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return trimmed.substring(start, end + 1)
    }

    private fun cleanList(items: List<String>, maxItems: Int): List<String> {
        return items
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
            .take(maxItems)
    }

    private fun mapContentType(raw: String?): ContentType? {
        val normalized = raw?.trim()?.lowercase() ?: return null
        return when {
            normalized.contains("product") || normalized.contains("shopping") -> ContentType.PRODUCT
            normalized.contains("error") || normalized.contains("stack") || normalized.contains("log") -> ContentType.ERROR_LOG
            normalized.contains("place") || normalized.contains("restaurant") || normalized.contains("hotel") -> ContentType.PLACE
            normalized.contains("quote") -> ContentType.QUOTE
            normalized.contains("voice") || normalized.contains("audio") -> ContentType.VOICE_NOTE
            normalized.contains("url") || normalized.contains("link") || normalized.contains("web") -> ContentType.URL
            normalized.contains("doc") || normalized.contains("receipt") || normalized.contains("article") -> ContentType.DOCUMENT
            normalized.contains("screen") -> ContentType.SCREENSHOT
            normalized.contains("photo") || normalized.contains("image") -> ContentType.IMAGE
            normalized.contains("text") || normalized.contains("note") -> ContentType.TEXT
            else -> null
        }
    }
}
