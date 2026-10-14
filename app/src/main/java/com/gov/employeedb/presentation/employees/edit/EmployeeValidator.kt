package com.gov.employeedb.presentation.employees.edit

import androidx.annotation.StringRes
import com.gov.employeedb.R
import com.gov.employeedb.domain.model.EmployeeField
import java.time.LocalDate

/** One rejected field and the localized reason to show beneath it (PLAN.md Phase-8 checklist). */
enum class EmployeeValidationError(@StringRes val messageRes: Int) {
    NAME_REQUIRED(R.string.validation_name_required),
    DATE_OF_BIRTH_INVALID(R.string.validation_dob_invalid),
    HIRE_DATE_IN_FUTURE(R.string.validation_hire_future),
    HIRE_DATE_NOT_AFTER_BIRTH(R.string.validation_hire_after_dob),
    SALARY_NEGATIVE(R.string.validation_salary_negative),
    EMAIL_INVALID(R.string.validation_email_invalid),
}

/**
 * The employee form's rules (PLAN.md Phase-8 checklist), pure and clock-injected: [today] is a
 * parameter so every boundary is testable and the form never reads the system clock itself.
 *
 * A missing `dateOfBirth`/`hireDate` is not an error - the columns are optional (PLAN.md 5.2) - but
 * an unset birth date cannot make a hire date "too early", so that rule simply does not apply.
 */
object EmployeeValidator {

    /** Birth dates before this year are treated as typos rather than as a very old employee. */
    private const val EARLIEST_BIRTH_YEAR = 1900

    /** Deliberately light: one `@`, a non-empty local part, and a dotted domain. */
    private val EMAIL = Regex("^[^@\\s]+@[^@\\s.]+(\\.[^@\\s.]+)+$")

    fun validate(form: EmployeeForm, today: LocalDate): Map<EmployeeField, EmployeeValidationError> =
        buildMap {
            if (form.fullName.isBlank()) {
                put(EmployeeField.FULL_NAME, EmployeeValidationError.NAME_REQUIRED)
            }

            val birth = form.dateOfBirth
            if (birth != null && (birth.isAfter(today) || birth.year < EARLIEST_BIRTH_YEAR)) {
                put(EmployeeField.DATE_OF_BIRTH, EmployeeValidationError.DATE_OF_BIRTH_INVALID)
            }

            val hire = form.hireDate
            if (hire != null) {
                when {
                    hire.isAfter(today) ->
                        put(EmployeeField.HIRE_DATE, EmployeeValidationError.HIRE_DATE_IN_FUTURE)

                    birth != null && !hire.isAfter(birth) ->
                        put(
                            EmployeeField.HIRE_DATE,
                            EmployeeValidationError.HIRE_DATE_NOT_AFTER_BIRTH,
                        )
                }
            }

            val salary = form.salaryValue
            if (salary != null && salary < 0.0) {
                put(EmployeeField.SALARY, EmployeeValidationError.SALARY_NEGATIVE)
            }

            val email = form.email.trim()
            if (email.isNotEmpty() && !EMAIL.matches(email)) {
                put(EmployeeField.EMAIL, EmployeeValidationError.EMAIL_INVALID)
            }
        }
}
