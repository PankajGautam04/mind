package com.mind.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.mind.app.core.database.entity.CollectionEntity
import com.mind.app.core.database.entity.CollectionMemoryCrossRef
import com.mind.app.core.database.entity.MemoryTagCrossRef
import com.mind.app.core.database.entity.TagEntity
import com.mind.app.core.database.model.CollectionWithCount
import com.mind.app.core.database.model.MemoryWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE ASC")
    fun observeAllTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE normalized_name = :normalizedName LIMIT 1")
    suspend fun findByNormalizedName(normalizedName: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMemoryTagRefs(refs: List<MemoryTagCrossRef>)

    @Query("DELETE FROM memory_tag_cross_ref WHERE memory_id = :memoryId")
    suspend fun clearTagsForMemory(memoryId: String)

    @Query("DELETE FROM memory_tag_cross_ref WHERE memory_id = :memoryId AND tag_id = :tagId")
    suspend fun removeTagFromMemory(memoryId: String, tagId: String)
}

@Dao
interface CollectionDao {
    @Query(
        """
        SELECT col.*,
               COUNT(DISTINCT m.id) AS memory_count,
               COALESCE(
                   (SELECT cover_m.thumbnail_path FROM memories cover_m WHERE cover_m.id = col.cover_memory_id AND cover_m.is_deleted = 0 LIMIT 1),
                   MAX(m.thumbnail_path)
               ) AS cover_thumbnail_path
        FROM collections col
        LEFT JOIN collection_memory_cross_ref cm ON cm.collection_id = col.id
        LEFT JOIN memories m ON m.id = cm.memory_id AND m.is_deleted = 0
        GROUP BY col.id
        ORDER BY col.is_suggested DESC, col.updated_at DESC, col.name COLLATE NOCASE ASC
        """
    )
    fun observeCollectionsWithCount(): Flow<List<CollectionWithCount>>

    @Transaction
    @Query(
        """
        SELECT m.* FROM memories m
        INNER JOIN collection_memory_cross_ref cm ON cm.memory_id = m.id
        WHERE cm.collection_id = :collectionId AND m.is_deleted = 0
        ORDER BY cm.added_at DESC
        """
    )
    fun observeMemoriesInCollection(collectionId: String): Flow<List<MemoryWithDetails>>

    @Transaction
    @Query(
        """
        SELECT col.*,
               COUNT(DISTINCT m.id) AS memory_count,
               MAX(m.thumbnail_path) AS cover_thumbnail_path
        FROM collections col
        INNER JOIN collection_memory_cross_ref target_cm ON target_cm.collection_id = col.id AND target_cm.memory_id = :memoryId
        LEFT JOIN collection_memory_cross_ref cm ON cm.collection_id = col.id
        LEFT JOIN memories m ON m.id = cm.memory_id AND m.is_deleted = 0
        GROUP BY col.id
        ORDER BY col.name COLLATE NOCASE ASC
        """
    )
    fun observeCollectionsForMemory(memoryId: String): Flow<List<CollectionWithCount>>

    @Query("SELECT * FROM collections WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun findByName(name: String): CollectionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCollection(collection: CollectionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemoryRef(ref: CollectionMemoryCrossRef)

    @Query("DELETE FROM collection_memory_cross_ref WHERE collection_id = :collectionId AND memory_id = :memoryId")
    suspend fun removeMemoryRef(collectionId: String, memoryId: String)

    @Query("UPDATE collections SET is_suggested = 0, updated_at = :updatedAt WHERE id = :collectionId")
    suspend fun acceptSuggestedCollection(collectionId: String, updatedAt: Long)

    @Query("DELETE FROM collections WHERE id = :collectionId")
    suspend fun deleteCollection(collectionId: String)
}
