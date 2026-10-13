package com.gov.employeedb.presentation.employees.list

import androidx.compose.runtime.Immutable
import com.gov.employeedb.domain.model.EmployeeSummary

/**
 * Everything the employee list renders from (PLAN.md Phase-6 checklist).
 *
 * [isLoading] covers the first page (full-height shimmer); [isLoadingMore] covers an appended page
 * (a footer spinner) so the list never blanks out while paging. [canLoadMore] is true while the
 * last window came back full, which is the only moment "load more" can still find rows.
 */
@Immutable
data class EmployeeListUiState(
    val items: List<EmployeeSummary> = emptyList(),
    val total: Int = 0,
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val query: String = "",
    val filters: EmployeeFilters = EmployeeFilters(),
    val sort: EmployeeSort = EmployeeSort(),
    val canLoadMore: Boolean = false,
    val error: Boolean = false,
    val isSeeding: Boolean = false,
) {

    /** True when a search term or a chip is narrowing the result, which changes the empty copy. */
    val isFiltering: Boolean
        get() = query.isNotBlank() || filters.isActive

    /** True once loading finished with nothing to show. */
    val isEmpty: Boolean
        get() = items.isEmpty() && !isLoading && !error
}
