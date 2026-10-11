package com.gov.employeedb.presentation.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.gov.employeedb.R

/**
 * The four destinations that have a tab in the bottom bar.
 *
 * Membership of this enum is exactly what "top level" means: the shell shows the bottom bar when
 * the current destination is one of these and hides it everywhere else (detail, edit, import), so
 * a detail screen gets the whole height of the window.
 *
 * [icon] is the outlined variant shown while the tab is unselected, [selectedIcon] the filled
 * variant shown while it is selected — the Material 3 bottom-navigation convention.
 */
enum class TopLevelDestination(
    val route: Screen,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
) {
    DASHBOARD(
        route = Screen.Dashboard,
        labelRes = R.string.dashboard,
        icon = Icons.Outlined.Dashboard,
        selectedIcon = Icons.Filled.Dashboard,
    ),
    EMPLOYEES(
        route = Screen.EmployeeList,
        labelRes = R.string.employees,
        icon = Icons.Outlined.People,
        selectedIcon = Icons.Filled.People,
    ),
    ANALYTICS(
        route = Screen.Analytics,
        labelRes = R.string.analytics,
        icon = Icons.Outlined.Analytics,
        selectedIcon = Icons.Filled.Analytics,
    ),
    SETTINGS(
        route = Screen.Settings,
        labelRes = R.string.settings,
        icon = Icons.Outlined.Settings,
        selectedIcon = Icons.Filled.Settings,
    ),
}
