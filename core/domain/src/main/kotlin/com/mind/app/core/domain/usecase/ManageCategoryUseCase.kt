package com.mind.app.core.domain.usecase

import com.mind.app.core.domain.repository.CategoryRepository
import com.mind.app.core.model.Category
import javax.inject.Inject

/**
 * Enforces category invariants (non-empty names, protected system fallback).
 */
class ManageCategoryUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
) {
    suspend fun create(name: String, parentId: String? = null): Result<Category> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Category name cannot be blank"))
        }
        return runCatching {
            categoryRepository.createCategory(name = trimmed, parentId = parentId, isAiSuggested = false)
        }
    }

    suspend fun rename(categoryId: String, newName: String): Result<Unit> {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Category name cannot be blank"))
        }
        return runCatching {
            categoryRepository.renameCategory(categoryId, trimmed)
        }
    }

    suspend fun delete(categoryId: String, reassignToCategoryId: String? = UNCATEGORIZED_ID): Result<Unit> {
        if (categoryId == UNCATEGORIZED_ID) {
            return Result.failure(IllegalArgumentException("Uncategorized cannot be deleted"))
        }
        return runCatching {
            categoryRepository.deleteCategory(categoryId, reassignToCategoryId)
        }
    }

    companion object {
        const val UNCATEGORIZED_ID = "cat_uncategorized"
    }
}
