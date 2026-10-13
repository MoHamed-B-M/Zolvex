package com.gov.employeedb.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Verifies Arabic-Indic/Eastern Arabic-Indic digit conversion (PLAN.md §7.4 #5). Pure JVM.
 */
class LocaleDigitsTest {

    @Test
    fun toAsciiDigits_convertsArabicIndic() {
        assertThat(LocaleDigits.toAsciiDigits("٠١٢٣٤٥٦٧٨٩")).isEqualTo("0123456789")
    }

    @Test
    fun toAsciiDigits_convertsEasternArabicIndic() {
        assertThat(LocaleDigits.toAsciiDigits("۰۱۲۳۴۵۶۷۸۹")).isEqualTo("0123456789")
    }

    @Test
    fun toAsciiDigits_mixedText() {
        assertThat(LocaleDigits.toAsciiDigits("تاريخ ١٢/٠٥/١٩٦٥")).isEqualTo("تاريخ 12/05/1965")
    }

    @Test
    fun toAsciiDigits_asciiUnchanged() {
        assertThat(LocaleDigits.toAsciiDigits("2024-05-12")).isEqualTo("2024-05-12")
    }
}