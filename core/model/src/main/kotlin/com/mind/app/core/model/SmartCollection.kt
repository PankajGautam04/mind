package com.mind.app.core.model

/**
 * Manual, smart rule-based, or AI-suggested collection of memories.
 */
data class SmartCollection(
    val id: String,
    val name: String,
    val description: String? = null,
    val isSmartCollection: Boolean = false,
    val isSuggested: Boolean = false,
    val rule: CollectionRule? = null,
    val coverMemoryId: String? = null,
    val coverThumbnailPath: String? = null,
    val iconName: String? = null,
    val memoryCount: Int = 0,
    val createdAtEpochMillis: Long = 0L,
    val updatedAtEpochMillis: Long = 0L,
)

/**
 * Declarative matching rule for automatic and smart collections.
 */
data class CollectionRule(
    val entities: List<String> = emptyList(),
    val categoryIds: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val keywords: List<String> = emptyList(),
)
