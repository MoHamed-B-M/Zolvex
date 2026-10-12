package com.gov.employeedb.domain.usecase

import com.gov.employeedb.domain.repository.EmployeeRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Reactive row count (used by the list header and the "empty vs full" dashboard states). */
class CountEmployeesUseCase @Inject constructor(
    private val repository: EmployeeRepository,
) {
    operator fun invoke(): Flow<Int> = repository.observeCount()
}