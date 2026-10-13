package com.gov.employeedb.presentation.importx

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.gov.employeedb.core.AppError
import com.gov.employeedb.core.AppResult
import com.gov.employeedb.data.excel.ExcelPreview
import com.gov.employeedb.data.excel.ExcelWriter.ErrorReportLabels
import com.gov.employeedb.data.excel.ImportErrorReason
import com.gov.employeedb.data.excel.ImportProgress
import com.gov.employeedb.data.excel.ImportRowError
import com.gov.employeedb.data.preferences.MappingProfileStore
import com.gov.employeedb.domain.model.EmployeeField
import com.gov.employeedb.domain.usecase.ExportErrorReportUseCase
import com.gov.employeedb.domain.usecase.ImportExcelUseCase
import com.gov.employeedb.domain.usecase.PreviewExcelUseCase
import com.gov.employeedb.testutil.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.io.ByteArrayInputStream
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * The import wizard's state machine (PLAN.md Phase-7): auto-advance only on a certain detection, the
 * duplicate-field gate, the profile save on confirmation, a cancellation that keeps committed chunks,
 * and the two ways the run can end — a result or a failure.
 *
 * The three use cases and the profile store are mocked; the SAF file stream is stubbed through the
 * resolver so no real document is needed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ImportViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val context = mockk<Context>()
    private val resolver = mockk<ContentResolver>(relaxed = true)
    private val uri = mockk<Uri>(relaxed = true)
    private val previewExcel = mockk<PreviewExcelUseCase>()
    private val importExcel = mockk<ImportExcelUseCase>()
    private val exportErrorReport = mockk<ExportErrorReportUseCase>()
    private val profileStore = mockk<MappingProfileStore>()

    @Before
    fun setUp() {
        every { context.contentResolver } returns resolver
        every { resolver.openInputStream(uri) } returns ByteArrayInputStream(ByteArray(0))
    }

    private fun TestScope.newViewModel(): ImportViewModel = ImportViewModel(
        context,
        previewExcel,
        importExcel,
        exportErrorReport,
        profileStore,
        StandardTestDispatcher(testScheduler),
    )

    @Test
    fun `a fully detected file lands on the preview step`() = runTest {
        val preview = preview(FULL_MAPPING)
        coEvery { previewExcel(uri) } returns AppResult.Success(preview)

        val viewModel = newViewModel()
        viewModel.onEvent(ImportEvent.FileSelected(uri))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.step).isEqualTo(ImportStep.PREVIEW)
        assertThat(state.mapping).isEqualTo(FULL_MAPPING)
        assertThat(state.isReadingFile).isFalse()
    }

    @Test
    fun `an uncertain detection stops on the mapping step`() = runTest {
        coEvery { previewExcel(uri) } returns AppResult.Success(
            preview(mapOf(0 to EmployeeField.FULL_NAME, 1 to EmployeeField.DATE_OF_BIRTH)),
        )

        val viewModel = newViewModel()
        viewModel.onEvent(ImportEvent.FileSelected(uri))
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.step).isEqualTo(ImportStep.MAPPING)
    }

    @Test
    fun `mapping two columns to one field blocks the mapping`() = runTest {
        coEvery { previewExcel(uri) } returns AppResult.Success(
            preview(mapOf(0 to EmployeeField.FULL_NAME, 1 to EmployeeField.DATE_OF_BIRTH)),
        )

        val viewModel = newViewModel()
        viewModel.onEvent(ImportEvent.FileSelected(uri))
        advanceUntilIdle()

        viewModel.onEvent(ImportEvent.MappingChanged(2, EmployeeField.HIRE_DATE))
        assertThat(viewModel.uiState.value.mappingValid).isTrue()

        viewModel.onEvent(ImportEvent.MappingChanged(2, EmployeeField.FULL_NAME))
        assertThat(viewModel.uiState.value.duplicateField).isEqualTo(EmployeeField.FULL_NAME)
        assertThat(viewModel.uiState.value.mappingValid).isFalse()

        viewModel.onEvent(ImportEvent.MappingChanged(2, EmployeeField.HIRE_DATE))
        assertThat(viewModel.uiState.value.duplicateField).isNull()
        assertThat(viewModel.uiState.value.mappingValid).isTrue()
    }

    @Test
    fun `confirming a complete mapping saves the profile and advances`() = runTest {
        coEvery { previewExcel(uri) } returns AppResult.Success(
            preview(mapOf(0 to EmployeeField.FULL_NAME, 1 to EmployeeField.DATE_OF_BIRTH)),
        )
        coEvery { profileStore.save(any(), any()) } returns Unit

        val viewModel = newViewModel()
        viewModel.onEvent(ImportEvent.FileSelected(uri))
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.step).isEqualTo(ImportStep.MAPPING)

        viewModel.onEvent(ImportEvent.MappingChanged(2, EmployeeField.HIRE_DATE))
        viewModel.onEvent(ImportEvent.Next)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.step).isEqualTo(ImportStep.PREVIEW)
        coVerify { profileStore.save(any(), any()) }
    }

    @Test
    fun `cancelling an in-flight import returns to the preview step`() = runTest {
        coEvery { previewExcel(uri) } returns AppResult.Success(preview(FULL_MAPPING))
        every { importExcel(any(), any(), any(), any(), any(), any(), any()) } returns flow {
            emit(ImportProgress.Started(10))
            awaitCancellation()
        }

        val viewModel = newViewModel()
        viewModel.onEvent(ImportEvent.FileSelected(uri))
        advanceUntilIdle()
        viewModel.onEvent(ImportEvent.Import)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.step).isEqualTo(ImportStep.IMPORTING)

        viewModel.onEvent(ImportEvent.CancelImport)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.step).isEqualTo(ImportStep.PREVIEW)
        assertThat(viewModel.uiState.value.progress).isNull()
    }

    @Test
    fun `a clean run lands on the result step with the counters`() = runTest {
        coEvery { previewExcel(uri) } returns AppResult.Success(preview(FULL_MAPPING))
        every { importExcel(any(), any(), any(), any(), any(), any(), any()) } returns flowOf(
            ImportProgress.Started(3),
            ImportProgress.Progress(processed = 3, totalRows = 3),
            ImportProgress.Done(
                imported = 2,
                skippedDuplicates = 1,
                failed = 0,
                errors = emptyList(),
                batchId = "batch-clean",
            ),
        )

        val viewModel = newViewModel()
        viewModel.onEvent(ImportEvent.FileSelected(uri))
        advanceUntilIdle()
        viewModel.onEvent(ImportEvent.Import)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.step).isEqualTo(ImportStep.RESULT)
        assertThat(state.result?.imported).isEqualTo(2)
        assertThat(state.result?.skippedDuplicates).isEqualTo(1)
        assertThat(state.result?.errorsTruncated).isFalse()
        assertThat(state.error).isNull()
    }

    @Test
    fun `rejected rows mark the report as truncated`() = runTest {
        val error = ImportRowError(5, "Hire", "nope", ImportErrorReason.INVALID_DATE)
        coEvery { previewExcel(uri) } returns AppResult.Success(preview(FULL_MAPPING))
        every { importExcel(any(), any(), any(), any(), any(), any(), any()) } returns flowOf(
            ImportProgress.Started(3),
            ImportProgress.Done(
                imported = 1,
                skippedDuplicates = 0,
                failed = 3,
                errors = listOf(error),
                batchId = "batch-errors",
            ),
        )

        val viewModel = newViewModel()
        viewModel.onEvent(ImportEvent.FileSelected(uri))
        advanceUntilIdle()
        viewModel.onEvent(ImportEvent.Import)
        advanceUntilIdle()

        val result = viewModel.uiState.value.result
        assertThat(result?.failed).isEqualTo(3)
        assertThat(result?.errors).hasSize(1)
        assertThat(result?.errorsTruncated).isTrue()
    }

    @Test
    fun `a broken run surfaces a read failure on the result step`() = runTest {
        coEvery { previewExcel(uri) } returns AppResult.Success(preview(FULL_MAPPING))
        every { importExcel(any(), any(), any(), any(), any(), any(), any()) } returns flow {
            throw IllegalStateException("stream broke")
        }

        val viewModel = newViewModel()
        viewModel.onEvent(ImportEvent.FileSelected(uri))
        advanceUntilIdle()
        viewModel.onEvent(ImportEvent.Import)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertThat(state.step).isEqualTo(ImportStep.RESULT)
        assertThat(state.result).isNull()
        assertThat(state.error).isInstanceOf(AppError.FileRead::class.java)
    }

    @Test
    fun `downloading the report emits the saved effect`() = runTest {
        val error = ImportRowError(5, "Hire", "nope", ImportErrorReason.INVALID_DATE)
        val labels = ErrorReportLabels("Sheet", "Row", "Column", "Value", "Reason")
        coEvery { previewExcel(uri) } returns AppResult.Success(preview(FULL_MAPPING))
        coEvery { exportErrorReport(any(), any(), any()) } returns AppResult.Success(Unit)
        every { importExcel(any(), any(), any(), any(), any(), any(), any()) } returns flowOf(
            ImportProgress.Done(
                imported = 0,
                skippedDuplicates = 0,
                failed = 1,
                errors = listOf(error),
                batchId = "batch-report",
            ),
        )

        val viewModel = newViewModel()
        viewModel.onEvent(ImportEvent.FileSelected(uri))
        advanceUntilIdle()
        viewModel.onEvent(ImportEvent.Import)
        advanceUntilIdle()

        val effects = mutableListOf<ImportEffect>()
        val job = launch { viewModel.effects.collect { effects += it } }
        viewModel.onEvent(ImportEvent.DownloadErrorReport(uri, labels))
        advanceUntilIdle()
        job.cancel()

        assertThat(effects).containsExactly(ImportEffect.ErrorReportSaved)
        coVerify { exportErrorReport(uri, any(), labels) }
    }

    private companion object {
        val FULL_MAPPING: Map<Int, EmployeeField> = mapOf(
            0 to EmployeeField.FULL_NAME,
            1 to EmployeeField.DATE_OF_BIRTH,
            2 to EmployeeField.HIRE_DATE,
        )

        fun preview(mapping: Map<Int, EmployeeField>): ExcelPreview = ExcelPreview(
            sheetName = "Employees",
            headerRowIndex = 1,
            headers = listOf("Name", "Birth", "Hire", "Department"),
            detectedMapping = mapping,
            sampleRows = listOf(listOf("Ada", "2000-01-01", "2020-01-01", "Ops")),
            totalRowEstimate = 10,
        )
    }
}
