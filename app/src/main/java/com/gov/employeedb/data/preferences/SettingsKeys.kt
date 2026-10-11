package com.gov.employeedb.data.preferences

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * DataStore keys for app settings.
 *
 * Values are stored as strings: both settings are small closed enums, and a string keeps the
 * stored form readable (and forward-compatible) without a custom
 * [androidx.datastore.core.Serializer].
 */
internal object SettingsKeys {
    /** Stores an [com.gov.employeedb.core.AppLanguage] tag (`en`, `fr`, `ar`). */
    val LANGUAGE: Preferences.Key<String> = stringPreferencesKey("language")

    /** Stores an [com.gov.employeedb.core.AppThemeMode] name (`LIGHT`, `DARK`, `SYSTEM`). */
    val THEME_MODE: Preferences.Key<String> = stringPreferencesKey("theme_mode")
}
