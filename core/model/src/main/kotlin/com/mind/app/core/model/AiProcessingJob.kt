package com.mind.app.core.model

/**
 * Persistent background WorkManager job tracking AI analysis of a memory.
 */
data class AiProcessingJob(
    val id: String,
    val memoryId: String,
    val workManagerId: String? = null,
    val status: AiJobStatus,
    val attemptCount: Int = 0,
    val lastError: String? = null,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val completedAtEpochMillis: Long? = null,
)

enum class AiJobStatus {
    QUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    RETRY_SCHEDULED,
    CANCELLED,
}
