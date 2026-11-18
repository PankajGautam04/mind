package com.mind.app.core.domain.usecase

import com.mind.app.core.domain.repository.SearchRepository
import com.mind.app.core.model.Memory
import com.mind.app.core.model.MemorySearchFilter
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Sanitizes and executes local full-text and filtered memory retrieval.
 */
class SearchMemoriesUseCase @Inject constructor(
    private val searchRepository: SearchRepository,
) {
    operator fun invoke(filter: MemorySearchFilter): Flow<List<Memory>> {
        val normalizedFilter = filter.copy(
            query = filter.query.trim(),
            tagNames = filter.tagNames.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet(),
        )
        return searchRepository.searchMemories(normalizedFilter)
    }
}
