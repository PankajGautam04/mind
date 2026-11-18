package com.mind.app.feature.collections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mind.app.core.domain.repository.CategoryRepository
import com.mind.app.core.domain.repository.CollectionRepository
import com.mind.app.core.domain.usecase.ManageCategoryUseCase
import com.mind.app.core.model.CategoryTreeNode
import com.mind.app.core.model.SmartCollection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CollectionsUiState(
    val collections: List<SmartCollection> = emptyList(),
    val categoryTree: List<CategoryTreeNode> = emptyList(),
    val expandedCategoryIds: Set<String> = emptySet(),
    val feedbackMessage: String? = null,
)

@HiltViewModel
class CollectionsViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val collectionRepository: CollectionRepository,
    private val manageCategoryUseCase: ManageCategoryUseCase,
) : ViewModel() {

    private val expandedIds = MutableStateFlow<Set<String>>(emptySet())
    private val feedback = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CollectionsUiState> = combine(
        collectionRepository.observeCollections(),
        categoryRepository.observeCategoryTree(),
        expandedIds,
        feedback,
    ) { collections, tree, expanded, message ->
        CollectionsUiState(
            collections = collections,
            categoryTree = tree,
            expandedCategoryIds = expanded,
            feedbackMessage = message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CollectionsUiState(),
    )

    fun toggleCategoryExpanded(categoryId: String) {
        expandedIds.value = expandedIds.value.let { current ->
            if (categoryId in current) current - categoryId else current + categoryId
        }
    }

    fun createCategory(name: String, parentId: String? = null) {
        viewModelScope.launch {
            manageCategoryUseCase.create(name, parentId)
                .onSuccess {
                    if (parentId != null) {
                        expandedIds.value = expandedIds.value + parentId
                    }
                    feedback.value = "Created \"${it.name}\""
                }
                .onFailure { feedback.value = it.message }
        }
    }

    fun renameCategory(categoryId: String, newName: String) {
        viewModelScope.launch {
            manageCategoryUseCase.rename(categoryId, newName)
                .onSuccess { feedback.value = "Renamed category to \"${newName.trim()}\"" }
                .onFailure { feedback.value = it.message }
        }
    }

    fun deleteCategory(categoryId: String, reassignToCategoryId: String?) {
        viewModelScope.launch {
            manageCategoryUseCase.delete(
                categoryId = categoryId,
                reassignToCategoryId = reassignToCategoryId ?: ManageCategoryUseCase.UNCATEGORIZED_ID,
            )
                .onSuccess { feedback.value = "Category deleted and memories reassigned" }
                .onFailure { feedback.value = it.message }
        }
    }

    fun mergeCategory(sourceCategoryId: String, targetCategoryId: String) {
        if (sourceCategoryId == targetCategoryId) return
        viewModelScope.launch {
            runCatching {
                categoryRepository.mergeCategories(sourceCategoryId, targetCategoryId)
            }.onSuccess {
                feedback.value = "Categories merged"
            }.onFailure {
                feedback.value = it.message ?: "Failed to merge categories"
            }
        }
    }

    fun createCollection(name: String, description: String?) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            collectionRepository.createCollection(name = trimmed, description = description)
            feedback.value = "Created collection \"$trimmed\""
        }
    }

    fun acceptSuggestedCollection(collectionId: String) {
        viewModelScope.launch {
            collectionRepository.acceptSuggestedCollection(collectionId)
            feedback.value = "Collection saved"
        }
    }

    fun clearFeedback() {
        feedback.value = null
    }
}
