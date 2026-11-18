package com.mind.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mind.app.capture.MindCaptureWidgetProvider
import com.mind.app.core.designsystem.theme.MindTheme
import com.mind.app.core.model.ThemeMode
import com.mind.app.feature.onboarding.OnboardingScreen
import com.mind.app.navigation.MindAppShell
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var launchAction by mutableStateOf<String?>(null)
    private var launchPrefillUrl by mutableStateOf<String?>(null)
    private var launchMemoryId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        consumeIntent(intent)

        setContent {
            val mainViewModel: MainViewModel = hiltViewModel()
            val loadedPreferences by mainViewModel.loadedPreferences.collectAsStateWithLifecycle()
            val preferences by mainViewModel.preferences.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val useDark = when (preferences.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.FOLLOW_SYSTEM -> systemDark
            }

            LaunchedEffect(useDark) {
                val style = if (useDark) {
                    SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT,
                    )
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }

            MindTheme(
                themeMode = preferences.themeMode,
                accentPalette = preferences.accentPalette,
            ) {
                when {
                    loadedPreferences == null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MindTheme.colors.background),
                        )
                    }
                    !loadedPreferences!!.onboardingCompleted -> {
                        OnboardingScreen()
                    }
                    else -> {
                        MindAppShell(
                            launchAction = launchAction,
                            launchPrefillUrl = launchPrefillUrl,
                            launchMemoryId = launchMemoryId,
                            onConsumeLaunchAction = {
                                launchAction = null
                                launchPrefillUrl = null
                                launchMemoryId = null
                            },
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeIntent(intent)
    }

    private fun consumeIntent(intent: Intent?) {
        launchAction = intent?.action
        launchPrefillUrl = intent?.getStringExtra(MindCaptureWidgetProvider.EXTRA_PREFILL_URL)
        launchMemoryId = intent?.getStringExtra(MindCaptureWidgetProvider.EXTRA_MEMORY_ID)
    }
}
