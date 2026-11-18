package com.mind.app

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
class MainViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {
    val loadedPreferences: StateFlow<UserPreferences?> = preferencesRepository
        .observePreferences()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    val preferences: StateFlow<UserPreferences> = preferencesRepository
        .observePreferences()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserPreferences(),
        )

    fun setAnimationStyle(style: AnimationStyle) {
        viewModelScope.launch {
            preferencesRepository.setAnimationStyle(style)
        }
    }
}
