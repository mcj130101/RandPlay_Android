package com.example.randplayer.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    background = DarkBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    secondaryContainer = Blue900,
    onSecondaryContainer = Blue200,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    background = LightBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    secondaryContainer = Blue100,
    onSecondaryContainer = Blue900,
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002)
)

fun getDynamicColorScheme(accentHex: String, isDark: Boolean): ColorScheme {
    val baseColor = try {
        Color(android.graphics.Color.parseColor(accentHex))
    } catch (e: Exception) {
        Blue500
    }

    return if (isDark) {
        darkColorScheme(
            primary = baseColor,
            onPrimary = Color.Black,
            primaryContainer = baseColor.copy(alpha = 0.3f),
            onPrimaryContainer = Color.White,
            secondary = baseColor.copy(alpha = 0.7f),
            onSecondary = Color.Black,
            background = Slate950,
            surface = Slate900,
            onSurface = Slate100,
            surfaceVariant = Slate800,
            onSurfaceVariant = Slate100,
            outline = Slate700,
            secondaryContainer = baseColor.copy(alpha = 0.2f),
            onSecondaryContainer = Color.White
        )
    } else {
        lightColorScheme(
            primary = baseColor,
            onPrimary = Color.White,
            primaryContainer = baseColor.copy(alpha = 0.1f),
            onPrimaryContainer = baseColor,
            secondary = baseColor.copy(alpha = 0.8f),
            onSecondary = Color.White,
            background = Slate50,
            surface = Color.White,
            onSurface = Slate950,
            surfaceVariant = Slate100,
            onSurfaceVariant = Slate800,
            outline = Slate100,
            secondaryContainer = baseColor.copy(alpha = 0.05f),
            onSecondaryContainer = baseColor
        )
    }
}

@Composable
fun AmbientAuraBackground(
    primaryColor: Color,
    isDark: Boolean
) {
    val secondaryColor = rememberSecondaryColor(primaryColor)
    val infiniteTransition = rememberInfiniteTransition(label = "aura")
    
    // Animation phases for floating movement
    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Restart),
        label = "phase1"
    )
    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(15000, easing = LinearEasing), RepeatMode.Restart),
        label = "phase2"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .blur(80.dp) // Deep blur for the aura effect
    ) {
        val w = size.width
        val h = size.height
        
        // Blob 1: Primary (Top Left area)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primaryColor.copy(alpha = if (isDark) 0.5f else 0.4f), Color.Transparent),
                center = Offset(
                    w * (0.2f + 0.15f * cos(phase1)),
                    h * (0.2f + 0.1f * sin(phase1))
                ),
                radius = w * 0.85f
            ),
            center = Offset(w * 0.2f, h * 0.2f),
            radius = w * 0.85f
        )

        // Blob 2: Secondary (Bottom Right area)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(secondaryColor.copy(alpha = if (isDark) 0.4f else 0.3f), Color.Transparent),
                center = Offset(
                    w * (0.8f + 0.1f * cos(phase2)),
                    h * (0.8f + 0.15f * sin(phase2))
                ),
                radius = w * 0.75f
            ),
            center = Offset(w * 0.8f, h * 0.8f),
            radius = w * 0.75f
        )

        // Blob 3: Blended (Center Right area)
        val blendedColor = primaryColor.copy(alpha = if (isDark) 0.3f else 0.2f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(blendedColor, Color.Transparent),
                center = Offset(
                    w * (0.7f + 0.12f * sin(phase1 * 0.8f)),
                    h * (0.4f + 0.12f * cos(phase2 * 0.9f))
                ),
                radius = w * 0.65f
            ),
            center = Offset(w * 0.7f, h * 0.4f),
            radius = w * 0.65f
        )
        
        // Blob 4: Soft Highlight (Bottom Left area)
        val accentColor = secondaryColor.copy(alpha = if (isDark) 0.2f else 0.15f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accentColor, Color.Transparent),
                center = Offset(
                    w * (0.3f + 0.1f * cos(phase2 * 1.1f)),
                    h * (0.7f + 0.1f * sin(phase1 * 0.7f))
                ),
                radius = w * 0.7f
            ),
            center = Offset(w * 0.3f, h * 0.7f),
            radius = w * 0.7f
        )
    }
}

@Composable
fun rememberSecondaryColor(primary: Color): Color {
    return androidx.compose.runtime.remember(primary) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(primary.toArgb(), hsv)
        // Instead of shifting hue, we adjust saturation and value for a monochromatic look
        hsv[1] = (hsv[1] * 0.8f).coerceIn(0f, 1f) // Desaturate slightly
        hsv[2] = (hsv[2] * 1.2f).coerceIn(0f, 1f) // Brighten slightly
        Color(android.graphics.Color.HSVToColor(hsv))
    }
}

fun Color.lerp(other: Color, fraction: Float): Color {
    return Color(
        red = red + (other.red - red) * fraction,
        green = green + (other.green - green) * fraction,
        blue = blue + (other.blue - blue) * fraction,
        alpha = alpha + (other.alpha - alpha) * fraction
    )
}

@Composable
fun RandPlayerTheme(
    accentColorHex: String = "#3B82F6",
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val isDefaultBlue = accentColorHex.equals("#3B82F6", ignoreCase = true)
    
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDefaultBlue -> if (darkTheme) DarkColorScheme else LightColorScheme
        else -> getDynamicColorScheme(accentColorHex, darkTheme)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
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
                if (isDefaultBlue) {
                    // Original Blue Theme Background
                    AsyncImage(
                        model = com.example.randplayer.R.drawable.background,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(if (darkTheme) 10.dp else 16.dp),
                        contentScale = ContentScale.Crop,
                        alpha = if (darkTheme) 0.6f else 0.4f
                    )
                    
                    // Scrim overlay to ensure legibility
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                colorScheme.background.copy(alpha = if (darkTheme) 0.6f else 0.7f)
                            )
                    )
                } else {
                    // Animated Aura Layer for other colors
                    AmbientAuraBackground(
                        primaryColor = colorScheme.primary,
                        isDark = darkTheme
                    )
                }
                
                // Content Layer
                content()
            }
        }
    }
}
