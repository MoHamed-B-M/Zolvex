package com.gov.employeedb.data.excel

import com.gov.employeedb.domain.model.AnalyticsSnapshot
import com.gov.employeedb.domain.model.BucketCount
import com.gov.employeedb.domain.model.Employee
import com.gov.employeedb.domain.model.EmployeeField
import com.gov.employeedb.domain.model.EmployeeSummary
import com.gov.employeedb.testutil.TestData
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Verifies employees, analytics and error-report writers round-trip or write correct structure.
 */
class ExcelWriterTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val today: LocalDate = TestData.TODAY
    private val enHeaders = listOf(
        "Full Name", "Date of Birth", "Hire Date", "Department", "Position",
        "Grade", "Salary", "Email", "Phone", "Address", "Notes",
    )

    @Test
    fun writeEmployees_roundTrip() = runBlocking {
        val emps = listOf(
            Employee(fullName = "Amine Benali", dateOfBirth = LocalDate.of(1965, 5, 12), hireDate = LocalDate.of(2001, 3, 1), department = "Finance", salary = 38500.0),
            Employee(fullName = "Sara Bouzid", dateOfBirth = LocalDate.of(1990, 9, 30), hireDate = LocalDate.of(2012, 4, 16), department = "Finance", salary = 32000.0),
        )
        val file = temp.newFile("employees.xlsx")
        file.outputStream().use { out ->
            ExcelWriter.writeEmployees(out, emps) { field ->
                when (field) {
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
        // preview + stream back via reader
        val reader = ExcelReader(Dispatchers.IO)
        val preview = reader.preview(file.inputStream()).getOrNull()!!
        assertThat(preview.headers).containsExactlyElementsIn(enHeaders)
        var count = 0
        reader.streamRows(File(file.toURI()).inputStream()).collect { row ->
            if (row.sourceRow <= preview.headerRowIndex + 1) return@collect
            count++
        }
        assertThat(count).isEqualTo(2)
    }

    @Test
    fun writeAnalytics_createsSheet() {
        val snap = AnalyticsSnapshot(
            total = 5,
            averageAge = 40.0,
            medianAge = 40.0,
            averageService = 10.0,
            medianService = 10.0,
            retirementSoon = 1,
            ageBuckets = listOf(BucketCount("B35_44", 2, 0.4f)),
            serviceBuckets = listOf(BucketCount("B10_15", 1, 0.2f)),
            departmentCounts = listOf(BucketCount("Finance", 3, 0.6f)),
            gradeCounts = listOf(BucketCount("A", 2, 0.4f)),
            longestServing = listOf(EmployeeSummary(1, "Ali", "Finance", "Officer", 60, 20)),
        )
        val labels = (0 until ExcelWriter.ANALYTICS_INDEX).map { "l$it" }
        val file = temp.newFile("analytics.xlsx")
        file.outputStream().use { out ->
            ExcelWriter.writeAnalytics(out, snap, labels)
        }
        // just ensure file exists and has size
        assertThat(file.length()).isGreaterThan(0)
    }

    @Test
    fun writeErrorReport_createsSheet() {
        val errs = listOf(
            ImportRowError(4, "Hire Date", "2024-5", ImportErrorReason.INVALID_DATE),
            ImportRowError(6, "Full Name", "", ImportErrorReason.MISSING_REQUIRED),
            ImportRowError(8, "Salary", "€1 234,56", ImportErrorReason.INVALID_NUMBER),
        )
        val labels = ExcelWriter.ErrorReportLabels("Errors", "Row", "Column", "Raw", "Reason")
        val file = temp.newFile("errors.xlsx")
        file.outputStream().use { out ->
            ExcelWriter.writeErrorReport(out, errs, labels)
        }
        assertThat(file.length()).isGreaterThan(0)
    }
}