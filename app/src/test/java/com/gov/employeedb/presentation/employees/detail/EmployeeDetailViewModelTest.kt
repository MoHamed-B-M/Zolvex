package com.gov.employeedb.presentation.employees.detail

import androidx.lifecycle.SavedStateHandle
import com.gov.employeedb.domain.model.Employee
import com.gov.employeedb.domain.usecase.DeleteEmployeeUseCase
import com.gov.employeedb.domain.usecase.GetEmployeeByIdUseCase
import com.gov.employeedb.testutil.FakeEmployeeRepository
import com.gov.employeedb.testutil.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/**
 * The read-only employee screen's contract (PLAN.md Phase-8 checklist): the row is *observed*, so a
 * later write to it reaches the state without a reload, and a delete reports `Deleted` exactly once
 * so the screen leaves for the list.
 *
 * The id is injected exactly the way the type-safe route injects it - as the `id` entry of the
 * [SavedStateHandle] - so a missing argument can never silently load employee `0`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EmployeeDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeEmployeeRepository()

    private fun newViewModel(id: Long = 7L): EmployeeDetailViewModel = EmployeeDetailViewModel(
        GetEmployeeByIdUseCase(repository),
        DeleteEmployeeUseCase(repository),
        SavedStateHandle(mapOf("id" to id)),
    )

    @Test
    fun `the observed row reaches the state and stops loading`() = runTest {
        repository.rows.value = listOf(employee())
        val viewModel = newViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.employee?.fullName).isEqualTo("Ada Lovelace")
        assertThat(state.isLoading).isFalse()
        assertThat(state.error).isFalse()
    }

    @Test
    fun `a later write to the row reaches the state without a reload`() = runTest {
        repository.rows.value = listOf(employee())
        val viewModel = newViewModel()
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.employee?.department).isEqualTo("Finance")

        repository.rows.value = listOf(employee().copy(department = "Ops", grade = "A"))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.employee?.department).isEqualTo("Ops")
        assertThat(state.employee?.grade).isEqualTo("A")
    }

    @Test
    fun `an unknown id finishes loading with no employee`() = runTest {
        val viewModel = newViewModel(id = 404L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.employee).isNull()
        assertThat(state.isLoading).isFalse()
        assertThat(state.error).isFalse()
    }

    @Test
    fun `delete removes the row and reports the one-shot effect`() = runTest {
        repository.rows.value = listOf(employee())
        val viewModel = newViewModel()
        advanceUntilIdle()

        val effects = mutableListOf<EmployeeDetailEffect>()
        val job = launch { viewModel.effects.collect { effects += it } }

        viewModel.onEvent(EmployeeDetailEvent.Delete)
        advanceUntilIdle()
        job.cancel()

        assertThat(repository.deleted).containsExactly(7L)
        assertThat(effects).containsExactly(EmployeeDetailEffect.Deleted)
    }

    @Test
    fun `a failed delete sets the error flag and navigates nowhere`() = runTest {
        repository.writeFailure = IllegalStateException("locked")
        repository.rows.value = listOf(employee())
        val viewModel = newViewModel()
        advanceUntilIdle()

        val effects = mutableListOf<EmployeeDetailEffect>()
        val job = launch { viewModel.effects.collect { effects += it } }

        viewModel.onEvent(EmployeeDetailEvent.Delete)
        advanceUntilIdle()
        job.cancel()

        assertThat(effects).isEmpty()
        assertThat(viewModel.uiState.value.error).isTrue()
    }

    @Test
    fun `a failed read sets the error flag and retry recovers`() = runTest {
        repository.readFailure = IllegalStateException("closed")
        val viewModel = newViewModel()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.error).isTrue()
        assertThat(viewModel.uiState.value.isLoading).isFalse()

        repository.readFailure = null
        repository.rows.value = listOf(employee())
        viewModel.onEvent(EmployeeDetailEvent.Retry)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.error).isFalse()
        assertThat(state.employee?.fullName).isEqualTo("Ada Lovelace")
    }

    private companion object {
        /** A complete row, so a test only has to state the one field it cares about. */
        fun employee(): Employee = Employee(
            id = 7L,
            fullName = "Ada Lovelace",
            dateOfBirth = LocalDate.of(1985, 4, 12),
            hireDate = LocalDate.of(2015, 1, 2),
            department = "Finance",
            position = "Analyst",
            grade = "B2",
            salary = 45_000.0,
            email = "ada@example.com",
            phone = "+212600000000",
            address = "12 Rue Example",
            notes = "Imported",
        )
    }
}
