package com.mind.app.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.mind.app.core.database.entity.CategoryEntity
import com.mind.app.core.database.entity.CollectionEntity
import com.mind.app.core.database.entity.MemoryCategoryCrossRef
import com.mind.app.core.database.entity.MemoryEntity
import com.mind.app.core.database.entity.MemoryTagCrossRef
import com.mind.app.core.database.entity.TagEntity
import com.mind.app.core.model.ConnectionType

/**
 * Full relational projection of a [MemoryEntity] with its category, subcategory, and tags.
 */
data class MemoryWithDetails(
    @Embedded val memory: MemoryEntity,
    @Relation(
        parentColumn = "category_id",
        entityColumn = "id",
    )
    val primaryCategory: CategoryEntity?,
    @Relation(
        parentColumn = "subcategory_id",
        entityColumn = "id",
    )
    val subcategory: CategoryEntity?,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = MemoryTagCrossRef::class,
            parentColumn = "memory_id",
            entityColumn = "tag_id",
        ),
    )
    val tags: List<TagEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = MemoryCategoryCrossRef::class,
            parentColumn = "memory_id",
            entityColumn = "category_id",
        ),
    )
    val additionalCategories: List<CategoryEntity>,
)

data class CategoryWithCount(
    @Embedded val category: CategoryEntity,
    @ColumnInfo(name = "memory_count") val memoryCount: Int,
)

data class CollectionWithCount(
    @Embedded val collection: CollectionEntity,
    @ColumnInfo(name = "memory_count") val memoryCount: Int,
    @ColumnInfo(name = "cover_thumbnail_path") val coverThumbnailPath: String?,
)

data class ConnectedMemoryRow(
    @ColumnInfo(name = "source_memory_id") val sourceMemoryId: String,
    @ColumnInfo(name = "target_memory_id") val targetMemoryId: String,
    @ColumnInfo(name = "connection_type") val connectionType: ConnectionType,
    @ColumnInfo(name = "strength") val strength: Float,
    @ColumnInfo(name = "reason") val reason: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "target_user_title") val targetUserTitle: String?,
    @ColumnInfo(name = "target_ai_title") val targetAiTitle: String?,
    @ColumnInfo(name = "target_primary_subject") val targetPrimarySubject: String?,
    @ColumnInfo(name = "target_thumbnail_path") val targetThumbnailPath: String?,
)
