package com.clouderp.wifireseller.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.clouderp.wifireseller.data.ThemePreference

private val LightColors = lightColorScheme(
    primary = Brand,
    onPrimary = Color.White,
    primaryContainer = BrandSoft,
    onPrimaryContainer = BrandDeep,
    secondary = BrandDeep,
    onSecondary = Color.White,
    background = Canvas,
    onBackground = Ink,
    surface = SurfaceLight,
    onSurface = Ink,
    surfaceVariant = BrandSoft,
    onSurfaceVariant = InkSoft,
    outline = Line,
    error = Danger,
    onError = Color.White,
    tertiary = Warn,
)

private val DarkColors = darkColorScheme(
    primary = BrandDark,
    onPrimary = BrandDeep,
    primaryContainer = BrandSoftDark,
    onPrimaryContainer = BrandDark,
    secondary = BrandDark,
    onSecondary = CanvasDark,
    background = CanvasDark,
    onBackground = InkDark,
    surface = SurfaceDark,
    onSurface = InkDark,
    surfaceVariant = BrandSoftDark,
    onSurfaceVariant = InkSoftDark,
    outline = LineDark,
    error = DangerDark,
    onError = CanvasDark,
    tertiary = WarnDark,
)

@Composable
fun WifiResellerTheme(
    preference: ThemePreference = ThemePreference.System,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (preference) {
        ThemePreference.System -> systemDark
        ThemePreference.Light -> false
        ThemePreference.Dark -> true
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = WifiTypography,
        content = content,
    )
}
