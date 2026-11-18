package com.mind.app.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.mind.app.core.model.AccentPalette
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Semantic color & surface system calibrated directly to the "Mist Glass" (light) and
 * "Deep Ocean Slate" (dark) palettes: translucent frosted layers over a subtle tinted gradient
 * canvas with soft corner glows, low-saturation semantic status colors, and crisp hairlines.
 */
@Immutable
data class MindColors(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceSubtle: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color,
    val accentSoft: Color,
    val onAccent: Color,
    val borderSubtle: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val favourite: Color,
    val isDark: Boolean,
    val accentSecondary: Color = Color(0xFFABC3FC),
    val accentTertiary: Color = Color(0xFF6B578F),
    val backdropTop: Color = Color(0xFFEBF1FE),
    val backdropBottom: Color = Color(0xFFF6F7FC),
    val glowPrimary: Color = Color(0x6BABC3FC),
    val glowSecondary: Color = Color(0x61D5CBEB),
    val hairline: Color = Color(0x140B152D),
    val rimHighlight: Color = Color(0xB3FFFFFF),
    val glassCardFill: Color = Color(0x9EFFFFFF),
    val glassCardFallback: Color = Color(0xB8FFFFFF),
    val glassChromeFill: Color = Color(0xB3FFFFFF),
    val glassChromeFallback: Color = Color(0xF5FBFCFF),
    val glassSheetFill: Color = Color(0xD1F6F9FE),
    val glassPillFill: Color = Color(0x8CFFFFFF),
    val shadowInk: Color = Color(0xFF0B152D),
    val successSoft: Color = Color(0xFFD9F0E1),
    val warningSoft: Color = Color(0xFFF9E9CE),
    val dangerSoft: Color = Color(0xFFFFE2E0),
    val identityContainers: List<Color> = LIGHT_IDENTITY_CONTAINERS,
    val onIdentityContainers: List<Color> = LIGHT_ON_IDENTITY_CONTAINERS,
)

private val LIGHT_IDENTITY_CONTAINERS = listOf(
    Color(0xFFDAECFF),
    Color(0xFFE9E6FF),
    Color(0xFFF9E1F3),
    Color(0xFFFFE2E0),
    Color(0xFFF8E6D1),
    Color(0xFFE0EFD8),
    Color(0xFFD3F1E9),
    Color(0xFFD1F0F8),
)

private val LIGHT_ON_IDENTITY_CONTAINERS = listOf(
    Color(0xFF173F65),
    Color(0xFF3E3463),
    Color(0xFF562B4E),
    Color(0xFF602A2A),
    Color(0xFF563500),
    Color(0xFF2A4618),
    Color(0xFF00483D),
    Color(0xFF004552),
)

private val DARK_IDENTITY_CONTAINERS = listOf(
    Color(0xFF243A50),
    Color(0xFF38334F),
    Color(0xFF472E42),
    Color(0xFF4E2E2D),
    Color(0xFF47341A),
    Color(0xFF2D3E24),
    Color(0xFF174038),
    Color(0xFF153E47),
)

private val DARK_ON_IDENTITY_CONTAINERS = listOf(
    Color(0xFFC5DEF8),
    Color(0xFFDBD6F7),
    Color(0xFFEFD0E7),
    Color(0xFFF7D0CD),
    Color(0xFFEED7BB),
    Color(0xFFCEE2C4),
    Color(0xFFBCE5DB),
    Color(0xFFBAE3ED),
)

private data class AccentSeed(
    val lightAccent: Color,
    val lightSoft: Color,
    val glowLight: Color,
    val hazeLight: Color,
    val darkAccent: Color,
    val darkSoft: Color,
    val onDarkAccent: Color,
    val glowDark: Color,
    val hazeDark: Color,
    val tintHue: Double,
    val tintChroma: Double,
)

private const val MIST_BASE_HUE = 266.0

private fun hslColor(hue: Float, sat: Float, light: Float): Color {
    val h = ((hue % 360f) + 360f) % 360f
    return Color(
        androidx.core.graphics.ColorUtils.HSLToColor(
            floatArrayOf(h, sat.coerceIn(0f, 1f), light.coerceIn(0f, 1f)),
        ),
    )
}

private fun seedFromHue(
    hue: Float,
    sat: Float = 0.72f,
    tintHue: Double = hue.toDouble(),
    tintChroma: Double = 0.90,
): AccentSeed {
    return AccentSeed(
        lightAccent = hslColor(hue, sat.coerceAtMost(0.78f), 0.46f),
        lightSoft = hslColor(hue, (sat * 0.75f).coerceIn(0.25f, 0.70f), 0.92f),
        glowLight = hslColor(hue - 8f, (sat * 0.85f).coerceIn(0.35f, 0.88f), 0.78f),
        hazeLight = hslColor(hue + 24f, (sat * 0.65f).coerceIn(0.25f, 0.75f), 0.84f),
        darkAccent = hslColor(hue, (sat * 0.88f).coerceIn(0.42f, 0.86f), 0.58f),
        darkSoft = hslColor(hue, (sat * 0.55f).coerceIn(0.22f, 0.55f), 0.24f),
        onDarkAccent = Color.White,
        glowDark = hslColor(hue - 6f, (sat * 0.72f).coerceIn(0.30f, 0.75f), 0.46f),
        hazeDark = hslColor(hue + 22f, (sat * 0.55f).coerceIn(0.22f, 0.62f), 0.36f),
        tintHue = tintHue,
        tintChroma = tintChroma,
    )
}

private fun AccentPalette.seed(): AccentSeed = when (this) {
    AccentPalette.NEURAL_INDIGO -> seedFromHue(hue = 226f, sat = 0.68f, tintHue = MIST_BASE_HUE, tintChroma = 1.0)
    AccentPalette.BIOLUMINESCENT_TEAL -> seedFromHue(hue = 204f, sat = 0.74f, tintHue = 238.0, tintChroma = 0.9)
    AccentPalette.SKY_AZURE -> seedFromHue(hue = 196f, sat = 0.84f, tintHue = 225.0, tintChroma = 0.95)
    AccentPalette.ELECTRIC_CYAN -> seedFromHue(hue = 184f, sat = 0.82f, tintHue = 210.0, tintChroma = 0.92)
    AccentPalette.SAGE_MEMORY -> seedFromHue(hue = 172f, sat = 0.66f, tintHue = 190.0, tintChroma = 0.75)
    AccentPalette.EMERALD_MIST -> seedFromHue(hue = 152f, sat = 0.68f, tintHue = 168.0, tintChroma = 0.80)
    AccentPalette.JADE_MINT -> seedFromHue(hue = 138f, sat = 0.62f, tintHue = 152.0, tintChroma = 0.75)
    AccentPalette.SPRING_LIME -> seedFromHue(hue = 96f, sat = 0.64f, tintHue = 115.0, tintChroma = 0.72)
    AccentPalette.CITRUS_SOL -> seedFromHue(hue = 46f, sat = 0.82f, tintHue = 68.0, tintChroma = 0.82)
    AccentPalette.AMBER_RECALL -> seedFromHue(hue = 34f, sat = 0.76f, tintHue = 75.0, tintChroma = 0.78)
    AccentPalette.TANGERINE_GLOW -> seedFromHue(hue = 22f, sat = 0.84f, tintHue = 45.0, tintChroma = 0.88)
    AccentPalette.SUNSET_CORAL -> seedFromHue(hue = 10f, sat = 0.80f, tintHue = 28.0, tintChroma = 0.88)
    AccentPalette.CRIMSON_PULSE -> seedFromHue(hue = 352f, sat = 0.78f, tintHue = 12.0, tintChroma = 0.90)
    AccentPalette.CHERRY_BLOSSOM -> seedFromHue(hue = 336f, sat = 0.76f, tintHue = 350.0, tintChroma = 0.88)
    AccentPalette.HOT_MAGENTA -> seedFromHue(hue = 318f, sat = 0.78f, tintHue = 332.0, tintChroma = 0.92)
    AccentPalette.ORCHID_BLOOM -> seedFromHue(hue = 296f, sat = 0.70f, tintHue = 312.0, tintChroma = 0.88)
    AccentPalette.ROSE_QUARTZ -> seedFromHue(hue = 274f, sat = 0.62f, tintHue = 295.0, tintChroma = 0.85)
    AccentPalette.ROYAL_VIOLET -> seedFromHue(hue = 258f, sat = 0.76f, tintHue = 278.0, tintChroma = 0.95)
    AccentPalette.COSMIC_PURPLE -> seedFromHue(hue = 244f, sat = 0.78f, tintHue = 264.0, tintChroma = 0.98)
    AccentPalette.COBALT_NOVA -> seedFromHue(hue = 216f, sat = 0.86f, tintHue = 248.0, tintChroma = 1.0)
    AccentPalette.ARCTIC_ICE -> seedFromHue(hue = 200f, sat = 0.52f, tintHue = 228.0, tintChroma = 0.65)
    AccentPalette.GRAPHITE_SLATE -> seedFromHue(hue = 220f, sat = 0.28f, tintHue = 240.0, tintChroma = 0.35)
    AccentPalette.TITANIUM_MIST -> seedFromHue(hue = 245f, sat = 0.32f, tintHue = 258.0, tintChroma = 0.42)
    AccentPalette.ROSE_GOLD -> seedFromHue(hue = 16f, sat = 0.56f, tintHue = 32.0, tintChroma = 0.68)
}

fun mindLightColors(accentPalette: AccentPalette): MindColors {
    val seed = accentPalette.seed()
    val shift = seed.tintHue - MIST_BASE_HUE
    val chroma = seed.tintChroma
    fun t(c: Color): Color = retintOklch(c, hueShift = shift, chromaScale = chroma)

    val shadowInk = t(Color(0xFF0B152D))
    val mistTop = t(Color(0xFFEBF1FE))
    val mistBottom = t(Color(0xFFF6F7FC))
    val mistLow = t(Color(0xFFF6F9FE))
    val mistLowest = t(Color(0xFFFBFCFF))
    val mistContainer = t(Color(0xFFEEF1F9))
    val mistHigh = t(Color(0xFFE7EBF4))

    return MindColors(
        background = t(Color(0xFFF0F4FD)),
        surface = mistLowest,
        surfaceElevated = mistLow,
        surfaceSubtle = mistContainer,
        textPrimary = t(Color(0xFF1E232E)),
        textSecondary = t(Color(0xFF4F5562)),
        textMuted = t(Color(0xFF646974)),
        accent = seed.lightAccent,
        accentSoft = seed.lightSoft,
        onAccent = Color.White,
        borderSubtle = t(Color(0xFFDCE0E8)),
        success = Color(0xFF337250),
        warning = Color(0xFF936123),
        danger = Color(0xFFA54743),
        favourite = Color(0xFFC64E6B),
        isDark = false,
        accentSecondary = seed.glowLight,
        accentTertiary = seed.hazeLight,
        backdropTop = mistTop,
        backdropBottom = mistBottom,
        glowPrimary = seed.glowLight.copy(alpha = 0.42f),
        glowSecondary = seed.hazeLight.copy(alpha = 0.38f),
        hairline = shadowInk.copy(alpha = 0.08f),
        rimHighlight = Color.White.copy(alpha = 0.85f),
        glassCardFill = Color.White.copy(alpha = 0.64f),
        glassCardFallback = Color.White.copy(alpha = 0.78f),
        glassChromeFill = Color.White.copy(alpha = 0.74f),
        glassChromeFallback = mistLowest.copy(alpha = 0.96f),
        glassSheetFill = mistLow.copy(alpha = 0.90f),
        glassPillFill = Color.White.copy(alpha = 0.56f),
        shadowInk = shadowInk,
        successSoft = Color(0xFFD9F0E1),
        warningSoft = Color(0xFFF9E9CE),
        dangerSoft = Color(0xFFFFE2E0),
        identityContainers = LIGHT_IDENTITY_CONTAINERS,
        onIdentityContainers = LIGHT_ON_IDENTITY_CONTAINERS,
    ).also {
        // Keep mistHigh referenced so unused warning never creeps in if extended
        @Suppress("UNUSED_EXPRESSION")
        mistHigh
    }
}

fun mindDarkColors(accentPalette: AccentPalette): MindColors {
    val seed = accentPalette.seed()
    val shift = seed.tintHue - MIST_BASE_HUE
    val chroma = seed.tintChroma
    fun t(c: Color): Color = retintOklch(c, hueShift = shift, chromaScale = chroma)

    val nightTop = t(Color(0xFF1D222C))
    val nightBottom = t(Color(0xFF1B1C23))
    val smoke = t(Color(0xFF292E39))
    val sheetDark = t(Color(0xFF232832))
    val shadowNight = t(Color(0xFF03060D))

    return MindColors(
        background = t(Color(0xFF1C1F28)),
        surface = t(Color(0xFF232730)),
        surfaceElevated = t(Color(0xFF272C36)),
        surfaceSubtle = t(Color(0xFF2D323C)),
        textPrimary = t(Color(0xFFE6E9F0)),
        textSecondary = t(Color(0xFFB6BBC5)),
        textMuted = t(Color(0xFF9A9EA9)),
        accent = seed.darkAccent,
        accentSoft = seed.darkSoft,
        onAccent = seed.onDarkAccent,
        borderSubtle = t(Color(0xFF343841)),
        success = Color(0xFF79C79B),
        warning = Color(0xFFE5B567),
        danger = Color(0xFFE69089),
        favourite = Color(0xFFF09DB3),
        isDark = true,
        accentSecondary = seed.glowDark,
        accentTertiary = seed.hazeDark,
        backdropTop = nightTop,
        backdropBottom = nightBottom,
        glowPrimary = seed.glowDark.copy(alpha = 0.28f),
        glowSecondary = seed.hazeDark.copy(alpha = 0.24f),
        hairline = Color.White.copy(alpha = 0.08f),
        rimHighlight = Color.White.copy(alpha = 0.14f),
        glassCardFill = Color.White.copy(alpha = 0.055f),
        glassCardFallback = Color.White.copy(alpha = 0.07f),
        glassChromeFill = smoke.copy(alpha = 0.74f),
        glassChromeFallback = smoke.copy(alpha = 0.96f),
        glassSheetFill = sheetDark.copy(alpha = 0.92f),
        glassPillFill = Color.White.copy(alpha = 0.08f),
        shadowInk = shadowNight,
        successSoft = Color(0xFF1E3E2E),
        warningSoft = Color(0xFF42321C),
        dangerSoft = Color(0xFF542C29),
        identityContainers = DARK_IDENTITY_CONTAINERS,
        onIdentityContainers = DARK_ON_IDENTITY_CONTAINERS,
    )
}

internal fun MindColors.toMaterialColorScheme(): ColorScheme {
    return if (isDark) {
        darkColorScheme(
            primary = accent,
            onPrimary = onAccent,
            primaryContainer = accentSoft,
            onPrimaryContainer = accent,
            secondary = accent,
            onSecondary = onAccent,
            secondaryContainer = Color.White.copy(alpha = 0.09f),
            onSecondaryContainer = textPrimary,
            background = background,
            onBackground = textPrimary,
            surface = surface,
            onSurface = textPrimary,
            surfaceVariant = surfaceSubtle,
            onSurfaceVariant = textSecondary,
            outline = borderSubtle,
            outlineVariant = hairline,
            surfaceTint = Color.Transparent,
            error = danger,
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = onAccent,
            primaryContainer = accentSoft,
            onPrimaryContainer = accent,
            secondary = accent,
            onSecondary = onAccent,
            secondaryContainer = surfaceSubtle,
            onSecondaryContainer = textPrimary,
            background = background,
            onBackground = textPrimary,
            surface = surface,
            onSurface = textPrimary,
            surfaceVariant = surfaceSubtle,
            onSurfaceVariant = textSecondary,
            outline = borderSubtle,
            outlineVariant = hairline,
            surfaceTint = Color.Transparent,
            error = danger,
        )
    }
}

private fun retintOklch(color: Color, hueShift: Double, chromaScale: Double): Color {
    if (hueShift == 0.0 && chromaScale == 1.0) return color
    val r = toLinear(color.red.toDouble())
    val g = toLinear(color.green.toDouble())
    val b = toLinear(color.blue.toDouble())
    val lp = cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
    val mp = cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
    val sp = cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
    val l = 0.2104542553 * lp + 0.7936177850 * mp - 0.0040720468 * sp
    val a = 1.9779984951 * lp - 2.4285922050 * mp + 0.4505937099 * sp
    val bb = 0.0259040371 * lp + 0.7827717662 * mp - 0.8086757660 * sp
    var c = hypot(a, bb) * chromaScale
    val h = ((atan2(bb, a) * 180.0 / PI + hueShift) % 360.0 + 360.0) % 360.0

    var rgb = oklchToLinearRgb(l, c, h)
    if (!inGamut(rgb)) {
        var lo = 0.0
        var hi = c
        repeat(20) {
            val mid = (lo + hi) * 0.5
            if (inGamut(oklchToLinearRgb(l, mid, h))) lo = mid else hi = mid
        }
        c = lo
        rgb = oklchToLinearRgb(l, c, h)
    }
    return Color(
        red = (toGamma(rgb.first.coerceIn(0.0, 1.0)) * 255.0).roundToInt().coerceIn(0, 255),
        green = (toGamma(rgb.second.coerceIn(0.0, 1.0)) * 255.0).roundToInt().coerceIn(0, 255),
        blue = (toGamma(rgb.third.coerceIn(0.0, 1.0)) * 255.0).roundToInt().coerceIn(0, 255),
        alpha = (color.alpha * 255f).roundToInt().coerceIn(0, 255),
    )
}

private fun oklchToLinearRgb(l: Double, c: Double, hDeg: Double): Triple<Double, Double, Double> {
    val rad = hDeg * PI / 180.0
    val a = c * cos(rad)
    val b = c * sin(rad)
    val lp = l + 0.3963377774 * a + 0.2158037573 * b
    val mp = l - 0.1055613458 * a - 0.0638541728 * b
    val sp = l - 0.0894841775 * a - 1.2914855480 * b
    val l3 = lp * lp * lp
    val m3 = mp * mp * mp
    val s3 = sp * sp * sp
    return Triple(
        4.0767416621 * l3 - 3.3077115913 * m3 + 0.2309699292 * s3,
        -1.2684380046 * l3 + 2.6097574011 * m3 - 0.3413193965 * s3,
        -0.0041960863 * l3 - 0.7034186147 * m3 + 1.7076147010 * s3,
    )
}

private fun inGamut(rgb: Triple<Double, Double, Double>): Boolean {
    val eps = 1e-4
    return rgb.first in -eps..(1.0 + eps) &&
        rgb.second in -eps..(1.0 + eps) &&
        rgb.third in -eps..(1.0 + eps)
}

private fun toLinear(v: Double): Double = if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
private fun toGamma(v: Double): Double = if (v <= 0.0031308) 12.92 * v else 1.055 * max(v, 0.0).pow(1.0 / 2.4) - 0.055

val LocalMindColors = staticCompositionLocalOf { mindLightColors(AccentPalette.NEURAL_INDIGO) }
