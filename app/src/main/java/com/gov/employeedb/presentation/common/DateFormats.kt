package com.gov.employeedb.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * The app's date-of-record format: the active locale's medium date, resolved from the configuration
 * the app pinned for the chosen language.
 *
 * Every screen that shows a [LocalDate] goes through here, so dates render in the user's language
 * and no screen hardcodes a pattern.
 */
@Composable
fun rememberDateFormatter(): DateTimeFormatter {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    }
}

/**
 * The app's instant-of-record format: the active locale's medium date plus a short time, for
 * timestamps (a `Long` of epoch milliseconds) such as the last update check.
 *
 * Kept beside [rememberDateFormatter] so both date renderings in the app resolve through the one
 * configuration the app pinned for the chosen language.
 */
@Composable
fun rememberDateTimeFormatter(): DateTimeFormatter {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale) {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(locale)
    }
}
