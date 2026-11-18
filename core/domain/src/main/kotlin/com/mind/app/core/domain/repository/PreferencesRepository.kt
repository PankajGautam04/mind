package com.mind.app.core.domain.repository

import com.mind.app.core.model.AccentPalette
import com.mind.app.core.model.AnimationStyle
import com.mind.app.core.model.AssistantAction
import com.mind.app.core.model.LayoutMode
import com.mind.app.core.model.ThemeMode
import com.mind.app.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow

/**
 * Contract for persistent user appearance, capture, and privacy preferences.
 */
interface PreferencesRepository {
    fun observePreferences(): Flow<UserPreferences>

    suspend fun setThemeMode(themeMode: ThemeMode)
    suspend fun setAccentPalette(accentPalette: AccentPalette)
    suspend fun setLayoutMode(layoutMode: LayoutMode)
    suspend fun setAnimationStyle(animationStyle: AnimationStyle)
    suspend fun setAnimationsEnabled(enabled: Boolean)
    suspend fun setFloatingCaptureEnabled(enabled: Boolean)
    suspend fun setCaptureConfirmationEnabled(enabled: Boolean)
    suspend fun setCloudAiEnabled(enabled: Boolean)
    suspend fun setLocalOnlyMode(localOnly: Boolean)
    suspend fun setAutoCategorizationEnabled(enabled: Boolean)
    suspend fun setAiLanguageTag(languageTag: String)
    suspend fun setGeminiModel(modelId: String)
    suspend fun setCustomBackendUrl(url: String)
    suspend fun saveGeminiApiKey(apiKey: String)
    suspend fun clearGeminiApiKey()
    suspend fun setAssistantAction(action: AssistantAction)
    suspend fun setOnboardingCompleted(completed: Boolean)
}
