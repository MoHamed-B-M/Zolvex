package com.gov.employeedb.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import com.gov.employeedb.core.AppLanguage
import com.gov.employeedb.core.AppThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * The process-wide settings store. The delegate guarantees a single instance per process, so no
 * Hilt module is required (a second instance on the same file would throw).
 */
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "zolvex_settings",
)

/**
 * Persisted app settings.
 *
 * DataStore performs all file I/O on its own dispatcher, so nothing here needs an injected
 * dispatcher: this class never touches `Dispatchers.IO` itself (PLAN.md ADR-8).
 */
@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore: DataStore<Preferences>
        get() = context.settingsDataStore

    /** The persisted UI language; emits [AppLanguage.DEFAULT] until the user picks one. */
    val language: Flow<AppLanguage> = preferences()
        .map { settings -> AppLanguage.fromTag(settings[SettingsKeys.LANGUAGE]) }
        .distinctUntilChanged()

    /** The persisted theme mode; emits [AppThemeMode.DEFAULT] until the user picks one. */
    val themeMode: Flow<AppThemeMode> = preferences()
        .map { settings -> AppThemeMode.fromName(settings[SettingsKeys.THEME_MODE]) }
        .distinctUntilChanged()

    /** Last update check time in milliseconds since epoch; null if never checked. */
    val lastUpdateCheckTime: Flow<Long?> = preferences()
        .map { settings -> settings[SettingsKeys.LAST_UPDATE_CHECK_TIME] }
        .distinctUntilChanged()

    /** Whether auto-check for updates is enabled; defaults to true. */
    val autoCheckEnabled: Flow<Boolean> = preferences()
        .map { settings -> settings[SettingsKeys.AUTO_CHECK_ENABLED] ?: true }
        .distinctUntilChanged()

    /** Whether to include pre-releases; defaults to false. */
    val includePrereleases: Flow<Boolean> = preferences()
        .map { settings -> settings[SettingsKeys.INCLUDE_PRERELEASES] ?: false }
        .distinctUntilChanged()

    suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { settings -> settings[SettingsKeys.LANGUAGE] = language.tag }
    }

    suspend fun setThemeMode(themeMode: AppThemeMode) {
        dataStore.edit { settings -> settings[SettingsKeys.THEME_MODE] = themeMode.name }
    }

    suspend fun setLastUpdateCheckTime(timestampMs: Long) {
        dataStore.edit { settings -> settings[SettingsKeys.LAST_UPDATE_CHECK_TIME] = timestampMs }
    }

    suspend fun setAutoCheckEnabled(enabled: Boolean) {
        dataStore.edit { settings -> settings[SettingsKeys.AUTO_CHECK_ENABLED] = enabled }
    }

    suspend fun setIncludePrereleases(include: Boolean) {
        dataStore.edit { settings -> settings[SettingsKeys.INCLUDE_PRERELEASES] = include }
    }

    /** A corrupt store must not crash the app: fall back to empty preferences and let the next
     * write repair the file. */
    private fun preferences(): Flow<Preferences> = dataStore.data.catch { cause ->
        if (cause is IOException) emit(emptyPreferences()) else throw cause
    }
}
