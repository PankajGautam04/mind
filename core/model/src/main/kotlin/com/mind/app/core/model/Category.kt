package com.mind.app.core.model

/**
 * Hierarchical category or subcategory for organizing memories.
 */
data class Category(
    val id: String,
    val name: String,
    val parentId: String? = null,
    val iconName: String? = null,
    val colorHex: Long? = null,
    val isSystemDefault: Boolean = false,
    val isAiSuggested: Boolean = false,
    val sortOrder: Int = 0,
    val createdAtEpochMillis: Long = 0L,
)

/**
 * Top-level category paired with its subcategories and live memory counts.
 */
data class CategoryTreeNode(
    val category: Category,
    val memoryCount: Int,
    val subcategories: List<CategoryCountItem> = emptyList(),
)

data class CategoryCountItem(
    val category: Category,
    val memoryCount: Int,
)
