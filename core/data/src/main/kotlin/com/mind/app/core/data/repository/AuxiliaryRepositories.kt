package com.mind.app.core.data.repository

import com.mind.app.core.data.datastore.UserPreferencesDataSource
import com.mind.app.core.data.mapper.toDomain
import com.mind.app.core.data.mapper.toEntity
import com.mind.app.core.database.dao.AiProcessingJobDao
import com.mind.app.core.database.dao.SourceApplicationDao
import com.mind.app.core.database.entity.SourceApplicationEntity
import com.mind.app.core.domain.repository.AiJobRepository
import com.mind.app.core.domain.repository.PreferencesRepository
import com.mind.app.core.domain.repository.SourceAppRepository
import com.mind.app.core.model.AccentPalette
import com.mind.app.core.model.AiJobStatus
import com.mind.app.core.model.AiProcessingJob
import com.mind.app.core.model.AnimationStyle
import com.mind.app.core.model.AssistantAction
import com.mind.app.core.model.LayoutMode
import com.mind.app.core.model.SourceApplication
import com.mind.app.core.model.ThemeMode
import com.mind.app.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomAiJobRepository @Inject constructor(
    private val aiProcessingJobDao: AiProcessingJobDao,
) : AiJobRepository {
    override fun observeJobsForMemory(memoryId: String): Flow<List<AiProcessingJob>> =
        aiProcessingJobDao.observeJobsForMemory(memoryId).map { list -> list.map { it.toDomain() } }

    override fun observePendingJobCount(): Flow<Int> =
        aiProcessingJobDao.observePendingJobCount()

    override suspend fun enqueueJobRecord(job: AiProcessingJob) {
        aiProcessingJobDao.upsertJob(job.toEntity())
    }

    override suspend fun updateJobStatus(jobId: String, status: AiJobStatus, error: String?) {
        val now = System.currentTimeMillis()
        val completedAt = if (status == AiJobStatus.SUCCEEDED || status == AiJobStatus.CANCELLED) now else null
        aiProcessingJobDao.updateJobStatus(jobId, status, error, now, completedAt)
    }
}

@Singleton
class RoomSourceAppRepository @Inject constructor(
    private val sourceAppDao: SourceApplicationDao,
) : SourceAppRepository {
    override fun observeSourceApps(): Flow<List<SourceApplication>> =
        sourceAppDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeExcludedPackages(): Flow<Set<String>> =
        sourceAppDao.observeExcludedPackages().map { it.toSet() }

    override suspend fun setAppExcluded(packageName: String, displayName: String, excluded: Boolean) {
        val existing = sourceAppDao.getByPackage(packageName)
        if (existing == null) {
            sourceAppDao.upsert(
                SourceApplicationEntity(
                    packageName = packageName,
                    displayName = displayName,
                    isExcluded = excluded,
                )
            )
        } else {
            sourceAppDao.updateExcluded(packageName, excluded)
        }
    }
}

@Singleton
class DataStorePreferencesRepository @Inject constructor(
    private val dataSource: UserPreferencesDataSource,
) : PreferencesRepository {
    override fun observePreferences(): Flow<UserPreferences> = dataSource.preferences
    override suspend fun setThemeMode(themeMode: ThemeMode) = dataSource.setThemeMode(themeMode)
    override suspend fun setAccentPalette(accentPalette: AccentPalette) = dataSource.setAccentPalette(accentPalette)
    override suspend fun setLayoutMode(layoutMode: LayoutMode) = dataSource.setLayoutMode(layoutMode)
    override suspend fun setAnimationStyle(animationStyle: AnimationStyle) = dataSource.setAnimationStyle(animationStyle)
    override suspend fun setAnimationsEnabled(enabled: Boolean) = dataSource.setAnimationsEnabled(enabled)
    override suspend fun setFloatingCaptureEnabled(enabled: Boolean) = dataSource.setFloatingCaptureEnabled(enabled)
    override suspend fun setCaptureConfirmationEnabled(enabled: Boolean) = dataSource.setCaptureConfirmationEnabled(enabled)
    override suspend fun setCloudAiEnabled(enabled: Boolean) = dataSource.setCloudAiEnabled(enabled)
    override suspend fun setLocalOnlyMode(localOnly: Boolean) = dataSource.setLocalOnlyMode(localOnly)
    override suspend fun setAutoCategorizationEnabled(enabled: Boolean) = dataSource.setAutoCategorizationEnabled(enabled)
    override suspend fun setAiLanguageTag(languageTag: String) = dataSource.setAiLanguageTag(languageTag)
    override suspend fun setGeminiModel(modelId: String) = dataSource.setGeminiModel(modelId)
    override suspend fun setCustomBackendUrl(url: String) = dataSource.setCustomBackendUrl(url)
    override suspend fun saveGeminiApiKey(apiKey: String) = dataSource.saveGeminiApiKey(apiKey)
    override suspend fun clearGeminiApiKey() = dataSource.clearGeminiApiKey()
    override suspend fun setAssistantAction(action: AssistantAction) = dataSource.setAssistantAction(action)
    override suspend fun setOnboardingCompleted(completed: Boolean) = dataSource.setOnboardingCompleted(completed)
}
