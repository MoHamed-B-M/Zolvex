package com.gov.employeedb.domain.model

/**
 * The immutable result of [com.gov.employeedb.domain.analytics.WorkforceAnalytics.compute],
 * consumed by the dashboard and both analytics tabs (PLAN.md §5.4). Null averages/medians mean no
 * row had a birth/hire date; the buckets then simply don't cover those rows.
 */
data class AnalyticsSnapshot(
    val total: Int,
    val averageAge: Double?,
    val medianAge: Double?,
    val averageService: Double?,
    val medianService: Double?,
    val retirementSoon: Int,
    val ageBuckets: List<BucketCount>,
    val serviceBuckets: List<BucketCount>,
    val departmentCounts: List<BucketCount>,
    val gradeCounts: List<BucketCount>,
    val longestServing: List<EmployeeSummary>,
)