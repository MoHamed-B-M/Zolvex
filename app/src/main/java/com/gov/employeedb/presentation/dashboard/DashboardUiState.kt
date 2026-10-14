package com.gov.employeedb.presentation.dashboard

import androidx.compose.runtime.Immutable
import com.gov.employeedb.domain.model.BucketCount
import com.gov.employeedb.domain.model.EmployeeSummary

/**
 * Everything the dashboard renders from (PLAN.md Phase-9 checklist).
 *
 * [isLoading] covers the first read only: once a snapshot has arrived, a pull to refresh keeps it on
 * screen and reports itself through [isRefreshing] instead, so refreshing never blanks a populated
 * dashboard. [isEmpty] is the state where the read succeeded but there is genuinely nothing to show,
 * which the content renders as the empty state rather than drawing empty charts.
 */
@Immutable
data class DashboardUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val total: Int = 0,
    val averageAge: Double? = null,
    val averageService: Double? = null,
    val retirementSoon: Int = 0,
    val ageBuckets: List<BucketCount> = emptyList(),
    val serviceBuckets: List<BucketCount> = emptyList(),
    val recent: List<EmployeeSummary> = emptyList(),
    val error: Boolean = false,
    val isSeeding: Boolean = false,
) {
    /** True when the read succeeded and there is genuinely nothing to display. */
    val isEmpty: Boolean get() = !isLoading && !error && total == 0
}
