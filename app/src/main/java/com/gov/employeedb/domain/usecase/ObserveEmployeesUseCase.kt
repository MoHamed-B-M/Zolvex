package com.gov.employeedb.domain.usecase

import com.gov.employeedb.domain.model.EmployeeSummary
import com.gov.employeedb.domain.repository.EmployeeRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Windowed, reactive employee list with the search/filter/sort parameters of PLAN.md §5.6. Bucket
 * filters arrive as epochDay ranges so filtering always happens in SQL, never in Kotlin.
 */
class ObserveEmployeesUseCase @Inject constructor(
    private val repository: EmployeeRepository,
) {
    operator fun invoke(
        query: String = "",
        department: String = "",
        birthFrom: Long? = null,
        birthTo: Long? = null,
        hireFrom: Long? = null,
        hireTo: Long? = null,
        sortKey: String = "NAME",
        ascending: Boolean = true,
        limit: Int = PAGE_SIZE,
        offset: Int = 0,
    ): Flow<List<EmployeeSummary>> = repository.observeEmployees(
        query = query,
        department = department,
        birthFrom = birthFrom,
        birthTo = birthTo,
        hireFrom = hireFrom,
        hireTo = hireTo,
        sortKey = sortKey,
        ascending = ascending,
        limit = limit,
        offset = offset,
    )

    private companion object {
        const val PAGE_SIZE = 100
    }
}