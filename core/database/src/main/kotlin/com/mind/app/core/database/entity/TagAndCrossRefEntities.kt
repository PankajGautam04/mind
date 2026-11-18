package com.mind.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mind.app.core.model.MetadataSource

@Entity(
    tableName = "tags",
    indices = [
        Index(value = ["normalized_name"], unique = true),
    ],
)
data class TagEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "normalized_name")
    val normalizedName: String,
    @ColumnInfo(name = "is_ai_generated")
    val isAiGenerated: Boolean = true,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = 0L,
)

@Entity(
    tableName = "memory_tag_cross_ref",
    primaryKeys = ["memory_id", "tag_id"],
    foreignKeys = [
        ForeignKey(
            entity = MemoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["memory_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tag_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["tag_id"]),
    ],
)
data class MemoryTagCrossRef(
    @ColumnInfo(name = "memory_id")
    val memoryId: String,
    @ColumnInfo(name = "tag_id")
    val tagId: String,
    @ColumnInfo(name = "source")
    val source: MetadataSource = MetadataSource.AI,
)

@Entity(
    tableName = "memory_category_cross_ref",
    primaryKeys = ["memory_id", "category_id"],
    foreignKeys = [
        ForeignKey(
            entity = MemoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["memory_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["category_id"]),
    ],
)
data class MemoryCategoryCrossRef(
    @ColumnInfo(name = "memory_id")
    val memoryId: String,
    @ColumnInfo(name = "category_id")
    val categoryId: String,
    @ColumnInfo(name = "is_primary")
    val isPrimary: Boolean = false,
)
