package com.mind.app.core.domain.repository

import com.mind.app.core.model.AiProcessingStatus
import com.mind.app.core.model.Memory
import com.mind.app.core.model.MemoryDetail
import com.mind.app.core.model.StorageStats
import kotlinx.coroutines.flow.Flow

/**
 * Contract for reading, organizing, and managing persisted digital memories.
 */
interface MemoryRepository {
    fun observeRecentMemories(limit: Int = 50): Flow<List<Memory>>
    fun observeFavouriteMemories(): Flow<List<Memory>>
    fun observeMemoriesByCategory(categoryId: String): Flow<List<Memory>>
    fun observeMemoryDetail(memoryId: String): Flow<MemoryDetail?>
    fun observeTotalMemoryCount(): Flow<Int>

    suspend fun getMemoryById(memoryId: String): Memory?
    suspend fun getRecentMemoriesSnapshot(limit: Int = 50): List<Memory>
    suspend fun getStorageStats(): StorageStats
    suspend fun clearCacheStorage()

    suspend fun upsertMemory(memory: Memory)
    suspend fun updateFavourite(memoryId: String, isFavourite: Boolean)
    suspend fun updateUserMetadata(
        memoryId: String,
        title: String?,
        notes: String?,
        categoryId: String?,
        subcategoryId: String?,
    )
    suspend fun updateAiStatus(memoryId: String, status: AiProcessingStatus)

    /** Soft-deletes a memory so Undo Capture can restore it immediately. */
    suspend fun softDeleteMemory(memoryId: String, deletedAtEpochMillis: Long)
    suspend fun restoreMemory(memoryId: String)

    /** Permanently deletes a memory record and its associated local files. */
    suspend fun hardDeleteMemory(memoryId: String)

    /** Permanently deletes a batch of memory records and their associated local files. */
    suspend fun hardDeleteMemories(memoryIds: Collection<String>)
    suspend fun deleteAllMemories()
}
