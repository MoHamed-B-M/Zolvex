package com.gov.employeedb.data.excel

import com.gov.employeedb.core.LocaleDigits
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.DateUtil

/**
 * Strict, locale-stable date parsing for the import pipeline (PLAN.md §6.2 / Phase-5 checklist).
 *
 * Supports the documented text patterns day-first (`dd/MM/uuuu`, `d/M/uuuu`, `dd-MM-uuuu`,
 * `uuuu-MM-dd`, `dd.MM.uuuu`) with a `MM/dd/uuuu` fallback only when day-first is impossible, plus
 * Excel serial dates read from a formatted [Cell]. A date before 1900-01-01 is
 * always rejected; when [today] is supplied, any date after it (a future DOB) is rejected too, so
 * the parser stays deterministic in tests instead of consulting a hidden clock.
 */
object DateParser {

    fun parse(
        raw: String?,
        cell: Cell? = null,
        today: LocalDate? = null,
    ): LocalDate? {
        val viaCell = cell?.let { parseCellDate(it) }
        if (viaCell != null) return viaCell.sanitize(today)

        val text = raw?.let { LocaleDigits.toAsciiDigits(it.trim()) }?.takeIf { it.isNotEmpty() }
            ?: return null
        val parsed = DAY_FIRST_PATTERNS.asSequence()
            .mapNotNull { tryParse(text, it) }
            .firstOrNull()
            ?: if (text.isMonthFirstAmbiguity()) tryParse(text, MONTH_FIRST) else null
        return parsed?.sanitize(today)
    }

    private fun parseCellDate(cell: Cell): LocalDate? {
        if (!DateUtil.isCellDateFormatted(cell)) return null
        return try {
            val dt = cell.dateCellValue
            dt.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        } catch (_: RuntimeException) {
            null
        }
    }

    private fun tryParse(text: String, pattern: String): LocalDate? = try {
        DateTimeFormatter.ofPattern(pattern, java.util.Locale.ROOT)
            .withResolverStyle(ResolverStyle.STRICT)
            .parse(text, LocalDate::from)
    } catch (_: RuntimeException) {
        null
    }

    /** "05/13/2024" — day-first patterns cannot hold month 13, so a month-first reading is legal. */
    private fun String.isMonthFirstAmbiguity(): Boolean {
        val parts = split('/')
        if (parts.size != 3) return false
        val first = parts[0].toIntOrNull() ?: return false
        val second = parts[1].toIntOrNull() ?: return false
        return second > 12 && first in 1..12
    }

    private fun LocalDate.sanitize(today: LocalDate?): LocalDate? {
        if (isBefore(YEAR_1900)) return null
        if (today != null && isAfter(today)) return null
        return this
    }

    private val YEAR_1900: LocalDate = LocalDate.of(1900, 1, 1)

    private val DAY_FIRST_PATTERNS = listOf(
        "dd/MM/uuuu", "d/M/uuuu", "dd-MM-uuuu", "uuuu-MM-dd", "dd.MM.uuuu",
    )

    private const val MONTH_FIRST = "MM/dd/uuuu"
}