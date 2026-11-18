package com.mind.app.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mind.app.core.model.AiJobStatus
import com.mind.app.core.model.ConnectionType

@Entity(
    tableName = "ai_processing_jobs",
    foreignKeys = [
        ForeignKey(
            entity = MemoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["memory_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["memory_id"]),
        Index(value = ["status"]),
    ],
)
data class AiProcessingJobEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "memory_id")
    val memoryId: String,
    @ColumnInfo(name = "work_manager_id")
    val workManagerId: String? = null,
    @ColumnInfo(name = "status")
    val status: AiJobStatus,
    @ColumnInfo(name = "attempt_count")
    val attemptCount: Int = 0,
    @ColumnInfo(name = "last_error")
    val lastError: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
    @ColumnInfo(name = "completed_at")
    val completedAt: Long? = null,
)

@Entity(tableName = "source_applications")
data class SourceApplicationEntity(
    @PrimaryKey
    @ColumnInfo(name = "package_name")
    val packageName: String,
    @ColumnInfo(name = "display_name")
    val displayName: String,
    @ColumnInfo(name = "is_excluded")
    val isExcluded: Boolean = false,
    @ColumnInfo(name = "capture_count")
    val captureCount: Int = 0,
    @ColumnInfo(name = "last_captured_at")
    val lastCapturedAt: Long? = null,
)

@Entity(
    tableName = "memory_connections",
    primaryKeys = ["source_memory_id", "target_memory_id"],
    foreignKeys = [
        ForeignKey(
            entity = MemoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["source_memory_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = MemoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["target_memory_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["target_memory_id"]),
    ],
)
data class MemoryConnectionEntity(
    @ColumnInfo(name = "source_memory_id")
    val sourceMemoryId: String,
    @ColumnInfo(name = "target_memory_id")
    val targetMemoryId: String,
    @ColumnInfo(name = "connection_type")
    val connectionType: ConnectionType,
    @ColumnInfo(name = "strength")
    val strength: Float,
    @ColumnInfo(name = "reason")
    val reason: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)

@Entity(
    tableName = "memory_embeddings",
    foreignKeys = [
        ForeignKey(
            entity = MemoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["memory_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["memory_id"], unique = true),
    ],
)
data class MemoryEmbeddingEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "memory_id")
    val memoryId: String,
    @ColumnInfo(name = "model_version")
    val modelVersion: String,
    @ColumnInfo(name = "vector_blob")
    val vectorBlob: ByteArray,
    @ColumnInfo(name = "dimension")
    val dimension: Int,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MemoryEmbeddingEntity) return false
        return id == other.id &&
            memoryId == other.memoryId &&
            modelVersion == other.modelVersion &&
            dimension == other.dimension &&
            createdAt == other.createdAt &&
            vectorBlob.contentEquals(other.vectorBlob)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + memoryId.hashCode()
        result = 31 * result + modelVersion.hashCode()
        result = 31 * result + vectorBlob.contentHashCode()
        result = 31 * result + dimension
        result = 31 * result + createdAt.hashCode()
        return result
    }
}
