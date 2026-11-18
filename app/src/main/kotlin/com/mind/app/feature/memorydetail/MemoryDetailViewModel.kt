package com.mind.app.feature.memorydetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.mind.app.core.domain.repository.AiEngineRepository
import com.mind.app.core.domain.repository.CategoryRepository
import com.mind.app.core.domain.repository.CollectionRepository
import com.mind.app.core.domain.repository.MemoryRepository
import com.mind.app.core.domain.repository.TagRepository
import com.mind.app.core.domain.usecase.ManageMemoryLifecycleUseCase
import com.mind.app.core.model.CategoryTreeNode
import com.mind.app.core.model.ContentType
import com.mind.app.core.model.MemoryDetail
import com.mind.app.core.model.MetadataSource
import com.mind.app.core.model.SmartCollection
import com.mind.app.navigation.MindDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class FullscreenImagePage(
    val memoryId: String,
    val title: String,
    val imagePath: String,
)

private data class DetailInteractionState(
    val isAnalyzing: Boolean = false,
    val statusMessage: String? = null,
    val askQuestion: String = "",
    val isAskingMemory: Boolean = false,
    val askMemoryAnswer: String? = null,
    val askMemoryError: String? = null,
    val imagePages: List<FullscreenImagePage> = emptyList(),
)

data class MemoryDetailUiState(
    val isLoading: Boolean = true,
    val detail: MemoryDetail? = null,
    val categories: List<CategoryTreeNode> = emptyList(),
    val allCollections: List<SmartCollection> = emptyList(),
    val fullscreenImagePages: List<FullscreenImagePage> = emptyList(),
    val isAnalyzing: Boolean = false,
    val statusMessage: String? = null,
    val askQuestion: String = "",
    val isAskingMemory: Boolean = false,
    val askMemoryAnswer: String? = null,
    val askMemoryError: String? = null,
)

@HiltViewModel
class MemoryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val memoryRepository: MemoryRepository,
    categoryRepository: CategoryRepository,
    private val collectionRepository: CollectionRepository,
    private val tagRepository: TagRepository,
    private val aiEngineRepository: AiEngineRepository,
    private val manageMemoryLifecycleUseCase: ManageMemoryLifecycleUseCase,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<MindDestination.MemoryDetail>()
    private val interactionFlow = MutableStateFlow(DetailInteractionState())

    init {
        viewModelScope.launch {
            val pages = withContext(Dispatchers.IO) {
                memoryRepository.getRecentMemoriesSnapshot(150).mapNotNull { memory ->
                    val path = memory.originalFilePath
                    if (memory.contentType != ContentType.VOICE_NOTE &&
                        !path.isNullOrBlank() &&
                        File(path).exists()
                    ) {
                        FullscreenImagePage(
                            memoryId = memory.id,
                            title = memory.displayTitle,
                            imagePath = path,
                        )
                    } else {
                        null
                    }
                }
            }
            interactionFlow.value = interactionFlow.value.copy(imagePages = pages)
        }
    }

    val uiState: StateFlow<MemoryDetailUiState> = combine(
        memoryRepository.observeMemoryDetail(route.memoryId),
        categoryRepository.observeCategoryTree(),
        collectionRepository.observeCollections(),
        interactionFlow,
    ) { detail, categories, collections, interaction ->
        val currentImage = detail?.memory?.let { mem ->
            val path = mem.originalFilePath
            if (mem.contentType != ContentType.VOICE_NOTE && !path.isNullOrBlank()) {
                FullscreenImagePage(mem.id, mem.displayTitle, path)
            } else {
                null
            }
        }
        val mergedPages = if (currentImage != null && interaction.imagePages.none { it.memoryId == currentImage.memoryId }) {
            listOf(currentImage) + interaction.imagePages
        } else {
            interaction.imagePages
        }

        MemoryDetailUiState(
            isLoading = false,
            detail = detail,
            categories = categories,
            allCollections = collections,
            fullscreenImagePages = mergedPages,
            isAnalyzing = interaction.isAnalyzing,
            statusMessage = interaction.statusMessage,
            askQuestion = interaction.askQuestion,
            isAskingMemory = interaction.isAskingMemory,
            askMemoryAnswer = interaction.askMemoryAnswer,
            askMemoryError = interaction.askMemoryError,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MemoryDetailUiState(),
    )

    fun toggleFavourite() {
        val memory = uiState.value.detail?.memory ?: return
        viewModelScope.launch {
            manageMemoryLifecycleUseCase.toggleFavourite(memory.id, memory.isFavourite)
        }
    }

    fun analyzeWithGemini() {
        if (interactionFlow.value.isAnalyzing) return
        viewModelScope.launch {
            interactionFlow.value = interactionFlow.value.copy(isAnalyzing = true)
            val result = aiEngineRepository.analyzeNow(route.memoryId)
            interactionFlow.value = interactionFlow.value.copy(
                isAnalyzing = false,
                statusMessage = result.fold(
                    onSuccess = { "Gemini analysis updated" },
                    onFailure = { err -> err.message ?: "Gemini analysis failed" },
                ),
            )
        }
    }

    fun updateAskQuestion(question: String) {
        interactionFlow.value = interactionFlow.value.copy(
            askQuestion = question,
            askMemoryError = null,
        )
    }

    fun submitMemoryQuestion() {
        val question = interactionFlow.value.askQuestion.trim()
        if (question.isEmpty() || interactionFlow.value.isAskingMemory) return
        viewModelScope.launch {
            interactionFlow.value = interactionFlow.value.copy(
                isAskingMemory = true,
                askMemoryError = null,
            )
            val result = aiEngineRepository.askAboutMemory(route.memoryId, question)
            interactionFlow.value = result.fold(
                onSuccess = { answer ->
                    interactionFlow.value.copy(
                        isAskingMemory = false,
                        askMemoryAnswer = answer,
                        askMemoryError = null,
                    )
                },
                onFailure = { err ->
                    interactionFlow.value.copy(
                        isAskingMemory = false,
                        askMemoryError = err.message ?: "Unable to answer right now.",
                    )
                },
            )
        }
    }

    fun clearMemoryAnswer() {
        interactionFlow.value = interactionFlow.value.copy(
            askQuestion = "",
            askMemoryAnswer = null,
            askMemoryError = null,
        )
    }

    fun toggleCollectionMembership(collection: SmartCollection, currentlyAssigned: Boolean) {
        val memoryId = uiState.value.detail?.memory?.id ?: return
        viewModelScope.launch {
            if (currentlyAssigned) {
                collectionRepository.removeMemoryFromCollection(collection.id, memoryId)
                interactionFlow.value = interactionFlow.value.copy(
                    statusMessage = "Removed from ${collection.name}",
                )
            } else {
                collectionRepository.addMemoryToCollection(collection.id, memoryId)
                interactionFlow.value = interactionFlow.value.copy(
                    statusMessage = "Added to ${collection.name}",
                )
            }
        }
    }

    fun createSpaceAndAssign(name: String) {
        val trimmed = name.trim()
        val memoryId = uiState.value.detail?.memory?.id ?: return
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val created = collectionRepository.createCollection(trimmed)
            collectionRepository.addMemoryToCollection(created.id, memoryId)
            interactionFlow.value = interactionFlow.value.copy(
                statusMessage = "Added to $trimmed",
            )
        }
    }

    fun updateMetadata(
        title: String,
        notes: String,
        categoryId: String?,
        subcategoryId: String?,
        tagsRaw: String,
    ) {
        val memoryId = uiState.value.detail?.memory?.id ?: return
        val parsedTags = tagsRaw.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }

        viewModelScope.launch {
            memoryRepository.updateUserMetadata(
                memoryId = memoryId,
                title = title.trim().ifBlank { null },
                notes = notes.trim().ifBlank { null },
                categoryId = categoryId,
                subcategoryId = subcategoryId,
            )
            tagRepository.setTagsForMemory(
                memoryId = memoryId,
                tagNames = parsedTags,
                source = MetadataSource.USER,
            )
            interactionFlow.value = interactionFlow.value.copy(statusMessage = "Memory updated")
        }
    }

    fun clearStatusMessage() {
        interactionFlow.value = interactionFlow.value.copy(statusMessage = null)
    }

    fun deleteMemory(onDeleted: () -> Unit) {
        val memoryId = uiState.value.detail?.memory?.id ?: return
        viewModelScope.launch {
            manageMemoryLifecycleUseCase.permanentlyDelete(memoryId)
            onDeleted()
        }
    }
}
