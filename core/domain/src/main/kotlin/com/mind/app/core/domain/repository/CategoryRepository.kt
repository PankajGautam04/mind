package com.mind.app.core.domain.repository

import com.mind.app.core.model.Category
import com.mind.app.core.model.CategoryTreeNode
import kotlinx.coroutines.flow.Flow

/**
 * Contract for managing Mind's flexible hierarchical category tree.
 */
interface CategoryRepository {
    fun observeCategoryTree(): Flow<List<CategoryTreeNode>>
    fun observeAllCategories(): Flow<List<Category>>

    suspend fun createCategory(name: String, parentId: String? = null, isAiSuggested: Boolean = false): Category
    suspend fun renameCategory(categoryId: String, newName: String)
    suspend fun deleteCategory(categoryId: String, reassignToCategoryId: String? = null)
    suspend fun mergeCategories(sourceCategoryId: String, targetCategoryId: String)
}
