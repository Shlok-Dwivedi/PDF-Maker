package com.nothing.pdfmaker.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = NothingWhite,
    onPrimary = NothingBlack,
    primaryContainer = NothingSurfaceLight,
    onPrimaryContainer = NothingWhite,
    secondary = NothingRed,
    onSecondary = NothingWhite,
    background = NothingBlack,
    onBackground = NothingWhite,
    surface = NothingSurface,
    onSurface = NothingWhite,
    surfaceVariant = NothingSurfaceLight,
    onSurfaceVariant = NothingGray,
    outline = NothingBorder
)

private val LightColorScheme = lightColorScheme(
    primary = NothingLightText,
    onPrimary = NothingLightSurface,
    primaryContainer = NothingLightSurfaceVariant,
    onPrimaryContainer = NothingLightText,
    secondary = NothingRed,
    onSecondary = NothingWhite,
    background = NothingLightBackground,
    onBackground = NothingLightText,
    surface = NothingLightSurface,
    onSurface = NothingLightText,
    surfaceVariant = NothingLightSurfaceVariant,
    onSurfaceVariant = NothingLightSubtext,
    outline = NothingLightBorder
)

val NothingShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun NothingLensTheme(
    isDarkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (isDarkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val bgColor = if (isDarkTheme) NothingBlack.toArgb() else NothingLightBackground.toArgb()
            window.statusBarColor = bgColor
            window.navigationBarColor = bgColor
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDarkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDarkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = NothingShapes,
        content = content
    )
}
