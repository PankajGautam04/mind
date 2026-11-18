package com.mind.app.feature.search

import androidx.lifecycle.SavedStateHandle
import com.mind.app.core.domain.repository.AiEngineRepository
import com.mind.app.core.domain.repository.CategoryRepository
import com.mind.app.core.domain.repository.CollectionRepository
import com.mind.app.core.domain.repository.MemoryRepository
import com.mind.app.core.domain.repository.PreferencesRepository
import com.mind.app.core.domain.repository.SearchRepository
import com.mind.app.core.domain.usecase.ManageMemoryLifecycleUseCase
import com.mind.app.core.domain.usecase.SearchMemoriesUseCase
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
import com.mind.app.core.model.MemorySearchFilter
import com.mind.app.core.model.SmartCollection
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
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun unifiedSearchBlendsLocalSemanticAndAskAnswerAndAddsSelectedToSpace() = runTest {
        val localMem = Memory(
            id = "m1",
            aiTitle = "Coffee receipt",
            contentType = ContentType.SCREENSHOT,
            capturedAtEpochMillis = 1_000L,
            updatedAtEpochMillis = 1_000L,
        )
        val semanticMem = Memory(
            id = "m2",
            aiTitle = "Espresso machine notes",
            contentType = ContentType.TEXT,
            capturedAtEpochMillis = 2_000L,
            updatedAtEpochMillis = 2_000L,
        )

        val collectionRepo = FakeCollectionRepository()
        val viewModel = SearchViewModel(
            savedStateHandle = SavedStateHandle(),
            searchMemoriesUseCase = SearchMemoriesUseCase(
                searchRepository = FakeSearchRepository(listOf(localMem)),
            ),
            categoryRepository = FakeCategoryRepository(),
            collectionRepository = collectionRepo,
            preferencesRepository = FakePreferencesRepository(),
            manageMemoryLifecycleUseCase = ManageMemoryLifecycleUseCase(
                memoryRepository = FakeMemoryRepository(),
                aiEngineRepository = FakeAiEngineRepository(semanticHits = listOf(semanticMem)),
            ),
            aiEngineRepository = FakeAiEngineRepository(semanticHits = listOf(semanticMem)),
        )

        val collectJob = launch { viewModel.uiState.collect() }
        advanceTimeBy(300)
        advanceUntilIdle()

        assertEquals(listOf("m1"), viewModel.uiState.value.results.map { it.id })

        viewModel.submitSmartQuery("coffee")
        advanceTimeBy(300)
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.askAnswer)
        assertEquals(listOf("m1", "m2"), viewModel.uiState.value.results.map { it.id })
        assertEquals(setOf("m2"), viewModel.uiState.value.semanticMatchIds)

        viewModel.enterSelectionMode("m1")
        viewModel.toggleMemorySelection("m2")
        advanceUntilIdle()

        viewModel.createSpaceAndAddSelected("Cafe Log")
        advanceUntilIdle()

        assertEquals(1, collectionRepo.collections.size)
        assertEquals("Cafe Log", collectionRepo.collections.first().name)
        assertEquals(setOf("m1", "m2"), collectionRepo.memberships[collectionRepo.collections.first().id])
        assertTrue(viewModel.uiState.value.selectedMemoryIds.isEmpty())

        collectJob.cancel()
    }

    private class FakeSearchRepository(
        private val items: List<Memory>,
    ) : SearchRepository {
        override fun searchMemories(filter: MemorySearchFilter): Flow<List<Memory>> =
            MutableStateFlow(items)
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
        val collections = mutableListOf<SmartCollection>()
        val memberships = mutableMapOf<String, MutableSet<String>>()
        private val flow = MutableStateFlow<List<SmartCollection>>(emptyList())

        override fun observeCollections(): Flow<List<SmartCollection>> = flow
        override fun observeMemoriesInCollection(collectionId: String): Flow<List<Memory>> =
            MutableStateFlow(emptyList())

        override suspend fun createCollection(
            name: String,
            description: String?,
            isSmartCollection: Boolean,
            rule: CollectionRule?,
        ): SmartCollection {
            val col = SmartCollection(
                id = "space_${collections.size + 1}",
                name = name,
                description = description,
                createdAtEpochMillis = 1_000L,
            )
            collections.add(col)
            flow.value = collections.toList()
            return col
        }

        override suspend fun addMemoryToCollection(collectionId: String, memoryId: String, isAutoAdded: Boolean) {
            memberships.getOrPut(collectionId) { mutableSetOf() }.add(memoryId)
        }

        override suspend fun removeMemoryFromCollection(collectionId: String, memoryId: String) {
            memberships[collectionId]?.remove(memoryId)
        }

        override suspend fun acceptSuggestedCollection(collectionId: String) = Unit
        override suspend fun deleteCollection(collectionId: String) = Unit
    }

    private class FakePreferencesRepository : PreferencesRepository {
        private val prefs = MutableStateFlow(UserPreferences())
        override fun observePreferences(): Flow<UserPreferences> = prefs
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

    private class FakeMemoryRepository : MemoryRepository {
        override fun observeRecentMemories(limit: Int): Flow<List<Memory>> = MutableStateFlow(emptyList())
        override fun observeFavouriteMemories(): Flow<List<Memory>> = MutableStateFlow(emptyList())
        override fun observeMemoriesByCategory(categoryId: String): Flow<List<Memory>> = MutableStateFlow(emptyList())
        override fun observeMemoryDetail(memoryId: String): Flow<MemoryDetail?> = MutableStateFlow(null)
        override fun observeTotalMemoryCount(): Flow<Int> = MutableStateFlow(0)
        override suspend fun getMemoryById(memoryId: String): Memory? = null
        override suspend fun getRecentMemoriesSnapshot(limit: Int): List<Memory> = emptyList()
        override suspend fun getStorageStats(): StorageStats = StorageStats(0, 0L)
        override suspend fun clearCacheStorage() = Unit
        override suspend fun upsertMemory(memory: Memory) = Unit
        override suspend fun updateFavourite(memoryId: String, isFavourite: Boolean) = Unit
        override suspend fun updateUserMetadata(
            memoryId: String,
            title: String?,
            notes: String?,
            categoryId: String?,
            subcategoryId: String?,
        ) = Unit
        override suspend fun updateAiStatus(memoryId: String, status: AiProcessingStatus) = Unit
        override suspend fun softDeleteMemory(memoryId: String, deletedAtEpochMillis: Long) = Unit
        override suspend fun restoreMemory(memoryId: String) = Unit
        override suspend fun hardDeleteMemory(memoryId: String) = Unit
        override suspend fun hardDeleteMemories(memoryIds: Collection<String>) = Unit
        override suspend fun deleteAllMemories() = Unit
    }

    private class FakeAiEngineRepository(
        private val semanticHits: List<Memory>,
    ) : AiEngineRepository {
        override suspend fun enqueueAnalysis(memoryId: String) = Unit
        override suspend fun enqueueLocalOcr(memoryId: String) = Unit
        override suspend fun scheduleRetryWhenOnline() = Unit
        override suspend fun analyzeNow(memoryId: String, markFailedOnError: Boolean): Result<Unit> = Result.success(Unit)
        override suspend fun cancelAnalysis(memoryId: String) = Unit
        override suspend fun retryFailedAnalyses(): Int = 0
        override suspend fun semanticSearch(query: String, limit: Int): Result<List<Memory>> =
            Result.success(semanticHits)
        override suspend fun askMind(question: String): Result<AskMindAnswer> =
            Result.success(AskMindAnswer(question, "You saved a coffee receipt.", semanticHits))
        override suspend fun askAboutMemory(memoryId: String, question: String): Result<String> =
            Result.success("Done")
        override suspend fun fetchAvailableModels(): Result<List<GeminiModelInfo>> = Result.success(emptyList())
        override suspend fun testConnection(): AiConnectionTestResult = AiConnectionTestResult(true, "OK")
    }
}
