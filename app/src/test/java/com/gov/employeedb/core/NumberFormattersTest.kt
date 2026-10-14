package com.gov.employeedb.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The dashboard's number-formatting contract (PLAN.md Phase-9 checklist): one decimal place for the
 * averages and a whole number with a percent sign for the legend shares.
 */
class NumberFormattersTest {

    @Test
    fun `oneDecimal always shows exactly one decimal place`() {
        assertThat(NumberFormatters.oneDecimal(34.7)).isEqualTo("34.7")
        assertThat(NumberFormatters.oneDecimal(6.0)).isEqualTo("6.0")
    }

    @Test
    fun `oneDecimal rounds to the nearest tenth`() {
        assertThat(NumberFormatters.oneDecimal(34.74)).isEqualTo("34.7")
        assertThat(NumberFormatters.oneDecimal(34.76)).isEqualTo("34.8")
    }

    @Test
    fun `percent is a whole number with a percent sign`() {
        assertThat(NumberFormatters.percent(0.347f)).isEqualTo("35%")
        assertThat(NumberFormatters.percent(0.5f)).isEqualTo("50%")
        assertThat(NumberFormatters.percent(0f)).isEqualTo("0%")
    }
}
