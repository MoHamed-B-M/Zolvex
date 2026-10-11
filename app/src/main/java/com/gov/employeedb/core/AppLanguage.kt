package com.gov.employeedb.core

import androidx.annotation.StringRes
import com.gov.employeedb.R
import java.util.Locale

/**
 * A language the app UI can be displayed in.
 *
 * [nativeNameRes] is a language's own name (for example "العربية"), so the Settings language
 * picker stays legible to a user who cannot read the language the UI is currently in.
 */
enum class AppLanguage(
    val tag: String,
    val isRtl: Boolean,
    @StringRes val nativeNameRes: Int,
) {
    EN("en", isRtl = false, nativeNameRes = R.string.language_english),
    FR("fr", isRtl = false, nativeNameRes = R.string.language_french),
    AR("ar", isRtl = true, nativeNameRes = R.string.language_arabic),
    ;

    /** BCP-47 locale used for resource selection and for date/number formatting. */
    val locale: Locale = Locale.forLanguageTag(tag)

    companion object {
        /** Used when the stored value is missing or unrecognised. */
        val DEFAULT: AppLanguage = EN

        /**
         * Resolves a language from a BCP-47 language tag such as `en`, `en-US` or `ar_EG`.
         * Region and script subtags are ignored; anything unknown falls back to [DEFAULT].
         */
        fun fromTag(tag: String?): AppLanguage {
            val primary = tag
                ?.trim()
                ?.substringBefore('-')
                ?.substringBefore('_')
                ?.lowercase(Locale.ROOT)
            return entries.firstOrNull { it.tag == primary } ?: DEFAULT
        }
    }
}
