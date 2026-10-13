package com.gov.employeedb.data.excel

import com.gov.employeedb.testutil.TestData
import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test

/**
 * Validates the strict date parsing rules (day-first, yyyy-MM-dd, and the month-first ambiguity guard).
 */
class DateParserTest {

    private val today: LocalDate = TestData.TODAY

    @Test
    fun parse_ddMMyyyy() {
        assertThat(DateParser.parse("01/03/2001", today = today)).isEqualTo(LocalDate.of(2001, 3, 1))
        assertThat(DateParser.parse("1/3/2001", today = today)).isEqualTo(LocalDate.of(2001, 3, 1))
    }

    @Test
    fun parse_dashAndDot() {
        assertThat(DateParser.parse("12-05-1965", today = today)).isEqualTo(LocalDate.of(1965, 5, 12))
        assertThat(DateParser.parse("12.05.1965", today = today)).isEqualTo(LocalDate.of(1965, 5, 12))
    }

    @Test
    fun parse_iso() {
        assertThat(DateParser.parse("1992-10-14", today = today)).isEqualTo(LocalDate.of(1992, 10, 14))
    }

    @Test
    fun parse_monthFirstAmbiguity() {
        // "05/13/2024" — day-first cannot hold month 13; parser falls back to MM/dd/yyyy
        assertThat(DateParser.parse("05/13/2024", today = today)).isEqualTo(LocalDate.of(2024, 5, 13))
    }

    @Test
    fun parse_rejectsFutureAndBefore1900() {
        assertThat(DateParser.parse("01/01/1899", today = today)).isNull()
        assertThat(DateParser.parse("12/12/3000", today = today)).isNull()
    }

    @Test
    fun parse_rejectsInvalidDayMonth() {
        assertThat(DateParser.parse("31/02/2024", today = today)).isNull()
        assertThat(DateParser.parse("40/05/2024", today = today)).isNull()
    }
}