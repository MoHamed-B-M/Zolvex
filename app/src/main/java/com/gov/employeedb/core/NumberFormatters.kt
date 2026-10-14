package com.gov.employeedb.core

import java.util.Locale

/**
 * Number formatting for the dashboard statistics (PLAN.md Phase-9 checklist).
 *
 * Both functions format with [Locale.ROOT] rather than the current locale, so a figure always reads
 * as ASCII digits with a `.` decimal separator. That matches every other numeric surface in the app:
 * the count-up text renders an `Int` directly, and [LocaleDigits] normalises imported Arabic-Indic
 * digits to ASCII, so a localised decimal separator here would be the one place a statistic did not
 * look like the raw counts beside it.
 */
object NumberFormatters {

    /** A single decimal place, e.g. `34.7` or `6.0`. */
    fun oneDecimal(value: Double): String = String.format(Locale.ROOT, "%.1f", value)

    /** A whole-number percentage, e.g. `35%`. */
    fun percent(share: Float): String = "${Math.round(share * 100)}%"
}
