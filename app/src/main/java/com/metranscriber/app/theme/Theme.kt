package com.metranscriber.app.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color


private val DarkColorScheme = darkColorScheme(
  primary = DeepIndigo,
  primaryContainer = DeepIndigoContainer,
  onPrimary = LightText,
  onPrimaryContainer = LightText,
  secondary = AccentCyan,
  onSecondary = Color(0xFF021014),
  secondaryContainer = Color(0xFF07323A),
  onSecondaryContainer = LightText,
  tertiary = GoldenSun,
  onTertiary = Color(0xFF201200),
  tertiaryContainer = Color(0xFF4D3200),
  onTertiaryContainer = LightText,
  background = PremiumBackground,
  surface = CardSurface,
  surfaceVariant = PanelSurface,
  surfaceContainer = PanelSurface,
  surfaceContainerHigh = PanelSurfaceHigh,
  onBackground = LightText,
  onSurface = LightText,
  onSurfaceVariant = MutedText,
  outline = SignalStroke,
  error = RecordingRed,
  onError = LightText
)

private val LightColorScheme = lightColorScheme(
  primary = DeepIndigo,
  primaryContainer = DeepIndigoContainer,
  onPrimary = Color(0xFFFDF8FF),
  onPrimaryContainer = Color(0xFFFDF8FF),
  secondary = AccentCyan,
  onSecondary = Color(0xFF041014),
  tertiary = SignalOrange,
  background = Color(0xFFF6F1FF),
  surface = Color(0xFFFCF8FF),
  surfaceVariant = Color(0xFFE8DFF8),
  onBackground = Color(0xFF171020),
  onSurface = Color(0xFF171020),
  onSurfaceVariant = Color(0xFF5C536C),
  outline = Color(0xFFCBBDE0),
  error = RecordingRed
)


@Composable
fun MeTranscriberTheme(
  darkTheme: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
