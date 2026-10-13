package com.gov.employeedb.core

/**
 * Converts the Arabic-Indic / Eastern Arabic-Indic digit blocks to ASCII digits (PLAN.md §7.4 #5).
 *
 * Arabic-locale devices and spreadsheets frequently store numbers as `٠١٢٣٤٥٦٧٨٩` or `۰۱۲۳۴۵۶۷۸۹`;
 * every numeric parse (dates, salaries, phone sniffing, form fields) runs through here first so the
 * import pipeline is locale-stable. The function is pure and never throws.
 */
object LocaleDigits {

    fun toAsciiDigits(raw: String): String = buildString(raw.length) {
        for (ch in raw) {
            append(ch.toAsciiDigit())
        }
    }

    private fun Char.toAsciiDigit(): Char = when (this) {
        in ARABIC_INDIC_ZERO..ARABIC_INDIC_NINE -> '0' + (this - ARABIC_INDIC_ZERO)
        in EASTERN_ZERO..EASTERN_NINE -> '0' + (this - EASTERN_ZERO)
        else -> this
    }

    private const val ARABIC_INDIC_ZERO = '\u0660'
    private const val ARABIC_INDIC_NINE = '\u0669'
    private const val EASTERN_ZERO = '\u06F0'
    private const val EASTERN_NINE = '\u06F9'
}