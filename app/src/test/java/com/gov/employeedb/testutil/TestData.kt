package com.gov.employeedb.testutil

import com.gov.employeedb.data.local.entity.EmployeeEntity
import com.gov.employeedb.data.local.projection.EmployeeStatRow
import java.time.LocalDate
import java.util.Locale

/**
 * Test fixtures for the Phase-4 JVM tests. [TODAY] is a fixed calendar anchor so every
 * date-derived assertion (bucket boundaries, medians, years of service) is deterministic.
 */
object TestData {

    val TODAY: LocalDate = LocalDate.of(2026, 10, 12)

    /** Full stat projection with only the fields relevant to analytics tests. */
    fun statRow(
        id: Long = 0L,
        fullName: String = "",
        position: String? = null,
        birthDate: LocalDate? = null,
        hireDate: LocalDate? = null,
        department: String? = null,
        grade: String? = null,
    ): EmployeeStatRow = EmployeeStatRow(id, fullName, position, birthDate, hireDate, department, grade)

    /**
     * Minimal `employees` row. [fullNameKey] is derived the same way [com.gov.employeedb.data.mapper.EmployeeMappers]
     * derives it (trim + `lowercase(Locale.ROOT)`) — the DAO test therefore documents that the
     * dedupe key is a repository concern, not a DAO concern.
     */
    fun entity(
        id: Long = 0L,
        fullName: String,
        birthDate: LocalDate? = null,
        hireDate: LocalDate? = null,
        department: String? = null,
        position: String? = null,
        grade: String? = null,
        createdAt: Long = 0L,
        updatedAt: Long = 0L,
    ): EmployeeEntity = EmployeeEntity(
        id = id,
        fullName = fullName,
        fullNameKey = fullName.trim().lowercase(Locale.ROOT),
        dateOfBirth = birthDate,
        hireDate = hireDate,
        department = department,
        position = position,
        grade = grade,
        salary = null,
        email = null,
        phone = null,
        address = null,
        notes = null,
        importBatchId = null,
        sourceRow = null,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
}