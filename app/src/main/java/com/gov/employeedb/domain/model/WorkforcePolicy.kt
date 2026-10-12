package com.gov.employeedb.domain.model

/**
 * The retirement thresholds shared by analytics and UI labels — a single source of truth so the
 * dashboard and the bucket legends can never disagree with [WorkforceAnalytics].
 */
object WorkforcePolicy {
    const val RETIREMENT_AGE = 60

    /** How many years before retirement an employee counts as "retirement soon" (age ≥ 55). */
    const val RETIREMENT_SOON_YEARS = 5
}