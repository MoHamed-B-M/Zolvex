package com.gov.employeedb.domain.usecase

import com.gov.employeedb.domain.model.Employee
import com.gov.employeedb.domain.repository.EmployeeRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Live single-employee stream for the detail screen; also emits `null` while the row is absent. */
class GetEmployeeByIdUseCase @Inject constructor(
    private val repository: EmployeeRepository,
) {
    operator fun invoke(id: Long): Flow<Employee?> = repository.observeById(id)
}