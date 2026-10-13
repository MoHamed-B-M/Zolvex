package com.gov.employeedb.data.excel

import com.gov.employeedb.domain.model.Employee
import com.gov.employeedb.domain.model.EmployeeField
import com.gov.employeedb.testutil.TestData
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Streaming memory behavior: read 10k rows through the SAX reader without OOM; use a real reader.
 */
class LargeFileStreamingTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val today: LocalDate = TestData.TODAY
    private val enHeaders = listOf(
        "Full Name", "Date of Birth", "Hire Date", "Department", "Position",
        "Grade", "Salary", "Email", "Phone", "Address", "Notes",
    )

    @Test
    fun stream_10k_rows_staysWithinHeapBudget() = runBlocking {
        // build 10k employees
        val baseDob = LocalDate.of(1970, 1, 1)
        val baseHire = LocalDate.of(2000, 1, 1)
        val emps = ArrayList<Employee>(10000)
        val batchId = UUID.randomUUID().toString()
        for (i in 0 until 10000) {
            val dob = baseDob.plus((i % 1000).toLong(), ChronoUnit.DAYS)
            val hire = baseHire.plus((i % 2000).toLong(), ChronoUnit.DAYS)
            emps.add(
                Employee(
                    fullName = "Emp $i",
                    dateOfBirth = dob,
                    hireDate = hire,
                    department = if (i % 2 == 0) "A" else "B",
                    position = "P",
                    grade = "C",
                    salary = 30000.0 + (i % 5000).toDouble(),
                    email = "emp$i@test.org",
                    phone = "+213 $i",
                    address = "Addr $i",
                    notes = null,
                    importBatchId = batchId,
                    sourceRow = i + 2, // 1-based relative
                ),
            )
        }
        val file = temp.newFile("large.xlsx")
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

        // measure heap before
        val rt = Runtime.getRuntime()
        System.gc()
        val before = rt.totalMemory() - rt.freeMemory()

        val reader = ExcelReader(Dispatchers.IO)
        val preview = reader.preview(file.inputStream()).getOrNull()!!
        var imported = 0
        reader.streamRows(File(file.toURI()).inputStream()).collect { row ->
            if (row.sourceRow <= preview.headerRowIndex + 1) return@collect
            imported++
            // small yield is automatic; don't hold row
        }
        System.gc()
        val after = rt.totalMemory() - rt.freeMemory()
        val deltaMb = (after - before) / (1024L * 1024L)
        assertThat(imported).isEqualTo(10000)
        assertThat(deltaMb).isLessThan(64)
    }
}