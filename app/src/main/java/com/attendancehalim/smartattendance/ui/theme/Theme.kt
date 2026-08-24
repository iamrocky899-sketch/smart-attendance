package com.attendancehalim.smartattendance.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = NavyPrimary,
    onPrimary = SurfaceWhite,
    primaryContainer = NavyLight,
    onPrimaryContainer = SurfaceWhite,
    secondary = BlueAccent,
    onSecondary = SurfaceWhite,
    secondaryContainer = SurfaceBorder,
    onSecondaryContainer = TextPrimary,
    tertiary = PunchInGreen,
    onTertiary = SurfaceWhite,
    background = BackgroundLight,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = BackgroundLight,
    onSurfaceVariant = TextSecondary,
    outline = SurfaceBorder
)

private val DarkColorScheme = darkColorScheme(
    primary = BlueAccentLight,
    onPrimary = NavyDark,
    primaryContainer = NavyPrimary,
    onPrimaryContainer = SurfaceWhite,
    secondary = BlueAccent,
    onSecondary = SurfaceWhite,
    background = NavyDark,
    onBackground = SurfaceWhite,
    surface = NavyPrimary,
    onSurface = SurfaceWhite,
    outline = NavyLight
)

@Composable
fun SmartAttendanceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = NavyDark.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}