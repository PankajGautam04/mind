package com.mind.app.core.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mind.app.core.data.security.SecureApiKeyStore
import com.mind.app.core.model.AccentPalette
import com.mind.app.core.model.AnimationStyle
import com.mind.app.core.model.AssistantAction
import com.mind.app.core.model.LayoutMode
import com.mind.app.core.model.ThemeMode
import com.mind.app.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local Preferences DataStore wrapper with resilient IO recovery, encrypted API key status,
 * and privacy-first defaults.
 */
@Singleton
class UserPreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val secureApiKeyStore: SecureApiKeyStore,
) {
    private val rawPrefsFlow: Flow<Preferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    val preferences: Flow<UserPreferences> = combine(
        rawPrefsFlow,
        secureApiKeyStore.hasApiKeyFlow,
    ) { prefs, hasApiKey ->
        UserPreferences(
            themeMode = enumOrDefault(prefs[Keys.THEME_MODE], ThemeMode.FOLLOW_SYSTEM),
            accentPalette = enumOrDefault(prefs[Keys.ACCENT_PALETTE], AccentPalette.NEURAL_INDIGO),
            layoutMode = enumOrDefault(prefs[Keys.LAYOUT_MODE], LayoutMode.GRID),
            animationStyle = enumOrDefault(prefs[Keys.ANIMATION_STYLE], AnimationStyle.COMPANION_WAVE),
            animationsEnabled = prefs[Keys.ANIMATIONS_ENABLED] ?: true,
            floatingCaptureEnabled = prefs[Keys.FLOATING_CAPTURE_ENABLED] ?: false,
            captureConfirmationEnabled = prefs[Keys.CAPTURE_CONFIRMATION_ENABLED] ?: true,
            cloudAiEnabled = prefs[Keys.CLOUD_AI_ENABLED] ?: false,
            localOnlyMode = prefs[Keys.LOCAL_ONLY_MODE] ?: true,
            autoCategorizationEnabled = prefs[Keys.AUTO_CATEGORIZATION_ENABLED] ?: true,
            aiLanguageTag = prefs[Keys.AI_LANGUAGE_TAG] ?: "en",
            geminiModel = normalizeModelId(prefs[Keys.GEMINI_MODEL]),
            customBackendUrl = prefs[Keys.CUSTOM_BACKEND_URL] ?: "",
            hasGeminiApiKey = hasApiKey,
            assistantAction = enumOrDefault(prefs[Keys.ASSISTANT_ACTION], AssistantAction.ASK_EVERY_TIME),
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) = edit { it[Keys.THEME_MODE] = mode.name }
    suspend fun setAccentPalette(palette: AccentPalette) = edit { it[Keys.ACCENT_PALETTE] = palette.name }
    suspend fun setLayoutMode(mode: LayoutMode) = edit { it[Keys.LAYOUT_MODE] = mode.name }
    suspend fun setAnimationStyle(style: AnimationStyle) = edit { it[Keys.ANIMATION_STYLE] = style.name }
    suspend fun setAnimationsEnabled(enabled: Boolean) = edit { it[Keys.ANIMATIONS_ENABLED] = enabled }
    suspend fun setFloatingCaptureEnabled(enabled: Boolean) = edit { it[Keys.FLOATING_CAPTURE_ENABLED] = enabled }
    suspend fun setCaptureConfirmationEnabled(enabled: Boolean) = edit { it[Keys.CAPTURE_CONFIRMATION_ENABLED] = enabled }
    suspend fun setCloudAiEnabled(enabled: Boolean) = edit {
        it[Keys.CLOUD_AI_ENABLED] = enabled
        if (enabled) {
            it[Keys.LOCAL_ONLY_MODE] = false
        }
    }
    suspend fun setLocalOnlyMode(localOnly: Boolean) = edit {
        it[Keys.LOCAL_ONLY_MODE] = localOnly
        if (localOnly) {
            it[Keys.CLOUD_AI_ENABLED] = false
        }
    }
    suspend fun setAutoCategorizationEnabled(enabled: Boolean) = edit { it[Keys.AUTO_CATEGORIZATION_ENABLED] = enabled }
    suspend fun setAiLanguageTag(tag: String) = edit { it[Keys.AI_LANGUAGE_TAG] = tag }
    suspend fun setGeminiModel(modelId: String) = edit {
        it[Keys.GEMINI_MODEL] = normalizeModelId(modelId)
    }
    suspend fun setCustomBackendUrl(url: String) = edit { it[Keys.CUSTOM_BACKEND_URL] = url.trim() }
    suspend fun saveGeminiApiKey(apiKey: String) {
        secureApiKeyStore.saveApiKey(apiKey)
        if (apiKey.isNotBlank()) {
            setCloudAiEnabled(true)
        }
    }
    suspend fun clearGeminiApiKey() {
        secureApiKeyStore.clearApiKey()
    }
    suspend fun setAssistantAction(action: AssistantAction) = edit { it[Keys.ASSISTANT_ACTION] = action.name }
    suspend fun setOnboardingCompleted(completed: Boolean) = edit { it[Keys.ONBOARDING_COMPLETED] = completed }

    private suspend inline fun edit(crossinline block: (MutablePreferences) -> Unit) {
        dataStore.edit { prefs -> block(prefs) }
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(raw: String?, default: T): T {
        if (raw.isNullOrBlank()) return default
        return runCatching { enumValueOf<T>(raw) }.getOrDefault(default)
    }

    private fun normalizeModelId(raw: String?): String {
        val clean = raw?.trim().orEmpty().removePrefix("models/")
        if (clean.isEmpty()) return UserPreferences.DEFAULT_GEMINI_MODEL
        return when (clean) {
            "gemini-2.0-flash",
            "gemini-2.5-flash",
            "gemini-3.5-flash" -> "gemini-3.8-flash"
            "gemini-2.0-flash-lite",
            "gemini-2.5-flash-lite" -> "gemini-3.5-flash-lite"
            "gemini-2.5-pro",
            "gemini-3-pro-preview" -> "gemini-4-argon"
            else -> clean
        }
    }

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_PALETTE = stringPreferencesKey("accent_palette")
        val LAYOUT_MODE = stringPreferencesKey("layout_mode")
        val ANIMATION_STYLE = stringPreferencesKey("animation_style")
        val ANIMATIONS_ENABLED = booleanPreferencesKey("animations_enabled")
        val FLOATING_CAPTURE_ENABLED = booleanPreferencesKey("floating_capture_enabled")
        val CAPTURE_CONFIRMATION_ENABLED = booleanPreferencesKey("capture_confirmation_enabled")
        val CLOUD_AI_ENABLED = booleanPreferencesKey("cloud_ai_enabled")
        val LOCAL_ONLY_MODE = booleanPreferencesKey("local_only_mode")
        val AUTO_CATEGORIZATION_ENABLED = booleanPreferencesKey("auto_categorization_enabled")
        val AI_LANGUAGE_TAG = stringPreferencesKey("ai_language_tag")
        val GEMINI_MODEL = stringPreferencesKey("gemini_model")
        val CUSTOM_BACKEND_URL = stringPreferencesKey("custom_backend_url")
        val ASSISTANT_ACTION = stringPreferencesKey("assistant_action")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }
}
