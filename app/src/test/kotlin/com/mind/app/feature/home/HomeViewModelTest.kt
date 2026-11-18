package com.mind.app.feature.home

import com.mind.app.capture.ImportedCaptureMedia
import com.mind.app.capture.MediaImportBridge
import com.mind.app.core.domain.repository.AiEngineRepository
import com.mind.app.core.domain.repository.CategoryRepository
import com.mind.app.core.domain.repository.CollectionRepository
import com.mind.app.core.domain.repository.MemoryRepository
import com.mind.app.core.domain.repository.PreferencesRepository
import com.mind.app.core.domain.repository.SourceAppRepository
import com.mind.app.core.domain.usecase.CaptureMemoryUseCase
import com.mind.app.core.domain.usecase.ManageMemoryLifecycleUseCase
import com.mind.app.core.domain.usecase.ObserveHomeDashboardUseCase
import com.mind.app.core.model.AccentPalette
import com.mind.app.core.model.AiConnectionTestResult
import com.mind.app.core.model.AiProcessingStatus
import com.mind.app.core.model.AnimationStyle
import com.mind.app.core.model.AskMindAnswer
import com.mind.app.core.model.AssistantAction
import com.mind.app.core.model.Category
import com.mind.app.core.model.CategoryTreeNode
import com.mind.app.core.model.CollectionRule
import com.mind.app.core.model.ContentType
import com.mind.app.core.model.GeminiModelInfo
import com.mind.app.core.model.LayoutMode
import com.mind.app.core.model.Memory
import com.mind.app.core.model.MemoryDetail
import com.mind.app.core.model.SmartCollection
import com.mind.app.core.model.SourceApplication
import com.mind.app.core.model.StorageStats
import com.mind.app.core.model.ThemeMode
import com.mind.app.core.model.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun togglesLayoutModeFavouriteAndCapturesNoteWithUndo() = runTest {
        val prefsFlow = MutableStateFlow(UserPreferences(layoutMode = LayoutMode.GRID))
        val memoriesFlow = MutableStateFlow(
            listOf(
                Memory(
                    id = "m1",
                    aiTitle = "Galaxy S25 Ultra",
                    capturedAtEpochMillis = System.currentTimeMillis(),
                    updatedAtEpochMillis = System.currentTimeMillis(),
                    isFavourite = false,
                )
            )
        )

        val memoryRepo = FakeMemoryRepository(memoriesFlow)
        val prefsRepo = FakePreferencesRepository(prefsFlow)
        val aiRepo = FakeAiEngineRepository()
        val collectionRepo = FakeCollectionRepository()
        val viewModel = HomeViewModel(
            observeHomeDashboardUseCase = ObserveHomeDashboardUseCase(
                memoryRepository = memoryRepo,
                categoryRepository = FakeCategoryRepository(),
                collectionRepository = collectionRepo,
            ),
            preferencesRepository = prefsRepo,
            manageMemoryLifecycleUseCase = ManageMemoryLifecycleUseCase(memoryRepo, aiRepo),
            captureMemoryUseCase = CaptureMemoryUseCase(
                memoryRepository = memoryRepo,
                preferencesRepository = prefsRepo,
                sourceAppRepository = FakeSourceAppRepository(),
                aiEngineRepository = aiRepo,
            ),
            collectionRepository = collectionRepo,
            aiEngineRepository = aiRepo,
            mediaImportBridge = FakeMediaImportBridge(),
        )

        val collectJob = launch { viewModel.uiState.collect() }
        advanceUntilIdle()
        assertEquals(LayoutMode.GRID, viewModel.uiState.value.layoutMode)
        assertEquals(1, viewModel.uiState.value.filteredMemories.size)

        viewModel.toggleLayoutMode()
        advanceUntilIdle()
        assertEquals(LayoutMode.LIST, viewModel.uiState.value.layoutMode)

        viewModel.toggleFavourite(viewModel.uiState.value.dashboard.recentMemories.first())
        advanceUntilIdle()
        assertEquals(true, memoryRepo.lastFavouriteUpdate)

        viewModel.saveQuickNote(title = "AWS Fix", body = "Check IAM role policy", isQuote = false)
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.dashboard.recentMemories.size)
        assertNotNull(viewModel.uiState.value.activeSaveOverlay)

        viewModel.undoLastCapture()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.activeSaveOverlay)
        assertEquals(1, viewModel.uiState.value.dashboard.recentMemories.size)

        viewModel.selectFilter(HomeTimelineFilter.VOICE)
        advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.filteredMemories.size)

        collectJob.cancel()
    }

    @Test
    fun multiSelectSelectAllAndBulkDeleteRemovesSelectedMemoriesAndCancelsWork() = runTest {
        val now = System.currentTimeMillis()
        val prefsFlow = MutableStateFlow(UserPreferences(layoutMode = LayoutMode.GRID))
        val memoriesFlow = MutableStateFlow(
            listOf(
                Memory(id = "m1", aiTitle = "One", capturedAtEpochMillis = now, updatedAtEpochMillis = now),
                Memory(id = "m2", aiTitle = "Two", capturedAtEpochMillis = now - 1000, updatedAtEpochMillis = now),
                Memory(id = "m3", aiTitle = "Three", capturedAtEpochMillis = now - 2000, updatedAtEpochMillis = now),
            )
        )
        val memoryRepo = FakeMemoryRepository(memoriesFlow)
        val prefsRepo = FakePreferencesRepository(prefsFlow)
        val aiRepo = FakeAiEngineRepository()
        val collectionRepo = FakeCollectionRepository()
        val viewModel = HomeViewModel(
            observeHomeDashboardUseCase = ObserveHomeDashboardUseCase(
                memoryRepository = memoryRepo,
                categoryRepository = FakeCategoryRepository(),
                collectionRepository = collectionRepo,
            ),
            preferencesRepository = prefsRepo,
            manageMemoryLifecycleUseCase = ManageMemoryLifecycleUseCase(memoryRepo, aiRepo),
            captureMemoryUseCase = CaptureMemoryUseCase(
                memoryRepository = memoryRepo,
                preferencesRepository = prefsRepo,
                sourceAppRepository = FakeSourceAppRepository(),
                aiEngineRepository = aiRepo,
            ),
            collectionRepository = collectionRepo,
            aiEngineRepository = aiRepo,
            mediaImportBridge = FakeMediaImportBridge(),
        )

        val collectJob = launch { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.enterSelectionMode("m1")
        advanceUntilIdle()
        assertEquals(true, viewModel.uiState.value.isSelectionMode)
        assertEquals(setOf("m1"), viewModel.uiState.value.selectedMemoryIds)

        viewModel.toggleMemorySelection("m3")
        advanceUntilIdle()
        assertEquals(setOf("m1", "m3"), viewModel.uiState.value.selectedMemoryIds)

        viewModel.toggleSelectAllVisible()
        advanceUntilIdle()
        assertEquals(setOf("m1", "m2", "m3"), viewModel.uiState.value.selectedMemoryIds)

        viewModel.toggleMemorySelection("m2")
        advanceUntilIdle()
        assertEquals(setOf("m1", "m3"), viewModel.uiState.value.selectedMemoryIds)

        viewModel.deleteSelectedMemories()
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isSelectionMode)
        assertEquals(listOf("m2"), viewModel.uiState.value.filteredMemories.map { it.id })
        assertEquals(setOf("m1", "m3"), aiRepo.cancelledIds.toSet())

        collectJob.cancel()
    }

    private class FakeMemoryRepository(
        private val memories: MutableStateFlow<List<Memory>>,
    ) : MemoryRepository {
        var lastFavouriteUpdate: Boolean? = null
        override fun observeRecentMemories(limit: Int): Flow<List<Memory>> = memories
        override fun observeFavouriteMemories(): Flow<List<Memory>> = MutableStateFlow(emptyList())
        override fun observeMemoriesByCategory(categoryId: String): Flow<List<Memory>> = MutableStateFlow(emptyList())
        override fun observeMemoryDetail(memoryId: String): Flow<MemoryDetail?> = MutableStateFlow(null)
        override fun observeTotalMemoryCount(): Flow<Int> = MutableStateFlow(memories.value.size)
        override suspend fun getMemoryById(memoryId: String): Memory? = memories.value.firstOrNull { it.id == memoryId }
        override suspend fun getRecentMemoriesSnapshot(limit: Int): List<Memory> = memories.value.take(limit)
        override suspend fun getStorageStats(): StorageStats = StorageStats(totalMemoryCount = memories.value.size)
        override suspend fun clearCacheStorage() = Unit
        override suspend fun upsertMemory(memory: Memory) {
            memories.value = listOf(memory) + memories.value.filterNot { it.id == memory.id }
        }
        override suspend fun updateFavourite(memoryId: String, isFavourite: Boolean) {
            lastFavouriteUpdate = isFavourite
        }
        override suspend fun updateUserMetadata(
            memoryId: String,
            title: String?,
            notes: String?,
            categoryId: String?,
            subcategoryId: String?,
        ) = Unit
        override suspend fun updateAiStatus(memoryId: String, status: AiProcessingStatus) = Unit
        override suspend fun softDeleteMemory(memoryId: String, deletedAtEpochMillis: Long) {
            memories.value = memories.value.filterNot { it.id == memoryId }
        }
        override suspend fun restoreMemory(memoryId: String) = Unit
        override suspend fun hardDeleteMemory(memoryId: String) {
            memories.value = memories.value.filterNot { it.id == memoryId }
        }
        override suspend fun hardDeleteMemories(memoryIds: Collection<String>) {
            val idSet = memoryIds.toSet()
            memories.value = memories.value.filterNot { it.id in idSet }
        }
        override suspend fun deleteAllMemories() {
            memories.value = emptyList()
        }
    }

    private class FakeCategoryRepository : CategoryRepository {
        override fun observeCategoryTree(): Flow<List<CategoryTreeNode>> = MutableStateFlow(emptyList())
        override fun observeAllCategories(): Flow<List<Category>> = MutableStateFlow(emptyList())
        override suspend fun createCategory(name: String, parentId: String?, isAiSuggested: Boolean) = Category("id", name)
        override suspend fun renameCategory(categoryId: String, newName: String) = Unit
        override suspend fun deleteCategory(categoryId: String, reassignToCategoryId: String?) = Unit
        override suspend fun mergeCategories(sourceCategoryId: String, targetCategoryId: String) = Unit
    }

    private class FakeCollectionRepository : CollectionRepository {
        override fun observeCollections(): Flow<List<SmartCollection>> = MutableStateFlow(emptyList())
        override fun observeMemoriesInCollection(collectionId: String): Flow<List<Memory>> = MutableStateFlow(emptyList())
        override suspend fun createCollection(name: String, description: String?, isSmartCollection: Boolean, rule: CollectionRule?) =
            SmartCollection("c1", name)
        override suspend fun addMemoryToCollection(collectionId: String, memoryId: String, isAutoAdded: Boolean) = Unit
        override suspend fun removeMemoryFromCollection(collectionId: String, memoryId: String) = Unit
        override suspend fun acceptSuggestedCollection(collectionId: String) = Unit
        override suspend fun deleteCollection(collectionId: String) = Unit
    }

    private class FakePreferencesRepository(
        private val flow: MutableStateFlow<UserPreferences>,
    ) : PreferencesRepository {
        override fun observePreferences(): Flow<UserPreferences> = flow
        override suspend fun setThemeMode(themeMode: ThemeMode) { flow.value = flow.value.copy(themeMode = themeMode) }
        override suspend fun setAccentPalette(accentPalette: AccentPalette) { flow.value = flow.value.copy(accentPalette = accentPalette) }
        override suspend fun setLayoutMode(layoutMode: LayoutMode) { flow.value = flow.value.copy(layoutMode = layoutMode) }
        override suspend fun setAnimationStyle(animationStyle: AnimationStyle) { flow.value = flow.value.copy(animationStyle = animationStyle) }
        override suspend fun setAnimationsEnabled(enabled: Boolean) { flow.value = flow.value.copy(animationsEnabled = enabled) }
        override suspend fun setFloatingCaptureEnabled(enabled: Boolean) { flow.value = flow.value.copy(floatingCaptureEnabled = enabled) }
        override suspend fun setCaptureConfirmationEnabled(enabled: Boolean) { flow.value = flow.value.copy(captureConfirmationEnabled = enabled) }
        override suspend fun setCloudAiEnabled(enabled: Boolean) { flow.value = flow.value.copy(cloudAiEnabled = enabled) }
        override suspend fun setLocalOnlyMode(localOnly: Boolean) { flow.value = flow.value.copy(localOnlyMode = localOnly) }
        override suspend fun setAutoCategorizationEnabled(enabled: Boolean) { flow.value = flow.value.copy(autoCategorizationEnabled = enabled) }
        override suspend fun setAiLanguageTag(languageTag: String) { flow.value = flow.value.copy(aiLanguageTag = languageTag) }
        override suspend fun setGeminiModel(modelId: String) { flow.value = flow.value.copy(geminiModel = modelId) }
        override suspend fun setCustomBackendUrl(url: String) { flow.value = flow.value.copy(customBackendUrl = url) }
        override suspend fun saveGeminiApiKey(apiKey: String) { flow.value = flow.value.copy(hasGeminiApiKey = true) }
        override suspend fun clearGeminiApiKey() { flow.value = flow.value.copy(hasGeminiApiKey = false) }
        override suspend fun setAssistantAction(action: AssistantAction) { flow.value = flow.value.copy(assistantAction = action) }
        override suspend fun setOnboardingCompleted(completed: Boolean) { flow.value = flow.value.copy(onboardingCompleted = completed) }
    }

    private class FakeSourceAppRepository : SourceAppRepository {
        override fun observeSourceApps(): Flow<List<SourceApplication>> = MutableStateFlow(emptyList())
        override fun observeExcludedPackages(): Flow<Set<String>> = MutableStateFlow(emptySet())
        override suspend fun setAppExcluded(packageName: String, displayName: String, excluded: Boolean) = Unit
    }

    private class FakeAiEngineRepository : AiEngineRepository {
        val cancelledIds = mutableListOf<String>()
        override suspend fun enqueueAnalysis(memoryId: String) = Unit
        override suspend fun enqueueLocalOcr(memoryId: String) = Unit
        override suspend fun scheduleRetryWhenOnline() = Unit
        override suspend fun analyzeNow(memoryId: String, markFailedOnError: Boolean): Result<Unit> = Result.success(Unit)
        override suspend fun cancelAnalysis(memoryId: String) { cancelledIds.add(memoryId) }
        override suspend fun retryFailedAnalyses(): Int = 0
        override suspend fun semanticSearch(query: String, limit: Int): Result<List<Memory>> = Result.success(emptyList())
        override suspend fun askMind(question: String): Result<AskMindAnswer> =
            Result.success(AskMindAnswer(question, "Ok", emptyList()))
        override suspend fun askAboutMemory(memoryId: String, question: String): Result<String> = Result.success("Ok")
        override suspend fun fetchAvailableModels(): Result<List<GeminiModelInfo>> = Result.success(emptyList())
        override suspend fun testConnection(): AiConnectionTestResult = AiConnectionTestResult(true, "OK")
    }

    private class FakeMediaImportBridge : MediaImportBridge {
        override suspend fun importUri(uriString: String): ImportedCaptureMedia? = ImportedCaptureMedia(
            originalPath = "/tmp/mock.jpg",
            thumbnailPath = "/tmp/mock_thumb.jpg",
            contentType = ContentType.IMAGE,
        )
        override fun allocateVoiceOutputPath(): String = "/tmp/mock_voice.m4a"
    }
}
