package com.metranscriber.app.theme

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.annotation.RequiresApi


private val DarkColorScheme = darkColorScheme(
  primary = DeepIndigo,
  primaryContainer = DeepIndigoContainer,
  secondary = AccentCyan,
  background = PremiumBackground,
  surface = CardSurface,
  onBackground = LightText,
  onSurface = LightText
)

private val LightColorScheme = lightColorScheme(
  primary = DeepIndigo,
  primaryContainer = DeepIndigoContainer,
  secondary = AccentCyan,
  background = Color(0xFFF9F9FB),
  surface = Color.White,
  onBackground = Color(0xFF1C1B1F),
  onSurface = Color(0xFF1C1B1F)
)


@Composable
fun MeTranscriberTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        dynamicColorScheme(darkTheme)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

@RequiresApi(Build.VERSION_CODES.S)
@SuppressLint("NewApi")
@Composable
private fun dynamicColorScheme(darkTheme: Boolean) =
  if (darkTheme) dynamicDarkColorScheme(LocalContext.current) else dynamicLightColorScheme(LocalContext.current)
