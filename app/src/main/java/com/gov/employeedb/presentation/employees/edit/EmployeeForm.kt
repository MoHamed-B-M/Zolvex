package com.gov.employeedb.presentation.employees.edit

import androidx.compose.runtime.Immutable
import com.gov.employeedb.core.LocaleDigits
import com.gov.employeedb.domain.model.Employee
import java.time.LocalDate

/**
 * The eleven editable values of an employee (PLAN.md Phase-8 checklist), held as the text the user
 * typed so a half-entered value is never lost to a parse.
 *
 * Only the two dates are typed: they can be set through the Material 3 date picker alone, so they
 * never exist in an unparsable state. [salary] stays a string because the field filters everything
 * but digits - including converting Arabic-Indic digits through
 * [com.gov.employeedb.core.LocaleDigits] - and the form keeps exactly what is on screen until
 * [salaryValue] is asked for.
 */
@Immutable
data class EmployeeForm(
    val fullName: String = "",
    val dateOfBirth: LocalDate? = null,
    val hireDate: LocalDate? = null,
    val department: String = "",
    val position: String = "",
    val grade: String = "",
    val salary: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val notes: String = "",
) {

    /** [salary] as a number, or `null` while the field is empty or holds no parsable digits. */
    val salaryValue: Double?
        get() = LocaleDigits.toAsciiDigits(salary).trim()
            .takeIf { it.isNotEmpty() }
            ?.toDoubleOrNull()
}

/** Snapshots a stored row into the form, so an edit starts from exactly what was persisted. */
fun Employee.toForm(): EmployeeForm = EmployeeForm(
    fullName = fullName,
    dateOfBirth = dateOfBirth,
    hireDate = hireDate,
    department = department.orEmpty(),
    position = position.orEmpty(),
    grade = grade.orEmpty(),
    salary = salary?.toSalaryText().orEmpty(),
    email = email.orEmpty(),
    phone = phone.orEmpty(),
    address = address.orEmpty(),
    notes = notes.orEmpty(),
)

/**
 * Rebuilds the [Employee] to persist.
 *
 * Blank optional fields collapse to `null`, so emptying a field really clears its column, and the
 * import provenance ([Employee.importBatchId] / [Employee.sourceRow]) and id survive an edit of an
 * imported row. `createdAt`/`updatedAt` are left unset because the repository stamps them, which
 * keeps the clock out of the form.
 */
fun EmployeeForm.toEmployee(existing: Employee?): Employee = Employee(
    id = existing?.id ?: 0L,
    fullName = fullName.trim(),
    dateOfBirth = dateOfBirth,
    hireDate = hireDate,
    department = department.clean(),
    position = position.clean(),
    grade = grade.clean(),
    salary = salaryValue,
    email = email.clean(),
    phone = phone.clean(),
    address = address.clean(),
    notes = notes.clean(),
    importBatchId = existing?.importBatchId,
    sourceRow = existing?.sourceRow,
)

/**
 * The salary field's input contract: Arabic-Indic and Eastern Arabic-Indic digits become ASCII and
 * everything that is not a digit is dropped, so the field can only ever hold a non-negative number.
 */
internal fun sanitizeSalary(raw: String): String =
    LocaleDigits.toAsciiDigits(raw).filter { it in '0'..'9' }

private fun String.clean(): String? = trim().takeIf { it.isNotEmpty() }

private fun Double.toSalaryText(): String =
    if (this % 1.0 == 0.0) toLong().toString() else toString()
