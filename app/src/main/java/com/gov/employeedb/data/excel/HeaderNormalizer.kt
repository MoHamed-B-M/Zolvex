package com.gov.employeedb.data.excel

import java.text.Normalizer

/**
 * Collapses a raw spreadsheet header to a comparable key (PLAN.md §6.2 step 1).
 *
 * Pipeline: trim → NFD → drop combining marks (`é→e`, `à→a`) → drop Arabic tashkeel
 * (`\u064B–\u0652`, `\u0670`) and tatweel (`\u0640`) → lowercase → replace punctuation/digits with
 * spaces → collapse whitespace → strip a leading Arabic definite article `ال` → trim. Both header
 * and synonym run through the same pipeline in [HeaderSynonyms], so `École`, `ecole`, `الإدارة`
 * and `الادارة` all converge on one key. Pure and deterministic.
 */
object HeaderNormalizer {

    fun normalize(raw: String): String {
        if (raw.isBlank()) return ""
        var s = raw.trim()
        s = Normalizer.normalize(s, Normalizer.Form.NFD)
        s = s.replace(MARK_NON_SPACING_REGEX, "")
        s = s.replace(TASHKEEL_TATWEEL_REGEX, "")
        s = s.lowercase()
        s = s.replace(PUNCTUATION_DIGIT_REGEX, SPACE)
        s = collapseWhitespace(s)
        s = stripLeadingArticle(s)
        return s.trim()
    }

    private fun collapseWhitespace(s: String): String = s.trim().split(WHITESPACE_REGEX).joinToString(SPACE)

    private fun stripLeadingArticle(s: String): String =
        if (s.length > ARTICLE.length && s.startsWith(ARTICLE) && s[ARTICLE.length].isLetter()) {
            s.removePrefix(ARTICLE)
        } else {
            s
        }

    private val MARK_NON_SPACING_REGEX = Regex("\\p{Mn}")
    private val TASHKEEL_TATWEEL_REGEX = Regex("[\u064B-\u0652\u0670\u0640]")
    private val PUNCTUATION_DIGIT_REGEX = Regex("[\\p{P}\\p{N}]")
    private val WHITESPACE_REGEX = Regex("\\s+")
    private const val SPACE = " "
    private const val ARTICLE = "ال"
}