package com.mind.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mind.app.capture.MediaImportBridge
import com.mind.app.core.domain.repository.AiEngineRepository
import com.mind.app.core.domain.repository.CollectionRepository
import com.mind.app.core.domain.repository.PreferencesRepository
import com.mind.app.core.domain.usecase.CaptureMemoryUseCase
import com.mind.app.core.domain.usecase.HomeDashboardData
import com.mind.app.core.domain.usecase.ManageMemoryLifecycleUseCase
import com.mind.app.core.domain.usecase.ObserveHomeDashboardUseCase
import com.mind.app.core.model.AnimationStyle
import com.mind.app.core.model.CaptureMethod
import com.mind.app.core.model.CategoryTreeNode
import com.mind.app.core.model.ContentType
import com.mind.app.core.model.LayoutMode
import com.mind.app.core.model.Memory
import com.mind.app.feature.common.MemorySelectionController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

enum class HomeTimelineFilter(val label: String) {
    ALL("All"),
    TODAY("Today"),
    THIS_WEEK("This Week"),
    VOICE("Voice"),
    ERRORS("Errors"),
    PRODUCTS("Products"),
}

data class ActiveSaveOverlay(
    val memoryId: String,
    val message: String,
    val thumbnailPath: String? = null,
)

private data class HomeInteractionState(
    val filter: HomeTimelineFilter,
    val selectedCategoryId: String?,
    val overlay: ActiveSaveOverlay?,
    val error: String?,
    val status: String?,
    val isSelectionMode: Boolean,
    val selectedIds: Set<String>,
)

data class HomeUiState(
    val isLoading: Boolean = true,
    val dashboard: HomeDashboardData = HomeDashboardData(
        recentMemories = emptyList(),
        favouriteCount = 0,
        totalMemoryCount = 0,
        failedAnalysisCount = 0,
        topCategories = emptyList(),
        collections = emptyList(),
    ),
    val filteredMemories: List<Memory> = emptyList(),
    val selectedFilter: HomeTimelineFilter = HomeTimelineFilter.ALL,
    val selectedCategoryId: String? = null,
    val layoutMode: LayoutMode = LayoutMode.GRID,
    val animationStyle: AnimationStyle = AnimationStyle.RIPPLE,
    val animationsEnabled: Boolean = true,
    val isCloudAiConfigured: Boolean = false,
    val activeSaveOverlay: ActiveSaveOverlay? = null,
    val errorMessage: String? = null,
    val statusMessage: String? = null,
    val isSelectionMode: Boolean = false,
    val selectedMemoryIds: Set<String> = emptySet(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    observeHomeDashboardUseCase: ObserveHomeDashboardUseCase,
    private val preferencesRepository: PreferencesRepository,
    private val manageMemoryLifecycleUseCase: ManageMemoryLifecycleUseCase,
    private val captureMemoryUseCase: CaptureMemoryUseCase,
    private val collectionRepository: CollectionRepository,
    private val aiEngineRepository: AiEngineRepository,
    private val mediaImportBridge: MediaImportBridge,
) : ViewModel() {

    private val selectedFilterFlow = MutableStateFlow(HomeTimelineFilter.ALL)
    private val selectedCategoryIdFlow = MutableStateFlow<String?>(null)
    private val activeSaveOverlayFlow = MutableStateFlow<ActiveSaveOverlay?>(null)
    private val errorMessageFlow = MutableStateFlow<String?>(null)
    private val statusMessageFlow = MutableStateFlow<String?>(null)
    private val selectionController = MemorySelectionController()

    private val filterPairFlow = combine(selectedFilterFlow, selectedCategoryIdFlow) { filter, catId ->
        filter to catId
    }

    private val interactionFlow = combine(
        filterPairFlow,
        activeSaveOverlayFlow,
        errorMessageFlow,
        statusMessageFlow,
        selectionController.state,
    ) { (filter, categoryId), overlay, error, status, selection ->
        HomeInteractionState(
            filter = filter,
            selectedCategoryId = categoryId,
            overlay = overlay,
            error = error,
            status = status,
            isSelectionMode = selection.isSelectionMode,
            selectedIds = selection.selectedIds,
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(
        observeHomeDashboardUseCase(),
        preferencesRepository.observePreferences(),
        interactionFlow,
    ) { dashboard, prefs, interaction ->
        val filtered = filterMemories(
            memories = dashboard.recentMemories,
            filter = interaction.filter,
            selectedCategoryId = interaction.selectedCategoryId,
            categories = dashboard.topCategories,
        )
        val visibleIds = filtered.mapTo(HashSet(filtered.size)) { it.id }
        val activeSelectedIds = interaction.selectedIds.intersect(visibleIds)
        val cloudReady = prefs.cloudAiEnabled &&
            !prefs.localOnlyMode &&
            (prefs.hasGeminiApiKey || prefs.customBackendUrl.isNotBlank())

        HomeUiState(
            isLoading = false,
            dashboard = dashboard,
            filteredMemories = filtered,
            selectedFilter = interaction.filter,
            selectedCategoryId = interaction.selectedCategoryId,
            layoutMode = prefs.layoutMode,
            animationStyle = prefs.animationStyle,
            animationsEnabled = prefs.animationsEnabled,
            isCloudAiConfigured = cloudReady,
            activeSaveOverlay = interaction.overlay,
            errorMessage = interaction.error,
            statusMessage = interaction.status,
            isSelectionMode = interaction.isSelectionMode,
            selectedMemoryIds = activeSelectedIds,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
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
        selectionController.toggleSelectAll(uiState.value.filteredMemories.map { it.id })
    }

    fun clearSelection() {
        selectionController.clear()
    }

    fun deleteSelectedMemories() {
        val visibleIds = uiState.value.filteredMemories.mapTo(HashSet()) { it.id }
        val toDelete = selectionController.clear().intersect(visibleIds)
        if (toDelete.isEmpty()) return
        viewModelScope.launch {
            manageMemoryLifecycleUseCase.permanentlyDeleteBatch(toDelete)
        }
    }

    fun addSelectedMemoriesToCollection(collectionId: String, collectionName: String) {
        val visibleIds = uiState.value.filteredMemories.mapTo(HashSet()) { it.id }
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
        val visibleIds = uiState.value.filteredMemories.mapTo(HashSet()) { it.id }
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

    fun retryPendingAnalyses() {
        viewModelScope.launch {
            val count = aiEngineRepository.retryFailedAnalyses()
            statusMessageFlow.value = if (count > 0) {
                "Retrying AI analysis for $count ${if (count == 1) "capture" else "captures"}"
            } else {
                "No pending captures to retry"
            }
        }
    }

    fun selectFilter(filter: HomeTimelineFilter) {
        selectedCategoryIdFlow.value = null
        selectedFilterFlow.value = filter
    }

    fun selectCategoryFilter(categoryId: String?) {
        if (categoryId == null || selectedCategoryIdFlow.value == categoryId) {
            selectedCategoryIdFlow.value = null
        } else {
            selectedCategoryIdFlow.value = categoryId
            selectedFilterFlow.value = HomeTimelineFilter.ALL
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

    fun allocateVoiceOutputPath(): String = mediaImportBridge.allocateVoiceOutputPath()

    fun saveVoiceMemory(filePath: String, durationSeconds: Int) {
        viewModelScope.launch {
            val saved = captureMemoryUseCase.saveVoiceMemory(
                audioFilePath = filePath,
                durationSeconds = durationSeconds,
                captureMethod = CaptureMethod.IN_APP,
            )
            activeSaveOverlayFlow.value = ActiveSaveOverlay(saved.id, "Voice memory saved")
        }
    }

    fun saveQuickNote(title: String, body: String, isQuote: Boolean) {
        val cleanBody = body.trim()
        if (cleanBody.isEmpty()) return
        viewModelScope.launch {
            val saved = captureMemoryUseCase.saveTextOrUrlMemory(
                text = cleanBody,
                title = title.trim().ifBlank { null },
                isQuote = isQuote,
                captureMethod = CaptureMethod.IN_APP,
            )
            activeSaveOverlayFlow.value = ActiveSaveOverlay(
                saved.id,
                if (isQuote) "Quote saved to Mind" else "Note saved to Mind",
            )
        }
    }

    fun saveLinkMemory(rawUrl: String, title: String, note: String) {
        val trimmedUrl = rawUrl.trim()
        if (trimmedUrl.isEmpty()) return
        val normalizedUrl = if (trimmedUrl.startsWith("http://", ignoreCase = true) ||
            trimmedUrl.startsWith("https://", ignoreCase = true)
        ) {
            trimmedUrl
        } else {
            "https://$trimmedUrl"
        }
        val combinedText = note.trim().ifBlank { normalizedUrl }

        viewModelScope.launch {
            val saved = captureMemoryUseCase.saveTextOrUrlMemory(
                text = combinedText,
                url = normalizedUrl,
                title = title.trim().ifBlank { normalizedUrl },
                isQuote = false,
                captureMethod = CaptureMethod.IN_APP,
            )
            activeSaveOverlayFlow.value = ActiveSaveOverlay(saved.id, "Link saved to Mind")
        }
    }

    fun importMediaUri(uriString: String) {
        viewModelScope.launch {
            val imported = mediaImportBridge.importUri(uriString)
            if (imported == null) {
                errorMessageFlow.value = "Could not import the selected media file."
                return@launch
            }
            val (saved, thumbPath) = if (imported.contentType == ContentType.VOICE_NOTE) {
                captureMemoryUseCase.saveVoiceMemory(
                    audioFilePath = imported.originalPath,
                    durationSeconds = 1,
                    captureMethod = CaptureMethod.IN_APP,
                ) to null
            } else {
                captureMemoryUseCase.saveImageMemory(
                    originalFilePath = imported.originalPath,
                    thumbnailPath = imported.thumbnailPath,
                    contentType = imported.contentType,
                    captureMethod = CaptureMethod.IN_APP,
                ) to imported.thumbnailPath
            }
            activeSaveOverlayFlow.value = ActiveSaveOverlay(
                memoryId = saved.id,
                message = "Saved to Mind",
                thumbnailPath = thumbPath,
            )
        }
    }

    fun undoLastCapture() {
        val active = activeSaveOverlayFlow.value ?: return
        activeSaveOverlayFlow.value = null
        viewModelScope.launch {
            manageMemoryLifecycleUseCase.permanentlyDelete(active.memoryId)
        }
    }

    fun dismissSaveOverlay() {
        activeSaveOverlayFlow.value = null
    }

    fun clearError() {
        errorMessageFlow.value = null
    }

    fun clearStatusMessage() {
        statusMessageFlow.value = null
    }

    private fun filterMemories(
        memories: List<Memory>,
        filter: HomeTimelineFilter,
        selectedCategoryId: String?,
        categories: List<CategoryTreeNode>,
    ): List<Memory> {
        if (selectedCategoryId != null) {
            val parentNode = categories.firstOrNull { it.category.id == selectedCategoryId }
            val matchingIds = buildSet {
                add(selectedCategoryId)
                parentNode?.subcategories?.forEach { add(it.category.id) }
            }
            return memories.filter { memory ->
                memory.primaryCategory?.id in matchingIds || memory.subcategory?.id in matchingIds
            }
        }

        val now = System.currentTimeMillis()
        val startOfToday = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val startOfWeek = now - 7L * 24L * 60L * 60L * 1000L

        return when (filter) {
            HomeTimelineFilter.ALL -> memories
            HomeTimelineFilter.TODAY -> memories.filter { it.capturedAtEpochMillis >= startOfToday }
            HomeTimelineFilter.THIS_WEEK -> memories.filter { it.capturedAtEpochMillis >= startOfWeek }
            HomeTimelineFilter.VOICE -> memories.filter { it.contentType == ContentType.VOICE_NOTE }
            HomeTimelineFilter.ERRORS -> memories.filter { it.contentType == ContentType.ERROR_LOG }
            HomeTimelineFilter.PRODUCTS -> memories.filter { it.contentType == ContentType.PRODUCT }
        }
    }
}
