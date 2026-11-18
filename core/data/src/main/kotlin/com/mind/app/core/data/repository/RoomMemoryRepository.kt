package com.mind.app.core.data.repository

import androidx.room.withTransaction
import com.mind.app.core.data.mapper.toDomain
import com.mind.app.core.data.mapper.toEntity
import com.mind.app.core.data.storage.LocalMediaStorage
import com.mind.app.core.database.MindDatabase
import com.mind.app.core.database.dao.AiProcessingJobDao
import com.mind.app.core.database.dao.CollectionDao
import com.mind.app.core.database.dao.MemoryConnectionDao
import com.mind.app.core.database.dao.MemoryDao
import com.mind.app.core.database.dao.SourceApplicationDao
import com.mind.app.core.database.entity.MemoryCategoryCrossRef
import com.mind.app.core.database.entity.SourceApplicationEntity
import com.mind.app.core.domain.repository.MemoryRepository
import com.mind.app.core.model.AiProcessingStatus
import com.mind.app.core.model.Memory
import com.mind.app.core.model.MemoryDetail
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomMemoryRepository @Inject constructor(
    private val database: MindDatabase,
    private val memoryDao: MemoryDao,
    private val collectionDao: CollectionDao,
    private val connectionDao: MemoryConnectionDao,
    private val aiJobDao: AiProcessingJobDao,
    private val sourceAppDao: SourceApplicationDao,
    private val localMediaStorage: LocalMediaStorage,
) : MemoryRepository {

    override fun observeRecentMemories(limit: Int): Flow<List<Memory>> =
        memoryDao.observeRecentMemories(limit).map { list -> list.map { it.toDomain() } }

    override fun observeFavouriteMemories(): Flow<List<Memory>> =
        memoryDao.observeFavouriteMemories().map { list -> list.map { it.toDomain() } }

    override fun observeMemoriesByCategory(categoryId: String): Flow<List<Memory>> =
        memoryDao.observeMemoriesByCategory(categoryId).map { list -> list.map { it.toDomain() } }

    override fun observeTotalMemoryCount(): Flow<Int> =
        memoryDao.observeTotalMemoryCount()

    override suspend fun getMemoryById(memoryId: String): Memory? =
        memoryDao.getMemoryWithDetailsById(memoryId)?.toDomain()

    override suspend fun getRecentMemoriesSnapshot(limit: Int): List<Memory> =
        memoryDao.getRecentMemoryDetailsSnapshot(limit).map { it.toDomain() }

    override suspend fun getStorageStats(): com.mind.app.core.model.StorageStats {
        val count = memoryDao.getTotalMemoryCountOnce()
        return localMediaStorage.calculateStorageStats(count)
    }

    override suspend fun clearCacheStorage() {
        localMediaStorage.clearCache()
    }

    override fun observeMemoryDetail(memoryId: String): Flow<MemoryDetail?> {
        return combine(
            memoryDao.observeMemoryById(memoryId),
            collectionDao.observeCollectionsForMemory(memoryId),
            connectionDao.observeConnectionsForMemory(memoryId),
            aiJobDao.observeLatestJobForMemory(memoryId),
        ) { details, collections, connections, latestJob ->
            details?.let {
                MemoryDetail(
                    memory = it.toDomain(),
                    additionalCategories = it.additionalCategories.map { cat -> cat.toDomain() },
                    collections = collections.map { col -> col.toDomain() },
                    connections = connections.map { row -> row.toDomain() },
                    latestAiJob = latestJob?.toDomain(),
                )
            }
        }
    }

    override suspend fun upsertMemory(memory: Memory) {
        database.withTransaction {
            val pkg = memory.sourcePackageName
            if (!pkg.isNullOrBlank()) {
                val existingApp = sourceAppDao.getByPackage(pkg)
                if (existingApp == null) {
                    sourceAppDao.upsert(
                        SourceApplicationEntity(
                            packageName = pkg,
                            displayName = memory.sourceAppName ?: pkg,
                            isExcluded = false,
                            captureCount = 1,
                            lastCapturedAt = memory.capturedAtEpochMillis,
                        )
                    )
                } else if (memoryDao.getMemoryEntityById(memory.id) == null) {
                    sourceAppDao.incrementCaptureCount(pkg, memory.capturedAtEpochMillis)
                }
            }

            memoryDao.upsertMemory(memory.toEntity())

            val refs = buildList {
                memory.primaryCategory?.id?.let {
                    add(MemoryCategoryCrossRef(memory.id, it, isPrimary = true))
                }
                memory.subcategory?.id?.let {
                    add(MemoryCategoryCrossRef(memory.id, it, isPrimary = false))
                }
            }
            memoryDao.clearCategoryRefsForMemory(memory.id)
            if (refs.isNotEmpty()) {
                memoryDao.upsertCategoryRefs(refs)
            }
        }
    }

    override suspend fun updateFavourite(memoryId: String, isFavourite: Boolean) {
        memoryDao.updateFavourite(memoryId, isFavourite, System.currentTimeMillis())
    }

    override suspend fun updateUserMetadata(
        memoryId: String,
        title: String?,
        notes: String?,
        categoryId: String?,
        subcategoryId: String?,
    ) {
        database.withTransaction {
            memoryDao.updateUserMetadata(
                memoryId = memoryId,
                userTitle = title?.trim()?.ifEmpty { null },
                userNotes = notes?.trim()?.ifEmpty { null },
                categoryId = categoryId,
                subcategoryId = subcategoryId,
                updatedAt = System.currentTimeMillis(),
            )
            memoryDao.clearCategoryRefsForMemory(memoryId)
            val refs = buildList {
                categoryId?.let { add(MemoryCategoryCrossRef(memoryId, it, isPrimary = true)) }
                subcategoryId?.let { add(MemoryCategoryCrossRef(memoryId, it, isPrimary = false)) }
            }
            if (refs.isNotEmpty()) {
                memoryDao.upsertCategoryRefs(refs)
            }
        }
    }

    override suspend fun updateAiStatus(memoryId: String, status: AiProcessingStatus) {
        memoryDao.updateAiStatus(memoryId, status, System.currentTimeMillis())
    }

    override suspend fun softDeleteMemory(memoryId: String, deletedAtEpochMillis: Long) {
        memoryDao.softDelete(memoryId, deletedAtEpochMillis)
    }

    override suspend fun restoreMemory(memoryId: String) {
        memoryDao.restore(memoryId, System.currentTimeMillis())
    }

    override suspend fun hardDeleteMemory(memoryId: String) {
        val existing = memoryDao.getMemoryEntityById(memoryId)
        database.withTransaction {
            memoryDao.hardDelete(memoryId)
        }
        if (existing != null) {
            localMediaStorage.deleteStoredMedia(existing.originalFilePath, existing.thumbnailPath)
        }
    }

    override suspend fun hardDeleteMemories(memoryIds: Collection<String>) {
        val distinctIds = memoryIds.distinct()
        if (distinctIds.isEmpty()) return
        val existingEntities = distinctIds.chunked(500).flatMap { chunk ->
            memoryDao.getMemoryEntitiesByIds(chunk)
        }
        database.withTransaction {
            distinctIds.chunked(500).forEach { chunk ->
                memoryDao.hardDeleteBatch(chunk)
            }
        }
        existingEntities.forEach { existing ->
            localMediaStorage.deleteStoredMedia(existing.originalFilePath, existing.thumbnailPath)
        }
    }

    override suspend fun deleteAllMemories() {
        database.withTransaction {
            memoryDao.deleteAll()
        }
        localMediaStorage.deleteAllStoredMedia()
    }
}
