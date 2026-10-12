package com.gov.employeedb.domain.model

/**
 * Everything the dashboard renders in a single emission (PLAN.md §5.4): the full workforce analysis
 * plus the newest rows by `createdAt`.
 */
data class DashboardStats(
    val snapshot: AnalyticsSnapshot,
    val recent: List<EmployeeSummary>,
)