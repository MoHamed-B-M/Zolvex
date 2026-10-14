package com.gov.employeedb.presentation.dashboard

import com.gov.employeedb.data.local.projection.EmployeeStatRow
import com.gov.employeedb.data.local.projection.ImportKeyRow
import com.gov.employeedb.domain.model.AgeBucket
import com.gov.employeedb.domain.model.Employee
import com.gov.employeedb.domain.model.EmployeeSummary
import com.gov.employeedb.domain.model.ServiceBucket
import com.gov.employeedb.domain.repository.EmployeeRepository
import com.gov.employeedb.domain.usecase.ObserveDashboardStatsUseCase
import com.gov.employeedb.domain.usecase.SeedDebugDataUseCase
import com.gov.employeedb.testutil.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/**
 * The dashboard ViewModel's contract (PLAN.md Phase-9 checklist): the first snapshot fills every
 * statistic, rows without dates still count toward the total, a refresh reports itself, a failing
 * read sets the error flag, and the debug seed runs once per tap.
 *
 * The repository is faked rather than mocked so the assertions are about the state the screen ends
 * up with, including the averages and bucket counts the analytics engine derives.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeStatRepository()

    private fun TestScope.newViewModel(): DashboardViewModel = DashboardViewModel(
        ObserveDashboardStatsUseCase(repository, StandardTestDispatcher(testScheduler)),
        SeedDebugDataUseCase(repository, StandardTestDispatcher(testScheduler)),
    )

    @Test
    fun `the first emission fills every statistic`() = runTest {
        val today = LocalDate.now()
        repository.statRows.value = listOf(
            statRow(id = 1L, name = "Young Analyst", age = 30, serviceYears = 4, today = today),
            statRow(id = 2L, name = "Senior Officer", age = 62, serviceYears = 20, today = today),
        )
        repository.recent.value = listOf(
            EmployeeSummary(2L, "Senior Officer", "Ops", "Officer", 62, 20),
        )

        val viewModel = newViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.error).isFalse()
        assertThat(state.total).isEqualTo(2)
        assertThat(state.averageAge).isEqualTo(46.0)
        assertThat(state.averageService).isEqualTo(12.0)
        assertThat(state.retirementSoon).isEqualTo(1)
        assertThat(state.ageBuckets).hasSize(AgeBucket.entries.size)
        assertThat(state.serviceBuckets).hasSize(ServiceBucket.entries.size)
        assertThat(state.recent.map { it.id }).containsExactly(2L)
    }

    @Test
    fun `rows without dates still count toward the total`() = runTest {
        repository.statRows.value = listOf(
            statRow(id = 1L, name = "No Dates", age = null, serviceYears = null, today = LocalDate.now()),
        )

        val viewModel = newViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.total).isEqualTo(1)
        assertThat(state.averageAge).isNull()
        assertThat(state.averageService).isNull()
        assertThat(state.retirementSoon).isEqualTo(0)
        assertThat(state.ageBuckets.map { it.count }).containsExactly(0, 0, 0, 0, 0)
    }

    @Test
    fun `a refresh reports itself and picks up new rows`() = runTest {
        repository.statRows.value = emptyList()
        val viewModel = newViewModel()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.total).isEqualTo(0)

        val today = LocalDate.now()
        repository.statRows.value = listOf(
            statRow(id = 1L, name = "A", age = 30, serviceYears = 4, today = today),
            statRow(id = 2L, name = "B", age = 40, serviceYears = 8, today = today),
        )

        viewModel.onEvent(DashboardEvent.Refresh)
        assertThat(viewModel.uiState.value.isRefreshing).isTrue()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isRefreshing).isFalse()
        assertThat(viewModel.uiState.value.total).isEqualTo(2)
    }

    @Test
    fun `a failing read sets the error flag and a later refresh clears it`() = runTest {
        repository.failure = IllegalStateException("stat read failed")
        val viewModel = newViewModel()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.error).isTrue()

        repository.failure = null
        viewModel.onEvent(DashboardEvent.Refresh)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.error).isFalse()
    }

    @Test
    fun `the debug seed runs once per tap and clears its flag`() = runTest {
        val viewModel = newViewModel()
        advanceUntilIdle()

        viewModel.onEvent(DashboardEvent.SeedDebugData)
        assertThat(viewModel.uiState.value.isSeeding).isTrue()
        viewModel.onEvent(DashboardEvent.SeedDebugData)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isSeeding).isFalse()
        assertThat(repository.batches).isEqualTo(SEED_BATCHES)
    }

    private companion object {
        /** 10 000 seeded rows written in 500-row chunks, exactly as `SeedDebugDataUseCase` does. */
        const val SEED_BATCHES = 20

        fun statRow(
            id: Long,
            name: String,
            age: Int?,
            serviceYears: Int?,
            today: LocalDate,
        ): EmployeeStatRow = EmployeeStatRow(
            id = id,
            fullName = name,
            position = "Officer",
            birthDate = age?.let { today.minusYears(it.toLong()) },
            hireDate = serviceYears?.let { today.minusYears(it.toLong()) },
            department = "Ops",
            grade = "A",
        )
    }

    /** Streams the test's stat rows and recents, with an injectable failure on the stat read. */
    private class FakeStatRepository : EmployeeRepository {

        val statRows = MutableStateFlow<List<EmployeeStatRow>>(emptyList())
        val recent = MutableStateFlow<List<EmployeeSummary>>(emptyList())
        var failure: Throwable? = null
        var batches = 0
            private set

        override fun observeEmployees(
            query: String,
            department: String,
            birthFrom: Long?,
            birthTo: Long?,
            hireFrom: Long?,
            hireTo: Long?,
            sortKey: String,
            ascending: Boolean,
            limit: Int,
            offset: Int,
        ): Flow<List<EmployeeSummary>> = flowOf(emptyList())

        override fun observeCount(): Flow<Int> = flowOf(0)

        override fun observeStatRows(): Flow<List<EmployeeStatRow>> {
            failure?.let { throw it }
            return statRows
        }

        override fun observeRecent(limit: Int): Flow<List<EmployeeSummary>> = recent

        override fun observeById(id: Long): Flow<Employee?> = flowOf(null)

        override suspend fun create(employee: Employee): Long = 0L

        override suspend fun existingImportKeys(): List<ImportKeyRow> = emptyList()

        override suspend fun insertImportBatch(employees: List<Employee>): List<Long> {
            batches += 1
            return emptyList()
        }

        override suspend fun getAllForExport(): List<Employee> = emptyList()

        override suspend fun update(employee: Employee) = Unit

        override suspend fun delete(id: Long) = Unit

        override suspend fun deleteAll() = Unit
    }
}
