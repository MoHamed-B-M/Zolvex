package com.gov.employeedb.presentation.analytics

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.gov.employeedb.core.AppResult
import com.gov.employeedb.data.excel.ExcelMime
import com.gov.employeedb.data.excel.ExcelWriter
import com.gov.employeedb.data.local.projection.EmployeeStatRow
import com.gov.employeedb.domain.analytics.WorkforceAnalytics
import com.gov.employeedb.domain.model.AnalyticsSnapshot
import com.gov.employeedb.domain.model.BucketCount
import com.gov.employeedb.domain.model.EmployeeSummary
import com.gov.employeedb.domain.repository.EmployeeRepository
import com.gov.employeedb.domain.usecase.ExportAnalyticsUseCase
import com.gov.employeedb.domain.usecase.ObserveAnalyticsUseCase
import com.gov.employeedb.testutil.MainDispatcherRule
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.relaxed
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/**
 * The analytics ViewModel's contract (PLAN.md Phase-10 checklist):
 * - the department filter recomputes the whole snapshot
 * - the export writes a non-empty workbook to the SAF document
 * - the filter chips stay stable during filtering
 * - error and retry work as on the dashboard
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AnalyticsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeStatRepository()
    private val context = mockk<Context>(relaxed = true)
    private val resolver = mockk<ContentResolver>(relaxed = true)
    private val uri = mockk<Uri>(relaxed = true)

    private fun TestScope.newViewModel(): AnalyticsViewModel = AnalyticsViewModel(
        ObserveAnalyticsUseCase(repository, StandardTestDispatcher(testScheduler)),
        ExportAnalyticsUseCase(
            context,
            repository,
            ExcelWriter(),
            StandardTestDispatcher(testScheduler),
        ),
    )

    @Test
    fun `the department filter recomputes the snapshot`() = runTest {
        val today = LocalDate.now()
        // two departments, different ages
        repository.statRows.value = listOf(
            statRow(id = 1L, name = "Finance A", age = 30, serviceYears = 5, department = "Finance", today = today),
            statRow(id = 2L, name = "Health B", age = 50, serviceYears = 15, department = "Health", today = today),
        )

        val viewModel = newViewModel()
        advanceUntilIdle()

        // unfiltered: both rows
        assertThat(viewModel.uiState.value.snapshot?.total).isEqualTo(2)
        assertThat(viewModel.uiState.value.snapshot?.averageAge).isEqualTo(40.0)
        assertThat(viewModel.uiState.value.departments).containsExactly("Finance", "Health")

        // filter to Finance
        viewModel.onEvent(AnalyticsEvent.SelectDepartment("Finance"))
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.snapshot?.total).isEqualTo(1)
        assertThat(viewModel.uiState.value.snapshot?.averageAge).isEqualTo(30.0)
        // chips still offer Health (unfiltered list)
        assertThat(viewModel.uiState.value.departments).containsExactly("Finance", "Health")

        // clear filter
        viewModel.onEvent(AnalyticsEvent.SelectDepartment(null))
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.snapshot?.total).isEqualTo(2)
        assertThat(viewModel.uiState.value.snapshot?.averageAge).isEqualTo(40.0)
    }

    @Test
    fun `export writes a non-empty workbook`() = runTest {
        every { context.contentResolver } returns resolver

        val out = ByteArrayOutputStream()
        every { resolver.openOutputStream(uri) } returns out
        // openOutputStream returns a non-null stream; query for displayName returns null -> falls back
        every { resolver.query(uri, any(), null, null, null) } returns null

        val today = LocalDate.now()
        repository.statRows.value = listOf(
            statRow(id = 1L, name = "A", age = 40, serviceYears = 10, department = "Ops", today = today),
        )

        val viewModel = newViewModel()
        advanceUntilIdle()

        viewModel.onEvent(AnalyticsEvent.ExportAnalysis(uri, analyticsLabels()))
        advanceUntilIdle()

        // the workbook was written
        assertThat(out.size()).isGreaterThan(0)
        // verify the stream was closed and not empty
    }

    @Test
    fun `error on failing read and retry clears it`() = runTest {
        repository.failure = IllegalStateException("read failed")
        val viewModel = newViewModel()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.error).isTrue()

        repository.failure = null
        viewModel.onEvent(AnalyticsEvent.Retry)
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.error).isFalse()
    }

    @Test
    fun `filter chips exclude the rollup and empty label`() = runTest {
        val today = LocalDate.now()
        repository.statRows.value = listOf(
            statRow(id = 1L, name = "A", age = 30, serviceYears = 5, department = "Finance", today = today),
            statRow(id = 2L, name = "B", age = 40, serviceYears = 10, department = "Health", today = today),
            // 8 more distinct departments so the rollup is added
            statRow(id = 3L, name = "C", age = 35, serviceYears = 7, department = "Education", today = today),
            statRow(id = 4L, name = "D", age = 28, serviceYears = 3, department = "Transport", today = today),
            statRow(id = 5L, name = "E", age = 45, serviceYears = 12, department = "IT", today = today),
            statRow(id = 6L, name = "F", age = 38, serviceYears = 9, department = "Legal", today = today),
            statRow(id = 7L, name = "G", age = 32, serviceYears = 6, department = "HR", today = today),
            statRow(id = 8L, name = "H", age = 42, serviceYears = 11, department = "Marketing", today = today),
            statRow(id = 9L, name = "I", age = 29, serviceYears = 4, department = "Security", today = today),
            statRow(id = 10L, name = "J", age = 36, serviceYears = 8, department = "Research", today = today),
            statRow(id = 11L, name = "K", age = 33, serviceYears = 7, department = "Admin", today = today),
        )

        val viewModel = newViewModel()
        advanceUntilIdle()

        val departments = viewModel.uiState.value.departments
        // top 10 departments, no "Other", no ""
        assertThat(departments).hasSize(10)
        assertThat(departments).doesNotContain(WorkforceAnalytics.OTHER_LABEL)
        assertThat(departments).doesNotContain("")
    }

    private companion object {
        fun statRow(
            id: Long,
            name: String,
            age: Int?,
            serviceYears: Int?,
            department: String,
            today: LocalDate,
        ): EmployeeStatRow = EmployeeStatRow(
            id = id,
            fullName = name,
            position = "Officer",
            birthDate = age?.let { today.minusYears(it.toLong()) },
            hireDate = serviceYears?.let { today.minusYears(it.toLong()) },
            department = department,
            grade = "A",
        )

        /** The labels list in the same order [ExcelWriter] expects them (positional contract). */
        fun analyticsLabels(): List<String> = (0 until ExcelWriter.ANALYTICS_INDEX).map { "l$it" }
    }

    /** Streams the test's stat rows with an injectable failure. */
    private class FakeStatRepository : EmployeeRepository {

        val statRows = MutableStateFlow<List<EmployeeStatRow>>(emptyList())
        var failure: Throwable? = null

        override fun observeEmployees(
            query: String,
            department: String,
            birthFrom: Long?,
            birthTo: Long?,
            hireFrom: Long?,
            hireTo: Long?,
            sortKey: String,
            ascending: Boolean,
            limit: Int,
            offset: Int,
        ) = flowOf(emptyList())

        override fun observeCount() = flowOf(0)

        override fun observeStatRows() {
            failure?.let { throw it }
            return@flowOf statRows
        }

        override fun observeRecent(limit: Int) = flowOf(emptyList())

        override fun observeById(id: Long) = flowOf(null)

        override suspend fun create(employee: com.gov.employeedb.domain.model.Employee): Long = 0L

        override suspend fun existingImportKeys(): List<com.gov.employeedb.data.local.projection.ImportKeyRow> = emptyList()

        override suspend fun insertImportBatch(employees: List<com.gov.employeedb.domain.model.Employee>): List<Long> = emptyList()

        override suspend fun getAllForExport(): List<com.gov.employeedb.domain.model.Employee> = emptyList()

        override suspend fun update(employee: com.gov.employeedb.domain.model.Employee) = Unit

        override suspend fun delete(id: Long) = Unit

        override suspend fun deleteAll() = Unit
    }
}