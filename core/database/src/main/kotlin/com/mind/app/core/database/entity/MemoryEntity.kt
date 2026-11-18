package com.mind.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mind.app.core.model.AiProcessingStatus
import com.mind.app.core.model.CaptureMethod
import com.mind.app.core.model.ContentType

/**
 * Primary table storing locally captured memories and AI-extracted metadata.
 */
@Entity(
    tableName = "memories",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["subcategory_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = SourceApplicationEntity::class,
            parentColumns = ["package_name"],
            childColumns = ["source_package_name"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["category_id"]),
        Index(value = ["subcategory_id"]),
        Index(value = ["source_package_name"]),
        Index(value = ["captured_at"]),
        Index(value = ["is_favourite"]),
        Index(value = ["ai_status"]),
        Index(value = ["is_deleted"]),
    ],
)
data class MemoryEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "original_file_path")
    val originalFilePath: String? = null,
    @ColumnInfo(name = "thumbnail_path")
    val thumbnailPath: String? = null,
    @ColumnInfo(name = "content_type")
    val contentType: ContentType,
    @ColumnInfo(name = "ai_title")
    val aiTitle: String? = null,
    @ColumnInfo(name = "user_title")
    val userTitle: String? = null,
    @ColumnInfo(name = "description")
    val description: String? = null,
    @ColumnInfo(name = "extracted_text")
    val extractedText: String? = null,
    @ColumnInfo(name = "primary_subject")
    val primarySubject: String? = null,
    @ColumnInfo(name = "main_topic")
    val mainTopic: String? = null,
    @ColumnInfo(name = "context_summary")
    val contextSummary: String? = null,
    @ColumnInfo(name = "category_id")
    val categoryId: String? = null,
    @ColumnInfo(name = "subcategory_id")
    val subcategoryId: String? = null,
    @ColumnInfo(name = "entities_json")
    val entities: List<String> = emptyList(),
    @ColumnInfo(name = "detected_objects_json")
    val detectedObjects: List<String> = emptyList(),
    @ColumnInfo(name = "source_package_name")
    val sourcePackageName: String? = null,
    @ColumnInfo(name = "source_app_name")
    val sourceAppName: String? = null,
    @ColumnInfo(name = "original_url")
    val originalUrl: String? = null,
    @ColumnInfo(name = "captured_at")
    val capturedAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "ai_status")
    val aiStatus: AiProcessingStatus,
    @ColumnInfo(name = "is_favourite")
    val isFavourite: Boolean = false,
    @ColumnInfo(name = "user_notes")
    val userNotes: String? = null,
    @ColumnInfo(name = "embedding_id")
    val embeddingId: String? = null,
    @ColumnInfo(name = "confidence_score")
    val confidenceScore: Float? = null,
    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false,
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long? = null,
    @ColumnInfo(name = "capture_method")
    val captureMethod: CaptureMethod = CaptureMethod.IN_APP,
)
