package com.gov.employeedb.data.excel

import com.gov.employeedb.domain.model.EmployeeField
import com.gov.employeedb.testutil.TestData
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Column detection must score trilingual headers, sniff data, assign greedily and compute confidence.
 */
class ColumnDetectorTest {

    private val enHeaders = listOf(
        "Full Name", "Date of Birth", "Hire Date", "Department", "Position",
        "Grade", "Salary", "Email", "Phone", "Address", "Notes",
    )

    @Test
    fun detect_exactEnglish_givesHighConfidence() {
        val samples = listOf(
            listOf("Amine Benali", "12/05/1965", "01/03/2001", "Finance", "Officer", "A", "38500", "a@x.com", "+2131", "Addr", ""),
        )
        val result = ColumnDetector.detect(enHeaders, samples)
        assertThat(result.confidence).isEqualTo(1.0f)
        assertThat(result.mapping[0]).isEqualTo(EmployeeField.FULL_NAME)
        assertThat(result.mapping[1]).isEqualTo(EmployeeField.DATE_OF_BIRTH)
        assertThat(result.mapping[2]).isEqualTo(EmployeeField.HIRE_DATE)
    }

    @Test
    fun detect_arabicSynonyms() {
        val arHeaders = listOf(
            "الاسم الكامل", "تاريخ الميلاد", "تاريخ التعيين", "القسم", "الوظيفة",
            "الدرجة", "الراتب", "البريد الإلكتروني", "الهاتف", "العنوان", "ملاحظات",
        )
        val samples = listOf(listOf("كريم", "١٢/٠٥/١٩٦٥", "٠١/٠٣/٢٠٠١", "القسم", "موظف", "أ", "٣٨٥٠٠", "a@x.com", "٠٥٥٥", "عنوان", ""))
        val result = ColumnDetector.detect(arHeaders, samples)
        assertThat(result.mapping.values).containsAtLeast(EmployeeField.FULL_NAME, EmployeeField.DATE_OF_BIRTH, EmployeeField.HIRE_DATE)
        assertThat(result.confidence).isEqualTo(1.0f)
    }

    @Test
    fun findHeaderRow_ignoresMergedTitleAndBlankRows() {
        // rows 0:title (count 0), 1:blank (count 0), 2:blank (count 0), 3:header (count 11 matches)
        val headerCells = enHeaders.map { it as String? }
        val rows = listOf(
            listOf("وزارة العمل — قائمة الموظفين"),
            listOf<String?>(),
            listOf<String?>(),
            headerCells,
            listOf("A", "12/05/1965", "01/03/2001", "D", "P", "A", "1000", "e@m.c", "t", "a", null),
        )
        val h = ColumnDetector.findHeaderRow(rows)
        assertThat(h.index).isEqualTo(3)
        assertThat(h.found).isTrue()
    }
}