package com.jarvis.launcher.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * JARVIS Futuristic Color Scheme
 * Cyberpunk/Neon aesthetic with glowing accents
 */

// Primary Colors - Cyan/Blue Neon
val NeonCyan = Color(0xFF00F0FF)
val NeonCyanDark = Color(0xFF00B8D4)
val NeonBlue = Color(0xFF0080FF)
val ElectricBlue = Color(0xFF4FC3F7)

// Accent Colors
val NeonPurple = Color(0xFFB388FF)
val NeonPink = Color(0xFFFF4081)
val NeonGreen = Color(0xFF00FF9F)
val NeonYellow = Color(0xFFFFF176)

// Background Colors - Dark with depth
val SpaceBlack = Color(0xFF0A0E1A)
val DeepNavy = Color(0xFF0F172A)
val DarkSlate = Color(0xFF1E293B)
val MidnightBlue = Color(0xFF1A2332)

// Surface Colors - Glass morphism
val GlassLight = Color(0x1AFFFFFF)
val GlassMedium = Color(0x33FFFFFF)
val GlassDark = Color(0x0DFFFFFF)

// Text Colors
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xB3FFFFFF)
val TextTertiary = Color(0x80FFFFFF)

// Status Colors
val SuccessGreen = Color(0xFF00E676)
val WarningOrange = Color(0xFFFFAB40)
val ErrorRed = Color(0xFFFF5252)

/**
 * Dark color scheme with futuristic neon accents
 */
private val JarvisDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = SpaceBlack,
    primaryContainer = NeonCyanDark,
    onPrimaryContainer = TextPrimary,

    secondary = NeonBlue,
    onSecondary = SpaceBlack,
    secondaryContainer = ElectricBlue,
    onSecondaryContainer = TextPrimary,

    tertiary = NeonPurple,
    onTertiary = SpaceBlack,
    tertiaryContainer = NeonPurple,
    onTertiaryContainer = TextPrimary,

    background = SpaceBlack,
    onBackground = TextPrimary,

    surface = DarkSlate,
    onSurface = TextPrimary,
    surfaceVariant = MidnightBlue,
    onSurfaceVariant = TextSecondary,

    error = ErrorRed,
    onError = TextPrimary,

    outline = NeonCyan.copy(alpha = 0.3f),
    outlineVariant = GlassMedium,
)

/**
 * JARVIS Launcher Theme
 * Applies futuristic Material 3 theming
 */
@Composable
fun JarvisLauncherTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = JarvisDarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}

/**
 * Custom color extensions for easy access
 */
object JarvisColors {
    val neonCyan = NeonCyan
    val neonBlue = NeonBlue
    val neonPurple = NeonPurple
    val neonPink = NeonPink
    val neonGreen = NeonGreen
    val glassLight = GlassLight
    val glassMedium = GlassMedium
    val glassDark = GlassDark
    val spaceBlack = SpaceBlack
    val deepNavy = DeepNavy
    val darkSlate = DarkSlate
}
