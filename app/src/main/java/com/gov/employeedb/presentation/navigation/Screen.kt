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
 * destination, told apart by its id: [NEW_EMPLOYEE_ID] means "new employee". The argument is
 * required rather than optional, so the encoded route always carries one and no destination can be
 * reached with it silently missing.
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

    /** The employee form; [id] is [NEW_EMPLOYEE_ID] when creating a new employee. */
    @Serializable
    data class EmployeeEdit(val id: Long) : Screen
}

/**
 * The `EmployeeEdit` argument that asks for a new employee rather than an existing row.
 *
 * A sentinel instead of a nullable argument: the route always carries an id, so the form reads it
 * from the `SavedStateHandle` with no fallback that could quietly turn a missing argument into
 * "create a new employee". Room row ids start at 1, so `-1` is free.
 */
const val NEW_EMPLOYEE_ID: Long = -1L
