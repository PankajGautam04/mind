package com.mind.app.feature.common

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MemorySelectionState(
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<String> = emptySet(),
)

/**
 * Shared multi-select state coordinator reused across memory feed ViewModels.
 */
class MemorySelectionController {
    private val _state = MutableStateFlow(MemorySelectionState())
    val state: StateFlow<MemorySelectionState> = _state.asStateFlow()

    fun enterWith(memoryId: String) {
        _state.value = MemorySelectionState(
            isSelectionMode = true,
            selectedIds = setOf(memoryId),
        )
    }

    fun startSelection() {
        _state.value = _state.value.copy(isSelectionMode = true)
    }

    fun toggle(memoryId: String) {
        val current = _state.value
        val updated = if (memoryId in current.selectedIds) {
            current.selectedIds - memoryId
        } else {
            current.selectedIds + memoryId
        }
        _state.value = current.copy(
            isSelectionMode = true,
            selectedIds = updated,
        )
    }

    fun toggleSelectAll(visibleIds: List<String>) {
        val visibleSet = visibleIds.toSet()
        if (visibleSet.isEmpty()) return
        val current = _state.value.selectedIds
        val allVisibleSelected = visibleSet.all { it in current }
        _state.value = MemorySelectionState(
            isSelectionMode = true,
            selectedIds = if (allVisibleSelected) current - visibleSet else current + visibleSet,
        )
    }

    fun clear(): Set<String> {
        val removed = _state.value.selectedIds
        _state.value = MemorySelectionState()
        return removed
    }
}
