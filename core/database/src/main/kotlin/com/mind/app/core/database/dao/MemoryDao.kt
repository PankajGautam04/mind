package com.mind.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.mind.app.core.database.entity.MemoryCategoryCrossRef
import com.mind.app.core.database.entity.MemoryEntity
import com.mind.app.core.database.model.MemoryWithDetails
import com.mind.app.core.model.AiProcessingStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Transaction
    @Query(
        """
        SELECT * FROM memories
        WHERE is_deleted = 0
        ORDER BY captured_at DESC
        LIMIT :limit
        """
    )
    fun observeRecentMemories(limit: Int): Flow<List<MemoryWithDetails>>

    @Transaction
    @Query(
        """
        SELECT * FROM memories
        WHERE is_deleted = 0 AND is_favourite = 1
        ORDER BY captured_at DESC
        """
    )
    fun observeFavouriteMemories(): Flow<List<MemoryWithDetails>>

    @Transaction
    @Query(
        """
        SELECT DISTINCT m.* FROM memories m
        LEFT JOIN memory_category_cross_ref mc ON mc.memory_id = m.id
        WHERE m.is_deleted = 0
          AND (m.category_id = :categoryId OR m.subcategory_id = :categoryId OR mc.category_id = :categoryId)
        ORDER BY m.captured_at DESC
        """
    )
    fun observeMemoriesByCategory(categoryId: String): Flow<List<MemoryWithDetails>>

    @Transaction
    @Query("SELECT * FROM memories WHERE id = :memoryId AND is_deleted = 0 LIMIT 1")
    fun observeMemoryById(memoryId: String): Flow<MemoryWithDetails?>

    @Transaction
    @Query("SELECT * FROM memories WHERE id = :memoryId AND is_deleted = 0 LIMIT 1")
    suspend fun getMemoryWithDetailsById(memoryId: String): MemoryWithDetails?

    @Transaction
    @Query("SELECT * FROM memories WHERE is_deleted = 0 ORDER BY captured_at DESC LIMIT :limit")
    suspend fun getRecentMemoryDetailsSnapshot(limit: Int): List<MemoryWithDetails>

    @Query("SELECT id FROM memories WHERE is_deleted = 0 AND ai_status IN ('FAILED', 'PENDING', 'SKIPPED_LOCAL_ONLY')")
    suspend fun getReanalyzableMemoryIds(): List<String>

    @Query("SELECT id FROM memories WHERE is_deleted = 0 AND ai_status = 'FAILED'")
    suspend fun getFailedMemoryIds(): List<String>

    @Query("SELECT COUNT(*) FROM memories WHERE is_deleted = 0")
    suspend fun getTotalMemoryCountOnce(): Int

    @Query("SELECT * FROM memories WHERE id = :memoryId LIMIT 1")
    suspend fun getMemoryEntityById(memoryId: String): MemoryEntity?

    @Query("SELECT * FROM memories WHERE id IN (:memoryIds)")
    suspend fun getMemoryEntitiesByIds(memoryIds: Collection<String>): List<MemoryEntity>

    @Query("SELECT COUNT(*) FROM memories WHERE is_deleted = 0")
    fun observeTotalMemoryCount(): Flow<Int>

    @Transaction
    @Query(
        """
        SELECT DISTINCT m.* FROM memories m
        LEFT JOIN memory_category_cross_ref mc ON mc.memory_id = m.id
        WHERE m.is_deleted = 0
          AND (:categoryId IS NULL OR m.category_id = :categoryId OR m.subcategory_id = :categoryId OR mc.category_id = :categoryId)
          AND (:sourcePackageName IS NULL OR m.source_package_name = :sourcePackageName)
          AND (:fromEpochMillis IS NULL OR m.captured_at >= :fromEpochMillis)
          AND (:toEpochMillis IS NULL OR m.captured_at <= :toEpochMillis)
          AND (:favouritesOnly = 0 OR m.is_favourite = 1)
        ORDER BY m.captured_at DESC
        """
    )
    fun observeFilteredMemories(
        categoryId: String?,
        sourcePackageName: String?,
        fromEpochMillis: Long?,
        toEpochMillis: Long?,
        favouritesOnly: Boolean,
    ): Flow<List<MemoryWithDetails>>

    @Transaction
    @Query(
        """
        SELECT DISTINCT m.* FROM memories m
        JOIN memories_fts fts ON m.rowid = fts.rowid
        LEFT JOIN memory_category_cross_ref mc ON mc.memory_id = m.id
        WHERE memories_fts MATCH :ftsQuery
          AND m.is_deleted = 0
          AND (:categoryId IS NULL OR m.category_id = :categoryId OR m.subcategory_id = :categoryId OR mc.category_id = :categoryId)
          AND (:sourcePackageName IS NULL OR m.source_package_name = :sourcePackageName)
          AND (:fromEpochMillis IS NULL OR m.captured_at >= :fromEpochMillis)
          AND (:toEpochMillis IS NULL OR m.captured_at <= :toEpochMillis)
          AND (:favouritesOnly = 0 OR m.is_favourite = 1)
        ORDER BY m.captured_at DESC
        """
    )
    fun searchMemoriesFts(
        ftsQuery: String,
        categoryId: String?,
        sourcePackageName: String?,
        fromEpochMillis: Long?,
        toEpochMillis: Long?,
        favouritesOnly: Boolean,
    ): Flow<List<MemoryWithDetails>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMemory(memory: MemoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategoryRefs(refs: List<MemoryCategoryCrossRef>)

    @Query("DELETE FROM memory_category_cross_ref WHERE memory_id = :memoryId")
    suspend fun clearCategoryRefsForMemory(memoryId: String)

    @Query("UPDATE memories SET is_favourite = :isFavourite, updated_at = :updatedAt WHERE id = :memoryId")
    suspend fun updateFavourite(memoryId: String, isFavourite: Boolean, updatedAt: Long)

    @Query(
        """
        UPDATE memories
        SET user_title = :userTitle,
            user_notes = :userNotes,
            category_id = :categoryId,
            subcategory_id = :subcategoryId,
            updated_at = :updatedAt
        WHERE id = :memoryId
        """
    )
    suspend fun updateUserMetadata(
        memoryId: String,
        userTitle: String?,
        userNotes: String?,
        categoryId: String?,
        subcategoryId: String?,
        updatedAt: Long,
    )

    @Query("UPDATE memories SET ai_status = :status, updated_at = :updatedAt WHERE id = :memoryId")
    suspend fun updateAiStatus(memoryId: String, status: AiProcessingStatus, updatedAt: Long)

    @Query(
        """
        UPDATE memories
        SET extracted_text = :extractedText, updated_at = :updatedAt
        WHERE id = :memoryId AND (extracted_text IS NULL OR extracted_text = '')
        """
    )
    suspend fun updateExtractedTextIfBlank(memoryId: String, extractedText: String, updatedAt: Long)

    @Query("UPDATE memories SET is_deleted = 1, deleted_at = :deletedAt, updated_at = :deletedAt WHERE id = :memoryId")
    suspend fun softDelete(memoryId: String, deletedAt: Long)

    @Query("UPDATE memories SET is_deleted = 0, deleted_at = NULL, updated_at = :updatedAt WHERE id = :memoryId")
    suspend fun restore(memoryId: String, updatedAt: Long)

    @Query("DELETE FROM memories WHERE id = :memoryId")
    suspend fun hardDelete(memoryId: String)

    @Query("DELETE FROM memories WHERE id IN (:memoryIds)")
    suspend fun hardDeleteBatch(memoryIds: Collection<String>)

    @Query("DELETE FROM memories")
    suspend fun deleteAll()
}
