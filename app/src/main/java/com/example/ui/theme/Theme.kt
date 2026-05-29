package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = NeonEmerald,
    secondary = SoftMint,
    tertiary = ElectricIndigo,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = CipherWireWhite,
    onSurface = CipherWireWhite,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = CipherWireGray,
    outline = CipherWireOutline
  )

private val LightColorScheme =
  lightColorScheme(
    primary = DarkBackground,
    secondary = PrimaryTealShadow,
    tertiary = ElectricIndigo,
    background = LightBackground,
    surface = LightSurface,
    onBackground = DarkBackground,
    onSurface = DarkBackground
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Default to gorgeous Slate Dark theme for cryptography!
  dynamicColor: Boolean = false, // Use our handcrafted design for distinctiveness
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
