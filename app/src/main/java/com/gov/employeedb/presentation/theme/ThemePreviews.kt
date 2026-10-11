package com.gov.employeedb.presentation.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.gov.employeedb.R
import com.gov.employeedb.core.AppLanguage

/**
 * Design-system previews: the type scale, the colour roles and the language names in both layout
 * directions and both themes.
 *
 * The three language names are rendered through `AppLanguage.nativeNameRes`, so these previews
 * double as the design check for the Settings language picker.
 */
@Composable
private fun ThemeSpecimen(darkTheme: Boolean, language: AppLanguage) {
    val colorScheme = if (darkTheme) ZolvexDarkColors else ZolvexLightColors
    CompositionLocalProvider(
        LocalLayoutDirection provides
            if (language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = MotionScheme.expressive(),
            shapes = AppShapes,
            typography = AppTypography.forLanguage(language),
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = colorScheme.surface) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TypeSpecimen(R.string.dashboard, MaterialTheme.typography.displaySmall)
                    TypeSpecimen(
                        R.string.employees,
                        MaterialTheme.typography.headlineSmallEmphasized,
                    )
                    TypeSpecimen(R.string.employee_detail, MaterialTheme.typography.titleLarge)
                    TypeSpecimen(R.string.search_hint, MaterialTheme.typography.bodyLarge)
                    TypeSpecimen(R.string.total_employees, MaterialTheme.typography.labelLarge)
                    ColorRoles()
                    LanguageNames()
                }
            }
        }
    }
}

@Composable
private fun TypeSpecimen(textRes: Int, style: TextStyle) {
    Text(
        text = stringResource(textRes),
        style = style,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

/** One swatch per colour-role family, each with a contrasting label colour in the middle. */
@Composable
private fun ColorRoles() {
    val scheme = MaterialTheme.colorScheme
    val roles = listOf(
        scheme.primary to scheme.onPrimary,
        scheme.secondaryContainer to scheme.onSecondaryContainer,
        scheme.tertiaryContainer to scheme.onTertiaryContainer,
        scheme.errorContainer to scheme.onErrorContainer,
        scheme.surfaceContainerHighest to scheme.onSurfaceVariant,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        roles.forEach { (container, onContainer) ->
            ColorSwatch(container = container, onContainer = onContainer)
        }
    }
}

@Composable
private fun ColorSwatch(container: Color, onContainer: Color) {
    Box(
        modifier = Modifier
            .size(width = 56.dp, height = 40.dp)
            .background(container, RoundedCornerShape(12.dp)),
    ) {
        Box(
            modifier = Modifier
                .padding(start = 8.dp, top = 8.dp)
                .size(width = 24.dp, height = 8.dp)
                .background(onContainer, RoundedCornerShape(4.dp)),
        )
    }
}

@Composable
private fun LanguageNames() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AppLanguage.entries.forEach { language ->
            Text(
                text = stringResource(language.nativeNameRes),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Preview(name = "Theme — English, light", widthDp = 360, heightDp = 480, showBackground = true)
@Composable
private fun ThemeEnglishLightPreview() {
    ThemeSpecimen(darkTheme = false, language = AppLanguage.EN)
}

@Preview(name = "Theme — English, dark", widthDp = 360, heightDp = 480, showBackground = true)
@Composable
private fun ThemeEnglishDarkPreview() {
    ThemeSpecimen(darkTheme = true, language = AppLanguage.EN)
}

@Preview(name = "Theme — العربية, light (RTL)", widthDp = 360, heightDp = 480, showBackground = true)
@Composable
private fun ThemeArabicLightPreview() {
    ThemeSpecimen(darkTheme = false, language = AppLanguage.AR)
}

@Preview(name = "Theme — العربية, dark (RTL)", widthDp = 360, heightDp = 480, showBackground = true)
@Composable
private fun ThemeArabicDarkPreview() {
    ThemeSpecimen(darkTheme = true, language = AppLanguage.AR)
}
