package com.mind.app.feature.settings

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mind.app.assist.MindAssistantRoleHelper
import com.mind.app.capture.CaptureTriggerActivity
import com.mind.app.capture.FloatingCaptureService
import com.mind.app.capture.MindScreenshotAccessibilityService
import com.mind.app.core.designsystem.component.LocalMindHazeState
import com.mind.app.core.designsystem.component.MindAnimationPreviewBox
import com.mind.app.core.designsystem.component.MindCompactPathPill
import com.mind.app.core.designsystem.component.MindEdgeBlurVeil
import com.mind.app.core.designsystem.component.MindFloatingPageTopBar
import com.mind.app.core.designsystem.component.MindGlassDialog
import com.mind.app.core.designsystem.component.MindGlassSelectionCheck
import com.mind.app.core.designsystem.component.MindGlassTextField
import com.mind.app.core.designsystem.component.MindPastelIconDisc
import com.mind.app.core.designsystem.component.MindPillButton
import com.mind.app.core.designsystem.component.MindSegmentedPillBar
import com.mind.app.core.designsystem.component.MindSelectableChip
import com.mind.app.core.designsystem.component.MindSurfaceCard
import com.mind.app.core.designsystem.component.OneUiChevronRow
import com.mind.app.core.designsystem.component.OneUiListDivider
import com.mind.app.core.designsystem.component.OneUiSectionTitle
import com.mind.app.core.designsystem.component.OneUiSwitch
import com.mind.app.core.designsystem.component.mindGlassEdge
import com.mind.app.core.designsystem.component.mindScrollEdgeFade
import com.mind.app.core.designsystem.theme.MindTheme
import com.mind.app.core.designsystem.theme.mindDarkColors
import com.mind.app.core.designsystem.theme.mindLightColors
import com.mind.app.core.model.AccentPalette
import com.mind.app.core.model.AnimationStyle
import com.mind.app.core.model.AssistantAction
import com.mind.app.core.model.CaptureMethod
import com.mind.app.core.model.LayoutMode
import com.mind.app.core.model.ThemeMode
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val prefs = uiState.preferences
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showSilentCaptureDialog by remember { mutableStateOf(false) }
    var showAssistantSetupDialog by remember { mutableStateOf(false) }
    var showAssistantActionDialog by remember { mutableStateOf(false) }
    var isSilentCaptureActive by remember { mutableStateOf(MindScreenshotAccessibilityService.isServiceActive()) }
    var isSilentSettingEnabled by remember {
        mutableStateOf(MindScreenshotAccessibilityService.isAccessibilitySettingEnabled(context))
    }
    var isDefaultAssistant by remember { mutableStateOf(MindAssistantRoleHelper.isMindDefaultAssistant(context)) }
    var showAddExcludedDialog by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isSilentCaptureActive = MindScreenshotAccessibilityService.isServiceActive()
                isSilentSettingEnabled = MindScreenshotAccessibilityService.isAccessibilitySettingEnabled(context)
                isDefaultAssistant = MindAssistantRoleHelper.isMindDefaultAssistant(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val assistantRoleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        isDefaultAssistant = MindAssistantRoleHelper.isMindDefaultAssistant(context)
    }
    var customExcludedPackage by remember { mutableStateOf("") }
    var customExcludedLabel by remember { mutableStateOf("") }
    var apiKeyInput by remember { mutableStateOf("") }
    var showApiKeyPlain by remember { mutableStateOf(false) }
    var customModelInput by remember(prefs.geminiModel) { mutableStateOf(prefs.geminiModel) }
    var showAdvancedProxy by remember { mutableStateOf(prefs.customBackendUrl.isNotBlank()) }
    var customProxyInput by remember(prefs.customBackendUrl) { mutableStateOf(prefs.customBackendUrl) }
    var animationPreviewReplay by remember { mutableIntStateOf(0) }
    var showAiConfig by remember { mutableStateOf(!prefs.hasGeminiApiKey) }
    var showCaptureGuide by remember { mutableStateOf(false) }
    var showExcludedApps by remember { mutableStateOf(false) }
    var showAnimationPicker by remember { mutableStateOf(false) }

    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        if (Settings.canDrawOverlays(context)) {
            viewModel.setFloatingCaptureEnabled(true)
            FloatingCaptureService.start(context)
        } else {
            viewModel.setFloatingCaptureEnabled(false)
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Display over other apps permission was not granted.")
            }
        }
    }

    LaunchedEffect(uiState.statusMessage) {
        val msg = uiState.statusMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearStatusMessage()
    }

    val listState = rememberLazyListState()
    val settingsHazeState = rememberHazeState()
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val scrollFraction by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / 160f).coerceIn(0f, 1f)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = settingsHazeState)
                .mindScrollEdgeFade(
                    topFade = topInset + 64.dp,
                    bottomFade = 42.dp + bottomInset,
                ),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = topInset + 72.dp,
                bottom = bottomInset + 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
                item {
                    MindCompactPathPill(
                        segments = listOf(
                            "${uiState.totalMemoryCount} memories",
                            formatBytes(uiState.storageStats.totalBytes),
                        ),
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OneUiSectionTitle(text = "Intelligence")
                        MindSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                            OneUiSwitchRow(
                                title = "Gemini Cloud AI",
                                subtitle = "Smart titles, transcripts, tags & Ask Mind",
                                checked = prefs.cloudAiEnabled && !prefs.localOnlyMode,
                                onCheckedChange = viewModel::setCloudAiEnabled,
                            )
                            OneUiListDivider(insetStart = false)
                            DisclosureHeaderRow(
                                title = "API Key & Model",
                                subtitle = if (prefs.hasGeminiApiKey) {
                                    "${prefs.geminiModel} · Key configured"
                                } else {
                                    "Tap to add your Gemini API key"
                                },
                                icon = Icons.Rounded.Key,
                                tintIndex = if (prefs.hasGeminiApiKey) 0 else 4,
                                expanded = showAiConfig,
                                onToggle = { showAiConfig = !showAiConfig },
                            )

                            AnimatedVisibility(
                                visible = showAiConfig,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically(),
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    OneUiListDivider(insetStart = false)
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                    ) {
                                    MindGlassTextField(
                                        value = apiKeyInput,
                                        onValueChange = { apiKeyInput = it },
                                        placeholder = "AIza...",
                                        label = if (prefs.hasGeminiApiKey) "Replace Gemini API Key" else "Gemini API Key",
                                        singleLine = true,
                                        visualTransformation = if (showApiKeyPlain) VisualTransformation.None else PasswordVisualTransformation(),
                                        trailingContent = {
                                            Box(
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .clip(CircleShape)
                                                    .clickable { showApiKeyPlain = !showApiKeyPlain },
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Icon(
                                                    imageVector = if (showApiKeyPlain) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                                    contentDescription = "Toggle key visibility",
                                                    tint = MindTheme.colors.textSecondary,
                                                    modifier = Modifier.size(18.dp),
                                                )
                                            }
                                        },
                                    )

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        MindPillButton(
                                            text = "Save API Key",
                                            onClick = {
                                                viewModel.saveGeminiApiKey(apiKeyInput)
                                                apiKeyInput = ""
                                            },
                                            enabled = apiKeyInput.isNotBlank(),
                                            compact = true,
                                            modifier = Modifier.weight(1f),
                                        )
                                        if (prefs.hasGeminiApiKey) {
                                            MindPillButton(
                                                text = "Clear",
                                                onClick = viewModel::clearGeminiApiKey,
                                                isPrimary = false,
                                                isDanger = true,
                                                compact = true,
                                            )
                                        }
                                    }

                                    Text(
                                        text = "Active Model: ${prefs.geminiModel}",
                                        style = MindTheme.typography.titleSmall,
                                        color = MindTheme.colors.textPrimary,
                                    )

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        uiState.availableModels.forEach { model ->
                                            MindSelectableChip(
                                                label = model.id,
                                                selected = prefs.geminiModel == model.id,
                                                onClick = {
                                                    customModelInput = model.id
                                                    viewModel.setGeminiModel(model.id)
                                                },
                                            )
                                        }
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        MindGlassTextField(
                                            value = customModelInput,
                                            onValueChange = { customModelInput = it },
                                            placeholder = "Custom Model ID",
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                        )
                                        MindPillButton(
                                            text = "Use",
                                            onClick = { viewModel.setGeminiModel(customModelInput) },
                                            enabled = customModelInput.isNotBlank(),
                                            isPrimary = false,
                                            compact = true,
                                        )
                                    }

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        MindPillButton(
                                            text = "Fetch Models",
                                            onClick = viewModel::fetchAvailableModels,
                                            enabled = !uiState.isFetchingModels,
                                            isPrimary = false,
                                            isLoading = uiState.isFetchingModels,
                                            compact = true,
                                            leadingIcon = Icons.Rounded.Refresh,
                                        )

                                        MindPillButton(
                                            text = "Test Connection",
                                            onClick = viewModel::testGeminiConnection,
                                            enabled = !uiState.isTestingConnection,
                                            isLoading = uiState.isTestingConnection,
                                            compact = true,
                                            leadingIcon = Icons.Rounded.AutoAwesome,
                                        )

                                        MindPillButton(
                                            text = "Analyze Pending",
                                            onClick = viewModel::retryFailedAnalyses,
                                            enabled = !uiState.isRetryingAnalyses,
                                            isPrimary = false,
                                            isLoading = uiState.isRetryingAnalyses,
                                            compact = true,
                                        )
                                    }

                                    uiState.connectionTestResult?.let { result ->
                                        val resultShape = RoundedCornerShape(16.dp)
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(resultShape)
                                                .background(MindTheme.colors.surfaceSubtle)
                                                .mindGlassEdge(
                                                    shape = resultShape,
                                                    hairlineColor = MindTheme.colors.hairline,
                                                    rimColor = MindTheme.colors.rimHighlight,
                                                    strokeWidth = 0.65.dp,
                                                ),
                                        ) {
                                            Text(
                                                text = if (result.success) {
                                                    "${result.message} (${result.latencyMs} ms)"
                                                } else {
                                                    result.message
                                                },
                                                style = MindTheme.typography.bodySmall,
                                                color = if (result.success) MindTheme.colors.accent else MindTheme.colors.danger,
                                                modifier = Modifier.padding(12.dp),
                                            )
                                        }
                                    }

                                    MindPillButton(
                                        text = if (showAdvancedProxy) "Hide Custom Backend Proxy URL" else "Optional: Custom Backend Proxy URL",
                                        onClick = { showAdvancedProxy = !showAdvancedProxy },
                                        isPrimary = false,
                                        compact = true,
                                    )

                                    if (showAdvancedProxy) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            MindGlassTextField(
                                                value = customProxyInput,
                                                onValueChange = { customProxyInput = it },
                                                placeholder = "Proxy Base URL (https://.../)",
                                                singleLine = true,
                                                modifier = Modifier.weight(1f),
                                            )
                                            MindPillButton(
                                                text = "Save",
                                                onClick = { viewModel.setCustomBackendUrl(customProxyInput) },
                                                isPrimary = false,
                                                compact = true,
                                            )
                                        }
                                    }
                                }

                                OneUiListDivider(insetStart = false)
                                OneUiSwitchRow(
                                    title = "Local-Only Mode",
                                    subtitle = "Keep every capture strictly on device with zero cloud requests",
                                    checked = prefs.localOnlyMode,
                                    onCheckedChange = viewModel::setLocalOnlyMode,
                                )
                                OneUiListDivider(insetStart = false)
                                OneUiSwitchRow(
                                    title = "Automatic Categorization",
                                    subtitle = "Assign categories, tags & connected memories",
                                    checked = prefs.autoCategorizationEnabled,
                                    onCheckedChange = viewModel::setAutoCategorizationEnabled,
                                )
                                }
                            }
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OneUiSectionTitle(text = "Capture & Shortcuts")
                        MindSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                        OneUiChevronRow(
                            title = if (isDefaultAssistant) {
                                "Power Button Assistant: Active"
                            } else {
                                "Power Button Assistant: Not Set"
                            },
                            subtitle = if (isDefaultAssistant) {
                                "Long-press Power / Side Button launches Mind"
                            } else {
                                "Set Mind as Digital Assistant for hardware button trigger"
                            },
                            icon = Icons.Rounded.PhoneAndroid,
                            tintIndex = if (isDefaultAssistant) 5 else 0,
                            onClick = { showAssistantSetupDialog = true },
                        )
                        OneUiListDivider(insetStart = true)
                        OneUiChevronRow(
                            title = "Long-Press Power Action",
                            subtitle = prefs.assistantAction.displayName,
                            icon = Icons.Rounded.Tune,
                            tintIndex = 1,
                            onClick = { showAssistantActionDialog = true },
                        )
                        if (!isSilentCaptureActive && !isSilentSettingEnabled) {
                            OneUiListDivider(insetStart = false)
                            OneUiChevronRow(
                                title = "Accessibility Screen Capture",
                                subtitle = "Enable in Accessibility settings",
                                icon = Icons.Rounded.Bolt,
                                tintIndex = 4,
                                onClick = { showSilentCaptureDialog = true },
                            )
                        }
                        OneUiListDivider(insetStart = false)
                        OneUiChevronRow(
                            title = "Capture Anything Hub (Tasker & Automate)",
                            subtitle = "Pick 'Mind Quick Capture' in Tasker/Automate Shortcuts, or tap to preview",
                            icon = Icons.Rounded.AutoAwesome,
                            tintIndex = 2,
                            onClick = {
                                context.startActivity(
                                    CaptureTriggerActivity.createCaptureAnythingIntent(context),
                                )
                            },
                        )
                        OneUiListDivider(insetStart = false)
                        DisclosureHeaderRow(
                            title = "More Capture Options & Guide",
                            subtitle = "Floating button, confirmation pill & setup instructions",
                            icon = Icons.Rounded.Info,
                            tintIndex = 7,
                            expanded = showCaptureGuide,
                            onToggle = { showCaptureGuide = !showCaptureGuide },
                        )

                        AnimatedVisibility(
                            visible = showCaptureGuide,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically(),
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                OneUiListDivider(insetStart = false)
                                OneUiSwitchRow(
                                    title = "Floating Capture Button",
                                    subtitle = "Optional draggable overlay over other apps",
                                    checked = prefs.floatingCaptureEnabled,
                                    onCheckedChange = { enable ->
                                        if (enable) {
                                            if (Settings.canDrawOverlays(context)) {
                                                viewModel.setFloatingCaptureEnabled(true)
                                                FloatingCaptureService.start(context)
                                            } else {
                                                overlayPermissionLauncher.launch(
                                                    Intent(
                                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                        "package:${context.packageName}".toUri(),
                                                    ),
                                                )
                                            }
                                        } else {
                                            viewModel.setFloatingCaptureEnabled(false)
                                            FloatingCaptureService.stop(context)
                                        }
                                    },
                                )
                                OneUiListDivider(insetStart = false)
                                OneUiSwitchRow(
                                    title = "Capture Confirmation",
                                    subtitle = "Show docked \"Saved\" pill with Undo",
                                    checked = prefs.captureConfirmationEnabled,
                                    onCheckedChange = viewModel::setCaptureConfirmationEnabled,
                                )
                                OneUiListDivider(insetStart = false)
                                Text(
                                    text = "1. Long-Press Power: Set Mind as Digital Assistant above, then map Samsung Side button -> Digital assistant.\n" +
                                        "2. Quick Settings Tile: Swipe down twice -> Edit buttons -> drag \"Save to Mind\".\n" +
                                        "3. Share Menu & Widget: Share anything to Mind or pin the 4x1 widget.",
                                    style = MindTheme.typography.bodySmall,
                                    color = MindTheme.colors.textSecondary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                )
                                OneUiListDivider(insetStart = true)
                                OneUiChevronRow(
                                    title = "Phone Settings (Side Button)",
                                    subtitle = "Assign Side Button Long-Press -> Digital assistant -> Mind",
                                    icon = Icons.Rounded.Settings,
                                    tintIndex = 2,
                                    onClick = {
                                        runCatching {
                                            context.startActivity(Intent(Settings.ACTION_SETTINGS))
                                        }
                                    },
                                )
                                OneUiListDivider(insetStart = true)
                                OneUiChevronRow(
                                    title = "Replay Onboarding Tour",
                                    subtitle = "Review capture methods & privacy setup",
                                    icon = Icons.Rounded.PlayCircleOutline,
                                    tintIndex = 5,
                                    onClick = viewModel::replayOnboarding,
                                )
                            }
                        }
                    }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OneUiSectionTitle(text = "Privacy")
                        val excludedCount = uiState.sourceApps.count { it.isExcluded }
                        MindSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                            DisclosureHeaderRow(
                                title = "Excluded Apps",
                                subtitle = if (excludedCount == 0) {
                                    "Skip cloud AI for sensitive apps"
                                } else {
                                    "$excludedCount app(s) excluded from cloud AI"
                                },
                                icon = Icons.Rounded.PrivacyTip,
                                tintIndex = 5,
                                expanded = showExcludedApps,
                                onToggle = { showExcludedApps = !showExcludedApps },
                            )

                            AnimatedVisibility(
                                visible = showExcludedApps,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically(),
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    if (uiState.sourceApps.isEmpty()) {
                                        OneUiListDivider(insetStart = false)
                                        Text(
                                            text = "No source apps tracked yet. Share content from an app or add a package below.",
                                            style = MindTheme.typography.bodySmall,
                                            color = MindTheme.colors.textMuted,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                                        )
                                    } else {
                                        uiState.sourceApps.forEach { app ->
                                            OneUiListDivider(insetStart = false)
                                            OneUiSwitchRow(
                                                title = app.displayName,
                                                subtitle = "${app.packageName} • ${app.captureCount} captures",
                                                checked = app.isExcluded,
                                                onCheckedChange = { viewModel.toggleAppExcluded(app) },
                                            )
                                        }
                                    }
                                    OneUiListDivider(insetStart = true)
                                    OneUiChevronRow(
                                        title = "Add Excluded Package",
                                        subtitle = "Block cloud AI for a specific app",
                                        icon = Icons.Rounded.Add,
                                        tintIndex = 3,
                                        onClick = {
                                            customExcludedPackage = ""
                                            customExcludedLabel = ""
                                            showAddExcludedDialog = true
                                        },
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OneUiSectionTitle(text = "Personalize")
                        MindSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                Text("Theme", style = MindTheme.typography.titleSmall, color = MindTheme.colors.textPrimary)
                                MindSegmentedPillBar(
                                    items = ThemeMode.entries,
                                    selectedItem = prefs.themeMode,
                                    labelProvider = { mode ->
                                        when (mode) {
                                            ThemeMode.FOLLOW_SYSTEM -> "System"
                                            ThemeMode.LIGHT -> "Light"
                                            ThemeMode.DARK -> "Dark"
                                        }
                                    },
                                    onSelect = viewModel::setThemeMode,
                                    modifier = Modifier.fillMaxWidth(),
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "Accent Colour",
                                        style = MindTheme.typography.titleSmall,
                                        color = MindTheme.colors.textPrimary,
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(MindTheme.colors.accentSoft)
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                    ) {
                                        Text(
                                            text = prefs.accentPalette.displayName,
                                            style = MindTheme.typography.labelSmall,
                                            color = MindTheme.colors.accent,
                                        )
                                    }
                                }
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    AccentPalette.entries.chunked(6).forEach { rowPalettes ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        ) {
                                            rowPalettes.forEach { palette ->
                                                val swatchColor = palette.previewColor(MindTheme.colors.isDark)
                                                val selected = prefs.accentPalette == palette
                                                val tileShape = RoundedCornerShape(14.dp)
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(40.dp)
                                                        .clip(tileShape)
                                                        .background(swatchColor)
                                                        .then(
                                                            if (selected) {
                                                                Modifier.border(
                                                                    width = 2.dp,
                                                                    color = Color.White,
                                                                    shape = tileShape,
                                                                )
                                                            } else {
                                                                Modifier.mindGlassEdge(
                                                                    shape = tileShape,
                                                                    hairlineColor = MindTheme.colors.hairline,
                                                                    rimColor = Color.White.copy(alpha = 0.28f),
                                                                    strokeWidth = 0.65.dp,
                                                                )
                                                            },
                                                        )
                                                        .clickable { viewModel.setAccentPalette(palette) },
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    if (selected) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(22.dp)
                                                                .clip(CircleShape)
                                                                .background(Color.Black.copy(alpha = 0.28f)),
                                                            contentAlignment = Alignment.Center,
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Rounded.Check,
                                                                contentDescription = palette.displayName,
                                                                tint = Color.White,
                                                                modifier = Modifier.size(14.dp),
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            OneUiListDivider(insetStart = false)
                            OneUiSwitchRow(
                                title = "Capture Animations",
                                subtitle = "Play spring dock motion on capture",
                                checked = prefs.animationsEnabled,
                                onCheckedChange = viewModel::setAnimationsEnabled,
                            )
                            OneUiListDivider(insetStart = false)
                            DisclosureHeaderRow(
                                title = "Animation Style",
                                subtitle = prefs.animationStyle.displayName,
                                icon = Icons.Rounded.AutoAwesome,
                                tintIndex = 1,
                                expanded = showAnimationPicker,
                                onToggle = { showAnimationPicker = !showAnimationPicker },
                            )

                            AnimatedVisibility(
                                visible = showAnimationPicker,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically(),
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    OneUiListDivider(insetStart = false)
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        MindAnimationPreviewBox(
                                            style = prefs.animationStyle,
                                            replayTrigger = animationPreviewReplay,
                                        )
                                    }
                                    AnimationStyle.entries.forEach { style ->
                                        OneUiListDivider(insetStart = true)
                                        val isSelectedStyle = prefs.animationStyle == style
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(
                                                    if (isSelectedStyle) {
                                                        MindTheme.colors.accentSoft.copy(alpha = 0.38f)
                                                    } else {
                                                        Color.Transparent
                                                    },
                                                )
                                                .clickable {
                                                    viewModel.setAnimationStyle(style)
                                                    animationPreviewReplay += 1
                                                }
                                                .padding(horizontal = 10.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        ) {
                                            MindGlassSelectionCheck(selected = isSelectedStyle)
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = style.displayName,
                                                    style = MindTheme.typography.bodyMedium,
                                                    color = MindTheme.colors.textPrimary,
                                                )
                                                Text(
                                                    text = style.summary,
                                                    style = MindTheme.typography.bodySmall,
                                                    color = MindTheme.colors.textSecondary,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OneUiSectionTitle(text = "Storage & Cache")
                        MindSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                            val stats = uiState.storageStats
                            StorageStatRow("Saved Memories", uiState.totalMemoryCount.toString())
                            OneUiListDivider(insetStart = false)
                            StorageStatRow("Media & Voice", formatBytes(stats.imageBytes))
                            OneUiListDivider(insetStart = false)
                            StorageStatRow("Thumbnails", formatBytes(stats.thumbnailBytes))
                            OneUiListDivider(insetStart = false)
                            StorageStatRow("Database & Index", formatBytes(stats.databaseBytes))
                            OneUiListDivider(insetStart = false)
                            StorageStatRow("App Cache", formatBytes(stats.cacheBytes))
                            OneUiListDivider(insetStart = false)
                            StorageStatRow("Total Mind Footprint", formatBytes(stats.totalBytes))
                            OneUiListDivider(insetStart = false)

                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                MindPillButton(
                                    text = "Clear App Cache",
                                    onClick = viewModel::clearCache,
                                    isPrimary = false,
                                    leadingIcon = Icons.Rounded.DeleteSweep,
                                    modifier = Modifier.fillMaxWidth(),
                                )

                                MindPillButton(
                                    text = "Delete All Memories",
                                    onClick = { showDeleteAllDialog = true },
                                    enabled = uiState.totalMemoryCount > 0,
                                    isPrimary = false,
                                    isDanger = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }

            MindEdgeBlurVeil(
                hazeState = settingsHazeState,
                fromTop = false,
                height = 56.dp + bottomInset,
                modifier = Modifier.align(Alignment.BottomCenter),
            )

            CompositionLocalProvider(LocalMindHazeState provides settingsHazeState) {
                MindFloatingPageTopBar(
                    title = "Settings",
                    onBack = onBack,
                    scrollFraction = scrollFraction,
                    alwaysShowTitle = true,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
            )
    }

    if (showAddExcludedDialog) {
        MindGlassDialog(
            onDismissRequest = { showAddExcludedDialog = false },
            title = "Exclude App from Cloud AI",
            subtitle = "Enter the Android package name of an app whose captures should remain local-only.",
            content = {
                MindGlassTextField(
                    value = customExcludedPackage,
                    onValueChange = { customExcludedPackage = it },
                    placeholder = "com.whatsapp",
                    label = "Package name",
                    singleLine = true,
                )
                MindGlassTextField(
                    value = customExcludedLabel,
                    onValueChange = { customExcludedLabel = it },
                    placeholder = "WhatsApp",
                    label = "Display label (optional)",
                    singleLine = true,
                )
            },
            actions = {
                MindPillButton(
                    text = "Cancel",
                    onClick = { showAddExcludedDialog = false },
                    isPrimary = false,
                    compact = true,
                )
                MindPillButton(
                    text = "Exclude",
                    onClick = {
                        viewModel.addCustomExcludedApp(customExcludedPackage, customExcludedLabel)
                        showAddExcludedDialog = false
                    },
                    enabled = customExcludedPackage.isNotBlank(),
                    compact = true,
                )
            },
        )
    }

    if (showSilentCaptureDialog) {
        MindGlassDialog(
            onDismissRequest = { showSilentCaptureDialog = false },
            title = when {
                isSilentCaptureActive -> "Silent One-Tap Screenshot Active"
                isSilentSettingEnabled -> "Restart Mind Screenshot Service"
                else -> "Enable Silent One-Tap Screenshot"
            },
            subtitle = when {
                isSilentCaptureActive ->
                    "Mind One-Tap Screenshot is ON. Screenshots triggered from the Quick Settings tile, Side Button, Widget, or Floating button save silently without Android's Screen Share dialog."
                isSilentSettingEnabled ->
                    "Accessibility permission is enabled, but Android paused the service process. Open Accessibility -> Installed apps -> Mind One-Tap Screenshot -> toggle OFF then ON."
                else ->
                    "Android requires confirming a Screen Share dialog on every MediaProjection capture. Enabling Mind's on-demand Accessibility screenshot service once lets Android 11+ save a single screen frame silently."
            },
            content = {
                if (!isSilentCaptureActive) {
                    Text(
                        text = "How to enable:\n" +
                            "1. Tap Open Accessibility -> Installed apps -> Mind One-Tap Screenshot -> Turn ON.\n" +
                            "2. If Android shows 'Restricted setting', tap Restricted Settings below -> ⋮ (top right) -> Allow restricted settings, then retry step 1.",
                        style = MindTheme.typography.bodySmall,
                        color = MindTheme.colors.textPrimary,
                    )
                    MindPillButton(
                        text = "Restricted Settings",
                        onClick = {
                            showSilentCaptureDialog = false
                            runCatching {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        "package:${context.packageName}".toUri(),
                                    ),
                                )
                            }
                        },
                        isPrimary = false,
                        compact = true,
                    )
                }
            },
            actions = {
                MindPillButton(
                    text = "Close",
                    onClick = { showSilentCaptureDialog = false },
                    isPrimary = false,
                    compact = true,
                )
                MindPillButton(
                    text = "Open Accessibility",
                    onClick = {
                        showSilentCaptureDialog = false
                        runCatching {
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        }
                    },
                    compact = true,
                )
            },
        )
    }

    if (showAssistantSetupDialog) {
        MindGlassDialog(
            onDismissRequest = { showAssistantSetupDialog = false },
            title = if (isDefaultAssistant) "Power Button Assistant Active" else "Enable Power Button Assistant",
            subtitle = if (isDefaultAssistant) {
                "Mind is set as your Default Digital Assistant app. Long-pressing the Samsung Side Button (or swiping from a bottom corner) will run: ${prefs.assistantAction.displayName}."
            } else {
                "Android lets any Digital Assistant app respond when you long-press the Power / Side Button — and delivers the current screen silently to Mind."
            },
            content = {
                Text(
                    text = "How to enable on Samsung Galaxy:\n" +
                        "1. Tap Set as Assistant below and select Mind.\n" +
                        "2. Phone Settings -> Advanced features -> Side button -> Long press -> Digital assistant -> confirm Mind.\n" +
                        "3. Enable \"Analyze on-screen images / Use screenshot\" in Default digital assistant settings.",
                    style = MindTheme.typography.bodySmall,
                    color = MindTheme.colors.textPrimary,
                )
                MindPillButton(
                    text = "Default App Settings",
                    onClick = {
                        showAssistantSetupDialog = false
                        runCatching {
                            context.startActivity(
                                MindAssistantRoleHelper.buildDefaultAssistantSettingsIntent(),
                            )
                        }
                    },
                    isPrimary = false,
                    compact = true,
                )
            },
            actions = {
                MindPillButton(
                    text = "Close",
                    onClick = { showAssistantSetupDialog = false },
                    isPrimary = false,
                    compact = true,
                )
                MindPillButton(
                    text = if (isDefaultAssistant) "Assistant Settings" else "Set as Assistant",
                    onClick = {
                        showAssistantSetupDialog = false
                        runCatching {
                            assistantRoleLauncher.launch(
                                MindAssistantRoleHelper.buildRequestAssistantRoleIntent(context),
                            )
                        }
                    },
                    compact = true,
                )
            },
        )
    }

    if (showAssistantActionDialog) {
        MindGlassDialog(
            onDismissRequest = { showAssistantActionDialog = false },
            title = "When Long-Pressing Power Button",
            content = {
                AssistantAction.entries.forEach { actionOption ->
                    val selected = prefs.assistantAction == actionOption
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (selected) MindTheme.colors.accentSoft.copy(alpha = 0.36f) else Color.Transparent,
                            )
                            .clickable {
                                viewModel.setAssistantAction(actionOption)
                                showAssistantActionDialog = false
                            }
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        MindGlassSelectionCheck(selected = selected)
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = actionOption.displayName,
                                style = MindTheme.typography.titleSmall,
                                color = MindTheme.colors.textPrimary,
                            )
                            Text(
                                text = actionOption.summary,
                                style = MindTheme.typography.bodySmall,
                                color = MindTheme.colors.textSecondary,
                            )
                        }
                    }
                }
            },
            actions = {
                MindPillButton(
                    text = "Done",
                    onClick = { showAssistantActionDialog = false },
                    isPrimary = false,
                    compact = true,
                )
            },
        )
    }

    if (showDeleteAllDialog) {
        MindGlassDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = "Delete all memories?",
            subtitle = "This deletes all saved memories, metadata, and local files permanently.",
            actions = {
                MindPillButton(
                    text = "Cancel",
                    onClick = { showDeleteAllDialog = false },
                    isPrimary = false,
                    compact = true,
                )
                MindPillButton(
                    text = "Delete All",
                    onClick = {
                        viewModel.deleteAllMemories()
                        showDeleteAllDialog = false
                    },
                    isDanger = true,
                    compact = true,
                )
            },
        )
    }
}

@Composable
private fun StorageStatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MindTheme.typography.bodyMedium,
            color = MindTheme.colors.textSecondary,
        )
        Text(
            text = value,
            style = MindTheme.typography.titleSmall,
            color = MindTheme.colors.textPrimary,
        )
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024L) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024.0) return String.format(Locale.US, "%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024.0) return String.format(Locale.US, "%.1f MB", mb)
    val gb = mb / 1024.0
    return String.format(Locale.US, "%.2f GB", gb)
}

@Composable
private fun OneUiSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(text = title, style = MindTheme.typography.bodyLarge, color = MindTheme.colors.textPrimary)
            Text(text = subtitle, style = MindTheme.typography.bodySmall, color = MindTheme.colors.textSecondary)
        }
        OneUiSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun DisclosureHeaderRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tintIndex: Int = 0,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "disclosureChevronRotation",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MindPastelIconDisc(
            icon = icon,
            tintIndex = tintIndex,
            size = 32.dp,
            iconSize = 18.dp,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, style = MindTheme.typography.bodyLarge, color = MindTheme.colors.textPrimary)
            Text(text = subtitle, style = MindTheme.typography.bodySmall, color = MindTheme.colors.textSecondary)
        }
        Icon(
            imageVector = Icons.Rounded.KeyboardArrowDown,
            contentDescription = if (expanded) "Collapse" else "Expand",
            tint = MindTheme.colors.textMuted,
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer { rotationZ = chevronRotation },
        )
    }
}

private fun AccentPalette.previewColor(isDark: Boolean): Color =
    if (isDark) mindDarkColors(this).accent else mindLightColors(this).accent
