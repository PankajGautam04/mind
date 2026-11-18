package com.mind.app.feature.favourites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mind.app.core.domain.repository.MemoryRepository
import com.mind.app.core.domain.repository.PreferencesRepository
import com.mind.app.core.domain.usecase.ManageMemoryLifecycleUseCase
import com.mind.app.core.model.LayoutMode
import com.mind.app.core.model.Memory
import com.mind.app.feature.common.MemorySelectionController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavouritesUiState(
    val memories: List<Memory> = emptyList(),
    val layoutMode: LayoutMode = LayoutMode.GRID,
    val isSelectionMode: Boolean = false,
    val selectedMemoryIds: Set<String> = emptySet(),
)

@HiltViewModel
class FavouritesViewModel @Inject constructor(
    memoryRepository: MemoryRepository,
    private val preferencesRepository: PreferencesRepository,
    private val manageMemoryLifecycleUseCase: ManageMemoryLifecycleUseCase,
) : ViewModel() {

    private val selectionController = MemorySelectionController()

    val uiState: StateFlow<FavouritesUiState> = combine(
        memoryRepository.observeFavouriteMemories(),
        preferencesRepository.observePreferences(),
        selectionController.state,
    ) { favourites, prefs, selection ->
        val validIds = favourites.mapTo(HashSet(favourites.size)) { it.id }
        FavouritesUiState(
            memories = favourites,
            layoutMode = prefs.layoutMode,
            isSelectionMode = selection.isSelectionMode,
            selectedMemoryIds = selection.selectedIds.intersect(validIds),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FavouritesUiState(),
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

    fun toggleLayoutMode() {
        val next = if (uiState.value.layoutMode == LayoutMode.GRID) LayoutMode.LIST else LayoutMode.GRID
        viewModelScope.launch {
            preferencesRepository.setLayoutMode(next)
        }
    }

    fun toggleFavourite(memory: Memory) {
        viewModelScope.launch {
            manageMemoryLifecycleUseCase.toggleFavourite(memory.id, memory.isFavourite)
        }
    }
}
