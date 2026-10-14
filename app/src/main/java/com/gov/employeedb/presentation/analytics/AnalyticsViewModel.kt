package com.gov.employeedb.presentation.analytics

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gov.employeedb.domain.analytics.WorkforceAnalytics
import com.gov.employeedb.domain.model.AnalyticsSnapshot
import com.gov.employeedb.domain.usecase.ExportAnalyticsUseCase
import com.gov.employeedb.domain.usecase.ObserveAnalyticsUseCase
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
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Everything the analytics screen can ask for. */
sealed interface AnalyticsEvent {

    /** A department chip was tapped; `null` clears the filter and returns to the whole workforce. */
    data class SelectDepartment(val department: String?) : AnalyticsEvent

    /** The export action: write the current snapshot as a workbook to the document behind [uri]. */
    data class ExportAnalysis(val uri: Uri, val labels: List<String>) : AnalyticsEvent

    /** The error state's Retry: read the statistics again. */
    data object Retry : AnalyticsEvent
}

/** The one-shot confirmations the screen reports through a snackbar. */
sealed interface AnalyticsEffect {

    /** The workbook was written. */
    data object AnalysisExported : AnalyticsEffect

    /** The write failed; the document is absent or incomplete. */
    data object ExportFailed : AnalyticsEffect
}

/**
 * Drives the analytics screen (PLAN.md Phase-10 checklist): subscribes to [ObserveAnalyticsUseCase]
 * for the chosen department and republishes the newest snapshot as [AnalyticsUiState].
 *
 * The department filter and the retry tick are combined into a single stream, so a chip tap and a
 * Retry mean the same thing to the collector: `collectLatest` drops the previous read and starts a
 * fresh one. The previous snapshot deliberately stays in the state while the new one is computed
 * (including on error, where Retry can still succeed), which is what lets the charts animate from
 * their old values to the new ones instead of blanking out.
 *
 * The export runs through [ExportAnalyticsUseCase] - which owns the IO dispatcher - and reports
 * itself once through [effects]; a second tap while a write is in flight is ignored so one workbook
 * cannot be written to twice.
 */
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val observeAnalytics: ObserveAnalyticsUseCase,
    private val exportAnalysis: ExportAnalyticsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<AnalyticsEffect>(Channel.BUFFERED)
    val effects: Flow<AnalyticsEffect> = _effects.receiveAsFlow()

    private val retryTick = MutableStateFlow(0)
    private val selectedDepartment = MutableStateFlow<String?>(null)
    private var exportJob: Job? = null

    init {
        viewModelScope.launch {
            combine(retryTick, selectedDepartment) { _, department -> department }
                .collectLatest { department -> load(department) }
        }
    }

    fun onEvent(event: AnalyticsEvent) {
        when (event) {
            is AnalyticsEvent.SelectDepartment -> select(event.department)
            is AnalyticsEvent.ExportAnalysis -> export(event.uri, event.labels)
            AnalyticsEvent.Retry -> retry()
        }
    }

    private fun select(department: String?) {
        if (selectedDepartment.value == department) return
        selectedDepartment.value = department
        _uiState.update { it.copy(selectedDepartment = department) }
    }

    private fun retry() {
        _uiState.update { it.copy(error = false, isLoading = it.snapshot == null) }
        retryTick.value += 1
    }

    private suspend fun load(department: String?) {
        try {
            observeAnalytics(department).collect { snapshot ->
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        snapshot = snapshot,
                        // Filtering can only shrink a department's count, so the chip row is taken
                        // from an unfiltered read and therefore never loses the chip that is in use.
                        departments = if (department == null) {
                            departmentNames(snapshot)
                        } else {
                            state.departments
                        },
                    )
                }
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            _uiState.update { it.copy(isLoading = false, error = true) }
        }
    }

    private fun export(uri: Uri, labels: List<String>) {
        if (exportJob?.isActive == true) return
        _uiState.update { it.copy(isExporting = true) }
        exportJob = viewModelScope.launch {
            val result = exportAnalysis(uri, LocalDate.now(), labels)
            _uiState.update { it.copy(isExporting = false) }
            _effects.trySend(
                if (result.isSuccess) AnalyticsEffect.AnalysisExported else AnalyticsEffect.ExportFailed,
            )
        }
    }

    /** The departments offered as filter chips: the largest ten, minus the synthetic rollup bucket. */
    private fun departmentNames(snapshot: AnalyticsSnapshot): List<String> =
        snapshot.departmentCounts
            .map { it.label }
            .filter { it != WorkforceAnalytics.OTHER_LABEL && it.isNotEmpty() }
}
