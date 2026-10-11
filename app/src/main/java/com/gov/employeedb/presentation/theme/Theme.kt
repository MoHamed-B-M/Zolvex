package com.gov.employeedb.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.gov.employeedb.core.AppThemeMode
import com.gov.employeedb.core.withLocale
import com.gov.employeedb.presentation.main.AppUiState

/**
 * The app theme: colour, type, shape, motion, plus the two composition locals that make a
 * language switch instant.
 *
 * `LocalContext` is replaced with a locale-aware context so every `stringResource` resolves in
 * [AppUiState.language] (see `withLocale`), and `LocalLayoutDirection` is set from the language
 * itself. The Activity declares `configChanges="locale|layoutDirection"`, so switching language
 * re-renders the tree instead of recreating the Activity.
 */
@Composable
fun AppTheme(
    uiState: AppUiState,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val language = uiState.language
    val localizedContext = remember(context, language) { context.withLocale(language) }
    val configuration = localizedContext.resources.configuration

    val darkTheme = when (uiState.themeMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        // Wallpaper-derived colour on Android 12+, so the app sits naturally in a themed device.
        // Below that, and whenever the user has not themed the device, the Zolvex palettes apply.
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme ->
            dynamicDarkColorScheme(localizedContext)

        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            dynamicLightColorScheme(localizedContext)

        darkTheme -> ZolvexDarkColors

        else -> ZolvexLightColors
    }

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides configuration,
        LocalLayoutDirection provides
            if (language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            // Expressive is the app's motion language: springs with a slight overshoot for
            // anything spatial, short curves for colour and alpha. Passed explicitly because the
            // default motion scheme in Material 3 is the non-expressive one.
            motionScheme = MotionScheme.expressive(),
            shapes = AppShapes,
            typography = AppTypography.forLanguage(language),
            content = content,
        )
    }
}
