package com.mind.app.capture

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mind.app.MainActivity
import com.mind.app.R
import com.mind.app.core.designsystem.component.dshPressableScale
import com.mind.app.core.designsystem.component.mindGlassEdge
import com.mind.app.core.designsystem.theme.MindTheme
import com.mind.app.core.domain.repository.PreferencesRepository
import com.mind.app.core.model.CaptureMethod
import com.mind.app.core.model.UserPreferences
import com.mind.app.navigation.ACTION_PICK_MEDIA_CAPTURE
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Translucent entry point invoked by the Quick Settings Tile, App Shortcuts, Tasker / Automate,
 * Home Widget, optional Floating Button, or Samsung Side Key.
 *
 * Supports three modes:
 * 1. Direct Silent Screen Capture ([ACTION_CAPTURE_SCREEN])
 * 2. Translucent Floating "Capture Anything" Hub ([ACTION_CAPTURE_ANYTHING])
 * 3. Tasker / Automate / Launcher Shortcut Picker ([Intent.ACTION_CREATE_SHORTCUT])
 */
@AndroidEntryPoint
class CaptureTriggerActivity : ComponentActivity() {

    @Inject lateinit var preferencesRepository: PreferencesRepository

    private var currentMethod: CaptureMethod = CaptureMethod.QUICK_SETTINGS
    private var openedAccessibilitySettings by mutableStateOf(false)
    private var isServiceActiveState by mutableStateOf(false)
    private var isSettingEnabledState by mutableStateOf(false)

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val serviceIntent = Intent(this, ScreenCaptureForegroundService::class.java).apply {
                putExtra(ScreenCaptureForegroundService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(ScreenCaptureForegroundService.EXTRA_RESULT_DATA, result.data)
                putExtra(ScreenCaptureForegroundService.EXTRA_CAPTURE_METHOD, currentMethod.name)
            }
            ContextCompat.startForegroundService(this, serviceIntent)
        }
        finishSilently()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val action = intent?.action
        val isCreateShortcut = action == Intent.ACTION_CREATE_SHORTCUT
        val isCaptureAnythingHub = action == ACTION_CAPTURE_ANYTHING

        val methodName = intent.getStringExtra(EXTRA_CAPTURE_METHOD) ?: CaptureMethod.QUICK_SETTINGS.name
        currentMethod = runCatching { CaptureMethod.valueOf(methodName) }
            .getOrDefault(CaptureMethod.QUICK_SETTINGS)
        val forceMediaProjection = intent.getBooleanExtra(EXTRA_FORCE_MEDIA_PROJECTION, false)
        val showSetupOnly = intent.getBooleanExtra(EXTRA_SHOW_SETUP_ONLY, false)

        if (!isCreateShortcut && !isCaptureAnythingHub) {
            if (forceMediaProjection || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                launchMediaProjectionPrompt()
                return
            }

            if (!showSetupOnly && tryDispatchSilentCapture(currentMethod)) {
                finishSilently()
                return
            }
        }

        refreshAccessibilityStatus()
        enableEdgeToEdge()

        setContent {
            val prefs by preferencesRepository.observePreferences()
                .collectAsStateWithLifecycle(initialValue = UserPreferences())

            MindTheme(
                themeMode = prefs.themeMode,
                accentPalette = prefs.accentPalette,
            ) {
                when {
                    isCreateShortcut -> {
                        TaskerShortcutPickerSheet(
                            onSelectShortcut = { label, targetIntent ->
                                returnShortcutResult(label, targetIntent)
                            },
                            onDismiss = {
                                setResult(Activity.RESULT_CANCELED)
                                finishSilently()
                            },
                        )
                    }
                    isCaptureAnythingHub -> {
                        CaptureAnythingOverlaySheet(
                            onLaunchMainAction = { mainAction ->
                                startActivity(
                                    Intent(this@CaptureTriggerActivity, MainActivity::class.java).apply {
                                        this.action = mainAction
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                    },
                                )
                                finishSilently()
                            },
                            onDismiss = { finishSilently() },
                        )
                    }
                    else -> {
                        SilentCaptureSetupSheet(
                            showSetupOnly = showSetupOnly,
                            isServiceActive = isServiceActiveState,
                            isSettingEnabled = isSettingEnabledState,
                            onRetrySilentCapture = {
                                if (tryDispatchSilentCapture(currentMethod, delayOverrideMs = 260L)) {
                                    finishSilently()
                                }
                            },
                            onOpenAccessibility = {
                                openedAccessibilitySettings = true
                                startActivity(
                                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    },
                                )
                            },
                            onOpenRestrictedSettings = {
                                startActivity(
                                    Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", packageName, null),
                                    ).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    },
                                )
                            },
                            onScreenShareOnce = { launchMediaProjectionPrompt() },
                            onDismiss = { finishSilently() },
                        )
                    }
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun returnShortcutResult(label: String, shortcutIntent: Intent) {
        val iconRes = Intent.ShortcutIconResource.fromContext(this, R.mipmap.ic_launcher)
        val resultIntent = Intent().apply {
            putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
            putExtra(Intent.EXTRA_SHORTCUT_NAME, label)
            putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconRes)
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finishSilently()
    }

    override fun onResume() {
        super.onResume()
        refreshAccessibilityStatus()
        if (openedAccessibilitySettings && isServiceActiveState) {
            openedAccessibilitySettings = false
            if (tryDispatchSilentCapture(currentMethod, delayOverrideMs = 380L)) {
                finishSilently()
            }
        }
    }

    private fun refreshAccessibilityStatus() {
        isServiceActiveState = MindScreenshotAccessibilityService.isServiceActive()
        isSettingEnabledState = MindScreenshotAccessibilityService.isAccessibilitySettingEnabled(this)
    }

    private fun tryDispatchSilentCapture(
        method: CaptureMethod,
        delayOverrideMs: Long? = null,
    ): Boolean {
        if (!MindScreenshotAccessibilityService.isServiceActive()) return false
        val delayMs = delayOverrideMs ?: if (method == CaptureMethod.QUICK_SETTINGS) 580L else 260L
        val appContext = applicationContext
        return MindScreenshotAccessibilityService.captureSilent(
            captureMethod = method,
            delayMs = delayMs,
            onUnavailableOrFailed = {
                appContext.startActivity(
                    createIntent(
                        context = appContext,
                        method = method,
                        forceMediaProjection = false,
                        showSetupOnly = true,
                    ),
                )
            },
        )
    }

    private fun launchMediaProjectionPrompt() {
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val captureIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            projectionManager.createScreenCaptureIntent(
                MediaProjectionConfig.createConfigForDefaultDisplay(),
            )
        } else {
            projectionManager.createScreenCaptureIntent()
        }
        projectionLauncher.launch(captureIntent)
    }

    private fun finishSilently() {
        suppressWindowTransitions()
        finish()
    }

    @Suppress("DEPRECATION")
    private fun suppressWindowTransitions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            overridePendingTransition(0, 0)
        }
    }

    companion object {
        const val ACTION_CAPTURE_SCREEN = "com.mind.app.ACTION_CAPTURE_SCREEN"
        const val ACTION_CAPTURE_ANYTHING = "com.mind.app.ACTION_CAPTURE_ANYTHING"
        const val EXTRA_CAPTURE_METHOD = "extra_capture_method"
        const val EXTRA_FORCE_MEDIA_PROJECTION = "extra_force_media_projection"
        const val EXTRA_SHOW_SETUP_ONLY = "extra_show_setup_only"

        fun createIntent(
            context: Context,
            method: CaptureMethod,
            forceMediaProjection: Boolean = false,
            showSetupOnly: Boolean = false,
        ): Intent {
            return Intent(context, CaptureTriggerActivity::class.java).apply {
                action = ACTION_CAPTURE_SCREEN
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(EXTRA_CAPTURE_METHOD, method.name)
                putExtra(EXTRA_FORCE_MEDIA_PROJECTION, forceMediaProjection)
                putExtra(EXTRA_SHOW_SETUP_ONLY, showSetupOnly)
            }
        }

        fun createCaptureAnythingIntent(context: Context): Intent {
            return Intent(context, CaptureTriggerActivity::class.java).apply {
                action = ACTION_CAPTURE_ANYTHING
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        }
    }
}

@Composable
private fun CaptureAnythingOverlaySheet(
    onLaunchMainAction: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MindTheme.colors
    val sheetShape = RoundedCornerShape(30.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.44f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp)
                .navigationBarsPadding()
                .shadow(
                    elevation = 24.dp,
                    shape = sheetShape,
                    ambientColor = colors.shadowInk,
                    spotColor = colors.shadowInk,
                )
                .mindGlassEdge(
                    shape = sheetShape,
                    hairlineColor = colors.hairline,
                    rimColor = colors.rimHighlight,
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
            shape = sheetShape,
            color = colors.glassChromeFallback,
            contentColor = colors.textPrimary,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Save to Mind",
                            style = MindTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = colors.textPrimary,
                        )
                        Text(
                            text = "Quick overlay for Voice, Note, Link, Photo & Search",
                            style = MindTheme.typography.bodySmall,
                            color = colors.textSecondary,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(colors.glassPillFill)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OverlayActionTile(
                        icon = Icons.Rounded.Mic,
                        title = "Voice Note",
                        subtitle = "Record audio",
                        modifier = Modifier.weight(1f),
                        onClick = { onLaunchMainAction(MindCaptureWidgetProvider.ACTION_OPEN_VOICE_CAPTURE) },
                    )
                    OverlayActionTile(
                        icon = Icons.AutoMirrored.Rounded.Notes,
                        title = "Quick Note",
                        subtitle = "Thought or quote",
                        modifier = Modifier.weight(1f),
                        onClick = { onLaunchMainAction(MindCaptureWidgetProvider.ACTION_OPEN_NOTE_CAPTURE) },
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OverlayActionTile(
                        icon = Icons.Rounded.Link,
                        title = "Save Link",
                        subtitle = "Bookmark URL",
                        modifier = Modifier.weight(1f),
                        onClick = { onLaunchMainAction(MindCaptureWidgetProvider.ACTION_OPEN_LINK_CAPTURE) },
                    )
                    OverlayActionTile(
                        icon = Icons.Rounded.PhotoLibrary,
                        title = "Import Photo",
                        subtitle = "From gallery",
                        modifier = Modifier.weight(1f),
                        onClick = { onLaunchMainAction(ACTION_PICK_MEDIA_CAPTURE) },
                    )
                }

                OverlayActionTile(
                    icon = Icons.Rounded.Search,
                    title = "Search & Ask Mind AI",
                    subtitle = "Find any saved memory instantly",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onLaunchMainAction(MindCaptureWidgetProvider.ACTION_OPEN_SEARCH) },
                )
            }
        }
    }
}

@Composable
private fun TaskerShortcutPickerSheet(
    onSelectShortcut: (String, Intent) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val colors = MindTheme.colors
    val sheetShape = RoundedCornerShape(30.dp)

    val options = remember(context) {
        listOf(
            Triple(
                "Capture Anything Hub",
                "Opens floating quick-capture menu over any app",
                CaptureTriggerActivity.createCaptureAnythingIntent(context),
            ),
            Triple(
                "Capture Screen",
                "Save current screen to Mind",
                CaptureTriggerActivity.createIntent(context, CaptureMethod.QUICK_SETTINGS),
            ),
            Triple(
                "Record Voice Memory",
                "Starts microphone recording right away",
                Intent(context, MainActivity::class.java).apply {
                    action = MindCaptureWidgetProvider.ACTION_OPEN_VOICE_CAPTURE
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                },
            ),
            Triple(
                "Quick Note / Quote",
                "Opens instant note input sheet",
                Intent(context, MainActivity::class.java).apply {
                    action = MindCaptureWidgetProvider.ACTION_OPEN_NOTE_CAPTURE
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                },
            ),
            Triple(
                "Save Link / URL",
                "Opens link bookmark sheet",
                Intent(context, MainActivity::class.java).apply {
                    action = MindCaptureWidgetProvider.ACTION_OPEN_LINK_CAPTURE
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                },
            ),
            Triple(
                "Import Photo",
                "Opens media picker to import screenshot/photo",
                Intent(context, MainActivity::class.java).apply {
                    action = ACTION_PICK_MEDIA_CAPTURE
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                },
            ),
            Triple(
                "Search Mind AI",
                "Opens semantic & keyword memory search",
                Intent(context, MainActivity::class.java).apply {
                    action = MindCaptureWidgetProvider.ACTION_OPEN_SEARCH
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                },
            ),
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.48f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp)
                .navigationBarsPadding()
                .shadow(24.dp, sheetShape, ambientColor = colors.shadowInk, spotColor = colors.shadowInk)
                .mindGlassEdge(sheetShape, colors.hairline, colors.rimHighlight)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
            shape = sheetShape,
            color = colors.glassChromeFallback,
            contentColor = colors.textPrimary,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Select Mind Shortcut",
                            style = MindTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = colors.textPrimary,
                        )
                        Text(
                            text = "For Tasker, Automate, MacroDroid & Launcher",
                            style = MindTheme.typography.bodySmall,
                            color = colors.textSecondary,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(colors.glassPillFill)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Cancel",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                options.forEach { (title, subtitle, targetIntent) ->
                    Surface(
                        onClick = { onSelectShortcut(title, targetIntent) },
                        shape = RoundedCornerShape(16.dp),
                        color = colors.glassCardFill,
                        contentColor = colors.textPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .mindGlassEdge(RoundedCornerShape(16.dp), colors.hairline, colors.rimHighlight),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(colors.accentSoft, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(17.dp),
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    style = MindTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = colors.textPrimary,
                                )
                                Text(
                                    text = subtitle,
                                    style = MindTheme.typography.bodySmall,
                                    color = colors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = null,
                                tint = colors.textMuted,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverlayActionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = MindTheme.colors
    val cardShape = RoundedCornerShape(18.dp)
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .dshPressableScale(interactionSource, pressedScale = 0.96f)
            .mindGlassEdge(cardShape, colors.hairline, colors.rimHighlight),
        shape = cardShape,
        color = colors.glassCardFill,
        contentColor = colors.textPrimary,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(colors.accentSoft, RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = title,
                    style = MindTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MindTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SilentCaptureSetupSheet(
    showSetupOnly: Boolean,
    isServiceActive: Boolean,
    isSettingEnabled: Boolean,
    onRetrySilentCapture: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenRestrictedSettings: () -> Unit,
    onScreenShareOnce: () -> Unit,
    onDismiss: () -> Unit,
) {
    val extended = MindTheme.colors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.42f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        val sheetShape = RoundedCornerShape(30.dp)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp)
                .navigationBarsPadding()
                .shadow(
                    elevation = 24.dp,
                    shape = sheetShape,
                    ambientColor = extended.shadowInk,
                    spotColor = extended.shadowInk,
                )
                .mindGlassEdge(
                    shape = sheetShape,
                    hairlineColor = extended.hairline,
                    rimColor = extended.rimHighlight,
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
            shape = sheetShape,
            color = extended.surfaceElevated,
            contentColor = extended.textPrimary,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                        extended.accentSecondary.copy(alpha = 0.14f),
                                    ),
                                ),
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = if (showSetupOnly) "CAPTURE UNAVAILABLE" else "SILENT CAPTURE SETUP",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Text(
                    text = when {
                        showSetupOnly && isServiceActive ->
                            "Could not capture this screen silently"
                        isSettingEnabled && !isServiceActive ->
                            "Restart Mind Screenshot Service"
                        else ->
                            "Enable 1-Tap Silent Screenshot"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )

                Text(
                    text = when {
                        showSetupOnly && isServiceActive ->
                            "Protected screens (like banking or DRM video) block silent capture, or the system paused briefly."
                        isSettingEnabled && !isServiceActive ->
                            "Accessibility permission is saved, but Android stopped the service in the background. Toggle it off and back on once in Accessibility."
                        else ->
                            "Turn on Mind's on-demand screenshot service once so Quick Settings, Side Key, and Widget save your screen instantly without Android's Screen Share popup."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (!isSettingEnabled) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = extended.surfaceElevated,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(18.dp),
                            )
                            Text(
                                text = "Samsung / Android 13+: If Mind is greyed out in Accessibility, tap Restricted Settings below → App Info → top-right ⋮ menu → Allow restricted settings.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                if (isServiceActive) {
                    Button(
                        onClick = onRetrySilentCapture,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Icon(Icons.Rounded.Screenshot, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("Retry Silent Capture")
                    }
                } else {
                    Button(
                        onClick = onOpenAccessibility,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Icon(Icons.Rounded.AccessibilityNew, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("Open Accessibility Settings")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (!isSettingEnabled) {
                        OutlinedButton(
                            onClick = onOpenRestrictedSettings,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Text("Restricted Settings")
                        }
                    }
                    TextButton(
                        onClick = onScreenShareOnce,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    ) {
                        Text("Screen Share once")
                    }
                }
            }
        }
    }
}
