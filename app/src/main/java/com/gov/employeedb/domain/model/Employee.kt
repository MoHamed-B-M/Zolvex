package com.gov.employeedb.domain.model

import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit

/**
 * The canonical employee record that crosses every layer (PLAN.md §5.1).
 *
 * [dateOfBirth] and [hireDate] are `java.time.LocalDate` (minSdk 26, no desugaring — ADR-11). Every
 * derivable value takes [today] explicitly so the domain never depends on a clock; tests and UI feed
 * a fixed date instead of calling `LocalDate.now()` internally.
 */
data class Employee(
    val id: Long = 0L,
    val fullName: String,
    val dateOfBirth: LocalDate? = null,
    val hireDate: LocalDate? = null,
    val department: String? = null,
    val position: String? = null,
    val grade: String? = null,
    val salary: Double? = null,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val notes: String? = null,
    val importBatchId: String? = null,
    val sourceRow: Int? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
) {
    /** Whole years between birth and [today]; `null` when [dateOfBirth] is unknown. */
    fun ageAt(today: LocalDate): Int? = dateOfBirth?.let { Period.between(it, today).years }

    /** Whole years of service as `floor(months / 12)`, clamped to zero (never negative). */
    fun serviceYearsAt(today: LocalDate): Int? =
        hireDate?.let { (ChronoUnit.MONTHS.between(it, today).coerceAtLeast(0) / 12).toInt() }

    val age: Int? get() = ageAt(LocalDate.now())
    val yearsOfService: Int? get() = serviceYearsAt(LocalDate.now())

    /** True once the employee is within [WorkforcePolicy.RETIREMENT_SOON_YEARS] of retirement. */
    val isRetirementSoon: Boolean
        get() = (age ?: return false) >=
            WorkforcePolicy.RETIREMENT_AGE - WorkforcePolicy.RETIREMENT_SOON_YEARS
}