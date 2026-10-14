package com.gov.employeedb.presentation.employees.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gov.employeedb.domain.usecase.DeleteEmployeeUseCase
import com.gov.employeedb.domain.usecase.GetEmployeeByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The two things the detail screen can ask for. */
sealed interface EmployeeDetailEvent {

    /** The delete dialog was confirmed. */
    data object Delete : EmployeeDetailEvent

    /** Reloads the row after a failed fetch. */
    data object Retry : EmployeeDetailEvent
}

/** One-shot requests the detail screen consumes exactly once. */
sealed interface EmployeeDetailEffect {

    /** The row is gone; the screen leaves for the list it came from. */
    data object Deleted : EmployeeDetailEffect
}

/**
 * The read-only employee screen's state holder (PLAN.md Phase-8 checklist).
 *
 * The id arrives as the type-safe route argument through [SavedStateHandle] (the route property is
 * named `id`), so there is no `Load(id)` call for the screen to forget and no nullable route state
 * to handle: the argument cannot be missing because the route does not compile without it.
 *
 * The row is *observed* rather than fetched once, so returning from the edit form shows the saved
 * values immediately, and [EmployeeDetailEvent.Retry] restarts the observation through the same
 * `collectLatest` tick [com.gov.employeedb.presentation.employees.list.EmployeeListViewModel] uses.
 */
@HiltViewModel
class EmployeeDetailViewModel @Inject constructor(
    private val getEmployee: GetEmployeeByIdUseCase,
    private val deleteEmployee: DeleteEmployeeUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val employeeId: Long = savedStateHandle[ID_ARG] ?: 0L

    private val _uiState = MutableStateFlow(EmployeeDetailUiState())
    val uiState: StateFlow<EmployeeDetailUiState> = _uiState.asStateFlow()

    private val _effects = Channel<EmployeeDetailEffect>(Channel.BUFFERED)
    val effects: Flow<EmployeeDetailEffect> = _effects.receiveAsFlow()

    private val retryTick = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            retryTick.collectLatest { observe() }
        }
    }

    fun onEvent(event: EmployeeDetailEvent) {
        when (event) {
            EmployeeDetailEvent.Delete -> delete()
            EmployeeDetailEvent.Retry -> retryTick.update { it + 1 }
        }
    }

    /** Mirrors the repository stream into the UI state until the screen leaves or Retry restarts it. */
    private suspend fun observe() {
        _uiState.update { it.copy(isLoading = true, error = false) }
        try {
            getEmployee(employeeId).collect { employee ->
                _uiState.update {
                    it.copy(
                        employee = employee,
                        isLoading = false,
                        error = false,
                    )
                }
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            _uiState.update { it.copy(isLoading = false, error = true) }
        }
    }

    private fun delete() {
        viewModelScope.launch {
            try {
                deleteEmployee(employeeId)
                _effects.trySend(EmployeeDetailEffect.Deleted)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                _uiState.update { it.copy(error = true) }
            }
        }
    }

    private companion object {
        /** The type-safe route property name of `Screen.EmployeeDetail.id`. */
        const val ID_ARG = "id"
    }
}
