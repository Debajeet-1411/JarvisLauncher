package com.jarvis.launcher.ui.theme

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// DataStore extension
private val Context.themeDataStore by preferencesDataStore(name = "theme_settings")

/**
 * Theme Manager for Customizable Themes
 *
 * Day 5: User-selectable themes
 *
 * Features:
 * - Multiple theme presets
 * - Custom color schemes
 * - Dark/Light mode support
 * - AMOLED black theme
 * - Persistent theme storage
 */
class ThemeManager(private val context: Context) {

    companion object {
        private const val THEME_KEY = "selected_theme"
        private const val CUSTOM_PRIMARY_KEY = "custom_primary"
        private const val CUSTOM_SECONDARY_KEY = "custom_secondary"
        private const val CUSTOM_ACCENT_KEY = "custom_accent"
    }

    /**
     * Get current theme
     */
    fun getCurrentTheme(): Flow<JarvisTheme> {
        val key = stringPreferencesKey(THEME_KEY)
        return context.themeDataStore.data.map { preferences ->
            val themeName = preferences[key] ?: "cyberpunk"
            ThemePresets.getTheme(themeName)
        }
    }

    /**
     * Set theme
     */
    suspend fun setTheme(theme: JarvisTheme) {
        val key = stringPreferencesKey(THEME_KEY)
        context.themeDataStore.edit { preferences ->
            preferences[key] = theme.name
        }
    }

    /**
     * Get custom colors
     */
    fun getCustomColors(): Flow<Triple<String?, String?, String?>> {
        return context.themeDataStore.data.map { preferences ->
            Triple(
                preferences[stringPreferencesKey(CUSTOM_PRIMARY_KEY)],
                preferences[stringPreferencesKey(CUSTOM_SECONDARY_KEY)],
                preferences[stringPreferencesKey(CUSTOM_ACCENT_KEY)]
            )
        }
    }

    /**
     * Save custom theme
     */
    suspend fun saveCustomTheme(primary: Color, secondary: Color, accent: Color) {
        context.themeDataStore.edit { preferences ->
            preferences[stringPreferencesKey(CUSTOM_PRIMARY_KEY)] = primary.value.toString()
            preferences[stringPreferencesKey(CUSTOM_SECONDARY_KEY)] = secondary.value.toString()
            preferences[stringPreferencesKey(CUSTOM_ACCENT_KEY)] = accent.value.toString()
        }
    }
}

/**
 * JARVIS Theme data class
 */
data class JarvisTheme(
    val name: String,
    val displayName: String,
    val primary: Color,
    val secondary: Color,
    val accent: Color,
    val background: Color,
    val surface: Color,
    val onPrimary: Color,
    val onSecondary: Color,
    val onBackground: Color,
    val isDark: Boolean = true
)

/**
 * Theme Presets
 */
object ThemePresets {

    // Original Cyberpunk theme
    val CYBERPUNK = JarvisTheme(
        name = "cyberpunk",
        displayName = "Cyberpunk",
        primary = JarvisColors.neonCyan,
        secondary = JarvisColors.neonBlue,
        accent = JarvisColors.neonPurple,
        background = JarvisColors.spaceBlack,
        surface = JarvisColors.darkSlate,
        onPrimary = JarvisColors.spaceBlack,
        onSecondary = Color.White,
        onBackground = Color.White,
        isDark = true
    )

    // Matrix Green theme
    val MATRIX = JarvisTheme(
        name = "matrix",
        displayName = "Matrix",
        primary = Color(0xFF00FF00),
        secondary = Color(0xFF00AA00),
        accent = Color(0xFF88FF00),
        background = Color(0xFF000000),
        surface = Color(0xFF001100),
        onPrimary = Color(0xFF000000),
        onSecondary = Color(0xFF000000),
        onBackground = Color(0xFF00FF00),
        isDark = true
    )

    // Iron Man theme
    val IRON_MAN = JarvisTheme(
        name = "iron_man",
        displayName = "Iron Man",
        primary = Color(0xFFFFD700),
        secondary = Color(0xFFFF0000),
        accent = Color(0xFFFFAA00),
        background = Color(0xFF0A0A0A),
        surface = Color(0xFF1A0A0A),
        onPrimary = Color(0xFF000000),
        onSecondary = Color.White,
        onBackground = Color(0xFFFFD700),
        isDark = true
    )

    // Ocean Blue theme
    val OCEAN = JarvisTheme(
        name = "ocean",
        displayName = "Ocean",
        primary = Color(0xFF0088FF),
        secondary = Color(0xFF00DDFF),
        accent = Color(0xFF00AAFF),
        background = Color(0xFF001122),
        surface = Color(0xFF002244),
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = Color(0xFF00DDFF),
        isDark = true
    )

    // Purple Haze theme
    val PURPLE_HAZE = JarvisTheme(
        name = "purple_haze",
        displayName = "Purple Haze",
        primary = Color(0xFFAA00FF),
        secondary = Color(0xFFFF00FF),
        accent = Color(0xFFDD00FF),
        background = Color(0xFF0A0010),
        surface = Color(0xFF1A0030),
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = Color(0xFFFF00FF),
        isDark = true
    )

    // AMOLED Black theme
    val AMOLED = JarvisTheme(
        name = "amoled",
        displayName = "AMOLED",
        primary = Color(0xFF00DDFF),
        secondary = Color(0xFF0088FF),
        accent = Color(0xFF00AAFF),
        background = Color(0xFF000000),
        surface = Color(0xFF000000),
        onPrimary = Color(0xFF000000),
        onSecondary = Color.White,
        onBackground = Color.White,
        isDark = true
    )

    // Light theme
    val LIGHT = JarvisTheme(
        name = "light",
        displayName = "Light",
        primary = Color(0xFF0088FF),
        secondary = Color(0xFF00AAFF),
        accent = Color(0xFF0066FF),
        background = Color(0xFFF5F5F5),
        surface = Color.White,
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = Color(0xFF000000),
        isDark = false
    )

    // Sunset theme
    val SUNSET = JarvisTheme(
        name = "sunset",
        displayName = "Sunset",
        primary = Color(0xFFFF6600),
        secondary = Color(0xFFFF00AA),
        accent = Color(0xFFFFAA00),
        background = Color(0xFF1A0A00),
        surface = Color(0xFF2A1000),
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = Color(0xFFFFAA00),
        isDark = true
    )

    /**
     * All available themes
     */
    val ALL_THEMES = listOf(
        CYBERPUNK,
        MATRIX,
        IRON_MAN,
        OCEAN,
        PURPLE_HAZE,
        AMOLED,
        LIGHT,
        SUNSET
    )

    /**
     * Get theme by name
     */
    fun getTheme(name: String): JarvisTheme {
        return ALL_THEMES.find { it.name == name } ?: CYBERPUNK
    }
}

/**
 * Theme preview data
 */
data class ThemePreview(
    val theme: JarvisTheme,
    val icon: String
) {
    companion object {
        val PREVIEWS = listOf(
            ThemePreview(ThemePresets.CYBERPUNK, "🌃"),
            ThemePreview(ThemePresets.MATRIX, "💚"),
            ThemePreview(ThemePresets.IRON_MAN, "🦾"),
            ThemePreview(ThemePresets.OCEAN, "🌊"),
            ThemePreview(ThemePresets.PURPLE_HAZE, "💜"),
            ThemePreview(ThemePresets.AMOLED, "⚫"),
            ThemePreview(ThemePresets.LIGHT, "☀️"),
            ThemePreview(ThemePresets.SUNSET, "🌅")
        )
    }
}
