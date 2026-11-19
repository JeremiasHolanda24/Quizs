package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class QuizExtendedColors(
  val cardBackground: Color,
  val surfaceElevated: Color,
  val inputBackground: Color,
  val border: Color,
  val accentBadgeBg: Color,
  val accentBadgeText: Color,
  val categoryBadgeBg: Color,
  val categoryBadgeText: Color,
  val successBg: Color,
  val successBorder: Color,
  val successText: Color,
  val errorBg: Color,
  val errorBorder: Color,
  val errorText: Color,
  val maybeBg: Color,
  val maybeBorder: Color,
  val maybeText: Color,
  val optionCorrectBg: Color,
  val optionWrongBg: Color,
  val optionMaybeBg: Color
)

private val DarkExtendedColors = QuizExtendedColors(
  cardBackground = DarkCardBg,
  surfaceElevated = DarkSurfaceBg,
  inputBackground = DarkInputBg,
  border = DarkBorder,
  accentBadgeBg = DarkAccentBadgeBg,
  accentBadgeText = DarkAccentBadgeText,
  categoryBadgeBg = DarkCatBadgeBg,
  categoryBadgeText = DarkCatBadgeText,
  successBg = DarkSuccessBg,
  successBorder = DarkSuccessBorder,
  successText = DarkSuccessText,
  errorBg = DarkErrorBg,
  errorBorder = DarkErrorBorder,
  errorText = DarkErrorText,
  maybeBg = DarkMaybeBg,
  maybeBorder = DarkMaybeBorder,
  maybeText = DarkMaybeText,
  optionCorrectBg = OptionCorrectFill,
  optionWrongBg = OptionWrongFill,
  optionMaybeBg = OptionMaybeFill
)

private val LightExtendedColors = QuizExtendedColors(
  cardBackground = LightCardBg,
  surfaceElevated = LightSurfaceBg,
  inputBackground = LightInputBg,
  border = LightBorder,
  accentBadgeBg = LightAccentBadgeBg,
  accentBadgeText = LightAccentBadgeText,
  categoryBadgeBg = LightCatBadgeBg,
  categoryBadgeText = LightCatBadgeText,
  successBg = LightSuccessBg,
  successBorder = LightSuccessBorder,
  successText = LightSuccessText,
  errorBg = LightErrorBg,
  errorBorder = LightErrorBorder,
  errorText = LightErrorText,
  maybeBg = LightMaybeBg,
  maybeBorder = LightMaybeBorder,
  maybeText = LightMaybeText,
  optionCorrectBg = OptionCorrectFill,
  optionWrongBg = OptionWrongFill,
  optionMaybeBg = OptionMaybeFill
)

val LocalQuizColors = staticCompositionLocalOf { DarkExtendedColors }

private val DarkColorScheme = darkColorScheme(
  primary = DarkPrimary,
  onPrimary = Color.White,
  primaryContainer = DarkAccentBadgeBg,
  onPrimaryContainer = DarkAccentBadgeText,
  secondary = DarkCatBadgeText,
  onSecondary = DarkBodyBg,
  secondaryContainer = DarkCatBadgeBg,
  onSecondaryContainer = DarkCatBadgeText,
  background = DarkBodyBg,
  onBackground = DarkTextMain,
  surface = DarkCardBg,
  onSurface = DarkTextMain,
  surfaceVariant = DarkSurfaceBg,
  onSurfaceVariant = DarkTextMuted,
  outline = DarkBorder,
  error = OptionWrongFill,
  onError = Color.White
)

private val LightColorScheme = lightColorScheme(
  primary = LightPrimary,
  onPrimary = Color.White,
  primaryContainer = LightAccentBadgeBg,
  onPrimaryContainer = LightAccentBadgeText,
  secondary = LightCatBadgeText,
  onSecondary = Color.White,
  secondaryContainer = LightCatBadgeBg,
  onSecondaryContainer = LightCatBadgeText,
  background = LightBodyBg,
  onBackground = LightTextMain,
  surface = LightCardBg,
  onSurface = LightTextMain,
  surfaceVariant = LightSurfaceBg,
  onSurfaceVariant = LightTextMuted,
  outline = LightBorder,
  error = OptionWrongFill,
  onError = Color.White
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

  CompositionLocalProvider(LocalQuizColors provides extendedColors) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      content = content
    )
  }
}

object QuizTheme {
  val colors: QuizExtendedColors
    @Composable
    get() = LocalQuizColors.current
}
