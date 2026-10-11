package com.gov.employeedb.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import com.gov.employeedb.core.AppLanguage

/**
 * Arabic needs taller lines than Latin: Cairo's ascenders, descenders and the vowel marks used in
 * formal Arabic extend past the Latin metrics the Material 3 type scale is built on, and the UI
 * mixes Arabic with Latin digits and file names.
 */
private const val ARABIC_LINE_HEIGHT_SCALE = 1.15f

/**
 * No line-height trimming for Arabic. Material 3's default trim removes the leading above the
 * first line and below the last, which clips the marks that sit above the tallest ascender;
 * centring the extra leading keeps those marks inside the line box.
 */
private val ArabicLineHeightStyle = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

/** Inter on the stock Material 3 type scale — English and French. */
val latinTypography: Typography = Typography().withFamily(InterFamily)

/** Cairo with wider lines and untrimmed leading — Arabic. */
val arabicTypography: Typography = Typography().withFamily(
    family = CairoFamily,
    lineHeightScale = ARABIC_LINE_HEIGHT_SCALE,
    lineHeightStyle = ArabicLineHeightStyle,
)

/**
 * The type scale for a UI language.
 *
 * Script selection is driven by the language, not by the locale's layout direction, because
 * Arabic is the only RTL language the app ships and a future RTL language with a Latin script
 * would still need Inter.
 */
object AppTypography {
    fun forLanguage(language: AppLanguage): Typography =
        if (language == AppLanguage.AR) arabicTypography else latinTypography
}

/**
 * Returns a copy of this type scale rendered in [family], optionally scaling every line height by
 * [lineHeightScale] and overriding the [lineHeightStyle].
 *
 * All 29 styles are passed to the constructor explicitly: the type scale is read as a whole by
 * every screen, so a style silently left on the wrong font family is a visible bug, and this way
 * a future Material 3 style is a compile error here rather than a missing font at runtime.
 * (`Typography.copy` is unusable: the shipped artifact declares two `copy` overloads with
 * identical signatures, which is ambiguous at the call site.)
 */
private fun Typography.withFamily(
    family: FontFamily,
    lineHeightScale: Float = 1f,
    lineHeightStyle: LineHeightStyle? = null,
): Typography {
    fun style(base: TextStyle): TextStyle = base.copy(
        fontFamily = family,
        lineHeight = if (base.lineHeight == TextUnit.Unspecified) {
            base.lineHeight
        } else {
            base.lineHeight * lineHeightScale
        },
        lineHeightStyle = lineHeightStyle ?: base.lineHeightStyle,
    )

    return Typography(
        displayLarge = style(displayLarge),
        displayMedium = style(displayMedium),
        displaySmall = style(displaySmall),
        headlineLarge = style(headlineLarge),
        headlineMedium = style(headlineMedium),
        headlineSmall = style(headlineSmall),
        titleLarge = style(titleLarge),
        titleMedium = style(titleMedium),
        titleSmall = style(titleSmall),
        bodyLarge = style(bodyLarge),
        bodyMedium = style(bodyMedium),
        bodySmall = style(bodySmall),
        labelLarge = style(labelLarge),
        labelMedium = style(labelMedium),
        labelSmall = style(labelSmall),
        displayLargeEmphasized = style(displayLargeEmphasized),
        displayMediumEmphasized = style(displayMediumEmphasized),
        displaySmallEmphasized = style(displaySmallEmphasized),
        headlineLargeEmphasized = style(headlineLargeEmphasized),
        headlineMediumEmphasized = style(headlineMediumEmphasized),
        headlineSmallEmphasized = style(headlineSmallEmphasized),
        titleLargeEmphasized = style(titleLargeEmphasized),
        titleMediumEmphasized = style(titleMediumEmphasized),
        titleSmallEmphasized = style(titleSmallEmphasized),
        bodyLargeEmphasized = style(bodyLargeEmphasized),
        bodyMediumEmphasized = style(bodyMediumEmphasized),
        bodySmallEmphasized = style(bodySmallEmphasized),
        labelLargeEmphasized = style(labelLargeEmphasized),
        labelMediumEmphasized = style(labelMediumEmphasized),
        labelSmallEmphasized = style(labelSmallEmphasized),
    )
}
