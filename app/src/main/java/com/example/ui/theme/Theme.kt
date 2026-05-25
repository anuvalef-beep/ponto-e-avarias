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
    primary = MintGreenDark,
    secondary = PaleSageDark,
    tertiary = DarkCreamText,
    background = MossCharcoalBgDark,
    surface = SurfaceDark,
    onPrimary = DeepForestDark,
    onSecondary = MossCharcoalBgDark,
    onBackground = SoftWhiteTextDark,
    onSurface = SoftWhiteTextDark,
    primaryContainer = DeepForestDark,
    onPrimaryContainer = SoftWhiteTextDark,
    secondaryContainer = SurfaceVariantDark,
    onSecondaryContainer = PaleSageDark,
    tertiaryContainer = WarmBronzeDark,
    onTertiaryContainer = DarkCreamText,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = LeafGrayDark,
    outline = LeafGrayDark
  )

private val LightColorScheme =
  lightColorScheme(
    primary = ForageGreenVal,
    secondary = MediumSageVal,
    tertiary = SandClayVal,
    background = NaturalBgVal,
    surface = androidx.compose.ui.graphics.Color.White,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    onSecondary = DarkForestCharcoalVal,
    onTertiary = DarkForestCharcoalVal,
    onBackground = DarkForestCharcoalVal,
    onSurface = DarkForestCharcoalVal,
    primaryContainer = LightSageVal,
    onPrimaryContainer = LeafGrayVal,
    secondaryContainer = LightSageVal,
    onSecondaryContainer = LeafGrayVal,
    tertiaryContainer = SandClayVal,
    onTertiaryContainer = DarkForestCharcoalVal,
    surfaceVariant = LightSageVal,
    onSurfaceVariant = MutedSageVal,
    outline = WarmSandGrayVal
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
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
