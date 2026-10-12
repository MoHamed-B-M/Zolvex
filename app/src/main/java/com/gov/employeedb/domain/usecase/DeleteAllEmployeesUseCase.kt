package com.gov.employeedb.domain.usecase

import com.gov.employeedb.domain.repository.EmployeeRepository
import javax.inject.Inject

/** Removes every row (double-confirmed "Delete all" in settings, PLAN.md phase table row 9). */
class DeleteAllEmployeesUseCase @Inject constructor(
    private val repository: EmployeeRepository,
) {
    suspend operator fun invoke() = repository.deleteAll()
}