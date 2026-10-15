package com.gov.employeedb.presentation.theme

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.gov.employeedb.core.AppThemeMode
import com.gov.employeedb.core.withLocale
import com.gov.employeedb.presentation.main.AppUiState

/**
 * The app theme: colour, type, shape, motion, plus the two composition locals that localize the tree.
 *
 * `LocalContext` is replaced with a locale-aware context so every `stringResource` resolves in the
 * painted language (see `withLocale`), and `LocalLayoutDirection` is set from that language itself.
 * The Activity declares `configChanges="locale|layoutDirection"`, so switching language re-renders
 * the tree instead of recreating the Activity.
 *
 * A theme or language change cross-fades the shell (PLAN.md §8 rows 23-24): the shell dips to
 * transparent, the new palette and locale are applied, then it fades back in. The painted language
 * and theme are held in local state for that one frame, so the fade never needs two copies of the
 * content. That matters because the content is the navigation shell: wrapping it in `AnimatedContent`
 * would compose a second `NavHost` during the transition and lose the back stack.
 */
@Composable
fun AppTheme(
    uiState: AppUiState,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    var language by remember { mutableStateOf(uiState.language) }
    var themeMode by remember { mutableStateOf(uiState.themeMode) }
    val shellAlpha = remember { Animatable(1f) }
    val fadeSpec = AppMotion.fastEffects<Float>()

    LaunchedEffect(uiState.language, uiState.themeMode) {
        if (uiState.language == language && uiState.themeMode == themeMode) return@LaunchedEffect
        shellAlpha.animateTo(targetValue = 0f, animationSpec = fadeSpec)
        language = uiState.language
        themeMode = uiState.themeMode
        shellAlpha.animateTo(targetValue = 1f, animationSpec = fadeSpec)
    }

    val localizedContext = remember(context, language) { context.withLocale(language) }
    val configuration = localizedContext.resources.configuration

    val darkTheme = when (themeMode) {
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
            // Expressive is the app's motion language: springs with a slight overshoot for anything
            // spatial, short curves for colour and alpha. Passed explicitly because the default
            // motion scheme in Material 3 is the non-expressive one.
            motionScheme = MotionScheme.expressive(),
            shapes = AppShapes,
            typography = AppTypography.forLanguage(language),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = shellAlpha.value },
            ) {
                content()
            }
        }
    }
}
