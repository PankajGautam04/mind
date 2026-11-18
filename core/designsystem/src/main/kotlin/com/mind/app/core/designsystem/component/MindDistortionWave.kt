package com.mind.app.core.designsystem.component

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Paint
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.mind.app.core.designsystem.theme.MindColors
import com.mind.app.core.designsystem.theme.MindTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Faithful Android/Compose + AGSL port of dsh-go's DistortionWave system:
 * signed-distance wave front emerging from an anchor rect toward the tap point, progressive
 * 3-tier blur + asymmetric optical lens lift + spectral aberration + OKLab prismatic band +
 * breathing perimeter rim & crisp white glass hairline + 64 twinkling starlight sparks.
 */
enum class MindWaveType {
    VERTICAL,
    VERTICAL_TOP_DOWN,
    HORIZONTAL,
    CENTER,
}

class MindDistortionWaveEasing(
    private val x1: Float,
    private val y1: Float,
    private val x2: Float,
    private val y2: Float,
) {
    fun transform(x: Float): Float {
        if (x <= 0f) return 0f
        if (x >= 1f) return 1f
        var low = 0f
        var high = 1f
        repeat(24) {
            val s = (low + high) * 0.5f
            if (bezier(s, x1, x2) < x) {
                low = s
            } else {
                high = s
            }
        }
        return bezier((low + high) * 0.5f, y1, y2)
    }

    private fun bezier(s: Float, a: Float, b: Float): Float {
        val r = 1f - s
        return 3f * r * r * s * a + 3f * r * s * s * b + s * s * s
    }
}

data class MindDistortionWavePalette(
    val colors: List<Color>,
    val ahead: Color,
    val behind: Color,
) {
    companion object {
        fun fromMindColors(mindColors: MindColors): MindDistortionWavePalette {
            val hsl = FloatArray(3)
            android.graphics.Color.colorToHSV(mindColors.accent.toArgb(), hsl)
            val hue = hsl[0]
            val baseSat = if (mindColors.isDark) 0.42f else 0.46f
            val baseLight = if (mindColors.isDark) 0.70f else 0.64f

            fun tone(shift: Float, s: Float, lightness: Float): Color {
                val h = ((hue + shift) % 360f + 360f) % 360f
                val argb = androidx.core.graphics.ColorUtils.HSLToColor(
                    floatArrayOf(h, s.coerceIn(0f, 1f), lightness.coerceIn(0f, 1f)),
                )
                return Color(argb)
            }

            return MindDistortionWavePalette(
                colors = listOf(
                    tone(-30f, baseSat * 0.9f, baseLight + 0.06f),
                    tone(14f, baseSat, baseLight - 0.04f),
                    tone(-8f, baseSat * 1.1f, baseLight - 0.08f),
                    tone(32f, baseSat * 0.85f, baseLight + 0.05f),
                    tone(-18f, baseSat, baseLight),
                    tone(6f, baseSat * 0.9f, baseLight + 0.08f),
                    tone(24f, baseSat * 1.05f, baseLight - 0.03f),
                ),
                ahead = tone(-22f, baseSat * 0.8f, baseLight + 0.12f),
                behind = tone(20f, baseSat * 1.05f, baseLight - 0.12f),
            )
        }
    }
}

data class MindDistortionWaveStyle(
    val type: MindWaveType,
    val anticipation: Float = 0.50f,
    val travel: Float = 2.28f,
    val launchGap: Float = 0f,
    val linger: Float = -0.35f,
    val rimFade: Float = 3.03f,
    val rimEasing: MindDistortionWaveEasing = MindDistortionWaveEasing(0.42f, 0f, 0.58f, 1f),
    val returnSpeed: Float = 1.45f,
    val easing: MindDistortionWaveEasing = MindDistortionWaveEasing(0.22f, 1f, 0.36f, 1f),
    val pull: Float = 24f,
    val inhale: Float = 56f,
    val gatherGlow: Float = 0.33f,
    val gatherDots: Float = 1f,
    val amplitude: Float = 34f,
    val lensWidth: Float = 150f,
    val aberration: Float = 0.62f,
    val swell: Float = 22f,
    val blur: Float = 18f,
    val blurAhead: Float = 90f,
    val blurBehind: Float = 240f,
    val lead: Float = 130f,
    val tail: Float = 300f,
    val strength: Float = 0.92f,
    val rimWidth: Float = 14f,
    val rimUnevenness: Float = 1f,
    val rimStrength: Float = 0.82f,
    val rimSoftness: Float = 343f,
    val cornerRadius: Float = 50f,
    val rimPulse: Float = 0.6f,
    val hairlineWidth: Float = 1.5f,
    val hairlineOpacity: Float = 0.9f,
    val hairlineWhite: Float = 0.95f,
    val hairlineLinger: Float = 0.47f,
    val hairlineFade: Float = 3f,
    val orientation: Float = 0.91f,
    val particleCount: Int = 64,
    val particleSize: Float = 1.8f,
    val particleReach: Float = 150f,
    val particleFlight: Float = 30f,
    val particleGap: Float = 24f,
    val particleLife: Float = 2.19f,
    val particleSpread: Float = 1f,
) {
    val durationSeconds: Float
        get() = anticipation + max(
            max(travel * 0.6f, linger + rimFade),
            max(linger + hairlineLinger + hairlineFade, particleLife),
        )

    companion object {
        val Vertical = MindDistortionWaveStyle(type = MindWaveType.VERTICAL)
        val VerticalTopDown = MindDistortionWaveStyle(type = MindWaveType.VERTICAL_TOP_DOWN)

        fun capture(topDown: Boolean = false): MindDistortionWaveStyle = MindDistortionWaveStyle(
            type = if (topDown) MindWaveType.VERTICAL_TOP_DOWN else MindWaveType.VERTICAL,
            anticipation = 0.44f,
            travel = 2.08f,
            linger = -0.30f,
            rimFade = 2.40f,
            hairlineLinger = 0.34f,
            hairlineFade = 2.38f,
            particleLife = 1.92f,
            returnSpeed = 1.85f,
            aberration = 0.65f,
            cornerRadius = 50f,
        )

        val Center = MindDistortionWaveStyle(
            type = MindWaveType.CENTER,
            anticipation = 0.25f,
            travel = 3.43f,
            rimFade = 2.51f,
            hairlineLinger = 0.96f,
            strength = 0.6f,
            rimStrength = 0.6f,
            aberration = 0f,
            gatherGlow = 0f,
            gatherDots = 0f,
            particleCount = 89,
            particleLife = 3.93f,
            particleSpread = 0.64f,
            cornerRadius = 42f,
            pull = 14.4f,
            inhale = 33.6f,
            amplitude = 20.4f,
            lensWidth = 90f,
            swell = 13.2f,
            blur = 10.8f,
            blurAhead = 54f,
            blurBehind = 144f,
            lead = 78f,
            tail = 180f,
            rimSoftness = 205.8f,
            particleReach = 90f,
            particleFlight = 18f,
            particleGap = 11f,
        )
    }
}

fun defaultWaveSourceDp(type: MindWaveType, widthDp: Float, heightDp: Float): Rect = when (type) {
    MindWaveType.VERTICAL -> Rect(
        left = 20f,
        top = (heightDp - 96f).coerceAtLeast(heightDp * 0.72f),
        right = (widthDp - 20f).coerceAtLeast(60f),
        bottom = (heightDp - 38f).coerceAtLeast(heightDp * 0.85f),
    )
    MindWaveType.VERTICAL_TOP_DOWN -> Rect(
        left = 20f,
        top = -6f,
        right = (widthDp - 20f).coerceAtLeast(60f),
        bottom = 54f,
    )
    MindWaveType.HORIZONTAL -> Rect(
        left = 0f,
        top = heightDp * 0.343f,
        right = 24f,
        bottom = heightDp * 0.6565f,
    )
    MindWaveType.CENTER -> {
        val cx = widthDp * 0.5f
        val cy = heightDp * 0.5f
        Rect(cx - 32f, cy - 32f, cx + 32f, cy + 32f)
    }
}

internal class MindWaveFront(
    val origin: Offset,
    val tilt: Float,
    val spine: Float,
    val radius: Float,
    val moved: Float,
    val squash: Float,
    val time: Float,
    val energy: Float,
    val swell: Float,
    val size: Size,
    val gravity: Float,
    val glow: Float,
    val clearRadius: Float,
    val hairClearRadius: Float,
    val rim: Float,
    val fallen: Float,
    val sparkle: Float,
    val flying: Float,
    val launched: Boolean,
    val hasCleared: Boolean,
    val ring: Boolean,
    val sideways: Boolean,
    val topDown: Boolean,
    val hold: Float,
) {
    companion object {
        const val COVER_ZONE = 60f
        private const val ARC_SQUASH = 1.4f
        private const val RELEASE_SEC = 0.22f
        private const val GATHERED_GLOW_CAP = 0.5f
        private const val OVERSHOOT = 100f
        private const val MAX_TILT = 0.45f
        private const val SETTLE_START = 0.2f
        private const val SETTLE_END = 0.55f

        fun smooth(a: Float, b: Float, x: Float): Float {
            val t = ((x - a) / (b - a)).coerceIn(0f, 1f)
            return t * t * (3f - 2f * t)
        }

        fun compute(
            progress: Float,
            sourceDp: Rect,
            sizeDp: Size,
            style: MindDistortionWaveStyle,
            focusDp: Offset? = null,
        ): MindWaveFront {
            val duration = style.durationSeconds
            val t = progress.coerceIn(0f, 1f) * duration
            val sinceLaunch = t - style.anticipation
            val gathered = smooth(0f, max(style.anticipation, 0.001f), t)
            val x = (sinceLaunch / style.travel).coerceIn(0f, 1f)
            val moved = style.easing.transform(x)

            val ring = style.type == MindWaveType.CENTER
            val sideways = style.type == MindWaveType.HORIZONTAL
            val topDown = style.type == MindWaveType.VERTICAL_TOP_DOWN
            val fullSquash = if (ring) 1f else ARC_SQUASH
            val startRadius = min(sourceDp.width, sourceDp.height) * 0.5f
            val length = if (sideways) sourceDp.height else sourceDp.width
            val startSpine = if (ring) 0f else max(0f, length * 0.5f - startRadius)
            val turned = when (style.type) {
                MindWaveType.HORIZONTAL -> (PI * 0.5).toFloat()
                MindWaveType.VERTICAL_TOP_DOWN -> PI.toFloat()
                else -> 0f
            }
            val axisX = cos(turned)
            val axisY = sin(turned)
            val centre = sourceDp.center

            var lean = 0f
            if (!ring && focusDp != null) {
                val off = when {
                    sideways -> focusDp.y - centre.y
                    topDown -> centre.x - focusDp.x
                    else -> focusDp.x - centre.x
                }
                lean = (off / max(length * 0.5f, 1f)).coerceIn(-1f, 1f)
            }
            val fullShift = lean * length * 0.5f * style.orientation
            val fullLean = lean * style.orientation * MAX_TILT
            val fullTilt = turned + fullLean
            val opening = smooth(0f, 0.45f, moved)

            val cs = cos(fullTilt)
            val sn = sin(fullTilt)
            var reach = 0f
            val corners = arrayOf(
                Offset.Zero,
                Offset(sizeDp.width, 0f),
                Offset(0f, sizeDp.height),
                Offset(sizeDp.width, sizeDp.height),
            )
            for (corner in corners) {
                val dx = corner.x - centre.x - fullShift * axisX
                val dy = corner.y - centre.y - fullShift * axisY
                reach = max(reach, hypot((cs * dx + sn * dy) * fullSquash, -sn * dx + cs * dy))
            }

            val crossed = reach + (style.swell + COVER_ZONE) * fullSquash
            val settling = 1f - smooth(SETTLE_START, SETTLE_END, x)

            val launchRadius = startRadius + max(style.launchGap, 0f)
            val radius = if (sinceLaunch < 0f) {
                launchRadius + style.inhale * (1f - gathered)
            } else {
                launchRadius + (crossed + OVERSHOOT - launchRadius) * moved
            }
            val gatheredGlow = min(GATHERED_GLOW_CAP * max(style.gatherGlow, 0f), 1f)
            val clearStart = startRadius - style.rimSoftness
            val clearEnd = reach + (style.rimSoftness + style.swell) * fullSquash
            val falling = min(t / max(style.anticipation, 0.001f), 1f)

            return MindWaveFront(
                origin = Offset(centre.x + fullShift * opening * axisX, centre.y + fullShift * opening * axisY),
                tilt = turned + fullLean * opening,
                spine = startSpine * (1f - opening),
                radius = radius,
                moved = moved,
                squash = 1f + (fullSquash - 1f) * smooth(0f, 0.5f, moved),
                time = t,
                energy = smooth(0f, 0.05f, x) * settling,
                swell = style.swell * smooth(0f, 0.3f, moved),
                size = sizeDp,
                gravity = style.pull * gathered * (1f - smooth(0f, RELEASE_SEC, sinceLaunch)),
                glow = if (sinceLaunch < 0f) {
                    gatheredGlow * gathered
                } else {
                    (gatheredGlow + (1f - gatheredGlow) * smooth(0f, 0.07f, x)) * settling
                },
                clearRadius = clearStart + (clearEnd - clearStart) * style.rimEasing.transform(
                    (sinceLaunch - style.linger) / max(style.rimFade, 0.01f),
                ),
                hairClearRadius = clearStart + (clearEnd - clearStart) * style.rimEasing.transform(
                    (sinceLaunch - style.linger - style.hairlineLinger) / max(style.hairlineFade, 0.01f),
                ),
                rim = style.rimStrength * smooth(0f, 0.1f, x) * (1f - smooth(0.9f, 1f, t / duration)),
                fallen = falling * falling,
                sparkle = smooth(0f, max(style.anticipation, 0.001f) * 0.35f, t),
                flying = max(sinceLaunch, 0f),
                launched = sinceLaunch > 0f,
                hasCleared = radius > crossed,
                ring = ring,
                sideways = sideways,
                topDown = topDown,
                hold = if (ring) startRadius else 0f,
            )
        }
    }
}

private object MindWaveRim {
    const val COUNT = 7

    fun computeLumps(
        out: FloatArray,
        time: Float,
        sizeDp: Size,
        cornerRadius: Float,
        pulse: Float,
    ) {
        val ex = max(sizeDp.width * 0.5f - cornerRadius, 0f)
        val ey = max(sizeDp.height * 0.5f - cornerRadius, 0f)
        val perimeter = 4f * (ex + ey + 0.5f * PI.toFloat() * cornerRadius)
        val n = COUNT.toFloat()
        for (i in 0 until COUNT) {
            val fi = i.toFloat()
            val a = hash(fi, 12.9898f, 1.3f)
            val b = hash(fi, 78.233f, 4.1f)
            val c = hash(fi, 39.425f, 2.7f)
            val breath = (0.25f + 0.3f * pulse) * sin(time * (1.1f + 1.2f * a) * (1f + pulse) + fi * 2.4f)
            out[i * 3] = perimeter * (fi + 0.35f * a) / n + time * (-22f + 44f * b) * (1f + 2f * pulse)
            out[i * 3 + 1] = perimeter / n * (0.2f + 0.25f * c)
            out[i * 3 + 2] = (0.2f + 2.6f * b) * (1f + breath)
        }
    }

    private fun hash(i: Float, a: Float, b: Float): Float {
        val x = sin(i * a + b) * 43758.5453f
        return x - floor(x)
    }
}

internal class MindWaveParticle private constructor(
    val index: Float,
    val column: Float,
    val heading: Float,
    val pace: Float,
    val drift: Float,
    val sway: Float,
    val swayRate: Float,
    val twinkleRate: Float,
    val phase: Float,
    val life: Float,
    val size: Float,
    val brightness: Float,
    val hop: Float,
    val hopOffset: Float,
    private val reach: Float,
    private val flight: Float,
    private val gap: Float,
    private val anticipation: Float,
    private val gatherDots: Float,
    private val spread: Float,
) {
    fun pointDp(front: MindWaveFront, sourceDp: Rect): Offset {
        if (front.ring) return if (front.launched) ringOutflow(front, sourceDp) else ringInflow(front, sourceDp)
        return if (front.launched) outflow(front, sourceDp) else inflow(front, sourceDp)
    }

    fun opacity(front: MindWaveFront): Float {
        val twinkle = 0.5f + 0.5f * sin(front.time * twinkleRate + phase)
        val glint = brightness * (0.7f + 0.3f * twinkle)
        if (!front.launched) {
            val along = hopAt(front.time).second
            val hopping = MindWaveFront.smooth(0f, 0.25f, along) * (1f - MindWaveFront.smooth(0.55f, 1f, along))
            val ending = 1f - MindWaveFront.smooth(anticipation * 0.75f, anticipation, front.time)
            return front.sparkle * hopping * ending * glint * gatherDots
        }
        val burning = 1f - MindWaveFront.smooth(life * 0.55f, life, front.flying)
        return burning * glint
    }

    private fun hopAt(time: Float): Pair<Float, Float> {
        val hops = time / hop + hopOffset
        val count = floor(hops)
        return count to (hops - count)
    }

    private fun onSource(source: Rect, sideways: Boolean, topDown: Boolean, along: Float, out: Float): Offset = when {
        sideways -> Offset(source.center.x + out, source.center.y + along)
        topDown -> Offset(source.center.x + along, source.center.y + out)
        else -> Offset(source.center.x + along, source.center.y - out)
    }

    private fun inflow(front: MindWaveFront, source: Rect): Offset {
        val (count, along) = hopAt(front.time)
        val half = (if (front.sideways) source.height else source.width) * 0.5f
        val face = (if (front.sideways) source.width else source.height) * 0.5f
        val x = (hash(index, count, 1f) * 2f - 1f) * (half + 24f)
        val nearest = gap + flight
        val away = nearest + max(reach - nearest, 0f) * hash(index, count, 2f)
        val start = onSource(source, front.sideways, front.topDown, x, face + away)
        val corner = min(source.width, source.height) * 0.5f
        val target = onSource(source, front.sideways, front.topDown, x.coerceIn(-half + corner, half - corner), face)
        val dx = target.x - start.x
        val dy = target.y - start.y
        val length = max(sqrt(dx * dx + dy * dy), 1f)
        val travelled = flight * along * along
        return Offset(start.x + dx / length * travelled, start.y + dy / length * travelled)
    }

    private fun outflow(front: MindWaveFront, source: Rect): Offset {
        val start = min(source.width, source.height) * 0.5f
        val widening = 1f + FAN * front.moved * pace * spread
        val swaying = sway * sin(swayRate * front.flying + phase) * min(front.flying / 0.25f, 1f)
        val sideLen = if (front.sideways) source.height else source.width
        val x = column * sideLen * 0.5f * widening + swaying
        val height = (start + (front.radius - start) * pace * spread) * (1f - BOW * column * column) +
            drift * front.flying * spread
        val cs = cos(front.tilt)
        val sn = sin(front.tilt)
        return Offset(front.origin.x + cs * x + sn * height, front.origin.y + sn * x - cs * height)
    }

    private fun ringInflow(front: MindWaveFront, source: Rect): Offset {
        val (count, along) = hopAt(front.time)
        val angle = hash(index, count, 1f) * 2f * PI.toFloat()
        val nearest = min(source.width, source.height) * 0.5f + gap + flight
        val away = nearest + max(reach - nearest, 0f) * hash(index, count, 2f)
        val distance = away - flight * along * along
        return Offset(source.center.x + cos(angle) * distance, source.center.y + sin(angle) * distance)
    }

    private fun ringOutflow(front: MindWaveFront, source: Rect): Offset {
        val edge = min(source.width, source.height) * 0.5f
        val distance = edge + ((front.radius - edge) * pace + drift * front.flying) * spread
        val swaying = sway * sin(swayRate * front.flying + phase) * min(front.flying / 0.25f, 1f)
        val dx = cos(heading)
        val dy = sin(heading)
        return Offset(front.origin.x + dx * distance - dy * swaying, front.origin.y + dy * distance + dx * swaying)
    }

    companion object {
        private const val FAN = 0.22f
        private const val BOW = 0.30f

        fun create(index: Int, style: MindDistortionWaveStyle): MindWaveParticle {
            val i = (index + 1).toFloat()
            fun scatter(step: Float): Float = (i * step) % 1f
            val small = scatter(0.5497005f)
            return MindWaveParticle(
                index = i,
                column = (scatter(0.7548777f) * 2f - 1f) * 0.92f,
                heading = scatter(0.7548777f) * 2f * PI.toFloat(),
                pace = 0.35f + 0.72f * sqrt(scatter(0.8191725f)),
                drift = 15f + 50f * scatter(0.6710436f),
                sway = 3f + 11f * scatter(0.4142136f),
                swayRate = 2.5f + 3.5f * scatter(0.7320508f),
                twinkleRate = 8f + 12f * scatter(0.236068f),
                phase = scatter(0.618034f) * 2f * PI.toFloat(),
                life = style.particleLife * (0.4f + 0.6f * scatter(0.6457513f)),
                size = style.particleSize * (0.55f + 0.45f * small * small),
                brightness = 0.3f + 0.7f * scatter(0.6055513f),
                hop = 0.16f + 0.14f * scatter(0.1231056f),
                hopOffset = scatter(0.472136f),
                reach = style.particleReach,
                flight = style.particleFlight,
                gap = style.particleGap,
                anticipation = style.anticipation,
                gatherDots = style.gatherDots,
                spread = style.particleSpread.coerceIn(0f, 1f),
            )
        }

        private fun hash(i: Float, n: Float, salt: Float): Float {
            val x = sin(i * 12.9898f + n * 78.233f + salt * 37.719f) * 43758.5453f
            return x - floor(x)
        }
    }
}

private fun colorToOklab(color: Color): FloatArray {
    fun linear(c: Float): Double {
        val d = c.toDouble()
        return if (d <= 0.04045) d / 12.92 else ((d + 0.055) / 1.055).pow(2.4)
    }
    val lr = linear(color.red)
    val lg = linear(color.green)
    val lb = linear(color.blue)
    val l = cbrt(0.4122214708 * lr + 0.5363325363 * lg + 0.0514459929 * lb)
    val m = cbrt(0.2119034982 * lr + 0.6806995451 * lg + 0.1073969566 * lb)
    val s = cbrt(0.0883024619 * lr + 0.2817188376 * lg + 0.6299787005 * lb)
    return floatArrayOf(
        (0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s).toFloat(),
        (1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s).toFloat(),
        (0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s).toFloat(),
    )
}

class MindWaveSnapshot internal constructor(
    internal val sharp: Bitmap,
    internal val nearBlur: Bitmap,
    internal val farBlur: Bitmap,
    internal val wDp: Float,
    internal val hDp: Float,
    internal val sharpScaleX: Float,
    internal val sharpScaleY: Float,
    internal val blurScaleX: Float,
    internal val blurScaleY: Float,
    private val ownsSharp: Boolean,
) {
    internal val sharpShader: BitmapShader = BitmapShader(sharp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    internal val nearShader: BitmapShader = BitmapShader(nearBlur, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    internal val farShader: BitmapShader = BitmapShader(farBlur, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)

    fun recycleAll() {
        if (ownsSharp && !sharp.isRecycled) sharp.recycle()
        if (nearBlur !== sharp && !nearBlur.isRecycled) nearBlur.recycle()
        if (farBlur !== sharp && farBlur !== nearBlur && !farBlur.isRecycled) farBlur.recycle()
    }
}

/**
 * Holds the most recent in-memory screen capture frame so [SaveOverlayActivity] can start the
 * DistortionWave lens on frame 0 without waiting for disk JPEG re-decoding.
 */
object MindCaptureSnapshotCache {
    @Volatile
    private var cachedBitmap: Bitmap? = null

    fun put(bitmap: Bitmap?) {
        if (bitmap == null || bitmap.isRecycled) return
        val copy = runCatching {
            val src = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && bitmap.config == Bitmap.Config.HARDWARE) {
                bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: return@runCatching null
            } else if (bitmap.config != Bitmap.Config.ARGB_8888) {
                bitmap.copy(Bitmap.Config.ARGB_8888, false) ?: bitmap
            } else {
                bitmap
            }
            val maxDim = max(src.width, src.height)
            val scaled = if (maxDim > 1440) {
                val ratio = 1440f / maxDim.toFloat()
                val w = (src.width * ratio).roundToInt().coerceAtLeast(1)
                val h = (src.height * ratio).roundToInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(src, w, h, true)
            } else if (src === bitmap) {
                src.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                src
            }
            if (src !== bitmap && src !== scaled && !src.isRecycled) {
                src.recycle()
            }
            scaled
        }.getOrNull()
        if (copy != null && !isBitmapMostlyBlack(copy)) {
            val old = cachedBitmap
            cachedBitmap = copy
            if (old != null && old !== copy && !old.isRecycled) {
                old.recycle()
            }
        } else {
            copy?.let { if (!it.isRecycled) it.recycle() }
            cachedBitmap = null
        }
    }

    fun peek(): Bitmap? {
        val bmp = cachedBitmap
        return if (bmp != null && !bmp.isRecycled) bmp else null
    }

    fun clear() {
        val old = cachedBitmap
        cachedBitmap = null
        if (old != null && !old.isRecycled) {
            old.recycle()
        }
    }
}

/**
 * Detects whether a captured screen frame is all-black or transparent (e.g. DRM / FLAG_SECURE
 * or uninitialized hardware surface) so the overlay never paints a black rectangle over the app.
 */
fun isBitmapMostlyBlack(bitmap: Bitmap): Boolean {
    if (bitmap.isRecycled || bitmap.width < 4 || bitmap.height < 4) return true
    var nonBlackSamples = 0
    var totalSamples = 0
    val cols = 8
    val rows = 8
    for (r in 1..rows) {
        val y = (bitmap.height * r / (rows + 1)).coerceIn(0, bitmap.height - 1)
        for (c in 1..cols) {
            val x = (bitmap.width * c / (cols + 1)).coerceIn(0, bitmap.width - 1)
            val px = bitmap.getPixel(x, y)
            val alpha = (px ushr 24) and 0xFF
            val red = (px ushr 16) and 0xFF
            val green = (px ushr 8) and 0xFF
            val blue = px and 0xFF
            totalSamples++
            if (alpha > 16 && (red > 6 || green > 6 || blue > 6)) {
                nonBlackSamples++
            }
        }
    }
    return nonBlackSamples <= (totalSamples / 20)
}

internal suspend fun prepareWaveSnapshot(
    sourceBitmap: Bitmap,
    wDp: Float,
    hDp: Float,
    style: MindDistortionWaveStyle,
    recycleSource: Boolean,
): MindWaveSnapshot = withContext(Dispatchers.Default) {
    val isHardware = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        sourceBitmap.config == Bitmap.Config.HARDWARE
    val softwareSharp = if (isHardware) {
        sourceBitmap.copy(Bitmap.Config.ARGB_8888, false) ?: sourceBitmap
    } else {
        sourceBitmap
    }
    if (recycleSource && softwareSharp !== sourceBitmap && !sourceBitmap.isRecycled) {
        sourceBitmap.recycle()
    }
    val blurScale = 0.5f
    val blurW = (wDp * blurScale).roundToInt().coerceIn(48, 960)
    val blurH = (hDp * blurScale).roundToInt().coerceIn(48, 960)
    val downscaledSrc = Bitmap.createScaledBitmap(softwareSharp, blurW, blurH, true)
    val downscaled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        downscaledSrc.config != Bitmap.Config.ARGB_8888
    ) {
        (downscaledSrc.copy(Bitmap.Config.ARGB_8888, false) ?: downscaledSrc).also {
            if (it !== downscaledSrc && !downscaledSrc.isRecycled) downscaledSrc.recycle()
        }
    } else {
        downscaledSrc
    }
    val nearR = (7f * blurScale * 0.90f).roundToInt().coerceIn(2, 24)
    val farR = (style.blur * blurScale * 0.95f).roundToInt().coerceIn(3, 36)
    val (nearBmp, farBmp) = coroutineScope {
        val nearJob = async { fastBoxBlurBitmap(downscaled, nearR) }
        val farJob = async { fastBoxBlurBitmap(downscaled, farR) }
        nearJob.await() to farJob.await()
    }
    if (downscaled !== softwareSharp && !downscaled.isRecycled) {
        downscaled.recycle()
    }
    MindWaveSnapshot(
        sharp = softwareSharp,
        nearBlur = nearBmp,
        farBlur = farBmp,
        wDp = wDp,
        hDp = hDp,
        sharpScaleX = softwareSharp.width.toFloat() / wDp.coerceAtLeast(1f),
        sharpScaleY = softwareSharp.height.toFloat() / hDp.coerceAtLeast(1f),
        blurScaleX = blurW.toFloat() / wDp.coerceAtLeast(1f),
        blurScaleY = blurH.toFloat() / hDp.coerceAtLeast(1f),
        ownsSharp = recycleSource || softwareSharp !== sourceBitmap,
    )
}

/**
 * 3-pass rounded separable box blur approximating a true Gaussian without integer darkening.
 */
private fun fastBoxBlurBitmap(src: Bitmap, radius: Int): Bitmap {
    val w = src.width
    val h = src.height
    if (w <= 1 || h <= 1 || radius < 1) return src.copy(Bitmap.Config.ARGB_8888, false) ?: src
    val pix = IntArray(w * h)
    src.getPixels(pix, 0, w, 0, 0, w, h)
    val tmp = IntArray(w * h)
    repeat(3) {
        boxBlurHorizontal(pix, tmp, w, h, radius)
        boxBlurVertical(tmp, pix, w, h, radius)
    }
    return Bitmap.createBitmap(pix, w, h, Bitmap.Config.ARGB_8888)
}

private fun boxBlurHorizontal(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int) {
    val div = r * 2 + 1
    val halfDiv = div / 2
    for (y in 0 until h) {
        val rowOffset = y * w
        var aSum = 0
        var rSum = 0
        var gSum = 0
        var bSum = 0
        for (i in -r..r) {
            val px = src[rowOffset + i.coerceIn(0, w - 1)]
            aSum += (px ushr 24) and 0xFF
            rSum += (px ushr 16) and 0xFF
            gSum += (px ushr 8) and 0xFF
            bSum += px and 0xFF
        }
        for (x in 0 until w) {
            dst[rowOffset + x] =
                (((aSum + halfDiv) / div) shl 24) or
                (((rSum + halfDiv) / div) shl 16) or
                (((gSum + halfDiv) / div) shl 8) or
                ((bSum + halfDiv) / div)
            val addIdx = (x + r + 1).coerceIn(0, w - 1)
            val subIdx = (x - r).coerceIn(0, w - 1)
            val pAdd = src[rowOffset + addIdx]
            val pSub = src[rowOffset + subIdx]
            aSum += ((pAdd ushr 24) and 0xFF) - ((pSub ushr 24) and 0xFF)
            rSum += ((pAdd ushr 16) and 0xFF) - ((pSub ushr 16) and 0xFF)
            gSum += ((pAdd ushr 8) and 0xFF) - ((pSub ushr 8) and 0xFF)
            bSum += (pAdd and 0xFF) - (pSub and 0xFF)
        }
    }
}

private fun boxBlurVertical(src: IntArray, dst: IntArray, w: Int, h: Int, r: Int) {
    val div = r * 2 + 1
    val halfDiv = div / 2
    for (x in 0 until w) {
        var aSum = 0
        var rSum = 0
        var gSum = 0
        var bSum = 0
        for (i in -r..r) {
            val px = src[i.coerceIn(0, h - 1) * w + x]
            aSum += (px ushr 24) and 0xFF
            rSum += (px ushr 16) and 0xFF
            gSum += (px ushr 8) and 0xFF
            bSum += px and 0xFF
        }
        for (y in 0 until h) {
            dst[y * w + x] =
                (((aSum + halfDiv) / div) shl 24) or
                (((rSum + halfDiv) / div) shl 16) or
                (((gSum + halfDiv) / div) shl 8) or
                ((bSum + halfDiv) / div)
            val addIdx = (y + r + 1).coerceIn(0, h - 1) * w + x
            val subIdx = (y - r).coerceIn(0, h - 1) * w + x
            val pAdd = src[addIdx]
            val pSub = src[subIdx]
            aSum += ((pAdd ushr 24) and 0xFF) - ((pSub ushr 24) and 0xFF)
            rSum += ((pAdd ushr 16) and 0xFF) - ((pSub ushr 16) and 0xFF)
            gSum += ((pAdd ushr 8) and 0xFF) - ((pSub ushr 8) and 0xFF)
            bSum += (pAdd and 0xFF) - (pSub and 0xFF)
        }
    }
}

/**
 * Wraps the base screen ([content]) and an optional full-screen [revealed] destination
 * (e.g., Companion Mode), driving dsh-go's DistortionWave enter & exit animation cleanly
 * from [sourceRectInRootPx] and optional tap [tapFocusInRootPx].
 *
 * Touch input is forwarded to the live [revealed] UI as soon as the wave front clears (~1.05s)
 * on enter, and back/close exits seamlessly down into the source anchor with [style.returnSpeed].
 */
@Composable
fun MindDistortionWaveTransitionHost(
    isRevealed: Boolean,
    sourceRectInRootPx: Rect?,
    tapFocusInRootPx: Offset?,
    onTransitionSettled: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    style: MindDistortionWaveStyle = MindDistortionWaveStyle.Vertical,
    animationsEnabled: Boolean = true,
    content: @Composable () -> Unit,
    revealed: @Composable () -> Unit,
) {
    val colors = MindTheme.colors
    val palette = remember(colors) { MindDistortionWavePalette.fromMindColors(colors) }
    val density = LocalDensity.current
    val progress = remember { Animatable(if (isRevealed) 1f else 0f) }

    val contentGraphicsLayer = rememberGraphicsLayer()
    val revealedGraphicsLayer = rememberGraphicsLayer()

    var contentCache by remember { mutableStateOf<MindWaveSnapshot?>(null) }
    var revealedCache by remember { mutableStateOf<MindWaveSnapshot?>(null) }
    var mountRevealed by remember { mutableStateOf(isRevealed) }
    var animating by remember { mutableStateOf(false) }
    var captureBaseRequested by remember { mutableStateOf(false) }
    var captureRevealedRequested by remember { mutableStateOf(false) }

    // Record wave orientation anchored around last direction change just like _RouteProgress
    var heldSourcePx by remember { mutableStateOf(sourceRectInRootPx) }
    var heldFocusPx by remember { mutableStateOf(tapFocusInRootPx) }

    DisposableEffect(Unit) {
        onDispose {
            contentCache?.recycleAll()
            revealedCache?.recycleAll()
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val wPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val hPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val wDp = wPx / density.density
        val hDp = hPx / density.density

        LaunchedEffect(isRevealed) {
            if (isRevealed) {
                heldSourcePx = sourceRectInRootPx
                heldFocusPx = tapFocusInRootPx
                mountRevealed = true

                if (!animationsEnabled) {
                    progress.snapTo(1f)
                    onTransitionSettled(true)
                    return@LaunchedEffect
                }

                // Request on-demand snapshot capture for both base & revealed content behind opaque base
                captureBaseRequested = true
                captureRevealedRequested = true
                awaitFrame()
                awaitFrame()

                var sharpOut = runCatching { contentGraphicsLayer.toImageBitmap().asAndroidBitmap() }.getOrNull()
                var sharpIn = runCatching { revealedGraphicsLayer.toImageBitmap().asAndroidBitmap() }.getOrNull()
                if (sharpIn == null || sharpOut == null) {
                    awaitFrame()
                    if (sharpOut == null) {
                        sharpOut = runCatching { contentGraphicsLayer.toImageBitmap().asAndroidBitmap() }.getOrNull()
                    }
                    if (sharpIn == null) {
                        sharpIn = runCatching { revealedGraphicsLayer.toImageBitmap().asAndroidBitmap() }.getOrNull()
                    }
                }
                val (preparedOut, preparedIn) = coroutineScope {
                    val outJob = async {
                        sharpOut?.takeIf { !it.isRecycled }?.let {
                            prepareWaveSnapshot(it, wDp, hDp, style, recycleSource = true)
                        }
                    }
                    val inJob = async {
                        sharpIn?.takeIf { !it.isRecycled }?.let {
                            prepareWaveSnapshot(it, wDp, hDp, style, recycleSource = true)
                        }
                    }
                    outJob.await() to inJob.await()
                }
                if (preparedOut != null) {
                    contentCache?.recycleAll()
                    contentCache = preparedOut
                }
                if (preparedIn != null) {
                    revealedCache?.recycleAll()
                    revealedCache = preparedIn
                }
                captureBaseRequested = false
                captureRevealedRequested = false

                animating = true
                val targetMs = (style.durationSeconds * 1000f).coerceAtLeast(280f)
                var curProgress = progress.value.coerceIn(0f, 1f)
                var lastFrameNs = 0L
                while (curProgress < 1f) {
                    val frameNs = awaitFrame()
                    if (lastFrameNs == 0L) {
                        lastFrameNs = frameNs
                        curProgress = max(curProgress, 0.002f)
                        progress.snapTo(curProgress)
                    } else {
                        val dtMs = (((frameNs - lastFrameNs) / 1_000_000f)).coerceIn(0f, 34f)
                        lastFrameNs = frameNs
                        curProgress = (curProgress + dtMs / targetMs).coerceAtMost(1f)
                        progress.snapTo(curProgress)
                    }
                }
                progress.snapTo(1f)
                animating = false
                // Free outgoing blur buffers while resting; re-captured on exit
                contentCache?.recycleAll()
                contentCache = null
                revealedCache?.recycleAll()
                revealedCache = null
                onTransitionSettled(true)
            } else {
                if (!mountRevealed && progress.value <= 0f) {
                    onTransitionSettled(false)
                    return@LaunchedEffect
                }
                if (!animationsEnabled) {
                    progress.snapTo(0f)
                    mountRevealed = false
                    onTransitionSettled(false)
                    return@LaunchedEffect
                }

                // Capture current base & revealed states behind opaque revealed view before collapsing
                captureBaseRequested = true
                captureRevealedRequested = true
                awaitFrame()
                awaitFrame()
                var sharpIn = runCatching { revealedGraphicsLayer.toImageBitmap().asAndroidBitmap() }.getOrNull()
                var sharpOut = runCatching { contentGraphicsLayer.toImageBitmap().asAndroidBitmap() }.getOrNull()
                if (sharpIn == null || sharpOut == null) {
                    awaitFrame()
                    if (sharpIn == null) {
                        sharpIn = runCatching { revealedGraphicsLayer.toImageBitmap().asAndroidBitmap() }.getOrNull()
                    }
                    if (sharpOut == null) {
                        sharpOut = runCatching { contentGraphicsLayer.toImageBitmap().asAndroidBitmap() }.getOrNull()
                    }
                }
                val (preparedIn, preparedOut) = coroutineScope {
                    val inJob = async {
                        sharpIn?.takeIf { !it.isRecycled }?.let {
                            prepareWaveSnapshot(it, wDp, hDp, style, recycleSource = true)
                        }
                    }
                    val outJob = async {
                        sharpOut?.takeIf { !it.isRecycled }?.let {
                            prepareWaveSnapshot(it, wDp, hDp, style, recycleSource = true)
                        }
                    }
                    inJob.await() to outJob.await()
                }
                if (preparedIn != null) {
                    revealedCache?.recycleAll()
                    revealedCache = preparedIn
                }
                if (preparedOut != null) {
                    contentCache?.recycleAll()
                    contentCache = preparedOut
                }
                captureBaseRequested = false
                captureRevealedRequested = false

                animating = true
                val collapseMs = ((style.durationSeconds / style.returnSpeed) * 1000f).coerceAtLeast(280f)
                val easedCurve = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
                val startProgress = progress.value.coerceIn(0f, 1f)
                val durationActual = (collapseMs * startProgress).coerceAtLeast(220f)
                var elapsedMs = 0f
                var lastFrameNs = 0L
                while (elapsedMs < durationActual) {
                    val frameNs = awaitFrame()
                    if (lastFrameNs == 0L) {
                        lastFrameNs = frameNs
                        progress.snapTo((startProgress - 0.002f).coerceAtLeast(0f))
                    } else {
                        val dtMs = ((frameNs - lastFrameNs) / 1_000_000f).coerceIn(0f, 34f)
                        lastFrameNs = frameNs
                        elapsedMs = (elapsedMs + dtMs).coerceAtMost(durationActual)
                        val frac = (elapsedMs / durationActual).coerceIn(0f, 1f)
                        val nextVal = startProgress * (1f - easedCurve.transform(frac))
                        progress.snapTo(nextVal)
                    }
                }
                progress.snapTo(0f)
                animating = false
                mountRevealed = false
                contentCache?.recycleAll()
                contentCache = null
                revealedCache?.recycleAll()
                revealedCache = null
                onTransitionSettled(false)
            }
        }

        val p = progress.value.coerceIn(0f, 1f)
        val sourceDp = remember(heldSourcePx, wDp, hDp, style.type) {
            heldSourcePx?.let { r ->
                Rect(
                    left = r.left / density.density,
                    top = r.top / density.density,
                    right = r.right / density.density,
                    bottom = r.bottom / density.density,
                )
            } ?: defaultWaveSourceDp(style.type, wDp, hDp)
        }
        val focusDp = remember(heldFocusPx, density.density) {
            heldFocusPx?.let { o -> Offset(o.x / density.density, o.y / density.density) }
        }
        val front = remember(p, sourceDp, wDp, hDp, focusDp, style) {
            MindWaveFront.compute(
                progress = p,
                sourceDp = sourceDp,
                sizeDp = Size(wDp, hDp),
                style = style,
                focusDp = focusDp,
            )
        }
        val isCapturing = captureBaseRequested || captureRevealedRequested

        // 1. Base content layer (draws natively at 0 overhead when idle; captures behind opaque revealed on exit)
        val drawBaseLive = (!animating && p <= 0.001f) || (!front.hasCleared && contentCache == null)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(if (isCapturing && !isRevealed) -1f else 0f)
                .drawWithContent {
                    if (captureBaseRequested) {
                        contentGraphicsLayer.record {
                            this@drawWithContent.drawContent()
                        }
                        drawLayer(contentGraphicsLayer)
                    } else if (drawBaseLive) {
                        drawContent()
                    }
                },
        ) {
            content()
        }

        // 2. Revealed Companion content layer (captures behind opaque base on enter, draws live once cleared)
        if (mountRevealed) {
            val blockTouches = animating && !front.hasCleared
            val drawRevealedLive = (!animating && p >= 0.999f) ||
                (front.hasCleared && front.energy <= 0.002f) ||
                (front.launched && revealedCache == null)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(if (isCapturing && isRevealed) -1f else 0.5f)
                    .drawWithContent {
                        if (captureRevealedRequested) {
                            revealedGraphicsLayer.record {
                                this@drawWithContent.drawContent()
                            }
                            drawLayer(revealedGraphicsLayer)
                        } else if (drawRevealedLive) {
                            drawContent()
                        }
                    }
                    .then(
                        if (blockTouches) {
                            Modifier.pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val ev = awaitPointerEvent()
                                        ev.changes.forEach { it.consume() }
                                    }
                                }
                            }
                        } else {
                            Modifier
                        },
                    ),
            ) {
                revealed()
            }
        }

        // 3. Distortion Wave Lens + Band + Starlight Particles overlay
        if (p > 0.001f && p < 0.999f && animationsEnabled) {
            MindDistortionWaveStageCanvas(
                front = front,
                sourceDp = sourceDp,
                style = style,
                palette = palette,
                contentFrame = contentCache,
                revealedFrame = revealedCache,
                density = density.density,
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(10f),
            )
        }
    }
}

/**
 * Full-screen or preview Distortion Wave stage for screen capture animations: renders a frozen
 * bitmap through dsh-go's progressive blur + chromatic lens shader alongside the OKLab band & sparks.
 */
@Composable
fun MindDistortionWaveScreenCaptureStage(
    progress: Float,
    style: MindDistortionWaveStyle,
    frozenBitmap: Bitmap?,
    accentSource: MindColors = MindTheme.colors,
    cornerRadiusDp: Float = 50f,
    sourceRectFraction: Rect? = null,
    showLensSnapshot: Boolean = true,
    isPreview: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val palette = remember(accentSource) { MindDistortionWavePalette.fromMindColors(accentSource) }
    val density = LocalDensity.current.density
    val particles = remember(style.particleCount, isPreview, style.type) {
        val count = if (isPreview) min(style.particleCount, 36) else style.particleCount
        List(count) { idx -> MindWaveParticle.create(idx, style) }
    }
    val lumps = remember { FloatArray(MindWaveRim.COUNT * 3) }
    val nativePaint = remember { Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG) }
    val lensShader = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        remember { RuntimeShader(DISTORTION_WAVE_LENS_AGSL) }
    } else {
        null
    }
    val bandShader = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        remember { RuntimeShader(DISTORTION_WAVE_BAND_AGSL) }
    } else {
        null
    }
    val packedOklab = remember(palette) { packPaletteOklab(palette) }

    BoxWithConstraints(modifier = modifier) {
        val wDp = constraints.maxWidth.toFloat().coerceAtLeast(1f) / density
        val hDp = constraints.maxHeight.toFloat().coerceAtLeast(1f) / density
        val effectiveStyle = remember(style, cornerRadiusDp, isPreview) {
            if (isPreview) {
                style.copy(
                    cornerRadius = cornerRadiusDp,
                    amplitude = 20f,
                    lensWidth = 88f,
                    blur = 11f,
                    blurAhead = 52f,
                    blurBehind = 136f,
                    lead = 72f,
                    tail = 165f,
                    rimWidth = 9f,
                    rimSoftness = 160f,
                    swell = 12f,
                    inhale = 28f,
                    pull = 14f,
                    hairlineWidth = 1.2f,
                )
            } else {
                style.copy(cornerRadius = cornerRadiusDp)
            }
        }
        val sourceDp = remember(sourceRectFraction, wDp, hDp, effectiveStyle.type, isPreview) {
            if (sourceRectFraction != null) {
                Rect(
                    sourceRectFraction.left * wDp,
                    sourceRectFraction.top * hDp,
                    sourceRectFraction.right * wDp,
                    sourceRectFraction.bottom * hDp,
                )
            } else if (isPreview) {
                if (effectiveStyle.type == MindWaveType.VERTICAL_TOP_DOWN) {
                    Rect(wDp * 0.22f, -4f, wDp * 0.78f, hDp * 0.16f)
                } else {
                    Rect(wDp * 0.22f, hDp * 0.78f, wDp * 0.78f, hDp * 0.96f)
                }
            } else {
                defaultWaveSourceDp(effectiveStyle.type, wDp, hDp)
            }
        }

        var snapshot by remember { mutableStateOf<MindWaveSnapshot?>(null) }
        val fallbackImageBitmap = remember(frozenBitmap) {
            frozenBitmap?.takeIf { !it.isRecycled }?.asImageBitmap()
        }
        DisposableEffect(Unit) {
            onDispose { snapshot?.recycleAll() }
        }
        LaunchedEffect(frozenBitmap, wDp, hDp, effectiveStyle) {
            val bmp = frozenBitmap
            if (bmp == null || bmp.isRecycled) {
                snapshot?.recycleAll()
                snapshot = null
            } else {
                val prepared = prepareWaveSnapshot(bmp, wDp, hDp, effectiveStyle, recycleSource = false)
                snapshot?.recycleAll()
                snapshot = prepared
            }
        }

        val front = MindWaveFront.compute(
            progress = progress,
            sourceDp = sourceDp,
            sizeDp = Size(wDp, hDp),
            style = effectiveStyle,
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            if (progress > 0.001f) {
                val frame = snapshot
                val activeWave = progress < 0.999f
                val lensActive = activeWave && (!front.hasCleared || front.energy > 0.002f || front.gravity > 0.002f)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    lensShader != null &&
                    bandShader != null
                ) {
                    if (showLensSnapshot && lensActive && frame != null && !frame.sharp.isRecycled) {
                        drawLensAgsl(
                            shader = lensShader,
                            frame = frame,
                            front = front,
                            style = effectiveStyle,
                            side = 0f,
                            density = density,
                            paint = nativePaint,
                        )
                    } else if (showLensSnapshot && fallbackImageBitmap != null) {
                        drawImage(
                            image = fallbackImageBitmap,
                            dstSize = IntSize(size.width.roundToInt().coerceAtLeast(1), size.height.roundToInt().coerceAtLeast(1)),
                        )
                    }
                    if (activeWave) {
                        MindWaveRim.computeLumps(
                            out = lumps,
                            time = front.time,
                            sizeDp = front.size,
                            cornerRadius = effectiveStyle.cornerRadius,
                            pulse = effectiveStyle.rimPulse,
                        )
                        drawBandAgsl(
                            shader = bandShader,
                            front = front,
                            style = effectiveStyle,
                            paletteOklab = packedOklab,
                            lumps = lumps,
                            density = density,
                            paint = nativePaint,
                        )
                    }
                } else {
                    if (showLensSnapshot && fallbackImageBitmap != null) {
                        drawImage(
                            image = fallbackImageBitmap,
                            dstSize = IntSize(size.width.roundToInt().coerceAtLeast(1), size.height.roundToInt().coerceAtLeast(1)),
                        )
                    }
                    if (activeWave) {
                        drawWaveFallbackBandAndRim(front, effectiveStyle, palette, density)
                    }
                }
                if (activeWave) {
                    drawWaveParticles(particles, front, sourceDp, density)
                }
            }
        }
    }
}

/**
 * Draws the full wave stage (incoming revealed lens, outgoing content lens, OKLab band + rim,
 * and 64 twinkling particles) using AGSL on API 33+ and smooth multi-stop Canvas fallback on API 26-32.
 */
@Composable
private fun MindDistortionWaveStageCanvas(
    front: MindWaveFront,
    sourceDp: Rect,
    style: MindDistortionWaveStyle,
    palette: MindDistortionWavePalette,
    contentFrame: MindWaveSnapshot?,
    revealedFrame: MindWaveSnapshot?,
    density: Float,
    modifier: Modifier = Modifier,
) {
    val particles = remember(style.particleCount, style) {
        List(style.particleCount) { idx -> MindWaveParticle.create(idx, style) }
    }
    val lumps = remember { FloatArray(MindWaveRim.COUNT * 3) }
    val nativePaint = remember { Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG) }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val revealedLensShader = remember { RuntimeShader(DISTORTION_WAVE_LENS_AGSL) }
        val contentLensShader = remember { RuntimeShader(DISTORTION_WAVE_LENS_AGSL) }
        val bandShader = remember { RuntimeShader(DISTORTION_WAVE_BAND_AGSL) }

        val packedOklab = remember(palette) { packPaletteOklab(palette) }

        Canvas(modifier = modifier) {
            val acting = front.energy > 0.002f
            val rFrame = revealedFrame
            if (front.launched && acting && rFrame != null && !rFrame.sharp.isRecycled) {
                drawLensAgsl(
                    shader = revealedLensShader,
                    frame = rFrame,
                    front = front,
                    style = style,
                    side = -1f,
                    density = density,
                    paint = nativePaint,
                )
            }
            val cFrame = contentFrame
            if (!front.hasCleared && (front.launched || front.gravity > 0f) && cFrame != null && !cFrame.sharp.isRecycled) {
                drawLensAgsl(
                    shader = contentLensShader,
                    frame = cFrame,
                    front = front,
                    style = style,
                    side = 1f,
                    density = density,
                    paint = nativePaint,
                )
            }

            MindWaveRim.computeLumps(
                out = lumps,
                time = front.time,
                sizeDp = front.size,
                cornerRadius = style.cornerRadius,
                pulse = style.rimPulse,
            )
            drawBandAgsl(
                shader = bandShader,
                front = front,
                style = style,
                paletteOklab = packedOklab,
                lumps = lumps,
                density = density,
                paint = nativePaint,
            )
            drawWaveParticles(particles, front, sourceDp, density)
        }
    } else {
        Canvas(modifier = modifier) {
            drawWaveFallbackBandAndRim(front, style, palette, density)
            drawWaveParticles(particles, front, sourceDp, density)
        }
    }
}

/**
 * Reusable atmospheric Distortion Wave band + sparkle overlay for live previews and capture FX.
 */
@Composable
fun MindDistortionWaveBandEffect(
    progress: Float,
    style: MindDistortionWaveStyle = MindDistortionWaveStyle.Vertical,
    accentSource: MindColors = MindTheme.colors,
    cornerRadiusDp: Float = 26f,
    sourceRectFraction: Rect? = null,
    isPreview: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val palette = remember(accentSource) { MindDistortionWavePalette.fromMindColors(accentSource) }
    val density = LocalDensity.current.density
    val particles = remember(style.particleCount, isPreview) {
        val count = if (isPreview) min(style.particleCount, 32) else style.particleCount
        List(count) { idx -> MindWaveParticle.create(idx, style) }
    }
    val lumps = remember { FloatArray(MindWaveRim.COUNT * 3) }
    val paint = remember { Paint(Paint.ANTI_ALIAS_FLAG) }
    val bandShader = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        remember { RuntimeShader(DISTORTION_WAVE_BAND_AGSL) }
    } else {
        null
    }
    val packedOklab = remember(palette) { packPaletteOklab(palette) }

    BoxWithConstraints(modifier = modifier) {
        val wDp = constraints.maxWidth.toFloat().coerceAtLeast(1f) / density
        val hDp = constraints.maxHeight.toFloat().coerceAtLeast(1f) / density
        val effectiveStyle = remember(style, cornerRadiusDp, isPreview) {
            if (isPreview) {
                style.copy(
                    cornerRadius = cornerRadiusDp,
                    lead = 64f,
                    tail = 140f,
                    rimWidth = 8f,
                    rimSoftness = 140f,
                    swell = 11f,
                    inhale = 26f,
                    hairlineWidth = 1.1f,
                )
            } else {
                style.copy(cornerRadius = cornerRadiusDp)
            }
        }
        val sourceDp = remember(sourceRectFraction, wDp, hDp, effectiveStyle.type, isPreview) {
            if (sourceRectFraction != null) {
                Rect(
                    sourceRectFraction.left * wDp,
                    sourceRectFraction.top * hDp,
                    sourceRectFraction.right * wDp,
                    sourceRectFraction.bottom * hDp,
                )
            } else if (isPreview) {
                if (effectiveStyle.type == MindWaveType.VERTICAL_TOP_DOWN) {
                    Rect(wDp * 0.22f, hDp * 0.06f, wDp * 0.78f, hDp * 0.22f)
                } else {
                    Rect(wDp * 0.25f, hDp * 0.72f, wDp * 0.75f, hDp * 0.88f)
                }
            } else {
                defaultWaveSourceDp(effectiveStyle.type, wDp, hDp)
            }
        }
        val front = MindWaveFront.compute(
            progress = progress,
            sourceDp = sourceDp,
            sizeDp = Size(wDp, hDp),
            style = effectiveStyle,
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            if (progress > 0.002f && progress < 0.998f) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && bandShader != null) {
                    MindWaveRim.computeLumps(
                        out = lumps,
                        time = front.time,
                        sizeDp = front.size,
                        cornerRadius = effectiveStyle.cornerRadius,
                        pulse = effectiveStyle.rimPulse,
                    )
                    drawBandAgsl(
                        shader = bandShader,
                        front = front,
                        style = effectiveStyle,
                        paletteOklab = packedOklab,
                        lumps = lumps,
                        density = density,
                        paint = paint,
                    )
                } else {
                    drawWaveFallbackBandAndRim(front, effectiveStyle, palette, density)
                }
                drawWaveParticles(particles, front, sourceDp, density)
            }
        }
    }
}

private fun packPaletteOklab(palette: MindDistortionWavePalette): FloatArray {
    val src = if (palette.colors.isEmpty()) listOf(palette.ahead) else palette.colors.take(8)
    val out = FloatArray(10 * 3 + 1)
    for (k in 0 until 8) {
        val lab = colorToOklab(src[k % src.size])
        out[k * 3] = lab[0]
        out[k * 3 + 1] = lab[1]
        out[k * 3 + 2] = lab[2]
    }
    val aheadLab = colorToOklab(palette.ahead)
    out[24] = aheadLab[0]; out[25] = aheadLab[1]; out[26] = aheadLab[2]
    val behindLab = colorToOklab(palette.behind)
    out[27] = behindLab[0]; out[28] = behindLab[1]; out[29] = behindLab[2]
    out[30] = src.size.toFloat()
    return out
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun DrawScope.drawLensAgsl(
    shader: RuntimeShader,
    frame: MindWaveSnapshot,
    front: MindWaveFront,
    style: MindDistortionWaveStyle,
    side: Float,
    density: Float,
    paint: Paint,
) {
    shader.setFloatUniform("uSize", frame.wDp, frame.hDp)
    shader.setFloatUniform("uDensity", density)
    shader.setFloatUniform("uSharpScale", frame.sharpScaleX, frame.sharpScaleY)
    shader.setFloatUniform("uBlurScale", frame.blurScaleX, frame.blurScaleY)
    shader.setFloatUniform("uFront", front.origin.x, front.origin.y, front.spine, front.radius)
    shader.setFloatUniform("uShape", front.squash, front.time, front.swell, front.tilt)
    shader.setFloatUniform("uMotion", front.energy, front.gravity, if (front.ring) 1f else 0f, front.hold)
    shader.setFloatUniform("uLens", style.amplitude, style.lensWidth, style.aberration, side)
    shader.setFloatUniform("uBlur", style.blur, style.blurAhead, style.blurBehind, if (front.launched) 1f else 0f)
    shader.setInputShader("uSharp", frame.sharpShader)
    shader.setInputShader("uNear", frame.nearShader)
    shader.setInputShader("uFar", frame.farShader)

    drawIntoCanvas { canvas ->
        paint.shader = shader
        canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint)
        paint.shader = null
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun DrawScope.drawBandAgsl(
    shader: RuntimeShader,
    front: MindWaveFront,
    style: MindDistortionWaveStyle,
    paletteOklab: FloatArray,
    lumps: FloatArray,
    density: Float,
    paint: Paint,
) {
    shader.setFloatUniform("uSize", front.size.width, front.size.height)
    shader.setFloatUniform("uDensity", density)
    shader.setFloatUniform("uFront", front.origin.x, front.origin.y, front.spine, front.radius)
    shader.setFloatUniform("uShape", front.squash, front.time, front.swell, front.tilt)
    shader.setFloatUniform("uGlow", front.glow, front.rim, front.clearRadius, front.hairClearRadius)
    shader.setFloatUniform("uKind", if (front.ring) 1f else 0f, front.hold, style.lead, style.tail)
    shader.setFloatUniform("uRimA", style.strength, style.rimWidth, style.rimSoftness, style.cornerRadius)
    shader.setFloatUniform("uRimB", style.rimUnevenness, style.rimPulse, style.hairlineWidth, style.hairlineOpacity)
    shader.setFloatUniform("uRimC", style.hairlineWhite, paletteOklab[30], 0f, 0f)

    for (k in 0 until 8) {
        val base = k * 3
        shader.setFloatUniform("uP$k", paletteOklab[base], paletteOklab[base + 1], paletteOklab[base + 2], 1f)
    }
    shader.setFloatUniform("uAhead", paletteOklab[24], paletteOklab[25], paletteOklab[26], 1f)
    shader.setFloatUniform("uBehind", paletteOklab[27], paletteOklab[28], paletteOklab[29], 1f)

    for (k in 0 until MindWaveRim.COUNT) {
        val base = k * 3
        shader.setFloatUniform("uL$k", lumps[base], lumps[base + 1], lumps[base + 2], 0f)
    }

    drawIntoCanvas { canvas ->
        paint.shader = shader
        canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint)
        paint.shader = null
    }
}

private fun DrawScope.drawWaveParticles(
    particles: List<MindWaveParticle>,
    front: MindWaveFront,
    sourceDp: Rect,
    density: Float,
) {
    for (p in particles) {
        val alpha = p.opacity(front).coerceIn(0f, 1f)
        if (alpha <= 0.01f) continue
        val ptDp = p.pointDp(front, sourceDp)
        drawCircle(
            color = Color.White.copy(alpha = alpha),
            radius = (p.size * 0.5f) * density,
            center = Offset(ptDp.x * density, ptDp.y * density),
        )
    }
}

private fun DrawScope.drawWaveFallbackBandAndRim(
    front: MindWaveFront,
    style: MindDistortionWaveStyle,
    palette: MindDistortionWavePalette,
    density: Float,
) {
    val glowAlpha = front.glow * style.strength
    if (glowAlpha > 0.01f) {
        val rPx = (front.radius * density).coerceAtLeast(16f)
        val bandPx = ((style.lead + style.tail) * 0.45f * density).coerceAtLeast(24f)
        val centerPx = Offset(front.origin.x * density, front.origin.y * density)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    palette.behind.copy(alpha = glowAlpha * 0.42f),
                    palette.ahead.copy(alpha = glowAlpha * 0.56f),
                    Color.White.copy(alpha = glowAlpha * 0.36f),
                    Color.Transparent,
                ),
                center = centerPx,
                radius = rPx + bandPx,
            ),
            center = centerPx,
            radius = rPx + bandPx,
        )
    }
    if (front.rim > 0.01f) {
        val strokePx = (style.rimWidth * 0.65f * density).coerceAtLeast(2f)
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    palette.ahead.copy(alpha = front.rim * 0.45f),
                    palette.behind.copy(alpha = front.rim * 0.45f),
                ),
            ),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(style.cornerRadius * density),
            style = Stroke(width = strokePx),
        )
    }
}

// =================================================================================================
// AGSL Shaders (Android 13 / API 33+): exact port of dsh-go distortion_wave_field + lens + band
// =================================================================================================

private const val DISTORTION_WAVE_FIELD_AGSL = """
const float coverZone = 60.0;
const float wavePi = 3.14159265;

float heldAt(float2 p, float hold, float2 origin) {
    if (hold <= 0.0) return 1.0;
    return smoothstep(hold * 0.9, hold * 1.8, length(p - origin));
}

float fieldAt(
    float2 p,
    float4 front,
    float4 shape,
    float ringFlag,
    out float2 n,
    out float2 q,
    out float2 raw,
    out float g
) {
    raw = p - front.xy;
    float cs = cos(shape.w);
    float sn = sin(shape.w);
    q = float2(cs * raw.x + sn * raw.y, -sn * raw.x + cs * raw.y);
    float spine = front.z;
    float radius = front.w;
    float squash = shape.x;
    float time = shape.y;
    float swell = shape.z;

    float mx = q.x * squash;
    float2 v = float2(max(abs(mx) - spine, 0.0), q.y);
    float l = max(length(v), 0.001);
    float sx = q.x >= 0.0 ? 1.0 : -1.0;
    float2 grad = float2(v.x * sx * squash, v.y) / l;
    g = max(length(grad), 0.001);

    float bend;
    if (ringFlag > 0.5) {
        float a = atan(q.y, q.x);
        bend = 0.62 * sin(2.0 * a + time * 1.1) + 0.38 * sin(3.0 * a - time * 1.5 + 1.7);
    } else {
        float s = mx * 0.0058;
        bend = 0.62 * sin(s + time * 1.1) + 0.38 * sin(s * 2.1 - time * 1.5 + 1.7);
    }

    float2 m = grad / g;
    n = float2(cs * m.x - sn * m.y, sn * m.x + cs * m.y);
    return (l - radius) / g + swell * bend;
}
"""

private const val DISTORTION_WAVE_LENS_AGSL = """
uniform float2 uSize;
uniform float uDensity;
uniform float2 uSharpScale;
uniform float2 uBlurScale;
uniform float4 uFront;
uniform float4 uShape;
uniform float4 uMotion;
uniform float4 uLens;
uniform float4 uBlur;
uniform shader uSharp;
uniform shader uNear;
uniform shader uFar;

$DISTORTION_WAVE_FIELD_AGSL

const float nearBlur = 7.0;

float easeSmooth(float a, float b, float x) {
    float t = clamp((x - a) / max(b - a, 0.001), 0.0, 1.0);
    return t * t * (3.0 - 2.0 * t);
}

half4 layerAt(float2 xDp, float nearFrac, float farFrac) {
    float2 sharpMax = max(uSize * uSharpScale - float2(0.5), float2(0.5));
    float2 blurMax = max(uSize * uBlurScale - float2(0.5), float2(0.5));
    float2 sharpPx = clamp(xDp * uSharpScale, float2(0.5), sharpMax);
    float2 blurPx = clamp(xDp * uBlurScale, float2(0.5), blurMax);
    half4 c = uSharp.eval(sharpPx);
    if (nearFrac > 0.0) {
        half4 a = uNear.eval(blurPx) * half(nearFrac);
        c = a + c * (1.0 - a.a);
    }
    if (farFrac > 0.0) {
        half4 b = uFar.eval(blurPx) * half(farFrac);
        c = b + c * (1.0 - b.a);
    }
    return c;
}

half4 main(float2 fragCoord) {
    float2 p = fragCoord / max(uDensity, 0.5);
    float ringFlag = uMotion.z;
    float holdVal = uMotion.w;

    float2 n;
    float2 q;
    float2 raw;
    float g;
    float d = fieldAt(p, uFront, uShape, ringFlag, n, q, raw, g);
    float energy = uMotion.x;
    float gravity = uMotion.y;
    float side = uLens.w;

    float cover = 1.0;
    if (side > 0.5) {
        if (uBlur.w > 0.5) cover = smoothstep(-coverZone, coverZone, d);
        if (cover <= 0.0) {
            return half4(0.0);
        }
    } else if (side < -0.5) {
        if (d > coverZone + 6.0) {
            return half4(0.0);
        }
    }

    float u = d / (uLens.y * (d > 0.0 ? 0.85 : 1.5));
    float lift = exp(-u * u);
    float r = max(length(raw), 1.0);

    float held = heldAt(p, holdVal, uFront.xy);
    float2 swollen = n * energy * uLens.x * lift * held;
    float2 inward = raw / r;
    float2 s = p - swollen + inward * gravity * exp(-r / 900.0) * held;
    float2 split = (swollen - inward * gravity * exp(-r / 240.0) * held) * uLens.z;

    float nearFrac = 0.0;
    float farFrac = 0.0;
    if (energy > 0.0 && uBlur.x > 0.0) {
        float2 sn;
        float2 sq;
        float2 sraw;
        float sg;
        float ds = fieldAt(s, uFront, uShape, ringFlag, sn, sq, sraw, sg);
        float su = ds / (ds > 0.0 ? uBlur.y : uBlur.z);
        float radius = energy * uBlur.x * exp(-su * su) * heldAt(s, holdVal, uFront.xy);
        nearFrac = easeSmooth(0.0, nearBlur, radius);
        farFrac = easeSmooth(nearBlur, uBlur.x, radius);
    }

    if (dot(split, split) < 0.0625) {
        return layerAt(s, nearFrac, farFrac) * half(cover);
    }

    half4 c0 = layerAt(s - split, nearFrac, farFrac);
    half4 c1 = layerAt(s - split * 0.5, nearFrac, farFrac);
    half4 c2 = layerAt(s, nearFrac, farFrac);
    half4 c3 = layerAt(s + split * 0.5, nearFrac, farFrac);
    half4 c4 = layerAt(s + split, nearFrac, farFrac);
    half3 rgb = c0.rgb * half3(0.571, 0.0, 0.0)
        + c1.rgb * half3(0.343, 0.206, 0.0)
        + c2.rgb * half3(0.086, 0.588, 0.086)
        + c3.rgb * half3(0.0, 0.206, 0.343)
        + c4.rgb * half3(0.0, 0.0, 0.571);
    half a = (c0.a + c1.a + c2.a + c3.a + c4.a) * 0.2;
    return half4(rgb * half(cover), a * half(cover));
}
"""

private const val DISTORTION_WAVE_BAND_AGSL = """
uniform float2 uSize;
uniform float uDensity;
uniform float4 uFront;
uniform float4 uShape;
uniform float4 uGlow;
uniform float4 uKind;
uniform float4 uRimA;
uniform float4 uRimB;
uniform float4 uRimC;
uniform float4 uP0;
uniform float4 uP1;
uniform float4 uP2;
uniform float4 uP3;
uniform float4 uP4;
uniform float4 uP5;
uniform float4 uP6;
uniform float4 uP7;
uniform float4 uAhead;
uniform float4 uBehind;
uniform float4 uL0;
uniform float4 uL1;
uniform float4 uL2;
uniform float4 uL3;
uniform float4 uL4;
uniform float4 uL5;
uniform float4 uL6;

$DISTORTION_WAVE_FIELD_AGSL

float sq(float x) {
    return x * x;
}

float3 oklabToSRGB(float3 c) {
    float l = c.x + 0.3963377774 * c.y + 0.2158037573 * c.z;
    float m = c.x - 0.1055613458 * c.y - 0.0638541728 * c.z;
    float s = c.x - 0.0894841775 * c.y - 1.2914855480 * c.z;
    l = l * l * l;
    m = m * m * m;
    s = s * s * s;
    float3 lin = clamp(float3(
        4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
        -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
        -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s
    ), 0.0, 1.0);
    float3 linSafe = max(lin, float3(0.00001));
    return mix(12.92 * lin, 1.055 * pow(linSafe, float3(1.0 / 2.4)) - 0.055, step(float3(0.0031308), lin));
}

float continuousRectSDF(float2 p, float2 halfSize, float radius) {
    float a = min(radius * 1.528665, min(halfSize.x, halfSize.y));
    float2 q = abs(p) - (halfSize - a);
    if (q.x <= 0.0 || q.y <= 0.0) {
        return max(abs(p.x) - halfSize.x, abs(p.y) - halfSize.y);
    }
    const float n = 3.26;
    float2 u = max(q / max(a, 0.001), float2(0.0001));
    float norm = pow(pow(u.x, n) + pow(u.y, n), 1.0 / n);
    float2 g = pow(u / max(norm, 0.0001), float2(n - 1.0));
    return (norm - 1.0) * a / max(length(g), 0.001);
}

float2 perimeterAt(float2 p, float2 halfSize, float r) {
    float ex = max(halfSize.x - r, 0.0);
    float ey = max(halfSize.y - r, 0.0);
    float arc = 0.5 * wavePi * r;
    float P = 4.0 * (ex + ey + arc);
    float ax = abs(p.x);
    float ay = abs(p.y);
    if (ax > ex && ay > ey) {
        float2 v = p - float2(p.x >= 0.0 ? ex : -ex, p.y >= 0.0 ? ey : -ey);
        float a;
        float base;
        if (p.x >= 0.0 && p.y < 0.0) {
            a = atan(v.x, -v.y);
            base = ex;
        } else if (p.x >= 0.0) {
            a = atan(v.y, v.x);
            base = ex + arc + 2.0 * ey;
        } else if (p.y >= 0.0) {
            a = atan(-v.x, v.y);
            base = 3.0 * ex + 2.0 * arc + 2.0 * ey;
        } else {
            a = atan(-v.y, -v.x);
            base = 3.0 * ex + 3.0 * arc + 4.0 * ey;
        }
        return float2(base + r * clamp(a, 0.0, 0.5 * wavePi), P);
    }
    bool side = ax > ex ? true : (ay > ey ? false : (halfSize.x - ax) < (halfSize.y - ay));
    if (side) {
        return float2(p.x >= 0.0 ? ex + arc + (p.y + ey) : 3.0 * ex + 3.0 * arc + 2.0 * ey + (ey - p.y), P);
    }
    return float2(p.y < 0.0 ? (p.x >= 0.0 ? p.x : P + p.x) : ex + 2.0 * arc + 2.0 * ey + (ex - p.x), P);
}

float lumpAt(float s, float P, float4 lump) {
    float ds = s - lump.x;
    ds -= P * floor(ds / max(P, 1.0) + 0.5);
    return lump.z * exp(-sq(ds / max(lump.y, 1.0)));
}

float rimThickness(float s, float P) {
    float t = 0.08 + lumpAt(s, P, uL0) + lumpAt(s, P, uL1) + lumpAt(s, P, uL2) + lumpAt(s, P, uL3)
        + lumpAt(s, P, uL4) + lumpAt(s, P, uL5) + lumpAt(s, P, uL6);
    return t / 1.0498;
}

float waveDither(float2 p) {
    return fract(52.9829189 * fract(dot(p, float2(0.06711056, 0.00583715)))) - 0.5;
}

float3 paletteColor(float k) {
    if (k < 0.5) return uP0.xyz;
    if (k < 1.5) return uP1.xyz;
    if (k < 2.5) return uP2.xyz;
    if (k < 3.5) return uP3.xyz;
    if (k < 4.5) return uP4.xyz;
    if (k < 5.5) return uP5.xyz;
    if (k < 6.5) return uP6.xyz;
    return uP7.xyz;
}

float3 paletteAt(float u) {
    float count = max(uRimC.y, 1.0);
    float x = fract(u) * count;
    float i = min(floor(x), count - 1.0);
    float j = mod(i + 1.0, count);
    float m = fract(x);
    m = m * m * (3.0 - 2.0 * m);
    return mix(paletteColor(i), paletteColor(j), m);
}

half4 main(float2 fragCoord) {
    float2 p = fragCoord / max(uDensity, 0.5);
    float glow = uGlow.x;
    float rim = uGlow.y;
    float ringFlag = uKind.x;
    float holdVal = uKind.y;
    float lead = uKind.z;
    float tail = uKind.w;
    float strength = uRimA.x;
    float rimWidth = uRimA.y;
    float rimSoftness = uRimA.z;
    float cornerRadius = uRimA.w;
    float unevenness = uRimB.x;
    float pulse = uRimB.y;
    float hairWidth = uRimB.z;
    float hairOpacity = uRimB.w;
    float hairWhite = uRimC.x;

    float2 c = p - uSize * 0.5;
    float edgeDistance = continuousRectSDF(c, uSize * 0.5, cornerRadius);
    float inset = max(-edgeDistance, 0.0);
    float inside = 1.0 - smoothstep(0.0, 0.75, edgeDistance);
    bool nearEdge = rim > 0.0 && inside > 0.0
        && inset < max(rimWidth * 2.2 * (1.0 + 1.9 * unevenness), hairWidth * 0.5 + 0.75);
    if (glow <= 0.0 && !nearEdge) {
        return half4(0.0);
    }

    float2 n;
    float2 q;
    float2 raw;
    float g;
    float d = fieldAt(p, uFront, uShape, ringFlag, n, q, raw, g);
    float time = uShape.y;

    bool ring = ringFlag > 0.5;
    float turn = atan(q.y, q.x);
    float along = q.x * 0.0068;
    float lobes = ring
        ? 0.5 + 0.5 * sin(2.0 * turn + time * 1.4) * cos(turn - time * 0.7 + 1.2)
        : 0.5 + 0.5 * sin(along * 1.3 + time * 1.4) * cos(along * 0.6 - time * 0.7 + 1.2);
    float bandA = 0.0;
    if (glow > 0.0 && d < lead) {
        if (d >= 0.0) {
            float k = 1.0 - d / max(lead, 1.0);
            bandA = k * k * k * (k * (k * 6.0 - 15.0) + 10.0);
        } else {
            float baseT = 1.0 - clamp(-d / max(tail * mix(0.6, 1.3, lobes), 1.0), 0.0, 1.0);
            bandA = pow(max(baseT, 0.0001), 2.3);
        }
        bandA *= glow * strength * mix(0.6, 1.0, exp(-inset / 140.0)) * heldAt(p, holdVal, uFront.xy);
    }

    float around = atan(c.y / max(uSize.y, 1.0), c.x / max(uSize.x, 1.0));
    float rimA = 0.0;
    float hairA = 0.0;
    float shimmer = 0.5;
    if (nearEdge) {
        float2 edge = perimeterAt(c, uSize * 0.5, cornerRadius);
        float thickness = 1.0;
        if (unevenness > 0.0) {
            thickness = max(1.0 + (rimThickness(edge.x, edge.y) - 1.0) * unevenness, 0.04);
        }
        float hug = exp(-sq(inset / max(rimWidth * thickness, 0.5)));
        float reached = 1.0 - smoothstep(-lead * 0.7, lead * 0.5, d);
        float kept = smoothstep(-rimSoftness, rimSoftness, d + (uFront.w - uGlow.z) / g);
        shimmer = 0.5 + 0.5 * sin(time * 3.4 - edge.x / max(edge.y, 1.0) * 4.0 * wavePi);
        rimA = hug * reached * kept * rim * mix(1.0, 0.7 + 0.3 * shimmer, pulse) * inside;

        if (hairWidth > 0.0) {
            float line = 1.0 - smoothstep(hairWidth * 0.5, hairWidth * 0.5 + 0.75, inset);
            float hairKept = smoothstep(-rimSoftness, rimSoftness, d + (uFront.w - uGlow.w) / g);
            hairA = line * reached * hairKept * rim * hairOpacity
                * mix(0.5, 1.0, smoothstep(0.1, 1.5, thickness))
                * mix(1.0, 0.55 + 0.45 * shimmer, pulse) * inside;
        }
    }

    float a = 1.0 - (1.0 - bandA) * (1.0 - rimA);
    float total = 1.0 - (1.0 - a) * (1.0 - hairA);
    if (total <= 0.0005) {
        return half4(0.0);
    }

    float flow = 0.62 + (ring ? turn / (2.0 * wavePi) : along * 0.2) + time * 0.13 + d * 0.0015
        + (ring ? 0.0 : 0.05 * sin(q.y * 0.009 + time));
    float3 lab = paletteAt(flow);
    lab = mix(lab, uAhead.xyz, 0.5 * exp(-sq((d - 30.0) / 50.0)));
    lab = mix(lab, uBehind.xyz, 0.5 * exp(-sq((d + 84.0) / 72.0)));
    lab.yz *= 1.22;
    lab = mix(lab, float3(0.99, 0.0, 0.0), 0.7 * exp(-sq((d + 8.0) / 26.0)));

    float3 rimLab = paletteAt(around / (2.0 * wavePi) + time * (0.12 + 0.25 * pulse) + inset * 0.0012);
    rimLab.yz *= 1.1;
    lab = mix(lab, rimLab, rimA / (rimA + bandA + 0.0001));

    float3 rgb = oklabToSRGB(lab);
    if (hairA > 0.0) {
        float3 hair = mix(oklabToSRGB(rimLab), float3(1.0), hairWhite * (0.5 + 0.5 * shimmer));
        rgb = (rgb * a * (1.0 - hairA) + hair * hairA) / total;
    }

    float noise = waveDither(p) * 2.0 / 255.0;
    return half4(half3(clamp(rgb * total + noise, 0.0, 1.0)), half(clamp(total + noise, 0.0, 1.0)));
}
"""
