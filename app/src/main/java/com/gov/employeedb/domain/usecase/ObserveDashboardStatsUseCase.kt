package com.gov.employeedb.domain.usecase

import com.gov.employeedb.domain.analytics.WorkforceAnalytics
import com.gov.employeedb.domain.model.DashboardStats
import com.gov.employeedb.domain.repository.EmployeeRepository
import com.gov.employeedb.di.DefaultDispatcher
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn

/**
 * Recomputes the dashboard whenever the data changes. The combine lambda runs on the default
 * dispatcher (PLAN.md phase table row 5) so analysis work never touches the main thread; results
 * are handed back to the collector wherever it lives.
 */
class ObserveDashboardStatsUseCase @Inject constructor(
    private val repository: EmployeeRepository,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
) {
    operator fun invoke(): Flow<DashboardStats> = combine(
        repository.observeStatRows(),
        repository.observeRecent(RECENT_COUNT),
    ) { rows, recent ->
        DashboardStats(
            snapshot = WorkforceAnalytics.compute(rows, LocalDate.now()),
            recent = recent,
        )
    }.flowOn(dispatcher)

    private companion object {
        const val RECENT_COUNT = 5
    }
}