package com.gov.employeedb.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/*
 * Zolvex colour system.
 *
 * The scheme is derived from the single brand seed #1B5E5A rather than hand-picked, so every
 * role keeps a predictable contrast relationship with the others. The derivation follows the
 * Material 3 tonal-palette model (PLAN.md ADR-2, Appendix A A13):
 *
 *  1. The seed is converted to CIELAB/CIELCh: hue 190°, tone (Lightness) 36.
 *  2. Five tonal palettes are built at that hue with Material's baseline chroma ratios of the
 *     neutral series (primary 36, secondary 16, tertiary 24, neutral 4, neutral-variant 6 —
 *     the tertiary palette rotating 60° to hue 250° so it stays distinguishable from primary).
 *  3. Each role is the palette entry at a fixed tone (L*), which is what the Material 3 role
 *     spec prescribes: primary = tone 40 (light) / tone 80 (dark), containers = 90/30, and so on.
 *  4. Chroma is clamped into the sRGB gamut per tone, so dark tones stay black-ish instead of
 *     being clipped to a flat #000000.
 *
 * Verified against the Material 3 baseline palette: the same procedure reproduces #6750A4 at
 * tone 40, #EADDFF at tone 90 and #21005D at tone 10 exactly, which is the check that the
 * tone calibration is correct. Error roles are deliberately NOT tinted: they use the Material 3
 * baseline red so an error can never be mistaken for a brand-teal success state.
 */

/** Light theme: the Material 3 light role mapping of the Zolvex tonal palettes. */
val ZolvexLightColors: ColorScheme = lightColorScheme(
    // Primary — tone 40 of the primary palette; containers at 90/10.
    primary = Color(0xFF006A65), // primary 40
    onPrimary = Color(0xFFFFFFFF), // primary 100
    primaryContainer = Color(0xFFB7EDE8), // primary 90
    onPrimaryContainer = Color(0xFF00201E), // primary 10
    inversePrimary = Color(0xFF5FD9D1), // primary 80

    // Secondary — the same hue, lower chroma; still part of the brand family.
    secondary = Color(0xFF226864), // secondary 40
    onSecondary = Color(0xFFFFFFFF), // secondary 100
    secondaryContainer = Color(0xFFD0E7E5), // secondary 90
    onSecondaryContainer = Color(0xFF00201E), // secondary 10

    // Tertiary — hue rotated 60° for the accent that marks "new" and "selected".
    tertiary = Color(0xFF00658C), // tertiary 40
    onTertiary = Color(0xFFFFFFFF), // tertiary 100
    tertiaryContainer = Color(0xFFCEE6F8), // tertiary 90
    onTertiaryContainer = Color(0xFF001E2E), // tertiary 10

    // Surfaces — near-neutral so tables and charts stay readable for long sessions.
    background = Color(0xFFFCFCFC), // neutral 99
    onBackground = Color(0xFF0F1E1D), // neutral 10
    surface = Color(0xFFF8F9F9), // neutral 98
    onSurface = Color(0xFF0F1E1D), // neutral 10
    surfaceVariant = Color(0xFFDCE4E3), // neutral-variant 90
    onSurfaceVariant = Color(0xFF354B49), // neutral-variant 30
    surfaceTint = Color(0xFF006A65), // primary 40
    inverseSurface = Color(0xFF253332), // neutral 20
    inverseOnSurface = Color(0xFFEEF1F1), // neutral 95

    // Error — Material 3 baseline red, intentionally outside the seed hue.
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    outline = Color(0xFF677B79), // neutral-variant 50
    outlineVariant = Color(0xFFB9CAC8), // neutral-variant 80
    scrim = Color(0xFF000000),

    // Surface containers — the five tonal steps Material 3 components use for hierarchy.
    surfaceBright = Color(0xFFF8F9F9), // neutral 98
    surfaceContainer = Color(0xFFEBEEEE), // neutral 94
    surfaceContainerHigh = Color(0xFFE4E9E8), // neutral 92
    surfaceContainerHighest = Color(0xFFDEE4E3), // neutral 90
    surfaceContainerLow = Color(0xFFF2F4F4), // neutral 96
    surfaceContainerLowest = Color(0xFFFFFFFF), // neutral 100
    surfaceDim = Color(0xFFD4DBDB), // neutral 87

    // Fixed roles — identical in both themes, for content that must not flip with the theme.
    primaryFixed = Color(0xFFB7EDE8), // primary 90
    primaryFixedDim = Color(0xFF5FD9D1), // primary 80
    onPrimaryFixed = Color(0xFF00201E), // primary 10
    onPrimaryFixedVariant = Color(0xFF00504C), // primary 30
    secondaryFixed = Color(0xFFD0E7E5), // secondary 90
    secondaryFixedDim = Color(0xFFA1CFCB), // secondary 80
    onSecondaryFixed = Color(0xFF00201E), // secondary 10
    onSecondaryFixedVariant = Color(0xFF00504C), // secondary 30
    tertiaryFixed = Color(0xFFCEE6F8), // tertiary 90
    tertiaryFixedDim = Color(0xFF99CDF0), // tertiary 80
    onTertiaryFixed = Color(0xFF001E2E), // tertiary 10
    onTertiaryFixedVariant = Color(0xFF004C6A), // tertiary 30
)

/** Dark theme: the same palettes read at the Material 3 dark tone steps. */
val ZolvexDarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF5FD9D1), // primary 80
    onPrimary = Color(0xFF003734), // primary 20
    primaryContainer = Color(0xFF00504C), // primary 30
    onPrimaryContainer = Color(0xFFB7EDE8), // primary 90
    inversePrimary = Color(0xFF006A65), // primary 40

    secondary = Color(0xFFA1CFCB), // secondary 80
    onSecondary = Color(0xFF003734), // secondary 20
    secondaryContainer = Color(0xFF00504C), // secondary 30
    onSecondaryContainer = Color(0xFFD0E7E5), // secondary 90

    tertiary = Color(0xFF99CDF0), // tertiary 80
    onTertiary = Color(0xFF00344B), // tertiary 20
    tertiaryContainer = Color(0xFF004C6A), // tertiary 30
    onTertiaryContainer = Color(0xFFCEE6F8), // tertiary 90

    background = Color(0xFF0F1E1D), // neutral 10
    onBackground = Color(0xFFDEE4E3), // neutral 90
    surface = Color(0xFF0F1E1D), // neutral 10
    onSurface = Color(0xFFDEE4E3), // neutral 90
    surfaceVariant = Color(0xFF354B49), // neutral-variant 30
    onSurfaceVariant = Color(0xFFB9CAC8), // neutral-variant 80
    surfaceTint = Color(0xFF5FD9D1), // primary 80
    inverseSurface = Color(0xFFDEE4E3), // neutral 90
    inverseOnSurface = Color(0xFF253332), // neutral 20

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    outline = Color(0xFF819593), // neutral-variant 60
    outlineVariant = Color(0xFF354B49), // neutral-variant 30
    scrim = Color(0xFF000000),

    surfaceBright = Color(0xFF2E3C3A), // neutral 24
    surfaceContainer = Color(0xFF142221), // neutral 12
    surfaceContainerHigh = Color(0xFF1E2D2B), // neutral 17
    surfaceContainerHighest = Color(0xFF2A3736), // neutral 22
    surfaceContainerLow = Color(0xFF0F1E1D), // neutral 10
    surfaceContainerLowest = Color(0xFF07100F), // neutral 4
    surfaceDim = Color(0xFF0B1514), // neutral 6

    primaryFixed = Color(0xFFB7EDE8), // primary 90
    primaryFixedDim = Color(0xFF5FD9D1), // primary 80
    onPrimaryFixed = Color(0xFF00201E), // primary 10
    onPrimaryFixedVariant = Color(0xFF00504C), // primary 30
    secondaryFixed = Color(0xFFD0E7E5), // secondary 90
    secondaryFixedDim = Color(0xFFA1CFCB), // secondary 80
    onSecondaryFixed = Color(0xFF00201E), // secondary 10
    onSecondaryFixedVariant = Color(0xFF00504C), // secondary 30
    tertiaryFixed = Color(0xFFCEE6F8), // tertiary 90
    tertiaryFixedDim = Color(0xFF99CDF0), // tertiary 80
    onTertiaryFixed = Color(0xFF001E2E), // tertiary 10
    onTertiaryFixedVariant = Color(0xFF004C6A), // tertiary 30
)
