package com.example.ui.quiz

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Draws a soft glowing vignette strictly along the four screen edges
 * (green for correct, amber/yellow for contextual maybe, red for incorrect)
 * without covering the center content.
 */
@Composable
fun AnswerEdgeFlashOverlay(
  outcome: AnswerOutcome?,
  alpha: Float,
  modifier: Modifier = Modifier
) {
  if (outcome == null || alpha <= 0.005f) return

  val baseColor = when (outcome) {
    AnswerOutcome.CORRECT -> Color(0xFF10B981)
    AnswerOutcome.MAYBE -> Color(0xFFF59E0B)
    AnswerOutcome.WRONG -> Color(0xFFEF4444)
  }
  val edgeColor = baseColor.copy(alpha = (alpha * 0.48f).coerceIn(0f, 1f))
  val rimColor = baseColor.copy(alpha = (alpha * 0.72f).coerceIn(0f, 1f))

  Canvas(modifier = modifier.fillMaxSize()) {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return@Canvas

    val glowDepth = 64.dp.toPx().coerceAtMost(minOf(w, h) * 0.25f)

    // Top edge glow
    drawRect(
      brush = Brush.verticalGradient(
        colors = listOf(edgeColor, Color.Transparent),
        startY = 0f,
        endY = glowDepth
      ),
      topLeft = Offset.Zero,
      size = Size(w, glowDepth)
    )

    // Bottom edge glow
    drawRect(
      brush = Brush.verticalGradient(
        colors = listOf(Color.Transparent, edgeColor),
        startY = h - glowDepth,
        endY = h
      ),
      topLeft = Offset(0f, h - glowDepth),
      size = Size(w, glowDepth)
    )

    // Left edge glow
    drawRect(
      brush = Brush.horizontalGradient(
        colors = listOf(edgeColor, Color.Transparent),
        startX = 0f,
        endX = glowDepth
      ),
      topLeft = Offset.Zero,
      size = Size(glowDepth, h)
    )

    // Right edge glow
    drawRect(
      brush = Brush.horizontalGradient(
        colors = listOf(Color.Transparent, edgeColor),
        startX = w - glowDepth,
        endX = w
      ),
      topLeft = Offset(w - glowDepth, 0f),
      size = Size(glowDepth, h)
    )

    // Subtle crisp outer rim
    val strokePx = 6.dp.toPx()
    drawRect(
      color = rimColor,
      topLeft = Offset(strokePx / 2f, strokePx / 2f),
      size = Size(w - strokePx, h - strokePx),
      style = Stroke(width = strokePx)
    )
  }
}
