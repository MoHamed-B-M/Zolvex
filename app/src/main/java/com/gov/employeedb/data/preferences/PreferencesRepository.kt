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

    suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { settings -> settings[SettingsKeys.LANGUAGE] = language.tag }
    }

    suspend fun setThemeMode(themeMode: AppThemeMode) {
        dataStore.edit { settings -> settings[SettingsKeys.THEME_MODE] = themeMode.name }
    }

    /** A corrupt store must not crash the app: fall back to empty preferences and let the next
     * write repair the file. */
    private fun preferences(): Flow<Preferences> = dataStore.data.catch { cause ->
        if (cause is IOException) emit(emptyPreferences()) else throw cause
    }
}
