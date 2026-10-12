package com.gov.employeedb.domain.analytics

import com.gov.employeedb.data.local.projection.EmployeeStatRow
import com.gov.employeedb.testutil.TestData
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test

/**
 * Contract of PLAN.md §9.1 for [WorkforceAnalytics]: every bucket boundary, the null-date rules,
 * medians for even/odd counts, the retirement-soon threshold and the department rollup. Pure JVM —
 * no Robolectric needed, and [TestData.TODAY] keeps every date arithmetic deterministic.
 */
class WorkforceAnalyticsTest {

    private val today: LocalDate = TestData.TODAY

    private fun rowAtAge(age: Int): EmployeeStatRow = TestData.statRow(
        id = age.toLong(),
        fullName = "Age$age",
        birthDate = today.minusYears(age.toLong()),
    )

    @Test
    fun ageBuckets_assignOnHalfOpenIntervals() {
        val snapshot = WorkforceAnalytics.compute(
            listOf(24, 25, 34, 35, 44, 45, 54, 55).map(::rowAtAge),
            today,
        )
        val counts = snapshot.ageBuckets.associate { it.label to it.count }

        assertThat(counts).containsExactly(
            "UNDER_25", 1,
            "B25_34", 2,
            "B35_44", 2,
            "B45_54", 2,
            "B55_PLUS", 1,
        )
    }

    @Test
    fun serviceBuckets_assignOnHalfOpenIntervals() {
        val years = listOf(4, 5, 9, 10, 14, 15, 19, 20)
        val snapshot = WorkforceAnalytics.compute(
            years.map { y ->
                TestData.statRow(id = y.toLong(), fullName = "S$y", hireDate = today.minusYears(y.toLong()))
            },
            today,
        )
        val counts = snapshot.serviceBuckets.associate { it.label to it.count }

        assertThat(counts).containsExactly(
            "LESS_5", 1,
            "B5_10", 2,
            "B10_15", 2,
            "B15_20", 2,
            "B20_PLUS", 1,
        )
    }

    @Test
    fun nullDates_countInTotal_butSkipAveragesAndBuckets() {
        val snapshot = WorkforceAnalytics.compute(
            listOf(
                rowAtAge(20),
                rowAtAge(30),
                TestData.statRow(id = 3L, fullName = "NoDates"),
            ),
            today,
        )

        assertThat(snapshot.total).isEqualTo(3)
        assertThat(snapshot.averageAge).isEqualTo(25.0)
        assertThat(snapshot.medianAge).isEqualTo(25.0)
        assertThat(snapshot.averageService).isNull()
        assertThat(snapshot.medianService).isNull()
        assertThat(snapshot.ageBuckets.sumOf { it.count }).isEqualTo(2)
        assertThat(snapshot.serviceBuckets.sumOf { it.count }).isEqualTo(0)
    }

    @Test
    fun median_handlesEvenAndOddCounts() {
        val odd = WorkforceAnalytics.compute(listOf(20, 30, 40).map(::rowAtAge), today)
        assertThat(odd.medianAge).isEqualTo(30.0)
        assertThat(odd.averageAge).isEqualTo(30.0)

        val even = WorkforceAnalytics.compute(listOf(20, 30, 40, 50).map(::rowAtAge), today)
        assertThat(even.medianAge).isEqualTo(35.0)
        assertThat(even.averageAge).isEqualTo(35.0)
    }

    @Test
    fun retirementSoon_countsAnyoneAged55OrOlder() {
        val snapshot = WorkforceAnalytics.compute(
            listOf(54, 55, 59, 60).map(::rowAtAge),
            today,
        )

        assertThat(snapshot.retirementSoon).isEqualTo(3)
    }

    @Test
    fun departmentCounts_rollUpPastTenGroupsIntoOther() {
        val departments = (1..11).map { "Dept%02d".format(it) }
        val snapshot = WorkforceAnalytics.compute(
            departments.map { dept ->
                TestData.statRow(id = dept.hashCode().toLong(), fullName = dept, department = dept)
            },
            today,
        )

        assertThat(snapshot.departmentCounts).hasSize(11)
        assertThat(snapshot.departmentCounts.last().label).isEqualTo("Other")
        assertThat(snapshot.departmentCounts.last().count).isEqualTo(1)
        assertThat(snapshot.departmentCounts.sumOf { it.count }).isEqualTo(11)
    }

    @Test
    fun gradeCounts_sortByCountDescending_thenLabelAscending() {
        val snapshot = WorkforceAnalytics.compute(
            listOf("A2", "A2", "A1", "B1").mapIndexed { i, grade ->
                TestData.statRow(id = i.toLong(), fullName = "G$i", grade = grade)
            },
            today,
        )

        assertThat(snapshot.gradeCounts.map { it.label }).containsExactly("A2", "A1", "B1").inOrder()
        assertThat(snapshot.gradeCounts.first().count).isEqualTo(2)
    }

    @Test
    fun longestServing_returnsTopFiveByYearsOfService() {
        val snapshot = WorkforceAnalytics.compute(
            listOf(1990, 1991, 1992, 1993, 1994, 1995).map { year ->
                TestData.statRow(
                    id = year.toLong(),
                    fullName = "Worker$year",
                    hireDate = LocalDate.of(year, 1, 1),
                )
            },
            today,
        )

        assertThat(snapshot.longestServing).hasSize(5)
        assertThat(snapshot.longestServing.map { it.id })
            .containsExactly(1990L, 1991L, 1992L, 1993L, 1994L)
            .inOrder()
        assertThat(snapshot.longestServing.map { it.serviceYears })
            .containsExactly(36, 35, 34, 33, 32)
            .inOrder()
    }

    @Test
    fun emptyRows_produceAZeroedSnapshot() {
        val snapshot = WorkforceAnalytics.compute(emptyList(), today)

        assertThat(snapshot.total).isEqualTo(0)
        assertThat(snapshot.averageAge).isNull()
        assertThat(snapshot.medianAge).isNull()
        assertThat(snapshot.averageService).isNull()
        assertThat(snapshot.retirementSoon).isEqualTo(0)
        assertThat(snapshot.ageBuckets.map { it.count }).containsExactly(0, 0, 0, 0, 0)
        assertThat(snapshot.serviceBuckets.map { it.count }).containsExactly(0, 0, 0, 0, 0)
        assertThat(snapshot.departmentCounts).isEmpty()
        assertThat(snapshot.gradeCounts).isEmpty()
        assertThat(snapshot.longestServing).isEmpty()
    }

    @Test
    fun compute_handles10kRowsUnder50ms() {
        val rows = (0 until 10_000).map { i ->
            TestData.statRow(
                id = i.toLong(),
                fullName = "Employee$i",
                position = "Position",
                birthDate = today.minusYears((20 + (i % 40)).toLong()),
                hireDate = today.minusYears((1 + (i % 35)).toLong()),
                department = "Dept${i % 12}",
                grade = "A${i % 5}",
            )
        }

        // Warm the JIT once so the measured call reflects steady-state cost, not class-loading.
        WorkforceAnalytics.compute(rows, today)

        val start = System.nanoTime()
        val snapshot = WorkforceAnalytics.compute(rows, today)
        val elapsedMs = (System.nanoTime() - start) / 1_000_000

        assertThat(snapshot.total).isEqualTo(10_000)
        assertThat(snapshot.departmentCounts).hasSize(12)
        assertThat(elapsedMs).isLessThan(50L)
    }
}