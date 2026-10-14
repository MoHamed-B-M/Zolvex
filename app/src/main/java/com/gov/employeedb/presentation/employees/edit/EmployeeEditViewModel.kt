package com.gov.employeedb.presentation.employees.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gov.employeedb.domain.model.Employee
import com.gov.employeedb.domain.usecase.CreateEmployeeUseCase
import com.gov.employeedb.domain.usecase.GetEmployeeByIdUseCase
import com.gov.employeedb.domain.usecase.UpdateEmployeeUseCase
import com.gov.employeedb.presentation.navigation.NEW_EMPLOYEE_ID
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Everything the employee form can ask for (PLAN.md Phase-8 checklist). */
sealed interface EmployeeEditEvent {

    /** One of the eleven editable values changed; the id-free ones keep the form's own direction. */
    data class FullNameChanged(val value: String) : EmployeeEditEvent
    data class DateOfBirthChanged(val value: LocalDate) : EmployeeEditEvent
    data class HireDateChanged(val value: LocalDate) : EmployeeEditEvent
    data class DepartmentChanged(val value: String) : EmployeeEditEvent
    data class PositionChanged(val value: String) : EmployeeEditEvent
    data class GradeChanged(val value: String) : EmployeeEditEvent
    data class SalaryChanged(val value: String) : EmployeeEditEvent
    data class EmailChanged(val value: String) : EmployeeEditEvent
    data class PhoneChanged(val value: String) : EmployeeEditEvent
    data class AddressChanged(val value: String) : EmployeeEditEvent
    data class NotesChanged(val value: String) : EmployeeEditEvent

    /** The Save button: validates, then writes, then reports through [EmployeeEditEffect]. */
    data object Save : EmployeeEditEvent

    /** Reloads the row after a failed fetch. */
    data object Retry : EmployeeEditEvent
}

/** One-shot requests the form's screen consumes exactly once. */
sealed interface EmployeeEditEffect {

    /** The row was written; the screen shows the success mark and then leaves the form. */
    data object Saved : EmployeeEditEffect

    /** The write failed; the cause is not actionable, so the screen shows the generic message. */
    data object SaveFailed : EmployeeEditEffect
}

/**
 * The add/edit form's state holder (PLAN.md Phase-8 checklist).
 *
 * The route's id arrives through [SavedStateHandle] and decides the mode: [NEW_EMPLOYEE_ID] means
 * "create", anything else loads that row into the form. The form is the single source of truth while
 * the user types and [EmployeeValidator] re-runs on every change, so [EmployeeEditUiState.isValid]
 * is never stale; [EmployeeEditUiState.showErrors] is what keeps the messages out of the way until
 * the first Save, and [EmployeeEditUiState.canSave] is what disables the button afterwards.
 *
 * The loaded row is kept as [existing] rather than re-read at save time, so an edit preserves the
 * id and the import provenance ([Employee.importBatchId] / [Employee.sourceRow]) even though neither
 * is shown on the form.
 */
@HiltViewModel
class EmployeeEditViewModel @Inject constructor(
    private val getEmployee: GetEmployeeByIdUseCase,
    private val createEmployee: CreateEmployeeUseCase,
    private val updateEmployee: UpdateEmployeeUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val employeeId: Long = savedStateHandle[ID_ARG] ?: NEW_EMPLOYEE_ID

    private val isEditing: Boolean = employeeId != NEW_EMPLOYEE_ID

    /** The persisted row, so `toEmployee` can rebuild it without losing what the form cannot show. */
    private var existing: Employee? = null

    private val _uiState = MutableStateFlow(
        EmployeeEditUiState(isEditing = isEditing, isLoading = isEditing),
    )
    val uiState: StateFlow<EmployeeEditUiState> = _uiState.asStateFlow()

    private val _effects = Channel<EmployeeEditEffect>(Channel.BUFFERED)
    val effects: Flow<EmployeeEditEffect> = _effects.receiveAsFlow()

    init {
        if (isEditing) {
            viewModelScope.launch { load() }
        }
    }

    fun onEvent(event: EmployeeEditEvent) {
        when (event) {
            EmployeeEditEvent.Save -> save()
            EmployeeEditEvent.Retry -> viewModelScope.launch { load() }
            else -> change(event)
        }
    }

    /** Applies one edited value and re-validates, so the errors always describe the current form. */
    private fun change(event: EmployeeEditEvent) {
        val current = _uiState.value
        val form = current.form.with(event)
        _uiState.value = current.copy(
            form = form,
            errors = EmployeeValidator.validate(form, LocalDate.now()),
        )
    }

    private fun save() {
        // Re-validated here as well, because Save is also reachable while the row is still loading.
        val errors = EmployeeValidator.validate(_uiState.value.form, LocalDate.now())
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(errors = errors, showErrors = true) }
            return
        }
        val employee = _uiState.value.form.toEmployee(existing)
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, showErrors = true, errors = errors) }
            try {
                if (isEditing) {
                    updateEmployee(employee)
                } else {
                    createEmployee(employee)
                }
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
                _effects.trySend(EmployeeEditEffect.Saved)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                _uiState.update { it.copy(isSaving = false) }
                _effects.trySend(EmployeeEditEffect.SaveFailed)
            }
        }
    }

    /** Reads the row once, not as a stream: the form must not be overwritten while it is edited. */
    private suspend fun load() {
        _uiState.update { it.copy(isLoading = true, error = false) }
        try {
            val row = getEmployee(employeeId).first()
            existing = row
            val form = row?.toForm() ?: EmployeeForm()
            _uiState.update {
                it.copy(
                    form = form,
                    errors = EmployeeValidator.validate(form, LocalDate.now()),
                    isLoading = false,
                    notFound = row == null,
                )
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            _uiState.update { it.copy(isLoading = false, error = true) }
        }
    }

    private companion object {
        /** The type-safe route property name of `Screen.EmployeeEdit.id`. */
        const val ID_ARG = "id"
    }
}

/** The one field [event] changes, leaving every other value of the form untouched. */
private fun EmployeeForm.with(event: EmployeeEditEvent): EmployeeForm = when (event) {
    is EmployeeEditEvent.FullNameChanged -> copy(fullName = event.value)
    is EmployeeEditEvent.DateOfBirthChanged -> copy(dateOfBirth = event.value)
    is EmployeeEditEvent.HireDateChanged -> copy(hireDate = event.value)
    is EmployeeEditEvent.DepartmentChanged -> copy(department = event.value)
    is EmployeeEditEvent.PositionChanged -> copy(position = event.value)
    is EmployeeEditEvent.GradeChanged -> copy(grade = event.value)
    is EmployeeEditEvent.SalaryChanged -> copy(salary = sanitizeSalary(event.value))
    is EmployeeEditEvent.EmailChanged -> copy(email = event.value)
    is EmployeeEditEvent.PhoneChanged -> copy(phone = event.value)
    is EmployeeEditEvent.AddressChanged -> copy(address = event.value)
    is EmployeeEditEvent.NotesChanged -> copy(notes = event.value)
    EmployeeEditEvent.Save, EmployeeEditEvent.Retry -> this
}
