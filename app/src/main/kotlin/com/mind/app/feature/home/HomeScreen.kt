package com.mind.app.feature.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.mind.app.capture.CaptureTriggerActivity
import com.mind.app.capture.MindCaptureWidgetProvider
import com.mind.app.capture.MindScreenshotAccessibilityService
import com.mind.app.capture.VoiceRecorderHelper
import com.mind.app.core.designsystem.component.LocalMindHazeState
import com.mind.app.core.designsystem.component.MindCollageTile
import com.mind.app.core.designsystem.component.MindDeleteSelectedDialog
import com.mind.app.core.designsystem.component.MindEdgeBlurVeil
import com.mind.app.core.designsystem.component.MindEmptyState
import com.mind.app.core.designsystem.component.MindGlassIconButton
import com.mind.app.core.designsystem.component.MindGlassTextField
import com.mind.app.core.designsystem.component.MindGlassTier
import com.mind.app.core.designsystem.component.MindPillButton
import com.mind.app.core.designsystem.component.MindSaveAnimationOverlay
import com.mind.app.core.designsystem.component.MindSegmentedPillBar
import com.mind.app.core.designsystem.component.MindSelectionActionDock
import com.mind.app.core.designsystem.component.MindSelectionTopBar
import com.mind.app.core.designsystem.component.dshPressableScale
import com.mind.app.core.designsystem.component.mindGlassBackdrop
import com.mind.app.core.designsystem.component.mindGlassEdge
import com.mind.app.core.designsystem.component.mindHorizontalScrollEdgeFade
import com.mind.app.core.designsystem.component.mindScrollEdgeFade
import com.mind.app.core.designsystem.theme.MindTheme
import com.mind.app.core.model.CaptureMethod
import com.mind.app.core.model.CategoryTreeNode
import com.mind.app.core.model.ContentType
import com.mind.app.core.model.Memory
import com.mind.app.feature.collections.MindSpacePickerSheet
import com.mind.app.navigation.ACTION_PICK_MEDIA_CAPTURE
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCollections: () -> Unit,
    onOpenFavourites: () -> Unit,
    onOpenCategory: (String) -> Unit,
    onOpenMemory: (String) -> Unit,
    pendingExternalCaptureAction: String? = null,
    pendingExternalPrefillUrl: String? = null,
    onConsumeExternalCaptureAction: () -> Unit = {},
    onOverlaySheetActiveChanged: (Boolean) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var isSilentCaptureActive by remember {
        mutableStateOf(MindScreenshotAccessibilityService.isServiceActive())
    }
    var showCaptureSheet by remember { mutableStateOf(false) }
    var showNoteSheet by remember { mutableStateOf(false) }
    var showLinkSheet by remember { mutableStateOf(false) }
    var prefillLinkUrl by remember { mutableStateOf("") }
    var showVoiceSheet by remember { mutableStateOf(false) }
    var isRecordingAudio by remember { mutableStateOf(false) }
    var voiceSeconds by remember { mutableIntStateOf(0) }
    var audioPermissionError by remember { mutableStateOf<String?>(null) }
    var showBulkDeleteConfirm by remember { mutableStateOf(false) }
    var showSpacePicker by remember { mutableStateOf(false) }

    val isAnySheetOpen = showCaptureSheet || showVoiceSheet || showNoteSheet || showLinkSheet
    LaunchedEffect(isAnySheetOpen) {
        onOverlaySheetActiveChanged(isAnySheetOpen)
    }
    DisposableEffect(Unit) {
        onDispose { onOverlaySheetActiveChanged(false) }
    }

    BackHandler(enabled = uiState.isSelectionMode) {
        viewModel.clearSelection()
    }

    val voiceRecorder = remember(context) { VoiceRecorderHelper(context) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isSilentCaptureActive = MindScreenshotAccessibilityService.isServiceActive()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            voiceRecorder.stopSilently()
        }
    }

    val startVoiceCapture: () -> Unit = {
        val outputFile = File(viewModel.allocateVoiceOutputPath())
        val started = voiceRecorder.startRecording(outputFile)
        if (started.isSuccess) {
            voiceSeconds = 0
            isRecordingAudio = true
            showVoiceSheet = true
        } else {
            audioPermissionError = "Unable to start microphone recording on this device."
        }
    }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            startVoiceCapture()
        } else {
            audioPermissionError = "Microphone permission is required to record voice memories."
        }
    }

    val requestVoiceCapture: () -> Unit = {
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPerm) {
            startVoiceCapture()
        } else {
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            viewModel.importMediaUri(uri.toString())
        }
    }

    LaunchedEffect(pendingExternalCaptureAction, pendingExternalPrefillUrl) {
        when (pendingExternalCaptureAction) {
            MindCaptureWidgetProvider.ACTION_OPEN_VOICE_CAPTURE -> {
                requestVoiceCapture()
                onConsumeExternalCaptureAction()
            }
            MindCaptureWidgetProvider.ACTION_OPEN_NOTE_CAPTURE -> {
                showNoteSheet = true
                onConsumeExternalCaptureAction()
            }
            MindCaptureWidgetProvider.ACTION_OPEN_LINK_CAPTURE -> {
                prefillLinkUrl = pendingExternalPrefillUrl.orEmpty()
                showLinkSheet = true
                onConsumeExternalCaptureAction()
            }
            ACTION_PICK_MEDIA_CAPTURE -> {
                mediaPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo),
                )
                onConsumeExternalCaptureAction()
            }
            else -> Unit
        }
    }

    LaunchedEffect(isRecordingAudio) {
        if (!isRecordingAudio) return@LaunchedEffect
        while (isRecordingAudio) {
            delay(1000)
            voiceSeconds += 1
        }
    }

    LaunchedEffect(uiState.errorMessage, uiState.statusMessage, audioPermissionError) {
        val msg = uiState.errorMessage ?: uiState.statusMessage ?: audioPermissionError
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
            viewModel.clearStatusMessage()
            audioPermissionError = null
        }
    }

    val navBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val statusTopInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val feedBottomPadding = 148.dp + navBottomInset
    val feedTopPadding = statusTopInset + 62.dp
    val homeHazeState = rememberHazeState()

    var collageColumns by remember { mutableIntStateOf(2) }
    var previousCollageColumns by remember { mutableIntStateOf(2) }
    var gesturePinchScale by remember { mutableFloatStateOf(1.0f) }
    var zoomStepPulse by remember { mutableFloatStateOf(1.0f) }
    var activePhotoViewerMemoryId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(collageColumns) {
        if (collageColumns != previousCollageColumns) {
            val shrinking = collageColumns > previousCollageColumns
            previousCollageColumns = collageColumns
            zoomStepPulse = if (shrinking) 0.92f else 1.08f
            delay(75L)
            zoomStepPulse = 1.0f
        }
    }

    val animatedCanvasScale by animateFloatAsState(
        targetValue = (gesturePinchScale * zoomStepPulse).coerceIn(0.84f, 1.18f),
        animationSpec = spring(dampingRatio = 0.56f, stiffness = 300f),
        label = "home_canvas_zoom_scale",
    )

    val targetTileSpacing = when (collageColumns) {
        1 -> 12.dp
        2 -> 8.dp
        3 -> 6.dp
        else -> 4.dp
    }
    val tileSpacing by animateDpAsState(
        targetValue = targetTileSpacing,
        animationSpec = spring(dampingRatio = 0.66f, stiffness = 300f),
        label = "home_tile_spacing",
    )
    val horizPadding by animateDpAsState(
        targetValue = if (collageColumns >= 3) 10.dp else MindTheme.spacing.screenHorizontal,
        animationSpec = spring(dampingRatio = 0.66f, stiffness = 300f),
        label = "home_horiz_padding",
    )

    val memories = uiState.filteredMemories

    CompositionLocalProvider(LocalMindHazeState provides homeHazeState) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent),
        ) {
            // 1. Immersive Zoomable Collage Canvas
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = homeHazeState)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            var accumZoom = 1f
                            var steppedInGesture = false
                            do {
                                val event = awaitPointerEvent()
                                val pressedCount = event.changes.count { it.pressed }
                                if (pressedCount >= 2) {
                                    val zoomDelta = event.calculateZoom()
                                    if (zoomDelta != 1f) {
                                        accumZoom = (accumZoom * zoomDelta).coerceIn(0.62f, 1.55f)
                                        gesturePinchScale = (1f + (accumZoom - 1f) * 0.48f)
                                            .coerceIn(0.86f, 1.16f)

                                        if (!steppedInGesture) {
                                            if (accumZoom <= 0.78f && collageColumns < 4) {
                                                collageColumns += 1
                                                accumZoom = 1f
                                                gesturePinchScale = 1f
                                                steppedInGesture = true
                                            } else if (accumZoom >= 1.24f && collageColumns > 1) {
                                                collageColumns -= 1
                                                accumZoom = 1f
                                                gesturePinchScale = 1f
                                                steppedInGesture = true
                                            }
                                        } else if (accumZoom in 0.90f..1.10f) {
                                            steppedInGesture = false
                                        }
                                        event.changes.forEach { it.consume() }
                                    }
                                }
                            } while (event.changes.any { it.pressed })
                            gesturePinchScale = 1f
                        }
                    }
                    .graphicsLayer {
                        scaleX = animatedCanvasScale
                        scaleY = animatedCanvasScale
                    }
                    .mindScrollEdgeFade(
                        topFade = statusTopInset + 24.dp,
                        bottomFade = 92.dp + navBottomInset,
                    ),
            ) {
                if (memories.isEmpty() && !uiState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                start = MindTheme.spacing.screenHorizontal,
                                end = MindTheme.spacing.screenHorizontal,
                                top = feedTopPadding,
                                bottom = feedBottomPadding,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        val isFiltered = uiState.selectedFilter != HomeTimelineFilter.ALL ||
                            uiState.selectedCategoryId != null
                        MindEmptyState(
                            title = if (isFiltered) {
                                "No saved memories in this category"
                            } else {
                                "Your saved collage is ready"
                            },
                            subtitle = if (isFiltered) {
                                "Pick All at the bottom or capture something new."
                            } else {
                                "Capture any screen, record voice thoughts, bookmark links, or share from any app."
                            },
                            action = {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    if (isFiltered) {
                                        MindPillButton(
                                            text = "Show All",
                                            onClick = { viewModel.selectFilter(HomeTimelineFilter.ALL) },
                                            compact = true,
                                        )
                                    } else {
                                        MindPillButton(
                                            text = "Import Photo",
                                            onClick = {
                                                mediaPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                                )
                                            },
                                            compact = true,
                                            leadingIcon = Icons.Rounded.PhotoLibrary,
                                        )
                                        MindPillButton(
                                            text = "Add Memory",
                                            onClick = { showCaptureSheet = true },
                                            isPrimary = false,
                                            compact = true,
                                        )
                                    }
                                }
                            },
                        )
                    }
                } else {
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(collageColumns),
                        contentPadding = PaddingValues(
                            start = horizPadding,
                            end = horizPadding,
                            top = feedTopPadding,
                            bottom = feedBottomPadding,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(tileSpacing),
                        verticalItemSpacing = tileSpacing,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        itemsIndexed(
                            items = memories,
                            key = { _, memory -> memory.id },
                            span = { index, memory ->
                                val hasPhoto = memory.contentType != ContentType.VOICE_NOTE &&
                                    (!memory.originalFilePath.isNullOrBlank() || !memory.thumbnailPath.isNullOrBlank())
                                val isHero = collageColumns in 2..3 && hasPhoto && index % 7 == 0
                                if (isHero) StaggeredGridItemSpan.FullLine else StaggeredGridItemSpan.SingleLane
                            },
                        ) { index, memory ->
                            val isSelected = memory.id in uiState.selectedMemoryIds
                            val hasPhoto = memory.contentType != ContentType.VOICE_NOTE &&
                                (!memory.originalFilePath.isNullOrBlank() || !memory.thumbnailPath.isNullOrBlank())
                            val isHero = collageColumns in 2..3 && hasPhoto && index % 7 == 0

                            MindCollageTile(
                                memory = memory,
                                tileIndex = index,
                                columns = collageColumns,
                                isHeroSpan = isHero,
                                selectionMode = uiState.isSelectionMode,
                                selected = isSelected,
                                modifier = Modifier.animateItem(
                                    fadeInSpec = spring(dampingRatio = 0.70f, stiffness = 280f),
                                    placementSpec = spring(dampingRatio = 0.60f, stiffness = 250f),
                                    fadeOutSpec = tween(180),
                                ),
                                onClick = {
                                    when {
                                        uiState.isSelectionMode -> viewModel.toggleMemorySelection(memory.id)
                                        hasPhoto -> activePhotoViewerMemoryId = memory.id
                                        else -> onOpenMemory(memory.id)
                                    }
                                },
                                onLongClick = { viewModel.enterSelectionMode(memory.id) },
                                onToggleFavourite = { viewModel.toggleFavourite(memory) },
                            )
                        }
                    }
                }
            }

            // 2. Soft Top & Bottom Edge Blur Veils
            MindEdgeBlurVeil(
                hazeState = homeHazeState,
                fromTop = true,
                height = statusTopInset + 18.dp,
                tintAlpha = 0.12f,
                modifier = Modifier.align(Alignment.TopCenter),
            )
            MindEdgeBlurVeil(
                hazeState = homeHazeState,
                fromTop = false,
                height = 96.dp + navBottomInset,
                modifier = Modifier.align(Alignment.BottomCenter),
            )

            // 3. Minimal Floating Top Chrome: ONLY "Saved" on left + Settings on right (or Selection bar)
            if (uiState.isSelectionMode) {
                MindSelectionTopBar(
                    selectedCount = uiState.selectedMemoryIds.size,
                    allSelected = memories.isNotEmpty() && memories.all { it.id in uiState.selectedMemoryIds },
                    onClose = viewModel::clearSelection,
                    onToggleSelectAll = viewModel::toggleSelectAllVisible,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding(),
                )
            } else {
                HomeFloatingTopOverlay(
                    savedCount = memories.size,
                    collageColumns = collageColumns,
                    onCycleZoom = {
                        collageColumns = when (collageColumns) {
                            2 -> 3
                            3 -> 4
                            4 -> 1
                            else -> 2
                        }
                    },
                    onOpenSettings = onOpenSettings,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding(),
                )
            }

            // 4. Bottom Floating Categories Bar + Capture Button (or Bulk Selection Dock)
            if (uiState.isSelectionMode) {
                MindSelectionActionDock(
                    selectedCount = uiState.selectedMemoryIds.size,
                    onDeleteSelected = { showBulkDeleteConfirm = true },
                    secondaryActionLabel = "Add to Space",
                    onSecondaryAction = { showSpacePicker = true },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 90.dp),
                )
            } else if (!isAnySheetOpen) {
                HomeBottomCategoriesAndCaptureBar(
                    categories = uiState.dashboard.topCategories,
                    selectedCategoryId = uiState.selectedCategoryId,
                    selectedFilter = uiState.selectedFilter,
                    onSelectAll = { viewModel.selectFilter(HomeTimelineFilter.ALL) },
                    onSelectCategory = viewModel::selectCategoryFilter,
                    onSelectTimelineFilter = viewModel::selectFilter,
                    onOpenCaptureSheet = { showCaptureSheet = true },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 78.dp),
                )
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 144.dp),
            )

            MindSaveAnimationOverlay(
                visible = uiState.activeSaveOverlay != null,
                style = uiState.animationStyle,
                animationsEnabled = uiState.animationsEnabled,
                title = uiState.activeSaveOverlay?.message ?: "Saved to Mind",
                thumbnailPath = uiState.activeSaveOverlay?.thumbnailPath,
                hazeState = homeHazeState,
                onViewMemory = uiState.activeSaveOverlay?.memoryId?.let { targetId ->
                    {
                        viewModel.dismissSaveOverlay()
                        onOpenMemory(targetId)
                    }
                },
                onFinished = viewModel::dismissSaveOverlay,
            )

            // 5. In-Window Frosted-Glass Bottom Sheets (real live Haze blur over the Home collage)
            HomeFrostedBottomSheet(
                visible = showCaptureSheet,
                onDismissRequest = { showCaptureSheet = false },
                hazeState = homeHazeState,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Save to Mind",
                        style = MindTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MindTheme.colors.textPrimary,
                    )
                    Text(
                        text = "Add a voice thought, note, link, or photo — organized automatically by AI.",
                        style = MindTheme.typography.bodySmall,
                        color = MindTheme.colors.textSecondary,
                    )
                }

                // 2x2 Bento Quick-Capture Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CaptureBentoCard(
                        icon = Icons.Rounded.Mic,
                        title = "Voice Note",
                        subtitle = "Speak & transcribe",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            showCaptureSheet = false
                            requestVoiceCapture()
                        },
                    )
                    CaptureBentoCard(
                        icon = Icons.AutoMirrored.Rounded.Notes,
                        title = "Quick Note",
                        subtitle = "Thought or quote",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            showCaptureSheet = false
                            showNoteSheet = true
                        },
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CaptureBentoCard(
                        icon = Icons.Rounded.Link,
                        title = "Save Link",
                        subtitle = "Bookmark a URL",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            showCaptureSheet = false
                            showLinkSheet = true
                        },
                    )
                    CaptureBentoCard(
                        icon = Icons.Rounded.PhotoLibrary,
                        title = "Photo",
                        subtitle = "Pick from gallery",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            showCaptureSheet = false
                            mediaPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                    )
                }
            }

            HomeFrostedBottomSheet(
                visible = showVoiceSheet,
                onDismissRequest = {
                    isRecordingAudio = false
                    voiceRecorder.stopSilently()
                    showVoiceSheet = false
                },
                hazeState = homeHazeState,
            ) {
                val eqTransition = rememberInfiniteTransition(label = "voice_eq")
                val barFractions = listOf(0.45f, 0.9f, 0.65f, 0.95f, 0.55f).mapIndexed { idx, base ->
                    eqTransition.animateFloat(
                        initialValue = 0.25f,
                        targetValue = base,
                        animationSpec = infiniteRepeatable(
                            animation = tween(360 + idx * 95),
                            repeatMode = RepeatMode.Reverse,
                        ),
                        label = "eq_$idx",
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = "Recording Voice Memory",
                        style = MindTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MindTheme.colors.textPrimary,
                    )

                    Row(
                        modifier = Modifier.height(54.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        barFractions.forEach { frac ->
                            Box(
                                modifier = Modifier
                                    .width(7.dp)
                                    .height((14 + 38 * frac.value).dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(MindTheme.colors.accent, MindTheme.colors.accentSecondary),
                                        ),
                                    ),
                            )
                        }
                    }

                    Text(
                        text = String.format(Locale.US, "%02d:%02d", voiceSeconds / 60, voiceSeconds % 60),
                        style = MindTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MindTheme.colors.textPrimary,
                    )
                    Text(
                        text = "Saved locally on-device as .m4a",
                        style = MindTheme.typography.bodySmall,
                        color = MindTheme.colors.textSecondary,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        MindPillButton(
                            text = "Discard",
                            onClick = {
                                isRecordingAudio = false
                                voiceRecorder.stopSilently()
                                showVoiceSheet = false
                            },
                            isPrimary = false,
                            modifier = Modifier.weight(1f),
                        )
                        MindPillButton(
                            text = "Save Audio",
                            onClick = {
                                isRecordingAudio = false
                                val recorded = voiceRecorder.stopAndFinalize()
                                showVoiceSheet = false
                                if (recorded != null) {
                                    viewModel.saveVoiceMemory(recorded.first.absolutePath, recorded.second)
                                } else {
                                    audioPermissionError = "Recording was too short or empty."
                                }
                            },
                            leadingIcon = Icons.Rounded.Stop,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            var noteTitle by remember(showNoteSheet) { mutableStateOf("") }
            var noteBody by remember(showNoteSheet) { mutableStateOf("") }
            var isQuote by remember(showNoteSheet) { mutableStateOf(false) }

            HomeFrostedBottomSheet(
                visible = showNoteSheet,
                onDismissRequest = { showNoteSheet = false },
                hazeState = homeHazeState,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (isQuote) "Save Quote" else "Quick Note",
                        style = MindTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MindTheme.colors.textPrimary,
                    )
                    MindSegmentedPillBar(
                        items = listOf(false, true),
                        selectedItem = isQuote,
                        labelProvider = { if (it) "Quote" else "Note" },
                        onSelect = { isQuote = it },
                        modifier = Modifier.width(142.dp),
                    )
                }

                MindGlassTextField(
                    value = noteTitle,
                    onValueChange = { noteTitle = it },
                    placeholder = "Title (optional)",
                    singleLine = true,
                )
                MindGlassTextField(
                    value = noteBody,
                    onValueChange = { noteBody = it },
                    placeholder = "Write a thought, error message, code snippet, or quote...",
                    singleLine = false,
                    minLines = 4,
                    maxLines = 8,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MindPillButton(
                        text = "Cancel",
                        onClick = { showNoteSheet = false },
                        isPrimary = false,
                        compact = true,
                    )
                    MindPillButton(
                        text = "Save to Mind",
                        onClick = {
                            viewModel.saveQuickNote(noteTitle, noteBody, isQuote)
                            showNoteSheet = false
                        },
                        enabled = noteBody.isNotBlank(),
                        compact = true,
                    )
                }
            }

            var linkUrl by remember(prefillLinkUrl, showLinkSheet) { mutableStateOf(prefillLinkUrl) }
            var linkTitle by remember(showLinkSheet) { mutableStateOf("") }
            var linkNote by remember(showLinkSheet) { mutableStateOf("") }

            HomeFrostedBottomSheet(
                visible = showLinkSheet,
                onDismissRequest = {
                    prefillLinkUrl = ""
                    showLinkSheet = false
                },
                hazeState = homeHazeState,
            ) {
                Text(
                    text = "Save Link / URL",
                    style = MindTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MindTheme.colors.textPrimary,
                )
                MindGlassTextField(
                    value = linkUrl,
                    onValueChange = { linkUrl = it },
                    placeholder = "https://...",
                    singleLine = true,
                )
                MindGlassTextField(
                    value = linkTitle,
                    onValueChange = { linkTitle = it },
                    placeholder = "Title (optional)",
                    singleLine = true,
                )
                MindGlassTextField(
                    value = linkNote,
                    onValueChange = { linkNote = it },
                    placeholder = "Why are you saving this? (optional)",
                    singleLine = false,
                    minLines = 2,
                    maxLines = 4,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MindPillButton(
                        text = "Cancel",
                        onClick = {
                            prefillLinkUrl = ""
                            showLinkSheet = false
                        },
                        isPrimary = false,
                        compact = true,
                    )
                    MindPillButton(
                        text = "Save Link",
                        onClick = {
                            viewModel.saveLinkMemory(linkUrl, linkTitle, linkNote)
                            prefillLinkUrl = ""
                            showLinkSheet = false
                        },
                        enabled = linkUrl.isNotBlank(),
                        compact = true,
                    )
                }
            }
        }
    }

    if (activePhotoViewerMemoryId != null) {
        val photoMemories = remember(memories) {
            memories.filter { mem ->
                mem.contentType != ContentType.VOICE_NOTE &&
                    (!mem.originalFilePath.isNullOrBlank() || !mem.thumbnailPath.isNullOrBlank())
            }
        }
        if (photoMemories.isNotEmpty()) {
            HomeDirectPhotoViewerDialog(
                memories = photoMemories,
                initialMemoryId = activePhotoViewerMemoryId!!,
                selectedIds = uiState.selectedMemoryIds,
                onToggleSelect = { memoryId ->
                    if (!uiState.isSelectionMode) {
                        viewModel.enterSelectionMode(memoryId)
                    } else {
                        viewModel.toggleMemorySelection(memoryId)
                    }
                },
                onToggleFavourite = viewModel::toggleFavourite,
                onOpenDetails = { memoryId ->
                    activePhotoViewerMemoryId = null
                    onOpenMemory(memoryId)
                },
                onDismiss = { activePhotoViewerMemoryId = null },
            )
        } else {
            activePhotoViewerMemoryId = null
        }
    }

    if (showBulkDeleteConfirm && uiState.selectedMemoryIds.isNotEmpty()) {
        MindDeleteSelectedDialog(
            selectedCount = uiState.selectedMemoryIds.size,
            onDismiss = { showBulkDeleteConfirm = false },
            onConfirmDelete = {
                showBulkDeleteConfirm = false
                viewModel.deleteSelectedMemories()
            },
        )
    }

    if (showSpacePicker && uiState.selectedMemoryIds.isNotEmpty()) {
        MindSpacePickerSheet(
            title = "Add ${uiState.selectedMemoryIds.size} ${if (uiState.selectedMemoryIds.size == 1) "item" else "items"} to Space",
            collections = uiState.dashboard.collections,
            onSelectCollection = { collection ->
                showSpacePicker = false
                viewModel.addSelectedMemoriesToCollection(collection.id, collection.name)
            },
            onCreateAndSelect = { name ->
                showSpacePicker = false
                viewModel.createSpaceAndAddSelected(name)
            },
            onDismiss = { showSpacePicker = false },
        )
    }
}

@Composable
private fun HomeFrostedBottomSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    hazeState: HazeState?,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MindTheme.colors
    val sheetShape = RoundedCornerShape(30.dp)
    val scrimInteraction = remember { MutableInteractionSource() }
    val sheetConsumeInteraction = remember { MutableInteractionSource() }

    BackHandler(enabled = visible, onBack = onDismissRequest)

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(180)),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.42f))
                    .clickable(
                        interactionSource = scrimInteraction,
                        indication = null,
                        onClick = onDismissRequest,
                    ),
            )
        }

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                animationSpec = spring(dampingRatio = 0.78f, stiffness = 360f),
                initialOffsetY = { it },
            ) + fadeIn(tween(200)),
            exit = slideOutVertically(
                animationSpec = tween(220),
                targetOffsetY = { it },
            ) + fadeOut(tween(180)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 12.dp),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 24.dp,
                        shape = sheetShape,
                        ambientColor = colors.shadowInk,
                        spotColor = colors.shadowInk,
                    )
                    .then(
                        if (hazeState != null) {
                            Modifier.mindGlassBackdrop(
                                shape = sheetShape,
                                hazeState = hazeState,
                                blurRadius = 32.dp,
                                tintAlpha = if (colors.isDark) 0.54f else 0.68f,
                            )
                        } else {
                            Modifier.clip(sheetShape)
                        },
                    )
                    .clickable(
                        interactionSource = sheetConsumeInteraction,
                        indication = null,
                        onClick = {},
                    )
                    .mindGlassEdge(
                        shape = sheetShape,
                        hairlineColor = colors.hairline,
                        rimColor = colors.rimHighlight,
                        strokeWidth = 0.85.dp,
                    ),
                shape = sheetShape,
                color = if (hazeState != null) Color.Transparent else colors.glassChromeFallback,
                contentColor = colors.textPrimary,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 12.dp, bottom = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(38.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(colors.textMuted.copy(alpha = 0.42f)),
                    )
                    content()
                }
            }
        }
    }
}

@Composable
private fun HomeFloatingTopOverlay(
    savedCount: Int,
    collageColumns: Int,
    onCycleZoom: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MindTheme.colors
    val hazeState = LocalMindHazeState.current
    val pillInteraction = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MindTheme.spacing.screenHorizontal, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Floating "Saved" glass capsule on the left (tapping also cycles collage zoom)
        Surface(
            modifier = Modifier
                .dshPressableScale(pillInteraction, pressedScale = 0.95f)
                .then(
                    if (hazeState != null) {
                        Modifier.mindGlassBackdrop(
                            shape = CircleShape,
                            hazeState = hazeState,
                            blurRadius = 18.dp,
                            tintAlpha = if (colors.isDark) 0.72f else 0.70f,
                        )
                    } else {
                        Modifier.clip(CircleShape)
                    },
                )
                .clickable(
                    interactionSource = pillInteraction,
                    indication = null,
                    onClick = onCycleZoom,
                )
                .mindGlassEdge(
                    shape = CircleShape,
                    hairlineColor = colors.hairline,
                    rimColor = colors.rimHighlight,
                    strokeWidth = 0.65.dp,
                ),
            shape = CircleShape,
            color = if (hazeState != null) Color.Transparent else colors.glassChromeFallback,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Saved",
                    style = MindTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = colors.textPrimary,
                )
            }
        }

        // Floating Settings button on the right
        MindGlassIconButton(
            icon = Icons.Rounded.Settings,
            contentDescription = "Settings",
            onClick = onOpenSettings,
            size = 42.dp,
            iconSize = 19.dp,
            tint = colors.textPrimary,
            tier = MindGlassTier.CHROME,
        )
    }
}

@Composable
private fun HomeBottomCategoriesAndCaptureBar(
    categories: List<CategoryTreeNode>,
    selectedCategoryId: String?,
    selectedFilter: HomeTimelineFilter,
    onSelectAll: () -> Unit,
    onSelectCategory: (String?) -> Unit,
    onSelectTimelineFilter: (HomeTimelineFilter) -> Unit,
    onOpenCaptureSheet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MindTheme.colors
    val hazeState = LocalMindHazeState.current
    val barShape = CircleShape
    val addInteraction = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .shadow(
                    elevation = 12.dp,
                    shape = barShape,
                    ambientColor = colors.shadowInk,
                    spotColor = colors.shadowInk,
                )
                .then(
                    if (hazeState != null) {
                        Modifier.mindGlassBackdrop(
                            shape = barShape,
                            hazeState = hazeState,
                            blurRadius = 22.dp,
                        )
                    } else {
                        Modifier.clip(barShape)
                    },
                )
                .mindGlassEdge(
                    shape = barShape,
                    hairlineColor = colors.hairline,
                    rimColor = colors.rimHighlight,
                ),
            shape = barShape,
            color = if (hazeState != null) Color.Transparent else colors.glassChromeFallback,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .mindHorizontalScrollEdgeFade(leftFade = 16.dp, rightFade = 16.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val allSelected = selectedCategoryId == null && selectedFilter == HomeTimelineFilter.ALL
                item(key = "cat_all") {
                    BottomCategoryChip(
                        label = "All",
                        selected = allSelected,
                        onClick = onSelectAll,
                    )
                }

                items(
                    items = categories.filter { it.memoryCount > 0 },
                    key = { "cat_${it.category.id}" },
                ) { node ->
                    val selected = selectedCategoryId == node.category.id
                    BottomCategoryChip(
                        label = node.category.name,
                        selected = selected,
                        onClick = { onSelectCategory(node.category.id) },
                    )
                }

                val extraFilters = listOf(
                    HomeTimelineFilter.TODAY,
                    HomeTimelineFilter.VOICE,
                    HomeTimelineFilter.PRODUCTS,
                    HomeTimelineFilter.ERRORS,
                )
                items(extraFilters, key = { "tl_${it.name}" }) { filter ->
                    val selected = selectedCategoryId == null && selectedFilter == filter
                    BottomCategoryChip(
                        label = filter.label,
                        selected = selected,
                        onClick = { onSelectTimelineFilter(filter) },
                    )
                }
            }
        }

        Surface(
            onClick = onOpenCaptureSheet,
            interactionSource = addInteraction,
            shape = CircleShape,
            color = Color.Transparent,
            contentColor = Color.White,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier
                .dshPressableScale(addInteraction, pressedScale = 0.92f)
                .shadow(
                    elevation = 14.dp,
                    shape = CircleShape,
                    ambientColor = colors.shadowInk,
                    spotColor = colors.shadowInk,
                )
                .then(
                    if (hazeState != null) {
                        Modifier.mindGlassBackdrop(
                            shape = CircleShape,
                            hazeState = hazeState,
                            blurRadius = 24.dp,
                            tintAlpha = 0.38f,
                        )
                    } else {
                        Modifier.clip(CircleShape)
                    },
                )
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            colors.accent.copy(alpha = 0.86f),
                            colors.accentSecondary.copy(alpha = 0.76f),
                        ),
                    ),
                    CircleShape,
                )
                .mindGlassEdge(
                    shape = CircleShape,
                    hairlineColor = Color.White.copy(alpha = 0.24f),
                    rimColor = Color.White.copy(alpha = 0.48f),
                    strokeWidth = 0.85.dp,
                )
                .size(48.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Capture",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Composable
private fun BottomCategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MindTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val targetBg = if (selected) {
        colors.accent
    } else if (colors.isDark) {
        Color.White.copy(alpha = 0.08f)
    } else {
        Color.Black.copy(alpha = 0.05f)
    }
    val bg by animateColorAsState(
        targetValue = targetBg,
        animationSpec = tween(180),
        label = "bottom_cat_chip_bg",
    )
    val fg by animateColorAsState(
        targetValue = if (selected) Color.White else colors.textSecondary,
        animationSpec = tween(180),
        label = "bottom_cat_chip_fg",
    )

    Box(
        modifier = Modifier
            .dshPressableScale(interactionSource, pressedScale = 0.94f)
            .clip(CircleShape)
            .background(bg)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 13.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MindTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            ),
            color = fg,
            maxLines = 1,
        )
    }
}

@Composable
private fun HomeDirectPhotoViewerDialog(
    memories: List<Memory>,
    initialMemoryId: String,
    selectedIds: Set<String>,
    onToggleSelect: (String) -> Unit,
    onToggleFavourite: (Memory) -> Unit,
    onOpenDetails: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val startIndex = remember(memories, initialMemoryId) {
        memories.indexOfFirst { it.id == initialMemoryId }.coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(initialPage = startIndex) { memories.size }
    var isCurrentZoomed by remember { mutableStateOf(false) }
    val currentMemory = memories.getOrNull(pagerState.currentPage) ?: memories.first()
    val isSelected = currentMemory.id in selectedIds
    val viewerHazeState = rememberHazeState()
    val currentImagePath = remember(currentMemory.originalFilePath, currentMemory.thumbnailPath) {
        (currentMemory.originalFilePath?.takeIf { File(it).exists() } ?: currentMemory.thumbnailPath).orEmpty()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF070A12)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = viewerHazeState),
            ) {
                // Soft ambient backdrop of the current photo so glass controls always have rich content to blur
                if (currentImagePath.isNotBlank()) {
                    AsyncImage(
                        model = File(currentImagePath),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = 0.34f },
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.52f)),
                    )
                }

                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = !isCurrentZoomed,
                    modifier = Modifier.fillMaxSize(),
                ) { pageIndex ->
                    val item = memories[pageIndex]
                    val path = (item.originalFilePath?.takeIf { File(it).exists() } ?: item.thumbnailPath).orEmpty()
                    HomeZoomablePhotoPage(
                        imagePath = path,
                        contentDescription = item.displayTitle,
                        isActivePage = pagerState.currentPage == pageIndex,
                        onZoomStateChanged = { zoomed ->
                            if (pagerState.currentPage == pageIndex) {
                                isCurrentZoomed = zoomed
                            }
                        },
                    )
                }
            }

            // Top floating frosted-glass controls: Close, Select, Favourite, Open Details
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .mindGlassBackdrop(
                            shape = CircleShape,
                            hazeState = viewerHazeState,
                            blurRadius = 24.dp,
                            tintAlpha = 0.36f,
                        )
                        .background(Color.Black.copy(alpha = 0.24f), CircleShape)
                        .mindGlassEdge(
                            shape = CircleShape,
                            hairlineColor = Color.White.copy(alpha = 0.22f),
                            rimColor = Color.White.copy(alpha = 0.38f),
                        )
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .mindGlassBackdrop(
                                shape = CircleShape,
                                hazeState = viewerHazeState,
                                blurRadius = 24.dp,
                                tintAlpha = 0.36f,
                            )
                            .background(
                                if (isSelected) {
                                    MindTheme.colors.accent.copy(alpha = 0.85f)
                                } else {
                                    Color.Black.copy(alpha = 0.24f)
                                },
                                CircleShape,
                            )
                            .mindGlassEdge(
                                shape = CircleShape,
                                hairlineColor = Color.White.copy(alpha = 0.22f),
                                rimColor = Color.White.copy(alpha = 0.38f),
                            )
                            .clickable { onToggleSelect(currentMemory.id) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.CheckCircleOutline,
                            contentDescription = "Select photo",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .mindGlassBackdrop(
                                shape = CircleShape,
                                hazeState = viewerHazeState,
                                blurRadius = 24.dp,
                                tintAlpha = 0.36f,
                            )
                            .background(Color.Black.copy(alpha = 0.24f), CircleShape)
                            .mindGlassEdge(
                                shape = CircleShape,
                                hairlineColor = Color.White.copy(alpha = 0.22f),
                                rimColor = Color.White.copy(alpha = 0.38f),
                            )
                            .clickable { onToggleFavourite(currentMemory) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (currentMemory.isFavourite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Favourite",
                            tint = if (currentMemory.isFavourite) MindTheme.colors.favourite else Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    Box(
                        modifier = Modifier
                            .mindGlassBackdrop(
                                shape = CircleShape,
                                hazeState = viewerHazeState,
                                blurRadius = 24.dp,
                                tintAlpha = 0.36f,
                            )
                            .background(Color.Black.copy(alpha = 0.24f), CircleShape)
                            .mindGlassEdge(
                                shape = CircleShape,
                                hairlineColor = Color.White.copy(alpha = 0.22f),
                                rimColor = Color.White.copy(alpha = 0.38f),
                            )
                            .clickable { onOpenDetails(currentMemory.id) }
                            .padding(horizontal = 15.dp, vertical = 10.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = "Details",
                                style = MindTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White,
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp),
                            )
                        }
                    }
                }
            }

            // Bottom frosted-glass title & page indicator pill
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
                    .mindGlassBackdrop(
                        shape = CircleShape,
                        hazeState = viewerHazeState,
                        blurRadius = 24.dp,
                        tintAlpha = 0.40f,
                    )
                    .background(Color.Black.copy(alpha = 0.26f), CircleShape)
                    .mindGlassEdge(
                        shape = CircleShape,
                        hairlineColor = Color.White.copy(alpha = 0.22f),
                        rimColor = Color.White.copy(alpha = 0.36f),
                    )
                    .padding(horizontal = 16.dp, vertical = 9.dp),
            ) {
                Text(
                    text = "${pagerState.currentPage + 1} / ${memories.size} · ${currentMemory.displayTitle}",
                    style = MindTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun HomeZoomablePhotoPage(
    imagePath: String,
    contentDescription: String,
    isActivePage: Boolean,
    onZoomStateChanged: (Boolean) -> Unit,
) {
    var scale by remember(imagePath) { mutableFloatStateOf(1f) }
    var offset by remember(imagePath) { mutableStateOf(Offset.Zero) }

    LaunchedEffect(isActivePage) {
        if (!isActivePage) {
            scale = 1f
            offset = Offset.Zero
        }
    }

    LaunchedEffect(scale) {
        onZoomStateChanged(scale > 1.05f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(imagePath) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1.1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    },
                )
            }
            .pointerInput(imagePath) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val zoomChange = event.calculateZoom()
                        val panChange = event.calculatePan()
                        val isPinching = event.changes.count { it.pressed } >= 2
                        if (isPinching || scale > 1.05f) {
                            val nextScale = (scale * zoomChange).coerceIn(1f, 5f)
                            scale = nextScale
                            offset = if (nextScale <= 1.01f) Offset.Zero else offset + panChange
                            event.changes.forEach { it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = File(imagePath),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
        )
    }
}

@Composable
private fun CaptureBentoCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = MindTheme.colors
    val cardShape = RoundedCornerShape(20.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val translucentFill = if (colors.isDark) {
        Color.White.copy(alpha = 0.08f)
    } else {
        Color.White.copy(alpha = 0.56f)
    }
    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .dshPressableScale(interactionSource, pressedScale = 0.96f)
            .mindGlassEdge(
                shape = cardShape,
                hairlineColor = colors.hairline,
                rimColor = colors.rimHighlight,
            ),
        shape = cardShape,
        color = translucentFill,
        contentColor = colors.textPrimary,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MindTheme.colors.accentSoft, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MindTheme.colors.accent,
                    modifier = Modifier.size(19.dp),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MindTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MindTheme.colors.textPrimary,
                )
                Text(
                    text = subtitle,
                    style = MindTheme.typography.bodySmall,
                    color = MindTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
