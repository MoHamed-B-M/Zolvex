package com.gov.employeedb.domain.model

/**
 * The 11 editable columns of an employee record (PLAN.md §5.2), in Excel/detail order.
 *
 * Used by the import pipeline (header-synonym matching, missing-core validation, mapping UI) and the
 * Phase-8 edit form; [isCore] marks the columns an import cannot proceed without.
 */
enum class EmployeeField {
    FULL_NAME, DATE_OF_BIRTH, HIRE_DATE, DEPARTMENT, POSITION,
    GRADE, SALARY, EMAIL, PHONE, ADDRESS, NOTES;

    val isCore: Boolean get() = this in REQUIRED_CORE

    companion object {
        /** Fields without which a row cannot be imported or saved. */
        val REQUIRED_CORE: Set<EmployeeField> = setOf(FULL_NAME, DATE_OF_BIRTH, HIRE_DATE)
    }
}