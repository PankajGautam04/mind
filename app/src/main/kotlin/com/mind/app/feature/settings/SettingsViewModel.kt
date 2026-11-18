package com.mind.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mind.app.core.domain.repository.AiEngineRepository
import com.mind.app.core.domain.repository.MemoryRepository
import com.mind.app.core.domain.repository.PreferencesRepository
import com.mind.app.core.domain.repository.SourceAppRepository
import com.mind.app.core.model.AccentPalette
import com.mind.app.core.model.AiConnectionTestResult
import com.mind.app.core.model.AnimationStyle
import com.mind.app.core.model.AssistantAction
import com.mind.app.core.model.GeminiModelInfo
import com.mind.app.core.model.LayoutMode
import com.mind.app.core.model.SourceApplication
import com.mind.app.core.model.StorageStats
import com.mind.app.core.model.ThemeMode
import com.mind.app.core.model.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private val DEFAULT_MODEL_OPTIONS = listOf(
    GeminiModelInfo(
        id = "gemini-3.8-flash",
        displayName = "Gemini 3.8 Flash",
        description = "Recommended default multimodal workhorse (1M context)",
    ),
    GeminiModelInfo(
        id = "gemini-3.5-flash-lite",
        displayName = "Gemini 3.5 Flash-Lite",
        description = "Ultra-fast, cost-optimized multimodal Flash-Lite",
    ),
    GeminiModelInfo(
        id = "gemini-4-argon",
        displayName = "Gemini 4 Argon",
        description = "Efficient frontier reasoning & Ask Mind depth (1M context)",
    ),
    GeminiModelInfo(
        id = "gemini-4-hydrogen",
        displayName = "Gemini 4 Hydrogen",
        description = "High-efficiency Gemini 4 multimodal model",
    ),
    GeminiModelInfo(
        id = "gemini-4-carbon",
        displayName = "Gemini 4 Carbon",
        description = "Flagship deepest reasoning across complex captures",
    ),
)

private data class SettingsAiUiState(
    val storageStats: StorageStats = StorageStats(),
    val availableModels: List<GeminiModelInfo> = DEFAULT_MODEL_OPTIONS,
    val isFetchingModels: Boolean = false,
    val isTestingConnection: Boolean = false,
    val connectionTestResult: AiConnectionTestResult? = null,
    val isRetryingAnalyses: Boolean = false,
    val statusMessage: String? = null,
)

data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val totalMemoryCount: Int = 0,
    val sourceApps: List<SourceApplication> = emptyList(),
    val storageStats: StorageStats = StorageStats(),
    val availableModels: List<GeminiModelInfo> = DEFAULT_MODEL_OPTIONS,
    val isFetchingModels: Boolean = false,
    val isTestingConnection: Boolean = false,
    val connectionTestResult: AiConnectionTestResult? = null,
    val isRetryingAnalyses: Boolean = false,
    val statusMessage: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val memoryRepository: MemoryRepository,
    private val aiEngineRepository: AiEngineRepository,
    private val sourceAppRepository: SourceAppRepository,
) : ViewModel() {

    private val aiStateFlow = MutableStateFlow(SettingsAiUiState())

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesRepository.observePreferences(),
        memoryRepository.observeTotalMemoryCount(),
        sourceAppRepository.observeSourceApps(),
        aiStateFlow,
    ) { prefs, count, sourceApps, aiState ->
        SettingsUiState(
            preferences = prefs,
            totalMemoryCount = count,
            sourceApps = sourceApps,
            storageStats = aiState.storageStats,
            availableModels = aiState.availableModels,
            isFetchingModels = aiState.isFetchingModels,
            isTestingConnection = aiState.isTestingConnection,
            connectionTestResult = aiState.connectionTestResult,
            isRetryingAnalyses = aiState.isRetryingAnalyses,
            statusMessage = aiState.statusMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    init {
        refreshStorageStats()
    }

    fun refreshStorageStats() {
        viewModelScope.launch {
            val stats = memoryRepository.getStorageStats()
            aiStateFlow.value = aiStateFlow.value.copy(storageStats = stats)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
    }

    fun setAccentPalette(palette: AccentPalette) {
        viewModelScope.launch { preferencesRepository.setAccentPalette(palette) }
    }

    fun setLayoutMode(mode: LayoutMode) {
        viewModelScope.launch { preferencesRepository.setLayoutMode(mode) }
    }

    fun setAnimationStyle(style: AnimationStyle) {
        viewModelScope.launch { preferencesRepository.setAnimationStyle(style) }
    }

    fun setAnimationsEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setAnimationsEnabled(enabled) }
    }

    fun setFloatingCaptureEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setFloatingCaptureEnabled(enabled) }
    }

    fun setCaptureConfirmationEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setCaptureConfirmationEnabled(enabled) }
    }

    fun setAssistantAction(action: AssistantAction) {
        viewModelScope.launch {
            preferencesRepository.setAssistantAction(action)
            aiStateFlow.value = aiStateFlow.value.copy(
                statusMessage = "Power button action set to ${action.displayName}",
            )
        }
    }

    fun setCloudAiEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setCloudAiEnabled(enabled)
            if (enabled) {
                preferencesRepository.setLocalOnlyMode(false)
            }
        }
    }

    fun setLocalOnlyMode(localOnly: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setLocalOnlyMode(localOnly)
            if (localOnly) {
                preferencesRepository.setCloudAiEnabled(false)
            }
        }
    }

    fun setAutoCategorizationEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setAutoCategorizationEnabled(enabled) }
    }

    fun saveGeminiApiKey(apiKey: String) {
        val trimmed = apiKey.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            preferencesRepository.saveGeminiApiKey(trimmed)
            preferencesRepository.setLocalOnlyMode(false)
            preferencesRepository.setCloudAiEnabled(true)
            aiStateFlow.value = aiStateFlow.value.copy(
                connectionTestResult = null,
                statusMessage = "Gemini API key saved (AES-256-GCM) and Cloud AI enabled",
            )
        }
    }

    fun clearGeminiApiKey() {
        viewModelScope.launch {
            preferencesRepository.clearGeminiApiKey()
            aiStateFlow.value = aiStateFlow.value.copy(
                connectionTestResult = null,
                statusMessage = "Stored Gemini API key removed",
            )
        }
    }

    fun setGeminiModel(modelId: String) {
        val clean = modelId.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            preferencesRepository.setGeminiModel(clean)
            aiStateFlow.value = aiStateFlow.value.copy(statusMessage = "Model set to $clean")
        }
    }

    fun setCustomBackendUrl(url: String) {
        viewModelScope.launch {
            preferencesRepository.setCustomBackendUrl(url.trim())
            aiStateFlow.value = aiStateFlow.value.copy(
                connectionTestResult = null,
                statusMessage = if (url.isBlank()) "Custom backend proxy cleared" else "Custom proxy URL saved",
            )
        }
    }

    fun fetchAvailableModels() {
        if (aiStateFlow.value.isFetchingModels) return
        viewModelScope.launch {
            aiStateFlow.value = aiStateFlow.value.copy(isFetchingModels = true)
            val result = aiEngineRepository.fetchAvailableModels()
            result.fold(
                onSuccess = { models ->
                    val merged = (models + DEFAULT_MODEL_OPTIONS).distinctBy { it.id }
                    aiStateFlow.value = aiStateFlow.value.copy(
                        isFetchingModels = false,
                        availableModels = merged,
                        statusMessage = "Loaded ${models.size} Gemini models from API",
                    )
                },
                onFailure = { err ->
                    aiStateFlow.value = aiStateFlow.value.copy(
                        isFetchingModels = false,
                        statusMessage = err.message ?: "Failed to fetch models",
                    )
                },
            )
        }
    }

    fun testGeminiConnection() {
        if (aiStateFlow.value.isTestingConnection) return
        viewModelScope.launch {
            aiStateFlow.value = aiStateFlow.value.copy(isTestingConnection = true, connectionTestResult = null)
            val result = aiEngineRepository.testConnection()
            aiStateFlow.value = aiStateFlow.value.copy(
                isTestingConnection = false,
                connectionTestResult = result,
            )
        }
    }

    fun retryFailedAnalyses() {
        if (aiStateFlow.value.isRetryingAnalyses) return
        viewModelScope.launch {
            aiStateFlow.value = aiStateFlow.value.copy(isRetryingAnalyses = true)
            val count = aiEngineRepository.retryFailedAnalyses()
            aiStateFlow.value = aiStateFlow.value.copy(
                isRetryingAnalyses = false,
                statusMessage = if (count > 0) {
                    "Queued $count memories for Gemini analysis"
                } else {
                    "No pending or failed memories to queue"
                },
            )
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            memoryRepository.clearCacheStorage()
            val stats = memoryRepository.getStorageStats()
            aiStateFlow.value = aiStateFlow.value.copy(
                storageStats = stats,
                statusMessage = "Cache cleared",
            )
        }
    }

    fun deleteAllMemories() {
        viewModelScope.launch {
            memoryRepository.deleteAllMemories()
            val stats = memoryRepository.getStorageStats()
            aiStateFlow.value = aiStateFlow.value.copy(
                storageStats = stats,
                statusMessage = "All memories deleted",
            )
        }
    }

    fun clearStatusMessage() {
        aiStateFlow.value = aiStateFlow.value.copy(statusMessage = null)
    }

    fun toggleAppExcluded(app: SourceApplication) {
        viewModelScope.launch {
            val nextExcluded = !app.isExcluded
            sourceAppRepository.setAppExcluded(
                packageName = app.packageName,
                displayName = app.displayName,
                excluded = nextExcluded,
            )
            aiStateFlow.value = aiStateFlow.value.copy(
                statusMessage = if (nextExcluded) {
                    "Skipped cloud AI for ${app.displayName}"
                } else {
                    "Re-enabled cloud AI for ${app.displayName}"
                },
            )
        }
    }

    fun addCustomExcludedApp(packageName: String, displayName: String) {
        val cleanPkg = packageName.trim()
        if (cleanPkg.isEmpty()) return
        val cleanName = displayName.trim().ifEmpty { cleanPkg }
        viewModelScope.launch {
            sourceAppRepository.setAppExcluded(
                packageName = cleanPkg,
                displayName = cleanName,
                excluded = true,
            )
            aiStateFlow.value = aiStateFlow.value.copy(
                statusMessage = "Excluded $cleanName from Gemini AI analysis",
            )
        }
    }

    fun replayOnboarding() {
        viewModelScope.launch {
            preferencesRepository.setOnboardingCompleted(false)
        }
    }
}
