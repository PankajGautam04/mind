package com.mind.app.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mind.app.core.designsystem.theme.MindTheme

/**
 * Minimal overlapping translucent isometric glass layers mark.
 */
@Composable
fun MindLogoMark(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    accentColor: Color = MindTheme.colors.accent,
) {
    val secondaryColor = MindTheme.colors.accentSecondary

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val tileSide = w * 0.48f
        val corner = CornerRadius(w * 0.12f, w * 0.12f)

        // 1. Bottom Isometric Layer
        val bottomCenter = Offset(w * 0.50f, h * 0.60f)
        scale(scaleX = 1f, scaleY = 0.64f, pivot = bottomCenter) {
            rotate(degrees = 45f, pivot = bottomCenter) {
                val topLeft = Offset(bottomCenter.x - tileSide / 2f, bottomCenter.y - tileSide / 2f)
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFFB7185), accentColor),
                        start = topLeft,
                        end = Offset(topLeft.x + tileSide, topLeft.y + tileSide),
                    ),
                    topLeft = topLeft,
                    size = Size(tileSide, tileSide),
                    cornerRadius = corner,
                )
            }
        }

        // 2. Top Translucent Isometric Layer
        val topCenter = Offset(w * 0.50f, h * 0.40f)
        scale(scaleX = 1f, scaleY = 0.64f, pivot = topCenter) {
            rotate(degrees = 45f, pivot = topCenter) {
                val topLeft = Offset(topCenter.x - tileSide / 2f, topCenter.y - tileSide / 2f)
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            secondaryColor.copy(alpha = 0.92f),
                            accentColor.copy(alpha = 0.86f),
                        ),
                        start = topLeft,
                        end = Offset(topLeft.x + tileSide, topLeft.y + tileSide),
                    ),
                    topLeft = topLeft,
                    size = Size(tileSide, tileSide),
                    cornerRadius = corner,
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.68f),
                    topLeft = topLeft,
                    size = Size(tileSide, tileSide),
                    cornerRadius = corner,
                    style = Stroke(width = w * 0.028f),
                )
            }
        }
    }
}
