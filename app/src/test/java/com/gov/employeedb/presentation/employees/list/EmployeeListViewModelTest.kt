package com.gov.employeedb.presentation.employees.list

import com.gov.employeedb.data.local.projection.EmployeeStatRow
import com.gov.employeedb.data.local.projection.ImportKeyRow
import com.gov.employeedb.domain.model.AgeBucket
import com.gov.employeedb.domain.model.Employee
import com.gov.employeedb.domain.model.EmployeeSummary
import com.gov.employeedb.domain.repository.EmployeeRepository
import com.gov.employeedb.domain.usecase.CountEmployeesUseCase
import com.gov.employeedb.domain.usecase.DeleteEmployeeUseCase
import com.gov.employeedb.domain.usecase.GetEmployeeByIdUseCase
import com.gov.employeedb.domain.usecase.ObserveEmployeesUseCase
import com.gov.employeedb.domain.usecase.RestoreEmployeeUseCase
import com.gov.employeedb.domain.usecase.SeedDebugDataUseCase
import com.gov.employeedb.testutil.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/**
 * The list ViewModel's contract (PLAN.md Phase-6): the debounced query, the single-value filter
 * translation, the growing window, the delete/undo round-trip and the retry path.
 *
 * The repository is faked rather than mocked so the assertions are about the parameters the query
 * actually receives — `limit`, `offset`, `sortKey` — instead of about mock interactions.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EmployeeListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeRepository()

    private fun TestScope.newViewModel(): EmployeeListViewModel = EmployeeListViewModel(
        ObserveEmployeesUseCase(repository),
        CountEmployeesUseCase(repository),
        GetEmployeeByIdUseCase(repository),
        DeleteEmployeeUseCase(repository),
        RestoreEmployeeUseCase(repository),
        SeedDebugDataUseCase(repository, StandardTestDispatcher(testScheduler)),
    )

    @Test
    fun `the first window asks for one page and reports the total`() = runTest {
        repository.count.value = 250
        repository.employees.value = summaries(150)

        val viewModel = newViewModel()
        advanceUntilIdle()

        assertThat(repository.lastLimit).isEqualTo(PAGE_SIZE)
        assertThat(repository.lastOffset).isEqualTo(0)
        assertThat(viewModel.uiState.value.total).isEqualTo(250)
        assertThat(viewModel.uiState.value.isLoading).isFalse()
        assertThat(viewModel.uiState.value.canLoadMore).isTrue()
    }

    @Test
    fun `load more widens the window by exactly one page`() = runTest {
        repository.employees.value = summaries(150)

        val viewModel = newViewModel()
        advanceUntilIdle()

        viewModel.onEvent(EmployeeListEvent.LoadMore)
        advanceUntilIdle()

        assertThat(repository.lastLimit).isEqualTo(PAGE_SIZE * 2)
        assertThat(viewModel.uiState.value.canLoadMore).isFalse()
    }

    @Test
    fun `typing debounces and only the last term is queried`() = runTest {
        val viewModel = newViewModel()
        advanceUntilIdle()

        viewModel.onEvent(EmployeeListEvent.Search("ab"))
        viewModel.onEvent(EmployeeListEvent.Search("abc"))
        advanceUntilIdle()

        assertThat(repository.lastQuery).isEqualTo("abc")
        assertThat(viewModel.uiState.value.query).isEqualTo("abc")
    }

    @Test
    fun `a department filter reaches the query as a single value`() = runTest {
        val viewModel = newViewModel()
        advanceUntilIdle()

        viewModel.onEvent(EmployeeListEvent.ApplyFilter(EmployeeFilters(departments = setOf("Finance"))))
        advanceUntilIdle()

        assertThat(repository.lastDepartment).isEqualTo("Finance")
    }

    @Test
    fun `an age bucket becomes an inclusive-exclusive epochDay window`() = runTest {
        val viewModel = newViewModel()
        advanceUntilIdle()

        viewModel.onEvent(EmployeeListEvent.ApplyFilter(EmployeeFilters(ageBuckets = setOf(AgeBucket.B25_34))))
        advanceUntilIdle()

        val today = LocalDate.now()
        assertThat(repository.lastBirthFrom).isEqualTo(today.minusYears(35).toEpochDay() + 1)
        assertThat(repository.lastBirthTo).isEqualTo(today.minusYears(25).toEpochDay() + 1)
    }

    @Test
    fun `a sort selection reaches the query as a key and a direction`() = runTest {
        val viewModel = newViewModel()
        advanceUntilIdle()

        viewModel.onEvent(EmployeeListEvent.SetSort(EmployeeSort(SortField.HIRE_DATE, SortDirection.DESCENDING)))
        advanceUntilIdle()

        assertThat(repository.lastSortKey).isEqualTo("HIRE_DATE")
        assertThat(repository.lastAscending).isFalse()
    }

    @Test
    fun `a filter change resets the window to the first page`() = runTest {
        repository.employees.value = summaries(150)

        val viewModel = newViewModel()
        advanceUntilIdle()
        viewModel.onEvent(EmployeeListEvent.LoadMore)
        advanceUntilIdle()
        assertThat(repository.lastLimit).isEqualTo(PAGE_SIZE * 2)

        viewModel.onEvent(EmployeeListEvent.ApplyFilter(EmployeeFilters(ageBuckets = setOf(AgeBucket.UNDER_25))))
        advanceUntilIdle()

        assertThat(repository.lastLimit).isEqualTo(PAGE_SIZE)
    }

    @Test
    fun `delete then undo restores the removed employee`() = runTest {
        repository.rows += Employee(id = 7L, fullName = "Ada Lovelace")

        val viewModel = newViewModel()
        advanceUntilIdle()

        viewModel.onEvent(EmployeeListEvent.Delete(7L))
        advanceUntilIdle()
        assertThat(repository.deleted).containsExactly(7L)

        viewModel.onEvent(EmployeeListEvent.UndoDelete)
        advanceUntilIdle()

        assertThat(repository.created).hasSize(1)
        assertThat(repository.created.single().fullName).isEqualTo("Ada Lovelace")
        assertThat(repository.created.single().id).isEqualTo(0L)
    }

    @Test
    fun `a failed read sets the error flag and retry clears it`() = runTest {
        val viewModel = newViewModel()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.error).isFalse()

        repository.failure = IllegalStateException("query failed")
        viewModel.onEvent(EmployeeListEvent.Retry)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.error).isTrue()

        repository.failure = null
        viewModel.onEvent(EmployeeListEvent.Retry)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.error).isFalse()
    }

    @Test
    fun `the fab actions emit one-shot navigation effects in order`() = runTest {
        val viewModel = newViewModel()
        advanceUntilIdle()

        val effects = mutableListOf<EmployeeListEffect>()
        val job = launch { viewModel.effects.collect { effects += it } }
        viewModel.onEvent(EmployeeListEvent.AddClick)
        viewModel.onEvent(EmployeeListEvent.ImportClick)
        advanceUntilIdle()
        job.cancel()

        assertThat(effects)
            .containsExactly(EmployeeListEffect.NavigateToAdd, EmployeeListEffect.NavigateToImport)
            .inOrder()
    }

    private companion object {
        const val PAGE_SIZE = 100

        fun summaries(count: Int): List<EmployeeSummary> = List(count) { index ->
            EmployeeSummary(
                id = index.toLong(),
                fullName = "Person $index",
                department = "Ops",
                position = "Clerk",
                age = 30,
                serviceYears = 5,
            )
        }
    }

    /** Records every query parameter and streams the test's rows, including an injectable failure. */
    private class FakeRepository : EmployeeRepository {

        val employees = MutableStateFlow<List<EmployeeSummary>>(emptyList())
        val count = MutableStateFlow(0)
        val rows = mutableListOf<Employee>()
        val deleted = mutableListOf<Long>()
        val created = mutableListOf<Employee>()
        var failure: Throwable? = null

        var lastQuery = ""
            private set
        var lastDepartment = ""
            private set
        var lastBirthFrom: Long? = null
            private set
        var lastBirthTo: Long? = null
            private set
        var lastHireFrom: Long? = null
            private set
        var lastHireTo: Long? = null
            private set
        var lastSortKey = ""
            private set
        var lastAscending = true
            private set
        var lastLimit = -1
            private set
        var lastOffset = -1
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
        ): Flow<List<EmployeeSummary>> {
            lastQuery = query
            lastDepartment = department
            lastBirthFrom = birthFrom
            lastBirthTo = birthTo
            lastHireFrom = hireFrom
            lastHireTo = hireTo
            lastSortKey = sortKey
            lastAscending = ascending
            lastLimit = limit
            lastOffset = offset
            failure?.let { throw it }
            return employees
        }

        override fun observeCount(): Flow<Int> = count

        override fun observeStatRows(): Flow<List<EmployeeStatRow>> = flowOf(emptyList())

        override fun observeRecent(limit: Int): Flow<List<EmployeeSummary>> = flowOf(emptyList())

        override fun observeById(id: Long): Flow<Employee?> = flowOf(rows.firstOrNull { it.id == id })

        override suspend fun create(employee: Employee): Long {
            created += employee
            return created.size.toLong()
        }

        override suspend fun existingImportKeys(): List<ImportKeyRow> = emptyList()

        override suspend fun insertImportBatch(employees: List<Employee>): List<Long> = emptyList()

        override suspend fun getAllForExport(): List<Employee> = emptyList()

        override suspend fun update(employee: Employee) = Unit

        override suspend fun delete(id: Long) {
            deleted += id
        }

        override suspend fun deleteAll() = Unit
    }
}
