package com.gov.employeedb.presentation.common.preview

import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.LayoutDirection
import com.gov.employeedb.core.AppLanguage
import com.gov.employeedb.core.withLocale
import com.gov.employeedb.presentation.theme.AppShapes
import com.gov.employeedb.presentation.theme.AppTypography
import com.gov.employeedb.presentation.theme.ZolvexDarkColors
import com.gov.employeedb.presentation.theme.ZolvexLightColors

/**
 * One preview configuration: a theme and a UI language.
 *
 * The language decides the layout direction and the typeface, so covering English and Arabic
 * covers LTR and RTL — which is why the four combinations below (2 themes × 2 directions) are the
 * full matrix a component has to survive.
 */
@Immutable
data class PreviewVariant(
    val darkTheme: Boolean,
    val language: AppLanguage,
)

/** The four light/dark × LTR/RTL configurations every component preview is rendered in. */
class PreviewVariants : PreviewParameterProvider<PreviewVariant> {
    override val values: Sequence<PreviewVariant> = sequenceOf(
        PreviewVariant(darkTheme = false, language = AppLanguage.EN),
        PreviewVariant(darkTheme = true, language = AppLanguage.EN),
        PreviewVariant(darkTheme = false, language = AppLanguage.AR),
        PreviewVariant(darkTheme = true, language = AppLanguage.AR),
    )
}

/**
 * Renders [content] the way the app would, for one [variant].
 *
 * Previews go through the same theme as the running app, but with the Zolvex palettes instead of
 * the wallpaper-derived ones, so what a designer reviews is the brand palette. The context is
 * re-created for the variant's language, so an Arabic preview really shows Arabic strings rather
 * than English text inside an RTL layout.
 */
@Composable
fun AppPreview(variant: PreviewVariant, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val localizedContext = remember(context, variant.language) {
        context.withLocale(variant.language)
    }
    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedContext.resources.configuration,
        LocalLayoutDirection provides
            if (variant.language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
    ) {
        MaterialExpressiveTheme(
            colorScheme = if (variant.darkTheme) ZolvexDarkColors else ZolvexLightColors,
            motionScheme = MotionScheme.expressive(),
            shapes = AppShapes,
            typography = AppTypography.forLanguage(variant.language),
            content = content,
        )
    }
}
