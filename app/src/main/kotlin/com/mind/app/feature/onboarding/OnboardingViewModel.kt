package com.mind.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mind.app.core.domain.repository.PreferencesRepository
import com.mind.app.core.model.AnimationStyle
import com.mind.app.core.model.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = preferencesRepository
        .observePreferences()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserPreferences(),
        )

    fun completeOnboarding(
        useCloudAi: Boolean,
        apiKey: String,
        modelId: String,
        animationStyle: AnimationStyle,
        onDone: () -> Unit = {},
    ) {
        viewModelScope.launch {
            preferencesRepository.setCloudAiEnabled(useCloudAi)
            preferencesRepository.setLocalOnlyMode(!useCloudAi)
            preferencesRepository.setAnimationStyle(animationStyle)
            if (apiKey.isNotBlank()) {
                preferencesRepository.saveGeminiApiKey(apiKey.trim())
            }
            if (modelId.isNotBlank()) {
                preferencesRepository.setGeminiModel(modelId)
            }
            preferencesRepository.setOnboardingCompleted(true)
            onDone()
        }
    }

    fun skipOnboarding() {
        viewModelScope.launch {
            preferencesRepository.setOnboardingCompleted(true)
        }
    }
}
