package com.mind.app.core.domain.repository

import com.mind.app.core.model.CollectionRule
import com.mind.app.core.model.Memory
import com.mind.app.core.model.SmartCollection
import kotlinx.coroutines.flow.Flow

/**
 * Contract for manual, smart rule-based, and AI-suggested collections.
 */
interface CollectionRepository {
    fun observeCollections(): Flow<List<SmartCollection>>
    fun observeMemoriesInCollection(collectionId: String): Flow<List<Memory>>

    suspend fun createCollection(
        name: String,
        description: String? = null,
        isSmartCollection: Boolean = false,
        rule: CollectionRule? = null,
    ): SmartCollection

    suspend fun addMemoryToCollection(collectionId: String, memoryId: String, isAutoAdded: Boolean = false)
    suspend fun removeMemoryFromCollection(collectionId: String, memoryId: String)
    suspend fun acceptSuggestedCollection(collectionId: String)
    suspend fun deleteCollection(collectionId: String)
}
