package com.gov.employeedb.domain.usecase

import com.gov.employeedb.domain.model.Employee
import com.gov.employeedb.domain.repository.EmployeeRepository
import javax.inject.Inject

/** Persists edits to an existing employee (Phase-8 edit form). */
class UpdateEmployeeUseCase @Inject constructor(
    private val repository: EmployeeRepository,
) {
    suspend operator fun invoke(employee: Employee) = repository.update(employee)
}