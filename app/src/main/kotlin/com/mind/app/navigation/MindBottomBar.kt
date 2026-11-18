package com.mind.app.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import com.mind.app.core.designsystem.component.LocalMindHazeState
import com.mind.app.core.designsystem.component.dshPressableScale
import com.mind.app.core.designsystem.component.mindGlassBackdrop
import com.mind.app.core.designsystem.component.mindGlassEdge
import com.mind.app.core.designsystem.theme.MindTheme

enum class TopLevelTab(
    val label: String,
    val destination: MindDestination,
) {
    HOME(
        label = "Home",
        destination = MindDestination.Home,
    ),
    COLLECTIONS(
        label = "Spaces",
        destination = MindDestination.Collections,
    ),
    SEARCH(
        label = "Search",
        destination = MindDestination.Search(),
    ),
    FAVOURITES(
        label = "Favorites",
        destination = MindDestination.Favourites,
    ),
}

fun NavDestination?.matchesTab(tab: TopLevelTab): Boolean {
    if (this == null) return false
    return when (tab) {
        TopLevelTab.HOME -> hasRoute<MindDestination.Home>()
        TopLevelTab.COLLECTIONS -> hasRoute<MindDestination.Collections>()
        TopLevelTab.SEARCH -> hasRoute<MindDestination.Search>()
        TopLevelTab.FAVOURITES -> hasRoute<MindDestination.Favourites>()
    }
}

/**
 * Sleek Liquid-Glass floating capsule dock with real live backdrop blur,
 * spring-sliding pill indicator, and custom soft-curved modern icons.
 */
@Composable
fun MindBottomBar(
    currentDestination: NavDestination?,
    onSelectTab: (TopLevelTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MindTheme.colors
    val hazeState = LocalMindHazeState.current
    val dockShape = RoundedCornerShape(28.dp)
    val tabs = TopLevelTab.entries
    val selectedIndex = tabs.indexOfFirst { currentDestination.matchesTab(it) }.coerceAtLeast(0)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 316.dp)
                .fillMaxWidth()
                .height(56.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = dockShape,
                    ambientColor = colors.shadowInk,
                    spotColor = colors.shadowInk,
                )
                .mindGlassBackdrop(
                    shape = dockShape,
                    hazeState = hazeState,
                    blurRadius = 24.dp,
                )
                .mindGlassEdge(
                    shape = dockShape,
                    hairlineColor = colors.hairline,
                    rimColor = colors.rimHighlight,
                ),
            shape = dockShape,
            color = if (hazeState != null) Color.Transparent else colors.glassChromeFallback,
            contentColor = colors.textPrimary,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 5.dp, vertical = 5.dp),
            ) {
                val tabWidth = (maxWidth / tabs.size).coerceAtLeast(1.dp)
                val pillOffset by animateDpAsState(
                    targetValue = tabWidth * selectedIndex,
                    animationSpec = spring(
                        dampingRatio = 0.76f,
                        stiffness = 420f,
                    ),
                    label = "bottom_bar_pill_offset",
                )
                val pillShape = RoundedCornerShape(23.dp)
                val pillFill = if (colors.isDark) {
                    Color.White.copy(alpha = 0.14f)
                } else {
                    Color.White.copy(alpha = 0.76f)
                }

                Box(
                    modifier = Modifier
                        .offset(x = pillOffset)
                        .width(tabWidth)
                        .fillMaxHeight()
                        .clip(pillShape)
                        .background(pillFill)
                        .mindGlassEdge(
                            shape = pillShape,
                            hairlineColor = colors.hairline,
                            rimColor = if (colors.isDark) colors.rimHighlight else Color.White,
                            strokeWidth = 0.55.dp,
                        ),
                )

                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    tabs.forEachIndexed { index, tab ->
                        val selected = index == selectedIndex
                        val interactionSource = remember { MutableInteractionSource() }
                        val contentColor by animateColorAsState(
                            targetValue = if (selected) colors.textPrimary else colors.textMuted,
                            label = "dock_tint_${tab.name}",
                        )
                        val accentTint by animateColorAsState(
                            targetValue = if (selected) colors.accent else Color.Transparent,
                            label = "dock_accent_${tab.name}",
                        )
                        val iconScale by animateFloatAsState(
                            targetValue = if (selected) 1.08f else 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = 500f,
                            ),
                            label = "dock_scale_${tab.name}",
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .dshPressableScale(
                                    interactionSource = interactionSource,
                                    pressedScale = 0.94f,
                                )
                                .clip(pillShape)
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = { onSelectTab(tab) },
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                        ) {
                            MindModernDockIcon(
                                tab = tab,
                                selected = selected,
                                strokeColor = contentColor,
                                accentColor = accentTint,
                                modifier = Modifier
                                    .size(20.dp)
                                    .graphicsLayer {
                                        scaleX = iconScale
                                        scaleY = iconScale
                                    },
                            )
                            Text(
                                text = tab.label,
                                style = MindTheme.typography.labelSmall.copy(
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                ),
                                color = contentColor,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Custom smooth continuous-curve icons with rounded caps, rounded squircle geometry,
 * and soft duo-tone accent fills when active — zero sharp corners.
 */
@Composable
private fun MindModernDockIcon(
    tab: TopLevelTab,
    selected: Boolean,
    strokeColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = w * if (selected) 0.095f else 0.085f
        val strokeStyle = Stroke(
            width = strokeW,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )

        when (tab) {
            TopLevelTab.HOME -> {
                // Softly rounded 2x2 Bento Collage Grid (zero sharp corners)
                val gap = w * 0.08f
                val pad = w * 0.12f
                val colW = (w - pad * 2f - gap) / 2f
                val tallH = (h - pad * 2f - gap) * 0.58f
                val shortH = (h - pad * 2f - gap) * 0.42f
                val cr = CornerRadius(w * 0.12f, w * 0.12f)

                // Top-left (tall squircle)
                if (selected) {
                    drawRoundRect(
                        color = accentColor.copy(alpha = 0.32f),
                        topLeft = Offset(pad, pad),
                        size = Size(colW, tallH),
                        cornerRadius = cr,
                    )
                }
                drawRoundRect(
                    color = strokeColor,
                    topLeft = Offset(pad, pad),
                    size = Size(colW, tallH),
                    cornerRadius = cr,
                    style = strokeStyle,
                )

                // Bottom-left (short squircle)
                drawRoundRect(
                    color = strokeColor,
                    topLeft = Offset(pad, pad + tallH + gap),
                    size = Size(colW, shortH),
                    cornerRadius = cr,
                    style = strokeStyle,
                )

                // Top-right (short squircle)
                drawRoundRect(
                    color = strokeColor,
                    topLeft = Offset(pad + colW + gap, pad),
                    size = Size(colW, shortH),
                    cornerRadius = cr,
                    style = strokeStyle,
                )

                // Bottom-right (tall squircle)
                if (selected) {
                    drawRoundRect(
                        color = accentColor.copy(alpha = 0.32f),
                        topLeft = Offset(pad + colW + gap, pad + shortH + gap),
                        size = Size(colW, tallH),
                        cornerRadius = cr,
                    )
                }
                drawRoundRect(
                    color = strokeColor,
                    topLeft = Offset(pad + colW + gap, pad + shortH + gap),
                    size = Size(colW, tallH),
                    cornerRadius = cr,
                    style = strokeStyle,
                )
            }

            TopLevelTab.COLLECTIONS -> {
                // Modern layered soft squircle capsules (Spaces)
                val backCr = CornerRadius(w * 0.18f, w * 0.18f)
                val frontCr = CornerRadius(w * 0.20f, w * 0.20f)

                // Top-back pill bar
                drawRoundRect(
                    color = strokeColor.copy(alpha = 0.72f),
                    topLeft = Offset(w * 0.22f, h * 0.15f),
                    size = Size(w * 0.56f, h * 0.22f),
                    cornerRadius = backCr,
                    style = strokeStyle,
                )

                // Front main rounded capsule card
                if (selected) {
                    drawRoundRect(
                        color = accentColor.copy(alpha = 0.28f),
                        topLeft = Offset(w * 0.13f, h * 0.32f),
                        size = Size(w * 0.74f, h * 0.54f),
                        cornerRadius = frontCr,
                    )
                }
                drawRoundRect(
                    color = strokeColor,
                    topLeft = Offset(w * 0.13f, h * 0.32f),
                    size = Size(w * 0.74f, h * 0.54f),
                    cornerRadius = frontCr,
                    style = strokeStyle,
                )
            }

            TopLevelTab.SEARCH -> {
                // Smooth rounded lens with soft inner glow & curved handle
                val center = Offset(w * 0.44f, h * 0.44f)
                val radius = w * 0.29f
                if (selected) {
                    drawCircle(
                        color = accentColor.copy(alpha = 0.28f),
                        radius = radius,
                        center = center,
                    )
                }
                drawCircle(
                    color = strokeColor,
                    radius = radius,
                    center = center,
                    style = strokeStyle,
                )
                drawLine(
                    color = strokeColor,
                    start = Offset(w * 0.66f, h * 0.66f),
                    end = Offset(w * 0.85f, h * 0.85f),
                    strokeWidth = strokeW * 1.08f,
                    cap = StrokeCap.Round,
                )
            }

            TopLevelTab.FAVOURITES -> {
                // Plump, continuous-bezier modern Heart with rounded join
                val heartPath = Path().apply {
                    moveTo(w * 0.50f, h * 0.84f)
                    cubicTo(
                        w * 0.16f, h * 0.62f,
                        w * 0.10f, h * 0.34f,
                        w * 0.28f, h * 0.21f,
                    )
                    cubicTo(
                        w * 0.39f, h * 0.14f,
                        w * 0.47f, h * 0.21f,
                        w * 0.50f, h * 0.28f,
                    )
                    cubicTo(
                        w * 0.53f, h * 0.21f,
                        w * 0.61f, h * 0.14f,
                        w * 0.72f, h * 0.21f,
                    )
                    cubicTo(
                        w * 0.90f, h * 0.34f,
                        w * 0.84f, h * 0.62f,
                        w * 0.50f, h * 0.84f,
                    )
                    close()
                }
                if (selected) {
                    drawPath(
                        path = heartPath,
                        color = accentColor.copy(alpha = 0.32f),
                    )
                }
                drawPath(
                    path = heartPath,
                    color = strokeColor,
                    style = strokeStyle,
                )
            }
        }
    }
}
