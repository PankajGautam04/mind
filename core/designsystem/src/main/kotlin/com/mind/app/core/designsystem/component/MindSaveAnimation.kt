package com.mind.app.core.designsystem.component

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.mind.app.core.designsystem.theme.MindTheme
import com.mind.app.core.model.AnimationStyle
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

private val FluidEaseOut = CubicBezierEasing(0.22f, 1.0f, 0.36f, 1.0f)
private val FluidDissolveEase = CubicBezierEasing(0.32f, 0.0f, 0.24f, 1.0f)
private val SeaWaveFlowEasing = CubicBezierEasing(0.25f, 0.08f, 0.25f, 1.0f)

private data class FrozenScreenFrame(
    val androidBitmap: Bitmap,
    val imageBitmap: ImageBitmap,
)

private const val SEA_WAVE_AGSL_SHADER = """
uniform shader composable;
uniform float2 resolution;
uniform float progress;
uniform float4 crestTint;

half4 main(float2 fragCoord) {
    if (resolution.x <= 1.0 || resolution.y <= 1.0) {
        return composable.eval(fragCoord);
    }
    float p = clamp(progress, 0.0, 1.0);
    float2 uv = fragCoord / resolution;
    float minDim = max(min(resolution.x, resolution.y), 1.0);

    // Settle completely back to 0.0 by p = 0.80 so crossfade into live screen is 1:1 with zero ghosting
    float envIn = smoothstep(0.0, 0.14, p);
    float envOut = 1.0 - smoothstep(0.56, 0.80, p);
    float waveEnv = envIn * envOut;

    float edgeX = smoothstep(0.0, 0.07, uv.x) * (1.0 - smoothstep(0.93, 1.0, uv.x));
    float edgeY = smoothstep(0.0, 0.025, uv.y) * (1.0 - smoothstep(0.96, 1.0, uv.y));
    float edgeMask = edgeX * edgeY;

    // Gently curved wave front travelling calmly from top (-0.14) down to bottom (+1.18)
    float frontY = -0.14 + p * 1.32;
    float curve = 0.045 * cos((uv.x - 0.5) * 3.14159265)
                + 0.015 * sin(uv.x * 5.0 - p * 3.0);
    float contourY = uv.y - curve;

    float distA = contourY - frontY;
    float envA = exp(-distA * distA * 10.5);
    float waveA = sin(distA * 7.6) * envA;

    float distB = contourY - (frontY - 0.34);
    float envB = exp(-distB * distB * 13.0);
    float waveB = sin(distB * 6.2) * envB * 0.52;

    float sway = sin(uv.x * 3.5 + uv.y * 2.2 - p * 5.0);
    float ampPx = minDim * 0.025 * waveEnv * edgeMask;
    float dispY = ((waveA + waveB) * 1.12 + envA * 0.30) * ampPx;
    float dispX = sway * (envA * 0.62 + envB * 0.38) * ampPx * 0.70;

    float2 centerPx = resolution * 0.5;
    float floatScale = 1.0 + 0.014 * waveEnv;
    float2 baseCoord = centerPx + (fragCoord - centerPx) / floatScale;

    float2 sampleCoord = clamp(baseCoord - float2(dispX, dispY), float2(0.5), resolution - float2(0.5));
    half4 color = composable.eval(sampleCoord);

    float sheen = smoothstep(0.10, 0.90, exp(-distA * distA * 20.0))
                + smoothstep(0.15, 0.90, exp(-distB * distB * 22.0)) * 0.42;
    float glow = clamp(sheen * waveEnv * 0.10, 0.0, 0.15);
    color.rgb += crestTint.rgb * (glow * color.a);

    return color;
}
"""

private val SAVE_QUOTES = listOf(
    "Go find it, it's in your Mind.",
    "Held safely for the moment you need it.",
    "Your second brain caught that for you.",
    "Saved. Yes, you can close that tab now.",
    "Tucked away so your head stays clear.",
    "Out of sight, never out of Mind.",
    "Future you is going to smile finding this.",
    "Stashed before life steals your focus.",
    "Consider it remembered.",
    "Screenshot #47? Relax, Mind's got every detail.",
    "Caught cleanly. Carry on.",
    "Stored quietly. No folders, no clutter.",
    "One small spark saved for later.",
    "Remembered, so your real brain can breathe.",
    "Safe in your Mind. Ask anytime it slips away.",
    "Offloaded cleanly from working memory.",
)

private var lastSaveQuoteIndex = -1

private fun pickNextSaveQuote(): String {
    if (SAVE_QUOTES.size <= 1) return SAVE_QUOTES.firstOrNull().orEmpty()
    var next = Random.nextInt(SAVE_QUOTES.size)
    if (next == lastSaveQuoteIndex) {
        next = (lastSaveQuoteIndex + 1 + Random.nextInt(SAVE_QUOTES.size - 1)) % SAVE_QUOTES.size
    }
    lastSaveQuoteIndex = next
    return SAVE_QUOTES[next]
}

private fun lerpF(start: Float, stop: Float, fraction: Float): Float =
    start + (stop - start) * fraction

@Composable
private fun rememberFrozenScreenFrame(
    imagePath: String?,
    enabled: Boolean,
): FrozenScreenFrame? {
    val cachedSync = remember(imagePath, enabled) {
        if (!enabled) {
            null
        } else {
            MindCaptureSnapshotCache.peek()?.takeIf { !it.isRecycled && !isBitmapMostlyBlack(it) }?.let { bmp ->
                FrozenScreenFrame(
                    androidBitmap = bmp,
                    imageBitmap = bmp.asImageBitmap(),
                )
            }
        }
    }
    val frame by produceState<FrozenScreenFrame?>(
        initialValue = cachedSync,
        key1 = imagePath,
        key2 = enabled,
    ) {
        if (!enabled) {
            value = null
            return@produceState
        }
        val memBmp = MindCaptureSnapshotCache.peek()
        if (memBmp != null && !memBmp.isRecycled && !isBitmapMostlyBlack(memBmp)) {
            value = FrozenScreenFrame(
                androidBitmap = memBmp,
                imageBitmap = memBmp.asImageBitmap(),
            )
            return@produceState
        }
        if (imagePath.isNullOrBlank()) {
            value = null
            return@produceState
        }
        value = withContext(Dispatchers.IO) {
            val file = File(imagePath)
            if (!file.exists()) return@withContext null

            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, boundsOpts)
            val rawMax = maxOf(boundsOpts.outWidth, boundsOpts.outHeight)
            if (rawMax <= 0) return@withContext null

            var sampleSize = 1
            while (rawMax / sampleSize > 1440) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOpts) ?: return@withContext null
            if (isBitmapMostlyBlack(bitmap)) {
                bitmap.recycle()
                return@withContext null
            }
            FrozenScreenFrame(
                androidBitmap = bitmap,
                imageBitmap = bitmap.asImageBitmap(),
            )
        }
    }
    return frame
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun rememberSeaWaveShaderModifier(
    progressProvider: () -> Float,
    crestColor: Color,
): Modifier {
    val runtimeShader = remember { RuntimeShader(SEA_WAVE_AGSL_SHADER) }
    return Modifier.graphicsLayer {
        if (size.width > 1f && size.height > 1f) {
            runtimeShader.setFloatUniform("resolution", size.width, size.height)
            runtimeShader.setFloatUniform("progress", progressProvider().coerceIn(0f, 1f))
            runtimeShader.setFloatUniform(
                "crestTint",
                crestColor.red,
                crestColor.green,
                crestColor.blue,
                1f,
            )
            renderEffect = RenderEffect.createRuntimeShaderEffect(runtimeShader, "composable")
                .asComposeRenderEffect()
        }
    }
}

private const val SEA_WAVE_MESH_COLS = 20
private const val SEA_WAVE_MESH_ROWS = 36

private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
    val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private fun DrawScope.drawSeaWaveBitmapMesh(
    bitmap: Bitmap,
    progress: Float,
    crestColor: Color,
    meshPaint: Paint,
    vertsBuffer: FloatArray,
) {
    if (bitmap.isRecycled) return
    val w = size.width
    val h = size.height
    if (w <= 1f || h <= 1f) return

    val p = progress.coerceIn(0f, 1f)
    val minDim = minOf(w, h).coerceAtLeast(1f)
    val envIn = smoothstep(0f, 0.14f, p)
    val envOut = 1f - smoothstep(0.56f, 0.80f, p)
    val waveEnv = envIn * envOut
    val floatScale = 1f + 0.014f * waveEnv

    val imgW = bitmap.width.toFloat().coerceAtLeast(1f)
    val imgH = bitmap.height.toFloat().coerceAtLeast(1f)
    val cropScale = maxOf(w / imgW, h / imgH) * floatScale
    val drawW = imgW * cropScale
    val drawH = imgH * cropScale
    val dstLeft = (w - drawW) / 2f
    val dstTop = (h - drawH) / 2f

    val frontY = -0.14f + p * 1.32f
    val ampPx = minDim * 0.025f * waveEnv

    var idx = 0
    for (row in 0..SEA_WAVE_MESH_ROWS) {
        val v = row.toFloat() / SEA_WAVE_MESH_ROWS
        val baseY = dstTop + v * drawH
        val ny = (baseY / h).coerceIn(0f, 1f)
        val edgeY = smoothstep(0f, 0.025f, ny) * (1f - smoothstep(0.96f, 1f, ny))

        for (col in 0..SEA_WAVE_MESH_COLS) {
            val u = col.toFloat() / SEA_WAVE_MESH_COLS
            val baseX = dstLeft + u * drawW
            val nx = (baseX / w).coerceIn(0f, 1f)
            val edgeX = smoothstep(0f, 0.07f, nx) * (1f - smoothstep(0.93f, 1f, nx))
            val edgeMask = edgeX * edgeY

            val curve = 0.045f * cos((nx - 0.5f) * PI.toFloat()) +
                0.015f * sin(nx * 5f - p * 3.0f)
            val contourY = ny - curve

            val distA = contourY - frontY
            val envA = exp((-distA * distA * 10.5f).toDouble()).toFloat()
            val waveA = sin(distA * 7.6f) * envA

            val distB = contourY - (frontY - 0.34f)
            val envB = exp((-distB * distB * 13f).toDouble()).toFloat()
            val waveB = sin(distB * 6.2f) * envB * 0.52f

            val sway = sin(nx * 3.5f + ny * 2.2f - p * 5.0f)
            val amp = ampPx * edgeMask
            val dispX = sway * (envA * 0.62f + envB * 0.38f) * amp * 0.70f
            val dispY = ((waveA + waveB) * 1.12f + envA * 0.30f) * amp

            vertsBuffer[idx++] = baseX + dispX
            vertsBuffer[idx++] = baseY + dispY
        }
    }

    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawBitmapMesh(
            bitmap,
            SEA_WAVE_MESH_COLS,
            SEA_WAVE_MESH_ROWS,
            vertsBuffer,
            0,
            null,
            0,
            meshPaint,
        )
    }

    // Soft top-to-bottom ocean crest shimmer band tracking the descending wave front
    val bandHalfH = (h * 0.16f).coerceAtLeast(24f)
    val bandCenterY = frontY * h
    if (waveEnv > 0.02f && bandCenterY > -bandHalfH && bandCenterY < h + bandHalfH) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    crestColor.copy(alpha = 0.11f * waveEnv),
                    Color.White.copy(alpha = 0.06f * waveEnv),
                    Color.Transparent,
                ),
                startY = bandCenterY - bandHalfH,
                endY = bandCenterY + bandHalfH,
            ),
            topLeft = Offset(0f, (bandCenterY - bandHalfH).coerceAtLeast(0f)),
            size = androidx.compose.ui.geometry.Size(w, (bandHalfH * 2f).coerceAtMost(h)),
        )
    }
}

@Composable
private fun SeaWaveFrozenScreenLayer(
    frame: FrozenScreenFrame,
    progressProvider: () -> Float,
    alphaProvider: () -> Float,
    crestColor: Color,
    modifier: Modifier = Modifier,
) {
    // Keep outer transform 1:1 so settling waves align seamlessly with live pixels beneath
    val alphaModifier = modifier.graphicsLayer {
        alpha = alphaProvider().coerceIn(0f, 1f)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val shaderModifier = rememberSeaWaveShaderModifier(
            progressProvider = progressProvider,
            crestColor = crestColor,
        )
        Image(
            bitmap = frame.imageBitmap,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center,
            modifier = alphaModifier.then(shaderModifier),
        )
    } else {
        val meshPaint = remember { Paint(Paint.FILTER_BITMAP_FLAG) }
        val vertsBuffer = remember {
            FloatArray((SEA_WAVE_MESH_COLS + 1) * (SEA_WAVE_MESH_ROWS + 1) * 2)
        }
        Canvas(modifier = alphaModifier) {
            drawSeaWaveBitmapMesh(
                bitmap = frame.androidBitmap,
                progress = progressProvider(),
                crestColor = crestColor,
                meshPaint = meshPaint,
                vertsBuffer = vertsBuffer,
            )
        }
    }
}

/**
 * ColorOS / iOS-inspired weighted fluid glass capture choreography:
 * soft OS/canvas backdrop blur + calm hero card bloom -> seamless glide into a real frosted
 * optical glass pill refracting live content behind it -> personal quote settle -> soft dissolve.
 */
@Composable
fun MindSaveAnimationOverlay(
    visible: Boolean,
    style: AnimationStyle,
    animationsEnabled: Boolean,
    title: String = "Saved to Mind",
    thumbnailPath: String? = null,
    fullscreenImagePath: String? = null,
    hazeState: HazeState? = null,
    externalBlurActive: Boolean = true,
    onBackdropBlurFraction: (Float) -> Unit = {},
    onViewMemory: (() -> Unit)? = null,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val entryProgress = remember { Animatable(0f) }
    val morphProgress = remember { Animatable(0f) }
    val waveProgress = remember { Animatable(0f) }
    val atmosphereProgress = remember { Animatable(0f) }
    val backdropBlurFraction = remember { Animatable(0f) }
    val shadeRevealProgress = remember { Animatable(0f) }
    val quoteRevealProgress = remember { Animatable(0f) }
    val exitAlpha = remember { Animatable(1f) }
    val dismissOffsetX = remember { Animatable(0f) }

    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val density = LocalDensity.current

    var isDragging by remember { mutableStateOf(false) }
    var dismissedInstantly by remember(visible) { mutableStateOf(false) }
    var activeQuote by remember { mutableStateOf(SAVE_QUOTES.first()) }

    val triggerInstantDismiss: () -> Unit = {
        if (!dismissedInstantly) {
            dismissedInstantly = true
            onBackdropBlurFraction(0f)
            MindCaptureSnapshotCache.clear()
            scope.launch { exitAlpha.snapTo(0f) }
            onFinished()
        }
    }

    val hasThumbnail = !thumbnailPath.isNullOrBlank() && File(thumbnailPath).exists()
    val freezeImagePath = fullscreenImagePath
        ?.takeIf { it.isNotBlank() && File(it).exists() }
        ?: thumbnailPath?.takeIf { hasThumbnail }
    val isSeaWave = style == AnimationStyle.SEA_WAVE
    val isCompanionTopDown = style == AnimationStyle.COMPANION_WAVE_TOP_DOWN
    val isCompanionWave = style == AnimationStyle.COMPANION_WAVE || isCompanionTopDown
    val hasFreezeMedia = !freezeImagePath.isNullOrBlank()

    val frozenFrame = rememberFrozenScreenFrame(
        imagePath = freezeImagePath,
        enabled = visible && (isSeaWave || isCompanionWave) && hasFreezeMedia && animationsEnabled,
    )
    val latestFrozenFrame by rememberUpdatedState(frozenFrame)

    // Forward animated blur intensity continuously to native window blur (SaveOverlayActivity on API 31+)
    LaunchedEffect(Unit) {
        snapshotFlow { backdropBlurFraction.value * exitAlpha.value }
            .collectLatest { level ->
                onBackdropBlurFraction(level.coerceIn(0f, 1f))
            }
    }

    LaunchedEffect(visible, style, title, thumbnailPath, fullscreenImagePath) {
        if (!visible) {
            entryProgress.snapTo(0f)
            morphProgress.snapTo(0f)
            waveProgress.snapTo(0f)
            atmosphereProgress.snapTo(0f)
            backdropBlurFraction.snapTo(0f)
            shadeRevealProgress.snapTo(0f)
            quoteRevealProgress.snapTo(0f)
            exitAlpha.snapTo(1f)
            dismissOffsetX.snapTo(0f)
            isDragging = false
            onBackdropBlurFraction(0f)
            return@LaunchedEffect
        }

        activeQuote = pickNextSaveQuote()
        dismissOffsetX.snapTo(0f)
        exitAlpha.snapTo(1f)

        val targetRestBlur = when {
            isSeaWave || isCompanionWave -> 0f
            !externalBlurActive -> 0f
            hazeState != null -> 0.08f
            hasThumbnail || hasFreezeMedia -> 0.14f
            else -> 0.42f
        }

        if (animationsEnabled) {
            entryProgress.snapTo(0f)
            morphProgress.snapTo(0f)
            waveProgress.snapTo(0f)
            atmosphereProgress.snapTo(0f)
            backdropBlurFraction.snapTo(0f)
            shadeRevealProgress.snapTo(0f)
            quoteRevealProgress.snapTo(0f)

            if ((isSeaWave || isCompanionWave) && (hasFreezeMedia || isCompanionWave)) {
                if (hasFreezeMedia) {
                    withTimeoutOrNull(240L) {
                        snapshotFlow { latestFrozenFrame }.filterNotNull().first()
                    }
                }
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                entryProgress.snapTo(1f)

                if (isCompanionWave) {
                    val captureWaveStyle = MindDistortionWaveStyle.capture(topDown = isCompanionTopDown)
                    val targetMs = (captureWaveStyle.durationSeconds * 1000f).coerceAtLeast(1800f)
                    val shadeJob = launch {
                        delay(760L)
                        shadeRevealProgress.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(durationMillis = 540, easing = FluidEaseOut),
                        )
                    }
                    val quoteJob = launch {
                        delay(860L)
                        quoteRevealProgress.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(durationMillis = 580, easing = FluidEaseOut),
                        )
                    }
                    var curProgress = 0f
                    var lastFrameNs = 0L
                    while (curProgress < 1f && !dismissedInstantly) {
                        val frameNs = awaitFrame()
                        if (lastFrameNs == 0L) {
                            lastFrameNs = frameNs
                            curProgress = 0.002f
                            waveProgress.snapTo(curProgress)
                        } else {
                            val dtMs = ((frameNs - lastFrameNs) / 1_000_000f).coerceIn(0f, 34f)
                            lastFrameNs = frameNs
                            curProgress = (curProgress + dtMs / targetMs).coerceAtMost(1f)
                            waveProgress.snapTo(curProgress)
                        }
                    }
                    waveProgress.snapTo(1f)
                    morphProgress.snapTo(1f)
                    atmosphereProgress.snapTo(1f)
                    shadeJob.join()
                    quoteJob.join()
                } else {
                    val waveDurationMs = 2150
                    val waveMistJob = launch {
                        atmosphereProgress.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(durationMillis = waveDurationMs, easing = FluidEaseOut),
                        )
                    }
                    val shadeJob = launch {
                        delay(680L)
                        shadeRevealProgress.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(durationMillis = 540, easing = FluidEaseOut),
                        )
                    }
                    val quoteJob = launch {
                        delay(780L)
                        quoteRevealProgress.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(durationMillis = 580, easing = FluidEaseOut),
                        )
                    }

                    waveProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = waveDurationMs,
                            easing = SeaWaveFlowEasing,
                        ),
                    )
                    morphProgress.snapTo(1f)
                    waveMistJob.join()
                    shadeJob.join()
                    quoteJob.join()
                }
            } else {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)

                // Phase 1: Soft backdrop mist + symmetrical hero bloom (decent calm speed)
                val bloomJob = launch {
                    backdropBlurFraction.animateTo(
                        targetValue = if (hasThumbnail) 0.72f else targetRestBlur,
                        animationSpec = tween(durationMillis = 480, easing = FluidEaseOut),
                    )
                }
                launch {
                    atmosphereProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 1500, easing = FluidEaseOut),
                    )
                }

                if (hasThumbnail) {
                    entryProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = 0.80f,
                            stiffness = 175f,
                        ),
                    )
                    delay(360L)
                } else {
                    entryProgress.snapTo(1f)
                }
                bloomJob.join()

                // Phase 2: Smooth glide while bottom shade & quote surface effortlessly
                launch {
                    delay(120L)
                    backdropBlurFraction.animateTo(
                        targetValue = targetRestBlur,
                        animationSpec = tween(durationMillis = 620, easing = FluidEaseOut),
                    )
                }
                val shadeJob = launch {
                    shadeRevealProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 680, easing = FluidEaseOut),
                    )
                }
                val quoteJob = launch {
                    delay(140L)
                    quoteRevealProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 700, easing = FluidEaseOut),
                    )
                }
                morphProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = 0.86f,
                        stiffness = 135f,
                    ),
                )
                shadeJob.join()
                quoteJob.join()
            }

            // Quiet tactile confirmation once the quote rests cleanly
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            } else {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
        } else {
            entryProgress.snapTo(1f)
            morphProgress.snapTo(1f)
            waveProgress.snapTo(1f)
            atmosphereProgress.snapTo(1f)
            backdropBlurFraction.snapTo(targetRestBlur)
            shadeRevealProgress.snapTo(1f)
            quoteRevealProgress.snapTo(1f)
        }

        // Clean reading window with swipe-to-dismiss interaction pause
        var remainingMs = 1100L
        while (remainingMs > 0L && !dismissedInstantly) {
            delay(100L)
            if (!isDragging && abs(dismissOffsetX.value) < 4f) {
                remainingMs -= 100L
            }
        }
        if (dismissedInstantly) return@LaunchedEffect

        // Phase 4: Clean exhale dissolve out without bouncing the wave back to origin
        launch {
            backdropBlurFraction.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 420, easing = FluidDissolveEase),
            )
        }
        exitAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 440, easing = FluidDissolveEase),
        )
        if (!dismissedInstantly) {
            onBackdropBlurFraction(0f)
            MindCaptureSnapshotCache.clear()
            onFinished()
        }
    }

    if (!visible || dismissedInstantly) return

    val colors = MindTheme.colors
    val morph = morphProgress.value.coerceIn(0f, 1f)
    val entry = entryProgress.value.coerceIn(0f, 1.15f)
    val wave = waveProgress.value.coerceIn(0f, 1f)
    val blurFrac = backdropBlurFraction.value.coerceIn(0f, 1f)
    val globalAlpha = exitAlpha.value.coerceIn(0f, 1f)
    val navBottomPx = WindowInsets.navigationBars.getBottom(density).toFloat()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { alpha = globalAlpha },
    ) {
        val containerW = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val containerH = constraints.maxHeight.toFloat().coerceAtLeast(1f)

        // 0. Fullscreen frozen wave layers (Sea Wave OR dsh-go Companion Wave)
        val showFrozenSeaWave = animationsEnabled && isSeaWave && hasFreezeMedia && frozenFrame != null
        if (showFrozenSeaWave) {
            SeaWaveFrozenScreenLayer(
                frame = frozenFrame!!,
                progressProvider = { waveProgress.value },
                alphaProvider = { 1f },
                crestColor = colors.accent,
                modifier = Modifier.fillMaxSize(),
            )
        }
        val showCompanionWave = animationsEnabled && isCompanionWave
        if (showCompanionWave) {
            val captureWaveStyle = remember(isCompanionTopDown) {
                MindDistortionWaveStyle.capture(topDown = isCompanionTopDown)
            }
            MindDistortionWaveScreenCaptureStage(
                progress = wave,
                style = captureWaveStyle,
                frozenBitmap = if (hasFreezeMedia) frozenFrame?.androidBitmap else null,
                accentSource = colors,
                cornerRadiusDp = 50f,
                showLensSnapshot = frozenFrame != null,
                isPreview = false,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // 1. Soft atmospheric backdrop blur only for non-wave styles
        if (!isSeaWave && !isCompanionWave && hazeState != null && blurFrac > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = blurFrac }
                    .hazeEffect(
                        state = hazeState,
                        style = HazeStyle(
                            backgroundColor = Color(0xFF0A0C14),
                            tint = HazeTint(Color.Black.copy(alpha = if (colors.isDark) 0.28f else 0.14f)),
                            blurRadius = 24.dp,
                            noiseFactor = 0.02f,
                        ),
                    ),
            )
        } else if (!isSeaWave && !isCompanionWave && blurFrac > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = (if (colors.isDark) 0.16f else 0.06f) * blurFrac)),
            )
        }

        // 2. Soft ambient glow/wave mist (omitted when full-screen wave already plays)
        if (animationsEnabled && atmosphereProgress.value < 0.99f && style != AnimationStyle.MINIMAL &&
            !(isSeaWave && hasFreezeMedia) && !isCompanionWave
        ) {
            AtmosphericCaptureMist(
                style = style,
                progress = atmosphereProgress.value,
                accentColor = colors.accent,
                secondaryColor = colors.accentSecondary,
                tertiaryColor = colors.accentTertiary,
                isPreview = false,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // 3. Soft bottom gradient shade + bespoke editorial quote reveal
        val swipeThresholdPx = with(density) { 108.dp.toPx() }
        val swipeAlpha = (1f - (abs(dismissOffsetX.value) / (swipeThresholdPx * 2.1f))).coerceIn(0f, 1f)
        val shadeAlpha = shadeRevealProgress.value.coerceIn(0f, 1f) * swipeAlpha
        val quoteFocus = quoteRevealProgress.value.coerceIn(0f, 1f)
        val quoteAlpha = quoteFocus * swipeAlpha
        val exitShiftPx = (1f - globalAlpha) * with(density) { 7.dp.toPx() }
        val quoteShiftPx = (1f - quoteFocus) * with(density) { 18.dp.toPx() } + exitShiftPx
        val quoteBlurPx = with(density) {
            val enterBlur = (1f - quoteFocus) * 3.8.dp.toPx()
            val exitBlur = (1f - globalAlpha) * 3.0.dp.toPx()
            maxOf(enterBlur, exitBlur)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(visible) {
                    detectTapGestures(onTap = { triggerInstantDismiss() })
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { isDragging = true },
                        onHorizontalDrag = { _, dragAmount ->
                            scope.launch {
                                dismissOffsetX.snapTo(dismissOffsetX.value + dragAmount)
                            }
                        },
                        onDragEnd = {
                            isDragging = false
                            scope.launch {
                                if (abs(dismissOffsetX.value) > swipeThresholdPx) {
                                    val target = if (dismissOffsetX.value > 0) containerW else -containerW
                                    dismissOffsetX.animateTo(
                                        targetValue = target,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = 460f,
                                        ),
                                    )
                                    triggerInstantDismiss()
                                } else {
                                    dismissOffsetX.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = 0.78f,
                                            stiffness = 320f,
                                        ),
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                            scope.launch {
                                dismissOffsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(dampingRatio = 0.78f, stiffness = 320f),
                                )
                            }
                        },
                    )
                },
            contentAlignment = Alignment.BottomCenter,
        ) {
            if (shadeAlpha > 0.005f || quoteAlpha > 0.005f) {
                MindBottomQuoteShade(
                    quote = activeQuote,
                    statusLabel = title,
                    shadeAlpha = shadeAlpha,
                    quoteAlpha = quoteAlpha,
                    quoteFocus = quoteFocus,
                    quoteBlurPx = quoteBlurPx,
                    quoteShiftPx = quoteShiftPx,
                    dismissOffsetX = dismissOffsetX.value,
                    accentColor = colors.accent,
                    secondaryColor = colors.accentSecondary,
                    isPreview = false,
                    onTapQuote = onViewMemory,
                    onTapFallback = triggerInstantDismiss,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }

        // 4. Optional floating card glide ONLY for non-wave styles
        if (animationsEnabled && hasThumbnail && !isSeaWave && !isCompanionWave && morph < 0.99f) {
            val startSizePx = with(density) { 184.dp.toPx() }.coerceAtMost(containerW * 0.58f)
            val endSizePx = with(density) { 46.dp.toPx() }
            val startHeightPx = startSizePx * 1.24f
            val endHeightPx = endSizePx

            val entryScale = lerpF(0.86f, 1.0f, entry)
            val glideArcScale = 1f + sin(morph * PI).toFloat() * 0.045f

            val startCenter = Offset(
                x = containerW * 0.5f,
                y = containerH * 0.42f,
            )
            val endCenter = Offset(
                x = containerW * 0.5f,
                y = containerH - navBottomPx - with(density) { 52.dp.toPx() },
            )

            val currentW = lerpF(startSizePx, endSizePx, morph)
            val currentH = lerpF(startHeightPx, endHeightPx, morph)
            val currentCornerDp = lerpF(24f, 19f, morph).dp
            val currentCenterX = lerpF(startCenter.x, endCenter.x, morph)
            val currentCenterY = lerpF(startCenter.y, endCenter.y, morph)
            val entryAlpha = (entry / 0.30f).coerceIn(0f, 1f)
            val handoffAlpha = if (morph > 0.65f) {
                (1f - ((morph - 0.65f) / 0.34f)).coerceIn(0f, 1f)
            } else {
                1f
            }

            Surface(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = (currentCenterX - currentW / 2f).roundToInt(),
                            y = (currentCenterY - currentH / 2f).roundToInt(),
                        )
                    }
                    .size(
                        width = with(density) { currentW.toDp() },
                        height = with(density) { currentH.toDp() },
                    )
                    .graphicsLayer {
                        val combinedScale = entryScale * glideArcScale
                        scaleX = combinedScale
                        scaleY = combinedScale
                        alpha = entryAlpha * handoffAlpha
                    },
                shape = RoundedCornerShape(currentCornerDp),
                color = colors.surfaceElevated,
                border = mindGlassBorder(isDark = true, intensity = 1.2f),
                shadowElevation = lerpF(18f, 2f, morph).dp,
            ) {
                AsyncImage(
                    model = File(thumbnailPath!!),
                    contentDescription = if (title.isNotBlank()) title else null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun MindBottomQuoteShade(
    quote: String,
    @Suppress("UNUSED_PARAMETER") statusLabel: String,
    shadeAlpha: Float,
    quoteAlpha: Float,
    quoteFocus: Float,
    quoteBlurPx: Float,
    quoteShiftPx: Float,
    dismissOffsetX: Float,
    accentColor: Color,
    secondaryColor: Color,
    isPreview: Boolean,
    onTapQuote: (() -> Unit)?,
    onTapFallback: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val focus = quoteFocus.coerceIn(0f, 1f)
    val textScale = lerpF(0.96f, 1f, focus)
    val interactionSource = remember { MutableInteractionSource() }
    val darkMist = Color(0xFF060E1B)
    val tintedMist = lerpColor(darkMist, accentColor, 0.30f)
    val edgeTint = lerpColor(darkMist, secondaryColor, 0.22f)
    val glassTop = Color.White.copy(alpha = 0.96f)
    val glassBottom = lerpColor(Color(0xFFD8E6FF), accentColor, 0.22f).copy(alpha = 0.80f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0.00f to Color.Transparent,
                    0.30f to tintedMist.copy(alpha = 0.22f * shadeAlpha),
                    0.66f to edgeTint.copy(alpha = 0.50f * shadeAlpha),
                    1.00f to darkMist.copy(alpha = 0.74f * shadeAlpha),
                ),
            )
            .then(if (isPreview) Modifier else Modifier.navigationBarsPadding())
            .padding(
                start = if (isPreview) 18.dp else 28.dp,
                end = if (isPreview) 18.dp else 28.dp,
                top = if (isPreview) 22.dp else 44.dp,
                bottom = if (isPreview) 12.dp else 24.dp,
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        val quoteStyle = (if (isPreview) MindTheme.typography.bodySmall else MindTheme.typography.bodyLarge).copy(
            fontSize = if (isPreview) 12.sp else 15.sp,
            lineHeight = if (isPreview) 16.5.sp else 21.5.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.2.sp,
            brush = Brush.verticalGradient(colors = listOf(glassTop, glassBottom)),
            shadow = Shadow(
                color = Color.Black.copy(alpha = 0.56f),
                offset = Offset(0f, 1.5f),
                blurRadius = 14f,
            ),
        )

        Text(
            text = quote,
            style = quoteStyle,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .widthIn(max = if (isPreview) 280.dp else 360.dp)
                .graphicsLayer {
                    translationX = dismissOffsetX
                    translationY = quoteShiftPx
                    scaleX = textScale
                    scaleY = textScale
                    alpha = quoteAlpha
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && quoteBlurPx > 0.4f) {
                        renderEffect = RenderEffect
                            .createBlurEffect(quoteBlurPx, quoteBlurPx, Shader.TileMode.DECAL)
                            .asComposeRenderEffect()
                    }
                }
                .then(
                    (onTapQuote ?: onTapFallback)?.let { quoteTap ->
                        Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = quoteTap,
                        )
                    } ?: Modifier,
                )
                .padding(horizontal = 4.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun SeaWavePreviewMockup(
    waveProgress: Float,
    accentColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier,
) {
    val p = waveProgress.coerceIn(0f, 1f)
    val envIn = smoothstep(0f, 0.14f, p)
    val envOut = 1f - smoothstep(0.56f, 0.80f, p)
    val waveEnv = envIn * envOut
    val fade = if (p <= 0.78f) {
        1f
    } else {
        1f - FluidDissolveEase.transform(((p - 0.78f) / 0.22f).coerceIn(0f, 1f))
    }
    val baseModifier = modifier.graphicsLayer { alpha = fade }
    val shaderModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        baseModifier.then(
            rememberSeaWaveShaderModifier(
                progressProvider = { waveProgress },
                crestColor = accentColor,
            ),
        )
    } else {
        baseModifier
    }

    Box(
        modifier = shaderModifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF121829),
                        Color(0xFF19233D),
                        Color(0xFF0E1526),
                    ),
                ),
            ),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRoundRect(
                color = Color.White.copy(alpha = 0.08f),
                topLeft = Offset(w * 0.08f, h * 0.12f),
                size = androidx.compose.ui.geometry.Size(w * 0.28f, 10.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()),
            )
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.38f),
                        secondaryColor.copy(alpha = 0.30f),
                    ),
                ),
                topLeft = Offset(w * 0.08f, h * 0.22f),
                size = androidx.compose.ui.geometry.Size(w * 0.84f, h * 0.32f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx()),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.14f),
                topLeft = Offset(w * 0.08f, h * 0.60f),
                size = androidx.compose.ui.geometry.Size(w * 0.62f, 8.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.09f),
                topLeft = Offset(w * 0.08f, h * 0.69f),
                size = androidx.compose.ui.geometry.Size(w * 0.44f, 7.dp.toPx()),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.5.dp.toPx()),
            )

            // Descending top-to-bottom sea crest shimmer for API 26-32 preview
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU && waveEnv > 0.02f) {
                val bandY = (-0.14f + p * 1.32f) * h
                val halfBand = 34.dp.toPx()
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            accentColor.copy(alpha = 0.24f * waveEnv),
                            Color.White.copy(alpha = 0.12f * waveEnv),
                            Color.Transparent,
                        ),
                        startY = bandY - halfBand,
                        endY = bandY + halfBand,
                    ),
                )
            }
        }
    }
}

/**
 * Interactive preview box in Settings matching the calm bottom-shade & quote overlay.
 */
@Composable
fun MindAnimationPreviewBox(
    style: AnimationStyle,
    replayTrigger: Int,
    modifier: Modifier = Modifier,
) {
    val entry = remember { Animatable(0f) }
    val morph = remember { Animatable(0f) }
    val wave = remember { Animatable(0f) }
    val atmosphere = remember { Animatable(0f) }
    val shadeReveal = remember { Animatable(0f) }
    val quoteReveal = remember { Animatable(0f) }
    val density = LocalDensity.current

    LaunchedEffect(style, replayTrigger) {
        entry.snapTo(0f)
        morph.snapTo(0f)
        wave.snapTo(0f)
        atmosphere.snapTo(0f)
        shadeReveal.snapTo(0f)
        quoteReveal.snapTo(0f)

        if (style == AnimationStyle.SEA_WAVE ||
            style == AnimationStyle.COMPANION_WAVE ||
            style == AnimationStyle.COMPANION_WAVE_TOP_DOWN
        ) {
            entry.snapTo(1f)
            val isComp = style == AnimationStyle.COMPANION_WAVE ||
                style == AnimationStyle.COMPANION_WAVE_TOP_DOWN
            val previewWaveStyle = MindDistortionWaveStyle.capture(
                topDown = style == AnimationStyle.COMPANION_WAVE_TOP_DOWN,
            )
            val compDurationMs = (previewWaveStyle.durationSeconds * 1000f).roundToInt()
            launch {
                atmosphere.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = if (isComp) compDurationMs else 2100, easing = FluidEaseOut),
                )
            }
            val shadeJob = launch {
                delay(if (isComp) 720L else 640L)
                shadeReveal.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 520, easing = FluidEaseOut),
                )
            }
            val quoteJob = launch {
                delay(if (isComp) 820L else 740L)
                quoteReveal.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 540, easing = FluidEaseOut),
                )
            }
            wave.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = if (isComp) compDurationMs else 2100,
                    easing = if (isComp) androidx.compose.animation.core.LinearEasing else SeaWaveFlowEasing,
                ),
            )
            morph.snapTo(1f)
            shadeJob.join()
            quoteJob.join()
        } else {
            launch {
                atmosphere.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 1500, easing = FluidEaseOut),
                )
            }
            entry.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.80f, stiffness = 175f),
            )
            delay(360L)
            val shadeJob = launch {
                shadeReveal.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 680, easing = FluidEaseOut),
                )
            }
            val quoteJob = launch {
                delay(140L)
                quoteReveal.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 700, easing = FluidEaseOut),
                )
            }
            morph.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.86f, stiffness = 135f),
            )
            shadeJob.join()
            quoteJob.join()
        }
    }

    val colors = MindTheme.colors
    val p = morph.value.coerceIn(0f, 1f)
    val e = entry.value.coerceIn(0f, 1.15f)
    val wProg = wave.value.coerceIn(0f, 1f)
    val isSeaWave = style == AnimationStyle.SEA_WAVE
    val isCompanionTopDown = style == AnimationStyle.COMPANION_WAVE_TOP_DOWN
    val isCompanionWave = style == AnimationStyle.COMPANION_WAVE || isCompanionTopDown
    val previewShape = MindTheme.shapes.large

    val previewMockBitmap = remember(colors.accent, colors.accentSecondary, isCompanionWave) {
        if (isCompanionWave) createWavePreviewMockBitmap(colors.accent, colors.accentSecondary) else null
    }
    androidx.compose.runtime.DisposableEffect(previewMockBitmap) {
        onDispose {
            previewMockBitmap?.let { if (!it.isRecycled) it.recycle() }
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = previewShape,
        color = Color(0xFF141B29),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .clip(previewShape),
        ) {
            val w = constraints.maxWidth.toFloat().coerceAtLeast(1f)
            val h = constraints.maxHeight.toFloat().coerceAtLeast(1f)

            // Ambient color field
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF182032),
                                colors.accent.copy(alpha = 0.24f),
                                colors.accentSecondary.copy(alpha = 0.18f),
                                Color(0xFF111622),
                            ),
                        ),
                    ),
            ) {
                if (isSeaWave) {
                    SeaWavePreviewMockup(
                        waveProgress = wProg,
                        accentColor = colors.accent,
                        secondaryColor = colors.accentSecondary,
                    )
                }
                if (isCompanionWave) {
                    MindDistortionWaveScreenCaptureStage(
                        progress = wProg,
                        style = MindDistortionWaveStyle.capture(topDown = isCompanionTopDown),
                        frozenBitmap = previewMockBitmap,
                        accentSource = colors,
                        cornerRadiusDp = 20f,
                        showLensSnapshot = true,
                        isPreview = true,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                if (style != AnimationStyle.MINIMAL && !isSeaWave && !isCompanionWave) {
                    AtmosphericCaptureMist(
                        style = style,
                        progress = atmosphere.value,
                        accentColor = colors.accent,
                        secondaryColor = colors.accentSecondary,
                        tertiaryColor = colors.accentTertiary,
                        isPreview = true,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            // Soft bottom gradient shade + centered editorial quote (no pill, no screenshot badge)
            val qFocus = quoteReveal.value.coerceIn(0f, 1f)
            val qBlurPx = with(density) { (1f - qFocus) * 3.0.dp.toPx() }
            MindBottomQuoteShade(
                quote = SAVE_QUOTES[replayTrigger.mod(SAVE_QUOTES.size)],
                statusLabel = "Saved to Mind",
                shadeAlpha = shadeReveal.value.coerceIn(0f, 1f),
                quoteAlpha = qFocus,
                quoteFocus = qFocus,
                quoteBlurPx = qBlurPx,
                quoteShiftPx = (1f - qFocus) * with(density) { 12.dp.toPx() },
                dismissOffsetX = 0f,
                accentColor = colors.accent,
                secondaryColor = colors.accentSecondary,
                isPreview = true,
                onTapQuote = null,
                onTapFallback = null,
                modifier = Modifier.align(Alignment.BottomCenter),
            )

            // Mini card glide preview ONLY for non-wave styles
            if (!isSeaWave && !isCompanionWave && p < 0.99f) {
                val startW = with(density) { 68.dp.toPx() }
                val startH = with(density) { 84.dp.toPx() }
                val endSize = with(density) { 30.dp.toPx() }
                val curW = lerpF(startW, endSize, p)
                val curH = lerpF(startH, endSize, p)
                val startCenter = Offset(w / 2f, h * 0.40f)
                val endCenter = Offset(
                    x = w / 2f,
                    y = h - with(density) { 34.dp.toPx() },
                )
                val curCenterX = lerpF(startCenter.x, endCenter.x, p)
                val curCenterY = lerpF(startCenter.y, endCenter.y, p)
                val cardAlpha = (e / 0.35f).coerceIn(0f, 1f) *
                    if (p > 0.65f) (1f - (p - 0.65f) / 0.34f).coerceIn(0f, 1f) else 1f

                Surface(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = (curCenterX - curW / 2f).roundToInt(),
                                y = (curCenterY - curH / 2f).roundToInt(),
                            )
                        }
                        .size(
                            width = with(density) { curW.toDp() },
                            height = with(density) { curH.toDp() },
                        )
                        .graphicsLayer {
                            val s = lerpF(0.86f, 1f, e)
                            scaleX = s
                            scaleY = s
                            alpha = cardAlpha
                        },
                    shape = RoundedCornerShape(lerpF(14f, 11f, p).dp),
                    color = colors.accent.copy(alpha = 0.28f),
                    shadowElevation = 0.dp,
                    border = mindGlassBorder(isDark = true, intensity = 1.1f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(lerpF(18f, 11f, p).dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Soft, emotional diffused atmospheric light halos behind the capture card.
 */
@Composable
private fun AtmosphericCaptureMist(
    style: AnimationStyle,
    progress: Float,
    accentColor: Color,
    secondaryColor: Color,
    tertiaryColor: Color,
    isPreview: Boolean,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h * 0.42f)
        val dockTarget = Offset(w / 2f, h * if (isPreview) 0.80f else 0.88f)
        val envelope = sin(progress.coerceIn(0f, 1f) * PI).toFloat().coerceIn(0f, 1f)

        when (style) {
            AnimationStyle.RIPPLE -> {
                val ringRadius = (size.minDimension * (0.18f + progress * 0.62f)).coerceAtLeast(18f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Transparent,
                            accentColor.copy(alpha = 0.22f * envelope),
                            secondaryColor.copy(alpha = 0.12f * envelope),
                            Color.Transparent,
                        ),
                        center = center,
                        radius = ringRadius * 1.45f,
                    ),
                    radius = ringRadius * 1.45f,
                    center = center,
                )
            }

            AnimationStyle.SEA_WAVE -> {
                val waveCount = 3
                val segments = 28
                val sweepProgress = SeaWaveFlowEasing.transform(progress.coerceIn(0f, 1f))
                for (band in 0 until waveCount) {
                    val travel = (sweepProgress * 1.18f - band * 0.22f).coerceIn(0f, 1f)
                    val bandEnv = sin(travel * PI.toFloat()).coerceAtLeast(0f)
                    if (bandEnv > 0.01f) {
                        val baseY = lerpF(-h * 0.06f, h * 0.96f, travel)
                        val amp = (if (isPreview) 6f else 15f) * bandEnv * (1f - band * 0.16f)
                        val phase = sweepProgress * 3.8f + band * 1.1f
                        val wavePath = Path()
                        for (step in 0..segments) {
                            val frac = step.toFloat() / segments
                            val x = frac * w
                            val archOffset = -cos((frac - 0.5f) * PI.toFloat()) * (if (isPreview) 5f else 14f)
                            val y = baseY + archOffset + sin(frac * PI.toFloat() * 1.8f + phase) * amp
                            if (step == 0) wavePath.moveTo(x, y) else wavePath.lineTo(x, y)
                        }
                        val crestColor = if (band % 2 == 0) accentColor else secondaryColor
                        drawPath(
                            path = wavePath,
                            color = crestColor.copy(alpha = (0.16f - band * 0.03f) * bandEnv),
                            style = Stroke(width = if (isPreview) 1.75f else 3.5f),
                        )
                    }
                }
            }

            AnimationStyle.PORTAL -> {
                val glowR = (size.minDimension * 0.68f).coerceAtLeast(24f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            secondaryColor.copy(alpha = 0.28f * envelope),
                            accentColor.copy(alpha = 0.12f * envelope),
                            Color.Transparent,
                        ),
                        center = dockTarget,
                        radius = glowR,
                    ),
                    radius = glowR,
                    center = dockTarget,
                )
            }

            AnimationStyle.MORPH -> {
                val haloR = (size.maxDimension * 0.62f).coerceAtLeast(24f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.18f * envelope),
                            Color.Transparent,
                        ),
                        center = center,
                        radius = haloR,
                    ),
                    radius = haloR,
                    center = center,
                )
            }

            AnimationStyle.PARTICLE -> {
                val moteCount = 10
                val baseRadius = size.minDimension * (if (isPreview) 0.20f else 0.30f)
                for (i in 0 until moteCount) {
                    val angle = (i * 36.0 + progress * 22.0) * PI / 180.0
                    val drift = (1f - progress) * baseRadius * (0.7f + (i % 3) * 0.22f)
                    val mx = lerpF(center.x + cos(angle).toFloat() * drift, dockTarget.x, progress * 0.85f)
                    val my = lerpF(center.y + sin(angle).toFloat() * drift, dockTarget.y, progress * 0.85f)
                    val moteRadius = if (isPreview) 16f else 32f
                    val tint = if (i % 2 == 0) secondaryColor else accentColor
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                tint.copy(alpha = 0.28f * envelope),
                                Color.Transparent,
                            ),
                            center = Offset(mx, my),
                            radius = moteRadius,
                        ),
                        radius = moteRadius,
                        center = Offset(mx, my),
                    )
                }
            }

            AnimationStyle.LIQUID -> {
                val topWashR = (size.maxDimension * 0.60f).coerceAtLeast(24f)
                val botWashR = (size.maxDimension * 0.56f).coerceAtLeast(24f)
                val topOrigin = Offset(w * (0.24f + progress * 0.36f), h * 0.14f)
                val botOrigin = Offset(w * (0.76f - progress * 0.30f), h * 0.86f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.20f * envelope),
                            Color.Transparent,
                        ),
                        center = topOrigin,
                        radius = topWashR,
                    ),
                    radius = topWashR,
                    center = topOrigin,
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            tertiaryColor.copy(alpha = 0.18f * envelope),
                            Color.Transparent,
                        ),
                        center = botOrigin,
                        radius = botWashR,
                    ),
                    radius = botWashR,
                    center = botOrigin,
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.06f * envelope),
                    radius = (size.minDimension * 0.38f * (0.8f + progress * 0.3f)).coerceAtLeast(12f),
                    center = center,
                    style = Stroke(width = if (isPreview) 1f else 2f),
                )
            }

            AnimationStyle.COMPANION_WAVE,
            AnimationStyle.COMPANION_WAVE_TOP_DOWN,
            AnimationStyle.MINIMAL,
            -> Unit
        }
    }
}

private fun createWavePreviewMockBitmap(
    accentColor: Color,
    secondaryColor: Color,
): Bitmap {
    val w = 360
    val h = 216
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    bgPaint.shader = android.graphics.LinearGradient(
        0f,
        0f,
        w.toFloat(),
        h.toFloat(),
        intArrayOf(0xFF1A253B.toInt(), 0xFF0E1729.toInt()),
        null,
        Shader.TileMode.CLAMP,
    )
    canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

    val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    cardPaint.shader = android.graphics.LinearGradient(
        28f,
        42f,
        w - 28f,
        120f,
        intArrayOf(accentColor.copy(alpha = 0.55f).toArgb(), secondaryColor.copy(alpha = 0.45f).toArgb()),
        null,
        Shader.TileMode.CLAMP,
    )
    canvas.drawRoundRect(28f, 44f, w - 28f, 118f, 18f, 18f, cardPaint)

    val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x45DCE8FF }
    canvas.drawRoundRect(28f, 134f, w * 0.72f, 148f, 7f, 7f, barPaint)
    barPaint.color = 0x2BDCE8FF
    canvas.drawRoundRect(28f, 158f, w * 0.50f, 170f, 6f, 6f, barPaint)
    return bmp
}
