package com.gov.employeedb.domain.model

/**
 * One slice of a distribution (PLAN.md §5.4). [label] holds the **raw** value — an enum name for
 * `AgeBucket`/`ServiceBucket`, the department/grade string otherwise — and presentation maps it to a
 * localized label. [share] is `count / total` over *all* rows (`0f..1f`), so rows with null dates are
 * honestly missing from the visible slices instead of inflating them.
 */
data class BucketCount(
    val label: String,
    val count: Int,
    val share: Float,
)