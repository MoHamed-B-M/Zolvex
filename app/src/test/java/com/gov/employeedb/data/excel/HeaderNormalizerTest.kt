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
        assertThat(HeaderNormalizer.normalize("الإدارة")).isEqualTo("إدارة")
        assertThat(HeaderNormalizer.normalize("الادارة")).isEqualTo("ادارة")
        assertThat(HeaderNormalizer.normalize("الأجر الإجمالي")).isEqualTo("أجر إجمالي")
    }

    @Test
    fun normalize_collapsePunctuationDigits() {
        assertThat(HeaderNormalizer.normalize("Date of Birth (DOB)")).isEqualTo("date of birth")
        assertThat(HeaderNormalizer.normalize("E-mail")).isEqualTo("e mail")
    }
}