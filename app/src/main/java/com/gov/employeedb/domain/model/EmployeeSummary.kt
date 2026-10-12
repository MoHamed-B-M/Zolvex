package com.gov.employeedb.domain.model

/**
 * A compact, display-ready employee row used by the list, the dashboard recents and the
 * "longest serving" highlight (PLAN.md §5.4). [age]/[serviceYears] are snapshots taken at a fixed
 * `today` by the caller, so the model stays clock-free.
 */
data class EmployeeSummary(
    val id: Long,
    val fullName: String,
    val department: String?,
    val position: String?,
    val age: Int?,
    val serviceYears: Int?,
)