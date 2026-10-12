package com.gov.employeedb.data.mapper

import com.gov.employeedb.data.local.entity.EmployeeEntity
import com.gov.employeedb.data.local.projection.EmployeeListItemEntity
import com.gov.employeedb.domain.model.Employee
import com.gov.employeedb.domain.model.EmployeeSummary
import java.time.LocalDate
import java.time.Period
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Boundary conversions between Room rows and the domain model. */
object EmployeeMappers {

    fun toEntity(employee: Employee): EmployeeEntity = EmployeeEntity(
        id = employee.id,
        fullName = employee.fullName,
        fullNameKey = employee.fullName.normalizedKey(),
        dateOfBirth = employee.dateOfBirth,
        hireDate = employee.hireDate,
        department = employee.department,
        position = employee.position,
        grade = employee.grade,
        salary = employee.salary,
        email = employee.email,
        phone = employee.phone,
        address = employee.address,
        notes = employee.notes,
        importBatchId = employee.importBatchId,
        sourceRow = employee.sourceRow,
        createdAt = employee.createdAt,
        updatedAt = employee.updatedAt,
    )

    fun fromEntity(entity: EmployeeEntity): Employee = Employee(
        id = entity.id,
        fullName = entity.fullName,
        dateOfBirth = entity.dateOfBirth,
        hireDate = entity.hireDate,
        department = entity.department,
        position = entity.position,
        grade = entity.grade,
        salary = entity.salary,
        email = entity.email,
        phone = entity.phone,
        address = entity.address,
        notes = entity.notes,
        importBatchId = entity.importBatchId,
        sourceRow = entity.sourceRow,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
    )

    /** Display summary; [today] is explicit so the mapper stays deterministic in tests. */
    fun toSummary(item: EmployeeListItemEntity, today: LocalDate): EmployeeSummary = EmployeeSummary(
        id = item.id,
        fullName = item.fullName,
        department = item.department,
        position = item.position,
        age = item.dateOfBirth?.yearsUntil(today)?.takeIf { it >= 0 },
        serviceYears = item.hireDate?.serviceYearsUntil(today),
    )
}

/** Normalized dedupe key, trimmed and locale-stable lowercased (PLAN.md §5.6). */
internal fun String.normalizedKey(): String = trim().lowercase(Locale.ROOT)

private fun LocalDate.yearsUntil(today: LocalDate): Int = Period.between(this, today).years

private fun LocalDate.serviceYearsUntil(today: LocalDate): Int =
    (ChronoUnit.MONTHS.between(this, today).coerceAtLeast(0) / 12).toInt()