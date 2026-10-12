package com.gov.employeedb.domain.model

/**
 * Half-open age bands (`[minInclusive, maxExclusive)`, PLAN.md §5.3) used by the analytics engine
 * and the dashboard bucket legend. Non-overlapping intervals guarantee a row is never counted twice;
 * each boundary (24/25, 34/35, …) is asserted by [com.gov.employeedb.domain.analytics.WorkforceAnalyticsTest].
 */
enum class AgeBucket(val minInclusive: Int, val maxExclusive: Int) {
    UNDER_25(0, 25),
    B25_34(25, 35),
    B35_44(35, 45),
    B45_54(45, 55),
    B55_PLUS(55, Int.MAX_VALUE),
    ;

    companion object {
        fun of(age: Int): AgeBucket = entries.first { age in it.minInclusive until it.maxExclusive }
    }
}