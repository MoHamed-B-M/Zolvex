package com.gov.employeedb.presentation.navigation

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.gov.employeedb.presentation.analytics.AnalyticsScreen
import com.gov.employeedb.presentation.dashboard.DashboardScreen
import com.gov.employeedb.presentation.employees.detail.EmployeeDetailScreen
import com.gov.employeedb.presentation.employees.edit.EmployeeEditScreen
import com.gov.employeedb.presentation.employees.list.EmployeeListScreen
import com.gov.employeedb.presentation.importx.ImportScreen
import com.gov.employeedb.presentation.navigation.components.AppBottomBar
import com.gov.employeedb.presentation.settings.SettingsScreen

/** The base path of the employee-detail deep link, used to reach it via `adb` (PLAN §4, Phase 3). */
private const val EMPLOYEE_DETAIL_DEEP_LINK = "app://employee/detail"

/**
 * The navigation shell: a Scaffold that owns the bottom bar and the window's bottom inset, a
 * SharedTransitionLayout for the shared-element transitions later phases attach to the employee
 * cards, and the type-safe [NavHost] connecting every [Screen].
 *
 * Inset contract (see [com.gov.employeedb.presentation.common.components.ScreenScaffold]):
 *  - the shell consumes the bottom system bar: while a top-level tab is showing, the bottom bar
 *    covers it, and on detail/edit/import routes — where the bar is hidden — the same padding
 *    comes from `contentWindowInsets`, so no screen ever touches the bottom inset;
 *  - the top and horizontal insets belong to each screen's own Scaffold.
 *
 * `Modifier.padding(innerPadding)` drives the NavHost's height from the Scaffold padding, so the
 * animated show/hide of the bottom bar visibly makes room for the content instead of covering it.
 */
@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentTopLevel = backStackEntry?.destination.toTopLevelDestination()
    val navTransitions = rememberNavTransitions()

    Scaffold(
        modifier = modifier,
        bottomBar = {
            AppBottomBar(
                visible = currentTopLevel != null,
                selected = currentTopLevel,
                onSelect = navController::navigateToTopLevel,
            )
        },
        contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Bottom),
    ) { innerPadding ->
        SharedTransitionLayout(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Dashboard,
                enterTransition = navTransitions.enter,
                exitTransition = navTransitions.exit,
                popEnterTransition = navTransitions.popEnter,
                popExitTransition = navTransitions.popExit,
            ) {
                composable<Screen.Dashboard> { DashboardScreen() }

                composable<Screen.EmployeeList> {
                    EmployeeListScreen(
                        onEmployeeClick = { id ->
                            navController.navigate(Screen.EmployeeDetail(id))
                        },
                        onAddEmployee = {
                            navController.navigate(Screen.EmployeeEdit())
                        },
                        onImportExcel = {
                            navController.navigate(Screen.Import)
                        },
                    )
                }

                composable<Screen.Analytics> { AnalyticsScreen() }

                composable<Screen.Settings> { SettingsScreen() }

                composable<Screen.Import> {
                    ImportScreen(onNavigateUp = { navController.popBackStack() })
                }

                composable<Screen.EmployeeDetail>(
                    deepLinks = listOf(
                        navDeepLink<Screen.EmployeeDetail>(basePath = EMPLOYEE_DETAIL_DEEP_LINK),
                    ),
                ) {
                    EmployeeDetailScreen(onNavigateUp = { navController.popBackStack() })
                }

                composable<Screen.EmployeeEdit> {
                    EmployeeEditScreen(onNavigateUp = { navController.popBackStack() })
                }
            }
        }
    }
}

/**
 * The tab this destination belongs to, or `null` when it is not a top-level route.
 *
 * Checked against the whole hierarchy so the Bar shows whenever *some* ancestor is a tab — which
 * also keeps the bar in place if a future top-level destination ever gains nested routes.
 */
private fun NavDestination?.toTopLevelDestination(): TopLevelDestination? =
    if (this == null) {
        null
    } else {
        TopLevelDestination.entries.firstOrNull { topLevel ->
            hierarchy.any { it.hasRoute(topLevel.route::class) }
        }
    }

/** Switches (or returns to) a tab, preserving the state of the tab being left. */
private fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        // Pop to the graph root but keep every tab's own back stack; tapping the active tab
        // returns to its root instead of stacking duplicates.
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}