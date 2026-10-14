package com.gov.employeedb.presentation.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gov.employeedb.BuildConfig
import com.gov.employeedb.domain.usecase.ObserveDashboardStatsUseCase
import com.gov.employeedb.domain.usecase.SeedDebugDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Everything the dashboard screen can ask for. */
sealed interface DashboardEvent {
    /** The pull gesture or the error state's Retry: read the statistics again. */
    data object Refresh : DashboardEvent

    /** Debug-only: fill the database with the deterministic seed dataset. */
    data object SeedDebugData : DashboardEvent
}

/**
 * Drives the dashboard (PLAN.md Phase-9 checklist): reads the long-lived stat stream from
 * [ObserveDashboardStatsUseCase] and publishes the newest snapshot as [DashboardUiState].
 *
 * A refresh is just a bump of the retry tick, which `collectLatest` turns into a fresh subscription
 * and so a fresh emission (the analytics work itself runs on the default dispatcher, inside the use
 * case). The same event serves the pull gesture and the error state's Retry, since both mean exactly
 * "read it again". The elapsed time of each read is logged under `ZolvexPerf` in debug builds only,
 * and a read that throws sets [DashboardUiState.error] while the collector stays alive, so Retry can
 * succeed.
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val observeDashboardStats: ObserveDashboardStatsUseCase,
    private val seedDebugData: SeedDebugDataUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val retryTick = MutableStateFlow(0)
    private var seedJob: Job? = null

    init {
        viewModelScope.launch {
            retryTick.collectLatest { load() }
        }
    }

    fun onEvent(event: DashboardEvent) {
        when (event) {
            DashboardEvent.Refresh -> refresh()
            DashboardEvent.SeedDebugData -> startSeed()
        }
    }

    private fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        retryTick.value += 1
    }

    private suspend fun load() {
        val startedAtNanos = System.nanoTime()
        _uiState.update { it.copy(error = false) }
        try {
            observeDashboardStats().collect { stats ->
                val snapshot = stats.snapshot
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        total = snapshot.total,
                        averageAge = snapshot.averageAge,
                        averageService = snapshot.averageService,
                        retirementSoon = snapshot.retirementSoon,
                        ageBuckets = snapshot.ageBuckets,
                        serviceBuckets = snapshot.serviceBuckets,
                        recent = stats.recent,
                    )
                }
                logAnalytics(startedAtNanos, snapshot.total)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            _uiState.update { it.copy(isLoading = false, isRefreshing = false, error = true) }
        }
    }

    private fun logAnalytics(startedAtNanos: Long, rows: Int) {
        if (!BuildConfig.DEBUG) return
        val elapsedMs = (System.nanoTime() - startedAtNanos) / NANOS_PER_MILLI
        Log.i(ANALYTICS_TAG, "analytics_ms=$elapsedMs rows=$rows")
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
        const val ANALYTICS_TAG = "ZolvexPerf"
        const val NANOS_PER_MILLI = 1_000_000L
    }
}
