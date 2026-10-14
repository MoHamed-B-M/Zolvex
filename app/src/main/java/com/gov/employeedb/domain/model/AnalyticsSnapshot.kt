package com.gov.employeedb.domain.model

/**
 * The immutable result of [com.gov.employeedb.domain.analytics.WorkforceAnalytics.compute],
 * consumed by the dashboard and all four analytics tabs (PLAN.md §5.4). Null averages/medians mean
 * no row had a birth/hire date; the buckets then simply don't cover those rows.
 *
 * [youngestAge] and [oldestAge] are the ends of the same age population the average and the median
 * describe, so they are null exactly when those are.
 */
data class AnalyticsSnapshot(
    val total: Int,
    val averageAge: Double?,
    val medianAge: Double?,
    val youngestAge: Int?,
    val oldestAge: Int?,
    val averageService: Double?,
    val medianService: Double?,
    val retirementSoon: Int,
    val ageBuckets: List<BucketCount>,
    val serviceBuckets: List<BucketCount>,
    val departmentCounts: List<BucketCount>,
    val gradeCounts: List<BucketCount>,
    val longestServing: List<EmployeeSummary>,
)