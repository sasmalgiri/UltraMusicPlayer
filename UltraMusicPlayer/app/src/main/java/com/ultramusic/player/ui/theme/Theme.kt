package com.ultramusic.player.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Custom colors for UltraMusic Player
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Ultra Music accent colors
val UltraBlue = Color(0xFF2196F3)
val UltraPurple = Color(0xFF9C27B0)
val UltraGradientStart = Color(0xFF667eea)
val UltraGradientEnd = Color(0xFF764ba2)
val UltraAccent = Color(0xFF00BCD4)

// Speed/Pitch indicator colors
val SpeedFast = Color(0xFF4CAF50)
val SpeedSlow = Color(0xFFFF9800)
val PitchHigh = Color(0xFFE91E63)
val PitchLow = Color(0xFF3F51B5)

// Battle mode colors
val BattleRed = Color(0xFFE53935)
val BattleGreen = Color(0xFF43A047)
val BattleOrange = Color(0xFFFF9800)
val BattlePurple = Color(0xFF7B1FA2)

// Light theme battle colors (vibrant but readable)
val LightBattleBlue = Color(0xFF1976D2)
val LightBattlePurple = Color(0xFF7B1FA2)
val LightBattleAccent = Color(0xFF00ACC1)
val LightCardBackground = Color(0xFFF5F5F5)
val LightCardSurface = Color(0xFFFFFFFF)

private val DarkColorScheme = darkColorScheme(
    primary = UltraBlue,
    secondary = UltraPurple,
    tertiary = UltraAccent,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2D2D2D),
    onSurfaceVariant = Color(0xFFCACACA),
)

private val LightColorScheme = lightColorScheme(
    primary = LightBattleBlue,
    secondary = LightBattlePurple,
    tertiary = LightBattleAccent,
    background = Color(0xFFFAFAFA),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = LightCardBackground,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFFE0E0E0),
    outlineVariant = Color(0xFFCAC4D0),
    error = BattleRed,
    errorContainer = Color(0xFFFFDAD6),
    onError = Color.White,
    onErrorContainer = Color(0xFF410002),
)

/**
 * Theme mode options
 */
enum class ThemeMode {
    LIGHT,      // Always light theme
    DARK,       // Always dark theme
    SYSTEM      // Follow system setting
}

@Composable
fun UltraMusicPlayerTheme(
    themeMode: ThemeMode = ThemeMode.LIGHT,  // Default to LIGHT for better visibility
    dynamicColor: Boolean = false,  // Disable dynamic colors for consistent branding
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
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
        content = content
    )
}

