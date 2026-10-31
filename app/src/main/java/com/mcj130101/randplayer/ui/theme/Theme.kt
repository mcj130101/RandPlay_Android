package com.mcj130101.randplayer.ui.theme

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF1E1E1E),
    onPrimaryContainer = TokenDarkForeground,
    secondary = TokenDarkForeground,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF1A1A1A),
    onSecondaryContainer = TokenDarkForeground,
    tertiary = BrandSuccess,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF1A1A1A),
    onTertiaryContainer = BrandSuccess,
    background = TokenDarkBackground,
    onBackground = TokenDarkForeground,
    surface = TokenDarkSurface,
    onSurface = TokenDarkForeground,
    surfaceVariant = Color(0xFF141414),
    onSurfaceVariant = TokenDarkMuted,
    surfaceContainerLow = Color(0xFF080808),
    surfaceContainer = Color(0xFF121212),
    surfaceContainerHigh = Color(0xFF1A1A1A),
    surfaceContainerHighest = Color(0xFF262626),
    outline = TokenDarkBorder,
    outlineVariant = Color(0xFF222222),
    error = BrandDanger,
    onError = Color.White,
    errorContainer = Color(0xFF3D1F1F),
    onErrorContainer = BrandDanger
)

private val LightColorScheme = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEEEEE),
    onPrimaryContainer = TokenLightForeground,
    secondary = TokenLightForeground,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF0F0F0),
    onSecondaryContainer = TokenLightForeground,
    tertiary = BrandSuccess,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE6F8F3),
    onTertiaryContainer = BrandSuccess,
    background = TokenLightBackground,
    onBackground = TokenLightForeground,
    surface = TokenLightSurface,
    onSurface = TokenLightForeground,
    surfaceVariant = Color(0xFFF2F2F2),
    onSurfaceVariant = TokenLightMuted,
    surfaceContainerLow = Color(0xFFFBFBFB),
    surfaceContainer = Color(0xFFF5F5F5),
    surfaceContainerHigh = Color(0xFFEFEFEF),
    surfaceContainerHighest = Color(0xFFE5E5E5),
    outline = TokenLightBorder,
    outlineVariant = Color(0xFFF0F0F0),
    error = BrandDanger,
    onError = Color.White,
    errorContainer = Color(0xFFFFEBEE),
    onErrorContainer = BrandDanger
)

@Composable
fun RandPlayerTheme(
    accentColorHex: String = "#000000",
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val bgArgb = if (darkTheme) {
                android.graphics.Color.BLACK
            } else {
                android.graphics.Color.parseColor("#FAFAFA")
            }
            window.statusBarColor = bgArgb
            window.navigationBarColor = bgArgb
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography
    ) {
        CompositionLocalProvider(LocalContentColor provides colorScheme.onBackground) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colorScheme.background)
            ) {
                content()
            }
        }
    }
}
