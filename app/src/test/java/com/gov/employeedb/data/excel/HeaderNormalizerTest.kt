package com.gov.employeedb.data.excel

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Ensures normalization collapses accents, tashkeel and the Arabic definite article as specified.
 */
class HeaderNormalizerTest {

    @Test
    fun normalize_accentsAndCase() {
        assertThat(HeaderNormalizer.normalize("École")).isEqualTo("ecole")
        assertThat(HeaderNormalizer.normalize("NAISSANCE")).isEqualTo("naissance")
    }

    @Test
    fun normalize_stripsArticle() {
        // NFD + `\p{Mn}` drops the hamza mark on أ/إ, so both spellings converge on the bare alef.
        assertThat(HeaderNormalizer.normalize("الإدارة")).isEqualTo("ادارة")
        assertThat(HeaderNormalizer.normalize("الادارة")).isEqualTo("ادارة")
        // Only the single leading `ال` is stripped, and hamza is removed by the `\p{Mn}` pass.
        assertThat(HeaderNormalizer.normalize("الأجر الإجمالي")).isEqualTo("اجر الاجمالي")
    }

    @Test
    fun normalize_collapsePunctuationDigits() {
        // Parentheses become spaces; the abbreviation's letters survive as a trailing token.
        assertThat(HeaderNormalizer.normalize("Date of Birth (DOB)")).isEqualTo("date of birth dob")
        assertThat(HeaderNormalizer.normalize("E-mail")).isEqualTo("e mail")
    }
}