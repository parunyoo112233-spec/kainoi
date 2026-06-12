package com.example.ui.theme

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

private val DarkColorScheme =
  darkColorScheme(
    primary = Color(0xFFD0BCFF),
    secondary = HighDensitySecondary,
    tertiary = HighDensityTertiary,
    background = HighDensityDarkBackground,
    surface = HighDensitySurfaceDark,
    onPrimary = Color(0xFF381E72),
    onSecondary = Color(0xFFEADDFF),
    onBackground = Color(0xFFE6E1E5),
    onSurface = Color(0xFFE6E1E5)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = HighDensityPrimary,
    secondary = HighDensitySecondary,
    tertiary = HighDensityTertiary,
    background = HighDensityLightBackground,
    surface = HighDensitySurfaceLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1D1B20),
    onSurface = Color(0xFF1D1B20)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disabling dynamic colors by default so our custom Petrol details stand out
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
