package com.mind.app.core.designsystem.component

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision
import com.mind.app.core.designsystem.theme.MindTheme
import com.mind.app.core.model.AiProcessingStatus
import com.mind.app.core.model.ContentType
import com.mind.app.core.model.LayoutMode
import com.mind.app.core.model.Memory
import kotlinx.coroutines.delay
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Calm editorial card displaying a memory in true-aspect staggered masonry Grid or List mode.
 */
@Composable
fun MindMemoryCard(
    memory: Memory,
    layoutMode: LayoutMode,
    onClick: () -> Unit,
    onToggleFavourite: () -> Unit,
    modifier: Modifier = Modifier,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
) {
    val formattedDate = remember(memory.capturedAtEpochMillis) {
        formatCapturedDate(memory.capturedAtEpochMillis)
    }
    val selectionBorderColor = if (selected) MindTheme.colors.accent else null
    val selectionBorderWidth = if (selected) 2.dp else 0.8.dp
    val subtitleMeta = remember(memory.primaryCategory, memory.sourceAppName, formattedDate) {
        listOfNotNull(memory.primaryCategory?.name ?: memory.sourceAppName, formattedDate).joinToString(" · ")
    }

    val imagePath = remember(memory.originalFilePath, memory.thumbnailPath) {
        memory.originalFilePath?.takeIf { File(it).exists() } ?: memory.thumbnailPath
    }

    when (layoutMode) {
        LayoutMode.GRID -> {
            val hasImage = memory.contentType != ContentType.VOICE_NOTE && !imagePath.isNullOrBlank()
            val naturalRatio = remember(imagePath) { resolveImageAspectRatio(imagePath) }

            MindSurfaceCard(
                modifier = modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                onClick = onClick,
                onLongClick = onLongClick,
                borderColor = selectionBorderColor,
                borderWidth = selectionBorderWidth,
            ) {
                if (hasImage) {
                    val gridImageRequest = rememberCrispImageRequest(
                        path = imagePath!!,
                        targetWidthPx = 680,
                        targetHeightPx = 1200,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(naturalRatio)
                            .background(MindTheme.colors.surfaceSubtle),
                    ) {
                        AsyncImage(
                            model = gridImageRequest,
                            contentDescription = memory.displayTitle,
                            contentScale = ContentScale.Crop,
                            filterQuality = FilterQuality.High,
                            modifier = Modifier.fillMaxSize(),
                        )
                        if (selected) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MindTheme.colors.accent.copy(alpha = 0.16f)),
                            )
                        }
                        if (selectionMode) {
                            MindSelectionCheckBadge(
                                selected = selected,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(10.dp),
                            )
                        } else {
                            val favInteraction = remember { MutableInteractionSource() }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(10.dp)
                                    .size(30.dp)
                                    .dshPressableScale(favInteraction, pressedScale = 0.88f)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = if (memory.isFavourite) 0.48f else 0.28f))
                                    .clickable(
                                        interactionSource = favInteraction,
                                        indication = null,
                                        onClick = onToggleFavourite,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = if (memory.isFavourite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    contentDescription = if (memory.isFavourite) "Remove favourite" else "Favourite",
                                    tint = if (memory.isFavourite) MindTheme.colors.favourite else Color.White.copy(alpha = 0.92f),
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                } else {
                    NonImageGridPreview(
                        memory = memory,
                        onToggleFavourite = onToggleFavourite,
                        selectionMode = selectionMode,
                        selected = selected,
                    )
                }

                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = memory.displayTitle,
                        style = MindTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MindTheme.colors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AiStatusDot(status = memory.aiStatus)
                        Text(
                            text = subtitleMeta,
                            style = MindTheme.typography.labelSmall,
                            color = MindTheme.colors.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        LayoutMode.LIST -> {
            val colors = MindTheme.colors
            val hasImage = memory.contentType != ContentType.VOICE_NOTE && !imagePath.isNullOrBlank()
            val listSnippet = remember(
                memory.description,
                memory.userNotes,
                memory.extractedText,
                memory.originalUrl,
                memory.displayTitle,
            ) {
                listOfNotNull(
                    memory.description,
                    memory.userNotes,
                    memory.extractedText,
                    memory.originalUrl,
                )
                    .map { it.replace(Regex("\\s+"), " ").trim() }
                    .firstOrNull { it.isNotEmpty() && !it.equals(memory.displayTitle, ignoreCase = true) }
            }

            MindSurfaceCard(
                modifier = modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                onClick = onClick,
                onLongClick = onLongClick,
                borderColor = selectionBorderColor,
                borderWidth = selectionBorderWidth,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (selected) colors.accentSoft.copy(alpha = 0.26f) else Color.Transparent,
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (hasImage) {
                        val thumbShape = RoundedCornerShape(12.dp)
                        val listImageRequest = rememberCrispImageRequest(
                            path = imagePath!!,
                            targetWidthPx = 220,
                            targetHeightPx = 300,
                        )
                        Box(
                            modifier = Modifier
                                .size(width = 52.dp, height = 66.dp)
                                .clip(thumbShape)
                                .background(colors.surfaceSubtle)
                                .mindGlassEdge(
                                    shape = thumbShape,
                                    hairlineColor = colors.hairline,
                                    rimColor = colors.rimHighlight,
                                    strokeWidth = 0.6.dp,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            AsyncImage(
                                model = listImageRequest,
                                contentDescription = memory.displayTitle,
                                contentScale = ContentScale.Crop,
                                filterQuality = FilterQuality.High,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    } else {
                        val tintIdx = memory.contentType.tintIndex()
                        val idx = ((tintIdx % colors.identityContainers.size) + colors.identityContainers.size) % colors.identityContainers.size
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(colors.identityContainers[idx]),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (memory.contentType == ContentType.VOICE_NOTE) {
                                    Icons.Rounded.GraphicEq
                                } else {
                                    memory.contentType.icon()
                                },
                                contentDescription = null,
                                tint = colors.onIdentityContainers[idx],
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Text(
                            text = memory.displayTitle,
                            style = MindTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = colors.textPrimary,
                            maxLines = if (listSnippet != null) 1 else 2,
                            overflow = TextOverflow.Ellipsis,
                        )

                        if (listSnippet != null) {
                            Text(
                                text = listSnippet,
                                style = MindTheme.typography.bodySmall,
                                color = colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AiStatusDot(status = memory.aiStatus)
                            Text(
                                text = subtitleMeta,
                                style = MindTheme.typography.labelSmall,
                                color = colors.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    if (selectionMode) {
                        MindSelectionCheckBadge(selected = selected)
                    } else {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onToggleFavourite),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (memory.isFavourite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = if (memory.isFavourite) "Remove favourite" else "Favourite",
                                tint = if (memory.isFavourite) colors.favourite else colors.textMuted,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AiStatusDot(status: AiProcessingStatus) {
    val dotColor = when (status) {
        AiProcessingStatus.PENDING, AiProcessingStatus.PROCESSING -> MindTheme.colors.warning
        AiProcessingStatus.FAILED -> MindTheme.colors.danger
        AiProcessingStatus.COMPLETED,
        AiProcessingStatus.SKIPPED_LOCAL_ONLY,
        AiProcessingStatus.CANCELLED,
        -> return
    }
    Box(
        modifier = Modifier
            .size(7.dp)
            .clip(CircleShape)
            .background(dotColor),
    )
}

private fun resolveImageAspectRatio(imagePath: String?): Float {
    if (imagePath.isNullOrBlank()) return 0.80f
    val file = File(imagePath)
    if (!file.exists()) return 0.80f
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(imagePath, options)
    val w = options.outWidth
    val h = options.outHeight
    if (w <= 0 || h <= 0) return 0.80f
    return (w.toFloat() / h.toFloat()).coerceIn(0.62f, 1.55f)
}

@Composable
private fun NonImageGridPreview(
    memory: Memory,
    onToggleFavourite: () -> Unit,
    selectionMode: Boolean = false,
    selected: Boolean = false,
) {
    val colors = MindTheme.colors
    val snippet = memory.description ?: memory.extractedText ?: memory.originalUrl ?: memory.userNotes
    val isCode = memory.contentType == ContentType.ERROR_LOG
    val isVoice = memory.contentType == ContentType.VOICE_NOTE
    val isQuote = memory.contentType == ContentType.QUOTE

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                when {
                    selected -> colors.accentSoft.copy(alpha = 0.45f)
                    isCode -> if (colors.isDark) Color(0xFF141824) else Color(0xFFF0F3FA)
                    isVoice -> colors.accentSoft.copy(alpha = 0.5f)
                    else -> colors.surfaceSubtle.copy(alpha = 0.72f)
                },
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(colors.accentSoft, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isVoice) Icons.Rounded.GraphicEq else memory.contentType.icon(),
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(17.dp),
                )
            }
            if (selectionMode) {
                MindSelectionCheckBadge(selected = selected)
            } else {
                val favInteraction = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .dshPressableScale(favInteraction, pressedScale = 0.88f)
                        .clip(CircleShape)
                        .background(colors.surface.copy(alpha = 0.85f))
                        .clickable(
                            interactionSource = favInteraction,
                            indication = null,
                            onClick = onToggleFavourite,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (memory.isFavourite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = if (memory.isFavourite) "Remove favourite" else "Favourite",
                        tint = if (memory.isFavourite) colors.favourite else colors.textMuted,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        if (!snippet.isNullOrBlank()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isQuote) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(48.dp)
                            .clip(CircleShape)
                            .background(colors.accent),
                    )
                }
                Text(
                    text = snippet,
                    style = if (isCode) {
                        MindTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                    } else {
                        MindTheme.typography.bodySmall
                    },
                    color = colors.textSecondary,
                    maxLines = if (isQuote) 4 else 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun rememberCrispImageRequest(
    path: String,
    targetWidthPx: Int,
    targetHeightPx: Int,
): ImageRequest {
    val context = LocalContext.current
    return remember(context, path, targetWidthPx, targetHeightPx) {
        ImageRequest.Builder(context)
            .data(File(path))
            .size(targetWidthPx, targetHeightPx)
            .precision(Precision.INEXACT)
            .crossfade(true)
            .build()
    }
}

private fun ContentType.tintIndex(): Int = when (this) {
    ContentType.SCREENSHOT, ContentType.IMAGE -> 0
    ContentType.VOICE_NOTE -> 1
    ContentType.URL -> 7
    ContentType.QUOTE -> 2
    ContentType.PRODUCT -> 4
    ContentType.PLACE -> 6
    ContentType.ERROR_LOG -> 3
    ContentType.DOCUMENT, ContentType.TEXT -> 5
    ContentType.OTHER -> 0
}

fun ContentType.icon(): ImageVector = when (this) {
    ContentType.SCREENSHOT, ContentType.IMAGE -> Icons.Rounded.Image
    ContentType.VOICE_NOTE -> Icons.Rounded.Mic
    ContentType.URL -> Icons.Rounded.Link
    ContentType.QUOTE -> Icons.Rounded.FormatQuote
    ContentType.PRODUCT -> Icons.Rounded.ShoppingBag
    ContentType.PLACE -> Icons.Rounded.Place
    ContentType.ERROR_LOG -> Icons.Rounded.Code
    ContentType.DOCUMENT, ContentType.TEXT -> Icons.Rounded.Description
    ContentType.OTHER -> Icons.Rounded.BookmarkBorder
}

private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")

private fun formatCapturedDate(epochMillis: Long): String {
    return runCatching {
        Instant.ofEpochMilli(epochMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(DATE_FORMATTER)
    }.getOrDefault("")
}

/**
 * Full-bleed editorial collage tile for the zoomable Home mosaic.
 */
@Composable
fun MindCollageTile(
    memory: Memory,
    tileIndex: Int,
    columns: Int,
    onClick: () -> Unit,
    onToggleFavourite: () -> Unit,
    modifier: Modifier = Modifier,
    isHeroSpan: Boolean = false,
    selectionMode: Boolean = false,
    selected: Boolean = false,
    onLongClick: (() -> Unit)? = null,
) {
    val colors = MindTheme.colors
    val imagePath = remember(memory.originalFilePath, memory.thumbnailPath) {
        memory.originalFilePath?.takeIf { File(it).exists() } ?: memory.thumbnailPath
    }
    val hasImage = memory.contentType != ContentType.VOICE_NOTE && !imagePath.isNullOrBlank()
    val naturalRatio = remember(imagePath) { resolveImageAspectRatio(imagePath) }

    val collageRatio = remember(naturalRatio, tileIndex, columns, isHeroSpan) {
        when {
            isHeroSpan -> 1.48f
            columns <= 1 -> naturalRatio.coerceIn(0.68f, 1.32f)
            columns == 2 -> {
                val pattern = when (tileIndex % 6) {
                    0 -> 0.74f
                    1 -> 1.02f
                    2 -> 0.86f
                    3 -> 0.70f
                    4 -> 1.12f
                    else -> 0.82f
                }
                (naturalRatio * 0.55f + pattern * 0.45f).coerceIn(0.66f, 1.28f)
            }
            columns == 3 -> {
                val pattern = when (tileIndex % 5) {
                    0 -> 0.76f
                    1 -> 1.0f
                    2 -> 0.84f
                    3 -> 1.10f
                    else -> 0.78f
                }
                (naturalRatio * 0.45f + pattern * 0.55f).coerceIn(0.68f, 1.22f)
            }
            else -> if (tileIndex % 3 == 0) 0.85f else 1.0f
        }
    }

    var previousColumns by remember { mutableIntStateOf(columns) }
    var tileBounceTarget by remember { mutableFloatStateOf(1f) }
    LaunchedEffect(columns) {
        if (columns != previousColumns) {
            val shrinking = columns > previousColumns
            previousColumns = columns
            delay((tileIndex % 6) * 16L)
            tileBounceTarget = if (shrinking) 0.89f else 1.08f
            delay(75L)
            tileBounceTarget = 1f
        }
    }
    val animatedTileScale by animateFloatAsState(
        targetValue = tileBounceTarget,
        animationSpec = spring(dampingRatio = 0.54f, stiffness = 310f),
        label = "collage_tile_bounce",
    )

    val targetCornerRadius = when {
        columns <= 1 -> 22.dp
        columns == 2 -> 18.dp
        columns == 3 -> 14.dp
        else -> 10.dp
    }
    val animatedCorner by animateDpAsState(
        targetValue = targetCornerRadius,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 320f),
        label = "collage_tile_corner",
    )
    val animatedRatio by animateFloatAsState(
        targetValue = collageRatio,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 280f),
        label = "collage_tile_ratio",
    )
    val shape = RoundedCornerShape(animatedCorner)
    val selectionBorderColor = if (selected) colors.accent else null
    val selectionBorderWidth = if (selected) 2.dp else 0.75.dp

    MindSurfaceCard(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = animatedTileScale
                scaleY = animatedTileScale
            },
        shape = shape,
        onClick = onClick,
        onLongClick = onLongClick,
        borderColor = selectionBorderColor,
        borderWidth = selectionBorderWidth,
    ) {
        if (hasImage) {
            val targetPx = when {
                isHeroSpan || columns <= 1 -> 1080
                columns == 2 -> 720
                columns == 3 -> 520
                else -> 380
            }
            val imageRequest = rememberCrispImageRequest(
                path = imagePath!!,
                targetWidthPx = targetPx,
                targetHeightPx = (targetPx / collageRatio).toInt().coerceIn(320, 1440),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(animatedRatio.coerceIn(0.55f, 1.65f))
                    .background(colors.surfaceSubtle),
            ) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = memory.displayTitle,
                    contentScale = ContentScale.Crop,
                    filterQuality = FilterQuality.High,
                    modifier = Modifier.fillMaxSize(),
                )

                androidx.compose.animation.AnimatedVisibility(
                    visible = columns <= 2 || isHeroSpan,
                    enter = fadeIn(tween(220)),
                    exit = fadeOut(tween(150)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                ) {
                    val darkAccentMid = Color(
                        red = (colors.accent.red * 0.28f).coerceIn(0f, 1f),
                        green = (colors.accent.green * 0.28f).coerceIn(0f, 1f),
                        blue = (colors.accent.blue * 0.32f).coerceIn(0f, 1f),
                        alpha = 1f,
                    )
                    val darkAccentDeep = Color(
                        red = (colors.accent.red * 0.16f).coerceIn(0f, 1f),
                        green = (colors.accent.green * 0.16f).coerceIn(0f, 1f),
                        blue = (colors.accent.blue * 0.20f).coerceIn(0f, 1f),
                        alpha = 1f,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    0.0f to Color.Transparent,
                                    0.34f to darkAccentMid.copy(alpha = 0.54f),
                                    0.70f to darkAccentMid.copy(alpha = 0.86f),
                                    1.0f to darkAccentDeep.copy(alpha = 0.96f),
                                ),
                            )
                            .padding(start = 12.dp, end = 12.dp, top = 24.dp, bottom = 10.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            memory.primaryCategory?.name?.let { catName ->
                                Text(
                                    text = catName.uppercase(),
                                    style = MindTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White.copy(alpha = 0.84f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Text(
                                text = memory.displayTitle,
                                style = MindTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                if (selected) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colors.accent.copy(alpha = 0.22f)),
                    )
                }

                if (selectionMode) {
                    MindSelectionCheckBadge(
                        selected = selected,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(if (columns >= 4) 6.dp else 8.dp),
                    )
                } else {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = columns <= 2 || memory.isFavourite,
                        enter = fadeIn(tween(180)),
                        exit = fadeOut(tween(140)),
                        modifier = Modifier.align(Alignment.TopEnd),
                    ) {
                        val favInteraction = remember { MutableInteractionSource() }
                        Box(
                            modifier = Modifier
                                .padding(8.dp)
                                .size(if (columns >= 3) 24.dp else 28.dp)
                                .dshPressableScale(favInteraction, pressedScale = 0.88f)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = if (memory.isFavourite) 0.50f else 0.28f))
                                .clickable(
                                    interactionSource = favInteraction,
                                    indication = null,
                                    onClick = onToggleFavourite,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (memory.isFavourite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = if (memory.isFavourite) "Remove favourite" else "Favourite",
                                tint = if (memory.isFavourite) colors.favourite else Color.White.copy(alpha = 0.90f),
                                modifier = Modifier.size(if (columns >= 3) 13.dp else 15.dp),
                            )
                        }
                    }
                }
            }
        } else {
            val snippet = memory.description ?: memory.extractedText ?: memory.originalUrl ?: memory.userNotes
            val isVoice = memory.contentType == ContentType.VOICE_NOTE
            val animatedTextCardRatio by animateFloatAsState(
                targetValue = if (columns >= 3) 1f else 0.92f,
                animationSpec = spring(dampingRatio = 0.78f, stiffness = 340f),
                label = "collage_text_ratio",
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(animatedTextCardRatio)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                colors.accentSoft.copy(alpha = if (colors.isDark) 0.42f else 0.65f),
                                colors.surfaceSubtle,
                            ),
                        ),
                    )
                    .padding(if (columns >= 3) 8.dp else 12.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (columns >= 3) 24.dp else 30.dp)
                                .background(colors.accent.copy(alpha = 0.16f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (isVoice) Icons.Rounded.GraphicEq else memory.contentType.icon(),
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(if (columns >= 3) 13.dp else 16.dp),
                            )
                        }
                        if (selectionMode) {
                            MindSelectionCheckBadge(selected = selected)
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = memory.displayTitle,
                            style = (if (columns >= 3) MindTheme.typography.labelSmall else MindTheme.typography.titleSmall)
                                .copy(fontWeight = FontWeight.SemiBold),
                            color = colors.textPrimary,
                            maxLines = if (columns >= 3) 2 else 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (columns <= 2 && !snippet.isNullOrBlank()) {
                            Text(
                                text = snippet,
                                style = MindTheme.typography.bodySmall,
                                color = colors.textSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}
