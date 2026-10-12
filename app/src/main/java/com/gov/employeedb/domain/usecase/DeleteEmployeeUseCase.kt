package com.gov.employeedb.domain.usecase

import com.gov.employeedb.domain.repository.EmployeeRepository
import javax.inject.Inject

/** Physically removes one employee (detail-screen delete; the list keep an undo copy for the snackbar). */
class DeleteEmployeeUseCase @Inject constructor(
    private val repository: EmployeeRepository,
) {
    suspend operator fun invoke(id: Long) = repository.delete(id)
}