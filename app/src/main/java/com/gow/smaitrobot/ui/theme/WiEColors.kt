package com.gow.smaitrobot.ui.theme

/**
 * WiE 2026 brand color constants as #RRGGBB hex strings.
 *
 * These match the values in [app/src/main/assets/wie2026_theme.json] and are
 * provided as compile-time constants for use in tests, previews, and hardcoded
 * fallbacks. At runtime the [ThemeRepository] loads these from the JSON asset.
 */
object WiEColors {
    /** Primary brand color — WiE purple */
    const val PRIMARY = "#7B2D8B"

    /** Secondary accent — WiE teal */
    const val SECONDARY = "#00A99D"

    /** Tertiary accent — WiE orange */
    const val TERTIARY = "#F7941D"

    /** Page background — near-white */
    const val BACKGROUND = "#FAFAFA"

    /** Text/icon color on primary surfaces */
    const val ON_PRIMARY = "#FFFFFF"

    /** Text color on background */
    const val ON_BACKGROUND = "#1C1B1F"

    /** Card/sheet surface */
    const val SURFACE = "#FFFFFF"

    /** Text color on surface */
    const val ON_SURFACE = "#1C1B1F"
}
