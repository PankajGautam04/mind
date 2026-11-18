package com.mind.app.core.domain.usecase

import com.mind.app.core.domain.repository.CategoryRepository
import com.mind.app.core.domain.repository.CollectionRepository
import com.mind.app.core.domain.repository.MemoryRepository
import com.mind.app.core.model.AiProcessingStatus
import com.mind.app.core.model.CategoryTreeNode
import com.mind.app.core.model.Memory
import com.mind.app.core.model.SmartCollection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Aggregates the reactive streams needed by Mind's Home surface.
 */
data class HomeDashboardData(
    val recentMemories: List<Memory>,
    val favouriteCount: Int,
    val totalMemoryCount: Int,
    val failedAnalysisCount: Int,
    val topCategories: List<CategoryTreeNode>,
    val collections: List<SmartCollection>,
)

class ObserveHomeDashboardUseCase @Inject constructor(
    private val memoryRepository: MemoryRepository,
    private val categoryRepository: CategoryRepository,
    private val collectionRepository: CollectionRepository,
) {
    operator fun invoke(recentLimit: Int = 200): Flow<HomeDashboardData> {
        return combine(
            memoryRepository.observeRecentMemories(recentLimit),
            memoryRepository.observeFavouriteMemories(),
            memoryRepository.observeTotalMemoryCount(),
            categoryRepository.observeCategoryTree(),
            collectionRepository.observeCollections(),
        ) { recent, favourites, totalCount, categories, collections ->
            HomeDashboardData(
                recentMemories = recent,
                favouriteCount = favourites.size,
                totalMemoryCount = totalCount,
                failedAnalysisCount = recent.count { it.aiStatus == AiProcessingStatus.FAILED },
                topCategories = categories,
                collections = collections,
            )
        }
    }
}
