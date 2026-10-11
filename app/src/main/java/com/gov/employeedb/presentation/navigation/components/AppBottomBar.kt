package com.gov.employeedb.presentation.navigation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.gov.employeedb.presentation.common.preview.AppPreview
import com.gov.employeedb.presentation.common.preview.PreviewVariant
import com.gov.employeedb.presentation.common.preview.PreviewVariants
import com.gov.employeedb.presentation.navigation.TopLevelDestination
import com.gov.employeedb.presentation.theme.AppMotion

/**
 * The Material 3 bottom bar with one tab per [com.gov.employeedb.presentation.navigation.TopLevelDestination].
 *
 * The bar slides out of the way on detail, edit and import routes and slides back in when a tab is
 * reached again. The slide is driven by the motion-scheme specs (so the system animation scale is
 * honoured); sliding in from below is direction-neutral, which is exactly what an RTL layout needs.
 *
 * [selected] is null while the bar is animating out after the user left the last tab — there is no
 * selection worth highlighting on the way out.
 */
@Composable
fun AppBottomBar(
    visible: Boolean,
    selected: TopLevelDestination?,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(animationSpec = AppMotion.spatial()) { it } +
            fadeIn(animationSpec = AppMotion.effects()),
        exit = slideOutVertically(animationSpec = AppMotion.spatial()) { it } +
            fadeOut(animationSpec = AppMotion.effects()),
    ) {
        NavigationBar {
            TopLevelDestination.entries.forEach { destination ->
                val isSelected = destination == selected
                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onSelect(destination) },
                    icon = {
                        Icon(
                            imageVector = if (isSelected) destination.selectedIcon else destination.icon,
                            // The item's label is announced by the item itself, so the icon stays
                            // decorative and a screen reader does not read it twice.
                            contentDescription = null,
                        )
                    },
                    label = { Text(text = stringResource(destination.labelRes)) },
                )
            }
        }
    }
}

@Preview(name = "AppBottomBar")
@Composable
private fun AppBottomBarPreview(@PreviewParameter(PreviewVariants::class) variant: PreviewVariant) {
    AppPreview(variant) {
        Surface(modifier = Modifier.fillMaxWidth()) {
            AppBottomBar(
                visible = true,
                selected = TopLevelDestination.DASHBOARD,
                onSelect = {},
            )
        }
    }
}