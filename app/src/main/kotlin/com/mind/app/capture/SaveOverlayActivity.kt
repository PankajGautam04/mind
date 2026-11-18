package com.mind.app.capture

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mind.app.MainActivity
import com.mind.app.core.designsystem.component.MindSaveAnimationOverlay
import com.mind.app.core.designsystem.theme.MindTheme
import com.mind.app.core.domain.repository.PreferencesRepository
import com.mind.app.core.model.UserPreferences
import dagger.hilt.android.AndroidEntryPoint
import java.util.function.Consumer
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Displays Mind's signature save animation and bottom confirmation shade briefly over the current
 * screen after an external capture.
 */
@AndroidEntryPoint
class SaveOverlayActivity : ComponentActivity() {

    @Inject lateinit var preferencesRepository: PreferencesRepository

    private var memoryIdState by mutableStateOf<String?>(null)
    private var messageState by mutableStateOf("Saved to Mind")
    private var thumbnailPathState by mutableStateOf<String?>(null)
    private var screenshotPathState by mutableStateOf<String?>(null)
    private var crossWindowBlurEnabledState by mutableStateOf(false)
    private var lastRequestedBlurFraction = 0f
    private var lastAppliedBlurPx = 0

    private val crossWindowBlurListener = Consumer<Boolean> { enabled ->
        crossWindowBlurEnabledState = enabled
        applyBackdropBlur(lastRequestedBlurFraction)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            setTranslucent(true)
        }
        window.setBackgroundDrawableResource(android.R.color.transparent)
        enableEdgeToEdge()
        suppressWindowTransition(isOpen = true)
        readExtras(intent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            crossWindowBlurEnabledState = windowManager.isCrossWindowBlurEnabled
        }

        setContent {
            val prefs by preferencesRepository.observePreferences()
                .collectAsStateWithLifecycle(initialValue = UserPreferences())

            MindTheme(
                themeMode = prefs.themeMode,
                accentPalette = prefs.accentPalette,
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    MindSaveAnimationOverlay(
                        visible = memoryIdState != null || messageState.isNotEmpty(),
                        style = prefs.animationStyle,
                        animationsEnabled = prefs.animationsEnabled,
                        title = messageState,
                        thumbnailPath = thumbnailPathState,
                        fullscreenImagePath = screenshotPathState ?: thumbnailPathState,
                        externalBlurActive = crossWindowBlurEnabledState,
                        onBackdropBlurFraction = ::applyBackdropBlur,
                        onViewMemory = memoryIdState?.let { targetId ->
                            {
                                val openIntent = Intent(this@SaveOverlayActivity, MainActivity::class.java).apply {
                                    action = MindCaptureWidgetProvider.ACTION_OPEN_MEMORY
                                    putExtra(MindCaptureWidgetProvider.EXTRA_MEMORY_ID, targetId)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                }
                                startActivity(openIntent)
                                finishSilently()
                            }
                        },
                        onFinished = { finishSilently() },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            crossWindowBlurEnabledState = windowManager.isCrossWindowBlurEnabled
            windowManager.addCrossWindowBlurEnabledListener(crossWindowBlurListener)
            applyBackdropBlur(lastRequestedBlurFraction)
        }
    }

    override fun onPause() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            windowManager.removeCrossWindowBlurEnabledListener(crossWindowBlurListener)
        }
        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readExtras(intent)
    }

    private fun readExtras(targetIntent: Intent?) {
        memoryIdState = targetIntent?.getStringExtra(EXTRA_MEMORY_ID)
        messageState = targetIntent?.getStringExtra(EXTRA_MESSAGE) ?: "Saved to Mind"
        thumbnailPathState = targetIntent?.getStringExtra(EXTRA_THUMBNAIL_PATH)
        screenshotPathState = targetIntent?.getStringExtra(EXTRA_SCREENSHOT_PATH)
    }

    private fun applyBackdropBlur(fraction: Float) {
        lastRequestedBlurFraction = fraction.coerceIn(0f, 1f)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

        val maxBlurPx = (28f * resources.displayMetrics.density).roundToInt().coerceIn(32, 120)
        val targetPx = if (crossWindowBlurEnabledState) {
            (lastRequestedBlurFraction * maxBlurPx).roundToInt()
        } else {
            0
        }
        if (targetPx == lastAppliedBlurPx) return
        lastAppliedBlurPx = targetPx

        val params = window.attributes
        if (targetPx > 0) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            params.blurBehindRadius = targetPx
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            params.blurBehindRadius = 0
        }
        window.attributes = params
    }

    private fun finishSilently() {
        applyBackdropBlur(0f)
        finish()
        suppressWindowTransition(isOpen = false)
    }

    @Suppress("DEPRECATION")
    private fun suppressWindowTransition(isOpen: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val transitionType = if (isOpen) OVERRIDE_TRANSITION_OPEN else OVERRIDE_TRANSITION_CLOSE
            overrideActivityTransition(transitionType, 0, 0)
        } else {
            overridePendingTransition(0, 0)
        }
    }

    companion object {
        const val EXTRA_MEMORY_ID = "extra_memory_id"
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_THUMBNAIL_PATH = "extra_thumbnail_path"
        const val EXTRA_SCREENSHOT_PATH = "extra_screenshot_path"
    }
}
