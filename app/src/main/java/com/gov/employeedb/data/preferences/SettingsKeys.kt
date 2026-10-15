package com.gov.employeedb.data.preferences

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * DataStore keys for app settings.
 *
 * Values are stored as strings/longs/booleans depending on type; string forms
 * for enum-backed settings keep them forward-compatible.
 */
internal object SettingsKeys {
    /** Stores an [com.gov.employeedb.core.AppLanguage] tag (`en`, `fr`, `ar`). */
    val LANGUAGE: Preferences.Key<String> = stringPreferencesKey("language")

    /** Stores an [com.gov.employeedb.core.AppThemeMode] name (`LIGHT`, `DARK`, `SYSTEM`). */
    val THEME_MODE: Preferences.Key<String> = stringPreferencesKey("theme_mode")

    /** Last update check timestamp (milliseconds since epoch). */
    val LAST_UPDATE_CHECK_TIME: Preferences.Key<Long> = longPreferencesKey("last_update_check_time")

    /** Whether auto-check for updates is enabled (default: true). */
    val AUTO_CHECK_ENABLED: Preferences.Key<Boolean> = booleanPreferencesKey("auto_check_enabled")

    /** Whether to include pre-releases when checking for updates (default: false). */
    val INCLUDE_PRERELEASES: Preferences.Key<Boolean> = booleanPreferencesKey("include_prereleases")
}
