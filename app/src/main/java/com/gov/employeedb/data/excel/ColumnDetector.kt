package com.gov.employeedb.data.excel

import com.gov.employeedb.domain.model.EmployeeField

/**
 * The column-mapping brain of the import wizard (PLAN.md §6.2).
 *
 * [detect] scores every header against the trilingual synonyms (exact = 2, token/contains = 1),
 * data-sniffs zero-score columns (dates → the two date fields in column order, `@` → email,
 * phone-like → phone, currency/numeric → salary), then assigns greedily: highest score first, one
 * header per field, ties broken by column order. [confidence] is `mappedCoreFields / 3f`
 * (PLAN.md §6.2 rule 4) — below `0.8` or missing any core field forces the manual Step-2 mapping.
 *
 * [findHeaderRow] scans rows `0..9` for a header that matches ≥ 2 synonyms (government files often
 * carry a merged title row and blank rows above the real headers); when none qualifies it falls back
 * to row 0 and forces low confidence via [HeaderRow.found] = `false`.
 */
object ColumnDetector {

    /** A detected mapping plus its agreement score (PLAN.md Phase-5 checklist). */
    data class DetectionResult(
        val mapping: Map<Int, EmployeeField>,
        val confidence: Float,
        val unmapped: List<Int>,
    )

    /** Where [findHeaderRow] believes the header lives (`index` 0-based), and whether it is certain. */
    data class HeaderRow(
        val index: Int,
        val found: Boolean,
    )

    private val REQUIRED_CORE: Set<EmployeeField> = EmployeeField.REQUIRED_CORE
    private const val HEADER_SCAN_ROWS = 10
    private const val MIN_HEADER_MATCHES = 2
    private const val CELL_MIN_LENGTH = 2
    private const val CELL_MAX_LENGTH = 40

    fun detect(
        headers: List<String>,
        sampleRows: List<List<String?>>,
    ): DetectionResult {
        if (headers.isEmpty()) {
            return DetectionResult(emptyMap(), 0f, emptyList())
        }
        val scores = scoreHeaders(headers, sampleRows)
        val mapping = assignGreedily(scores)
        val unmapped = headers.indices.filterNot { it in mapping }
        val mappedCore = mapping.values.count { it in REQUIRED_CORE }
        val confidence = mappedCore / CORE_FIELDS.toFloat()
        return DetectionResult(mapping, confidence, unmapped)
    }

    fun findHeaderRow(rows: List<List<String?>>): HeaderRow {
        val scored = rows.take(HEADER_SCAN_ROWS).mapIndexed { index, cells ->
            index to cells.count { cell -> isPlausibleHeaderCell(cell) }
        }
        val best = scored.maxByOrNull { it.second } ?: return HeaderRow(0, found = false)
        if (best.second < MIN_HEADER_MATCHES) return HeaderRow(0, found = false)
        return HeaderRow(best.first, found = true)
    }

    private fun scoreHeaders(headers: List<String>, sampleRows: List<List<String?>>): Array<IntArray> {
        val scores = Array(headers.size) { IntArray(EmployeeField.entries.size) }
        headers.forEachIndexed { column, raw ->
            val normalized = HeaderNormalizer.normalize(raw).takeIf { it.isNotEmpty() }
            if (normalized != null) {
                EmployeeField.entries.forEachIndexed { fieldOrdinal, field ->
                    scores[column][fieldOrdinal] = synonymScore(normalized, field)
                }
            }
        }
        sniffData(headers, sampleRows, scores)
        return scores
    }

    private fun synonymScore(normalizedHeader: String, field: EmployeeField): Int {
        val keys = HeaderSynonyms.normalizedKeys.getValue(field)
        return when {
            normalizedHeader in keys -> EXACT_SCORE
            keys.any { normalizedHeader.contains(it) } -> CONTAINS_SCORE
            else -> 0
        }
    }

    /** §6.2 rule 2 — data sniffing for columns that matched no synonym at all. */
    private fun sniffData(headers: List<String>, sampleRows: List<List<String?>>, scores: Array<IntArray>) {
        var dateColumnsSeen = 0
        headers.indices.forEach { column ->
            if (scores[column].any { it > 0 }) return@forEach
            val values = sampleRows.mapNotNull { it.getOrNull(column) }
            if (values.isEmpty()) return@forEach

            if (values.all { parseAsDate(it) }) {
                when (dateColumnsSeen) {
                    0 -> scores[column][EmployeeField.DATE_OF_BIRTH.ordinal] += 1
                    1 -> scores[column][EmployeeField.HIRE_DATE.ordinal] += 1
                    else -> Unit
                }
                dateColumnsSeen++
            }
            if (values.all { it.contains('@') && it.contains('.') }) {
                scores[column][EmployeeField.EMAIL.ordinal] += 1
            }
            if (values.all { looksLikePhone(it) }) {
                scores[column][EmployeeField.PHONE.ordinal] += 1
            }
            if (values.all { looksLikeSalary(it) }) {
                scores[column][EmployeeField.SALARY.ordinal] += 1
            }
        }
    }

    /** §6.2 rule 3 — highest score first, one header per field, one field per header, column-order ties. */
    private fun assignGreedily(scores: Array<IntArray>): Map<Int, EmployeeField> {
        val candidates = mutableListOf<Triple<Int, EmployeeField, Int>>()
        scores.forEachIndexed { column, row ->
            EmployeeField.entries.forEachIndexed { ordinal, field ->
                if (row[ordinal] > 0) candidates += Triple(column, field, row[ordinal])
            }
        }
        candidates.sortWith(
            compareByDescending<Triple<Int, EmployeeField, Int>> { it.third }
                .thenBy { it.first }
                .thenBy { it.second.ordinal },
        )
        val mapping = mutableMapOf<Int, EmployeeField>()
        val taken = mutableSetOf<EmployeeField>()
        for ((column, field, _) in candidates) {
            if (field in taken) continue
            mapping[column] = field
            taken += field
        }
        return mapping
    }

    private fun parseAsDate(value: String): Boolean =
        DateParser.parse(value, today = null) != null

    private fun looksLikePhone(value: String): Boolean {
        val digits = value.count { it.isDigit() }
        return value.contains('+') || digits >= 8
    }

    private fun looksLikeSalary(value: String): Boolean {
        val digits = value.count { it.isDigit() }
        if (digits == 0) return false
        val separators = value.count { !it.isDigit() }
        val clean = value.replace(" ", "")
        val decimals = if (clean.contains('.')) clean.substringAfterLast('.') else ""
        return separators <= 4 && decimals.length <= 2
    }

    private fun isPlausibleHeaderCell(raw: String?): Boolean {
        if (raw.isNullOrBlank()) return false
        val normalized = HeaderNormalizer.normalize(raw)
        if (normalized.length !in CELL_MIN_LENGTH..CELL_MAX_LENGTH) return false
        return HeaderSynonyms.allNormalized.containsKey(normalized)
    }

    private const val EXACT_SCORE = 2
    private const val CONTAINS_SCORE = 1
    private const val CORE_FIELDS = 3
}