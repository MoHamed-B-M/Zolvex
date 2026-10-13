package com.gov.employeedb.data.excel

import com.gov.employeedb.domain.model.EmployeeField
import com.gov.employeedb.testutil.TestData
import com.google.common.truth.Truth.assertThat
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Test

/**
 * Exercises SAX-only reader: preview on fixtures and streaming/map counting against the malformed fixture.
 */
class ExcelReaderTest {

    private val today: LocalDate = TestData.TODAY

    @Test
    fun preview_en_sample_matchesContract() {
        val stream = load("/excel/en_sample.xlsx")
        val reader = ExcelReader(Dispatchers.IO)
        val res = reader.preview(stream)
        assertThat(res.isSuccess).isTrue()
        val prev = res.getOrNull()!!
        assertThat(prev.sheetName).isEqualTo("Employees")
        assertThat(prev.headerRowIndex).isEqualTo(0)
        assertThat(prev.headers).isEqualTo(
            listOf("Full Name", "Date of Birth", "Hire Date", "Department", "Position", "Grade", "Salary", "Email", "Phone", "Address", "Notes"),
        )
        assertThat(prev.totalRowEstimate).isEqualTo(10)
        // core 3 mapped -> confidence 1.0f
        assertThat(prev.detectedMapping.values).containsAtLeast(EmployeeField.FULL_NAME, EmployeeField.DATE_OF_BIRTH, EmployeeField.HIRE_DATE)
        assertThat(prev.detectedMapping.size >= 3).isTrue()
        assertThat(prev.confidenceOr1f(prev.detectedMapping)).isEqualTo(1.0f)
    }

    @Test
    fun preview_fr_sample_detectsHeaders() {
        val stream = load("/excel/fr_sample.xlsx")
        val reader = ExcelReader(Dispatchers.IO)
        val res = reader.preview(stream)
        assertThat(res.isSuccess).isTrue()
        val prev = res.getOrNull()!!
        assertThat(prev.totalRowEstimate).isEqualTo(10)
    }

    @Test
    fun preview_ar_sample_detectsHeaders() {
        val stream = load("/excel/ar_sample.xlsx")
        val reader = ExcelReader(Dispatchers.IO)
        val res = reader.preview(stream)
        assertThat(res.isSuccess).isTrue()
        val prev = res.getOrNull()!!
        assertThat(prev.totalRowEstimate).isEqualTo(10)
    }

    @Test
    fun preview_merged_headers_findsHeaderRow3() {
        val stream = load("/excel/merged_headers.xlsx")
        val reader = ExcelReader(Dispatchers.IO)
        val res = reader.preview(stream)
        assertThat(res.isSuccess).isTrue()
        val prev = res.getOrNull()!!
        assertThat(prev.headerRowIndex).isEqualTo(3)
        assertThat(prev.totalRowEstimate).isEqualTo(10)
    }

    @Test
    fun stream_malformed_countsErrors() = runBlocking {
        val stream = load("/excel/malformed.xlsx")
        val reader = ExcelReader(Dispatchers.IO)
        val preview = reader.preview(load("/excel/malformed.xlsx")).getOrNull()!!
        var imported = 0
        var failed = 0
        val reasons = mutableListOf<ImportErrorReason>()
        val sourceRows = mutableListOf<Int>()
        reader.streamRows(FileInputStream(resolveFixture("/excel/malformed.xlsx"))).collect { row ->
            // skip preamble/header? headerRowIndex 0, so skip rows with sourceRow <= headerRowIndex+1 = 1
            if (row.sourceRow <= preview.headerRowIndex + 1) return@collect
            val mapped = RowMapper.mapRow(row.cells, preview.headers, preview.detectedMapping, today, row.sourceRow, "test")
            if (mapped.errors.isNotEmpty()) {
                failed++
                reasons += mapped.errors.map { it.reason }
                sourceRows += mapped.errors.map { it.sourceRow }
            }
            if (mapped.employee != null) imported++
        }
        assertThat(imported).isEqualTo(6)
        assertThat(failed).isEqualTo(3)
        // reasons: INVALID_DATE (row 4), MISSING_REQUIRED (row 6), INVALID_NUMBER (row 8)
        val allReasons = mutableListOf<ImportErrorReason>()
        reasons.forEach { allReasons.addAll(it) }
        val allRows = mutableListOf<Int>()
        sourceRows.forEach { allRows.addAll(it) }
        assertThat(allReasons).containsAtLeast(
            ImportErrorReason.INVALID_DATE,
            ImportErrorReason.MISSING_REQUIRED,
            ImportErrorReason.INVALID_NUMBER,
        )
        assertThat(allRows).containsExactly(4, 6, 8)
    }

    private fun load(path: String): InputStream = javaClass.getResourceAsStream(path) ?: error("missing $path")

    private fun resolveFixture(path: String): File {
        val url = javaClass.getResource(path) ?: error("missing $path")
        return File(url.toURI())
    }

    private fun ExcelPreview.confidenceOr1f(mapping: Map<Int, EmployeeField>): Float {
        val core = mapping.values.count { it in EmployeeField.REQUIRED_CORE }
        return core / 3f
    }
}