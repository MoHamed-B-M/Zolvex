package com.gov.employeedb.presentation.common.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.gov.employeedb.R
import com.gov.employeedb.presentation.common.preview.AppPreview
import com.gov.employeedb.presentation.common.preview.PreviewVariant
import com.gov.employeedb.presentation.common.preview.PreviewVariants

/**
 * The frame every full screen is built on: an [AppTopBar] and the content below it.
 *
 * Inset contract (see [AppNavigation]):
 *  - the **top** inset is consumed by the top bar, so the content is never double-padded below the
 *    status bar;
 *  - the **horizontal** insets are kept (gesture handles and display cutouts in landscape);
 *  - the **bottom** inset is deliberately excluded: the app shell owns it, so the screen never
 *    needs to know whether a bottom bar is visible or animating. When there is no bottom bar the
 *    shell pads by the navigation bar itself, and when the bar is showing it pads by the bar.
 *
 * [actions] is passed straight through to [AppTopBar], so a screen can offer top-bar controls (the
 * dashboard's debug seed action, say) without owning a `Scaffold` itself.
 */
@Composable
fun ScreenScaffold(
    @StringRes titleRes: Int,
    modifier: Modifier = Modifier,
    onNavigateUp: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = { AppTopBar(titleRes = titleRes, onNavigateUp = onNavigateUp, actions = actions) },
        contentWindowInsets = WindowInsets.systemBars.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
        ),
        content = content,
    )
}

@Preview(name = "ScreenScaffold")
@Composable
private fun ScreenScaffoldPreview(@PreviewParameter(PreviewVariants::class) variant: PreviewVariant) {
    AppPreview(variant) {
        ScreenScaffold(titleRes = R.string.employees) { padding ->
            // An empty screen body is exactly what a routed-but-not-built destination renders;
            // this preview just exercises the frame itself.
        }
    }
}