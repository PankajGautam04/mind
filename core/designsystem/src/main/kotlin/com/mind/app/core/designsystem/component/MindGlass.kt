package com.mind.app.core.designsystem.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mind.app.core.designsystem.theme.MindTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlin.math.PI
import kotlin.math.cos

/**
 * Provides the shared [HazeState] of the active scrolling canvas so floating navigation docks
 * and chrome bars render real GPU-backed frosted glass blur behind themselves.
 */
val LocalMindHazeState = compositionLocalOf<HazeState?> { null }

/**
 * Four-tier material system matching dsh-go's GlassTier:
 * - [CHROME]: floating top bar pills, circular buttons, bottom capsule dock (Haze blur + rim + hairline)
 * - [SHEET]: modal sheets & drawers (stronger Haze blur + rim + hairline)
 * - [CARD]: grouped settings cards, memory cards, summary panels (translucent paper/smoke + rim + hairline)
 * - [PILL]: inline status pills, chips, breadcrumbs (subtle translucent capsule + hairline)
 */
enum class MindGlassTier {
    CHROME,
    SHEET,
    CARD,
    PILL,
}

/**
 * Full-screen Mist Glass / Deep Ocean Slate backdrop shared across top-level tabs and pages:
 * vertical low-chroma gradient with soft top-right primary glow and bottom-left neighbor haze.
 */
@Composable
fun MindMistBackdrop(
    modifier: Modifier = Modifier,
    glow: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = MindTheme.colors
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val longest = maxOf(w, h).coerceAtLeast(1f)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(colors.backdropTop, colors.backdropBottom),
                ),
            )
            if (glow) {
                if (colors.glowPrimary.alpha > 0f) {
                    val primaryCenter = Offset(w * 0.92f, 0f)
                    val primaryRadius = (longest * 0.62f).coerceAtLeast(32f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(colors.glowPrimary, Color.Transparent),
                            center = primaryCenter,
                            radius = primaryRadius,
                        ),
                        center = primaryCenter,
                        radius = primaryRadius,
                    )
                }
                if (colors.glowSecondary.alpha > 0f) {
                    val secondaryCenter = Offset(0f, h)
                    val secondaryRadius = (longest * 0.55f).coerceAtLeast(32f)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(colors.glowSecondary, Color.Transparent),
                            center = secondaryCenter,
                            radius = secondaryRadius,
                        ),
                        center = secondaryCenter,
                        radius = secondaryRadius,
                    )
                }
            }
        }
        content()
    }
}

/**
 * Feathered gradient veil that matches the Mist Backdrop so scrolled content dissolves
 * gently beneath floating top chrome or bottom docks instead of cutting sharply.
 */
@Composable
fun MindBackdropVeil(
    fromTop: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MindTheme.colors
    val base = if (fromTop) colors.backdropTop else colors.backdropBottom
    val stops = List(9) { i ->
        val t = i / 8f
        val eased = ((1f - cos(t * PI.toFloat())) * 0.5f)
        val alpha = if (fromTop) 1f - eased else eased
        t to base.copy(alpha = alpha * 0.94f)
    }.toTypedArray()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(colorStops = stops)),
    )
}

/**
 * Feathered progressive mist veil for top and bottom screen edges so scrolling content
 * softens smoothly into the ambient backdrop without a hard rectangular border or strap.
 */
@Composable
fun MindEdgeBlurVeil(
    @Suppress("UNUSED_PARAMETER") hazeState: HazeState?,
    fromTop: Boolean,
    height: Dp,
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") blurRadius: Dp = 18.dp,
    tintAlpha: Float = 0.24f,
) {
    val colors = MindTheme.colors
    val baseColor = if (fromTop) colors.backdropTop else colors.backdropBottom
    val edgeAlpha = (tintAlpha * 1.6f).coerceIn(0.12f, 0.52f)
    val stops = Array(13) { i ->
        val t = i / 12f
        val eased = (1f - cos(t * PI.toFloat())) * 0.5f
        val curve = if (fromTop) (1f - eased) * (1f - eased) else eased * eased
        t to baseColor.copy(alpha = (edgeAlpha * curve).coerceIn(0f, 1f))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(Brush.verticalGradient(colorStops = stops)),
    )
}

/**
 * Softens top and bottom viewport edges of any scrollable list or grid using a multi-stop
 * cosine feather so items appear and disappear smoothly without any sharp crop edge or Mach band.
 */
fun Modifier.mindScrollEdgeFade(
    topFade: Dp = 36.dp,
    bottomFade: Dp = 54.dp,
): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val h = size.height
        if (h <= 2f) return@drawWithContent
        val topFrac = (topFade.toPx() / h).coerceIn(0f, 0.42f)
        val botFrac = (bottomFade.toPx() / h).coerceIn(0f, 0.42f)
        if (topFrac <= 0.002f && botFrac <= 0.002f) return@drawWithContent

        val steps = 8
        val stops = buildList {
            if (topFrac > 0.002f) {
                for (i in 0..steps) {
                    val t = i.toFloat() / steps
                    val alpha = ((1f - cos(t * PI.toFloat())) * 0.5f).coerceIn(0f, 1f)
                    add((topFrac * t) to Color.Black.copy(alpha = alpha))
                }
            } else {
                add(0f to Color.Black)
            }
            if (botFrac > 0.002f) {
                val startBot = (1f - botFrac).coerceAtLeast(topFrac)
                val span = (1f - startBot).coerceAtLeast(0.0001f)
                for (i in 0..steps) {
                    val t = i.toFloat() / steps
                    val alpha = ((1f + cos(t * PI.toFloat())) * 0.5f).coerceIn(0f, 1f)
                    add((startBot + span * t) to Color.Black.copy(alpha = alpha))
                }
            } else {
                add(1f to Color.Black)
            }
        }.toTypedArray()

        drawRect(
            brush = Brush.verticalGradient(colorStops = stops),
            blendMode = BlendMode.DstIn,
        )
    }

/**
 * Softens left and right viewport edges of any horizontal row or carousel using a multi-stop
 * cosine feather so chips and pills appear and disappear smoothly without a sharp vertical cutout.
 */
fun Modifier.mindHorizontalScrollEdgeFade(
    leftFade: Dp = 20.dp,
    rightFade: Dp = 20.dp,
): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val w = size.width
        if (w <= 2f) return@drawWithContent
        val leftFrac = (leftFade.toPx() / w).coerceIn(0f, 0.35f)
        val rightFrac = (rightFade.toPx() / w).coerceIn(0f, 0.35f)
        if (leftFrac <= 0.002f && rightFrac <= 0.002f) return@drawWithContent

        val steps = 8
        val stops = buildList {
            if (leftFrac > 0.002f) {
                for (i in 0..steps) {
                    val t = i.toFloat() / steps
                    val alpha = ((1f - cos(t * PI.toFloat())) * 0.5f).coerceIn(0f, 1f)
                    add((leftFrac * t) to Color.Black.copy(alpha = alpha))
                }
            } else {
                add(0f to Color.Black)
            }
            if (rightFrac > 0.002f) {
                val startRight = (1f - rightFrac).coerceAtLeast(leftFrac)
                val span = (1f - startRight).coerceAtLeast(0.0001f)
                for (i in 0..steps) {
                    val t = i.toFloat() / steps
                    val alpha = ((1f + cos(t * PI.toFloat())) * 0.5f).coerceIn(0f, 1f)
                    add((startRight + span * t) to Color.Black.copy(alpha = alpha))
                }
            } else {
                add(1f to Color.Black)
            }
        }.toTypedArray()

        drawRect(
            brush = Brush.horizontalGradient(colorStops = stops),
            blendMode = BlendMode.DstIn,
        )
    }

@Composable
fun mindGlassBorder(
    isDark: Boolean = MindTheme.colors.isDark,
    intensity: Float = 1f,
): BorderStroke {
    val colors = MindTheme.colors
    val topAlpha = (if (isDark) 0.16f else 0.85f) * intensity
    val midAlpha = (if (isDark) 0.06f else 0.24f) * intensity
    val botAlpha = (if (isDark) 0.08f else 0.14f) * intensity
    val rimColor = if (isDark) Color.White else colors.rimHighlight
    return BorderStroke(
        width = 0.75.dp,
        brush = Brush.verticalGradient(
            0.0f to rimColor.copy(alpha = topAlpha.coerceIn(0f, 1f)),
            0.45f to colors.hairline.copy(alpha = (colors.hairline.alpha + midAlpha * 0.1f).coerceIn(0f, 1f)),
            1.0f to Color.White.copy(alpha = botAlpha.coerceIn(0f, 1f)),
        ),
    )
}

@Composable
fun mindGlassStyle(
    blurRadius: Dp = 20.dp,
    tintAlpha: Float? = null,
    isDark: Boolean = MindTheme.colors.isDark,
): HazeStyle {
    val colors = MindTheme.colors
    val baseFill = if (isDark) colors.glassChromeFill else Color.White
    val resolvedAlpha = tintAlpha ?: if (isDark) 0.72f else 0.70f
    return HazeStyle(
        backgroundColor = colors.glassChromeFallback,
        tint = HazeTint(baseFill.copy(alpha = resolvedAlpha)),
        blurRadius = blurRadius,
        noiseFactor = 0.022f,
    )
}

fun Modifier.mindGlassBackdrop(
    shape: Shape,
    hazeState: HazeState?,
    blurRadius: Dp = 20.dp,
    tintAlpha: Float? = null,
): Modifier = composed {
    if (hazeState != null) {
        val style = mindGlassStyle(blurRadius = blurRadius, tintAlpha = tintAlpha)
        this
            .clip(shape)
            .hazeEffect(state = hazeState, style = style)
    } else {
        this.clip(shape)
    }
}

/**
 * Draws dsh-go's signature inset 0.6dp hairline outline plus soft top-edge highlight rim
 * fading out at 45% height so glass cards catch light cleanly without muddy borders.
 */
fun Modifier.mindGlassEdge(
    shape: Shape,
    hairlineColor: Color,
    rimColor: Color,
    strokeWidth: Dp = 0.65.dp,
): Modifier = this.drawWithContent {
    drawContent()
    if (size.width <= 1f || size.height <= 1f) return@drawWithContent
    val wPx = strokeWidth.toPx().coerceAtLeast(0.75f)
    val rimPx = (wPx * 1.2f).coerceAtLeast(1f)
    val outline = shape.createOutline(size, layoutDirection, this)

    fun insetPathFor(strokePx: Float): Path {
        val inset = (strokePx * 0.5f).coerceAtMost(minOf(size.width, size.height) * 0.25f)
        fun shrinkCorner(c: CornerRadius): CornerRadius = CornerRadius(
            x = (c.x - inset).coerceAtLeast(0f),
            y = (c.y - inset).coerceAtLeast(0f),
        )
        return when (outline) {
            is Outline.Generic -> outline.path
            is Outline.Rectangle -> Path().apply {
                addRect(
                    Rect(
                        left = outline.rect.left + inset,
                        top = outline.rect.top + inset,
                        right = outline.rect.right - inset,
                        bottom = outline.rect.bottom - inset,
                    ),
                )
            }
            is Outline.Rounded -> Path().apply {
                val rr = outline.roundRect
                addRoundRect(
                    RoundRect(
                        left = rr.left + inset,
                        top = rr.top + inset,
                        right = rr.right - inset,
                        bottom = rr.bottom - inset,
                        topLeftCornerRadius = shrinkCorner(rr.topLeftCornerRadius),
                        topRightCornerRadius = shrinkCorner(rr.topRightCornerRadius),
                        bottomRightCornerRadius = shrinkCorner(rr.bottomRightCornerRadius),
                        bottomLeftCornerRadius = shrinkCorner(rr.bottomLeftCornerRadius),
                    ),
                )
            }
        }
    }

    if (hairlineColor.alpha > 0.002f) {
        drawPath(
            path = insetPathFor(wPx),
            color = hairlineColor,
            style = Stroke(width = wPx),
        )
    }
    if (rimColor.alpha > 0.002f) {
        drawPath(
            path = insetPathFor(rimPx),
            brush = Brush.verticalGradient(
                0.0f to rimColor,
                0.45f to Color.Transparent,
                startY = 0f,
                endY = size.height,
            ),
            style = Stroke(width = rimPx),
        )
    }
}

/**
 * Applies tactile dsh-go Pressable spring scale (settles cleanly to [pressedScale] on touch,
 * gently rebounds on release) paired cleanly with any [InteractionSource].
 */
fun Modifier.dshPressableScale(
    interactionSource: InteractionSource,
    enabled: Boolean = true,
    pressedScale: Float = 0.97f,
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (enabled && pressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = if (pressed) Spring.DampingRatioNoBouncy else 0.62f,
            stiffness = if (pressed) 580f else 420f,
        ),
        label = "dshPressScale",
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Reports raw pointer press coordinates relative to this composable so DistortionWave can lean
 * toward the finger on launch just like dsh-go ModeKey / WaveStage.
 */
fun Modifier.captureTapOffset(
    onDown: (Offset) -> Unit,
): Modifier = this.pointerInput(Unit) {
    detectTapGestures(
        onPress = { offset ->
            onDown(offset)
            tryAwaitRelease()
        },
    )
}
