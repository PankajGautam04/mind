package com.mind.app.feature.collections

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.mind.app.core.domain.repository.CollectionRepository
import com.mind.app.core.domain.repository.PreferencesRepository
import com.mind.app.core.domain.usecase.ManageMemoryLifecycleUseCase
import com.mind.app.core.model.LayoutMode
import com.mind.app.core.model.Memory
import com.mind.app.core.model.SmartCollection
import com.mind.app.feature.common.MemorySelectionController
import com.mind.app.navigation.MindDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CollectionDetailUiState(
    val isLoading: Boolean = true,
    val collection: SmartCollection? = null,
    val memories: List<Memory> = emptyList(),
    val layoutMode: LayoutMode = LayoutMode.GRID,
    val isSelectionMode: Boolean = false,
    val selectedMemoryIds: Set<String> = emptySet(),
)

@HiltViewModel
class CollectionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val collectionRepository: CollectionRepository,
    preferencesRepository: PreferencesRepository,
    private val manageMemoryLifecycleUseCase: ManageMemoryLifecycleUseCase,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<MindDestination.CollectionDetail>()
    private val selectionController = MemorySelectionController()

    val uiState: StateFlow<CollectionDetailUiState> = combine(
        collectionRepository.observeCollections(),
        collectionRepository.observeMemoriesInCollection(route.collectionId),
        preferencesRepository.observePreferences(),
        selectionController.state,
    ) { collections, memories, prefs, selection ->
        val validIds = memories.mapTo(HashSet(memories.size)) { it.id }
        CollectionDetailUiState(
            isLoading = false,
            collection = collections.firstOrNull { it.id == route.collectionId },
            memories = memories,
            layoutMode = prefs.layoutMode,
            isSelectionMode = selection.isSelectionMode,
            selectedMemoryIds = selection.selectedIds.intersect(validIds),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CollectionDetailUiState(),
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

    fun toggleSelectAll() {
        selectionController.toggleSelectAll(uiState.value.memories.map { it.id })
    }

    fun clearSelection() {
        selectionController.clear()
    }

    fun deleteSelectedMemories() {
        val validIds = uiState.value.memories.mapTo(HashSet()) { it.id }
        val toDelete = selectionController.clear().intersect(validIds)
        if (toDelete.isEmpty()) return
        viewModelScope.launch {
            manageMemoryLifecycleUseCase.permanentlyDeleteBatch(toDelete)
        }
    }

    fun removeSelectedFromCollection() {
        val validIds = uiState.value.memories.mapTo(HashSet()) { it.id }
        val toRemove = selectionController.clear().intersect(validIds)
        if (toRemove.isEmpty()) return
        viewModelScope.launch {
            toRemove.forEach { memoryId ->
                collectionRepository.removeMemoryFromCollection(route.collectionId, memoryId)
            }
        }
    }

    fun toggleFavourite(memory: Memory) {
        viewModelScope.launch {
            manageMemoryLifecycleUseCase.toggleFavourite(memory.id, memory.isFavourite)
        }
    }

    fun removeMemoryFromCollection(memoryId: String) {
        viewModelScope.launch {
            collectionRepository.removeMemoryFromCollection(route.collectionId, memoryId)
        }
    }

    fun acceptSuggested() {
        viewModelScope.launch {
            collectionRepository.acceptSuggestedCollection(route.collectionId)
        }
    }

    fun deleteCollection(onDeleted: () -> Unit) {
        viewModelScope.launch {
            collectionRepository.deleteCollection(route.collectionId)
            onDeleted()
        }
    }
}
