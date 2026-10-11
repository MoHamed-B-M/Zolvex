package com.gov.employeedb.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gov.employeedb.core.AppLanguage
import com.gov.employeedb.core.AppThemeMode
import com.gov.employeedb.core.LocaleManager
import com.gov.employeedb.data.preferences.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
 * through [AppUiState] and never open the preference file themselves.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    localeManager: LocaleManager,
) : ViewModel() {

    /** Flipped once the stored settings are in hand, which releases the splash screen. */
    private val isLoaded = MutableStateFlow(false)

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
}
