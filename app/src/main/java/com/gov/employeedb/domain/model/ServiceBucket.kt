package com.gov.employeedb.domain.model

/**
 * Half-open years-of-service bands (`[minInclusive, maxExclusive)`, PLAN.md §5.3) used by the
 * analytics engine and dashboard legend. Each boundary (4/5, 9/10, …) is asserted by
 * [com.gov.employeedb.domain.analytics.WorkforceAnalyticsTest].
 */
enum class ServiceBucket(val minInclusive: Int, val maxExclusive: Int) {
    LESS_5(0, 5),
    B5_10(5, 10),
    B10_15(10, 15),
    B15_20(15, 20),
    B20_PLUS(20, Int.MAX_VALUE),
    ;

    companion object {
        fun of(years: Int): ServiceBucket = entries.first { years in it.minInclusive until it.maxExclusive }
    }
}