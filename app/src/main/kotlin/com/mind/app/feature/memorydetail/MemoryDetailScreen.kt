package com.mind.app.feature.memorydetail

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.mind.app.capture.VoicePlaybackHelper
import com.mind.app.core.designsystem.component.LocalMindHazeState
import com.mind.app.core.designsystem.component.MindCompactPathPill
import com.mind.app.core.designsystem.component.MindEdgeBlurVeil
import com.mind.app.core.designsystem.component.MindEmptyState
import com.mind.app.core.designsystem.component.MindFloatingPageTopBar
import com.mind.app.core.designsystem.component.MindGlassDialog
import com.mind.app.core.designsystem.component.MindGlassIconButton
import com.mind.app.core.designsystem.component.MindGlassTextField
import com.mind.app.core.designsystem.component.MindGlassTier
import com.mind.app.core.designsystem.component.MindPillButton
import com.mind.app.core.designsystem.component.MindProcessingBadge
import com.mind.app.core.designsystem.component.MindSelectableChip
import com.mind.app.core.designsystem.component.MindSurfaceCard
import com.mind.app.core.designsystem.component.MindTagPill
import com.mind.app.core.designsystem.component.OneUiSectionTitle
import com.mind.app.core.designsystem.component.mindGlassBackdrop
import com.mind.app.core.designsystem.component.mindGlassEdge
import com.mind.app.core.designsystem.component.mindScrollEdgeFade
import com.mind.app.core.designsystem.theme.MindTheme
import com.mind.app.core.model.AiProcessingStatus
import com.mind.app.core.model.ContentType
import com.mind.app.core.model.Memory
import com.mind.app.feature.collections.MindSpacePickerSheet
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MemoryDetailScreen(
    onBack: () -> Unit,
    onOpenMemory: (String) -> Unit,
    viewModel: MemoryDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val detail = uiState.detail
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showSpacePicker by remember { mutableStateOf(false) }
    var showAskMemoryBox by remember { mutableStateOf(false) }
    var showFullscreenViewer by remember { mutableStateOf(false) }
    var isPlayingAudio by remember { mutableStateOf(false) }

    val playbackHelper = remember { VoicePlaybackHelper() }
    DisposableEffect(Unit) {
        onDispose { playbackHelper.stop() }
    }

    LaunchedEffect(uiState.statusMessage) {
        val msg = uiState.statusMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearStatusMessage()
    }

    val listState = rememberLazyListState()
    val detailHazeState = rememberHazeState()
    val statusTopInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val scrollFraction by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) {
                1f
            } else {
                (listState.firstVisibleItemScrollOffset / 180f).coerceIn(0f, 1f)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
    ) {
        if (detail == null && !uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                MindEmptyState(
                    title = "Memory not found",
                    subtitle = "This memory may have been deleted.",
                    action = {
                        MindPillButton(
                            text = "Go Back",
                            onClick = onBack,
                            compact = true,
                        )
                    },
                )
            }
        } else if (detail != null) {
            val memory = detail.memory
            val formattedTimestamp = remember(memory.capturedAtEpochMillis) {
                Instant.ofEpochMilli(memory.capturedAtEpochMillis)
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("MMM d, yyyy · h:mm a"))
            }
            val smartActions = remember(
                memory.id,
                memory.extractedText,
                memory.userNotes,
                memory.description,
                memory.originalUrl,
            ) {
                MemorySmartActions.detect(memory)
            }
            val pathSegments = remember(
                memory.primaryCategory?.name,
                memory.subcategory?.name,
                detail.collections,
            ) {
                buildList {
                    memory.primaryCategory?.name?.let(::add)
                    memory.subcategory?.name?.let(::add)
                    detail.collections.firstOrNull()?.name?.let { add("Space: $it") }
                }
            }
            var titleExpanded by remember(memory.id) { mutableStateOf(false) }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = detailHazeState)
                    .mindScrollEdgeFade(
                        topFade = statusTopInset + 64.dp,
                        bottomFade = 42.dp + navBottomInset,
                    ),
                contentPadding = PaddingValues(
                    start = MindTheme.spacing.screenHorizontal,
                    end = MindTheme.spacing.screenHorizontal,
                    top = statusTopInset + 72.dp,
                    bottom = navBottomInset + 32.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                    if (pathSegments.isNotEmpty()) {
                        item {
                            MindCompactPathPill(
                                segments = pathSegments,
                                onClick = { showSpacePicker = true },
                            )
                        }
                    }

                    if (memory.contentType == ContentType.VOICE_NOTE && !memory.originalFilePath.isNullOrBlank()) {
                    item {
                        MindSurfaceCard {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(MindTheme.colors.accentSoft, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.GraphicEq,
                                        contentDescription = null,
                                        tint = MindTheme.colors.accent,
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Voice Recording",
                                        style = MindTheme.typography.titleSmall,
                                        color = MindTheme.colors.textPrimary,
                                    )
                                    Text(
                                        text = if (isPlayingAudio) "Playing audio..." else "Tap Play to listen",
                                        style = MindTheme.typography.bodySmall,
                                        color = MindTheme.colors.textSecondary,
                                    )
                                }
                                MindPillButton(
                                    text = if (isPlayingAudio) "Stop" else "Play",
                                    onClick = {
                                        if (isPlayingAudio) {
                                            playbackHelper.stop()
                                            isPlayingAudio = false
                                        } else {
                                            val ok = playbackHelper.play(
                                                filePath = memory.originalFilePath!!,
                                                onCompleted = { isPlayingAudio = false },
                                                onError = {
                                                    isPlayingAudio = false
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar("Unable to play audio file")
                                                    }
                                                },
                                            )
                                            isPlayingAudio = ok
                                        }
                                    },
                                    compact = true,
                                    leadingIcon = if (isPlayingAudio) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                )
                            }
                        }
                    }
                } else {
                    val imagePath = memory.originalFilePath ?: memory.thumbnailPath
                    if (!imagePath.isNullOrBlank()) {
                        item {
                            val imageCardShape = RoundedCornerShape(24.dp)
                            MindSurfaceCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = imageCardShape,
                                onClick = { showFullscreenViewer = true },
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(imageCardShape),
                                ) {
                                    AsyncImage(
                                        model = File(imagePath),
                                        contentDescription = memory.displayTitle,
                                        contentScale = ContentScale.FillWidth,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(12.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF0A0C12).copy(alpha = 0.72f))
                                            .mindGlassEdge(
                                                shape = CircleShape,
                                                hairlineColor = Color.White.copy(alpha = 0.22f),
                                                rimColor = Color.Transparent,
                                                strokeWidth = 0.75.dp,
                                            ),
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.ZoomIn,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(15.dp),
                                            )
                                            Text(
                                                text = "Fullscreen",
                                                style = MindTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = Color.White,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                MindProcessingBadge(status = memory.aiStatus)
                                Text(
                                    text = formattedTimestamp,
                                    style = MindTheme.typography.labelMedium,
                                    color = MindTheme.colors.textMuted,
                                )
                            }
                            MindPillButton(
                                text = when {
                                    uiState.isAnalyzing -> "Analyzing"
                                    memory.aiStatus == AiProcessingStatus.FAILED -> "Retry Gemini"
                                    memory.aiStatus == AiProcessingStatus.COMPLETED -> "Re-analyze"
                                    else -> "Analyze Now"
                                },
                                onClick = viewModel::analyzeWithGemini,
                                enabled = !uiState.isAnalyzing,
                                isPrimary = false,
                                isLoading = uiState.isAnalyzing,
                                compact = true,
                                leadingIcon = Icons.Rounded.AutoAwesome,
                            )
                        }

                        Text(
                            text = memory.displayTitle,
                            style = MindTheme.typography.headlineMedium,
                            color = MindTheme.colors.textPrimary,
                            maxLines = if (titleExpanded) Int.MAX_VALUE else 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable { titleExpanded = !titleExpanded },
                        )
                        memory.sourceAppName?.let { source ->
                            Text(
                                text = "Captured from $source",
                                style = MindTheme.typography.bodySmall,
                                color = MindTheme.colors.textSecondary,
                            )
                        }
                        detail.latestAiJob?.lastError?.takeIf { memory.aiStatus == AiProcessingStatus.FAILED }?.let { err ->
                            Text(
                                text = "AI notice: $err",
                                style = MindTheme.typography.bodySmall,
                                color = MindTheme.colors.danger,
                            )
                        }
                    }
                }

                if (smartActions.isNotEmpty()) {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(smartActions, key = { it.id }) { action ->
                                SmartActionChip(
                                    action = action,
                                    onClick = {
                                        when (action.kind) {
                                            SmartActionKind.COPY_CODE -> {
                                                coroutineScope.launch {
                                                    clipboard.setClipEntry(
                                                        ClipEntry(ClipData.newPlainText("Copied code", action.value))
                                                    )
                                                    snackbarHostState.showSnackbar("Copied ${action.value}")
                                                }
                                            }
                                            SmartActionKind.OPEN_URL -> runCatching {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, action.value.toUri()))
                                            }
                                            SmartActionKind.CALL_PHONE -> runCatching {
                                                context.startActivity(Intent(Intent.ACTION_DIAL, "tel:${action.value}".toUri()))
                                            }
                                            SmartActionKind.SEND_EMAIL -> runCatching {
                                                context.startActivity(Intent(Intent.ACTION_SENDTO, "mailto:${action.value}".toUri()))
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }
                }

                memory.userNotes?.takeIf { it.isNotBlank() }?.let { notes ->
                    item {
                        MindSurfaceCard {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = "Your Notes",
                                    style = MindTheme.typography.labelLarge,
                                    color = MindTheme.colors.accent,
                                )
                                Text(
                                    text = notes,
                                    style = MindTheme.typography.bodyMedium,
                                    color = MindTheme.colors.textPrimary,
                                )
                            }
                        }
                    }
                }

                memory.description?.takeIf { it.isNotBlank() }?.let { description ->
                    item {
                        MindSurfaceCard(
                            shape = RoundedCornerShape(24.dp),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                MindTheme.colors.accentSoft.copy(alpha = 0.34f),
                                                Color.Transparent,
                                            ),
                                        ),
                                    )
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = MindTheme.colors.accent,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        text = "AI Summary",
                                        style = MindTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MindTheme.colors.accent,
                                    )
                                }
                                Text(
                                    text = description,
                                    style = MindTheme.typography.bodyLarge,
                                    color = MindTheme.colors.textPrimary,
                                )
                            }
                        }
                    }
                }

                memory.extractedText?.takeIf { it.isNotBlank() }?.let { extracted ->
                    item {
                        MindSurfaceCard {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = if (memory.contentType == ContentType.VOICE_NOTE) "Transcript" else "Extracted Text",
                                        style = MindTheme.typography.labelLarge,
                                        color = MindTheme.colors.accent,
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                coroutineScope.launch {
                                                    clipboard.setClipEntry(
                                                        ClipEntry(ClipData.newPlainText("Extracted text", extracted))
                                                    )
                                                    snackbarHostState.showSnackbar("Text copied")
                                                }
                                            },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.ContentCopy,
                                            contentDescription = "Copy text",
                                            tint = MindTheme.colors.textSecondary,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                                Text(
                                    text = extracted,
                                    style = MindTheme.typography.bodyMedium,
                                    color = MindTheme.colors.textSecondary,
                                )
                            }
                        }
                    }
                }

                item {
                    val isAskExpanded = showAskMemoryBox ||
                        uiState.isAskingMemory ||
                        uiState.askMemoryAnswer != null ||
                        uiState.askMemoryError != null
                    MindSurfaceCard {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showAskMemoryBox = !isAskExpanded },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = MindTheme.colors.accent,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Text(
                                        text = "Ask about this memory",
                                        style = MindTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                        color = MindTheme.colors.textPrimary,
                                    )
                                }
                                Text(
                                    text = if (isAskExpanded) "Hide" else "Ask ✨",
                                    style = MindTheme.typography.labelMedium,
                                    color = MindTheme.colors.accent,
                                )
                            }

                            AnimatedVisibility(visible = isAskExpanded) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    MindGlassTextField(
                                        value = uiState.askQuestion,
                                        onValueChange = viewModel::updateAskQuestion,
                                        placeholder = "e.g. What is the deadline or key takeaway?",
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                        keyboardActions = KeyboardActions(onSend = { viewModel.submitMemoryQuestion() }),
                                        trailingContent = {
                                            if (uiState.isAskingMemory) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(18.dp),
                                                    strokeWidth = 2.dp,
                                                )
                                            } else {
                                                val canSend = uiState.askQuestion.isNotBlank()
                                                Box(
                                                    modifier = Modifier
                                                        .size(30.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (canSend) MindTheme.colors.accentSoft else Color.Transparent,
                                                        )
                                                        .clickable(enabled = canSend, onClick = viewModel::submitMemoryQuestion),
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Rounded.Send,
                                                        contentDescription = "Ask Gemini",
                                                        tint = if (canSend) MindTheme.colors.accent else MindTheme.colors.textMuted,
                                                        modifier = Modifier.size(16.dp),
                                                    )
                                                }
                                            }
                                        },
                                    )

                                    uiState.askMemoryAnswer?.let { answer ->
                                        val answerShape = RoundedCornerShape(14.dp)
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(answerShape)
                                                .background(MindTheme.colors.accentSoft.copy(alpha = 0.45f))
                                                .mindGlassEdge(
                                                    shape = answerShape,
                                                    hairlineColor = MindTheme.colors.accent.copy(alpha = 0.25f),
                                                    rimColor = Color.Transparent,
                                                    strokeWidth = 0.75.dp,
                                                ),
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                            ) {
                                                Text(
                                                    text = answer,
                                                    style = MindTheme.typography.bodyMedium,
                                                    color = MindTheme.colors.textPrimary,
                                                )
                                                MindPillButton(
                                                    text = "Clear",
                                                    onClick = {
                                                        showAskMemoryBox = false
                                                        viewModel.clearMemoryAnswer()
                                                    },
                                                    isPrimary = false,
                                                    compact = true,
                                                )
                                            }
                                        }
                                    }

                                    uiState.askMemoryError?.let { error ->
                                        Text(
                                            text = error,
                                            style = MindTheme.typography.bodySmall,
                                            color = MindTheme.colors.danger,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    MindSurfaceCard {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Organization & Context",
                                    style = MindTheme.typography.labelLarge,
                                    color = MindTheme.colors.accent,
                                )
                                MindPillButton(
                                    text = "Space",
                                    onClick = { showSpacePicker = true },
                                    isPrimary = false,
                                    compact = true,
                                    leadingIcon = Icons.Rounded.Add,
                                )
                            }
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                memory.primaryCategory?.let { MindTagPill(it.name, highlighted = true) }
                                memory.subcategory?.let { MindTagPill(it.name, highlighted = true) }
                                detail.collections.forEach { collection ->
                                    Box(modifier = Modifier.clickable { showSpacePicker = true }) {
                                        MindTagPill("Space: ${collection.name}", highlighted = true)
                                    }
                                }
                                memory.tags.forEach { MindTagPill(it.name) }
                                memory.entities.forEach { MindTagPill(it) }
                            }
                        }
                    }
                }

                memory.originalUrl?.takeIf { it.isNotBlank() }?.let { url ->
                    item {
                        MindPillButton(
                            text = "Open Original URL",
                            onClick = {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                                }
                            },
                            isPrimary = false,
                            leadingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                if (detail.connections.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OneUiSectionTitle(text = "Connected memories")
                            detail.connections.forEach { conn ->
                                MindSurfaceCard(onClick = { onOpenMemory(conn.targetMemoryId) }) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(
                                            text = conn.targetTitle,
                                            style = MindTheme.typography.titleSmall,
                                            color = MindTheme.colors.textPrimary,
                                        )
                                        conn.reason?.let { reason ->
                                            Text(
                                                text = reason,
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

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }

            MindEdgeBlurVeil(
                hazeState = detailHazeState,
                fromTop = false,
                height = 56.dp + navBottomInset,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        CompositionLocalProvider(
            LocalMindHazeState provides if (detail != null) detailHazeState else null,
        ) {
            MindFloatingPageTopBar(
                title = "",
                onBack = onBack,
                scrollFraction = scrollFraction,
                alwaysShowTitle = false,
                modifier = Modifier.align(Alignment.TopCenter),
                actions = {
                    if (detail != null) {
                        MindGlassIconButton(
                            icon = Icons.Rounded.Share,
                            contentDescription = "Share memory",
                            onClick = { shareMemory(context, detail.memory) },
                            size = 38.dp,
                            iconSize = 17.dp,
                            tier = MindGlassTier.CHROME,
                        )
                        MindGlassIconButton(
                            icon = Icons.Rounded.Edit,
                            contentDescription = "Edit metadata",
                            onClick = { showEditDialog = true },
                            size = 38.dp,
                            iconSize = 17.dp,
                            tier = MindGlassTier.CHROME,
                        )
                        MindGlassIconButton(
                            icon = if (detail.memory.isFavourite) {
                                Icons.Rounded.Favorite
                            } else {
                                Icons.Rounded.FavoriteBorder
                            },
                            contentDescription = "Toggle favourite",
                            onClick = viewModel::toggleFavourite,
                            size = 38.dp,
                            iconSize = 17.dp,
                            tint = if (detail.memory.isFavourite) {
                                MindTheme.colors.favourite
                            } else {
                                MindTheme.colors.textSecondary
                            },
                            tier = MindGlassTier.CHROME,
                        )
                        MindGlassIconButton(
                            icon = Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete memory",
                            onClick = { showDeleteConfirm = true },
                            size = 38.dp,
                            iconSize = 17.dp,
                            tint = MindTheme.colors.danger,
                            tier = MindGlassTier.CHROME,
                        )
                    }
                },
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

    if (showEditDialog && detail != null) {
        val memory = detail.memory
        var titleInput by remember(memory.id) { mutableStateOf(memory.userTitle ?: memory.displayTitle) }
        var notesInput by remember(memory.id) { mutableStateOf(memory.userNotes.orEmpty()) }
        var tagsInput by remember(memory.id) {
            mutableStateOf(memory.tags.joinToString(", ") { it.name })
        }
        var selectedCategoryId by remember(memory.id) { mutableStateOf(memory.primaryCategory?.id) }
        var selectedSubcategoryId by remember(memory.id) { mutableStateOf(memory.subcategory?.id) }

        val selectedParentNode = uiState.categories.firstOrNull { it.category.id == selectedCategoryId }

        MindGlassDialog(
            onDismissRequest = { showEditDialog = false },
            title = "Edit Memory",
            content = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    MindGlassTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        placeholder = "Memory title",
                        label = "Title",
                        singleLine = true,
                    )
                    MindGlassTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        placeholder = "Add personal context...",
                        label = "Personal Notes",
                        singleLine = false,
                        minLines = 2,
                        maxLines = 4,
                    )
                    MindGlassTextField(
                        value = tagsInput,
                        onValueChange = { tagsInput = it },
                        placeholder = "design, receipt, inspiration",
                        label = "Tags (comma separated)",
                        singleLine = true,
                    )
                    if (uiState.categories.isNotEmpty()) {
                        Text(
                            text = "Category",
                            style = MindTheme.typography.labelLarge,
                            color = MindTheme.colors.textSecondary,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            uiState.categories.forEach { node ->
                                MindSelectableChip(
                                    label = node.category.name,
                                    selected = selectedCategoryId == node.category.id,
                                    onClick = {
                                        selectedCategoryId = node.category.id
                                        selectedSubcategoryId = null
                                    },
                                )
                            }
                        }
                    }
                    if (selectedParentNode != null && selectedParentNode.subcategories.isNotEmpty()) {
                        Text(
                            text = "Subcategory",
                            style = MindTheme.typography.labelLarge,
                            color = MindTheme.colors.textSecondary,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            selectedParentNode.subcategories.forEach { sub ->
                                MindSelectableChip(
                                    label = sub.category.name,
                                    selected = selectedSubcategoryId == sub.category.id,
                                    onClick = {
                                        selectedSubcategoryId = if (selectedSubcategoryId == sub.category.id) {
                                            null
                                        } else {
                                            sub.category.id
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            },
            actions = {
                MindPillButton(
                    text = "Cancel",
                    onClick = { showEditDialog = false },
                    isPrimary = false,
                    compact = true,
                )
                MindPillButton(
                    text = "Save",
                    onClick = {
                        viewModel.updateMetadata(
                            title = titleInput,
                            notes = notesInput,
                            categoryId = selectedCategoryId,
                            subcategoryId = selectedSubcategoryId,
                            tagsRaw = tagsInput,
                        )
                        showEditDialog = false
                    },
                    compact = true,
                )
            },
        )
    }

    if (showDeleteConfirm) {
        MindGlassDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = "Delete memory?",
            subtitle = "This permanently deletes the memory metadata and its locally stored files.",
            actions = {
                MindPillButton(
                    text = "Cancel",
                    onClick = { showDeleteConfirm = false },
                    isPrimary = false,
                    compact = true,
                )
                MindPillButton(
                    text = "Delete",
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteMemory(onBack)
                    },
                    isDanger = true,
                    compact = true,
                )
            },
        )
    }

    if (showSpacePicker && detail != null) {
        val assignedIds = detail.collections.mapTo(HashSet()) { it.id }
        MindSpacePickerSheet(
            title = "Organize in Spaces",
            collections = uiState.allCollections,
            assignedCollectionIds = assignedIds,
            onSelectCollection = { collection ->
                viewModel.toggleCollectionMembership(collection, collection.id in assignedIds)
            },
            onCreateAndSelect = { name ->
                viewModel.createSpaceAndAssign(name)
            },
            onDismiss = { showSpacePicker = false },
        )
    }

    if (showFullscreenViewer && detail != null) {
        val memory = detail.memory
        val fallbackPage = (memory.originalFilePath ?: memory.thumbnailPath)?.takeIf { it.isNotBlank() }?.let {
            listOf(FullscreenImagePage(memory.id, memory.displayTitle, it))
        }.orEmpty()
        val pages = uiState.fullscreenImagePages.ifEmpty { fallbackPage }
        if (pages.isNotEmpty()) {
            FullscreenImagePagerDialog(
                pages = pages,
                initialMemoryId = memory.id,
                onDismiss = { showFullscreenViewer = false },
            )
        }
    }
}

@Composable
private fun SmartActionChip(
    action: MemorySmartAction,
    onClick: () -> Unit,
) {
    val icon = when (action.kind) {
        SmartActionKind.COPY_CODE -> Icons.Rounded.ContentCopy
        SmartActionKind.CALL_PHONE -> Icons.Rounded.Phone
        SmartActionKind.SEND_EMAIL -> Icons.Rounded.Email
        SmartActionKind.OPEN_URL -> Icons.AutoMirrored.Rounded.OpenInNew
    }
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(MindTheme.colors.accentSoft)
            .mindGlassEdge(
                shape = CircleShape,
                hairlineColor = MindTheme.colors.accent.copy(alpha = 0.25f),
                rimColor = Color.Transparent,
                strokeWidth = 0.75.dp,
            )
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MindTheme.colors.accent,
                modifier = Modifier.size(15.dp),
            )
            Text(
                text = action.label,
                style = MindTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MindTheme.colors.accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun shareMemory(context: Context, memory: Memory) {
    val shareText = buildString {
        append(memory.displayTitle)
        memory.originalUrl?.takeIf { it.isNotBlank() }?.let {
            append("\n")
            append(it)
        }
        memory.userNotes?.takeIf { it.isNotBlank() }?.let {
            append("\n\n")
            append(it)
        }
        if (memory.contentType == ContentType.TEXT || memory.contentType == ContentType.QUOTE) {
            memory.extractedText?.takeIf { it.isNotBlank() }?.let {
                append("\n\n")
                append(it)
            }
        }
    }.trim()

    val mediaFile = memory.originalFilePath?.takeIf { it.isNotBlank() }?.let(::File)?.takeIf { it.exists() }
    val sendIntent = if (mediaFile != null) {
        val uri = runCatching {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", mediaFile)
        }.getOrNull()
        if (uri != null) {
            val mime = when {
                memory.contentType == ContentType.VOICE_NOTE || mediaFile.extension.equals("m4a", true) -> "audio/mp4"
                mediaFile.extension.equals("png", true) -> "image/png"
                mediaFile.extension.equals("webp", true) -> "image/webp"
                else -> "image/*"
            }
            Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, memory.displayTitle)
                if (shareText.isNotBlank()) putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, memory.displayTitle)
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
        }
    } else {
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, memory.displayTitle)
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
    }

    runCatching {
        context.startActivity(Intent.createChooser(sendIntent, "Share memory"))
    }
}

@Composable
private fun FullscreenImagePagerDialog(
    pages: List<FullscreenImagePage>,
    initialMemoryId: String,
    onDismiss: () -> Unit,
) {
    val startIndex = remember(pages, initialMemoryId) {
        pages.indexOfFirst { it.memoryId == initialMemoryId }.coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(initialPage = startIndex) { pages.size }
    var isCurrentZoomed by remember { mutableStateOf(false) }
    val viewerHazeState = rememberHazeState()
    val currentPage = pages.getOrNull(pagerState.currentPage) ?: pages.first()

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
                AsyncImage(
                    model = File(currentPage.imagePath),
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
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = !isCurrentZoomed,
                    modifier = Modifier.fillMaxSize(),
                ) { pageIndex ->
                    val page = pages[pageIndex]
                    ZoomableFullscreenImagePage(
                        imagePath = page.imagePath,
                        contentDescription = page.title,
                        isActivePage = pagerState.currentPage == pageIndex,
                        onZoomStateChanged = { zoomed ->
                            if (pagerState.currentPage == pageIndex) {
                                isCurrentZoomed = zoomed
                            }
                        },
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 22.dp)
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
                    ),
            ) {
                Text(
                    text = if (pages.size > 1) {
                        "${pagerState.currentPage + 1} / ${pages.size} · ${currentPage.title}"
                    } else {
                        currentPage.title
                    },
                    style = MindTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
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
                    contentDescription = "Close fullscreen",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun ZoomableFullscreenImagePage(
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
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y,
                ),
        )
    }
}
