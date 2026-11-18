package com.mind.app.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mind.app.core.database.entity.AiProcessingJobEntity
import com.mind.app.core.database.entity.MemoryConnectionEntity
import com.mind.app.core.database.entity.MemoryEmbeddingEntity
import com.mind.app.core.database.entity.SourceApplicationEntity
import com.mind.app.core.database.model.ConnectedMemoryRow
import com.mind.app.core.model.AiJobStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface AiProcessingJobDao {
    @Query("SELECT * FROM ai_processing_jobs WHERE memory_id = :memoryId ORDER BY created_at DESC")
    fun observeJobsForMemory(memoryId: String): Flow<List<AiProcessingJobEntity>>

    @Query("SELECT * FROM ai_processing_jobs WHERE memory_id = :memoryId ORDER BY created_at DESC LIMIT 1")
    fun observeLatestJobForMemory(memoryId: String): Flow<AiProcessingJobEntity?>

    @Query("SELECT COUNT(*) FROM ai_processing_jobs WHERE status IN ('QUEUED', 'RUNNING', 'RETRY_SCHEDULED')")
    fun observePendingJobCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertJob(job: AiProcessingJobEntity)

    @Query(
        """
        UPDATE ai_processing_jobs
        SET status = :status,
            last_error = :error,
            updated_at = :updatedAt,
            completed_at = :completedAt
        WHERE id = :jobId
        """
    )
    suspend fun updateJobStatus(
        jobId: String,
        status: AiJobStatus,
        error: String?,
        updatedAt: Long,
        completedAt: Long?,
    )
}

@Dao
interface SourceApplicationDao {
    @Query("SELECT * FROM source_applications ORDER BY capture_count DESC, display_name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<SourceApplicationEntity>>

    @Query("SELECT package_name FROM source_applications WHERE is_excluded = 1")
    fun observeExcludedPackages(): Flow<List<String>>

    @Query("SELECT * FROM source_applications WHERE package_name = :packageName LIMIT 1")
    suspend fun getByPackage(packageName: String): SourceApplicationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(app: SourceApplicationEntity)

    @Query("UPDATE source_applications SET is_excluded = :excluded WHERE package_name = :packageName")
    suspend fun updateExcluded(packageName: String, excluded: Boolean)

    @Query(
        """
        UPDATE source_applications
        SET capture_count = capture_count + 1,
            last_captured_at = :capturedAt
        WHERE package_name = :packageName
        """
    )
    suspend fun incrementCaptureCount(packageName: String, capturedAt: Long)
}

@Dao
interface MemoryConnectionDao {
    @Query(
        """
        SELECT mc.source_memory_id,
               mc.target_memory_id,
               mc.connection_type,
               mc.strength,
               mc.reason,
               mc.created_at,
               m.user_title AS target_user_title,
               m.ai_title AS target_ai_title,
               m.primary_subject AS target_primary_subject,
               m.thumbnail_path AS target_thumbnail_path
        FROM memory_connections mc
        INNER JOIN memories m ON m.id = mc.target_memory_id
        WHERE mc.source_memory_id = :memoryId AND m.is_deleted = 0
        ORDER BY mc.strength DESC, mc.created_at DESC
        """
    )
    fun observeConnectionsForMemory(memoryId: String): Flow<List<ConnectedMemoryRow>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConnections(connections: List<MemoryConnectionEntity>)

    @Query("DELETE FROM memory_connections WHERE source_memory_id = :memoryId OR target_memory_id = :memoryId")
    suspend fun deleteConnectionsForMemory(memoryId: String)
}

@Dao
interface MemoryEmbeddingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEmbedding(embedding: MemoryEmbeddingEntity)

    @Query("SELECT * FROM memory_embeddings")
    suspend fun getAllEmbeddings(): List<MemoryEmbeddingEntity>

    @Query("DELETE FROM memory_embeddings WHERE memory_id = :memoryId")
    suspend fun deleteByMemoryId(memoryId: String)
}
