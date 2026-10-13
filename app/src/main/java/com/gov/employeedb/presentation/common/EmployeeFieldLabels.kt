package com.gov.employeedb.presentation.common

import androidx.annotation.StringRes
import com.gov.employeedb.R
import com.gov.employeedb.domain.model.EmployeeField

/**
 * Employee field → localized label, shared by the column-mapping dropdown and the preview table
 * header (PLAN.md Phase-7 checklist).
 */
@StringRes
fun EmployeeField.labelRes(): Int = when (this) {
    EmployeeField.FULL_NAME -> R.string.full_name
    EmployeeField.DATE_OF_BIRTH -> R.string.date_of_birth
    EmployeeField.HIRE_DATE -> R.string.hire_date
    EmployeeField.DEPARTMENT -> R.string.department
    EmployeeField.POSITION -> R.string.position
    EmployeeField.GRADE -> R.string.grade
    EmployeeField.SALARY -> R.string.salary
    EmployeeField.EMAIL -> R.string.email
    EmployeeField.PHONE -> R.string.phone
    EmployeeField.ADDRESS -> R.string.address
    EmployeeField.NOTES -> R.string.notes
}
