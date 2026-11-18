package com.mind.app.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import com.mind.app.core.model.AccentPalette
import com.mind.app.core.model.ThemeMode

/**
 * Root theme entrypoint applying Mind's custom visual identity, light/dark modes, and accent palettes.
 */
@Composable
fun MindTheme(
    themeMode: ThemeMode = ThemeMode.FOLLOW_SYSTEM,
    accentPalette: AccentPalette = AccentPalette.NEURAL_INDIGO,
    content: @Composable () -> Unit,
) {
    val useDarkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.FOLLOW_SYSTEM -> isSystemInDarkTheme()
    }

    val mindColors = if (useDarkTheme) {
        mindDarkColors(accentPalette)
    } else {
        mindLightColors(accentPalette)
    }

    val spacing = MindSpacing()

    CompositionLocalProvider(
        LocalMindColors provides mindColors,
        LocalMindSpacing provides spacing,
        LocalContentColor provides mindColors.textPrimary,
    ) {
        MaterialTheme(
            colorScheme = mindColors.toMaterialColorScheme(),
            typography = MindTypography,
            shapes = MindMaterialShapes,
        ) {
            CompositionLocalProvider(
                LocalContentColor provides mindColors.textPrimary,
                content = content,
            )
        }
    }
}

object MindTheme {
    val colors: MindColors
        @Composable
        @ReadOnlyComposable
        get() = LocalMindColors.current

    val spacing: MindSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalMindSpacing.current

    val typography: Typography
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography

    val shapes: Shapes
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.shapes
}
