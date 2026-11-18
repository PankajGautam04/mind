package com.mind.app.feature.onboarding

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import com.mind.app.assist.MindAssistantRoleHelper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mind.app.capture.MindScreenshotAccessibilityService
import com.mind.app.core.designsystem.component.MindAnimationPreviewBox
import com.mind.app.core.designsystem.component.MindGlassSelectionCheck
import com.mind.app.core.designsystem.component.MindGlassTextField
import com.mind.app.core.designsystem.component.MindLogoMark
import com.mind.app.core.designsystem.component.MindMistBackdrop
import com.mind.app.core.designsystem.component.MindPillButton
import com.mind.app.core.designsystem.component.MindSelectableChip
import com.mind.app.core.designsystem.component.MindSurfaceCard
import com.mind.app.core.designsystem.component.OneUiListDivider
import com.mind.app.core.designsystem.component.OneUiSectionTitle
import com.mind.app.core.designsystem.theme.MindTheme
import com.mind.app.core.model.AnimationStyle
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    var selectedStyle by remember { mutableStateOf(prefs.animationStyle) }
    var useCloudAi by rememberSaveable { mutableStateOf(!prefs.localOnlyMode) }
    var apiKeyDraft by rememberSaveable { mutableStateOf("") }
    var showApiKey by rememberSaveable { mutableStateOf(false) }
    var selectedModel by rememberSaveable { mutableStateOf(prefs.geminiModel) }
    var initializedFromPrefs by remember { mutableStateOf(false) }

    LaunchedEffect(prefs.animationStyle, prefs.localOnlyMode, prefs.geminiModel) {
        if (!initializedFromPrefs) {
            selectedStyle = prefs.animationStyle
            useCloudAi = !prefs.localOnlyMode
            selectedModel = prefs.geminiModel
            initializedFromPrefs = true
        }
    }

    MindMistBackdrop(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { index ->
                    val isCurrent = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(if (isCurrent) 24.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isCurrent) MindTheme.colors.accent
                                else MindTheme.colors.textMuted.copy(alpha = 0.4f),
                            ),
                    )
                }
            }
            if (pagerState.currentPage < 2) {
                MindPillButton(
                    text = "Skip",
                    onClick = viewModel::skipOnboarding,
                    isPrimary = false,
                    compact = true,
                )
            } else {
                Spacer(modifier = Modifier.height(36.dp))
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) { page ->
            when (page) {
                0 -> OnboardingWelcomePage(
                    selectedStyle = selectedStyle,
                    onSelectStyle = { selectedStyle = it },
                )
                1 -> OnboardingCapturePage()
                else -> OnboardingIntelligencePage(
                    useCloudAi = useCloudAi,
                    onToggleCloudAi = { useCloudAi = it },
                    hasExistingKey = prefs.hasGeminiApiKey,
                    apiKeyDraft = apiKeyDraft,
                    onApiKeyChange = { apiKeyDraft = it },
                    showApiKey = showApiKey,
                    onToggleShowKey = { showApiKey = !showApiKey },
                    selectedModel = selectedModel,
                    onSelectModel = { selectedModel = it },
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (pagerState.currentPage > 0) {
                MindPillButton(
                    text = "Back",
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    },
                    isPrimary = false,
                )
            }
            MindPillButton(
                text = if (pagerState.currentPage < 2) "Continue" else "Get Started",
                onClick = {
                    if (pagerState.currentPage < 2) {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        viewModel.completeOnboarding(
                            useCloudAi = useCloudAi,
                            apiKey = apiKeyDraft,
                            modelId = selectedModel,
                            animationStyle = selectedStyle,
                        )
                    }
                },
                modifier = Modifier.weight(1f),
            )
        }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OnboardingWelcomePage(
    selectedStyle: AnimationStyle,
    onSelectStyle: (AnimationStyle) -> Unit,
) {
    var replayTrigger by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        MindLogoMark(size = 64.dp)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Welcome to Mind",
                style = MindTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                color = MindTheme.colors.textPrimary,
            )
            Text(
                text = "Your personal AI memory for Galaxy. Capture anything on screen in a split second, understand it asynchronously, and recall it with natural language.",
                style = MindTheme.typography.bodyLarge,
                color = MindTheme.colors.textSecondary,
            )
        }

        OneUiSectionTitle(text = "Signature save animation")

        MindSurfaceCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Tap any style to preview",
                        style = MindTheme.typography.titleMedium,
                        color = MindTheme.colors.textPrimary,
                    )
                    MindPillButton(
                        text = "Replay",
                        onClick = { replayTrigger++ },
                        isPrimary = false,
                        compact = true,
                        leadingIcon = Icons.Rounded.Refresh,
                    )
                }
                MindAnimationPreviewBox(
                    style = selectedStyle,
                    replayTrigger = replayTrigger,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    AnimationStyle.entries.forEach { style ->
                        MindSelectableChip(
                            label = style.displayName,
                            selected = selectedStyle == style,
                            onClick = {
                                onSelectStyle(style)
                                replayTrigger++
                            },
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun OnboardingCapturePage() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isDefaultAssistant by remember {
        mutableStateOf(MindAssistantRoleHelper.isMindDefaultAssistant(context))
    }
    var isSilentCaptureActive by remember {
        mutableStateOf(MindScreenshotAccessibilityService.isServiceActive())
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isDefaultAssistant = MindAssistantRoleHelper.isMindDefaultAssistant(context)
                isSilentCaptureActive = MindScreenshotAccessibilityService.isServiceActive()
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Capture from anywhere",
            style = MindTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
            color = MindTheme.colors.textPrimary,
        )
        Text(
            text = "Mind gives you four clean triggers tailored for Samsung One UI — with zero mandatory floating clutter.",
            style = MindTheme.typography.bodyLarge,
            color = MindTheme.colors.textSecondary,
        )

        OneUiSectionTitle(text = "Built-in Galaxy capture triggers")

        MindSurfaceCard(modifier = Modifier.fillMaxWidth()) {
            OnboardingFeatureRow(
                icon = Icons.Rounded.PhoneAndroid,
                iconTint = MindTheme.colors.accent,
                title = "Power / Side Button (Long Press Assistant)",
                subtitle = "Set Mind as your Default Digital Assistant so long-pressing the Power button opens Mind's quick menu or saves your screen silently (configure your action anytime in Settings).",
            )
            OneUiListDivider(insetStart = true)
            OnboardingFeatureRow(
                icon = Icons.Rounded.Tune,
                iconTint = MindTheme.colors.success,
                title = "Quick Settings Panel Tile",
                subtitle = "Swipe down Quick Settings -> Edit -> drag \"Save to Mind\" into your top panel for one-tap screen grabs.",
            )
            OneUiListDivider(insetStart = true)
            OnboardingFeatureRow(
                icon = Icons.Rounded.Widgets,
                iconTint = MindTheme.colors.warning,
                title = "Home Widget & App Icon Shortcuts",
                subtitle = "Long-press Mind's launcher icon or pin the 4x1 Mind Capture widget for Screen, Voice, Note, and Media.",
            )
            OneUiListDivider(insetStart = true)
            OnboardingFeatureRow(
                icon = Icons.Rounded.Share,
                iconTint = MindTheme.colors.favourite,
                title = "Android Share Sheet & Multi-Modal",
                subtitle = "Share posts, URLs, files, or products straight into Mind, or tap + Capture inside the app.",
            )
        }

        MindSurfaceCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Long-Press Power & Silent 1-Tap Setup",
                    style = MindTheme.typography.titleMedium,
                    color = MindTheme.colors.textPrimary,
                )
                Text(
                    text = "Set Mind as your Default Digital Assistant below to unlock Long-Press Power Button (and receive screenshots silently). For Quick Settings or Widget captures without prompts, you can also enable 'Mind One-Tap Screenshot' in Accessibility.",
                    style = MindTheme.typography.bodySmall,
                    color = MindTheme.colors.textSecondary,
                )
                MindPillButton(
                    text = if (isDefaultAssistant) {
                        "Mind is Default Digital Assistant ✓"
                    } else {
                        "Set Mind as Default Assistant"
                    },
                    onClick = {
                        runCatching {
                            assistantRoleLauncher.launch(
                                MindAssistantRoleHelper.buildRequestAssistantRoleIntent(context),
                            )
                        }
                    },
                    isPrimary = false,
                    compact = true,
                )
                MindPillButton(
                    text = if (isSilentCaptureActive) {
                        "Mind One-Tap Screenshot Active ✓"
                    } else {
                        "Enable Silent 1-Tap Screenshot"
                    },
                    onClick = {
                        runCatching {
                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        }
                    },
                    isPrimary = false,
                    compact = true,
                )
                if (!isSilentCaptureActive) {
                    MindPillButton(
                        text = "Greyed out? Allow Restricted Settings",
                        onClick = {
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
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun OnboardingFeatureRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(iconTint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = title,
                style = MindTheme.typography.titleMedium,
                color = MindTheme.colors.textPrimary,
            )
            Text(
                text = subtitle,
                style = MindTheme.typography.bodySmall,
                color = MindTheme.colors.textSecondary,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OnboardingIntelligencePage(
    useCloudAi: Boolean,
    onToggleCloudAi: (Boolean) -> Unit,
    hasExistingKey: Boolean,
    apiKeyDraft: String,
    onApiKeyChange: (String) -> Unit,
    showApiKey: Boolean,
    onToggleShowKey: () -> Unit,
    selectedModel: String,
    onSelectModel: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Intelligence & privacy",
            style = MindTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
            color = MindTheme.colors.textPrimary,
        )
        Text(
            text = "Saving to Mind is always instant. Choose how Mind organizes and searches your memories.",
            style = MindTheme.typography.bodyLarge,
            color = MindTheme.colors.textSecondary,
        )

        OneUiSectionTitle(text = "Processing mode")

        MindSurfaceCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleCloudAi(true) }
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MindTheme.colors.accent,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Cloud Gemini AI (Recommended)",
                        style = MindTheme.typography.titleMedium,
                        color = MindTheme.colors.textPrimary,
                    )
                    Text(
                        text = "Understands screenshots, voice notes & links; powers Semantic AI Search and Ask Mind.",
                        style = MindTheme.typography.bodySmall,
                        color = MindTheme.colors.textSecondary,
                    )
                }
                MindGlassSelectionCheck(selected = useCloudAi)
            }

            OneUiListDivider(insetStart = true)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleCloudAi(false) }
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = MindTheme.colors.success,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Local-Only Privacy Mode",
                        style = MindTheme.typography.titleMedium,
                        color = MindTheme.colors.textPrimary,
                    )
                    Text(
                        text = "No cloud calls ever leave your device. Organize manually and search via local FTS4 indexing.",
                        style = MindTheme.typography.bodySmall,
                        color = MindTheme.colors.textSecondary,
                    )
                }
                MindGlassSelectionCheck(selected = !useCloudAi)
            }
        }

        AnimatedVisibility(visible = useCloudAi) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OneUiSectionTitle(text = "Gemini API Key (Optional now)")
                MindSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        MindGlassTextField(
                            value = apiKeyDraft,
                            onValueChange = onApiKeyChange,
                            placeholder = "AIza...",
                            label = if (hasExistingKey) "API Key saved (leave blank to keep)" else "Gemini API key",
                            singleLine = true,
                            visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .clickable(onClick = onToggleShowKey),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = if (showApiKey) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                        contentDescription = "Toggle key visibility",
                                        tint = MindTheme.colors.textSecondary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            },
                        )
                        Text(
                            text = "Stored in encrypted Android Keystore-backed SharedPreferences. You can add or test your key later in Settings -> Gemini API Key.",
                            style = MindTheme.typography.bodySmall,
                            color = MindTheme.colors.textSecondary,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            listOf(
                                "gemini-3.8-flash" to "3.8 Flash",
                                "gemini-4-hydrogen" to "4 Hydrogen",
                                "gemini-4-argon" to "4 Argon",
                                "gemini-3.5-flash-lite" to "3.5 Flash-Lite",
                            ).forEach { (id, label) ->
                                MindSelectableChip(
                                    label = label,
                                    selected = selectedModel == id,
                                    onClick = { onSelectModel(id) },
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}
