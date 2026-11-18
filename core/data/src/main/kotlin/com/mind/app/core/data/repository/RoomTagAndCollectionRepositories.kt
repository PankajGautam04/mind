package com.mind.app.core.data.repository

import androidx.room.withTransaction
import com.mind.app.core.data.mapper.toDomain
import com.mind.app.core.data.mapper.toJson
import com.mind.app.core.database.MindDatabase
import com.mind.app.core.database.dao.CollectionDao
import com.mind.app.core.database.dao.TagDao
import com.mind.app.core.database.entity.CollectionEntity
import com.mind.app.core.database.entity.CollectionMemoryCrossRef
import com.mind.app.core.database.entity.MemoryTagCrossRef
import com.mind.app.core.database.entity.TagEntity
import com.mind.app.core.domain.repository.CollectionRepository
import com.mind.app.core.domain.repository.TagRepository
import com.mind.app.core.model.CollectionRule
import com.mind.app.core.model.Memory
import com.mind.app.core.model.MetadataSource
import com.mind.app.core.model.SmartCollection
import com.mind.app.core.model.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomTagRepository @Inject constructor(
    private val database: MindDatabase,
    private val tagDao: TagDao,
) : TagRepository {

    override fun observeAllTags(): Flow<List<Tag>> =
        tagDao.observeAllTags().map { list -> list.map { it.toDomain() } }

    override suspend fun setTagsForMemory(memoryId: String, tagNames: List<String>, source: MetadataSource) {
        database.withTransaction {
            tagDao.clearTagsForMemory(memoryId)
            val refs = tagNames
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinctBy { it.lowercase() }
                .map { name ->
                    val tag = getOrCreateTag(name, isAiGenerated = source == MetadataSource.AI)
                    MemoryTagCrossRef(memoryId = memoryId, tagId = tag.id, source = source)
                }
            if (refs.isNotEmpty()) {
                tagDao.upsertMemoryTagRefs(refs)
            }
        }
    }

    override suspend fun addTagToMemory(memoryId: String, tagName: String, source: MetadataSource) {
        val trimmed = tagName.trim()
        if (trimmed.isEmpty()) return
        database.withTransaction {
            val tag = getOrCreateTag(trimmed, isAiGenerated = source == MetadataSource.AI)
            tagDao.upsertMemoryTagRefs(
                listOf(MemoryTagCrossRef(memoryId = memoryId, tagId = tag.id, source = source))
            )
        }
    }

    override suspend fun removeTagFromMemory(memoryId: String, tagId: String) {
        tagDao.removeTagFromMemory(memoryId, tagId)
    }

    private suspend fun getOrCreateTag(name: String, isAiGenerated: Boolean): TagEntity {
        val normalized = name.lowercase()
        val existing = tagDao.findByNormalizedName(normalized)
        if (existing != null) return existing
        val created = TagEntity(
            id = "tag_${UUID.randomUUID()}",
            name = name,
            normalizedName = normalized,
            isAiGenerated = isAiGenerated,
            createdAt = System.currentTimeMillis(),
        )
        tagDao.insertTag(created)
        return tagDao.findByNormalizedName(normalized) ?: created
    }
}

@Singleton
class RoomCollectionRepository @Inject constructor(
    private val collectionDao: CollectionDao,
) : CollectionRepository {

    override fun observeCollections(): Flow<List<SmartCollection>> =
        collectionDao.observeCollectionsWithCount().map { list -> list.map { it.toDomain() } }

    override fun observeMemoriesInCollection(collectionId: String): Flow<List<Memory>> =
        collectionDao.observeMemoriesInCollection(collectionId).map { list -> list.map { it.toDomain() } }

    override suspend fun createCollection(
        name: String,
        description: String?,
        isSmartCollection: Boolean,
        rule: CollectionRule?,
    ): SmartCollection {
        val now = System.currentTimeMillis()
        val entity = CollectionEntity(
            id = "col_${UUID.randomUUID()}",
            name = name.trim(),
            description = description?.trim()?.ifEmpty { null },
            isSmartCollection = isSmartCollection,
            isSuggested = false,
            ruleJson = rule?.toJson(),
            coverMemoryId = null,
            iconName = null,
            createdAt = now,
            updatedAt = now,
        )
        collectionDao.upsertCollection(entity)
        return SmartCollection(
            id = entity.id,
            name = entity.name,
            description = entity.description,
            isSmartCollection = entity.isSmartCollection,
            isSuggested = false,
            rule = rule,
            memoryCount = 0,
            createdAtEpochMillis = now,
            updatedAtEpochMillis = now,
        )
    }

    override suspend fun addMemoryToCollection(collectionId: String, memoryId: String, isAutoAdded: Boolean) {
        collectionDao.insertMemoryRef(
            CollectionMemoryCrossRef(
                collectionId = collectionId,
                memoryId = memoryId,
                addedAt = System.currentTimeMillis(),
                isAutoAdded = isAutoAdded,
            )
        )
    }

    override suspend fun removeMemoryFromCollection(collectionId: String, memoryId: String) {
        collectionDao.removeMemoryRef(collectionId, memoryId)
    }

    override suspend fun acceptSuggestedCollection(collectionId: String) {
        collectionDao.acceptSuggestedCollection(collectionId, System.currentTimeMillis())
    }

    override suspend fun deleteCollection(collectionId: String) {
        collectionDao.deleteCollection(collectionId)
    }
}
