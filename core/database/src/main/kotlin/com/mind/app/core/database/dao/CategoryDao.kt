package com.mind.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mind.app.core.database.entity.CategoryEntity
import com.mind.app.core.database.model.CategoryWithCount
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query(
        """
        SELECT c.*, COUNT(DISTINCT m.id) AS memory_count
        FROM categories c
        LEFT JOIN memories m ON (m.category_id = c.id OR m.subcategory_id = c.id) AND m.is_deleted = 0
        GROUP BY c.id
        ORDER BY c.sort_order ASC, c.name COLLATE NOCASE ASC
        """
    )
    fun observeCategoriesWithCount(): Flow<List<CategoryWithCount>>

    @Query("SELECT * FROM categories ORDER BY sort_order ASC, name COLLATE NOCASE ASC")
    fun observeAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY sort_order ASC, name COLLATE NOCASE ASC")
    suspend fun getAllCategoriesOnce(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :categoryId LIMIT 1")
    suspend fun getById(categoryId: String): CategoryEntity?

    @Query(
        """
        SELECT * FROM categories
        WHERE LOWER(name) = LOWER(:name)
          AND ((:parentId IS NULL AND parent_id IS NULL) OR parent_id = :parentId)
        LIMIT 1
        """
    )
    suspend fun findByNameAndParent(name: String, parentId: String?): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnore(categories: List<CategoryEntity>)

    @Query("UPDATE categories SET name = :newName WHERE id = :categoryId")
    suspend fun rename(categoryId: String, newName: String)

    @Query("UPDATE memories SET category_id = :targetCategoryId WHERE category_id = :sourceCategoryId")
    suspend fun reassignPrimaryCategory(sourceCategoryId: String, targetCategoryId: String?)

    @Query("UPDATE memories SET subcategory_id = :targetSubcategoryId WHERE subcategory_id = :sourceCategoryId")
    suspend fun reassignSubcategory(sourceCategoryId: String, targetSubcategoryId: String?)

    @Query("UPDATE categories SET parent_id = :newParentId WHERE parent_id = :oldParentId")
    suspend fun reparentChildren(oldParentId: String, newParentId: String?)

    @Query("DELETE FROM categories WHERE id = :categoryId")
    suspend fun deleteById(categoryId: String)
}
