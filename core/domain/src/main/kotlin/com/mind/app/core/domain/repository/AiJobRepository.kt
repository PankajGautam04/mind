package com.mind.app.core.domain.repository

import com.mind.app.core.model.AiJobStatus
import com.mind.app.core.model.AiProcessingJob
import com.mind.app.core.model.SourceApplication
import kotlinx.coroutines.flow.Flow

/**
 * Contract for tracking deferrable WorkManager AI analysis jobs.
 */
interface AiJobRepository {
    fun observeJobsForMemory(memoryId: String): Flow<List<AiProcessingJob>>
    fun observePendingJobCount(): Flow<Int>
    suspend fun enqueueJobRecord(job: AiProcessingJob)
    suspend fun updateJobStatus(jobId: String, status: AiJobStatus, error: String? = null)
}

/**
 * Contract for source app tracking and privacy exclusion lists.
 */
interface SourceAppRepository {
    fun observeSourceApps(): Flow<List<SourceApplication>>
    fun observeExcludedPackages(): Flow<Set<String>>
    suspend fun setAppExcluded(packageName: String, displayName: String, excluded: Boolean)
}
