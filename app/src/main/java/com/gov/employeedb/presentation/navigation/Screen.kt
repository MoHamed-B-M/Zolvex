package com.gov.employeedb.presentation.navigation

import kotlinx.serialization.Serializable

/**
 * Every destination in the app, as a type instead of a string route.
 *
 * The type-safe navigation API (`composable<Screen.X>`) serialises these objects, so a route can
 * only be requested by constructing the destination the compiler knows about: an argument cannot
 * be forgotten, mis-typed or misspelled (see PLAN.md ADR-10).
 *
 * The two `EmployeeEdit` states — editing an existing row and creating a new one — are the same
 * destination: `id == null` means "new employee".
 */
@Serializable
sealed interface Screen {

    /** Overview: totals, averages and the two summary charts. */
    @Serializable
    data object Dashboard : Screen

    /** The searchable, sortable employee list. */
    @Serializable
    data object EmployeeList : Screen

    /** Age / service / department / grade analysis. */
    @Serializable
    data object Analytics : Screen

    /** Language, theme, backup note and About. */
    @Serializable
    data object Settings : Screen

    /** The Excel import wizard (pick file, map columns, preview, commit). */
    @Serializable
    data object Import : Screen

    /** One employee, read-only. */
    @Serializable
    data class EmployeeDetail(val id: Long) : Screen

    /** The employee form; [id] is `null` when creating a new employee. */
    @Serializable
    data class EmployeeEdit(val id: Long? = null) : Screen
}
