package com.example.ui.quiz

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private enum class ConfettiShape {
  RECTANGLE,
  CIRCLE,
  RIBBON
}

private data class ConfettiParticle(
  val startXFraction: Float,
  val startYFraction: Float,
  val velocityX: Float,
  val velocityY: Float,
  val gravity: Float,
  val widthPx: Float,
  val heightPx: Float,
  val color: Color,
  val shape: ConfettiShape,
  val initialRotation: Float,
  val rotationSpeed: Float,
  val swayAmplitude: Float,
  val swayFrequency: Float,
  val swayPhase: Float,
  val flipSpeed: Float,
  val spawnDelaySec: Float
)

private val ConfettiPalette = listOf(
  Color(0xFF3B82F6), // Bright Blue
  Color(0xFF10B981), // Emerald
  Color(0xFFF59E0B), // Amber Gold
  Color(0xFFEC4899), // Pink
  Color(0xFF8B5CF6), // Violet
  Color(0xFF06B6D4), // Cyan
  Color(0xFFEF4444)  // Coral Red
)

@Composable
fun ConfettiOverlay(
  modifier: Modifier = Modifier,
  durationMillis: Long = 4500L,
  particleCount: Int = 85
) {
  val particles = remember(particleCount) {
    val rng = Random(System.nanoTime())
    List(particleCount) {
      val shape = ConfettiShape.entries[rng.nextInt(ConfettiShape.entries.size)]
      val baseSize = rng.nextFloat() * 10f + 8f
      ConfettiParticle(
        startXFraction = rng.nextFloat(),
        startYFraction = -0.05f - rng.nextFloat() * 0.25f,
        velocityX = (rng.nextFloat() - 0.5f) * 260f,
        velocityY = rng.nextFloat() * 240f + 120f,
        gravity = rng.nextFloat() * 320f + 380f,
        widthPx = if (shape == ConfettiShape.RIBBON) baseSize * 0.55f else baseSize,
        heightPx = if (shape == ConfettiShape.RIBBON) baseSize * 1.9f else baseSize * 0.65f,
        color = ConfettiPalette[rng.nextInt(ConfettiPalette.size)],
        shape = shape,
        initialRotation = rng.nextFloat() * 360f,
        rotationSpeed = (rng.nextFloat() - 0.5f) * 340f,
        swayAmplitude = rng.nextFloat() * 38f + 12f,
        swayFrequency = rng.nextFloat() * 3.5f + 1.5f,
        swayPhase = rng.nextFloat() * 6.283f,
        flipSpeed = rng.nextFloat() * 6f + 3f,
        spawnDelaySec = rng.nextFloat() * 0.65f
      )
    }
  }

  var elapsedNanos by remember { mutableLongStateOf(0L) }
  val maxNanos = durationMillis * 1_000_000L

  LaunchedEffect(particles) {
    val startNanos = withFrameNanos { it }
    while (elapsedNanos < maxNanos) {
      withFrameNanos { frameNanos ->
        elapsedNanos = (frameNanos - startNanos).coerceAtLeast(0L)
      }
    }
  }

  if (elapsedNanos >= maxNanos) return

  val elapsedSec = elapsedNanos / 1_000_000_000f
  val totalSec = durationMillis / 1000f
  val fadeStartSec = totalSec * 0.68f
  val globalAlpha = if (elapsedSec <= fadeStartSec) {
    1f
  } else {
    (1f - (elapsedSec - fadeStartSec) / (totalSec - fadeStartSec)).coerceIn(0f, 1f)
  }

  Canvas(
    modifier = modifier
      .fillMaxSize()
      .testTag("confetti_overlay")
  ) {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return@Canvas

    particles.forEach { p ->
      val t = elapsedSec - p.spawnDelaySec
      if (t <= 0f) return@forEach

      val sway = sin(t * p.swayFrequency + p.swayPhase) * p.swayAmplitude
      val x = p.startXFraction * w + p.velocityX * t + sway
      val y = p.startYFraction * h + p.velocityY * t + 0.5f * p.gravity * t * t

      if (y > h + 40f || x < -60f || x > w + 60f) return@forEach

      val angle = p.initialRotation + p.rotationSpeed * t
      val flipScale = (0.35f + 0.65f * kotlin.math.abs(cos(t * p.flipSpeed)))
      val drawColor = p.color.copy(alpha = globalAlpha)

      rotate(degrees = angle, pivot = Offset(x, y)) {
        when (p.shape) {
          ConfettiShape.CIRCLE -> {
            drawCircle(
              color = drawColor,
              radius = p.widthPx * 0.5f,
              center = Offset(x, y)
            )
          }

          ConfettiShape.RECTANGLE,
          ConfettiShape.RIBBON -> {
            val scaledHeight = (p.heightPx * flipScale).coerceAtLeast(2f)
            drawRect(
              color = drawColor,
              topLeft = Offset(x - p.widthPx / 2f, y - scaledHeight / 2f),
              size = Size(p.widthPx, scaledHeight)
            )
          }
        }
      }
    }
  }
}
