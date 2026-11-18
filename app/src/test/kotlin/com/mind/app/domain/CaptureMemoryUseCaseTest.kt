package com.mind.app.domain

import com.mind.app.core.domain.repository.AiEngineRepository
import com.mind.app.core.domain.repository.MemoryRepository
import com.mind.app.core.domain.repository.PreferencesRepository
import com.mind.app.core.domain.repository.SourceAppRepository
import com.mind.app.core.domain.usecase.CaptureMemoryUseCase
import com.mind.app.core.model.AccentPalette
import com.mind.app.core.model.AiConnectionTestResult
import com.mind.app.core.model.AiProcessingStatus
import com.mind.app.core.model.AnimationStyle
import com.mind.app.core.model.AskMindAnswer
import com.mind.app.core.model.AssistantAction
import com.mind.app.core.model.CaptureMethod
import com.mind.app.core.model.ContentType
import com.mind.app.core.model.GeminiModelInfo
import com.mind.app.core.model.LayoutMode
import com.mind.app.core.model.Memory
import com.mind.app.core.model.MemoryDetail
import com.mind.app.core.model.SourceApplication
import com.mind.app.core.model.StorageStats
import com.mind.app.core.model.ThemeMode
import com.mind.app.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureMemoryUseCaseTest {

    @Test
    fun localOnlyModeSkipsAiQueueAndSavesImmediately() = runTest {
        val memoryRepo = RecordingMemoryRepository()
        val prefsRepo = InMemoryPrefsRepository(
            UserPreferences(cloudAiEnabled = false, localOnlyMode = true),
        )
        val sourceAppRepo = InMemorySourceAppRepository()
        val aiRepo = RecordingAiEngineRepository()
        val useCase = CaptureMemoryUseCase(memoryRepo, prefsRepo, sourceAppRepo, aiRepo)

        val saved = useCase.saveImageMemory(
            originalFilePath = "/data/user/0/com.mind.app/files/shot.jpg",
            thumbnailPath = "/data/user/0/com.mind.app/files/shot_thumb.jpg",
            captureMethod = CaptureMethod.QUICK_SETTINGS,
        )

        assertEquals(AiProcessingStatus.SKIPPED_LOCAL_ONLY, saved.aiStatus)
        assertEquals(CaptureMethod.QUICK_SETTINGS, saved.captureMethod)
        assertEquals(1, memoryRepo.savedMemories.size)
        assertTrue(aiRepo.enqueuedIds.isEmpty())
    }

    @Test
    fun cloudAiEnabledQueuesBackgroundAnalysisAndSavesVoiceAndUrls() = runTest {
        val memoryRepo = RecordingMemoryRepository()
        val prefsRepo = InMemoryPrefsRepository(
            UserPreferences(cloudAiEnabled = true, localOnlyMode = false),
        )
        val sourceAppRepo = InMemorySourceAppRepository()
        val aiRepo = RecordingAiEngineRepository()
        val useCase = CaptureMemoryUseCase(memoryRepo, prefsRepo, sourceAppRepo, aiRepo)

        val voice = useCase.saveVoiceMemory(
            audioFilePath = "/data/user/0/com.mind.app/files/voice.m4a",
            durationSeconds = 75,
        )
        assertEquals(ContentType.VOICE_NOTE, voice.contentType)
        assertEquals("Voice Memory (1:15)", voice.aiTitle)
        assertEquals(AiProcessingStatus.PENDING, voice.aiStatus)

        val quote = useCase.saveTextOrUrlMemory(
            text = "Simplicity is prerequisite for reliability.",
            isQuote = true,
        )
        assertEquals(ContentType.QUOTE, quote.contentType)

        val link = useCase.saveTextOrUrlMemory(
            text = "https://developer.android.com/media/grow/media-projection",
            url = "https://developer.android.com/media/grow/media-projection",
        )
        assertEquals(ContentType.URL, link.contentType)
        assertEquals(3, aiRepo.enqueuedIds.size)
    }

    @Test
    fun excludedSourceAppSkipsCloudAiEvenWhenCloudEnabled() = runTest {
        val memoryRepo = RecordingMemoryRepository()
        val prefsRepo = InMemoryPrefsRepository(
            UserPreferences(cloudAiEnabled = true, localOnlyMode = false),
        )
        val sourceAppRepo = InMemorySourceAppRepository(setOf("com.whatsapp"))
        val aiRepo = RecordingAiEngineRepository()
        val useCase = CaptureMemoryUseCase(memoryRepo, prefsRepo, sourceAppRepo, aiRepo)

        val saved = useCase.saveImageMemory(
            originalFilePath = "/data/user/0/com.mind.app/files/chat.jpg",
            thumbnailPath = "/data/user/0/com.mind.app/files/chat_thumb.jpg",
            sourcePackageName = "com.whatsapp",
            sourceAppName = "WhatsApp",
        )

        assertEquals(AiProcessingStatus.SKIPPED_LOCAL_ONLY, saved.aiStatus)
        assertEquals(1, memoryRepo.savedMemories.size)
        assertTrue(aiRepo.enqueuedIds.isEmpty())
        assertEquals(listOf(saved.id), aiRepo.enqueuedOcrIds)
    }

    @Test
    fun assistantScreenshotCapturePersistsMethodAndWebUrl() = runTest {
        val memoryRepo = RecordingMemoryRepository()
        val prefsRepo = InMemoryPrefsRepository(UserPreferences())
        assertEquals(AssistantAction.ASK_EVERY_TIME, UserPreferences().assistantAction)
        val useCase = CaptureMemoryUseCase(
            memoryRepo,
            prefsRepo,
            InMemorySourceAppRepository(),
            RecordingAiEngineRepository(),
        )

        val saved = useCase.saveImageMemory(
            originalFilePath = "/data/user/0/com.mind.app/files/assist.jpg",
            thumbnailPath = "/data/user/0/com.mind.app/files/assist_thumb.jpg",
            captureMethod = CaptureMethod.ASSISTANT,
            sourcePackageName = "com.android.chrome",
            sourceAppName = "Chrome",
            originalUrl = "https://developer.android.com",
        )

        assertEquals(CaptureMethod.ASSISTANT, saved.captureMethod)
        assertEquals("https://developer.android.com", saved.originalUrl)
        assertEquals("com.android.chrome", saved.sourcePackageName)
        assertEquals(1, memoryRepo.savedMemories.size)
    }

    private class InMemorySourceAppRepository(
        excluded: Set<String> = emptySet(),
    ) : SourceAppRepository {
        private val excludedFlow = MutableStateFlow(excluded)
        override fun observeSourceApps(): Flow<List<SourceApplication>> = MutableStateFlow(emptyList())
        override fun observeExcludedPackages(): Flow<Set<String>> = excludedFlow
        override suspend fun setAppExcluded(packageName: String, displayName: String, excluded: Boolean) {
            excludedFlow.value = if (excluded) excludedFlow.value + packageName else excludedFlow.value - packageName
        }
    }

    private class RecordingMemoryRepository : MemoryRepository {
        val savedMemories = mutableListOf<Memory>()
        override fun observeRecentMemories(limit: Int): Flow<List<Memory>> = MutableStateFlow(savedMemories)
        override fun observeFavouriteMemories(): Flow<List<Memory>> = MutableStateFlow(emptyList())
        override fun observeMemoriesByCategory(categoryId: String): Flow<List<Memory>> = MutableStateFlow(emptyList())
        override fun observeMemoryDetail(memoryId: String): Flow<MemoryDetail?> = MutableStateFlow(null)
        override fun observeTotalMemoryCount(): Flow<Int> = MutableStateFlow(savedMemories.size)
        override suspend fun getMemoryById(memoryId: String): Memory? = savedMemories.firstOrNull { it.id == memoryId }
        override suspend fun getRecentMemoriesSnapshot(limit: Int): List<Memory> = savedMemories.take(limit)
        override suspend fun getStorageStats(): StorageStats = StorageStats()
        override suspend fun clearCacheStorage() = Unit
        override suspend fun upsertMemory(memory: Memory) { savedMemories.add(memory) }
        override suspend fun updateFavourite(memoryId: String, isFavourite: Boolean) = Unit
        override suspend fun updateUserMetadata(memoryId: String, title: String?, notes: String?, categoryId: String?, subcategoryId: String?) = Unit
        override suspend fun updateAiStatus(memoryId: String, status: AiProcessingStatus) = Unit
        override suspend fun softDeleteMemory(memoryId: String, deletedAtEpochMillis: Long) = Unit
        override suspend fun restoreMemory(memoryId: String) = Unit
        override suspend fun hardDeleteMemory(memoryId: String) = Unit
        override suspend fun hardDeleteMemories(memoryIds: Collection<String>) {
            val idSet = memoryIds.toSet()
            savedMemories.removeAll { it.id in idSet }
        }
        override suspend fun deleteAllMemories() { savedMemories.clear() }
    }

    private class RecordingAiEngineRepository : AiEngineRepository {
        val enqueuedIds = mutableListOf<String>()
        val enqueuedOcrIds = mutableListOf<String>()
        override suspend fun enqueueAnalysis(memoryId: String) { enqueuedIds.add(memoryId) }
        override suspend fun enqueueLocalOcr(memoryId: String) { enqueuedOcrIds.add(memoryId) }
        override suspend fun scheduleRetryWhenOnline() = Unit
        override suspend fun analyzeNow(memoryId: String, markFailedOnError: Boolean): Result<Unit> = Result.success(Unit)
        override suspend fun cancelAnalysis(memoryId: String) = Unit
        override suspend fun retryFailedAnalyses(): Int = 0
        override suspend fun semanticSearch(query: String, limit: Int): Result<List<Memory>> = Result.success(emptyList())
        override suspend fun askMind(question: String): Result<AskMindAnswer> =
            Result.success(AskMindAnswer(question, "", emptyList()))
        override suspend fun askAboutMemory(memoryId: String, question: String): Result<String> = Result.success("")
        override suspend fun fetchAvailableModels(): Result<List<GeminiModelInfo>> = Result.success(emptyList())
        override suspend fun testConnection(): AiConnectionTestResult = AiConnectionTestResult(true, "OK")
    }

    private class InMemoryPrefsRepository(initial: UserPreferences) : PreferencesRepository {
        private val state = MutableStateFlow(initial)
        override fun observePreferences(): Flow<UserPreferences> = state
        override suspend fun setThemeMode(themeMode: ThemeMode) = Unit
        override suspend fun setAccentPalette(accentPalette: AccentPalette) = Unit
        override suspend fun setLayoutMode(layoutMode: LayoutMode) = Unit
        override suspend fun setAnimationStyle(animationStyle: AnimationStyle) = Unit
        override suspend fun setAnimationsEnabled(enabled: Boolean) = Unit
        override suspend fun setFloatingCaptureEnabled(enabled: Boolean) = Unit
        override suspend fun setCaptureConfirmationEnabled(enabled: Boolean) = Unit
        override suspend fun setCloudAiEnabled(enabled: Boolean) = Unit
        override suspend fun setLocalOnlyMode(localOnly: Boolean) = Unit
        override suspend fun setAutoCategorizationEnabled(enabled: Boolean) = Unit
        override suspend fun setAiLanguageTag(languageTag: String) = Unit
        override suspend fun setGeminiModel(modelId: String) = Unit
        override suspend fun setCustomBackendUrl(url: String) = Unit
        override suspend fun saveGeminiApiKey(apiKey: String) = Unit
        override suspend fun clearGeminiApiKey() = Unit
        override suspend fun setAssistantAction(action: AssistantAction) = Unit
        override suspend fun setOnboardingCompleted(completed: Boolean) = Unit
    }
}
