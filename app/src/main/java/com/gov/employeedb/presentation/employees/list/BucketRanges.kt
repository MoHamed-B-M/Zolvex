package com.gov.employeedb.presentation.employees.list

import com.gov.employeedb.domain.model.AgeBucket
import com.gov.employeedb.domain.model.ServiceBucket
import java.time.LocalDate

/**
 * Translates bucket selections into the epochDay windows the DAO filters on (PLAN.md §5.6 note:
 * "bucket filters are translated to `birthFrom/birthTo/hireFrom/hireTo` epochDay ranges in the
 * ViewModel").
 *
 * The bounds are inclusive-from / exclusive-to, matching `birth_date >= :from AND birth_date <
 * :to`. A person born exactly `maxExclusive` years ago already has that age, so that boundary day
 * belongs to the next bucket; likewise a hire exactly `minInclusive` years ago already counts as
 * that many years of service.
 */
internal object BucketRanges {

    /** `[from, to)` epochDays on `birth_date`, or `(null, null)` when no age band is selected. */
    fun birthRange(buckets: Set<AgeBucket>, today: LocalDate): Pair<Long?, Long?> {
        if (buckets.isEmpty()) return null to null
        val minAge = buckets.minOf { it.minInclusive }
        val maxAge = buckets.maxOf { it.maxExclusive }
        val from = if (maxAge == Int.MAX_VALUE) null else shifted(today.minusYears(maxAge.toLong()))
        return from to shifted(today.minusYears(minAge.toLong()))
    }

    /** `[from, to)` epochDays on `hire_date`, or `(null, null)` when no service band is selected. */
    fun hireRange(buckets: Set<ServiceBucket>, today: LocalDate): Pair<Long?, Long?> {
        if (buckets.isEmpty()) return null to null
        val minYears = buckets.minOf { it.minInclusive }
        val maxYears = buckets.maxOf { it.maxExclusive }
        val from = if (maxYears == Int.MAX_VALUE) null else shifted(today.minusMonths(maxYears.toLong() * MONTHS))
        return from to shifted(today.minusMonths(minYears.toLong() * MONTHS))
    }

    /** Turns an inclusive calendar boundary (e.g. "born on or before `day`") into the DAO's
     *  exclusive epochDay bound (`day` is included, so the bound is `day + 1`). */
    private fun shifted(day: LocalDate): Long = day.toEpochDay() + 1

    private const val MONTHS = 12L
}
