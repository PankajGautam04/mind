package com.mind.app.core.model

/**
 * Storage breakdown reported on the Settings -> Storage screen.
 */
data class StorageStats(
    val totalMemoryCount: Int = 0,
    val imageBytes: Long = 0L,
    val thumbnailBytes: Long = 0L,
    val databaseBytes: Long = 0L,
    val cacheBytes: Long = 0L,
) {
    val totalBytes: Long
        get() = imageBytes + thumbnailBytes + databaseBytes + cacheBytes
}

/**
 * Query parameters combining full-text, semantic, category, date, and source filters.
 */
data class MemorySearchFilter(
    val query: String = "",
    val categoryId: String? = null,
    val tagNames: Set<String> = emptySet(),
    val contentTypes: Set<ContentType> = emptySet(),
    val sourcePackageName: String? = null,
    val fromEpochMillis: Long? = null,
    val toEpochMillis: Long? = null,
    val favouritesOnly: Boolean = false,
)
