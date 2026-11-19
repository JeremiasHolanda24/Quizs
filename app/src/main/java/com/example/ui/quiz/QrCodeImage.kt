package com.example.ui.quiz

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

@Composable
fun QrCodeImage(
  content: String,
  contentDescription: String,
  modifier: Modifier = Modifier,
  size: Dp = 164.dp
) {
  val bitmap = remember(content) {
    generateQrBitmap(content = content, sizePx = 512)
  }

  if (bitmap != null) {
    Box(
      modifier = modifier
        .clip(RoundedCornerShape(16.dp))
        .background(Color.White)
        .padding(10.dp),
      contentAlignment = Alignment.Center
    ) {
      Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = Modifier.size(size)
      )
    }
  }
}

private fun generateQrBitmap(content: String, sizePx: Int): Bitmap? {
  if (content.isBlank()) return null
  return try {
    val hints = mapOf(
      EncodeHintType.MARGIN to 1,
      EncodeHintType.CHARACTER_SET to "UTF-8"
    )
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
    val pixels = IntArray(sizePx * sizePx)
    val black = android.graphics.Color.BLACK
    val white = android.graphics.Color.WHITE
    for (y in 0 until sizePx) {
      val rowOffset = y * sizePx
      for (x in 0 until sizePx) {
        pixels[rowOffset + x] = if (matrix[x, y]) black else white
      }
    }
    Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888).apply {
      setPixels(pixels, 0, sizePx, 0, 0, sizePx, sizePx)
    }
  } catch (_: Exception) {
    null
  }
}
