package com.mind.app.core.data.repository

import com.mind.app.core.data.mapper.toDomain
import com.mind.app.core.database.dao.MemoryDao
import com.mind.app.core.domain.repository.SearchRepository
import com.mind.app.core.model.Memory
import com.mind.app.core.model.MemorySearchFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Converts arbitrary user search text into a safe SQLite FTS4 prefix query.
 */
object FtsQuerySanitizer {
    private val TOKEN_REGEX = Regex("[\\p{L}\\p{N}_]+")

    fun toPrefixFtsQuery(rawQuery: String): String? {
        val tokens = TOKEN_REGEX.findAll(rawQuery)
            .map { it.value.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .toList()
        if (tokens.isEmpty()) return null
        return tokens.joinToString(separator = " ") { "$it*" }
    }
}

@Singleton
class RoomSearchRepository @Inject constructor(
    private val memoryDao: MemoryDao,
) : SearchRepository {

    override fun searchMemories(filter: MemorySearchFilter): Flow<List<Memory>> {
        val ftsQuery = FtsQuerySanitizer.toPrefixFtsQuery(filter.query)
        val sourceFlow = if (ftsQuery != null) {
            memoryDao.searchMemoriesFts(
                ftsQuery = ftsQuery,
                categoryId = filter.categoryId,
                sourcePackageName = filter.sourcePackageName,
                fromEpochMillis = filter.fromEpochMillis,
                toEpochMillis = filter.toEpochMillis,
                favouritesOnly = filter.favouritesOnly,
            )
        } else {
            memoryDao.observeFilteredMemories(
                categoryId = filter.categoryId,
                sourcePackageName = filter.sourcePackageName,
                fromEpochMillis = filter.fromEpochMillis,
                toEpochMillis = filter.toEpochMillis,
                favouritesOnly = filter.favouritesOnly,
            )
        }

        return sourceFlow.map { detailsList ->
            detailsList
                .map { it.toDomain() }
                .filter { memory ->
                    val matchesTypes = filter.contentTypes.isEmpty() || memory.contentType in filter.contentTypes
                    val matchesTags = filter.tagNames.isEmpty() ||
                        memory.tags.any { it.name.lowercase() in filter.tagNames }
                    matchesTypes && matchesTags
                }
        }
    }
}
