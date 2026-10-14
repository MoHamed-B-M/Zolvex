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
