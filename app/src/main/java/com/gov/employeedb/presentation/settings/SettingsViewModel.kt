package com.gov.employeedb.presentation.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gov.employeedb.core.AppLanguage
import com.gov.employeedb.core.AppThemeMode
import com.gov.employeedb.presentation.main.AppUiState
import com.gov.employeedb.presentation.main.AppViewModel
import com.gov.employeedb.core.LocaleManager
import com.gov.employeedb.data.excel.ExportFormat
import com.gov.employeedb.data.preferences.PreferencesRepository
import com.gov.employeedb.domain.model.EmployeeField
import com.gov.employeedb.presentation.common.*
import com.gov.employeedb.domain.repository.EmployeeRepository
import com.gov.employeedb.domain.usecase.ExportAnalyticsUseCase
import com.gov.employeedb.domain.usecase.ExportEmployeesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Drives the settings screen (PLAN.md Phase-11 checklist):
 * - Language/Theme changes persist instantly via [PreferencesRepository] and apply via [LocaleManager]
 * - Export all employees uses [ExportEmployeesUseCase] with the chosen [ExportFormat]
 * - Delete all uses [EmployeeRepository.deleteAll()] with a double-confirm dialog
 * - The employee count is refreshed after each mutating operation
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val employeeRepository: EmployeeRepository,
    private val exportEmployees: ExportEmployeesUseCase,
    private val exportAnalytics: ExportAnalyticsUseCase,
    private val appViewModel: AppViewModel,
    private val localeManager: LocaleManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects: Flow<SettingsEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            appViewModel.uiState.collect { appState ->
                _uiState.update { it.copy(language = appState.language, themeMode = appState.themeMode) }
            }
        }
        refreshEmployeeCount()
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.SetLanguage -> setLanguage(event.language)
            is SettingsEvent.SetThemeMode -> setThemeMode(event.themeMode)
            is SettingsEvent.SetExportFormat -> _uiState.update { it.copy(exportFormat = event.format) }
            is SettingsEvent.ExportAll -> exportAll(event.uri, event.format)
            is SettingsEvent.ImportReplaceConfirmed -> _uiState.update { it.copy(dialog = SettingsDialog.ImportReplace(it.employeeCount)) }
            is SettingsEvent.DeleteAllRequested -> _uiState.update { it.copy(dialog = SettingsDialog.DeleteAll1) }
            is SettingsEvent.DeleteAllConfirmed -> deleteAllConfirmed()
            SettingsEvent.DialogDismissed -> _uiState.update { it.copy(dialog = null) }
            SettingsEvent.Retry -> _uiState.update { it.copy(error = false) }
        }
    }

    private fun setLanguage(language: AppLanguage) {
        _uiState.update { it.copy(language = language) }
        viewModelScope.launch { preferencesRepository.setLanguage(language) }
        localeManager.cache(language)
    }

    private fun setThemeMode(themeMode: AppThemeMode) {
        _uiState.update { it.copy(themeMode = themeMode) }
        viewModelScope.launch { preferencesRepository.setThemeMode(themeMode) }
    }

    private fun exportAll(uri: Uri, format: ExportFormat) {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            val result = exportEmployees(uri, headersFor = { it.labelRes() }, format = format)
            _uiState.update { it.copy(isBusy = false) }
            _effects.trySend(if (result.isSuccess) SettingsEffect.Exported else SettingsEffect.ExportFailed)
        }
    }

    private fun deleteAllConfirmed() {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(isBusy = true, dialog = null) }
        viewModelScope.launch {
            try {
                employeeRepository.deleteAll()
                _effects.trySend(SettingsEffect.DataDeleted)
            } catch (e: Throwable) {
                if (e !is CancellationException) _uiState.update { it.copy(error = true) }
            } finally {
                refreshEmployeeCount()
                _uiState.update { it.copy(isBusy = false) }
            }
        }
    }

    private fun refreshEmployeeCount() {
        viewModelScope.launch {
            employeeRepository.observeCount().collect { count ->
                _uiState.update { it.copy(employeeCount = count) }
            }
        }
    }
}