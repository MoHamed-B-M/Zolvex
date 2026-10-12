package com.gov.employeedb.domain.usecase

import com.gov.employeedb.domain.analytics.WorkforceAnalytics
import com.gov.employeedb.domain.model.AnalyticsSnapshot
import com.gov.employeedb.domain.repository.EmployeeRepository
import com.gov.employeedb.di.DefaultDispatcher
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/**
 * Workforce analytics for the whole dataset or a single department. Filtering happens in Kotlin here
 * (≤ 10k stat rows) and the pure computation runs on the default dispatcher.
 */
class ObserveAnalyticsUseCase @Inject constructor(
    private val repository: EmployeeRepository,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
) {
    operator fun invoke(selectedDepartment: String? = null): Flow<AnalyticsSnapshot> =
        repository.observeStatRows()
            .map { rows ->
                val filtered = if (selectedDepartment.isNullOrBlank()) {
                    rows
                } else {
                    rows.filter { it.department == selectedDepartment }
                }
                WorkforceAnalytics.compute(filtered, LocalDate.now())
            }
            .flowOn(dispatcher)
}