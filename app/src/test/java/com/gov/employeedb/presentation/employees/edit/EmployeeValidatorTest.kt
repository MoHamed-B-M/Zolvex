package com.gov.employeedb.presentation.employees.edit

import com.gov.employeedb.domain.model.EmployeeField
import com.gov.employeedb.testutil.TestData
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test

/**
 * The form's rules (PLAN.md Phase-8 checklist).
 *
 * Every rule is exercised against a fixed [TestData.TODAY] and a form built by hand, because both
 * the clock and the form state are parameters rather than ambient state: nothing here touches
 * `LocalDate.now()` or a `ViewModel`.
 */
class EmployeeValidatorTest {

    private val today: LocalDate = TestData.TODAY

    @Test
    fun `a non-blank name on its own is a valid form`() {
        val errors = EmployeeValidator.validate(EmployeeForm(fullName = "Ada Lovelace"), today)

        assertThat(errors).isEmpty()
    }

    @Test
    fun `a blank or whitespace-only name is required`() {
        val blank = EmployeeValidator.validate(EmployeeForm(fullName = ""), today)
        val spaces = EmployeeValidator.validate(EmployeeForm(fullName = "   "), today)

        assertThat(blank[EmployeeField.FULL_NAME])
            .isEqualTo(EmployeeValidationError.NAME_REQUIRED)
        assertThat(spaces[EmployeeField.FULL_NAME])
            .isEqualTo(EmployeeValidationError.NAME_REQUIRED)
    }

    @Test
    fun `a missing birth date is allowed`() {
        val errors = EmployeeValidator.validate(EmployeeForm(fullName = "Ada"), today)

        assertThat(errors).doesNotContainKey(EmployeeField.DATE_OF_BIRTH)
    }

    @Test
    fun `a birth date in the future is rejected`() {
        val errors = EmployeeValidator.validate(
            EmployeeForm(fullName = "Ada", dateOfBirth = today.plusDays(1)),
            today,
        )

        assertThat(errors[EmployeeField.DATE_OF_BIRTH])
            .isEqualTo(EmployeeValidationError.DATE_OF_BIRTH_INVALID)
    }

    @Test
    fun `a birth date before 1900 is rejected`() {
        val errors = EmployeeValidator.validate(
            EmployeeForm(fullName = "Ada", dateOfBirth = LocalDate.of(1899, 12, 31)),
            today,
        )

        assertThat(errors[EmployeeField.DATE_OF_BIRTH])
            .isEqualTo(EmployeeValidationError.DATE_OF_BIRTH_INVALID)
    }

    @Test
    fun `a birth date of today is allowed`() {
        val errors = EmployeeValidator.validate(
            EmployeeForm(fullName = "Ada", dateOfBirth = today),
            today,
        )

        assertThat(errors).doesNotContainKey(EmployeeField.DATE_OF_BIRTH)
    }

    @Test
    fun `a hire date in the future is rejected`() {
        val errors = EmployeeValidator.validate(
            EmployeeForm(fullName = "Ada", hireDate = today.plusDays(1)),
            today,
        )

        assertThat(errors[EmployeeField.HIRE_DATE])
            .isEqualTo(EmployeeValidationError.HIRE_DATE_IN_FUTURE)
    }

    @Test
    fun `a hire date before the birth date is rejected`() {
        val errors = EmployeeValidator.validate(
            EmployeeForm(
                fullName = "Ada",
                dateOfBirth = LocalDate.of(1990, 5, 4),
                hireDate = LocalDate.of(1990, 5, 3),
            ),
            today,
        )

        assertThat(errors[EmployeeField.HIRE_DATE])
            .isEqualTo(EmployeeValidationError.HIRE_DATE_NOT_AFTER_BIRTH)
    }

    @Test
    fun `a hire date equal to the birth date is rejected`() {
        val sameDay = LocalDate.of(1990, 5, 4)
        val errors = EmployeeValidator.validate(
            EmployeeForm(fullName = "Ada", dateOfBirth = sameDay, hireDate = sameDay),
            today,
        )

        assertThat(errors[EmployeeField.HIRE_DATE])
            .isEqualTo(EmployeeValidationError.HIRE_DATE_NOT_AFTER_BIRTH)
    }

    @Test
    fun `a hire date after the birth date is allowed`() {
        val errors = EmployeeValidator.validate(
            EmployeeForm(
                fullName = "Ada",
                dateOfBirth = LocalDate.of(1990, 5, 4),
                hireDate = LocalDate.of(2015, 1, 2),
            ),
            today,
        )

        assertThat(errors).isEmpty()
    }

    @Test
    fun `a future hire date wins over the birth-date rule`() {
        val errors = EmployeeValidator.validate(
            EmployeeForm(
                fullName = "Ada",
                dateOfBirth = today.plusDays(2),
                hireDate = today.plusDays(1),
            ),
            today,
        )

        assertThat(errors[EmployeeField.HIRE_DATE])
            .isEqualTo(EmployeeValidationError.HIRE_DATE_IN_FUTURE)
    }

    @Test
    fun `a negative salary is rejected even though the field filters a minus sign out`() {
        val errors = EmployeeValidator.validate(
            EmployeeForm(fullName = "Ada", salary = "-1"),
            today,
        )

        assertThat(errors[EmployeeField.SALARY])
            .isEqualTo(EmployeeValidationError.SALARY_NEGATIVE)
    }

    @Test
    fun `an empty or zero salary is allowed`() {
        val empty = EmployeeValidator.validate(EmployeeForm(fullName = "Ada"), today)
        val zero = EmployeeValidator.validate(EmployeeForm(fullName = "Ada", salary = "0"), today)

        assertThat(empty).doesNotContainKey(EmployeeField.SALARY)
        assertThat(zero).doesNotContainKey(EmployeeField.SALARY)
    }

    @Test
    fun `a salary typed in Arabic-Indic digits is read as a number`() {
        val errors = EmployeeValidator.validate(
            EmployeeForm(fullName = "Ada", salary = "\u0661\u0662\u0663"),
            today,
        )

        assertThat(EmployeeForm(salary = "\u0661\u0662\u0663").salaryValue).isEqualTo(123.0)
        assertThat(errors).doesNotContainKey(EmployeeField.SALARY)
    }

    @Test
    fun `a malformed email is rejected and a blank one is allowed`() {
        val malformed = EmployeeValidator.validate(
            EmployeeForm(fullName = "Ada", email = "ada@example"),
            today,
        )
        val blank = EmployeeValidator.validate(EmployeeForm(fullName = "Ada", email = "  "), today)

        assertThat(malformed[EmployeeField.EMAIL])
            .isEqualTo(EmployeeValidationError.EMAIL_INVALID)
        assertThat(blank).doesNotContainKey(EmployeeField.EMAIL)
    }

    @Test
    fun `a well-formed email is allowed`() {
        val errors = EmployeeValidator.validate(
            EmployeeForm(fullName = "Ada", email = "ada.lovelace@example.co.uk"),
            today,
        )

        assertThat(errors).isEmpty()
    }

    @Test
    fun `every broken field is reported at once`() {
        val errors = EmployeeValidator.validate(
            EmployeeForm(
                fullName = " ",
                dateOfBirth = today.plusDays(1),
                hireDate = today.plusDays(2),
                salary = "-5",
                email = "not-an-email",
            ),
            today,
        )

        assertThat(errors.keys).containsExactly(
            EmployeeField.FULL_NAME,
            EmployeeField.DATE_OF_BIRTH,
            EmployeeField.HIRE_DATE,
            EmployeeField.SALARY,
            EmployeeField.EMAIL,
        )
    }
}
