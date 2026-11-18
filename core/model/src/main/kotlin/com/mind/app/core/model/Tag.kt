package com.mind.app.core.model

/**
 * Searchable tag attached to one or more memories.
 */
data class Tag(
    val id: String,
    val name: String,
    val isAiGenerated: Boolean = true,
    val createdAtEpochMillis: Long = 0L,
)

enum class MetadataSource {
    AI,
    USER,
}
