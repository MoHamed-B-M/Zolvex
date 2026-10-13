package com.gov.employeedb.domain.usecase

import com.gov.employeedb.data.excel.ExcelReader
import com.gov.employeedb.data.excel.ImportProgress
import com.gov.employeedb.data.excel.ImportRowError
import com.gov.employeedb.data.excel.RowMapper
import com.gov.employeedb.data.local.projection.ImportKeyRow
import com.gov.employeedb.domain.model.Employee
import com.gov.employeedb.domain.model.EmployeeField
import com.gov.employeedb.domain.repository.EmployeeRepository
import com.gov.employeedb.testutil.TestData
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.File
import java.io.FileInputStream
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * End-to-end import pipeline: mocks repository for dedupe/chunking, uses real ExcelReader and ExcelWriter.
 */
class ImportExcelUseCaseTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val today: LocalDate = TestData.TODAY
    private val enHeaders = listOf(
        "Full Name", "Date of Birth", "Hire Date", "Department", "Position",
        "Grade", "Salary", "Email", "Phone", "Address", "Notes",
    )

    @Test
    fun import_dedupesIdempotently() = runBlocking {
        val repository = mockk<EmployeeRepository>()
        coEvery { repository.existingImportKeys() } returns emptyList()
        val inserted = mutableListOf<List<Employee>>()
        coEvery { repository.insertImportBatch(any()) } coAnswers {
            val arg = firstArg<List<Employee>>()
            inserted += arg
            arg.map { it.hashCode().toLong() }
        }
        val useCase = ImportExcelUseCase(repository, ExcelReader(Dispatchers.IO), Dispatchers.IO)

        val emps = listOf(
            Employee(fullName = "Amine Benali", dateOfBirth = LocalDate.of(1965, 5, 12), hireDate = LocalDate.of(2001, 3, 1), salary = 38500.0),
            Employee(fullName = "Sara Bouzid", dateOfBirth = LocalDate.of(1990, 9, 30), hireDate = LocalDate.of(2012, 4, 16), salary = 32000.0),
        )

        val file = temp.newFile("small.xlsx")
        file.outputStream().use { out ->
            com.gov.employeedb.data.excel.ExcelWriter.writeEmployees(out, emps) { f ->
                when (f) {
                    EmployeeField.FULL_NAME -> enHeaders[0]
                    EmployeeField.DATE_OF_BIRTH -> enHeaders[1]
                    EmployeeField.HIRE_DATE -> enHeaders[2]
                    EmployeeField.DEPARTMENT -> enHeaders[3]
                    EmployeeField.POSITION -> enHeaders[4]
                    EmployeeField.GRADE -> enHeaders[5]
                    EmployeeField.SALARY -> enHeaders[6]
                    EmployeeField.EMAIL -> enHeaders[7]
                    EmployeeField.PHONE -> enHeaders[8]
                    EmployeeField.ADDRESS -> enHeaders[9]
                    EmployeeField.NOTES -> enHeaders[10]
                }
            }
        }

        // run 1
        val reader = ExcelReader(Dispatchers.IO)
        val preview = reader.preview(FileInputStream(file)).getOrNull()!!
        val events1 = useCase.invoke(
            FileInputStream(file),
            preview.headers,
            preview.detectedMapping,
            skipDuplicates = true,
            headerRowIndex = preview.headerRowIndex,
            totalRows = preview.totalRowEstimate,
            today = today,
        ).toList()
        val done1 = events1.filterIsInstance<ImportProgress.Done>().lastOrNull()!!
        assertThat(done1.imported).isEqualTo(2)
        assertThat(done1.skippedDuplicates).isEqualTo(0)
        coVerify(exactly = 1) { repository.insertImportBatch(any()) }

        // run 2 with same existing keys
        coEvery { repository.existingImportKeys() } returns listOf(
            ImportKeyRow(fullNameKey = "amine benali", birthEpochDay = LocalDate.of(1965, 5, 12).toEpochDay()),
            ImportKeyRow(fullNameKey = "sara bouzid", birthEpochDay = LocalDate.of(1990, 9, 30).toEpochDay()),
        )
        coEvery { repository.insertImportBatch(any()) } coAnswers { firstArg<List<Employee>>().map { it.hashCode().toLong() } }
        val events2 = useCase.invoke(
            FileInputStream(file),
            preview.headers,
            preview.detectedMapping,
            skipDuplicates = true,
            headerRowIndex = preview.headerRowIndex,
            totalRows = preview.totalRowEstimate,
            today = today,
        ).toList()
        val done2 = events2.filterIsInstance<ImportProgress.Done>().lastOrNull()!!
        assertThat(done2.imported).isEqualTo(0)
        assertThat(done2.skippedDuplicates).isEqualTo(2)
        // second run with all duplicates: buffer remains empty, so insertImportBatch may not be called
        coVerify(exactly = 1) { repository.insertImportBatch(any()) }
    }
}