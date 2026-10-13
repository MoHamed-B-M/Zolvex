package com.gov.employeedb.presentation.employees.list

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.gov.employeedb.R

/**
 * The four sortable columns (PLAN.md Phase-6 checklist). [key] is the exact token the DAO's
 * `ORDER BY` switches on, so the UI can never ask for a column the SQL does not know.
 */
enum class SortField(val key: String, @StringRes val labelRes: Int) {
    NAME("NAME", R.string.sort_name),
    AGE("AGE", R.string.sort_age),
    HIRE_DATE("HIRE_DATE", R.string.sort_hire_date),
    DEPARTMENT("DEPARTMENT", R.string.sort_department),
}

/** Ascending or descending — split from the field so the menu can offer both independently. */
enum class SortDirection(@StringRes val labelRes: Int) {
    ASCENDING(R.string.sort_ascending),
    DESCENDING(R.string.sort_descending),
}

/** A field plus a direction (default: name, A→Z). */
@Immutable
data class EmployeeSort(
    val field: SortField = SortField.NAME,
    val direction: SortDirection = SortDirection.ASCENDING,
) {
    val ascending: Boolean
        get() = direction == SortDirection.ASCENDING
}
