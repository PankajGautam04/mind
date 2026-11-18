package com.mind.app.domain

import com.mind.app.core.domain.repository.CategoryRepository
import com.mind.app.core.domain.usecase.ManageCategoryUseCase
import com.mind.app.core.model.Category
import com.mind.app.core.model.CategoryTreeNode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ManageCategoryUseCaseTest {
    private val fakeRepo = object : CategoryRepository {
        val deletedIds = mutableListOf<String>()
        override fun observeCategoryTree(): Flow<List<CategoryTreeNode>> = MutableStateFlow(emptyList())
        override fun observeAllCategories(): Flow<List<Category>> = MutableStateFlow(emptyList())
        override suspend fun createCategory(name: String, parentId: String?, isAiSuggested: Boolean) =
            Category(id = "cat_custom", name = name, parentId = parentId)
        override suspend fun renameCategory(categoryId: String, newName: String) = Unit
        override suspend fun deleteCategory(categoryId: String, reassignToCategoryId: String?) {
            deletedIds += categoryId
        }
        override suspend fun mergeCategories(sourceCategoryId: String, targetCategoryId: String) = Unit
    }

    private val useCase = ManageCategoryUseCase(fakeRepo)

    @Test
    fun rejectsBlankCategoryName() = runTest {
        val result = useCase.create("   ")
        assertTrue(result.isFailure)
    }

    @Test
    fun protectsUncategorizedFromDeletion() = runTest {
        val result = useCase.delete(ManageCategoryUseCase.UNCATEGORIZED_ID)
        assertTrue(result.isFailure)
        assertTrue(fakeRepo.deletedIds.isEmpty())
    }

    @Test
    fun createsTrimmedCategoryAndDeletesCustomCategory() = runTest {
        val created = useCase.create("  Kubernetes  ").getOrThrow()
        assertEquals("Kubernetes", created.name)

        val deleteResult = useCase.delete("cat_custom")
        assertTrue(deleteResult.isSuccess)
        assertEquals(listOf("cat_custom"), fakeRepo.deletedIds)
    }
}
