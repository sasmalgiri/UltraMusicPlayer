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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ==================== PREMIUM COLOR PALETTE ====================

// Base Purple Palette
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Ultra Music Primary Colors
val UltraBlue = Color(0xFF2196F3)
val UltraPurple = Color(0xFF9C27B0)
val UltraAccent = Color(0xFF00BCD4)

// Premium Gradient Colors
val UltraGradientStart = Color(0xFF667eea)
val UltraGradientEnd = Color(0xFF764ba2)
val NeonPink = Color(0xFFFF006E)
val NeonBlue = Color(0xFF00D4FF)
val NeonPurple = Color(0xFFBD00FF)
val NeonGreen = Color(0xFF39FF14)
val ElectricBlue = Color(0xFF00F5FF)
val VividPurple = Color(0xFF8B5CF6)
val HotPink = Color(0xFFFF1493)
val SunsetOrange = Color(0xFFFF6B35)
val MidnightBlue = Color(0xFF191970)

// Glassmorphism Colors
val GlassWhite = Color(0x33FFFFFF)
val GlassDark = Color(0x33000000)
val GlassBorder = Color(0x66FFFFFF)
val GlassBlur = Color(0x1AFFFFFF)

// Vibrant Accent Colors
val VibrantCyan = Color(0xFF00E5FF)
val VibrantMagenta = Color(0xFFFF00FF)
val VibrantYellow = Color(0xFFFFEA00)
val VibrantGreen = Color(0xFF00E676)
val VibrantRed = Color(0xFFFF1744)
val VibrantOrange = Color(0xFFFF9100)

// Speed/Pitch indicator colors
val SpeedFast = Color(0xFF00E676)  // Vibrant green
val SpeedSlow = Color(0xFFFF9100)  // Vibrant orange
val PitchHigh = Color(0xFFFF006E) // Neon pink
val PitchLow = Color(0xFF536DFE)  // Deep blue

// Battle mode colors - More vivid
val BattleRed = Color(0xFFFF1744)
val BattleGreen = Color(0xFF00E676)
val BattleOrange = Color(0xFFFF9100)
val BattlePurple = Color(0xFFD500F9)
val BattleBlue = Color(0xFF00B0FF)
val BattleYellow = Color(0xFFFFEA00)

// Card and Surface Colors - Dark Theme
val CardDark = Color(0xFF1A1A2E)
val CardDarkElevated = Color(0xFF16213E)
val SurfaceDark = Color(0xFF0F0F1A)
val SurfaceDarkElevated = Color(0xFF1A1A2E)

// Light theme colors
val LightBattleBlue = Color(0xFF1976D2)
val LightBattlePurple = Color(0xFF7B1FA2)
val LightBattleAccent = Color(0xFF00ACC1)
val LightCardBackground = Color(0xFFF8F9FA)
val LightCardSurface = Color(0xFFFFFFFF)

// ==================== PREMIUM GRADIENTS ====================

val PremiumGradient = Brush.linearGradient(
    colors = listOf(NeonPurple, NeonPink, SunsetOrange)
)

val CyberGradient = Brush.linearGradient(
    colors = listOf(NeonBlue, VibrantCyan, NeonGreen)
)

val SunsetGradient = Brush.linearGradient(
    colors = listOf(HotPink, SunsetOrange, VibrantYellow)
)

val OceanGradient = Brush.linearGradient(
    colors = listOf(MidnightBlue, UltraBlue, VibrantCyan)
)

val NeonGradient = Brush.linearGradient(
    colors = listOf(NeonPink, NeonPurple, NeonBlue)
)

val CardGradientDark = Brush.linearGradient(
    colors = listOf(
        Color(0xFF1A1A2E),
        Color(0xFF16213E)
    )
)

val AlbumArtOverlay = Brush.verticalGradient(
    colors = listOf(
        Color.Transparent,
        Color(0x40000000),
        Color(0xCC000000)
    )
)

private val DarkColorScheme = darkColorScheme(
    primary = VibrantCyan,
    secondary = NeonPurple,
    tertiary = NeonPink,
    background = SurfaceDark,
    surface = CardDark,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    surfaceVariant = CardDarkElevated,
    onSurfaceVariant = Color(0xFFE0E0E0),
    primaryContainer = Color(0xFF003A4F),
    onPrimaryContainer = VibrantCyan,
    secondaryContainer = Color(0xFF3D0054),
    onSecondaryContainer = Color(0xFFEBDDFF),
    outline = Color(0xFF3D3D5C),
    outlineVariant = Color(0xFF2A2A40),
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

