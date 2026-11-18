package com.mind.app.core.data.repository

import androidx.room.withTransaction
import com.mind.app.core.data.mapper.toDomain
import com.mind.app.core.database.MindDatabase
import com.mind.app.core.database.dao.CategoryDao
import com.mind.app.core.database.entity.CategoryEntity
import com.mind.app.core.database.seed.DefaultCategorySeeder
import com.mind.app.core.domain.repository.CategoryRepository
import com.mind.app.core.model.Category
import com.mind.app.core.model.CategoryCountItem
import com.mind.app.core.model.CategoryTreeNode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomCategoryRepository @Inject constructor(
    private val database: MindDatabase,
    private val categoryDao: CategoryDao,
) : CategoryRepository {

    override fun observeCategoryTree(): Flow<List<CategoryTreeNode>> {
        return categoryDao.observeCategoriesWithCount().map { items ->
            val roots = items.filter { it.category.parentId == null }
            val byParent = items.filter { it.category.parentId != null }.groupBy { it.category.parentId }

            roots.map { root ->
                val children = byParent[root.category.id].orEmpty().map { child ->
                    CategoryCountItem(
                        category = child.category.toDomain(),
                        memoryCount = child.memoryCount,
                    )
                }
                CategoryTreeNode(
                    category = root.category.toDomain(),
                    memoryCount = root.memoryCount,
                    subcategories = children,
                )
            }
        }
    }

    override fun observeAllCategories(): Flow<List<Category>> =
        categoryDao.observeAllCategories().map { list -> list.map { it.toDomain() } }

    override suspend fun createCategory(
        name: String,
        parentId: String?,
        isAiSuggested: Boolean,
    ): Category {
        val trimmed = name.trim()
        val existing = categoryDao.findByNameAndParent(trimmed, parentId)
        if (existing != null) return existing.toDomain()

        val entity = CategoryEntity(
            id = "cat_${UUID.randomUUID()}",
            name = trimmed,
            parentId = parentId,
            iconName = null,
            colorHex = null,
            isSystemDefault = false,
            isAiSuggested = isAiSuggested,
            sortOrder = 100,
            createdAt = System.currentTimeMillis(),
        )
        categoryDao.upsert(entity)
        return entity.toDomain()
    }

    override suspend fun renameCategory(categoryId: String, newName: String) {
        categoryDao.rename(categoryId, newName.trim())
    }

    override suspend fun deleteCategory(categoryId: String, reassignToCategoryId: String?) {
        if (categoryId == DefaultCategorySeeder.UNCATEGORIZED_ID) return
        val fallbackId = reassignToCategoryId ?: DefaultCategorySeeder.UNCATEGORIZED_ID
        database.withTransaction {
            val target = categoryDao.getById(categoryId)
            categoryDao.reassignPrimaryCategory(categoryId, fallbackId)
            categoryDao.reassignSubcategory(categoryId, null)
            categoryDao.reparentChildren(categoryId, target?.parentId)
            categoryDao.deleteById(categoryId)
        }
    }

    override suspend fun mergeCategories(sourceCategoryId: String, targetCategoryId: String) {
        if (sourceCategoryId == targetCategoryId) return
        database.withTransaction {
            val target = categoryDao.getById(targetCategoryId) ?: return@withTransaction
            if (target.parentId == null) {
                categoryDao.reassignPrimaryCategory(sourceCategoryId, targetCategoryId)
            } else {
                categoryDao.reassignPrimaryCategory(sourceCategoryId, target.parentId)
                categoryDao.reassignSubcategory(sourceCategoryId, targetCategoryId)
            }
            categoryDao.reparentChildren(sourceCategoryId, targetCategoryId)
            categoryDao.deleteById(sourceCategoryId)
        }
    }
}
