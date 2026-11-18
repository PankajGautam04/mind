package com.mind.app.core.database.converter

import androidx.room.TypeConverter
import com.mind.app.core.model.AiJobStatus
import com.mind.app.core.model.AiProcessingStatus
import com.mind.app.core.model.CaptureMethod
import com.mind.app.core.model.ConnectionType
import com.mind.app.core.model.ContentType
import com.mind.app.core.model.MetadataSource
import kotlinx.serialization.json.Json

/**
 * Room type converters for structured metadata lists and domain enums.
 */
class MindTypeConverters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return json.encodeToString(value.orEmpty())
    }

    @TypeConverter
    fun toStringList(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<String>>(raw) }.getOrDefault(emptyList())
    }

    @TypeConverter
    fun fromContentType(value: ContentType): String = value.name

    @TypeConverter
    fun toContentType(raw: String): ContentType =
        enumValueOfOrDefault(raw, ContentType.OTHER)

    @TypeConverter
    fun fromAiProcessingStatus(value: AiProcessingStatus): String = value.name

    @TypeConverter
    fun toAiProcessingStatus(raw: String): AiProcessingStatus =
        enumValueOfOrDefault(raw, AiProcessingStatus.PENDING)

    @TypeConverter
    fun fromCaptureMethod(value: CaptureMethod): String = value.name

    @TypeConverter
    fun toCaptureMethod(raw: String): CaptureMethod =
        enumValueOfOrDefault(raw, CaptureMethod.IN_APP)

    @TypeConverter
    fun fromAiJobStatus(value: AiJobStatus): String = value.name

    @TypeConverter
    fun toAiJobStatus(raw: String): AiJobStatus =
        enumValueOfOrDefault(raw, AiJobStatus.QUEUED)

    @TypeConverter
    fun fromConnectionType(value: ConnectionType): String = value.name

    @TypeConverter
    fun toConnectionType(raw: String): ConnectionType =
        enumValueOfOrDefault(raw, ConnectionType.RELATED_TOPIC)

    @TypeConverter
    fun fromMetadataSource(value: MetadataSource): String = value.name

    @TypeConverter
    fun toMetadataSource(raw: String): MetadataSource =
        enumValueOfOrDefault(raw, MetadataSource.AI)

    private inline fun <reified T : Enum<T>> enumValueOfOrDefault(raw: String, fallback: T): T {
        return runCatching { enumValueOf<T>(raw) }.getOrDefault(fallback)
    }
}
