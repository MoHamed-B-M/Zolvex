package com.gov.employeedb.presentation.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gov.employeedb.BuildConfig
import com.gov.employeedb.core.AppLanguage
import com.gov.employeedb.core.AppThemeMode
import com.gov.employeedb.core.LocaleManager
import com.gov.employeedb.data.preferences.PreferencesRepository
import com.gov.employeedb.data.work.UpdateCheckScheduler
import com.gov.employeedb.data.work.UpdateNotificationHelper
import com.gov.employeedb.domain.model.UpdateCheckResult
import com.gov.employeedb.domain.usecase.CheckForUpdateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Everything the UI can ask the app shell to change. */
sealed interface AppEvent {
    /** Persists the UI language; the next frame re-renders in it without recreating the Activity. */
    data class SetLanguage(val language: AppLanguage) : AppEvent

    /** Persists the light/dark/system preference. */
    data class SetThemeMode(val themeMode: AppThemeMode) : AppEvent
}

/**
 * Owns the app-wide settings state.
 *
 * It is the only place that reads [PreferencesRepository], so screens receive language and theme
 * through [AppUiState] and never open the preference file themselves. It also owns the one boot
 * time update check: the periodic work is scheduled here and the cooldown-respecting check is run
 * from here, so the composable that triggers it stays free of scheduling logic.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val checkForUpdateUseCase: CheckForUpdateUseCase,
    private val updateCheckScheduler: UpdateCheckScheduler,
    private val updateNotificationHelper: UpdateNotificationHelper,
    localeManager: LocaleManager,
) : ViewModel() {

    /** Flipped once the stored settings are in hand, which releases the splash screen. */
    private val isLoaded = MutableStateFlow(false)

    /** The most recent update check result; drives nothing in the shell yet, but is observable. */
    private val _updateState = MutableStateFlow<UpdateCheckResult>(UpdateCheckResult.Idle)
    val updateState: StateFlow<UpdateCheckResult> = _updateState.asStateFlow()

    /** Guards against a second boot check and against overlapping checks. */
    private var bootCheckStarted = false
    private var updateJob: Job? = null

    val uiState: StateFlow<AppUiState> = combine(
        preferencesRepository.language,
        preferencesRepository.themeMode,
        isLoaded,
    ) { language, themeMode, loaded ->
        AppUiState(language = language, themeMode = themeMode, isLoaded = loaded)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        // Seeded from the synchronous cache so the first frame already uses the persisted
        // language; `isLoaded = false` keeps it off-screen behind the splash screen anyway.
        initialValue = AppUiState(
            language = localeManager.cachedLanguage,
            themeMode = AppThemeMode.DEFAULT,
            isLoaded = false,
        ),
    )

    init {
        viewModelScope.launch {
            preferencesRepository.language.collect { language -> localeManager.cache(language) }
        }

        // A language chosen on the system "App languages" screen (API 33+) wins over the stored
        // value, so that screen and the in-app picker can never disagree.
        viewModelScope.launch {
            val systemLanguage = localeManager.systemLanguage() ?: return@launch
            if (systemLanguage != preferencesRepository.language.first()) {
                preferencesRepository.setLanguage(systemLanguage)
            }
        }

        viewModelScope.launch {
            // Waiting for both flows (rather than for the file to exist) is what makes the first
            // rendered frame complete: `combine` above cannot emit this pair until it has a value
            // for each of them.
            preferencesRepository.language.first()
            preferencesRepository.themeMode.first()
            isLoaded.value = true
        }
    }

    /**
     * Runs once, from the first frame painted after [AppUiState.isLoaded] turns true.
     *
     * Both branches hand their work to [viewModelScope], so the caller (a `LaunchedEffect`) returns
     * immediately and the frame is never blocked on DataStore or the network.
     */
    fun onBoot() {
        if (bootCheckStarted) return
        bootCheckStarted = true

        viewModelScope.launch {
            if (preferencesRepository.autoCheckEnabled.first()) {
                updateCheckScheduler.schedulePeriodic()
            }
        }
        checkForUpdate(force = false)
    }

    /**
     * Checks for a newer release. [force] bypasses the 24 h cooldown; the boot-time call does not
     * force, so a relaunch inside the cooldown window does no network work. When an update is
     * found the notification is posted here so the boot-time check surfaces even if the periodic
     * worker has not run yet.
     */
    fun checkForUpdate(force: Boolean) {
        if (updateJob?.isActive == true) return
        updateJob = viewModelScope.launch {
            _updateState.value = UpdateCheckResult.Checking
            val startedAtNanos = System.nanoTime()
            val result = checkForUpdateUseCase(force)
            _updateState.value = result
            logUpdateCheck(startedAtNanos, result)
            if (result is UpdateCheckResult.UpdateAvailable) {
                updateNotificationHelper.showUpdateNotification(result.info)
            }
        }
    }

    fun onEvent(event: AppEvent) {
        when (event) {
            is AppEvent.SetLanguage -> viewModelScope.launch {
                preferencesRepository.setLanguage(event.language)
            }

            is AppEvent.SetThemeMode -> viewModelScope.launch {
                preferencesRepository.setThemeMode(event.themeMode)
            }
        }
    }

    private fun logUpdateCheck(startedAtNanos: Long, result: UpdateCheckResult) {
        if (!BuildConfig.DEBUG) return
        val elapsedMs = (System.nanoTime() - startedAtNanos) / NANOS_PER_MILLI
        Log.i(UPDATE_TAG, "update_check_ms=$elapsedMs result=${result::class.simpleName}")
    }

    private companion object {
        const val UPDATE_TAG = "ZolvexPerf"
        const val NANOS_PER_MILLI = 1_000_000L
    }
}
