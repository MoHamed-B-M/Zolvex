package com.gov.employeedb.domain.usecase

import com.gov.employeedb.domain.model.Employee
import com.gov.employeedb.domain.repository.EmployeeRepository
import javax.inject.Inject

/**
 * Re-creates a physically deleted employee for the swipe-delete Undo snackbar (Phase 6). The old row
 * id is deliberately dropped so [EmployeeRepository.create] generates a fresh primary key.
 */
class RestoreEmployeeUseCase @Inject constructor(
    private val repository: EmployeeRepository,
) {
    suspend operator fun invoke(employee: Employee): Long = repository.create(employee.copy(id = 0L))
}