package com.gow.eng192lab.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.gow.eng192lab.data.model.ThemeConfig

/**
 * Material 3 theme wrapper driven by a [ThemeConfig] loaded from JSON.
 *
 * Parses hex color strings from [config.colors] using [android.graphics.Color.parseColor]
 * and builds a [lightColorScheme] for [MaterialTheme]. Typography uses Material 3
 * defaults (Roboto); custom fonts can be added later via [ThemeFonts].
 *
 * Usage:
 * ```kotlin
 * val config by themeRepository.config.collectAsStateWithLifecycle()
 * AppTheme(config = config) {
 *     HomeScreen()
 * }
 * ```
 */
@Composable
fun AppTheme(
    config: ThemeConfig = ThemeConfig.default(),
    content: @Composable () -> Unit
) {
    val colors = config.colors

    val colorScheme = lightColorScheme(
        primary = colors.primary.toComposeColor(),
        secondary = colors.secondary.toComposeColor(),
        tertiary = colors.tertiary.toComposeColor(),
        background = colors.background.toComposeColor(),
        onPrimary = colors.onPrimary.toComposeColor(),
        onBackground = colors.onBackground.toComposeColor(),
        surface = colors.surface.toComposeColor(),
        onSurface = colors.onSurface.toComposeColor()
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}

/**
 * Converts a #RRGGBB or #AARRGGBB hex string to a Compose [Color].
 * Falls back to [Color.Gray] if parsing fails.
 */
private fun String.toComposeColor(): Color {
    return try {
        val argb = android.graphics.Color.parseColor(this)
        Color(argb)
    } catch (e: IllegalArgumentException) {
        Color.Gray
    }
}
