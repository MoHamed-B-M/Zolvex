package com.gov.employeedb.presentation.employees.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gov.employeedb.domain.model.Employee
import com.gov.employeedb.domain.usecase.CountEmployeesUseCase
import com.gov.employeedb.domain.usecase.DeleteEmployeeUseCase
import com.gov.employeedb.domain.usecase.GetEmployeeByIdUseCase
import com.gov.employeedb.domain.usecase.ObserveEmployeesUseCase
import com.gov.employeedb.domain.usecase.RestoreEmployeeUseCase
import com.gov.employeedb.domain.usecase.SeedDebugDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Everything the list screen can ask for. */
sealed interface EmployeeListEvent {
    /** The raw search text changed (debounced by the ViewModel). */
    data class Search(val query: String) : EmployeeListEvent

    /** The filter sheet was applied. */
    data class ApplyFilter(val filters: EmployeeFilters) : EmployeeListEvent

    /** The sort menu changed the field or direction. */
    data class SetSort(val sort: EmployeeSort) : EmployeeListEvent

    /** The user scrolled to the end of the loaded window. */
    data object LoadMore : EmployeeListEvent

    /** A swipe-to-delete removed one row; the full record is kept for [UndoDelete]. */
    data class Delete(val id: Long) : EmployeeListEvent

    /** The Snackbar's Undo action re-inserts the last deleted employee. */
    data object UndoDelete : EmployeeListEvent

    /** Debug-only: fill the database with the deterministic seed dataset. */
    data object SeedDebugData : EmployeeListEvent

    /** The last read failed and the error state's Retry was tapped. */
    data object Retry : EmployeeListEvent

    /** The expandable FAB's "Add employee" action. */
    data object AddClick : EmployeeListEvent

    /** The expandable FAB's "Import Excel" action. */
    data object ImportClick : EmployeeListEvent
}

/** One-shot navigation requests the screen consumes once. */
sealed interface EmployeeListEffect {
    data object NavigateToAdd : EmployeeListEffect
    data object NavigateToImport : EmployeeListEffect
}

/**
 * Drives the employee list (PLAN.md Phase-6 checklist): a debounced search, filter/sort state, and a
 * windowed Room query whose `limit` grows one [PAGE_SIZE] page at a time.
 *
 * The query/filters/sort/page inputs are combined into one [ListParams] stream and consumed with
 * `collectLatest`, so a new keystroke cancels the in-flight query exactly like `flatMapLatest`
 * would. Deletion fetches the full [Employee] first, so the 5-second Snackbar window can restore
 * everything the summary did not carry.
 */
@HiltViewModel
class EmployeeListViewModel @Inject constructor(
    private val observeEmployees: ObserveEmployeesUseCase,
    private val countEmployees: CountEmployeesUseCase,
    private val getEmployee: GetEmployeeByIdUseCase,
    private val deleteEmployee: DeleteEmployeeUseCase,
    private val restoreEmployee: RestoreEmployeeUseCase,
    private val seedDebugData: SeedDebugDataUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmployeeListUiState())
    val uiState: StateFlow<EmployeeListUiState> = _uiState.asStateFlow()

    private val _effects = Channel<EmployeeListEffect>(Channel.BUFFERED)
    val effects: Flow<EmployeeListEffect> = _effects.receiveAsFlow()

    private val query = MutableStateFlow("")
    private val filters = MutableStateFlow(EmployeeFilters())
    private val sort = MutableStateFlow(EmployeeSort())
    private val page = MutableStateFlow(FIRST_PAGE)
    private val retryTick = MutableStateFlow(0)

    private var pendingUndo: Employee? = null
    private var seedJob: Job? = null

    private data class ListParams(
        val filters: EmployeeFilters,
        val sort: EmployeeSort,
        val page: Int,
    )

    private val params: Flow<ListParams> = combine(
        query.debounce(QUERY_DEBOUNCE_MS).distinctUntilChanged(),
        filters,
        sort,
        page,
        retryTick,
    ) { text, filterState, sortState, pageNumber, _ ->
        ListParams(filterState.copy(query = text), sortState, pageNumber)
    }

    init {
        viewModelScope.launch {
            countEmployees().collect { total -> _uiState.update { it.copy(total = total) } }
        }
        viewModelScope.launch {
            params.collectLatest { load(it) }
        }
    }

    fun onEvent(event: EmployeeListEvent) {
        when (event) {
            is EmployeeListEvent.Search -> onSearch(event.query)
            is EmployeeListEvent.ApplyFilter -> {
                filters.value = event.filters.copy(query = query.value)
                page.value = FIRST_PAGE
            }

            is EmployeeListEvent.SetSort -> {
                sort.value = event.sort
                page.value = FIRST_PAGE
            }

            EmployeeListEvent.LoadMore -> if (_uiState.value.canLoadMore && !_uiState.value.isLoading) {
                page.value += 1
            }

            is EmployeeListEvent.Delete -> delete(event.id)
            EmployeeListEvent.UndoDelete -> undoDelete()
            EmployeeListEvent.Retry -> retryTick.value += 1
            EmployeeListEvent.SeedDebugData -> startSeed()
            EmployeeListEvent.AddClick -> _effects.trySend(EmployeeListEffect.NavigateToAdd)
            EmployeeListEvent.ImportClick -> _effects.trySend(EmployeeListEffect.NavigateToImport)
        }
    }

    private fun onSearch(text: String) {
        if (query.value == text) return
        query.value = text
        page.value = FIRST_PAGE
    }

    private suspend fun load(params: ListParams) {
        val firstPage = params.page == FIRST_PAGE
        _uiState.update {
            it.copy(
                filters = params.filters,
                sort = params.sort,
                query = params.filters.query,
                isLoading = firstPage,
                isLoadingMore = !firstPage,
                error = false,
            )
        }
        val today = LocalDate.now()
        val (birthFrom, birthTo) = BucketRanges.birthRange(params.filters.ageBuckets, today)
        val (hireFrom, hireTo) = BucketRanges.hireRange(params.filters.serviceBuckets, today)
        try {
            observeEmployees(
                query = params.filters.query,
                department = params.filters.department,
                birthFrom = birthFrom,
                birthTo = birthTo,
                hireFrom = hireFrom,
                hireTo = hireTo,
                sortKey = params.sort.field.key,
                ascending = params.sort.ascending,
                limit = PAGE_SIZE * params.page,
                offset = 0,
            ).collect { items ->
                val window = PAGE_SIZE * params.page
                _uiState.update {
                    it.copy(
                        items = items,
                        isLoading = false,
                        isLoadingMore = false,
                        canLoadMore = items.size >= window,
                    )
                }
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            _uiState.update { it.copy(isLoading = false, isLoadingMore = false, error = true) }
        }
    }

    private fun delete(id: Long) {
        viewModelScope.launch {
            val employee = getEmployee(id).first() ?: return@launch
            deleteEmployee(id)
            pendingUndo = employee
        }
    }

    private fun undoDelete() {
        val employee = pendingUndo ?: return
        pendingUndo = null
        viewModelScope.launch { restoreEmployee(employee) }
    }

    private fun startSeed() {
        if (seedJob?.isActive == true) return
        _uiState.update { it.copy(isSeeding = true) }
        seedJob = viewModelScope.launch {
            try {
                seedDebugData()
            } finally {
                _uiState.update { it.copy(isSeeding = false) }
            }
        }
    }

    private companion object {
        const val PAGE_SIZE = 100
        const val FIRST_PAGE = 1
        const val QUERY_DEBOUNCE_MS = 200L
    }
}
