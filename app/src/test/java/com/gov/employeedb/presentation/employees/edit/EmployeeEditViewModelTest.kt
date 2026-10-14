package com.gov.employeedb.presentation.employees.edit

import androidx.lifecycle.SavedStateHandle
import com.gov.employeedb.domain.model.Employee
import com.gov.employeedb.domain.model.EmployeeField
import com.gov.employeedb.domain.usecase.CreateEmployeeUseCase
import com.gov.employeedb.domain.usecase.GetEmployeeByIdUseCase
import com.gov.employeedb.domain.usecase.UpdateEmployeeUseCase
import com.gov.employeedb.presentation.navigation.NEW_EMPLOYEE_ID
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
 * The add/edit form's contract (PLAN.md Phase-8 checklist): the route decides the mode, the form is
 * live and re-validated on every keystroke, the first invalid Save reveals the reasons instead of
 * writing, and a successful write reports `Saved` exactly once.
 *
 * The repository is faked rather than mocked, so the assertions are about the [Employee] that was
 * actually persisted - including the values the form derives (trimmed text, parsed salary, blank
 * optionals collapsed to `null`).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EmployeeEditViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeEmployeeRepository()

    private fun newViewModel(id: Long = NEW_EMPLOYEE_ID): EmployeeEditViewModel =
        EmployeeEditViewModel(
            GetEmployeeByIdUseCase(repository),
            CreateEmployeeUseCase(repository),
            UpdateEmployeeUseCase(repository),
            SavedStateHandle(mapOf("id" to id)),
        )

    @Test
    fun `a create route starts from an empty form with the errors still hidden`() = runTest {
        val viewModel = newViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isEditing).isFalse()
        assertThat(state.isLoading).isFalse()
        assertThat(state.form).isEqualTo(EmployeeForm())
        assertThat(state.showErrors).isFalse()
        assertThat(state.isValid).isFalse()
        assertThat(state.canSave).isTrue()
        assertThat(state.errorFor(EmployeeField.FULL_NAME)).isNull()
    }

    @Test
    fun `a route without the id argument is a create form`() = runTest {
        val viewModel = EmployeeEditViewModel(
            GetEmployeeByIdUseCase(repository),
            CreateEmployeeUseCase(repository),
            UpdateEmployeeUseCase(repository),
            SavedStateHandle(),
        )
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isEditing).isFalse()
        assertThat(repository.created).isEmpty()
    }

    @Test
    fun `the edit route loads the row into the form`() = runTest {
        repository.rows.value = listOf(employee())
        val viewModel = newViewModel(id = 7L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.isEditing).isTrue()
        assertThat(state.isLoading).isFalse()
        assertThat(state.notFound).isFalse()
        assertThat(state.isValid).isTrue()
        assertThat(state.form.fullName).isEqualTo("Ada Lovelace")
        assertThat(state.form.dateOfBirth).isEqualTo(LocalDate.of(1985, 4, 12))
        assertThat(state.form.hireDate).isEqualTo(LocalDate.of(2015, 1, 2))
        assertThat(state.form.department).isEqualTo("Finance")
        assertThat(state.form.salary).isEqualTo("45000")
        assertThat(state.form.email).isEqualTo("ada@example.com")
    }

    @Test
    fun `an id that no longer exists is reported as not found`() = runTest {
        val viewModel = newViewModel(id = 404L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.notFound).isTrue()
        assertThat(state.isLoading).isFalse()
        assertThat(state.error).isFalse()
    }

    @Test
    fun `save on an invalid form reveals the reason and writes nothing`() = runTest {
        val viewModel = newViewModel()
        advanceUntilIdle()

        viewModel.onEvent(EmployeeEditEvent.Save)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.showErrors).isTrue()
        assertThat(state.errorFor(EmployeeField.FULL_NAME))
            .isEqualTo(EmployeeValidationError.NAME_REQUIRED)
        assertThat(state.isValid).isFalse()
        assertThat(state.canSave).isFalse()
        assertThat(repository.created).isEmpty()
        assertThat(state.isSaved).isFalse()
    }

    @Test
    fun `a valid create writes the form and reports success once`() = runTest {
        val viewModel = newViewModel()
        advanceUntilIdle()

        val effects = mutableListOf<EmployeeEditEffect>()
        val job = launch { viewModel.effects.collect { effects += it } }

        viewModel.onEvent(EmployeeEditEvent.FullNameChanged("  Ada Lovelace  "))
        viewModel.onEvent(EmployeeEditEvent.DepartmentChanged(" Finance "))
        viewModel.onEvent(EmployeeEditEvent.SalaryChanged("45000"))
        viewModel.onEvent(EmployeeEditEvent.EmailChanged("ada@example.com"))
        viewModel.onEvent(EmployeeEditEvent.Save)
        advanceUntilIdle()
        job.cancel()

        val written = repository.created.single()
        assertThat(written.id).isEqualTo(0L)
        assertThat(written.fullName).isEqualTo("Ada Lovelace")
        assertThat(written.department).isEqualTo("Finance")
        assertThat(written.salary).isEqualTo(45_000.0)
        assertThat(written.email).isEqualTo("ada@example.com")
        assertThat(written.notes).isNull()
        assertThat(effects).containsExactly(EmployeeEditEffect.Saved)
        assertThat(viewModel.uiState.value.isSaved).isTrue()
        assertThat(viewModel.uiState.value.isSaving).isFalse()
    }

    @Test
    fun `salary input keeps digits only and converts Arabic-Indic ones`() = runTest {
        val viewModel = newViewModel()
        advanceUntilIdle()

        viewModel.onEvent(EmployeeEditEvent.SalaryChanged("1\u0662,3 4"))
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.form.salary).isEqualTo("1234")
        assertThat(viewModel.uiState.value.form.salaryValue).isEqualTo(1234.0)
    }

    @Test
    fun `a failed write reports the generic failure and stays on the form`() = runTest {
        repository.writeFailure = IllegalStateException("disk full")
        val viewModel = newViewModel()
        advanceUntilIdle()
        viewModel.onEvent(EmployeeEditEvent.FullNameChanged("Ada Lovelace"))

        val effects = mutableListOf<EmployeeEditEffect>()
        val job = launch { viewModel.effects.collect { effects += it } }

        viewModel.onEvent(EmployeeEditEvent.Save)
        advanceUntilIdle()
        job.cancel()

        assertThat(effects).containsExactly(EmployeeEditEffect.SaveFailed)
        assertThat(viewModel.uiState.value.isSaving).isFalse()
        assertThat(viewModel.uiState.value.isSaved).isFalse()
    }

    @Test
    fun `saving an edit keeps the id and the import provenance`() = runTest {
        repository.rows.value = listOf(
            employee().copy(importBatchId = "batch-1", sourceRow = 42),
        )
        val viewModel = newViewModel(id = 7L)
        advanceUntilIdle()

        viewModel.onEvent(EmployeeEditEvent.GradeChanged("A"))
        viewModel.onEvent(EmployeeEditEvent.Save)
        advanceUntilIdle()

        val written = repository.updated.single()
        assertThat(written.id).isEqualTo(7L)
        assertThat(written.grade).isEqualTo("A")
        assertThat(written.importBatchId).isEqualTo("batch-1")
        assertThat(written.sourceRow).isEqualTo(42)
        assertThat(repository.created).isEmpty()
    }

    @Test
    fun `clearing an optional field writes a null instead of a blank`() = runTest {
        repository.rows.value = listOf(employee())
        val viewModel = newViewModel(id = 7L)
        advanceUntilIdle()

        viewModel.onEvent(EmployeeEditEvent.PhoneChanged("   "))
        viewModel.onEvent(EmployeeEditEvent.NotesChanged(""))
        viewModel.onEvent(EmployeeEditEvent.Save)
        advanceUntilIdle()

        val written = repository.updated.single()
        assertThat(written.phone).isNull()
        assertThat(written.notes).isNull()
    }

    @Test
    fun `a failed read sets the error flag and retry reloads the row`() = runTest {
        repository.readFailure = IllegalStateException("closed")
        repository.rows.value = listOf(employee())
        val viewModel = newViewModel(id = 7L)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.error).isTrue()
        assertThat(viewModel.uiState.value.isLoading).isFalse()

        repository.readFailure = null
        viewModel.onEvent(EmployeeEditEvent.Retry)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.error).isFalse()
        assertThat(state.notFound).isFalse()
        assertThat(state.form.fullName).isEqualTo("Ada Lovelace")
    }

    private companion object {
        /** A complete, valid row, so a test only has to state the one field it cares about. */
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
