package com.gov.employeedb.domain.analytics

import com.gov.employeedb.data.local.projection.EmployeeStatRow
import com.gov.employeedb.domain.model.AgeBucket
import com.gov.employeedb.domain.model.AnalyticsSnapshot
import com.gov.employeedb.domain.model.BucketCount
import com.gov.employeedb.domain.model.EmployeeSummary
import com.gov.employeedb.domain.model.ServiceBucket
import com.gov.employeedb.domain.model.WorkforcePolicy
import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Pure workforce analytics over the stat projection (PLAN.md §5.4): no Android, no I/O, no clock.
 * Tests and the dashboard both pass an explicit [today], so every bucket boundary, median and
 * average is deterministic. Null birth/hire dates count toward [AnalyticsSnapshot.total] but are
 * excluded from averages, medians, buckets and the top-five list.
 */
object WorkforceAnalytics {

    fun compute(rows: List<EmployeeStatRow>, today: LocalDate): AnalyticsSnapshot {
        val total = rows.size
        val ages = rows.mapNotNull { it.ageAt(today) }
        val services = rows.mapNotNull { it.serviceYearsAt(today) }
        return AnalyticsSnapshot(
            total = total,
            averageAge = average(ages),
            medianAge = median(ages.sorted()),
            youngestAge = ages.minOrNull(),
            oldestAge = ages.maxOrNull(),
            averageService = average(services),
            medianService = median(services.sorted()),
            retirementSoon = ages.count {
                it >= WorkforcePolicy.RETIREMENT_AGE - WorkforcePolicy.RETIREMENT_SOON_YEARS
            },
            ageBuckets = ageBucketCounts(ages, total),
            serviceBuckets = serviceBucketCounts(services, total),
            departmentCounts = labelCounts(rows.asSequence().map { it.department ?: "" }, total),
            gradeCounts = labelCounts(rows.asSequence().map { it.grade ?: "" }, total),
            longestServing = longestServing(rows, today),
        )
    }

    private fun average(values: List<Int>): Double? =
        if (values.isEmpty()) null else values.sum().toDouble() / values.size

    private fun median(sorted: List<Int>): Double? {
        if (sorted.isEmpty()) return null
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) {
            sorted[mid].toDouble()
        } else {
            (sorted[mid - 1] + sorted[mid]) / 2.0
        }
    }

    private fun ageBucketCounts(ages: List<Int>, total: Int): List<BucketCount> =
        AgeBucket.entries.map { bucket ->
            val count = ages.count { AgeBucket.of(it) == bucket }
            BucketCount(bucket.name, count, share(count, total))
        }

    private fun serviceBucketCounts(services: List<Int>, total: Int): List<BucketCount> =
        ServiceBucket.entries.map { bucket ->
            val count = services.count { ServiceBucket.of(it) == bucket }
            BucketCount(bucket.name, count, share(count, total))
        }

    /**
     * Counts by label, sorted by count desc (tie-break label asc) so the chart order is stable. When
     * more than [maxLabels] groups exist the tail rolls up into a single [OTHER_LABEL] bucket.
     */
    private fun labelCounts(labels: Sequence<String>, total: Int, maxLabels: Int = 10): List<BucketCount> {
        val sorted = labels.groupingBy { it }.eachCount().entries.sortedWith(
            compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key },
        )
        return buildList {
            sorted.take(maxLabels).forEach { (label, count) -> add(BucketCount(label, count, share(count, total))) }
            val dropped = sorted.drop(maxLabels)
            if (dropped.isNotEmpty()) {
                val others = dropped.sumOf { it.value }
                add(BucketCount(OTHER_LABEL, others, share(others, total)))
            }
        }
    }

    /** Top five employees by years of service, tied deterministically by name then id. */
    private fun longestServing(rows: List<EmployeeStatRow>, today: LocalDate): List<EmployeeSummary> =
        rows.asSequence()
            .mapNotNull { row ->
                val hire = row.hireDate ?: return@mapNotNull null
                row to (ChronoUnit.MONTHS.between(hire, today).coerceAtLeast(0) / 12).toInt()
            }
            .sortedWith(
                compareByDescending<Pair<EmployeeStatRow, Int>> { it.second }
                    .thenBy { it.first.fullName.lowercase(Locale.ROOT) }
                    .thenBy { it.first.id },
            )
            .take(5)
            .map { (row, serviceYears) ->
                EmployeeSummary(
                    id = row.id,
                    fullName = row.fullName,
                    department = row.department,
                    position = row.position,
                    age = row.ageAt(today),
                    serviceYears = serviceYears,
                )
            }
            .toList()

    private fun share(count: Int, total: Int): Float =
        if (total == 0) 0f else count.toFloat() / total

    private fun EmployeeStatRow.ageAt(today: LocalDate): Int? =
        birthDate?.let { Period.between(it, today).years }?.takeIf { it >= 0 }

    private fun EmployeeStatRow.serviceYearsAt(today: LocalDate): Int? =
        hireDate?.let { (ChronoUnit.MONTHS.between(it, today).coerceAtLeast(0) / 12).toInt() }

    /**
     * The label of the synthetic rollup bucket [labelCounts] appends when a distribution has more
     * than ten groups. It is the only bucket label the engine invents instead of reading out of the
     * data, so presentation localizes it while leaving real department and grade names untouched.
     */
    const val OTHER_LABEL = "Other"
}