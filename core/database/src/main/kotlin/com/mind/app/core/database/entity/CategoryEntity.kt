package com.mind.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Hierarchical category or subcategory entity.
 */
@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["parent_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["parent_id"]),
        Index(value = ["parent_id", "name"]),
    ],
)
data class CategoryEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "parent_id")
    val parentId: String? = null,
    @ColumnInfo(name = "icon_name")
    val iconName: String? = null,
    @ColumnInfo(name = "color_hex")
    val colorHex: Long? = null,
    @ColumnInfo(name = "is_system_default")
    val isSystemDefault: Boolean = false,
    @ColumnInfo(name = "is_ai_suggested")
    val isAiSuggested: Boolean = false,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int = 0,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = 0L,
)
