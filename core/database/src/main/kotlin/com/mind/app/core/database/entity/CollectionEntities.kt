package com.mind.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "collections",
    indices = [
        Index(value = ["is_smart_collection"]),
        Index(value = ["is_suggested"]),
    ],
)
data class CollectionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "description")
    val description: String? = null,
    @ColumnInfo(name = "is_smart_collection")
    val isSmartCollection: Boolean = false,
    @ColumnInfo(name = "is_suggested")
    val isSuggested: Boolean = false,
    @ColumnInfo(name = "rule_json")
    val ruleJson: String? = null,
    @ColumnInfo(name = "cover_memory_id")
    val coverMemoryId: String? = null,
    @ColumnInfo(name = "icon_name")
    val iconName: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = 0L,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = 0L,
)

@Entity(
    tableName = "collection_memory_cross_ref",
    primaryKeys = ["collection_id", "memory_id"],
    foreignKeys = [
        ForeignKey(
            entity = CollectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["collection_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = MemoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["memory_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["memory_id"]),
    ],
)
data class CollectionMemoryCrossRef(
    @ColumnInfo(name = "collection_id")
    val collectionId: String,
    @ColumnInfo(name = "memory_id")
    val memoryId: String,
    @ColumnInfo(name = "added_at")
    val addedAt: Long,
    @ColumnInfo(name = "is_auto_added")
    val isAutoAdded: Boolean = false,
)
