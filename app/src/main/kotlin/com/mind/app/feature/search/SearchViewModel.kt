package com.mind.app.feature.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.mind.app.core.domain.repository.AiEngineRepository
import com.mind.app.core.domain.repository.CategoryRepository
import com.mind.app.core.domain.repository.CollectionRepository
import com.mind.app.core.domain.repository.PreferencesRepository
import com.mind.app.core.domain.usecase.ManageMemoryLifecycleUseCase
import com.mind.app.core.domain.usecase.SearchMemoriesUseCase
import com.mind.app.core.model.AskMindAnswer
import com.mind.app.core.model.CategoryTreeNode
import com.mind.app.core.model.ContentType
import com.mind.app.core.model.LayoutMode
import com.mind.app.core.model.Memory
import com.mind.app.core.model.MemorySearchFilter
import com.mind.app.core.model.SmartCollection
import com.mind.app.feature.common.MemorySelectionController
import com.mind.app.navigation.MindDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private data class AiAssistState(
    val activeAiQuery: String = "",
    val isAiRunning: Boolean = false,
    val semanticResults: List<Memory> = emptyList(),
    val askAnswer: AskMindAnswer? = null,
    val aiError: String? = null,
    val hasRunAiSearch: Boolean = false,
)

private data class SearchExtraState(
    val collections: List<SmartCollection>,
    val assist: AiAssistState,
    val selectionIds: Set<String>,
    val isSelectionMode: Boolean,
    val statusMessage: String?,
)

data class SearchUiState(
    val filter: MemorySearchFilter = MemorySearchFilter(),
    val results: List<Memory> = emptyList(),
    val semanticMatchIds: Set<String> = emptySet(),
    val categories: List<CategoryTreeNode> = emptyList(),
    val collections: List<SmartCollection> = emptyList(),
    val layoutMode: LayoutMode = LayoutMode.LIST,
    val isAiRunning: Boolean = false,
    val askAnswer: AskMindAnswer? = null,
    val aiError: String? = null,
    val hasRunAiSearch: Boolean = false,
    val isSelectionMode: Boolean = false,
    val selectedMemoryIds: Set<String> = emptySet(),
    val statusMessage: String? = null,
) {
    val visibleSelectableIds: List<String>
        get() = results.map { it.id }
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val searchMemoriesUseCase: SearchMemoriesUseCase,
    categoryRepository: CategoryRepository,
    private val collectionRepository: CollectionRepository,
    private val preferencesRepository: PreferencesRepository,
    private val manageMemoryLifecycleUseCase: ManageMemoryLifecycleUseCase,
    private val aiEngineRepository: AiEngineRepository,
) : ViewModel() {

    private val initialRoute = savedStateHandle.toRoute<MindDestination.Search>()
    private val filterFlow = MutableStateFlow(
        MemorySearchFilter(categoryId = initialRoute.categoryId)
    )
    private val aiAssistFlow = MutableStateFlow(AiAssistState())
    private val statusMessageFlow = MutableStateFlow<String?>(null)
    private val selectionController = MemorySelectionController()

    private val localResultsFlow = filterFlow
        .debounce(150)
        .flatMapLatest { filter -> searchMemoriesUseCase(filter) }

    private val extraStateFlow = combine(
        collectionRepository.observeCollections(),
        aiAssistFlow,
        selectionController.state,
        statusMessageFlow,
    ) { collections, assist, selection, status ->
        SearchExtraState(
            collections = collections,
            assist = assist,
            selectionIds = selection.selectedIds,
            isSelectionMode = selection.isSelectionMode,
            statusMessage = status,
        )
    }

    val uiState: StateFlow<SearchUiState> = combine(
        filterFlow,
        localResultsFlow,
        categoryRepository.observeCategoryTree(),
        preferencesRepository.observePreferences(),
        extraStateFlow,
    ) { filter, localResults, categories, prefs, extra ->
        val assist = extra.assist
        val aiMatchesCurrentQuery = assist.hasRunAiSearch &&
            filter.query.trim().equals(assist.activeAiQuery, ignoreCase = true)

        val mergedResults = if (aiMatchesCurrentQuery) {
            mergeUnifiedResults(localResults, assist.semanticResults, filter)
        } else {
            localResults
        }
        val semanticOnlyIds = if (aiMatchesCurrentQuery) {
            val localIdSet = localResults.mapTo(HashSet()) { it.id }
            assist.semanticResults.mapNotNullTo(HashSet()) { if (it.id !in localIdSet) it.id else null }
        } else {
            emptySet()
        }
        val validIds = mergedResults.mapTo(HashSet()) { it.id }

        SearchUiState(
            filter = filter,
            results = mergedResults,
            semanticMatchIds = semanticOnlyIds,
            categories = categories,
            collections = extra.collections,
            layoutMode = prefs.layoutMode,
            isAiRunning = assist.isAiRunning,
            askAnswer = if (aiMatchesCurrentQuery || assist.isAiRunning) assist.askAnswer else null,
            aiError = if (aiMatchesCurrentQuery || assist.isAiRunning) assist.aiError else null,
            hasRunAiSearch = aiMatchesCurrentQuery,
            isSelectionMode = extra.isSelectionMode,
            selectedMemoryIds = extra.selectionIds.intersect(validIds),
            statusMessage = extra.statusMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SearchUiState(filter = filterFlow.value),
    )

    fun enterSelectionMode(memoryId: String? = null) {
        if (memoryId != null) {
            selectionController.enterWith(memoryId)
        } else {
            selectionController.startSelection()
        }
    }

    fun toggleMemorySelection(memoryId: String) {
        selectionController.toggle(memoryId)
    }

    fun toggleSelectAllVisible() {
        selectionController.toggleSelectAll(uiState.value.visibleSelectableIds)
    }

    fun clearSelection() {
        selectionController.clear()
    }

    fun deleteSelectedMemories() {
        val visibleIds = uiState.value.visibleSelectableIds.toSet()
        val toDelete = selectionController.clear().intersect(visibleIds)
        if (toDelete.isEmpty()) return
        aiAssistFlow.value = aiAssistFlow.value.let { assist ->
            assist.copy(
                semanticResults = assist.semanticResults.filterNot { it.id in toDelete },
                askAnswer = assist.askAnswer?.let { answer ->
                    answer.copy(citedMemories = answer.citedMemories.filterNot { it.id in toDelete })
                },
            )
        }
        viewModelScope.launch {
            manageMemoryLifecycleUseCase.permanentlyDeleteBatch(toDelete)
        }
    }

    fun addSelectedMemoriesToCollection(collectionId: String, collectionName: String) {
        val visibleIds = uiState.value.visibleSelectableIds.toSet()
        val toAdd = selectionController.clear().intersect(visibleIds)
        if (toAdd.isEmpty()) return
        viewModelScope.launch {
            toAdd.forEach { memoryId ->
                collectionRepository.addMemoryToCollection(collectionId, memoryId)
            }
            statusMessageFlow.value = "Added ${toAdd.size} ${if (toAdd.size == 1) "item" else "items"} to $collectionName"
        }
    }

    fun createSpaceAndAddSelected(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val visibleIds = uiState.value.visibleSelectableIds.toSet()
        val toAdd = selectionController.clear().intersect(visibleIds)
        viewModelScope.launch {
            val created = collectionRepository.createCollection(trimmed)
            toAdd.forEach { memoryId ->
                collectionRepository.addMemoryToCollection(created.id, memoryId)
            }
            statusMessageFlow.value = if (toAdd.isNotEmpty()) {
                "Created $trimmed and added ${toAdd.size} ${if (toAdd.size == 1) "item" else "items"}"
            } else {
                "Created Space $trimmed"
            }
        }
    }

    fun updateQuery(query: String) {
        filterFlow.value = filterFlow.value.copy(query = query)
        if (query.isBlank()) {
            aiAssistFlow.value = AiAssistState()
        }
    }

    fun submitSmartQuery(explicitQuery: String? = null) {
        val query = (explicitQuery ?: filterFlow.value.query).trim()
        if (query.isEmpty() || aiAssistFlow.value.isAiRunning) return

        filterFlow.value = filterFlow.value.copy(query = query)
        aiAssistFlow.value = AiAssistState(
            activeAiQuery = query,
            isAiRunning = true,
            hasRunAiSearch = true,
        )

        viewModelScope.launch {
            val (semanticRes, askRes) = coroutineScope {
                val semanticDeferred = async { aiEngineRepository.semanticSearch(query) }
                val askDeferred = async { aiEngineRepository.askMind(query) }
                semanticDeferred.await() to askDeferred.await()
            }

            val combinedError = when {
                askRes.isFailure && semanticRes.isFailure ->
                    askRes.exceptionOrNull()?.message
                        ?: semanticRes.exceptionOrNull()?.message
                        ?: "Cloud AI search unavailable right now."
                askRes.isFailure -> askRes.exceptionOrNull()?.message
                else -> null
            }

            aiAssistFlow.value = AiAssistState(
                activeAiQuery = query,
                isAiRunning = false,
                semanticResults = semanticRes.getOrDefault(emptyList()),
                askAnswer = askRes.getOrNull(),
                aiError = combinedError,
                hasRunAiSearch = true,
            )
        }
    }

    fun dismissAiAssist() {
        aiAssistFlow.value = AiAssistState()
    }

    fun clearStatusMessage() {
        statusMessageFlow.value = null
    }

    fun selectCategory(categoryId: String?) {
        val current = filterFlow.value.categoryId
        filterFlow.value = filterFlow.value.copy(
            categoryId = if (current == categoryId) null else categoryId
        )
    }

    fun toggleContentType(type: ContentType) {
        val current = filterFlow.value.contentTypes
        val updated = if (type in current) current - type else current + type
        filterFlow.value = filterFlow.value.copy(contentTypes = updated)
    }

    fun toggleFavouritesOnly() {
        filterFlow.value = filterFlow.value.copy(favouritesOnly = !filterFlow.value.favouritesOnly)
    }

    fun clearFilters() {
        filterFlow.value = MemorySearchFilter()
        aiAssistFlow.value = AiAssistState()
    }

    fun toggleFavourite(memory: Memory) {
        viewModelScope.launch {
            manageMemoryLifecycleUseCase.toggleFavourite(memory.id, memory.isFavourite)
        }
    }

    fun toggleLayoutMode() {
        val current = uiState.value.layoutMode
        val next = if (current == LayoutMode.GRID) LayoutMode.LIST else LayoutMode.GRID
        viewModelScope.launch {
            preferencesRepository.setLayoutMode(next)
        }
    }

    private fun mergeUnifiedResults(
        local: List<Memory>,
        semantic: List<Memory>,
        filter: MemorySearchFilter,
    ): List<Memory> {
        val merged = LinkedHashMap<String, Memory>(local.size + semantic.size)
        local.forEach { merged[it.id] = it }
        semantic.filter { memory ->
            matchesStructuredFilters(memory, filter)
        }.forEach { merged.putIfAbsent(it.id, it) }
        return merged.values.toList()
    }

    private fun matchesStructuredFilters(memory: Memory, filter: MemorySearchFilter): Boolean {
        if (filter.favouritesOnly && !memory.isFavourite) return false
        if (filter.contentTypes.isNotEmpty() && memory.contentType !in filter.contentTypes) return false
        val catId = filter.categoryId ?: return true
        return memory.primaryCategory?.id == catId || memory.subcategory?.id == catId
    }
}
