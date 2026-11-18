package com.mind.app.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mind.app.core.designsystem.theme.MindTheme
import com.mind.app.core.model.AiProcessingStatus

/**
 * Translucent Mist-Glass card surface following dsh-go's three laws:
 * material contrast over heavy elevation shadows, hairline + top-edge luminous rim skeleton,
 * and smooth spring press scale feedback.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MindSurfaceCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    borderColor: Color? = null,
    borderWidth: Dp = 0.75.dp,
    containerColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MindTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val hasInteraction = onClick != null || onLongClick != null
    val indication = LocalIndication.current
    val clickModifier = when {
        onLongClick != null -> Modifier.combinedClickable(
            interactionSource = interactionSource,
            indication = indication,
            onClick = onClick ?: {},
            onLongClick = onLongClick,
        )
        onClick != null -> Modifier.clickable(
            interactionSource = interactionSource,
            indication = indication,
            onClick = onClick,
        )
        else -> Modifier
    }

    val explicitBorder = borderColor?.let { BorderStroke(borderWidth, it) }
    val fill = containerColor ?: colors.glassCardFill

    Surface(
        modifier = modifier
            .dshPressableScale(interactionSource = interactionSource, enabled = hasInteraction)
            .clip(shape)
            .then(clickModifier)
            .then(
                if (borderColor == null) {
                    Modifier.mindGlassEdge(
                        shape = shape,
                        hairlineColor = colors.hairline,
                        rimColor = colors.rimHighlight,
                        strokeWidth = 0.65.dp,
                    )
                } else {
                    Modifier
                },
            ),
        shape = shape,
        color = fill,
        contentColor = colors.textPrimary,
        border = explicitBorder,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape),
            content = content,
        )
    }
}

/**
 * 44dp circular frosted chrome button matching dsh-go's GlassIconButton in top bars.
 */
@Composable
fun MindGlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 20.dp,
    tint: Color = MindTheme.colors.textPrimary,
    tier: MindGlassTier = MindGlassTier.CHROME,
) {
    val colors = MindTheme.colors
    val hazeState = LocalMindHazeState.current
    val interactionSource = remember { MutableInteractionSource() }
    val useBlur = hazeState != null && tier != MindGlassTier.PILL
    val fill = when (tier) {
        MindGlassTier.CHROME -> if (useBlur) Color.Transparent else colors.glassChromeFallback
        MindGlassTier.SHEET -> if (useBlur) Color.Transparent else colors.glassSheetFill
        MindGlassTier.CARD -> colors.glassCardFill
        MindGlassTier.PILL -> colors.glassPillFill
    }

    Surface(
        modifier = modifier
            .size(size)
            .dshPressableScale(interactionSource = interactionSource, pressedScale = 0.94f)
            .then(
                if (useBlur) {
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
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
            )
            .mindGlassEdge(
                shape = CircleShape,
                hairlineColor = colors.hairline,
                rimColor = colors.rimHighlight,
                strokeWidth = 0.65.dp,
            ),
        shape = CircleShape,
        color = fill,
        contentColor = tint,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

/**
 * dsh-go AppPage floating chrome bar: compact 56dp height with 44dp circular glass Back button,
 * centered 1-line title fading cleanly in on scroll, feathered top backdrop veil, and trailing actions.
 */
@Composable
fun MindFloatingPageTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    scrollFraction: Float = 0f,
    alwaysShowTitle: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = MindTheme.colors
    val hazeState = LocalMindHazeState.current
    val f = scrollFraction.coerceIn(0f, 1f)
    val titleAlpha = when {
        title.isBlank() -> 0f
        alwaysShowTitle -> 1f
        else -> ((f - 0.15f) / 0.45f).coerceIn(0f, 1f)
    }
    val titleShiftPx = (1f - titleAlpha) * 14f
    val statusTopInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(modifier = modifier.fillMaxWidth()) {
        MindEdgeBlurVeil(
            hazeState = hazeState,
            fromTop = true,
            height = statusTopInset + 42.dp,
            tintAlpha = 0.14f + 0.12f * f,
        )

        Layout(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            content = {
                MindGlassIconButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    onClick = onBack,
                )

                Box(
                    modifier = Modifier.graphicsLayer {
                        alpha = titleAlpha
                        translationY = titleShiftPx
                    },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = title,
                        style = MindTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions,
                )
            },
        ) { measurables, constraints ->
            val relaxed = constraints.copy(minWidth = 0, minHeight = 0)
            val leading = measurables[0].measure(relaxed)
            val trailing = measurables[2].measure(relaxed)
            val gapPx = 8.dp.roundToPx()
            val maxTitleW = (constraints.maxWidth - leading.width - trailing.width - gapPx * 2)
                .coerceAtLeast(0)
            val titlePlaceable = measurables[1].measure(
                relaxed.copy(maxWidth = maxTitleW),
            )
            val barH = maxOf(leading.height, titlePlaceable.height, trailing.height, constraints.minHeight)
            layout(constraints.maxWidth, barH) {
                leading.placeRelative(0, (barH - leading.height) / 2)
                trailing.placeRelative(constraints.maxWidth - trailing.width, (barH - trailing.height) / 2)

                val minCenterX = leading.width + gapPx
                val maxCenterX = (constraints.maxWidth - trailing.width - gapPx - titlePlaceable.width)
                    .coerceAtLeast(minCenterX)
                val preferredCenterX = (constraints.maxWidth - titlePlaceable.width) / 2
                val titleX = preferredCenterX.coerceIn(minCenterX, maxCenterX)
                titlePlaceable.placeRelative(titleX, (barH - titlePlaceable.height) / 2)
            }
        }
    }
}

/**
 * 32dp circular tinted pastel icon disc matching dsh-go SettingsIcon identity colors.
 */
@Composable
fun MindPastelIconDisc(
    icon: ImageVector,
    tintIndex: Int,
    modifier: Modifier = Modifier,
    isDanger: Boolean = false,
    size: Dp = 32.dp,
    iconSize: Dp = 18.dp,
) {
    val colors = MindTheme.colors
    val idx = ((tintIndex % colors.identityContainers.size) + colors.identityContainers.size) % colors.identityContainers.size
    val bg = if (isDanger) colors.dangerSoft else colors.identityContainers[idx]
    val fg = if (isDanger) colors.danger else colors.onIdentityContainers[idx]
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.size(iconSize),
        )
    }
}

/**
 * Compact 1-line folder/category/space breadcrumb pill that never wraps or expands vertically
 * even with very long names.
 */
@Composable
fun MindCompactPathPill(
    segments: List<String>,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val cleanSegments = segments.map { it.trim() }.filter { it.isNotEmpty() }
    if (cleanSegments.isEmpty()) return
    val colors = MindTheme.colors
    val text = cleanSegments.joinToString("  /  ")
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = modifier
            .dshPressableScale(interactionSource = interactionSource, enabled = onClick != null)
            .clip(CircleShape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .mindGlassEdge(
                shape = CircleShape,
                hairlineColor = colors.hairline,
                rimColor = colors.rimHighlight,
                strokeWidth = 0.6.dp,
            ),
        shape = CircleShape,
        color = colors.glassPillFill,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.FolderOpen,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = text,
                style = MindTheme.typography.labelMedium,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Clean dsh-go editorial screen header with capped title lines so long dynamic labels
 * never push page content down off screen.
 */
@Composable
fun OneUiExpandedHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = MindTheme.spacing.screenHorizontal,
                end = MindTheme.spacing.screenHorizontal,
                top = 6.dp,
                bottom = 4.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (navigationIcon != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                navigationIcon()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    content = actions,
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
                Text(
                    text = title,
                    style = MindTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MindTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MindTheme.typography.bodySmall,
                        color = MindTheme.colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (navigationIcon == null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    content = actions,
                )
            }
        }
    }
}

/**
 * Inset hairline divider styled identically to dsh-go grouped list & settings cards.
 */
@Composable
fun OneUiListDivider(
    modifier: Modifier = Modifier,
    insetStart: Boolean = false,
) {
    HorizontalDivider(
        modifier = modifier.padding(
            start = if (insetStart) 60.dp else 16.dp,
            end = 16.dp,
        ),
        thickness = 0.6.dp,
        color = MindTheme.colors.hairline,
    )
}

/**
 * Quiet tracked eyebrow section label placed above grouped cards (dsh-go PageSectionLabel style).
 */
@Composable
fun OneUiSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MindTheme.typography.labelLarge.copy(letterSpacing = 0.3.sp),
        color = MindTheme.colors.textSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/**
 * Refined pill toggle matching dsh-go switch track proportions.
 */
@Composable
fun OneUiSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MindTheme.colors
    val trackColor by animateColorAsState(
        targetValue = if (checked) colors.accent else colors.textMuted.copy(alpha = 0.34f),
        animationSpec = tween(180),
        label = "oneui_switch_track",
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 21.dp else 3.dp,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 480f),
        label = "oneui_switch_thumb",
    )
    val knobColor = if (checked) colors.onAccent else Color.White

    Box(
        modifier = modifier
            .size(width = 48.dp, height = 28.dp)
            .clip(CircleShape)
            .background(trackColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onCheckedChange(!checked) },
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(22.dp)
                .background(knobColor, CircleShape),
        )
    }
}

/**
 * Grouped row following dsh-go SettingsMetrics: 32dp pastel icon disc, 12dp gap, concise titles, trailing chevron.
 */
@Composable
fun OneUiChevronRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = MindTheme.colors.accent,
    tintIndex: Int? = null,
    onClick: () -> Unit,
) {
    val colors = MindTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (icon != null) {
            if (tintIndex != null) {
                MindPastelIconDisc(icon = icon, tintIndex = tintIndex)
            } else {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(iconTint.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = title,
                style = MindTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 21.sp),
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MindTheme.typography.bodySmall,
                    color = colors.textMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.textMuted,
            modifier = Modifier.size(18.dp),
        )
    }
}

/**
 * dsh-go SearchCapsuleButton: filled stadium capsule search bar, no heavy border, hairline accent.
 */
@Composable
fun MindSearchTriggerBar(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search or ask Mind...",
) {
    val colors = MindTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val fill = if (colors.isDark) Color.White.copy(alpha = 0.07f) else colors.glassCardFill

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .dshPressableScale(interactionSource = interactionSource)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
            )
            .mindGlassEdge(
                shape = CircleShape,
                hairlineColor = colors.hairline,
                rimColor = colors.rimHighlight,
                strokeWidth = 0.6.dp,
            ),
        shape = CircleShape,
        color = fill,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(width = 46.dp, height = 46.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search",
                    tint = colors.textMuted,
                    modifier = Modifier.size(19.dp),
                )
            }
            Text(
                text = placeholder,
                style = MindTheme.typography.bodyMedium,
                color = colors.textMuted,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Box(
                modifier = Modifier.size(width = 46.dp, height = 46.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = colors.accent.copy(alpha = 0.85f),
                    modifier = Modifier.size(17.dp),
                )
            }
        }
    }
}

/**
 * dsh-go SegmentedPill: stadium track with sliding pill thumb driven by spring physics.
 */
@Composable
fun <T> MindSegmentedPillBar(
    items: List<T>,
    selectedItem: T,
    labelProvider: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    val colors = MindTheme.colors
    val selectedIndex = items.indexOf(selectedItem).coerceAtLeast(0)
    val trackColor = if (colors.isDark) Color.White.copy(alpha = 0.07f) else colors.shadowInk.copy(alpha = 0.06f)
    val thumbColor = if (colors.isDark) Color.White.copy(alpha = 0.18f) else Color.White

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(CircleShape)
            .background(trackColor)
            .padding(3.dp),
    ) {
        val segmentWidth = (maxWidth / items.size).coerceAtLeast(1.dp)
        val thumbOffset by animateDpAsState(
            targetValue = segmentWidth * selectedIndex,
            animationSpec = spring(dampingRatio = 0.78f, stiffness = 420f),
            label = "segmentedThumbOffset",
        )

        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .width(segmentWidth)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(thumbColor)
                .mindGlassEdge(
                    shape = CircleShape,
                    hairlineColor = colors.hairline,
                    rimColor = if (colors.isDark) colors.rimHighlight else Color.White,
                    strokeWidth = 0.5.dp,
                ),
        )

        Row(modifier = Modifier.fillMaxSize()) {
            items.forEachIndexed { index, item ->
                val selected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelect(item) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = labelProvider(item),
                        style = MindTheme.typography.labelLarge.copy(
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        ),
                        color = if (selected) colors.textPrimary else colors.textSecondary,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun MindTagPill(
    label: String,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
) {
    val colors = MindTheme.colors
    val bg = if (highlighted) colors.accentSoft else colors.glassPillFill
    val fg = if (highlighted) colors.accent else colors.textSecondary

    Surface(
        modifier = modifier.mindGlassEdge(
            shape = CircleShape,
            hairlineColor = if (highlighted) colors.accent.copy(alpha = 0.18f) else colors.hairline,
            rimColor = if (highlighted) Color.Transparent else colors.rimHighlight,
            strokeWidth = 0.5.dp,
        ),
        shape = CircleShape,
        color = bg,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Text(
            text = label,
            style = MindTheme.typography.labelMedium,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
        )
    }
}

@Composable
fun MindProcessingBadge(
    status: AiProcessingStatus,
    modifier: Modifier = Modifier,
) {
    val colors = MindTheme.colors
    val (label, icon, tint, bg) = when (status) {
        AiProcessingStatus.PENDING -> Quad(
            "Queued",
            Icons.Rounded.Schedule,
            colors.textSecondary,
            colors.glassPillFill,
        )
        AiProcessingStatus.PROCESSING -> Quad(
            "Analyzing",
            Icons.Rounded.AutoAwesome,
            colors.accent,
            colors.accentSoft,
        )
        AiProcessingStatus.FAILED -> Quad(
            "Retry AI",
            Icons.Rounded.ErrorOutline,
            colors.warning,
            colors.warningSoft,
        )
        AiProcessingStatus.SKIPPED_LOCAL_ONLY -> Quad(
            "Local only",
            Icons.Rounded.Lock,
            colors.textMuted,
            colors.glassPillFill,
        )
        AiProcessingStatus.CANCELLED -> Quad(
            "Paused",
            Icons.Rounded.Schedule,
            colors.textMuted,
            colors.glassPillFill,
        )
        AiProcessingStatus.COMPLETED -> return
    }

    Surface(
        modifier = modifier.mindGlassEdge(
            shape = CircleShape,
            hairlineColor = tint.copy(alpha = 0.20f),
            rimColor = Color.Transparent,
            strokeWidth = 0.5.dp,
        ),
        shape = CircleShape,
        color = bg,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(12.dp),
            )
            Text(
                text = label,
                style = MindTheme.typography.labelSmall,
                color = tint,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun MindEmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    MindSurfaceCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            MindLogoMark(size = 56.dp, accentColor = MindTheme.colors.accent)
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = title,
                style = MindTheme.typography.titleLarge,
                color = MindTheme.colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MindTheme.typography.bodyMedium,
                color = MindTheme.colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            if (action != null) {
                Spacer(modifier = Modifier.height(20.dp))
                action()
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

/**
 * Floating translucent selection header matching dsh-go glass chrome.
 */
@Composable
fun MindSelectionTopBar(
    selectedCount: Int,
    allSelected: Boolean,
    onClose: () -> Unit,
    onToggleSelectAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MindTheme.colors
    val hazeState = LocalMindHazeState.current
    val shape = RoundedCornerShape(22.dp)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MindTheme.spacing.screenHorizontal, vertical = 10.dp)
            .then(
                if (hazeState != null) {
                    Modifier.mindGlassBackdrop(shape = shape, hazeState = hazeState, blurRadius = 18.dp)
                } else {
                    Modifier.clip(shape)
                },
            )
            .mindGlassEdge(
                shape = shape,
                hairlineColor = colors.hairline,
                rimColor = colors.rimHighlight,
            ),
        shape = shape,
        color = if (hazeState != null) Color.Transparent else colors.glassChromeFallback,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Exit selection",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(
                    text = if (selectedCount == 0) "Select items" else "$selectedCount selected",
                    style = MindTheme.typography.titleMedium,
                    color = colors.textPrimary,
                )
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (allSelected) colors.accent else colors.accentSoft)
                    .clickable(onClick = onToggleSelectAll),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DoneAll,
                        contentDescription = null,
                        tint = if (allSelected) colors.onAccent else colors.accent,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = if (allSelected) "None" else "All",
                        style = MindTheme.typography.labelMedium,
                        color = if (allSelected) colors.onAccent else colors.accent,
                    )
                }
            }
        }
    }
}

/**
 * Floating pill dock anchored cleanly above navigation chrome for bulk actions on selected saves.
 */
@Composable
fun MindSelectionActionDock(
    selectedCount: Int,
    onDeleteSelected: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
) {
    val colors = MindTheme.colors
    val hazeState = LocalMindHazeState.current
    Surface(
        modifier = modifier
            .then(
                if (hazeState != null) {
                    Modifier.mindGlassBackdrop(
                        shape = CircleShape,
                        hazeState = hazeState,
                        blurRadius = 20.dp,
                    )
                } else {
                    Modifier.clip(CircleShape)
                },
            )
            .mindGlassEdge(
                shape = CircleShape,
                hairlineColor = colors.hairline,
                rimColor = colors.rimHighlight,
            ),
        shape = CircleShape,
        color = if (hazeState != null) Color.Transparent else colors.glassChromeFallback,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val secondaryLabel = secondaryActionLabel
            if (secondaryLabel != null && onSecondaryAction != null) {
                MindPillButton(
                    text = secondaryLabel,
                    onClick = onSecondaryAction,
                    enabled = selectedCount > 0,
                    isPrimary = false,
                    compact = true,
                )
            }

            MindPillButton(
                text = if (selectedCount > 0) "Delete ($selectedCount)" else "Delete",
                onClick = onDeleteSelected,
                enabled = selectedCount > 0,
                isPrimary = true,
                isDanger = true,
                compact = true,
                leadingIcon = Icons.Rounded.DeleteOutline,
            )
        }
    }
}

@Composable
fun MindDeleteSelectedDialog(
    selectedCount: Int,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit,
) {
    val noun = if (selectedCount == 1) "memory" else "memories"
    MindGlassDialog(
        onDismissRequest = onDismiss,
        title = "Delete $selectedCount $noun?",
        subtitle = "This permanently deletes the selected $noun and removes their saved media files from this device.",
        actions = {
            MindPillButton(
                text = "Cancel",
                onClick = onDismiss,
                isPrimary = false,
                compact = true,
            )
            MindPillButton(
                text = "Delete",
                onClick = onConfirmDelete,
                isPrimary = true,
                isDanger = true,
                compact = true,
            )
        },
    )
}

/**
 * Tactile stadium Mist-Glass pill button replacing boxy Material3 filled and outlined buttons.
 */
@Composable
fun MindPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPrimary: Boolean = true,
    isDanger: Boolean = false,
    isLoading: Boolean = false,
    compact: Boolean = false,
    leadingIcon: ImageVector? = null,
) {
    val colors = MindTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val fill = when {
        !enabled -> colors.glassPillFill
        isDanger && isPrimary -> colors.danger
        isPrimary -> colors.accent
        else -> colors.glassCardFill
    }
    val fg = when {
        !enabled -> colors.textMuted
        isDanger && isPrimary -> Color.White
        isDanger -> colors.danger
        isPrimary -> colors.onAccent
        else -> colors.textPrimary
    }
    val hairline = when {
        isPrimary -> Color.Transparent
        isDanger -> colors.danger.copy(alpha = 0.28f)
        else -> colors.hairline
    }
    val rim = when {
        isPrimary && enabled -> Color.White.copy(alpha = 0.28f)
        isPrimary -> Color.Transparent
        else -> colors.rimHighlight
    }

    Surface(
        modifier = modifier
            .heightIn(min = if (compact) 36.dp else 44.dp)
            .dshPressableScale(interactionSource = interactionSource, enabled = enabled && !isLoading, pressedScale = 0.96f)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                enabled = enabled && !isLoading,
                onClick = onClick,
            )
            .mindGlassEdge(
                shape = CircleShape,
                hairlineColor = hairline,
                rimColor = rim,
                strokeWidth = 0.65.dp,
            ),
        shape = CircleShape,
        color = fill,
        contentColor = fg,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 14.dp else 18.dp,
                vertical = if (compact) 8.dp else 11.dp,
            ),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(if (compact) 14.dp else 16.dp),
                    strokeWidth = 2.dp,
                    color = fg,
                )
                Spacer(modifier = Modifier.width(7.dp))
            } else if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(if (compact) 15.dp else 17.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = (if (compact) MindTheme.typography.labelMedium else MindTheme.typography.labelLarge).copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Translucent Mist-Glass text input field with hairline highlight and zero Material border cutouts.
 */
@Composable
fun MindGlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else 6,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val colors = MindTheme.colors
    val shape = RoundedCornerShape(16.dp)
    val fill = if (colors.isDark) Color.White.copy(alpha = 0.06f) else colors.surfaceSubtle

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (!label.isNullOrBlank()) {
            Text(
                text = label,
                style = MindTheme.typography.labelMedium,
                color = colors.textSecondary,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .mindGlassEdge(
                    shape = shape,
                    hairlineColor = colors.hairline,
                    rimColor = colors.rimHighlight,
                    strokeWidth = 0.65.dp,
                ),
            shape = shape,
            color = fill,
            contentColor = colors.textPrimary,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart,
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MindTheme.typography.bodyMedium,
                            color = colors.textMuted,
                            maxLines = if (singleLine) 1 else 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        singleLine = singleLine,
                        minLines = minLines,
                        maxLines = maxLines,
                        visualTransformation = visualTransformation,
                        keyboardOptions = keyboardOptions,
                        keyboardActions = keyboardActions,
                        textStyle = MindTheme.typography.bodyMedium.copy(color = colors.textPrimary),
                        cursorBrush = SolidColor(colors.accent),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (trailingContent != null) {
                    trailingContent()
                }
            }
        }
    }
}

/**
 * Custom Mist-Glass selection circle replacing raw Material RadioButtons.
 */
@Composable
fun MindGlassSelectionCheck(
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MindTheme.colors
    val fill by animateColorAsState(
        targetValue = if (selected) colors.accent else Color.Transparent,
        animationSpec = tween(durationMillis = 150),
        label = "glassCheckBg",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) colors.accent else colors.textMuted.copy(alpha = 0.45f),
        animationSpec = tween(durationMillis = 150),
        label = "glassCheckBorder",
    )
    Box(
        modifier = modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(fill)
            .border(width = 1.4.dp, color = borderColor, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Selected",
                tint = colors.onAccent,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

/**
 * Capsule selectable Mist-Glass chip replacing Material FilterChip rectangles.
 */
@Composable
fun MindSelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    val colors = MindTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val bg by animateColorAsState(
        targetValue = if (selected) colors.accentSoft else colors.glassPillFill,
        animationSpec = tween(150),
        label = "chipBg",
    )
    val fg by animateColorAsState(
        targetValue = if (selected) colors.accent else colors.textSecondary,
        animationSpec = tween(150),
        label = "chipFg",
    )

    Surface(
        modifier = modifier
            .dshPressableScale(interactionSource = interactionSource, pressedScale = 0.95f)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
            )
            .mindGlassEdge(
                shape = CircleShape,
                hairlineColor = if (selected) colors.accent.copy(alpha = 0.35f) else colors.hairline,
                rimColor = if (selected) Color.Transparent else colors.rimHighlight,
                strokeWidth = 0.6.dp,
            ),
        shape = CircleShape,
        color = bg,
        contentColor = fg,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(14.dp),
                )
            }
            Text(
                text = label,
                style = MindTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                ),
                color = fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Floating Mist-Glass dialog replacing blocky Material3 AlertDialog across Settings and collections.
 */
@Composable
fun MindGlassDialog(
    onDismissRequest: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit,
) {
    val colors = MindTheme.colors
    val shape = RoundedCornerShape(28.dp)
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 22.dp,
                    shape = shape,
                    ambientColor = colors.shadowInk,
                    spotColor = colors.shadowInk,
                )
                .mindGlassEdge(
                    shape = shape,
                    hairlineColor = colors.hairline,
                    rimColor = colors.rimHighlight,
                    strokeWidth = 0.75.dp,
                ),
            shape = shape,
            color = colors.glassChromeFallback,
            contentColor = colors.textPrimary,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = title,
                    style = MindTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.textPrimary,
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MindTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                    )
                }
                if (content != null) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        content = content,
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions,
                )
            }
        }
    }
}

@Composable
internal fun MindSelectionCheckBadge(
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    val bg by animateColorAsState(
        targetValue = if (selected) MindTheme.colors.accent else Color.Black.copy(alpha = 0.42f),
        animationSpec = tween(durationMillis = 150),
        label = "selectionBg",
    )
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(bg)
            .border(
                width = 1.25.dp,
                color = if (selected) MindTheme.colors.onAccent else Color.White.copy(alpha = 0.85f),
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Selected",
                tint = MindTheme.colors.onAccent,
                modifier = Modifier.size(15.dp),
            )
        }
    }
}
