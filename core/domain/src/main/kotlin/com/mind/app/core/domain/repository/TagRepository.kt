package com.mind.app.core.domain.repository

import com.mind.app.core.model.MetadataSource
import com.mind.app.core.model.Tag
import kotlinx.coroutines.flow.Flow

/**
 * Contract for managing searchable tags and memory-tag associations.
 */
interface TagRepository {
    fun observeAllTags(): Flow<List<Tag>>
    suspend fun setTagsForMemory(memoryId: String, tagNames: List<String>, source: MetadataSource)
    suspend fun addTagToMemory(memoryId: String, tagName: String, source: MetadataSource)
    suspend fun removeTagFromMemory(memoryId: String, tagId: String)
}
