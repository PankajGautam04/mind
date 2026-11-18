package com.mind.app.core.domain.usecase

import com.mind.app.core.domain.repository.AiEngineRepository
import com.mind.app.core.domain.repository.MemoryRepository
import javax.inject.Inject

/**
 * Coordinates soft deletion (enabling Undo Capture), restoration, and permanent removal.
 */
class ManageMemoryLifecycleUseCase @Inject constructor(
    private val memoryRepository: MemoryRepository,
    private val aiEngineRepository: AiEngineRepository,
) {
    suspend fun toggleFavourite(memoryId: String, currentlyFavourite: Boolean) {
        memoryRepository.updateFavourite(memoryId, !currentlyFavourite)
    }

    suspend fun softDelete(memoryId: String, nowEpochMillis: Long = System.currentTimeMillis()) {
        memoryRepository.softDeleteMemory(memoryId, nowEpochMillis)
    }

    suspend fun restore(memoryId: String) {
        memoryRepository.restoreMemory(memoryId)
    }

    suspend fun permanentlyDelete(memoryId: String) {
        aiEngineRepository.cancelAnalysis(memoryId)
        memoryRepository.hardDeleteMemory(memoryId)
    }

    suspend fun permanentlyDeleteBatch(memoryIds: Collection<String>) {
        val ids = memoryIds.distinct()
        if (ids.isEmpty()) return
        ids.forEach { aiEngineRepository.cancelAnalysis(it) }
        memoryRepository.hardDeleteMemories(ids)
    }
}
