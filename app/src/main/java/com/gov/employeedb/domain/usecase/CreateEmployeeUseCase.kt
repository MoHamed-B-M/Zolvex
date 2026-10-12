package com.gov.employeedb.domain.usecase

import com.gov.employeedb.domain.model.Employee
import com.gov.employeedb.domain.repository.EmployeeRepository
import javax.inject.Inject

/** Persists a new employee (Phase-8 add form and Phase-5 import path share this). */
class CreateEmployeeUseCase @Inject constructor(
    private val repository: EmployeeRepository,
) {
    /** Inserts [employee] and returns its generated row id. */
    suspend operator fun invoke(employee: Employee): Long = repository.create(employee)
}