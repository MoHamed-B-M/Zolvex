package com.gov.employeedb.presentation.importx

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gov.employeedb.core.AppError
import com.gov.employeedb.core.AppResult.Failure
import com.gov.employeedb.core.AppResult.Success
import com.gov.employeedb.data.excel.ExcelPreview
import com.gov.employeedb.data.excel.ExcelWriter.ErrorReportLabels
import com.gov.employeedb.data.excel.ImportProgress
import com.gov.employeedb.data.excel.ImportResult
import com.gov.employeedb.data.excel.SafDocumentHelper
import com.gov.employeedb.data.preferences.MappingProfileStore
import com.gov.employeedb.di.IoDispatcher
import com.gov.employeedb.domain.model.EmployeeField
import com.gov.employeedb.domain.usecase.ExportErrorReportUseCase
import com.gov.employeedb.domain.usecase.ImportExcelUseCase
import com.gov.employeedb.domain.usecase.PreviewExcelUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Everything the wizard's steps can ask for. */
sealed interface ImportEvent {

    /** SAF returned a document for the user's pick. */
    data class FileSelected(val uri: Uri) : ImportEvent

    /** One column's dropdown changed; `null` is the Ignore row. */
    data class MappingChanged(val column: Int, val field: EmployeeField?) : ImportEvent

    /** The "skip duplicates" switch moved. */
    data class SkipDuplicatesChanged(val skip: Boolean) : ImportEvent

    /** The step's primary action (Continue). */
    data object Next : ImportEvent

    /** The step's chrome action (Back). */
    data object Back : ImportEvent

    /** Commit the mapping and stream the sheet. */
    data object Import : ImportEvent

    /** Abort an in-flight import; committed chunks stay durable (PLAN.md §6.3). */
    data object CancelImport : ImportEvent

    /** Write the failure report to the SAF document [uri], using the localized [labels]. */
    data class DownloadErrorReport(val uri: Uri, val labels: ErrorReportLabels) : ImportEvent

    /** Leave the wizard for the employee list. */
    data object ViewEmployees : ImportEvent

    /** Dismiss the inline failure without retrying. */
    data object ClearError : ImportEvent
}

/** One-shot requests the screen consumes once. */
sealed interface ImportEffect {
    data object NavigateToEmployees : ImportEffect
    data object ErrorReportSaved : ImportEffect
}

/**
 * Drives the import wizard (PLAN.md Phase-7 checklist): preview a picked file, confirm a column
 * mapping, stream it into the database, and hand back a failure report.
 *
 * [FileSelected] delegates detection to [PreviewExcelUseCase] — which already re-applies a stored
 * mapping profile — and jumps straight to preview when the detected core fields are complete,
 * dropping to the mapping step only when something needs a human. A mid-import [ImportEvent.CancelImport]
 * cancels the collector; the rows already committed in their 500-row transactions remain, per §6.3.
 */
@HiltViewModel
class ImportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val previewExcel: PreviewExcelUseCase,
    private val importExcel: ImportExcelUseCase,
    private val exportErrorReport: ExportErrorReportUseCase,
    private val profileStore: MappingProfileStore,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImportUiState())
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    private val _effects = Channel<ImportEffect>(Channel.BUFFERED)
    val effects: Flow<ImportEffect> = _effects.receiveAsFlow()

    private var importJob: Job? = null

    fun onEvent(event: ImportEvent) {
        when (event) {
            is ImportEvent.FileSelected -> readFile(event.uri)
            is ImportEvent.MappingChanged -> setMapping(event.column, event.field)
            is ImportEvent.SkipDuplicatesChanged ->
                _uiState.update { it.copy(skipDuplicates = event.skip) }

            ImportEvent.Next -> next()
            ImportEvent.Back -> back()
            ImportEvent.Import -> startImport()
            ImportEvent.CancelImport -> cancelImport()
            is ImportEvent.DownloadErrorReport -> writeErrorReport(event.uri, event.labels)
            ImportEvent.ViewEmployees -> _effects.trySend(ImportEffect.NavigateToEmployees)
            ImportEvent.ClearError -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun readFile(uri: Uri) {
        _uiState.update {
            it.copy(
                step = ImportStep.SELECT,
                uri = uri,
                preview = null,
                mapping = emptyMap(),
                progress = null,
                result = null,
                error = null,
                isReadingFile = true,
            )
        }
        viewModelScope.launch {
            when (val outcome = previewExcel(uri)) {
                is Success -> applyPreview(uri, outcome.data)
                is Failure -> _uiState.update {
                    it.copy(isReadingFile = false, error = outcome.error)
                }
            }
        }
    }

    private suspend fun applyPreview(uri: Uri, preview: ExcelPreview) {
        val mapping = preview.detectedMapping
        val detectedCore = mapping.values.count { it.isCore }
        val confidence = detectedCore / EmployeeField.REQUIRED_CORE.size.toFloat()
        val coreComplete = EmployeeField.REQUIRED_CORE.all { it in mapping.values }
        val fileName = withContext(dispatcher) { SafDocumentHelper.displayName(context, uri) }
        val step = if (confidence >= CONFIDENCE_THRESHOLD && coreComplete) {
            ImportStep.PREVIEW
        } else {
            ImportStep.MAPPING
        }
        _uiState.update {
            it.copy(
                isReadingFile = false,
                preview = preview,
                mapping = mapping,
                fileName = fileName,
                step = step,
            )
        }
    }

    private fun setMapping(column: Int, field: EmployeeField?) {
        _uiState.update { state ->
            val mapping = state.mapping.toMutableMap()
            if (field == null) {
                mapping.remove(column)
            } else {
                mapping[column] = field
            }
            state.copy(mapping = mapping)
        }
    }

    private fun next() {
        val state = _uiState.value
        when (state.step) {
            ImportStep.SELECT -> {
                if (state.preview == null) return
                _uiState.update {
                    it.copy(step = if (state.mappingValid) ImportStep.PREVIEW else ImportStep.MAPPING)
                }
            }

            ImportStep.MAPPING -> {
                if (!state.mappingValid) return
                val headers = state.preview?.headers ?: return
                viewModelScope.launch { profileStore.save(headers, state.mapping) }
                _uiState.update { it.copy(step = ImportStep.PREVIEW) }
            }

            else -> Unit
        }
    }

    private fun back() {
        when (_uiState.value.step) {
            ImportStep.MAPPING -> _uiState.update { it.copy(step = ImportStep.SELECT) }
            ImportStep.PREVIEW -> _uiState.update { it.copy(step = ImportStep.MAPPING) }
            else -> Unit
        }
    }

    private fun startImport() {
        val state = _uiState.value
        val preview = state.preview ?: return
        val uri = state.uri ?: return
        if (importJob?.isActive == true) return
        _uiState.update {
            it.copy(
                step = ImportStep.IMPORTING,
                progress = ImportProgress.Started(preview.totalRowEstimate),
                result = null,
                error = null,
            )
        }
        importJob = viewModelScope.launch {
            try {
                withContext(dispatcher) {
                    val input = SafDocumentHelper.openInput(context, uri)
                    if (input == null) {
                        onImportFailed(state.fileName)
                        return@withContext
                    }
                    input.use { stream ->
                        importExcel(
                            input = stream,
                            headers = preview.headers,
                            mapping = state.mapping,
                            skipDuplicates = state.skipDuplicates,
                            headerRowIndex = preview.headerRowIndex,
                            totalRows = preview.totalRowEstimate,
                            today = LocalDate.now(),
                        ).collect { progress -> onProgress(progress) }
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                onImportFailed(state.fileName)
            }
        }
    }

    private fun cancelImport() {
        importJob?.cancel()
        importJob = null
        _uiState.update { it.copy(step = ImportStep.PREVIEW, progress = null) }
    }

    private fun onProgress(progress: ImportProgress) {
        if (progress !is ImportProgress.Done) {
            _uiState.update { it.copy(progress = progress) }
            return
        }
        val result = ImportResult(
            imported = progress.imported,
            skippedDuplicates = progress.skippedDuplicates,
            failed = progress.failed,
            errors = progress.errors,
            batchId = progress.batchId,
            errorsTruncated = progress.failed > progress.errors.size,
        )
        _uiState.update {
            it.copy(step = ImportStep.RESULT, progress = progress, result = result, error = null)
        }
    }

    private fun onImportFailed(fileName: String?) {
        _uiState.update {
            it.copy(step = ImportStep.RESULT, error = AppError.FileRead(fileName, null))
        }
    }

    private fun writeErrorReport(uri: Uri, labels: ErrorReportLabels) {
        val errors = _uiState.value.result?.errors.orEmpty()
        if (errors.isEmpty()) return
        _uiState.update { it.copy(isDownloadingReport = true) }
        viewModelScope.launch {
            when (val outcome = exportErrorReport(uri, errors, labels)) {
                is Success -> {
                    _uiState.update { it.copy(isDownloadingReport = false) }
                    _effects.trySend(ImportEffect.ErrorReportSaved)
                }

                is Failure -> _uiState.update {
                    it.copy(isDownloadingReport = false, error = outcome.error)
                }
            }
        }
    }

    private companion object {
        /** PLAN.md §6.2 rule 6: auto-advance only when the detection is essentially certain. */
        const val CONFIDENCE_THRESHOLD = 0.8f
    }
}
