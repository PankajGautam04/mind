package com.mind.app.core.domain.repository

import com.mind.app.core.model.Memory
import com.mind.app.core.model.MemorySearchFilter
import kotlinx.coroutines.flow.Flow

/**
 * Contract for local full-text, filtered, and semantic retrieval of memories.
 */
interface SearchRepository {
    fun searchMemories(filter: MemorySearchFilter): Flow<List<Memory>>
}
